
package com.jiangyang.dubbo.api.datacalculation;

import com.jiangyang.dubbo.api.common.Result;

/**
 * 数据计算服务Dubbo API接口
 * 
 * @author jiangyang
 * @since 1.0.0
 */
public interface DataCalculationService {

    /**
     * 处理图片上传和识别请求
     * 
     * @param request 图片上传请求
     * @return 处理结果
     */
    Result<String> processImageUpload(ImageUploadRequest request);

    /**
     * 获取图片识别结果
     * 
     * @param requestId 请求ID
     * @return 识别结果
     */
    Result<ImageRecognitionResult> getImageRecognitionResult(String requestId);

    /**
     * 执行数据计算
     * 
     * @param request 计算请求
     * @return 计算结果
     */
    Result<CalculationResponse> executeCalculation(CalculationRequest request);

    /**
     * 异步执行数据计算
     * 
     * @param request 计算请求
     * @return 任务ID
     */
    Result<String> executeCalculationAsync(CalculationRequest request);

    /**
     * 获取计算任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    Result<CalculationTaskStatus> getCalculationTaskStatus(String taskId);

    /**
     * 取消计算任务
     * 
     * @param taskId 任务ID
     * @return 取消结果
     */
    Result<Boolean> cancelCalculationTask(String taskId);

    /**
     * 图片上传请求
     */
    class ImageUploadRequest {
        private String requestId;
        private String businessType;
        private String userId;
        private String source;
        private String textInfo;
        private java.util.List<ImageFile> images;
        private java.util.Map<String, Object> extraParams;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public String getTextInfo() { return textInfo; }
        public void setTextInfo(String textInfo) { this.textInfo = textInfo; }
        
        public java.util.List<ImageFile> getImages() { return images; }
        public void setImages(java.util.List<ImageFile> images) { this.images = images; }
        
        public java.util.Map<String, Object> getExtraParams() { return extraParams; }
        public void setExtraParams(java.util.Map<String, Object> extraParams) { this.extraParams = extraParams; }

        /**
         * 图片文件信息
         */
        public static class ImageFile {
            private String imageId;
            private String imageName;
            private String imageType;
            private Long imageSize;
            private String imageContent;
            private String imageUrl;
            private String description;
            private Integer order;

            // Getters and Setters
            public String getImageId() { return imageId; }
            public void setImageId(String imageId) { this.imageId = imageId; }
            
            public String getImageName() { return imageName; }
            public void setImageName(String imageName) { this.imageName = imageName; }
            
            public String getImageType() { return imageType; }
            public void setImageType(String imageType) { this.imageType = imageType; }
            
            public Long getImageSize() { return imageSize; }
            public void setImageSize(Long imageSize) { this.imageSize = imageSize; }
            
            public String getImageContent() { return imageContent; }
            public void setImageContent(String imageContent) { this.imageContent = imageContent; }
            
            public String getImageUrl() { return imageUrl; }
            public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public Integer getOrder() { return order; }
            public void setOrder(Integer order) { this.order = order; }
        }
    }

    /**
     * 图片识别结果
     */
    class ImageRecognitionResult {
        private String requestId;
        private String businessType;
        private String userId;
        private String recognitionStatus;
        private Long recognitionStartTime;
        private Long recognitionEndTime;
        private Long recognitionDuration;
        private java.util.List<ImageResult> imageResults;
        private java.util.List<SqlStatement> sqlStatements;
        private String recognizedText;
        private java.util.List<String> errorMessages;
        private java.util.Map<String, Object> metadata;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getRecognitionStatus() { return recognitionStatus; }
        public void setRecognitionStatus(String recognitionStatus) { this.recognitionStatus = recognitionStatus; }
        
        public Long getRecognitionStartTime() { return recognitionStartTime; }
        public void setRecognitionStartTime(Long recognitionStartTime) { this.recognitionStartTime = recognitionStartTime; }
        
        public Long getRecognitionEndTime() { return recognitionEndTime; }
        public void setRecognitionEndTime(Long recognitionEndTime) { this.recognitionEndTime = recognitionEndTime; }
        
        public Long getRecognitionDuration() { return recognitionDuration; }
        public void setRecognitionDuration(Long recognitionDuration) { this.recognitionDuration = recognitionDuration; }
        
        public java.util.List<ImageResult> getImageResults() { return imageResults; }
        public void setImageResults(java.util.List<ImageResult> imageResults) { this.imageResults = imageResults; }
        
