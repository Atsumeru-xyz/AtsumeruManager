package xyz.atsumeru.manager.managers;

import io.reactivex.functions.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import xyz.atsumeru.manager.IParsersManager;
import xyz.atsumeru.manager.models.content.Content;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DummyParsersManager implements IParsersManager {

    @Override
    public void init() {
        // Stub
    }

    @Override
    public void reload() {
        // Stub
    }

    @Override
    public boolean isAuthorizationSuccess(long parserId) {
        return false;
    }

    @Override
    public boolean isSupportsAuthorization(long parserId) {
        return false;
    }

    @Override
    public boolean isWebViewAuthSupported(long parserId) {
        return false;
    }

    @Override
    public boolean isPreferCookiesAuth(long parserId) {
        return false;
    }

    @Override
    public void authorize(long parserId, Runnable onAuthRunnable) {
        // Stub
    }

    @Override
    public void checkParserUpdates(java.util.function.Consumer<List<Long>> onResult, java.util.function.Consumer<Throwable> onError) {
        // Stub
    }

    @Override
    public void updateParsers(List<Long> parserIds, java.util.function.Consumer<String> titleConsumer, BiConsumer<Integer, Integer> progressConsumer, java.util.function.Consumer<Throwable> throwableConsumer) {
        // Stub
    }

    @Override
    public void installParser(String link, String parserFileName, Runnable onResult, java.util.function.Consumer<Throwable> throwableConsumer) {
        // Stub
    }

    @Override
    public boolean ifParserPresent(long parserId) {
        return false;
    }

    @Override
    public boolean containsParser(String hostName) {
        return false;
    }

    @Override
    public String getParserFilePath(long parserId) {
        return null;
    }

    @Override
    public void createComicVineParser() {
        // Stub
    }

    @Override
    public void openComicVineAPIRequestUrl() {
        // Stub
    }

    @Override
    public void fetchAvailableRepositories(Consumer<List<Tab>> onSuccessConsumer, Consumer<Throwable> throwableConsumer) {
        // Stub
    }

    @Override
    public void fetchRepositoryCatalog(String repositoryType, Supplier<Pane> rootPaneSupplier, Supplier<FlowPane> flowPaneSupplier,
                                       Consumer<List<Node>> onSuccessConsumer, Consumer<Throwable> throwableConsumer) {
        // Stub
    }

    @Override
    public void fetchMetadata(String input, String secondInput, TextField comicsDBTextInput, TextField comicVineTextInput, java.util.function.Consumer<String> addLinkConsumer, BiConsumer<Boolean, Content> onResultConsumer) {
        // Stub
    }

    @Override
    public boolean isSupportsMetadataFetching() {
        return false;
    }
}
