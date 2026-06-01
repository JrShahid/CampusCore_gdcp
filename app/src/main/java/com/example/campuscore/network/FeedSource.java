package com.example.campuscore.network;

public class FeedSource {
    private final String name;
    private final String url;
    private final String category;

    public FeedSource(String name, String url, String category) {
        this.name = name;
        this.url = url;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCategory() {
        return category;
    }
}
