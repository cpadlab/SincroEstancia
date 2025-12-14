package sincroestancia.src.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Componente contenedor para ventanas modales superpuestas (Overlay).
 * * Permite mostrar contenido crítico (como formularios de edición o mensajes importantes)
 * bloqueando la interacción con el resto de la aplicación hasta que se cierre.
 * * Características técnicas:
 * - Oscurecimiento del fondo (Dimming) al 50%.
 * - Centrado automático del contenido (ocupando el 90% de la pantalla).
 * - Gestión de capas (Z-Index 500) para garantizar que aparezca sobre el Drawer y el contenido base.
 * * @author Carlos Padilla Labella
 */
public class Modal extends JPanel {

    private JFrame parentFrame;
    private boolean isOpen = false;

    /**
     * Constructor del Modal.
     * * Configura el panel como transparente (para pintar el fondo oscurecido manualmente)
     * y añade un listener de ratón que "consume" los eventos de clic, impidiendo que
     * el usuario interactúe con los componentes que quedan debajo (bloqueo de UI).
     */
    public Modal() {
        
        setLayout(null);
        setOpaque(false);
        setVisible(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { }
        });

    }

    /**
     * Abre el modal y muestra el componente especificado.
     * * Pasos de implementación:
     * - Elimina cualquier contenido previo.
     * - Añade el nuevo panel de contenido.
     * - Activa la visibilidad y recalcula las dimensiones para centrarlo.
     * * @param contentPanel El panel a mostrar (ej. un formulario de Huésped).
     */
    public void show(Component contentPanel) {
        
        removeAll();
        add(contentPanel);

        this.isOpen = true;
        this.setVisible(true);
        
        recalculateBounds();
        repaint();
    }

    /**
     * Cierra el modal, oculta el overlay y libera los recursos visuales contenidos.
     */
    public void close() {
        this.isOpen = false;
        this.setVisible(false);
        removeAll();
    }

    /**
     * Vincula el Modal a la ventana principal en una capa de alta prioridad.
     * * Estrategia de Capas (JLayeredPane):
     * - Contenido Base: DEFAULT_LAYER (0).
     * - Drawer (Menú): POPUP_LAYER (300).
     * - Drag Layer: (400).
     * - Modal: Integer(500). Esto asegura que el modal tape absolutamente todo lo demás.
     * * @param frame La ventana JFrame padre.
     */
    public void attachToFrame(JFrame frame) {
        this.parentFrame = frame;
        
        frame.getLayeredPane().add(this, Integer.valueOf(500)); 
        
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                recalculateBounds();
            }
        });
        
        recalculateBounds();
    }

    /**
     * Recalcula la geometría del overlay y del contenido interno.
     * * El overlay ocupa el 100% de la ventana.
     * * El contenido interno se centra y se redimensiona al 90% del ancho y alto disponibles.
     */
    private void recalculateBounds() {
        if (parentFrame != null) {
            
            setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
            
            if (getComponentCount() > 0) {
                
                Component c = getComponent(0);

                int w = (int) (parentFrame.getWidth() * 0.9);
                int h = (int) (parentFrame.getHeight() * 0.9);
                
                int x = (parentFrame.getWidth() - w) / 2;
                int y = (parentFrame.getHeight() - h) / 2;
                
                c.setBounds(x, y, w, h);
            }
            
            revalidate();
            repaint();
        }
    }

    /**
     * Pinta el fondo semitransparente para crear el efecto de oscurecimiento (dimming).
     * * Usa color negro con canal Alpha 128 (aprox 50% de opacidad).
     */
    @Override
    protected void paintComponent(Graphics g) {

        if (isOpen) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 128));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        super.paintComponent(g);

    }
}