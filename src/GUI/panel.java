package GUI;

import control_avrdudes.control_avrdudes;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class panel extends JPanel {

    private static final Color BG_APP          = new Color(245, 247, 250);
    private static final Color BG_CONSOLE      = new Color(30, 30, 30);
    private static final Color FG_CONSOLE      = new Color(200, 230, 201);
    private static final Color FG_CONSOLE_DIM  = new Color(120, 144, 156);
    private static final Color COLOR_ERROR     = new Color(239, 83, 80);
    private static final Color COLOR_WARNING   = new Color(255, 202, 40);
    private static final Color COLOR_INFO      = new Color(66, 165, 245);
    private static final Color COLOR_SUCCESS   = new Color(102, 187, 106);
    private static final Color COLOR_FUSE      = new Color(0, 229, 255);
    private static final Color COLOR_HEADER    = new Color(100, 181, 246);
    private static final Color COLOR_PROGRESS    = new Color(97, 97, 97);
    private static final Color COLOR_TIMESTAMP  = new Color(120, 144, 156);
    private static final Color ACCENT          = new Color(25, 118, 210);
    private static final Color ACCENT_HOVER    = new Color(21, 101, 192);
    private static final Color DANGER          = new Color(211, 47, 47);
    private static final Color DANGER_HOVER   = new Color(198, 40, 40);
    private static final Color SUCCESS_BTN    = new Color(46, 125, 50);
    private static final Color SUCCESS_HOVER  = new Color(38, 107, 42);
    private static final Color BORDER          = new Color(218, 220, 224);
    private static final Color STATUS_BG      = new Color(238, 240, 244);
    private static final Color TOOLBAR_BG      = new Color(255, 255, 255);
    private static final Color LABEL_SEC       = new Color(97, 97, 97);
    private static final Color LABEL_PRI       = new Color(33, 33, 33);

    private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private String hex = "";
    private String gHex = "";
    private String mc = "m16";
    private String pg = "usbasp";
    private String lfuse = "0x";
    private String hfuse = "0x";
    private String efuse = "0x";

    private JTextPane consolePane;
    private StyledDocument consoleDoc;
    private JLabel statusDot;
    private JLabel statusText;
    private javax.swing.Timer statusTimer;
    private long commandStartTime;

    private JComboBox<String> selectL;
    private JComboBox<String> selectH;
    private JComboBox<String> selectE;
    private JComboBox<String> comboMc;
    private JComboBox<String> comboPg;
    private JLabel lblHex;
    private JLabel lblMc;
    private JLabel lblPg;
    private JLabel lblLfuse;
    private JLabel lblHfuse;
    private JLabel lblEfuse;
    private JMenuBar menuBar;
    private JButton btnListaMc;
    private JButton btnListaPg;

    private AttributeSet styleNormal;
    private AttributeSet styleError;
    private AttributeSet styleWarning;
    private AttributeSet styleInfo;
    private AttributeSet styleSuccess;
    private AttributeSet styleFuse;
    private AttributeSet styleHeader;
    private AttributeSet styleProgress;
    private AttributeSet styleTimestamp;
    private MutableAttributeSet styleBold;

    public panel() {
        setBackground(BG_APP);
        initComponents();
        initConsoleStyles();
        layoutComponents();
    }

    private void initComponents() {
        String monoFont = getMonospacedFont();

        consolePane = new JTextPane();
        consoleDoc = consolePane.getStyledDocument();
        consolePane.setEditable(false);
        consolePane.setFont(new Font(monoFont, Font.PLAIN, 13));
        consolePane.setBackground(BG_CONSOLE);
        consolePane.setCaretColor(FG_CONSOLE);
        consolePane.setSelectionColor(new Color(70, 130, 180));
        consolePane.setSelectedTextColor(Color.WHITE);
        consolePane.setMargin(new Insets(10, 12, 10, 12));
        ConsoleUtils.disableFlicker(consolePane);

        selectL = createFuseCombo();
        selectH = createFuseCombo();
        selectE = createFuseCombo();

        comboMc = new JComboBox<>();
        comboMc.setEditable(true);
        comboMc.addItem("m16");
        comboMc.addItem("m2560");
        comboMc.addItem("m328p");
        comboMc.setToolTipText("Microcontrolador (escriba o seleccione)");
        comboMc.setPreferredSize(new Dimension(130, 30));
        comboMc.addActionListener(e -> {
            mc = Objects.toString(comboMc.getSelectedItem(), "m16");
            lblMc.setText(mc);
        });

        comboPg = new JComboBox<>();
        comboPg.setEditable(true);
        comboPg.addItem("usbasp");
        comboPg.addItem("arduino");
        comboPg.setToolTipText("Programador (escriba o seleccione)");
        comboPg.setPreferredSize(new Dimension(130, 30));
        comboPg.addActionListener(e -> {
            pg = Objects.toString(comboPg.getSelectedItem(), "usbasp");
            lblPg.setText(pg);
        });

        lblHex = new JLabel("Ningún archivo");
        lblMc = new JLabel(mc);
        lblPg = new JLabel(pg);
        lblLfuse = new JLabel("lfuse: " + lfuse);
        lblHfuse = new JLabel("hfuse: " + hfuse);
        lblEfuse = new JLabel("efuse: " + efuse);

        createMenuBar();
    }

    private void initConsoleStyles() {
        MutableAttributeSet base = new SimpleAttributeSet();
        StyleConstants.setFontFamily(base, getMonospacedFont());
        StyleConstants.setFontSize(base, 13);

        styleNormal = copyWithColor(base, FG_CONSOLE);
        styleError = copyWithColor(base, COLOR_ERROR);
        styleWarning = copyWithColor(base, COLOR_WARNING);
        styleInfo = copyWithColor(base, COLOR_INFO);
        styleSuccess = copyWithColor(base, COLOR_SUCCESS);
        styleFuse = copyWithColor(base, COLOR_FUSE);
        styleHeader = copyWithColor(base, COLOR_HEADER);
        styleProgress = copyWithColor(base, COLOR_PROGRESS);
        styleTimestamp = copyWithColor(base, COLOR_TIMESTAMP);

        styleBold = new SimpleAttributeSet(copyWithColor(base, FG_CONSOLE));
        StyleConstants.setBold(styleBold, true);
    }

    private static AttributeSet copyWithColor(AttributeSet base, Color color) {
        MutableAttributeSet s = new SimpleAttributeSet(base);
        StyleConstants.setForeground(s, color);
        return s;
    }

    private String getMonospacedFont() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "Menlo";
        if (os.contains("win")) return "Consolas";
        return "Monospace";
    }

    private JComboBox<String> createFuseCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setEditable(true);
        combo.addItem("0x");
        combo.addItem("0b");
        combo.setPreferredSize(new Dimension(150, 30));
        combo.addActionListener(e -> updateFuseLabels());
        return combo;
    }

    private void updateFuseLabels() {
        lfuse = Objects.toString(selectL.getSelectedItem(), "0x");
        hfuse = Objects.toString(selectH.getSelectedItem(), "0x");
        efuse = Objects.toString(selectE.getSelectedItem(), "0x");
        lblLfuse.setText("lfuse: " + lfuse);
        lblHfuse.setText("hfuse: " + hfuse);
        lblEfuse.setText("efuse: " + efuse);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        add(createToolBar(), BorderLayout.NORTH);
        add(createFusePanel(), BorderLayout.WEST);
        add(createConsoleArea(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createConsoleArea() {
        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(BG_APP);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(50, 50, 50));
        titleBar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JLabel titleLabel = new JLabel("Consola");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setForeground(new Color(180, 180, 180));
        titleBar.add(titleLabel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        statusPanel.setOpaque(false);

        statusDot = new JLabel("\u25CF");
        statusDot.setFont(statusDot.getFont().deriveFont(Font.PLAIN, 13f));
        statusDot.setForeground(COLOR_PROGRESS);

        statusText = new JLabel("Listo");
        statusText.setFont(statusText.getFont().deriveFont(Font.PLAIN, 11f));
        statusText.setForeground(new Color(150, 150, 150));

        JButton btnClear = new JButton("\u2715");
        btnClear.setFont(btnClear.getFont().deriveFont(Font.PLAIN, 11f));
        btnClear.setForeground(new Color(150, 150, 150));
        btnClear.setBackground(new Color(60, 60, 60));
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        btnClear.setOpaque(true);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        btnClear.addActionListener(e -> {
            consolePane.setText("");
            setStatusReady();
        });

        statusPanel.add(statusDot);
        statusPanel.add(statusText);
        statusPanel.add(btnClear);
        titleBar.add(statusPanel, BorderLayout.EAST);

        container.add(titleBar, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(consolePane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        consolePane.setBorder(BorderFactory.createEmptyBorder());
        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }

    private void setStatusReady() {
        statusDot.setForeground(COLOR_PROGRESS);
        statusText.setText("Listo");
        if (statusTimer != null) statusTimer.stop();
    }

    private void setStatusRunning(String command) {
        statusDot.setForeground(COLOR_WARNING);
        commandStartTime = System.currentTimeMillis();
        statusText.setText("Ejecutando: " + command + "... (0.0s)");

        if (statusTimer != null) statusTimer.stop();
        statusTimer = new javax.swing.Timer(100, e -> {
            double elapsed = (System.currentTimeMillis() - commandStartTime) / 1000.0;
            statusText.setText("Ejecutando: " + command + " (" + String.format("%.1f", elapsed) + "s)");
        });
        statusTimer.start();
    }

    private void setStatusSuccess(String command) {
        if (statusTimer != null) statusTimer.stop();
        double elapsed = (System.currentTimeMillis() - commandStartTime) / 1000.0;
        statusDot.setForeground(COLOR_SUCCESS);
        statusText.setText("\u2713 " + command + " — " + String.format("%.1f", elapsed) + "s");
    }

    private void setStatusError(String command) {
        if (statusTimer != null) statusTimer.stop();
        double elapsed = (System.currentTimeMillis() - commandStartTime) / 1000.0;
        statusDot.setForeground(COLOR_ERROR);
        statusText.setText("\u2717 " + command + " — Error (" + String.format("%.1f", elapsed) + "s)");
    }

    private void clearConsole() {
        consolePane.setText("");
    }

    private void appendToConsole(String text, AttributeSet style) {
        try {
            consoleDoc.insertString(consoleDoc.getLength(), text, style);
        } catch (BadLocationException e) {
            // ignore
        }
        consolePane.setCaretPosition(consoleDoc.getLength());
    }

    private void appendTimestamp() {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        appendToConsole("[" + time + "] ", styleTimestamp);
    }

    private void appendHeader(String command, String mcVal, String pgVal, String hexVal) {
        String line = "\u2550".repeat(55) + "\n";
        appendToConsole(line, styleHeader);

        StringBuilder header = new StringBuilder();
        header.append("  ").append(command);
        if (!mcVal.isEmpty()) header.append("  |  MC: ").append(mcVal);
        if (!pgVal.isEmpty()) header.append("  |  PG: ").append(pgVal);
        if (!hexVal.isEmpty() && !command.contains("Lista") && !command.contains("Prueba"))
            header.append("  |  File: ").append(hexVal);
        header.append("\n");

        appendToConsole(header.toString(), styleBold);
        appendToConsole(line, styleHeader);
    }

    private void appendStyledOutput(String rawOutput) {
        String[] lines = rawOutput.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.matches("^[#= .]+$")) {
                appendToConsole(trimmed + "\n", styleProgress);
                continue;
            }

            String lower = trimmed.toLowerCase();

            if (lower.contains("error") || lower.contains("cannot find")
                    || lower.contains("not found") || lower.contains("failed")
                    || lower.contains("no such file") || lower.startsWith("error")) {
                appendToConsole(trimmed + "\n", styleError);
                continue;
            }

            if (lower.contains("warning") || lower.contains("safemode")) {
                appendToConsole(trimmed + "\n", styleWarning);
                continue;
            }

            if (lower.contains("lfuse") || lower.contains("hfuse") || lower.contains("efuse")
                    || lower.contains("extended fuse") || lower.contains("low fuse") || lower.contains("high fuse")) {
                appendToConsole(trimmed + "\n", styleFuse);
                continue;
            }

            if (lower.contains("verification successful") || lower.contains("bytes written")
                    || lower.contains("bytes read") || lower.contains("programmed")) {
                appendToConsole(trimmed + "\n", styleSuccess);
                continue;
            }

            appendToConsole(line + "\n", styleNormal);
        }
    }

    private void appendListAsTable(String rawOutput, String type) {
        String[] lines = rawOutput.split("\n");
        java.util.List<String[]> entries = new java.util.ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.toLowerCase().startsWith("avrdude"))
                continue;

            if (trimmed.toLowerCase().contains("not found") || trimmed.toLowerCase().contains("cannot find")
                    || trimmed.toLowerCase().contains("error"))
                continue;

            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                String id = trimmed.substring(0, eqIdx).trim();
                String desc = trimmed.substring(eqIdx + 1).trim();
                if (!id.isEmpty()) {
                    entries.add(new String[]{id, desc});
                }
            }
        }

        if (entries.isEmpty()) {
            appendStyledOutput(rawOutput);
            return;
        }

        String title = type.equals("mc") ? "Microcontroladores" : "Programadores";
        appendToConsole("  \u250C" + "\u2500".repeat(50) + "\u2510\n", styleInfo);
        appendToConsole("  \u2502 " + String.format("%-4s  %-43s\u2502", "ID", title) + "\n", styleBold);
        appendToConsole("  \u251C" + "\u2500".repeat(50) + "\u2524\n", styleInfo);

        int count = 0;
        for (String[] entry : entries) {
            String id = entry[0];
            String desc = entry[1];
            if (desc.length() > 41) desc = desc.substring(0, 38) + "...";
            appendToConsole("  \u2502 " + String.format("%-6s%-43s\u2502", id, desc) + "\n", styleNormal);
            count++;
            if (count >= 50) {
                appendToConsole("  \u2502 " + String.format("%-6s%-43s\u2502", "...", "(" + (entries.size() - 50) + " ms)") + "\n", styleProgress);
                break;
            }
        }

        appendToConsole("  \u2514" + "\u2500".repeat(50) + "\u2518\n", styleInfo);
        appendToConsole("  Total: " + entries.size() + " disponibles\n", styleInfo);
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();

        JMenu menuBuild = new JMenu("Build");
        JMenu menuFuses = new JMenu("Fuses");
        JMenu menuList = new JMenu("Lista");

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        addMenuItem(menuBuild, "Write .hex", "Write .hex", KeyStroke.getKeyStroke(KeyEvent.VK_W, mask));
        addMenuItem(menuBuild, "Read .hex", "Read .hex", KeyStroke.getKeyStroke(KeyEvent.VK_R, mask));
        addMenuItem(menuBuild, "Verificar .hex", "Verificar .hex", null);

        addMenuItem(menuFuses, "Read fuses", "Read fuses", null);
        addMenuItem(menuFuses, "Write fuses", "Write fuses", null);

        addMenuItem(menuList, "Lista mc", "Lista mc", KeyStroke.getKeyStroke(KeyEvent.VK_M, mask | InputEvent.SHIFT_DOWN_MASK));
        addMenuItem(menuList, "Lista programadores", "Lista programadores", KeyStroke.getKeyStroke(KeyEvent.VK_P, mask | InputEvent.SHIFT_DOWN_MASK));

        menuBar.add(menuBuild);
        menuBar.add(menuFuses);
        menuBar.add(menuList);
    }

    private void addMenuItem(JMenu menu, String text, String command, KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(command);
        item.addActionListener(new CargarAction());
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        menu.add(item);
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(TOOLBAR_BG);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        JButton btnOpen = makeFlatButton("\u2191 Abrir", "abrir_hex", ACCENT, ACCENT_HOVER);
        btnOpen.setToolTipText("Seleccionar archivo .hex");
        btnOpen.addActionListener(new ArchivoAction());
        toolBar.add(btnOpen);

        JButton btnSave = makeFlatButton("\u2193 Guardar", "guardar_hex", ACCENT, ACCENT_HOVER);
        btnSave.setToolTipText("Guardar archivo .hex");
        btnSave.addActionListener(new GuardarAction());
        toolBar.add(btnSave);

        JButton btnConnect = makeFlatButton("\u26A1 Conexión", "Prueba conexión", SUCCESS_BTN, SUCCESS_HOVER);
        btnConnect.setToolTipText("Probar conexión con programador");
        btnConnect.addActionListener(new CargarAction());
        toolBar.add(btnConnect);

        JButton btnClear = makeFlatButton("\u232B Borrar", "borrar", DANGER, DANGER_HOVER);
        btnClear.setToolTipText("Limpiar consola");
        btnClear.addActionListener(e -> {
            clearConsole();
            setStatusReady();
            appendToConsole("Consola limpiada.\n", styleProgress);
        });
        toolBar.add(btnClear);

        toolBar.addSeparator();

        toolBar.add(makeSectionLabel("MC:"));
        toolBar.add(comboMc);

        btnListaMc = makeFlatButton("\u21BB", "Lista mc", LABEL_SEC, LABEL_PRI);
        btnListaMc.setToolTipText("Listar microcontroladores disponibles");
        btnListaMc.setFont(btnListaMc.getFont().deriveFont(Font.PLAIN, 13f));
        btnListaMc.setMargin(new Insets(2, 6, 2, 6));
        btnListaMc.addActionListener(new CargarAction());
        toolBar.add(btnListaMc);

        toolBar.addSeparator();

        toolBar.add(makeSectionLabel("PG:"));
        toolBar.add(comboPg);

        btnListaPg = makeFlatButton("\u21BB", "Lista programadores", LABEL_SEC, LABEL_PRI);
        btnListaPg.setToolTipText("Listar programadores disponibles");
        btnListaPg.setFont(btnListaPg.getFont().deriveFont(Font.PLAIN, 13f));
        btnListaPg.setMargin(new Insets(2, 6, 2, 6));
        btnListaPg.addActionListener(new CargarAction());
        toolBar.add(btnListaPg);

        return toolBar;
    }

    private JButton makeFlatButton(String text, String actionCommand, Color bg, Color hoverBg) {
        JButton btn = new JButton(text);
        btn.setActionCommand(actionCommand);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 12f));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverBg);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bg);
            }
        });
        return btn;
    }

    private JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setForeground(LABEL_SEC);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        return lbl;
    }

    private JPanel createFusePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_APP);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        panel.setPreferredSize(new Dimension(200, 0));

        JLabel sectionTitle = new JLabel("Fuses");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 14f));
        sectionTitle.setForeground(ACCENT);

        Font valueFont = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11f);
        lblLfuse.setFont(valueFont);
        lblLfuse.setForeground(new Color(0, 150, 136));
        lblHfuse.setFont(valueFont);
        lblHfuse.setForeground(new Color(0, 150, 136));
        lblEfuse.setFont(valueFont);
        lblEfuse.setForeground(new Color(0, 150, 136));

        JLabel lblL = new JLabel("Low Fuse");
        JLabel lblH = new JLabel("High Fuse");
        JLabel lblE = new JLabel("Ext. Fuse");
        for (JLabel l : new JLabel[]{lblL, lblH, lblE}) {
            l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
            l.setForeground(LABEL_PRI);
        }

        JButton btnRead = makeFlatButton("Leer Fuses", "Read fuses", ACCENT, ACCENT_HOVER);
        btnRead.addActionListener(new CargarAction());
        btnRead.setMinimumSize(new Dimension(150, 32));

        JButton btnWrite = makeFlatButton("Escribir Fuses", "Write fuses", DANGER, DANGER_HOVER);
        btnWrite.addActionListener(new CargarAction());
        btnWrite.setMinimumSize(new Dimension(150, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridy = 0; gbc.weighty = 1.0; gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(new JLabel(), gbc);

        gbc.gridy = 1; gbc.weighty = 0; gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(sectionTitle, gbc);

        gbc.gridy = 2; gbc.insets = new Insets(4, 0, 2, 0);
        panel.add(lblL, gbc);
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 1, 0);
        panel.add(selectL, gbc);
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(lblLfuse, gbc);

        gbc.gridy = 5; gbc.insets = new Insets(4, 0, 2, 0);
        panel.add(lblH, gbc);
        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 1, 0);
        panel.add(selectH, gbc);
        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(lblHfuse, gbc);

        gbc.gridy = 8; gbc.insets = new Insets(4, 0, 2, 0);
        panel.add(lblE, gbc);
        gbc.gridy = 9; gbc.insets = new Insets(0, 0, 1, 0);
        panel.add(selectE, gbc);
        gbc.gridy = 10; gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(lblEfuse, gbc);

        gbc.gridy = 11; gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(btnRead, gbc);

        gbc.gridy = 12; gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(btnWrite, gbc);

        gbc.gridy = 13; gbc.weighty = 1.0;
        panel.add(new JLabel(), gbc);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(250, 250, 252));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 222, 228)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));

        Font valueFont = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11f);

        lblHex.setFont(valueFont);
        lblHex.setForeground(new Color(33, 33, 33));
        lblMc.setFont(valueFont);
        lblMc.setForeground(new Color(33, 33, 33));
        lblPg.setFont(valueFont);
        lblPg.setForeground(new Color(33, 33, 33));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        center.setOpaque(false);
        center.add(makeStatusChip("HEX", lblHex, ACCENT));
        center.add(makeVSeparator());
        center.add(makeStatusChip("MC", lblMc, new Color(106, 27, 154)));
        center.add(makeVSeparator());
        center.add(makeStatusChip("PG", lblPg, new Color(0, 121, 107)));

        statusBar.add(center, BorderLayout.CENTER);
        return statusBar;
    }

    private JPanel makeStatusChip(String title, JLabel value, Color accent) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chip.setOpaque(false);

        JLabel dot = new JLabel("\u25CF");
        dot.setFont(dot.getFont().deriveFont(Font.PLAIN, 9f));
        dot.setForeground(accent);

        JLabel titleLabel = new JLabel(title + " ");
        titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 11f));
        titleLabel.setForeground(new Color(120, 120, 130));

        chip.add(dot);
        chip.add(titleLabel);
        chip.add(value);

        return chip;
    }

    private JSeparator makeVSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 16));
        sep.setForeground(new Color(220, 222, 228));
        return sep;
    }

    private void updateMcCombo(java.util.List<String> mcIds) {
        if (mcIds.isEmpty()) return;
        String saved = mc;
        comboMc.removeAllItems();
        for (String id : mcIds) {
            comboMc.addItem(id);
        }
        if (mcIds.contains(saved)) {
            comboMc.setSelectedItem(saved);
        } else {
            comboMc.setSelectedIndex(0);
        }
        mc = Objects.toString(comboMc.getSelectedItem(), "m16");
        lblMc.setText(mc);
    }

    private void updatePgCombo(java.util.List<String> pgIds) {
        if (pgIds.isEmpty()) return;
        String saved = pg;
        comboPg.removeAllItems();
        for (String id : pgIds) {
            comboPg.addItem(id);
        }
        if (pgIds.contains(saved)) {
            comboPg.setSelectedItem(saved);
        } else {
            comboPg.setSelectedIndex(0);
        }
        pg = Objects.toString(comboPg.getSelectedItem(), "usbasp");
        lblPg.setText(pg);
    }

    private class ArchivoAction implements ActionListener {
        private final JFileChooser fc = new JFileChooser();

        @Override
        public void actionPerformed(ActionEvent e) {
            fc.setDialogTitle("Seleccionar archivo .hex");
            fc.setFileFilter(new FileNameExtensionFilter("Archivos HEX (*.hex)", "hex"));
            int option = fc.showOpenDialog(panel.this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File archivo = fc.getSelectedFile();
                hex = archivo.getAbsolutePath();
                lblHex.setText(archivo.getName());
            } else if (option == JFileChooser.CANCEL_OPTION) {
                hex = "";
                lblHex.setText("Ningún archivo");
            }
        }
    }

    private class GuardarAction implements ActionListener {
        private final JFileChooser fc = new JFileChooser();

        @Override
        public void actionPerformed(ActionEvent e) {
            fc.setDialogTitle("Guardar archivo .hex");
            fc.setFileFilter(new FileNameExtensionFilter("Archivos HEX (*.hex)", "hex"));
            int option = fc.showSaveDialog(panel.this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File archivo = fc.getSelectedFile();
                String path = archivo.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".hex")) {
                    path += ".hex";
                    archivo = new File(path);
                }
                gHex = path;
                lblHex.setText(archivo.getName());
            } else if (option == JFileChooser.CANCEL_OPTION) {
                gHex = "";
            }
        }
    }

    private class CargarAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            if (actionCommand == null || actionCommand.isEmpty()) {
                actionCommand = "Prueba conexión";
            }

            String finalAction = actionCommand;
            String[] fuses = {lfuse, hfuse, efuse};

            boolean isListaMc = finalAction.equals("Lista mc");
            boolean isListaPg = finalAction.equals("Lista programadores");

            if (isListaMc) {
                btnListaMc.setEnabled(false);
                btnListaMc.setText("...");
            }
            if (isListaPg) {
                btnListaPg.setEnabled(false);
                btnListaPg.setText("...");
            }

            clearConsole();
            appendTimestamp();
            appendHeader(finalAction, mc, pg, hex);

            setStatusRunning(finalAction);

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    control_avrdudes c = new control_avrdudes();
                    return c.cargar(hex, gHex, mc, pg, fuses, finalAction);
                }

                @Override
                protected void done() {
                    try {
                        String result = get();

                        boolean isList = isListaMc || isListaPg;

                        if (isList) {
                            String type = isListaMc ? "mc" : "pg";
                            appendListAsTable(result, type);
                            appendToConsole("\n", styleNormal);
                        } else {
                            appendStyledOutput(result);
                        }

                        setStatusSuccess(finalAction);

                        if (isListaMc) {
                            java.util.List<String> mcIds = control_avrdudes.parseAvrdudeList(result, "mc");
                            updateMcCombo(mcIds);
                            btnListaMc.setEnabled(true);
                            btnListaMc.setText("\u21BB");
                        }
                        if (isListaPg) {
                            java.util.List<String> pgIds = control_avrdudes.parseAvrdudeList(result, "pg");
                            updatePgCombo(pgIds);
                            btnListaPg.setEnabled(true);
                            btnListaPg.setText("\u21BB");
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        clearConsole();
                        appendTimestamp();
                        appendHeader("Error", mc, pg, "");
                        appendToConsole(ex.getMessage() + "\n", styleError);
                        setStatusError(finalAction);
                        btnListaMc.setEnabled(true);
                        btnListaMc.setText("\u21BB");
                        btnListaPg.setEnabled(true);
                        btnListaPg.setText("\u21BB");
                    }
                }
            };
            worker.execute();
        }
    }
}

class ConsoleUtils {
    static void disableFlicker(JTextPane pane) {
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    }
}