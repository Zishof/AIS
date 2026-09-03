package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;

/**
 * Analisis read-only dan explainable atas ringkasan KRS. Pengambilan rincian sengaja dilakukan
 * hanya saat popup diminta; renderer daftar tetap memakai ringkasan ringan yang sudah ada.
 */
public final class KrsMahasiswaAnalisisHelper {

	private KrsMahasiswaAnalisisHelper() {
	}

	public static AnalisisKrs analisis(Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa,
			boolean remedial) {
		AnalisisKrs hasil = new AnalisisKrs();
		hasil.mahasiswa = mahasiswa;
		hasil.krsMahasiswa = krsMahasiswa;
		hasil.remedial = remedial;
		if (mahasiswa == null || krsMahasiswa == null || krsMahasiswa.getSemester() == null) {
			hasil.temuan.add("Konteks mahasiswa atau KRS belum lengkap sehingga rincian belum dapat dianalisis.");
			hasil.rekomendasi.add("Muat ulang halaman dan pastikan periode KRS sudah dipilih.");
			return hasil;
		}

		hasil.ringkasan = KrsDetailHelper.rubahKeteranganPengambilanKRS(krsMahasiswa, remedial);
		List<Long> ids = KrsDetailHelper.ambilDetailperkuliahan(mahasiswa,
				krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(),
				krsMahasiswa.getSemesterPendek(), remedial, null, null, false, false, false);
		for (Long id : ids) {
			Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(
					Detailperkuliahan.class, String.valueOf(id));
			if (detail == null) continue;
			ItemKrs item = buatItem(detail);
			hasil.items.add(item);
			hasil.totalSks += item.sks;
			if (item.disetujui) {
				hasil.disetujui++;
				hasil.sksDisetujui += item.sks;
				if (item.dinilai) hasil.dinilai++;
				else hasil.belumDinilai++;
			} else if (item.menungguPersetujuan) {
				hasil.menungguPersetujuan++;
				hasil.sksMenunggu += item.sks;
			} else {
				hasil.statusLain++;
				hasil.sksStatusLain += item.sks;
			}
		}
		hasil.rekapSksSemester = krsMahasiswa.getSksYangDiambil();
		hasil.selisihSks = hasil.totalSks - hasil.rekapSksSemester;
		hasil.susunKesimpulan();
		Collections.sort(hasil.items, new Comparator<ItemKrs>() {
			@Override
			public int compare(ItemKrs kiri, ItemKrs kanan) {
				int prioritasKiri = prioritas(kiri);
				int prioritasKanan = prioritas(kanan);
				if (prioritasKiri != prioritasKanan) return prioritasKiri - prioritasKanan;
				return (kiri.kode + kiri.nama).compareToIgnoreCase(kanan.kode + kanan.nama);
			}

			private int prioritas(ItemKrs item) {
				if (item.menungguPersetujuan || (!item.disetujui && !item.menungguPersetujuan)) return 0;
				return item.dinilai ? 2 : 1;
			}
		});
		return hasil;
	}

	private static ItemKrs buatItem(Detailperkuliahan detail) {
		ItemKrs item = new ItemKrs();
		item.id = detail.getId();
		Matakuliah matakuliah = null;
		Perkuliahan perkuliahan = detail.getPerkuliahan();
		if (perkuliahan != null) matakuliah = perkuliahan.getMatakuliah();
		if (matakuliah == null) matakuliah = detail.getMatakuliahKonversi();
		item.kode = matakuliah == null ? "-" : aman(matakuliah.getKode());
		item.nama = matakuliah == null ? aman(detail.getNama()) : aman(matakuliah.getNama());
		item.sks = matakuliah == null || matakuliah.getSks() == null ? 0 : matakuliah.getSks();
		item.disetujui = Detailperkuliahan.DISETUJUI.equals(detail.getPersetujuan());
		item.menungguPersetujuan = Detailperkuliahan.BELUM_DISETUJUI.equals(detail.getPersetujuan());
		item.statusPersetujuan = item.disetujui ? "Disetujui"
				: (item.menungguPersetujuan ? "Belum disetujui" : "Status belum dikenali");
		item.nilai = detail.getTotalNilai() == null ? 0.0 : detail.getTotalNilai();
		item.dinilai = item.disetujui && item.nilai >= 0.1;
		return item;
	}

	private static String aman(String value) {
		return value == null || value.trim().isEmpty() ? "-" : value.trim();
	}

	public static final class AnalisisKrs {
		private Mahasiswa mahasiswa;
		private KrsMahasiswa krsMahasiswa;
		private boolean remedial;
		private String ringkasan = "";
		private int disetujui;
		private int menungguPersetujuan;
		private int statusLain;
		private int dinilai;
		private int belumDinilai;
		private int totalSks;
		private int sksDisetujui;
		private int sksMenunggu;
		private int sksStatusLain;
		private int rekapSksSemester;
		private int selisihSks;
		private int skorKesiapan;
		private String prioritas = "PERLU VERIFIKASI";
		private String arahKeputusan = "Periksa konteks KRS sebelum mengambil keputusan lanjutan.";
		private String kesimpulan = "Data KRS belum tersedia.";
		private final List<ItemKrs> items = new ArrayList<ItemKrs>();
		private final List<String> temuan = new ArrayList<String>();
		private final List<String> rekomendasi = new ArrayList<String>();

