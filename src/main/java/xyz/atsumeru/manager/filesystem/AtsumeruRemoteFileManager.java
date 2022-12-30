package xyz.atsumeru.manager.filesystem;

import com.atsumeru.api.AtsumeruAPI;
import io.reactivex.Single;
import xyz.atsumeru.manager.managers.filesystem.IFileSystemManager;
import xyz.atsumeru.manager.models.filesystem.DirectoryListing;
import xyz.atsumeru.manager.models.filesystem.DirectoryPath;

import java.util.stream.Collectors;

public class AtsumeruRemoteFileManager implements IFileSystemManager {

    public static AtsumeruRemoteFileManager create() {
        return new AtsumeruRemoteFileManager();
    }

    private AtsumeruRemoteFileManager() {}

    @Override
    public Single<DirectoryListing> getDirectoryListing(String requestPath) {
        return AtsumeruAPI.getDirectoryListing(requestPath)
                .map(directory -> new DirectoryListing(
                        directory.getParent(),
                        directory.getDirectories()
                                .stream()
                                .map(path -> new DirectoryPath(path.getType(), path.getName(), path.getPath()))
                                .collect(Collectors.toList())
                ));
    }
}
