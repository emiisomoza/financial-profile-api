package com.finantialhub.finantialhubapi.infrastructure.persistence;

import com.finantialhub.finantialhubapi.domain.model.SummarySubscription;
import org.springframework.data.repository.ListCrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SummarySubscriptionRepository extends ListCrudRepository<SummarySubscription, UUID> {

    Optional<SummarySubscription> findByUserIdAndStatus(UUID userId, SummarySubscription.Status status);

    List<SummarySubscription> findByStatusAndNextSendAtBefore(
            SummarySubscription.Status status, Instant threshold);
}
