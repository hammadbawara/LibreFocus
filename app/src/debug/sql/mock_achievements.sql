-- Mock achievement seed data for manual testing.
-- This targets the Room achievements table used by LibreFocus.
-- The same (type, sourceDateUtc) combination must stay unique.

DELETE FROM achievements;

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_WEEK',
    strftime('%s', 'now', '-21 days') * 1000,
    strftime('%s', 'now', '-21 days') * 1000,
    1,
    7
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_WEEK',
    strftime('%s', 'now', '-14 days') * 1000,
    strftime('%s', 'now', '-14 days') * 1000,
    2,
    7
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_WEEK',
    strftime('%s', 'now', '-7 days') * 1000,
    strftime('%s', 'now', '-7 days') * 1000,
    3,
    7
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_MONTH',
    strftime('%s', 'now', '-2 days') * 1000,
    strftime('%s', 'now', '-2 days') * 1000,
    1,
    30
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_10',
    strftime('%s', 'now', '-9 days') * 1000,
    strftime('%s', 'now', '-9 days') * 1000,
    1,
    10
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_20',
    strftime('%s', 'now', '-19 days') * 1000,
    strftime('%s', 'now', '-19 days') * 1000,
    1,
    20
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_30',
    strftime('%s', 'now', '-29 days') * 1000,
    strftime('%s', 'now', '-29 days') * 1000,
    1,
    30
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_100',
    strftime('%s', 'now', '-99 days') * 1000,
    strftime('%s', 'now', '-99 days') * 1000,
    1,
    100
);

INSERT INTO achievements (type, achievedAtUtc, sourceDateUtc, occurrenceCount, thresholdValue)
VALUES (
    'PERFECT_365',
    strftime('%s', 'now', '-364 days') * 1000,
    strftime('%s', 'now', '-364 days') * 1000,
    1,
    365
);
