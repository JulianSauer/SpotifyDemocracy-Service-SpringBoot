package de.juliansauer.spotifyshare.spotify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.miscellaneous.Device;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.player.*;
import de.juliansauer.spotifyshare.rest.AuthorizationCode;
import de.juliansauer.spotifyshare.storage.ConfigManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
public class SpotifyController implements ISpotifyController {

    private final String scope = "user-read-playback-state," +
            "user-modify-playback-state";

    private final int UPDATE_DELAY = 2000;

    private Map<String, SpotifyApi> spotifyAccounts;

    private final URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:4200/create");

    ConfigManager config;

    public SpotifyController() {
        spotifyAccounts = new HashMap<>();
        config = new ConfigManager();
    }

    @Override
    public ResponseEntity addUser(String code, String userId) {
        SpotifyApi api = getAuthorizationCodeCredentials(code);
        if (api == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        spotifyAccounts.put(userId, api);
        if (api.getAccessToken() != null && api.getRefreshToken() != null)
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity nextSong(String userId) {
        if (nextSong(userId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity previousSong(String userId) {
        if (previousSong(userId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity pauseSong(String userId) {
        if (pauseSong(userId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity resumeSong(String userId) {
        if (resumeSong(userId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public CurrentlyPlayingContext getSongContext(String userId) {
        return getSongContext(userId, true);
    }

    @Override
    public String getDeviceId(String userId) {
        return getDeviceId(userId, true);
    }

    @Override
    public AuthorizationCode getAuthorizationCodeUri() {
        SpotifyApi api = new SpotifyApi.Builder()
                .setClientId(config.getClientId())
                .setClientSecret(config.getClientSecret())
                .setRedirectUri(redirectUri)
                .build();
        AuthorizationCodeUriRequest authorizationCodeUriRequest = api.authorizationCodeUri()
                .scope(scope)
                .show_dialog(true)
                .build();

        final URI uri = authorizationCodeUriRequest.execute();

        return new AuthorizationCode(uri.toString());
    }

    @Override
    public int getRemainingMS(@RequestParam String userId) {
        CurrentlyPlayingContext context = getSongContext(userId);
        if (context == null || !context.getIs_playing())
            return 20000;
        Track track = context.getItem();
        int progress = context.getProgress_ms();
        int duration = track.getDurationMs();
        return duration - progress + UPDATE_DELAY;
    }

    private boolean nextSong(String userId, boolean retry) {

        if (!spotifyAccounts.containsKey(userId))
            return false;

        SkipUsersPlaybackToNextTrackRequest skipUsersPlaybackToNextTrackRequest = spotifyAccounts.get(userId)
                .skipUsersPlaybackToNextTrack()
                .device_id(getDeviceId(userId))
                .build();

        try {
            skipUsersPlaybackToNextTrackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId);
            return nextSong(userId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;

    }

    private boolean previousSong(String userId, boolean retry) {

        if (!spotifyAccounts.containsKey(userId))
            return false;

        SkipUsersPlaybackToPreviousTrackRequest skipUsersPlaybackToPreviousTrackRequest = spotifyAccounts.get(userId)
                .skipUsersPlaybackToPreviousTrack()
                .device_id(getDeviceId(userId))
                .build();

        try {
            skipUsersPlaybackToPreviousTrackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId);
            return previousSong(userId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;
    }

    private boolean pauseSong(String userId, boolean retry) {

        CurrentlyPlayingContext currentContex = getSongContext(userId);
        if (!spotifyAccounts.containsKey(userId)
                || currentContex == null
                || !currentContex.getIs_playing())
            return false;

        PauseUsersPlaybackRequest pauseUsersPlaybackRequest = spotifyAccounts.get(userId)
                .pauseUsersPlayback()
                .device_id(getDeviceId(userId))
                .build();

        try {
            pauseUsersPlaybackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId);
            return pauseSong(userId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;

    }

    private boolean resumeSong(String userId, boolean retry) {

        CurrentlyPlayingContext currentContex = getSongContext(userId);
        if (!spotifyAccounts.containsKey(userId)
                || currentContex == null
                || currentContex.getIs_playing())
            return false;

        StartResumeUsersPlaybackRequest startResumeUsersPlaybackRequest = spotifyAccounts.get(userId)
                .startResumeUsersPlayback()
                .device_id(getDeviceId(userId))
                .build();

        try {
            startResumeUsersPlaybackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId);
            return resumeSong(userId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;

    }

    private CurrentlyPlayingContext getSongContext(String userId, boolean retry) {
        if (!spotifyAccounts.containsKey(userId))
            return null;

        GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest = spotifyAccounts.get(userId)
                .getInformationAboutUsersCurrentPlayback()
                .build();

        try {
            return getInformationAboutUsersCurrentPlaybackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId);
            return getSongContext(userId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getDeviceId(String userId, boolean retry) {

        if (!spotifyAccounts.containsKey(userId))
            return "";

        GetUsersAvailableDevicesRequest getUsersAvailableDevicesRequest = spotifyAccounts.get(userId)
                .getUsersAvailableDevices()
                .build();
        try {
            Device[] devices = getUsersAvailableDevicesRequest.execute();
            for (Device device : devices)
                if (device.getIs_active())
                    return device.getId();
        } catch (UnauthorizedException e) {
            updateTokens(userId);
            return getDeviceId(userId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }

        return "";

    }

    /**
     * Creates access and refresh tokens using code retrieved from callback.
     *
     * @param callbackCode Callback from {@link SpotifyController#getAuthorizationCodeUri()}
     * @return Api object containing tokens
     */
    private SpotifyApi getAuthorizationCodeCredentials(String callbackCode) {
        SpotifyApi api = new SpotifyApi.Builder()
                .setClientId(config.getClientId())
                .setClientSecret(config.getClientSecret())
                .setRedirectUri(redirectUri)
                .build();
        AuthorizationCodeRequest authorizationCodeRequest = api.authorizationCode(callbackCode).build();

        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            api.setAccessToken(authorizationCodeCredentials.getAccessToken());
            api.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }

        return api;
    }

    private boolean updateTokens(String userId) {
        if (!spotifyAccounts.containsKey(userId))
            return false;

        SpotifyApi api = spotifyAccounts.get(userId);
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = api
                .authorizationCodeRefresh()
                .build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
            api.setAccessToken(authorizationCodeCredentials.getAccessToken());
            api.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            spotifyAccounts.put(userId, api);
            return api.getAccessToken() != null && api.getRefreshToken() != null;
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }

        return false;
    }

}
