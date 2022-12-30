package xyz.atsumeru.manager.listeners;

public interface OnButtonClickListener {
    void onClick(String action, int position, Runnable onClickRunnable);
}
