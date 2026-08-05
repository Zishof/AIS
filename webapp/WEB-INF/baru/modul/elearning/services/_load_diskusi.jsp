<%@page import="ais.database.model.file.PertemuanFileContent"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Pertemuan"%>
<%@page import="ais.database.model.PertemuanPunyaDiskusi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.Guru"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
// 1. Keamanan dan Autentikasi
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) return;

// 2. Tangkap Parameter dari AJAX _diskusi.jsp
String random = request.getParameter("rnd"); // Random ID sinkronisasi UI
String idPertemuanParam = request.getParameter("idPertemuan");

if (idPertemuanParam == null || idPertemuanParam.trim().isEmpty() || random == null) return;

// 3. Identifikasi Peran Pengguna
Mahasiswa mahasiswa = tbmuser.getMahasiswa();
Dosen dosen = tbmuser.ambilDosen();
Siswa siswa = tbmuser.getSiswa();
Guru guru = tbmuser.ambilGuru();
boolean isSuperAdmin = Common.getApakahAdmin();

Pertemuan pertemuan = null;
boolean isKomentarDitutup = false;

// 4. Tarik Data Diskusi dari Database
Map<Long, List<PertemuanPunyaDiskusi>> mapBalasan = new HashMap<Long, List<PertemuanPunyaDiskusi>>();
List<PertemuanPunyaDiskusi> listDiskusiUtama = new ArrayList<PertemuanPunyaDiskusi>();
Session mySession = null;

