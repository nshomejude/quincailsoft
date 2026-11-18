package com.quincaisoft.audit.api;
import java.util.*;import java.time.Instant;
public interface AuditService {record Event(UUID id,UUID actorId,UUID operationId,String action,String sourceType,UUID sourceId,String reason,Instant recordedAt){} void append(UUID operation,String action,String sourceType,UUID source,String reason);List<Event> recent();}