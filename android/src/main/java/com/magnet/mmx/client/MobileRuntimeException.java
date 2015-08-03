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

/**
 * This class defines runtime exceptions that can occur when using the Magnet APIs.
 *
 */
public class MobileRuntimeException extends RuntimeException {

  /**
   * Default constructor.
   */
  public MobileRuntimeException() {
    super();
  }

  /**
   * Constructor that instantiates an exception using the specified message string.
   * @param message The message string.
   */
  public MobileRuntimeException(String message) {
    super(message);
  }

  /**
   * Constructor that instantiates an exception using the specified cause.
   * @param cause The exception cause.
   */
  public MobileRuntimeException(Exception cause) {
    super(cause);
  }

  /**
   * Constructor that instantiates an exception using the specified message string and cause.
   * @param message The message string.
   * @param cause The exception cause.
   */
  public MobileRuntimeException(String message, Exception cause) {
    super(message, cause);
  }

}
