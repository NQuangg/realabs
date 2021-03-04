package com.vi.realabs.model.firestore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Classroom {
    private String teacherId;
    private List<Lab> labs;
}
