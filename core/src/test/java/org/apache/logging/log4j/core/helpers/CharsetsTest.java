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

package org.apache.logging.log4j.core.helpers;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

import org.junit.Test;

public class CharsetsTest {

    @Test
    public void testReturnDefaultIfNameIsNull() {
        Charset actual = Charsets.getSupportedCharset(null);
        assertSame(Charset.defaultCharset(), actual);
    }

    @Test
    public void testReturnDefaultIfNameIsUnsupported() {
        Charset actual = Charsets.getSupportedCharset("INeedMoreSupport");
        assertSame(Charset.defaultCharset(), actual);
    }

    @Test(expected = IllegalCharsetNameException.class)
    public void testThrowsExceptionIfNameIsIllegal() {
        Charset actual = Charsets.getSupportedCharset("spaces not allowed");
        assertSame(Charset.defaultCharset(), actual);
    }

    @Test
    public void testReturnRequestedCharsetIfSupported() {
        Charset actual1 = Charsets.getSupportedCharset("UTF-8");
        assertSame(Charset.forName("UTF-8"), actual1);

        Charset actual2 = Charsets.getSupportedCharset("ISO-8859-1");
        assertSame(Charset.forName("ISO-8859-1"), actual2);

        // This part of the test is NOT portable across all Java platforms.
        // There is no guarantee that KOI8-R is on any given platform
        if (Charset.isSupported("KOI8-R")) {
            Charset actual3 = Charsets.getSupportedCharset("KOI8-R");
            assertSame(Charset.forName("KOI8-R"), actual3);
        }
    }

}
