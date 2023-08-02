import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.util.HashMap;

public class Actuator_Client {

    public static boolean putClientRequest(String ip, String resource, int isActive) throws SQLException 
    {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        // Nella putHandler
        // se gli arriva ON -> accendi
        // se gli arriva OFF -> spegni
        JSONObject object = new JSONObject();
        String action = "";
        if(isActive==0)
        {
            action = "OFF";
        }
        else if(isActive==1)
        {
            action = "ON";
        }
        else // vuol dire che isActive == 2
        {
            action = "POWER";
        }
        // object.put("action", isActive ? "ON" : "OFF");
        // Fa la richiesta di PUT 
        CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);
        
        if (response == null) 
        {
            System.err.println("An error occurred while contacting the actuator");
            return false;
        } 
        else 
        {
            CoAP.ResponseCode code = response.getCode();
            //System.out.println(code);
            switch (code) 
            {
                case CHANGED:
                    System.err.println("STATO CAMBIATO CORRETTAMENTE\n");
                    return true;
                    break;
                case BAD_OPTION:
                    return false;
                    System.err.println("ERRORE NEL CAMBIO STATO\n");
                    break;
                default:
                     System.err.println("ERRORE DEFAULT\n");
                     break;
            }

        }
        // QUI NON CI DOVREBBE ARRIVARE MAI
        System.out.println("QUI NON CI DEVE ARRIVARE\n");
        return false;
    }

}

