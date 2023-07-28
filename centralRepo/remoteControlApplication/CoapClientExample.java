package it.unipi.iot.remoteControlApplication;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

public class CoapClientExample {

    private static final String ACTUATOR_URL = "coap://127.0.0.1:5683/actuator"; // Sostituisci con l'URL corretto dell'attuatore CoAP

    public void performAction(String action) {
        CoapClient client = new CoapClient(ACTUATOR_URL);
        CoapResponse response = client.post(action, 0); // Metodo POST per inviare azioni all'attuatore

        if (response != null && response.isSuccess()) {
            System.out.println("Action performed successfully!");
        } else {
            System.out.println("Action failed!");
        }
    }
}