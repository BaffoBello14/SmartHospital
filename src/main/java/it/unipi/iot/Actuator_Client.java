package it.unipi.iot;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Actuator_Client {

    private static final Logger logger = Logger.getLogger(Actuator_Client.class.getName());

    public static boolean putClientRequest(String ip, String resource, int isActive, int time) throws SQLException, IllegalStateException {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        JSONObject object = new JSONObject();
        object.put("level", isActive);
        object.put("time", time);
        System.out.println(object);
    
        CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);
    
        if (response == null) {
            logger.log(Level.SEVERE, "An error occurred while contacting the actuator");
            throw new IllegalStateException("An error occurred while contacting the actuator");
        }
    
        CoAP.ResponseCode code = response.getCode();
        switch (code) {
            case CHANGED:
                logger.log(Level.INFO, "STATO CAMBIATO CORRETTAMENTE");
                System.out.println("Attuatore: " + resource + " attivato");
                return true;
            case BAD_OPTION:
                System.out.println("Errore attivazione attuatore: " + resource);
                logger.log(Level.SEVERE, "ERRORE NEL CAMBIO STATO");
                return false;
            default:
                logger.log(Level.WARNING, "ERRORE DEFAULT");
                return false;
        }
    }
    

    public static boolean checkActuatorStatus(String ip, String resource) {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        CoapResponse response = client.get();
    
        return response != null && response.getCode() == CoAP.ResponseCode.CONTENT;
    }

    public static String getActuatorStatus(String ip, String resource) {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        CoapResponse response = client.get();
    
        if (response != null && response.getCode() == CoAP.ResponseCode.CONTENT) {
            return response.getResponseText();
        } else {
            System.err.println("Failed to get status of actuator at IP " + ip);
            return null;
        }
    }
    
}
