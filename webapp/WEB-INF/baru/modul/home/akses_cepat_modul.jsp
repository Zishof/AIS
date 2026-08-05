<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Tbmuser"%>
<%
// =============================================================================
// AKSES CEPAT MODUL — grid pintasan modul yang RAMAH & RESPONSIF.
// Daftar & gate peran MENGIKUTI index.zul + MainAction.setHeaderShortcutVisible(...)
// (baris ~1150-1179). Tiap modul mengarah ke padanan JSP /baru?p=<modul>.
// eMedic & Sister DILEWATI (belum ada modul JSP-nya).
// =============================================================================
Tbmuser tbmuserAkm = Common.getCurrentUser(request);
String ctxAkm = request.getContextPath();
String rndAkm = Common.getGeneratedBarCode(6);

if (tbmuserAkm != null) {
    Tbmrole rz = null;
    try { rz = tbmuserAkm.hakAkses(); } catch (Exception e) {}

    boolean adaSiswa = false, adaMhs = false;
    try { adaSiswa = tbmuserAkm.getSiswa() != null; } catch (Exception e) {}
    try { adaMhs = tbmuserAkm.getMahasiswa() != null; } catch (Exception e) {}

    boolean bFeeder = false, bSister = false;
    try { bFeeder = Common.getApakahAdminBolehAksesFeeder(); } catch (Exception e) {}
    try { bSister = Common.getApakahAdminBolehAksesSister(); } catch (Exception e) {}

    // Setiap entri: {label, ikon FontAwesome, warna hex, url p=}
    List<String[]> mods = new ArrayList<String[]>();
    if (rz != null) {
        if (Boolean.TRUE.equals(rz.getDashboard()))            mods.add(new String[]{"Akademik","fa-graduation-cap","#4f46e5","akademik"});
        if (Boolean.TRUE.equals(rz.getElearning()))            mods.add(new String[]{"e-Learning","fa-laptop-code","#0ea5e9","elearning"});
        if (Boolean.TRUE.equals(rz.getPustaka()))              mods.add(new String[]{"Pustaka","fa-book-open","#0d9488","pustaka"});
        if (Boolean.TRUE.equals(rz.getKegiatanDanPrestasi()))  mods.add(new String[]{"Prestasi","fa-trophy","#f59e0b","prestasi"});
        if (Boolean.TRUE.equals(rz.getKalenderAkademik()))     mods.add(new String[]{"Kalender Akademik","fa-calendar-days","#6366f1","kalenderakademik"});
        if (Boolean.TRUE.equals(rz.getInfoKegiatan()))         mods.add(new String[]{"Info Kegiatan","fa-bullhorn","#ec4899","infokegiatan"});
        if (Boolean.TRUE.equals(rz.getKepegawaian()))          mods.add(new String[]{"Kepegawaian","fa-user-tie","#8b5cf6","kepegawaian"});
        if (Boolean.TRUE.equals(rz.getPresensiKehadiran()))    mods.add(new String[]{"Presensi","fa-fingerprint","#14b8a6","presensi"});
        if (Boolean.TRUE.equals(rz.getKinerja()))              mods.add(new String[]{"Kinerja","fa-chart-line","#f97316","kinerja"});
        if (Boolean.TRUE.equals(rz.getTampilkanGaji()))        mods.add(new String[]{"Gaji","fa-money-bill-wave","#22c55e","gaji"});
        if (Boolean.TRUE.equals(rz.getPembayaran()))           mods.add(new String[]{"Pembayaran","fa-credit-card","#10b981","pembayaran"});
        if (Boolean.TRUE.equals(rz.getKeuangan()))             mods.add(new String[]{"Keuangan","fa-coins","#eab308","keuangan"});
        if (Boolean.TRUE.equals(rz.getAkunting()) && !adaSiswa && !adaMhs)
                                                               mods.add(new String[]{"Akuntansi","fa-calculator","#3b82f6","akuntansi"});
        if (Boolean.TRUE.equals(rz.getPengadaan()))            mods.add(new String[]{"Pengadaan","fa-boxes-stacked","#f43f5e","pengadaan"});
        if (Boolean.TRUE.equals(rz.getKantin()))               mods.add(new String[]{"Kantin / Toko","fa-utensils","#f97316","kantin"});
        if (Boolean.TRUE.equals(rz.getDashboardKoperasi()))    mods.add(new String[]{"Koperasi","fa-handshake","#06b6d4","koperasi"});
        if (Boolean.TRUE.equals(rz.getAdministrasi()))         mods.add(new String[]{"Surat Menyurat","fa-envelope-open-text","#7c3aed","suratmenyurat"});
        if (Boolean.TRUE.equals(rz.getWorkflow()))             mods.add(new String[]{"Workflow","fa-diagram-project","#2563eb","pagesmastersoppengajuananda"});
        if (Boolean.TRUE.equals(rz.getDasborRepository()))     mods.add(new String[]{"Repository","fa-folder-open","#64748b","repository"});
        if (Boolean.TRUE.equals(rz.getDasboardAntarJemput()))  mods.add(new String[]{"Antar Jemput","fa-bus","#0891b2","antarjemput"});
        if (Boolean.TRUE.equals(rz.getTampilkanSpmi()))        mods.add(new String[]{"SPMI","fa-clipboard-check","#84cc16","spmi"});
    }
    if (bFeeder) mods.add(new String[]{"Neo Feeder","fa-cloud-arrow-up","#0284c7","feeder"});
    // Sister & eMedic: belum ada modul JSP → tidak ditampilkan.

    if (!mods.isEmpty()) {
%>
<style>
    .akm-<%=rndAkm%>-wrap { border:none; border-radius:16px; }
    .akm-<%=rndAkm%>-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; }
    @media (min-width:576px){ .akm-<%=rndAkm%>-grid{ grid-template-columns:repeat(4,minmax(0,1fr)); } }
    @media (min-width:768px){ .akm-<%=rndAkm%>-grid{ grid-template-columns:repeat(5,minmax(0,1fr)); } }
    @media (min-width:1200px){ .akm-<%=rndAkm%>-grid{ grid-template-columns:repeat(6,minmax(0,1fr)); } }
    .akm-<%=rndAkm%>-tile {
        display:flex; flex-direction:column; align-items:center; justify-content:flex-start;
        gap:8px; padding:14px 6px; border-radius:14px; background:#fff; border:1px solid #eef1f6;
        text-decoration:none; cursor:pointer; transition:transform .18s ease, box-shadow .18s ease, border-color .18s ease;
        min-height:100%;
    }
    .akm-<%=rndAkm%>-tile:hover { transform:translateY(-4px); box-shadow:0 10px 22px rgba(15,23,42,.12); border-color:transparent; }
    .akm-<%=rndAkm%>-tile:active { transform:translateY(-1px); }
    .akm-<%=rndAkm%>-ic {
        width:46px; height:46px; border-radius:13px; display:flex; align-items:center; justify-content:center;
        color:#fff; font-size:19px; flex-shrink:0; box-shadow:0 6px 14px rgba(15,23,42,.15);
    }
    .akm-<%=rndAkm%>-lbl { font-size:12px; font-weight:600; color:#334155; text-align:center; line-height:1.25; letter-spacing:.1px; }
    @media (max-width:400px){ .akm-<%=rndAkm%>-lbl{ font-size:11px; } .akm-<%=rndAkm%>-ic{ width:42px; height:42px; font-size:17px; } }
</style>

<div class="card shadow-sm mb-4 akm-<%=rndAkm%>-wrap">
    <div class="card-body p-3 p-md-4">
        <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center">
                <span class="d-inline-flex align-items-center justify-content-center me-2"
                      style="width:30px;height:30px;border-radius:9px;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;">
                    <i class="fas fa-th-large" style="font-size:13px;"></i>
                </span>
                <%=Common.getBahasaConfig("Akses Cepat Modul")%>
            </h6>
            <span class="badge rounded-pill text-bg-light border text-secondary fw-semibold"><%=mods.size()%> <%=Common.getBahasaConfig("Modul")%></span>
        </div>

        <div class="akm-<%=rndAkm%>-grid">
            <% for (String[] m : mods) {
                 String label = m[0]; String ikon = m[1]; String warna = m[2]; String pTarget = m[3];
                 String url = ctxAkm + "/baru?p=" + pTarget;
            %>
            <a class="akm-<%=rndAkm%>-tile" href="<%=url%>" title="<%=Common.getBahasaConfig(label)%>">
                <span class="akm-<%=rndAkm%>-ic" style="background:linear-gradient(135deg,<%=warna%>,<%=warna%>cc);">
                    <i class="fas <%=ikon%>"></i>
                </span>
                <span class="akm-<%=rndAkm%>-lbl"><%=Common.getBahasaConfig(label)%></span>
            </a>
            <% } %>
        </div>
    </div>
</div>
<%
    } // end !mods.isEmpty
} // end tbmuser != null
%>
