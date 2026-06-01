{
  "version": "2.0",
  "summary": {
    "title": "Play4Change Threat Model",
    "owner": "Radesh Govind",
    "description": "OWASP Threat Dragon model for Play4Change. Created April 2026 per ADR-018. Updated at the start of each phase that introduces a new attack surface.",
    "id": 0
  },
  "detail": {
    "contributors": [],
    "diagrams": [
      {
        "id": 0,
        "title": "Play4Change — System Overview",
        "diagramType": "STRIDE",
        "placeholder": "Drag and drop from the stencil to add components",
        "thumbnail": "",
        "version": "2.0",
        "cells": [
          {
            "type": "tm.BoundaryBox",
            "size": { "width": 720, "height": 300 },
            "position": { "x": 0, "y": 0 },
            "attrs": { "text": { "text": "Public Internet" } },
            "id": "boundary-internet"
          },
          {
            "type": "tm.BoundaryBox",
            "size": { "width": 720, "height": 200 },
            "position": { "x": 0, "y": 320 },
            "attrs": { "text": { "text": "DMZ — Nginx Reverse Proxy" } },
            "id": "boundary-dmz"
          },
          {
            "type": "tm.BoundaryBox",
            "size": { "width": 720, "height": 300 },
            "position": { "x": 0, "y": 540 },
            "attrs": { "text": { "text": "Application Tier — Spring Boot Server" } },
            "id": "boundary-app"
          },
          {
            "type": "tm.BoundaryBox",
            "size": { "width": 720, "height": 200 },
            "position": { "x": 0, "y": 860 },
            "attrs": { "text": { "text": "Data Tier" } },
            "id": "boundary-data"
          },
          {
            "type": "tm.Actor",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 60, "y": 40 },
            "attrs": { "text": { "text": "Learner\n(Mobile App)" } },
            "id": "actor-learner"
          },
          {
            "type": "tm.Actor",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 220, "y": 40 },
            "attrs": { "text": { "text": "Admin\n(Browser)" } },
            "id": "actor-admin"
          },
          {
            "type": "tm.Actor",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 400, "y": 40 },
            "attrs": { "text": { "text": "Mistral AI\n(External API)" } },
            "id": "actor-mistral"
          },
          {
            "type": "tm.Actor",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 540, "y": 40 },
            "attrs": { "text": { "text": "Resend\n(Email)" } },
            "id": "actor-resend"
          },
          {
            "type": "tm.Actor",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 400, "y": 140 },
            "attrs": { "text": { "text": "Google OAuth\n(JWKS)" } },
            "id": "actor-google"
          },
          {
            "type": "tm.Actor",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 540, "y": 140 },
            "attrs": { "text": { "text": "Facebook OAuth\n(Graph API)" } },
            "id": "actor-facebook"
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 140, "y": 360 },
            "attrs": { "text": { "text": "Nginx\nProxy" } },
            "id": "process-nginx"
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 60, "y": 580 },
            "attrs": { "text": { "text": "Identity\n(auth/)" } },
            "id": "process-identity",
            "threats": [
              {
                "id": "t1",
                "title": "Magic link token stolen from DB",
                "type": "Information disclosure",
                "status": "Mitigated",
                "severity": "High",
                "description": "Database compromise exposes all pending magic link tokens.",
                "mitigation": "Tokens stored as SHA-256 hashes only. Raw token never persisted. ADR-016 G1.",
                "modelType": "STRIDE",
                "new": false,
                "number": 1
              },
              {
                "id": "t2",
                "title": "Magic link double-claim (TOCTOU)",
                "type": "Tampering",
                "status": "Mitigated",
                "severity": "Medium",
                "description": "Race condition allows a single-use token to authenticate two sessions.",
                "mitigation": "Atomic CTE UPDATE in MagicLinkTokenJpaRepository. ADR-016 G2.",
                "modelType": "STRIDE",
                "new": false,
                "number": 2
              },
              {
                "id": "t3",
                "title": "Google ID token audience not validated",
                "type": "Spoofing",
                "status": "Mitigated",
                "severity": "High",
                "description": "ID token issued to a different app accepted by Play4Change.",
                "mitigation": "Audience validation mandatory. Blank GOOGLE_CLIENT_ID is a hard error. ADR-016 G3.",
                "modelType": "STRIDE",
                "new": false,
                "number": 3
              },
              {
                "id": "t4",
                "title": "Facebook OAuth token not app-verified",
                "type": "Spoofing",
                "status": "Open",
                "severity": "Medium",
                "description": "Facebook access token from any app accepted. Audience not verified.",
                "mitigation": "Deferred. Requires FACEBOOK_APP_ID + FACEBOOK_APP_SECRET. ADR-016 G4.",
                "modelType": "STRIDE",
                "new": false,
                "number": 4
              },
              {
                "id": "t5",
                "title": "Brute force on /auth/magic-link",
                "type": "Denial of service",
                "status": "Open",
                "severity": "High",
                "description": "No rate limiting allows email spam and enumeration.",
                "mitigation": "Bucket4j + Redis rate limiting planned Phase 07 Task 7.1. THREAT-LOG R05.",
                "modelType": "STRIDE",
                "new": false,
                "number": 5
              }
            ]
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 200, "y": 580 },
            "attrs": { "text": { "text": "Topic\n(topic/)" } },
            "id": "process-topic",
            "threats": [
              {
                "id": "t6",
                "title": "SSRF via admin URL ingestion",
                "type": "Information disclosure",
                "status": "Open",
                "severity": "High",
                "description": "Admin submits an internal-network URL to POST /admin/topics.",
                "mitigation": "RFC 1918 + loopback block + https-only implemented Phase 02 Task 2.4. ZAP to confirm Phase 07 Task 7.4. THREAT-LOG R06.",
                "modelType": "STRIDE",
                "new": false,
                "number": 6
              },
              {
                "id": "t7",
                "title": "Prompt injection via malicious URL content",
                "type": "Tampering",
                "status": "Mitigated",
                "severity": "Medium",
                "description": "Malicious content at the ingested URL manipulates the Mistral system prompt.",
                "mitigation": "Output schema validation. AI output deserialized into typed Kotlin data class. Phase 02 Task 2.4.",
                "modelType": "STRIDE",
                "new": false,
                "number": 7
              },
              {
                "id": "t8",
                "title": "XSS via AI-generated HTML stored in DB",
                "type": "Tampering",
                "status": "Mitigated",
                "severity": "High",
                "description": "Mistral returns HTML/script content stored and later rendered as XSS.",
                "mitigation": "jsoup Safelist.none() strips all HTML before persistence. Phase 02 Task 2.4.",
                "modelType": "STRIDE",
                "new": false,
                "number": 8
              }
            ]
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 340, "y": 580 },
            "attrs": { "text": { "text": "Enrollment\n(enrollment/)" } },
            "id": "process-enrollment"
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 480, "y": 580 },
            "attrs": { "text": { "text": "Struggle\n(struggle/)" } },
            "id": "process-struggle"
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 340, "y": 660 },
            "attrs": { "text": { "text": "PeerReview\n(peerreview/)" } },
            "id": "process-peerreview"
          },
          {
            "type": "tm.Process",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 480, "y": 660 },
            "attrs": { "text": { "text": "Badge\n(badge/)" } },
            "id": "process-badge",
            "threats": [
              {
                "id": "t9",
                "title": "Badge issued without completing all tasks",
                "type": "Elevation of privilege",
                "status": "Mitigated",
                "severity": "Medium",
                "description": "Badge issuance endpoint called bypassing task completion check.",
                "mitigation": "BadgeIssuanceService validates task completion before issuing. DB unique constraint prevents double-issuance.",
                "modelType": "STRIDE",
                "new": false,
                "number": 9
              }
            ]
          },
          {
            "type": "tm.Store",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 60, "y": 900 },
            "attrs": { "text": { "text": "PostgreSQL 16\n+ pgvector" } },
            "id": "store-postgres"
          },
          {
            "type": "tm.Store",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 220, "y": 900 },
            "attrs": { "text": { "text": "Redis 7\n(cache)" } },
            "id": "store-redis"
          },
          {
            "type": "tm.Store",
            "size": { "width": 112, "height": 60 },
            "position": { "x": 380, "y": 900 },
            "attrs": { "text": { "text": "MinIO\n(S3-compat)" } },
            "id": "store-minio"
          }
        ]
      }
    ]
  }
}
