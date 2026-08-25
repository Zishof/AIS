# Keputusan Registrasi Member Umum

Keputusan V1 pilot: `OUT_OF_SCOPE_V1 + DISABLED_BY_DEFAULT`.

Halaman `/sosial/daftar` tetap informasional dan tidak menyediakan form/CTA yang seolah membuat akun. Donasi memerlukan akun AIS existing. Flag `sosial_general_registration_enabled` dan `sosial_allow_guest_donation` harus tetap off.

Jika dibuka pada V2, wajib ada verifikasi email/WhatsApp, password policy/hash AIS, anti-bot/rate limit, duplicate detection/merge, consent, minimal role tanpa self-selection, tenant binding, recovery, audit, privacy retention, serta security UAT.
