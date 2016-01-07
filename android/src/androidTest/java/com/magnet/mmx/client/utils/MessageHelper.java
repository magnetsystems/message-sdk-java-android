/**
 * Copyright (c) 2012-2016 Magnet Systems. All rights reserved.
 */
package com.magnet.mmx.client.utils;

import com.magnet.max.android.User;
import com.magnet.mmx.client.api.ExecMonitor;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.common.Log;

import static junit.framework.Assert.*;

public class MessageHelper {

  private static final String TAG = MessageHelper.class.getSimpleName();

  public static void sendMessage(final MMXMessage message,
      final ExecMonitor<String, FailureDescription> sendMonitor, final ExecMonitor.Status expectedSendStatus,
      final ExecMonitor<MMXMessage, FailureDescription> receiveMonitor, final ExecMonitor.Status expectedReceiveStatus,
      final ExecMonitor<Boolean, FailureDescription> ackSend, final ExecMonitor<String, Void> ackReceiveMonitor) {
    //assertTrue(MMX.getMMXClient().isConnected());
    MMX.start();
    MMX.EventListener messageListener = new MMX.EventListener() {
      public boolean onMessageReceived(final MMXMessage message) {
        Log.d(TAG, "onMessageReceived(): " + message.getId());

        if(null != receiveMonitor) receiveMonitor.invoked(message);

        return false;
      }

      @Override
      public boolean onMessageSendError(String messageId,
          MMXMessage.FailureCode code, String text) {
        Log.d(TAG, "onMessageSendError(): msgId="+messageId+", code="+code+", text="+text);
        if(null != receiveMonitor)  receiveMonitor.failed(new FailureDescription(code, new Exception(text)));
        return false;
      }

      @Override
      public boolean onMessageAcknowledgementReceived(User from, String messageId) {
        if(null != ackReceiveMonitor)  ackReceiveMonitor.invoked(messageId);
        return false;
      }
    };
    MMX.registerListener(messageListener);

    final String messageId = message.send(new MMXMessage.OnFinishedListener<String>() {
      public void onSuccess(String result) {
        Log.d(TAG, "sendMessage(): onSuccess() msgId=" + result);
        sendMonitor.invoked(result);
      }

      public void onFailure(MMXMessage.FailureCode code, Throwable ex) {
        Log.e(TAG, "sendMessage(): onFailure() failureCode=" + code, ex);
        sendMonitor.failed(new FailureDescription(code, ex));
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
        assertNotNull(receiveMonitor.getReturnValue());
        assertEquals(message.getId(), receiveMonitor.getReturnValue().getId());
      }
    }

    //check ack send
    if(null != ackSend) {
      //do the acknowledgement
      receiveMonitor.getReturnValue().acknowledge(new MMXMessage.OnFinishedListener<Void>() {
        @Override public void onSuccess(Void result) {
          Log.d(TAG, "ACK sent for message " + message.getId());
          ackSend.invoked(Boolean.TRUE);
        }

        @Override public void onFailure(MMXMessage.FailureCode code, Throwable throwable) {
          Log.d(TAG, "ACK sent failed for message " + message.getId());
          ackSend.failed(new FailureDescription(code, throwable));
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
