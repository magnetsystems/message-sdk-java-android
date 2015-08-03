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

import java.io.StringReader;
import java.util.Date;
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.Payload;
import com.magnet.mmx.util.DisposableFile;
import com.magnet.mmx.util.FileUtil;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TimeUtil;
import com.magnet.mmx.util.Utils;

/**
 * The client handler for MMX extended data/payload message.  The stanza for the
 * MMX extension is &lt;mmx ...&gt;&lt;/mmx&gt;.
 */
public class MMXPayloadMsgHandler {

  /**
   * Parse the MMX extension stanza from a String.
   * @param parser
   * @param xmlPayload
   * @return
   */
  public static MMXPacketExtension parse(XmlPullParser parser, String xmlPayload) {
    try {
      MMXPayloadMsgHandler.Provider provider = (MMXPayloadMsgHandler.Provider)
        ProviderManager.getExtensionProvider(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
      if (provider == null) {
        return null;
      }
      parser.setInput(new StringReader(xmlPayload));
      return (MMXPacketExtension) provider.parseExtension(parser);
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    }

  }

  // The encoder for the MMX stanza.
  public static class MMXPacketExtension implements PacketExtension {
    private Headers mHeaders;
    private Payload mPayload;
    private transient CharSequence mXml;

    public MMXPacketExtension(MMXPayload payload) {
      this(payload.getMetaExt(), payload.getPayloadExt());
    }

    public MMXPacketExtension(Map<String, String> headers, Payload payload) {
      if (headers != null) {
        mHeaders = new Headers();
        mHeaders.putAll(headers);
      }
      mPayload = payload;
      DisposableFile file = mPayload.getFile();
      if (file != null && file.isBinary()) {
        if (mHeaders == null) {
          mHeaders = new Headers();
        }
        if (mHeaders.getContentEncoding(null) == null) {
          mHeaders.setContentEncoding(Constants.BASE64);
        }
      }
    }

    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_MSG_PAYLOAD;
    }

    @Override
    public CharSequence toXML() {
      if (mXml != null) {
        return mXml;
      }

      // TODO: the SDK should be smart enough not to do XML escape for certain
      // content type or using an option to disable XML escape.
      boolean xmlEsc = true;
      Date sentTime = new Date();
      mPayload.setSentTime(sentTime);

      XmlStringBuilder xml = new XmlStringBuilder();
      xml.halfOpenElement(getElementName());
      xml.xmlnsAttribute(getNamespace());
      xml.rightAngelBracket();
      if (mHeaders != null) {
        xml.openElement(Constants.MMX_META);
        // The headers encoded by GSON is XML safe; no XML escape is needed.
        xml.append(GsonData.getGson().toJson(mHeaders));
        xml.closeElement(Constants.MMX_META);
      }
      // The <payload/> element is mandatory.
      xml.halfOpenElement(Constants.MMX_PAYLOAD);
      xml.optAttribute(Constants.MMX_ATTR_MTYPE, mPayload.getMsgType());
      xml.optAttribute(Constants.MMX_ATTR_CID, mPayload.getCid());
      xml.attribute(Constants.MMX_ATTR_CHUNK, mPayload.formatChunk());
      xml.attribute(Constants.MMX_ATTR_STAMP, TimeUtil.toString(sentTime));
      xml.rightAngelBracket();
      CharSequence csq = null;
      if (mPayload.getData() != null) {
        if (!xmlEsc) {
          csq = mPayload.getData();
        } else {
          if (mPayload.getDataSize() >= Constants.PAYLOAD_THRESHOLD) {
            csq = FileUtil.encodeForXml(mPayload.getData());
          } else {
            csq = Utils.escapeForXML(mPayload.getData());
          }
        }
      } else if (mPayload.getFile() != null) {
        csq = FileUtil.encodeFile(mPayload.getFile(), xmlEsc);
        // TODO: it is problematic if not caching the xml; otherwise, finish
        // the file after being sent.
        mPayload.getFile().finish();
      }
      if (csq != null) {
        xml.append(csq);
      }
      xml.closeElement(Constants.MMX_PAYLOAD);
      xml.closeElement(getElementName());

      mXml = xml;
      return xml;
    }

    public MMXPayload getPayload() {
      return new MMXPayload(mHeaders, mPayload);
    }
  }

  // The decoder for the MMX stanza.
  public static class Provider implements PacketExtensionProvider {
    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
      Headers headers = null;
      Payload payload = null;
      boolean done = false;
      do {
        int eventType = parser.next();
        if (eventType == XmlPullParser.START_TAG) {
          if (Constants.MMX_META.equals(parser.getName())) {
            headers = GsonData.getGson().fromJson(parser.nextText(), Headers.class);
          } else if (Constants.MMX_PAYLOAD.equals(parser.getName())) {
            String mtype = parser.getAttributeValue(null, Constants.MMX_ATTR_MTYPE);
            String cid = parser.getAttributeValue(null, Constants.MMX_ATTR_CID);
            Date sentTime = TimeUtil.toDate(parser.getAttributeValue(null,
                Constants.MMX_ATTR_STAMP));
            String chunk = parser.getAttributeValue(null,
                Constants.MMX_ATTR_CHUNK);
            String data = parser.nextText();
            payload = new Payload(mtype, data);
            payload.setSentTime(sentTime);
            payload.parseChunk(chunk);
            payload.setCid(cid);
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (Constants.MMX.equals(parser.getName())) {
            done = true;
          }
        }
      } while (!done);

      return new MMXPacketExtension(headers, payload);
    }
  }

  public static void registerMsgProvider() {
    ProviderManager.addExtensionProvider(Constants.MMX,
        Constants.MMX_NS_MSG_PAYLOAD, new MMXPayloadMsgHandler.Provider());
  }
}
