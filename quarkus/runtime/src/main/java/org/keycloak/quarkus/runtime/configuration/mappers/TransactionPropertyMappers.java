package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.OptionCategory;

import java.util.Arrays;

public class TransactionPropertyMappers {

    private TransactionPropertyMappers(){}

    public static PropertyMapper[] getTransactionPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("transaction-xa-enabled")
                        .to("quarkus.datasource.jdbc.transactions")
                        .defaultValue(Boolean.TRUE.toString())
                        .description("Manually override the transaction type. Transaction type XA and the appropriate driver is used by default.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .isBuildTimeProperty(true)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build(),
                builder().from("transaction-jta-enabled")
                        .to("quarkus.datasource.jdbc.transactions")
                        .defaultValue(Boolean.TRUE.toString())
                        .description("Manually override use of JTA. Enabled default.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .isBuildTimeProperty(true)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build(),
        };
    }

  private static String getQuarkusTransactionsValue(String txValue, ConfigSourceInterceptorContext context) {
    boolean isXaEnabled = getBooleanValue("kc.transaction-xa-enabled", context, true);
    boolean isJtaEnabled = getBooleanValue("kc.transaction-jta-enabled", context, true);
    
    if (!isJtaEnabled) {
      return "disabled";
    }
    
    if (isXaEnabled) {
      return "xa";
    }
    
    return "enabled";
  }
  
  private static <T> PropertyMapper.Builder<T> builder() {
    return PropertyMapper.builder(OptionCategory.TRANSACTION);
  }

  private static boolean getBooleanValue(String key, ConfigSourceInterceptorContext context, boolean defaultValue) {
    boolean v = defaultValue;
    ConfigValue cv = context.proceed(key);
    if (cv != null) {
      v = Boolean.parseBoolean(cv.getValue());
    }
    return v;
  }
}
