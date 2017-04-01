package me.tombailey.store.http;

/**
 * Created by Tom on 20/01/2017.
 */
public enum Method {

    GET, POST, DELETE, PUT;

    public String getValue() {
        return toString();
    }

}
