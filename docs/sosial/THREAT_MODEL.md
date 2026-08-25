# Threat Model Modul Sosial

| Ancaman | Kontrol utama | Evidence wajib |
|---|---|---|
| Tenant swap/IDOR | tenant server-side, ownership query | cross-tenant/owner test |
| Nominal/status tampering | server total + state machine | mutation tests |
| Callback forgery/replay | channel HMAC, IP, row lock, fingerprint | invalid/duplicate/concurrent fixtures |
| Double allocation/receipt | transaction/row lock, unique receipt | concurrency test |
| Privilege escalation | service capability guard + menu privilege | direct endpoint denial |
| CSRF/abuse | CSRF, rate limit, POST-only | negative tests |
| XSS/injection | output escaping, allow-list/path validation, ORM parameters | payload tests |
| Secret leakage | encrypted storage, password UI, redacted payload | log/export scan |
| Receipt forgery | opaque token, payment-verified issuance | invalid/void token test |
| Insider correction | maker-checker, immutable event/audit | same-actor denial |
| Proxy IP spoof | remoteAddr only; trusted proxy must be explicit | topology review |

Residual blockers: provider contract, legacy DES-based secret encryption, runtime evidence, and formal privacy retention approval.
