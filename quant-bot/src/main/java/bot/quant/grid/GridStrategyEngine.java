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

    private BigDecimal currentPrice = new BigDecimal("95759");
    private BigDecimal lastGridCenter = new BigDecimal("95759");
    private boolean active = false;
    private List<BigDecimal> buyGrids = new ArrayList<>();
    private List<BigDecimal> sellGrids = new ArrayList<>();
    private List<String> tradeHistory = new ArrayList<>();

    // 初始模拟资金
    private double balanceUsdt = 5000;
    private double balanceBtc = 0.0522;
    private double initialInvestment = 10000;

    public java.util.Map<String, Object> getPortfolioSnapshot() {
        double currentVal = balanceUsdt + (balanceBtc * currentPrice.doubleValue());
        double pnl = currentVal - initialInvestment;
        
        java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
        snapshot.put("currentPrice", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        snapshot.put("balanceUsdt", String.format("%.2f", balanceUsdt));
        snapshot.put("balanceBtc", String.format("%.4f", balanceBtc));
        snapshot.put("totalValueUsdt", String.format("%.2f", currentVal));
        snapshot.put("pnlUsdt", String.format("%.2f", pnl));
        snapshot.put("activeGrids", buyGrids.size() + sellGrids.size());
        snapshot.put("lastTrades", tradeHistory.size() > 5 ? tradeHistory.subList(tradeHistory.size()-5, tradeHistory.size()) : tradeHistory);
        return snapshot;
    }

    @Scheduled(fixedRate = 5000)
    public void runStrategy() {
        BigDecimal realPrice = binanceService.getLatestPrice("BTCUSDT");
        if (realPrice != null) {
            currentPrice = realPrice;
            log.info("[市场实价] BTC/USDT: {}", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        } else {
            simulatePriceMovement();
            log.info("[模拟行情] 无法连接 API，使用模拟价格: {}", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        }

        if (!active) {
            autoAlignGrids();
            active = true;
            return;
        }

        // 自动化：如果市价偏离网格中心超过 2%，自动重新对齐网格
        BigDecimal deviation = currentPrice.subtract(lastGridCenter).abs()
                                .divide(lastGridCenter, 4, BigDecimal.ROUND_HALF_UP);
        if (deviation.compareTo(new BigDecimal("0.02")) > 0) {
            log.info("检测到价格偏离超过 2%，正在自动重新对齐网格...");
            autoAlignGrids();
        }

        executeGridLongLogic();
    }

    private void autoAlignGrids() {
        log.info("--- 自动化网格对齐: {} ---", currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        lastGridCenter = currentPrice;
        setupLongGrids(currentPrice);
    }

    private void setupLongGrids(BigDecimal centerPrice) {
        buyGrids.clear();
        sellGrids.clear();
        for (int i = 1; i <= 5; i++) {
            BigDecimal buyLevel = centerPrice.subtract(centerPrice.multiply(new BigDecimal(0.005 * i)));
            BigDecimal sellLevel = centerPrice.add(centerPrice.multiply(new BigDecimal(0.005 * i)));
            buyGrids.add(buyLevel);
            sellGrids.add(sellLevel);
        }
        log.info("网格重置完成。中轴: {}", centerPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    private void simulatePriceMovement() {
        // 模拟波动
        double rand = Math.random();
        double change;
        if (rand > 0.95) {
            change = (Math.random() - 0.3) * 1000; 
        } else {
            change = (Math.random() - 0.5) * 200;
        }
        currentPrice = currentPrice.add(new BigDecimal(change));
        if (currentPrice.compareTo(BigDecimal.ZERO) < 0) currentPrice = new BigDecimal("10000");
    }

    private void executeGridLongLogic() {
        buyGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) <= 0) {
                double amountToBuy = 0.01; 
                double cost = grid.doubleValue() * amountToBuy;
                if (balanceUsdt >= cost) {
                    balanceUsdt -= cost;
                    balanceBtc += amountToBuy;
                    String msg = String.format("自动成交[买入] @ %s", grid.setScale(2, BigDecimal.ROUND_HALF_UP));
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
                String msg = String.format("自动成交[卖出] @ %s", grid.setScale(2, BigDecimal.ROUND_HALF_UP));
                tradeHistory.add(msg);
                log.info(msg);
                buyGrids.add(grid.subtract(grid.multiply(new BigDecimal("0.005"))));
                return true;
            }
            return false;
        });
    }
}
