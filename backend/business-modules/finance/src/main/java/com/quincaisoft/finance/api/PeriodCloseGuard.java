package com.quincaisoft.finance.api;import java.time.LocalDate;public interface PeriodCloseGuard {default String domain(){return "unknown";}void verifyCanClose(LocalDate start,LocalDate end);}
