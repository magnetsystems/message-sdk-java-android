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

/**
 * The GlobalAddress class encapsulates an address in the MMX system.
 * This is typically in the format of type:name where name is the
 * XID (full or partial) of the specified type.
 */
public abstract class GlobalAddress implements Serializable {
  private static final long serialVersionUID = 2909131415302883976L;
  private static final String TAG = GlobalAddress.class.getSimpleName();
  public enum Type {
    /**
     * The type for "user" addresses
     */
    USER("user"),
    /**
     * The type for "topic" addresses
     */
    TOPIC("topic");

    private final String mTypePrefix;

    private Type(String typePrefix) {
      mTypePrefix = typePrefix;
    }

    /**
     * Returns the string value of the prefix for this type.
     * @return the prefix for this type.
     */
    public String getTypePrefix() {
      return mTypePrefix;
    }
  }
  private static final char DELIMITER = ':';
  private final Type mType;
  private final String mName;
  private final MMXid mXid;

  protected GlobalAddress(Type type, String name) {
    if (name == null) {
      throw new IllegalArgumentException("Cannot have a null name.");
    }
    mType = type;
    mName = name;
    mXid = null;
  }

  protected GlobalAddress(Type type, MMXid xid) {
    if (xid == null) {
      throw new IllegalArgumentException("Cannot have null XID");
    }
    mType = type;
    mXid = xid;
    mName = null;
  }

  /**
   * The type for this GlobalAddress
   * @return the type of GlobalAddress
   */
  public final Type getType() {
    return mType;
  }

  /**
   * The topic name from this GlobalAddress.
   * @return the topic name
   */
  public final String getName() {
    return mName;
  }

  /**
   * The user MMX ID from this GlobalAddress.
   * @return the user MMX ID
   */
  public final MMXid getXid() {
    return mXid;
  }

  /**
   * The string value of this GlobalAddress
   * @return the string representation of this GlobalAddress
   */
  @Override
  public final String toString() {
    if (mType == Type.USER) {
      return mType.getTypePrefix() + DELIMITER + mXid;
    } else {
      return mType.getTypePrefix() + DELIMITER + mName;
    }
  }

  /**
   * Parses the specified String as an MMXAddress
   *
   * @param address The string value of the address
   * @return the MMXAddress of the specified String or null if parsing failed
   */
  public static GlobalAddress parse(String address) {
    int delimiterIndex = address.indexOf(DELIMITER);
    if (delimiterIndex >= 0) {
      String prefix = address.substring(0, delimiterIndex);
      String name = address.substring(delimiterIndex + 1);
      if (prefix.equals(Type.USER.getTypePrefix())) {
        return new User(MMXid.parse(name));
      } else if (prefix.equals(Type.TOPIC.getTypePrefix())) {
        return new Topic(name);
      } else {
        throw new IllegalArgumentException("Unable to parse address because the " +
                "prefix is invalid: " + address + ", " + prefix);
      }
    } else {
      throw new IllegalArgumentException("Malformed address doesn't contain delimiter: " + address);
    }
  }

  /**
   * Convenience method to convert an array of GlobalAddress into a String[]
   * populated with the address names.
   *
   * @param addresses the addresses to convert
   * @return the address names
   */
  public static Object[] convertDestination(GlobalAddress[] addresses) {
    String[] topics = null;
    MMXid[] xids = null;
    Type type = null;
    for (int i=addresses.length; --i>=0;) {
      GlobalAddress curAddress = addresses[i];
      if (type != null && type != curAddress.getType()) {
        throw new IllegalArgumentException("Mixed address types are not supported.");
      }
      type = curAddress.getType();
      if (type == Type.TOPIC) {
        if (topics == null) {
          topics = new String[addresses.length];
        }
        topics[i] = addresses[i].getName();
      } else {
        if (xids == null) {
          xids = new MMXid[addresses.length];
        }
        xids[i] = addresses[i].getXid();
      }
    }
    return topics != null ? topics : xids;
  }

  /**
   * Convenience method to convert an array of user MMX ID's into an array of
   * GlobalAddress.
   *
   * @param type the type of the specified names
   * @param xids the MMX ID's to convert
   * @return the converted array
   */
  public static GlobalAddress[] convertDestination(GlobalAddress.Type type, MMXid[] xids) {
    GlobalAddress[] destinations = new GlobalAddress[xids.length];
    for (int i=xids.length; --i>=0;) {
      destinations[i] = new GlobalAddress.User(xids[i]);
    }
    return destinations;
  }

  /**
   * Convenience method to convert a String[] of topic names into an array of
   * GlobalAddress.
   *
   * @param type the type of the specified names
   * @param names the topic names to convert
   * @return the converted array
   */
  public static GlobalAddress[] convertDestination(GlobalAddress.Type type, String[] names) {
    GlobalAddress[] destinations = new GlobalAddress[names.length];
    for (int i=names.length; --i>=0;) {
      destinations[i] = new GlobalAddress.Topic(names[i]);
    }
    return destinations;
  }
  /**
   * An address for a user in the MMX system.
   */
  public static class User extends GlobalAddress {
    public User(MMXid xid) {
      super(Type.USER, xid);
    }
  }

  /**
   * An address for a topic in the MMX system.
   */
  public static class Topic extends GlobalAddress {
    public Topic(String name) {
      super(Type.TOPIC, name);
    }
  }
}
