package org.cmdmac.enlarge.server.apps.filemanager;

import java.util.ArrayList;

/**
 * Created by fengzhiping on 2018/10/12.
 */

public class Dir extends File {
    private ArrayList<File> children = new ArrayList<>();

    public ArrayList<File> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<File> children) {
        this.children = children;
    }

    public Dir(String name, long lastModify, long size) {
        super(name, lastModify, size);
    }

    public Dir(java.io.File dir) {
        super(dir);

        java.io.File files[] = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                addFile(new File(f));
            }
        }
    }

    public void addFile(File f) {
        children.add(f);
    }
}
