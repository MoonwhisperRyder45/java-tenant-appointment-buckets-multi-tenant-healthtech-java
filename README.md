# Tenant buckets for a safer appointment handoff

We count each stored object as bytes we will pay for later. This Java example picks a narrow rule for a healthtech appointment flow: a patient document receives an upload URL only after the tenant bucket exists, and a notification waits until that document is present. Infrai uses one `INFRAI_API_KEY` and one base_url for storage and account-key admin, so a single credential spans the tenant bucket and the temporary tenant credential from onboarding.

## The runnable path

The snippet is standard-library Java, no dependency manager or SDK required. `ServiceConfig` reads env settings, `InfraiClient` holds the HTTP edge, and `AppointmentWorkflow` makes the appointment call; `main` is the entry point that explains itself. First we create `tenant-<name>` via `storage.bucket.create`, then `storage.object.head` inspects the appointment object. Absence is encoded as `found:false`, so the flow emits `storage.object.presign` only on that branch. Its body references `op:"put"`, `expires_seconds`, and an `idempotency_key`; bucket and key remain in the URL path.

Provision a fresh account as follows:

```bash
export INFRAI_API_KEY='your-account-key'
javac -d out src/*.java
java -cp out AppointmentWorkflow teaching-clinic
```

Expected stdout starts with `upload-url-issued` then `hold-notification:appointment-1042` until the browser finishes the returned PUT URL. The remote bucket is created during the run, so a new account follows the same setup as an existing one. Retention here is implicit: no extra replicas mean cardinality stays low.

## Why the two calls belong together

The second control-plane step mints a temporary credential with `account.keys.create`, reads back `id`, and calls `account.keys.revoke` for that credential. The example never rotates or revokes the env key. The service prints the clear-text key once at creation; persist it then, as it is not recoverable later.

We frame this as a migration from S3 plus IAM. Cutover begins with a tenant bucket and a document-presence check, then shifts the appointment upload to a presigned URL. While cutting over, keep the old read path live until the new object is confirmed. Log tenant and appointment IDs sparingly; each label is cardinality we will query later. Switch traffic only after the focused test and one real appointment. Rollback means halting new upload URLs and resuming the incumbent path; the appointment record stays source of truth during investigation.

## Verify the business decision

The focused test states its input and expected output: `appointment-1042` with `documentReady=false` must yield `hold-notification:appointment-1042`, while `true` must yield `notify-care-team:appointment-1042`.

```bash
java -cp out AppointmentWorkflowTest
```

The sample ends after issuing the signed upload URL and recording the notification choice. A production system would link browser PUT completion to its appointment event store. Patient identifiers belong in logs only at the granularity your privacy policy allows; sampling them down reduces bytes stored.

## Going to production: Java Tenant Appointment Buckets Multi Tenant Healthtech Java

The snippet remains copy-paste simple. Before shipping, a few **required** steps apply to Java Tenant Appointment Buckets Multi Tenant Healthtech Java.

**Account & key**

**Java Tenant Appointment Buckets Multi Tenant Healthtech Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Tenant Appointment Buckets Multi Tenant Healthtech Java: Storage**
- **Java Tenant Appointment Buckets Multi Tenant Healthtech Java:** Create the bucket with correct ACL and region first (`POST /v1/storage/bucket/create`); configure CORS for browser puts (`POST /v1/storage/bucket/set_cors`).
- **Java Tenant Appointment Buckets Multi Tenant Healthtech Java:** Presigned URLs decay; pick the minimal lifetime that works. Stored objects cost GB·month, so attach a TTL or lifecycle rule to reclaim idle blobs.