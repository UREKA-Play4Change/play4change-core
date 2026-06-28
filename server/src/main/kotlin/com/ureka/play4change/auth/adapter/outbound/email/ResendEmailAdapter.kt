package com.ureka.play4change.auth.adapter.outbound.email

import com.ureka.play4change.auth.application.ResendProperties
import com.ureka.play4change.auth.port.outbound.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.ConditionContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
@Primary
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('\${resend.api-key:}')")
class ResendEmailAdapter(private val props: ResendProperties) : EmailPort {

    private val log = LoggerFactory.getLogger(ResendEmailAdapter::class.java)
    private val restTemplate = RestTemplate()

    override fun sendMagicLink(toEmail: String, token: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(props.apiKey)
        }
        val body = mapOf(
            "from" to props.from,
            "to" to listOf(toEmail),
            "subject" to "Your Play4Change sign-in code",
            "html" to buildHtmlBody(token),
            "text" to buildTextBody(token)
        )
        try {
            restTemplate.postForEntity(
                "https://api.resend.com/emails",
                HttpEntity(body, headers),
                Map::class.java
            )
            log.debug("Magic link email sent via Resend to {}", toEmail)
        } catch (ex: Exception) {
            log.error("Failed to send magic link email via Resend to {}: {}", toEmail, ex.message)
            throw IllegalStateException("Email delivery failed: ${ex.message}", ex)
        }
    }

    private fun buildHtmlBody(token: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#f4f7fb;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
            <tr><td align="center">
              <table width="520" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <tr>
                  <td style="background:linear-gradient(135deg,#1d4ed8,#16a34a);padding:32px 40px;text-align:center;">
                    <p style="margin:0;font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Play4Change</p>
                    <p style="margin:6px 0 0;font-size:13px;color:rgba(255,255,255,0.75);font-weight:400;">Adaptive learning powered by AI</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding:40px 40px 32px;">
                    <p style="margin:0 0 8px;font-size:22px;font-weight:700;color:#111827;">Your sign-in code</p>
                    <p style="margin:0 0 28px;font-size:15px;color:#6b7280;line-height:1.6;">
                      Copy the code below and paste it into the app or web sign-in screen.<br>
                      It expires in <strong style="color:#111827;">15 minutes</strong> and can only be used once.
                    </p>
                    <table width="100%" cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="background:#f0f9ff;border:2px solid #bfdbfe;border-radius:12px;padding:24px;text-align:center;">
                          <p style="margin:0 0 8px;font-size:11px;font-weight:600;color:#6b7280;text-transform:uppercase;letter-spacing:1px;">Sign-in code</p>
                          <p style="margin:0;font-size:15px;font-family:'Courier New',Courier,monospace;font-weight:700;color:#1d4ed8;word-break:break-all;letter-spacing:1px;">$token</p>
                        </td>
                      </tr>
                    </table>
                    <p style="margin:28px 0 0;font-size:13px;color:#9ca3af;line-height:1.6;">
                      If you didn't request this code, you can safely ignore this email — no action is needed.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td style="background:#f9fafb;border-top:1px solid #f3f4f6;padding:20px 40px;text-align:center;">
                    <p style="margin:0;font-size:12px;color:#9ca3af;">
                      &copy; 2025 Play4Change — ISEL &amp; U!REKA<br>
                      This is an automated message. Please do not reply.
                    </p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
    """.trimIndent()

    override fun sendRecoveryEmailVerification(toEmail: String, token: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(props.apiKey)
        }
        val body = mapOf(
            "from" to props.from,
            "to" to listOf(toEmail),
            "subject" to "Verify your Play4Change recovery email",
            "html" to buildRecoveryHtmlBody(token),
            "text" to buildRecoveryTextBody(token)
        )
        try {
            restTemplate.postForEntity(
                "https://api.resend.com/emails",
                HttpEntity(body, headers),
                Map::class.java
            )
            log.debug("Recovery email verification sent via Resend to {}", toEmail)
        } catch (ex: Exception) {
            log.error("Failed to send recovery email verification via Resend to {}: {}", toEmail, ex.message)
            throw IllegalStateException("Email delivery failed: ${ex.message}", ex)
        }
    }

    private fun buildTextBody(token: String): String = """
        Your Play4Change sign-in code:

            $token

        Copy this code and paste it into the sign-in screen.
        It expires in 15 minutes and can only be used once.

        If you did not request this, you can safely ignore this email.
    """.trimIndent()

    private fun buildRecoveryHtmlBody(token: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#f4f7fb;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
            <tr><td align="center">
              <table width="520" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <tr>
                  <td style="background:linear-gradient(135deg,#1d4ed8,#16a34a);padding:32px 40px;text-align:center;">
                    <p style="margin:0;font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Play4Change</p>
                    <p style="margin:6px 0 0;font-size:13px;color:rgba(255,255,255,0.75);font-weight:400;">Adaptive learning powered by AI</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding:40px 40px 32px;">
                    <p style="margin:0 0 8px;font-size:22px;font-weight:700;color:#111827;">Verify your recovery email</p>
                    <p style="margin:0 0 28px;font-size:15px;color:#6b7280;line-height:1.6;">
                      You added this address as a recovery email for your Play4Change account.<br>
                      Copy the code below and paste it into the app to verify it.<br>
                      The code expires in <strong style="color:#111827;">24 hours</strong>.
                    </p>
                    <table width="100%" cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="background:#f0fdf4;border:2px solid #bbf7d0;border-radius:12px;padding:24px;text-align:center;">
                          <p style="margin:0 0 8px;font-size:11px;font-weight:600;color:#6b7280;text-transform:uppercase;letter-spacing:1px;">Verification code</p>
                          <p style="margin:0;font-size:15px;font-family:'Courier New',Courier,monospace;font-weight:700;color:#16a34a;word-break:break-all;letter-spacing:1px;">$token</p>
                        </td>
                      </tr>
                    </table>
                    <p style="margin:28px 0 0;font-size:13px;color:#9ca3af;line-height:1.6;">
                      If you didn't add a recovery email, you can safely ignore this message — your account remains unchanged.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td style="background:#f9fafb;border-top:1px solid #f3f4f6;padding:20px 40px;text-align:center;">
                    <p style="margin:0;font-size:12px;color:#9ca3af;">
                      &copy; 2025 Play4Change — ISEL &amp; U!REKA<br>
                      This is an automated message. Please do not reply.
                    </p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
    """.trimIndent()

    private fun buildRecoveryTextBody(token: String): String = """
        Verify your Play4Change recovery email:

            $token

        Copy this code and paste it into the app to verify your recovery email.
        It expires in 24 hours.

        If you did not add a recovery email to your account, you can safely ignore this email.
    """.trimIndent()
}
