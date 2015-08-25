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

package com.magnet.mmx.client.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.UserCommand;
import com.magnet.mmx.protocol.Constants.UserCreateMode;
import com.magnet.mmx.protocol.MMXAttribute;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.SearchAction;
import com.magnet.mmx.protocol.StatusCode;
import com.magnet.mmx.protocol.TagSearch;
import com.magnet.mmx.protocol.TagSearch.Operator;
import com.magnet.mmx.protocol.UserCreate;
import com.magnet.mmx.protocol.UserId;
import com.magnet.mmx.protocol.UserInfo;
import com.magnet.mmx.protocol.UserQuery;
import com.magnet.mmx.protocol.UserTags;
import com.magnet.mmx.util.TagUtil;
import com.magnet.mmx.util.XIDUtil;

/**
 * Account Manager allows user to change the password, update the display
 * name and email address, and query for users.
 */
public class AccountManager {
  private final static String TAG = "AccountManager";
  private MMXConnection mCon;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new AccountManager(con);
    }
  };

  static class UserMMXIQHandler<Request, Result>
                      extends MMXIQHandler<Request, Result> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_USER;
    }
  }

  /**
   * @hide
   * Get an instance per connection.
   * @param con
   * @return
   */
  public static AccountManager getInstance(MMXConnection con) {
    return (AccountManager) con.getManager(TAG, sCreator);
  }

  protected AccountManager(MMXConnection con) {
    mCon = con;
    MMXIQHandler<Object, MMXStatus> iqHandler =
        new UserMMXIQHandler<Object, MMXStatus>();
    iqHandler.registerIQProvider();
  }

  /**
   * @hide
   * Create an anonymous account or regular user account.  In order to use this
   * method, the caller must provide a valid privilege key, API key and App ID.
   * There is a user ID length validation for regular user account.
   * @param account
   * @return The status.
   * @throws MMXException Not supported, or user creation error.
   */
  public MMXStatus createAccount(UserCreate account) throws MMXException {
    // Only check regular user account length; anonymous user ID is longer.
    if (account.getCreateMode() == UserCreateMode.UPGRADE_USER) {
      String userId = account.getUserId();
      if (userId == null || userId.length() < Constants.MMX_MIN_USERID_LEN ||
          userId.length() > Constants.MMX_MAX_USERID_LEN) {
        throw new MMXException("The user ID must be "+
            Constants.MMX_MIN_USERID_LEN+" to "+
            Constants.MMX_MAX_USERID_LEN+" characters long", StatusCode.BAD_REQUEST);
      }
    }
    try {
      UserMMXIQHandler<UserCreate, MMXStatus> iqHandler =
          new UserMMXIQHandler<UserCreate, MMXStatus>();
      iqHandler.sendSetIQ(mCon, Constants.UserCommand.create.toString(),
          account, MMXStatus.class, iqHandler);
      return iqHandler.getResult();
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get the account information of the current user.
   * @return The current user's account information.
   * @throws MMXException
   */
  public UserInfo getUserInfo() throws MMXException {
    return getUserInfo(mCon.getUserId());
  }

  private static class MapOfUserInfo extends HashMap<String, UserInfo> {
    public MapOfUserInfo() {
      super();
    }

    public MapOfUserInfo(int size) {
      super(size);
    }
  }

  /**
   * Get user information of multiple users by their user ID's.
   * @param uids A set of unescaped user ID's.
   * @return A map of user ID (key) and user Info (value).
   * @throws MMXException
   */
  public Map<String, UserInfo> getUserInfo(Set<String> uids) throws MMXException {
    ArrayList<UserId> userIds = new ArrayList<UserId>(uids.size());
    for (String uid : uids) {
      UserId userId = new UserId(XIDUtil.escapeNode(uid));
      userIds.add(userId);
    }
    try {
      UserMMXIQHandler<List<UserId>, MapOfUserInfo> iqHandler = new
          UserMMXIQHandler<List<UserId>, MapOfUserInfo>();
      iqHandler.sendGetIQ(mCon, Constants.UserCommand.list.toString(),
          userIds, MapOfUserInfo.class, iqHandler);
      MapOfUserInfo usersInfo = iqHandler.getResult();
      return usersInfo;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Get a user information of the specified user ID.
   * @param uid An un-escaped user ID.
   * @return A user info.
   * @throws MMXException User not found.
   */
  public UserInfo getUserInfo(String uid) throws MMXException {
    try {
      UserId userId = new UserId(XIDUtil.escapeNode(uid));
      UserMMXIQHandler<UserId, UserInfo> iqHandler = new
          UserMMXIQHandler<UserId, UserInfo>();
      iqHandler.sendGetIQ(mCon, Constants.UserCommand.get.toString(),
          userId, UserInfo.class, iqHandler);
      UserInfo userInfo = iqHandler.getResult();
      userInfo.setUserId(XIDUtil.getReadableNode(userInfo.getUserId()));
      return userInfo;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Update the display name and email for the current authenticated user.
   * The userID in <code>info</code> will be ignored.
   * @param info A non-null account info to be updated.
   * @return The status.
   * @throws MMXException User not found.
   */
  public MMXStatus updateAccount(UserInfo info) throws MMXException {
    try {
      UserMMXIQHandler<UserInfo, MMXStatus> iqHandler = new
          UserMMXIQHandler<UserInfo, MMXStatus>();
      iqHandler.sendSetIQ(mCon, Constants.UserCommand.update.toString(),
          info, MMXStatus.class, iqHandler);
      return iqHandler.getResult();
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Change the password for the current authenticated user.
   * @param newPassword The new password.
   * @throws MMXException Not connected to MMX Server.
   */
  public void changePassword(String newPassword) throws MMXException {
    try {
      org.jivesoftware.smack.AccountManager xmppActMgr =
        org.jivesoftware.smack.AccountManager.getInstance(mCon.getXMPPConnection());
      xmppActMgr.changePassword(newPassword);
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Request to reset the password.  The new temporary password will be sent
   * via e-mail (or SMS) and the user will login with a temporary password and
   * then set the permanent password via {@link #changePassword(String)}
   * @param request
   * @throws MMXException
   */
//  public MMXStatus resetPassword(UserReset request) throws MMXException {
//    boolean signoff = false;
//    try {
//      // Use a guest account to reset password.
//      if (!mCon.isAuthenticated()) {
//        mCon.loginAsGuest();
//        signoff = true;
//      } else if (!mCon.isGuest()) {
//        throw new MMXException(
//            "Cannot reset password while being logged in; please logout first");
//      }
//
//      UserMMXIQHandler<UserReset, MMXStatus> iqHandler =
//          new UserMMXIQHandler<UserReset, MMXStatus>();
//      iqHandler.sendSetIQ(mCon, Constants.UserCommand.reset.toString(),
//          request, MMXStatus.class, (IQListener<MMXStatus>) iqHandler);
//      return iqHandler.getResult();
//    } catch (MMXException e) {
//      throw e;
//    } catch (Throwable e) {
//      throw new MMXException(e);
//    } finally {
//      if (signoff) {
//        mCon.logout();
//      }
//    }
//  }

  /**
   * @hide
   * Bulk search for users.  Caller provides the bulk search attributes for a
   * list of people and all MMX users match with any (i.e. OR operation) of the
   * attributes will be returned.  Although this search is designed for bulk
   * search, it can be used to search a user with multiple criteria (see the
   * teh example of "Chris Doe" below.)
   * <pre>
   * MMXAttribute<Type>[] people = {
   *  new MMXAttribute<Type>(Type.email, "john.doe@acme.com"),
   *  new MMXAttribute<Type>(Type.email, "christine.doe@first.com"),
   *  new MMXAttribute<Type>(Type.name, "Chris Doe"),     // same person as christine.doe@first.com
   *  new MMXAttribute<Type>(Type.phone, "650-555-1212"); // same person as christine.doe@first.com
   * };
   * Response resp = new AccountManager.bulkSearch(Array.asList(people), 500);
   * </pre>
   * @param attrs A bulk list of users' search attribute and value.
   * @param maxRows null for unlimit, or a max number of rows.
   * @return A query response.
   * @throws MMXException Not connected to MMX Server.
   * @deprecated Use {@link #searchBy(com.magnet.mmx.protocol.UserQuery.Operator, com.magnet.mmx.protocol.UserQuery.Search, Integer)}
   */
  @Deprecated
  public UserQuery.Response bulkSearch(List<MMXAttribute<UserQuery.Type>> attrs,
                                        Integer maxRows) throws MMXException {
    try {
      UserQuery.BulkSearchRequest rqt = new UserQuery.BulkSearchRequest(attrs, maxRows);
      UserMMXIQHandler<UserQuery.BulkSearchRequest, UserQuery.Response> iqHandler =
          new UserMMXIQHandler<UserQuery.BulkSearchRequest, UserQuery.Response>();
      iqHandler.sendGetIQ(mCon, Constants.UserCommand.query.toString(), rqt,
          UserQuery.Response.class, iqHandler);
      return iqHandler.getResult();
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  /**
   * Search for users with the matching attributes.  The multi-valued tags, if
   * specified, are compared with any matches (i.e. implicit OR); no wild card
   * characters are accepted in each tag.
   * @param operator The AND or OR operator.
   * @param attr Single or multi-values attributes.
   * @param maxRows null for unlimit, or a max number of rows to be returned.
   * @return The search result.
   * @throws MMXException
   */
  public UserQuery.Response searchBy(SearchAction.Operator operator,
                  UserQuery.Search attr, Integer maxRows) throws MMXException {
    try {
      UserQuery.SearchRequest rqt = new UserQuery.SearchRequest(operator, attr,
          0, (maxRows == null) ? -1 : maxRows.intValue());
      UserMMXIQHandler<UserQuery.SearchRequest, UserQuery.Response> iqHandler =
          new UserMMXIQHandler<UserQuery.SearchRequest, UserQuery.Response>();
      iqHandler.sendGetIQ(mCon, Constants.UserCommand.search.toString(), rqt,
          UserQuery.Response.class, iqHandler);
      return iqHandler.getResult();
    } catch (MMXException e) {
      throw e;
    } catch (Throwable e) {
      throw new MMXException(e.getMessage(), e);
    }
  }

  private static class UserList extends ArrayList<MMXid> {
  }

  /**
   * @hide
   * Search users by all matching tags or any matching tags.
   * @param tags A list of tags.
   * @param matchAll true for all matching tags, false for any matching tags.
   * @return A list of matching users.
   * @throws MMXException
   */
  public List<MMXid> searchByTags(List<String> tags, boolean matchAll)
                      throws MMXException {
    TagSearch rqt = new TagSearch(matchAll ? Operator.AND : Operator.OR, tags);
    UserMMXIQHandler<TagSearch, UserList> iqHandler =
        new UserMMXIQHandler<TagSearch, UserList>();
    iqHandler.sendGetIQ(mCon, Constants.UserCommand.searchByTags.toString(),
        rqt, UserList.class, iqHandler);
    return iqHandler.getResult();
  }

  /**
   * Get the tags for the current user.
   * @return A tag info object.
   * @throws MMXException
   */
  public UserTags getAllTags() throws MMXException {
    UserMMXIQHandler<Void, UserTags> iqHandler =
        new UserMMXIQHandler<Void, UserTags>();
    iqHandler.sendGetIQ(mCon, Constants.UserCommand.getTags.toString(), null,
        UserTags.class, iqHandler);
    UserTags userTags = iqHandler.getResult();
    return userTags;
  }

  /**
   * Set the tags for the current user.  If the list is null or empty, all tags
   * will be removed.
   * @param tags A list of tags, or null.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus setAllTags(List<String> tags) throws MMXException {
    return doTags(UserCommand.setTags, tags);
  }

  /**
   * Add tags to the current user.
   * @param tags A list of tags to be added.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus addTags(List<String> tags) throws MMXException {
    return doTags(UserCommand.addTags, tags);
  }

  /**
   * Remove tags from the current user.
   * @param tags A list of tags to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeTags(List<String> tags) throws MMXException {
    return doTags(UserCommand.removeTags, tags);
  }

  private MMXStatus doTags(UserCommand cmd, List<String> tags) throws MMXException {
    if (cmd == UserCommand.setTags) {
      if (tags == null) {
        tags = new ArrayList<String>(0);
      }
      if (!tags.isEmpty()) {
        validateTags(tags);
      }
    } else {
      validateTags(tags);
    }
    UserTags userTags = new UserTags(tags);
    UserMMXIQHandler<UserTags, MMXStatus> iqHandler =
        new UserMMXIQHandler<UserTags, MMXStatus>();
    iqHandler.sendSetIQ(mCon, cmd.toString(), userTags, MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  private void validateTags(List<String> tags) throws MMXException {
    if (tags == null || tags.isEmpty()) {
      throw new MMXException("List of tags cannot be null or empty", StatusCode.BAD_REQUEST);
    }
    try {
      TagUtil.validateTags(tags);
    } catch (IllegalArgumentException e) {
      throw new MMXException(e.getMessage(), StatusCode.BAD_REQUEST);
    }
  }
}
