package xyz.atsumeru.manager.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class AppUpdate {
    @SerializedName("version_code")
    private int versionCode;
    @SerializedName("version_name")
    private String versionName;
    @SerializedName("min_version_code")
    private int minVersionCode;

    @SerializedName("changelog_en")
    private List<String> changelogEnglish;

    @SerializedName("changelog_ru")
    private List<String> changelogRussian;

    @SerializedName("update_url")
    private String updateUrl;
}
