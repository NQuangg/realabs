package com.vi.realabs.model.member;

import com.vi.realabs.model.member.Course;
import lombok.Data;

import java.util.List;

@Data
public class CourseWrapper {
    private List<Course> courses;
}
