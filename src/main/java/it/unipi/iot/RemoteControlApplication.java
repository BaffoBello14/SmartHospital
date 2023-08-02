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

    public int retrieveSensorType(String iniziale)
    {
        if (iniziale.startsWith("O"))
        {
            return 0;
        }
        else if (iniziale.startsWith("T"))
        {
            return 1;
        } 
        else // Vuol dire che è H
        {
            return 2;
        }
    }

    public String retrieveActuatorType(int index) 
    {
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

    public boolean alreadyInUse(String patient_id, int index) throws SQLException
    {
        if(!pazienti.get(patient_id)[index].isEmpty())
        {
            // Vuol dire che in quel posto della mappa ce gia un ip
            // quindi l'attuatore index è attivo
            return true;
        }
        return false;
    }

    public String retrieveActuatorIP(String patient_id, int index) throws SQLException 
    {
        /*
        String ids = pazienti.keySet().stream().filter(id -> id.equals(patient_id)).findFirst().orElse(null);
        if (ids != null && !pazienti.get(patient_id)[index].isEmpty()) 
        {
            // Questo caso non ha senso
            // Vuol dire che stai cercando l'ip di un attuatore gia attivo
            return pazienti.get(patient_id)[index];
        } 
        else 
        {
            String type = retrieveActuatorType(index);
            String ip = DB.retrieveActuatorIP(type);
            if (ip != null) 
            {
                pazienti.get(patient_id)[index] = ip;
            }
            pazienti.get(patient_id)[index] = ip;
            return ip;
        }
        */
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

    /*
    public String registerActuator(String patient_id, int index) throws SQLException 
    {
        String ip = retrieveActuatorIP(patient_id, index);
        if (!ip.isEmpty()) 
        {
            // Vuol dire che l'ip è stato recuperato correttamwnte
            // String type = retrieveActuatorType(index);
            // ip = DB.retrieveActuatorIP(type);
            // if (ip != null) 
            // {
                // pazienti.get(patient_id)[index] = ip;
                // DB.updateActuatorStatus(ip, patient_id);
            // }
        }
        return ip;
    }
    */

    public void changeActuatorStatus(String patient_id, int index, boolean isActive) throws SQLException 
    {
        String[] newIps = pazienti.get(patient_id);
        // Recupero l'ip
        String actuatorIp = DB.retrieveActuatorIP(patient_id);
        if(isActive)
        {
            // Va attivato
            // Lo aggiungo alla mappa
            newIps[index] = actuatorIp;
            pazienti.put(patient_id, newIps);
        }
        else
        {
            newIps[index] = "";
            pazienti.put(patient_id, newIps);
        }
        // Aggiorno il DB
        DB.updateActuatorStatus(actuatorIp, isActive);
        // Richiedo la PUT
        Actuator_Client.putClientRequest(actuatorIp, retrieveActuatorType(index), isActive);
    }

    /*
    public void activateActuator(String patient_id, int index, boolean isActive) throws SQLException 
    {
        // String actuatorIp = registerActuator(patient_id, index);
        String actuatorIp = "";
        if(isActive)
        {
            // Se è da attivare
            // Recupero l'ip e faccio la put con ON
            actuatorIp = retrieveActuatorIP(patient_id, index);
            if(!actuatorIp.isEmpty())
            {
                // Aggiorno lo stato sul DB
                DB.updateActuatorStatus(actuatorIp, patient_id);
                // Richiedo la put

                Actuator_Client.putClientRequest(actuatorIp, retrieveActuatorType(index), isActive);

                CoapClient client = new CoapClient("coap://[" + actuatorIp + "]/" + "actuator" + index);
                JSONObject object = new JSONObject();
                object.put("action", isActive ? "ON" : "OFF");

                CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);

                if (response == null) 
                {
                    System.err.println("An error occurred while contacting the actuator");
                } 
                else 
                {
                    CoAP.ResponseCode code = response.getCode();
                    switch (code) 
                    {
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
        else
        {
            // Altrimenti rimuovo l'ip dalla mappa pazienti
            // e poi faccio la put con OFF
            // pazienti.remove(patient_id, actuatorIp);
            String[] tmp = pazienti.get(patient_id);
            tmp[index] = "";
            pazienti.put(patient_id, tmp);
            // L'ip posso comunque recuperarlo dal DB
            // Ma non deve piu esserci nella mappa
            // actuatorIp = DB.retrieveActuatorIP(retrieveActuatorType(index));
            DB.turnOffActuator(DB.retrieveActuatorIP(retrieveActuatorType(index)), patient_id);
        }
        // Nemmeno prima questo if aveva senso
        // la registerActuator non poteva dare valori nulli
        /*
        if (actuatorIp != null) 
         {
            CoapClient client = new CoapClient("coap://[" + actuatorIp + "]/" + "actuator" + index);

            JSONObject object = new JSONObject();
            object.put("action", isActive ? "ON" : "OFF");

            CoapResponse response = client.put(object.toJSONString().replace("\"",""), MediaTypeRegistry.APPLICATION_JSON);

            if (response == null) 
            {
                System.err.println("An error occurred while contacting the actuator");
            } 
            else 
            {
                CoAP.ResponseCode code = response.getCode();
                switch (code) 
                {
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
    */

    public void run() 
    {
        pazienti.put("", attuatori);

        try 
        {
            List<String> typeList = Arrays.asList("oxygen_sensor", "temperature_sensor", "heartbeat_sensor");
            HashMap<String, Float> retrieved;

            for (String s : typeList) {
                // Recupero i dati del sensore s dal DB
                retrieved = new HashMap<>(DB.retrieveSensorData(s));

                if (retrieved.isEmpty()) 
                {
                    System.out.println("NESSUN DATO RECUPERATO\n");
                } 
                else 
                {
                    for (String key : retrieved.keySet()) 
                    {
                        // Tolgo la prima lettera che corrisponde al tipo di sensore
                        String patient_id = key.substring(1, key.length());
                        int index = -1;
                        index = retrieveSensorType(key.substring(0, 1));

                        // Controllo che tipo di dato abbiamo
                        if(key.equals("heartbeat_sensor"))
                        {
                            // Analizziamo il valore del battito xardiaco
                            if(retrieved.get(key)>=DNG_OX_TH)
                            {
                                // Se è critico, deve essere acceso med
                                System.out.println("Danger! Activating actuator...\n");
                                // Controllare se l'attuatore è gia attivo
                                if(alreadyInUse(patient_id, index))
                                {
                                    // L'attuatore è gia in uso
                                    // Non fare nulla
                                    System.out.println("ATTUATORE GIA IN USO, LASCIARLO ATTIVO\n");
                                }
                                else
                                {
                                    // Vuol dire che non era attivo e bisogna attivarlo
                                    try 
                                    {
                                        changeActuatorStatus(patient_id, index, true);
                                        System.out.println("ATTUATORE OFF -> ON\n");
                                    } 
                                    catch (SQLException e) 
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            else if(retrieved.get(key)>=CTR_OX_TH)
                            {
                                // Controllo incrociato
                                // Questo valore è al limite, bisogna vedere gli altri

                            }
                            else
                            {
                                System.out.println("All good. Deactivating actuator...");
                                if(!alreadyInUse(patient_id, index))
                                {
                                    // Non era attivo quindi lo lascio disattivato
                                    System.out.println("ATTUATORE RIMANE SPENTO\n");
                                }
                                else
                                {
                                    // Era acceso e devo disattivarlo
                                    System.out.println("L'ATTUATORE ERA ATTIVO E DEVO SPEGNERLO\n");
                                    try 
                                    {
                                        changeActuatorStatus(patient_id, index, false);
                                        System.out.println("ATTUATORE ON -> OFF\n");
                                    } 
                                    catch (SQLException e) 
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        else if(key.equals("oxygen_sensor"))
                        {

                        }
                        else if(key.equals("temperature_sensor"))
                        {

                        }
                        else
                        {
                            System.out.println("SENSORE NON RICONOSCIUTOOO\n");
                        }
                        
                        /*
                        float threshold = -1;

                        if (key.startsWith("O")) 
                        {
                            index = 0;
                            // La scelta della threshold è sbagliata
                            // non ha senso perche CTR è per il controllo incrociato
                            threshold = retrieved.get(key) >= DNG_OX_TH ? DNG_OX_TH : CTR_OX_TH;
                        } 
                        else if (key.startsWith("T")) 
                        {
                            index = 1;
                            threshold = retrieved.get(key) >= DNG_TEMP_TH ? DNG_TEMP_TH : CTR_TEMP_TH;
                        } 
                        else if (key.startsWith("H")) 
                        {
                            index = 2;
                            threshold = retrieved.get(key) >= DNG_HB_TH ? DNG_HB_TH : CTR_HB_TH;
                        }
                        */

                        // if (index != -1 && threshold != -1) 
                        // {
                            // Bisogna recuperare le rispettive threshold

                            // if (retrieved.get(key) >= threshold)
                            /* 
                            if (retrieved.get(key) >= 2) 
                            {
                                
                                System.out.println("Danger! Activating actuator...\n");
                                // Controllare se l'attuatore è gia attivo
                                if(alreadyInUse(patient_id, index))
                                {
                                    // L'attuatore è gia in uso
                                    // Non fare nulla
                                    System.out.println("ATTUATORE GIA IN USO, LASCIARLO ATTIVO\n");
                                }
                                else
                                {
                                    // Vuol dire che non era attivo e bisogna attivarlo
                                    try 
                                    {
                                        activateActuator(patient_id, index, true);
                                    } 
                                    catch (SQLException e) 
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            } 
                            else 
                            {
                                System.out.println("All good. Deactivating actuator...");
                                if(!alreadyInUse(patient_id, index))
                                {
                                    // Non era attivo quindi lo lascio disattivato
                                    System.out.println("ATTUATORE RIMANE SPENTO\n");
                                }
                                else
                                {
                                    // Era acceso e devo disattivarlo
                                    System.out.println("L'ATTUATORE ERA ATTIVO E DEVO SPEGNERLO\n");
                                    try 
                                    {
                                        activateActuator(patient_id, index, false);
                                        System.out.println("ATTUATORE SPENTO\n");
                                    } 
                                    catch (SQLException e) 
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            */ 
                            
                        // }
                    }
                }
            }
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        } 
        finally 
        {
            Thread.currentThread().interrupt();
        }

    }

    public static void main(String[] args) 
    {
        Thread thread = new Thread(new RemoteControlApplication());
        thread.start();
    }

}
