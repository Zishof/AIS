package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ais.database.model.DetailBiaya;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.SettingBiaya;

/**
 * Mesin keputusan bersama untuk menjelaskan hasil audit tagihan pada seluruh keluarga
 * {@code DaftarUlangMahasiswa*Action}.
 *
 * <h3>Mengapa helper ini diperlukan</h3>
 * Halaman pembayaran mahasiswa lama dan mahasiswa baru memakai entitas yang berbeda, tetapi
 * keduanya melewati rantai domain yang sama: data orang, pemilihan {@link SettingBiaya}, item
 * biaya, template {@link DetailBiaya}, billing bulanan bila dipakai, kegiatan pembayaran,
 * cicilan yang sudah committed, lalu baris tagihan pada layar. Audit teknis di masing-masing
 * action tetap dipertahankan karena query dan entitas orangnya memang berbeda. Helper ini
 * berada satu tingkat di atas audit tersebut: fakta-fakta yang sudah ditemukan dinormalisasi
 * menjadi {@link Data}, kemudian dinilai dengan urutan keputusan yang sama agar pengguna tidak
 * menerima penjelasan berbeda untuk keadaan yang identik.
 *
 * <h3>Urutan keputusan</h3>
 * Keputusan sengaja mendahulukan kegagalan paling hulu. Tidak adanya Setting Biaya harus
 * dijelaskan sebelum tidak adanya template atau baris layar, sebab komponen di hilir memang
 * mustahil terbentuk. Setelah sumber konfigurasi terbukti ada, algoritma memeriksa item aktif,
 * mode default/billing, template produksi, dan akhirnya keadaan transaksi. Jika baris tagihan
 * terlihat, nominal serta pembayaran committed dipakai untuk membedakan belum dibayar,
 * pembayaran sebagian, lunas, nominal nol, dan pembayaran yang melampaui nominal. Jika layar
 * kosong tetapi cicilan sudah ada, kondisi tidak langsung dianggap error: tagihan lunas memang
 * dapat disembunyikan dari daftar tunggakan. Keadaan itu diberi arahan untuk memeriksa History.
 *
 * <h3>Batas interpretasi</h3>
 * {@code nilaiDibayarCommitted} harus berasal dari query database, bukan nilai sementara pada
 * komponen input. {@code nominalTagihanTampil} adalah jumlah bruto baris sumber yang sedang
 * dianalisis. Selisih keduanya disebut estimasi sisa karena aturan diskon, denda, pengecualian,
 * atau alokasi lintas item dapat membuat angka transaksi final berbeda. Helper tidak menulis
 * database, tidak membuat tagihan, dan tidak mengubah biodata. Ia hanya membentuk penjelasan
 * deterministik dari fakta yang diberikan action pemanggil.
 *
 * <p>Java 7 compatible; tidak menyimpan state dan aman dipakai ulang per event ZK.</p>
 */
public final class DaftarUlangTagihanAnalisisHelper {

	private static final double EPSILON = 0.01d;

	private DaftarUlangTagihanAnalisisHelper() {
	}

	/** Fakta lintas-action yang diperlukan mesin keputusan. */
	public static final class Data {
		public String identitas = "-";
		public String jenisPembayaran = "-";
		public String statusAkademik = "-";
		public String mode = "-";
		public String kesimpulanTeknis = "";
		public String tindakanTeknis = "";
		public int semester;
		public int settingKhusus;
		public int kandidatSetting;
		public int itemBiayaAktif;
		public int pengaturanBulanan;
		public int templateAkhir;
		public int hasilProduksi;
		public int kegiatan;
		public int cicilan;
		public int barisLayar;
		public int settingDefault;
		public int settingBilling;
		public double nominalTagihanTampil;
		public double nilaiDibayarCommitted;
	}

	/** Hasil semantik yang dapat diuji tanpa bergantung pada komponen ZK. */
	public static final class Hasil {
		private String kode;
		private String tingkat;
		private String judul;
		private String apaYangTerjadi;
		private String dampak;
		private String keyakinan;
		private String warna;
		private String latar;
		private final List<String> alasan = new ArrayList<String>();
		private final List<String> langkah = new ArrayList<String>();
		private final List<String> catatan = new ArrayList<String>();

