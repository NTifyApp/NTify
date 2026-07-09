/*
 * Copyright [2025-2026] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.cache;

import com.google.gson.Gson;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public class Cache {
    private static final Gson gson = new Gson();
    private static final long MAX_CHUNK_SIZE = 8L * 1024 * 1024; //8MB
    private static final long MIN_COMPACT_SIZE = 1L * 1024 * 1024; //1MB
    private static final double DEAD_RATIO_THRESHOLD = 0.5;

    private final File cacheDir;
    private final boolean cacheEnabled;
    private final Map<String, Namespace> namespaces = new ConcurrentHashMap<>();

    public Cache() throws IOException {
        cacheEnabled = !PublicValues.config.getFields().cacheDisabled;

        if(!cacheEnabled) {
            cacheDir = null;
            return;
        }

        cacheDir = new File(PublicValues.fileslocation, "progcache");

        if(!cacheDir.exists()) {
            if(!cacheDir.mkdir()) {
                throw new IOException("Unable to create cache directory");
            }
        }
    }

    /**
     * Returns the handle for a given owner namespace (e.g. a View class name).
     * All entries put through this handle are stored under progcache/&lt;name&gt;-* chunk files.
     */
    public Namespace namespace(String name) {
        return namespaces.computeIfAbsent(name, n -> {
            try {
                return new Namespace(n);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Wipes every namespace's on-disk chunks/index, including ones never touched this run
     * (found by scanning progcache/ for "&lt;name&gt;-index.dat" files). Intended to be called
     * on application shutdown so cache entries never survive across app version changes.
     */
    public void clearAll() {
        if(!cacheEnabled) {
            for(Namespace ns : namespaces.values()) {
                try {
                    ns.clear();
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
            }
            return;
        }

        File[] indexFiles = cacheDir.listFiles((dir, fileName) -> fileName.endsWith("-index.dat"));
        if(indexFiles == null) return;

        for(File indexFile : indexFiles) {
            String fileName = indexFile.getName();
            String namespaceName = fileName.substring(0, fileName.length() - "-index.dat".length());
            try {
                namespace(namespaceName).clear();
            } catch (Exception e) {
                ConsoleLogging.Throwable(e);
            }
        }
    }

    public class Namespace {
        private final String name;
        private final Map<String, Entry> index = new ConcurrentHashMap<>();
        private final Map<String, byte[]> inMemory;
        private final ReentrantLock lock = new ReentrantLock();
        private int currentChunk = 0;
        private long currentChunkSize = 0;
        private long totalBytesOnDisk = 0;
        private long deadBytes = 0;

        private Namespace(String name) throws IOException {
            this.name = name;

            if(!cacheEnabled) {
                inMemory = new ConcurrentHashMap<>();
                return;
            }
            inMemory = null;
            loadIndex();
        }

        private File chunkFile(int chunkNum) {
            return new File(cacheDir, name + "-chunk" + chunkNum + ".dat");
        }

        private File indexFile() {
            return new File(cacheDir, name + "-index.dat");
        }

        //Index format: repeated [idLen:u16][id bytes][chunkNum:i32][offset:i64][length:i32][tombstone:u8]
        //Appended on every mutation and replayed at startup; last record per id wins.
        private void loadIndex() throws IOException {
            File idx = indexFile();
            if(!idx.exists()) return;

            try(DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(idx)))) {
                while(in.available() > 0) {
                    int idLen = in.readUnsignedShort();
                    byte[] idBytes = new byte[idLen];
                    in.readFully(idBytes);
                    String id = new String(idBytes, StandardCharsets.UTF_8);
                    int chunkNum = in.readInt();
                    long offset = in.readLong();
                    int length = in.readInt();
                    boolean tombstone = in.readUnsignedByte() != 0;

                    if(tombstone) {
                        Entry removed = index.remove(id);
                        if(removed != null) deadBytes += removed.length;
                    } else {
                        Entry previous = index.put(id, new Entry(chunkNum, offset, length));
                        if(previous != null) deadBytes += previous.length;
                        totalBytesOnDisk += length;
                    }
                    if(chunkNum > currentChunk) currentChunk = chunkNum;
                }
            }

            File chunk = chunkFile(currentChunk);
            currentChunkSize = chunk.exists() ? chunk.length() : 0;
        }

        private void appendIndexRecord(String id, int chunkNum, long offset, int length, boolean tombstone) throws IOException {
            byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
            try(DataOutputStream out = new DataOutputStream(new FileOutputStream(indexFile(), true))) {
                out.writeShort(idBytes.length);
                out.write(idBytes);
                out.writeInt(chunkNum);
                out.writeLong(offset);
                out.writeInt(length);
                out.writeByte(tombstone ? 1 : 0);
            }
        }

        public boolean has(String id) {
            if(!cacheEnabled) return inMemory.containsKey(id);
            return index.containsKey(id);
        }

        public void put(String id, byte[] value) throws IOException {
            if(!cacheEnabled) {
                inMemory.put(id, value);
                return;
            }

            lock.lock();
            try {
                if(currentChunkSize + value.length > MAX_CHUNK_SIZE && currentChunkSize > 0) {
                    currentChunk++;
                    currentChunkSize = 0;
                }

                long offset = currentChunkSize;
                try(FileOutputStream out = new FileOutputStream(chunkFile(currentChunk), true)) {
                    out.write(value);
                }
                currentChunkSize += value.length;
                totalBytesOnDisk += value.length;

                Entry previous = index.put(id, new Entry(currentChunk, offset, value.length));
                appendIndexRecord(id, currentChunk, offset, value.length, false);
                if(previous != null) deadBytes += previous.length;

                compactIfNeeded();
            } finally {
                lock.unlock();
            }
        }

        public void put(String id, Object value) throws IOException {
            put(id, gson.toJson(value).getBytes(StandardCharsets.UTF_8));
        }

        public byte[] get(String id) throws IOException {
            if(!cacheEnabled) return inMemory.get(id);

            lock.lock();
            try {
                Entry e = index.get(id);
                if(e == null) throw new IOException("Unable to find cache entry: " + id);

                byte[] value = new byte[e.length];
                try(RandomAccessFile raf = new RandomAccessFile(chunkFile(e.chunkNum), "r")) {
                    raf.seek(e.offset);
                    raf.readFully(value);
                }
                return value;
            } finally {
                lock.unlock();
            }
        }

        public <T> T get(String id, Class<T> type) throws IOException {
            return gson.fromJson(new String(get(id), StandardCharsets.UTF_8), type);
        }

        public void remove(String id) throws IOException {
            if(!cacheEnabled) {
                inMemory.remove(id);
                return;
            }

            lock.lock();
            try {
                Entry e = index.remove(id);
                if(e == null) return;
                appendIndexRecord(id, e.chunkNum, e.offset, e.length, true);
                deadBytes += e.length;

                compactIfNeeded();
            } finally {
                lock.unlock();
            }
        }

        private void compactIfNeeded() throws IOException {
            if(totalBytesOnDisk < MIN_COMPACT_SIZE) return;
            if(deadBytes < totalBytesOnDisk * DEAD_RATIO_THRESHOLD) return;
            compact();
        }

        public void clear() throws IOException {
            if(!cacheEnabled) {
                inMemory.clear();
                return;
            }

            lock.lock();
            try {
                for(int i = 0; i <= currentChunk; i++) {
                    File c = chunkFile(i);
                    if(c.exists() && !c.delete()) {
                        ConsoleLogging.error("Failed to delete cache chunk: " + c.getName());
                    }
                }
                File idx = indexFile();
                if(idx.exists() && !idx.delete()) {
                    ConsoleLogging.error("Failed to delete cache index: " + idx.getName());
                }

                index.clear();
                currentChunk = 0;
                currentChunkSize = 0;
                totalBytesOnDisk = 0;
                deadBytes = 0;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Rewrites all live entries into fresh chunks/index, reclaiming space left behind by
         * removed/overwritten entries. Triggered automatically by put()/remove() once dead space
         * crosses DEAD_RATIO_THRESHOLD - never called directly, so writes stay cheap (plain append)
         * in the common case and only pay the rewrite cost once waste has actually accumulated.
         */
        private void compact() throws IOException {
            if(!cacheEnabled) return;

            Map<String, Entry> liveEntries = new ConcurrentHashMap<>(index);
            String tempPrefix = name + "-compact-" + System.nanoTime();
            int newChunk = 0;
            long newChunkSize = 0;
            File newChunkFile = new File(cacheDir, tempPrefix + "-chunk" + newChunk + ".dat");
            Map<String, Entry> newIndex = new ConcurrentHashMap<>();

            for(Map.Entry<String, Entry> en : liveEntries.entrySet()) {
                byte[] value = get(en.getKey());

                if(newChunkSize + value.length > MAX_CHUNK_SIZE && newChunkSize > 0) {
                    newChunk++;
                    newChunkSize = 0;
                    newChunkFile = new File(cacheDir, tempPrefix + "-chunk" + newChunk + ".dat");
                }

                long offset = newChunkSize;
                try(FileOutputStream out = new FileOutputStream(newChunkFile, true)) {
                    out.write(value);
                }
                newChunkSize += value.length;
                newIndex.put(en.getKey(), new Entry(newChunk, offset, value.length));
            }

            //Remove old chunks/index, then rename the new ones into place.
            for(int i = 0; i <= currentChunk; i++) {
                File old = chunkFile(i);
                if(old.exists()) old.delete();
            }
            File oldIdx = indexFile();
            if(oldIdx.exists()) oldIdx.delete();

            for(int i = 0; i <= newChunk; i++) {
                File tmp = new File(cacheDir, tempPrefix + "-chunk" + i + ".dat");
                if(tmp.exists()) tmp.renameTo(chunkFile(i));
            }

            index.clear();
            index.putAll(newIndex);
            currentChunk = newChunk;
            currentChunkSize = newChunkSize;

            long liveBytes = 0;
            for(Map.Entry<String, Entry> en : newIndex.entrySet()) {
                Entry e = en.getValue();
                appendIndexRecord(en.getKey(), e.chunkNum, e.offset, e.length, false);
                liveBytes += e.length;
            }
            totalBytesOnDisk = liveBytes;
            deadBytes = 0;
        }

        private class Entry {
            final int chunkNum;
            final long offset;
            final int length;

            Entry(int chunkNum, long offset, int length) {
                this.chunkNum = chunkNum;
                this.offset = offset;
                this.length = length;
            }
        }
    }
}
