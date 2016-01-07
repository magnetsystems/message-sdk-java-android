/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.mmx.client.api.MMXMessage;

public class FailureDescription {
  private MMXMessage.FailureCode code;
  private Throwable exception;

  public FailureDescription(MMXMessage.FailureCode code, Throwable exception) {
    this.code = code;
    this.exception = exception;
  }

  public MMXMessage.FailureCode getCode() {
    return code;
  }

  public Throwable getException() {
    return exception;
  }
}
