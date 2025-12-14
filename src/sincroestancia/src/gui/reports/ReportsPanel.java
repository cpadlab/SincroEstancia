package sincroestancia.src.gui.reports;

import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.VutItem;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.services.PdfReportService;

/**
 * Panel principal para la visualización de reportes y estadísticas (Dashboard de Rendimiento).
 * * Integra la librería JFreeChart para renderizar gráficos interactivos sobre:
 * - Evolución de ingresos anuales.
 * - Demografía de los huéspedes (nacionalidades).
 * - Tendencias de ocupación porcentual.
 * * Permite filtrar los datos por año actual y exportar un informe completo en PDF.
 * * También muestra una lista rápida de próximas operaciones (Check-in/Check-out).
 * * @author Carlos Padilla Labella
 */
public class ReportsPanel extends JPanel {

    private DatabaseService dbService;
    private int currentVutId = -1;
    private int selectedYear;
    
    private JComboBox<Integer> yearSelector;
    private JLabel lblRevenueValue;
    private JLabel lblOccupancyValue;
    
    private JButton btnExportPdf; 
    
    private JPanel revenueChartContainer;
    private JPanel nationalityChartContainer;
    private JPanel occupancyChartContainer;
    
    private JPanel movementsListPanel; 

    /**
     * Constructor del panel de reportes.
     * * Inicializa el servicio de base de datos y establece el año actual como filtro por defecto.
     * * Construye la interfaz gráfica completa.
     */
    public ReportsPanel() { 
        this.dbService = new DatabaseService();
        this.selectedYear = LocalDate.now().getYear();
        initComponents();
    }

    /**
     * Actualiza el contexto de la vivienda seleccionada y refresca los datos.
     * * @param vutId Identificador de la nueva vivienda activa.
     */
    public void updateVut(int vutId) {
        this.currentVutId = vutId;
        refreshData();
    }

    /**
     * Inicialización y disposición de los componentes visuales.
     * * Estructura:
     * - Arriba: Encabezado con título, filtros y tarjetas KPI (Ingresos, Ocupación).
     * - Izquierda: Pestañas (JTabbedPane) con los gráficos de JFreeChart.
     * - Derecha: Panel lateral con la lista de próximas entradas y salidas.
     */
    private void initComponents() {
        
        setLayout(new BorderLayout(20, 20));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(0, 15));
        topPanel.setBackground(Color.WHITE);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(Color.WHITE);
        
        JLabel title = new JLabel("Panel de control");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        
        JPanel filterPanel = createFilterPanel();
        
        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(filterPanel, BorderLayout.EAST);
        
        JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        cardsPanel.setBackground(Color.WHITE);
        cardsPanel.setPreferredSize(new Dimension(0, 100)); 
        
        JPanel cardRevenue = createKpiCard("Ingresos Totales", "€ 0.00", new Color(220, 255, 220), new Color(0, 100, 0));
        lblRevenueValue = (JLabel) cardRevenue.getClientProperty("valueLabel");
        
        JPanel cardOccupancy = createKpiCard("Ocupación media anual", "0%", new Color(220, 240, 255), new Color(0, 50, 150));
        lblOccupancyValue = (JLabel) cardOccupancy.getClientProperty("valueLabel");

        cardsPanel.add(cardRevenue);
        cardsPanel.add(cardOccupancy);

        topPanel.add(headerRow, BorderLayout.NORTH);
        topPanel.add(cardsPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        revenueChartContainer = new JPanel(new BorderLayout());
        nationalityChartContainer = new JPanel(new BorderLayout());
        occupancyChartContainer = new JPanel(new BorderLayout());
        
        tabs.addTab("Facuración", revenueChartContainer);
        tabs.addTab("Demografía", nationalityChartContainer);
        tabs.addTab("Ocupación", occupancyChartContainer);
        
        add(tabs, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setPreferredSize(new Dimension(320, 0)); 
        rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(230, 230, 230))); 

