package com.jiangyang.dubbo.api.bgai;

import com.jiangyang.dubbo.api.common.Result;

/**
 * BGAI服务Dubbo API接口
 * 
 * @author jiangyang
 * @since 1.0.0
 */
public interface BgaiService {

    /**
     * 处理图片识别请求（网关内部接口）
     * 
     * @param request 图片识别请求
     * @return 识别结果
     */
    Result<ImageRecognitionResult> processImageRecognition(ImageRecognitionRequest request);

    /**
     * 处理逻辑流程图和文字请求（网关内部接口）
     * 
     * @param request 逻辑处理请求
     * @return 处理结果
     */
    Result<LogicProcessResult> processLogicRequest(LogicProcessRequest request);

    /**
     * 图片识别请求
     */
    class ImageRecognitionRequest {
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
     * 逻辑处理请求
     */
    class LogicProcessRequest {
        private String requestId;
        private String businessType;
        private String userId;
        private String source;
        private String logicFlowChart;
        private String logicDescription;
        private java.util.List<CalculationStep> calculationSteps;
        private java.util.List<ParameterDescription> inputParameters;
        private java.util.List<ParameterDescription> outputParameters;
        private String complexity;
        private Long estimatedExecutionTime;
        private java.util.Map<String, Object> originalParameters;
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
        
        public String getLogicFlowChart() { return logicFlowChart; }
        public void setLogicFlowChart(String logicFlowChart) { this.logicFlowChart = logicFlowChart; }
        
        public String getLogicDescription() { return logicDescription; }
        public void setLogicDescription(String logicDescription) { this.logicDescription = logicDescription; }
        
        public java.util.List<CalculationStep> getCalculationSteps() { return calculationSteps; }
        public void setCalculationSteps(java.util.List<CalculationStep> calculationSteps) { this.calculationSteps = calculationSteps; }
        
        public java.util.List<ParameterDescription> getInputParameters() { return inputParameters; }
        public void setInputParameters(java.util.List<ParameterDescription> inputParameters) { this.inputParameters = inputParameters; }
        
        public java.util.List<ParameterDescription> getOutputParameters() { return outputParameters; }
        public void setOutputParameters(java.util.List<ParameterDescription> outputParameters) { this.outputParameters = outputParameters; }
        
        public String getComplexity() { return complexity; }
        public void setComplexity(String complexity) { this.complexity = complexity; }
        
        public Long getEstimatedExecutionTime() { return estimatedExecutionTime; }
        public void setEstimatedExecutionTime(Long estimatedExecutionTime) { this.estimatedExecutionTime = estimatedExecutionTime; }
        
        public java.util.Map<String, Object> getOriginalParameters() { return originalParameters; }
        public void setOriginalParameters(java.util.Map<String, Object> originalParameters) { this.originalParameters = originalParameters; }
        
        public java.util.Map<String, Object> getExtraParams() { return extraParams; }
        public void setExtraParams(java.util.Map<String, Object> extraParams) { this.extraParams = extraParams; }

        /**
         * 计算步骤
         */
        public static class CalculationStep {
            private Integer stepNumber;
            private String stepName;
            private String stepDescription;
            private String stepType;
            private java.util.List<Integer> dependencies;
            private String formula;

            // Getters and Setters
            public Integer getStepNumber() { return stepNumber; }
            public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }
            
            public String getStepName() { return stepName; }
            public void setStepName(String stepName) { this.stepName = stepName; }
            
            public String getStepDescription() { return stepDescription; }
            public void setStepDescription(String stepDescription) { this.stepDescription = stepDescription; }
            
            public String getStepType() { return stepType; }
            public void setStepType(String stepType) { this.stepType = stepType; }
            
            public java.util.List<Integer> getDependencies() { return dependencies; }
            public void setDependencies(java.util.List<Integer> dependencies) { this.dependencies = dependencies; }
            
            public String getFormula() { return formula; }
            public void setFormula(String formula) { this.formula = formula; }
        }

        /**
         * 参数描述
         */
        public static class ParameterDescription {
            private String parameterName;
            private String parameterType;
            private String parameterDescription;
            private Boolean required;
            private Object defaultValue;
            private String valueRange;

            // Getters and Setters
            public String getParameterName() { return parameterName; }
            public void setParameterName(String parameterName) { this.parameterName = parameterName; }
            
            public String getParameterType() { return parameterType; }
            public void setParameterType(String parameterType) { this.parameterType = parameterType; }
            
            public String getParameterDescription() { return parameterDescription; }
            public void setParameterDescription(String parameterDescription) { this.parameterDescription = parameterDescription; }
            
