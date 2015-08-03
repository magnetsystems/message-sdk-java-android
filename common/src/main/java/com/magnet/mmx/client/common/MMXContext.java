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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * An abstraction for platform specific features.
 */
public abstract class MMXContext {
  /**
   * Constructor to default configuration.  All derived classes must call this.
   */
  protected MMXContext() {
  }
  /**
   * Return the writable directory for MMX files.
   * @return a file object representing a writeable directory for mmx.
   */
  public abstract File getDataDir();
  /**
   * Get the current application version.
   * @return
   */
  public abstract String getAppVersion();
  /**
   * Get a unique device ID.
   * @return
   */
  public abstract String getDeviceId();
  /**
   * Open a file for output under the application path.  If the file does not
   * exist, it will be created for private only and for read-write.  If the
   * file exists, it will be opened for append.
   * @param name
   * @return
   * @throws IOException
   * @see {@link #getDataDir()}
   */
  public abstract FileOutputStream openFileOutput( String name ) throws IOException;
  /**
   * Open a file for input under the application path.
   * @param name
   * @return
   * @throws IOException
   * @see {@link #getDataDir()}
   */
  public abstract FileInputStream openFileInput( String name ) throws IOException;
  /**
   * Return the path of the file.
   * @param name
   * @return
   * @see #getDataDir()
   */
  public String getFilePath( String name ){
    return getDataDir().getAbsolutePath()+File.separatorChar+name;
  }
  /**
   * Create a file with <code>name</code> under the application path.
   * @param name
   * @return
   * @throws IOException
   * @see {@link #getDataDir()}
   */
  public File createFile( String name ) throws IOException {
    FileOutputStream fos = openFileOutput(name);
    fos.close();
    return new File(getFilePath(name));
  }
}
