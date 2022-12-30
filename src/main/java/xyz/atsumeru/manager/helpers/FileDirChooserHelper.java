package xyz.atsumeru.manager.helpers;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class FileDirChooserHelper {
    private static final DirectoryChooser directoryChooser = new DirectoryChooser();
    private static final FileChooser fileChooser = new FileChooser();

    public static File chooseDirectory() {
        return directoryChooser.showDialog(FXApplication.getInstance().getCurrentStage());
    }

    public static File chooseFile(@Nullable String extensionDescription, @Nullable Collection<String> extensions) {
        configureExtensionFilters(extensionDescription, GUArray.isNotEmpty(extensions) ? extensions.toArray(new String[0]) : null);
        return fileChooser.showOpenDialog(FXApplication.getInstance().getCurrentStage());
    }

    public static List<File> chooseFiles(@Nullable String extensionDescription, @Nullable Collection<String> extensions) {
        configureExtensionFilters(extensionDescription, GUArray.isNotEmpty(extensions) ? extensions.toArray(new String[0]) : null);
        return fileChooser.showOpenMultipleDialog(FXApplication.getInstance().getCurrentStage());
    }

    private static void configureExtensionFilters(@Nullable String extensionDescription, @Nullable String... extensions) {
        if (GUString.isNotEmpty(extensionDescription) && GUArray.isNotEmpty(extensions)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extensionDescription, extensions));
        } else {
            fileChooser.getExtensionFilters().clear();
        }
    }
}

