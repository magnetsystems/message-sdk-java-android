/*   Copyright (c) 2016 Magnet Systems, Inc.
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
package com.magnet.mmx.client.common;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smackx.privacy.PrivacyList;
import org.jivesoftware.smackx.privacy.PrivacyListManager;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem.Type;

import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.util.XIDUtil;

/**
 * Manage the private list for user blocking.  Typically the caller will set up
 * a privacy list {@link #setPrivacyList(List)} and then enable the privacy list
 * {@link #enablePrivacyList(boolean)}.
 *
 */
public class PrivacyManager {
  private final static String TAG = "PrivacyManager";
  private final static String DEFAULT_LIST_NAME = "default";
  private final static String ERRMSG_NOT_GET_PRIVLIST = "Unable to get privacy list";
  private final static String ERRMSG_NOT_SET_PRIVLIST = "Unable to set privacy list";
  private final static String ERRMSG_NOT_DEL_PRIVLIST = "Unable to delete privacy list";
  private final static String ERRMSG_PRIVLIST_NOT_FOUND = "Privacy list not found";
  private final static String ERRMSG_PRIVLIST_OP_FAILED = "Operation on pivacy list failed";
  private final MMXConnection mCon;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new PrivacyManager(con);
    }
  };

  private PrivacyManager(MMXConnection con) {
    mCon = con;
  }

  /**
   * @hide
   * Get the instance per connection.
   * @param con
   * @return
   */
  public static PrivacyManager getInstance(MMXConnection con) {
    return (PrivacyManager) con.getManager(TAG, sCreator);
  }

  /**
   * Get the global privacy list.
   * @return  An empty list or a list of blocked users.
   * @throws MMXException
   */
  public List<MMXid> getPrivacyList() throws MMXException {
    return getPrivacyList(DEFAULT_LIST_NAME);
  }

  List<MMXid> getPrivacyList(String listName) throws MMXException {
    if (!mCon.isAuthenticated()) {
      throw new MMXException("Not connected");
    }
    try {
      PrivacyListManager plmgr = PrivacyListManager.getInstanceFor(mCon.getXMPPConnection());
      PrivacyList plist = plmgr.getPrivacyList(listName);
      List<MMXid> blacklist = new ArrayList<MMXid>(plist.getItems().size());
      for (PrivacyItem item : plist.getItems()) {
        if (item.getType() == Type.jid) {
          MMXid xid = new MMXid(XIDUtil.getUserId(item.getValue()), null, null);
          blacklist.add(xid);
        }
      }
      return blacklist;
    } catch (XMPPErrorException e) {
      if (!Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new MMXException(ERRMSG_NOT_GET_PRIVLIST, e);
      }
      // The privacy list does not exist.
      return new ArrayList<MMXid>(0);
    } catch (Throwable e) {
      throw new MMXException(ERRMSG_NOT_GET_PRIVLIST, e);
    }
  }

  /**
   * Set a list of users to be blocked in the global privacy list.  The privacy
   * list is not active until it is enabled by {@link #enablePrivacyList(boolean)}.
   * If <code>list</code> is empty, it is clearing out the privacy list, but the
   * privacy is not deleted.
   * @param list A non-null list.
   * @throws MMXException
   * @see {@link #enablePrivacyList(boolean)}
   */
  public void setPrivacyList(List<MMXid> list) throws MMXException {
    setPrivacyList(DEFAULT_LIST_NAME, list);
  }

  void setPrivacyList(String listName, List<MMXid> list) throws MMXException {
    if (list == null) {
      throw new IllegalArgumentException("Privacy list cannot be null");
    }
    PrivacyListManager plmgr = PrivacyListManager.getInstanceFor(mCon.getXMPPConnection());
    List<PrivacyItem> items = new ArrayList<PrivacyItem>(list.size());
    for (MMXid xid : list) {
      // Blocking everything: iq, message, and presence
      PrivacyItem item = new PrivacyItem(Type.jid, XIDUtil.makeXID(xid.getUserId(),
          mCon.getAppId(), mCon.getDomain()), false, 0);
      items.add(item);
    }
    try {
      try {
        plmgr.updatePrivacyList(listName, items);
      } catch (XMPPErrorException e) {
        if (!Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
          throw new MMXException(ERRMSG_NOT_SET_PRIVLIST, e);
        }
        // This should not happen with Smack because createPrivacyList and
        // updatePriavcyList are same.
        plmgr.createPrivacyList(listName, items);
      }
    } catch (NoResponseException e) {
      throw new MMXException(ERRMSG_NOT_SET_PRIVLIST, e);
    } catch (NotConnectedException e) {
      throw new MMXException(ERRMSG_NOT_SET_PRIVLIST, e);
    } catch (XMPPException e) {
      throw new MMXException(ERRMSG_NOT_SET_PRIVLIST, e);
    }
  }

  /**
   * Delete the global privacy list.
   * @throws MMXException
   */
  public void deletePrivacyList() throws MMXException {
    deletePrivacyList(DEFAULT_LIST_NAME);
  }

  // Delete a give privacy list.
  void deletePrivacyList(String listName) throws MMXException {
    PrivacyListManager plmgr = PrivacyListManager.getInstanceFor(mCon.getXMPPConnection());
    try {
      plmgr.deletePrivacyList(listName);
    } catch (XMPPErrorException e) {
      if (!Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new MMXException(ERRMSG_NOT_DEL_PRIVLIST, e);
      }
    } catch (Throwable e) {
      throw new MMXException(ERRMSG_NOT_DEL_PRIVLIST, e);
    }
  }

  /**
   * Enable or disable the global privacy list.  The privacy list must be
   * created via {@link #setPrivacyList(List)} prior to this operation.
   * @param enable true for enabling, false for disabling.
   * @throws MMXException Privacy list does not exist, code 404
   * @throws MMXException Privacy list operation failed
   */
  public void enablePrivacyList(boolean enable) throws MMXException {
    try {
      PrivacyListManager plmgr = PrivacyListManager.getInstanceFor(mCon.getXMPPConnection());
      if (enable) {
        plmgr.setDefaultListName(DEFAULT_LIST_NAME);
      } else {
        plmgr.declineDefaultList();
      }
    } catch (XMPPErrorException e) {
      if (!Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new MMXException(ERRMSG_PRIVLIST_OP_FAILED, e);
      }
      throw new MMXException(ERRMSG_PRIVLIST_NOT_FOUND, 404, e);
    } catch (Throwable e) {
      throw new MMXException(ERRMSG_PRIVLIST_OP_FAILED, e);
    }
  }

  /**
   * Check if the global privacy list enabled.
   * @return true if enabled, false if disabled.
   * @throws MMXException
   */
  public boolean isPrivacyListEnabled() throws MMXException {
    try {
      PrivacyListManager plmgr = PrivacyListManager.getInstanceFor(mCon.getXMPPConnection());
      PrivacyList pl = plmgr.getDefaultList();
      return (pl != null) && DEFAULT_LIST_NAME.equals(pl.getName());
    } catch (XMPPErrorException e) {
      if (!Condition.item_not_found.equals(e.getXMPPError().getCondition())) {
        throw new MMXException(ERRMSG_PRIVLIST_OP_FAILED, e);
      }
      return false;
    } catch (Throwable e) {
      throw new MMXException(ERRMSG_PRIVLIST_OP_FAILED, e);
    }
  }
}
