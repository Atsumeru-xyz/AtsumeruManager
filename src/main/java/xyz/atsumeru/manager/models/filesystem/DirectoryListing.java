package xyz.atsumeru.manager.models.filesystem;

import com.google.gson.annotations.Expose;

import java.util.List;

public class DirectoryListing {
    @Expose
    public String parent;
    @Expose
    public List<DirectoryPath> directories;

    public DirectoryListing(String parent, List<DirectoryPath> directories) {
        this.parent = parent;
        this.directories = directories;
    }
}