package org.cmdmac.enlarge.server.apps.filemanager;

/**
 * Created by fengzhiping on 2018/10/12.
 */

public class File {
    private String name;
    private long lastModify;
    private long size;
    private boolean isDir = false;

    public boolean getIsDir() {
        return isDir;
    }

    public void setIsDir(boolean dir) {
        isDir = dir;
    }

    public File(String name, long lastModify, long size) {
        this.name = name;
        this.lastModify = lastModify;
        this.size = size;
    }

    public File(java.io.File file) {
        setName(file.getName());
        setLastModify(file.lastModified());
        setSize(file.length());
        setIsDir(file.isDirectory());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastModify() {
        return lastModify;
    }

    public void setLastModify(long lastModify) {
        this.lastModify = lastModify;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
