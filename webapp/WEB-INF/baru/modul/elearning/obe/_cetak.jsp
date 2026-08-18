<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.obe.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%
String rnd = Common.getGeneratedBarCode(7);
String idKpmStr = request.getParameter("kurikulumPunyaMatakuliah");

if (idKpmStr == null || idKpmStr.trim().isEmpty() || idKpmStr.equals("null")) return;

KurikulumPunyaMatakuliah kpm = null;
Session sess = HibernateUtil.openSession();

Matakuliah mk = null;
Kurikulum kur = null;
Jurusan jur = null;
Fakultas fak = null;

List<CapaianLulusan> listCpl = new ArrayList<CapaianLulusan>();
List<CapaianPembelajaranLulusan> listCpmk = new ArrayList<CapaianPembelajaranLulusan>();
List<BahanKajian> listBahan = new ArrayList<BahanKajian>();
List<ReferensiLulusan> listUtama = new ArrayList<ReferensiLulusan>();
List<ReferensiLulusan> listPendukung = new ArrayList<ReferensiLulusan>();
List<Matakuliah> listMkPrasyarat = new ArrayList<Matakuliah>();
TreeMap<Integer, Map> mapsRinci = new TreeMap<Integer, Map>();

try {
    kpm = (KurikulumPunyaMatakuliah) sess.get(KurikulumPunyaMatakuliah.class, Long.parseLong(idKpmStr));
    if (kpm != null) {
        mk = kpm.getMatakuliah();
        kur = kpm.getKurikulum();
        if (kur != null) jur = kur.getJurusan();
        if (jur != null) fak = jur.getFakultas();

        // 1. Ekstrak CPL
        HashSet<Long> longsCPL = new HashSet<Long>();
        if (mk != null && mk.getCapaianLulusan() != null && !mk.getCapaianLulusan().trim().isEmpty()) {
            for (String s : mk.getCapaianLulusan().split(",")) {
                if (!s.trim().isEmpty()) { try { longsCPL.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:47");} }
            }
        }
        if (!longsCPL.isEmpty()) {
            listCpl = ConstantValues.simpleList(sess.createCriteria(CapaianLulusan.class)
                .add(Restrictions.in("id", longsCPL)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), CapaianLulusan.class);
        }

        // 2. Ekstrak CPMK
        HashSet<Long> longsCPMK = new HashSet<Long>();
        if (mk != null && mk.getCapaianPembelajaranLulusan() != null && !mk.getCapaianPembelajaranLulusan().trim().isEmpty()) {
            for (String s : mk.getCapaianPembelajaranLulusan().split(",")) {
                if (!s.trim().isEmpty()) { try { longsCPMK.add(Long.parseLong(s.trim())); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:60");} }
            }
        }
        if (!longsCPMK.isEmpty()) {
            listCpmk = ConstantValues.simpleList(sess.createCriteria(CapaianPembelajaranLulusan.class)
                .add(Restrictions.in("id", longsCPMK)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), CapaianPembelajaranLulusan.class);
        }

        // 3. Ekstrak Rincian
        JSONObject jsonArrayKpm = new JSONObject();
        try {
            String rincianDataStr = kpm.getRincian() != null ? kpm.getRincian().trim() : "{}";
            if (rincianDataStr.startsWith("[")) {
                JSONArray tempArray = new JSONArray(rincianDataStr);
                for(int i = 0; i < tempArray.length(); i++) {
                    JSONObject item = tempArray.getJSONObject(i);
                    String key = item.optString("keyData", "key_" + System.currentTimeMillis() + "_" + i);
                    jsonArrayKpm.put(key, item);
                }
            } else {
                jsonArrayKpm = new JSONObject(rincianDataStr);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:83");}
        mapsRinci = kpm.populateRinci(jsonArrayKpm);

        // 4. Ekstrak Bahan Kajian
        if(mk != null && mk.getBahanKajian() != null && !mk.getBahanKajian().trim().isEmpty()) {
            for(String s : mk.getBahanKajian().split(",")) {
                if(!s.trim().isEmpty()) {
                    try { 
                        BahanKajian obj = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(), Long.parseLong(s.trim()));
                        if(obj != null) listBahan.add(obj); 
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:93");} 
                }
            }
        }
        
        // 5. Ekstrak Pustaka
        if(kpm.getPustaka() != null && !kpm.getPustaka().trim().isEmpty()) {
            for(String s : kpm.getPustaka().split(",")) {
                if(!s.trim().isEmpty()) {
                    try { 
                        ReferensiLulusan obj = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(), Long.parseLong(s.trim()));
                        if(obj != null) listUtama.add(obj); 
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:105");} 
                }
            }
        }
        if(kpm.getPustakaPendukung() != null && !kpm.getPustakaPendukung().trim().isEmpty()) {
            for(String s : kpm.getPustakaPendukung().split(",")) {
                if(!s.trim().isEmpty()) {
                    try { 
                        ReferensiLulusan obj = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(), Long.parseLong(s.trim()));
                        if(obj != null) listPendukung.add(obj); 
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:115");} 
                }
            }
        }

        // 6. MK Prasyarat (dari KPM)
        if(kpm.getMkPrasyarat() != null && !kpm.getMkPrasyarat().trim().isEmpty()) {
            for(String s : kpm.getMkPrasyarat().split(",")) {
                if(!s.trim().isEmpty()) {
                    try { 
                        Matakuliah obj = (Matakuliah) ConstantValues.ambil(Matakuliah.class.getName(), Long.parseLong(s.trim()));
                        if(obj != null) listMkPrasyarat.add(obj);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:127");} 
                }
            }
        }
    }
} finally {
    try { sess.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:133");}
    ais.common.ElearningSessionUtil.closeQuietly(sess);
}

if (kpm == null || mk == null) return;

String strUniversitas = "UNIVERSITAS";
String strFakultas = fak != null ? fak.getNama().toUpperCase() : "-";
String strProdi = jur != null ? jur.getNama() : "-";

String strTgl = kpm.getTanggalPenyusunan() != null ? Common.dateFormat1.get().format(kpm.getTanggalPenyusunan()) : "-";
String strRumpun = mk.getKelompokMatakuliah() != null ? mk.getKelompokMatakuliah().getNama() : "-";
String strSks = "T=" + (mk.getSksDiskusi()!=null?mk.getSksDiskusi():0) + " P=" + (mk.getSksPraktek()!=null?mk.getSksPraktek():0) + " (" + mk.getSks() + " SKS)";
String strSmst = kpm.getSemester() != null ? kpm.getSemester().toString() : "-";

String valPengembang = kpm.getPengembangRps() != null ? kpm.getPengembangRps().replaceAll("\n", "<br>") : "-";
String valKoordinator = kpm.getKoordinator() != null ? kpm.getKoordinator().replaceAll("\n", "<br>") : "-";
String valKaprodi = jur != null && jur.getKaprodi() != null ? jur.getKaprodi().getNama() : "-";

StringBuilder sbBahan = new StringBuilder();
for (int i=0; i<listBahan.size(); i++) sbBahan.append((i+1)).append(". ").append(listBahan.get(i).getNama()).append("<br>");

StringBuilder sbPusUtama = new StringBuilder("<b>Utama:</b><br>");
for (int i=0; i<listUtama.size(); i++) sbPusUtama.append((i+1)).append(". ").append(listUtama.get(i).getNama()).append("<br>");

StringBuilder sbPusPendukung = new StringBuilder("<br><b>Pendukung:</b><br>");
for (int i=0; i<listPendukung.size(); i++) sbPusPendukung.append((i+1)).append(". ").append(listPendukung.get(i).getNama()).append("<br>");

StringBuilder sbPrasyarat = new StringBuilder();
for (int i=0; i<listMkPrasyarat.size(); i++) {
    sbPrasyarat.append((i+1)).append(". ").append(listMkPrasyarat.get(i).getKode()).append(" - ").append(listMkPrasyarat.get(i).getNama()).append("<br>");
}

int totalSubCpmkRows = 0;
if (!mapsRinci.isEmpty()) {
    for (Map map : mapsRinci.values()) {
        JSONObject jsonObj = (JSONObject) map.get("jsonObject");
        if (jsonObj != null) {
            int jmlCpmk = jsonObj.isNull("jumlahCpmk") ? 1 : jsonObj.getInt("jumlahCpmk");
            for (int i = 1; i <= jmlCpmk; i++) {
                String de = jsonObj.isNull("sub_cpmk_des" + (i == 1 ? "" : i)) ? "" : jsonObj.getString("sub_cpmk_des" + (i == 1 ? "" : i));
                if (!de.isEmpty()) totalSubCpmkRows++;
            }
        }
    }
}
if (totalSubCpmkRows == 0) totalSubCpmkRows = 1; 

int cplRows = listCpl.isEmpty() ? 1 : listCpl.size();
int cpmkRows = listCpmk.isEmpty() ? 1 : listCpmk.size();

int totalRowSpanCapaian = 3 + cplRows + cpmkRows + totalSubCpmkRows;
%>

<style>
    /* Styling tampilan layar pratinjau RPS */
    .table-rps-preview { width: 100%; border-collapse: collapse; font-family: 'Times New Roman', Times, serif; font-size: 11pt; color: #000; }
    .table-rps-preview th, .table-rps-preview td { border: 1px solid #000; padding: 8px; vertical-align: top; }
    .table-rps-preview th { text-align: center; font-weight: bold; background-color: #f2f2f2; }
    .table-rps-preview .bg-light { background-color: #f2f2f2 !important; }
</style>

<div class="card border-0 shadow-sm rounded-4 mb-4 animate__animated animate__fadeIn">
    <div class="card-body p-4">
        
        <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
            <div>
                <h6 class="fw-bold text-danger mb-1"><i class="fas fa-file-pdf me-2"></i><%=Common.getBahasaConfig("Cetak Rencana Pembelajaran Semester")%></h6>
                <small class="text-muted"><%=Common.getBahasaConfig("Pratinjau format cetak PDF Rencana Pembelajaran Semester berdasarkan data OBE.")%></small>
            </div>
            
            <button type="button" class="btn btn-danger shadow-sm fw-bold px-4 rounded-pill" onclick="cetakDokumenRps<%=rnd%>()">
                <i class="fas fa-print me-2"></i><%=Common.getBahasaConfig("Cetak PDF")%>
            </button>
        </div>

        <div id="printAreaRps<%=rnd%>" class="bg-white text-dark">
            
            <table class="table-rps-preview mb-0">
                <tr>
                    <td colspan="6" class="text-center bg-white border-bottom-0 pb-4">
                        <h4 class="fw-bold mb-0 mt-2"><%=strUniversitas%></h4>
                        <h5 class="fw-bold mb-0"><%=strFakultas%></h5>
                        <h5 class="fw-bold mb-0"><%=Common.getBahasaConfig("PROGRAM STUDI")%> <%=strProdi.toUpperCase()%></h5>
                    </td>
                </tr>
                <tr>
                    <td colspan="6" class="text-center bg-light fw-bold fs-5 py-3">
                        <%=Common.getBahasaConfig("RENCANA PEMBELAJARAN SEMESTER")%>
                    </td>
                </tr>
                <tr class="bg-light fw-bold text-center">
                    <td style="width: 25%;"><%=Common.getBahasaConfig("MATA KULIAH (MK)")%></td>
                    <td style="width: 15%;"><%=Common.getBahasaConfig("KODE")%></td>
                    <td style="width: 15%;"><%=Common.getBahasaConfig("Rumpun MK")%></td>
                    <td style="width: 15%;"><%=Common.getBahasaConfig("BOBOT (sks)")%></td>
                    <td style="width: 15%;"><%=Common.getBahasaConfig("SEMESTER")%></td>
                    <td style="width: 15%;"><%=Common.getBahasaConfig("Tgl Penyusunan")%></td>
                </tr>
                <tr class="text-center">
                    <td class="fw-bold"><%=mk.getNama()%></td>
                    <td><%=mk.getKode()%></td>
                    <td><%=strRumpun%></td>
                    <td><%=strSks%></td>
                    <td><%=strSmst%></td>
                    <td><%=strTgl%></td>
                </tr>
                <tr>
                    <td rowspan="2" class="align-middle fw-bold text-center bg-light">
                        <%=Common.getBahasaConfig("Otorisasi")%>
                    </td>
                    <td colspan="2" class="text-center fw-bold bg-light"><%=Common.getBahasaConfig("Dosen Pengembang RPS")%></td>
                    <td colspan="2" class="text-center fw-bold bg-light"><%=Common.getBahasaConfig("Koordinator RMK")%></td>
                    <td class="text-center fw-bold bg-light"><%=Common.getBahasaConfig("Ketua Program Studi")%></td>
                </tr>
                <tr style="height: 60px; vertical-align: bottom;" class="text-center">
                    <td colspan="2"><u><%=valPengembang%></u></td>
                    <td colspan="2"><u><%=valKoordinator%></u></td>
                    <td><u><%=valKaprodi%></u></td>
                </tr>
            </table>

            <table class="table-rps-preview border-top-0 mt-0 mb-4">
                <tr>
                    <td rowspan="<%= totalRowSpanCapaian %>" style="width: 15%;" class="fw-bold bg-light border-top-0">
                        <%=Common.getBahasaConfig("Capaian Pembelajaran (CP)")%>
                    </td>
                    <td colspan="2" class="fw-bold bg-light border-top-0"><%=Common.getBahasaConfig("CPL-PROGRAM STUDI yang dibebankan pada MK")%></td>
                </tr>
                <% if(listCpl.isEmpty()) { %>
                    <tr><td style="width: 15%;" class="fw-bold text-center">-</td><td>-</td></tr>
                <% } else { %>
                    <% for(CapaianLulusan cpl : listCpl) { %>
                    <tr>
                        <td style="width: 15%;" class="fw-bold text-center"><%=cpl.getKode()%></td>
                        <td><%=cpl.getNama()%></td>
                    </tr>
                    <% } %>
                <% } %>
                
                <tr>
                    <td colspan="2" class="fw-bold bg-light"><%=Common.getBahasaConfig("Capaian Pembelajaran Mata Kuliah (CPMK)")%></td>
                </tr>
                <% if(listCpmk.isEmpty()) { %>
                    <tr><td class="fw-bold text-center">-</td><td>-</td></tr>
                <% } else { %>
                    <% for(CapaianPembelajaranLulusan cpmk : listCpmk) { %>
                    <tr>
                        <td class="fw-bold text-center"><%=cpmk.getKode()%></td>
                        <td><%=cpmk.getNama()%></td>
                    </tr>
                    <% } %>
                <% } %>
                
                <tr>
                    <td colspan="2" class="fw-bold bg-light"><%=Common.getBahasaConfig("Kemampuan akhir tiap tahapan belajar (Sub-CPMK)")%></td>
                </tr>
                <% 
                boolean hasSubCpmk = false;
                if(!mapsRinci.isEmpty()) {
                    int subCount = 1;
                    for (Map map : mapsRinci.values()) {
                        JSONObject jsonObj = (JSONObject) map.get("jsonObject");
                        if(jsonObj != null) {
                            int jmlCpmk = jsonObj.isNull("jumlahCpmk") ? 1 : jsonObj.getInt("jumlahCpmk");
                            for (int i = 1; i <= jmlCpmk; i++) {
                                String de = jsonObj.isNull("sub_cpmk_des" + (i == 1 ? "" : i)) ? "" : jsonObj.getString("sub_cpmk_des" + (i == 1 ? "" : i));
                                if (!de.isEmpty()) { 
                                    hasSubCpmk = true;
                %>
                                    <tr>
                                        <td class="fw-bold text-center">Sub-CPMK <%=subCount++%></td>
                                        <td><%=de%></td>
                                    </tr>
                <%              }
                            }
                        }
                    }
                }
                if (!hasSubCpmk) { %>
                    <tr><td class="fw-bold text-center">-</td><td>-</td></tr>
                <% } %>

                <tr>
                    <td class="fw-bold bg-light"><%=Common.getBahasaConfig("Deskripsi Singkat MK")%></td>
                    <td colspan="2"><%=mk.getDeskripsiPembelajaran() != null ? mk.getDeskripsiPembelajaran().replaceAll("\n", "<br>") : "-"%></td>
                </tr>
                <tr>
                    <td class="fw-bold bg-light"><%=Common.getBahasaConfig("Materi Pembelajaran")%></td>
                    <td colspan="2"><%=sbBahan.length() > 0 ? sbBahan.toString() : "-"%></td>
                </tr>
                <tr>
                    <td class="fw-bold bg-light"><%=Common.getBahasaConfig("Pustaka")%></td>
                    <td colspan="2">
                        <%=sbPusUtama.toString()%>
                        <%=sbPusPendukung.toString()%>
                    </td>
                </tr>
                <tr>
                    <td class="fw-bold bg-light"><%=Common.getBahasaConfig("Dosen Pengampu")%></td>
                    <td colspan="2"><%=valPengembang%></td>
                </tr>
                <tr>
                    <td class="fw-bold bg-light"><%=Common.getBahasaConfig("Matakuliah Syarat")%></td>
                    <td colspan="2"><%=sbPrasyarat.length() > 0 ? sbPrasyarat.toString() : "-"%></td>
                </tr>
            </table>

            <table class="table-rps-preview mt-4">
                <thead>
                    <tr>
                        <th rowspan="2" class="align-middle" style="width: 5%;"><%=Common.getBahasaConfig("Mg Ke-")%></th>
                        <th rowspan="2" class="align-middle" style="width: 15%;"><%=Common.getBahasaConfig("Kemampuan akhir tiap tahapan belajar (Sub-CPMK)")%></th>
                        <th colspan="2" class="align-middle"><%=Common.getBahasaConfig("Penilaian")%></th>
                        <th colspan="2" class="align-middle"><%=Common.getBahasaConfig("Bentuk Pembelajaran, Metode Pembelajaran, Penugasan Mahasiswa, [Estimasi Waktu]")%></th>
                        <th rowspan="2" class="align-middle" style="width: 15%;"><%=Common.getBahasaConfig("Materi Pembelajaran")%> [Pustaka]</th>
                        <th rowspan="2" class="align-middle" style="width: 5%;"><%=Common.getBahasaConfig("Bobot Penilaian (%)")%></th>
                    </tr>
                    <tr>
                        <th style="width: 15%;"><%=Common.getBahasaConfig("Indikator")%></th>
                        <th style="width: 15%;"><%=Common.getBahasaConfig("Kriteria & Teknik")%></th>
                        <th style="width: 15%;"><%=Common.getBahasaConfig("Luring (offline)")%></th>
                        <th style="width: 15%;"><%=Common.getBahasaConfig("Daring (online)")%></th>
                    </tr>
                    <tr class="bg-light text-center text-muted small">
                        <td>(1)</td><td>(2)</td><td>(3)</td><td>(4)</td><td>(5)</td><td>(6)</td><td>(7)</td><td>(8)</td>
                    </tr>
                </thead>
                <tbody>
                    <% if(mapsRinci.isEmpty()) { %>
                        <tr><td colspan="8" class="text-center p-4"><%=Common.getBahasaConfig("Rincian pertemuan belum tersedia.")%></td></tr>
                    <% } else { 
                        for (Map map : mapsRinci.values()) {
                            JSONObject jsonObj = (JSONObject) map.get("jsonObject");
                            if (jsonObj != null) {
                                int mulaiMgg = jsonObj.getInt("mulaiMingguKe");
                                int sampaiMgg = jsonObj.getInt("sampaiMingguKe");
                                int jmlCpmk = jsonObj.isNull("jumlahCpmk") ? 1 : jsonObj.getInt("jumlahCpmk");

                                StringBuilder sbSubCpmk = new StringBuilder();
                                for (int i = 1; i <= jmlCpmk; i++) {
                                    String de = jsonObj.isNull("sub_cpmk_des" + (i == 1 ? "" : i)) ? "" : jsonObj.getString("sub_cpmk_des" + (i == 1 ? "" : i));
                                    if (!de.isEmpty()) sbSubCpmk.append("&bull; ").append(de).append("<br>");
                                }

                                StringBuilder sbMateri = new StringBuilder();
                                JSONObject bahanKajians = jsonObj.isNull("bahanKajians") ? new JSONObject() : jsonObj.getJSONObject("bahanKajians");
                                Iterator<String> pus = bahanKajians.keys();
                                while (pus.hasNext()) {
                                    try {
                                        String idBahan = pus.next();
                                        JSONObject p = bahanKajians.isNull(idBahan) ? new JSONObject() : bahanKajians.getJSONObject(idBahan);
                                        sbMateri.append("- ").append(p.getString("nama")).append("<br>");
                                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:386");}
                                }

                                StringBuilder sbRef = new StringBuilder();
                                JSONObject pustakaUtamas = jsonObj.isNull("pustakaUtamas") ? new JSONObject() : jsonObj.getJSONObject("pustakaUtamas");
                                Iterator<String> pu = pustakaUtamas.keys();
                                while (pu.hasNext()) {
                                    try {
                                        String idBahan = pu.next();
                                        JSONObject p = pustakaUtamas.isNull(idBahan) ? new JSONObject() : pustakaUtamas.getJSONObject(idBahan);
                                        sbRef.append("[U] ").append(p.getString("nama")).append("<br>");
                                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:397");}
                                }
                                JSONObject pustakaPendukungs = jsonObj.isNull("pustakaPendukungs") ? new JSONObject() : jsonObj.getJSONObject("pustakaPendukungs");
                                Iterator<String> pd = pustakaPendukungs.keys();
                                while (pd.hasNext()) {
                                    try {
                                        String idBahan = pd.next();
                                        JSONObject p = pustakaPendukungs.isNull(idBahan) ? new JSONObject() : pustakaPendukungs.getJSONObject(idBahan);
                                        sbRef.append("[P] ").append(p.getString("nama")).append("<br>");
                                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/elearning/obe/_cetak.jsp:406");}
                                }
                    %>
                    <tr>
                        <td class="text-center fw-bold"><%= mulaiMgg == sampaiMgg ? mulaiMgg : (mulaiMgg + "-" + sampaiMgg) %></td>
                        <td><%=sbSubCpmk.toString()%></td>
                        <td><%=jsonObj.isNull("indikator") ? "-" : jsonObj.getString("indikator").replaceAll("\n", "<br>")%></td>
                        <td><%=jsonObj.isNull("teknikDanKriteria") ? "-" : jsonObj.getString("teknikDanKriteria").replaceAll("\n", "<br>")%></td>
                        <td>
                            <b><%=Common.getBahasaConfig("Bentuk/Metode:")%></b><br>
                            <%=jsonObj.isNull("metodePembelajaran") ? "-" : jsonObj.getString("metodePembelajaran").replaceAll("\n", "<br>")%><br><br>
                            <b><%=Common.getBahasaConfig("Aktivitas:")%></b><br>
                            <%=jsonObj.isNull("pembelajaranLuring") ? "-" : jsonObj.getString("pembelajaranLuring").replaceAll("\n", "<br>")%>
                        </td>
                        <td><%=jsonObj.isNull("pembelajaranDaring") ? "-" : jsonObj.getString("pembelajaranDaring").replaceAll("\n", "<br>")%></td>
                        <td>
                            <b><%=Common.getBahasaConfig("Materi:")%></b><br>
                            <%=sbMateri.length() > 0 ? sbMateri.toString() : "-"%>
                            <br><b><%=Common.getBahasaConfig("Pustaka:")%></b><br>
                            <%=sbRef.length() > 0 ? sbRef.toString() : "-"%>
                        </td>
                        <td class="text-center">-</td>
                    </tr>
                    <%      }
                        } 
                    } %>
                </tbody>
            </table>

        </div>
        </div>
</div>

<script>
    function cetakDokumenRps<%=rnd%>() {
        var printContents = document.getElementById('printAreaRps<%=rnd%>').innerHTML;
        
        var printWindow = window.open('', '', 'height=800,width=1200');
        printWindow.document.write('<html><head><title><%=Common.getBahasaConfig("Cetak RPS")%></title>');
        
        // Memasukkan CSS kustom agar sesuai dengan format PDF
        printWindow.document.write('<style>');
        printWindow.document.write('body { font-family: "Times New Roman", Times, serif; font-size: 11pt; color: #000; padding: 20px; }');
        printWindow.document.write('.table-rps-preview { width: 100%; border-collapse: collapse; margin-bottom: 20px; }');
        printWindow.document.write('.table-rps-preview th, .table-rps-preview td { border: 1px solid #000; padding: 8px; vertical-align: top; }');
        printWindow.document.write('.table-rps-preview th { text-align: center; font-weight: bold; background-color: #f2f2f2; }');
        printWindow.document.write('.bg-light { background-color: #f2f2f2 !important; -webkit-print-color-adjust: exact; color-adjust: exact; }');
        printWindow.document.write('.text-center { text-align: center; }');
        printWindow.document.write('.fw-bold { font-weight: bold; }');
        printWindow.document.write('.fs-5 { font-size: 1.25rem; }');
        printWindow.document.write('.py-3 { padding-top: 1rem; padding-bottom: 1rem; }');
        printWindow.document.write('.pb-4 { padding-bottom: 1.5rem; }');
        printWindow.document.write('.mb-0 { margin-bottom: 0; }');
        printWindow.document.write('.mb-4 { margin-bottom: 1.5rem; }');
        printWindow.document.write('.mt-0 { margin-top: 0; }');
        printWindow.document.write('.mt-2 { margin-top: 0.5rem; }');
        printWindow.document.write('.mt-4 { margin-top: 1.5rem; }');
        printWindow.document.write('.align-middle { vertical-align: middle; }');
        printWindow.document.write('.border-top-0 { border-top: 0 !important; }');
        printWindow.document.write('.border-bottom-0 { border-bottom: 0 !important; }');
        printWindow.document.write('.p-0 { padding: 0 !important; }');
        printWindow.document.write('.p-2 { padding: 0.5rem !important; }');
        printWindow.document.write('.p-4 { padding: 1.5rem !important; }');
        printWindow.document.write('.w-100 { width: 100% !important; }');
        printWindow.document.write('.h-100 { height: 100% !important; }');
        printWindow.document.write('.text-muted { color: #6c757d !important; }');
        printWindow.document.write('.small { font-size: 0.875em; }');
        printWindow.document.write('@page { size: A4 landscape; margin: 15mm; }');
        printWindow.document.write('</style>');
        
        printWindow.document.write('</head><body>');
        printWindow.document.write(printContents);
        printWindow.document.write('</body></html>');

        printWindow.document.close();
        printWindow.focus();
        
        // Memberi waktu kepada browser untuk merender gaya sebelum dialog cetak muncul
        setTimeout(function() {
            printWindow.print();
            printWindow.close();
        }, 250);
    }
</script>