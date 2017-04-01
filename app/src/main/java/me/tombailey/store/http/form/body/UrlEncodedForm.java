package me.tombailey.store.http.form.body;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomba on 25/02/2017.
 */

public class UrlEncodedForm extends FormBody {

    private static final String UTF_8 = "UTF-8";


    private List<Param> mParams;

    private UrlEncodedForm(List<Param> params) {
        mParams = params;
    }

    @Override
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(mParams.size());

        byte[] equals = "=".getBytes(UTF_8);
        byte[] and = "&".getBytes(UTF_8);

        for (int index = 0; index < mParams.size(); index++) {
            Param param = mParams.get(index);
            String name = URLEncoder.encode(param.getName(), UTF_8);
            String value = URLEncoder.encode(param.getValue(), UTF_8);

            byteArrayOutputStream.write(name.getBytes(UTF_8));
            byteArrayOutputStream.write(equals);
            byteArrayOutputStream.write(value.getBytes(UTF_8));

            if (index != mParams.size() - 1) {
                byteArrayOutputStream.write(and);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    public String getContentType() {
        return "application/x-www-form-urlencoded; charset=" + UTF_8;
    }


    public static class Param {

        private String mName;
        private String mValue;

        public Param(String name, String value) {
            mName = name;
            mValue = value;
        }

        public String getName() {
            return mName;
        }

        public String getValue() {
            return mValue;
        }
    }

    public static class Builder {

        private List<Param> params;

        public Builder() {
            params = new ArrayList<Param>(8);
        }

        public Builder add(String name, String value) {
            params.add(new Param(name, value));
            return this;
        }

        public Builder add(String name, int value) {
            params.add(new Param(name, String.valueOf(value)));
            return this;
        }

        public Builder add(String name, boolean value) {
            params.add(new Param(name, String.valueOf(value)));
            return this;
        }

        public UrlEncodedForm build() {
            return new UrlEncodedForm(params);
        }

    }

}
