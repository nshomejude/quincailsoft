package com.quincaisoft.operations.api;

import com.quincaisoft.operations.api.OperationsService.*;
import com.quincaisoft.shared.TenantMutation;
import java.util.List;
import java.util.UUID;

/** Actor-owned bounded exports. Export creation does not grant later download access. */
public interface ExportService {
 List<Export> list();
 Export get(UUID id);
 @TenantMutation Export request(ExportRequest request);
 @TenantMutation Export run(UUID id,Command request);
 @TenantMutation Export cancel(UUID id,Command request);
 Content download(UUID id);
}
