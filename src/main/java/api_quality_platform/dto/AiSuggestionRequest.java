package api_quality_platform.dto;

public class AiSuggestionRequest {

    private String sampleResponse;
    private String openApiSchema;

    public String getSampleResponse() {
        return sampleResponse;
    }

    public void setSampleResponse(String sampleResponse) {
        this.sampleResponse = sampleResponse;
    }

    public String getOpenApiSchema() {
        return openApiSchema;
    }

    public void setOpenApiSchema(String openApiSchema) {
        this.openApiSchema = openApiSchema;
    }
}
