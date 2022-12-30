package xyz.atsumeru.manager.models.filesystem;

import com.google.gson.annotations.Expose;

public class DirectoryPath {
    @Expose
    public String type;
    @Expose
    public String name;
    @Expose
    public String path;

    public DirectoryPath(String type, String name, String path) {
        this.type = type;
        this.name = name;
        this.path = path;
    }
}