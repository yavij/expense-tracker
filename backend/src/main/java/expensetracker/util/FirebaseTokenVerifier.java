package expensetracker.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies Firebase Authentication ID tokens by fetching Google's public certificates
 * and validating the JWT signature, issuer, and audience.
 */
public class FirebaseTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenVerifier.class);
    private static final String CERTS_URL =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

    private static volatile Map<String, PublicKey> cachedKeys = new ConcurrentHashMap<>();
    private static volatile long cacheExpiry = 0;

    public static class FirebaseUser {
        private final String uid;
        private final String phone;

        public FirebaseUser(String uid, String phone) {
            this.uid = uid;
            this.phone = phone;
        }

        public String getUid() { return uid; }
        public String getPhone() { return phone; }
    }

    public static FirebaseUser verify(String idToken, String projectId) throws Exception {
        if (idToken == null || idToken.isBlank())
            throw new IllegalArgumentException("idToken is required");
        if (projectId == null || projectId.isBlank())
            throw new IllegalArgumentException("FIREBASE_PROJECT_ID is required");

        refreshKeysIfNeeded();

        String[] parts = idToken.split("\\.");
        if (parts.length < 2) throw new Exception("Malformed token");

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        JsonObject header = new Gson().fromJson(headerJson, JsonObject.class);
        String kid = header.has("kid") ? header.get("kid").getAsString() : null;

        if (kid == null || !cachedKeys.containsKey(kid)) {
            refreshKeys();
            if (kid == null || !cachedKeys.containsKey(kid))
                throw new Exception("Unknown signing key in Firebase token");
        }

        PublicKey key = cachedKeys.get(kid);

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

        String expectedIssuer = "https://securetoken.google.com/" + projectId;
        if (!expectedIssuer.equals(claims.getIssuer()))
            throw new Exception("Invalid issuer: expected " + expectedIssuer);

        if (claims.getAudience() == null || !claims.getAudience().contains(projectId))
            throw new Exception("Invalid audience");

        String uid = claims.getSubject();
        if (uid == null || uid.isBlank())
            throw new Exception("Token missing subject (uid)");

        Object phoneClaim = claims.get("phone_number");
        String phone = phoneClaim != null ? phoneClaim.toString() : null;

        return new FirebaseUser(uid, phone);
    }

    private static void refreshKeysIfNeeded() throws Exception {
        if (System.currentTimeMillis() < cacheExpiry && !cachedKeys.isEmpty()) return;
        refreshKeys();
    }

    private static synchronized void refreshKeys() throws Exception {
        if (System.currentTimeMillis() < cacheExpiry && !cachedKeys.isEmpty()) return;

        URL url = new URL(CERTS_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        long maxAge = 3600;
        String cc = conn.getHeaderField("Cache-Control");
        if (cc != null && cc.contains("max-age=")) {
            try {
                String val = cc.split("max-age=")[1].split("[,\\s]")[0];
                maxAge = Long.parseLong(val);
            } catch (Exception ignored) {}
        }
        cacheExpiry = System.currentTimeMillis() + (maxAge * 1000);

        JsonObject certs = new Gson().fromJson(
                new InputStreamReader(conn.getInputStream()), JsonObject.class);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Map<String, PublicKey> newKeys = new ConcurrentHashMap<>();

        for (String keyId : certs.keySet()) {
            String pem = certs.get(keyId).getAsString();
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(pem.getBytes()));
            newKeys.put(keyId, cert.getPublicKey());
        }

        cachedKeys = newKeys;
        log.info("Refreshed {} Firebase public keys", newKeys.size());
    }
}
