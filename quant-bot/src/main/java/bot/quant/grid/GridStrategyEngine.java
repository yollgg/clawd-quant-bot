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
    private MarketAnalyzer analyzer;

    @Autowired
    private BinanceService binanceService;

    private BigDecimal currentPrice = new BigDecimal("90000");
    private BigDecimal lastGridCenter = new BigDecimal("90000");
    private boolean active = false;
    private List<BigDecimal> buyGrids = new ArrayList<>();
    private List<BigDecimal> sellGrids = new ArrayList<>();
    private List<String> tradeHistory = new ArrayList<>();

    private double balanceUsdt = 5000;
    private double balanceBtc = 0.05;
    private double initialInvestment = 10000;
    private double stopLossThreshold = 0.05;

    @Scheduled(fixedRate = 10000) // 每 10s 进行一次多维度扫描
    public void runStrategy() {
        // 1. 获取币安实价
        BigDecimal realPrice = binanceService.getLatestPrice("BTCUSDT");
        if (realPrice != null) currentPrice = realPrice;

        // 2. 宏观与波动率分析
        double vol4h = analyzer.calculateVolatility("BTCUSDT", "4h", 20);
        String flow = analyzer.getGlobalCapitalFlow();

        if (!active) {
            log.info("系统初始化：4H波动率={}, 资金流向={}", String.format("%.4f", vol4h), flow);
            autoAlignGrids(vol4h);
            active = true;
        }

        // 3. 止损检查
        double currentVal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        if ((initialInvestment - currentVal) / initialInvestment > stopLossThreshold) {
            log.error("!!! 触发止损 !!! 停止所有逻辑。");
            active = false;
            return;
        }

        // 4. 自适应重布网：如果偏离度 > 波动率的两倍，则重置
        BigDecimal deviation = currentPrice.subtract(lastGridCenter).abs()
                                .divide(lastGridCenter, 4, BigDecimal.ROUND_HALF_UP);
        if (deviation.doubleValue() > vol4h * 1.5) {
            log.info("检测到行情漂移，基于最新波动率 {} 自动对齐...", vol4h);
            autoAlignGrids(vol4h);
        }

        executeGridLogic();
    }

    private void autoAlignGrids(double volatility) {
        lastGridCenter = currentPrice;
        buyGrids.clear();
        sellGrids.clear();
        
        // 动态间距逻辑：基础间距 = 波动率 / 4
        double baseStep = Math.max(0.003, volatility / 4);
        
        for (int i = 1; i <= 5; i++) {
            double offset = baseStep * Math.pow(1.3, i - 1);
            buyGrids.add(currentPrice.subtract(currentPrice.multiply(new BigDecimal(offset))));
            sellGrids.add(currentPrice.add(currentPrice.multiply(new BigDecimal(offset))));
        }
        log.info("网格自适应完成：中轴={}, 基础步长={}", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP), String.format("%.4f", baseStep));
    }

    private void executeGridLogic() {
        buyGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) <= 0) {
                double amount = 0.01 * (1 + (5 - buyGrids.size()) * 0.2); 
                double cost = grid.doubleValue() * amount;
                if (balanceUsdt >= cost) {
                    balanceUsdt -= cost;
                    balanceBtc += amount;
                    recordTrade("买入", grid, amount);
                    return true;
                }
            }
            return false;
        });

        sellGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) >= 0 && balanceBtc >= 0.01) {
                double amount = 0.01;
                balanceUsdt += grid.doubleValue() * amount;
                balanceBtc -= amount;
                recordTrade("卖出", grid, amount);
                return true;
            }
            return false;
        });
    }

    private void recordTrade(String type, BigDecimal price, double amount) {
        String msg = String.format("自动成交[%s] @ %s, 数量: %.4f", type, price.setScale(2, BigDecimal.ROUND_HALF_UP), amount);
        log.info(msg);
        tradeHistory.add(msg);
    }

    public java.util.Map<String, Object> getPortfolioSnapshot() {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("totalValueUsdt", String.format("%.2f", balanceUsdt + (balanceBtc * currentPrice.doubleValue())));
        res.put("balanceBtc", String.format("%.4f", balanceBtc));
        res.put("lastTrades", tradeHistory.size() > 5 ? tradeHistory.subList(tradeHistory.size()-5, tradeHistory.size()) : tradeHistory);
        return res;
    }
}
