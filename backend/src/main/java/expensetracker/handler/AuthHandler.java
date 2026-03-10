package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.LoginEventDao;
import expensetracker.dao.UserDao;
import expensetracker.model.User;
import expensetracker.util.FirebaseTokenVerifier;
import expensetracker.util.GoogleTokenVerifier;
import expensetracker.util.JwtUtil;
import expensetracker.dto.GoogleLoginRequest;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class PhoneLoginRequest {
    public String phone;
    public String firebaseUid;
    public String idToken;
    public String otp;  // Dev mode: when DEV_PHONE_OTP env is set, accept otp instead of idToken
}

class UpdateProfileRequest {
    public String phoneNumber;
}

public class AuthHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private final UserDao userDao = new UserDao();
    private final LoginEventDao loginEventDao = new LoginEventDao();

    public Handler googleLogin = ctx -> {
        GoogleLoginRequest body = ctx.bodyAsClass(GoogleLoginRequest.class);
        String idToken = body != null ? body.getIdToken() : null;
        if (idToken == null || idToken.isBlank()) {
            ctx.status(400).json(Map.of("error", "idToken is required"));
            return;
        }
        GoogleTokenVerifier.GoogleUserInfo info = GoogleTokenVerifier.verify(idToken);
        if (info.getEmail() == null || info.getEmail().isBlank()) {
            ctx.status(401).json(Map.of("error", "Invalid token"));
            return;
        }

        User user = userDao.findByEmail(info.getEmail()).orElseGet(() -> {
            String role = "USER";
            if (userDao.count() == 0) role = "ADMIN";
            return userDao.insert(info.getEmail(), info.getName(), info.getPicture(), role);
        });

        String ip = ctx.header("X-Forwarded-For");
        if (ip == null) ip = ctx.ip();
        loginEventDao.insert(user.getId(), ip);

        String token = JwtUtil.createToken(user);
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userResponse(user));
        ctx.json(response);
    };

    public Handler me = ctx -> {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        ctx.json(userResponse(user));
    };

    public Handler phoneLogin = ctx -> {
        PhoneLoginRequest body = ctx.bodyAsClass(PhoneLoginRequest.class);
        if (body.phone == null || body.phone.isBlank()) {
            ctx.status(400).json(Map.of("error", "phone is required"));
            return;
        }

        String devOtp = System.getenv("DEV_PHONE_OTP");
        if (devOtp != null && !devOtp.isBlank() && body.otp != null && body.otp.equals(devOtp)) {
            // Dev OTP matches - allow login without Firebase
        } else if (body.idToken == null || body.idToken.isBlank()) {
            ctx.status(400).json(Map.of("error", "idToken is required (or set DEV_PHONE_OTP for dev mode)"));
            return;
        } else {
            String projectId = System.getenv("FIREBASE_PROJECT_ID");
            if (projectId != null && !projectId.isBlank()) {
                try {
                    FirebaseTokenVerifier.FirebaseUser fbUser =
                            FirebaseTokenVerifier.verify(body.idToken, projectId);
                    if (fbUser.getPhone() != null && !fbUser.getPhone().equals(body.phone)) {
                        ctx.status(400).json(Map.of("error", "Phone number mismatch"));
                        return;
                    }
                } catch (Exception e) {
                    log.warn("Firebase token verification failed: {}", e.getMessage());
                    ctx.status(401).json(Map.of("error", "Invalid Firebase token"));
                    return;
                }
            } else {
                log.debug("FIREBASE_PROJECT_ID not set, skipping server-side token verification");
            }
        }

        User user = userDao.findByPhone(body.phone).orElseGet(() -> {
            String role = "USER";
            if (userDao.count() == 0) role = "ADMIN";
            String email = "phone_" + body.phone.replaceAll("[^0-9]", "") + "@phone.local";
            return userDao.insertWithPhone(email, body.phone, null, role, body.phone);
        });

        String ip = ctx.header("X-Forwarded-For");
        if (ip == null) ip = ctx.ip();
        loginEventDao.insert(user.getId(), ip);

        String token = JwtUtil.createToken(user);
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userResponse(user));
        ctx.json(response);
    };

    public Handler updateProfile = ctx -> {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }

        UpdateProfileRequest body = ctx.bodyAsClass(UpdateProfileRequest.class);
        if (body.phoneNumber != null && !body.phoneNumber.isBlank()) {
            userDao.updatePhone(user.getId(), body.phoneNumber);
            user.setPhoneNumber(body.phoneNumber);
        }

        ctx.json(userResponse(user));
    };

    private Map<String, Object> userResponse(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("pictureUrl", u.getPictureUrl());
        m.put("phoneNumber", u.getPhoneNumber());
        m.put("role", u.getRole());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }
}
