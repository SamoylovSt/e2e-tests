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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        // 2. Очищаем данные в целевой таблице (на всех листах)
        sheetsService.spreadsheets().values()
                .clear(targetSpreadsheetId, "A:ZZ", null)
                .execute();

        // 3. Копируем КАЖДЫЙ лист из источника в целевую таблицу
        for (Sheet sheet : sourceSheets) {
            Integer sourceSheetId = sheet.getProperties().getSheetId();

            CopySheetToAnotherSpreadsheetRequest copyRequest = new CopySheetToAnotherSpreadsheetRequest();
            copyRequest.setDestinationSpreadsheetId(targetSpreadsheetId);

            sheetsService.spreadsheets().sheets()
                    .copyTo(sourceSpreadsheetId, sourceSheetId, copyRequest)
                    .execute();

            log.info("Copied sheet '{}'", sheet.getProperties().getTitle());
        }

        // 4. Удаляем старые пустые листы (те, которые были изначально)
        Spreadsheet updatedTarget = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
        int targetSheetCount = updatedTarget.getSheets().size();
        int sourceSheetCount = sourceSheets.size();

        // Удаляем лишние листы (обычно это старый "Sheet1")
        while (targetSheetCount > sourceSheetCount) {
            // Находим лист для удаления (который не из источника)
            for (Sheet sheet : updatedTarget.getSheets()) {
                boolean isInSource = sourceSheets.stream()
                        .anyMatch(s -> s.getProperties().getTitle().equals(sheet.getProperties().getTitle()));

                if (!isInSource && updatedTarget.getSheets().size() > 1) {
                    DeleteSheetRequest deleteRequest = new DeleteSheetRequest();
                    deleteRequest.setSheetId(sheet.getProperties().getSheetId());

                    BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest();
                    batchRequest.setRequests(List.of(new Request().setDeleteSheet(deleteRequest)));
                    sheetsService.spreadsheets().batchUpdate(targetSpreadsheetId, batchRequest).execute();
                    break;
                }
            }

            // Обновляем информацию после удаления
            updatedTarget = sheetsService.spreadsheets().get(targetSpreadsheetId).execute();
            targetSheetCount = updatedTarget.getSheets().size();
        }

        log.info("All {} sheets copied from {} to {}", sourceSheetCount, sourceSpreadsheetId, targetSpreadsheetId);
    }

    public List<List<Object>> readSheet(String spreadsheetId, String range) throws IOException {
        // Импортируем правильные классы для ValueRange
        com.google.api.services.sheets.v4.model.ValueRange result = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return result.getValues();
    }
}