package gui;
import client.TimeOutDatagramPacket;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;

public class ShellGUI {
    // utente connesso
    private ClientGUI client;

    // componenti principali
    private JFrame shellJFrame;
    private JPanel shellJPanel;

    // mostra username e benvenuto
    private JPanel welcomeJPanel;

    // istruzioni
    private JLabel instructionsJLabel;

    // richiama la funzione che aggiorna tutti i valori
    private JButton updateJButton;

    // JLabel contenente il punteggio
    private JLabel valuePointsJLabel;

    // aggiunta amico
    private JPanel addFriendJPanel;
    private JTextField addFriendJTextField;
    private JButton addFriendJButton;

    // invio richiesta di sfida
    private JPanel sendMatchRequestsJPanel;
    private JTextField matchJTextField;
    private JButton sendMatchJButton;

    // visualizza gli amici
    private JPanel friendJPanel;
    private JList<String> friendsJList;
    private DefaultListModel<String> friends;

    // visualizza gli amici online
    private JPanel onlineFriendsJPanel;
    private JList<String> onlineFriendsJList;
    private DefaultListModel<String> onlineFriends;

    // visualizza classifica ordinata
    private JPanel rankingJPanel;
    private JList<String> rankingJList;
    private DefaultListModel<String> ranking;

    // visualizza richieste di sfida da parte degli amici
    private JPanel requestsJPanel;
    private JList<TimeOutDatagramPacket> matchRequestsJList;
    private JLabel manualJLabel;
    private final DefaultListModel<TimeOutDatagramPacket> requestsLM;

    // disconnette e riapre il form di login
    private JButton exitJButton;

    /*
    thread che dorme finche' non ho richieste di sfida,
    quando si sveglia aggiunge la richiesta alla JList contenente le richieste,
    e lancia un thread di un pool che dorme per T1  secondi e successivamente
    rimuove la richiesta dalla lista (timeout)
     */
    private Thread visual;

    // query automatiche inviate al server per mostrare tutte le cose del client: punteggio, amici, ranking
    private void updateArea(JsonObject obj, String key, DefaultListModel<String> list) {
        // estraggo l'array contente le info desiderate e le inserisco nel corrispettivo text are
        JsonArray elements = obj.getAsJsonArray(key);

        // classifica caso particolare:
        if (key.equals("Classifica")) {
            list.removeAllElements();
        }

        if (elements != null) {
            for (JsonElement element : elements) {
                String item = element.getAsString();
                if (!list.contains(item)) {
                    list.addElement(item);
                }
            }
        }
    }

    // aggiorna tutti i parametri dell'utente
    private void update() throws IOException {
        JsonObject obj;

        // aggiorna il label che visualizza il punteggio
        valuePointsJLabel.setText(client.showPoints());

        // aggiorna amici
        obj = new Gson().fromJson(client.listFriends(), JsonObject.class);
        updateArea(obj, "Amici", friends);

        // aggiorno classifica
        obj = new Gson().fromJson(client.showRanking(), JsonObject.class);
        updateArea(obj, "Classifica", ranking);

        // aggiorna amici online
        obj = new Gson().fromJson(client.onlineFriends(), JsonObject.class);
        updateArea(obj, "Amici online", onlineFriends);
    }


    public ShellGUI(String username, ClientGUI client) throws IOException {

        /*
        i dati del client vengono aggiornati ad ogni richiesta di operazione,
        o su esplicita richiesta cliccando un apposito bottone
        */

        // client
        this.client = client;

        // welcome user
        manualJLabel.setText("Ciao " + username + ", questa e' la tua shell");

        // lista visibile nella JList visualizzante gli amici
        friends = new DefaultListModel<>();
        friendsJList.setModel(friends);

        // lista visibile nella JList visualizzante la classifica
        ranking = new DefaultListModel<>();
        rankingJList.setModel(ranking);

        // lista visibile nella JList visualizzante gli amici online
        onlineFriends = new DefaultListModel<>();
        onlineFriendsJList.setModel(onlineFriends);

        // lista visibile nella JList visualizzante le richieste di sfida
        requestsLM = new DefaultListModel<>();
        matchRequestsJList.setModel(requestsLM);

        // thread che mantiene aggiornata la lista di richiesta sfide
        visual = new Thread(new TaskUpdateRequestsJList(client.getRequests(), requestsLM));
        visual.start();

        // appena l'utente si connette la shell si aggiorna
        update();

        // creazione frame che mostra la shell
        shellJFrame = new JFrame("Quizzle");
        shellJFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        shellJFrame.setContentPane(shellJPanel);
        shellJFrame.pack();
        shellJFrame.setVisible(true);

        // crea relazione di amicizia
        addFriendJButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);

