/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.AssertionUtils;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.common.Log;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageHelper {

  private static final String TAG = MessageHelper.class.getSimpleName();

  public static void sendMessage(final MMXMessage message,
      final ExecMonitor<String, ApiError> sendMonitor, final ExecMonitor.Status expectedSendStatus,
      final ExecMonitor<MMXMessage, ApiError> receiveMonitor, final ExecMonitor.Status expectedReceiveStatus,
      final ExecMonitor<Boolean, ApiError> ackSend, final ExecMonitor<String, Void> ackReceiveMonitor) {
    //assertTrue(MMX.getMMXClient().isConnected());
    MMX.start();
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(final MMXMessage messageReceived) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());

        if(messageReceived.getId().equals(message.getId())) {
          if (null != receiveMonitor) receiveMonitor.invoked(messageReceived);
        } else {
          Log.d(TAG, "onMessageReceived(): " + messageReceived.getId() + " is not expected " + message.getId());
        }

        return false;
      }

      @Override
      public boolean onMessageSendError(String messageId, ApiError apiError, String text) {
        Log.d(TAG, "onMessageSendError(): msgId="+messageId+", code="+apiError+", text="+text);
        if(null != receiveMonitor)  receiveMonitor.failed(apiError);
        return false;
      }

      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        if(null != ackReceiveMonitor)  ackReceiveMonitor.invoked(messageId);
        return false;
      }
    };
    MMX.registerListener(messageListener);

    final String messageId = message.send(new ApiCallback<String>() {
      public void success(String result) {
        Log.d(TAG, "sendMessage(): success() msgId=" + result);
        sendMonitor.invoked(result);
      }

      public void failure(ApiError apiError) {
        Log.e(TAG, "sendMessage(): failure() failureCode=" + apiError);
        sendMonitor.failed(apiError);
      }
    });

    // Check if the send is success
    ExecMonitor.Status sendStatus = sendMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC * 2);
    assertEquals(expectedSendStatus, sendStatus);
    if(sendStatus == ExecMonitor.Status.INVOKED) {
      assertEquals(messageId, sendMonitor.getReturnValue());
    }

    // Check if the receive is success
    if(null != receiveMonitor) {
      ExecMonitor.Status receiveStatus = receiveMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
      assertEquals(expectedReceiveStatus, receiveStatus);
      if(expectedReceiveStatus == ExecMonitor.Status.INVOKED) {
        MMXMessage messageReceived = receiveMonitor.getReturnValue();
        assertNotNull(messageReceived);
        assertEquals(message.getId(), messageReceived.getId());
        AssertionUtils.assertMap(message.getContent(), messageReceived.getContent());
      }
    }

    //check ack send
    if(null != ackSend) {
      //do the acknowledgement
      receiveMonitor.getReturnValue().acknowledge(new ApiCallback<Void>() {
        @Override public void success(Void result) {
          Log.d(TAG, "ACK sent for message " + message.getId());
          ackSend.invoked(Boolean.TRUE);
        }

        @Override public void failure(ApiError apiError) {
          Log.d(TAG, "ACK sent failed for message " + message.getId());
          ackSend.failed(apiError);
        }
      });
      ExecMonitor.Status ackStatus = ackSend.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
      assertEquals(ExecMonitor.Status.INVOKED, ackStatus);

      //check acknowledgement response
      if(null != ackReceiveMonitor) {
        ExecMonitor.Status ackResponseStatus = ackReceiveMonitor.waitFor(TestConstants.TIMEOUT_IN_MILISEC);
        assertEquals(ExecMonitor.Status.INVOKED, ackResponseStatus);
        assertEquals(message.getId(), ackReceiveMonitor.getReturnValue());
      }
    }

    MMX.unregisterListener(messageListener);
  }
}
