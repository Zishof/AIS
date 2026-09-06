package ais.action.master.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;

/**
 * Helper terpusat untuk menghitung <b>nominal tagihan mahasiswa</b> ketika item biaya yang
 * ditagih bukan harga tetap, melainkan harga satuan yang harus <b>dikalikan sesuatu</b>
 * (jumlah SKS yang diambil, jumlah matakuliah ber-UTS, jumlah matakuliah remedial, ada/tidaknya
 * konversi, tunggakan semester lalu, dan seterusnya).
 *
 * <h2>Apa yang sebenarnya dimaksud &quot;modifikasi nominal&quot;</h2>
 *
 * <p>Nama kelas ini mudah disalahpahami. Kelas ini <b>bukan</b> pintu bagi operator untuk
 * mengetikkan nominal tagihan seorang mahasiswa sesuka hati; kelas ini tidak menerima satu pun
 * nominal dari parameter pemanggil dan tidak punya jalur "isi angka bebas". Yang dilakukannya
 * adalah <b>menurunkan</b> nominal secara deterministik dari tiga sumber yang seluruhnya berupa
 * data master atau data akademik:</p>
 *
 * <ol>
 *   <li><b>Harga dasar.</b> {@link DetailBiaya#getNilaiBiaya()} pada jalur tagihan biasa, atau
 *   {@link PengaturanPembayaranBulanan#getNominal()} pada jalur tagihan bulanan/cicilan.</li>
 *   <li><b>Skema perkalian.</b> {@link ItemBiaya#getPenghitungan()} &mdash; sebuah string yang
 *   dipilih operator dari combobox {@code ItemBiaya.PENGHITUNGAN_MAP}. String inilah yang
 *   memilih cabang rumus di kedua method besar kelas ini.</li>
 *   <li><b>Fakta akademik mahasiswa.</b> Isi KRS-nya ({@link Detailperkuliahan} /
 *   {@link Perkuliahan} / {@link Matakuliah}), status lulus, matakuliah konversi, dan tunggakan
 *   semester sebelumnya &mdash; semuanya dibaca lewat {@link Mahasiswa} dan
 *   {@link KrsDetailHelper}.</li>
 * </ol>
 *
 * <p>Konsekuensi penting untuk penilaian risiko: <b>permukaan otorisasi yang sesungguhnya bukan
 * di kelas ini</b>, melainkan (a) di layar master {@code ItemBiaya} (siapa boleh mengubah
 * {@code penghitungan} dan harga satuan), (b) di layar {@code DetailSettingBiaya} /
 * {@code PengaturanPembayaranBulanan} (siapa boleh mengubah harga per prodi/per bulan), dan
 * (c) di layar KRS (siapa boleh mengubah matakuliah yang diambil mahasiswa, karena mengubah KRS
 * berarti mengubah tagihan). Kelas ini sendiri <b>tidak memeriksa hak akses sama sekali</b> dan
 * memang tidak seharusnya &mdash; ia dipanggil dari dalam render tabel tagihan dan dari getter
 * entity, tempat pemeriksaan hak akses sudah (seharusnya) dilakukan lebih dulu. Lihat bagian
 * &quot;Batasan&quot; di bawah untuk hal yang justru <i>tidak</i> aman.</p>
 *
 * <h2>Dua jalur, dua method</h2>
 *
 * <table border="1">
 *   <caption>Perbandingan kedua jalur</caption>
 *   <tr><th></th><th>{@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}</th>
 *       <th>{@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)}</th></tr>
 *   <tr><td>Untuk</td><td>tagihan biasa (satu baris rincian biaya per semester)</td>
 *       <td>tagihan bulanan/cicilan (satu baris per bulan)</td></tr>
 *   <tr><td>Harga dasar</td><td>{@code DetailBiaya.nilaiBiaya}</td>
 *       <td>{@code PengaturanPembayaranBulanan.nominal}</td></tr>
 *   <tr><td>Hasil ditulis ke</td><td>{@code DetailBiaya.nilaiBiayaBaru} (efek samping)</td>
 *       <td>nilai kembalian {@code Double}</td></tr>
 *   <tr><td>Rincian perhitungan ditulis ke</td><td>{@code DetailBiaya.keterangan} ({@code @Transient})</td>
 *       <td>{@code PengaturanPembayaranBulanan.keterangan} (<b>kolom terpetakan</b>)</td></tr>
 *   <tr><td>Penyaring tahapan</td><td>tidak ada (selalu {@code null})</td>
 *       <td>{@link PengaturanPembayaranBulanan#hitungTahap(Mahasiswa, Integer)}</td></tr>
 *   <tr><td>Gerbang tambahan</td><td>tidak ada</td>
 *       <td>{@link PengaturanPembayaranBulanan#getDikalikanDenganKondisiKhusus()} harus menyala</td></tr>
 * </table>
 *
 * <h2>Aturan utama (invarian yang dituju)</h2>
 * <ul>
 *   <li>{@link ItemBiaya#TIDAK_ADA_PENGHITUNGAN} &mdash; atau {@code penghitungan}
 *   {@code null}/kosong &mdash; selalu memakai nominal asli tanpa perkalian apa pun.</li>
 *   <li>{@link DetailBiaya} non-bulanan memakai {@code nilaiBiaya} sebagai harga dasar.</li>
 *   <li>{@link PengaturanPembayaranBulanan} memakai nominal bulanan sebagai harga dasar.</li>
 *   <li><b>Hasil {@code 0} adalah nilai sah, bukan nilai kosong.</b> Mahasiswa yang tidak
 *   mengambil satu SKS pun memang harus ditagih {@code 0} untuk item berbasis SKS; hasil itu
 *   tidak boleh dipaksa kembali ke nominal awal. Aturan ini yang membedakan kelas ini dari
 *   implementasi lama yang menganggap {@code 0} sebagai "belum dihitung".</li>
 * </ul>
 *
 * <h2>Rantai pemanggil</h2>
 * <ul>
 *   <li>{@link DetailBiaya#updateKeterangan(Mahasiswa, Integer)} &rarr;
 *   {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}. Dipicu antara lain oleh
 *   {@code Kegiatan.ambilJumlahTagihan(...)} saat mendapati {@code nilaiBiayaBaru} masih
 *   {@code null}.</li>
 *   <li>{@link PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)} &rarr;
 *   {@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)}.</li>
 *   <li>{@code PembayaranUtilHelper} dan {@code DetailPembayaranMahasiswaRenderer} memanggil
 *   bentuk statisnya langsung saat menyusun tabel tagihan di layar.</li>
 *   <li>{@code KegiatanPersistenceHelper} memakai {@link #isTanpaPenghitungan(ItemBiaya)} untuk
 *   memutuskan apakah sebuah item perlu dihitung ulang sama sekali.</li>
 * </ul>
 *
 * <h2>Batasan dan jebakan yang perlu diketahui</h2>
 * <ul>
 *   <li><b>Rantai {@code if/else-if} tanpa {@code else} penutup.</b> Kedua method besar memilih
 *   cabang dengan membandingkan {@code penghitungan} satu per satu. Bila tidak ada satu pun
 *   cabang yang cocok, <b>tidak ada nilai yang ditulis</b>: {@code updateKeterangan} meninggalkan
 *   {@code nilaiBiayaBaru} apa adanya (mungkin {@code null}, mungkin sisa perhitungan
 *   sebelumnya), dan {@code ambilNominalModifikasi} mengembalikan nominal dasar tanpa perkalian.
 *   Ini berarti menambah konstanta baru ke {@code ItemBiaya.PENGHITUNGAN_MAP} tanpa menambah
 *   cabang di sini menghasilkan <b>kegagalan diam</b>, bukan galat.</li>
 *   <li><b>Satu skema penghitungan sudah kehilangan cabangnya.</b>
 *   {@link ItemBiaya#DIKALI_JUMLAH_SKS_UAS_REMDIAL} punya cabang di
 *   {@link #ambilNominalModifikasi(PengaturanPembayaranBulanan, Mahasiswa, Integer)} tetapi
 *   <b>tidak</b> di {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)} &mdash; blok yang
 *   seharusnya menanganinya justru menguji {@link ItemBiaya#DIKALI_JUMLAH_SKS_UTS_REMEDIAL}
 *   untuk kedua kalinya (dua blok kembar persis; yang kedua tak akan pernah terjangkau). Lihat
 *   catatan rinci pada method tersebut.</li>
 *   <li><b>{@code ambilNominalModifikasi} menulis ke kolom terpetakan.</b> Meski namanya
 *   diawali {@code ambil} (getter-like), method itu memanggil
 *   {@link PengaturanPembayaranBulanan#setKeterangan(String)} pada entity yang umumnya masih
 *   dikelola sesi Hibernate. Pada instance terkelola, dirty-checking akan menerbitkan
 *   {@code UPDATE} beserta revisi Envers walau tidak ada yang "diubah" secara semantik. Ini
 *   perwujudan pola getter-mutasi-field yang sudah terdokumentasi luas di
 *   {@code ais/database/model/}, dan alasan {@code KegiatanHelper} menandai entity ini read-only
 *   saat hitung ulang massal.</li>
 *   <li><b>Tidak ada jejak audit perubahan nominal.</b> Kelas ini tidak menulis
 *   {@code posting_history}, tidak memanggil {@code Common.catatLog}, dan tidak menyimpan nominal
 *   sebelum/sesudah. Jejak yang ada hanyalah (a) revisi Envers pada tabel master yang diubah
 *   operator ({@code item_biaya}, {@code detail_biaya}, {@code pengaturan_pembayaran_bulanan}),
 *   dan (b) string {@code keterangan} yang merekam rumusnya dalam bentuk teks bebas
 *   (mis. {@code "SPP (500.000) x 20 SKS, sbb : Kalkulus:3sks, ..."}). Karena itu, untuk
 *   merekonstruksi "mengapa tagihan mahasiswa X berubah" seseorang harus menggabungkan riwayat
 *   Envers master biaya dengan riwayat KRS &mdash; kelas ini sendiri tidak mencatat apa pun.</li>
 *   <li><b>Biaya query berat, tanpa cache.</b> Setiap cabang memuat ulang seluruh
 *   {@link Detailperkuliahan} mahasiswa satu per satu lewat
 *   {@link GeneralValueObject#ambilData(Class, String)} (N+1 query). Untuk satu baris tagihan
 *   ini sudah mahal; dipanggil dari renderer tabel untuk ratusan mahasiswa, biayanya berlipat.
 *   Pemanggil massal wajib menyiapkan sesi/cache-nya sendiri.</li>
 *   <li><b>Aritmetika {@code double} untuk uang.</b> Seluruh perkalian memakai {@code double}
 *   tanpa pembulatan eksplisit, sehingga hasil seperti {@code 1.0000000000000002} mungkin muncul
 *   dan baru dibulatkan (atau tidak) di lapisan tampilan/posting.</li>
 * </ul>
 *
 * @see ItemBiaya#getPenghitungan()
 * @see DetailBiaya#updateKeterangan(Mahasiswa, Integer)
 * @see PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)
 */
