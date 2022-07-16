package io.confluent.ps.preflight;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.List;

public class PreFlightReport {

  List<CheckResult> results = new ArrayList<>();

  public void add(CheckResult result) {
    results.add(result);
  }

  public void print() {
    out.println(results);
  }

  public void addAll(List<CheckResult> results) {
    this.results.addAll(results);
  }
}
