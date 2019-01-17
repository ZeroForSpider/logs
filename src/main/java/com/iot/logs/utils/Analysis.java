package com.iot.logs.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * 读取日志的数据类型
 */
class LogInfo {
    private String dateTime;

    private Integer threadNum;

    private String rawInfo;

    public void setRawInfo(String rawInfo) {
        this.rawInfo = rawInfo;
    }

    public String getRawInfo() {
        return rawInfo;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    public String getDateTime() {
        return dateTime;
    }

    public Integer getThreadNum() {
        return threadNum;
    }

    public LogInfo() {
    }

    public LogInfo(String dateTime, Integer threadNum, String rawInfo) {
        this.dateTime = dateTime;
        this.threadNum = threadNum;
        this.rawInfo = rawInfo;
    }

    @Override
    public String toString() {
        return "{" +
                " " + dateTime + '\'' +
                "  " + threadNum +
                '}';
    }
}


/**
 * 写入日志的数据类型
 */
class OutputLogInfo {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(OutputLogInfo.class);

    private LogInfo currentLogInfo;

    private LogInfo previousLogInfo;

    public LogInfo getCurrentLogInfo() {
        return currentLogInfo;
    }

    public LogInfo getPreviousLogInfo() {
        return previousLogInfo;
    }

    public void setCurrentLogInfo(LogInfo currentLogInfo) {
        this.currentLogInfo = currentLogInfo;
    }

    public void setPreviousLogInfo(LogInfo previousLogInfo) {
        this.previousLogInfo = previousLogInfo;
    }

    public OutputLogInfo() {
    }

    public OutputLogInfo(LogInfo currentLogInfo, LogInfo previousLogInfo) {
        this.currentLogInfo = currentLogInfo;
        this.previousLogInfo = previousLogInfo;
    }

    /**
     * 获得秒数
     *
     * @param currentTime 当前时间
     * @param nextTime    下一个时间
     * @return 秒数
     * @throws ParseException
     */
    public static long timeDifference(String currentTime, String nextTime) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long seconds = (sdf.parse(currentTime).getTime() -
                sdf.parse(nextTime).getTime()) / 1000;
        try {

        } catch (Exception e) {
            logger.info(currentTime + " " + nextTime + "日期转换出错" + e);
        }
        return seconds;
    }

