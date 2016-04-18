/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.survey.model;

public class SentList {

  
  
  private String recipientUserId;

  
  private String messageId;

  
  private String deviceId;

  public String getRecipientUserId() {
    return recipientUserId;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getDeviceId() {
    return deviceId;
  }


  /**
  * Builder for SentList
  **/
  public static class SentListBuilder {
    private SentList toBuild = new SentList();

    public SentListBuilder() {
    }

    public SentList build() {
      return toBuild;
    }

    public SentListBuilder recipientUserId(String value) {
      toBuild.recipientUserId = value;
      return this;
    }

    public SentListBuilder messageId(String value) {
      toBuild.messageId = value;
      return this;
    }

    public SentListBuilder deviceId(String value) {
      toBuild.deviceId = value;
      return this;
    }
  }
}