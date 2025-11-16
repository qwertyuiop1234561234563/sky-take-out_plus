package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.sky.utils.RandomTtlUtil.getRandomTtl;


@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl implements DishService {

        private final DishFlavorMapper dishFlavorMapper;
        private final DishMapper dishMapper;
        private final SetmealDishMapper setmealDishMapper;
        private final StringRedisTemplate stringRedisTemplate;
        private final RedissonClient redissonClient;


    private static final String DISH_KEY_PREFIX = "dish:";
    private static final String NULL_VALUE = "NULL";
    private static final long NULL_CACHE_TTL = 300;
    private static final String LOCK_KEY_PREFIX = "lock:dish:";

        /**
         * 新增菜品
         * @param dishDTO
         */
        @Transactional
        @Override
        public void saveWithFlavor(DishDTO dishDTO) {

            //向菜品表插入1条数据
            Dish dish = new Dish();
            BeanUtils.copyProperties(dishDTO, dish);
            dishMapper.insert(dish);
            String cacheKey = DISH_KEY_PREFIX + dish.getId();
            stringRedisTemplate.delete(cacheKey);
            //获取insert语句生成的主键值
            long dishId = dish.getId();
            //向口味表插入n条数据
            List<DishFlavor> flavors = dishDTO.getFlavors();
            if(flavors!=null&&flavors.size()>0){
                flavors.forEach(dishFlavor->{
                    dishFlavor.setDishId(dishId);
                });
                dishFlavorMapper.insertBatch(flavors);
            }

        }

        @Override
        public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
            PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
            Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
            return new PageResult(page.getTotal(), page.getResult());
        }
        /**
         * 批量删除菜品
         * @param ids
         */
        @Transactional
        @Override
        public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除----是否存在起售中的商品
        for(Long id:ids){
            Dish dish = dishMapper.getById(id);
            if( dish.getStatus()== StatusConstant.ENABLE){
                //当前菜品下存在起售中的商品，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前菜品是否能够删除----是否被套餐关联
        //根据菜品id查询套餐id
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            //当前菜品下存在被套餐关联，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        //删除菜品表中的数据
//        for(Long id:ids){
//            dishMapper.deleteById(id);
//            //删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
            //优化
            //删除菜品表中的数据
            dishMapper.deleteByIds(ids);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishIds(ids);



    }

    /**
     * 根据id查询菜品和口味
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        String cacheKey = DISH_KEY_PREFIX + id;
        String lockKey = LOCK_KEY_PREFIX + id;


        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            if (NULL_VALUE.equals(cacheValue)) {
                log.info("命中空值缓存，菜品ID: {}", id);
                return null;
            }
            Dish dish = JSON.parseObject(cacheValue, Dish.class);
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setFlavors(flavors);
            return dishVO;
        }
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(100, 3000, TimeUnit.MILLISECONDS);
            if (locked) {
                cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cacheValue != null) {
                    Dish dish = JSON.parseObject(cacheValue, Dish.class);
                    List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
                    //将查询到的菜品和口味封装到DishVO中
                    DishVO dishVO = new DishVO();
                    BeanUtils.copyProperties(dish, dishVO);
                    dishVO.setFlavors(flavors);
                    return dishVO;
                }
                Dish dish = dishMapper.getById(id);
                String jsonString = JSON.toJSONString(dish);
                if (dish == null) {
                    // 缓存空对象，短TTL
                    stringRedisTemplate.opsForValue().set(cacheKey, NULL_VALUE,
                            NULL_CACHE_TTL, TimeUnit.SECONDS);
                    log.info("缓存空对象，菜品ID: {}", id);
                } else {
                    stringRedisTemplate.opsForValue().set(cacheKey, jsonString,
                            getRandomTtl(), TimeUnit.SECONDS);
                }
                List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
                DishVO dishVO = new DishVO();
                BeanUtils.copyProperties(dish, dishVO);
                dishVO.setFlavors(flavors);
                return dishVO;
            }else {
                log.info("获取锁失败，等待后重试，菜品ID: {}", id);
                Thread.sleep(50);
                return getByIdWithFlavor(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁失败", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
        /**
         * 更新菜品
         * @param dishDTO
         */
        @Transactional
        @Override
        public void updateWithFlavor(DishDTO dishDTO) {
            //向菜品表更新1条数据
            Dish dish = new Dish();
            BeanUtils.copyProperties(dishDTO, dish);
            dishMapper.update(dish);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(dishDTO.getId());
            //向口味表插入n条数据
            List<DishFlavor> flavors = dishDTO.getFlavors();
            if(flavors!=null&&flavors.size()>0){
                flavors.forEach(dishFlavor->{
                    dishFlavor.setDishId(dishDTO.getId());
                });
                dishFlavorMapper.insertBatch(flavors);
            }
        }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);
        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish dish1 : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish1, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(dish1.getId());
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;

    }

    @Override
    public void startOrStop(Integer status, Long id) {
            Dish dish = Dish.builder().status(status).id(id).build();
        dishMapper.update(dish);

    }
}
