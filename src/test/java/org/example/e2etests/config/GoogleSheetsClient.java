package org.example.e2etests.config;


import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GoogleSheetsClient {
    private final Sheets sheetsService;

    private static final String DEFAULT_SHEET = "Default sheet";

    public GoogleSheetsClient(String credentialsJson) throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));

        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("e2e-tests")
                .build();
    }

    public void copyToExistingSpreadsheet(String sourceSpreadsheetId, String targetSpreadsheetId) throws IOException {
        createDefaultSheet(targetSpreadsheetId);

        Spreadsheet source = sheetsService.spreadsheets().get(sourceSpreadsheetId).execute();
        List<Sheet> sourceSheets = source.getSheets();

        Spreadsheet target = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
        for (Sheet sheet : target.getSheets()) {
            if (!sheet.getProperties().getTitle().equals(DEFAULT_SHEET)) {
                DeleteSheetRequest deleteRequest = new DeleteSheetRequest();
                deleteRequest.setSheetId(sheet.getProperties().getSheetId());
                sheetsService.spreadsheets().batchUpdate(targetSpreadsheetId,
                        new BatchUpdateSpreadsheetRequest().setRequests(List.of(new Request().setDeleteSheet(deleteRequest)))).execute();
            }
        }

        for (Sheet sheet : sourceSheets) {
            sheetsService.spreadsheets().sheets().copyTo(sourceSpreadsheetId, sheet.getProperties().getSheetId(),
                    new CopySheetToAnotherSpreadsheetRequest().setDestinationSpreadsheetId(targetSpreadsheetId)).execute();
        }

        target = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
        List<Request> renameRequests = new ArrayList<>();
        Set<String> existing = target.getSheets().stream().map(s -> s.getProperties().getTitle()).collect(Collectors.toSet());
        for (Sheet sheet : target.getSheets()) {
            String name = sheet.getProperties().getTitle();
            if (name.contains("(копия)")) {
                String newName = name.replaceAll("\\s*\\(копия\\)\\s*\\d*", "").trim();
                if (!existing.contains(newName)) {
                    renameRequests.add(new Request().setUpdateSheetProperties(
                            new UpdateSheetPropertiesRequest().setProperties(new SheetProperties()
                                    .setSheetId(sheet.getProperties().getSheetId()).setTitle(newName)).setFields("title")));
                }
            }
        }
        if (!renameRequests.isEmpty()) {
            sheetsService.spreadsheets().batchUpdate(targetSpreadsheetId,
                    new BatchUpdateSpreadsheetRequest().setRequests(renameRequests)).execute();
        }
    }

    public List<List<Object>> readSheet(String spreadsheetId, String range) throws IOException {
        ValueRange result = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return result.getValues();
    }

    private void createDefaultSheet(String spreadsheetId) throws IOException {
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
        boolean hasDefaultSheet = spreadsheet.getSheets().stream()
                .anyMatch(sheet -> DEFAULT_SHEET.equals(sheet.getProperties().getTitle()));

        if (!hasDefaultSheet) {
            AddSheetRequest addSheetRequest = new AddSheetRequest();
            addSheetRequest.setProperties(new SheetProperties().setTitle(DEFAULT_SHEET));

            Request request = new Request().setAddSheet(addSheetRequest);
            sheetsService.spreadsheets().batchUpdate(spreadsheetId,
                    new BatchUpdateSpreadsheetRequest().setRequests(List.of(request))).execute();
        }
    }
}