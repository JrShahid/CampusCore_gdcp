package com.example.campuscore.models;

public class FeedItemModel {
    private final String title;
    private final String description;
    private final String articleUrl;
    private final String imageUrl;
    private final String sourceName;
    private final String category;
    private final String publishedDate;

    public FeedItemModel(String title, String description, String articleUrl, String imageUrl,
                         String sourceName, String category, String publishedDate) {
        this.title = safe(title);
        this.description = safe(description);
        this.articleUrl = safe(articleUrl);
        this.imageUrl = safe(imageUrl);
        this.sourceName = safe(sourceName);
        this.category = safe(category);
        this.publishedDate = safe(publishedDate);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getCategory() {
        return category;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String stableId() {
        return articleUrl.isEmpty() ? title + sourceName + publishedDate : articleUrl;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
