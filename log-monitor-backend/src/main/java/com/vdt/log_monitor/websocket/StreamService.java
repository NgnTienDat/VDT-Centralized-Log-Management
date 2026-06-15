//package com.vdt.log_monitor.websocket;
//
//import com.vdt.log_monitor.common.dto.LogIngestedEvent;
//import com.vdt.log_monitor.common.dto.LogMessageDto;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class StreamService {
//    private final SimpMessagingTemplate messagingTemplate;
//    @Async
//    @EventListener
//    public void onLogIngested(LogIngestedEvent event) {
//        LogMessageDto log = event.getLog();
//        String topic = buildTopic(log);
//        System.out.println("Publishing to topic: " + topic);
//        messagingTemplate.convertAndSend(topic, log);
//    }
//}
