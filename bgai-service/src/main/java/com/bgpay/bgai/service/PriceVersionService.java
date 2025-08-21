package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.PriceVersion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiangyang.base.datasource.annotation.DataSource;
import jakarta.validation.constraints.Pattern;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zly
 * @since 2025-03-10 15:32:02
 */
@DataSource("master")
public interface PriceVersionService extends IService<PriceVersion> {
    Integer getCurrentVersion(@Pattern(regexp = "chat|reasoner") String modelType);
}
