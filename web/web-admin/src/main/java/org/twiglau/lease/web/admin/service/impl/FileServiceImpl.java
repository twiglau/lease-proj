package org.twiglau.lease.web.admin.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.twiglau.lease.config.MinioProperties;
import org.twiglau.lease.exception.FileStorageException;
import org.twiglau.lease.web.admin.service.IFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 文件管理实现
 *
 * @author 朱俊伟
 * @since 2024/06/10 00:01
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;


    /**
     * MultipartFile file 解释
     * file.getBytes()：直接返回整个文件内容，加载到内存当中
     * file.getInputStream():输入流，逐个字节读取文件内容，加载到内存只是某个片段
     * file.getName():
     * @param file
     * @return
     */
    @Override
    public String upload(MultipartFile file) {
        System.out.printf("getName: %s \n", file.getName());
        System.out.println(file.getOriginalFilename());
        try {
            initBucket();
//            String fileName = DateUtil.format(new Date(), "yyyMMdd")
//                    + "/"
//                    + IdUtil.simpleUUID()
//                    + "-"
//                    + file.getOriginalFilename();

            String fileName = new SimpleDateFormat("yyyyMMdd").format(new Date())
                    + "/"
                    + UUID.randomUUID()
                    + "-"
                    + file.getOriginalFilename();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            return minioProperties.getEndPoint()
                    + "/"
                    + minioProperties.getBucketName()
                    + "/"
                    + fileName;
        } catch (IOException e) {
            throw new FileStorageException("读取上传文件失败", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("文件上传失败", e);
        }
    }


    /**
     * 初始化存储桶
     * 检查存储桶是否存在，如果不存在，则创建存储桶并设置公共读权限
     * 这个方法确保了存储桶在使用前已经正确配置，适用于需要公共访问的场景。
     * @throws ServerException
     * @throws InsufficientDataException
     * @throws ErrorResponseException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidResponseException
     * @throws XmlParserException
     * @throws InternalException
     */
    private void initBucket() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketName())
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config("""
                            {
                              "Statement" : [ {
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                              } ],
                              "Version" : "2012-10-17"
                            }
                            """.formatted(minioProperties.getBucketName()))
                    .build());
        }
    }

}
