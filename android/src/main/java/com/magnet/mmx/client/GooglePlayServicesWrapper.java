package com.magnet.mmx.client;

import android.content.Context;
import android.content.Intent;

import com.magnet.mmx.client.common.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps the Google Play Services classes for gcm functionality.  Allows
 * loose coupling of GCM/Google play so we don't need to make it a hard
 * dependency in the SDK.
 *
 */
class GooglePlayServicesWrapper {
  private static final String TAG = GooglePlayServicesWrapper.class.getSimpleName();
  public static final int PLAY_SERVICES_AVAILABLE = 0;
  public static final int PLAY_SERVICES_SERVICE_VERSION_UPDATE_REQUIRED = 2;
  public static final int PLAY_SERVICES_MAGNET_VERSION_INCOMPATIBILITY = 3;
  public static final int PLAY_SERVICES_UNAVAILABLE = 4;

  public static final int GCM_MESSAGE_TYPE_SEND_ERROR = 0;
  public static final int GCM_MESSAGE_TYPE_DELETED = 1;
  public static final int GCM_MESSAGE_TYPE_MESSAGE = 2;
  public static final int GCM_MESSAGE_TYPE_SEND_EVENT = 3;

  private static GooglePlayServicesWrapper sInstance = null;
  private final Context mContext;
  private Class mGoogleApiAvailability_class = null;
  private Class mGoogleCloudMessaging_class = null;
  private Class mInstanceID_class = null;

  private GooglePlayServicesWrapper(Context context) {
    mContext = context.getApplicationContext();
    //lookup the classes and methods
    try {
      mGoogleApiAvailability_class = Class.forName("com.google.android.gms.common.GoogleApiAvailability");
    } catch (ClassNotFoundException e) {
      Log.i(TAG, "GooglePlayServicesWrapper() " + e.getMessage());
    }
    try {
      mGoogleCloudMessaging_class = Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
    } catch (ClassNotFoundException e) {
      Log.i(TAG, "GooglePlayServicesWrapper() " + e.getMessage());
    }
    try {
      mInstanceID_class = Class.forName("com.google.android.gms.iid.InstanceID");
    } catch (ClassNotFoundException e) {
      Log.i(TAG, "GooglePlayServicesWrapper() " + e.getMessage());
    }
  }

