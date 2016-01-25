package com.magnet.mmx.client.java;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.MMXContext;
import com.magnet.mmx.client.MMXSettings;
import com.magnet.mmx.client.common.*;
import com.magnet.mmx.client.helper.DeveloperContext;
import com.magnet.mmx.protocol.AuthData;
import com.magnet.mmx.protocol.MMXError;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXid;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.jivesoftware.smack.packet.XMPPError;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mmicevic on 1/20/16.
 *
 */

public class TestMessaging {

    private static final int NUMBER_OF_USERS = 5;
    private static final int NUMBER_OF_SIMPLE_MESSAGES = 50;

//    private static final String MMS_URL = "http://localhost:8443";
    private static final String MMS_URL = "http://ec2s-mms20.magnet.com:8443";
    private static final String DEV_USER = "developer";
    private static final String DEV_PWD = "developer";
//    private static final String MMX_HOST = "localhost";
    private static final String MMX_HOST = "ec2s-mmx20.magnet.com";
    private static final int MMX_PORT = 5222;

    private static DeveloperContext developerContext = new DeveloperContext(MMS_URL, DEV_USER, DEV_PWD);
    private static String appId;

    private static String deviceIdTo = null;
    private static String userDisplayNameTo = null;

    private static Counts counts = new Counts();


    @BeforeClass
    public static void setUp() throws IOException, OAuthSystemException, OAuthProblemException {

        //CREATE APP
        appId = developerContext.createApp();

        //REGISTER USERS
        for (int index = 1; index <= NUMBER_OF_USERS; index++) {
            developerContext.registerUser(String.valueOf(index));
        }
    }


    @Test
    public void test1() throws IOException, MMXException {


        Map<Integer, MMXClient> clients = new HashMap<>();

        String deviceIdPrefix = "device-";
        String userDisplayNamePrexix = "nick-";
        //
        //AUTHENTICATE AND CONNECT ALL USERS
        for (int index = 1; index <= NUMBER_OF_USERS; index++) {

            //AUTHENTICATE USER
            String suffixKey = String.valueOf(index);
            String deviceId = deviceIdPrefix + index;
            developerContext.authenticateUser(suffixKey, deviceId);
            String userId = developerContext.getUserId(suffixKey);

            //XMPP CONNECT
            String userDisplayName = userDisplayNamePrexix + index;
            String appPath = ".";
            String appVersion = "v1";
            MMXContext mmxContext = new MMXContext(appPath, appVersion, deviceId);
            MMXSettings mmxSettings = new MMXSettings(mmxContext, userDisplayName);

            mmxSettings.setString(MMXSettings.PROP_HOST, MMX_HOST);
            mmxSettings.setInt(MMXSettings.PROP_PORT, MMX_PORT);
            mmxSettings.setBoolean(MMXSettings.PROP_ENABLE_TLS, true);

            String mmxUserId = userId + "%" + appId;

            MMXConnectionListener connectionListener = new ConnListener();
            MMXMessageListener msgListener = new MessageListener();

            MMXClient.MMXConnectionOptions mmxOptions = new MMXClient.MMXConnectionOptions();
            mmxOptions.setAutoCreate(false);
            mmxOptions.setSuspendDelivery(false);
            MMXClient mmxClient = new MMXClient(mmxContext, mmxSettings);

            String userToken = developerContext.getUserToken(suffixKey);
            mmxClient.connect(mmxUserId, userToken.getBytes(), connectionListener, msgListener, mmxOptions);
            mmxClient.setPriority(0);
            clients.put(index, mmxClient);
        }

        // EACH USER TO SEND N MESSAGES
        for (int index = 1; index <= NUMBER_OF_USERS; index++) {
            MMXClient mmxClient = clients.get(index);
            if (mmxClient != null) {

                //SEND MESSAGES
                for (int i = 0; i < NUMBER_OF_SIMPLE_MESSAGES; i++) {

                    int indexTo = getRandomInt(1, NUMBER_OF_USERS);
                    String userIdTo = developerContext.getUserId(String.valueOf(indexTo));
                    String mmxUserIdTo = userIdTo + "%" + appId;
//                    System.out.println("Sending " + index + " --> " + indexTo);

                    MMXid[] to = {new MMXid(mmxUserIdTo, deviceIdTo, userDisplayNameTo)};
                    MMXPayload payload = new MMXPayload("Hello.");
                    Options options = new Options();

                    mmxClient.getMessageManager().sendPayload(to, payload, options);

                    String keyCounts = deviceIdPrefix + indexTo;
                    counts.increment(keyCounts + "@sent");
                }
            }
        }

        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // DISCONNECT ALL USERS
        for (int index = 1; index <= NUMBER_OF_USERS; index++) {
            MMXClient mmxClient = clients.get(index);
            if (mmxClient != null) {
                //XMPP DISCONNECT
                try {
                    mmxClient.disconnect();
                }
                catch (MMXException e) {
                    e.printStackTrace();
                }
            }
        }

        //VALIDATE COUNTS
        for (int index = 1; index <= NUMBER_OF_USERS; index++) {
            String keyCounts = deviceIdPrefix + index;
            int sent = counts.get(keyCounts + "@sent");
            int received = counts.get(keyCounts + "@received");
            System.out.println(keyCounts + " @sent=" + sent + " @received=" + received);
            Assert.assertEquals(sent, received);
        }
    }

