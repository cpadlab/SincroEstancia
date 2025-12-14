package sincroestancia.src.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Componente de interfaz gráfica que implementa un menú lateral deslizante (Drawer).
 * * Se comporta como un panel superpuesto (Overlay) que se desliza desde la izquierda.
 * * Características principales:
 * - Integración en JLayeredPane (Capa POPUP_LAYER) para flotar sobre el contenido.
 * - Diseño responsivo: El menú ocupa el 50% del ancho de la ventana (mínimo 300px).
 * - Animación suave de apertura/cierre.
 * - Fondo oscurecido (Dimming) que cierra el menú al hacer clic fuera de él.
 * * @author Carlos Padilla Labella
 */
public class Drawer extends JPanel {

    private final JPanel contentPanel;
    private JFrame parentFrame;

    private Timer animationTimer = null;

    private int currentX = 0;
    private boolean isOpen = false;
    private int drawerWidth = 300;
    private final int ANIMATION_SPEED = 40; 

    /**
     * Constructor del Drawer.
     * * Configura el panel transparente base y el contenedor de contenido deslizante.
     * * Inicializa la lógica de animación y los listeners de ratón para el cierre automático.
     * * @param content El componente o panel que se mostrará dentro del menú lateral.
     */
    public Drawer(Component content) {

        super(null);
        setOpaque(false);
        setVisible(false);

        contentPanel = new JPanel();
        contentPanel.setLayout(new java.awt.BorderLayout());
        contentPanel.add(content);
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0,0,0,50)));

        currentX = -drawerWidth;
        contentPanel.setBounds(currentX, 0, drawerWidth, 0);
        this.add(contentPanel);

        animationTimer = new Timer(10, e -> {
            
            if (isOpen) {
                
                currentX += ANIMATION_SPEED;
                
                if (currentX >= 0) {
                    currentX = 0;
                    animationTimer.stop();
                }

            } else {
                
                currentX -= ANIMATION_SPEED;
                
                if (currentX <= -drawerWidth) {
                    currentX = -drawerWidth;
                    animationTimer.stop();
                    setVisible(false);
                }

            }

            contentPanel.setLocation(currentX, 0);
            repaint();

        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!contentPanel.getBounds().contains(e.getPoint())) {
                    close();
                }
            }
        });
    }

    /**
     * Vincula este Drawer a una ventana JFrame específica.
     * * Pasos de implementación:
     * - Inserta el componente en la capa POPUP_LAYER del JLayeredPane del frame.
     * Esto asegura que esté por encima del contenido normal pero debajo de diálogos modales.
     * - Añade un listener de redimensionamiento al frame para ajustar el ancho dinámicamente.
     * * @param frame La ventana principal a la que se adhiere el menú.
     */
    public void attachToFrame(JFrame frame) {
        
        this.parentFrame = frame;
        
        frame.getLayeredPane().add(this, JLayeredPane.POPUP_LAYER);
        
        updateBounds();
        
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                updateBounds();
            }
        });

    }
    
    /**
     * Recalcula las dimensiones y posición de los componentes según el tamaño actual de la ventana.
     * * Lógica de Ancho Dinámico:
     * - El Drawer ocupará el 50% del ancho de la ventana padre.
     * - Se establece un mínimo de 300px para asegurar usabilidad en pantallas pequeñas.
     */
    private void updateBounds() {
        
        if (parentFrame != null) {
            
            int frameW = parentFrame.getWidth();
            int frameH = parentFrame.getHeight();
            
            setBounds(0, 0, frameW, frameH);
            
            this.drawerWidth = Math.max(300, (int)(frameW * 0.5));
            
            contentPanel.setSize(this.drawerWidth, frameH);
            
            if (!isOpen) {
                currentX = -this.drawerWidth;
                contentPanel.setLocation(currentX, 0);
            } else {
                contentPanel.setLocation(0, 0);
            }
            
            revalidate();
        }
    }

    /**
     * Inicia la animación de apertura del menú.
     */
    public void open() {
        if (!isOpen) {
            isOpen = true;
            this.setVisible(true);
            updateBounds();
            animationTimer.start();
        }
    }

    /**
     * Inicia la animación de cierre del menú.
     */
    public void close() {
        if (isOpen) {
            isOpen = false;
            animationTimer.start();
        }
    }

    /**
     * Alterna entre el estado abierto y cerrado.
     */
    public void toggle() {
        if (isOpen) close(); else open();
    }

    /**
     * Renderizado personalizado para el efecto de oscurecimiento (Overlay).
     * * Dibuja un fondo negro semitransparente sobre toda la ventana.
     * * La opacidad se calcula dinámicamente en función de la posición del menú deslizante
     * para crear un efecto de "fade in/out" sincronizado con el movimiento.
     */
    @Override
    protected void paintComponent(Graphics g) {
        
        if (isOpen || animationTimer.isRunning()) {
            
            Graphics2D g2 = (Graphics2D) g;
            
            float opacity = (float) (drawerWidth + currentX) / drawerWidth * 0.5f;
            opacity = Math.max(0, Math.min(0.5f, opacity));
            
            g2.setColor(new Color(0, 0, 0, (int)(opacity * 255)));
            g2.fillRect(0, 0, getWidth(), getHeight());

        }

        super.paintComponent(g);

    }
}