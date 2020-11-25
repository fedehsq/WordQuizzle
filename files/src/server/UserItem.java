package server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

// classe che viene messa come attachment alla key dell'utente quando instaura una connessione TCP col server
public class UserItem {
    // utente
    private User user;
    // salvera' il primo messaggio del client, contente la lunghezza del messaggio successivo
    private ByteBuffer bufMessSize;
    // buffer di input
    private ByteBuffer bufIn;
    // buffer di output
    private ByteBuffer bufOut;
    // l'user è connesso?
    private boolean connected;
    // quando invia richiesta sfida
    private boolean gameRequest;
    // chiave registrata col selettore
    private SelectionKey key;

    public UserItem(SelectionKey key) {
        // 2 bytes contengono la lunghezza del messaggio passata da writeUTF (short)
        bufMessSize = ByteBuffer.allocate(2);
        connected = false;
        gameRequest = false;
        this.key = key;
    }

    // metodi getter e setter
    public User getUser() {
        return user;
    }
    public ByteBuffer getMessSize() {
        return bufMessSize;
    }
    public ByteBuffer getBufIn() {
        return bufIn;
    }
    public ByteBuffer getBufOut() {
        return bufOut;
    }
    public SocketAddress getAddress() throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        return client.getRemoteAddress();
    }
    public SelectionKey getKey() {
        return key;
    }
    public boolean isConnected() {
        return connected;
    }
    public boolean isGameRequest() {
        return gameRequest;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public void setBufInSize(int len) {
        bufIn = ByteBuffer.allocate(len);
    }
    public void setGameRequest(boolean gameRequest) {
        this.gameRequest = gameRequest;
    }
    public void setBufOut(int len) {
        bufOut = ByteBuffer.allocate(len);
    }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
