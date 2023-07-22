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

    private static MqttClient connectToBroker() {
        String clientId = "tcp://127.0.0.1:1883";
        try {
            MqttClient mqttClient = new MqttClient(MQTT_BROKER, clientId);
            mqttClient.setCallback(new MyClient());
            mqttClient.connect();
            return mqttClient;
        } catch (MqttException e) {
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

    public void connectionLost(Throwable throwable) {
        System.out.println("CONNECTION LOST");
        throwable.printStackTrace();
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilizzato in questo esempio
        System.out.print("DELIVERY COMPLETATA!\n");
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // MSG = id=sender_id value=sensor_value
        System.out.println("MESSAGE ARRIVED - Topic: " + topic + ", Payload: " + message);

        // Controllo del nome del topic
        if (topic.equals(OXYGEN_TOPIC) || topic.equals(HEARTBEAT_TOPIC) || topic.equals(TEMPERATURE_TOPIC)) {
            try {
                // Estrazione del payload dai messaggi MQTT
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                // Conversione del payload da String a JsonObject
                JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
                // Estrazione dell'id del sensore dal payload
                String sensorId = jsonPayload.get("id").getAsString();

                if (topic.equals(OXYGEN_TOPIC)) {
                    float oxygenLevel = jsonPayload.get("value").getAsFloat();
                    // Inserimento dei dati nel database
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
                    // Inserimento dei dati nel database
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
                    float temperature = jsonPayload.get("value").getAsFloat();
                    // Inserimento dei dati nel database
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

    public static void main(String[] args) {
        // Creazione dell'istanza singleton DatabaseConnection
        DB.getDb();

        // Creazione dell'istanza del client MQTT e sottoscrizione ai topic
        MqttClient mqttClient = connectToBroker();
        subscribeToTopics(mqttClient);
    }
}
