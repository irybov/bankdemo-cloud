package com.github.irybov.admin;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletResponse;

//@ControllerAdvice
public class CsrfCookieAdvice {
//    @ModelAttribute
    public void loadCsrfToken(CsrfToken token, HttpServletResponse response) {
        if(token != null){response.setHeader(token.getHeaderName(), token.getToken());}
    }
}
