package aina.util;

import java.util.HashMap;

public class AppSession {
    private String userId;
    private HashMap<String, Object> sessionData;

    public AppSession(String userId) {
        this.userId = userId;
        this.sessionData = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public void addSessionData(String key, Object value) {
        sessionData.put(key, value);
    }
    public Object getSessionData(String key) {
        return sessionData.get(key);
    }
    public void removeSessionData(String key) {
        sessionData.remove(key);
    }
    public void clearSessionData() {
        sessionData.clear();
    }
}
