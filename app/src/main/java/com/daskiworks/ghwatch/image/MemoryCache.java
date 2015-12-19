/*
 * Copyright 2014 contributors as indicated by the @authors tag.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daskiworks.ghwatch.image;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Memory cache for images.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class MemoryCache {

  private static final String TAG = "MemoryCache";
  private Map<String, Bitmap> cache = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(10, 1.5f, true));// Last argument true for LRU ordering
  private long size = 0;// current allocated size
  private long limit = 1000000;// max memory in bytes

  public MemoryCache() {
    // use 25% of available heap size
    setLimit(Runtime.getRuntime().maxMemory() / 4);
  }

  public void setLimit(long new_limit) {
    limit = new_limit;
    Log.i(TAG, "MemoryCache will use up to " + limit / 1024. / 1024. + "MB");
  }

  public Bitmap get(String id) {
    try {
      if (!cache.containsKey(id))
        return null;
      return cache.get(id);
    } catch (NullPointerException ex) {
      // NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78
      return null;
    }
  }

  public void put(String id, Bitmap bitmap) {
    try {
      if (cache.containsKey(id))
        size -= getSizeInBytes(cache.get(id));
      cache.put(id, bitmap);
      size += getSizeInBytes(bitmap);
      checkSize();
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  private void checkSize() {
    Log.i(TAG, "cache size=" + size + " length=" + cache.size());
    if (size > limit) {
      Iterator<Entry<String, Bitmap>> iter = cache.entrySet().iterator();// least recently accessed item will be the first one iterated
      while (iter.hasNext()) {
        Entry<String, Bitmap> entry = iter.next();
        size -= getSizeInBytes(entry.getValue());
        iter.remove();
        if (size <= limit)
          break;
      }
      Log.i(TAG, "Clean cache. New size " + cache.size());
    }
  }

  public void clear() {
    Log.i(TAG, "Go to clear MemoryCache");
    try {
      cache.clear();
      size = 0;
    } catch (NullPointerException ex) {
      // NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78
    }
  }

  long getSizeInBytes(Bitmap bitmap) {
    if (bitmap == null)
      return 0;
    return bitmap.getRowBytes() * bitmap.getHeight();
  }
}