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

    public boolean changeActuatorStatus(String patient_id, int index, int isActive) throws SQLException 
    {
        String[] newIps = pazienti.get(patient_id);
        // Recupero l'ip
        String actuatorIp = "";
        if(pazienti.get(patient_id)[index].isEmpty())
            actuatorIp = DB.retrieveActuatorIP(retrieveActuatorType(index));
        else
            actuatorIp = pazienti.get(patient_id)[index];
        if(actuatorIp.isEmpty())
        {
            // Non ha trovato l'ip 
            System.out.println("IP NON TROVATO\n");
            return false;
        }
        // Richiedo la PUT
        if(Actuator_Client.putClientRequest(actuatorIp, retrieveActuatorType(index), isActive))
        {      
            if(isActive!=0)
            {
                // Aggiorno il DB
                DB.updateActuatorStatus(actuatorIp, patient_id, true);  
                // Va attivato
                // Lo aggiungo alla mappa
                newIps[index] = actuatorIp;
                pazienti.put(patient_id, newIps);
            }
            else
            {
                // Aggiorno il DB
                DB.updateActuatorStatus(actuatorIp, patient_id, false);  
                newIps[index] = "";
                pazienti.put(patient_id, newIps);
            }
            return true;
        }
        else
        {
            // Errore nella PUT
            System.out.println("ERRORE NELLA PUT\n");
            return false;
        }
    }

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
                        int index = retrieveSensorType(key);

                        // Controllo che tipo di dato abbiamo
                        // if(key.equals("heartbeat_sensor"))
                        if(index==0)
                        {
                            // Analizziamo il valore del ossigeno
                            int value = 0;
                            if(retrieved.get(key)<=CTR_OX_TH)
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
                                    if(retrieved.get(key)<=DNG_OX_TH)
                                        value = 2; // pericolo forte
                                    else
                                        value = 1; // controllabile
                                    try 
                                    {
                                        if(changeActuatorStatus(patient_id, index, value))
                                        {
                                            System.out.println("ATTUATORE OFF -> ON\n");
                                        }
                                        else
                                        {
                                            return;
                                        }
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
                                        changeActuatorStatus(patient_id, index, 0);
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
