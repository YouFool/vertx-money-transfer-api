package org.jlnh.model;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Bank account.
 *
 * @author João Heckmann
 */
public class Account {

    private UUID id;
    private BigDecimal balance;

    public Account() {
    }

    public Account(UUID id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public Account(JsonObject payload) {
        this(
                UUID.fromString(payload.getString("ID")),
                BigDecimal.valueOf(payload.getDouble("BALANCE"))
        );
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", balance=" + balance +
                '}';
    }
}
