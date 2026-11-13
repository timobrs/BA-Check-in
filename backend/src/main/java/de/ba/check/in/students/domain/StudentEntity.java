package de.ba.check.in.students.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity extends TimestampedEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "matriculation_number", nullable = false, unique = true, length = 50)
    private String matriculationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_group_id", nullable = false)
    private CourseGroupEntity courseGroup;

    /** Erstellt einen Studierenden. */
    public static StudentEntity create(UserEntity user, String matriculationNumber, CourseGroupEntity courseGroup) {
        StudentEntity entity = new StudentEntity();
        entity.user = user;
        entity.matriculationNumber = matriculationNumber;
        entity.courseGroup = courseGroup;
        return entity;
    }
}
