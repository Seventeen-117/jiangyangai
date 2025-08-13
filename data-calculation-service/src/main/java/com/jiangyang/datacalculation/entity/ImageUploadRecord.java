package com.jiangyang.datacalculation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图片上传记录实体
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
@TableName("image_upload_record")
public class ImageUploadRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 请求来源
     */
    private String source;

    /**
     * 文字信息
     */
    private String textInfo;

    /**
     * 处理状态：PENDING-待处理，PROCESSING-处理中，COMPLETED-已完成，FAILED-失败
     */
    private String status;

    /**
     * 扩展参数
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> extraParams;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
