package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

  @Autowired
  private ShoppingCartService shoppingCartService;

  /**
   * 添加购物车
   * @param shoppingCart
   * @return
   */
  @PostMapping("/add")
  public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
    log.info("购物车数据：{}", shoppingCart);

    //1.设置userId，（购物车没传），指定当前是哪个用户的购物车数据
    Long currentId = BaseContext.getCurrentId();
    shoppingCart.setUserId(currentId);

    LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
    lambdaQueryWrapper.eq(ShoppingCart::getUserId, currentId);

    //2.查询当前添加的菜品/套餐是否已在购物车中
    if (shoppingCart.getDishId() != null) {
      //添加的是菜品
      lambdaQueryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
    } else {
      //添加的是套餐
      lambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
    }

    //SQL: select * from shopping_cart where user_id = ? and dish_id = ?
    //or:  select * from shopping_cart where user_id = ? and setmeal_id = ?
    ShoppingCart cartServiceOne = shoppingCartService.getOne(lambdaQueryWrapper);

    if (cartServiceOne != null){
      //是 -> 原来的数据基础上加一
      Integer number = cartServiceOne.getNumber();
      cartServiceOne.setNumber(number + 1);
      shoppingCartService.updateById(cartServiceOne);
      return R.success(cartServiceOne);
    } else {
      //否 -> 添加到购物车，数量默认为1
      shoppingCart.setNumber(1);
      shoppingCart.setCreateTime(LocalDateTime.now());
      shoppingCartService.save(shoppingCart);
      return R.success(shoppingCart);
    }
  }

  /**
   * 查看当前用户的购物车
   * @return
   */
  @GetMapping("/list")
  public R<List<ShoppingCart>> list(){
    log.info("查看购物车");
    LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
    queryWrapper.orderByAsc(ShoppingCart::getCreateTime);
    List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
    return R.success(list);
  }

  /**
   * 清空购物车
   * @return
   */
  @DeleteMapping("/clean")
  public R<String> clean(){
    //delete from shopping_cart where user_id = ?
    LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
    shoppingCartService.remove(queryWrapper);
    return R.success("清空购物车成功");
  }
}
