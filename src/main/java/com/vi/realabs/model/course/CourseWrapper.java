package com.vi.realabs.model.course;

import com.vi.realabs.model.course.Course;
import lombok.Data;

import java.util.List;

@Data
public class CourseWrapper {
    private List<Course> courses;
}
