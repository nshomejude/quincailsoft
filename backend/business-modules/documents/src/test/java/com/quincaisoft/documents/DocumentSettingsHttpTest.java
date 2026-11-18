package com.quincaisoft.documents;

import com.quincaisoft.documents.api.DocumentService;
import com.quincaisoft.documents.web.DocumentController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentSettingsHttpTest {
 private static final String BODY="""
  {"issuerName":"Synthetic","issuerAddress":"Address","registrationNumber":"REG","fiscalSeries":"INV","locale":"en","version":0}
  """;
 private static final class RecordingDocuments implements DocumentService {
  int calls;UUID operation;Settings input;
  public Settings configure(UUID id,Settings settings){calls++;operation=id;input=settings;return new Settings(settings.issuerName(),settings.issuerAddress(),settings.registrationNumber(),settings.fiscalSeries(),settings.locale(),1);}
  public Settings configure(Settings settings){throw new AssertionError("HTTP must use durable command overload");}
  public Settings settings(){throw new AssertionError("Unexpected settings read");}
  public Snapshot issue(Issue request){throw new AssertionError("Unexpected issue");}
  public Snapshot get(UUID id){throw new AssertionError("Unexpected document read");}
  public List<Snapshot> list(UUID source){throw new AssertionError("Unexpected document list");}
  public String render(UUID id,boolean copy){throw new AssertionError("Unexpected document rendering");}
 }
 @Test void settingsRequiresUuidHeaderAndForwardsItToDurableCommand()throws Exception {
  var service=new RecordingDocuments();var mvc=MockMvcBuilders.standaloneSetup(new DocumentController(service)).build();
  mvc.perform(put("/api/v1/documents/settings").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isBadRequest());
  mvc.perform(put("/api/v1/documents/settings").header("Idempotency-Key","not-a-uuid").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isBadRequest());
  assertEquals(0,service.calls);
  UUID operation=UUID.randomUUID();var settings=new DocumentService.Settings("Synthetic","Address","REG","INV","en",0);
  mvc.perform(put("/api/v1/documents/settings").header("Idempotency-Key",operation).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
  assertEquals(1,service.calls);assertEquals(operation,service.operation);assertEquals(settings,service.input);
 }
}
