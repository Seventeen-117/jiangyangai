package com.yue.chatAgent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yue.chatAgent.entity.AiAgent;

/**
 * AI代理服务接口
 * 
 * @author yue
 * @version 1.0.0
 */
public interface AiAgentService extends IService<AiAgent> {

    /**
     * 根据类型获取AI代理
     * 
     * @param type 代理类型
     * @return AI代理
     */
    AiAgent getByType(String type);

    /**
     * 测试AI代理连接
     * 
     * @param id 代理ID
     * @return 测试结果
     */
    boolean testConnection(Long id);
}
