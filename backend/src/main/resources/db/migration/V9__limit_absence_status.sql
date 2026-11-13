ALTER TABLE absence_reports
DROP CONSTRAINT chk_absence_reports_status;

UPDATE absence_reports
SET status = 'ARCHIVED'
WHERE status = 'WITHDRAWN';

ALTER TABLE absence_reports
ADD CONSTRAINT chk_absence_reports_status
CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'ARCHIVED'));