        public java.util.List<SqlStatement> getSqlStatements() { return sqlStatements; }
        public void setSqlStatements(java.util.List<SqlStatement> sqlStatements) { this.sqlStatements = sqlStatements; }
        
        public String getRecognizedText() { return recognizedText; }
        public void setRecognizedText(String recognizedText) { this.recognizedText = recognizedText; }
        
        public java.util.List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(java.util.List<String> errorMessages) { this.errorMessages = errorMessages; }
        
        public java.util.Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }

        /**
         * 图片识别结果
         */
        public static class ImageResult {
            private String imageId;
            private String imageName;
            private String status;
            private String recognizedText;
            private java.util.List<java.util.List<String>> tableData;
            private java.util.List<FieldInfo> fields;
            private ChartInfo chartInfo;
            private Double confidence;
            private String errorMessage;

            // Getters and Setters
            public String getImageId() { return imageId; }
            public void setImageId(String imageId) { this.imageId = imageId; }
            
            public String getImageName() { return imageName; }
            public void setImageName(String imageName) { this.imageName = imageName; }
            
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
            
            public String getRecognizedText() { return recognizedText; }
            public void setRecognizedText(String recognizedText) { this.recognizedText = recognizedText; }
            
            public java.util.List<java.util.List<String>> getTableData() { return tableData; }
            public void setTableData(java.util.List<java.util.List<String>> tableData) { this.tableData = tableData; }
            
            public java.util.List<FieldInfo> getFields() { return fields; }
            public void setFields(java.util.List<FieldInfo> fields) { this.fields = fields; }
            
            public ChartInfo getChartInfo() { return chartInfo; }
            public void setChartInfo(ChartInfo chartInfo) { this.chartInfo = chartInfo; }
            
            public Double getConfidence() { return confidence; }
            public void setConfidence(Double confidence) { this.confidence = confidence; }
            
            public String getErrorMessage() { return errorMessage; }
            public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        }

        /**
         * 字段信息
         */
        public static class FieldInfo {
            private String fieldName;
            private String fieldValue;
            private String fieldType;
            private String description;
            private Double confidence;

            // Getters and Setters
            public String getFieldName() { return fieldName; }
            public void setFieldName(String fieldName) { this.fieldName = fieldName; }
            
            public String getFieldValue() { return fieldValue; }
            public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
            
            public String getFieldType() { return fieldType; }
            public void setFieldType(String fieldType) { this.fieldType = fieldType; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public Double getConfidence() { return confidence; }
            public void setConfidence(Double confidence) { this.confidence = confidence; }
        }

        /**
         * 图表信息
         */
        public static class ChartInfo {
            private String chartType;
            private String title;
            private java.util.Map<String, Object> chartData;
            private java.util.Map<String, Object> axisInfo;

            // Getters and Setters
            public String getChartType() { return chartType; }
            public void setChartType(String chartType) { this.chartType = chartType; }
            
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            
            public java.util.Map<String, Object> getChartData() { return chartData; }
            public void setChartData(java.util.Map<String, Object> chartData) { this.chartData = chartData; }
            
            public java.util.Map<String, Object> getAxisInfo() { return axisInfo; }
            public void setAxisInfo(java.util.Map<String, Object> axisInfo) { this.axisInfo = axisInfo; }
        }

        /**
         * SQL语句
         */
        public static class SqlStatement {
            private String sqlId;
            private String sqlType;
            private String sqlContent;
            private String description;
            private String targetTable;
            private java.util.List<String> relatedFields;
            private Integer priority;
            private Boolean validated;

            // Getters and Setters
            public String getSqlId() { return sqlId; }
            public void setSqlId(String sqlId) { this.sqlId = sqlId; }
            
            public String getSqlType() { return sqlType; }
            public void setSqlType(String sqlType) { this.sqlType = sqlType; }
            
            public String getSqlContent() { return sqlContent; }
            public void setSqlContent(String sqlContent) { this.sqlContent = sqlContent; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public String getTargetTable() { return targetTable; }
            public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
            
            public java.util.List<String> getRelatedFields() { return relatedFields; }
            public void setRelatedFields(java.util.List<String> relatedFields) { this.relatedFields = relatedFields; }
            
            public Integer getPriority() { return priority; }
            public void setPriority(Integer priority) { this.priority = priority; }
            
            public Boolean getValidated() { return validated; }
            public void setValidated(Boolean validated) { this.validated = validated; }
        }
    }

