package com.inventory.vehicle.common.util;

import com.inventory.vehicle.dashboard.presentation.EmployeeCartItemRow;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

public class ReceiptPrinter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int RECEIPT_WIDTH = 42;
    private static final String DEFAULT_PRINTER_NAME = "Generic / Text Only";

    public static PrintResult printReceipt(String employeeName, LocalDate saleDate, List<EmployeeCartItemRow> cartItems,
            Long saleId) {
        try {
            String receiptContent = buildReceiptContent(employeeName, saleDate, cartItems, saleId);
            PrintService printer = findReceiptPrinter();
            if (printer == null) {
                return PrintResult.failed("No printer found.");
            }

            DocPrintJob job = printer.createPrintJob();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            byte[] receiptBytes = normalizeLineEndings(receiptContent).getBytes(StandardCharsets.UTF_8);
            Doc doc = new SimpleDoc(receiptBytes, flavor, null);
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            job.print(doc, attributes);
            return PrintResult.printed(printer.getName());
        } catch (Exception e) {
            System.err.println("Error printing receipt: " + e.getMessage());
            e.printStackTrace();
            return PrintResult.failed(e.getMessage());
        }
    }

    private static PrintService findReceiptPrinter() {
        String configuredPrinterName = System.getenv("RECEIPT_PRINTER_NAME");
        String preferredPrinterName = configuredPrinterName == null || configuredPrinterName.isBlank()
                ? DEFAULT_PRINTER_NAME
                : configuredPrinterName.trim();

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            if (printerNamesMatch(service.getName(), preferredPrinterName)) {
                return service;
            }
        }

        return PrintServiceLookup.lookupDefaultPrintService();
    }

    private static boolean printerNamesMatch(String actualName, String expectedName) {
        return normalizePrinterName(actualName).equals(normalizePrinterName(expectedName));
    }

    private static String normalizePrinterName(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String normalizeLineEndings(String receiptContent) {
        return receiptContent.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\r\n") + "\r\n\r\n";
    }

    private static String buildReceiptContent(String employeeName, LocalDate saleDate,
            List<EmployeeCartItemRow> cartItems, Long saleId) {
        StringBuilder sb = new StringBuilder();

        sb.append(center("R.M.R AUTO PARTS AND ACCESSORIES SHOP")).append("\n");
        sb.append(center("Guituan, Ipil, Zamboanga Sibugay")).append("\n");
        sb.append(center("SALES RECEIPT")).append("\n\n");

        sb.append("Transaction: ").append(saleId).append("\n");
        sb.append("Employee: ").append(trimToWidth(employeeName)).append("\n");
        sb.append("Date: ").append(DATE_FORMAT.format(saleDate)).append(" ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n\n");

        sb.append(line()).append("\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (EmployeeCartItemRow item : cartItems) {
            appendWrapped(sb, item.getProductName());
            appendWrapped(sb, item.getBrandName() + " " + item.getVehicleTypeName() + " " + item.getModelCode());
            if (item.getStockNumber() != null && !item.getStockNumber().isBlank()) {
                appendWrapped(sb, "Stock #: " + item.getStockNumber());
            }
            String quantityPrice = item.getQuantity() + " x " + printMoney(item.getPriceSold());
            sb.append(padRight(quantityPrice, 18))
                    .append(padLeft(printMoney(item.getTotalAmount()), 14))
                    .append("\n\n");

            totalAmount = totalAmount.add(item.getTotalAmount());
        }

        sb.append(line()).append("\n");
        sb.append(padRight("TOTAL", 18)).append(padLeft(printMoney(totalAmount), 14)).append("\n");
        sb.append(line()).append("\n");

        return sb.toString();
    }

    private static String printMoney(BigDecimal amount) {
        return MoneyFormat.peso(amount).replace("₱", "PHP ");
    }

    private static void appendWrapped(StringBuilder sb, String value) {
        String remaining = value == null ? "" : value.trim();
        if (remaining.isEmpty()) {
            return;
        }
        while (remaining.length() > RECEIPT_WIDTH) {
            int breakAt = remaining.lastIndexOf(' ', RECEIPT_WIDTH);
            if (breakAt <= 0) {
                breakAt = RECEIPT_WIDTH;
            }
            sb.append(remaining, 0, breakAt).append("\n");
            remaining = remaining.substring(breakAt).trim();
        }
        sb.append(remaining).append("\n");
    }

    private static String center(String value) {
        if (value.length() >= RECEIPT_WIDTH) {
            return value;
        }
        int leftPadding = (RECEIPT_WIDTH - value.length()) / 2;
        return " ".repeat(leftPadding) + value;
    }

    private static String line() {
        return "-".repeat(RECEIPT_WIDTH);
    }

    private static String trimToWidth(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 20 ? value : value.substring(0, 20);
    }

    private static String padRight(String value, int width) {
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        return value + " ".repeat(width - value.length());
    }

    private static String padLeft(String value, int width) {
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        return " ".repeat(width - value.length()) + value;
    }

    public record PrintResult(boolean printed, String printerName, String errorMessage) {
        static PrintResult printed(String printerName) {
            return new PrintResult(true, printerName, null);
        }

        static PrintResult failed(String errorMessage) {
            return new PrintResult(false, null, errorMessage);
        }
    }
}
