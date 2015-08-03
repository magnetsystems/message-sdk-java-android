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

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.magnet.mmx.client.common.MMXException;
import com.magnet.mmx.util.DisposableTextFile;
import com.magnet.mmx.util.FileCharSequence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestFileCharSequence extends InstrumentationTestCase {
  private final static byte[] sCharData = { 0, '0', 0, '1', 0, '2', 0, '3', 0, '4',
                                            0, '5', 0, '6', 0, '7', 0, '8', 0, '9' };
  private static final String TAG = TestFileCharSequence.class.getSimpleName();
  private File mFile;

  @Override
  public void setUp() {
    mFile = this.getInstrumentation().getTargetContext().getFileStreamPath("c.txt");
  }

  public void testFileCharSequence() throws IOException {
    int size = 2048000;   // 2M bytes
    mFile.delete();
    createFile(mFile, sCharData, size);

    DisposableTextFile dtf = new DisposableTextFile(mFile.getPath(), true);
//    System.out.println("Outfile is "+mFile.getAbsolutePath());
    assertEquals(size, dtf.length());
    FileCharSequence fcsq = new FileCharSequence(dtf);
    long tod = System.currentTimeMillis();
    int end = fcsq.length();  // 1M chars
    for (int start = 0; start < end; ) {
      int len = Math.min(8120, end - start);
//      System.out.println("read from "+start+" to "+(start+len));
      String str = fcsq.subSequence(start, start + len).toString();
      assertEquals("0123456789", str.substring(0, 10));
      start += len;
    }
    long elapsed = System.currentTimeMillis() - tod;
    String str = fcsq.subSequence(0, 10).toString();
    assertEquals("First 10 chars", "0123456789", str);
    fcsq.close();
    assertTrue("Read 1M chars in "+elapsed+"ms", elapsed <= 20000L);
  }

  // Populate a file with "data" of a "total" file size.
  private void createFile(File file, byte[] data, int total) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    while (total > 0) {
      int len = Math.min(data.length, total);
      fos.write(data, 0, len);
      total -= len;
    }
    fos.close();
  }
}
