package it.unipi.iot;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class RemoteControlApplication implements Runnable {

    // S1 -> A1
    // S2,S3 -> A2
    // S2,S3 -> A3

    // tropomina = 0.1 g/ml if val > 1 allora pericolo
    private static final float DNG_TRP_TH = 0.1f;
    private static final float CTR_TRP_TH = 0.07f;

    private static final float DNG_OX_TH = 49.9f;
    private static final float CTR_OX_TH = 59.9f;

    private static final int U_DNG_HB_TH = 250;
    private static final int U_CTR_HB_TH = 220;
    private static final int L_DNG_HB_TH = 20;
    private static final int L_CTR_HB_TH = 30;

    private static final RemoteControlApplication instance = new RemoteControlApplication();

    String[] attuatori = new String[3];
    static HashMap<String, String[]> pazienti = new HashMap<>();

    public static RemoteControlApplication getInstance() {
        return instance;
    }

    public int retrieveSensorType(String id)
    {
        if (id.startsWith("o"))
        {
            return 0;
        }
        else if (id.startsWith("t"))
        {
            return 1;
        } 
        else // Vuol dire che è C
        {
            return 2;
        }
    }

    public String retrieveActuatorType(int index) 
    {
        String type = "";
        switch (index) 
        {
            case 0:
                type = "mask";
                break;
            case 1:
                type = "medicine";
                break;
            case 2:
                type = "defibrillator";
                break;
            default:
                System.out.println("Invalid control value!");
                return "";
        }
        return type;
    }

    public String retrieveActuatorIP(String patient_id, int index) throws SQLException 
    {
        // String type = retrieveActuatorType(index);
        String ip = DB.retrieveActuatorIP(retrieveActuatorType(index));
        if (!ip.isEmpty()) 
        {
            // Ip recuperato -> lo inserisco nella tabella
            pazienti.get(patient_id)[index] = ip;
            return ip;
        }
        else
        {
            // Vuol dire che non ce l'ip nel DB
            System.out.println("ERRORE: IP NON PRESENTE NEL DB\n");
            return "";
        }
    }

    public boolean changeActuatorStatus(String patient_id, int index, int isActive) throws SQLException {
        // Check if the patient exists in the pazienti map
        String[] patientData = pazienti.get(patient_id);
        if (patientData == null) {
            // Add new patient to the map
            patientData = new String[]{"", "", ""};
            pazienti.put(patient_id, patientData);
        }
    
        // Check if a valid IP for the actuator already exists
        String actuatorIp = patientData[index];
        if (!"".equals(actuatorIp)) {
            
        }
        else{
            // Search for a new IP for the actuator
            actuatorIp = DB.retrieveActuatorIP(retrieveActuatorType(index));
            if (actuatorIp.isEmpty()) {
                System.out.println("ERROR: IP NOT FOUND FOR PATIENT " + patient_id);
                // Check if the patientData array is empty, if so remove the entry from the map
                if (Arrays.stream(patientData).allMatch(s -> s == null || s.isEmpty())) {
                    pazienti.remove(patient_id);
                }
                return false;  // No IP available
            }
        }
    
        // Try to change actuator status
        final int MAX_ATTEMPTS = 3;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                // Make the PUT request
                if (Actuator_Client.putClientRequest(actuatorIp, retrieveActuatorType(index), isActive)) {
                    DB.updateActuatorStatus(actuatorIp, patient_id, isActive != 0);
                    if(isActive != 0){
                        patientData[index] = actuatorIp;  // Save the IP
                        pazienti.put(patient_id, patientData);
                    }
                    else{
                        patientData[index] = ""; 
                        pazienti.put(patient_id, patientData);
                        // Check if the patientData array is empty, if so remove the entry from the map
                        if (Arrays.stream(patientData).allMatch(s -> s == null || s.isEmpty())) {
                            pazienti.remove(patient_id);
                        }
                    }
                    return true;  // Success
                } else {
                    // Error in the PUT request
                    System.out.println("ERROR IN PUT REQUEST, ATTEMPT " + (attempt + 1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        System.out.println("Failed to activate actuator for patient " + patient_id);
        return false;  // Failure
    }
    

    public int checkCardio(float cardioValue)
    {
        if(cardioValue>=U_DNG_HB_TH || cardioValue<=L_DNG_HB_TH)
            return 2; // pericoloso
        else if(cardioValue>=U_CTR_HB_TH || cardioValue<=L_CTR_HB_TH)
            return 1;
        else
            return 0;
    }

    public int checkTroponin(float trpValue)
    {
        if(trpValue>=DNG_TRP_TH)
            return 2; // pericoloso
        else if(trpValue>=CTR_TRP_TH)
            return 1;
        else
            return 0;
    }

    public int calculateDanger(float trpValue, float cardioValue)
    {
        int trpLevel = checkTroponin(trpValue);
        int cardioLevel = checkCardio(cardioValue);
        return trpLevel + cardioLevel;
    }

    public void run() {
        try {
            pazienti = DB.retrieveActiveActuators();
            int i = 1;
            for (;;) {
                String patientId = String.format("%03d", i);
                List<Float> sensorValues = DB.retrieveSensorData(patientId);
    
                if (sensorValues == null || sensorValues.size() != 3) {
                    i = i + 1;
                    if(i == 4) i = 1;
                    System.out.println("NO Data for patient " + patientId + "!!!");
                    continue;  // Vai al prossimo paziente se non ci sono dati per questo
                }
    
                //System.out.println("Data for patient " + patientId + ": " + sensorValues);
    
                // Oxygen
                int value = sensorValues.get(0) <= DNG_OX_TH ? 2 : sensorValues.get(0) <= CTR_OX_TH ? 1 : 0;
                if(value == 0 && (!pazienti.containsKey(patientId) || "".equals(pazienti.get(patientId)[0]))){

                }
                else{              
                    if (changeActuatorStatus(patientId, 0, value)) {
                        //System.out.println("Oxygen actuator on level " + value);
                    } else {
                        System.out.println("Call the doctor!!! (oxygen)");
                    }
                }
    
                // Troponin and Cardio
                float trpValue = sensorValues.get(1);
                float cardioValue = sensorValues.get(2);
                value = calculateDanger(trpValue, cardioValue);
                if(value == 0 && (!pazienti.containsKey(patientId) || ("".equals(pazienti.get(patientId)[1]) && "".equals(pazienti.get(patientId)[2])))){
                    // Do nothing
                }
                else{
                    if(value == 0){
                        if(!"".equals(pazienti.get(patientId)[1])){
                            changeActuatorStatus(patientId, 1, value);
                        }
                        if(!"".equals(pazienti.get(patientId)[2])){
                            changeActuatorStatus(patientId, 2, value);
                        }
                    }
                    else{
                        boolean actuator1 = "".equals(pazienti.get(patientId)[1]) || changeActuatorStatus(patientId, 1, value);
                        boolean actuator2 = "".equals(pazienti.get(patientId)[2]) || changeActuatorStatus(patientId, 2, value);
                        if (!actuator1 && value < 3) {
                            System.out.println("Call the doctor!!! heart desease");
                        }
                        else if (!actuator2 && value < 4) {
                            changeActuatorStatus(patientId, 2, value + 1);
                        }
                    }
                }

                i = i + 1;
                if(i == 4) i = 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) 
    {
        Thread thread = new Thread(new RemoteControlApplication());
        thread.start();
    }

}
