<%--
  _modul_simple.jsp — Template halaman modul pendataan sederhana.
  Tidak dimaksudkan sebagai halaman mandiri; dijadikan referensi pola JSP
  saat membangun modul baru yang menggunakan AIS2.Table + AIS2.Modal.

  Pola dasar:
    1. ais2-page-header  → judul, breadcrumb, tombol aksi utama
    2. ais2-filter-card  → form filter
    3. div#tabelXyz      → AIS2.Table.init(...)
  Setiap modul customise bagian ini sesuai field dari Action class-nya.
--%>
