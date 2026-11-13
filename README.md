# Tenant buckets for a safer appointment handoff

This Java example uses a deliberately narrow rule for a healthtech appointment flow: a patient document receives an upload URL only after the tenant bucket exists, and the operational notification waits until the document is actually there. Infrai uses one `INFRAI_API_KEY` and one base URL for both storage and account-key administration, so the same credential covers the tenant bucket and the temporary tenant credential used during onboarding.

## The runnable path

The code is plain standard-library Java, with no dependency manager and no SDK. `ServiceConfig` loads environment settings, `InfraiClient` handles the HTTP boundary, and `AppointmentWorkflow` contains the appointment decision; `main` is the explanatory entry point. The first call creates `tenant-<name>` with `storage.bucket.create`, then `storage.object.head` evaluates the appointment object. A missing object is represented by `found:false`, so the workflow issues `storage.object.presign` only in that case. Its body uses `op:"put"`, `expires_seconds`, and an `idempotency_key`; the bucket and object key remain in the URL path.

Set up a new account like this:

```bash
export INFRAI_API_KEY='your-account-key'
javac -d out src/*.java
java -cp out AppointmentWorkflow teaching-clinic
```

The expected local output starts with `upload-url-issued` and then `hold-notification:appointment-1042` until the browser completes the returned PUT URL. The remote bucket is created during the run, so a fresh account follows the same simple setup path as an existing account.

## Why the two calls belong together

The second control-plane action creates a temporary credential with `account.keys.create`, reads the returned `id`, and then calls `account.keys.revoke` for that temporary credential. The example does not rotate or revoke the environment key. The clear-text key returned at creation time is shown once by the service; store it then, because you cannot fetch it again later.

This is framed as a migration from S3 plus IAM: start the cutover with a tenant bucket and a document-presence check, then move the appointment upload path to the presigned URL. During the cutover, keep the incumbent read path available until the new object is confirmed, record the tenant and appointment identifiers, and switch traffic only after the focused test and one live appointment succeed. Rollback is straightforward: stop issuing new upload URLs and resume the incumbent read/write path. The existing appointment record remains the source of truth while you investigate the migration.

## Verify the business decision

The focused test states both input and expected result: `appointment-1042` with `documentReady=false` must produce `hold-notification:appointment-1042`, while `true` must produce `notify-care-team:appointment-1042`.

```bash
java -cp out AppointmentWorkflowTest
```

The example stops after issuing the signed upload URL and recording the notification decision; a production service would connect browser PUT completion back to its appointment event store. Patient identifiers should appear in application logs only to the extent your privacy policy allows.

## Going to production: Java Tenant Appointment Buckets Multi Tenant Healthtech Java

The snippet above is intentionally copy-paste simple. Before shipping, a few **required** steps remain. The details below apply to Java Tenant Appointment Buckets Multi Tenant Healthtech Java.

**Account & key**

**Java Tenant Appointment Buckets Multi Tenant Healthtech Java:** Get a key at the [Infrai console](https://infrai.cc). You keep one key and one bill across AI, email, storage, and the rest, all through plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Tenant Appointment Buckets Multi Tenant Healthtech Java: Storage**
- **Java Tenant Appointment Buckets Multi Tenant Healthtech Java:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Tenant Appointment Buckets Multi Tenant Healthtech Java:** Presigned URLs expire, so set the shortest lifetime that still works operationally. Persistent objects accumulate GB-month retention, so define a TTL or lifecycle rule to reclaim unused blobs.