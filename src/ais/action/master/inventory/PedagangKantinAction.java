package ais.action.master.inventory;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Data Pedagang/Mitra Kantin (tampilan master mandiri) — konversi dari {@code modul/kantin/pedagang}.
 *
 * <p>Seorang pedagang adalah akun pengguna ({@link Tbmuser}) yang ditugaskan mengelola sebuah
 * {@link Toko}. Halaman ini menautkan akun pengguna ke toko dan mengatur status aktifnya.
 * (Penugasan juga bisa dilakukan dari detail Toko; halaman ini menyediakan daftar menyeluruh.)</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat mitra tokonya sendiri.</p>
 */
public class PedagangKantinAction extends GenericCrudAction<Pedagang> {

    private static final long serialVersionUID = 1L;

    private Combobox toko;
    private Bandbox bdUser;
    private org.zkoss.zul.Textbox keterangan;
    private Tbmuser userDipilih;

    private Toko scopeTokoCache;
    private boolean scopeResolved = false;

    private Toko scopeToko() {
        if (!scopeResolved) {
            scopeResolved = true;
            try {
                Toko ct = Common.getCurrentToko();
                if (ct != null && ct.getId() != null) {
                    Boolean b = ct.getBolehMelihatTokolain();
                    if (b == null || !b.booleanValue()) {
                        scopeTokoCache = ct;
                    }
                }
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/PedagangKantinAction.java:75");
            }
        }
        return scopeTokoCache;
    }

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Pedagang> getEntityClass() {
        return Pedagang.class;
    }

    @Override
    protected Pedagang createNewEntity() {
        return new Pedagang();
    }

    @Override
    protected String getWindowTitle() {
        return "Data Pedagang";
    }

    @Override
    protected String getIntroTitle() {
        return "Pedagang / Mitra Kantin";
    }

