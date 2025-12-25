package com.agenthub.knowledge.bootstrap;

import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.db.repo.KnowledgeSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionSeeder implements CommandLineRunner {
    private final KnowledgeSubscriptionRepository repo;

    @Override
    public void run(String... args) {
        long count = repo.count();
        if (count > 0) return;

        List<Seed> seeds = List.of(
                new Seed("Hacker News Frontpage", "https://hnrss.org/frontpage", "tech,news"),
                new Seed("BBC Technology", "https://feeds.bbci.co.uk/news/technology/rss.xml", "tech,news"),
                new Seed("MIT Technology Review", "https://www.technologyreview.com/feed/", "tech,insight"),
                new Seed("The Verge", "https://www.theverge.com/rss/index.xml", "tech,product"),
                new Seed("Nature - Machine Learning", "https://www.nature.com/subjects/machine-learning.rss", "science,ml"),
                new Seed("ScienceDaily - AI", "https://www.sciencedaily.com/rss/computers_math/artificial_intelligence.xml", "science,ai"),
                new Seed("arXiv - Computer Science", "https://export.arxiv.org/rss/cs", "paper,cs"),
                new Seed("arXiv - AI", "https://export.arxiv.org/rss/cs.AI", "paper,ai"),
                new Seed("arXiv - ML", "https://export.arxiv.org/rss/cs.LG", "paper,ml"),
                new Seed("Google Research Blog", "https://research.google/blog/rss/", "research,ai"),
                new Seed("OpenAI Blog", "https://openai.com/blog/rss/", "ai,product"),
                new Seed("AWS Machine Learning Blog", "https://aws.amazon.com/blogs/machine-learning/feed/", "ml,engineering")
        );

        for (Seed s : seeds) {
            KnowledgeSubscriptionEntity e = new KnowledgeSubscriptionEntity();
            e.setId(UUID.randomUUID());
            e.setName(s.name());
            e.setSourceType("RSS");
            e.setFeedUrl(s.feedUrl());
            e.setEnabled(true);
            e.setFetchLimit(10);
            e.setTags(s.tags());
            repo.save(e);
        }

        log.info("Seeded {} knowledge subscriptions", seeds.size());
    }

    private record Seed(String name, String feedUrl, String tags) {}
}


