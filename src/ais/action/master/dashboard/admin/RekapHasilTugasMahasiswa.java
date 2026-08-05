package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Rekap nilai semua tugas untuk satu mahasiswa (atau calon mahasiswa) di seluruh pertemuannya.
 *
 * Ditampilkan sebagai grid/tabel ZK + grafik HTML/CSS (lihat {@link RekapNilaiView}); karena hanya
 * satu orang, grafik berbentuk jaring laba-laba (spider) nilai per tugas. File Excel asli baru
 * dibuat saat tombol "Download Data" ditekan.
 */
public class RekapHasilTugasMahasiswa extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private Mahasiswa mahasiswa;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private boolean simple;

	private VOPembelajaran voPembelajaran;

	public RekapHasilTugasMahasiswa(boolean simple, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			VOPembelajaran voPembelajaran) {
		super();
		this.simple = simple;
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.voPembelajaran = voPembelajaran;
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

		if (mahasiswa == null && biodataCalonMahasiswa == null) {
			return;
		}

		// 1) Susun data rekap satu mahasiswa ke dalam model.
		String namaPeserta = mahasiswa != null ? mahasiswa.getNama()
				: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "";
		// KE-10: voPembelajaran.infoSimple() menyentuh proxy Perkuliahan yang bisa DETACHED
		// (LazyInitializationException: could not initialize proxy - no Session) karena VO dibawa
		// lintas-request. Guard: bila gagal, judul cukup tanpa konteks perkuliahan (tidak mematikan dasbor).
		String infoPembelajaran = "";
		if (voPembelajaran != null) {
			try {
				infoPembelajaran = " pada \"" + voPembelajaran.infoSimple() + "\"";
			} catch (Exception eLazy) {
				infoPembelajaran = "";
			}
		}
		final String judulAtas = "Rekap Tugas " + namaPeserta + infoPembelajaran;
		final List<String> judul = new ArrayList<String>();
		final List<RekapNilaiView.Peserta> peserta = new ArrayList<RekapNilaiView.Peserta>();
		bangunModel(judul, peserta);

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
					File file = RekapNilaiView.writeExcel(judulAtas, judul, peserta);
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap Tugas.xlsx");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
		print.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// 3) Isi: grafik ringkas + tabel rinci (responsif HP & desktop).
		Vbox wrap = new Vbox();
		wrap.setWidth("100%");
		wrap.setStyle("padding:12px;box-sizing:border-box;overflow:auto;");
		wrap.setParent(center);

		wrap.appendChild(RekapNilaiView.dashboard(judulAtas, judul, peserta));

		wrap.appendChild(ais.ui.util.DashboardUiKit.html(
				"<div style='font-size:12px;font-weight:800;color:#0f172a;margin:14px 0 6px;'>Data Rinci per Tugas</div>"
						+ "<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Daftar lengkap nilai untuk setiap tugas. Geser ke samping bila kolom tugas banyak.</div>"));

		Grid grid = RekapNilaiView.tableGrid(judul, peserta);
		grid.setParent(wrap);
	}

	/** Susun daftar judul tugas dan satu baris peserta beserta nilainya. */
	private void bangunModel(List<String> judul, List<RekapNilaiView.Peserta> peserta) throws Exception {

		TreeMap<String, Long> pertemuans = null;
		if (voPembelajaran != null) {
			pertemuans = voPembelajaran.ambilPertemuan();
		} else if (mahasiswa != null) {
			Session session = HibernateUtil.currentSession();
			pertemuans = mahasiswa.ambilPertemuan(session);
		}
		if (pertemuans == null || pertemuans.isEmpty()) {
			return;
		}

		List<Tugas> tugasAll = new ArrayList<Tugas>();
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (!pertemuan.getJudultugas().isEmpty()) {
				tugasAll.add(pertemuan);
			}
			Collection<TugasPertemuan> tugases = pertemuan.ambilTugasPertemuanTotal().values();
			for (TugasPertemuan tugasPertemuan : tugases) {
				if (!tugasPertemuan.getJudultugas().isEmpty()) {
					tugasAll.add(tugasPertemuan);
				}
			}
		}

		Map<Long, TreeMap<Long, TugasFileContent>> nilais = new HashMap<Long, TreeMap<Long, TugasFileContent>>();
		for (Tugas tugas : tugasAll) {
			nilais.put(tugas.getId(), tugas.ambilTugasFileContentTotal());
			judul.add(tugas.getJudultugas());
		}

		if (mahasiswa != null) {
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
			RekapNilaiView.Peserta p = new RekapNilaiView.Peserta(mahasiswa.getNim(), mahasiswa.getNama(),
					mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama(),
					krsMahasiswa == null ? "" : krsMahasiswa.getKelas());
			RekapNilaiView.isiNilai(p, mahasiswa.getId(), mahasiswa, null, tugasAll, nilais);
			peserta.add(p);
		} else if (biodataCalonMahasiswa != null) {
			RekapNilaiView.Peserta p = new RekapNilaiView.Peserta(
					biodataCalonMahasiswa.getNoRegistrasi() + "/" + biodataCalonMahasiswa.getNoUjian(),
					biodataCalonMahasiswa.getNama(), RekapNilaiView.prodiPilihan(biodataCalonMahasiswa),
					biodataCalonMahasiswa.getStatusAwalMahasiswa() == null ? ""
							: biodataCalonMahasiswa.getStatusAwalMahasiswa().getNama());
			RekapNilaiView.isiNilai(p, biodataCalonMahasiswa.getId(), null, biodataCalonMahasiswa, tugasAll, nilais);
			peserta.add(p);
		}
	}
}
