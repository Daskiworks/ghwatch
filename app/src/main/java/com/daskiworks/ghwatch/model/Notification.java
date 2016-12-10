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

import android.graphics.Color;

import com.daskiworks.ghwatch.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JavaBean to hold info about one Github notification.
 * 
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 * 
 */
public class Notification implements Serializable {

  private static final long serialVersionUID = 3L;

  private long id;
  private String url;
  private String subjectTitle;
  private String subjectType;
  private String subjectUrl;
  private String subjectLatestCommentUrl;
  private String repositoryFullName;
  private String repositoryAvatarUrl;
  private String reason;
  private Date updatedAt;

  /**
   * Flag indicating that that details (fields marked DETAIL) had been loaded from server already.
   */
  private boolean detailLoaded = false;
  private String subjectDetailHtmlUrl;

  /**
   * Status related to the notification: open, closed, merged (DETAIL)
   */
  private String subjectStatus;

  /**
   * List of labels for the notification subject (DETAIL)
   */
  private List<Label> subjectLabels;

  /**
   * Constructor for unit tests.
   * 
   * @param id to be used
   */
  protected Notification(long id) {
    this.id = id;
  }

  /**
   * Constructor for unit tests.
   * 
   * @param id to be used
   */
  protected Notification(long id, Date updatedAt) {
    this.id = id;
    this.updatedAt = updatedAt;
  }

  /**
   * Constructor for unit tests.
   * 
   * @param id to be used
   * @param repositoryFullName to be used
   */
  protected Notification(long id, String repositoryFullName) {
    this.id = id;
    this.repositoryFullName = repositoryFullName;
  }

  /**
   * Filling constructor for common use.
   * 
   */
  public Notification(long id, String url, String subjectTitle, String subjectType, String subjectUrl, String subjectLatestCommentUrl,
      String repositoryFullName, String repositoryAvatarUrl, Date updatedAt, String reason) {
    super();
    this.id = id;
    this.url = Utils.trimToNull(url);
    this.subjectTitle = subjectTitle;
    this.subjectType = subjectType;
    this.subjectUrl = Utils.trimToNull(subjectUrl);
    this.subjectLatestCommentUrl = Utils.trimToNull(subjectLatestCommentUrl);
    this.repositoryFullName = repositoryFullName;
    this.repositoryAvatarUrl = Utils.trimToNull(repositoryAvatarUrl);
    this.updatedAt = updatedAt;
    this.reason = reason;
  }

  public long getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getSubjectTitle() {
    return subjectTitle;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public String getSubjectUrl() {
    return subjectUrl;
  }

  public String getSubjectLatestCommentUrl() {
    return subjectLatestCommentUrl;
  }

  /**
   * Get base URL of notification (API URL) to get view URL from (using other REST request to get "html_url" from).
   * 
   * @return
   */
  public String getViewBaseUrl() {
    if (subjectLatestCommentUrl != null)
      return subjectLatestCommentUrl;
    else
      return subjectUrl;
  }

  public String getRepositoryFullName() {
    return repositoryFullName;
  }

  public String getRepositoryAvatarUrl() {
    return repositoryAvatarUrl;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public String getReason() {
    return reason;
  }

  public boolean isDetailLoaded() {
    return detailLoaded;
  }

  public void setDetailLoaded(boolean detailLoaded) {
    this.detailLoaded = detailLoaded;
  }

  public String getSubjectDetailHtmlUrl() {
    return subjectDetailHtmlUrl;
  }

  public void setSubjectDetailHtmlUrl(String subjectDetailHtmlUrl) {
    this.subjectDetailHtmlUrl = subjectDetailHtmlUrl;
  }

  public String getSubjectStatus() {
    return subjectStatus;
  }

  /**
   * Get color for subject status.
   * 
   * @return integer with color or null if status is not available
   */
  public Integer getSubjectStatusColor() {
    if ("open".equalsIgnoreCase(subjectStatus)) {
      return Color.parseColor("#6CC644");
    } else if ("closed".equalsIgnoreCase(subjectStatus)) {
      return Color.parseColor("#B34545");
      // return Color.parseColor("#BD2C00");
    } else if ("merged".equalsIgnoreCase(subjectStatus)) {
      return Color.parseColor("#6E5494");
    } else {
      return null;
    }
  }

  public void setSubjectStatus(String subjectStatus) {
    this.subjectStatus = subjectStatus;
  }

  public List<Label> getSubjectLabels() {
    return subjectLabels;
  }

  public void setSubjectLabels(List<Label> subjectLabels) {
    this.subjectLabels = subjectLabels;
  }

  public void addSubjectLabel(Label label){
    if(subjectLabels == null){
      subjectLabels = new ArrayList<>();
    }
    subjectLabels.add(label);
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
    Notification other = (Notification) obj;
    if (id != other.id)
      return false;
    return true;
  }

}
