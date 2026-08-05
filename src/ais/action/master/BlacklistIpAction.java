package ais.action.master;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.GenericActionDashboardHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlacklistIp;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Pendataan referensi Blacklist IP.
 * Digunakan untuk membatasi akses dari alamat IP tertentu ke sistem.
 */
public class BlacklistIpAction extends GenericCrudAction<BlacklistIp> {

    private static final long serialVersionUID = -5779730267402400328L;

    // ZK auto-wired tambahan dari ZUL
    protected Html dashboardHtml;
    protected Html progressHtml;
    private Textbox searchkode;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<BlacklistIp> getEntityClass() {
        return BlacklistIp.class;
    }

    @Override
    protected BlacklistIp createNewEntity() {
        return new BlacklistIp();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Blacklist IP";
    }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "keterangan", "aktif" };
    }

    @Override
    protected void onAfterInit(Component comp) throws Exception {
        refreshDashboardSafe();
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(BlacklistIp.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.desc("id"));
        }
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchnama == null || searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new BlacklistIpRenderer();
    }

    // ======================== Override onSearchDefault (progress indicator) ========================

    @Override
    public void onSearchDefault(Event event) {
        GenericActionDashboardHelper.showProgress(progressHtml, 15, "Memuat data",
                "Membaca data sesuai filter yang aktif.");
        super.onSearchDefault(event);
        refreshDashboardSafe();
        GenericActionDashboardHelper.hideProgress(progressHtml);
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final BlacklistIp blacklistIp) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(blacklistIp.getKode());
        kode.setWidth("100%");
        fb.addRow("Alamat IP Blacklist *", kode,
                "Contoh: 192.168.0.1, atau 192.168.0.*, atau 192.168.*, atau *.168.0.1, atau *.168.0.*");

        nama = new Textbox(blacklistIp.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Blacklist", nama);

        keterangan = new Textbox(blacklistIp.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
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

    // ======================== Save logic ========================

    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Alamat IP",
            		"Kolom Alamat IP belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Alamat IP.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        Session session = HibernateUtil.currentSession();
        BlacklistIp entity = currentEntity;
        if (entity.getId() != null) {
            entity = (BlacklistIp) session.load(BlacklistIp.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Dashboard helper ========================

    private void refreshDashboardSafe() {
        try {
            GenericActionDashboardHelper.refresh(dashboardHtml, progressHtml, BlacklistIp.class,
                    "Dasbor Blacklist IP",
                    "Ringkasan alamat IP yang dibatasi aksesnya untuk menjaga keamanan sistem.");
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/BlacklistIpAction.java:219");}
        }
    }

    // ======================== Renderer ========================

    class BlacklistIpRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final BlacklistIp blacklistIp = (BlacklistIp) arg1;

            new Label(blacklistIp.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(BlacklistIp.class, blacklistIp, blacklistIp.getNama()).setParent(arg0);
            new Label(blacklistIp.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(blacklistIp.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    blacklistIp.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(blacklistIp);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, blacklistIp, BlacklistIpAction.this).setParent(arg0);
        }
    }
}
