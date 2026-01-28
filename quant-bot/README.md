# 🤖 Crypto Quant Grid Bot (Spring Boot)

这是一个基于 Spring Boot 3.4 构建的虚拟货币量化交易系统（100U 合约模拟版）。它集成了**智能波动率分析**引导下的**自适应做多网格策略**，并配有**实时 Telegram 成交通知**。

## 🌟 核心特性

- **Spring Boot 架构**: 高效的定时任务驱动 (`@Scheduled`)。
- **100U 合约模式**: 模拟 5x 杠杆、保证金制度及爆仓止损逻辑。
- **自适应波动率 (Volatility-Aware)**: 基于币安 4H K线计算 ATR，动态调整网格间距。
- **实时监控推送**: 独立的 Node.js 脚本秒级推送到 Telegram。
- **每小时自动化报表**: 定时推送资产、持仓及挂单快照。

## 📂 项目结构

- `src/main/java/bot/quant/grid/`
    - `GridStrategyEngine`: 策略核心（含杠杆与金字塔逻辑）。
    - `MarketAnalyzer`: 4H 波动率与资金流向分析模块。
    - `BinanceService`: 实时行情采集。
    - `PortfolioController`: 持仓数据接口。
- `docs/`: 详细的计划与执行文档。
- `quant-bot-monitor.js`: Telegram 推送守护进程。

## 🤖 多智能体协作路线图 (Sub-Agents)

本系统支持通过召唤专门的智能体来异步完善：

1. **`analyst-agent` (行情分析员)**: 负责 24/7 监控社交媒体与宏观数据，向引擎推送风险评分。
2. **`dev-agent` (架构师)**: 负责将 H2 内存库升级为实体数据库，并完善 Web 控制面板。
3. **`tester-agent` (测试员)**: 负责进行蒙特卡洛模拟，寻找当前行情下的最优网格参数。

---
*Powered by Clawdbot Agent*
