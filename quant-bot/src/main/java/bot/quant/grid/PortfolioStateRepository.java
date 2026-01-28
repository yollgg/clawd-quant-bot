package bot.quant.grid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioStateRepository extends JpaRepository<PortfolioState, String> {
}
