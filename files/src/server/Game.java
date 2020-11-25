package server;

import com.google.gson.Gson;
import common.UtilityClass;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

// classe che rappresenta il match, con i due utenti e i vari parametri, utilizzo di un selettore
public class Game implements Runnable {

    // stringa di inizio game
    private static String welcome = "Via alla sfida di traduzione!\n" +
            "Avete " +  (UtilityClass.T2 / 1000) + " secondi per tradurre correttamente " +
            UtilityClass.K + " parole.\n" +
            "fine\n";

    /*
    chiavi del selettore "principale" del server dei due utenti che stanno per sfidarsi,
    necessarie perché posso ricavare tutti gli alttributi di essi, e rimetterli in modalita' lettura a fine game,
    (sono state poste con interestOps = 0 nel corpo principale del server), e se necessario, svegliare il selettore.
    */
    private SelectionKey mainKeyUserOne;
    private SelectionKey mainKeyUserTwo;

    /*
    il game tra due utenti viene gestito da un selettore, piuttosto che creare due thread uno per ogni utente.
    Dunque si avranno due nuove chiavi da registrare al selettore che gestisce il game tra i due user.
    */
    private Selector selector;
    private SelectionKey gameKeyUserTwo;
    private SelectionKey gameKeyUserOne;

    // parole del game, estratte dal dizionario
    private Dictionary gameWords;

    // hashmap degli utenti registrati, necessaria per scrivere il nuovo punteggio a fine game
    private ConcurrentHashMap<String, User> registeredUser;

    public Game(SelectionKey mainKeyUserOne, SelectionKey mainKeyUserTwo, String[] words,
                ConcurrentHashMap<String, User> registeredUser) throws IOException {

        // utenti registrati
        this.registeredUser = registeredUser;

        // per ripristinare le operazioni sui canali e l'altro selettore a fine game
        this.mainKeyUserOne = mainKeyUserOne;
        this.mainKeyUserTwo = mainKeyUserTwo;

        /*
        registro i due users al selettore del game,
        il selettore del server principale li ha momentanemante sospesi dal readySet (interestOps = 0)

        la classe UserItemInGame estende UserItem, aggiungendo i parametri del game:
        aggiunge l'indice della parola da tradurre e i punti del game attuale
        */
        selector = Selector.open();
        gameKeyUserOne = mainKeyUserOne.channel()
                .register(selector, SelectionKey.OP_WRITE,
                        new UserItemInGame(mainKeyUserOne, (UserItem) mainKeyUserOne.attachment()));

        gameKeyUserTwo = mainKeyUserTwo.channel()
                .register(selector, SelectionKey.OP_WRITE,
                        new UserItemInGame(mainKeyUserTwo, (UserItem) mainKeyUserTwo.attachment()));

        // parole del game, estratte dal dizionario
        gameWords = new Dictionary();
        gameWords.setDictionary(words);
    }

