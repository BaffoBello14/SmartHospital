package it.unipi.iot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Coordinator implements MqttCallback {
    private static final String MQTT_BROKER = "tcp://127.0.0.1:1883";
    private static final String OXYGEN_TOPIC = "ossigeno";
    private static final String HEARTBEAT_TOPIC = "battito";
    private static final String TEMPERATURE_TOPIC = "temperatura";

    private MqttClient mqttClient;
    private CoapServer coapServer;

    // Coap resource definition
    public static class MyCoapResource extends CoapResource 
    {
        public MyCoapResource(String name)
        {
            super(name);
        }

        @Override
        public void handleGET(CoapExchange exchange) 
        {
            exchange.respond("NO GET HANDLER\n");
        }

        @Override
        public void handlePOST(CoapExchange exchange) 
        {
            System.out.println("STARTING HANDLE POST\n");
            // Try to register new actuator

            String s = new String(exchange.getRequestPayload());
            
            JSONObject obj;
            JSONParser parser = new JSONParser();

            System.out.println("TRYNA PARSE STRING: "+s);
            
            try 
            {
                obj = (JSONObject) parser.parse(s);
            } 
            catch (ParseException e) 
            {
                throw new RuntimeException(e);
            }

            Response response;
            InetAddress address = exchange.getSourceAddress();

            System.out.println("TRYNA REGISTER THE ACTUATOR WITH IP: "+address);
            
            int success = -1;
            
            try (Connection connection = DB.getDb())
            {
                System.out.println("CONNESSIONE STABILITA\n");
                try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO" + DATABASE_NAME +" (ip, type, status) VALUES(?,?,?);")) 
                {
                    // 1 = Inet
                    ps.setString(1, address.substring(1)); //substring(1)
                    // 2 = Tipo attuatore
                    ps.setString(2, (String) obj.get("type"));
                    // Actuator start status
                    // Start status = off = boolean 0
                    ps.setInt(3, 0);
                    
                    System.out.println("PREPARED STATEMENT CON INDIRIZZO: "+address.substring(1)+" TIPO ATTUATORE: "+actuatorType.toString()+" STATO: "+status.toString());
                    System.out.println("CERCO DI ESEGUIRE LA UPDATE\n");
                    
                    // Tryna execute UPDATE
                    ps.executeUpdate();
                    // Ritorna il numero di righe coinvolte
                    success = ps.getUpdateCount();
                    if(success>0)
                    {
                        System.out.println("UPDATE ESEGUITA\n");
                        System.out.println("RIGHE COINVOLTE: "+success+"\n");
                        response = new Response(CoAP.ResponseCode.CREATED);
                    }
                    else
                    {
                        System.out.println("ERRORE NELLA UPDATE UPDATE\n SUCCESS="+success);
                        response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            
            // Return number of raw coinvolte
            //return ps.getUpdateCount();
            exchange.respond(response);
        }
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
        try 
        {
            MqttClient mqttClient = new MqttClient(MQTT_BROKER, clientId);
            mqttClient.setCallback(this);
            mqttClient.connect();
            return mqttClient;
        } 
        catch (MqttException e) 
        {
            e.printStackTrace();
        }
        return null;
    }

    private void subscribeToTopics() {
        try 
        {
            this.mqttClient.subscribe(OXYGEN_TOPIC);
            this.mqttClient.subscribe(HEARTBEAT_TOPIC);
            this.mqttClient.subscribe(TEMPERATURE_TOPIC);
        } 
        catch (MqttException e) 
        {
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

    public void messageArrived(String topic, MqttMessage message) throws Exception 
    {
        System.out.println("MESSAGE ARRIVED - Topic: " + topic + ", Payload: " + message);

        if (topic.equals(OXYGEN_TOPIC) || topic.equals(HEARTBEAT_TOPIC) || topic.equals(TEMPERATURE_TOPIC)) 
        {
            try 
            {
                // payload retrieve
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
                String sensorId = jsonPayload.get("id").getAsString();

                if (topic.equals(OXYGEN_TOPIC)) 
                {
                    float oxygenLevel = jsonPayload.get("value").getAsFloat();
                    try (Connection connection = DB.getDb()) 
                    {
                        String sql = "INSERT INTO oxygen_sensor (id, value) VALUES ('" + sensorId + "', " + oxygenLevel + ")";
                        try (Statement statement = connection.createStatement()) 
                        {
                            statement.executeUpdate(sql);
                        }
                    } 
                    catch (SQLException e) 
                    {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } 
                else if (topic.equals(HEARTBEAT_TOPIC)) 
                {
                    int heartbeat = jsonPayload.get("value").getAsInt();
                    try (Connection connection = DB.getDb()) 
                    {
                        String sql = "INSERT INTO heartbeat_sensor (id, value) VALUES ('" + sensorId + "', " + heartbeat + ")";
                        try (Statement statement = connection.createStatement()) 
                        {
                            statement.executeUpdate(sql);
                        }
                    } 
                    catch (SQLException e) 
                    {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } 
                else if (topic.equals(TEMPERATURE_TOPIC)) 
                {
                    float temperature = jsonPayload.get("value").getAsFloat();
                    try (Connection connection = DB.getDb()) 
                    {
                        String sql = "INSERT INTO temperature_sensor (id, value) VALUES ('" + sensorId + "', " + temperature + ")";
                        try (Statement statement = connection.createStatement()) 
                        {
                            statement.executeUpdate(sql);
                        }
                    } 
                    catch (SQLException e) 
                    {
                        System.err.println("Error inserting data into the database: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } 
            catch (Exception e) 
            {
                System.err.println("Error parsing message payload: " + e.getMessage());
                e.printStackTrace();
            }
        } 
        else 
        {
            System.out.println("UNKNOWN TOPIC: " + topic);
        }
    }

    public static void main(String[] args) 
    {
        // Adding DB entry point
        DB.getDb();
        Coordinator coordinator = new Coordinator();
    }
}