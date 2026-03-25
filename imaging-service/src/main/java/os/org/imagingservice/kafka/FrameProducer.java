package os.org.imagingservice.kafka;

import os.org.imagingservice.dto.FrameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FrameProducer {

    private static final Logger log = LoggerFactory.getLogger(FrameProducer.class);

    private final KafkaTemplate<String, FrameEvent> kafkaTemplate;

    public FrameProducer(KafkaTemplate<String, FrameEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRawFrame(FrameEvent event) {
        kafkaTemplate.send("raw-frames", event.frameId(), event);
        log.info("Published raw frame {} to raw-frames", event.frameId());
    }

    public void sendProcessedFrame(FrameEvent event) {
        kafkaTemplate.send("processed-frames", event.frameId(), event);
        log.info("Published processed frame {} to processed-frames", event.frameId());
    }
}
