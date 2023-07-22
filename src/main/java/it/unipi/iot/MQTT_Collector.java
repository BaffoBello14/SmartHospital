package it.unipi.iot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MQTT_Collector implements MqttCallback {

    private static final String MQTT_BROKER = "tcp://127.0.0.1:1883";
    private static final String OXYGEN_TOPIC = "ossigeno";
    private static final String HEARTBEAT_TOPIC = "battito";
    private static final String TEMPERATURE_TOPIC = "temperatura";
    private static final int HEARTBEAT_THRESHOLD = 70;
    private static final int OXYGEN_THRESHOLD = 80;
    private static final double TEMPERATURE_THRESHOLD = 35.5;

    private static MqttClient connectToBroker() {
        String clientId = "tcp://127.0.0.1:1883";
        try 
        {
            MqttClient mqttClient = new MqttClient(MQTT_BROKER, clientId);
            mqttClient.setCallback(new MyClient());
            mqttClient.connect();
            return mqttClient;
        } 
        catch (MqttException e) 
        {
            e.printStackTrace();
        }
        return null;
    }

    private static void subscribeToTopics(MqttClient mqttClient) {
        try {
            mqttClient.subscribe(OXYGEN_TOPIC);
            mqttClient.subscribe(HEARTBEAT_TOPIC);
            mqttClient.subscribe(TEMPERATURE_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleOxygenLevel(String sensorId, float oxygenLevel) {
        // Logic to handle oxygen level
        // Determine pill dispenser status based on oxygen level
        if (oxygenLevel < OXYGEN_THRESHOLD) {
            setPillDispenserStatus(sensorId, "APERTA");
        } else {
            setPillDispenserStatus(sensorId, "CHIUSA");
        }
    }

    private void handleHeartbeat(String sensorId, int heartbeat) {
        // Logic to handle heartbeat
        // Determine pill dispenser status based on heartbeat
        if (heartbeat > HEARTBEAT_THRESHOLD) {
            setPillDispenserStatus(sensorId, "APERTA");
        } else {
            setPillDispenserStatus(sensorId, "CHIUSA");
        }
    }

    private void handleTemperature(String sensorId, double temperature) {
        // Logic to handle temperature
        // Determine pill dispenser status based on temperature
        if (temperature > TEMPERATURE_THRESHOLD) {
            setPillDispenserStatus(sensorId, "APERTA");
        } else {
            setPillDispenserStatus(sensorId, "CHIUSA");
        }
    }

    private void setPillDispenserStatus(String sensorId, String status) {
        // Logic to set the pill dispenser status
        // You can update a variable, call a method, or perform any necessary actions
        System.out.println("SENSOR ID: " + sensorId + ", SCATOLA: " + status);
        // From this point, oxygen and heartbeat must decrease
    }

    public void connectionLost(Throwable throwable) {
        System.out.println("CONNECTION LOST");
        throwable.printStackTrace();
    }

    public void deliveryComplete(IMqttDeliveryToken token) 
    {
        // Not used in this example
        System.out.print("DELIVERY COMPLETATA!\n");
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("MESSAGE ARRIVED - Topic: " + topic + ", Payload: " + message);

        if (topic.equals(OXYGEN_TOPIC) || topic.equals(HEARTBEAT_TOPIC) || topic.equals(TEMPERATURE_TOPIC)) {
            try {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
                String sensorId = jsonPayload.get("id").getAsString();

                if (topic.equals(OXYGEN_TOPIC)) {
                    float oxygenLevel = jsonPayload.get("value").getAsFloat();
                    handleOxygenLevel(sensorId, oxygenLevel);
                    
                    // Insert data into the database
                    try (Connection connection = DB.getDb()) {
                        String sql = "INSERT INTO oxygen_sensor (sensor_id, value) VALUES ('" + sensorId + "', " + oxygenLevel + ")";
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate(sql);
                        }
                    } catch (SQLException e) {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (topic.equals(HEARTBEAT_TOPIC)) {
                    int heartbeat = jsonPayload.get("value").getAsInt();
                    handleHeartbeat(sensorId, heartbeat);
                    
                    // Insert data into the database
                    try (Connection connection = DB.getDb()) {
                        String sql = "INSERT INTO heartbeat_sensor (sensor_id, value) VALUES ('" + sensorId + "', " + heartbeat + ")";
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate(sql);
                        }
                    } catch (SQLException e) {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (topic.equals(TEMPERATURE_TOPIC)) {
                    double temperature = jsonPayload.get("value").getAsDouble();
                    handleTemperature(sensorId, temperature);
                    
                    // Insert data into the database
                    try (Connection connection = DB.getDb()) {
                        String sql = "INSERT INTO temperature_sensor (sensor_id, value) VALUES ('" + sensorId + "', " + temperature + ")";
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate(sql);
                        }
                    } catch (SQLException e) {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing message payload: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("UNKNOWN TOPIC: " + topic);
        }
    }


    public static void main(String[] args) 
    {
        // Create Singleton DatabaseConnection instance
        DB.getDb(); // Questo assicura che la connessione al database venga stabilita una volta sola.

        // Instance creation
        MqttClient mqttClient = connectToBroker();

        // Mqtt connection added to topic
        subscribeToTopics(mqttClient);
    }
}
