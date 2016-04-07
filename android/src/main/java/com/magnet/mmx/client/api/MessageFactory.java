/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.api;

import com.google.gson.reflect.TypeToken;
import com.magnet.max.android.Attachment;
import com.magnet.max.android.User;
import com.magnet.max.android.util.MagnetUtils;
import com.magnet.max.android.util.StringUtil;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.util.GsonData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MessageFactory {
  private static final String TAG = "MessageFactory";

  public static final String CONTENT_ATTACHMENTS = "_attachments";

  public static MMXMessage create(MMXTopic topic, com.magnet.mmx.client.common.MMXMessage message) {
    MMXMessage newMessage = convertMessagePayload(message);

    UserCache userCache = UserCache.getInstance();
    HashSet<String> usersToRetrieve = new HashSet<String>();
    usersToRetrieve.add(message.getFrom().getUserId());
    MMXid toUserId = message.getTo();
    if (toUserId != null) {
      usersToRetrieve.add(message.getTo().getUserId());
    }

    //identify all the users that need to be retrieved and populate the cache
    MMXid[] otherRecipients = message.getReplyAll();
    if (otherRecipients != null) {
      //this is normal message.  getReplyAll() returns null for pubsub messages
      for (MMXid mmxId : otherRecipients) {
        Log.d(TAG, "------otherRecipients : " + mmxId.getUserId());
        usersToRetrieve.add(mmxId.getUserId());
      }
    }

    //fill the cache
    userCache.fillCacheByUserId(usersToRetrieve, UserCache.DEFAULT_ACCEPTED_AGE); //five minutes old is ok

    HashSet<User> recipients = new HashSet<User>();
    //populate the values
    User receiver;
    if (null == topic && toUserId != null) {
      receiver = userCache.getByUserId(message.getTo().getUserId());
      if (receiver == null) {
        Log.e(TAG, "fromMMXMessage(): FAILURE: Unable to retrieve receiver from cache:  " +
            "receiver=" + receiver + ".  Message will be dropped.");
        return null;
      }
      Log.d(TAG, "------receiver : " + receiver.getUserIdentifier());
      recipients.add(receiver);
    }
    User sender = userCache.getByUserId(message.getFrom().getUserId());
    if (sender == null) {
      Log.e(TAG, "fromMMXMessage(): FAILURE: Unable to retrieve sender from cache:  " +
          "sender=" + sender + ".  Message will be dropped.");
      return null;
    }

    if (otherRecipients != null) {
      for (MMXid otherRecipient : otherRecipients) {
        recipients.add(userCache.getByUserId(otherRecipient.getUserId()));
      }
    }

    // Extract attachments
    String attachmentsStr = MagnetUtils.trimQuotes(message.getPayload().getAllMetaData().get(CONTENT_ATTACHMENTS));
    if(StringUtil.isNotEmpty(attachmentsStr)) {
      List<Attachment>
          attachments = GsonData.getGson().fromJson(attachmentsStr, new TypeToken<List<Attachment>>() {}.getType());
      if(null != attachments && attachments.size() > 0) {
        newMessage.attachments(attachments);
      }
    }
    Log.d(TAG, "-----------message conversion, topic : " + topic + ", message : " + message);
    if(null != topic) {
      Log.d(TAG, "It's a channel message");
      newMessage.channel(MMXChannel.fromMMXTopic(topic));
    } else if(recipients.size() > 0){
      Log.d(TAG, "It's a in-app message");
      newMessage.recipients(recipients);
    } else {
      throw new IllegalArgumentException("Neither recipients nor channel is set in message.");
    }
    return newMessage
        .sender(sender).id(message.getId())
        .timestamp(message.getPayload().getSentTime());
  }

  private static MMXMessage convertMessagePayload(com.magnet.mmx.client.common.MMXMessage message) {
    //TODO get content type
    String contentType = null;
    MMXMessage newMessage = null;
    if(null == contentType) {
      MMXMessage mmxMessage = new MMXMessage();
      //populate the message content
      HashMap<String, String> content = new HashMap<String, String>();
      for (Map.Entry<String,String> entry : message.getPayload().getAllMetaData().entrySet()) {
        if(!CONTENT_ATTACHMENTS.equals(entry.getKey())) {
          content.put(entry.getKey(), entry.getValue());
        }
      }
      mmxMessage.content(content);
      return mmxMessage;
    } else if(contentType.startsWith("object/")) {
      String typeName = contentType.substring("object/".length());
      MMXMessage typedMessage = new MMXMessage();
      try {
        typedMessage.contentType(typeName).payload(MMXTypedDataConverter.unMarshall(message.getPayload().getDataAsText().toString(), Class.forName(contentType)));
        return typedMessage;
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }

    return null;
  }
}
