-- 初始化 Agent Hub 数据表（MySQL，从 0 开始：BIGINT 自增主键）

CREATE TABLE IF NOT EXISTS knowledge_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',
    name VARCHAR(200) NOT NULL COMMENT '订阅名称（站点/栏目名称）',
    zh_name VARCHAR(200) NOT NULL COMMENT '订阅中文名（便于理解）',
    capability_summary VARCHAR(600) NOT NULL COMMENT '网站能力总结（中文，一句话说明内容价值）',
    source_type VARCHAR(50) NOT NULL COMMENT '来源类型（例如：RSS）',
    feed_url VARCHAR(1500) NOT NULL COMMENT '订阅地址（例如 RSS/Atom 地址）',
    feed_url_hash CHAR(64) NOT NULL COMMENT '订阅地址哈希（SHA-256，用于唯一约束）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    fetch_limit INT NOT NULL DEFAULT 20 COMMENT '每次抓取最大条数',
    tags VARCHAR(500) NULL COMMENT '标签（逗号分隔，用于分类/筛选）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_subscription_feed_url_hash (feed_url_hash),
    KEY idx_knowledge_subscription_feed_url (feed_url(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='知识站点订阅配置（供抓取类 Agent 使用）';

CREATE TABLE IF NOT EXISTS knowledge_article (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',
    subscription_id BIGINT NULL COMMENT '关联的订阅ID',
    source_name VARCHAR(200) NULL COMMENT '来源名称（冗余字段，便于查询展示）',
    url VARCHAR(2000) NOT NULL COMMENT '文章原始链接',
    url_hash CHAR(64) NOT NULL COMMENT 'URL 哈希（SHA-256，用于去重唯一键）',
    title VARCHAR(600) NULL COMMENT '文章原始标题',
    published_at TIMESTAMP NULL COMMENT '原始发布时间（若可解析）',
    fetched_at TIMESTAMP NULL COMMENT '抓取时间',
    raw_html LONGTEXT NULL COMMENT '抓取到的原始HTML（可选存储，便于排查）',
    extracted_text LONGTEXT NULL COMMENT '从HTML抽取的正文纯文本',
    detected_lang VARCHAR(32) NULL COMMENT '语言检测结果（粗略）',
    zh_title VARCHAR(600) NULL COMMENT '中文标题（翻译/改写）',
    zh_content LONGTEXT NULL COMMENT '中文正文（翻译/意译，力求自然）',
    reflection LONGTEXT NULL COMMENT '读后感（尽量像真实读者，不要机械AI口吻）',
    status VARCHAR(32) NOT NULL COMMENT '状态（DONE/NEEDS_REVIEW/ERROR）',
    error_message LONGTEXT NULL COMMENT '错误信息（仅 ERROR 时）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_article_url_hash (url_hash),
    KEY idx_knowledge_article_subscription_id (subscription_id),
    CONSTRAINT fk_knowledge_article_subscription
        FOREIGN KEY (subscription_id) REFERENCES knowledge_subscription(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='知识文章（抓取、理解、翻译与读后感的落库结果）';

CREATE TABLE IF NOT EXISTS agent_run (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键（自增）',
    agent_name VARCHAR(200) NOT NULL COMMENT 'Agent 名称',
    started_at TIMESTAMP NOT NULL COMMENT '开始时间',
    finished_at TIMESTAMP NULL COMMENT '结束时间',
    status VARCHAR(32) NOT NULL COMMENT '状态（SUCCESS/FAILED/RUNNING）',
    stats_json LONGTEXT NULL COMMENT '统计信息（JSON）',
    error_message LONGTEXT NULL COMMENT '错误信息（仅 FAILED 时）',
    PRIMARY KEY (id),
    KEY idx_agent_run_agent_name (agent_name),
    KEY idx_agent_run_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Agent 运行日志（便于观测与排障）';

-- 默认订阅源（SQL 方式，幂等：依赖 uk_knowledge_subscription_feed_url_hash）
INSERT INTO knowledge_subscription (name, zh_name, capability_summary, source_type, feed_url, feed_url_hash, enabled, fetch_limit, tags)
VALUES
  -- 科技 / 创新
  ('TechCrunch', '科技创业新鲜事', '聚焦创业、融资、产品发布与科技趋势，适合快速捕捉一线动态。', 'RSS', 'https://techcrunch.com/feed/', SHA2('https://techcrunch.com/feed/', 256), 1, 10, 'tech,innovation,startup'),
  ('The Verge', '科技产品与数码文化', '偏产品评测与科技文化解读，适合把“新东西”讲清楚。', 'RSS', 'https://www.theverge.com/rss/index.xml', SHA2('https://www.theverge.com/rss/index.xml', 256), 1, 10, 'tech,product,culture'),
  ('MIT Technology Review', 'MIT 科技评论', '前沿科技与 AI 趋势洞察，偏深度与未来走向。', 'RSS', 'https://www.technologyreview.com/feed/', SHA2('https://www.technologyreview.com/feed/', 256), 1, 10, 'tech,ai,future'),
  ('WIRED', '连线杂志', '科技×社会×文化的长文与观点，适合做更宏观的趋势理解。', 'RSS', 'https://www.wired.com/feed/rss', SHA2('https://www.wired.com/feed/rss', 256), 1, 10, 'tech,culture,society'),

  -- 科学 / 健康 / 医学
  ('WebMD', 'WebMD 健康科普', '面向大众的健康知识与疾病预防解释，适合做“实用型科普”。', 'RSS', 'https://rssfeeds.webmd.com/rss/rss.aspx?RSSSource=RSS_PUBLIC', SHA2('https://rssfeeds.webmd.com/rss/rss.aspx?RSSSource=RSS_PUBLIC', 256), 1, 10, 'health,medicine'),
  ('Mayo Clinic', '梅奥诊所健康指南', '权威医学机构的健康建议与知识库更新，适合做可信度高的科普。', 'RSS', 'https://www.mayoclinic.org/rss/all-updates', SHA2('https://www.mayoclinic.org/rss/all-updates', 256), 1, 10, 'health,medicine'),
  ('Knowable Magazine', 'Knowable 科学解读', '把最新研究讲给普通人听，擅长把复杂论文翻译成可读故事。', 'RSS', 'https://knowablemagazine.org/rss', SHA2('https://knowablemagazine.org/rss', 256), 1, 10, 'science,explain'),
  ('ScienceAlert', '科学快讯', '科研新闻与通俗解读，更新快，适合做热点科学搬运。', 'RSS', 'https://www.sciencealert.com/feed', SHA2('https://www.sciencealert.com/feed', 256), 1, 10, 'science,news'),
  ('Smithsonian Magazine', '史密森尼杂志', '科学、历史、文化的高质量叙事，适合做“知识型长文”。', 'RSS', 'https://www.smithsonianmag.com/rss/', SHA2('https://www.smithsonianmag.com/rss/', 256), 1, 10, 'science,history,culture'),
  ('ScienceDaily - AI', 'ScienceDaily 人工智能', 'AI 相关科研新闻聚合，适合快速扫一遍研究趋势。', 'RSS', 'https://www.sciencedaily.com/rss/computers_math/artificial_intelligence.xml', SHA2('https://www.sciencedaily.com/rss/computers_math/artificial_intelligence.xml', 256), 1, 10, 'science,ai,news'),
  ('Nature - Machine Learning', 'Nature 机器学习专题', '顶级期刊的机器学习专题更新，适合追踪高质量研究线索。', 'RSS', 'https://www.nature.com/subjects/machine-learning.rss', SHA2('https://www.nature.com/subjects/machine-learning.rss', 256), 1, 10, 'science,ml'),
  ('Quanta Magazine', 'Quanta 量子杂志', '数学/物理/计算机前沿的故事化报道，适合做深度科普与洞察。', 'RSS', 'https://www.quantamagazine.org/feed/', SHA2('https://www.quantamagazine.org/feed/', 256), 1, 10, 'science,math,ai'),

  -- 社会 / 文化 / 思想
  ('Aeon', 'Aeon 人文与思想', '哲学、人文与社会思考的长文精选，适合做观点型深读。', 'RSS', 'https://aeon.co/feed', SHA2('https://aeon.co/feed', 256), 1, 10, 'culture,philosophy,society'),
  ('Medium (Technology)', 'Medium 技术随笔', '大量一线作者的原创随笔与经验分享，适合挖“具体方法/踩坑”。', 'RSS', 'https://medium.com/feed/tag/technology', SHA2('https://medium.com/feed/tag/technology', 256), 1, 10, 'culture,tech,essay'),
  ('The Atlantic', '大西洋月刊', '社会文化与政治的深度分析，适合做结构化解读与观点延展。', 'RSS', 'https://www.theatlantic.com/feed/all/', SHA2('https://www.theatlantic.com/feed/all/', 256), 1, 10, 'society,politics,culture'),
  ('Longreads', '长文精选', '全球长文聚合，适合做高质量故事/调查类文章的筛选器。', 'RSS', 'https://longreads.com/feed/', SHA2('https://longreads.com/feed/', 256), 1, 10, 'longform,culture,story'),
  ('Nautilus', 'Nautilus 科学人文故事', '科学与人文交叉的叙事型文章，适合做“讲故事的科普”。', 'RSS', 'https://nautil.us/feed/', SHA2('https://nautil.us/feed/', 256), 1, 10, 'science,culture,story'),
  ('3 Quarks Daily', '三夸克日报', '文学/哲学/科学的观点精选，适合做跨学科的日常灵感来源。', 'RSS', 'https://www.3quarksdaily.com/3quarksdaily/feed', SHA2('https://www.3quarksdaily.com/3quarksdaily/feed', 256), 1, 10, 'culture,philosophy,science'),

  -- 商业 / 管理 / 创业
  ('Harvard Business Review', '哈佛商业评论', '管理学与组织策略的高密度文章，适合提炼可执行的管理建议。', 'RSS', 'https://hbr.org/feed', SHA2('https://hbr.org/feed', 256), 1, 10, 'business,management'),
  ('Fast Company', '快公司', '创新商业与产品/品牌趋势，适合抓案例、做本地化改写。', 'RSS', 'https://www.fastcompany.com/feed', SHA2('https://www.fastcompany.com/feed', 256), 1, 10, 'business,innovation'),

  -- 研发/资讯
  ('Hacker News Frontpage', 'HN 技术热榜', '技术圈最强“线索雷达”，适合发现新工具、新论文、新观点。', 'RSS', 'https://hnrss.org/frontpage', SHA2('https://hnrss.org/frontpage', 256), 1, 10, 'tech,news,startup'),
  ('BBC Technology', 'BBC 科技新闻', '大众视角的科技新闻，适合做通俗化选题与背景补充。', 'RSS', 'https://feeds.bbci.co.uk/news/technology/rss.xml', SHA2('https://feeds.bbci.co.uk/news/technology/rss.xml', 256), 1, 10, 'tech,news'),
  ('Google Research Blog', 'Google 研究博客', '谷歌研究团队的成果解读与应用方向，适合追踪大厂研究落地。', 'RSS', 'https://research.google/blog/rss/', SHA2('https://research.google/blog/rss/', 256), 1, 10, 'research,ai'),
  ('OpenAI Blog', 'OpenAI 官方博客', '模型/产品更新与观点文章，适合做 AI 领域一手信息汇总。', 'RSS', 'https://openai.com/blog/rss/', SHA2('https://openai.com/blog/rss/', 256), 1, 10, 'ai,product,news'),
  ('AWS Machine Learning Blog', 'AWS 机器学习博客', '工程落地与云上实践为主，适合提炼“怎么做”与架构模板。', 'RSS', 'https://aws.amazon.com/blogs/machine-learning/feed/', SHA2('https://aws.amazon.com/blogs/machine-learning/feed/', 256), 1, 10, 'ml,engineering,cloud'),
  ('arXiv - Computer Science', 'arXiv 计算机科学', '预印本聚合，适合批量发现论文选题与研究脉络。', 'RSS', 'https://export.arxiv.org/rss/cs', SHA2('https://export.arxiv.org/rss/cs', 256), 1, 10, 'paper,cs'),
  ('arXiv - AI', 'arXiv 人工智能', 'AI 方向最新预印本更新，适合追踪热点方向与关键论文。', 'RSS', 'https://export.arxiv.org/rss/cs.AI', SHA2('https://export.arxiv.org/rss/cs.AI', 256), 1, 10, 'paper,ai'),
  ('arXiv - ML', 'arXiv 机器学习', '机器学习方向最新预印本更新，适合做论文速读与趋势总结。', 'RSS', 'https://export.arxiv.org/rss/cs.LG', SHA2('https://export.arxiv.org/rss/cs.LG', 256), 1, 10, 'paper,ml')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  zh_name = VALUES(zh_name),
  capability_summary = VALUES(capability_summary),
  source_type = VALUES(source_type),
  tags = VALUES(tags),
  enabled = VALUES(enabled),
  fetch_limit = VALUES(fetch_limit);


