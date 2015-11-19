package com.magnet.mmx.client.api.attachment;

import retrofit.MagnetCall;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;

/**
 * Copyright (c) 2012-2015 Magnet Systems. All rights reserved.
 */
public interface AttachmentService {
  /**
   *
   * POST
   * @param file style:Query optional:false
   * @param callback asynchronous callback
   */
  @Multipart
  @POST("/api/com.magnet.server/file/save")
  MagnetCall<Void> uploadAttachment(@Part("file") com.squareup.okhttp.RequestBody file, retrofit.Callback<Void> callback);

}
