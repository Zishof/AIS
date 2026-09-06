package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.PembayaranUtilHelper;
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
import ais.database.model.KegiatanTemporary;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.WaktuUtil;

/**
 * Servlet <i>host-to-host</i> untuk kanal pembayaran <b>Maja</b>.
 *
 * <p>Nama kanal tidak dijelaskan di mana pun pada kode; satu-satunya penandanya adalah
 * literal {@code "Maja"} yang dipakai sebagai label {@code validator} pada data pembayaran
 * dan sebagai nama saat memetakan alamat IP menjadi {@link BankHost}.</p>
 *
 * <h4>Bentuk pesan</h4>
 * <p>Permintaan berupa JSON pada badan dengan kunci {@code va}, {@code amount}, dan
 * {@code date}; kunci {@code va} dan {@code date} boleh diambil dari parameter permintaan
 * bila tidak ada di JSON. Balasan berupa JSON berkunci {@code response_code} dan
 * {@code response_message}:</p>
 * <ul>
 *   <li>{@code 0000} &mdash; berhasil;</li>
 *   <li>{@code 0005} &mdash; dipakai untuk tiga keadaan berbeda: nilai awal
 *       {@code "Alamat IP Tidak terdaftar"}, tagihan kedaluwarsa, dan kesalahan internal;</li>
 *   <li>{@code 0013} &mdash; nominal tidak sesuai;</li>
 *   <li>{@code 0014} &mdash; tagihan sudah terbayar.</li>
 * </ul>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; endpoint ini TIDAK memiliki autentikasi</h4>
 * <ul>
 *   <li>{@link #process} tidak memeriksa tanda tangan, token, kunci API, Basic Auth, maupun
 *       mTLS.</li>
 *   <li>Pemetaan alamat IP menjadi {@link BankHost} <b>tidak dipakai sebagai gerbang</b>.
 *       Pesan bawaan {@code "Alamat IP Tidak terdaftar"} memberi kesan ada penyaringan IP,
 *       tetapi itu hanya nilai awal yang ditimpa begitu VA ditemukan; pemrosesan tetap
 *       berjalan meskipun {@code bankHost} bernilai {@code null}. Pemetaan itu sendiri punya
 *       dua jalur pelonggaran: konfigurasi
 *       {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} yang membuat baris
 *       {@link BankHost} baru untuk IP pemanggil apa pun, dan baris cadangan ber-IP
 *       {@code 0.0.0.0}.</li>
 *   <li>Pada {@code applicationContext-security.xml} URL {@code /Maja} jatuh ke aturan
 *       penampung {@code /**} yang bernilai {@code IS_AUTHENTICATED_ANONYMOUSLY}.</li>
 * </ul>
 * <p>Berbeda dari sebagian gerbang lain, kanal ini <b>tidak punya mode inquiry</b>: setiap
 * permintaan yang lolos pemeriksaan nominal langsung membukukan pembayaran. Satu panggilan
 * tanpa autentikasi karena itu cukup untuk menandai sebuah tagihan lunas.</p>
 *
 * <h4>Catatan arsitektur</h4>
 * <p>Setiap permintaan &mdash; berhasil maupun gagal, dari IP dikenal maupun tidak &mdash;
 * wajib menghasilkan satu baris {@code LogHostToHost} lewat
 * {@code PembayaranGatewayHelper.catatLogHostToHost} di blok {@code finally}. Ini pola
 * <i>audit shadow</i> yang berlaku di seluruh gerbang pembayaran AIS dan merupakan fakta
 * arsitektur yang disengaja, bukan cacat.</p>
 *
 * @see ais.database.model.VirtualAccountBank
 * @see ais.action.ws.util.PembayaranGatewayHelper
 */
