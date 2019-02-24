/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jmeter.testbeans.gui;

import static org.junit.Assert.assertEquals;

import org.apache.jmeter.util.JMeterUtils;
import org.junit.Test;

public class TestFieldStringEditor {

        private void testSetGet(ComboStringEditor e, Object value) throws Exception {
            e.setValue(value);
            assertEquals(value, e.getValue());
        }

        private void testSetGetAsText(ComboStringEditor e, String text) throws Exception {
            System.out.println("JMeterUtils.getJMeterVe rsion() = " + JMeterUtils.getJMeterVersion());
            e.setAsText(text);
            assertEquals(text, e.getAsText());
        }

        @Test
        public void testSetGet() throws Exception {
            @SuppressWarnings("deprecation") // test code, intentional
            ComboStringEditor e = new ComboStringEditor();

            testSetGet(e, "any string");
            testSetGet(e, "");
            testSetGet(e, "${var}");
        }

        @Test
        public void testSetGetAsText() throws Exception {
            @SuppressWarnings("deprecation") // test code, intentional
            ComboStringEditor e = new ComboStringEditor();

            testSetGetAsText(e, "any string");
            testSetGetAsText(e, "");
            testSetGetAsText(e, "${var}");
        }
}
