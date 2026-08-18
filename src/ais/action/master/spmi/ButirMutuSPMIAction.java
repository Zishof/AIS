package ais.action.master.spmi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

public class ButirMutuSPMIAction extends BaseSPMIAction {

    private static final long serialVersionUID = -5779730267402400328L;

    // ---- Search fields ----
    private Combobox searchjenisSPMI;
    private Combobox searchstandarSPMI;

    // ---- Form fields ----
    private MyIntbox nomorUrut;
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox jenisSPMI;
    private Combobox standarSPMI;

    // ---- Current entity ----
    private ButirMutuSPMI butirMutuSPMI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        Common.insertComboDanSemua(searchjenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        Common.insertComboDanSemua(searchstandarSPMI, "nama", "jenisSPMI",
                StandarSPMI.class, Restrictions.eq("aktif", true));

        searchjenisSPMI.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                JenisSPMI sel = selectedValue(searchjenisSPMI);
                Common.insertComboDanSemua(searchstandarSPMI, "nama", "jenisSPMI", StandarSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("jenisSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                onSearchDefault(null);
            }
        });

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(ButirMutuSPMI.class,
                new String[]{"id", "nomorUrut", "standarSPMI", "nama", "keterangan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class ButirMutuSPMIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ButirMutuSPMI item = (ButirMutuSPMI) obj;

            new Label(item.getNomorUrut() + "").setParent(row);
            new Label(item.getStandarSPMI().getJenisSPMI().getNama()).setParent(row);
            new Label(item.getStandarSPMI().getNama()).setParent(row);
            RevisiHelper.createNewRevisi(ButirMutuSPMI.class, item, item.getNama()).setParent(row);
            new Label(item.getKeterangan()).setParent(row);

            final MyCheckboxConfig aktifCb = new MyCheckboxConfig("Aktif");
            aktifCb.setDisabled(!edit);
            aktifCb.setChecked(item.getAktif());
            aktifCb.setParent(row);
            row.setAttribute("checkbox", aktifCb);
            aktifCb.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    item.setAktif(aktifCb.isChecked());
                    Common.refreshSaveOrUpdate(item);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, item, ButirMutuSPMIAction.this).setParent(row);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new ButirMutuSPMI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        butirMutuSPMI = (ButirMutuSPMI) obj;
        buildForm(butirMutuSPMI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final ButirMutuSPMI item) throws Exception {
        FormHolder fh = prepareFormWindow("Pendataan Pernyataan Ayat Standar/Butir Mutu SPMI");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "No Urut");
        row.appendChild(nomorUrut = new MyIntbox(item.getNomorUrut()));

        row = addFormRow(rows, "Pernyataan Ayat Standar/Butir Mutu *");
        row.appendChild(nama = new Textbox(item.getNama()));
        nama.setWidth("90%");
        nama.setRows(5);

        row = addFormRow(rows, "Jenis SPMI *");
        row.appendChild(jenisSPMI = new Combobox());
        jenisSPMI.setWidth("90%");
        Common.insertCombo(jenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(true, jenisSPMI,
                item.getStandarSPMI() == null
                        ? selectedValue(searchjenisSPMI)
                        : item.getStandarSPMI().getJenisSPMI());
        jenisSPMI.setReadonly(true);

        row = addFormRow(rows, "Standar SPMI/Referensi Eksternal *");
        row.appendChild(standarSPMI = new Combobox());
        standarSPMI.setWidth("90%");
        Common.insertCombo(standarSPMI, "nama", "jenisSPMI",
                StandarSPMI.class, Restrictions.eq("aktif", true));
        Common.selectComboItem(standarSPMI,
                item.getStandarSPMI() == null
                        ? selectedValue(searchstandarSPMI)
                        : item.getStandarSPMI());
        standarSPMI.setReadonly(true);

        // Cascade: when Jenis changes, reload Standar list
        EventListener jenisCascade = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                JenisSPMI sel = selectedValue(jenisSPMI);
                Common.insertCombo(standarSPMI, "nama", "jenisSPMI", StandarSPMI.class,
                        sel == null ? Restrictions.eq("aktif", true)
                                    : Restrictions.and(Restrictions.eq("jenisSPMI", sel),
                                                       Restrictions.eq("aktif", true)));
                Common.selectComboItem(true, standarSPMI,
                        item.getStandarSPMI() == null
                                ? selectedValue(searchstandarSPMI)
                                : item.getStandarSPMI());
            }
        };
        jenisCascade.onEvent(null);
        jenisSPMI.addEventListener("onChange", jenisCascade);

        row = addFormRow(rows, "Keterangan");
        row.appendChild(keterangan = new Textbox(item.getKeterangan()));
        keterangan.setWidth("90%");
        keterangan.setRows(5);

        finaliseFormWindow(fh, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
    }

    // =====================================================================
    // Save
    // =====================================================================

    public boolean onSave(Event event) throws Exception {
        if (nomorUrut.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, nomor urut Butir Mutu SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Nomor Urut pada form dengan nilai angka; "
                    + "(2) pastikan kolom tidak kosong sebelum menyimpan; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Pernyataan Ayat Standar/Butir Mutu belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Pernyataan pada form dengan teks yang sesuai; "
                    + "(2) pastikan teks tidak kosong atau hanya berisi spasi; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisSPMI.getSelectedItem() == null || jenisSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Jenis SPMI dari daftar pilihan; "
                    + "(2) pastikan daftar sudah memuat data Jenis SPMI yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (standarSPMI.getSelectedItem() == null || standarSPMI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Standar SPMI/Referensi Eksternal belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Jenis SPMI terlebih dahulu agar daftar Standar termuat; "
                    + "(2) pilih Standar SPMI dari daftar pilihan yang tersedia; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (butirMutuSPMI.getId() != null) {
            butirMutuSPMI = (ButirMutuSPMI) session.load(ButirMutuSPMI.class, butirMutuSPMI.getId());
        }
        butirMutuSPMI.setNomorUrut(nomorUrut.getValue());
        butirMutuSPMI.setNama(nama.getValue());
        butirMutuSPMI.setStandarSPMI((StandarSPMI) standarSPMI.getSelectedItem().getValue());
        butirMutuSPMI.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, butirMutuSPMI);
        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(ButirMutuSPMI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchstandarSPMI.getSelectedItem() == null || searchstandarSPMI.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("standarSPMI", searchstandarSPMI.getSelectedItem().getValue()));

        if (searchjenisSPMI.getSelectedItem() != null && searchjenisSPMI.getSelectedItem().getValue() != null) {
            criteria.createAlias("standarSPMI", "standarSPMI")
                    .add(Restrictions.eq("standarSPMI.jenisSPMI", searchjenisSPMI.getSelectedItem().getValue()));
        }

        if (order) criteria.addOrder(Order.asc("nomorUrut"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<ButirMutuSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new ButirMutuSPMIRenderer());
    }

}
