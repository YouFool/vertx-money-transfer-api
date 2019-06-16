import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
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

@RunWith(VertxUnitRunner.class)
public class MoneyTransferVerticleTest {

    @Rule
    public RunTestOnContext runTestOnContext = new RunTestOnContext();

    private Vertx vertx;
    private int port = 8081;

    private Transaction testTransaction;

    @Before
    public void setUp(TestContext context) {
        this.createTestTransaction();
        vertx = runTestOnContext.vertx();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("HTTP_PORT", port)
                        .put("url", "jdbc:h2:mem:test?shutdown=true")
                        .put("driver_class", "org.h2.Driver")
                );
        vertx.deployVerticle(MoneyTransferVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    private void createTestTransaction() {
        Account account1 = new Account(UUID.randomUUID(), BigDecimal.valueOf(100));
        Account account2 = new Account(UUID.randomUUID(), BigDecimal.valueOf(0));
        testTransaction = new Transaction(null, account1, account2, BigDecimal.valueOf(100));
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void just_run(TestContext context) {
        Async async = context.async();
        final String json = Json.encodePrettily(testTransaction);
        vertx.createHttpClient().post(port, "localhost", "/api/transfer") //
                .putHeader("Content-Type", "application/json") //
                .putHeader("Content-Length", Integer.toString(json.length())) //
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        Transaction transaction = Json.decodeValue(body.toString(), Transaction.class);
                        context.assertEquals(transaction.getAmount(), 100);
                        context.assertEquals(transaction.getFrom(), 0);
                        context.assertNull(transaction.getTo());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", "/api/accounts",
                response ->
                        response.handler(body -> {
                            context.assertTrue(body.toString().contains("10000"));
                            async.complete();
                        }));
    }
}
