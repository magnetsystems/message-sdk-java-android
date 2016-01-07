/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import android.util.Log;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TestCaseTimer extends TestWatcher {
  private long startTime;

  @Override protected void starting(Description description) {
    super.starting(description);

    startTime = System.currentTimeMillis();
    Log.d(description.getClassName(), "\n----------------------------starting test case " + description.getMethodName() + "----------------------------\n");

  }

  @Override protected void finished(Description description) {
    super.finished(description);

    Log.d(description.getClassName(), "\n----------------------------finished test case " + description.getMethodName() + " in " + (System.currentTimeMillis() - startTime) + " milliseconds----------------------------\n");

  }
}
