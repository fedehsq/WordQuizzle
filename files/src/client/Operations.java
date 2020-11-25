package client;
import com.google.gson.JsonObject;

public interface Operations {

    String register(String username, String password);

    String login(String username, String password) ;

    String addFriend(String friendName);

    String match(String friendName);

    String logout();

    JsonObject listFriends();

    JsonObject onlineFriends();

    String showPoints();

    JsonObject showRanking();
}
