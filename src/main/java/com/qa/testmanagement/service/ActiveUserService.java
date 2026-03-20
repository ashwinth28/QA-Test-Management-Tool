package com.qa.testmanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveUserService {

    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();

    public void userLoggedIn(String username, String sessionId) {
        activeSessions.put(sessionId, username);
        sessionToUsername.put(username, sessionId);
    }

    public void userLoggedOut(String sessionId) {
        String username = activeSessions.remove(sessionId);
        if (username != null) {
            sessionToUsername.remove(username);
        }
    }

    public int getActiveUserCount() {
        return activeSessions.size();
    }

    public String getCurrentUsername() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession(false);
            if (session != null) {
                return (String) session.getAttribute("username");
            }
        }
        return null;
    }

    public boolean isUserActive(String username) {
        return sessionToUsername.containsKey(username);
    }
}