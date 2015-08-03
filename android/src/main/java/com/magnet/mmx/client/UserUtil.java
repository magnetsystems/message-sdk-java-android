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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;

import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.UserQuery;

/**
 * @hide
 * Utility class to extract data from Android Contacts.
 */
public class UserUtil {
  private final static String TAG = "UserUtil";
  private final static String[] PROFILE_PROJECTION = { 
    Profile._ID,
    Profile.DISPLAY_NAME_PRIMARY,
  };
  private final static String[] PROFILE_DATA_PROJECTION = {
    Data.MIMETYPE,
    Email.ADDRESS,  // same as Data.DATA1
    Phone.NORMALIZED_NUMBER,
    Email.TYPE,   // TYPE_WORK, TYPE_MOBILE...
    Data.IS_PRIMARY,
  };
  private final static String[] SEARCH_PROJECTION = {
    Data.MIMETYPE,
    Email.ADDRESS,  // same as Data.DATA1
    Phone.NORMALIZED_NUMBER,
  };
  private final static int COLUMN_RAW_CONTACTS_ID = 0;  // profile projection
  private final static int COLUMN_DISPLAY_NAME = 1;     // profile projection
  private final static int COLUMN_MIMETYPE = 0;     // profile data/search
  private final static int COLUMN_EMAILADDR = 1;    // profile data/search
  private final static int COLUMN_PHONENUM = 2;     // profile data/search
  private final static int COLUMN_TYPE = 3;         // profile data/search
  private final static int COLUMN_ISPRIMARY = 4;    // profile data/search
  
  /**
   * Get the MMX account info from Android contacts Profile.
   * @param context
   * @return
   */
  public static UserInfo getAccountInfoFromProfile(Context context) {
    Cursor dc = null;
    ContentResolver resolver= context.getContentResolver();
    Cursor pc = resolver.query(Profile.CONTENT_URI, PROFILE_PROJECTION, null, 
        null, null);
    if (pc == null) {
      return null;
    }
    UserInfo acctInfo = null;
    try {
      while (pc.moveToNext()) {
        String displayName = pc.getString(COLUMN_DISPLAY_NAME);
        if (displayName == null || displayName.isEmpty()) {
          continue;
        }
        if (acctInfo == null) {
          acctInfo = new UserInfo();
        }
        acctInfo.setDisplayName(displayName);
        long rawContactsId = pc.getLong(COLUMN_RAW_CONTACTS_ID);
        
        Uri profileDataUri = Uri.withAppendedPath(Profile.CONTENT_URI, "data");
        dc = resolver.query(profileDataUri, PROFILE_DATA_PROJECTION,
            Data.RAW_CONTACT_ID+"=? AND "+Data.MIMETYPE+" IN (?,?)", 
            new String[] { String.valueOf(rawContactsId), 
                    Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE  }, null);
        boolean firstEmail = true, firstPhone = true;
        while (dc != null && dc.moveToNext()) {
          String data;
          int type = dc.getInt(COLUMN_TYPE);
          int isPrimary = dc.getInt(COLUMN_ISPRIMARY);
          boolean isPhone = Phone.CONTENT_ITEM_TYPE.equals(dc.getString(COLUMN_MIMETYPE));
          if (isPhone) {
            data = dc.getString(COLUMN_PHONENUM);
            if (isPrimary == 1 || firstPhone) {
              // TODO: phone number is not supported yet.
//            acctInfo.setPhone(data);
              firstPhone = false;
            }
          } else {
            data = dc.getString(COLUMN_EMAILADDR);
            if (isPrimary == 1 || firstEmail) {
              acctInfo.setEmail(data);
              firstEmail = false;
            }
          }
        }
      }
      return acctInfo;
    } finally {
      pc.close();
      if (dc != null) {
        dc.close();
      }
    }
  }
  
  /**
   * Use Android Contacts to get the search attributes for MMX users.
   * @param context
   * @return List of users, or null if error.
   */
  public static List<MMXAttribute<UserQuery.Type>> getAttrsFromContacts(Context context) {
    ContentResolver resolver= context.getContentResolver();
    Cursor cc = null;
    try {
      // Use all email addresses and phone numbers as search criteria.
      cc = resolver.query(Data.CONTENT_URI, SEARCH_PROJECTION,
          Data.MIMETYPE+" IN (?,?)", new String[] {
            Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE }, null);
      ArrayList<MMXAttribute<UserQuery.Type>> attr = 
          new ArrayList<MMXAttribute<UserQuery.Type>>(cc.getCount());
      while (cc.moveToNext()) {
        String data;
        UserQuery.Type type;
        boolean isPhone = Phone.CONTENT_ITEM_TYPE.equals(cc.getString(COLUMN_MIMETYPE));
        if (isPhone) {
          type = UserQuery.Type.phone;
          data = cc.getString(COLUMN_PHONENUM);
        } else {
          type = UserQuery.Type.email;
          data = cc.getString(COLUMN_EMAILADDR);
        }
        if (data == null || data.isEmpty()) {
          continue;
        }
        attr.add(new MMXAttribute<UserQuery.Type>(type, data));
      }
      return attr;
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    } finally {
      if (cc != null) {
        cc.close();
      }
    }
  }
}
