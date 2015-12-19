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

import java.util.Iterator;

import android.test.AndroidTestCase;

/**
 * Unit test for {@link WatchedRepositories}
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class WatchedRepositoriesTest extends AndroidTestCase {

  public void test_addRepository_get_getRepositoryById_size_isEmpty() {
    WatchedRepositories tested = new WatchedRepositories();
    assertEquals(0, tested.size());
    assertTrue(tested.isEmpty());
    assertNull(tested.get(0));
    assertNull(tested.get(-1));
    assertNull(tested.getRepositoryById(10));

    // case - first one added
    Repository on10 = new Repository(10, "test/test1");
    tested.addRepository(on10);
    assertEquals(1, tested.size());
    assertFalse(tested.isEmpty());
    assertEquals(on10, tested.get(0));
    assertEquals(on10, tested.getRepositoryById(10));
    assertNull(tested.getRepositoryById(20));

    // case - second one added, assert order is correct
    Repository on20 = new Repository(20, "test/test2");
    tested.addRepository(on20);
    assertEquals(2, tested.size());
    assertFalse(tested.isEmpty());
    assertEquals(on10, tested.get(0));
    assertEquals(on20, tested.get(1));
    assertNull(tested.get(2));
    assertNull(tested.get(-1));
    assertEquals(on10, tested.getRepositoryById(10));
    assertEquals(on20, tested.getRepositoryById(20));

    // case - no duplicit id's are added, old one instance is preserved inside
    Repository nn10 = new Repository(10, "test/test1");
    tested.addRepository(nn10);
    assertEquals(2, tested.size());
    assertFalse(tested.isEmpty());
    assertNotSame(nn10, tested.getRepositoryById(10));
    assertEquals(on10, tested.getRepositoryById(10));
    assertEquals(on10, tested.get(0));
  }

  public void test_removeRepositoryById() {
    WatchedRepositories tested = new WatchedRepositories();

    Repository on10 = new Repository(10, "test/test1");
    Repository on20 = new Repository(20, "test/test2");
    Repository on30 = new Repository(30, "atest/test1");
    tested.addRepository(on10);
    tested.addRepository(on20);
    tested.addRepository(on30);
    assertEquals(3, tested.size());

    tested.removeRepositoryById(20);
    assertEquals(2, tested.size());
    assertEquals(on30, tested.get(0));
    assertEquals(on10, tested.get(1));
    assertNull(tested.get(2));
  }

  public void test_contains() {
    WatchedRepositories tested = new WatchedRepositories();

    tested.addRepository(new Repository(10, "test/test1"));
    tested.addRepository(new Repository(30, "test/test2"));

    assertFalse(tested.contains(null));
    assertTrue(tested.contains(new Repository(10, "test/test1")));
    assertFalse(tested.contains(new Repository(20, "test/test2")));
    assertTrue(tested.contains(new Repository(30, "test/test3")));
  }

  public void test_LastFullUpdateTimestamp() {
    WatchedRepositories tested = new WatchedRepositories();
    assertEquals(0, tested.getLastFullUpdateTimestamp());

    long now = System.currentTimeMillis();
    tested.setLastFullUpdateTimestamp(now);
    assertEquals(now, tested.getLastFullUpdateTimestamp());
  }

  public void test_iterator() {
    WatchedRepositories tested = new WatchedRepositories();

    Iterator<Repository> i = tested.iterator();
    assertNotNull(i);
    assertFalse(i.hasNext());

    Repository n10 = new Repository(10, "test/test1");
    tested.addRepository(n10);
    i = tested.iterator();
    assertNotNull(i);
    assertTrue(i.hasNext());
    assertEquals(n10, i.next());
    assertFalse(i.hasNext());

    Repository n20 = new Repository(20, "test/test2");
    tested.addRepository(n20);
    i = tested.iterator();
    assertNotNull(i);
    assertTrue(i.hasNext());
    assertEquals(n10, i.next());
    assertTrue(i.hasNext());
    assertEquals(n20, i.next());
    assertFalse(i.hasNext());
  }

}
