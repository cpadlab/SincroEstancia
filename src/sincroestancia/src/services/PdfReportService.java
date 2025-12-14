package sincroestancia.src.services;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.DefaultFontMapper;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jfree.chart.JFreeChart;

/**
 * Servicio encargado de la generación y exportación de informes en formato PDF.
 * * Utiliza la librería OpenPDF para la estructura del documento y se integra con
 * JFreeChart para renderizar gráficos estadísticos directamente dentro del reporte.
 * * Este servicio produce documentos estandarizados con portada, resumen de métricas
 * (KPIs) y análisis visual.
 * * @author Carlos Padilla Labella
 */
public class PdfReportService {

    private static final org.openpdf.text.Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final org.openpdf.text.Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 14, org.openpdf.text.Font.BOLD);
    private static final org.openpdf.text.Font TEXT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final org.openpdf.text.Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

    /**
     * Genera un informe PDF completo guardándolo en la ruta especificada.
     * * Pasos de implementación:
     * - Inicializa un documento PDF con tamaño A4.
     * - Si se proporciona una ruta de portada válida, carga la imagen, la escala proporcionalmente
     * para que ajuste al ancho de página y la inserta al inicio.
     * - Añade metadatos del informe: Título de la vivienda, periodo analizado y fecha de generación.
     * - Crea una tabla (PdfPTable) para estructurar los datos estadísticos (KPIs) en formato Clave-Valor.
     * - Renderizado de Gráficos: Itera sobre la lista de objetos JFreeChart. Para cada uno, crea un
     * 'PdfTemplate' (lienzo vectorial) y utiliza un objeto Graphics2D para dibujar el gráfico.
     * Esto asegura que los gráficos no pierdan calidad al hacer zoom en el PDF.
     * * @param filePath Ruta absoluta donde se guardará el archivo .pdf resultante.
     * @param vutName Nombre de la vivienda para el título.
     * @param monthYear Texto descriptivo del periodo (ej. "Año 2024").
     * @param coverPath Ruta a la imagen de portada de la vivienda.
     * @param stats Mapa de estadísticas (Clave -> Valor) para la tabla de resumen.
     * @param charts Lista de gráficos JFreeChart generados previamente para incluir en el reporte.
     * @return true si el PDF se generó correctamente, false si ocurrió algún error de E/S o de librería.
     */
    public boolean generateReport(String filePath, String vutName, String monthYear, String coverPath, Map<String, String> stats, List<JFreeChart> charts) {
        
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            if (coverPath != null && !coverPath.isEmpty()) {
                try {
                    
                    Image cover = Image.getInstance(coverPath);
                    float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                    
                    if (cover.getWidth() > maxWidth) {
                        float scaler = (maxWidth / cover.getWidth()) * 100;
                        cover.scalePercent(scaler);
                    } else {
                        cover.scaleToFit(maxWidth, 300);
                    }
                    
                    cover.setAlignment(Element.ALIGN_CENTER);
                    cover.setSpacingAfter(20);
                    document.add(cover);

                } catch (Exception e) {
                    System.err.println("No se pudo cargar la imagen de portada: " + e.getMessage());
                }
            }

            Paragraph title = new Paragraph("Informe: " + vutName, TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Periodo: " + monthYear, SUBTITLE_FONT);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(20);
            document.add(period);

            document.add(new Paragraph("Fecha de reporte: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()), TEXT_FONT));
            document.add(new Paragraph(" ", TEXT_FONT));

            document.add(new Paragraph("Resumen de Métricas", SUBTITLE_FONT));
            document.add(new Paragraph(" ", TEXT_FONT));

            PdfPTable table = new PdfPTable(2); 
            table.setWidthPercentage(100);
            
            for (Map.Entry<String, String> entry : stats.entrySet()) {
                addCellToTable(table, entry.getKey(), true);
                addCellToTable(table, entry.getValue(), false);
            }
            
            document.add(table);
            document.add(new Paragraph(" ", TEXT_FONT));

            if (charts != null && !charts.isEmpty()) {
                document.add(new Paragraph("Análisis Gráfico", SUBTITLE_FONT));
                document.add(new Paragraph(" ", TEXT_FONT));
                
                int width = 500;
                int height = 300;

                for (JFreeChart chart : charts) {
                    PdfContentByte contentByte = writer.getDirectContent();
                    PdfTemplate template = contentByte.createTemplate(width, height);
                    
                    Graphics2D g2d = template.createGraphics(width, height, new DefaultFontMapper());
                    Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
                    
                    chart.draw(g2d, r2d);
                    g2d.dispose();
                    
                    Image chartImage = Image.getInstance(template);
                    chartImage.setAlignment(Element.ALIGN_CENTER);
                    chartImage.setSpacingAfter(15);
                    document.add(chartImage);
                }
            }

            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Método auxiliar para añadir celdas a la tabla de estadísticas con el estilo correcto.
     * * Aplica padding, alineación y un color de fondo gris claro si la celda es una cabecera (Key).
     * * @param table La tabla PdfPTable a la que se añadirá la celda.
     * @param text El contenido textual de la celda.
     * @param isHeader true para aplicar estilo de etiqueta (negrita, fondo gris), false para valor (texto plano).
     */
    private void addCellToTable(PdfPTable table, String text, boolean isHeader) {
        
        PdfPCell cell = new PdfPCell(new Phrase(text, isHeader ? HEADER_FONT : TEXT_FONT));
        
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        if (isHeader) {
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        }
        
        table.addCell(cell);

    }
}