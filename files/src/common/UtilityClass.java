package common;

import server.UserItem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/*
stateless utility class, public e final, per le operazioni ripetute piu' volte dai vari server task
private constructor per prevenire istanziazioni
Scrive e legge utilizzando NIO
 */

public final class UtilityClass {

    // parole da tradurre per game
    public static int K;
    // parole totali del dizionario
    public static int N;
    // tempo massimo di attesa del datagramma UDP di richiesta sfida
    public static int T1;
    // durata massima del game
    public static int T2;
    // punti assegnati per risposta corretta
    public static int X;
    // punti sottratti per risposta incorretta
    public static int Y;
    // punti extra assegnati al vincitore
    public static int Z;

    // Private constructor to prevent instantiation
    private UtilityClass() {
        throw new UnsupportedOperationException();
    }

    /**
     * Estrae dalla stringa s il parametro k e lo inizializza
     * @param s stringa letta dal file di configurazione
     * @param k parametro da salvare
     * @return valore numero del parametro
     */
    private static int initializeVar(String s, String k) {
        // estraggo i vari parametri dal file di configurazione
        StringBuilder stringBuilder = new StringBuilder();
        char[] chars = s.substring(s.indexOf(k)).toCharArray();
        // T1 e T" iniziano dalla 3 pos
        for (int j = (k.length() == 1) ? 2 : 3; chars[j] != '\n'; j++) {
            stringBuilder.append(chars[j]);
        }
        return Integer.parseInt(stringBuilder.toString().trim());
    }

    /**
     legge dal file di configurazione e inizializza i parametri
     */
    public static void readConfig() {
        String s = readFromFile("config.ini");
        K = initializeVar(s,"K");

        // controllo parametri
        if (K <= 0) {
            System.out.println("Aumentare il parametro K");
            System.exit(-1);
        }
        N = initializeVar(s,"N");

        if (N <= 0) {
            System.out.println("Aumentare il parametro N");
            System.exit(-1);
        }

        if (K > N) {
            System.out.println("Aumentare il parametro N o diminuire K");
            System.exit(-1);
        }

        T1 = initializeVar(s,"T1");

        if (T1 <= 1000) {
            System.out.println("Aumentare il parametro T1");
            System.exit(-1);
        }

        T2 = initializeVar(s,"T2");

        if (T2 <= 1000) {
            System.out.println("Aumentare il parametro T2");
            System.exit(-1);
        }

        X = initializeVar(s,"X");
        if (X <= 0) {
            System.out.println("Aumentare il parametro X");
            System.exit(-1);
        }

        Y = initializeVar(s,"Y");
        if (Y < 0) {
            System.out.println("Aumentare il parametro Y");
            System.exit(-1);
        }

        Z = initializeVar(s,"Z");
        if (Z <= 0) {
            System.out.println("Aumentare il parametro Z");
            System.exit(-1);
        }
    }

    /**
     * Legge il contenuto di un file e lo esporta in stringa
     * @param filename nome del file da leggere
     * @return la stringa contenente il contenuto del file
     */
    public static String readFromFile(String filename) {
        try {
            Files.createFile(Paths.get(filename));
            // il file è sicuramente vuoto
            return null;
        } catch (FileAlreadyExistsException e) {
            // il file esisteva gia' posso leggere
        } catch (IOException e) {
            e.printStackTrace();
        }
        // conterra' l'oggetto contenuto nel file "filename"
        StringBuilder deserialized = new StringBuilder();
        try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(8196);
            while (fileChannel.read(buf) > 0) {
                // controllo se il buffer è pieno, se lo è, "scarico" nello string builder
                if (buf.position() == buf.limit()) {
                    // position = 0
                    buf.flip();
                    deserialized.append(new String(buf.array(), 0, buf.limit()));
                    // pronto ad essere sovrascritto
                    buf.clear();
                }
            }
            // nel caso di una sola iterazione del while, o alla fine
            deserialized.append(new String(buf.array(), 0, buf.position()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deserialized.toString();
    }

    /**
     * Scrive la stringa s sul file filename
     *
     * @param s stringa da scrivere
     * @param filename file su cui scrivere
     */
    public static void writeToFile(String s, String filename) {
        try {
            Files.createFile(Paths.get(filename));
        } catch (FileAlreadyExistsException e) {
            // il file esisteva gia' posso scrivere
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuffer buf = ByteBuffer.wrap(s.getBytes());
        try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), StandardOpenOption.WRITE)) {
            while (buf.hasRemaining()) {
                fileChannel.write(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Legge il messaggio del client con chiave key
     * @param key chiave del client
     * @return la stringa letta
     */
    public static String readMessage(SelectionKey key) {
        /*
        la funzione writeUTF con la quale il client invia la stringa di richiesta,
        contiene come primi due bytes uno short che rappresenta i bytes seguenti della stringa,
        uso questo short per definire la giusta size del buffer di ricezione.
        */

        SocketChannel client = (SocketChannel) key.channel();
        UserItem item = (UserItem) key.attachment();

        ByteBuffer size = item.getMessSize();
        // leggo la size del messaggio
        try {
            client.read(size);
        } catch (IOException e) {
            // connessione interrotta brutalmente dal client
            System.out.println(e.toString());
            return null;
        }
        // torno in modalita' lettura
        size.flip();
        // salvo la dimensione della stringa
        short n = size.getShort();
        // pulisco questo buffer
        size.clear();
        // alloco il buffer di ricezione della giusta misura
        item.setBufInSize(n);
        ByteBuffer in = item.getBufIn();
        // leggo il messaggio
        try {
            client.read(in);
        } catch (IOException e) {
            // connessione interrotta brutalmente dal client
            System.out.println(e.toString());
            return null;
        }
        // torno in modalita' lettura
        in.flip();
        // il messaggio del client viene salvato in questa stringa
        return new String(item.getBufIn().array(), 0, in.limit(), StandardCharsets.UTF_8);
    }

    /**
     * Prepara il messaggio da inviare al client
     * @param key chiave del client
     * @param s stringa da scrivere nel buffer di output del client
     */
    public static void makeMessage(SelectionKey key, String s) {
        UserItem item = (UserItem) key.attachment();
        item.setBufOut(s.length());
        item.getBufOut().put(s.getBytes());
        item.getBufOut().flip();
    }

    /**
     * Scrive il messaggio al client di chiave key
     * @param key chiave del client
     */
    public static boolean writeMessage(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        UserItem item = (UserItem) key.attachment();
        ByteBuffer out = item.getBufOut();
        while (out.hasRemaining()) {
            try {
                client.write(out);
            } catch (IOException e) {
                System.out.println(e.toString());
                return false;
            }
        }
        return true;
    }
}