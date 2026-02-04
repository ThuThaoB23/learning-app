package com.learnapp.service;

import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.TopicRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public Page<Topic> listActive(Pageable pageable) {
        return topicRepository.findByStatusAndDeletedAtIsNull(TopicStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Topic getActiveById(UUID id) {
        Topic topic = topicRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found"));
        if (topic.getStatus() != TopicStatus.ACTIVE) {
            throw new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found");
        }
        return topic;
    }
}
