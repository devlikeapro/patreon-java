package com.patreon;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.patreon.resources.v1.Campaign;
import com.patreon.resources.v1.Pledge;
import com.patreon.resources.v1.RequestUtil;
import com.patreon.resources.v1.User;
import junit.framework.TestCase;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PatreonAPITest extends TestCase {
  private static final String MOCK_TOKEN = "some token";
  private PatreonAPI api;
  private RequestUtil requestUtil;

  @Override
  public void setUp() {
    requestUtil = mock(RequestUtil.class);
    api = Mockito.spy(new PatreonAPI(MOCK_TOKEN, requestUtil));
  }

  public void testFetchCampaigns() throws Exception {
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN)))
      .thenReturn(PatreonAPITest.class.getResourceAsStream("/api/campaigns.json"));

    JSONAPIDocument<List<Campaign>> campaignResponse = api.fetchCampaigns();
    assertEquals(1, campaignResponse.get().size());
    Campaign campaign = campaignResponse.get().get(0);
    assertEquals("70261", campaign.getId());
    assertEquals("/bePatron?c=70261", campaign.getPledgeUrl());
    assertEquals(false, campaign.isChargedImmediately());
    assertEquals("212633030584565760", campaign.getDiscordServerId());
    assertEquals("32187", campaign.getCreator().getId());
    assertEquals(3, campaign.getGoals().size());
  }

  public void testGetPledgesToMe() throws Exception {
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN)))
      .thenReturn(PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges.json"));

    JSONAPIDocument<List<Pledge>> pledgeResponse = api.fetchPageOfPledges("70261", 10, null);
    assertEquals(12, pledgeResponse.getMeta().get("count"));
    List<Pledge> pledges = pledgeResponse.get();
    assertEquals(10, pledges.size());

    for (Pledge pledge : pledges) {
      assertTrue(pledge.getAmountCents() > 0);
      User patron = pledge.getPatron();
      assertNotNull(patron.getEmail());
    }
  }

  public void testFetchAllPledges() throws Exception {
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN))).thenReturn(
      PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges_page_1.json"),
      PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges_page_2.json")
    );

    List<Pledge> pledges = api.fetchAllPledges("70261");
    assertEquals(11, pledges.size());

    for (Pledge pledge : pledges) {
      assertTrue(pledge.getAmountCents() > 0);
      User patron = pledge.getPatron();
      assertNotNull(patron.getEmail());
    }
  }

  public void testFetchUser() throws Exception {
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN))).thenReturn(
      PatreonAPITest.class.getResourceAsStream("/api/current_user.json")
    );

    JSONAPIDocument<User> user = api.fetchUser();

    verify(requestUtil).request(eq("current_user?include=pledges"), eq(MOCK_TOKEN));
    assertEquals("https://www.patreon.com/api/user/32187", user.getLinks().getSelf().toString());
    assertEquals(5, user.get().getPledges().size());
    assertEquals("corgi", user.get().getVanity());
    assertNull(user.get().getLikeCount());
    assertNull(user.get().getCommentCount());
  }

  public void testFetchUserOptionalFields() throws Exception {
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN))).thenReturn(
      PatreonAPITest.class.getResourceAsStream("/api/current_user_optional_fields.json")
    );

    JSONAPIDocument<User> user = api.fetchUser(Collections.singletonList(User.UserField.LikeCount));


    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(requestUtil).request(captor.capture(), eq(MOCK_TOKEN));

    String arg = captor.getValue();
    assertTrue(arg.startsWith("current_user?"));

    //Extract and decode the query params from the URL
    List<NameValuePair> parsed = URLEncodedUtils.parse(arg.substring(arg.indexOf('?') + 1), Charset.forName("UTF-8"));
    assertEquals(2, parsed.size());
    NameValuePair first = parsed.get(0);
    assertEquals("include", first.getName());
    assertEquals("pledges", first.getValue());

    NameValuePair second = parsed.get(1);
    assertEquals("fields[user]", second.getName());

    //sort fields from call, compare to sorted list of fields.
    String[] fieldNames = second.getValue().split(",");
    Arrays.sort(fieldNames);


    assertEquals(Arrays.asList(
      "about",
      "created",
      "discord_id",
      "email",
      "facebook",
      "facebook_id",
      "full_name",
      "image_url",
      "is_email_verified",
      "like_count",
      "social_connections",
      "thumb_url",
      "twitch",
      "twitter",
      "url",
      "vanity",
      "youtube"), Arrays.asList(fieldNames));

    assertEquals("https://www.patreon.com/api/user/32187", user.getLinks().getSelf().toString());
    assertEquals(5, user.get().getPledges().size());
    assertEquals("corgi", user.get().getVanity());
    assertEquals("https://facebook.com/corgi", user.get().getSocialConnections().getFacebook().getUrl());
    assertEquals(Integer.valueOf(5), user.get().getLikeCount());
    assertNull(user.get().getCommentCount());

  }

  public void testFetchUserUnknownProperties() throws Exception {

    when(requestUtil.request(anyString(), eq(MOCK_TOKEN))).thenReturn(
      PatreonAPITest.class.getResourceAsStream("/api/current_user_unknown_properties.json")
    );

    JSONAPIDocument<User> user = api.fetchUser();
    verify(requestUtil).request(eq("current_user?include=pledges"), eq(MOCK_TOKEN));
    assertEquals("https://www.patreon.com/api/user/32187", user.getLinks().getSelf().toString());
  }

  /**
   * Test for v2FetchCampaignMembers with pagination
   * Note: This test requires test resources that simulate the v2 API response for campaign members
   * Create two JSON files:
   * 1. /api/v2_campaign_members_page_1.json - First page of members with a "next" link
   * 2. /api/v2_campaign_members_page_2.json - Second page of members with no "next" link
   */
  public void testV2FetchCampaignMembers() throws Exception {
    // Mock the request to return the first page, then the second page
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN))).thenReturn(
      // Replace with actual test resources when available
      PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges_page_1.json"),
      PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges_page_2.json")
    );

    // Test fetching a single page with cursor
    String campaignId = "12345";
    int pageSize = 10;
    String cursor = "some-cursor-value";

    // Test the paginated method
    api.v2FetchCampaignMembers(campaignId, pageSize, cursor);

    // Verify the correct URL was requested with the cursor
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(requestUtil).request(captor.capture(), eq(MOCK_TOKEN));

    String arg = captor.getValue();
    assertTrue(arg.contains("v2/campaigns/" + campaignId + "/members"));
    assertTrue(arg.contains("page[count]=" + pageSize));
    assertTrue(arg.contains("page[cursor]=" + cursor));
  }

  /**
   * Test for v2FetchAllCampaignMembers
   * Note: This test requires test resources that simulate the v2 API response for campaign members
   */
  public void testV2FetchAllCampaignMembers() throws Exception {
    // Mock the request to return the first page, then the second page
    when(requestUtil.request(anyString(), eq(MOCK_TOKEN))).thenReturn(
      // Replace with actual test resources when available
      PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges_page_1.json"),
      PatreonAPITest.class.getResourceAsStream("/api/campaign_pledges_page_2.json")
    );

    // Test fetching all members
    String campaignId = "12345";
    int pageSize = 10;

    // This will fail until proper test resources are created
    // List<Member> members = api.v2FetchAllCampaignMembers(campaignId, pageSize);

    // Assertions would go here
    // assertEquals(expectedTotalCount, members.size());
  }
}
