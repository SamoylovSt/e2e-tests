package org.example.e2etests;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docker.image.tags")
class DockerImageTagsProperties {

    private String common;
    private String gateway;
    private String authService;
    private String dataImporter;
    private String profileService;
    private String projectService;
    private String mentorService;
    private String jobMarketAnalyticsService;

    public void setCommon(String common) { this.common = common; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public void setAuthService(String authService) { this.authService = authService; }
    public void setDataImporter(String dataImporter) { this.dataImporter = dataImporter; }
    public void setProfileService(String profileService) { this.profileService = profileService; }
    public void setProjectService(String projectService) { this.projectService = projectService; }
    public void setMentorService(String mentorService) { this.mentorService = mentorService; }
    public void setJobMarketAnalyticsService(String jobMarketAnalyticsService) { this.jobMarketAnalyticsService = jobMarketAnalyticsService; }

    public String gateway() { return resolve(gateway, "GATEWAY_DOCKER_IMAGE_TAG"); }
    public String authService() { return resolve(authService, "AUTH_SERVICE_DOCKER_IMAGE_TAG"); }
    public String dataImporter() { return resolve(dataImporter, "DATA_IMPORTER_DOCKER_IMAGE_TAG"); }
    public String profileService() { return resolve(profileService, "PROFILE_SERVICE_DOCKER_IMAGE_TAG"); }
    public String projectService() { return resolve(projectService, "PROJECT_SERVICE_DOCKER_IMAGE_TAG"); }
    public String mentorService() { return resolve(mentorService, "MENTOR_SERVICE_DOCKER_IMAGE_TAG"); }
    public String jobMarketAnalyticsService() { return resolve(jobMarketAnalyticsService, "JOB_MARKET_ANALYTICS_SERVICE_DOCKER_IMAGE_TAG"); }

    private String resolve(String serviceTag, String varName) {
        if (serviceTag != null && !serviceTag.isBlank()) return serviceTag;
        if (common != null && !common.isBlank()) return common;
        throw new IllegalStateException(
            "Не задана ни переменная " + varName + ", ни TESTCONTAINER_DOCKER_IMAGES_TAG"
        );
    }
}
