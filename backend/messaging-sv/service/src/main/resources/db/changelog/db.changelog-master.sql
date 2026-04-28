--liquibase formatted sql

--changeset r8n:V1_create_messaging_schema
CREATE SCHEMA IF NOT EXISTS messaging;
