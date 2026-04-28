package org.example.e2etests;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class GoogleSheetsCopyTest extends E2eTestBase {

    @Autowired
    private GoogleSheetsTestHelper googleSheetsHelper;

    @Autowired
    private String testSpreadsheetId;

    @Test
    void shouldCreateSpreadsheetCopy() {
        log.info("Test spreadsheet ID: {}", testSpreadsheetId);

        assertThat(testSpreadsheetId).isNotBlank();
        try {
            var values = googleSheetsHelper.readSheet(testSpreadsheetId, "A1:Z1");
            log.info("Successfully read spreadsheet. Values: {}", values);
        } catch (Exception e) {
            log.error("Failed to read spreadsheet", e);
            throw new RuntimeException("Spreadsheet copy was not created properly", e);
        }
    }
}