public final class PembayaranNominalModifikasiHelper {

	/**
	 * Konstruktor privat: kelas ini murni kumpulan method statis dan tidak boleh diinstansiasi.
	 *
	 * <p>Dipasangkan dengan modifier {@code final} pada kelasnya, ini menutup dua jalur
	 * penyalahgunaan sekaligus: membuat objeknya dan mewarisinya untuk menimpa rumus biaya.</p>
	 */
	private PembayaranNominalModifikasiHelper() {
	}

	/**
	 * Menormalkan {@code Double} yang mungkin {@code null} menjadi {@code 0.0}.
	 *
	 * <p>Dipakai di jalur "pulang cepat" {@link #updateKeterangan(DetailBiaya, Mahasiswa, Integer)}
	 * agar {@code nilaiBiayaBaru} selalu terisi angka ketika perhitungan memang tidak berlaku,
	 * sehingga pemanggil tidak perlu membedakan "belum dihitung" dari "tidak perlu dihitung".</p>
	 *
	 * <p><b>Perhatikan asimetrinya:</b> normalisasi ini hanya dipakai pada jalur pulang cepat.
	 * Di dalam rantai perhitungan, harga dasar diambil langsung lewat
	 * {@code detailBiaya.getNilaiBiaya()} tanpa {@code safeDouble}, sehingga item biaya berskema
	 * perkalian yang harganya {@code null} akan melempar {@code NullPointerException} saat
	 * di-unbox pada perkalian &mdash; bukan menghasilkan {@code 0}.</p>
	 *
	 * @param value nilai yang mungkin {@code null}
	 * @return {@code value} apa adanya, atau {@code 0.0} bila {@code value} {@code null};
	 *         tidak pernah {@code null}
	 */
	private static Double safeDouble(Double value) {
		return value == null ? Double.valueOf(0.0) : value;
	}

