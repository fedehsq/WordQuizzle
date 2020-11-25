package cli;
import client.Client;
import com.google.gson.JsonObject;
import common.UtilityClass;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
    Questa classe e' stata creata esclusivamente per testare la concorrenza e solidità del programma.

    classe che simula in automatico diverse sessioni di utenti sul server.
    prima si registrano 1024 utenti in contemporanea,
    successivamente 128 eseguono il login, dove i primi 64 attendono di essere sfidati
    e a partita conclusa richiedono tutti gli altri servizi offerti dal server.
    Altri 64 eseguono il login, aggiungono un amico di cui sopra e lo sfidano
    Si hanno 3 threadpool, rispettivamente da 1024 threads per la registrazioni,
    e altri 2 da 64 per la sessione tcp col server
    con numeri piu' grandi si hanno errori TCP dovuti all'operazione di connect (backlog piena del server) e
    di conseguenza fallisce (a meno di non aspettare un intervallo di tempo discreto tra una connect e l'altra)
 */


public class AutoClient extends Client {

    public AutoClient() {
        super();
    }

    /**
     * Match tra due utenti, ovverride della superclasse metodo astratto
     */
    @Override
    public void game() {
        try {
            // parte la partita
            String receive, send;
            while (!(receive = getIn().readLine()).equals("fine")) {
                System.out.println(receive);
            }

            while (true) {
                receive = getIn().readLine();
                System.out.println(receive);
                // se ho finito, oppure se la partita e' terminata il mess successivo sara' di nuovo del server
                if (receive.startsWith("Hai finito")) {
                    continue;
                } else if (receive.startsWith("Partita terminata")) {
                    while (!(receive = getIn().readLine()).equals("fine")) {
                        System.out.println(receive);
                    }
                    return;
                }
                // parola inviata a caso
                send = "ciao";
                getOut().writeUTF(send);

            }
            // caso crash server
        } catch (IOException e) {
            System.out.println("Server offline");
            System.exit(-1);
        }
    }



    public static void main(String[] args) throws InterruptedException {
        UtilityClass.readConfig();

        // per registrare
        ThreadPoolExecutor p0 = (ThreadPoolExecutor) Executors.newFixedThreadPool(1024);
        // user
        ThreadPoolExecutor p1 =(ThreadPoolExecutor)Executors.newFixedThreadPool(64);
        // user
        ThreadPoolExecutor p2 = (ThreadPoolExecutor)Executors.newFixedThreadPool(64);

        // registrazione
        for (int i = 0; i < 1024; i++) {
            int finalI = i;
            p0.execute(() -> {
                String name = "User" + finalI;
                System.out.println(new AutoClient().register(name, "123"));
            });
        }

        p0.shutdown();
        p0.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);


        // questi effettuano il login, e attendono che qualcuno li sfidi
        // a partita terminata richiedono le altre op del server
        for (int i = 0; i < 128; i += 2) {
            int finalI = i;
            p1.execute(() -> {
                try {
                    // connessione
                    AutoClient c = new AutoClient();
                    // login
                    System.out.println(c.login("User" + finalI, "123"));
                    // necessario attendere altrimenti le successive connect falliranno a causa della backlog del server
                    Thread.sleep(100);
                    if (!c.isConnected()) {
                        return;
                    }
                    // attende sfida
                    c.game();
                    // amici, oggetto json
                    JsonObject obj = c.listFriends();
                    System.out.println(c.myFromJsonArray("Amici", obj.getAsJsonArray("Amici")));
                    // punteggio
                    System.out.println(c.showPoints());
                    // classifica, oggetto json
                    obj = c.showRanking();
                    System.out.println(c.myFromJsonArray("Classifica", obj.getAsJsonArray("Classifica")));
                    // logout
                    System.out.println(c.logout());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }


        // questi effettuano il login aggiungono un amico di cui sopra
        // e lo sfidano
        for (int i = 1; i < 128; i += 2) {
            int finalI = i;
            p2.execute(() -> {
                try {
                    AutoClient c = new AutoClient();
                    System.out.println(c.login("User" + finalI, "123"));
                    // necessario attendere altrimenti le successive connect falliranno a causa della backlog del server
                    Thread.sleep(100);
                    if (!c.isConnected()) {
                        return;
                    }

                    // aggiungo amico di nome i + 1
                    System.out.println(c.addFriend("User" + (finalI - 1)));

                    // sfida
                    c.getOut().writeUTF("start User"+ (finalI - 1));
                    c.game();
                    System.out.println(c.logout());

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        p1.shutdown();
        p1.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        p2.shutdown();
        p2.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);


    }


}