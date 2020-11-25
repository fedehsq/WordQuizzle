package client;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/*
    Thread del client che viene lanciato quando esso esegue correttamente l'operazione di login.
    Rimane in attesa finche' un user amico del client non richiede una sfida.
    Una volta ricevuto il datagramma, lo incapsula in una classe che oltre a contenere il datagramma stesso,
    contiene anche il tempo di validita' di esso.
    Viene inserito in una coda condivisa col thread principale, dunque necessita di essere sincronizzata
 */

public class RequestListner implements Runnable {
    private DatagramSocket datagramSocket;
    private final ArrayList<TimeOutDatagramPacket> requests;

    public RequestListner(DatagramSocket datagramSocket, ArrayList<TimeOutDatagramPacket> requests) {
        this.datagramSocket = datagramSocket;
        this.requests = requests;
    }

    @Override
    public void run() {
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[64], 0, 64);
                datagramSocket.receive(packet);
                synchronized (requests) {
                    requests.add(new TimeOutDatagramPacket(System.currentTimeMillis(), packet));
                    requests.notifyAll();
                }
            } catch (IOException e) {
                // chiusura socket quando il client si disconnette
                return;
            }
        }
    }
}
