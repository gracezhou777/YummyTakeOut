package com.itheima.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 自定义元数据对象处理器
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

  /**
   * 插入操作自动填充
   * @param metaObject
   */
  @Override
  public void insertFill(MetaObject metaObject) {
    log.info("公共字段自动填充[insert]....");
    log.info(metaObject.toString());

    //设置时间
    metaObject.setValue("createTime", LocalDateTime.now());
    metaObject.setValue("updateTime", LocalDateTime.now());
    //设置当前登录用户的ID
    metaObject.setValue("createUser", BaseContext.getCurrentId());
    metaObject.setValue("updateUser", BaseContext.getCurrentId());
  }

  /**
   * 修改操作自动填充
   * @param metaObject
   */
  @Override
  public void updateFill(MetaObject metaObject) {
    log.info("公共字段自动填充[update]....");
    log.info(metaObject.toString());
    //设置时间
    metaObject.setValue("updateTime", LocalDateTime.now());
    //设置当前登录用户的ID
    metaObject.setValue("updateUser", BaseContext.getCurrentId());
  }
}
