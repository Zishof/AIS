<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.Pedagang"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.common.Common"%>
<%
// ==========================================
// 0. BLOK AJAX UNTUK MANUAL KIRIM NOTIFIKASI & LIHAT PASSWORD
// ==========================================

// --- AJAX TAMPILKAN PASSWORD ---
if ("true".equals(request.getParameter("is_ajax_get_password"))) {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    JSONObject jsonResponse = new JSONObject();
    org.hibernate.Session dbSession = null;
    try {
        String idAnggotaStr = request.getParameter("id_anggota");
        if (idAnggotaStr != null && !idAnggotaStr.trim().isEmpty()) {
            dbSession = HibernateUtil.openSession();
            AnggotaKoperasi anggota = (AnggotaKoperasi) dbSession.get(AnggotaKoperasi.class, Long.parseLong(idAnggotaStr));
            if (anggota != null && anggota.getUserid() != null) {
                Tbmuser usr = (Tbmuser) dbSession.get(Tbmuser.class, anggota.getUserid());
                if (usr != null) {
                    jsonResponse.put("status", "success");
                    jsonResponse.put("userid", usr.getUserId());
                    jsonResponse.put("password", Common.desEncrypter.get().decrypt(usr.getUserPassword()));
                } else {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "Kredensial login pengguna tidak ditemukan pada sistem.");
                }
            } else {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Anggota tidak valid atau tidak memiliki User ID.");
            }
        } else {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "ID Anggota tidak boleh kosong.");
        }
    } catch (Exception e) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", e.getMessage());
    } finally {
        if (dbSession != null) {
            try { dbSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/anggota_koperasi.jsp:51");}
            try { dbSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/anggota_koperasi.jsp:52");}
        }
        HibernateUtil.closeSessionQuietly(dbSession);
    }
    out.print(jsonResponse.toString());
    return; // Hentikan render HTML karena ini adalah request AJAX
}

// --- AJAX KIRIM NOTIFIKASI MASSAL ---
if ("true".equals(request.getParameter("is_ajax_kirim_notif"))) {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    JSONObject jsonResponse = new JSONObject();
    try {
        boolean paksaKirim = "true".equals(request.getParameter("paksa"));
        List<String> warnings = new ArrayList<String>();
        
        // Memanggil method yang telah di-update dengan parameter boolean 'abaikanHariMinggu'
        ais.action.master.helper.util.EvaluasiBelanjaProcessor.prosesEvaluasiDanKembalikanNotifikasi(warnings, paksaKirim);
        
        // Cetak warnings ke console
        for (String w : warnings) {
            System.out.println(w);
        }
        
        JSONArray warningsArray = new JSONArray(warnings);
        jsonResponse.put("status", "success");
        jsonResponse.put("data", warningsArray);
    } catch (Exception e) {
        jsonResponse.put("status", "error");
        jsonResponse.put("message", e.getMessage());
    }
    out.print(jsonResponse.toString());
    return; 
}

// --- AJAX KIRIM NOTIFIKASI SATUAN ---
if ("true".equals(request.getParameter("is_ajax_kirim_notif_satuan"))) {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    JSONObject jsonResponse = new JSONObject();
    org.hibernate.Session dbSession = null;
    org.hibernate.Transaction tx = null;
    try {
        String idAnggotaStr = request.getParameter("id_anggota");
        List<String> warnings = new ArrayList<String>();

        if (idAnggotaStr != null && !idAnggotaStr.trim().isEmpty()) {
            dbSession = HibernateUtil.openSession();
            tx = dbSession.beginTransaction();
            
            AnggotaKoperasi anggota = (AnggotaKoperasi) dbSession.get(AnggotaKoperasi.class, Long.parseLong(idAnggotaStr));

            if (anggota != null && anggota.getJenisAnggotaKoperasi() != null) {
                int targetBelanja = anggota.getJenisAnggotaKoperasi().getTargetFrekuensiBelanja() != null ? anggota.getJenisAnggotaKoperasi().getTargetFrekuensiBelanja() : 0;
                int maxPelanggaran = anggota.getJenisAnggotaKoperasi().getMaksimalPelanggaran() != null ? anggota.getJenisAnggotaKoperasi().getMaksimalPelanggaran() : 0;

                ais.action.master.helper.util.EvaluasiBelanjaProcessor.cekDanKirimNotifikasiPelanggaran(dbSession, anggota, targetBelanja, maxPelanggaran, warnings);
                
                tx.commit();
            } else {
                warnings.add("ERROR: Anggota tidak ditemukan atau tidak memiliki Jenis Keanggotaan yang valid.");
                if(tx != null) tx.rollback();
            }
        } else {
            warnings.add("ERROR: Parameter ID Anggota kosong.");
        }

        JSONArray warningsArray = new JSONArray(warnings);
        jsonResponse.put("status", "success");
        jsonResponse.put("data", warningsArray);
    } catch (Exception e) {
        if (tx != null) {
            try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/anggota_koperasi.jsp:125");}
        }
        jsonResponse.put("status", "error");
        jsonResponse.put("message", e.getMessage());
    } finally {
        if (dbSession != null) {
            try { dbSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/anggota_koperasi.jsp:131");}
            try { dbSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/anggota_koperasi.jsp:132");}
        }
        HibernateUtil.closeSessionQuietly(dbSession);
    }
    out.print(jsonResponse.toString());
    return; 
}

// ==========================================
// 1. Pengecekan Sesi & Keamanan Akses
// ==========================================
Tbmuser tbmuser = null;
Toko toko = null;
boolean isAdmin = false;
boolean dataMemberHarusReferneceKedataMaster = false;
String idTokoAktif = "";

try {
    tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"status\":\"error\", \"message\":\"" + Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali ke dalam sistem.") + "\"}");
        return;
    }
    
    Pedagang pedagang = tbmuser.getPedagang();
    toko = pedagang == null ? null : pedagang.getToko();
    isAdmin = (toko == null);
    idTokoAktif = toko != null ? String.valueOf(toko.getId()) : "";
    
    dataMemberHarusReferneceKedataMaster = Common.getKonfigurasi("data_member_harus_refernece_ke_data_master", Konfigurasi.AKTIF).getNilai().equalsIgnoreCase(Konfigurasi.AKTIF);

} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/anggota_koperasi.jsp:165");
}
// Catatan: blok ini memakai session thread-local (Common.getCurrentUser + lazy load
// getPedagang/getToko) yang ditutup oleh FilterJSP per-request — tidak ada dbSession lokal
// yang dibuka di sini, sehingga tidak perlu (dan tidak bisa) ditutup manual.

String rnd = Common.getGeneratedBarCode(7);
String urlQr = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=kantin%2Fmember&s=qr_login";
%>

