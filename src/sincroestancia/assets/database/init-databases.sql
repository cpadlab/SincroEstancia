
CREATE TABLE IF NOT EXISTS vuts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    cover TEXT,
    apikey TEXT,
    url TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS config (
    id INTEGER PRIMARY KEY DEFAULT 1,
    selected_vut INTEGER,
    google_calendar_id TEXT,
    google_credentials_path TEXT,
    FOREIGN KEY(selected_vut) REFERENCES vuts(id) ON DELETE SET NULL
);

INSERT OR IGNORE INTO config (id, selected_vut) VALUES (1, NULL);

CREATE TABLE IF NOT EXISTS days (
    vut_id INTEGER NOT NULL,
    day_date TEXT NOT NULL,
    day_price FLOAT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('reserved', 'free', 'paid')),
    season TEXT NOT NULL CHECK(season IN ('high', 'low', 'average')),
    is_synced BOOLEAN NOT NULL DEFAULT 0,
    google_event_id TEXT,    
    FOREIGN KEY(vut_id) REFERENCES vuts(id) ON DELETE CASCADE,
    PRIMARY KEY (vut_id, day_date)
);

CREATE TABLE IF NOT EXISTS reservations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vut_id INTEGER NOT NULL,
    guest_name TEXT NOT NULL,
    guest_dni TEXT NOT NULL,
    guest_email TEXT,
    guest_phone TEXT,
    check_in_date TEXT NOT NULL,
    check_out_date TEXT NOT NULL,
    pax_count INTEGER NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT 0,
    has_checkin BOOLEAN NOT NULL DEFAULT 0,
    has_checkout BOOLEAN NOT NULL DEFAULT 0,
    is_synced BOOLEAN NOT NULL DEFAULT 0,
    google_event_id TEXT,
    google_event_in_id TEXT,
    google_event_out_id TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY(vut_id) REFERENCES vuts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS checkins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reservation_id INTEGER NOT NULL,
    payment_method TEXT,
    payment_identifier TEXT,
    payment_holder TEXT,
    card_expiry_date TEXT,
    payment_date TEXT,
    confirmed_entry_time TEXT,
    confirmed_exit_time TEXT,
    final_price REAL,
    rules_accepted BOOLEAN NOT NULL DEFAULT 0,
    gdpr_accepted BOOLEAN NOT NULL DEFAULT 0,
    is_synced BOOLEAN NOT NULL DEFAULT 0,
    google_event_id TEXT,
    signed_at TEXT NOT NULL,
    FOREIGN KEY(reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS guests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    checkin_id INTEGER NOT NULL,
    fullname TEXT NOT NULL,
    surname1 TEXT NOT NULL,
    surname2 TEXT,
    sex TEXT NOT NULL,
    birth_date TEXT NOT NULL,
    nationality TEXT NOT NULL,
    id_document_type TEXT NOT NULL,
    id_document_number TEXT NOT NULL,
    id_support_number TEXT,
    address_full TEXT,
    address_municipality TEXT,
    address_country TEXT,
    phone TEXT,
    email TEXT,
    is_minor BOOLEAN NOT NULL DEFAULT 0,
    guardian_id INTEGER,
    FOREIGN KEY(checkin_id) REFERENCES checkins(id) ON DELETE CASCADE,
    FOREIGN KEY(guardian_id) REFERENCES guests(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS checkouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reservation_id INTEGER NOT NULL,
    actual_exit_time TEXT NOT NULL,
    keys_returned BOOLEAN NOT NULL DEFAULT 0,
    damage_detected BOOLEAN NOT NULL DEFAULT 0,
    damage_description TEXT,
    is_synced BOOLEAN NOT NULL DEFAULT 0,
    google_event_id TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY(reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
);