public record ServiceConfig(String apiKey, String tenantName) {
    public static ServiceConfig fromEnvironment(String tenantName) {
        return new ServiceConfig(System.getenv("INFRAI_API_KEY"), tenantName);
    }
}