            public Boolean getRequired() { return required; }
            public void setRequired(Boolean required) { this.required = required; }
            
            public Object getDefaultValue() { return defaultValue; }
            public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
            
            public String getValueRange() { return valueRange; }
            public void setValueRange(String valueRange) { this.valueRange = valueRange; }
        }
    }

    /**
     * 逻辑处理结果
     */
    class LogicProcessResult {
        private String requestId;
        private String businessType;
        private String userId;
        private String processStatus;
        private Long processStartTime;
        private Long processEndTime;
        private Long processDuration;
        private java.util.Map<String, Object> calculationBasis;
        private java.util.List<CalculationFormula> formulas;
        private java.util.List<DataSourceInfo> dataSources;
        private java.util.List<CalculationRule> calculationRules;
        private java.util.List<ValidationRule> validationRules;
        private OutputFormat outputFormat;
        private ExecutionAdvice executionAdvice;
        private java.util.List<String> errorMessages;
        private java.util.Map<String, Object> metadata;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getProcessStatus() { return processStatus; }
        public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
        
        public Long getProcessStartTime() { return processStartTime; }
        public void setProcessStartTime(Long processStartTime) { this.processStartTime = processStartTime; }
        
        public Long getProcessEndTime() { return processEndTime; }
        public void setProcessEndTime(Long processEndTime) { this.processEndTime = processEndTime; }
        
        public Long getProcessDuration() { return processDuration; }
        public void setProcessDuration(Long processDuration) { this.processDuration = processDuration; }
        
        public java.util.Map<String, Object> getCalculationBasis() { return calculationBasis; }
        public void setCalculationBasis(java.util.Map<String, Object> calculationBasis) { this.calculationBasis = calculationBasis; }
        
        public java.util.List<CalculationFormula> getFormulas() { return formulas; }
        public void setFormulas(java.util.List<CalculationFormula> formulas) { this.formulas = formulas; }
        
        public java.util.List<DataSourceInfo> getDataSources() { return dataSources; }
        public void setDataSources(java.util.List<DataSourceInfo> dataSources) { this.dataSources = dataSources; }
        
        public java.util.List<CalculationRule> getCalculationRules() { return calculationRules; }
        public void setCalculationRules(java.util.List<CalculationRule> calculationRules) { this.calculationRules = calculationRules; }
        
        public java.util.List<ValidationRule> getValidationRules() { return validationRules; }
        public void setValidationRules(java.util.List<ValidationRule> validationRules) { this.validationRules = validationRules; }
        
        public OutputFormat getOutputFormat() { return outputFormat; }
        public void setOutputFormat(OutputFormat outputFormat) { this.outputFormat = outputFormat; }
        
        public ExecutionAdvice getExecutionAdvice() { return executionAdvice; }
        public void setExecutionAdvice(ExecutionAdvice executionAdvice) { this.executionAdvice = executionAdvice; }
        
        public java.util.List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(java.util.List<String> errorMessages) { this.errorMessages = errorMessages; }
        
        public java.util.Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }

        /**
         * 计算公式
         */
        public static class CalculationFormula {
            private String formulaId;
            private String formulaName;
            private String expression;
            private String description;
            private String conditions;
            private Integer priority;

            // Getters and Setters
            public String getFormulaId() { return formulaId; }
            public void setFormulaId(String formulaId) { this.formulaId = formulaId; }
            
            public String getFormulaName() { return formulaName; }
            public void setFormulaName(String formulaName) { this.formulaName = formulaName; }
            
            public String getExpression() { return expression; }
            public void setExpression(String expression) { this.expression = expression; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public String getConditions() { return conditions; }
            public void setConditions(String conditions) { this.conditions = conditions; }
            
            public Integer getPriority() { return priority; }
            public void setPriority(Integer priority) { this.priority = priority; }
        }

        /**
         * 数据源信息
         */
        public static class DataSourceInfo {
            private String dataSourceId;
            private String dataSourceName;
            private String dataSourceType;
            private String dataSourceUrl;
            private String dataFormat;
            private String updateFrequency;
            private String dataQuality;

            // Getters and Setters
            public String getDataSourceId() { return dataSourceId; }
            public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
            
            public String getDataSourceName() { return dataSourceName; }
            public void setDataSourceName(String dataSourceName) { this.dataSourceName = dataSourceName; }
            
            public String getDataSourceType() { return dataSourceType; }
            public void setDataSourceType(String dataSourceType) { this.dataSourceType = dataSourceType; }
            
