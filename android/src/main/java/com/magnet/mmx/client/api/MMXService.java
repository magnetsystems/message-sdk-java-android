package com.magnet.mmx.client.api;

import android.content.Context;

import com.magnet.core.MagnetService;
import com.magnet.mmx.client.DeviceIdAccessor;
import com.magnet.mmx.client.DeviceIdGenerator;
import com.magnet.mmx.client.FileBasedClientConfig;
import com.magnet.mmx.client.MMXClientConfig;
import com.magnet.mmx.client.common.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the MMX MagnetService implementation for integration with
 * the Magnet MAX platform.
 */
public class MMXService implements MagnetService {
  private static final String TAG = MMXService.class.getSimpleName();
  private static final String DEFAULT_CLIENT_CONFIG = "mmx.properties";
  private Context mContext;

  @Override
  public String getName() {
    return MMXService.class.getSimpleName();
  }

  @Override
  public boolean allowsMultipleInstances() {
    return false;
  }

  @Override
  public void onCreate(Context context, final Map<String, String> configs) {
    //map the configs into a clientConfig and apply it.
    //FIXME: This is temporary until we get the configs from blowfish
    mContext = context;
    InputStream is;
    try {
      is = context.getAssets().open(DEFAULT_CLIENT_CONFIG);
      MMXClientConfig oldPropfileConfig = new FileBasedClientConfig(context, is);
      MMX.init(context, oldPropfileConfig);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to find the assets/" + DEFAULT_CLIENT_CONFIG + " file.");
    }
  }

  @Override
  public void onAppTokenUpdate(String appId, String deviceId, String appToken) {
    //not implemented for now
    Log.d(TAG, "onAppTokenUpdate(): Not implemented for now.  appId=" + appId +
            ", deviceId=" + deviceId + ", appToken=" + appToken);
  }

  @Override
  public void onUserTokenUpdate(final String userName, final String deviceId, final String userToken) {
    Log.d(TAG, "onUserTokenUpdate(): userName=" + userName +
            ", deviceId=" + deviceId + ", userToken=" + userToken);
    //set the deviceId
    DeviceIdGenerator.setDeviceIdAccessor(mContext, new DeviceIdAccessor() {
      public String getId(Context context) {
        return deviceId;
      }
      public boolean obfuscated() {
        return false;
      }
    });
    //logout/login
    if (MMX.getCurrentUser() != null) {
      MMX.logout(new MMX.OnFinishedListener<Void>() {
        public void onSuccess(Void result) {
          Log.d(TAG, "onUserTokenUpdate(): logout success");
          loginHelper(userName, deviceId, userToken);
        }

        public void onFailure(MMX.FailureCode code, Throwable ex) {
          Log.e(TAG, "onUserTokenUpdate(): logout failure: " + code, ex);
        }
      });
    } else {
      loginHelper(userName, deviceId, userToken);
    }
  }

  private void loginHelper(final String userName, final String deviceId, final String userToken) {
    final AtomicBoolean success = new AtomicBoolean(false);
    if (userName != null && deviceId != null && userToken != null) {
      MMX.login(userName, userToken.getBytes(), new MMX.OnFinishedListener<Void>() {
        @Override
        public void onSuccess(Void result) {
          Log.d(TAG, "loginHelper(): success");
        }

        @Override
        public void onFailure(MMX.FailureCode code, Throwable ex) {
          Log.e(TAG, "loginHelper(): failure=" + code, ex);
        }
      });
    }
  }

  @Override
  public void onClose(boolean gracefully) {
    Log.d(TAG, "onClose(): gracefully = " + gracefully);
  }

}
