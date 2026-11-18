package com.quincaisoft.branch.api;
import com.quincaisoft.shared.BootstrapScope;import java.util.*;import java.time.*;
public interface BranchBootstrapService { void initialize(BootstrapScope s,String name,String timezone); }
