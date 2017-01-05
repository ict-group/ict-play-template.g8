# --- !Ups

CREATE TABLE contestazione (
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  contrattoid BIGINT,
  divisioneid NUMERIC(19),
  codice VARCHAR(255) NOT NULL,
  descrizione TEXT NOT NULL,
  contestazione TEXT NOT NULL,
  recidiva TEXT NOT NULL
  --CONSTRAINT fkce5ee450cb0d8919 FOREIGN KEY (contrattoid) REFERENCES tabcontrlav (id),
  --CONSTRAINT fkce5ee4502ce8e155 FOREIGN KEY (divisioneid) REFERENCES tabccosto (id)
);

CREATE TABLE contestazioniprovvedimenti
(
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  azcod INTEGER NOT NULL,
  datainserimentocontestazione TIMESTAMP,
  dalleore TIMESTAMP,
  alleore TIMESTAMP,
  ownercontestazione VARCHAR(25),
  idindagatore NUMERIC(19),
  protocollocontestazione VARCHAR(255),
  dataevento TIMESTAMP,
  presso VARCHAR(255),
  iddipentente NUMERIC(19),
  idsegnalante NUMERIC(19),
  note TEXT,
  descrizionecontestazione TEXT,
  descrizionerecidiva TEXT,
  idtipocontestazione NUMERIC(19),
  sospensione BOOLEAN,
  datarrcontestazione TIMESTAMP,
  datarrfinecontestazione TIMESTAMP,
  idesitoraccomandata NUMERIC(19),
  notecontestazione TEXT,
  idfornitegiustificazioni NUMERIC(19),
  datagiustificazione TIMESTAMP,
  datafinecontestazione TIMESTAMP,
  giustificazionecontestazione TEXT,
  flagchiusuracontestazione BOOLEAN,
  datachiusuracontestazione TIMESTAMP,
  idpropostaprovvedimento NUMERIC(19),
  giornisospensioneproposti NUMERIC(19),
  oremultaproposte NUMERIC(19),
  datatrasfprovv TIMESTAMP,
  protocolloprovvedimento VARCHAR(255),
  idtipoprovvedimento NUMERIC(19),
  dataprovvedimento TIMESTAMP,
  giornisospensione NUMERIC(19),
  oremulta NUMERIC(19),
  dataraccomandataprovvedimento TIMESTAMP,
  ownerprovvedimento VARCHAR(255),
  notegenerali TEXT,
  dataapplicazionesospensione TIMESTAMP,
  noteprovvedimento TEXT,
  dataimpugnazione TIMESTAMP,
  noteimpugnazione TEXT
  -----
);

# --- !Downs

DROP TABLE contestazione;

DROP TABLE contestazioniprovvedimenti;

