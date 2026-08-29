package ais.common.newui.sekolah;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.util.DepositHelper;
import ais.common.Common;
import ais.common.TunaiSiswaCommon;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyCheckboxConfig;

/**
 * Kontrak JSON kasir pembayaran siswa/calon siswa — paritas pem_online.zul
 * (PembayaranOnline) tanpa komponen ZK. Action:
 * meta (subjek+tabungan+konfigurasi+csrf), list (tagihan+aturan), options
 * (akun bayar tunai/tabungan), revisions (riwayat), save (bayar tunai via
 * TunaiSiswaCommon.onSave dengan Rows sintetis pola WizardPembayaranSiswaHelper).
 *
 * Deviasi sadar dari ZK (didokumentasikan di handover):
 * - Jembatan calon->siswa hanya dibaca, tidak mem-persist setCalonSiswa.
 * - Override nominal (nilaiBiayaBisaDiubahSaatPembayaran) paritas wizard:
 *   tidak mengubah master NominalBiaya.
 * - Struk tidak dicetak dari controller; klien menerima id+kode transaksi.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiPemOnlineController {
    private static final String MODULE = "sekolah", PAGE = "pem_online";
    private NewUiPemOnlineController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, PAGE, action)) {
                response.setContentType("application/json; charset=UTF-8");
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            boolean mutation = "save".equals(action);
            if (mutation && (!"POST".equalsIgnoreCase(request.getMethod()) || !csrf(request))) {
                response.setContentType("application/json; charset=UTF-8");
                response.setStatus(403); fail(json, "CSRF_INVALID", "Token CSRF tidak valid."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            Subjek subjek = resolveSubjek(request, user);
            response.setContentType("application/json; charset=UTF-8");
            if ("export_kuitansi".equals(action)) kuitansiStruk(json, request, user);
            else if ("lookup".equals(action)) lookup(json, request, user);
            else if ("meta".equals(action)) meta(json, request, subjek, user);
            else if ("list".equals(action)) list(json, request, subjek);
            else if ("options".equals(action)) options(json, subjek);
            else if ("revisions".equals(action)) riwayat(json, request, subjek);
            else if ("save".equals(action)) bayar(json, request, subjek, user);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses kasir pembayaran. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiPemOnlineController"); } catch (Exception ignored) { }
        }
        response.setContentType("application/json; charset=UTF-8");
        write(response, json);
    }

    // -------------------------------------------------------------- kuitansi
    /**
     * Cetak struk/kuitansi PDF sebuah PembayaranSiswa — paritas
     * PembayaranSiswaUtil.cetakStruk namun headless: parameter dibangun sama
     * (kode transaksi, waktu cetak, properti siswa/calon, dataPembayaran) lalu
     * dirender via Report.generateFileReportSimple("sekolah/struk_pembayaran").
     * PDF dikirim sebagai base64 di amplop JSON (JSP delegasi sudah memegang
     * getWriter() sehingga streaming biner via getOutputStream tidak mungkin).
     */
    private static void kuitansiStruk(JSONObject j, HttpServletRequest r, Tbmuser user)
            throws Exception {
        if (user == null) throw new SecurityException("Sesi tidak dikenal.");
        Long pembayaranId = id(r, "pembayaranId", true);
        PembayaranSiswa pembayaran;
        Session s = HibernateUtil.openSession();
        try { pembayaran = (PembayaranSiswa) s.get(PembayaranSiswa.class, pembayaranId); }
        finally { s.close(); }
        if (pembayaran == null) throw new IllegalArgumentException("Pembayaran tidak ditemukan.");
        // Scoping identitas: siswa/calon hanya boleh mencetak pembayarannya sendiri;
        // orang tua terbatas pada anaknya.
        boolean relasiTerbatas = user.getSiswa() != null || user.getCalonSiswa() != null
                || user.getOrangTua() != null || user.getMahasiswa() != null;
        if (relasiTerbatas) {
            boolean milikSendiri =
                    (user.getSiswa() != null && pembayaran.getSiswa() != null
                            && user.getSiswa().getId().equals(pembayaran.getSiswa().getId()))
                    || (user.getCalonSiswa() != null && pembayaran.getCalonSiswa() != null
                            && user.getCalonSiswa().getId().equals(pembayaran.getCalonSiswa().getId()));
            if (!milikSendiri && user.getOrangTua() != null && pembayaran.getSiswa() != null) {
                java.util.List anak = user.getOrangTua().ambilAnakSiswa();
                milikSendiri = anak != null && anak.contains(pembayaran.getSiswa().getId());
            }
            if (!milikSendiri) throw new SecurityException("Kuitansi di luar cakupan pengguna.");
        }

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id_pembayaran", pembayaran.getId());
        parameters.put("id_sekolah", pembayaran.getSekolah() != null ? pembayaran.getSekolah().getId() : -1L);
        parameters.put("id_bri", -1L);
        parameters.put("id_bni", -1L);
        parameters.put("id_bsi", -1L);
        parameters.put("id_va", -1L);
        parameters.put("kode_transaksi", kodeTransaksi(pembayaran.getId()));
        parameters.put("waktu_cetak", Common.dateFormat1.get().format(
                pembayaran.getTanggal() != null ? pembayaran.getTanggal() : new Date()));
        if (pembayaran.getCalonSiswa() != null)
            Common.insertProperty(CalonSiswa.class, pembayaran.getCalonSiswa(), parameters, "");
        else if (pembayaran.getSiswa() != null)
            Common.insertProperty(Siswa.class, pembayaran.getSiswa(), parameters, "");
        ais.action.master.sekolah.util.PembayaranSiswaUtil.dataPembayaran(pembayaran, null, null, null, null,
                (Map) parameters);

        java.io.File pdf = ais.action.report.Report.generateFileReportSimple(
                ais.action.report.Report.PDF, (Map) parameters, "sekolah/struk_pembayaran");
        if (pdf == null || !pdf.exists())
            throw new IllegalStateException("PDF struk gagal dibuat.");
        byte[] isi = java.nio.file.Files.readAllBytes(pdf.toPath());
        j.put("namaFile", "struk_" + kodeTransaksi(pembayaran.getId()) + ".pdf");
        j.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }

    /** Kode transaksi 8 digit (paritas PembayaranSiswaUtil.formatKodeTransaksi yang privat). */
    private static String kodeTransaksi(Long id) {
        if (id == null) return "00000000";
        String kode = String.valueOf(id);
        return kode.length() >= 8 ? kode.substring(kode.length() - 8) : String.format("%08d", id);
    }

    /** Subjek kasir: siswa XOR calon siswa, dengan scoping identitas server-side. */
    private static final class Subjek {
        Siswa siswa; CalonSiswa calon;
        boolean staf; // boleh membayar tunai (gate ZK :2513)
    }

    private static Subjek resolveSubjek(HttpServletRequest r, Tbmuser user) {
        Subjek subjek = new Subjek();
        if (user == null) throw new SecurityException("Sesi tidak dikenal.");
        boolean relasiTerbatas = user.getSiswa() != null || user.getCalonSiswa() != null
                || user.getOrangTua() != null || user.getMahasiswa() != null;
        subjek.staf = !relasiTerbatas;
        Long siswaId = id(r, "siswaId", false);
        Long calonId = id(r, "calonSiswaId", false);
        if (user.getSiswa() != null) { subjek.siswa = user.getSiswa(); return subjek; }
        if (user.getCalonSiswa() != null) { subjek.calon = user.getCalonSiswa(); return subjek; }
        Session s = HibernateUtil.openSession();
        try {
            if (siswaId != null) subjek.siswa = (Siswa) s.get(Siswa.class, siswaId);
            else if (calonId != null) subjek.calon = (CalonSiswa) s.get(CalonSiswa.class, calonId);
        } finally { s.close(); }
        if (user.getOrangTua() != null) {
            List anak = user.getOrangTua().ambilAnakSiswa();
            if (subjek.siswa == null || anak == null || !anak.contains(subjek.siswa.getId()))
                throw new SecurityException("Siswa di luar cakupan orang tua.");
        }
        return subjek;
    }

    private static void requireSubjek(Subjek subjek) {
        if (subjek.siswa == null && subjek.calon == null)
            throw new IllegalArgumentException("Siswa atau calon siswa wajib dipilih.");
    }

    // ---------------------------------------------------------------- lookup
    /**
     * Pencarian subjek kasir. Pengguna ber-relasi terbatas hanya melihat dirinya
     * (flag "sendiri" = true agar klien mengunci pilihan). Paritas banbox ZK:
     * q kosong TETAP mengembalikan halaman pertama daftar (onOpen menampilkan
     * daftar tanpa mengetik).
     */
    private static void lookup(JSONObject j, HttpServletRequest r, Tbmuser user) throws Exception {
        String q = text(r.getParameter("q"), "");
        JSONArray siswaArr = new JSONArray(), calonArr = new JSONArray();
        boolean sendiriSaja = user.getSiswa() != null || user.getCalonSiswa() != null;
        if (user.getSiswa() != null) {
            Siswa sendiri = user.getSiswa();
            siswaArr.put(new JSONObject().put("id", sendiri.getId())
                    .put("nama", nz(sendiri.getNamaSiswa())).put("kode", nz(sendiri.getNomorInduk())));
        } else if (user.getCalonSiswa() != null) {
            CalonSiswa sendiri = user.getCalonSiswa();
            calonArr.put(new JSONObject().put("id", sendiri.getId())
                    .put("nama", nz(sendiri.getNamaSiswa())).put("kode", nz(sendiri.getNoRegistrasi())));
        } else {
            boolean adaFilter = q.length() >= 2;
            Session s = HibernateUtil.openSession();
            try {
                List anak = user.getOrangTua() == null ? null : user.getOrangTua().ambilAnakSiswa();
                Criteria cs = s.createCriteria(Siswa.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .addOrder(Order.asc("namaSiswa")).setMaxResults(20);
                if (adaFilter) cs.add(Restrictions.or(Restrictions.ilike("namaSiswa", "%" + q + "%"),
                        Restrictions.ilike("nomorInduk", "%" + q + "%")));
                if (anak != null) cs.add(anak.isEmpty()
                        ? Restrictions.sqlRestriction("1=0") : Restrictions.in("id", anak));
                for (Object o : cs.list()) {
                    Siswa siswa = (Siswa) o;
                    siswaArr.put(new JSONObject().put("id", siswa.getId())
                            .put("nama", nz(siswa.getNamaSiswa())).put("kode", nz(siswa.getNomorInduk())));
                }
                if (anak == null) {
                    Criteria cc = s.createCriteria(CalonSiswa.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .addOrder(Order.asc("namaSiswa")).setMaxResults(20);
                    if (adaFilter) cc.add(Restrictions.or(Restrictions.ilike("namaSiswa", "%" + q + "%"),
                            Restrictions.ilike("noRegistrasi", "%" + q + "%")));
                    for (Object o : cc.list()) {
                        CalonSiswa calon = (CalonSiswa) o;
                        calonArr.put(new JSONObject().put("id", calon.getId())
                                .put("nama", nz(calon.getNamaSiswa())).put("kode", nz(calon.getNoRegistrasi())));
                    }
                }
            } finally { s.close(); }
        }
        j.put("siswa", siswaArr).put("calon", calonArr).put("sendiri", sendiriSaja);
    }

    // ------------------------------------------------------------------ meta
    private static void meta(JSONObject j, HttpServletRequest r, Subjek subjek, Tbmuser user) throws Exception {
        j.put("staf", subjek.staf);
        j.put("csrf", csrfToken(r));
        if (subjek.siswa != null) {
            j.put("subjekTipe", "siswa").put("subjekId", subjek.siswa.getId())
             .put("subjekNama", nz(subjek.siswa.getNamaSiswa())).put("subjekKode", nz(subjek.siswa.getNomorInduk()));
        } else if (subjek.calon != null) {
            j.put("subjekTipe", "calon").put("subjekId", subjek.calon.getId())
             .put("subjekNama", nz(subjek.calon.getNamaSiswa())).put("subjekKode", nz(subjek.calon.getNoRegistrasi()));
        }
        if (subjek.siswa != null || subjek.calon != null) {
            j.put("tabungan", DepositHelper.hitungDeposit(subjek.siswa, subjek.calon));
            // Paritas 5b: penjurusan wajib menghentikan pemuatan tagihan.
            String penjurusan = cekPenjurusan(subjek);
            if (penjurusan != null) j.put("penjurusanWajib", penjurusan);
        }
        j.put("tampilkanTabungan", subjek.siswa != null && boleh("tampilkan_tabungan_siswa", true));
        j.put("tolakTotalNol", boleh("payment_gateway_tolak_total_nol_atau_minus", true));
    }

    private static String cekPenjurusan(Subjek subjek) {
        try {
            if (subjek.calon != null && subjek.calon.getPenjurusanSekolah() == null
                    && subjek.calon.getSekolah() != null
                    && Boolean.TRUE.equals(subjek.calon.getSekolah().getPenjurusanWajibDipilih()))
                return "Penjurusan calon siswa wajib dipilih sebelum pembayaran.";
            if (subjek.siswa != null && subjek.siswa.getPenjurusanSekolah() == null
                    && subjek.siswa.getSekolah() != null
                    && Boolean.TRUE.equals(subjek.siswa.getSekolah().getPenjurusanWajibDipilih()))
                return "Penjurusan siswa wajib dipilih sebelum pembayaran.";
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "NewUiPemOnlineController.cekPenjurusan"); }
        return null;
    }

    // ------------------------------------------------------------------ list
    private static void list(JSONObject j, HttpServletRequest r, Subjek subjek) throws Exception {
        requireSubjek(subjek);
        String tolakPenjurusan = cekPenjurusan(subjek);
        if (tolakPenjurusan != null) throw new IllegalArgumentException(tolakPenjurusan);
        Integer bln = integerObject(r, "sdBulan"), thn = integerObject(r, "sdTahun");
        if (bln == null) bln = Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.MONTH) + 1);
        if (thn == null) thn = Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.YEAR));
        // Paritas checkbox staf "Tampilkan pilihan bukan tagihan" (pem_online).
        boolean tampilkanBukanTagihan = subjek.staf
                && "true".equalsIgnoreCase(r.getParameter("tampilkanBukanTagihan"));
        List<Tagihan> rows = muatTagihan(subjek, bln, thn, tampilkanBukanTagihan);
        JSONArray arr = new JSONArray();
        for (Tagihan t : rows) arr.put(encodeTagihan(t));
        j.put("rows", arr).put("total", rows.size())
         .put("sdBulan", bln.intValue()).put("sdTahun", thn.intValue())
         .put("tabungan", DepositHelper.hitungDeposit(subjek.siswa, subjek.calon))
         .put("csrf", csrfToken(r));
    }

    /** Pipeline paritas prosesTampilPembayaranParalel (sekuensial, tanpa side-effect tulis). */
    private static List<Tagihan> muatTagihan(Subjek subjek, Integer bln, Integer thn,
            boolean tampilkanBukanTagihan) {
        List<Tagihan> hasil = new ArrayList<Tagihan>();
        Session s = HibernateUtil.openSession();
        try {
            CalonSiswa jembatan = null;
            if (subjek.calon == null && subjek.siswa != null) {
                Long calonTertaut = subjek.siswa.getCalonSiswa();
                if (calonTertaut != null) jembatan = (CalonSiswa) s.get(CalonSiswa.class, calonTertaut);
                if (jembatan == null) {
                    Long calSis = (Long) s.createCriteria(CalonSiswa.class)
                            .add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
                            .setProjection(Projections.property("id"))
                            .add(Restrictions.ilike("namaSiswa", nz(subjek.siswa.getNamaSiswa())))
                            .add(Restrictions.eq("tanggalLahir", subjek.siswa.getTanggalLahir()))
                            .setMaxResults(1)
                            .addOrder(Order.desc("id")).addOrder(Order.desc("tahunMasuk")).uniqueResult();
                    if (calSis != null) jembatan = (CalonSiswa) s.get(CalonSiswa.class, calSis);
                }
            }
            List<Object[]> target = new ArrayList<Object[]>();
            if (jembatan != null) {
                List pbCalon = PengaturanBiaya.terapkanFilterPembayaran(
                        s.createCriteria(PengaturanBiaya.class), null, jembatan)
                        .addOrder(Order.desc("id")).list();
                for (Object o : pbCalon) target.add(new Object[] { o, null, jembatan });
            }
            List pbUtama = PengaturanBiaya.terapkanFilterPembayaran(
                    s.createCriteria(PengaturanBiaya.class), subjek.siswa, subjek.calon)
                    .addOrder(Order.desc("id")).list();
            for (Object o : pbUtama) target.add(new Object[] { o, subjek.siswa, subjek.calon });

            for (Object[] pasang : target) {
                PengaturanBiaya pb = (PengaturanBiaya) pasang[0];
                Siswa siswa = (Siswa) pasang[1];
                CalonSiswa calon = (CalonSiswa) pasang[2];
                if (!Boolean.TRUE.equals(pb.getAktif())) continue;
                JenisBiayaSekolah jbs = pb.getJenisBiayaSekolah();
                if (jbs == null) continue;
                boolean pakaiCalon = Boolean.TRUE.equals(jbs.getGunakanCalonSiswa());
                boolean valid;
                if (!pakaiCalon && siswa != null) valid = DetailTagihanSiswaHelper.apakahAda(pb, siswa);
                else if (pakaiCalon && calon != null) valid = DetailTagihanCalonSiswaHelper.apakahAda(pb, calon);
                else continue;
                if (!valid) continue;
                List<Tagihan> tagihans = pakaiCalon
                        ? TagihanUtilCalonSiswa.getTagihan(jbs, pb, calon, bln, thn, false)
                        : TagihanUtil.getTagihan(jbs, pb, siswa, bln, thn, false);
                boolean bulanan = "Bulanan".equalsIgnoreCase(nz(jbs.getPeriode()));
                for (Tagihan t : tagihans) {
                    if (!layakTampil(t, tampilkanBukanTagihan)) continue;
                    if (bulanan) {
                        if (t.getTahunbulan() == null) continue;
                        if (pb.getBulanMulai() != null && t.getTahunbulan().intValue() < pb.getBulanMulai().intValue()) continue;
                        if (pb.getBulanSampai() != null && t.getTahunbulan().intValue() > pb.getBulanSampai().intValue()) break;
                    }
                    hasil.add(t);
                }
            }
        } finally { s.close(); }
        return hasil;
    }

    /** Predikat kelayakan-tampil baris (PembayaranOnline :2915/:3002). */
    private static boolean layakTampil(Tagihan t, boolean tampilkanBukanTagihan) {
        try {
            if (!Boolean.TRUE.equals(t.getAktif())) return false;
            if (!tampilkanBukanTagihan && t.ambilBukanTagihanData()) return false;
            if (t.getNominalBiaya() == null || Boolean.TRUE.equals(t.getNominalBiaya().getBukanTagihan())) return false;
            if (t.getPembayaranSiswaDetail() != null) return false;
            ItemBiayaSekolah item = t.getItemBiayaSekolah();
            boolean bisaDiubah = item != null && Boolean.TRUE.equals(item.getNilaiBiayaBisaDiubahSaatPembayaran());
            return bisaDiubah || (t.getNominal() != null && t.getNominal().doubleValue() > 0.1);
        } catch (Exception e) { return false; }
    }

    private static JSONObject encodeTagihan(Tagihan t) throws Exception {
        ItemBiayaSekolah item = t.getItemBiayaSekolah();
        PengaturanBiaya pb = t.getPengaturanBiaya();
        JenisBiayaSekolah jbs = pb == null ? null : pb.getJenisBiayaSekolah();
        int tahunbulanSekarang = PembayaranSiswa.convert(
                Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.YEAR)),
                Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.MONTH) + 1)).intValue();
        boolean wajibBulanIni = item != null && Boolean.TRUE.equals(item.getWajibPilihJikaBulanDipilih())
                && t.getTahunbulan() != null && t.getTahunbulan().intValue() <= tahunbulanSekarang;
        JSONObject o = new JSONObject()
                .put("id", t.getId())
                .put("item", item == null ? "" : nz(item.getNama()))
                .put("itemId", item == null ? null : item.getId())
                .put("jenis", jbs == null ? "" : nz(jbs.getNama()))
                .put("jenisKode", jbs == null ? "" : nz(jbs.getKode()))
                .put("periode", jbs == null ? "" : nz(jbs.getPeriode()))
                .put("bulan", t.getBulan()).put("tahun", t.getTahun())
                .put("tahunbulan", t.getTahunbulan()).put("bayarKe", t.getBayarKe())
                .put("tahunAjaran", nz(t.getTahunAjaran()))
                .put("nominal", nvl(t.getNominal())).put("diskon", nvl(t.getDiskon())).put("denda", nvl(t.getDenda()))
                .put("bisaDiubah", item != null && Boolean.TRUE.equals(item.getNilaiBiayaBisaDiubahSaatPembayaran()))
                .put("wajibPilih", item != null && Boolean.TRUE.equals(item.getWajibPilih()))
                .put("wajibBulanIni", wajibBulanIni)
                .put("bolehDiangsur", item != null && Boolean.TRUE.equals(item.getBolehDiangsur()))
                .put("terakumulasiBulanan", jbs != null && Boolean.TRUE.equals(jbs.getPilihanItemBiayaTerakumulasiBulanan()))
                .put("dibayarSebayak", t.getNominalBiaya() == null ? null : t.getNominalBiaya().getDibayarSebayak())
                .put("terkunciPengaturan", (pb != null && pb.getKunci() != null) || t.getKunci() != null);
        if (item != null && item.getHarusBayar() != null) o.put("harusBayarItemId", item.getHarusBayar().getId());
        if (pb != null && pb.getWajibDibayarSebelumnya() != null && pb.getWajibDibayarSebelumnya().trim().length() > 0)
            o.put("wajibDibayarSebelumnya", nz(pb.getWajibDibayarSebelumnya()));
        return o;
    }

    // --------------------------------------------------------------- options
    private static void options(JSONObject j, Subjek subjek) throws Exception {
        requireSubjek(subjek);
        ais.database.model.sekolah.Sekolah sekolah = subjek.siswa != null ? subjek.siswa.getSekolah()
                : subjek.calon.getSekolah();
        if (sekolah == null || sekolah.getId() == null)
            throw new IllegalArgumentException("Sekolah subjek belum tersedia.");
        double tabungan = DepositHelper.hitungDeposit(subjek.siswa, subjek.calon);
        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            List akuns = s.createCriteria(AkunPembayaranSiswa.class)
                    .add(Restrictions.or(Restrictions.eq("dariTabungan", true), Restrictions.eq("manual", true)))
                    .add(Restrictions.eq("sekolah", sekolah))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).list();
            for (Object o : akuns) {
                AkunPembayaranSiswa akun = (AkunPembayaranSiswa) o;
                boolean dariTabungan = Boolean.TRUE.equals(akun.getDariTabungan());
                if (dariTabungan && tabungan <= 0.1) continue; // ZK :2535
                double biayaAdmin = 0.0;
                try { biayaAdmin = Double.parseDouble(
                        Common.getKonfigurasi(akun.getId() + "_biaya_administrasi", "0.0").getNilai()); }
                catch (Exception ignored) { }
                arr.put(new JSONObject().put("id", akun.getId()).put("nama", nz(akun.getNama()))
                        .put("dariTabungan", dariTabungan).put("biayaAdministrasi", biayaAdmin));
            }
        } finally { s.close(); }
        j.put("akun", arr).put("tabungan", tabungan);
    }

    // ------------------------------------------------------------- revisions
    private static void riwayat(JSONObject j, HttpServletRequest r, Subjek subjek) throws Exception {
        requireSubjek(subjek);
        int page = integer(r, "page", 0), size = Math.min(50, Math.max(5, integer(r, "size", 10)));
        Session s = HibernateUtil.openSession();
        try {
            org.hibernate.criterion.Criterion pemilik = subjek.siswa != null
                    ? Restrictions.eq("pembayaranSiswa.siswa", subjek.siswa)
                    : Restrictions.eq("pembayaranSiswa.calonSiswa", subjek.calon);
            Number total = (Number) s.createCriteria(PembayaranSiswaDetail.class)
                    .createAlias("pembayaranSiswa", "pembayaranSiswa").add(pemilik)
                    .setProjection(Projections.rowCount()).uniqueResult();
            Criteria crit = s.createCriteria(PembayaranSiswaDetail.class)
                    .createAlias("pembayaranSiswa", "pembayaranSiswa")
                    .createAlias("itemBiayaSekolah", "itemBiayaSekolah", Criteria.LEFT_JOIN)
                    .createAlias("tagihan", "tagihan", Criteria.LEFT_JOIN)
                    .add(pemilik)
                    .addOrder(Order.desc("id"))
                    .setFirstResult(Math.max(0, page) * size).setMaxResults(size);
            JSONArray arr = new JSONArray();
            for (Object o : crit.list()) {
                PembayaranSiswaDetail d = (PembayaranSiswaDetail) o;
                PembayaranSiswa p = d.getPembayaranSiswa();
                JSONObject baris = new JSONObject()
                        .put("id", d.getId())
                        .put("nominal", nvl(d.getNominal()))
                        .put("item", d.getItemBiayaSekolah() == null ? "" : nz(d.getItemBiayaSekolah().getNama()));
                try {
                    Tagihan tag = d.getTagihan();
                    if (tag != null) {
                        baris.put("tagihanId", tag.getId()).put("bayarKe", tag.getBayarKe());
                        PengaturanBiaya pb = tag.getPengaturanBiaya();
                        JenisBiayaSekolah jbs = pb == null ? null : pb.getJenisBiayaSekolah();
                        if (jbs != null) {
                            baris.put("jenis", nz(jbs.getNama())).put("jenisKode", nz(jbs.getKode()));
                        }
                        if (tag.getTahunAjaran() != null) baris.put("tahunAjaran", nz(tag.getTahunAjaran()));
                    }
                } catch (Exception ignored) { }
                if (p != null) {
                    baris.put("pembayaranId", p.getId()).put("bulan", p.getBulan()).put("tahun", p.getTahun())
                         .put("via", p.getAkunPembayaranSiswa() == null ? "" : nz(p.getAkunPembayaranSiswa().getNama()));
                    if (p.getTanggalBayar() != null) baris.put("waktu", p.getTanggalBayar().getTime());
                }
                arr.put(baris);
            }
            j.put("rows", arr).put("total", total == null ? 0 : total.intValue()).put("page", page).put("size", size);
        } finally { s.close(); }
    }

    // ------------------------------------------------------------------ save
    private static void bayar(JSONObject j, HttpServletRequest r, Subjek subjek, Tbmuser user) throws Exception {
        requireSubjek(subjek);
        if (!subjek.staf) throw new SecurityException("Pembayaran tunai hanya untuk petugas kasir.");
        String tolakPenjurusan = cekPenjurusan(subjek);
        if (tolakPenjurusan != null) throw new IllegalArgumentException(tolakPenjurusan);

        Long akunId = id(r, "akunId", true);
        Set<Long> dipilih = idSet(r.getParameter("tagihanIds"));
        Double depositTopUp = doubleObject(r, "depositTopUp");
        boolean pakaiTabungan = "true".equalsIgnoreCase(r.getParameter("pakaiTabungan"));
        Integer bln = integerObject(r, "sdBulan"), thn = integerObject(r, "sdTahun");
        if (bln == null) bln = Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.MONTH) + 1);
        if (thn == null) thn = Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.YEAR));
        Map<Long, Double> override = overrideMap(r.getParameter("nominalOverride"));

        boolean adaDeposit = depositTopUp != null && depositTopUp.doubleValue() > 0.1;
        if (dipilih.isEmpty() && !adaDeposit)
            throw new IllegalArgumentException("Belum ada tagihan yang dipilih.");
        // Paritas checkbox staf "Boleh pilih kustom per item biaya" (R0):
        // mematikan aturan dependensi R1-R4; hanya berlaku untuk petugas.
        boolean pilihCustom = subjek.staf && "true".equalsIgnoreCase(r.getParameter("pilihCustom"));
        boolean tampilkanBukanTagihan = subjek.staf
                && "true".equalsIgnoreCase(r.getParameter("tampilkanBukanTagihan"));

        // Muat ulang dari server — jangan percaya daftar klien.
        List<Tagihan> layak = muatTagihan(subjek, bln, thn, tampilkanBukanTagihan);
        Map<Long, Tagihan> perId = new HashMap<Long, Tagihan>();
        for (Tagihan t : layak) perId.put(t.getId(), t);
        List<Tagihan> terpilih = new ArrayList<Tagihan>();
        for (Long tid : dipilih) {
            Tagihan t = perId.get(tid);
            if (t == null) throw new IllegalArgumentException(
                    "Tagihan " + tid + " tidak tersedia untuk dibayar (sudah lunas/di luar cakupan).");
            terpilih.add(t);
        }
        validasiKeterpilihan(layak, dipilih, subjek.staf, pilihCustom);

        double total = 0.0;
        for (Tagihan t : terpilih) {
            Double dasar = t.getNominal();
            ItemBiayaSekolah item = t.getItemBiayaSekolah();
            if (item != null && Boolean.TRUE.equals(item.getNilaiBiayaBisaDiubahSaatPembayaran())
                    && override.containsKey(t.getId())) dasar = override.get(t.getId());
            total += (nvl(dasar) + nvl(t.getDenda())) - nvl(t.getDiskon());
        }
        if (adaDeposit) total += depositTopUp.doubleValue();
        if (boleh("payment_gateway_tolak_total_nol_atau_minus", true) && total <= 0.0 && !adaDeposit)
            throw new IllegalArgumentException("Total pembayaran nol atau minus tidak dapat diproses.");

        AkunPembayaranSiswa akun;
        Session s = HibernateUtil.openSession();
        try { akun = (AkunPembayaranSiswa) s.get(AkunPembayaranSiswa.class, akunId); } finally { s.close(); }
        if (akun == null) throw new IllegalArgumentException("Cara pembayaran tidak ditemukan.");
        double tabungan = DepositHelper.hitungDeposit(subjek.siswa, subjek.calon);
        Double tabunganDipakai = null;
        if (Boolean.TRUE.equals(akun.getDariTabungan()) || pakaiTabungan) {
            double biayaAdmin = 0.0;
            try { biayaAdmin = Double.parseDouble(
                    Common.getKonfigurasi(akun.getId() + "_biaya_administrasi", "0.0").getNilai()); }
            catch (Exception ignored) { }
            if (Boolean.TRUE.equals(akun.getDariTabungan()) && (total + biayaAdmin) > tabungan)
                throw new IllegalArgumentException("Saldo tabungan tidak mencukupi.");
            tabunganDipakai = Double.valueOf(tabungan);
        }

        // Rows sintetis — pola WizardPembayaranSiswaHelper.buatRowsMock.
        Rows rows = new Rows();
        for (Tagihan t : terpilih) {
            Row row = new Row();
            MyCheckboxConfig pilih = new MyCheckboxConfig();
            pilih.setChecked(true);
            row.setAttribute("pilih", pilih);
            row.setAttribute("tagihan", t);
            ItemBiayaSekolah item = t.getItemBiayaSekolah();
            if (item != null && Boolean.TRUE.equals(item.getNilaiBiayaBisaDiubahSaatPembayaran())) {
                Doublebox nominal = new Doublebox();
                nominal.setValue(override.containsKey(t.getId()) ? override.get(t.getId()) : t.getNominal());
                row.setAttribute("nominal", nominal);
            }
            rows.appendChild(row);
        }
        String validator = user.getUserNama() == null ? user.getUserId() : user.getUserNama();
        Date tanggal = tanggal(r.getParameter("tanggal"));
        PembayaranSiswa hasil = TunaiSiswaCommon.onSave(subjek.siswa, subjek.calon, terpilih,
                adaDeposit ? depositTopUp : Double.valueOf(0.0), tabunganDipakai, validator, akun, rows, tanggal);
        if (hasil == null || hasil.getId() == null)
            throw new IllegalArgumentException("Pembayaran ditolak: total nol atau data tidak valid.");
        // Kode transaksi = 8 digit terakhir id, pola PembayaranSiswaUtil.formatKodeTransaksi.
        String kode = String.valueOf(hasil.getId());
        kode = ("00000000" + kode).substring(("00000000" + kode).length() - 8);
        j.put("pembayaranId", hasil.getId()).put("nominal", nvl(hasil.getNominal())).put("kode", kode);
    }

    /**
     * Penegakan ulang aturan R1/R2/R3/R4/R5/R6 pem_online atas himpunan pilihan.
     * Fail-closed: pelanggaran melempar VALIDATION_FAILED dengan pesan spesifik.
     */
    private static void validasiKeterpilihan(List<Tagihan> layak, Set<Long> dipilih,
            boolean staf, boolean pilihCustom) {
        int tahunbulanSekarang = PembayaranSiswa.convert(
                Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.YEAR)),
                Integer.valueOf(new java.util.GregorianCalendar().get(java.util.Calendar.MONTH) + 1)).intValue();
        // R5/R6: baris wajib mengikat non-staf; petugas (merupakanAdmin ZK)
        // boleh melepasnya. R1-R4 dilewati saat petugas memakai pilihCustom.
        if (!staf) {
            for (Tagihan t : layak) {
                ItemBiayaSekolah item = t.getItemBiayaSekolah();
                boolean wajib = item != null && (Boolean.TRUE.equals(item.getWajibPilih())
                        || (Boolean.TRUE.equals(item.getWajibPilihJikaBulanDipilih())
                            && t.getTahunbulan() != null && t.getTahunbulan().intValue() <= tahunbulanSekarang));
                if (wajib && !dipilih.contains(t.getId()))
                    throw new IllegalArgumentException("Tagihan wajib '" + nz(item.getNama()) + "' harus ikut dibayar.");
            }
        }
        if (staf && pilihCustom) return;
        Set<Long> itemDipilih = new HashSet<Long>();
        for (Tagihan t : layak) if (dipilih.contains(t.getId()) && t.getItemBiayaSekolah() != null)
            itemDipilih.add(t.getItemBiayaSekolah().getId());
        for (Tagihan t : layak) {
            if (!dipilih.contains(t.getId())) continue;
            PengaturanBiaya pb = t.getPengaturanBiaya();
            // R1: prasyarat eksplisit CSV ",id1,id2,".
            if (pb != null && pb.getWajibDibayarSebelumnya() != null
                    && pb.getWajibDibayarSebelumnya().trim().length() > 0) {
                String csv = pb.getWajibDibayarSebelumnya();
                for (Tagihan lain : layak) {
                    if (csv.contains("," + lain.getId() + ",") && !dipilih.contains(lain.getId()))
                        throw new IllegalArgumentException(
                                "Tagihan prasyarat '" + namaTagihan(lain) + "' harus ikut dibayar.");
                }
            }
            // R4: rantai harusBayar per item (satu tingkat + transitif dalam daftar layak).
            ItemBiayaSekolah item = t.getItemBiayaSekolah();
            ItemBiayaSekolah prasyarat = item == null ? null : item.getHarusBayar();
            int kedalaman = 0;
            while (prasyarat != null && kedalaman++ < 12) {
                boolean adaTagihanPrasyarat = false;
                for (Tagihan lain : layak)
                    if (lain.getItemBiayaSekolah() != null
                            && prasyarat.getId().equals(lain.getItemBiayaSekolah().getId())) { adaTagihanPrasyarat = true; break; }
                if (adaTagihanPrasyarat && !itemDipilih.contains(prasyarat.getId()))
                    throw new IllegalArgumentException(
                            "Item '" + nz(prasyarat.getNama()) + "' wajib dibayar lebih dulu.");
                prasyarat = prasyarat.getHarusBayar();
            }
            // R2/R3: urutan bulanan/angsuran per item — tidak boleh melompati yang lebih awal.
            if (t.getTahunbulan() != null && item != null) {
                for (Tagihan lain : layak) {
                    if (lain.getItemBiayaSekolah() == null || lain.getTahunbulan() == null) continue;
                    if (!item.getId().equals(lain.getItemBiayaSekolah().getId())) continue;
                    long kunciLain = urutan(lain), kunciIni = urutan(t);
                    if (kunciLain < kunciIni && !dipilih.contains(lain.getId()))
                        throw new IllegalArgumentException(
                                "Tagihan '" + namaTagihan(lain) + "' lebih awal dan harus ikut dibayar.");
                }
            }
        }
    }

    private static long urutan(Tagihan t) {
        long tahunbulan = t.getTahunbulan() == null ? 0L : t.getTahunbulan().longValue();
        long bayarKe = t.getBayarKe() == null ? 0L : t.getBayarKe().longValue();
        return tahunbulan * 10000L + bayarKe;
    }

    private static String namaTagihan(Tagihan t) {
        String item = t.getItemBiayaSekolah() == null ? "?" : nz(t.getItemBiayaSekolah().getNama());
        return item + (t.getBulan() == null ? "" : " " + t.getBulan() + "/" + t.getTahun());
    }

    // ------------------------------------------------------------- utilities
    private static Map<Long, Double> overrideMap(String raw) {
        Map<Long, Double> map = new HashMap<Long, Double>();
        if (raw == null || raw.trim().length() == 0) return map;
        try {
            JSONObject o = new JSONObject(raw);
            Iterator keys = o.keys();
            while (keys.hasNext()) {
                String k = String.valueOf(keys.next());
                map.put(Long.valueOf(k), Double.valueOf(o.getDouble(k)));
            }
        } catch (Exception e) { throw new IllegalArgumentException("nominalOverride tidak valid."); }
        return map;
    }

    private static Set<Long> idSet(String csv) {
        Set<Long> ids = new HashSet<Long>();
        if (csv == null) return ids;
        for (String bagian : csv.split(",")) {
            String v = bagian.trim();
            if (v.length() == 0) continue;
            try { ids.add(Long.valueOf(v)); }
            catch (Exception e) { throw new IllegalArgumentException("tagihanIds tidak valid."); }
        }
        return ids;
    }

    private static Date tanggal(String raw) {
        if (raw == null || raw.trim().length() == 0) return new Date();
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(raw.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("tanggal tidak valid (yyyy-MM-dd)."); }
    }

    private static boolean boleh(String kunci, boolean fallback) {
        try { return Common.bolehKonfigurasi(kunci); } catch (Exception e) { return fallback; }
    }

    private static boolean csrf(HttpServletRequest r) {
        Object e = r.getSession().getAttribute("newUiCsrfToken");
        String v = r.getHeader("X-CSRF-Token");
        return e != null && v != null && String.valueOf(e).equals(v);
    }

    private static String csrfToken(HttpServletRequest r) {
        Object existing = r.getSession().getAttribute("newUiCsrfToken");
        if (existing != null) return String.valueOf(existing);
        byte[] b = new byte[24];
        new java.security.SecureRandom().nextBytes(b);
        StringBuilder s = new StringBuilder(48);
        for (int i = 0; i < b.length; i++) {
            s.append(Character.forDigit((b[i] >> 4) & 0xF, 16)).append(Character.forDigit(b[i] & 0xF, 16));
        }
        String value = s.toString();
        r.getSession().setAttribute("newUiCsrfToken", value);
        return value;
    }

    private static Long id(HttpServletRequest r, String n, boolean required) {
        String v = r.getParameter(n);
        if (v == null || v.trim().length() == 0) {
            if (required) throw new IllegalArgumentException(n + " wajib diisi.");
            return null;
        }
        try { return Long.valueOf(v.trim()); }
        catch (Exception e) { throw new IllegalArgumentException(n + " tidak valid."); }
    }

    private static Integer integerObject(HttpServletRequest r, String n) {
        String v = r.getParameter(n);
        if (v == null || v.trim().length() == 0) return null;
        try { return Integer.valueOf(v.trim()); }
        catch (Exception e) { throw new IllegalArgumentException(n + " tidak valid."); }
    }

    private static Double doubleObject(HttpServletRequest r, String n) {
        String v = r.getParameter(n);
        if (v == null || v.trim().length() == 0) return null;
        try { return Double.valueOf(v.trim()); }
        catch (Exception e) { throw new IllegalArgumentException(n + " tidak valid."); }
    }

    private static int integer(HttpServletRequest r, String n, int fallback) {
        try { return Integer.parseInt(text(r.getParameter(n), String.valueOf(fallback))); }
        catch (Exception e) { return fallback; }
    }

    private static double nvl(Double v) { return v == null ? 0.0 : v.doubleValue(); }
    private static String nz(String v) { return v == null ? "" : v; }
    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }
}
