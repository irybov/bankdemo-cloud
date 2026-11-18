package com.github.irybov.account;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.springframework.web.server.ResponseStatusException;

public class Record5xxPredicate implements Predicate<Throwable> {
	
    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof ResponseStatusException ex) return ex.getStatusCode().is5xxServerError();
        return throwable instanceof IOException || throwable instanceof TimeoutException;
    }
}
