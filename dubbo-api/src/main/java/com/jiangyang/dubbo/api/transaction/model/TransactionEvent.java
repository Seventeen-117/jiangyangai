package com.jiangyang.dubbo.api.transaction.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事务事件模型
 * 用于在服务间传递事务状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 全局事务ID
     */
    private String globalTransactionId;

    /**
     * 业务事务ID
     */
    private String transactionId;

    /**
     * 分支事务ID
     */
    private String branchTransactionId;

    /**
     * 事务状态
     */
    private String status;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 事件来源
     */
    private String source;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 扩展信息
     */
    private Map<String, Object> extraData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 处理时间
     */
    private LocalDateTime processTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否异步处理
     */
    private Boolean asyncProcess;

    /**
     * 回调URL
     */
    private String callbackUrl;

    /**
     * 标签
     */
    private String tags;

    /**
     * 版本号
     */
    private String version;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 修改者
     */
    private String modifier;

    /**
     * 备注
     */
    private String remark;
}
