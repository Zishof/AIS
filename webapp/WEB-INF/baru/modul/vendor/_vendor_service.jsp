<%@page import="javax.servlet.http.Cookie"%>
<%@page import="java.io.File"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.util.Properties"%>
<%@page import="javax.mail.BodyPart"%>
<%@page import="javax.mail.Message"%>
<%@page import="javax.mail.Multipart"%>
<%@page import="javax.mail.Transport"%>
<%@page import="javax.mail.internet.InternetAddress"%>
<%@page import="javax.mail.internet.MimeBodyPart"%>
<%@page import="javax.mail.internet.MimeMessage"%>
<%@page import="javax.mail.internet.MimeMultipart"%>
<%@page import="java.util.Collection"%>
<%@page import="javax.servlet.http.Part"%>
<%@page import="java.lang.reflect.Method"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.PengumumanAkademis"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="org.apache.commons.lang.RandomStringUtils"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.model.Tbmrole"%>
<%@page import="ais.delivery.email.sender.MailSender"%>
<%@page import="ais.database.model.asset.PenyediaAsset"%>
<%@page import="ais.database.model.asset.PenyediaAssetPunyaDokumen"%>
<%@page import="ais.database.model.asset.DokumenPenyediaAsset"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String nvl(String s) { return s == null ? "" : s.trim(); }
    private boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private Object safeInvoke(Object obj, String method) {
        try { if (obj == null) return null; Method m = obj.getClass().getMethod(method, new Class[]{}); return m.invoke(obj, new Object[]{}); } catch(Exception e) { return null; }
    }
    private boolean safeBool(Object obj, String method, boolean def) {
        try { Object x = safeInvoke(obj, method); return x == null ? def : Boolean.valueOf(String.valueOf(x)).booleanValue(); } catch(Exception e) { return def; }
    }
    private JSONObject readExtra(PenyediaAsset v) {
        try {
            String f = v == null ? null : v.getFormula();
            if (f != null && f.trim().startsWith("{")) return new JSONObject(f);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:52");}
        return new JSONObject();
    }
    private void putIfParam(JSONObject o, HttpServletRequest request, String key) {
        try { String v = request.getParameter(key); if (v != null) o.put(key, v.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:56");}
    }
    private String getSubmittedFileName(Part part) {
        if (part == null) return "";
        String cd = part.getHeader("content-disposition");
        if (cd == null) return "file";
        String[] items = cd.split(";");
        for (int i=0;i<items.length;i++) {
            String item = items[i].trim();
            if (item.startsWith("filename")) {
                String name = item.substring(item.indexOf('=') + 1).trim().replace("\"", "");
                int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                return slash >= 0 ? name.substring(slash + 1) : name;
            }
        }
        return "file";
    }
    private JSONObject vendorJson(PenyediaAsset v) throws Exception {
        JSONObject o = new JSONObject();
        if (v == null) return o;
        o.put("id", v.getId());
        o.put("kode", nvl(v.getKode()));
        o.put("nama", nvl(v.getNama()));
        o.put("pemilik", nvl(v.getPemilik()));
        o.put("kontak", nvl(v.getKontak()));
        o.put("email", nvl(v.getEmail()));
        o.put("telp", nvl(v.getTelp()));
        o.put("fax", nvl(v.getFax()));
        o.put("alamat", nvl(v.getAlamat()));
        o.put("kodePos", nvl(v.getKodePos()));
        o.put("npwp", nvl(v.getNpwp()));
        o.put("noAktaPendirian", nvl(v.getNoAktaPendirian()));
        o.put("namaNotaris", nvl(v.getNamaNotaris()));
        o.put("noPengesahan", nvl(v.getNoPengesahan()));
        o.put("noRek", nvl(v.getNoRek()));
        o.put("atasNama", nvl(v.getAtasNama()));
        o.put("aktif", v.getAktif());
        return o;
    }
    private int profilScore(PenyediaAsset v, JSONObject x) {
        int total = 16; int ok = 0;
        if(!empty(v.getNama())) ok++; if(!empty(v.getEmail())) ok++; if(!empty(v.getTelp())) ok++; if(!empty(v.getKontak())) ok++;
        if(!empty(v.getAlamat())) ok++; if(!empty(v.getNpwp())) ok++; if(!empty(v.getPemilik())) ok++; if(!empty(v.getNoAktaPendirian())) ok++;
        if(!empty(v.getNoPengesahan())) ok++; if(!empty(v.getNoRek())) ok++; if(!empty(v.getAtasNama())) ok++; if(!empty(x.optString("nib"))) ok++;
        if(!empty(x.optString("bidang"))) ok++; if(!empty(x.optString("pernyataan"))) ok++; if(!empty(x.optString("produkJasa"))) ok++; if(!empty(x.optString("pengalaman"))) ok++;
        return (int)Math.round((ok * 100.0) / total);
    }
    private String cleanFilename(String s) {
        if (s == null || s.trim().isEmpty()) return "file";
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String cfg(String key, String def) {
        try { return Common.getKonfigurasi(key, def).getNilai(); } catch(Exception e) { return def; }
    }
    private boolean cfgAktif(String key, String def) {
        try {
            String v = cfg(key, def);
            return "Aktif".equalsIgnoreCase(v) || "Ya".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v);
        } catch(Exception e) { return false; }
    }
    private int cfgInt(String key, int def) {
        try { return Integer.parseInt(cfg(key, String.valueOf(def)).trim()); } catch(Exception e) { return def; }
    }
    private String cookiePath(HttpServletRequest request) {
        String p = request.getContextPath();
        return p == null || p.trim().isEmpty() ? "/" : p;
    }
    private void setVendorCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int maxAge) {
        try {
            Cookie c = new Cookie(name, value == null ? "" : value);
            c.setPath(cookiePath(request));
            c.setMaxAge(maxAge);
            response.addCookie(c);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:130");}
    }
    private void clearVendorCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        setVendorCookie(request, response, name, "", 0);
    }
    private Tbmrole ensurePenyediaRole(Session db) throws Exception {
        if (ConstantValues.tbmrolePenyedia == null) {
            ConstantValues.tbmrolePenyedia = (Tbmrole) db.createCriteria(Tbmrole.class)
                    .add(Restrictions.eq("roleId", Tbmrole.PENYEDIA))
                    .setMaxResults(1).uniqueResult();
        }
        if (ConstantValues.tbmrolePenyedia == null) {
            ConstantValues.tbmrolePenyedia = new Tbmrole();
            ConstantValues.tbmrolePenyedia.setRoleId(Tbmrole.PENYEDIA);
            ConstantValues.tbmrolePenyedia.setNama("Penyedia / Perusahaan");
            db.save(ConstantValues.tbmrolePenyedia);
            db.flush();
        }
        return ConstantValues.tbmrolePenyedia;
    }
    private String normalizeUsername(String s) {
        s = nvl(s).toLowerCase();
        if (s.indexOf(',') >= 0) s = s.split(",")[0].trim();
        s = s.replaceAll("[^a-z0-9@._-]", "");
        if (s.length() < 3) s = "vendor" + RandomStringUtils.randomNumeric(4);
        return s;
    }
    private String buildUniqueUsername(Session db, PenyediaAsset v) {
        String base = normalizeUsername(v == null ? "" : v.getEmail());
        if (base.length() < 3 || base.indexOf("@") < 0) {
            String nama = v == null ? "vendor" : nvl(v.getNama());
            String[] parts = StringUtils.split(nama, " ");
            base = normalizeUsername((parts != null && parts.length > 0 ? parts[0] : "vendor") + RandomStringUtils.randomNumeric(3));
        }
        String username = base;
        int loop = 0;
        while (true) {
            try {
                Object existing = db.createCriteria(Tbmuser.class).add(Restrictions.eq("userId", username)).setMaxResults(1).uniqueResult();
                if (existing == null) return username;
            } catch (Exception e) {
                return username;
            }
            loop++;
            username = base + loop;
            if (loop > 50) return base + RandomStringUtils.randomNumeric(5);
        }
    }
    private Tbmuser createOrResetVendorUser(Session db, PenyediaAsset vendor, String passwordPlain) throws Exception {
        ensurePenyediaRole(db);
        Tbmuser tbmuser = (Tbmuser) db.createCriteria(Tbmuser.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.eq("penyediaAsset", vendor))
                .setMaxResults(1).uniqueResult();
        if (tbmuser == null || tbmuser.getUserId() == null) {
            tbmuser = new Tbmuser();
            tbmuser.setUserId(buildUniqueUsername(db, vendor));
            tbmuser.setEmail(nvl(vendor.getEmail()));
            tbmuser.setIs_encripted(true);
            tbmuser.setRoot(false);
            tbmuser.setAktif(true);
            tbmuser.setUserNama(nvl(vendor.getNama()));
            tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passwordPlain.trim()));
            tbmuser.setUserRole(ConstantValues.tbmrolePenyedia);
            tbmuser.setUserShow(1);
            tbmuser.setPenyediaAsset(vendor);
            db.save(tbmuser);
        } else {
            tbmuser.setEmail(nvl(vendor.getEmail()));
            tbmuser.setIs_encripted(true);
            tbmuser.setRoot(false);
            tbmuser.setAktif(true);
            tbmuser.setUserNama(nvl(vendor.getNama()));
            tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passwordPlain.trim()));
            tbmuser.setUserRole(ConstantValues.tbmrolePenyedia);
            tbmuser.setUserShow(1);
            db.update(tbmuser);
        }
        db.flush();
        return tbmuser;
    }
    private String vendorPortalUrl(HttpServletRequest request) {
        String scheme = request.getScheme() == null ? "https" : request.getScheme();
        String server = request.getServerName() == null ? "" : request.getServerName();
        int port = request.getServerPort();
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        String portPart = (port == 80 || port == 443 || port <= 0) ? "" : ":" + port;
        return scheme + "://" + server + portPart + ctx + "/vendor";
    }
    private String htmlEscape(String s) {
        s = nvl(s);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String buildEmailAkunVendor(HttpServletRequest request, PenyediaAsset vendor, Tbmuser user, String passwordPlain) {
        String url = vendorPortalUrl(request);
        String namaKontak = nvl(vendor.getKontak()).isEmpty() ? vendor.getNama() : vendor.getKontak();
        String namaVendor = nvl(vendor.getNama());
        String username = user == null ? "" : nvl(user.getUserId());
        String portal = htmlEscape(url);
        return "<div style='font-family:Arial,Helvetica,sans-serif;line-height:1.72;color:#0f172a;background:#f8fafc;padding:22px'>"
                + "<div style='max-width:860px;margin:auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:18px;overflow:hidden'>"
                + "<div style='background:linear-gradient(90deg,#1a4087,#d2aa2a);color:#fff;padding:24px 28px'>"
                + "<h1 style='margin:0;font-size:24px'>" + htmlEscape(cfg("vendor_email_judul", "Informasi Akun Portal Vendor")) + "</h1>"
                + "<p style='margin:8px 0 0;opacity:.92'>" + htmlEscape(cfg("vendor_email_subjudul", "Akses resmi untuk melengkapi profil, legalitas, kualifikasi, dan dokumen rekanan.")) + "</p>"
                + "</div>"
                + "<div style='padding:28px'>"
                + "<p>Yth. <b>" + htmlEscape(namaKontak) + "</b>,</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_pembuka", "Terima kasih telah mendaftar sebagai calon vendor. Akun ini digunakan untuk melengkapi profil, legalitas, kualifikasi, dan dokumen rekanan secara mandiri.")) + "</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_akun", "Berikut username dan password untuk masuk ke Portal Vendor. Simpan informasi ini dengan aman dan gunakan hanya oleh petugas resmi perusahaan.")) + "</p>"
                + "<table cellpadding='10' cellspacing='0' style='border-collapse:separate;border-spacing:0;width:100%;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;margin:18px 0'>"
                + "<tr><td style='width:190px;font-weight:bold;border-bottom:1px solid #e2e8f0'>Nama Vendor</td><td style='border-bottom:1px solid #e2e8f0'>" + htmlEscape(namaVendor) + "</td></tr>"
                + "<tr><td style='font-weight:bold;border-bottom:1px solid #e2e8f0'>Username</td><td style='border-bottom:1px solid #e2e8f0'><b>" + htmlEscape(username) + "</b></td></tr>"
                + "<tr><td style='font-weight:bold;border-bottom:1px solid #e2e8f0'>Password</td><td style='border-bottom:1px solid #e2e8f0'><b>" + htmlEscape(passwordPlain) + "</b></td></tr>"
                + "<tr><td style='font-weight:bold'>Link Login</td><td><a href='" + portal + "'>" + portal + "</a></td></tr>"
                + "</table>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_profil", "Setelah login, lengkapi data perusahaan agar tim pengadaan mudah mengenali identitas, kontak, dan informasi rekening vendor.")) + "</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_legalitas", "Lengkapi legalitas dan kualifikasi agar proses evaluasi administrasi dapat dilakukan lebih cepat, transparan, dan terdokumentasi.")) + "</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_kualifikasi", "Bidang pekerjaan khusus dapat dilengkapi dengan pengalaman, izin usaha, tenaga ahli, dukungan distributor, layanan purna jual, dan produk atau jasa utama.")) + "</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_dokumen", "Setiap dokumen memiliki status sehingga vendor dapat melihat mana yang belum diperiksa, perlu revisi, atau sudah terverifikasi.")) + "</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_pengumuman", "Akun vendor juga dapat digunakan untuk memantau pengumuman pengadaan dan menjaga data perusahaan tetap mutakhir.")) + "</p>"
                + "<p>" + htmlEscape(cfg("vendor_email_paragraf_penutup", "Mohon segera login dan lengkapi data dengan benar. Status akhir vendor tetap mengikuti proses verifikasi dan evaluasi internal.")) + "</p>"
                + "<p style='margin-top:20px'>Hormat kami,<br><b>Tim Pengadaan</b></p>"
                + "<p style='font-size:12px;color:#64748b;border-top:1px solid #e2e8f0;padding-top:14px'>Email ini dibuat otomatis oleh sistem. Mohon tidak membalas email ini. Untuk keamanan, segera simpan kredensial di tempat yang aman dan lakukan koordinasi internal apabila akun digunakan oleh PIC perusahaan.</p>"
                + "</div></div></div>";
    }
    private PenyediaAsset findVendorByEmail(Session db, String email) {
        email = nvl(email).toLowerCase();
        if (email.indexOf(',') >= 0) email = email.split(",")[0].trim();
        if (email.length() < 3) return null;
        try {
            List list = db.createCriteria(PenyediaAsset.class)
                    .add(Restrictions.ilike("email", email, MatchMode.ANYWHERE))
                    .setMaxResults(10).list();
            for (int i = 0; i < list.size(); i++) {
                PenyediaAsset v = (PenyediaAsset) list.get(i);
                String em = nvl(v.getEmail()).toLowerCase();
                String[] parts = em.split(",");
                for (int j = 0; j < parts.length; j++) {
                    if (email.equals(parts[j].trim())) return v;
                }
                if (em.contains(email)) return v;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:272");}
        return null;
    }
    /*
     * EMAIL WAJIB PORTAL VENDOR
     * ----------------------------------------------------------------------
     * Jalur ini sengaja mengirim email langsung melalui JavaMail dengan membaca
     * konfigurasi SMTP yang sama seperti MailSender.java, namun TIDAK membaca
     * dan TIDAK bergantung pada konfigurasi:
     *     aktfikan_pengiriman_notif
     *
     * Tujuannya: email akun vendor tetap terkirim meskipun notifikasi aplikasi
     * dimatikan. MailSender tetap disediakan sebagai fallback, tetapi pengiriman
     * utama dilakukan langsung dari JSP ini agar modul pendaftaran vendor tidak
     * terhambat oleh status notifikasi.
     */
    private boolean sendEmailVendorLangsung(String subject, String body, String sender, String recipientsTemp) {
        String hasil = "";
        try {
            recipientsTemp = nvl(recipientsTemp);
            sender = nvl(sender);
            if (recipientsTemp.isEmpty() || !Common.isValidEmailAddress(recipientsTemp.split(",")[0].trim())) {
                System.out.println("[Vendor Email] Email penerima kosong/tidak valid: " + recipientsTemp);
                return false;
            }
            if (sender.isEmpty()) {
                sender = Common.getKonfigurasi("default_email_sender", "no-reply@ecampus.id").getNilai();
            }

            boolean tidakBoleh = Common.getKonfigurasi("email_tidak_boleh_kirim_dari", "notify@tarunabakti.or.id")
                    .getNilai().trim().equalsIgnoreCase(sender);
            if (tidakBoleh) {
                System.out.println("[Vendor Email] Sender diblokir konfigurasi email_tidak_boleh_kirim_dari: " + sender);
                return false;
            }

            String emailMonitoring = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
            if (!emailMonitoring.trim().isEmpty()) {
                recipientsTemp = recipientsTemp.trim().isEmpty() ? emailMonitoring : recipientsTemp + "," + emailMonitoring;
            }

            if (recipientsTemp.endsWith("@ahoo.co")) {
                recipientsTemp = recipientsTemp.replaceAll("@ahoo.co", "@yahoo.co");
            }
            if (recipientsTemp.endsWith("@mail.com")) {
                recipientsTemp = recipientsTemp.replaceAll("@mail.com", "@gmail.com");
            }

            Properties props = new Properties();
            String mailhost = Common.getKonfigurasi("default_mailhost", "smtp.gmail.com").getNilai();
            String protocol = Common.getKonfigurasi("default_mail_protocol", "smtp").getNilai();
            String auth = Common.getKonfigurasi("default_mail_auth", "true").getNilai();
            String port = Common.getKonfigurasi("default_mail_port", "465").getNilai();
            String soketPort = Common.getKonfigurasi("default_mail_soket_port", "465").getNilai();
            String soketClass = Common.getKonfigurasi("default_mail_soket_class", "javax.net.ssl.SSLSocketFactory").getNilai();
            String soketFallback = Common.getKonfigurasi("default_mail_soket_fallback", "false").getNilai();
            String soketQuitwaitback = Common.getKonfigurasi("default_mail_soket_quitwait", "false").getNilai();
            String starttls = Common.getKonfigurasi("mail.smtp.starttls.enable", "").getNilai();
            String protocols = Common.getKonfigurasi("mail.smtp.ssl.protocols", "TLSv1.2").getNilai();
            String ssl = Common.getKonfigurasi("mail.smtp.ssl.enable", "").getNilai();

            props.setProperty("mail.transport.protocol", protocol);
            props.setProperty("mail.host", mailhost);
            props.setProperty("mail.smtp.host", mailhost);
            props.put("mail.smtp.auth", auth);
            if (!ssl.trim().isEmpty()) {
                props.put("mail.smtp.ssl.enable", ssl);
            }
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.socketFactory.port", soketPort);
            if (!soketClass.trim().isEmpty()) {
                props.put("mail.smtp.socketFactory.class", soketClass);
            }
            if (!soketFallback.trim().isEmpty()) {
                props.put("mail.smtp.socketFactory.fallback", soketFallback);
            }
            if (!soketQuitwaitback.trim().isEmpty()) {
                props.setProperty("mail.smtp.quitwait", soketQuitwaitback);
            }
            if (!starttls.trim().isEmpty()) {
                props.put("mail.smtp.starttls.enable", starttls);
            }
            if (!protocols.trim().isEmpty()) {
                props.put("mail.smtp.ssl.protocols", protocols);
            }

            boolean debugMail = Common.getKonfigurasi("mail_debug", "Tidak Aktif").getNilai().equalsIgnoreCase("Aktif");

            javax.mail.Session mailSession = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    String emailUsername = Common.getKonfigurasi("default_email_username", "zishof").getNilai();
                    String emailPassword = Common.getKonfigurasi("default_email_password", "zishof").getNilai();
                    return new javax.mail.PasswordAuthentication(emailUsername, emailPassword);
                }
            });
            mailSession.setDebug(debugMail);

            MimeMessage message = new MimeMessage(mailSession);
            message.setSender(new InternetAddress(sender));
            message.setFrom(new InternetAddress(sender));
            message.setSubject(subject, "UTF-8");
            message.setContent(body, "text/html; charset=UTF-8");
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientsTemp));

            Transport.send(message);
            hasil = "[Vendor Email] Kirim akun vendor ke " + recipientsTemp + " sukses. Konfigurasi aktfikan_pengiriman_notif diabaikan khusus email vendor.";
            System.out.println(hasil);
            return true;
        } catch (Exception e) {
            hasil = "[Vendor Email] Kirim akun vendor gagal: " + e.getMessage();
            System.out.println(hasil);
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:383");
            return false;
        }
    }

    private boolean sendCredentialEmail(HttpServletRequest request, PenyediaAsset vendor, Tbmuser user, String passwordPlain) {
        String sender = "";
        String subject = "";
        String body = "";
        String recipients = "";
        try {
            sender = Common.getKonfigurasi("email_pendaftaran_vendor_sender",
                    Common.getKonfigurasi("default_email_sender", "no-reply@ecampus.id").getNilai()).getNilai();
            subject = Common.getKonfigurasi("email_pendaftaran_vendor_subject",
                    "Informasi Akun Portal Vendor").getNilai();
            body = buildEmailAkunVendor(request, vendor, user, passwordPlain);
            recipients = nvl(vendor.getEmail());

            // Pengiriman utama: langsung melalui JavaMail agar tidak bergantung pada aktfikan_pengiriman_notif.
            boolean directSent = sendEmailVendorLangsung(subject, body, sender, recipients);
            if (directSent) {
                return true;
            }

            // Fallback: tetap coba MailSender jika pengiriman langsung gagal.
            try {
                MailSender.sendMail(new JSONArray(), subject, body, sender, recipients, vendor);
                System.out.println("[Vendor Email] Fallback MailSender dipanggil setelah direct mail gagal.");
                return true;
            } catch (Exception mailSenderError) {
                mailSenderError.printStackTrace(); ais.common.ErrorAuditUtil.record(mailSenderError, "auto-audit webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:413");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:417");
            return false;
        }
    }

%>
<%
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setDateHeader("Expires", 0);
JSONObject res = new JSONObject();
Session db = null; Transaction tx = null;
try {
    String aksi = request.getParameter("action");
    if (aksi == null) aksi = "";
    db = HibernateUtil.getSessionFactory().openSession();

    if ("list_pengumuman".equals(aksi)) {
        JSONArray arr = new JSONArray();
        try {
            /*
             * VENDOR ANNOUNCEMENT FILTER
             * ------------------------------------------------------------
             * Modul vendor hanya boleh menampilkan PengumumanAkademis yang
             * memang diperuntukkan untuk vendor/penyedia, yaitu:
             *     PengumumanAkademis.UNTUK_VENDOR
             *
             * Kriteria di bawah mengikuti pola TampilanPengumumanVendorAction
             * yang Bapak berikan: pengumuman aktif, masih relevan berdasarkan
             * tanggal/sampai atau tetap ditampilkan, lalu diurutkan berdasarkan
             * nomor urut kategori, tanggal terbaru, dan id terbaru.
             */
            org.hibernate.criterion.Criterion r = Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_VENDOR);
            org.hibernate.Criteria criteria = db.createCriteria(PengumumanAkademis.class)
                    .add(Restrictions.or(
                            Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
                                    Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
                            Restrictions.or(Restrictions.le("tanggal", ais.ui.util.WaktuUtil.getDate()),
                                    Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()))))
                    .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                    .add(r);
            try {
                criteria.createAlias("kategoriPengumuman", "kategoriPengumuman", org.hibernate.Criteria.LEFT_JOIN)
                        .addOrder(Order.asc("kategoriPengumuman.nomorUrut"));
            } catch (Exception orderKategoriError) { ais.common.ErrorAuditUtil.record(orderKategoriError, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:461");
                // Jika pada instalasi tertentu relasi kategori belum tersedia, pengumuman vendor tetap ditampilkan.
            }
            criteria.addOrder(Order.desc("tanggal"));
            criteria.addOrder(Order.desc("id"));
            criteria.setMaxResults(6);

            List list = criteria.list();
            for (int i=0; i<list.size(); i++) {
                PengumumanAkademis p = (PengumumanAkademis) list.get(i);
                JSONObject o = new JSONObject();
                o.put("id", safeInvoke(p, "getId"));
                o.put("judul", nvl(String.valueOf(safeInvoke(p, "getJudul"))));
                String isi = nvl(String.valueOf(safeInvoke(p, "getKeterangan")));
                if ("null".equalsIgnoreCase(isi)) isi = "";
                if (isi.length() > 220) isi = isi.substring(0, 217) + "...";
                o.put("isi", isi);
                Date t = (Date) safeInvoke(p, "getTanggal");
                o.put("tanggal", t == null ? "-" : Common.dateFormat.get().format(t));
                o.put("diperuntukkan", PengumumanAkademis.UNTUK_VENDOR);
                arr.put(o);
            }
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:484");
        }
        res.put("status", "success");
        res.put("data", arr);
        res.put("total", arr.length());
        res.put("limit", 6);
        res.put("filter", "PengumumanAkademis.UNTUK_VENDOR");
    }
    else if ("register".equals(aksi)) {
        String nama = nvl(request.getParameter("nama"));
        String kontak = nvl(request.getParameter("kontak"));
        String email = nvl(request.getParameter("email"));
        String telp = nvl(request.getParameter("telp"));
        String npwp = nvl(request.getParameter("npwp"));
        String bidang = nvl(request.getParameter("bidang"));
        String pesan = nvl(request.getParameter("pesan"));

        if (empty(nama) || empty(kontak) || empty(email) || empty(npwp) || empty(pesan)) {
            res.put("status", "error");
            res.put("message", "Nama vendor, kontak, email, NPWP, dan pesan wajib diisi.");
        } else if (!Common.isValidEmailAddress(email.split(",")[0].trim())) {
            res.put("status", "error");
            res.put("message", "Format email kontak vendor tidak valid.");
        } else {
            PenyediaAsset cek = findVendorByEmail(db, email);
            if (cek != null) {
                res.put("status", "error");
                res.put("code", "EMAIL_REGISTERED");
                res.put("message", cfg("vendor_pesan_email_sudah_terdaftar", "Email kontak vendor sudah terdaftar. Gunakan fitur Kirim Ulang Akses Vendor untuk mendapatkan kembali username dan password."));
            } else if (!empty(npwp) && db.createCriteria(PenyediaAsset.class).add(Restrictions.eq("npwp", npwp.trim())).setMaxResults(1).uniqueResult() != null) {
                // Cegah vendor ganda berdasarkan NPWP (selaras validasi di modul admin).
                res.put("status", "error");
                res.put("code", "NPWP_REGISTERED");
                res.put("message", cfg("vendor_pesan_npwp_sudah_terdaftar", "NPWP sudah terdaftar pada vendor lain. Jika ini perusahaan Anda, gunakan fitur Kirim Ulang Akses Vendor."));
            } else {
                String passwordPlain = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
                Tbmuser userBaru = null;
                PenyediaAsset v = null;
                boolean emailTerkirim = false;

                tx = db.beginTransaction();
                v = new PenyediaAsset();
                v.setKode("VND-" + new java.text.SimpleDateFormat("yyMMddHHmmss").format(new Date()));
                v.setNama(nama);
                v.setKontak(kontak);
                v.setEmail(email);
                v.setTelp(telp);
                v.setNpwp(npwp);
                v.setKeterangan("Pendaftaran awal vendor. Bidang: " + bidang + "\nPesan: " + pesan);

                // Akun dibuat langsung agar calon vendor bisa login dan melengkapi data mandiri.
                // Status verifikasi tetap disimpan di formula, sehingga admin tetap bisa melakukan validasi.
                v.setAktif(Boolean.TRUE);
                try { v.setTanggalPembuatan(new Date()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:537");}

                JSONObject ex = new JSONObject();
                ex.put("bidang", bidang);
                ex.put("pesanPendaftaran", pesan);
                ex.put("statusPendaftaran", "Menunggu verifikasi admin");
                ex.put("tanggalPendaftaran", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                v.setFormula(ex.toString());
                db.save(v);
                db.flush();

                userBaru = createOrResetVendorUser(db, v, passwordPlain);
                tx.commit(); tx = null;

                emailTerkirim = sendCredentialEmail(request, v, userBaru, passwordPlain);

                res.put("status", "success");
                res.put("username", userBaru.getUserId());
                res.put("email_sent", emailTerkirim);
                res.put("message", emailTerkirim
                        ? cfg("vendor_pesan_pendaftaran_sukses_email", "Pendaftaran berhasil. Username dan password telah dikirim ke email kontak vendor.")
                        : cfg("vendor_pesan_pendaftaran_sukses_email_gagal", "Pendaftaran berhasil dan akun vendor telah dibuat, tetapi email kredensial belum berhasil dikirim. Silakan hubungi admin pengadaan."));
            }
        }
    }
    else if ("resend_credentials".equals(aksi)) {
        String email = nvl(request.getParameter("email"));
        if (empty(email)) {
            res.put("status", "error");
            res.put("message", "Masukkan email kontak vendor yang sudah terdaftar.");
        } else if (!Common.isValidEmailAddress(email.split(",")[0].trim())) {
            res.put("status", "error");
            res.put("message", "Format email tidak valid.");
        } else {
            PenyediaAsset v = findVendorByEmail(db, email);
            if (v == null) {
                res.put("status", "error");
                res.put("message", "Email belum terdaftar sebagai vendor. Silakan lakukan pendaftaran vendor terlebih dahulu.");
            } else {
                String passwordPlain = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
                Tbmuser userBaru = null;
                boolean emailTerkirim = false;
                tx = db.beginTransaction();
                if (empty(v.getEmail())) v.setEmail(email);
                userBaru = createOrResetVendorUser(db, v, passwordPlain);
                db.update(v);
                tx.commit(); tx = null;
                emailTerkirim = sendCredentialEmail(request, v, userBaru, passwordPlain);
                if (emailTerkirim) {
                    res.put("status", "success");
                    res.put("message", "Informasi username dan password terbaru berhasil dikirim ulang ke email vendor terdaftar. Silakan cek Inbox, Spam, atau Junk Mail.");
                } else {
                    res.put("status", "error");
                    res.put("message", "Akun vendor ditemukan dan password sudah diperbarui, tetapi email belum berhasil dikirim. Silakan hubungi admin pengadaan untuk bantuan pengiriman ulang.");
                }
            }
        }
    }
    else if ("login".equals(aksi)) {
        String username = nvl(request.getParameter("username"));
        String password = nvl(request.getParameter("password"));
        PenyediaAsset vendor = null;
        Tbmuser user = null;
        try {
            List users = db.createCriteria(Tbmuser.class)
                .add(Restrictions.or(Restrictions.eq("userId", username), Restrictions.ilike("email", username, MatchMode.ANYWHERE)))
                .setMaxResults(5).list();
            for (int i=0; i<users.size(); i++) {
                Tbmuser u = (Tbmuser) users.get(i);
                try {
                    String p = Common.desEncrypter.get().decrypt(u.getUserPassword());
                    if (password.equals(p) && u.getPenyediaAsset() != null) { user = u; vendor = u.getPenyediaAsset(); break; }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:609");}
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:611");}
        if (vendor == null) {
            try {
                String enc = Common.desEncrypter.get().encrypt(password);
                vendor = (PenyediaAsset) db.createCriteria(PenyediaAsset.class)
                    .add(Restrictions.ilike("email", username, MatchMode.ANYWHERE))
                    .add(Restrictions.eq("keterangan", enc)).setMaxResults(1).uniqueResult();
            } catch(Exception e) { vendor = null; }
        }
        if (vendor == null) { res.put("status", "error"); res.put("message", "Email/username atau password salah."); }
        else if (!vendor.getAktif()) { res.put("status", "error"); res.put("message", "Akun vendor belum aktif atau belum disetujui admin."); }
        else {
            request.getSession().setAttribute("VENDOR_LOGGED_IN", vendor);
            if (user != null) request.getSession().setAttribute("VENDOR_USER_LOGGED_IN", user);
            if (cfgAktif("vendor_login_gunakan_cookie", "Tidak Aktif")) {
                try {
                    tx = db.beginTransaction();
                    PenyediaAsset vCookie = (PenyediaAsset) db.get(PenyediaAsset.class, vendor.getId());
                    JSONObject extraCookie = readExtra(vCookie);
                    String token = RandomStringUtils.randomAlphanumeric(64);
                    extraCookie.put("cookieToken", token);
                    extraCookie.put("cookieTokenTanggal", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    vCookie.setFormula(extraCookie.toString());
                    db.update(vCookie);
                    tx.commit(); tx = null;
                    int hari = cfgInt("vendor_login_cookie_hari", 7);
                    int maxAge = hari <= 0 ? -1 : hari * 24 * 60 * 60;
                    setVendorCookie(request, response, "VENDOR_ID", String.valueOf(vCookie.getId()), maxAge);
                    setVendorCookie(request, response, "VENDOR_TOKEN", token, maxAge);
                    request.getSession().setAttribute("VENDOR_LOGGED_IN", vCookie);
                    vendor = vCookie;
                } catch(Exception cookieError) {
                    try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:643");}
                    tx = null;
                    clearVendorCookie(request, response, "VENDOR_ID");
                    clearVendorCookie(request, response, "VENDOR_TOKEN");
                }
            } else {
                clearVendorCookie(request, response, "VENDOR_ID");
                clearVendorCookie(request, response, "VENDOR_TOKEN");
            }
            res.put("status", "success"); res.put("vendor_id", vendor.getId());
        }
    }
    else if ("logout".equals(aksi)) {
        try { request.getSession().removeAttribute("VENDOR_LOGGED_IN"); request.getSession().removeAttribute("VENDOR_USER_LOGGED_IN"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:656");}
        clearVendorCookie(request, response, "VENDOR_ID");
        clearVendorCookie(request, response, "VENDOR_TOKEN");
        res.put("status", "success");
    }
    else if ("get_profile".equals(aksi)) {
        PenyediaAsset sv = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN");
        if (sv == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            PenyediaAsset v = (PenyediaAsset) db.get(PenyediaAsset.class, sv.getId());
            JSONObject extra = readExtra(v);
            int total = 0, verified = 0, revisi = 0, kedaluwarsa = 0, segera = 0;
            try {
                List docs = db.createCriteria(PenyediaAssetPunyaDokumen.class).add(Restrictions.eq("penyediaAsset", v)).list();
                total = docs.size();
                long now = System.currentTimeMillis();
                for(int i=0;i<docs.size();i++){
                    PenyediaAssetPunyaDokumen d=(PenyediaAssetPunyaDokumen)docs.get(i);
                    if(PenyediaAssetPunyaDokumen.VERIFIKASI.equals(d.getStatus())) verified++;
                    if(PenyediaAssetPunyaDokumen.REVISI.equals(d.getStatus())) revisi++;
                    java.util.Date bs = d.getTanggalBerlakuSampai();
                    if(bs != null){ long sisa = (bs.getTime()-now)/86400000L; if(sisa < 0) kedaluwarsa++; else if(sisa <= 30) segera++; }
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:679");}
            JSONObject stat = new JSONObject(); stat.put("profil", profilScore(v, extra)); stat.put("dokumenTotal", total); stat.put("dokumenTerverifikasi", verified); stat.put("dokumenRevisi", revisi); stat.put("dokumenKedaluwarsa", kedaluwarsa); stat.put("dokumenSegeraKedaluwarsa", segera);
            String notif = total == 0 ? "Template dokumen vendor belum dikonfigurasi admin." : (revisi > 0 ? "Ada dokumen yang harus direvisi." : "Lengkapi seluruh dokumen wajib dan tunggu verifikasi admin.");
            if (kedaluwarsa > 0) notif = "<span class='text-danger fw-bold'><i class='fas fa-triangle-exclamation me-1'></i>" + kedaluwarsa + " dokumen sudah kedaluwarsa</span> dan perlu diperbarui. " + notif;
            else if (segera > 0) notif = "<span class='text-warning fw-bold'><i class='fas fa-clock me-1'></i>" + segera + " dokumen akan kedaluwarsa dalam 30 hari.</span> " + notif;
            stat.put("notifikasi", notif);
            res.put("status", "success"); res.put("vendor", vendorJson(v)); res.put("extra", extra); res.put("stat", stat);
        }
    }
    else if ("update_profile".equals(aksi)) {
        PenyediaAsset sv = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN");
        if (sv == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            tx = db.beginTransaction();
            PenyediaAsset v = (PenyediaAsset) db.get(PenyediaAsset.class, sv.getId());
            String tipe = nvl(request.getParameter("tipe_update"));
            JSONObject extra = readExtra(v);
            if ("profil".equals(tipe)) {
                v.setNama(nvl(request.getParameter("nama"))); v.setPemilik(nvl(request.getParameter("pemilik"))); v.setKontak(nvl(request.getParameter("kontak"))); v.setEmail(nvl(request.getParameter("email"))); v.setTelp(nvl(request.getParameter("telp"))); v.setKodePos(nvl(request.getParameter("kodePos"))); v.setAlamat(nvl(request.getParameter("alamat")));
            } else if ("legal".equals(tipe)) {
                v.setNpwp(nvl(request.getParameter("npwp"))); v.setNoAktaPendirian(nvl(request.getParameter("noAktaPendirian"))); v.setNamaNotaris(nvl(request.getParameter("namaNotaris"))); v.setNoPengesahan(nvl(request.getParameter("noPengesahan"))); v.setNoRek(nvl(request.getParameter("noRek"))); v.setAtasNama(nvl(request.getParameter("atasNama")));
                putIfParam(extra, request, "nib"); putIfParam(extra, request, "pkp"); putIfParam(extra, request, "blacklist"); putIfParam(extra, request, "bankNama"); putIfParam(extra, request, "pernyataan");
            } else if ("khusus".equals(tipe)) {
                putIfParam(extra, request, "bidang"); putIfParam(extra, request, "pengalaman"); putIfParam(extra, request, "izinKhusus"); putIfParam(extra, request, "tenagaAhli"); putIfParam(extra, request, "purnaJual"); putIfParam(extra, request, "dukungan"); putIfParam(extra, request, "produkJasa");
            }
            v.setFormula(extra.toString()); db.update(v); tx.commit(); tx = null;
            request.getSession().setAttribute("VENDOR_LOGGED_IN", v);
            res.put("status", "success"); res.put("message", "Data vendor berhasil diperbarui.");
        }
    }
    else if ("list_dokumen".equals(aksi)) {
        PenyediaAsset sv = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN");
        if (sv == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            tx = db.beginTransaction();
            PenyediaAsset v = (PenyediaAsset) db.get(PenyediaAsset.class, sv.getId());
            try {
                List templates = db.createCriteria(DokumenPenyediaAsset.class).addOrder(Order.asc("id")).list();
                for(int i=0; i<templates.size(); i++) {
                    DokumenPenyediaAsset t = (DokumenPenyediaAsset) templates.get(i);
                    Object exists = db.createCriteria(PenyediaAssetPunyaDokumen.class).add(Restrictions.eq("penyediaAsset", v)).add(Restrictions.eq("dokumenPenyediaAsset", t)).setMaxResults(1).uniqueResult();
                    if (exists == null) { PenyediaAssetPunyaDokumen d = new PenyediaAssetPunyaDokumen(); d.setPenyediaAsset(v); d.setDokumenPenyediaAsset(t); d.setStatus(PenyediaAssetPunyaDokumen.BELUM); db.save(d); }
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:722");}
            tx.commit(); tx = null;
            JSONArray arr = new JSONArray();
            List docs = db.createCriteria(PenyediaAssetPunyaDokumen.class).createAlias("dokumenPenyediaAsset", "dok", org.hibernate.Criteria.LEFT_JOIN).add(Restrictions.eq("penyediaAsset", v)).addOrder(Order.asc("dok.id")).list();
            for(int i=0;i<docs.size();i++){
                PenyediaAssetPunyaDokumen d = (PenyediaAssetPunyaDokumen) docs.get(i); Object dok = d.getDokumenPenyediaAsset();
                JSONObject o = new JSONObject(); o.put("id", d.getId()); o.put("status", d.getStatus()); o.put("keterangan", nvl(d.getKeterangan()));
                o.put("kode", nvl(String.valueOf(safeInvoke(dok, "getKode")))); o.put("nama", nvl(String.valueOf(safeInvoke(dok, "getNama")))); o.put("wajib", safeBool(dok, "getWajib", false));
                String ket = nvl(d.getKeterangan()); String link = "";
                int p = ket.indexOf("FILE:"); if (p >= 0) { int end = ket.indexOf("|", p); link = end > p ? ket.substring(p+5, end).trim() : ket.substring(p+5).trim(); }
                // Masa berlaku dokumen (selaras modul admin) + tanda kedaluwarsa / segera kedaluwarsa (≤30 hari).
                java.util.Date bs = d.getTanggalBerlakuSampai();
                if (bs != null) {
                    o.put("berlakuSampai", new java.text.SimpleDateFormat("dd-MM-yyyy").format(bs));
                    o.put("berlakuIso", new java.text.SimpleDateFormat("yyyy-MM-dd").format(bs));
                    long sisaHari = (bs.getTime() - System.currentTimeMillis()) / 86400000L;
                    o.put("kedaluwarsa", sisaHari < 0);
                    o.put("segeraKedaluwarsa", sisaHari >= 0 && sisaHari <= 30);
                } else {
                    o.put("berlakuSampai", ""); o.put("berlakuIso", ""); o.put("kedaluwarsa", false); o.put("segeraKedaluwarsa", false);
                }
                o.put("link", link); arr.put(o);
            }
            res.put("status", "success"); res.put("data", arr);
        }
    }
    else if ("upload_dokumen".equals(aksi)) {
        PenyediaAsset sv = (PenyediaAsset) request.getSession().getAttribute("VENDOR_LOGGED_IN");
        if (sv == null) { res.put("status", "error"); res.put("message", "Sesi login berakhir."); }
        else {
            Long id = Long.valueOf(nvl(request.getParameter("id")));
            String catatan = nvl(request.getParameter("catatan"));
            Part filePart = null;
            try { filePart = request.getPart("file"); } catch(Exception e) { filePart = null; }
            tx = db.beginTransaction();
            PenyediaAssetPunyaDokumen d = (PenyediaAssetPunyaDokumen) db.get(PenyediaAssetPunyaDokumen.class, id);
            if (d == null || d.getPenyediaAsset() == null || !d.getPenyediaAsset().getId().equals(sv.getId())) { res.put("status", "error"); res.put("message", "Dokumen tidak valid."); }
            else {
                String link = "";
                if (filePart != null && filePart.getSize() > 0) {
                    String original = cleanFilename(getSubmittedFileName(filePart));
                    File dir = new File(Common.REAL_PATH + "/media/vendor_documents/" + sv.getId()); dir.mkdirs();
                    String fname = System.currentTimeMillis() + "_" + original;
                    File outFile = new File(dir, fname);
                    InputStream in = null; FileOutputStream fos = null;
                    try {
                        in = filePart.getInputStream(); fos = new FileOutputStream(outFile); byte[] buf = new byte[8192]; int len; while((len=in.read(buf))>0){ fos.write(buf,0,len); }
                    } finally {
                        try { if (fos != null) fos.close(); } catch (Exception ioClose) { ais.common.ErrorAuditUtil.record(ioClose, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:770");}
                        try { if (in != null) in.close(); } catch (Exception ioClose) { ais.common.ErrorAuditUtil.record(ioClose, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:771");}
                    }
                    link = Common.ROOT + "/media/vendor_documents/" + sv.getId() + "/" + fname;
                }
                String existing = nvl(d.getKeterangan());
                String ket = (link.isEmpty() ? existing : "FILE:" + link) + (empty(catatan) ? "" : " | CATATAN:" + catatan);
                d.setKeterangan(ket); d.setStatus(PenyediaAssetPunyaDokumen.BELUM);
                // Masa berlaku dokumen (opsional) yang diisi vendor saat unggah; format input HTML = yyyy-MM-dd.
                String berlakuStr = nvl(request.getParameter("berlaku_sampai"));
                if (!berlakuStr.isEmpty()) {
                    try { d.setTanggalBerlakuSampai(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(berlakuStr)); } catch (Exception eDate) { ais.common.ErrorAuditUtil.record(eDate, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:781");}
                }
                db.update(d);
                res.put("status", "success"); res.put("message", "Dokumen berhasil dikirim. Status akan diverifikasi admin.");
            }
            tx.commit(); tx = null;
        }
    }
    else { res.put("status", "error"); res.put("message", "Aksi tidak dikenali."); }
} catch(Exception e) {
    try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:791");}
    try { res.put("status", "error"); res.put("message", e.getMessage() == null ? e.toString() : e.getMessage()); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:792");}
} finally {
    try { if (db != null) db.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/vendor/_vendor_service.jsp:794");}
}
out.print(res.toString());
out.flush();
%>
