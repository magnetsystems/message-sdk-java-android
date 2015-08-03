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

package com.magnet.mmx.client.common;

/**
 * The general exception for MMX.
 */
public class MMXException extends Exception {
  private static final long serialVersionUID = -4946667362526327456L;
  /**
   * The request has malformed data.
   */
  public final static int BAD_REQUEST = 400;
  /**
   * Unauthorized access.
   */
  public final static int UNAUTHORIZED = 401;
  /**
   * Resource is not found.
   */
  public final static int NOT_FOUND = 404;
  /**
   * The request is too large.
   */
  public final static int REQUEST_TOO_LARGE = 413;
  /**
   * This feature is not implemented.
   */
  public final static int NOT_IMPLEMENTED = 501;

  private int mCode;

  /**
   * @hide
   * Constructor with a message.
   * @param msg A message.
   */
  public MMXException(String msg) {
    super(msg);
  }

  /**
   * @hide
   * Constructor with a cause.
   * @param cause A lower level exception.
   */
  public MMXException(Throwable cause) {
    super(cause);
  }

  /**
   * @hide
   * Constructor with a message and a status code.
   * @param msg A message.
   * @param code A status code.
   */
  public MMXException(String msg, int code) {
    super(msg);
    mCode = code;
  }

  /**
   * @hide
   * Constructor with a message and cause.
   * @param msg A message.
   * @param cause A lower level exception.
   */
  public MMXException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * @hide
   * Constructor with a message, a status code and cause.
   * @param msg A message.
   * @param code A status code.
   * @param cause A lower level exception.
   */
  public MMXException(String msg, int code, Throwable cause) {
    super(msg, cause);
    mCode = code;
  }

  /**
   * Return the status code for this exception.
   * @return The statuc code.
   */
  public int getCode() {
    return mCode;
  }
}
