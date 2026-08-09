package com.github.irybov.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
public class AppIT {

    @Test
	void context_loading(ApplicationContext context) {assertThat(context).isNotNull();}
    
}
