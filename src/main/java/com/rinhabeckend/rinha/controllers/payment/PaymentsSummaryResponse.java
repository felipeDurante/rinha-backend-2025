package com.rinhabeckend.rinha.controllers.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rinhabeckend.rinha.services.payment.SummaryData;

public record PaymentsSummaryResponse (@JsonProperty("default") SummaryData defaultGateway,
                                       SummaryData fallback){
}
