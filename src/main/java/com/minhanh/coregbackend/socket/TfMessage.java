package com.minhanh.coregbackend.socket;

public class TfMessage {
    private String id;

    public TfMessage() {}

    public TfMessage(String name) {
        this.id = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
