/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.userManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.launchpad.webapp.integrationtest.util.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the 'createUser' Sling Post Operation
 */
public class CreateUserTest {
    private static Random random = new Random(System.currentTimeMillis());
    private String testUserId;
    private String testUserId2;
    
    private final UserManagerTestUtil H = new UserManagerTestUtil(); 

    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
	@After
	public void cleanup() throws Exception {
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			H.assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testUserId2 != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId2 + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			H.assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		H.tearDown();
	}

	/*
		<form action="/system/userManager/user.create.html" method="POST">
		   <div>Name: <input type="text" name=":name" value="testUser" /></div>
		   <div>Password: <input type="text" name="pwd" value="testUser" /></div>
		   <div>Password Confirm: <input type="text" name="pwdConfirm" value="testUser" /></div>
		   <input type="submit" value="Submit" />
		</form>
	 */
	@Test 
	public void testCreateUser() throws IOException, JsonException {
	    testUserId = "testUser" + random.nextInt() + System.currentTimeMillis();
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";
		final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("marker", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		final Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		{
	        // fetch the user profile json to verify the settings
	        final String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
	        final String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
	        assertNotNull(json);
	        final JsonObject jsonObj = JsonUtil.parseObject(json);
	        assertEquals(testUserId, jsonObj.getString("marker"));
	        assertFalse(jsonObj.containsKey(":name"));
	        assertFalse(jsonObj.containsKey("pwd"));
	        assertFalse(jsonObj.containsKey("pwdConfirm"));
		}
		
        {
            // fetch the session info to verify that the user can log in
            final Credentials newUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");
            final String getUrl = HttpTest.HTTP_BASE_URL + "/system/sling/info.sessionInfo.json";
            final String json = H.getAuthenticatedContent(newUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
            assertNotNull(json);
            final JsonObject jsonObj = JsonUtil.parseObject(json);
            assertEquals(testUserId, jsonObj.getString("userID"));
        }
	}

	@Test 
	public void testNotAuthorizedCreateUser() throws IOException, JsonException {
		testUserId2 = H.createTestUser();
		
	    String testUserId3 = "testUser" + random.nextInt() + System.currentTimeMillis();
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";
		final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId3));
		postParams.add(new NameValuePair("marker", testUserId3));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		final Credentials creds = new UsernamePasswordCredentials(testUserId2, "testPwd");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	@Test 
	public void testAuthorizedCreateUser() throws IOException, JsonException {
		testUserId2 = H.createTestUser();
		H.grantUserManagementRights(testUserId2);
		
	    testUserId = "testUser" + random.nextInt() + System.currentTimeMillis();
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";
		final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("marker", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		final Credentials creds = new UsernamePasswordCredentials(testUserId2, "testPwd");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		{
	        // fetch the user profile json to verify the settings
	        final String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
	        final String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
	        assertNotNull(json);
	        final JsonObject jsonObj = JsonUtil.parseObject(json);
	        assertEquals(testUserId, jsonObj.getString("marker"));
	        assertFalse(jsonObj.containsKey(":name"));
	        assertFalse(jsonObj.containsKey("pwd"));
	        assertFalse(jsonObj.containsKey("pwdConfirm"));
		}
		
        {
            // fetch the session info to verify that the user can log in
            final Credentials newUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");
            final String getUrl = HttpTest.HTTP_BASE_URL + "/system/sling/info.sessionInfo.json";
            final String json = H.getAuthenticatedContent(newUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
            assertNotNull(json);
            final JsonObject jsonObj = JsonUtil.parseObject(json);
            assertEquals(testUserId, jsonObj.getString("userID"));
        }
	}

	@Test 
	public void testCreateUserMissingUserId() throws IOException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	@Test 
	public void testCreateUserMissingPwd() throws IOException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

        String userId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", userId));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	@Test 
	public void testCreateUserWrongConfirmPwd() throws IOException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

        String userId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", userId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd2"));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	@Test 
	public void testCreateUserUserAlreadyExists() throws IOException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

		testUserId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		//post the same info again, should fail
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	/*
	<form action="/system/userManager/user.create.html" method="POST">
	   <div>Name: <input type="text" name=":name" value="testUser" /></div>
	   <div>Password: <input type="text" name="pwd" value="testUser" /></div>
	   <div>Password Confirm: <input type="text" name="pwdConfirm" value="testUser" /></div>
	   <div>Extra Property #1: <input type="text" name="displayName" value="My Test User" /></div>
	   <div>Extra Property #2: <input type="text" name="url" value="http://www.apache.org" /></div>
	   <input type="submit" value="Submit" />
	</form>
	*/
	@Test 
	public void testCreateUserWithExtraProperties() throws IOException, JsonException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

		testUserId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("marker", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		postParams.add(new NameValuePair("displayName", "My Test User"));
		postParams.add(new NameValuePair("url", "http://www.apache.org"));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the user profile json to verify the settings
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertEquals(testUserId, jsonObj.getString("marker"));
		assertEquals("My Test User", jsonObj.getString("displayName"));
		assertEquals("http://www.apache.org", jsonObj.getString("url"));
		assertFalse(jsonObj.containsKey(":name"));
		assertFalse(jsonObj.containsKey("pwd"));
		assertFalse(jsonObj.containsKey("pwdConfirm"));
	}

	/**
	 * Test for SLING-1642 to verify that user self-registration by the anonymous
	 * user is not allowed by default.
	 */
	@Test 
	public void testAnonymousSelfRegistrationDisabled() throws IOException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

		String userId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", userId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		//user create without logging in as a privileged user should return a 500 error
		H.getHttpClient().getState().clearCredentials();
		H.assertPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}
	
	
	/**
	 * Test for SLING-1677
	 */
	@Test 
	public void testCreateUserResponseAsJSON() throws IOException, JsonException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.json";

		testUserId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("marker", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

		//make sure the json response can be parsed as a JSON object
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertNotNull(jsonObj);
	}

	/**
	 * Test for SLING-7831
	 */
	@Test 
	public void testCreateUserCustomPostResponse() throws IOException, JsonException {
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user.create.html";

		testUserId = "testUser" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new NameValuePair(":responseType", "custom"));
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String content = H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_HTML, postParams, HttpServletResponse.SC_OK);
		assertEquals("Thanks!", content); //verify that the content matches the custom response
	}
}
