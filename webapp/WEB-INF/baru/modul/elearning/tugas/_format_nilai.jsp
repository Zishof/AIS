<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.database.model.*"%>
<%@ page import="ais.database.model.sekolah.*"%>

<%
    String idTugasStr = request.getParameter("idTugas");
    String jenis = request.getParameter("jenis"); // Pertemuan / TugasPertemuan
    
    if (idTugasStr == null || idTugasStr.isEmpty()) {
        out.println("<div class='alert alert-danger'>Tugas tidak ditemukan.</div>");
        return;
    }
    
    Long idTugas = Long.parseLong(idTugasStr);
    Session sess = HibernateUtil.openSession();
    // try/finally membungkus seluruh sisa halaman karena sess dipakai untuk
    // lazy-load saat render HTML; ditutup di finally pada akhir file.
    try {

    // 1. Ambil Objek Tugas
    Tugas tugas = null;
    if ("TugasPertemuan".equalsIgnoreCase(jenis)) {
        tugas = (Tugas) sess.get(TugasPertemuan.class, idTugas);
    } else {
        tugas = (Tugas) sess.get(Pertemuan.class, idTugas);
    }

    if (tugas == null) return;
    
    // 2. Tentukan Induk (Perkuliahan atau JadwalPelajaran)
    Perkuliahan perkuliahan = null;
    JadwalPelajaran jadwalPelajaran = null;
    
    if (tugas instanceof Pertemuan) {
        perkuliahan = ((Pertemuan) tugas).getPerkuliahan();
        jadwalPelajaran = ((Pertemuan) tugas).getJadwalPelajaran();
    } else if (tugas instanceof TugasPertemuan) {
        Pertemuan pp = ((TugasPertemuan) tugas).ambilPertemuan();
        if(pp != null) {
            perkuliahan = pp.getPerkuliahan();
            jadwalPelajaran = pp.getJadwalPelajaran();
        }
    }
%>

<div class="container-fluid p-3">
    <form id="formSimpanFormatNilaiTugas" method="post" action="SimpanFormatNilaiTugasServlet">
        <input type="hidden" name="idTugas" value="<%= idTugas %>" />
        <input type="hidden" name="jenisTugas" value="<%= jenis %>" />
        
        <div class="alert alert-info">
            <i class="fa fa-info-circle me-2"></i> <strong>Nilai masuk ke format penilaian:</strong> Tentukan bobot atau CPMK dari tugas ini.
        </div>

        <% if (perkuliahan != null) { 
            List<FormatNilai> formatNilais = Common.getFormatNilais(sess, perkuliahan);
            boolean isObe = (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap()));
            
            if (isObe) {
                // OBE Logic: Multi-Select CPMK / Sub-CPMK
                JSONObject jsonObe = tugas.getFormatNilais() != null && !tugas.getFormatNilais().isEmpty() ? new JSONObject(tugas.getFormatNilais()) : new JSONObject();
        %>
                <table class="table table-bordered table-striped">
                    <thead class="bg-primary text-white">
                        <tr>
                            <th><%= Common.getBahasaConfig("Pilih") %></th>
                            <th><%= Common.getBahasaConfig("Sub-CPMK") %></th>
                            <th width="150px"><%= Common.getBahasaConfig("Bobot (%)") %></th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (FormatNilai nilai : formatNilais) { 
                             if (nilai.getStatusPertemuan() != null) { 
                                 boolean isChecked = !jsonObe.isNull(nilai.getId().toString());
                                 double bobotVal = isChecked ? jsonObe.getDouble(nilai.getId().toString()) : 100.0;
                        %>
                        <tr>
                            <td class="text-center">
                                <input type="checkbox" name="chkFormatNilai" value="<%= nilai.getId() %>" <%= isChecked ? "checked" : "" %> />
                            </td>
                            <td><%= nilai.getNama() %> (<%= Common.numberFormat.get().format(nilai.getPersen()) %>%)</td>
                            <td>
                                <input type="number" step="0.1" class="form-control" name="bobot_<%= nilai.getId() %>" value="<%= bobotVal %>" />
                            </td>
                        </tr>
                        <%   } 
                           } %>
                    </tbody>
                </table>
        <%  } else { 
                // NON-OBE Logic: Single Select
        %>
                <div class="mb-3">
                    <label class="form-label fw-bold"><%= Common.getBahasaConfig("Pilih Kategori Nilai:") %></label>
                    <select class="form-select" name="formatNilaiId">
                        <option value="">-- Tidak Ada --</option>
                        <% for (FormatNilai nilai : formatNilais) { 
                             if (nilai.getStatusPertemuan() != null) { 
                                 String sel = (tugas.getFormatNilai() != null && tugas.getFormatNilai().getId().equals(nilai.getId())) ? "selected" : "";
                        %>
                            <option value="<%= nilai.getId() %>" <%= sel %>><%= nilai.getNama() %> (<%= Common.numberFormat.get().format(nilai.getPersen()) %>%)</option>
                        <%   } 
                           } %>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold"><%= Common.getBahasaConfig("Dengan bobot sebesar:") %></label>
                    <input type="number" step="0.1" class="form-control w-25" name="prosentaseNilai" value="<%= tugas.getProsentase() != null ? tugas.getProsentase() : "0.0" %>" />
                </div>
        <%  } %>

        <% } else if (jadwalPelajaran != null) { 
             // Terapkan render dropdown sesuai query `DetailGrupKategoriItemPenilaianSiswa` Anda di sini
             // (Logic diringkas karena strukturnya cukup panjang di java, Anda bisa membangun iterasi Comboitem-nya di backend / servlet atau di sini)
        %>
             <div class="mb-3">
                 <label class="form-label fw-bold"><%= Common.getBahasaConfig("Format Penilaian Siswa:") %></label>
                 <select class="form-select" name="jenisItemPenilaianId">
                      <option value="">-- Pilih Jenis Penilaian --</option>
                      </select>
             </div>
        <% } %>

        <hr>
        <div class="text-end">
            <button type="button" class="btn btn-secondary" onclick="closeModal()"><%= Common.getBahasaConfig("Batal") %></button>
            <button type="submit" class="btn btn-success">
                <i class="fa fa-save"></i> Simpan Format Nilai
            </button>
        </div>
    </form>
</div>
<%
    } finally {
        HibernateUtil.closeSessionQuietly(sess);
    }
%>