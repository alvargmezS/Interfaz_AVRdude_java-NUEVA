package interfaz;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public final class Elevator {

    private Elevator() {}

    public static boolean isRunningAsAdmin() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return isWindowsAdmin();
        } else {
            return isUnixRoot();
        }
    }

    public static boolean elevateAndRestart() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return elevateWindows();
        } else if (os.contains("mac")) {
            return elevateMacOS();
        } else {
            return elevateLinux();
        }
    }

    public static String getElevationInstructions() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "Clic derecho en el .jar -> Ejecutar como administrador.";
        } else if (os.contains("mac")) {
            return "Introduzca su contrasena de macOS cuando se solicite.";
        } else {
            return "Instale pkexec (polkit) o ejecute: sudo java -jar Interfaz_AVRdude.jar";
        }
    }

    private static boolean isUnixRoot() {
        try {
            Process p = new ProcessBuilder("id", "-u").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null) {
                    int uid = Integer.parseInt(line.trim());
                    p.waitFor();
                    return uid == 0;
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean isWindowsAdmin() {
        try {
            Process p = new ProcessBuilder("net", "session").start();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean elevateMacOS() throws Exception {
        JPasswordField pwdField = new JPasswordField(20);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Introduzca la contrasena de administrador:"), BorderLayout.NORTH);
        panel.add(pwdField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(null, panel,
                "Permisos de Administrador",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option != JOptionPane.OK_OPTION) {
            return false;
        }

        String password = new String(pwdField.getPassword());
        if (password.isEmpty()) {
            return false;
        }

        List<String> cmd = buildJavaCommand();

        List<String> sudoCmd = new ArrayList<>();
        sudoCmd.add("sudo");
        sudoCmd.add("-S");
        sudoCmd.add("-k");
        sudoCmd.add("--preserve-env=PATH,HOME,LANG");
        sudoCmd.addAll(cmd);

        ProcessBuilder pb = new ProcessBuilder(sudoCmd);
        pb.environment().put("HOME", System.getProperty("user.home"));
        pb.environment().put("LANG", System.getenv().getOrDefault("LANG", "en_US.UTF-8"));
        String userPath = System.getenv().getOrDefault("PATH", "");
        if (!userPath.isEmpty()) {
            pb.environment().put("PATH", userPath + ":/opt/homebrew/bin:/usr/local/bin");
        } else {
            pb.environment().put("PATH", "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin");
        }
        pb.redirectErrorStream(true);

        Process p = pb.start();

        OutputStream os = p.getOutputStream();
        os.write((password + "\n").getBytes());
        os.flush();
        os.close();

        StringBuilder output = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(p.getInputStream());
        try (BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (line.toLowerCase().contains("incorrect password") ||
                    line.toLowerCase().contains("sorry") ||
                    line.toLowerCase().contains("authentication failure")) {
                    p.destroyForcibly();
                    throw new IOException("Contrasena incorrecta.");
                }
            }
        }

        // El EOF indica que la instancia elevada ya terminó: si falló, mostrar el error
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            int tail = Math.max(0, output.length() - 1500);
            throw new IOException("La instancia elevada fallio (codigo de salida " + exitCode + "):\n"
                    + output.substring(tail));
        }

        return true;
    }

    private static boolean elevateWindows() throws Exception {
        String javaExe = getJavaExecutable();
        String jarPath = getJarPath();
        String classPath = System.getProperty("java.class.path");

        StringBuilder cmdLine = new StringBuilder();
        if (jarPath != null) {
            cmdLine.append("-jar \"").append(jarPath).append("\"");
        } else {
            cmdLine.append("-cp \"").append(classPath).append("\" interfaz.interfaz");
        }

        File vbs = File.createTempFile("avrElevate_", ".vbs");
        vbs.deleteOnExit();

        try (PrintWriter w = new PrintWriter(new FileWriter(vbs))) {
            w.println("Set objShell = CreateObject(\"Shell.Application\")");
            w.println("objShell.ShellExecute \"" + escapeVbs(javaExe) + "\", \""
                    + escapeVbs(cmdLine.toString()) + "\", \"\", \"runas\", 1");
        }

        Process p = new ProcessBuilder("cscript", "//nologo", vbs.getAbsolutePath())
                .inheritIO().start();
        p.waitFor();
        return true;
    }

    private static boolean elevateLinux() throws Exception {
        List<String> cmd = buildJavaCommand();

        if (hasCommand("pkexec")) {
            List<String> fullCmd = new ArrayList<>();
            fullCmd.add("pkexec");
            fullCmd.add("env");
            addEnvAssignment(fullCmd, "HOME", System.getProperty("user.home"));
            addEnvAssignment(fullCmd, "DISPLAY", System.getenv("DISPLAY"));
            addEnvAssignment(fullCmd, "XAUTHORITY", getXAuthorityPath());
            addEnvAssignment(fullCmd, "WAYLAND_DISPLAY", System.getenv("WAYLAND_DISPLAY"));
            addEnvAssignment(fullCmd, "XDG_RUNTIME_DIR", System.getenv("XDG_RUNTIME_DIR"));
            fullCmd.addAll(cmd);

            ProcessBuilder pb = new ProcessBuilder(fullCmd);
            pb.inheritIO();
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) return true;
            throw new IOException("Elevacion con pkexec cancelada o fallida.");
        }

        if (hasCommand("gksudo")) {
            List<String> fullCmd = new ArrayList<>();
            fullCmd.add("gksudo");
            fullCmd.addAll(cmd);
            Process p = new ProcessBuilder(fullCmd).inheritIO().start();
            p.waitFor();
            return true;
        }

        if (hasCommand("kdesudo")) {
            List<String> fullCmd = new ArrayList<>();
            fullCmd.add("kdesudo");
            fullCmd.addAll(cmd);
            Process p = new ProcessBuilder(fullCmd).inheritIO().start();
            p.waitFor();
            return true;
        }

        JPasswordField pwdField = new JPasswordField(20);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Introduzca la contrasena de root (sudo):"), BorderLayout.NORTH);
        panel.add(pwdField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(null, panel,
                "Permisos de Administrador",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return false;

        String password = new String(pwdField.getPassword());
        if (password.isEmpty()) return false;

        List<String> sudoCmd = new ArrayList<>();
        sudoCmd.add("sudo");
        sudoCmd.add("-S");
        sudoCmd.add("-k");
        sudoCmd.add("--preserve-env=PATH,HOME,DISPLAY,XAUTHORITY,WAYLAND_DISPLAY,XDG_RUNTIME_DIR");
        sudoCmd.addAll(cmd);

        ProcessBuilder pb = new ProcessBuilder(sudoCmd);
        pb.environment().put("HOME", System.getProperty("user.home"));
        pb.environment().put("DISPLAY", System.getenv().getOrDefault("DISPLAY", ""));
        String xauth = getXAuthorityPath();
        if (xauth != null) {
            pb.environment().put("XAUTHORITY", xauth);
        }
        pb.environment().put("WAYLAND_DISPLAY", System.getenv().getOrDefault("WAYLAND_DISPLAY", ""));
        pb.environment().put("XDG_RUNTIME_DIR", System.getenv().getOrDefault("XDG_RUNTIME_DIR", ""));
        String userPath = System.getenv().getOrDefault("PATH", "");
        if (!userPath.isEmpty()) {
            pb.environment().put("PATH", userPath + ":/usr/local/bin:/usr/bin:/bin");
        } else {
            pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin");
        }
        pb.redirectErrorStream(true);

        Process p = pb.start();
        OutputStream os = p.getOutputStream();
        os.write((password + "\n").getBytes());
        os.flush();
        os.close();
        p.getInputStream().close();

        return true;
    }

    private static List<String> buildJavaCommand() {
        List<String> cmd = new ArrayList<>();
        String javaExe = getJavaExecutable();
        cmd.add(javaExe);

        String jarPath = getJarPath();
        if (jarPath != null) {
            cmd.add("-jar");
            cmd.add(jarPath);
        } else {
            String classPath = System.getProperty("java.class.path");
            cmd.add("-cp");
            cmd.add(classPath);
            cmd.add("interfaz.interfaz");
        }
        return cmd;
    }

    private static String getJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (javaHome != null) {
            String exe = javaHome + File.separator + "bin" + File.separator + "java";
            if (os.contains("win")) exe += ".exe";
            return exe;
        }
        return "java";
    }

    private static String getJarPath() {
        try {
            String path = interfaz.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            if (path != null && path.endsWith(".jar")) {
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    path = path.replaceFirst("^/", "");
                }
                return path;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String escapeVbs(String s) {
        return s.replace("\"", "\"\"");
    }

    private static boolean hasCommand(String command) {
        try {
            Process p = new ProcessBuilder("which", command).start();
            p.getInputStream().close();
            p.getErrorStream().close();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void addEnvAssignment(List<String> cmd, String name, String value) {
        if (value != null && !value.isEmpty()) {
            cmd.add(name + "=" + value);
        }
    }

    private static String getXAuthorityPath() {
        String xauth = System.getenv("XAUTHORITY");
        if (xauth != null && !xauth.isEmpty() && new File(xauth).canRead()) {
            return xauth;
        }
        File fallback = new File(System.getProperty("user.home"), ".Xauthority");
        if (fallback.isFile() && fallback.canRead()) {
            return fallback.getAbsolutePath();
        }
        return null;
    }
}