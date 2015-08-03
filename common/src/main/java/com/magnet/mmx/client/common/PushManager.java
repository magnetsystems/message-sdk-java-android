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

import java.util.List;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Constants.MessageCommand;
import com.magnet.mmx.protocol.Constants.PingPongCommand;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MsgEvents;
import com.magnet.mmx.protocol.MsgId;
import com.magnet.mmx.protocol.MsgTags;
import com.magnet.mmx.protocol.Notification;
import com.magnet.mmx.protocol.PingPong;
import com.magnet.mmx.util.XIDUtil;

/**
 * This manager allows the caller to send device wake-up messages or auto
 * connecting messages via Push channels.  The destination of the messages must
 * be a device which is capable to receive push messages.
 *
 */
public class PushManager {
  private final static String TAG = "PushManager";
  private MMXConnection mCon;
  private final static Creator sCreator = new Creator() {
    @Override
    public Object newInstance(MMXConnection con) {
      return new PushManager(con);
    }
  };

  // For auto-connect push message
  static class PushMMXIQHandler<Request, Result> extends
                          MMXIQHandler<Request, Result> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_MSG_PUSH;
    }
  }

  // For wakeup-only push message
  static class WakeupMMXIQHandler<Request, Result> extends
                          MMXIQHandler<Request, Result> {
    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_MSG_WAKEUP;
    }
  }

  /**
   * @hide
   * Get the instance of PushManager.
   * @param con
   * @return
   */
  public static PushManager getInstance(MMXConnection con) {
    return (PushManager) con.getManager(TAG, sCreator);
  }

  protected PushManager(MMXConnection con) {
    mCon = con;
    MMXIQHandler<Object, MMXStatus> pushHandler =
        new PushMMXIQHandler<Object, MMXStatus>();
    pushHandler.registerIQProvider();
    MMXIQHandler<Object, MMXStatus> wakeupHandler =
        new WakeupMMXIQHandler<Object, MMXStatus>();
    wakeupHandler.registerIQProvider();
  }

  /**
   * @hide
   * Wake up a client with a Magnet ping/pong payload.  The target device must
   * be registered with push information.  This method is meant for testing.
   * @param userId The target user ID.
   * @param deviceId The device ID of a target user.
   * @param type PingPong or Pong type.
   * @param payload A PingPong JSONifiable object.
   * @return A status.
   * @throws MMXException
   */
  public MMXStatus wakeup(String userId, String deviceId, PingPongCommand type,
                          PingPong payload) throws MMXException {
    String xid = XIDUtil.makeXID(userId, mCon.getAppId(), mCon.getDomain(),
                          deviceId);
    WakeupMMXIQHandler<PingPong, MMXStatus> iqHandler =
        new WakeupMMXIQHandler<PingPong, MMXStatus>();
    iqHandler.sendGetIQ(mCon, xid, type.toString(), payload, MMXStatus.class,
        iqHandler);
    return iqHandler.getResult();
  }

  /**
   * Push a payload to a client and invoke a callback in the client app.
   * @param userId The target user ID.
   * @param deviceId The device ID of a target user.
   * @param type A payload type.
   * @param payload A JSONifiable object.
   * @return A status.
   * @throws MMXException
   * @see {@link DeviceManager#getDevices(String)}
   */
  public MMXStatus push(String userId, String deviceId, String type,
                        Object payload) throws MMXException {
    String xid = XIDUtil.makeXID(userId, mCon.getAppId(), mCon.getDomain(),
                          deviceId);
    PushMMXIQHandler<Object, MMXStatus> iqHandler =
        new PushMMXIQHandler<Object, MMXStatus>();
    iqHandler.sendGetIQ(mCon, xid, type, payload, MMXStatus.class,
        iqHandler);
    return iqHandler.getResult();
  }

  /**
   * @hide
   * Show a notification in the client app.
   * @param userId The target user ID.
   * @param deviceId The device ID of a target user.
   * @param payload A Notification payload.
   * @return A status.
   * @throws MMXException
   * @see {@link DeviceManager#getDevices(String)}
   */
  public MMXStatus notify(String userId, String deviceId, Notification payload)
      throws MMXException {
    String xid = XIDUtil.makeXID(userId, mCon.getAppId(), mCon.getDomain(),
                          deviceId);
    WakeupMMXIQHandler<Notification, MMXStatus> iqHandler =
        new WakeupMMXIQHandler<Notification, MMXStatus>();
    iqHandler.sendGetIQ(mCon, xid, PingPongCommand.notify.toString(), payload,
        MMXStatus.class, iqHandler);
    return iqHandler.getResult();
  }

  /**
   * @hide
   * Get the tags from a push message.
   * @param msgId The push message ID.
   * @return Tags info object.
   * @throws MMXException
   */
  public MsgTags getAllTags(String msgId) throws MMXException {
    return MessageManager.getInstance(mCon).getTags(
        MsgId.IdType.pushMessage, msgId);
  }

  /**
   * @hide
   * Set the tags to a message.  The entire old tags will be overwritten
   * by the new tags.
   * @param msgId The push message ID.
   * @param tags A list of tags, or an empty list.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus setAllTags(String msgId, List<String> tags) throws MMXException {
    return MessageManager.getInstance(mCon).doTags(MessageCommand.setTags,
        MsgId.IdType.pushMessage, msgId, tags);
  }

  /**
   * @hide
   * Add the tags to a message.
   * @param msgId The push message ID.
   * @param tags A list of tags to be added.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus addTags(String msgId, List<String> tags) throws MMXException {
    return MessageManager.getInstance(mCon).doTags(MessageCommand.addTags,
        MsgId.IdType.pushMessage, msgId, tags);
  }

  /**
   * @hide
   * Remove the tags from a message.
   * @param msgId The push message ID.
   * @param tags A list of tags to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeTags(String msgId, List<String> tags) throws MMXException {
    return MessageManager.getInstance(mCon).doTags(MessageCommand.removeTags,
        MsgId.IdType.pushMessage, msgId, tags);
  }

  /**
   * @hide
   * Get the events from a push message.
   * @param msgId The push message ID.
   * @return The events info object.
   * @throws MMXException
   */
  public MsgEvents getEvents(String msgId) throws MMXException {
    return MessageManager.getInstance(mCon).getEvents(
        MsgId.IdType.pushMessage, msgId);
  }

  /**
   * @hide
   * Set the events to a message.  The entire old events will be overwritten
   * by the new events.
   * @param msgId The push message ID.
   * @param events A list of events, or an empty list.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus setEvents(String msgId, List<String> events) throws MMXException {
    return MessageManager.getInstance(mCon).doEvents(MessageCommand.setEvents,
        MsgId.IdType.pushMessage, msgId, events);
  }

  /**
   * @hide
   * Add the events to a message.
   * @param msgId The push message ID.
   * @param events A list of events to be added.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus addEvents(String msgId, List<String> events) throws MMXException {
    return MessageManager.getInstance(mCon).doEvents(MessageCommand.addEvents,
        MsgId.IdType.pushMessage, msgId, events);
  }

  /**
   * @hide
   * Remove the events from a message.
   * @param msgId The push message ID.
   * @param events A list of events to be removed.
   * @return The status.
   * @throws MMXException
   */
  public MMXStatus removeEvents(String msgId, List<String> events) throws MMXException {
    return MessageManager.getInstance(mCon).doEvents(MessageCommand.removeEvents,
        MsgId.IdType.pushMessage, msgId, events);
  }
}
