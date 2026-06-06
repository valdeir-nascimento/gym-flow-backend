package br.com.gym.flow.users.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.usecase.GetLatestAnamnesisQuery;
import br.com.gym.flow.users.application.usecase.GetLatestAnamnesisUseCase;
import br.com.gym.flow.users.application.usecase.GrantHealthConsentCommand;
import br.com.gym.flow.users.application.usecase.GrantHealthConsentUseCase;
import br.com.gym.flow.users.application.usecase.ListAnamnesisHistoryQuery;
import br.com.gym.flow.users.application.usecase.ListAnamnesisHistoryUseCase;
import br.com.gym.flow.users.application.usecase.RegisterAnamnesisCommand;
import br.com.gym.flow.users.application.usecase.RegisterAnamnesisUseCase;
import br.com.gym.flow.users.domain.spi.AnamnesisView;
import br.com.gym.flow.users.domain.spi.HealthConsentView;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Anamnesis / initial assessment of a student (RF-017). Registration is a
 * professor action gated by the student↔instructor bond and a prior health-data
 * consent (RNF-008). Reads follow the ownership policy (student/bonded
 * instructor/administrator). The acting user comes from the {@code X-User-Id} header.
 */
@RestController
@RequestMapping("/users/{studentId}")
@RequiredArgsConstructor
class AnamnesisController implements AnamnesisApi {

    private final RegisterAnamnesisUseCase registerAnamnesis;
    private final GetLatestAnamnesisUseCase getLatestAnamnesis;
    private final ListAnamnesisHistoryUseCase listAnamnesisHistory;
    private final GrantHealthConsentUseCase grantHealthConsent;

    @PostMapping("/health-consent")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public Result<HealthConsentView> grantConsent(@PathVariable UUID studentId,
                                           @RequestHeader("X-User-Id") UUID actorId,
                                           @RequestHeader("X-User-Role") String actorRole) {
        return grantHealthConsent.execute(new GrantHealthConsentCommand(studentId, actorId, actorRole));
    }

    @PostMapping("/anamnesis")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public Result<AnamnesisView> register(@PathVariable UUID studentId,
                                   @Valid @RequestBody RegisterAnamnesisRequest req,
                                   @RequestHeader("X-User-Id") UUID actorId,
                                   @RequestHeader("X-User-Role") String actorRole,
                                   HttpServletResponse response) {
        Result<AnamnesisView> result = registerAnamnesis.execute(new RegisterAnamnesisCommand(
            studentId, req.weightKg(), req.heightCm(), req.objectives(), req.conditioningHistory(),
            req.injuries(), req.medicalRestrictions(), req.contraindications(), req.observations(),
            actorId, actorRole));
        if (result.isSuccess()) {
            response.setHeader(HttpHeaders.LOCATION,
                "/users/" + studentId + "/anamnesis/" + result.getOrThrow().id());
        }
        return result;
    }

    @GetMapping("/anamnesis")
    @Override
    public Result<AnamnesisView> latest(@PathVariable UUID studentId,
                                 @RequestHeader("X-User-Id") UUID actorId,
                                 @RequestHeader("X-User-Role") String actorRole) {
        return getLatestAnamnesis.execute(new GetLatestAnamnesisQuery(studentId, actorId, actorRole));
    }

    @GetMapping("/anamnesis/history")
    @Override
    public Result<List<AnamnesisView>> history(@PathVariable UUID studentId,
                                        @RequestHeader("X-User-Id") UUID actorId,
                                        @RequestHeader("X-User-Role") String actorRole) {
        return listAnamnesisHistory.execute(new ListAnamnesisHistoryQuery(studentId, actorId, actorRole));
    }
}
