package com.github.irybov.operation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface OperationOutboxRepo extends ListCrudRepository<OperationOutbox, UUID> {

    @Query("""
	        SELECT * FROM bankdemo.operations_outbox
	        WHERE status = 'PENDING' AND next_attempt_at <= :now
	        ORDER BY next_attempt_at
	        LIMIT :batchSize
	        -- FOR UPDATE SKIP LOCKED
	        FOR UPDATE
	        """)
    List<OperationOutbox> claimBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
