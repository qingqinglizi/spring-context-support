package org.springframework.ui.jasperreports;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;

public abstract class JasperReportsUtils
{
    public static JRDataSource convertReportData(Object value)
            throws IllegalArgumentException
    {
        if ((value instanceof JRDataSource)) {
            return (JRDataSource)value;
        }
        if ((value instanceof Collection)) {
            return new JRBeanCollectionDataSource((Collection)value);
        }
        if ((value instanceof Object[])) {
            return new JRBeanArrayDataSource((Object[])value);
        }
        throw new IllegalArgumentException("Value [" + value + "] cannot be converted to a JRDataSource");
    }

    public static void render(JRExporter exporter, JasperPrint print, Writer writer)
            throws JRException
    {
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setParameter(JRExporterParameter.OUTPUT_WRITER, writer);
        exporter.exportReport();
    }

    public static void render(JRExporter exporter, JasperPrint print, OutputStream outputStream)
            throws JRException
    {
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
        exporter.exportReport();
    }

    public static void renderAsCsv(JasperReport report, Map<String, Object> parameters, Object reportData, Writer writer)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        render(new JRCsvExporter(), print, writer);
    }

    public static void renderAsCsv(JasperReport report, Map<String, Object> parameters, Object reportData, Writer writer, Map<JRExporterParameter, Object> exporterParameters)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setParameters(exporterParameters);
        render(exporter, print, writer);
    }

    public static void renderAsHtml(JasperReport report, Map<String, Object> parameters, Object reportData, Writer writer)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        render(new JRHtmlExporter(), print, writer);
    }

    public static void renderAsHtml(JasperReport report, Map<String, Object> parameters, Object reportData, Writer writer, Map<JRExporterParameter, Object> exporterParameters)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        JRHtmlExporter exporter = new JRHtmlExporter();
        exporter.setParameters(exporterParameters);
        render(exporter, print, writer);
    }

    public static void renderAsPdf(JasperReport report, Map<String, Object> parameters, Object reportData, OutputStream stream)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        render(new JRPdfExporter(), print, stream);
    }

    public static void renderAsPdf(JasperReport report, Map<String, Object> parameters, Object reportData, OutputStream stream, Map<JRExporterParameter, Object> exporterParameters)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setParameters(exporterParameters);
        render(exporter, print, stream);
    }

    public static void renderAsXls(JasperReport report, Map<String, Object> parameters, Object reportData, OutputStream stream)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        render(new JRXlsExporter(), print, stream);
    }

    public static void renderAsXls(JasperReport report, Map<String, Object> parameters, Object reportData, OutputStream stream, Map<JRExporterParameter, Object> exporterParameters)
            throws JRException
    {
        JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
        JRXlsExporter exporter = new JRXlsExporter();
        exporter.setParameters(exporterParameters);
        render(exporter, print, stream);
    }
}
