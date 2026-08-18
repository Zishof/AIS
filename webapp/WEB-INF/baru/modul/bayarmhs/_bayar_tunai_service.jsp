<%-- BUILD 2026-06-29: dipaksa rekompilasi untuk cegah IncompatibleClassChangeError dari method PembayaranUtil yang berubah static->instance pada JSP ter-compile lama. WAJIB bersihkan work dir Tomcat saat deploy lalu restart. --%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.DetailBiaya"%>
<%@page import="ais.database.model.ItemBiaya"%>
<%@page import="ais.database.model.DetailKegiatan"%>
<%@page import="ais.database.model.PengaturanPembayaranBulanan"%>
<%@page import="ais.database.model.JadwalPembayaran"%>
<%@page import="ais.database.model.CicilanPembayaran"%>
<%@page import="ais.database.model.JenisPembayaran"%>
<%@page import="ais.database.model.LogPembayaran"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.VOMahasiswa"%>
<%@page import="ais.action.ws.util.PembayaranUtil"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.ArrayList"%>

<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        return;
    }

    String rnd = request.getParameter("rnd") != null ? request.getParameter("rnd") : Common.getGeneratedBarCode(6);
    if (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null) {
        out.print("<div class='alert alert-danger shadow-sm rounded-4 m-4 text-center'><i class='fas fa-shield-alt fa-2x mb-3 text-danger'></i><br><b>" + Common.getBahasaConfig("Akses ditolak.") + "</b><br>" + Common.getBahasaConfig("Layanan ini khusus bagi Administrator atau Petugas Kasir.") + "</div>");
        return;
    }

    String idStr = request.getParameter("id");
    String jkIdStr = request.getParameter("jkId");
    String isMahasiswaStr = request.getParameter("isMahasiswa");
    String smtStr = request.getParameter("smt");
    String payload = request.getParameter("payload");

    // TANGKAPAN DATA TAMBAHAN DARI FORM MANUAL
    String caraBayarParam = request.getParameter("caraBayar");
    String tglBayarParam = request.getParameter("tglBayar");
    String ketParam = request.getParameter("keterangan");
    String tglKwitansiParam = request.getParameter("tglKwitansi");
    
    if (payload == null || payload.trim().isEmpty() || idStr == null || jkIdStr == null) {
        out.print("<div class='alert alert-danger shadow-sm rounded-4 m-4 text-center'><i class='fas fa-exclamation-triangle fa-2x mb-3 text-danger'></i><br><b>" + Common.getBahasaConfig("Galat Parameter") + "</b><br>" + Common.getBahasaConfig("Parameter pembayaran tunai tidak sah atau tidak lengkap.") + "</div>");
        return;
    }

    Session sess = null;
    Transaction tx = null;
    Kegiatan kegiatanAktif = null;
    double totalBayar = 0.0;
    
    try {
        sess = HibernateUtil.openSession();
        tx = sess.beginTransaction();

        long id = Long.parseLong(idStr);
        long jkId = Long.parseLong(jkIdStr);
        boolean isMahasiswa = "true".equalsIgnoreCase(isMahasiswaStr);
        int smtInt = (smtStr != null && !smtStr.isEmpty()) ? Integer.parseInt(smtStr) : 1;
        
        VOMahasiswa person = (VOMahasiswa) (isMahasiswa 
                        ? ConstantValues.ambil(Mahasiswa.class.getName(), id, true) 
                        : ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), id, true));
                        
        if (person == null) {
            if (tx != null && tx.isActive()) tx.rollback();
            out.print("<div class='alert alert-danger shadow-sm rounded-4 m-4 text-center'><i class='fas fa-user-times fa-2x mb-3 text-danger'></i><br><b>" + Common.getBahasaConfig("Data Tidak Ditemukan") + "</b><br>" + Common.getBahasaConfig("Data Pelanggan tidak ditemukan di pangkalan data.") + "</div>");
            return;
        }
        
        Mahasiswa mhs = null;
        BiodataCalonMahasiswa calon = null;
        if (isMahasiswa) mhs = (Mahasiswa) person;
        else calon = (BiodataCalonMahasiswa) person;
        
        JenisKegiatan jk = (JenisKegiatan) sess.get(JenisKegiatan.class, jkId);
        
        // PROSES PARAMETER TANGGAL DAN KETERANGAN DARI FORM
        Date tglBayar = WaktuUtil.getDate();
        if(tglBayarParam != null && !tglBayarParam.trim().isEmpty()) {
            try { tglBayar = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tglBayarParam); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:92");}
        }
        
        Date tglKwitansi = WaktuUtil.getDate();
        if(tglKwitansiParam != null && !tglKwitansiParam.trim().isEmpty()) {
            try { tglKwitansi = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tglKwitansiParam); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:97");}
        }
        
        String ketManual = (ketParam != null && !ketParam.trim().isEmpty()) ? ketParam.trim() : "Transaksi Manual Kasir";

        // ====================================================================
        // PENCEGAHAN DUPLIKASI KODE UNIK (CONSTRAINT VIOLATION 25P02)
        // ====================================================================

        kegiatanAktif = person.ambilKegiatansRefresh(smtInt, jk, true);
        if (kegiatanAktif != null && kegiatanAktif.getId() != null)
        	kegiatanAktif = (Kegiatan) sess.load(Kegiatan.class, kegiatanAktif.getId());
		else
			kegiatanAktif = new Kegiatan();

        kegiatanAktif.setTanggal(tglBayar);
        kegiatanAktif.setValidated(1);
        kegiatanAktif.setValidator(tbmuser.getUserNama());
        kegiatanAktif.setKeterangan(ketManual);
        kegiatanAktif.setMahasiswa(mhs);
        kegiatanAktif.setCalonMahasiswa(calon);
        kegiatanAktif.setJenisKegiatan(jk);
        kegiatanAktif.setSemster(smtInt);

        sess.saveOrUpdate(kegiatanAktif);
        
        // ====================================================================
        // LOGIKA PENENTUAN JADWAL UNTUK KALKULASI DENDA
        // ====================================================================
        Collection<DetailKegiatan> detailKegiatans = kegiatanAktif.ambilDetailKegiatan(false);
        
        Integer tahunAngkatanMhs = 0;
        String semesterMulaiVal = "";
        ais.database.model.Jenjang jenjang = null;
        ais.database.model.JenisSeleksi jenisSeleksi = null;
        String jenisKuliah = "";
        String nimAtauNoReg = "";
        ais.database.model.GelombangPendaftaran gelombangPendaftaran = null;

        if (isMahasiswa) {
            tahunAngkatanMhs = mhs.getTahunangkatan() != null ? mhs.getTahunangkatan() : WaktuUtil.getCalendar().get(java.util.Calendar.YEAR);
            semesterMulaiVal = mhs.getSemesterMulai() != null ? mhs.getSemesterMulai() : "";
            jenjang = mhs.getJurusan() != null ? mhs.getJurusan().getJenjang() : null;
            jenisSeleksi = mhs.getJenisSeleksi();
            jenisKuliah = mhs.getProgram();
            nimAtauNoReg = mhs.getNim();
            try { gelombangPendaftaran = (ais.database.model.GelombangPendaftaran) mhs.getClass().getMethod("getGelombangPendaftaran").invoke(mhs); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:143");}
        } else {
            tahunAngkatanMhs = calon.getTahun() != null ? calon.getTahun() : WaktuUtil.getCalendar().get(java.util.Calendar.YEAR);
            semesterMulaiVal = calon.getSemesterMulai() != null ? calon.getSemesterMulai() : "";
            jenjang = calon.getJenjang();
            jenisSeleksi = calon.getJenisSeleksi();
            jenisKuliah = calon.getProgram();
            nimAtauNoReg = calon.getNoRegistrasi();
            gelombangPendaftaran = calon.getGelombangPendaftaran();
        }

        Integer tahunAkademikMulai = Common.getTahunAkademik(smtInt, tahunAngkatanMhs, 0, semesterMulaiVal);
        String tahunAkademikJadwal = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
        
        PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
        java.io.Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
                tglBayar, jk, jenjang, tahunAkademikJadwal,
                "Ganjil".equalsIgnoreCase(semesterMulaiVal) || ais.database.model.Perkuliahan.GANJIL.equalsIgnoreCase(semesterMulaiVal),
                jenisSeleksi, jenisKuliah, nimAtauNoReg, gelombangPendaftaran);
        
        JadwalPembayaran jadwalPembayaran = null;
        if (serializables != null && serializables.length > 0) { jadwalPembayaran = (JadwalPembayaran) serializables[0]; }
        if (jadwalPembayaran == null && kegiatanAktif.getJadwalPembayaran() != null) { jadwalPembayaran = kegiatanAktif.getJadwalPembayaran(); }
        
        JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null && jadwalPembayaran.getKhususUntukNim().contains("," + nimAtauNoReg + ",") ? jadwalPembayaran : null;

        // EVALUASI JENIS PEMBAYARAN YANG DIPILIH
        JenisPembayaran jpManual = ConstantValues.TUNAI;
        if(caraBayarParam != null && !caraBayarParam.trim().isEmpty()) {
            try { 
                JenisPembayaran tempJp = (JenisPembayaran) sess.get(JenisPembayaran.class, Long.parseLong(caraBayarParam));
                if (tempJp != null) jpManual = tempJp;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:175");}
        }

        // MENGURAI PAYLOAD DAN MENYIMPAN RINCIAN CICILAN BERIKUT DENDA
        String[] items = payload.split(",");
        int urutanCicilan = 1;

        System.out.println("payload -> "+payload);
        
        for (String item : items) {
            String[] parts = item.split("\\|");
            if (parts.length == 2) {
                try {
                    String idUnik = parts[0].trim();
                    double nominal = Double.parseDouble(parts[1].trim());

                    if (nominal > 0) {
                        DetailBiaya db = null;
                        PengaturanPembayaranBulanan pb = null;
                        DetailKegiatan dkSesuai = null;
                        Double dendaKalkulasi = 0.0;
                        
                        if (idUnik.startsWith("DB_")) {
                            long dbId = Long.parseLong(idUnik.replace("DB_", ""));
                            db = (DetailBiaya) sess.get(DetailBiaya.class, dbId);
                            if (db != null) {
                                if (detailKegiatans != null) {
                                    for(DetailKegiatan dk : detailKegiatans) {
                                        if(dk.getDetailBiaya() != null && dk.getDetailBiaya().getId().equals(db.getId()) && dk.getPengaturanPembayaranBulanan() == null) { dkSesuai = dk; break; }
                                    }
                                }
                                
                                Double jmlTagihan = Kegiatan.ambilJumlahTagihan(dkSesuai, kegiatanAktif, db, false);
                                if(jmlTagihan == null) jmlTagihan = 0.0;
                                
                                Double hasilDenda = dkSesuai != null && dkSesuai.getMenggunakanDendaCustom() ? jmlTagihan
                                        : dkSesuai != null && (dkSesuai.getBatalkanDenda() || jmlTagihan.intValue() == 0) ? jmlTagihan
                                        : db.checkDenda(jmlTagihan, tglBayar, jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan(), null);

                                if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom()) {
                                    db.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dkSesuai.getDendaCustom() != null ? dkSesuai.getDendaCustom() : 0.0) + ".");
                                }

                                Double nilaiDenda = hasilDenda - jmlTagihan;
                                if (dkSesuai != null && !dkSesuai.getMenggunakanDendaCustom()) {
                                    dkSesuai.setDendaCustom(nilaiDenda);
                                    dendaKalkulasi = nilaiDenda;
                                } else if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom()) {
                                    dendaKalkulasi = dkSesuai.getDendaCustom() != null ? dkSesuai.getDendaCustom() : 0.0;
                                }
                            }
                        } else if (idUnik.startsWith("PB_")) {
                            long pbId = Long.parseLong(idUnik.replace("PB_", ""));
                            pb = (PengaturanPembayaranBulanan) sess.get(PengaturanPembayaranBulanan.class, pbId);
                            if (pb != null) {
                                db = pb.getDetailBiaya();
                                if (detailKegiatans != null) {
                                    for(DetailKegiatan dk : detailKegiatans) {
                                        if(dk.getPengaturanPembayaranBulanan() != null && dk.getPengaturanPembayaranBulanan().getId().equals(pb.getId())) { dkSesuai = dk; break; }
                                    }
                                }
                                
                                Double jmlTagihan = Kegiatan.ambilJumlahTagihan(dkSesuai, db, kegiatanAktif, mhs, smtInt, pb);
                                if(jmlTagihan == null) jmlTagihan = 0.0;
                                
                                Double hasilDenda = dkSesuai != null && (dkSesuai.getBatalkanDenda() || jmlTagihan.intValue() == 0) ? jmlTagihan
                                        : dkSesuai != null && dkSesuai.getMenggunakanDendaCustom() ? jmlTagihan
                                        : pb.checkDenda(jmlTagihan, tglBayar, jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan());

                                if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom()) {
                                    pb.setInfoDenda(" Penambahan denda senilai " + Common.numberFormat.get().format(dkSesuai.getDendaCustom() != null ? dkSesuai.getDendaCustom() : 0.0) + ".");
                                }

                                Double nilaiDenda = hasilDenda - jmlTagihan;
                                if (dkSesuai != null && !dkSesuai.getMenggunakanDendaCustom()) {
                                    dkSesuai.setDendaCustom(nilaiDenda);
                                    dendaKalkulasi = nilaiDenda;
                                } else if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom()) {
                                    dendaKalkulasi = dkSesuai.getDendaCustom() != null ? dkSesuai.getDendaCustom() : 0.0;
                                }
                            }
                        }
                        System.out.println("db -> "+db);
                        if (db != null) {
                            ItemBiaya itemBiaya = db.getItemBiaya();
                            CicilanPembayaran cp = new CicilanPembayaran();
                            cp.setKegiatan(kegiatanAktif);
                            cp.setKe(urutanCicilan++);
                            cp.setDetailBiaya(db);
                            cp.setItemBiaya(itemBiaya);
                            cp.setPengaturanPembayaranBulanan(pb);
                            cp.setNilai(nominal);
                            cp.setDenda(dendaKalkulasi); // APPLY DENDA HASIL KALKULASI
                            
                            cp.setTanggal(tglBayar);
                            cp.setTanggalKwitansi(tglKwitansi);
                            cp.setKeterangan(ketManual);
                            cp.setJenisPembayaran(jpManual);
                            
                            cp.setValidator(tbmuser.getUserNama());
                            if (pb != null && isMahasiswa) {
                                Double nilaiAsli = pb.ambilNominalModifikasi(mhs, smtInt);
                                cp.setNilaiAsli(nilaiAsli);
                            }

                            sess.save(cp);
                            totalBayar += nominal;
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:284");}
            }
        }
        
        // MENGAKUMULASI TAGIHAN DAN MEREKAM LOG PEMBAYARAN
        if (totalBayar > 0) {
            LogPembayaran logPembayaran = new LogPembayaran();
            logPembayaran.setKegiatan(kegiatanAktif);
            logPembayaran.setNominal(totalBayar);
            logPembayaran.setKeterangan(ketManual);
            logPembayaran.setValidator(tbmuser.getUserNama());
            sess.saveOrUpdate(logPembayaran);
            
            // Rekalkulasi kegiatan via soft-delete memory
            Double[] d = kegiatanAktif.hitungTotalDanDendaFromCicilan();
            Double jumlah = d[0]; Double denda = d[1];
            kegiatanAktif.setDenda(denda);
            kegiatanAktif.setAmount(jumlah);
            sess.update(kegiatanAktif);
            
            PembayaranUtil.getInstance().updateTunggakan(kegiatanAktif, sess);
        }

        tx.commit();

    } catch (Exception e) {
        if (tx != null && tx.isActive()) {
            try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:311");}
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/bayarmhs/_bayar_tunai_service.jsp:313");
        out.print("<div class='alert alert-danger shadow-sm rounded-4 m-4 text-center'><i class='fas fa-exclamation-circle fa-2x mb-3 text-danger'></i><br><b>" + Common.getBahasaConfig("Galat Sistem") + "</b><br>" + Common.getBahasaConfig("Sistem gagal memproses transaksi pembayaran tunai: ") + e.getMessage() + "</div>");
        return;
    } finally {
        if (sess != null) { sess.disconnect(); sess.close(); }
    }
%>

<style>
    .btn-aksi-<%=rnd%> { transition: transform 0.2s ease, box-shadow 0.2s ease; letter-spacing: 0.5px; }
    .btn-aksi-<%=rnd%>:hover { transform: translateY(-2px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,.15) !important; }
</style>

<div class="animate__animated animate__fadeIn p-4 text-center">
    <div class="mb-4">
        <i class="fas fa-check-circle text-success" style="font-size: 5rem;"></i>
    </div>
    <h4 class="fw-bold text-dark tracking-wide mb-3"><%= Common.getBahasaConfig("Validasi Transaksi Berhasil") %></h4>
    <p class="text-secondary mb-4">
        <%= Common.getBahasaConfig("Pembayaran tunai sebesar") %> <b class="text-dark fs-5">Rp <%= Common.numberFormat.get().format(totalBayar) %></b> <%= Common.getBahasaConfig("telah terekam sebagai LUNAS ke dalam sistem.") %>
    </p>
    
    <div class="d-flex flex-column flex-sm-row justify-content-center gap-3 mt-3 border-top pt-4">
        <button type="button" class="btn btn-primary px-5 py-2 rounded-pill fw-bold shadow-sm btn-aksi-<%=rnd%>" data-bs-dismiss="modal" onclick="window.tutupDanSegarkan<%=rnd%>()">
            <i class="fas fa-arrow-left me-2"></i><%= Common.getBahasaConfig("Kembali (Selesai)") %>
        </button>
    </div>
</div>

<script>
    var hasRefreshed<%=rnd%> = false;
    
    // Fungsi ini dipanggil untuk memaksa reload halaman beserta ID_KEGIATAN
    window.tutupDanSegarkan<%=rnd%> = function() {
        if (hasRefreshed<%=rnd%>) return;
        hasRefreshed<%=rnd%> = true;
        
        if(typeof tampilkanToast === 'function') {
            tampilkanToast('<%=Common.getBahasaConfigJS("Menyegarkan riwayat tagihan...")%>', 'bg-info text-white');
        }
        
        // Memaksa reload penuh (full-page reload) dengan query string id={ID_KEGIATAN}
        let currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('id', '<%= kegiatanAktif != null ? kegiatanAktif.getId() : idStr %>');
        currentUrl.searchParams.set('refresh', 'true');
        window.location.href = currentUrl.toString();
    };

    (function() {
        const modalEl = document.getElementById('modalProsesTunai<%=rnd%>');
        
        if (modalEl) {
            // Tangkap aksi ketika pengguna menutup popup dengan klik area luar (overlay) atau menekan ESC
            modalEl.addEventListener('hidden.bs.modal', function() {
                window.tutupDanSegarkan<%=rnd%>();
            });
        }
        
        // Tangkap juga aksi apabila ada tombol "X" close di area header modal
        const closeBtns = document.querySelectorAll('#modalProsesTunai<%=rnd%> .btn-close');
        closeBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                window.tutupDanSegarkan<%=rnd%>();
            });
        });
    })();
</script>