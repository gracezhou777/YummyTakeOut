package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*") //所有i请求都拦截
@Slf4j
public class LoginCheckFilter implements Filter {
  //路经匹配器，支持通配符
  public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    //1.获取本次请求URI
    String requestURI = request.getRequestURI();
    log.info("拦截到请求： {}", requestURI);

    //2.判断本次请求是否需要处理
    //定义不需要处理的请求路经
    String[] urls = new String[]{
        "/employee/login",
        "/employee/logout",
        "/backend/**",
        "/front/**",
        "/common/**",
        "/user/sendMsg",
        "/user/login"
    };
    boolean check = check(urls, requestURI);

    //3.如果不需要处理，直接放行
    if (check) {
      log.info("本次请求{}不需要处理", requestURI);
      filterChain.doFilter(request, response);
      return;
    }

    //4-1.需要处理，判断登录状态。如果已登录，则直接放行 - web端
    if (request.getSession().getAttribute("employee") != null){
      log.info("用户已登录，用户ID为{}", request.getSession().getAttribute("employee"));
      Long empId = (Long) request.getSession().getAttribute("employee");
      BaseContext.setCurrentId(empId);

      filterChain.doFilter(request, response);
      return;
    }

    //4-2.需要处理，判断登录状态。如果已登录，则直接放行 - 移动端
    if (request.getSession().getAttribute("user") != null){
      log.info("用户已登录，用户ID为{}", request.getSession().getAttribute("user"));
      Long userId = (Long) request.getSession().getAttribute("user");
      BaseContext.setCurrentId(userId);

      filterChain.doFilter(request, response);
      return;
    }

    //5.如果未登录则返回未登录结果，通过输出流方式向客户端页面响应数据
    log.info("用户未登录");
    response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN"))); //NOTLOGIN是和前端对应的，这儿不能乱写
    return;
  }

  /**
   * 路经匹配，检查本次请求是否需要放行
   * @param urls
   * @param requestURI
   * @return
   */
  public boolean check(String[] urls, String requestURI){
    for (String url : urls){
      boolean match = PATH_MATCHER.match(url, requestURI);
      if (match) return true;
    }
    return false;
  }
}
