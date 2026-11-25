package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/test-outbox-data-h2.sql")
@DataJdbcTest
@TestPropertySource(properties = "spring.config.location=classpath:/")
@Import({BillOutboxRelay.class, JacksonAutoConfiguration.class})
class OperationOutboxRepoTest {
	
	@Autowired
	private OperationOutboxRepo repo;
	
    @Autowired
    private BillOutboxRelay relay;
    
    @MockitoBean
    private BillClient billClient;
	
//	@MockitoBean
//	private TaskScheduler taskScheduler;

	@Test
	void can_get_batch() {
		
		List<OperationOutbox> claimed = repo.claimBatch(Instant.now(), 20);
	    assertThat(claimed)
        .hasSize(2)
        .extracting(OperationOutbox::getId)
        .containsExactly(
            UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
            UUID.fromString("b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        );
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Test
    void shouldHandleSuccessPermanentErrorAndTransientBackoff() {
		
        UUID id1 = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // attempts = 0
        UUID id2 = UUID.fromString("b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"); // attempts = 2
        // 1st item succeeds -> SENT
        doNothing().when(billClient).updateBalance(eq(id1), anyMap());
        // 2nd item fails with transient 500 -> stays PENDING, attempts=3, backoff=40s
        doThrow(new RuntimeException("Service Unavailable"))
            .when(billClient).updateBalance(eq(id2), anyMap());

        Instant beforeRun = Instant.now();
        relay.relayPendingBills();

        OperationOutbox updated1 = repo.findById(id1).orElseThrow();
        assertThat(updated1.getStatus()).isEqualTo(OperationOutbox.Status.SENT);

        OperationOutbox updated2 = repo.findById(id2).orElseThrow();
        assertThat(updated2.getStatus()).isEqualTo(OperationOutbox.Status.PENDING);
        assertThat(updated2.getAttempts()).isEqualTo(3);
        // 10 * 2^(3-1) = 40s backoff
        assertThat(updated2.getNextAttemptAt()).isAfter(beforeRun.plusSeconds(35));
    }
	
}
