package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.sun.org.apache.bcel.internal.generic.LMUL;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
  @Autowired
  private DishFlavorService dishFlavorService;

  /**
   * 新增菜品，同时保存口味数据
   * @param dishDto
   */
  @Transactional //两张表操作，加入事务控制
  @Override
  public void saveWithFlavor(DishDto dishDto) {
    //保存菜品的基本信息道菜品表dish
    this.save(dishDto);

    //保存菜品口味数据之前先要找到菜品ID
    Long dishId = dishDto.getId();
    //得到菜品口味，并遍历
    List<DishFlavor> flavors = dishDto.getFlavors();
    flavors.stream().map((item) -> {
      item.setDishId(dishId);
      return item;
    }).collect(Collectors.toList());
    //保存菜品口味数据到dish_flavor
    dishFlavorService.saveBatch(flavors);
  }

  /**
   * 根据ID查询菜品信息和对应的口味信息
   * @param id
   * @return
   */
  @Override
  public DishDto getByIdWithFlavor(Long id) {
    //查询菜品基本信息，从dish表查询
    Dish dish = this.getById(id);

    DishDto dishDto = new DishDto();
    BeanUtils.copyProperties(dish, dishDto);

    //查询菜品对应口味信息，从dish_flavor查询
    LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(DishFlavor::getDishId, id);
    List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
    dishDto.setFlavors(flavors);
    return dishDto;
  }

  /**
   * 更新菜品信息和对应的口味信息
   * @param dishDto
   */
  @Override
  @Transactional
  public void updateWithFlavor(DishDto dishDto) {
    //更新dish表基本信息
    this.updateById(dishDto);

    //清理当前菜品对应dish_flavor表中的数据，delete操作
    LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper();
    queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
    dishFlavorService.remove(queryWrapper); //delete操作

    //更新dish_flavor表，重新添加，insert操作
    List<DishFlavor> flavors = dishDto.getFlavors();
    flavors.stream().map((item) -> {
      item.setDishId(dishDto.getId());
      return item;
    }).collect(Collectors.toList());
    //保存菜品口味数据到dish_flavor
    dishFlavorService.saveBatch(flavors);
  }
}
