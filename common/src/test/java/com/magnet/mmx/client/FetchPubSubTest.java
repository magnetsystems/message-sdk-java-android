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


package com.magnet.mmx.client;

import com.magnet.mmx.protocol.Constants;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.*;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class FetchPubSubTest {

  static XMPPConnection connection;
  static String NAME_NODE_HEADLINES = "headlines";
  static String TEST_JID = "pbuser";
  static String TEST_PASSWORD = "test";
  static PubSubManager mgr;

  public static class HeadlineFeed implements PacketExtension {

    private String payload;
    public void setPayload(String text) {
      this.payload = text;
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
      return payload;
    }
  }
  @BeforeClass
  public static void setup() throws Exception {
//    ConnectionConfiguration config = new ConnectionConfiguration("127.0.0.1", 5222);
//    config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
//    connection = new XMPPTCPConnection(config);
//    connection.connect();
//    connection.login(TEST_JID, TEST_PASSWORD, "unittest");// Log into the server
//    mgr = new PubSubManager(connection);
  }

  @Ignore
  @Test
  public void test1CreateNode() {


    LeafNode myNode = null;

    try {

      Node node = mgr.getNode(NAME_NODE_HEADLINES);
      List<Subscription> subscriptionsList = node.getSubscriptions();
      node.subscribe(TEST_JID+"@login1.local");

    } catch (XMPPException e) {//node does not exists,
      e.printStackTrace();
      // Create the node
      ConfigureForm form = new ConfigureForm(FormType.submit);
      form.setAccessModel(AccessModel.open);
      form.setDeliverPayloads(true);
      form.setNotifyRetract(true);
      form.setPersistentItems(true);
      form.setPublishModel(PublishModel.open);

      // create it
      LeafNode headlinesNode = null;
      try {
        headlinesNode = (LeafNode) mgr.createNode(NAME_NODE_HEADLINES, form);
      } catch (NoResponseException e1) {
        e1.printStackTrace();
      } catch (XMPPErrorException e1) {
        e1.printStackTrace();
      } catch (NotConnectedException e1) {
        e1.printStackTrace();
      }
      assertNotNull(headlinesNode);
    } catch (NotConnectedException e) {
      e.printStackTrace();
    } catch (NoResponseException e) {
      e.printStackTrace();
    }
  }
  @Ignore
  @Test
  public void test2Publish() throws NotConnectedException, XMPPErrorException, NoResponseException {
    LeafNode node = mgr.getNode(NAME_NODE_HEADLINES);
    String news = "<headline xmlns=\"pubsub:test:headlines\"><summary>Helen is working from home today at " + System.currentTimeMillis() + "</summary></headline>";
    SimplePayload pubPayload = new SimplePayload(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD, news);
    long id = System.currentTimeMillis();
    node.publish(new PayloadItem(Long.toString(id), pubPayload));

  }

  @Ignore
  @Test
  public void test3Fetch() throws NotConnectedException, XMPPErrorException, NoResponseException {
    LeafNode node = mgr.getNode(NAME_NODE_HEADLINES);
    Collection<Item> items = node.getItems();
    for (Item item: items) {
      assertEquals(Constants.MMX, item.getElementName());
      assertEquals(Constants.MMX_NS_MSG_PAYLOAD, item.getNamespace());
      assertTrue(item.toString().startsWith("Helen is working"));
    }
  }
}