        JLabel lblListTitle = new JLabel("Próximos Eventos", SwingConstants.CENTER);
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblListTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        lblListTitle.setForeground(Color.GRAY);
        
        movementsListPanel = new JPanel();
        movementsListPanel.setLayout(new BoxLayout(movementsListPanel, BoxLayout.Y_AXIS));
        movementsListPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(movementsListPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel listContainerWithPadding = new JPanel(new BorderLayout());
        listContainerWithPadding.setBackground(Color.WHITE);
        listContainerWithPadding.setBorder(new EmptyBorder(0, 15, 0, 0)); 
        listContainerWithPadding.add(lblListTitle, BorderLayout.NORTH);
        listContainerWithPadding.add(scrollPane, BorderLayout.CENTER);

        rightPanel.add(listContainerWithPadding, BorderLayout.CENTER);
        
        add(rightPanel, BorderLayout.EAST);
    }
    
    /**
     * Crea el panel de filtros superior (Selector de año, botón refrescar y exportar).
     * * @return JPanel configurado.
     */
    private JPanel createFilterPanel() {
        
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.setBackground(Color.WHITE);
        
        yearSelector = new JComboBox<>();
        for (int i = 2024; i <= 2030; i++) yearSelector.addItem(i);
        yearSelector.setSelectedItem(selectedYear);

        yearSelector.addActionListener(e -> {
            selectedYear = (int) yearSelector.getSelectedItem();
            refreshData(); 
        });

        JButton btnRefresh = new JButton("Recargar");
        ButtonUtils.styleSecondary(btnRefresh);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setBackground(new Color(245, 245, 245)); 
        btnRefresh.addActionListener(e -> {
            refreshData();
            btnRefresh.setEnabled(false);
            new Timer(500, evt -> {
                btnRefresh.setEnabled(true);
                ((Timer)evt.getSource()).stop();
            }).start();
        });
        
        btnExportPdf = new JButton("Reporte PDF");
        btnExportPdf.setFocusPainted(false);
        btnExportPdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportPdf.setBackground(new Color(255, 235, 235)); 
        btnExportPdf.setForeground(new Color(180, 0, 0)); 
        ButtonUtils.styleDanger(btnExportPdf);
        btnExportPdf.addActionListener(e -> exportPdfAction());

        p.add(new JLabel("Año: "));
        p.add(yearSelector);
        p.add(Box.createHorizontalStrut(20)); 
        p.add(btnRefresh);
        p.add(Box.createHorizontalStrut(5)); 
        p.add(btnExportPdf); 

        return p;
    }
   
