package org.example.e2etests.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CreateProjectRequest {
    @JsonProperty("author_telegram_user_id")
    private Long authorTelegramUserId;
    @JsonProperty("github_repository_url")
    private String githubRepositoryUrl;
    @JsonProperty("programming_language")
    private String programmingLanguage;
    @JsonProperty("roadmap_project")
    private String roadmapProject;
    @JsonProperty("author_telegram_username")
    private String authorTelegramUsername;
    @JsonProperty("added_timestamp")
    private String addedTimestamp;
    @JsonProperty("project_source_type")
    private String projectSourceType;
}
