package com.github.irybov.operation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.CacheStrategy;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema="bankdemo", name="operations_outbox")
@ToString
@EqualsAndHashCode(of = "id", cacheStrategy = CacheStrategy.NEVER)
public class OperationOutbox implements Persistable<UUID> {
	
    @Id
    @NonNull
    private UUID id;
    @NonNull
    private String payload;
    @NonNull
    private Status status;
    private int attempts;
    @NonNull
    private Instant nextAttemptAt;
    
    @Transient
    private boolean isNew = false;
    public enum Status { PENDING, SENT, FAILED }
    
    @PersistenceCreator
    public OperationOutbox(UUID id, String payload, Status status, int attempts, Instant nextAttemptAt) {
        this.id = id;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
    }
    
    public static OperationOutbox pending(String payload) {
        OperationOutbox item = new OperationOutbox(
                UUID.randomUUID(),
                payload,
                Status.PENDING,
                0,
                Instant.now()
            );
        item.isNew = true;
        return item;
    }
}
