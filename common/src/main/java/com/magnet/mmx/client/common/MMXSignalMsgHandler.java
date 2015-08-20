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
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.util.GsonData;

/**
 * The client handler for MMX extended signal message.  The stanza for the
 * MMX extension is &lt;mmx ...&gt;&lt;/mmx&gt;.
 */
public class MMXSignalMsgHandler {

  /**
   * Parse the MMX extension stanza from a String.
   * @param parser
   * @param xmlPayload
   * @return
   */
  public static MMXPacketExtension parse(XmlPullParser parser, String xmlPayload) {
    try {
      MMXSignalMsgHandler.Provider provider = (MMXSignalMsgHandler.Provider)
        ProviderManager.getExtensionProvider(Constants.MMX, Constants.MMX_NS_MSG_SIGNAL);
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
    private MmxHeaders mMmxMeta;
    private transient CharSequence mXml;

    public MMXPacketExtension(Map<String, Object> mmxMeta) {
      if (mmxMeta != null) {
        mMmxMeta = new MmxHeaders();
        mMmxMeta.putAll(mmxMeta);
      }
    }

    @Override
    public String getElementName() {
      return Constants.MMX;
    }

    @Override
    public String getNamespace() {
      return Constants.MMX_NS_MSG_SIGNAL;
    }

    @Override
    public CharSequence toXML() {
      if (mXml != null) {
        return mXml;
      }

      XmlStringBuilder xml = new XmlStringBuilder();
      xml.halfOpenElement(getElementName());
      xml.xmlnsAttribute(getNamespace());
      xml.rightAngelBracket();
      if (mMmxMeta != null) {
        xml.openElement(Constants.MMX_MMXMETA);
        xml.append(GsonData.getGson().toJson(mMmxMeta));
        xml.closeElement(Constants.MMX_MMXMETA);
      }
      xml.closeElement(getElementName());

      mXml = xml;
      return xml;
    }

    public MmxHeaders getMmxMeta() {
      return mMmxMeta;
    }
  }

  // The decoder for the MMX stanza.
  public static class Provider implements PacketExtensionProvider {
    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
      MmxHeaders mmxMeta = null;
      boolean done = false;
      do {
        int eventType = parser.next();
        if (eventType == XmlPullParser.START_TAG) {
          if (Constants.MMX_MMXMETA.equals(parser.getName())) {
            mmxMeta = GsonData.getGson().fromJson(parser.nextText(), MmxHeaders.class);
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (Constants.MMX.equals(parser.getName())) {
            done = true;
          }
        }
      } while (!done);

      return new MMXPacketExtension(mmxMeta);
    }
  }

  public static void registerMsgProvider() {
    ProviderManager.addExtensionProvider(Constants.MMX,
        Constants.MMX_NS_MSG_SIGNAL, new MMXSignalMsgHandler.Provider());
  }
}
