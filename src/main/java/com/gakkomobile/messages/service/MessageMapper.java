package com.gakkomobile.messages.service;

import com.gakkomobile.messages.domain.ThreadType;
import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
interface MessageMapper {

    @Mapping(target = "id", source = "message.id")
    @Mapping(target = "threadId", source = "message.threadId")
    @Mapping(target = "senderId", source = "message.senderId")
    @Mapping(target = "content", source = "message.content")
    @Mapping(target = "isUrgent", source = "message.urgent")
    @Mapping(target = "isBuffered", source = "message.buffered")
    @Mapping(target = "scheduledDeliveryAt", source = "message.scheduledDeliveryAt")
    @Mapping(target = "status", source = "message.status")
    @Mapping(target = "createdAt", source = "message.createdAt")
    @Mapping(target = "senderName", source = "senderName")
    ChatMessageResponseDTO toResponse(ChatMessageEntity message, String senderName);

    @Mapping(target = "threadId", source = "id")
    @Mapping(target = "type", source = "threadType")
    @Mapping(target = "lastMessageAt", ignore = true)
    @Mapping(target = "lastMessageContent", ignore = true)
    @Mapping(target = "unreadCount", constant = "0L")
    ChatThreadSummaryResponseDTO toNewThreadSummary(ChatThreadEntity thread);

    @Mapping(target = "type", source = "threadType", qualifiedByName = "threadType")
    ChatThreadSummaryResponseDTO toSummary(ChatThreadSummaryView view);

    List<ChatThreadSummaryResponseDTO> toSummaries(List<ChatThreadSummaryView> views);

    default Instant map(OffsetDateTime value) {

        if (Objects.isNull(value)) {
            return null;
        }
        return value.toInstant();
    }

    @Named("threadType")
    default ThreadType threadType(String threadType) {

        return ThreadType.valueOf(threadType);
    }
}
