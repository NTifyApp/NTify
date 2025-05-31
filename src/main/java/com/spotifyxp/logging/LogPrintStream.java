/*
 * Copyright [2025] [Gianluca Beil]
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
package com.spotifyxp.logging;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import org.apache.commons.io.IOUtils;

import javax.xml.crypto.Data;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogPrintStream {
    private final PrintStream internalPrintStream;
    private boolean enableLogging = false;
    private String currentLogFile = "";
    private FileWriter writer = null;

    public PrintStream asPrintStream() {
        return internalPrintStream;
    }

    public void setLogging(boolean logging) {
        this.enableLogging = logging;
        new File("logs", currentLogFile).delete();
    }

    public void checkLogFiles() {
        File[] foundLogs = new File(PublicValues.fileslocation, "logs").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ENGLISH).endsWith(".log");
            }
        });
        if(foundLogs != null && foundLogs.length > PublicValues.config.getInt(ConfigValues.logging_maxkept.name)) {
            Arrays.sort(foundLogs, Comparator.comparing(f -> LocalDateTime.parse(f.getName().replace(".log", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))));
            for(int i = 0; i < (foundLogs.length - PublicValues.config.getInt(ConfigValues.logging_maxkept.name) + 1); i++) {
                if(!foundLogs[i].delete()) {
                    ConsoleLogging.error("Failed to delete log: " + foundLogs[i].getName());
                }
            }
        }
        if(enableLogging) {
            try {
                File logs = new File(PublicValues.fileslocation, "logs");
                if (!logs.exists()) {
                    if (!logs.mkdir()) {
                        throw new RuntimeException("Failed to create logs directory");
                    }
                }
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String logFileName = new File(formatter.format(new Date()) + ".log").getName();
                currentLogFile = logFileName;
                if (!new File(logs, logFileName).createNewFile()) {
                    throw new RuntimeException("Failed to create log file");
                }
                writer = new FileWriter(new File(logs, logFileName));
            }catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
        }
    }

    public LogPrintStream(boolean enablePrint, PrintStream originalPrintStream) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread("logprintstream-shutdown-hook") {
            @Override
            public void run() {
                if(enableLogging) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        internalPrintStream = new java.io.PrintStream(new java.io.OutputStream() {
            @Override public void write(int b) {}
        }) {
            @Override
            public void flush() {
                if(enableLogging) {
                    try {
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.flush();
                }
            }

            @Override
            public void close() {
                if(enableLogging) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.close();
                }
            }

            @Override
            public void write(int b) {
                if(enableLogging) {
                    try {
                        writer.write(b);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.write(b);
                }
            }

            @Override
            public void write(byte[] b) {
                if(enableLogging) {
                    try {
                        writer.write("Tried to write bytes! String representation: " + IOUtils.toString(b, Charset.defaultCharset().toString()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    try {
                        originalPrintStream.write(b);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void write(byte[] buf, int off, int len) {
                if(enableLogging) {
                    try {
                        writer.write("Tried to write bytes with len '" + len + " and offset '" + off + "' ! String representation: " + IOUtils.toString(buf, Charset.defaultCharset().toString()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.write(buf, off, len);
                }
            }

            @Override
            public void print(boolean b) {
                if(enableLogging) {
                    try {
                        writer.write(String.valueOf(b));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(b);
                }
            }

            @Override
            public void print(char c) {
                if(enableLogging) {
                    try {
                        writer.write(c);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(c);
                }
            }

            @Override
            public void print(int i) {
                if(enableLogging) {
                    try {
                        writer.write(i);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(i);
                }
            }

            @Override
            public void print(long l) {
                if(enableLogging) {
                    try {
                        writer.write(String.valueOf(l));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(l);
                }
            }

            @Override
            public void print(float f) {
                if(enableLogging) {
                    try {
                        writer.write(String.valueOf(f));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(f);
                }
            }

            @Override
            public void print(double d) {
                if(enableLogging) {
                    try {
                        writer.write(String.valueOf(d));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(d);
                }
            }

            @Override
            public void print(char[] s) {
                if(enableLogging) {
                    try {
                        writer.write(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(s);
                }
            }

            @Override
            public void print(String s) {
                if(enableLogging) {
                    try {
                        writer.write(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(s);
                }
            }

            @Override
            public void print(Object obj) {
                if(enableLogging) {
                    try {
                        writer.write("Tried to write object! toString(): " + obj.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(obj);
                }
            }

            @Override
            public void println() {
                originalPrintStream.println();
            }

            @Override
            public void println(boolean x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(char x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(int x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(long x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(float x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(double x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(char[] x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(String x) {
                if(enableLogging) {
                    try {
                        writer.write(x + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public void println(Object x) {
                if(enableLogging) {
                    try {
                        writer.write("Tried to write Object! toString(): " + x.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(enablePrint) {
                    originalPrintStream.println(x);
                }
            }

            @Override
            public java.io.PrintStream printf(String format, Object... args) {
                return originalPrintStream.printf(format, args);
            }

            @Override
            public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) {
                return originalPrintStream.printf(l, format, args);
            }

            @Override
            public java.io.PrintStream format(String format, Object... args) {
                return originalPrintStream.format(format, args);
            }

            @Override
            public java.io.PrintStream format(java.util.Locale l, String format, Object... args) {
                return originalPrintStream.format(l, format, args);
            }

            @Override
            public java.io.PrintStream append(CharSequence csq) {
                if(enableLogging) {
                    try {
                        writer.append(csq);
                        return this;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                originalPrintStream.append(csq);
                return originalPrintStream;
            }

            @Override
            public java.io.PrintStream append(CharSequence csq, int start, int end) {
                if(enableLogging) {
                    try {
                        writer.append(csq, start, end);
                        return this;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                originalPrintStream.append(csq, start, end);
                return originalPrintStream;
            }

            @Override
            public java.io.PrintStream append(char c) {
                if(enableLogging) {
                    try {
                        writer.append(c);
                        return this;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                originalPrintStream.append(c);
                return originalPrintStream;
            }
        };
    }
}
