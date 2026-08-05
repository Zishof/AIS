package ais.action.master.helper;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;

/**
 * Helper revisi umum. Implementasi detail dipusatkan di GenericRevisiHelper.
 */
@SuppressWarnings({ "rawtypes" })
public class RevisiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiHelper(Class<?> myClass, Serializable serializable) throws Exception {
        super(myClass, buildTitle(myClass, null), null, null,
                serializable == null ? null : new GenericRevisiHelper.EntityIdFilter(resolveId(serializable)));
    }

    public RevisiHelper(A a, Class<?> myClass, Serializable serializable, String title, String border, boolean closable)
            throws Exception {
        super(myClass, buildTitle(myClass, title), null, null,
                serializable == null ? null : new GenericRevisiHelper.EntityIdFilter(resolveId(serializable)));
        setBorder(border == null ? "normal" : border);
        setClosable(closable);
        if (a != null) {
            a.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    tampilkanModal(RevisiHelper.this, event);
                }
            });
        }
    }


    private static String buildTitle(Class<?> myClass, String title) {
        String base = title == null || title.trim().length() == 0 ? "Riwayat Revisi Data" : title.trim();
        try {
            if (myClass != null && myClass.getName() != null && myClass.getName().trim().length() > 0) {
                return base + " - " + myClass.getName();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:63");
        }
        return base;
    }

    private static Serializable resolveId(Serializable serializable) {
        try {
            if (serializable instanceof GeneralValueObject) {
                return ((GeneralValueObject) serializable).getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:73");
        }
        return serializable;
    }

    public static Vbox createNewRevisi(final Class class1, GeneralValueObject generalValueObject, String label) {
        return createNewRevisi(class1, generalValueObject, null, label, null);
    }

    public static Vbox createNewRevisi(final Class class1, GeneralValueObject generalValueObject, String label,
            String style) {
        return createNewRevisi(class1, generalValueObject, null, label, style);
    }

    public static Vbox createNewRevisi(final Class class1, GeneralValueObject generalValueObject, Long refIdLagi,
            String label) {
        return createNewRevisi(class1, generalValueObject, refIdLagi, label, null);
    }

    public static Vbox createNewRevisi(final Class class1, final GeneralValueObject generalValueObject,
            final Long refIdLagi, String label, String style) {

        Vbox vbox = new Vbox();

        try {
            if (style != null && style.trim().length() > 0) {
                vbox.setStyle(style);
            } else {
                vbox.setStyle("max-width:100%;");
            }

            final String labelRevisi = label == null || label.trim().length() == 0 ? "Revisi" : label.trim();

            if (!bolehLihatRevisi()) {
                Label labelBiasa = new Label(labelRevisi);

                labelBiasa.setStyle(
                        "font-size:11px;"
                        + "color:black;"
                        + "white-space:normal;"
                        + "word-wrap:break-word;"
                        + "overflow-wrap:break-word;"
                        + "word-break:break-word;"
                        + "line-height:15px;"
                        + "max-width:100%;"
                        + "display:block;"
                );

                labelBiasa.setMultiline(true);
                labelBiasa.setParent(vbox);

                return vbox;
            }

            A link = new A();
            link.setLabel(labelRevisi);
            link.setTooltiptext("Lihat riwayat revisi data");

            link.setStyle(
                    "display:inline-block;"
                    + "font-size:11px;"
                    + "font-weight:bold;"
                    + "color:#2563eb;"
                    + "text-decoration:none;"
                    + "border:1px solid #bfdbfe;"
                    + "background:#eff6ff;"
                    + "border-radius:5px;"
                    + "padding:3px 7px;"
                    + "line-height:16px;"
                    + "white-space:normal;"
                    + "word-wrap:break-word;"
                    + "overflow-wrap:break-word;"
                    + "word-break:break-word;"
                    + "max-width:100%;"
                    + "cursor:pointer;"
            );

            link.setParent(vbox);

            link.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    if (generalValueObject == null || generalValueObject.getId() == null) {
                        MyMessageboxConfig.show("Data belum tersimpan sehingga belum memiliki riwayat revisi.");
                        return;
                    }

                    Serializable id = refIdLagi == null ? generalValueObject.getId() : refIdLagi;

                    RevisiHelper helper = new RevisiHelper(class1, id);
                    tampilkanModal(helper, event);
                }
            });

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException(
                    "menampilkan tautan riwayat revisi",
                    e, new String[] {
                            "Muat ulang (refresh) halaman ini lalu coba kembali.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        }

        return vbox;
    }

    private static boolean bolehLihatRevisi() {
        String daftar = "am,amp";
        try {
            daftar = Common.getKonfigurasi("boleh_lihat_revisi", "am,amp").getNilai().trim();
        } catch (Exception e) {
            daftar = "am,amp";
        }
        if (daftar == null || daftar.trim().length() == 0) {
            return false;
        }

        Tbmuser tbmuser = null;
        String userId = null;
        String roleId = null;
        try {
            tbmuser = Common.getCurrentUser();
            if (tbmuser != null) {
                userId = tbmuser.getUserId();
                Tbmrole role = tbmuser.hakAkses();
                roleId = role == null ? null : role.getRoleId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:194");
        }

        return containsToken(daftar, roleId) || containsToken(daftar, userId);
    }

    private static boolean containsToken(String csv, String value) {
        if (csv == null || value == null) {
            return false;
        }
        String cleanedValue = value.trim();
        if (cleanedValue.length() == 0) {
            return false;
        }
        String[] tokens = csv.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i] == null ? "" : tokens[i].trim();
            if (token.length() > 0 && token.equalsIgnoreCase(cleanedValue)) {
                return true;
            }
        }
        return false;
    }

    private static void tampilkanModal(RevisiHelper helper, Event event) throws Exception {
        if (helper == null) {
            return;
        }

        if (helper.getParent() == null && helper.getPage() == null) {
            Component root = null;
            try {
                Page page = ExecutionsCtrl.getCurrentCtrl() == null ? null
                        : ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
                if (page != null) {
                    root = page.getFirstRoot();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:231");
            }

            if (root == null && event != null && event.getTarget() != null) {
                try {
                    Page page = event.getTarget().getPage();
                    if (page != null) {
                        root = page.getFirstRoot();
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:240");
                }
            }

            if (root != null) {
                root.appendChild(helper);
            } else {
                MyMessageboxConfig.show("Jendela riwayat revisi belum bisa dibuka karena halaman utama belum siap.");
                return;
            }
        }

        helper.setVisible(true);
        helper.onModal();
    }

    @SuppressWarnings("deprecation")
	public static void tampilDetail(final MyDetail detail, Rows rows, final java.util.Date tanggal, final String olehId,
            final GeneralValueObject currentObj) {
        if (detail == null || rows == null) {
            return;
        }
        try {
            Common.clear(detail);
            Row info = new Row();
            info.setParent(rows);
            ais.ui.util.ZkCompat.setSpans(info, "4");
            info.appendChild(new org.zkoss.zul.Label("Tanggal revisi: "
                    + (tanggal == null ? "" : Common.datetimeFormat2s.get().format(tanggal)) + " oleh "
                    + (olehId == null ? "" : olehId)));
            rows.setParent(detail);
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:272");}
        }
    }

    public static void updatePropertyAndSave(GeneralValueObject obj, ClassMetadata meta, String prop, Object newVal) {
        Session session = null;
        Transaction tx = null;
        try {
            if (obj == null || obj.getId() == null || meta == null || prop == null) {
                return;
            }
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            Object current = session.get(obj.getClass(), obj.getId());
            if (current != null) {
                meta.setPropertyValue(current, prop, newVal, EntityMode.POJO);
                session.saveOrUpdate(current);
            }
            tx.commit();
        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:292");}
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:293");}
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:296");}
                try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:297");}
                try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:298");}
            }
        }
    }
}
