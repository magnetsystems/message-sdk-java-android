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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.magnet.mmx.client.common.GlobalAddress;
import com.magnet.mmx.client.common.Log;
import com.magnet.mmx.client.common.MMXid;
import com.magnet.mmx.util.MMXQueue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the MMXQueue for Android that uses the filesystem as a backing store
 * for the queue items and keeps an index in SQLite.
 */
class PersistentQueue implements MMXQueue {
  private static final String TAG = PersistentQueue.class.getSimpleName();
  private static final String MMX_ITEM_SUBDIR = PersistentQueue.class.getName();
  private final MMXClient mClient;
  private final QueueDatabaseHelper mDatabaseHelper;
  private final File mItemDir;

  /**
   * The constructor for this queue
   *
   * @param client the MMXClient instance to use with this queue
   */
  PersistentQueue(MMXClient client) {
    mClient = client;
    mDatabaseHelper = new QueueDatabaseHelper(mClient.getContext(), PersistentQueue.class.getName() + "-" + client.mName);
    mItemDir = new File(mClient.getContext().getFilesDir(), MMX_ITEM_SUBDIR + '.' + client.mName);
    if (!mItemDir.exists()) {
      if (!mItemDir.mkdir()) {
        throw new RuntimeException("Unable to create queue directory for MMXClient: " + client.mName);
      }
    }
  }

