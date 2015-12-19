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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * JavaBean to hold info about GitHub watched repositories.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class WatchedRepositories implements Serializable, Iterable<Repository> {

  private static final long serialVersionUID = 4L;

  private LinkedHashMap<Long, Repository> repositories = new LinkedHashMap<Long, Repository>();

  // internal caches
  private transient List<Repository> items = null;

  private void clearInternalCaches() {
    items = null;
  }

  /**
   * Timestamp when list was loaded from server as full update.
   */
  private long lastFullUpdateTimestamp;

  /**
   * Add repository.
   * 
   * @param repository to add
   */
  public void addRepository(Repository repository) {
    if (!repositories.containsKey(repository.getId())) {
      repositories.put(repository.getId(), repository);
      clearInternalCaches();
    }
  }

  /**
   * Remove repository from stream by id.
   * 
   * @param id to remove repository for
   * @return removed object or null
   */
  public Repository removeRepositoryById(long id) {
    Repository ret = repositories.remove(id);
    clearInternalCaches();
    return ret;
  }

  /**
   * Check if stream contains repository.
   * 
   * @param repository to check
   * @return true if stream contains given repo
   */
  public boolean contains(Repository repository) {
    if (repository == null)
      return false;
    return repositories.containsKey(repository.getId());
  }

  /**
   * Get number of repositories in stream.
   * 
   * @return number of repositories
   */
  public int size() {
    return repositories.size();
  }

  /**
   * Get notification from given position in stream.
   * 
   * @param position to get notification for.
   * @return notification, or null if position is out of number of notifications.
   */
  public Repository get(int position) {
    if (position < 0 || position >= repositories.size())
      return null;
    initItemList();
    return items.get(position);
  }

  /**
   * Get repository with given <code>id</code> in stream.
   * 
   * @param id to notification for.
   * @return notification, or null if not in stream.
   */
  public Repository getRepositoryById(long id) {
    return repositories.get(id);
  }

  protected void initItemList() {
    if (items == null) {
      TreeSet<Repository> ts = new TreeSet<Repository>(new Comparator<Repository>() {
        Collator c = Collator.getInstance();

        @Override
        public int compare(Repository lhs, Repository rhs) {
          return c.compare(lhs.getRepositoryFullName(), rhs.getRepositoryFullName());
        }
      });
      if (!repositories.isEmpty())
        ts.addAll(repositories.values());
      items = new ArrayList<Repository>(ts);
    }
  }

  public long getLastFullUpdateTimestamp() {
    return lastFullUpdateTimestamp;
  }

  public void setLastFullUpdateTimestamp(long lastUpdateTimestamp) {
    this.lastFullUpdateTimestamp = lastUpdateTimestamp;
  }

  /**
   * Check if stream is empty or not.
   * 
   * @return true if stream is empty
   */
  public boolean isEmpty() {
    return repositories.isEmpty();
  }

  @Override
  public Iterator<Repository> iterator() {
    initItemList();
    return items.iterator();
  }

}
