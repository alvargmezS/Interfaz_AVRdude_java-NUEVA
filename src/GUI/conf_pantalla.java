package GUI;

import java.awt.*;

public class conf_pantalla {

    private final int ancho;
    private final int alto;

    public conf_pantalla() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        ancho = bounds.width - screenInsets.left - screenInsets.right;
        alto = bounds.height - screenInsets.top - screenInsets.bottom;
    }

    public int ancho() {
        return ancho;
    }

    public int alto() {
        return alto;
    }

    public Dimension getScreenSize() {
        return new Dimension(ancho, alto);
    }
}