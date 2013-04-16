/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.async.appender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;

public class FastRollingFileManager extends RollingFileManager {
    private static FastRollingFileManagerFactory factory = new FastRollingFileManagerFactory();

    private final boolean isImmediateFlush;
    private RandomAccessFile randomAccessFile;
    private final ByteBuffer buffer;
    private ThreadLocal<Boolean> isEndOfBatch = new ThreadLocal<Boolean>();

    public FastRollingFileManager(RandomAccessFile raf, String fileName,
            String pattern, OutputStream os, boolean append,
            boolean immediateFlush, long size, long time, TriggeringPolicy policy,
            RolloverStrategy strategy, String advertiseURI) {
        super(fileName, pattern, os, append, size, time, policy, strategy, advertiseURI);
        this.isImmediateFlush = immediateFlush;
        this.randomAccessFile = raf;
        isEndOfBatch.set(Boolean.FALSE);
        buffer = ByteBuffer.allocate(256 * 1024); // TODO make configurable?
    }

    public static FastRollingFileManager getFastRollingFileManager(
            String fileName, String filePattern, boolean isAppend,
            boolean immediateFlush, TriggeringPolicy policy,
            RolloverStrategy strategy, String advertiseURI) {
        return (FastRollingFileManager) getManager(fileName, new FactoryData(
                filePattern, isAppend, immediateFlush, policy, strategy, 
                advertiseURI), factory);
    }

    public Boolean isEndOfBatch() {
        return isEndOfBatch.get();
    }

    public void setEndOfBatch(boolean isEndOfBatch) {
        this.isEndOfBatch.set(Boolean.valueOf(isEndOfBatch));
    }

    @Override
    protected synchronized void write(byte[] bytes, int offset, int length) {
        super.write(bytes, offset, length); // writes to dummy output stream

        if (length > buffer.remaining()) {
            flush();
        }
        buffer.put(bytes, offset, length);
        if (isImmediateFlush || isEndOfBatch.get() == Boolean.TRUE) {
            flush();
        }
    }

    @Override
    protected void createFileAfterRollover() throws IOException {
        this.randomAccessFile = new RandomAccessFile(getFileName(), "rw");
        if (isAppend()) {
            randomAccessFile.seek(randomAccessFile.length());
        }
    }

    @Override
    public void flush() {
        buffer.flip();
        try {
            randomAccessFile.write(buffer.array(), 0, buffer.limit());
        } catch (IOException ex) {
            String msg = "Error writing to RandomAccessFile " + getName();
            throw new AppenderRuntimeException(msg, ex);
        }
        buffer.clear();
    }
    
    @Override
    public void close() {
    	flush();
        try {
        	randomAccessFile.close();
        } catch (final IOException ex) {
            LOGGER.error("Unable to close RandomAccessFile " + getName() + ". " + ex);
        }
    }

    /**
     * Factory to create a FastRollingFileManager.
     */
    private static class FastRollingFileManagerFactory implements
            ManagerFactory<FastRollingFileManager, FactoryData> {

        /**
         * Create the FastRollingFileManager.
         * 
         * @param name
         *            The name of the entity to manage.
         * @param data
         *            The data required to create the entity.
         * @return a RollingFileManager.
         */
        public FastRollingFileManager createManager(String name,
                FactoryData data) {
            File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            if (!data.append) {
                file.delete();
            }
            long size = data.append ? file.length() : 0;
            long time = file.lastModified();

            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(name, "rw");
                return new FastRollingFileManager(raf, name, data.pattern,
                        new DummyOutputStream(), data.append,
                        data.immediateFlush, size, time, data.policy,
                        data.strategy, data.advertiseURI);
            } catch (FileNotFoundException ex) {
                LOGGER.error("FastRollingFileManager (" + name + ") " + ex);
            }
            return null;
        }
    }

    private static class DummyOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private final String pattern;
        private final boolean append;
        private final boolean immediateFlush;
        private final TriggeringPolicy policy;
        private final RolloverStrategy strategy;
        private final String advertiseURI;

        /**
         * Create the data for the factory.
         * 
         * @param pattern
         *            The pattern.
         * @param append
         *            The append flag.
         * @param immediateFlush
         */
        public FactoryData(String pattern, boolean append,
                boolean immediateFlush, TriggeringPolicy policy,
                RolloverStrategy strategy, String advertiseURI) {
            this.pattern = pattern;
            this.append = append;
            this.immediateFlush = immediateFlush;
            this.policy = policy;
            this.strategy = strategy;
            this.advertiseURI = advertiseURI;
        }
    }

}