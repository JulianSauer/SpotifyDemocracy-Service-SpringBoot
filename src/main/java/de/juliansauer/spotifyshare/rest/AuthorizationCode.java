package de.juliansauer.spotifyshare.rest;

public class AuthorizationCode {

    private final String uri;

    public AuthorizationCode(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

}
