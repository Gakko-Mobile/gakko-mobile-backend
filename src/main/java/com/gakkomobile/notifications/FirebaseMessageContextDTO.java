package com.gakkomobile.notifications;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
class FirebaseMessageContextDTO {

    String category;
    String targetScreen;

    public FirebaseMessageContextDTO(String category) {
        this.category = category;
    }
}
