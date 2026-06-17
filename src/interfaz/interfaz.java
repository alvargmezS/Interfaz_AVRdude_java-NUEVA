package interfaz;

import GUI.frame;

import javax.swing.*;
import java.awt.*;

public class interfaz {

    private static final String FLATLAF_LIGHT = "com.formdev.flatlaf.FlatLightLaf";
    private static final String FLATLAF_DARK  = "com.formdev.flatlaf.FlatDarkLaf";

    public static void main(String[] args) {
        checkAndElevate();

        configurePlatformProperties();

        String laf = tryLookAndFeel(FLATLAF_LIGHT);
        if (laf == null) {
            laf = tryLookAndFeel(FLATLAF_DARK);
        }
        if (laf == null) {
            applySystemLookAndFeel();
        }

        setUIFonts();

        SwingUtilities.invokeLater(() -> {
            frame f = new frame();
            f.setSize(960, 680);
            f.setMinimumSize(new Dimension(780, 520));
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
        });
    }

    private static void checkAndElevate() {
        if (Elevator.isRunningAsAdmin()) {
            return;
        }

        int option = JOptionPane.showConfirmDialog(
                null,
                "AVRdude Interface necesita permisos de administrador para acceder\n"
                        + "al programador USB y ejecutar avrdude.\n\n"
                        + "¿Desea elevar los privilegios ahora?",
                "Permisos de Administrador",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (option != JOptionPane.YES_OPTION) {
            System.exit(0);
        }

        try {
            boolean elevated = Elevator.elevateAndRestart();
            if (elevated) {
                System.exit(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "No se pudieron obtener permisos de administrador:\n"
                            + e.getMessage() + "\n\n"
                            + Elevator.getElevationInstructions(),
                    "Error de Elevacion",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        System.exit(0);
    }

    private static void configurePlatformProperties() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "AVRdude");
            System.setProperty("apple.awt.antialiasing", "on");
            System.setProperty("apple.awt.textantialiasing", "on");
        }
        System.setProperty("swing.aatext", "true");
    }

    private static String tryLookAndFeel(String className) {
        try {
            Class.forName(className);
            UIManager.setLookAndFeel(className);
            return className;
        } catch (Exception e) {
            return null;
        }
    }

    private static void applySystemLookAndFeel() {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                UIManager.setLookAndFeel("com.apple.laf.AquaLookAndFeel");
            } else if (os.contains("win")) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } else {
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                } catch (Exception e) {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                }
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored2) {
            }
        }
    }

    private static void setUIFonts() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String fontName;
        if (os.contains("mac")) {
            fontName = "SF Pro Text";
        } else if (os.contains("win")) {
            fontName = "Segoe UI";
        } else {
            fontName = "SansSerif";
        }

        Font uiFont = new Font(fontName, Font.PLAIN, 13);
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource(uiFont));
            }
        }
    }
}