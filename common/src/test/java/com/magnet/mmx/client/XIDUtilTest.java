/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package com.magnet.mmx.client;

import com.magnet.mmx.util.XIDUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class XIDUtilTest {

  @BeforeClass
  public static void setup() throws Exception {
  }

  @Test
  public void testFullXID() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe%appId@domain/resource");
    assertEquals("john.doe", tokens[0]);
    assertEquals("appId", tokens[1]);
    assertEquals("domain", tokens[2]);
    assertEquals("resource", tokens[3]);
  }

  @Test
  public void testFullJID() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe@domain/resource");
    assertEquals("john.doe", tokens[0]);
    assertNull(tokens[1]);
    assertEquals("domain", tokens[2]);
    assertEquals("resource", tokens[3]);
  }

  @Test
  public void testBaredXID() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe%appId@domain");
    assertEquals("john.doe", tokens[0]);
    assertEquals("appId", tokens[1]);
    assertEquals("domain", tokens[2]);
    assertNull(tokens[3]);
  }

  @Test
  public void testBaredJID() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe@domain");
    assertEquals("john.doe", tokens[0]);
    assertNull(tokens[1]);
    assertEquals("domain", tokens[2]);
    assertNull(tokens[3]);
  }

  @Test
  public void testXIDNode() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe%appId");
    assertEquals("john.doe", tokens[0]);
    assertEquals("appId", tokens[1]);
    assertNull(tokens[2]);
    assertNull(tokens[3]);
  }

  @Test
  public void testJIDNode() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe");
    assertEquals("john.doe", tokens[0]);
    assertNull(tokens[1]);
    assertNull(tokens[2]);
    assertNull(tokens[3]);
  }

  @Test
  public void testBaredXIDEmail() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe@magnet.com%appId@domain");
    assertEquals("john.doe@magnet.com", tokens[0]);
    assertEquals("appId", tokens[1]);
    assertEquals("domain", tokens[2]);
    assertNull(tokens[3]);
  }

  @Test
  public void testBaredJIDEmail() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe@magnet.com@domain");
    assertEquals("john.doe@magnet.com", tokens[0]);
    assertNull(tokens[1]);
    assertEquals("domain", tokens[2]);
    assertNull(tokens[3]);
  }

  @Test
  public void testFullXIDEmail() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe@magnet.com%appId@domain/resource");
    assertEquals("john.doe@magnet.com", tokens[0]);
    assertEquals("appId", tokens[1]);
    assertEquals("domain", tokens[2]);
    assertEquals("resource", tokens[3]);
  }

  @Test
  public void testFullJIDEmail() {
    String [] tokens;
    tokens = XIDUtil.parseXID("john.doe@magnet.com@domain/resource");
    assertEquals("john.doe@magnet.com", tokens[0]);
    assertNull(tokens[1]);
    assertEquals("domain", tokens[2]);
    assertEquals("resource", tokens[3]);
  }
}
