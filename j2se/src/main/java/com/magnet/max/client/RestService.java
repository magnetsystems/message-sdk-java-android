/*   Copyright (c) 2016 Magnet Systems, Inc.  All Rights Reserved.
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
package com.magnet.max.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by mmicevic on 2/29/16.
 *
 */
public class RestService<T> {

  private final String baseUrl;

  public RestService(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  protected T getService(final String mediaType,
      final Map<String, String> headers, Class<T> cl) {

    OkHttpClient httpClient = new OkHttpClient();
    httpClient.interceptors().add(new Interceptor() {
      @Override
      public com.squareup.okhttp.Response intercept(Chain chain)
          throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();
        builder.header("Content-type", mediaType);
        if (headers != null) {
          for (String header : headers.keySet()) {
            builder.header(header, headers.get(header));
          }
        }
        builder.method(original.method(), original.body());
        Request request = builder.build();
        return chain.proceed(request);
      }
    });

    Gson gson = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(Date.class, new DateDeserializer())
        .create();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(httpClient)
        .build();

    return retrofit.create(cl);
  }

  protected void checkResponseForErrors(Response<?> response)
      throws MaxServiceException, IOException {

    if (response.code() != 200 && response.code() != 201) {
      String err = getErrorMessage(response);
      throw new MaxServiceException(response.code(), response.message() + ";"
          + (err == null ? "" : " err: " + err));
    }
  }

  private String getErrorMessage(Response<?> response) throws IOException {

    String msg = null;
    if (response != null && response.errorBody() != null) {
      msg = response.errorBody().string();
    }
    return msg;
  }

  private static class DateDeserializer implements JsonDeserializer<Date> {
    private static final DateTimeFormatter ISO8601 = ISODateTimeFormat
        .dateTimeParser().withZoneUTC();

    @Override
    public Date deserialize(JsonElement jsonElement, Type typeOF,
        JsonDeserializationContext context) throws JsonParseException {
      try {
        return ISO8601.parseDateTime(jsonElement.getAsString()).toDate();
      } catch (Exception e) {
        e.printStackTrace();
        throw new JsonParseException("Unparseable date: \"" + jsonElement
            .getAsString());
      }
    }
  }
}
