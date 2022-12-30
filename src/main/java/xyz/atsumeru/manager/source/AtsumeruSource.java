package xyz.atsumeru.manager.source;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.manager.ServerManager;
import com.atsumeru.api.model.AtsumeruMessage;
import com.atsumeru.api.model.Readable;
import com.atsumeru.api.model.server.Server;
import com.atsumeru.api.utils.AtsumeruApiConstants;
import com.atsumeru.api.utils.LibraryPresentation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.BuildProps;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.adapter.AtsumeruAdapter;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ConstantConditions")
public class AtsumeruSource {
    public static final String ARCHIVE_HASH_TAG = "atsumeru";
    public static final String SERIE_HASH_TAG = "atsumeru-serie";

    private static final AtsumeruAdapter adapterSeries = new AtsumeruAdapter(LibraryPresentation.SERIES);
    private static final AtsumeruAdapter adapterSeriesAndSingles = new AtsumeruAdapter(LibraryPresentation.SERIES_AND_SINGLES);
    private static final AtsumeruAdapter adapterSingles = new AtsumeruAdapter(LibraryPresentation.SINGLES);
    private static final AtsumeruAdapter adapterArchives = new AtsumeruAdapter(LibraryPresentation.ARCHIVES);

    private static final Map<Integer, AtsumeruSource> sources = new HashMap<>();

    private final Server server;

    public AtsumeruSource(Server server) {
        this.server = server;
        sources.put(server.getId(), this);
    }

    public static void init() {
        AtsumeruApiConstants.setHttpConnectTimeout(10000);
        AtsumeruApiConstants.setHttpReadTimeout(25000);
        AtsumeruApiConstants.setUserAgent(String.format("%s (v%s)", BuildProps.getAppName(), BuildProps.getVersion()));

        AtsumeruAPI.init(FXApplication.getOkHttpClient().newBuilder(), false);

        Optional.ofNullable(loadServers()).ifPresent(servers -> servers.forEach(AtsumeruSource::new));
    }

    private static List<Server> loadServers() {
        String serverListJson = Settings.Atsumeru.getAtsumeruServers();
        int currentServer = Settings.Atsumeru.getCurrentAtsumeruServer();

        if (GUString.isNotEmpty(serverListJson)) {
            List<Server> servers = new Gson().fromJson(serverListJson, new TypeToken<List<Server>>() {
            }.getType());

            AtsumeruAPI.getServerManager().addServers(servers);

            if (currentServer < 0 && GUArray.isNotEmpty(servers)) {
                currentServer = servers.get(0).getId();
            }

            if (currentServer >= 0) {
                AtsumeruAPI.changeServer(currentServer);
                System.out.println("AtsumeruSource@loadServers: set current server = " + currentServer);
            }

            return servers;
        }

        return null;
    }

    @Nullable
    public static Server getCurrentServer() {
        return AtsumeruAPI.getServerManager().getCurrentServer();
    }

