package ais.action.master.helper.virtualaccount;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

import ais.action.master.helper.util.MahasiswaSmartlinkChannelWindow;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.util.SmartlinkChannelWindow;
import ais.common.BJBUtil;
import ais.common.BRIDataUtil;
import ais.common.BSIMajaUtil;
import ais.common.Common;
import ais.common.OttoUtil;
import ais.common.OnlineBmtUtil;
import ais.common.URLBuilder;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper untuk membuat/memperbarui tagihan {@link VirtualAccountBank} bagi <b>calon mahasiswa</b>
 * (PMB, via {@link #sendRequest}) dan bagi <b>mahasiswa aktif</b> (KRS/pembayaran semester, via
 * {@link #downloadData}). Kedua method utamanya menghitung total tagihan dari kumpulan item biaya
 * yang dipilih, menentukan waktu kedaluwarsa tagihan (berdasarkan konfigurasi
 * {@code tagihan_expired_akhir_hari}/{@code tagihan_expired_jam}/{@code tagihan_expired_day}, atau
 * override eksplisit lewat parameter {@code waktuSampai}), lalu memanggil salah satu gateway
 * pembayaran eksternal sesuai kanal yang aktif — QRIS (Jaring), Finpay, Flip, Otto, BRIVA,
 * BankAltimtara, Maja (BSI), Jaring VA, Esmartlink, BJB langsung, atau VA generik bank host biasa —
 * dan menyimpan hasilnya (nomor VA/link pembayaran/payload request-response mentah) ke satu baris
 * {@link VirtualAccountBank} yang dikembalikan ke pemanggil.
 *
 * <p>
 * <b>Pola umum tiap cabang kanal</b>: susun payload JSON/form sesuai kontrak API gateway
 * bersangkutan, panggil {@code Common.executeHttp}/util spesifik kanal (mis. {@link BJBUtil},
 * {@link BRIDataUtil}, {@link BSIMajaUtil}, {@link OttoUtil}), lalu petakan respons ke
 * {@code kode}/{@code link}/{@code bank} pada entitas. Kegagalan HTTP/parsing pada satu kanal
 * ditangkap lokal, dilaporkan lewat {@code Common.tampilErrorJikaAdmin}, dan method mengembalikan
 * {@code null} (kanal Esmartlink secara khusus juga menambahkan pesan ke daftar {@code warnings}
 * bila diberikan). Baris {@link VirtualAccountBank} baru/yang diperbarui disimpan dalam transaksi
 * Hibernate tersendiri via {@link MahasiswaVirtualAccountHelper#openSession()}, dan session selalu
 * ditutup lewat {@link #closeSessionQuietly(Session)} di blok {@code finally} — penting karena
 * method ini dipanggil dari Timer ZK sehingga tidak boleh bergantung pada session bersama yang
 * mungkin sudah ditutup oleh helper lain.
 * </p>
 *
 * <p>
 * <b>Sebelum membuat VA baru</b>, kedua method mencari dahulu VA aktif (belum kedaluwarsa, belum
 * bermasalah) dengan kombinasi cicilan/keterangan/mahasiswa yang identik agar tidak menerbitkan VA
 * ganda untuk tagihan yang sama; VA lama dipakai ulang kecuali {@code update=true} diminta eksplisit
 * atau kanal e-smartlink berubah.
 * </p>
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-02)</b> — beberapa kanal sebelumnya memakai
 * kredensial/kunci rahasia yang tertanam langsung sebagai nilai default
 * {@code Common.getKonfigurasi(key, defaultValue)} di kode ini (dipakai bila admin belum mengisi
 * konfigurasi terkait di database): kredensial Esmartlink ({@code username_va_e_smartlink}/
 * {@code password_va_e_smartlink}, muncul di 2 titik), signature key + app id BankAltimtara
 * ({@code key_bankaltimtara_baru}/{@code app_id_bankaltimtara_baru}), secret key Basic-Auth
 * gateway VA Jaring ({@code va_jaring_screet_key}, base64, mendekode menjadi
 * {@code "jaring:jaring"}), dan secret key Basic-Auth gateway QRIS Jaring
 * ({@code qris_jaring_screet_key}, base64, mendekode menjadi {@code "bsn:bsn"}). Seluruh default
 * itu sudah dihapus (kini string kosong).
 * </p>
 *
 * <p>
 * <b>TINDAK LANJUT DI LUAR PERUBAHAN KODE INI</b>: seluruh nilai yang sebelumnya tertanam sudah
 * lama berada di riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi di sisi eSmartlink,
 * BankAltimtara, dan Jaring bila masih aktif di produksi.
 * </p>
 */
public class DownloadTagihanMahasiswaBankOnline {

	/**
	 * Varian ringkas {@link #sendRequest(Mahasiswa, BiodataCalonMahasiswa, Set, Double,
	 * PerguruanTinggi, BankHost, Map, String)} tanpa parameter {@code param}/{@code waktuSampai}
	 * (memakai {@code param} kosong dan waktu kedaluwarsa default sesuai konfigurasi).
	 */
	@SuppressWarnings("rawtypes")
	public static VirtualAccountBank sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			final Set<KegiatanTemporary> selectedKegiatanTemporary, Double biayaAdministrasi,
			PerguruanTinggi perguruanTinggi, BankHost bankHost) throws Exception {
		Map param = new HashMap();
		String waktuSampai = null;
		return sendRequest(mahasiswa, biodataCalonMahasiswa, selectedKegiatanTemporary, biayaAdministrasi,
				perguruanTinggi, bankHost, param, waktuSampai);
	}

	/**
	 * Membuat/mengambil-ulang tagihan {@link VirtualAccountBank} untuk pembayaran biaya PMB
	 * (Penerimaan Mahasiswa Baru) satu {@link BiodataCalonMahasiswa} (atau {@link Mahasiswa} bila
	 * sudah menjadi mahasiswa aktif) berdasarkan kumpulan {@link KegiatanTemporary} (item keranjang
	 * biaya) yang dipilih. Menghitung total dari seluruh item, menyusun deskripsi tagihan, menentukan
	 * waktu kedaluwarsa (konfigurasi atau {@code waktuSampai} eksplisit), lalu — bila kanal
	 * e-smartlink aktif — menyusun payload dan memanggil gateway Esmartlink; bila tidak, menerbitkan
	 * kode VA generik dengan prefiks {@code prefix_va_bank_online}. Baris VA lama yang masih berlaku
	 * dan cocok kriterianya dipakai ulang, bukan diduplikasi.
	 *
	 * @param mahasiswa                  mahasiswa terkait (bisa {@code null} bila tagihan untuk calon
	 *                                    mahasiswa murni)
	 * @param biodataCalonMahasiswa      calon mahasiswa terkait (bisa {@code null} bila mahasiswa
	 *                                    sudah aktif)
	 * @param selectedKegiatanTemporary  kumpulan item biaya (keranjang) yang akan ditagihkan; kosong
	 *                                    atau {@code null} membuat method mengembalikan {@code null}
	 * @param biayaAdmin                 biaya admin tambahan yang ditambahkan ke total tagihan
	 * @param perguruanTinggi            perguruan tinggi pemilik data VA baru (dipakai saat membuat
	 *                                    entitas {@link VirtualAccountBank} baru)
	 * @param bankHost                   host bank tujuan VA, boleh {@code null} untuk VA tanpa host
	 *                                    spesifik
	 * @param param                      opsi tambahan: {@code esmartlinkBayarVia}, {@code update}
	 *                                    (paksa terbitkan ulang), {@code warnings} (List untuk
	 *                                    menampung pesan galat kanal), {@code smartlink},
	 *                                    {@code smartlink_direct}, {@code payment_url}
	 * @param waktuSampai                override waktu kedaluwarsa relatif (lihat konstanta
	 *                                    {@code SmartlinkChannelWindow.WAKTU_*}), {@code null} untuk
	 *                                    memakai aturan konfigurasi default
	 * @return baris {@link VirtualAccountBank} yang tersimpan, atau {@code null} bila tidak ada item
	 *         yang dipilih, gateway gagal, atau proses dialihkan ke dialog pemilihan channel
	 * @throws Exception diteruskan dari kegagalan Hibernate/HTTP yang tidak tertangani secara lokal
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static VirtualAccountBank sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			final Set<KegiatanTemporary> selectedKegiatanTemporary, Double biayaAdmin, PerguruanTinggi perguruanTinggi,
			BankHost bankHost, Map param, String waktuSampai) throws Exception {

		String esmartlinkBayarVia = (String) (param.get("esmartlinkBayarVia") == null ? null
				: param.get("esmartlinkBayarVia"));
		boolean update = (Boolean) (param.get("update") == null ? false : param.get("update"));
		List<String> warnings = (param.get("warnings") == null ? null : (List<String>) param.get("warnings"));
		Boolean smartlink = (Boolean) (param.get("smartlink") == null ? false : param.get("smartlink"));
		Boolean onlineBmt = Boolean.TRUE.equals(param.get(OnlineBmtUtil.PARAM_KEY));

		VirtualAccountBank virtualAccountBankOnline = null;
		Session session = null;
		try {
			if (selectedKegiatanTemporary == null || selectedKegiatanTemporary.isEmpty()) {
				return null;
			}

			StringBuilder cicilanBuilder = new StringBuilder();
			StringBuilder pembBuilder = new StringBuilder();
			Double total = 0.0;
			StringBuilder detailbiayaBuilder = new StringBuilder();
			for (KegiatanTemporary kegiatanTemporary : selectedKegiatanTemporary) {
				if (kegiatanTemporary == null || kegiatanTemporary.getId() == null) {
					continue;
				}
				Double nilai = kegiatanTemporary.getAmount();
				nilai = nilai == null ? 0.0 : nilai;
				total += nilai;
				if (cicilanBuilder.length() > 0) {
					cicilanBuilder.append(",");
				}
				cicilanBuilder.append("Keranjang-").append(kegiatanTemporary.getId()).append("-").append(nilai);

				String desc = kegiatanTemporary.getKeterangan();
				desc = (desc == null || desc.isEmpty() ? (kegiatanTemporary.getJenisKegiatan() == null ? "Tagihan"
						: kegiatanTemporary.getJenisKegiatan().getNama()) : desc) + ", Rp. "
						+ Common.numberFormat.get().format(nilai);
				pembBuilder
						.append(kegiatanTemporary.getJenisKegiatan() == null ? ""
								: kegiatanTemporary.getJenisKegiatan().getKode().trim())
						.append(",").append(desc).append(";");

				if (detailbiayaBuilder.length() > 0) {
					detailbiayaBuilder.append(",");
				}
				detailbiayaBuilder.append(kegiatanTemporary.getId());

				if (mahasiswa != null && kegiatanTemporary.getMahasiswa() != null
						&& !mahasiswa.getId().equals(kegiatanTemporary.getMahasiswa().getId())) {
					return null;
				}

				if (biodataCalonMahasiswa != null && kegiatanTemporary.getCalonMahasiswa() != null
						&& !biodataCalonMahasiswa.getId().equals(kegiatanTemporary.getCalonMahasiswa().getId())) {
					return null;
				}

				mahasiswa = kegiatanTemporary.getMahasiswa();
				biodataCalonMahasiswa = kegiatanTemporary.getCalonMahasiswa();
			}
			String cicilan = cicilanBuilder.toString();
			String pemb = pembBuilder.toString();
			String detailbiaya = detailbiayaBuilder.toString();
			if (cicilan.length() == 0 || total.doubleValue() < 0.01) {
				return null;
			}

			Calendar calendar6 = Calendar.getInstance();
			calendar6.set(Calendar.DATE, calendar6.get(Calendar.DATE) + 1);

			boolean tagihan_expired_akhir_hari = Common
					.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF);
			Date expired_date = null;
			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e,
							"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
				}
			} else {
				String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
				if (!tagihan_expired_jam.isEmpty()) {
					if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.HOUR_OF_DAY,
									calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
							expired_date = calendar.getTime();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						}
					}
				} else {
					String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();

					if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE,
									calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
							expired_date = calendar.getTime();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						}
					}
				}
			}

			if (waktuSampai != null) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_15_MENIT)) {
						calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 15);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_30_MENIT)) {
						calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 30);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_JAM)) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 1);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_3_JAM)) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 3);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_6_JAM)) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 6);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_24_JAM)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_3_HARI)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 3);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_MINGGU)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_BULAN)) {
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
					}
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:203");
				}
			}

			session = MahasiswaVirtualAccountHelper.openSession();
			virtualAccountBankOnline = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.eq("terjadiKendala", false))
					.add(bankHost == null || bankHost.getId() == null ? Restrictions.isNull("bankHost")
							: Restrictions.eq("bankHost.id", bankHost.getId()))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("cicilan", cicilan))
					.add(Restrictions.or(
							mahasiswa == null || mahasiswa.getId() == null ? Restrictions.sqlRestriction("false")
									: Restrictions.eq("mahasiswa.id", mahasiswa.getId()),
							biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null
									? Restrictions.sqlRestriction("false")
									: Restrictions.eq("biodataCalonMahasiswa.id", biodataCalonMahasiswa.getId())))
					.add(Restrictions.eq("semester", 0)).add(Restrictions.isNull("kegiatan")).setMaxResults(1)
					.addOrder(Order.desc("id")).uniqueResult();

			if (virtualAccountBankOnline != null && virtualAccountBankOnline.getChannel() != null
					&& esmartlinkBayarVia != null
					&& esmartlinkBayarVia.equalsIgnoreCase(virtualAccountBankOnline.getChannel())) {
				update = false;
			}

			if (virtualAccountBankOnline == null || update) {

				if (virtualAccountBankOnline == null) {
					virtualAccountBankOnline = new VirtualAccountBank(perguruanTinggi.getId());
				} else if (update && virtualAccountBankOnline != null) {
					virtualAccountBankOnline.setVa(null);
					virtualAccountBankOnline.setLink(null);
					virtualAccountBankOnline.setResponse(null);
				}

				int jml_digit_prefix_va_bank_online = 10;
				try {
					String nilaiDigitPrefix = Common
							.getKonfigurasi("jml_digit_prefix_va_bank_online", jml_digit_prefix_va_bank_online + "")
							.getNilai();
					if (nilaiDigitPrefix != null && nilaiDigitPrefix.trim().length() > 0) {
						jml_digit_prefix_va_bank_online = Integer.parseInt(nilaiDigitPrefix.trim());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:244");
					// TODO: handle exception
				}

				String subva = Common.bolehKonfigurasi("subva_paka_nim", Konfigurasi.TIDAK_AKTIF) ? mahasiswa.getNim()
								: Common.getGeneratedAngkaDigit(jml_digit_prefix_va_bank_online);

				if (onlineBmt) {
					if (!OnlineBmtUtil.isPerguruanTinggiReady(perguruanTinggi.getId())) return null;
					OnlineBmtUtil.prepareInvoice(virtualAccountBankOnline);
				} else if (Common.bolehKonfigurasi("aktifkan_va_e_smartlink", Konfigurasi.TIDAK_AKTIF) || smartlink) {
					virtualAccountBankOnline.setLink("");
					String variableSmartlink = Common.getKonfigurasi("channel_biaya_e_smartlink",
							"VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart")
							.getNilai();

					if (virtualAccountBankOnline != null && virtualAccountBankOnline.getKanalPembayaran() != null) {
						variableSmartlink = virtualAccountBankOnline.getKanalPembayaran()
								.getVariableBiayaAdminEsmartlink();
					}

					System.out.println("esmartlinkBayarVia -> " + esmartlinkBayarVia);

					if (!variableSmartlink.isEmpty() && esmartlinkBayarVia == null) {

						if (param.get("smartlink_direct") != null && ((Boolean) param.get("smartlink_direct"))) {

							String va = Common.getGeneratedBarCode();
							virtualAccountBankOnline.setLink(param.get("payment_url") + "");
							virtualAccountBankOnline.setKode(va);
							virtualAccountBankOnline.setBank("Esmartlink");

						} else {

							MyWindow window = new MyWindow("Pilih Channel Pembayaran", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							MahasiswaSmartlinkChannelWindow.initBanyak(window, mahasiswa, biodataCalonMahasiswa,
									selectedKegiatanTemporary, param, biayaAdmin, bankHost, pemb, cicilan, total, true);
							window.setHeight("90%");
							window.setWidth("600px");
							window.setVisible(true);
							window.onModal();

							return null;
						}

					} else {

						String hasil = "";
						try {
							if (expired_date == null) {
								try {
									Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
									calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
									expired_date = calendar.getTime();
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e,
											"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n",
											true);
								}
							}

							String va = Common.getGeneratedBarCode(30);
							int mytotal = total.intValue() + biayaAdmin.intValue();
							JSONObject postData = new JSONObject();
							postData.put("order_id", va);
							postData.put("amount", mytotal);
							postData.put("description", pemb);
							JSONObject customer = new JSONObject();

							if (biodataCalonMahasiswa != null) {
								customer.put("name", biodataCalonMahasiswa.getNama() == null ? ""
										: biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", ""));
								customer.put("email", biodataCalonMahasiswa.getEmail().split(",")[0].split(";")[0]);
								customer.put("phone", biodataCalonMahasiswa.getHp());

								String sender_email = perguruanTinggi.getEmail();
								if (biodataCalonMahasiswa != null && !biodataCalonMahasiswa.getEmail().isEmpty()) {
									sender_email = biodataCalonMahasiswa.getEmail().split(",")[0];
								}

								String sender_phone_number = perguruanTinggi.getTelepon();
								if (biodataCalonMahasiswa != null && !biodataCalonMahasiswa.getHp().isEmpty()
										&& biodataCalonMahasiswa.getHp().length() > 8
										&& biodataCalonMahasiswa.getHp().length() < 15) {
									sender_phone_number = biodataCalonMahasiswa.getHp();
								}

								customer.put("email", sender_email);
								customer.put("phone", sender_phone_number);

							} else {

								customer.put("name", mahasiswa.getNama() == null ? ""
										: mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", ""));

								String sender_email = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
										.getEmail();
								if (mahasiswa != null && !mahasiswa.getEmail().isEmpty()) {
									sender_email = mahasiswa.getEmail().split(",")[0];
								}

								String sender_phone_number = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
										.getTelepon();
								if (mahasiswa != null && !mahasiswa.getTelp().isEmpty()
										&& mahasiswa.getTelp().length() > 8 && mahasiswa.getTelp().length() < 15) {
									sender_phone_number = mahasiswa.getTelp();
								}

								customer.put("email", sender_email);
								customer.put("phone", sender_phone_number);
							}

							postData.put("customer", customer);

							JSONArray itemsSmartlink = new JSONArray();

							for (KegiatanTemporary kegiatanTemporary : selectedKegiatanTemporary) {
								Double nilai = kegiatanTemporary.getAmount();

								String desc = kegiatanTemporary.getKeterangan();
								desc = (desc.isEmpty() ? (kegiatanTemporary.getJenisKegiatan().getNama()) : desc)
										+ ", Rp. " + Common.numberFormat.get().format(nilai);

								if (desc.length() > 255) {
									desc = desc.substring(0, 255);
								}

								if (nilai > 0.01) {
									JSONObject jsonObject = new JSONObject();
									jsonObject.put("name", desc);
									jsonObject.put("amount", nilai.intValue());
									jsonObject.put("qty", 1);
									itemsSmartlink.put(jsonObject);
								}
							}

							if (biayaAdmin != null && biayaAdmin.intValue() > 0) {

								JSONObject jsonObject = new JSONObject();
								jsonObject.put("name", "Biaya Admin");
								jsonObject.put("amount", biayaAdmin.intValue());
								jsonObject.put("qty", 1);

								itemsSmartlink.put(jsonObject);
							}

							postData.put("item", itemsSmartlink);
							if (esmartlinkBayarVia == null) {
								String cannel_va_e_smartlink = Common
										.getKonfigurasi("cannel_va_e_smartlink", "VA_CIMB,VA_BRI").getNilai();
								String[] ch = cannel_va_e_smartlink.split(",");
								JSONArray channel = new JSONArray();
								for (int i = 0; i < ch.length; i++) {
									if (!ch[i].trim().isEmpty()) {
										channel.put(ch[i].trim());
									}
								}
								postData.put("channel", channel);
							} else {
								JSONArray channel = new JSONArray();
								channel.put(esmartlinkBayarVia);
								postData.put("channel", channel);
							}

							String link = Common.getRequestHostWithProtocol();
							if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
								link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol())
										.getNilai();
							}

							postData.put("type", "payment-page");
							postData.put("payment_mode", "CLOSE");
							postData.put("expired_time", Common.iso8601.get().format(expired_date));
							postData.put("callback_url", link + "/Esmartlink");
							postData.put("success_redirect_url", link + "/PembayaranSukses");
							postData.put("failed_redirect_url", link + "/PembayaranGagal");

							String strURL = Common
									.getKonfigurasi("gateway_url_va_e_smartlink",
											"https://payment-service-sbx.pakar-digital.com/api/payment/create-order")
									.getNilai();

							String username_va_e_smartlink = Common
									.getKonfigurasi("username_va_e_smartlink", "")
									.getNilai().trim();
							String password_va_e_smartlink = Common
									.getKonfigurasi("password_va_e_smartlink", "").getNilai().trim();
							if (virtualAccountBankOnline != null
									&& virtualAccountBankOnline.getKanalPembayaran() != null) {
								username_va_e_smartlink = virtualAccountBankOnline.getKanalPembayaran()
										.getUsernameEsmartlink();
								password_va_e_smartlink = virtualAccountBankOnline.getKanalPembayaran()
										.getPasswordEsmartlink();
							}
							hasil = VirtualAccountBank.curlSmartlink(strURL, username_va_e_smartlink,
									password_va_e_smartlink, postData);

							JSONObject jSONObject = new JSONObject(hasil);

							if (!(jSONObject.get("code") + "").equals("0")) {
								try {
									if (warnings != null) {
										warnings.add(jSONObject.getString("message"));
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:446");
									// TODO: handle exception
								}
								return null;
							}

							JSONObject data = jSONObject.getJSONObject("data");

							virtualAccountBankOnline.setRequest(postData.toString());
							virtualAccountBankOnline.setResponse(jSONObject.toString());
							virtualAccountBankOnline.setLink(data.get("payment_url") + "");
							virtualAccountBankOnline.setKode(va);
							virtualAccountBankOnline.setBank("Esmartlink");
						} catch (Exception e) {
							if (warnings != null) {
								warnings.add(e.getMessage());
							}
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
							return null;
						}
					}
				}

				else {

					String va = Common.getKonfigurasi("prefix_va_bank_online", "").getNilai() + subva;
					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setBank("Bank Online");
				}

				virtualAccountBankOnline.setKadaluarsa(expired_date);
				virtualAccountBankOnline.setOtomatis(false);

				virtualAccountBankOnline.setCicilan(cicilan);
				virtualAccountBankOnline.setJenisKegiatan(null);
				virtualAccountBankOnline.setKeterangan(pemb);
				virtualAccountBankOnline.setTotal(total);
				virtualAccountBankOnline.setBulanan("");
				virtualAccountBankOnline.setDetailbiaya(detailbiaya);
				virtualAccountBankOnline.setBiayaAdmin(biayaAdmin);
				virtualAccountBankOnline.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				virtualAccountBankOnline.setMahasiswa(mahasiswa);
				virtualAccountBankOnline.setJadwalPembayaran(null);
				virtualAccountBankOnline.setSemester(0);
				virtualAccountBankOnline.setTahunAkademik(null);
				virtualAccountBankOnline.setBankHost(bankHost);

				Transaction transaction = null;
				try {
					if (session == null || !session.isOpen()) {
						closeSessionQuietly(session);
						session = MahasiswaVirtualAccountHelper.openSession();
					}
					transaction = session.beginTransaction();
					Common.refreshSaveOrUpdate(session, virtualAccountBankOnline);
					transaction.commit();
				} catch (Exception txe) {
					rollbackQuietly(transaction);
					throw txe;
				}

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e, "Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n",
					true);
		} finally {
			closeSessionQuietly(session);
		}
		return virtualAccountBankOnline;
	}

	/**
	 * Varian ringkas {@link #downloadData(Mahasiswa, Integer, JadwalPembayaran, Collection, Grid,
	 * Map, Double, Double, Double, BankHost, String)} tanpa {@code waktuSampai} (memakai waktu
	 * kedaluwarsa default sesuai konfigurasi/jadwal pembayaran).
	 */
	@SuppressWarnings({ "rawtypes" })
	public static VirtualAccountBank downloadData(Mahasiswa mahasiswa, Integer smt, JadwalPembayaran myjadwalPembayaran,
			Collection detailBiayas, Grid gridCicilan, Map param, Double biayaAdmin, Double tabungan, Double topup,
			BankHost bankHost) throws Exception {
		String waktuSampai = null;
		return downloadData(mahasiswa, smt, myjadwalPembayaran, detailBiayas, gridCicilan, param, biayaAdmin, tabungan,
				topup, bankHost, waktuSampai);
	}

	/**
	 * Method utama pembuatan tagihan {@link VirtualAccountBank} untuk pembayaran semester/KRS
	 * mahasiswa aktif. Menggabungkan biaya dari {@code detailBiayas} (item biaya reguler) dan baris
	 * cicilan yang dicentang di {@code gridCicilan} (pembayaran bulanan/per-item), lalu memilih salah
	 * satu kanal pembayaran berdasarkan flag boolean di {@code param} ({@code qris}, {@code finpay},
	 * {@code otto}, {@code briva}, {@code flip}, {@code maja}, {@code smartlink}) atau konfigurasi
	 * global kanal aktif ({@code aktifkan_va_bankaltimtara_baru}, {@code aktifkan_va_maja},
	 * {@code aktifkan_va_jaring}, {@code aktifkan_va_e_smartlink}, {@code aktifkan_va_bjb_langsung}),
	 * dengan VA bank host generik sebagai fallback bila tidak ada kanal khusus yang aktif.
	 *
	 * <p>
	 * Sebelum membuat entitas baru, method memvalidasi lewat
	 * {@link MahasiswaVirtualAccountHelper#pastikanTagihanBelumDibayar} bahwa tagihan dengan
	 * kombinasi kunci yang sama belum lunas (melempar
	 * {@link MahasiswaVirtualAccountHelper.TagihanSudahDibayarException} bila sudah), dan mencari VA
	 * aktif yang cocok untuk dipakai ulang (memperhitungkan {@code topup} bila diisi).
	 * </p>
	 *
	 * @param mahasiswa           mahasiswa yang ditagih
	 * @param smt                 nomor semester tagihan
	 * @param myjadwalPembayaran  jadwal pembayaran terkait (menentukan jenis kegiatan, batas waktu,
	 *                            khusus-NIM), boleh {@code null}
	 * @param detailBiayas        koleksi {@link DetailBiaya} yang membentuk total tagihan
	 * @param gridCicilan         grid ZK berisi baris cicilan/pembayaran bulanan yang dicentang user;
	 *                            boleh {@code null} bila tidak ada opsi cicilan
	 * @param param               flag kanal ({@code qris}/{@code finpay}/{@code otto}/{@code briva}/
	 *                            {@code flip}/{@code maja}/{@code smartlink}/{@code update}) serta opsi
	 *                            lain ({@code warnings}, {@code esmartlinkBayarVia}, {@code items},
	 *                            {@code tahunAkademik}, {@code ket}, {@code pemb}, {@code cicilan},
	 *                            {@code total})
	 * @param biayaAdmin          biaya admin tambahan; disisipkan sebagai item "Biaya Admin" terpisah
	 *                            ke {@code items} bila lebih dari nol
	 * @param tabungan            nilai tabungan yang dipakai untuk mengurangi tagihan, disimpan pada
	 *                            entitas VA
	 * @param topup               nominal top-up (mis. untuk kanal yang mendukung saldo), turut
	 *                            menentukan pencarian VA lama yang cocok
	 * @param bankHost            host bank tujuan VA
	 * @param waktuSampai         override waktu kedaluwarsa relatif, {@code null} untuk memakai aturan
	 *                            default (akhir hari/jam/hari sesuai konfigurasi, atau tanggal akhir
	 *                            {@code myjadwalPembayaran})
	 * @return baris {@link VirtualAccountBank} yang tersimpan, atau {@code null} bila gateway gagal
	 *         atau proses dialihkan ke dialog pemilihan channel e-smartlink
	 * @throws Exception termasuk {@link MahasiswaVirtualAccountHelper.TagihanSudahDibayarException}
	 *                    bila tagihan dengan kunci yang sama sudah lunas
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static VirtualAccountBank downloadData(Mahasiswa mahasiswa, Integer smt, JadwalPembayaran myjadwalPembayaran,
			Collection detailBiayas, Grid gridCicilan, Map param, Double biayaAdmin, Double tabungan, Double topup,
			BankHost bankHost, String waktuSampai) throws Exception {

		Session session = null;
		try {
			System.out.println("param -> " + param);

			// Safe boolean parsing to avoid NullPointerException or ClassCastException
			Boolean qris = Boolean.TRUE.equals(param.get("qris"));
			Boolean finpay = Boolean.TRUE.equals(param.get("finpay"));
			Boolean otto = Boolean.TRUE.equals(param.get("otto"));
			Boolean briva = Boolean.TRUE.equals(param.get("briva"));
			Boolean flip = Boolean.TRUE.equals(param.get("flip"));
			Boolean maja = Boolean.TRUE.equals(param.get("maja"));
			Boolean smartlink = Boolean.TRUE.equals(param.get("smartlink"));
			Boolean onlineBmt = Boolean.TRUE.equals(param.get(OnlineBmtUtil.PARAM_KEY));
			boolean update = Boolean.TRUE.equals(param.get("update"));

			List<String> warnings = (param.get("warnings") == null ? null : (List<String>) param.get("warnings"));
			String esmartlinkBayarVia = (String) param.get("esmartlinkBayarVia");
			String tahunAkademik = param.get("tahunAkademik") == null ? Common.getCurrentTahunAkademik()
					: String.valueOf(param.get("tahunAkademik"));

			// Use StringBuilder for memory efficiency in loops
			StringBuilder detailbiaya = new StringBuilder();
			for (Object o : detailBiayas) {
				if (o instanceof DetailBiaya) {
					DetailBiaya biaya = (DetailBiaya) o;
					if (detailbiaya.length() > 0)
						detailbiaya.append(",");
					detailbiaya.append(biaya.getId());
				}
			}

			StringBuilder ket = new StringBuilder(param.get("ket") == null ? "" : String.valueOf(param.get("ket")));
			StringBuilder pemb = new StringBuilder(param.get("pemb") == null ? "" : String.valueOf(param.get("pemb")));
			StringBuilder cicilan = new StringBuilder(
					param.get("cicilan") == null ? "" : String.valueOf(param.get("cicilan")));
			Double total = param.get("total") == null ? 0.0 : (Double) param.get("total");

			JadwalPembayaran jdw = (myjadwalPembayaran != null && myjadwalPembayaran.getKhususUntukNim() != null
					&& myjadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ","))
							? myjadwalPembayaran
							: null;

			JSONArray items = (param.get("items") == null ? new JSONArray() : (JSONArray) param.get("items"));

			// Hapus "Biaya Admin" lama (mungkin dari channel sebelumnya) lalu masukkan yang baru.
			// Ini mencegah duplikasi DAN nilai basi saat user ganti channel (mis. BCA→BNI).
			JSONArray itemsBersih = new JSONArray();
			for (int i = 0; i < items.length(); i++) {
				try {
					if (!"Biaya Admin".equals(items.getJSONObject(i).optString("description"))) {
						itemsBersih.put(items.getJSONObject(i));
					}
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:584");
				}
			}
			items = itemsBersih;
			if (biayaAdmin.intValue() > 0) {
				JSONObject jsonObjectitems = new JSONObject();
				jsonObjectitems.put("description", "Biaya Admin");
				jsonObjectitems.put("unitPrice", biayaAdmin.intValue());
				jsonObjectitems.put("qty", 1);
				jsonObjectitems.put("amount", biayaAdmin.intValue());
				items.put(jsonObjectitems);
			}

			StringBuilder keteranganSimpleBanget = new StringBuilder();

			if (gridCicilan != null) {
				List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
				for (Row row : mycicilanrows) {
					MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

					if (jumlahCicilan != null && jumlahCicilan.getValue() != null
							&& jumlahCicilan.getValue().intValue() != 0) {
						CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
								.getAttribute("cicilanPembayaran");

						if (cicilanPembayaranSebelumnya.getId() == null) {
							try {
								PengaturanPembayaranBulanan biaya = cicilanPembayaranSebelumnya
										.getPengaturanPembayaranBulanan();
								Double nilai = jumlahCicilan.getValue();

								if (biaya != null) {
									String descSimpleBanget = String.valueOf(biaya.getId());
									if (keteranganSimpleBanget.length() > 0)
										keteranganSimpleBanget.append(";");
									keteranganSimpleBanget.append(descSimpleBanget);

									MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan,
											"Bulanan-" + biaya.getId() + "-" + nilai);

									Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw,
											myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());

									String desc = biaya.getKeterangan();
									desc = (desc.isEmpty() ? (biaya.getDetailBiaya().getItemBiaya().getNama()) : desc)
											+ ", Rp. " + Common.numberFormat.get().format(nilai)
											+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

									if (ket.length() > 0)
										ket.append(",");
									ket.append(biaya.getDetailBiaya().getItemBiaya().getNama());

									pemb.append(biaya.getDetailBiaya().getItemBiaya().getKode().trim()).append(",")
											.append(desc).append(";");
									total += nilai;

									JSONObject jsonObjectitems = new JSONObject();
									jsonObjectitems.put("description", desc);
									jsonObjectitems.put("unitPrice", nilai.intValue());
									jsonObjectitems.put("qty", 1);
									jsonObjectitems.put("amount", nilai.intValue());
									items.put(jsonObjectitems);

								} else {
									Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
									ItemBiaya itemBiaya;
									DetailBiaya detailBiaya = (DetailBiaya) (myItemBiaya == null
											|| myItemBiaya.getSelectedItem() == null ? null
													: myItemBiaya.getSelectedItem().getValue());

									if (row.getAttribute("detailBiaya") != null) {
										detailBiaya = (DetailBiaya) row.getAttribute("detailBiaya");
										itemBiaya = detailBiaya.getItemBiaya();
									} else if (cicilanPembayaranSebelumnya != null
											&& cicilanPembayaranSebelumnya.getItemBiaya() != null
											&& cicilanPembayaranSebelumnya.getItemBiaya().getId() != null) {
										itemBiaya = cicilanPembayaranSebelumnya.getItemBiaya();
									} else {
										itemBiaya = detailBiaya.getItemBiaya();
									}

									String descSimpleBanget = String.valueOf(itemBiaya.getId());
									if (keteranganSimpleBanget.length() > 0)
										keteranganSimpleBanget.append(";");
									keteranganSimpleBanget.append(descSimpleBanget);

									MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan,
											"Item-" + itemBiaya.getId() + "-" + nilai + "-" + detailBiaya.getBayarKe()
													+ "-" + detailBiaya.getId());

									String desc = itemBiaya.getNama() + ", Rp. "
											+ Common.numberFormat.get().format(nilai);

									if (ket.length() > 0)
										ket.append(",");
									ket.append(itemBiaya.getNama());

									pemb.append(itemBiaya.getKode().trim()).append(",").append(desc).append(";");
									total += nilai;

									JSONObject jsonObjectitems = new JSONObject();
									jsonObjectitems.put("description", desc);
									jsonObjectitems.put("unitPrice", nilai.intValue());
									jsonObjectitems.put("qty", 1);
									jsonObjectitems.put("amount", nilai.intValue());
									items.put(jsonObjectitems);
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e,
										"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
							}
						}
					}
				}
			}

			// Membuka Session setelah preprocessing yang tidak butuh db
			session = MahasiswaVirtualAccountHelper.openSession();

			Calendar calendar6 = Calendar.getInstance();
			calendar6.set(Calendar.DATE, calendar6.get(Calendar.DATE) + 1);

			boolean tagihan_expired_akhir_hari = Common
					.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF);
			Date expired_date = myjadwalPembayaran == null ? calendar6.getTime() : myjadwalPembayaran.getEndDate();

			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e,
							"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
				}
			} else {
				String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
				if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.HOUR_OF_DAY,
								calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
						expired_date = calendar.getTime();
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
					}
				} else {
					String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();
					if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE,
									calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
							expired_date = calendar.getTime();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						}
					}
				}
			}

			if (waktuSampai != null) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_15_MENIT)) {
						calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 15);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_30_MENIT)) {
						calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 30);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_JAM)) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 1);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_3_JAM)) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 3);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_6_JAM)) {
						calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 6);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_24_JAM)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_3_HARI)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 3);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_MINGGU)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
					} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_BULAN)) {
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
					}
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:774");
				}
			}

			VirtualAccountBank virtualAccountBankOnline = (VirtualAccountBank) session
					.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
					.add(bankHost == null || bankHost.getId() == null ? Restrictions.isNull("bankHost")
							: Restrictions.eq("bankHost.id", bankHost.getId()))
					.add(topup != null && topup > 0.1 ? Restrictions.eq("topup", topup)
							: Restrictions.sqlRestriction("true"))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("keterangan", pemb.toString() + (qris ? "qris:true" : "")
							+ (finpay ? "finpay:true" : "") + (onlineBmt ? OnlineBmtUtil.MARKER : "")))
					.add(mahasiswa == null || mahasiswa.getId() == null ? Restrictions.sqlRestriction("false")
							: Restrictions.eq("mahasiswa.id", mahasiswa.getId()))
					.add(Restrictions.eq("semester", smt))
					.add(myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("jenisKegiatan.id", myjadwalPembayaran.getJenisKegiatan().getId()))
					.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

			MahasiswaVirtualAccountHelper.pastikanTagihanBelumDibayar(session, mahasiswa, null, smt,
					myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan(), myjadwalPembayaran,
					pemb.toString() + (qris ? "qris:true" : "") + (finpay ? "finpay:true" : "")
							+ (onlineBmt ? OnlineBmtUtil.MARKER : ""),
					cicilan.toString(), detailbiaya.toString(), total);

			if (virtualAccountBankOnline != null && virtualAccountBankOnline.getChannel() != null
					&& esmartlinkBayarVia != null
					&& esmartlinkBayarVia.equalsIgnoreCase(virtualAccountBankOnline.getChannel())) {
				update = false;
			}

			if (virtualAccountBankOnline == null || update) {

				if (virtualAccountBankOnline == null) {
					virtualAccountBankOnline = new VirtualAccountBank(
							mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getId());
				} else if (update) {
					virtualAccountBankOnline.setVa(null);
					virtualAccountBankOnline.setLink(null);
					virtualAccountBankOnline.setResponse(null);
				}

				virtualAccountBankOnline.setKanalPembayaran(
						myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
								: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

				if (onlineBmt) {
					Long ptId = mahasiswa == null || mahasiswa.getJurusan() == null
							|| mahasiswa.getJurusan().getFakultas() == null
							|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? null
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getId();
					if (!OnlineBmtUtil.isPerguruanTinggiReady(ptId)) {
						if (warnings != null) warnings.add("Kanal Online BMT belum diaktifkan untuk perguruan tinggi ini.");
						return null;
					}
					OnlineBmtUtil.prepareInvoice(virtualAccountBankOnline);
				} else if (qris) {
					try {
						int mytotal = total.intValue() + biayaAdmin.intValue();
						JSONObject postData = new JSONObject();
						String va = Common.getGeneratedAngkaDigit(10);
						postData.put("merchantId",
								Common.getKonfigurasi("qris_jaring_merchantId", "3200124010015").getNilai());
						postData.put("terminalId",
								Common.getKonfigurasi("qris_jaring_terminalId", "10010005").getNilai());
						postData.put("trxId", va);
						postData.put("amount", String.valueOf(mytotal));
						postData.put("expire", Common.getKonfigurasi("qris_jaring_expire", "7200").getNilai());
						postData.put("posId", mahasiswa.getNim());
						postData.put("timestamp", Common.databaseDateFormat1.get().format(WaktuUtil.getDate()));

						String strURL = Common.getKonfigurasi("qris_jaring_gateway_url",
								"http://api.jsa2.host/agg/api/v1/qris/generate").getNilai();
						String screet_key = Common.getKonfigurasi("qris_jaring_screet_key", "").getNilai();

						String sign = postData.getString("merchantId") + postData.getString("terminalId")
								+ postData.getString("posId") + postData.getString("trxId")
								+ postData.getString("amount") + postData.getString("expire")
								+ postData.getString("timestamp") + screet_key;
						String token = DigestUtils.sha256Hex(sign);

						postData.put("signature", token);

						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Accept", "application/json");
						headers.put("Authorization", "Basic " + screet_key);

						String hasil = Common.executeHttp(strURL, "POST", postData.toString(), headers,
								"application/json");
						JSONObject jSONObject = new JSONObject(hasil);

						if (!jSONObject.getString("ack").equals("00")) {
							return null;
						}

						String data = jSONObject.getString("data");
						byte[] decodedBytes = org.apache.commons.codec.binary.Base64.decodeBase64(data);
						JSONObject jSONObjectDecode = new JSONObject(new String(decodedBytes));

						virtualAccountBankOnline.setRequest(postData.toString());
						virtualAccountBankOnline.setResponse(jSONObjectDecode.toString());
						virtualAccountBankOnline.setKode(jSONObjectDecode.getString("rawQRIS"));
						virtualAccountBankOnline.setBank("QRIS");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (finpay) {
					try {
						int mytotal = total.intValue() + biayaAdmin.intValue();

						if (expired_date == null) {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							expired_date = calendar.getTime();
						}

						String strURL = Common.getKonfigurasi("finpay_gateway_url_data",
								"https://devo.finnet.co.id/pg/payment/card/initiate").getNilai();
						String ApiKeyFinpay = Common.getKonfigurasi("finpay_apikeyfinpay_data", "").getNilai();
						String TokenFinpay = Common.getKonfigurasi("finpay_tokenFinpay_data", "").getNilai();

						String sender_name = mahasiswa.getNama();
						String sender_email = (mahasiswa.getEmail() != null && !mahasiswa.getEmail().isEmpty())
								? mahasiswa.getEmail().split(",")[0]
								: null;
						if (sender_email == null || sender_email.isEmpty()) {
							PerguruanTinggi pt = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
									? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
									: PerguruanTinggiUtil.getPerguruanTinggi();
							sender_email = pt.getEmail().split(",")[0].split(";")[0];
						}

						String sender_phone_number = mahasiswa.getTelp();
						if (sender_phone_number == null || sender_phone_number.isEmpty()) {
							PerguruanTinggi pt = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
									? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
									: PerguruanTinggiUtil.getPerguruanTinggi();
							sender_phone_number = pt.getTelepon();
						}

						if (sender_phone_number.startsWith("08"))
							sender_phone_number = "+62" + sender_phone_number.substring(1);
						else if (sender_phone_number.startsWith("0"))
							sender_phone_number = "+62" + sender_phone_number.substring(1);
						else if (!sender_phone_number.startsWith("+"))
							sender_phone_number = "+62" + sender_phone_number;

						JSONObject postData = new JSONObject();
						JSONObject customer = new JSONObject();
						customer.put("email", sender_email);
						customer.put("firstName", sender_name);
						customer.put("lastName", mahasiswa.getJurusan().getNama());
						customer.put("mobilePhone", sender_phone_number);
						postData.put("customer", customer);

						String va = String.valueOf(WaktuUtil.getDate().getTime());
						JSONObject order = new JSONObject();
						order.put("id", va);
						order.put("amount", String.valueOf(mytotal));
						order.put("description", keteranganSimpleBanget.toString().trim());
						postData.put("order", order);

						JSONObject url = new JSONObject();
						url.put("callbackUrl", Common.CURRENT_URL + "/Finpay");
						postData.put("url", url);

						String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(ApiKeyFinpay,
								TokenFinpay);

						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Authorization", screet_key);

						String hasil = Common.executeHttp(strURL, "POST", postData.toString(), headers,
								"application/json");
						JSONObject jSONObject = new JSONObject(hasil);

						try {
							virtualAccountBankOnline.setKadaluarsa(
									Common.databaseDateFormat1.get().parse(jSONObject.get("expiryLink") + ""));
						} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:942");
						}

						virtualAccountBankOnline.setRequest(postData.toString());
						virtualAccountBankOnline.setResponse(jSONObject.toString());

						if (!jSONObject.isNull("redirecturl")) {
							virtualAccountBankOnline.setLink(jSONObject.get("redirecturl") + "");
						} else {
							return null;
						}

						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("Finpay");

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (flip) {
					try {
						int mytotal = total.intValue() + biayaAdmin.intValue();

						if (expired_date == null) {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							expired_date = calendar.getTime();
						}

						String strURL = Common
								.getKonfigurasi("flip_gateway_url_v2", "https://bigflip.id/api/v2/pwf/bill").getNilai();

						String sender_address = mahasiswa.getAlamat();
						String sender_name = mahasiswa.getNama();
						String sender_email = mahasiswa.getEmail() != null ? mahasiswa.getEmail().split(",")[0] : "";
						String sender_phone_number = mahasiswa.getTelp();

						if (sender_address == null || sender_address.isEmpty())
							sender_address = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1();
						if (sender_email.isEmpty())
							sender_email = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getEmail();
						if (sender_phone_number == null || sender_phone_number.isEmpty())
							sender_phone_number = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
									.getTelepon();

						if (sender_phone_number.startsWith("08"))
							sender_phone_number = "+62" + sender_phone_number.substring(1);
						else if (sender_phone_number.startsWith("0"))
							sender_phone_number = "+62" + sender_phone_number.substring(1);
						else if (!sender_phone_number.startsWith("+"))
							sender_phone_number = "+62" + sender_phone_number;

						Map<String, Object> params = new HashMap<String, Object>();
						params.put("title", ket.toString().trim());
						params.put("amount", mytotal);
						params.put("type", "SINGLE");
						params.put("step", 2);
						params.put("expired_date", Common.databaseDateFormat2.get().format(expired_date));
						params.put("is_address_required", (sender_address.trim().isEmpty() ? 0 : 1));
						params.put("is_phone_number_required", (sender_phone_number.trim().isEmpty() ? 0 : 1));
						params.put("sender_name", sender_name);
						params.put("sender_email", sender_email);
						params.put("sender_phone_number", sender_phone_number);
						params.put("sender_address", sender_address);
						params.put("charge_fee", 1);

						String postData = URLBuilder.httpBuildQuery(params, "UTF-8");
						String apiKeyFlip = Common.getKonfigurasi("flip_gateway_api_key_flip", "").getNilai();
						String tokenFlip = Common.getKonfigurasi("flip_gateway_api_token_flip", "").getNilai();

						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Authorization",
								DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(apiKeyFlip, tokenFlip));

						String hasil = Common.executeHttp(strURL, "POST", postData, headers,
								"application/x-www-form-urlencoded");
						JSONObject jSONObject = new JSONObject(hasil);

						virtualAccountBankOnline.setRequest(postData);
						virtualAccountBankOnline.setResponse(jSONObject.toString());

						if (!jSONObject.isNull("link_url")) {
							virtualAccountBankOnline.setLink(jSONObject.get("link_url") + "");
						}

						virtualAccountBankOnline.setKode(jSONObject.get("link_id") + "");
						virtualAccountBankOnline.setBank("Flip");

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (otto) {
					try {
						int mytotal = total.intValue() + biayaAdmin.intValue();
						JSONObject data = OttoUtil.post(mahasiswa, mytotal, virtualAccountBankOnline);
						virtualAccountBankOnline.setLink(data.getJSONObject("responseData").get("endpointUrl") + "");
						virtualAccountBankOnline.setKode(data.getJSONObject("responseData").get("orderId") + "");
						virtualAccountBankOnline.setBank("Otto");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (briva) {
					try {
						int mytotal = total.intValue() + biayaAdmin.intValue();
						JSONObject data = BRIDataUtil.post(mahasiswa, mytotal, ket.toString(),
								virtualAccountBankOnline);
						expired_date = virtualAccountBankOnline.getKadaluarsaWaktu();
						virtualAccountBankOnline.setKode(
								(data.getJSONObject("virtualAccountData").get("virtualAccountNo") + "").trim());
						virtualAccountBankOnline.setBank("BRI");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (Common.bolehKonfigurasi("aktifkan_va_bankaltimtara_baru", Konfigurasi.TIDAK_AKTIF)) {
					try {
						JSONObject jsonObject = new JSONObject();
						String kodeInstitusi = Common.getKonfigurasi("kode_institusi_bankaltimtara_baru", "6001")
								.getNilai().trim();
						String jenisNama = myjadwalPembayaran.getJenisKegiatan() != null
								? myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan()
								: Common.getKonfigurasi("jenis_pembayaran_bankaltimtara_baru", "Uang Kuliah Tunggal")
										.getNilai().trim();
						String jenisTagihan = myjadwalPembayaran.getJenisKegiatan() != null
								? myjadwalPembayaran.getJenisKegiatan().getKode()
								: Common.getKonfigurasi("jenis_tagihan_bankaltimtara_baru", "01").getNilai().trim();
						int mytotal = total.intValue() + biayaAdmin.intValue();

						jsonObject.put("kode_institusi", kodeInstitusi);
						jsonObject.put("jenis_tagihan", jenisTagihan);
						jsonObject.put("jenis_pembayaran", jenisNama);
						jsonObject.put("tagihan", mytotal);
						jsonObject.put("nama", mahasiswa.getNama());
						jsonObject.put("npm", mahasiswa.getNim());
						jsonObject.put("kelompok_ukt", mahasiswa.getStatusAwalMahasiswa().getNama());
						jsonObject.put("jenjang", mahasiswa.getJenjang().getNama());
						jsonObject.put("semester", String.valueOf(smt));
						jsonObject.put("prodi", mahasiswa.getJurusan().getNama());
						jsonObject.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
						jsonObject.put("keterangan", ket.toString());

						String idSmt = "0";
						try {
							idSmt = tahunAkademik.split("/")[0] + (smt % 2 == 0 ? "2" : "1");
						} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1096");
						}

						jsonObject.put("semester_sesi", Integer.parseInt(idSmt));
						String va = kodeInstitusi + jenisTagihan + mahasiswa.getNim();

						try {
							String linkPost = Common.getKonfigurasi("url_create_va_bankaltimtara_baru",
									"http://36.66.232.249:8017/ubt/create_va").getNilai().trim();
							String signatureKey = Common.getKonfigurasi("key_bankaltimtara_baru",
									"").getNilai().trim();
							String appId = Common
									.getKonfigurasi("app_id_bankaltimtara_baru", "")
									.getNilai().trim();
							String payload = appId + ";create_va:" + mahasiswa.getNim();
							String signature = Common.buildHmacSignature(payload, signatureKey);

							Map<String, String> headers = new HashMap<String, String>();
							headers.put("signature", signature);

							String hasil = Common.executeHttp(linkPost, "POST", jsonObject.toString(), headers,
									"application/json");
							JSONObject jsonObject2 = new JSONObject(hasil);

							virtualAccountBankOnline.setRequest(jsonObject.toString());
							virtualAccountBankOnline.setResponse(jsonObject2.toString());
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						}

						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("bankaltimtara baru");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (Common.bolehKonfigurasi("aktifkan_va_maja", Konfigurasi.TIDAK_AKTIF) || maja) {
					try {
						String prefix = myjadwalPembayaran != null && myjadwalPembayaran.getJenisKegiatan() != null
								&& myjadwalPembayaran.getJenisKegiatan().getPrefixKodePembayaran() != null
										? myjadwalPembayaran.getJenisKegiatan().getPrefixKodePembayaran()
										: Common.getKonfigurasi("prefix_va_bank_online", "").getNilai();

						JSONObject jsonObject = new JSONObject();
						String va = prefix + Common.getGeneratedAngkaDigit(10);
						int mytotal = total.intValue() + biayaAdmin.intValue();
						jsonObject.put("date", Common.databaseDateFormat.get().format(new Date()));
						jsonObject.put("amount", mytotal);
						jsonObject.put("name", mahasiswa.getNama());
						jsonObject.put("email", mahasiswa.getEmail());
						jsonObject.put("address", mahasiswa.getAlamat());
						jsonObject.put("va", va);
						jsonObject.put("attribute1", mahasiswa.getJurusan().getFakultas().getNama());
						jsonObject.put("attribute2", mahasiswa.getJurusan().getNama());
						jsonObject.put("attribute3", mahasiswa.getNim());
						jsonObject.put("attribute4", mahasiswa.getStatusAwalMahasiswa() == null ? ""
								: mahasiswa.getStatusAwalMahasiswa().getNama());
						jsonObject.put("attribute5", mahasiswa.getProgram());
						jsonObject.put("items", items);
						jsonObject.put("attributes", new JSONArray());

						String CLIENT_TOKEN = null;
						try {
							CLIENT_TOKEN = BSIMajaUtil.sendRequestToken();
						} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1163");
						}

						try {
							JSONObject jsonObject2 = BSIMajaUtil.sendRequest(jsonObject, CLIENT_TOKEN, true);
							virtualAccountBankOnline.setRequest(jsonObject.toString());
							virtualAccountBankOnline.setResponse(jsonObject2.toString());
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						}

						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("Maja");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (Common.bolehKonfigurasi("aktifkan_va_jaring", Konfigurasi.TIDAK_AKTIF)) {
					try {
						String va = Common.getGeneratedAngkaDigit(10);
						int mytotal = total.intValue() + biayaAdmin.intValue();
						JSONObject postData = new JSONObject();
						postData.put("custName", mahasiswa.getNama());
						postData.put("custID", mahasiswa.getNim());
						postData.put("trxID", va);
						postData.put("productID", Common.getKonfigurasi("va_jaring_produk_id", "207").getNilai());
						postData.put("paymentType", Common.getKonfigurasi("va_jaring_payment_type", "04").getNilai());
						postData.put("productName", myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan());
						postData.put("amount", String.valueOf(mytotal));
						postData.put("expire", Common.getKonfigurasi("va_jaring_expire", "1440").getNilai());
						postData.put("urlCallback", Common.getRequestHostWithProtocol() + "/Jaring");
						postData.put("timestamp", Common.databaseDateFormat1.get().format(WaktuUtil.getDate()));

						String strURL = Common.getKonfigurasi("va_jaring_gateway_url",
								"http://sandbox.jaring.host/api/v3/billpay/inquiry").getNilai();
						String screet_key = Common.getKonfigurasi("va_jaring_screet_key", "")
								.getNilai();

						String sign = postData.getString("custName") + postData.getString("custID")
								+ postData.getString("trxID") + postData.getString("productID")
								+ postData.getString("productName") + postData.getString("paymentType")
								+ postData.getString("timestamp") + postData.getString("amount")
								+ postData.getString("expire") + postData.getString("urlCallback") + screet_key;
						String token = DigestUtils.sha256Hex(sign);
						postData.put("signature", token);

						Map<String, String> headers = new HashMap<String, String>();
						headers.put("Accept", "application/json");
						headers.put("Authorization", "Basic " + screet_key);

						String hasil = Common.executeHttp(strURL, "POST", postData.toString(), headers,
								"application/json");
						JSONObject jSONObject = new JSONObject(hasil);

						if (!jSONObject.getString("ack").equals("00")) {
							return null;
						}

						virtualAccountBankOnline.setRequest(postData.toString());
						virtualAccountBankOnline.setResponse(jSONObject.toString());
						virtualAccountBankOnline.setKode(jSONObject.getString("payCode"));
						virtualAccountBankOnline.setBank("Jaring");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}

				} else if (Common.bolehKonfigurasi("aktifkan_va_e_smartlink", Konfigurasi.TIDAK_AKTIF) || smartlink) {
					virtualAccountBankOnline.setLink("");
					String variableSmartlink = Common.getKonfigurasi("channel_biaya_e_smartlink",
							"VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart")
							.getNilai();

					if (virtualAccountBankOnline != null && virtualAccountBankOnline.getKanalPembayaran() != null) {
						variableSmartlink = virtualAccountBankOnline.getKanalPembayaran()
								.getVariableBiayaAdminEsmartlink();
					}

					if (!variableSmartlink.isEmpty() && esmartlinkBayarVia == null) {
						if (param.get("smartlink_direct") != null
								&& Boolean.TRUE.equals(param.get("smartlink_direct"))) {
							String va = Common.getGeneratedBarCode();
							virtualAccountBankOnline.setLink(param.get("payment_url") + "");
							virtualAccountBankOnline.setKode(va);
							virtualAccountBankOnline.setBank("Esmartlink");
						} else {
							MyWindow window = new MyWindow("Pilih Channel Pembayaran", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							MahasiswaSmartlinkChannelWindow.init(window, mahasiswa, null, smt, myjadwalPembayaran,
									detailBiayas, param, biayaAdmin, tabungan, topup, bankHost, ket.toString(),
									pemb.toString(), cicilan.toString(), total, items, true);
							window.setHeight("90%");
							window.setWidth("600px");
							window.setVisible(true);
							window.onModal();
							return null;
						}
					} else {
						try {
							if (expired_date == null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
								expired_date = calendar.getTime();
							}

							String va = Common.getGeneratedBarCode(30);
							// Hitung amount dari sum(items) agar selalu identik dengan payload — bukan dari total+biayaAdmin
							// yang bisa berbeda jika items sudah dimodifikasi di luar alur normal.
							int mytotal = 0;
							for (int ix = 0; ix < items.length(); ix++) {
								try {
									mytotal += items.getJSONObject(ix).optInt("amount", 0);
								} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1279");
								}
							}
							if (mytotal == 0) {
								mytotal = total.intValue() + biayaAdmin.intValue();
							}
							JSONObject postData = new JSONObject();
							postData.put("order_id", va);
							postData.put("amount", mytotal);
							postData.put("description", myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan());

							JSONObject customer = new JSONObject();
							customer.put("name", mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", ""));

							String sender_email = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getEmail();
							if (mahasiswa != null && !mahasiswa.getEmail().isEmpty())
								sender_email = mahasiswa.getEmail().split(",")[0];

							String sender_phone_number = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
									.getTelepon();
							if (mahasiswa != null && !mahasiswa.getTelp().isEmpty() && mahasiswa.getTelp().length() > 8
									&& mahasiswa.getTelp().length() < 15) {
								sender_phone_number = mahasiswa.getTelp();
							}

							customer.put("email", sender_email);
							customer.put("phone", sender_phone_number);
							postData.put("customer", customer);

							JSONArray itemsSmartlink = new JSONArray();
							for (int i = 0; i < items.length(); i++) {
								try {
									JSONObject d = items.getJSONObject(i);
									int amount = d.getInt("amount");
									if (amount > 0) {
										JSONObject jsonObject = new JSONObject();
										String description = d.getString("description");
										if (description.length() > 255) {
											description = description.substring(0, 255);
										}

										jsonObject.put("name", description);
										jsonObject.put("amount", amount);
										jsonObject.put("qty", 1);
										itemsSmartlink.put(jsonObject);
									}
								} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1325");
								}
							}
							postData.put("item", itemsSmartlink);

							if (esmartlinkBayarVia == null) {
								String cannel_va_e_smartlink = Common
										.getKonfigurasi("cannel_va_e_smartlink", "VA_CIMB,VA_BRI").getNilai();
								String[] ch = cannel_va_e_smartlink.split(",");
								JSONArray channel = new JSONArray();
								for (String c : ch) {
									if (!c.trim().isEmpty())
										channel.put(c.trim());
								}
								postData.put("channel", channel);
							} else {
								JSONArray channel = new JSONArray();
								channel.put(esmartlinkBayarVia);
								postData.put("channel", channel);
							}

							String link = Common.getRequestHostWithProtocol();
							if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
								link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol())
										.getNilai();
							}

							postData.put("type", "payment-page");
							postData.put("payment_mode", "CLOSE");
							postData.put("expired_time", Common.iso8601.get().format(expired_date));
							postData.put("callback_url", link + "/Esmartlink");
							postData.put("success_redirect_url", link + "/PembayaranSukses");
							postData.put("failed_redirect_url", link + "/PembayaranGagal");

							String strURL = Common
									.getKonfigurasi("gateway_url_va_e_smartlink",
											"https://payment-service-sbx.pakar-digital.com/api/payment/create-order")
									.getNilai();
							String username_va_e_smartlink = Common
									.getKonfigurasi("username_va_e_smartlink", "")
									.getNilai().trim();
							String password_va_e_smartlink = Common
									.getKonfigurasi("password_va_e_smartlink", "").getNilai().trim();

							if (virtualAccountBankOnline != null
									&& virtualAccountBankOnline.getKanalPembayaran() != null) {
								username_va_e_smartlink = virtualAccountBankOnline.getKanalPembayaran()
										.getUsernameEsmartlink();
								password_va_e_smartlink = virtualAccountBankOnline.getKanalPembayaran()
										.getPasswordEsmartlink();
							}

							String hasil = VirtualAccountBank.curlSmartlink(strURL, username_va_e_smartlink,
									password_va_e_smartlink, postData);

							// FIX JSONException "A JSONObject text must begin with '{'": curlSmartlink() memanggil
							// gateway pembayaran eksternal (payment-service Esmartlink) via HTTP -- bila gateway
							// down/timeout/mengembalikan halaman error HTML (bukan JSON), atau koneksi terputus
							// sebelum body lengkap terkirim, `hasil` menjadi kosong/bukan JSON valid. Sebelumnya
							// new JSONObject(hasil) langsung melempar JSONException generik yang membingungkan --
							// validasi eksplisit di sini memberi pesan akar-masalah yang jelas (termasuk cuplikan
							// respons mentah utk diagnosis), tetap tertangkap oleh catch (Exception e) di bawah
							// (sama seperti sebelumnya) sehingga perilaku "gagal -> null + info ke admin" tak berubah.
							if (hasil == null || hasil.trim().isEmpty() || !hasil.trim().startsWith("{")) {
								String cuplikan = hasil == null ? "(null)"
										: (hasil.trim().length() > 200 ? hasil.trim().substring(0, 200) + "..." : hasil.trim());
								throw new Exception("Gateway pembayaran (Esmartlink) tidak merespons dengan format JSON yang "
										+ "benar -- kemungkinan gateway sedang down/timeout/gangguan jaringan. Respons mentah: "
										+ cuplikan);
							}

							JSONObject jSONObject = new JSONObject(hasil);

							if (!(jSONObject.get("code") + "").equals("0")) {
								try {
									if (warnings != null) {
										warnings.add(jSONObject.getString("message"));
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1387");
									// TODO: handle exception
								}
								return null;
							}

							JSONObject data = jSONObject.getJSONObject("data");
							virtualAccountBankOnline.setRequest(postData.toString());
							virtualAccountBankOnline.setResponse(jSONObject.toString());
							virtualAccountBankOnline.setLink(data.get("payment_url") + "");
							virtualAccountBankOnline.setKode(va);
							virtualAccountBankOnline.setBank("Esmartlink");
						} catch (Exception e) {
							if (warnings != null)
								warnings.add(e.getMessage());
							Common.tampilErrorJikaAdmin(e,
									"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
							return null;
						}
					}

				} else if (Common.bolehKonfigurasi("aktifkan_va_bjb_langsung", Konfigurasi.TIDAK_AKTIF)) {
					try {
						String va = Common.getGeneratedAngkaDigit(12);
						int mytotal = total.intValue() + biayaAdmin.intValue();
						BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
						String hp = biodataMahasiswa.getHp();
						String telp = biodataMahasiswa.getTeleponRumah();

						String customer_phone = (hp == null || hp.trim().equals("08100000000000000000")
								|| hp.trim().equals("0000000000") ? "" : hp)
								+ (telp == null || telp.trim().isEmpty() || telp.trim().equals("00000000000000000000")
										|| telp.trim().equals("000000000")
												? ""
												: (hp == null || hp.trim().isEmpty()
														|| hp.trim().equals("08100000000000000000")
														|| hp.trim().equals("0000000000") ? "" : " / ") + telp);

						String product_code = Common.maxPanjangAkhir(myjadwalPembayaran.getJenisKegiatan().getKode(),
								2);

						JSONObject postData = new JSONObject();
						postData.put("customer_email", biodataMahasiswa.getEmail());
						postData.put("billing_type", "f");
						postData.put("customer_code", va);
						postData.put("customer_phone", customer_phone);
						postData.put("description", Common.maxPanjangAkhir(pemb.toString(), 1000));
						postData.put("client_refnum", va);
						postData.put("amount", String.valueOf(mytotal));
						postData.put("customer_name", mahasiswa.getNama() == null ? ""
								: mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", ""));
						postData.put("product_code", product_code);
						postData.put("cin", Common.getKonfigurasi("bjb_langsung_cin", "530").getNilai());
						postData.put("expired_date", Common.databaseDateFormat1.get().format(expired_date));
						postData.put("client_type", "1");
						postData.put("va_type", "m");
						postData.put("currency", "360");

						JSONObject jSONObject = BJBUtil.billingBJB(postData.toString(), true);
						virtualAccountBankOnline.setRequest(postData.toString());
						virtualAccountBankOnline.setResponse(jSONObject.toString());
						virtualAccountBankOnline.setKode(jSONObject.getString("va_number"));
						virtualAccountBankOnline.setBank("BJB");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e,
								"Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n", true);
						return null;
					}
				} else {
					int jml_digit_prefix_va_bank_online = 10;
					try {
						jml_digit_prefix_va_bank_online = Integer
								.parseInt(Common.getKonfigurasi("jml_digit_prefix_va_bank_online",
										String.valueOf(jml_digit_prefix_va_bank_online)).getNilai());
					} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1461");
					}

					String subva = Common.bolehKonfigurasi("subva_paka_nim", Konfigurasi.TIDAK_AKTIF) ? mahasiswa.getNim()
									: Common.getGeneratedAngkaDigit(jml_digit_prefix_va_bank_online);
					String prefix = myjadwalPembayaran != null && myjadwalPembayaran.getJenisKegiatan() != null
							&& myjadwalPembayaran.getJenisKegiatan().getPrefixKodePembayaran() != null
									? myjadwalPembayaran.getJenisKegiatan().getPrefixKodePembayaran()
									: Common.getKonfigurasi("prefix_va_bank_online", "").getNilai();

					String va = prefix + subva;
					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setBank("Bank Online");
				}

				virtualAccountBankOnline.setKadaluarsa(expired_date);
				virtualAccountBankOnline.setOtomatis(false);
				virtualAccountBankOnline.setCicilan(cicilan.toString());
				virtualAccountBankOnline
						.setJenisKegiatan(myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());
				virtualAccountBankOnline.setKeterangan(pemb.toString() + (qris ? "qris:true" : "")
						+ (finpay ? "finpay:true" : "") + (onlineBmt ? OnlineBmtUtil.MARKER : ""));
				virtualAccountBankOnline.setTotal(total);
				virtualAccountBankOnline.setBulanan("");
				virtualAccountBankOnline.setDetailbiaya(detailbiaya.toString());
				virtualAccountBankOnline.setBiayaAdmin(biayaAdmin);
				virtualAccountBankOnline.setMahasiswa(mahasiswa);
				virtualAccountBankOnline.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankOnline.setSemester(smt);
				virtualAccountBankOnline
						.setTahunAkademik(myjadwalPembayaran == null ? null : myjadwalPembayaran.getTahunAkademik());
				virtualAccountBankOnline.setBankHost(bankHost);
				virtualAccountBankOnline.setTabungan(tabungan);
				virtualAccountBankOnline.setTopup(topup);

				Transaction transaction = null;
				try {
					if (session == null || !session.isOpen()) {
						closeSessionQuietly(session);
						session = MahasiswaVirtualAccountHelper.openSession();
					}
					transaction = session.beginTransaction();
					if (virtualAccountBankOnline.getId() == null) {
						session.save(virtualAccountBankOnline);
					} else {
						session.update(virtualAccountBankOnline);
					}
					transaction.commit();
				} catch (Exception txe) {
					rollbackQuietly(transaction);
					throw txe;
				}
			}

			return virtualAccountBankOnline;

		} catch (MahasiswaVirtualAccountHelper.TagihanSudahDibayarException e) {
			throw e;
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e, "Terjadi kesalahan. Harap informasikan ke admin, info teknis sbb:\n\n",
						true);
			} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1521");

			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1524");
			return null;
		} finally {
			// Blok finally untuk memastikan tidak ada koneksi database yang bocor.
			// Karena method ini dipanggil dari Timer ZK, gunakan session lokal dan jangan
			// bergantung pada session bersama yang bisa sudah tertutup oleh helper lain.
			closeSessionQuietly(session);
		}
	}

	/** Rollback transaksi Hibernate secara aman; galat rollback dicatat ke audit dan tidak dilempar ulang. */
	private static void rollbackQuietly(Transaction transaction) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1539");
		}
	}

	/** Menutup session Hibernate secara aman (clear + disconnect + close), menelan seluruh galat agar aman dipanggil di blok {@code finally}. */
	private static void closeSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1551");
				}
				try {
					session.disconnect();
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1555");
				}
				session.close();
			}
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankOnline.java:1559");
		}
	}
}