public class Maja extends HttpServlet {
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
	public Maja() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Perilakunya sama dengan POST karena payload selalu dibaca dari badan permintaan.
	 * Kegagalan ditelan {@link Common#tampilErrorJikaAdmin(Exception)} sehingga mitra tidak
	 * menerima kode status 5xx; pada kondisi itu badan balasan bisa kosong.</p>
	 *
	 * @param request  permintaan masuk dari mitra
	 * @param response balasan yang akan diisi JSON hasil
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
	 * Menangani permintaan HTTP POST dengan meneruskannya ke {@link #process}.
	 *
	 * <p>Perilaku sama persis dengan {@link #doGet}.</p>
	 *
	 * @param request  permintaan masuk dari mitra
	 * @param response balasan yang akan diisi JSON hasil
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
	 * Membukukan pembayaran satu nomor VA dan menyusun balasan untuk mitra.
	 *
	 * <h4>Urutan pemeriksaan</h4>
	 * <ol>
	 *   <li>VA dicari lewat {@link VirtualAccountBank#ambilVa(String, Double, BankHost)}. Bila
	 *       tidak ada, atau totalnya tidak lebih dari 0,1, balasan tetap memakai nilai awal
	 *       {@code 0005} sehingga mitra menerima pesan {@code "Alamat IP Tidak terdaftar"} yang
	 *       sebenarnya menyesatkan.</li>
	 *   <li>{@code nominalP} harus sama persis dengan {@code biayaAdmin + total}; bila tidak,
	 *       balasan {@code 0013 Invalid Amount}.</li>
	 *   <li>Pemeriksaan kedaluwarsa berada di belakang syarat {@code !chekLagi}. <b>Perhatikan:</b>
	 *       satu-satunya pemanggil, {@link #process}, selalu mengirim {@code chekLagi} bernilai
	 *       {@code true}, sehingga pemeriksaan kedaluwarsa pada praktiknya <b>tidak pernah
	 *       dijalankan</b> dan VA yang sudah lewat tanggal tetap dapat dibayar.</li>
	 *   <li>VA yang sudah lunas dibalas {@code 0014 Tagihan sudah terbayar}.</li>
	 * </ol>
	 *
	 * <h4>Dua cabang pemilik tagihan</h4>
	 * <ul>
	 *   <li><b>Sekolah</b> &mdash; bila VA menunjuk {@code siswa} atau {@code calonSiswa},
	 *       pembukuan diserahkan ke {@link VirtualAccountBank#bayarSiswa}.</li>
	 *   <li><b>Perguruan tinggi</b> &mdash; sebuah {@link Kegiatan} dicari atau dibuat, lalu
	 *       daftar {@code cicilan} pada VA diurai per token: angka murni dan awalan
	 *       {@code Bulanan-} menunjuk {@link PengaturanPembayaranBulanan}, awalan {@code Item-}
	 *       menunjuk {@link ItemBiaya}. Tiap token menghasilkan satu {@link CicilanPembayaran}
	 *       yang di-<i>idempoten</i>-kan lewat kolom {@code ref}, sekaligus satu entri rincian
	 *       pada larik JSON yang ikut disimpan ke log H2H. Setelah semua token selesai, total
	 *       dan denda dihitung ulang, {@link Kegiatan} disimpan, dan VA ditandai lunas lewat
	 *       {@link VirtualAccountBank#updateVa}.</li>
	 * </ul>
	 *
	 * <p><b>Keamanan:</b> method ini menerima {@code bankHost} bernilai {@code null} dan tetap
	 * memproses. Seluruh pemeriksaan di atas bersifat konsistensi data, <b>bukan</b> otorisasi;
	 * lihat peringatan pada dokumentasi kelas.</p>
	 *
	 * <p>Kegagalan apa pun menghasilkan {@code 0005 Terjadi kesalahan internal} setelah transaksi
	 * yang masih aktif di-<i>rollback</i>. Session ditutup dan log H2H ditulis di blok
	 * {@code finally}.</p>
	 *
	 * @param nominalP  nominal setoran yang dilaporkan mitra
	 * @param tanggalP  tanggal transaksi dalam format {@code Common.databaseDateFormat1}; bila
	 *                  gagal diurai dipakai waktu server saat ini
	 * @param va        nomor Virtual Account
	 * @param bank      label bank yang disimpan sebagai {@code validator} pada data pembayaran
	 * @param bankHost  host bank hasil pemetaan IP; boleh {@code null} dan tidak menjadi gerbang
	 * @param request   permintaan asal, diteruskan ke pencatat log H2H
	 * @param data      payload JSON mentah, disimpan apa adanya pada log H2H
	 * @param chekLagi  {@code true} melewati pemeriksaan kedaluwarsa; selalu bernilai {@code true}
	 *                  pada satu-satunya pemanggil
	 * @return objek JSON balasan berisi {@code response_code} dan {@code response_message}
	 * @throws Exception bila kegagalan terjadi di luar jangkauan penanganan internal
	 */
	@SuppressWarnings({ "unchecked", "static-access" })
	public static JSONObject doProcess(int nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi) throws Exception {
		JSONObject jsonObjectResponse = new JSONObject();
		jsonObjectResponse.put("response_code", "0005");
		jsonObjectResponse.put("response_message", "Error Other, Alamat IP Tidak terdaftar");
		String body = jsonObjectResponse.toString();

		String nim = "";
		String nama = "";
		JSONArray rincian = new JSONArray();
		// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;

		VirtualAccountBank virtualAccountBankNtt = null;
		Session session = null;
		try {

			virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, (double)nominalP,  bankHost);

			if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {
				session = HibernateUtil.openSession();
				{

					int nominal = nominalP;

					if (nominal != (virtualAccountBankNtt.getBiayaAdmin().intValue()
							+ virtualAccountBankNtt.getTotal().intValue())) {
						jsonObjectResponse = new JSONObject();
						jsonObjectResponse.put("response_code", "0013");
						jsonObjectResponse.put("response_message", "Invalid Amount");
						body = jsonObjectResponse.toString();
					} else {

						Date tanggal = ais.ui.util.WaktuUtil.getDate();
						try {
							tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:124");
							// TODO: handle exception
						}

						if (!chekLagi && virtualAccountBankNtt != null
								&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
							jsonObjectResponse.put("response_code", "0005");
							jsonObjectResponse.put("response_message", "Pembayaran kadaluwarsa");
						} else {


								if (VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt)) {
									jsonObjectResponse = new JSONObject();
									jsonObjectResponse.put("response_code", "0014");
									jsonObjectResponse.put("response_message", "Tagihan sudah terbayar");
									body = jsonObjectResponse.toString();
								} else if (virtualAccountBankNtt.getSiswa() != null
									|| virtualAccountBankNtt.getCalonSiswa() != null) {

								if (virtualAccountBankNtt.getSiswa() != null) {

									nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getSiswa().getNama();

								} else if (virtualAccountBankNtt.getCalonSiswa() != null) {

									nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getCalonSiswa().getNama();

								}

								VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session, tanggal, bank, false,
										data, false);

								jsonObjectResponse = new JSONObject();
								jsonObjectResponse.put("response_code", "0000");
								jsonObjectResponse.put("response_message", "Success");
								body = jsonObjectResponse.toString();

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
//								String kodeUnik = Kegiatan.generateKodeUnik(mahasiswa, biodataCalonMahasiswa,
//										virtualAccountBankNtt.getJenisKegiatan(), semester,
//										virtualAccountBankNtt.getJadwalPembayaran(), "", null);

									kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)
											.addOrder(Order.asc("id"))

											.add(biodataCalonMahasiswa != null
													? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
													: Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("jenisKegiatan",
													virtualAccountBankNtt.getJenisKegiatan()))
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
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:224");

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
								System.out.println(
										"cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

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
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:267");
												}

												if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
													subtotal = 0.0 - subtotal;
												}

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
												if (bankHost != null) {
													cicilanPembayaran.setJenisPembayaran(
															bankHost == null || bankHost.getJenisPembayaran() == null
																	? ConstantValues.TUNAI
																	: bankHost.getJenisPembayaran());
												}
												cicilanPembayaran.setDenda(0.0);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

												session.getTransaction().begin();
												if (cicilanPembayaran.getId() == null)
													session.save(cicilanPembayaran);
												else
													Common.refreshUpdate(session, cicilanPembayaran);
												session.getTransaction().commit();

												JSONObject jsonObjectRinci = new JSONObject();
												jsonObjectRinci.put("nama", pengaturanPembayaranBulanan.getDetailBiaya()
														.getItemBiaya().getNama());
												jsonObjectRinci.put("bulan",
														pengaturanPembayaranBulanan.getNamaBulan());
												jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
														.ambilNominalModifikasi(mahasiswa, semester));

												rincian.put(jsonObjectRinci);

											} else {

											}
										}

										else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
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
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:342");
												}

												if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
													subtotal = 0.0 - subtotal;
												}
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
												if (bankHost != null) {
													cicilanPembayaran.setJenisPembayaran(
															bankHost == null || bankHost.getJenisPembayaran() == null
																	? ConstantValues.TUNAI
																	: bankHost.getJenisPembayaran());
												}
												cicilanPembayaran.setDenda(0.0);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												session.getTransaction().begin();
												if (cicilanPembayaran.getId() == null)
													session.save(cicilanPembayaran);
												else
													Common.refreshUpdate(session, cicilanPembayaran);
												session.getTransaction().commit();

												JSONObject jsonObjectRinci = new JSONObject();
												jsonObjectRinci.put("nama", pengaturanPembayaranBulanan.getDetailBiaya()
														.getItemBiaya().getNama());
												jsonObjectRinci.put("bulan",
														pengaturanPembayaranBulanan.getNamaBulan());
												jsonObjectRinci.put("nominal", subtotal);

												rincian.put(jsonObjectRinci);

											}
										}

										else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
											KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) session
													.createCriteria(KegiatanTemporary.class)
													.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
													.uniqueResult();

											if (kegiatanTemporary != null) {
												String ref = "ntt-keranjang-" + kegiatanTemporary.getId() + "-"
														+ idPemBul + "-" + virtualAccountBankNtt.getId();

												Session sessionLocalKeg = HibernateUtil.openSession();
												try {

													mahasiswa = kegiatanTemporary.getMahasiswa();
													Integer smt = kegiatanTemporary.getSemster();
													jenisKegiatan = kegiatanTemporary.getJenisKegiatan();

													kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan);
													if (kegiatan == null || kegiatan.getId() == null) {
														kegiatan = new Kegiatan();
													} else {
														kegiatan = (Kegiatan) sessionLocalKeg
																.createCriteria(Kegiatan.class)
																.add(Restrictions.idEq(kegiatan.getId()))
																.uniqueResult();
													}

													kegiatan.setJenisKegiatan(jenisKegiatan);
													kegiatan.setJadwalPembayaran(
															kegiatanTemporary.getJadwalPembayaran());
													kegiatan.setMahasiswa(mahasiswa);
													kegiatan.setSemster(smt);
													kegiatan.setStatusMahasiswa(kegiatanTemporary.getStatusMahasiswa());
													kegiatan.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
													kegiatan.setTanggal(tanggal);
													kegiatan.setValidated(1);
													kegiatan.setJenisKegiatan(jenisKegiatan);
													kegiatan.setValidator(bank);
													kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

													try {
														sessionLocalKeg.getTransaction().begin();
														Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
														sessionLocalKeg.getTransaction().commit();
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:439");
														// TODO: handle exception
													}

													List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
															.createCriteria(CicilanPembayaran.class).add(Restrictions
																	.eq("kegiatanTemporary", kegiatanTemporary))
															.list();
													for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
														try {
															cicilanPembayaran.setRef(ref);
															cicilanPembayaran.setKegiatan(kegiatan);
															cicilanPembayaran.setValidator(bank);
															cicilanPembayaran.setTanggal(tanggal);
															if (bankHost != null) {
																cicilanPembayaran.setJenisPembayaran(bankHost == null
																		|| bankHost.getJenisPembayaran() == null
																				? ConstantValues.TUNAI
																				: bankHost.getJenisPembayaran());
															}
															sessionLocalKeg.getTransaction().begin();
															Common.refreshSaveOrUpdate(sessionLocalKeg,
																	cicilanPembayaran);
															sessionLocalKeg.getTransaction().commit();

															JSONObject jsonObjectRinci = new JSONObject();
															jsonObjectRinci.put("nama", cicilanPembayaran
																	.getDetailBiaya().getItemBiaya().getNama());
															jsonObjectRinci.put("nominal",
																	cicilanPembayaran.getNilai());

															rincian.put(jsonObjectRinci);

														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:472");
															// TODO: handle exception
														}
													}

													try {
														if (kegiatanTemporary.getKegiatan() == null
																|| (kegiatanTemporary.getKegiatan() != null
																		&& !kegiatanTemporary.getKegiatan().getId()
																				.equals(kegiatan.getId()))) {
															sessionLocalKeg.getTransaction().begin();
															kegiatanTemporary.setKegiatan(kegiatan);
															Common.refreshSaveOrUpdate(sessionLocalKeg,
																	kegiatanTemporary);
															sessionLocalKeg.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:488");
														// TODO: handle exception
													}

													Number jumlah = (Number) sessionLocalKeg
															.createCriteria(CicilanPembayaran.class)
															.add(Restrictions.isNotNull("itemBiaya"))
															.add(Restrictions.eq("kegiatan", kegiatan))
															.setProjection(Projections.sum("nilai")).uniqueResult();
													nilaiBiayaHarusDiBayars = 0.0;
													Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();

													try {

														Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
														Collection<DetailBiaya> mydetailBiayas = PembayaranUtilHelper
																.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan,
																		false);
														for (Object o : mydetailBiayas) {
															if (o instanceof DetailBiaya) {
																DetailBiaya detailBiaya = (DetailBiaya) o;
																map.put(detailBiaya.getId(), detailBiaya);
															} else if (o instanceof PengaturanPembayaranBulanan) {
																PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
																DetailBiaya detailBiaya = pengaturanPembayaranBulanan
																		.getDetailBiaya();
																map.put(detailBiaya.getId(), detailBiaya);
															}
														}

														for (DetailBiaya detailBiaya : map.values()) {
															nilaiBiayaHarusDiBayars += Kegiatan
																	.ambilJumlahTagihan(kegiatan, detailBiaya);
														}

														kegiatan.setAmountTerhutang(
																nilaiBiayaHarusDiBayars - amountTotal);
														kegiatan.setAmount(amountTotal);
														sessionLocalKeg.getTransaction().begin();
														Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
														sessionLocalKeg.getTransaction().commit();
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
													}

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												} finally {
													Common.closeNativeSessionQuietly(sessionLocalKeg);
												}
											}
										}

										else if (idPemBul != null && idPemBul.startsWith("Item-")) {
											ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
													.simpleObject(
															session.createCriteria(ItemBiaya.class)
																	.add(Restrictions.idEq(
																			Long.parseLong(idPemBul.split("-")[1]))),
															ItemBiaya.class);

											if (itemBiaya != null) {
												String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
														+ virtualAccountBankNtt.getId();
												Double subtotal = 0.0;
												try {
													String[] spl = idPemBul.split("-");
													subtotal = Double.parseDouble(spl[2]);
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:557");
												}

												Long detailBiayaId = null;
												try {
													String[] spl = idPemBul.split("-");
													detailBiayaId = Long.parseLong(spl[4]);
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:564");
												}

												if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
													subtotal = 0.0 - subtotal;
												}

												CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
														.createCriteria(CicilanPembayaran.class)
														.add(Restrictions.eq("ref", ref)).setMaxResults(1)
														.uniqueResult();

												if (cicilanPembayaran == null) {
													cicilanPembayaran = new CicilanPembayaran(
															DetailBiaya.muatRefAman(session, detailBiayaId));
												}
												cicilanPembayaran.setDetailBiaya(
														DetailBiaya.muatRefAman(session, detailBiayaId));
												cicilanPembayaran.setRef(ref);
												cicilanPembayaran.setValidator(bank);
												cicilanPembayaran.setKegiatan(kegiatan);
												cicilanPembayaran.setItemBiaya(itemBiaya);
												cicilanPembayaran.setPengaturanPembayaranBulanan(null);
												cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

												cicilanPembayaran.setNilai(subtotal);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												cicilanPembayaran.setTanggal(tanggal);
												if (bankHost != null) {
													cicilanPembayaran.setJenisPembayaran(
															bankHost == null || bankHost.getJenisPembayaran() == null
																	? ConstantValues.TUNAI
																	: bankHost.getJenisPembayaran());
												}
												cicilanPembayaran.setDenda(0.0);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												session.getTransaction().begin();
												if (cicilanPembayaran.getId() == null)
													session.save(cicilanPembayaran);
												else
													Common.refreshUpdate(session, cicilanPembayaran);
												session.getTransaction().commit();

												JSONObject jsonObjectRinci = new JSONObject();
												jsonObjectRinci.put("nama", itemBiaya.getNama());
												jsonObjectRinci.put("bulan", "");
												jsonObjectRinci.put("nominal", subtotal);

												rincian.put(jsonObjectRinci);

											}
										}
									}
								}

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

								jsonObjectResponse = new JSONObject();
								jsonObjectResponse.put("response_code", "0000");
								jsonObjectResponse.put("response_message", "Success");
								body = jsonObjectResponse.toString();

								VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data, bank);
							}
						}
					}
				}

			}
		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/Maja.java:653");
			}
			jsonObjectResponse = new JSONObject();
			jsonObjectResponse.put("response_code", "0005");
			jsonObjectResponse.put("response_message", "Terjadi kesalahan internal");
			body = jsonObjectResponse.toString();
			h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			Common.closeNativeSessionQuietly(session);
			// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception, dan
			// TANPA syarat bankHost (request dari IP tak dikenal pun WAJIB tercatat).
			ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
					body, (double) nominalP, rincian.toString(), h2hStackTrace);
		}
		return jsonObjectResponse;
	}

	/**
	 * Membaca permintaan HTTP mentah, memprosesnya, dan menuliskan balasan JSON.
	 *
	 * <p>Langkah yang dijalankan berurutan:</p>
	 * <ol>
	 *   <li>badan permintaan dibaca baris demi baris menjadi satu string JSON &mdash; pemisah
	 *       baris dibuang sehingga payload multi-baris digabung rapat;</li>
	 *   <li>payload dan <i>query string</i> dicetak ke {@code System.out};</li>
	 *   <li>nomor VA diambil dari kunci {@code va}, dengan cadangan parameter permintaan bernama
	 *       sama; nominal dari kunci {@code amount}; tanggal dari kunci {@code date} dengan
	 *       cadangan serupa;</li>
	 *   <li>alamat IP pemanggil dipetakan menjadi {@link BankHost} dengan label {@code "Maja"};</li>
	 *   <li>{@link #doProcess} dipanggil dengan {@code chekLagi} bernilai {@code true} &mdash;
	 *       lihat catatan di sana mengenai pemeriksaan kedaluwarsa yang karenanya tidak pernah
	 *       berjalan;</li>
	 *   <li>hasil ditulis sebagai {@code application/json} disertai header {@code length} khusus
	 *       berisi panjang badan balasan.</li>
	 * </ol>
	 *
	 * <p><b>Keamanan:</b> tidak ada satu pun pemeriksaan kredensial di sini, dan hasil pemetaan
	 * {@link BankHost} tidak pernah diuji sebelum pemrosesan dilanjutkan. Pencetakan payload dan
	 * <i>query string</i> ke keluaran standar juga menyalin isi transaksi ke log server.</p>
	 *
	 * <p>Perhatikan bahwa {@code req.getInt("amount")} dipanggil tanpa penjagaan, sehingga
	 * payload tanpa kunci {@code amount} &mdash; atau badan permintaan kosong, yang membuat
	 * {@code req} bernilai {@code null} &mdash; menggagalkan pemrosesan sebelum sempat masuk ke
	 * {@link #doProcess}, sehingga tidak ada baris log H2H yang tercatat untuk permintaan
	 * seperti itu.</p>
	 *
	 * @param request  permintaan masuk dari mitra
	 * @param response balasan yang akan diisi JSON hasil
	 * @throws Exception bila pembacaan permintaan, penguraian JSON, atau penulisan balasan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request

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

		JSONObject req = data == null ? null : new JSONObject(data);

		String va = req == null || req.isNull("va") ? request.getParameter("va") : req.getString("va");
		String bank = "Maja";
		int nominalP = req.getInt("amount");
		String tanggalP = req == null || req.isNull("date") ? request.getParameter("date") : req.getString("date");

		// 05, request tidak diizinkan
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = Maja.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true).toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

}

