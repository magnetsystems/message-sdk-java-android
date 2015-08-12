package com.magnet.mmx.client.api;

public class MagnetMessageTest extends MMXInstrumentationTestCase {
  private static final String TAG = MagnetMessageTest.class.getSimpleName();

  public void testStartEndSession() {
    final MagnetMessage.OnFinishedListener<Void> sessionListener = getSessionListener();
    MagnetMessage.startSession(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(MagnetMessage.getMMXClient().isConnected());
    MagnetMessage.endSession(sessionListener);
    synchronized (sessionListener) {
      try {
        sessionListener.wait(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertFalse(MagnetMessage.getMMXClient().isConnected());
  }


}
