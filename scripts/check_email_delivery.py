#!/usr/bin/env python3
"""
Diagnoses magic-link email delivery by:
  1. Checking DNS (SPF / DKIM / DMARC) on the sending domain
  2. Sending a real email via the Resend API
  3. Polling Resend for the final delivery status

Usage:
    python3 scripts/check_email_delivery.py <to_email> <resend_api_key>

Example:
    python3 scripts/check_email_delivery.py rades.govind@yahoo.com re_xxxxxxxxxxxx
"""

import json
import subprocess
import sys
import time
import urllib.request
import urllib.error

SENDING_DOMAIN = "radesh-govind.com"
FROM_ADDRESS   = f"noreply@{SENDING_DOMAIN}"
RESEND_API     = "https://api.resend.com"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def dig(name: str, record_type: str) -> str:
    result = subprocess.run(
        ["dig", "+short", record_type, name],
        capture_output=True, text=True
    )
    return result.stdout.strip()


def section(title: str):
    print(f"\n{'─' * 60}")
    print(f"  {title}")
    print('─' * 60)


HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "application/json",
}


def resend_post(path: str, payload: dict, api_key: str) -> dict:
    data = json.dumps(payload).encode()
    req  = urllib.request.Request(
        RESEND_API + path,
        data=data,
        headers={**HEADERS, "Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def resend_get(path: str, api_key: str) -> dict:
    req = urllib.request.Request(
        RESEND_API + path,
        headers={**HEADERS, "Authorization": f"Bearer {api_key}"},
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


# ---------------------------------------------------------------------------
# Step 1 — DNS checks
# ---------------------------------------------------------------------------

def check_dns():
    section("1 / DNS — SPF · DKIM · DMARC")

    # SPF
    spf = dig(SENDING_DOMAIN, "TXT")
    spf_records = [line for line in spf.splitlines() if "v=spf1" in line]
    if spf_records:
        print(f"  ✅  SPF  : {spf_records[0]}")
    else:
        print(f"  ❌  SPF  : no SPF record found for {SENDING_DOMAIN}")

    # DKIM — Resend publishes the key at resend._domainkey.<domain>
    dkim = dig(f"resend._domainkey.{SENDING_DOMAIN}", "TXT")
    if dkim:
        preview = dkim[:80] + "…" if len(dkim) > 80 else dkim
        print(f"  ✅  DKIM : {preview}")
    else:
        print(f"  ❌  DKIM : no record at resend._domainkey.{SENDING_DOMAIN}")

    # DMARC
    dmarc = dig(f"_dmarc.{SENDING_DOMAIN}", "TXT")
    if dmarc:
        print(f"  ✅  DMARC: {dmarc}")
    else:
        print(f"  ⚠️   DMARC: no DMARC record — Yahoo requires this for good deliverability")


# ---------------------------------------------------------------------------
# Step 2 — Send test email via Resend
# ---------------------------------------------------------------------------

def send_test_email(to_email: str, api_key: str) -> str:
    section("2 / Sending test email via Resend API")

    payload = {
        "from":    FROM_ADDRESS,
        "to":      [to_email],
        "subject": "[Play4Change] Delivery test",
        "html": (
            "<p>This is a delivery test from the Play4Change diagnostic script.</p>"
            "<p>If you can read this, email delivery is working correctly.</p>"
        ),
        "text": (
            "This is a delivery test from the Play4Change diagnostic script.\n"
            "If you can read this, email delivery is working correctly."
        ),
    }

    try:
        resp = resend_post("/emails", payload, api_key)
        email_id = resp.get("id", "")
        print(f"  ✅  Accepted by Resend — email ID: {email_id}")
        return email_id
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"  ❌  Resend rejected the request: HTTP {e.code}")
        print(f"      {body}")
        sys.exit(1)


# ---------------------------------------------------------------------------
# Step 3 — Poll Resend for delivery status
# ---------------------------------------------------------------------------

def poll_status(email_id: str, api_key: str, attempts: int = 8, interval: int = 5):
    section("3 / Polling Resend for delivery status")

    terminal_statuses = {"delivered", "bounced", "complained", "failed"}

    for i in range(1, attempts + 1):
        print(f"  [{i}/{attempts}] checking…", end=" ", flush=True)
        try:
            data   = resend_get(f"/emails/{email_id}", api_key)
            status = data.get("last_event", data.get("status", "unknown"))
            print(status)

            if status in terminal_statuses:
                print()
                if status == "delivered":
                    print("  ✅  DELIVERED — the email reached Yahoo's servers.")
                    print("      If it is not in the inbox, check the spam / junk folder.")
                elif status == "bounced":
                    print("  ❌  BOUNCED — Yahoo rejected the email.")
                    print("      This means the address does not exist or Yahoo's MX refused it.")
                elif status == "complained":
                    print("  ⚠️   COMPLAINT — recipient marked a previous email as spam.")
                    print("      Yahoo will silently drop future emails from this sender.")
                elif status == "failed":
                    print("  ❌  FAILED — Resend could not deliver (check Resend dashboard logs).")
                return

        except urllib.error.HTTPError as e:
            print(f"HTTP {e.code} — retrying")

        if i < attempts:
            time.sleep(interval)

    print("\n  ⏳  Status still pending after all attempts.")
    print("     Open https://resend.com/emails to check manually.")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)

    to_email, api_key = sys.argv[1], sys.argv[2]
    print(f"\n  Target  : {to_email}")
    print(f"  From    : {FROM_ADDRESS}")

    check_dns()
    email_id = send_test_email(to_email, api_key)
    poll_status(email_id, api_key)

    print()
