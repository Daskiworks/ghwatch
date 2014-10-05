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
package com.daskiworks.ghwatch.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaBean to hold info about GitHub notification stream.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class NotificationStream implements Serializable, Iterable<Notification> {

  private static final long serialVersionUID = 4L;

  private LinkedHashMap<Long, Notification> notifications = new LinkedHashMap<Long, Notification>();

  // internal caches
  private transient List<Notification> items = null;
  private transient List<NotifCount> repoInfo = null;

  private void clearInternalCaches() {
    items = null;
    repoInfo = null;
  }

  /**
   * Timestamp when stream was loaded from server as full update.
   */
  private long lastFullUpdateTimestamp;

  /**
   * Content of Last-Modified header from last server call
   */
  private String lastModified;

  /**
   * Add notification to the stream. If id already exists in stream no new is added (because we give newer notif at the begin of stream)
   * 
   * @param notification
   */
  public void addNotification(Notification notification) {
    if (!notifications.containsKey(notification.getId())) {
      notifications.put(notification.getId(), notification);
      clearInternalCaches();
    }
  }

  /**
   * Remove notification from stream by id.
   * 
   * @param id to remove notification for
   */
  public void removeNotificationById(long id) {
    notifications.remove(id);
    clearInternalCaches();
  }

  /**
   * Check if stream contains notification.
   * 
   * @param notification to check
   * @return true if stream contains given notification
   */
  public boolean contains(Notification notification) {
    if (notification == null)
      return false;
    return notifications.containsKey(notification.getId());
  }

  /**
   * Get number of notifications in stream.
   * 
   * @return number of notifications
   */
  public int size() {
    return notifications.size();
  }

  /**
   * Get notification from given position in stream.
   * 
   * @param position to get notification for.
   * @return notification, or null if position is out of number of notifications.
   */
  public Notification get(int position) {
    if (position < 0 || position >= notifications.size())
      return null;
    initItemList();
    return items.get(position);
  }

  /**
   * Get notification with given <code>id</code> in stream.
   * 
   * @param id to notification for.
   * @return notification, or null if not in stream.
   */
  public Notification getNotificationById(long id) {
    return notifications.get(id);
  }

  protected void initItemList() {
    if (items == null) {
      items = new ArrayList<Notification>();
      if (!notifications.isEmpty())
        items.addAll(notifications.values());
    }
  }

  public long getLastFullUpdateTimestamp() {
    return lastFullUpdateTimestamp;
  }

  public void setLastFullUpdateTimestamp(long lastUpdateTimestamp) {
    this.lastFullUpdateTimestamp = lastUpdateTimestamp;
  }

  public String getLastModified() {
    return lastModified;
  }

  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * Check if this stream contains some new notifications compared to provided old stream.
   * 
   * @param oldStream to check against
   * @return true if there are some new notifications.
   */
  public boolean isNewNotification(NotificationStream oldStream) {
    initItemList();

    if (oldStream == null || oldStream.isEmpty()) {
      return !items.isEmpty();
    }

    for (Notification n : items) {
      Notification oldNotif = oldStream.getNotificationById(n.getId());
      if (oldNotif == null) {
        return true;
      } else {
        Date d1 = n.getUpdatedAt();
        Date d2 = oldNotif.getUpdatedAt();
        if ((d1 == null && d2 != null) || (d1 != null && d2 == null) || (d1 != null && d2 != null && !d1.equals(d2)))
          return true;
      }

    }
    return false;
  }

  /**
   * Check if stream is empty or not.
   * 
   * @return true if stream is empty
   */
  public boolean isEmpty() {
    return notifications.isEmpty();
  }

  /**
   * @return true if all notifications are from same repository.
   */
  public boolean allNotificationsFromSameRepository() {
    initItemList();
    if (items == null || items.isEmpty())
      return false;

    String lr = null;

    for (Notification n : items) {
      if (lr == null) {
        lr = n.getRepositoryFullName();
      } else {
        if (!lr.equals(n.getRepositoryFullName()))
          return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<Notification> iterator() {
    initItemList();
    return items.iterator();
  }

  /**
   * Get info about repositories stream contains Notifications for. Info contains repository title and count of notifications in repository.
   * 
   * @return list of info (never null)
   */
  public List<NotifCount> getRepositoriesInfo() {
    if (repoInfo == null) {
      Map<String, NotifCount> m = new HashMap<String, NotifCount>();
      repoInfo = new ArrayList<NotifCount>();
      initItemList();
      if (items != null && !items.isEmpty()) {
        for (Notification n : items) {
          String rn = n.getRepositoryFullName();
          NotifCount nc = m.get(rn);
          if (nc == null) {
            nc = new NotifCount();
            nc.title = rn;
            m.put(rn, nc);
            repoInfo.add(nc);
          }
          nc.count++;
        }
      }
    }
    return repoInfo;
  }

}
