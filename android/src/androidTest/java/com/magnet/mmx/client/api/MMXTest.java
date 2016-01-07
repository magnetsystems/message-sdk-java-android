package com.magnet.mmx.client.api;

import android.support.test.runner.AndroidJUnit4;
import com.magnet.max.android.User;
import com.magnet.mmx.client.utils.MaxHelper;
import com.magnet.mmx.client.utils.UserHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MMXTest {
  private static final String TAG = MMXTest.class.getSimpleName();

  @BeforeClass
  public static void setUp() {
    MaxHelper.initMax();
  }

  @AfterClass
  public static void tearDown() {
  }

  @Test
  public void testLoginLogout() {
    UserHelper.registerAndLogin(UserHelper.MMX_TEST_USER_1, UserHelper.MMX_TEST_USER_1);
    User currentUser = MMX.getCurrentUser();
    assertEquals(UserHelper.MMX_TEST_USER_1, currentUser.getFirstName());
    assertEquals(UserHelper.MMX_TEST_USER_1.toLowerCase(), currentUser.getUserName());

    UserHelper.logout();
//    assertNull(MMX.getCurrentUser());
//    assertFalse(MMX.getMMXClient().isConnected());
  }
}
