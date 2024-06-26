package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

  @Autowired
  private DishService dishService;

  @Autowired
  private DishFlavorService dishFlavorService;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 新增菜品
   * @param dishDto
   * @return
   */
  @PostMapping
  public R<String> save(@RequestBody DishDto dishDto){ //不能直接传dish，因为dishFlavor没有在dish里声明
    log.info(dishDto.toString());
    dishService.saveWithFlavor(dishDto);

    //第一种，清理所有菜品的缓存数据
//    Set keys = redisTemplate.keys("dish_*");//所有以dish_开头的key
//    redisTemplate.delete(keys);

    //第二种，精确清理
    String key = "dish_" + dishDto.getCategoryId() + "_1";
    redisTemplate.delete(key);

    return R.success("新增菜品成功");
  }

  /**
   * 菜品信息的分页
   * @param page
   * @param pageSize
   * @param name
   * @return
   */
  @GetMapping("/page")
  public R<Page> page(int page, int pageSize, String name){
    //构造分页构造器
    Page<Dish> pageInfo = new Page<>(page, pageSize);
    Page<DishDto> dishDtoPage = new Page<>();

    //条件构造器
    LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.like(name != null, Dish::getName, name);
    queryWrapper.orderByDesc(Dish::getUpdateTime);
    //执行分页查询
    dishService.page(pageInfo, queryWrapper);

    //对象拷贝
    //不拷贝records对象,因为records对象就是分页的集合，我们要手动处理
    BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");
    //通过流的方式来得到新的records
    List<Dish> records =  pageInfo.getRecords();
    List<DishDto> list = records.stream().map((item) ->{
      DishDto dishDto = new DishDto();
      //拷贝dish信息
      BeanUtils.copyProperties(item, dishDto);
      Long categoryId = item.getCategoryId(); //分类ID
      //根据ID来查询分类对象并在dishDto set好
      Category category = categoryService.getById(categoryId);
      if (category != null){
        String categoryName = category.getName();
        dishDto.setCategoryName(categoryName);
      }
      return dishDto;
    }).collect(Collectors.toList());

    dishDtoPage.setRecords(list);

    return R.success(dishDtoPage);
  }

  /**
   * 根据ID查询菜品信息和对应的口味信息
   * @param id
   * @return
   */
  @GetMapping("/{id}")
  public R<DishDto> get(@PathVariable Long id){
    DishDto dishDto = dishService.getByIdWithFlavor(id);
    return R.success(dishDto);
  }

  /**
   * 修改菜品
   * @param dishDto
   * @return
   */
  @PutMapping
  public R<String> update(@RequestBody DishDto dishDto){
    log.info(dishDto.toString());
    dishService.updateWithFlavor(dishDto);

    //第一种，清理所有菜品的缓存数据
//    Set keys = redisTemplate.keys("dish_*");//所有以dish_开头的key
//    redisTemplate.delete(keys);

    //第二种，精确清理
    String key = "dish_" + dishDto.getCategoryId() + "_1";
    redisTemplate.delete(key);

    return R.success("修改菜品成功");
  }

  /**
   * 根据条件来查询对应的菜品数据
   * @param dish
   * @return
   */
//  @GetMapping("/list")
//  public R<List<Dish>> list(Dish dish){
//    //构造查询条件
//    LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//    queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
//    //查询处于启售状态的
//    queryWrapper.eq(Dish::getStatus, 1);
//    //添加排序条件
//    queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//    //执行查询
//    List<Dish> list = dishService.list(queryWrapper);
//    return R.success(list);
//  }
  @GetMapping("/list")
  public R<List<DishDto>> list(Dish dish){
    List<DishDto> dishDtoList = null;
    //动态构造key
    String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus(); //key会是一个字符串

    //1.Redis中获取缓存数据,多份数据
    dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

    //2.如果存在，直接返回
    if (dishDtoList != null){
      return R.success(dishDtoList);
    }

    //3.如果不存在，需要查询数据库

    //构造查询条件
    LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
    //查询处于启售状态的
    queryWrapper.eq(Dish::getStatus, 1);
    //添加排序条件
    queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
    //执行查询
    List<Dish> list = dishService.list(queryWrapper);
    dishDtoList = list.stream().map((item) ->{
      DishDto dishDto = new DishDto();
      //拷贝dish信息
      BeanUtils.copyProperties(item, dishDto);
      Long categoryId = item.getCategoryId(); //分类ID
      //根据ID来查询分类对象并在dishDto set好
      Category category = categoryService.getById(categoryId);
      if (category != null){
        String categoryName = category.getName();
        dishDto.setCategoryName(categoryName);
      }

      //当前菜品ID
      Long dishId = item.getId();
      LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
      lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
      List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
      dishDto.setFlavors(dishFlavorList);
      return dishDto;
    }).collect(Collectors.toList());

    //将查询到的菜品数据缓存到Redis
    redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
    return R.success(dishDtoList);
  }


}
