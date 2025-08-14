package com.yue.chatAgent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI代理实体类
 * 
 * @author yue
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ai_agent")
public class AiAgent {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 代理名称
     */
    private String name;

    /**
     * 代理描述
     */
    private String description;

    /**
     * 代理类型 (openai, azure, ollama)
     */
    private String type;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 配置参数 (JSON格式)
     */
    private String config;

    /**
     * 状态 (0: 禁用, 1: 启用)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}
