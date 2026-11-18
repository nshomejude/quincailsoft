package com.quincaisoft.identity.api;
import java.util.*;import java.time.*;
public interface IdentityService {
record Member(UUID actorId,String role,boolean active,long authorizationEpoch){}
record Change(UUID id,UUID operationId,UUID targetActor,UUID requestedBy,UUID approvedBy,String desiredRole,boolean desiredActive,long expectedEpoch,String reason,String status,long version,Instant createdAt){}
record Request(UUID operationId,UUID targetActor,String desiredRole,boolean desiredActive,long expectedEpoch,String reason){}
List<Member> members();List<Change> changes();Change request(Request input);Change approve(UUID id,UUID operation,long version);
}