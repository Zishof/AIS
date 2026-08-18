package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AbsensiHelper;
import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Rekap nilai semua tugas untuk seluruh peserta pada satu mata kuliah / kelas.
 *
 * Ditampilkan sebagai grid/tabel ZK + grafik HTML/CSS (lihat {@link RekapNilaiView}); file Excel
 * asli baru dibuat saat tombol "Download Data" ditekan.
 */
public class RekapHasilTugasPerVoPertemuan extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private boolean simple;

	private VOPembelajaran voPembelajaran;

	public RekapHasilTugasPerVoPertemuan(boolean simple, VOPembelajaran voPembelajaran) {
		super();
		this.simple = simple;
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

		// 1) Susun data rekap satu kali ke dalam model yang ringkas.
		final String judulAtas = "Semua Tugas \"" + (voPembelajaran == null ? "" : voPembelajaran.infoSimple()) + "\"";
		final List<String> judul = new ArrayList<String>();
		final List<RekapNilaiView.Peserta> peserta = new ArrayList<RekapNilaiView.Peserta>();
		// Bangun model di blok terlindung: bila ada satu data rusak (pertemuan/mahasiswa/KRS), kerangka
		// + grafik + tabel TETAP tampil dengan data yang berhasil dikumpulkan — bukan panel kosong total.
		try {
			bangunModel(judul, peserta);
		} catch (Throwable t) {
			Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
		}

		// 2) Kerangka tampilan.
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		// Borderlayout WAJIB bertinggi pasti agar ter-render. Saat ditanam di
		// tabpanel "Nilai" (window di-set 500px oleh pemanggil), tanpa tinggi ini
		// borderlayout collapse 0px sehingga konten rekap tidak tampil.
		borderlayout.setHeight("100%");
		borderlayout.setWidth("100%");

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
				"<div style='font-size:12px;font-weight:800;color:#0f172a;margin:14px 0 6px;'>Data Rinci per Peserta</div>"
						+ "<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Daftar lengkap nilai tiap peserta untuk setiap tugas. Geser ke samping bila kolom tugas banyak.</div>"));

		Grid grid = RekapNilaiView.tableGrid(judul, peserta);
		grid.setParent(wrap);
	}

	/** Susun daftar judul tugas dan daftar peserta beserta nilainya. */
	private void bangunModel(List<String> judul, List<RekapNilaiView.Peserta> peserta) throws Exception {

		final TreeMap<String, Long> pertemuans = new TreeMap<String, Long>();
		if (voPembelajaran instanceof Perkuliahan) {
			Perkuliahan kul = (Perkuliahan) voPembelajaran;
			Perkuliahan kuliyah = kul.getMerupakan_paralel() && kul.getPerkuliahan_paralel() != null
					? kul.getPerkuliahan_paralel()
					: kul;
			List<Perkuliahan> perkuliahans = kuliyah.ambilParalelPerkuliahan();
			if (!perkuliahans.contains(kuliyah)) {
				perkuliahans.add(kuliyah);
			}
			for (Perkuliahan perkuliahan : perkuliahans) {
				pertemuans.putAll(perkuliahan.ambilPertemuan());
			}
		} else if (voPembelajaran != null) {
			pertemuans.putAll(voPembelajaran.ambilPertemuan());
		}
		if (pertemuans.isEmpty()) {
			return;
		}

		List<Tugas> tugasAll = new ArrayList<Tugas>();
		for (Long pertemuanid : pertemuans.values()) {
			try {
				if (pertemuanid == null) {
					continue;
				}
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan == null) {
					// Pertemuan tak bisa dimuat (terhapus/lazy) — lewati, jangan NPE & gagalkan semua.
					continue;
				}
				if (!pertemuan.getJudultugas().isEmpty()) {
					tugasAll.add(pertemuan);
				}
				Collection<TugasPertemuan> tugases = pertemuan.ambilTugasPertemuanTotal().values();
				for (TugasPertemuan tugasPertemuan : tugases) {
					if (tugasPertemuan != null && !tugasPertemuan.getJudultugas().isEmpty()) {
						tugasAll.add(tugasPertemuan);
					}
				}
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerVoPertemuan.java:190");
				// lewati satu pertemuan bila datanya rusak; pertemuan lain tetap diproses.
			}
		}

		Map<Long, TreeMap<Long, TugasFileContent>> nilais = new HashMap<Long, TreeMap<Long, TugasFileContent>>();
		for (Tugas tugas : tugasAll) {
			try {
				nilais.put(tugas.getId(), tugas.ambilTugasFileContentTotal());
				judul.add(tugas.getJudultugas());
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerVoPertemuan.java:200");
				// lewati satu tugas bila gagal memuat berkas/nilainya; tugas lain tetap muncul.
			}
		}

		// Sumber daftar peserta: pertemuan pertama yang VALID (tidak null). Versi lama langsung
		// memakai elemen pertama tanpa cek null -> NPE bila pertemuan itu gagal dimuat.
		Pertemuan pertemuan = null;
		for (Long pid : pertemuans.values()) {
			if (pid == null) {
				continue;
			}
			try {
				pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pid.toString());
			} catch (Throwable t) {
				pertemuan = null;
			}
			if (pertemuan != null) {
				break;
			}
		}
		if (pertemuan == null) {
			return;
		}

		List<?> hasilUjianMahasiswas;
		if (voPembelajaran instanceof JadwalUjianPMB) {
			hasilUjianMahasiswas = AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan);
		} else {
			hasilUjianMahasiswas = pertemuan.ambilMahasiswa();
		}
		if (hasilUjianMahasiswas == null) {
			return;
		}

		for (Object valueObject : hasilUjianMahasiswas) {
			try {
				if (valueObject instanceof Mahasiswa) {
					Mahasiswa mahasiswa = (Mahasiswa) valueObject;
					// Sinkronisasi KRS (operasi tulis) dibungkus tersendiri: bila gagal untuk SATU
					// mahasiswa, baris tetap ditampilkan tanpa kelas — bukan menggagalkan seluruh rekap.
					String kelas = "";
					try {
						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
						kelas = krsMahasiswa == null ? "" : krsMahasiswa.getKelas();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerVoPertemuan.java:245");
						// abaikan — kelas dikosongkan saja.
					}
					RekapNilaiView.Peserta p = new RekapNilaiView.Peserta(mahasiswa.getNim(), mahasiswa.getNama(),
							mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama(), kelas);
					RekapNilaiView.isiNilai(p, mahasiswa.getId(), mahasiswa, null, tugasAll, nilais);
					peserta.add(p);
				} else if (valueObject instanceof BiodataCalonMahasiswa) {
					BiodataCalonMahasiswa cal = (BiodataCalonMahasiswa) valueObject;
					RekapNilaiView.Peserta p = new RekapNilaiView.Peserta(cal.getNoRegistrasi() + "/" + cal.getNoUjian(),
							cal.getNama(), RekapNilaiView.prodiPilihan(cal),
							cal.getStatusAwalMahasiswa() == null ? "" : cal.getStatusAwalMahasiswa().getNama());
					RekapNilaiView.isiNilai(p, cal.getId(), null, cal, tugasAll, nilais);
					peserta.add(p);
				}
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerVoPertemuan.java:260");
				// lewati satu peserta bila datanya rusak; peserta lain tetap tampil.
			}
		}
	}
}