	/**
	 * Menentukan apakah sebuah {@link ItemBiaya} <b>tidak</b> memakai skema perkalian apa pun,
	 * sehingga nominalnya dipakai apa adanya.
	 *
	 * <p>Tiga keadaan diperlakukan sama sebagai "tanpa penghitungan":</p>
	 * <ul>
	 *   <li>{@code itemBiaya} sendiri {@code null} &mdash; rincian biaya yatim, tidak ada skema
	 *   yang bisa dibaca;</li>
	 *   <li>{@code penghitungan} {@code null} atau kosong setelah {@code trim()} &mdash; data
	 *   lama dari sebelum kolom ini ada, atau operator belum memilih apa pun;</li>
	 *   <li>{@code penghitungan} persis {@link ItemBiaya#TIDAK_ADA_PENGHITUNGAN} &mdash; operator
	 *   memilih "Tidak ada penghitungan" secara sadar.</li>
	 * </ul>
	 *
	 * <p><b>Sikap fail-safe:</b> saat ragu, method ini mengembalikan {@code true}, yaitu
	 * "pakai nominal asli". Itu pilihan yang benar untuk keuangan &mdash; data yang tidak
	 * lengkap tidak boleh menghasilkan tagihan yang dikalikan angka acak. Bandingkan dengan
	 * kebalikannya (default "hitung"), yang akan mengubah harga tetap menjadi
	 * {@code 0} setiap kali skema tidak terbaca.</p>
	 *
	 * <p><b>Perbandingan memakai konstanta, bukan kunci combobox.</b> Yang dibandingkan adalah
	 * <i>label</i> ({@code "Tidak ada penghitungan"}), bukan kunci numeriknya di
	 * {@code ItemBiaya.PENGHITUNGAN_MAP}. Artinya nilai kolom {@code penghitungan} di basis data
	 * memang menyimpan labelnya; mengganti teks label di kemudian hari akan memutus pencocokan
	 * ini untuk seluruh baris lama.</p>
	 *
	 * <p><b>Dipanggil dari:</b> kedua method besar kelas ini sebagai gerbang paling awal, dan
	 * dari {@code KegiatanPersistenceHelper} untuk memutuskan apakah sebuah item biaya perlu
	 * dihitung ulang sama sekali sebelum menyusun tagihan.</p>
	 *
	 * @param itemBiaya item biaya yang diperiksa; boleh {@code null}
	 * @return {@code true} bila nominal harus dipakai apa adanya tanpa perkalian
	 */
	public static boolean isTanpaPenghitungan(ItemBiaya itemBiaya) {
		if (itemBiaya == null) {
			return true;
		}
		String penghitungan = itemBiaya.getPenghitungan();
		return penghitungan == null || penghitungan.trim().length() == 0
				|| ItemBiaya.TIDAK_ADA_PENGHITUNGAN.equals(penghitungan);
	}