<div class="container-fluid py-3 py-md-4" style="background-color: #f4f7f6; min-height: 100vh;">

    <div id="viewList<%=rnd%>" class="animate__animated animate__fadeIn">
        
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 gap-3">
            <div class="text-center text-md-start">
                <h4 class="fw-bolder text-primary mb-1">
                    <i class="fas fa-users me-2"></i><%=Common.getBahasaConfig("Manajemen Anggota Koperasi")%>
                </h4>
                <p class="text-muted small mb-0"><%=Common.getBahasaConfig("Kelola data pelanggan, informasi keanggotaan, dan kredensial sistem sivitas akademika.")%></p>
            </div>
            
            <div class="d-flex flex-wrap gap-2 justify-content-center">
                <button class="btn btn-light rounded-pill px-4 shadow-sm fw-bold border text-primary" onclick="resetAndLoadData<%=rnd%>()" title="<%=Common.getBahasaConfig("Perbarui Data")%>">
                    <i class="fas fa-sync-alt me-1"></i><%=Common.getBahasaConfig("Muat Ulang")%>
                </button>
                
                <% if (isAdmin) { %>
                <button class="btn btn-warning rounded-pill px-4 shadow-sm fw-bold text-dark text-nowrap" onclick="konfirmasiKirimNotifMassal<%=rnd%>()" id="btnKirimNotif<%=rnd%>" title="<%=Common.getBahasaConfig("Evaluasi seluruh member")%>">
                    <i class="fas fa-paper-plane me-1"></i><%=Common.getBahasaConfig("Kirim Notifikasi (Massal)")%>
                </button>
                <button class="btn btn-primary rounded-pill px-4 shadow-sm fw-bold text-nowrap" onclick="bukaFormTambah<%=rnd%>()">
                    <i class="fas fa-user-plus me-1"></i><%=Common.getBahasaConfig("Tambah Anggota")%>
                </button>
                <% } %>
            </div>
        </div>

        <%-- Fitur "Dasbor Statistik Anggota" -- kartu ringkasan + rincian batang per jenis keanggotaan,
             dihitung SEKALI saat halaman dibuka, TIDAK ikut berubah mengikuti filter Jenis/Tipe/Pencarian
             di bawah. Query SAMA PERSIS dgn KantinHelper.anggotaStatistik (aksi server dipakai Desktop/
             Android, TIDAK toko-scoped -- lihat JavaDoc method itu) supaya taat asas lintas platform,
             dijalankan via fetchData<%=rnd%> (endpoint SQL generik yg sudah dipakai halaman ini). --%>
        <div class="row g-2 mb-4" id="statistikAnggotaKpi<%=rnd%>">
            <div class="col-6 col-md-3"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Total Anggota</div><div class="fs-5 fw-bold" id="saTotal<%=rnd%>">0</div></div></div>
            <div class="col-6 col-md-3"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Aktif</div><div class="fs-5 fw-bold text-success" id="saAktif<%=rnd%>">0</div></div></div>
            <div class="col-6 col-md-3"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Non-Aktif</div><div class="fs-5 fw-bold text-secondary" id="saNonaktif<%=rnd%>">0</div></div></div>
            <div class="col-6 col-md-3"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small text-muted fw-bold text-uppercase" style="font-size:10px;">Wajib PIN</div><div class="fs-5 fw-bold text-warning" id="saWajibPin<%=rnd%>">0</div></div></div>
        </div>
        <div class="row g-3 mb-4">
            <div class="col-12"><div class="p-3 rounded-3 border bg-white shadow-sm h-100"><div class="small fw-bold mb-2">Per Jenis Keanggotaan</div><div id="saBarJenis<%=rnd%>"></div></div></div>
        </div>

        <div class="card border-0 shadow-sm rounded-4 mb-4 border-top border-primary border-3">
            <div class="card-body p-3 p-md-4">
                <div class="row g-3 align-items-end">

                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-tags me-1 text-primary"></i><%=Common.getBahasaConfig("Jenis Anggota")%></label>
                        <select class="form-select border-0 shadow-sm bg-light fw-medium filter-anggota-<%=rnd%>" id="filterJenisAnggota<%=rnd%>">
                            <option value=""><%=Common.getBahasaConfig("- Semua Jenis -")%></option>
                        </select>
                    </div>

                    <div class="col-md-3">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-layer-group me-1 text-primary"></i><%=Common.getBahasaConfig("Tipe Anggota")%></label>
                        <select class="form-select border-0 shadow-sm bg-light fw-medium filter-anggota-<%=rnd%>" id="filterTipeAnggota<%=rnd%>">
                            <option value=""><%=Common.getBahasaConfig("- Semua Tipe -")%></option>
                        </select>
                    </div>

                    <div class="col-md-4">
                        <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-search me-1 text-primary"></i><%=Common.getBahasaConfig("Pencarian Spesifik")%></label>
                        <div class="input-group shadow-sm">
                            <span class="input-group-text bg-light border-end-0 text-muted"><i class="fas fa-keyboard"></i></span>
                            <input type="text" class="form-control border-start-0 bg-light fw-medium filter-anggota-<%=rnd%>" id="searchAnggota<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Pencarian Nama, Kode, atau Telepon...")%>">
                        </div>
                    </div>

                    <div class="col-md-2 d-grid">
                        <button class="btn btn-primary rounded-3 shadow-sm fw-bold" onclick="resetAndLoadData<%=rnd%>()">
                            <i class="fas fa-filter me-2"></i><%=Common.getBahasaConfig("Saring")%>
                        </button>
                    </div>

                </div>
            </div>
        </div>

        <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
            <div class="card-body p-0 pt-1">
                <div class="table-responsive px-3 py-2" style="min-height: 400px;">
                    <table class="table table-hover table-striped align-middle mb-0">
                        <thead class="table-light text-secondary">
                            <tr>
                                <th class="fw-bold text-uppercase small py-3 text-center" style="width: 50px;">#</th>
                                <th class="fw-bold text-uppercase small text-center" style="width: 60px;"><i class="fas fa-image me-1"></i><%=Common.getBahasaConfig("Foto")%></th>
                                <th class="text-start fw-bold text-uppercase small"><i class="fas fa-id-card me-1"></i><%=Common.getBahasaConfig("Kode Member")%></th>
                                <th class="text-start fw-bold text-uppercase small"><i class="fas fa-font me-1"></i><%=Common.getBahasaConfig("Nama Anggota")%></th>
                                <th class="text-start fw-bold text-uppercase small"><i class="fas fa-user-shield me-1"></i><%=Common.getBahasaConfig("User ID")%></th>
                                <th class="text-center fw-bold text-uppercase small"><i class="fas fa-tag me-1"></i><%=Common.getBahasaConfig("Klasifikasi")%></th>
                                <th class="text-start fw-bold text-uppercase small"><i class="fas fa-address-book me-1"></i><%=Common.getBahasaConfig("Kontak")%></th>
                                <th class="text-center fw-bold text-uppercase small" style="width: 100px;"><i class="fas fa-toggle-on me-1"></i><%=Common.getBahasaConfig("Status")%></th>
                                <th class="text-center fw-bold text-uppercase small" style="width: 170px;"><i class="fas fa-cogs"></i></th>
                            </tr>
                        </thead>
                        <tbody id="tabelDataAnggota<%=rnd%>">
                            <tr>
                                <td colspan="9" class="text-center py-5">
                                    <div class="spinner-border text-primary mb-3"></div>
                                    <br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Sistem sedang memuat data anggota...")%></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="bg-light p-3 border-top d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                    <div class="small fw-bold text-muted" id="pagingInfoAnggota<%=rnd%>"></div>
                    <div class="d-flex align-items-center bg-white rounded-pill shadow-sm border p-1">
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnPrevPage<%=rnd%>" onclick="changePageAnggota<%=rnd%>(-1)">
                            <i class="fas fa-chevron-left me-1"></i><span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Sebelumnya")%></span>
                        </button>
                        <div class="px-3 fw-bold text-dark small">
                            <span id="pageNumberDisplay<%=rnd%>">1 / 1</span>
                        </div>
                        <button class="btn btn-sm btn-light rounded-pill px-3 fw-bold text-primary" id="btnNextPage<%=rnd%>" onclick="changePageAnggota<%=rnd%>(1)">
                            <span class="d-none d-sm-inline"><%=Common.getBahasaConfig("Selanjutnya")%></span><i class="fas fa-chevron-right ms-1"></i>
                        </button>
                    </div>
                </div>

            </div>
        </div>
    </div>

    <div id="viewForm<%=rnd%>" class="animate__animated animate__fadeIn" style="display: none;">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card border-0 shadow-lg rounded-4 border-top border-success border-4">
                    <div class="card-header bg-white border-bottom-0 pt-4 pb-0 px-4 d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold text-dark mb-0" id="formTitle<%=rnd%>">
                            <i class="fas fa-id-card text-success me-2"></i><%=Common.getBahasaConfig("Formulir Keanggotaan")%>
                        </h5>
                        
                        <div class="d-flex gap-3" id="headerSwitches<%=rnd%>">
                            <div class="form-check form-switch fs-5 mb-0">
                                <input class="form-check-input shadow-sm" type="checkbox" id="inputStatusAktif<%=rnd%>" checked style="cursor: pointer;">
                                <label class="form-check-label fs-6 fw-bold text-success" for="inputStatusAktif<%=rnd%>" id="labelStatusAktif<%=rnd%>"><%=Common.getBahasaConfig("Aktif")%></label>
                            </div>
                        </div>

                    </div>
                    
                    <div class="card-body p-4">
                        <form id="formAnggota<%=rnd%>" onsubmit="event.preventDefault(); simpanAnggota<%=rnd%>();">
                            <input type="hidden" id="inputIdAnggota<%=rnd%>" value="">
                            
                            <div class="row g-4 mt-1">
                                
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-tags me-1 text-primary"></i><%=Common.getBahasaConfig("Jenis Anggota Koperasi")%> <span class="text-danger">*</span></label>
                                    <select class="form-select shadow-sm fw-semibold" id="inputJenisAnggota<%=rnd%>" required>
                                        <option value=""><%=Common.getBahasaConfig("Memuat data jenis...")%></option>
                                    </select>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary mb-1"><i class="fas fa-layer-group me-1 text-primary"></i><%=Common.getBahasaConfig("Kategori Referensi Sivitas")%> <span class="text-danger">*</span></label>
                                    <select class="form-select shadow-sm fw-semibold" id="inputKategori<%=rnd%>" onchange="ubahModeInput<%=rnd%>()" required>
                                        <option value=""><%=Common.getBahasaConfig("Memuat data kategori...")%></option>
                                    </select>
                                </div>

                                <div class="col-md-12 p-3 bg-primary bg-opacity-10 rounded-3 border border-primary border-opacity-25" id="areaCariReferensi<%=rnd%>" style="display: none;">
                                    <label class="form-label small fw-bold text-primary mb-1" id="labelCariReferensi<%=rnd%>"><i class="fas fa-database me-1"></i><%=Common.getBahasaConfig("Cari Data Induk Sivitas...")%></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-search text-primary"></i></span>
                                        <input type="text" class="form-control border-start-0 border-end-0 fw-bold" list="listReferensi<%=rnd%>" id="inputCariReferensi<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik minimal 3 huruf/angka identitas...")%>" autocomplete="off">
                                        <button class="btn btn-primary fw-bold" type="button" onclick="loadDataReferensi<%=rnd%>()" title="<%=Common.getBahasaConfig("Muat Ulang Database Master")%>"><i class="fas fa-sync-alt"></i></button>
                                    </div>
                                    <datalist id="listReferensi<%=rnd%>"></datalist>
                                    <input type="hidden" id="idRefSelected<%=rnd%>" value="">
                                    <small class="text-muted fst-italic mt-2 d-block"><i class="fas fa-info-circle me-1"></i><%=Common.getBahasaConfig("Data Identitas, Nama, dan Telepon akan terisi secara otomatis jika data master ditemukan.")%></small>
                                </div>

                                <div class="col-md-6 mt-4">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-fingerprint me-1 text-muted"></i><%=Common.getBahasaConfig("Kode Member Koperasi")%> <span class="text-danger">*</span></label>
                                    <div class="input-group input-group-sm shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-qrcode text-primary"></i></span>
                                        <input type="text" class="form-control fw-bolder border-start-0 bg-light text-primary" id="inputKodeMember<%=rnd%>" readonly placeholder="<%=Common.getBahasaConfig("Dibuat otomatis oleh sistem...")%>" required>
                                    </div>
                                    <div class="form-check mt-2">
                                        <input class="form-check-input" type="checkbox" id="checkManualKode<%=rnd%>" onchange="toggleManualKode<%=rnd%>()">
                                        <label class="form-check-label small text-muted fw-medium" for="checkManualKode<%=rnd%>">
                                            <%=Common.getBahasaConfig("Edit Kode Member Secara Manual (Untuk Data Lampau)")%>
                                        </label>
                                    </div>
                                </div>
                                
                                <div class="col-md-6 mt-4">
                                    <label class="form-label small fw-bold text-secondary"><i class="far fa-id-card me-1 text-muted"></i><%=Common.getBahasaConfig("Nomor Identitas (KTP/NIM/NIS/NIK)")%></label>
                                    <div class="input-group input-group-sm shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-hashtag text-muted"></i></span>
                                        <input type="text" class="form-control fw-bold border-start-0" id="inputKodeIdentitas<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Nomor Identitas Pribadi")%>">
                                    </div>
                                </div>

                                <div class="col-md-12">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-font me-1 text-muted"></i><%=Common.getBahasaConfig("Nama Lengkap Sesuai Identitas")%> <span class="text-danger">*</span></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-user-edit text-muted"></i></span>
                                        <input type="text" class="form-control fw-bold border-start-0" id="inputNama<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Masukkan nama lengkap anggota")%>" required>
                                    </div>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-phone-alt me-1 text-muted"></i><%=Common.getBahasaConfig("Nomor Telepon Rumah/Kantor")%></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-phone text-muted"></i></span>
                                        <input type="tel" class="form-control border-start-0 fw-medium" id="inputTelepon<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 021-123456")%>">
                                    </div>
                                </div>
                                
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fab fa-whatsapp me-1 text-success"></i><%=Common.getBahasaConfig("Nomor HP / WhatsApp Aktif")%> <span class="text-danger" id="tandaWajibHp<%=rnd%>" style="display: none;">*</span></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-mobile-alt text-success"></i></span>
                                        <input type="tel" class="form-control border-start-0 fw-bolder" id="inputHp<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Contoh: 08123456789")%>">
                                    </div>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-envelope me-1 text-muted"></i><%=Common.getBahasaConfig("Alamat Surel (Email)")%> <span class="text-danger" id="tandaWajibEmail<%=rnd%>" style="display: none;">*</span></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-at text-muted"></i></span>
                                        <input type="email" class="form-control border-start-0 fw-medium" id="inputEmail<%=rnd%>" placeholder="<%=Common.getBahasaConfig("nama.pengguna@email.com")%>">
                                    </div>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="far fa-calendar-times me-1 text-danger"></i><%=Common.getBahasaConfig("Masa Berlaku (Kedaluwarsa)")%></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-calendar-day text-muted"></i></span>
                                        <input type="date" class="form-control border-start-0 fw-medium" id="inputTanggalKadaluarsa<%=rnd%>" onchange="evaluasiStatusAktif<%=rnd%>()">
                                    </div>
                                    <small class="text-muted d-block mt-1" style="font-size: 11px;"><i class="fas fa-info-circle me-1 text-info"></i><%=Common.getBahasaConfig("Kosongkan jika aktif selamanya. Jika sudah kedaluwarsa, akun ini otomatis dikunci.")%></small>
                                </div>

                                <div class="col-md-12 mt-4 border-top pt-3">
                                    <h6 class="fw-bold text-primary mb-3"><i class="fas fa-user-lock me-2"></i><%=Common.getBahasaConfig("Kredensial Akun (Akses Aplikasi)")%></h6>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-user-circle me-1 text-muted"></i><%=Common.getBahasaConfig("User ID (Nama Pengguna)")%></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-user text-muted"></i></span>
                                        <input type="text" class="form-control border-start-0 fw-bold" id="inputUserId<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Ketik User ID yang mudah diingat")%>" onblur="checkUserId<%=rnd%>()">
                                        <span class="input-group-text bg-white" id="statusUserId<%=rnd%>" style="display:none;"></span>
                                    </div>
                                    <small class="text-danger fw-bold mt-1 d-block" id="errUserId<%=rnd%>" style="display:none; font-size: 11px;"><i class="fas fa-exclamation-circle me-1"></i><%=Common.getBahasaConfig("Pembuatan gagal. User ID telah digunakan oleh pengguna lain!")%></small>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-key me-1 text-muted"></i><%=Common.getBahasaConfig("Kata Sandi (Password)")%></label>
                                    <div class="input-group shadow-sm">
                                        <span class="input-group-text bg-light border-end-0"><i class="fas fa-asterisk text-muted"></i></span>
                                        <input type="password" class="form-control border-start-0" id="inputPass<%=rnd%>" placeholder="<%=Common.getBahasaConfig("Isi untuk membuat atau mengubah sandi")%>">
                                    </div>
                                </div>

                                <div class="col-12 mt-3">
                                    <label class="form-label small fw-bold text-secondary"><i class="fas fa-map-marker-alt me-1 text-muted"></i><%=Common.getBahasaConfig("Alamat / Keterangan Lainnya")%></label>
                                    <textarea class="form-control text-muted shadow-sm" id="inputKeterangan<%=rnd%>" rows="3" placeholder="<%=Common.getBahasaConfig("Masukkan alamat domisili atau catatan khusus yang diperlukan.")%>"></textarea>
                                </div>

                                <div class="col-12 mt-5 text-end border-top pt-4">
                                    <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" onclick="tutupForm<%=rnd%>()">
                                        <i class="fas fa-times text-danger me-2"></i><%=Common.getBahasaConfig("Batal")%>
                                    </button>
                                    <button type="submit" class="btn btn-success px-5 rounded-pill shadow-sm fw-bold" id="btnSimpan<%=rnd%>">
                                        <i class="fas fa-save me-2"></i><%=Common.getBahasaConfig("Simpan Data Anggota")%>
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalLogNotif<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-light border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-clipboard-list text-warning me-2"></i><%=Common.getBahasaConfig("Log Evaluasi Belanja & Notifikasi")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2">
                <div id="printAreaLog<%=rnd%>" class="bg-dark text-success p-3 rounded-3" style="font-family: monospace; font-size: 12px; white-space: pre-wrap; height: 400px; overflow-y: auto;">
                </div>
            </div>
            <div class="modal-footer border-top-0 bg-light">
                <button type="button" class="btn btn-secondary px-4 rounded-pill fw-bold shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
                <button type="button" class="btn btn-info text-white px-4 rounded-pill fw-bold shadow-sm" onclick="cetakLogNotif<%=rnd%>()">
                    <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak Log")%>
                </button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="modalLihatPassword<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-sm modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow-lg">
            <div class="modal-header bg-info border-bottom-0 pb-3">
                <h5 class="modal-title fw-bold text-dark">
                    <i class="fas fa-key me-2"></i><%=Common.getBahasaConfig("Kredensial Akses")%>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 pt-2 text-center bg-light">
                <div class="mb-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("User ID")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-user text-muted"></i></span>
                        <input type="text" class="form-control border-start-0 bg-white fw-bold text-primary text-center" id="lblShowUserId<%=rnd%>" readonly>
                    </div>
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold text-secondary mb-1"><%=Common.getBahasaConfig("Password")%></label>
                    <div class="input-group shadow-sm">
                        <span class="input-group-text bg-white border-end-0"><i class="fas fa-lock text-muted"></i></span>
                        <input type="text" class="form-control border-start-0 bg-white fw-bold text-danger text-center" id="lblShowPassword<%=rnd%>" readonly>
                    </div>
                </div>
            </div>
            <div class="modal-footer border-top-0 bg-light justify-content-center">
                <button type="button" class="btn btn-secondary px-4 rounded-pill fw-bold shadow-sm w-100" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Tutup")%></button>
            </div>
        </div>
    </div>
