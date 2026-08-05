<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.VOPembelajaran"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.Pertemuan"%>
<%
// 1. Validasi User
Tbmuser tbmuser = Common.getCurrentUser(request);
if (tbmuser == null || tbmuser.getUserId() == null) {
    response.sendRedirect(request.getContextPath() + "/logoff");
    return;
}
// 2. Ambil Data Pertemuan
String idStr = request.getParameter("pertemuan");
Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, idStr, true);

// Validasi data null untuk mencegah error 500
if (pertemuan == null) {
    out.print("<div class='alert alert-danger'>Data pertemuan tidak ditemukan.</div>");
    return;
}
Long pId = pertemuan.getId();
VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
String baseUrl = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning";
String linkIzin = baseUrl + "&s=pengajuan_izin_atau_sakit&pertemuan=" + pId;
%>
<div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="card-header bg-primary bg-gradient text-white p-3 border-0">
            <div class="d-flex align-items-center">
                <i class="fas fa-chalkboard-teacher me-2 fs-4"></i>
                <h6 class="mb-0 fw-bold text-uppercase"><%=Common.getBahasaConfig("Pengajuan Izin/Sakit") %> <%=pembelajaran.infoSimple() %></h6>
            </div>
        </div>
        
        
        <div class="card-body p-3">
                    <div class="d-grid gap-2">
                         <button class="btn btn-outline-dark btn-sm" 
                            onclick="var cm='cm_'+(++variable); loadModalContentCustomSimpan('<%=Common.getBahasaConfigJS("Pengajuan Izin") %>', '<%=linkIzin%>&contentModal='+cm, '800px', true, 'saveData_'+cm+'();reloadAbsen<%=pId%>(true);', cm);">
                            <i class="fas fa-external-link-alt me-1"></i> <%=Common.getBahasaConfig("Buka Form Pengajuan") %>
                        </button>
                    </div>
                    <div id="info_izin_<%=pId%>" class="mt-2">
                    </div>
		</div>

       
        
</div>

<script>
/**
 * Fungsi reload data Absensi
 * Menggunakan Promise.all untuk fetch data secara paralel
 */
async function reloadAbsen<%=pId%>(withDelay) {
    if (withDelay) {
        setTimeout(() => doReloadAbsen<%=pId%>(), 1000);
    } else {
        doReloadAbsen<%=pId%>();
    }
}

async function doReloadAbsen<%=pId%>() {
    const baseUrl = "<%=Common.ROOT%>/baru?hanya_tampil_jsp=true&p=elearning&s=";
    const pParam = "&pertemuan=<%=pId%>";

    

    try {
       

        // 3. Fetch data (tambahkan searchParams ke URL mahasiswa)
        const [resIzin] = await Promise.all([
            fetch(baseUrl + "_daftar_peserta_absen_izin_dan_sakit" + pParam)
        ]);

        const htmlIzin = await resIzin.text();


        document.getElementById("info_izin_<%=pId%>").innerHTML = htmlIzin;


    } catch (error) {
        console.error("Gagal memuat data presensi:", error);
    }
}

// Inisialisasi awal
reloadAbsen<%=pId%>(false);
</script>