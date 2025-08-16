package com.jiangyang.datacalculation.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 图片识别结果模型
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Data
public class ImageRecognitionResult {

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
     * 识别状态
     */
    private String recognitionStatus;

    /**
     * 识别开始时间
     */
    private Long recognitionStartTime;

    /**
     * 识别完成时间
     */
    private Long recognitionEndTime;

    /**
     * 识别耗时（毫秒）
     */
    private Long recognitionDuration;

    /**
     * 图片识别结果列表
     */
    private List<ImageResult> imageResults;

    /**
     * 生成的SQL语句
     */
    private List<SqlStatement> sqlStatements;

    /**
     * 识别出的文本内容
     */
    private String recognizedText;

    /**
     * 错误信息
     */
    private List<String> errorMessages;

    /**
     * 元数据信息
     */
    private Map<String, Object> metadata;

    /**
     * 图片识别结果
     */
    @Data
    public static class ImageResult {
        
        /**
         * 图片ID
         */
        private String imageId;
        
        /**
         * 图片名称
         */
        private String imageName;
        
        /**
         * 识别状态
         */
        private String status;
        
        /**
         * 识别出的文本内容
         */
        private String recognizedText;
        
        /**
         * 识别出的表格数据
         */
        private List<List<String>> tableData;
        
        /**
         * 识别出的字段信息
         */
        private List<FieldInfo> fields;
        
        /**
         * 识别出的图表信息
         */
        private ChartInfo chartInfo;
        
        /**
         * 置信度
         */
        private Double confidence;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }

    /**
     * 字段信息
     */
    @Data
    public static class FieldInfo {
        
        /**
         * 字段名称
         */
        private String fieldName;
        
        /**
         * 字段值
         */
        private String fieldValue;
        
        /**
         * 字段类型
         */
        private String fieldType;
        
        /**
         * 字段描述
         */
        private String description;
        
        /**
         * 置信度
         */
        private Double confidence;
    }

    /**
     * 图表信息
     */
    @Data
    public static class ChartInfo {
        
        /**
         * 图表类型
         */
        private String chartType;
        
        /**
         * 图表标题
         */
        private String title;
        
        /**
         * 图表数据
         */
        private Map<String, Object> chartData;
        
        /**
         * 坐标轴信息
         */
        private Map<String, Object> axisInfo;
    }

    /**
     * SQL语句
     */
    @Data
    public static class SqlStatement {
        
        /**
         * SQL ID
         */
        private String sqlId;
        
        /**
         * SQL类型（SELECT, INSERT, UPDATE, DELETE等）
         */
        private String sqlType;
        
        /**
         * SQL语句内容
         */
        private String sqlContent;
        
        /**
         * SQL描述
         */
        private String description;
        
        /**
         * 目标表名
         */
        private String targetTable;
        
        /**
         * 相关字段
         */
        private List<String> relatedFields;
        
        /**
         * 执行优先级
         */
        private Integer priority;
        
        /**
         * 是否已验证
         */
        private Boolean validated;
    }
}
