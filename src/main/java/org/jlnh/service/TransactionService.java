package org.jlnh.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jlnh.model.Account;

import java.math.BigDecimal;

/**
 * Service responsible to manage transactions between accounts.
 */
public class TransactionService {

    private static final Logger LOGGER = LogManager.getLogger(TransactionService.class);

    public void transferMoney(Account sender, Account receiver, BigDecimal amount) { //TODO return a future
        // TODO query the DB to see if the users exist

        LOGGER.error("Transferring: ".concat(amount.toString()));
        LOGGER.error("From: ".concat(sender.toString()));
        LOGGER.error("To: ".concat(receiver.toString()));

        BigDecimal senderBalance = sender.getBalance();
        if (senderBalance.compareTo(amount) < 0) {
            LOGGER.error("Bankrupt!!");
            //throw new Exception("Bankrupt!!");
        } else {
            //TODO transaction
            receiver.setBalance(receiver.getBalance().add(amount));
            sender.setBalance(sender.getBalance().subtract(amount));
            LOGGER.error("Success!!");
            LOGGER.error(sender.toString());
            LOGGER.error(receiver.toString());
        }

    }
}
