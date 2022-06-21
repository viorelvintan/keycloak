package org.keycloak.it.storage.database;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.CLITest;
import org.keycloak.it.junit5.extension.WithDatabase;

import static org.junit.jupiter.api.Assertions.assertTrue;

@CLITest
@WithDatabase(alias = "cockroach")
public class CockroachDbTest extends BasicDatabaseTest {
    @Override
    protected void assertWrongUsername(CLIResult cliResult) {
        cliResult.assertMessage("ERROR: FATAL: password authentication failed for user \"wrong\"");
    }

    //no passwords in cockroach insecure mode, just stubbing for now to implement the interface
    @Override
    protected void assertWrongPassword(CLIResult cliResult) {
        assertTrue(true);
    }
}
