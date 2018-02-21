package de.juliansauer.spotifyshare.rest;

public class Song {

    private final String title;
    private final String artists;

    public Song(String title, String artists) {
        this.title = title;
        this.artists = artists;
    }

    public String getTitle() {
        return title;
    }

    public String getArtists() {
        return artists;
    }

}
