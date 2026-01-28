package bot.quant.grid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PortfolioState {
    @Id
    private String id = "MAIN";
    private Double balanceUsdt = 100.0;
    private Double balanceBtc = 0.0;
    private Double initialInvestment = 100.0;

    // Standard Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Double getBalanceUsdt() { return balanceUsdt; }
    public void setBalanceUsdt(Double balanceUsdt) { this.balanceUsdt = balanceUsdt; }
    public Double getBalanceBtc() { return balanceBtc; }
    public void setBalanceBtc(Double balanceBtc) { this.balanceBtc = balanceBtc; }
    public Double getInitialInvestment() { return initialInvestment; }
    public void setInitialInvestment(Double initialInvestment) { this.initialInvestment = initialInvestment; }
}
