package io.fundrequest.platform.admin.claim;

import io.fundrequest.core.request.RequestService;
import io.fundrequest.core.request.claim.domain.TrustedRepo;
import io.fundrequest.core.request.claim.dto.RequestClaimDto;
import io.fundrequest.core.request.claim.infrastructure.TrustedRepoRepository;
import io.fundrequest.core.request.view.RequestDto;
import io.fundrequest.core.request.view.RequestDtoMother;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AutoClaimServiceImplTest {

    private AutoClaimServiceImpl autoClaimService;
    private TrustedRepoRepository trustedRepoRepository;
    private ClaimModerationService claimModerationService;
    private RequestService requestService;

    @Before
    public void setUp() {
        trustedRepoRepository = mock(TrustedRepoRepository.class);
        claimModerationService = mock(ClaimModerationService.class);
        requestService = mock(RequestService.class);
        autoClaimService = new AutoClaimServiceImpl(claimModerationService, trustedRepoRepository, requestService);
    }

    @Test
    public void autoClaim() {
        RequestDto request = RequestDtoMother.freeCodeCampNoUserStories();
        when(trustedRepoRepository.findAll()).thenReturn(Collections.singletonList(TrustedRepo.builder().owner(request.getIssueInformation().getOwner()).build()));
        when(claimModerationService.listPendingRequestClaims()).thenReturn(Collections.singletonList(RequestClaimDto.builder().id(1L).build()));
        when(requestService.findRequest(1L)).thenReturn(request);

        autoClaimService.autoApproveTrustedRepos();

        verify(claimModerationService).approveClaim(1L);
    }
}