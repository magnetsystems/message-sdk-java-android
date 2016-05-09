/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.ext.poll.MMXPoll;
import com.magnet.mmx.client.ext.poll.MMXPollOption;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

public class PollHelper {

  public static MMXPoll getPollById(String pollId) {
    final ExecMonitor<MMXPoll, ApiError> retrievePollResult = new ExecMonitor<>("RetrievePollResult");
    MMXPoll.get(pollId, new ApiCallback<MMXPoll>() {
      @Override public void success(MMXPoll result) {
        retrievePollResult.invoked(result);
      }

      @Override public void failure(ApiError apiError) {
        retrievePollResult.failed(apiError);
      }
    });
    ExecMonitor.Status retrievePollStatus = retrievePollResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(retrievePollResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, retrievePollStatus);

    return retrievePollResult.getReturnValue();
  }

  public static void vote(MMXPoll poll, List<MMXPollOption> options) {
    final ExecMonitor<MMXMessage, ApiError> chooseOptionResult = new ExecMonitor<>("ChooseOptionResult");
    poll.choose(options, new ApiCallback<MMXMessage>() {
      @Override public void success(MMXMessage result) {
        chooseOptionResult.invoked(null);
      }

      @Override public void failure(ApiError apiError) {
        chooseOptionResult.failed(apiError);
      }
    });
    ExecMonitor.Status chooseOptionStatus = chooseOptionResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(chooseOptionResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, chooseOptionStatus);;
  }
}
