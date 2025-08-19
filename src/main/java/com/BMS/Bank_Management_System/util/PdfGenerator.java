package com.BMS.Bank_Management_System.util;

import com.BMS.Bank_Management_System.dto.TransactionDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class PdfGenerator {

    public ByteArrayInputStream generateStatement(String accountNumber, List<TransactionDTO> transactions) {
        return generatePdf("Bank Statement", accountNumber, transactions);
    }

    public ByteArrayInputStream generateReceipt(String accountNumber, List<TransactionDTO> transactions) {
        return generatePdf("Transaction Receipt", accountNumber, transactions);
    }

    private ByteArrayInputStream generatePdf(String titleText, String accountNumber, List<TransactionDTO> transactions) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Account Number: " + accountNumber));
            document.add(new Paragraph("Generated on: " + new java.util.Date()));
            document.add(Chunk.NEWLINE);

            // Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 6, 3, 4});

            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);

            PdfPCell hcell;
            hcell = new PdfPCell(new Phrase("Date", headFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hcell);

            hcell = new PdfPCell(new Phrase("Description", headFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hcell);

            hcell = new PdfPCell(new Phrase("Amount", headFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hcell);

            hcell = new PdfPCell(new Phrase("Performed By", headFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hcell);

            for (TransactionDTO txn : transactions) {
                PdfPCell cell;

                // Date
                cell = new PdfPCell(new Phrase(txn.getTimestamp().toString()));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);

                // Description
                cell = new PdfPCell(new Phrase(txn.getDescription()));
                cell.setPaddingLeft(5);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);

                // Amount
                cell = new PdfPCell(new Phrase(String.valueOf(txn.getAmount())));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);

                // Performed By
                String performer = txn.getPerformedByUsername() != null ? txn.getPerformedByUsername() : "SYSTEM";
                cell = new PdfPCell(new Phrase(performer));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            document.add(table);
            document.close();

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
