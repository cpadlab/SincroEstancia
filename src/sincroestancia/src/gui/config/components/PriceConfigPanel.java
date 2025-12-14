package sincroestancia.src.gui.config.components;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.VutItem;

/**
 * Panel para la configuración masiva de precios y temporadas.
 * * Divide la interfaz en dos secciones:
 * 1. Izquierda: Definición de niveles de precios. El sistema clasifica automáticamente
 * los precios en 'Baja', 'Media' o 'Alta' temporada según su valor relativo.
 * 2. Derecha: Calendario visual interactivo (DateRangePicker) para seleccionar rangos de fechas
 * y aplicarles el precio seleccionado.
 * * @author carlospadilla
 */
public class PriceConfigPanel extends JPanel {

    private DefaultListModel<PriceItem> listModel;
    private JList<PriceItem> priceList;
    private JTextField txtNewPrice;

    private DateRangePicker calendarPicker; 
    
    private JButton btnAddPrice;
    private JButton btnApplyRange;
    private JButton btnClearPrices;

    private DatabaseService dbService;
    private VutItem currentVut;

    /**
     * Constructor.
     * * Inicializa el servicio de base de datos y construye la interfaz dividida en dos columnas.
     */
    public PriceConfigPanel() {
        this.dbService = new DatabaseService();
        initComponents();
    }

    /**
     * Establece la vivienda activa y refresca el calendario visual con sus datos.
     * * @param vut Objeto VutItem con la información de la vivienda.
     */
    public void setVut(VutItem vut) {
        this.currentVut = vut;
        if (this.currentVut != null) {
            calendarPicker.setVutId(this.currentVut.getId());
        } else {
            calendarPicker.setVutId(-1);
        }
    }

    /**
     * Inicialización de componentes gráficos.
     * * Usa un GridLayout de 1 fila y 2 columnas para separar la gestión de precios del calendario.
     */
    private void initComponents() {
        
        setLayout(new GridLayout(1, 2, 20, 0)); 
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(createTitledBorder("1. Definición de niveles de precios"));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.setBackground(Color.WHITE);
        
        JLabel lblPrice = new JLabel("Precio (€):");
        txtNewPrice = new JTextField(8);
        btnAddPrice = new JButton("Añadir");
        ButtonUtils.stylePrimary(btnAddPrice);
        
        btnClearPrices = new JButton("Limpiar");
        btnClearPrices.setBackground(new Color(255, 230, 230)); 

        inputPanel.add(lblPrice);
        inputPanel.add(txtNewPrice);
        inputPanel.add(btnAddPrice);
        inputPanel.add(btnClearPrices);

        listModel = new DefaultListModel<>();
        priceList = new JList<>(listModel);
        priceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollList = new JScrollPane(priceList);

        leftPanel.add(inputPanel, BorderLayout.NORTH);
        leftPanel.add(scrollList, BorderLayout.CENTER);
        leftPanel.add(new JLabel("<html><i>* El sisterma autocalcula Low/Avg/High basado en los valores.</i></html>"), BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(createTitledBorder("2. Seleccionar periodos y Aplicar"));

        calendarPicker = new DateRangePicker();
        rightPanel.add(calendarPicker, BorderLayout.CENTER);

        JPanel southContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southContainer.setBackground(Color.WHITE);
        
        JLabel helpLabel = new JLabel("<html><small>Seleccione la fecha de inicio y, a continuación, la fecha de finalización.</small></html>");
        helpLabel.setForeground(Color.GRAY);
        
        btnApplyRange = new JButton("Aplicar precio a la selección");
        ButtonUtils.styleSecondary(btnApplyRange);
        btnApplyRange.setPreferredSize(new Dimension(200, 40));
        
        southContainer.add(helpLabel);
        southContainer.add(Box.createHorizontalStrut(15));
        southContainer.add(btnApplyRange);
        
        rightPanel.add(southContainer, BorderLayout.SOUTH);

        add(leftPanel);
        add(rightPanel);

        setupListeners();

    }

    /**
     * Configura la lógica de interacción de los botones.
     */
    private void setupListeners() {
        
        btnAddPrice.addActionListener(e -> {
            try {
                double price = Double.parseDouble(txtNewPrice.getText().trim());
                if (price <= 0) throw new NumberFormatException();
                addPriceToList(price);
                txtNewPrice.setText("");
                recalculateSeasons(); 
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive number.", "Invalid Price", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnClearPrices.addActionListener(e -> listModel.clear());
        btnApplyRange.addActionListener(e -> applyPriceToRange());
    }

    /**
     * Añade un nuevo precio a la lista evitando duplicados.
     */
    private void addPriceToList(double price) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).price == price) return;
        }
        listModel.addElement(new PriceItem(price));
    }

