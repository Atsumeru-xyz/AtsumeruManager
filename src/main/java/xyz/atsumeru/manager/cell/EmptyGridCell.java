package xyz.atsumeru.manager.cell;

import org.controlsfx.control.GridCell;

public class EmptyGridCell<T> extends GridCell<T> {

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(null);
    }
}
