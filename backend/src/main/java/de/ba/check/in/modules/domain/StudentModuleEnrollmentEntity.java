package de.ba.check.in.modules.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "student_module_enrollments", uniqueConstraints = @UniqueConstraint(
        name = "uq_student_module_enrollments", columnNames = {"student_id", "module_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentModuleEnrollmentEntity extends CreatedOnlyEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private UserEntity assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    /** Erstellt eine Modulzuordnung. */
    public static StudentModuleEnrollmentEntity create(StudentEntity student, ModuleEntity module,
            UserEntity actor, Instant now) {
        StudentModuleEnrollmentEntity enrollment = new StudentModuleEnrollmentEntity();
        enrollment.student = student;
        enrollment.module = module;
        enrollment.assignedBy = actor;
        enrollment.assignedAt = now;
        return enrollment;
    }
}
