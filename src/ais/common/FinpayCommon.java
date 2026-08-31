package ais.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.report.Report;
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
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayRequestDetail;
import ais.database.model.finpay.FinpayRequestDetailBiaya;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

/**
 * Utilitas integrasi <b>payment gateway Finpay</b> (Payment Gateway Indonesia) untuk pembayaran
 * mahasiswa dan calon mahasiswa di AIS: menyusun rincian biaya yang akan dibayar dari komponen
 * ZK (grid cicilan/biaya), membentuk dan menandatangani permintaan pembayaran sesuai spesifikasi
 * API Finpay, mengirimkannya lewat HTTP POST, lalu menyimpan hasilnya (kode pembayaran, status)
 * sebagai entitas {@link ais.database.model.finpay.FinpayRequest} beserta rinciannya
 * ({@link ais.database.model.finpay.FinpayRequestDetail}/
 * {@link ais.database.model.finpay.FinpayRequestDetailBiaya}).
 *
 * <h2>Dua kelompok method</h2>
 * <p>
 * Kelas ini terbagi menjadi dua kelompok fungsi yang saling melengkapi dalam satu alur pembayaran:
 * </p>
 * <ul>
 * <li><b>Penyusun rincian biaya dari UI ZK</b> — {@link #populateDetailBiaya(Grid, List)},
 * {@link #populateFinpayRequestDetailDariDetailBiaya(List)}, dan
 * {@link #populateFinpayRequestDetail(Grid, Mahasiswa, Integer, JadwalPembayaran)} membaca
 * nilai-nilai yang sudah dimasukkan/dihitung pengguna pada grid ZK (kolom biaya, cicilan) dan
 * mengonversinya menjadi objek {@link ais.database.model.finpay.FinpayRequestDetail}/
 * {@link ais.database.model.finpay.FinpayRequestDetailBiaya} siap kirim, termasuk menghitung ulang
 * denda keterlambatan lewat
 * {@link ais.database.model.PengaturanPembayaranBulanan#checkDenda(Double, java.util.Date,
 * JadwalPembayaran, JenisKegiatan)} untuk cicilan baru.</li>
 * <li><b>Pengirim transaksi ke gateway Finpay</b> — {@link #bayarCalonMahasiswa(BiodataCalonMahasiswa,
 * JenisKegiatan)} (alur khusus pendaftaran mahasiswa baru) dan
 * {@link #onSaveFinpay(Double, Mahasiswa, BiodataCalonMahasiswa, JenisKegiatan, JadwalPembayaran,
 * Integer, String, String, Double, Double, List, List, Event)} (titik masuk umum, dipanggil dari
 * layar pembayaran) menyiapkan data pelanggan (email/id/no HP/nama, dengan fallback bila kosong),
 * membentuk payload lewat {@link #generateFinpayPostdata}, mengirimkannya lewat
 * {@link #sendRequest}, lalu menampilkan kode pembayaran ke pengguna dan (asinkron, lewat timer
 * default) mencetak dan mengirim email bukti pembayaran PDF lewat
 * {@link ais.common.CommonEmail#infoBayarViaFinpay(FinpayRequest, File)}.</li>
 * </ul>
 *
 * <h2>Format tanda tangan permintaan (signature)</h2>
 * <p>
 * {@link #generateFinpayPostdata} mengikuti pola tanda tangan khas Finpay: seluruh nilai field
 * (bukan kunci) yang tidak kosong digabung berurutan dengan pemisah {@code "%"}, diubah ke huruf
 * besar, ditambahkan {@code "%" + password merchant} di akhir, lalu di-hash SHA-256 lewat
 * {@link #sha256InJava} untuk menghasilkan {@code mer_signature}. Payload akhir yang dikirim
 * ({@code sendData}) berupa string form-urlencoded manual ({@code key=value} digabung {@code &})
 * yang diawali {@code mer_signature}.
 * </p>
 *
 * <h2>Peringatan keamanan — kredensial merchant Finpay tertanam sebagai nilai default</h2>
 * <p>
 * {@link #generateFinpayPostdata} membaca kredensial merchant lewat
 * {@link Common#getKonfigurasi(String, String)} dengan nilai default tertanam di kode:
 * {@code finpay_merchant_id} default {@code "AK444"} dan {@code finpay_password_merchant} default
 * {@code "ak2016"} (juga {@code finpay_sof_id} default {@code "finpay021"}). Password merchant ini
 * dipakai langsung sebagai bagian dari perhitungan {@code mer_signature} — bila default ini masih
 * aktif di instalasi produksi manapun, siapa pun yang membaca kode sumber dapat memalsukan
 * signature permintaan pembayaran ke gateway Finpay.
 * </p>
 * <p>
 * <b>Lebih parah</b>: {@link #generateFinpayPostdata} juga MENCETAK password merchant ke
 * {@code System.out} dalam bentuk teks polos lewat baris
 * {@code System.out.println("output+password = " + output.toUpperCase())} — variabel
 * {@code output} pada titik ini sudah digabung dengan {@code mer_password}, sehingga baris log ini
 * secara harfiah membocorkan password merchant ke log aplikasi setiap kali transaksi pembayaran
 * dibuat. Ini adalah temuan keamanan aktif (bukan sekadar kode tak terpakai) karena dijalankan
 * pada SETIAP pembentukan permintaan pembayaran.
 * </p>
 * <p>
 * Endpoint default gateway pada {@link #sendRequest} ({@code new_finpay_gateway_url}, default
 * {@code https://sandbox.finpay.co.id/servicescode/api/apiFinpay.php}) mengarah ke domain
 * SANDBOX Finpay, sehingga dampak langsung kemungkinan terbatas pada lingkungan uji — namun
 * kredensial merchant tetap merupakan rahasia otentikasi nyata yang tertanam permanen di kode
 * sumber DAN aktif dicetak ke log runtime. Sesuai cakupan pekerjaan dokumentasi ini, nilai
 * maupun baris pencatatan tersebut TIDAK diubah — direkomendasikan agar tim terkait meninjau log
 * aplikasi yang sudah terlanjur berjalan (kemungkinan sudah memuat password merchant), merotasi
 * {@code finpay_password_merchant} di sisi Finpay, dan menghapus/menutupi baris pencatatan
 * tersebut secara terpisah dari pekerjaan dokumentasi ini.
 * </p>
 */
