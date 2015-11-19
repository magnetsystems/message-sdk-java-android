/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api.attachment;

import java.io.File;

public class FileAttachment extends MMXAttachment<File> {

  public FileAttachment(MimeType mimeType, File content) {
    super(mimeType, content, null != content ? content.length() : -1);
  }

}
