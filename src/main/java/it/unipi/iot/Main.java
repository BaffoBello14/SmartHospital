package it.unipi.iot;

public class Main {
    public static void main(String[] args) {
        
        Coordinator coordinator = new Coordinator();
        Thread coordinatorThread = new Thread(coordinator);

        // Create RemoteControlApplication and start its thread
        RemoteControlApplication remoteControlApplication = new RemoteControlApplication();
        Thread remoteControlApplicationThread = new Thread(remoteControlApplication);

        // Create ConsoleInterface with remoteControlApplication as dependency and start its thread
        ConsoleInterface consoleInterface = new ConsoleInterface(remoteControlApplication);
        Thread consoleInterfaceThread = new Thread(consoleInterface);

        coordinatorThread.start();
        remoteControlApplicationThread.start();
        consoleInterfaceThread.start(); // Start the ConsoleInterface thread
    }
}