  /**
   * Adds an item to the queue.
   *
   * @param item the item to add
   * @return true if successful, false otherwise
   */
  public synchronized boolean addItem(MMXQueue.Item item) {
    ObjectOutputStream oos = null;
    SQLiteDatabase db = null;
    try {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "addItem(): saving item wrapper for item: " + item.getId());
      }
      File wrapperFile = new File(mItemDir, item.getId());
      if (wrapperFile.createNewFile()) {
        FileOutputStream fos = new FileOutputStream(wrapperFile, false);
        oos = new ObjectOutputStream(mClient.mEncryptor.encodeStream(fos));
        oos.writeObject(item);

        //write the record to the database;
        db = mDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(QueueDatabaseHelper.QUEUE_ITEM_ID, item.getId());
        values.put(QueueDatabaseHelper.QUEUE_ITEM_PATH, wrapperFile.getAbsolutePath());
        values.put(QueueDatabaseHelper.QUEUE_ITEM_TIME, System.currentTimeMillis());
        values.put(QueueDatabaseHelper.QUEUE_ITEM_TYPE, item.getType().toString());
        db.insert(QueueDatabaseHelper.QUEUE_ITEM_TABLE, "", values);
        return true;
      } else {
        Log.e(TAG, "addItem(): unable to create the item wrapper file.");
        return false;
      }
    } catch (IOException e) {
      Log.e(TAG, "addItem():  Unable to store item for later sending.", e);
      return false;
    } finally {
      if (oos != null) {
        try {
          oos.close();
        } catch (IOException e) {
          Log.w(TAG, "addItem():  Unable to close ObjectOutputStream", e);
        }
      }
      if (db != null) {
        db.close();
      }
    }
  }

  private static class QueueDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = QueueDatabaseHelper.class.getSimpleName();
    private static final int VERSION = 1;
    private static final String QUEUE_ITEM_TABLE = "queue_item";
    private static final String QUEUE_ITEM_ID = "id";
    private static final String QUEUE_ITEM_TYPE = "type";
    private static final String QUEUE_ITEM_PATH = "path";
    private static final String QUEUE_ITEM_TIME = "timestamp";
    private static final String CREATE_QUEUE_ITEM_TABLE =
            "CREATE TABLE " + QUEUE_ITEM_TABLE + " (" +
                    QUEUE_ITEM_ID + " TEXT PRIMARY KEY," +
                    QUEUE_ITEM_TYPE + " TEXT NOT NULL, " +
                    QUEUE_ITEM_PATH + " TEXT NOT NULL," +
                    QUEUE_ITEM_TIME + " INT NOT NULL" +
                    ")";

    public QueueDatabaseHelper(Context context, String name) {
      super(context, name, null, VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onCreate():  Creating queue table for: " + this.getDatabaseName());
      }
      db.execSQL(CREATE_QUEUE_ITEM_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onUpgrade(): begin");
      }
    }
  }

  /**
   * Processes the pending items on the queue
   */
  public synchronized void processPendingItems() {
    //this is only for sent messages
    SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
    Cursor itemCursor = null;
    try {
      itemCursor = db.query(QueueDatabaseHelper.QUEUE_ITEM_TABLE, null, null, null, null, null, QueueDatabaseHelper.QUEUE_ITEM_TIME);
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "processPendingItems(): Found " + itemCursor.getCount() + " pending items to send.");
      }
      int idIdx = itemCursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_ID);
      int pathIdx = itemCursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_PATH);
      int typeIdx = itemCursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_TYPE);
      while (itemCursor.moveToNext()) {
        String id = itemCursor.getString(idIdx);
        String itemPath = itemCursor.getString(pathIdx);
        Item.Type type = Item.Type.valueOf(itemCursor.getString(typeIdx));
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "processPendingItems(): processing " + id);
        }
        if (!mClient.isConnected()) {
          //TODO: for now, just check if connected.  In the future, we need to ensure we get a callback
          //TODO: to maintain the order of the queued messages.
          Log.e(TAG, "processPendingItems(): Cannot process items because not connected.");
          break;
        }
        File itemFile = new File(itemPath);
        if (itemFile.exists()) {
          ObjectInputStream ois = null;
          try {
            FileInputStream fis = new FileInputStream(itemFile);
            ois = new ObjectInputStream(mClient.mEncryptor.decodeStream(fis));

            switch (type) {
              case MESSAGE:
                //send the message
                Item.Message messageItem = (Item.Message) ois.readObject();
                mClient.getMessageManager().sendPayload(messageItem.getId(), (MMXid[])
                                GlobalAddress.convertDestination(messageItem.getDestination()),
                        messageItem.getPayload(), messageItem.getOptions());
                break;
              case PUBSUB:
                MMXPubSubManager psm = mClient.getPubSubManager();
                Item.PubSub pubSubItem = (Item.PubSub) ois.readObject();
                psm.publishToTopic(pubSubItem.getId(), pubSubItem.getRealTopic(),
                        pubSubItem.getTopic(), pubSubItem.getPayload());
                break;
              default:
                Log.w(TAG, "processPendingItems():  Skipping unknown item type: " + type);
            }
          } catch (Exception e) {
            Log.e(TAG, "processPendingItems() Caught exception while processing item: " + id, e);
          } finally {
            if (ois != null) {
              try {
                ois.close();
              } catch (IOException e) {
                Log.w(TAG, "processPendingItems(): Unable to close ObjectInputStream.", e);
              }
            }
            //remove the file now that we don't need it anymore
            boolean success = itemFile.delete();
            if (!success) {
              Log.w(TAG, "processPendingItems(): Unable to remove file: " + itemFile.getName());
            }
          }
        } else {
          Log.e(TAG, "processPendingItems(): Item file NOT found for pending: " + id + ".  Skipping item.");
        }
        //remove the record from the table
        int deleteCount = db.delete(QueueDatabaseHelper.QUEUE_ITEM_TABLE,
                QueueDatabaseHelper.QUEUE_ITEM_ID + "=?", new String[] {id});
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "processPendingItems(): Removed db entry for item id: " + id + ". count=" + deleteCount);
        }
      }
    } finally {
      if (itemCursor != null) {
        itemCursor.close();
      }
      if (db != null) {
        db.close();
      }
    }
  }

  /**
   * Removes all items from the queue
   */
  public synchronized void removeAllItems() {
    String dbPath = mDatabaseHelper.getReadableDatabase().getPath();
    SQLiteDatabase.deleteDatabase(new File(dbPath));
    File[] files = mItemDir.listFiles();
    for (File file : files) {
      boolean success = file.delete();
      if (!success) {
        Log.w(TAG, "removeAllItems(): Unable to remove file " + file.getName());
      }
    }
  }

  /**
   * Removes a single item from the queue
   *
   * @param id the id of the item to remove
   * @return true is successful, false otherwise
   */
  public synchronized boolean removeItem(String id) {
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "removeItem(): attempting to cancel item: " + id);
      }
      db = mDatabaseHelper.getWritableDatabase();
      String[] whereArgs = new String[] {id};
      cursor = db.query(QueueDatabaseHelper.QUEUE_ITEM_TABLE, null,
              QueueDatabaseHelper.QUEUE_ITEM_ID + "=?", whereArgs, null, null, null);
      if (cursor.moveToNext()) {
        //message can be canceled
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "removeItem(): removing item from queue: " + id);
        }
        int wrapperPathIndex = cursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_PATH);
        String wrapperPath = cursor.getString(wrapperPathIndex);
        File wrapperFile = new File(wrapperPath);
        boolean success = wrapperFile.delete();
        int rowsDeleted = db.delete(QueueDatabaseHelper.QUEUE_ITEM_TABLE,
                QueueDatabaseHelper.QUEUE_ITEM_ID + "=?", whereArgs);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "removeItem(): wrapper file delection success=" + success + ". Rows deleted=" + rowsDeleted);
        }
        return true;
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      if (db != null) {
        db.close();
      }
    }
    return false;
  }

  /**
   * Retrieves the identifiers of all the pending items in the queue
   *
   * @return the identifiers of the pending items
   */
  public synchronized Set<String> getPendingItemIds() {
    HashSet<String> pendingItems = new HashSet<String>();
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = mDatabaseHelper.getReadableDatabase();
      cursor = db.query(QueueDatabaseHelper.QUEUE_ITEM_TABLE, null, null, null, null, null, null);
      int idIndex = cursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_ID);
      while (cursor.moveToNext()) {
        pendingItems.add(cursor.getString(idIndex));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      if (db != null) {
        db.close();
      }
    }
    return pendingItems;
  }
  
  /**
   * Get the pending items based on the type.  To avoid blowing up the Java
   * heap, <code>discardPayload</code> should be set to true.
   * @param type
   * @param discardPayload true to exclude the payload.
   * @return
   */
  public synchronized Map<String, MMXQueue.Item> getPendingItems(
                                Item.Type type, boolean discardPayload) {
    Map<String, MMXQueue.Item> items = new HashMap<String, MMXQueue.Item>();
    SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
    Cursor itemCursor = null;
    try {
      itemCursor = db.query(QueueDatabaseHelper.QUEUE_ITEM_TABLE, null, "type=?", 
          new String[] { type.toString() }, null, null, null);
      int idIdx = itemCursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_ID);
      int pathIdx = itemCursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_PATH);
      int typeIdx = itemCursor.getColumnIndex(QueueDatabaseHelper.QUEUE_ITEM_TYPE);
      while (itemCursor.moveToNext()) {
        String id = itemCursor.getString(idIdx);
        String itemPath = itemCursor.getString(pathIdx);
        File itemFile = new File(itemPath);
        if (!itemFile.exists()) {
          continue;
        }
        
        Item.Type itemType = Item.Type.valueOf(itemCursor.getString(typeIdx));
        ObjectInputStream ois = null;
        try {
          FileInputStream fis = new FileInputStream(itemFile);
          ois = new ObjectInputStream(mClient.mEncryptor.decodeStream(fis));
          switch (itemType) {
          case MESSAGE:
            Item.Message msg = (Item.Message) ois.readObject();
            if (discardPayload) {
              msg.setPayload(null);
            }
            items.put(msg.getId(), msg);
            break;
          case PUBSUB:
            Item.PubSub pubitem = (Item.PubSub) ois.readObject();
            if (discardPayload) {
              pubitem.setPayload(null);
            }
            items.put(pubitem.getId(), pubitem);
            break;
          default:
            Log.w(TAG, "getPendingItems(): Skipping unknown item type: "+itemType);
          }
        } catch (Throwable e) {
          Log.e(TAG, "getPendingItems(): cannot read "+itemType+" item: "+id, e);
        } finally {
          if (ois != null) {
            try {
              ois.close();
            } catch (IOException e) {
              // Ignored.
            }
          }
        }
      }
    } finally {
      if (itemCursor != null) {
        itemCursor.close();
      }
      if (db != null) {
        db.close();
      }
    }
    return items;
  }
}
