package cli;

import common.UtilityClass;

// timer dei match: dorme per T2 secondi e se si risveglia comunica che è scaduto il tempo
public class Timer implements Runnable {
    @Override
    public void run() {
        try {
            Thread.sleep(UtilityClass.T2);
            System.out.print("\nTempo scaduto! La prossima parola inviata non verra' considerata, premi invio:\n> ");
        } catch (InterruptedException e) {
            // e' stato interrotto perche' l'utente ha finito in tempo
        }

    }
}
