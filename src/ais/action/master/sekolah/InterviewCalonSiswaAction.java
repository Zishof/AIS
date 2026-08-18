package ais.action.master.sekolah;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.InterviewCalonSiswa;
import ais.database.model.sekolah.InterviewPunyaCalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class InterviewCalonSiswaAction extends org.zkoss.zk.ui.util.GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    // Auto-wired by GenericAutowireComposer (field name = component id in ZUL)
    Listbox lbDaftarSesi;
    Button  btnBuatSesiBar;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        if (btnBuatSesiBar != null) {
            btnBuatSesiBar.addEventListener("onClick", new EventListener() {
                public void onEvent(Event ev) throws Exception {
                    bukaFormSesi(null);
                }
            });
        }
        muatDaftarSesi(null);
    }

    // ── MUAT DAFTAR SESI ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void muatDaftarSesi(GelombangPendaftaranPsb gelombangFilter) {
        if (lbDaftarSesi == null) { return; }
        lbDaftarSesi.getItems().clear();

        Session s = HibernateUtil.currentSession();
        org.hibernate.Criteria c = s.createCriteria(InterviewCalonSiswa.class);
        if (gelombangFilter != null) {
            c.add(Restrictions.eq("gelombangPendaftaranPsb", gelombangFilter));
        }
        c.addOrder(Order.desc("id"));

        List<InterviewCalonSiswa> list = c.list();
        for (final InterviewCalonSiswa ics : list) {
            Listitem li = new Listitem();
            li.setValue(ics);

            new Listcell(ics.getId() != null ? ics.getId().toString() : "-").setParent(li);
            new Listcell(ics.getNama() != null ? ics.getNama() : "-").setParent(li);
            new Listcell(ics.getTahunAjaran() != null ? ics.getTahunAjaran() : "-").setParent(li);

            String waktu = "-";
            if (ics.getMulai() != null && ics.getSampai() != null) {
                waktu = Common.dateFormat51.get().format(ics.getMulai())
                    + " – " + Common.dateFormat51.get().format(ics.getSampai());
            } else if (ics.getMulai() != null) {
                waktu = Common.dateFormat51.get().format(ics.getMulai());
            }
            new Listcell(waktu).setParent(li);

            String pegawaiNama = ics.getPegawai() != null && ics.getPegawai().getNama() != null
                ? ics.getPegawai().getNama() : "-";
            new Listcell(pegawaiNama).setParent(li);

            Listcell lcAksi = new Listcell();
            lcAksi.setParent(li);

            Button btnEdit = new Button(Common.getBahasaConfig("Edit"));
            btnEdit.setSclass("btn btn-sm btn-outline-primary me-1");
            btnEdit.addEventListener("onClick", new EventListener() {
                public void onEvent(Event ev) throws Exception { bukaFormSesi(ics); }
            });
            btnEdit.setParent(lcAksi);

            Button btnPeserta = new Button(Common.getBahasaConfig("Peserta"));
            btnPeserta.setSclass("btn btn-sm btn-outline-success me-1");
            btnPeserta.addEventListener("onClick", new EventListener() {
                public void onEvent(Event ev) throws Exception { bukaFormPeserta(ics); }
            });
            btnPeserta.setParent(lcAksi);

            Button btnHapus = new Button(Common.getBahasaConfig("Hapus"));
            btnHapus.setSclass("btn btn-sm btn-outline-danger");
            btnHapus.addEventListener("onClick", new EventListener() {
                public void onEvent(Event ev) throws Exception { hapusSesi(ics); }
            });
            btnHapus.setParent(lcAksi);

            li.setParent(lbDaftarSesi);
        }
    }

    // ── HELPER ──────────────────────────────────────────────────────────────

    /** Membangun Grid 2-kolom di dalam parent dan mengembalikan Rows-nya. */
    private static Rows buatFormGrid(Component parent) {
        Grid grid = new Grid();
        grid.setHflex("1");
        Columns cols = new Columns();
        cols.setParent(grid);
        Column c1 = new Column();
        c1.setWidth("38%");
        c1.setParent(cols);
        new Column().setParent(cols);
        Rows rows = new Rows();
        rows.setParent(grid);
        grid.setParent(parent);
        return rows;
    }

    /** Menambah satu baris form (label + komponen input) ke dalam Rows. */
    private static void addRow(Rows rows, String label, Component input) {
        MyFormRow row = new MyFormRow();
        new Label(Common.getBahasaConfig(label) + ":").setParent(row);
        input.setParent(row);
        row.setParent(rows);
    }

    // ── FORM SESI INTERVIEW ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void bukaFormSesi(final InterviewCalonSiswa ics) throws Exception {
        final boolean isNew = (ics == null || ics.getId() == null);
        final InterviewCalonSiswa data = isNew ? new InterviewCalonSiswa() : ics;
        String judul = isNew ? Common.getBahasaConfig("Buat Sesi Wawancara Baru")
                             : Common.getBahasaConfig("Edit Sesi Wawancara");

        final MyWindow win = new MyWindow(judul, "normal", true);
        win.setWidth("720px");

        Rows rows = buatFormGrid(win);

        final Textbox tbNama = new Textbox(data.getNama() != null ? data.getNama() : "");
        tbNama.setHflex("1");
        addRow(rows, "Nama Sesi *", tbNama);

        final Textbox tbTA = new Textbox(data.getTahunAjaran() != null ? data.getTahunAjaran() : "");
        tbTA.setHflex("1");
        addRow(rows, "Tahun Ajaran", tbTA);

        final Datebox dbMulai = new Datebox();
        dbMulai.setFormat("dd/MM/yyyy HH:mm");
        if (data.getMulai() != null) { dbMulai.setValue(data.getMulai()); }
        addRow(rows, "Mulai *", dbMulai);

        final Datebox dbSampai = new Datebox();
        dbSampai.setFormat("dd/MM/yyyy HH:mm");
        if (data.getSampai() != null) { dbSampai.setValue(data.getSampai()); }
        addRow(rows, "Sampai *", dbSampai);

        final Combobox cbPlatform = new Combobox();
        cbPlatform.setHflex("1");
        cbPlatform.setReadonly(true);
        String[][] platforms = {
            { "0", "Tidak Aktif / Tatap Muka" },
            { "1", "Jitsi Meet" },
            { "2", "Google Meet" },
            { "3", "Zoom" },
            { "4", "BigBlueButton" },
            { "5", "Skype" },
            { "6", "WhatsApp" },
            { "7", "Lainnya" }
        };
        for (String[] p : platforms) {
            int kode = Integer.parseInt(p[0]);
            Comboitem ci = new Comboitem(p[1]);
            ci.setValue(Integer.valueOf(kode));
            ci.setParent(cbPlatform);
            if (data.getOnlineMenggunakan() == kode) { cbPlatform.setSelectedItem(ci); }
        }
        addRow(rows, "Platform Video", cbPlatform);

        final Textbox tbZoom  = new Textbox(data.getZoomLink()  != null ? data.getZoomLink()  : "");
        tbZoom.setHflex("1");
        addRow(rows, "Link Zoom", tbZoom);

        final Textbox tbBbb   = new Textbox(data.getBbbLink()   != null ? data.getBbbLink()   : "");
        tbBbb.setHflex("1");
        addRow(rows, "Link BigBlueButton", tbBbb);

        final Textbox tbSkype = new Textbox(data.getSkypeLink() != null ? data.getSkypeLink() : "");
        tbSkype.setHflex("1");
        addRow(rows, "Link Skype", tbSkype);

        final Textbox tbWa    = new Textbox(data.getWaLink()    != null ? data.getWaLink()    : "");
        tbWa.setHflex("1");
        addRow(rows, "No. WhatsApp / Link WA", tbWa);

        final Textbox tbLain  = new Textbox(data.getLainLink()  != null ? data.getLainLink()  : "");
        tbLain.setHflex("1");
        addRow(rows, "Link Lain (Google Meet / Lainnya)", tbLain);

        final Intbox ibKap = new Intbox();
        if (data.getKapasitasRuangan() != null) { ibKap.setValue(data.getKapasitasRuangan()); }
        addRow(rows, "Kapasitas Ruangan", ibKap);

        final Textbox tbKet = new Textbox(data.getKeterangan() != null ? data.getKeterangan() : "");
        tbKet.setHflex("1");
        tbKet.setMultiline(true);
        tbKet.setRows(3);
        addRow(rows, "Keterangan", tbKet);

        // Pewawancara (Pegawai) — Combobox
        final Combobox cbPegawai = new Combobox();
        cbPegawai.setHflex("1");
        cbPegawai.setAutocomplete(true);
        Session sLoad = HibernateUtil.currentSession();
        List<Pegawai> pegawaiList = sLoad.createCriteria(Pegawai.class)
            .addOrder(Order.asc("nama")).setMaxResults(500).list();
        for (Pegawai p : pegawaiList) {
            Comboitem ci = new Comboitem(p.getNama() != null ? p.getNama() : "");
            ci.setValue(p.getId());
            ci.setParent(cbPegawai);
            if (data.getPegawai() != null && p.getId() != null
                    && p.getId().equals(data.getPegawai().getId())) {
                cbPegawai.setSelectedItem(ci);
            }
        }
        addRow(rows, "Pewawancara (Pegawai)", cbPegawai);

        // Gelombang PSB — Combobox
        final Combobox cbGelombang = new Combobox();
        cbGelombang.setHflex("1");
        cbGelombang.setReadonly(true);
        List<GelombangPendaftaranPsb> gelList = sLoad.createCriteria(GelombangPendaftaranPsb.class)
            .addOrder(Order.desc("id")).setMaxResults(100).list();
        for (GelombangPendaftaranPsb g : gelList) {
            Comboitem ci = new Comboitem(g.getNama() != null ? g.getNama() : "");
            ci.setValue(g.getId());
            ci.setParent(cbGelombang);
            if (data.getGelombangPendaftaranPsb() != null && g.getId() != null
                    && g.getId().equals(data.getGelombangPendaftaranPsb().getId())) {
                cbGelombang.setSelectedItem(ci);
            }
        }
        addRow(rows, "Gelombang PSB", cbGelombang);

        Hbox hboxFooterSesi = new Hbox();
        hboxFooterSesi.setSpacing("8px");
        hboxFooterSesi.setParent(win);

        MyToolbarbuttonConfig btnBatalSesi = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        btnBatalSesi.setTooltiptext("Batal");
        btnBatalSesi.setParent(hboxFooterSesi);
        btnBatalSesi.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                win.detach();
            }
        });

        Button btnSimpan = new Button(Common.getBahasaConfig("Simpan"));
        btnSimpan.setSclass("btn btn-primary");
        btnSimpan.setParent(hboxFooterSesi);
        btnSimpan.addEventListener("onClick", new EventListener() {
            @SuppressWarnings("unchecked")
            public void onEvent(Event ev) throws Exception {
                String nama = tbNama.getValue() != null ? tbNama.getValue().trim() : "";
                if (nama.isEmpty()) {
                    MyMessageboxConfig.show(Common.getBahasaConfig("Nama sesi tidak boleh kosong."));
                    return;
                }
                if (dbMulai.getValue() == null || dbSampai.getValue() == null) {
                    MyMessageboxConfig.show(Common.getBahasaConfig("Waktu mulai dan sampai wajib diisi."));
                    return;
                }
                if (dbSampai.getValue().before(dbMulai.getValue())) {
                    MyMessageboxConfig.show(Common.getBahasaConfig("Waktu sampai tidak boleh sebelum waktu mulai."));
                    return;
                }

                data.setNama(nama);
                String ta = tbTA.getValue() != null ? tbTA.getValue().trim() : "";
                data.setTahunAjaran(ta.isEmpty() ? null : ta);
                data.setMulai(dbMulai.getValue());
                data.setSampai(dbSampai.getValue());
                if (cbPlatform.getSelectedItem() != null) {
                    data.setOnlineMenggunakan((Integer) cbPlatform.getSelectedItem().getValue());
                }
                data.setZoomLink(tbZoom.getValue().trim().isEmpty()  ? null : tbZoom.getValue().trim());
                data.setBbbLink(tbBbb.getValue().trim().isEmpty()    ? null : tbBbb.getValue().trim());
                data.setSkypeLink(tbSkype.getValue().trim().isEmpty()? null : tbSkype.getValue().trim());
                data.setWaLink(tbWa.getValue().trim().isEmpty()      ? null : tbWa.getValue().trim());
                data.setLainLink(tbLain.getValue().trim().isEmpty()  ? null : tbLain.getValue().trim());
                data.setKapasitasRuangan(ibKap.getValue() != null ? ibKap.getValue() : null);
                String ket = tbKet.getValue() != null ? tbKet.getValue().trim() : "";
                data.setKeterangan(ket.isEmpty() ? null : ket);

                Session sOp = HibernateUtil.currentSession();
                if (cbPegawai.getSelectedItem() != null) {
                    Long pgId = (Long) cbPegawai.getSelectedItem().getValue();
                    data.setPegawai((Pegawai) sOp.get(Pegawai.class, pgId));
                }
                if (cbGelombang.getSelectedItem() != null) {
                    Long gId = (Long) cbGelombang.getSelectedItem().getValue();
                    data.setGelombangPendaftaranPsb(
                        (GelombangPendaftaranPsb) sOp.get(GelombangPendaftaranPsb.class, gId));
                }

                Sekolah skolah = SekolahUtil.getSekolah();
                Yayasan yayasan = SekolahUtil.getYayasan();
                if (skolah  != null) { data.setSekolah(skolah);  }
                if (yayasan != null) { data.setYayasan(yayasan); }

                Transaction tx = sOp.beginTransaction();
                sOp.saveOrUpdate(data);
                tx.commit();

                win.detach();
                muatDaftarSesi(null);
                MyMessageboxConfig.show(Common.getBahasaConfig("Sesi wawancara berhasil disimpan."));
            }
        });

        win.doModal();
    }

    // ── FORM PESERTA ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void bukaFormPeserta(final InterviewCalonSiswa ics) throws Exception {
        if (ics == null) { return; }
        String judul = Common.getBahasaConfig("Peserta Wawancara: ") + ics.getNama();
        final MyWindow win = new MyWindow(judul, "normal", true);
        win.setWidth("80%");

        Session s = HibernateUtil.currentSession();
        List<InterviewPunyaCalonSiswa> listPeserta = s.createCriteria(InterviewPunyaCalonSiswa.class)
            .add(Restrictions.eq("interviewCalonSiswa", ics))
            .addOrder(Order.asc("id"))
            .list();

        Listbox lbPeserta = new Listbox();
        lbPeserta.setHeight("280px");
        lbPeserta.setHflex("1");
        lbPeserta.setParent(win);

        Listhead lhead = new Listhead();
        lhead.setParent(lbPeserta);
        String[] kolom = { "ID", Common.getBahasaConfig("Nama"), Common.getBahasaConfig("No Reg"),
                           Common.getBahasaConfig("Waktu Khusus"),
                           Common.getBahasaConfig("Siap"), Common.getBahasaConfig("Aksi") };
        for (String k : kolom) { new Listheader(k).setParent(lhead); }

        for (final InterviewPunyaCalonSiswa rec : listPeserta) {
            Listitem li = new Listitem();
            new Listcell(rec.getId() != null ? rec.getId().toString() : "-").setParent(li);
            String namaCasis = rec.getCalonSiswa() != null && rec.getCalonSiswa().getNama() != null
                ? rec.getCalonSiswa().getNama() : "-";
            new Listcell(namaCasis).setParent(li);
            String noReg = rec.getCalonSiswa() != null && rec.getCalonSiswa().getNoRegistrasi() != null
                ? rec.getCalonSiswa().getNoRegistrasi() : "-";
            new Listcell(noReg).setParent(li);

            String waktuKhusus = "-";
            if (rec.getMulai() != null) {
                waktuKhusus = Common.dateFormat51.get().format(rec.getMulai());
                if (rec.getSampai() != null) {
                    waktuKhusus += " – " + Common.dateFormat51.get().format(rec.getSampai());
                }
            }
            new Listcell(waktuKhusus).setParent(li);
            new Listcell(Boolean.TRUE.equals(rec.getSiap()) ? "Ya" : "-").setParent(li);

            Listcell lcAksi = new Listcell();
            Button btnHapusPeserta = new Button(Common.getBahasaConfig("Hapus"));
            btnHapusPeserta.setSclass("btn btn-sm btn-outline-danger");
            btnHapusPeserta.addEventListener("onClick", new EventListener() {
                public void onEvent(Event ev) throws Exception {
                    Session sHapus = HibernateUtil.currentSession();
                    Transaction tx = sHapus.beginTransaction();
                    InterviewPunyaCalonSiswa managed = (InterviewPunyaCalonSiswa)
                        sHapus.get(InterviewPunyaCalonSiswa.class, rec.getId());
                    if (managed != null) { sHapus.delete(managed); }
                    tx.commit();
                    win.detach();
                    bukaFormPeserta(ics);
                }
            });
            btnHapusPeserta.setParent(lcAksi);
            lcAksi.setParent(li);
            li.setParent(lbPeserta);
        }

        Label lblTambah = new Label("──── " + Common.getBahasaConfig("Tambah Peserta") + " ────");
        lblTambah.setSclass("fw-bold text-primary");
        lblTambah.setParent(win);

        Rows rowsTambah = buatFormGrid(win);

        final Textbox tbNoReg = new Textbox();
        tbNoReg.setHflex("1");
        addRow(rowsTambah, "No. Registrasi Calon Siswa *", tbNoReg);

        final Datebox dbMulaiP = new Datebox();
        dbMulaiP.setFormat("dd/MM/yyyy HH:mm");
        addRow(rowsTambah, "Waktu Mulai Khusus (opsional)", dbMulaiP);

        final Datebox dbSampaiP = new Datebox();
        dbSampaiP.setFormat("dd/MM/yyyy HH:mm");
        addRow(rowsTambah, "Waktu Sampai Khusus (opsional)", dbSampaiP);

        final Textbox tbCatatanP = new Textbox();
        tbCatatanP.setHflex("1");
        tbCatatanP.setMultiline(true);
        tbCatatanP.setRows(2);
        addRow(rowsTambah, "Catatan", tbCatatanP);

        Hbox hboxFooterPeserta = new Hbox();
        hboxFooterPeserta.setSpacing("8px");
        hboxFooterPeserta.setParent(win);

        MyToolbarbuttonConfig btnBatalPeserta = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        btnBatalPeserta.setTooltiptext("Batal");
        btnBatalPeserta.setParent(hboxFooterPeserta);
        btnBatalPeserta.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                win.detach();
            }
        });

        Button btnTambahPeserta = new Button(Common.getBahasaConfig("Tambahkan Peserta"));
        btnTambahPeserta.setSclass("btn btn-success");
        btnTambahPeserta.setParent(hboxFooterPeserta);
        btnTambahPeserta.addEventListener("onClick", new EventListener() {
            @SuppressWarnings("unchecked")
            public void onEvent(Event ev) throws Exception {
                String noReg = tbNoReg.getValue() != null ? tbNoReg.getValue().trim() : "";
                if (noReg.isEmpty()) {
                    MyMessageboxConfig.show(
                        Common.getBahasaConfig("Masukkan No. Registrasi calon siswa terlebih dahulu."));
                    return;
                }
                Session sFind = HibernateUtil.currentSession();
                CalonSiswa casis = (CalonSiswa) sFind.createCriteria(CalonSiswa.class)
                    .add(Restrictions.eq("noRegistrasi", noReg))
                    .setMaxResults(1)
                    .uniqueResult();
                if (casis == null) {
                    MyMessageboxConfig.show(
                        Common.getBahasaConfig("Calon siswa dengan No. Registrasi tersebut tidak ditemukan."));
                    return;
                }

                InterviewPunyaCalonSiswa rec = new InterviewPunyaCalonSiswa();
                rec.setInterviewCalonSiswa(ics);
                rec.setCalonSiswa(casis);
                rec.setMulai(dbMulaiP.getValue());
                rec.setSampai(dbSampaiP.getValue());
                String cat = tbCatatanP.getValue() != null ? tbCatatanP.getValue().trim() : "";
                rec.setKeterangan(cat.isEmpty() ? null : cat);

                Transaction tx = sFind.beginTransaction();
                sFind.save(rec);
                tx.commit();
                win.detach();
                bukaFormPeserta(ics);
            }
        });

        win.doModal();
    }

    // ── HAPUS SESI ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void hapusSesi(final InterviewCalonSiswa ics) throws Exception {
        if (ics == null || ics.getId() == null) { return; }
        String msg = Common.getBahasaConfig("Yakin menghapus sesi '") + ics.getNama()
            + Common.getBahasaConfig("'? Semua peserta di sesi ini ikut dihapus.");
        MyMessageboxConfig.show(msg, Common.getBahasaConfig("Konfirmasi"),
            MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
            MyMessageboxConfig.QUESTION,
            new EventListener() {
                public void onEvent(Event ev) throws Exception {
                    if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) { return; }
                    Session s = HibernateUtil.currentSession();
                    Transaction tx = s.beginTransaction();
                    List pesertaList = s.createCriteria(InterviewPunyaCalonSiswa.class)
                        .add(Restrictions.eq("interviewCalonSiswa", ics)).list();
                    for (Object o : pesertaList) { s.delete(o); }
                    InterviewCalonSiswa managed = (InterviewCalonSiswa)
                        s.get(InterviewCalonSiswa.class, ics.getId());
                    if (managed != null) { s.delete(managed); }
                    tx.commit();
                    muatDaftarSesi(null);
                    MyMessageboxConfig.show(Common.getBahasaConfig("Sesi wawancara berhasil dihapus."));
                }
            });
    }

    // ── STATIC HELPER — dipanggil dari halaman admin PSB ────────────────────

    /**
     * Tampilkan popup penugasan wawancara untuk satu calon siswa.
     * Popup berisi daftar sesi aktif hari ini dan memungkinkan admin
     * menugaskan calon ke salah satu sesi.
     */
    @SuppressWarnings("unchecked")
    public static void tampilkanInterview(final CalonSiswa casis, final Component container) {
        if (casis == null || container == null) { return; }

        String judul = Common.getBahasaConfig("Penugasan Wawancara: ") + casis.getNama();
        final MyWindow win = new MyWindow(judul, "normal", true);
        win.setWidth("60%");
        if (container.getPage() != null) { win.setPage(container.getPage()); }

        Session s = HibernateUtil.currentSession();
        String hariIni = Common.databaseDateFormat.get().format(WaktuUtil.getDate());
        List<InterviewCalonSiswa> sesiAktif = s.createCriteria(InterviewCalonSiswa.class)
            .add(Restrictions.sqlRestriction(
                "date('" + hariIni + "') between date(mulai) and date(sampai)"))
            .addOrder(Order.asc("mulai"))
            .list();

        if (sesiAktif.isEmpty()) {
            Label lbl = new Label(Common.getBahasaConfig("Tidak ada sesi wawancara aktif hari ini."));
            lbl.setSclass("text-muted fst-italic");
            lbl.setParent(win);

            MyToolbarbuttonConfig btnTutupKosong = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
            btnTutupKosong.setTooltiptext("Tutup");
            btnTutupKosong.setParent(win);
            btnTutupKosong.addEventListener("onClick", new EventListener() {
                public void onEvent(Event ev) throws Exception {
                    win.detach();
                }
            });

            try { win.doModal(); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/InterviewCalonSiswaAction.java:529"); }
            return;
        }

        InterviewPunyaCalonSiswa existing = (InterviewPunyaCalonSiswa) s
            .createCriteria(InterviewPunyaCalonSiswa.class)
            .add(Restrictions.eq("calonSiswa", casis))
            .setMaxResults(1)
            .uniqueResult();

        if (existing != null) {
            Label lbl = new Label(Common.getBahasaConfig("Sudah ditugaskan ke sesi: ")
                + (existing.getInterviewCalonSiswa() != null
                    ? existing.getInterviewCalonSiswa().getNama() : "-"));
            lbl.setSclass("text-success fw-bold");
            lbl.setParent(win);
        }

        new Label(Common.getBahasaConfig("Pilih sesi wawancara:")).setParent(win);

        final Combobox cbSesi = new Combobox();
        cbSesi.setHflex("1");
        cbSesi.setReadonly(true);
        cbSesi.setParent(win);

        for (InterviewCalonSiswa sesi : sesiAktif) {
            String label = sesi.getNama();
            if (sesi.getMulai() != null) {
                label += " [" + Common.dateFormat51.get().format(sesi.getMulai()) + "]";
            }
            Comboitem ci = new Comboitem(label);
            ci.setValue(sesi.getId());
            ci.setParent(cbSesi);
        }

        Hbox hboxFooterTugas = new Hbox();
        hboxFooterTugas.setSpacing("8px");
        hboxFooterTugas.setParent(win);

        MyToolbarbuttonConfig btnBatalTugas = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        btnBatalTugas.setTooltiptext("Batal");
        btnBatalTugas.setParent(hboxFooterTugas);
        btnBatalTugas.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                win.detach();
            }
        });

        Button btnTugaskan = new Button(Common.getBahasaConfig("Tugaskan ke Sesi Ini"));
        btnTugaskan.setSclass("btn btn-primary");
        btnTugaskan.setParent(hboxFooterTugas);
        btnTugaskan.addEventListener("onClick", new EventListener() {
            public void onEvent(Event ev) throws Exception {
                if (cbSesi.getSelectedItem() == null) {
                    MyMessageboxConfig.show(Common.getBahasaConfig("Pilih sesi wawancara terlebih dahulu."));
                    return;
                }
                Long sesiId = (Long) cbSesi.getSelectedItem().getValue();
                Session sNew = HibernateUtil.currentSession();
                InterviewCalonSiswa sesiDipilih = (InterviewCalonSiswa)
                    sNew.get(InterviewCalonSiswa.class, sesiId);
                if (sesiDipilih == null) {
                    MyMessageboxConfig.show(Common.getBahasaConfig("Sesi tidak ditemukan."));
                    return;
                }
                InterviewPunyaCalonSiswa rec = new InterviewPunyaCalonSiswa();
                rec.setInterviewCalonSiswa(sesiDipilih);
                rec.setCalonSiswa(casis);
                Transaction tx = sNew.beginTransaction();
                sNew.save(rec);
                tx.commit();
                win.detach();
                MyMessageboxConfig.show(casis.getNama()
                    + Common.getBahasaConfig(" berhasil ditugaskan ke sesi wawancara."));
            }
        });

        try { win.doModal(); } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/InterviewCalonSiswaAction.java:593"); }
    }
}
