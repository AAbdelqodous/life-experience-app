package com.maintainance.service_center.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account"),
    INVITE_STAFF("invite-staff"),
    RESET_PASSWORD("reset_password")

    ;

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
