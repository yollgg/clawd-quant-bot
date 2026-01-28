package bot.quant.grid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class PortfolioState {
    @Id
    private String id = "MAIN";
    private Double balanceUsdt = 100.0;
    private Double balanceBtc = 0.0;
    private Double initialInvestment = 100.0;
}
