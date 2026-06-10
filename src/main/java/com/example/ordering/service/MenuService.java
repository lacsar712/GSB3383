package com.example.ordering.service;

import com.example.ordering.dao.MenuItemDao;
import com.example.ordering.domain.Role;
import com.example.ordering.domain.dto.MenuItemDTO;
import com.example.ordering.exception.ApiException;
import com.example.ordering.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.List;

public class MenuService {
    private final MenuItemDao menuItemDao;

    public MenuService() {
        this(new MenuItemDao());
    }

    public MenuService(final MenuItemDao menuItemDao) {
        this.menuItemDao = menuItemDao;
    }

    public List<MenuItemDTO> list(final String query, final Role role) {
        boolean includeUnavailable = role == Role.ADMIN;
        return menuItemDao.list(query, includeUnavailable);
    }

    public MenuItemDTO create(
            final String name,
            final String description,
            final String priceRaw,
            final String imageUrl,
            final boolean isAvailable
    ) {
        String validName = ValidationUtil.requireLength("菜品名称", name, 1, 100);
        String validDescription = ValidationUtil.requireLength("菜品描述", description, 1, 500);
        BigDecimal validPrice = ValidationUtil.requirePositiveDecimal("价格", priceRaw);
        String validImage = ValidationUtil.requireLength("图片链接", imageUrl, 1, 255);

        long id = menuItemDao.create(validName, validDescription, validPrice, validImage, isAvailable);
        return menuItemDao.findById(id).orElseThrow(() -> new ApiException(500, "INTERNAL_ERROR", "创建菜品失败"));
    }

    public MenuItemDTO update(
            final long id,
            final String name,
            final String description,
            final String priceRaw,
            final String imageUrl,
            final boolean isAvailable
    ) {
        String validName = ValidationUtil.requireLength("菜品名称", name, 1, 100);
        String validDescription = ValidationUtil.requireLength("菜品描述", description, 1, 500);
        BigDecimal validPrice = ValidationUtil.requirePositiveDecimal("价格", priceRaw);
        String validImage = ValidationUtil.requireLength("图片链接", imageUrl, 1, 255);
        boolean updated = menuItemDao.update(id, validName, validDescription, validPrice, validImage, isAvailable);
        if (!updated) {
            throw new ApiException(404, "NOT_FOUND", "菜品不存在");
        }
        return menuItemDao.findById(id).orElseThrow(() -> new ApiException(500, "INTERNAL_ERROR", "更新菜品失败"));
    }
}
