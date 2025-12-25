-- 初始化 Agent Hub 数据表

CREATE TABLE IF NOT EXISTS knowledge_subscription (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    feed_url VARCHAR(1500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fetch_limit INT NOT NULL DEFAULT 20,
    tags VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_article (
    id UUID PRIMARY KEY,
    subscription_id UUID,
    source_name VARCHAR(200),
    url VARCHAR(2000) NOT NULL,
    title VARCHAR(600),
    published_at TIMESTAMP,
    fetched_at TIMESTAMP,
    raw_html TEXT,
    extracted_text TEXT,
    detected_lang VARCHAR(32),
    zh_title VARCHAR(600),
    zh_content TEXT,
    reflection TEXT,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_article_url ON knowledge_article(url);

CREATE TABLE IF NOT EXISTS agent_run (
    id UUID PRIMARY KEY,
    agent_name VARCHAR(200) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    stats_json TEXT,
    error_message TEXT
);

ALTER TABLE knowledge_article
    ADD CONSTRAINT IF NOT EXISTS fk_knowledge_article_subscription
    FOREIGN KEY (subscription_id) REFERENCES knowledge_subscription(id);

COMMENT ON TABLE knowledge_subscription IS '知识站点订阅配置（供抓取类 Agent 使用）';
COMMENT ON COLUMN knowledge_subscription.id IS '主键';
COMMENT ON COLUMN knowledge_subscription.name IS '订阅名称（站点/栏目名称）';
COMMENT ON COLUMN knowledge_subscription.source_type IS '来源类型（例如：RSS）';
COMMENT ON COLUMN knowledge_subscription.feed_url IS '订阅地址（例如 RSS/Atom 地址）';
COMMENT ON COLUMN knowledge_subscription.enabled IS '是否启用';
COMMENT ON COLUMN knowledge_subscription.fetch_limit IS '每次抓取最大条数';
COMMENT ON COLUMN knowledge_subscription.tags IS '标签（逗号分隔，用于分类/筛选）';
COMMENT ON COLUMN knowledge_subscription.created_at IS '创建时间';
COMMENT ON COLUMN knowledge_subscription.updated_at IS '更新时间';

COMMENT ON TABLE knowledge_article IS '知识文章（抓取、理解、翻译与读后感的落库结果）';
COMMENT ON COLUMN knowledge_article.id IS '主键';
COMMENT ON COLUMN knowledge_article.subscription_id IS '关联的订阅ID';
COMMENT ON COLUMN knowledge_article.source_name IS '来源名称（冗余字段，便于查询展示）';
COMMENT ON COLUMN knowledge_article.url IS '文章原始链接（用于去重）';
COMMENT ON COLUMN knowledge_article.title IS '文章原始标题';
COMMENT ON COLUMN knowledge_article.published_at IS '原始发布时间（若可解析）';
COMMENT ON COLUMN knowledge_article.fetched_at IS '抓取时间';
COMMENT ON COLUMN knowledge_article.raw_html IS '抓取到的原始HTML（可选存储，便于排查）';
COMMENT ON COLUMN knowledge_article.extracted_text IS '从HTML抽取的正文纯文本';
COMMENT ON COLUMN knowledge_article.detected_lang IS '语言检测结果（粗略）';
COMMENT ON COLUMN knowledge_article.zh_title IS '中文标题（翻译/改写）';
COMMENT ON COLUMN knowledge_article.zh_content IS '中文正文（翻译/意译，力求自然）';
COMMENT ON COLUMN knowledge_article.reflection IS '读后感（尽量像真实读者，不要机械AI口吻）';
COMMENT ON COLUMN knowledge_article.status IS '状态（DONE/NEEDS_REVIEW/ERROR）';
COMMENT ON COLUMN knowledge_article.error_message IS '错误信息（仅 ERROR 时）';
COMMENT ON COLUMN knowledge_article.created_at IS '创建时间';
COMMENT ON COLUMN knowledge_article.updated_at IS '更新时间';

COMMENT ON TABLE agent_run IS 'Agent 运行日志（便于观测与排障）';
COMMENT ON COLUMN agent_run.id IS '主键';
COMMENT ON COLUMN agent_run.agent_name IS 'Agent 名称';
COMMENT ON COLUMN agent_run.started_at IS '开始时间';
COMMENT ON COLUMN agent_run.finished_at IS '结束时间';
COMMENT ON COLUMN agent_run.status IS '状态（SUCCESS/FAILED/RUNNING）';
COMMENT ON COLUMN agent_run.stats_json IS '统计信息（JSON）';
COMMENT ON COLUMN agent_run.error_message IS '错误信息（仅 FAILED 时）';


