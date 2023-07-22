package it.unipi.iot;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

public class CoapRegistration {

    private static final String REGISTRATION_URL = "coap://127.0.0.1:5683/registration"; // Indirizzo del server CoAP di registrazione

    public static boolean register(String deviceId, String deviceType) {
        try {
            CoapClient client = new CoapClient(REGISTRATION_URL);
            String payload = "id=" + deviceId + "&type=" + deviceType;
            CoapResponse response = client.post(payload, 0); // Metodo POST per la registrazione

            if (response != null && response.isSuccess()) {
                System.out.println("Registration successful!");
                return true;
            } else {
                System.out.println("Registration failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