    private void start() throws IOException {

        /*
        per prima cosa il server invia la stringa di inizio match,
        seccessivamente prepara la prima query da inviare
        e fa iniziare il game, settando la durata massima del match
        */
        UtilityClass.makeMessage(gameKeyUserOne, welcome);
        UtilityClass.makeMessage(gameKeyUserTwo, welcome);
        UtilityClass.writeMessage(gameKeyUserOne);
        UtilityClass.writeMessage(gameKeyUserTwo);

        // prima parola che il selettore inviera'
        String s = "Challenge 1/" + UtilityClass.K + ": " + gameWords.get(0) + "\n";
        UtilityClass.makeMessage(gameKeyUserOne, s);
        UtilityClass.makeMessage(gameKeyUserTwo, s);

        /*
        quando nWords == 2*K (nWordsTot, parole da tradurre) il game termina termina (entrambi hanno finito).
        necessario per fare terminare il game oltre allo scadere del tempo
         */
        int nWords = 0, nWordsTot = 2 * UtilityClass.K;
        // tempo di inizio del game
        long start = System.currentTimeMillis();
        // ciclo nel quale il server invia le parole a analizza le risposte
        while (nWords < nWordsTot && System.currentTimeMillis() - start < UtilityClass.T2) {
            // tempo rimanente
            long remaining = UtilityClass.T2 - (System.currentTimeMillis() - start);
            /*
            unica operazione bloccante:
            se non ci sono canali pronti si blocca per al massimo la durata del match rimanente
            */
            selector.select(remaining);
            // iteratore sulle chiavi pronte
            Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
            // cicla finché l'iteratore non è vuoto, oppure finche' c'e' tempo
            while (readyKeys.hasNext() && System.currentTimeMillis() - start < UtilityClass.T2) {
                // chiave selezionata dal selettore/iteratore
                SelectionKey key = readyKeys.next();
                // rimozione
                readyKeys.remove();

                if (key.isWritable()) {
                    UserItemInGame item = (UserItemInGame) key.attachment();
                    UtilityClass.writeMessage(key);
                    // se l indice e' ultimo allora ha finito e attende l'avversario
                    if (item.getIndex() == UtilityClass.K) {
                        key.interestOps(0);
                    } else {
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }

                if (key.isReadable()) {
                    UserItemInGame item = (UserItemInGame) key.attachment();
                    // aumento contatore delle parole
                    nWords++;
                    // risposta alla query
                    String resp = UtilityClass.readMessage(key);
                    // punti attuali del utente nel game
                    int oldPoints = item.getGamePoints();
                    /*
                    indice che serve al server per controllare se l'utente ha tradotto correttamente la parola.
                    controlla se le traduzioni fornite dal servizio esterno al server
                    contengono la parola scritta dal client.
                    Il server si aspetta la traduzione per la parola in posizione i
                    */
                    int i = item.getIndex();
                    if (gameWords.isOk(i, resp)) {
                        // aggiungo X punti
                        item.setGamePoints(oldPoints + UtilityClass.X);
                        // incremento il counter delle parole corrette
                        item.setCorrectedAnswer(item.getCorrectedAnswer() + 1);
                    } else {
                        // decremento Y punti se la risposta e' sbagliata
                        item.setGamePoints(oldPoints - UtilityClass.Y);
                        // incremento contatore parole sbagliate
                        item.setIncorrectedAnswer(item.getIncorrectedAnswer() + 1);
                    }
                    // parola successiva
                    item.setIndex(i + 1);
                    i = item.getIndex();
                    /*
                    se index == K, l'utente ha finito, e se e' il primo ad aver finito,
                    ottiene il bonus "firstFinisher", altrimenti avanti con la prossima query
                    */
                    if (i < UtilityClass.K) {
                        // preparo nuova parola da inviare
                        String str = "Challenge " + (i + 1) + "/" + UtilityClass.K + ": " + gameWords.get(i) + "\n";
                        UtilityClass.makeMessage(key, str);

                        // ha finito per primo, attende l'avversario
                    } else if (nWords < nWordsTot) {
                        /* in caso di interruzzione forzata,
                         il server continua a leggere dal client disconnesso le parole nulle,
                          e verrebbe visto come primo finisher
                         */
                        if (resp != null) {
                            item.setFirstFinisher(true);
                            UtilityClass.makeMessage(key, "Hai finito, in attesa dell'avversario...\n");
                        }
                    }
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        }

        // aggiorna il punteggio degli utenti e prepara il messagio di terminazione partita
        updatePoints();

        // comunico l'esito ai giocatori
        UtilityClass.writeMessage(gameKeyUserOne);
        UtilityClass.writeMessage(gameKeyUserTwo);

        // ripristino le chiavi del vecchio selettore
        mainKeyUserOne.interestOps(SelectionKey.OP_READ);
        mainKeyUserTwo.interestOps(SelectionKey.OP_READ);
        mainKeyUserOne.selector().wakeup();

        // chiudo il selettore del game
        selector.close();
    }

    private void updatePoints() {
        /*
        Aggiorno e comunico i punteggi fatti ora in game sommati ai precedenti
        necessario sincornizzare perche' il campo punti di questo utente potrebbe essere richiesto da un amico nel
        corpo principale del server
        */

        UserItemInGame userItem1 = (UserItemInGame)gameKeyUserOne.attachment();
        UserItemInGame userItem2 = (UserItemInGame)gameKeyUserTwo.attachment();

        // query a cui non hanno risposto
        userItem1.setEmptyAnswer(UtilityClass.K - userItem1.getIndex());
        userItem2.setEmptyAnswer(UtilityClass.K - userItem2.getIndex());

        // l'utente con piu' punti vince la partita e si aggiundica i punti bonus
        int u1GamePoints = userItem1.getGamePoints(), u2GamePoints = userItem2.getGamePoints();
        if (u1GamePoints > u2GamePoints) {
            userItem1.setExtraPoints(UtilityClass.Z);
        } else if (u1GamePoints < u2GamePoints) {
            userItem2.setExtraPoints(UtilityClass.Z);

        /*
        se hanno totalizzato lo stesso punteggio, vince chi ha finito prima
        se hanno lo stesso punteggio, ma non hanno finito in tempo, non c'e' un vincitore
        */
        } else if (userItem1.isFirstFinisher()) {
            userItem1.setExtraPoints(UtilityClass.Z);
        } else if (userItem2.isFirstFinisher()){
            userItem2.setExtraPoints(UtilityClass.Z);
        }

        // aggiorno punteggi
        User u1, u2;
        synchronized (u1 = userItem1.getUser()) {
            u1.setPoint(u1GamePoints + userItem1.getExtraPoints() + u1.getPoint());
        }
        synchronized (u2 = userItem2.getUser()) {
            u2.setPoint(u2GamePoints + userItem2.getExtraPoints() + u2.getPoint());
        }
        // salvo il nuovo punteggio degli utenti
        UtilityClass.writeToFile(new Gson().toJson(registeredUser), "users.json");

        // messaggio di fine da inviare agli utenti
        String s1 = makeFinalMessage(userItem1, userItem2);
        String s2 = makeFinalMessage(userItem2, userItem1);

        // chi ha guadagnato i punti bonus ha vinto il game
        if (userItem1.getExtraPoints() > userItem2.getExtraPoints()) {
            s1 += "Congratulazioni, hai vinto! Hai guadagnato " + UtilityClass.Z + " punti extra, per un totale di " +
                    (UtilityClass.Z + u1GamePoints) + " punti!\n" + "fine\n";
            s2 += "fine\n";
        } else if (userItem2.getExtraPoints() > userItem1.getExtraPoints()) {
            s2 += "Congratulazioni, hai vinto! Hai guadagnato " + UtilityClass.Z + " punti extra, per un totale di " +
                    (UtilityClass.Z + u2GamePoints) + " punti!\n" + "fine\n";
            s1 += "fine\n";

        // hanno lo stesso punteggio e non hanno finito in tempo
        } else {
            s1 += "Nessun vincitore.\n" + "fine\n";
            s2 += "Nessun vincitore.\n" + "fine\n";
        }

        // preparo messaggi da inviare
        UtilityClass.makeMessage(gameKeyUserOne, s1);
        UtilityClass.makeMessage(gameKeyUserTwo, s2);
    }

    private String makeFinalMessage(UserItemInGame userItem, UserItemInGame userItem2) {
        return "Partita terminata!\n" +
                "Hai tradotto correttamente " + userItem.getCorrectedAnswer() + " parole.\n" +
                "Ne hai sbagliate "  + userItem.getIncorrectedAnswer() +
                " e non hai risposto a " + userItem.getEmptyAnswer() + ".\n" +
                "Hai totalizzato " + userItem.getGamePoints() + " punti.\n" +
                "Il tuo avversario ha totalizzato " + userItem2.getGamePoints() +
                " punti.\n";
    }

    @Override
    public void run() {
        try {
            System.out.println("partita iniziata");
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
