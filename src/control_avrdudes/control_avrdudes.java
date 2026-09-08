package control_avrdudes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class control_avrdudes {

    private static final int PROCESS_TIMEOUT_SECONDS = 30;
    private static final String AVRDUDE_CMD = getAvrdudeCommand();

    private static String getAvrdudeCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "avrdude.exe";
        }
        return "avrdude";
    }

    public String cargar(String hex, String gHex, String mc, String pg, String[] fuses, String bitclock, String action) {
        if (action.equals("Read fuses")) {
            return readFuses(pg, mc, bitclock);
        }

        List<String> cmd = buildCommand(hex, gHex, mc, pg, fuses, bitclock, action);
        if (cmd == null) {
            return "Acción no reconocida: " + action;
        }

        String prefix = getActionDescription(hex, gHex, mc, action);
        return prefix + "\n" + executeCommand(cmd);
    }

    private String getActionDescription(String hex, String gHex, String mc, String action) {
        switch (action) {
            case "Write .hex": return "Escribiendo flash desde " + hex;
            case "Read .hex": return "Guardando flash en " + gHex;
            case "Verificar .hex": return "Verificando " + hex + " con flash de " + mc;
            case "Write fuses": return "Escribiendo configuración de fuses:";
            case "Prueba conexión": return "Prueba de conexión:";
            case "Lista mc": return "Lista de Microcontroladores:";
            case "Lista programadores": return "Lista de Programadores:";
            default: return action + ":";
        }
    }

    private List<String> buildCommand(String hex, String gHex, String mc, String pg, String[] fuses, String bitclock, String action) {
        switch (action) {
            case "Write .hex":
                return withBitclock(commandList("-c", pg, "-P", "usb", "-p", mc, "-U", "flash:w:" + hex + ":i"), bitclock);
            case "Read .hex":
                return withBitclock(commandList("-c", pg, "-P", "usb", "-p", mc, "-U", "flash:r:" + gHex + ":i"), bitclock);
            case "Verificar .hex":
                return withBitclock(commandList("-c", pg, "-P", "usb", "-p", mc, "-U", "flash:v:" + hex + ":i"), bitclock);
            case "Write fuses": {
                String[] cf = convertFuses(fuses);
                return withBitclock(commandList("-c", pg, "-P", "usb", "-p", mc,
                        "-U", "lfuse:w:" + cf[0] + ":m",
                        "-U", "hfuse:w:" + cf[1] + ":m",
                        "-U", "efuse:w:" + cf[2] + ":m"), bitclock);
            }
            case "Prueba conexión":
                // Leer la firma es la verificación real de la cadena USB-ISP-microcontrolador
                return withBitclock(commandList("-c", pg, "-P", "usb", "-p", mc, "-U", "signature:r:-:h"), bitclock);
            case "Lista mc":
                return commandList("-p", "?");
            case "Lista programadores":
                return commandList("-c", "?");
            default:
                return null;
        }
    }

    // -B fija el bitclock y evita que avrdude envíe el ajuste automático de SCK al programador
    private List<String> withBitclock(List<String> cmd, String bitclock) {
        if (bitclock != null && !bitclock.isEmpty()) {
            cmd.add("-B");
            cmd.add(bitclock);
        }
        return cmd;
    }

    private List<String> commandList(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(AVRDUDE_CMD);
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    private String readFuses(String pg, String mc, String bitclock) {
        StringBuilder result = new StringBuilder("Leyendo fuses:\n");
        String[] labels = {"Low fuse", "High fuse", "Extended fuse"};
        String[] fuseArgs = {"lfuse", "hfuse", "efuse"};

        for (int i = 0; i < 3; i++) {
            List<String> cmd = withBitclock(commandList("-c", pg, "-P", "usb", "-p", mc,
                    "-U", fuseArgs[i] + ":r:-:h"), bitclock);
            result.append(labels[i]).append(": ");
            result.append(executeCommand(cmd));
            result.append("\n");
        }

        return result.toString();
    }

    private String[] convertFuses(String[] fuses) {
        String[] result = new String[fuses.length];
        for (int i = 0; i < fuses.length; i++) {
            if (fuses[i] != null && fuses[i].startsWith("0b")) {
                try {
                    String binaryPart = fuses[i].substring(2);
                    int decimal = Integer.parseInt(binaryPart, 2);
                    result[i] = "0x" + Integer.toHexString(decimal).toUpperCase();
                } catch (NumberFormatException e) {
                    result[i] = fuses[i];
                }
            } else {
                result[i] = fuses[i];
            }
        }
        return result;
    }

    public static List<String> parseAvrdudeList(String output, String type) {
        List<String> ids = new ArrayList<>();
        if (output == null || output.isEmpty()) {
            return ids;
        }

        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("=")) {
                String[] parts = trimmed.split("=");
                if (parts.length >= 1) {
                    String id = parts[0].trim();
                    if (!id.isEmpty() && !id.startsWith("avrdude") && !id.startsWith("valid")) {
                        if (type.equals("mc") && id.matches("[a-zA-Z]\\w*")) {
                            ids.add(id);
                        } else if (type.equals("pg") && id.matches("[a-zA-Z]\\w*")) {
                            ids.add(id);
                        }
                    }
                }
            }
        }
        return ids;
    }

    private String executeCommand(List<String> cmd) {
        StringBuilder output = new StringBuilder();
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);

            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                pb.environment().put("PATH", pb.environment().getOrDefault("PATH", "") + ";C:\\Program Files\\avrdude;C:\\avrdude");
            } else if (os.contains("mac")) {
                pb.environment().put("PATH", pb.environment().getOrDefault("PATH", "") + ":/opt/homebrew/bin:/usr/local/bin");
            } else {
                pb.environment().put("PATH", pb.environment().getOrDefault("PATH", "") + ":/usr/local/bin:/usr/bin");
            }

            p = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = p.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                output.append("\nERROR: El comando excedió el tiempo límite de ")
                      .append(PROCESS_TIMEOUT_SECONDS).append(" segundos.");
            }

            int exitCode = finished ? p.exitValue() : -1;
            if (exitCode != 0 && output.length() == 0) {
                if (isAvrdudeNotFound(output.toString())) {
                    return "ERROR: avrdude no encontrado. Asegúrese de que esté instalado y en el PATH del sistema.\n"
                         + "- macOS: brew install avrdude\n"
                         + "- Linux: sudo apt install avrdude\n"
                         + "- Windows: Descargue desde https://avrdudes.github.io/avrdude/";
                }
                output.append("avrdude finalizó con código de salida: ").append(exitCode);
            }

        } catch (IOException e) {
            if (!isAvrdudeInPath()) {
                return "ERROR: avrdude no encontrado. Asegúrese de que esté instalado y en el PATH del sistema.\n"
                     + "- macOS: brew install avrdude\n"
                     + "- Linux: sudo apt install avrdude\n"
                     + "- Windows: Descargue desde https://avrdudes.github.io/avrdude/";
            }
            return "Error de ejecución: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Proceso interrumpido.";
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
        return output.toString();
    }

    private static boolean isAvrdudeInPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> dirs = new ArrayList<>();

        String path = System.getenv("PATH");
        if (path != null && !path.isEmpty()) {
            dirs.addAll(Arrays.asList(path.split(File.pathSeparator)));
        }
        if (os.contains("win")) {
            dirs.add("C:\\Program Files\\avrdude");
            dirs.add("C:\\avrdude");
        } else if (os.contains("mac")) {
            dirs.add("/opt/homebrew/bin");
            dirs.add("/usr/local/bin");
        } else {
            dirs.add("/usr/local/bin");
            dirs.add("/usr/bin");
        }

        for (String dir : dirs) {
            if (dir == null || dir.isEmpty()) continue;
            File candidate = new File(dir, AVRDUDE_CMD);
            if (candidate.isFile() && candidate.canExecute()) {
                return true;
            }
        }
        return false;
    }

    private boolean isAvrdudeNotFound(String output) {
        return output.toLowerCase().contains("no se encontr") || output.toLowerCase().contains("not found")
                || output.toLowerCase().contains("cannot find");
    }
}