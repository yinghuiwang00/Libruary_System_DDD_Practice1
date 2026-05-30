package com.library.circulation.domain.model.enums;

public enum HoldStatus {
    WAITING,
    READY_FOR_PICKUP,
    FULFILLED,
    CANCELLED,
    EXPIRED,
    EXPIRED_NOT_PICKED_UP
}
