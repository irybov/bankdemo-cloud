package com.github.irybov.operation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.querydsl.sql.SQLQueryFactory;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class BillOutboxRelay {

    private static final TypeReference<Map<Integer, Double>> MAP_TYPE = new TypeReference<>() {};

    private final OperationOutboxRepo outboxRepository;
    private final BillClient billClient;
    private final ObjectMapper objectMapper;
    private final TransactionOperations txOps;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
//    @Transactional
    public void relayPendingBills() {
    	
        List<OperationOutbox> batch = txOps.execute(status -> {
            Instant now = Instant.now();
            List<OperationOutbox> claimed = outboxRepository.claimBatch(now, 20);
            Instant leaseUntil = now.plusSeconds(30); // Must exceed max @Retryable timeout (5s)
            claimed.forEach(item -> item.setNextAttemptAt(leaseUntil));
            return outboxRepository.saveAll(claimed);
        });
        
        if (batch == null || batch.isEmpty()) return;
        // NO TX: Send all 20 HTTP calls concurrently on virtual threads
//        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
//            for (OperationOutbox item : batch) {
//                executor.submit(() -> processItem(item));
//            }
//        } // Blocks virtual scheduler thread until all 20 tasks finish, then starts 1s fixedDelay
//        // TX 2: Persist all 20 updated statuses in a single batch save (~2ms DB connection hold)
//        outboxRepository.saveAll(batch);
//    }
        for (OperationOutbox item : batch) {
            try {
                Map<Integer, Double> payload = objectMapper.readValue(item.getPayload(), MAP_TYPE);
                billClient.updateBalance(item.getId(), payload);
                item.setStatus(OperationOutbox.Status.SENT);
            }
            catch (NoSuchElementException | HttpClientErrorException.BadRequest | HttpClientErrorException.UnprocessableContent ex) {
                // Non-retryable 4xx business error -> mark FAILED so it doesn't poison the queue
                item.setStatus(OperationOutbox.Status.FAILED);
            }
            catch (Exception ex) {
                // Transient outage -> keep PENDING and back off in DB (10s, 20s, 40s... up to 5 min)
                int nextAttempt = item.getAttempts() + 1;
                long backoffSeconds = Math.min(300, (long) (10 * Math.pow(2, nextAttempt - 1)));
                item.setAttempts(nextAttempt);
                item.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
            }
            outboxRepository.save(item);
        }
    }
}
