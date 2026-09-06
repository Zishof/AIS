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

	/**
	 * Konstruktor privat: kelas ini hanya berisi method statis dan tidak boleh diinstansiasi.
	 */
	private KrsMahasiswaAnalisisHelper() {
	}

	/**
	 * Menyusun analisis eksplainable satu baris {@link KrsMahasiswa} (rekap KRS per semester dari
	 * mahasiswa, bukan pendaftaran per mata kuliah) dengan memuat rincian {@link Detailperkuliahan}
	 * terkait, menghitung ringkasan SKS/status, lalu menyimpulkan prioritas tindak lanjut.
	 *
	 * <p>Pengambilan rincian sengaja dilakukan di sini (bukan saat render daftar) agar daftar KRS
	 * tetap ringan; method ini hanya dipanggil saat popup analisis diminta oleh pengguna.</p>
	 *
	 * @param mahasiswa mahasiswa pemilik KRS yang dianalisis; jika {@code null} analisis dihentikan
	 *        lebih awal dengan temuan "konteks belum lengkap".
	 * @param krsMahasiswa baris rekap KRS semester yang dianalisis; jika {@code null} atau
	 *        semesternya {@code null} analisis dihentikan lebih awal.
	 * @param remedial {@code true} bila konteks pengambilan KRS remedial, diteruskan apa adanya ke
	 *        {@link KrsDetailHelper#rubahKeteranganPengambilanKRS(KrsMahasiswa, boolean)} dan
	 *        {@link KrsDetailHelper#ambilDetailperkuliahan}.
	 * @return hasil analisis {@link AnalisisKrs}; tidak pernah {@code null}. Bila konteks tidak
	 *         lengkap, hasil dikembalikan dengan daftar item kosong dan temuan/rekomendasi generik.
	 */
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

	/**
	 * Membentuk satu {@link ItemKrs} dari sebuah {@link Detailperkuliahan}, mengambil kode/nama/SKS
	 * mata kuliah dari {@link Perkuliahan#getMatakuliah()} atau, bila tidak ada, dari
	 * {@link Detailperkuliahan#getMatakuliahKonversi()} sebagai fallback (mis. konversi nilai luar).
	 *
	 * <p>Catatan: {@link Detailperkuliahan#getPersetujuan()} dinormalisasi oleh getter-nya sendiri
	 * sehingga hanya pernah bernilai {@link Detailperkuliahan#DISETUJUI} atau
	 * {@link Detailperkuliahan#BELUM_DISETUJUI} &mdash; akibatnya {@code item.disetujui} dan
	 * {@code item.menungguPersetujuan} selalu tepat satu yang bernilai {@code true}, dan cabang
	 * "status lain" di {@link #analisis} bersifat defensif (secara praktik tidak tercapai kecuali
	 * normalisasi pada model berubah di kemudian hari).</p>
	 *
	 * @param detail baris {@link Detailperkuliahan} sumber; tidak boleh {@code null}.
	 * @return item ringkasan siap tampil, tidak pernah {@code null}.
	 */
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

	/**
	 * Menormalkan string untuk tampilan: {@code null} atau string kosong/hanya spasi menjadi
	 * {@code "-"}, selain itu dikembalikan hasil {@link String#trim()}.
	 *
	 * @param value nilai mentah, boleh {@code null}.
	 * @return nilai yang aman ditampilkan, tidak pernah {@code null} atau kosong.
	 */
	private static String aman(String value) {
		return value == null || value.trim().isEmpty() ? "-" : value.trim();
	}

	/**
	 * Hasil analisis explainable atas satu baris {@link KrsMahasiswa}: ringkasan jumlah/SKS per
	 * status persetujuan dan penilaian, daftar item mata kuliah, serta kesimpulan berupa prioritas,
	 * arah keputusan, temuan, dan rekomendasi tindak lanjut. Immutable bagi pemanggil di luar paket
	 * ini &mdash; seluruh field hanya diisi oleh {@link KrsMahasiswaAnalisisHelper} dan hanya dibaca
	 * lewat getter.
	 */
	public static final class AnalisisKrs {
		/** Mahasiswa pemilik KRS yang dianalisis; dapat {@code null} bila konteks tidak lengkap. */
		private Mahasiswa mahasiswa;
		/** Baris rekap KRS semester yang dianalisis; dapat {@code null} bila konteks tidak lengkap. */
		private KrsMahasiswa krsMahasiswa;
		/** {@code true} bila konteks pengambilan KRS ini adalah remedial. */
		private boolean remedial;
		/** Teks ringkasan keterangan pengambilan KRS dari {@link KrsDetailHelper}. */
		private String ringkasan = "";
		/** Jumlah mata kuliah dengan status {@link Detailperkuliahan#DISETUJUI}. */
		private int disetujui;
		/** Jumlah mata kuliah dengan status {@link Detailperkuliahan#BELUM_DISETUJUI}. */
		private int menungguPersetujuan;
		/** Jumlah mata kuliah dengan status persetujuan di luar 0/1 (defensif, lihat {@link #buatItem}). */
		private int statusLain;
		/** Jumlah mata kuliah disetujui yang sudah mempunyai nilai final (nilai &gt;= 0.1). */
		private int dinilai;
		/** Jumlah mata kuliah disetujui yang belum mempunyai nilai final. */
		private int belumDinilai;
		/** Total SKS seluruh item hasil rincian {@link Detailperkuliahan}. */
		private int totalSks;
		/** Total SKS mata kuliah berstatus disetujui. */
		private int sksDisetujui;
		/** Total SKS mata kuliah berstatus menunggu persetujuan. */
		private int sksMenunggu;
		/** Total SKS mata kuliah berstatus lain (defensif). */
		private int sksStatusLain;
		/** SKS rekap semester dari {@link KrsMahasiswa#getSksYangDiambil()}, sumber pembanding independen. */
		private int rekapSksSemester;
		/** Selisih {@link #totalSks} dikurangi {@link #rekapSksSemester}; nol berarti konsisten. */
		private int selisihSks;
		/** Skor kesiapan 0-100 gabungan progres persetujuan dan penilaian, lihat {@link #susunKesimpulan()}. */
		private int skorKesiapan;
		/** Label prioritas tindak lanjut: {@code "RENDAH"}, {@code "SEDANG"}, {@code "TINGGI"}, atau {@code "PERLU VERIFIKASI"}. */
		private String prioritas = "PERLU VERIFIKASI";
		/** Kalimat arah keputusan yang disarankan untuk pengguna. */
		private String arahKeputusan = "Periksa konteks KRS sebelum mengambil keputusan lanjutan.";
		/** Kalimat kesimpulan ringkas mengenai kondisi KRS ini. */
		private String kesimpulan = "Data KRS belum tersedia.";
		/** Daftar item mata kuliah hasil rincian, dalam urutan tampil (menunggu/status lain dulu). */
		private final List<ItemKrs> items = new ArrayList<ItemKrs>();
		/** Daftar kalimat temuan yang mendasari kesimpulan. */
		private final List<String> temuan = new ArrayList<String>();
		/** Daftar kalimat rekomendasi tindak lanjut. */
		private final List<String> rekomendasi = new ArrayList<String>();

		/**
		 * Menurunkan {@link #skorKesiapan}, {@link #prioritas}, {@link #arahKeputusan},
		 * {@link #kesimpulan}, {@link #temuan}, dan {@link #rekomendasi} dari akumulasi status yang
		 * sudah dihitung di {@link KrsMahasiswaAnalisisHelper#analisis}. Dipanggil tepat sekali per
		 * analisis, setelah seluruh item selesai diproses.
		 *
		 * <p>Skor kesiapan: 50% dari proporsi mata kuliah yang sudah disetujui, ditambah 50% dari
		 * proporsi mata kuliah disetujui yang sudah dinilai (dibulatkan per komponen, dijumlah, lalu
		 * dibatasi maksimum 100).</p>
		 */
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

		/** @return mahasiswa pemilik KRS yang dianalisis; dapat {@code null} bila konteks tidak lengkap. */
		public Mahasiswa getMahasiswa() { return mahasiswa; }
		/** @return baris rekap KRS semester yang dianalisis; dapat {@code null} bila konteks tidak lengkap. */
		public KrsMahasiswa getKrsMahasiswa() { return krsMahasiswa; }
		/** @return {@code true} bila konteks pengambilan KRS ini adalah remedial. */
		public boolean isRemedial() { return remedial; }
		/** @return teks ringkasan keterangan pengambilan KRS. */
		public String getRingkasan() { return ringkasan; }
		/** @return jumlah total item mata kuliah hasil rincian ({@link #items}.size()). */
		public int getTotalMatakuliah() { return items.size(); }
		/** @return jumlah mata kuliah berstatus disetujui. */
		public int getDisetujui() { return disetujui; }
		/** @return jumlah mata kuliah berstatus menunggu persetujuan. */
		public int getMenungguPersetujuan() { return menungguPersetujuan; }
		/** @return jumlah mata kuliah berstatus persetujuan di luar 0/1 (defensif). */
		public int getStatusLain() { return statusLain; }
		/** @return jumlah mata kuliah disetujui yang sudah dinilai. */
		public int getDinilai() { return dinilai; }
		/** @return jumlah mata kuliah disetujui yang belum dinilai. */
		public int getBelumDinilai() { return belumDinilai; }
		/** @return total SKS seluruh item hasil rincian. */
		public int getTotalSks() { return totalSks; }
		/** @return total SKS mata kuliah berstatus disetujui. */
		public int getSksDisetujui() { return sksDisetujui; }
		/** @return total SKS mata kuliah berstatus menunggu persetujuan. */
		public int getSksMenunggu() { return sksMenunggu; }
		/** @return total SKS mata kuliah berstatus lain (defensif). */
		public int getSksStatusLain() { return sksStatusLain; }
		/** @return SKS rekap semester dari {@link KrsMahasiswa#getSksYangDiambil()}. */
		public int getRekapSksSemester() { return rekapSksSemester; }
		/** @return selisih total SKS rincian dikurangi rekap semester; nol berarti konsisten. */
		public int getSelisihSks() { return selisihSks; }
		/** @return {@code true} bila {@link #getSelisihSks()} bernilai nol (rincian konsisten dengan rekap). */
		public boolean isSksKonsisten() { return selisihSks == 0; }
		/** @return skor kesiapan 0-100 gabungan progres persetujuan dan penilaian. */
		public int getSkorKesiapan() { return skorKesiapan; }
		/** @return label prioritas tindak lanjut. */
		public String getPrioritas() { return prioritas; }
		/** @return kalimat arah keputusan yang disarankan. */
		public String getArahKeputusan() { return arahKeputusan; }
		/** @return kalimat kesimpulan ringkas mengenai kondisi KRS ini. */
		public String getKesimpulan() { return kesimpulan; }
		/** @return daftar item mata kuliah hasil rincian, tidak dapat diubah (unmodifiable). */
		public List<ItemKrs> getItems() { return Collections.unmodifiableList(items); }
		/** @return daftar kalimat temuan yang mendasari kesimpulan, tidak dapat diubah (unmodifiable). */
		public List<String> getTemuan() { return Collections.unmodifiableList(temuan); }
		/** @return daftar kalimat rekomendasi tindak lanjut, tidak dapat diubah (unmodifiable). */
		public List<String> getRekomendasi() { return Collections.unmodifiableList(rekomendasi); }
	}

	/**
	 * Baris tampilan satu mata kuliah dalam popup analisis KRS: identitas mata kuliah, SKS, status
	 * persetujuan, dan status penilaian. Dibentuk oleh {@link KrsMahasiswaAnalisisHelper#buatItem}
	 * dari sebuah {@link Detailperkuliahan}.
	 */
	public static final class ItemKrs {
		/** Id baris {@link Detailperkuliahan} sumber. */
		private Long id;
		/** Kode mata kuliah, atau {@code "-"} bila mata kuliah tidak dikenali. */
		private String kode;
		/** Nama mata kuliah (atau nama bebas dari {@link Detailperkuliahan#getNama()} bila mata kuliah tidak dikenali). */
		private String nama;
		/** SKS mata kuliah; {@code 0} bila mata kuliah atau SKS-nya tidak dikenali. */
		private int sks;
		/** {@code true} bila status persetujuan adalah {@link Detailperkuliahan#DISETUJUI}. */
		private boolean disetujui;
		/** {@code true} bila status persetujuan adalah {@link Detailperkuliahan#BELUM_DISETUJUI}. */
		private boolean menungguPersetujuan;
		/** Label status persetujuan siap tampil: "Disetujui", "Belum disetujui", atau "Status belum dikenali". */
		private String statusPersetujuan;
		/** {@code true} bila disetujui dan mempunyai nilai final &gt;= 0.1. */
		private boolean dinilai;
		/** Nilai final dari {@link Detailperkuliahan#getTotalNilai()}; {@code 0.0} bila belum ada. */
		private double nilai;

		/** @return id baris {@link Detailperkuliahan} sumber. */
		public Long getId() { return id; }
		/** @return kode mata kuliah, atau {@code "-"} bila tidak dikenali. */
		public String getKode() { return kode; }
		/** @return nama mata kuliah siap tampil. */
		public String getNama() { return nama; }
		/** @return SKS mata kuliah. */
		public int getSks() { return sks; }
		/** @return {@code true} bila status persetujuan disetujui. */
		public boolean isDisetujui() { return disetujui; }
		/** @return {@code true} bila status persetujuan menunggu persetujuan. */
		public boolean isMenungguPersetujuan() { return menungguPersetujuan; }
		/** @return label status persetujuan siap tampil. */
		public String getStatusPersetujuan() { return statusPersetujuan; }
		/** @return {@code true} bila mata kuliah ini sudah dinilai. */
		public boolean isDinilai() { return dinilai; }
		/** @return nilai final mata kuliah. */
		public double getNilai() { return nilai; }
	}
}