    @Override
    public String toString() {
        String result = "";
        try {
            result = "时间差:" + String.valueOf(timeDifference(previousLogInfo.getDateTime(), currentLogInfo.getDateTime())) + "秒" + " 差值:" + String.valueOf(this.currentLogInfo.getThreadNum() - this.previousLogInfo.getThreadNum()) + " : " +
                    "" + previousLogInfo +
                    ", " + currentLogInfo + "\n";
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

}


/**
 * 日志分析类
 */
public class Analysis {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(Analysis.class);

    /**
     * 读取日志信息
     *
     * @param fileName 文件名
     * @return
     */

    public static List<LogInfo> reaLogs(String fileName) {
        List<LogInfo> logInfos = new ArrayList<>();
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                String[] strings = tempString.split(" - ");
                logInfos.add(new LogInfo(
                        strings[0].split(",")[0],
                        Integer.valueOf(strings[2].split(":")[1].split(",")[0]),
                        tempString
                ));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e1) {
                }
            }
        }
        logger.info(String.valueOf(logInfos.size()));
        return logInfos;
    }


    /**
     * 写入文件信息
     *
     * @param fileName 文件名
     * @param content  内容
     */

    public static void writeLogs(String fileName, String content) {
        try {
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            FileWriter writer = new FileWriter("log\\" + fileName, true);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param list         日志信息列表
     * @param minThreshold 最小阀值
     * @param maxThreshold 最大阀值
     */
    public static void analysisLogs(List<LogInfo> list, int minThreshold, int maxThreshold) throws ParseException {
        if (minThreshold > maxThreshold) {
            logger.info("阀值最小值大于阀值最大值");
            System.exit(0);
        }
        List<LogTime> lowList = new ArrayList<>();
        List<LogTime> middleList = new ArrayList<>();
        List<LogTime> hightList = new ArrayList<>();
        List<OutputLogInfo> outputLogInfos = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            LogInfo currentLogInfo = list.get(i);
            LogInfo nextLogInfo = list.get(i + 1);
            LogTime logTime = new LogTime(currentLogInfo.getDateTime(), nextLogInfo.getDateTime());
            int differenceValue = currentLogInfo.getThreadNum() - nextLogInfo.getThreadNum();
            boolean lowFlag = false, middleFlag = false, hightFLag = false;
            if (differenceValue >= 0) {
                if (differenceValue < minThreshold) {
                    lowFlag = true;
                    lowList.add(logTime);
                } else if (differenceValue <= maxThreshold) {
                    middleFlag = true;
                    middleList.add(logTime);
                } else if (differenceValue > maxThreshold) {
                    hightFLag = true;
                    hightList.add(logTime);
                }
            } else {
                outputLogInfos.add(new OutputLogInfo(currentLogInfo, nextLogInfo));
            }
            //小于阀值
            if (lowList.size() != 0 && (middleFlag || hightFLag)) {
                String currentTime = lowList.get(0).getCurrentTime();
                String nextTime = lowList.get(lowList.size() - 1).getNextTime();
                writeLogs("lowLogs.txt", "时间差:" + OutputLogInfo.timeDifference(nextTime, currentTime) + "秒 " + currentTime + "——" + nextTime + " 小于阀值" + minThreshold + "的数据共" + lowList.size() + " 条\n");
                logger.info("生成低于阀值" + minThreshold + "日志数据" + lowList.size() + "条，已写入到lowLogs.txt");
                lowList.removeAll(lowList);
            }
            //阀值中间
            if (middleList.size() != 0 && (lowFlag || hightFLag)) {
                String currentTime = middleList.get(0).getCurrentTime();
                String nextTime = middleList.get(middleList.size() - 1).getNextTime();
                writeLogs("middleLogs.txt", "时间差:" + OutputLogInfo.timeDifference(nextTime, currentTime) + "秒 " + currentTime + "——" + nextTime + "阀值区间[" + minThreshold + "," + maxThreshold + "]" + "的数据共" + middleList.size() + " 条\n");
                logger.info("生成阀值区间[" + minThreshold + "," + maxThreshold + "]日志数据" + middleList.size() + "条，已写入到middleLogs.txt");
                middleList.removeAll(middleList);
            }
            //大于阀值
            if (hightList.size() != 0 && (lowFlag || middleFlag)) {
                String currentTime = hightList.get(0).getCurrentTime();
                String nextTime = hightList.get(hightList.size() - 1).getNextTime();
                writeLogs("hightLogs.txt", "时间差:" + OutputLogInfo.timeDifference(nextTime, currentTime) + "秒 " + currentTime + "——" + nextTime + " 大于阀值" + maxThreshold + "的数据共" + hightList.size() + " 条\n");
                logger.info("生成高于阀值" + maxThreshold + "日志数据" + hightList.size() + "条，已写入到hightLogs.txt");
                hightList.removeAll(hightList);
            }
        }
        //差值为负数
        if (outputLogInfos.size() != 0) {
            writeLogs("negativeLogs.txt", outputLogInfos.toString().replace(",", "") + "\n");
            logger.info("生产差值为负数的日志数据" + outputLogInfos.size() + "条，已写入到negativeLogs.txt");
        }
    }


    /**
     * 在日志中找到最小的衰减值
     *
     * @param list 日志记录
     */
    public static void findMinValueFromLogs(List<LogInfo> list, int minValueCount) {
        List<MinValueLogInfo> minValueLogInfolist = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            LogInfo currentLogInfo = list.get(i);
            LogInfo nextLogInfo = list.get(i + 1);
            int differenceValue = currentLogInfo.getThreadNum() - nextLogInfo.getThreadNum();
            if (differenceValue >= 0) {
                OutputLogInfo outputLogInfo = new OutputLogInfo(currentLogInfo, nextLogInfo);
                minValueLogInfolist.add(new MinValueLogInfo(differenceValue, outputLogInfo));
            }
            Collections.sort(minValueLogInfolist, (x, y) -> {
                if (x.getMinValue() > y.getMinValue()) {
                    return 1;
                }
                if (x.getMinValue().equals(y.getMinValue())) {
                    return 0;
                }
                return -1;
            });
        }

        List<MinValueLogInfo> writeMinValueLogInfos = new ArrayList<>();
        if (minValueLogInfolist.size() < minValueCount) {
            logger.info("最小值集合的长度小于" + minValueCount);
            System.exit(0);
        }
        for (int i = 0; i < minValueCount; i++) {
            writeMinValueLogInfos.add(minValueLogInfolist.get(i));
        }
        writeLogs("minValueLogInfos.txt", writeMinValueLogInfos.toString());
    }


    public static void sumsOfTimeZones(List<LogInfo> list, long minutes, int minThreshold, int maxThreshold) throws ParseException {
        List<LogInfo> sumOfTimeZonesList = new ArrayList<>();
        long timeDifference = 0;
        long seconds = minutes * 60;
        TimeZones timeZones = new TimeZones();
        for (int i = 0; i < list.size() - 1 && timeDifference <= seconds; i++) {
            sumOfTimeZonesList.add(list.get(i));
            timeDifference = OutputLogInfo.timeDifference(list.get(i + 1).getDateTime(), list.get(0).getDateTime());
        }
        timeZones.setCurrentTime(sumOfTimeZonesList.get(0).getDateTime());
        timeZones.setNextTime(sumOfTimeZonesList.get(sumOfTimeZonesList.size() - 1).getDateTime());
        int lowSum = 0;
        int middleSum = 0;
        int hightSum = 0;
        for (int i = 0; i < sumOfTimeZonesList.size(); i++) {
            LogInfo currentLogInfo = list.get(i);
            LogInfo nextLogInfo = list.get(i + 1);
            LogTime logTime = new LogTime(currentLogInfo.getDateTime(), nextLogInfo.getDateTime());
            int differenceValue = currentLogInfo.getThreadNum() - nextLogInfo.getThreadNum();
            if (differenceValue >= 0) {
                if (differenceValue < minThreshold) {
                    lowSum += differenceValue;
                } else if (differenceValue <= maxThreshold) {
                    middleSum += differenceValue;
                } else if (differenceValue > maxThreshold) {
                    hightSum += differenceValue;
                }
            }
        }
        timeZones.setLowersum(lowSum);
        timeZones.setMiddleSum(middleSum);
        timeZones.setHigthSum(hightSum);
        writeLogs("timeZones.txt", timeZones.toString());
    }

}

