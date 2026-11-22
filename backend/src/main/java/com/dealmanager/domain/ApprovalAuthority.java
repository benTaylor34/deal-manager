package com.dealmanager.domain;

/** Who has the authority to approve a deal, given its discount. */
public enum ApprovalAuthority {
    /** Relationship manager: within delegated authority, so approved on submit. */
    RM,
    /** Discount too deep for the RM: needs a Pricing Committee decision. */
    PRICING_COMMITTEE
}
