package de.juliansauer.spotifyshare.spotify;

import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import de.juliansauer.spotifyshare.rest.AuthorizationCode;
import de.juliansauer.spotifyshare.rest.Song;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

public interface ISpotifyController {

    @RequestMapping(value = "/add", method = GET)
    ResponseEntity addUser(String code, String userId);

    @RequestMapping(value = "/next", method = GET)
    ResponseEntity nextSong(String userId);

    @RequestMapping(value = "/previous", method = GET)
    ResponseEntity previousSong(String userId);

    @RequestMapping(value = "/pause", method = GET)
    ResponseEntity pauseSong(String userId);

    @RequestMapping(value = "/resume", method = GET)
    ResponseEntity resumeSong(String userId);

    /**
     * Returns the time in milliseconds until the current song ends. If no song is being played a default value is returned.
     *
     * @param userId Reference for SpotifyPlayer account
     * @return Remaining time in milliseconds
     */
    @RequestMapping(value = "/remaining", method = GET)
    int getRemainingMS(String userId);

    /**
     * Returns information about current playback.
     *
     * @param userId Reference for SpotifyPlayer account
     * @return Playback information
     */
    @RequestMapping(value = "/currentSong", method = GET)
    Song getCurrentSong(String userId);

    /**
     * Returns currently used device.
     *
     * @param userId Reference for SpotifyPlayer account
     * @return Device ID
     */
    @RequestMapping(value = "/device", method = GET)
    String getDeviceId(@RequestParam String userId);

    /**
     * Creates a Uri to ask a user for authorization.
     *
     * @return Uri for the user
     */
    @RequestMapping(value = "/authorization", method = GET)
    AuthorizationCode getAuthorizationCodeUri();

}