    /**
     * Recalcula dinámicamente la "Temporada" de cada precio en la lista.
     * * Algoritmo basado en percentiles:
     * - Ordena los precios de menor a mayor.
     * - Divide la lista en terciles:
     * - Inferior 33% -> Temporada Baja (Low).
     * - Medio 33% -> Temporada Media (Average).
     * - Superior 33% -> Temporada Alta (High).
     * * Esto permite al usuario solo preocuparse por el dinero, no por la etiqueta.
     */
    private void recalculateSeasons() {
        int size = listModel.size();
        if (size == 0) return;

        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            prices.add(listModel.get(i).price);
        }
        Collections.sort(prices);

        for (int i = 0; i < size; i++) {
            PriceItem item = listModel.get(i);
            int rank = prices.indexOf(item.price);
            
            if (size == 1) {
                item.season = "average"; 
            } else if (size == 2) {
                item.season = (rank == 0) ? "low" : "high";
            } else {
                double percentile = (double) rank / (size - 1);
                if (percentile < 0.33) item.season = "low";
                else if (percentile < 0.66) item.season = "average";
                else item.season = "high";
            }
        }
        priceList.repaint();
    }

    /**
     * Aplica el precio seleccionado en la lista al rango de fechas seleccionado en el calendario.
     * * Flujo de ejecución:
     * - Valida que haya una vivienda, un precio y un rango de fechas seleccionados.
     * - Pide confirmación al usuario mostrando el resumen de la operación.
     * - Llama a `dbService.update_price_range` para persistir los cambios.
     * - Recarga el calendario visual para mostrar los nuevos colores.
     */
    private void applyPriceToRange() {
        
        if (currentVut == null) {
            JOptionPane.showMessageDialog(this, "No VUT selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PriceItem selectedItem = priceList.getSelectedValue();
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Select a price from the left list.", "Selection Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate start = calendarPicker.getStartDate();
        LocalDate end = calendarPicker.getEndDate();

        if (start == null) {
            JOptionPane.showMessageDialog(this, "Please select at least one date on the calendar.", "No Date Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String msg = String.format("Apply %.2f€ (%s)\nFrom: %s\nTo: %s?", 
                selectedItem.price, selectedItem.season, start, end);
                
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Update", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {

            boolean success = dbService.update_price_range(currentVut.getId(), start, end, selectedItem.price, selectedItem.season);
            
            if (success) {
                JOptionPane.showMessageDialog(this, "Calendar updated successfully!");
                calendarPicker.resetSelection(); 
                calendarPicker.reloadData();                 
            } else {
                JOptionPane.showMessageDialog(this, "Database Error.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Crea un borde con título estandarizado y elegante.
     */
    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(50, 50, 50)
        );
    }

    /**
     * Clase interna para representar un elemento de precio en la lista JList.
     */
    private static class PriceItem {

        double price;
        String season; 

        public PriceItem(double price) {
            this.price = price;
            this.season = "average"; 
        }

        @Override
        public String toString() {
            return String.format("%.2f €  -  [%s]", price, season.toUpperCase());
        }
        
    }
}