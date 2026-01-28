package bot.quant.grid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GridBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(GridBotApplication.class, args);
    }
}
