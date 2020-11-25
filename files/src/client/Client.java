package client;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import server.RegisterInterface;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

public abstract class Client implements Operations {
    // per connessione con server
    private Socket socket;
    // per input dal server
    private BufferedReader in;
    // verso il server
    private DataOutputStream out;
    // variabile di connessione
    private boolean connected;
    // thread che gestisce le richieste di sfida
    private Thread requester;
    // coda nella quale vengono inserite dal thread le richieste di sfida
    private ArrayList<TimeOutDatagramPacket> requests;
    // socket per ricevere/inviare accettazione sfide
    private DatagramSocket datagramSocket;

    public Client() {
        datagramSocket = null;
        socket = null;
        in = null;
        out = null;
        connected = false;
    }

    private void startService() throws SocketException {
        // esito positivo, utente connesso
        connected = true;
        // richieste di sfida da parte degli amici
        requests = new ArrayList<>();
        // per inviare risposta di accettazione/rifiuto sfide al server
        datagramSocket = new DatagramSocket(socket.getLocalSocketAddress());
        // thread "gestore" delle richieste di sfida
        requester = new Thread(new RequestListner(datagramSocket, requests));
        requester.start();
    }

    // metodo diverso a seconda del tipo di client di test
    public abstract void game() throws IOException;

    public BufferedReader getIn() {
        return in;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public boolean isConnected() {
        return connected;
    }

    public ArrayList<TimeOutDatagramPacket> getRequests() {
        return requests;
    }

    public DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    /**
     * Chiude la connessione TCP del client col server
     */
    private void close() throws IOException {
        in.close();
        out.close();
        socket.close();
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    /**
     * Registrazione del client al server fornendo le proprie credenziali
     *
     * @param username richiesto dal client
     * @param password scelta dal client
     */
    @Override
    public String register(String username, String password) {
        String fromServer = null;
        try {
            // Returns a reference to the remote object Registry for the local host on the specified port
            Registry r = LocateRegistry.getRegistry(8080);
            // Returns the remote reference bound to the specified name in this registry
            Remote RemoteObject = r.lookup("REGISTRATION-SERVER");
            RegisterInterface serverObject = (RegisterInterface) RemoteObject;
            if (serverObject.register(username, password)) {
                fromServer = "Registrazione eseguita con successo";
            }
        } catch (Exception e) {
            // controllo se l'eccezzione è di tipo closed server
            if (e instanceof java.rmi.ConnectException) {
                fromServer = "Server offline";
            } else {
                fromServer = e.toString();
            }
        }
        return fromServer;
    }

    @Override
    public String login(String username, String password) {
        String fromServer;
        try {
            // se non sono gia' connesso apro socket e mi connetto momentaneamente
            if (!connected) {
                socket = new Socket();
                // connesione al server
                socket.connect(new InetSocketAddress("localhost", 8888));
                // stream
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new DataOutputStream(socket.getOutputStream());
            }
            // richiede login (se gia' connesso l'errore sara' dato dal server)
            out.writeUTF("login " + username + " " + password);
            // risposta
            fromServer = in.readLine();
            if (fromServer.endsWith("successo")) {
                startService();
                // se non ero gia' connesso ed ho avuto qualche errore
            } else if (!connected) {
                close();
            }
        } catch (IOException e) {
            fromServer = "Server offline";
        }
        return fromServer;
    }

    @Override
    public String addFriend(String friendName) {
        try {
            out.writeUTF("aggiungi_amico " + friendName);
            return in.readLine();
        } catch (IOException e) {
            return "Server offline";
        }
    }

    @Override
    public String match(String friendName) {
        try {
            out.writeUTF("sfida " + friendName);
            return in.readLine();
        } catch (IOException e) {
            return "Server offline";
        }
    }

    @Override
    public String logout() {
        String s;
        try {
            out.writeUTF("logout");
            s = in.readLine();
            connected = false;
            close();
        } catch (IOException e) {
            s = "Server offline";
        }
        return s;
    }

    @Override
    public JsonObject listFriends()  {
        try {
            out.writeUTF("lista_amici");
            // la stringa e' come ogetto JSON
            return new Gson().fromJson(in.readLine(), JsonObject.class);
        } catch (IOException e) {
            return new Gson().fromJson("{\"key\":" + "\"Server offline\"}", JsonObject.class);
        }


    }

    @Override
    public JsonObject onlineFriends() {
        try {
            out.writeUTF("amici_online");
            // la stringa e' come ogetto JSON
            return new Gson().fromJson(in.readLine(), JsonObject.class);
        } catch (IOException e) {
            return new Gson().fromJson("{\"key\":" + "\"Server offline\"}", JsonObject.class);
        }
    }

    @Override
    public String showPoints()  {
        try {
            out.writeUTF("mostra_punteggio");
            return in.readLine();
        } catch (IOException e) {
            return "Server offline";
        }
    }

    @Override
    public JsonObject showRanking() {
        try {
            out.writeUTF("mostra_classifica");
            // la stringa e' come ogetto JSON
            return new Gson().fromJson(in.readLine(), JsonObject.class);
        } catch (IOException e) {
            return new Gson().fromJson("{\"key\":" + "\"Server offline\"}", JsonObject.class);
        }

    }

    // converte un array json in una stringa ben formattata
    public String myFromJsonArray(String key, JsonArray jsonElements) {
        if (jsonElements == null) {
            return "Server offline";
        }
        StringBuilder s = new StringBuilder(key + ": ");
        int i;
        for (i = 0; i < jsonElements.size() - 1; i++) {
            String str = jsonElements.get(i).getAsString() + ", ";
            s.append(str);
        }
        s.append(jsonElements.get(i).getAsString());
        return s.toString();
    }

}