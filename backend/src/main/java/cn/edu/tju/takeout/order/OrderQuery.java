package cn.edu.tju.takeout.order;

import java.time.LocalDateTime;

public record OrderQuery(
        String status, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer size) {}
