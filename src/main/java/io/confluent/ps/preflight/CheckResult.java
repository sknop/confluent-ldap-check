package io.confluent.ps.preflight;

public class CheckResult {

  final boolean passed;
  final Throwable e;
  final String reason;

  public CheckResult() {
    this.passed = true;
    this.e = null;
    this.reason = null;
  }

  public CheckResult(Throwable e) {
    this.passed = false;
    this.e = e;
    this.reason = e.getMessage();
  }

  public CheckResult(String reason) {
    this.passed = false;
    this.e = null;
    this.reason = reason;
  }

  @Override
  public String toString() {
    if (passed) return "CheckResult{passed=" + true + "}"; else return (
      "CheckResult{passed=" +
      false +
      "reason=" +
      reason +
      ",exception=" +
      (e != null ? e.getMessage() : "") +
      "}"
    );
  }
}
