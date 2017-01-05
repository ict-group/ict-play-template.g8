# --- !Ups

CREATE TABLE parametricontestazioni (
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  contrattoid BIGINT,
  divisioneid NUMERIC(19),
  datainizio TIMESTAMP,
  maxgiornisospensione NUMERIC(19),
  giornipergiustificazione NUMERIC(19),
  giornirrcontestazione NUMERIC(19),
  maxoremulta NUMERIC(19),
  riferimentostatuto TEXT NOT NULL
  --CONSTRAINT fkce5ee450cb0d8919 FOREIGN KEY (contrattoid) REFERENCES tabcontrlav (id),
  --CONSTRAINT fkce5ee4502ce8e155 FOREIGN KEY (divisioneid) REFERENCES tabccosto (id)
);

CREATE UNIQUE INDEX uq__parametricontestazioni ON parametricontestazioni (contrattoid, divisioneid, datainizio);

CREATE TABLE firmataricontestazioni (
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  qualificaid NUMERIC(19),
  divisioneid NUMERIC(19),
  firmatariocontestazione VARCHAR(255),
  firmatarioprovvedimento VARCHAR(255)
);

CREATE TABLE destinatariemailcontestazioni (
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  areaid NUMERIC(19),
  mailcontestazione VARCHAR(255),
  mailprovvedimento VARCHAR(255),
  mailsospensione VARCHAR(255)
);

CREATE TABLE allegaticontestazioni (
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  contestazioneid NUMERIC(19),
  tipofile VARCHAR(255),
  nomefile VARCHAR(255),
  datacaricamento TIMESTAMP,
  objectid VARCHAR(255),
  destinazione VARCHAR(255)
);

ALTER TABLE contestazioniprovvedimenti ADD COLUMN passatoapaghe VARCHAR;

# --- !Downs

DROP TABLE parametricontestazioni;

DROP TABLE firmataricontestazioni;

DROP TABLE destinatariemailcontestazioni;
