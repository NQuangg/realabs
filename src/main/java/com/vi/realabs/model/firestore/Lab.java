package com.vi.realabs.model.firestore;

import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Lab {
    private String id;
    private String title;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lab lab = (Lab) o;
        return id.equals(lab.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
