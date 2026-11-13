package de.ba.check.in.students.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseGroupEntity extends TimestampedEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Erstellt einen Kurs. */
    public static CourseGroupEntity create(String name) {
        CourseGroupEntity entity = new CourseGroupEntity();
        entity.name = name;
        entity.active = true;
        return entity;
    }

}
