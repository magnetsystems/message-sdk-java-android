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
package com.magnet.mmx.restAssured.utils;

import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUtils {
  public static String HTTPBaseUrl = "http://localhost";
  public static String HTTPSBaseUrl = "https://localhost";

  public static String user = "admin";
  public static String pass = "admin";

  public static String appId = "n9ci8ffoitr";
  public static String appKey = "55cff1f0-be33-4ba5-b76f-231954ab369f";
  public static String appOwner = "f7758430-e198-11e4-bce8-617dbe9255aa";

  public static String HEADER_APPID = "X-mmx-app-id";
  public static String HEADER_APPKEY = "X-mmx-api-key";

  public static String SIZE_PARAM = "size";
  public static String OFFSET_PARAM = "offset";


  public static String JSON = "application/json";
  public static Map<String, String> mmxApiHeaders = new HashMap<String, String>();
  public static Map<String, String> mmxApiBadHeaders = new HashMap<String, String>();

  static {
    mmxApiHeaders.put(HEADER_APPID, appId);
    mmxApiHeaders.put(HEADER_APPKEY, appKey);
  }

  public static List<Header> mmxBadApiHeaders = new ArrayList<Header>();

  static {
    mmxApiBadHeaders.put(HEADER_APPID, appId);
    mmxApiBadHeaders.put(HEADER_APPKEY, "thisisbadkey");
  }

  public static Headers toHeaders(Map<String, String> mapItems) {
    List<Header> headers = new ArrayList<Header>();
    for (String key : mapItems.keySet()) {
      Header header = new Header(key, mapItems.get(key));
      headers.add(header);
    }
    return new Headers(headers);
  }

  public static String DEVICE_ID_1 = "test-client1-devId-1429039847558";
  public static String DEVICE_ID_2 = "test-client2-devId-1429039847558";

  public static String USER_ID_1 = "mmxclienttest1";
  public static String USER_ID_2 = "mmxclienttest2";
}

