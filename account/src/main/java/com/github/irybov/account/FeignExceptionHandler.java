package com.github.irybov.account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import feign.Response;
import feign.codec.ErrorDecoder;

@Component
public class FeignExceptionHandler implements ErrorDecoder {
	
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
    	
        HttpStatus status = HttpStatus.resolve(response.status());
        String message = readMessage(response);

        if (status != null && status.is5xxServerError()) {
            // Throw ResponseStatusException (or a custom ExternalServiceException) for 5xx
            return new ResponseStatusException(status, message);
        }
        if (status != null && status.is4xxClientError()) {
            // Handle 4xx separately so CircuitBreaker can ignore it
            return new ResponseStatusException(status, message);
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String readMessage(Response response) {
    	
        if (response.body() == null) return "Upstream service error (" + response.status() + ")";
        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
            String body = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
            return body.isBlank() ? "Upstream service error (" + response.status() + ")" : body;
        }
        catch (IOException e) {return "Failed to read error body: " + e.getMessage();}
    }
/*
	@Override
	public Exception decode(String methodKey, Response response) {
		return new ResponseStatusException(HttpStatus.valueOf(response.status()), readMessage(response));
	}

    private String readMessage(Response response) {
        return Optional.ofNullable(response.body())
                .map(body -> {
                    try (Reader reader = body.asReader(Charset.defaultCharset())) {
                        return new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
                    }
                    catch (IOException e) {return e.getMessage();}
                })
                .orElse("");
    }
*/    
}
