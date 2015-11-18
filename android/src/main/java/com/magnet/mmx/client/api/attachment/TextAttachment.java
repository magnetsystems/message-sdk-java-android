/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api.attachment;

public class TextAttachment extends MMXAttachment<String> {

  public TextAttachment(String content) {
    this(MimeType.TEXT_PLAIN, content);
  }

  public TextAttachment(MimeType mimeType, String content) {
    super(mimeType, content, null != content ? content.length() : -1);
  }
}
