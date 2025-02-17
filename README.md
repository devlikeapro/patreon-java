# patreon-java
Interact with the Patreon API via OAuth.

> **It's a fork which support Patreon API v2**!!

Step 0. Install the library
---

Add the following to your `build.gradle.kts` file:

```kotlin
repositories {
    // The fork is not published to maven central yet, so you need to add the jitpack repository
    maven("https://jitpack.io")
}

dependencies {
    // Or specify a specific commit hash instead of master-SNAPSHOT
    implementation("com.github.devlikeapro:patreon-java:master-SNAPSHOT")
}
```


Step 1. Get your client_id and client_secret
---
Visit the [Patreon Platform Page](https://www.patreon.com/platform)
while logged in as a Patreon creator to register your client.

This will provide you with a `client_id` and a `client_secret`.

Step 2. Use this library
---

## For the Log In with Patreon flow

```java
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.patreon.PatreonAPI;
import com.patreon.PatreonOAuth;
import com.patreon.PatreonOAuth;
import com.patreon.resources.v1.User;
import com.patreon.resources.v1.Pledge;

    ...

String clientId = null; // Get this when you set up your client
String clientSecret = null; // Get this when you set up your client
String redirectUri = null; // Provide this to set up your client

String code = null; // Get this from the query parameter `code`

PatreonOAuth oauthClient = new PatreonOAuth(clientId, clientSecret, redirectUri);
PatreonOAuth.TokensResponse tokens = oauthClient.getTokens(code);
//Store the refresh TokensResponse in your data store
String accessToken = tokens.getAccessToken();

PatreonAPI apiClient = new PatreonAPI(accessToken);
JSONAPIDocument<User> userResponse = apiClient.fetchUser();
User user = userResponse.get();
Log.

i(user.getFullName());
List<Pledge> pledges = user.getPledges()
if(pledges !=null&&pledges.

size() >0){
Pledge pledge = pledges.get(0);
    Log.

i(pledge.getAmountCents());
        }
// You should save the user's PatreonOAuth.TokensResponse in your database
// (for refreshing their Patreon data whenever you like),
// along with any relevant user info or pledge info you want to store.
```
