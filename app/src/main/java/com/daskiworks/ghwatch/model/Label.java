/*
 * Copyright 2016 contributors as indicated by the @authors tag.
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

/**
 * Label for notification subject (issue or pull request). Contains label name and color for the label badge background (color in hex, eg. <code>fc2929</code>).
 *
 * @author Vlastimil Elias <vlastimil.elias@worldonline.cz>
 */
public class Label implements Serializable {

  private static final long serialVersionUID = 1L;

  String name;
  String color;

  public Label(String name, String color) {
    this.name = name;
    this.color = color;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Label{" +
        "name='" + name + '\'' +
        ", color='" + color + '\'' +
        '}';
  }
}
