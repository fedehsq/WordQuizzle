package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegisterInterface extends Remote {

    // eccezioni lanciate
    @SuppressWarnings("serial")
	class ClientAlreadyRegisteredException extends Exception {
        public ClientAlreadyRegisteredException(String s) {
            super(s);
        }
    }

    @SuppressWarnings("serial")
	class InvalidPasswordException extends Exception {
        public InvalidPasswordException(String s) {
            super(s);
        }
    }
    @SuppressWarnings("serial")
	class InvalidUsernameException extends Exception {
        public InvalidUsernameException(String s) {
            super(s);
        }
    }

    // il client richiede la registrazione
    // restituisce true se l'operazione va a buon fine, solleva eccezione altrimenti
    boolean register(String username, String password) throws
            RemoteException, ClientAlreadyRegisteredException, InvalidPasswordException, InvalidUsernameException;
}
