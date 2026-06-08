package com.iot.monitor.alert.controller;

import com.iot.monitor.alert.service.AlertRuleService;
import com.iot.monitor.alert.service.AlertService;
import com.iot.monitor.common.dto.AlertRuleDTO;
import com.iot.monitor.common.dto.ApiResponse;
import com.iot.monitor.common.dto.PageResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts/rules")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @PostMapping
    public ApiResponse<AlertRuleDTO> createRule(@RequestBody AlertRuleDTO dto) {
        return ApiResponse.success(alertRuleService.createRule(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlertRuleDTO> updateRule(@PathVariable Long id, @RequestBody AlertRuleDTO dto) {
        return ApiResponse.success(alertRuleService.updateRule(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertRuleDTO> getRuleById(@PathVariable Long id) {
        return ApiResponse.success(alertRuleService.getRuleById(id));
    }

    @GetMapping
    public ApiResponse<PageResult<AlertRuleDTO>> listRules(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(alertRuleService.listRules(pageNum, pageSize, ruleName, alertType, alertLevel, enabled));
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<Void> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        alertRuleService.toggleRule(id, enabled);
        return ApiResponse.success();
    }
}
