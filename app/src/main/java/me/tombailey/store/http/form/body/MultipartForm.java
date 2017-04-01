package me.tombailey.store.http.form.body;

import java.io.IOException;

/**
 * Created by tomba on 25/02/2017.
 */

public class MultipartForm extends FormBody {
    @Override
    public byte[] getBytes() throws IOException {
        //TODO: impl
        return new byte[0];
    }

    @Override
    public String getContentType() {
        //TODO: impl
        return null;
    }
}
