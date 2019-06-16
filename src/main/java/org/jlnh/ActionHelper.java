package org.jlnh;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.jlnh.model.Transaction;

import java.util.NoSuchElementException;

/**
 * Some helper code.
 */
public class ActionHelper {

    /**
     * Returns a handler writing the received {@link AsyncResult} to the routing context and setting the HTTP status to
     * the given status.
     * @param context the routing context
     * @param status the status
     * @return the handler
     */
    private static <T> Handler<AsyncResult<T>> writeJsonResponse(RoutingContext context, int status) {
        return ar -> {
            if (ar.failed()) {
                if (ar.cause() instanceof NoSuchElementException) {
                    context.response().setStatusCode(404).end(ar.cause().getMessage());
                } else {
                    context.fail(ar.cause());
                }
            } else {
                context.response().setStatusCode(status)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(ar.result()));
            }
        };
    }

    public static <T> Handler<AsyncResult<T>> ok(RoutingContext rc) {
        return writeJsonResponse(rc,200);
    }

    public static <T> Handler<AsyncResult<T>> created(RoutingContext rc) {
        return writeJsonResponse(rc,201);
    }

    public static Handler<AsyncResult<Void>> noContent(RoutingContext rc) {
        return ar -> {
            if (ar.failed()) {
                if (ar.cause() instanceof NoSuchElementException) {
                    rc.response().setStatusCode(404).end(ar.cause().getMessage());
                } else {
                    rc.fail(ar.cause());
                }
            } else {
                rc.response().setStatusCode(204).end();
            }
        };
    }

    public static Handler<AsyncResult<Transaction>> handleTransfer(RoutingContext routingContext, Transaction transaction) {
        return ar -> {
            if (ar.failed()) {
                if (ar.cause() instanceof IllegalStateException) {
                    routingContext.response().setStatusCode(404).end("Could not transfer: user does not have sufficient funds");
                } else {
                    routingContext.fail(ar.cause());
                }
            }
            transaction.setTo(null); // who transferred the money does not need to know his friend's balance
            routingContext.response().setStatusCode(201).end(Json.encodePrettily(transaction));
        };
    }
}
