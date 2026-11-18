package com.quincaisoft.sync.api;
import java.util.*;import java.time.*;
public interface SyncService {
 record Challenge(UUID operationId,String name,String publicKeyPem){}
 record Proof(UUID operationId,long version,String signatureBase64){}
 record Approval(UUID operationId,long version,String reason){}
 record Device(UUID id,UUID epoch,UUID actorId,String name,String status,String challenge,Instant expiresAt,String fingerprint,long version,boolean offlineSelling){}
 record Recovery(UUID operationId,UUID sourceOperationId,UUID deviceId,UUID deviceEpoch,long sequence,String classification,String payloadJson,String signatureBase64,String reason){}
 record Receipt(UUID evidenceId,UUID sourceOperationId,String status,String payloadHash,Instant receivedAt,boolean businessPosted){}
 record Operation(UUID operationId,String status,String sourceType,UUID sourceId,Instant acceptedAt){}
 List<Device> devices();@com.quincaisoft.shared.TenantMutation Device challenge(Challenge input);@com.quincaisoft.shared.TenantMutation Device prove(UUID id,Proof input);@com.quincaisoft.shared.TenantMutation Device approve(UUID id,Approval input);Receipt recover(Recovery input);List<Receipt> recovery();Operation status(UUID operation);
}
