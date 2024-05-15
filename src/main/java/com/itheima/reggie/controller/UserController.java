package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

  @Autowired
  private UserService userService;

  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 发送手机短信验证码
   * @param session
   * @param user
   * @return
   */
  @PostMapping("/sendMsg")
  public R<String> sendMsg(HttpSession session, @RequestBody User user){
    //1.获取手机号
    String phone = user.getPhone();
    if (StringUtils.isNotEmpty(phone)){
      //2.生成随机四位验证码
      String code = ValidateCodeUtils.generateValidateCode(4).toString();
      log.info("code= :{}", code);

      //3.调用阿里云提供的短信服务来发送短信
      //SMSUtils.sendMessage("瑞吉外卖", "", phone, code);

//      //4.将生成的验证码保存到session
//      session.setAttribute(phone, code);

      //4.将将生成的验证码保存到redis中，并设置有效时间5分钟
      redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
      return R.success("手机验证码发送成功");
    } else {
      return R.error("短信发送失败");
    }
  }

  /**
   * 移动端用户登录
   * @param session
   * @param map
   * @return
   */
  @PostMapping("/login")
  public R<User> login(HttpSession session, @RequestBody Map map){
    log.info(map.toString());

    //1.获取手机号
    String phone = map.get("phone").toString();

    //2.获取验证码
    String code = map.get("code").toString();

//    //3.从session获取保存的验证码
//    Object codeInSession = session.getAttribute(phone);

    //3.从redis中获取缓存的验证码
    Object codeInSession = redisTemplate.opsForValue().get(phone);

    //4.进行比对,比对成功，登录成功
    if (codeInSession != null && codeInSession.equals(code)){
      //判断手机号对应的用户是否为新用户
      LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
      queryWrapper.eq(User::getPhone, phone);
      User user = userService.getOne(queryWrapper);
      //如果是新用户，自动完成注册
      if (user == null){
        user = new User();
        user.setPhone(phone);
        user.setStatus(1);
        userService.save(user);
      }
      session.setAttribute("user", user.getId());

      //登录成功，删除缓存中的验证码
      redisTemplate.delete(phone);

      return R.success(user);
    }

    //登录失败
    return R.error("登录失败");
  }

}
