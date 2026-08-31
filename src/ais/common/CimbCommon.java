package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.cimb.CimbRequest;
import ais.database.model.cimb.CimbRequestDetail;
import ais.database.model.cimb.CimbRequestDetailBiaya;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

/**
 * Helper terpusat untuk alur pembayaran via <b>CIMB Niaga Virtual Account (VA)</b> di AIS,
 * mencakup: menyusun tombol pemilihan metode bayar di UI ZKoss, mengumpulkan rincian tagihan dari
 * berbagai sumber (grid biaya kuliah, grid cicilan, parameter request web), serta menyimpan
 * permintaan pembayaran ({@link CimbRequest} beserta rincian {@link CimbRequestDetail}/
 * {@link CimbRequestDetailBiaya}) ke database.
 *
 * <h2>Model integrasi: VA dibuat lokal, bukan panggilan API real-time ke CIMB</h2>
 * <p>
 * Berbeda dari sebagian integrasi payment gateway lain di AIS yang memanggil API pihak ketiga
 * secara real-time untuk membuat token/VA (bandingkan {@link OttoUtil} yang memanggil OttoPay lewat
 * {@code curl}), alur CIMB pada kelas ini TIDAK melakukan panggilan keluar ke API/gateway CIMB sama
 * sekali — nomor VA dan seluruh detail transaksi dibuat/disimpan sepenuhnya di sisi aplikasi AIS
 * (lihat komentar eksplisit pada {@link #sendRequest} yang menyebut "CIMB tidak memanggil gateway di
 * sini (VA dibuat lokal)"). Rekonsiliasi dengan CIMB sesungguhnya (verifikasi pembayaran
 * masuk/notifikasi) berada di komponen terpisah di luar berkas ini (lihat modul terkait di bawah
 * paket {@code CIMB3rdParty}/{@code BillPaymentWS} pada basis kode, yang TIDAK diubah sebagai bagian
 * dari dokumentasi ini) — kelas ini murni menyiapkan sisi permintaan/pencatatan di aplikasi. Tidak
 * ditemukan kredensial, API key, atau merchant id tertanam pada berkas ini; seluruh data yang
 * ditulis ke {@link CimbRequest} berasal dari input pengguna/entitas domain (mahasiswa, biaya,
 * cicilan), bukan konfigurasi rahasia.
 * </p>
 *
 * <h2>Peta method</h2>
 * <ul>
 * <li>{@link #createButton()} — menyiapkan konfigurasi tombol "Bayar via CIMB Niaga" (label dapat
 * dikonfigurasi lewat {@code label_pembayaran_via_cimb}, ikon dapat dikustomisasi lewat lampiran
 * {@link LampiranLain#BG_TOMBOL_PEMBAYARAN_VIA_CIMB}).</li>
 * <li>{@link #populateDetailBiaya}, {@link #populateCimbRequestDetailDariDetailBiaya},
 * {@link #populateCimbRequestDetail(HttpServletRequest, Mahasiswa, String, Integer)},
 * {@link #populateCimbRequestDetail(Grid, Mahasiswa, Integer, JadwalPembayaran)} — empat cara
 * berbeda mengumpulkan rincian item biaya yang akan dibayar, tergantung sumber datanya: grid biaya
 * kuliah ZKoss (dengan komponen input yang bisa diedit pengguna), request HTTP dari halaman web non-
 * ZKoss, atau grid cicilan pembayaran (dengan perhitungan denda otomatis lewat
 * {@link PengaturanPembayaranBulanan#checkDenda}).</li>
 * <li>{@link #bayarCalonMahasiswa(BiodataCalonMahasiswa, JenisKegiatan)} — alur khusus pembayaran
 * pendaftaran mahasiswa baru: menghitung rincian biaya lewat {@link PembayaranUtil} berdasarkan
 * program studi kelulusan/pilihan calon mahasiswa, lalu langsung memicu {@link #onSaveCimb}.</li>
 * <li>{@link #onPilihCimb}/{@link #onSaveCimb} — titik masuk dari aksi UI (klik tombol bayar):
 * memvalidasi nominal, memanggil {@link #sendRequest} untuk menyimpan permintaan, lalu menampilkan
 * jendela nomor VA ({@code /common/cimb/no_va.zul}) bila berhasil atau pesan galat teknis lewat
 * {@link InfoTeknisPembayaran} bila gagal.</li>
 * <li>{@link #sendRequest} — implementasi kanonik yang benar-benar menyimpan {@link CimbRequest}
 * beserta seluruh baris detailnya ke database, masing-masing dalam transaksi Hibernate terpisah per
 * baris (bukan satu transaksi besar mencakup seluruhnya).</li>
 * </ul>
 */
