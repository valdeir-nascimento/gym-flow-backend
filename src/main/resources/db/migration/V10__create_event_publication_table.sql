-- Spring Modulith event publication registry (spring-modulith-starter-jpa).
-- With ddl-auto=validate the JpaEventPublication entity is validated at startup, and
-- `spring.modulith.republish-outstanding-events-on-restart=true` reads this table on boot,
-- so it must exist in every environment. Schema mirrors Modulith's canonical PostgreSQL
-- definition (id + listener/event/payload + publication/completion timestamps).

CREATE TABLE event_publication (
    id               UUID         NOT NULL,
    listener_id      TEXT         NOT NULL,
    event_type       TEXT         NOT NULL,
    serialized_event TEXT         NOT NULL,
    publication_date TIMESTAMPTZ  NOT NULL,
    completion_date  TIMESTAMPTZ,
    PRIMARY KEY (id)
);

-- Republish-on-restart looks up still-incomplete publications (completion_date IS NULL).
CREATE INDEX ix_event_publication_incomplete
    ON event_publication (publication_date)
    WHERE completion_date IS NULL;

-- Marking a publication complete matches on (listener, event payload).
CREATE INDEX ix_event_publication_listener_event
    ON event_publication (listener_id, serialized_event);
