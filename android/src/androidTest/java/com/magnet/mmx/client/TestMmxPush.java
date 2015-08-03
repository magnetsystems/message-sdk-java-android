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

import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.gson.JsonObject;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.GCMPayload;
import com.magnet.mmx.protocol.PushMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class TestMmxPush extends InstrumentationTestCase {

  public static final String DO_SOMETHING_IMMEDIATELY = "doSomething immediately";
  public static final String CALLBACK_URL = "http://preview.magnet.com:5221/mmxmgmt/v1/pushreply?pushmessageid=261ty171765";
  public static final String IMAGE_URL = "http://i2.cdn.turner.com/cnnnext/dam/assets/150220074741-two-and-a-half-men-season-finale-kutcher-lorre-cryer-large-169.jpg";
  public static final String RAHULS_MAC_BOOK_PRO_LOCAL = "Rahuls-MacBook-Pro.local";
  public static final String ID_VALUE = "8dbef120271b1b4a974acaa7e32dcc66";
  public static final String SELECTOR_FETCH_MESSAGE = "@selector(fetchMessage:)";
  public static final String ORDER_STATUS = "Order status";
  public static final String ORDER_STRING = "Your order is ready for pickup at 465 E El Camino Real, Palo Alto, CA";
  public static final String CHIME_AIFF = "chime.aiff";
  public static final String SIMPLE_ICON = "icon.png";
  private static String TEST_CUSTOM_STRING;

  private static String TEST_RETRIEVE_STRING = "mmx:w:retrieve" + "\r\n{\n" +
      "    \"_mmx\" : {\n" +
      "        \"ty\" : \"mmx:w:retrieve\",\n" +
      "    }\n" +
      "}";

  private static String TEST_PING_STRING = "mmx:w:ping\r\n{\n" +
      "    \"_mmx\" : {\n" +
      "        \"ty\" : \"mmx:w:ping\",\n" +
      "        \"id\" : \"261ty171765\",\n" +
      "        \"cu\":\"http://preview.magnet.com:5221/mmxmgmt/v1/pushreply?pushmessageid=261ty171765\"   \n" +
      "    }\n" +
      "}";

  private static String TEST_PUSH_STRING = "mmx:p:notify\r\n{\n" +
      "    \"title\" : \"Order status\",\n" +
      "    \"body\" : \"Your order is ready for pickup at 465 E El Camino Real, Palo Alto, CA\",\n" +
      "    \"icon\" : \"order-updated.png\",\n" +
      "    \"sound\" :\"chime.aiff\",\n" +
      "    \"_mmx\" : {\n" +
      "        \"ty\" : \"mmx:p\",\n" +
      "        \"id\" : \"261ty171890\",\n" +
      "        \"ct\" : \"NotificationBean\"\n" +
      "        \"cu\":\"http://preview.magnet.com:5221/mmxmgmt/v1/pushreply?pushmessageid=261ty171890\",\n" +
      "        \"custom\" : {\"action\":\"dosomething\", \"url\":\"http://preview.magnet.com\", \"jsontext\":\"\\{\\\"from\\\":\\\"Rahuls-MacBook-Pro.local\\\",\\\"id\\\":\\\"8dbef120271b1b4a974acaa7e32dcc66\\\",\\\"action\\\":\\\"@selector(fetchMessage:)\"},   \n" +
      "    }\n" +
      "}";

//  private final static GCMPayload TEST_PING_PAYLOAD = new GCMPayload().setMmx(
//      (Map<String, ? super Object>) (new HashMap<String, Object>().put(Constants.PAYLOAD_TYPE_KEY, "mmx:w:" + Constants.PingPongCommand.ping.name()) ));

  // TODO replace with proper constants used by MMX server
  private final static String PING_HEADER = "mmx:w:ping";
  private final static String RETRIEVE_HEADER = "mmx:w:retrieve";
  private final static String NOTIFY_HEADER = "mmx:p:notify";

  @Override
  public void setUp() throws JSONException {

    JSONObject mmxDictionary = new JSONObject();
    JSONObject mmxBlock = new JSONObject();
    mmxBlock.put(Constants.PAYLOAD_MMX_KEY, mmxDictionary);

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(RETRIEVE_HEADER).append("\r\n");
    mmxDictionary.put(Constants.PAYLOAD_TYPE_KEY, RETRIEVE_HEADER);
    stringBuilder.append(mmxBlock.toString());
    TEST_RETRIEVE_STRING = stringBuilder.toString();

    mmxDictionary.put(Constants.PAYLOAD_ID_KEY, ID_VALUE);
    mmxDictionary.put(Constants.PAYLOAD_CALLBACK_URL_KEY, CALLBACK_URL);

    StringBuilder pingBuilder = new StringBuilder();
    pingBuilder.append(PING_HEADER).append("\r\n");
    mmxDictionary.put(Constants.PAYLOAD_TYPE_KEY, PING_HEADER);
    pingBuilder.append(mmxBlock.toString());
    TEST_PING_STRING = pingBuilder.toString();

    JSONObject customObject = new JSONObject();
    customObject.put("action", DO_SOMETHING_IMMEDIATELY);
    customObject.put("priority", (int) 1);
    customObject.put("URL", IMAGE_URL);

    // "jsontext"
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("from", RAHULS_MAC_BOOK_PRO_LOCAL);
    jsonObject.put("id", ID_VALUE);
    jsonObject.put("action", SELECTOR_FETCH_MESSAGE);

    customObject.put("jsontext", jsonObject.toString());

    mmxDictionary.put(Constants.PAYLOAD_CUSTOM_KEY, customObject);
    mmxBlock.put(Constants.PAYLOAD_PUSH_TITLE, ORDER_STATUS);
    mmxBlock.put(Constants.PAYLOAD_PUSH_BODY, ORDER_STRING);
    mmxBlock.put(Constants.PAYLOAD_PUSH_SOUND, CHIME_AIFF);
    mmxBlock.put(Constants.PAYLOAD_PUSH_ICON, SIMPLE_ICON);

    StringBuilder notifyBuilder = new StringBuilder();
    notifyBuilder.append(NOTIFY_HEADER).append("\r\n");
    mmxDictionary.put(Constants.PAYLOAD_TYPE_KEY, NOTIFY_HEADER);
    notifyBuilder.append(mmxBlock.toString());
    TEST_PUSH_STRING = notifyBuilder.toString();

    TEST_CUSTOM_STRING = customObject.toString();

  }
  @SmallTest
  public void testPushMessageParseWakeup() {
    PushMessage pushMessage = PushMessage.decode(TEST_RETRIEVE_STRING, null);
    assertTrue(pushMessage.getAction() == PushMessage.Action.WAKEUP);
    assertTrue(("mmx:w:" + Constants.PingPongCommand.retrieve.name()).equals(
        ((GCMPayload) pushMessage.getPayload()).getMmx().get(Constants.PAYLOAD_TYPE_KEY)));

  }
  @SmallTest
  public void testPushMessageParsePing() {
    PushMessage pushMessage = PushMessage.decode(TEST_PING_STRING, null);
    assertTrue(pushMessage.getAction() == PushMessage.Action.WAKEUP);
    GCMPayload gcmPayload = (GCMPayload) pushMessage.getPayload();
    assertTrue(("mmx:w:" + Constants.PingPongCommand.ping.name()).equals(
        gcmPayload.getMmx().get(Constants.PAYLOAD_TYPE_KEY)));

    String idString = (String) gcmPayload.getMmx().get(Constants.PAYLOAD_ID_KEY);
    assertEquals(ID_VALUE, idString);

    String urlString = (String) gcmPayload.getMmx().get(Constants.PAYLOAD_CALLBACK_URL_KEY);
    assertEquals(CALLBACK_URL, urlString);

  }
  @SmallTest
  public void testPushMessageParsePush() throws JSONException {
    PushMessage pushMessage = PushMessage.decode(TEST_PUSH_STRING, null);
    assertTrue(pushMessage.getAction() == PushMessage.Action.PUSH);
    GCMPayload gcmPayload = (GCMPayload) pushMessage.getPayload();
    assertTrue(("mmx:p:" + Constants.PingPongCommand.notify.name()).equals(
        gcmPayload.getMmx().get(Constants.PAYLOAD_TYPE_KEY)));

    String idString = (String) gcmPayload.getMmx().get(Constants.PAYLOAD_ID_KEY);
    assertEquals(ID_VALUE, idString);

    String urlString = (String) gcmPayload.getMmx().get(Constants.PAYLOAD_CALLBACK_URL_KEY);
    assertEquals(CALLBACK_URL, urlString);
    assertEquals(CHIME_AIFF, gcmPayload.getSound());
    assertEquals(ORDER_STRING, gcmPayload.getBody());
    assertEquals(ORDER_STATUS, gcmPayload.getTitle());
    assertEquals(SIMPLE_ICON, gcmPayload.getIcon());

    Map<String, Object> customString = (Map<String, Object>) gcmPayload.getMmx().get(Constants.PAYLOAD_CUSTOM_KEY);
    JSONObject jsonObject = new JSONObject(customString);
    assertEquals(DO_SOMETHING_IMMEDIATELY, jsonObject.getString("action"));
    assertEquals(IMAGE_URL, jsonObject.get("URL"));
    assertEquals(1, jsonObject.getInt("priority"));
    assertTrue(jsonObject.has("jsontext"));
    String jsonText = jsonObject.getString("jsontext");
    JSONObject json = new JSONObject(jsonText);
    assertEquals(RAHULS_MAC_BOOK_PRO_LOCAL, json.getString("from"));
    assertEquals(ID_VALUE, json.getString("id"));
    assertEquals(SELECTOR_FETCH_MESSAGE, json.getString("action"));

    // now format it as an Intent
    Intent pushIntent = MMXWakeupIntentService.buildPushIntent(gcmPayload);
    assertEquals(MMXClient.ACTION_PUSH_RECEIVED, pushIntent.getAction());
    assertEquals(ID_VALUE, pushIntent.getStringExtra(MMXClient.EXTRA_PUSH_ID));
    assertEquals(ORDER_STRING, pushIntent.getStringExtra(MMXClient.EXTRA_PUSH_BODY));
    assertEquals(ORDER_STATUS, pushIntent.getStringExtra(MMXClient.EXTRA_PUSH_TITLE));
    assertEquals(CHIME_AIFF, pushIntent.getStringExtra(MMXClient.EXTRA_PUSH_SOUND));
    assertEquals(SIMPLE_ICON, pushIntent.getStringExtra(MMXClient.EXTRA_PUSH_ICON));
    String jsonString = pushIntent.getStringExtra(MMXClient.EXTRA_PUSH_CUSTOM_JSON);
    JSONObject jsonObject2 = new JSONObject(jsonString);

    assertEquals(jsonObject.length(), jsonObject2.length());


  }
}