</div>

<script>
    // ==========================================
    // BAGIAN 1: UTILITIES & STATE GLOBAL
    // ==========================================
    const masterModelClass<%=rnd%> = "ais.database.model.koperasi.AnggotaKoperasi";
    const wajibRefMaster<%=rnd%> = <%=dataMemberHarusReferneceKedataMaster%>;
    const isAdmin<%=rnd%> = <%=isAdmin%>;
    
    let arrDataReferensi<%=rnd%> = []; 
    let isEditMode<%=rnd%> = false; 
    let isUserIdValid<%=rnd%> = true; 

    let currentPage<%=rnd%> = 1;
    const limitPerPage<%=rnd%> = 10;
    let totalRecords<%=rnd%> = 0;
    
    let modalLogNotifInstance<%=rnd%> = null;
    let modalConfirmNotifInstance<%=rnd%> = null;
    let modalLihatPasswordInstance<%=rnd%> = null;

    const fetchData<%=rnd%> = async (sql) => {
        try {
            const res = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sql: sql, action: "sql" })
            });
            const result = await res.json();
            return result.data || [];
        } catch (e) { 
            console.error("Fetch SQL Error:", e); 
            return []; 
        }
    };

    const showToast<%=rnd%> = (msg, colorClass) => {
        if(typeof tampilkanToast === "function") {
            tampilkanToast(msg, colorClass);
        } else {
            alert(msg);
        }
    };

    // ==========================================
    // KEBIJAKAN KONTAK WAJIB PER TIPE ANGGOTA
    // ==========================================
    // Peta id tipe -> { wajibHp, wajibEmail }, diisi saat loadComboMasterAnggota.
    let mapKebijakanTipe<%=rnd%> = {};

    // Cermin TipeAnggotaKoperasi.defaultWajibHp: saat kolom wajib_hp NULL,
    // wajib HP dianggap true bila nama tipe mengandung pegawai/dosen/guru/umum
    // (selain itu false, mis. Mahasiswa/Siswa). wajib_email NULL -> false utk semua.
    const defaultWajibHp<%=rnd%> = (nama) => {
        const n = (nama || '').toLowerCase();
        return n.includes('pegawai') || n.includes('dosen') || n.includes('guru') || n.includes('umum');
    };
    const bacaBool<%=rnd%> = (val, defVal) => {
        if (val === null || val === undefined || val === '') return defVal;
        return (val === true || val === 't' || val === 'true');
    };

    // Ambil kebijakan tipe terpilih; fallback default per nama bila peta belum termuat
    const kebijakanTipe<%=rnd%> = (idTipe) => {
        if (!idTipe) return { wajibHp: false, wajibEmail: false };
        if (mapKebijakanTipe<%=rnd%>[idTipe]) return mapKebijakanTipe<%=rnd%>[idTipe];
        const elKategori = document.getElementById('inputKategori<%=rnd%>');
        const opt = elKategori.options[elKategori.selectedIndex];
        const nama = opt ? (opt.getAttribute('data-nama') || '') : '';
        return { wajibHp: defaultWajibHp<%=rnd%>(nama), wajibEmail: false };
    };

    // Tanda * dinamis pada label HP/Email mengikuti kebijakan tipe terpilih
    const perbaruiTandaKontakWajib<%=rnd%> = () => {
        const kebijakan = kebijakanTipe<%=rnd%>(document.getElementById('inputKategori<%=rnd%>').value);
        document.getElementById('tandaWajibHp<%=rnd%>').style.display = kebijakan.wajibHp ? 'inline' : 'none';
        document.getElementById('tandaWajibEmail<%=rnd%>').style.display = kebijakan.wajibEmail ? 'inline' : 'none';
    };

    // ==========================================
    // KIRIM NOTIFIKASI MANUAL & LIHAT KREDENSIAL
    // ==========================================
    const lihatPassword<%=rnd%> = async (idAnggota) => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Mengambil data kredensial...")%>', 'bg-info text-dark');

        let fetchUrl = "<%=Common.ROOT%>/baru?p=kantin%2Fmember&s=anggota_koperasi";
        fetchUrl += '&is_ajax_get_password=true&id_anggota=' + idAnggota + '&hanya_tampil_jsp=true';

        try {
            const response = await fetch(fetchUrl, { method: 'GET' });
            const result = await response.json();

            if (result.status === 'success') {
                document.getElementById('lblShowUserId<%=rnd%>').value = result.userid || '-';
                document.getElementById('lblShowPassword<%=rnd%>').value = result.password || '-';

                if (!modalLihatPasswordInstance<%=rnd%>) {
                    modalLihatPasswordInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalLihatPassword<%=rnd%>'));
                }
                modalLihatPasswordInstance<%=rnd%>.show();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan:")%> ' + result.message, 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server untuk mengambil password.")%>', 'bg-danger text-white');
        }
    };

    const renderTampilanLogNotifikasi<%=rnd%> = (logArray) => {
        let logText = "";
        if(logArray.length === 0) {
            logText = "Tidak ada log atau tidak ada tindakan yang dilakukan (semua aman/sudah terkirim).";
        } else {
            logArray.forEach(log => {
                let color = '#ffffff';
                if (log.includes('ERROR')) color = '#ff4d4d';
                else if (log.includes('SUCCESS') || log.includes('ACTION')) color = '#20c997';
                else if (log.includes('WARNING') || log.includes('SKIP')) color = '#ffc107';
                else if (log.includes('INFO')) color = '#0dcaf0';
                
                logText += '<span style="color: ' + color + ';">' + log + '</span>\n';
            });
        }

        document.getElementById('printAreaLog<%=rnd%>').innerHTML = logText;
        
        if (!modalLogNotifInstance<%=rnd%>) {
            modalLogNotifInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalLogNotif<%=rnd%>'));
        }
        modalLogNotifInstance<%=rnd%>.show();
    };

    const konfirmasiKirimNotifMassal<%=rnd%> = () => {
        if (!document.getElementById('modalConfirmNotif<%=rnd%>')) {
            const modalHtml = `
            <div class="modal fade" id="modalConfirmNotif<%=rnd%>" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                        <div class="modal-header bg-warning text-dark border-bottom-0 pb-2 pt-3 px-4">
                            <h5 class="modal-title fw-bold">
                                <i class="fas fa-paper-plane me-2"></i><%=Common.getBahasaConfig("Konfirmasi Pengiriman")%>
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-4 text-center">
                            <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3" id="iconWarningNotif<%=rnd%>" style="display:none;"></i>
                            <p class="text-dark fw-medium mb-4" id="textConfirmNotif<%=rnd%>"></p>
                            <div class="mt-4">
                                <button type="button" class="btn btn-light px-4 me-2 rounded-pill fw-bold border shadow-sm" data-bs-dismiss="modal"><%=Common.getBahasaConfig("Batal")%></button>
                                <button type="button" class="btn btn-primary px-4 rounded-pill fw-bold shadow-sm" id="btnEksekusiNotifMassal<%=rnd%>">
                                    <i class="fas fa-check me-2"></i><%=Common.getBahasaConfig("Ya, Kirim Sekarang")%>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
        
        const hariIni = new Date().getDay(); // 0 = Minggu
        const textEl = document.getElementById('textConfirmNotif<%=rnd%>');
        const iconEl = document.getElementById('iconWarningNotif<%=rnd%>');
        const btnEksekusi = document.getElementById('btnEksekusiNotifMassal<%=rnd%>');
        
        let isPaksa = false;
        
        if (hariIni === 0) {
            // Jika hari Minggu
            textEl.innerHTML = '<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin memproses dan mengirim notifikasi evaluasi massal ke seluruh anggota aktif?")%>';
            iconEl.style.display = 'none';
            isPaksa = false;
        } else {
            // Jika bukan hari Minggu
            textEl.innerHTML = '<%=Common.getBahasaConfigJS("<b>Perhatian!</b> Sekarang BUKAN hari Minggu (bukan jadwal rutin evaluasi belanja).<br><br>Apakah Anda yakin tetap ingin memaksa mengirim pesan evaluasi di hari libur ini?")%>';
            iconEl.style.display = 'inline-block';
            isPaksa = true;
        }
        
        btnEksekusi.setAttribute('onclick', 'eksekusiKirimNotifMassal<%=rnd%>(' + isPaksa + ')');
        
        modalConfirmNotifInstance<%=rnd%> = new bootstrap.Modal(document.getElementById('modalConfirmNotif<%=rnd%>'));
        modalConfirmNotifInstance<%=rnd%>.show();
    };

    const eksekusiKirimNotifMassal<%=rnd%> = async (isPaksa) => {
        if (modalConfirmNotifInstance<%=rnd%>) {
            modalConfirmNotifInstance<%=rnd%>.hide();
        }

        const btn = document.getElementById('btnKirimNotif<%=rnd%>');
        const oriText = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Memproses...")%>';
        btn.disabled = true;

        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Memulai evaluasi massal seluruh anggota aktif...")%>', 'bg-info text-dark');

        let fetchUrl = "<%=Common.ROOT%>/baru?p=kantin%2Fmember&s=anggota_koperasi";
        fetchUrl += '&is_ajax_kirim_notif=true&paksa=' + isPaksa + '&hanya_tampil_jsp=true';

        try {
            const response = await fetch(fetchUrl, { method: 'GET' });
            const result = await response.json();

            if (result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Proses evaluasi massal selesai!")%>', 'bg-success text-white');
                renderTampilanLogNotifikasi<%=rnd%>(result.data || []);
                loadDataAnggota<%=rnd%>();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan:")%> ' + result.message, 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server saat sinkronisasi notifikasi.")%>', 'bg-danger text-white');
        } finally {
            btn.innerHTML = oriText;
            btn.disabled = false;
        }
    };

    const kirimNotifSatuan<%=rnd%> = async (idAnggota) => {
        showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Mengevaluasi target belanja anggota yang dipilih...")%>', 'bg-info text-dark');

        let fetchUrl = "<%=Common.ROOT%>/baru?p=kantin%2Fmember&s=anggota_koperasi";
        fetchUrl += '&is_ajax_kirim_notif_satuan=true&id_anggota=' + idAnggota + '&hanya_tampil_jsp=true';

        try {
            const response = await fetch(fetchUrl, { method: 'GET' });
            const result = await response.json();

            if (result.status === 'success') {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Proses evaluasi anggota selesai!")%>', 'bg-success text-white');
                renderTampilanLogNotifikasi<%=rnd%>(result.data || []);
                loadDataAnggota<%=rnd%>();
            } else {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Terjadi kesalahan:")%> ' + result.message, 'bg-danger text-white');
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal terhubung ke server untuk mengevaluasi data anggota.")%>', 'bg-danger text-white');
        }
    };

    const cetakLogNotif<%=rnd%> = () => {
        const logContent = document.getElementById('printAreaLog<%=rnd%>').textContent;
        const printWindow = window.open('', '', 'height=600,width=800');
        
        let html = '<html><head><title><%=Common.getBahasaConfig("Cetak Log Evaluasi")%></title>';
        html += '<style>body { font-family: monospace; white-space: pre-wrap; padding: 20px; }</style>';
        html += '</head><body>';
        html += '<h3>Laporan Log Evaluasi Belanja & Notifikasi</h3><hr>';
        html += logContent;
        html += '</body></html>';

        printWindow.document.write(html);
        printWindow.document.close();
        printWindow.focus();
        setTimeout(() => { printWindow.print(); }, 500);
    };

    // Label UI Switch Status Aktif
    document.getElementById('inputStatusAktif<%=rnd%>').addEventListener('change', function() {
        const lbl = document.getElementById('labelStatusAktif<%=rnd%>');
        if(this.checked) {
            lbl.innerText = '<%=Common.getBahasaConfigJS("Aktif")%>';
            lbl.className = 'form-check-label fs-6 fw-bold text-success';
        } else {
            lbl.innerText = '<%=Common.getBahasaConfigJS("Tidak Aktif")%>';
            lbl.className = 'form-check-label fs-6 fw-bold text-danger';
        }
    });

    // Logika Evaluasi Masa Berlaku / Kedaluwarsa
    const evaluasiStatusAktif<%=rnd%> = () => {
        const valTgl = document.getElementById('inputTanggalKadaluarsa<%=rnd%>').value;
        const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
        
        if (valTgl !== '') {
            const today = new Date();
            today.setHours(0,0,0,0);
            const expDate = new Date(valTgl);
            expDate.setHours(0,0,0,0);
            
            // Jika hari ini sudah sama atau melebihi tanggal kedaluwarsa
            if (expDate <= today) {
                chkAktif.checked = false;
                chkAktif.disabled = true;
                chkAktif.dispatchEvent(new Event('change'));
                return;
            }
        }
        
        // Jika tidak kedaluwarsa atau dikosongkan, kembalikan kemampuan edit toggle
        chkAktif.disabled = false;
        chkAktif.dispatchEvent(new Event('change'));
    };

    // Toggle Manual Kode Member
    const toggleManualKode<%=rnd%> = () => {
        const isChecked = document.getElementById('checkManualKode<%=rnd%>').checked;
        const elKode = document.getElementById('inputKodeMember<%=rnd%>');
        
        if (isChecked) {
            elKode.readOnly = false;
            elKode.classList.remove('bg-light');
            elKode.focus();
        } else {
            elKode.readOnly = true;
            elKode.classList.add('bg-light');
            generateAndSetKodeMember<%=rnd%>();
        }
    };

    // ==========================================
    // AUTO GENERATOR KODE MEMBER
    // ==========================================
    const extractPrefix<%=rnd%> = () => {
        const elJenis = document.getElementById('inputJenisAnggota<%=rnd%>');
        if (elJenis && elJenis.selectedIndex > 0) {
            const kode = elJenis.options[elJenis.selectedIndex].getAttribute('data-kode');
            if (kode && kode.trim() !== '') return kode.toUpperCase();
        } 
        return "MEM";
    };

    const generateAndSetKodeMember<%=rnd%> = async () => {
        if (isEditMode<%=rnd%>) return; 
        if(document.getElementById('checkManualKode<%=rnd%>').checked) return;
        
        const elKode = document.getElementById('inputKodeMember<%=rnd%>');
        elKode.value = '<%=Common.getBahasaConfigJS("Menghitung Kode...")%>';

        const prefix = extractPrefix<%=rnd%>();
        const idJenis = document.getElementById('inputJenisAnggota<%=rnd%>').value;
        
        const date = new Date();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const yyyy = date.getFullYear();

        try {
            const sqlAll = "SELECT COALESCE(MAX(id), 0) as jml FROM koperasi.anggota_koperasi";
            const resAll = await fetchData<%=rnd%>(sqlAll);
            let countAll = (resAll && resAll[0]) ? parseInt(resAll[0].jml) : 0;

            let countJenis = 0;
            if (idJenis) {
                const sqlJenis = "SELECT COUNT(id) as jml FROM koperasi.anggota_koperasi WHERE jenis_anggota_koperasi = " + idJenis;
                const resJenis = await fetchData<%=rnd%>(sqlJenis);
                countJenis = (resJenis && resJenis[0]) ? parseInt(resJenis[0].jml) : 0;
            }

            let numJenis = countJenis + 1;
            let numAll = countAll + 1;
            let proposedCode = "";
            let isUnique = false;

            while (!isUnique) {
                proposedCode = prefix + "-" + numJenis + "/" + mm + "/" + yyyy + "/" + numAll;
                let sqlCheck = "SELECT id FROM koperasi.anggota_koperasi WHERE kode = '" + proposedCode + "'";
                let resCheck = await fetchData<%=rnd%>(sqlCheck);
                
                if (resCheck && resCheck.length > 0) {
                    numJenis++;
                    numAll++;
                } else {
                    isUnique = true;
                }
            }
            elKode.value = proposedCode;
        } catch (e) {
            console.error("Error generating code", e);
            elKode.value = prefix + "-" + new Date().getTime(); 
        }
    };


    // ==========================================
    // INIT MASTER DATA (Combo Jenis & Tipe Anggota)
    // ==========================================
    const loadComboMasterAnggota<%=rnd%> = async () => {
        // Jenis Anggota
        const sqlJenis = "SELECT id, nama, kode FROM koperasi.jenis_anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        const resJenis = await fetchData<%=rnd%>(sqlJenis);
        
        let optHtmlJenis = '<option value=""><%=Common.getBahasaConfig("- Pilih Jenis -")%></option>';
        resJenis.forEach(item => {
            optHtmlJenis += '<option value="' + item.id + '" data-kode="' + (item.kode || 'MEM') + '">' + item.nama + '</option>';
        });
        document.getElementById('inputJenisAnggota<%=rnd%>').innerHTML = optHtmlJenis;
        
        let optFilterJenis = '<option value=""><%=Common.getBahasaConfig("- Semua Jenis -")%></option>';
        resJenis.forEach(item => { optFilterJenis += '<option value="' + item.id + '">' + item.nama + '</option>'; });
        document.getElementById('filterJenisAnggota<%=rnd%>').innerHTML = optFilterJenis;

        // Tipe Anggota (sekalian memuat kebijakan kontak wajib per tipe)
        const sqlTipe = "SELECT id, nama, kode, wajib_hp, wajib_email FROM koperasi.tipe_anggota_koperasi WHERE aktif = true ORDER BY nama ASC";
        const resTipe = await fetchData<%=rnd%>(sqlTipe);

        let optHtmlTipe = '<option value="" data-nama="UMUM"><%=Common.getBahasaConfig("- Pilih Tipe/Kategori -")%></option>';
        mapKebijakanTipe<%=rnd%> = {};
        resTipe.forEach(item => {
            const safeNama = item.nama ? item.nama.toUpperCase() : 'UMUM';
            optHtmlTipe += '<option value="' + item.id + '" data-nama="' + safeNama + '">' + item.nama + '</option>';
            mapKebijakanTipe<%=rnd%>[item.id] = {
                wajibHp: bacaBool<%=rnd%>(item.wajib_hp, defaultWajibHp<%=rnd%>(item.nama)),
                wajibEmail: bacaBool<%=rnd%>(item.wajib_email, false)
            };
        });
        document.getElementById('inputKategori<%=rnd%>').innerHTML = optHtmlTipe;
        
        let optFilterTipe = '<option value=""><%=Common.getBahasaConfig("- Semua Tipe -")%></option>';
        resTipe.forEach(item => { optFilterTipe += '<option value="' + item.id + '">' + item.nama + '</option>'; });
        document.getElementById('filterTipeAnggota<%=rnd%>').innerHTML = optFilterTipe;
    };

    // ==========================================
    // LOGIKA PENGECEKAN USER ID
    // ==========================================
    const checkUserId<%=rnd%> = async () => {
        if (isEditMode<%=rnd%>) {
            isUserIdValid<%=rnd%> = true;
            document.getElementById('errUserId<%=rnd%>').style.display = 'none';
            document.getElementById('statusUserId<%=rnd%>').style.display = 'none';
            return;
        }

        const uid = document.getElementById('inputUserId<%=rnd%>').value.trim();
        const errEl = document.getElementById('errUserId<%=rnd%>');
        const statEl = document.getElementById('statusUserId<%=rnd%>');
        const idAnggota = document.getElementById('inputIdAnggota<%=rnd%>').value;
        
        if (uid === '') {
            isUserIdValid<%=rnd%> = true;
            errEl.style.display = 'none';
            statEl.style.display = 'none';
            return;
        }

        let sql = "SELECT userid as id FROM public.tbmuser WHERE userid = '" + uid.replace(/'/g, "''") + "'";
        if(idAnggota !== '') {
            sql += " AND id != (SELECT tbmuser FROM koperasi.anggota_koperasi WHERE id = " + idAnggota + " LIMIT 1)";
        }
        
        statEl.style.display = 'inline-block';
        statEl.innerHTML = '<i class="fas fa-spinner fa-spin text-primary"></i>';
        
        try {
            const res = await fetchData<%=rnd%>(sql);
            if (res && res.length > 0) {
                isUserIdValid<%=rnd%> = false;
                statEl.innerHTML = '<i class="fas fa-times text-danger"></i>';
                errEl.style.display = isEditMode<%=rnd%> ? 'none' : 'block';
            } else {
                isUserIdValid<%=rnd%> = true;
                statEl.innerHTML = '<i class="fas fa-check text-success"></i>';
                errEl.style.display = 'none';
            }
        } catch (e) {
            console.error("Gagal mengecek User ID", e);
        }
    };


    // ==========================================
    // BAGIAN 2: LOGIKA LIST, PAGING & SEARCH
    // ==========================================
    const buildFilterSQL<%=rnd%> = () => {
        const keyword = document.getElementById('searchAnggota<%=rnd%>').value.trim().replace(/'/g, "''");
        const filterJenis = document.getElementById('filterJenisAnggota<%=rnd%>').value;
        const filterTipe = document.getElementById('filterTipeAnggota<%=rnd%>').value;

        let filters = " WHERE 1=1 ";
        if (keyword !== '') {
            filters += " AND (a.nama ILIKE '%" + keyword + "%' OR a.kode ILIKE '%" + keyword + "%' OR a.kode_identitas ILIKE '%" + keyword + "%' OR a.email_nasabah ILIKE '%" + keyword + "%' OR a.hp ILIKE '%" + keyword + "%' OR a.userid ILIKE '%" + keyword + "%') ";
        }
        if (filterJenis !== '') {
            filters += " AND a.jenis_anggota_koperasi = " + filterJenis + " ";
        }
        if (filterTipe !== '') {
            filters += " AND a.tipe_anggota_koperasi = " + filterTipe + " ";
        }
        return filters;
    };

    const resetAndLoadData<%=rnd%> = () => {
        currentPage<%=rnd%> = 1; 
        loadDataAnggota<%=rnd%>();
    };

    const loadDataAnggota<%=rnd%> = async () => {
        const tbody = document.getElementById('tabelDataAnggota<%=rnd%>');
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5"><div class="spinner-border text-primary mb-3"></div><br><span class="text-muted fw-medium"><%=Common.getBahasaConfig("Mengambil data...")%></span></td></tr>';

        const sqlFilters = buildFilterSQL<%=rnd%>();
        const offset = (currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%>;

        const sqlCount = "SELECT COUNT(*) AS jumlah FROM koperasi.anggota_koperasi a " + sqlFilters + ";";
        const sqlData = "SELECT a.id, a.kode, a.kode_identitas, a.nama, a.hp, a.telp as telepon, a.email_nasabah as email, a.aktif, " +
                        "j.nama as nama_jenis_anggota, a.userid " + 
                        "FROM koperasi.anggota_koperasi a " +
                        "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id " +
                        sqlFilters + " ORDER BY a.id DESC LIMIT " + limitPerPage<%=rnd%> + " OFFSET " + offset + ";";

        try {
            const [resCount, resData] = await Promise.all([
                fetchData<%=rnd%>(sqlCount),
                fetchData<%=rnd%>(sqlData)
            ]);

            totalRecords<%=rnd%> = (resCount && resCount[0]) ? parseInt(resCount[0].jumlah || 0) : 0;
            const recordList = resData || [];

            let htmlList = '';

            if (recordList.length === 0) {
                htmlList = '<tr><td colspan="9" class="text-center text-muted py-5"><i class="fas fa-users-slash fa-3x mb-3 opacity-50 d-block"></i><%=Common.getBahasaConfig("Tidak ada data anggota yang ditemukan.")%></td></tr>';
            } else {
                let no = offset + 1; 
                recordList.forEach((row) => {
                    const namaJenis = row.nama_jenis_anggota || '<%=Common.getBahasaConfigJS("Umum")%>';
                    const kontakHp = row.hp ? row.hp : '-';
                    const kontakTelp = row.telepon ? row.telepon : '-';
                    const showKontak = (kontakHp !== '-' ? '<div class="text-nowrap"><i class="fab fa-whatsapp text-success me-1"></i>' + kontakHp + '</div>' : '') + 
                                       (kontakTelp !== '-' && kontakHp === '-' ? '<div class="text-nowrap"><i class="fas fa-phone text-secondary me-1"></i>' + kontakTelp + '</div>' : '');

                    const isStatusAktif = (row.aktif === null || row.aktif === true || row.aktif === 'true');
                    const badgeStatus = isStatusAktif ? '<span class="badge bg-success bg-opacity-10 text-success border border-success px-2 py-1"><i class="fas fa-check-circle me-1"></i><%=Common.getBahasaConfig("Aktif")%></span>' : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger px-2 py-1"><i class="fas fa-times-circle me-1"></i><%=Common.getBahasaConfig("Non-Aktif")%></span>';

                    const txtKode = row.kode ? row.kode : '-';
                    const txtUserid = row.userid ? row.userid : '-';
                    const urldata = '<%=urlQr%>&anggotaKoperasi='+row.id;

                    let actBtn = '';
                    if (isAdmin<%=rnd%>) {
                        actBtn += '<button class="btn btn-sm btn-outline-info text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="lihatPassword<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Lihat Kredensial (Password)")%>"><i class="fas fa-key"></i></button>';
                        actBtn += '<button class="btn btn-sm btn-outline-primary text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="kirimNotifSatuan<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Evaluasi & Kirim Notif")%>"><i class="fas fa-paper-plane"></i></button>';
                    }
                    actBtn += '<button class="btn btn-sm btn-outline-success text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="loadModalContentCustom(\'QR-Code\', \''+urldata+'\', \'600px\');" title="<%=Common.getBahasaConfig("Lihat Kode Login")%>"><i class="fas fa-qrcode"></i></button>' +
                              '<button class="btn btn-sm btn-outline-warning text-dark shadow-sm px-2 me-1 mb-1 fw-bold" onclick="editAnggota<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Edit Data Anggota")%>"><i class="fas fa-edit"></i></button>' +
                              '<button class="btn btn-sm btn-outline-danger shadow-sm px-2 mb-1 fw-bold" onclick="hapusAnggota<%=rnd%>(' + row.id + ')" title="<%=Common.getBahasaConfig("Hapus Data Anggota")%>"><i class="fas fa-trash-alt"></i></button>';
                    
                    htmlList += '<tr class="border-bottom">' +
                            '<td class="text-center text-muted fw-medium">' + (no++) + '</td>' +
                            '<td class="text-center"><i class="fas fa-user-circle fa-2x text-secondary opacity-50"></i></td>' +
                            '<td class="text-start fw-bold text-primary">' + txtKode + '</td>' +
                            '<td class="text-start">' +
                                '<div class="fw-bold text-dark">' + (row.nama || '-') + '</div>' +
                                '<small class="text-muted"><i class="far fa-id-card me-1"></i>' + (row.kode_identitas || '-') + '</small>' +
                            '</td>' +
                            '<td class="text-start fw-semibold text-secondary">' + txtUserid + '</td>' +
                            '<td class="text-center"><span class="badge bg-info text-dark rounded-pill px-3 shadow-sm py-2 w-100"><i class="fas fa-tags me-1"></i>' + namaJenis + '</span></td>' +
                            '<td class="text-start">' + (showKontak || '-') + '</td>' +
                            '<td class="text-center">' + badgeStatus + '</td>' +
                            '<td class="text-center text-nowrap">' + actBtn + '</td>' +
                        '</tr>';
                });
            }
            tbody.innerHTML = htmlList;
            updatePaginationUI<%=rnd%>();

        } catch (error) {
            console.error("Gagal load tabel anggota:", error);
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger py-5"><i class="fas fa-exclamation-triangle fa-2x mb-3 d-block"></i><%=Common.getBahasaConfig("Gagal menarik data. Silakan muat ulang.")%></td></tr>';
        }
    };

    const updatePaginationUI<%=rnd%> = () => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>) || 1;
        document.getElementById('pageNumberDisplay<%=rnd%>').innerText = currentPage<%=rnd%> + " / " + totalPages;
        
        const startIdx = totalRecords<%=rnd%> > 0 ? ((currentPage<%=rnd%> - 1) * limitPerPage<%=rnd%> + 1) : 0;
        const endIdx = Math.min(currentPage<%=rnd%> * limitPerPage<%=rnd%>, totalRecords<%=rnd%>);
        
        document.getElementById('pagingInfoAnggota<%=rnd%>').innerHTML = '<i class="fas fa-list-ul text-primary me-2"></i><%=Common.getBahasaConfig("Menampilkan")%> <b class="text-dark">' + startIdx + ' - ' + endIdx + '</b> <%=Common.getBahasaConfig("dari total")%> <b class="text-dark">' + totalRecords<%=rnd%> + '</b> <%=Common.getBahasaConfig("anggota")%>';
        
        document.getElementById('btnPrevPage<%=rnd%>').disabled = (currentPage<%=rnd%> === 1);
        document.getElementById('btnNextPage<%=rnd%>').disabled = (currentPage<%=rnd%> === totalPages);
    };

    const changePageAnggota<%=rnd%> = (direction) => {
        const totalPages = Math.ceil(totalRecords<%=rnd%> / limitPerPage<%=rnd%>);
        if (direction === -1 && currentPage<%=rnd%> > 1) {
            currentPage<%=rnd%>--;
            loadDataAnggota<%=rnd%>();
        } else if (direction === 1 && currentPage<%=rnd%> < totalPages) {
            currentPage<%=rnd%>++;
            loadDataAnggota<%=rnd%>();
        }
    };


    // ==========================================
    // BAGIAN 3: LOGIKA FORM SIVITAS & DINAMIS UI
    // ==========================================
    const bukaFormTambah<%=rnd%> = () => {
        isEditMode<%=rnd%> = false; 
        isUserIdValid<%=rnd%> = true;
        
        document.getElementById('inputIdAnggota<%=rnd%>').value = '';
        document.getElementById('formAnggota<%=rnd%>').reset();
        document.getElementById('idRefSelected<%=rnd%>').value = '';
        
        // Reset Tanggal Kadaluarsa dan Aktifkan Switch
        document.getElementById('inputTanggalKadaluarsa<%=rnd%>').value = '';
        
        // Menampilkan kembali switch Aktif
        const headerSwitches = document.getElementById('headerSwitches<%=rnd%>');
        if (headerSwitches) {
            headerSwitches.classList.remove('d-none');
            headerSwitches.classList.add('d-flex');
        }

        // Reset Status User ID agar bisa diisi
        document.getElementById('inputUserId<%=rnd%>').readOnly = false;
        document.getElementById('inputUserId<%=rnd%>').disabled = false;
        document.getElementById('errUserId<%=rnd%>').style.display = 'none';
        document.getElementById('statusUserId<%=rnd%>').style.display = 'none';
        
        const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
        chkAktif.disabled = false;
        chkAktif.checked = true;
        chkAktif.dispatchEvent(new Event('change'));

        const cbKategori = document.getElementById('inputKategori<%=rnd%>');
        cbKategori.value = '';
        cbKategori.disabled = false; 
        
        document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-user-plus text-primary me-2"></i><%=Common.getBahasaConfig("Tambah Anggota Baru")%>';
        
        const chkManual = document.getElementById('checkManualKode<%=rnd%>');
        chkManual.checked = false;
        toggleManualKode<%=rnd%>();
        
        ubahModeInput<%=rnd%>();
        generateAndSetKodeMember<%=rnd%>(); 
        
        document.getElementById('inputNama<%=rnd%>').readOnly = false;
        
        document.getElementById('viewList<%=rnd%>').style.display = 'none';
        document.getElementById('viewForm<%=rnd%>').style.display = 'block';
    };

    const tutupForm<%=rnd%> = () => {
        document.getElementById('viewForm<%=rnd%>').style.display = 'none';
        document.getElementById('viewList<%=rnd%>').style.display = 'block';
        loadDataAnggota<%=rnd%>();
    };

    const ubahModeInput<%=rnd%> = () => {
        const elKategori = document.getElementById('inputKategori<%=rnd%>');
        const idTipe = elKategori.value;
        const optSelected = elKategori.options[elKategori.selectedIndex];
        const namaTipe = optSelected ? (optSelected.getAttribute('data-nama') || '') : '';

        let internalCat = 'UMUM';
        if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
        else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
        else if (namaTipe.includes('GURU')) internalCat = 'GURU';
        else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
        else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';

        const areaRef = document.getElementById('areaCariReferensi<%=rnd%>');
        const labelRef = document.getElementById('labelCariReferensi<%=rnd%>');
        
        if (!isEditMode<%=rnd%>) {
            document.getElementById('inputCariReferensi<%=rnd%>').value = '';
            document.getElementById('idRefSelected<%=rnd%>').value = '';
        }

        if (internalCat === 'UMUM' || idTipe === '' || !wajibRefMaster<%=rnd%>) {
            areaRef.style.display = 'none';
            document.getElementById('inputNama<%=rnd%>').readOnly = false;
        } else {
            if (isEditMode<%=rnd%>) {
                 areaRef.style.display = 'none'; 
            } else {
                 areaRef.style.display = 'block';
                 labelRef.innerText = '<%=Common.getBahasaConfigJS("Cari Master Data Sivitas ")%>' + optSelected.text;
                 loadDataReferensi<%=rnd%>(internalCat, ''); 
            }
            document.getElementById('inputNama<%=rnd%>').readOnly = false;
        }

        // Segarkan tanda * kontak wajib setiap kali tipe/kategori berubah
        perbaruiTandaKontakWajib<%=rnd%>();
    };

    document.getElementById('inputJenisAnggota<%=rnd%>').addEventListener('change', () => {
        if (!isEditMode<%=rnd%>) generateAndSetKodeMember<%=rnd%>();
    });
    document.getElementById('inputKategori<%=rnd%>').addEventListener('change', () => {
        if (!isEditMode<%=rnd%>) generateAndSetKodeMember<%=rnd%>();
    });

    let timeoutRef<%=rnd%> = null;
    document.getElementById('inputCariReferensi<%=rnd%>').addEventListener('input', function() {
        const keyword = this.value.trim();
        const opts = document.getElementById('listReferensi<%=rnd%>').options;
        const isMatch = Array.from(opts).some(opt => opt.value === keyword);
        
        if (!isMatch && keyword.length >= 3) {
            clearTimeout(timeoutRef<%=rnd%>);
            timeoutRef<%=rnd%> = setTimeout(() => {
                loadDataReferensi<%=rnd%>('', keyword);
            }, 600); 
        }
    });

    const loadDataReferensi<%=rnd%> = async (kategoriOverride, keyword = '') => {
        const elKategori = document.getElementById('inputKategori<%=rnd%>');
        const optSelected = elKategori.options[elKategori.selectedIndex];
        const namaTipe = optSelected ? (optSelected.getAttribute('data-nama') || '') : '';
        
        let internalCat = 'UMUM';
        if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
        else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
        else if (namaTipe.includes('GURU')) internalCat = 'GURU';
        else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
        else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';

        const kategori = kategoriOverride || internalCat;
        if (kategori === 'UMUM') return;

        const elSearchRef = document.getElementById('inputCariReferensi<%=rnd%>');
        const originalPlaceholder = elSearchRef.placeholder;
        elSearchRef.placeholder = '<%=Common.getBahasaConfigJS("Mencari data...")%>';
        
        let sql = '';
        let prefix = '';
        const safeKeyword = keyword.replace(/'/g, "''");
        let condSql = "";
        
        if (kategori === 'MAHASISWA') {
            condSql = safeKeyword !== '' ? " AND (nama ILIKE '%" + safeKeyword + "%' OR nim ILIKE '%" + safeKeyword + "%') " : "";
            sql = "SELECT id, nama, nim as kode, telp as hp FROM public.mahasiswa WHERE aktif = true " + condSql + " ORDER BY nama ASC LIMIT 50";
            prefix = "NIM";
        } else if (kategori === 'SISWA') {
            condSql = safeKeyword !== '' ? " AND (nama ILIKE '%" + safeKeyword + "%' OR nomor_induk ILIKE '%" + safeKeyword + "%') " : "";
            sql = "SELECT id, nama_siswa as nama, nomor_induk as kode, telepon_siswa as hp FROM sekolah.siswa WHERE aktif = true " + condSql + " ORDER BY nama ASC LIMIT 50";
            prefix = "NIS";
        } else if (kategori === 'GURU') {
            condSql = safeKeyword !== '' ? " AND (nama ILIKE '%" + safeKeyword + "%' OR nuptk ILIKE '%" + safeKeyword + "%') " : "";
            sql = "SELECT id, nama_guru as nama, nuptk as kode, telepon_guru as hp FROM sekolah.guru WHERE aktif = true " + condSql + " ORDER BY nama ASC LIMIT 50";
            prefix = "NUPTK";
        } else if (kategori === 'DOSEN') {
            condSql = safeKeyword !== '' ? " AND (nama ILIKE '%" + safeKeyword + "%' OR nidn ILIKE '%" + safeKeyword + "%') " : "";
            sql = "SELECT id, nama, nidn as kode, telp as hp FROM public.dosen WHERE aktif = true " + condSql + " ORDER BY nama ASC LIMIT 50";
            prefix = "NIDN";
        } else if (kategori === 'PEGAWAI') {
            condSql = safeKeyword !== '' ? " AND (nama ILIKE '%" + safeKeyword + "%' OR code ILIKE '%" + safeKeyword + "%' OR mycode ILIKE '%" + safeKeyword + "%') " : "";
            sql = "SELECT id, nama, code as kode, hp FROM public.pegawai WHERE aktif = true " + condSql + " ORDER BY nama ASC LIMIT 50";
            prefix = "NIK";
        }

        const data = await fetchData<%=rnd%>(sql);
        arrDataReferensi<%=rnd%> = data;
        
        let htmlList = '';
        data.forEach(item => {
            const label = item.nama + ' (' + prefix + ': ' + (item.kode || '-') + ')';
            htmlList += '<option value="' + label + '" data-id="' + item.id + '" data-nama="' + item.nama + '" data-kode="' + (item.kode || '') + '" data-hp="' + (item.hp || '') + '">';
        });
        
        document.getElementById('listReferensi<%=rnd%>').innerHTML = htmlList;
        elSearchRef.placeholder = originalPlaceholder;
    };

    document.getElementById('inputCariReferensi<%=rnd%>').addEventListener('change', function() {
        const val = this.value;
        const opts = document.getElementById('listReferensi<%=rnd%>').childNodes;
        let match = null;
        
        for (let i = 0; i < opts.length; i++) {
            if (opts[i].value === val) {
                match = {
                    id: opts[i].getAttribute('data-id'),
                    nama: opts[i].getAttribute('data-nama'),
                    kode: opts[i].getAttribute('data-kode'),
                    hp: opts[i].getAttribute('data-hp')
                };
                break;
            }
        }

        if (match) {
            document.getElementById('idRefSelected<%=rnd%>').value = match.id;
            document.getElementById('inputNama<%=rnd%>').value = match.nama;
            document.getElementById('inputKodeIdentitas<%=rnd%>').value = match.kode;
            if(match.hp) document.getElementById('inputHp<%=rnd%>').value = match.hp;
        } else {
            document.getElementById('idRefSelected<%=rnd%>').value = '';
            document.getElementById('inputNama<%=rnd%>').value = '';
            document.getElementById('inputKodeIdentitas<%=rnd%>').value = '';
        }
    });


    // ==========================================
    // BAGIAN 4: SIMPAN, EDIT, & HAPUS
    // ==========================================
    const simpanAnggota<%=rnd%> = async () => {
        
        if (!isEditMode<%=rnd%> && !isUserIdValid<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Pembuatan gagal. User ID telah digunakan oleh pengguna lain!")%>', 'bg-danger text-white');
            document.getElementById('inputUserId<%=rnd%>').focus();
            return;
        }
        
        const idAnggota = document.getElementById('inputIdAnggota<%=rnd%>').value;
        
        const elKategori = document.getElementById('inputKategori<%=rnd%>');
        const idTipeAnggota = elKategori.value;
        const optSelected = elKategori.options[elKategori.selectedIndex];
        const namaTipe = optSelected ? (optSelected.getAttribute('data-nama') || '') : '';

        let internalCat = 'UMUM';
        if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
        else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
        else if (namaTipe.includes('GURU')) internalCat = 'GURU';
        else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
        else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';
        
        const idRef = document.getElementById('idRefSelected<%=rnd%>').value;
        const idJenisAnggota = document.getElementById('inputJenisAnggota<%=rnd%>').value;
        const btnSimpan = document.getElementById('btnSimpan<%=rnd%>');
        const isManual = document.getElementById('checkManualKode<%=rnd%>').checked;
        
        if (!isEditMode<%=rnd%> && internalCat !== 'UMUM' && idRef === '' && wajibRefMaster<%=rnd%>) {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Silakan pilih data referensi master sivitas dari daftar pencarian yang tersedia!")%>', 'bg-warning text-dark');
            document.getElementById('inputCariReferensi<%=rnd%>').focus();
            return;
        }

        // Validasi kontak wajib sesuai kebijakan tipe anggota terpilih
        const kebijakanKontak = kebijakanTipe<%=rnd%>(idTipeAnggota);
        if (kebijakanKontak.wajibHp && document.getElementById('inputHp<%=rnd%>').value.trim() === '') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Nomor HP wajib diisi untuk tipe member ini")%>', 'bg-danger text-white');
            document.getElementById('inputHp<%=rnd%>').focus();
            return;
        }
        if (kebijakanKontak.wajibEmail && document.getElementById('inputEmail<%=rnd%>').value.trim() === '') {
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Email wajib diisi untuk tipe member ini")%>', 'bg-danger text-white');
            document.getElementById('inputEmail<%=rnd%>').focus();
            return;
        }

        const dataObj = {
            nama: document.getElementById('inputNama<%=rnd%>').value,
            kodeIdentitas: document.getElementById('inputKodeIdentitas<%=rnd%>').value,
            hp: document.getElementById('inputHp<%=rnd%>').value,
            telp: document.getElementById('inputTelepon<%=rnd%>').value, 
            email: document.getElementById('inputEmail<%=rnd%>').value, 
            keterangan: document.getElementById('inputKeterangan<%=rnd%>').value,
            aktif: document.getElementById('inputStatusAktif<%=rnd%>').checked
        };
        
        const tglKadal = document.getElementById('inputTanggalKadaluarsa<%=rnd%>').value;
        if(tglKadal !== "") {
            dataObj.tanggalKadaluarsa = tglKadal;
        } else {
            dataObj.tanggalKadaluarsa = null;
        }
        
        const uIdVal = document.getElementById('inputUserId<%=rnd%>').value.trim();
        const passVal = document.getElementById('inputPass<%=rnd%>').value.trim();
        if (uIdVal !== '') dataObj.userid = uIdVal;
        if (passVal !== '') dataObj.pass = passVal;

        let proposedCode = document.getElementById('inputKodeMember<%=rnd%>').value.trim();
        if (!isEditMode<%=rnd%> && !isManual) {
            let checkSql = "SELECT id FROM koperasi.anggota_koperasi WHERE kode = '" + proposedCode + "'";
            let resCheck = await fetchData<%=rnd%>(checkSql);
            
            if (resCheck && resCheck.length > 0) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Kode member berbenturan dengan sesi lain. Mengkalkulasi ulang kode secara otomatis...")%>', 'bg-warning text-dark');
                await generateAndSetKodeMember<%=rnd%>();
                proposedCode = document.getElementById('inputKodeMember<%=rnd%>').value;
            }
        }
        dataObj.kode = proposedCode;
        
        if (idTipeAnggota !== '') {
            dataObj.tipeAnggotaKoperasi = idTipeAnggota;
        }

        if (wajibRefMaster<%=rnd%>) {
            if (internalCat === 'MAHASISWA' && idRef !== '') { dataObj.mahasiswa = idRef; }
            else if (internalCat === 'SISWA' && idRef !== '') { dataObj.siswa = idRef; }
            else if (internalCat === 'GURU' && idRef !== '') { dataObj.guru = idRef; }
            else if (internalCat === 'DOSEN' && idRef !== '') { dataObj.dosen = idRef; }
            else if (internalCat === 'PEGAWAI' && idRef !== '') { dataObj.pegawai = idRef; }
        }
        
        if (idJenisAnggota !== '') {
            dataObj.jenisAnggotaKoperasi = idJenisAnggota;
        }
        
        const payload = { 
            action: "simpanDataRinci", 
            log: "true",
            class: masterModelClass<%=rnd%>, 
            data: dataObj
        };

        if (idAnggota !== '') {
            payload.id = idAnggota;
        }

        const oriBtn = btnSimpan.innerHTML;
        btnSimpan.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span><%=Common.getBahasaConfig("Menyimpan...")%>';
        btnSimpan.disabled = true;

        try {
            const response = await fetch('<%=Common.ROOT%>/Data', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            
            if (result.status === '00' || result.status === 'success' || result.id) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data anggota berhasil disimpan secara permanen!")%>', 'bg-success text-white');
                tutupForm<%=rnd%>(); 
            } else {
                const errorStr = (result.description || "").toLowerCase();
                if (errorStr.includes("duplicate key") || errorStr.includes("unique constraint") || errorStr.includes("sudah ada")) {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Gagal. Terjadi duplikasi data unik (misalnya Kode). Sistem akan memperbarui kode, silakan coba tekan Simpan kembali.")%>', 'bg-warning text-dark');
                    await generateAndSetKodeMember<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Gagal melakukan penyimpanan data ke server.")%>', 'bg-danger text-white');
                }
            }
        } catch (e) {
            console.error(e);
            showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi jaringan terputus. Tidak dapat menyimpan data.")%>', 'bg-danger text-white');
        } finally {
            btnSimpan.innerHTML = oriBtn;
            btnSimpan.disabled = false;
        }
    };

    const editAnggota<%=rnd%> = async (id) => {
        isEditMode<%=rnd%> = true; 
        isUserIdValid<%=rnd%> = true;
        document.getElementById('errUserId<%=rnd%>').style.display = 'none';
        document.getElementById('statusUserId<%=rnd%>').style.display = 'none';
        
        // Menampilkan kembali switch Aktif di mode edit
        const headerSwitches = document.getElementById('headerSwitches<%=rnd%>');
        if (headerSwitches) {
            headerSwitches.classList.remove('d-none');
            headerSwitches.classList.add('d-flex');
        }
        
        const sql = 'SELECT a.id, a.kode, a.kode_identitas, a.nama, a.hp, a.telp as telepon, a.email_nasabah as email, a.keterangan, a.aktif, a.mahasiswa, a.siswa, a.guru, a.dosen, a.pegawai, a.jenis_anggota_koperasi, a.tipe_anggota_koperasi, a.userid, TO_CHAR(a.tanggal_kadaluarsa, \'YYYY-MM-DD\') as tgl_kadaluarsa FROM koperasi.anggota_koperasi a WHERE a.id = ' + id;
        const res = await fetchData<%=rnd%>(sql);
        
        if (res.length > 0) {
            const data = res[0];
            
            document.getElementById('inputIdAnggota<%=rnd%>').value = data.id;
            
            const elKode = document.getElementById('inputKodeMember<%=rnd%>');
            elKode.value = data.kode || '';
            elKode.readOnly = true;
            elKode.classList.add('bg-light');
            
            const chkManual = document.getElementById('checkManualKode<%=rnd%>');
            chkManual.checked = false;
            
            document.getElementById('inputKodeIdentitas<%=rnd%>').value = data.kode_identitas || '';
            document.getElementById('inputNama<%=rnd%>').value = data.nama || '';
            document.getElementById('inputHp<%=rnd%>').value = data.hp || '';
            document.getElementById('inputTelepon<%=rnd%>').value = data.telepon || '';
            document.getElementById('inputEmail<%=rnd%>').value = data.email || '';
            document.getElementById('inputTanggalKadaluarsa<%=rnd%>').value = data.tgl_kadaluarsa || '';
            document.getElementById('inputKeterangan<%=rnd%>').value = data.keterangan || '';
            
            document.getElementById('inputUserId<%=rnd%>').value = data.userid || '';
            document.getElementById('inputPass<%=rnd%>').value = '';
            
            // Mengunci User ID secara paksa pada mode Edit agar tidak dapat diubah
            document.getElementById('inputUserId<%=rnd%>').readOnly = true;
            document.getElementById('inputUserId<%=rnd%>').disabled = true;
            
            if (data.jenis_anggota_koperasi) {
                document.getElementById('inputJenisAnggota<%=rnd%>').value = data.jenis_anggota_koperasi;
            } else {
                document.getElementById('inputJenisAnggota<%=rnd%>').value = '';
            }

            if (data.tipe_anggota_koperasi) {
                document.getElementById('inputKategori<%=rnd%>').value = data.tipe_anggota_koperasi;
            } else {
                document.getElementById('inputKategori<%=rnd%>').value = '';
            }

            const isAktif = (data.aktif === null || data.aktif === true || data.aktif === 'true');
            const chkAktif = document.getElementById('inputStatusAktif<%=rnd%>');
            chkAktif.checked = isAktif;
            
            // Lakukan evaluasi disable jika ternyata sudah kedaluwarsa
            evaluasiStatusAktif<%=rnd%>();
            
            let refId = '';
            if(data.mahasiswa) { refId = data.mahasiswa; }
            else if(data.siswa) { refId = data.siswa; }
            else if(data.guru) { refId = data.guru; }
            else if(data.dosen) { refId = data.dosen; }
            else if(data.pegawai) { refId = data.pegawai; }
            
            const cbKategori = document.getElementById('inputKategori<%=rnd%>');
            cbKategori.disabled = true; 
            
            document.getElementById('idRefSelected<%=rnd%>').value = refId;
            
            document.getElementById('formTitle<%=rnd%>').innerHTML = '<i class="fas fa-user-edit text-warning me-2"></i><%=Common.getBahasaConfig("Ubah Data Anggota")%>';
            
            ubahModeInput<%=rnd%>();
            
            const namaTipe = cbKategori.options[cbKategori.selectedIndex] ? cbKategori.options[cbKategori.selectedIndex].getAttribute('data-nama') : '';
            let internalCat = 'UMUM';
            if (namaTipe.includes('MAHASISWA')) internalCat = 'MAHASISWA';
            else if (namaTipe.includes('SISWA')) internalCat = 'SISWA';
            else if (namaTipe.includes('GURU')) internalCat = 'GURU';
            else if (namaTipe.includes('DOSEN')) internalCat = 'DOSEN';
            else if (namaTipe.includes('PEGAWAI')) internalCat = 'PEGAWAI';

            if (internalCat !== 'UMUM' && refId !== '') {
                 document.getElementById('inputCariReferensi<%=rnd%>').value = data.nama + " (ID Master: " + refId + ")";
            }
            
            document.getElementById('viewList<%=rnd%>').style.display = 'none';
            document.getElementById('viewForm<%=rnd%>').style.display = 'block';
        }
    };

    const hapusAnggota<%=rnd%> = async (id) => {
        if (typeof prosesDeleteData === 'function') {
            prosesDeleteData(masterModelClass<%=rnd%>, id, function(){ 
                 if (document.querySelectorAll('#tabelDataAnggota<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                     currentPage<%=rnd%>--;
                 }
                 loadDataAnggota<%=rnd%>();
            });
        } else {
            const isConfirm = confirm('<%=Common.getBahasaConfigJS("Apakah Anda yakin ingin menghapus data anggota ini?")%>');
            if (!isConfirm) return;

            const payload = {
                action: "deleteData",
                class: masterModelClass<%=rnd%>,
                id: id
            };

            try {
                const response = await fetch('<%=Common.ROOT%>/Data', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const result = await response.json();
                if (result.status === '00' || result.status === 'success') {
                    showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Data anggota berhasil dihapus sepenuhnya!")%>', 'bg-success text-white');
                    if (document.querySelectorAll('#tabelDataAnggota<%=rnd%> tr').length === 1 && currentPage<%=rnd%> > 1) {
                        currentPage<%=rnd%>--;
                    }
                    loadDataAnggota<%=rnd%>();
                } else {
                    showToast<%=rnd%>(result.description || '<%=Common.getBahasaConfigJS("Data tidak dapat dihapus. Kemungkinan data ini telah terhubung dengan rekam transaksi pembelanjaan.")%>', 'bg-danger text-white');
                }
            } catch (error) {
                showToast<%=rnd%>('<%=Common.getBahasaConfigJS("Koneksi jaringan terputus. Penghapusan dibatalkan.")%>', 'bg-danger text-white');
            }
        }
    };

    // ==========================================
    // INISIALISASI
    // ==========================================
    document.addEventListener("DOMContentLoaded", () => {
        loadComboMasterAnggota<%=rnd%>();

        // Trigger Pencarian Saat Tekan Enter di semua field filter
        const filterInputs = document.querySelectorAll('.filter-anggota-<%=rnd%>');
        filterInputs.forEach(input => {
            if (input.tagName === 'SELECT') {
                input.addEventListener("change", () => resetAndLoadData<%=rnd%>());
            } else {
                input.addEventListener("keydown", (e) => {
                    if (e.key === "Enter") {
                        e.preventDefault();
                        resetAndLoadData<%=rnd%>();
                    }
                });
            }
        });
        
        loadDataAnggota<%=rnd%>();

        // Dasbor Statistik Anggota
        muatStatistikAnggota<%=rnd%>();
    });

    // ==========================================
    // DASBOR STATISTIK ANGGOTA
    // ==========================================
    function buatBarAnggota<%=rnd%>(container, data) {
        if (!data || data.length === 0) { container.innerHTML = '<div class="text-muted small">Belum ada data.</div>'; return; }
        const maks = Math.max(...data.map(d => Number(d.jumlah) || 0)) || 1;
        container.innerHTML = data.map((d, i) => {
            const persen = Math.max(4, Math.round(((Number(d.jumlah) || 0) / maks) * 100));
            return '<div class="d-flex align-items-center gap-2 py-1" style="border-bottom:1px solid #f1f3f5;">'
                + '<div class="text-muted" style="width:16px;font-size:10px;font-weight:800;">' + (i + 1) + '</div>'
                + '<div class="flex-grow-1 text-truncate" style="font-size:12px;font-weight:600;">' + (d.label || '-') + '</div>'
                + '<div style="width:120px;height:6px;background:#f1f3f5;border-radius:99px;overflow:hidden;"><div style="width:' + persen + '%;height:100%;background:linear-gradient(90deg,#0d6efd,#7c3aed);"></div></div>'
                + '<div style="width:36px;text-align:right;font-size:11px;font-weight:800;">' + d.jumlah + '</div>'
                + '</div>';
        }).join('');
    }

    async function muatStatistikAnggota<%=rnd%>() {
        try {
            const kpi = await fetchData<%=rnd%>(
                "SELECT COUNT(*) total, COUNT(CASE WHEN COALESCE(a.aktif,true)=true THEN 1 END) aktif, "
                + "COUNT(CASE WHEN COALESCE(a.aktif,true)=false THEN 1 END) nonaktif, "
                + "COUNT(CASE WHEN COALESCE(j.wajib_pin,false)=true THEN 1 END) wajibpin "
                + "FROM koperasi.anggota_koperasi a LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id");
            if (kpi && kpi[0]) {
                const k = kpi[0];
                document.getElementById('saTotal<%=rnd%>').textContent = k.total || 0;
                document.getElementById('saAktif<%=rnd%>').textContent = k.aktif || 0;
                document.getElementById('saNonaktif<%=rnd%>').textContent = k.nonaktif || 0;
                document.getElementById('saWajibPin<%=rnd%>').textContent = k.wajibpin || 0;
            }

            const jenis = await fetchData<%=rnd%>(
                "SELECT COALESCE(j.nama,'Tanpa Jenis') label, COUNT(*) jumlah FROM koperasi.anggota_koperasi a "
                + "LEFT JOIN koperasi.jenis_anggota_koperasi j ON a.jenis_anggota_koperasi = j.id "
                + "GROUP BY label ORDER BY jumlah DESC LIMIT 8");
            buatBarAnggota<%=rnd%>(document.getElementById('saBarJenis<%=rnd%>'), jenis);
        } catch (e) {
            console.error('Gagal memuat statistik anggota:', e);
        }
    }
</script>