    private static int getRandomInt(int from, int to) {

        double r = Math.random();
        //range
        double rr = r * (to - from);
        //translate
        rr = rr + from;
        return (int) Math.round(rr);
    }


    // LISTENERS
    static class ConnListener implements MMXConnectionListener {

        @Override
        public void onConnectionEstablished() {
//            System.out.println("onConnectionEstablished");
        }

        @Override
        public void onReconnectingIn(int interval) {
//            System.out.println("onReconnectingIn: " + interval);
        }

        @Override
        public void onConnectionClosed() {
//            System.out.println("onConnectionClosed");
        }

        @Override
        public void onConnectionFailed(Exception cause) {
//            System.out.println("onConnectionFailed: " + cause.getMessage());
            cause.printStackTrace();
        }

        @Override
        public void onAuthenticated(MMXid user) {
//            System.out.println("onAuthenticated: " + user);
        }

        @Override
        public void onAuthFailed(MMXid user) {
//            System.out.println("onAuthFailed: " + user);
        }

        @Override
        public void onAccountCreated(MMXid user) {
//            System.out.println("onAccountCreated: " + user);
        }
    }

    static class MessageListener implements MMXMessageListener {

        @Override
        public void onMessageReceived(MMXMessage message, String receiptId) {
            try {
//                MMXPayload payload = message.getPayload();
//                long elapsed = -1;
//                if (payload != null && payload.getSentTime() != null) {
//                    elapsed = System.currentTimeMillis() - payload.getSentTime().getTime();
//                }
                //            if (mDisplayName) {
                //                accountGet(message.getFrom().getUserId());
                //            }
                //            if (payload.getMetaData("type", null).equals("LARGEMSG")) {
                //                System.out.println("onMessageReceived: LARGEMSG, hdrs=" + message.getPayload().getAllMetaData() +
                //                        ", size=" + payload.getDataSize() + ", elapsed=" + elapsed);
                ////                System.out.println(Utils.subSequenceHeadTail(payload.getDataAsText(), 1024));
                //            } else {
//                System.out.println("onMessageReceived: " + message + ", receiptId=" + receiptId + ", elapsed=" + elapsed);
                String keyCounts = message.getTo().getDeviceId();
                counts.increment(keyCounts + "@received");

                //            }
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onMessageDelivered(MMXid recipient, String msgId) {
//            System.out.println("onMessageDelivered: to=" + recipient + ", msgId=" + msgId);
        }

        @Override
        public void onMessageSent(String msgId) {
//            System.out.println("onMessageSent: msgId=" + msgId);
        }

        @Override
        public void onMessageSubmitted(String msgId) {
//            System.out.println("onMessageSent: msgId=" + msgId);
        }

        @Override
        public void onMessageAccepted(List<MMXid> list, String msgId) {
//            System.out.println("onMessageSent: msgId=" + msgId);
        }

        @Override
        public void onMessageFailed(String msgId) {
            System.err.println("onMessageFailed: msgId=" + msgId);
        }

        @Override
        public void onInvitationReceived(Invitation invitation) {
//            System.out.println("onInvitationReceived: " + invitation);
        }

        @Override
        public void onAuthReceived(AuthData auth) {
//            System.out.println("onAuthReceived: apikey=" + auth.getApiKey() +
//                    ", authToken=" + auth.getAuthToken() + ", user=" + auth.getUserId() +
//                    ", pwd=" + auth.getPassword());
        }

        @Override
        public void onItemReceived(MMXMessage msg, MMXTopic topic) {

//            System.out.println("onItemReceived: topic=" + topic + ", itemId=" + msg.getId() +
//                    ", payload=" + msg.getPayload() + ", msg=" + msg);


            //        try {
            //            TopicTemplate template = mTemplateMgr.getItemView(topic);
            //            if (template != null) {
            //                CharSequence view = mTemplateMgr.bindItemView(template, msg);
            //                println(view.toString());
            //            }
            //        } catch (Throwable e) {
            //            e.printStackTrace();
            //        }
        }

        @Override
        public void onErrorMessageReceived(MMXErrorMessage message) {
            if (message.isMMXError()) {
                MMXError error = message.getMMXError();
                System.err.println("onErrorMessageReceived: msgId=" + message.getId() +
                        ", MMXError=" + error);
            }
            else if (message.isXMPPError()) {
                XMPPError error = message.getXMPPError();
                System.err.println("onErrorMessageReceived: msgId=" + message.getId() +
                        ", XMPPError=" + error);
            }
            else if (message.isCustomError()) {
                System.err.println("onErrorMessageReceived: CustomError[" + message.getCustomError() + "]=" + message);
            }
        }
    }

}

