/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.ext.survey.MMXPoll;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

public class PollHelper2 {

  public static MMXPoll getPollById(String pollId) {
    final ExecMonitor<MMXPoll, FailureDescription> retrievePollResult = new ExecMonitor<>("RetrievePollResult");
    MMXPoll.get(pollId, new MMX.OnFinishedListener<MMXPoll>() {
      @Override public void onSuccess(MMXPoll result) {
        retrievePollResult.invoked(result);
      }

      @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
        retrievePollResult.failed(new FailureDescription(code, ex));
      }
    });
    ExecMonitor.Status retrievePollStatus = retrievePollResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(retrievePollResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, retrievePollStatus);

    return retrievePollResult.getReturnValue();
  }

  public static void vote(MMXPoll poll, int optionIndex) {
    final ExecMonitor<Boolean, FailureDescription> chooseOptionResult = new ExecMonitor<>("ChooseOptionResult");
    poll.choose(poll.getOptions().get(optionIndex), new MMX.OnFinishedListener<Boolean>() {
      @Override public void onSuccess(Boolean result) {
        chooseOptionResult.invoked(result);
      }

      @Override public void onFailure(MMX.FailureCode code, Throwable ex) {
        chooseOptionResult.failed(new FailureDescription(code, ex));
      }
    });
    ExecMonitor.Status chooseOptionStatus = chooseOptionResult.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(chooseOptionResult.getFailedValue()).isNull();
    assertEquals(ExecMonitor.Status.INVOKED, chooseOptionStatus);
    assertEquals(Boolean.TRUE, chooseOptionResult.getReturnValue());
  }
}
