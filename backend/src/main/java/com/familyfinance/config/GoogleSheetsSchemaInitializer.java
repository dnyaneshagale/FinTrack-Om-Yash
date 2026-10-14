package com.familyfinance.config;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "sheets")
public class GoogleSheetsSchemaInitializer {
    private static final Map<String, List<String>> SCHEMA = Map.of(
        "Users", List.of("userId", "username", "displayName", "role", "active", "createdAt", "updatedAt"),
        "Accounts", List.of("accountId", "ownerUserId", "currency", "openingBalancePaise", "openingDate", "active", "createdAt", "updatedAt"),
        "Transactions", List.of("transactionId", "accountId", "type", "amountPaise", "transactionDate", "description", "notes", "createdBy", "createdAt", "idempotencyKey", "needId", "transferId", "reversalOfTransactionId"),
        "Needs", List.of("needId", "requestedBy", "cause", "amountPaise", "requiredByDate", "notes", "status", "createdAt", "updatedAt", "reviewedBy", "reviewedAt"),
        "NeedFunding", List.of("fundingId", "needId", "transactionId", "amountPaise", "fundedBy", "fundedAt", "notes"),
        "AuditLog", List.of("auditId", "actorUserId", "action", "entityType", "entityId", "timestamp", "details"),
        "Reconciliations", List.of("reconciliationId", "accountId", "reconciliationDate", "actualBalancePaise", "calculatedBalancePaise", "differencePaise", "submittedBy", "createdAt", "notes"));

    private final Sheets sheets;
    private final String spreadsheetId;
    public GoogleSheetsSchemaInitializer(Sheets sheets, @Value("${google.sheets.spreadsheet-id}") String spreadsheetId) { this.sheets = sheets; this.spreadsheetId = spreadsheetId; }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void initialize() throws Exception {
        if (spreadsheetId.isBlank()) throw new IllegalStateException("GOOGLE_SHEETS_SPREADSHEET_ID is required when STORAGE_MODE=sheets");
        Spreadsheet existing = sheets.spreadsheets().get(spreadsheetId).setFields("sheets.properties.title").execute();
        var titles = existing.getSheets().stream().map(sheet -> sheet.getProperties().getTitle()).collect(java.util.stream.Collectors.toSet());
        var requests = new ArrayList<Request>();
        for (var title : SCHEMA.keySet()) if (!titles.contains(title)) requests.add(new Request().setAddSheet(new AddSheetRequest().setProperties(new SheetProperties().setTitle(title))));
        if (!requests.isEmpty()) sheets.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();
        for (var entry : SCHEMA.entrySet()) sheets.spreadsheets().values().update(spreadsheetId, "'" + entry.getKey() + "'!A1", new ValueRange().setValues(List.of(new ArrayList<Object>(entry.getValue())))).setValueInputOption("RAW").execute();
    }
}
