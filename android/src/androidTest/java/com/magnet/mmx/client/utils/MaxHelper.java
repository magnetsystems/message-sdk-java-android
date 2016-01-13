/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import com.magnet.max.android.MaxCore;
import com.magnet.mmx.client.api.MMX;

public class MaxHelper {

  public static void initMax() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    try {
      MaxCore.init(context, new MaxAndroidJsonConfig(context, com.magnet.mmx.test.R.raw.keys));
    } catch (IllegalStateException e) {

    }
    MaxCore.register(MMX.getModule());

    com.magnet.mmx.client.common.Log.setLoggable(null, com.magnet.mmx.client.common.Log.VERBOSE);
  }

}
