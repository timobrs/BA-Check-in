package de.ba.check.in.attendance.repository;

import de.ba.check.in.attendance.domain.AttendanceEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttendanceEventRepository extends JpaRepository<AttendanceEventEntity, UUID> {
}
