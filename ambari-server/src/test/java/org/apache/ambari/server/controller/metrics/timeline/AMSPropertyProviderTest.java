/**
 * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.ambari.server.controller.metrics.timeline;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AMSPropertyProvider.class, AmbariServer.class})
public class AMSPropertyProviderTest {
  private static final String PROPERTY_ID1 = PropertyHelper.getPropertyId("metrics/cpu", "cpu_user");
  private static final String PROPERTY_ID2 = PropertyHelper.getPropertyId("metrics/memory", "mem_free");
  private static final String PROPERTY_ID3 = PropertyHelper.getPropertyId("metrics/dfs/datanode", "blocks_replicated");
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");

  private static final String FILE_PATH_PREFIX = "ams" + File.separator;
  private static final String SINGLE_HOST_METRICS_FILE_PATH = FILE_PATH_PREFIX + "single_host_metric.json";
  private static final String MULTIPLE_HOST_METRICS_FILE_PATH = FILE_PATH_PREFIX + "multiple_host_metrics.json";
  private static final String SINGLE_COMPONENT_METRICS_FILE_PATH = FILE_PATH_PREFIX + "single_component_metrics.json";
  private static final String MULTIPLE_COMPONENT_REGEXP_METRICS_FILE_PATH = FILE_PATH_PREFIX + "multiple_component_regexp_metrics.json";
  private static final String EMBEDDED_METRICS_FILE_PATH = FILE_PATH_PREFIX + "embedded_host_metric.json";
  private static final String AGGREGATE_METRICS_FILE_PATH = FILE_PATH_PREFIX + "aggregate_component_metric.json";

  @Test
  public void testPopulateResourcesForSingleHostMetric() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244701L, 1416445244901L, 1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID1), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "cpu_user");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");
    uriBuilder.addParameter("startTime", "1416445244701");
    uriBuilder.addParameter("endTime", "1416445244901");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertNotNull("No value for property " + PROPERTY_ID1, val);
    Assert.assertEquals(111, val.length);
  }

  @Test
  public void testPopulateResourcesForSingleHostMetricPointInTime() throws Exception {
    setUpCommonMocks();

    // given
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = Collections.emptyMap();
    Request request = PropertyHelper.getReadRequest(Collections.singleton
      (PROPERTY_ID1), temporalInfoMap);
    System.out.println(request);

    // when
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);

    // then
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(res);
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "cpu_user");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Double val = (Double) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertEquals(41.088, val, 0.001);
  }

  @Test
  public void testPopulateResourcesForMultipleHostMetricscPointInTime() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_HOST_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = Collections.emptyMap();
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{ add(PROPERTY_ID1); add(PROPERTY_ID2); }}, temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "cpu_user,mem_free");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");

    URIBuilder uriBuilder2 = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder2.addParameter("metricNames", "mem_free,cpu_user");
    uriBuilder2.addParameter("hostname", "h1");
    uriBuilder2.addParameter("appId", "HOST");
    Assert.assertTrue(uriBuilder.toString().equals(streamProvider.getLastSpec())
        || uriBuilder2.toString().equals(streamProvider.getLastSpec()));
    Double val1 = (Double) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertNotNull("No value for property " + PROPERTY_ID1, val1);
    Assert.assertEquals(41.088, val1, 0.001);
    Double val2 = (Double)res.getPropertyValue(PROPERTY_ID2);
    Assert.assertNotNull("No value for property " + PROPERTY_ID2, val2);
    Assert.assertEquals(2.47025664E8, val2, 0.1);
  }

  @Test
  public void testPopulateResourcesForMultipleHostMetrics() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_HOST_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244701L, 1416445244901L, 1L));
    temporalInfoMap.put(PROPERTY_ID2, new TemporalInfoImpl(1416445244701L, 1416445244901L, 1L));
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{ add(PROPERTY_ID1); add(PROPERTY_ID2); }}, temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "cpu_user,mem_free");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");
    uriBuilder.addParameter("startTime", "1416445244701");
    uriBuilder.addParameter("endTime", "1416445244901");

    URIBuilder uriBuilder2 = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder2.addParameter("metricNames", "mem_free,cpu_user");
    uriBuilder2.addParameter("hostname", "h1");
    uriBuilder2.addParameter("appId", "HOST");
    uriBuilder2.addParameter("startTime", "1416445244701");
    uriBuilder2.addParameter("endTime", "1416445244901");
    Assert.assertTrue(uriBuilder.toString().equals(streamProvider.getLastSpec())
      || uriBuilder2.toString().equals(streamProvider.getLastSpec()));
    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertEquals(111, val.length);
    val = (Number[][]) res.getPropertyValue(PROPERTY_ID2);
    Assert.assertEquals(86, val.length);
  }

  @Test
  public void testPopulateResourcesForRegexpMetrics() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_COMPONENT_REGEXP_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds =
        new HashMap<String, Map<String, PropertyInfo>>() {{
      put("RESOURCEMANAGER", new HashMap<String, PropertyInfo>() {{
        put("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AvailableMB",
            new PropertyInfo("yarn.QueueMetrics.Queue=(.+).AvailableMB", true, false));
      }});
    }};

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
        propertyIds,
        streamProvider,
        sslConfiguration,
        metricHostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID
    );


    String propertyId1 = "metrics/yarn/Queue/root/AvailableMB";
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");// should be set?
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(propertyId1, new TemporalInfoImpl(1416528819369L, 1416528819569L, 1L));
    Request request = PropertyHelper.getReadRequest(
        Collections.singleton(propertyId1), temporalInfoMap);
    Set<Resource> resources =
        propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "yarn.QueueMetrics.Queue=root.AvailableMB");
    uriBuilder.addParameter("appId", "RESOURCEMANAGER");
    uriBuilder.addParameter("startTime", "1416528819369");
    uriBuilder.addParameter("endTime", "1416528819569");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue("metrics/yarn/Queue/root/AvailableMB");
    Assert.assertNotNull("No value for property metrics/yarn/Queue/root/AvailableMB", val);
    Assert.assertEquals(238, val.length);
  }

  @Test
  public void testPopulateResourcesForSingleComponentMetric() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_COMPONENT_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Component);

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/rpc", "RpcQueueTime_avg_time");
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1416528819369L, 1416528819569L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "rpc.rpc.RpcQueueTimeAvgTime");
    uriBuilder.addParameter("appId", "NAMENODE");
    uriBuilder.addParameter("startTime", "1416528819369");
    uriBuilder.addParameter("endTime", "1416528819569");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(propertyId);
    Assert.assertNotNull("No value for property " + propertyId, val);
    Assert.assertEquals(238, val.length);
  }

  @Test
  public void testPopulateMetricsForEmbeddedHBase() throws Exception {
    AmbariManagementController ams = createNiceMock(AmbariManagementController.class);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
    expect(ams.getClusters()).andReturn(clusters).anyTimes();
    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService("HDP", "2.2", "METRICS_COLLECTOR")).andReturn("AMS").anyTimes();
    expect(ambariMetaInfo.getComponent("HDP", "2.2", "AMS", "METRICS_COLLECTOR"))
            .andReturn(componentInfo).anyTimes();
    expect(componentInfo.getTimelineAppid()).andReturn("AMS-HBASE");
    replay(ams, clusters, cluster, ambariMetaInfo, componentInfo);
    PowerMock.replayAll();

    TestStreamProvider streamProvider = new TestStreamProvider(EMBEDDED_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Component);

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/hbase/regionserver", "requests");
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "METRICS_COLLECTOR");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1421694000L, 1421697600L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "regionserver.Server.totalRequestCount");
    uriBuilder.addParameter("appId", "AMS-HBASE");
    uriBuilder.addParameter("startTime", "1421694000");
    uriBuilder.addParameter("endTime", "1421697600");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(propertyId);
    Assert.assertEquals(189, val.length);
  }

  @Test
  public void testAggregateFunctionForComponentMetrics() throws Exception {
    AmbariManagementController ams = createNiceMock(AmbariManagementController.class);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
    expect(ams.getClusters()).andReturn(clusters).anyTimes();
    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService("HDP", "2.2", "HBASE_REGIONSERVER")).andReturn("HBASE").anyTimes();
    expect(ambariMetaInfo.getComponent("HDP", "2.2", "HBASE", "HBASE_REGIONSERVER"))
            .andReturn(componentInfo).anyTimes();
    expect(componentInfo.getTimelineAppid()).andReturn("HBASE");
    replay(ams, clusters, cluster, ambariMetaInfo, componentInfo);
    PowerMock.replayAll();

    TestStreamProvider streamProvider = new TestStreamProvider(AGGREGATE_METRICS_FILE_PATH);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Component);
    PropertyHelper.updateMetricsWithAggregateFunctionSupport(propertyIds);

    AMSComponentPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/rpc", "NumOpenConnections._sum");
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1429824611300L, 1429825241400L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder.addParameter("metricNames", "rpc.rpc.NumOpenConnections._sum");
    uriBuilder.addParameter("appId", "HBASE");
    uriBuilder.addParameter("startTime", "1429824611300");
    uriBuilder.addParameter("endTime", "1429825241400");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(propertyId);
    Assert.assertEquals(32, val.length);
  }

  static class TestStreamProviderForHostComponentHostMetricsTest extends TestStreamProvider {
    String hostMetricFilePath = FILE_PATH_PREFIX + "single_host_metric.json";
    String hostComponentMetricFilePath = FILE_PATH_PREFIX + "single_host_component_metrics.json";
    Set<String> specs = new HashSet<String>();

    public TestStreamProviderForHostComponentHostMetricsTest(String fileName) {
      super(fileName);
    }

    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (spec.contains("HOST")) {
        this.fileName = hostMetricFilePath;
      } else {
        this.fileName = hostComponentMetricFilePath;
      }

      specs.add(spec);

      return super.readFrom(spec);
    }

    public Set<String> getAllSpecs() {
      return specs;
    }
  }

  @Test
  public void testPopulateResourcesForHostComponentHostMetrics() throws Exception {
    setUpCommonMocks();
    TestStreamProviderForHostComponentHostMetricsTest streamProvider =
      new TestStreamProviderForHostComponentHostMetricsTest(null);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    AMSPropertyProvider propertyProvider = new AMSHostComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244701L, 1416445251802L, 1L));
    temporalInfoMap.put(PROPERTY_ID3, new TemporalInfoImpl(1416445244701L, 1416445251802L, 1L));
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{ add(PROPERTY_ID1); add(PROPERTY_ID3); }},
      temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);

    Set<String> specs = streamProvider.getAllSpecs();
    Assert.assertEquals(2, specs.size());
    String hostMetricSpec = null;
    String hostComponentMetricsSpec = null;
    for (String spec : specs) {
      if (spec.contains("HOST")) {
        hostMetricSpec = spec;
      } else {
        hostComponentMetricsSpec = spec;
      }
    }
    Assert.assertNotNull(hostMetricSpec);
    Assert.assertNotNull(hostComponentMetricsSpec);
    // Verify calls
    URIBuilder uriBuilder1 = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder1.addParameter("metricNames", "dfs.datanode.BlocksReplicated");
    uriBuilder1.addParameter("hostname", "h1");
    uriBuilder1.addParameter("appId", "DATANODE");
    uriBuilder1.addParameter("startTime", "1416445244701");
    uriBuilder1.addParameter("endTime", "1416445251802");
    Assert.assertEquals(uriBuilder1.toString(), hostComponentMetricsSpec);

    URIBuilder uriBuilder2 = AMSPropertyProvider.getAMSUriBuilder("localhost", 8188);
    uriBuilder2.addParameter("metricNames", "cpu_user");
    uriBuilder2.addParameter("hostname", "h1");
    uriBuilder2.addParameter("appId", "HOST");
    uriBuilder2.addParameter("startTime", "1416445244701");
    uriBuilder2.addParameter("endTime", "1416445251802");
    Assert.assertEquals(uriBuilder2.toString(), hostMetricSpec);

    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertEquals(111, val.length);
    val = (Number[][]) res.getPropertyValue(PROPERTY_ID3);
    Assert.assertNotNull("No value for property " + PROPERTY_ID3, val);
    Assert.assertEquals(8, val.length);
  }

  public static class TestMetricHostProvider implements MetricHostProvider {

    @Override
    public String getCollectorHostName(String clusterName, MetricsService service)
      throws SystemException {
      return "localhost";
    }

    @Override
    public String getHostName(String clusterName, String componentName) throws SystemException {
      return "h1";
    }

    @Override
    public String getCollectorPortName(String clusterName, MetricsService service) throws SystemException {
      return "8188";
    }

    @Override
    public boolean isCollectorHostLive(String clusterName, MetricsService service) throws SystemException {
      return true;
    }

    @Override
    public boolean isCollectorComponentLive(String clusterName, MetricsService service) throws SystemException {
      return true;
    }
  }

  private void setUpCommonMocks() throws AmbariException {
    AmbariManagementController ams = createNiceMock(AmbariManagementController.class);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
    expect(ams.getClusters()).andReturn(clusters).anyTimes();
    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService(anyObject(String.class),
            anyObject(String.class), anyObject(String.class))).andReturn("HDFS").anyTimes();
    expect(ambariMetaInfo.getComponent(anyObject(String.class),anyObject(String.class),
            anyObject(String.class), anyObject(String.class)))
            .andReturn(componentInfo).anyTimes();
    replay(ams, clusters, cluster, ambariMetaInfo);
    PowerMock.replayAll();
  }
}
