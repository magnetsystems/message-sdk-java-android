/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api.attachment;

public class BytesAttachment extends MMXAttachment<byte[]> {

  public BytesAttachment(MimeType mimeType, byte[] content) {
    super(mimeType, content, null != content ? content.length : -1);
  }
}
