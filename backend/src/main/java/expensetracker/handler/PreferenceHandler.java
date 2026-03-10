package expensetracker.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import expensetracker.dao.UserPreferenceDao;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.lang.reflect.Type;
import java.util.Map;

public class PreferenceHandler {

    private final UserPreferenceDao prefDao = new UserPreferenceDao();
    private final Gson gson = new Gson();

    /** GET /api/preferences – returns all preferences for the authenticated user. */
    public void get(Context ctx) {
        try {
            User user = ctx.attribute("user");
            Map<String, String> prefs = prefDao.findByUserId(user.getId());
            ctx.json(prefs);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Failed to load preferences"));
        }
    }

    /** PUT /api/preferences – bulk upsert preferences from JSON body: { "theme": "dark", "layout": "sidebar", ... } */
    public void update(Context ctx) {
        try {
            User user = ctx.attribute("user");
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> prefs = gson.fromJson(ctx.body(), type);

            if (prefs == null || prefs.isEmpty()) {
                ctx.status(400).json(Map.of("error", "No preferences provided"));
                return;
            }

            // Validate known keys
            for (String key : prefs.keySet()) {
                if (key.length() > 50 || prefs.get(key).length() > 500) {
                    ctx.status(400).json(Map.of("error", "Preference key/value too long"));
                    return;
                }
            }

            prefDao.upsertAll(user.getId(), prefs);
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Failed to save preferences"));
        }
    }
}
