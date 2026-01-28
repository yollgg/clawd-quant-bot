package bot.quant.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GridStrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(GridStrategyEngine.class);

    @Autowired
    private SentimentService sentimentService;

    @Autowired
    private BinanceService binanceService;

    private BigDecimal currentPrice = new BigDecimal("90060.25");
    private BigDecimal lastGridCenter = new BigDecimal("90060.25");
    private boolean active = false;
    private List<BigDecimal> buyGrids = new ArrayList<>();
    private List<BigDecimal> sellGrids = new ArrayList<>();
    private List<String> tradeHistory = new ArrayList<>();

    // 初始模拟资金
    private double balanceUsdt = 5000;
    private double balanceBtc = 0.0522;
    private double initialInvestment = 10000;
    private double stopLossThreshold = 0.05; // 5% 止损阈值

    public java.util.Map<String, Object> getPortfolioSnapshot() {
        double currentVal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        double pnl = currentVal - initialInvestment;
        double pnlPercentage = (pnl / initialInvestment);
        
        java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
        snapshot.put("currentPrice", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        snapshot.put("balanceUsdt", String.format("%.2f", balanceUsdt));
        snapshot.put("balanceBtc", String.format("%.4f", balanceBtc));
        snapshot.put("totalValueUsdt", String.format("%.2f", currentVal));
        snapshot.put("pnlUsdt", String.format("%.2f", pnl));
        snapshot.put("pnlPercentage", String.format("%.2f%%", pnlPercentage * 100));
        snapshot.put("activeGrids", buyGrids.size() + sellGrids.size());
        snapshot.put("status", active ? "RUNNING" : "STOPPED_BY_LIMIT");
        snapshot.put("lastTrades", tradeHistory.size() > 5 ? tradeHistory.subList(tradeHistory.size()-5, tradeHistory.size()) : tradeHistory);
        return snapshot;
    }

    @Scheduled(fixedRate = 5000)
    public void runStrategy() {
        BigDecimal realPrice = binanceService.getLatestPrice("BTCUSDT");
        if (realPrice != null) {
            currentPrice = realPrice;
        } else {
            simulatePriceMovement();
        }

        if (!active && tradeHistory.isEmpty()) {
            autoAlignGrids();
            active = true;
            return;
        }

        if (!active) return;

        // 1. 止损评估机制 (基于总资产)
        double currentVal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        if ((initialInvestment - currentVal) / initialInvestment > stopLossThreshold) {
            log.warn("!!! 触发止损 !!! 当前亏损超过 {}%，停止所有交易。", stopLossThreshold * 100);
            active = false;
            return;
        }

        // 2. 动态自适应：偏离网格中心超过 2% 自动重布网
        BigDecimal deviation = currentPrice.subtract(lastGridCenter).abs()
                                .divide(lastGridCenter, 4, BigDecimal.ROUND_HALF_UP);
        if (deviation.compareTo(new BigDecimal("0.02")) > 0) {
            log.info("检测到价格偏离超过 2%，执行金字塔分布重布网...");
            autoAlignGrids();
        }

        executeGridLongLogic();
    }

    private void autoAlignGrids() {
        log.info("--- 策略对齐/更新: {} ---", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        lastGridCenter = currentPrice;
        setupPyramidGrids(currentPrice);
    }

    private void setupPyramidGrids(BigDecimal centerPrice) {
        buyGrids.clear();
        sellGrids.clear();
        
        // 3. 金字塔分布：间距采用非线性增长 (类似高斯思路)
        for (int i = 1; i <= 5; i++) {
            double offsetFactor = 0.005 * Math.pow(1.5, i-1);
            BigDecimal buyLevel = centerPrice.subtract(centerPrice.multiply(new BigDecimal(offsetFactor)));
            BigDecimal sellLevel = centerPrice.add(centerPrice.multiply(new BigDecimal(offsetFactor)));
            buyGrids.add(buyLevel);
            sellGrids.add(sellLevel);
        }
        log.info("金字塔网格布局完成。中轴: {}", centerPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    private void simulatePriceMovement() {
        double change = (Math.random() - 0.5) * 200;
        currentPrice = currentPrice.add(new BigDecimal(change));
    }

    private void executeGridLongLogic() {
        // 4. 仓位管理：金字塔买入，越低买越多
        buyGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) <= 0) {
                double baseAmount = 0.01;
                // 根据剩余网格数计算权重，越低仓位越大
                double multiplier = 1.0 + (buyGrids.size() * 0.2); 
                double amountToBuy = baseAmount * multiplier;
                
                double cost = grid.doubleValue() * amountToBuy;
                if (balanceUsdt >= cost) {
                    balanceUsdt -= cost;
                    balanceBtc += amountToBuy;
                    String msg = String.format("金字塔[买入] @ %s, 数量: %.4f", grid.setScale(2, BigDecimal.ROUND_HALF_UP), amountToBuy);
                    tradeHistory.add(msg);
                    log.info(msg);
                    sellGrids.add(grid.add(grid.multiply(new BigDecimal("0.005"))));
                    return true;
                }
            }
            return false;
        });

        sellGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) >= 0 && balanceBtc >= 0.01) {
                double amountToSell = 0.01;
                double revenue = grid.doubleValue() * amountToSell;
                balanceUsdt += revenue;
                balanceBtc -= amountToSell;
                String msg = String.format("止盈[卖出] @ %s", grid.setScale(2, BigDecimal.ROUND_HALF_UP));
                tradeHistory.add(msg);
                log.info(msg);
                buyGrids.add(grid.subtract(grid.multiply(new BigDecimal("0.005"))));
                return true;
            }
            return false;
        });
    }
}
