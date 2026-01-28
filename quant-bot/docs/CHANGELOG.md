# 量化系统开发日志 (Development Log)

## 2026-01-28 22:55 - 系统持久化与生产级重构

### 1. 数据库层实现 (Persistence Layer)
- **目标**: 解决系统重启后数据丢失的问题，确保资产和成交记录的永久性。
- **操作**:
    - 在 `pom.xml` 中引入 `spring-boot-starter-data-jpa` 和 `h2` 数据库依赖。
    - 创建 `TradeRecord` 实体类，记录：成交类型、价格、数量、成交后余额、时间戳。
    - 创建 `PortfolioState` 实体类，记录：USDT 余额、BTC 持仓、初始投入金额。
    - 配置 `application.properties`，将数据库设置为文件模式 (`./data/quantdb`)。

### 2. 核心引擎“去模拟化” (De-mocking Core Engine)
- **目标**: 替换所有占位符代码，接入真实市场逻辑。
- **操作**:
    - **行情源**: 彻底打通 `BinanceService`，每一秒的决策都基于币安 API 的真实成交价。
    - **波动率算法**: 实现 `MarketAnalyzer` 中的 ATR (平均真实波幅) 计算，基于 14 根 4 小时 K 线动态调整网格步长。
    - **合约账本**: 实现精细的保证金计算逻辑 (单笔可用资金 15%)，配合 5 倍杠杆模拟真实的盈亏曲线。

### 3. 风险控制模块 (Risk Management)
- **目标**: 保护 100U 初始本金不被极端行情归零。
- **操作**:
    - 引入“熔断开关”：每 5 秒计算总资产净值，一旦回撤超过 15%，程序自动进入 `STOPPED_BY_LIMIT` 状态。

### 4. 自动化监控与汇报 (Automation & Reporting)
- **目标**: 减少人工干预，实现主动提醒。
- **操作**:
    - 启动 `quant-bot-monitor.js` 守护进程，10 秒/次监听数据库成交更新并推送至 Telegram。
    - 注册 Cron 任务 `hourly-portfolio-report`，每小时整点生成资产快照。

---
**当前状态**: 阶段三 (模拟仓运行) 已完成生产级加固。
**下一步计划**: 对接多智能体行情分析模块 (Analyst Agent)。
