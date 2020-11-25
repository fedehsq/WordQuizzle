package server;

import java.nio.channels.SelectionKey;

// classe che estendeUserItem, aggiunge l'indice della parola da tradurre e i punti del game attuale
public class UserItemInGame extends UserItem {
    // indice della parola del dizionario da tradurre
    private int i;
    // punti del game
    private int gamePoints;
    // punti extra per vittoria
    private int extraPoints;
    // risposte corrette
    private int correctedAnswer;
    // risposte sbagliate
    private int incorrectedAnswer;
    // risposte non date
    private int emptyAnswer;
    // chi finisce prima
    private boolean firstFinisher;

    public UserItemInGame(SelectionKey key, UserItem userItem) {
        super(key);
        this.setUser(userItem.getUser());
        this.setConnected(true);
        // nessun altro setter necessario
        i = gamePoints = correctedAnswer = incorrectedAnswer = emptyAnswer = extraPoints =  0;
        firstFinisher = false;
    }

    // getter e setter
    // indice parola nel dizionionario alla quale ha risposto
    public int getIndex() {
        return i;
    }
    public int getGamePoints() {
        return gamePoints;
    }
    public int getExtraPoints() {
        return extraPoints;
    }
    public int getCorrectedAnswer() {
        return correctedAnswer;
    }
    public int getIncorrectedAnswer() {
        return incorrectedAnswer;
    }
    public int getEmptyAnswer() {
        return emptyAnswer;
    }
    public boolean isFirstFinisher() {
        return firstFinisher;
    }
    public void setIndex(int i) {
        this.i = i;
    }
    public void setGamePoints(int points) {
        this.gamePoints = points;
    }
    public void setExtraPoints(int extraPoints) {
        this.extraPoints = extraPoints;
    }
    public void setCorrectedAnswer(int correctedAnswer) {
        this.correctedAnswer = correctedAnswer;
    }
    public void setIncorrectedAnswer(int incorrectedAnswer) {
        this.incorrectedAnswer = incorrectedAnswer;
    }
    public void setEmptyAnswer(int emptyAnswer) {
        this.emptyAnswer = emptyAnswer;
    }
    public void setFirstFinisher(boolean firstFinisher) {
        this.firstFinisher = firstFinisher;
    }
}