public class FinpayCommon {

	/** Instans penghitung hash SHA-256 (dipakai untuk menandatangani permintaan Finpay di {@link #generateFinpayPostdata}) yang dibagi statis oleh seluruh pemanggil kelas ini. */
	public static SHA256InJava sha256InJava = new SHA256InJava();

	/**
	 * Membaca setiap baris {@link Row} yang <b>terlihat</b> (tidak {@code isVisible()==false}) pada
	 * {@code gridss} untuk membentuk daftar {@link FinpayRequestDetailBiaya}. Nilai biaya diambil
	 * dengan prioritas: (1) bila komponen input pada baris ({@code attribute "tag"}) berupa
	 * {@link Doublebox} dan item biayanya {@link ItemBiaya#getNilaiBisaDiubah() dapat diubah},
	 * pakai nilai dari kotak input tersebut; (2) bila komponennya {@link Label}, parse teksnya
	 * lewat {@link Common#numberFormat}; (3) sebaliknya, pakai
	 * {@link DetailBiaya#getNilaiBiayaBaru()} atau {@link DetailBiaya#getNilaiBiaya()} sebagai
	 * nilai dasar. Khusus item biaya bertipe {@link ItemBiaya#DIKALI_NILAI_MINUS}, nilai akhirnya
	 * ditimpa dari {@code pengurangan} yang cocok (dicocokkan lewat id {@link DetailBiaya} yang
	 * disimpan pada atribut {@code "itemBiaya"} tiap {@link MyDoubleboxMin}).
	 *
	 * @param gridss      grid ZK sumber baris biaya, dengan setiap {@link Row} membawa atribut
	 *                    {@code "myValue"} (berisi {@link DetailBiaya}) dan {@code "tag"} (berisi
	 *                    komponen input/label nilai)
	 * @param pengurangan daftar komponen pengurang untuk item biaya bertipe
	 *                    {@link ItemBiaya#DIKALI_NILAI_MINUS}
	 * @return daftar {@link FinpayRequestDetailBiaya} siap dipakai membentuk permintaan pembayaran
	 */
	@SuppressWarnings("unchecked")
	public static List<FinpayRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas = new ArrayList<FinpayRequestDetailBiaya>();
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FinpayCommon.java:80");
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

