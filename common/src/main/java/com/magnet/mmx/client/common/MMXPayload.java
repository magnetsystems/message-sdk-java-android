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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.protocol.Payload;
import com.magnet.mmx.util.Base64;
import com.magnet.mmx.util.DisposableFile;
import com.magnet.mmx.util.FileUtil;

/**
 * This class represents an immutable application data to be sent through
 * MessageManager or published through PubSubManager.  It consisted of two
 * components: meta data and content.  The optional meta data allows application
 * to specify any arbitrary data associated with the content in name-value
 * pairs. The content is the actual data which can be originated from memory or
 * loaded from a file.
 */
public class MMXPayload implements Serializable {
  /**
   * An optional header name for the sender to specify an alternative response
   * address.  The value should be an MMX ID of a user or an end-point.
   */
  public final static String REPLY_TO = Headers.REPLY_TO;
  /**
   * An optional header name for the payload MIME content type.
   */
  public final static String CONTENT_TYPE = Headers.CONTENT_TYPE;
  /**
   * An optional header name for the payload encoding type.  Although
   * the value is application specific, one standard value is "base64".
   */
  public final static String CONTENT_ENCODING = Headers.CONTENT_ENCODING;
  private static final long serialVersionUID = -1590042423835668427L;
  private MmxHeaders mMmxMeta;
  private Headers mMeta;
  private Payload mPayload;

  /**
   * Get the allowable size of the payload and meta data.
   * @return The allowable max payload size.
   */
  public static int getMaxSizeAllowed() {
    return Payload.getMaxSizeAllowed();
  }

  /**
   * Use unidentified a text data as payload.  The text data can be a
   * string, structured text as JSON, or encoded text as Base64.  Unless the
   * data is self identifiable or unilateral known by the application,
   * application may use {@link #MMXPayload(String, CharSequence)} or
   * {@link #setMetaData(String, String)} to identify the data.
   * @param textData Text based application data.
   */
  public MMXPayload(CharSequence textData) {
    this(null, textData);
  }

  /**
   * Use unidentified data from a file as payload.  It is not a file transfer,
   * but an efficient way to have large data back by a file.  The file data
   * can be any UTF-8 structured or unstructured text, or binary data.  Unless
   * the data is self identifiable or unilateral known by the application,
   * application should use {@link #MMXPayload(String, DisposableFile)} or
   * {@link #setMetaData(String, String)} to identify the data.
   * @param fileData The file contains the application data.
   */
  public MMXPayload(DisposableFile fileData) {
    this(null, fileData);
  }

  /**
   * Use an identified text data as payload.  The text data can be a string,
   * structured text such as JSON or XML, or base64 encoded text.  The payload
   * is identifiable by the <code>type</code>.  Application can use
   * <code>type</code> to convert <code>textData</code> into an object.
   * @param type An identifier for the data content.
   * @param textData Data content in text.
   * @see com.magnet.util.TypeMapper#getClassByType(String)
   */
  MMXPayload(String type, CharSequence textData) {
    mMmxMeta = new MmxHeaders();
    mMeta = new Headers();
    mPayload = new Payload(type, textData);
  }

  /**
   * Use an identified data from file as payload.  It is not a file transfer,
   * but an efficient way to have large data back by a file.  The file data
   * can be any UTF-8 structured or unstructured text, or binary data.  The data
   * is identifiable by the <code>type</code>.  Application can use
   * <code>type</code> to map the content in <code>fileData</code> to an
   * object.
   * @param type An identifier for the data content.
   * @param fileData Data content stored in a file.
   * @see com.magnet.util.TypeMapper#getClassByType(String)
   */
  MMXPayload(String type, DisposableFile fileData) {
    mMmxMeta = new MmxHeaders();
    mMeta = new Headers();
    mPayload = new Payload(type, fileData);
  }

  /**
   * @hide
   * An internal constructor used by incoming receiver.
   */
  public MMXPayload(MmxHeaders mmxMeta, Headers meta, Payload payload) {
    if ((mMmxMeta = mmxMeta) == null) {
      mMmxMeta = new MmxHeaders();
    }
    if ((mMeta = meta) == null) {
      mMeta = new Headers();
    }
    mPayload = payload;
  }

  /**
   * Set an MMX meta header to this payload.  Duplicated header will be
   * overwritten.
   * @param name A header name.
   * @param value A JSON String value.
   * @return
   */
  MMXPayload setMmxMetaData(String name, Object value) {
    mMmxMeta.put(name, value);
    return this;
  }

  /**
   * Set all application meta <code>headers</code> to this payload.  Duplicated
   * headers will be overwritten.
   * @param headers A non-null collection of meta headers.
   * @return This object.
   */
  public MMXPayload setAllMetaData(Map<String, String> headers) {
    mMeta.putAll(headers);
    return this;
  }

  /**
   * Set an application meta header to this payload.  Duplicated header will be
   * overwritten.
   * @param name A header name.
   * @param value A value.
   * @return This object.
   */
  public MMXPayload setMetaData(String name, String value) {
    mMeta.put(name, value);
    return this;
  }

  /**
   * A convenient method to set Content-Type in the header.
   * @param value The MIME content-type value.
   * @return This object.
   */
  public MMXPayload setContentType(String value) {
    mMeta.setContentType(value);
    return this;
  }

