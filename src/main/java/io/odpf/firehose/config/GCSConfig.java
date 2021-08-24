package io.odpf.firehose.config;

import org.aeonbits.owner.Config;

/**
 * GCS_TYPE needs to be set as SINK_OBJECT_STORAGE or DLQ_OBJECT_STORAGE.
 */
public interface GCSConfig extends Config {

    @Key("${GCS_TYPE}_LOCAL_DIRECTORY")
    @DefaultValue("")
    String getGCSLocalDirectory();

    @Key("${GCS_TYPE}_GCS_GOOGLE_CLOUD_PROJECT_ID")
    String getGCloudProjectID();

    @Key("${GCS_TYPE}_GCS_BUCKET_NAME")
    String getGCSBucketName();

    @Key("${GCS_TYPE}_GCS_CREDENTIAL_PATH")
    String getGCSCredentialPath();
}