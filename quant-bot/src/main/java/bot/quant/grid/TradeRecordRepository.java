package bot.quant.grid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRecordRepository extends JpaRepository<TradeRecord, Long> {
}
