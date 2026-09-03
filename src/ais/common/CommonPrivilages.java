package ais.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.sys.SessionsCtrl;

import ais.database.hibernate.AuditTrailHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.LogUserActifity;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Helper menu dan privilege untuk common privilages. Tipe ini membentuk navigasi berdasarkan hak
 * pengguna dan menjadi satu sumber pemeriksaan tampilan menu agar action tidak menyusun kebijakan
 * sendiri.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Object LOG_ACTIVITY_SCHEMA_LOCK},
 * {@code boolean logActivitySchemaChecked}, {@code Integer READ}, {@code Integer CREATE}, {@code Integer
 * UPDATE}, {@code Integer DELETE}, {@code Integer APPROVE}, {@code Integer REJECT}; validasi/perhitungan ({@code
 * doCheckPrevilagesRead()}, {@code checkPrevilages()}, {@code checkPrevilages()}, {@code checkPrevilages()},
 * {@code checkPrevilages()}); mutasi data ({@code saveActivity()}); operasi domain lain ({@code
 * bersihkanKarakterDatabase()}, {@code pastikanSequenceLogUserActifity()}, {@code buildKeterangan()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class CommonPrivilages {
	private static final Object LOG_ACTIVITY_SCHEMA_LOCK = new Object();
	private static volatile boolean logActivitySchemaChecked = false;

    public final static Integer READ = 0;
    public final static Integer CREATE = 1;
    public final static Integer UPDATE = 2;
    public final static Integer DELETE = 3;
    public final static Integer APPROVE = 4;
    public final static Integer REJECT = 5;

    @SuppressWarnings({ "rawtypes", "deprecation" })
    public static boolean saveActivity(Class classCalled, Integer activityType, Serializable serializable,
            String perubahan) {
        if (serializable == null || !AuditTrailHelper.isAuditable(serializable)) {
            AuditTrailHelper.debug("CommonPrivilages.saveActivity skip non-auditable "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
            return false;
        }
        Session session = null;
        Transaction transaction = null;
        try {
            DetailLogLogin detailLogLogin = (DetailLogLogin) (SessionsCtrl.getCurrent() == null ? null
                    : SessionsCtrl.getCurrent().getAttribute("detailLogLogin"));
            if (detailLogLogin == null || detailLogLogin.getId() == null) {
                AuditTrailHelper.debug("CommonPrivilages.saveActivity skip karena detailLogLogin tidak tersedia untuk "
                        + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
                return false;
            }

            String namaClass = serializable.getClass().getSimpleName();
            String activity = activityType.equals(READ) ? "Membaca data " + namaClass
                    : activityType.equals(CREATE) ? "Menyimpan data baru " + namaClass
                            : activityType.equals(UPDATE) ? "Merubah data " + namaClass
                                    + (classCalled == null ? "" : " " + classCalled.getSimpleName())
                                    : activityType.equals(DELETE) ? "Menghapus data " + namaClass : "";

            String keterangan1 = buildKeterangan(serializable);

			pastikanSequenceLogUserActifity();

            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            LogUserActifity actifity = new LogUserActifity();
            actifity.setDetailLogLogin(detailLogLogin);
            actifity.setKeterangan(bersihkanKarakterDatabase(activity));
            actifity.setKeterangan1(bersihkanKarakterDatabase(keterangan1));
            actifity.setKeterangan12(bersihkanKarakterDatabase(perubahan == null ? "" : perubahan));
            actifity.setActivityType(activityType);
            actifity.setClassCalled(bersihkanKarakterDatabase(classCalled == null ? "" : classCalled.getName()));

            AuditTrailHelper.debug("CommonPrivilages.saveActivity mulai simpan "
                    + AuditTrailHelper.describeActivity(activityType) + " "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable))
                    + ", perubahan=" + AuditTrailHelper.abbreviate(perubahan == null ? "" : perubahan.replace('\n', ';'), 800));

            session.save(actifity);
            transaction.commit();
            AuditTrailHelper.debug("CommonPrivilages.saveActivity sukses "
                    + AuditTrailHelper.describeActivity(activityType) + " "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)));
            return true;
        } catch (Exception e) {
            try {
                if (transaction != null && !transaction.wasCommitted() && !transaction.wasRolledBack()) {
                    transaction.rollback();
                }
            } catch (Exception rollbackException) {
				ErrorAuditUtil.record(rollbackException,
						"CommonPrivilages.saveActivity gagal rollback audit aktivitas");
            }
            AuditTrailHelper.debug("CommonPrivilages.saveActivity gagal "
                    + AuditTrailHelper.describeActivity(activityType) + " "
                    + AuditTrailHelper.describeEntity(serializable, AuditTrailHelper.safeIdentifier(serializable)), e);
			/*
			 * Audit aktivitas adalah fungsi pendamping. Kegagalannya wajib tercatat ke
			 * ErrorLog/server log, tetapi tidak boleh menampilkan message box atau
			 * membatalkan pembuatan halaman/transaksi utama pengguna.
			 */
			ErrorAuditUtil.record(e, "CommonPrivilages.saveActivity gagal menyimpan LogUserActifity");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
        return false;
    }

	/** PostgreSQL menolak byte NUL di seluruh kolom teks UTF-8. */
	private static String bersihkanKarakterDatabase(String nilai) {
		if (nilai == null || nilai.indexOf('\u0000') < 0) {
			return nilai;
		}
		return nilai.replace('\u0000', ' ');
	}

	/**
	 * Memulihkan sequence legacy LogUserActifity yang pada sebagian database lama
	 * tidak ikut terbawa saat restore. Hibernate IDENTITY PostgreSQL mengharapkan
	 * nama baku public.log_user_actifity_id_seq untuk mengambil id hasil insert.
	 * Dijalankan paling banyak sekali per JVM dan idempoten.
	 */
	private static void pastikanSequenceLogUserActifity() {
		if (logActivitySchemaChecked) {
			return;
		}
		synchronized (LOG_ACTIVITY_SCHEMA_LOCK) {
			if (logActivitySchemaChecked) {
				return;
			}
			Session schemaSession = null;
			Transaction schemaTransaction = null;
			try {
				schemaSession = HibernateUtil.getSessionFactory().openSession();
				schemaTransaction = schemaSession.beginTransaction();
				schemaSession.createSQLQuery(
						"create sequence if not exists public.log_user_actifity_id_seq").executeUpdate();
				schemaSession.createSQLQuery(
						"lock table public.log_user_actifity in access exclusive mode").executeUpdate();
				schemaSession.createSQLQuery(
						"select setval('public.log_user_actifity_id_seq', "
								+ "coalesce((select max(id) from public.log_user_actifity), 0) + 1, false)")
						.uniqueResult();
				schemaSession.createSQLQuery(
						"alter table public.log_user_actifity alter column id set default "
								+ "nextval('public.log_user_actifity_id_seq')").executeUpdate();
				schemaSession.createSQLQuery(
						"alter sequence public.log_user_actifity_id_seq owned by public.log_user_actifity.id")
						.executeUpdate();
				schemaTransaction.commit();
			} catch (Exception e) {
				try {
					if (schemaTransaction != null && !schemaTransaction.wasCommitted()
							&& !schemaTransaction.wasRolledBack()) {
						schemaTransaction.rollback();
					}
				} catch (Exception rollbackException) {
					ErrorAuditUtil.record(rollbackException,
							"CommonPrivilages.pastikanSequenceLogUserActifity rollback gagal");
				}
				ErrorAuditUtil.record(e,
						"Gagal memperbaiki public.log_user_actifity_id_seq; audit dibuat fail-open");
			} finally {
				logActivitySchemaChecked = true;
				HibernateUtil.closeSessionQuietly(schemaSession);
			}
		}
	}

    @SuppressWarnings({ "rawtypes", "deprecation" })
    private static String buildKeterangan(Serializable serializable) {
        StringBuilder keterangan = new StringBuilder();
        if (serializable == null) {
            return "";
        }
        try {
            String clazz = serializable.getClass().getName();
            ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Class.forName(clazz.split("_")[0]));
            if (classMetadata != null) {
                Serializable id = classMetadata.getIdentifier(serializable, EntityMode.POJO);
                keterangan.append("id=").append(id).append('\n');
                String[] properties = classMetadata.getPropertyNames();
                for (int i = 0; i < properties.length; i++) {
                    String property = properties[i];
                    Object value = null;
                    try {
                        value = classMetadata.getPropertyValue(serializable, property, EntityMode.POJO);
                    } catch (Exception e) {
                        continue;
                    }
                    if (value == null) {
                        continue;
                    }
                    // FIX LazyInitializationException: value bisa berupa koleksi/proxy Hibernate
                    // lazy (mis. Tbmrole.menus, sebuah Set) yang belum ter-initialize. Memanggil
                    // .toString() padanya (lewat StringBuilder.append di bawah) ATAU
                    // myClassMetadata.getPropertyValue di bawah memicu inisiasi yang butuh
                    // session aktif -- kalau session sudah tertutup (audit dipicu belakangan
                    // di luar konteks request asli, lihat DataUtil.ubahDataHistory), melempar
                    // LazyInitializationException. Cek isInitialized dulu SEBELUM disentuh sama
                    // sekali, jangan andalkan catch semata -- properti ini memang tak bisa
                    // di-string-kan pada kondisi ini, konsisten dgn niat "abaikan property yang
                    // tidak bisa di-string-kan" di bawah.
                    if (!Hibernate.isInitialized(value)) {
                        continue;
                    }
                    Object displayValue = null;
                    try {
                        ClassMetadata myClassMetadata = HibernateUtil.getClassMetadata(value.getClass());
                        if (myClassMetadata != null) {
							try {
								displayValue = myClassMetadata.getPropertyValue(value, "nama", EntityMode.POJO);
							} catch (Exception tidakPunyaNama) {
								Serializable valueId = myClassMetadata.getIdentifier(value, EntityMode.POJO);
								displayValue = AuditTrailHelper.describeEntity(value, valueId);
							}
                        }
                    } catch (Exception e) {
                        displayValue = null;
                    }
                    try {
                        keterangan.append(property).append("=")
								.append(displayValue == null ? nilaiAuditAman(value) : nilaiAuditAman(displayValue))
								.append('\n');
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPrivilages.java:139");
                        // abaikan property yang tidak bisa di-string-kan
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return keterangan.toString();
    }

	/**
	 * Membentuk teks audit tanpa memanggil {@code toString()} object domain secara buta.
	 * Method {@code toString()} beberapa entity/komponen membaca relasi lazy di dalamnya;
	 * saat audit dijalankan setelah session asal ditutup hal itu memicu
	 * {@code LazyInitializationException} dan mengganggu update bisnis utama.
	 */
	private static String nilaiAuditAman(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
				|| value instanceof Character || value instanceof java.util.Date || value instanceof java.util.Calendar
				|| value.getClass().isEnum()) {
			return String.valueOf(value);
		}
		if (value instanceof java.util.Collection) {
			return value.getClass().getSimpleName() + "#size=" + ((java.util.Collection) value).size();
		}
		if (value instanceof java.util.Map) {
			return value.getClass().getSimpleName() + "#size=" + ((java.util.Map) value).size();
		}
		Serializable id = AuditTrailHelper.safeIdentifier(value);
		if (id != null) {
			return AuditTrailHelper.describeEntity(value, id);
		}
		return value.getClass().getName();
	}

    public static String[] MUST_CHECKED = new String[] { "/pages/master/mahasiswa.zul", "/pages/master/fakultas.zul",
            "/pages/master/dosen.zul", "/pages/master/pegawai.zul", "/pages/master/jurusan.zul",
            "/pages/master/matakuliah.zul", "/pages/master/konfigurasi.zul", "/pages/master/setting_biaya.zul",
            "/pages/master/penilaian.zul", "/pages/master/konfigurasi_detail.zul",
            "/pages/master/detail_biaya_excel.zul", "/pages/master/detail_biaya.zul" };

    public static void doCheckPrevilagesRead() {
        try {
            String url = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getRequestPath();
            for (String u : MUST_CHECKED) {
                if (u.equalsIgnoreCase(url)) {
                    if (!CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
                        Common.goLogoff();
                    }
                    return;
                }
            }

        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPrivilages.java:168");

        }

    }

    public static boolean checkPrevilages(Integer privilagesCode, Tbmuser tbmuser) {
        Menu menu = Common.getCurrentMenu();
        return checkPrevilages(menu, privilagesCode, tbmuser);
    }

    public static boolean checkPrevilages(Integer privilagesCode) {
        Menu menu = Common.getCurrentMenu();
        return checkPrevilages(menu, privilagesCode);
    }

    public static Map<String, List<RolePrivilage>> rolePrivilagesUtama = new HashMap<String, List<RolePrivilage>>();

    public static boolean checkPrevilages(Menu menu, Integer privilagesCode) {
        Tbmuser tbmuser = Common.getCurrentUser();
        return checkPrevilages(menu, privilagesCode, tbmuser);
    }

    public static boolean checkPrevilages(Menu menu, Integer privilagesCode, Tbmuser tbmuser) {
        boolean result = false;
        if (menu == null) {
            return result;
        }

        if (tbmuser == null || tbmuser.hakAkses() == null) {
            return false;
        }
        Tbmrole tbmrole = tbmuser.hakAkses();

        String key = tbmrole.getRoleId() + "_" + menu.getId();

        List<RolePrivilage> rolePrivilages = rolePrivilagesUtama.get(key);
        boolean reload = rolePrivilages == null;

        if (reload) {
            Session session = null;
            try {
                session = HibernateUtil.currentNativeSession();
                rolePrivilages = ConstantValues.simpleList(session.createCriteria(RolePrivilage.class)
                        .add(Restrictions.eq("role", tbmrole)).add(Restrictions.eq("menu", menu)), RolePrivilage.class);
                rolePrivilagesUtama.put(key, rolePrivilages);
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            } finally {
                HibernateUtil.closeSession();
            }
        }

        if (rolePrivilages == null) {
            return false;
        }

        for (RolePrivilage rolePrivilage : rolePrivilages) {
            if (rolePrivilage != null) {
                if (READ.equals(privilagesCode)) {
                    result = rolePrivilage.getRead().equals(1);
                } else if (CREATE.equals(privilagesCode)) {
                    result = rolePrivilage.getCreate().equals(1);
                } else if (UPDATE.equals(privilagesCode)) {
                    result = rolePrivilage.getUpdate().equals(1);
                } else if (DELETE.equals(privilagesCode)) {
                    result = rolePrivilage.getDelete().equals(1);
                } else if (APPROVE.equals(privilagesCode)) {
                    result = rolePrivilage.getApprove().equals(1);
                } else if (REJECT.equals(privilagesCode)) {
                    result = rolePrivilage.getReject().equals(1);
                }
            }
            if (result) {
                break;
            }
        }

        return result;
    }
}