    /**
    * Lógica para la generación y exportación del informe PDF.
    * * Recopila todos los datos actuales de la pantalla y genera los gráficos
    * en memoria para pasárselos al servicio PdfReportService.
    */
    private void exportPdfAction() {
        
        if (currentVutId == -1) {
            JOptionPane.showMessageDialog(this, "No se ha seleccionado ninguna propiedad..", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        VutItem vut = dbService.get_vut_details_by_id(currentVutId);
        String vutName = (vut != null) ? vut.getName() : "VUT #" + currentVutId;
        String coverPath = (vut != null) ? vut.getCoverPath() : null;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar informe anual");
        
        fileChooser.setSelectedFile(new java.io.File("report_" + vutName + "_" + selectedYear + ".pdf"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            
            java.io.File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf"; 
            }

            Map<String, String> stats = new java.util.LinkedHashMap<>();
            stats.put("Nombre del VUT", vutName);
            stats.put("Año", String.valueOf(selectedYear));
            stats.put("Ingresos anuales totales", lblRevenueValue.getText());
            stats.put("Ocupación media", lblOccupancyValue.getText());

            stats.put("Generado el", LocalDate.now().toString());

            java.util.List<JFreeChart> chartList = new ArrayList<>();

            DefaultCategoryDataset datasetRev = new DefaultCategoryDataset();
            Map<Integer, Double> dataRev = dbService.getYearlyRevenueData(currentVutId, selectedYear);
            for (Map.Entry<Integer, Double> entry : dataRev.entrySet()) {
                datasetRev.addValue(entry.getValue(), "Revenue", java.time.Month.of(entry.getKey()).name());
            }
            chartList.add(ChartFactory.createBarChart(
                "Evolución de Facuración " + selectedYear, "Month", "Amount (€)", datasetRev, 
                PlotOrientation.VERTICAL, false, true, false
            ));

            DefaultPieDataset datasetNat = new DefaultPieDataset();
            Map<String, Integer> dataNat = dbService.getNationalityStats(currentVutId);
            
            for (Map.Entry<String, Integer> entry : dataNat.entrySet()) {
                datasetNat.setValue(entry.getKey(), entry.getValue());
            }
            
            chartList.add(ChartFactory.createPieChart("Procedencia de los visitantes (de todos los tiempos)", datasetNat, true, true, false));

            DefaultCategoryDataset datasetOcc = new DefaultCategoryDataset();
            Map<Integer, Double> dataOcc = dbService.getYearlyOccupancyStats(currentVutId, selectedYear);
            for (Map.Entry<Integer, Double> entry : dataOcc.entrySet()) {
                datasetOcc.addValue(entry.getValue(), "Ocupación %", java.time.Month.of(entry.getKey()).name());
            }
            chartList.add(ChartFactory.createLineChart(
                "Tasa de ocupación " + selectedYear, "Mensual", "Porcentaje (%)", datasetOcc, 
                PlotOrientation.VERTICAL, false, true, false
            ));

            PdfReportService pdfService = new PdfReportService();
            boolean success = pdfService.generateReport(
                filePath, 
                vutName, 
                "Reporte Anual " + selectedYear,
                coverPath, 
                stats, 
                chartList
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Annual PDF generated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Error generating PDF. Check console.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Método auxiliar para crear las tarjetas de KPI (Key Performance Indicators) superiores.
     */
    private JPanel createKpiCard(String title, String initialValue, Color bgColor, Color textColor) {
        
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 0),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(100, 100, 100));

        JLabel lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValue.setForeground(textColor);
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        card.putClientProperty("valueLabel", lblValue);

        return card;
    }

    /**
     * Crea una fila visual para la lista de próximas operaciones (Check-in/Check-out).
     * * Colorea la barra lateral: Verde para Entradas, Rojo para Salidas.
     */
    private JPanel createMovementRow(Map<String, String> mov) {

        String type = mov.get("type"); 
        String date = mov.get("date");
        String guest = mov.get("guest");

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(250, 250, 250)); 
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55)); 
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel colorBar = new JPanel();
        colorBar.setPreferredSize(new Dimension(4, 0));
        
        JLabel lblType = new JLabel(type);
        lblType.setFont(new Font("Segoe UI", Font.BOLD, 10));
        
        if (type.equals("CHECK-IN")) {
            colorBar.setBackground(new Color(46, 204, 113)); 
            lblType.setForeground(new Color(39, 174, 96));
        } else {
            colorBar.setBackground(new Color(231, 76, 60)); 
            lblType.setForeground(new Color(192, 57, 43));
        }

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(new Color(250, 250, 250));
        textPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        
        JLabel lblDate = new JLabel(date);
        lblDate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JLabel lblGuest = new JLabel(guest);
        lblGuest.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblGuest.setForeground(Color.GRAY);
        
        JPanel dateTypeRow = new JPanel(new BorderLayout());
        dateTypeRow.setBackground(new Color(250, 250, 250));
        dateTypeRow.add(lblDate, BorderLayout.WEST);
        dateTypeRow.add(lblType, BorderLayout.EAST);
        
        textPanel.add(dateTypeRow);
        textPanel.add(lblGuest);

        row.add(colorBar, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);

        return row;
    }

