package xyz.atsumeru.manager.helpers;

import com.jpro.webapi.WebAPI;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;

import java.util.Optional;

public class LayoutHelpers {
    public static final ColorAdjust COVER_ADJUST_DEFAULT = new ColorAdjust(0, 0, 0, 0);
    public static final DropShadow COVER_DROPSHADOW = new DropShadow(5, Color.BLACK);
    public static final ColorAdjust COVER_ADJUST_HOVER = new ColorAdjust(0, 0, -0.7, 0);

    public static final ColorAdjust COVER_ADJUST_MONOCHROME = new ColorAdjust(0, -1, -0.8, 0.3);
    public static final ColorAdjust COVER_ADJUST_MONOCHROME_HOVER = new ColorAdjust(0, 0, -0.5, 0);

    public static final Effect COVER_ADJUST_BOX_BLUR;
    public static final Effect COVER_ADJUST_BOX_BLUR_HOVER;

    public static final Effect BACKGROUND_ADJUST_BOX_BLUR;
    public static final Effect BACKGROUND_ADJUST_BOX_BLUR_HOVER;

    static {
        COVER_ADJUST_DEFAULT.setInput(COVER_DROPSHADOW);
        COVER_ADJUST_HOVER.setInput(COVER_DROPSHADOW);

        COVER_ADJUST_MONOCHROME_HOVER.setInput(COVER_ADJUST_MONOCHROME);

        ColorAdjust colorAdjustForWeb = new ColorAdjust(0, -0.8, -0.8, 0);
        COVER_ADJUST_BOX_BLUR = Optional.of(new BoxBlur(8, 8, 3))
                .filter(effect -> !WebAPI.isBrowser())
                .map(effect -> {
                    effect.setInput(COVER_ADJUST_DEFAULT);
                    return effect;
                })
                .map(Effect.class::cast)
                .orElse(colorAdjustForWeb);

        COVER_ADJUST_BOX_BLUR_HOVER = Optional.of(new BoxBlur(8, 8, 3))
                .filter(effect -> !WebAPI.isBrowser())
                .map(effect -> {
                    effect.setInput(COVER_ADJUST_HOVER);
                    return effect;
                })
                .map(Effect.class::cast)
                .orElse(colorAdjustForWeb);

        BACKGROUND_ADJUST_BOX_BLUR = Optional.of(new BoxBlur(12, 12, 3))
                .filter(effect -> !WebAPI.isBrowser())
                .map(effect -> {
                    effect.setInput(COVER_DROPSHADOW);
                    return effect;
                })
                .map(Effect.class::cast)
                .orElse(colorAdjustForWeb);

        BACKGROUND_ADJUST_BOX_BLUR_HOVER = Optional.of(new BoxBlur(12, 12, 3))
                .filter(effect -> !WebAPI.isBrowser())
                .map(effect -> {
                    effect.setInput(COVER_ADJUST_HOVER);
                    return effect;
                })
                .map(Effect.class::cast)
                .orElse(colorAdjustForWeb);
    }

    /**
     * Set whether all Button children of the given Parent are visible.
     *
     * @param parent  the parent node
     * @param visible whether the buttons should be visible
     */
    public static void setChildButtonVisible(Parent parent, boolean visible) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setVisible(visible);
                button.setManaged(visible);
            } else if (child instanceof Parent) {
                setChildButtonVisible((Parent) child, visible);
            }
        }
    }
}
