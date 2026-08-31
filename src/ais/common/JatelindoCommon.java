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
import ais.database.model.file.LampiranLain;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;
import ais.database.model.jatelindo.JatelindoRequestDetailBiaya;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kumpulan helper statis untuk alur pembayaran via Jatelindo — integrasi Virtual Account (VA)
 * Bank Mandiri di AIS. Berbeda dari payment gateway lain yang memanggil API pihak ketiga secara
 * langsung, alur Jatelindo pada kelas ini membangkitkan nomor VA secara LOKAL (gabungan
 * {@code merchant_id} + digit acak, lihat {@link #sendRequest}) dan menyimpannya sebagai record
 * {@link JatelindoRequest} di database aplikasi — bank/penyedia VA yang kemudian mencocokkan
 * pembayaran masuk terhadap nomor VA tersebut lewat mekanisme rekonsiliasi di luar kelas ini
 * (notifikasi/callback ditangani di kelas lain, bukan di sini).
 *
 * <h2>Alur baku</h2>
 * <ol>
 * <li>UI menyusun daftar item biaya yang akan dibayar (lewat salah satu varian
 * {@code populateJatelindoRequestDetail}/{@code populateDetailBiaya}, tergantung sumber data:
 * grid biaya reguler, grid cicilan, atau parameter request HTTP).</li>
 * <li>{@link #onPilihJatelindo} (dipanggil langsung atau lewat {@link #onSaveJatelindo}) memanggil
 * {@link #sendRequest} untuk membuat &amp; menyimpan {@link JatelindoRequest} beserta seluruh
 * baris detail biayanya, lalu menghasilkan barcode/QR pembayaran dan menampilkan halaman instruksi
 * pembayaran ({@code /common/jatelindo/no_va.zul}) berisi nomor VA, nominal, biaya administrasi,
 * dan nominal terbilang.</li>
 * <li>Bila penyimpanan gagal, detail teknis dicatat ke {@link InfoTeknisPembayaran} (pola yang
 * dipakai bersama seluruh payment gateway di AIS) dan pengguna melihat pesan gagal generik lewat
 * {@link MyMessageboxConfig}, sementara detail teknis tersedia untuk admin.</li>
 * </ol>
 *
 * <p>
 * Nilai {@code merchant_id} (dibaca dari konfigurasi {@code jatelindo_merchant_id}, default
 * {@code "129"}) dan {@code jatelindo_biaya_administrasi} dibaca dari
 * {@link Common#getKonfigurasi(String, String)} saat runtime — bukan konstanta tertanam di kode
 * sumber; tidak ditemukan kredensial/API key tertanam pada kelas ini.
 * </p>
 */
public class JatelindoCommon {

	/**
	 * Membangun konfigurasi tombol UI "Bayar via Jatelindo/Mandiri", termasuk logika penyalinan
	 * gambar tombol kustom (bila diunggah admin lewat {@link LampiranLain}) ke folder
	 * {@code img/} aplikasi supaya dapat diakses langsung sebagai aset statis.
	 *
	 * <p>
	 * Gambar tombol default adalah {@code img/mandiri.jpg}; bila admin sudah mengunggah gambar
	 * kustom (lampiran {@link LampiranLain#BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO}), gambar tersebut
	 * disalin ke {@code img/} (hanya bila belum ada) agar dapat dirujuk sebagai path web biasa.
	 * Label tombol dibaca dari konfigurasi {@code label_pembayaran_via_jatelindo}
	 * (default {@code "Bayar via Mandiri"}).
	 * </p>
	 *
	 * @return konfigurasi tombol siap pakai (label + path gambar) untuk dirender di UI
	 */
	public static MyButtonConfig createButton() {
		File fileViaJatelindo = new File(Common.REAL_PATH + "/img/mandiri.jpg");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaJatelindo = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaJatelindo.getName());
				boolean ada = fileDiImg.exists();
				System.out.println(
						"fileViaJatelindo = " + fileViaJatelindo + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaJatelindo);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaJatelindo = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_jatelindo", "Bayar via Mandiri").getNilai(),
				"/img/" + fileViaJatelindo.getName());
		return bayarViaJatelindo;
	}

	/**
	 * Membaca baris-baris {@link Grid} biaya (ZK) yang sedang ditampilkan pengguna dan
	 * mengubahnya menjadi daftar {@link JatelindoRequestDetailBiaya}, mengambil nilai nominal
	 * dari komponen input yang aktif di tiap baris (kotak angka bila nilai boleh diubah, atau
	 * label bila tidak) — hanya baris yang terlihat ({@code row.isVisible()}) yang diproses.
	 *
	 * <p>
	 * Untuk item biaya dengan jenis penghitungan {@link ItemBiaya#DIKALI_NILAI_MINUS}, nominal
	 * TIDAK diambil dari komponen di baris grid utama, melainkan dicari padanannya di daftar
	 * {@code pengurangan} (komponen {@link MyDoubleboxMin} terpisah untuk item pengurang) — bila
	 * ditemukan padanan berdasarkan id {@link DetailBiaya}, nilainya dipakai sebagai nominal.
	 * </p>
	 *
	 * @param gridss      komponen {@link Grid} ZK yang menampilkan daftar biaya
	 * @param pengurangan daftar komponen input nilai pengurangan (untuk item biaya bertipe
	 *                    {@link ItemBiaya#DIKALI_NILAI_MINUS})
	 * @return daftar {@link JatelindoRequestDetailBiaya} sesuai baris grid yang terlihat, siap
	 *         dipakai sebagai bagian dari request pembayaran
	 */
	@SuppressWarnings("unchecked")
	public static List<JatelindoRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas = new ArrayList<JatelindoRequestDetailBiaya>();
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JatelindoCommon.java:110");
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

				JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya = new JatelindoRequestDetailBiaya();
				jatelindoRequestDetailBiaya.setDetailBiaya(detailBiaya);
				jatelindoRequestDetailBiaya.setNilai(biaya);
				jatelindoRequestDetailBiayas.add(jatelindoRequestDetailBiaya);
			}
		}
		return jatelindoRequestDetailBiayas;
	}

	/**
	 * Mengonversi daftar {@link JatelindoRequestDetailBiaya} (hasil {@link #populateDetailBiaya})
	 * menjadi daftar {@link JatelindoRequestDetail} yang siap disimpan sebagai bagian dari satu
	 * {@link JatelindoRequest}, dengan nomor urut ({@code ke}) berurutan mulai dari 1 dan
	 * tanggal diisi waktu sekarang lewat {@code WaktuUtil.getDate()}.
	 *
	 * @param jatelindoRequestDetailBiayas daftar detail biaya sumber
	 * @return daftar {@link JatelindoRequestDetail} hasil konversi, urutan sama dengan input
	 */
	public static List<JatelindoRequestDetail> populateJatelindoRequestDetailDariDetailBiaya(
			List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas) {
		List<JatelindoRequestDetail> jatelindoRequestDetails = new ArrayList<JatelindoRequestDetail>();

		int i = 1;
		for (JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya : jatelindoRequestDetailBiayas) {
			JatelindoRequestDetail jatelindoRequestDetail = new JatelindoRequestDetail();
			jatelindoRequestDetail.setPengaturanPembayaranBulanan(null);
			jatelindoRequestDetail.setItemBiaya(jatelindoRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			jatelindoRequestDetail.setKeterangan(jatelindoRequestDetailBiaya.getKeterangan());
			jatelindoRequestDetail.setNilai(jatelindoRequestDetailBiaya.getNilai());
			jatelindoRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			jatelindoRequestDetail.setKe(i);
			jatelindoRequestDetails.add(jatelindoRequestDetail);
			i++;
		}

		return jatelindoRequestDetails;
	}

	/**
	 * Membangun daftar {@link JatelindoRequestDetail} dari parameter HTTP request langsung
	 * (dipakai jalur pembayaran yang dipicu dari URL/API, bukan dari grid ZK) — parameter
	 * {@code jenis} menentukan sumber data ({@code "bulanan"} → {@link
	 * PengaturanPembayaranBulanan}, selain itu → {@link DetailBiaya} langsung) dan parameter
	 * {@code data} berisi daftar id yang dipisah koma.
	 *
	 * <p>
	 * Untuk baris bertipe {@code PengaturanPembayaranBulanan}, nominal dihitung lewat
	 * {@link PengaturanPembayaranBulanan#ambilNominalModifikasi(Mahasiswa, Integer)} (memperhatikan
	 * modifikasi nominal per mahasiswa/semester); untuk baris {@link DetailBiaya} langsung,
	 * nominal diambil dari {@code nilaiBiayaBaru} bila ada, atau {@code nilaiBiaya} bawaan.
	 * Keterangan tiap baris disusun otomatis (kode+nama item, nominal terformat, dan info
	 * validator bila diberikan).
	 * </p>
	 *
	 * <p>
	 * Membuka sesi Hibernate native sendiri dan menutupnya lewat
	 * {@link HibernateUtil#closeSession()} di akhir method (bukan di blok {@code finally} —
	 * sesi tidak ditutup bila terjadi exception di tengah proses).
	 * </p>
	 *
	 * @param request   HTTP request yang membawa parameter {@code jenis} dan {@code data}
	 * @param mahasiswa mahasiswa terkait, dipakai untuk menghitung nominal modifikasi pada
	 *                  pembayaran bulanan
	 * @param validator teks validator/identitas pemroses, disisipkan ke keterangan bila tidak
	 *                  kosong
	 * @param semester  semester terkait, dipakai untuk menghitung nominal modifikasi pada
	 *                  pembayaran bulanan
	 * @return daftar {@link JatelindoRequestDetail} sesuai id-id pada parameter {@code data}
	 */
	public static List<JatelindoRequestDetail> populateJatelindoRequestDetail(HttpServletRequest request,
			Mahasiswa mahasiswa, String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
		System.out.println("jenis => " + jenis + ", data => " + data);
		List<JatelindoRequestDetail> jatelindoRequestDetails = new ArrayList<JatelindoRequestDetail>();

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

			JatelindoRequestDetail jatelindoRequestDetail = new JatelindoRequestDetail();
			jatelindoRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			jatelindoRequestDetail.setDetailBiaya(detailBiaya);
			jatelindoRequestDetail.setItemBiaya(itemBiaya);
			jatelindoRequestDetail.setKeterangan(keterangan);
			jatelindoRequestDetail.setNilai(nilai);
			jatelindoRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			jatelindoRequestDetail.setKe(i);
			jatelindoRequestDetail.setDenda(0.0);
			jatelindoRequestDetail.setNilaiAsli(nilai);
			jatelindoRequestDetails.add(jatelindoRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return jatelindoRequestDetails;
	}

	/**
	 * Membangun daftar {@link JatelindoRequestDetail} dari grid cicilan pembayaran ({@link
	 * CicilanPembayaran}) — varian ini khusus untuk alur pembayaran cicilan, hanya memproses
	 * baris yang nilainya diisi pengguna (nilai absolut &gt; 0.01).
	 *
	 * <p>
	 * Untuk baris tanpa {@link CicilanPembayaran} yang sudah ada (cicilan baru,
	 * {@code cicilanPembayaran.getId() == null}) dan terkait
	 * {@link PengaturanPembayaranBulanan}, method ini MENGHITUNG ULANG denda secara real-time
	 * lewat {@link PengaturanPembayaranBulanan#checkDenda} — termasuk memeriksa apakah
	 * {@code jadwalPembayaran} berlaku khusus untuk NIM mahasiswa bersangkutan (kolom
	 * {@code khususUntukNim} berformat daftar NIM dipisah koma, dicek dengan pola
	 * {@code ",NIM,"}) — sehingga nominal denda pada request yang dikirim sudah termasuk
	 * penyesuaian denda terbaru, bukan nilai yang dihitung sebelumnya.
	 * </p>
	 *
	 * <p>
	 * Bila validator pada {@code cicilanPembayaran} yang sudah ada kosong/tidak valid
	 * (kosong, hanya whitespace, atau literal string {@code "null"}), method mengisi validator
	 * dengan representasi string pengguna yang sedang login ({@link Common#getCurrentUser()}).
	 * </p>
	 *
	 * @param gridCicilan      komponen {@link Grid} ZK yang menampilkan baris-baris cicilan
	 * @param mahasiswa        mahasiswa terkait, dipakai untuk menghitung ulang nominal/denda
	 * @param semester         semester terkait, dipakai untuk menghitung ulang nominal
	 * @param jadwalPembayaran jadwal pembayaran acuan untuk perhitungan denda; boleh {@code null}
	 * @return daftar {@link JatelindoRequestDetail} hanya untuk baris cicilan yang diisi nilainya
	 */
	public static List<JatelindoRequestDetail> populateJatelindoRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<JatelindoRequestDetail> jatelindoRequestDetails = new ArrayList<JatelindoRequestDetail>();

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

				JatelindoRequestDetail jatelindoRequestDetail = new JatelindoRequestDetail();

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

				jatelindoRequestDetail.setDetailBiaya(detailBiaya);
				jatelindoRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				jatelindoRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				jatelindoRequestDetail.setItemBiaya(itemBiaya);
				jatelindoRequestDetail.setKeterangan(keterangan.getValue());
				jatelindoRequestDetail.setNilai(jumlahCicilan.getValue());
				jatelindoRequestDetail.setTanggal(tanggal.getValue());
				jatelindoRequestDetail.setKe(i);

				jatelindoRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				jatelindoRequestDetail
						.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
								: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, jatelindoRequestDetail.getTanggal(),
								jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						jatelindoRequestDetail.setDenda(denda);
						jatelindoRequestDetail.setNilaiAsli(nom);
					}
				}

				jatelindoRequestDetails.add(jatelindoRequestDetail);
				i++;
			}
		}

		return jatelindoRequestDetails;
	}

	/**
	 * Titik masuk pembayaran biaya pendaftaran mahasiswa baru via Jatelindo/VA Mandiri untuk
	 * satu {@link BiodataCalonMahasiswa}. Menentukan program studi acuan (prodi lulus bila sudah
	 * ada, atau salah satu prodi pilihan bila belum), mengambil daftar biaya yang harus dibayar
	 * lewat {@link PembayaranUtil#getDetailBiayaCalonMahasiswa}, menghitung jadwal pembayaran dan
	 * dendanya lewat {@link PembayaranUtil#getJadwalPembayaranDanDendaBerdasarkanTahunAkademik},
	 * lalu — bila ada jadwal pembayaran yang valid dan ada biaya yang harus dibayar — langsung
	 * memicu penyimpanan request pembayaran lewat {@link #onSaveJatelindo} dengan keterangan tetap
	 * {@code "Pembayaran Pendaftaran Mahasiswa Baru"}.
	 *
	 * <p>
	 * Bila tidak ada biaya yang harus dibayar atau jadwal pembayaran tidak ditemukan, method ini
	 * tidak melakukan apa pun (tidak melempar exception, tidak membuat request).
	 * </p>
	 *
	 * @param calonMahasiswa data calon mahasiswa yang akan membayar biaya pendaftaran
	 * @param jenisKegiatan  jenis kegiatan yang menjadi acuan perhitungan biaya
	 * @throws Exception diteruskan dari kegagalan pengambilan data biaya/jadwal atau proses
	 *                    penyimpanan request pembayaran
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

				List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas = new ArrayList<JatelindoRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya = new JatelindoRequestDetailBiaya();
					jatelindoRequestDetailBiaya.setDetailBiaya(detailBiaya);
					jatelindoRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					jatelindoRequestDetailBiayas.add(jatelindoRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += jatelindoRequestDetailBiaya.getNilai();
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				JatelindoCommon.onSaveJatelindo(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan,
						jadwalPembayaran, 1, calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru",
						0.0, nilaiBiayaHarusDiBayars,
						JatelindoCommon.populateJatelindoRequestDetailDariDetailBiaya(jatelindoRequestDetailBiayas),
						jatelindoRequestDetailBiayas, null);

			}
		}

	}

	/**
	 * Implementasi kanonik pemicu pembuatan request pembayaran Jatelindo dari UI: membaca
	 * {@code jatelindo_merchant_id} dari konfigurasi, memanggil {@link #sendRequest} untuk
	 * membangkitkan nomor VA dan menyimpan seluruh detail request, lalu — bila berhasil —
	 * membuat QR/barcode pembayaran (lewat {@link BarcodeCommon#generateCRCode}) dan menampilkan
	 * jendela instruksi pembayaran {@code /common/jatelindo/no_va.zul} berisi nomor VA, nominal,
	 * biaya administrasi, total, tautan QR, dan nominal terbilang (lewat
	 * {@link IndonesianNumberToWords#convert(long)}).
	 *
	 * <p>
	 * Bila {@link #sendRequest} mengembalikan {@code null} (gagal), method menampilkan pesan
	 * peringatan generik ke pengguna lewat {@link MyMessageboxConfig} sambil detail teknis
	 * kegagalan sudah tercatat di {@link InfoTeknisPembayaran} oleh {@link #sendRequest} — pola
	 * yang seragam dengan payment gateway lain di AIS agar pengguna tidak melihat pesan error
	 * teknis mentah namun admin tetap punya jejak diagnosis.
	 * </p>
	 *
	 * @param amn                       nominal yang akan dibayar
	 * @param mahasiswa                 mahasiswa pembayar; boleh {@code null} bila pembayar
	 *                                  adalah calon mahasiswa
	 * @param biodataCalonMahasiswa     calon mahasiswa pembayar; boleh {@code null} bila
	 *                                  pembayar adalah mahasiswa aktif
	 * @param jenisKegiatan             jenis kegiatan terkait pembayaran
	 * @param jadwalPembayaran          jadwal pembayaran acuan, boleh {@code null}
	 * @param semester                  semester terkait
	 * @param tahunAkademik             tahun akademik terkait
	 * @param keterangan                keterangan pembayaran
	 * @param pengurangan               nilai pengurangan/diskon yang diterapkan
	 * @param nilaiBiayaHarusDiBayars   total nilai biaya sebelum pengurangan
	 * @param jatelindoRequestDetails   daftar detail item pembayaran
	 * @param jatelindoRequestDetailBiayas daftar detail biaya terkait item pembayaran
	 * @param event                     event ZK pemicu, diteruskan apa adanya (tidak dipakai
	 *                                  langsung pada implementasi saat ini)
	 * @return selalu {@code true} — nilai kembalian tidak mencerminkan sukses/gagalnya
	 *         pembuatan request (lihat tampilan pesan error ke pengguna untuk status
	 *         sesungguhnya)
	 * @throws Exception diteruskan dari kegagalan {@link #sendRequest} atau proses pembuatan
	 *                    barcode/URL instruksi pembayaran
	 */
	public static boolean onPilihJatelindo(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<JatelindoRequestDetail> jatelindoRequestDetails,
			List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas, Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("jatelindo_merchant_id", "129").getNilai().trim();

		final JatelindoRequest jatelindoRequest = JatelindoCommon.sendRequest(mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, merchant_id, jatelindoRequestDetails, jatelindoRequestDetailBiayas, true);
		if (jatelindoRequest != null) {

			Double biayaAdministrasi = jatelindoRequest.getBiayaAdministrasi();

			String code = jatelindoRequest.getTrxId();

			File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + jatelindoRequest.getId() + ".png");

			BarcodeCommon.generateCRCode(code, myfilebarcode1);

			String myUrl = "/common/jatelindo/no_va.zul?va=" + URLEncoder.encode(jatelindoRequest.getTrxId(), "UTF-8")
					+ "&nominal="
					+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(jatelindoRequest.getAmount()), "UTF-8")
					+ "&biayaAdministrasi="
					+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
					+ "&biayaTotal="
					+ URLEncoder.encode(
							"Rp. " + Common.numberFormat.get().format(jatelindoRequest.getAmount() + biayaAdministrasi),
							"UTF-8")
					+ "&qr="
					+ URLEncoder.encode(
							Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(), "UTF-8")
					+ "&terbilang="
					+ URLEncoder.encode(
							IndonesianNumberToWords.convert((long) (jatelindoRequest.getAmount() + biayaAdministrasi)),
							"UTF-8")
					+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

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
	 * Pembungkus tipis di atas {@link #onPilihJatelindo} dengan satu pengaman tambahan: menolak
	 * (mengembalikan {@code false} tanpa membuat request apa pun) bila nominal {@code amn}
	 * kurang dari {@code 0.01} — mencegah pembuatan request pembayaran dengan nominal nol/negatif.
	 *
	 * @param amn                       nominal yang akan dibayar; harus &gt;= 0.01 agar diproses
	 * @param mahasiswa                 mahasiswa pembayar; boleh {@code null}
	 * @param biodataCalonMahasiswa     calon mahasiswa pembayar; boleh {@code null}
	 * @param jenisKegiatan             jenis kegiatan terkait pembayaran
	 * @param jadwalPembayaran          jadwal pembayaran acuan
	 * @param semester                  semester terkait
	 * @param tahunAkademik             tahun akademik terkait
	 * @param keterangan                keterangan pembayaran
	 * @param pengurangan               nilai pengurangan/diskon
	 * @param nilaiBiayaHarusDiBayars   total nilai biaya sebelum pengurangan
	 * @param jatelindoRequestDetails   daftar detail item pembayaran
	 * @param jatelindoRequestDetailBiayas daftar detail biaya terkait item pembayaran
	 * @param event                     event ZK pemicu
	 * @return {@code false} bila {@code amn < 0.01} (request tidak dibuat); {@code true} bila
	 *         diteruskan ke {@link #onPilihJatelindo} (lihat catatan nilai kembalian pada method
	 *         tersebut)
	 * @throws Exception diteruskan dari {@link #onPilihJatelindo}
	 */
	@SuppressWarnings({})
	public static boolean onSaveJatelindo(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<JatelindoRequestDetail> jatelindoRequestDetails,
			final List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas, final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihJatelindo(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester,
				tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, jatelindoRequestDetails,
				jatelindoRequestDetailBiayas, event);

		return true;
	}

	public static JatelindoRequest sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars, Double amount, String merchant_id,
			List<JatelindoRequestDetail> jatelindoRequestDetails,
			List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas, Boolean hapusCicilanSebelumnya)
			throws Exception {

		// Bersihkan detail kegagalan lama agar info transaksi sebelumnya tidak bocor ke alert.
		InfoTeknisPembayaran.bersihkan();

		JatelindoRequest jatelindoRequest = new JatelindoRequest();

		try {

			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_jatelindo", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JatelindoCommon.java:435");

			}
			String virtual_account = merchant_id + Common.getGeneratedAngkaDigit(generatedAngkaDigit);

			Double biayaAdministrasi = 0.0;
			try {
				biayaAdministrasi = Double
						.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JatelindoCommon.java:444");

			}

			jatelindoRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			jatelindoRequest.setNama(virtual_account);
			jatelindoRequest.setTrxId(virtual_account);
			jatelindoRequest.setMerchant_id(merchant_id);
			jatelindoRequest.setMerchant("Mandiri");
			jatelindoRequest.setMahasiswa(mahasiswa);
			jatelindoRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			jatelindoRequest.setJenisKegiatan(jenisKegiatan);
			jatelindoRequest.setJadwalPembayaran(jadwalPembayaran);
			jatelindoRequest.setSemester(semester);
			jatelindoRequest.setTahunAkademik(tahunAkademik);
			jatelindoRequest.setKeterangan(keterangan);
			jatelindoRequest.setPengurangan(pengurangan);
			jatelindoRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
			jatelindoRequest.setAmount(amount);
			jatelindoRequest.setBiayaAdministrasi(biayaAdministrasi);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(jatelindoRequest);
			session.getTransaction().commit();

			for (JatelindoRequestDetail jatelindoRequestDetail : jatelindoRequestDetails) {
				jatelindoRequestDetail.setJatelindoRequest(jatelindoRequest);
				session.getTransaction().begin();
				session.save(jatelindoRequestDetail);
				session.getTransaction().commit();
			}

			for (JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya : jatelindoRequestDetailBiayas) {
				jatelindoRequestDetailBiaya.setJatelindoRequest(jatelindoRequest);
				session.getTransaction().begin();
				session.save(jatelindoRequestDetailBiaya);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			// Jatelindo tidak memanggil gateway di sini (VA dibuat lokal) — kegagalan berarti
			// request GAGAL disimpan di aplikasi. Catat detailnya agar alert tidak generik.
			InfoTeknisPembayaran.catat("Request Jatelindo (VA Mandiri) GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
		}
		return jatelindoRequest;
	}

}