    @Override
    protected String getIntroDescription() {
        return "Daftar pemilik/penjaga toko di kantin beserta akun login dan tokonya. "
                + "Tambahkan dengan memilih akun pengguna lalu menautkannya ke sebuah toko.";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Pedagang.class)
                .add(searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.desc("id"));
        }
        String kw = searchnama == null ? "" : searchnama.getValue().trim();
        if (!kw.isEmpty()) {
            criteria.createAlias("tbmuser", "u");
            criteria.add(Restrictions.or(
                    Restrictions.ilike("u.userId", kw, MatchMode.ANYWHERE),
                    Restrictions.ilike("u.userNama", kw, MatchMode.ANYWHERE)));
        }
        Toko st = scopeToko();
        if (st != null) {
            criteria.add(Restrictions.eq("toko", st));
        }
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new PedagangRenderer();
    }

    // ======================== Form UI ========================

    @Override
    protected void buildFormContent(MyWindow window, final Pedagang p) throws Exception {
        Toko st = scopeToko();
        userDipilih = p.getTbmuser();

        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        ZkCompat.setFlex(center, true);
        center.setParent(borderlayout);

        Div cardWrap = new Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        Grid formGrid = new Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);
        Rows rows = new Rows();
        rows.setParent(formGrid);
        FormBuilder fb = new FormBuilder(rows);

        fb.addRow("Akun Pengguna *", buildUserBandbox(p), "Pilih akun pengguna yang menjadi pedagang");

        toko = new Combobox();
        toko.setWidth("100%");
        toko.setReadonly(true);
        Common.insertComboDanSemua(toko, "nama", Toko.class, Restrictions.eq("aktif", true));
        if (st != null) {
            Common.selectComboItem(true, toko, st);
            toko.setDisabled(true);
        } else {
            Common.selectComboItem(toko, p.getToko());
        }
        fb.addRow("Toko *", toko, "Toko yang dikelola pedagang ini");

        keterangan = new org.zkoss.zul.Textbox(p.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan, "Catatan tambahan bila diperlukan");

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);
        Toolbar toolbar = new Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup tanpa menyimpan");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan data pedagang");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        save.setParent(toolbar);

        borderlayout.setParent(window);
    }

    private Bandbox buildUserBandbox(Pedagang p) {
        bdUser = new Bandbox();
        bdUser.setReadonly(true);
        bdUser.setWidth("100%");
        if (p.getTbmuser() != null) {
            bdUser.setValue(labelUser(p.getTbmuser()));
        }

        Bandpopup bp = new Bandpopup();
        bp.setParent(bdUser);
        bp.setWidth("340px");

        Vlayout v = new Vlayout();
        v.setStyle("padding:6px;");
        v.setParent(bp);

        Hlayout h = new Hlayout();
        h.setStyle("gap:6px;");
        h.setParent(v);
        final Textbox txtCari = new Textbox();
        txtCari.setWidth("230px");
        txtCari.setTooltiptext("user id / nama pengguna");
        h.appendChild(txtCari);
        Button bCari = new Button("Cari");
        h.appendChild(bCari);

        final Listbox lst = new Listbox();
        lst.setWidth("320px");
        lst.setHeight("220px");
        lst.setStyle("margin-top:6px;");
        lst.setParent(v);
        org.zkoss.zul.Listhead lh = new org.zkoss.zul.Listhead();
        lh.setParent(lst);
        new org.zkoss.zul.Listheader("User ID").setParent(lh);
        new org.zkoss.zul.Listheader("Nama").setParent(lh);

        EventListener doSearch = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                cariUser(txtCari.getValue(), lst);
            }
        };
        bCari.addEventListener("onClick", doSearch);
        txtCari.addEventListener("onOK", doSearch);

        lst.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Listitem it = lst.getSelectedItem();
                if (it != null && it.getValue() != null) {
                    userDipilih = (Tbmuser) it.getValue();
                    bdUser.setValue(labelUser(userDipilih));
                    bdUser.close();
                }
            }
        });

        return bdUser;
    }

    @SuppressWarnings("unchecked")
    private void cariUser(String kw, Listbox lst) {
        lst.getItems().clear();
        String k = kw == null ? "" : kw.trim();
        if (k.isEmpty()) {
            return;
        }
        java.util.List<Tbmuser> users = HibernateUtil.currentSession().createCriteria(Tbmuser.class)
                .add(Restrictions.or(
                        Restrictions.ilike("userId", k, MatchMode.ANYWHERE),
                        Restrictions.ilike("userNama", k, MatchMode.ANYWHERE)))
                .addOrder(Order.asc("userNama")).setMaxResults(25).list();
        for (Tbmuser u : users) {
            Listitem it = new Listitem();
            it.setValue(u);
            new Listcell(u.getUserId()).setParent(it);
            new Listcell(u.getUserNama()).setParent(it);
            it.setParent(lst);
        }
    }

    private static String labelUser(Tbmuser u) {
        if (u == null) {
            return "";
        }
        String id = u.getUserId() == null ? "" : u.getUserId();
        String nm = u.getUserNama() == null ? "" : u.getUserNama();
        return nm.isEmpty() ? id : (id + " — " + nm);
    }

    // ======================== Save logic ========================

    public boolean onSave(Event event) throws Exception {
        if (userDipilih == null) {
            MyMessageboxConfig.show("Mohon maaf, Akun Pengguna belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Akun Pengguna pada kolom yang tersedia; (2) pilih akun pengguna yang sesuai dengan pedagang; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Toko st = scopeToko();
        Toko tk = st != null ? st
                : (Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue());
        if (tk == null) {
            MyMessageboxConfig.show("Mohon maaf, Toko belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Toko pada kolom yang tersedia; (2) pilih salah satu Toko yang sesuai; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        Pedagang p = currentEntity;
        if (p.getId() != null) {
            p = (Pedagang) session.load(Pedagang.class, p.getId());
            currentEntity = p;
        }
        p.setTbmuser(userDipilih);
        p.setToko(tk);
        p.setKeterangan(keterangan.getValue());
        if (p.getId() == null) {
            p.setAktif(true);
        }
        Common.refreshSaveOrUpdate(session, p);
        return true;
    }

    // ======================== Renderer ========================

    class PedagangRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Pedagang p = (Pedagang) arg1;

            String lbl = p.getTbmuser() == null ? "(tanpa akun)" : labelUser(p.getTbmuser());
            RevisiHelper.createNewRevisi(Pedagang.class, p, lbl).setParent(arg0);
            new Label(p.getToko() == null ? "" : p.getToko().getNama()).setParent(arg0);
            new Label(p.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(p.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event ev) throws Exception {
                    p.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(p);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, p, PedagangKantinAction.this).setParent(arg0);
        }
    }
}
