package com.familyfinance.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class GoogleSheetsConfig {
    @Bean
    @ConditionalOnProperty(name = "app.storage.mode", havingValue = "sheets")
    Sheets googleSheets(@Value("${google.service-account-json}") String serviceAccountJson) throws Exception {
        if (serviceAccountJson.isBlank()) throw new IllegalStateException("GOOGLE_SERVICE_ACCOUNT_JSON is required when STORAGE_MODE=sheets");
        var credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))).createScoped(List.of(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials)).setApplicationName("Fintrack Om Yash").build();
    }
}