		public String getKode() { return kode; }
		public String getJudul() { return judul; }
		public String getTingkat() { return tingkat; }
	}

	/**
	 * Menjumlahkan nominal bruto yang menjadi sumber baris layar. Method menerima collection
	 * mentah karena kedua action lama masih memakai {@code ArrayList} tanpa generic.
	 */
	public static double hitungNominalTagihanTampil(Collection<?> dataTagihan) {
		double total = 0.0d;
		if (dataTagihan == null) return total;
		for (Object o : dataTagihan) {
			try {
				if (o instanceof PengaturanPembayaranBulanan) {
					Double nominal = ((PengaturanPembayaranBulanan) o).getNominal();
					total += nominal == null ? 0.0d : nominal.doubleValue();
				} else if (o instanceof DetailBiaya) {
					DetailBiaya detail = (DetailBiaya) o;
					Double nominal = detail.getNilaiBiayaBaru() == null
							? detail.getNilaiBiaya() : detail.getNilaiBiayaBaru();
					total += nominal == null ? 0.0d : nominal.doubleValue();
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"DaftarUlangTagihanAnalisisHelper: hitung nominal sumber layar");
			}
		}
		return total;
	}

	/** Mengisi jumlah Setting Biaya bermode default dan bermode Billing pada data analisis. */
	public static void hitungModeSetting(Data data, Collection<SettingBiaya> settings) {
		if (data == null || settings == null) return;
		for (SettingBiaya setting : settings) {
			if (setting == null) continue;
			if (Boolean.TRUE.equals(setting.getGunakanBiayaDefault())) data.settingDefault++;
			else data.settingBilling++;
		}
	}

