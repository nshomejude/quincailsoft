package com.quincaisoft.documents;

import com.quincaisoft.documents.api.DocumentService;
import com.quincaisoft.documents.application.DocumentSourceReader;
import com.quincaisoft.documents.web.DocumentController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentCopyPreviewTest {
 @Test void callerCannotSuppressCopyMarkerThroughLegacyReadOnlyPrintRoute()throws Exception {
  var docs=mock(DocumentService.class);UUID id=UUID.randomUUID();
  when(docs.get(id)).thenReturn(new DocumentService.Snapshot(id,"INV-001","INVOICE",UUID.randomUUID(),LocalDate.now(),"1000","digest","{}",Instant.now()));
  when(docs.render(id,true)).thenReturn("<html>COPIE</html>");
  SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("cashier",null,List.of(new SimpleGrantedAuthority("sales:read"),new SimpleGrantedAuthority("documents:read"))));
  try {
   var mvc=MockMvcBuilders.standaloneSetup(new DocumentController(docs)).build();
   mvc.perform(get("/api/v1/documents/"+id+"/print").param("copy","false")).andExpect(status().isOk()).andExpect(content().string("<html>COPIE</html>"));
   verify(docs).render(id,true);verify(docs,never()).render(id,false);
  }finally{SecurityContextHolder.clearContext();}
 }
 @Test void unauthenticatedIdentityCannotUseAuthorityStringsToReadDocumentSources(){
  var authentication=new UsernamePasswordAuthenticationToken("not-authenticated",null,List.of(new SimpleGrantedAuthority("sales:read")));
  authentication.setAuthenticated(false);SecurityContextHolder.getContext().setAuthentication(authentication);
  try {assertThrows(AccessDeniedException.class,()->new DocumentSourceReader(null,null,null).authorize("DOC01"));}
  finally{SecurityContextHolder.clearContext();}
 }
}
