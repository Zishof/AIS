<%@page import="ais.database.model.rab.SatuanKerja"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.StatuskehadiranKaryawanHarian"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.payroll.CutiDanIzin"%>
<%@page import="ais.common.Common"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.*"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_presensi_harian.jsp:24");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
%><%!
    /** Konversi URL foto absolut ke relatif agar tidak rusak saat IP/host server berubah.
     *  Hanya memproses URL /al?d= (endpoint lampiran lokal); Google Drive dll dibiarkan. */
    private static String toRelativeFotoUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        if (!url.startsWith("http")) return url;
        int idx = url.indexOf("/al?d=");
        if (idx >= 0) return url.substring(idx); // "/al?d=..."
        idx = url.indexOf("/ais/al?d=");
        if (idx >= 0) return url.substring(idx);
        return url;
    }
%><%
    
    Tbmuser currentUser = Common.getCurrentUser(request);
    if (currentUser == null) { 
        out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; 
    }

    String startDateStr = request.getParameter("startDate");
    String endDateStr = request.getParameter("endDate");
    String q = request.getParameter("q");
    String jenisEntitas = request.getParameter("jenisEntitas");
    String statusAbsensiId = request.getParameter("statusAbsensiId");
    String satuanKerjaId = request.getParameter("satuanKerjaId");
    String[] hariAktifArr = request.getParameterValues("hariAktif");
    
    // Konversi Array Hari Aktif
    List<Integer> activeDays = new ArrayList<>();
    if (hariAktifArr != null) {
        for (String h : hariAktifArr) { activeDays.add(Integer.parseInt(h)); }
    }

    Session sess = null;
    try {
        sess = HibernateUtil.openSession();
		Criteria crit = sess.createCriteria(StatuskehadiranKaryawanHarian.class);
        
        // --- BLOK 1: HAK AKSES & OTORISASI (Kompatibel dengan Hibernate 3.6) ---
        Tbmrole tbmrole = currentUser.hakAkses();
        boolean bolehLihatPegawaiLain = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataPegawaiLain());
        boolean bolehLihatSatuanKerja = tbmrole != null && Boolean.TRUE.equals(tbmrole.getMelihatDataSatkerLain());
        
        Pegawai myPegawai = currentUser.ambilPegawai();
        Dosen myDosen = currentUser.ambilDosen();
        Guru myGuru = currentUser.ambilGuru();
        Mahasiswa myMhs = currentUser.getMahasiswa();
        Siswa mySiswa = currentUser.getSiswa();
        SatuanKerja mySatker = currentUser.ambilSatuanKerja();

        if (!bolehLihatPegawaiLain) {
            // Perbaikan Logika: Jika semua entitas user bernilai null, langsung batasi data
            if (myPegawai == null && myDosen == null && myGuru == null && myMhs == null && mySiswa == null) {
                crit.add(Restrictions.sqlRestriction("1=0"));
            } else {
                // Merangkai kondisi OR tanpa menggunakan .conditions()
                Disjunction meOr = Restrictions.disjunction();
                if (myPegawai != null) meOr.add(Restrictions.eq("pegawai", myPegawai));
                if (myDosen != null) meOr.add(Restrictions.eq("dosen", myDosen));
                if (myGuru != null) meOr.add(Restrictions.eq("guru", myGuru));
                if (myMhs != null) meOr.add(Restrictions.eq("mahasiswa", myMhs));
                if (mySiswa != null) meOr.add(Restrictions.eq("siswa", mySiswa));
                
                crit.add(meOr);
            }
        } else {
            // Boleh melihat data orang lain, lakukan pemeriksaan tingkat Satuan Kerja
            if (!bolehLihatSatuanKerja) {
                if (mySatker != null) {
                    crit.add(Restrictions.or(Restrictions.eq("satuanKerja", mySatker), Restrictions.isNull("satuanKerja")));
                } else {
                    crit.add(Restrictions.isNull("satuanKerja"));
                }
            }
        }

        // --- BLOK 2: FILTER FORM (Tanggal, Satker, Status, Entitas) ---
        if (startDateStr != null && endDateStr != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            crit.add(Restrictions.between("tanggal", sdf.parse(startDateStr), sdf.parse(endDateStr)));
        }
        if (satuanKerjaId != null && !satuanKerjaId.isEmpty()) {
            crit.add(Restrictions.eq("satuanKerja.id", Long.parseLong(satuanKerjaId)));
        }
        if (statusAbsensiId != null && !statusAbsensiId.isEmpty()) {
            crit.add(Restrictions.eq("statusabsensi.id", Long.parseLong(statusAbsensiId)));
        }
        if (jenisEntitas != null && !jenisEntitas.equals("ALL")) {
            if (jenisEntitas.equals("PEGAWAI")) crit.add(Restrictions.isNotNull("pegawai"));
            else if (jenisEntitas.equals("DOSEN")) crit.add(Restrictions.isNotNull("dosen"));
            else if (jenisEntitas.equals("GURU")) crit.add(Restrictions.isNotNull("guru"));
            else if (jenisEntitas.equals("MAHASISWA")) crit.add(Restrictions.isNotNull("mahasiswa"));
            else if (jenisEntitas.equals("SISWA")) crit.add(Restrictions.isNotNull("siswa"));
        }

        // --- BLOK 3: FILTER PENCARIAN DIPERLUAS ---
        if (q != null && !q.trim().isEmpty()) {
            crit.createAlias("pegawai", "p", Criteria.LEFT_JOIN);
            crit.createAlias("dosen", "d", Criteria.LEFT_JOIN);
            crit.createAlias("guru", "g", Criteria.LEFT_JOIN);
            crit.createAlias("mahasiswa", "m", Criteria.LEFT_JOIN);
            crit.createAlias("siswa", "s", Criteria.LEFT_JOIN);
            
            Disjunction or = Restrictions.disjunction();
            
            or.add(Restrictions.ilike("p.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("p.kode", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("p.mycode", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("d.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("d.nidn", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("g.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("g.nuptk", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("m.nama", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("m.nim", q, MatchMode.ANYWHERE));
            
            or.add(Restrictions.ilike("s.namaSiswa", q, MatchMode.ANYWHERE));
            or.add(Restrictions.ilike("s.nomorInduk", q, MatchMode.ANYWHERE));
            
            crit.add(or);
        }

        // ... [Lanjutkan dengan pengurutan Data (Order) dan pengambilan data list()] ...

        crit.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id")).setMaxResults(500);

        List<StatuskehadiranKaryawanHarian> list = crit.list();
        JSONArray dataArr = new JSONArray();
        
        // --- Variabel Hitungan Statistik ---
        int sTepat = 0, sTelat = 0, sCepat = 0, sLain = 0;
        int jPeg = 0, jDos = 0, jGur = 0, jMhs = 0, jSis = 0;
        
        java.util.function.Function<Double, String> formatDurasi = (Double hours) -> {
            if (hours == null || hours <= 0) return "";
            int totalMin = (int) (hours * 60);
            int h = totalMin / 60;
            int m = totalMin % 60;
            String res = "";
            if (h > 0) res += h + " " + Common.getBahasaConfig("jam") + " ";
            if (m > 0) res += m + " " + Common.getBahasaConfig("menit");
            return res.trim();
        };

        for (StatuskehadiranKaryawanHarian sk : list) {
        	// --- BLOK PENYARINGAN HARI AKTIF (SUDAH DIPERBAIKI) ---
            if (sk.getTanggal() != null && !activeDays.isEmpty()) {
                Calendar c = Calendar.getInstance();
                c.setTime(sk.getTanggal());
                
                // Karena Common.haris (Minggu=1 ... Sabtu=7) identik dengan Java Calendar,
                // kita bisa langsung mengambil nilainya tanpa dikurangi.
                int mapHari = c.get(Calendar.DAY_OF_WEEK);
                
                // Jika hari aktual tidak ada di dalam daftar checkbox yang dicentang, LEWATI
                if (!activeDays.contains(mapHari)) {
                    continue; 
                }
            }
            // --- AKHIR BLOK PENYARINGAN HARI AKTIF ---
            JSONObject obj = new JSONObject();
            String nama = "-";
            
            // --- Mengumpulkan Data Entitas & Hitungan Entitas ---
            if (sk.getSiswa() != null) { nama = sk.getSiswa().getNamaSiswa(); jSis++; }
            else if (sk.getMahasiswa() != null) { nama = sk.getMahasiswa().getNama(); jMhs++; }
            else if (sk.getGuru() != null) { nama = sk.getGuru().getNama(); jGur++; }
            else if (sk.getDosen() != null) { nama = sk.getDosen().getNama(); jDos++; }
            else if (sk.getPegawai() != null) { nama = sk.getPegawai().getNama(); jPeg++; }

            CutiDanIzin cuti = sk.getCutiDanIzin();
            boolean isDisetujui = cuti != null && Boolean.TRUE.equals(cuti.getSetujui());

            // --- Logika Hitungan Status Absen Aktual ---
            boolean isTerlambat = Boolean.TRUE.equals(sk.getDatangTerlambat());
            boolean isCepatPulang = Boolean.TRUE.equals(sk.getPulangCepat());

            if (isTerlambat) sTelat++;
            else if (isCepatPulang) sCepat++;
            else if (sk.getMasukjam() != null) sTepat++;
            else sLain++;

            JSONObject datang = new JSONObject();
            datang.put("jam", sk.getMasukjam() != null ? Common.timeFormat.get().format(sk.getMasukjam()) : "-");
            if (isTerlambat && !isDisetujui) {
                datang.put("status", Common.getBahasaConfig("Terlambat") + " " + formatDurasi.apply(sk.getJumlahTerlambat()));
                datang.put("css", "text-danger");
            } else if (Boolean.TRUE.equals(sk.getDatangCepat())) {
                datang.put("status", Common.getBahasaConfig("Lebih Cepat") + " " + formatDurasi.apply(sk.getJumlahMasukSebelumWaktunya()));
                datang.put("css", "text-success");
            } else {
                datang.put("status", Common.getBahasaConfig("Tepat Waktu"));
                datang.put("css", "text-success");
            }
            datang.put("foto", toRelativeFotoUrl(sk.getFotoAbsenDatang()));
            datang.put("lokasi", sk.getLokasiAbsenDatang());

            JSONObject pulang = new JSONObject();
            pulang.put("jam", sk.getPulangJam() != null ? Common.timeFormat.get().format(sk.getPulangJam()) : "-");
            if (sk.getPulangJam() != null) {
                if (Boolean.TRUE.equals(sk.getPulangTerlambat())) {
                    pulang.put("status", Common.getBahasaConfig("Terlambat") + " " + formatDurasi.apply(sk.getJumlahPulangSetelahWaktunya()));
                    pulang.put("css", "text-info");
                } else if (isCepatPulang && !isDisetujui) {
                    pulang.put("status", Common.getBahasaConfig("Lebih Cepat") + " " + formatDurasi.apply(sk.getJumlahCepatKeluar()));
                    pulang.put("css", "text-danger");
                } else {
                    pulang.put("status", Common.getBahasaConfig("Tepat Waktu"));
                    pulang.put("css", "text-info");
                }
            }
            pulang.put("foto", toRelativeFotoUrl(sk.getFotoAbsenPulang()));
            pulang.put("lokasi", sk.getLokasiAbsenPulang());

            obj.put("nama", nama);
            obj.put("tanggal", sk.getTanggal() != null ? Common.dateFormat6.get().format(sk.getTanggal()) : "-");
            obj.put("datang", datang);
            obj.put("pulang", pulang);
            obj.put("statusAbsen", sk.getStatusabsensi() != null ? sk.getStatusabsensi().getNama() : "");
            obj.put("keterangan", sk.getKeterangan() != null ? sk.getKeterangan() : "");
            dataArr.put(obj);
        }

        JSONObject result = new JSONObject();
        result.put("status", "00");
        result.put("data", dataArr);
        
        // --- Memasukkan Objek Statistik ke JSON ---
        JSONObject statObj = new JSONObject();
        statObj.put("tepatWaktu", sTepat);
        statObj.put("terlambat", sTelat);
        statObj.put("cepatPulang", sCepat);
        statObj.put("tidakAbsen", sLain);
        statObj.put("jmlPegawai", jPeg);
        statObj.put("jmlDosen", jDos);
        statObj.put("jmlGuru", jGur);
        statObj.put("jmlMahasiswa", jMhs);
        statObj.put("jmlSiswa", jSis);
        result.put("statistik", statObj);

        out.print(result.toString());
    } catch (Exception e) { 
        out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}"); 
    } finally { 
        if (sess != null && sess.isOpen()) { try { sess.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/presensi/_service_presensi_harian.jsp:259");} }
        HibernateUtil.closeSessionQuietly(sess);
    }
%>