	@SuppressWarnings("unchecked")
	public static void updateKeterangan(DetailBiaya detailBiaya, Mahasiswa mahasiswa, Integer semester) {

		if (detailBiaya == null) {
			return;
		}

		ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
		if (itemBiaya == null) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		if (isTanpaPenghitungan(itemBiaya)) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		if (mahasiswa == null || mahasiswa.getId() == null || semester == null) {
			detailBiaya.setNilaiBiayaBaru(safeDouble(detailBiaya.getNilaiBiaya()));
			return;
		}

		JenisKegiatan jenisKegiatan = detailBiaya.getJenisKegiatan();

		if (mahasiswa != null && mahasiswa.getId() != null && detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getTerhubungKeNilaiTambahan() && detailBiaya.getItemBiaya().getParameterTambahan() != null) {
			String parameterTambahanInds = (String) HibernateUtil.currentSession()
					.createCriteria(BiodataMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.desc("id")).setMaxResults(1)
					.setProjection(Projections.property("parameterTambahanInds")).uniqueResult();
			if (parameterTambahanInds != null && !parameterTambahanInds.trim().isEmpty()) {
				String[] spl = parameterTambahanInds.split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					String lbl = value.length > 0 ? value[0].trim() : "";
					String val = value.length > 1 ? value[1].trim() : "";
					if (!val.isEmpty() && Common.isNumber(val)) {
						try {
							String[] labelParts = lbl.split("->");
							String parameterId = labelParts.length > 1 ? labelParts[1].trim() : "";
							if (parameterId.equalsIgnoreCase(detailBiaya.getItemBiaya().getParameterTambahan().getId().toString())) {
								detailBiaya.setNilaiBiayaBaru(Common.numberFormat.get().parse(val.trim()).doubleValue());
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MAHASISWA)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester() < semester) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKS = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester().equals(semester)) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					jmlSKS += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x " + ((int) jmlSKS)
					+ " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKS * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
				&& semester > 1) {

			Kegiatan kegiatan = mahasiswa.ambilKegiatans((semester - 1), jenisKegiatan);
			if (kegiatan != null) {
				detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (Tungggakan semester " + (semester - 1) + " senilai Rp. "
						+ Common.numberFormat.get().format(kegiatan.getAmountTerhutang()) + ")");
				detailBiaya.setNilaiBiayaBaru(kegiatan.getAmountTerhutang());
				detailBiaya.setTunggakanLalu(kegiatan.getAmountTerhutang());
			} else {
				Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa,
						(semester - 1), ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

				double totalTunggakan = 0.0;
				for (Object o : detailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya biaya = (DetailBiaya) o;
						Double nilai = biaya.hitungTotalKegiatan(kegiatan);
						if (nilai > 0.01) {
							totalTunggakan += nilai;
						}
					}
				}
				detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (Tungggakan semester " + (semester - 1) + " senilai Rp. "
						+ Common.numberFormat.get().format(totalTunggakan) + ")");
				detailBiaya.setNilaiBiayaBaru(totalTunggakan);
				detailBiaya.setTunggakanLalu(totalTunggakan);
			}
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			double SKSMatakuliahKonversi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

					SKSMatakuliahKonversi += sks.doubleValue();
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) SKSMatakuliahKonversi) + " SKS Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(SKSMatakuliahKonversi * harga);
		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getSemester() != null && mahasiswa != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA)) {

			boolean lulus = detailBiaya.getSemester().equals(mahasiswa.getSemesterLulus()) && mahasiswa.getSemesterLulus() != null
					&& ConstantValues.LULUS != null && mahasiswa.getStatusKeluar() != null
					&& mahasiswa.getStatusKeluar().getId().equals(ConstantValues.LULUS.getId());

			Double harga = detailBiaya.getNilaiBiaya();
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + (lulus ? " (" + Common.numberFormat.get().format(harga)
					+ ") x 1 karena lulus di semester " + mahasiswa.getSemesterLulus() : ""));
			detailBiaya.setNilaiBiayaBaru((lulus ? 1 : 0) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSks();

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (data.isEmpty() ? 0 : 1) + " Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((data.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
				&& !detailBiaya.getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
			Double harga = detailBiaya.getNilaiBiaya();
			String nama = detailBiaya.getItemBiaya().getNamaMatakuliah().trim();
			String[] spl = nama.split(";");

			Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkTertentu(semester, spl);

			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? ""
									: d.getMatakuliahKonversi().getNama() + "-" + d.getMatakuliahKonversi().getNama())
							: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
									+ d.getPerkuliahan().getMatakuliah().getNama());
					daftarMk += daftarMk.isEmpty() ? dd : ", " + dd;
				}

			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (data.isEmpty() ? 0 : 1) + " " + detailBiaya.getItemBiaya().getNamaMatakuliah() + ", sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((data.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA)
				&& !detailBiaya.getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
			Double harga = detailBiaya.getNilaiBiaya();
			String nama = detailBiaya.getItemBiaya().getNamaMatakuliah().trim();
			String[] spl = nama.split(";");

			Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkSdSmtTertentu(semester, spl);

			Map<String, Integer> daftarMk = new HashMap<String, Integer>();
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? ""
									: d.getMatakuliahKonversi().getNama() + "-" + d.getMatakuliahKonversi().getNama())
							: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
									+ d.getPerkuliahan().getMatakuliah().getNama());

					Integer c = daftarMk.get(dd.toLowerCase());
					if (c == null) {
						c = 1;
					} else {
						c++;
					}
					daftarMk.put(dd.toLowerCase(), c);
				}

			}

			System.out.println("daftarMk -> " + daftarMk);

			String mk = "";
			for (String m : daftarMk.keySet()) {
				Integer c = daftarMk.get(m.toLowerCase());
				if (c != null && c > 1) {
					mk += mk.isEmpty() ? m : ", " + m;
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (mk.isEmpty() ? 0 : 1) + " " + detailBiaya.getItemBiaya().getNamaMatakuliah() + ", sbb : " + mk);
			detailBiaya.setNilaiBiayaBaru((mk.isEmpty() ? 0 : 1) * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_KONVERSI)) {
			Double harga = detailBiaya.getNilaiBiaya();
			Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null);

			double SKSMatakuliahKonversi = data.size();
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					String dd = d.getPerkuliahan() == null
							? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
							: d.getPerkuliahan().getMatakuliah().getNama();
					daftarMk += daftarMk.isEmpty() ? dd : "," + dd;
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) SKSMatakuliahKonversi) + " MK Konversi, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(SKSMatakuliahKonversi * harga);
		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSPraktek = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {
					Integer sks = d.getMatakuliahKonversi().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSPraktek * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSPraktek = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {
					Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {
					Integer sks = d.getMatakuliahKonversi().getSksPraktek();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSPraktek += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSPraktek * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSDiskusi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSDiskusi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSDiskusi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSDiskusi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSDiskusi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_SIMULASI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			boolean semua = false;
			Integer tahapan = null;
			Integer semesterPendek = null;
			boolean remedial = false;
			Integer persetujuan = null;
			Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, semua,
					persetujuan);

			double jmlSKSSimulasi = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null && d.getPerkuliahan() != null) {

					Integer sks = d.getPerkuliahan().getMatakuliah().getSksSimulasi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSSimulasi += sks.doubleValue();
					}
				}
			}

			data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

			for (Long detailperkuliahanid : data) {
				Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (d != null) {

					Integer sks = d.getMatakuliahKonversi().getSksSimulasi();
					if (sks > 0) {
						String dd = d.getPerkuliahan() == null
								? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
								: d.getPerkuliahan().getMatakuliah().getNama();
						daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

						jmlSKSSimulasi += sks.doubleValue();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ ((int) jmlSKSSimulasi) + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jmlSKSSimulasi * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}

					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(1)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 1 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(2)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}
			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 2 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(3)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 3 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah != null && matakuliah.getSks().equals(4)
							&& detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " remedial 4 SKS, yaitu: " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		////////////////////////////////////////////////

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas()
							&& (detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true, false,
					null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true, false,
					null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah += matakuliah.getSks();
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		///////////////////////////////////////////////

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_PRAKTEK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);

			String daftarMk = "";
			int jumlahMatakuliah = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getMerupakanMkPraktek()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_DISKUSI_TEORI)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);

			String daftarMk = "";
			int jumlahMatakuliah = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (matakuliah.getMerupakanMkTeori()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if ((detailperkuliahan.getPerkuliahan() == null
							|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
							&& (detailperkuliahan.getPerkuliahan() == null
									|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		}

		else if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru(jumlahMatakuliah * harga);

		} else if (detailBiaya.getItemBiaya() != null
				&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_SP)) {

			Double harga = detailBiaya.getNilaiBiaya();

			Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null,
					Perkuliahan.SEMESTER_PENDEK);
			Double jumlahMatakuliah = 0.0;
			String daftarMk = "";
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() == null) {
						continue;
					}
					Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan().getMatakuliah();
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
						jumlahMatakuliah++;
						daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
					}
				}
			}

			detailBiaya.setKeterangan(detailBiaya.getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga) + ") x "
					+ (jumlahMatakuliah.intValue() > 0 ? 1 : 0) + " SP, sbb : " + daftarMk);
			detailBiaya.setNilaiBiayaBaru((jumlahMatakuliah.intValue() > 0 ? 1 : 0) * harga);

		}

	
	}

	public static Double ambilNominalModifikasi(PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Mahasiswa mahasiswa, Integer semester) {

		if (pengaturanPembayaranBulanan == null) {
			return Double.valueOf(0.0);
		}

		Double nominalModifikasi = pengaturanPembayaranBulanan.getNominal();
		if (nominalModifikasi == null) {
			nominalModifikasi = Double.valueOf(0.0);
		}

		DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
		ItemBiaya itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
		if (isTanpaPenghitungan(itemBiaya)) {
			return nominalModifikasi;
		}

		if (mahasiswa == null || semester == null) {
			return nominalModifikasi;
		}

		Integer tahapan = pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester);
		if (pengaturanPembayaranBulanan.getDetailBiaya() != null) {

			if (pengaturanPembayaranBulanan.getDikalikanDenganKondisiKhusus()) {

				if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MAHASISWA)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null && d.getPerkuliahan().getSemester() < semester) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKS = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null
								&& d.getPerkuliahan().getSemester().equals(semester)) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							jmlSKS += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jmlSKS) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKS * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getSemester() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA)) {

					boolean lulus = pengaturanPembayaranBulanan.getDetailBiaya().getSemester().equals(mahasiswa.getSemesterLulus())
							&& mahasiswa.getSemesterLulus() != null && ConstantValues.LULUS != null
							&& mahasiswa.getStatusKeluar() != null
							&& mahasiswa.getStatusKeluar().getId().equals(ConstantValues.LULUS.getId());

					Double harga = pengaturanPembayaranBulanan.getNominal();
					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
							+ (lulus ? " (" + Common.numberFormat.get().format(harga) + ") x 1 karena lulus di semester "
									+ mahasiswa.getSemesterLulus() : ""));
					nominalModifikasi = ((lulus ? 1 : 0) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (data.isEmpty() ? 0 : 1) + " Konversi, sbb : " + daftarMk);
					nominalModifikasi = ((data.isEmpty() ? 0 : 1) * harga);
				} else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
						&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
					Double harga = pengaturanPembayaranBulanan.getNominal();

					String nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim();
					String[] spl = nama.split(";");

					Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkTertentu(semester, spl);

					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? ""
											: d.getMatakuliahKonversi().getNama() + "-"
													+ d.getMatakuliahKonversi().getNama())
									: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
											+ d.getPerkuliahan().getMatakuliah().getNama());
							daftarMk += daftarMk.isEmpty() ? dd : ", " + dd;
						}

					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (data.isEmpty() ? 0 : 1) + " "
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah() + ", sbb : " + daftarMk);
					nominalModifikasi = ((data.isEmpty() ? 0 : 1) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA)
						&& !pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim().isEmpty()) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					String nama = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah().trim();
					String[] spl = nama.split(";");

					Collection<Long> data = mahasiswa.ambilDetailperkuliahanMkSdSmtTertentu(semester, spl);

					Map<String, Integer> daftarMk = new HashMap<String, Integer>();
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? ""
											: d.getMatakuliahKonversi().getNama() + "-"
													+ d.getMatakuliahKonversi().getNama())
									: (d.getPerkuliahan().getMatakuliah().getNama() + "-"
											+ d.getPerkuliahan().getMatakuliah().getNama());

							Integer c = daftarMk.get(dd.toLowerCase());
							if (c == null) {
								c = 1;
							} else {
								c++;
							}
							daftarMk.put(dd.toLowerCase(), c);
						}

					}

					System.out.println("daftarMk -> " + daftarMk);

					String mk = "";
					for (String m : daftarMk.keySet()) {
						Integer c = daftarMk.get(m.toLowerCase());
						if (c != null && c > 1) {
							mk += mk.isEmpty() ? m : ", " + m;
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (mk.isEmpty() ? 0 : 1) + " "
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNamaMatakuliah() + ", sbb : " + mk);

					nominalModifikasi = ((mk.isEmpty() ? 0 : 1) * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((jumlahMatakuliah > 0 ? 1 : 0)) + " jumlah matakuliah, sbb : " + daftarMk);
					nominalModifikasi = ((jumlahMatakuliah > 0 ? 1 : 0) * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					double SKSMatakuliahKonversi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSks();

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

							SKSMatakuliahKonversi += sks.doubleValue();
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) SKSMatakuliahKonversi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (SKSMatakuliahKonversi * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_KONVERSI)) {
					Double harga = pengaturanPembayaranBulanan.getNominal();
					Collection<Long> data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, null);

					double SKSMatakuliahKonversi = data.size();
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							String dd = d.getPerkuliahan() == null
									? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
									: d.getPerkuliahan().getMatakuliah().getNama();
							daftarMk += daftarMk.isEmpty() ? dd : "," + dd;
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) SKSMatakuliahKonversi) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (SKSMatakuliahKonversi * harga);
				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSPraktek = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Integer sks = d.getMatakuliahKonversi().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSPraktek * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSPraktek = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {
							Integer sks = d.getPerkuliahan().getMatakuliah().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Integer sks = d.getMatakuliahKonversi().getSksPraktek();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSPraktek += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSPraktek) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSPraktek * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSDiskusi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSDiskusi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSDiskusi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksDiskusi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSDiskusi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSDiskusi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSDiskusi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_SKS_SIMULASI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					boolean semua = false;
					Integer semesterPendek = null;
					boolean remedial = false;
					Integer persetujuan = null;
					Collection<Long> data = mahasiswa.ambilDetailperkuliahan(semester, tahapan, semesterPendek,
							remedial, semua, persetujuan);

					double jmlSKSSimulasi = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null && d.getPerkuliahan() != null) {

							Integer sks = d.getPerkuliahan().getMatakuliah().getSksSimulasi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSSimulasi += sks.doubleValue();
							}
						}
					}

					data = KrsDetailHelper.ambilDetailperkuliahanKonversi(mahasiswa, semester);

					for (Long detailperkuliahanid : data) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {

							Integer sks = d.getMatakuliahKonversi().getSksSimulasi();
							if (sks > 0) {
								String dd = d.getPerkuliahan() == null
										? (d.getMatakuliahKonversi() == null ? "" : d.getMatakuliahKonversi().getNama())
										: d.getPerkuliahan().getMatakuliah().getNama();
								daftarMk += daftarMk.isEmpty() ? dd + ":" + sks + "sks" : ", " + dd + ":" + sks + "sks";

								jmlSKSSimulasi += sks.doubleValue();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + ((int) jmlSKSSimulasi) + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jmlSKSSimulasi * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}

							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah.intValue()) + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_UTS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_UAS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}
					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(1)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 1 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(2)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 2 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(3)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 3 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null, true);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah != null && matakuliah.getSks().equals(4)
									&& detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " remedial 4 SKS, yaitu: " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				////////////////////////////////////////////////

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas()
									&& (detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UTS_REMEDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true,
							false, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUts() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_SKS_UAS_REMDIAL)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, null, null, true,
							false, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getTerdapatUas() && detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah += matakuliah.getSks();
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " SKS, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				///////////////////////////////////////////////

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_PRAKTEK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);

					String daftarMk = "";
					int jumlahMatakuliah = 0;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getMerupakanMkPraktek()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					double jml = (double) jumlahMatakuliah;

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jml * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null && pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
						.equals(ItemBiaya.DIKALI_JUMLAH_MK_DISKUSI_TEORI)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);

					String daftarMk = "";
					int jumlahMatakuliah = 0;
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (matakuliah.getMerupakanMkTeori()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + (jumlahMatakuliah) + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan, null);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if ((detailperkuliahan.getPerkuliahan() == null
									|| detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
									&& (detailperkuliahan.getPerkuliahan() == null
											|| !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}

				else if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_JUMLAH_MK_SP)) {

					Double harga = pengaturanPembayaranBulanan.getNominal();

					Collection<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan(semester, tahapan,
							Perkuliahan.SEMESTER_PENDEK);
					Double jumlahMatakuliah = 0.0;
					String daftarMk = "";
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPerkuliahan() == null) {
								continue;
							}
							Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
									? detailperkuliahan.getMatakuliahKonversi()
									: detailperkuliahan.getPerkuliahan().getMatakuliah();
							if (detailperkuliahan.getPerkuliahan() != null
									&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
									&& !detailperkuliahan.getPerkuliahan().getMerupakanRemedial()) {
								jumlahMatakuliah++;
								daftarMk += daftarMk.isEmpty() ? matakuliah.getNama() : "," + matakuliah.getNama();
							}
						}
					}

					pengaturanPembayaranBulanan.setKeterangan(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " (" + Common.numberFormat.get().format(harga)
							+ ") x " + jumlahMatakuliah.intValue() + " matakuliah, sbb : " + daftarMk);
					nominalModifikasi = (jumlahMatakuliah * harga);

				}
			}
		}

		return nominalModifikasi;
	
	}
}
