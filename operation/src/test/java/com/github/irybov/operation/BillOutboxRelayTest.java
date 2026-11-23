package com.github.irybov.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.HttpClientErrorException;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BillOutboxRelayTest {

    @Mock
    private OperationOutboxRepo outboxRepository;

    @Mock
    private BillClient billClient;

    @Mock
    private TransactionOperations txOps;

    private BillOutboxRelay relay;

    @BeforeEach
    void setUp() {
        // Make txOps.execute(...) simply run the lambda directly without a real DB transaction
        lenient().when(txOps.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        // Pass claimed items straight through saveAll(...) as the real repository does
        lenient().when(outboxRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        relay = new BillOutboxRelay(outboxRepository, billClient, new ObjectMapper(), txOps);
    }

    @Test
    void shouldMarkSentWhenBillClientSucceeds() {
        UUID id = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        OperationOutbox item = new OperationOutbox(
            id, "{\"1001\": 500.00}", OperationOutbox.Status.PENDING, 0, Instant.now().minusSeconds(60)
        );

        when(outboxRepository.claimBatch(any(Instant.class), eq(20))).thenReturn(List.of(item));

        relay.relayPendingBills();

        verify(billClient).updateBalance(id, Map.of(1001, 500.00));
        assertThat(item.getStatus()).isEqualTo(OperationOutbox.Status.SENT);
        verify(outboxRepository).save(item);
    }

    @Test
    void shouldMarkFailedOnNonRetryable4xxError() {
        UUID id = UUID.fromString("e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55");
        OperationOutbox item = new OperationOutbox(
            id, "{\"999999\": -50.00}", OperationOutbox.Status.PENDING, 0, Instant.now().minusSeconds(60)
        );

        when(outboxRepository.claimBatch(any(Instant.class), eq(20))).thenReturn(List.of(item));
        doThrow(HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        )).when(billClient).updateBalance(eq(id), anyMap());

        relay.relayPendingBills();

        assertThat(item.getStatus()).isEqualTo(OperationOutbox.Status.FAILED);
        assertThat(item.getAttempts()).isZero(); // does not increment retry counter
        verify(outboxRepository).save(item);
    }

    @Test
    void shouldKeepPendingAndApplyExponentialBackoffOnTransientFailure() {
        UUID id = UUID.fromString("b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22");
        OperationOutbox item = new OperationOutbox(
            id, "{\"1001\": -1250.50, \"1002\": 1250.50}", OperationOutbox.Status.PENDING, 2, Instant.now().minusSeconds(10)
        );

        when(outboxRepository.claimBatch(any(Instant.class), eq(20))).thenReturn(List.of(item));
        doThrow(new RuntimeException("Connection timeout"))
            .when(billClient).updateBalance(eq(id), anyMap());

        Instant before = Instant.now();
        relay.relayPendingBills();

        assertThat(item.getStatus()).isEqualTo(OperationOutbox.Status.PENDING);
        assertThat(item.getAttempts()).isEqualTo(3);
        // attempts=3 -> 10 * 2^(3-1) = 40 seconds backoff
        assertThat(item.getNextAttemptAt())
            .isAfterOrEqualTo(before.plusSeconds(39))
            .isBeforeOrEqualTo(before.plusSeconds(45));
        verify(outboxRepository).save(item);
    }

    @Test
    void shouldDoNothingWhenNoPendingItemsClaimed() {
        when(outboxRepository.claimBatch(any(Instant.class), eq(20))).thenReturn(List.of());

        relay.relayPendingBills();

        verifyNoInteractions(billClient);
        verify(outboxRepository, never()).save(any());
    }

}
