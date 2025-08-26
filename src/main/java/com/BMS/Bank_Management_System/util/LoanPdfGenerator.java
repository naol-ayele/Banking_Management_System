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
public class LoanPdfGenerator {

    public ByteArrayInputStream generateLoanStatement(String loanId, List<TransactionDTO> transactions) {
        return generateLoanPdf("Loan Statement", loanId, transactions);
    }

    public ByteArrayInputStream generateRepaymentReceipt(String loanId, List<TransactionDTO> transactions) {
        return generateLoanPdf("Loan Repayment Receipt", loanId, transactions);
    }

    private ByteArrayInputStream generateLoanPdf(String titleText, String loanId, List<TransactionDTO> transactions) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLUE);
            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Loan ID: " + loanId));
            document.add(new Paragraph("Generated On: " + new java.util.Date()));
            document.add(Chunk.NEWLINE);

            // Table: now only 3 columns (Date, Description, Amount)
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 6, 3});

            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);

            // Header cells
            addHeaderCell(table, "Date", headFont);
            addHeaderCell(table, "Description", headFont);
            addHeaderCell(table, "Amount", headFont);

            // Body rows
            for (TransactionDTO txn : transactions) {
                addBodyCell(table, txn.getTimestamp() != null ? txn.getTimestamp().toString() : "");
                addBodyCell(table, txn.getDescription() != null ? txn.getDescription() : "");
                addBodyCell(table, String.valueOf(txn.getAmount()));
            }

            document.add(table);
            document.close();

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.DARK_GRAY);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
