import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DBPool {

    private static HikariDataSource ds;

    static {
        try {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl("jdbc:mysql://localhost:3306/litecore_db");
            config.setUsername("root");
            config.setPassword("10863geo*"); // CHANGE IF YOU HAVE PASSWORD

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);

            config.setConnectionTimeout(3000);     // 3 sec max wait
            config.setIdleTimeout(60000);          // keep idle for 1 min
            config.setMaxLifetime(600000);         // 10 min recycle

            ds = new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}


