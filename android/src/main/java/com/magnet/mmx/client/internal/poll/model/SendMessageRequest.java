/**
 * File generated by Magnet Magnet Lang Tool on Apr 11, 2016 1:09:57 PM
 * @see {@link http://developer.magnet.com}
 */

package com.magnet.mmx.client.internal.poll.model;

public class SendMessageRequest {

  
  
  private java.util.Map<String, String> content;

  
  private String mmxAppId;

  
  private String userId;

  
  private Boolean receipt;

  
  private String deviceId;

  public java.util.Map<String, String> getContent() {
    return content;
  }

  public String getMmxAppId() {
    return mmxAppId;
  }

  public String getUserId() {
    return userId;
  }

  public Boolean getReceipt() {
    return receipt;
  }

  public String getDeviceId() {
    return deviceId;
  }


  /**
  * Builder for SendMessageRequest
  **/
  public static class SendMessageRequestBuilder {
    private SendMessageRequest toBuild = new SendMessageRequest();

    public SendMessageRequestBuilder() {
    }

    public SendMessageRequest build() {
      return toBuild;
    }

    public SendMessageRequestBuilder content(java.util.Map<String, String> value) {
      toBuild.content = value;
      return this;
    }

    public SendMessageRequestBuilder mmxAppId(String value) {
      toBuild.mmxAppId = value;
      return this;
    }

    public SendMessageRequestBuilder userId(String value) {
      toBuild.userId = value;
      return this;
    }

    public SendMessageRequestBuilder receipt(Boolean value) {
      toBuild.receipt = value;
      return this;
    }

    public SendMessageRequestBuilder deviceId(String value) {
      toBuild.deviceId = value;
      return this;
    }
  }
}
