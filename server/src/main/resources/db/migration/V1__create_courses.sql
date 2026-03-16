-- V1__create_courses.sql
CREATE TABLE courses (
                         id              VARCHAR(36)  PRIMARY KEY,
                         title           VARCHAR(255) NOT NULL,
                         subject_domain  VARCHAR(100) NOT NULL,
                         status          VARCHAR(30)  NOT NULL DEFAULT 'IN_PROGRESS',
                         created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);