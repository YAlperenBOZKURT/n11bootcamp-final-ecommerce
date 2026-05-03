package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.public-url:${minio.url}}")
    private String minioPublicUrl;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            // Allow public read so browser can load images directly
            // I add AWS S3 policy for simplicity
            String policy = """
                    {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
                    """.formatted(bucket);
            minioClient.setBucketPolicy(
                    io.minio.SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO bucket init failed", e);
        }
    }

    public String upload(MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String objectName = "products/" + UUID.randomUUID() + ext;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            return minioPublicUrl + "/" + bucket + "/" + objectName;
        } catch (Exception e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
