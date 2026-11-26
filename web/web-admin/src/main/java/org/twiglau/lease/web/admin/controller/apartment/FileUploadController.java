package org.twiglau.lease.web.admin.controller.apartment;


import org.twiglau.lease.common.result.Result;
import org.twiglau.lease.web.admin.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件管理
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Tag(name = "文件管理")
@RequestMapping("/admin/file")
@RestController
@AllArgsConstructor
public class FileUploadController {

    private final IFileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping("upload")
    public Result<String> upload(@RequestParam MultipartFile file) {
        String url = fileService.upload(file);
        return Result.ok(url);
    }

}
