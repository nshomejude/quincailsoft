package com.quincaisoft.documents.web;
import com.quincaisoft.documents.api.DocumentService;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.http.MediaType;import java.util.*;
@RestController @RequestMapping("/api/v1/documents") public class DocumentController {
 private final DocumentService docs;public DocumentController(DocumentService docs){this.docs=docs;}
 @GetMapping @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')") public List<DocumentService.Snapshot> list(@RequestParam(required=false) UUID sourceId){return docs.list(sourceId).stream().filter(this::allowed).toList();}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')") public DocumentService.Snapshot get(@PathVariable UUID id){var d=docs.get(id);if(!allowed(d))throw new org.springframework.security.access.AccessDeniedException("Source document permission required");return d;}
 @GetMapping(value="/{id}/print",produces=MediaType.TEXT_HTML_VALUE) @PreAuthorize("hasAnyAuthority('documents:read','SCOPE_documents:read')") public String print(@PathVariable UUID id,@RequestParam(defaultValue="true") boolean copy){get(id);return docs.render(id,copy);}
 @GetMapping("/settings") @PreAuthorize("hasAnyAuthority('documents:configure','SCOPE_documents:configure')") public DocumentService.Settings settings(){return docs.settings();}
 @PutMapping("/settings") @PreAuthorize("hasAnyAuthority('documents:configure','SCOPE_documents:configure')") public DocumentService.Settings configure(@RequestHeader("Idempotency-Key") UUID operationId,@RequestBody DocumentService.Settings settings){return docs.configure(operationId,settings);}
 private boolean allowed(DocumentService.Snapshot d){String p=switch(d.kind()){case "INVOICE"->"sales:read";case "QUOTATION"->"quotations:read";case "CREDIT_NOTE"->"returns:read";default->"documents:restricted";};var a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals(p)||x.getAuthority().equals("SCOPE_"+p));}
}
