package io.confluent.ps.preflight;

import java.util.List;
import java.util.function.Function;

public abstract class Check<T> implements Function<T, List<CheckResult>> {}
