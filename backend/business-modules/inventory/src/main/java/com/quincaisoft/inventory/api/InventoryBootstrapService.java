package com.quincaisoft.inventory.api;
import com.quincaisoft.shared.BootstrapScope;import java.util.*;import java.time.*;
public interface InventoryBootstrapService { void initialize(BootstrapScope s,UUID warehouseId,String name); }
