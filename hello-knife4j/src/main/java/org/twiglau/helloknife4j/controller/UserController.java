package org.twiglau.helloknife4j.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.twiglau.helloknife4j.entity.User;

@RestController
@RequestMapping("/user")
@Tag(name = "用户信息管理")
public class UserController {

    @GetMapping("getByName")
    @Operation(summary = "根据用户名称获取用户信息")
   public User getByName(
           @Parameter(description = "参数用户名称")
           @RequestParam(value = "name", required = false)
           String name
    ) {
      User user = new User();
      user.setName(name);
      return user;
   }
}
