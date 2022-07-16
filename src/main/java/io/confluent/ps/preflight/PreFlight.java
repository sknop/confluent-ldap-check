package io.confluent.ps.preflight;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.kafka.common.config.AbstractConfig;

public interface PreFlight<C extends AbstractConfig, T> {
  C loadConfig(Path configPath) throws IOException;
  T build(C config);
  PreFlightReport run(T component, List<Check<T>> checkList);
}
