public final class AppointmentWorkflowTest {
    public static void main(String[] args) {
        String held = AppointmentWorkflow.notificationFor("appointment-1042", false);
        String sent = AppointmentWorkflow.notificationFor("appointment-1042", true);
        if (!held.equals("hold-notification:appointment-1042")) throw new AssertionError(held);
        if (!sent.equals("notify-care-team:appointment-1042")) throw new AssertionError(sent);
        System.out.println("appointment notification decision passed");
    }
}
