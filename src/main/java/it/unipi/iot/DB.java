package it.unipi.iot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        try {
            if (db == null || db.isClosed()) {
                String jdbcUrl = String.format(JDBC_URL, HOST, PORT, DATABASE_NAME);
                Properties properties = new Properties();
                properties.put("user", USERNAME);
                properties.put("password", PASSWORD);
                properties.put("zeroDateTimeBehavior", "CONVERT_TO_NULL");
                properties.put("serverTimeZone", "CET");
    
                db = DriverManager.getConnection(jdbcUrl, properties);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    
        return db;
    }

    public static void insertSensorData(String tableName, String id, float value) throws SQLException {
        String query = "INSERT INTO " + tableName + " (id, value) VALUES (?, ?)";
        executeUpdate(query, id, Float.toString(value));
    }

    public static void replaceActuator(String ip, String type) throws SQLException {
        String query = "REPLACE INTO .actuator (ip, type) VALUES (?, ?)";
        executeUpdate(query, ip, type);
    }

    public static void deleteActuator(String ip) throws SQLException {
        String query = "DELETE FROM actuator WHERE ip = ?";
        executeUpdate(query, ip);
    }

    public static String retrieveActuatorIP(String type) throws SQLException {
        db = getDb();
        String ip = "";
        PreparedStatement ps = db.prepareStatement("SELECT ip FROM actuator WHERE type = ? AND status = 0 LIMIT 1");
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) 
        {
            ip = rs.getString("ip");
        }
        rs.close();
        return ip;
    }

    public static void updateActuatorStatus(String ip, String patientId, boolean isActive) throws SQLException {
        db = getDb();
        PreparedStatement ps = db.prepareStatement("UPDATE actuator SET status = ? WHERE ip = ?");
        ps.setInt(1, isActive ? Integer.parseInt(patientId) : 0);
        ps.setString(2, ip);
        ps.executeUpdate();
    }

    private static void executeUpdate(String query, String... params) throws SQLException {
        try (Connection connection = getDb();
             PreparedStatement ps = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    /*public static Map<String, Float> retrieveSensorData(String sensorType) throws SQLException {
        db = getDb();
        
        HashMap<String, Float> retrieved = new HashMap<>();

        String query = "SELECT id, AVG(value) AS media_valore "
                + "FROM ( "
                + "  SELECT "
                + "    t1.id, "
                + "    t1.value "
                + "  FROM "
                + "    (SELECT "
                + "      @row_num := IF(@prev_value = id, @row_num + 1, 1) AS rn, "
                + "      value, "
                + "      @prev_value := id AS id "
                + "    FROM "
                + "      " + sensorType + " t, "
                + "      (SELECT @row_num := 1) x, "
                + "      (SELECT @prev_value := '') y "
                + "    ORDER BY "
                + "      id, "
                + "      timestamp DESC "
                + "    ) t1 "
                + "  WHERE t1.rn <= 5 "
                + ") t2 "
                + "GROUP BY id";
        
        try (PreparedStatement ps = db.prepareStatement(query)) {
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    float sensorValue = result.getFloat("media_valore");
                    String id = result.getString("id");
                    retrieved.put(id, sensorValue);
                }
            }
        }
        
        return retrieved;
    }*/

    public static List<Float> retrieveSensorData(String patientId) throws SQLException {
        db = getDb();
    
        List<String> sensorTypes = Arrays.asList("oxygen_sensor", "troponin_sensor", "cardio_sensor");
        List<Float> retrievedValues = new ArrayList<>();
    
        for (String sensorType : sensorTypes) {
            String query = "SELECT AVG(value) AS media_valore " +
                    "FROM ( " +
                    "SELECT value FROM " + sensorType + " WHERE id = ? " +  // sostituisci con l'id corrispondente
                    "ORDER BY timestamp DESC LIMIT 5) as t";
    
            try (PreparedStatement ps = db.prepareStatement(query)) {
                ps.setString(1, sensorType.charAt(0) + patientId);  // Imposta l'id del sensore
                try (ResultSet result = ps.executeQuery()) {
                    if (result.next()) {
                        if (result.getObject("media_valore") != null) {
                            float sensorValue = result.getFloat("media_valore");
                            retrievedValues.add(sensorValue);
                        } else {
                            return null;  // Nessun dato valido per questo paziente
                        }
                    } else {
                        return null;  // Nessun dato per questo paziente
                    }
                }
            }
        }
    
        return retrievedValues;
    }

    public static HashMap<String, String[]> retrieveActiveActuators() throws SQLException {
        db = getDb();
    
        HashMap<String, String[]> activeActuators = new HashMap<>();
        PreparedStatement ps = db.prepareStatement("SELECT ip, type, status FROM actuator WHERE status <> 0");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String ip = rs.getString("ip");
            String type = rs.getString("type");
            String patientId = String.valueOf(rs.getInt("status"));
    
            if (Actuator_Client.checkActuatorStatus(ip, type)) {
                String[] actuatorIps = activeActuators.getOrDefault(patientId, new String[3]);
                for (int i = 0; i < actuatorIps.length; i++) {
                    if (actuatorIps[i] == null) {
                        actuatorIps[i] = ""; // initialize with empty string if null
                    }
                }
                actuatorIps[type.equals("mask") ? 0 : type.equals("medicine") ? 1 : 2] = ip;
    
                activeActuators.put(patientId, actuatorIps);
            } else {
                // Remove the actuator from the database if it is not responding
                PreparedStatement del_ps = db.prepareStatement("DELETE FROM actuator WHERE ip = ?");
                del_ps.setString(1, ip);
                del_ps.executeUpdate();
            }
        }
        rs.close();
        return activeActuators;
    }
}