package sincroestancia.src.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import sincroestancia.src.utils.StyleUtils;

/**
 * Clase de utilidad estática encargada de estandarizar la apariencia visual de los botones.
 * * Define estilos semánticos (Primario, Secundario, Peligro, Éxito) y asegura la
 * consistencia en toda la aplicación imponiendo dimensiones fijas y comportamientos
 * interactivos comunes (Hover, Click).
 * * @author Carlos Padilla Labella
 */
public class ButtonUtils {

    private static final Dimension STANDARD_BUTTON_SIZE = new Dimension(180, 30);

    /**
     * Aplica el estilo visual "Primario"
     * * @param btn El botón al que se aplicará el estilo.
     */
    public static void stylePrimary(JButton btn) {
        applyStyle(btn, StyleUtils.COLOR_PRIMARY, StyleUtils.COLOR_PRIMARY_HOVER, Color.WHITE);
    }

    /**
     * Aplica el estilo visual "Secundario" (Gris Claro).
     * * @param btn El botón al que se aplicará el estilo.
     */
    public static void styleSecondary(JButton btn) {
        applyStyle(btn, StyleUtils.COLOR_SECONDARY, StyleUtils.COLOR_SECONDARY_HOVER, StyleUtils.COLOR_TEXT_SECONDARY);
        btn.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
    }

    /**
     * Aplica el estilo visual de "Peligro" (Rojo).
     * * @param btn El botón al que se aplicará el estilo.
     */
    public static void styleDanger(JButton btn) {
        applyStyle(btn, StyleUtils.COLOR_DANGER, StyleUtils.COLOR_DANGER_HOVER, Color.WHITE);
    }
    
    /**
     * Aplica el estilo visual de "Éxito" (Verde).
     * * @param btn El botón al que se aplicará el estilo.
     */
    public static void styleSuccess(JButton btn) {
        applyStyle(btn, StyleUtils.COLOR_SUCCESS, StyleUtils.COLOR_SUCCESS_HOVER, Color.WHITE);
    }

    /**
     * Lógica interna para aplicar la renderización personalizada a los botones estándar.
     * * Características aplicadas:
     * - Fuente tipográfica global.
     * - Cursores de mano (Hand Cursor).
     * - Eliminación de bordes y rellenos por defecto de Swing.
     * - Imposición de tamaño fijo (STANDARD_BUTTON_SIZE) para uniformidad.
     * - Sobrescritura del método paintComponent para dibujar bordes redondeados (8px)
     * y gestionar los cambios de color al pasar el ratón (Hover) o presionar.
     */
    private static void applyStyle(JButton btn, Color bgColor, Color hoverColor, Color textColor) {

        btn.setFont(StyleUtils.FONT_BUTTON);
        btn.setForeground(textColor);
        btn.setBackground(bgColor);
        
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.setPreferredSize(STANDARD_BUTTON_SIZE);
        btn.setMinimumSize(STANDARD_BUTTON_SIZE);
        btn.setMaximumSize(STANDARD_BUTTON_SIZE);
        
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (btn.getModel().isPressed()) {
                    g2.setColor(hoverColor.darker());
                } else if (btn.getModel().isRollover() && btn.isEnabled()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(btn.isEnabled() ? btn.getBackground() : new Color(220, 220, 220));
                }

                g2.fillRoundRect(0, 0, btn.getWidth(), btn.getHeight(), 8, 8);
        
                if (bgColor == StyleUtils.COLOR_SECONDARY && btn.isEnabled()) {
                     g2.setColor(new Color(200, 200, 200));
                     g2.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, 8, 8);
                }

                g2.dispose();
                super.paint(g, c);
            }
        });
        
        btn.addPropertyChangeListener("enabled", evt -> btn.repaint());
    }

    /**
     * Aplica un estilo de borde redondeado básico sin imponer un tamaño fijo.
     * * Este método se mantiene por compatibilidad con componentes específicos como el Calendario,
     * donde el tamaño de los botones es variable o muy pequeño.
     * * @param button El botón a estilizar.
     */
    public static void applyRoundedStyle(JButton button) {
        applyRoundedStyle(button, 8);
    }

    /**
     * Aplica un estilo de borde redondeado con un radio personalizado.
     * * @param button El botón a estilizar.
     * @param radius Radio de las esquinas en píxeles.
     */
    public static void applyRoundedStyle(JButton button, int radius) {
        final java.awt.Color originalBg = button.getBackground();

        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                java.awt.Color bgColor = btn.getBackground();
                if (bgColor == null) {
                    bgColor = originalBg;
                }

                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, radius, radius);

                g2.dispose();
                super.paint(g, c);
            }
        });
    }
}