package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    @Transactional
    @Override
    public void save(SetmealDTO setmealDTO) {
        //将SetmealDTO转换为Setmeal实体类
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //向套餐表插入1条数据
        setmealMapper.insert(setmeal);
        //获取套餐id
        Long setmealId = setmeal.getId();
        //将SetmealDishDTO转换为SetmealDish实体类
        for (SetmealDish setmealDish : setmealDTO.getSetmealDishes()) {
            setmealDish.setSetmealId(setmealId);
        }
        //向套餐菜品关系表插入n条数据
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());
    }

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> pageInfo = setmealMapper.selectPage(setmealPageQueryDTO);
        return new PageResult(pageInfo.getTotal(), pageInfo.getResult());
    }
}
