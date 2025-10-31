package com.sky.service.impl;

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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

        @Autowired
        private DishFlavorMapper dishFlavorMapper;
        @Autowired
        private DishMapper dishMapper;
        @Autowired
        private SetmealDishMapper setmealDishMapper;
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
        //根据id查询菜品
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        //将查询到的菜品和口味封装到DishVO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
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
