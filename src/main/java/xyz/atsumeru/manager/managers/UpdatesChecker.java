package xyz.atsumeru.manager.managers;

import com.google.gson.GsonBuilder;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.NonNull;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;
import xyz.atsumeru.manager.BuildProps;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.helpers.DialogBuilder;
import xyz.atsumeru.manager.helpers.JavaHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.models.AppUpdate;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.views.Snackbar;

import java.util.Map;

public class UpdatesChecker {
    public enum UpdateBranch { ALPHA, BETA, RELEASE }

    private static final String GITHUB_REPOSITORY = "https://atsumerudev.github.io/";

    public static void check(@NonNull StackPane dialogRootPane, UpdateBranch updateBranch, boolean showNoUpdatesNotification) {
        createApi().getUpdateInfo(BuildProps.getUpdatesCheckUrl())
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .map(updateMap -> updateMap.get(updateBranch.name().toLowerCase()))
                .subscribe(
                        update -> Platform.runLater(() -> showUpdateDialog(dialogRootPane, update, showNoUpdatesNotification)),
                        throwable -> {
                            throwable.printStackTrace();
                            Snackbar.showSnackBar(dialogRootPane, LocaleManager.getString("gui.unable_check_updates"), Snackbar.Type.ERROR);
                        }
                );
    }

    private static void showUpdateDialog(@NonNull StackPane dialogRootPane, AppUpdate update, boolean showNoUpdatesNotification) {
        int versionCode = BuildProps.getVersionCode();
        if (update.getVersionCode() > versionCode) {
            Label versionLabel = new Label(String.format("%s (v%s)\n", BuildProps.getAppName(), update.getVersionName()));
            versionLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

            Label changelogText = new Label(GUString.join("\n", LocaleManager.isCurrentSystemLocaleIsCyrillic() ? update.getChangelogRussian() : update.getChangelogEnglish()));
            changelogText.setFont(Font.font(11));

            DialogBuilder.create(dialogRootPane)
                    .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                    .withHeading(LocaleManager.getString("gui.update_available"))
                    .withBody(versionLabel)
                    .withBody(changelogText)
                    .withButton(ButtonType.YES, LocaleManager.getString("gui.download"), () -> JavaHelper.openURL(update.getUpdateUrl(), BuildProps.getSystemType()))
                    .withOnOpenRunnable(() -> FXApplication.dimContent(true))
                    .withOnCloseRunnable(() -> FXApplication.dimContent(false))
                    .withClosable(versionCode >= update.getMinVersionCode())
                    .show();
        } else if (showNoUpdatesNotification) {
            Snackbar.showSnackBar(dialogRootPane, LocaleManager.getString("gui.updates_latest_version_installed"), Snackbar.Type.SUCCESS);
        }
    }

    private static Api createApi() {
        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(GITHUB_REPOSITORY)
                .client(FXApplication.getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .build();

        return restAdapter.create(Api.class);
    }

    public interface Api {
        @GET
        Single<Map<String, AppUpdate>> getUpdateInfo(@Url String url);
    }
}
