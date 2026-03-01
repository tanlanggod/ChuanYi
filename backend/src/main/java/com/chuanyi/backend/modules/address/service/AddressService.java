package com.chuanyi.backend.modules.address.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.modules.address.entity.UserAddressEntity;
import com.chuanyi.backend.modules.address.mapper.UserAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddressService {

    private final UserAddressMapper userAddressMapper;

    public AddressService(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    public Map<String, Object> list() {
        Long userId = UserContext.currentUserId();
        List<UserAddressEntity> addresses = userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getUserId, userId)
                        .orderByDesc(UserAddressEntity::getIsDefault, UserAddressEntity::getId)
        );
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("addresses", addresses);
        return data;
    }

    public Map<String, Object> save(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        Long id = parseLong(body.get("id"));

        UserAddressEntity entity;
        if (id == null) {
            entity = new UserAddressEntity();
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUserId(userId);
        } else {
            entity = userAddressMapper.selectById(id);
            if (entity == null || !userId.equals(entity.getUserId())) {
                entity = new UserAddressEntity();
                entity.setId(id);
                entity.setUserId(userId);
                entity.setCreatedAt(LocalDateTime.now());
            }
        }

        entity.setReceiverName(parseText(body.get("receiverName")));
        entity.setReceiverPhone(parseText(body.get("receiverPhone")));
        entity.setProvince(parseText(body.get("province")));
        entity.setCity(parseText(body.get("city")));
        entity.setDistrict(parseText(body.get("district")));
        entity.setDetailAddress(parseText(body.get("detailAddress")));
        entity.setIsDefault(parseBoolean(body.get("isDefault")) ? 1 : 0);
        entity.setUpdatedAt(LocalDateTime.now());

        if (entity.getId() == null) {
            userAddressMapper.insert(entity);
        } else {
            userAddressMapper.updateById(entity);
        }

        if (Integer.valueOf(1).equals(entity.getIsDefault())) {
            userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddressEntity>()
                    .eq(UserAddressEntity::getUserId, userId)
                    .ne(UserAddressEntity::getId, entity.getId())
                    .set(UserAddressEntity::getIsDefault, 0)
                    .set(UserAddressEntity::getUpdatedAt, LocalDateTime.now()));
        }
        ensureHasDefault(userId, entity.getId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        return data;
    }

    public Boolean delete(Long id) {
        Long userId = UserContext.currentUserId();
        UserAddressEntity current = userAddressMapper.selectOne(
                new LambdaQueryWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getId, id)
                        .eq(UserAddressEntity::getUserId, userId)
        );
        if (current == null) {
            return Boolean.TRUE;
        }
        boolean deletedDefault = Integer.valueOf(1).equals(current.getIsDefault());
        userAddressMapper.delete(new LambdaQueryWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getId, id)
                .eq(UserAddressEntity::getUserId, userId));
        if (deletedDefault) {
            ensureHasDefault(userId, null);
        }
        return Boolean.TRUE;
    }

    private void ensureHasDefault(Long userId, Long preferredAddressId) {
        Long defaultCount = userAddressMapper.selectCount(
                new LambdaQueryWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getUserId, userId)
                        .eq(UserAddressEntity::getIsDefault, 1)
        );
        if (defaultCount != null && defaultCount > 0) {
            return;
        }

        UserAddressEntity target = null;
        if (preferredAddressId != null) {
            target = userAddressMapper.selectOne(
                    new LambdaQueryWrapper<UserAddressEntity>()
                            .eq(UserAddressEntity::getId, preferredAddressId)
                            .eq(UserAddressEntity::getUserId, userId)
            );
        }
        if (target == null) {
            List<UserAddressEntity> list = userAddressMapper.selectList(
                    new LambdaQueryWrapper<UserAddressEntity>()
                            .eq(UserAddressEntity::getUserId, userId)
                            .orderByAsc(UserAddressEntity::getId)
                            .last("LIMIT 1")
            );
            if (list != null && !list.isEmpty()) {
                target = list.get(0);
            }
        }
        if (target == null) {
            return;
        }

        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getId, target.getId())
                .eq(UserAddressEntity::getUserId, userId)
                .set(UserAddressEntity::getIsDefault, 1)
                .set(UserAddressEntity::getUpdatedAt, LocalDateTime.now()));
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.valueOf(text);
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
