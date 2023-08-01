package SmartSuit.unipi.it.remoteControlApplication;

import SmartSuit.unipi.it.DatabaseAccess;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.util.HashMap;

public class Actuator_Client {

    public static void actuatorCall(String ip, String resource, String action, int overThreshold) throws SQLException 
    {
        CoapClient client = new CoapClient("coap://[" + ip + "]/" + resource);
        JSONObject object = new JSONObject();
        object.put("threshold", overThreshold);
        object.put("action", action);
        
        CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);
        
        if (response == null) 
        {
            System.err.println("An error occurred while contacting the actuator");
        } 
        else 
        {
            CoAP.ResponseCode code = response.getCode();
            //System.out.println(code);
            switch (code) 
            {
                case CHANGED:
                    System.err.println("State correctly changed because of danger or user input");
                    DatabaseAccess.updateActuators("/" + ip, resource, action);
                    break;
                case BAD_OPTION:
                    System.err.println("Parameters error");
                    break;
            }

        }
    }

    public static void setIsDanger(String danger, boolean val){
        isDanger.put(danger, val);
    }

    public static HashMap<String, Boolean> getIsDanger(){return isDanger;}

}

