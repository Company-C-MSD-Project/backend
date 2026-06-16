package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/** News article response (lib/news-admin-data.ts NewsArticle), snake_case. */
@Getter
@Builder
public class NewsArticleV1 {
    private final String id;
    private final String title;
    private final String excerpt;
    private final String body;
    private final String category;
    private final String tag;

    @JsonProperty("image_url")
    private final String imageUrl;

    private final String status;

    @JsonProperty("publish_at")
    private final String publishAt;

    @JsonProperty("created_at")
    private final String createdAt;

    @JsonProperty("updated_at")
    private final String updatedAt;
}
