package ais.action.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.GenericRevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * Layar penjelajah riwayat revisi (audit trail) LINTAS-ENTITAS berbasis Hibernate Envers: pengguna
 * memilih satu kelas entitas turunan {@link GeneralValueObject} (daftar kelas dibangun otomatis dari
 * metadata Hibernate — {@link #initClassCombobox()} — sehingga selalu sinkron dengan model data
 * terbaru tanpa perlu didaftarkan manual), lalu mencari revisinya berdasarkan tipe perubahan
 * (Tambah/Ubah/Hapus), ID (untuk pencarian "Semua Kolom"), atau nilai properti tertentu. Setiap
 * baris hasil menampilkan ringkasan entitas pada revisi tersebut dan tombol untuk membuka riwayat
 * revisi lengkap entitas itu lewat {@link GenericRevisiHelper}. Berbeda dari layar lain yang
 * memakai session Hibernate bersama (thread-local), kelas ini sengaja membuka
 * {@link Session Session} sendiri per operasi ({@link #initClassCombobox()}, {@link
 * #onSearchDefault(Event)}) dan menutupnya eksplisit lewat {@link #closeSession(Session)}, karena
 * query {@link AuditReader} lintas-kelas rawan konflik dengan sesi yang dipakai bersama.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;


    private Combobox searchClass;
    private Combobox searchKolom;
    private Textbox searchData;
    private Combobox searchTipe;
    private MyGrid grid;

    /** Melakukan pengecekan keamanan baku, mengisi combobox tipe perubahan dan daftar kelas entitas, lalu menyegarkan dasbor ringkasan revisi. */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.doCheckSecurity();
        initTipe();
        initClassCombobox();
		refreshDashboardAman();
    }

    private void initTipe() {
        if (searchTipe == null || searchTipe.getChildren().size() > 0) {
            try {
                if (searchTipe != null && searchTipe.getSelectedItem() == null && searchTipe.getItemCount() > 0) {
                    searchTipe.setSelectedIndex(0);
                }
            } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
            return;
        }
        addCombo(searchTipe, "Semua", "");
        addCombo(searchTipe, "Tambah", "ADD");
        addCombo(searchTipe, "Ubah", "MOD");
        addCombo(searchTipe, "Hapus", "DEL");
        searchTipe.setSelectedIndex(0);
    }

    private void addCombo(Combobox combo, String label, Object value) {
        Comboitem item = new Comboitem(label);
        item.setValue(value);
        combo.appendChild(item);
    }

    private void initClassCombobox() {
        Session session = null;
        try {
            if (searchClass == null) {
                return;
            }
            searchClass.getChildren().clear();
            session = HibernateUtil.getSessionFactory().openSession();
            Map metadataMap = session.getSessionFactory().getAllClassMetadata();
            List classes = new ArrayList();

            for (Object key : metadataMap.keySet()) {
                ClassMetadata meta = (ClassMetadata) metadataMap.get(key);
                Class mappedClass = meta == null ? null : meta.getMappedClass(EntityMode.POJO);
                if (mappedClass != null && GeneralValueObject.class.isAssignableFrom(mappedClass)) {
                    classes.add(mappedClass);
                }
            }

            Collections.sort(classes, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Class c1 = (Class) o1;
                    Class c2 = (Class) o2;
                    return c1.getSimpleName().compareToIgnoreCase(c2.getSimpleName());
                }
            });

            for (Object obj : classes) {
                Class clazz = (Class) obj;
                Comboitem item = new Comboitem(clazz.getSimpleName());
                item.setDescription(clazz.getName());
                item.setValue(clazz);
                searchClass.appendChild(item);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSession(session);
        }
    }

    /**
     * Menyegarkan combobox pilihan kolom sesuai kelas entitas yang baru dipilih: selalu menyediakan
     * opsi "Semua Kolom / ID" di awal, diikuti seluruh nama properti Hibernate kelas tersebut.
     *
     * @param event event ZK pemicu perubahan pilihan kelas
     */
    public void onChangeClass(Event event) {
        if (searchKolom == null) {
            return;
        }
        searchKolom.getChildren().clear();
        searchKolom.setText("");

        Comboitem semua = new Comboitem("-- Semua Kolom / ID --");
        semua.setValue("ALL");
        searchKolom.appendChild(semua);
        searchKolom.setSelectedItem(semua);

        try {
            if (searchClass != null && searchClass.getSelectedItem() != null) {
                Class clazz = (Class) searchClass.getSelectedItem().getValue();
                ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
                if (meta != null) {
                    String[] propNames = meta.getPropertyNames();
                    for (int i = 0; i < propNames.length; i++) {
                        Comboitem item = new Comboitem(propNames[i]);
                        item.setValue(propNames[i]);
                        searchKolom.appendChild(item);
                    }
                }
            }
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/RevisiAction.java:154");}
        }
    }

    /**
     * Menjalankan pencarian riwayat revisi untuk kelas entitas terpilih (wajib dipilih lebih dulu),
     * difilter tipe perubahan (Tambah/Ubah/Hapus) dan kata kunci opsional — pencarian "Semua Kolom"
     * hanya menerima ID berupa angka (bukan teks bebas), sedang pencarian per kolom memakai
     * pencocokan {@code LIKE} pada properti terpilih. Hasil dibatasi 200 baris terbaru, seluruh
     * properti entitas hasil di-inisialisasi eager (mengatasi lazy-loading di luar sesi) lewat
     * {@link #eagerInitialize}, lalu dirender ke grid memakai {@link GlobalAuditRenderer}.
     *
     * @param event event ZK pemicu pencarian
     * @throws Exception diteruskan apa adanya dari kegagalan query Envers
     */
    public void onSearchDefault(Event event) throws Exception {
        if (searchClass == null || searchClass.getSelectedItem() == null) {
            MyMessageboxConfig.show("Silakan pilih Nama Tabel / Class terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return;
        }

        Class selectedClass = (Class) searchClass.getSelectedItem().getValue();
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            AuditReader reader = AuditReaderFactory.get(session);
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(selectedClass, false, true);
            query.addOrder(AuditEntity.revisionNumber().desc());

            String tipe = searchTipe == null || searchTipe.getSelectedItem() == null ? ""
                    : String.valueOf(searchTipe.getSelectedItem().getValue());
            if ("ADD".equals(tipe)) {
                query.add(AuditEntity.revisionType().eq(RevisionType.ADD));
            } else if ("MOD".equals(tipe)) {
                query.add(AuditEntity.revisionType().eq(RevisionType.MOD));
            } else if ("DEL".equals(tipe)) {
                query.add(AuditEntity.revisionType().eq(RevisionType.DEL));
            }

            String keyword = searchData == null || searchData.getValue() == null ? "" : searchData.getValue().trim();
            if (keyword.length() > 0) {
                String kolom = searchKolom == null || searchKolom.getSelectedItem() == null ? "ALL"
                        : String.valueOf(searchKolom.getSelectedItem().getValue());
                if ("ALL".equals(kolom)) {
                    try {
                        Long idTarget = Long.valueOf(keyword);
                        query.add(AuditEntity.id().eq(idTarget));
                    } catch (Exception e) {
                        MyMessageboxConfig.show(
                                "Pencarian semua kolom hanya mendukung ID angka. Untuk teks, pilih nama kolom tertentu.",
                                "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                } else {
                    query.add(AuditEntity.property(kolom).like(keyword, org.hibernate.criterion.MatchMode.ANYWHERE));
                }
            }

            query.setMaxResults(200);
            List results = query.getResultList();
            eagerInitialize(selectedClass, session, results);

            ListModel model = new SimpleListModel(results);
            grid.setRowRenderer(new GlobalAuditRenderer(selectedClass));
            grid.setModelCheckMobile(model);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeSession(session);
        }
    }

    private void eagerInitialize(Class selectedClass, Session session, List results) {
        try {
            ClassMetadata meta = HibernateUtil.getSessionFactory().getClassMetadata(selectedClass);
            if (results == null || meta == null) {
                return;
            }
            String[] props = meta.getPropertyNames();
            for (int i = 0; i < results.size(); i++) {
                Object entity = extractEntity(results.get(i), selectedClass);
                if (entity == null) {
                    continue;
                }
                try { entity.toString(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
                for (int p = 0; p < props.length; p++) {
                    try {
                        Object value = meta.getPropertyValue(entity, props[p], EntityMode.POJO);
                        if (value != null) {
                            Hibernate.initialize(value);
                        }
                    } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
                }
            }
        } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
    }

    private Object extractEntity(Object row, Class selectedClass) {
        if (row == null) {
            return null;
        }
        if (selectedClass.isInstance(row)) {
            return row;
        }
        if (row instanceof Object[]) {
            Object[] arr = (Object[]) row;
            if (arr.length > 0 && selectedClass.isInstance(arr[0])) {
                return arr[0];
            }
        }
        return null;
    }

    class GlobalAuditRenderer extends ais.ui.util.MyRowRenderer {
        private Class entityClass;
        private ClassMetadata classMetadata;

        public GlobalAuditRenderer(Class entityClass) {
            this.entityClass = entityClass;
            this.classMetadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        }

        public void render(Row row, Object data) throws Exception {
            Object entity = extractEntity(data, entityClass);
            if (!(entity instanceof Serializable)) {
                row.setVisible(false);
                return;
            }

            Serializable auditEntityObj = (Serializable) entity;
            Serializable entityIdTemp = null;
            try {
                entityIdTemp = classMetadata.getIdentifier(auditEntityObj, EntityMode.POJO);
            } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

            final Serializable entityId = entityIdTemp;
            final Class selectedEntityClass = entityClass;

            new Label(entityId == null ? "" : String.valueOf(entityId)).setParent(row);
            new Label(selectedEntityClass.getSimpleName()).setParent(row);
            new Label(String.valueOf(auditEntityObj)).setParent(row);

            MyToolbarbuttonConfig detail = new MyToolbarbuttonConfig("Buka Revisi", "/img/svg/history.svg");
            detail.setParent(row);
            detail.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
                public void onEvent(Event event) throws Exception {
                    GenericRevisiHelper helper = new GenericRevisiHelper(selectedEntityClass, "Riwayat Revisi Data", null, null,
                            new GenericRevisiHelper.EntityIdFilter(entityId));
                    tampilkanModal(helper, event);
                }
            });
        }
    }

    private void tampilkanModal(GenericRevisiHelper helper, Event event) throws Exception {
        if (helper == null) {
            return;
        }
        if (helper.getParent() == null && helper.getPage() == null) {
            org.zkoss.zk.ui.Component root = null;
            try {
                org.zkoss.zk.ui.Page page = org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl() == null ? null
                        : org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
                if (page != null) {
                    root = page.getFirstRoot();
                }
            } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
            if (root == null && event != null && event.getTarget() != null) {
                try {
                    org.zkoss.zk.ui.Page page = event.getTarget().getPage();
                    if (page != null) {
                        root = page.getFirstRoot();
                    }
                } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
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

    private void closeSession(Session session) {
        if (session != null) {
            try { session.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
            try { session.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
            try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
        }
    }

	private void refreshDashboardAman() {
		try {
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Riwayat Revisi", "Ringkas perubahan data penting agar admin mudah melihat tambah, ubah, dan hapus data tanpa membuka baris satu per satu.");
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RevisiAction.java:363");
			}
		}
	}

}
