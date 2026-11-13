package de.ba.check.in.modules.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import de.ba.check.in.students.domain.CourseGroupEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Entity
@Table(name = "modules", uniqueConstraints = @UniqueConstraint(
        name = "uq_modules_course_normalized_name", columnNames = {"course_group_id", "normalized_name"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModuleEntity extends TimestampedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_group_id", nullable = false)
    private CourseGroupEntity courseGroup;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_user_id")
    private UserEntity lecturerUser;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "ignored", nullable = false)
    private boolean ignored = false;

    /** Erstellt ein Modul. */
    public static ModuleEntity create(CourseGroupEntity courseGroup, String name) {
        ModuleEntity module = new ModuleEntity();
        module.courseGroup = courseGroup;
        module.name = name.trim();
        module.normalizedName = normalizeName(name);
        return module;
    }

    /** Vereinheitlicht den Namen. */
    public static String normalizeName(String value) {
        return (value == null ? "" : value.trim()).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
