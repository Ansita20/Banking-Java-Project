package bankproject.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, ProcessedCommand.Key> {
}
