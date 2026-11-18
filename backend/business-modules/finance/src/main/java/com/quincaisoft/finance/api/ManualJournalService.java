package com.quincaisoft.finance.api;import java.util.*;import java.time.*;
public interface ManualJournalService{
 record Line(String account,String debitXaf,String creditXaf){}
 record Draft(UUID operationId,LocalDate businessDate,String reason,List<Line> lines){}
 record Command(UUID operationId,long version,String reason){}
 record Reversal(UUID operationId,long version,LocalDate businessDate,String reason){}
 record Journal(UUID id,LocalDate businessDate,String status,String reason,UUID createdBy,UUID approvedBy,UUID reversalOf,UUID journalId,long version,List<Line> lines){}
 Journal get(UUID id);List<Journal> list();Journal draft(Draft p);Journal command(UUID id,String action,Command p);Journal reversal(UUID id,Reversal p);
}