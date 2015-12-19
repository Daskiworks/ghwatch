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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.test.AndroidTestCase;

/**
 * Unit test for {@link NotificationStream}
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class NotificationStreamTest extends AndroidTestCase {

  public void test_addNotification_get_getNotificationById_size_isEmpty() {
    NotificationStream tested = new NotificationStream();
    assertEquals(0, tested.size());
    assertTrue(tested.isEmpty());
    assertNull(tested.get(0));
    assertNull(tested.get(-1));
    assertNull(tested.getNotificationById(10));

    // case - first one added
    Notification on10 = new Notification(10);
    tested.addNotification(on10);
    assertEquals(1, tested.size());
    assertFalse(tested.isEmpty());
    assertEquals(on10, tested.get(0));
    assertEquals(on10, tested.getNotificationById(10));
    assertNull(tested.getNotificationById(20));

    // case - second one aded, assert order is correct
    Notification on20 = new Notification(20);
    tested.addNotification(on20);
    assertEquals(2, tested.size());
    assertFalse(tested.isEmpty());
    assertEquals(on10, tested.get(0));
    assertEquals(on20, tested.get(1));
    assertNull(tested.get(2));
    assertNull(tested.get(-1));
    assertEquals(on10, tested.getNotificationById(10));
    assertEquals(on20, tested.getNotificationById(20));

    // case - no duplicit id's are added, old one instance is preserved inside
    Notification nn10 = new Notification(10);
    tested.addNotification(nn10);
    assertEquals(2, tested.size());
    assertFalse(tested.isEmpty());
    assertNotSame(nn10, tested.getNotificationById(10));
    assertEquals(on10, tested.getNotificationById(10));
    assertEquals(on10, tested.get(0));
  }

  public void test_removeNotificationById() {
    NotificationStream tested = new NotificationStream();

    Notification on10 = new Notification(10);
    Notification on20 = new Notification(20);
    Notification on30 = new Notification(30);
    tested.addNotification(on10);
    tested.addNotification(on20);
    tested.addNotification(on30);
    assertEquals(3, tested.size());

    tested.removeNotificationById(20);
    assertEquals(2, tested.size());
    assertEquals(on10, tested.get(0));
    assertEquals(on30, tested.get(1));
    assertNull(tested.get(2));
  }

  public void test_contains() {
    NotificationStream tested = new NotificationStream();

    tested.addNotification(new Notification(10));
    tested.addNotification(new Notification(30));

    assertFalse(tested.contains(null));
    assertTrue(tested.contains(new Notification(10)));
    assertFalse(tested.contains(new Notification(20)));
    assertTrue(tested.contains(new Notification(30)));
  }

  public void test_LastFullUpdateTimestamp() {
    NotificationStream tested = new NotificationStream();
    assertEquals(0, tested.getLastFullUpdateTimestamp());

    long now = System.currentTimeMillis();
    tested.setLastFullUpdateTimestamp(now);
    assertEquals(now, tested.getLastFullUpdateTimestamp());
  }

  public void test_LastModified() {
    NotificationStream tested = new NotificationStream();
    assertNull(tested.getLastModified());

    tested.setLastModified("LM");
    assertEquals("LM", tested.getLastModified());
  }

  public void test_isNewNotification() {
    NotificationStream tested = new NotificationStream();

    assertFalse(tested.isNewNotification(null));

    NotificationStream oldStream = new NotificationStream();
    assertFalse(tested.isNewNotification(oldStream));

    oldStream.addNotification(new Notification(10));
    assertFalse(tested.isNewNotification(oldStream));

    tested.addNotification(new Notification(10));
    assertFalse(tested.isNewNotification(oldStream));

    tested.addNotification(new Notification(20));
    assertTrue(tested.isNewNotification(oldStream));

    oldStream.addNotification(new Notification(20));
    assertFalse(tested.isNewNotification(oldStream));

    tested.addNotification(new Notification(250));
    tested.addNotification(new Notification(289));
    assertTrue(tested.isNewNotification(oldStream));

    oldStream.addNotification(new Notification(250));
    oldStream.addNotification(new Notification(289));
    assertFalse(tested.isNewNotification(oldStream));

    // case - same date means no new notification
    oldStream.addNotification(new Notification(289, new Date(125)));
    tested.addNotification(new Notification(289, new Date(125)));
    assertFalse(tested.isNewNotification(oldStream));

    // case - changed date means new notification
    oldStream.addNotification(new Notification(290, new Date(125)));
    tested.addNotification(new Notification(290, new Date(1255)));
    assertTrue(tested.isNewNotification(oldStream));

  }

  public void test_allNotificationsFromSameRepository() {
    NotificationStream tested = new NotificationStream();

    assertFalse(tested.allNotificationsFromSameRepository());

    tested.addNotification(new Notification(10, "test/test1"));
    assertTrue(tested.allNotificationsFromSameRepository());

    tested.addNotification(new Notification(20, "test/test1"));
    assertTrue(tested.allNotificationsFromSameRepository());

    tested.addNotification(new Notification(30, "test/test2"));
    assertFalse(tested.allNotificationsFromSameRepository());
  }

  public void test_iterator() {
    NotificationStream tested = new NotificationStream();

    Iterator<Notification> i = tested.iterator();
    assertNotNull(i);
    assertFalse(i.hasNext());

    Notification n10 = new Notification(10);
    tested.addNotification(n10);
    i = tested.iterator();
    assertNotNull(i);
    assertTrue(i.hasNext());
    assertEquals(n10, i.next());
    assertFalse(i.hasNext());

    Notification n20 = new Notification(20);
    tested.addNotification(n20);
    i = tested.iterator();
    assertNotNull(i);
    assertTrue(i.hasNext());
    assertEquals(n10, i.next());
    assertTrue(i.hasNext());
    assertEquals(n20, i.next());
    assertFalse(i.hasNext());
  }

  public void test_getRepositoriesInfo() {
    NotificationStream tested = new NotificationStream();

    List<NotifCount> l = tested.getRepositoriesInfo();
    assertNotNull(l);
    assertEquals(0, l.size());

    tested.addNotification(new Notification(20, "test/test1"));
    l = tested.getRepositoriesInfo();
    assertNotNull(l);
    assertEquals(1, l.size());
    assertNotifCount(l.get(0), "test/test1", 1);

    tested.addNotification(new Notification(30, "test/test2"));
    tested.addNotification(new Notification(40, "test/test1"));
    tested.addNotification(new Notification(50, "test/test1"));
    tested.addNotification(new Notification(60, "test/test2"));
    tested.addNotification(new Notification(70, "atest/test1"));
    l = tested.getRepositoriesInfo();
    assertNotNull(l);
    assertEquals(3, l.size());
    assertNotifCount(l.get(0), "test/test1", 3);
    assertNotifCount(l.get(1), "test/test2", 2);
    assertNotifCount(l.get(2), "atest/test1", 1);
  }

  private void assertNotifCount(NotifCount notifCount, String expectedTitle, int expectedCount) {
    assertEquals(expectedTitle, notifCount.title);
    assertEquals(expectedCount, notifCount.count);
  }
}
