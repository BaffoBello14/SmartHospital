package it.unipi.iot;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Actuator_Client {

    private static final Logger logger = Logger.getLogger(Actuator_Client.class.getName());

    private static HashMap<String, Integer> status = new HashMap<>();

    public static String putMedicine(String ip, int isActive, int time)
    {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" +  "medicine/quantity");
        JSONObject object = new JSONObject();
        object.put("level", isActive);
        object.put("time", time);
        CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            logger.log(Level.SEVERE, "An error occurred while contacting the actuator");
            throw new IllegalStateException("An error occurred while contacting the actuator");
        }
        CoAP.ResponseCode code = response.getCode();
        switch (code) {
            case CHANGED:
                return "medicine/type";
            case BAD_OPTION:
                System.out.println("Errore attivazione attuatore: medicine");
                logger.log(Level.SEVERE, "ERRORE NEL CAMBIO STATO");
                return "";
            case FORBIDDEN:
                return "medicine/type";
            default:
                logger.log(Level.WARNING, "ERRORE DEFAULT");
                return "";
        }
    }

    public static boolean putClientRequest(String ip, String resource, int isActive, int time) throws SQLException, IllegalStateException 
    {
        int stato = localCheck(ip);
        if("defibrillator".equals(resource)){
            if(stato == isActive || (stato >= 0 && stato <=2 && isActive >= 0 && isActive <=2)){
                return true;
            }
        }
        if(stato==isActive)
        {
            return true;
        }
        if("medicine".equals(resource))
        {
            resource = putMedicine(ip, isActive, time);
            if(resource.equals("")) return false;
        }
        
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        JSONObject object = new JSONObject();
        object.put("level", isActive);
        object.put("time", time);
        if(resource.equals("medicine/type")) resource = "medicine";
    
        CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);
    
        if (response == null) {
            logger.log(Level.SEVERE, "An error occurred while contacting the actuator");
            throw new IllegalStateException("An error occurred while contacting the actuator");
        }
    
        CoAP.ResponseCode code = response.getCode();
        switch (code) {
            case CHANGED:
                logger.log(Level.INFO, "STATO CAMBIATO CORRETTAMENTE");
                System.out.println("Attuatore: " + resource);
                status.put(ip, isActive);
                return true;
            case BAD_OPTION:
                logger.log(Level.SEVERE, "ERRORE NEL CAMBIO STATO");
                System.out.println("Attuatore: " + resource);  
                return false;
            case FORBIDDEN:
                return true;
            default:
                logger.log(Level.WARNING, "ERRORE DEFAULT");
                return false;
        }
    }
    
    public static int localCheck(String ip)
    {
        int res = -1;
        if(status.keySet().isEmpty())
        {
            return res;
        }
        for(String keyIp : status.keySet())
        {
            if(keyIp.equals(ip))
                res = status.get(keyIp);
        }
        return res;
    }

    public static boolean checkActuatorStatus(String ip, String resource) 
    {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        CoapResponse response = client.get();
    
        return response != null && response.getCode() == CoAP.ResponseCode.CONTENT;
    }

    public static String getActuatorStatus(String ip, String resource) 
    {
        int check = localCheck(ip);
        String stato = " ";
        switch(check)
        {
            case 1:
            if(resource.equals("medicine"))
                stato = "TYPE 1 ";
            stato = stato + "ON LOW";
            break;
            case 2:
            if(resource.equals("medicine"))
                stato = "TYPE 1 ";
            stato = stato + "ON HIGH";
            break;
            case 3:
            stato = "TYPE 2 ON LOW";
            break;
            case 4:
            stato = "TYPE 2 ON HIGH";
            break;
            default:
            stato = "ERROR";
            break;
        }
        return stato;
    }
    
}
