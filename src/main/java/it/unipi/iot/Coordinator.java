package it.unipi.iot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.paho.client.mqttv3.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class Coordinator extends CoapServer implements MqttCallback {

    private static final String MQTT_BROKER = "tcp://[::1]:1883";
    private static final String OXYGEN_TOPIC = "ossigeno";
    private static final String CARDIO_TOPIC = "cardio";
    private static final String TROPONIN_TOPIC = "tropomina";

    private MqttClient mqttClient;
    private CoapServer coapServer;

    public static class MyCoapResource extends CoapResource {
        public MyCoapResource(String name) {
            super(name);
        }

        @Override
        public void handlePOST(CoapExchange exchange) 
        {
            System.out.println("STARTING HANDLE POST\n");

            // Extracting request information
            String s = new String(exchange.getRequestPayload());
            JSONObject obj;
            JSONParser parser = new JSONParser();
            
            try 
            {
                obj = (JSONObject) parser.parse(s);
            } 
            catch (ParseException e) 
            {
                throw new RuntimeException(e);
            }

            InetAddress address = exchange.getSourceAddress();
            String ip = address.toString().substring(1); // Removes initial slash
            String type = (String) obj.get("type");

            // Using DB class method to register actuator
            try 
            {
                DB.replaceActuator(ip, type, "ON");
                System.out.println("Actuator registered successfully.");
                exchange.respond(ResponseCode.CREATED);
            } 
            catch (SQLException e) 
            {
                e.printStackTrace();
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Override
        public void handleDELETE(CoapExchange exchange) 
        {
            System.out.println("STARTING HANDLE DELETE\n");

            // Extracting request information
            InetAddress address = exchange.getSourceAddress();
            String ip = address.toString().substring(1); // Removes initial slash

            // Using DB class method to delete actuator
            try 
            {
                DB.deleteActuator(ip);
                System.out.println("Actuator deleted successfully.");
                exchange.respond(ResponseCode.DELETED);
            } 
            catch (SQLException e) 
            {
                e.printStackTrace();
                exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        }

    }

    public Coordinator() {
        this.coapServer = new CoapServer(5683);
        this.coapServer.add(new MyCoapResource("registration"));
        this.coapServer.start();

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
            this.mqttClient.subscribe(CARDIO_TOPIC);
            this.mqttClient.subscribe(TROPONIN_TOPIC);
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

        if (topic.equals(OXYGEN_TOPIC) || topic.equals(CARDIO_TOPIC) || topic.equals(TROPONIN_TOPIC)) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
            String sensorId = jsonPayload.get("id").getAsString();
            float value = jsonPayload.get("value").getAsFloat();

            String tableName = "";
            switch (topic) {
                case OXYGEN_TOPIC:
                    tableName = "oxygen_sensor";
                    break;
                case CARDIO_TOPIC:
                    tableName = "cardio_sensor";
                    value = jsonPayload.get("value").getAsInt();
                    break;
                case TROPONIN_TOPIC:
                    tableName = "troponin_sensor";
                    break;
            }

            try {
                DB.insertSensorData(tableName, sensorId, value);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Coordinator coordinator = new Coordinator();
    }
}
