package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ScheduleRequest {
    @NotBlank(message = "课表名称不能为空")
    private String name;

    @NotNull(message = "学期开始日期不能为空")
    private LocalDate periodStartDate;

    @NotNull(message = "学期结束日期不能为空")
    private LocalDate periodEndDate;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }
    public LocalDate getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }
}
