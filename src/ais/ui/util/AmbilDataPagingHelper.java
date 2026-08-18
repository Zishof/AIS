package ais.ui.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Paging;
import org.zkoss.zul.South;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * Helper generik paging server-side untuk semua banbox/popup "AmbilData*".
 *
 * Pola lama: criteria dengan setMaxResults(Common.MAX_RESULT_100) lalu grid
 * mold "paging" (paging di sisi client, maksimal 100 baris, boros memori).
 *
 * Pola baru (mengikuti ais.action.master.sekolah.helper.AmbilDataSiswaBanbox):
 * kelas pemakai menyediakan {@link CriteriaFactory#initCriteria(Session, boolean)},
 * helper ini yang menghitung total baris (count query), menarik HANYA satu
 * halaman data (setFirstResult/setMaxResults), dan mengelola komponen
 * {@link Paging} ZK. Session dibuka dan ditutup di sini sehingga tidak
 * membocorkan session thread-local; entity hasil query dapat diinisialisasi
 * relasinya selagi session masih terbuka lewat {@link Inisialisasi}.
 *
 * Contoh pemakaian di display():
 *
 * <pre>
 * pagingHelper = new AmbilDataPagingHelper();
 * pagingHelper.pasangOnPaging(new EventListener() { ... onSearchDefault(null); ... });
 * pagingHelper.pasangGridDanPaging(center, grid = new MyGrid());
 * </pre>
 *
 * dan di onSearchDefault():
 *
 * <pre>
 * List&lt;Mahasiswa&gt; data = pagingHelper.cari(new AmbilDataPagingHelper.CriteriaFactory() {
 * 	public Criteria initCriteria(Session session, boolean isOrder) { ... }
 * }, Mahasiswa.class, inisialisasiOpsional);
 * </pre>
 */
public class AmbilDataPagingHelper implements Serializable {

	private static final long serialVersionUID = 7726011206201260712L;

	/** Jumlah baris per halaman standar seluruh popup AmbilData*. */
	public static final int PAGE_SIZE_DEFAULT = 5;

	/**
	 * Kontrak pembuat criteria dua-fase: isOrder=false dipakai untuk count
	 * query (tanpa Order karena projection rowCount menolak order by kolom
	 * yang tidak diselect di beberapa database), isOrder=true untuk menarik
	 * data halaman aktif.
	 */
	public interface CriteriaFactory {
		Criteria initCriteria(Session session, boolean isOrder);
	}

	/** Hook inisialisasi relasi lazy selagi session masih terbuka. */
	public interface Inisialisasi<T> {
		void init(T data);
	}

	private final Paging paging;
	private final int pageSize;

	public AmbilDataPagingHelper() {
		this(PAGE_SIZE_DEFAULT);
	}

	public AmbilDataPagingHelper(int pageSize) {
		this.pageSize = pageSize <= 0 ? PAGE_SIZE_DEFAULT : pageSize;
		paging = new Paging();
		paging.setPageSize(this.pageSize);
		paging.setMold("os");
		paging.setDetailed(true);
	}

	public Paging getPaging() {
		return paging;
	}

	public int getPageSize() {
		return pageSize;
	}

	/** Listener pindah halaman: panggil ulang pencarian TANPA reset halaman. */
	public void pasangOnPaging(final EventListener cariUlang) {
		paging.addEventListener("onPaging", cariUlang);
	}

	/**
	 * Event untuk tombol "Cari"/onOK/onChange field filter: reset ke halaman
	 * pertama dulu supaya hasil filter baru tidak terdampar di offset lama.
	 */
	public EventListener buatEventCari(final EventListener cariUlang) {
		return new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				paging.setActivePage(0);
				cariUlang.onEvent(arg0);
			}
		};
	}

	/**
	 * Tata letak seragam: grid mengisi Center, kontrol paging menempel di
	 * South. Mengembalikan borderlayout-nya bila pemanggil perlu mengatur
	 * tinggi.
	 */
	public Borderlayout pasangGridDanPaging(Component parent, org.zkoss.zul.Grid grid) {
		Borderlayout layout = new MyBorderlayout();
		/* PENTING: Borderlayout WAJIB punya tinggi eksplisit. Ketika ditaruh sebagai
		 * satu-satunya anak di dalam sebuah Center/kontainer flex (mis. popup
		 * AmbilDataRuangBanbox), Borderlayout TANPA width/height akan KOLAPS ke tinggi 0,
		 * sehingga region Center (grid) & South (paging) tidak terlihat → popup tampak
		 * KOSONG/blank meski data ada. Popup lain (mis. AmbilDataSiswaBanbox) menaruh grid
		 * LANGSUNG di Center flex tanpa Borderlayout perantara sehingga tak pernah kolaps.
		 * Isi penuh parent agar konsisten & tidak pernah blank. */
		layout.setWidth("100%");
		layout.setHeight("100%");
		layout.setParent(parent);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(layout);

		South south = new South();
		south.setParent(layout);

		grid.setWidth("100%");
		grid.setParent(center);
		paging.setParent(south);
		return layout;
	}

	public <T> List<T> cari(CriteriaFactory factory, Class<T> kelas) {
		return cari(factory, kelas, null);
	}

	/**
	 * Tutup session pencarian dengan BENAR: transaksi implisit diakhiri dulu.
	 *
	 * PostgreSQL + Hibernate (autocommit=false): SELECT pertama otomatis
	 * membuka transaksi. session.close() TIDAK menutup transaksi itu, sehingga
	 * koneksi kembali ke pool berstatus "idle in transaction" dan lock yang
	 * pernah dipegangnya tertahan berjam-jam (gejala: "Row masih dikunci
	 * transaksi lain ... idle in transaction" pada disposisi_sop dengan query
	 * terakhir = count siswa/perkuliahan milik popup AmbilData). Rollback
	 * lewat koneksi JDBC mengakhiri transaksi implisit tersebut.
	 */
	public static void tutupSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.doWork(new org.hibernate.jdbc.Work() {
				@Override
				public void execute(java.sql.Connection connection) throws java.sql.SQLException {
					if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
						connection.rollback();
					}
				}
			});
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:175");
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:179");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:183");
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:187");
		}
	}

	/**
	 * Varian untuk migrasi massal: criteria SUDAH dibangun pemanggil (pola
	 * lama onSearchDefault dengan session thread-local). Criteria yang sama
	 * dipakai dua kali: (1) dihitung totalnya — Order dilepas sementara lewat
	 * CriteriaImpl.orderEntries karena "select count(*) ... order by kolom"
	 * ditolak database, projection direset kembali sesudahnya; (2) ditarik
	 * satu halaman dengan setFirstResult/setMaxResults(pageSize) yang
	 * sekaligus menimpa setMaxResults(MAX_RESULT_100) warisan lama.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> cariDenganCriteria(Criteria criteria, Class<T> kelas) {
		try {
			/* Rantai criteria yang diakhiri createCriteria(asosiasi) mengembalikan
			 * Subcriteria; orderEntries hidup di CriteriaImpl AKAR. Tanpa resolusi
			 * ini Order tidak tercabut dan count query gagal:
			 * "column ... must appear in the GROUP BY clause". */
			Criteria akar = criteria;
			try {
				while (akar instanceof org.hibernate.impl.CriteriaImpl.Subcriteria) {
					akar = ((org.hibernate.impl.CriteriaImpl.Subcriteria) akar).getParent();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:212");
			}

			List simpanOrder = null;
			java.lang.reflect.Field fieldOrder = null;
			try {
				fieldOrder = org.hibernate.impl.CriteriaImpl.class.getDeclaredField("orderEntries");
				fieldOrder.setAccessible(true);
				List orderEntries = (List) fieldOrder.get(akar);
				simpanOrder = new ArrayList(orderEntries);
				orderEntries.clear();
			} catch (Exception e) {
				fieldOrder = null;
			}

			/* sqlRestriction("1=1 order by ...") menyisipkan ORDER BY ke dalam
			 * criterionEntries (WHERE) — tidak tercakup oleh strip orderEntries di atas.
			 * PostgreSQL menolak COUNT query jika ada ORDER BY pada kolom di luar GROUP BY.
			 * Strip sementara entry tersebut sebelum count, pulihkan sesudahnya. */
			List simpanOrderByCriterion = null;
			java.lang.reflect.Field fieldCriterionEntries = null;
			try {
				fieldCriterionEntries = org.hibernate.impl.CriteriaImpl.class.getDeclaredField("criterionEntries");
				fieldCriterionEntries.setAccessible(true);
				List criterionEntries = (List) fieldCriterionEntries.get(akar);
				simpanOrderByCriterion = new ArrayList();
				java.util.Iterator it = criterionEntries.iterator();
				while (it.hasNext()) {
					Object entry = it.next();
					if (entry != null && entry.toString().toLowerCase().contains("order by")) {
						simpanOrderByCriterion.add(entry);
						it.remove();
					}
				}
			} catch (Exception e) {
				fieldCriterionEntries = null;
			}

			/* PENTING: projection & eksekusi WAJIB pada criteria AKAR (root), bukan
			 * pada `criteria` yang dikirim pemanggil. Bila rantai criteria diakhiri
			 * createCriteria(asosiasi) — mis. AmbilDataKurikulumBanbox memakai
			 * createCriteria("jurusan", LEFT_JOIN) — maka `criteria` adalah Subcriteria,
			 * sehingga setProjection di sini akan me-resolve "id" ke entitas asosiasi
			 * (jurusan.id), BUKAN entitas akar. Gejala: countDistinct menghitung id
			 * asosiasi (paging menampilkan jumlah halaman) tetapi distinct id yang
			 * ditarik adalah id asosiasi, lalu session.get(kelasAkar, idAsosiasi)
			 * mengembalikan null → grid tampil KOSONG meski paging berisi.
			 * Memakai `akar` membuat "id" kembali ter-resolve ke entitas akar. */

			/* countDistinct("id") menghitung entitas unik, bukan baris SQL —
			 * sehingga LEFT JOIN ke kurikulumPunyaMatakuliah tidak menggandakan
			 * total dan jumlah halaman di paging control tetap akurat. */
			akar.setProjection(Projections.countDistinct("id"));
			Object total = akar.uniqueResult();
			int totalData = total == null ? 0 : ((Number) total).intValue();
			paging.setTotalSize(totalData);

			akar.setProjection(null);
			akar.setResultTransformer(Criteria.ROOT_ENTITY);

			int activePage = paging.getActivePage() < 0 ? 0 : paging.getActivePage();
			if (activePage * pageSize >= totalData) {
				activePage = 0;
				paging.setActivePage(0);
			}
			akar.setFirstResult(activePage * pageSize);
			akar.setMaxResults(pageSize);

			/* Ambil ID dulu dengan DISTINCT agar duplikat baris dari LEFT JOIN
			 * (kurikulumPunyaMatakuliah, dll.) tidak mencuri slot LIMIT — tanpa
			 * DISTINCT satu Perkuliahan dengan 5 entri kurikulum menghabiskan
			 * seluruh kuota LIMIT 5, sehingga halaman tampil kosong.
			 * session.get() dari session yang sama menjaga lazy proxy tetap valid.
			 * ORDER BY dipulihkan SETELAH query ini — bukan sebelumnya — karena
			 * PostgreSQL menolak ORDER BY ekspresi yang tidak ada di SELECT DISTINCT id
			 * (KE-1 s/d KE-7: "ORDER BY expressions must appear in select list"). */
			akar.setProjection(Projections.distinct(Projections.property("id")));
			List ids = akar.list();
			akar.setProjection(null);
			akar.setResultTransformer(Criteria.ROOT_ENTITY);

			if (fieldOrder != null && simpanOrder != null) {
				try {
					((List) fieldOrder.get(akar)).addAll(simpanOrder);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:296");
				}
			}
			if (fieldCriterionEntries != null && simpanOrderByCriterion != null) {
				try {
					((List) fieldCriterionEntries.get(akar)).addAll(simpanOrderByCriterion);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:302");
				}
			}

			System.out.println("[cariDenganCriteria:" + kelas.getSimpleName() + "]"
					+ " total=" + totalData
					+ " activePage=" + activePage
					+ " pageSize=" + pageSize
					+ " idsCount=" + ids.size());

			org.hibernate.Session loadSession = null;
			try {
				loadSession = (org.hibernate.Session)
						((org.hibernate.impl.CriteriaImpl) akar).getSession();
			} catch (Exception ex) {
				loadSession = ais.database.hibernate.HibernateUtil.currentSession();
			}

			List<T> data = new ArrayList<T>();
			for (Object id : ids) {
				if (id == null) continue;
				try {
					T entity = (T) loadSession.get(kelas, (java.io.Serializable) id);
					if (entity != null && !data.contains(entity)) {
						data.add(entity);
					}
				} catch (Exception ex) {
					System.out.println("[cariDenganCriteria:" + kelas.getSimpleName()
							+ "] gagal load id=" + id + " err=" + ex.getMessage());
				}
			}
			System.out.println("[cariDenganCriteria:" + kelas.getSimpleName() + "]"
					+ " loadedCount=" + data.size());
			return data;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<T>();
		}
	}

	/**
	 * Sama seperti {@link #cariDenganCriteria}, TAPI halaman diurutkan SERVER-SIDE pada
	 * {@code sortField} (asc/desc). cariDenganCriteria biasa menarik id via SELECT DISTINCT id
	 * TANPA order (PostgreSQL menolak ORDER BY kolom di luar SELECT DISTINCT) → sort hilang.
	 * Di sini {@code sortField} IKUT di projection distinct sehingga ORDER BY valid.
	 * sortField null/kosong → delegasi cariDenganCriteria (perilaku lama). Error → fallback.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> cariDenganCriteriaUrut(Criteria criteria, Class<T> kelas, String sortField, boolean sortAsc) {
		if (sortField == null || sortField.trim().length() == 0) {
			return cariDenganCriteria(criteria, kelas);
		}
		try {
			Criteria akar = criteria;
			try {
				while (akar instanceof org.hibernate.impl.CriteriaImpl.Subcriteria) {
					akar = ((org.hibernate.impl.CriteriaImpl.Subcriteria) akar).getParent();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:360");
			}
			List simpanOrder = null;
			java.lang.reflect.Field fieldOrder = null;
			try {
				fieldOrder = org.hibernate.impl.CriteriaImpl.class.getDeclaredField("orderEntries");
				fieldOrder.setAccessible(true);
				List orderEntries = (List) fieldOrder.get(akar);
				simpanOrder = new ArrayList(orderEntries);
				orderEntries.clear();
			} catch (Exception e) {
				fieldOrder = null;
			}
			List simpanOrderByCriterion = null;
			java.lang.reflect.Field fieldCriterionEntries = null;
			try {
				fieldCriterionEntries = org.hibernate.impl.CriteriaImpl.class.getDeclaredField("criterionEntries");
				fieldCriterionEntries.setAccessible(true);
				List criterionEntries = (List) fieldCriterionEntries.get(akar);
				simpanOrderByCriterion = new ArrayList();
				java.util.Iterator it = criterionEntries.iterator();
				while (it.hasNext()) {
					Object entry = it.next();
					if (entry != null && entry.toString().toLowerCase().contains("order by")) {
						simpanOrderByCriterion.add(entry);
						it.remove();
					}
				}
			} catch (Exception e) {
				fieldCriterionEntries = null;
			}
			akar.setProjection(Projections.countDistinct("id"));
			Object total = akar.uniqueResult();
			int totalData = total == null ? 0 : ((Number) total).intValue();
			paging.setTotalSize(totalData);
			akar.setProjection(null);
			akar.setResultTransformer(Criteria.ROOT_ENTITY);
			int activePage = paging.getActivePage() < 0 ? 0 : paging.getActivePage();
			if (activePage * pageSize >= totalData) {
				activePage = 0;
				paging.setActivePage(0);
			}
			akar.setFirstResult(activePage * pageSize);
			akar.setMaxResults(pageSize);
			List ids = new ArrayList();
			boolean sortBerhasil = false;
			try {
				akar.setProjection(Projections.distinct(Projections.projectionList()
						.add(Projections.property("id")).add(Projections.property(sortField))));
				akar.addOrder(sortAsc ? org.hibernate.criterion.Order.asc(sortField)
						: org.hibernate.criterion.Order.desc(sortField));
				List rows = akar.list();
				for (Object row : rows) {
					Object id = (row instanceof Object[]) ? ((Object[]) row)[0] : row;
					if (id != null) {
						ids.add(id);
					}
				}
				sortBerhasil = true;
			} catch (Exception exSort) {
				System.out.println("[cariDenganCriteriaUrut:" + kelas.getSimpleName() + "] sort gagal field="
						+ sortField + " err=" + exSort.getMessage());
			}
			if (fieldOrder != null) {
				try {
					((List) fieldOrder.get(akar)).clear();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:426");
				}
			}
			if (!sortBerhasil) {
				akar.setProjection(Projections.distinct(Projections.property("id")));
				ids = akar.list();
			}
			akar.setProjection(null);
			akar.setResultTransformer(Criteria.ROOT_ENTITY);
			if (fieldOrder != null && simpanOrder != null) {
				try {
					((List) fieldOrder.get(akar)).addAll(simpanOrder);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:438");
				}
			}
			if (fieldCriterionEntries != null && simpanOrderByCriterion != null) {
				try {
					((List) fieldCriterionEntries.get(akar)).addAll(simpanOrderByCriterion);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:444");
				}
			}
			org.hibernate.Session loadSession = null;
			try {
				loadSession = (org.hibernate.Session) ((org.hibernate.impl.CriteriaImpl) akar).getSession();
			} catch (Exception ex) {
				loadSession = ais.database.hibernate.HibernateUtil.currentSession();
			}
			List<T> data = new ArrayList<T>();
			for (Object id : ids) {
				if (id == null) {
					continue;
				}
				try {
					T entity = (T) loadSession.get(kelas, (java.io.Serializable) id);
					if (entity != null && !data.contains(entity)) {
						data.add(entity);
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:463");
				}
			}
			return data;
		} catch (Throwable t) {
			Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
			return cariDenganCriteria(criteria, kelas);
		}
	}

	/**
	 * Hitung total data, sinkronkan komponen paging, lalu tarik satu halaman.
	 * Session dibuka di sini dan SELALU ditutup di finally.
	 */
	public <T> List<T> cari(CriteriaFactory factory, Class<T> kelas, Inisialisasi<T> inisialisasi) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			Criteria countCriteria = factory.initCriteria(session, false);
			countCriteria.setProjection(Projections.rowCount());
			Object total = countCriteria.uniqueResult();
			int totalData = total == null ? 0 : ((Number) total).intValue();
			paging.setTotalSize(totalData);

			int activePage = paging.getActivePage() < 0 ? 0 : paging.getActivePage();
			if (activePage * pageSize >= totalData) {
				/* Filter baru bisa membuat halaman aktif melebihi total. */
				activePage = 0;
				paging.setActivePage(0);
			}

			List<T> data = ConstantValues.simpleList(factory.initCriteria(session, true)
					.setFirstResult(activePage * pageSize).setMaxResults(pageSize), kelas);

			if (inisialisasi != null && data != null) {
				for (T t : data) {
					if (t == null) {
						continue;
					}
					try {
						inisialisasi.init(t);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataPagingHelper.java:505");
					}
				}
			}
			return data == null ? new ArrayList<T>() : data;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<T>();
		} finally {
			/* Akhiri transaksi implisit SEBELUM close, lihat tutupSessionQuietly. */
			tutupSessionQuietly(session);
		}
	}
}