    /**
     * 计算请求
     */
    class CalculationRequest {
        private String requestId;
        private String businessType;
        private java.util.Map<String, Object> parameters;
        private String userId;
        private String source;
        private java.util.Map<String, Object> extraParams;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        
        public java.util.Map<String, Object> getParameters() { return parameters; }
        public void setParameters(java.util.Map<String, Object> parameters) { this.parameters = parameters; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public java.util.Map<String, Object> getExtraParams() { return extraParams; }
        public void setExtraParams(java.util.Map<String, Object> extraParams) { this.extraParams = extraParams; }
    }

    /**
     * 计算响应
     */
    class CalculationResponse {
        private Integer code;
        private String message;
        private CalculationData data;

        // Getters and Setters
        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public CalculationData getData() { return data; }
        public void setData(CalculationData data) { this.data = data; }

        /**
         * 计算数据
         */
        public static class CalculationData {
            private String requestId;
            private String businessType;
            private String calculationStatus;
            private Long calculationStartTime;
            private Long calculationEndTime;
            private Long calculationDuration;
            private java.util.Map<String, Object> calculationResult;
            private java.util.List<CalculationLog> calculationLogs;
            private java.util.List<String> errorMessages;
            private java.util.List<String> warningMessages;
            private java.util.Map<String, Object> metadata;

            // Getters and Setters
            public String getRequestId() { return requestId; }
            public void setRequestId(String requestId) { this.requestId = requestId; }
            
            public String getBusinessType() { return businessType; }
            public void setBusinessType(String businessType) { this.businessType = businessType; }
            
            public String getCalculationStatus() { return calculationStatus; }
            public void setCalculationStatus(String calculationStatus) { this.calculationStatus = calculationStatus; }
            
            public Long getCalculationStartTime() { return calculationStartTime; }
            public void setCalculationStartTime(Long calculationStartTime) { this.calculationStartTime = calculationStartTime; }
            
            public Long getCalculationEndTime() { return calculationEndTime; }
            public void setCalculationEndTime(Long calculationEndTime) { this.calculationEndTime = calculationEndTime; }
            
            public Long getCalculationDuration() { return calculationDuration; }
            public void setCalculationDuration(Long calculationDuration) { this.calculationDuration = calculationDuration; }
            
            public java.util.Map<String, Object> getCalculationResult() { return calculationResult; }
            public void setCalculationResult(java.util.Map<String, Object> calculationResult) { this.calculationResult = calculationResult; }
            
            public java.util.List<CalculationLog> getCalculationLogs() { return calculationLogs; }
            public void setCalculationLogs(java.util.List<CalculationLog> calculationLogs) { this.calculationLogs = calculationLogs; }
            
            public java.util.List<String> getErrorMessages() { return errorMessages; }
            public void setErrorMessages(java.util.List<String> errorMessages) { this.errorMessages = errorMessages; }
            
            public java.util.List<String> getWarningMessages() { return warningMessages; }
            public void setWarningMessages(java.util.List<String> warningMessages) { this.warningMessages = warningMessages; }
            
            public java.util.Map<String, Object> getMetadata() { return metadata; }
            public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
        }

        /**
         * 计算日志
         */
        public static class CalculationLog {
            private Long timestamp;
            private String level;
            private String message;
            private String stepName;
            private Object inputData;
            private Object outputData;
            private Long executionTime;

            // Getters and Setters
            public Long getTimestamp() { return timestamp; }
            public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
            
            public String getLevel() { return level; }
            public void setLevel(String level) { this.level = level; }
            
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
            
            public String getStepName() { return stepName; }
            public void setStepName(String stepName) { this.stepName = stepName; }
            
            public Object getInputData() { return inputData; }
            public void setInputData(Object inputData) { this.inputData = inputData; }
            
            public Object getOutputData() { return outputData; }
            public void setOutputData(Object outputData) { this.outputData = outputData; }
            
            public Long getExecutionTime() { return executionTime; }
            public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
        }
    }

    /**
     * 计算任务状态
     */
    class CalculationTaskStatus {
        private String taskId;
        private String requestId;
        private String businessType;
        private String taskStatus;
        private String taskType;
        private Long startTime;
        private Long endTime;
        private Long duration;
        private String errorMessage;

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        
        public String getTaskStatus() { return taskStatus; }
        public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
        
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        
        public Long getStartTime() { return startTime; }
        public void setStartTime(Long startTime) { this.startTime = startTime; }
        
        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }
        
        public Long getDuration() { return duration; }
        public void setDuration(Long duration) { this.duration = duration; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
