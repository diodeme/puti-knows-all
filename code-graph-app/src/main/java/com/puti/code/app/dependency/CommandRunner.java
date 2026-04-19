package com.puti.code.app.dependency;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 进程执行封装，用于运行 Maven/Gradle 等外部命令
 */
@Slf4j
public class CommandRunner {

    private final long timeoutMinutes;

    public CommandRunner(long timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public int run(String[] command, String workDir) {
        return run(command, workDir, Collections.emptyMap());
    }

    /**
     * 在指定工作目录执行命令，支持自定义环境变量（如 JAVA_HOME）
     *
     * @param command 命令及参数
     * @param workDir 工作目录
     * @param envVars 额外的环境变量
     * @return 进程退出码，-1 表示执行失败或超时
     */
    public int run(String[] command, String workDir, Map<String, String> envVars) {
        log.info("Running command: {} in {}", String.join(" ", command), workDir);

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(new File(workDir))
                .redirectErrorStream(true);

        if (envVars != null && !envVars.isEmpty()) {
            pb.environment().putAll(envVars);
            if (log.isDebugEnabled()) {
                envVars.forEach((k, v) -> log.debug("  env: {}={}", k, k.equals("JAVA_HOME") ? v : "***"));
            }
        }

        try {
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("Command timed out after {} minutes: {}", timeoutMinutes, String.join(" ", command));
                return -1;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Command failed (exit {}):\n{}", exitCode, output);
            } else {
                log.info("Command completed successfully");
                if (log.isDebugEnabled()) {
                    log.debug("Command output:\n{}", output);
                }
            }
            return exitCode;

        } catch (IOException | InterruptedException e) {
            log.error("Command execution error: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }
    }
}
