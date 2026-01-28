# 🤖 Crypto Quant Grid Bot (Spring Boot)

这是一个基于 Spring Boot 3.4 构建的虚拟货币量化交易系统（模拟仓版）。它集成了**智能舆论分析**引导下的**自适应做多网格策略**，并配有**实时 Telegram 成交通知**。

## 🌟 核心特性

- **Spring Boot 架构**: 高效的定时任务驱动 (`@Scheduled`)。
- **自适应网格 (Auto Re-align)**: 每 5 秒监测市价，当价格偏离网格中心 > 2% 时，自动重新对齐网格，防止踏空。
- **做多网格策略 (Grid Long)**: 针对上升/震荡趋势优化，初始持仓 50%，分批低买高卖。
- **实时监控推送**: 独立的 Node.js 脚本实时监听成交 API，秒级推送到 Telegram。
- **模拟仓系统**: 无需 API Key 即可进行压力测试和策略验证。

## 📂 项目结构

- `src/main/java/bot/quant/grid/`
    - `GridStrategyEngine`: 策略核心逻辑。
    - `PortfolioController`: 持仓与成交历史 API。
    - `SentimentService`: 舆论与波动率分析（Mock）。
- `docs/`
    - `PLAN.md`: 项目设计蓝图。
    - `EXECUTION.md`: 执行与逻辑细节。
- `quant-bot-monitor.js`: Telegram 推送守护进程。

## 🚀 快速开始

### 1. 启动量化引擎 (Java)
```bash
cd quant-bot
mvn spring-boot:run
```

### 2. 启动 Telegram 监控 (Node)
```bash
node quant-bot-monitor.js
```

### 3. 查看实时持仓
```bash
curl http://localhost:8080/portfolio
```

## 📊 当前运行状态 (2026-01-28)
- **初始投入**: 10,000 USDT
- **当前仓位**: 50% BTC + 50% USDT (进可攻退可守)
- **自动化状态**: 已开启全自动对齐与成交推送。

---
*Powered by Clawdbot Agent*
