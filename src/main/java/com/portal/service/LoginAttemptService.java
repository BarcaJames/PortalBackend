package com.portal.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MINUTES;

@Service
public class LoginAttemptService {
    // TODO change this value to something like 3 or 5
    public static final int MAXIMUM_NUMBER_OF_ATTEMPTS = 50;
    public static final int ATTEMPTS_INCREMENT = 1;
    /* String is the username which is the key of the cache and Integer rep the number of time user tried to log in*/
    private LoadingCache<String, Integer> loginAttemptCache;

    public LoginAttemptService() {
        super();
        loginAttemptCache = CacheBuilder.newBuilder()
                .expireAfterWrite(15, MINUTES)
                .maximumSize(100) //Max of 100 entry at any given time
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) throws Exception {
                        return 0; // Initialize the cache value for each key to zero
                    }
                });
    }

    // remove user from cache
    public void evictUserFromLoginAttemptCache(String username){
        loginAttemptCache.invalidate(username);
    }

    public void addUserToLoginAttemptCache(String username){
        int attempts = 0;

        try {
            // Add one to attempt in cache
            attempts = ATTEMPTS_INCREMENT + loginAttemptCache.get(username);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // Added incremented value to cache
        loginAttemptCache.put(username, attempts);

    }

    public boolean hasExceededMaxAttempts(String username){
        try {
            return loginAttemptCache.get(username) >= MAXIMUM_NUMBER_OF_ATTEMPTS;
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }
}
