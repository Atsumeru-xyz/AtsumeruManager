package xyz.atsumeru.manager;

import io.reactivex.functions.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import xyz.atsumeru.manager.models.content.Content;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface IParsersManager {
    void init();
    void reload();

    boolean isAuthorizationSuccess(long parserId);
    boolean isSupportsAuthorization(long parserId);
    boolean isWebViewAuthSupported(long parserId);
    boolean isPreferCookiesAuth(long parserId);
    void authorize(long parserId, Runnable onAuthRunnable);

    void checkParserUpdates(java.util.function.Consumer<List<Long>> onResult, java.util.function.Consumer<Throwable> onError);
    void updateParsers(List<Long> parserIds, java.util.function.Consumer<String> titleConsumer, BiConsumer<Integer, Integer> progressConsumer, java.util.function.Consumer<Throwable> throwableConsumer);
    void installParser(String link, String parserFileName, Runnable onResult, java.util.function.Consumer<Throwable> throwableConsumer);

    boolean ifParserPresent(long parserId);
    boolean containsParser(String hostName);

    String getParserFilePath(long parserId);

    void createComicVineParser();
    void openComicVineAPIRequestUrl();

    void fetchAvailableRepositories(Consumer<List<Tab>> onSuccessConsumer, Consumer<Throwable> throwableConsumer);
    void fetchRepositoryCatalog(String repositoryType, Supplier<Pane> rootPaneSupplier, Supplier<FlowPane> flowPaneSupplier,
                                Consumer<List<Node>> onSuccessConsumer, Consumer<Throwable> throwableConsumer);

    void fetchMetadata(String input, String secondInput, TextField comicsDBTextInput, TextField comicVineTextInput, java.util.function.Consumer<String> addLinkConsumer, BiConsumer<Boolean, Content> onResultConsumer);
    boolean isSupportsMetadataFetching();
}
