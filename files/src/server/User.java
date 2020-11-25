package server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

// il server crea l'utente una volta ricevute le credenziali "username" e "password
public class User {

    // per classifica ordinata in maniera decrescente quando un utente la richiede
    static class SortByPoint implements Comparator<User> {
        @Override
        public int compare(User u1, User u2) {
            return u2.getPoint() - u1.getPoint();
        }
    }

    private String username;
    private String password;
    private int points;
    private ArrayList<String> friends;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.friends = new ArrayList<>();
        this.points = 0;
    }

    // retituisce username
    public String getUsername() {
        return username;
    }

    // restituisce la password
    public String getPassword() {
        return password;
    }

    // restituisce la lista di amici di this
    public ArrayList<String> getFriends() {
        return friends;
    }

    // restituisce i punti
    public int getPoint() {
        return points;
    }

    // aggiorna in punteggio
    public void setPoint(int point) {
        this.points = point;
    }

    // ridefinizione di alcuni metodi necessari per un corretto funzionamento con le map
    @Override
    public String toString() {
        return "<" + username + ", " + password + ", " + points + ">";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return getUsername().equals(user.getUsername()) &&
                getPassword().equals(user.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getPassword());
    }
}