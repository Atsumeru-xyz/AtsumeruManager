package xyz.atsumeru.manager.listeners;

public interface OnDialogInputListener {
    void onConfirmInput(String input, boolean overrideFields);
    void onConfirmInput(String input, String secondInput, boolean overrideFields);
}
