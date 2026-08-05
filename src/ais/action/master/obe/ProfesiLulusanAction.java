package ais.action.master.obe;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.obe.ProfesiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyRowRenderer;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

@SuppressWarnings({"deprecation", "unchecked"})
public class ProfesiLulusanAction extends ObeBaseAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox  kode;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox fakultas;
    private Combobox jurusan;

    private ProfesiLulusan profesiLulusan;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        String[] contents = {"id", "kode", "nama", "jurusan", "keterangan", "aktif"};
        initCommon(comp, ProfesiLulusan.class, contents);
    }

    // ── Tambah / edit ─────────────────────────────────────────────────────────

    public void onAdd(Event event) throws Exception {
        initForm(new ProfesiLulusan());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        initForm((ProfesiLulusan) obj);
    }

    private void initForm(ProfesiLulusan pl) {
        this.profesiLulusan = pl;

        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        FormContext ctx = buildFormBorderlayout("Pendataan Profesi Lulusan");
        Rows rows = ctx.rows;

        kode = new Textbox(pl.getKode());
        nama = new Textbox(pl.getNama());
        addKodeNamaFakultasJurusanRows(rows, kode, nama, fakultas, jurusan,
                pl.getJurusan(), "Kode Profesi Lulusan", "Nama Profesi Lulusan");

        keterangan = addKeteranganRow(rows, pl.getKeterangan());

        buildSouthToolbar(ctx.borderlayout, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        attachAndShow(ctx.borderlayout);
    }

    // ── Simpan ────────────────────────────────────────────────────────────────

    public boolean onSave(Event event) throws Exception {
        if (!validateNamaRequired(nama, "Profesi Lulusan")) return false;
        if (!validateJurusanRequired(jurusan)) return false;

        Session session = HibernateUtil.currentSession();
        if (profesiLulusan.getId() != null) {
            profesiLulusan = (ProfesiLulusan) session.load(ProfesiLulusan.class, profesiLulusan.getId());
        }
        profesiLulusan.setJurusan((Jurusan) jurusan.getSelectedItem().getValue());
        profesiLulusan.setKode(kode.getValue());
        profesiLulusan.setNama(nama.getValue());
        profesiLulusan.setKeterangan(keterangan.getValue());
        profesiLulusan.setPerguruanTinggi(perguruanTinggi);

        Common.refreshSaveOrUpdate(session, profesiLulusan);
        return true;
    }

    // ── Pencarian & grid ─────────────────────────────────────────────────────

    @Override
    public Criteria initCriteria(boolean order) {
        // ProfesiLulusan tidak createAlias jurusan (nullable), filter berbeda
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(ProfesiLulusan.class);
        c.add(org.hibernate.criterion.Restrictions.eq("perguruanTinggi", perguruanTinggi));
        c.add(searchaktif == null || searchaktif.isChecked()
                ? org.hibernate.criterion.Restrictions.or(
                        org.hibernate.criterion.Restrictions.isNull("aktif"),
                        org.hibernate.criterion.Restrictions.eq("aktif", true))
                : org.hibernate.criterion.Restrictions.sqlRestriction("true"));
        if (order) {
            c.addOrder(org.hibernate.criterion.Order.asc("kode"));
            c.addOrder(org.hibernate.criterion.Order.asc("nama"));
        }
        if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
            c.add(org.hibernate.criterion.Restrictions.ilike("nama",
                    searchnama.getValue().trim(),
                    org.hibernate.criterion.MatchMode.ANYWHERE));
        }
        return c;
    }

    @Override
    public void onSearchDefault(Event event) {
        executeSearch(initCriteria(false), initCriteria(true), new ProfesiLulusanRenderer());
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    class ProfesiLulusanRenderer extends MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ProfesiLulusan pl = (ProfesiLulusan) obj;

            new Label(pl.getKode()).setParent(row);
            namaCellRingkas(ProfesiLulusan.class, pl, pl.getNama()).setParent(row);
            new Label(pl.getJurusan() == null ? "" : pl.getJurusan().getNama()).setParent(row);
            ringkasanKeterangan(pl.getKeterangan()).setParent(row);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(pl.getAktif());
            checkbox.setParent(row);
            row.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    pl.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(pl);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, pl, ProfesiLulusanAction.this).setParent(row);
        }
    }
}
