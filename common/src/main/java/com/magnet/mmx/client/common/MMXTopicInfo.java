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

import java.io.Serializable;
import java.util.Date;

import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.TopicAction.PublisherType;
import com.magnet.mmx.protocol.TopicInfo;
import com.magnet.mmx.util.XIDUtil;

/**
 * Topic information from a topic search or list.
 */
public class MMXTopicInfo implements Serializable {
  private static final long serialVersionUID = -5212539242296015590L;
  private MMXTopic mTopic;
  private boolean mCollection;
  private String mDescription;
  private boolean mPersistent;
  private int mMaxItems;
  private int mMaxPayloadSize;
  private Date mCreationDate;
  private Date mModifiedDate;
  private PublisherType mPublisherType;
  private MMXid mCreator;
  private boolean mSubscriptionEnabled;

  /**
   * @hide
   */
  public MMXTopicInfo(MMXTopic topic, TopicInfo topicInfo) {
    mTopic = topic;
    mCollection = topicInfo.isCollection();
    mDescription = topicInfo.getDescription();
    mPersistent = topicInfo.isPersistent();
    mMaxItems = topicInfo.getMaxItems();
    mMaxPayloadSize = topicInfo.getMaxPayloadSize();
    mCreationDate = topicInfo.getCreationDate();
    mModifiedDate = topicInfo.getModifiedDate();
    mPublisherType = topicInfo.getPublisherType();
    mCreator = XIDUtil.toXid(topicInfo.getCreator());
    mSubscriptionEnabled = topicInfo.isSubscriptionEnabled();
  }

  /**
   * Get the topic identifier.
   * @return
   */
  public MMXTopic getTopic() {
    return mTopic;
  }

  /**
   * Check if this topic is a collection containing leaf topics and/or other
   * collections.  A collection topic can only be subscribed, but it does not
   * contain published items.  However, only leaf topics can contain
   * published items.
   * @return true for a collection topic; false for publishing and subscription.
   */
  public boolean isCollection() {
    return mCollection;
  }

  /**
   * Get the topic description.
   * @return The description, or null.
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * Check if the published items are persisted in this topic.
   * @return true if this topic holds persistent items; otherwise, false.
   */
  public boolean isPersistent() {
    return mPersistent;
  }

  /**
   * Max number of persisted published items to be held in this topic.
   * @return Maximum number of persisted published items.
   */
  public int getMaxItems() {
    return mMaxItems;
  }

  /**
   * Get the max payload size of published items.
   * @return The configured maximum payload size.
   */
  public int getMaxPayloadSize() {
    return mMaxPayloadSize;
  }

  /**
   * Get the topic creation date/time.
   * @return The topic creation date/time.
   */
  public Date getCreationDate() {
    return mCreationDate;
  }

  /**
   * Get the last modified date/time of this topic.
   * @return The last modified date/time.
   */
  public Date getModifiedDate() {
    return mModifiedDate;
  }

  /**
   * Get the publishing role to this topic.
   * @return The publisher type.
   */
  public PublisherType getPublisherType() {
    return mPublisherType;
  }

  /**
   * Get the creator MMX ID.  The user ID is always in lower case.
   * @return The creator MMX ID.
   */
  public MMXid getCreator() {
    return mCreator;
  }

  /**
   * Check if subscription is enabled for this topic.
   * @return true if subscription is enabled; otherwise, false.
   */
  public boolean isSubscriptionEnabled() {
    return mSubscriptionEnabled;
  }

  /**
   * Get the topic information in string format for debug purpose.
   * @return Informative data about the topic.
   */
  @Override
  public String toString() {
    return "[topic="+mTopic+", desc="+mDescription+", sub="+mSubscriptionEnabled+
        ", maxItems="+mMaxItems+", maxSize="+mMaxPayloadSize+", pubtype="+mPublisherType+
        ", create="+mCreationDate+", mod="+mModifiedDate+", creator="+mCreator+"]";
  }
}
