package org.keycloak.models.utils;

import java.sql.SQLException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
//import javax.persistence.EntityTransaction;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * adapted from https://github.com/timveil-cockroach/spring-examples/blob/master/common/src/main/java/io/crdb/spring/common/ExceptionChecker.java
 */
public class RetryUtil {

  private static final Logger logger = Logger.getLogger(RetryUtil.class);

  public static final int MAX_RETRIES = 3;
  
  // this is thrown when CRDB needs the client to retry
  private static final String POSTGRES_SERIALIZATION_FAILURE = "40001";

  // the following codes are often encountered when nodes become unavailable during processing
  private static final String POSTGRES_STATEMENT_COMPLETION_UNKNOWN = "40003";
  private static final String POSTGRES_CONNECTION_DOES_NOT_EXIST = "08003";
  private static final String POSTGRES_CONNECTION_FAILURE = "08006";

  /*
  public static boolean rollbackJpaTransaction(KeycloakSession session) {
    EntityTransaction eTrx = session.getProvider(JpaConnectionProvider.class).getEntityManager().getTransaction();
    if (eTrx.isActive()) {
      try {
        eTrx.rollback();
        return true;
      } catch (Exception e) {
        logger.warn("Error when rolling JPA transaction back.", e);
      }
    }
    return false;
  }
  */
  
  public static boolean shouldRetry(Throwable ex) {
    logger.infof("shouldRetry? %s", ex);

    if (ex == null) {
      return false;
    }

    SQLException sqlException = ExceptionUtils.throwableOfType(ex, SQLException.class);

    if (sqlException == null && ex.getSuppressed() != null) {
      for (Throwable t : ex.getSuppressed()) {
        logger.infof("suppressed %s", t.getClass().getName()); 
        sqlException = ExceptionUtils.throwableOfType(t, SQLException.class);
        if (sqlException != null) break;
      }
    }
    
    if (sqlException == null && ex.getCause() != null) {
      for (Throwable t : ex.getCause().getSuppressed()) {
        logger.infof("cause suppressed %s", t.getClass().getName()); 
        sqlException = ExceptionUtils.throwableOfType(t, SQLException.class);
        if (sqlException != null) break;
      }
    }

    if (sqlException != null) {
      StringWriter o = new StringWriter();
      sqlException.printStackTrace(new PrintWriter(o));
      logger.infof("sqlException %s", o.toString());
    }
    if (sqlException != null) {
      return shouldRetry(sqlException);
    }

    logger.warnf("Exception is not a SQLException.  Will not be retried.  Class is %s.", ex.getClass());

    return false;
  }

  private static boolean shouldRetry(SQLException ex) {
    String sqlState = ex.getSQLState();
    int errorCode = ex.getErrorCode();

    if (errorCode != 0) {
      return false;
    }

    if (sqlState == null) {
      return false;
    }

    boolean retryable = isRetryableState(sqlState);

    logger.infof("SQLException is retryable? %b : sql state [%s], error code [%d], message [%s]", retryable, sqlState, errorCode, ex.getMessage());

    return retryable;
  }

  private static boolean isRetryableState(String sqlState) {
    // ------------------
    // POSTGRES: https://www.postgresql.org/docs/current/errcodes-appendix.html
    // ------------------

    return POSTGRES_SERIALIZATION_FAILURE.equals(sqlState)
        || POSTGRES_STATEMENT_COMPLETION_UNKNOWN.equals(sqlState)
        || POSTGRES_CONNECTION_FAILURE.equals(sqlState)
        || POSTGRES_CONNECTION_DOES_NOT_EXIST.equals(sqlState);
  }
}
