package gui;
import client.TimeOutDatagramPacket;
import common.UtilityClass;
import javax.swing.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

// task che mantiene aggiornata la lista di richiesta sfide
public class TaskUpdateRequestsJList implements Runnable {

    // pool che manitente per T1 secondi il nome dello sfidante nella JList
    private ThreadPoolExecutor executor;
    // lista contenente le richieste di sfida
    private final ArrayList<TimeOutDatagramPacket> requests;
    // lista che visualizza nell apposita JList le richieste valide
    private final DefaultListModel<TimeOutDatagramPacket> requestsLM;

    public TaskUpdateRequestsJList(ArrayList<TimeOutDatagramPacket> requests, DefaultListModel<TimeOutDatagramPacket> requestsLM) {
        this.requests = requests;
        this.requestsLM = requestsLM;
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try {
            start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void start() throws InterruptedException {
        for (;;) {
            synchronized (requests) {
                // finche' è vuota sospensione
                while (requests.isEmpty()) {
                    requests.wait();
                }

                // estrae e rimuove la richiesta
                TimeOutDatagramPacket packet = requests.get(0);
                requests.remove(0);

                // aggiungo le richieste alla JList
                synchronized (requestsLM) {
                    requestsLM.addElement(packet);
                }

                // IL THREAD DORME PER N SECONDI, PRENDE LA LOCK SULLA LISTA, RIMUOVE richiesta, RILASCIA LOCK
                executor.execute(() -> {
                    try {
                        Thread.sleep(UtilityClass.T1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (requestsLM) {
                        requestsLM.removeElement(packet);
                    }
                });
            }
        }
    }
}
