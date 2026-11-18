package com.quincaisoft.documents.web;
import com.quincaisoft.documents.api.DocumentWorkspace;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.http.MediaType;import java.util.*;
@RestController @RequestMapping("/api/v1/documents") @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')")
public class DocumentWorkspaceController {
 private final DocumentWorkspace service;public DocumentWorkspaceController(DocumentWorkspace service){this.service=service;}
 @GetMapping("/templates")public List<DocumentWorkspace.Template> templates(){return service.templates();}
 @GetMapping("/templates/{templateId}/sources")public List<DocumentWorkspace.SourceCandidate> sources(@PathVariable String templateId){return service.sources(templateId);}
 @GetMapping("/captures")public List<DocumentWorkspace.Capture> list(){return service.list();}
 @GetMapping("/captures/{id}")public DocumentWorkspace.Capture get(@PathVariable UUID id){return service.get(id);}
 @PostMapping("/captures")@PreAuthorize("hasAnyAuthority('documents:write','SCOPE_documents:write')")public DocumentWorkspace.Capture capture(@RequestHeader("Idempotency-Key")UUID key,@RequestBody DocumentWorkspace.CaptureRequest r){same(key,r.operationId());return service.capture(r);}
 @GetMapping(value="/captures/{id}/preview",produces=MediaType.TEXT_HTML_VALUE)public String preview(@PathVariable UUID id,@RequestParam(defaultValue="A4")String paper){return service.preview(id,paper);}
 @PostMapping("/captures/{id}/print-attempts")@PreAuthorize("hasAnyAuthority('documents:print','SCOPE_documents:print')")public DocumentWorkspace.PrintResult printCapture(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DocumentWorkspace.PrintRequest r){same(key,r.operationId());return service.print(id,true,r);}
 @PostMapping("/{id}/print-attempts")@PreAuthorize("hasAnyAuthority('documents:print','SCOPE_documents:print')")public DocumentWorkspace.PrintResult printOriginal(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DocumentWorkspace.PrintRequest r){same(key,r.operationId());return service.print(id,false,r);}
 @GetMapping("/print-attempts")public List<DocumentWorkspace.PrintResult> attempts(){return service.attempts();}
 @PostMapping("/print-attempts/{id}/outcomes")@PreAuthorize("hasAnyAuthority('documents:print','SCOPE_documents:print')")public DocumentWorkspace.Outcome outcome(@PathVariable UUID id,@RequestHeader("Idempotency-Key")UUID key,@RequestBody DocumentWorkspace.OutcomeRequest r){same(key,r.operationId());return service.outcome(id,r);}
 private void same(UUID header,UUID operation){if(!header.equals(operation))throw new IllegalArgumentException("Idempotency-Key must equal operationId");}
}
