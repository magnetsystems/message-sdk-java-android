/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import android.support.test.runner.AndroidJUnit4;
import com.magnet.max.android.User;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.TestCaseTimer;
import com.magnet.mmx.client.utils.UserHelper;
import com.magnet.mmx.client.utils.UserPreferencesHelper;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class MMXUserPreferencesTest {
  private static final String TAG = MMXChannelSingleSessionTest.class.getSimpleName();

  @Rule
  public TestCaseTimer testCaseTimer = new TestCaseTimer();

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();

    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_1, UserHelper.MMX_TEST_USER_1);
  }

  @AfterClass
  public static void tearDown() {
    UserHelper.logout();
  }

  @Test
  public void testBlockingUsers() {
    String suffix = String.valueOf(System.currentTimeMillis());
    User user2 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_2 + suffix, UserHelper.MMX_TEST_USER_2, UserHelper.MMX_TEST_USER_2, null, false);
    User user3 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_3 + suffix, UserHelper.MMX_TEST_USER_3, UserHelper.MMX_TEST_USER_3, null, false);
    User user4 = UserHelper.registerUser(UserHelper.MMX_TEST_USER_4 + suffix, UserHelper.MMX_TEST_USER_4, UserHelper.MMX_TEST_USER_4, null, false);

    UserPreferencesHelper.blockUsers(Arrays.asList(user2, user3));
    assertThat(UserPreferencesHelper.getBlockedUsers()).containsOnly(user2, user3);

    UserPreferencesHelper.unblockUsers(Arrays.asList(user3));
    assertThat(UserPreferencesHelper.getBlockedUsers()).containsOnly(user2);

    UserPreferencesHelper.blockUsers(Arrays.asList(user4));
    assertThat(UserPreferencesHelper.getBlockedUsers()).containsOnly(user2, user4);


    UserPreferencesHelper.unblockUsers(Arrays.asList(user2, user4));
    assertThat(UserPreferencesHelper.getBlockedUsers()).isEmpty();
  }
}
