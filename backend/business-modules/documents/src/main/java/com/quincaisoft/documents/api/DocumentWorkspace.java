package com.quincaisoft.documents.api;
import java.time.*;import java.util.*;
/** Fixed, trusted source captures and print evidence; never accepts arbitrary source JSON. */
public interface DocumentWorkspace {
 record Template(String id,String title,String titleFr,String paper,String permission,String basis){}
 record SourceCandidate(UUID id,String label,String status,LocalDate businessDate){}
 record CaptureRequest(UUID operationId,String templateId,UUID sourceId){}
 record Body(String sourceReference,String sourceStatus,String sourceVersion,LocalDate businessDate,String party,List<String> columns,List<List<String>> rows,String totalXaf,String basis){}
 record Content(String templateVersion,Template template,DocumentService.Settings issuer,Body source,UUID actorId,Instant capturedAt){}
 record Capture(UUID id,String templateId,UUID sourceId,UUID actorId,Instant capturedAt,String sha256,Content content){}
 record PrintRequest(UUID operationId,String reason,String paper){}
 record PrintResult(UUID id,UUID documentId,boolean capture,UUID actorId,Instant requestedAt,String paper,boolean duplicate,String reason,String documentHash,String status,String html){}
 record OutcomeRequest(UUID operationId,String status,String reason){}
 record Outcome(UUID id,UUID attemptId,UUID actorId,Instant recordedAt,String status,String reason){}
 List<Template> templates();List<SourceCandidate> sources(String templateId);
 @com.quincaisoft.shared.TenantMutation Capture capture(CaptureRequest request);Capture get(UUID id);List<Capture> list();
 String preview(UUID id,String paper);@com.quincaisoft.shared.TenantMutation PrintResult print(UUID id,boolean capture,PrintRequest request);
 List<PrintResult> attempts();@com.quincaisoft.shared.TenantMutation Outcome outcome(UUID id,OutcomeRequest request);
}
