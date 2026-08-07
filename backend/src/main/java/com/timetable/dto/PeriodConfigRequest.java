package com.timetable.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public class PeriodConfigRequest {
    @NotNull
    private List<PeriodItem> configs;

    public List<PeriodItem> getConfigs() { return configs; }
    public void setConfigs(List<PeriodItem> configs) { this.configs = configs; }

    public static class PeriodItem {
        @NotNull
        private Integer periodNumber;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
        @NotNull
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
