package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Rekap nilai ujian satu mahasiswa (atau calon) di seluruh pertemuannya — satu blok per pertemuan.
 *
 * Ditampilkan sebagai grid/tabel ZK + grafik HTML/CSS (lihat {@link RekapNilaiView}); karena hanya
 * satu orang, grafik berbentuk jaring laba-laba (spider) nilai per komponen ujian. File Excel asli
 * baru dibuat saat tombol "Download Data" ditekan.
 */
public class RekapHasilUjianMahasiswa extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private Mahasiswa mahasiswa;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private VOPembelajaran voPembelajaran;

	private boolean simple;

	private List<RekapNilaiView.Section> sections = new ArrayList<RekapNilaiView.Section>();

	public RekapHasilUjianMahasiswa(boolean simple, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			VOPembelajaran voPembelajaran) {
		super();
		this.simple = simple;
		this.voPembelajaran = voPembelajaran;
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		// 1) Susun semua blok rekap (satu blok per pertemuan) untuk peserta ini.
		if (mahasiswa != null || biodataCalonMahasiswa != null) {
			Session session = null;
			try {
				session = openBackgroundSession();
				sections = bangunSections(session);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				closeNativeSessionQuietly(session);
			}
		}

		// 2) Kerangka tampilan.
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		if (!simple) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					detach();
				}
			});
			cancel.setParent(toolbar);
		}

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
		print.setTooltiptext("Unduh rekap dalam format Excel (.xlsx)");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					File file = RekapNilaiView.writeExcelSections(sections);
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap Hasil Ujian.xlsx");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
		print.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// 3) Isi: tiap pertemuan jadi satu blok grafik + tabel (responsif HP & desktop).
		Vbox wrap = new Vbox();
		wrap.setWidth("100%");
		wrap.setStyle("padding:12px;box-sizing:border-box;overflow:auto;");
		wrap.setParent(center);

		RekapNilaiView.renderSections(wrap, sections);
	}

	/** Bentuk satu Section per pertemuan aktif yang punya komponen ujian, untuk peserta ini. */
	private List<RekapNilaiView.Section> bangunSections(Session session) throws Exception {
		List<RekapNilaiView.Section> hasil = new ArrayList<RekapNilaiView.Section>();

		Mahasiswa mahasiswaData = mahasiswa == null ? null
				: (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
		BiodataCalonMahasiswa biodataData = biodataCalonMahasiswa == null ? null
				: (BiodataCalonMahasiswa) session.get(BiodataCalonMahasiswa.class, biodataCalonMahasiswa.getId());
		if (mahasiswaData == null && biodataData == null) {
			return hasil;
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		List<Pertemuan> pertemuans = loadPertemuanList(session, ambilPertemuanIds(voPembelajaran), mahasiswaData,
				biodataData);
		if (pertemuans == null) {
			return hasil;
		}

		for (Pertemuan pertemuan : pertemuans) {
			if (!isAktif(pertemuan)) {
				continue;
			}
			Collection<PertemuanPunyaUjian> pertemuanPunyaUjians = loadPertemuanPunyaUjian(session, pertemuan, tbmuser);
			if (pertemuanPunyaUjians == null || pertemuanPunyaUjians.isEmpty()) {
				continue;
			}

			List<String> judul = new ArrayList<String>();
			for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
				judul.add(RekapUjianSupport.judulKolom(ppu));
			}

			RekapNilaiView.Peserta p;
			if (mahasiswaData != null) {
				KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswaData);
				p = new RekapNilaiView.Peserta(mahasiswaData.getNim(), mahasiswaData.getNama(),
						mahasiswaData.getJurusan() == null ? "" : mahasiswaData.getJurusan().getNama(),
						krs == null ? "" : krs.getKelas());
				for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
					boolean ikut = mahasiswaData.getId() != null
							&& !safeString(ppu.getMhsYgTidakIkut()).contains("," + mahasiswaData.getId() + ",");
					Double nilai = ikut ? RekapUjianSupport.ambilNilai(session, ppu, mahasiswaData, null) : null;
					p.tambah(RekapUjianSupport.sel(ikut, nilai));
				}
			} else {
				p = new RekapNilaiView.Peserta(
						safeString(biodataData.getNoRegistrasi()) + "/" + safeString(biodataData.getNoUjian()),
						biodataData.getNama(), RekapNilaiView.prodiPilihan(biodataData),
						biodataData.getStatusAwalMahasiswa() == null ? ""
								: biodataData.getStatusAwalMahasiswa().getNama());
				for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
					Double nilai = RekapUjianSupport.ambilNilai(session, ppu, null, biodataData);
					p.tambah(RekapUjianSupport.sel(true, nilai));
				}
			}

			List<RekapNilaiView.Peserta> baris = new ArrayList<RekapNilaiView.Peserta>();
			baris.add(p);
			String topik = safeString(pertemuan.getTopik());
			hasil.add(new RekapNilaiView.Section(topik.length() == 0 ? "PERTEMUAN" : topik.toUpperCase(), judul, baris));
		}
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private List<Pertemuan> loadPertemuanList(Session session, List<Long> pertemuanIds, Mahasiswa mahasiswaData,
			BiodataCalonMahasiswa biodataData) {
		List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
		if (pertemuanIds != null && !pertemuanIds.isEmpty()) {
			for (Long pertemuanId : pertemuanIds) {
				if (pertemuanId == null) {
					continue;
				}
				Pertemuan pertemuan = (Pertemuan) session.get(Pertemuan.class, pertemuanId);
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
			return pertemuans;
		}

		Criterion pemilik = null;
		if (mahasiswaData != null) {
			pemilik = Restrictions.eq("mahasiswa", mahasiswaData);
		}
		if (biodataData != null) {
			Criterion bioCrit = Restrictions.eq("biodataCalonMahasiswa", biodataData);
			pemilik = pemilik == null ? bioCrit : Restrictions.or(pemilik, bioCrit);
		}
		if (pemilik == null) {
			return pertemuans;
		}

		Criteria criteria = session.createCriteria(HasilUjianMahasiswa.class).add(pemilik)
				.createAlias("pertemuanPunyaUjian", "pertemuanPunyaUjian")
				.createAlias("pertemuanPunyaUjian.ujian", "ujian").add(Restrictions.eq("ujian.aktif", true))
				.setProjection(Projections.groupProperty("pertemuanPunyaUjian.pertemuan"));
		pertemuans.addAll(criteria.list());
		return pertemuans;
	}

	@SuppressWarnings("unchecked")
	private Collection<PertemuanPunyaUjian> loadPertemuanPunyaUjian(Session session, Pertemuan pertemuan,
			Tbmuser tbmuser) {
		try {
			Collection<PertemuanPunyaUjian> data = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser).values();
			if (data != null) {
				return data;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			return session.createCriteria(PertemuanPunyaUjian.class).createAlias("ujian", "ujian")
					.add(Restrictions.eq("pertemuan", pertemuan)).add(Restrictions.eq("ujian.aktif", true))
					.addOrder(Order.asc("id")).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return new ArrayList<PertemuanPunyaUjian>();
	}

	private List<Long> ambilPertemuanIds(VOPembelajaran voPembelajaran) {
		List<Long> ids = new ArrayList<Long>();
		if (voPembelajaran == null) {
			return ids;
		}
		try {
			TreeMap<String, Long> treeMap = voPembelajaran.ambilPertemuan();
			if (treeMap != null) {
				for (Long pertemuanId : treeMap.values()) {
					if (pertemuanId != null) {
						ids.add(pertemuanId);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return ids;
	}

	private boolean isAktif(Pertemuan pertemuan) {
		try {
			return pertemuan != null && Boolean.TRUE.equals(pertemuan.getAktif());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Sesi DEDICATED dari SessionFactory untuk pembacaan laporan; TIDAK memakai currentNativeSession
	 * yang bisa ditutup proses lain. Ditutup di {@code finally} pemanggil lewat
	 * {@link #closeNativeSessionQuietly(Session)}.
	 */
	private Session openBackgroundSession() throws Exception {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (session == null || !session.isOpen()) {
			session = HibernateUtil.getSessionFactory().openSession();
		}
		if (session == null || !session.isOpen()) {
			throw new IllegalStateException("Session database laporan hasil ujian tidak dapat dibuka.");
		}
		return session;
	}

	private void closeNativeSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilUjianMahasiswa.java:326");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilUjianMahasiswa.java:330");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilUjianMahasiswa.java:334");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private String safeString(Object value) {
		return value == null ? "" : value.toString();
	}
}
