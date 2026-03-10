package expensetracker.model;

public class UserPreference {
    private String userId;
    private String prefKey;
    private String prefValue;

    public UserPreference() {}

    public UserPreference(String userId, String prefKey, String prefValue) {
        this.userId = userId;
        this.prefKey = prefKey;
        this.prefValue = prefValue;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPrefKey() { return prefKey; }
    public void setPrefKey(String prefKey) { this.prefKey = prefKey; }

    public String getPrefValue() { return prefValue; }
    public void setPrefValue(String prefValue) { this.prefValue = prefValue; }
}
