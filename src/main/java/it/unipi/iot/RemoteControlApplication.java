package it.unipi.iot;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.simple.JSONObject;

public class RemoteControlApplication implements Runnable {

    private static final float DNG_TEMP_TH = 38.9f;
    private static final float CTR_TEMP_TH = 36.9f;

    private static final float DNG_OX_TH = 49.9f;
    private static final float CTR_OX_TH = 59.9f;

    private static final int DNG_HB_TH = 70;
    private static final int CTR_HB_TH = 80;

    private static final RemoteControlApplication instance = new RemoteControlApplication();

    String[] attuatori = new String[3];
    static HashMap<String, String[]> pazienti = new HashMap<>();

    public static RemoteControlApplication getInstance() {
        return instance;
    }

    public String retrieveActuatorType(int index) {
        String type = "";
        switch (index) {
            case 0:
                type = "mask";
                break;
            case 1:
                type = "med";
                break;
            case 2:
                type = "scossa";
                break;
            default:
                System.out.println("Invalid control value!");
                return "";
        }
        return type;
    }

    public String retrieveActuatorIP(String patient_id, int index) throws SQLException {
        String ids = pazienti.keySet().stream().filter(id -> id.equals(patient_id)).findFirst().orElse(null);
        if (ids != null && !pazienti.get(patient_id)[index].equals("")) {
            return pazienti.get(patient_id)[index];
        } else {
            String type = retrieveActuatorType(index);
            String ip = DB.retrieveActuatorIP(type);
            if (ip != null) {
                pazienti.get(patient_id)[index] = ip;
            }
            return ip;
        }
    }

    public String registerActuator(String patient_id, int index) throws SQLException {
        String ip = retrieveActuatorIP(patient_id, index);
        if (ip == null) {
            String type = retrieveActuatorType(index);
            ip = DB.retrieveActuatorIP(type);
            if (ip != null) {
                pazienti.get(patient_id)[index] = ip;
                DB.updateActuatorStatus(ip, patient_id);
            }
        }
        return ip;
    }

    public void activateActuator(String patient_id, int index, boolean isActive) throws SQLException {
        String actuatorIp = registerActuator(patient_id, index);
        if (actuatorIp != null) {
            CoapClient client = new CoapClient("coap://[" + actuatorIp + "]/" + "actuator" + index);

            JSONObject object = new JSONObject();
            object.put("action", isActive ? "ON" : "OFF");

            CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);

            if (response == null) {
                System.err.println("An error occurred while contacting the actuator");
            } else {
                CoAP.ResponseCode code = response.getCode();
                switch (code) {
                    case CHANGED:
                        System.err.println("State correctly changed because of danger or user input");
                        break;
                    case BAD_OPTION:
                        System.err.println("Parameters error");
                        break;
                }
            }
        }
    }

    public void run() {
        pazienti.put("", attuatori);

        try {
            List<String> typeList = Arrays.asList("oxygen_sensor", "temperature_sensor", "heartbeat_sensor");
            HashMap<String, Float> retrieved;

            for (String s : typeList) {
                // Recupero i dati dei sensori dal database.
                retrieved = new HashMap<>(DB.retrieveSensorData(s));

                if (retrieved.isEmpty()) {
                    System.out.println("NESSUN DATO RECUPERATO\n");
                } else {
                    for (String key : retrieved.keySet()) {
                        String patient_id = key.substring(1, key.length());
                        int index = -1;
                        float threshold = -1;

                        if (key.startsWith("O")) {
                            index = 0;
                            threshold = retrieved.get(key) >= DNG_OX_TH ? DNG_OX_TH : CTR_OX_TH;
                        } else if (key.startsWith("T")) {
                            index = 1;
                            threshold = retrieved.get(key) >= DNG_TEMP_TH ? DNG_TEMP_TH : CTR_TEMP_TH;
                        } else if (key.startsWith("H")) {
                            index = 2;
                            threshold = retrieved.get(key) >= DNG_HB_TH ? DNG_HB_TH : CTR_HB_TH;
                        }

                        if (index != -1 && threshold != -1) {
                            if (retrieved.get(key) >= threshold) {
                                System.out.println("Danger! Activating actuator...");
                                try {
                                    activateActuator(patient_id, index, true);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.out.println("All good. Deactivating actuator...");
                                try {
                                    activateActuator(patient_id, index, false);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt();
        }

    }

    public static void main(String[] args) {
        Thread thread = new Thread(new RemoteControlApplication());
        thread.start();
    }

}
