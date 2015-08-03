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
package com.magnet.mmx.util;

import java.io.Serializable;
import java.util.Set;

import com.magnet.mmx.client.common.GlobalAddress;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.client.common.Options;

/**
 * The MMXQueue should be implemented and managed per MMXClient and represents a
 * queue on which to place pending submissions into the MMX system (message or published items)
 * if they cannot be sent at the time of the call.
 */
public interface MMXQueue {
  /**
   * An item in the queue
   */
  public static abstract class Item implements Serializable {
    public static final class Message extends Item {
      private GlobalAddress[] mDestination;

      public Message(String id, MMXPayload payload) {
        super(id, Type.MESSAGE, payload);
      }

      /**
       * The destination of this item
       *
       * @return the destination of this item
       */
      public GlobalAddress[] getDestination() { return mDestination; }

      /**
       * Set the destination for this item
       *
       * @param destination the new destination
       */
      public void setDestination(GlobalAddress[] destination) {
        mDestination = destination;
      }
    }

    public static final class PubSub extends Item {
      private String mRealTopic;
      private String mTopic;

      public PubSub(String id, String realTopic, String topic, MMXPayload payload) {
        super(id, Type.PUBSUB, payload);
        mRealTopic = realTopic;
        mTopic = topic;
      }

      public String getRealTopic() {
        return mRealTopic;
      }

      public String getTopic() {
        return mTopic;
      }
    }

    /**
     * The type of the item
     */
    public enum Type {
      /**
       * a message
       */
      MESSAGE,
      /**
       * an item to publish
       */
      PUBSUB}

    private String mId;
    private Type mType;
    private MMXPayload mPayload;
    private Options mOptions;

    /**
     * The basic constructor
     * @param id the id of the item
     * @param type the type of the item
     * @param payload the item's payload
     */
    public Item(String id, Type type, MMXPayload payload) {
      mId = id;
      mType = type;
      mPayload = payload;
    }

    /**
     * The identifier for this item
     *
     * @return the identifier
     */
    public String getId() {
      return mId;
    }

    /**
     * Set the identifier for this item
     *
     * @param id the new identifier
     */
    public void setId(String id) {
      mId = id;
    }

    /**
     * The type of this item
     *
     * @return the type
     */
    public Type getType() {
      return mType;
    }

    /**
     * Set the type of this item
     *
     * @param type the new type
     */
    public void setType(Type type) {
      mType = type;
    }

    /**
     * The payload for this item
     *
     * @return the payload
     */
    public MMXPayload getPayload() {
      return mPayload;
    }

    /**
     * Set the payload for this item
     *
     * @param payload the new payload
     */
    public void setPayload(MMXPayload payload) {
      mPayload = payload;
    }

    /**
     * The options for this item
     *
     * @return the options
     */
    public Options getOptions() {
      return mOptions;
    }

    /**
     * Set the options for this item
     *
     * @param options the new options
     */
    public void setOptions(Options options) {
      mOptions = options;
    }
  }

  /**
   * Adds an item to the queue.
   *
   * @param item the item to add
   * @return true if successful, false otherwise
   */
  public boolean addItem(Item item);

  /**
   * Retrieves the identifiers of all the pending items in the queue
   *
   * @return the identifiers of the pending items
   */
  public Set<String> getPendingItemIds();

  /**
   * Processes the pending items on the queue
   */
  public void processPendingItems();

  /**
   * Removes all items from the queue
   */
  public void removeAllItems();

  /**
   * Removes a single item from the queue
   *
   * @param id the id of the item to remove
   * @return true is successful, false otherwise
   */
  public boolean removeItem(String id);
}
