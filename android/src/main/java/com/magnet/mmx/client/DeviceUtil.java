/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.magnet.mmx.client;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.protocol.CarrierEnum;
import com.magnet.mmx.util.CryptoUtil;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * A utility class for devices.  This util requires READ_PHONE_STATE, 
 * ACCESS_NETWORK_STATE and BLUETOOTH permissions.  The BLUETOOTH permission is
 * required if {@link #genDeviceId(Context)} or{@link #getBTAddress(Context)} is
 * called.
 */
public class DeviceUtil {
  private final static String TAG = "DeviceUtil";
  private final static boolean HIDE_PHONE_NUMBER = false;
  private final static String ISO_US = "us";
  private static String sMyLineNumber;
  
  /**
   * Get my last known location using the passive provider.
   * TODO: we should add Google Location Service from Play Service too.
   * @param context
   * @return The last known location, or null.
   */
  public static Address getCurrentLocation(Context context) {
    Location bestCurLoc = null;
    Address curAddr = null;
    LocationManager locMgr = (LocationManager) context.getSystemService(
        Context.LOCATION_SERVICE);
    String[] providers = { // LocationManager.GPS_PROVIDER,
                           // LocationManager.NETWORK_PROVIDER,
                           LocationManager.PASSIVE_PROVIDER,
                           };
    for (String provider : providers) {
      try {
        Location curLoc = locMgr.getLastKnownLocation(provider);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "provider="+provider+" => loc="+curLoc);
        }
        if (curLoc == null) {
          continue;
        }
        if (bestCurLoc == null || (curLoc.hasAccuracy() && 
            curLoc.getAccuracy() < bestCurLoc.getAccuracy())) {
          bestCurLoc = curLoc;
        }
      } catch (Exception e) {
        Log.e(TAG, "getCurrentLocation() error", e);
      }
    }
    if (bestCurLoc == null) {
      Log.e(TAG, "@@@ getCurrentLocation() return null; did you turn on Location Service?");
      return null;
    }
    curAddr = new Address(Locale.getDefault());
    curAddr.setLatitude(bestCurLoc.getLatitude());
    curAddr.setLongitude(bestCurLoc.getLongitude());
    return curAddr;
  }

  /**
   * Get my line1 number.  If not found, return null.  For US, it will be always
   * prepended with '1' if it does not have one.  It is just a workaround for 
   * the server not able to do phone number matching.
   * @param context
   * @return My line number or null.
   */
  public static String getLineNumber(Context context) {
    if (sMyLineNumber != null) {
      return sMyLineNumber.isEmpty() ? null : sMyLineNumber;
    }
    if (sMyLineNumber == null) {
      TelephonyManager mgr = (TelephonyManager) context.getSystemService(
          Context.TELEPHONY_SERVICE);
      sMyLineNumber = mgr.getLine1Number();  // can be empty
    }
    if (sMyLineNumber == null) {
      sMyLineNumber = "";
    }
    if (sMyLineNumber.isEmpty()) {
      return null;
    }
    return sMyLineNumber = normalizePhoneNumber(context, sMyLineNumber);
  }

  /**
   * Strip non-numeric characters.  Currently a hack is provided to insert the
   * leading '1' if missing for US carrier.  It does not handle roaming with
   * US SIM card.  This hack is not needed if the server stores the phone
   * number in reverse order and does pattern matching.
   * @param phoneNumber
   * @return
   */
  public static String normalizePhoneNumber(Context context, String phoneNumber) {
    StringBuilder sb = new StringBuilder();
    char[] ca = phoneNumber.toCharArray();
    for (int i = 0; i < ca.length; i++) {
      char c = ca[i];
      if (c >= '0' && c <= '9') {
        sb.append(c);
      }
    }
    TelephonyManager telMgr = (TelephonyManager) context.getSystemService(
        Context.TELEPHONY_SERVICE);
//    Log.d(TAG, "NetworkCountryIso()="+telMgr.getNetworkCountryIso()+
//        " SimCountryIso="+telMgr.getSimCountryIso());
    // For US, it requires 11 digits including the leading '1'.
    if (ISO_US.equals(telMgr.getNetworkCountryIso()) && sb.length() == 10) {
      sb.insert(0, '1');
    }
    return sb.toString();
  }
  
  /**
   * Scramble a sensitive value.  Currently it is disabled.
   * @param str
   * @return
   */
  public static String scramble(String str) {
    if (!HIDE_PHONE_NUMBER) {
      return str;
    }
    if (str == null)
      return null;
    return CryptoUtil.generateMd5(str);
  }
  
  /**
   * Get the bluetooth MAC address.
   * @param context
   * @return Bluetooth MAC address (with colons), or null
   */
  public static String getBTAddress(Context context) {
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    return (adapter == null) ? null : adapter.getAddress();
  }
  
  /**
   * Generate a unique ID for a non-sharable device.  The ID selection is
   * based on this precedence order: phone number, vehicle number, BT MAC 
   * address and UUID.
   * @param context
   * @return
   */
  public static String genDeviceId(Context context) {
    String deviceId = scramble(getLineNumber(context));
    if ((deviceId == null) && ((deviceId = getBTAddress(context)) == null)) {
      deviceId = UUID.randomUUID().toString();
    }
    return deviceId;
  }
  
  // Mapping MCC+MNC to a carrier
  private final static HashMap<String, CarrierEnum> sCarriers = 
      new HashMap<String, CarrierEnum>() {{
        put("310004", CarrierEnum.VERIZON);
        put("310005", CarrierEnum.VERIZON);
        put("310012", CarrierEnum.VERIZON);
        put("310016", CarrierEnum.CRICKET);
        put("310026", CarrierEnum.TMOBILE);
        put("310030", CarrierEnum.ATT);
        put("310053", CarrierEnum.VIRGIN);
        put("310070", CarrierEnum.ATT);
        put("310090", CarrierEnum.CRICKET);
        put("310120", CarrierEnum.SPRINT);
        put("310150", CarrierEnum.CRICKET);
        put("310170", CarrierEnum.ATT);
        put("310260", CarrierEnum.TMOBILE);
        put("310410", CarrierEnum.ATT);
        put("310490", CarrierEnum.TMOBILE);
        put("310560", CarrierEnum.ATT);
        put("310680", CarrierEnum.ATT);
        put("310980", CarrierEnum.ATT);
        put("311480", CarrierEnum.VERIZON);
        put("311481", CarrierEnum.VERIZON);
        put("311482", CarrierEnum.VERIZON);
        put("311483", CarrierEnum.VERIZON);
        put("311484", CarrierEnum.VERIZON);
        put("311485", CarrierEnum.VERIZON);
        put("311486", CarrierEnum.VERIZON);
        put("311487", CarrierEnum.VERIZON);
        put("311488", CarrierEnum.VERIZON);
        put("311489", CarrierEnum.VERIZON);
        put("311490", CarrierEnum.SPRINT);
        put("311660", CarrierEnum.METROPCS);
        put("311870", CarrierEnum.BOOST);
        put("311999", CarrierEnum.TRACFONE);        
  }};
  
  /**
   * Get a carrier using the SIM operator or network operator.  For CDMA phones,
   * if the current active network is non-mobile, it will return null.
   * @param context
   * @return null or a carrier.
   */
  public static CarrierEnum getCarrier(Context context) {
    TelephonyManager telMgr = (TelephonyManager)
        context.getSystemService(Context.TELEPHONY_SERVICE);
    String mccmnc = padMCCMNC(telMgr.getSimOperator());
    if (mccmnc == null || mccmnc.isEmpty()) {
      // Cannot detect a SIM card operator, try network operator.
      ConnectivityManager conMgr = (ConnectivityManager)
          context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
      if (networkInfo != null && networkInfo.getType() == 
          ConnectivityManager.TYPE_MOBILE) {
        mccmnc = padMCCMNC(telMgr.getNetworkOperator());
        if (mccmnc == null || mccmnc.isEmpty()) {
          return null;
        }
      }
    }
    return sCarriers.get(mccmnc);
  }
  
  // If the MCC+MNC is 5 digit, pad a leading 0 to MNC.
  private static String padMCCMNC(String mccmnc) {
    if ((mccmnc != null) && (mccmnc.length() == 5)) {
      return mccmnc.substring(0, 3)+'0'+mccmnc.substring(3, 5);
    }
    return mccmnc;
  }
}
