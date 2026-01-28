package bot.quant.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class GridStrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(GridStrategyEngine.class);

    @Autowired
    private MarketAnalyzer analyzer;

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private TradeRecordRepository tradeRepository;

    @Autowired
    private PortfolioStateRepository portfolioRepository;

    private BigDecimal currentPrice = BigDecimal.ZERO;
    private BigDecimal lastGridCenter = BigDecimal.ZERO;
    private boolean active = true;
    private final List<BigDecimal> buyGrids = new ArrayList<>();
    private final List<BigDecimal> sellGrids = new ArrayList<>();

    private PortfolioState state;
    private final double leverage = 5.0; 
    private final double stopLossThreshold = 0.15;

    @PostConstruct
    public void init() {
        // 从数据库加载状态，如果没有则创建
        state = portfolioRepository.findById("MAIN").orElseGet(() -> {
            PortfolioState newState = new PortfolioState();
            return portfolioRepository.save(newState);
        });
        log.info("量化引擎启动。初始状态: {} USDT, {} BTC", state.getBalanceUsdt(), state.getBalanceBtc());
    }

    @Scheduled(fixedRate = 5000)
    public void tick() {
        if (!active) return;

        BigDecimal price = binanceService.getLatestPrice("BTCUSDT");
        if (price == null) return;
        this.currentPrice = price;

        if (lastGridCenter.equals(BigDecimal.ZERO)) {
            refreshMarketContext();
        }

        checkRiskStatus();
        processGrids();
    }

    private void refreshMarketContext() {
        double currentVolatility = analyzer.calculateVolatility("BTCUSDT", "4h", 14);
        this.lastGridCenter = currentPrice;
        
        buyGrids.clear();
        sellGrids.clear();
        
        double baseStep = Math.max(0.002, currentVolatility / 5.0);
        for (int i = 1; i <= 5; i++) {
            buyGrids.add(currentPrice.subtract(currentPrice.multiply(BigDecimal.valueOf(baseStep * i))));
            sellGrids.add(currentPrice.add(currentPrice.multiply(BigDecimal.valueOf(baseStep * i))));
        }
        log.info("网格重组 [DB已同步]：中心={}, 步长={}, 波动率={}", currentPrice, baseStep, currentVolatility);
    }

    private void checkRiskStatus() {
        double currentTotal = state.getBalanceUsdt() + (state.getBalanceBtc() * currentPrice.doubleValue());
        double drawDown = (state.getInitialInvestment() - currentTotal) / state.getInitialInvestment();
        if (drawDown > stopLossThreshold) {
            log.error("!!! 触发止损 !!! 停止运行。");
            this.active = false;
        }
    }

    private void processGrids() {
        buyGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) <= 0) {
                executeTrade("BUY", grid);
                return true;
            }
            return false;
        });

        sellGrids.removeIf(grid -> {
            if (currentPrice.compareTo(grid) >= 0 && state.getBalanceBtc() > 0) {
                executeTrade("SELL", grid);
                return true;
            }
            return false;
        });
    }

    private void executeTrade(String type, BigDecimal price) {
        double margin = state.getBalanceUsdt() * 0.15;
        if ("BUY".equals(type) && state.getBalanceUsdt() > margin) {
            double amount = (margin * leverage) / price.doubleValue();
            state.setBalanceUsdt(state.getBalanceUsdt() - margin);
            state.setBalanceBtc(state.getBalanceBtc() + amount);
            saveAndRecord("合约开多", price, amount);
        } else if ("SELL".equals(type) && state.getBalanceBtc() > 0) {
            double amountToSell = state.getBalanceBtc() / (double) Math.max(1, sellGrids.size());
            // 简化合约盈亏计算
            double pnl = (price.doubleValue() - lastGridCenter.doubleValue()) * amountToSell * leverage;
            state.setBalanceUsdt(state.getBalanceUsdt() + margin + pnl);
            state.setBalanceBtc(state.getBalanceBtc() - amountToSell);
            saveAndRecord("合约平多", price, amountToSell);
        }
    }

    private void saveAndRecord(String action, BigDecimal price, double amount) {
        // 1. 保存持仓状态到数据库
        portfolioRepository.save(state);

        // 2. 写入成交记录到数据库
        TradeRecord record = new TradeRecord();
        record.setType(action);
        record.setPrice(price);
        record.setAmount(amount);
        record.setBalanceUsdtAfter(state.getBalanceUsdt());
        record.setTimestamp(LocalDateTime.now());
        tradeRepository.save(record);

        log.info("DB成交同步: {} @ {}, 余额: {} USDT", action, price, state.getBalanceUsdt());
    }

    public Map<String, Object> getPortfolioSnapshot() {
        Map<String, Object> map = new HashMap<>();
        double total = state.getBalanceUsdt() + (state.getBalanceBtc() * currentPrice.doubleValue());
        map.put("price", currentPrice.setScale(2, RoundingMode.HALF_UP));
        map.put("totalValue", String.format("%.2f", total));
        map.put("usdt", String.format("%.2f", state.getBalanceUsdt()));
        map.put("btc", String.format("%.6f", state.getBalanceBtc()));
        map.put("pnl", String.format("%.2f%%", (total - state.getInitialInvestment())));
        
        // 从数据库获取最后 5 条记录
        List<String> lastTrades = tradeRepository.findAll().stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(5)
                .map(r -> String.format("%s @ %s (%s)", r.getType(), r.getPrice(), r.getTimestamp()))
                .collect(Collectors.toList());
        map.put("lastTrades", lastTrades);
        return map;
    }
}
