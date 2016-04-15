/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.ext.survey;

import com.magnet.mmx.client.api.MMXTypedPayload;

public class ObjectIdentifier implements MMXTypedPayload {
  public static final String TYPE = "ObjectIdentifier";

  private String objectType;
  private String objectId;

  public ObjectIdentifier(String objectType, String objectId) {
    this.objectType = objectType;
    this.objectId = objectId;
  }

  public String getObjectType() {
    return objectType;
  }

  public String getObjectId() {
    return objectId;
  }
}
