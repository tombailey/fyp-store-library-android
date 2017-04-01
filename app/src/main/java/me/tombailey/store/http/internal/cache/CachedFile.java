package me.tombailey.store.http.internal.cache;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by tomba on 26/02/2017.
 */

public class CachedFile extends RealmObject {

    @PrimaryKey
    private String id;

    @Required
    @Index
    private Long validUntil;

    @Required
    @Index
    private Long lastUsed;

    @Required
    @Index
    private Integer size;

    @Required
    private String filePath;

    public CachedFile() {

    }

    public String getId() {
        return id;
    }

    public long getValidUntil() {
        return validUntil;
    }

    public int getSize() {
        return size;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public static CachedFile create(String id, long validUntil, long lastUsed, int size,
                                    String filePath) {
        CachedFile cachedFile = new CachedFile();
        cachedFile.setId(id);
        cachedFile.setValidUntil(validUntil);
        cachedFile.setLastUsed(lastUsed);
        cachedFile.setSize(size);
        cachedFile.setFilePath(filePath);
        return cachedFile;
    }
}
