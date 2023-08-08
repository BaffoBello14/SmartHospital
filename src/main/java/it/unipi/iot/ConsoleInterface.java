package it.unipi.iot;

import java.util.Scanner;

public class ConsoleInterface implements Runnable {

    private final RemoteControlApplication application;

    public ConsoleInterface(RemoteControlApplication application) {
        this.application = application;
    }

    private void printCommands() {
        System.out.println("Commands:");
        System.out.println("getActuatorStatuses [patientId] - Get the status of actuators for a specific patient or all patients if no ID provided");
        System.out.println("getAvailableActuators - Get the list of available actuators");
        System.out.println("setOxygenThresholds <low> <high> - Set the thresholds for oxygen levels");
        System.out.println("setCardioThresholds <lowPulse> <highPulse> <lowPressure> <highPressure> - Set the thresholds for heart rate and blood pressure");
        System.out.println("setTroponinThresholds <low> <high> - Set the thresholds for troponin levels");
        System.out.println("activateActuator <patientId> <actuatorType> <level> <duration> - Activate a specific actuator for a specific patient for a given duration");
        System.out.println("quit - Exit the program");
        System.out.println("help - Show the list of commands");
    }

    @Override
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            printCommands(); // Print commands at start
            while (true) {
                System.out.println("Enter command:");
                String command = scanner.nextLine();
                String[] parts = command.split(" ");
                switch (parts[0]) {
                    case "getActuatorStatuses":
                        if (parts.length > 1) {
                            String patientId = parts[1];
                            String statuses = application.getActuatorStatuses(patientId);
                            System.out.println(statuses);
                        } else {
                            String allStatuses = application.getActuatorStatuses();
                            System.out.println(allStatuses);
                        }
                        break;
                    case "getAvailableActuators":
                        System.out.println(application.getAvailableActuators());
                        break;
                    case "setOxygenThresholds":
                        application.setOxygenThresholds(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
                        break;
                    case "setCardioThresholds":
                        application.setCardioThresholds(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                        break;
                    case "setTroponinThresholds":
                        application.setTroponinThresholds(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
                        break;
                    case "activateActuator":
                        application.activateActuator(parts[1], parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                        break;
                    case "quit":
                        System.out.println("Exiting");
                        System.exit(0);
                        break;
                    case "help":
                        printCommands(); // Print commands when "help" is entered
                        break;
                    default:
                        System.out.println("Unknown command");
                }
            }
        }
    }
}
