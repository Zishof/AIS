<%@page import="ais.common.IndonesianNumberToWords"%>
<%@page import="ais.database.model.GeneralValueObject"%>
<%@page import="ais.database.model.koperasi.CaraPembayaranKoperasi"%>
<%@page import="ais.action.master.helper.util.PerguruanTinggiUtil"%>
<%@page import="java.io.*"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.*"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.OnlineBmtUtil"%>
<%@page import="ais.action.master.helper.virtualaccount.DownloadTagihanAnggotaKoperasiBankOnline"%>
<%@page import="ais.database.model.Konfigurasi"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.VirtualAccountBank"%>
<%@page import="ais.database.model.koperasi.AnggotaKoperasi"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Order"%>
<%
// Menyiapkan Header Response agar dikenali sebagai JSON murni
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();

try {
    // 1. Verifikasi Sesi Pengguna
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status", "01");
        result.put("message", Common.getBahasaConfig("Sesi Anda telah berakhir. Silakan masuk kembali."));
        out.print(result.toString());
        return;
    }
    String caraPembayaranKoperasidata = request.getParameter("cara_pembayaran_id");
    CaraPembayaranKoperasi caraPembayaranKoperasi = (CaraPembayaranKoperasi) (caraPembayaranKoperasidata == null ? null : GeneralValueObject.ambilData(CaraPembayaranKoperasi.class, caraPembayaranKoperasidata));
    if(caraPembayaranKoperasi == null){
        out.print(Common.getBahasaConfig("Cara Pembayaran tidak ditemukan."));
        return;
    }
    

    // 2. Ambil payload body request (karena dikirim via fetch JSON)
    StringBuilder sb = new StringBuilder();
    BufferedReader reqReader = request.getReader();
    String reqLine;
    while ((reqLine = reqReader.readLine()) != null) {
        sb.append(reqLine);
    }
    
    JSONObject payload = new JSONObject();
    if (sb.length() > 0) {
        payload = new JSONObject(sb.toString());
    }

    // Ambil nominal & id_member, mengakomodir baik JSON maupun Parameter URL Form biasa
    String idMemberStr = payload.has("id_member") ? payload.getString("id_member") : request.getParameter("id_member");
    double nominal = payload.has("nominal") ? payload.getDouble("nominal") : Double.parseDouble(request.getParameter("nominal").trim());
    double admin = payload.has("admin") ? payload.getDouble("admin") : Double.parseDouble(request.getParameter("admin").trim());
    String selectedChannelCode = payload.optString("selectedChannelCode",
            request.getParameter("selectedChannelCode") == null ? "" : request.getParameter("selectedChannelCode")).trim();
    boolean onlineBmt = "ONLINE_BMT".equalsIgnoreCase(selectedChannelCode);
    if (onlineBmt) {
        String configuredAdmin = Common.getKonfigurasi(Konfigurasi.ONLINE_BMT_BIAYA_ADMINISTRASI, "0.0").getNilai();
        admin = Common.isNumber(configuredAdmin) ? Double.parseDouble(configuredAdmin) : 0.0;
    }
    if (nominal < 10000) {
        result.put("status", "01");
        result.put("message", Common.getBahasaConfig("Nominal pengisian saldo tidak valid. Minimal Rp 10.000."));
        out.print(result.toString());
        return;
    }

    // 3. Ambil data AnggotaKoperasi menggunakan Session Hibernate
    Session mySession = null;
    AnggotaKoperasi anggotaKoperasi = tbmuser.getAnggotaKoperasi();
    
    try {
        mySession = HibernateUtil.openSession();
        
        if (anggotaKoperasi == null && idMemberStr != null && !idMemberStr.isEmpty()) {
            anggotaKoperasi = (AnggotaKoperasi) mySession.get(AnggotaKoperasi.class, Long.parseLong(idMemberStr));
        }

        if (anggotaKoperasi == null) {
            result.put("status", "01");
            result.put("message", Common.getBahasaConfig("Data Anggota tidak ditemukan di sistem."));
            out.print(result.toString());
            return;
        }
        if (onlineBmt && (!OnlineBmtUtil.isGlobalEnabled()
                || caraPembayaranKoperasi.getKanalPembayaran() == null
                || !Boolean.TRUE.equals(caraPembayaranKoperasi.getKanalPembayaran().getAktfkanPembayaranViaOnlineBmt()))) {
            result.put("status", "01");
            result.put("message", Common.getBahasaConfig("Kanal Online BMT belum diaktifkan untuk cara pembayaran ini."));
            out.print(result.toString());
            return;
        }

        // =========================================================
        // CEK VIRTUAL ACCOUNT (TOPUP) YANG MASIH AKTIF (BELUM EXPIRED & BELUM DIBAYAR)
        // =========================================================
        Calendar calNow = ais.ui.util.WaktuUtil.getCalendar();
        Date dateNow = calNow.getTime();
        String keteranganTopup = onlineBmt
                ? "Topup senilai " + Common.numberFormat.get().format(nominal) + OnlineBmtUtil.MARKER
                : "Topup Saldo - " + anggotaKoperasi.getNama()+" senilai "+IndonesianNumberToWords.convert((long)nominal);
         
        @SuppressWarnings("unchecked")
        List<VirtualAccountBank> existingVaList = mySession.createCriteria(VirtualAccountBank.class)
                .add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi))
                .add(Restrictions.eq("bank", onlineBmt ? OnlineBmtUtil.BANK_NAME : "Esmartlink"))
                .add(Restrictions.eq("keterangan", keteranganTopup))
                .add(Restrictions.eq("total", nominal))
                .add(Restrictions.gt("kadaluarsa", dateNow))
                .add(Restrictions.isNull("deposit"))
                .add(Restrictions.isNull("kegiatan"))
                .add(Restrictions.isNull("pembayaran"))
                .addOrder(Order.desc("id"))
                .list();

        // Jika ada VA aktif yang persis sama, gunakan kembali URL-nya
        if (existingVaList != null && !existingVaList.isEmpty()) {
            VirtualAccountBank existingVa = existingVaList.get(0);
            if (onlineBmt || (existingVa.getLink() != null && !existingVa.getLink().isEmpty())) {
                result.put("status", "00");
                result.put("url", existingVa.getLink() == null ? "" : existingVa.getLink());
                result.put("reference", existingVa.getKode());
                
                JSONArray dataArr = new JSONArray();
                JSONObject linkObj = new JSONObject();
                linkObj.put("link", existingVa.getLink() == null ? "" : existingVa.getLink());
                linkObj.put("reference", existingVa.getKode());
                dataArr.put(linkObj);
                result.put("data", dataArr);
                
                result.put("message", Common.getBahasaConfig("Menampilkan transaksi yang masih aktif."));
                out.print(result.toString());
                out.flush();
                return;
            }
        }

        // =========================================================
        // JIKA TIDAK ADA VA AKTIF, GENERATE BARU KE E-SMARTLINK
        // =========================================================
        Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        Date expired_date = calendar.getTime();

        // Sanitasi data kontak customer
        String sender_name = (anggotaKoperasi.getNama() != null) ? anggotaKoperasi.getNama() : "Member Koperasi";
        String sender_email = (anggotaKoperasi.getEmail() != null && !anggotaKoperasi.getEmail().trim().isEmpty()) ? anggotaKoperasi.getEmail() : anggotaKoperasi.generateEmail();
        String sender_phone_number = (anggotaKoperasi.getHp() != null && anggotaKoperasi.getHp().length() >= 8) ? anggotaKoperasi.getHp() : "080000000000";

        String va = Common.getGeneratedBarCode(30);
        int mytotal = (int) nominal;
        int biayaAdmin = (int) admin; 

        if (onlineBmt) {
            Map<String, Object> bmtParam = new HashMap<String, Object>();
            bmtParam.put(OnlineBmtUtil.PARAM_KEY, Boolean.TRUE);
            VirtualAccountBank newVA = DownloadTagihanAnggotaKoperasiBankOnline.downloadData(
                    anggotaKoperasi, Double.valueOf(nominal), Double.valueOf(admin), bmtParam,
                    null, caraPembayaranKoperasi);
            if (newVA == null || newVA.getKode() == null || newVA.getKode().trim().isEmpty()) {
                result.put("status", "01");
                result.put("message", Common.getBahasaConfig("Nomor invoice Online BMT tidak dapat dibuat."));
            } else {
                result.put("status", "00");
                result.put("url", "");
                result.put("reference", newVA.getKode());
                JSONArray dataArr = new JSONArray();
                JSONObject linkObj = new JSONObject();
                linkObj.put("link", "");
                linkObj.put("reference", newVA.getKode());
                dataArr.put(linkObj);
                result.put("data", dataArr);
                result.put("message", Common.getBahasaConfig("Nomor invoice Online BMT berhasil dibuat."));
            }
            out.print(result.toString());
            out.flush();
            return;
        }
        
        JSONObject postData = new JSONObject();
        postData.put("order_id", va);
        postData.put("amount", mytotal + biayaAdmin);
        postData.put("description", keteranganTopup);
        
        JSONObject customer = new JSONObject();
        customer.put("name", sender_name.replaceAll("[^\\sa-zA-Z0-9]", ""));
        customer.put("email", sender_email);
        customer.put("phone", sender_phone_number);
        postData.put("customer", customer);

        JSONArray itemsSmartlink = new JSONArray();
        JSONObject itemTopup = new JSONObject();
        itemTopup.put("name", "Top-Up Saldo Koperasi");
        itemTopup.put("amount", mytotal);
        itemTopup.put("qty", 1);
        itemsSmartlink.put(itemTopup);
        
        if (biayaAdmin > 0) {
            JSONObject adminObj = new JSONObject();
            adminObj.put("name", "Biaya Admin");
            adminObj.put("amount", biayaAdmin);
            adminObj.put("qty", 1);
            itemsSmartlink.put(adminObj);
        }
        postData.put("item", itemsSmartlink);

        String cannel_va_e_smartlink = request.getParameter("selectedChannelCode");
        String[] ch = cannel_va_e_smartlink.split(",");
        JSONArray channel = new JSONArray();
        for (int i = 0; i < ch.length; i++) {
            if (!ch[i].trim().isEmpty()) {
                channel.put(ch[i].trim());
            }
        }
        postData.put("channel", channel);

        String link = Common.getRequestHostWithProtocol();
        if (Common.getKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF).getNilai().equals(Konfigurasi.AKTIF)) {
            link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
        }

        postData.put("type", "payment-page");
        postData.put("payment_mode", "CLOSE");
        
        // Format Waktu ke ISO8601 (Fallback manual jika Common.iso8601 bermasalah)
        SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        postData.put("expired_time", iso8601.format(expired_date));
        
        postData.put("callback_url", link + "/Esmartlink");
        postData.put("success_redirect_url", link + "/PembayaranSukses");
        postData.put("failed_redirect_url", link + "/PembayaranGagal");

        String strURL = Common.getKonfigurasi("gateway_url_va_e_smartlink", "https://payment-service-sbx.pakar-digital.com/api/payment/create-order").getNilai();
        
        // Autentikasi API
        String usernameEsmartlink = Common.getKonfigurasi("username_va_e_smartlink", "").getNilai();
        String passwordEsmartlink = Common.getKonfigurasi("password_va_e_smartlink", "").getNilai();
        
       String hasil = VirtualAccountBank.curlSmartlink(strURL, usernameEsmartlink,
    		   passwordEsmartlink, postData);

        // 6. Evaluasi Hasil Kembalian API E-Smartlink
        JSONObject jSONObject = new JSONObject(hasil);

        if (jSONObject.has("code") && (jSONObject.get("code") + "").equals("0")) {
            JSONObject data = jSONObject.getJSONObject("data");
            String paymentUrl = data.getString("payment_url");
            
            // SIMPAN DATA VIRTUAL ACCOUNT KE DATABASE (SIMILAR TO DownloadTagihanAnggotaKoperasiBankOnline)
            VirtualAccountBank newVA = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi(request).getId());
            newVA.setBank("Esmartlink");
            newVA.setKode(va);
            newVA.setTopup(nominal);
            newVA.setCaraPembayaranKoperasi(caraPembayaranKoperasi);
            newVA.setTotal(nominal);
            newVA.setBiayaAdmin((double)biayaAdmin);
            newVA.setKeterangan(keteranganTopup);
            newVA.setOtomatis(false);
            newVA.setKadaluarsa(expired_date);
            newVA.setAnggotaKoperasi(anggotaKoperasi);
            newVA.setRequest(postData.toString());
            newVA.setResponse(jSONObject.toString());
            newVA.setLink(paymentUrl);
            newVA.setBulanan("");
            
            
            // Simpan Data
            mySession.getTransaction().begin();
            mySession.saveOrUpdate(newVA);
            mySession.getTransaction().commit();

            // Response JSON ke Client
            result.put("status", "00");
            result.put("url", paymentUrl); 
            
            JSONArray dataArr = new JSONArray();
            JSONObject linkObj = new JSONObject();
            linkObj.put("link", paymentUrl);
            dataArr.put(linkObj);
            result.put("data", dataArr);
            result.put("message", Common.getBahasaConfig("Sukses"));

        } else {
            result.put("status", "01");
            result.put("message", Common.getBahasaConfig("Gagal mendapatkan tautan dari gerbang pembayaran."));
        }

    } catch (Exception e) {
        if (mySession != null && mySession.getTransaction().isActive()) {
            mySession.getTransaction().rollback();
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/_topup_service.jsp:251");
        result.put("status", "01");
        result.put("message", Common.getBahasaConfig("Terjadi kesalahan peladen internal: ") + e.getMessage());
    } finally {
        // SELALU TUTUP SESI DI FINALLY
        if (mySession != null) {
            try { mySession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/_topup_service.jsp:257");}
            try { mySession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/member/_topup_service.jsp:258");}
        }
        HibernateUtil.closeSessionQuietly(mySession);
    }

    out.print(result.toString());
    out.flush();
    
} catch(Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/member/_topup_service.jsp:267");
}
%>
