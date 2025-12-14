package sincroestancia.src.gui.components;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

/**
 * Implementación de borde personalizado para definir la geometría y espaciado de componentes redondeados.
 * * Aunque hereda de AbstractBorder, su función principal en este sistema de diseño es:
 * - Definir el radio de curvatura (por defecto 8px, estilo 'rounded-lg').
 * - Establecer el padding interno (Insets) para que el texto no toque los bordes.
 * * Nota: El método paintBorder no dibuja una línea visible; el dibujo del fondo redondeado
 * se gestiona usualmente en el ui-delegate del componente (ver ButtonUtils).
 * * @author Carlos Padilla Labella
 */
public class RoundedBorder extends AbstractBorder {

    private final int radius;

    /**
     * Constructor. Aplica un radio de 8 píxeles.
     */
    public RoundedBorder() {
        this.radius = 8;
    }

    /**
     * Constructor personalizado.
     * * @param radius Radio de las esquinas en píxeles.
     */
    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    /**
     * Método de pintado del borde.
     * * En esta implementación específica, no se realiza dibujo de líneas (stroke).
     * Se configura el antialiasing por seguridad, pero se deja el pintado visual
     * al propio componente para permitir fondos rellenos completos.
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.dispose();
    }

    /**
     * Define el margen interior (padding) del componente.
     * * Se establece un espaciado generoso (10px vertical, 20px horizontal) para
     * mantener la estética de botones modernos y legibles.
     */
    @Override
    public java.awt.Insets getBorderInsets(Component c) {
        return new java.awt.Insets(10, 20, 10, 20);
    }

    @Override
    public java.awt.Insets getBorderInsets(Component c, java.awt.Insets insets) {
        insets.left = insets.right = 20;
        insets.top = insets.bottom = 10;
        return insets;
    }
}
