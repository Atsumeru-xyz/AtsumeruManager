package xyz.atsumeru.manager.controls.jfoenix;

import com.jfoenix.controls.JFXRippler;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

public class JFXRoundedRippler extends JFXRippler {
    private final int maskArcHeight;
    private final int maskArcWidth;

    public JFXRoundedRippler(Node control, int maskArcHeight, int maskArcWidth) {
        super(control, RipplerMask.RECT, RipplerPos.FRONT);
        this.maskArcHeight = maskArcHeight;
        this.maskArcWidth = maskArcWidth;
    }

    @Override
    protected Node getMask() {
        Node mask = super.getMask();
        if (mask instanceof Rectangle) {
            Rectangle rectangle = (Rectangle) mask;
            rectangle.setArcHeight(maskArcHeight);
            rectangle.setArcWidth(maskArcWidth);
        }
        return mask;
    }
}
