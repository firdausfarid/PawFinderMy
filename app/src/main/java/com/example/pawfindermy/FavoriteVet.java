package com.example.pawfindermy;

public class FavoriteVet {
    public String placeId;
    public String name;
    public String address;


    public FavoriteVet() { }

    public FavoriteVet(String placeId, String name, String address) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
    }
}