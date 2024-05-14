package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

  @Autowired
  private SetmealDishService setmealDishService;

  /**
   * 新增套餐，同时保存套餐和菜品的关联关系
   * @param setmealDto
   */
  @Override
  @Transactional
  public void saveWithDish(SetmealDto setmealDto) {
    //保存套餐的基本信息，操作setmeal表，执行insert操作
    this.save(setmealDto);

    //保存套餐和菜品的关联信息，操作setmeal_dish表，执行insert操作
    List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes(); //存的只有dishId, setmealId没有值
    setmealDishes.stream().map((item) -> {
      item.setSetmealId(setmealDto.getId());
      return item;
    }).collect(Collectors.toList());

    setmealDishService.saveBatch(setmealDishes);

  }

  /**
   * 删除套餐，同时删除套餐和菜品的关联关系
   * @param ids
   */
  @Override
  @Transactional
  public void removeWithDish(List<Long> ids) {
    //查询套餐状态，是否为停售
    LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper();
    //select count(*) from setmeal where id in (1, 2, 3) and status = 1;
    queryWrapper.in(Setmeal::getId, ids);
    queryWrapper.eq(Setmeal::getStatus, 1);
    int count = this.count(queryWrapper);//IService的自带方法

    //不能删除，抛出一个业务异常
    if (count > 0){
      throw new CustomException("套餐正在售卖中，不能删除");
    }

    //可以删除，先删除套餐表中的数据--setmeal
    this.removeByIds(ids);

    //删除关系表中的数据--setmeal_dish
    LambdaQueryWrapper<SetmealDish> queryWrapper2 = new LambdaQueryWrapper<>();
    //delete from setmeal_dish where setmeal_id in (1, 2, 3)
    queryWrapper2.in(SetmealDish::getSetmealId, ids);
    setmealDishService.remove(queryWrapper2);
  }
}
