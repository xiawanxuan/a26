package com.iot.monitor.common.event;

import com.iot.monitor.common.entity.AlertRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertTriggeredEvent extends ApplicationEvent {

    private final AlertRecord alertRecord;

    public AlertTriggeredEvent(Object source, AlertRecord alertRecord) {
        super(source);
        this.alertRecord = alertRecord;
    }
}
