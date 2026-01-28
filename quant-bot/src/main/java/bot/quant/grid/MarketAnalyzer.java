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

    public double calculateVolatility(String symbol, String interval, int limit) {
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode klines = mapper.readTree(response.body().string());
                double sumOfRanges = 0;
                for (JsonNode k : klines) {
                    double high = k.get(2).asDouble();
                    double low = k.get(3).asDouble();
                    sumOfRanges += (high - low);
                }
                double avgRange = sumOfRanges / klines.size();
                double lastClose = klines.get(klines.size() - 1).get(4).asDouble();
                return avgRange / lastClose; 
            }
        } catch (Exception e) {
            log.error("K线数据采集异常: {}", e.getMessage());
        }
        return 0.015; // 兜底 1.5% 波动率
    }

    public String fetchFearAndGreedIndex() {
        // 生产级：对接 alternative.me 的恐惧贪婪指数
        Request request = new Request.Builder().url("https://api.alternative.me/fng/").build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode res = mapper.readTree(response.body().string());
                return res.get("data").get(0).get("value_classification").asText();
            }
        } catch (Exception e) {
            log.error("舆情指数获取失败: {}", e.getMessage());
        }
        return "Neutral";
    }
}
