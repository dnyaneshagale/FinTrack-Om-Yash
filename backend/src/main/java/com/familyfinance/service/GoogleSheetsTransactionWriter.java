package com.familyfinance.service;

import com.familyfinance.domain.Ledger;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "sheets")
public class GoogleSheetsTransactionWriter {
    private final Sheets sheets;
    private final String spreadsheetId;
    public GoogleSheetsTransactionWriter(Sheets sheets, @Value("${google.sheets.spreadsheet-id}") String spreadsheetId) { this.sheets = sheets; this.spreadsheetId = spreadsheetId; }
    public void append(Ledger.Transaction transaction, String idempotencyKey) {
        try {
            var row = new java.util.ArrayList<Object>(List.of(transaction.id().toString(), transaction.accountId().toString(), transaction.type().name(), Long.toString(transaction.amountPaise()), transaction.date().toString(), transaction.description(), transaction.notes(), transaction.createdBy().toString(), OffsetDateTime.now(LedgerService.ZONE).toString(), idempotencyKey, transaction.needId() == null ? "" : transaction.needId().toString(), transaction.transferId() == null ? "" : transaction.transferId().toString(), ""));
            sheets.spreadsheets().values().append(spreadsheetId, "'Transactions'!A2", new ValueRange().setValues(List.of(row))).setValueInputOption("RAW").setInsertDataOption("INSERT_ROWS").execute();
        } catch (Exception exception) { throw new IllegalStateException("Google Sheets transaction write failed", exception); }
    }
}
