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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * A J2SE client context.  This context creates an application silo for a Java
 * client.
 */
public class MMXContext extends com.magnet.mmx.client.common.MMXContext {
  private File mPath;
  private String mVersion;
  private String mDeviceId;
  
  /**
   * Default constructor.  The <code>resourceId</code> can have any printable
   * characters except '@', '/', or '%'.
   * @param appPath Application data directory.
   * @param appVersion Application version string.
   * @param resourceId A unique end-point ID (e.g. MAC-address, instance name)
   */
  public MMXContext(String appPath, String appVersion, String resourceId) {
    super();
    mPath = new File(appPath);
    mVersion = appVersion;
    mDeviceId = resourceId;
    
    // Use console logging as the default handler.
    Logger.getGlobal().addHandler(new ConsoleHandler());
  }
  
  @Override
  public File getDataDir() {
    return mPath;
  }

  @Override
  public String getAppVersion() {
    return mVersion;
  }

  @Override
  public FileOutputStream openFileOutput(String name) throws IOException {
    return new FileOutputStream(getFilePath(name));
  }

  @Override
  public FileInputStream openFileInput(String name) throws IOException {
    return new FileInputStream(getFilePath(name));
  }
  
  @Override
  public String getDeviceId() {
    return mDeviceId;
  }
}
