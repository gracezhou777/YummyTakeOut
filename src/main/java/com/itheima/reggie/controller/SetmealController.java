package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

  @Autowired
  private SetmealService setmealService;

  @Autowired
  private SetmealDishService setmealDishService;
  
  @Autowired
  private CategoryService categoryService;

  /**
   * 新增套餐
   * @param setmealDto
   * @return
   */
  @PostMapping
  @CacheEvict(value = "setmealCache", allEntries = true)
  public R<String> save(@RequestBody SetmealDto setmealDto){
    log.info("套餐信息：{}", setmealDto);
    setmealService.saveWithDish(setmealDto);
    return R.success("新增套餐成功");
  }

  /**
   * 套餐分页查询
   * @param page
   * @param pageSize
   * @param name
   * @return
   */
  @GetMapping("/page")
  public R<Page> page(int page, int pageSize, String name){
    Page<Setmeal> pageInfo = new Page<>(page, pageSize);
    Page<SetmealDto> dtoPage = new Page<>(); //为了categoryName


    LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.like(name != null, Setmeal::getName, name);
    queryWrapper.orderByDesc(Setmeal::getUpdateTime);
    setmealService.page(pageInfo, queryWrapper);

    //对象拷贝
    BeanUtils.copyProperties(pageInfo, dtoPage, "records");
    List<Setmeal> records = pageInfo.getRecords();

    List<SetmealDto> list = records.stream().map((item) ->{
      SetmealDto setmealDto = new SetmealDto();
      BeanUtils.copyProperties(item, setmealDto);

      Long categoryId = item.getCategoryId();
      Category category = categoryService.getById(categoryId);
      if (category != null){
        String categoryName = category.getName();
        setmealDto.setCategoryName(categoryName);
      }
      return setmealDto;
    }).collect(Collectors.toList());

    dtoPage.setRecords(list);
    return R.success(dtoPage);
  }

  /**
   * 删除套餐
   * @param ids
   * @return
   */
  @DeleteMapping
  @CacheEvict(value = "setmealCache", allEntries = true)
  public R<String> deleteByIds(@RequestParam List<Long> ids){
    log.info("删除套餐ids:{}", ids);
    setmealService.removeWithDish(ids);
    return R.success("删除套餐成功");
  }

  /**
   * 根据条件查询套餐数据
   * @param setmeal
   * @return
   */
  @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
  @GetMapping("/list")
  public R<List<Setmeal>> list(Setmeal setmeal){
    LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
    queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
    queryWrapper.orderByDesc(Setmeal::getUpdateTime);

    List<Setmeal> list = setmealService.list(queryWrapper);
    return R.success(list);
  }
//  @GetMapping("/list")
//  public R<List<Setmeal>> list(Map map){
//    log.info(map.toString());
//    LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
//    queryWrapper.eq(map.get("categoryId") != null, Setmeal::getCategoryId, map.get("categoryId"));
//    queryWrapper.eq(map.get("status") != null, Setmeal::getStatus, map.get("status"));
//    queryWrapper.orderByDesc(Setmeal::getUpdateTime);
//
//    List<Setmeal> list = setmealService.list(queryWrapper);
//    log.info(list.toString());
//    return R.success(list);
//  }
}
