package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
 * Servlet gateway <b>host-to-host (H2H)</b> Bank BJB (Bank Pembangunan Daerah Jawa Barat dan
 * Banten) — pintu masuk callback bank untuk notifikasi pembayaran Virtual Account mahasiswa/calon
 * mahasiswa.
 *
 * <h3>Otentikasi H2H — FAKTA ARSITEKTUR YANG WAJIB DIPAHAMI</h3>
 * <p>Berbeda dengan gateway bergaya SNAP seperti {@link Briva} yang memverifikasi tanda tangan
 * RSA dan bearer token pada cabang transaksinya, kelas ini <b>sama sekali tidak memiliki
 * mekanisme verifikasi tanda tangan/HMAC/bearer token</b> — tidak ada cabang penerbitan token,
 * tidak ada pembacaan header {@code Authorization}, dan tidak ada satu pun kelas
 * {@code java.security.Signature}/{@code KeyFactory} yang dipakai di berkas ini. Satu-satunya
 * penyaring identitas pemanggil adalah pemetaan alamat IP ke {@link BankHost} lewat
 * {@link PembayaranUtil#getBankHost(String, String)}, dan hasilnya <b>boleh {@code null}</b> —
 * nilai itu diteruskan apa adanya ke {@link #doProcess} tanpa penjaga penolakan (tidak ada
 * padanan gerbang {@code gerbang_bankhost_null_posting_*} seperti yang sudah ditambahkan pada
 * {@link Briva} setelah audit token H2H-nya).</p>
 * <p>Lebih jauh, {@link VirtualAccountBank#ambilVa(String, Double, BankHost)} mencocokkan baris VA
 * dengan kriteria <code>bankHost IS NULL OR bankHost = :bankHostPemanggil</code> — klausa pertama
 * tidak bersyarat pada identitas pemanggil sama sekali. Akibatnya VA "netral" (kolom
 * {@code bankHost} kosong di basis data — kondisi wajar untuk VA yang belum pernah tersentuh bank
 * mana pun) dapat diposting oleh pemanggil <b>mana pun</b>, termasuk yang IP-nya sama sekali tidak
 * terdaftar di {@link BankHost} (sehingga {@code bankHost} pemanggil ikut bernilai {@code null}).
 * Gabungan kedua fakta ini berarti pengesahan pembayaran lewat endpoint ini bertumpu sepenuhnya
 * pada kerahasiaan nomor VA dan ketepatan nominal ({@code transaction_amount} harus sama persis
 * dengan {@code totalBiaya()}, lihat {@link #doProcess}) — untuk VA netral, tidak ada lapis
 * kriptografis maupun IP sama sekali yang menghalangi pemanggil tak dikenal. Ini bukan regresi
 * yang diperkenalkan Javadoc ini; ini adalah keadaan kode sejak awal, didokumentasikan di sini
 * agar tidak keliru dianggap sudah tertutup oleh pemetaan {@link BankHost}.</p>
 *
 * @see ais.action.ws.util.PembayaranGatewayHelper
 * @see VirtualAccountBank
 * @see BankHost
 * @see Briva
 */
public class Bjb extends HttpServlet {
	/**
	 * Versi serialisasi {@link java.io.Serializable} yang diwarisi dari {@link HttpServlet}.
	 *
	 * <p>Bernilai tetap {@code 1L}: servlet ini tidak pernah diserialisasi antarnode, sehingga nilai
	 * ini semata memenuhi kontrak {@code Serializable} dan meredam peringatan kompilator.</p>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran, dipakai {@link #process(HttpServletRequest, HttpServletResponse)}
	 * untuk memetakan alamat IP pemanggil menjadi {@link BankHost} lewat
	 * {@link PembayaranUtil#getBankHost(String, String)}, dan oleh {@link #doProcess doProcess} untuk
	 * menjumlahkan cicilan lewat {@code getTotalDanDendaFromCicilan}.
	 *
	 * <p><b>Perhatikan:</b> {@link BankHost} hasil pemetaan ini <i>tidak pernah dijadikan syarat
	 * penolakan</i> di kelas ini — nilai {@code null} tetap diteruskan ke {@link #doProcess} dan
	 * hanya memengaruhi jenis pembayaran (jatuh ke {@code ConstantValues.TUNAI} bila
	 * {@code bankHost} atau {@code bankHost.getJenisPembayaran()} kosong). Lihat Javadoc kelas untuk
	 * konsekuensi keamanannya.</p>
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Membentuk instance servlet.
	 *
	 * <p>Konstruktor tanpa argumen yang dibutuhkan wadah servlet: Tomcat membuat satu instance
	 * {@link Bjb} lalu memakainya kembali untuk kedua {@code url-pattern} yang dipetakan ke kelas
	 * ini. Tidak ada state yang disiapkan di sini — inisialisasi terjadi pada penginisialisasi
	 * field {@link #pembayaranUtil}.</p>
	 *
	 * <p>Karena instance dipakai bersama oleh banyak thread permintaan, jangan menambahkan field
	 * instance yang dapat berubah dan bergantung pada satu permintaan; pakai variabel lokal.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Bjb() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>GET dan POST diperlakukan identik — keduanya sekadar meneruskan ke {@link #process}. Jalur
	 * GET disediakan agar endpoint mudah diuji manual dan agar permintaan yang salah metode tetap
	 * menghasilkan balasan JSON, bukan halaman galat wadah servlet.</p>
	 *
	 * <p><b>Penanganan galat:</b> setiap {@link Exception} ditangkap dan diserahkan ke
	 * {@code Common.tampilErrorJikaAdmin}. Konsekuensinya, bila kegagalan terjadi sebelum badan
	 * respons sempat ditulis (mis. saat mem-parsing {@code transaction_amount} pada
	 * {@link #process}, lihat Javadoc-nya), bank dapat menerima badan kosong tanpa satu pun baris
	 * {@link ais.database.model.LogHostToHost} tercatat.</p>
	 *
	 * @param request  permintaan dari BJB
	 * @param response respons yang akan diisi JSON
	 * @throws ServletException bila wadah servlet melaporkan kegagalan
	 * @throws IOException      bila penulisan respons gagal
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
	 * Menangani permintaan HTTP POST dengan mendelegasikannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Inilah metode yang sesungguhnya dipakai BJB untuk mengirim notifikasi pembayaran Virtual
	 * Account. Isinya sama persis dengan {@link #doGet}.</p>
	 *
	 * <p><b>Penanganan galat:</b> sama dengan {@link #doGet} — pengecualian ditelan oleh
	 * {@code Common.tampilErrorJikaAdmin} sehingga tidak merambat ke wadah servlet.</p>
	 *
	 * @param request  permintaan dari BJB
	 * @param response respons yang akan diisi JSON
	 * @throws ServletException bila wadah servlet melaporkan kegagalan
	 * @throws IOException      bila penulisan respons gagal
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
	 * Memproses satu notifikasi pembayaran Virtual Account BJB: menyelesaikan baris VA, memverifikasi
	 * nominal, membentuk/memutakhirkan {@link Kegiatan}, membukukan cicilan per token, lalu menandai VA
	 * lunas.
	 *
	 * <p>VA dicari lewat {@link VirtualAccountBank#ambilVa(String, Double, BankHost)}. VA yang tidak
	 * ditemukan, atau totalnya tidak lebih dari 0,1, membuat method berakhir dengan respons default
	 * {@code response_code=0005} tanpa membukukan apa pun. VA yang sudah lunas dijawab dengan
	 * {@code response_code=0014}.</p>
	 *
	 * <h4>Verifikasi nominal</h4>
	 * <p>Satu-satunya pemeriksaan sebelum pembukuan adalah kecocokan persis antara {@code nominalP}
	 * (dibulatkan ke {@code int}) dan {@link VirtualAccountBank#totalBiaya()}; ketidakcocokan dijawab
	 * {@code response_code=0013} ("Invalid Amount") tanpa mengubah data apa pun. Ini <b>bukan</b>
	 * verifikasi tanda tangan/token &mdash; lihat peringatan pada Javadoc kelas.</p>
	 *
	 * <h4>Token cicilan</h4>
	 * <p>Daftar {@code cicilan} pada VA diurai per token yang dipisah koma: angka murni dan awalan
	 * {@code Bulanan-} menunjuk {@link PengaturanPembayaranBulanan}, awalan {@code Item-} menunjuk
	 * {@link ItemBiaya}, dan awalan {@code Keranjang-} diserahkan ke
	 * {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}. Tiap token menghasilkan satu
	 * {@link CicilanPembayaran} yang di-<i>upsert</i> lewat kolom {@code ref} (idempoten terhadap
	 * notifikasi berulang), sekaligus satu entri rincian pada larik JSON yang ikut disimpan ke log H2H.</p>
	 *
	 * <p>Setelah semua token selesai, total dan denda dihitung ulang lewat
	 * {@link PembayaranUtil#getTotalDanDendaFromCicilan}, {@link Kegiatan} disimpan, dan VA ditandai
	 * lunas lewat {@link VirtualAccountBank#updateVa}.</p>
	 *
	 * <p><b>Keamanan:</b> method ini menerima {@code bankHost} bernilai {@code null} dan tetap
	 * memproses &mdash; nilai itu hanya memengaruhi jenis pembayaran cicilan (jatuh ke
	 * {@code ConstantValues.TUNAI} bila kosong), bukan gerbang penolakan. Lihat peringatan pada Javadoc
	 * kelas untuk konsekuensi lengkapnya.</p>
	 *
	 * <p>Apa pun hasilnya, satu baris {@link ais.database.model.LogHostToHost} selalu ditulis di blok
	 * {@code finally} lewat {@code PembayaranGatewayHelper.catatLogHostToHost} &mdash; termasuk jejak
	 * <i>stack trace</i> bila terjadi galat.</p>
	 *
	 * @param nominalP  nominal setoran yang dilaporkan BJB
	 * @param tanggalP  tanggal transaksi dalam format {@code Common.databaseDateFormat1}; bila gagal
	 *                  diurai dipakai waktu server saat ini
	 * @param va        nomor VA yang dinotifikasikan
	 * @param bank      label bank yang disimpan sebagai {@code validator} pada data pembayaran (selalu
	 *                  {@code "BJB"} dari pemanggil {@link #process})
	 * @param bankHost  host bank hasil pemetaan IP; boleh {@code null} dan tidak menjadi gerbang
	 * @param request   permintaan asal, diteruskan ke pencatat log H2H
	 * @param data      payload mentah permintaan, disimpan apa adanya pada log H2H
	 * @param chekLagi  penanda warisan; saat ini tidak dipakai di badan method
	 * @return objek JSON balasan berisi {@code response_code} dan {@code response_message} BJB
	 * @throws Exception bila kegagalan terjadi di luar jangkauan penanganan internal
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi) throws Exception {
		JSONObject jsonObjectResponse = new JSONObject();
		jsonObjectResponse.put("response_code", "0005");
		jsonObjectResponse.put("response_message", "Error Other, Alamat IP Tidak terdaftar");
		String body = jsonObjectResponse.toString();

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			Session session = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

						if (VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt)) {
							jsonObjectResponse = new JSONObject();
							jsonObjectResponse.put("response_code", "0014");
							jsonObjectResponse.put("response_message", "Tagihan sudah terbayar");
							return jsonObjectResponse;
						}

					session = HibernateUtil.getSessionFactory().openSession();
					{

						Double nominal = nominalP;

						if (nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
							jsonObjectResponse = new JSONObject();
							jsonObjectResponse.put("response_code", "0013");
							jsonObjectResponse.put("response_message", "Invalid Amount");
							body = jsonObjectResponse.toString();
						} else {

							JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
							Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
							BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
									.getBiodataCalonMahasiswa();

							Integer semester = virtualAccountBankNtt.getSemester();

							nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
							nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

							Date tanggal = ais.ui.util.WaktuUtil.getDate();
							try {
								tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:137");
								// TODO: handle exception
							}

							Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
									: session.createCriteria(Kegiatan.class)
											.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
											.uniqueResult());

							if (kegiatan == null || kegiatan.getId() == null) {

								kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).addOrder(Order.asc("id"))

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
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:185");

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:226");
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

											JSONObject jsonObjectRinci = new JSONObject();
											jsonObjectRinci.put("nama", pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya().getNama());
											jsonObjectRinci.put("bulan", pengaturanPembayaranBulanan.getNamaBulan());
											jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
													.ambilNominalModifikasi(mahasiswa, semester));

											rincian.put(jsonObjectRinci);

										} else {

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:291");
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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:351");
											}

											Long detailBiayaId = null;
											try {
												String[] spl = idPemBul.split("-");
												detailBiayaId = Long.parseLong(spl[4]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:358");
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
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											JSONObject jsonObjectRinci = new JSONObject();
											jsonObjectRinci.put("nama", itemBiaya.getNama());
											jsonObjectRinci.put("bulan", "");
											jsonObjectRinci.put("nominal", subtotal);

											rincian.put(jsonObjectRinci);
										}
									} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
										// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
										// menjadi Kegiatan+Cicilan nyata - pemroses terpusat yang sama dengan Esmartlink.
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

							jsonObjectResponse = new JSONObject();
							jsonObjectResponse.put("response_code", "0000");
							jsonObjectResponse.put("response_message", "Success");
							body = jsonObjectResponse.toString();
						}

					}

				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
				jsonObjectResponse = new JSONObject();
				jsonObjectResponse.put("response_code", "0005");
				jsonObjectResponse.put("response_message", "Terjadi kesalahan internal");
				body = jsonObjectResponse.toString();
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:450");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:451");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bjb.java:452");}
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						body, nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return jsonObjectResponse;
	}

	/**
	 * Membaca notifikasi BJB dari badan permintaan, memetakan IP pemanggil ke {@link BankHost}, lalu
	 * memanggil {@link #doProcess} dan menuliskan balasan JSON.
	 *
	 * <p>Badan permintaan dibaca baris demi baris menjadi satu string JSON (pemisah baris dibuang).
	 * Nomor VA dan tanggal transaksi diambil dari badan JSON, dengan jatuh ke parameter query bila
	 * medan JSON kosong; nominal ({@code transaction_amount}) hanya diambil dari badan JSON &mdash;
	 * bila badan bukan JSON yang sah, pemanggilan {@code req.getDouble(...)} melempar
	 * {@link Exception} sebelum satu pun baris log H2H sempat ditulis (lihat catatan pada
	 * {@link #doGet}).</p>
	 *
	 * <p>Label bank selalu {@code "BJB"}. Balasan ditulis dengan header {@code Content-Type:
	 * application/json} dan header non-standar {@code length} berisi panjang badan.</p>
	 *
	 * @param request  permintaan dari BJB
	 * @param response respons yang akan diisi JSON hasil {@link #doProcess}
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

		String va = req == null || req.isNull("va_number") ? request.getParameter("va_number")
				: req.getString("va_number");
		String bank = "BJB";
		double nominalP = req.getDouble("transaction_amount");
		String tanggalP = req == null || req.isNull("transaction_date") ? request.getParameter("transaction_date")
				: req.getString("transaction_date");

		// 05, request tidak diizinkan
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = Bjb.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true).toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

}

