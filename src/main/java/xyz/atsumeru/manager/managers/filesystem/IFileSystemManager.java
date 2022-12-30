package xyz.atsumeru.manager.managers.filesystem;

import io.reactivex.Single;
import xyz.atsumeru.manager.models.filesystem.DirectoryListing;

public interface IFileSystemManager {
    Single<DirectoryListing> getDirectoryListing(String requestPath);
}