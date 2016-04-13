/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.internal.survey;

import com.magnet.mmx.client.ext.poll.MMXPoll;
import retrofit.Callback;
import retrofit.MagnetCall;

public interface PollService {
  MagnetCall<String> createPoll(MMXPoll poll, Callback<String> callback);

  MagnetCall<MMXPoll> getPoll(String id, Callback<MMXPoll> callback);

  MagnetCall<Void> choose(String pollId, String optionId, Callback<Void> callback);
}
