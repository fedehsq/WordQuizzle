package server;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.UtilityClass;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    // canale del server
    private ServerSocketChannel serverSocketChannel;
    // selettore che seleziona i canali pronti
    private Selector selector;
    // serializza e deserializza
    private Gson gson;
    // gestisce richieste di sfida
    private ThreadPoolExecutor requests;
    // gestisce le sfide
    private ThreadPoolExecutor games;
    // utenti registrati
    private ConcurrentHashMap<String, User> registeredUsers;
    // utenti connessi
    private HashMap<String, UserItem> connectedUsers;
    // parole italiane estratte durante la configurazione del server
    private String[] italianWords;

    /**
     * Crea il server NIO
     */
    public Server() {
        // legge i parametri (N, K, ...) da un file di configurazione
        UtilityClass.readConfig();
        // serializzazione/deserializzazione json
        gson = new Gson();
        // necessario per Gson
        Type hashSetType = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
        // deserializza gli utenti registrati al servizio, se il file esiste e non è vuoto
        registeredUsers = gson.fromJson(UtilityClass.readFromFile("users.json"), hashSetType);
        // se file json vuoto
        if (registeredUsers == null) {
            registeredUsers = new ConcurrentHashMap<>();
        }
        // utenti attualmente connessi
        connectedUsers = new HashMap<>();
        // gestisce richieste di sfida
        requests = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        // gestisce sfide
        games = (ThreadPoolExecutor)Executors.newCachedThreadPool();
        // inizializzazione dizionario
        italianWords = new String[UtilityClass.N];
        fillDictionary();
    }

    private void fillDictionary() {
        // riempie il dizionario prendendo le parole italiane da un file testuale
        String words = UtilityClass.readFromFile("words.txt");
        if (words != null) {
            // inserisce nel dizionario N parole italiane
            StringTokenizer token = new StringTokenizer(words);
            for (int i = 0; i < UtilityClass.N && token.hasMoreTokens(); i++) {
                italianWords[i] = token.nextToken();
            }
        }
    }

    /**
     * apre il serverSocket channel e fa la bind
     */
    private void configure() {
        try {
            // server NIO
            serverSocketChannel = ServerSocketChannel.open();
            // "collega" sulla porta 8888
            serverSocketChannel.bind(new InetSocketAddress("localhost", 8888));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registra il server con un selettore
     */
    private void registerWithSelector() {
        try {
            // spre selettore
            selector = Selector.open();
            // per essere usato col selettore, dev'essere non bloccante
            serverSocketChannel.configureBlocking(false);
            // registrazione server col selettore per op di accettazione connessioni
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inizializza il servizio di registrazione
     */
    private void acceptRegistration() {
        // pronto a ricevere richieste di registrazione
        try {
            // Creazione di un'istanza dell'oggetto RegisteredImpl
            RegisterImplementation register = new RegisterImplementation(registeredUsers);
            // Esportazione dell'Oggetto tramite interfaccia
            RegisterInterface stub = (RegisterInterface) UnicastRemoteObject.exportObject(register, 0);
            // Creazione di un registry sulla porta 8080
            LocateRegistry.createRegistry(8080);
            Registry r = LocateRegistry.getRegistry(8080);
            // Pubblicazione dello stub nel registry
            r.rebind("REGISTRATION-SERVER", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void disconnect(SelectionKey key) throws IOException {
        UserItem item = (UserItem) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        // controllo se voleva disconnettersi o eventuali errori forzati
        User user = item.getUser();
        // cluent chiusura forzata
        if (user != null) {
            // l'utente era loggato
            connectedUsers.remove(user.getUsername());
        }
        key.cancel();
        client.close();
    }

    /**
     * Legge i messaggi dei client
     * @param key chiave del client
     * @throws IOException errori IO
     */
    private void readMessage(SelectionKey key) throws IOException {
        UserItem item = (UserItem) key.attachment();
        // messaggio ricevuto dal client
        String message = UtilityClass.readMessage(key);
        // se returna null vuol dire che il client ha disconnesso di forza
        if (message == null) {
            disconnect(key);
            return;
        }
        // tokenizzo il messaggio:
        StringTokenizer stringTokenizer = new StringTokenizer(message);
        // controllo cosa desidera il client
        String request = stringTokenizer.nextToken();
        System.out.println(request);
        // stringa di risposta
        String s = "";
        // per tutte le operazione, tranne login, l'user dev'essere connesso
        if (!item.isConnected()) {
            if (!request.equals("login")) {
                s = "Per usufruire dei servizi, devi accedere\n";
                UtilityClass.makeMessage(key, s);
                return;
            }
        }
        switch (request) {
            case "login": {

                // controllo parametri
                int n = stringTokenizer.countTokens();
                if (n != 2) {
                    s = "Parametri invalidi, usa login <username password>\n";
                    break;
                }
                // salvo username
                String username = stringTokenizer.nextToken();
                // salvo password
                String password = stringTokenizer.nextToken();
                // l'utente e' gia' connesso in questo client
                if (item.isConnected()) {
                    s = "Sei gia' connesso con username: " + item.getUser().getUsername() + "\n";
                    break;
                }
                // controllo se e' presente la chiave "username", ovvero gia' connesso in un altro client
                if (connectedUsers.containsKey(username)) {
                    s = "L'utente " + username + " e' gia' connesso\n";
                    break;
                }
                /*
                si ha un accesso concorrente a questa struttura, in fase di registrazione e nel game,
                ma gestisce in automarico la sincronizzazione
               */
                User user = registeredUsers.get(username);
                // utente non registrato
                if (user == null) {
                    s = "Username inesistente\n";
                    break;
                }
                // controllo che la password sia corretta
                if (user.getPassword().equals(password)) {
                    item.setUser(user);
                    item.setConnected(true);
                    connectedUsers.put(username, item);
                    s = "Login eseguito con successo\n";
                } else {
                    // password inserita non corretta
                    s = "Password errata\n";
                }
                break;
            }
            case "logout": {
                // nessun altro parametro oltre alla richiesta
                if (stringTokenizer.hasMoreTokens()) {
                    s = "Parametri invalidi, usa 'logout'\n";
                    break;
                }
                s = "Logout eseguito con successo\n";
                // lo rimuovo dalla lista
                connectedUsers.remove(item.getUser().getUsername());
                // e lo disconnetto (la socket viene chiusa dopo che gli ho mandato l'esito)
                item.setConnected(false);
                break;
            }
            case "aggiungi_amico": {
                // controllo parametri
                if (stringTokenizer.countTokens() != 1) {
                    s = "Parametri invalidi, usa 'aggiungi_amico <friendname>'\n";
                    break;
                }
                // salvo il nomeutente dell'amico
                String friendName = stringTokenizer.nextToken();
                // se mi autoaggiungo errore
                if (friendName.equals(item.getUser().getUsername())) {
                    s = "Non puoi aggiungerti come amico...\n";
                    break;
                }
                /*
                si ha un accesso concorrente a questa struttura, in fase di registrazione e nel game,
                ma gestisce in automarico la sincronizzazione
                */
                // controlla se l'amico che si vuole aggiungere sia registrato
                User friend = registeredUsers.get(friendName);
                if (friend == null) {
                    s = "Utente non registrato\n";
                    break;
                }
                // controllo che non siano gia' amici
                if (!item.getUser().getFriends().contains(friendName)) {
                    // aggiunta bidirezionale
                    item.getUser().getFriends().add(friendName);
                    friend.getFriends().add(item.getUser().getUsername());
                    // salvo su file
                    UtilityClass.writeToFile(gson.toJson(registeredUsers), "users.json");
                    s = "Amicizia creata\n";
                } else {
                    s = "Gia' amici\n";
                }
                break;
            }
            case "lista_amici": {
                // nessun altro parametro oltre alla richiesta
                if (stringTokenizer.hasMoreTokens()) {
                    s = "Parametri invalidi, usa 'lista_amici'\n";
                    break;
                }
                s = "{\"Amici\":" + gson.toJson(item.getUser().getFriends()) + "}\n";
                break;
            }
            // sfida puo' essere inoltrata solo ad un amico online
            case "sfida": {
                // controllo parametri
                if (stringTokenizer.countTokens() != 1) {
                    s = "Parametri invalidi, usa 'sfida <friendname>'";
                    break;
                }
                String friendName = stringTokenizer.nextToken();
                // controllo se sono amici
                if (item.getUser().getFriends().contains(friendName)) {
                    // controllo che l'amico sia online
                    UserItem friend = connectedUsers.get(friendName);
                    if (friend == null) {
                        s = friendName + " non connesso\n";
                        break;
                    }
                    // estraggo l'indirizzo dell'amico
                    SocketAddress destinationAddress = connectedUsers.get(friendName).getAddress();
                    item.setGameRequest(true);
                    /*
                    il canale del client dopo questa richiesta viene messo momentaneamente con interestOps = 0,
                    (nell'iterazione successiva grazie alla variabile "gameRequest").
                    Il messaggio successivo sara' l'esito della richiesta da lui inviata all'amico.
                    Passo la richiesta al threadpool per essere elaborata (inviata tramite UDP all'amico).
                    Allo scadere del timeout, o in caso di acceattazione / rifiuto, questo thread scrivera'
                    l'esito sul canale del client richiedente.
                    */
                    requests.execute(new ServerRequest(friendName, destinationAddress, key));
                    s = "In attesa di accettazione\n";
                } else {
                    s = "Non siete amici\n";
                }
                break;
            }
            // questo caso viene inviato in automatico dal client dopo che l'amico ha accettato la sfida
            case "start": {
                String friend = stringTokenizer.nextToken();
                // controllo che sia sempre online
                if (connectedUsers.containsKey(friend)) {
                    SelectionKey friendKey = connectedUsers.get(friend).getKey();
                    /*
                    passo i due channel degli sfidanti al Thread del pool atto alla gestione delle sfide
                    e metto le key con ops = 0
                    */
                    key.interestOps(0);
                    friendKey.interestOps(0);
                    // inizia il match tra i due utenti
                   games.execute(new Game(key, friendKey, italianWords, registeredUsers));
                } else {
                    s = "Amico non piu' online";
                }
                break;

            }
            case "mostra_punteggio": {
                // nessun altro parametro oltre alla richiesta
                if (stringTokenizer.hasMoreTokens()) {
                    s = "Parametri invalidi, usa 'mostra_punteggio\n";
                    break;
                }
                s = "Punteggio: " + item.getUser().getPoint() + "\n";
                break;
            }
            case "mostra_classifica": {
                // nessun altro parametro oltre alla richiesta
                if (stringTokenizer.hasMoreTokens()) {
                    s = "Parametri invalidi, usa 'mostra_classifica'\n";
                    break;
                }
                User user = item.getUser();
                // estrae i punteggi degli amici e li ordina in modo decrescente
                ArrayList<User> friends = new ArrayList<>();
                friends.add(user);
                // aggiunge gli amici
                for (String friend : user.getFriends()) {
                    friends.add(registeredUsers.get(friend));
                }
                // ordina usando il punteggio come ordine
                friends.sort(new User.SortByPoint());
                // estraggo dagli utenti amici solo i campi nome e punteggio
                ArrayList<String> ranking = new ArrayList<>();
                for (User friend : friends) {
                    User u;
                    /*
                    se fosse in game potrebbe essere in aggiornamento il suo punteggio,
                    necessario sincronizzare
                    */
                    synchronized (u = friend) {
                        ranking.add(u.getUsername() + " " + u.getPoint());
                    }
                }
                s = "{\"Classifica\":" + gson.toJson(ranking) + "}\n";
                break;
            }
            case "amici_online": {
                // nessun altro parametro oltre alla richiesta
                if (stringTokenizer.hasMoreTokens()) {
                    s = "Parametri invalidi, usa 'amici_online'\n";
                    break;
                }
                // controlla quali amici sono online
                ArrayList<String> friends = new ArrayList<>();
                for (String friend : item.getUser().getFriends()) {
                    if (connectedUsers.containsKey(friend)) {
                        friends.add(friend);
                    }
                }
                s = "{\"Amici online\":" + gson.toJson(friends) + "}\n";
                break;
            }
            default:
                s = "Operazione invalida\n";
                break;
        }
        // se l'utente non e' in game
        if (key.interestOps() != 0) {
            UtilityClass.makeMessage(key, s);
        }
    }

    /**
     * accetta connessioni dai client, e li serve
     */
    private void start() throws IOException {
        // registra tramite RMI
        acceptRegistration();
        // ciclo nel quale il server elabora le richieste dei vari client
        for (;;) {
            // unica operazione bloccante: se non ci sono canali pronti
            selector.select();
            // iteratore sulle chiavi prontr
            Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
            // cicla finché l'iteratore non è vuoto
            while (readyKeys.hasNext()) {
                // chiave selezionata dal selettore/iteratore
                SelectionKey key = readyKeys.next();
                // rimozione
                readyKeys.remove();
                // qualche client richiede la connessione
                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    /*
                    la connessione viene instaurata,
                    all'iterazione successiva il server verifica se l'utente è registrato
                     */
                    SocketChannel client = serverSocketChannel.accept();
                    client.configureBlocking(false);
                    // il primo messaggio arrivera' dal client
                    SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
                    // key necessaria per sfida
                    clientKey.attach(new UserItem(clientKey));

                    // un client ha scritto sul canale l'operazione
                } else if (key.isReadable() && key.isValid()) {
                    // legge la richiesta del client
                    readMessage(key);
                    // prepara per la risposta
                    if (key.isValid()) {
                        if (key.interestOps() != 0) {
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                } else if (key.isWritable() && key.isValid()) {
                    UserItem item = (UserItem) key.attachment();
                    // retituisce true se e' riuscito a scrivere sul canale, false in caso di chiusura brutale
                    boolean writen = UtilityClass.writeMessage(key);
                    /*
                    chiusura forzata dal client oppure l'utente ha richiesto la disconessione
                    oppure ha sbagliato le credenziali, se non c'e' stata chiusura forzata,
                    viene settata la variabile connected a false nell'iterazione precedente in lettura,
                    e adesso viene realmente disconesso e rimosso
                    */
                    if (!writen || !item.isConnected()) {
                        disconnect(key);
                    }
                    /*
                    l'utente e' in attesa di una risposta da parte dell'amico,
                    quindi il canale momentaneamnte non ha ops!
                    viene settato in modalita' scrittura quando si avra' l'esito della richiesta
                    */
                     else if (item.isGameRequest()) {
                        key.interestOps(0);
                    } else {
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        /*
        all'avvio il server deserializza il file json contentente gli utenti regitstrati,
        li mette in un HashSet e lo passa al server RMI che controllera' se chi si vuol registrare
        richiede la registrazione con un username già utilizzato
        */
        Server server = new Server();
        server.configure();
        server.registerWithSelector();
        server.start();
    }
}