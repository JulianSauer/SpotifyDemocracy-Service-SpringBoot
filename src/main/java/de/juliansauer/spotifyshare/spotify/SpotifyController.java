package de.juliansauer.spotifyshare.spotify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.miscellaneous.Device;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.player.*;
import de.juliansauer.spotifyshare.rest.AuthorizationCode;
import de.juliansauer.spotifyshare.rest.Song;
import de.juliansauer.spotifyshare.storage.ConfigManager;
import de.juliansauer.spotifyshare.voting.Channel;
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
    
    private Map<String, Channel> channels;

    private final URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:4200/create");

    ConfigManager config;

    public SpotifyController() {
        channels = new HashMap<>();
        config = new ConfigManager();
    }

    @Override
    public ResponseEntity addUser(String code, String userId, String channelId) {
        SpotifyApi api = getAuthorizationCodeCredentials(code);
        if (api == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        Channel channel = new Channel(userId, channelId, api);
        channels.put(channelId, channel);
        if (api.getAccessToken() != null && api.getRefreshToken() != null)
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity nextSong(String userId, String channelId) {
        if (nextSong(userId, channelId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity previousSong(String userId, String channelId) {
        if (previousSong(userId, channelId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity pauseSong(String userId, String channelId) {
        if (pauseSong(userId, channelId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity resumeSong(String userId, String channelId) {
        if (resumeSong(userId, channelId, true))
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public Song getCurrentSong(String userId, String channelId) {
        CurrentlyPlayingContext context = getSongContext(userId, channelId, true);
        if (context == null)
            return new Song("", "");
        Track track = context.getItem();
        ArtistSimplified[] artists = track.getArtists();
        StringBuilder songInformation = new StringBuilder();

        for (int i = 0; i < artists.length; i++) {
            if (i == artists.length - 1) {
                songInformation.append(artists[i].getName());
            } else {
                songInformation.append(artists[i].getName());
                songInformation.append(", ");
            }
        }

        return new Song(track.getName(), songInformation.toString());
    }

    @Override
    public String getDeviceId(String userId, String channelId) {
        return getDeviceId(userId, channelId, true);
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
    public int getRemainingMS(@RequestParam String userId, String channelId) {
        CurrentlyPlayingContext context = getSongContext(userId, channelId, true);
        if (context == null || !context.getIs_playing())
            return 20000;
        Track track = context.getItem();
        int progress = context.getProgress_ms();
        int duration = track.getDurationMs();
        return duration - progress + UPDATE_DELAY;
    }

    private boolean nextSong(String userId, String channelId, boolean retry) {

        if (!channels.containsKey(channelId))
            return false;

        SkipUsersPlaybackToNextTrackRequest skipUsersPlaybackToNextTrackRequest = channels.get(channelId).getApi()
                .skipUsersPlaybackToNextTrack()
                .device_id(getDeviceId(userId, channelId))
                .build();

        try {
            skipUsersPlaybackToNextTrackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId, channelId);
            return nextSong(userId, channelId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;

    }

    private boolean previousSong(String userId, String channelId, boolean retry) {

        if (!channels.containsKey(channelId))
            return false;

        SkipUsersPlaybackToPreviousTrackRequest skipUsersPlaybackToPreviousTrackRequest = channels.get(channelId).getApi()
                .skipUsersPlaybackToPreviousTrack()
                .device_id(getDeviceId(userId, channelId))
                .build();

        try {
            skipUsersPlaybackToPreviousTrackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId, channelId);
            return previousSong(userId, channelId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;
    }

    private boolean pauseSong(String userId, String channelId, boolean retry) {

        CurrentlyPlayingContext currentContex = getSongContext(userId, channelId, true);
        if (!channels.containsKey(channelId)
                || currentContex == null
                || !currentContex.getIs_playing())
            return false;

        PauseUsersPlaybackRequest pauseUsersPlaybackRequest = channels.get(channelId).getApi()
                .pauseUsersPlayback()
                .device_id(getDeviceId(userId, channelId))
                .build();

        try {
            pauseUsersPlaybackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId, channelId);
            return pauseSong(userId, channelId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;

    }

    private boolean resumeSong(String userId, String channelId, boolean retry) {

        CurrentlyPlayingContext currentContex = getSongContext(userId, channelId, true);
        if (!channels.containsKey(channelId)
                || currentContex == null
                || currentContex.getIs_playing())
            return false;

        StartResumeUsersPlaybackRequest startResumeUsersPlaybackRequest = channels.get(channelId).getApi()
                .startResumeUsersPlayback()
                .device_id(getDeviceId(userId, channelId))
                .build();

        try {
            startResumeUsersPlaybackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId, channelId);
            return resumeSong(userId, channelId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // I don't know why this is thrown
        }

        return true;

    }

    private CurrentlyPlayingContext getSongContext(String userId, String channelId, boolean retry) {
        if (!channels.containsKey(channelId))
            return null;

        GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest = channels.get(channelId).getApi()
                .getInformationAboutUsersCurrentPlayback()
                .build();

        try {
            return getInformationAboutUsersCurrentPlaybackRequest.execute();
        } catch (UnauthorizedException e) {
            updateTokens(userId, channelId);
            return getSongContext(userId, channelId, false);
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getDeviceId(String userId, String channelId, boolean retry) {

        if (!channels.containsKey(channelId))
            return "";

        GetUsersAvailableDevicesRequest getUsersAvailableDevicesRequest = channels.get(channelId).getApi()
                .getUsersAvailableDevices()
                .build();
        try {
            Device[] devices = getUsersAvailableDevicesRequest.execute();
            for (Device device : devices)
                if (device.getIs_active())
                    return device.getId();
        } catch (UnauthorizedException e) {
            updateTokens(userId, channelId);
            return getDeviceId(userId, channelId, false);
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

    private boolean updateTokens(String userId, String channelId) {
        if (!channels.containsKey(channelId)
                || !channels.get(channelId).getOwner().equals(userId))
            return false;

        SpotifyApi api = channels.get(channelId).getApi();
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = api
                .authorizationCodeRefresh()
                .build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
            api.setAccessToken(authorizationCodeCredentials.getAccessToken());
            api.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            channels.get(channelId).setApi(api);
            return api.getAccessToken() != null && api.getRefreshToken() != null;
        } catch (IOException | SpotifyWebApiException e) {
            e.printStackTrace();
        }

        return false;
    }

}
