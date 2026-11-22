create table client (
    id      bigserial primary key,
    name    varchar(200) not null unique,
    segment varchar(50)  not null
);

create table deal (
    id                 bigserial primary key,
    client_id          bigint       not null references client (id),
    name               varchar(200) not null,
    status             varchar(30)  not null,
    start_date         date         not null,
    end_date           date         not null,
    required_authority varchar(30),
    created_by         varchar(100) not null,
    created_at         timestamptz  not null,
    updated_at         timestamptz  not null,
    version            bigint       not null default 0,
    constraint deal_dates_chk check (end_date > start_date)
);
create index deal_status_idx on deal (status);

create table pricing_line (
    id               bigserial primary key,
    deal_id          bigint         not null references deal (id) on delete cascade,
    service          varchar(100)   not null,
    standard_price   numeric(19, 4) not null check (standard_price > 0),
    proposed_price   numeric(19, 4) not null check (proposed_price >= 0),
    committed_volume bigint         not null check (committed_volume > 0)
);
create index pricing_line_deal_idx on pricing_line (deal_id);

-- Append-only: the application never updates or deletes audit rows.
create table audit_event (
    id          bigserial primary key,
    deal_id     bigint       not null references deal (id),
    actor       varchar(100) not null,
    action      varchar(40)  not null,
    from_status varchar(30),
    to_status   varchar(30),
    reason      text,
    details     jsonb,
    occurred_at timestamptz  not null
);
create index audit_event_deal_idx on audit_event (deal_id, occurred_at);
