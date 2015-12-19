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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.daskiworks.ghwatch.Utils;

/**
 * Asynchronous internet images loader with caching (in memory and on filesystem).
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class ImageLoader {

  private static final String TAG = "ImageLoader";

  /**
   * Timeout of images in file cache [millis]
   */
  private static final int IMG_FILE_CACHE_TIMEOUT = 24 * 60 * 60 * 1000;

  private Context context;
  private MemoryCache memoryCache = new MemoryCache();
  private FileCache fileCache;
  private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
  private ExecutorService executorService;
  private Map<String, ImageToLoad> imageToLoadTasksMap = new WeakHashMap<String, ImageToLoad>();

  private static ImageLoader instance;

  /**
   * Get instance for use.
   * 
   * @param context to be used in loader
   * @return image loader instance for use
   */
  public static ImageLoader getInstance(Context context) {
    if (instance == null)
      instance = new ImageLoader(context);
    return instance;
  }

  private ImageLoader(Context context) {
    this.context = context.getApplicationContext();
    fileCache = new FileCache(context);
    executorService = Executors.newFixedThreadPool(5);
  }

  /**
   * Display image from given URL - cached.
   * 
   * @param url to get image from
   * @param imageView to display image into
   */
  public void displayImage(String url, ImageView imageView) {
    if (url == null || url.isEmpty())
      imageView.setVisibility(View.INVISIBLE);
    imageViews.put(imageView, url);
    Bitmap bitmap = memoryCache.get(url);
    if (bitmap != null) {
      showImageInView(imageView, bitmap, false);
    } else {
      queueImageLoad(url, imageView);
      setProgressBarVisibility(imageView, true);
    }
  }

  /**
   * Load image for given URL. All caches are used as in {@link #displayImage(String, ImageView)}.
   * 
   * @param url to get image from
   * @return image bitmap or null if not available
   */
  public Bitmap loadImageWithFileLevelCache(String url) {
    if (url == null || url.isEmpty())
      return null;
    Bitmap bitmap = memoryCache.get(url);
    if (bitmap != null)
      return bitmap;
    return loadImageWithFileLevelCache(null, url);
  }

  private void setProgressBarVisibility(ImageView imageView, boolean visible) {
    ProgressBar pb = (ProgressBar) ((View) imageView.getParent()).findViewById(android.R.id.progress);
    if (visible) {
      if (pb != null) {
        pb.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
      }
    } else {
      if (pb != null) {
        pb.setVisibility(View.INVISIBLE);
      }
      imageView.setVisibility(View.VISIBLE);
    }
  }

  private void queueImageLoad(String url, ImageView imageView) {
    synchronized (imageToLoadTasksMap) {
      ImageToLoad p = imageToLoadTasksMap.get(url);
      if (p == null) {
        p = new ImageToLoad(url, imageView);
        imageToLoadTasksMap.put(url, p);
        executorService.submit(new ImageLoaderTask(p));
      } else {
        p.addImageView(imageView);
      }
    }

  }

  protected Bitmap loadImageWithFileLevelCache(ImageToLoad imageToLoad, String url) {

    if (imageToLoad != null)
      url = imageToLoad.url;
    File f = fileCache.getFile(url);

    // get from file cache if exists there and not too old
    if (f.exists()) {
      boolean isTimeoutValid = (f.lastModified() > System.currentTimeMillis() - IMG_FILE_CACHE_TIMEOUT);
      if (isTimeoutValid || imageToLoad != null) {
        Bitmap b = decodeFile(f);
        if (b != null) {
          if (isTimeoutValid) {
            return b;
          } else {
            // temporarily show image before new one is loaded from web
            showImage(imageToLoad, b, false);
          }
        }
      }
    }

    // from web
    if (Utils.isInternetConnectionAvailable(Utils.getConnectivityManager(context))) {

      InputStream is = null;
      OutputStream os = null;
      try {
        Bitmap bitmap = null;
        URL imageUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);
        is = conn.getInputStream();
        if (!f.exists()) {
          Log.d(TAG, "File do not exists: " + f.getAbsolutePath());
          f.createNewFile();
        }
        os = new FileOutputStream(f);
        Utils.copyStream(is, os);
        os.close();
        os = null;
        bitmap = decodeFile(f);
        return bitmap;
      } catch (Throwable ex) {
        Log.w(TAG, "Can't load image from server for URL " + url + " due exception: " + ex.getMessage(), ex);
        if (ex instanceof OutOfMemoryError)
          memoryCache.clear();
      } finally {
        Utils.closeStream(os);
        Utils.closeStream(is);
      }
    }

    if (f.exists()) {
      // not connected to internet or some error, so use from file if available (even timed out) to show at least something
      return decodeFile(f);
    }
    return null;
  }

  // decodes image and scales it to reduce memory consumption
  private Bitmap decodeFile(File f) {
    if (!f.exists())
      return null;
    FileInputStream stream1 = null;
    FileInputStream stream2 = null;
    try {
      // decode image size
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;
      stream1 = new FileInputStream(f);
      BitmapFactory.decodeStream(stream1, null, o);
      stream1.close();

      // Find the correct scale value. It should be the power of 2.
      final int REQUIRED_SIZE = 70;
      int width_tmp = o.outWidth, height_tmp = o.outHeight;
      int scale = 1;
      while (true) {
        if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
          break;
        width_tmp /= 2;
        height_tmp /= 2;
        scale *= 2;
      }

      // decode with inSampleSize
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = scale;
      stream2 = new FileInputStream(f);
      Bitmap bitmap = BitmapFactory.decodeStream(stream2, null, o2);
      stream2.close();
      return bitmap;

    } catch (Exception e) {
      Log.w(TAG, "Can't decode cached image file due exception: " + e.getMessage(), e);
    } finally {
      Utils.closeStream(stream1);
      Utils.closeStream(stream2);
    }
    return null;
  }

  private class ImageToLoad {
    public String url;
    public boolean isShownAlready = false;
    Set<WeakReference<ImageView>> imageViewList = new HashSet<WeakReference<ImageView>>();

    public ImageToLoad(String u, ImageView i) {
      url = u;
      imageViewList.add(new WeakReference<ImageView>(i));
    }

    public void addImageView(ImageView i) {
      imageViewList.add(new WeakReference<ImageView>(i));
    }
  }

  class ImageLoaderTask implements Runnable {
    ImageToLoad imageToLoad;

    ImageLoaderTask(ImageToLoad photoToLoad) {
      this.imageToLoad = photoToLoad;
    }

    @Override
    public void run() {
      try {
        synchronized (imageToLoadTasksMap) {
          if (!isAtLeastOneImageViewValid(imageToLoad)) {
            imageToLoadTasksMap.remove(imageToLoad.url);
            return;
          }
        }
        Bitmap bmp = loadImageWithFileLevelCache(imageToLoad, null);
        if (bmp != null)
          memoryCache.put(imageToLoad.url, bmp);
        showImage(imageToLoad, bmp, true);
      } catch (Throwable th) {
        Log.e(TAG, "Image loading error: " + th.getMessage(), th);
      }
    }

  }

  private void showImage(ImageToLoad imageToLoad, Bitmap bmp, boolean removeFromMap) {
    synchronized (imageToLoadTasksMap) {
      if (removeFromMap)
        imageToLoadTasksMap.remove(imageToLoad.url);
      if (isAtLeastOneImageViewValid(imageToLoad)) {
        BitmapDisplayerTask bd = new BitmapDisplayerTask(bmp, imageToLoad);
        Activity a = getActivity(imageToLoad.imageViewList);
        if (a != null)
          a.runOnUiThread(bd);
      }
    }
  }

  private Activity getActivity(Set<WeakReference<ImageView>> s) {
    for (WeakReference<ImageView> wr : s) {
      if (wr != null) {
        ImageView iv = wr.get();
        if (iv != null)
          return (Activity) iv.getContext();
      }
    }
    return null;
  }

  boolean isAtLeastOneImageViewValid(ImageToLoad photoToLoad) {
    for (WeakReference<ImageView> wr : photoToLoad.imageViewList) {
      if (wr != null)
        if (isImageViewValid(photoToLoad, wr.get()))
          return true;
    }
    return false;
  }

  private boolean isImageViewValid(ImageToLoad photoToLoad, ImageView iv) {
    if (iv == null)
      return false;
    String tag = imageViews.get(iv);
    if (tag != null && tag.equals(photoToLoad.url)) {
      return true;
    }
    return false;
  }

  class BitmapDisplayerTask implements Runnable {
    Bitmap bitmap;
    ImageToLoad imageToLoad;

    public BitmapDisplayerTask(Bitmap b, ImageToLoad p) {
      bitmap = b;
      imageToLoad = p;
    }

    public void run() {
      for (WeakReference<ImageView> wr : imageToLoad.imageViewList) {
        if (wr != null) {
          ImageView iv = wr.get();
          if (isImageViewValid(imageToLoad, iv)) {
            if (bitmap != null) {
              showImageInView(iv, bitmap, !imageToLoad.isShownAlready);
            }
          }
        }
      }
      imageToLoad.isShownAlready = true;
    }
  }

  protected void showImageInView(ImageView iv, Bitmap bitmap, boolean animate) {
    iv.clearAnimation();
    if (animate)
      iv.setAlpha(0f);
    iv.setImageBitmap(bitmap);
    setProgressBarVisibility(iv, false);
    if (animate)
      iv.animate().alpha(1f).setDuration(200).start();
  }

  /**
   * Clear memory cache.
   */
  public void clearCache() {
    memoryCache.clear();
  }

}
