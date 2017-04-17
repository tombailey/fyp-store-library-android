package me.tombailey.store.http;

import android.util.Base64;
import android.util.Log;

import javax.net.ssl.SSLSocketFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import me.tombailey.store.http.form.body.FormBody;
import me.tombailey.store.http.internal.cache.CachedFile;

/**
 * Created by Tom on 20/01/2017.
 */
public class Request {

    private static final String LOG_TAG = Response.class.getName();

    private static final int ONE_MINUTE_IN_SECONDS = 60000;

    private static final String CRLF = "\r\n";
    private static final String ISO_8859_1 = "ISO-8859-1";


    private static Cache sCache;


    private Proxy mProxy;

    private String mUrl;
    private String mMethod;
    private List<Header> mHeaders;

    private FormBody mFormBody;

    private int mTimeout;

    private Request(Proxy proxy, String url, String method, List<Header> headers, int timeout) {
        this(proxy, url, method, headers, timeout, null);
    }

    private Request(Proxy proxy, String url, String method, List<Header> headers, int timeout,
                    FormBody formBody) {
        mProxy = proxy;

        mUrl = url;
        mMethod = method;
        mHeaders = headers;
        mFormBody = formBody;

        mTimeout = timeout;
    }

    public Response execute() throws IOException {
        String identifier = mMethod + mUrl;
        if (isResponseCached(identifier)) {
            return getCachedResponse(identifier);
        }

        Socket socket = createSocket();
        //grab inputStream before request is sent, and therefore before data is received
        InputStream socketInputStream = socket.getInputStream();
        writeRequest(socket);

        Response response = getResponse(socketInputStream);
        socket.close();

        if (shouldCache(response)) {
            tryToCacheResponse(response);
        }

        return response;
    }

    public boolean isResponseCached(String cachedFileIdentifier) {
        if (sCache == null) {
            return false;
        } else {
            Realm realm = sCache.getRealm();
            RealmQuery<CachedFile> cachedFileQuery = realm.where(CachedFile.class)
                    .equalTo("id", cachedFileIdentifier);
            CachedFile cachedFile = cachedFileQuery.findFirst();

            if (cachedFile == null) {
                realm.close();
                return false;
            } else {
                if (cachedFile.getValidUntil() <= System.currentTimeMillis()) {
                    removeCachedFile(realm, cachedFile);
                    realm.close();
                    return false;
                } else {
                    realm.close();
                    return true;
                }
            }
        }
    }

    private void removeCachedFile(Realm realm, CachedFile cachedFile) {
        new File(cachedFile.getFilePath()).delete();
        realm.beginTransaction();
        cachedFile.deleteFromRealm();
        realm.commitTransaction();
    }

    private Response getCachedResponse(String cacheFileIdentifier) throws IOException {
        Realm realm = sCache.getRealm();
        RealmQuery<CachedFile> cachedFileQuery = realm.where(CachedFile.class)
                .equalTo("id", cacheFileIdentifier);

        CachedFile cachedFile = cachedFileQuery.findFirst();
        realm.beginTransaction();
        cachedFile.setLastUsed(System.currentTimeMillis());
        realm.commitTransaction();
        File cacheFile = new File(cachedFile.getFilePath());
        realm.close();

        return responseFromFile(cacheFile);
    }

