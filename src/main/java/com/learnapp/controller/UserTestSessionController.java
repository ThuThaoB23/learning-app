package com.learnapp.controller;

import com.learnapp.dto.SubmitTestItemAnswerRequest;
import com.learnapp.dto.SubmitTestItemAnswerResponse;
import com.learnapp.dto.SubmitTestSessionAnswersRequest;
import com.learnapp.dto.SubmitTestSessionAnswersResponse;
import com.learnapp.dto.TestSessionResponse;
import com.learnapp.dto.CreateTopicSessionRequest;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.TestSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/sessions")
@Tag(name = "User Test Sessions", description = "Learning test session APIs")
public class UserTestSessionController {

    private final TestSessionService testSessionService;

    public UserTestSessionController(TestSessionService testSessionService) {
        this.testSessionService = testSessionService;
    }

    @Operation(summary = "Create daily session", description = "Create or return today's active daily session.")
    @PostMapping("/daily")
    public TestSessionResponse createDaily(@AuthenticationPrincipal UserPrincipal principal) {
        return testSessionService.createDailySession(principal.id());
    }

    @Operation(summary = "Create topic session", description = "Create an active practice session from one or more topics.")
    @PostMapping("/topic")
    public TestSessionResponse createTopicSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTopicSessionRequest request
    ) {
        return testSessionService.createTopicSession(
                principal.id(),
                request.topicIds(),
                request.totalItems()
        );
    }

    @Operation(summary = "Get session detail", description = "Get a session with ordered items.")
    @GetMapping("/{sessionId}")
    public TestSessionResponse getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        return testSessionService.getSession(principal.id(), sessionId);
    }

    @Operation(summary = "Submit answer", description = "Submit answer for one item in an active session.")
    @PostMapping("/{sessionId}/items/{itemId}/answer")
    public SubmitTestItemAnswerResponse answer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID itemId,
            @Valid @RequestBody SubmitTestItemAnswerRequest request
    ) {
        return testSessionService.submitAnswer(
                principal.id(),
                sessionId,
                itemId,
                request.answer(),
                request.timeMs()
        );
    }

    @Operation(summary = "Submit all answers", description = "Submit answers for all pending items in one request.")
    @PostMapping("/{sessionId}/answers")
    public SubmitTestSessionAnswersResponse submitAllAnswers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitTestSessionAnswersRequest request
    ) {
        return testSessionService.submitAllAnswers(
                principal.id(),
                sessionId,
                request.answers()
        );
    }

    @Operation(summary = "Complete session", description = "Mark an active session as completed.")
    @PostMapping("/{sessionId}/complete")
    public TestSessionResponse complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        return testSessionService.completeSession(principal.id(), sessionId);
    }

    @Operation(summary = "Abandon session", description = "Mark an active session as abandoned.")
    @PostMapping("/{sessionId}/abandon")
    public TestSessionResponse abandon(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        return testSessionService.abandonSession(principal.id(), sessionId);
    }
}
