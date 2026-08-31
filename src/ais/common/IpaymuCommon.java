package ais.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.json.JSONObject;
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
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.database.model.ipaymu.IpaymuRequestDetailBiaya;
import ais.database.model.ipaymu.IpaymuResponse;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kelas utilitas statis untuk integrasi payment gateway <b>iPaymu</b> di AIS, menangani alur
 * pembayaran biaya kuliah/pendaftaran mahasiswa (mahasiswa aktif maupun calon mahasiswa) mulai
 * dari penyusunan rincian tagihan dari komponen antarmuka ZK, pemanggilan API iPaymu lewat
 * {@link HttpURLConnection} langsung (bukan {@code curl} eksternal seperti pada
 * {@code BRIDataUtil}/{@code DokuCommon}), penyimpanan record permintaan & respons ke database,
 * hingga pengalihan browser pengguna ke halaman pembayaran/redirect iPaymu.
 *
 * <p>
 * Struktur kelas ini SANGAT MIRIP dengan {@code ais.common.DokuCommon} (integrasi payment
 * gateway lain di AIS) — keduanya menyediakan pasangan method {@code populateDetailBiaya},
 * {@code populate*RequestDetailDariDetailBiaya}, {@code populate*RequestDetail},
 * {@code bayarCalonMahasiswa}, {@code onSave*}, dan {@code sendRequest} dengan pola kerja yang
 * identik, hanya berbeda pada model data ({@code Ipaymu*} vs {@code Doku*}) dan mekanisme
 * pemanggilan gateway-nya. Perbedaan penting: iPaymu mendukung DUA MODE transaksi (lihat
 * {@link #onSaveIpaymu}), sedangkan Doku hanya satu mode form-post.
 * </p>
 *
 * <p>
 * Alur kerja umum melibatkan tiga lapisan:
 * </p>
 * <ol>
 * <li><b>Penyusunan rincian tagihan</b> — {@link #populateDetailBiaya(Grid, List)},
 * {@link #populateIpaymuRequestDetailDariDetailBiaya(List)}, dan
 * {@link #populateIpaymuRequestDetail(Grid, Mahasiswa, Integer, JadwalPembayaran)} membaca
 * nilai dari komponen grid ZK yang sudah diisi/diedit pengguna, lalu mengonversinya menjadi
 * objek {@link IpaymuRequestDetail}/{@link IpaymuRequestDetailBiaya}.</li>
 * <li><b>Orkestrasi pembayaran</b> — {@link #bayarCalonMahasiswa(BiodataCalonMahasiswa,
 * JenisKegiatan)} adalah titik masuk khusus alur pendaftaran mahasiswa baru, menghitung detail
 * biaya wajib lewat {@link PembayaranUtil} lalu mendelegasikan ke {@link #onSaveIpaymu}.</li>
 * <li><b>Eksekusi transaksi</b> — {@link #onSaveIpaymu} adalah method inti: menentukan MODE
 * transaksi (langsung Virtual Account vs halaman pembayaran redirect) berdasarkan konfigurasi
 * {@code ipaymu_langsung_menggunakan_virtual_account}, menyusun parameter permintaan sesuai
 * mode, lalu memanggil {@link #sendRequest} yang benar-benar melakukan panggilan HTTP POST ke
 * API iPaymu, mem-parsing respons JSON, menyimpan record {@link IpaymuRequest} (dan
 * {@link IpaymuResponse} bila mode VA langsung menghasilkan nomor VA), lalu mengarahkan browser
 * pengguna ke URL hasil (baik halaman pembayaran iPaymu maupun halaman info VA internal AIS)
 * lewat {@link Common#displayWindow(String, boolean, String)}.</li>
 * </ol>
 *
 * <p>
 * Kegagalan pada {@link #sendRequest} ditangani lewat pola "Informasi Teknis" bersama seluruh
 * payment gateway AIS ({@link InfoTeknisPembayaran}): setiap jenis kegagalan (server menolak
 * dengan kode status non-2xx, koneksi gagal/{@code ConnectException}, timeout/
 * {@code SocketTimeoutException}, respons sukses tapi tanpa data transaksi, kegagalan
 * penyimpanan ke database setelah gateway menerima transaksi) dicatat dengan pesan spesifik
 * lewat {@link InfoTeknisPembayaran#catat(String)} sebelum mengembalikan {@code null}, sehingga
 * {@link #onSaveIpaymu} dapat menampilkan alert kegagalan yang informatif ke pengguna/admin
 * tanpa perlu membuka log server.
 * </p>
 *
 * <p>
 * <b>PERINGATAN KEAMANAN:</b> method {@link #onSaveIpaymu} membaca kredensial integrasi iPaymu
 * dari konfigurasi database dengan NILAI DEFAULT yang ditulis langsung (hardcoded) di kode
 * sumber sebagai fallback: {@code ipaymu_key} (default
 * {@code "HZ2j4j8y112OHd2UVWH60QXfT04Pf1"}) — nilai ini setara dengan API key/secret merchant
 * iPaymu dan dikirim langsung sebagai parameter {@code key} pada setiap permintaan ke API
 * iPaymu. Bila nilai tersebut adalah key produksi sungguhan (bukan sekadar contoh), siapa pun
 * dengan akses baca kode sumber dapat memakai API key tersebut untuk memanggil API iPaymu atas
 * nama merchant ini. Javadoc ini TIDAK mengubah nilai tersebut sesuai instruksi; lihat
 * ringkasan laporan terkait untuk detail lokasi baris agar dapat ditindaklanjuti (mis. hapus
 * default hardcoded, pastikan konfigurasi selalu diisi lewat database/secret store, dan rotasi
 * key di sisi iPaymu bila memang bocor).
 * </p>
 */
public class IpaymuCommon {

	/**
	 * Menyusun daftar {@link IpaymuRequestDetailBiaya} dari baris-baris sebuah komponen
	 * {@link Grid} ZK yang menampilkan rincian biaya, sambil menerapkan aturan pengurangan
	 * khusus untuk item biaya bertipe {@link ItemBiaya#DIKALI_NILAI_MINUS}. Method ini setara
	 * (logika identik) dengan {@code ais.common.DokuCommon#populateDetailBiaya(Grid, List)},
	 * hanya berbeda tipe objek hasil ({@link IpaymuRequestDetailBiaya} vs
	 * {@code DokuRequestDetailBiaya}) — lihat javadoc method tersebut untuk uraian rinci alur
	 * pembacaan nilai per baris.
	 *
	 * @param gridss      komponen {@link Grid} ZK berisi baris rincian biaya
	 * @param pengurangan daftar komponen {@link MyDoubleboxMin} untuk item biaya bertipe
	 *                    {@code DIKALI_NILAI_MINUS}
	 * @return daftar {@link IpaymuRequestDetailBiaya} siap simpan, satu per baris grid yang
	 *         tampak
	 */
	@SuppressWarnings("unchecked")
	public static List<IpaymuRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas = new ArrayList<IpaymuRequestDetailBiaya>();
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/IpaymuCommon.java:78");
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

				IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya = new IpaymuRequestDetailBiaya();
				ipaymuRequestDetailBiaya.setDetailBiaya(detailBiaya);
				ipaymuRequestDetailBiaya.setNilai(biaya);
				ipaymuRequestDetailBiayas.add(ipaymuRequestDetailBiaya);
			}
		}
		return ipaymuRequestDetailBiayas;
	}

	/**
	 * Mengonversi daftar {@link IpaymuRequestDetailBiaya} menjadi daftar
	 * {@link IpaymuRequestDetail} yang siap disimpan, dengan nomor urut berurutan mulai dari 1
	 * dan tanggal diisi tanggal saat ini. Setara logika dengan
	 * {@code DokuCommon#populateDokuRequestDetailDariDetailBiaya(List)}.
	 *
	 * @param ipaymuRequestDetailBiayas daftar rincian biaya sumber
	 * @return daftar {@link IpaymuRequestDetail} yang setara, dengan
	 *         {@code pengaturanPembayaranBulanan} selalu {@code null}
	 */
	public static List<IpaymuRequestDetail> populateIpaymuRequestDetailDariDetailBiaya(
			List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas) {
		List<IpaymuRequestDetail> ipaymuRequestDetails = new ArrayList<IpaymuRequestDetail>();

		int i = 1;
		for (IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya : ipaymuRequestDetailBiayas) {
			IpaymuRequestDetail ipaymuRequestDetail = new IpaymuRequestDetail();
			ipaymuRequestDetail.setPengaturanPembayaranBulanan(null);
			ipaymuRequestDetail.setItemBiaya(ipaymuRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			ipaymuRequestDetail.setKeterangan(ipaymuRequestDetailBiaya.getKeterangan());
			ipaymuRequestDetail.setNilai(ipaymuRequestDetailBiaya.getNilai());
			ipaymuRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			ipaymuRequestDetail.setKe(i);
			ipaymuRequestDetails.add(ipaymuRequestDetail);
			i++;
		}

		return ipaymuRequestDetails;
	}

	/**
	 * Menyusun daftar {@link IpaymuRequestDetail} dari baris-baris grid cicilan pembayaran ZK,
	 * hanya menyertakan baris yang jumlah cicilannya diisi. Logika penentuan validator,
	 * item biaya/pengaturan pembayaran bulanan, serta perhitungan ulang denda & nilai asli
	 * untuk cicilan baru identik dengan
	 * {@code DokuCommon#populateDokuRequestDetail(Grid, Mahasiswa, Integer, JadwalPembayaran)}
	 * — lihat javadoc method tersebut untuk uraian rinci.
	 *
	 * @param gridCicilan      komponen {@link Grid} ZK berisi baris cicilan pembayaran
	 * @param mahasiswa        mahasiswa pemilik cicilan
	 * @param semester         semester berjalan
	 * @param jadwalPembayaran jadwal pembayaran yang berlaku, boleh {@code null}
	 * @return daftar {@link IpaymuRequestDetail} untuk baris cicilan yang diisi
	 */
	public static List<IpaymuRequestDetail> populateIpaymuRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<IpaymuRequestDetail> ipaymuRequestDetails = new ArrayList<IpaymuRequestDetail>();

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

				IpaymuRequestDetail ipaymuRequestDetail = new IpaymuRequestDetail();

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

				ipaymuRequestDetail.setDetailBiaya(detailBiaya); 
				ipaymuRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				ipaymuRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				ipaymuRequestDetail.setItemBiaya(itemBiaya);
				ipaymuRequestDetail.setKeterangan(keterangan.getValue());
				ipaymuRequestDetail.setNilai(jumlahCicilan.getValue());
				ipaymuRequestDetail.setTanggal(tanggal.getValue());
				ipaymuRequestDetail.setKe(i);

				ipaymuRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				ipaymuRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, ipaymuRequestDetail.getTanggal(),
								jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						ipaymuRequestDetail.setDenda(denda);
						ipaymuRequestDetail.setNilaiAsli(nom);
					}
				}

				ipaymuRequestDetails.add(ipaymuRequestDetail);
				i++;
			}
		}

		return ipaymuRequestDetails;
	}

	/**
	 * Titik masuk alur pembayaran biaya pendaftaran untuk seorang calon mahasiswa lewat
	 * gateway iPaymu. Logikanya identik dengan
	 * {@code DokuCommon#bayarCalonMahasiswa(BiodataCalonMahasiswa, JenisKegiatan)}: menentukan
	 * program studi acuan, menghitung {@link DetailBiaya} yang wajib dibayar lewat
	 * {@link PembayaranUtil#getDetailBiayaCalonMahasiswa}, menentukan jadwal pembayaran lewat
	 * {@link PembayaranUtil#getJadwalPembayaranDanDendaBerdasarkanTahunAkademik}, lalu
	 * mendelegasikan eksekusi transaksi ke {@link #onSaveIpaymu}.
	 *
	 * @param calonMahasiswa data calon mahasiswa yang akan membayar biaya pendaftaran
	 * @param jenisKegiatan  jenis kegiatan/gelombang penerimaan yang menentukan komponen biaya
	 *                       yang berlaku
	 * @throws Exception diteruskan dari kegagalan perhitungan biaya/jadwal pembayaran
	 *                    ({@link PembayaranUtil}) atau dari {@link #onSaveIpaymu}
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

				List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas = new ArrayList<IpaymuRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya = new IpaymuRequestDetailBiaya();
					ipaymuRequestDetailBiaya.setDetailBiaya(detailBiaya);
					ipaymuRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					ipaymuRequestDetailBiayas.add(ipaymuRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += ipaymuRequestDetailBiaya.getNilai();
				}

				IpaymuCommon.onSaveIpaymu(nilaiBiayaHarusDiBayars, null, calonMahasiswa, jenisKegiatan,
						jadwalPembayaran, 1, calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru",
						0.0, nilaiBiayaHarusDiBayars,
						IpaymuCommon.populateIpaymuRequestDetailDariDetailBiaya(ipaymuRequestDetailBiayas),
						ipaymuRequestDetailBiayas, null);

			}
		}

	}

	/**
	 * Implementasi inti eksekusi transaksi pembayaran lewat gateway iPaymu, mendukung DUA MODE
	 * yang dipilih berdasarkan konfigurasi
	 * {@code ipaymu_langsung_menggunakan_virtual_account}:
	 *
	 * <p>
	 * Bila {@code amn} (nominal yang harus dibayar) kurang dari {@code 0.01}, method langsung
	 * mengembalikan {@code false} tanpa melakukan apa pun.
	 * </p>
	 *
	 * <ul>
	 * <li><b>Mode langsung Virtual Account</b> (konfigurasi aktif) — memanggil endpoint
	 * {@code ipaymu_gateway_url_va} (default {@code GetVa.php}) untuk memperoleh nomor VA
	 * langsung tanpa halaman pembayaran interaktif; parameter berisi {@code key}, {@code amount},
	 * dan {@code notes} (ringkasan item). Bila berhasil (respons memiliki {@code trxId}), browser
	 * diarahkan ke {@link IpaymuRequest#getUrl()} yang sudah disiapkan {@link #sendRequest}
	 * (URL info VA internal AIS bila API tidak mengembalikan URL eksplisit).</li>
	 * <li><b>Mode halaman pembayaran</b> (konfigurasi tidak aktif, default) — memanggil endpoint
	 * {@code ipaymu_gateway_url} (default {@code payment.htm}) dengan parameter lengkap gaya
	 * form pembayaran iPaymu klasik ({@code action=payment}, {@code product}, {@code price},
	 * {@code quantity=1}, {@code comments}, serta URL callback {@code ureturn}/{@code unotify}/
	 * {@code ucancel} yang mengarah ke halaman {@code /common/ipaymu/return.zul},
	 * {@code ipaymu_path_url_response} (default {@code /FinPayResponse}), dan
	 * {@code /common/ipaymu/batal.zul}). Bila berhasil (respons memiliki {@code url}), browser
	 * diarahkan ke halaman pembayaran iPaymu tersebut.</li>
	 * </ul>
	 *
	 * <p>
	 * Kedua mode menyusun {@code add_info1}/{@code add_info2} (ringkasan nama+identitas
	 * pembayar dan daftar item non-cicilan) sebelum memanggil {@link #sendRequest}, dan
	 * menampilkan alert kegagalan beserta "Informasi Teknis" lewat
	 * {@link InfoTeknisPembayaran#pesanGagal()} bila {@link #sendRequest} mengembalikan hasil
	 * tanpa data transaksi yang valid.
	 * </p>
	 *
	 * @param amn                       nominal total yang harus dibayar (rupiah)
	 * @param mahasiswa                 mahasiswa pembayar, boleh {@code null} bila pembayar
	 *                                  adalah calon mahasiswa
	 * @param biodataCalonMahasiswa     calon mahasiswa pembayar, boleh {@code null}
	 * @param jenisKegiatan             jenis kegiatan terkait transaksi
	 * @param jadwalPembayaran          jadwal pembayaran yang berlaku
	 * @param semester                  semester terkait transaksi
	 * @param tahunAkademik             tahun akademik terkait transaksi
	 * @param keterangan                keterangan/deskripsi transaksi
	 * @param pengurangan               nilai pengurangan/diskon yang sudah diperhitungkan
	 * @param nilaiBiayaHarusDiBayars   nilai total biaya sebelum pengurangan
	 * @param ipaymuRequestDetails      rincian item pembayaran
	 * @param ipaymuRequestDetailBiayas rincian biaya mentah
	 * @param event                     event ZK pemicu (tidak dipakai langsung di badan method
	 *                                  ini)
	 * @return {@code true} bila proses (baik sukses menampilkan halaman iPaymu maupun gagal
	 *         dengan alert ditampilkan) selesai dijalankan; {@code false} hanya bila
	 *         {@code amn} kurang dari {@code 0.01}
	 * @throws Exception diteruskan dari kegagalan {@link #sendRequest} atau penyusunan
	 *                    parameter permintaan
	 */
	@SuppressWarnings({})
	public static boolean onSaveIpaymu(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<IpaymuRequestDetail> ipaymuRequestDetails,
			List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas, Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String add_info1 = "";
		String add_info2 = "";

		for (IpaymuRequestDetail detail : ipaymuRequestDetails) {
			if (detail.getIdCicilan() == null) {
				add_info2 += add_info2.isEmpty() ? detail.getKeterangan() : "; " + detail.getKeterangan();
			}
		}

		if (mahasiswa != null) {
			add_info2 = mahasiswa.getNama() + "-" + mahasiswa.getNim() + ", " + add_info2;
		} else if (biodataCalonMahasiswa != null) {
			add_info2 = biodataCalonMahasiswa.getNama()
					+ (biodataCalonMahasiswa.getNoRegistrasi() == null
							|| biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty() ? ""
									: "-" + biodataCalonMahasiswa.getNoRegistrasi())
					+ (biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().isEmpty()
							? ""
							: "-" + biodataCalonMahasiswa.getNoUjian())
					+ ", " + add_info2;
		}

		String amount = amn.intValue() + "";
		if (mahasiswa != null) {
			add_info1 = mahasiswa.getNama() + "-" + mahasiswa.getNim();
		} else if (biodataCalonMahasiswa != null) {
			add_info1 = biodataCalonMahasiswa.getNama() + "-" + biodataCalonMahasiswa.getNoRegistrasi();
		}

		String key = Common.getKonfigurasi("ipaymu_key", "HZ2j4j8y112OHd2UVWH60QXfT04Pf1").getNilai();
		Boolean langsung = Common.bolehKonfigurasi("ipaymu_langsung_menggunakan_virtual_account");

		if (langsung) {
			String urlStr = Common.getKonfigurasi("ipaymu_gateway_url_va", "https://my.ipaymu.com/api/GetVa.php")
					.getNilai();

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("key", key);
			params.put("amount", amount);
			params.put("notes", add_info2);

			String postData = URLBuilder.httpBuildQuery(params, "UTF-8");
			final IpaymuRequest ipaymuRequest = IpaymuCommon.sendRequest(urlStr, postData, mahasiswa,
					biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan,
					pengurangan, nilaiBiayaHarusDiBayars, amn, add_info2, ipaymuRequestDetails,
					ipaymuRequestDetailBiayas);
			if (ipaymuRequest != null && ipaymuRequest.getTrxId() != null
					&& !ipaymuRequest.getTrxId().trim().isEmpty()) {

				Common.displayWindow(ipaymuRequest.getUrl().trim(), true, "95%");

			} else {
				// Tampilkan alert kegagalan BESERTA "Informasi Teknis" yang dicatat sendRequest
				// (pola bersama seluruh payment gateway, lihat InfoTeknisPembayaran).
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}

		} else {
			String urlStr = Common.getKonfigurasi("ipaymu_gateway_url", "https://my.ipaymu.com/payment.htm").getNilai();
			String return_url = Common.getRequestHostWithProtocol()
					+ Common.getKonfigurasi("ipaymu_path_url_response", "/FinPayResponse").getNilai();

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("key", key);
			params.put("action", "payment");
			params.put("product", add_info1);
			params.put("price", amount);
			params.put("quantity", "1");
			params.put("comments", add_info2);
			params.put("ureturn", Common.getRequestHostWithProtocol() + "/common/ipaymu/return.zul?kembalian=1");
			params.put("unotify", return_url);
			params.put("ucancel", Common.getRequestHostWithProtocol() + "/common/ipaymu/batal.zul");
			params.put("format", "json");

			String postData = URLBuilder.httpBuildQuery(params, "UTF-8");
			final IpaymuRequest ipaymuRequest = IpaymuCommon.sendRequest(urlStr, postData, mahasiswa,
					biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan,
					pengurangan, nilaiBiayaHarusDiBayars, amn, add_info2, ipaymuRequestDetails,
					ipaymuRequestDetailBiayas);
			if (ipaymuRequest != null && ipaymuRequest.getUrl() != null && !ipaymuRequest.getUrl().trim().isEmpty()) {

				Common.displayWindow(ipaymuRequest.getUrl().trim(), true, "95%");

			} else {
				// Alert kegagalan + "Informasi Teknis" dari sendRequest (lihat InfoTeknisPembayaran).
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}

		return true;
	}

	/**
	 * Melakukan panggilan HTTP POST langsung (lewat {@link HttpURLConnection}, bukan proses
	 * {@code curl} eksternal) ke endpoint API iPaymu yang diberikan, mem-parsing respons JSON,
	 * dan menyimpan record {@link IpaymuRequest} (beserta detail dan, bila mode VA
	 * menghasilkan nomor VA, record {@link IpaymuResponse} berstatus
	 * {@link IpaymuResponse#PENDING}) ke database.
	 *
	 * <p>
	 * Alur kerja: (1) membersihkan riwayat "Informasi Teknis" lewat
	 * {@link InfoTeknisPembayaran#bersihkan()}; (2) mengirim {@code postData} (hasil
	 * {@link URLBuilder#httpBuildQuery(Map, String)}) ke {@code urlStr} lewat POST; (3) bila
	 * kode status HTTP bukan 2xx, membaca error stream, mencatat detail kegagalan, dan
	 * mengembalikan {@code null} tanpa menyentuh database; (4) bila sukses, mem-parsing body
	 * respons sebagai {@link JSONObject} untuk mengambil {@code va} dan {@code id} (trxId); (5)
	 * bila trxId, va, dan url semuanya kosong (gateway menolak secara logis meski status HTTP
	 * sukses), mencatat detail kegagalan (tanpa langsung berhenti — penyimpanan tetap
	 * dilanjutkan dengan data kosong agar jejak percobaan tetap tersimpan); (6) menyusun dan
	 * menyimpan record {@link IpaymuRequest} beserta seluruh {@link IpaymuRequestDetail}/
	 * {@link IpaymuRequestDetailBiaya} terkait, masing-masing dalam transaksi Hibernate
	 * terpisah; (7) bila {@code va} tidak kosong, turut menyimpan record {@link IpaymuResponse}
	 * awal berstatus {@code PENDING} sebagai jejak VA yang diterbitkan.
	 * </p>
	 *
	 * <p>
	 * Penanganan galat granular per jenis kegagalan jaringan: {@link java.net.ConnectException}
	 * (gateway tak terhubung), {@link java.net.SocketTimeoutException} (timeout), dan
	 * {@link Exception} umum lainnya masing-masing dicatat dengan pesan "Informasi Teknis" yang
	 * spesifik lewat {@link InfoTeknisPembayaran#catat(String)} sebelum mengembalikan
	 * {@code null} — lihat javadoc kelas untuk penjelasan pola ini.
	 * </p>
	 *
	 * @param urlStr                    URL endpoint API iPaymu yang dipanggil (berbeda
	 *                                  tergantung mode di {@link #onSaveIpaymu})
	 * @param postData                  data POST yang sudah di-encode (query string
	 *                                  {@code application/x-www-form-urlencoded})
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
	 * @param comments                  ringkasan item biaya
	 * @param ipaymuRequestDetails      rincian item pembayaran yang akan disimpan
	 * @param ipaymuRequestDetailBiayas rincian biaya mentah yang akan disimpan
	 * @return record {@link IpaymuRequest} yang berhasil disimpan, atau {@code null} bila
	 *         gateway tidak dapat dihubungi/menolak permintaan, atau bila penyimpanan ke
	 *         database gagal setelah gateway menerima transaksi
	 * @throws Exception diteruskan dari kegagalan di luar blok try-catch internal (mis.
	 *                    kegagalan encode URL pada {@link URLEncoder#encode(String, String)})
	 */
	public static IpaymuRequest sendRequest(String urlStr, String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String comments,
			List<IpaymuRequestDetail> ipaymuRequestDetails, List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas)
			throws Exception {
		// Pola "Informasi Teknis" kegagalan payment gateway (lihat InfoTeknisPembayaran):
		// bersihkan detail lama agar kegagalan transaksi sebelumnya tidak bocor ke alert ini.
		InfoTeknisPembayaran.bersihkan();

		String hasil = "";
		JSONObject param;
		String va;
		String trxId;
		try {
			// curl_init and url
			URL url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);

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

			if (code < 200 || code >= 300) {
				// Server iPaymu MENOLAK permintaan (kode status bukan sukses) → catat detailnya
				// supaya pengguna/admin tahu penyebab pastinya (bukan sekadar "Transaksi Gagal").
				String badan = "";
				try {
					java.io.InputStream err = con.getErrorStream();
					if (err != null) {
						StringBuilder errBuf = new StringBuilder();
						int ce;
						while ((ce = err.read()) != -1) {
							errBuf.append((char) ce);
						}
						err.close();
						badan = errBuf.toString();
					}
				} catch (Exception ignore) {
					ais.common.ErrorAuditUtil.record(ignore, "iPaymu sendRequest gagal baca error stream; url=" + urlStr);
				}
				String info = "Server iPaymu menolak permintaan, kode status=" + code
						+ (badan.trim().isEmpty() ? "" : ", respons server: " + InfoTeknisPembayaran.potong(badan, 300))
						+ ". URL gateway: " + urlStr;
				InfoTeknisPembayaran.catat(info);
				ais.common.ErrorAuditUtil.record(new IllegalStateException(info), "iPaymu sendRequest ditolak; url=" + urlStr);
				return null;
			}

			// read the response
			DataInputStream input = new DataInputStream(con.getInputStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			input.close();
			hasil = resultBuf.toString();

			param = new JSONObject(hasil);

			va = param.isNull("va") ? "" : param.getString("va");
			trxId = param.isNull("id") ? "" : param.getString("id");
			System.out.println("==> response param => " + param);
			System.out.println("==> response va => " + va);
			System.out.println("==> response trxId => " + trxId);

			// Respons HTTP sukses namun TANPA tanda transaksi (id/va/url kosong semua) →
			// gateway menolak secara logis (mis. key salah). Catat agar alert pemanggil
			// (yang mengecek trxId/url kosong) menampilkan penyebabnya.
			if (trxId.trim().isEmpty() && va.trim().isEmpty() && param.isNull("url")) {
				InfoTeknisPembayaran.catat("Gateway iPaymu merespons kode status=" + code
						+ " namun tanpa id/va/url transaksi. Respons server: " + InfoTeknisPembayaran.potong(hasil, 300)
						+ ". URL gateway: " + urlStr);
			}
		} catch (java.net.ConnectException ce) {
			// Gateway iPaymu tak bisa dihubungi (unreachable/refused) → kembalikan null agar
			// pemanggil menampilkan alert kegagalan beserta informasi teknisnya, bukan crash.
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway iPaymu (" + urlStr + "): "
					+ InfoTeknisPembayaran.potong(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			ais.common.ErrorAuditUtil.record(ce, "iPaymu sendRequest ConnectException; url=" + urlStr);
			return null;
		} catch (java.net.SocketTimeoutException te) {
			// Gateway iPaymu tidak merespons dalam batas waktu → sama: null, jangan crash halaman.
			InfoTeknisPembayaran.catat("Gateway iPaymu (" + urlStr + ") tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			ais.common.ErrorAuditUtil.record(te, "iPaymu sendRequest timeout; url=" + urlStr);
			return null;
		} catch (Exception e) {
			// Kegagalan request/parse lain (respons bukan JSON, koneksi putus, dsb) → null.
			InfoTeknisPembayaran.catat("Gagal memproses request/respons iPaymu (" + urlStr + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			ais.common.ErrorAuditUtil.record(e, "iPaymu sendRequest gagal; url=" + urlStr);
			return null;
		}

		IpaymuRequest ipaymuRequest = new IpaymuRequest();
		ipaymuRequest.setNama(param.isNull("sessionID") ? va : param.getString("sessionID"));
		ipaymuRequest.setUrl(param.isNull("url")
				? "/common/ipaymu/no_va.zul?va=" + URLEncoder.encode(va, "UTF-8") + "&nominal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amount), "UTF-8")
				: param.getString("url"));
		ipaymuRequest.setMahasiswa(mahasiswa);
		ipaymuRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
		ipaymuRequest.setJenisKegiatan(jenisKegiatan);
		ipaymuRequest.setJadwalPembayaran(jadwalPembayaran);
		ipaymuRequest.setSemester(semester);
		ipaymuRequest.setTahunAkademik(tahunAkademik);
		ipaymuRequest.setKeterangan(keterangan);
		ipaymuRequest.setPengurangan(pengurangan);
		ipaymuRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
		ipaymuRequest.setAmount(amount);
		ipaymuRequest.setComments(comments);
		ipaymuRequest.setTrxId(trxId);

		Session session = HibernateUtil.currentNativeSession();
		try {
			session.getTransaction().begin();
			session.save(ipaymuRequest);
			session.getTransaction().commit();

			for (IpaymuRequestDetail ipaymuRequestDetail : ipaymuRequestDetails) {
				ipaymuRequestDetail.setIpaymuRequest(ipaymuRequest);
				session.getTransaction().begin();
				session.save(ipaymuRequestDetail);
				session.getTransaction().commit();
			}

			for (IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya : ipaymuRequestDetailBiayas) {
				ipaymuRequestDetailBiaya.setIpaymuRequest(ipaymuRequest);
				session.getTransaction().begin();
				session.save(ipaymuRequestDetailBiaya);
				session.getTransaction().commit();
			}

			if (va != null && !va.trim().isEmpty()) {
				IpaymuResponse ipaymuResponse = new IpaymuResponse();
				ipaymuResponse.setNama(va);
				ipaymuResponse.setStatus(IpaymuResponse.PENDING);
				ipaymuResponse.setMerchant("e-Campus");
				ipaymuResponse.setTrxId(trxId);
				ipaymuResponse.setProduct(comments);
				ipaymuResponse.setBuyer("Mahasiswa");
				ipaymuResponse.setNoRekeningDeposit(va);
				ipaymuResponse.setComments(comments);
				session.getTransaction().begin();
				session.save(ipaymuResponse);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			// Transaksi sudah diterima gateway iPaymu namun GAGAL disimpan di aplikasi →
			// catat detailnya lalu kembalikan null agar alert pemanggil informatif.
			InfoTeknisPembayaran.catat("Transaksi diterima gateway iPaymu namun GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200)
					+ (trxId == null || trxId.trim().isEmpty() ? "" : ". trxId=" + trxId));
			ais.common.ErrorAuditUtil.record(e, "iPaymu sendRequest gagal simpan; trxId=" + trxId + ", url=" + urlStr);
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) {
				ais.common.ErrorAuditUtil.record(ignore, "iPaymu sendRequest rollback gagal; url=" + urlStr);
			}
			return null;
		}

		return ipaymuRequest;
	}

}
