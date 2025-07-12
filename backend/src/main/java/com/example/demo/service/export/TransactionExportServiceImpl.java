package com.example.demo.service.export;

import com.example.demo.model.customer.Customer;
import com.example.demo.model.customer.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

// iText PDF
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

@Service
public class TransactionExportServiceImpl implements TransactionExportService {

    @Override
    public ByteArrayInputStream transactionsToCsv(List<Transaction> transactions) {
        final CSVFormat format = CSVFormat.Builder.create()
                .setHeader("ID", "Timestamp", "Amount", "Type", "Description", "Status", "CustomerId", "RecipientId")
                .build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(new OutputStreamWriter(out)), format)) {

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Transaction tx : transactions) {
                csvPrinter.printRecord(
                        tx.getId(),
                        tx.getTimestamp() != null ? dtf.format(tx.getTimestamp()) : "",
                        tx.getAmount() != null ? tx.getAmount().toPlainString() : "",
                        tx.getType(),
                        tx.getDescription(),
                        tx.getStatus(),
                        tx.getCustomerId(),
                        tx.getRecipientId()
                );
            }
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    @Override
    public ByteArrayInputStream transactionsToPdf(List<Transaction> transactions, Customer customer) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Transaction Report", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Customer info
            Font fontInfo = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph custInfo = new Paragraph(
                    "Customer: " + (customer != null ? customer.getFullName() : "") +
                    "\nEmail: " + (customer != null ? customer.getEmail() : ""),
                    fontInfo
            );
            custInfo.setSpacingAfter(10);
            document.add(custInfo);

            // Table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Table header
            String[] headers = {"ID", "Timestamp", "Amount", "Type", "Description", "Status", "CustomerId", "RecipientId"};
            for (String h : headers) {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                header.setPhrase(new Phrase(h));
                table.addCell(header);
            }

            // Table rows
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Transaction tx : transactions) {
                table.addCell(tx.getId() != null ? tx.getId().toString() : "");
                table.addCell(tx.getTimestamp() != null ? dtf.format(tx.getTimestamp()) : "");
                table.addCell(tx.getAmount() != null ? tx.getAmount().toPlainString() : "");
                table.addCell(tx.getType() != null ? tx.getType() : "");
                table.addCell(tx.getDescription() != null ? tx.getDescription() : "");
                table.addCell(tx.getStatus() != null ? tx.getStatus() : "");
                table.addCell(tx.getCustomerId() != null ? tx.getCustomerId().toString() : "");
                table.addCell(tx.getRecipientId() != null ? tx.getRecipientId().toString() : "");
            }

            document.add(table);
            document.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }
}