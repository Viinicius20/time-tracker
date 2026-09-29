package com.timetracker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.*;
import java.awt.image.BufferedImage;
import org.springframework.stereotype.Component;

@Component
public class Notifier {
    private TrayIcon icon;

    @PostConstruct
    void init() throws AWTException {
        if (!SystemTray.isSupported()) return;
        icon = new TrayIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "Time Tracker");
        SystemTray.getSystemTray().add(icon);
    }

    public void notify(String title, String message) {
        if (icon != null) icon.displayMessage(title, message, TrayIcon.MessageType.WARNING);
    }

    @PreDestroy
    void close() {
        if (icon != null) SystemTray.getSystemTray().remove(icon);
    }
}