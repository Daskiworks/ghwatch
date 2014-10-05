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

import com.daskiworks.ghwatch.Utils;

/**
 * JavaBean to hold info about one Github repository.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class Repository implements Serializable {

  private static final long serialVersionUID = 2L;

  private long id;
  private String url;
  private String htmlUrl;
  private String repositoryFullName;
  private String repositoryAvatarUrl;

  /**
   * Constructor for unit tests.
   * 
   * @param id
   * @param repositoryFullName
   */
  protected Repository(long id, String repositoryFullName) {
    super();
    this.id = id;
    this.repositoryFullName = repositoryFullName;
  }

  public Repository(long id, String url, String repositoryFullName, String repositoryAvatarUrl, String htmlUrl) {
    super();
    this.id = id;
    this.url = Utils.trimToNull(url);
    this.repositoryFullName = repositoryFullName;
    this.repositoryAvatarUrl = Utils.trimToNull(repositoryAvatarUrl);
    this.htmlUrl = Utils.trimToNull(htmlUrl);
  }

  public long getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getHtmlUrl() {
    return htmlUrl;
  }

  public String getRepositoryFullName() {
    return repositoryFullName;
  }

  public String getRepositoryAvatarUrl() {
    return repositoryAvatarUrl;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Repository other = (Repository) obj;
    if (id != other.id)
      return false;
    return true;
  }

}
