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
package org.apache.logging.log4j.core.appender.db;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Test;

public class AbstractDatabaseAppenderTest {
    private static abstract class LocalAbstractDatabaseAppender extends
            AbstractDatabaseAppender<LocalAbstractDatabaseManager> {
        public LocalAbstractDatabaseAppender(final String name, final Filter filter, final boolean handleException,
                final LocalAbstractDatabaseManager manager) {
            super(name, filter, handleException, manager);
        }
    }

    private static abstract class LocalAbstractDatabaseManager extends AbstractDatabaseManager {
        public LocalAbstractDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }
    }

    private LocalAbstractDatabaseAppender appender;

    private LocalAbstractDatabaseManager manager;

    public void setUp(final String name) {
        this.manager = createMockBuilder(LocalAbstractDatabaseManager.class).withConstructor(String.class, int.class)
                .withArgs(name, 0).addMockedMethod("release").createStrictMock();

        this.appender = createMockBuilder(LocalAbstractDatabaseAppender.class)
                .withConstructor(String.class, Filter.class, boolean.class, LocalAbstractDatabaseManager.class)
                .withArgs(name, null, true, this.manager).createStrictMock();
    }

    @After
    public void tearDown() {
        verify(this.manager, this.appender);
    }

    @Test
    public void testAppend() {
        this.setUp("name");

        final LogEvent event1 = createStrictMock(LogEvent.class);
        final LogEvent event2 = createStrictMock(LogEvent.class);

        this.manager.writeInternal(same(event1));
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.append(event1);

        verify(this.manager, this.appender);
        reset(this.manager, this.appender);
        this.manager.writeInternal(same(event2));
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.append(event2);
    }

    @Test
    public void testNameAndGetLayout01() {
        this.setUp("testName01");

        replay(this.manager, this.appender);

        assertEquals("The name is not correct.", "testName01", this.appender.getName());
        assertNull("The layout should always be null.", this.appender.getLayout());
    }

    @Test
    public void testNameAndGetLayout02() {
        this.setUp("anotherName02");

        replay(this.manager, this.appender);

        assertEquals("The name is not correct.", "anotherName02", this.appender.getName());
        assertNull("The layout should always be null.", this.appender.getLayout());
    }

    @Test
    public void testReplaceManager() {
        this.setUp("name");

        replay(this.manager, this.appender);

        final LocalAbstractDatabaseManager manager = this.appender.getManager();

        assertSame("The manager should be the same.", this.manager, manager);

        verify(this.manager, this.appender);
        reset(this.manager, this.appender);
        this.manager.release();
        expectLastCall();
        final LocalAbstractDatabaseManager newManager = createMockBuilder(LocalAbstractDatabaseManager.class)
                .withConstructor(String.class, int.class).withArgs("name", 0).addMockedMethod("release")
                .createStrictMock();
        newManager.connectInternal();
        expectLastCall();
        replay(this.manager, this.appender, newManager);

        this.appender.replaceManager(newManager);

        verify(this.manager, this.appender, newManager);
        reset(this.manager, this.appender, newManager);
        newManager.release();
        expectLastCall();
        replay(this.manager, this.appender, newManager);

        this.appender.stop();

        verify(newManager);
    }

    @Test
    public void testStartAndStop() {
        this.setUp("name");

        this.manager.connectInternal();
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.start();

        verify(this.manager, this.appender);
        reset(this.manager, this.appender);
        this.manager.release();
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.stop();
    }
}