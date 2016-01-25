package com.magnet.mmx.client.java;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmicevic on 1/22/16.
 *
 */
public class Counts {

    private Map<String, Integer> counts = new HashMap<String, Integer>();

    private synchronized void addAndStore(String key, int value) {

        Integer val = counts.get(key) == null ? 0 : counts.get(key);
        counts.put(key, val + value);
    }

    public void increment(String key) {
        addAndStore(key, 1);
    }
//    public void decrement(String key) {
//        addAndStore(key, -1);
//    }
    public synchronized int get(String key) {
        return counts.get(key) == null ? 0 : counts.get(key);
    }


 }
