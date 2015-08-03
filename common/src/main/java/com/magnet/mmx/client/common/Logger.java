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

import java.util.logging.Level;

/**
 * @hide
 */
public interface Logger {
  /**
   * Mapping from com.manget.mmx.client.Log level to java.util.logging.Level.
   */
  public final static Level[] LEVELS = { 
    null, Level.OFF, Level.ALL, Level.FINER, Level.INFO, Level.WARNING, Level.SEVERE
  };
  /**
   * Get the log level for a log tag or default log level.
   * @param tag A log tag, or null for default log level.
   * @return {@link Log#SUPPRESS}, {@link Log#VERBOSE}, {@link Log#DEBUG},
   *          {@link Log#INFO}, {@link Log#WARN}, or {@link Log#ERROR}.
   * @throws UnsupportedOperationException
   */
  public int getLoggable( String tag )
                                      throws UnsupportedOperationException;
  /**
   * Set the log level for a log tag or default log level.
   * @param tag A log tag, or null for default log level.
   * @param level {@link Log#SUPPRESS}, {@link Log#VERBOSE}, {@link Log#DEBUG}
   *                {@link Log#INFO}, {@link Log#WARN}, {@link Log#ERROR}.
   * @throws UnsupportedOperationException
   */
  public void setLoggable( String tag, int level) 
                                      throws UnsupportedOperationException;
  /**
   * Log for verbose.
   * @param tag A log tag
   * @param data
   * @param cause
   */
  public void verbose( String tag, String data, Throwable cause);
  /**
   * Log for debug.
   * @param tag A non-null log tag.
   * @param data
   * @param cause
   */
  public void debug( String tag, String data, Throwable cause );
  /**
   * Log for info.
   * @param tag A non-null log tag.
   * @param data
   * @param cause
   */
  public void info( String tag, String data, Throwable cause );
  /**
   * Log for warnings.
   * @param tag A non-null log tag.
   * @param data
   * @param cause
   */
  public void warn( String tag, String data, Throwable cause );
  /**
   * Log for errors.
   * @param tag A non-null log tag.
   * @param data
   * @param cause
   */
  public void error( String tag, String data, Throwable cause );
}
