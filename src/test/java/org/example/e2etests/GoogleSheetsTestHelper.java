package org.example.e2etests;


import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GoogleSheetsTestHelper {
    private final Sheets sheetsService;

    public GoogleSheetsTestHelper(String credentialsJson) throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/drive"));

        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("e2e-tests")
                .build();
    }

    public void copyToExistingSpreadsheet(String sourceSpreadsheetId, String targetSpreadsheetId) throws IOException {
        // 1. Получаем ВСЕ листы из ИСХОДНОЙ таблицы
        Spreadsheet source = sheetsService.spreadsheets().get(sourceSpreadsheetId).execute();
        List<Sheet> sourceSheets = source.getSheets();
        List<String> sourceNames = sourceSheets.stream()
                .map(s -> s.getProperties().getTitle())
                .toList();

        // 2. Удаляем ВСЕ старые листы из целевой таблицы (кроме одного)
        Spreadsheet target = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
        List<Sheet> targetSheets = target.getSheets();

        while (targetSheets.size() > 1) {
            DeleteSheetRequest deleteRequest = new DeleteSheetRequest();
            deleteRequest.setSheetId(targetSheets.get(0).getProperties().getSheetId());

            BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest();
            batchRequest.setRequests(List.of(new Request().setDeleteSheet(deleteRequest)));
            sheetsService.spreadsheets().batchUpdate(targetSpreadsheetId, batchRequest).execute();

            targetSheets = sheetsService.spreadsheets().get(targetSpreadsheetId).execute().getSheets();
        }

        // 3. Копируем КАЖДЫЙ лист из источника
        for (Sheet sheet : sourceSheets) {
            Integer sourceSheetId = sheet.getProperties().getSheetId();

            CopySheetToAnotherSpreadsheetRequest copyRequest = new CopySheetToAnotherSpreadsheetRequest();
            copyRequest.setDestinationSpreadsheetId(targetSpreadsheetId);

            sheetsService.spreadsheets().sheets()
                    .copyTo(sourceSpreadsheetId, sourceSheetId, copyRequest)
                    .execute();

            log.info("Copied sheet '{}'", sheet.getProperties().getTitle());
        }

        // 4. Удаляем последний старый лист
        target = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
        for (Sheet sheet : target.getSheets()) {
            if (!sourceNames.contains(sheet.getProperties().getTitle()) && target.getSheets().size() > 1) {
                DeleteSheetRequest deleteRequest = new DeleteSheetRequest();
                deleteRequest.setSheetId(sheet.getProperties().getSheetId());

                BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest();
                batchRequest.setRequests(List.of(new Request().setDeleteSheet(deleteRequest)));
                sheetsService.spreadsheets().batchUpdate(targetSpreadsheetId, batchRequest).execute();
                break;
            }
        }

        // 5. **НОВОЕ: Переименовываем листы, убирая "(копия)"**
        target = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
        List<Request> renameRequests = new ArrayList<>();
        Set<String> existingNames = target.getSheets().stream()
                .map(s -> s.getProperties().getTitle())
                .collect(Collectors.toSet());

        for (Sheet sheet : target.getSheets()) {
            String name = sheet.getProperties().getTitle();
            if (name.contains("(копия)")) {
                String newName = name.replaceAll("\\s*\\(копия\\)\\s*\\d*", "").trim();

                // Переименовываем ТОЛЬКО если имя не занято
                if (!existingNames.contains(newName)) {
                    renameRequests.add(new Request().setUpdateSheetProperties(
                            new UpdateSheetPropertiesRequest()
                                    .setProperties(new SheetProperties()
                                            .setSheetId(sheet.getProperties().getSheetId())
                                            .setTitle(newName))
                                    .setFields("title")));
                }
            }
        }

        if (!renameRequests.isEmpty()) {
            sheetsService.spreadsheets().batchUpdate(targetSpreadsheetId,
                    new BatchUpdateSpreadsheetRequest().setRequests(renameRequests)).execute();
            log.info("Renamed {} sheets", renameRequests.size());
        }

        log.info("All {} sheets copied successfully", sourceSheets.size());
    }

    public List<List<Object>> readSheet(String spreadsheetId, String range) throws IOException {
        // Импортируем правильные классы для ValueRange
        com.google.api.services.sheets.v4.model.ValueRange result = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return result.getValues();
    }
}