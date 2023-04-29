package xyz.atsumeru.manager.enums;

import java.util.ArrayList;
import java.util.List;

public enum Status {
    UNKNOWN(0),
    ONGOING(1),
    COMPLETE(2),
    SINGLE(3),
    OVA(4),
    ONA(5),
    LICENSED(6),
    EMPTY(7),
    ANNOUNCEMENT(8),
    NOT_RELEASED(9),
    CANCELED(10),
    ON_HOLD(11),
    ANTHOLOGY(12),
    MAGAZINE(13);

    public final int id;

    Status(int id) {
        this.id = id;
    }

    public static List<Status> getReadableStatuses() {
        return new ArrayList<>() {{
            add(UNKNOWN);
            add(ONGOING);
            add(COMPLETE);
            add(SINGLE);
            add(ANTHOLOGY);
            add(MAGAZINE);
            add(LICENSED);
            add(ANNOUNCEMENT);
            add(NOT_RELEASED);
            add(CANCELED);
            add(ON_HOLD);
            add(EMPTY);
        }};
    }
}