public class CimbCommon {

	/**
	 * Menyiapkan konfigurasi tombol "Bayar via CIMB Niaga" untuk ditampilkan di UI ZKoss: label
	 * diambil dari konfigurasi {@code label_pembayaran_via_cimb} (default "Bayar via CIMB Niaga"),
	 * dan ikon diambil dari lampiran kustom {@link LampiranLain#BG_TOMBOL_PEMBAYARAN_VIA_CIMB} bila
	 * tersedia (disalin ke folder {@code /img} lokal bila belum ada di sana), jatuh kembali ke ikon
	 * default {@code cimb-logo.jpg} bila lampiran tidak ditemukan atau terjadi kegagalan saat
	 * menyalin.
	 *
	 * @return konfigurasi tombol siap pakai ({@link MyButtonConfig}) berisi label dan path ikon
	 */
	public static MyButtonConfig createButton() {
		File fileViaCimb = new File(Common.REAL_PATH + "/img/cimb-logo.jpg");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaCimb = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaCimb.getName());
				boolean ada = fileDiImg.exists();
				System.out.println("fileViaCimb = " + fileViaCimb + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaCimb);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaCimb = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_cimb", "Bayar via CIMB Niaga").getNilai(),
				"/img/" + fileViaCimb.getName());
		return bayarViaCimb;
	}

	/**
	 * Mengumpulkan rincian item biaya dari sebuah {@link Grid} ZKoss (tampilan tabel biaya kuliah)
	 * menjadi daftar {@link CimbRequestDetailBiaya}. Untuk setiap baris grid yang terlihat
	 * ({@code isVisible()}), nilai biaya diambil dengan prioritas: (1) bila komponen input pada baris
	 * tersebut adalah {@link Doublebox} dan item biayanya memang boleh diubah nilainya, ambil nilai
	 * dari {@link Doublebox}; (2) bila komponennya {@link Label}, parsing nilai numerik dari teks
	 * label lewat {@link Common#numberFormat}; (3) jika tidak keduanya, jatuh kembali ke
	 * {@code detailBiaya.getNilaiBiayaBaru()} atau {@code getNilaiBiaya()}. Khusus item biaya dengan
	 * mode penghitungan {@link ItemBiaya#DIKALI_NILAI_MINUS}, nilai akhirnya justru ditimpa dari
	 * komponen pengurangan yang cocok di {@code pengurangan} (dicari berdasarkan kecocokan id
	 * {@link DetailBiaya}), bukan dari nilai grid utama.
	 *
	 * @param gridss      grid ZKoss sumber, setiap barisnya diharapkan memiliki atribut
	 *                    {@code "myValue"} berisi {@link DetailBiaya} dan {@code "tag"} berisi
	 *                    komponen input (opsional)
	 * @param pengurangan daftar komponen input pengurangan (dengan atribut {@code "itemBiaya"} berisi
	 *                    {@link DetailBiaya} terkait) yang dipakai khusus untuk item biaya bertipe
	 *                    {@link ItemBiaya#DIKALI_NILAI_MINUS}
	 * @return daftar {@link CimbRequestDetailBiaya}, satu per baris grid yang terlihat
	 */
	@SuppressWarnings("unchecked")
	public static List<CimbRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<CimbRequestDetailBiaya> cimbRequestDetailBiayas = new ArrayList<CimbRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
			System.out.println("myRows -> " + myRows.size());
			for (Row row : myRows) {
				if (!row.isVisible()) {
					continue;
				}
				DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

				Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru();
				try {
					Component component = (Component) row.getAttribute("tag");
					if (component instanceof Doublebox
							&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
						Doublebox jumlah = (Doublebox) component;
						biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					} else if (component instanceof Label) {
						Label myLabel = (Label) component;
						// System.out.println("myLabel = " +
						// myLabel.getValue());
						biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CimbCommon.java:110");
				}

				if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					for (MyDoubleboxMin kurang : pengurangan) {
						DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
						if (penguranganItemBiaya != null
								&& penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
							biaya = kurang.getValue() == null ? 0.0 : kurang.getValue();
							break;
						}
					}
				}

				CimbRequestDetailBiaya cimbRequestDetailBiaya = new CimbRequestDetailBiaya();
				cimbRequestDetailBiaya.setDetailBiaya(detailBiaya);
				cimbRequestDetailBiaya.setNilai(biaya);
				cimbRequestDetailBiayas.add(cimbRequestDetailBiaya);
			}
		}
		return cimbRequestDetailBiayas;
	}

	/**
	 * Mengonversi daftar {@link CimbRequestDetailBiaya} (hasil {@link #populateDetailBiaya}) menjadi
	 * daftar {@link CimbRequestDetail} yang siap disimpan sebagai baris rincian permintaan CIMB,
	 * dengan nomor urut ({@code ke}) dimulai dari 1 sesuai urutan pada {@code cimbRequestDetailBiayas}
	 * dan tanggal diisi dengan waktu saat ini ({@link ais.ui.util.WaktuUtil#getDate()}).
	 * {@code pengaturanPembayaranBulanan} selalu diset {@code null} pada hasil konversi ini (jalur
	 * ini dipakai untuk pembayaran biaya langsung, bukan cicilan bulanan berjadwal).
	 *
	 * @param cimbRequestDetailBiayas daftar rincian biaya sumber
	 * @return daftar {@link CimbRequestDetail} baru, satu per elemen sumber
	 */
	public static List<CimbRequestDetail> populateCimbRequestDetailDariDetailBiaya(
			List<CimbRequestDetailBiaya> cimbRequestDetailBiayas) {
		List<CimbRequestDetail> cimbRequestDetails = new ArrayList<CimbRequestDetail>();

		int i = 1;
		for (CimbRequestDetailBiaya cimbRequestDetailBiaya : cimbRequestDetailBiayas) {
			CimbRequestDetail cimbRequestDetail = new CimbRequestDetail();
			cimbRequestDetail.setPengaturanPembayaranBulanan(null);
			cimbRequestDetail.setItemBiaya(cimbRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			cimbRequestDetail.setKeterangan(cimbRequestDetailBiaya.getKeterangan());
			cimbRequestDetail.setNilai(cimbRequestDetailBiaya.getNilai());
			cimbRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			cimbRequestDetail.setKe(i);
			cimbRequestDetails.add(cimbRequestDetail);
			i++;
		}

		return cimbRequestDetails;
	}

	/**
	 * Mengumpulkan rincian item biaya dari parameter {@link HttpServletRequest} (dipakai jalur
	 * pembayaran non-ZKoss, mis. endpoint web sederhana) menjadi daftar {@link CimbRequestDetail}.
	 * Membaca parameter {@code jenis} ("bulanan" untuk mengambil dari
	 * {@link PengaturanPembayaranBulanan}, nilai lain untuk mengambil langsung dari
	 * {@link DetailBiaya}) dan {@code data} (daftar id, dipisah koma) dari request. Untuk setiap id,
	 * nominal dihitung (untuk jenis bulanan lewat
	 * {@link PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)}, untuk jenis
	 * lain langsung dari {@code nilaiBiayaBaru}/{@code nilaiBiaya}), dan keterangan disusun otomatis
	 * berisi kode+nama item biaya, nominal terformat, serta info {@code validator} bila diberikan.
	 *
	 * <p>
	 * <b>Catatan:</b> kondisi {@code jenis.equalsIgnoreCase(jenis)} pada percabangan jenis biaya
	 * selalu bernilai {@code true} (membandingkan variabel dengan dirinya sendiri) — cabang
	 * {@code else} pada percabangan tersebut tidak pernah tereksekusi dalam implementasi saat ini.
	 * Perilaku ini didokumentasikan apa adanya sesuai kode; tidak diubah sebagai bagian dari
	 * pekerjaan dokumentasi ini.
	 * </p>
	 *
	 * @param request   request HTTP sumber parameter {@code jenis} dan {@code data}
	 * @param mahasiswa mahasiswa pembayar, dipakai untuk menghitung nominal modifikasi pembayaran
	 *                  bulanan
	 * @param validator keterangan tambahan (mis. nama validator/petugas) yang disisipkan ke
	 *                  keterangan tiap baris bila tidak kosong
	 * @param semester  semester yang dipakai untuk menghitung nominal modifikasi pembayaran bulanan
	 * @return daftar {@link CimbRequestDetail} hasil parsing parameter request
	 */
	public static List<CimbRequestDetail> populateCimbRequestDetail(HttpServletRequest request, Mahasiswa mahasiswa,
			String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
		System.out.println("jenis => " + jenis + ", data => " + data);
		List<CimbRequestDetail> cimbRequestDetails = new ArrayList<CimbRequestDetail>();

		Session session = HibernateUtil.currentNativeSession();
		int i = 1;
		for (String d : data.split(",")) {

			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
			ItemBiaya itemBiaya = null;
			DetailBiaya detailBiaya = null;
			Double nilai = 0.0;

			if (jenis.equalsIgnoreCase(jenis)) {
				pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
						.createCriteria(PengaturanPembayaranBulanan.class)
						.add(Restrictions.idEq(Long.parseLong(d.trim()))).uniqueResult();
				detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
				itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				nilai = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
			} else {
				detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
						.add(Restrictions.idEq(Long.parseLong(d.trim()))).uniqueResult();
				itemBiaya = detailBiaya.getItemBiaya();
				nilai = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru());
			}

			String keterangan = "";
			if ((keterangan == null || keterangan.trim().isEmpty()) && pengaturanPembayaranBulanan != null) {
				keterangan = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "-"
						+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + ", bulan "
						+ pengaturanPembayaranBulanan.getNamaBulan() + " " + ", nominal Rp. "
						+ Common.numberFormat.get().format(nilai)
						+ (validator.trim().isEmpty() ? "" : ", validator : " + validator);
			} else if ((keterangan == null || keterangan.trim().isEmpty()) && itemBiaya != null && nilai != null) {
				keterangan = itemBiaya.getKode() + "-" + itemBiaya.getNama() + ", nominal Rp. "
						+ Common.numberFormat.get().format(nilai)
						+ (validator.trim().isEmpty() ? "" : ", validator : " + validator);

			}

			CimbRequestDetail cimbRequestDetail = new CimbRequestDetail();
			cimbRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			cimbRequestDetail.setDetailBiaya(detailBiaya);
			cimbRequestDetail.setItemBiaya(itemBiaya);
			cimbRequestDetail.setKeterangan(keterangan);
			cimbRequestDetail.setNilai(nilai);
			cimbRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			cimbRequestDetail.setKe(i);
			cimbRequestDetail.setDenda(0.0);
			cimbRequestDetail.setNilaiAsli(nilai);
			cimbRequestDetails.add(cimbRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return cimbRequestDetails;
	}

	/**
	 * Mengumpulkan rincian item biaya dari {@link Grid} ZKoss tampilan cicilan pembayaran, menjadi
	 * daftar {@link CimbRequestDetail}. Hanya baris dengan nilai cicilan signifikan (di luar rentang
	 * {@code -0.01} hingga {@code 0.01}) yang diproses. Untuk baris cicilan BARU (belum punya id
	 * {@link CicilanPembayaran}) yang terkait {@link PengaturanPembayaranBulanan}, method ini juga
	 * menghitung ULANG denda keterlambatan secara otomatis lewat
	 * {@link PengaturanPembayaranBulanan#checkDenda} — memperhitungkan apakah ada jadwal pembayaran
	 * khusus untuk NIM mahasiswa tersebut ({@code jadwalPembayaran.getKhususUntukNim()}) — dan
	 * menyimpannya sebagai selisih antara nilai dengan-denda dan nilai nominal asli. Validator
	 * (keterangan siapa yang memvalidasi cicilan) diambil dari {@link CicilanPembayaran} yang ada,
	 * atau bila kosong/{@code "null"}, jatuh kembali ke representasi string pengguna yang sedang
	 * login ({@link Common#getCurrentUser()}).
	 *
	 * @param gridCicilan      grid ZKoss cicilan, setiap barisnya membawa atribut
	 *                         {@code jumlahCicilan}, {@code cicilanPembayaran}, {@code tanggal},
	 *                         {@code itemBiaya}, dan {@code keterangan} sebagai komponen input
	 * @param mahasiswa        mahasiswa pembayar, dipakai untuk perhitungan denda
	 * @param semester         semester berjalan, dipakai untuk perhitungan nominal modifikasi
	 * @param jadwalPembayaran jadwal pembayaran aktif, dipakai untuk menentukan berlaku tidaknya
	 *                         pengecualian jadwal khusus NIM saat menghitung denda; boleh
	 *                         {@code null}
	 * @return daftar {@link CimbRequestDetail} untuk baris cicilan dengan nilai signifikan
	 */
	public static List<CimbRequestDetail> populateCimbRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<CimbRequestDetail> cimbRequestDetails = new ArrayList<CimbRequestDetail>();

		int i = 1;
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() != null
					&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {

				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

				String val = cicilanPembayaran == null ? null : cicilanPembayaran.getValidator();
				if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
					Tbmuser tbmuser = Common.getCurrentUser();
					val = (tbmuser == null ? "" : tbmuser.toString());

				}

				CimbRequestDetail cimbRequestDetail = new CimbRequestDetail();

				Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
						: myItemBiaya.getSelectedItem().getValue();
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
						.getPengaturanPembayaranBulanan();
				ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();
				if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
					itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				} else if (jenisBiaya != null && jenisBiaya instanceof ItemBiaya) {
					itemBiaya = (ItemBiaya) jenisBiaya;
				}

				cimbRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				cimbRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				cimbRequestDetail.setItemBiaya(itemBiaya);
				cimbRequestDetail.setKeterangan(keterangan.getValue());
				cimbRequestDetail.setNilai(jumlahCicilan.getValue());
				cimbRequestDetail.setTanggal(tanggal.getValue());
				cimbRequestDetail.setKe(i);

				cimbRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				cimbRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, cimbRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan())
								- nom;
						cimbRequestDetail.setDenda(denda);
						cimbRequestDetail.setNilaiAsli(nom);
					}
				}

				cimbRequestDetails.add(cimbRequestDetail);
				i++;
			}
		}

		return cimbRequestDetails;
	}

	/**
	 * Alur pembayaran khusus untuk pendaftaran mahasiswa baru: menghitung rincian biaya yang wajib
	 * dibayar {@code calonMahasiswa} lewat {@link PembayaranUtil}, berdasarkan program studi
	 * kelulusan ({@code prodiLulus}) bila sudah ditentukan, atau salah satu program studi
	 * pilihan (prodi1/prodi2) bila kelulusan belum ditentukan. Bila ada biaya yang harus dibayar dan
	 * jadwal pembayaran yang berlaku ditemukan (lewat
	 * {@link PembayaranUtil#getJadwalPembayaranDanDendaBerdasarkanTahunAkademik}), method ini
	 * langsung menyusun {@link CimbRequestDetailBiaya} untuk tiap item biaya dan memicu
	 * {@link #onSaveCimb} dengan keterangan tetap {@code "Pembayaran Pendaftaran Mahasiswa Baru"} dan
	 * nominal pembulatan hasil format angka ({@link Common#numberFormat}, untuk menghindari
	 * perbedaan floating point kecil antara nilai mentah dan nilai yang ditampilkan ke pengguna).
	 *
	 * @param calonMahasiswa data calon mahasiswa yang akan membayar biaya pendaftaran
	 * @param jenisKegiatan  jenis kegiatan akademik terkait pembayaran (mis. gelombang pendaftaran)
	 * @throws Exception diteruskan dari {@link PembayaranUtil} atau dari {@link #onSaveCimb}/
	 *                    {@link #onPilihCimb}/{@link #sendRequest} di ujung rantai pemanggilan
	 */
	public static void bayarCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa, JenisKegiatan jenisKegiatan)
			throws Exception {
		Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		if (prodiLulus == null || prodiLulus.getId() == null) {
			Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
					: calonMahasiswa.getProdi1();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, myjurusan1, false);
			detailBiayas.addAll(detailBiayas1);
		} else {
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, prodiLulus, false);
			detailBiayas.addAll(detailBiayas1);
		}

		if (!detailBiayas.isEmpty()) {

			Serializable[] serializables = PembayaranUtil.getInstance()
					.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(calonMahasiswa.getTanggalDaftar(),
							jenisKegiatan, calonMahasiswa.getJenjang(), calonMahasiswa.getTahunAkademik(),
							calonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(
									Perkuliahan.GANJIL),
							calonMahasiswa.getJenisSeleksi(), calonMahasiswa.getProgram(),
							calonMahasiswa.getNoRegistrasi(), calonMahasiswa.getGelombangPendaftaran());
			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

			if (jadwalPembayaran != null) {
				Double nilaiBiayaHarusDiBayars = 0.0;

				List<CimbRequestDetailBiaya> cimbRequestDetailBiayas = new ArrayList<CimbRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					CimbRequestDetailBiaya cimbRequestDetailBiaya = new CimbRequestDetailBiaya();
					cimbRequestDetailBiaya.setDetailBiaya(detailBiaya);
					cimbRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					cimbRequestDetailBiayas.add(cimbRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += cimbRequestDetailBiaya.getNilai();
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				CimbCommon.onSaveCimb(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						CimbCommon.populateCimbRequestDetailDariDetailBiaya(cimbRequestDetailBiayas),
						cimbRequestDetailBiayas, null);

			}
		}

	}

	/**
	 * Titik masuk UI untuk memilih metode bayar CIMB: memanggil {@link #sendRequest} untuk membuat
	 * dan menyimpan {@link CimbRequest} beserta rincian detailnya. Bila berhasil (request tersimpan),
	 * membuka jendela ZKoss nomor VA ({@code /common/cimb/no_va.zul}) yang menampilkan trxId sebagai
	 * nomor VA dan nominal terformat lewat {@link Common#displayWindow(String, boolean, String)}.
	 * Bila gagal (request bernilai {@code null} dari {@link #sendRequest}), menampilkan pesan
	 * peringatan generik yang detail teknisnya sudah dicatat lebih dulu oleh
	 * {@link InfoTeknisPembayaran} di dalam {@link #sendRequest} — pola bersama yang dipakai seluruh
	 * payment gateway di AIS agar pesan galat ke pengguna tetap ringkas namun detail teknis tetap
	 * tersedia untuk audit/dukungan.
	 *
	 * @param amn                      nominal yang akan dibayar
	 * @param mahasiswa                mahasiswa pembayar, boleh {@code null} bila pembayar adalah
	 *                                 calon mahasiswa
	 * @param biodataCalonMahasiswa    calon mahasiswa pembayar, boleh {@code null} bila pembayar
	 *                                 adalah mahasiswa aktif
	 * @param jenisKegiatan            jenis kegiatan akademik terkait pembayaran
	 * @param jadwalPembayaran         jadwal pembayaran yang berlaku
	 * @param semester                 semester berjalan
	 * @param tahunAkademik            tahun akademik berjalan
	 * @param keterangan               keterangan transaksi
	 * @param pengurangan              nilai pengurangan/diskon yang diterapkan, boleh {@code null}
	 * @param nilaiBiayaHarusDiBayars  total nilai biaya sebelum pengurangan
	 * @param cimbRequestDetails       rincian detail transaksi per item biaya
	 * @param cimbRequestDetailBiayas  rincian detail biaya mentah (sebelum dikonversi ke
	 *                                 {@link CimbRequestDetail})
	 * @param event                    event ZKoss pemicu (tidak dipakai langsung oleh method ini,
	 *                                 diteruskan apa adanya ke {@link #sendRequest})
	 * @return selalu {@code true} — nilai kembalian tidak membedakan sukses/gagal; keberhasilan
	 *         ditentukan dari isi jendela yang ditampilkan ke pengguna
	 * @throws Exception diteruskan dari {@link #sendRequest} atau kegagalan encode URL parameter
	 *                    jendela hasil
	 */
	public static boolean onPilihCimb(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<CimbRequestDetail> cimbRequestDetails,
			List<CimbRequestDetailBiaya> cimbRequestDetailBiayas, Event event) throws Exception {

		final CimbRequest cimbRequest = CimbCommon.sendRequest(mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
				jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, amn,
				cimbRequestDetails, cimbRequestDetailBiayas, true);
		if (cimbRequest != null) {

			String myUrl = "/common/cimb/no_va.zul?va=" + URLEncoder.encode(cimbRequest.getTrxId(), "UTF-8")
					+ "&nominal="
					+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(cimbRequest.getAmount()), "UTF-8");

			Common.displayWindow(myUrl, true, "65%");

		} else {
			// Tampilkan alert + "Informasi Teknis" yang dicatat sendRequest (pola bersama
			// seluruh payment gateway via InfoTeknisPembayaran).
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	/**
	 * Validasi ringan sebelum mendelegasikan ke {@link #onPilihCimb}: menolak permintaan dengan
	 * nominal yang secara efektif nol atau negatif (kurang dari {@code 0.01}), lalu meneruskan
	 * seluruh parameter apa adanya ke {@link #onPilihCimb} bila valid. Parameter identik dengan
	 * {@link #onPilihCimb} — lihat javadoc method tersebut untuk detail masing-masing.
	 *
	 * @return {@code false} bila nominal kurang dari {@code 0.01} (permintaan ditolak tanpa
	 *         menyimpan apa pun); {@code true} bila permintaan diteruskan ke {@link #onPilihCimb}
	 *         (nilai kembali {@link #onPilihCimb} sendiri diabaikan)
	 * @throws Exception diteruskan dari {@link #onPilihCimb}
	 */
	@SuppressWarnings({})
	public static boolean onSaveCimb(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<CimbRequestDetail> cimbRequestDetails,
			final List<CimbRequestDetailBiaya> cimbRequestDetailBiayas, final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihCimb(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester, tahunAkademik,
				keterangan, pengurangan, nilaiBiayaHarusDiBayars, cimbRequestDetails, cimbRequestDetailBiayas, event);

		return true;
	}

	/**
	 * Implementasi kanonik pembuatan permintaan pembayaran CIMB: satu-satunya method di kelas ini
	 * yang benar-benar menyimpan {@link CimbRequest} beserta seluruh baris
	 * {@link CimbRequestDetail}/{@link CimbRequestDetailBiaya} terkait ke database. Tidak ada
	 * panggilan API/gateway CIMB eksternal apa pun di sini — VA dibuat sepenuhnya lokal (lihat
	 * javadoc kelas {@link CimbCommon} untuk penjelasan model integrasi ini).
	 *
	 * <p>
	 * Sebelum menyimpan, {@link InfoTeknisPembayaran#bersihkan()} dipanggil untuk membersihkan
	 * detail kegagalan transaksi sebelumnya agar tidak "bocor" ke alert pengguna untuk transaksi
	 * baru ini. {@link CimbRequest} induk disimpan dalam transaksi Hibernate tersendiri, lalu setiap
	 * baris {@link CimbRequestDetail} dan {@link CimbRequestDetailBiaya} disimpan MASING-MASING
	 * dalam transaksi terpisah pula (bukan satu transaksi besar mencakup seluruh operasi) — pola ini
	 * berarti kegagalan di tengah penyimpanan baris detail dapat meninggalkan {@link CimbRequest}
	 * induk tersimpan dengan sebagian detail saja (tidak sepenuhnya atomik).
	 * </p>
	 *
	 * <p>
	 * Bila terjadi kegagalan (Hibernate atau lainnya) selama proses ini, detail teknisnya dicatat ke
	 * {@link InfoTeknisPembayaran} (dengan pesan error dipotong maksimal 200 karakter lewat
	 * {@link InfoTeknisPembayaran#potong}) untuk ditampilkan ke admin/pemanggil (lewat
	 * {@link #onPilihCimb}), dan galat itu sendiri hanya ditampilkan ke admin lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} — TIDAK dilempar ulang ke pemanggil, sehingga
	 * pemanggil harus memeriksa apakah {@link CimbRequest} yang dikembalikan memiliki id
	 * (tersimpan) atau tidak untuk mengetahui sukses/gagalnya operasi.
	 * </p>
	 *
	 * @param mahasiswa                 mahasiswa pembayar, boleh {@code null} bila pembayar calon
	 *                                  mahasiswa
	 * @param biodataCalonMahasiswa     calon mahasiswa pembayar, boleh {@code null} bila pembayar
	 *                                  mahasiswa aktif
	 * @param jenisKegiatan             jenis kegiatan akademik terkait pembayaran
	 * @param jadwalPembayaran          jadwal pembayaran yang berlaku
	 * @param semester                  semester berjalan
	 * @param tahunAkademik             tahun akademik berjalan
	 * @param keterangan                keterangan transaksi
	 * @param pengurangan               nilai pengurangan/diskon
	 * @param nilaiBiayaHarusDiBayars   total nilai biaya sebelum pengurangan
	 * @param amount                    nominal akhir yang harus dibayar
	 * @param cimbRequestDetails        rincian detail transaksi per item biaya, dihubungkan ke
	 *                                  {@link CimbRequest} yang baru dibuat lalu disimpan satu per
	 *                                  satu
	 * @param cimbRequestDetailBiayas   rincian detail biaya mentah, dihubungkan dan disimpan dengan
	 *                                  cara yang sama
	 * @param hapusCicilanSebelumnya    penanda apakah cicilan sebelumnya untuk transaksi terkait
	 *                                  perlu dihapus (disimpan ke {@link CimbRequest}, logika
	 *                                  penghapusannya sendiri berada di luar method ini)
	 * @return {@link CimbRequest} yang baru dibuat; memiliki id (berhasil tersimpan) bila proses
	 *         sukses, atau objek kosong tanpa id bila terjadi kegagalan yang tertangkap secara
	 *         internal
	 * @throws Exception praktis tidak pernah dilempar ke pemanggil karena seluruh badan method
	 *                    dibungkus {@code try/catch(Exception)} internal; dideklarasikan untuk
	 *                    konsistensi dengan signature pemanggil dalam rantai {@code onPilihCimb}/
	 *                    {@code onSaveCimb}/{@code bayarCalonMahasiswa}
	 */
	public static CimbRequest sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars, Double amount,
			List<CimbRequestDetail> cimbRequestDetails, List<CimbRequestDetailBiaya> cimbRequestDetailBiayas,
			Boolean hapusCicilanSebelumnya) throws Exception {

		// Bersihkan detail kegagalan lama agar info transaksi sebelumnya tidak bocor ke alert.
		InfoTeknisPembayaran.bersihkan();

		CimbRequest cimbRequest = new CimbRequest();

		try {

			cimbRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			cimbRequest.setNama(mahasiswa == null ? biodataCalonMahasiswa.toString() : mahasiswa.toString());
			cimbRequest.setMahasiswa(mahasiswa);
			cimbRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			cimbRequest.setJenisKegiatan(jenisKegiatan);
			cimbRequest.setJadwalPembayaran(jadwalPembayaran);
			cimbRequest.setSemester(semester);
			cimbRequest.setTahunAkademik(tahunAkademik);
			cimbRequest.setKeterangan(keterangan);
			cimbRequest.setPengurangan(pengurangan);
			cimbRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
			cimbRequest.setAmount(amount);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(cimbRequest);
			session.getTransaction().commit();

			for (CimbRequestDetail cimbRequestDetail : cimbRequestDetails) {
				cimbRequestDetail.setCimbRequest(cimbRequest);
				session.getTransaction().begin();
				session.save(cimbRequestDetail);
				session.getTransaction().commit();
			}

			for (CimbRequestDetailBiaya cimbRequestDetailBiaya : cimbRequestDetailBiayas) {
				cimbRequestDetailBiaya.setCimbRequest(cimbRequest);
				session.getTransaction().begin();
				session.save(cimbRequestDetailBiaya);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			// CIMB tidak memanggil gateway di sini (VA dibuat lokal) — kegagalan berarti
			// request GAGAL disimpan di aplikasi. Catat detailnya agar alert tidak generik.
			InfoTeknisPembayaran.catat("Request CIMB Niaga GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
		}

		return cimbRequest;
	}

}
