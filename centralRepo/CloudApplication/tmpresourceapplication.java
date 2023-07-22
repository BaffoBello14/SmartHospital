package it.unipi.iot;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

public class ResourceRegistration {

    private static final String REGISTRATION_URL = "coap://127.0.0.1:5683/resource"; // Indirizzo del server CoAP per la registrazione delle risorse

    public static boolean register(String deviceId, String resourceName, String resourceType) {
        try {
            CoapClient client = new CoapClient(REGISTRATION_URL);
            String payload = "id=" + deviceId + "&name=" + resourceName + "&type=" + resourceType;
            CoapResponse response = client.post(payload, 0); // Metodo POST per la registrazione delle risorse

            if (response != null && response.isSuccess()) {
                System.out.println("Resource Registration successful!");
                return true;
            } else {
                System.out.println("Resource Registration failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
