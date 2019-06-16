package org.jlnh.model;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a given transaction made between two accounts.
 *
 * @author João Heckmann
 */
public class Transaction {

    private UUID id;
    private Account from;
    private Account to;
    private BigDecimal amount;

    public Transaction() {
    }

    public Transaction(UUID id, Account from, Account to, BigDecimal amount) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public Transaction(JsonObject payload) {
        this(
                (UUID) payload.getValue("ID", UUID.class), //
                (Account) payload.getValue("FROM", Account.class), //
                (Account) payload.getValue("TO", Account.class), //
                BigDecimal.valueOf(payload.getDouble("AMOUNT"))
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Account getFrom() {
        return from;
    }

    public void setFrom(Account from) {
        this.from = from;
    }

    public Account getTo() {
        return to;
    }

    public void setTo(Account to) {
        this.to = to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", from=" + from +
                ", to=" + to +
                ", amount=" + amount +
                '}';
    }
}
