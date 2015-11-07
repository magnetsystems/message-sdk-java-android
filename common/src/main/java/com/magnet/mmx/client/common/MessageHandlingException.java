package com.magnet.mmx.client.common;

/**
 * An internal exception used to indicate what the message
 * listener was unable to handle the message.  Meant to be thrown
 * when handling a onMessageReceived to prevent the ack from being
 * sent.
 */
public class MessageHandlingException extends RuntimeException {
  public MessageHandlingException(String message) {
    super(message);
  }

  public MessageHandlingException(Throwable t) {
    super(t);
  }
}
