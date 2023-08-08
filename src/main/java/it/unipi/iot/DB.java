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

    private DB() {
        // Private constructor to prevent direct instantiation
    }

    public static Connection createDbConnection() {
        Connection connection = null;
        try {
            String jdbcUrl = String.format(JDBC_URL, HOST, PORT, DATABASE_NAME);
            Properties properties = new Properties();
            properties.put("user", USERNAME);
            properties.put("password", PASSWORD);
            properties.put("zeroDateTimeBehavior", "CONVERT_TO_NULL");
            properties.put("serverTimeZone", "CET");

            connection = DriverManager.getConnection(jdbcUrl, properties);
        } catch (SQLException e) {
            System.err.println("Error when creating a new DB connection: " + e.getMessage());
            e.printStackTrace();
        }

        return connection;
    }

    public static void insertSensorData(String tableName, String id, float value) {
        String query = "INSERT INTO " + tableName + " (id, value) VALUES (?, ?)";
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, id);
            ps.setFloat(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error while inserting sensor data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void replaceActuator(String ip, String type) {
        String query = "REPLACE INTO .actuator (ip, type) VALUES (?, ?)";
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, ip);
            ps.setString(2, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error while replacing actuator: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void deleteActuator(String ip) {
        String query = "DELETE FROM actuator WHERE ip = ?";
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error while deleting actuator: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String retrieveActuatorIP(String type) {
        String ip = "";
        String query = "SELECT ip FROM actuator WHERE type = ? AND status = 0 LIMIT 1";
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ip = rs.getString("ip");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while retrieving actuator IP: " + e.getMessage());
            e.printStackTrace();
        }

        return ip;
    }

    public static void updateActuatorStatus(String ip, String patientId, boolean isActive) {
        String query = "UPDATE actuator SET status = ? WHERE ip = ?";
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, isActive ? Integer.parseInt(patientId) : 0);
            ps.setString(2, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error while updating actuator status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<Float> retrieveSensorData(String patientId) {
        List<String> sensorTypes = Arrays.asList("oxygen_sensor", "troponin_sensor", "cardio_sensor");
        List<Float> retrievedValues = new ArrayList<>();

        for (String sensorType : sensorTypes) {
            String query = "SELECT AVG(value) AS media_valore " +
                    "FROM ( " +
                    "SELECT value FROM " + sensorType + " WHERE id = ? " +
                    "ORDER BY timestamp DESC LIMIT 5) as t";
            try (Connection connection = createDbConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, sensorType.charAt(0) + patientId);  // Set the sensor id
                try (ResultSet result = ps.executeQuery()) {
                    if (result.next()) {
                        float sensorValue = result.getFloat("media_valore");
                        retrievedValues.add(sensorValue);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error while retrieving sensor data for patient " + patientId + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        return retrievedValues;
    }

    public static HashMap<String, String[]> retrieveActiveActuators() {
        HashMap<String, String[]> activeActuators = new HashMap<>();
        String query = "SELECT ip, type, status FROM actuator WHERE status <> 0";
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ip = rs.getString("ip");
                String type = rs.getString("type");
                String patientId = String.valueOf(rs.getInt("status"));

                if (Actuator_Client.checkActuatorStatus(ip, type)) {
                    String[] actuatorIps = activeActuators.getOrDefault(patientId, new String[3]);
                    actuatorIps[type.equals("mask") ? 0 : type.equals("medicine") ? 1 : 2] = ip;
                    activeActuators.put(patientId, actuatorIps);
                } else {
                    deleteActuator(ip);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while retrieving active actuators: " + e.getMessage());
            e.printStackTrace();
        }

        return activeActuators;
    }

    public static List<String> queryActuatorsWithStatus(int status) throws SQLException {
        List<String> actuatorIps = new ArrayList<>();
    
        // Query to get all actuators with the given status
        String query = "SELECT ip FROM actuator WHERE status = ?";
    
        try (Connection connection = createDbConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
    
            // Set the status parameter in the query
            ps.setInt(1, status);
    
            try (ResultSet rs = ps.executeQuery()) {
                // Loop through the result set and add each IP to the list
                while (rs.next()) {
                    String ip = rs.getString("ip");
                    actuatorIps.add(ip);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while querying actuators with status " + status + ": " + e.getMessage());
            throw e;  // Re-throw the exception to be handled by the caller
        }
    
        return actuatorIps;
    }
    
}