package com.sky.service.impl;


import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;


import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final WorkspaceService workspaceService;

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
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // 查询date日期对应的商品销售Top10
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);
        // 拼接商品名称列表，格式为：鱼香肉丝,宫保鸡丁,水煮鱼
        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()), ",");
        // 拼接销量列表，格式为：260,215,200
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()), ",");
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
    /**
     * 导出业务数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库,获取营业数据---查询最近30天的营业数据
        LocalDate beginTime = LocalDate.now().minusDays(30);
        LocalDate endTime = LocalDate.now();
        LocalDateTime beginDateTime = LocalDateTime.of(beginTime, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(endTime, LocalTime.MAX);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginDateTime, endDateTime);
        //2.通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            XSSFSheet sheet = workbook.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间：" + beginTime + "至" + endTime);
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getValidOrderCount());
            row =  sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = beginTime.plusDays(i);
                LocalDateTime dateBeginTime = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime dateEndTime = LocalDateTime.of(date, LocalTime.MAX);
                BusinessDataVO businessDataVO1 = workspaceService.getBusinessData(dateBeginTime, dateEndTime);
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO1.getTurnover());
                row.getCell(3).setCellValue(businessDataVO1.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO1.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO1.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO1.getNewUsers());
            }


            //3.通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.close();
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
