package coapclient.unipi.it;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.sql.*;

public class MyClient implements MqttCallback {
	
	private static final String MQTT_BROKER = "tcp://127.0.0.1:1883";
    private static final String OXYGEN_TOPIC = "ossigeno";
    private static final String HEARTBEAT_TOPIC = "battito";
    // Il battito non è critico fra 70 e 130 bpm
    private static final int HEARTBEAT_THRESHOLD = 70;
    // La saturazione va bene tra 80 e 100
    private static final int OXYGEN_THRESHOLD = 80;
    
    private static MqttClient connectToBroker() 
    {
    	System.out.println("SONO DENTRO LA CONNECT TO BROKER\n");
        String clientId = "tcp://127.0.0.1:1883";
        System.out.println("CLIENT ID: " + clientId);
        // MemoryPersistence persistence = new MemoryPersistence();
        try 
        {
        	System.out.println("PROVO A ISTANZIARE UN MQTT CLIENT\n");
            
        	// MqttClient mqttClient = new MqttClient(MQTT_BROKER, clientId, persistence);
        	MqttClient mqttClient = new MqttClient(MQTT_BROKER, clientId);
            
        	System.out.println("ISTANZIATO UN MQTT CLIENT\n");
            System.out.println("ADESSO SETTO LA CALLBACK\n");
            
            mqttClient.setCallback(new MyClient());
            
            System.out.println("CALLBACK SETTATA\n");
            System.out.println("ADESSO STARTO LA CONNECT\n");
            
            mqttClient.connect();
            
            System.out.println("CONNECT RIUSCITA\n");
            System.out.println("RESTITUISCO AL MAIN UN OGGETTO MQTT CLIENT CONNESSO\n");
            
            return mqttClient;
        } 
        catch (MqttException e) 
        {
            e.printStackTrace();
        }
        return null;
    }
    
    private static void subscribeToTopics(MqttClient mqttClient) 
    {
    	try 
    	{
    		System.out.println("DENTRO LA SUBSCRIBE-TO-TOPIC\n");
            
    		mqttClient.subscribe(OXYGEN_TOPIC);
            
    		System.out.println("ISCRITTO AL TOPIC OSSIGENO\n");
            
    		mqttClient.subscribe(HEARTBEAT_TOPIC);
            
    		System.out.println("ISCRITTO AL TOPIC BATTITO\n");
        } 
    	catch (MqttException e) 
    	{
            e.printStackTrace();
        }
    }
    
    private void handleOxygenLevel(float oxygenLevel) {
        // Logic to handle oxygen level
        // Determine pill dispenser status based on oxygen level
        if (oxygenLevel < OXYGEN_THRESHOLD) 
        {
            setPillDispenserStatus("APERTA");
        } 
        else 
        {
            setPillDispenserStatus("CHIUSA");
        }
    }
    
    private void handleHeartbeat(int heartbeat) {
        // Logic to handle heartbeat
        // Determine pill dispenser status based on heartbeat
        if (heartbeat > HEARTBEAT_THRESHOLD) {
            setPillDispenserStatus("APERTA");
        } else {
            setPillDispenserStatus("CHIUSA");
        }
    }
    
    private void setPillDispenserStatus(String status) {
        // Logic to set the pill dispenser status
        // You can update a variable, call a method, or perform any necessary actions
        System.out.println("SCATOLA: " + status);
        // From this point, oxygen and heartbeat must decrease
    }

	public void connectionLost(Throwable throwable) 
	{
		// TODO Auto-generated method stub
		System.out.println("SEI DENTRO LA CONNECTION LOST!\n");
		
	}

	public void deliveryComplete(IMqttDeliveryToken token) 
	{
		// TODO Auto-generated method stub
		System.out.println("SEI DENTRO LA DELIVERY COMPLETE!\n");
		
	}

	
	public void messageArrived(String topic, MqttMessage message) throws Exception 
	{
		// TODO Auto-generated method stub
		System.out.println("SEI DENTRO LA MESSAGGE ARRIVED! MESSAGGIO ARRIVATO!\n");
		System.out.println("TOPIC RICEVUTO: " + topic + "\n");
		if (topic.equals(OXYGEN_TOPIC)) 
		{
            // float oxygenLevel = Float.parseFloat(message.toString());
            // handleOxygenLevel(oxygenLevel);
			int oxygenLevel = Integer.parseInt(message.toString());
			System.out.println("HO RICEVUTO OSSIGENO : "+ oxygenLevel + "\n");
			handleOxygenLevel(oxygenLevel);
        } 
		else if (topic.equals(HEARTBEAT_TOPIC)) 
		{
            int heartbeat = Integer.parseInt(message.toString());
            System.out.println("HO RICEVUTO BATTITO : "+ heartbeat + "\n");
            handleHeartbeat(heartbeat);
        }
		else
		{
			System.out.println("MESSAGGIO NON RICONOSCIUTO! USARE ossigeno O battito\n");
		}
	}
		
	/*
	public void messageArrived(String topic, MqttMessage message) throws Exception 
	{
		// TODO Auto-generated method stub
		System.out.println("SEI DENTRO LA MESSAGGE ARRIVED! MESSAGGIO ARRIVATO!\n");
		System.out.println("TOPIC RICEVUTO: " + topic + "\n");
		
		JSONObject obj;
        JSONParser parser = new JSONParser();
        
        try 
        {
        	String data = new String(message.getPayload(), StandardCharsets.UTF_8);
        	obj = (JSONObject) parser.parse(data);

        } 
        catch (ParseException e) {
        	throw new RuntimeException(e);
        }

        try
        {
        	int modified = DatabaseAccess.insertData((Long) obj.get("value"), topic);  //using Long since JSON parser
        	if (modified < 1)
        	{
        		System.err.println("DataBase error: could not insert new data");
        	}
        }catch (SQLException e)
        {
        	System.err.println("DataBase error: cannot connect");
        }
        
	}
	*/
	
	public static void main(String[] args) 
	{
		// creo, setcallback, connect, subscribe
		System.out.println("SIAMO NEL MAIN! ADESSO SI AVVIERA LA CONNECT-TO-BROKER\n");
		// Creo l'istanza
        MqttClient mqttClient = connectToBroker();
        
        System.out.println("CONNECT-TO-BROKER FATTA\n");
        System.out.println("ADESSO DEVO ISCRIVERMI AI TOPIC\n");
        
        subscribeToTopics(mqttClient);
        
        System.out.println("ISCRIZIONE AI TOPIC RIUSCITA\n");
    }
	

}
