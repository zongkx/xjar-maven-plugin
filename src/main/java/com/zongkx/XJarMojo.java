package com.zongkx;

import io.xjar.XCryptos;
import io.xjar.XEncryption;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE)
public class XJarMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * 加密密码
     */
    @Parameter(property = "io.xjar.xjar.password", required = true)
    private String password;

    /**
     * 需要加密的类的包名匹配规则（如：com.example.**.class）
     */
    @Parameter(property = "io.xjar.xjar.includes", defaultValue = "**/*.class")
    private String[] includes;

    /**
     * 排除不需要加密的类
     */
    @Parameter(property = "io.xjar.xjar.excludes")
    private String[] excludes;
    /**
     * 本地 Go 可执行文件的绝对路径 (例如: C:\Program Files\Go\bin\go.exe)
     * 如果不指定，插件将尝试直接从系统环境变量中调用 "go"
     */
    @Parameter(property = "xjar.goPath")
    private String goPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // 1. 获取打包生成的原始 JAR 文件
        File artifactFile = project.getArtifact().getFile();
        if (artifactFile == null || !artifactFile.exists()) {
            getLog().info("未找到生成的 JAR 包，跳过 XJar 加固。");
            return;
        }
        String sourcePath = artifactFile.getAbsolutePath();
        String targetPath = sourcePath.endsWith(".jar")
                ? sourcePath.substring(0, sourcePath.length() - 4) + ".x.jar"
                : sourcePath + ".x.jar";
        getLog().info("正在对 JAR 包进行 XJar 加固: " + sourcePath);

        try {
            XEncryption xEncryption = XCryptos.encryption()
                    .from(sourcePath)  // 加密的源文件
                    .use(password);

            for (String include : includes) {
                xEncryption.include(include);
            }
            for (String ex : excludes) {
                xEncryption.exclude(ex);
            }
            xEncryption.to(targetPath);
            getLog().info("XJar 加固成功！生成加密包: " + targetPath);
            File jarDir = new File(targetPath).getParentFile();
            File goFile = new File(jarDir, "xjar.go");

            if (goFile.exists()) {
                getLog().info("检测到解密 Go 源码，正在启动本地 Go 环境进行编译...");
                compileGoLauncher(jarDir, goFile);
            } else {
                getLog().warn("未找到 xjar.go 文件，跳过自动编译阶段。");
            }
        } catch (Exception e) {
            getLog().error("XJar 加固失败: " + e.getMessage(), e);
            throw new MojoExecutionException("XJar 加固过程中发生错误", e);
        }
    }
    /**
     * 调用本地 Go 编译器执行 go build
     */
    private void compileGoLauncher(File workDir, File goFile) throws Exception {
        String goExecutable;

        // 1. 判断用户是否显式配置了 goPath
        if (goPath != null && !goPath.trim().isEmpty()) {
            File customGo = new File(goPath);
            if (!customGo.exists()) {
                throw new MojoExecutionException("配置的 xjar.goPath 路径不存在: " + goPath);
            }
            goExecutable = customGo.getAbsolutePath();
            getLog().info("使用用户自定义的 Go 路径: " + goExecutable);
        } else {
            // 2. 如果未配置，则尝试依赖系统环境变量中的 "go"
            // 在 Windows 环境下，底层执行时会自动去 PATH 变量里寻找 go.exe
            goExecutable = isWindows() ? "go.exe" : "go";
            getLog().info("未检测到 xjar.goPath 配置，将尝试使用系统环境变量 (PATH) 中的默认 '" + goExecutable + "' 命令...");
        }

        // 3. 构建编译命令: go build xjar.go
        ProcessBuilder pb = new ProcessBuilder(goExecutable, "build", goFile.getName());
        pb.directory(workDir); // 设置命令执行的工作目录（即 target 目录）
        pb.redirectErrorStream(true); // 合并标准错误流和标准输出流

        getLog().info("正在执行编译命令: " + String.join(" ", pb.command()));

        try {
            Process process = pb.start();

            // 实时读取控制台输出，防止缓冲区满导致进程假死
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().info("[Go Build] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                getLog().info("Go 启动器编译成功！已在 target 目录下生成可执行文件。");
                // 顺手清理掉临时的 xjar.go 源码，保持目录整洁
                if (goFile.delete()) {
                    getLog().info("已清理临时 Go 源码文件 (xjar.go)。");
                }
            } else {
                throw new MojoExecutionException("Go 编译失败，退出码: " + exitCode + "。请确认配置或系统环境。");
            }

        } catch (java.io.IOException e) {
            // 如果系统 PATH 里找不到 go 命令，此处会抛出 IOException
            if (goPath == null || goPath.trim().isEmpty()) {
                throw new MojoExecutionException("无法唤起系统的 '" + goExecutable + "' 命令。请确保本地已安装 Go 语言环境，且已将 Go 的 bin 目录配入系统环境变量 (PATH) 中；或者在插件 <configuration> 中显式配置 <goPath>。", e);
            } else {
                throw e;
            }
        }
    }

    /**
     * 辅助方法：判断当前操作系统是否为 Windows
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}