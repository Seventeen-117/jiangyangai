package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.Choices;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiangyang.base.datasource.annotation.DataSource;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:37:28
 */
@DataSource("master")
public interface ChoicesService extends IService<Choices> {
    public void insertChoices(Choices choices);
}
