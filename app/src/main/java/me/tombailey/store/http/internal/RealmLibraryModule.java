package me.tombailey.store.http.internal;

import io.realm.annotations.RealmModule;
import me.tombailey.store.http.internal.cache.CachedFile;

/**
 * Created by tomba on 09/03/2017.
 */

@RealmModule(library = true, classes = {CachedFile.class})
public class RealmLibraryModule {
}
