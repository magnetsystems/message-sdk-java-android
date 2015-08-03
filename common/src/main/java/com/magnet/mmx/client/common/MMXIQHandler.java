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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.util.GsonData;

/**
 * @hide
 * The abstract IQ provider and handler for MMX extension.  The developer
 * needs to implement the <code>getElementName</code>, <code>getNamespace</code>
 * and provide the POJO's of the <code>Request</code> and <code>Result</code>.
 * Before to use this handler, its IQProvider must be registered first.  This
 * registration is commonly done in the constructor of each Manager:
 * <pre>
 * private MessageManager(MMXConnection con) {
 *   ...
 *   MsgMMXIQHandler myIQHandler = new MsgMMXIQHandler<Request,Result>(...);
 *   myIQHandler.registerIQProvider();
 * }
 *
 * or
 *
 * static {
 *    MMXIQHandler myIQHandler = new MyMMXIQHandler<Request,Result>(...);
 *    myIQHandler.registerIQProvider();
 * }
 * </pre>
 * To send the "set" IQ and get the result:
 * <pre>
 *    MMXIQHandler myIQHandler = new MyMMXIQHandler<Request, Result>(...);
 *    myIQHandler.sendSetIQ(con, "create", myData, Result.class, listener);
 *    Result result = myIQHandler.getResult();
 * </pre>
 *
 * @param <Request>
 * @param <Result>
 */
