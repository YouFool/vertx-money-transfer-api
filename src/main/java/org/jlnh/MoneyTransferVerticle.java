package org.jlnh;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jlnh.model.Account;
import org.jlnh.model.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jlnh.ActionHelper.ok;

/**
 * Verticle responsible for the money transfer API.
 *
 * @author João Heckmann
 */
public class MoneyTransferVerticle extends AbstractVerticle {

    private JDBCClient jdbcClient;

    private static final Logger LOGGER = LogManager.getLogger(MoneyTransferVerticle.class);


    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

//        router.post("/api/transfer").handler(this::transferMoney);
        router.post("/api/transfer").handler(this::transfer);

        router.get("/api/accounts").handler(this::getAllAccounts);
        router.get("/api/accounts/:id").handler(this::getOneAccount);

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        ConfigRetriever.getConfigAsFuture(configRetriever)
                .compose(config -> {
                    jdbcClient = JDBCClient.createShared(vertx, config, "test");

                    return connect() //
                            .compose(sqlConnection -> { //
                                Future<Void> future = Future.future();
                                createTablesIfNeeded(sqlConnection) //
                                        .setHandler(event -> { //
                                            sqlConnection.close();
                                            future.handle(event.mapEmpty());
                                        });
                                return future;
                            }).compose(v -> createHttpServer(config, router));
                }).setHandler(startFuture);
    }

    /**
     *
     * @param config
     * @param router
     * @return
     */
    private Future<Void> createHttpServer(JsonObject config, Router router) {
        Future<Void> future = Future.future();
        vertx.createHttpServer() //
            .requestHandler(router) //
                .listen(config.getInteger("HTTP_PORT", 8080), res -> future.handle(res.mapEmpty()));
        return future;
    }

    /**
     *
     * @return
     */
    public Future<SQLConnection> connect() {
        Future<SQLConnection> future = Future.future();
        jdbcClient.getConnection(asyncResult -> //
                future.handle(asyncResult.map(connection -> //
                        connection.setOptions(new SQLOptions().setAutoGeneratedKeys(true)))));
        return future;
    }

    /**
     *
     * @param connection
     * @return
     */
    private Future<SQLConnection> createTablesIfNeeded(SQLConnection connection) {
        Future<SQLConnection> future = Future.future();
        vertx.fileSystem().readFile("scripts/V__01_Create.sql", ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
            } else {
                connection.execute(ar.result().toString(),
                        ar2 -> future.handle(ar2.map(connection))
                );
            }
        });
        return future;
    }

    /**
     *
     * @param routingContext
     */
    private void transfer(RoutingContext routingContext) {
        Transaction theTransaction = routingContext.getBodyAsJson().mapTo(Transaction.class);
        LOGGER.error("Transaction incoming: ".concat(theTransaction.toString()));

        connect() //
                .compose(sqlConnection -> this.doTransfer(sqlConnection, theTransaction))
                .setHandler(ActionHelper.handleTransfer(routingContext, theTransaction));
    }

    /**
     *
     * @param sqlConnection
     * @param theTransaction
     * @return
     */
    private Future<Transaction> doTransfer(SQLConnection sqlConnection, Transaction theTransaction) {
        Future<Transaction> transactionFuture = Future.future();

        this.fetchOne(sqlConnection, theTransaction.getFrom().getId().toString(), false)
                .compose(fromAccount -> {
                    Future<Account> accountFuture = Future.future();

                    this.fetchOne(sqlConnection, theTransaction.getTo().getId().toString(), false)
                            .compose(toAccount -> {
                                this.transferMoney(sqlConnection, fromAccount, toAccount, theTransaction.getAmount())
                                        .setHandler(event -> {
                                            if (event.failed()) {
                                                transactionFuture.fail(new IllegalStateException("Could not transfer money!"));
                                            }
                                            transactionFuture.complete();
                                        });
                            return transactionFuture;
                            });
                    return accountFuture;
                });

        return transactionFuture;
    }

    /**
     *
     * @param routingContext
     */
    private void getAllAccounts(RoutingContext routingContext) {
        connect()
                .compose(this::fetchAllAccounts) //
                .setHandler(ok(routingContext));
    }

    /**
     *
     * @param connection
     * @return
     */
    private Future<List<Account>> fetchAllAccounts(SQLConnection connection) {
        Future<List<Account>> future = Future.future();
        connection.query("SELECT * FROM account", result -> {
                    connection.close();
                     future.handle(
                            result.map(rs -> rs.getRows().stream(  ).map(Account::new).collect(Collectors.toList()))
                    );
                }
        );
        return future;
    }

    /**
     *
     * @param routingContext
     */
    private void getOneAccount(RoutingContext routingContext) {
        String id = routingContext.pathParam("id");
        connect() //
                .compose(sqlConnection -> this.fetchOne(sqlConnection, id, true)) //
                .setHandler(ok(routingContext));
    }

    /**
     *
     * @param sqlConnection
     * @param id
     * @return
     */
    private Future<Account> fetchOne(SQLConnection sqlConnection, String id, boolean closeConnection) {
        Future<Account> future = Future.future();
        String sql = "SELECT * FROM account WHERE id = ?";
        sqlConnection.queryWithParams(sql, new JsonArray().add(id), result -> {
            if (closeConnection) {
                sqlConnection.close();
            }
            future.handle(result.map(
                    resultSet -> new Account(resultSet.getRows().get(0)))
            );
        });

        return future;
    }

    /**
     *
     * @param sqlConnection
     * @param sender
     * @param receiver
     * @param amount
     */
    public Future<Transaction> transferMoney(SQLConnection sqlConnection, Account sender, Account receiver, BigDecimal amount) {
        Future<Transaction> future = Future.future();

        LOGGER.error("Transferring: ".concat(amount.toString()));
        LOGGER.error("From: ".concat(sender.toString()));
        LOGGER.error("To: ".concat(receiver.toString()));

        BigDecimal senderBalance = sender.getBalance();
        if (senderBalance.compareTo(amount) < 0) {
            LOGGER.error("Sender: ".concat(sender.toString()).concat(" is bankrupt!"));
            future.fail(new IllegalStateException("Sender: ".concat(sender.toString()).concat(" is bankrupt!")));
        } else {
            JsonArray params = new JsonArray()
                    .add(UUID.randomUUID().toString())
                    .add(sender.getId().toString())
                    .add(receiver.getId().toString())
                    .add(amount.doubleValue());
            sqlConnection.updateWithParams("INSERT INTO transaction VALUES(?, ?, ?, ?)", params, ar -> {
                sender.setBalance(senderBalance.subtract(amount));
                JsonArray params2 = new JsonArray().add(sender.getBalance().doubleValue()).add(sender.getId().toString());
                sqlConnection.updateWithParams("UPDATE account SET balance = ? WHERE id = ?", params2, event ->  {

                    receiver.setBalance(receiver.getBalance().add(amount));
                    JsonArray params3 = new JsonArray().add(receiver.getBalance().doubleValue()).add(receiver.getId().toString());
                    sqlConnection.updateWithParams("UPDATE account SET balance = ? WHERE id = ?", params3, event2 -> {
                        //sqlConnection.close();
                    });
                });
            });
            future.complete();
        }
        return future;
    }

}