package com.agenthub.attract.service;

import com.agenthub.attract.model.EngagementSignals;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class AttractivenessScoringService {

    public BigDecimal score(EngagementSignals s, Instant publishedAt) {
        double points = s == null || s.hnPoints() == null ? 0.0 : s.hnPoints();
        double comments = s == null || s.commentCount() == null ? 0.0 : s.commentCount();
        double likes = s == null || s.likeCount() == null ? 0.0 : s.likeCount();
        double views = s == null || s.viewCount() == null ? 0.0 : s.viewCount();
        double shares = s == null || s.shareCount() == null ? 0.0 : s.shareCount();

        double pScore = Math.log1p(points) * 10.0;
        double cScore = Math.log1p(comments) * 8.0;
        double lScore = Math.log1p(likes) * 4.0;
        double vScore = Math.log1p(views) * 2.0;
        double sScore = Math.log1p(shares) * 6.0;

        double recency = 0.0;
        if (publishedAt != null) {
            long hours = Math.max(0, Duration.between(publishedAt, Instant.now()).toHours());
            // 近 48 小时给予衰减加成：0~12 分
            recency = Math.max(0.0, 12.0 - (hours / 4.0));
        }

        double total = pScore + cScore + lScore + vScore + sScore + recency;
        return BigDecimal.valueOf(total).setScale(4, RoundingMode.HALF_UP);
    }
}