    /**
     * Coordina la actualización de todos los datos del panel.
     * * Invoca al servicio de base de datos para recalcular KPIs y redibujar gráficos.
     */
    private void refreshData() {
        if (currentVutId == -1) return;

        double revenue = dbService.getTotalYearlyRevenue(currentVutId, selectedYear);
        lblRevenueValue.setText(String.format("€ %.2f", revenue));

        double occupancy = dbService.getYearlyOccupancyPercentage(currentVutId, selectedYear);
        lblOccupancyValue.setText(String.format("%.1f%%", occupancy));

        updateRevenueChart();
        updateNationalityChart();
        updateOccupancyChart();
        updateMovementsList();

    }
    
    /**
     * Refresca la lista lateral de movimientos.
     */
    private void updateMovementsList() {
        
        movementsListPanel.removeAll();
        ArrayList<Map<String, String>> movements = dbService.getUpcomingMovements(currentVutId);

        if (movements.isEmpty()) {
            
            JLabel empty = new JLabel("No hay operaciones previstas..");
            
            empty.setBorder(new EmptyBorder(10, 10, 10, 10));
            empty.setForeground(Color.GRAY);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            movementsListPanel.add(empty);

        } else {
            
            for (Map<String, String> mov : movements) {
                movementsListPanel.add(createMovementRow(mov));
                movementsListPanel.add(Box.createRigidArea(new Dimension(0, 8))); 
            }
        }
        
        movementsListPanel.revalidate();
        movementsListPanel.repaint();

    }

    /**
     * Genera y actualiza el Gráfico de Barras de Ingresos.
     */
    private void updateRevenueChart() {
        
        revenueChartContainer.removeAll();
        Map<Integer, Double> data = dbService.getYearlyRevenueData(currentVutId, selectedYear);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (Map.Entry<Integer, Double> entry : data.entrySet()) {
            String monthName = Month.of(entry.getKey()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dataset.addValue(entry.getValue(), "Ingresos", monthName);
        }
        
        JFreeChart barChart = ChartFactory.createBarChart(
            "Total Facturado " + selectedYear, "Mes", "Cantidad (€)",
            dataset, PlotOrientation.VERTICAL, false, true, false
        );
        
        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(46, 204, 113));
        
        revenueChartContainer.add(new ChartPanel(barChart), BorderLayout.CENTER);
        revenueChartContainer.revalidate();

    }

    /**
     * Genera y actualiza el Gráfico de Pastel de Nacionalidades.
     */
    private void updateNationalityChart() {
        
        nationalityChartContainer.removeAll();
        Map<String, Integer> data = dbService.getNationalityStats(currentVutId);
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }
        
        JFreeChart pieChart = ChartFactory.createPieChart(
            "Procedencia de los clientes", dataset, true, true, false
        );
        
        PiePlot plot = (PiePlot) pieChart.getPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        
        nationalityChartContainer.add(new ChartPanel(pieChart), BorderLayout.CENTER);
        nationalityChartContainer.revalidate();
    }

    private void updateOccupancyChart() {
        
        occupancyChartContainer.removeAll();
        
        Map<Integer, Double> data = dbService.getYearlyOccupancyStats(currentVutId, selectedYear);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (Map.Entry<Integer, Double> entry : data.entrySet()) {
            String monthName = Month.of(entry.getKey()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dataset.addValue(entry.getValue(), "Ocupación %", monthName);
        }
        
        JFreeChart lineChart = ChartFactory.createLineChart(
            "Tasa de ocupación " + selectedYear, "mES", "Porcentaje (%)",
            dataset, PlotOrientation.VERTICAL, false, true, false
        );
        
        CategoryPlot plot = lineChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(52, 152, 219));
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        
        occupancyChartContainer.add(new ChartPanel(lineChart), BorderLayout.CENTER);
        occupancyChartContainer.revalidate();

    }
}