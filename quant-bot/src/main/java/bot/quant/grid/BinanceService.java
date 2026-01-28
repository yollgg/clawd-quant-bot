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

@Service
public class BinanceService {
    private static final Logger log = LoggerFactory.getLogger(BinanceService.class);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public BigDecimal getLatestPrice(String symbol) {
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode node = mapper.readTree(response.body().string());
                return new BigDecimal(node.get("price").asText());
            }
        } catch (Exception e) {
            log.error("Failed to fetch price from Binance: {}", e.getMessage());
        }
        return null;
    }
}
