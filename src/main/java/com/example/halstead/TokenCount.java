package com.example.halstead;

public class TokenCount {
    private String token;
    private int frequency;

    public TokenCount(String token, int frequency) {
        this.token = token;
        this.frequency = frequency;
    }
    public String getToken() { return token; }
    public int getFrequency() { return frequency; }
}