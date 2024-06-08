CREATE TABLE coincap (
    changePercent numeric(60,20),
    marketCapUsd numeric(60,20),
    maxSupply numeric(60,20),
    priceUsd numeric(60,20),
    rank integer,
    supply numeric(60,20),
    volumeDay numeric(60,20),
    vwmap numeric(60,20),
    lastUpdate bigint,
    id varchar(255) not null,
    name varchar(255),
    symbol varchar(255),
    primary key (id)
);

ALTER TABLE coincap REPLICA IDENTITY FULL;
