package io.confluent.ps.preflight;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PreFlightReport {

  List<CheckResult> results = new ArrayList<>();

  public void add(CheckResult result) {
    results.add(result);
  }

  public void print() {
    out.println("Results:");
    out.println(results.stream().map(CheckResult::toString).collect(Collectors.joining("\n")));
  }

  public void addAll(List<CheckResult> results) {
    this.results.addAll(results);
  }
}
