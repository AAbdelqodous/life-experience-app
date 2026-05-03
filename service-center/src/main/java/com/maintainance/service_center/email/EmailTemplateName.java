package com.maintainance.service_center.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account"),
    INVITE_STAFF("invite-staff")

    ;

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
