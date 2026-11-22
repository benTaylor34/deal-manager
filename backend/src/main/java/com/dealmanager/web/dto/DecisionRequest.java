package com.dealmanager.web.dto;

import jakarta.validation.constraints.Size;

/** The "why" for submit/approve/reject/revise. Required for reject (checked in the service). */
public record DecisionRequest(@Size(max = 2000) String comment) {
}
