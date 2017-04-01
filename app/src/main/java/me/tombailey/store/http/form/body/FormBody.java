package me.tombailey.store.http.form.body;

import java.io.IOException;

/**
 * Created by Tom on 20/01/2017.
 */
public abstract class FormBody {

    public  abstract byte[] getBytes() throws IOException;

    public abstract String getContentType();

}
