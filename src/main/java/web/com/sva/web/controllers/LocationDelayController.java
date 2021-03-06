/*
 * 文件名：LocationDelayController.java
 * 版权：Copyright 2013-2016 Huawei Tech. Co. Ltd. All Rights Reserved. 
 * 描述： LocationDelayController.java
 * 修改人：wwx283823
 * 修改时间：2016-3-16
 * 修改内容：新增
 */
package com.sva.web.controllers;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.RequestContext;
import com.sva.common.Util;
import com.sva.common.conf.Params;
import com.sva.dao.LocationDelayDao;
import com.sva.model.LocationDelayModel;

/**
 * 定位延迟测试controller
 * 
 * @author wwx283823
 * @version iSoftStone 2016-3-16
 * @since iSoftStone
 */
@Controller
@RequestMapping(value = "/delay")
public class LocationDelayController
{

    private static final Logger LOG = Logger.getLogger(AccuracyController.class);

    @Autowired
    private LocationDelayDao locationDelayDao;

    @RequestMapping(value = "/api/getTableData", method = {RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> getTableData(HttpServletRequest request)
    {
        Map<String, Object> modelMap = new HashMap<String, Object>(2);
        List<LocationDelayModel> resultList = new ArrayList<LocationDelayModel>(10);
        List<LocationDelayModel> store;
        Object userName = request.getSession().getAttribute("username");
        @SuppressWarnings("unchecked")
        List<String> storeides = (List<String>) request.getSession()
                .getAttribute("storeides");
        if ("admin".equals(userName))
        {

            resultList = locationDelayDao.getAllData();
        }
        else if (!storeides.isEmpty())
        {
            String storeid = storeides.get(0);
            String[] stores = storeid.split(",");
            for (int i = 0; i < stores.length; i++)
            {
                store = locationDelayDao.getAllDataByStoreid(Integer.parseInt(stores[i]));
                if (store != null && !store.isEmpty())
                {
                    resultList.addAll(store);
                }
            }
        }

        modelMap.put("error", null);
        modelMap.put("data", resultList);
        return modelMap;
    }

    
    @RequestMapping(value = "/api/getTableDataNew", method = {RequestMethod.GET})
    @ResponseBody
    public Map<String, Object> getTableDataNew(HttpServletRequest request,
        @RequestParam("placeId") String placeId,
        @RequestParam("startTime") String startTime,
        @RequestParam("endTime") String endTime)
    {
        RequestContext requestContext = new RequestContext(request);
        Locale myLocale = requestContext.getLocale();
        String dataDelayName = Params.dataDelay;
        String positionDelayName = Params.positionDelay;
        String total = Params.total;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        long startTimestamp = 0;
        long endTimestamp = 0;
        try
        {
            startTimestamp = sdf.parse(startTime).getTime();
            endTimestamp = sdf.parse(endTime).getTime();
        }
        catch (ParseException e)
        {
            LOG.debug("shi jian zhuanhuan erro:"+e.getMessage());
        }
        List<LocationDelayModel> res = new ArrayList<LocationDelayModel>(10);
        List<LocationDelayModel> store;
        List<String> listName = new ArrayList<>();
        
        Object userName = request.getSession().getAttribute("username");
        @SuppressWarnings("unchecked")
        List<String> storeides = (List<String>) request.getSession()
                .getAttribute("storeides");
        if (("admin").equals(userName))
        {
            if (placeId!="")
            {
                res = locationDelayDao.getDataByPlaceIdTime(startTimestamp,endTimestamp,placeId);
            }else
            {
                res = locationDelayDao.getAllDatas(startTimestamp,endTimestamp);
            }
        }
        else
        {
            if (placeId!="")
            {
                if (!storeides.isEmpty())
                {
                    String storeid = storeides.get(0);
                    String[] stores = storeid.split(",");
                    for (int i = 0; i < stores.length; i++)
                    {
                        store = locationDelayDao.getDataByPlaceIdTime(startTimestamp, endTimestamp,stores[i]);
                        if (store != null)
                        {
                            if (res == null)
                            {
                                res = store;
                            }
                            else if (!store.isEmpty())
                            {
                                res.addAll(store);
                            }
                        }
                    }
                }
            }else
            {
                if (!storeides.isEmpty())
                {
                    String storeid = storeides.get(0);
                    String[] stores = storeid.split(",");
                    for (int i = 0; i < stores.length; i++)
                    {
                        store = locationDelayDao.getDataByPlaceIdTime(startTimestamp,endTimestamp,stores[i]);
                        if (store != null)
                        {
                            if (res == null)
                            {
                                res = store;
                            }
                            else if (!store.isEmpty())
                            {
                                res.addAll(store);
                            }
                        }
                    }
                }
            }
        }
        if (!Locale.CHINA.equals(myLocale))
        {
            dataDelayName = "dataDelay";
            positionDelayName = "positionDelay";
            total = "total";
        }
        listName.add(dataDelayName);
        listName.add(positionDelayName);
        
        Map<String, Object> modelMap = Util.getCDFLocationDelay(listName, res,total);
        modelMap.put("data", res);
        modelMap.put("CDFName", listName);

        return modelMap;
        
     
    }  
    
    /*
     * 导出excel Time：2015/10/12 lwx274026
     */
    @RequestMapping(value = "/api/exportCodeTemplate")
    @ResponseBody
    public void exportCodeTemplate(HttpServletRequest request,
            HttpServletResponse response)
    {
        RequestContext requestContext = new RequestContext(request);
        Locale myLocale = requestContext.getLocale();

        String logInfo = Params.locationInfo;
        String sheetName = Params.locationName;
        String excelPlace = Params.excelPlace;
        String excelfloor = Params.excelfloor;
        String dataDelay = Params.dataDelay;
        String delayTime = Params.delayTime;
        String positionDelay = Params.positionDelay;

        if (!Locale.CHINA.equals(myLocale))
        {
            logInfo = "No locationDelay information, please add locationDelay information, please add...";
            sheetName = "locationDelay information";
            excelPlace = "place";
            excelfloor = "floor";
            dataDelay = "dataDelay";
            positionDelay = "positionDelay";
            delayTime = "uploadTime";
        }
        Cell placeTitle = null;
        Cell floorTitle = null;
        Cell xTitle = null;
        Cell yTitle = null;
        Cell startDateTitle = null;
        Cell typeTitle = null;
        try
        {
            // 1,得到所有的精度汇总信息
            List<LocationDelayModel> summaryInformation = locationDelayDao
                    .getAllData();
            if (summaryInformation == null || summaryInformation.isEmpty())
            {
                LOG.info(logInfo);
            }
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle titleStyle = getCellStyle(workbook, 0);
            CellStyle contentStyle = getCellStyle(workbook, 1);
            // 创建第一行的（标题）样式
            Row row = sheet.getRow(0);
            if (row == null)
            {
                row = sheet.createRow(0);
                // 第一行第1列显示地点

                exportTitle(titleStyle, excelPlace, placeTitle, row, 0);

                // 第一行第2列显示楼层

                exportTitle(titleStyle, excelfloor, floorTitle, row, 1);
                // 第一行第三列显示坐标X米

                exportTitle(titleStyle, dataDelay, xTitle, row, 2);
                // 第一行第四列显示坐标y米

                exportTitle(titleStyle, positionDelay, yTitle, row, 3);
                // 第一行第五列显示开始时间

                exportTitle(titleStyle, delayTime, startDateTitle, row, 4);
                // 第一行第六列显示结束时间

                row.setHeight((short) 600);
                // 设置特性列宽度为10000
                sheet.setColumnWidth(0, 2000);
                sheet.setColumnWidth(1, 2000);
                sheet.setColumnWidth(2, 3000);
                sheet.setColumnWidth(3, 3000);
                sheet.setColumnWidth(4, 5000);

            }
            // 从第二行开始遍历所有的数据写入excel内容中
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            LocationDelayModel accuracyModel;
            int sumSize = summaryInformation.size();
            for (int i = 1; i < sumSize + 1; i++)
            {
                accuracyModel = summaryInformation.get(i - 1);

                row = sheet.getRow(i);
                if (row == null)
                {
                    row = sheet.createRow(i);
                }
                typeTitle = inputValue(placeTitle, floorTitle, xTitle, yTitle,
                        startDateTitle, typeTitle, contentStyle, row, sdf, accuracyModel);
            }

            response.reset();
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");
            response.setHeader("Content-disposition", "attachment; filename="
                    + URLEncoder.encode(positionDelay, "utf-8") + ""
                    + new SimpleDateFormat("yyyyMMdd").format(new Date())
                    + ".xlsx");// 设定输出文件头
            workbook.write(response.getOutputStream());
        }
        catch (Exception e)
        {
            LOG.error("Exception.", e);
        }
    }

    private Cell inputValue(Cell placeTitle, Cell floorTitle, Cell xTitle,
            Cell yTitle, Cell startDateTitle, Cell typeTitle, 
            CellStyle contentStyle, Row row, SimpleDateFormat sdf,
            LocationDelayModel accuracyModel)
    {
        // 第i行第0列赋值地点值

        exportContent(contentStyle, placeTitle, row, 0,
                accuracyModel.getStore().getName());

        // 第二行第1列赋值楼层值

        exportContent(contentStyle, floorTitle, row, 1,
                accuracyModel.getMaps().getFloor());
        // 第二行第2列赋值坐标X米值

        exportContent(contentStyle, xTitle, row, 2,
                String.valueOf(accuracyModel.getDataDelay()));
        // 第二行第3列赋值坐标Y米值

        exportContent(contentStyle, yTitle, row, 3,
                String.valueOf(accuracyModel.getPositionDelay()));
        // 第二行第4列赋值开始时间值

        exportContent(contentStyle, startDateTitle, row, 4,
                sdf.format(accuracyModel.getUpdateTime()));
        // 第二行第5列赋值结束时间值

        return typeTitle;
    }

    /*
     * 设置excel的样式 Time：2015/10/12 lwx274026
     */
    private CellStyle getCellStyle(Workbook workbook, int type)
    {
        Font font;
        CellStyle style = workbook.createCellStyle();
        // 1,设置excel单元格标题
        if (type == 0)
        {
            font = workbook.createFont();
            font.setFontHeightInPoints((short) 14);// 设置字体大小
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
            font.setColor(HSSFColor.BLACK.index);
            style.setWrapText(true);
            style.setBorderBottom(HSSFCellStyle.BORDER_THIN); // 下边框
            style.setBorderLeft(HSSFCellStyle.BORDER_THIN);// 左边框
            style.setBorderTop(HSSFCellStyle.BORDER_THIN);// 上边框
            style.setBorderRight(HSSFCellStyle.BORDER_THIN);// 右边框
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 居中
            style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// 垂直居中
            style.setWrapText(true);
            style.setFillForegroundColor(HSSFColor.PALE_BLUE.index);
            style.setFillPattern(CellStyle.SOLID_FOREGROUND);

        }
        // 设置数据内容单元格样式
        else
        {
            font = workbook.createFont();
            font.setFontHeightInPoints((short) 11);// 设置字体大小
            font.setColor(HSSFColor.BLACK.index);

            style.setWrapText(true);
            style.setBorderBottom(HSSFCellStyle.BORDER_THIN); // 下边框
            style.setBorderLeft(HSSFCellStyle.BORDER_THIN);// 左边框
            style.setBorderTop(HSSFCellStyle.BORDER_THIN);// 上边框
            style.setBorderRight(HSSFCellStyle.BORDER_THIN);// 右边框
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 居中
            style.setFont(font);
        }
        return style;
    }

    private void exportTitle(CellStyle titleStyle, String name, Cell column,
            Row row, int number)
    {
        column = row.getCell(number);
        if (column == null)
        {
            column = row.createCell(number);
        }
        column.setCellStyle(titleStyle);
        column.setCellValue(name);
    }

    private void exportContent(CellStyle titleStyle, Cell column, Row row,
            int number, String name)
    {

        if (column == null)
        {
            column = row.createCell(number);
        }
        column.setCellStyle(titleStyle);
        column.setCellValue(name);

    }

}
