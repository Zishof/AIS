package ais.common.newui.ticket;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import ais.action.master.ticket.TicketKonfigurasiAction;
import ais.action.master.ticket.TicketNotifikasi;
import ais.action.master.ticket.TicketPdf;
import ais.action.master.ticket.TicketingAction;
import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiPermission;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.NewUiUnggahRequest;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.ticket.Ticket;
import ais.database.model.ticket.TicketKategori;
import ais.database.model.ticket.TicketKomentar;

/**
 * Kontrak native tiga menu Ticketing: daftar/operasional, dashboard, dan
 * konfigurasi.
 *
 * <p>Controller ini sengaja tidak memakai Generic CRUD. {@link Ticket} wajib
 * dibatasi oleh {@link TicketingAction#scopedCriteria(Session, Tbmuser)},
 * komentar internal hanya dapat dibaca pengelola, dan pembaruan status hanya
 * boleh dilakukan administrator/developer atau petugas yang ditugaskan.
 * Menampilkan entity mentah akan melewati semua aturan tersebut.</p>
 *
 * <p>Semua mutasi memakai POST, privilege menu, token CSRF, dan pemeriksaan
 * scope ulang. Respons baca tidak mengandung token sesi selain CSRF yang hanya
 * diterbitkan oleh aksi {@code meta}.</p>
 */
public final class NewUiTicketController {

    private static final String MODULE = "ticket";
    public static final String MODE_TICKETING = "ticketing";
    public static final String MODE_DASHBOARD = "ticket_dashboard";
    public static final String MODE_KONFIGURASI = "ticket_konfigurasi";
    private static final int BATAS_DAFTAR = 300;
    private static final int BATAS_DASHBOARD = 5000;
    private static final long REF_LAMPIRAN_DIHAPUS = -111111119L;

    private static final String[] STATUS = {
        Ticket.STATUS_BARU, Ticket.STATUS_DITINJAU, Ticket.STATUS_DIPROSES,
        Ticket.STATUS_MENUNGGU_RESPON, Ticket.STATUS_SELESAI,
        Ticket.STATUS_DITUTUP, Ticket.STATUS_DITOLAK
    };
    private static final String[] TIPE = {
        Ticket.TIPE_KENDALA, Ticket.TIPE_PERMINTAAN, Ticket.TIPE_PROGRESS,
        Ticket.TIPE_INTERAKSI, Ticket.TIPE_LAINNYA
    };
    private static final String[] PRIORITAS = {
        Ticket.PRIORITAS_RENDAH, Ticket.PRIORITAS_SEDANG,
        Ticket.PRIORITAS_TINGGI, Ticket.PRIORITAS_KRITIS
    };

    private NewUiTicketController() {
    }

