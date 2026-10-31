import java.io.IOException;

public final class AppointmentWorkflow {
    private final InfraiClient infrai;
    private final String bucket;

    public AppointmentWorkflow(InfraiClient infrai, String bucket) {
        this.infrai = infrai;
        this.bucket = bucket;
    }

    public String preparePatientDocument(String appointmentId) throws IOException, InterruptedException {
        infrai.bucketCreate(bucket);
        String objectKey = "appointments/" + appointmentId + "/clinical-note.pdf";
        String head = infrai.objectHead(bucket, objectKey);
        if (InfraiClient.found(head)) return "existing-document";
        infrai.objectPresign(bucket, objectKey);
        return "upload-url-issued";
    }

    public static String notificationFor(String appointmentId, boolean documentReady) {
        if (!documentReady) return "hold-notification:" + appointmentId;
        return "notify-care-team:" + appointmentId;
    }

    public static void main(String[] args) throws Exception {
        String tenant = args.length == 0 ? "teaching-clinic" : args[0];
        ServiceConfig config = ServiceConfig.fromEnvironment(tenant);
        String bucket = "tenant-" + config.tenantName();
        InfraiClient client = new InfraiClient(config.apiKey());
        AppointmentWorkflow workflow = new AppointmentWorkflow(client, bucket);
        String result = workflow.preparePatientDocument("appointment-1042");
        System.out.println(result);
        System.out.println(notificationFor("appointment-1042", "existing-document".equals(result)));

        String temporary = client.keyCreate("temporary-" + tenant);
        String temporaryId = InfraiClient.value(temporary, "id");
        if (!temporaryId.isBlank()) client.keyRevoke(temporaryId);
    }
}
