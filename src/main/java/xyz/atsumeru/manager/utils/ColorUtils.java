package xyz.atsumeru.manager.utils;

import com.crazyxacker.libs.palettefx.Palette;
import com.crazyxacker.libs.palettefx.Target;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ColorUtils {
    private static final Color defaultColor = Color.web("#1c1d23");
    private static final List<Target> TARGETS = new ArrayList<>() {{
        add(Target.MUTED);
        add(Target.DARK_MUTED);
        add(Target.DARK_VIBRANT);
        add(Target.VIBRANT);
        add(Target.LIGHT_MUTED);
        add(Target.LIGHT_VIBRANT);
    }};

    public static String getImageAccent(@Nullable Image bufferedImage) {
        return Optional.ofNullable(bufferedImage)
                .map(image -> {
                    try {
                        return Palette.from(image).generate();
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .map(palette -> String.format("#%06x", 0xFFFFFF & com.crazyxacker.libs.palettefx.ColorUtils.getRGB(getMutedColorFromPalette(palette))))
                .orElse(null);
    }

    public static Color getMutedColorFromPalette(Palette palette) {
        return TARGETS.stream()
                .filter(target -> palette.getColorForTarget(target, defaultColor) != defaultColor)
                .findFirst()
                .map(target -> palette.getColorForTarget(target, defaultColor))
                .orElse(defaultColor);
    }
}
