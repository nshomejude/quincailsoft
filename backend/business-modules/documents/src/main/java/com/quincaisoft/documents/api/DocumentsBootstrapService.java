package com.quincaisoft.documents.api;
import com.quincaisoft.shared.BootstrapScope;import java.util.*;import java.time.*;
public interface DocumentsBootstrapService { void initialize(BootstrapScope s,String issuer,String address,String registration,String prefix,String locale,int year); }
