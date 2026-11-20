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
    void matchingOperationDelegatesToTheScopedRevocationService() {
        IdentityService service = mock(IdentityService.class);
        var controller = new SessionRevocationController(service);
        UUID operation = UUID.randomUUID(), requestId = UUID.randomUUID(), target = UUID.randomUUID();
        var request = new IdentityService.Revoke(operation, target, 4, "Sign out current sessions");
        var result = new IdentityService.Revocation(operation, target, 5, true,
                "CURRENT_COMPANY_BRANCH_AUTHORIZATION_EPOCH");
        when(service.revoke(request)).thenReturn(result);

        assertEquals(result, controller.revoke(operation, requestId, request));
        verify(service).revoke(request);
        verifyNoMoreInteractions(service);
    }

    @Test
    void malformedOperationAndMissingRequestIdCannotReachRevocationService() {
        IdentityService service = mock(IdentityService.class);
        var controller = new SessionRevocationController(service);
        UUID operation = UUID.randomUUID(), requestId = UUID.randomUUID();
        var valid = new IdentityService.Revoke(operation, UUID.randomUUID(), 4, "Revoked");
        var noOperation = new IdentityService.Revoke(null, UUID.randomUUID(), 4, "Revoked");

        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(UUID.randomUUID(), requestId, valid));
        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(null, requestId, valid));
        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(operation, null, valid));
        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(operation, requestId, noOperation));
        assertThrows(IllegalArgumentException.class,
                () -> controller.revoke(operation, requestId, null));
        verifyNoInteractions(service);
    }

    @Test
    void originalHttpRouteAndAuthenticationPolicyRemainStable() {
        var mapping = SessionRevocationController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/v1/identity/sessions"}, mapping.value());
        var policy = SessionRevocationController.class.getAnnotation(PreAuthorize.class);
        assertEquals("isAuthenticated()", policy.value());
    }
}
