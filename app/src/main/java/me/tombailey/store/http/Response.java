package me.tombailey.store.http;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.tombailey.store.http.internal.Pair;

/**
 * Created by Tom on 20/01/2017.
 */
public class Response {

    public static final int CARRIAGE_RETURN = 0x0D;
    public static final int LINE_FEED = 0x0A;


    private static final String LOG_TAG = Response.class.getName();

    private static final String ISO_8859_1 = "ISO-8859-1";

    private static final String CRLF = "\r\n";


    private String mHttpVersion;

    private int mStatusCode;
    private String mStatusText;

    private Header[] mHeaders;
    private byte[] mMessageBody;

    protected Response(String httpVersion, int statusCode, String statusText, Header[] headers, byte[] messageBody) {
        mHttpVersion = httpVersion;
        mStatusCode = statusCode;
        mStatusText = statusText;

        mHeaders = headers;
        mMessageBody = messageBody;
    }

    public String getHttpVersion() {
        return mHttpVersion;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getStatusText() {
        return mStatusText;
    }

    public Header[] getHeaders() {
        return mHeaders;
    }

    public Header getHeader(String name) {
        for (Header header : mHeaders) {
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    public byte[] getMessageBody() {
        return mMessageBody;
    }

    public String getMessageBodyString() throws UnsupportedEncodingException {
        Header contentTypeHeader = getHeader("content-type");
        if (contentTypeHeader == null) {
            Log.w(LOG_TAG, "content-type header missing, getMessageBodyString() may have the wrong encoding");
            return new String(mMessageBody);
        }

        String contentTypeStr = contentTypeHeader.getValue();
        String charSet = contentTypeStr.substring(contentTypeStr.indexOf("charset=") + 8);
        return new String(mMessageBody, charSet);
    }

    protected int length() {
        int statusLineLength = (mHttpVersion + " " + mStatusCode + " " + mStatusText).getBytes().length + 2;

        int headersLength = 0;
        for (Header header : mHeaders) {
            headersLength +=
                    (header.getName().getBytes().length +
                            ": ".getBytes().length +
                            header.getValue().getBytes().length) + 2;
        }
        //for CRLF after headers finish
        headersLength += 2;

        return statusLineLength + headersLength + mMessageBody.length;
    }

    protected void cache(File cacheFile) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
        fileOutputStream.write((mHttpVersion + " " + mStatusCode + " " + mStatusText + CRLF).getBytes());

        for (Header header : mHeaders) {
            fileOutputStream.write(header.getName().getBytes(ISO_8859_1));
            fileOutputStream.write(": ".getBytes(ISO_8859_1));
            fileOutputStream.write(header.getValue().getBytes(ISO_8859_1));
            fileOutputStream.write(new byte[]{CARRIAGE_RETURN, LINE_FEED});
        }
        fileOutputStream.write(new byte[]{CARRIAGE_RETURN, LINE_FEED});

        fileOutputStream.write(mMessageBody);
        fileOutputStream.close();
    }


    public static Response fromInputStream(InputStream inputStream) throws IOException {
        byte[] response = getByteResponseFromInputStream(inputStream);

        Pair<Integer, String[]> metaData = getMetaData(response);
        int metaDataLength = metaData.first();
        String[] metaDataParts = metaData.second();


        String[] statusLineParts = metaDataParts[0].split(" ");
        String httpVersion = statusLineParts[0];
        int statusCode = Integer.parseInt(statusLineParts[1]);
        String statusText = statusLineParts[2];

        Header[] headers = getHeaders(metaDataParts);

        byte[] messageBody = Arrays.copyOfRange(response, metaDataLength, response.length);

        return new Response(httpVersion, statusCode, statusText, headers, messageBody);
    }

    private static byte[] getByteResponseFromInputStream(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);


        byte[] buffer = new byte[1024]; //1kb
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10 * 1024); //10kb

        int bytesRead = bufferedInputStream.read(buffer);
        while (bytesRead != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
            bytesRead = bufferedInputStream.read(buffer);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private static Pair<Integer, String[]> getMetaData(byte[] response) {
        int metaDataLength = 0;
        List<String> metaDataParts = new ArrayList<String>(16);
        ByteArrayOutputStream metaDataPartByteArray = new ByteArrayOutputStream(256);

        for (int index = 0; index + 3 < response.length; index++) {
            if (response[index] == CARRIAGE_RETURN && response[index + 1] == LINE_FEED) {
                if (response[index + 2] == CARRIAGE_RETURN && response[index + 3] == LINE_FEED) {
                    metaDataLength = index + 4;
                    //double CRLF indicates end of headers
                    break;
                } else {
                    try {
                        metaDataParts.add(metaDataPartByteArray.toString(ISO_8859_1));
                    } catch (UnsupportedEncodingException uee) {
                        throw new RuntimeException(uee);
                    }

                    //skip over line_feed
                    index++;

                    metaDataPartByteArray = new ByteArrayOutputStream(256);
                }
            } else {
                metaDataPartByteArray.write(response, index, 1);
            }
        }

        return new Pair<Integer, String[]>(metaDataLength,
                metaDataParts.toArray(new String[metaDataParts.size()]));
    }

    private static Header[] getHeaders(String[] metaDataParts) {
        Header[] headers = new Header[metaDataParts.length - 1];
        for (int index = 1; index < metaDataParts.length; index++) {
            String[] nameValue = metaDataParts[index].split(": ");
            headers[index - 1] = new Header(nameValue[0], nameValue[1]);
        }
        return headers;
    }

}
