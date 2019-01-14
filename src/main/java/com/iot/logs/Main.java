package com.iot.logs;

import com.iot.logs.utils.Analysis;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author LHT
 */
public class Main {
    public static void main(String[] args) throws ParseException {
      //  Analysis.analysisLogs(Analysis.reaLogs("C:\\igpUip.log"), 80, 140);
       // Analysis.findMinValueFromLogs(Analysis.reaLogs("C:\\igpUip.log"),20);
       Analysis.sumsOfTimeZones(Analysis.reaLogs("C:\\igpUip.log"),5,2,3);
    }
}
