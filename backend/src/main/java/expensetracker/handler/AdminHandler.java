package expensetracker.handler;

import expensetracker.dao.LoginEventDao;
import expensetracker.dao.UserDao;
import expensetracker.model.LoginEvent;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminHandler {
    private final UserDao userDao = new UserDao();
    private final LoginEventDao loginEventDao = new LoginEventDao();

    public void users(Context ctx) {
        User admin = ctx.attribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            ctx.status(403).json(Map.of("error", "Forbidden"));
            return;
        }
        List<User> list = userDao.findAll();
        ctx.json(list.stream().map(this::userResponse).collect(Collectors.toList()));
    }

    public void logins(Context ctx) {
        User admin = ctx.attribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            ctx.status(403).json(Map.of("error", "Forbidden"));
            return;
        }
        int limit = Math.min(Math.max(1, parseInt(ctx.queryParam("limit"), 50)), 100);
        int offset = Math.max(0, parseInt(ctx.queryParam("offset"), 0));
        List<LoginEvent> list = loginEventDao.findAllWithUserEmail(limit, offset);
        Map<String, Object> result = new HashMap<>();
        result.put("items", list.stream().map(this::loginEventResponse).collect(Collectors.toList()));
        result.put("limit", limit);
        result.put("offset", offset);
        ctx.json(result);
    }

    private int parseInt(String s, int defaultVal) {
        if (s == null || s.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private Map<String, Object> userResponse(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("role", u.getRole());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> loginEventResponse(LoginEvent e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("userEmail", e.getUserEmail());
        m.put("loggedInAt", e.getLoggedInAt() != null ? e.getLoggedInAt().toString() : null);
        m.put("ipAddress", e.getIpAddress());
        return m;
    }
}
