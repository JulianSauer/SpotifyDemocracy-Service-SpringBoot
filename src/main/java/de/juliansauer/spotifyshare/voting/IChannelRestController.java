package de.juliansauer.spotifyshare.voting;

import de.juliansauer.spotifyshare.rest.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

public interface IChannelRestController {

    @RequestMapping(value = "/users", method = GET)
    Set<User> getUsers(String channelId);

    @RequestMapping(value = "/logout", method = GET)
    ResponseEntity logout(String userId, String channelId);

}
