package com.library.task;

import com.library.mapper.BorrowRecordMapper;
import com.library.service.StatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 逾期标记任务：把已过应还日期的「借阅中」记录改为「已逾期」。
 * 业务判断本身不依赖状态（按 due_time 实时算），这里落库是为了看板统计准确。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueTask {

    private final BorrowRecordMapper recordMapper;
    private final StatService statService;

    @Scheduled(cron = "0 0 * * * ?")
    public void markOverdue() {
        int count = recordMapper.markOverdue();
        if (count > 0) {
            log.info("逾期任务：标记 {} 条借阅记录为逾期", count);
            statService.invalidate();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        markOverdue();
    }
}
