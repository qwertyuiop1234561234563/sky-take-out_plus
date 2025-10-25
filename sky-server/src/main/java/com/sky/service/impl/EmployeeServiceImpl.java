package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     *
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
//        //1、将DTO对象转换为实体对象
//        Employee employee = new Employee();
//        employee.setUsername(employeeDTO.getUsername());
//        employee.setPassword(DigestUtils.md5DigestAsHex(employeeDTO.getPassword().getBytes()));
//        employee.setName(employeeDTO.getName());
//        employee.setPhone(employeeDTO.getPhone());
//        employee.setSex(employeeDTO.getSex());
//        employee.setIdNumber(employeeDTO.getIdNumber());
//        employee.setStatus(StatusConstant.ENABLE);

        //对象属性拷贝
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        //设置默认密码为123456，需要进行md5加密处理
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        //设置账号状态为1（启用）0（禁用）默认启用
        employee.setStatus(StatusConstant.ENABLE);
        //设置创建时间和更新时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //设置创建人和更新人
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        //2、调用mapper插入数据
        employeeMapper.insert(employee);
    }
    /**
     * 员工分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //1、调用PageHelper分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        //2、调用mapper查询分页数据
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        //3、返回PageResult对象
        long total = page.getTotal();
        List<Employee> records = page.getResult();
        return new PageResult(total, records);
    }
    /**
     * 启用禁用员工账号
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
//  1
//        Employee employee = new Employee();
//        employee.setId(id);
//        employee.setStatus(status);
//  2
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(employee);
    }
    /**
     * 根据id查询员工
     *
     * @param id
     * @return
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("****");
        return employee;
    }
    /**
     * 更新员工信息
     *
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        //1、将DTO对象转换为实体对象
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        //设置更新时间和更新人
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        //2、调用mapper更新数据
        employeeMapper.update(employee);
    }

}
