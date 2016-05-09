/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXUserPreferences;
import com.magnet.mmx.client.common.Log;
import java.util.HashSet;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class UserPreferencesHelper {
  private static final String TAG = UserPreferencesHelper.class.getSimpleName();

  public static void blockUsers(List<User> users) {
    final ExecMonitor<Boolean, ApiError> execMonitor = new ExecMonitor<>();
    MMXUserPreferences.blockUsers(new HashSet<User>(users), new ApiCallback<Boolean>() {
      @Override public void success(Boolean result) {
        execMonitor.invoked(result);
      }

      @Override public void failure(ApiError apiError) {
        Log.e(TAG, apiError.getMessage(), apiError);
        execMonitor.failed(apiError);
      }
    });

    ExecMonitor.Status status = execMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(status).isEqualTo(ExecMonitor.Status.INVOKED);
    assertThat(execMonitor.getReturnValue()).isTrue();
  }

  public static void unblockUsers(List<User> users) {
    final ExecMonitor<Boolean, ApiError> execMonitor = new ExecMonitor<>();
    MMXUserPreferences.unblockUsers(new HashSet<User>(users), new ApiCallback<Boolean>() {
      @Override public void success(Boolean result) {
        execMonitor.invoked(result);
      }

      @Override public void failure(ApiError apiError) {
        Log.e(TAG, apiError.toString(), apiError);
        execMonitor.failed(apiError);
      }
    });

    ExecMonitor.Status status = execMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(status).isEqualTo(ExecMonitor.Status.INVOKED);
    assertThat(execMonitor.getReturnValue()).isTrue();
  }

  public static List<User> getBlockedUsers() {
    final ExecMonitor<List<User>, ApiError> execMonitor = new ExecMonitor<>();
    MMXUserPreferences.getBlockedUsers(new ApiCallback<List<User>>() {
      @Override public void success(List<User> result) {
        execMonitor.invoked(result);
      }

      @Override public void failure(ApiError apiError) {
        Log.e(TAG, apiError.toString(), apiError);
        execMonitor.failed(apiError);
      }
    });

    ExecMonitor.Status status = execMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
    assertThat(status).isEqualTo(ExecMonitor.Status.INVOKED);
    return execMonitor.getReturnValue();
  }
}
