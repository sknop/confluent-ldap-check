package io.confluent.ps.preflight;

public class CheckResult {

  final String title;
  final boolean passed;
  final Throwable e;
  final String reason;

  public CheckResult(String title) {
    this.title = title;
    this.passed = true;
    this.e = null;
    this.reason = null;
  }

  public CheckResult(String title, Throwable e) {
    this.title = title;
    this.passed = false;
    this.e = e;
    this.reason = e.getMessage();
  }

  public CheckResult(String title, String reason) {
    this.title = title;
    this.passed = false;
    this.e = null;
    this.reason = reason;
  }

  @Override
  public String toString() {
    if (passed) return "CheckResult{title=\""+title+"\",passed=" + true + "}"; else return (
      "CheckResult{title=\""+title+"\",passed=" +
      false +
      ", reason=" +
      reason +
      ", exception=" +
      (e != null ? e.getMessage() : "") +
      "}"
    );
  }
}