abstract class MMXIQHandler<Request, Result>
                        implements IQListener<Result> {
  public abstract String getElementName();
  public abstract String getNamespace();
  private final static long TIMEOUT = 30000L;
  private boolean mTimedOut;
  protected Result mResult;
  protected MMXStatus mError;

  class Provider implements IQProvider {
    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
      int eventType = parser.getEventType();
      do {
        if (eventType == XmlPullParser.START_TAG &&
            parser.getName().equalsIgnoreCase(getElementName())) {
          String ctype = parser.getAttributeValue(null, Constants.MMX_ATTR_CTYPE);
          String cmd = parser.getAttributeValue(null, Constants.MMX_ATTR_COMMAND);
          String dst = parser.getAttributeValue(null, Constants.MMX_ATTR_DST);
          String payload = parser.nextText();
          if (payload != null && !payload.isEmpty()) {
            return new MMXIQ(cmd, ctype, payload, dst);
          } else {
            return new MMXIQ(cmd, ctype, null, dst);
          }
        }
        eventType = parser.next();
      } while (eventType != XmlPullParser.END_DOCUMENT);
      throw new Exception("Unable to find mmx element");
    }
  }

  /**
   * In standard IQ, if the "to" is a client JID, the XMPP server will route
   * the IQ directly to the client.  Without intercepting every IQ or modifying
   * the XMPP server, the destination is in an optional "dst" attribute and the
   * "to" remains as the XMPP server.
   */
  class MMXIQ extends IQ {
    private Request mRequest;
    private String mPayload;
    private String mCommand;
    private String mContentType;
    private String mDestination;

    /**
     * Constructor for IQ request.
     * @param cmd
     * @param request
     * @param dst The full XID where this IQ targets to, or null.
     */
    public MMXIQ(String cmd, Request request, String dst) {
      mCommand = cmd;
      mContentType = GsonData.CONTENT_TYPE_JSON;
      mRequest = request;
      mDestination = dst;
    }

    // Constructor for IQ result.
    MMXIQ(String cmd, String ctype, String payload, String dst) {
      mCommand = cmd;
      mContentType = ctype;
      mPayload = payload;
      mDestination = dst;
    }

    /**
     * Get the payload in the MMX extension.
     * @return
     */
    public String getPayload() {
      if (mPayload == null && mRequest != null) {
        mPayload = GsonData.getGson().toJson(mRequest);
      }
      return mPayload;
    }

    /**
     * Get the command attribute in the MMX extension.
     * @return
     */
    public String getCommand() {
      return mCommand;
    }

    /**
     * Get the content type of the payload.
     * @return
     */
    public String getContentType() {
      return mContentType;
    }

    /**
     * Get the destination (full XID) of this IQ.
     * @return
     */
    public String getDestination() {
      return mDestination;
    }

    /**
     * Construct the IQ MMX stanza.
     */
    @Override
    public XmlStringBuilder getChildElementXML() {
      XmlStringBuilder xml = new XmlStringBuilder();
      xml.halfOpenElement(getElementName());
      xml.xmlnsAttribute(getNamespace());
      xml.attribute(Constants.MMX_ATTR_COMMAND, mCommand);
      xml.attribute(Constants.MMX_ATTR_CTYPE, mContentType);
      xml.optAttribute(Constants.MMX_ATTR_DST, mDestination);
      xml.rightAngelBracket();
      if (mRequest != null) {
        xml.append(GsonData.getGson().toJson(mRequest));
      }
      xml.closeElement(getElementName());
      return xml;
    }
  }

  /**
   * Wait for the IQ result with a predefined timeout.  If any errors from the
   * server, the MMXException will be thrown.  This blocking call may cause
   * the Smack Reader Thread blocked if it is performed in the callback thread.
   * @return
   * @throws MMXException
   */
  public Result getResult() throws MMXException {
    synchronized(this) {
      if (mError == null && mResult == null) {
        try {
          mTimedOut = true;
          this.wait(TIMEOUT);
        } catch (InterruptedException e) {
          // Ignored.
        }
      }
    }
    if (mError != null) {
      throw new MMXException(mError.getMessage(), mError.getCode(), null);
    }
    if (mTimedOut) {
      throw new MMXException("Response timed out", Constants.STATUS_CODE_500);
    }
    return mResult;
  }

  @Override
  public void onReceived(Result result) {
    synchronized(this) {
      mResult = result;
      mTimedOut = false;
      this.notify();
    }
  }

  // IQ error with MMX payload.
  @Override
  public void onError(String cmd, MMXStatus status) {
    synchronized(this) {
      mError = status;
      mTimedOut = false;
      this.notify();
    }
  }

  // IQ error with standard payload.
  @Override
  public void onError(String xml) {
    synchronized(this) {
      mError = new MMXStatus();
      mError.setMessage(xml);
      mError.setCode(Constants.STATUS_CODE_500);
      mTimedOut = false;
      this.notify();
    }
  }

  /**
   * Register the incoming IQ parser.  It is one time only.
   */
  public void registerIQProvider() {
    ProviderManager.addIQProvider(getElementName(), getNamespace(), new Provider());
  }

  /**
   * Send a request using SET-IQ to MMX and wait for a result.
   * @param con
   * @param cmd
   * @param rqt
   * @param resultClz
   * @param listener Specify <code>this</code> for blocking result.
   * @throws MMXException
   */
  public void sendSetIQ(MMXConnection con, String cmd, Request rqt,
                      final Class<Result> resultClz,
                      final IQListener<Result> listener) throws MMXException {
    sendIQ(con, IQ.Type.SET, null, cmd, rqt, resultClz, listener);
  }

  /**
   * Send a request using SET-IQ to a client and wait for a result.
   * @param con
   * @param dst A full JID.
   * @param cmd
   * @param rqt
   * @param resultClz
   * @param listener
   * @throws MMXException
   */
  public void sendSetIQ(MMXConnection con, String dst, String cmd, Request rqt,
            final Class<Result> resultClz, final IQListener<Result> listener)
                throws MMXException {
    sendIQ(con, IQ.Type.SET, dst, cmd, rqt, resultClz, listener);
  }

  /**
   * Send a request using GET-IQ to MMX and wait for a result.
   * @param con
   * @param type
   * @param cmd
   * @param rqt
   * @param resultClz
   * @param listener Specify <code>this</code> for blocking result.
   * @throws MMXException
   */
  public void sendGetIQ(MMXConnection con, String cmd, Request rqt,
            final Class<Result> resultClz, final IQListener<Result> listener)
                throws MMXException {
    sendIQ(con, IQ.Type.GET, null, cmd, rqt, resultClz, listener);
  }

  /**
   * Send a request using GET-IQ to a client and wait for a result.
   * @param con
   * @param dst A full JID.
   * @param cmd
   * @param rqt
   * @param resultClz
   * @param listener
   * @throws MMXException
   */
  public void sendGetIQ(MMXConnection con, String dst, String cmd, Request rqt,
            final Class<Result> resultClz, final IQListener<Result> listener)
                throws MMXException {
    sendIQ(con, IQ.Type.GET, dst, cmd, rqt, resultClz, listener);
  }

  // Send an IQ (either SET or GET) request to MMX and wait for a result.
  protected void sendIQ(MMXConnection con, IQ.Type type, String dst, String cmd,
                      Request rqt, final Class<Result> resultClz,
                      final IQListener<Result> listener) throws MMXException {
    final XMPPConnection xmppCon = con.getXMPPConnection();
    if (xmppCon == null) {
      throw new ConnectionException("Not connect to MMX Server", null);
    }
    try {
      IQ iq = new MMXIQ(cmd, rqt, dst);
      iq.setPacketID(con.genId());
      iq.setType(type);
      PacketFilter packetFilter = new IQReplyFilter(iq, xmppCon);
      PacketListener packetListener = new PacketListener() {
        @Override
        public void processPacket(Packet packet) throws NotConnectedException {
          if (listener == null) {
            xmppCon.removePacketListener(this);
            return;
          }
          if (packet instanceof MMXIQHandler.MMXIQ) {
            MMXIQ iq = (MMXIQ) packet;
            if (GsonData.CONTENT_TYPE_JSON.equals(iq.getContentType())) {
              if (iq.getType() == IQ.Type.ERROR) {
                MMXStatus res = GsonData.getGson().fromJson(iq.getPayload(), MMXStatus.class);
                listener.onError(iq.getCommand(), res);
              } else {
                Result res = GsonData.getGson().fromJson(iq.getPayload(), resultClz);
                listener.onReceived(res);
              }
            }
          } else if (((IQ) packet).getType() == IQ.Type.ERROR) {
            listener.onError(packet.toString());
          } else {
            listener.onError("Unsupported IQ extension: "+packet+
                             "\nForgot to register an IQ Provider?");
          }
          xmppCon.removePacketListener(this);
        }
      };
      xmppCon.addPacketListener(packetListener, packetFilter);
      xmppCon.sendPacket(iq);
    } catch (NotConnectedException e) {
      throw new MMXException(e.getMessage(), e);
    }
  }
}
