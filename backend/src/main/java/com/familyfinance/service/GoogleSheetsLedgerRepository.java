package com.familyfinance.service;

import com.familyfinance.domain.Ledger;
import com.familyfinance.domain.LedgerRepository;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Component
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "sheets")
public class GoogleSheetsLedgerRepository implements LedgerRepository {
    private final Sheets sheets;
    private final String spreadsheetId;
    public GoogleSheetsLedgerRepository(Sheets sheets, @Value("${google.sheets.spreadsheet-id}") String spreadsheetId) { this.sheets = sheets; this.spreadsheetId = spreadsheetId; }

    @Override public List<Ledger.User> users() { return read("Users").stream().skip(1).map(row -> new Ledger.User(uuid(row, 0), text(row, 1), text(row, 2), Ledger.Role.valueOf(text(row, 3)))).toList(); }
    @Override public List<Ledger.Account> accounts() { return read("Accounts").stream().skip(1).map(row -> new Ledger.Account(uuid(row, 0), uuid(row, 1), text(row, 1), Long.parseLong(text(row, 3)), LocalDate.parse(text(row, 4)))).toList(); }
    @Override public List<Ledger.Transaction> transactions(UUID accountId) { return read("Transactions").stream().skip(1).filter(row -> accountId.toString().equals(text(row, 1))).map(this::transaction).toList(); }
    @Override public Optional<Ledger.Need> need(UUID needId) { return read("Needs").stream().skip(1).filter(row -> needId.toString().equals(text(row, 0))).findFirst().map(this::need); }
    @Override public Ledger.Transaction appendTransaction(Ledger.Transaction transaction, String idempotencyKey) { new GoogleSheetsTransactionWriter(sheets, spreadsheetId).append(transaction, idempotencyKey); return transaction; }
    @Override public Ledger.Need saveNeed(Ledger.Need need) { throw new UnsupportedOperationException("Need updates require a verified multi-row Sheets operation"); }

    private List<List<Object>> read(String tab) { try { ValueRange values = sheets.spreadsheets().values().get(spreadsheetId, "'" + tab + "'!A1:Z").execute(); return values.getValues() == null ? List.of() : values.getValues(); } catch (GoogleJsonResponseException exception) { if (exception.getStatusCode() == 403) throw new IllegalStateException("Google Sheets permission denied for " + tab + ". Share the spreadsheet with the client_email from GOOGLE_SERVICE_ACCOUNT_JSON and verify GOOGLE_SHEETS_SPREADSHEET_ID.", exception); throw new IllegalStateException("Google Sheets read failed for " + tab + " (HTTP " + exception.getStatusCode() + ")", exception); } catch (Exception exception) { throw new IllegalStateException("Google Sheets read failed for " + tab, exception); } }
    private Ledger.Transaction transaction(List<Object> row) { return new Ledger.Transaction(uuid(row, 0), uuid(row, 1), Ledger.Type.valueOf(text(row, 2)), Long.parseLong(text(row, 3)), LocalDate.parse(text(row, 4)), text(row, 5), text(row, 6), uuid(row, 7), nullableUuid(row, 10), nullableUuid(row, 11)); }
    private Ledger.Need need(List<Object> row) { return new Ledger.Need(uuid(row, 0), uuid(row, 1), text(row, 1), text(row, 2), Long.parseLong(text(row, 3)), LocalDate.parse(text(row, 4)), text(row, 5), Ledger.NeedStatus.valueOf(text(row, 6)), 0, OffsetDateTime.parse(text(row, 7))); }
    private static String text(List<Object> row, int index) { return row.size() > index && row.get(index) != null ? String.valueOf(row.get(index)) : ""; }
    private static UUID uuid(List<Object> row, int index) { return UUID.fromString(text(row, index)); }
    private static UUID nullableUuid(List<Object> row, int index) { return text(row, index).isBlank() ? null : uuid(row, index); }
}
