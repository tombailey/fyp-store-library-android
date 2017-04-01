package me.tombailey.store.http;

/**
 * Created by tomba on 26/01/2017.
 */

public interface ResultListener<T> {

    void onResult(T result);
    void onError(Throwable throwable);

}
