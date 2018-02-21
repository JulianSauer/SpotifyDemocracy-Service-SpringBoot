package de.juliansauer.spotifyshare.voting;

import com.wrapper.spotify.SpotifyApi;

import java.util.HashSet;
import java.util.Set;

public class Channel {

    private Set<String> users;

    private String owner;

    private String name;

    private SpotifyApi api;

    public Channel(String owner, String name, SpotifyApi api) {
        this.owner = owner;
        this.name = name;
        this.api = api;

        users = new HashSet<>();
        users.add(owner);
    }

    public boolean addUser(String newUser) {
        if (users.contains(newUser))
            return false;
        users.add(newUser);
        return true;
    }

    public boolean removeUser(String user) {
        if (!users.contains(user))
            return false;
        users.remove(user);
        return true;
    }

    public Set<String> getUsers() {
        return users;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public SpotifyApi getApi() {
        return api;
    }

    public void setApi(SpotifyApi api) {
        this.api = api;
    }

}
