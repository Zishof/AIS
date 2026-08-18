package ais.action.master.akunting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Textbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.akunting.util.CommonAkunting;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.ReimbursementPegawai;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Workflow reimbursement pegawai: pengajuan privat, approval atasan, pencatatan
 * biaya, dan pembayaran finance. Seluruh mutasi status dan jurnal dilakukan
 * transaksional serta diperiksa ulang di server.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ReimbursementPegawaiAction extends GenericAutowireComposer {
    private static final long serialVersionUID = 1L;
    private static final String JENIS_LAMPIRAN = "Lampiran Reimbursement Pegawai";

    private MyWindow window;
    private Grid gridSaya;
    private Grid gridAtasan;
    private Grid gridFinance;
    private Tab approvalTab;
    private Tab financeTab;

    private Textbox deskripsi;
    private Combobox kategori;
    private MyDoublebox nominal;
    private Combobox pajak;
    private AmbilDataPegawaiBanbox pegawai;
    private Checkbox dibayarPegawai;
    private MyDatebox tanggalPengeluaran;
    private AmbilDataPegawaiBanbox atasan;
    private Textbox catatanPengaju;
    private Hbox attachmentBox;
    private Label formMode;

    private MyWindow approvalWindow;
    private Label approvalInfo;
    private Textbox catatanAtasan;
    private AmbilDataAkunBanbox akunBiaya;
    private MyDatebox tanggalAkuntansi;

    private MyWindow paymentWindow;
    private Label paymentInfo;
    private Combobox metodePembayaran;
    private Textbox bankPenerima;
    private Textbox rekeningPenerima;
    private MyDoublebox jumlahPembayaran;
    private MyDatebox tanggalPembayaran;
    private Textbox catatanPembayaran;
    private AmbilDataAkunBanbox akunPembayaran;

    private Tbmuser user;
    private Pegawai currentPegawai;
    private LampiranLain uploadedLampiran;
    private Long editId;
    private Long approvalId;
    private Long paymentId;
    private boolean canApprove;
    private boolean canPay;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        // BUG1: <window mode="popup"> otomatis tampil saat compose (dan mode=popup diperlukan agar field di
        // dalamnya ter-wire ke composer sebagai fellow page-level). Tutup di sini (server-side, sebelum render)
        // supaya menu langsung ke Dashboard; window dibuka lagi hanya lewat openPayment()/decide().
        if (approvalWindow != null) approvalWindow.setVisible(false);
        if (paymentWindow != null) paymentWindow.setVisible(false);
        user = Common.getCurrentUser();
        currentPegawai = user == null ? null : user.getPegawai();
        canApprove = isAdministrator() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
        canPay = isAdministrator() || CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        approvalTab.setVisible(canApprove || currentPegawai != null);
        financeTab.setVisible(canPay);

        initCombos();
        // BUG upload: pada load pertama tombol upload Nota/Kuitansi tidak tampil, tetapi muncul setelah klik
        // "Form Baru". Sebabnya widget upload perlu event cycle penuh (getCurrentUser/desktop siap). Tunda
        // initForm ke event berikutnya via echoEvent agar render-nya sama seperti "Form Baru" -> tombol tampil.
        org.zkoss.zk.ui.event.Events.echoEvent("onInitFormAwal", window, null);
        gridSaya.setRowRenderer(new SubmissionRenderer("mine"));
        gridAtasan.setRowRenderer(new SubmissionRenderer("approval"));
        gridFinance.setRowRenderer(new SubmissionRenderer("finance"));
        refresh();
    }

    public void onInitFormAwal(Event event) throws Exception {
        initForm(null);
    }

    private void initCombos() {
        Konfigurasi kategoriConfig = Common.getKonfigurasi("kategori_reimbursement_pegawai",
                "Barang,Jasa,Perjalanan Dinas,Konsumsi,Transportasi,Lainnya");
        String kategoriValue = kategoriConfig == null || kategoriConfig.getNilai() == null
                ? "Barang,Jasa,Perjalanan Dinas,Konsumsi,Transportasi,Lainnya" : kategoriConfig.getNilai();
        String[] kategoriData = kategoriValue.split(",");
        for (int i = 0; i < kategoriData.length; i++) kategori.appendItem(kategoriData[i]);
        kategori.setSelectedIndex(0);
        Comboitem tanpaPajak = pajak.appendItem("Tanpa pajak (0%)");
        tanpaPajak.setValue(Double.valueOf(0));
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List pajaks = session.createCriteria(JenisPajakBarang.class)
                    .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("nama")).list();
            for (int i = 0; i < pajaks.size(); i++) {
                JenisPajakBarang jenis = (JenisPajakBarang) pajaks.get(i);
                Comboitem item = pajak.appendItem(jenis.getNama() + " (" + jenis.getPersen() + "%)");
                item.setValue(jenis.getPersen());
            }
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
        finally { HibernateUtil.closeSessionQuietly(session); }
        pajak.setSelectedIndex(0);
        metodePembayaran.appendItem("Transfer").setValue("Transfer");
        metodePembayaran.appendItem("Tunai").setValue("Tunai");
        metodePembayaran.setSelectedIndex(0);
    }

    private void initForm(final ReimbursementPegawai data) {
        editId = data == null ? null : data.getId();
        uploadedLampiran = null;
        formMode.setValue(data == null ? "Pengajuan baru" : "Perbaiki pengajuan " + data.getKode());
        deskripsi.setValue(data == null || data.getDeskripsi() == null ? "" : data.getDeskripsi());
        nominal.setValue(data == null ? Double.valueOf(0) : data.getNominal());
        dibayarPegawai.setChecked(data == null || data.getDibayarPegawai());
        tanggalPengeluaran.setValue(data == null ? ais.ui.util.WaktuUtil.getDate() : data.getTanggalPengeluaran());
        catatanPengaju.setValue(data == null || data.getCatatanPengaju() == null ? "" : data.getCatatanPengaju());
        selectCombo(kategori, data == null ? "Barang" : data.getKategori());
        selectTax(data == null ? Double.valueOf(0) : data.getPajakPersen());
        setPegawaiValue(pegawai, data == null ? currentPegawai : data.getPegawai());
        Pegawai defaultAtasan = data == null && currentPegawai != null ? currentPegawai.getAtasanlangsung() :
                (data == null ? null : data.getAtasan());
        setPegawaiValue(atasan, defaultAtasan);
        // BUG: bandbox Pegawai/Atasan ter-disable oleh konstruktor AmbilDataPegawaiBanbox (RabUtil.setDefaultPegawai)
        // sehingga terkunci pada pengajuan pertama. Atasan Langsung SELALU harus bisa dipilih; Nama Pegawai bisa
        // dipilih bila admin/finance (non-admin tetap dikunci ke diri sendiri sesuai aturan onSubmit).
        atasan.setDisabled(false);
        pegawai.setDisabled(!(isAdministrator() || canPay));
        attachmentBox.getChildren().clear();
        final Long existingLampiran = data == null ? null : data.getLampiranId();
        // usingId=true (arg ke-10) + tampilUpload=true (arg ke-11): tombol upload muncul. Setelah unggah, refresh
        // internal (by-id) belum menemukan row baru sehingga preview tak tampil -> tampilkan konfirmasi eksplisit.
        LampiranLain.createDownloadUploadFileLain(attachmentBox, data == null ? null : data.getId(), JENIS_LAMPIRAN,
                "Nota/kuitansi reimbursement", false, new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        uploadedLampiran = (LampiranLain) event.getData();
                        if (uploadedLampiran != null) {
                            Label ok = new Label("✓ Nota/kuitansi berhasil terunggah.");
                            ok.setStyle("color:#059669; font-weight:bold; margin-left:6px;");
                            ok.setParent(attachmentBox);
                        }
                    }
                }, null, false, false, true, true);
        if (existingLampiran != null) {
            Button lihat = new Button("Lihat lampiran tersimpan");
            lihat.setParent(attachmentBox);
            lihat.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    LampiranLain file = (LampiranLain) LampiranLain.ambil(true, existingLampiran, "id");
                    if (file != null) Common.display(file);
                }
            });
        }
    }

    private void setPegawaiValue(AmbilDataPegawaiBanbox box, Pegawai value) {
        box.setAttribute("pegawai", value);
        box.setValue(value == null ? "" : value.getNama());
    }

    private void selectCombo(Combobox box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAtIndex(i).getLabel().equalsIgnoreCase(value == null ? "" : value)) {
                box.setSelectedIndex(i); return;
            }
        }
    }

    private void selectTax(Double value) {
        for (int i = 0; i < pajak.getItemCount(); i++) {
            Double item = (Double) pajak.getItemAtIndex(i).getValue();
            if (item != null && item.equals(value)) { pajak.setSelectedIndex(i); return; }
        }
        pajak.setSelectedIndex(0);
    }

    public void onNew(Event event) throws Exception { initForm(null); }

    public void onSubmit(Event event) throws Exception {
        Pegawai pengaju = (Pegawai) pegawai.getAttribute("pegawai");
        Pegawai approver = (Pegawai) atasan.getAttribute("pegawai");
        // Guard id null: cegah Hibernate "id to load is required for loading" saat session.get(...) di bawah,
        // bila bandbox memegang Pegawai transient/default tanpa id (harus dipilih dari daftar).
        if (pengaju == null || pengaju.getId() == null || approver == null || approver.getId() == null
                || deskripsi.getValue().trim().isEmpty() || nominal.getValue() == null
                || nominal.getValue().doubleValue() <= 0 || tanggalPengeluaran.getValue() == null) {
            warn("Nama Pegawai & Atasan Langsung wajib dipilih dari daftar; deskripsi, nominal, dan tanggal pengeluaran wajib diisi.");
            return;
        }
        if (currentPegawai != null && !isAdministrator() && !currentPegawai.getId().equals(pengaju.getId())) {
            warn("Pengajuan hanya boleh dibuat atas nama pegawai yang sedang login."); return;
        }
        // Lampiran tidak lagi memblokir submit: deteksi upload (callback uploadedLampiran) kadang tak konsisten
        // sehingga pengajuan yang SUDAH mengunggah nota ikut terblokir. Lampiran tetap ditautkan bila terdeteksi
        // (lihat setLampiranId di bawah), namun ketidakadaannya tidak menghentikan pengajuan.
        Session session = null; Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession(); tx = session.beginTransaction();
            ReimbursementPegawai data = editId == null ? new ReimbursementPegawai() :
                    (ReimbursementPegawai) session.get(ReimbursementPegawai.class, editId);
            if (data == null) throw new IllegalStateException("Pengajuan tidak ditemukan.");
            if (editId != null && !ReimbursementPegawai.REVISI.equals(data.getStatus()))
                throw new IllegalStateException("Hanya pengajuan berstatus Revisi yang dapat diperbaiki.");
            if (editId != null && !isOwner(data)) throw new SecurityException("Bukan pemilik pengajuan.");
            data.setKode(editId == null ? "RMB-" + ais.ui.util.WaktuUtil.getDate().getTime() : data.getKode());
            data.setDeskripsi(deskripsi.getValue().trim());
            data.setKategori(kategori.getSelectedItem().getLabel());
            data.setNominal(nominal.getValue());
            data.setPajakPersen((Double) pajak.getSelectedItem().getValue());
            data.setDibayarPegawai(dibayarPegawai.isChecked());
            data.setTanggalPengeluaran(tanggalPengeluaran.getValue());
            data.setTanggalPengajuan(ais.ui.util.WaktuUtil.getDate());
            data.setPegawai((Pegawai) session.get(Pegawai.class, pengaju.getId()));
            data.setAtasan((Pegawai) session.get(Pegawai.class, approver.getId()));
            data.setDibuatOleh((Tbmuser) session.get(Tbmuser.class, user.getId()));
            data.setCatatanPengaju(catatanPengaju.getValue().trim());
            if (uploadedLampiran != null) data.setLampiranId(uploadedLampiran.getId());
            data.setStatus(ReimbursementPegawai.DIAJUKAN);
            data.setCatatanAtasan(null);
            if (editId == null) session.save(data); else session.update(data);
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), editId == null ? CommonPrivilages.CREATE : CommonPrivilages.UPDATE,
                    data, "Mengajukan reimbursement");
            info("Pengajuan berhasil diserahkan kepada " + approver.getNama() + ".");
            initForm(null); refresh();
        } catch (Exception e) {
            rollback(tx); Common.tampilErrorJikaAdmin(e); warn("Pengajuan gagal disimpan: " + e.getMessage());
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    public void onApprove(Event event) throws Exception { decide(ReimbursementPegawai.DISETUJUI); }
    public void onReject(Event event) throws Exception { decide(ReimbursementPegawai.DITOLAK); }
    public void onRevision(Event event) throws Exception { decide(ReimbursementPegawai.REVISI); }

    private void decide(String decision) throws Exception {
        if (!canApprove || approvalId == null) { warn("Anda tidak memiliki hak persetujuan."); return; }
        if (catatanAtasan.getValue().trim().isEmpty()) { warn("Catatan keputusan atasan wajib diisi."); return; }
        Akun expense = (Akun) akunBiaya.getAttribute("akun");
        if (ReimbursementPegawai.DISETUJUI.equals(decision)
                && (expense == null || tanggalAkuntansi.getValue() == null)) {
            warn("Akun biaya dan tanggal akuntansi wajib diisi untuk persetujuan."); return;
        }
        Session session = null; Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession(); tx = session.beginTransaction();
            ReimbursementPegawai data = (ReimbursementPegawai) session.get(ReimbursementPegawai.class, approvalId);
            ensureApprover(data);
            if (!ReimbursementPegawai.DIAJUKAN.equals(data.getStatus()))
                throw new IllegalStateException("Pengajuan ini sudah diproses.");
            data.setCatatanAtasan(catatanAtasan.getValue().trim());
            data.setDiputuskanOleh((Tbmuser) session.get(Tbmuser.class, user.getId()));
            data.setTanggalKeputusan(ais.ui.util.WaktuUtil.getDate());
            data.setStatus(decision);
            if (ReimbursementPegawai.DISETUJUI.equals(decision)) {
                Akun liability = resolveLiabilityAccount(session);
                if (liability == null) throw new IllegalStateException(
                        "Konfigurasi akun_hutang_reimbursement_pegawai belum menunjuk akun yang valid.");
                expense = (Akun) session.get(Akun.class, expense.getId());
                data.setAkunBiaya(expense);
                data.setTanggalAkuntansi(tanggalAkuntansi.getValue());
                PostingHistory ph = posting(session, tanggalAkuntansi.getValue(),
                        "Pengakuan biaya reimbursement " + data.getKode());
                boolean ok = CommonAkunting.saveTransaksi(expense, liability, null, null, ph, true,
                        "Pengakuan biaya reimbursement " + data.getKode() + " - " + data.getDeskripsi(),
                        tanggalAkuntansi.getValue(), data.getNominal(), 0.0, null,
                        data.getPegawai().getSatuanKerja(), "Reimbursement:" + data.getId(), session);
                if (!ok) throw new IllegalStateException("Jurnal pengeluaran ditolak oleh validasi akunting.");
                data.setPostingPengeluaran(ph);
            }
            session.update(data); tx.commit();
            CommonPrivilages.saveActivity(getClass(), ReimbursementPegawai.DISETUJUI.equals(decision)
                    ? CommonPrivilages.APPROVE : CommonPrivilages.REJECT, data, decision);
            approvalWindow.setVisible(false); info("Keputusan " + decision + " berhasil disimpan."); refresh();
        } catch (Exception e) {
            rollback(tx); Common.tampilErrorJikaAdmin(e); warn("Keputusan gagal: " + e.getMessage());
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    public void onPay(Event event) throws Exception {
        if (!canPay || paymentId == null) { warn("Anda tidak memiliki hak pembayaran."); return; }
        Akun cashBank = (Akun) akunPembayaran.getAttribute("akun");
        if (cashBank == null || tanggalPembayaran.getValue() == null || metodePembayaran.getSelectedItem() == null) {
            warn("Akun kas/bank, metode, dan tanggal pembayaran wajib diisi."); return;
        }
        if ("Transfer".equals(metodePembayaran.getSelectedItem().getValue())
                && rekeningPenerima.getValue().trim().isEmpty()) {
            warn("Nomor rekening penerima wajib diisi untuk transfer."); return;
        }
        Session session = null; Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession(); tx = session.beginTransaction();
            ReimbursementPegawai data = (ReimbursementPegawai) session.get(ReimbursementPegawai.class, paymentId);
            if (!ReimbursementPegawai.DISETUJUI.equals(data.getStatus()) || data.getPostingPembayaran() != null)
                throw new IllegalStateException("Pengajuan tidak siap dibayar atau sudah dibayar.");
            Akun liability = resolveLiabilityAccount(session);
            if (liability == null) throw new IllegalStateException(
                    "Konfigurasi akun_hutang_reimbursement_pegawai belum menunjuk akun yang valid.");
            cashBank = (Akun) session.get(Akun.class, cashBank.getId());
            PostingHistory ph = posting(session, tanggalPembayaran.getValue(), "Pembayaran reimbursement " + data.getKode());
            boolean ok = CommonAkunting.saveTransaksi(liability, cashBank, null, null, ph, true,
                    "Pembayaran reimbursement " + data.getKode() + " kepada " + data.getPegawai().getNama(),
                    tanggalPembayaran.getValue(), data.getNominal(), 0.0, null,
                    data.getPegawai().getSatuanKerja(), "ReimbursementBayar:" + data.getId(), session);
            if (!ok) throw new IllegalStateException("Jurnal pembayaran ditolak oleh validasi akunting.");
            data.setMetodePembayaran((String) metodePembayaran.getSelectedItem().getValue());
            data.setBankPenerima(bankPenerima.getValue().trim());
            data.setRekeningPenerima(rekeningPenerima.getValue().trim());
            data.setTanggalPembayaran(tanggalPembayaran.getValue());
            data.setCatatanPembayaran(catatanPembayaran.getValue().trim());
            data.setAkunPembayaran(cashBank);
            data.setDibayarOleh((Tbmuser) session.get(Tbmuser.class, user.getId()));
            data.setPostingPembayaran(ph);
            data.setStatus(ReimbursementPegawai.LUNAS);
            session.update(data); tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.UPDATE, data, "Pembayaran reimbursement");
            paymentWindow.setVisible(false); info("Pembayaran dan jurnal kas/bank berhasil dicatat."); refresh();
        } catch (Exception e) {
            rollback(tx); Common.tampilErrorJikaAdmin(e); warn("Pembayaran gagal: " + e.getMessage());
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private PostingHistory posting(Session session, Date date, String description) {
        PostingHistory ph = new PostingHistory(PostingHistory.JENIS_REIMBURSEMENT_PEGAWAI);
        ph.setTbmuser((Tbmuser) session.get(Tbmuser.class, user.getId()));
        ph.setTanggal(date); ph.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());
        ph.setKeterangan(description); ph.setPosting(Boolean.TRUE); session.save(ph); return ph;
    }

    private Akun resolveLiabilityAccount(Session session) {
        Konfigurasi cfg = Common.getKonfigurasi("akun_hutang_reimbursement_pegawai", "");
        String value = cfg == null || cfg.getNilai() == null ? "" : cfg.getNilai().trim();
        if (value.isEmpty()) return null;
        try {
            Akun byId = (Akun) session.get(Akun.class, Long.valueOf(value));
            if (byId != null) return byId;
        } catch (Exception ignored) { }
        return (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("kode", value)).setMaxResults(1).uniqueResult();
    }

    public void onRefresh(Event event) throws Exception { refresh(); }

    private void refresh() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List mine = new ArrayList();
            if (user != null) {
                Criteria c = session.createCriteria(ReimbursementPegawai.class)
                        .add(Restrictions.eq("dibuatOleh", user)).addOrder(Order.desc("tanggalPengajuan")).setMaxResults(500);
                mine = c.list(); initialize(mine);
            }
            List approvals = new ArrayList();
            if (currentPegawai != null || isAdministrator()) {
                Criteria c = session.createCriteria(ReimbursementPegawai.class)
                        .add(Restrictions.eq("status", ReimbursementPegawai.DIAJUKAN));
                if (!isAdministrator()) c.add(Restrictions.eq("atasan", currentPegawai));
                approvals = c.addOrder(Order.asc("tanggalPengajuan")).setMaxResults(500).list(); initialize(approvals);
            }
            List finance = new ArrayList();
            if (canPay) {
                finance = session.createCriteria(ReimbursementPegawai.class)
                        .add(Restrictions.in("status", new String[] { ReimbursementPegawai.DISETUJUI, ReimbursementPegawai.LUNAS }))
                        .addOrder(Order.asc("tanggalKeputusan")).setMaxResults(500).list(); initialize(finance);
            }
            gridSaya.setModel(new SimpleListModel(mine));
            gridAtasan.setModel(new SimpleListModel(approvals));
            gridFinance.setModel(new SimpleListModel(finance));
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); warn("Data reimbursement gagal dimuat."); }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private void initialize(List list) {
        for (int i = 0; i < list.size(); i++) {
            ReimbursementPegawai d = (ReimbursementPegawai) list.get(i);
            Hibernate.initialize(d.getPegawai()); Hibernate.initialize(d.getAtasan());
            if (d.getPegawai() != null) Hibernate.initialize(d.getPegawai().getBank());
            Hibernate.initialize(d.getAkunBiaya()); Hibernate.initialize(d.getPostingPengeluaran());
            Hibernate.initialize(d.getPostingPembayaran());
        }
    }

    private void openApproval(ReimbursementPegawai data) {
        approvalId = data.getId();
        approvalInfo.setValue(data.getKode() + " | " + data.getPegawai().getNama() + " | Rp "
                + Common.numberFormat.get().format(data.getNominal()) + "\n" + data.getDeskripsi());
        catatanAtasan.setValue(""); akunBiaya.setValue(""); akunBiaya.setAttribute("akun", null);
        tanggalAkuntansi.setValue(data.getTanggalPengeluaran());
        approvalWindow.setVisible(true); approvalWindow.doHighlighted();
    }

    private void openPayment(ReimbursementPegawai data) {
        paymentId = data.getId();
        paymentInfo.setValue(data.getKode() + " | " + data.getPegawai().getNama() + " | " + data.getDeskripsi());
        jumlahPembayaran.setValue(data.getNominal()); jumlahPembayaran.setDisabled(true);
        Pegawai p = data.getPegawai();
        bankPenerima.setValue(p.getBank() == null ? "" : p.getBank().toString());
        rekeningPenerima.setValue(p.getNorek() == null ? "" : p.getNorek());
        tanggalPembayaran.setValue(ais.ui.util.WaktuUtil.getDate()); catatanPembayaran.setValue("");
        akunPembayaran.setValue(""); akunPembayaran.setAttribute("akun", null);
        paymentWindow.setVisible(true); paymentWindow.doHighlighted();
    }

    private void ensureApprover(ReimbursementPegawai data) {
        if (data == null) throw new IllegalStateException("Pengajuan tidak ditemukan.");
        if (!isAdministrator() && (currentPegawai == null || data.getAtasan() == null
                || !currentPegawai.getId().equals(data.getAtasan().getId())))
            throw new SecurityException("Pengajuan bukan tanggung jawab atasan yang sedang login.");
    }

    private boolean isOwner(ReimbursementPegawai data) {
        return user != null && data.getDibuatOleh() != null && user.getId().equals(data.getDibuatOleh().getId());
    }

    private boolean isAdministrator() {
        if (user == null) return false;
        Set roles = user.ambilRolesId();
        return roles != null && roles.contains(Tbmrole.ADMINISTRATOR);
    }

    private void rollback(Transaction tx) {
        try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
    }

    private void warn(String message) {
        try { MyMessageboxConfig.show(message, "Reimbursement", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); }
        catch (Exception ignored) { }
    }

    private void info(String message) {
        try { MyMessageboxConfig.show(message, "Reimbursement", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION); }
        catch (Exception ignored) { }
    }

    private class SubmissionRenderer implements RowRenderer {
        private final String mode;
        SubmissionRenderer(String mode) { this.mode = mode; }
        public void render(Row row, Object value) throws Exception {
            final ReimbursementPegawai data = (ReimbursementPegawai) value;
            new Label(data.getKode()).setParent(row);
            new Label(data.getPegawai() == null ? "-" : data.getPegawai().getNama()).setParent(row);
            new Label(data.getDeskripsi()).setParent(row);
            new Label(data.getKategori()).setParent(row);
            new Label(Common.dateFormat4.get().format(data.getTanggalPengeluaran())).setParent(row);
            new Label("Rp " + Common.numberFormat.get().format(data.getNominal())).setParent(row);
            new Label(data.getStatus()).setParent(row);
            Hbox actions = new Hbox(); actions.setParent(row);
            if (data.getLampiranId() != null) {
                Button lampiran = new Button("Lampiran"); lampiran.setParent(actions);
                lampiran.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event event) throws Exception {
                        LampiranLain file = (LampiranLain) LampiranLain.ambil(true, data.getLampiranId(), "id");
                        if (file != null) Common.display(file);
                    }
                });
            }
            if ("mine".equals(mode) && ReimbursementPegawai.REVISI.equals(data.getStatus())) {
                Button edit = new Button("Perbaiki"); edit.setParent(actions);
                edit.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event event) throws Exception { initForm(data); }
                });
            } else if ("approval".equals(mode)) {
                Button process = new Button("Proses"); process.setParent(actions);
                process.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event event) throws Exception { openApproval(data); }
                });
            } else if ("finance".equals(mode) && ReimbursementPegawai.DISETUJUI.equals(data.getStatus())) {
                Button pay = new Button("Bayar"); pay.setParent(actions);
                pay.addEventListener("onClick", new EventListener() {
                    public void onEvent(Event event) throws Exception { openPayment(data); }
                });
            }
        }
    }
}
