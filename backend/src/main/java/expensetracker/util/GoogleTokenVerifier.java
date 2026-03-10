package expensetracker.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleTokenVerifier {
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final Gson gson = new Gson();

    public static GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }
        try {
            String url = TOKENINFO_URL + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            try (InputStreamReader reader = new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                if (json == null || !json.has("email")) {
                    throw new IllegalArgumentException("Invalid token response");
                }
                GoogleUserInfo info = new GoogleUserInfo();
                info.setEmail(json.has("email") ? json.get("email").getAsString() : null);
                info.setName(json.has("name") ? json.get("name").getAsString() : null);
                info.setPicture(json.has("picture") ? json.get("picture").getAsString() : null);
                return info;
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            throw new RuntimeException("Failed to verify Google token", e);
        }
    }

    public static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPicture() { return picture; }
        public void setPicture(String picture) { this.picture = picture; }
    }
}
