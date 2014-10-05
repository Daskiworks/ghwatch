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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.util.Log;

/**
 * File cache for images downloaded from server. External storage is used if available.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class FileCache {

  private static final String TAG = "FileCache";

  private File cacheDir;

  /**
   * Create file cache.
   * 
   * @param context
   */
  public FileCache(Context context) {
    if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
      cacheDir = new File(context.getExternalCacheDir(), "inet_img");
    else
      cacheDir = context.getCacheDir();
    Log.i(TAG, "FileCache will use folder " + cacheDir.getAbsolutePath());
    if (!cacheDir.exists()) {
      if (cacheDir.mkdirs())
        Log.i(TAG, "FileCache folder " + cacheDir.getAbsolutePath() + " created");
    }
  }

  /**
   * Get cache file for given URL.
   * 
   * @param url to get cache file for
   * @return file for given URL
   */
  public File getFile(String url) {
    String filename = url.replace("http://", "").replace("https://", "").replace("/", "-").replace("..", "-");
    try {
      filename = URLEncoder.encode(filename, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // this should never happen
    }
    File f = new File(cacheDir, filename);
    return f;
  }

  /**
   * Clear all files from cache.
   */
  public void clear() {
    Log.i(TAG, "Go to clear FileCache folder " + cacheDir.getAbsolutePath());
    File[] files = cacheDir.listFiles();
    if (files == null)
      return;
    for (File f : files)
      f.delete();
  }

  /**
   * Clear all timeouted files from cache.
   * 
   * @param timeout in milliseconds to delete files for
   */
  public void clear(int timeout) {
    Log.i(TAG, "Go to clear FileCache folder " + cacheDir.getAbsolutePath() + " for files older than " + timeout + "ms.");
    if (timeout < 100)
      return;

    long ts = System.currentTimeMillis() - timeout - 1000;
    File[] files = cacheDir.listFiles();
    if (files == null)
      return;
    for (File f : files) {
      if (f.lastModified() < ts) {
        f.delete();
      }
    }
  }

}