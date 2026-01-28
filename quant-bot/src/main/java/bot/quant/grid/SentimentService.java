package bot.quant.grid;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SentimentService {
    private static final Logger log = LoggerFactory.getLogger(SentimentService.class);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 对接外部真实舆情 API (Fear & Greed Index)
     * 同时也预留了 AI 情感分析接口
     */
    public double getFearAndGreedScore() {
        Request request = new Request.Builder()
                .url("https://api.alternative.me/fng/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode root = mapper.readTree(response.body().string());
                int value = root.get("data").get(0).get("value").asInt();
                log.info("实时恐惧贪婪指数: {}", value);
                return value / 100.0; // 归一化为 0.0 - 1.0
            }
        } catch (Exception e) {
            log.error("舆情采集异常: {}", e.getMessage());
        }
        return 0.5; // 默认中性
    }
}
