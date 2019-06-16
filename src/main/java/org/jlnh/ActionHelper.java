package org.jlnh;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jlnh.model.Transaction;

import java.util.NoSuchElementException;

/**
 * Helper code to handle async results.
 */
public class ActionHelper {

    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
    private static final String CONTENT_TYPE = "content-type";

    private ActionHelper() {
    }

    /**
     * Returns a handler writing the received {@link AsyncResult} to the routing context.
     *
     * @param context the routing context
     * @return the handler
     */
    static <T> Handler<AsyncResult<T>> ok(RoutingContext context) {
        return ar -> {
            if (ar.failed()) {
                if (ar.cause() instanceof NoSuchElementException) {
                    context.response() //
                            .setStatusCode(404) //
                            .end(ar.cause().getMessage());
                } else {
                    context.fail(ar.cause());
                }
            } else {
                context.response().setStatusCode(200) //
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8) //
                        .end(Json.encodePrettily(ar.result()));
            }
        };
    }

    /**
     * Returns a handler to the transfer async result.
     *
     * @param context the routing context
     * @param transaction the transaction
     * @return the handler
     */
    static Handler<AsyncResult<Transaction>> handleTransfer(RoutingContext context, Transaction transaction) {
        return ar -> {
            if (ar.failed()) {
                JsonObject failureJson = new JsonObject() //
                        .put("error", ar.cause().getMessage()); //
                if (ar.cause() instanceof IllegalStateException) {
                    failureJson.put("cause", "User does not have sufficient funds")
                            .put("code", 400);
                    context.response() //
                            .setStatusCode(400) //
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8) //
                            .end(Json.encodePrettily(failureJson));
                } else {
                    context.fail(ar.cause());
                }
            } else {
                transaction.setTo(null); // Who transferred the money does not need to know his friend's balance
                Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                context.response() //
                        .setStatusCode(201) //
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8) //
                        .end(Json.encodePrettily(transaction)); //TODO should encode ar.result()
            }
        };
    }
}