    private Response responseFromFile(File cacheFile) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(cacheFile);
        Response response = Response.fromInputStream(fileInputStream);
        fileInputStream.close();
        return response;
    }

    private Socket createSocket() throws IOException {
        URL url = new URL(mUrl);

        int port = url.getPort();
        if (port == -1) {
            if (mUrl.toLowerCase().startsWith("https://")) {
                port = 443;
            } else {
                port = 80;
            }
        }

        String host = url.getHost();
        Socket socket = mProxy.getSocketFor(host, port, mTimeout);

        //handle SSL/TLS handshake for HTTPS connections
        if (port == 443) {
            socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, port, true);
        }

        return socket;
    }

    private void writeRequest(Socket socket) throws IOException {
        URL url = new URL(mUrl);
        String host = url.getHost();
        String file = url.getFile();
        if (file.equals("")) {
            file = "/";
        }

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        byte[] statusLineBytes = (mMethod + " " + file + " HTTP/1.1" + CRLF).getBytes(ISO_8859_1);
        dataOutputStream.write(statusLineBytes);


        //explicitly avoid keep-alive (not currently supported)
        byte[] connectionBytes = ("Connection: close" + CRLF).getBytes(ISO_8859_1);
        dataOutputStream.write(connectionBytes);
        byte[] hostBytes = ("Host: " + host + CRLF).getBytes(ISO_8859_1);
        dataOutputStream.write(hostBytes);

        for (Header header : mHeaders) {
            byte[] headerBytes = (header.getName() + ": " + header.getValue() + CRLF).getBytes(ISO_8859_1);
            dataOutputStream.write(headerBytes);
        }

        byte[] endOfHeaderBytes = CRLF.getBytes(ISO_8859_1);
        if (mFormBody != null) {
            byte[] contentTypeHeader =
                    ("Content-type: " + mFormBody.getContentType() + CRLF).getBytes(ISO_8859_1);
            dataOutputStream.write(contentTypeHeader);

            byte[] formBody = mFormBody.getBytes();

            byte[] contentLengthHeader =
                    ("Content-length: " + formBody.length + CRLF).getBytes(ISO_8859_1);
            dataOutputStream.write(contentLengthHeader);
            dataOutputStream.write(endOfHeaderBytes);

            dataOutputStream.write(formBody);
        } else {
            dataOutputStream.write(endOfHeaderBytes);
        }

        dataOutputStream.flush();
        //don't explicitly close OutputStream as keep-alive might be used
    }

    private Response getResponse(InputStream socketInputStream) throws IOException {
        return Response.fromInputStream(socketInputStream);
    }

    private boolean shouldCache(Response response) {
        String identifier = mMethod + mUrl;
        if (isResponseCached(identifier)) {
            return false;
        } else {
            for (Header header : response.getHeaders()) {
                if (header.getName().equalsIgnoreCase("cache-control")) {
                    String headerValue = header.getValue().toLowerCase();
                    if (headerValue.startsWith("public") || headerValue.startsWith("private")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void tryToCacheResponse(Response response) {
        if (sCache != null) {
            int cacheMaxAge = getCacheMaxAge(response);
            if (cacheMaxAge > 0) {
                int responseLength = response.length();
                if (responseLength <= sCache.getMaxSize()) {

                    if (responseLength > remainingCacheSpace()) {
                        removeCacheFilesForSpace(responseLength);
                    }

                    try {
                        String identifier = mMethod + mUrl;
                        //base64 encode to avoid problematic slashes, colons, etc in file names
                        File cacheTo = new File(sCache.getCacheDirectory(),
                                Base64.encodeToString(identifier.getBytes(), 0));
                        if (!cacheTo.exists()) {
                            cacheTo.createNewFile();
                        }
                        response.cache(cacheTo);

                        createCachedFile(response, identifier, cacheMaxAge, cacheTo);
                    } catch (IOException ioe) {
                        Log.w(LOG_TAG, "failed to cache a response");
                        ioe.printStackTrace();
                    }
                }


            }
        }
    }

    private void removeCacheFilesForSpace(int requiredSpace) {
        Realm realm = sCache.getRealm();
        RealmResults<CachedFile> largestCachedFiles =
                realm.where(CachedFile.class).findAllSorted("lastUsed", Sort.DESCENDING);
        int index = 0;
        while (remainingCacheSpace() > requiredSpace) {
            removeCachedFile(realm, largestCachedFiles.get(index));
            index++;
        }
        realm.close();
    }

    private void createCachedFile(Response response, String identifier, int cacheMaxAge, File cacheTo) throws IOException {
        Realm realm = sCache.getRealm();
        realm.beginTransaction();
        long validUntil = System.currentTimeMillis() + (cacheMaxAge * 1000);
        CachedFile cachedFile = CachedFile.create(identifier, validUntil, System.currentTimeMillis(),
                response.length(), cacheTo.getAbsolutePath());
        realm.insert(cachedFile);
        realm.commitTransaction();
        realm.close();
    }

    private int remainingCacheSpace() {
        //TODO: store size rather than computing each time?
        int maxSize = sCache.getMaxSize();
        int currentSize = 0;

        Realm realm = sCache.getRealm();
        RealmResults<CachedFile> cachedFileQuery = realm.where(CachedFile.class)
                .findAll();
        for (CachedFile cachedFile : cachedFileQuery) {
            currentSize += cachedFile.getSize();
        }
        realm.close();

        return maxSize - currentSize;
    }

    private int getCacheMaxAge(Response response) {
        int cacheMaxAge = -1;
        for (Header header : response.getHeaders()) {
            if (header.getName().equalsIgnoreCase("cache-control")) {
                String cacheDirective = header.getValue().toLowerCase();

                int maxAgeStarts = cacheDirective.indexOf("max-age=") + 8;
                if (maxAgeStarts >= 0) {
                    String maxAgeTime = "";
                    for (int index = maxAgeStarts; index < cacheDirective.length(); index++) {
                        try {
                            maxAgeTime += Integer.parseInt(cacheDirective.substring(index, index + 1));
                        } catch (NumberFormatException nfe) {
                            break;
                        }
                    }
                    return Integer.parseInt(maxAgeTime);
                }
            }
        }
        return cacheMaxAge;
    }

    public static void setCache(Cache cache) {
        sCache = cache;
    }

    public static class Builder {

        private Proxy mProxy;

        private String mUrl;
        private Method mMethod;
        private List<Header> mHeaders;

        private FormBody mFormBody;

        private Integer mTimeout;

        public Builder() {
            mHeaders = new ArrayList<Header>(4);
            mTimeout = ONE_MINUTE_IN_SECONDS;
        }

        public Builder proxy(Proxy proxy) {
            mProxy = proxy;
            return this;
        }

        public Builder url(String url) throws MalformedURLException, InsecureRequestException {
            return url(new URL(url));
        }

        public Builder url(URL url) throws InsecureRequestException {
            if (!url.getHost().toLowerCase().endsWith(".onion") &&
                    url.getProtocol().equalsIgnoreCase("http")) {
                throw new InsecureRequestException("destination is not hosted over HTTPS and " +
                        "outside of the TOR network. It is vulnerable to interception and/or " +
                        "manipulation. See " +
                        "https://www.torproject.org/docs/faq.html.en#CanExitNodesEavesdrop");
            }

            mUrl = url.toString();
            return this;
        }

        public Builder timeout(int milliseconds) {
            mTimeout = milliseconds;
            return this;
        }

        public Builder get() {
            mMethod = Method.GET;
            return this;
        }

        public Builder post() {
            return post(null);
        }

        public Builder post(FormBody formBody) {
            mMethod = Method.POST;
            mFormBody = formBody;
            return this;
        }

        public Builder delete() {
            return delete(null);
        }

        public Builder delete(FormBody formBody) {
            mMethod = Method.DELETE;
            mFormBody = formBody;
            return this;
        }

        public Builder put() {
            return put(null);
        }

        public Builder put(FormBody formBody) {
            mMethod = Method.PUT;
            mFormBody = formBody;
            return this;
        }

        public Builder header(String name, String value) {
            mHeaders.add(new Header(name, value));
            return this;
        }

        public Request build() {
            if (mProxy == null || mUrl == null || mMethod == null) {
                throw new IllegalArgumentException("method (DELETE, GET, POST, PUT), proxy or url was missing");
            }

            if (mFormBody == null) {
                return new Request(mProxy, mUrl, mMethod.getValue(), mHeaders, mTimeout);
            } else {
                return new Request(mProxy, mUrl, mMethod.getValue(), mHeaders, mTimeout, mFormBody);
            }
        }

    }

}
