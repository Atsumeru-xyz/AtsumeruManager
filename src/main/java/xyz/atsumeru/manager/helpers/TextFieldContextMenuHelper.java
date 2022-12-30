package xyz.atsumeru.manager.helpers;

import com.sun.javafx.scene.control.behavior.TextInputControlBehavior;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.control.skin.TextInputControlSkin;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.TreeMap;

public class TextFieldContextMenuHelper {
    private enum TextFieldModification { UPPERCASE, UPPERCASE_FIRST_WORD, UPPERCASE_ALL_WORDS, UPPERCASE_BETWEEN_SEPARATOR, LOWERCASE }
    private enum TextFieldUtility { COPY_ALL, CLEAR_ALL }

    private static Field behaviorField;
    private static Field contextMenuField;

    public static void localizeDefaultMenu(TextInputControl... inputControls) {
        Platform.runLater(() -> {
            for (TextInputControl inputControl : inputControls) {
                manipulateWithMenu(inputControl, false);
                behaviorField = null;
                contextMenuField = null;
            }
        });
    }

    public static void addMenuItemsIntoDefaultMenu(TextField... fields) {
        Platform.runLater(() -> {
            for (TextField field : fields) {
                manipulateWithMenu(field, true);
                behaviorField = null;
                contextMenuField = null;
            }
        });
    }

