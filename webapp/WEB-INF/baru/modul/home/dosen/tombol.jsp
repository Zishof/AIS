<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Dosen"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.ambilDosen() == null) return;
    Dosen dosen = tbmuser.ambilDosen();
    String rnd = java.util.UUID.randomUUID().toString().replace("-","").substring(0,8);
    String dosenId = dosen.getId() != null ? dosen.getId().toString() : "";
    String root = Common.ROOT;
%>

<style>
    .dosen-shortcut-btn .btn {
        border-radius: 8px;
        transition: all .2s ease-in-out;
        font-weight: 500;
        box-shadow: 0 2px 4px rgba(0,0,0,.05);
    }
    .dosen-shortcut-btn .btn:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,.1); }
</style>

<div class="card shadow-sm border-0 rounded-4 mb-4 animate__animated animate__fadeInUp">
    <div class="card-header bg-white border-bottom-0 pt-4 pb-2 rounded-top-4">
        <h5 class="mb-0 text-dark fw-bold">
            <i class="fas fa-link text-primary me-2"></i><%=Common.getBahasaConfig("Pintasan Akademik Dosen")%>
        </h5>
    </div>
    <div class="card-body">
        <div class="d-flex flex-wrap gap-2 dosen-shortcut-btn align-items-center">

            <%-- Kelompok akademik --%>
            <button class="btn btn-outline-primary btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasterperkuliahanzul&s=index')">
                <i class="fas fa-chalkboard me-1"></i><%=Common.getBahasaConfig("Perkuliahan Saya")%>
            </button>

            <button class="btn btn-outline-success btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasterperkuliahanzul&s=laporan_per_dosen')">
                <i class="fas fa-calendar-alt me-1"></i><%=Common.getBahasaConfig("Jadwal Mengajar")%>
            </button>

            <button class="btn btn-outline-info btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasterpenilaianmahasiswazul&s=index')">
                <i class="far fa-check-circle me-1"></i><%=Common.getBahasaConfig("Input Nilai")%>
            </button>

            <button class="btn btn-outline-info btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasterpresensiperkuliahanzul&s=index')">
                <i class="fas fa-fingerprint me-1"></i><%=Common.getBahasaConfig("Input Kehadiran")%>
            </button>

            <button class="btn btn-outline-secondary btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasteradvisorymahasiswazul&s=index')">
                <i class="fas fa-user-friends me-1"></i><%=Common.getBahasaConfig("Pembimbingan PA")%>
            </button>

            <button class="btn btn-outline-secondary btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasterskripsiataupenelitianzul&s=index')">
                <i class="fas fa-graduation-cap me-1"></i><%=Common.getBahasaConfig("Bimbingan TA/Skripsi")%>
            </button>

            <div class="vr mx-2 d-none d-md-block" style="height:30px"></div>

            <%-- Laporan / Cetak --%>
            <button class="btn btn-primary btn-sm px-3" type="button"
                    onclick="bukaLaporanDosen_<%=rnd%>('Biodata_Dosen', 'Daftar Riwayat Hidup')">
                <i class="fas fa-print me-1"></i><%=Common.getBahasaConfig("Cetak DRH")%>
            </button>

            <button class="btn btn-primary btn-sm px-3" type="button"
                    onclick="bukaLaporanDosen_<%=rnd%>('Indeks_Prestasi_Dosen', 'Indeks Prestasi Dosen')">
                <i class="fas fa-chart-line me-1"></i><%=Common.getBahasaConfig("Indeks Prestasi")%>
            </button>

            <button class="btn btn-primary btn-sm px-3" type="button"
                    onclick="bukaLaporanDosen_<%=rnd%>('Rekap_Kehadiran_Dosen', 'Rekap Kehadiran')">
                <i class="fas fa-clipboard-list me-1"></i><%=Common.getBahasaConfig("Rekap Kehadiran")%>
            </button>

            <button class="btn btn-outline-primary btn-sm px-3" type="button"
                    onclick="bukaModulDosen_<%=rnd%>('<%=root%>/baru?p=pagesmasterangketpenilaianolehzul&s=index')">
                <i class="fas fa-star me-1"></i><%=Common.getBahasaConfig("Angket Penilaian")%>
            </button>
        </div>
    </div>
</div>

<script>
    function bukaModulDosen_<%=rnd%>(url) {
        window.location.href = url;
    }

    function bukaLaporanDosen_<%=rnd%>(namaFile, judulLaporan) {
        var url = '<%=root%>/baru?hanya_tampil_jsp=true&p=laporan%2Fgeneral_dosen&s=index'
                + '&dosen=<%=dosenId%>&fileLaporan=' + namaFile
                + '&judulLaporan=' + encodeURIComponent(judulLaporan);
        if (typeof loadModalContentCustomSimpan === 'function') {
            loadModalContentCustomSimpan(judulLaporan, url, '85%', null, '', 'modalLaporanDosen_<%=rnd%>');
        } else {
            window.open(url, '_blank');
        }
    }
</script>
