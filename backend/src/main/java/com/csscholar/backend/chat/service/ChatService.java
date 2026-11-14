package com.csscholar.backend.chat.service;

import com.csscholar.backend.chat.client.TutorClient;
import com.csscholar.backend.chat.dto.*;
import com.csscholar.backend.chat.entity.Conversation;
import com.csscholar.backend.chat.entity.Message;
import com.csscholar.backend.chat.entity.MessageSource;
import com.csscholar.backend.chat.repository.ConversationRepository;
import com.csscholar.backend.chat.repository.MessageRepository;
import com.csscholar.backend.chat.repository.MessageSourceRepository;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.entity.DocumentChunk;
import com.csscholar.backend.document.repository.DocumentChunkRepository;
import com.csscholar.backend.document.repository.DocumentRepository;
import com.csscholar.backend.workspace.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageSourceRepository messageSourceRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final WorkspaceService workspaceService;
    private final TutorClient tutorClient;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MessageSourceRepository messageSourceRepository,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            WorkspaceService workspaceService,
            TutorClient tutorClient
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageSourceRepository = messageSourceRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.workspaceService = workspaceService;
        this.tutorClient = tutorClient;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or unauthorized: " + conversationId));

        workspaceService.getWorkspaceById(userId, conversation.getWorkspace().getId());

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream().map(this::mapToMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or unauthorized: " + conversationId));

        UUID workspaceId = conversation.getWorkspace().getId();
        workspaceService.getWorkspaceById(userId, workspaceId);

        String userQuery = request.getContent().trim();

        // 1. Persist User Message
        Message userMessage = new Message(conversation, "USER", userQuery);
        messageRepository.save(userMessage);

        // 2. Fetch recent conversation history (last 8 messages prior to current query)
        List<Message> historyMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> historyList = new ArrayList<>();
        // Exclude the newly saved user message from prior history
        for (Message m : historyMessages) {
            if (m.getId() != null && m.getId().equals(userMessage.getId())) {
                continue;
            }
            Map<String, String> entry = new HashMap<>();
            entry.put("role", m.getRole().toLowerCase());
            entry.put("content", m.getContent());
            historyList.add(entry);
        }
        if (historyList.size() > 8) {
            historyList = historyList.subList(historyList.size() - 8, historyList.size());
        }

        // 3. Invoke Tutor Engine via TutorClient
        TutorChatInternalRequest tutorRequest = new TutorChatInternalRequest(
                userQuery,
                workspaceId,
                userId,
                conversationId,
                historyList,
                request.getProvider(),
                request.getModelName()
        );

        TutorChatInternalResponse tutorResponse = tutorClient.generateAnswer(tutorRequest);

        // 4. Persist Assistant Message
        Message assistantMessage = new Message(conversation, "ASSISTANT", tutorResponse.getAnswer());
        assistantMessage.setStatus(tutorResponse.getGroundingStatus());
        assistantMessage.setProvider(tutorResponse.getProvider());
        assistantMessage.setModelName(tutorResponse.getModelName());
        assistantMessage.setInputTokens(tutorResponse.getInputTokens());
        assistantMessage.setOutputTokens(tutorResponse.getOutputTokens());
        assistantMessage.setLatencyMs(tutorResponse.getLatencyMs());

        // 5. Persist Citations / Message Sources
        if (tutorResponse.getCitations() != null) {
            for (TutorCitationDto cit : tutorResponse.getCitations()) {
                Document doc = null;
                if (cit.getDocumentId() != null) {
                    doc = documentRepository.findById(cit.getDocumentId()).orElse(null);
                }
                DocumentChunk chunk = null;
                if (cit.getChunkId() != null) {
                    chunk = documentChunkRepository.findById(cit.getChunkId()).orElse(null);
                }

                MessageSource source = new MessageSource(
                        assistantMessage,
                        chunk,
                        doc,
                        cit.getRank(),
                        cit.getSourceId(),
                        cit.getRelevanceScore()
                );
                assistantMessage.addSource(source);
            }
        }

        Message savedAssistantMessage = messageRepository.save(assistantMessage);

        // 6. Update Conversation lastMessageAt and title if still default
        conversation.setLastMessageAt(Instant.now());
        if ("New Conversation".equalsIgnoreCase(conversation.getTitle())) {
            String derivedTitle = userQuery.length() > 60 ? userQuery.substring(0, 57) + "..." : userQuery;
            conversation.setTitle(derivedTitle);
        }
        conversationRepository.save(conversation);

        // 7. Return mapped response
        return mapToMessageResponse(savedAssistantMessage);
    }

    public MessageResponse mapToMessageResponse(Message message) {
        MessageResponse dto = new MessageResponse();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setProvider(message.getProvider());
        dto.setModelName(message.getModelName());
        dto.setInputTokens(message.getInputTokens());
        dto.setOutputTokens(message.getOutputTokens());
        dto.setLatencyMs(message.getLatencyMs());
        dto.setCreatedAt(message.getCreatedAt());

        if (message.getSources() != null) {
            List<MessageSourceDto> sourceDtos = new ArrayList<>();
            for (MessageSource s : message.getSources()) {
                MessageSourceDto sd = new MessageSourceDto();
                sd.setId(s.getId());
                sd.setSourceRank(s.getSourceRank());
                sd.setCitationLabel(s.getCitationLabel());
                sd.setRelevanceScore(s.getRelevanceScore());

                if (s.getDocument() != null) {
                    sd.setDocumentId(s.getDocument().getId());
                    sd.setDocumentTitle(s.getDocument().getTitle());
                }
                if (s.getChunk() != null) {
                    sd.setChunkId(s.getChunk().getId());
                    sd.setPageNumber(s.getChunk().getPageNumber());
                    sd.setSlideNumber(s.getChunk().getSlideNumber());
                    sd.setSectionHeading(s.getChunk().getSectionTitle());
                    sd.setChunkContent(s.getChunk().getContent());
                    if (sd.getDocumentId() == null && s.getChunk().getDocument() != null) {
                        sd.setDocumentId(s.getChunk().getDocument().getId());
                        sd.setDocumentTitle(s.getChunk().getDocument().getTitle());
                    }
                }
                sourceDtos.add(sd);
            }
            dto.setSources(sourceDtos);
        }

        return dto;
    }
}

