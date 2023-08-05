package it.unipi.iot;

public class Main {
    public static void main(String[] args) {
        
        Coordinator coordinator = new Coordinator();
        Thread coordinatorThread = new Thread(coordinator);

        
        Thread remoteControlApplicationThread = new Thread(new RemoteControlApplication());

        coordinatorThread.start();
        remoteControlApplicationThread.start();
    }
}