  /**
   * A convenient method to set Content-Encoding in the header.
   * @param value The content-encoding type.
   * @return This object.
   */
  public MMXPayload setContentEncoding(String value) {
    mMeta.setContentEncoding(value);
    return this;
  }

//  /**
//   * A convenient method to set Reply-To in the header.
//   * @param value The reply-to address.
//   * @return This object.
//   */
//  public MMXPayload setReplyTo(MMXid value) {
//    mHeaders.setReplyTo(value.toJson());
//    return this;
//  }

  /**
   * Specify the recipients in the header.  The JSON serialization will be
   * performed just before the message is sent, but not in this method.
   * @return This object.
   */
  MMXPayload setTo(MMXid[] xids) {
    mMmxMeta.put(MmxHeaders.TO, xids);
    return this;
  }

  /**
   * Unmarshall the recipients from the deserialized JSON object to MMX ID's.
   * This is not the counter part of {@link #setTo(MMXid[])}.
   * @return
   */
  MMXid[] unmarshallTo() {
    List<Map<String, String>> list = (List<Map<String, String>>)
        mMmxMeta.getHeader(MmxHeaders.TO, null);
    if (list == null) {
      return null;
    }
    int i = 0;
    MMXid[] xids = new MMXid[list.size()];
    for (Map<String, String> map : list) {
      xids[i++] = MMXid.fromMap(map);
    }
    return xids;
  }

  /**
   * Get the identifier of the data.
   * @return The data identifier.
   */
  String getType() {
    return mPayload.getMsgType();
  }

  /**
   * Get the total data size.
   * @return The total payload size.
   */
  public int getDataSize() {
    return mPayload.getDataSize();
  }

  /**
   * Get the sent time when the payload was sent.
   * @return The sent time or null.
   */
  public Date getSentTime() {
    return mPayload.getSentTime();
  }

  /**
   * Get the raw data and convert it to text data.  If the raw data is binary,
   * the text data will be base64 encoded.  If the raw data is UTF-8, the text
   * data will be converted to 16-bit characters.
   * @return The char sequence of the encoded data.
   */
  public CharSequence getDataAsText() {
    DisposableFile file;
    if ((file = mPayload.getFile()) != null) {
      return FileUtil.encodeFile(file, false);
    } else {
      return mPayload.getData();
    }
  }

  /**
   * Get the raw data as input stream.  If the data is base64 encoded, it will
   * be decoded first.
   * @return An input stream of the raw data.
   * @throws IOException
   */
  public InputStream getDataAsInputStream() throws IOException {
    if (!Constants.BASE64.equalsIgnoreCase(getContentEncoding())) {
      // Content is not base64 encoded, get it as input stream.
      return mPayload.getDataAsInputStream();
    }

    FileInputStream ins = null;
    File tmpFile = File.createTempFile("mmx", ".bin");
    tmpFile.deleteOnExit();
    if (mPayload.getData() != null) {
      // Memory content is base64 encoded
      Base64.decodeToFile(mPayload.getData().toString(), tmpFile.getPath());
    } else if (mPayload.getFile() != null) {
      // File content is base64 encoded.
      Base64.decodeFileToFile(mPayload.getFile().getPath(), tmpFile.getPath());
    }

    ins = new FileInputStream(tmpFile);
    tmpFile.delete();
    return ins;
  }

  /**
   * Get all meta headers from this payload.
   * @return The meta headers in this payload.
   */
  public Map<String, String> getAllMetaData() {
    return mMeta;
  }

  /**
   * Get a meta header from this payload.
   * @param name The name of the meta header.
   * @param defVal The default value to be returned if the header does not exist.
   * @return A value.
   */
  public String getMetaData(String name, String defVal) {
    return mMeta.getHeader(name, defVal);
  }

  /**
   * A convenient method getting the Content-Encoding if available.
   * @return null or a content encoding type.
   */
  public String getContentEncoding() {
    return mMeta.getContentEncoding(null);
  }

//  /**
//   * A convenient method getting an alternative response address if available.
//   * @return null or an alternative response address.
//   */
//  public MMXid getReplyTo() {
//    String replyTo = mHeaders.getReplyTo(null);
//    if (replyTo == null)
//      return null;
//    return MMXid.fromJson(replyTo);
//  }

  /**
   * A convenient method getting the Content-Type if available.
   * @return null or a content type.
   */
  public String getContentType() {
    return mMeta.getContentType(null);
  }

  /**
   * Get the internal MMX meta header extension object.
   * @return
   */
  MmxHeaders getMmxMeta() {
    return mMmxMeta;
  }

  /**
   * @hide
   * Get the internal meta header extension object.
   * @return
   */
  Headers getMetaExt() {
    return mMeta;
  }

  /**
   * @hide
   * Get the internal payload extension object.
   * @return
   */
  Payload getPayloadExt() {
    return mPayload;
  }

  /**
   * Get the total size of the payload which includes the content data, content
   * data identifier and meta data.
   * @return The total size of the payload.
   */
  int getSize() {
    int size = 0;
    if (mMeta != null) {
      for (Map.Entry<String, String> header : mMeta.entrySet()) {
        size += header.getKey().length();
        size += header.getValue().length();
      }
    }
    if (mPayload != null) {
      String type = mPayload.getMsgType();
      if (type != null) {
        size += type.length();
      }
      size += mPayload.getDataSize();
    }
    return size;
  }

  @Override
  public String toString() {
    return "[ mmxmeta=("+mMmxMeta+"), meta=("+mMeta+"), payload="+mPayload+" ]";
  }
}
