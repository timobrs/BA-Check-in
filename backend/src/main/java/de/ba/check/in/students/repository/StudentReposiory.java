package de.ba.check.in.students.repository;

import de.ba.check.in.students.domain.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentReposiory extends JpaRepository<StudentEntity, UUID> {
}
