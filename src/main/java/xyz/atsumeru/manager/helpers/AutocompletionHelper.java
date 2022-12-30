package xyz.atsumeru.manager.helpers;

import com.jfoenix.controls.JFXAutoCompletePopup;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.Pair;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutocompletionHelper {
    private static final Map<String, Pair<ChangeListener<Boolean>, InvalidationListener>> listeners = new HashMap<>();

    public static void bindSingleAutocompletion(TextField textField, Set<String> suggestion) {
        bindAutocompletion(textField, suggestion, null, false);
    }

    public static void bindMultiAutocompletion(TextField textField, Set<String> suggestion) {
        bindAutocompletion(textField, suggestion, ",", false);
    }

    public static void bindMultiAutocompletion(TextField textField, Set<String> suggestion, boolean showOnEmptyField) {
        bindAutocompletion(textField, suggestion, ",", showOnEmptyField);
    }

    public static void removeListeners(TextField textField) {
        Pair<ChangeListener<Boolean>, InvalidationListener> oldListeners = listeners.get(textField.getId());
        if (oldListeners != null && oldListeners.first != null) {
            textField.focusedProperty().removeListener(oldListeners.first);
        }
        if (oldListeners != null && oldListeners.second != null) {
            textField.textProperty().removeListener(oldListeners.second);
        }
    }

    private static void bindAutocompletion(TextField textField, Set<String> suggestion, String delimiter, boolean showAlways) {
        JFXAutoCompletePopup<String> autoCompletePopup = new JFXAutoCompletePopup<>();
        autoCompletePopup.getSuggestions().addAll(suggestion);

        autoCompletePopup.setSelectionHandler(event -> {
            if (GUString.isNotEmpty(delimiter)) {
                textField.setText((textField.getText().contains(delimiter)
                        ? textField.getText().substring(0, textField.getText().lastIndexOf(",") + 1)
                        : "") + event.getObject());
            } else {
                textField.setText(event.getObject());
            }
            textField.positionCaret(textField.getText().length());

            if (showAlways) {
                textField.setText(textField.getText() + ",");
                FXUtils.runLaterDelayed(() -> autoCompletePopup.show(textField), 100);
            }
        });

        Pair<ChangeListener<Boolean>, InvalidationListener> newListeners = new Pair<>();
        if (showAlways) {
            newListeners.first = (o, oldVal, newVal) -> {
                if (newVal) {
                    autoCompletePopup.show(textField);
                }
            };
            textField.focusedProperty().addListener(newListeners.first);
        } else {
            // filtering options
            newListeners.second = observable -> {
                String lastWord;
                if (GUString.isNotEmpty(delimiter)) {
                    List<String> words = GUArray.splitString(textField.getText().toLowerCase(), delimiter);
                    lastWord = !textField.getText().endsWith(delimiter) ? words.get(words.size() - 1).trim() : "";
                } else {
                    lastWord = textField.getText().toLowerCase();
                }

                autoCompletePopup.filter(string -> string.toLowerCase().contains(lastWord));
                if (autoCompletePopup.getFilteredSuggestions().isEmpty() || lastWord.isEmpty()) {
                    autoCompletePopup.hide();
                } else {
                    autoCompletePopup.show(textField);
                }
            };
            textField.textProperty().addListener(newListeners.second);
        }
        listeners.put(textField.getId(), newListeners);
    }
}
