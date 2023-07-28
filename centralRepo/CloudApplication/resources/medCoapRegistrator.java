import java.net.InetAddress;

import org.eclipse.californium.core.CoapResource;

public class medCoapRegistrator extends CoapResource{
    
    // Metodo obbligatorio da implementare, forzato

    // SUCCESS

    public medCoapRegistrator(String name) 
    {
        super(name);
    }

    public void handlePOST (CoapExchange exchange) 
    {
        String s = new String(exchange.getRequestPayload());
        JSONObject obj;
        JSONParser parser = new JSONParser();
        try 
        {
            obj = (JSONObject) parser.parse(s);
        } 
        catch (ParseException e) 
        {
            throw new RuntimeException(e);
        }

        Response response;
        InetAddress address = exchange.getSourceAddress();
        System.out.println("NOW TRYNA REGISTERING\n");
        System.out.println("HOST NAME: "+address.getHostName()+"\nHOST ADDRESS: "+address.getHostAddress());
        System.out.println("...");
        int success = -1;
        // Tryna connect to DB 
        try (Connection connection = DB.getDb())
        {
            System.out.println("CONNESSIONE STABILITA\n");
            try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO iot.actuator(ip, type) VALUES(?,?);")) 
            {
                String ip = address.toString().substring(1); // Rimuove il slash iniziale
                // 1 = Inet
                ps.setString(1, ip); //substring(1)
                // 2 = Tipo attuatore
                ps.setString(2, (String) obj.get("type"));
                    
                System.out.println("PREPARED STATEMENT CON INDIRIZZO: "+ ip +" TIPO ATTUATORE: "+(String) obj.get("type"));
                System.out.println("CERCO DI ESEGUIRE LA UPDATE\n");

                // Tryna execute UPDATE
                ps.executeUpdate();
                // Ritorna il numero di righe coinvolte
                success = ps.getUpdateCount();
                if(success>0)
                {
                    System.out.println("UPDATE ESEGUITA\n");
                    System.out.println("RIGHE COINVOLTE: "+success+"\n");
                    response = new Response(CoAP.ResponseCode.CREATED);
                }
                else
                {
                    System.out.println("ERRORE NELLA UPDATE UPDATE\n SUCCESS="+success);
                    response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            response = new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
        // Return number of raw coinvolte
        //return ps.getUpdateCount();
        exchange.respond(response);
    }
    
}
