package org.twiglau.lease.web.admin.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理服务
 *
 * @author 朱俊伟
 * @since 2024/06/10 00:01
 */
public interface IFileService {
    String upload(MultipartFile file);
}
