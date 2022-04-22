package org.keycloak.connections.jpa.util;


import org.hibernate.HibernateException;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * From https://github.com/cockroachdb/hibernate-savepoint-fix/blob/master/CockroachDBTransactionCoordinatorBuilder.java
 *
 * ---- Comment from original version ---
 * Created by justin on 7/12/17.
 */
public class CockroachDbTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {
    public static final String SHORT_NAME = "jdbc";

    /**
     * Singleton access
     */
    public static final CockroachDbTransactionCoordinatorBuilder INSTANCE = new CockroachDbTransactionCoordinatorBuilder();

    @Override
    public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
        if ( owner instanceof JdbcResourceTransactionAccess) {
            return new CockroachDbTransactionCoordinator( this, owner, (JdbcResourceTransactionAccess) owner );
        }

        throw new HibernateException(
                "Could not determine ResourceLocalTransactionAccess to use in building TransactionCoordinator"
        );
    }

    @Override
    public boolean isJta() {
        return false;
    }

    @Override
    public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
        return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
    }

    @Override
    public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
        return new DdlTransactionIsolatorNonJtaImpl( jdbcContext );
    }
}
