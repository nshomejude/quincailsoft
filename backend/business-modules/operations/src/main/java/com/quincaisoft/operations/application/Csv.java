package com.quincaisoft.operations.application;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded RFC 4180-style CSV reader. No evaluation of cell contents. */
public final class Csv {
 private Csv() {}
 public static List<List<String>> parse(String source, String delimiter) {
  if(source==null||source.isBlank()||source.getBytes(StandardCharsets.UTF_8).length>512_000)throw new IllegalArgumentException("Nonempty UTF-8 CSV, maximum 512000 bytes");
  if(!Set.of(",",";").contains(delimiter))throw new IllegalArgumentException("Delimiter must be comma or semicolon");
  if(source.startsWith("\ufeff"))source=source.substring(1);
  char sep=delimiter.charAt(0);List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false,closed=false;
  for(int i=0;i<source.length();i++) {
   char ch=source.charAt(i);
   if(quoted){if(ch=='"'){if(i+1<source.length()&&source.charAt(i+1)=='"'){cell.append('"');i++;}else{quoted=false;closed=true;}}else cell.append(ch);}
   else if(ch==sep){row.add(cell.toString());cell.setLength(0);closed=false;}
   else if(ch=='\r'||ch=='\n'){if(ch=='\r'&&i+1<source.length()&&source.charAt(i+1)=='\n')i++;if(!row.isEmpty()||cell.length()>0||closed){row.add(cell.toString());rows.add(List.copyOf(row));}row.clear();cell.setLength(0);closed=false;}
   else if(ch=='"'){if(cell.length()!=0||closed)throw new IllegalArgumentException("Unexpected quote in CSV");quoted=true;}
   else {if(closed)throw new IllegalArgumentException("Characters after closing CSV quote");if(ch=='\0')throw new IllegalArgumentException("NUL is not a valid CSV character");cell.append(ch);}
   if(cell.length()>10_000||row.size()>40||rows.size()>501)throw new IllegalArgumentException("CSV row, column or field limit exceeded");
  }
  if(quoted)throw new IllegalArgumentException("Unterminated CSV quote");
  if(!row.isEmpty()||cell.length()>0||closed){row.add(cell.toString());rows.add(List.copyOf(row));}
  if(rows.size()<2||rows.size()>501)throw new IllegalArgumentException("CSV requires 1–500 data rows");
  return List.copyOf(rows);
 }
 public static String row(List<?> values){return values.stream().map(Csv::cell).collect(java.util.stream.Collectors.joining(","))+"\r\n";}
 public static String cell(Object value){String s=value==null?"":value.toString();String stripped=s.stripLeading();if(s.startsWith("\t")||s.startsWith("\r")||!stripped.isEmpty()&&"=+-@".indexOf(stripped.charAt(0))>=0)s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