                // amico aggiunto da mettere nel textarea
                String friend = addFriendJTextField.getText();

                //invia richiesta di aggiunta al server e successivamernte aggiorna tutte le JList
                try {
                    String fromServer = client.addFriend(friend);
                    if (fromServer.equals("Amicizia creata")) {
                        // aggiorno la shell
                        update();
                        JOptionPane.showMessageDialog(new JFrame(), fromServer, "Quizzle", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(new JFrame(), fromServer, "Quizzle", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // aggiornameto dati al click del bottone
        updateJButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // richiesta di aggiornare i dati
                try {
                    update();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // per accettare la richiesta di sfida, doppio click sul nome dell'avversario
        matchRequestsJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                @SuppressWarnings("unchecked")
				JList<TimeOutDatagramPacket> list = (JList<TimeOutDatagramPacket>) e.getSource();

                // se doppio click
                if (e.getClickCount() == 2) {
                    // indice dell'amico da sfidare
                    int i = list.locationToIndex(e.getPoint());
                    // username dell'amico
                    TimeOutDatagramPacket sfidante;
                    // lo rimuovo dalla lista visualizzata, accesso condiviso, lock
                    synchronized (requestsLM) {
                        sfidante = requestsLM.get(i);
                        // rimuovo la richieste dalla lista
                        requestsLM.remove(i);
                        try {
                            // accetta la sfida
                            client.getDatagramSocket().send(new DatagramPacket("ok".getBytes(), 0, 2,
                                    sfidante.getPacket().getSocketAddress()));

                            // nascondo la shell durante il game
                            shellJFrame.setVisible(false);

                            // thread che gestisce il game, parte il game
                            new Thread(() -> {
                                try {
                                    // mostra il frame di gioco
                                    client.gameGui(shellJFrame);
                                    client.game();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }).start();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });


        // richiesta di sfida ad un amico
        sendMatchJButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                String fromServer = client.match(matchJTextField.getText());
                // sospendo finché non arriva l'esito dall'amico, mi verra' comunicato l'esito dal server
                if (fromServer.endsWith("accettazione")) {
                    // apre il frame col timeout
                    new WaitRequestGUI(client, shellJFrame);
                } else {
                    // errore di qualche tipo (non amici / amico non online)
                    JOptionPane.showMessageDialog(new JFrame(), fromServer, "Quizzle", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // esce e si riapre la schermata di login
        exitJButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                client.logout();
                shellJFrame.setVisible(false);
                shellJFrame.dispose();
                LoginGUI loginGUI = new LoginGUI();
                loginGUI.show();
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void $$$setupUI$$$() {
        shellJPanel = new JPanel();
        shellJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(8, 5, new Insets(0, 0, 0, 0), -1, -1));
        addFriendJPanel = new JPanel();
        addFriendJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        shellJPanel.add(addFriendJPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addFriendJPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Aggiungi amico", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, addFriendJPanel.getFont()), new Color(-11908097)));
        final JLabel label1 = new JLabel();
        label1.setText("Nome amico");
        addFriendJPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addFriendJTextField = new JTextField();
        addFriendJPanel.add(addFriendJTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        addFriendJButton = new JButton();
        addFriendJButton.setText("Aggiungi");
        addFriendJPanel.add(addFriendJButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        addFriendJPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        welcomeJPanel = new JPanel();
        welcomeJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(50, 50, 50, 50), -1, -1));
        welcomeJPanel.setBackground(new Color(-13092808));
        shellJPanel.add(welcomeJPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        manualJLabel = new JLabel();
        Font manualJLabelFont = this.$$$getFont$$$("Noto Sans", Font.BOLD, 28, manualJLabel.getFont());
        if (manualJLabelFont != null) manualJLabel.setFont(manualJLabelFont);
        manualJLabel.setForeground(new Color(-1));
        manualJLabel.setText("Ciao Federico, questa e' la tua shell");
        welcomeJPanel.add(manualJLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_SOUTH, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        instructionsJLabel = new JLabel();
        instructionsJLabel.setForeground(new Color(-1));
        instructionsJLabel.setText("<html>Puoi aggiungere/sfidare un amico inserido il suo username nell'apposito JTextField e premere il relativo JButton. <BR>\nLe JList 'Amici', 'Amici online' e 'Classifica' vengono aggiornati col click sul JButton 'Aggiorna'. <BR>\nLa JList 'Richieste di sfida' viene tenuta aggiornata in tempo reale. <BR>\nPer accettare una sfida basta cliccare due volte sul nome del richiedente. <BR>\nPer sfidare un amico e' necessario che sia connesso. <BR>\nHave Fun! </html>\"");
        welcomeJPanel.add(instructionsJLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        valuePointsJLabel = new JLabel();
        valuePointsJLabel.setText("Punteggio");
        shellJPanel.add(valuePointsJLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateJButton = new JButton();
        updateJButton.setText("Aggiorna");
        shellJPanel.add(updateJButton, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        shellJPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        shellJPanel.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        sendMatchRequestsJPanel = new JPanel();
        sendMatchRequestsJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        shellJPanel.add(sendMatchRequestsJPanel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sendMatchRequestsJPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Sfida amico", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-15578693)));
        final JLabel label2 = new JLabel();
        label2.setText("Nome amico");
        sendMatchRequestsJPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        matchJTextField = new JTextField();
        sendMatchRequestsJPanel.add(matchJTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        sendMatchRequestsJPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        sendMatchJButton = new JButton();
        sendMatchJButton.setText("Sfida  ");
        sendMatchRequestsJPanel.add(sendMatchJButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        shellJPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 150), null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Amici online", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, scrollPane1.getFont()), new Color(-15578693)));
        onlineFriendsJPanel = new JPanel();
        onlineFriendsJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane1.setViewportView(onlineFriendsJPanel);
        onlineFriendsJList = new JList();
        onlineFriendsJPanel.add(onlineFriendsJList, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        shellJPanel.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 150), null, 0, false));
        scrollPane2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Classifica", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-15578693)));
        rankingJPanel = new JPanel();
        rankingJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane2.setViewportView(rankingJPanel);
        rankingJList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        rankingJList.setModel(defaultListModel1);
        rankingJPanel.add(rankingJList, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        shellJPanel.add(scrollPane3, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 150), null, 0, false));
        scrollPane3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Amici", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-15578693)));
        friendJPanel = new JPanel();
        friendJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane3.setViewportView(friendJPanel);
        friendsJList = new JList();
        final DefaultListModel defaultListModel2 = new DefaultListModel();
        friendsJList.setModel(defaultListModel2);
        friendJPanel.add(friendsJList, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        shellJPanel.add(scrollPane4, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 150), null, 0, false));
        scrollPane4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Richieste di sfida", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-15578693)));
        requestsJPanel = new JPanel();
        requestsJPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane4.setViewportView(requestsJPanel);
        matchRequestsJList = new JList();
        requestsJPanel.add(matchRequestsJList, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        exitJButton = new JButton();
        exitJButton.setText("Esci");
        shellJPanel.add(exitJButton, new com.intellij.uiDesigner.core.GridConstraints(6, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), new Dimension(100, -1), 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return shellJPanel;
    }
}
