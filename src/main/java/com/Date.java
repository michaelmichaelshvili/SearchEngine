package com;

import java.util.HashMap;
import java.util.Map;

/**
 * This class hekp to the parser. we save in this class to maps.
 * DateToDateNum - map from month name and is short write to the number of the month
 * MonthToNumberOfDays - map from month to the numbers if days he have
 */
public class Date {
    public static Map<String,String> DateToDateNum =new HashMap<String,String>();
    static {
        DateToDateNum.put("JAN","01");
        DateToDateNum.put("JANUARY","01");
        DateToDateNum.put("FEB","02");
        DateToDateNum.put("FEBRUARY","02");
        DateToDateNum.put("MAR","03");
        DateToDateNum.put("MARCH","03");
        DateToDateNum.put("APR","04");
        DateToDateNum.put("APRIL","04");
        DateToDateNum.put("MAY","05");
        DateToDateNum.put("JUN","06");
        DateToDateNum.put("JUNE","06");
        DateToDateNum.put("JUL","07");
        DateToDateNum.put("JULY","07");
        DateToDateNum.put("AUG","08");
        DateToDateNum.put("AUGUST","08");
        DateToDateNum.put("SEP","09");
        DateToDateNum.put("SEPTEMBER","09");
        DateToDateNum.put("OCT","10");
        DateToDateNum.put("OCTOBER","10");
        DateToDateNum.put("NOV","11");
        DateToDateNum.put("NOVEMBER","11");
        DateToDateNum.put("DEC","12");
        DateToDateNum.put("DECEMBER","12");
    }
    public static Map<String,Integer> MonthToNumberOfDays =new HashMap<String,Integer>();
    static {
        MonthToNumberOfDays.put("JAN",31);
        MonthToNumberOfDays.put("JANUARY",31);
        MonthToNumberOfDays.put("FEB",28);
        MonthToNumberOfDays.put("FEBRUARY",28);
        MonthToNumberOfDays.put("MAR",31);
        MonthToNumberOfDays.put("MARCH",31);
        MonthToNumberOfDays.put("APR",30);
        MonthToNumberOfDays.put("APRIL",30);
        MonthToNumberOfDays.put("MAY",31);
        MonthToNumberOfDays.put("JUN",30);
        MonthToNumberOfDays.put("JUNE",30);
        MonthToNumberOfDays.put("JUL",31);
        MonthToNumberOfDays.put("JULY",31);
        MonthToNumberOfDays.put("AUG",31);
        MonthToNumberOfDays.put("AUGUST",31);
        MonthToNumberOfDays.put("SEP",30);
        MonthToNumberOfDays.put("SEPTEMBER",30);
        MonthToNumberOfDays.put("OCT",31);
        MonthToNumberOfDays.put("OCTOBER",31);
        MonthToNumberOfDays.put("NOV",30);
        MonthToNumberOfDays.put("NOVEMBER",30);
        MonthToNumberOfDays.put("DEC",31);
        MonthToNumberOfDays.put("DECEMBER",31);
    }

}
