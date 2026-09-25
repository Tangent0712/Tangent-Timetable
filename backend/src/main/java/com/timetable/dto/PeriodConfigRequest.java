package com.timetable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public class PeriodConfigRequest {
    @NotEmpty(message = "作息时间不能为空")
    @Valid
    private List<PeriodItem> configs;

    public List<PeriodItem> getConfigs() { return configs; }
    public void setConfigs(List<PeriodItem> configs) { this.configs = configs; }

    public static class PeriodItem {
        @NotNull(message = "节次不能为空")
        @Min(value = 1, message = "节次范围为 1-20")
        @Max(value = 20, message = "节次范围为 1-20")
        private Integer periodNumber;

        @NotNull(message = "开始时间不能为空")
        private LocalTime startTime;

        @NotNull(message = "结束时间不能为空")
        private LocalTime endTime;

        @NotBlank(message = "时段分类不能为空")
        private String category;

        public Integer getPeriodNumber() { return periodNumber; }
        public void setPeriodNumber(Integer periodNumber) { this.periodNumber = periodNumber; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
