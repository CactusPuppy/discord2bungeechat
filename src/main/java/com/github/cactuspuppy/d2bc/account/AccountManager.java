package com.github.cactuspuppy.d2bc.account;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccountManager {
    protected static ConcurrentMap<UUID, D2BCAccount> accounts = new ConcurrentHashMap<>();
}
