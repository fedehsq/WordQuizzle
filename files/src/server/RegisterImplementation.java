package server;

import com.google.gson.Gson;
import common.UtilityClass;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

// classe che realizza il servizio di registrazione degli utenti al servizio
@SuppressWarnings("serial")
public class RegisterImplementation extends RemoteServer implements RegisterInterface {
    // hashmap degli utenti registrati
    private ConcurrentHashMap<String, User> registeredUser;

    /**
     * @param registeredUser utenti registrati al servizio
     */
    public RegisterImplementation(ConcurrentHashMap<String, User> registeredUser) {
        this.registeredUser = registeredUser;
    }

    /**
     * Registra l'utente al servizio
     * @param username richiesto dall'utente
     * @param password rìselezionata dall'utente
     * @return true se la registrazione va a buon fine
     * @throws RemoteException
     * @throws ClientAlreadyRegisteredException se l'username e' gia' in uso
     * @throws InvalidPasswordException se l'utente non inserisce alcuna password
     */
    @Override
    public boolean register(String username, String password) throws RemoteException,
            ClientAlreadyRegisteredException, InvalidPasswordException, InvalidUsernameException {

        // un utente prova a non inserire l'username
        if (username.equals("")) {
            throw new InvalidUsernameException("Username non inserito");
        }
        // un utente prova a non inserire la password
        if (password.equals("")) {
            throw new InvalidPasswordException("Password non inserita");
        }

        User u = registeredUser.putIfAbsent(username, new User(username, password));
        if (u != null) {
            throw new ClientAlreadyRegisteredException("Utente gia' registrato");
        }
        //serializzo su file ad ogni utente aggiunto
        UtilityClass.writeToFile(new Gson().toJson(registeredUser), "users.json");
        return true;
    }


}
