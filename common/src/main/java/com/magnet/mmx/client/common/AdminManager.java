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

import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.AppDelete;
import com.magnet.mmx.protocol.AppRead;
import com.magnet.mmx.protocol.AppUpdate;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.util.XIDUtil;

/**
 * @hide
 * Administrative Manager to create/update/delete/get the application.
 *
 */
public class AdminManager {
  private final static String TAG = "AdminManager";
  private MMXConnection mCon;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new AdminManager(con);
    }
  };

  static class AppMgmtIQHandler<Request, Result>
                      extends MMXIQHandler<Request, Result> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_APP;
    }
  }

  /**
   * @hide
   * @param con
   * @return
   */
  public static AdminManager getInstance(MMXConnection con) {
    return (AdminManager) con.getManager(TAG, sCreator);
  }

  protected AdminManager(MMXConnection con) {
    mCon = con;
    MMXIQHandler<AppCreate.Request, AppCreate.Response> iqHandler =
        new AppMgmtIQHandler<AppCreate.Request, AppCreate.Response>();
    iqHandler.registerIQProvider();
  }

  /**
   * Create the application in MMX.  If the server user ID is an e-mail address,
   * it will be escaped in the <code>rqt</code> as a side-effect.
   * @param rqt
   * @return
   * @throws MMXException
   */
  public AppCreate.Response createApp(AppCreate.Request rqt) throws MMXException {
    rqt.setServerUserId(XIDUtil.escapeNode(rqt.getServerUserId()));
    AppMgmtIQHandler<AppCreate.Request, AppCreate.Response> iqHandler =
        new AppMgmtIQHandler<AppCreate.Request, AppCreate.Response>();
    iqHandler.sendSetIQ(mCon, Constants.AppMgmtCommand.create.toString(), rqt,
        AppCreate.Response.class, iqHandler);
    return iqHandler.getResult();
  }

  public MMXStatus updateApp(String appId, AppUpdate rqt) throws MMXException {
    rqt.setAppId(appId);
    AppMgmtIQHandler<AppUpdate, MMXStatus> iqHandler =
        new AppMgmtIQHandler<AppUpdate, MMXStatus>();
    iqHandler.sendSetIQ(mCon, Constants.AppMgmtCommand.update.toString(), rqt,
        MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  /**
   * Get the registered application information.  The server user ID will be
   * in human readable format.
   * @param appId
   * @return
   * @throws MMXException
   */
  public AppRead.Response readApp(String appId) throws MMXException {
    AppRead.Request rqt = new AppRead.Request();
    rqt.setAppId(appId);
    AppMgmtIQHandler<AppRead.Request, AppRead.Response> iqHandler =
        new AppMgmtIQHandler<AppRead.Request, AppRead.Response>();
    iqHandler.sendGetIQ(mCon, Constants.AppMgmtCommand.read.toString(), rqt,
        AppRead.Response.class, iqHandler);
    AppRead.Response resp = iqHandler.getResult();
    resp.setServerUserId(XIDUtil.getReadableNode(resp.getServerUserId()));
    return resp;
  }

  public AppDelete.Response deleteApp(String appId) throws MMXException {
    AppDelete.Request rqt = new AppDelete.Request();
    rqt.setAppId(appId);
    AppMgmtIQHandler<AppDelete.Request, AppDelete.Response> iqHandler =
        new AppMgmtIQHandler<AppDelete.Request, AppDelete.Response>();
    iqHandler.sendSetIQ(mCon, Constants.AppMgmtCommand.delete.toString(), rqt,
        AppDelete.Response.class, iqHandler);
    // TODO: remove the app root node from pubsub.
    return iqHandler.getResult();
  }

}
