package com.Wisam.passenger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nezuma on 1/19/17.
 */

public class DataHolder {
    static Map<String, WeakReference<Object>> data = new HashMap<String, WeakReference<Object>>();


    static void save(String id, Object object) {
        data.put(id, new WeakReference<Object>(object));
    }

    static Object retrieve(String id) {
        WeakReference<Object> objectWeakReference = data.get(id);
        return objectWeakReference.get();
    }
}