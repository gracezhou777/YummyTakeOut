package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类的管理
 */
@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

  @Autowired
  private CategoryService categoryService;

  @PostMapping
  public R<String> save(@RequestBody Category category){
    log.info("category:{}", category);
    categoryService.save(category);
    return R.success("新增分类成功");
  }

  /**
   * 分页查询
   * @param page
   * @param pageSize
   * @return
   */
  @GetMapping("/page")
  public R<Page> page(int page, int pageSize){
    //分页构造器
    Page<Category> pageInfo = new Page<>(page, pageSize);

    //条件构造器，设置排序
    LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
    lambdaQueryWrapper.orderByAsc(Category::getSort);

    //进行分页查询
    categoryService.page(pageInfo,lambdaQueryWrapper);
    return R.success(pageInfo);
  }

  /**
   * 根据ID删除分类
   * @param ids
   * @return
   */
  @DeleteMapping
  public R<String> delete(Long ids){
    log.info("删除分类，id为：{}", ids);

    // 普通写法，没有关联菜品和套餐
    // categoryService.removeById(id); //继承自IService

    categoryService.remove(ids);

    return R.success("分类信息删除成功");
  }

  /**
   * 根据ID来修改分类信息
   * @param category
   * @return
   */
  @PutMapping
  public R<String> update(@RequestBody Category category){
    log.info("修改分类信息:{}", category);
    categoryService.updateById(category);
    return R.success("修改分类成功");
  }

  /**
   * 根据条件查询分类数据，下拉框
   * @param category
   * @return
   */
  @GetMapping("/list")
  public R<List<Category>> list(Category category){
    //条件构造器
    LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
    //添加条件
    queryWrapper.eq(category.getType() != null, Category::getType, category.getType());
    //添加排序条件
    queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

    List<Category> list = categoryService.list(queryWrapper);
    return R.success(list);
  }


}
