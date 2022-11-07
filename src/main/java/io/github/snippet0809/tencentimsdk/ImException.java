package io.github.snippet0809.tencentimsdk;

public class ImException extends Exception {

    private String msg;

    public ImException(String msg) {
        super(msg);
    }
}