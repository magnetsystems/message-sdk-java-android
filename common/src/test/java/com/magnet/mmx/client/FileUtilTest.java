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

import com.magnet.mmx.util.DisposableTextFile;
import com.magnet.mmx.util.FileCharSequence;
import com.magnet.mmx.util.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test base64 file encoding and decoding.
 */
public class FileUtilTest {
  private final static byte[] sData = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xf };
  private final static byte[] sCharData = { 0, '0', 0, '1', 0, '2', 0, '3', 0, '4',
                                            0, '5', 0, '6', 0, '7', 0, '8', 0, '9' };
  private final static File sInFile = new File("data.bin");
  private final static File sOutFile = new File("b64.txt");

  @BeforeClass
  public static void setup() throws Exception {
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

  // Check the file of "total" file size if it contains "data".
  private void checkFile(File file, byte[] data, int total) throws IOException {
    assertEquals(total, (int) file.length());
    FileInputStream fis = new FileInputStream(file);
    byte[] buf = new byte[data.length];
    while (total > 0) {
      int n = fis.read(buf, 0, buf.length);
      for (int i = 0; i < n; i++) {
        assertEquals(data[i], buf[i]);
      }
      total -= n;
    }
  }

  private void testEncodeFile(int fileSize) throws IOException {
    sInFile.delete();
    sOutFile.delete();
    createFile(sInFile, sData, fileSize);

    int total = FileUtil.encodeFile(sInFile, sOutFile);
    assertEquals((fileSize+2)/3*4, sOutFile.length());
    sInFile.delete();

    total = FileUtil.decodeFile(sOutFile, sInFile);
    assertEquals(fileSize, total);
    sOutFile.delete();

    checkFile(sInFile, sData, fileSize);
    sInFile.delete();
  }

  @Test
  public void testEncode_Empty_File() throws IOException {
    testEncodeFile(0);
  }

  @Test
  public void testEncode_1_Byte_File() throws IOException {
    testEncodeFile(1);
  }

  @Test
  public void testEncode_500_Bytes_File() throws IOException {
    testEncodeFile(500);
  }

  @Test
  public void testEncode_501_Bytes_File() throws IOException {
    testEncodeFile(501);
  }

  @Test
  public void testEncode_502_Bytes_File() throws IOException {
    testEncodeFile(502);
  }

  @Test
  public void testEncode_503_Bytes_File() throws IOException {
    testEncodeFile(503);
  }

  @Test
  public void testEncode_20000_Bytes_File() throws IOException {
    testEncodeFile(20000);
  }

  @Test
  public void testEncode_20001_Bytes_File() throws IOException {
    testEncodeFile(20001);
  }

  @Test
  public void testEncode_20002_Bytes_File() throws IOException {
    testEncodeFile(20002);
  }

  @Test
  public void testEncode_20003_Bytes_File() throws IOException {
    testEncodeFile(20003);
  }

  @Test
  public void testEncode_2M_Bytes_File() throws IOException {
    testEncodeFile(2000000);
  }

  @Test
  public void testFileCharSequence() throws IOException {
    int size = 2048000;   // 2MB
    sOutFile.delete();
    createFile(sOutFile, sCharData, size);

    DisposableTextFile dtf = new DisposableTextFile(sOutFile.getPath(), true);
//    System.out.println("Outfile is "+sOutFile.getAbsolutePath());
    assertEquals(size, dtf.length());
    FileCharSequence fcsq = new FileCharSequence(dtf);
    long tod = System.currentTimeMillis();
    int end = fcsq.length();  // 1M chars
    for (int start = 0; start < end; ) {
      int len = Math.min(8000, end - start);
      System.out.println("read from "+start+" to "+(start+len));
      String str = fcsq.subSequence(start, start + len).toString();
      assertEquals("0123456789", str.substring(0, 10));
      start += len;
    }
    long elapsed = System.currentTimeMillis() - tod;
    String str = fcsq.subSequence(0, 10).toString();
    assertEquals("First 10 chars", "0123456789", str);
    fcsq.close();
    assertTrue("Read 1M chars in "+elapsed+"ms", elapsed <= 5000L);
  }
}
