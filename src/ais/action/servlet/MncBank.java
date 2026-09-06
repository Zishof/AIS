package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;

/**
 * Servlet <i>host-to-host</i> (H2H) untuk kanal pembayaran <b>MNC Bank</b>.
 *
 * <p>Servlet ini adalah titik masuk yang dipanggil langsung oleh sistem MNC Bank untuk dua
 * operasi dasar Virtual Account (VA):</p>
 * <ul>
 *   <li><b>Inquiry</b> — bank menanyakan "tagihan apa di balik nomor VA ini?". Dipilih ketika
 *       payload JSON <b>tidak</b> memuat kunci {@code amt}. Balasan berisi nama penagih,
 *       nomor induk, rincian item tagihan, dan nominal.</li>
 *   <li><b>Payment</b> — bank memberi tahu bahwa setoran sudah diterima. Dipilih ketika payload
 *       memuat {@code amt}. Jalur ini <b>menulis</b> ke basis data: membuat/memutakhirkan
 *       {@link Kegiatan}, {@link CicilanPembayaran}, dan menandai VA lunas lewat
 *       {@link VirtualAccountBank#updateVa}.</li>
 * </ul>
 *
 * <h4>Bentuk pesan</h4>
 * <p>Permintaan berupa JSON pada <i>body</i> dengan kunci {@code prefix}, {@code customerId}
 * (digabung menjadi nomor VA) dan opsional {@code amt}. Balasan selalu JSON dengan kunci
 * {@code referenceNo}, {@code resultCd}, {@code resultMsg}, {@code amt}, {@code goodsNm}, dan
 * {@code billingNm}. Kode hasil yang dipakai: {@code 0000} sukses, {@code 9611} sudah dibayar,
 * {@code 9612} kedaluwarsa, {@code 9613} VA tidak ditemukan / nominal tidak sesuai.</p>
 *
 * <h4>PERINGATAN KEAMANAN — endpoint ini TIDAK memiliki autentikasi</h4>
 * <p>Fakta ini didokumentasikan agar tidak hilang, bukan sebagai anjuran:</p>
 * <ul>
 *   <li>{@link #process} <b>tidak pernah</b> memeriksa tanda tangan, token, kunci API, Basic
 *       Auth, maupun mTLS. Seluruh header hanya dibaca untuk dicetak ke {@code System.out}.</li>
 *   <li>Satu-satunya pengenal pemanggil adalah alamat IP yang dipetakan ke {@link BankHost}
 *       oleh {@code PembayaranUtil.getBankHost(String, String)}. Nilai itu <b>tidak dipakai
 *       sebagai gerbang</b>: pemrosesan tetap berjalan meskipun hasilnya {@code null}.
 *       Selain itu pemetaan tersebut punya dua jalur pelonggaran — konfigurasi
 *       {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} yang membuat baris
 *       {@code BankHost} baru untuk IP pemanggil apa pun, dan baris cadangan ber-IP
 *       {@code 0.0.0.0} yang menampung sisanya.</li>
 *   <li>Pada {@code applicationContext-security.xml} URL {@code /MncBank} jatuh ke aturan
 *       penampung {@code /**} yang bernilai {@code IS_AUTHENTICATED_ANONYMOUSLY}, sehingga
 *       lapisan Spring Security pun tidak menutupnya.</li>
 * </ul>
 * <p>Akibatnya nomor VA berperan sebagai satu-satunya "rahasia": siapa pun yang dapat
 * menghubungi endpoint ini bisa memakainya sebagai <i>oracle</i> yang mengembalikan nama
 * siswa/mahasiswa, nomor induk, dan rincian tagihan; dan karena cabang <i>payment</i> berada
 * di endpoint tanpa gerbang yang sama, tagihan juga dapat ditandai lunas tanpa setoran nyata.
 * Pemeriksaan {@code nominal tidak sesuai} bukan penghalang, sebab nominal yang benar dapat
 * dibaca lebih dahulu lewat operasi inquiry.</p>
 *
 * <h4>Catatan arsitektur</h4>
 * <p>Setiap permintaan — berhasil maupun gagal, dikenal maupun tidak — <b>wajib</b> tercatat ke
 * {@link LogHostToHost} melalui {@code PembayaranGatewayHelper.catatLogHostToHost} di blok
 * {@code finally}. Ini adalah pola <i>audit shadow</i> yang berlaku di seluruh gerbang
 * pembayaran AIS dan merupakan fakta arsitektur yang disengaja, bukan cacat.</p>
 *
 * @see ais.database.model.VirtualAccountBank
 * @see ais.action.ws.util.PembayaranGatewayHelper
 * @see ais.action.ws.util.PembayaranUtil
 */
