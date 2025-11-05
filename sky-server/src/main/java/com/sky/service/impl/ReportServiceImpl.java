package com.sky.service.impl;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //存放begin到end的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历begin到end的所有日期，将其添加到dateList中
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        // 遍历dateList，查询每个日期对应的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额，营业额是指状态为已完成的订单的金额总和
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        // 拼接日期列表，格式为：yyyy-MM-dd,yyyy-MM-dd,yyyy-MM-dd
        String dateListStr = StringUtils.join(dateList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放begin到end的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历begin到end的所有日期，将其添加到dateList中
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        // 遍历dateList，查询每个日期对应的用户统计信息
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询date日期对应的用户统计信息
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();

            map.put("end", endTime);
            // 查询date日期对应的用户总量
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);
            newUserList.add(newUser == null ? 0 : newUser);

        }
        // 拼接日期列表，格式为：yyyy-MM-dd,yyyy-MM-dd,yyyy-MM-dd
        String dateListStr = StringUtils.join(dateList, ",");
        return UserReportVO.builder()
                .dateList(dateListStr)
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }
    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 存放begin到end的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        // 遍历begin到end的所有日期，将其添加到dateList中
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        // 遍历dateList，查询每个日期对应的订单统计信息
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询date日期对应的订单统计信息
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 查询date日期对应的订单总量
            Integer orderCount = getOrderCount(null, beginTime, endTime);
            orderCountList.add(orderCount);
            // 查询date日期对应的有效订单量
            Integer validOrderCount = getOrderCount(Orders.COMPLETED, beginTime, endTime);
            validOrderCountList.add(validOrderCount);

        }
        // 拼接日期列表，格式为：yyyy-MM-dd,yyyy-MM-dd,yyyy-MM-dd
        String dateListStr = StringUtils.join(dateList, ",");
        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(orderCountList.stream().reduce(Integer::sum).get())
                .validOrderCount(validOrderCountList.stream().reduce(Integer::sum).get())
                .orderCompletionRate(orderCountList.stream().reduce(Integer::sum).get() == 0 ? 0.0 :
                        (double) validOrderCountList.stream().reduce(Integer::sum).get() / orderCountList.stream().reduce(Integer::sum).get())
                .build();
    }
    /**
     * 根据状态和下单时间查询订单数量
     * @param status
     * @param begin
     * @param end
     * @return
     */
    private Integer getOrderCount(Integer status, LocalDateTime begin, LocalDateTime end) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        // 查询date日期对应的订单总量
        Integer orderCount = orderMapper.countByMap(map);
        return orderCount == null ? 0 : orderCount;
    }
}
