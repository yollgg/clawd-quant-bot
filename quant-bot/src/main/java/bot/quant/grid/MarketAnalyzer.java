package bot.quant.grid;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(MarketAnalyzer.class);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取 K 线数据并计算波动率 (ATR 简化版)
     */
    public double calculateVolatility(String symbol, String interval, int limit) {
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode klines = mapper.readTree(response.body().string());
                double totalRange = 0;
                for (JsonNode k : klines) {
                    double high = k.get(2).asDouble();
                    double low = k.get(3).asDouble();
                    totalRange += (high - low);
                }
                double avgRange = totalRange / klines.size();
                double currentPrice = klines.get(klines.size() - 1).get(4).asDouble();
                return avgRange / currentPrice; // 返回百分比波动率
            }
        } catch (Exception e) {
            log.error("分析波动率失败: {}", e.getMessage());
        }
        return 0.02; // 默认 2%
    }

    /**
     * 模拟获取全球宏观资金流向与大户数据
     */
    public String getGlobalCapitalFlow() {
        // 实际可对接 CoinGlass 或 OKLink API
        // 返回建议方向: BULLISH, BEARISH, NEUTRAL
        return "BULLISH"; 
    }
}
