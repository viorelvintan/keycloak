package org.keycloak.exceptions;

/**
 * Exception thrown for cases when CockroachDB returns a retryable error ( error code 40001 or 'retry transaction' string at the start of the error message).
 * @author harture
 */
public class RetryableTransactionException extends RuntimeException{

    public RetryableTransactionException(Throwable t){
        super(t);
    }

}
