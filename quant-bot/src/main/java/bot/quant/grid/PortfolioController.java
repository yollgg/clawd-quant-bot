package bot.quant.grid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class PortfolioController {

    @Autowired
    private GridStrategyEngine strategyEngine;

    @GetMapping("/portfolio")
    public Map<String, Object> getPortfolio() {
        return strategyEngine.getPortfolioSnapshot();
    }
}
