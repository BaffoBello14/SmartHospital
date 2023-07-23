package it.unipi.iot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Coordinator implements MqttCallback {
    private static final String MQTT_BROKER = "tcp://127.0.0.1:1883";
    private static final String OXYGEN_TOPIC = "ossigeno";
    private static final String HEARTBEAT_TOPIC = "battito";
    private static final String TEMPERATURE_TOPIC = "temperatura";

    private MqttClient mqttClient;
    private CoapServer coapServer;

    // Definizione di una nuova risorsa CoAP
    public static class MyCoapResource extends CoapResource {
        public MyCoapResource(String name) {
            super(name);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond("Hello, CoAP!");
        }

        // implementa altri metodi come handlePOST, handlePUT ecc. se necessario
    }

    public Coordinator() {
        // Inizializzazione del server CoAP
        this.coapServer = new CoapServer(5683);
        this.coapServer.add(new MyCoapResource("test"));
        this.coapServer.start();

        // Inizializzazione del client MQTT
        this.mqttClient = connectToBroker();
        subscribeToTopics();
    }

    private MqttClient connectToBroker() {
        String clientId = "tcp://127.0.0.1:1883";
        try {
            MqttClient mqttClient = new MqttClient(MQTT_BROKER, clientId);
            mqttClient.setCallback(this);
            mqttClient.connect();
            return mqttClient;
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void subscribeToTopics() {
        try {
            this.mqttClient.subscribe(OXYGEN_TOPIC);
            this.mqttClient.subscribe(HEARTBEAT_TOPIC);
            this.mqttClient.subscribe(TEMPERATURE_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connectionLost(Throwable throwable) {
        System.out.println("CONNECTION LOST");
        throwable.printStackTrace();
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("DELIVERY COMPLETATA!");
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
                    try (Connection connection = DB.getDb()) {
                        String sql = "INSERT INTO oxygen_sensor (id, value) VALUES ('" + sensorId + "', " + oxygenLevel + ")";
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate(sql);
                        }
                    } catch (SQLException e) {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (topic.equals(HEARTBEAT_TOPIC)) {
                    int heartbeat = jsonPayload.get("value").getAsInt();
                    try (Connection connection = DB.getDb()) {
                        String sql = "INSERT INTO heartbeat_sensor (id, value) VALUES ('" + sensorId + "', " + heartbeat + ")";
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate(sql);
                        }
                    } catch (SQLException e) {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (topic.equals(TEMPERATURE_TOPIC)) {
                    float temperature = jsonPayload.get("value").getAsFloat();
                    try (Connection connection = DB.getDb()) {
                        String sql = "INSERT INTO temperature_sensor (id, value) VALUES ('" + sensorId + "', " + temperature + ")";
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
        DB.getDb();
        Coordinator coordinator = new Coordinator();
    }
}
