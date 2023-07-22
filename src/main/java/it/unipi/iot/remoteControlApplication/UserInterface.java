package it.unipi.iot.remoteControlApplication;

import java.util.Scanner;

public class UserInterface {

    private static final String QUIT_COMMAND = "quit";

    private CoapClientExample coapClientExample;

    public UserInterface(CoapClientExample coapClientExample) {
        this.coapClientExample = coapClientExample;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter an action to perform on the actuator (e.g., on/off):");
            System.out.println("Type 'quit' to exit.");

            String input = scanner.nextLine();

            if (input.equalsIgnoreCase(QUIT_COMMAND)) {
                break;
            }

            // Invia l'azione all'attuatore tramite il CoAP Client
            coapClientExample.performAction(input);
        }

        scanner.close();
    }
}
