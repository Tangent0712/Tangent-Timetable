package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CourseRequest {
    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String location;
    private String teacher;

    @NotNull(message = "星期不能为空")
    private Integer dayOfWeek;

    @NotNull(message = "开始节次不能为空")
    private Integer startPeriod;

    @NotNull(message = "结束节次不能为空")
    private Integer endPeriod;

    @NotNull(message = "周次不能为空")
    private List<Integer> weeks;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public Integer getStartPeriod() { return startPeriod; }
    public void setStartPeriod(Integer startPeriod) { this.startPeriod = startPeriod; }
    public Integer getEndPeriod() { return endPeriod; }
    public void setEndPeriod(Integer endPeriod) { this.endPeriod = endPeriod; }
    public List<Integer> getWeeks() { return weeks; }
    public void setWeeks(List<Integer> weeks) { this.weeks = weeks; }
}
