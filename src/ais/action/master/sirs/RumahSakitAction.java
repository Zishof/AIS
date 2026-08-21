package ais.action.master.sirs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.util.RumahSakitUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.RumahSakit;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/** CRUD profil fasilitas kesehatan dan tema website publik/eMedic. */
public class RumahSakitAction extends GenericCrudAction<RumahSakit> {
    private static final long serialVersionUID = 1L;

    private Textbox kode, nama, namaSingkat, domain, alamat, telepon, whatsapp, email,
            website, motto, deskripsi, nomorIzin, css, warna;
    private Combobox jenisFasilitas, pilihanTampilan;
    private Checkbox aktif;

    @Override protected Class<RumahSakit> getEntityClass() { return RumahSakit.class; }
    @Override protected RumahSakit createNewEntity() { return new RumahSakit(); }
    @Override protected String getWindowTitle() { return "Pendataan Fasilitas Kesehatan"; }
    @Override protected String getIntroTitle() { return "Profil Rumah Sakit / Fasilitas Kesehatan"; }
    @Override protected String getIntroDescription() {
        return "Kelola identitas, domain, profil, dan tema website Rumah Sakit, Puskesmas, Posyandu, Klinik, serta fasilitas kesehatan lainnya.";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Criteria criteria = HibernateUtil.currentSession().createCriteria(RumahSakit.class);
        if (order) criteria.addOrder(Order.asc("nama"));
        if (searchnama != null && !searchnama.getValue().trim().isEmpty())
            criteria.add(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        if (searchkode != null && !searchkode.getValue().trim().isEmpty())
            criteria.add(Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        if (searchaktif != null && searchaktif.isChecked()) criteria.add(Restrictions.eq("aktif", Boolean.TRUE));
        return criteria;
    }

    @Override protected MyRowRenderer createRenderer() { return new RumahSakitRenderer(); }

    @Override
    public void onSearchDefault(Event event) {
        RumahSakitUtil.clearCache();
        super.onSearchDefault(event);
    }

    @Override
    protected void buildFormContent(MyWindow window, final RumahSakit entity) throws Exception {
        org.zkoss.zul.Borderlayout layout = new ais.ui.util.MyBorderlayout();
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(layout); ZkCompat.setFlex(center, true);
        org.zkoss.zul.Div card = new org.zkoss.zul.Div(); card.setStyle(FormBuilder.STYLE_CARD_WRAP); card.setParent(center);
        org.zkoss.zul.Grid form = new org.zkoss.zul.Grid(); form.setStyle("border:none;width:100%;"); form.setParent(card);
        Rows rows = new Rows(); rows.setParent(form); FormBuilder fb = new FormBuilder(rows);

        kode = textbox(entity.getKode(), 1); fb.addRow("Kode", kode);
        nama = textbox(entity.getNama(), 1); fb.addRow("Nama Fasilitas *", nama);
        namaSingkat = textbox(entity.getNamaSingkat(), 1); fb.addRow("Nama Singkat", namaSingkat);
        jenisFasilitas = combo(new String[][] {
            {RumahSakit.JENIS_RUMAH_SAKIT, "Rumah Sakit"}, {RumahSakit.JENIS_PUSKESMAS, "Puskesmas"},
            {RumahSakit.JENIS_POSYANDU, "Posyandu"}, {RumahSakit.JENIS_KLINIK, "Klinik"},
            {RumahSakit.JENIS_PRAKTIK_MANDIRI, "Praktik Mandiri"}, {RumahSakit.JENIS_LABORATORIUM, "Laboratorium Kesehatan"},
            {RumahSakit.JENIS_APOTEK, "Apotek"}, {RumahSakit.JENIS_LAINNYA, "Fasilitas Kesehatan Lainnya"}
        }, entity.getJenisFasilitas()); fb.addRow("Jenis Fasilitas *", jenisFasilitas);
        domain = textbox(entity.getDomain(), 1); fb.addRow("Domain *", domain);
        alamat = textbox(entity.getAlamat(), 3); fb.addRow("Alamat", alamat);
        telepon = textbox(entity.getTelepon(), 1); fb.addRow("Telepon", telepon);
        whatsapp = textbox(entity.getWhatsapp(), 1); fb.addRow("WhatsApp", whatsapp);
        email = textbox(entity.getEmail(), 1); fb.addRow("Email", email);
        website = textbox(entity.getWebsite(), 1); fb.addRow("Website", website);
        motto = textbox(entity.getMotto(), 2); fb.addRow("Motto", motto);
        deskripsi = textbox(entity.getDeskripsi(), 4); fb.addRow("Deskripsi", deskripsi);
        nomorIzin = textbox(entity.getNomorIzinOperasional(), 1); fb.addRow("Nomor Izin Operasional", nomorIzin);
        css = textbox(entity.getCss(), 1); fb.addRow("File Tema CSS", css);
        warna = textbox(entity.getWarna(), 1); fb.addRow("Warna Utama (#RRGGBB)", warna);
        pilihanTampilan = combo(new String[][] {{RumahSakit.TAMPILAN_BARU, "Website Baru"},
                {RumahSakit.TAMPILAN_KLASIK, "Website Klasik"}, {RumahSakit.TAMPILAN_DEFAULT, "Default (Website Baru)"}},
                entity.getPiilhanTampilan()); fb.addRow("Pilihan Tampilan", pilihanTampilan);
        aktif = new Checkbox("Aktif"); aktif.setChecked(entity.getAktif()); fb.addRow("Status", aktif);

        org.zkoss.zul.South south = new org.zkoss.zul.South(); south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA); south.setParent(layout); ZkCompat.setFlex(south, true);
        org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar(); toolbar.setStyle("padding:6px 12px;"); toolbar.setParent(south);
        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.addEventListener("onClick", new EventListener() { public void onEvent(Event event) { addWindow.setVisible(false); } }); cancel.setParent(toolbar);
        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.addEventListener("onClick", new EventListener() { public void onEvent(Event event) throws Exception {
            if (onSave(event)) { onSearchDefault(null); addWindow.setVisible(false); }
        }}); save.setParent(toolbar); layout.setParent(window);
    }

    @Override
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty() || domain.getValue().trim().isEmpty()) {
            warning("Nama fasilitas dan domain wajib diisi."); return false;
        }
        String color = warna.getValue().trim();
        if (!color.isEmpty() && !color.matches("#[0-9A-Fa-f]{6}")) {
            warning("Warna utama harus memakai format #RRGGBB, misalnya #047857."); return false;
        }
        String cssValue = css.getValue().trim();
        if (!cssValue.isEmpty() && !cssValue.replace('\\', '/').substring(cssValue.replace('\\', '/').lastIndexOf('/') + 1).matches("[A-Za-z0-9._-]+\\.css")) {
            warning("Tema CSS harus berupa nama file .css yang aman."); return false;
        }
        Session session = HibernateUtil.currentSession();
        Number duplicate = (Number) session.createCriteria(RumahSakit.class).setProjection(Projections.rowCount())
                .add(Restrictions.ilike("domain", domain.getValue().trim(), MatchMode.EXACT))
                .add(currentEntity.getId() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", currentEntity.getId())).uniqueResult();
        if (duplicate != null && duplicate.intValue() > 0) { warning("Domain tersebut sudah digunakan fasilitas kesehatan lain."); return false; }
        RumahSakit entity = currentEntity;
        if (entity.getId() != null) entity = (RumahSakit) session.load(RumahSakit.class, entity.getId());
        entity.setKode(kode.getValue()); entity.setNama(nama.getValue()); entity.setNamaSingkat(namaSingkat.getValue());
        entity.setJenisFasilitas(selected(jenisFasilitas)); entity.setDomain(domain.getValue()); entity.setAlamat(alamat.getValue());
        entity.setTelepon(telepon.getValue()); entity.setWhatsapp(whatsapp.getValue()); entity.setEmail(email.getValue());
        entity.setWebsite(website.getValue()); entity.setMotto(motto.getValue()); entity.setDeskripsi(deskripsi.getValue());
        entity.setNomorIzinOperasional(nomorIzin.getValue()); entity.setCss(cssValue); entity.setWarna(color);
        entity.setPiilhanTampilan(selected(pilihanTampilan)); entity.setAktif(aktif.isChecked());
        Common.refreshSaveOrUpdate(session, entity); RumahSakitUtil.clearCache(); currentEntity = entity;
        return true;
    }

