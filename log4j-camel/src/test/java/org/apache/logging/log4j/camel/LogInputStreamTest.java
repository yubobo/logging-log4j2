/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.camel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

/**
 * @author msicker
 * @version 1.0.0
 */
public class LogInputStreamTest extends ContextTestSupport {

    private static final String TEST_BODY = "Hello World";

    public void testA() throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:a");
        mock.expectedBodiesReceived(TEST_BODY);
        final InputStream is = new ByteArrayInputStream(TEST_BODY.getBytes());
        template.sendBody("direct:a", is);
        assertMockEndpointsSatisfied();
    }

    public void testB() throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:b");
        mock.expectedMessageCount(1);
        mock.message(0).body().convertToString().isEqualTo(TEST_BODY);
        final InputStream is = new ByteArrayInputStream(TEST_BODY.getBytes());
        template.sendBody("direct:b", is);
        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:a")
                        .to("log4j:a")
                        .to("mock:a");

                from("direct:b")
                        .to("log4j:b")
                        .to("mock:b");
            }
        };
    }
}
