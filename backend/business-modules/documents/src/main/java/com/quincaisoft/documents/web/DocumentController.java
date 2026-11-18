package com.quincaisoft.documents.web;
import com.quincaisoft.documents.api.DocumentService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.http.MediaType;import java.util.*;
@RestController @RequestMapping("/api/v1/documents") public class DocumentController {
 private final DocumentService docs;public DocumentController(DocumentService docs){this.docs=docs;}
 @GetMapping @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')") public List<DocumentService.Snapshot> list(@RequestParam(required=false) UUID sourceId){return docs.list(sourceId);}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')") public DocumentService.Snapshot get(@PathVariable UUID id){return docs.get(id);}
 @GetMapping(value="/{id}/print",produces=MediaType.TEXT_HTML_VALUE) @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')") public String print(@PathVariable UUID id,@RequestParam(defaultValue="true") boolean copy){return docs.render(id,copy);}
 @GetMapping("/settings") @PreAuthorize("hasAnyAuthority('documents:configure','SCOPE_documents:configure')") public DocumentService.Settings settings(){return docs.settings();}
 @PutMapping("/settings") @PreAuthorize("hasAnyAuthority('documents:configure','SCOPE_documents:configure')") public DocumentService.Settings configure(@RequestHeader("Idempotency-Key") UUID operationId,@RequestBody DocumentService.Settings settings){return docs.configure(operationId,settings);}
}
