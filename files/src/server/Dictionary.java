package server;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import common.UtilityClass;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

// dizionario per ogni game
public class Dictionary {
    // parole sottoposte agli sfidanti
    private String[] it;
    /*
     traduzioni di 'it' restituite dal servizio online,
     siccome per una parola il servizio puo' restituire piu' tradizioni, utilizzo due ArrayList annidati,
     uno per indicizzare, e l'altro, quello interno, mantiene eventualmente piu' traduzioni
     */
    private ArrayList<ArrayList<String>> en;
    // parole aggiunte nel dizionario
    private int nElem;

    public Dictionary() {
        // k parole nel dizionario del game
        it = new String[UtilityClass.K];
        en = new ArrayList<>();
        // elementi attualmenti inseriti
        nElem = 0;
    }

    /*
    s e' la stringa di risposta alla query che restituisce l'utente,
    il server controlla che essa sia uguale ad una di quelle da lui tradotte usando il servizio
    in posizione i durante il setup della sfida
     */
    public boolean isOk(int i, String s) {
        // controllo se la traduzione dell'utente è contenuta nella lista di traduzione fornite dal server
        System.out.println("<" + s + "," + en.get(i));
        for (String str : en.get(i)) {
            if (str.equals(s)) {
                return true;
            }
        }
        return false;
    }


    // controlla se la parola italiana e' gia stata inserita nella lista delle parole da tradurre
    public boolean contains(String s) {
        for (int i = 0; i < nElem; i++) {
            if (it[i].equals(s)) {
                return true;
            }
        }
        return false;
    }

    // parola italiana da tradurre restituita agli utenti
    public String get(int i) {
        return it[i];
    }

    /*
     accesso concorrente a 'words' se piu' game in contemporanea,
     ma accesso solo in lettura, non serve sincronizzare
     */
    public void setDictionary(String[] words) {
        for (int i = 0; i < UtilityClass.K; i++) {
            String itWord;
            // estrae a caso K parole dal dizionario, ripete finche' le parole non sono tutte diverse
            if (contains(itWord = words[((int) (Math.random() * words.length))])) {
                System.out.println("doppione " + i);
                i--;
                continue;
            }
            // legge dal servizio online la risposta
            String jsonElement = makeQuery(itWord);
            // estrae l'array delle traduzioni da jsonElement e aggiunge <it, [en]> al dizionario
            readAndAdd(jsonElement, itWord);
        }
    }

    private String makeQuery(String s) {
        // query
        StringBuilder jsonElement = new StringBuilder();
        try {
            String query = "https://api.mymemory.translated.net/get?q=" + s + "&langpair=it|en";
            // richiesta http GET
            URL url = new URL(query);
            InputStream in = url.openStream();
            in = new BufferedInputStream(in);
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            // linea letta
            String line;

            // costruisce la stringa di formato JSON
            while ((line = r.readLine()) != null) {
                jsonElement.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // tutta la risposta del servizio
        return jsonElement.toString();

    }

    private void readAndAdd(String jsonElement, String itWord) {

        // lista di traduzioni che assocero' alla lista indicizzante
        ArrayList<String> traductions = new ArrayList<>();

        Gson gson = new Gson();

        // la stringa e' come ogetto JSON
        JsonObject obj = gson.fromJson(jsonElement, JsonObject.class);

        // estraggo l'array contente le traduzioni
        JsonArray elements = obj.getAsJsonArray("matches");
        for (JsonElement element : elements) {
            JsonObject obj1 = gson.fromJson(element, JsonObject.class);
            // la traduzione ha come chiave "translation"
            String word = obj1.get("translation").getAsString();
            // aggiungo la traduzione word nell'arraylist che assocerò all'indice nElem
            traductions.add(word.toLowerCase());
        }

        // aggiungo la parola italiana alla quale devono corrispondere le traduzioni in traductions
        it[nElem] = itWord;
        // array di parole tradotte in posizione nElem
        en.add(nElem++, traductions);
    }
}