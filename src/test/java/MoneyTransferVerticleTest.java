import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jlnh.MoneyTransferVerticle;
import org.jlnh.model.Account;
import org.jlnh.model.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tests for the Money Transfer verticle.
 *
 * @author João Heckmann
 */
@RunWith(VertxUnitRunner.class)
public class MoneyTransferVerticleTest {

    @Rule
    public RunTestOnContext runTestOnContext = new RunTestOnContext();

    private Vertx vertx;
    private int port = 8080;

    private Transaction sampleTransaction;

    @Before
    public void setUp(TestContext context) {
        this.initializeSampleTransaction();
        vertx = runTestOnContext.vertx();
        vertx.deployVerticle(MoneyTransferVerticle.class.getName(), context.asyncAssertSuccess());
    }

    private void initializeSampleTransaction() {
        Account account1 = new Account(UUID.fromString("f4e05ee5-12eb-4ae0-92c7-2cb4b6cd8ce2"), BigDecimal.valueOf(9.99));
        Account account2 = new Account(UUID.fromString("123e4567-e89b-12d3-a456-556642440000"), BigDecimal.valueOf(0));
        this.sampleTransaction = new Transaction(null, account1, account2, BigDecimal.valueOf(0.01));
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void should_get_all_accounts(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/api/accounts",
                response ->
                        response.handler(body -> {
                            context.assertTrue(body.toString().contains("e6908ec0-1b70-4982-9362-8e9bdabbbd97"));
                            context.assertTrue(body.toString().contains("f4e05ee5-12eb-4ae0-92c7-2cb4b6cd8ce2"));
                            context.assertTrue(body.toString().contains("123e4567-e89b-12d3-a456-556642440000"));
                            async.complete();
                        }));
    }

    @Test
    public void should_get_only_one_account(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/api/accounts/f4e05ee5-12eb-4ae0-92c7-2cb4b6cd8ce2",
                response ->
                        response.handler(body -> {
                            context.assertTrue(body.toString().contains("f4e05ee5-12eb-4ae0-92c7-2cb4b6cd8ce2"));
                            context.assertTrue(body.toString().contains("9.99"));
                            async.complete();
                        }));
    }

    @Test
    public void should_transfer_money(TestContext context) {
        Async async = context.async();

        Transaction transaction = sampleTransaction;
        final String json = Json.encodePrettily(transaction);

        vertx.createHttpClient().post(port, "localhost", "/api/transfer") //
                .putHeader("Content-Type", "application/json") //
                .putHeader("Content-Length", Integer.toString(json.length())) //
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        Transaction responseTransaction = Json.decodeValue(body.toString(), Transaction.class);
                        context.assertEquals(responseTransaction.getAmount(), BigDecimal.valueOf(0.01));
                        context.assertEquals(responseTransaction.getFrom().getBalance(), BigDecimal.valueOf(9.98));
                        context.assertNull(responseTransaction.getTo());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }

    @Test
    public void should_not_transfer_money_not_enough_funds(TestContext context) {
        Async async = context.async();

        Transaction transaction = sampleTransaction;
        transaction.setAmount(BigDecimal.valueOf(99999));

        final String json = Json.encodePrettily(transaction);
        vertx.createHttpClient().post(port, "localhost", "/api/transfer") //
                .putHeader("Content-Type", "application/json") //
                .putHeader("Content-Length", Integer.toString(json.length())) //
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 400);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertTrue(body.toString().contains("User does not have sufficient funds"));
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }
}
