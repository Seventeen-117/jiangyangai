package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.PriceConfig;
import com.bgpay.bgai.entity.PriceQuery;
import com.jiangyang.base.datasource.annotation.DataSource;

@DataSource("master")
public interface PriceCacheService {
    PriceConfig getPriceConfig(PriceQuery query);
    public void refreshCacheByModel(String modelType);

    void clearPriceConfigCache();
}