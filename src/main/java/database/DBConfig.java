package database;


import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Data
public final class DBConfig {

    private String host;
    private int port;
    private String name;
    private String username;
    private String password;

    private int maximumPoolSize;
    private int minimumIdle;
    private long connectionTimeout;
    private long idleTimeout;
    private long maxLifetime;

    private DBConfig() {
        load();
    }

    private static final class InstanceHolder {
        private static final DBConfig instance = new DBConfig();
    }

    public static DBConfig gI() {
        return InstanceHolder.instance;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Path path = Paths.get("data/config/database.yaml");

        if (!Files.exists(path)) {
            throw new RuntimeException("database.yaml not found in root directory");
        }

        try (InputStream is = Files.newInputStream(path)) {

            Load load = new Load(LoadSettings.builder().build());
            Map<String, Object> root = (Map<String, Object>) load.loadFromInputStream(is);
            Map<String, Object> db = (Map<String, Object>) root.get("database");
            Map<String, Object> pool = (Map<String, Object>) db.get("pool");

            host = (String) db.get("host");
            port = ((Number) db.get("port")).intValue();
            name = (String) db.get("name");
            username = (String) db.get("username");
            password = (String) db.get("password");

            maximumPoolSize = ((Number) pool.get("maximumPoolSize")).intValue();
            minimumIdle = ((Number) pool.get("minimumIdle")).intValue();
            connectionTimeout = ((Number) pool.get("connectionTimeout")).longValue();
            idleTimeout = ((Number) pool.get("idleTimeout")).longValue();
            maxLifetime = ((Number) pool.get("maxLifetime")).longValue();

            log.info("Database configuration loaded");

        } catch (Exception e) {
            log.error("Failed to load database.yaml", e);
            throw new RuntimeException(e);
        }
    }


}
