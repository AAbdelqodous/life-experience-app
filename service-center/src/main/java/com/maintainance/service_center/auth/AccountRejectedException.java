package com.maintainance.service_center.auth;

public class AccountRejectedException extends RuntimeException {
    public AccountRejectedException() {
        super("Account rejected");
    }
}
