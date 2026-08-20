<%@page session="false"%>
<%--
  ============================================================================
  ENDPOINT DATA (JSON) untuk tab "Satuan Kerja" pada halaman Anggota.

  Seluruh logikanya dipusatkan di ais.action.servlet.api.SatuanKerjaKantinHelper
  -- kelas yang SAMA dipakai POS Desktop/Android lewat PosApi. Dengan begitu
  daftar, penyaring (aktif = default_item true, cakupan per yayasan, lalu per
  pendaftar), dan cara penugasan member tidak mungkin berbeda antar kanal.

  Aksi: list | simpan | hapus | anggota_list | anggota_simpan
  ============================================================================
--%>
<%@page import="org.json.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.action.servlet.api.SatuanKerjaKantinHelper"%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject hasil = new JSONObject();
try {
    Tbmuser pengguna = Common.getCurrentUser(request);
    if (pengguna == null || pengguna.getUserId() == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        hasil.put("status", "97");
        hasil.put("description", "Sesi berakhir. Silakan masuk kembali.");
        out.print(hasil.toString());
        return;
    }

    // Parameter dibungkus JSONObject supaya bentuk masukannya SAMA dengan yang
    // diterima helper dari PosApi -- tidak ada cabang khusus JSP di dalam helper.
    JSONObject req = new JSONObject();
    java.util.Enumeration<String> namaParam = request.getParameterNames();
    while (namaParam.hasMoreElements()) {
        String n = namaParam.nextElement();
        req.put(n, request.getParameter(n));
    }
    String aksi = request.getParameter("aksi");
    aksi = aksi == null ? "" : aksi.trim();

    if ("list".equals(aksi)) {
        req.put("tampilkan_nonaktif", "true".equalsIgnoreCase(request.getParameter("tampilkan_nonaktif")));
        SatuanKerjaKantinHelper.satuanKerjaList(pengguna, request, req, hasil);
    } else if ("simpan".equals(aksi)) {
        req.put("aktif", !"false".equalsIgnoreCase(request.getParameter("aktif")));
        SatuanKerjaKantinHelper.satuanKerjaSimpan(pengguna, request, req, hasil);
    } else if ("hapus".equals(aksi)) {
        SatuanKerjaKantinHelper.satuanKerjaHapus(req, hasil);
    } else if ("anggota_list".equals(aksi)) {
        SatuanKerjaKantinHelper.satuanKerjaAnggotaList(req, hasil);
    } else if ("anggota_simpan".equals(aksi)) {
        // Daftar id dikirim sbg CSV oleh halaman JSP; helper mengharapkan array.
        String csv = request.getParameter("anggota_id");
        JSONArray arr = new JSONArray();
        if (csv != null) {
            for (String bagian : csv.split(",")) {
                if (bagian.trim().length() > 0) {
                    arr.put(bagian.trim());
                }
            }
        }
        req.put("anggota_id", arr);
        SatuanKerjaKantinHelper.satuanKerjaAnggotaSimpan(req, hasil);
    } else {
        hasil.put("status", "91");
        hasil.put("description", "Aksi tidak dikenal.");
    }
} catch (Exception e) {
    ais.common.ErrorAuditUtil.record(e, "satuan_kerja_service.jsp");
    hasil = new JSONObject();
    hasil.put("status", "99");
    hasil.put("description", "Terjadi kesalahan saat memproses permintaan.");
}
out.print(hasil.toString());
%>
