package io.fluo.api.config;

import java.util.concurrent.TimeUnit;

public interface TransactionConfiguration {
  public static final String TRANSACTION_PREFIX = "io.fluo.tx";
  public static final String ROLLBACK_TIME_PROP = TRANSACTION_PREFIX + ".rollbackTime";

  public void setRollbackTime(long time, TimeUnit tu);
}
