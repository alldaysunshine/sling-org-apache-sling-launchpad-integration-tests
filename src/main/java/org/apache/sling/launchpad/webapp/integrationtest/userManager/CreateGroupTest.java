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

/**
 * Tests for the 'createGroup' Sling Post Operation
 */
public class CreateGroupTest extends UserManagerTestUtil {
    private static Random random = new Random(System.currentTimeMillis());

	String testUserId = null;
	String testGroupId = null;
	String testGroupId2 = null;

	@Override
	public void tearDown() throws Exception {
		if (testGroupId != null) {
			//remove the test group if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testGroupId2 != null) {
			//remove the test group if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId2 + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}

		super.tearDown();
	}

	public void testCreateGroup() throws IOException, JsonException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		testGroupId = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId));
		postParams.add(new NameValuePair("marker", testGroupId));
		assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the group profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".json";
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertEquals(testGroupId, jsonObj.getString("marker"));
	}

	public void testNotAuthorizedCreateGroup() throws IOException, JsonException {
		testUserId = createTestUser();
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String testGroupId2 = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId2));
		postParams.add(new NameValuePair("marker", testGroupId2));
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testAuthorizedCreateGroup() throws IOException, JsonException {
		testUserId = createTestUser();
		grantUserManagementRights(testUserId);
		
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		testGroupId = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId));
		postParams.add(new NameValuePair("marker", testGroupId));
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the group profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".json";
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertEquals(testGroupId, jsonObj.getString("marker"));
	}

	/**
	 * Test for SLING-7831
	 */
	public void testCreateGroupCustomPostResponse() throws IOException, JsonException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		testGroupId = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new NameValuePair(":responseType", "custom"));
		postParams.add(new NameValuePair(":name", testGroupId));
		postParams.add(new NameValuePair("marker", testGroupId));

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String content = getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_HTML, postParams, HttpServletResponse.SC_OK);
		assertEquals("Thanks!", content); //verify that the content matches the custom response
	}

	public void testCreateGroupMissingGroupId() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testCreateGroupAlreadyExists() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		testGroupId = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId));
		assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);

		//post the same info again, should fail
		assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testCreateGroupWithExtraProperties() throws IOException, JsonException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		testGroupId = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId));
		postParams.add(new NameValuePair("marker", testGroupId));
		postParams.add(new NameValuePair("displayName", "My Test Group"));
		postParams.add(new NameValuePair("url", "http://www.apache.org"));
		assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the group profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".json";
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertEquals(testGroupId, jsonObj.getString("marker"));
		assertEquals("My Test Group", jsonObj.getString("displayName"));
		assertEquals("http://www.apache.org", jsonObj.getString("url"));
	}
	
	
	/**
	 * Test for SLING-1677
	 */
	public void testCreateGroupResponseAsJSON() throws IOException, JsonException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.json";

		testGroupId = "testGroup" + random.nextInt();
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId));
		postParams.add(new NameValuePair("marker", testGroupId));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = getAuthenticatedPostContent(creds, postUrl, CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

		//make sure the json response can be parsed as a JSON object
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertNotNull(jsonObj);
	}	
}
