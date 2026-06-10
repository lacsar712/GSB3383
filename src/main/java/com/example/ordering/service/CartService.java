package com.example.ordering.service;

import com.example.ordering.dao.CartDao;
import com.example.ordering.dao.MenuItemDao;
import com.example.ordering.domain.dto.CartDTO;
import com.example.ordering.domain.dto.CartItemDTO;
import com.example.ordering.domain.dto.MenuItemDTO;
import com.example.ordering.exception.ApiException;
import com.example.ordering.util.ValidationUtil;

import java.util.List;

public class CartService {
    private final CartDao cartDao;
    private final MenuItemDao menuItemDao;

    public CartService() {
        this(new CartDao(), new MenuItemDao());
    }

    public CartService(final CartDao cartDao, final MenuItemDao menuItemDao) {
        this.cartDao = cartDao;
        this.menuItemDao = menuItemDao;
    }

    public CartDTO getCart(final long userId) {
        List<CartItemDTO> items = cartDao.listByUser(userId);
        return new CartDTO(items, cartDao.sumTotal(items));
    }

    public CartDTO addItem(final long userId, final long menuItemId, final int quantity) {
        ValidationUtil.requirePositiveInt("数量", quantity);
        MenuItemDTO menuItem = menuItemDao.findById(menuItemId)
                .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "菜品不存在"));
        if (!menuItem.isAvailable()) {
            throw new ApiException(400, "MENU_ITEM_UNAVAILABLE", "菜品已下架，无法加入购物车");
        }
        cartDao.addOrIncrease(userId, menuItemId, quantity);
        return getCart(userId);
    }

    public CartDTO updateQuantity(final long userId, final long cartItemId, final int quantity) {
        if (quantity < 0) {
            throw new ApiException(400, "VALIDATION_ERROR", "数量不能小于 0");
        }
        if (quantity == 0) {
            boolean deleted = cartDao.deleteById(userId, cartItemId);
            if (!deleted) {
                throw new ApiException(404, "NOT_FOUND", "购物车项不存在");
            }
            return getCart(userId);
        }
        boolean updated = cartDao.updateQuantity(userId, cartItemId, quantity);
        if (!updated) {
            throw new ApiException(404, "NOT_FOUND", "购物车项不存在");
        }
        return getCart(userId);
    }

    public CartDTO deleteItem(final long userId, final long cartItemId) {
        boolean deleted = cartDao.deleteById(userId, cartItemId);
        if (!deleted) {
            throw new ApiException(404, "NOT_FOUND", "购物车项不存在");
        }
        return getCart(userId);
    }
}
