package com.quincaisoft.operations.application;

import com.quincaisoft.operations.api.OperationsService.*;
import java.math.*;
import java.util.*;

/** Closed, typed allowlists: authority, stock, prices in fractional XAF and posting fields cannot be smuggled in. */
public final class ImportValidation {
 private ImportValidation(){}
 private static final Map<Entity,Set<String>> FIELDS=Map.of(
  Entity.PRODUCT,Set.of("sku","name","category","brand","barcode","base_unit","pack_size","selling_price","reorder_level","tax_rate","track_stock","track_serial","track_expiry","description","status"),
  Entity.CUSTOMER,Set.of("name","type","phone","email","address","tax_reference","credit_limit","payment_terms_days","consent","status"),
  Entity.SUPPLIER,Set.of("code","name","contact_person","phone","email","address","tax_reference","payment_terms_days","bank_reference","status"),
  Entity.CATEGORY,Set.of("code","name","parent_category","brand","description","status"),
  Entity.UNIT,Set.of("name","symbol","base_unit","conversion_factor","rounding_precision","status"));
 public static Row row(Entity entity,int number,Map<String,String> data){
  if(entity==null)throw new IllegalArgumentException("Master entity required");var unknown=new TreeSet<>(data.keySet());unknown.removeAll(FIELDS.get(entity));if(!unknown.isEmpty())throw new IllegalArgumentException("Unsupported input fields: "+String.join(", ",unknown));
  String status=value(data,"status","");if(!Set.of("","Active","Inactive").contains(status))throw new IllegalArgumentException("Only master Active/Inactive status is valid; imported records remain inactive");
  String name=required(data,"name",200),code=value(data,"sku",value(data,"code",""));
  if(entity==Entity.PRODUCT&&!code.matches("[A-Za-z0-9._/-]{1,80}"))throw new IllegalArgumentException("Valid SKU required");
  if(entity!=Entity.PRODUCT&&!code.isEmpty()&&!code.matches("[A-Za-z0-9_-]{1,40}"))throw new IllegalArgumentException("Invalid master code");
  String base=value(data,"base_unit","");if(entity==Entity.PRODUCT||entity==Entity.UNIT){base=required(data,"base_unit",32);if(!base.matches("[a-z][a-z0-9_-]*"))throw new IllegalArgumentException("Invalid base unit");}
  String price=entity==Entity.PRODUCT?whole(required(data,"selling_price",30)):"0";
  String tax=scaled(value(data,"tax_rate","0"),100);if(Long.parseLong(tax)>10000)throw new IllegalArgumentException("Tax rate exceeds 100 percent");
  String pack=scaled(value(data,"pack_size","1"),1000),reorder=scaled(value(data,"reorder_level","0"),1000);if(Long.parseLong(pack)==0)throw new IllegalArgumentException("Pack size must be positive");
  UUID relation=null;String reference=value(data,entity==Entity.CATEGORY?"parent_category":"category","");if(!reference.isEmpty())relation=UUID.fromString(reference);
  String type=value(data,"type","Individual");if(!Set.of("Individual","Contractor","Company").contains(type))throw new IllegalArgumentException("Invalid customer type");
  String email=text(data,"email",254);if(!email.isEmpty()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Invalid email");
  int terms=integer(value(data,"payment_terms_days","0"),3650),precision=integer(value(data,"rounding_precision","3"),3);
  String symbol=text(data,"symbol",32),factor=value(data,"conversion_factor","1");if(entity==Entity.UNIT){if(!symbol.matches("[a-z][a-z0-9_-]{0,31}"))throw new IllegalArgumentException("Valid lowercase unit symbol required");rational(factor);}
  return new Row(number,name,code,base,price,tax,relation,text(data,"brand",200),text(data,"barcode",80),text(data,"description",10000),pack,reorder,bool(data,"track_stock",true),bool(data,"track_serial",false),bool(data,"track_expiry",false),type,text(data,"contact_person",200),text(data,"phone",80),email,text(data,"address",2000),text(data,"tax_reference",100),whole(value(data,"credit_limit","0")),terms,bool(data,"consent",false),text(data,"bank_reference",2000),symbol,factor,precision);
 }
 public static String[] rational(String value){if(value==null||!value.matches("[0-9]+(?:\\.[0-9]{1,9})?")||value.length()>30)throw new IllegalArgumentException("Conversion must be a positive exact decimal");BigDecimal decimal=new BigDecimal(value);if(decimal.signum()<=0)throw new IllegalArgumentException("Conversion must be positive");BigInteger numerator=decimal.unscaledValue(),denominator=BigInteger.TEN.pow(decimal.scale()),gcd=numerator.gcd(denominator);return new String[]{Long.toString(numerator.divide(gcd).longValueExact()),Long.toString(denominator.divide(gcd).longValueExact())};}
 private static String required(Map<String,String>d,String key,int limit){String s=text(d,key,limit);if(s.isBlank())throw new IllegalArgumentException(key+" is required");return s;}
 private static String text(Map<String,String>d,String key,int limit){String s=value(d,key,"");if(s.length()>limit)throw new IllegalArgumentException(key+" exceeds maximum length");return s;}
 private static String value(Map<String,String>d,String key,String fallback){String s=d.get(key);return s==null||s.isBlank()?fallback:s.strip();}
 private static int integer(String s,int max){long n=Long.parseLong(whole(s));if(n>max)throw new IllegalArgumentException("Integer exceeds permitted range");return (int)n;}
 private static String whole(String s){if(!s.matches("[0-9]+(?:\\.0+)?")||s.length()>30)throw new IllegalArgumentException("XAF and whole-number fields require exact nonnegative integers");return Long.toString(new BigDecimal(s).longValueExact());}
 private static String scaled(String s,int scale){if(s==null||!s.matches("[0-9]+(?:\\.[0-9]+)?")||s.length()>30)throw new IllegalArgumentException("Exact nonnegative decimal required");return Long.toString(new BigDecimal(s).multiply(BigDecimal.valueOf(scale)).longValueExact());}
 private static boolean bool(Map<String,String>d,String key,boolean fallback){String s=d.get(key);if(s==null)return fallback;return switch(s.strip().toLowerCase(Locale.ROOT)){case "true","1","yes"->true;case "","false","0","no"->false;default->throw new IllegalArgumentException(key+" must be true or false");};}
}
