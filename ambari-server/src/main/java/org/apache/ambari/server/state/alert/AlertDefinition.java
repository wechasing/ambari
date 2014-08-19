/**
® * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state.alert;


/**
 * Represents information required to run and collection of alerts.
 */
public class AlertDefinition {

  private String serviceName = null;
  private String componentName = null;

  private String name = null;
  private Scope scope = null;
  private int interval = 1;
  private boolean enabled = true;
  private Source source = null;
  private String label = null;

  /**
   * @return the service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param name the service name
   */
  public void setServiceName(String name) {
    serviceName = name;
  }

  /**
   * @return the component name
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   *
   * @param name the component name
   */
  public void setComponentName(String name) {
    componentName = name;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param definitionName
   *          the definition name.
   */
  public void setName(String definitionName) {
    name = definitionName;
  }

  /**
   * @return the scope
   */
  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope definitionScope) {
    scope = definitionScope;
  }

  /**
   * @return the interval
   */
  public int getInterval() {
    return interval;
  }

  public void setInterval(int definitionInterval) {
    interval = definitionInterval;
  }

  /**
   * @return {@code true} if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean definitionEnabled) {
    enabled = definitionEnabled;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source definitionSource) {
    source = definitionSource;
  }

  /**
   * @return the label for the definition or {@code null} if none.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label for this definition.
   *
   * @param definitionLabel
   */
  public void setLabel(String definitionLabel) {
    label = definitionLabel;
  }

  @Override
  public boolean equals(Object obj) {
    if (null == obj || !obj.getClass().equals(AlertDefinition.class)) {
      return false;
    }

    return name.equals(((AlertDefinition) obj).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

}