	/**
	 * Mengubah fakta audit menjadi diagnosis operasional. Percabangan diurutkan dari sumber
	 * paling hulu menuju transaksi agar satu gejala tidak menghasilkan saran yang saling
	 * bertentangan. Kondisi normal tetap diberi penjelasan, bukan ditampilkan sebagai error.
	 */
	public static Hasil analisis(Data d) {
		Hasil h = new Hasil();
		if (d == null) {
			set(h, "DATA_TIDAK_TERSEDIA", "Perlu diperiksa", "Data analisis belum lengkap",
					"Sistem belum menerima fakta yang cukup untuk menjelaskan tagihan.",
					"Kesimpulan belum dapat dipakai untuk mengambil tindakan.", "#b45309", "#fff7ed");
			h.keyakinan = "Rendah - fakta audit belum tersedia.";
			h.langkah.add("Tutup popup, muat ulang data mahasiswa, lalu jalankan Analisis Data kembali.");
			return h;
		}

		double tagihan = Math.max(0.0d, d.nominalTagihanTampil);
		double dibayar = Math.max(0.0d, d.nilaiDibayarCommitted);
		double sisa = Math.max(0.0d, tagihan - dibayar);
		int sumberSetting = d.kandidatSetting + d.settingKhusus;

		if (sumberSetting <= 0) {
			set(h, "SETTING_TIDAK_COCOK", "Perlu diperbaiki", "Konfigurasi biaya belum cocok",
					"Tidak ada Setting Biaya umum maupun khusus yang lolos seluruh data mahasiswa dan periode yang dipilih.",
					"Item, nominal, dan tagihan tidak dapat dibentuk sebelum sumber konfigurasi tersedia.", "#b91c1c", "#fef2f2");
			h.langkah.add("Buka Setting Biaya dan buat atau duplikasi konfigurasi yang sesuai dengan data pada audit.");
			h.langkah.add("Samakan jenis pembayaran, periode, semester, angkatan, jenjang, status, program, dan prodi; gunakan nilai Semua hanya bila tarif memang berlaku umum.");
		} else if (d.itemBiayaAktif <= 0) {
			set(h, "ITEM_BIAYA_KOSONG", "Perlu diperbaiki", "Setting ditemukan, tetapi item tagihan belum siap",
					"Konfigurasi utama cocok, namun tidak ada Item Biaya aktif yang berlaku untuk semester ini.",
					"Sistem mengetahui kelompok biaya mahasiswa, tetapi belum mempunyai komponen dan nominal untuk ditagihkan.", "#b91c1c", "#fef2f2");
			h.langkah.add("Buka Setting Biaya yang cocok, tambahkan Item Biaya, lalu isi nominalnya.");
			h.langkah.add("Pastikan item aktif dan Min/Max Semester mencakup semester " + d.semester + ".");
		} else if (d.settingBilling > 0 && d.settingDefault == 0 && d.pengaturanBulanan <= 0
				&& d.barisLayar <= 0) {
			set(h, "BILLING_BELUM_DIBUAT", "Perlu diperbaiki", "Rencana Billing belum dibuat",
					"Setting Biaya memakai mode Billing, tetapi belum ditemukan pembagian bulan atau angsuran untuk semester ini.",
					"Item biaya sudah ada, namun belum ada jadwal dan nominal tahap yang dapat ditampilkan sebagai tagihan.", "#b91c1c", "#fef2f2");
			h.langkah.add("Pada baris Setting Biaya, buka Action lalu pilih Buat Billing.");
			h.langkah.add("Isi bulan atau tahap, tanggal jatuh tempo, dan nominal; pastikan total Billing sesuai Item Biaya.");
		} else if (d.templateAkhir <= 0 && d.barisLayar <= 0) {
			set(h, "TEMPLATE_TIDAK_COCOK", "Perlu diperbaiki", "Template tagihan tertahan oleh kriteria lanjutan",
					"Setting dan Item Biaya sudah ditemukan, tetapi tidak ada DetailBiaya yang lolos seluruh kriteria produksi.",
					"Tagihan belum dapat diterbitkan untuk orang dan semester ini.", "#b91c1c", "#fef2f2");
			h.langkah.add("Lihat baris PENYEBAB TERBUKTI pada bukti teknis, lalu perbaiki kriteria template yang disebutkan.");
			h.langkah.add("Periksa terutama status, semester mulai, tahun akademik, prodi, program, kewarganegaraan, kelas, tempat tinggal, dan parameter tambahan.");
		} else if (d.barisLayar > 0 && tagihan <= EPSILON) {
			set(h, "NOMINAL_NOL", "Perlu diperbaiki", "Baris tagihan ada, tetapi nominalnya nol",
					"Sumber tagihan berhasil dibaca, namun jumlah seluruh nominal pada layar adalah nol.",
					"Pembayaran tidak dapat diproses sebagai tagihan bernilai sampai nominal sumber diperbaiki.", "#b91c1c", "#fef2f2");
			h.langkah.add("Periksa nominal Item Biaya default atau nominal setiap tahap Billing sesuai mode sumber.");
			h.langkah.add("Simpan perubahan, klik Refresh, lalu pastikan total tidak lagi nol.");
		} else if (d.barisLayar > 0 && dibayar > tagihan + EPSILON) {
			set(h, "PEMBAYARAN_MELEBIHI_TAGIHAN", "Perlu diperiksa", "Pembayaran lebih besar daripada nominal yang terbaca",
					"Database mencatat pembayaran " + angka(dibayar) + " sementara sumber layar berjumlah " + angka(tagihan) + ".",
					"Ada kemungkinan alokasi pembayaran mencakup item lain, tagihan berubah setelah transaksi, atau terjadi pembayaran berlebih.", "#b45309", "#fff7ed");
			h.langkah.add("Buka History dan cocokkan setiap cicilan dengan Item Biaya, semester, dan jenis pembayaran.");
			h.langkah.add("Jangan menghapus transaksi sebelum sumber selisih dipastikan dan bukti pembayaran diverifikasi.");
		} else if (d.barisLayar > 0 && dibayar + EPSILON >= tagihan) {
			set(h, "LUNAS_MASIH_TAMPIL", "Sudah lunas", "Tagihan telah terbayar penuh",
					"Nominal yang terbaca " + angka(tagihan) + " dan pembayaran committed " + angka(dibayar) + " sudah menutup tagihan.",
					"Tidak ada sisa pembayaran. Baris dapat masih terlihat sampai layar atau rekap dimuat ulang.", "#15803d", "#f0fdf4");
			h.langkah.add("Periksa History untuk memastikan seluruh transaksi benar, kemudian klik Refresh bila baris masih tampil sebagai tunggakan.");
		} else if (d.barisLayar > 0 && dibayar > EPSILON) {
			set(h, "PEMBAYARAN_SEBAGIAN", "Normal", "Tagihan valid dan sedang dicicil",
					"Tagihan sebesar " + angka(tagihan) + " sudah menerima pembayaran committed " + angka(dibayar)
							+ "; estimasi sisa saat ini " + angka(sisa) + ".",
					"Tagihan tetap tampil karena belum lunas. Ini kondisi normal, bukan kegagalan Setting Biaya.", "#15803d", "#f0fdf4");
			h.langkah.add("Lanjutkan pembayaran sebesar sisa yang berlaku, atau sesuai tahap jatuh tempo jika mode tagihan bulanan/angsuran.");
			h.langkah.add("Gunakan History bila perlu memastikan pembayaran sebelumnya sudah masuk ke item yang benar.");
		} else if (d.barisLayar > 0) {
			set(h, "BELUM_DIBAYAR", "Normal", "Tagihan valid dan belum dibayar",
					"Sistem menemukan sumber konfigurasi serta " + d.barisLayar + " baris tagihan dengan total " + angka(tagihan) + ".",
					"Belum ada cicilan committed untuk jenis pembayaran dan semester ini.", "#15803d", "#f0fdf4");
			h.langkah.add("Pilih item yang akan dibayar, masukkan nominal sesuai ketentuan, lalu proses pembayaran.");
		} else if (d.cicilan > 0) {
			set(h, "TAGIHAN_TERBAYAR_TIDAK_TAMPIL", "Kemungkinan normal", "Tagihan tidak tampil karena sudah memiliki pembayaran",
					"Sumber pembayaran mempunyai " + d.cicilan + " transaksi committed senilai " + angka(dibayar)
							+ ", sedangkan daftar tunggakan tidak menampilkan baris.",
					"Tagihan yang sudah lunas dapat disembunyikan. Bila belum lunas, perlu diperiksa alokasi pembayaran atau sumber nominalnya.", "#0369a1", "#eff6ff");
			h.langkah.add("Buka History dan cocokkan total pembayaran dengan nominal tagihan asli.");
			h.langkah.add("Jika masih ada sisa tetapi baris tetap kosong, klik Refresh/Proses Tagihan lalu jalankan analisis kembali.");
		} else if (d.hasilProduksi > 0) {
			set(h, "LAYAR_BELUM_SINKRON", "Perlu dimuat ulang", "Sumber tagihan ditemukan, tetapi layar belum menampilkannya",
					"Query produksi menemukan " + d.hasilProduksi + " sumber tagihan, sementara daftar pada layar masih kosong.",
					"Konfigurasi tidak perlu dibuat ulang. Kemungkinan data layar, filter semester, atau cache belum mengikuti hasil query terbaru.", "#0369a1", "#eff6ff");
			h.langkah.add("Pastikan semester dan jenis pembayaran yang dipilih sama dengan hasil analisis, lalu klik Refresh.");
			h.langkah.add("Jika tetap kosong, jalankan Proses Tagihan dan periksa pengecualian NIM atau filter baris lunas.");
		} else if (d.templateAkhir > 0 && d.hasilProduksi <= 0) {
			set(h, "QUERY_PRODUKSI_KOSONG", "Perlu diperiksa", "Template ada, tetapi tagihan belum lolos proses produksi",
					"Template DetailBiaya cocok pada audit dasar, namun query produksi tidak menghasilkan baris untuk layar.",
					"Tagihan belum tersedia; biasanya ada kriteria dinamis, kondisi akademik, pengecualian, atau periode yang masih menahan hasil.", "#b45309", "#fff7ed");
			h.langkah.add("Periksa audit template dan data dinamis, lalu klik Proses Tagihan agar baris dibentuk ulang.");
			h.langkah.add("Periksa pula status lulus/keluar, semester pindahan, kelas, tempat tinggal, paket, dan pengecualian NIM.");
		} else {
			set(h, "BELUM_TERGENERASI", "Perlu diproses", "Sumber biaya ada, tetapi rantai tagihan belum lengkap",
					"Konfigurasi hulu ditemukan, namun belum ada baris tagihan maupun transaksi untuk semester ini.",
					"Tagihan belum dapat dipilih pada layar pembayaran sampai proses pembentukan selesai.", "#b45309", "#fff7ed");
			h.langkah.add("Klik Refresh untuk membuang data layar lama, kemudian jalankan Proses Tagihan.");
			h.langkah.add("Jika tetap kosong, gunakan bukti teknis di bawah untuk menemukan tahap pertama yang gagal.");
		}

		h.keyakinan = (d.barisLayar > 0 || sumberSetting <= 0 || d.itemBiayaAktif <= 0)
				? "Tinggi - didukung data layar dan/atau hasil eliminasi konfigurasi."
				: "Sedang - sumber hulu terbaca, tetapi keadaan layar kosong masih perlu dikonfirmasi melalui History dan proses pembentukan tagihan.";
		h.alasan.add("Subjek: " + d.identitas + "; status akademik: " + d.statusAkademik
				+ "; jenis pembayaran: " + d.jenisPembayaran + "; semester: " + d.semester + ".");
		h.alasan.add("Setting cocok: " + sumberSetting + " (umum " + d.kandidatSetting + ", khusus " + d.settingKhusus + ").");
		h.alasan.add("Mode sumber: " + modeSumber(d) + "; Item Biaya aktif: " + d.itemBiayaAktif
				+ "; template akhir: " + d.templateAkhir + ".");
		h.alasan.add("Baris layar: " + d.barisLayar + "; kegiatan pembayaran: " + d.kegiatan
				+ "; cicilan committed: " + d.cicilan + ".");
		if (d.kandidatSetting > 1) {
			h.catatan.add("Ada lebih dari satu Setting Biaya umum yang cocok. Sistem tetap mengikuti prioritas selector produksi, tetapi admin sebaiknya memastikan konfigurasi yang tumpang tindih memang disengaja.");
		}
		if (d.settingDefault > 0 && d.settingBilling > 0) {
			h.catatan.add("Kandidat memakai campuran mode default dan Billing. Pastikan hanya sumber yang memang berlaku yang mempunyai prioritas tertinggi.");
		}
		if (d.kesimpulanTeknis != null && !d.kesimpulanTeknis.trim().isEmpty()) {
			h.catatan.add("Kesimpulan audit teknis: " + d.kesimpulanTeknis.trim());
		}
		tambahLangkahTeknisTanpaDuplikasi(h.langkah, d.tindakanTeknis);
		return h;
	}

