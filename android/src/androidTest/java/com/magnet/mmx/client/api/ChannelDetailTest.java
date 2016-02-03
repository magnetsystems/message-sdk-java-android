/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import android.support.test.runner.AndroidJUnit4;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.internal.channel.ChannelSummaryRequest;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ChannelDetailTest {
  private static final String TAG = ChannelDetailTest.class.getSimpleName();

  @Test
  public void testChannelDetailOptions() {
    ChannelDetailOptions defaultOptions = new ChannelDetailOptions.Builder().build();
    assertEquals(5, defaultOptions.getNumOfMessages().intValue());
    assertEquals(5, defaultOptions.getNumOfSubcribers().intValue());

    ChannelSummaryRequest defaultChannelSummaryRequest = new ChannelSummaryRequest(Collections.EMPTY_LIST, defaultOptions);
    assertEquals(5, defaultOptions.getNumOfMessages().intValue());
    assertEquals(5, defaultOptions.getNumOfSubcribers().intValue());


    ChannelDetailOptions options = new ChannelDetailOptions.Builder().numOfMessages(6).numOfSubcribers(10).build();
    assertEquals(6, options.getNumOfMessages().intValue());
    assertEquals(10, options.getNumOfSubcribers().intValue());

    ChannelSummaryRequest channelSummaryRequest = new ChannelSummaryRequest(Collections.EMPTY_LIST, options);
    assertEquals(6, channelSummaryRequest.getNumOfMessages().intValue());
    assertEquals(10, channelSummaryRequest.getNumOfSubcribers().intValue());

    //Log.d(TAG, "-------------\n\n channel summary request : " + channelSummaryRequest);

  }
}
