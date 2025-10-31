package com.sky.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.utils.JacksonUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JacksonUtil jacksonUtil;
    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        //构造缓存key
        String key = "dish_" + categoryId;
        //从Redis中查询缓存
        String cacheJson = stringRedisTemplate.opsForValue().get(key);
        List<DishVO> list = jacksonUtil.toGenericObj(cacheJson,new TypeReference<List<DishVO>>(){});
        if(list != null && list.size() > 0) {
            //如果缓存中存在数据，直接返回
            return Result.success(list);
        }
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        //如果不存在，查询数据库
        list = dishService.listWithFlavor(dish);
        String listJson = jacksonUtil.toJson(list);
        stringRedisTemplate.opsForValue().set(key,listJson);

        return Result.success(list);
    }




}
