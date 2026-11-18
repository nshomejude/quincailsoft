package com.quincaisoft.reporting.domain;
import java.time.*;import java.time.temporal.ChronoUnit;
public final class ReportRules {
 private ReportRules(){}
 public static void range(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||ChronoUnit.DAYS.between(from,to)>366)throw new IllegalArgumentException("Choose an inclusive report range of at most 367 dates");}
 public static void page(int offset,int limit){if(offset<0||offset>100000||limit<1||limit>1000)throw new IllegalArgumentException("Report offset 0–100000 and limit 1–1000 required");}
 public static String cell(String value){String v=value==null?"":value;String trim=v.stripLeading();if(!trim.isEmpty()&&"=+-@".indexOf(trim.charAt(0))>=0)v="'"+v;return "\""+v.replace("\"","\"\"")+"\"";}
}
