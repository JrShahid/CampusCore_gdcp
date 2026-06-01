package com.example.campuscore.network;

import com.example.campuscore.utils.UpdatesConstants;
import com.example.campuscore.utils.AcademicDataProvider;

import java.util.ArrayList;
import java.util.List;

public final class UpdatesFeedProvider {
    private UpdatesFeedProvider() {
    }

    public static List<FeedSource> sourcesForDepartment(String department) {
        String departmentName = AcademicDataProvider.departmentNameForCode(department);
        List<FeedSource> sources = new ArrayList<>();
        sources.add(new FeedSource("MIT Technology Review", "https://www.technologyreview.com/feed/", UpdatesConstants.CATEGORY_AI));
        sources.add(new FeedSource("IEEE Spectrum", "https://spectrum.ieee.org/feed", UpdatesConstants.CATEGORY_TECHNOLOGY));
        sources.add(new FeedSource("freeCodeCamp", "https://www.freecodecamp.org/news/rss/", UpdatesConstants.CATEGORY_PROGRAMMING));
        sources.add(new FeedSource("arXiv Computer Science", "https://arxiv.org/rss/cs", UpdatesConstants.CATEGORY_RESEARCH));

        if ("Computer Science".equalsIgnoreCase(departmentName) || "Information Technology".equalsIgnoreCase(departmentName)) {
            sources.add(new FeedSource("TechCrunch", "https://techcrunch.com/feed/", UpdatesConstants.CATEGORY_TECHNOLOGY));
            return sources;
        }

        if ("Electronics".equalsIgnoreCase(departmentName)) {
            sources.add(new FeedSource("IEEE Spectrum", "https://spectrum.ieee.org/feed", UpdatesConstants.CATEGORY_TECHNOLOGY));
            return sources;
        }

        return sources;
    }

    public static List<String> categories() {
        List<String> categories = new ArrayList<>();
        categories.add(UpdatesConstants.CATEGORY_ALL);
        categories.add(UpdatesConstants.CATEGORY_RESEARCH);
        categories.add(UpdatesConstants.CATEGORY_TECHNOLOGY);
        categories.add(UpdatesConstants.CATEGORY_PROGRAMMING);
        categories.add(UpdatesConstants.CATEGORY_AI);
        categories.add(UpdatesConstants.CATEGORY_CYBERSECURITY);
        return categories;
    }
}