public class MncBank extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton pembantu pembayaran, dipakai di {@link #process} untuk memetakan alamat IP
	 * pemanggil menjadi {@link BankHost}.
	 *
	 * <p>Bersifat {@code static} sehingga dibagi seluruh permintaan; {@code PembayaranUtil}
	 * sendiri tidak menyimpan keadaan per-permintaan sehingga aman dipakai bersama.</p>
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun selain memanggil konstruktor induk; seluruh
	 * kebergantungan diambil lewat field statis {@link #pembayaranUtil}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public MncBank() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Servlet ini memperlakukan GET, POST, PUT, dan TRACE secara identik — keempatnya
	 * membaca payload JSON dari <i>body</i> permintaan — karena mitra bank tidak konsisten
	 * dalam memilih metode HTTP.</p>
	 *
	 * <p>Setiap kegagalan ditelan oleh {@link Common#tampilErrorJikaAdmin(Exception)} sehingga
	 * pemanggil tidak pernah menerima kode status 5xx; pada kondisi itu badan balasan bisa
	 * kosong.</p>
	 *
	 * @param request  permintaan masuk dari sistem bank
	 * @param response balasan yang akan diisi JSON hasil pemrosesan
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan HTTP POST — metode yang lazim dipakai MNC Bank — dengan
	 * meneruskannya ke {@link #process}.
	 *
	 * <p>Perilaku sama persis dengan {@link #doGet}; lihat catatan penanganan galat di sana.</p>
	 *
	 * @param request  permintaan masuk dari sistem bank
	 * @param response balasan yang akan diisi JSON hasil pemrosesan
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan HTTP PUT dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Perilaku sama persis dengan {@link #doGet}; disediakan karena sebagian mitra
	 * mengirim notifikasi pembayaran memakai PUT.</p>
	 *
	 * @param request  permintaan masuk dari sistem bank
	 * @param response balasan yang akan diisi JSON hasil pemrosesan
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doPut(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan HTTP TRACE dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Menimpa perilaku bawaan {@link HttpServlet#doTrace} yang seharusnya hanya
	 * memantulkan header; di sini TRACE diperlakukan sebagai kanal transaksi biasa.</p>
	 *
	 * @param request  permintaan masuk dari sistem bank
	 * @param response balasan yang akan diisi JSON hasil pemrosesan
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doTrace(HttpServletRequest, HttpServletResponse)
	 */
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Inti pemrosesan inquiry maupun pembayaran untuk satu nomor VA.
	 *
	 * <h4>Urutan pemeriksaan</h4>
	 * <ol>
	 *   <li>VA dicari lewat {@link VirtualAccountBank#ambilVa(String, Double, BankHost)}; bila
	 *       tidak ada dibalas {@code 9613 kode pembayaran tidak ditemukan}.</li>
	 *   <li>Bila {@code chek} bernilai {@code false} dan konfigurasi {@code chek_kadaluarsa}
	 *       aktif, VA yang sudah lewat tanggal {@code kadaluarsa} dibalas {@code 9612 expired}.</li>
	 *   <li>Pada jalur pembayaran ({@code inquery} bernilai {@code false}), {@code nominalP}
	 *       harus sama persis dengan {@code biayaAdmin + total} VA; bila tidak dibalas
	 *       {@code 9613 nominal tidak sesuai}.</li>
	 *   <li>VA yang sudah lunas dibalas {@code 9611 paid}.</li>
	 * </ol>
	 *
	 * <h4>Dua cabang pemilik tagihan</h4>
	 * <ul>
	 *   <li><b>Sekolah</b> — bila VA menunjuk {@code siswa} atau {@code calonSiswa}, perhitungan
	 *       didelegasikan ke {@link VirtualAccountBank#bayarSiswa}, lalu rincian tiap
	 *       {@link Tagihan} (nama item, bulan, tahun, denda, tenggat, diskon) dirangkai menjadi
	 *       {@code goodsNm} bertanda pemisah titik koma.</li>
	 *   <li><b>Perguruan tinggi</b> — bila VA menunjuk {@link Mahasiswa} atau
	 *       {@link BiodataCalonMahasiswa}, sebuah {@link Kegiatan} dicari atau dibuat, lalu
	 *       daftar {@code cicilan} pada VA diurai per token: angka murni dan awalan
	 *       {@code Bulanan-} menunjuk {@link PengaturanPembayaranBulanan}, awalan {@code Item-}
	 *       menunjuk {@link ItemBiaya}, dan awalan {@code Keranjang-} diserahkan ke
	 *       {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}. Item ber-penghitungan
	 *       {@link ItemBiaya#DIKALI_NILAI_MINUS} dibalik tandanya.</li>
	 * </ul>
	 *
	 * <p>Pada jalur pembayaran setiap token menghasilkan satu {@link CicilanPembayaran} yang
	 * di-<i>idempoten</i>-kan lewat kolom {@code ref} berpola
	 * {@code ntt-<idKegiatan>-<token>-<idVa>}: baris yang sudah ada dimutakhirkan, bukan
	 * diduplikasi. Setelah semua token selesai, total dan denda dihitung ulang oleh
	 * {@code PembayaranUtil.getTotalDanDendaFromCicilan}, {@link Kegiatan} disimpan, dan VA
	 * ditandai lunas.</p>
	 *
	 * <h4>Transaksi dan pencatatan</h4>
	 * <p>Method membuka {@link Session} Hibernate sendiri dan menutupnya di {@code finally};
	 * penyimpanan dilakukan dalam beberapa transaksi kecil yang di-<i>commit</i> berurutan,
	 * sehingga kegagalan di tengah dapat meninggalkan sebagian cicilan tersimpan sementara VA
	 * belum ditandai lunas. Apa pun hasilnya, satu baris {@link LogHostToHost} selalu ditulis
	 * di blok {@code finally} — termasuk jejak <i>stack trace</i> bila terjadi galat.</p>
	 *
	 * <p><b>Keamanan:</b> method ini menerima {@code bankHost} bernilai {@code null} dan tetap
	 * memproses. Seluruh pemeriksaan di atas bersifat konsistensi data, <b>bukan</b> otorisasi;
	 * lihat peringatan pada dokumentasi kelas.</p>
	 *
	 * @param nominalP  nominal setoran yang dilaporkan bank; pada inquiry bernilai {@code 0.0}
	 * @param tanggalP  tanggal transaksi dalam format {@code Common.databaseDateFormat1}; bila
	 *                  gagal diurai dipakai waktu server saat ini
	 * @param va        nomor Virtual Account hasil gabungan {@code prefix} dan {@code customerId}
	 * @param bank      label bank yang disimpan sebagai {@code validator} pada data pembayaran
	 * @param bankHost  host bank hasil pemetaan IP; boleh {@code null} dan tidak menjadi gerbang
	 * @param request   permintaan asal, diteruskan ke pencatat log H2H
	 * @param data      payload JSON mentah, disimpan apa adanya pada log H2H
	 * @param chekLagi  penanda warisan; saat ini tidak dipakai di badan method
	 * @param inquery   {@code true} untuk inquiry (hanya membaca), {@code false} untuk pembayaran
	 * @param chek      {@code true} untuk melewati pemeriksaan kedaluwarsa dan status lunas
	 * @return string JSON balasan yang siap dikirim ke bank
	 * @throws Exception bila terjadi kegagalan yang tidak tertangani di luar blok pemrosesan
	 */
	@SuppressWarnings("unchecked")
	public static String doProcess(Double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean chek) throws Exception {

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("referenceNo", Common.getGeneratedBarCode());
		jsonObject.put("resultCd", "0000");
		jsonObject.put("resultMsg", "SUCCESS");
		jsonObject.put("amt", "0");
		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String goodsNm = "";
			String nama = "";
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;
			VirtualAccountBank virtualAccountBankNtt = null;
			try {
				Session session = HibernateUtil.getSessionFactory().openSession();
				try {
				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va,  nominalP, bankHost);

				if (virtualAccountBankNtt == null) {
					jsonObject.put("resultCd", "9613");
					jsonObject.put("resultMsg", "kode pembayaran tidak ditemukan");
				} else if (!chek
						&& Common.bolehKonfigurasi("chek_kadaluarsa")
						&& virtualAccountBankNtt != null
						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
					jsonObject.put("resultCd", "9612");
					jsonObject.put("resultMsg", "expired");
				} else if (virtualAccountBankNtt != null && !inquery
						&& (nominalP.intValue() != (virtualAccountBankNtt.getBiayaAdmin().intValue()
								+ virtualAccountBankNtt.getTotal().intValue()))) {
					jsonObject.put("resultCd", "9613");
					jsonObject.put("resultMsg", "nominal tidak sesuai");
				} else if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, false, chek)) {
					jsonObject.put("resultCd", "9611");
					jsonObject.put("resultMsg", "paid");
				} else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

					Date tanggal = ais.ui.util.WaktuUtil.getDate();
					try {
						tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MncBank.java:154");
					}

					if (virtualAccountBankNtt.getSiswa() != null || virtualAccountBankNtt.getCalonSiswa() != null) {

						if (virtualAccountBankNtt.getSiswa() != null) {

							nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
							nama = virtualAccountBankNtt.getSiswa().getNama();

						} else if (virtualAccountBankNtt.getCalonSiswa() != null) {

							nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
							nama = virtualAccountBankNtt.getCalonSiswa().getNama();

						}

						Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session,
								tanggal, bank, inquery, data, false);
						goodsNm = "";
						Double amt = 0.0;
						for (List<Tagihan> tagihans : map.values()) {

							for (Tagihan tagihan : tagihans) {

								String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
										+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
												? " (ke " + tagihan.getBayarKe() + ")"
												: "");
								if (tagihan.getBulan() != null && tagihan.getBulan() > 0 && tagihan.getBulan() <= 12) {
									ket += ", Bulan " + Common.BULAN[tagihan.getBulan() - 1];
								}
								if (tagihan.getTahun() != null && tagihan.getTahun() > 1900) {
									ket += ", Tahun " + tagihan.getTahun();
								}
								Double denda = tagihan.getDenda();
								if (denda > 0.01) {
									ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
								}
								Date tglDeadline = tagihan.getTanggalDeadline();
								if (tglDeadline != null) {
									ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
								}

								ket += (tagihan.getDiskonSiswa() != null ? " - " + tagihan.getDiskonSiswa().getNama()
										: "");

								goodsNm += goodsNm.isEmpty() ? ket : ";" + ket;
								amt += tagihan.getNominal();
							}

						}

						jsonObject.put("amt", amt.intValue() + "");
						jsonObject.put("goodsNm", goodsNm);
						jsonObject.put("referenceNo", nim);
						jsonObject.put("billingNm", nama);
					} else {

						JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
						Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
						BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();

						Integer semester = virtualAccountBankNtt.getSemester();

						nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
						nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

						Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
								: session.createCriteria(Kegiatan.class)
										.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan())).uniqueResult());

						if (kegiatan == null || kegiatan.getId() == null) {

							kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)

									.addOrder(Order.asc("id"))

									.add(biodataCalonMahasiswa != null
											? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
											: Restrictions.eq("mahasiswa", mahasiswa))
									.add(Restrictions.eq("jenisKegiatan", virtualAccountBankNtt.getJenisKegiatan()))
									.add(Restrictions.eq("semster", semester))

									.setMaxResults(1).uniqueResult();
						}

						if (kegiatan == null || kegiatan.getId() == null) {
							kegiatan = new Kegiatan();
						}

						kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
						kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
						kegiatan.setNama(nama);
						kegiatan.setUploadVirtualAccount(null);
						kegiatan.setAmount(virtualAccountBankNtt.getTotal());
						kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
						kegiatan.setMahasiswa(mahasiswa);
						kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
						kegiatan.setSemster(semester);
						kegiatan.setJenisKegiatan(jenisKegiatan);
						kegiatan.setTanggal(tanggal);
						kegiatan.setValidated(1);
						kegiatan.setValidator(bank);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, kegiatan);
						session.getTransaction().commit();

						List<Long> detailBiayasId = new ArrayList<Long>();
						for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
							try {
								detailBiayasId.add(Long.parseLong(id.trim()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:267");

							}
						}

						Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
								.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("id", detailBiayasId))
								.list();

						Double nilaiBiayaHarusDiBayars = 0.0;
						for (DetailBiaya detailBiaya : detailBiayas) {
							Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

							nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

						}

						if (inquery) {

							Double total = 0.0;
							if (virtualAccountBankNtt.getCicilan() != null
									&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
								for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
									if (Common.isNumber(idPemBul)) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

										if (pengaturanPembayaranBulanan != null) {

											Double subtotal = pengaturanPembayaranBulanan
													.ambilNominalModifikasi(mahasiswa, semester);
											total += subtotal;

											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;

										}
									} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
												.uniqueResult();

										if (pengaturanPembayaranBulanan != null) {

											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[spl.length - 1]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:319");
											}

											if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}
											total += subtotal;
											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;
										}
									} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
										ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
												.uniqueResult();

										if (itemBiaya != null) {

											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[2]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:342");
											}

											@SuppressWarnings("unused")
											Integer bayarke = 1;
											try {
												String[] spl = idPemBul.split("-");
												bayarke = Integer.parseInt(spl[3]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:350");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}
											total += subtotal;

											String t = itemBiaya.getNama();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;
										}
									}
								}

							}

