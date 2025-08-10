package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ConfigCategory;
import com.signature.mapper.ConfigCategoryMapper;
import com.signature.service.ConfigCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置分类服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ConfigCategoryServiceImpl extends ServiceImpl<ConfigCategoryMapper, ConfigCategory> implements ConfigCategoryService {

    @Override
    public ConfigCategory findByCategoryCode(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            log.warn("findByCategoryCode: categoryCode is empty");
            return null;
        }

        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigCategory::getCategoryCode, categoryCode)
                .eq(ConfigCategory::getStatus, 1);

        ConfigCategory result = this.getOne(wrapper);
        log.info("findByCategoryCode: categoryCode={}, found={}", categoryCode, result != null);
        return result;
    }

    @Override
    public List<ConfigCategory> findAllEnabled() {
        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigCategory::getStatus, 1)
                .orderByAsc(ConfigCategory::getSortOrder)
                .orderByAsc(ConfigCategory::getId);

        List<ConfigCategory> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<ConfigCategory> findByStatus(Integer status) {
        if (status == null) {
            log.warn("findByStatus: status is null");
            return List.of();
        }

        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigCategory::getStatus, status)
                .orderByAsc(ConfigCategory::getSortOrder)
                .orderByAsc(ConfigCategory::getId);

        List<ConfigCategory> result = this.list(wrapper);
        log.info("findByStatus: status={}, count={}", status, result.size());
        return result;
    }

    @Override
    @Transactional
    public ConfigCategory createConfigCategory(ConfigCategory configCategory) {
        if (configCategory == null) {
            log.error("createConfigCategory: configCategory is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(configCategory.getCategoryCode())) {
            log.error("createConfigCategory: categoryCode is required");
            return null;
        }

        if (!StringUtils.hasText(configCategory.getCategoryName())) {
            log.error("createConfigCategory: categoryName is required");
            return null;
        }

        // 检查分类编码是否已存在
        if (existsByCategoryCode(configCategory.getCategoryCode())) {
            log.error("createConfigCategory: categoryCode already exists: {}", configCategory.getCategoryCode());
            return null;
        }

        // 设置默认值
        if (configCategory.getStatus() == null) {
            configCategory.setStatus(1); // 默认启用
        }
        if (configCategory.getSortOrder() == null) {
            configCategory.setSortOrder(getNextSortOrder());
        }

        boolean saved = this.save(configCategory);
        if (saved) {
            log.info("createConfigCategory: created category id={}, categoryCode={}", 
                    configCategory.getId(), configCategory.getCategoryCode());
            return configCategory;
        } else {
            log.error("createConfigCategory: failed to save category");
            return null;
        }
    }

    @Override
    @Transactional
    public ConfigCategory updateConfigCategory(ConfigCategory configCategory) {
        if (configCategory == null || configCategory.getId() == null) {
            log.error("updateConfigCategory: configCategory or id is null");
            return null;
        }

        ConfigCategory existing = this.getById(configCategory.getId());
        if (existing == null) {
            log.error("updateConfigCategory: category not found, id={}", configCategory.getId());
            return null;
        }

        // 如果更新了分类编码，检查是否与其他分类冲突
        if (StringUtils.hasText(configCategory.getCategoryCode()) && 
            !configCategory.getCategoryCode().equals(existing.getCategoryCode())) {
            
            if (existsByCategoryCode(configCategory.getCategoryCode())) {
                log.error("updateConfigCategory: categoryCode already exists: {}", configCategory.getCategoryCode());
                return null;
            }
        }

        boolean updated = this.updateById(configCategory);
        if (updated) {
            log.info("updateConfigCategory: updated category id={}, categoryCode={}", 
                    configCategory.getId(), configCategory.getCategoryCode());
            return configCategory;
        } else {
            log.error("updateConfigCategory: failed to update category");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteConfigCategory(Long id) {
        if (id == null) {
            log.warn("deleteConfigCategory: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deleteConfigCategory: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("updateStatus: id or status is null");
            return false;
        }

        LambdaUpdateWrapper<ConfigCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ConfigCategory::getId, id)
                .set(ConfigCategory::getStatus, status)
                .set(ConfigCategory::getUpdatedTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("updateStatus: id={}, status={}, updated={}", id, status, updated);
        return updated;
    }

    @Override
    @Transactional
    public int batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            log.warn("batchUpdateStatus: ids is empty or status is null");
            return 0;
        }

        LambdaUpdateWrapper<ConfigCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ConfigCategory::getId, ids)
                .set(ConfigCategory::getStatus, status)
                .set(ConfigCategory::getUpdatedTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? ids.size() : 0;
        log.info("batchUpdateStatus: ids={}, status={}, updated={}", ids, status, updatedCount);
        return updatedCount;
    }

    @Override
    public boolean existsByCategoryCode(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            log.warn("existsByCategoryCode: categoryCode is empty");
            return false;
        }

        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigCategory::getCategoryCode, categoryCode);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByCategoryCode: categoryCode={}, exists={}", categoryCode, exists);
        return exists;
    }

    @Override
    public List<ConfigCategory> findByCategoryNameLike(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            log.warn("findByCategoryNameLike: categoryName is empty");
            return List.of();
        }

        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ConfigCategory::getCategoryName, categoryName)
                .eq(ConfigCategory::getStatus, 1)
                .orderByAsc(ConfigCategory::getSortOrder)
                .orderByAsc(ConfigCategory::getId);

        List<ConfigCategory> result = this.list(wrapper);
        log.info("findByCategoryNameLike: categoryName={}, count={}", categoryName, result.size());
        return result;
    }

    @Override
    public List<ConfigCategory> findBySortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            log.warn("findBySortOrder: sortOrder is null");
            return List.of();
        }

        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigCategory::getSortOrder, sortOrder)
                .eq(ConfigCategory::getStatus, 1)
                .orderByAsc(ConfigCategory::getId);

        List<ConfigCategory> result = this.list(wrapper);
        log.info("findBySortOrder: sortOrder={}, count={}", sortOrder, result.size());
        return result;
    }

    @Override
    public Integer getNextSortOrder() {
        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ConfigCategory::getSortOrder)
                .orderByDesc(ConfigCategory::getSortOrder)
                .last("LIMIT 1");

        ConfigCategory lastCategory = this.getOne(wrapper);
        Integer nextOrder = (lastCategory != null && lastCategory.getSortOrder() != null) ? 
                lastCategory.getSortOrder() + 10 : 10;
        
        log.info("getNextSortOrder: nextOrder={}", nextOrder);
        return nextOrder;
    }

    @Override
    @Transactional
    public boolean adjustSortOrder(Long id, String direction) {
        if (id == null || !StringUtils.hasText(direction)) {
            log.warn("adjustSortOrder: id or direction is invalid");
            return false;
        }

        ConfigCategory category = this.getById(id);
        if (category == null) {
            log.error("adjustSortOrder: category not found, id={}", id);
            return false;
        }

        Integer currentOrder = category.getSortOrder();
        if (currentOrder == null) {
            log.warn("adjustSortOrder: current sortOrder is null");
            return false;
        }

        ConfigCategory targetCategory = null;
        LambdaQueryWrapper<ConfigCategory> wrapper = new LambdaQueryWrapper<>();
        
        if ("up".equalsIgnoreCase(direction)) {
            // 向上调整：找到排序顺序小于当前的最大值
            wrapper.lt(ConfigCategory::getSortOrder, currentOrder)
                   .orderByDesc(ConfigCategory::getSortOrder)
                   .last("LIMIT 1");
        } else if ("down".equalsIgnoreCase(direction)) {
            // 向下调整：找到排序顺序大于当前的最小值
            wrapper.gt(ConfigCategory::getSortOrder, currentOrder)
                   .orderByAsc(ConfigCategory::getSortOrder)
                   .last("LIMIT 1");
        } else {
            log.warn("adjustSortOrder: invalid direction: {}", direction);
            return false;
        }

        targetCategory = this.getOne(wrapper);
        if (targetCategory == null) {
            log.warn("adjustSortOrder: no target category found for direction: {}", direction);
            return false;
        }

        // 交换排序顺序
        Integer targetOrder = targetCategory.getSortOrder();
        category.setSortOrder(targetOrder);
        targetCategory.setSortOrder(currentOrder);

        boolean updated = this.updateById(category) && this.updateById(targetCategory);
        log.info("adjustSortOrder: id={}, direction={}, updated={}", id, direction, updated);
        return updated;
    }

    @Override
    public Object getConfigCategoryStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计总数
        long totalCount = this.count();
        statistics.put("total", totalCount);

        // 统计启用数量
        LambdaQueryWrapper<ConfigCategory> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(ConfigCategory::getStatus, 1);
        long enabledCount = this.count(enabledWrapper);
        statistics.put("enabled", enabledCount);

        // 统计禁用数量
        LambdaQueryWrapper<ConfigCategory> disabledWrapper = new LambdaQueryWrapper<>();
        disabledWrapper.eq(ConfigCategory::getStatus, 0);
        long disabledCount = this.count(disabledWrapper);
        statistics.put("disabled", disabledCount);

        log.info("getConfigCategoryStatistics: statistics={}", statistics);
        return statistics;
    }
}
