package api_quality_platform.dto;

public class ApiResponse {

    private Long id;

    private String name;

    private String url;

    private String method;

    private String authType;

    public ApiResponse(Long id, String name, String url, String method, String authType) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.method = method;
        this.authType = authType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getAuthType() {
        return authType;
    }
}
