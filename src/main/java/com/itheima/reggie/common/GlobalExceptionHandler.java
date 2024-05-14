package com.itheima.reggie.common;

import java.sql.SQLIntegrityConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局异常处理
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class}) //拦截哪些controller
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 异常处理方法
   * @return
   */
  @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
  public R<String> exceptionHanlder(SQLIntegrityConstraintViolationException ex){
    log.error(ex.getMessage());

    if (ex.getMessage().contains("Duplicate entry")){
      //Duplicate entry 'asdfasdfa' for key 'employee.idx_username'
      String[] split = ex.getMessage().split(" ");
      String msg = split[2] + "已存在";
      return R.error(msg);
    } else{
      String msg = "未知错误";
      return R.error(msg);
    }
  }

  /**
   * 异常处理方法
   * @return
   */
  @ExceptionHandler(CustomException.class)
  public R<String> exceptionHanlder(CustomException ex){
    log.error(ex.getMessage());
    return R.error(ex.getMessage());
  }

}