    private static void manipulateWithMenu(TextInputControl field, boolean addNewItems) {
        if (field.getSkin() instanceof TextInputControlSkin) {
            try {
                ContextMenu contextMenu = getContextMenu(field);
                contextMenu.showingProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        translateDefaultMenuItems(contextMenu);
                        if (addNewItems) {
                            ObservableList<MenuItem> items = contextMenu.getItems();
                            createMenuItems(field).forEach(items::add);
                        }
                    }
                });
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        } else if (field instanceof MFXTextField) {
            MFXContextMenu contextMenu = getMFXContextMenu(field);
            translateDefaultMenuItems(contextMenu);

            if (addNewItems) {
                removeDeleteItem(contextMenu);
                ObservableList<Node> nodes = contextMenu.getItems();
                createMFXMenuItems(field).forEach(nodes::add);
            }

            setContextMenuIconsColor(contextMenu);
        }
    }

    private static ContextMenu getContextMenu(TextInputControl textField) throws NoSuchFieldException, IllegalAccessException {
        if (behaviorField == null) {
            if (textField instanceof TextField) {
                behaviorField = TextFieldSkin.class.getDeclaredField("behavior");
                behaviorField.setAccessible(true);
            } else if (textField instanceof TextArea) {
                behaviorField = TextAreaSkin.class.getDeclaredField("behavior");
                behaviorField.setAccessible(true);
            }
        }
        if (contextMenuField == null) {
            contextMenuField = TextInputControlBehavior.class.getDeclaredField("contextMenu");
            contextMenuField.setAccessible(true);
        }

        Object behavior = behaviorField.get(textField.getSkin());
        return (ContextMenu) contextMenuField.get(behavior);
    }

    private static MFXContextMenu getMFXContextMenu(TextInputControl textField) {
        MFXTextField mfxTextField = (MFXTextField) textField;
        return mfxTextField.getMFXContextMenu();
    }

    private static void setContextMenuIconsColor(MFXContextMenu contextMenu) {
        contextMenu.getItems()
                .stream()
                .filter(MFXContextMenuItem.class::isInstance)
                .map(MFXContextMenuItem.class::cast)
                .map(Labeled::getGraphic)
                .filter(Objects::nonNull)
                .filter(MFXFontIcon.class::isInstance)
                .map(MFXFontIcon.class::cast)
                .forEach(icon -> icon.setColor(Color.LIGHTGRAY));
    }

    private static void translateDefaultMenuItems(MFXContextMenu contextMenu) {
        contextMenu.getItems()
                .stream()
                .filter(MFXContextMenuItem.class::isInstance)
                .map(MFXContextMenuItem.class::cast)
                .forEach(mfxContextMenuItem -> {
                    try {
                        mfxContextMenuItem.setText(
                                LocaleManager.getString("context." + mfxContextMenuItem.getText().replace(" ", "_").toLowerCase())
                        );
                    } catch (Exception ignored) {
                        // Item may be translated already
                    }
                });
    }

    private static void removeDeleteItem(MFXContextMenu contextMenu) {
        contextMenu.getItems()
                .stream()
                .filter(MFXContextMenuItem.class::isInstance)
                .map(MFXContextMenuItem.class::cast)
                .filter(item -> GUString.equalsIgnoreCase(item.getText(), LocaleManager.getString("context.delete")))
                .findFirst()
                .ifPresent(deleteItem -> contextMenu.getItems().remove(deleteItem));
    }

    private static void translateDefaultMenuItems(ContextMenu contextMenu) {
        contextMenu.getItems().forEach(it -> {
            if (!(it instanceof SeparatorMenuItem)) {
                try {
                    it.setText(LocaleManager.getString("context." + it.getText().replace(" ", "_").toLowerCase()));
                } catch (Exception ignored) {
                    // Item may be translated already
                }
            }
        });
    }

    private static void modifyTextInTextField(TextInputControl textField, TextFieldModification modification) {
        String inputText = GUString.isNotEmpty(textField.getSelectedText()) ? textField.getSelectedText() : textField.getText();

        String modifiedText = null;
        switch (modification) {
            case UPPERCASE:
                modifiedText = inputText.toUpperCase();
                break;
            case UPPERCASE_FIRST_WORD:
                modifiedText = GUString.capitalize(inputText, true, false);
                break;
            case UPPERCASE_ALL_WORDS:
                modifiedText = GUString.capitalizeFully(inputText, new char[]{' ', ',', ';'});
                break;
            case UPPERCASE_BETWEEN_SEPARATOR:
                modifiedText =  GUString.capitalizeFully(inputText, new char[]{',', ';'});
                break;
            case LOWERCASE:
                modifiedText = inputText.toLowerCase();
                break;
        }

        setTextInTextField(textField, modifiedText);
    }

    private static void executeTextUtilityInTextField(TextInputControl textField, TextFieldUtility utility) {
        String inputText = GUString.isNotEmpty(textField.getSelectedText()) ? textField.getSelectedText() : textField.getText();

        switch (utility) {
            case COPY_ALL:
                FXUtils.copyToClipboard(inputText);
                break;
            case CLEAR_ALL:
                textField.clear();
                break;
        }
    }

    private static void setTextInTextField(TextInputControl textField, String modifiedText) {
        if (GUString.isNotEmpty(textField.getSelectedText())) {
            textField.replaceSelection(modifiedText);
        } else {
            textField.setText(modifiedText);
        }
    }

    private static Line getMFXLineSeparator() {
        Line separator = MFXContextMenu.Builder.getLineSeparator();
        separator.getStyleClass().add("line-separator");
        return separator;
    }

    private static TreeMap<Integer, MenuItem> createMenuItems(TextInputControl textField) {
        TreeMap<Integer, MenuItem> menuItems = new TreeMap<>();
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldModification.UPPERCASE));
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldModification.UPPERCASE_FIRST_WORD));
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldModification.UPPERCASE_ALL_WORDS));
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldModification.UPPERCASE_BETWEEN_SEPARATOR));
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldModification.LOWERCASE));
        menuItems.put(menuItems.size(), new SeparatorMenuItem());
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldUtility.COPY_ALL));
        menuItems.put(menuItems.size(), new CustomMenuItem(textField, TextFieldUtility.CLEAR_ALL));
        menuItems.put(menuItems.size(), new SeparatorMenuItem());
        return menuItems;
    }

    private static TreeMap<Integer, Node> createMFXMenuItems(TextInputControl textField) {
        TreeMap<Integer, Node> menuItems = new TreeMap<>();
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldModification.UPPERCASE));
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldModification.UPPERCASE_FIRST_WORD));
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldModification.UPPERCASE_ALL_WORDS));
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldModification.UPPERCASE_BETWEEN_SEPARATOR));
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldModification.LOWERCASE));
        menuItems.put(menuItems.size(), getMFXLineSeparator());
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldUtility.COPY_ALL, "mfx-content-copy"));
        menuItems.put(menuItems.size(), new CustomMFXMenuItem(textField, TextFieldUtility.CLEAR_ALL, "mfx-delete-alt"));
        menuItems.put(menuItems.size(), getMFXLineSeparator());
        return menuItems;
    }

    private static class CustomMenuItem extends MenuItem {
        public CustomMenuItem(TextInputControl textField, TextFieldModification modification) {
            super(LocaleManager.getString("context." + modification.toString().toLowerCase()));
            setOnAction(event -> modifyTextInTextField(textField, modification));
        }

        public CustomMenuItem(TextInputControl textField, TextFieldUtility utility) {
            super(LocaleManager.getString("context." + utility.toString().toLowerCase()));
            setOnAction(event -> executeTextUtilityInTextField(textField, utility));
        }
    }

    private static class CustomMFXMenuItem extends MFXContextMenuItem {
        public CustomMFXMenuItem(TextInputControl textField, TextFieldModification modification) {
            super(LocaleManager.getString("context." + modification.toString().toLowerCase()));
            setOnAction(event -> modifyTextInTextField(textField, modification));
        }

        public CustomMFXMenuItem(TextInputControl textField, TextFieldUtility utility) {
            super(LocaleManager.getString("context." + utility.toString().toLowerCase()));
            setOnAction(event -> executeTextUtilityInTextField(textField, utility));
        }

        public CustomMFXMenuItem(TextInputControl textField, TextFieldUtility utility, String icon) {
            this(textField, utility);
            setGraphic(new MFXFontIcon(icon, 12));
        }
    }
}
