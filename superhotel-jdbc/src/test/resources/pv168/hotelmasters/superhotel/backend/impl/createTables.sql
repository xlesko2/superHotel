CREATE TABLE "GUEST" (
    "ID" BIGINT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "GSTNAME" VARCHAR(255) NOT NULL,
    "ADDRESS" VARCHAR(255) NOT NULL,
    "BIRTHDAY" DATE,
    "CARDNUMBER" BIGINT
);

CREATE TABLE "ROOM" (
  "ID" BIGINT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  "ROOMNAME" VARCHAR(255) NOT NULL,
  "CAPACITY" INTEGER NOT NULL,
  "PRICE" DOUBLE NOT NULL
);

CREATE TABLE "ACCOMMODATION" (
  "ID" BIGINT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  "GUESTID" BIGINT REFERENCES GUEST (ID),
  "ROOMID" BIGINT REFERENCES ROOM (ID),
  "STARTDATE" DATE,
  "ENDDATE" DATE,
  "PRICE" DOUBLE
);
