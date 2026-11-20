package org.twiglau.hellominio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.twiglau.hellominio.config.MinioConfig;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioConfig config;

    /** 建桶（已存在则跳过） */
    public void ensureBucket() {
        String bucket = config.getBucket();
        try {
            s3Client.headBucket(b -> b.bucket(bucket));
            System.out.println("[桶] 已存在: " + bucket);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(b -> b.bucket(bucket));
            System.out.println("[桶] 已创建: " + bucket);
        }
    }

    /** 上传：字符串当作对象内容写入 */
    public void upload(String key, String content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(config.getBucket())
                        .key(key)
                        .contentType("text/plain; charset=utf-8")
                        .build(),
                RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));
        System.out.println("[上传] " + key);
    }

    /** 下载：把对象内容读回字符串 */
    public String download(String key) {
        ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(config.getBucket())
                        .key(key)
                        .build());
        String content = bytes.asUtf8String();
        System.out.println("[下载] " + key + " -> " + content);
        return content;
    }

    /** 列举桶内对象 */
    public List<String> list() {
        ListObjectsV2Response resp = s3Client.listObjectsV2(b -> b.bucket(config.getBucket()));
        List<String> keys = resp.contents().stream().map(S3Object::key).toList();
        System.out.println("[列举] " + keys);
        return keys;
    }

    /** 生成临时下载链接（本地磁盘最难自己实现的特性） */
    public String presignedUrl(String key, Duration ttl) {
        String url = s3Presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(ttl)
                                .getObjectRequest(g -> g.bucket(config.getBucket()).key(key))
                                .build())
                .url().toString();
        System.out.println("[预签名URL] " + url);
        return url;
    }

    /** 删除对象 */
    public void delete(String key) {
        s3Client.deleteObject(b -> b.bucket(config.getBucket()).key(key));
        System.out.println("[删除] " + key);
    }
}
