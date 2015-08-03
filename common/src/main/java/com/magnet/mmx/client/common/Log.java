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
 * @hide
 * A platform independent log utility used primarily by the SDK.
 */
public class Log {
  public final static int SUPPRESS = 1;
  public final static int VERBOSE = 2;
  public final static int DEBUG = 3;
  public final static int INFO = 4;
  public final static int WARN = 5;
  public final static int ERROR = 6;

  private final static String sGlobalTag = "MMX";
  private static Logger sLogger;

  static {
    String[] loggerClassNames = new String[] {
        "com.magnet.mmx.client.AndroidLogger",
        "com.magnet.mmx.client.JavaLogger",
        };
    for (String clzName : loggerClassNames) {
      try {
        sLogger = (Logger) Class.forName(clzName).newInstance();
        sLogger.info("Log", "Use "+clzName+" as the logger", null);
        break;
      } catch (Throwable e) {
        // continue;
      }
    }
    if (sLogger == null) {
      System.err.println("Unable to find logger class; logging is disabled");
    }
  }

  /**
   * Get the current log level for a tag.  If the log level is not set for the
   * tag, the default log level will be returned.
   * @param tag The log tag, or null for default log level.
   * @return Get the log level of a tag.
   */
  public static int getLoggable(String tag) {
    return (sLogger == null) ? SUPPRESS : sLogger.getLoggable(tag);
  }

  /**
   * Set the log level for the specified tag in this process.
   * @param tag The log tag, or null for default log level.
   * @param level The log level: {@link #SUPPRESS} , {@link #VERBOSE} , {@link #DEBUG} ,
   *            {@link #INFO} , {@link #WARN} , {@link #ERROR}
   * @return <code>true</code> if the log level was successfully set; otherwise an exception is thrown.
   * @exception IllegalArgumentException Invalid log level.
   */
  public static boolean setLoggable(String tag, int level) {
    if (level < SUPPRESS || level > ERROR) {
      throw new IllegalArgumentException("Invalid log level: "+level);
    }
    if (sLogger == null) {
      return false;
    }
    sLogger.setLoggable(tag, level);
    return true;
  }

  /**
   * Determine whether the log level for the tag is at least at the specified level.  If no
   * level is set for the specified tag, it will compare against {@link #sDefaultLogLevel}.
   * A log level with {@link #SUPPRESS} always returns <code>false</code>.
   * Use this method or {@link #isLoggable(int)} to guard against
   * {@link #v(String, String)} and {@link #d(String, String)}.
   * @param tag The log tag, or null for default log level.
   * @param level The log level: {@link #VERBOSE}, {@link #DEBUG}, {@link #INFO},
   *    {@link #WARN}, {@link #ERROR}
   * @return <code>true</code> if the log level for the tag is at least at the specified level; <code>false</code> otherwise.
   */
  public static boolean isLoggable(String tag, int level) {
    int tagLevel = getLoggable(tag);
    return tagLevel > SUPPRESS && level >= tagLevel;
  }

  /**
   * Sends a verbose log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static void v(String tag, String msg) {
    v(tag, msg, null);
  }

  /**
   * Sends a verbose log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static void v(String tag, String msg, Throwable tr) {
    if (isLoggable(tag, VERBOSE)) {
      sLogger.verbose(tag, getNewMsg(sGlobalTag, msg), tr);
    }
  }

  /**
   * Sends a debug log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static void d(String tag, String msg) {
    d(tag, msg, null);
  }

  /**
   * Sends a debug log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static void d(String tag, String msg, Throwable tr) {
    if (isLoggable(tag, DEBUG)) {
      sLogger.debug(tag, getNewMsg(sGlobalTag, msg), tr);
    }
  }

  /**
   * Sends an information log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static void i(String tag, String msg) {
    i(tag, msg, null);
  }

  /**
   * Sends an information log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static void i(String tag, String msg, Throwable tr) {
    if (isLoggable(tag, INFO)) {
      sLogger.info(tag, getNewMsg(sGlobalTag, msg), tr);
    }
  }

  /**
   * Sends a warning log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static void w(String tag, String msg) {
    w(tag, msg, null);
  }

  /**
   * Sends a warning log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static void w(String tag, String msg, Throwable tr) {
    if (isLoggable(tag, WARN)) {
      sLogger.warn(tag, getNewMsg(sGlobalTag, msg), tr);
    }
  }

  /**
   * Sends an error log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static void e(String tag, String msg) {
    e(tag, msg, null);
  }

  /**
   * Sends an error log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static void e(String tag, String msg, Throwable tr) {
    if (isLoggable(tag, ERROR)) {
      sLogger.error(tag, getNewMsg(sGlobalTag, msg), tr);
    }
  }

  private static String getNewMsg(String tag, String msg) {
    return ((tag == null ? "" : tag) + "[" + Thread.currentThread().getName()
              + "]" + (msg == null || msg.length() == 0 ? "" : ": " + msg));
  }
}