    @Nullable
    public static Server getServerById(int serverId) {
        return AtsumeruAPI.getServerManager()
                .listServers()
                .stream()
                .filter(server -> server.getId() == serverId)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static AtsumeruSource getSource(@NonNull Server server) {
        return getSourceById(server.getId());
    }

    @Nullable
    public static AtsumeruSource getSourceById(int serverId) {
        AtsumeruAPI.changeServer(serverId);
        return sources.get(serverId);
    }

    public static void removeSource(int serverId) {
        AtsumeruAPI.getServerManager().removeServer(getServerById(serverId));
        sources.remove(serverId);
    }

    public static AtsumeruAdapter getAdapter(@Nullable LibraryPresentation presentation) {
        if (presentation == null) {
            return adapterSeries;
        }
        switch (presentation) {
            case SERIES_AND_SINGLES:
                return adapterSeriesAndSingles;
            case SINGLES:
                return adapterSingles;
            case ARCHIVES:
                return adapterArchives;
            case SERIES:
            default:
                return adapterSeries;
        }
    }

    public static String getCurrentUserName() {
        return getCurrentServer().getBasicCredentials().getFirst();
    }

    public static void requestSerieRescan(String serieId, Runnable runnable) {
        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(
                AtsumeruAPI.importerRescan(serieId, false, true)
                        .cache()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(Schedulers.io())
                        .subscribe(message -> {
                            runnable.run();
                            disposable.get().dispose();
                        }, throwable -> {
                            throwable.printStackTrace();
                            runnable.run();
                            disposable.get().dispose();
                        })
        );
    }

    public static boolean isChapterHash(String itemHash) {
        return GUString.isNotEmpty(itemHash) && !itemHash.startsWith(ARCHIVE_HASH_TAG);
    }

    public static void saveServers() {
        String json = new Gson().toJson(AtsumeruAPI.getServerManager().listServers());
        AtsumeruSettings.putAtsumeruServers(json);
    }

    public static List<Server> listServers() {
        return AtsumeruAPI.getServerManager().listServers();
    }

    public static Map<String, String> createAuthorizationHeaders() {
        return Optional.ofNullable(AtsumeruAPI.getServerManager())
                .map(ServerManager::getCurrentServer)
                .map(Server::createBasicCredentials)
                .map(credentials -> Collections.singletonMap("Authorization", credentials))
                .orElseGet(HashMap::new);
    }

    public Chapter fetchChapterItem(Chapter chapter) {
        Readable readable = !isChapterHash(chapter.getLink())
                ? AtsumeruAPI.getBookVolume(chapter.getLink()).blockingGet()
                : AtsumeruAPI.getBookChapter(chapter.getLink()).blockingGet();
        for (int i = 1; i <= readable.getPagesCount(); i++) {
            chapter.getImages().add(getPageUrl(readable.getId(), i));
        }

        return chapter;
    }

    private String getPageUrl(String volumeHash, int page) {
        return AtsumeruApiConstants.getPagesUrl()
                .replace("{hash}", volumeHash)
                .replace("{page}", String.valueOf(page))
                .replace("{is_convert}", String.valueOf(false));
    }

    // Sync
    public Single<AtsumeruMessage> syncReaded(String hash, String chapterHash, int currentPage) {
        System.out.println("Atsumeru@syncReaded: syncing " + hash + " (chapter = " + chapterHash + ") with progress = " + currentPage);
        return AtsumeruAPI.getUpdateReadHistory(hash, chapterHash, currentPage);
    }

    public Single<AtsumeruMessage> syncReadedBatch(Map<String, String> values) {
        System.out.println("Atsumeru@syncReaded: syncing multiple values: " + values.toString());
        return AtsumeruAPI.postUpdateReadHistory(values);
    }

    public void deleteSyncHistory(String itemHash) {
        syncReaded(itemHash, AtsumeruSource.isChapterHash(itemHash) ? itemHash : null, 0)
                .cache()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        responseMessage -> System.out.println("Atsumeru@deleteSyncHistory: got message from server - " + responseMessage.getMessage()),
                        Throwable::printStackTrace
                );
    }

    public static class AtsumeruSettings {
        private static final String KEY_ATSUMERU_SERVERS = "atsumeru_servers";
        private static final String DEFAULT_ATSUMERU_SERVERS = "";
        private static final String KEY_CURRENT_ATSUMERU_SERVER = "current_atsumeru_server";
        private static final int DEFAULT_CURRENT_ATSUMERU_SERVER = -1;

        public static String getAtsumeruServers() {
            return Settings.getInstance().getString(KEY_ATSUMERU_SERVERS, DEFAULT_ATSUMERU_SERVERS);
        }

        public static void putAtsumeruServers(String value) {
            Settings.getInstance().putString(KEY_ATSUMERU_SERVERS, value);
        }

        public static int getCurrentAtsumeruServer() {
            return Settings.getInstance().getInt(KEY_CURRENT_ATSUMERU_SERVER, DEFAULT_CURRENT_ATSUMERU_SERVER);
        }

        public static void putCurrentAtsumeruServer(int value) {
            Settings.getInstance().putInt(KEY_CURRENT_ATSUMERU_SERVER, value);
        }
    }
}
