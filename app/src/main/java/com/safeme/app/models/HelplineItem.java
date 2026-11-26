package com.safeme.app.models;

public class HelplineItem {
    private final String title;
    private final String number;
    private final String description;
    private final String availability;

    public HelplineItem(String title, String number, String description, String availability) {
        this.title = title;
        this.number = number;
        this.description = description;
        this.availability = availability;
    }

    public String getTitle() { return title; }
    public String getNumber() { return number; }
    public String getDescription() { return description; }
    public String getAvailability() { return availability; }
}
