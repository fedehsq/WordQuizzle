package cli;
import client.Client;
import client.TimeOutDatagramPacket;
import com.google.gson.JsonObject;
import common.UtilityClass;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.StringTokenizer;

// test che ricalca l'esempio fornito nella consegna del progetto
public class ManualClient extends Client {

    // per leggere da tastiera
    private BufferedReader reader;

    private static String usage = "usage : COMMAND [ ARGS ...] (Digita: 'wq --help' per i comandi disponibili)\n" +
            "Commands:\n" +
            "'registra_utente nickUtente password': registra l'utente\n" +
            "'login nickUtente password': effettua il login\n" +
            "'logout': effettua il logout\n" +
            "'aggiungi_amico nickAmico': crea relazione di amicizia con nickAmico\n" +
            "'lista_amici': mostra la lista dei propri amici\n" +
            "'sfida nickAmico': richiesta di una sfida a nickAmico\n" +
            "'mostra_punteggio': mostra il punteggio dell’utente\n" +
            "'mostra_classifica': mostra una classifica degli amici dell’utente (incluso l’utente stesso)\n" +
            "'mostra_richieste': mostra le richieste di sfida da parte degli amici\n" +
            "'amici_online': mostra gli amici online che e' possibile sfidare\n" +
            "'esci': termina il programma";

    public ManualClient() {
        super();
        reader = new BufferedReader(new InputStreamReader(System.in));
    }


    /**
     * Match tra due utenti, ovverride della superclasse metodo astratto
     */
    @Override
    public void game() {
        System.out.println("Preparazione sfida in corso...");
        try {
            String send, receive;
            // messaggio di start dal server(stringa di welcome)
            while (!(receive = getIn().readLine()).equals("fine")) {
                System.out.println(receive);
            }

            // thread che mi interrompe dopo K secondi se non ho finito (tempo scaduto)
            Thread timer = new Thread(new Timer());
            timer.start();

            while (true) {
                // attendo le parole del server e le stampo
                receive = getIn().readLine();
                System.out.println(receive);
                // se ho finito, oppure se la partita e' terminata, il mess successivo sara' di nuovo del server
                if (receive.startsWith("Hai finito")) {
                    // finito per primo, interrompo il thread "timer"
                    timer.interrupt();
                    continue;
                } else if (receive.startsWith("Partita terminata")) {
                    // game finito da entrambi in tempo
                    timer.interrupt();
                    // esito dal server
                    while (!(receive = getIn().readLine()).equals("fine")) {
                        System.out.println(receive);
                    }
                    // torna al menu'
                    System.out.println(("torno al menu'"));
                    return;
                }
                // richiesta risposta alla query
                System.out.print("> ");
                //legge da input
                send = reader.readLine();

                // se non e' ancora scaduto il tempo invio la risposta
                if (timer.isAlive()) {
                    getOut().writeUTF(send);
                }
            }
        } catch (IOException e) {
            System.out.println("Server offline");
        }
    }


