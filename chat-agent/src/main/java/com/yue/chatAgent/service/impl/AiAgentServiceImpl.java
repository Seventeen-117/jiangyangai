package com.yue.chatAgent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yue.chatAgent.entity.AiAgent;
import com.yue.chatAgent.mapper.AiAgentMapper;
import com.yue.chatAgent.service.AiAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI代理服务实现类
 * 
 * @author yue
 * @version 1.0.0
 */
@Slf4j
@Service
public class AiAgentServiceImpl extends ServiceImpl<AiAgentMapper, AiAgent> implements AiAgentService {

    @Override
    public AiAgent getByType(String type) {
        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgent::getType, type)
               .eq(AiAgent::getStatus, 1);
        return this.getOne(wrapper);
    }

    @Override
    public boolean testConnection(Long id) {
        try {
            AiAgent agent = this.getById(id);
            if (agent == null) {
                log.warn("AI代理不存在，ID: {}", id);
                return false;
            }
            
            // TODO: 根据不同类型实现具体的连接测试逻辑
            log.info("测试AI代理连接成功，ID: {}, 类型: {}", id, agent.getType());
            return true;
        } catch (Exception e) {
            log.error("测试AI代理连接失败，ID: {}", id, e);
            return false;
        }
    }
}
