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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.magnet.mmx.client.common.Log;

/**
 * @hide
 * The Java (J2SE) implementation of the logger using System.out.
 * Magnet Server team should override this class or rewrite this class.
 * If overriding this, specify the class name in 
 * com.magnet.mmx.client.Log static block.
 */
public class JavaLogger implements com.magnet.mmx.client.common.Logger {

  @Override
  public int getLoggable(String tag) throws UnsupportedOperationException {
//  Logger logger = (tag == null) ? Logger.getGlobal() : Logger.getLogger(tag);
    Logger logger = Logger.getGlobal();
    Level level = logger.getLevel();
    if (level == Level.OFF) return Log.SUPPRESS;
    if (level == Level.ALL) return Log.VERBOSE;
    if (level == Level.FINER) return Log.DEBUG;
    if (level == Level.INFO) return Log.INFO;
    if (level == Level.WARNING) return Log.WARN;
    if (level == Level.SEVERE) return Log.ERROR;
    return Log.SUPPRESS;
  }

  @Override
  public void setLoggable(String tag, int level)
      throws UnsupportedOperationException {
//  Logger logger = (tag == null) ? Logger.getGlobal() : Logger.getLogger(tag);
    Logger logger = Logger.getGlobal();
    logger.setLevel(LEVELS[level]);
  }

  @Override
  public void verbose(String tag, String data, Throwable cause) {
    // TODO: Can't figure out why log() does not work with ConsoleHandler!
//    Logger.getGlobal().log(Level.ALL, tag+" "+data, cause);
//    Logger logger = Logger.getGlobal();
//    data = concat(tag, data, cause);
//    logger.finest(data);
    System.out.println("V/"+tag+' '+System.currentTimeMillis()+' '+data);
    if (cause != null) {
      cause.printStackTrace();
    }
  }

  @Override
  public void debug(String tag, String data, Throwable cause) {
//     Logger.getGlobal().log(Level.FINER, tag+" "+data, cause);
    System.out.println("D/"+tag+' '+System.currentTimeMillis()+' '+data);
    if (cause != null) {
      cause.printStackTrace();
    }
  }

  @Override
  public void info(String tag, String data, Throwable cause) {
//  Logger.getGlobal().log(Level.INFO, tag+" "+data, cause);   
    System.out.println("I/"+tag+' '+System.currentTimeMillis()+' '+data);
    if (cause != null) {
      cause.printStackTrace();
    }
  }

  @Override
  public void warn(String tag, String data, Throwable cause) {
//  Logger.getGlobal().log(Level.WARNING, tag+" "+data, cause);
    System.out.println("W/"+tag+' '+System.currentTimeMillis()+' '+data);
    if (cause != null) {
      cause.printStackTrace();
    }
  }

  @Override
  public void error(String tag, String data, Throwable cause) {
//  Logger.getGlobal().log(Level.SEVERE, tag+" "+data, cause);
    System.out.println("E/"+tag+' '+System.currentTimeMillis()+' '+data);
    if (cause != null) {
      cause.printStackTrace();
    }
  }
  
//private String concat(String tag, String data, Throwable cause) {
//  StringWriter sw = new StringWriter((tag == null ? 0 : (tag.length()+1))+data.length()+1024);
//  if (tag != null) {
//    sw.append(tag).append(' ');
//  }
//  sw.append(data).append("\r\n");
//  if (cause != null) {
//    cause.printStackTrace(new PrintWriter(sw));
//  }
//  return sw.toString();
//}
}
