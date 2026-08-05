<%@page import="ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline"%>
<%@page import="java.io.Serializable"%>
<%@page import="java.util.Map"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.BankHost"%>
<%@page import="ais.action.servlet.Esmartlink"%>
<%@page contentType="application/json; charset=UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ais.database.model.VirtualAccountBank"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%><%
    // Inisialisasi Objek JSON untuk kembalian (response)
    JSONObject responseJson = new JSONObject();

    try {
        // 1. Pengecekan Sesi & Keamanan Akses
        Tbmuser tbmuser = Common.getCurrentUser(request);
        if (tbmuser == null || tbmuser.getUserId() == null) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan login kembali."));
            out.print(responseJson.toString());
            return;
        }

        // 2. Mengambil Parameter ID Virtual Account
        String idVa = request.getParameter("id_va");
        System.out.println("idVa "+idVa);
        if (idVa == null || idVa.trim().isEmpty()) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Parameter ID tagihan (id_va) tidak ditemukan."));
            out.print(responseJson.toString());
            return;
        }

        // 3. Mengambil Objek VirtualAccountBank dari Database
        VirtualAccountBank virtualAccountBank = (VirtualAccountBank) GeneralValueObject.ambilData(VirtualAccountBank.class, idVa, true);
        
        if (virtualAccountBank == null) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Data tagihan Virtual Account tidak valid atau tidak ditemukan."));
            out.print(responseJson.toString());
            return;
        }

        // 4. Validasi Response Pendaftaran VA Sebelumnya
        if (virtualAccountBank.getResponse() == null || virtualAccountBank.getResponse().isEmpty()) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Tidak ada data respon (Transaction ID) yang terhubung pada tagihan ini."));
            out.print(responseJson.toString());
            return;
        }

        JSONObject req = new JSONObject(virtualAccountBank.getResponse());
        if (!req.has("data")) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Format data tagihan tidak sesuai dengan standar sistem."));
            out.print(responseJson.toString());
            return;
        }

        JSONObject dataReq = req.getJSONObject("data");
        String transaction_id = dataReq.has("transaction_id") ? dataReq.get("transaction_id") + "" : "";

        if (transaction_id.isEmpty()) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Transaction ID tidak ditemukan pada data transaksi."));
            out.print(responseJson.toString());
            return;
        }

        // 5. Pengaturan URL & Kredensial E-Smartlink
        String linkPost = Common.getKonfigurasi("url_status_va_smartlink", "https://payment-service.pakar-digital.com/api/payment/inquiry-order/").getNilai().trim() + transaction_id;
        
        String username_va_e_smartlink = Common.getKonfigurasi("username_va_e_smartlink", "api-smartlink-sbx@budi-mulia.com").getNilai().trim();
        String password_va_e_smartlink = Common.getKonfigurasi("password_va_e_smartlink", "sQ3f2PMbGWvNxvi").getNilai().trim();

        // Cek Hierarki Kepemilikan (Siswa / Calon Siswa / Kanal)
        if (virtualAccountBank.getSiswa() != null && virtualAccountBank.getSiswa().getSekolah() != null) {
            username_va_e_smartlink = virtualAccountBank.getSiswa().getSekolah().getUsernameEsmartlink();
            password_va_e_smartlink = virtualAccountBank.getSiswa().getSekolah().getPasswordEsmartlink();
        } else if (virtualAccountBank.getCalonSiswa() != null && virtualAccountBank.getCalonSiswa().getSekolah() != null) {
            username_va_e_smartlink = virtualAccountBank.getCalonSiswa().getSekolah().getUsernameEsmartlink();
            password_va_e_smartlink = virtualAccountBank.getCalonSiswa().getSekolah().getPasswordEsmartlink();
        }

        if (virtualAccountBank.getKanalPembayaran() != null) {
            username_va_e_smartlink = virtualAccountBank.getKanalPembayaran().getUsernameEsmartlink();
            password_va_e_smartlink = virtualAccountBank.getKanalPembayaran().getPasswordEsmartlink();
        }

        // 6. Generate Key & Eksekusi CURL
        String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(username_va_e_smartlink, password_va_e_smartlink);
        String[] command = { "curl", "--location", "--request", "GET", linkPost, "--header", "Content-Type: application/json", "--header", "Authorization: Basic " + screet_key };

        ProcessBuilder process = new ProcessBuilder(command);
        Process p = process.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        
        String hasil = builder.toString();

        if (hasil == null || hasil.trim().isEmpty()) {
            responseJson.put("status", "error");
            responseJson.put("message", Common.getBahasaConfig("Tidak ada respon dari server pembayaran (Gateway)."));
            out.print(responseJson.toString());
            return;
        }

        // 7. Evaluasi Hasil Status Pembayaran
        JSONObject jsonObject2 = new JSONObject(hasil);
        String status = jsonObject2.isNull("data") || jsonObject2.getJSONObject("data").isNull("status") ? "ERROR" : jsonObject2.getJSONObject("data").get("status") + "";
        boolean masuk = status.trim().equalsIgnoreCase("success");

        if (masuk) {
            // Berhasil dibayar, lanjutkan proses settlement di sisi database
            BankHost bankHostDefault = null;
            BankHost targetBankHost = virtualAccountBank.getBankHost() == null ? bankHostDefault : virtualAccountBank.getBankHost();
            
            Esmartlink.doProses(hasil, request, targetBankHost, virtualAccountBank.getBank(), true);
            
            responseJson.put("status", "success");
            responseJson.put("message", Common.getBahasaConfig("Pembayaran berhasil diverifikasi dan diselesaikan."));
        } else {
            // Menunggu atau Gagal
            responseJson.put("status", "pending");
            responseJson.put("message", Common.getBahasaConfig("Pembayaran belum terselesaikan atau berstatus: ") + status.toUpperCase());
        }
        
        // Lampirkan data mentah jika pihak front-end membutuhkannya
        responseJson.put("data", jsonObject2);

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/_check_ulang_pembayaran.jsp:142");
        responseJson.put("status", "error");
        responseJson.put("message", Common.getBahasaConfig("Terjadi kesalahan sistem saat menghubungi gateway pembayaran. ") + e.getMessage());
    }

    // Mengembalikan (print) respons akhir dalam format JSON
    out.print(responseJson.toString());%>