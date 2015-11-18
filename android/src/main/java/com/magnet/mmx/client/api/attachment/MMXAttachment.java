/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api.attachment;

import com.google.gson.annotations.Expose;

public abstract class MMXAttachment<T> {
  enum Status {
    INIT,
    INLINE,
    TRANSFERING,
    COMPLETE
  }

  public interface AttachmentTrasferLister {
    void onStart(MMXAttachment attachment);
    void onProgress(MMXAttachment attachment, long processedBytes);
    void onComplete(MMXAttachment attachment);
    void onError(MMXAttachment attachment, Throwable error);
  }

  @Expose
  protected Status status = Status.INIT;
  protected String name;
  protected String description;
  protected MimeType mimeType;
  protected long length = -1;
  @Expose
  protected T content;
  /** The id to retrieve the attachement from server */
  protected String resourceId;

  public MMXAttachment(MimeType mimeType, T content, long length) {
    this(null, null, mimeType, length, content);
  }

  public MMXAttachment(String name, String description, MimeType mimeType, long length, T content) {
    this.name = name;
    this.description = description;
    this.mimeType = mimeType;
    this.length = length;
    this.content = content;
  }

  public MimeType getMimeType() {
    return mimeType;
  }

  public T getContent() {
    return content;
  }

  public String getResourceId() {
    return resourceId;
  }

  public Status getStatus() {
    return status;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public long getLength() {
    return length;
  }

  public void upload(AttachmentTrasferLister lister) {

  }

  public void download(AttachmentTrasferLister lister) {

  }
}
