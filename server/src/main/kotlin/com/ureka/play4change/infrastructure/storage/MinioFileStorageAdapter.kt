package com.ureka.play4change.infrastructure.storage

import com.ureka.play4change.application.port.FileStoragePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI

@Component
class MinioFileStorageAdapter(private val props: MinioProperties) : FileStoragePort {

    private val log = LoggerFactory.getLogger(MinioFileStorageAdapter::class.java)

    private val client: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(URI.create(props.endpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.accessKey, props.secretKey)
                )
            )
            .region(Region.US_EAST_1)
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
            .build()
    }

    override fun uploadFile(key: String, bytes: ByteArray, contentType: String): String {
        ensureBucketExists(props.bucket)
        client.putObject(
            PutObjectRequest.builder()
                .bucket(props.bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(bytes.size.toLong())
                .build(),
            RequestBody.fromBytes(bytes)
        )
        log.debug("Uploaded {} bytes to {}/{}", bytes.size, props.bucket, key)
        return "${props.endpoint}/${props.bucket}/$key"
    }

    override fun downloadFile(key: String): ByteArray {
        return client.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(props.bucket)
                .key(key)
                .build()
        ).asByteArray()
    }

    override fun deleteFile(key: String) {
        client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(props.bucket)
                .key(key)
                .build()
        )
    }

    private fun ensureBucketExists(bucket: String) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
        } catch (ex: NoSuchBucketException) {
            log.info("Bucket '{}' not found — creating it", bucket)
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
        }
    }
}