try {
    mySession = HibernateUtil.openSession();
    Long idPertemuan = Long.parseLong(idPertemuanParam.trim());
    pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idPertemuanParam.trim(), true);
    
    if (pertemuan != null) {
        isKomentarDitutup = pertemuan.getKomentarDitutup() != null && pertemuan.getKomentarDitutup();

        TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();
        pertemuansa.put(idPertemuan.toString(), idPertemuan);
        
        boolean dosenBol = (dosen != null);
        boolean mahasiswaBol = (mahasiswa != null);
        boolean guruBol = (guru != null);
        boolean siswaBol = (siswa != null);
        boolean adminBol = isSuperAdmin;

        TreeMap<String, Object[]> dataMateriKomentar = PertemuanFileContent.ambilKomentar(
            pertemuansa, false, dosenBol, mahasiswaBol, guruBol, siswaBol, adminBol, "", null
        );

        if (dataMateriKomentar != null) {
            Collection<Object[]> dataMateriValuesKomentar = dataMateriKomentar.values();
            for (Object[] objects : dataMateriValuesKomentar) {
                if (objects != null && objects.length > 0) {
                    Long diskusiId = (Long) objects[0];
                    PertemuanPunyaDiskusi diskusi = (PertemuanPunyaDiskusi) GeneralValueObject.ambilData(PertemuanPunyaDiskusi.class, diskusiId.toString());
                    
                    if (diskusi != null) {
                        if (diskusi.getParent() == null) {
                            listDiskusiUtama.add(diskusi);
                        } else {
                            Long parentId = diskusi.getParent().getId();
                            if (!mapBalasan.containsKey(parentId)) {
                                mapBalasan.put(parentId, new ArrayList<PertemuanPunyaDiskusi>());
                            }
                            mapBalasan.get(parentId).add(diskusi);
                        }
                    }
                }
            }
        }
        
        Comparator<PertemuanPunyaDiskusi> ascComparator = new Comparator<PertemuanPunyaDiskusi>() {
            public int compare(PertemuanPunyaDiskusi o1, PertemuanPunyaDiskusi o2) {
                return o1.getId().compareTo(o2.getId());
            }
        };
        Collections.sort(listDiskusiUtama, ascComparator);
        for (List<PertemuanPunyaDiskusi> balasanList : mapBalasan.values()) {
            Collections.sort(balasanList, ascComparator);
        }
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_load_diskusi.jsp:109");
    out.println("<div class='text-center py-4 text-danger'><i class='fas fa-exclamation-triangle fa-2x mb-2 d-block'></i>Gagal memuat data diskusi</div>");
    return;
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

if (pertemuan == null) return;

SimpleDateFormat sdfWaktu = new SimpleDateFormat("dd MMM yyyy, HH:mm");

class FotoHelper {
    public String getUrlFoto(PertemuanPunyaDiskusi ppd) {
        Tbmuser target = null;
        if (ppd.getMahasiswa() != null) target = new Tbmuser(ppd.getMahasiswa());
        else if (ppd.getSiswa() != null) target = new Tbmuser(ppd.getSiswa());
        else if (ppd.getBiodataCalonMahasiswa() != null) {
            target = ppd.getBiodataCalonMahasiswa().getMahasiswa() != null ? new Tbmuser(ppd.getBiodataCalonMahasiswa().getMahasiswa()) : new Tbmuser(ppd.getBiodataCalonMahasiswa());
        }
        else if (ppd.getCalonSiswa() != null) target = new Tbmuser(ppd.getCalonSiswa());
        else if (ppd.getDosen() != null) target = new Tbmuser(ppd.getDosen());
        else if (ppd.getGuru() != null) target = new Tbmuser(ppd.getGuru());
        else if (ppd.getTbmuser() != null) target = ppd.getTbmuser();
        return target != null ? CommonMedia.getUrlFotoPengguna(target) : Common.ROOT + "/images/default_user.png";
    }
}
FotoHelper fotoHelper = new FotoHelper();

// 5. RENDER HTML FRAGMENT DAFTAR DISKUSI
if (listDiskusiUtama.isEmpty()) { 
%>
    <div class="text-center py-5">
        <i class="fas fa-comments-dollar fs-1 text-secondary mb-3 d-block"></i>
        <h6 class="fw-bold text-muted"><%= Common.getBahasaConfig("Belum ada diskusi yang tercatat untuk pertemuan ini.") %></h6>
    </div>
<% } else { 
    for (PertemuanPunyaDiskusi diskusi : listDiskusiUtama) { 
%>
        <div class="komentar-induk-<%=random%>" id="komentar-<%=diskusi.getId()%>" style="transition: opacity 0.4s ease;">
            <div class="d-flex align-items-start gap-3">
                <img src="<%= fotoHelper.getUrlFoto(diskusi) %>" class="avatar-bulat-<%=random%> shadow-sm" alt="Foto" onclick="tampilGambar(this)">
                
                <div class="flex-grow-1 w-100">
                    <% 
                    String oleh = "";
                    if (diskusi.getBiodataCalonMahasiswa() != null) oleh = diskusi.getBiodataCalonMahasiswa().getNama() + " (" + Common.getBahasaConfig("Calon Mahasiswa") + ")";
                    else if (diskusi.getMahasiswa() != null) oleh = diskusi.getMahasiswa().getNama() + " (" + Common.getBahasaConfig("Mahasiswa") + ")";
                    else if (diskusi.getSiswa() != null) oleh = diskusi.getSiswa().getNama() + " (" + Common.getBahasaConfig("Siswa") + ")";
                    else if (diskusi.getCalonSiswa() != null) oleh = diskusi.getCalonSiswa().getNama() + " (" + Common.getBahasaConfig("Calon Siswa") + ")";
                    else if (diskusi.getDosen() != null) oleh = diskusi.getDosen().getNama() + " (" + Common.getBahasaConfig("Dosen") + ")";
                    else if (diskusi.getGuru() != null) oleh = diskusi.getGuru().getNama() + " (" + Common.getBahasaConfig("Guru") + ")";
                    else if (diskusi.getTbmuser() != null) { Tbmrole rDsk = diskusi.getTbmuser().hakAkses(); oleh = diskusi.getTbmuser().getUserNama() + (rDsk != null ? " (" + rDsk.getRoleName() + ")" : ""); }
                    
                    String waktuDiskusi = diskusi.getTanggal_dirubah() != null ? sdfWaktu.format(diskusi.getTanggal_dirubah()) : "-";
                    String isiBersih = diskusi.getIsi() != null ? diskusi.getIsi().replaceAll("\n", "<br>") : "";
                    List<String> urls = Common.getUrls(isiBersih);
                    for (String u : urls) {
                        isiBersih = StringUtils.replace(isiBersih, u, "<a href='" + u + "' target='_blank' style='color:#0000EE; text-decoration:underline;'>" + u + "</a>");
                    }
                    %>
                    
                    <div class="gelembung-komentar-<%=random%> w-100">
                        <div class="header-gelembung-<%=random%>"><%= oleh %> - <%= waktuDiskusi %></div>
                        <%= isiBersih %>
                    </div>
                    
                    <div id="wadahLampiran_<%=random%>_<%=diskusi.getId()%>" class="w-100 mb-2"></div>
                    
                    <% 
                    boolean isOwner = tbmuser.getUserId().equals(diskusi.getTbmuser() != null ? diskusi.getTbmuser().getUserId() : "");
                    boolean bolehHapus = isOwner || isSuperAdmin || (dosen != null && dosen.getId() != null);
                    %>
                    
                    <div class="d-flex gap-3 mt-1 ps-2">
                        <% if (!isKomentarDitutup) { %>
                            <button type="button" class="btn-aksi-diskusi-<%=random%>" onclick="tampilFormBalasan<%=random%>('<%=diskusi.getId()%>')"><%= Common.getBahasaConfig("Balas") %></button>
                        <% } %>
                        <% if (bolehHapus) { %>
                            <button type="button" class="btn-aksi-diskusi-<%=random%> text-danger" onclick="hapusKomentar<%=random%>('<%=diskusi.getId()%>')"><%= Common.getBahasaConfig("Hapus") %></button>
                        <% } %>
                    </div>

                    <div class="mt-3 p-2 bg-white border rounded-3 shadow-sm d-none form-balasan-<%=random%> w-100" id="formBalasan-<%=diskusi.getId()%>">
                        <textarea class="form-control border-secondary-subtle mb-2 shadow-sm rounded-3 input-balasan-<%=random%>" rows="2" placeholder="<%= Common.getBahasaConfig("Tuliskan balasan Anda di sini...") %>" style="border: 1px solid #9fb8bf; resize: none; font-size: 12px;"></textarea>
                        <div class="d-flex justify-content-end gap-2 mt-2">
                            <button type="button" class="btn btn-sm btn-outline-secondary px-3" style="font-size: 10px;" onclick="tampilFormBalasan<%=random%>('<%=diskusi.getId()%>')"><%= Common.getBahasaConfig("Batal") %></button>
                            <button type="button" class="btn btn-sm btn-primary px-3 shadow-sm" style="font-size: 10px;" onclick="simpanKomentar<%=random%>(this, '<%=diskusi.getId()%>')"><%= Common.getBahasaConfig("Kirim") %></button>
                        </div>
                    </div>
                </div>
            </div>
            
            <% if (mapBalasan.containsKey(diskusi.getId())) { %>
                <div class="garis-balasan-<%=random%> mt-3 d-flex flex-column gap-3 w-100">
                    <% for (PertemuanPunyaDiskusi balasan : mapBalasan.get(diskusi.getId())) { %>
                        <div class="d-flex align-items-start gap-3 w-100" id="komentar-<%=balasan.getId()%>" style="transition: opacity 0.4s ease;">
                            <img src="<%= fotoHelper.getUrlFoto(balasan) %>" class="avatar-bulat-kecil-<%=random%> shadow-sm" alt="Foto" onclick="tampilGambar(this)">
                            
                            <div class="flex-grow-1 w-100">
                                <% 
                                String olehB = "";
                                if (balasan.getBiodataCalonMahasiswa() != null) olehB = balasan.getBiodataCalonMahasiswa().getNama() + " (" + Common.getBahasaConfig("Calon Mahasiswa") + ")";
                                else if (balasan.getMahasiswa() != null) olehB = balasan.getMahasiswa().getNama() + " (" + Common.getBahasaConfig("Mahasiswa") + ")";
                                else if (balasan.getSiswa() != null) olehB = balasan.getSiswa().getNama() + " (" + Common.getBahasaConfig("Siswa") + ")";
                                else if (balasan.getCalonSiswa() != null) olehB = balasan.getCalonSiswa().getNama() + " (" + Common.getBahasaConfig("Calon Siswa") + ")";
                                else if (balasan.getDosen() != null) olehB = balasan.getDosen().getNama() + " (" + Common.getBahasaConfig("Dosen") + ")";
                                else if (balasan.getGuru() != null) olehB = balasan.getGuru().getNama() + " (" + Common.getBahasaConfig("Guru") + ")";
                                else if (balasan.getTbmuser() != null) { Tbmrole rBal = balasan.getTbmuser().hakAkses(); olehB = balasan.getTbmuser().getUserNama() + (rBal != null ? " (" + rBal.getRoleName() + ")" : ""); }
                                String waktuBalasan = balasan.getTanggal_dirubah() != null ? sdfWaktu.format(balasan.getTanggal_dirubah()) : "-";
                                
                                String isiB = balasan.getIsi() != null ? balasan.getIsi().replaceAll("\n", "<br>") : "";
                                List<String> urlsB = Common.getUrls(isiB);
                                for (String u : urlsB) {
                                    isiB = StringUtils.replace(isiB, u, "<a href='" + u + "' target='_blank' style='color:#0000EE; text-decoration:underline;'>" + u + "</a>");
                                }
                                %>
                                
                                <div class="gelembung-komentar-<%=random%> w-100">
                                    <div class="header-gelembung-<%=random%>"><%= olehB %> - <%= waktuBalasan %></div>
                                    <%= isiB %>
                                </div>
                                
                                <div id="wadahLampiran_<%=random%>_<%=balasan.getId()%>" class="w-100 mb-1"></div>
                                
                                <% 
                                boolean isOwnerBalasan = tbmuser.getUserId().equals(balasan.getTbmuser() != null ? balasan.getTbmuser().getUserId() : "");
                                boolean bolehHapusBalasan = isOwnerBalasan || isSuperAdmin || (dosen != null && dosen.getId() != null);
                                if (bolehHapusBalasan) { 
                                %>
                                <div class="d-flex mt-1 ps-2">
                                    <button type="button" class="btn-aksi-diskusi-<%=random%> text-danger" onclick="hapusKomentar<%=random%>('<%=balasan.getId()%>')"><%= Common.getBahasaConfig("Hapus") %></button>
                                </div>
                                <% } %>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>
<%  }
} 
%>