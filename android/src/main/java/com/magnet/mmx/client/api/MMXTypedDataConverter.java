/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import com.google.gson.Gson;

public class MMXTypedDataConverter {
  private static Gson gson = new Gson();

  public static String marshall(Object object) {
    return gson.toJson(object);
  }

  public static Object unMarshall(String string, Class clazz) {
    return gson.fromJson(string, clazz);
  }
}
