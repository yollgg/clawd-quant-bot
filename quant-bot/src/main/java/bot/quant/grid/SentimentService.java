package bot.quant.grid;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class SentimentService {
    
    /**
     * 模拟舆论分析逻辑
     * 返回 0.0 到 1.0 的情感波动分。
     * 低于 0.3 代表舆论平稳，适合网格策略。
     */
    public double getSentimentVolatility() {
        // 实际开发中会抓取 Twitter/News API 进行 NLP 分析
        // 这里模拟一个平稳的市场状态
        return 0.15 + (new Random().nextDouble() * 0.1); 
    }

    public boolean isMarketCalm() {
        return getSentimentVolatility() < 0.35;
    }
}