    /**
     * Il client si avvia e prova ad iniziare una sessione
     */
    private void start() throws IOException {
        System.out.println(usage);
        String send = "";
        while (!send.equals("esci")) {
            System.out.print("> ");
            //legge da input
            send = reader.readLine();
            // per "splittare" la richiesta
            StringTokenizer stringTokenizer = new StringTokenizer(send);
            if (stringTokenizer.countTokens() == 0) {
                System.out.println("Non si e' richiesta nessuna operazione");
                continue;
            }
            // la prima stringa è l'operazione
            String request = stringTokenizer.nextToken();

            /*
            controllo di non aver già instaurato una connessione, se non sono connesso,
            e la prima operazione non e' login, registra_utente, o esci, stampa il messaggio
             */
            if (!isConnected()
                    && !request.equals("login")
                    && !request.equals("registra_utente")
                    && !request.equals("esci")) {

                System.out.println("Per usufruire dei servizi, devi accedere");
                continue;
            }

            switch (request) {
                case "wq": {
                    if (!stringTokenizer.hasMoreTokens()) {
                        System.out.println("usa 'wq --help' per visualizzare le operazioni disponibili");
                    }
                    if (stringTokenizer.nextToken().equals("--help")) {
                        System.out.println(usage);
                    } else {
                        System.out.println("usa 'wq --help' per visualizzare le operazioni disponibili");
                    }
                    break;
                }
                case "registra_utente": {
                    // controllo di non aver già instaurato una connessione
                    if (isConnected()) {
                        System.out.println("Per registrare un altro utente, prima devi disconnetterti");
                        break;
                    }
                    int n = stringTokenizer.countTokens();
                    // non ho passato i corretti argomenti
                    if (n == 0 || n > 2) {
                        System.out.println("Parametri invalidi");
                    } else if (n == 1) {
                        // passata password vuota, l'eccezione viene gestita nel server
                        System.out.println(register(stringTokenizer.nextToken(), ""));
                    } else {
                        // parametri corretti
                        String username = stringTokenizer.nextToken();
                        String password = stringTokenizer.nextToken();
                        System.out.println(register(username, password));
                    }
                    break;
                }
                case "login": {
                    // controllo parametri
                    if (stringTokenizer.countTokens() != 2) {
                        System.out.println("Parametri invalidi, digita 'login <username password>'");
                    } else {
                        // richiede e stampa risultato
                        System.out.println(login(stringTokenizer.nextToken(), stringTokenizer.nextToken()));
                    }
                    break;

                }
                case "logout": {
                    // controllo parametri
                    if (stringTokenizer.hasMoreTokens()) {
                        System.out.println("Parametri invalidi, digita 'logout'");
                    } else {
                        // richiede e stampa risultato
                        System.out.println(logout());
                    }
                    break;
                }
                case "lista_amici": {
                    // controllo parametri
                    if (stringTokenizer.hasMoreTokens()) {
                        System.out.println("Parametri invalidi, digita 'lista_amici'");
                    } else {
                        // oggetto json, richiede e stampa risultato
                        JsonObject obj = listFriends();
                        System.out.println(myFromJsonArray("Amici", obj.getAsJsonArray("Amici")));
                    }
                    break;
                }
                case "mostra_punteggio": {
                    // controllo parametri
                    if (stringTokenizer.hasMoreTokens()) {
                        System.out.println("Parametri invalidi, digita 'mostra_punteggio'");
                    } else {
                        // richiede e stampa risultato
                        System.out.println(showPoints());
                    }
                    break;
                }
                case "mostra_classifica": {
                    // controllo parametri
                    if (stringTokenizer.hasMoreTokens()) {
                        System.out.println("Parametri invalidi, digita 'lista_amici'");
                    } else {
                        // oggetto json, richiede e stampa risultato
                        JsonObject obj = showRanking();
                        System.out.println(myFromJsonArray("Classifica", obj.getAsJsonArray("Classifica")));
                    }
                    break;
                }

                case "amici_online":{
                    // controllo parametri
                    if (stringTokenizer.hasMoreTokens()) {
                        System.out.println("Parametri invalidi, digita 'amici_online'");
                    } else {
                        // oggetto json, richiede e stampa risultato
                        JsonObject obj = onlineFriends();
                        System.out.println(myFromJsonArray("amici_online", obj.getAsJsonArray("Amici online")));
                    }
                    break;
                }
                case "aggiungi_amico": {
                    // controllo parametri
                    if (stringTokenizer.countTokens() != 1) {
                        System.out.println("Parametri invalidi, digita 'aggiungi_amico <username>'");
                    } else {
                        // richiede e stampa risultato
                        System.out.println(addFriend(stringTokenizer.nextToken()));
                    }
                    break;

                }
                case "sfida": {
                    // controllo parametri
                    if (stringTokenizer.countTokens() != 1) {
                        System.out.println("Parametri invalidi, digita 'sfida <username>'");
                    } else {
                        String receive = match(stringTokenizer.nextToken());
                        System.out.println(receive);
                        if (receive.endsWith("accettazione")) {
                            // sospendo finché non arriva l'esito dall'amico, mi verra' comunicato l'esito dal server
                            receive = getIn().readLine();
                            System.out.println(receive);
                            // se l'amico ha accettato, inzia la sfida
                            if (receive.contains("ha accettato,")) {
                                // invio messaggio al server che fa partire la sfida
                                String friend = new StringTokenizer(receive).nextToken();
                                getOut().writeUTF("start " + friend);
                                game();
                            }
                        }
                    }
                    break;
                }
                case "mostra_richieste": {
                    /*
                    metodo aggiuntivo il quale mostra le richieste di sfida da parte degli amici.
                    Le richieste sono memorizzate in una coda condivisa col thread atto alla ricezione
                    dei datagramma di richiesta.
                    Vengono mostrate le richieste con etichetta di validita'/non validita'.
                    Vengono poi rimosse in caso di rifiuto o se sono scadute
                     */
                    if (stringTokenizer.hasMoreTokens()) {
                        System.out.println("Parametri invalidi");
                        break;
                    }
                    // coda condivisa, lock
                    synchronized (getRequests()) {
                        ArrayList<TimeOutDatagramPacket> requests = getRequests();
                        if (requests.isEmpty()) {
                            System.out.println("Nessuna richiesta");
                            break;
                        }
                        for (int i = 0; i < requests.size(); i++) {
                            // pacchetto UDP contenente richiesta
                            TimeOutDatagramPacket p = requests.get(i);
                            // contiene il nome dello sfidante
                            String friendName = new String(p.getPacket().getData(), 0, p.getPacket().getLength(), StandardCharsets.UTF_8);
                            // controllo se la richiesta e' ancora valida
                            if (System.currentTimeMillis() - p.getKeepAlive() > UtilityClass.T1) {
                                System.out.println("Scaduta: richiesta di sfida da parte di: " + friendName);
                                requests.remove(i--);
                            } else {
                                System.out.println("Valida: richiesta di sfida da parte di: " + friendName);
                            }
                        }
                    }
                    /*
                    rilascio la lock della struttura dati mentre si decide chi sfidare/rifiutare,
                    così da non perdere eventuali altre richieste di sfida in arrivo
                    */
                    System.out.print("Seleziona una richiesta valida digitando il nome dell'avversario,\n" +
                            "oppure digita 'no <name>' per rifiutare,\n" +
                            "oppure premi 'invio' per tornare al menu'\n> ");
                    String message = reader.readLine();
                    synchronized (getRequests()) {
                        ArrayList<TimeOutDatagramPacket> requests = getRequests();
                        // per "splittare" l input
                        StringTokenizer tokenizer = new StringTokenizer(message);
                        // torna al menu
                        if (tokenizer.countTokens() == 0) {
                            break;
                        }
                        // cosa si e' richiesto
                        String s = tokenizer.nextToken();
                        // caso rifiuto richiesta
                        if (s.startsWith("no")) {
                            // controllo che esista la richiesta da rifiutare
                            String name = tokenizer.nextToken();
                            for (int i = 0; i < requests.size(); i++) {
                                // pacchetto UDP contenente richiesta
                                TimeOutDatagramPacket p = requests.get(i);
                                // cerco il nome dello sfidante
                                if (p.getSenderName().equals(name)) {
                                    // rifiuta
                                    getDatagramSocket().send(new DatagramPacket("no".getBytes(), 0, 2,
                                            p.getPacket().getSocketAddress()));
                                    // rimuove da coda
                                    requests.remove(i);
                                    System.out.println("Richiesta rifiutata");
                                    break;
                                }
                            }
                            continue;
                        }
                    }
                    // flag che fara'� iniziare il game
                    boolean found = false;
                    synchronized (getRequests()) {
                        ArrayList<TimeOutDatagramPacket> requests = getRequests();
                        // se l'input non inizia con 'no', si e' accettata una richiesta
                        for (int i = 0; i < requests.size(); i++) {
                            // controllo che esista la richiesta da rifiutare
                            TimeOutDatagramPacket p = requests.get(i);
                            // cerco il nome dello sfidante
                            if (p.getSenderName().equals(message)) {
                                if ((System.currentTimeMillis() - p.getKeepAlive()) < UtilityClass.T1) {
                                    // accetta
                                    getDatagramSocket().send(new DatagramPacket("ok".getBytes(), 0, 2,
                                            p.getPacket().getSocketAddress()));
                                    // rimuove richiesta
                                    requests.remove(i);
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (found) {
                    //  inizia sfida
                        game();
                    } else {
                        System.out.println("Nessuna richiesta balida da parte di " + message);
                    }
                    break;
                }
                case "esci": {
                    System.exit(0);
                }
                default: {
                    System.out.println("Operazione invalida: usa 'wq --help' per visualizzare le operazioni disponibili");
                }
            }
        }

    }

    public static void main (String[]args) throws IOException {
        UtilityClass.readConfig();
        ManualClient client = new ManualClient();
        client.start();

    }
}
