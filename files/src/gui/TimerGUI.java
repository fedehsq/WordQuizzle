package gui;

import common.UtilityClass;

import javax.swing.*;

public class TimerGUI implements Runnable {
    @Override
    public void run() {
        // thread che mi interrompe dopo K secondi se non ho finito (tempo scaduto)
        try {
            String s = "Tempo scaduto! La prossima parola inviata non verra' considerata";
            Thread.sleep(UtilityClass.T2);
            JOptionPane.showMessageDialog(new JFrame(), s, "Quizzle", JOptionPane.ERROR_MESSAGE);
        } catch (InterruptedException e) {
            // e' stato interrotto perche' l'utente ha finito in tempo
        }
    }
}
