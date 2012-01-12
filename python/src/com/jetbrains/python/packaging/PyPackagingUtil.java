package com.jetbrains.python.packaging;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.python.PythonHelpersLocator;
import com.jetbrains.python.sdk.SdkUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * @author vlan
 */
public class PyPackagingUtil {
  public final static int OK = 0;
  public final static int ERROR_WRONG_USAGE = 1;
  public final static int ERROR_NO_PACKAGING_TOOLS = 2;
  public final static int ERROR_INVALID_SDK = -1;
  public final static int ERROR_TIMEOUT = -2;
  public final static int ERROR_INVALID_OUTPUT = -3;

  private static final String PACKAGING_TOOL = "packaging_tool.py";
  private static final String VIRTUALENV = "virtualenv.py";
  private static final int TIMEOUT = 30000;
  private static final Logger LOG = Logger.getInstance("#" + PyPackagingUtil.class.getName());
  private static PyPackagingUtil ourInstance = null;

  private Map<Sdk, List<PyPackage>> myPackagesCache = new HashMap<Sdk, List<PyPackage>>();

  private PyPackagingUtil() {
  }

  @NotNull
  public static String createVirtualEnv(@NotNull Sdk sdk, @NotNull String desinationDir) throws PyExternalProcessException {
    final String virtualenv = PythonHelpersLocator.getHelperPath(VIRTUALENV);
    runPythonHelper(sdk, list(virtualenv, "--never-download", "--distribute", desinationDir));
    // TODO: Return the path to the virtualenv interpreter
    return desinationDir;
  }

  public static void deleteVirtualEnv(@NotNull String virtualEnvDir) {
    FileUtil.delete(new File(virtualEnvDir));
  }

  @NotNull
  public static PyPackagingUtil getInstance() {
    if (ourInstance == null) {
      ourInstance = new PyPackagingUtil();
    }
    return ourInstance;
  }

  @NotNull
  public List<PyPackage> getInstalledPackages(@NotNull Sdk sdk) throws PyExternalProcessException {
    final List<PyPackage> cached = myPackagesCache.get(sdk);
    if (cached != null) {
      return cached;
    }
    final String packagingTool = PythonHelpersLocator.getHelperPath(PACKAGING_TOOL);
    final String output = runPythonHelper(sdk, list(packagingTool, "list"));
    final List<PyPackage> packages = parsePackagingToolOutput(output);
    myPackagesCache.put(sdk, packages);
    return packages;
  }

  public void clearCaches() {
    myPackagesCache.clear();
  }

  private static <T> List<T> list(T... xs) {
    return Arrays.asList(xs);
  }

  @NotNull
  private static String runPythonHelper(@NotNull Sdk sdk, @NotNull List<String> cmd) throws PyExternalProcessException {
    final String homePath = sdk.getHomePath();
    if (homePath == null) {
      throw new PyExternalProcessException(ERROR_INVALID_SDK, "Cannot find interpreter for SDK");
    }
    final String parentDir = new File(homePath).getParent();
    final List<String> cmdline = new ArrayList<String>();
    cmdline.add(homePath);
    cmdline.addAll(cmd);
    ProcessOutput output = SdkUtil.getProcessOutput(parentDir, cmdline.toArray(new String[cmdline.size()]), null, TIMEOUT);
    final int retcode = output.getExitCode();
    if (output.isTimeout()) {
      throw new PyExternalProcessException(ERROR_TIMEOUT, "Timed out");
    }
    else if (retcode != 0) {
      final String stdout = output.getStdout();
      final String stderr = output.getStderr();
      String message = stderr;
      if (message.trim().isEmpty()) {
        message = stdout;
      }
      LOG.debug(String.format("Error when running '%s'\nSTDOUT: %s\nSTDERR: %s\n\n",
                              StringUtil.join(cmdline, " "),
                              stdout,
                              stderr));
      throw new PyExternalProcessException(retcode, message);
    }
    return output.getStdout();
  }

  @NotNull
  private static List<PyPackage> parsePackagingToolOutput(@NotNull String s) throws PyExternalProcessException {
    final String[] lines = StringUtil.splitByLines(s);
    final List<PyPackage> packages = new ArrayList<PyPackage>();
    for (String line : lines) {
      final List<String> fields = StringUtil.split(line, "\t");
      if (fields.size() < 3) {
        throw new PyExternalProcessException(ERROR_WRONG_USAGE, String.format("Invalid output format of '%s'", PACKAGING_TOOL));
      }
      final String name = fields.get(0);
      final String version = fields.get(1);
      final String location = fields.get(2);
      packages.add(new PyPackage(name, version, location, new ArrayList<PyRequirement>()));
    }
    return packages;
  }
}
