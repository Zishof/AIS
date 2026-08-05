package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
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
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Rekap nilai ujian per pertemuan (satu blok untuk tiap pertemuan), bisa dibatasi pada satu
 * mahasiswa/calon tertentu.
 *
 * Ditampilkan sebagai grid/tabel ZK + grafik HTML/CSS (lihat {@link RekapNilaiView}); file Excel
 * asli baru dibuat saat tombol "Download Data" ditekan.
 */
public class RekapHasilUjian extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private Pertemuan[] pertemuans;

	private boolean simple;

	private Mahasiswa mahasiswa = null;
	private BiodataCalonMahasiswa biodataCalonMahasiswa = null;

	public RekapHasilUjian(Pertemuan... pertemuans) {
		this(null, null, pertemuans);
	}

	public RekapHasilUjian(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, Pertemuan... pertemuans) {
		this(false, mahasiswa, biodataCalonMahasiswa, pertemuans);
	}

	public RekapHasilUjian(boolean simple, Pertemuan... pertemuans) {
		this(simple, null, null, pertemuans);
	}

	public RekapHasilUjian(boolean simple, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			Pertemuan... pertemuans) {
		super();
		this.simple = simple;
		this.pertemuans = pertemuans;
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

		// 1) Susun semua blok rekap (satu blok per pertemuan).
		final List<RekapNilaiView.Section> sections = bangunSections();

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

	/** Bentuk satu Section per pertemuan aktif yang memiliki peserta dan komponen ujian. */
	@SuppressWarnings("unchecked")
	private List<RekapNilaiView.Section> bangunSections() throws Exception {
		List<RekapNilaiView.Section> sections = new ArrayList<RekapNilaiView.Section>();
		if (pertemuans == null || pertemuans.length == 0) {
			return sections;
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		// Sesi DEDIKASI (openSession), bukan currentSession() yg koneksinya bisa sudah ditutup
		// c3p0 ("This connection has been closed") saat dipakai ulang lintas operasi. Ditutup di
		// finally (clear/disconnect/close); semua data sudah dimaterialisasi ke DTO Section.
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {

		for (Pertemuan pertemuan : pertemuans) {
			if (pertemuan == null || !pertemuan.getAktif()) {
				continue;
			}

			List<GeneralValueObject> hasilUjianMahasiswas = pertemuans[0].getJadwalUjianPMB() != null
					? session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
							.setProjection(Projections.groupProperty("biodataCalonMahasiswa"))
							.createAlias("pertemuanPunyaUjian", "pertemuanPunyaUjian")
							.add(Restrictions.eq("pertemuanPunyaUjian.pertemuan", pertemuan)).list()
					: session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
							.setProjection(Projections.groupProperty("mahasiswa"))
							.createAlias("pertemuanPunyaUjian", "pertemuanPunyaUjian")
							.add(Restrictions.eq("pertemuanPunyaUjian.pertemuan", pertemuan)).list();
			if (hasilUjianMahasiswas.isEmpty()) {
				continue;
			}

			Collection<PertemuanPunyaUjian> pertemuanPunyaUjians = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser)
					.values();
			if (pertemuanPunyaUjians.isEmpty()) {
				continue;
			}

			List<String> judul = new ArrayList<String>();
			for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
				judul.add(RekapUjianSupport.judulKolom(ppu));
			}

			Collections.sort(hasilUjianMahasiswas);

			List<RekapNilaiView.Peserta> peserta = new ArrayList<RekapNilaiView.Peserta>();
			for (GeneralValueObject valueObject : hasilUjianMahasiswas) {
				if (valueObject instanceof Mahasiswa) {
					Mahasiswa m = (Mahasiswa) valueObject;
					if (mahasiswa != null && !mahasiswa.getId().equals(m.getId())) {
						continue;
					}
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(m);
					RekapNilaiView.Peserta p = new RekapNilaiView.Peserta(m.getNim(), m.getNama(),
							m.getJurusan() == null ? "" : m.getJurusan().getNama(),
							krsMahasiswa == null ? "" : krsMahasiswa.getKelas());
					for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
						boolean ikut = !ppu.getMhsYgTidakIkut().contains("," + m.getId() + ",");
						Double nilai = ikut ? RekapUjianSupport.ambilNilai(session, ppu, m, null) : null;
						p.tambah(RekapUjianSupport.sel(ikut, nilai));
					}
					peserta.add(p);
				} else if (valueObject instanceof BiodataCalonMahasiswa) {
					BiodataCalonMahasiswa cal = (BiodataCalonMahasiswa) valueObject;
					if (biodataCalonMahasiswa != null && !biodataCalonMahasiswa.getId().equals(cal.getId())) {
						continue;
					}
					RekapNilaiView.Peserta p = new RekapNilaiView.Peserta(cal.getNoRegistrasi() + "/" + cal.getNoUjian(),
							cal.getNama(), RekapNilaiView.prodiPilihan(cal),
							cal.getStatusAwalMahasiswa() == null ? "" : cal.getStatusAwalMahasiswa().getNama());
					for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
						Double nilai = RekapUjianSupport.ambilNilai(session, ppu, null, cal);
						p.tambah(RekapUjianSupport.sel(true, nilai));
					}
					peserta.add(p);
				}
			}

			if (!peserta.isEmpty()) {
				sections.add(new RekapNilaiView.Section(
						pertemuan.getTopik() == null ? "Pertemuan" : pertemuan.getTopik().toUpperCase(), judul, peserta));
			}
		}
		return sections;
		} finally {
			try { session.clear(); } catch (Exception ignoreA) { ais.common.ErrorAuditUtil.record(ignoreA, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilUjian.java:227");}
			try { session.disconnect(); } catch (Exception ignoreB) { ais.common.ErrorAuditUtil.record(ignoreB, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilUjian.java:228");}
			try { session.close(); } catch (Exception ignoreC) { ais.common.ErrorAuditUtil.record(ignoreC, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilUjian.java:229");}
		}
	}
}
