package com.google.gwt.eclipse.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the settings for a GWT compile.
 */
public class SimpleGWTCompileSettings {

  private List<String> entryPointModules = new ArrayList<String>();

  private String extraArgs = "";

  private String logLevel = "INFO";
  
  private String outputStyle = "OBFUSCATED";

  private String vmArgs = "-Xmx512m";
  
  public SimpleGWTCompileSettings(List<String> entryPointModules) {
    this.entryPointModules = entryPointModules;
  }
  
  public List<String> getEntryPointModules() {
    return entryPointModules;
  }

  public String getExtraArgs() {
    return extraArgs;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public String getOutputStyle() {
    return outputStyle;
  }

  public String getVmArgs() {
    return vmArgs;
  }

  /**
   * Entry point modules for compilation are <strong>not</strong> persisted; we
   * simply default to using the project's defined entry point modules instead.
   */
  public void setEntryPointModules(List<String> entryPointModules) {
    this.entryPointModules = entryPointModules;
  }

  public void setExtraArgs(String args) {
    this.extraArgs = args;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public void setOutputStyle(String outputStyle) {
    this.outputStyle = outputStyle;
  }

  public void setVmArgs(String vmArgs) {
    this.vmArgs = vmArgs;
  }

}
