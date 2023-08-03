package it.unipi.iot;

public class Main {
    public static void main(String[] args) {
        
        Thread coordinatorThread = new Thread(Coordinator::new);
        
        Thread remoteControlApplicationThread = new Thread(new RemoteControlApplication());

        coordinatorThread.start();
        remoteControlApplicationThread.start();
    }
}
