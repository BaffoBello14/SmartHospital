// package it.unipi.iot.remoteControlApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

public class RemoteControlApplication {

    // Setting threshold
    // private static float UP_TEMP_TH = 43;
    // private static float LW_TEMP_TH = 34.5;
    // DANGER: TEMP > 39
    private static float DNG_TEMP_TH = 38.9;

    // private static float UP_OX_TH = 100.00;
    // private static float LW_OX_TH = 40.0;
    // DANGER OX < 50
    private static float DNG_OX_TH = 49.9;

    // private static int UP_HB_TH = 140;
    // private static int LW_HB_TH = 50;
    // DANGER: HB < 70
    private static int DNG_HB_TH = 70;


    
    public static RemoteControlApplication getInstance(){
        return instance;
    }

    public void run() 
    {

        try
        {
            // HashMap<String, Integer> values = DatabaseAccess.retrieveData();
            //System.out.println(values);
            
            // Preparing HashMap to temporaneamente store data retireved
            // id STARTSWITH 1 = oxygen
            // id STARTSWITH 2 = hb
            // id STARTSWITH 3 = temperature
            // String = idSensore
            // Value = valoreSensore
            HashMap<String, Float> retrieved = new HashMap<>();

            // Try to connect w/ DB
            // try (Connection connection = DB.getDb())
            List<String> typeList = Arrays.asList("oxygen_sensor", "temperature_sensor", "heartbeat_sensor");
            // String = tipo sensore
            // Float = media ultimi 3 valori sensore
            try (Connection connection = DB.getDb())
            {
                for (String s : typeList) 
                {
                    System.out.println("MONITORING "+s+"TABLE\n");
                    String query = "SELECT id, AVG(valore) AS media_valore "
                                    + "FROM ("
                                    + "  SELECT id, valore,"
                                    + "         ROW_NUMBER() OVER (PARTITION BY id ORDER BY timestamp DESC) as rn "
                                    + "  FROM " + s + ") t "
                                    + "WHERE rn <= 3 "
                                    + "GROUP BY id";
                    // Creazione del PreparedStatement
                    try (PreparedStatement ps = connection.prepareStatement(query)) 
                    {

                        // Eseguire la query
                        try (ResultSet result = ps.executeQuery()) 
                        {

                            // Processa i risultati
                            int count = 0;
                            while (result.next()) 
                            {
                                float sensorValue = result.getFloat("media_valore");
                                String id = result.getString("id");
                                System.out.println("ID: " + id + ", Media Valore: " + sensorValue);
                                // Inserisco la misurazione recuperata nella mappa
                                retrieved.put(id, sensorValue);
                                count++;

                            }
                            System.out.println("HO "+count+" ELEMENTI NELLA MAPPA");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            if(retrieved.isEmpty())
            {
                // Mappa vuota
                System.out.println("NESSUN DATO RECUPERATO\n");
            }
            else
            {
                // Implements logic control
                // Controllo soglie di threshold
                // Recupero il primo numero dell'id che rappresenta il tipo di sensore
                for(String s : retrieved.keySet())
                {
                    if(s.startsWith("H"))
                    {
                        // Allora stiamo analizzando il battito
                        // Recupero il valore corrispondente al battito
                        if(retrieved.get(s)>=DNG_HB_TH)
                        {
                            // Allora il battito cardiaco è critico -> do something
                            System.out.println("BATTITO CARDIACO CRITICO :  "+retrieved.get(s));
                        }
                    }
                    else if(s.startsWith("O"))
                    {
                        if(retrieved.get(s)>=DNG_HB_TH)
                        {
                            // Allora l'ossigeno è critico -> do something
                            System.out.println("OSSIGENO CRITICO :  "+retrieved.get(s));
                        }
                    }
                    else if(s.startsWith("T"))
                    {
                        if(retrieved.get(s)>=DNG_TEMP_TH)
                        {
                            // Allora temperatura critica -> do something
                            System.out.println("TEMPERATURA CRITICA :  "+retrieved.get(s));
                        }
                    }
                    else
                    {
                        // non ci dovrebbe entrare mai
                        System.out.println("SENSORE NON RICONOSICUTO\n");
                    }
                }
            }
        }

    }

}
