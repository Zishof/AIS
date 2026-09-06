package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
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
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;

/**
 * Servlet penerima notifikasi pembayaran <b>FinPay</b>.
 *
 * <p>Berbeda dari {@code ais.action.servlet.FinPayResponse} yang dipetakan ke
 * {@code /FinPayResponse}, kelas ini dipetakan ke {@code /Finpay} dan menangani bentuk
 * notifikasi bersarang milik FinPay.</p>
 *
 * <h4>Bentuk pesan</h4>
 * <p>Permintaan berupa JSON pada badan dengan struktur bersarang:</p>
 * <pre>
 * {
 *   "order":  { "id": "&lt;nomor VA&gt;", "amount": &lt;nominal&gt; },
 *   "result": { "payment": { "status": "PAID", "datetime": "&lt;tanggal&gt;" } }
 * }
 * </pre>
 * <p>Hanya {@code status} bernilai {@code "PAID"} yang diperlakukan sebagai pembayaran
 * nyata; nilai lain diproses sebagai inquiry yang tidak membukukan apa pun. Balasan berupa
 * teks biasa {@code "OK"}, {@code "ALREADY_PAID"}, atau {@code "ERROR"}.</p>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tanda tangan notifikasi tidak pernah diperiksa</h4>
 * <ul>
 *   <li>{@link #process} membaca seluruh header <b>hanya untuk mencetaknya</b> ke
 *       {@code System.out}. Token maupun tanda tangan yang dikirim FinPay tidak pernah
 *       diambil, apalagi diverifikasi.</li>
 *   <li>Pemetaan alamat IP menjadi {@link BankHost} <b>tidak dipakai sebagai gerbang</b>;
 *       pemrosesan berjalan meskipun hasilnya {@code null}. Pemetaan itu sendiri punya dua
 *       jalur pelonggaran: konfigurasi
 *       {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} yang membuat baris
 *       {@link BankHost} baru untuk IP pemanggil apa pun, dan baris cadangan ber-IP
 *       {@code 0.0.0.0}.</li>
 *   <li>Pada {@code applicationContext-security.xml} URL {@code /Finpay} jatuh ke aturan
 *       penampung {@code /**} yang bernilai {@code IS_AUTHENTICATED_ANONYMOUSLY}.</li>
 * </ul>
 * <p>Akibatnya medan {@code result.payment.status} &mdash; satu-satunya penentu apakah sebuah
 * tagihan dibukukan lunas &mdash; sepenuhnya berada di tangan pengirim pesan, tanpa apa pun
 * yang membuktikan pesan itu benar berasal dari FinPay.</p>
 *
 * <h4>Catatan arsitektur</h4>
 * <p>Setiap permintaan wajib menghasilkan satu baris {@code LogHostToHost} lewat
 * {@code PembayaranGatewayHelper.catatLogHostToHost} di blok {@code finally}, tanpa syarat
 * {@code bankHost}. Ini pola <i>audit shadow</i> yang berlaku di seluruh gerbang pembayaran
 * AIS dan merupakan fakta arsitektur yang disengaja, bukan cacat.</p>
 *
 * @see ais.database.model.VirtualAccountBank
 * @see ais.action.ws.util.PembayaranGatewayHelper
 */
