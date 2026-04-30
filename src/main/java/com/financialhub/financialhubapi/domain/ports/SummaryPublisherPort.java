package com.financialhub.financialhubapi.domain.ports;

import com.financialhub.financialhubapi.application.usecases.SummaryService;
import com.financialhub.financialhubapi.domain.model.User;

public interface SummaryPublisherPort {
    void publish(SummaryService.Summary summary, User user);
}
