package com.example.campuscore.repositories;

import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;

import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.FeedItemModel;
import com.example.campuscore.network.FeedSource;
import com.example.campuscore.network.UpdatesFeedProvider;
import com.example.campuscore.utils.UpdatesConstants;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CampusCoreUpdatesRepository {
    private static final String TAG = "CampusCoreUpdatesRepo";

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    public CampusCoreUpdatesRepository() {
        httpClient = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();
    }

    public void fetchUpdates(String department, FirestoreCallback<List<FeedItemModel>> callback) {
        executorService.execute(() -> {
            List<FeedItemModel> mergedItems = new ArrayList<>();
            List<FeedSource> sources = UpdatesFeedProvider.sourcesForDepartment(department);
            for (FeedSource source : sources) {
                try {
                    String xml = fetchFeedXml(source);
                    mergedItems.addAll(parseFeed(xml, source));
                } catch (Exception error) {
                    Log.w(TAG, "Unable to fetch RSS source: " + source.getName(), error);
                }
            }

            List<FeedItemModel> uniqueItems = uniqueLatestFirst(mergedItems);
            if (uniqueItems.isEmpty()) {
                mainHandler.post(() -> callback.onError("Unable to fetch updates"));
                return;
            }
            mainHandler.post(() -> callback.onSuccess(uniqueItems));
        });
    }

    public List<FeedItemModel> filterUpdates(List<FeedItemModel> allItems, String category, String subjectLabel) {
        String normalizedCategory = normalize(category);
        String normalizedSubject = normalize(subjectLabel);
        boolean includeAllCategories = normalizedCategory.isEmpty()
                || UpdatesConstants.CATEGORY_ALL.toLowerCase(Locale.US).equals(normalizedCategory);
        boolean includeAllSubjects = normalizedSubject.isEmpty()
                || "all subjects".equals(normalizedSubject);

        List<FeedItemModel> filteredItems = new ArrayList<>();
        for (FeedItemModel item : allItems) {
            boolean categoryMatches = includeAllCategories
                    || normalize(item.getCategory()).equals(normalizedCategory)
                    || inferCategory(item).equals(normalizedCategory);
            boolean subjectMatches = includeAllSubjects || matchesSubject(item, normalizedSubject);
            if (categoryMatches && subjectMatches) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    private String fetchFeedXml(FeedSource source) throws Exception {
        Request request = new Request.Builder()
                .url(source.getUrl())
                .header("User-Agent", "CampusCore Android RSS Reader")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("RSS request failed");
            }
            return response.body().string();
        }
    }

    private List<FeedItemModel> parseFeed(String xml, FeedSource source) throws Exception {
        List<FeedItemModel> items = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new StringReader(xml));

        boolean insideItem = false;
        String title = "";
        String description = "";
        String link = "";
        String imageUrl = "";
        String publishedDate = "";

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if ("item".equalsIgnoreCase(tagName) || "entry".equalsIgnoreCase(tagName)) {
                    insideItem = true;
                    title = "";
                    description = "";
                    link = "";
                    imageUrl = "";
                    publishedDate = "";
                } else if (insideItem) {
                    if ("title".equalsIgnoreCase(tagName)) {
                        title = readText(parser);
                    } else if ("description".equalsIgnoreCase(tagName) || "summary".equalsIgnoreCase(tagName)) {
                        description = readText(parser);
                    } else if ("link".equalsIgnoreCase(tagName)) {
                        link = readLink(parser, link);
                    } else if ("pubDate".equalsIgnoreCase(tagName)
                            || "published".equalsIgnoreCase(tagName)
                            || "updated".equalsIgnoreCase(tagName)) {
                        publishedDate = readText(parser);
                    } else if ("media:thumbnail".equalsIgnoreCase(tagName)
                            || "media:content".equalsIgnoreCase(tagName)
                            || "enclosure".equalsIgnoreCase(tagName)) {
                        imageUrl = readImageUrl(parser, imageUrl);
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String tagName = parser.getName();
                if (insideItem && ("item".equalsIgnoreCase(tagName) || "entry".equalsIgnoreCase(tagName))) {
                    FeedItemModel item = buildFeedItem(title, description, link, imageUrl, publishedDate, source);
                    if (!item.getTitle().isEmpty() && !item.getArticleUrl().isEmpty()) {
                        items.add(item);
                    }
                    insideItem = false;
                }
            }
            eventType = parser.next();
        }
        return items;
    }

    private FeedItemModel buildFeedItem(String title, String description, String link, String imageUrl,
                                        String publishedDate, FeedSource source) {
        String cleanDescription = cleanHtml(description);
        return new FeedItemModel(
                cleanHtml(title),
                trimDescription(cleanDescription),
                link,
                imageUrl,
                source.getName(),
                source.getCategory(),
                readableDate(publishedDate)
        );
    }

    private String readText(XmlPullParser parser) throws Exception {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
        }
        return text == null ? "" : text.trim();
    }

    private String readLink(XmlPullParser parser, String fallback) throws Exception {
        String href = parser.getAttributeValue(null, "href");
        if (href != null && !href.trim().isEmpty()) {
            return href.trim();
        }
        String text = readText(parser);
        return text.isEmpty() ? fallback : text;
    }

    private String readImageUrl(XmlPullParser parser, String fallback) {
        String url = parser.getAttributeValue(null, "url");
        if (url != null && !url.trim().isEmpty() && isImageUrl(url)) {
            return url.trim();
        }
        return fallback;
    }


    private List<FeedItemModel> uniqueLatestFirst(List<FeedItemModel> items) {
        Set<String> seen = new HashSet<>();
        List<FeedItemModel> uniqueItems = new ArrayList<>();
        for (FeedItemModel item : items) {
            if (seen.add(item.stableId())) {
                uniqueItems.add(item);
            }
        }
        Collections.sort(uniqueItems, (first, second) -> parseDateMillis(second.getPublishedDate())
                .compareTo(parseDateMillis(first.getPublishedDate())));
        if (uniqueItems.size() > UpdatesConstants.MAX_FEED_ITEMS) {
            return new ArrayList<>(uniqueItems.subList(0, UpdatesConstants.MAX_FEED_ITEMS));
        }
        return uniqueItems;
    }

    private boolean matchesSubject(FeedItemModel item, String normalizedSubject) {
        String searchable = normalize(item.getTitle() + " " + item.getDescription() + " " + item.getCategory());
        String[] tokens = normalizedSubject.replace("-", " ").split("\\s+");
        for (String token : tokens) {
            if (token.length() > 2 && searchable.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String inferCategory(FeedItemModel item) {
        String searchable = normalize(item.getTitle() + " " + item.getDescription());
        if (searchable.contains("security") || searchable.contains("cyber")) {
            return UpdatesConstants.CATEGORY_CYBERSECURITY.toLowerCase(Locale.US);
        }
        if (searchable.contains("ai") || searchable.contains("artificial intelligence")
                || searchable.contains("machine learning")) {
            return UpdatesConstants.CATEGORY_AI.toLowerCase(Locale.US);
        }
        return normalize(item.getCategory());
    }

    private String cleanHtml(String value) {
        if (value == null) {
            return "";
        }
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().replaceAll("\\s+", " ").trim();
    }

    private String trimDescription(String value) {
        if (value.length() <= 180) {
            return value;
        }
        return value.substring(0, 177).trim() + "...";
    }

    private String readableDate(String rawDate) {
        Long millis = parseDateMillis(rawDate);
        if (millis == 0L) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(millis));
    }


    @NonNull
    private Long parseDateMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        String[] patterns = {
                "yyyy-MM-dd",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss zzz"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = format.parse(value.trim());
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException ignored) {
            }
        }
        return 0L;
    }

    private boolean isImageUrl(String url) {
        String normalized = url.toLowerCase(Locale.US);
        return normalized.startsWith("https://")
                && (normalized.contains(".jpg") || normalized.contains(".jpeg")
                || normalized.contains(".png") || normalized.contains(".webp"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).trim();
    }
}
