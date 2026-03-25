package com.finantialhub.finantialhubapi.domain.ports;

import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import com.finantialhub.finantialhubapi.domain.model.User;

public interface SummaryPublisherPort {
    void publish(SummaryService.Summary summary, User user);
}
