package ais.action.master.obe;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.ReferensiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public class BahanKajianAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox  kode;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox fakultas;
    private Combobox jurusan;
    private AmbilDataMatakuliahBanbox khususBuatMk;

    // Multi-select referensi
    private HashMap<Long, ReferensiLulusan> selectedReferensiLulusan;
    private Row rowJp;

    // Tab relasi
    private Tabpanel manajemenMatakuliah;
    private EventListener eventListener;

    private BahanKajian bahanKajian;
    private Matakuliah contextMkForAdd;

    // ── Tab handlers ──────────────────────────────────────────────────────────

    public void onMatakuliah(Event event) {
        if (manajemenMatakuliah.getChildren().size() == 0) {
            BahanKajianVsCapaianLulusanVsMatakuliahAction laporan =
                    new BahanKajianVsCapaianLulusanVsMatakuliahAction();
            laporan.setHeight("100%");
            laporan.setWidth("100%");
            laporan.setParent(manajemenMatakuliah);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "jurusan", "kode", "nama", "referensi", "khususBuatMk", "keterangan", "aktif"};
        initCommon(comp, BahanKajian.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    public void onAdd(Event event) throws Exception {
        initForm(new BahanKajian());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((BahanKajian) obj);
    }

    /** Entry point untuk membuka form dari modul lain (popup external). */
    public static void onAddExternal(Event event, EventListener listener,
            BahanKajian bk, Matakuliah khususMk) throws Exception {
        BahanKajianAction action = new BahanKajianAction();
        action.eventListener = listener;
        action.addWindow = new MyWindow();
        ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
                .appendChild(action.addWindow);
        action.addWindow.setHeight("98%");
        action.addWindow.setWidth("550px");
        action.contextMkForAdd = khususMk;
        action.initForm(bk);
        action.lockFakultasJurusanIfSelected(action.fakultas, action.jurusan);
        action.addWindow.setVisible(true);
        action.addWindow.onModal();
    }

    private void initForm(final BahanKajian bk) {
        this.bahanKajian = bk;

        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        FormContext ctx = buildFormBorderlayout("Pendataan Bahan Kajian");
        Rows rows = ctx.rows;

        kode = new Textbox(bk.getKode());
        nama = new Textbox(bk.getNama());
        addKodeNamaFakultasJurusanRows(rows, kode, nama, fakultas, jurusan,
                bk.getJurusan(), "Kode Bahan Kajian", "Nama Bahan Kajian");

        khususBuatMk = addKhususMkRow(rows, bk.getKhususBuatMk(), contextMkForAdd);
        keterangan   = addKeteranganRow(rows, bk.getKeterangan());

        // Baris multi-select referensi (span 2 kolom)
        rowJp = new MyFormRow();
        ZkCompat.setSpans(rowJp, "2");
        rowJp.setParent(rows);

        selectedReferensiLulusan = new HashMap<Long, ReferensiLulusan>();
        loadReferensiCheckboxes();

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                    if (eventListener != null) {
                        eventListener.onEvent(new Event("", addWindow, BahanKajianAction.this.bahanKajian));
                    }
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    /** Memuat daftar ReferensiLulusan dan merender checkbox-nya. */
    private void loadReferensiCheckboxes() {
        List<ReferensiLulusan> refs = ConstantValues.simpleList(
                HibernateUtil.currentSession().createCriteria(ReferensiLulusan.class)
                        .add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
                        .addOrder(Order.asc("nama"))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                ReferensiLulusan.class);

        selectedReferensiLulusan.clear();
        for (String d : bahanKajian.getReferensi().split(",")) {
            try {
                if (!d.trim().isEmpty()) {
                    Long id = Long.parseLong(d.trim());
                    ReferensiLulusan ref = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(), id);
                    if (ref != null && !selectedReferensiLulusan.containsKey(id)) {
                        selectedReferensiLulusan.put(id, ref);
                    }
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/BahanKajianAction.java:174"); }
        }

        renderCheckboxGrid(rowJp, refs, selectedReferensiLulusan,
                "Pilih Referensi", new CheckboxLabelFn<ReferensiLulusan>() {
                    @Override public String label(ReferensiLulusan r) { return r.getNama(); }
                });
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Bahan Kajian")) return false;
        if (!validateJurusanRequired(jurusan)) return false;

        Session session = HibernateUtil.currentSession();
        if (bahanKajian.getId() != null) {
            bahanKajian = (BahanKajian) session.load(BahanKajian.class, bahanKajian.getId());
        }
        bahanKajian.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
        bahanKajian.setKode(kode.getValue());
        bahanKajian.setNama(nama.getValue());
        bahanKajian.setKeterangan(keterangan.getValue());
        bahanKajian.setPerguruanTinggi(perguruanTinggi);
        bahanKajian.setKhususBuatMk((Matakuliah) khususBuatMk.getAttribute("matakuliah"));
        bahanKajian.setReferensi(mapKeysToString(selectedReferensiLulusan));

        Common.refreshSaveOrUpdate(session, bahanKajian);
        return true;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    @Override
    public Criteria initCriteria(boolean order) {
        return buildBaseCriteria(HibernateUtil.currentSession(), BahanKajian.class,
                order, true, "kode", "nama");
    }

    @Override
    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        executeSearch(initCriteria(false), initCriteria(true), new BahanKajianRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    class BahanKajianRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final BahanKajian bk = (BahanKajian) obj;

            new Label(bk.getKode()).setParent(row);
            namaCellRingkas(BahanKajian.class, bk, bk.getNama()).setParent(row);
            new Label(bk.getJurusan() == null ? "" : bk.getJurusan().getNama()).setParent(row);

            // Kolom referensi: tampilkan nama-nama yang dipilih
            ringkasanKeterangan(resolveNamaFromCsv(bk.getReferensi(), ReferensiLulusan.class)).setParent(row);
            new Label(bk.getKhususBuatMk() == null ? "" : bk.getKhususBuatMk().getNama()).setParent(row);
            ringkasanKeterangan(bk.getKeterangan()).setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(bk.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    bk.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(bk);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, bk, BahanKajianAction.this).setParent(row);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Mengonversi CSV ID entity menjadi String nama yang digabung koma. */
    static String resolveNamaFromCsv(String csv, Class<?> entityClass) {
        StringBuilder sb = new StringBuilder();
        for (String d : csv.split(",")) {
            try {
                if (!d.trim().isEmpty()) {
                    String token = d.trim();
                    // Formula OBE lama dapat menyimpan key berbentuk "urutan_id"
                    // (contoh 8_23971), sedangkan versi baru menyimpan ID saja.
                    int pemisah = token.lastIndexOf('_');
                    if (pemisah >= 0 && pemisah < token.length() - 1) {
                        String kandidatId = token.substring(pemisah + 1).trim();
                        if (Common.isNumber(kandidatId)) token = kandidatId;
                    }
                    if (!Common.isNumber(token)) continue;
                    Long id = Long.parseLong(token);
                    Object entity = ConstantValues.ambil(entityClass.getName(), id);
                    if (entity != null) {
                        try {
                            String nama = (String) entity.getClass().getMethod("getNama").invoke(entity);
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(nama);
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/BahanKajianAction.java:268"); }
                    }
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/obe/BahanKajianAction.java:271"); }
        }
        return sb.toString();
    }
}