							jsonObject.put("amt", total.intValue() + "");
							jsonObject.put("goodsNm", goodsNm);
							jsonObject.put("referenceNo", nim);
							jsonObject.put("billingNm", nama);
						}

						else {

//							Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
//							Double total = totalCicilan[0];
//							Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
//							System.out.println("cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

							if (virtualAccountBankNtt.getCicilan() != null
									&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
								for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
									if (Common.isNumber(idPemBul)) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

										if (pengaturanPembayaranBulanan != null) {
											String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
													+ virtualAccountBankNtt.getId();

											ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya();

											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[spl.length - 1]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:398");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();

											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														pengaturanPembayaranBulanan.getDetailBiaya());
											}
											cicilanPembayaran.setRef(ref);
											cicilanPembayaran.setValidator(bank);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setItemBiaya(
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya());
											cicilanPembayaran
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
											cicilanPembayaran.setNilai(subtotal);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											cicilanPembayaran.setTanggal(tanggal);
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;

										}

									} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
												.uniqueResult();

										if (pengaturanPembayaranBulanan != null) {
											String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
													+ virtualAccountBankNtt.getId();

											ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya();
											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[spl.length - 1]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:457");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();

											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														pengaturanPembayaranBulanan.getDetailBiaya());
											}
											cicilanPembayaran.setRef(ref);
											cicilanPembayaran.setValidator(bank);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setItemBiaya(itemBiaya);
											cicilanPembayaran
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

											cicilanPembayaran.setNilai(subtotal);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											cicilanPembayaran.setTanggal(tanggal);
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;

										}

									} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
										ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
												.simpleObject(
														session.createCriteria(ItemBiaya.class)
																.add(Restrictions
																		.idEq(Long.parseLong(idPemBul.split("-")[1]))),
														ItemBiaya.class);

										if (itemBiaya != null) {
											String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
													+ virtualAccountBankNtt.getId();
											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[2]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:514");
											}

											Long detailBiayaId = null;
											try {
												String[] spl = idPemBul.split("-");
												detailBiayaId = Long.parseLong(spl[4]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:521");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();

											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														DetailBiaya.muatRefAman(session, detailBiayaId));
											}
											cicilanPembayaran.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
											cicilanPembayaran.setRef(ref);
											cicilanPembayaran.setValidator(bank);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setItemBiaya(itemBiaya);
											cicilanPembayaran.setPengaturanPembayaranBulanan(null);
											cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

											cicilanPembayaran.setNilai(subtotal);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											cicilanPembayaran.setTanggal(tanggal);
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											String t = itemBiaya.getNama();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;
										}

									} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
										// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
										// menjadi Kegiatan+Cicilan nyata â€” pemroses terpusat yang sama dengan Esmartlink.
										ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
												virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
									}
								}
							}

							Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
							Double jumlah = d[0];
							Double denda = d[1];
							kegiatan.setDenda(denda.doubleValue());
							kegiatan.setAmountTerhutang(
									nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

							kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
									: virtualAccountBankNtt.getTotal());
							kegiatan.setValidator(bank);

							session.getTransaction().begin();
							Common.refreshUpdate(session, kegiatan);
							session.getTransaction().commit();

							VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data, bank);

						}
					}

				}

				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:594");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:595");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:596");}
					}
				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				try {
					jsonObject.put("goodsNm", goodsNm);
				} catch (Exception eGoods) { ais.common.ErrorAuditUtil.record(eGoods, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:609");
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						jsonObject.toString(), nominalP, goodsNm, h2hStackTrace);
			}
		}
		return jsonObject.toString();
	}

	/**
	 * Mengurai payload JSON MNC Bank lalu memanggil {@link #doProcess}.
	 *
	 * <p>Nomor VA dibentuk dengan merangkai kunci {@code prefix} dan {@code customerId}. Mode
	 * operasi ditentukan oleh ada tidaknya kunci {@code amt}: bila {@code amt} tidak ada,
	 * {@code inquery} bernilai {@code true} sehingga pemrosesan hanya membaca; bila ada,
	 * pemrosesan berlanjut ke jalur pembayaran yang menulis ke basis data.</p>
	 *
	 * <p>Tanggal transaksi selalu diambil dari jam server, bukan dari payload. Bila
	 * {@link #doProcess} melempar kegagalan, method mengembalikan literal {@code "ERROR"}
	 * sehingga bank menerima badan balasan yang bukan JSON.</p>
	 *
	 * @param data     payload JSON mentah dari bank
	 * @param request  permintaan asal, diteruskan ke pencatat log H2H
	 * @param bankHost host bank hasil pemetaan IP; boleh {@code null}
	 * @param bank     label bank yang dipakai sebagai {@code validator}
	 * @param chek     {@code true} untuk melewati pemeriksaan kedaluwarsa dan status lunas
	 * @return string JSON balasan, atau {@code "ERROR"} bila pemrosesan gagal
	 * @throws Exception bila payload bukan JSON yang sah sehingga penguraian awal gagal
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = new JSONObject(data);

		String va = req.get("prefix") + "" + req.get("customerId") + "";

		double nominalP = 0.0;
		try {
			nominalP = Double.parseDouble(req.get("amt") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:627");
			// TODO: handle exception
		}

		String tanggalP = Common.databaseDateFormat1.get().format(WaktuUtil.getDate());

		String body;
		try {
			body = MncBank
					.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, req.isNull("amt"), chek)
					.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MncBank.java:639");
			body = "ERROR";
		}

		return body;
	}

	/**
	 * Membaca permintaan HTTP mentah, memprosesnya, dan menuliskan balasan JSON.
	 *
	 * <p>Langkah yang dijalankan berurutan:</p>
	 * <ol>
	 *   <li>seluruh nama dan nilai header ditelusuri lalu dicetak ke {@code System.out};</li>
	 *   <li><i>body</i> permintaan dibaca baris demi baris menjadi satu string JSON — perhatikan
	 *       bahwa pemisah baris dibuang sehingga payload multi-baris digabung rapat;</li>
	 *   <li>alamat IP pemanggil dipetakan menjadi {@link BankHost} dengan label {@code "MNC Bank"};</li>
	 *   <li>bila payload tidak kosong, {@link #doProses} dipanggil dengan {@code chek} bernilai
	 *       {@code false}; payload kosong menghasilkan balasan kosong;</li>
	 *   <li>hasil ditulis sebagai {@code application/json} disertai header {@code length} khusus
	 *       yang berisi panjang badan balasan.</li>
	 * </ol>
	 *
	 * <p><b>Keamanan:</b> tidak ada satu pun pemeriksaan kredensial di sini. Header dibaca semata
	 * untuk dicetak, dan hasil pemetaan {@link BankHost} tidak pernah diuji sebelum pemrosesan
	 * dilanjutkan. Pencetakan seluruh header, payload, dan <i>query string</i> ke keluaran standar
	 * juga berarti bahan autentikasi apa pun yang dikirim mitra akan tersalin ke log server.</p>
	 *
	 * @param request  permintaan masuk dari sistem bank
	 * @param response balasan yang akan diisi JSON hasil pemrosesan
	 * @throws Exception bila pembacaan permintaan atau penulisan balasan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {

			String headerName = headerNames.nextElement();

			Enumeration<String> headers = request.getHeaders(headerName);
			while (headers.hasMoreElements()) {
				String headerValue = headers.nextElement();
				System.out.println("==> headerName => " + headerName + " headerValue " + headerValue);
			}

		}

		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> VA data => " + data);
		System.out.println("==> VA querystring => " + querystring);

		String bank = "MNC Bank";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = data == null || data.isEmpty() ? "" : doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

}

