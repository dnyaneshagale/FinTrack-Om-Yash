package com.familyfinance.service;

import com.familyfinance.domain.Ledger;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "sheets")
public class GoogleSheetsFundingWriter {
    private final Sheets sheets;
    private final String spreadsheetId;
    public GoogleSheetsFundingWriter(Sheets sheets, @Value("${google.sheets.spreadsheet-id}") String spreadsheetId) { this.sheets = sheets; this.spreadsheetId = spreadsheetId; }
    public void append(Ledger.Funding funding) {
        try {
            var row = new java.util.ArrayList<Object>(List.of(funding.id().toString(), funding.needId().toString(), funding.transactionId().toString(), Long.toString(funding.amountPaise()), funding.fundedBy().toString(), funding.fundedAt().toString(), funding.notes()));
            sheets.spreadsheets().values().append(spreadsheetId, "'NeedFunding'!A2", new ValueRange().setValues(List.of(row))).setValueInputOption("RAW").setInsertDataOption("INSERT_ROWS").execute();
        } catch (Exception exception) { throw new IllegalStateException("Google Sheets funding write failed; transaction requires recovery", exception); }
    }
}
