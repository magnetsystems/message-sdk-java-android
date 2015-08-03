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

import com.magnet.mmx.util.Converter;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 * Test converter util.
 */
public class ConverterTest {
  @BeforeClass
  public static void setup() throws Exception {
  }

  @Test
  public void testGenReceiptId() {
    String msgId = "rkYuhFSiQPWNVKejPAoYqQ-2";
    String jid = "user1%zbi2mgw0v2@magnet-linux/computer-2";
    String token = msgId+'#'+jid;
    String receiptId = Converter.Scrambler.convert(token).toString();
    assertNotEquals(token, receiptId);
    //System.out.println("token="+token+", receiptId="+receiptId);

    String token1 = Converter.Scrambler.convert(receiptId).toString(); 
    assertEquals(token, token1);
    String[] tokens = token1.split("#");
    assertEquals(msgId, tokens[0]);
    assertEquals(jid, tokens[1]);
  }

  @Test
  public void testGenReceiptId2() {
    String msgId = "dd4d8b36-d328-46f5-b0c6-07bc9307bb6c-50000";
    String jid = "firstname2.lastname2@52.143.201.73/6505551212";
    String token = msgId+'#'+jid;
    String receiptId = Converter.Scrambler.convert(token).toString();
    assertNotEquals(token, receiptId);
    //System.out.println("token="+token+", receiptId="+receiptId);

    String token1 = Converter.Scrambler.convert(receiptId).toString(); 
    assertEquals(token, token1);
    String[] tokens = token1.split("#");
    assertEquals(msgId, tokens[0]);
    assertEquals(jid, tokens[1]);
  }
}
