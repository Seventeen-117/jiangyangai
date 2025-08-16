package com.jiangyang.datacalculation.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 图片上传请求模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class ImageUploadRequest {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 请求来源
     */
    private String source;

    /**
     * 图片文件列表（Base64编码或文件路径）
     */
    @NotEmpty(message = "图片文件不能为空")
    private List<ImageFile> images;

    /**
     * 文字信息
     */
    private String textInfo;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 图片文件信息
     */
    @Data
    public static class ImageFile {
        
        /**
         * 图片ID
         */
        private String imageId;
        
        /**
         * 图片名称
         */
        private String imageName;
        
        /**
         * 图片类型
         */
        private String imageType;
        
        /**
         * 图片大小（字节）
         */
        private Long imageSize;
        
        /**
         * 图片内容（Base64编码）
         */
        private String imageContent;
        
        /**
         * 图片URL（如果已上传到存储服务）
         */
        private String imageUrl;
        
        /**
         * 图片描述
         */
        private String description;
        
        /**
         * 图片顺序
         */
        private Integer order;
    }
}