            public String getDataSourceUrl() { return dataSourceUrl; }
            public void setDataSourceUrl(String dataSourceUrl) { this.dataSourceUrl = dataSourceUrl; }
            
            public String getDataFormat() { return dataFormat; }
            public void setDataFormat(String dataFormat) { this.dataFormat = dataFormat; }
            
            public String getUpdateFrequency() { return updateFrequency; }
            public void setUpdateFrequency(String updateFrequency) { this.updateFrequency = updateFrequency; }
            
            public String getDataQuality() { return dataQuality; }
            public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }
        }

        /**
         * 计算规则
         */
        public static class CalculationRule {
            private String ruleId;
            private String ruleName;
            private String ruleType;
            private String description;
            private java.util.Map<String, Object> parameters;
            private Integer executionOrder;

            // Getters and Setters
            public String getRuleId() { return ruleId; }
            public void setRuleId(String ruleId) { this.ruleId = ruleId; }
            
            public String getRuleName() { return ruleName; }
            public void setRuleName(String ruleName) { this.ruleName = ruleName; }
            
            public String getRuleType() { return ruleType; }
            public void setRuleType(String ruleType) { this.ruleType = ruleType; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            
            public java.util.Map<String, Object> getParameters() { return parameters; }
            public void setParameters(java.util.Map<String, Object> parameters) { this.parameters = parameters; }
            
            public Integer getExecutionOrder() { return executionOrder; }
            public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }
        }

        /**
         * 验证规则
         */
        public static class ValidationRule {
            private String validationRuleId;
            private String validationRuleName;
            private String validationType;
            private String validationExpression;
            private String errorMessage;
            private String severity;

            // Getters and Setters
            public String getValidationRuleId() { return validationRuleId; }
            public void setValidationRuleId(String validationRuleId) { this.validationRuleId = validationRuleId; }
            
            public String getValidationRuleName() { return validationRuleName; }
            public void setValidationRuleName(String validationRuleName) { this.validationRuleName = validationRuleName; }
            
            public String getValidationType() { return validationType; }
            public void setValidationType(String validationType) { this.validationType = validationType; }
            
            public String getValidationExpression() { return validationExpression; }
            public void setValidationExpression(String validationExpression) { this.validationExpression = validationExpression; }
            
            public String getErrorMessage() { return errorMessage; }
            public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
            
            public String getSeverity() { return severity; }
            public void setSeverity(String severity) { this.severity = severity; }
        }

        /**
         * 输出格式
         */
        public static class OutputFormat {
            private String outputType;
            private java.util.Map<String, Object> outputStructure;
            private Integer precision;
            private String unit;
            private String formatRule;

            // Getters and Setters
            public String getOutputType() { return outputType; }
            public void setOutputType(String outputType) { this.outputType = outputType; }
            
            public java.util.Map<String, Object> getOutputStructure() { return outputStructure; }
            public void setOutputStructure(java.util.Map<String, Object> outputStructure) { this.outputStructure = outputStructure; }
            
            public Integer getPrecision() { return precision; }
            public void setPrecision(Integer precision) { this.precision = precision; }
            
            public String getUnit() { return unit; }
            public void setUnit(String unit) { this.unit = unit; }
            
            public String getFormatRule() { return formatRule; }
            public void setFormatRule(String formatRule) { this.formatRule = formatRule; }
        }

        /**
         * 执行建议
         */
        public static class ExecutionAdvice {
            private String executionMode;
            private Integer recommendedConcurrency;
            private Long recommendedTimeout;
            private java.util.List<String> performanceOptimizations;
            private java.util.List<String> riskWarnings;

            // Getters and Setters
            public String getExecutionMode() { return executionMode; }
            public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
            
            public Integer getRecommendedConcurrency() { return recommendedConcurrency; }
            public void setRecommendedConcurrency(Integer recommendedConcurrency) { this.recommendedConcurrency = recommendedConcurrency; }
            
            public Long getRecommendedTimeout() { return recommendedTimeout; }
            public void setRecommendedTimeout(Long recommendedTimeout) { this.recommendedTimeout = recommendedTimeout; }
            
            public java.util.List<String> getPerformanceOptimizations() { return performanceOptimizations; }
            public void setPerformanceOptimizations(java.util.List<String> performanceOptimizations) { this.performanceOptimizations = performanceOptimizations; }
            
            public java.util.List<String> getRiskWarnings() { return riskWarnings; }
            public void setRiskWarnings(java.util.List<String> riskWarnings) { this.riskWarnings = riskWarnings; }
        }
    }
}
