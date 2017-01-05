# --- !Ups

CREATE TABLE provvedimento (
  id NUMERIC(19) PRIMARY KEY DEFAULT nextval('hibernate_sequence') NOT NULL,
  contrattoid BIGINT,
  divisioneid NUMERIC(19),
  codice VARCHAR(255) NOT NULL,
  descrizione TEXT NOT NULL,
  tipo VARCHAR(255) NOT NULL
  --CONSTRAINT fkce5ee450cb0d8919 FOREIGN KEY (contrattoid) REFERENCES tabcontrlav (id),
  --CONSTRAINT fkce5ee4502ce8e155 FOREIGN KEY (divisioneid) REFERENCES tabccosto (id)
);

# --- !Downs

DROP TABLE provvedimento;


