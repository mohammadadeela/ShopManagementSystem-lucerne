package com.lucerne.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        Path external = Path.of("config", "database.properties");
        try (InputStream input = Files.exists(external)
                ? Files.newInputStream(external)
                : DatabaseConfig.class.getResourceAsStream("/config/database.properties")) {
            if (input == null) throw new IllegalStateException("database.properties was not found");
            PROPERTIES.load(input);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private DatabaseConfig() { }

    public static String jdbcUrl() {
        return "jdbc:mysql://" + get("db.host") + ":" + get("db.port") + "/" + get("db.name")
                + "?useSSL=" + get("db.useSSL")
                + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"
                + "&connectTimeout=" + get("db.connectTimeout") + "&rewriteBatchedStatements=true";
    }

    public static String username() { return get("db.user"); }
    public static String password() { return get("db.password"); }
    public static String databaseName() { return get("db.name"); }
    public static String host() { return get("db.host"); }
    public static String port() { return get("db.port"); }

    private static String get(String key) {
        String environmentKey = key.toUpperCase().replace('.', '_');
        return System.getenv().getOrDefault(environmentKey, PROPERTIES.getProperty(key, "")).trim();
    }
}
