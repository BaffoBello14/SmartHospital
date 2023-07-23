package it.unipi.iot.remoteControlApplication;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

public class PeriodicDataRetrieval {

    private static final String SENSOR_URL = "coap://127.0.0.1:5683/sensor"; // Sostituisci con l'URL corretto del sensore CoAP
    private static final int RETRIEVAL_INTERVAL = 5000; // Intervallo di recupero dei dati in millisecondi (5 secondi nel nostro esempio)

    public void startRetrieval() {
        new Thread(() -> {
            while (true) {
                CoapClient client = new CoapClient(SENSOR_URL);
                CoapResponse response = client.get();
                
                if (response != null && response.isSuccess()) {
                    String payload = response.getResponseText();
                    // Elabora il payload ricevuto e aggiorna l'interfaccia utente
                    // Implementa qui la logica di controllo in base ai dati dei sensori
                }

                try {
                    Thread.sleep(RETRIEVAL_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
