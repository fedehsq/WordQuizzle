package server;

import common.UtilityClass;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

// task che inoltra le richieste da un utente x ad un altro y
public class ServerRequest implements Runnable {
    // username dell'amico
    private String friend;
    // address del client destinazione
    private SocketAddress destination;

    /*
    passaggio di chiave necessario per estrarre il canale e
    per settare la successiva modalita' in lettura del client richiedente
     */
    private SelectionKey senderKey;

    public ServerRequest(String friend, SocketAddress destination, SelectionKey senderKey) {
        this.friend = friend;
        this.destination = destination;
        this.senderKey = senderKey;
    }

    @Override
    public void run() {

        SocketChannel sender = (SocketChannel)senderKey.channel();
        UserItem item = (UserItem)senderKey.attachment();
        byte[] send;
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            // tempo massimo di attesa nella risposta dell'amico
            datagramSocket.setSoTimeout(UtilityClass.T1);
            String s = item.getUser().getUsername();
            DatagramPacket datagramPacket = new DatagramPacket(s.getBytes(), 0, s.length(), destination);
            datagramSocket.send(datagramPacket);
            // la risposta dell'amico puo' essere "ok" o "no"
            byte[] resp = new byte[2];
            datagramSocket.receive(new DatagramPacket(resp,2));
            if ((new String(resp, 0, 2, StandardCharsets.UTF_8)).equals("ok")) {
                send = (friend + " ha accettato, preparazione in corso...\n").getBytes();
            } else {
                send = (friend + " non ha accettato\n").getBytes();
            }

        } catch (IOException e) {
            // il server comunica all'inviatore della richiesta che l'amico non ha accettato per scadere del timeout
            send = "Tempo scaduto, richiesta non accettata\n".getBytes();
        }
        try {
            // il server inoltra la risposta al richiedente
            sender.write(ByteBuffer.wrap(send));
            item.setGameRequest(false);
            // ripristino operazione
            senderKey.interestOps(SelectionKey.OP_READ);
            // sveglio il selettore se per caso era in attesa
            senderKey.selector().wakeup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