				FinpayRequestDetailBiaya finpayRequestDetailBiaya = new FinpayRequestDetailBiaya();
				finpayRequestDetailBiaya.setDetailBiaya(detailBiaya);
				finpayRequestDetailBiaya.setNilai(biaya);
				finpayRequestDetailBiayas.add(finpayRequestDetailBiaya);
			}
		}
		return finpayRequestDetailBiayas;
	}

	/**
	 * Mengonversi daftar {@link FinpayRequestDetailBiaya} (hasil {@link #populateDetailBiaya})
	 * menjadi daftar {@link FinpayRequestDetail} yang setara — dipakai untuk alur pembayaran
	 * berbasis rincian biaya langsung (bukan cicilan), berbeda dari
	 * {@link #populateFinpayRequestDetail} yang membaca dari grid cicilan. Setiap detail diberi
	 * nomor urut ({@code setKe}) mulai 1 sesuai urutan pada daftar masukan, dan tanggal transaksi
	 * diisi dengan waktu saat ini ({@link ais.ui.util.WaktuUtil#getDate()}).
	 *
	 * @param finpayRequestDetailBiayas daftar rincian biaya sumber
	 * @return daftar {@link FinpayRequestDetail} yang setara, siap disertakan pada permintaan
	 *         pembayaran
	 */
	public static List<FinpayRequestDetail> populateFinpayRequestDetailDariDetailBiaya(
			List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas) {
		List<FinpayRequestDetail> finpayRequestDetails = new ArrayList<FinpayRequestDetail>();

		int i = 1;
		for (FinpayRequestDetailBiaya finpayRequestDetailBiaya : finpayRequestDetailBiayas) {
			FinpayRequestDetail finpayRequestDetail = new FinpayRequestDetail();
			finpayRequestDetail.setPengaturanPembayaranBulanan(null);
			finpayRequestDetail.setItemBiaya(finpayRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			finpayRequestDetail.setKeterangan(finpayRequestDetailBiaya.getKeterangan());
			finpayRequestDetail.setNilai(finpayRequestDetailBiaya.getNilai());
			finpayRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			finpayRequestDetail.setKe(i);
			finpayRequestDetails.add(finpayRequestDetail);
			i++;
		}

		return finpayRequestDetails;
	}

	/**
	 * Membaca grid cicilan pembayaran ZK ({@code gridCicilan}) untuk membentuk daftar
	 * {@link FinpayRequestDetail}, hanya menyertakan baris dengan nilai cicilan yang secara
	 * signifikan bukan nol (di luar rentang {@code -0.01}..{@code 0.01}). Untuk setiap baris,
	 * jenis biaya diambil dari pilihan pengguna pada combobox item biaya (bisa berupa
	 * {@link PengaturanPembayaranBulanan} untuk pembayaran bulanan, atau {@link DetailBiaya}
	 * untuk biaya satuan); bila baris merepresentasikan cicilan BARU (belum punya
	 * {@link CicilanPembayaran} tersimpan) pada pengaturan pembayaran bulanan, nilai denda
	 * keterlambatan dihitung ulang di tempat lewat
	 * {@link PengaturanPembayaranBulanan#checkDenda(Double, java.util.Date, JadwalPembayaran,
	 * JenisKegiatan)}, dengan {@code jadwalPembayaran} hanya dipakai bila jadwal tersebut memang
	 * berlaku khusus untuk NIM mahasiswa yang bersangkutan ({@code khususUntukNim} memuat NIM-nya).
	 * Validator cicilan (audit siapa yang mengubah) diisi dari pengguna yang sedang login bila
	 * belum ada validator tersimpan sebelumnya.
	 *
	 * @param gridCicilan     grid ZK sumber baris cicilan, tiap {@link Row} membawa atribut
	 *                        {@code "jumlahCicilan"}, {@code "cicilanPembayaran"}, {@code "tanggal"},
	 *                        {@code "itemBiaya"}, dan {@code "keterangan"}
	 * @param mahasiswa       mahasiswa pemilik cicilan, dipakai untuk resolusi nominal modifikasi
	 *                        dan validasi jadwal khusus NIM
	 * @param semester        semester berjalan, dipakai untuk menghitung nominal modifikasi
	 *                        pembayaran bulanan
	 * @param jadwalPembayaran jadwal pembayaran yang berlaku (untuk perhitungan denda), boleh
	 *                         {@code null}
	 * @return daftar {@link FinpayRequestDetail} untuk baris cicilan yang bernilai signifikan
	 */
	public static List<FinpayRequestDetail> populateFinpayRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<FinpayRequestDetail> finpayRequestDetails = new ArrayList<FinpayRequestDetail>();

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

				FinpayRequestDetail finpayRequestDetail = new FinpayRequestDetail();

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

				finpayRequestDetail.setDetailBiaya(detailBiaya);
				finpayRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				finpayRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				finpayRequestDetail.setItemBiaya(itemBiaya);
				finpayRequestDetail.setKeterangan(keterangan.getValue());
				finpayRequestDetail.setNilai(jumlahCicilan.getValue());
				finpayRequestDetail.setTanggal(tanggal.getValue());
				finpayRequestDetail.setKe(i);

				finpayRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				finpayRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, finpayRequestDetail.getTanggal(),
								jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						finpayRequestDetail.setDenda(denda);
						finpayRequestDetail.setNilaiAsli(nom);
					}
				}

				finpayRequestDetails.add(finpayRequestDetail);
				i++;
			}
		}

		return finpayRequestDetails;
	}

	/**
	 * Alur pembayaran khusus <b>pendaftaran mahasiswa baru</b>: menentukan rincian biaya yang
	 * harus dibayar {@code calonMahasiswa} berdasarkan program studi yang relevan (memakai
	 * {@link BiodataCalonMahasiswa#getProdiLulus()} bila sudah ditentukan kelulusannya, atau
	 * jatuh ke pilihan program studi pertama/kedua yang didaftarkan bila belum), diselesaikan
	 * lewat {@link PembayaranUtil#getInstance()}; menentukan jadwal pembayaran yang berlaku lewat
	 * {@link PembayaranUtil#getJadwalPembayaranDanDendaBerdasarkanTahunAkademik}; lalu — bila ada
	 * biaya dan jadwal yang ditemukan — merangkai seluruh rincian menjadi satu permintaan
	 * pembayaran dan mendelegasikannya ke
	 * {@link #onSaveFinpay(Double, Mahasiswa, BiodataCalonMahasiswa, JenisKegiatan,
	 * JadwalPembayaran, Integer, String, String, Double, Double, List, List, Event)} dengan
	 * keterangan tetap {@code "Pembayaran Pendaftaran Mahasiswa Baru"}.
	 *
	 * @param calonMahasiswa calon mahasiswa yang akan membayar biaya pendaftaran
	 * @param jenisKegiatan  jenis kegiatan akademik yang menentukan struktur biaya yang berlaku
	 * @throws Exception diteruskan dari kegagalan resolusi biaya/jadwal atau dari
	 *                    {@link #onSaveFinpay}
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

				List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas = new ArrayList<FinpayRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					FinpayRequestDetailBiaya finpayRequestDetailBiaya = new FinpayRequestDetailBiaya();
					finpayRequestDetailBiaya.setDetailBiaya(detailBiaya);
					finpayRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					finpayRequestDetailBiayas.add(finpayRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += finpayRequestDetailBiaya.getNilai();
				}

				FinpayCommon.onSaveFinpay(nilaiBiayaHarusDiBayars, null, calonMahasiswa, jenisKegiatan,
						jadwalPembayaran, 1, calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru",
						0.0, nilaiBiayaHarusDiBayars,
						FinpayCommon.populateFinpayRequestDetailDariDetailBiaya(finpayRequestDetailBiayas),
						finpayRequestDetailBiayas, null);

			}
		}

	}

	/**
	 * Titik masuk umum pengiriman transaksi ke gateway Finpay, dipanggil dari layar pembayaran
	 * mahasiswa maupun calon mahasiswa. Berhenti lebih awal ({@code return false}) tanpa mengirim
	 * apa pun bila nominal {@code amn} kurang dari 0.01 (dianggap tidak ada tagihan). Menyiapkan
	 * data pelanggan (email/id/no HP/nama) dari {@code mahasiswa} atau {@code biodataCalonMahasiswa}
	 * (salah satu wajib diisi) dengan fallback: email kosong diganti {@code "<id>@info.com"}, no HP
	 * kosong diganti {@code "081300000"}. Membentuk nomor invoice baru lewat
	 * {@link Common#getGeneratedBarCode()}, menyusun payload lewat {@link #generateFinpayPostdata},
	 * lalu mengirimkannya lewat {@link #sendRequest}.
	 *
	 * <p>
	 * Bila gateway mengembalikan kode pembayaran yang valid, ditampilkan dialog informasi kode
	 * pembayaran ke pengguna ({@link MyMessageboxConfig#show}); setelah dialog ditutup, sebuah
	 * timer default ({@link Common#createDefaultTimer}) menjalankan pembuatan bukti pembayaran PDF
	 * ({@link Report#generatePDFReport}/{@link Report#generateFileReport}) secara asinkron, lalu
	 * mengirimkannya lewat email ({@link ais.common.CommonEmail#infoBayarViaFinpay}) — kegagalan
	 * pengiriman email pada tahap ini ditangkap dan hanya dicatat, tidak menggagalkan transaksi
	 * yang sudah berhasil. Bila gateway TIDAK mengembalikan kode pembayaran valid, ditampilkan
	 * dialog peringatan berisi {@link InfoTeknisPembayaran#pesanGagal()} (pesan teknis kegagalan
	 * yang sudah dicatat oleh {@link #sendRequest}).
	 * </p>
	 *
	 * @param amn                        nominal tagihan yang harus dibayar; method tidak melakukan
	 *                                   apa pun bila nilainya di bawah 0.01
	 * @param mahasiswa                  mahasiswa pembayar, boleh {@code null} bila yang membayar
	 *                                   adalah calon mahasiswa
	 * @param biodataCalonMahasiswa      calon mahasiswa pembayar, boleh {@code null} bila yang
	 *                                   membayar adalah mahasiswa aktif
	 * @param jenisKegiatan              jenis kegiatan akademik terkait transaksi
	 * @param jadwalPembayaran           jadwal pembayaran yang berlaku, disimpan pada
	 *                                   {@link FinpayRequest} untuk audit
	 * @param semester                   semester berjalan, disimpan pada {@link FinpayRequest}
	 * @param tahunAkademik              tahun akademik berjalan, disimpan pada {@link FinpayRequest}
	 * @param keterangan                 deskripsi transaksi, disimpan pada {@link FinpayRequest}
	 * @param pengurangan                nilai potongan/diskon yang berlaku, disimpan untuk audit
	 * @param nilaiBiayaHarusDiBayars    total nilai biaya sebelum potongan, disimpan untuk audit
	 * @param finpayRequestDetails       rincian pembayaran per item, disimpan sebagai anak
	 *                                   {@link FinpayRequest}
	 * @param finpayRequestDetailBiayas  rincian biaya per item, disimpan sebagai anak
	 *                                   {@link FinpayRequest}
	 * @param event                      event ZK pemicu (tidak dipakai langsung oleh isi method
	 *                                   ini, hanya bagian dari tanda tangan handler)
	 * @return {@code true} bila permintaan diproses (baik berhasil maupun ditolak gateway dengan
	 *         dialog peringatan ditampilkan); {@code false} hanya bila {@code amn} di bawah 0.01
	 * @throws Exception diteruskan dari kegagalan {@link #sendRequest} (koneksi gateway gagal,
	 *                    respons tidak valid, atau kegagalan penyimpanan lokal)
	 */
	@SuppressWarnings({})
	public static boolean onSaveFinpay(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<FinpayRequestDetail> finpayRequestDetails,
			List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas, Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String add_info1 = "";
		String add_info2 = "";
		String add_info3 = "";

		String amount = amn.intValue() + "";
		if (mahasiswa != null) {
			add_info1 = mahasiswa.getNama() + "-" + mahasiswa.getNim();
			add_info2 = mahasiswa.getJurusan().getNama();
			add_info3 = mahasiswa.getJurusan().getFakultas().getNama();
		} else if (biodataCalonMahasiswa != null) {
			add_info1 = biodataCalonMahasiswa.getNama() + "-" + biodataCalonMahasiswa.getNoRegistrasi();
			add_info2 = biodataCalonMahasiswa.getNoUjian() == null ? "" : biodataCalonMahasiswa.getNoUjian();
			add_info3 = "";
		}
		String add_info4 = "";
		String add_info5 = "";

		String cust_email = "";
		String cust_id = "";
		String cust_msisdn = "";
		String cust_name = "";

		if (mahasiswa != null) {
			try {
				cust_email = mahasiswa.getEmail().trim().isEmpty() ? mahasiswa.getNim() + "@info.com"
						: mahasiswa.getEmail().split(",")[0];
			} catch (Exception e) {
				cust_email = mahasiswa.getEmail().trim().isEmpty() ? mahasiswa.getNim() + "@info.com"
						: mahasiswa.getEmail().trim();
			}
			cust_id = mahasiswa.getNim();
			cust_name = mahasiswa.getNama();
			cust_msisdn = mahasiswa.getTelp() == null || mahasiswa.getTelp().trim().isEmpty() ? "081300000"
					: mahasiswa.getTelp().trim();
		} else if (biodataCalonMahasiswa != null) {
			try {
				cust_email = biodataCalonMahasiswa.getEmail().trim().isEmpty()
						? biodataCalonMahasiswa.getNim() + "@info.com"
						: biodataCalonMahasiswa.getEmail().split(",")[0];
			} catch (Exception e) {
				cust_email = biodataCalonMahasiswa.getEmail().trim().isEmpty()
						? biodataCalonMahasiswa.getNim() + "@info.com"
						: biodataCalonMahasiswa.getEmail().trim();
			}
			cust_id = biodataCalonMahasiswa.getNoRegistrasi();
			cust_name = biodataCalonMahasiswa.getNama();
			cust_msisdn = biodataCalonMahasiswa.getHp() == null || biodataCalonMahasiswa.getHp().trim().isEmpty()
					? "081300000"
					: biodataCalonMahasiswa.getHp().trim();
		}

		String invoice = Common.getGeneratedBarCode();
		TreeMap<String, String> data = FinpayCommon.generateFinpayPostdata(invoice, amount, add_info1, add_info2,
				add_info3, add_info4, add_info5, cust_email, cust_id, cust_msisdn, cust_name, null);

		final FinpayRequest finpayRequest = FinpayCommon.sendRequest(data, mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, finpayRequestDetails, finpayRequestDetailBiayas);
		if (finpayRequest != null && finpayRequest.getPaymentCode() != null
				&& !finpayRequest.getPaymentCode().trim().isEmpty()) {

			final String informasiPembayaran = Common
					.getKonfigurasi("finpay_payment_info", "http://portalfinpay.com/index.php/bank").getNilai();

			MyMessageboxConfig.show("Kode pembayaran Anda adalah " + finpayRequest.getPaymentCode()
					+ " dengan tagihan sebesar " + Common.numberFormat.get().format(amn)
					+ "\n\nAnda dapat membayar tagihan ini dengan memasukkan kode \"" + finpayRequest.getPaymentCode()
					+ "\" di semua channel Finpay.\nUntuk informasi lebih lanjut bisa dilihat di "
					+ informasiPembayaran, "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "unchecked", "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									String info = "Kode Pembayaran\t\t: " + finpayRequest.getPaymentCode() + "\n";
									info += "Kode invoice\t\t\t: " + finpayRequest.getInvoice() + "\n";
									info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
									info += "Info Pembayaran \t: " + informasiPembayaran + "\n\n";
									if (finpayRequest.getMahasiswa() != null) {
										info += "NIM \t\t\t\t: " + finpayRequest.getMahasiswa().getNim() + "\n";
										info += "Nama \t\t\t\t: " + finpayRequest.getMahasiswa().getNama() + "\n";
									} else if (finpayRequest.getBiodataCalonMahasiswa() != null) {
										info += "No. Reg \t\t\t: "
												+ finpayRequest.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
										if (finpayRequest.getBiodataCalonMahasiswa().getNoUjian() != null) {
											info += "No. Ujian \t\t\t: "
													+ finpayRequest.getBiodataCalonMahasiswa().getNoUjian() + "\n";
										}
										info += "Nama \t\t\t\t: " + finpayRequest.getBiodataCalonMahasiswa().getNama()
												+ "\n";
									}

									Map parameters = ais.common.HashMapGenerator.getRand();
									parameters.put("tanggal", finpayRequest.getTanggal_dirubah());
									parameters.put("finpayRequest", finpayRequest.getId());
									parameters.put("info", info);
									Report.generatePDFReport(Report.PDF, parameters, "Bukti_Finpay_Mahasiswa",
											ais.ui.util.WaktuUtil.getDate());

									try {
										File file = Report.generateFileReport(Report.PDF, parameters,
												"Bukti_Finpay_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),
												Common.locale);
										CommonEmail.infoBayarViaFinpay(finpayRequest, file);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FinpayCommon.java:374");

									}

								}
							}, "Menyiapkan pembayaran via finpay..");

						}
					});
		} else {
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	/**
	 * Menyusun payload permintaan Finpay lengkap dengan tanda tangan {@code mer_signature}. Setiap
	 * field {@code add_info1}..{@code add_info5} dipotong maksimum 29 karakter,
	 * {@code cust_email}/{@code cust_id} maksimum 49 karakter, {@code cust_msisdn}/{@code cust_name}
	 * maksimum 32 karakter (batasan panjang field API Finpay). Kredensial merchant
	 * ({@code finpay_merchant_id}, {@code finpay_password_merchant}, {@code finpay_sof_id}) dibaca
	 * dari konfigurasi dengan nilai default tertanam — lihat peringatan keamanan pada javadoc
	 * kelas.
	 *
	 * <p>
	 * {@code sof_type} otomatis ditentukan dari {@code payment_code}: {@code null} berarti
	 * permintaan pembuatan pembayaran baru ({@code "pay"}), sedangkan nilai tidak-null berarti
	 * permintaan pengecekan status pembayaran yang sudah ada ({@code "check"}, dengan
	 * {@code payment_code} disertakan). URL callback ({@code failed_url}/{@code success_url}/
	 * {@code return_url}) dibentuk otomatis dari host request saat ini lewat
	 * {@link Common#getRequestHostWithProtocol()}.
	 * </p>
	 * <p>
	 * Signature dihitung dengan menggabung seluruh NILAI field yang tidak kosong (bukan field
	 * kosong) memakai pemisah {@code "%"} sesuai urutan penyisipan {@link TreeMap} (terurut
	 * alfabetis berdasarkan kunci), diubah ke huruf besar, ditambah {@code "%" + password merchant}
	 * di akhir, lalu di-hash SHA-256. Lihat peringatan keamanan pada javadoc kelas: proses ini
	 * MENCETAK password merchant ke {@code System.out} dalam bentuk teks polos.
	 * </p>
	 *
	 * @param invoice      nomor invoice unik transaksi
	 * @param amount       nominal tagihan (string angka bulat)
	 * @param add_info1    info tambahan 1 (lazim: nama-NIM/no registrasi)
	 * @param add_info2    info tambahan 2 (lazim: jurusan/no ujian)
	 * @param add_info3    info tambahan 3 (lazim: fakultas)
	 * @param add_info4    info tambahan 4 (tidak dipakai pemanggil saat ini, selalu kosong)
	 * @param add_info5    info tambahan 5 (tidak dipakai pemanggil saat ini, selalu kosong)
	 * @param cust_email   email pelanggan
	 * @param cust_id      id pelanggan (NIM/no registrasi)
	 * @param cust_msisdn  nomor HP pelanggan
	 * @param cust_name    nama pelanggan
	 * @param payment_code kode pembayaran yang sudah ada (untuk permintaan cek status), atau
	 *                     {@code null} untuk permintaan pembuatan pembayaran baru
	 * @return peta data permintaan terurut, dengan tambahan kunci {@code "sendData"} berisi string
	 *         form-urlencoded siap kirim (diawali {@code mer_signature})
	 */
	public static TreeMap<String, String> generateFinpayPostdata(String invoice, String amount, String add_info1,
			String add_info2, String add_info3, String add_info4, String add_info5, String cust_email, String cust_id,
			String cust_msisdn, String cust_name, String payment_code) {

		add_info1 = Common.maxPanjang(add_info1, 29);
		add_info2 = Common.maxPanjang(add_info2, 29);
		add_info3 = Common.maxPanjang(add_info3, 29);
		add_info4 = Common.maxPanjang(add_info4, 29);
		add_info5 = Common.maxPanjang(add_info5, 29);

		String merchant_id = Common.getKonfigurasi("finpay_merchant_id", "AK444").getNilai();
		String mer_password = Common.getKonfigurasi("finpay_password_merchant", "ak2016").getNilai();

		String timeout = Common.getKonfigurasi("finpay_timeout", "30").getNilai();

		String sof_id = Common.getKonfigurasi("finpay_sof_id", "finpay021").getNilai();
		// String sof_type = Common.getKonfigurasi("finpay_sof_type",
		// "pay").getNilai();

		String trans_date = Common.datetimeFormat1s.get().format(ais.ui.util.WaktuUtil.getDate());

		TreeMap<String, String> data = new TreeMap<String, String>();
		data.put("amount", amount);
		data.put("invoice", invoice);
		data.put("merchant_id", merchant_id);
		data.put("sof_id", sof_id);

		if (payment_code == null) {
			data.put("sof_type", "pay");
		} else {
			data.put("sof_type", "check");
			data.put("payment_code", payment_code);
		}
		data.put("timeout", timeout);
		data.put("trans_date", trans_date);

		data.put("add_info1", add_info1);
		data.put("add_info2", add_info2);
		data.put("add_info3", add_info3);
		data.put("add_info4", add_info4);
		data.put("add_info5", add_info5);

		cust_email = Common.maxPanjang(cust_email, 49);
		data.put("cust_email", cust_email);

		cust_id = Common.maxPanjang(cust_id, 49);
		data.put("cust_id", cust_id);

		cust_msisdn = Common.maxPanjang(cust_msisdn, 32);
		data.put("cust_msisdn", cust_msisdn);

		cust_name = Common.maxPanjang(cust_name, 32);
		data.put("cust_name", cust_name);

		String failed_url = Common.getRequestHostWithProtocol() + "/common/finpay/batal.zul";
		String success_url = Common.getRequestHostWithProtocol() + "/common/finpay/return.zul";
		String return_url = Common.getRequestHostWithProtocol()
				+ Common.getKonfigurasi("finpay_path_url_response", "/FinPayResponse").getNilai();

		data.put("failed_url", failed_url);
		data.put("success_url", success_url);
		data.put("return_url", return_url);

		String output = "";
		for (String s : data.values()) {
			if (!s.trim().isEmpty()) {
				output += output.trim().isEmpty() ? s : "%" + s;
			}
		}

		System.out.println("output = " + output.toUpperCase());

		output = output.toUpperCase() + "%" + mer_password;

		System.out.println("output+password = " + output.toUpperCase());

		String mer_signature = sha256InJava.getSHA256Hash(output);

		String sendData = "mer_signature=" + mer_signature;

		for (String s : data.keySet()) {
			sendData += ("&" + s + "=" + data.get(s));
		}

		System.out.println("sendData = " + sendData);
		data.put("sendData", sendData);
		return data;
	}

	/**
	 * Implementasi kanonik pengiriman permintaan ke gateway Finpay lewat HTTP POST mentah
	 * ({@link HttpURLConnection}, bukan pustaka HTTP tingkat tinggi), lalu menyimpan hasilnya
	 * sebagai {@link FinpayRequest} beserta seluruh rincian ({@link FinpayRequestDetail}/
	 * {@link FinpayRequestDetailBiaya}) dalam beberapa transaksi Hibernate terpisah (satu
	 * transaksi per baris, bukan satu transaksi besar).
	 *
	 * <p>
	 * Sebelum mengirim, riwayat kegagalan lama dibersihkan lewat
	 * {@link InfoTeknisPembayaran#bersihkan()} agar dialog kegagalan (bila terjadi) tidak
	 * menampilkan penyebab dari transaksi sebelumnya. Endpoint gateway dibaca dari konfigurasi
	 * {@code new_finpay_gateway_url} (default domain sandbox Finpay, lihat javadoc kelas). Setiap
	 * kegagalan pada tahap koneksi/pengiriman ({@link java.net.ConnectException} — gateway tidak
	 * terjangkau, {@link java.net.SocketTimeoutException} — gateway tidak merespons, atau
	 * exception lain), tahap parsing JSON respons, maupun tahap penyimpanan lokal setelah gateway
	 * menerima transaksi, dicatat secara spesifik dan deskriptif lewat
	 * {@link InfoTeknisPembayaran#catat(String)} (untuk ditampilkan ke pengguna lewat
	 * {@link InfoTeknisPembayaran#pesanGagal()} di pemanggil) SEBELUM exception aslinya dilempar
	 * ulang — alur kegagalan sendiri tidak diubah, hanya diperkaya dengan konteks diagnostik.
	 * Bila {@code payment_code} pada respons kosong (gateway menolak permintaan tanpa exception),
	 * kode dan pesan status dari server dicatat sebagai peringatan meski method tetap melanjutkan
	 * membentuk {@link FinpayRequest} dari respons tersebut.
	 * </p>
	 *
	 * @param data                       peta data permintaan hasil {@link #generateFinpayPostdata},
	 *                                   HARUS memuat kunci {@code "sendData"} berisi payload
	 *                                   form-urlencoded siap kirim
	 * @param mahasiswa                  mahasiswa pembayar, boleh {@code null}
	 * @param biodataCalonMahasiswa      calon mahasiswa pembayar, boleh {@code null}
	 * @param jenisKegiatan              jenis kegiatan akademik terkait, disimpan pada hasil
	 * @param jadwalPembayaran           jadwal pembayaran terkait, disimpan pada hasil
	 * @param semester                   semester berjalan, disimpan pada hasil
	 * @param tahunAkademik              tahun akademik berjalan, disimpan pada hasil
	 * @param keterangan                 deskripsi transaksi, disimpan pada hasil
	 * @param pengurangan                nilai potongan yang berlaku, disimpan pada hasil
	 * @param nilaiBiayaHarusDiBayars    total biaya sebelum potongan, disimpan pada hasil
	 * @param amount                     nominal tagihan final, disimpan pada hasil
	 * @param finpayRequestDetails       rincian pembayaran per item yang akan dikaitkan ke
	 *                                   {@link FinpayRequest} yang tersimpan
	 * @param finpayRequestDetailBiayas  rincian biaya per item yang akan dikaitkan ke
	 *                                   {@link FinpayRequest} yang tersimpan
	 * @return {@link FinpayRequest} yang sudah tersimpan di database, memuat kode pembayaran dan
	 *         status hasil respons gateway
	 * @throws IOException  diteruskan dari kegagalan koneksi/IO HTTP mentah
	 * @throws Exception    diteruskan dari kegagalan parsing JSON respons atau kegagalan
	 *                      penyimpanan Hibernate
	 */
	public static FinpayRequest sendRequest(TreeMap<String, String> data, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, List<FinpayRequestDetail> finpayRequestDetails,
			List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas) throws IOException, Exception {
		// Bersihkan detail kegagalan lama agar alert tidak menampilkan penyebab transaksi sebelumnya.
		InfoTeknisPembayaran.bersihkan();
		// curl_init and url
		String strURL = Common
				.getKonfigurasi("new_finpay_gateway_url", "https://sandbox.finpay.co.id/servicescode/api/apiFinpay.php")
				.getNilai();
		URL url = new URL(strURL);

		String res;
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);

			String postData = data.get("sendData");

			con.setRequestProperty("Content-length", String.valueOf(postData.length()));

			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(postData);
			output.close();

			// "Post data send ... waiting for reply");
			int code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Response    (Code):" + code);
			System.out.println("Response (Message):" + con.getResponseMessage());

			// read the response
			DataInputStream input = new DataInputStream(con.getInputStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			input.close();

			res = resultBuf.toString();
		} catch (java.net.ConnectException ce) {
			// Gateway Finpay tak bisa dihubungi (unreachable/refused). Alur tetap seperti semula:
			// exception dilempar ulang; catat penyebabnya untuk alert pemanggil.
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway Finpay (" + strURL + "): "
					+ InfoTeknisPembayaran.potong(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			throw ce;
		} catch (java.net.SocketTimeoutException te) {
			// Gateway Finpay tidak merespons dalam batas waktu baca (timeout).
			InfoTeknisPembayaran.catat("Gateway Finpay (" + strURL + ") tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			throw te;
		} catch (Exception e) {
			// Kegagalan request/baca respons lain — catat lalu lempar ulang (alur tetap sama).
			InfoTeknisPembayaran.catat("Gagal memproses request/respons Finpay (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			throw e;
		}

		System.out.println("==> res param => " + res);

		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(res);
		} catch (Exception e) {
			// Respons Finpay tidak bisa diparse sebagai JSON — catat potongan respons mentahnya.
			InfoTeknisPembayaran.catat("Gagal memproses request/respons Finpay (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200)
					+ ". Respons server: " + InfoTeknisPembayaran.potong(res, 300));
			throw e;
		}

		System.out.println("==> response jsonObject => " + jsonObject);

		String payment_code_respons = jsonObject.optString("payment_code", "");
		if (payment_code_respons == null || payment_code_respons.trim().isEmpty()) {
			// Finpay menolak/menggagalkan permintaan (payment_code kosong) — catat kode + pesan
			// server apa adanya agar alert pemanggil tidak generik. Alur tetap seperti semula.
			String status_code = jsonObject.optString("status_code", "");
			String status_desc = jsonObject.optString("status_desc", "");
			InfoTeknisPembayaran.catat("Server Finpay menolak permintaan, kode status=" + status_code
					+ (status_desc.trim().isEmpty() ? "" : ", pesan=" + status_desc.trim())
					+ ". Respons server: " + InfoTeknisPembayaran.potong(res, 300) + " (URL: " + strURL + ")");
		}

		FinpayRequest finpayRequest = new FinpayRequest();
		finpayRequest.setNama(data.get("mer_signature"));
		finpayRequest.setTipe(data.get("sof_id"));
		finpayRequest.setMerchant(data.get("merchant_id"));
		finpayRequest.setInvoice(data.get("invoice"));
		finpayRequest.setPaymentCode(jsonObject.getString("payment_code"));
		finpayRequest.setResultCode(jsonObject.getString("status_code"));
		finpayRequest.setStatus(jsonObject.getString("status_desc"));
		finpayRequest.setMahasiswa(mahasiswa);
		finpayRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
		finpayRequest.setJenisKegiatan(jenisKegiatan);
		finpayRequest.setJadwalPembayaran(jadwalPembayaran);
		finpayRequest.setSemester(semester);
		finpayRequest.setTahunAkademik(tahunAkademik);
		finpayRequest.setKeterangan(keterangan);
		finpayRequest.setPengurangan(pengurangan);
		finpayRequest.setAmount(amount);
		finpayRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);

		try {
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(finpayRequest);
			session.getTransaction().commit();

			for (FinpayRequestDetail finpayRequestDetail : finpayRequestDetails) {
				finpayRequestDetail.setFinpayRequest(finpayRequest);
				session.getTransaction().begin();
				session.save(finpayRequestDetail);
				session.getTransaction().commit();
			}

			for (FinpayRequestDetailBiaya finpayRequestDetailBiaya : finpayRequestDetailBiayas) {
				finpayRequestDetailBiaya.setFinpayRequest(finpayRequest);
				session.getTransaction().begin();
				session.save(finpayRequestDetailBiaya);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			// Finpay sudah menerima transaksi namun penyimpanan lokal gagal — beri tahu
			// penyebabnya supaya admin memeriksa Error Log/DB, bukan menyalahkan gateway.
			InfoTeknisPembayaran.catat("Transaksi diterima gateway namun GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			throw e;
		}

		HibernateUtil.closeSession();

		return finpayRequest;
	}

}
