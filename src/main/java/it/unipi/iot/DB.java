package it.unipi.iot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DB {
    private static final String JDBC_URL = "jdbc:mysql://%s:%d/%s";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "iot22-23";
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE_NAME = "iot";

    private static Connection db = null;

    private DB() {
        // Costruttore privato per evitare l'istanziazione diretta
    }

    public static Connection getDb() {
        if (db == null) {
            String jdbcUrl = String.format(JDBC_URL, HOST, PORT, DATABASE_NAME);
            Properties properties = new Properties();
            properties.put("user", USERNAME);
            properties.put("password", PASSWORD);
            properties.put("zeroDateTimeBehavior", "CONVERT_TO_NULL");
            properties.put("serverTimeZone", "CET");

            try {
                db = DriverManager.getConnection(jdbcUrl, properties);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return db;
    }
}