	/** Render ringkasan eksekutif. Bukti teknis rinci tetap dirender oleh action sesudah blok ini. */
	public static String htmlRingkasan(Data data) {
		Hasil hasil = analisis(data);
		StringBuffer html = new StringBuffer();
		html.append("<div style='margin-bottom:12px;border:1px solid ").append(hasil.warna)
				.append(";background:").append(hasil.latar).append(";padding:12px;line-height:1.5'>")
				.append("<div style='font-size:16px;font-weight:700;color:").append(hasil.warna).append("'>")
				.append(esc(hasil.judul)).append("</div>")
				.append("<div style='margin-top:3px'><b>Status analisis:</b> ").append(esc(hasil.tingkat))
				.append(" &nbsp;|&nbsp; <b>Kode:</b> ").append(esc(hasil.kode)).append("</div>")
				.append("<div style='margin-top:3px'><b>Keyakinan:</b> ").append(esc(hasil.keyakinan)).append("</div>")
				.append("<div style='margin-top:8px'><b>Apa yang terjadi</b><br>").append(esc(hasil.apaYangTerjadi)).append("</div>")
				.append("<div style='margin-top:7px'><b>Dampaknya</b><br>").append(esc(hasil.dampak)).append("</div></div>");

		html.append("<div style='display:block;margin-bottom:10px;padding:11px;background:#eff6ff;border-left:4px solid #2563eb'>")
				.append("<b>Yang sebaiknya dilakukan</b>").append(htmlList(hasil.langkah)).append("</div>");

		html.append("<div style='margin-bottom:10px;padding:11px;background:#f8fafc;border:1px solid #cbd5e1'>")
				.append("<b>Bukti utama yang dibaca sistem</b>").append(htmlList(hasil.alasan));
		if (data != null) {
			double sisa = Math.max(0.0d, data.nominalTagihanTampil - data.nilaiDibayarCommitted);
			html.append("<div style='margin-top:8px;padding-top:8px;border-top:1px solid #cbd5e1'>")
					.append("Nominal sumber layar: <b>").append(angka(data.nominalTagihanTampil))
					.append("</b> &nbsp;|&nbsp; Pembayaran committed: <b>").append(angka(data.nilaiDibayarCommitted))
					.append("</b> &nbsp;|&nbsp; Estimasi sisa: <b>").append(angka(sisa)).append("</b></div>");
		}
		html.append("</div>");

		if (!hasil.catatan.isEmpty()) {
			html.append("<div style='margin-bottom:10px;padding:10px;background:#fff7ed;border-left:4px solid #f97316'>")
					.append("<b>Catatan analisis</b>").append(htmlList(hasil.catatan)).append("</div>");
		}
		html.append("<div style='margin:12px 0 7px;padding-top:10px;border-top:2px solid #cbd5e1'>")
				.append("<b>Bukti teknis lengkap</b><br><span style='color:#475569'>Bagian berikut menjelaskan setiap tahap query untuk admin. Pengguna operasional cukup mengikuti ringkasan dan langkah di atas.</span></div>");
		return html.toString();
	}

