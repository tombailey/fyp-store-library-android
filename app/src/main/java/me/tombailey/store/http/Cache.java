package me.tombailey.store.http;

import android.content.Context;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import me.tombailey.store.http.internal.RealmLibraryModule;

/**
 * Created by tomba on 25/02/2017.
 */

public class Cache {

    private File mCacheDirectory;
    private int mMaxSize;
    private RealmConfiguration mRealmConfiguration;

    /**
     *
     * @param cacheDirectory the directory to store cached files in
     * @param maxSize the max size, in bytes, that all files in the cache directory should not
     *                exceed
     */
    private Cache(File cacheDirectory, int maxSize, RealmConfiguration realmConfiguration) {
        mCacheDirectory = cacheDirectory;
        mMaxSize = maxSize;

        mRealmConfiguration = realmConfiguration;
    }

    protected File getCacheDirectory() {
        return mCacheDirectory;
    }

    protected int getMaxSize() {
        return mMaxSize;
    }

    protected Realm getRealm() {
        return Realm.getInstance(mRealmConfiguration);
    }

    public static class Builder {

        private File mCacheDirectory;
        private int mMaxSize;
        private RealmConfiguration mRealmConfiguration;

        public Builder() {

        }

        public Builder cacheDirectory(File cacheDirectory) {
            mCacheDirectory = cacheDirectory;
            return this;
        }

        public Builder maxSize(int maxSize) {
            mMaxSize = maxSize;
            return this;
        }

        public Builder context(Context context) {
            Realm.init(context);
            mRealmConfiguration = new RealmConfiguration.Builder()
                    .modules(new RealmLibraryModule())
                    .name("me.tombailey.store.http.cache")
                    .build();
            return this;
        }

        public Cache build() {
            return new Cache(mCacheDirectory, mMaxSize, mRealmConfiguration);
        }

    }
}
