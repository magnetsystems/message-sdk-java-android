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

import com.magnet.mmx.util.AppHelper;
import com.magnet.mmx.util.CryptoUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.security.SignatureException;

import static org.junit.Assert.assertEquals;


public class GuestLoginTest {
  private static XMPPTCPConnection connection;
  private static final String TEST_APPID= "i1bd84ljnlq";
  private static final String TEST_APIKEY = "62c9ee83-ca26-4887-8566-d3384fae3dde";
  private static final String TEST_SECRET = "fdsafdafd";
  private static final String EXPECTED_PASSWORD = "OXHTAGZAyG1j6lYnLg1tKKPFClI=";

  @BeforeClass
  public static void setup() throws Exception {
//    ConnectionConfiguration config = new ConnectionConfiguration("127.0.0.1", 5222);
//    config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
//    connection = new XMPPTCPConnection(config);
//    connection.connect();
//    connection.login("admin", "test", "unittest");

  }
  @Ignore
  @Test
  public void testBasicLogin() throws IOException, SmackException, XMPPException, SignatureException {
    String hmacPassword = CryptoUtil.generateHmacSha1(TEST_APIKEY, TEST_SECRET);
    assertEquals(EXPECTED_PASSWORD, hmacPassword);
    //connection.login("admin", "test", "unittest");
    //connection.disconnect();
    //connection.connect();
    String guestUserId =  AppHelper.generateUser(null, TEST_APPID, TEST_APPID);
    connection.login(guestUserId, hmacPassword, "unittest");// Log into the server
    connection.disconnect();

  }
}
