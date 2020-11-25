package client;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

// contiene il datagramma di richiesta sfida e l'intervallo di tempo di validita' di esso
public class TimeOutDatagramPacket {
    private long keepAlive;
    private DatagramPacket packet;
    private String senderName;

    public TimeOutDatagramPacket(long keepAlive, DatagramPacket packet) {
        this.keepAlive = keepAlive;
        this.packet = packet;
        this.senderName = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public long getKeepAlive() {
        return keepAlive;
    }

    public String getSenderName() {
        return senderName;
    }

    // username dell'inviatore
    @Override
    public String toString() {
        return getSenderName();
    }
}
