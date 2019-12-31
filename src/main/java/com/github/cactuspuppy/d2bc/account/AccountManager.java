package com.github.cactuspuppy.d2bc.account;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccountManager {
    protected static ConcurrentMap<UUID, D2BCAccount> mcUIDMap = new ConcurrentHashMap<>();

    protected static ConcurrentHashMap<Long, D2BCAccount> discordIDMap = new ConcurrentHashMap<>();

    public void saveToDisk() {
        //TODO
    }

    public D2BCAccount fetchMCAccount(UUID uuid) {

    }
}
