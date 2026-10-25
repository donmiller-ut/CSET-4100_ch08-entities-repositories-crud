\set ON_ERROR_STOP on

CREATE ROLE northstar_owner
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION;

CREATE ROLE northstar_migrator
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    IN ROLE northstar_owner;

CREATE ROLE northstar_app
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION;

CREATE DATABASE northstar_market OWNER northstar_owner;

\connect northstar_market

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE TEMPORARY ON DATABASE northstar_market FROM PUBLIC;
GRANT CONNECT ON DATABASE northstar_market TO northstar_migrator, northstar_app;

CREATE SCHEMA northstar AUTHORIZATION northstar_owner;

ALTER ROLE northstar_migrator IN DATABASE northstar_market
    SET search_path = northstar, pg_catalog;
ALTER ROLE northstar_app IN DATABASE northstar_market
    SET search_path = northstar, pg_catalog;

\echo 'Set untracked passwords with \password northstar_migrator and \password northstar_app.'