	private static void set(Hasil h, String kode, String tingkat, String judul, String terjadi,
			String dampak, String warna, String latar) {
		h.kode = kode;
		h.tingkat = tingkat;
		h.judul = judul;
		h.apaYangTerjadi = terjadi;
		h.dampak = dampak;
		h.warna = warna;
		h.latar = latar;
	}

	private static String modeSumber(Data d) {
		String detail;
		if (d.settingDefault > 0 && d.settingBilling > 0) detail = "campuran default dan Billing";
		else if (d.settingBilling > 0) detail = "Billing/bulanan";
		else if (d.settingDefault > 0) detail = "nominal default/sekali tagih";
		else detail = d.mode == null ? "belum teridentifikasi" : d.mode;
		return detail + (d.mode == null || d.mode.trim().isEmpty() ? "" : " (hasil produksi: " + d.mode + ")");
	}

	private static void tambahLangkahTeknisTanpaDuplikasi(List<String> tujuan, String tindakan) {
		if (tindakan == null || tindakan.trim().isEmpty()) return;
		String[] baris = tindakan.split("\\n");
		for (String s : baris) {
			if (s == null || s.trim().isEmpty()) continue;
			String langkah = s.trim();
			boolean ada = false;
			for (String existing : tujuan) {
				if (existing.equalsIgnoreCase(langkah)) { ada = true; break; }
			}
			if (!ada) tujuan.add(langkah);
		}
	}

	private static String htmlList(List<String> items) {
		StringBuffer html = new StringBuffer("<ol style='margin:6px 0 0 20px;padding:0'>");
		for (String item : items) html.append("<li style='margin-bottom:4px'>").append(esc(item)).append("</li>");
		return html.append("</ol>").toString();
	}

	private static String angka(double nilai) {
		java.text.DecimalFormat format = new java.text.DecimalFormat("#,##0.##");
		return "Rp" + format.format(Math.max(0.0d, nilai));
	}

	private static String esc(String nilai) {
		if (nilai == null) return "-";
		return nilai.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