public class Finpay extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton pembantu pembayaran, dipakai di {@link #process} untuk memetakan alamat IP
	 * pemanggil menjadi {@link BankHost}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun; seluruh kebergantungan diambil lewat field
	 * statis {@link #pembayaranUtil}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Finpay() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Servlet ini memperlakukan GET, POST, PUT, dan TRACE secara identik karena bentuk
	 * pengiriman notifikasi dapat berbeda antar konfigurasi mitra. Kegagalan ditelan
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga pengirim tidak menerima 5xx.</p>
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat
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
	 * Menangani permintaan HTTP POST &mdash; metode yang lazim dipakai FinPay &mdash; dengan
	 * meneruskannya ke {@link #process}.
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat
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
	 * <p>Perilaku sama persis dengan {@link #doGet}.</p>
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat
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
	 * <p>Menimpa perilaku bawaan {@link HttpServlet#doTrace} yang seharusnya hanya memantulkan
	 * header; di sini TRACE diperlakukan sebagai kanal notifikasi biasa.</p>
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat
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
	 * Memproses satu notifikasi FinPay: menyelesaikan VA-nya lalu membukukan pembayaran bila
	 * memang bermode pembayaran.
	 *
	 * <p>VA dicari lewat {@link VirtualAccountBank#ambilVa(String, Double, BankHost)}. VA yang
	 * tidak ditemukan, atau yang totalnya tidak lebih dari 0,1, membuat method berakhir tanpa
	 * membukukan apa pun. VA yang sudah lunas dijawab {@code "ALREADY_PAID"}.</p>
	 *
	 * <h4>Dua cabang pemilik tagihan</h4>
	 * <ul>
	 *   <li><b>Sekolah</b> &mdash; bila VA menunjuk {@code siswa} atau {@code calonSiswa},
	 *       pembukuan diserahkan ke {@link VirtualAccountBank#bayarSiswa}, dengan {@code inquery}
	 *       diteruskan apa adanya sehingga mode inquiry tidak menyimpan apa pun.</li>
	 *   <li><b>Perguruan tinggi</b> &mdash; sebuah {@link Kegiatan} dicari atau dibuat, lalu
	 *       daftar {@code cicilan} pada VA diurai per token: angka murni dan awalan
	 *       {@code Bulanan-} menunjuk {@link PengaturanPembayaranBulanan}, awalan {@code Item-}
	 *       menunjuk {@link ItemBiaya}, dan awalan {@code Keranjang-} diserahkan ke
	 *       {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}. Tiap token menghasilkan
	 *       satu {@link CicilanPembayaran} yang di-<i>idempoten</i>-kan lewat kolom {@code ref},
	 *       sekaligus satu entri rincian pada larik JSON yang ikut disimpan ke log H2H.</li>
	 * </ul>
	 *
	 * <p>Pada mode pembayaran, setelah semua token selesai total dan denda dihitung ulang,
	 * {@link Kegiatan} disimpan, dan VA ditandai lunas lewat
	 * {@link VirtualAccountBank#updateVa}.</p>
	 *
	 * <p><b>Keamanan:</b> method ini menerima {@code bankHost} bernilai {@code null} dan tetap
	 * memproses; seluruh pemeriksaan yang ada bersifat konsistensi data, <b>bukan</b> otorisasi.
	 * Lihat peringatan pada dokumentasi kelas.</p>
	 *
	 * <p>Apa pun hasilnya, satu baris log H2H selalu ditulis di blok {@code finally} &mdash;
	 * termasuk jejak <i>stack trace</i> bila terjadi galat.</p>
	 *
	 * @param nominalP  nominal setoran yang dilaporkan mitra
	 * @param tanggalP  tanggal transaksi dalam format {@code Common.databaseDateFormat1}; bila
	 *                  gagal diurai dipakai waktu server saat ini
	 * @param va        nomor VA, diambil dari {@code order.id}
	 * @param bank      label bank yang disimpan sebagai {@code validator} pada data pembayaran
	 * @param bankHost  host bank hasil pemetaan IP; boleh {@code null} dan tidak menjadi gerbang
	 * @param request   permintaan asal, diteruskan ke pencatat log H2H
	 * @param data      payload JSON mentah, disimpan apa adanya pada log H2H
	 * @param chekLagi  penanda warisan; saat ini tidak dipakai di badan method
	 * @param inquery   {@code true} untuk inquiry (tidak membukukan), {@code false} untuk
	 *                  pembayaran
	 * @param chek      {@code true} untuk melewati pemeriksaan status lunas
	 * @return {@code "OK"} bila selesai, atau {@code "ALREADY_PAID"} bila tagihan sudah lunas
	 * @throws Exception bila kegagalan terjadi di luar jangkauan penanganan internal
	 */
	@SuppressWarnings("unchecked")
	public static String doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean chek) throws Exception {

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va,  nominalP, bankHost);

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {
						if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, false, chek)) {
							return "ALREADY_PAID";
						}
					Session session = null;
					try {
					session = HibernateUtil.getSessionFactory().openSession();
					{

						Date tanggal = ais.ui.util.WaktuUtil.getDate();
						try {
							tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:135");
//							e.printStackTrace();
						}

						if (virtualAccountBankNtt.getSiswa() != null || virtualAccountBankNtt.getCalonSiswa() != null) {

							if (virtualAccountBankNtt.getSiswa() != null) {

								nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
								nama = virtualAccountBankNtt.getSiswa().getNama();

							} else if (virtualAccountBankNtt.getCalonSiswa() != null) {

								nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
								nama = virtualAccountBankNtt.getCalonSiswa().getNama();

							}

							VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session, tanggal, bank, inquery, data, false);

						} else {

							JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
							Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
							BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
									.getBiodataCalonMahasiswa();

							Integer semester = virtualAccountBankNtt.getSemester();

							nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
							nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

							Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
									: session.createCriteria(Kegiatan.class)
											.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
											.uniqueResult());

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
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:213");

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

							Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
							Double total = totalCicilan[0];
							Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
							System.out.println("cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:255");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											if (!inquery) {

												CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
														.createCriteria(CicilanPembayaran.class)
														.add(Restrictions.eq("ref", ref)).setMaxResults(1)
														.uniqueResult();

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

											}

											JSONObject jsonObjectRinci = new JSONObject();
											jsonObjectRinci.put("nama", pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya().getNama());
											jsonObjectRinci.put("bulan", pengaturanPembayaranBulanan.getNamaBulan());
											jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
													.ambilNominalModifikasi(mahasiswa, semester));

											rincian.put(jsonObjectRinci);
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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:323");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											if (!inquery) {

												CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
														.createCriteria(CicilanPembayaran.class)
														.add(Restrictions.eq("ref", ref)).setMaxResults(1)
														.uniqueResult();

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

											}
											JSONObject jsonObjectRinci = new JSONObject();
											jsonObjectRinci.put("nama", pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya().getNama());
											jsonObjectRinci.put("bulan", pengaturanPembayaranBulanan.getNamaBulan());
											jsonObjectRinci.put("nominal", subtotal);

											rincian.put(jsonObjectRinci);
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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:387");
											}

											Long detailBiayaId = null;
											try {
												String[] spl = idPemBul.split("-");
												detailBiayaId = Long.parseLong(spl[4]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:394");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											if (!inquery) {

												CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
														.createCriteria(CicilanPembayaran.class)
														.add(Restrictions.eq("ref", ref)).setMaxResults(1)
														.uniqueResult();

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

											}
											JSONObject jsonObjectRinci = new JSONObject();
											jsonObjectRinci.put("nama", itemBiaya.getNama());
											jsonObjectRinci.put("bulan", "");
											jsonObjectRinci.put("nominal", subtotal);

											rincian.put(jsonObjectRinci);
										}

									} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
										// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
										// menjadi Kegiatan+Cicilan nyata â€” pemroses terpusat yang sama dengan Esmartlink.
										ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
												virtualAccountBankNtt, inquery, bank, bankHost, tanggal, data, null);
									}
								}
							}

							if (!inquery) {
								Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
										kegiatan);
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
							try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:476");}
							try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:477");}
							try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:478");}
						}
					}
				}

			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						"OK", nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return "OK";
	}

	/**
	 * Mengurai payload JSON bersarang FinPay lalu memanggil {@link #doProcess}.
	 *
	 * <p>Nomor VA diambil dari {@code order.id} dan nominal dari {@code order.amount}; tanggal
	 * transaksi dari {@code result.payment.datetime}. Kesalahan pada nominal maupun tanggal
	 * ditelan sehingga nominal bernilai {@code 0.0} dan tanggal bernilai string kosong &mdash;
	 * yang kemudian membuat {@link #doProcess} memakai waktu server.</p>
	 *
	 * <p>Mode operasi ditentukan medan {@code result.payment.status}: hanya nilai {@code "PAID"}
	 * (tanpa memandang besar kecil huruf) yang membuat {@code inquery} bernilai {@code false}
	 * sehingga pembayaran benar-benar dibukukan.</p>
	 *
	 * <p><b>Perhatikan:</b> pembacaan {@code order} di awal method tidak dijaga, sehingga payload
	 * tanpa objek {@code order} menggagalkan method sebelum blok {@code try} yang menangkap
	 * galat. Sebaliknya, kegagalan membaca {@code result.payment.status} tertangkap dan
	 * menghasilkan balasan {@code "ERROR"}.</p>
	 *
	 * @param data     payload JSON mentah dari FinPay
	 * @param request  permintaan asal, diteruskan ke pencatat log H2H
	 * @param bankHost host bank hasil pemetaan IP; boleh {@code null}
	 * @param bank     label bank yang dipakai sebagai {@code validator}
	 * @param chek     {@code true} untuk melewati pemeriksaan status lunas
	 * @return hasil {@link #doProcess}, atau {@code "ERROR"} bila pemrosesan gagal
	 * @throws Exception bila payload bukan JSON yang sah atau tidak memuat objek {@code order}
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = new JSONObject(data);

		JSONObject order = req.getJSONObject("order");

		String va = order.get("id") + "";

		double nominalP = 0.0;
		try {
			nominalP = Double.parseDouble(order.get("amount") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:509");
			// TODO: handle exception
		}

		String tanggalP = "";
		try {
			tanggalP = req.getJSONObject("result").getJSONObject("payment").get("datetime") + "";
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Finpay.java:516");
			// TODO: handle exception
		}

		String body;
		try {
			String status = req.getJSONObject("result").getJSONObject("payment").get("status").toString();
			boolean masuk = status.trim().equalsIgnoreCase("PAID");
			body = Finpay.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, !masuk, chek)
					.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Finpay.java:527");
			body = "ERROR";
		}

		return body;
	}

	/**
	 * Membaca notifikasi FinPay dari badan permintaan, memprosesnya, dan menuliskan balasan.
	 *
	 * <p>Langkah yang dijalankan berurutan:</p>
	 * <ol>
	 *   <li>seluruh nama dan nilai header ditelusuri lalu dicetak ke {@code System.out};</li>
	 *   <li>badan permintaan dibaca baris demi baris menjadi satu string JSON &mdash; pemisah
	 *       baris dibuang sehingga payload multi-baris digabung rapat;</li>
	 *   <li>payload dan <i>query string</i> dicetak ke {@code System.out};</li>
	 *   <li>alamat IP pemanggil dipetakan menjadi {@link BankHost} dengan label
	 *       {@code "Finpay"};</li>
	 *   <li>bila payload tidak kosong, {@link #doProses} dipanggil dengan {@code chek} bernilai
	 *       {@code false}; payload kosong menghasilkan balasan kosong;</li>
	 *   <li>hasil ditulis disertai header {@code length} khusus berisi panjang badan balasan, dan
	 *       header {@code Content-Type} bernilai {@code application/json} &mdash; padahal isinya
	 *       sebenarnya teks biasa seperti {@code "OK"}.</li>
	 * </ol>
	 *
	 * <p><b>Keamanan:</b> header dibaca semata untuk dicetak, sehingga tanda tangan atau token
	 * FinPay tidak pernah diverifikasi; hasil pemetaan {@link BankHost} juga tidak pernah diuji
	 * sebelum pemrosesan dilanjutkan. Pencetakan seluruh header, payload, dan <i>query string</i>
	 * ke keluaran standar berarti bahan autentikasi apa pun yang dikirim mitra tersalin ke log
	 * server. Lihat peringatan pada dokumentasi kelas.</p>
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat
	 * @throws Exception bila pembacaan permintaan, penguraian JSON, atau penulisan balasan gagal
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

		String bank = "Finpay";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = data == null || data.isEmpty() ? "" : doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

}

