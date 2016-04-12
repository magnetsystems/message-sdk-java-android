/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.mmx.client.api.MMX;

public class FailureDescription {
  private MMX.FailureCode code;
  private Throwable exception;

  public FailureDescription(MMX.FailureCode code, Throwable exception) {
    this.code = code;
    this.exception = exception;
  }

  public MMX.FailureCode getCode() {
    return code;
  }

  public Throwable getException() {
    return exception;
  }

  @Override public String toString() {
    return "FailureDescription { \n" + "  FailureCode : " + code.toString() + "\n exception : " + exception + "\n}";
  }
}
