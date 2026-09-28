package za.co.unilinkhub.report.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.report.domain.Report;
import za.co.unilinkhub.report.domain.ReportStatus;
import za.co.unilinkhub.report.domain.ReportTargetType;
import za.co.unilinkhub.report.repository.ReportRepository;

import java.util.List;
import java.util.UUID;

public interface JpaReportRepository extends JpaRepository<Report, UUID>, ReportRepository {

    @Override
    List<Report> findByStatus(ReportStatus status);

    @Override
    List<Report> findByReporterId(UUID reporterId);

    @Override
    long countByStatus(ReportStatus status);

    @Override
    long countByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId);
}
