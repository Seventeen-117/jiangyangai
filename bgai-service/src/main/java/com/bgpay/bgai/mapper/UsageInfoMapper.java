package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bgpay.bgai.entity.UsageInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zly
 * @since 2025-03-08 23:09:50
 */
@Mapper
public interface UsageInfoMapper extends BaseMapper<UsageInfo> {

    default List<UsageInfo> selectBatchByIds(List<Long> ids) {
        LambdaQueryWrapper<UsageInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UsageInfo::getId, ids);
        return this.selectList(queryWrapper);
    }

    default UsageInfo findByCompletionId(String completionId) {
        LambdaQueryWrapper<UsageInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UsageInfo::getChatCompletionId, completionId);
        return this.selectOne(queryWrapper);
    }

    @Select("SELECT * FROM usage_info WHERE chat_completion_id = #{completionId} LIMIT 1")
    UsageInfo selectByCompletionId(@Param("completionId") String completionId);

}
