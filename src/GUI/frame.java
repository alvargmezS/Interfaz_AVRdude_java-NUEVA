package GUI;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class frame extends JFrame {

    public frame() {
        setTitle("AVRdude");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        URL iconUrl = getClass().getResource("Iconos/microcontrolador(64).png");
        if (iconUrl != null) {
            setIconImage(new ImageIcon(iconUrl).getImage());
        }

        panel mainPanel = new panel();
        setContentPane(mainPanel);
        setJMenuBar(mainPanel.getMenuBar());
    }
}