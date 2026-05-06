package com.safepark.controller;

import com.safepark.entity.ChatMessage;
import com.safepark.entity.User;
import com.safepark.repository.ChatMessageRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 챗봇 메시지 전송
     * POST /api/chatbot/message
     * body: { message, context (optional) }
     */
    @PostMapping("/message")
    @Transactional
    public ResponseEntity<?> sendMessage(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request
    ) {
        try {
            User user = getUserFromToken(token);

            String message = request.getOrDefault("message", "").toString();
            if (message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "메시지를 입력해주세요"));
            }

            Long analysisId = request.containsKey("context")
                    ? Long.parseLong(request.get("context").toString())
                    : null;

            // 사용자 메시지 저장
            ChatMessage userMsg = new ChatMessage();
            userMsg.setUserId(user.getId());
            userMsg.setAnalysisId(analysisId);
            userMsg.setRole("user");
            userMsg.setContent(message);
            chatMessageRepository.save(userMsg);

            // AI 응답 생성 (추후 실제 AI 연동 필요)
            String aiResponse = generateResponse(message);

            // 어시스턴트 메시지 저장
            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setUserId(user.getId());
            assistantMsg.setAnalysisId(analysisId);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(aiResponse);
            chatMessageRepository.save(assistantMsg);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "userMessage", message,
                            "assistantMessage", aiResponse
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 대화 내역 조회
     * GET /api/chatbot/history?limit=50
     */
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "50") int limit
    ) {
        try {
            User user = getUserFromToken(token);

            List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
            List<Map<String, Object>> result = messages.stream()
                    .limit(limit)
                    .map(msg -> Map.<String, Object>of(
                            "id", msg.getId(),
                            "role", msg.getRole(),
                            "content", msg.getContent(),
                            "createdAt", msg.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 대화 이력 삭제
     * DELETE /api/chatbot/history
     */
    @DeleteMapping("/history")
    @Transactional
    public ResponseEntity<?> deleteChatHistory(@RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);
            chatMessageRepository.deleteByUserId(user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "대화 이력이 삭제되었습니다"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private User getUserFromToken(String token) {
        String jwt = token.replace("Bearer ", "");
        String username = jwtTokenProvider.getUsernameFromToken(jwt);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // 추후 실제 AI API 연동으로 교체 필요
    private String generateResponse(String message) {
        if (message.contains("주차")) {
            return "주차 관련 문의를 해주셨군요. 현재 위치에서 가까운 주차장을 찾아드릴까요? 앱의 지도 기능을 이용해 주변 주차장을 확인해보세요.";
        } else if (message.contains("과태료") || message.contains("단속")) {
            return "과태료 및 단속 관련 문의입니다. 안전한 주차를 위해 단속구역을 확인하고, 주차 전 사진 분석 기능을 활용해 위험도를 미리 확인해보세요.";
        } else {
            return "안녕하세요! SafePark 챗봇입니다. 주차 관련 궁금한 점을 물어보세요. 주변 주차장 안내, 단속구역 확인, 과태료 확률 분석 등을 도와드립니다.";
        }
    }
}
