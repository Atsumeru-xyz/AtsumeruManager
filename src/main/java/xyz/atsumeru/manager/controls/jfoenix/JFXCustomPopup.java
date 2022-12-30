/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package xyz.atsumeru.manager.controls.jfoenix;

import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import lombok.Setter;
import xyz.atsumeru.manager.skins.JFXCustomPopupSkin;

/**
 * JFXCustomPopup is the material design implementation of a popup.
 *
 * @author Shadi Shaheen
 * @version 2.0
 * @since 2017-03-01
 */
@DefaultProperty(value = "popupContent")
public class JFXCustomPopup extends PopupControl {
    @Setter private Runnable onShowRunnable;
    @Setter private Runnable onHideRunnable;

    public enum PopupHPosition {
        RIGHT, LEFT
    }

    public enum PopupVPosition {
        TOP, BOTTOM
    }

    /**
     * Creates empty popup.
     */
    public JFXCustomPopup() {
        this(null);
    }

    /**
     * creates popup with a specified container and content
     *
     * @param content the node that will be shown in the popup
     */
    public JFXCustomPopup(Region content) {
        setPopupContent(content);
        initialize();
    }

    private void initialize() {
        this.setAutoFix(false);
        this.setAutoHide(true);
        this.setHideOnEscape(true);
        this.setConsumeAutoHidingEvents(false);
        this.getStyleClass().add(DEFAULT_STYLE_CLASS);
        getScene().getRoot().setStyle("-fx-background-color: TRANSPARENT");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXCustomPopupSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     * Setters / Getters                                                       *
     *                                                                         *
     **************************************************************************/

    private final ObjectProperty<Region> popupContent = new SimpleObjectProperty<>(new Pane());

    public final ObjectProperty<Region> popupContentProperty() {
        return this.popupContent;
    }

    public final Region getPopupContent() {
        return this.popupContentProperty().get();
    }

    public final void setPopupContent(final Region popupContent) {
        this.popupContentProperty().set(popupContent);
    }

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * show the popup using the default position
     */
    public void show(Node node) {
        this.show(node, PopupVPosition.TOP, PopupHPosition.LEFT, 0, 0);
    }

    /**
     * show the popup according to the specified position
     *
     * @param vAlign can be TOP/BOTTOM
     * @param hAlign can be LEFT/RIGHT
     */
    public void show(Node node, PopupVPosition vAlign, PopupHPosition hAlign) {
        this.show(node, vAlign, hAlign, 0, 0);
    }

    /**
     * show the popup according to the specified position with a certain offset
     *
     * @param vAlign      can be TOP/BOTTOM
     * @param hAlign      can be LEFT/RIGHT
     * @param initOffsetX on the x axis
     * @param initOffsetY on the y axis
     */
    public void show(Node node, PopupVPosition vAlign, PopupHPosition hAlign, double initOffsetX, double initOffsetY) {
        if (!isShowing()) {
            if (onShowRunnable != null) {
                onShowRunnable.run();
            }
            if (node.getScene() == null || node.getScene().getWindow() == null) {
                throw new IllegalStateException("Can not show popup. The node must be attached to a scene/window.");
            }
            Window parent = node.getScene().getWindow();
            final Point2D origin = node.localToScene(0, 0);
            final double anchorX = parent.getX() + origin.getX()
                    + node.getScene().getX() + (hAlign == PopupHPosition.RIGHT ? ((Region) node).getWidth() : 0);
            final double anchorY = parent.getY() + origin.getY()
                    + node.getScene()
                    .getY() + (vAlign == PopupVPosition.BOTTOM ? ((Region) node).getHeight() : 0);
            this.show(parent, anchorX, anchorY);
            ((JFXCustomPopupSkin) getSkin()).reset(vAlign, hAlign, initOffsetX, initOffsetY);
            Platform.runLater(() -> ((JFXCustomPopupSkin) getSkin()).animate());
        }
    }

    public void show(Window window, double x, double y, PopupVPosition vAlign, PopupHPosition hAlign, double initOffsetX, double initOffsetY) {
        if (!isShowing()) {
            if (onShowRunnable != null) {
                onShowRunnable.run();
            }
            if (window == null) {
                throw new IllegalStateException("Can not show popup. The node must be attached to a scene/window.");
            }
            Window parent = window;
            final double anchorX = parent.getX() + x + initOffsetX;
            final double anchorY = parent.getY() + y + initOffsetY;
            this.show(parent, anchorX, anchorY);
            ((JFXCustomPopupSkin) getSkin()).reset(vAlign, hAlign, initOffsetX, initOffsetY);
            Platform.runLater(() -> ((JFXCustomPopupSkin) getSkin()).animate());
        }
    }

    @Override
    public void hide() {
        super.hide();
        if (onHideRunnable != null) {
            onHideRunnable.run();
        }
        ((JFXCustomPopupSkin) getSkin()).init();
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Initialize the style class to 'jfx-popup'.
     * <p>
     * This is the selector class from which CSS can be used to style
     * this control.
     */
    private static final String DEFAULT_STYLE_CLASS = "jfx-popup";
}