		private void susunKesimpulan() {
			int total = getTotalMatakuliah();
			int skorPersetujuan = total == 0 ? 0 : (int) Math.round(disetujui * 50.0 / total);
			int skorPenilaian = disetujui == 0 ? 0 : (int) Math.round(dinilai * 50.0 / disetujui);
			skorKesiapan = Math.min(100, skorPersetujuan + skorPenilaian);
			if (total == 0) {
				kesimpulan = "Mahasiswa belum mempunyai mata kuliah pada konteks KRS ini.";
				prioritas = "PERLU VERIFIKASI";
				arahKeputusan = "Verifikasi periode dan pembentukan KRS; jangan menyimpulkan mahasiswa tidak aktif hanya dari KRS kosong.";
				temuan.add("Belum ada baris mata kuliah yang dapat diperiksa.");
				rekomendasi.add("Pastikan semester, tahap, jenis semester, dan pilihan remedial sudah sesuai.");
				return;
			}
			if (menungguPersetujuan > 0) {
				kesimpulan = menungguPersetujuan + " dari " + total
						+ " mata kuliah masih menunggu persetujuan.";
				prioritas = "TINGGI";
				arahKeputusan = "Tuntaskan persetujuan Dosen PA sebelum finalisasi KRS atau evaluasi progres nilai.";
				temuan.add("Proses persetujuan KRS belum tuntas; mata kuliah yang menunggu belum masuk cakupan penilaian.");
				rekomendasi.add("Dosen PA perlu memeriksa dan menetapkan persetujuan mata kuliah yang masih tertunda.");
			} else {
				kesimpulan = "Seluruh mata kuliah yang dikenali sudah melewati tahap persetujuan.";
				prioritas = "RENDAH";
				arahKeputusan = "Lanjutkan pemantauan penilaian dan kecocokan rekap SKS.";
			}
			if (disetujui > 0 && belumDinilai == disetujui) {
				if (!"TINGGI".equals(prioritas)) prioritas = "SEDANG";
				arahKeputusan = "Pastikan periode penilaian; tindak lanjuti dosen bila batas input nilai sudah lewat.";
				temuan.add("Semua mata kuliah yang disetujui belum mempunyai nilai final.");
				rekomendasi.add("Periksa jadwal penilaian; bila masa penilaian telah selesai, konfirmasi input nilai dosen.");
			} else if (belumDinilai > 0) {
				if (!"TINGGI".equals(prioritas)) prioritas = "SEDANG";
				arahKeputusan = "Fokus pada mata kuliah belum dinilai yang ditempatkan di urutan teratas tabel.";
				temuan.add(belumDinilai + " mata kuliah yang disetujui masih belum dinilai.");
				rekomendasi.add("Prioritaskan pemeriksaan mata kuliah belum dinilai agar progres akademik lengkap.");
			} else if (dinilai > 0) {
				temuan.add("Seluruh mata kuliah yang disetujui sudah mempunyai nilai.");
			}
			if (statusLain > 0) {
				prioritas = "TINGGI";
				arahKeputusan = "Tahan finalisasi dan periksa status persetujuan nonstandar bersama administrator.";
				temuan.add(statusLain + " mata kuliah mempunyai status persetujuan di luar nilai standar 0/1.");
				rekomendasi.add("Administrator perlu memeriksa status persetujuan nonstandar tersebut.");
			}
			if (selisihSks != 0) {
				if ("RENDAH".equals(prioritas)) prioritas = "SEDANG";
				temuan.add("SKS hasil rincian berbeda " + Math.abs(selisihSks)
						+ " SKS dari rekap semester (rincian " + totalSks + ", rekap " + rekapSksSemester + ").");
				rekomendasi.add("Periksa mata kuliah konversi/remedial dan jalankan sinkronisasi KRS bila perbedaan tidak diharapkan.");
			}
			if (rekomendasi.isEmpty()) {
				rekomendasi.add("Tidak ada tindak lanjut mendesak; pertahankan konsistensi persetujuan dan penilaian.");
			}
		}

		public Mahasiswa getMahasiswa() { return mahasiswa; }
		public KrsMahasiswa getKrsMahasiswa() { return krsMahasiswa; }
		public boolean isRemedial() { return remedial; }
		public String getRingkasan() { return ringkasan; }
		public int getTotalMatakuliah() { return items.size(); }
		public int getDisetujui() { return disetujui; }
		public int getMenungguPersetujuan() { return menungguPersetujuan; }
		public int getStatusLain() { return statusLain; }
		public int getDinilai() { return dinilai; }
		public int getBelumDinilai() { return belumDinilai; }
		public int getTotalSks() { return totalSks; }
		public int getSksDisetujui() { return sksDisetujui; }
		public int getSksMenunggu() { return sksMenunggu; }
		public int getSksStatusLain() { return sksStatusLain; }
		public int getRekapSksSemester() { return rekapSksSemester; }
		public int getSelisihSks() { return selisihSks; }
		public boolean isSksKonsisten() { return selisihSks == 0; }
		public int getSkorKesiapan() { return skorKesiapan; }
		public String getPrioritas() { return prioritas; }
		public String getArahKeputusan() { return arahKeputusan; }
		public String getKesimpulan() { return kesimpulan; }
		public List<ItemKrs> getItems() { return Collections.unmodifiableList(items); }
		public List<String> getTemuan() { return Collections.unmodifiableList(temuan); }
		public List<String> getRekomendasi() { return Collections.unmodifiableList(rekomendasi); }
	}

	public static final class ItemKrs {
		private Long id;
		private String kode;
		private String nama;
		private int sks;
		private boolean disetujui;
		private boolean menungguPersetujuan;
		private String statusPersetujuan;
		private boolean dinilai;
		private double nilai;

		public Long getId() { return id; }
		public String getKode() { return kode; }
		public String getNama() { return nama; }
		public int getSks() { return sks; }
		public boolean isDisetujui() { return disetujui; }
		public boolean isMenungguPersetujuan() { return menungguPersetujuan; }
		public String getStatusPersetujuan() { return statusPersetujuan; }
		public boolean isDinilai() { return dinilai; }
		public double getNilai() { return nilai; }
	}
}
