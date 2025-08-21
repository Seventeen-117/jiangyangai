package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.jiangyang.base.datasource.annotation.DataSource;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
@DataSource("master")
public interface BillingService {
    public void processBatch(List<UsageCalculationDTO> batch,String userId);

    void processSingleRecord(UsageCalculationDTO calculationDTO,String userId);

    public void processMessage(MessageExt messageExt);


}
