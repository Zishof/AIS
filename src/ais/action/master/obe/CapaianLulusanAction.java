package ais.action.master.obe;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.KategoriCpl;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public class CapaianLulusanAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox  kode;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox fakultas;
    private Combobox jurusan;
    private Combobox cbKategori;
    private AmbilDataMatakuliahBanbox khususBuatMk;

    // Multi-select
    private HashMap<Long, ProfilLulusan>   selectedProfilLulusan;
    private HashMap<Long, BahanKajian>     selectedBahanKajian;
    private HashMap<Long, ReferensiLulusan> selectedReferensiLulusan;
    private Row rowJp;
    private Matakuliah contextMkForAdd;
    private Row rowBk;
    private Row rowReferensi;

    // Tab relasi
    private Tabpanel manajemenProfil;
    private Tabpanel manajemenBahanKajian;
    private Tabpanel manajemenCpmk;
    private EventListener eventListener;

    private CapaianLulusan capaianLulusan;

    // ── Tab handlers ──────────────────────────────────────────────────────────

    public void onProfil(Event event) {
        if (manajemenProfil.getChildren().size() == 0) {
            CapaianLulusanVsProfilLulusanAction rel = new CapaianLulusanVsProfilLulusanAction();
            rel.setHeight("100%"); rel.setWidth("100%");
            rel.setParent(manajemenProfil);
        }
    }

    public void onBahanKajian(Event event) {
        if (manajemenBahanKajian.getChildren().size() == 0) {
            CapaianLulusanVsBahanKajianAction rel = new CapaianLulusanVsBahanKajianAction();
            rel.setHeight("100%"); rel.setWidth("100%");
            rel.setParent(manajemenBahanKajian);
        }
    }

    public void onCpmk(Event event) {
        if (manajemenCpmk.getChildren().size() == 0) {
            CapaianLulusanVsCapaianPembelajaranLulusanAction rel =
                    new CapaianLulusanVsCapaianPembelajaranLulusanAction();
            rel.setHeight("100%"); rel.setWidth("100%");
            rel.setParent(manajemenCpmk);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "jurusan", "kode", "nama", "kategori", "profil",
                "bahanKajian", "referensi", "khususBuatMk", "keterangan", "aktif"};
        initCommon(comp, CapaianLulusan.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    public void onAdd(Event event) throws Exception {
        initForm(new CapaianLulusan());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((CapaianLulusan) obj);
    }

    public static void onAddExternal(Event event, EventListener listener,
            CapaianLulusan cl, Matakuliah khususMk) throws Exception {
        CapaianLulusanAction action = new CapaianLulusanAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
                .appendChild(action.addWindow);
        action.addWindow.setHeight("98%");
        action.addWindow.setWidth("550px");
        action.contextMkForAdd = khususMk;
        action.initForm(cl);
        action.lockFakultasJurusanIfSelected(action.fakultas, action.jurusan);
        action.addWindow.setVisible(true);
        action.addWindow.onModal();
    }

    private void initForm(final CapaianLulusan cl) {
        this.capaianLulusan = cl;

        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        FormContext ctx = buildFormBorderlayout("Pendataan Capaian Lulusan");
        Rows rows = ctx.rows;

        kode = new Textbox(cl.getKode());
        nama = new Textbox(cl.getNama());
        addKodeNamaFakultasJurusanRows(rows, kode, nama, fakultas, jurusan,
                cl.getJurusan(), "Kode Capaian Lulusan", "Nama Capaian Lulusan");

        khususBuatMk = addKhususMkRow(rows, cl.getKhususBuatMk(), contextMkForAdd);
        keterangan   = addKeteranganRow(rows, cl.getKeterangan());

        // Kategori CPL sesuai SN-Dikti
        cbKategori = new Combobox();
        cbKategori.setReadonly(true);
        cbKategori.setWidth("90%");
        Comboitem emptyCategory = new Comboitem("- Pilih Kategori -");
        emptyCategory.setValue("");
        cbKategori.appendChild(emptyCategory);
        List<KategoriCpl> categories = KategoriCplAction.activeCategories(
                HibernateUtil.currentSession(), perguruanTinggi);
        for (KategoriCpl category : categories) {
            Comboitem ci = new Comboitem(category.getNama());
            ci.setValue(category.getNama());
            cbKategori.appendChild(ci);
        }
        String curKat = cl.getKategori();
        int katIdx = 0;
        boolean currentFound = curKat == null || curKat.trim().isEmpty();
        for (int i = 0; i < cbKategori.getItemCount(); i++) {
            Object value = cbKategori.getItemAtIndex(i).getValue();
            if (curKat.equals(value)) {
                katIdx = i;
                currentFound = true;
                break;
            }
        }
        // Data lama tetap dapat diedit walaupun kategorinya sudah dinonaktifkan/dihapus.
        if (!currentFound) {
            Comboitem legacy = new Comboitem(curKat);
            legacy.setValue(curKat);
            cbKategori.appendChild(legacy);
            katIdx = cbKategori.getItemCount() - 1;
        }
        cbKategori.setSelectedIndex(katIdx);
        addFormRow(rows, "Kategori CPL (SN-Dikti)", cbKategori);

        // Baris Profil Lulusan (span 2)
        rowJp = new MyFormRow();
        ZkCompat.setSpans(rowJp, "2");
        rowJp.setParent(rows);

        // Baris Bahan Kajian (span 2)
        rowBk = new MyFormRow();
        ZkCompat.setSpans(rowBk, "2");
        rowBk.setParent(rows);

        // Baris Referensi (span 2)
        rowReferensi = new MyFormRow();
        ZkCompat.setSpans(rowReferensi, "2");
        rowReferensi.setParent(rows);

        selectedProfilLulusan    = new HashMap<Long, ProfilLulusan>();
        selectedBahanKajian      = new HashMap<Long, BahanKajian>();
        selectedReferensiLulusan = new HashMap<Long, ReferensiLulusan>();

        // Reload keduanya; onChange jurusan juga me-reload
        loadCheckboxes();
        jurusan.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                loadCheckboxes();
            }
        });

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                    if (eventListener != null) {
                        eventListener.onEvent(new Event("", addWindow,
                                CapaianLulusanAction.this.capaianLulusan));
                    }
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    private void loadCheckboxes() {
        Jurusan j = isUnselected(jurusan) ? null : (Jurusan) jurusan.getSelectedItem().getValue();

        // --- Profil Lulusan ---
        List<ProfilLulusan> profils = ConstantValues.simpleList(
                HibernateUtil.currentSession().createCriteria(ProfilLulusan.class)
                        .add(j == null ? Restrictions.eq("perguruanTinggi", perguruanTinggi)
                                : Restrictions.sqlRestriction("true"))
                        .add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", j)))
                        .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                ProfilLulusan.class);

        selectedProfilLulusan.clear();
        for (String d : capaianLulusan.getProfil().split(",")) {
            try {
                if (!d.trim().isEmpty()) {
                    Long id = Long.parseLong(d.trim());
                    ProfilLulusan p = (ProfilLulusan) ConstantValues.ambil(ProfilLulusan.class.getName(), id);
                    if (p != null && !selectedProfilLulusan.containsKey(id)) selectedProfilLulusan.put(id, p);
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianLulusanAction.java:242"); }
        }

        // Deduplikasi by kode untuk profil
        List<ProfilLulusan> profilsUnique = new ArrayList<ProfilLulusan>();
        List<String> seenKode = new ArrayList<String>();
        for (ProfilLulusan p : profils) {
            if (!seenKode.contains(p.getKode())) {
                seenKode.add(p.getKode());
                profilsUnique.add(p);
            }
        }
        renderCheckboxGrid(rowJp, profilsUnique, selectedProfilLulusan,
                "Pilih Profil Lulusan", new CheckboxLabelFn<ProfilLulusan>() {
                    @Override public String label(ProfilLulusan p) {
                        return p.getKode() + " " + p.getNama();
                    }
                });

        // --- Bahan Kajian ---
        List<BahanKajian> bahanKajians = ConstantValues.simpleList(
                HibernateUtil.currentSession().createCriteria(BahanKajian.class)
                        .add(j == null ? Restrictions.eq("perguruanTinggi", perguruanTinggi)
                                : Restrictions.sqlRestriction("true"))
                        .add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", j)))
                        .addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                BahanKajian.class);

        selectedBahanKajian.clear();
        for (String d : capaianLulusan.getBahanKajian().split(",")) {
            try {
                if (!d.trim().isEmpty()) {
                    Long id = Long.parseLong(d.trim());
                    BahanKajian bk = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(), id);
                    if (bk != null && !selectedBahanKajian.containsKey(id)) selectedBahanKajian.put(id, bk);
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianLulusanAction.java:279"); }
        }

        List<BahanKajian> bkUnique = new ArrayList<BahanKajian>();
        List<String> seenBkKode = new ArrayList<String>();
        for (BahanKajian bk : bahanKajians) {
            if (!seenBkKode.contains(bk.getKode())) {
                seenBkKode.add(bk.getKode());
                bkUnique.add(bk);
            }
        }
        renderCheckboxGrid(rowBk, bkUnique, selectedBahanKajian,
                "Pilih Bahan Kajian", new CheckboxLabelFn<BahanKajian>() {
                    @Override public String label(BahanKajian bk) {
                        return bk.getKode() + " " + bk.getNama();
                    }
                });

        // --- Referensi CPL (SN-Dikti/KKNI/Peta Okupasi/SKKNI) ---
        List<ReferensiLulusan> refs = ConstantValues.simpleList(
                HibernateUtil.currentSession().createCriteria(ReferensiLulusan.class)
                        .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                        .addOrder(Order.asc("nama"))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                ReferensiLulusan.class);

        selectedReferensiLulusan.clear();
        for (String d : capaianLulusan.getReferensi().split(",")) {
            try {
                if (!d.trim().isEmpty()) {
                    Long id = Long.parseLong(d.trim());
                    ReferensiLulusan ref = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(), id);
                    if (ref != null && !selectedReferensiLulusan.containsKey(id))
                        selectedReferensiLulusan.put(id, ref);
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianLulusanAction.java:314"); }
        }
        renderCheckboxGrid(rowReferensi, refs, selectedReferensiLulusan,
                "Referensi CPL (SN-Dikti/KKNI/Peta Okupasi)", new CheckboxLabelFn<ReferensiLulusan>() {
                    @Override public String label(ReferensiLulusan r) { return r.getNama(); }
                });
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Capaian Lulusan")) return false;
        if (!validateJurusanRequired(jurusan)) return false;

        Session session = HibernateUtil.currentSession();
        if (capaianLulusan.getId() != null) {
            capaianLulusan = (CapaianLulusan) session.load(CapaianLulusan.class, capaianLulusan.getId());
        }
        capaianLulusan.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
        capaianLulusan.setKode(kode.getValue());
        capaianLulusan.setNama(nama.getValue());
        capaianLulusan.setKeterangan(keterangan.getValue());
        capaianLulusan.setPerguruanTinggi(perguruanTinggi);
        capaianLulusan.setKhususBuatMk((Matakuliah) khususBuatMk.getAttribute("matakuliah"));
        capaianLulusan.setProfil(mapKeysToString(selectedProfilLulusan));
        capaianLulusan.setBahanKajian(mapKeysToString(selectedBahanKajian));
        capaianLulusan.setReferensi(mapKeysToString(selectedReferensiLulusan));
        if (cbKategori != null && cbKategori.getSelectedItem() != null) {
            Object v = cbKategori.getSelectedItem().getValue();
            capaianLulusan.setKategori(v == null ? "" : v.toString());
        }

        Common.refreshSaveOrUpdate(session, capaianLulusan);
        return true;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    @Override
    public Criteria initCriteria(boolean order) {
        return buildBaseCriteria(HibernateUtil.currentSession(), CapaianLulusan.class,
                order, true, "kode", "nama");
    }

    @Override
    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        executeSearch(initCriteria(false), initCriteria(true), new CapaianLulusanRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    class CapaianLulusanRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final CapaianLulusan cl = (CapaianLulusan) obj;

            new Label(cl.getKode()).setParent(row);
            namaCellRingkas(CapaianLulusan.class, cl, cl.getNama()).setParent(row);
            new Label(cl.getJurusan() == null ? "" : cl.getJurusan().getNama()).setParent(row);
            new Label(cl.getKategori()).setParent(row);
            ringkasanKeterangan(BahanKajianAction.resolveNamaFromCsv(cl.getProfil(), ProfilLulusan.class)).setParent(row);
            ringkasanKeterangan(BahanKajianAction.resolveNamaFromCsv(cl.getBahanKajian(), BahanKajian.class)).setParent(row);
            ringkasanKeterangan(BahanKajianAction.resolveNamaFromCsv(cl.getReferensi(), ReferensiLulusan.class)).setParent(row);
            new Label(cl.getKhususBuatMk() == null ? "" : cl.getKhususBuatMk().getNama()).setParent(row);
            ringkasanKeterangan(cl.getKeterangan()).setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(cl.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    cl.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(cl);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, cl, CapaianLulusanAction.this).setParent(row);
        }
    }
}