    public static void handle(HttpServletRequest request,
            HttpServletResponse response, String mode) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) {
                throw new IllegalArgumentException("Mode Ticketing tidak dikenal.");
            }
            String action = text(request.getParameter("action"), "meta").toLowerCase();
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, mode, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses aksi tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null || user.getUserId() == null) {
                throw new SecurityException("Sesi pengguna tidak dikenal.");
            }

            if (MODE_TICKETING.equals(mode) && "export_attachment".equals(action)) {
                unduhLampiran(request, response, user);
                return;
            }
            if (MODE_TICKETING.equals(mode) && "export_pdf".equals(action)) {
                cetakPdf(request, response, user);
                return;
            }

            if ("meta".equals(action)) {
                meta(json, request, mode, user);
            } else if (MODE_TICKETING.equals(mode)) {
                operasional(json, request, action, user);
            } else if (MODE_DASHBOARD.equals(mode)) {
                dashboard(json, request, action, user);
            } else {
                konfigurasi(json, request, action, user);
            }
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR",
                    "Gagal memproses Ticketing. Detail dicatat di log server.");
            try {
                ais.common.ErrorAuditUtil.record(e, "NewUiTicketController");
            } catch (Exception ignored) {
            }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_TICKETING.equals(mode) || MODE_DASHBOARD.equals(mode)
                || MODE_KONFIGURASI.equals(mode);
    }

    private static void meta(JSONObject j, HttpServletRequest request,
            String mode, Tbmuser user) throws Exception {
        NewUiPermission izin = NewUiRouteGuard.permissionFor(request, MODULE, mode);
        boolean pengelola = TicketingAction.bolehKelolaSemua(user);
        j.put("mode", mode);
        j.put("judul", judul(mode));
        j.put("bolehTambah", izin != null && izin.isCanCreate());
        j.put("bolehUbah", izin != null && izin.isCanUpdate());
        j.put("bolehHapus", izin != null && izin.isCanDelete());
        j.put("pengelolaSemua", pengelola);
        j.put("modulAktif", nilaiBoolean(TicketKonfigurasiAction.KEY_AKTIF, true));
        j.put("wajibSop", nilaiBoolean(TicketKonfigurasiAction.KEY_WAJIB_SOP, true));
        j.put("fabAktif", Konfigurasi.AKTIF.equalsIgnoreCase(
                nilaiKonfigurasi(TicketKonfigurasiAction.KEY_FAB,
                        Konfigurasi.TIDAK_AKTIF)));
        j.put("roleDeveloper", nilaiKonfigurasi(
                TicketKonfigurasiAction.KEY_ROLE_DEV, ""));
        j.put("pilihanStatus", array(STATUS));
        j.put("pilihanTipe", array(TIPE));
        j.put("pilihanPrioritas", array(PRIORITAS));
        j.put("kategori", kategori());
        j.put("satuanKerja", satuanKerja());
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));

        // CRM mempunyai lifecycle sendiri dan tetap dinyatakan eksplisit agar
        // klien tidak membuat tombol palsu. Lampiran/PDF sudah memiliki kontrak.
        j.put("crmNative", false);
        j.put("lampiranNative", true);
        j.put("cetakPdfNative", true);
        j.put("maxUploadBytes", maxUploadBytes());
    }

    private static String judul(String mode) {
        if (MODE_DASHBOARD.equals(mode)) {
            return "Dashboard Ticketing";
        }
        if (MODE_KONFIGURASI.equals(mode)) {
            return "Konfigurasi Ticketing";
        }
        return "Ticketing Management";
    }

    private static void operasional(JSONObject j, HttpServletRequest r,
            String action, Tbmuser user) throws Exception {
        if ("list".equals(action)) {
            daftar(j, r, user);
        } else if ("detail".equals(action)) {
            detail(j, r, user);
        } else if ("create".equals(action)) {
            wajibCsrf(r);
            String kind = text(r.getParameter("kind"), "ticket");
            if ("comment".equals(kind)) {
                tambahKomentar(j, r, user);
            } else if ("ticket".equals(kind)) {
                tambahTicket(j, r, user);
            } else {
                throw new IllegalArgumentException("Jenis pembuatan tidak dikenal.");
            }
        } else if ("update".equals(action)) {
            wajibCsrf(r);
            ubahTicket(j, r, user);
        } else if ("upload".equals(action)) {
            wajibCsrf(r);
            unggahLampiran(j, r, user);
        } else if ("delete".equals(action)
                && "attachment".equals(text(r.getParameter("kind"), ""))) {
            wajibCsrf(r);
            hapusLampiran(j, r, user);
        } else {
            throw new IllegalArgumentException("Aksi Ticketing tidak dikenal.");
        }
    }

    @SuppressWarnings("unchecked")
    private static void daftar(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = TicketingAction.scopedCriteria(s, user);
            String q = text(r.getParameter("q"), "");
            String status = text(r.getParameter("status"), "");
            String tipe = text(r.getParameter("tipe"), "");
            if (q.length() > 0) {
                c.add(Restrictions.or(
                        Restrictions.ilike("judul", q, MatchMode.ANYWHERE),
                        Restrictions.or(
                                Restrictions.ilike("deskripsi", q, MatchMode.ANYWHERE),
                                Restrictions.ilike("nomorTiket", q, MatchMode.ANYWHERE))));
            }
            if (status.length() > 0) {
                wajibPilihan(status, STATUS, "Status tidak sah.");
                c.add(Restrictions.eq("status", status));
            }
            if (tipe.length() > 0) {
                wajibPilihan(tipe, TIPE, "Tipe tiket tidak sah.");
                c.add(Restrictions.eq("tipe", tipe));
            }
            List<Ticket> rows = c.setMaxResults(BATAS_DAFTAR).list();
            JSONArray data = new JSONArray();
            for (Ticket t : rows) {
                data.put(ticketRingkas(t, user));
            }
            j.put("rows", data);
            j.put("total", data.length());
            j.put("dibatasi", rows.size() >= BATAS_DAFTAR);
        } finally {
            s.close();
        }
    }

    private static void detail(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        Session s = HibernateUtil.openSession();
        try {
            Ticket t = ticketDalamScope(s, user, id(r));
            boolean kelola = bolehKelola(t, user);
            JSONObject data = ticketRingkas(t, user);
            data.put("deskripsi", nz(t.getDeskripsi()));
            data.put("modul", nz(t.getModul()));
            data.put("kategori", t.getTicketKategori() == null
                    ? "" : nz(t.getTicketKategori().getNama()));
            data.put("kategoriId", t.getTicketKategori() == null
                    ? JSONObject.NULL : t.getTicketKategori().getId());
            data.put("satuanKerja", t.getSatuanKerja() == null
                    ? "" : nz(t.getSatuanKerja().getNama()));
            data.put("tanggalTarget", tanggal(t.getTanggalTarget()));
            data.put("tanggalSelesai", tanggal(t.getTanggalSelesai()));
            data.put("ditugaskanKe", nz(t.getDitugaskanKeNama()));
            data.put("bolehKelola", kelola);

            @SuppressWarnings("unchecked")
            List<TicketKomentar> comments = s.createCriteria(TicketKomentar.class)
                    .add(Restrictions.eq("ticket", t)).addOrder(Order.asc("id")).list();
            List<Long> commentIds = new ArrayList<Long>();
            for (TicketKomentar k : comments) {
                if (!Boolean.TRUE.equals(k.getInternal()) || kelola) commentIds.add(k.getId());
            }
            Map<Long, LampiranLain> commentAttachments = lampiranKomentar(s, commentIds);
            JSONArray komentar = new JSONArray();
            for (TicketKomentar k : comments) {
                if (Boolean.TRUE.equals(k.getInternal()) && !kelola) {
                    continue;
                }
                komentar.put(new JSONObject()
                        .put("id", k.getId())
                        .put("isi", nz(k.getIsi()))
                        .put("nama", nz(k.getNama()))
                        .put("tipePengguna", nz(k.getTipePengguna()))
                        .put("internal", Boolean.TRUE.equals(k.getInternal()))
                        .put("tanggal", tanggal(k.getTanggal()))
                        .put("attachment", lampiranJson(commentAttachments.get(k.getId()))));
            }
            data.put("attachment", lampiranJson(lampiran(
                    s, t.getId(), TicketingAction.LAMPIRAN_TIKET)));
            data.put("komentar", komentar);
            j.put("data", data);
        } finally {
            s.close();
        }
    }

    private static JSONObject ticketRingkas(Ticket t, Tbmuser user)
            throws Exception {
        return new JSONObject()
                .put("id", t.getId())
                .put("nomor", nz(t.getNomorTiket()))
                .put("judul", nz(t.getJudul()))
                .put("tipe", nz(t.getTipe()))
                .put("prioritas", nz(t.getPrioritas()))
                .put("status", nz(t.getStatus()))
                .put("progress", t.getProgress() == null ? 0 : t.getProgress())
                .put("pengaju", nz(t.getPengajuNama()))
                .put("pengajuTipe", nz(t.getPengajuTipe()))
                .put("tanggalDibuat", tanggal(t.getTanggalDibuat()))
                .put("bolehKelola", bolehKelola(t, user));
    }

    private static void tambahTicket(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        if (nilaiBoolean(TicketKonfigurasiAction.KEY_WAJIB_SOP, true)) {
            throw new IllegalArgumentException(
                    "Pengajuan tiket wajib melalui menu Pengajuan SOP (Workflow).");
        }
        String judul = wajib(r, "judul", "Judul tiket wajib diisi.");
        String tipe = text(r.getParameter("tipe"), Ticket.TIPE_KENDALA);
        String prioritas = text(r.getParameter("prioritas"), Ticket.PRIORITAS_SEDANG);
        wajibPilihan(tipe, TIPE, "Tipe tiket tidak sah.");
        wajibPilihan(prioritas, PRIORITAS, "Prioritas tiket tidak sah.");

        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        Ticket t = new Ticket();
        try {
            tx = s.beginTransaction();
            t.setJudul(judul);
            t.setDeskripsi(text(r.getParameter("deskripsi"), ""));
            t.setTipe(tipe);
            t.setPrioritas(prioritas);
            t.setModul(text(r.getParameter("modul"), ""));
            t.setStatus(Ticket.STATUS_BARU);
            t.setProgress(Integer.valueOf(0));
            t.setAktif(Boolean.TRUE);
            t.setTanggalDibuat(new Date());
            t.setPengajuUserId(user.getUserId());
            t.setPengajuNama(user.getUserNama());
            t.setPengajuTipe(TicketingAction.tipePengguna(user));
            t.setPengajuEmail(user.getEmail());
            t.setPengajuHp(user.getHp());
            try {
                if (user.hakAkses() != null && user.hakAkses().getRoleId() != null) {
                    t.setHakAksesTarget("," + user.hakAkses().getRoleId() + ",");
                }
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "NewUiTicketController.create.role");
            }
            Long kategoriId = idOpsional(r.getParameter("kategoriId"));
            if (kategoriId != null) {
                TicketKategori kategori = (TicketKategori) s.get(TicketKategori.class, kategoriId);
                if (kategori == null || !Boolean.TRUE.equals(kategori.getAktif())) {
                    throw new IllegalArgumentException("Kategori tiket tidak ditemukan.");
                }
                t.setTicketKategori(kategori);
            }
            Long satuanKerjaId = idOpsional(r.getParameter("satuanKerjaId"));
            if (satuanKerjaId != null) {
                SatuanKerja unit = (SatuanKerja) s.get(SatuanKerja.class, satuanKerjaId);
                if (unit == null) {
                    throw new IllegalArgumentException("Satuan kerja tidak ditemukan.");
                }
                t.setSatuanKerja(unit);
            }
            s.save(t);
            s.flush();
            t.setNomorTiket("TKT-" + t.getId());
            s.update(t);
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            s.close();
        }
        TicketNotifikasi.tiketBaru(t);
        j.put("data", new JSONObject().put("id", t.getId())
                .put("nomor", nz(t.getNomorTiket())));
        j.put("message", "Tiket berhasil dibuat.");
    }

    private static void tambahKomentar(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        String isi = wajib(r, "isi", "Isi komentar wajib diisi.");
        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        Ticket t;
        TicketKomentar k = new TicketKomentar();
        boolean kelola;
        try {
            t = ticketDalamScope(s, user, id(r));
            kelola = bolehKelola(t, user);
            tx = s.beginTransaction();
            k.setTicket(t);
            k.setIsi(isi);
            k.setInternal(kelola && bool(r.getParameter("internal")));
            k.setTanggal(new Date());
            k.setUserId(user.getUserId());
            k.setNama(user.getUserNama());
            k.setTipePengguna(TicketingAction.tipePengguna(user));
            s.save(k);
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            s.close();
        }
        TicketNotifikasi.komentarBaru(t, k, kelola);
        j.put("message", "Komentar berhasil dikirim.");
    }

    private static void ubahTicket(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        String status = wajib(r, "status", "Status wajib dipilih.");
        String prioritas = wajib(r, "prioritas", "Prioritas wajib dipilih.");
        wajibPilihan(status, STATUS, "Status tiket tidak sah.");
        wajibPilihan(prioritas, PRIORITAS, "Prioritas tiket tidak sah.");
        int progress = angka(r.getParameter("progress"), 0, 100,
                "Progress harus di antara 0 sampai 100.");

        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        Ticket t;
        String statusLama;
        try {
            t = ticketDalamScope(s, user, id(r));
            if (!bolehKelola(t, user)) {
                throw new SecurityException("Anda tidak berhak mengelola tiket ini.");
            }
            statusLama = t.getStatus();
            tx = s.beginTransaction();
            t.setStatus(status);
            t.setPrioritas(prioritas);
            t.setProgress(Integer.valueOf(progress));
            if (Ticket.STATUS_SELESAI.equals(status) && t.getTanggalSelesai() == null) {
                t.setTanggalSelesai(new Date());
            }
            s.update(t);
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            s.close();
        }
        if (statusLama != null && !statusLama.equals(status)) {
            TicketNotifikasi.statusBerubah(t, statusLama);
        }
        j.put("message", "Perubahan tiket tersimpan.");
    }

    /** Unggah satu lampiran aktif, mengikuti lifecycle LampiranLain halaman ZK. */
    private static void unggahLampiran(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        if (!(r instanceof NewUiUnggahRequest)) {
            throw new IllegalArgumentException("Permintaan upload tidak membawa berkas.");
        }
        NewUiUnggahRequest upload = (NewUiUnggahRequest) r;
        File file = upload.getBerkas();
        try {
            if (file == null || !file.exists() || file.length() <= 0L) {
                throw new IllegalArgumentException("Berkas kosong atau tidak dapat dibaca.");
            }
            if (file.length() > maxUploadBytes()) {
                throw new IllegalArgumentException(
                        "Ukuran berkas melampaui batas upload institusi.");
            }
            AttachmentTarget target = attachmentTarget(r, user);
            Session helperSession = HibernateUtil.openSession();
            LampiranLain dibuat;
            try {
                dibuat = (LampiranLain) FileFotoLain.createFileFotoLain(
                        user, helperSession, LampiranLain.class, Boolean.FALSE,
                        target.ref, target.jenis, null, file,
                        safeFileName(upload.getNamaBerkas()));
            } finally {
                HibernateUtil.closeSessionQuietly(helperSession);
            }
            if (dibuat == null || dibuat.getId() == null) {
                throw new IllegalArgumentException("Lampiran gagal disimpan.");
            }

            // Helper legacy memakai nama File temporary. Pulihkan nama asli
            // setelah BLOB berhasil ditulis pada koneksi streaming yang aman.
            Session s = HibernateUtil.openSession();
            Transaction tx = null;
            try {
                tx = s.beginTransaction();
                LampiranLain item = (LampiranLain) s.get(LampiranLain.class, dibuat.getId());
                if (item == null) throw new IllegalArgumentException("Lampiran gagal dimuat kembali.");
                item.setNama(safeFileName(upload.getNamaBerkas()));
                item.setKeterangan(limited(r.getParameter("keterangan"), 1000));
                s.update(item);
                s.flush();
                tx.commit();
                j.put("attachment", lampiranJson(item));
            } catch (Exception e) {
                rollback(tx);
                throw e;
            } finally {
                HibernateUtil.closeSessionQuietly(s);
            }
            j.put("message", "Lampiran berhasil diunggah.");
        } finally {
            try { if (file != null && file.exists()) file.delete(); }
            catch (Exception ignored) { }
        }
    }

    private static void hapusLampiran(JSONObject j, HttpServletRequest r,
            Tbmuser user) throws Exception {
        AttachmentTarget target = attachmentTarget(r, user);
        Long attachmentId = idOpsional(r.getParameter("attachmentId"));
        if (attachmentId == null) throw new IllegalArgumentException("Id lampiran wajib dikirim.");
        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            LampiranLain item = lampiranMilik(s, attachmentId, target);
            tx = s.beginTransaction();
            item.setRef(REF_LAMPIRAN_DIHAPUS);
            s.update(item);
            s.flush();
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(s);
        }
        try { LampiranLain.resetLokasi(Boolean.FALSE, target.ref, target.jenis); }
        catch (Exception ignored) { }
        j.put("message", "Lampiran berhasil dihapus.");
    }

    private static void unduhLampiran(HttpServletRequest r,
            HttpServletResponse response, Tbmuser user) throws Exception {
        AttachmentTarget target = attachmentTarget(r, user);
        Long attachmentId = idOpsional(r.getParameter("attachmentId"));
        if (attachmentId == null) throw new IllegalArgumentException("Id lampiran wajib dikirim.");
        Session s = HibernateUtil.openSession();
        LampiranLain item;
        String nama;
        try {
            item = lampiranMilik(s, attachmentId, target);
            nama = attachmentName(item);
        } finally {
            HibernateUtil.closeSessionQuietly(s);
        }
        byte[] isi = FileFotoLain.ambilIsiBlob(item);
        if (isi == null || isi.length == 0) {
            throw new IllegalArgumentException("Isi lampiran tidak tersedia di penyimpanan.");
        }
        response.setContentType(mime(nama));
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + headerFileName(nama) + "\"");
        response.setContentLength(isi.length);
        OutputStream out = response.getOutputStream();
        out.write(isi);
        out.flush();
    }

    private static void cetakPdf(HttpServletRequest r,
            HttpServletResponse response, Tbmuser user) throws Exception {
        Session s = HibernateUtil.openSession();
        File pdf = null;
        try {
            Ticket t = ticketDalamScope(s, user, id(r));
            pdf = TicketPdf.cetak(t, bolehKelola(t, user));
            if (pdf == null || !pdf.exists() || pdf.length() <= 0L) {
                throw new IllegalArgumentException("PDF tiket gagal dibuat.");
            }
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + headerFileName("tiket_" + nz(t.getNomorTiket()) + ".pdf") + "\"");
            if (pdf.length() <= Integer.MAX_VALUE) response.setContentLength((int) pdf.length());
            InputStream in = new FileInputStream(pdf);
            try {
                IOUtils.copy(in, response.getOutputStream());
                response.getOutputStream().flush();
            } finally {
                in.close();
            }
        } finally {
            HibernateUtil.closeSessionQuietly(s);
            try { if (pdf != null && pdf.exists()) pdf.delete(); }
            catch (Exception ignored) { }
        }
    }

    private static AttachmentTarget attachmentTarget(HttpServletRequest r,
            Tbmuser user) throws Exception {
        Session s = HibernateUtil.openSession();
        try {
            Ticket ticket = ticketDalamScope(s, user, id(r));
            Long commentId = idOpsional(r.getParameter("commentId"));
            if (commentId == null) {
                return new AttachmentTarget(ticket.getId(),
                        TicketingAction.LAMPIRAN_TIKET);
            }
            TicketKomentar comment = (TicketKomentar) s.createCriteria(TicketKomentar.class)
                    .add(Restrictions.idEq(commentId))
                    .add(Restrictions.eq("ticket", ticket)).setMaxResults(1).uniqueResult();
            if (comment == null) throw new IllegalArgumentException(
                    "Komentar tidak ditemukan pada tiket ini.");
            if (Boolean.TRUE.equals(comment.getInternal()) && !bolehKelola(ticket, user)) {
                throw new SecurityException("Lampiran catatan internal hanya untuk pengelola.");
            }
            return new AttachmentTarget(comment.getId(),
                    TicketingAction.LAMPIRAN_KOMENTAR);
        } finally {
            HibernateUtil.closeSessionQuietly(s);
        }
    }

    private static LampiranLain lampiranMilik(Session s, Long id,
            AttachmentTarget target) {
        LampiranLain item = (LampiranLain) s.createCriteria(LampiranLain.class)
                .add(Restrictions.idEq(id)).add(Restrictions.eq("ref", target.ref))
                .add(Restrictions.eq("jenis", target.jenis))
                .setMaxResults(1).uniqueResult();
        if (item == null) throw new IllegalArgumentException(
                "Lampiran tidak ditemukan pada tiket atau komentar ini.");
        return item;
    }

    private static LampiranLain lampiran(Session s, Long ref, String jenis) {
        return (LampiranLain) s.createCriteria(LampiranLain.class)
                .add(Restrictions.eq("ref", ref)).add(Restrictions.eq("jenis", jenis))
                .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, LampiranLain> lampiranKomentar(Session s,
            List<Long> ids) {
        Map<Long, LampiranLain> hasil = new HashMap<Long, LampiranLain>();
        if (ids == null || ids.isEmpty()) return hasil;
        List<LampiranLain> rows = s.createCriteria(LampiranLain.class)
                .add(Restrictions.in("ref", ids))
                .add(Restrictions.eq("jenis", TicketingAction.LAMPIRAN_KOMENTAR))
                .addOrder(Order.desc("id")).list();
        for (LampiranLain item : rows) {
            if (!hasil.containsKey(item.getRef())) hasil.put(item.getRef(), item);
        }
        return hasil;
    }

    private static Object lampiranJson(LampiranLain item) throws Exception {
        if (item == null) return JSONObject.NULL;
        return new JSONObject().put("id", item.getId())
                .put("name", attachmentName(item))
                .put("description", nz(item.getKeterangan()))
                .put("uploadedAt", tanggal(item.getTanggal_dirubah()))
                .put("uploadedBy", nz(item.getOleh()));
    }

    private static String attachmentName(LampiranLain item) {
        String nama = safeFileName(item == null ? null : item.getNama());
        String prefix = item != null && item.getId() != null ? item.getId() + "_" : "";
        return prefix.length() > 0 && nama.startsWith(prefix)
                ? nama.substring(prefix.length()) : nama;
    }

    private static long maxUploadBytes() {
        long kb = 1024L;
        try {
            kb = Long.parseLong(Common.getKonfigurasi(
                    "ukuran_maksimal_file_diupload", "1024").getNilai());
        } catch (Exception ignored) { }
        if (kb < 1L) kb = 1024L;
        return Math.min(NewUiUnggahRequest.BATAS_UKURAN, kb * 1024L);
    }

    private static String safeFileName(String value) {
        String name = text(value, "lampiran").replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replace('\r', '_').replace('\n', '_').replace('"', '_').trim();
        if (name.length() > 255) name = name.substring(name.length() - 255);
        return name.length() == 0 ? "lampiran" : name;
    }

    private static String headerFileName(String value) {
        return safeFileName(value).replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private static String mime(String name) {
        String n = safeFileName(name).toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (n.endsWith(".doc")) return "application/msword";
        if (n.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (n.endsWith(".xls")) return "application/vnd.ms-excel";
        if (n.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    private static String limited(String value, int max) {
        String result = value == null ? "" : value.trim();
        return result.length() <= max ? result : result.substring(0, max);
    }

    private static final class AttachmentTarget {
        final Long ref;
        final String jenis;

        AttachmentTarget(Long ref, String jenis) {
            this.ref = ref;
            this.jenis = jenis;
        }
    }

    private static void dashboard(JSONObject j, HttpServletRequest r,
            String action, Tbmuser user) throws Exception {
        if ("list".equals(action)) {
            daftar(j, r, user);
            return;
        }
        if (!"ringkasan".equals(action)) {
            throw new IllegalArgumentException("Aksi dashboard tidak dikenal.");
        }
        Session s = HibernateUtil.openSession();
        try {
            @SuppressWarnings("unchecked")
            List<Ticket> tickets = TicketingAction.scopedCriteria(s, user)
                    .setMaxResults(BATAS_DASHBOARD).list();
            Map<String, Integer> perStatus = hitungAwal(STATUS);
            Map<String, Integer> perTipe = hitungAwal(TIPE);
            Map<String, Integer> perPrioritas = hitungAwal(PRIORITAS);
            int selesai = 0;
            int berjalan = 0;
            long jumlahProgress = 0;
            for (Ticket t : tickets) {
                tambah(perStatus, t.getStatus());
                tambah(perTipe, t.getTipe());
                tambah(perPrioritas, t.getPrioritas());
                if (Ticket.STATUS_SELESAI.equals(t.getStatus())
                        || Ticket.STATUS_DITUTUP.equals(t.getStatus())) {
                    selesai++;
                } else if (!Ticket.STATUS_DITOLAK.equals(t.getStatus())) {
                    berjalan++;
                }
                jumlahProgress += t.getProgress() == null ? 0 : t.getProgress().intValue();
            }
            j.put("total", tickets.size());
            j.put("berjalan", berjalan);
            j.put("selesai", selesai);
            j.put("rataProgress", tickets.isEmpty() ? 0
                    : (int) (jumlahProgress / tickets.size()));
            j.put("perStatus", peta(perStatus));
            j.put("perTipe", peta(perTipe));
            j.put("perPrioritas", peta(perPrioritas));
            j.put("dibatasi", tickets.size() >= BATAS_DASHBOARD);
        } finally {
            s.close();
        }
    }

    private static void konfigurasi(JSONObject j, HttpServletRequest r,
            String action, Tbmuser user) throws Exception {
        if ("list".equals(action)) {
            j.put("kategori", kategori());
            return;
        }
        wajibCsrf(r);
        String kind = text(r.getParameter("kind"), "config");
        if ("update".equals(action) && "config".equals(kind)) {
            simpanKonfigurasi(j, r);
        } else if (("create".equals(action) || "update".equals(action))
                && "category".equals(kind)) {
            simpanKategori(j, r, "update".equals(action));
        } else if ("delete".equals(action) && "category".equals(kind)) {
            hapusKategori(j, r);
        } else {
            throw new IllegalArgumentException("Aksi konfigurasi tidak dikenal.");
        }
    }

    private static void simpanKonfigurasi(JSONObject j, HttpServletRequest r)
            throws Exception {
        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = s.beginTransaction();
            simpanNilai(s, TicketKonfigurasiAction.KEY_AKTIF,
                    bool(r.getParameter("modulAktif")) ? "true" : "false");
            simpanNilai(s, TicketKonfigurasiAction.KEY_WAJIB_SOP,
                    bool(r.getParameter("wajibSop")) ? "true" : "false");
            simpanNilai(s, TicketKonfigurasiAction.KEY_FAB,
                    bool(r.getParameter("fabAktif"))
                            ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
            simpanNilai(s, TicketKonfigurasiAction.KEY_ROLE_DEV,
                    text(r.getParameter("roleDeveloper"), "").replace(" ", ""));
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            s.close();
        }
        try {
            MemoryDbUtil.getKonfigurasi().remove(TicketKonfigurasiAction.KEY_AKTIF);
            MemoryDbUtil.getKonfigurasi().remove(TicketKonfigurasiAction.KEY_WAJIB_SOP);
            MemoryDbUtil.getKonfigurasi().remove(TicketKonfigurasiAction.KEY_FAB);
            MemoryDbUtil.getKonfigurasi().remove(TicketKonfigurasiAction.KEY_ROLE_DEV);
        } catch (Throwable cacheError) {
            try {
                MemoryDbUtil.resetLocalReferences();
            } catch (Throwable ignored) {
            }
        }
        j.put("message", "Konfigurasi Ticketing tersimpan.");
    }

    private static void simpanKategori(JSONObject j, HttpServletRequest r,
            boolean mengubah) throws Exception {
        String nama = wajib(r, "nama", "Nama kategori wajib diisi.");
        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = s.beginTransaction();
            TicketKategori kategori = mengubah
                    ? (TicketKategori) s.get(TicketKategori.class, id(r))
                    : new TicketKategori();
            if (kategori == null) {
                throw new IllegalArgumentException("Kategori tidak ditemukan.");
            }
            kategori.setNama(nama);
            kategori.setKeterangan(text(r.getParameter("keterangan"), ""));
            kategori.setWarna(text(r.getParameter("warna"), "#0ea5e9"));
            kategori.setNomorUrut(Integer.valueOf(angka(
                    text(r.getParameter("nomorUrut"), "0"), 0, 100000,
                    "Nomor urut tidak sah.")));
            kategori.setAktif(Boolean.valueOf(!"false".equalsIgnoreCase(
                    text(r.getParameter("aktif"), "true"))));
            if (mengubah) {
                s.update(kategori);
            } else {
                s.save(kategori);
            }
            tx.commit();
            j.put("id", kategori.getId());
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            s.close();
        }
        j.put("message", "Kategori tersimpan.");
    }

    private static void hapusKategori(JSONObject j, HttpServletRequest r)
            throws Exception {
        Session s = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            TicketKategori kategori = (TicketKategori) s.get(TicketKategori.class, id(r));
            if (kategori == null) {
                throw new IllegalArgumentException("Kategori tidak ditemukan.");
            }
            Number dipakai = (Number) s.createCriteria(Ticket.class)
                    .add(Restrictions.eq("ticketKategori", kategori))
                    .setProjection(org.hibernate.criterion.Projections.rowCount())
                    .uniqueResult();
            if (dipakai != null && dipakai.longValue() > 0) {
                throw new IllegalArgumentException(
                        "Kategori masih dipakai tiket dan tidak dapat dihapus.");
            }
            tx = s.beginTransaction();
            s.delete(kategori);
            tx.commit();
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            s.close();
        }
        j.put("message", "Kategori dihapus.");
    }

    private static Ticket ticketDalamScope(Session s, Tbmuser user, Long id)
            throws Exception {
        Ticket t = (Ticket) TicketingAction.scopedCriteria(s, user)
                .add(Restrictions.eq("id", id)).setMaxResults(1).uniqueResult();
        if (t == null) {
            throw new SecurityException("Tiket tidak ditemukan atau di luar lingkup Anda.");
        }
        return t;
    }

    private static boolean bolehKelola(Ticket t, Tbmuser user) {
        return TicketingAction.bolehKelolaSemua(user)
                || (t != null && user != null && user.getUserId() != null
                        && user.getUserId().equals(t.getDitugaskanKeUserId()));
    }

    @SuppressWarnings("unchecked")
    private static JSONArray kategori() throws Exception {
        JSONArray a = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            List<TicketKategori> rows = s.createCriteria(TicketKategori.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"),
                            Restrictions.eq("aktif", Boolean.TRUE)))
                    .addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id")).list();
            for (TicketKategori k : rows) {
                a.put(new JSONObject().put("id", k.getId())
                        .put("nama", nz(k.getNama()))
                        .put("keterangan", nz(k.getKeterangan()))
                        .put("warna", nz(k.getWarna()))
                        .put("nomorUrut", k.getNomorUrut() == null ? 0 : k.getNomorUrut())
                        .put("aktif", Boolean.TRUE.equals(k.getAktif())));
            }
        } finally {
            s.close();
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray satuanKerja() throws Exception {
        JSONArray a = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            List<SatuanKerja> rows = s.createCriteria(SatuanKerja.class)
                    .addOrder(Order.asc("id")).setMaxResults(500).list();
            for (SatuanKerja unit : rows) {
                a.put(new JSONObject().put("id", unit.getId())
                        .put("nama", nz(unit.getNama()))
                        .put("kode", nz(unit.getKode())));
            }
        } finally {
            s.close();
        }
        return a;
    }

    private static String nilaiKonfigurasi(String nama, String bawaan)
            throws Exception {
        Session s = HibernateUtil.openSession();
        try {
            Konfigurasi k = (Konfigurasi) s.createCriteria(Konfigurasi.class)
                    .add(Restrictions.eq("nama", nama)).setMaxResults(1).uniqueResult();
            return k == null || k.getNilai() == null ? bawaan : k.getNilai();
        } finally {
            s.close();
        }
    }

    private static boolean nilaiBoolean(String nama, boolean bawaan)
            throws Exception {
        return "true".equalsIgnoreCase(nilaiKonfigurasi(nama,
                bawaan ? "true" : "false"));
    }

    private static void simpanNilai(Session s, String nama, String nilai) {
        Konfigurasi k = (Konfigurasi) s.createCriteria(Konfigurasi.class)
                .add(Restrictions.eq("nama", nama)).setMaxResults(1).uniqueResult();
        if (k == null) {
            k = new Konfigurasi(nama, nilai);
            s.save(k);
        } else {
            k.setNilai(nilai);
            s.update(k);
        }
    }

    private static Map<String, Integer> hitungAwal(String[] keys) {
        Map<String, Integer> hasil = new LinkedHashMap<String, Integer>();
        for (String key : keys) {
            hasil.put(key, Integer.valueOf(0));
        }
        return hasil;
    }

    private static void tambah(Map<String, Integer> map, String key) {
        if (key == null) {
            return;
        }
        Integer lama = map.get(key);
        map.put(key, Integer.valueOf(lama == null ? 1 : lama.intValue() + 1));
    }

    private static JSONObject peta(Map<String, Integer> map) throws Exception {
        JSONObject j = new JSONObject();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            j.put(entry.getKey(), entry.getValue());
        }
        return j;
    }

    private static JSONArray array(String[] values) {
        JSONArray a = new JSONArray();
        for (String value : values) {
            a.put(value);
        }
        return a;
    }

    private static void wajibCsrf(HttpServletRequest r) {
        if (!"POST".equalsIgnoreCase(r.getMethod()) || !NewUiCsrfUtil.isValid(r)) {
            throw new SecurityException("Token keamanan tidak valid. Muat ulang halaman.");
        }
    }

    private static Long id(HttpServletRequest r) {
        Long id = idOpsional(r.getParameter("id"));
        if (id == null) {
            throw new IllegalArgumentException("Id wajib dikirim.");
        }
        return id;
    }

    private static Long idOpsional(String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Id tidak sah.");
        }
    }

    private static String wajib(HttpServletRequest r, String nama, String pesan) {
        String nilai = text(r.getParameter(nama), "");
        if (nilai.length() == 0) {
            throw new IllegalArgumentException(pesan);
        }
        return nilai;
    }

    private static void wajibPilihan(String nilai, String[] pilihan, String pesan) {
        for (String item : pilihan) {
            if (item.equals(nilai)) {
                return;
            }
        }
        throw new IllegalArgumentException(pesan);
    }

    private static int angka(String raw, int min, int max, String pesan) {
        try {
            int nilai = Integer.parseInt(raw == null ? "" : raw.trim());
            if (nilai < min || nilai > max) {
                throw new NumberFormatException();
            }
            return nilai;
        } catch (Exception e) {
            throw new IllegalArgumentException(pesan);
        }
    }

    private static boolean bool(String raw) {
        return "true".equalsIgnoreCase(text(raw, "false"))
                || "1".equals(text(raw, "false"));
    }

    private static String tanggal(Date value) {
        return value == null ? "" : Common.dateFormat5.get().format(value);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0
                ? fallback : value.trim();
    }

    private static void rollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception ignored) {
        }
    }

    private static void fail(JSONObject j, String code, String message)
            throws Exception {
        j.put("ok", false).put("code", code)
                .put("message", message == null ? "Operasi ditolak." : message);
    }

    private static void write(HttpServletResponse response, JSONObject json)
            throws Exception {
        response.getWriter().write(json.toString());
    }
}
