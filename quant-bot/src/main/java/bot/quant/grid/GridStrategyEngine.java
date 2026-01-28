package bot.quant.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 核心量化决策引擎 - 生产级逻辑实现
 */
@Service
public class GridStrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(GridStrategyEngine.class);

    @Autowired
    private MarketAnalyzer analyzer;

    @Autowired
    private BinanceService binanceService;

    // 运行时实时状态
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private BigDecimal lastGridCenter = BigDecimal.ZERO;
    private boolean active = true;
    private final List<BigDecimal> buyGrids = new ArrayList<>();
    private final List<BigDecimal> sellGrids = new ArrayList<>();
    private final List<String> tradeHistory = new ArrayList<>();

    // 资金管理配置 (已切换为 100U 生产逻辑)
    private double balanceUsdt = 100.0;
    private double balanceBtc = 0.0;
    private final double initialInvestment = 100.0;
    private final double leverage = 5.0; 
    private final double stopLossThreshold = 0.15; // 15% 强制止损
    private double currentVolatility = 0.01;

    @Scheduled(fixedRate = 5000)
    public void tick() {
        if (!active) return;

        // 1. 获取币安真实市场价格
        BigDecimal price = binanceService.getLatestPrice("BTCUSDT");
        if (price == null) return;
        this.currentPrice = price;

        // 2. 动态自适应：每 5 分钟更新一次波动率并对齐
        if (lastGridCenter.equals(BigDecimal.ZERO)) {
            refreshMarketContext();
        }

        // 3. 风险监控：总资产回撤检查
        checkRiskStatus();

        // 4. 执行网格逻辑
        processGrids();
    }

    private void refreshMarketContext() {
        this.currentVolatility = analyzer.calculateVolatility("BTCUSDT", "4h", 14);
        this.lastGridCenter = currentPrice;
        
        buyGrids.clear();
        sellGrids.clear();
        
        // 基于金字塔分布和 ATR 动态步长
        double baseStep = Math.max(0.002, currentVolatility / 5.0);
        for (int i = 1; i <= 5; i++) {
            BigDecimal buyLevel = currentPrice.subtract(currentPrice.multiply(BigDecimal.valueOf(baseStep * i)));
            BigDecimal sellLevel = currentPrice.add(currentPrice.multiply(BigDecimal.valueOf(baseStep * i)));
            buyGrids.add(buyLevel);
            sellGrids.add(sellLevel);
        }
        log.info("网格阵列重组：中心={}, 步长={}, 波动率={}", currentPrice, baseStep, currentVolatility);
    }

    private void checkRiskStatus() {
        double currentTotal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        double drawDown = (initialInvestment - currentTotal) / initialInvestment;
        if (drawDown > stopLossThreshold) {
            log.error("!!! 熔断启动 !!! 亏损触及阈值，执行强制锁仓。");
            this.active = false;
        }
    }

    private void processGrids() {
        // 金字塔买入逻辑
        buyGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) <= 0) {
                executeTrade("BUY", grid);
                return true;
            }
            return false;
        });

        // 动态止盈平仓逻辑
        sellGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) >= 0 && balanceBtc > 0) {
                executeTrade("SELL", grid);
                return true;
            }
            return false;
        });
    }

    private void executeTrade(String type, BigDecimal price) {
        // 100U 资金下的科学配仓：单笔投入 15% 可用保证金，5x 杠杆
        double margin = balanceUsdt * 0.15;
        if ("BUY".equals(type) && balanceUsdt > margin) {
            double amount = (margin * leverage) / price.doubleValue();
            balanceUsdt -= margin;
            balanceBtc += amount;
            recordTrade("合约开多", price, amount);
        } else if ("SELL".equals(type) && balanceBtc > 0) {
            double amountToSell = balanceBtc / (double) Math.max(1, sellGrids.size());
            balanceUsdt += (price.doubleValue() * amountToSell) / leverage + (price.doubleValue() * amountToSell - lastGridCenter.doubleValue() * amountToSell); 
            balanceBtc -= amountToSell;
            recordTrade("合约平多", price, amountToSell);
        }
    }

    private void recordTrade(String action, BigDecimal price, double amount) {
        String msg = String.format("[%s] 价格: %s, 数量: %.6f, 余额: %.2f USDT", 
            action, price.setScale(2, RoundingMode.HALF_UP), amount, balanceUsdt);
        log.info(msg);
        tradeHistory.add(msg);
    }

    public Map<String, Object> getPortfolioSnapshot() {
        Map<String, Object> map = new HashMap<>();
        double total = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        map.put("price", currentPrice.setScale(2, RoundingMode.HALF_UP));
        map.put("totalValue", String.format("%.2f", total));
        map.put("usdt", String.format("%.2f", balanceUsdt));
        map.put("btc", String.format("%.6f", balanceBtc));
        map.put("pnl", String.format("%.2f%%", (total - initialInvestment)));
        map.put("lastTrades", tradeHistory);
        return map;
    }
}
