package com.github.irybov.operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import feign.Response;
import feign.codec.ErrorDecoder;

@Component
public class FeignExceptionHandler implements ErrorDecoder {

	@Override
	public Exception decode(String methodKey, Response response) {
		return new ResponseStatusException(HttpStatus.valueOf(response.status()), readMessage(response));
	}

    private String readMessage(Response response) {

        String message = null;
        try(Reader reader = response.body().asReader(Charset.defaultCharset());) {
            message = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }
}
