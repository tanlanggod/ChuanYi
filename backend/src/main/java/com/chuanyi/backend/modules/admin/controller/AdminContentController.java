package com.chuanyi.backend.modules.admin.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.common.web.AuthHeaderUtil;
import com.chuanyi.backend.modules.admin.service.AdminAuthService;
import com.chuanyi.backend.modules.admin.service.AdminContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private static final Logger log = LoggerFactory.getLogger(AdminContentController.class);

    private final AdminAuthService adminAuthService;
    private final AdminContentService adminContentService;
    private final String uploadDir;
    private final long uploadMaxBytes;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedContentTypes;

    public AdminContentController(AdminAuthService adminAuthService,
                                  AdminContentService adminContentService,
                                  @Value("${app.upload-dir:uploads}") String uploadDir,
                                  @Value("${app.upload.max-bytes:10485760}") long uploadMaxBytes,
                                  @Value("${app.upload.allowed-extensions:jpg,jpeg,png,webp,gif}") String allowedExtensions,
                                  @Value("${app.upload.allowed-content-types:image/jpeg,image/png,image/webp,image/gif}") String allowedContentTypes) {
        this.adminAuthService = adminAuthService;
        this.adminContentService = adminContentService;
        this.uploadDir = uploadDir;
        this.uploadMaxBytes = uploadMaxBytes;
        this.allowedExtensions = parseCsvAsSet(allowedExtensions);
        this.allowedContentTypes = parseCsvAsSet(allowedContentTypes);
    }

    @GetMapping("/banners")
    public ApiResponse<Map<String, Object>> banners(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") Integer pageNo,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminContentService.listBanners(keyword, status, pageNo, pageSize));
    }

    @PostMapping("/banners/{id}/status")
    public ApiResponse<Map<String, Object>> updateBannerStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                               @PathVariable Long id,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminContentService.updateBannerStatus(id, body));
    }

    @PostMapping("/banners/save")
    public ApiResponse<Map<String, Object>> saveBanner(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminContentService.saveBanner(body));
    }

    @GetMapping("/gallery")
    public ApiResponse<Map<String, Object>> gallery(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String displayStatus,
                                                    @RequestParam(defaultValue = "1") Integer pageNo,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminContentService.listGallery(keyword, displayStatus, pageNo, pageSize));
    }

    @PostMapping("/gallery/{id}/status")
    public ApiResponse<Map<String, Object>> updateGalleryStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                @PathVariable Long id,
                                                                @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminContentService.updateGalleryStatus(id, body));
    }

    @PostMapping("/gallery/{id}/save")
    public ApiResponse<Map<String, Object>> saveGallery(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @PathVariable Long id,
                                                        @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminContentService.saveGallery(id, body));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestPart("file") MultipartFile file) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "file is required");
        }
        if (file.getSize() > uploadMaxBytes) {
            throw new BizException(400, "file too large, max " + (uploadMaxBytes / 1024 / 1024) + "MB");
        }

        String contentType = safeLowerCase(file.getContentType());
        if (!allowedContentTypes.contains(contentType)) {
            throw new BizException(400, "unsupported content type: " + contentType);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension.isEmpty()) {
            extension = extensionFromContentType(contentType);
        }
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            throw new BizException(400, "unsupported file extension");
        }

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
            String safeName = "content_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = uploadRoot.resolve(safeName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new BizException(400, "invalid file name");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            Map<String, Object> data = new HashMap<String, Object>();
            data.put("url", "/uploads/" + safeName);
            return ApiResponse.ok(data);
        } catch (IOException e) {
            log.error("Upload failed. originalFileName={}, size={}, contentType={}, uploadRoot={}, reason={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType(), uploadRoot, e.getMessage(), e);
            String reason = e.getMessage() == null ? "io error" : e.getMessage();
            throw new BizException(500, "upload failed: " + reason);
        }
    }

    private static Set<String> parseCsvAsSet(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<String>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(AdminContentController::safeLowerCase)
                .filter(item -> !item.isEmpty())
                .forEach(set::add);
        return set;
    }

    private static String safeLowerCase(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,10}")) {
            return "";
        }
        return extension;
    }

    private static String extensionFromContentType(String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return "jpg";
        }
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        if ("image/gif".equals(contentType)) {
            return "gif";
        }
        return "";
    }
}
