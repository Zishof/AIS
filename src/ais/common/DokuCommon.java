package ais.common;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import ais.database.model.BiodataMahasiswa;
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
import ais.database.model.doku.DokuRequest;
import ais.database.model.doku.DokuRequestDetail;
import ais.database.model.doku.DokuRequestDetailBiaya;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kelas utilitas statis untuk integrasi payment gateway <b>Doku</b> di AIS, menangani seluruh
 * alur pembayaran biaya kuliah/pendaftaran mahasiswa (baik mahasiswa aktif maupun calon
 * mahasiswa) mulai dari penyusunan rincian tagihan dari komponen antarmuka ZK, penghitungan
 * tanda tangan transaksi (SHA1), penyimpanan record permintaan pembayaran ke database, hingga
 * pembentukan formulir HTML auto-submit yang mengarahkan browser pengguna ke gateway pembayaran
 * Doku (pola integrasi Doku "Basic API" lama: form POST browser berisi field tersandi, bukan
 * REST API server-to-server).
 *
 * <p>
 * Alur kerja umum melibatkan tiga lapisan:
 * </p>
 * <ol>
 * <li><b>Penyusunan rincian tagihan</b> — {@link #populateDetailBiaya(Grid, List)},
 * {@link #populateDokuRequestDetailDariDetailBiaya(List)}, dan
 * {@link #populateDokuRequestDetail(Grid, Mahasiswa, Integer, JadwalPembayaran)} membaca nilai
 * dari komponen grid ZK (baris biaya, baris cicilan) yang sudah diisi/diedit pengguna di layar
 * pembayaran, lalu mengonversinya menjadi objek {@link DokuRequestDetail}/
 * {@link DokuRequestDetailBiaya} yang siap disimpan.</li>
 * <li><b>Orkestrasi pembayaran</b> — {@link #bayarCalonMahasiswa(BiodataCalonMahasiswa,
 * JenisKegiatan)} adalah titik masuk khusus alur pendaftaran mahasiswa baru: menghitung detail
 * biaya yang wajib dibayar calon mahasiswa berdasarkan program studi & gelombang pendaftaran
 * lewat {@link PembayaranUtil}, lalu mendelegasikan ke {@link #onSaveDoku} untuk memulai
 * transaksi. Untuk alur pembayaran mahasiswa aktif, pemanggil biasanya memanggil
 * {@link #onSaveDoku} secara langsung dari layar pembayaran terkait.</li>
 * <li><b>Eksekusi transaksi</b> — {@link #onSaveDoku} adalah method inti: menghitung tanda
 * tangan transaksi ({@code WORDS = SHA1(AMOUNT + key + TRANSIDMERCHANT)}), memanggil
 * {@link #sendRequest} untuk menyimpan record {@link DokuRequest} beserta detailnya ke database
 * SEBELUM permintaan dikirim ke gateway (karena Doku "Basic API" memakai form-post browser,
 * bukan panggilan HTTP langsung dari server — request ke Doku baru benar-benar terjadi saat
 * browser pengguna men-submit form), lalu menulis berkas HTML sementara berisi form auto-submit
 * ke direktori {@code /tmp} pada {@code Common.REAL_PATH} dan menampilkannya lewat
 * {@link Common#displayWindow(String, boolean, String)} agar browser pengguna diarahkan ke
 * gateway pembayaran Doku.</li>
 * </ol>
 *
 * <p>
 * Kegagalan penyimpanan record transaksi ({@link #sendRequest}) ditangani lewat pola
 * "Informasi Teknis" bersama seluruh payment gateway AIS ({@link InfoTeknisPembayaran}):
 * detail teknis kegagalan dicatat lewat {@link InfoTeknisPembayaran#catat(String)} dan
 * ditampilkan ke pengguna sebagai bagian pesan alert kegagalan
 * ({@link InfoTeknisPembayaran#pesanGagal()}) oleh {@link #onSaveDoku}, sehingga pengguna/admin
 * mendapat konteks teknis tanpa perlu membuka log server.
 * </p>
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01):</b> method {@link #onSaveDoku} sebelumnya membaca
 * {@code doku_key} (dipakai sebagai bagian dari perhitungan SHA1 tanda tangan transaksi
 * {@code WORDS} — setara dengan shared-secret Doku; nilai yang sama sebelumnya juga ditanam
 * ulang sebagai data uji di {@code ais.common.AeSimpleSHA1.main}, sudah diperbaiki terpisah)
 * dengan nilai default rahasia tertulis langsung di kode sumber. Default itu sudah DIHAPUS (kini
 * string kosong) — siapa pun yang sebelumnya membaca kode sumber dapat menghitung ulang tanda
 * tangan {@code WORDS} yang sah dan berpotensi memalsukan/mengubah parameter transaksi yang
 * dikirim ke Doku selama kredensial lama masih aktif. {@code doku_merchant_id} TETAP memiliki
 * default ({@code "10444535"}) karena itu pengenal merchant, bukan rahasia kriptografis — tidak
 * diubah pada perbaikan ini. <b>Tindak lanjut yang TETAP diperlukan di luar perubahan kode
 * ini:</b> {@code doku_key} yang sebelumnya tertanam sudah lama berada di riwayat SVN dan WAJIB
 * dianggap bocor — perlu dirotasi di sisi Doku bila masih aktif di produksi.
 * </p>
 *
 * <p><b>Riwayat keamanan (DIPERBAIKI 2026-09-07):</b> {@code ais.action.servlet.DokuVerifyServlet}
 * dan {@code ais.action.servlet.DokuResponseServlet} sebelumnya TIDAK memverifikasi checksum
 * {@code WORDS} atas notifikasi/pre-check yang diterima dari gateway Doku — keduanya hanya
 * mencocokkan identifier transaksi mentah ({@code trxId} atau {@code TRANSIDMERCHANT}) dan/atau
 * {@code AMOUNT}, tanpa memverifikasi bahwa {@code WORDS} yang menyertainya benar-benar tanda
 * tangan sah transaksi tersebut. Karena {@code DokuResponseServlet} memakai hasil itu untuk
 * MEMFINALISASI pembayaran/pendaftaran begitu {@code RESULT} bertuliskan {@code "Success"}, celah
 * ini memungkinkan siapa pun yang mengetahui/menebak {@code TRANSIDMERCHANT} suatu transaksi untuk
 * memalsukan notifikasi pelunasan tanpa pembayaran nyata. Ditambahkan
 * {@link #verifikasiChecksum(DokuRequest, String, String)} sebagai gerbang tunggal yang dipakai
 * kedua servlet SEBELUM data notifikasi/verifikasi dipakai untuk keputusan apa pun; lihat javadoc
 * method tersebut untuk rincian formula checksum Doku Basic Store yang diterapkan.</p>
 */
public class DokuCommon {

	/**
	 * Menyusun daftar {@link DokuRequestDetailBiaya} dari baris-baris (rows) sebuah komponen
	 * {@link Grid} ZK yang menampilkan rincian biaya, sambil menerapkan aturan pengurangan
	 * khusus untuk item biaya bertipe {@link ItemBiaya#DIKALI_NILAI_MINUS}.
	 *
	 * <p>
	 * Untuk setiap baris grid yang tampak ({@code isVisible()}), nilai biaya diambil dari
	 * atribut {@code myValue} ({@link DetailBiaya}) baris tersebut (memakai
	 * {@code nilaiBiayaBaru} bila ada, jika tidak memakai {@code nilaiBiaya}), lalu ditimpa
	 * dengan nilai komponen input pada atribut {@code tag} bila komponen tersebut berupa
	 * {@link Doublebox} yang dapat diedit ({@code getItemBiaya().getNilaiBisaDiubah()}) atau
	 * berupa {@link Label} (diparsing dari teks berformat angka). Bila item biaya bertipe
	 * {@code DIKALI_NILAI_MINUS}, nilai akhirnya diambil dari komponen
	 * {@link MyDoubleboxMin} yang sesuai pada parameter {@code pengurangan} (dicocokkan lewat
	 * id {@link DetailBiaya} pada atribut {@code itemBiaya}).
	 * </p>
	 *
	 * @param gridss      komponen {@link Grid} ZK berisi baris rincian biaya, dengan setiap
	 *                    {@link Row} membawa atribut {@code myValue} ({@link DetailBiaya}) dan
	 *                    {@code tag} (komponen input terkait)
	 * @param pengurangan daftar komponen {@link MyDoubleboxMin} untuk item biaya bertipe
	 *                    {@code DIKALI_NILAI_MINUS}, dicocokkan lewat atribut {@code itemBiaya}
	 * @return daftar {@link DokuRequestDetailBiaya} siap simpan, satu per baris grid yang
	 *         tampak
	 */
	@SuppressWarnings("unchecked")
	public static List<DokuRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<DokuRequestDetailBiaya> dokuRequestDetailBiayas = new ArrayList<DokuRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
			for (Row row : myRows) {
				if (!row.isVisible()) {
					continue;
				}
				DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

				Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru();
				try {
					Component component = (Component) row.getAttribute("tag");
					if (component instanceof Doublebox && detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
						Doublebox jumlah = (Doublebox) component;
						biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					} else if (component instanceof Label) {
						Label myLabel = (Label) component;
						// System.out.println("myLabel = " +
						// myLabel.getValue());
						biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DokuCommon.java:72");
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

				DokuRequestDetailBiaya dokuRequestDetailBiaya = new DokuRequestDetailBiaya();
				dokuRequestDetailBiaya.setDetailBiaya(detailBiaya);
				dokuRequestDetailBiaya.setNilai(biaya);
				dokuRequestDetailBiayas.add(dokuRequestDetailBiaya);
			}
		}
		return dokuRequestDetailBiayas;
	}

	/**
	 * Mengonversi daftar {@link DokuRequestDetailBiaya} (hasil {@link #populateDetailBiaya})
	 * menjadi daftar {@link DokuRequestDetail} yang siap disimpan sebagai rincian permintaan
	 * pembayaran Doku, dengan nomor urut ({@code ke}) berurutan mulai dari 1 dan tanggal
	 * diisi tanggal saat ini ({@link ais.ui.util.WaktuUtil#getDate()}). Dipakai pada alur
	 * pembayaran biaya non-cicilan (mis. {@link #bayarCalonMahasiswa}).
	 *
	 * @param dokuRequestDetailBiayas daftar rincian biaya sumber
	 * @return daftar {@link DokuRequestDetail} yang setara, dengan {@code pengaturanPembayaranBulanan}
	 *         selalu {@code null} (bukan pembayaran bulanan)
	 */
	public static List<DokuRequestDetail> populateDokuRequestDetailDariDetailBiaya(
			List<DokuRequestDetailBiaya> dokuRequestDetailBiayas) {
		List<DokuRequestDetail> dokuRequestDetails = new ArrayList<DokuRequestDetail>();

		int i = 1;
		for (DokuRequestDetailBiaya dokuRequestDetailBiaya : dokuRequestDetailBiayas) {
			DokuRequestDetail dokuRequestDetail = new DokuRequestDetail();
			dokuRequestDetail.setPengaturanPembayaranBulanan(null);
			dokuRequestDetail.setItemBiaya(dokuRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			dokuRequestDetail.setKeterangan(dokuRequestDetailBiaya.getKeterangan());
			dokuRequestDetail.setNilai(dokuRequestDetailBiaya.getNilai());
			dokuRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			dokuRequestDetail.setKe(i);
			dokuRequestDetails.add(dokuRequestDetail);
			i++;
		}

		return dokuRequestDetails;
	}

	/**
	 * Menyusun daftar {@link DokuRequestDetail} dari baris-baris grid cicilan pembayaran ZK,
	 * hanya menyertakan baris yang jumlah cicilannya diisi (bukan nol/mendekati nol).
	 * </p>
	 * <p>
	 * Untuk setiap baris yang memenuhi syarat, method ini:
	 * </p>
	 * <ul>
	 * <li>Menentukan validator (petugas) dari {@link CicilanPembayaran#getValidator()}, atau
	 * bila kosong/null, memakai representasi pengguna yang sedang login
	 * ({@link Common#getCurrentUser()}).</li>
	 * <li>Menentukan {@link ItemBiaya}/{@link DetailBiaya}/{@link PengaturanPembayaranBulanan}
	 * yang berlaku, bergantung pada tipe nilai yang dipilih pengguna pada combobox jenis biaya
	 * (bisa berupa {@link PengaturanPembayaranBulanan} untuk pembayaran bulanan/SPP, atau
	 * {@link DetailBiaya} untuk biaya sekali bayar).</li>
	 * <li>Menyalin nilai {@code denda}/{@code nilaiAsli} dari {@link CicilanPembayaran} yang
	 * sudah ada bila cicilan tersebut sudah memiliki id (record lama); bila cicilan baru
	 * (belum memiliki id) dan terkait {@link PengaturanPembayaranBulanan}, denda dan nilai asli
	 * DIHITUNG ULANG lewat {@link PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa,
	 * Integer)} dan {@link PengaturanPembayaranBulanan#checkDenda}, termasuk pengecekan apakah
	 * {@code jadwalPembayaran} berlaku khusus untuk NIM mahasiswa yang bersangkutan.</li>
	 * </ul>
	 *
	 * @param gridCicilan     komponen {@link Grid} ZK berisi baris cicilan pembayaran, dengan
	 *                        atribut {@code jumlahCicilan}, {@code cicilanPembayaran},
	 *                        {@code tanggal}, {@code itemBiaya}, dan {@code keterangan} pada
	 *                        setiap {@link Row}
	 * @param mahasiswa       mahasiswa pemilik cicilan, dipakai untuk menghitung nominal
	 *                        modifikasi dan pengecekan jadwal khusus NIM
	 * @param semester        semester berjalan, diteruskan ke perhitungan nominal modifikasi
	 * @param jadwalPembayaran jadwal pembayaran yang berlaku (untuk pengecekan denda
	 *                         keterlambatan), boleh {@code null}
	 * @return daftar {@link DokuRequestDetail} untuk baris cicilan yang diisi, dengan nomor
	 *         urut ({@code ke}) berurutan mulai dari 1
	 */
	public static List<DokuRequestDetail> populateDokuRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<DokuRequestDetail> dokuRequestDetails = new ArrayList<DokuRequestDetail>();

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

				DokuRequestDetail dokuRequestDetail = new DokuRequestDetail();

				Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
						: myItemBiaya.getSelectedItem().getValue();
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
						.getPengaturanPembayaranBulanan();
				ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();
				DetailBiaya detailBiaya = null;
				if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
					itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				} else if (jenisBiaya != null && jenisBiaya instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) jenisBiaya;
					itemBiaya = detailBiaya.getItemBiaya();
				}

				dokuRequestDetail.setDetailBiaya(detailBiaya);
				dokuRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				dokuRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				dokuRequestDetail.setItemBiaya(itemBiaya);
				dokuRequestDetail.setKeterangan(keterangan.getValue());
				dokuRequestDetail.setNilai(jumlahCicilan.getValue());
				dokuRequestDetail.setTanggal(tanggal.getValue());
				dokuRequestDetail.setKe(i);

				dokuRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				dokuRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, dokuRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						dokuRequestDetail.setDenda(denda);
						dokuRequestDetail.setNilaiAsli(nom);
					}
				}

				dokuRequestDetails.add(dokuRequestDetail);
				i++;
			}
		}

		return dokuRequestDetails;
	}

	/**
	 * Titik masuk alur pembayaran biaya pendaftaran untuk seorang calon mahasiswa lewat
	 * gateway Doku. Menentukan program studi acuan (memakai {@code prodiLulus} bila calon
	 * mahasiswa sudah dinyatakan lulus/diterima pada satu program studi, atau salah satu dari
	 * {@code prodi1}/{@code prodi2} bila belum), menghitung daftar {@link DetailBiaya} yang
	 * wajib dibayar lewat {@link PembayaranUtil#getDetailBiayaCalonMahasiswa}, menentukan
	 * jadwal pembayaran & denda yang berlaku lewat
	 * {@link PembayaranUtil#getJadwalPembayaranDanDendaBerdasarkanTahunAkademik}, lalu bila
	 * ditemukan jadwal pembayaran yang valid, mendelegasikan eksekusi transaksi ke
	 * {@link #onSaveDoku}. Bila tidak ada biaya yang harus dibayar atau tidak ditemukan jadwal
	 * pembayaran yang berlaku, method ini tidak melakukan apa pun (tidak ada transaksi
	 * dimulai).
	 *
	 * @param calonMahasiswa data calon mahasiswa yang akan membayar biaya pendaftaran
	 * @param jenisKegiatan  jenis kegiatan/gelombang penerimaan yang menentukan komponen biaya
	 *                       yang berlaku
	 * @throws Exception diteruskan dari kegagalan perhitungan biaya/jadwal pembayaran
	 *                    ({@link PembayaranUtil}) atau dari {@link #onSaveDoku}
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

				List<DokuRequestDetailBiaya> dokuRequestDetailBiayas = new ArrayList<DokuRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					DokuRequestDetailBiaya dokuRequestDetailBiaya = new DokuRequestDetailBiaya();
					dokuRequestDetailBiaya.setDetailBiaya(detailBiaya);
					dokuRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					dokuRequestDetailBiayas.add(dokuRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += dokuRequestDetailBiaya.getNilai();
				}

				DokuCommon.onSaveDoku(nilaiBiayaHarusDiBayars, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						DokuCommon.populateDokuRequestDetailDariDetailBiaya(dokuRequestDetailBiayas),
						dokuRequestDetailBiayas, null);

			}
		}

	}

	/**
	 * Implementasi inti eksekusi transaksi pembayaran lewat gateway Doku: menghitung tanda
	 * tangan transaksi, menyimpan record permintaan ke database lewat {@link #sendRequest},
	 * lalu membentuk dan menampilkan formulir HTML auto-submit yang mengarahkan browser
	 * pengguna ke gateway Doku.
	 *
	 * <p>
	 * Bila {@code amn} (nominal yang harus dibayar) kurang dari {@code 0.01}, method langsung
	 * mengembalikan {@code false} tanpa melakukan apa pun (tidak ada gunanya memulai transaksi
	 * bernilai nol). Selain itu, method ini:
	 * </p>
	 * <ol>
	 * <li>Menyusun {@code add_info2} — ringkasan item biaya non-cicilan dalam format teks yang
	 * disyaratkan Doku ({@code "<nama>,<harga>.00,1,<harga>.00"}, dipisah {@code ;} antar
	 * item), dipakai sebagai isi keranjang belanja ({@code BASKET}) pada form Doku.</li>
	 * <li>Membaca kredensial Doku ({@code doku_key}, {@code doku_merchant_id}) dari konfigurasi
	 * — lihat peringatan keamanan pada javadoc kelas — dan menghitung tanda tangan transaksi
	 * {@code WORDS = SHA1(AMOUNT + key + TRANSIDMERCHANT)} lewat {@link AeSimpleSHA1#SHA1}.</li>
	 * <li>Memanggil {@link #sendRequest} untuk menyimpan record {@link DokuRequest} beserta
	 * detailnya SEBELUM permintaan dikirim ke gateway.</li>
	 * <li>Bila penyimpanan berhasil (record memiliki {@code nama}), mengambil data biodata
	 * mahasiswa terbaru ({@link BiodataMahasiswa}, bila {@code mahasiswa} tidak {@code null})
	 * untuk mengisi field identitas pembayar (nama, email, telepon, alamat, kode pos, tanggal
	 * lahir, kota, provinsi) pada form Doku, lalu menulis berkas HTML form auto-submit ke
	 * {@code Common.REAL_PATH + "/tmp/" + WORDS + ".html"} dan menampilkannya lewat
	 * {@link Common#displayWindow(String, boolean, String)} sehingga browser pengguna
	 * diarahkan (via {@code <script>...submit()</script>}) ke URL gateway Doku
	 * ({@code doku_gateway_url}).</li>
	 * <li>Bila penyimpanan gagal, menampilkan alert kegagalan beserta "Informasi Teknis" lewat
	 * {@link InfoTeknisPembayaran#pesanGagal()} dan {@link MyMessageboxConfig}.</li>
	 * </ol>
	 *
	 * @param amn                       nominal total yang harus dibayar (rupiah)
	 * @param mahasiswa                 mahasiswa pembayar, boleh {@code null} bila pembayar
	 *                                  adalah calon mahasiswa
	 * @param biodataCalonMahasiswa     calon mahasiswa pembayar, boleh {@code null} bila
	 *                                  pembayar adalah mahasiswa aktif
	 * @param jenisKegiatan             jenis kegiatan terkait transaksi
	 * @param jadwalPembayaran          jadwal pembayaran yang berlaku
	 * @param semester                  semester terkait transaksi
	 * @param tahunAkademik             tahun akademik terkait transaksi
	 * @param keterangan                keterangan/deskripsi transaksi
	 * @param pengurangan               nilai pengurangan/diskon yang sudah diperhitungkan
	 * @param nilaiBiayaHarusDiBayars   nilai total biaya sebelum pengurangan
	 * @param dokuRequestDetails        rincian item pembayaran (hasil salah satu method
	 *                                  {@code populateDokuRequestDetail*})
	 * @param dokuRequestDetailBiayas   rincian biaya mentah (hasil {@link #populateDetailBiaya})
	 * @param event                     event ZK pemicu (tidak dipakai langsung di badan method
	 *                                  ini, kemungkinan disediakan untuk kompatibilitas
	 *                                  pemanggil/pengembangan lanjutan)
	 * @return {@code true} bila proses (baik sukses menampilkan form Doku maupun gagal dengan
	 *         alert ditampilkan) selesai dijalankan; {@code false} hanya bila {@code amn}
	 *         kurang dari {@code 0.01} (transaksi tidak dimulai sama sekali)
	 * @throws Exception diteruskan dari kegagalan {@link #sendRequest} atau operasi
	 *                    penulisan berkas HTML
	 */
	@SuppressWarnings({})
	public static boolean onSaveDoku(final Double amn, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars,
			List<DokuRequestDetail> dokuRequestDetails, List<DokuRequestDetailBiaya> dokuRequestDetailBiayas,
			Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String add_info2 = "";

		for (DokuRequestDetail detail : dokuRequestDetails) {
			if (detail.getIdCicilan() == null) {
				String ket = detail.getItemBiaya().getNama() + "," + (detail.getNilai().intValue() + ".00") + ",1,"
						+ (detail.getNilai().intValue() + ".00");
				add_info2 += add_info2.isEmpty() ? ket : ";" + ket;
			}
		}

		String key = Common.getKonfigurasi("doku_key", "").getNilai();

		String TRANSIDMERCHANT = Common.getGeneratedBarCode();
		String STOREID = Common.getKonfigurasi("doku_merchant_id", "10444535").getNilai();
		String AMOUNT = amn.intValue() + ".00";
		String WORDS = AMOUNT + key + TRANSIDMERCHANT;
		WORDS = AeSimpleSHA1.SHA1(WORDS);

		final DokuRequest dokuRequest = DokuCommon.sendRequest(TRANSIDMERCHANT, WORDS, mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, add_info2, dokuRequestDetails, dokuRequestDetailBiayas);
		if (dokuRequest != null && dokuRequest.getNama() != null && !dokuRequest.getNama().trim().isEmpty()) {

			String CNAME = "";
			String CEMAIL = "";
			String PHONE = "";
			String CADDRESS = "";
			String CZIPCODE = "";
			String BIRTHDATE = "";
			String CCITY = "";
			String CSTATE = "";
			if (mahasiswa != null) {
				Session session = HibernateUtil.currentNativeSession();
				BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();
				CNAME = mahasiswa.getNama();
				CEMAIL = mahasiswa.getEmail().split(",")[0];
				PHONE = mahasiswa.getTelp();
				CADDRESS = mahasiswa.getAlamat();
				CZIPCODE = biodataMahasiswa == null ? "" : biodataMahasiswa.getKodepos();
				BIRTHDATE = mahasiswa.getTanggallahir() == null ? ""
						: Common.databaseDateFormat.get().format(mahasiswa.getTanggallahir());
				CCITY = biodataMahasiswa == null || biodataMahasiswa.getKota() == null ? ""
						: biodataMahasiswa.getKota().getNama();
				CSTATE = biodataMahasiswa == null || biodataMahasiswa.getPropinsi() == null ? ""
						: biodataMahasiswa.getPropinsi().getNama();
			}

			String url = Common
					.getKonfigurasi("doku_gateway_url", "https://apps.myshortcart.com/payment/request-payment/")
					.getNilai();

			String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
					+ "<html lang=\"en\">" + "<head>" + "	<title>Form Transaksi</title>" + "</head>" + "<body>" +

					"Loading...<FORM ID=\"order" + WORDS + "\" NAME=\"order\" METHOD=\"Post\" ACTION=\"" + url + "\" >"
					+ "	<input type=hidden name=\"BASKET\" value=\"" + add_info2 + "\">"
					+ "	<input type=hidden name=\"STOREID\" value=\"" + STOREID + "\"> "
					+ "	<input type=hidden name=\"TRANSIDMERCHANT\" value=\"" + TRANSIDMERCHANT + "\">"
					+ "	<input type=hidden name=\"AMOUNT\" value=\"" + AMOUNT + "\">"
					+ "	<input type=hidden name=\"URL\" value=\"" + Common.getRequestHostWithProtocol() + "\">"
					+ "	<input type=hidden name=\"WORDS\" value=\"" + WORDS + "\">"
					+ "	<input type=hidden name=\"CNAME\" value=\"" + CNAME + "\">"
					+ "	<input type=hidden name=\"CEMAIL\" value=\"" + CEMAIL + "\">"
					+ "	<input type=hidden name=\"CWPHONE\" value=\"" + PHONE + "\">"
					+ "	<input type=hidden name=\"CHPHONE\" value=\"" + PHONE + "\"> "
					+ "	<input type=hidden name=\"CMPHONE\" value=\"" + PHONE + "\">"
					+ "	<input type=hidden name=\"CADDRESS\" value=\"" + CADDRESS + "\">"
					+ "	<input type=hidden name=\"CZIPCODE\" value=\"" + CZIPCODE + "\">"
					+ "	<input type=hidden name=\"BIRTHDATE\" value=\"" + BIRTHDATE + "\">"
					+ "	<input type=hidden name=\"CCITY\" value=\"" + CCITY + "\">"
					+ "	<input type=hidden name=\"CSTATE\" value=\"" + CSTATE + "\">"
					+ "	<input type=hidden name=\"CCOUNTRY\" value=\"360\">"
					+ "	<input type=hidden name=\"SADDRESS\" value=\"" + CADDRESS + "\">"
					+ "	<input type=hidden name=\"SZIPCODE\" value=\"" + CZIPCODE + "\">"
					+ "	<input type=hidden name=\"SCITY\" value=\"" + CCITY + "\">"
					+ "	<input type=hidden name=\"SSTATE\" value=\"" + CSTATE + "\">"
					+ "	<input type=hidden name=\"SCOUNTRY\" value=\"360\"></FORM>" +

					"<script>document.getElementById(\"order" + WORDS + "\").submit();</script></body>" + "</html>";

			File myFile = new File(Common.REAL_PATH + "/tmp/" + WORDS + ".html");
			System.out.println("Report file = " + myFile.getAbsolutePath());
			myFile.getParentFile().mkdirs();
			FileWriter fileWriter = new FileWriter(myFile);
			fileWriter.write(html);
			fileWriter.close();

			Common.displayWindow("/tmp/" + WORDS + ".html", true, "95%");

		} else {
			// Tampilkan alert kegagalan BESERTA "Informasi Teknis" yang dicatat sendRequest
			// (pola bersama seluruh payment gateway, lihat InfoTeknisPembayaran).
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	/**
	 * Menyimpan record {@link DokuRequest} beserta seluruh rincian {@link DokuRequestDetail}
	 * dan {@link DokuRequestDetailBiaya} terkait ke database, dipanggil oleh {@link
	 * #onSaveDoku} SEBELUM permintaan pembayaran benar-benar dikirim ke gateway Doku (karena
	 * integrasi Doku memakai form-post browser, bukan panggilan HTTP server-to-server —
	 * penyimpanan lokal ini menjadi jejak transaksi yang harus ada terlebih dahulu).
	 *
	 * <p>
	 * Setiap entitas ({@link DokuRequest} induk, lalu setiap {@link DokuRequestDetail} dan
	 * {@link DokuRequestDetailBiaya}) disimpan dalam transaksi Hibernate TERPISAH (begin/save/
	 * commit berulang per entitas, bukan satu transaksi besar) memakai sesi native
	 * ({@link HibernateUtil#currentNativeSession()}). Sebelum menyimpan, riwayat "Informasi
	 * Teknis" kegagalan sebelumnya dibersihkan lewat {@link InfoTeknisPembayaran#bersihkan()}
	 * agar tidak bocor ke pesan kegagalan transaksi yang baru.
	 * </p>
	 *
	 * @param TRANSIDMERCHANT           id transaksi unik sisi merchant (dari
	 *                                  {@link Common#getGeneratedBarCode()}), disimpan sebagai
	 *                                  {@code nama} pada {@link DokuRequest}
	 * @param WORDS                     tanda tangan transaksi (hash SHA1) yang sudah dihitung
	 *                                  oleh {@link #onSaveDoku}, disimpan sebagai {@code trxId}
	 * @param mahasiswa                 mahasiswa pembayar, boleh {@code null}
	 * @param biodataCalonMahasiswa     calon mahasiswa pembayar, boleh {@code null}
	 * @param jenisKegiatan             jenis kegiatan terkait transaksi
	 * @param jadwalPembayaran          jadwal pembayaran yang berlaku
	 * @param semester                  semester terkait transaksi
	 * @param tahunAkademik             tahun akademik terkait transaksi
	 * @param keterangan                keterangan/deskripsi transaksi
	 * @param pengurangan               nilai pengurangan/diskon
	 * @param nilaiBiayaHarusDiBayars   nilai total biaya sebelum pengurangan
	 * @param amount                    nominal akhir yang harus dibayar
	 * @param comments                  ringkasan item biaya (field {@code comments} pada
	 *                                  {@link DokuRequest}, biasanya sama dengan {@code add_info2}
	 *                                  dari {@link #onSaveDoku})
	 * @param dokuRequestDetails        rincian item pembayaran yang akan disimpan
	 * @param dokuRequestDetailBiayas   rincian biaya mentah yang akan disimpan
	 * @return record {@link DokuRequest} yang berhasil disimpan beserta id-nya, atau
	 *         {@code null} bila terjadi kegagalan penyimpanan (transaksi Hibernate yang aktif
	 *         di-rollback dan detail teknis dicatat lewat {@link InfoTeknisPembayaran#catat}
	 *         serta {@link ais.common.ErrorAuditUtil#record})
	 * @throws Exception saat ini tidak pernah dilempar dari badan method (kegagalan ditangani
	 *                    dan dikembalikan sebagai {@code null}); dideklarasikan untuk
	 *                    kompatibilitas dengan pemanggil yang menangani {@code Exception}
	 */
	public static DokuRequest sendRequest(String TRANSIDMERCHANT, String WORDS, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String comments, List<DokuRequestDetail> dokuRequestDetails,
			List<DokuRequestDetailBiaya> dokuRequestDetailBiayas) throws Exception {
		// Pola "Informasi Teknis" kegagalan payment gateway (lihat InfoTeknisPembayaran):
		// bersihkan detail lama agar kegagalan transaksi sebelumnya tidak bocor ke alert ini.
		InfoTeknisPembayaran.bersihkan();

		DokuRequest dokuRequest = new DokuRequest();
		dokuRequest.setNama(TRANSIDMERCHANT);
		dokuRequest.setTrxId(WORDS);
		dokuRequest.setMahasiswa(mahasiswa);
		dokuRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
		dokuRequest.setJenisKegiatan(jenisKegiatan);
		dokuRequest.setJadwalPembayaran(jadwalPembayaran);
		dokuRequest.setSemester(semester);
		dokuRequest.setTahunAkademik(tahunAkademik);
		dokuRequest.setKeterangan(keterangan);
		dokuRequest.setPengurangan(pengurangan);
		dokuRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
		dokuRequest.setAmount(amount);
		dokuRequest.setComments(comments);

		Session session = HibernateUtil.currentNativeSession();
		try {
			session.getTransaction().begin();
			session.save(dokuRequest);
			session.getTransaction().commit();

			for (DokuRequestDetail dokuRequestDetail : dokuRequestDetails) {
				dokuRequestDetail.setDokuRequest(dokuRequest);
				session.getTransaction().begin();
				session.save(dokuRequestDetail);
				session.getTransaction().commit();
			}

			for (DokuRequestDetailBiaya dokuRequestDetailBiaya : dokuRequestDetailBiayas) {
				dokuRequestDetailBiaya.setDokuRequest(dokuRequest);
				session.getTransaction().begin();
				session.save(dokuRequestDetailBiaya);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			// Gagal simpan data transaksi Doku (Doku memakai form-post browser, request ke
			// gateway baru terjadi SETELAH data tersimpan) → catat detailnya lalu kembalikan
			// null agar pemanggil menampilkan alert kegagalan beserta informasi teknisnya.
			InfoTeknisPembayaran.catat("Data transaksi Doku (" + TRANSIDMERCHANT
					+ ") GAGAL disimpan di aplikasi sebelum diteruskan ke gateway: " + e.getClass().getSimpleName()
					+ " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			ais.common.ErrorAuditUtil.record(e, "Doku sendRequest gagal simpan; transId=" + TRANSIDMERCHANT);
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) {
				ais.common.ErrorAuditUtil.record(ignore, "Doku sendRequest rollback gagal; transId=" + TRANSIDMERCHANT);
			}
			return null;
		}

		return dokuRequest;
	}

	/**
	 * Memverifikasi checksum ({@code WORDS}) sebuah notifikasi atau pre-check dari gateway Doku
	 * terhadap {@link DokuRequest} lokal yang menjadi acuannya — gerbang wajib yang harus lulus
	 * SEBELUM data tersebut dipakai untuk keputusan apa pun (mis. membalas {@code "Continue"} di
	 * {@code ais.action.servlet.DokuVerifyServlet}, atau memfinalisasi pembayaran di
	 * {@code ais.action.servlet.DokuResponseServlet}).
	 *
	 * <p>Pada protokol Doku Basic Store, {@code WORDS} dihitung sebagai
	 * {@code SHA1(AMOUNT + doku_key + TRANSIDMERCHANT)} — persis formula yang dipakai
	 * {@link #onSaveDoku} untuk membuat permintaan pembayaran, dan nilai hasilnya disimpan apa
	 * adanya sebagai {@link DokuRequest#getTrxId()} lewat {@link #sendRequest}. Verifikasi ini
	 * karena itu CUKUP mencocokkan {@code words} yang diterima dengan
	 * {@link DokuRequest#getTrxId()} milik baris yang sudah ditemukan pemanggil (lewat {@code trxId}
	 * pada {@code DokuVerifyServlet}, atau lewat {@code nama}/{@code TRANSIDMERCHANT} pada
	 * {@code DokuResponseServlet}) — TIDAK perlu menghitung ulang SHA1 dengan {@code doku_key} SAAT
	 * INI, yang bisa saja sudah dirotasi sejak permintaan dibuat (baris lama tetap terverifikasi
	 * terhadap {@code doku_key} yang berlaku ketika baris itu dibuat). Selain {@code words},
	 * {@code amountText} yang dikirim juga WAJIB sama dengan {@link DokuRequest#getAmount()}
	 * tersimpan, agar nominal transaksi tidak bisa dimanipulasi lewat parameter request meski
	 * {@code words} kebetulan cocok.</p>
	 *
	 * @param dokuRequest baris {@link DokuRequest} kandidat hasil pencarian pemanggil; {@code null}
	 *                    (mis. tidak ditemukan) selalu dijawab tidak valid
	 * @param words       nilai parameter {@code WORDS} mentah dari request masuk; {@code null}
	 *                    atau kosong selalu dijawab tidak valid
	 * @param amountText  nilai parameter {@code AMOUNT} mentah dari request masuk (format desimal
	 *                    Doku, mis. {@code "150000.00"})
	 * @return {@code true} hanya bila {@code dokuRequest} bukan {@code null}, {@code words} cocok
	 *         PERSIS (case-sensitive, setelah di-trim) dengan {@link DokuRequest#getTrxId()}, dan
	 *         {@code amountText} berhasil di-parse serta sama (dibulatkan ke satuan rupiah) dengan
	 *         {@link DokuRequest#getAmount()}; {@code false} untuk selain itu (termasuk bila
	 *         {@code amountText} tidak bisa di-parse)
	 */
	public static boolean verifikasiChecksum(DokuRequest dokuRequest, String words, String amountText) {
		if (dokuRequest == null || words == null || words.trim().isEmpty()) {
			return false;
		}
		if (!dokuRequest.getTrxId().equals(words.trim())) {
			return false;
		}
		if (amountText == null || amountText.trim().isEmpty()) {
			return false;
		}
		try {
			int amount = (int) Double.parseDouble(amountText.trim());
			return amount == dokuRequest.getAmount().intValue();
		} catch (Exception e) {
			return false;
		}
	}

}
