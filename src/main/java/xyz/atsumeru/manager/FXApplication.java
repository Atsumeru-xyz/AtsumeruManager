package xyz.atsumeru.manager;

import com.jpro.webapi.WebAPI;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import org.controlsfx.control.StatusBar;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDecorator;
import xyz.atsumeru.manager.enums.AppCloseBehaviorInMetadataEditor;
import xyz.atsumeru.manager.facade.SSLSocketFactoryFacade;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.*;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FXApplication extends Application {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BuildProps.getAppName());

    @Getter
    private static FXApplication instance;

    @Getter @Setter
    private static IParsersManager parsersManager = new DummyParsersManager();

    @Getter
    private static final OkHttpClient okHttpClient = createOkHttpClient();

    @Getter
    private final BooleanProperty isAppResizing = new SimpleBooleanProperty(false);

    @Getter
    private Stage currentStage;
    @Getter
    private Node rootNode;
    @Getter
    private StatusBar statusBar;

    public static String getName() {
        return String.format("%s (v%s)", BuildProps.getAppName(), BuildProps.getVersion());
    }

    public static boolean isNativeImage() {
        try {
            Class<?> clazz = Class.forName("org.graalvm.nativeimage.ImageInfo");
            Method method = clazz.getDeclaredMethod("inImageCode");
            return (Boolean) method.invoke(clazz);
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        // Распаковка нужных библиотек (AWT, JVM, etc)
        WorkspaceManager.unpackLibraries();

        // Подгружаем конфигурационные файлы
        Settings.init();

        // Конфигурируем логгирование в STDOUT/STDERR
        configureFileLogging();

        // Подгружаем языковой ресурс
        LocaleManager.setLocale(Settings.getDefaultAppLanguageCode());
        LocaleManager.loadResourceBundle();

        // Инициализируем AtsumeruSource
        AtsumeruSource.init();

        // Инициализируем менеджер парсеров
        parsersManager.init();

        //Настраиваем среду. Создаем нужные папки, если их нет
        WorkspaceManager.configureWorkspace();

        // Инициализация библиотеки SevenZip
        initSevenZip();

        // Загрузка шрифтов
        loadFonts();

        // Загрузка главного FXML файла приложения
        Pair<Node, MainController> nodeMainControllerPair = FXUtils.loadFXML("/fxml/Main.fxml");
        Node rootNode = nodeMainControllerPair.getFirst();

        this.currentStage = primaryStage;
        this.rootNode = rootNode;

        // Создание декоратора и сцены
        Scene scene = createScene(primaryStage, rootNode);
        
        // Установка иконки приложения в таскбаре
        FXUtils.setTaskbarAppIcon(primaryStage, ViewUtils.createAppIconImage());
        FXUtils.setAppAccentColor(rootNode, Settings.getAppAccentColor());

        // Настройка Stage
        primaryStage.setTitle(getName());
        primaryStage.setMinWidth(250);
        primaryStage.setMinHeight(400);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Дополнительная инициализация MainController
        nodeMainControllerPair.getSecond().init();

        // Слушатель изменения размеров сцены
        ChangeListener<Number> stageSizeListener = createStageResizeListener();
        primaryStage.widthProperty().addListener(stageSizeListener);
        primaryStage.heightProperty().addListener(stageSizeListener);

        scene.getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);
    }

    private Scene createScene(Stage primaryStage, Node rootNode) {
        if (!WebAPI.isBrowser()) {
            Pair<JFXCustomDecorator, Scene> customDecoratorScenePair = ViewUtils.createDecoratorWithScene(primaryStage, rootNode, 1250, 800, false);
            this.statusBar = customDecoratorScenePair.getFirst().getStatusBar();
            return customDecoratorScenePair.getSecond();
        } else {
            return ViewUtils.createScene(primaryStage, StageStyle.UNDECORATED, (Parent) rootNode, 1250, 800);
        }
    }

    private void closeWindowEvent(@NotNull WindowEvent event) {
        if (GUString.equalsIgnoreCase(TabPaneManager.getSelectedTabId(), "tabContentEdit") && Settings.getAppCloseBehaviorInMetadataEditor() == AppCloseBehaviorInMetadataEditor.CLOSE_EDITOR) {
            event.consume();
            TabPaneManager.selectHomeTab();
        }
    }

    private void initSevenZip() {
        try {
            SevenZip.initSevenZipFromPlatformJAR(SevenZip.getPlatformBestMatch(), new File(WorkspaceManager.TMP_DIR));
        } catch (SevenZipNativeInitializationException e) {
            e.printStackTrace();
            System.err.println("Unable to load SevenZip!");
        }
    }

    private void loadFonts() {
        FXUtils.loadFont("/fonts/OpenSans-Bold.ttf");
        FXUtils.loadFont("/fonts/OpenSans-BoldItalic.ttf");
        FXUtils.loadFont("/fonts/OpenSans-ExtraBold.ttf");
        FXUtils.loadFont("/fonts/OpenSans-ExtraBoldItalic.ttf");
        FXUtils.loadFont("/fonts/OpenSans-Italic.ttf");
        FXUtils.loadFont("/fonts/OpenSans-Light.ttf");
        FXUtils.loadFont("/fonts/OpenSans-LightItalic.ttf");
        FXUtils.loadFont("/fonts/OpenSans-Regular.ttf");
        FXUtils.loadFont("/fonts/OpenSans-SemiBold.ttf");
        FXUtils.loadFont("/fonts/OpenSans-SemiBoldItalic.ttf");
    }

    private ChangeListener<Number> createStageResizeListener() {
        return new ChangeListener<>() {
            final Timer timer = new Timer();
            final long delayTime = 200;
            TimerTask task = null;

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, final Number newValue) {
                isAppResizing.set(true);
                if (task != null) {
                    task.cancel();
                }

                task = new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            isAppResizing.set(false);
                            TabsManager.getTabController(TabAtsumeruLibraryController.class).recalculateGridView();
                        });
                    }
                };
                // schedule new task
                timer.schedule(task, delayTime);
            }
        };
    }

    public static void dimContent(boolean isDim) {
        animateDimContent(Duration.seconds(.15), isDim);
    }

    public static void animateDimContent(Duration duration, boolean isDim) {
        MainController controller = TabsManager.getTabController(MainController.class);
        controller.opaqueLayer.setOpacity(isDim ? 0 : 1);
        ViewUtils.setNodeVisibleAndManaged(isDim, controller.opaqueLayer);
        FadeTransition transition = new FadeTransition(duration, controller.opaqueLayer);
        transition.setFromValue(isDim ? 0 : 1);
        transition.setToValue(isDim ? 1 : 0);
        transition.playFromStart();
    }

    public static URL getResource(String name) {
        return instance.getClass().getResource(name);
    }

    private static void configureFileLogging() {
        if (isNativeImage()) {
            try {
                System.setOut(new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {}

                    @Override
                    public void write(byte @NotNull [] b) {}

                    @Override
                    public void write(byte @NotNull [] b, int off, int len) {}
                }));
                System.setErr(new PrintStream("./error.log"));
            } catch (FileNotFoundException ignored) {
            }
        }
    }

    private static OkHttpClient createOkHttpClient() {
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);

        return new OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .allEnabledTlsVersions()
                        .allEnabledCipherSuites()
                        .build(), ConnectionSpec.CLEARTEXT))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2))
                .sslSocketFactory(
                        new SSLSocketFactoryFacade(),
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                )
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();
    }
}