class LogTime {
    String currentTime;

    String nextTime;

    public String getCurrentTime() {
        return currentTime;
    }

    public String getNextTime() {
        return nextTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public void setNextTime(String nextTime) {
        this.nextTime = nextTime;
    }

    public LogTime() {
    }

    public LogTime(String currentTime, String nextTime) {
        this.currentTime = currentTime;
        this.nextTime = nextTime;
    }

    @Override
    public String toString() {
        return "LogTime{" +
                "currentTime='" + currentTime + '\'' +
                ", nextTime='" + nextTime + '\'' +
                '}' + "\n";
    }
}

class MinValueLogInfo {
    private Integer minValue;
    private OutputLogInfo logInfo;

    public MinValueLogInfo() {
    }

    public MinValueLogInfo(Integer minValue, OutputLogInfo logInfo) {
        this.minValue = minValue;
        this.logInfo = logInfo;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public void setLogInfo(OutputLogInfo logInfo) {
        this.logInfo = logInfo;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public OutputLogInfo getLogInfo() {
        return logInfo;
    }

    @Override
    public String toString() {
        return logInfo.toString();
    }
}


class TimeZones {
    private String currentTime;

    private String nextTime;

    private Integer lowersum;

    private Integer middleSum;

    private Integer higthSum;


    public TimeZones() {
    }


    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public void setNextTime(String nextTime) {
        this.nextTime = nextTime;
    }

    public void setLowersum(Integer lowersum) {
        this.lowersum = lowersum;
    }

    public void setMiddleSum(Integer middleSum) {
        this.middleSum = middleSum;
    }

    public void setHigthSum(Integer higthSum) {
        this.higthSum = higthSum;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public String getNextTime() {
        return nextTime;
    }

    public Integer getLowersum() {
        return lowersum;
    }

    public Integer getMiddleSum() {
        return middleSum;
    }

    public Integer getHigthSum() {
        return higthSum;
    }

    @Override
    public String
    toString() {
        try {
            return "时间差:" + Long.valueOf(OutputLogInfo.timeDifference(nextTime, currentTime) / 60) + "分钟 {" +
                    " " + currentTime + '\'' +
                    " '" + nextTime + '\'' +
                    ", 低于阀值的总和=" + lowersum +
                    ", 在阀值之间的总和=" + middleSum +
                    ", 大于阀门的总和=" + higthSum +
                    '}';
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
}
