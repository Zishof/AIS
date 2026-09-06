package ais.action.master.helper.virtualaccount;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Tipe khusus untuk download no registrasi calon mahasiswa bank bankaltimtara. Kelas ini memberi
 * nama dan batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranUtil pembayaranUtil}, {@code
 * JenisKegiatan jenisKegiatan}; pembacaan/pencarian ({@code downloadData()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-06)</b>: kredensial gateway {@code bankaltimtara_username}/
 * {@code bankaltimtara_password} sebelumnya memiliki nilai default hardcode {@code "ubtva1"}/
 * {@code "12345678"} sebagai fallback {@link Common#getKonfigurasi(String, String)} — sudah diganti
 * string kosong, pola identik dgn perbaikan di
 * {@code DownloadTagihanMahasiswaBankBankaltimtara}/{@code DownloadNoUjianCalonMahasiswaBankBankaltimtara}.
 * Objek {@code login} (username+password JSON) sebelumnya juga dicetak ke stdout/log server via
 * {@code System.out.println} — baris log tersebut sudah dihapus.
 * </p>
 *
 * @see MyWindow
 */
public class DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	static JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);

	public DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara() {
		super();

	}

	public DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	@SuppressWarnings("rawtypes")
	public static VirtualAccountBank downloadData(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JadwalPembayaran myjadwalPembayaran, Collection detailBiayas, Double biayaAdmin, boolean pakaiva)
			throws Exception {

		String detailbiaya = "";

		Session session = MahasiswaVirtualAccountHelper.openSession();
		try {

		String pemb = "";
		String ket = "";
		Double total = 0.0;

		String cicilan = "";
		for (Object o : detailBiayas) {
			if (o instanceof DetailBiaya) {
				DetailBiaya biaya = (DetailBiaya) o;
				ItemBiaya itemBiaya = biaya.getItemBiaya();
				detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());

				Double nilai = (biaya.getNilaiBiayaBaru() == null ? biaya.getNilaiBiaya() : biaya.getNilaiBiayaBaru());
				if (nilai > 0.01) {
					pemb += itemBiaya.getKode().trim() + "," + itemBiaya.getNama().trim() + "," + (nilai).longValue()
							+ ";";

					cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-" + biaya.getBayarKe() + "-"
									+ biaya.getId() + "-" + biaya.getId()));

					ket += ket.isEmpty() ? itemBiaya.getNama() : "," + itemBiaya.getNama();

					total += nilai;
				}
			}
		}

		String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
		Date expired_date = myjadwalPembayaran.getEndDate();
		if (!tagihan_expired_jam.isEmpty()) {
			if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY,
							calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara.java:94");
				}
			}
		} else {
			String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();

			if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara.java:106");
				}
			}
		}

		VirtualAccountBank virtualAccountBankBankaltimtara = (VirtualAccountBank) session
				.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
				.add(Restrictions.eq("keterangan", pemb)).add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.add(Restrictions.eq("pakaiva", pakaiva))
				.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
				.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (virtualAccountBankBankaltimtara == null) {
			int mytotal = total.intValue() + biayaAdmin.intValue();
			int panjang = 15;
			try {
				panjang = Integer.parseInt(Common.getKonfigurasi("bankaltimtara_panjang_va", panjang + "").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara.java:123");
			}

			String va = Common.getKonfigurasi("bankaltimtara_prefix_va", "0099").getNilai()
					+ Common.getGeneratedAngkaDigit(panjang);
			if (pakaiva) {
				JSONObject jsonObject = new JSONObject();

				System.out.println("Request body: ");

				String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_url_autentication",
						"https://api-dev.bankaltimtara.co.id:8300/api/user/auth").getNilai());

				String user = Common.getKonfigurasi("bankaltimtara_username", "").getNilai();
				String pwd = Common.getKonfigurasi("bankaltimtara_password", "").getNilai();

				JSONObject login = new JSONObject();
				login.put("username", user);
				login.put("password", pwd);

				String[] command = { "curl", "--silent", "--show-error", "--location", strURL, "--header",
						"Content-type: application/json", "--data-raw", login.toString() };

				ProcessBuilder process = new ProcessBuilder(command);
				process.redirectErrorStream(true);
				Process p;
				p = process.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				String hasil = builder.toString();
				System.out.println(hasil);

				JSONObject jSONObject = BankaltimtaraResponseUtil.parseJson(hasil, "autentikasi VA registrasi");

				String token = BankaltimtaraResponseUtil.ambilStringWajib(jSONObject, "token", "autentikasi VA registrasi");

				strURL = (Common.getKonfigurasi("bankaltimtara_gateway_url_create_va",
						"https://api-dev.bankaltimtara.co.id:8300/api/va/create").getNilai());

//			String inst_id = Common.getKonfigurasi("bankaltimtara_inst_id_va", "0099").getNilai();
//			jsonObject.put("inst_id", inst_id);
				jsonObject.put("number", va);
				jsonObject.put("name", Common.maxPanjang(
						biodataCalonMahasiswa.getNama() + " " + biodataCalonMahasiswa.getNoRegistrasi(), 30));
				jsonObject.put("amount", mytotal + "");
				jsonObject.put("description", Common.maxPanjang(ket, 30));

				String postData = jsonObject.toString();

				System.out.println(postData + "");

				command = new String[] { "curl", "--silent", "--show-error", "--location", strURL, "--header",
						"Content-type: application/json", "--header", "Authorization: Bearer " + token,
						"--data-raw", postData };

				process = new ProcessBuilder(command);
				process.redirectErrorStream(true);

				p = process.start();
				reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				builder = new StringBuilder();
				line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				hasil = builder.toString();
				System.out.println(hasil);

				jSONObject = BankaltimtaraResponseUtil.parseJson(hasil, "pembuatan VA registrasi");

				JSONObject response = jSONObject;
				BankaltimtaraResponseUtil.pastikanSukses(response, "pembuatan VA registrasi");

				if (response.getString("code").equals("00")) {
					virtualAccountBankBankaltimtara = new VirtualAccountBank(
							PerguruanTinggiUtil.getPerguruanTinggi().getId());
					virtualAccountBankBankaltimtara.setPakaiva(pakaiva);
					virtualAccountBankBankaltimtara.setKanalPembayaran(
							myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
									: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());
					virtualAccountBankBankaltimtara.setKadaluarsa(expired_date);
					virtualAccountBankBankaltimtara.setOtomatis(false);
					virtualAccountBankBankaltimtara.setKode(va);
					virtualAccountBankBankaltimtara.setRequest(postData);
					virtualAccountBankBankaltimtara
							.setLink(response.isNull("barcode") ? "" : response.get("barcode") + "");
					virtualAccountBankBankaltimtara.setResponse(response == null ? "" : response.toString());
					virtualAccountBankBankaltimtara.setCicilan(cicilan);
					virtualAccountBankBankaltimtara.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
					virtualAccountBankBankaltimtara.setKeterangan(pemb);
					virtualAccountBankBankaltimtara.setTotal(total);
					virtualAccountBankBankaltimtara.setBulanan("");
					virtualAccountBankBankaltimtara.setDetailbiaya(detailbiaya);

					virtualAccountBankBankaltimtara.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
					virtualAccountBankBankaltimtara.setJadwalPembayaran(myjadwalPembayaran);
					virtualAccountBankBankaltimtara.setSemester(0);
					virtualAccountBankBankaltimtara.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
					virtualAccountBankBankaltimtara.setBank("Bank Bankaltimtara");

					MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
					session.saveOrUpdate(virtualAccountBankBankaltimtara);
					MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
				}

			} else {
				virtualAccountBankBankaltimtara = new VirtualAccountBank(
						PerguruanTinggiUtil.getPerguruanTinggi().getId());
				virtualAccountBankBankaltimtara.setPakaiva(pakaiva);
				virtualAccountBankBankaltimtara.setKanalPembayaran(
						myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
								: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());
				virtualAccountBankBankaltimtara.setKadaluarsa(expired_date);
				virtualAccountBankBankaltimtara.setOtomatis(false);
				virtualAccountBankBankaltimtara.setKode(va);

				try {
					DownloadTagihanMahasiswaBankBankaltimtara.qris(mytotal, va, virtualAccountBankBankaltimtara);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara.java:246");
				}
				virtualAccountBankBankaltimtara.setCicilan(cicilan);
				virtualAccountBankBankaltimtara.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
				virtualAccountBankBankaltimtara.setKeterangan(pemb);
				virtualAccountBankBankaltimtara.setTotal(total);
				virtualAccountBankBankaltimtara.setBulanan("");
				virtualAccountBankBankaltimtara.setDetailbiaya(detailbiaya);

				virtualAccountBankBankaltimtara.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				virtualAccountBankBankaltimtara.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankBankaltimtara.setSemester(0);
				virtualAccountBankBankaltimtara.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
				virtualAccountBankBankaltimtara.setBank("Bank Bankaltimtara");

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				session.saveOrUpdate(virtualAccountBankBankaltimtara);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
			}
		}

		// session.disconnect();
		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
		MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

		return virtualAccountBankBankaltimtara;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara.java:276");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara.java:277");}
			}
		}
	}
}
