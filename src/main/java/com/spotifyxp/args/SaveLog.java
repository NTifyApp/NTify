/*
 * Copyright [2024] [Gianluca Beil]
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
package com.spotifyxp.args;

import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

public class SaveLog implements Argument {
    private static File logPath;

    private void generateLogFile(String commands) {
        logPath = new File(commands, new Random().nextInt(9999) + ".log");
        if (logPath.exists()) {
            generateLogFile(commands);
        }
    }

    @Override
    public Runnable runArgument(String commands) {
        return new Runnable() {
            @Override
            public void run() {
                generateLogFile(commands);
                PublicValues.debug = true;
                PrintStream defaultOutStream = System.out;
                PrintStream defaultErrStream = System.err;
                System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
                    @Override
                    public void write(int b) {
                    }
                }) {
                    @Override
                    public void flush() {
                        defaultOutStream.flush();
                    }

                    @Override
                    public void close() {
                        defaultOutStream.close();
                    }

                    @Override
                    public void write(int b) {
                        defaultOutStream.write(b);
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        defaultOutStream.write(b);
                    }

                    @Override
                    public void write(byte[] buf, int off, int len) {
                        defaultOutStream.write(buf, off, len);
                    }

                    @Override
                    public void print(boolean b) {
                        defaultOutStream.print(b);
                    }

                    @Override
                    public void print(char c) {
                        defaultOutStream.print(c);
                    }

                    @Override
                    public void print(int i) {
                        defaultOutStream.print(i);
                    }

                    @Override
                    public void print(long l) {
                        defaultOutStream.print(l);
                    }

                    @Override
                    public void print(float f) {
                        defaultOutStream.print(f);
                    }

                    @Override
                    public void print(double d) {
                        defaultOutStream.print(d);
                    }

                    @Override
                    public void print(char[] s) {
                        defaultOutStream.print(s);
                    }

                    @Override
                    public void print(String s) {
                        defaultOutStream.print(s);
                        FileUtils.appendToFile(logPath.getAbsolutePath(), s);
                    }

                    @Override
                    public void print(Object obj) {
                        defaultOutStream.print(obj);
                    }

                    @Override
                    public void println() {
                        defaultOutStream.println();
                    }

                    @Override
                    public void println(boolean x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(char x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(int x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(long x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(float x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(double x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(char[] x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public void println(String x) {
                        defaultOutStream.println(x);
                        FileUtils.appendToFile(logPath.getAbsolutePath(), x);
                    }

                    @Override
                    public void println(Object x) {
                        defaultOutStream.println(x);
                    }

                    @Override
                    public java.io.PrintStream printf(String format, Object... args) {
                        return defaultOutStream.printf(format, args);
                    }

                    @Override
                    public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) {
                        return defaultOutStream.printf(l, format, args);
                    }

                    @Override
                    public java.io.PrintStream format(String format, Object... args) {
                        return defaultOutStream.format(format, args);
                    }

                    @Override
                    public java.io.PrintStream format(java.util.Locale l, String format, Object... args) {
                        return defaultOutStream.format(l, format, args);
                    }

                    @Override
                    public java.io.PrintStream append(CharSequence csq) {
                        return defaultOutStream.append(csq);
                    }

                    @Override
                    public java.io.PrintStream append(CharSequence csq, int start, int end) {
                        return defaultOutStream.append(csq, start, end);
                    }

                    @Override
                    public java.io.PrintStream append(char c) {
                        return defaultOutStream.append(c);
                    }
                });
                System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
                    @Override
                    public void write(int b) {
                    }
                }) {
                    @Override
                    public void flush() {
                        defaultErrStream.flush();
                    }

                    @Override
                    public void close() {
                        defaultErrStream.close();
                    }

                    @Override
                    public void write(int b) {
                        defaultErrStream.write(b);
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        defaultErrStream.write(b);
                    }

                    @Override
                    public void write(byte[] buf, int off, int len) {
                        defaultErrStream.write(buf, off, len);
                    }

                    @Override
                    public void print(boolean b) {
                        defaultErrStream.print(b);
                    }

                    @Override
                    public void print(char c) {
                        defaultErrStream.print(c);
                    }

                    @Override
                    public void print(int i) {
                        defaultErrStream.print(i);
                    }

                    @Override
                    public void print(long l) {
                        defaultErrStream.print(l);
                    }

                    @Override
                    public void print(float f) {
                        defaultErrStream.print(f);
                    }

                    @Override
                    public void print(double d) {
                        defaultErrStream.print(d);
                    }

                    @Override
                    public void print(char[] s) {
                        defaultErrStream.print(s);
                    }

                    @Override
                    public void print(String s) {
                        defaultErrStream.print(s);
                        FileUtils.appendToFile(logPath.getAbsolutePath(), s);
                    }

                    @Override
                    public void print(Object obj) {
                        defaultErrStream.print(obj);
                    }

                    @Override
                    public void println() {
                        defaultErrStream.println();
                    }

                    @Override
                    public void println(boolean x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(char x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(int x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(long x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(float x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(double x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(char[] x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public void println(String x) {
                        defaultErrStream.println(x);
                        FileUtils.appendToFile(logPath.getAbsolutePath(), x);
                    }

                    @Override
                    public void println(Object x) {
                        defaultErrStream.println(x);
                    }

                    @Override
                    public java.io.PrintStream printf(String format, Object... args) {
                        return defaultErrStream.printf(format, args);
                    }

                    @Override
                    public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) {
                        return defaultErrStream.printf(l, format, args);
                    }

                    @Override
                    public java.io.PrintStream format(String format, Object... args) {
                        return defaultErrStream.format(format, args);
                    }

                    @Override
                    public java.io.PrintStream format(java.util.Locale l, String format, Object... args) {
                        return defaultErrStream.printf(l, format, args);
                    }

                    @Override
                    public java.io.PrintStream append(CharSequence csq) {
                        return defaultErrStream.append(csq);
                    }

                    @Override
                    public java.io.PrintStream append(CharSequence csq, int start, int end) {
                        return defaultErrStream.append(csq, start, end);
                    }

                    @Override
                    public java.io.PrintStream append(char c) {
                        return defaultErrStream.append(c);
                    }
                });
            }
        };
    }

    @Override
    public String getName() {
        return "savelog";
    }

    @Override
    public String getDescription() {
        return "Save logs in the specified location";
    }

    @Override
    public boolean hasParameter() {
        return true;
    }
}