  public synchronized static GooglePlayServicesWrapper getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new GooglePlayServicesWrapper(context);
    }
    return sInstance;
  }

  /**
   * Checks to see if Google Play Services is available.
   *
   * @return PLAY_SERVICES_AVAILABLE, PLAY_SERVICES_SERVICE_VERSION_UPDATE_REQUIRED,
   *        PLAY_SERVICES_UNAVAILABLE, PLAY_SERVICES_MAGNET_INCOMPATIBILITY
   */
  int isPlayServicesAvailable() {
    if (mGoogleApiAvailability_class != null) {
      String errorMsg;
      Throwable throwable;
      try {
        Method getInstance_method = mGoogleApiAvailability_class.getDeclaredMethod("getInstance");
        Object googleApiAvailabilityInstance = getInstance_method.invoke(null);

        Method isPlayServicesAvailable_method = mGoogleApiAvailability_class.getDeclaredMethod(
                "isGooglePlayServicesAvailable", Context.class);
        Integer result = (Integer) isPlayServicesAvailable_method.invoke(googleApiAvailabilityInstance, mContext);
        Log.d(TAG, "isPlayServicesAvailable(): isGooglePlayServicesAvailable returned: " + result);
        switch (result) {
          case 0:
            //success
            return PLAY_SERVICES_AVAILABLE;
          case 2:
            //version update required
            return PLAY_SERVICES_SERVICE_VERSION_UPDATE_REQUIRED;
          default:
            //everything else
            return PLAY_SERVICES_UNAVAILABLE;
        }
      } catch (NoSuchMethodException e) {
        // in this case,
        errorMsg = "isPlayServicesAvailable(): Unable to find method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (InvocationTargetException e) {
        errorMsg = "isPlayServicesAvailable(): Unable to invoke method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (IllegalAccessException e) {
        errorMsg = "isPlayServicesAvailable(): Unable to access method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (Exception e) {
        errorMsg = "isPlayServicesAvailable(): Unknown error occurred.";
        throwable = e;
      }
      Log.e(TAG, errorMsg, throwable);
      return PLAY_SERVICES_MAGNET_VERSION_INCOMPATIBILITY;
    } else {
      return PLAY_SERVICES_UNAVAILABLE;
    }
  }

  /**
   * Whether or not GCM is available.
   *
   * @return PLAY_SERVICES_AVAILABLE or PLAY_SERVICES_UNAVAILABLE
   */
  int isGcmAvailable() {
    if (mGoogleCloudMessaging_class != null) {
      return PLAY_SERVICES_AVAILABLE;
    }
    return PLAY_SERVICES_UNAVAILABLE;
  }

  void showErrorNotification(int result) {
    if (mGoogleApiAvailability_class != null) {
      String errorMsg = null;
      Throwable throwable = null;
      try {
        Method getInstance_method = mGoogleApiAvailability_class.getDeclaredMethod("getInstance");
        Object googleApiAvailabilityInstance = getInstance_method.invoke(null);

        Method showErrorNotification_method = mGoogleApiAvailability_class.getDeclaredMethod(
                "showErrorNotification", Context.class, int.class);
        showErrorNotification_method.invoke(googleApiAvailabilityInstance, mContext, result);
      } catch (NoSuchMethodException e) {
        // in this case,
        errorMsg = "showErrorNotification(): Unable to find method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (InvocationTargetException e) {
        errorMsg = "showErrorNotification(): Unable to invoke method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (IllegalAccessException e) {
        errorMsg = "showErrorNotification(): Unable to access method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (Exception e) {
        errorMsg = "showErrorNotification(): Unknown error occurred.";
        throwable = e;
      }
      Log.e(TAG, errorMsg, throwable);
    } else {
      Log.w(TAG, "showErrorNotification(): Unable to find GoogleApiAvailability class");
    }
  }

  /**
   * Register this app for GCM with the specified senderId
   *
   * @deprecated This uses a deprecated method so in turn it is deprecated
   * @param senderId the senderId
   * @return the gcm token
   */
  String registerGcm(String senderId) {
    if (mGoogleCloudMessaging_class != null) {
      String errorMsg;
      Throwable throwable;
      try {
        Method getInstance_method = mGoogleCloudMessaging_class.getDeclaredMethod("getInstance", Context.class);
        Object gcmInstance = getInstance_method.invoke(null, mContext);

        Method register_method = mGoogleCloudMessaging_class.getDeclaredMethod(
                "register", String[].class);
        String gcmRegId = (String) register_method.invoke(gcmInstance, (Object)new String[]{senderId});
        Log.d(TAG, "registerGcm(): completed with regId=" + gcmRegId);
        return gcmRegId;
      } catch (NoSuchMethodException e) {
        // in this case
        errorMsg = "registerGcm(): Unable to find method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (InvocationTargetException e) {
        errorMsg = "registerGcm(): Unable to invoke method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (IllegalAccessException e) {
        errorMsg = "registerGcm(): Unable to access method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (Exception e) {
        errorMsg = "registerGcm(): Unknown error occurred.";
        throwable = e;
      }
      Log.e(TAG, errorMsg, throwable);
    } else {
      Log.w(TAG, "registerGcm(): Unable to find GoogleCloudMessaging class");
    }
    return null;
  }

  /**
   * Register this app for GCM with the specified senderId.
   * NOTE:  This uses the new InstanceID mechanism.
   *
   * @param senderId the senderId
   * @return the gcm token
   */
  String registerGcmNew(String senderId) {
    if (mGoogleCloudMessaging_class != null) {
      String errorMsg;
      Throwable throwable;
      try {
        Method getInstance_method = mInstanceID_class.getDeclaredMethod("getInstance", Context.class);
        Object instanceIDInstance = getInstance_method.invoke(null, mContext);

        Method getToken_method = mInstanceID_class.getDeclaredMethod(
                "getToken", String.class, String.class);
        String gcmRegId = (String) getToken_method.invoke(instanceIDInstance, senderId, "GCM");
        Log.d(TAG, "registerGcmNew(): completed with regId=" + gcmRegId);
        return gcmRegId;
      } catch (NoSuchMethodException e) {
        // in this case
        errorMsg = "registerGcmNew(): Unable to find method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (InvocationTargetException e) {
        errorMsg = "registerGcmNew(): Unable to invoke method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (IllegalAccessException e) {
        errorMsg = "registerGcmNew(): Unable to access method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (Exception e) {
        errorMsg = "registerGcmNew(): Unknown error occurred.";
        throwable = e;
      }
      Log.e(TAG, errorMsg, throwable);
    } else {
      Log.w(TAG, "registerGcmNew(): Unable to find GoogleCloudMessaging class");
    }
    return null;
  }

  /**
   * Retrieve the error string for Play Services Availability
   *
   * @param result the error code
   * @return the error string
   */
  String getErrorString(int result) {
    if (mGoogleApiAvailability_class != null) {
      String errorMsg;
      Throwable throwable;
      try {
        Method getInstance_method = mGoogleApiAvailability_class.getMethod("getInstance");
        Object googleApiAvailabilityInstance = getInstance_method.invoke(null);

        Method getErrorString_method = mGoogleApiAvailability_class.getDeclaredMethod(
                "getErrorString", int.class);
        String errorString = (String) getErrorString_method.invoke(googleApiAvailabilityInstance, result);
        Log.d(TAG, "getErrorString(): success.  returning " + errorString);
        return errorString;
      } catch (NoSuchMethodException e) {
        // in this case,
        errorMsg = "getErrorString(): Unable to find method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (InvocationTargetException e) {
        errorMsg = "getErrorString(): Unable to invoke method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (IllegalAccessException e) {
        errorMsg = "getErrorString(): Unable to access method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (Exception e) {
        errorMsg = "getErrorString(): Unknown error occurred.";
        throwable = e;
      }
      Log.e(TAG, errorMsg, throwable);
    } else {
      Log.w(TAG, "getErrorString(): Unable to find GoogleApiAvailability class");
    }
    return null;
  }

  /**
   * Retrieve the GCM message type.
   *
   * @param intent the GCM intent
   * @return the message type (See GCM_MESSAGE_*)
   */
  int getGcmMessageType(Intent intent) {
    if (mGoogleCloudMessaging_class != null) {
      String errorMsg = null;
      Throwable throwable = null;
      try {
        Method getInstance_method = mGoogleCloudMessaging_class.getDeclaredMethod("getInstance", Context.class);
        Object gcmInstance = getInstance_method.invoke(null, mContext);

        Method getMessageType_method = mGoogleCloudMessaging_class.getDeclaredMethod(
                "getMessageType", Intent.class);
        String type = (String) getMessageType_method.invoke(gcmInstance, intent);
        Log.d(TAG, "getGcmMessageType(): completed with type=" + type);
        if ("deleted_messages".equals(type)) {
          return GCM_MESSAGE_TYPE_DELETED;
        } else if ("gcm".equals(type)) {
          return GCM_MESSAGE_TYPE_MESSAGE;
        } else if ("send_error".equals(type)) {
          return GCM_MESSAGE_TYPE_SEND_ERROR;
        } else if ("send_event".equals(type)) {
          return GCM_MESSAGE_TYPE_SEND_EVENT;
        }
      } catch (NoSuchMethodException e) {
        // in this case
        errorMsg = "getGcmMessageType(): Unable to find method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (InvocationTargetException e) {
        errorMsg = "getGcmMessageType(): Unable to invoke method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (IllegalAccessException e) {
        errorMsg = "getGcmMessageType(): Unable to access method.  This is an " +
                "incompatibility between Google Play Services and Magnet.";
        throwable = e;
      } catch (Exception e) {
        errorMsg = "getGcmMessageType(): Unknown error occurred.";
        throwable = e;
      }
      Log.e(TAG, errorMsg, throwable);
    } else {
      Log.w(TAG, "getGcmMessageType(): Unable to find GoogleCloudMessaging class");
    }
    return -1;
  }
}
