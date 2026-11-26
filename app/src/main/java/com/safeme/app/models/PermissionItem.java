package com.safeme.app.models;

public class PermissionItem {
    public static final int TYPE_LOCATION = 1;
    public static final int TYPE_NOTIFICATIONS = 2;
    public static final int TYPE_CAMERA = 3;
    public static final int TYPE_MICROPHONE = 4;
    public static final int TYPE_FOREGROUND_LOCATION = 5;
    public static final int TYPE_BACKGROUND_LOCATION = 6;
    public static final int TYPE_MEDIA_PICKER = 7;

    private final int type;
    private final String title;
    private final String reason;
    private final boolean granted;
    private final boolean systemHandledWithoutPrompt;

    public PermissionItem(int type, String title, String reason, boolean granted, boolean systemHandledWithoutPrompt) {
        this.type = type;
        this.title = title;
        this.reason = reason;
        this.granted = granted;
        this.systemHandledWithoutPrompt = systemHandledWithoutPrompt;
    }

    public int getType() { return type; }
    public String getTitle() { return title; }
    public String getReason() { return reason; }
    public boolean isGranted() { return granted; }
    public boolean isSystemHandledWithoutPrompt() { return systemHandledWithoutPrompt; }
}
