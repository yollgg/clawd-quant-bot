package bot.quant.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class GridStrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(GridStrategyEngine.class);

    @Autowired
    private MarketAnalyzer analyzer;

    @Autowired
    private BinanceService binanceService;

    private BigDecimal currentPrice = new BigDecimal("90000");
    private BigDecimal lastGridCenter = new BigDecimal("90000");
    private boolean active = false;
    private List<BigDecimal> buyGrids = new ArrayList<>();
    private List<BigDecimal> sellGrids = new ArrayList<>();
    private List<String> tradeHistory = new ArrayList<>();

    // 初始模拟资金调优为 100 USDT (开启 5x 杠杆模拟)
    private double balanceUsdt = 100;
    private double balanceBtc = 0;
    private double initialInvestment = 100;
    private double leverage = 5.0; 
    private double stopLossThreshold = 0.10; // 模拟合约模式止损

    @Scheduled(fixedRate = 10000)
    public void runStrategy() {
        BigDecimal realPrice = binanceService.getLatestPrice("BTCUSDT");
        if (realPrice != null) currentPrice = realPrice;

        double vol4h = analyzer.calculateVolatility("BTCUSDT", "4h", 20);
        
        if (!active && tradeHistory.isEmpty()) {
            autoAlignGrids(vol4h);
            active = true;
        }

        if (!active) return;

        // 止损检查
        double currentVal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        if ((initialInvestment - currentVal) / initialInvestment > stopLossThreshold) {
            log.error("!!! 触发止损 !!! 保证金不足或趋势严重破位，停止运行。");
            active = false;
            return;
        }

        // 漂移对齐
        BigDecimal deviation = currentPrice.subtract(lastGridCenter).abs()
                                .divide(lastGridCenter, 4, BigDecimal.ROUND_HALF_UP);
        if (deviation.doubleValue() > vol4h * 1.5) {
            autoAlignGrids(vol4h);
        }

        executeGridLogic();
    }

    private void autoAlignGrids(double volatility) {
        lastGridCenter = currentPrice;
        buyGrids.clear();
        sellGrids.clear();
        double baseStep = Math.max(0.003, volatility / 4);
        for (int i = 1; i <= 5; i++) {
            double offset = baseStep * Math.pow(1.3, i - 1);
            buyGrids.add(currentPrice.subtract(currentPrice.multiply(new BigDecimal(offset))));
            sellGrids.add(currentPrice.add(currentPrice.multiply(new BigDecimal(offset))));
        }
        log.info("杠杆网格重置：中轴={}, 步长={}", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP), String.format("%.4f", baseStep));
    }

    private void executeGridLogic() {
        buyGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) <= 0) {
                // 每层网格使用 10U 保证金，开 5x 杠杆
                double margin = 10.0;
                double amount = (margin * leverage) / grid.doubleValue();
                if (balanceUsdt >= margin) {
                    balanceUsdt -= margin;
                    balanceBtc += amount;
                    recordTrade("合约开多", grid, amount);
                    // 挂出止盈网格 (步长 0.5%)
                    sellGrids.add(grid.add(grid.multiply(new BigDecimal("0.005"))));
                    return true;
                }
            }
            return false;
        });

        sellGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) >= 0 && balanceBtc > 0) {
                // 简化：每次网格成交卖出约 1/5 仓位
                double amount = balanceBtc / 5.0;
                balanceUsdt += grid.doubleValue() * amount / leverage + (grid.doubleValue() * amount); // 粗略模拟盈亏回吐
                balanceBtc -= amount;
                recordTrade("合约平多", grid, amount);
                return true;
            }
            return false;
        });
    }

    private void recordTrade(String type, BigDecimal price, double amount) {
        String msg = String.format("%s @ %s, 数量: %.4f", type, price.setScale(2, BigDecimal.ROUND_HALF_UP), amount);
        log.info(msg);
        tradeHistory.add(msg);
    }

    public Map<String, Object> getPortfolioSnapshot() {
        Map<String, Object> res = new HashMap<>();
        double currentVal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        res.put("totalValueUsdt", String.format("%.2f", currentVal));
        res.put("balanceUsdt", String.format("%.2f", balanceUsdt));
        res.put("balanceBtc", String.format("%.6f", balanceBtc));
        res.put("currentPrice", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        res.put("activeGrids", buyGrids.size());
        res.put("leverage", leverage + "x");
        res.put("lastTrades", tradeHistory.size() > 5 ? tradeHistory.subList(tradeHistory.size()-5, tradeHistory.size()) : tradeHistory);
        return res;
    }
}
