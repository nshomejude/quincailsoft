package com.quincaisoft.identity;

import com.quincaisoft.identity.api.IdentityService;
import com.quincaisoft.identity.web.SessionRevocationController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionRevocationControllerTest {
    @Test
    void matchedOperationDelegatesToTheExistingScopedRevocationService() {
        IdentityService service = mock(IdentityService.class);
        var controller = new SessionRevocationController(service);
        UUID operation = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        var request = new IdentityService.Revoke(operation, target, 4, "Sign out all current sessions");
        var result = new IdentityService.Revocation(operation, target, 5, true,
                "CURRENT_COMPANY_BRANCH_AUTHORIZATION_EPOCH");
        when(service.revoke(request)).thenReturn(result);

        assertEquals(result, controller.revoke(operation, request));
        verify(service, times(1)).revoke(request);
        verifyNoMoreInteractions(service);
    }

    @Test
    void mismatchedAndMissingOperationIdentitiesCannotReachTheService() {
        IdentityService service = mock(IdentityService.class);
        var controller = new SessionRevocationController(service);
        UUID operation = UUID.randomUUID();
        var request = new IdentityService.Revoke(operation, UUID.randomUUID(), 4, "Revoked");

        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(UUID.randomUUID(), request));
        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(null, request));
        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(operation, null));
        verifyNoInteractions(service);
    }

    @Test
    void routeRequiresAnAuthenticatedCallerButNotTheIdentityManagePermission() throws Exception {
        var route = SessionRevocationController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/v1/identity/revocations"}, route.value());
        var action = SessionRevocationController.class.getMethod(
                "revoke", UUID.class, IdentityService.Revoke.class);
        assertEquals("isAuthenticated()", action.getAnnotation(PreAuthorize.class).value());
    }
}
