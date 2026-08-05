<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%
Long lg = 0L;
boolean pt = false;
boolean ya = false;

try {
    lg = Long.parseLong(Common.getGeneratedAngkaDigit(5));
    Tbmuser tbmuser = Common.getCurrentUser(request);
    
    if (tbmuser != null) {
        boolean[] ptYa = Common.chekPtAtauSekolah(tbmuser);
        pt = ptYa[0];
        ya = ptYa[1];
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/home/admin.jsp:18");
} finally {
    // Blok finally untuk memastikan penutupan session atau pelepasan resource (jika ada pemanggilan koneksi database secara langsung).
}
%>

<style>
    .kartu-elevasi-<%=lg%> { transition: transform 0.3s ease, box-shadow 0.3s ease; border: none; border-radius: 10px; }
    .kartu-elevasi-<%=lg%>:hover { transform: translateY(-4px); box-shadow: 0 0.5rem 1rem rgba(0,0,0,0.15) !important; }
    .indikator-muat-<%=lg%> { padding: 2rem 0; color: #6c757d; font-weight: 500; text-align: center; }
</style>

<div class="row g-3">
    <div class="col-12">
        <div class="card mb-3 shadow-sm kartu-elevasi-<%=lg%>">
            <div class="bg-holder d-none d-lg-block bg-card" style="background-image:url(<%=request.getContextPath()%>/img/corner-2.png);"></div>
            <div class="card-body position-relative">
                <div class="row w-100" id="wadah_1_<%=lg%>">
                    <div class="col-12 indikator-muat-<%=lg%>"><i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sedang memuat data...")%></div>
                </div>
            </div>
        </div>
    </div>

    <div class="col-12">
        <div class="card mb-3 shadow-sm kartu-elevasi-<%=lg%>">
            <div class="bg-holder d-none d-lg-block bg-card" style="background-image:url(<%=request.getContextPath()%>/img/corner-3.png);"></div>
            <div class="card-body position-relative">
                <div class="row w-100" id="wadah_2_<%=lg%>">
                    <div class="col-12 indikator-muat-<%=lg%>"><i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sedang memuat data...")%></div>
                </div>
            </div>
        </div>
    </div>

    <div class="col-12">
        <div class="card mb-3 shadow-sm kartu-elevasi-<%=lg%>">
            <div class="bg-holder d-none d-lg-block bg-card" style="background-image:url(<%=request.getContextPath()%>/img/corner-4.png);"></div>
            <div class="card-body position-relative">
                <div class="row w-100" id="wadah_3_<%=lg%>">
                    <div class="col-12 indikator-muat-<%=lg%>"><i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sedang memuat data...")%></div>
                </div>
            </div>
        </div>
    </div>

    

    <% if (pt) { %>
    
    <div class="col-12">
        <div class="card mb-3 shadow-sm kartu-elevasi-<%=lg%>">
            <div class="bg-holder d-none d-lg-block bg-card" style="background-image:url(<%=request.getContextPath()%>/img/corner-5.png);"></div>
            <div class="card-body position-relative">
                <div class="row w-100" id="wadah_4_<%=lg%>">
                    <div class="col-12 indikator-muat-<%=lg%>"><i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sedang memuat data...")%></div>
                </div>
            </div>
        </div>
    </div>
    
    <div class="col-12">
        <div class="card mb-3 shadow-sm kartu-elevasi-<%=lg%>">
            <div class="bg-holder d-none d-lg-block bg-card" style="background-image:url(<%=request.getContextPath()%>/img/corner-6.png);"></div>
            <div class="card-body position-relative">
                <div class="row w-100" id="wadah_5_<%=lg%>">
                    <div class="col-12 indikator-muat-<%=lg%>"><i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sedang memuat data...")%></div>
                </div>
            </div>
        </div>
    </div>

    <div class="col-12">
        <div class="card mb-3 shadow-sm kartu-elevasi-<%=lg%>">
            <div class="bg-holder d-none d-lg-block bg-card" style="background-image:url(<%=request.getContextPath()%>/img/corner-7.png);"></div>
            <div class="card-body position-relative">
                <div class="row w-100" id="wadah_6_<%=lg%>">
                    <div class="col-12 indikator-muat-<%=lg%>"><i class="fas fa-spinner fa-spin me-2"></i> <%=Common.getBahasaConfig("Sedang memuat data...")%></div>
                </div>
            </div>
        </div>
    </div>
    <% } %>
</div>

<script>
    /**
     * Fungsi asinkronus untuk mengambil dan merender data dashboard secara efisien.
     */
    async function memuatDataAdmin<%=lg%>(idWadah, param1, param2, idDaftar1, idDaftar2) {
        const kontainer = document.getElementById(idWadah);
        if (!kontainer) return;

        const parameterPencarian = [param1, param2];
        let strukturHtml = '';

        try {
            // Mengambil data secara berurutan agar penempatan layout tidak rusak
            for (const item of parameterPencarian) {
                const respons = await fetch("<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=home%2Fadmin&s=" + item);
                
                if (!respons.ok) {
                    throw new Error("Gagal terhubung ke server saat mengambil data: " + item);
                }
                
                const htmlTeks = await respons.text(); 
                strukturHtml += htmlTeks;
            }
            
            // Menyisipkan hasil gabungan HTML ke dalam antarmuka
            kontainer.innerHTML = strukturHtml;
            
            // Mengeksekusi script bawaan jika fungsi tersebut tersedia di core system
            if (typeof executeScripts === "function") {
                 executeScripts(kontainer);
            }
            
            // Menginisialisasi ulang sistem Paging (List.js) untuk komponen yang baru dirender
            const parameterDaftar = [idDaftar1, idDaftar2];
            parameterDaftar.forEach(function(idDaftarItem) {
                if (typeof hidupkanUlangPagingList === "function") {
                    hidupkanUlangPagingList(idDaftarItem);
                }
            });
            
        } catch (error) {
            console.error("Terjadi kesalahan:", error);
            kontainer.innerHTML = '<div class="col-12 text-center text-danger py-3"><i class="fas fa-exclamation-triangle fa-2x mb-2"></i><br><%=Common.getBahasaConfig("Gagal memuat data dari peladen.")%></div>';
        }
    }

    // =======================================================
    // EKSEKUSI PEMUATAN DATA BERDASARKAN TIPE INSTITUSI
    // =======================================================
    <% if (pt) { %>
        memuatDataAdmin<%=lg%>('wadah_1_<%=lg%>', 'jml_mahasiswa_per_tahun_angkatan', 'jml_mahasiswa_per_tahun_lulus', 'Jml_mahasiswa_per_tahun_angkatan', 'Jml_mahasiswa_per_tahun_lulus');
        memuatDataAdmin<%=lg%>('wadah_2_<%=lg%>', 'jml_mahasiswa_cuti', 'jml_calon_mahasiswa_per_prodi', 'Jml_mahasiswa_cuti', 'Jml_calon_mahasiswa_per_prodi');
        memuatDataAdmin<%=lg%>('wadah_3_<%=lg%>', 'jml_dosen_per_prodi', 'jml_skripsi', 'Jml_dosen_per_prodi', 'Jml_tugas_akhir');
        memuatDataAdmin<%=lg%>('wadah_4_<%=lg%>', 'jml_perkuliahan', 'jml_pertemuan', 'Jml_perkuliahan', 'Jml_pertemuan');
        memuatDataAdmin<%=lg%>('wadah_5_<%=lg%>', 'jml_pkl', 'jml_kkn', 'Jml_pkl', 'Jml_kkn');
        memuatDataAdmin<%=lg%>('wadah_6_<%=lg%>', 'jml_pertemuan_pkl', 'jml_pertemuan_kkn', 'Jml_pertemuan_pkl', 'Jml_pertemuan_kkn');
        
    <% } else if (ya) { %>
        memuatDataAdmin<%=lg%>('wadah_1_<%=lg%>', 'jml_siswa_per_tahun_angkatan', 'jml_siswa_per_tahun_lulus', 'Jml_siswa_per_tahun_angkatan', 'Jml_siswa_per_tahun_lulus');
        memuatDataAdmin<%=lg%>('wadah_2_<%=lg%>', 'jml_siswa_cuti', 'jml_calon_siswa_per_sekolah', 'Jml_siswa_cuti', 'Jml_calon_siswa_per_sekolah');
        memuatDataAdmin<%=lg%>('wadah_3_<%=lg%>', 'jml_guru_per_sekolah', 'jml_jadwalPelajaran', 'Jml_guru_per_sekolah', 'Jml_jadwalPelajaran');
    <% } %>
</script>