    private Textbox textbox(String value, int rows) { Textbox box = new Textbox(value == null ? "" : value); box.setWidth("100%"); if (rows > 1) box.setRows(rows); return box; }
    private Combobox combo(String[][] values, String selected) {
        Combobox box = new Combobox(); box.setReadonly(true); box.setWidth("100%");
        for (String[] value : values) { Comboitem item = new Comboitem(value[1]); item.setValue(value[0]); item.setParent(box); if (value[0].equals(selected)) box.setSelectedItem(item); }
        if (box.getSelectedItem() == null && !box.getItems().isEmpty()) box.setSelectedIndex(0); return box;
    }
    private String selected(Combobox box) { return box.getSelectedItem() == null ? "" : String.valueOf(box.getSelectedItem().getValue()); }
    private void warning(String message) { MyMessageboxConfig.show(message, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION); }

    class RumahSakitRenderer extends MyRowRenderer {
        @Override public void render(Row row, Object data) throws Exception {
            RumahSakit entity = (RumahSakit) data; row.setValign("top");
            new Label(entity.getKode()).setParent(row);
            RevisiHelper.createNewRevisi(RumahSakit.class, entity, entity.getNama()).setParent(row);
            new Label(entity.getLabelJenisFasilitas()).setParent(row); new Label(entity.getDomain()).setParent(row);
            new Label(entity.getPiilhanTampilan()).setParent(row); new Label(entity.getAktif() ? "Aktif" : "Nonaktif").setParent(row);
            Common.copyEditDeleteButtons(edit, delete, entity, RumahSakitAction.this).setParent(row);
        }
    }
}
