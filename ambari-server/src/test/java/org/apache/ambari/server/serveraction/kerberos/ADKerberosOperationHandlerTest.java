/*
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

package org.apache.ambari.server.serveraction.kerberos;

import junit.framework.Assert;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.IAnswer;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.easymock.EasyMock.*;

public class ADKerberosOperationHandlerTest extends KerberosOperationHandlerTest {
  private static final String DEFAULT_ADMIN_PRINCIPAL = "cluser_admin@HDP01.LOCAL";
  private static final String DEFAULT_ADMIN_PASSWORD = "Hadoop12345";
  private static final String DEFAULT_LDAP_URL = "ldaps://10.0.100.4";
  private static final String DEFAULT_PRINCIPAL_CONTAINER_DN = "ou=HDP,DC=HDP01,DC=LOCAL";
  private static final String DEFAULT_REALM = "HDP01.LOCAL";

  @Test(expected = KerberosKDCConnectionException.class)
  public void testOpenExceptionLdapUrlNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };
    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.close();
  }

  @Test(expected = KerberosLDAPContainerException.class)
  public void testOpenExceptionPrincipalContainerDnNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
      }
    };
    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testOpenExceptionAdminCredentialsNotProvided() throws Exception {
    KerberosOperationHandler handler = new ADKerberosOperationHandler();
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };
    handler.open(null, DEFAULT_REALM, kerberosEnvMap);
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPassword() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, "wrong", null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class))).andAnswer(new IAnswer<LdapContext>() {
      @Override
      public LdapContext answer() throws Throwable {
        throw new AuthenticationException();
      }
    }).once();

    replayAll();

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPrincipal() throws Exception {
    KerberosCredential kc = new KerberosCredential("wrong", DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class))).andAnswer(new IAnswer<LdapContext>() {
      @Override
      public LdapContext answer() throws Throwable {
        throw new AuthenticationException();
      }
    }).once();

    replayAll();

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testTestAdministratorCredentialsKDCConnectionException() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, "invalid");
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class))).andAnswer(new IAnswer<LdapContext>() {
      @Override
      public LdapContext answer() throws Throwable {
        throw new CommunicationException();
      }
    }).once();

    replayAll();

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }


  @Test
  public void testTestAdministratorCredentialsSuccess() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createSearchControls"))
        .createNiceMock();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class)))
        .andAnswer(new IAnswer<LdapContext>() {
          @Override
          public LdapContext answer() throws Throwable {
            LdapContext ldapContext = createNiceMock(LdapContext.class);
            expect(ldapContext.search(anyObject(String.class), anyObject(String.class), anyObject(SearchControls.class)))
                .andAnswer(new IAnswer<NamingEnumeration<SearchResult>>() {
                  @Override
                  public NamingEnumeration<SearchResult> answer() throws Throwable {
                    NamingEnumeration<SearchResult> result = createNiceMock(NamingEnumeration.class);
                    expect(result.hasMore()).andReturn(false).once();
                    replay(result);
                    return result;
                  }
                })
                .once();
            replay(ldapContext);
            return ldapContext;
          }
        })
        .once();
    expect(handler.createSearchControls()).andAnswer(new IAnswer<SearchControls>() {
      @Override
      public SearchControls answer() throws Throwable {
        SearchControls searchControls = createNiceMock(SearchControls.class);
        replay(searchControls);
        return searchControls;
      }
    }).once();

    replayAll();

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test
  public void testProcessCreateTemplateDefault() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
      }
    };

    Capture<Name> capturedName = new Capture<Name>(CaptureType.ALL);
    Capture<Attributes> capturedAttributes = new Capture<Attributes>(CaptureType.ALL);

    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createSearchControls"))
        .createNiceMock();

    NamingEnumeration<SearchResult> searchResult = createNiceMock(NamingEnumeration.class);
    expect(searchResult.hasMore()).andReturn(false).once();

    LdapContext ldapContext = createNiceMock(LdapContext.class);
    expect(ldapContext.search(anyObject(String.class), anyObject(String.class), anyObject(SearchControls.class)))
        .andReturn(searchResult)
        .once();

    expect(ldapContext.createSubcontext(capture(capturedName), capture(capturedAttributes)))
        .andReturn(createNiceMock(DirContext.class))
        .anyTimes();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class)))
        .andReturn(ldapContext)
        .once();

    expect(handler.createSearchControls()).andAnswer(new IAnswer<SearchControls>() {
      @Override
      public SearchControls answer() throws Throwable {
        SearchControls searchControls = createNiceMock(SearchControls.class);
        replay(searchControls);
        return searchControls;
      }
    }).once();

    replayAll();

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.createPrincipal("nn/c6501.ambari.apache.org", "secret", true);
    handler.createPrincipal("hdfs@" + DEFAULT_REALM, "secret", false);
    handler.close();

    List<Attributes> attributesList = capturedAttributes.getValues();
    Attributes attributes;

    attributes = attributesList.get(0);
    String[] objectClasses = new String[]{"top", "person", "organizationalPerson", "user"};

    Assert.assertNotNull(attributes);
    Assert.assertEquals(7, attributes.size());

    Assert.assertNotNull(attributes.get("objectClass"));
    Assert.assertEquals(objectClasses.length, attributes.get("objectClass").size());
    for (int i = 0; i < objectClasses.length; i++) {
      Assert.assertEquals(objectClasses[i], attributes.get("objectClass").get(i));
    }

    Assert.assertNotNull(attributes.get("cn"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", attributes.get("cn").get());

    Assert.assertNotNull(attributes.get("servicePrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", attributes.get("servicePrincipalName").get());

    Assert.assertNotNull(attributes.get("userPrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@HDP01.LOCAL", attributes.get("userPrincipalName").get());

    Assert.assertNotNull(attributes.get("unicodePwd"));
    Assert.assertEquals("\"secret\"", new String((byte[]) attributes.get("unicodePwd").get(), Charset.forName("UTF-16LE")));

    Assert.assertNotNull(attributes.get("accountExpires"));
    Assert.assertEquals("0", attributes.get("accountExpires").get());

    Assert.assertNotNull(attributes.get("userAccountControl"));
    Assert.assertEquals("66048", attributes.get("userAccountControl").get());

    attributes = attributesList.get(1);
    Assert.assertNotNull(attributes);
    Assert.assertEquals(6, attributes.size());

    Assert.assertNotNull(attributes.get("objectClass"));
    Assert.assertEquals(objectClasses.length, attributes.get("objectClass").size());
    for (int i = 0; i < objectClasses.length; i++) {
      Assert.assertEquals(objectClasses[i], attributes.get("objectClass").get(i));
    }

    Assert.assertNotNull(attributes.get("cn"));
    Assert.assertEquals("hdfs", attributes.get("cn").get());

    Assert.assertNotNull(attributes.get("userPrincipalName"));
    Assert.assertEquals("hdfs@HDP01.LOCAL", attributes.get("userPrincipalName").get());

    Assert.assertNotNull(attributes.get("unicodePwd"));
    Assert.assertEquals("\"secret\"", new String((byte[]) attributes.get("unicodePwd").get(), Charset.forName("UTF-16LE")));

    Assert.assertNotNull(attributes.get("accountExpires"));
    Assert.assertEquals("0", attributes.get("accountExpires").get());

    Assert.assertNotNull(attributes.get("userAccountControl"));
    Assert.assertEquals("66048", attributes.get("userAccountControl").get());
  }

  @Test
  public void testProcessCreateTemplateCustom() throws Exception {
    KerberosCredential kc = new KerberosCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>() {
      {
        put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, DEFAULT_LDAP_URL);
        put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, DEFAULT_PRINCIPAL_CONTAINER_DN);
        put(ADKerberosOperationHandler.KERBEROS_ENV_CREATE_ATTRIBUTES_TEMPLATE, "" +
            "#set( $user = \"${principal_primary}-${principal_digest}\" )" +
            "{" +
            "  \"objectClass\": [" +
            "    \"top\"," +
            "    \"person\"," +
            "    \"organizationalPerson\"," +
            "    \"user\"" +
            "  ]," +
            "  \"cn\": \"$user\"," +
            "  \"sAMAccountName\": \"$user.substring(0,20)\"," +
            "  #if( $is_service )" +
            "  \"servicePrincipalName\": \"$principal_name\"," +
            "  #end" +
            "  \"userPrincipalName\": \"$normalized_principal\"," +
            "  \"unicodePwd\": \"$password\"," +
            "  \"accountExpires\": \"0\"," +
            "  \"userAccountControl\": \"66048\"" +
            "}");
      }
    };

    Capture<Name> capturedName = new Capture<Name>();
    Capture<Attributes> capturedAttributes = new Capture<Attributes>();

    ADKerberosOperationHandler handler = createMockBuilder(ADKerberosOperationHandler.class)
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createInitialLdapContext", Properties.class, Control[].class))
        .addMockedMethod(ADKerberosOperationHandler.class.getDeclaredMethod("createSearchControls"))
        .createNiceMock();

    NamingEnumeration<SearchResult> searchResult = createNiceMock(NamingEnumeration.class);
    expect(searchResult.hasMore()).andReturn(false).once();

    LdapContext ldapContext = createNiceMock(LdapContext.class);
    expect(ldapContext.search(anyObject(String.class), anyObject(String.class), anyObject(SearchControls.class)))
        .andReturn(searchResult)
        .once();

    expect(ldapContext.createSubcontext(capture(capturedName), capture(capturedAttributes)))
        .andReturn(createNiceMock(DirContext.class))
        .once();

    expect(handler.createInitialLdapContext(anyObject(Properties.class), anyObject(Control[].class)))
        .andReturn(ldapContext)
        .once();

    expect(handler.createSearchControls()).andAnswer(new IAnswer<SearchControls>() {
      @Override
      public SearchControls answer() throws Throwable {
        SearchControls searchControls = createNiceMock(SearchControls.class);
        replay(searchControls);
        return searchControls;
      }
    }).once();

    replayAll();

    handler.open(kc, DEFAULT_REALM, kerberosEnvMap);
    handler.createPrincipal("nn/c6501.ambari.apache.org", "secret", true);
    handler.close();

    Attributes attributes = capturedAttributes.getValue();
    String[] objectClasses = new String[]{"top", "person", "organizationalPerson", "user"};

    Assert.assertNotNull(attributes);
    Assert.assertEquals(8, attributes.size());

    Assert.assertNotNull(attributes.get("objectClass"));
    Assert.assertEquals(objectClasses.length, attributes.get("objectClass").size());
    for (int i = 0; i < objectClasses.length; i++) {
      Assert.assertEquals(objectClasses[i], attributes.get("objectClass").get(i));
    }

    Assert.assertNotNull(attributes.get("cn"));
    Assert.assertEquals("nn-995e1580db28198e7fda1417ab5d894c877937d2", attributes.get("cn").get());

    Assert.assertNotNull(attributes.get("servicePrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org", attributes.get("servicePrincipalName").get());

    Assert.assertNotNull(attributes.get("userPrincipalName"));
    Assert.assertEquals("nn/c6501.ambari.apache.org@HDP01.LOCAL", attributes.get("userPrincipalName").get());

    Assert.assertNotNull(attributes.get("sAMAccountName"));
    Assert.assertTrue(attributes.get("sAMAccountName").get().toString().length() <= 20);
    Assert.assertEquals("nn-995e1580db28198e7", attributes.get("sAMAccountName").get());

    Assert.assertNotNull(attributes.get("unicodePwd"));
    Assert.assertEquals("\"secret\"", new String((byte[]) attributes.get("unicodePwd").get(), Charset.forName("UTF-16LE")));

    Assert.assertNotNull(attributes.get("accountExpires"));
    Assert.assertEquals("0", attributes.get("accountExpires").get());

    Assert.assertNotNull(attributes.get("userAccountControl"));
    Assert.assertEquals("66048", attributes.get("userAccountControl").get());

  }

  /**
   * Implementation to illustrate the use of operations on this class
   *
   * @throws Throwable
   */
  @Test
  @Ignore
  public void testLive() throws Throwable {
    ADKerberosOperationHandler handler = new ADKerberosOperationHandler();
    String principal = System.getProperty("principal");
    String password = System.getProperty("password");
    String realm = System.getProperty("realm");
    String ldapUrl = System.getProperty("ldap_url");
    String containerDN = System.getProperty("container_dn");

    if (principal == null) {
      principal = DEFAULT_ADMIN_PRINCIPAL;
    }

    if (password == null) {
      password = DEFAULT_ADMIN_PASSWORD;
    }

    if (realm == null) {
      realm = DEFAULT_REALM;
    }

    if (ldapUrl == null) {
      ldapUrl = DEFAULT_LDAP_URL;
    }

    if (containerDN == null) {
      containerDN = DEFAULT_PRINCIPAL_CONTAINER_DN;
    }

    KerberosCredential credentials = new KerberosCredential(principal, password, null);
    Map<String, String> kerberosEnvMap = new HashMap<String, String>();

    kerberosEnvMap.put(ADKerberosOperationHandler.KERBEROS_ENV_LDAP_URL, ldapUrl);
    kerberosEnvMap.put(ADKerberosOperationHandler.KERBEROS_ENV_PRINCIPAL_CONTAINER_DN, containerDN);

    handler.open(credentials, realm, kerberosEnvMap);

    System.out.println("Test Admin Credentials: " + handler.testAdministratorCredentials());
    // does the principal already exist?
    System.out.println("Principal exists: " + handler.principalExists("nn/c1508.ambari.apache.org"));

    handler.close();

    handler.open(credentials, realm, kerberosEnvMap);

    String evaluatedPrincipal;

    evaluatedPrincipal = "nn/c6501.ambari.apache.org@" + DEFAULT_REALM;
    if (handler.principalExists(evaluatedPrincipal)) {
      handler.setPrincipalPassword(evaluatedPrincipal, "some password");
    } else {
      handler.createPrincipal(evaluatedPrincipal, "some password", true);
    }

    evaluatedPrincipal = "hdfs@" + DEFAULT_REALM;
    if (handler.principalExists(evaluatedPrincipal)) {
      handler.setPrincipalPassword(evaluatedPrincipal, "some password");
    } else {
      handler.createPrincipal(evaluatedPrincipal, "some password", true);
    }

    kerberosEnvMap.put(ADKerberosOperationHandler.KERBEROS_ENV_CREATE_ATTRIBUTES_TEMPLATE,
        "#set( $user = \"${principal_primary}-${principal_digest}\" )" +
            "{" +
            "  \"objectClass\": [" +
            "    \"top\"," +
            "    \"person\"," +
            "    \"organizationalPerson\"," +
            "    \"user\"" +
            "  ]," +
            "  \"cn\": \"$user\"," +
            "  \"sAMAccountName\": \"$user.substring(0,20)\"," +
            "  #if( $is_service )" +
            "  \"servicePrincipalName\": \"$principal_name\"," +
            "  #end" +
            "  \"userPrincipalName\": \"$normalized_principal\"," +
            "  \"unicodePwd\": \"$password\"," +
            "  \"accountExpires\": \"0\"," +
            "  \"userAccountControl\": \"66048\"" +
            "}"
    );

    handler.close();

    handler.open(credentials, realm, kerberosEnvMap);

    // remove the principal
    handler.removePrincipal("abcdefg");
    handler.removePrincipal("abcdefg/c1509.ambari.apache.org@" + DEFAULT_REALM);

    handler.createPrincipal("abcdefg/c1509.ambari.apache.org@" + DEFAULT_REALM, "some password", true);
    handler.createPrincipal("abcdefg@" + DEFAULT_REALM, "some password", false);

    //update the password
    handler.setPrincipalPassword("abcdefg/c1509.ambari.apache.org@" + DEFAULT_REALM, "some password");

    handler.close();
  }
}