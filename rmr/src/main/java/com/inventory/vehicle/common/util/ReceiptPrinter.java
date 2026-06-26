package com.inventory.vehicle.common.util;

import com.inventory.vehicle.dashboard.presentation.EmployeeCartItemRow;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.print.PrinterJob;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ReceiptPrinter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void printReceipt(String employeeName, LocalDate saleDate, List<EmployeeCartItemRow> cartItems,
            Long saleId) {
        try {
            String receiptContent = buildReceiptContent(employeeName, saleDate, cartItems, saleId);
            PrinterJob job = PrinterJob.createPrinterJob();

            if (job != null && job.showPrintDialog(null)) {
                TextFlow textFlow = createReceiptTextFlow(receiptContent);
                boolean success = job.printPage(textFlow);
                if (success) {
                    job.endJob();
                }
            }
        } catch (Exception e) {
            System.err.println("Error printing receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String buildReceiptContent(String employeeName, LocalDate saleDate,
            List<EmployeeCartItemRow> cartItems, Long saleId) {
        StringBuilder sb = new StringBuilder();

        sb.append("RMR SURPLUS\n");
        sb.append("SALES RECEIPT\n\n");

        sb.append("Transaction ID: ").append(saleId).append("\n");
        sb.append("Employee: ").append(employeeName).append("\n");
        sb.append("Date: ").append(DATE_FORMAT.format(saleDate)).append(" ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n\n");

        sb.append("─────────────────────────────────────\n\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (EmployeeCartItemRow item : cartItems) {
            sb.append(item.getProductName()).append("\n");
            sb.append("  ").append(item.getBrandName()).append(" | ").append(item.getVehicleTypeName())
                    .append(" | ").append(item.getModelCode()).append("\n");
            sb.append("  Stock #: ").append(item.getStockNumber()).append("\n");
            sb.append("  ").append(item.getQuantity()).append("x ")
                    .append(MoneyFormat.peso(item.getPriceSold())).append(" = ")
                    .append(MoneyFormat.peso(item.getTotalAmount())).append("\n\n");

            totalAmount = totalAmount.add(item.getTotalAmount());
        }

        sb.append("─────────────────────────────────────\n");
        sb.append("TOTAL: ").append(MoneyFormat.peso(totalAmount)).append("\n");
        sb.append("─────────────────────────────────────\n");

        return sb.toString();
    }

    private static TextFlow createReceiptTextFlow(String content) {
        TextFlow textFlow = new TextFlow();
        Text text = new Text(content);
        text.setFont(new Font("Courier New", 10));
        textFlow.getChildren().add(text);
        textFlow.setPrefWidth(400);
        textFlow.setLineSpacing(2);
        return textFlow;
    }
}
