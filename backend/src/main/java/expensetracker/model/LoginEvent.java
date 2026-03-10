package expensetracker.model;

import java.time.Instant;

public class LoginEvent {
    private String id;
    private String userId;
    private Instant loggedInAt;
    private String ipAddress;
    private String userEmail; // for admin list (join)

    public LoginEvent() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Instant getLoggedInAt() { return loggedInAt; }
    public void setLoggedInAt(Instant loggedInAt) { this.loggedInAt = loggedInAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
