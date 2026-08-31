package ais.action.master.sekolah.util;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.WaktuUtil;

/**
 * Utilitas pencetakan struk dan pengiriman notifikasi pembayaran siswa modul Sekolah, mendukung
 * beberapa jalur pembayaran: tunai/manual ({@link #cetakStruk}), deposit ({@link #cetakDeposit}),
 * dan berbagai payment gateway bank — BRI, BNI, BSI, dan Virtual Account
 * ({@link #cetakBri}/{@link #cetakBni}/{@link #cetakBsi}/{@link #cetakVa}). Keempat method
 * {@code cetak<Bank>} mengikuti pola yang identik: menyiapkan parameter laporan (id transaksi,
 * id sekolah, nominal terbilang, kode transaksi 8 digit dari {@link #formatKodeTransaksi}, waktu
 * cetak), menyalin data biodata siswa/calon siswa, memanggil {@link #dataPembayaran} untuk
 * melampirkan rincian item tagihan, lalu menghasilkan PDF struk lewat {@link Report} dan
 * mengirimkannya ({@link #kirim}).
 *
 * <p>
 * {@link #dataPembayaran} adalah inti pengumpulan data: mengambil seluruh
 * {@link PembayaranSiswaDetail} terkait satu pembayaran (dicocokkan lewat salah satu dari
 * bniRequest/briRequest/bsiRequest/virtualAccountBank/pembayaranSiswa — berhenti lebih awal bila
 * {@code pembayaranSiswa} belum tersimpan, untuk menghindari {@code TransientObjectException}
 * Hibernate), menjumlahkan nominal riil tiap baris (memakai {@code nominalManual} bila diisi,
 * atau {@code nominal+denda-diskon} dari {@link Tagihan} terkait), dan menyinkronkan ulang kolom
 * {@code nominal}/{@code tambahanDeposit} pada {@link PembayaranSiswa} bila berbeda dari hasil
 * hitung. Method ini membuka sesi Hibernate sendiri dan hanya membuka transaksi baru bila belum
 * ada transaksi aktif (mendukung dipanggil dari dalam maupun luar transaksi pemanggil).
 * </p>
 *
 * <p>
 * {@link #kirim} mengirim notifikasi pembayaran berhasil ke wali murid lewat dua kanal: email
 * (lampiran PDF struk, lewat {@link MailSender}) dan, bila konfigurasi
 * {@code aktifkan_kirim_notif_pembayaran_ke_wa} aktif, WhatsApp (lewat {@code Wa.kirimWaViaUltramsg},
 * dijalankan dengan jeda 2 detik lewat timer agar tidak memblokir alur cetak). Salam pembuka
 * (Pagi/Siang/Sore/Malam) disesuaikan otomatis dengan jam saat pengiriman.
 * </p>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PembayaranSiswaUtil {

	/**
	 * Mengumpulkan rincian item pembayaran (satu pembayaran dicocokkan lewat salah satu request
	 * bank/VA atau {@code pembayaranSiswa} langsung), menjumlahkan nominal riil, menyinkronkan
	 * ulang {@code pembayaranSiswa.nominal}/{@code tambahanDeposit} bila berbeda, dan mengisi
	 * {@code parameters} dengan kunci {@code "terbilang"} (nominal dalam kata) dan {@code "maps"}
	 * (daftar baris rincian item siap dicetak). Berhenti tanpa efek bila {@code pembayaranSiswa}
	 * diberikan tapi belum tersimpan (id null) dan tidak ada request bank/VA lain yang valid.
	 *
	 * @param pembayaranSiswa     entitas pembayaran utama, boleh {@code null} bila dicocokkan lewat request bank/VA
	 * @param bniRequest          request BNI terkait, boleh {@code null}
	 * @param briRequest          request BRI terkait, boleh {@code null}
	 * @param bsiRequest          request BSI terkait, boleh {@code null}
	 * @param virtualAccountBank  request Virtual Account terkait, boleh {@code null}
	 * @param parameters          peta parameter laporan yang akan diisi {@code terbilang}/{@code maps}
	 * @throws Exception diteruskan dari kegagalan Hibernate (transaksi lokal di-rollback bila dibuka sendiri)
	 */
	public static void dataPembayaran(PembayaranSiswa pembayaranSiswa, BniRequest bniRequest, BriRequest briRequest,
			BsiRequest bsiRequest, VirtualAccountBank virtualAccountBank, Map parameters) throws Exception {

		Session session = null;
		boolean isLocalTransaction = false;
		List<PembayaranSiswaDetail> pembayaranSiswaDetails = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			
			// Amankan transaksi data saat update nominal
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				isLocalTransaction = true;
			}

			Criteria criteria = session.createCriteria(PembayaranSiswaDetail.class);

			// Pengkondisian Filter dengan alias yang dioptimalkan
			if (bniRequest != null && bniRequest.getId() != null) {
				criteria.createAlias("pembayaranSiswa", "ps").add(Restrictions.eq("ps.bniRequest", bniRequest));
			} else if (briRequest != null && briRequest.getId() != null) {
				criteria.createAlias("pembayaranSiswa", "ps").add(Restrictions.eq("ps.briRequest", briRequest));
			} else if (bsiRequest != null && bsiRequest.getId() != null) {
				criteria.createAlias("pembayaranSiswa", "ps").add(Restrictions.eq("ps.bsiRequest", bsiRequest));
			} else if (virtualAccountBank != null && virtualAccountBank.getId() != null) {
				criteria.createAlias("pembayaranSiswa", "ps")
						.add(Restrictions.eq("ps.virtualAccountBank", virtualAccountBank));
			} else if (pembayaranSiswa != null && pembayaranSiswa.getId() != null) {
				criteria.add(Restrictions.eq("pembayaranSiswa", pembayaranSiswa));
			} else {
				// FIX TransientObjectException: objek pembayaranSiswa null / belum tersimpan (id null)
				// TIDAK boleh jadi parameter Criteria (Hibernate menolak entity transient). Tak ada
				// detail untuk pembayaran yang belum tersimpan -> hentikan lebih awal agar tidak crash.
				return;
			}

			// Optimasi Relasi: Menggunakan LEFT_JOIN agar record tidak hilang jika ada field relasi yang null di DB
			pembayaranSiswaDetails = criteria.createAlias("tagihan", "tagihan", Criteria.LEFT_JOIN)
					.createAlias("tagihan.pengaturanBiaya", "pengaturanBiaya", Criteria.LEFT_JOIN)
					.createAlias("pengaturanBiaya.jenisBiayaSekolah", "jenisBiayaSekolah", Criteria.LEFT_JOIN)
					.addOrder(Order.asc("jenisBiayaSekolah.nama")).addOrder(Order.asc("tagihan.tahunbulan")).list();

			List<Map> maps = new ArrayList<Map>();
			double total = 0.0; // Menggunakan primitif murni untuk efisiensi memori
			Tbmuser tbmuserValidator = null;

			for (PembayaranSiswaDetail detail : pembayaranSiswaDetails) {
				double nominal = 0.0;
				Tagihan tagihan = detail.getTagihan();

				// Pengecekan Null yang ketat pada relasi Tagihan
				double tagNominal = (tagihan != null && tagihan.getNominal() != null) ? tagihan.getNominal() : 0.0;
				double tagDenda = (tagihan != null && tagihan.getDenda() != null) ? tagihan.getDenda() : 0.0;
				double tagDiskon = (tagihan != null && tagihan.getDiskon() != null) ? tagihan.getDiskon() : 0.0;
				double tagDiskonTdk = (tagihan != null && tagihan.getDiskonTidakLangsung() != null) ? tagihan.getDiskonTidakLangsung() : 0.0;

				if (detail.getNominalManual() != null) {
					nominal = detail.getNominalManual();
				} else {
					nominal = (tagNominal + tagDenda) - tagDiskon;
				}

				total += nominal;
				Map map = new HashMap();

				Common.insertProperty(PembayaranSiswaDetail.class, detail, map, "", 1);

				// Ekstraksi data yang aman dari NullPointerException
				Long jenisBiayaId = null;
				String jenisBiayaNama = "";
				if (tagihan != null && tagihan.getPengaturanBiaya() != null
						&& tagihan.getPengaturanBiaya().getJenisBiayaSekolah() != null) {
					jenisBiayaId = tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getId();
					jenisBiayaNama = tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getNama();
				}

				map.put("jenis_biaya_id", jenisBiayaId);
				map.put("jenis_biaya", jenisBiayaNama);
				map.put("tanggal", detail.getPembayaranSiswa() != null ? detail.getPembayaranSiswa().getTanggalBayar() : null);
				map.put("bulan", tagihan != null ? tagihan.getBulan() : null);
				map.put("tahun", tagihan != null ? tagihan.getTahun() : null);
				map.put("id_transaksi", detail.getPembayaranSiswa() != null ? detail.getPembayaranSiswa().getId() : null);

				// Optimasi String Builder
				StringBuilder item = new StringBuilder();
				if (tagihan != null && tagihan.getItemBiayaSekolah() != null && tagihan.getItemBiayaSekolah().getNama() != null) {
					item.append(tagihan.getItemBiayaSekolah().getNama());
				}
				if (tagihan != null && tagihan.getBulan() != null && tagihan.getBulan() > 0) {
					item.append(" ").append(Common.BULAN[tagihan.getBulan() - 1]);
				}
				if (tagihan != null && tagihan.getTahun() != null && tagihan.getTahun() > 0) {
					item.append(" ").append(tagihan.getTahun());
				}
				map.put("item_biaya", item.toString());

				map.put("nominal", nominal);
				map.put("denda", tagDenda);
				map.put("nilai", tagihan != null ? tagihan.ambilNominal() : 0.0);
				map.put("diskon", tagDiskon);
				map.put("diskon_tidak_langsung", tagDiskonTdk);
				map.put("diskon_siswa", (tagihan != null && tagihan.getDiskonSiswa() != null) ? tagihan.getDiskonSiswa().getNama() : "");

				AkunPembayaranSiswa akun = detail.getPembayaranSiswa() != null ? detail.getPembayaranSiswa().getAkunPembayaranSiswa() : null;
				map.put("cara", akun == null ? "" : akun.getNama());

				PembayaranSiswa pembayaranSiswaData = detail.getPembayaranSiswa();
				map.put("tambahan_deposit", 0.0);
				map.put("validator", pembayaranSiswaData == null ? "" : (pembayaranSiswaData.getValidator() != null ? pembayaranSiswaData.getValidator() : ""));

				if (pembayaranSiswaData != null) {
					tbmuserValidator = pembayaranSiswaData.getValidatorUser();
				}

				map.put("bayarke", tagihan != null ? tagihan.getBayarKe() : null);
				map.put("dibayarsebayak", detail.getNominalBiaya() != null ? detail.getNominalBiaya().getDibayarSebayak() : null);

				KelasSiswa kelasSiswa = tagihan != null ? tagihan.getKelasSiswa() : null;
				map.put("kelas", kelasSiswa == null ? "" : (kelasSiswa.getNama() != null ? kelasSiswa.getNama() : ""));

				if (tagihan != null && tagihan.getCalonSiswa() != null) {
					CalonSiswa calon = tagihan.getCalonSiswa();
					map.put("nomor_induk", calon.getNoRegistrasi() != null ? calon.getNoRegistrasi() : "");
					map.put("nama_siswa", calon.getNama() != null ? calon.getNama() : "");
					map.put("sekolah_id", calon.getSekolah() != null ? calon.getSekolah().getId() : null);
					map.put("asrama", "");
				} else if (tagihan != null && tagihan.getSiswa() != null) {
					Siswa siswa = tagihan.getSiswa();
					String noInduk = siswa.getNomorInduk() == null || siswa.getNomorInduk().trim().isEmpty()
							? siswa.getNomorIndukNasional()
							: siswa.getNomorInduk();

					map.put("nomor_induk", noInduk != null ? noInduk : "");
					map.put("nama_siswa", siswa.getNama() != null ? siswa.getNama() : "");
					map.put("sekolah_id", siswa.getSekolah() != null ? siswa.getSekolah().getId() : null);

					AsramaSiswa asramaSiswa = siswa.getAsrama();
					map.put("asrama", asramaSiswa == null ? "" : (asramaSiswa.getNama() != null ? asramaSiswa.getNama() : ""));
				}
				
				maps.add(map);
			}

			try {
				if (tbmuserValidator != null && tbmuserValidator.getUserId() != null) {
					Common.insertProperty(Tbmuser.class, tbmuserValidator, parameters, "validatorData", 2);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:198");
				// Ignored
			}

			if (pembayaranSiswa != null) {
				Double nPembayaran = pembayaranSiswa.getNominal() != null ? pembayaranSiswa.getNominal() : 0.0;
				if (Double.compare(total, nPembayaran) != 0) {
					pembayaranSiswa.setNominal(total);
					pembayaranSiswa.setTambahanDeposit(total);
					Common.refreshUpdate(session, pembayaranSiswa);
				}
			}

			if (isLocalTransaction) {
				session.getTransaction().commit();
			}

			parameters.put("terbilang", IndonesianNumberToWords.convert((long) total));
			parameters.put("maps", maps);

		} catch (Exception e) {
			if (isLocalTransaction && session != null && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:220");}
			}
			throw e;
		} finally {
			if (pembayaranSiswaDetails != null) {
				pembayaranSiswaDetails.clear();
				pembayaranSiswaDetails = null; 
			}
			
			// Wajib Membersihkan Memori Session
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:231");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:232");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:233");}
			}
		}
	}

	/** Menghasilkan kode transaksi 8 digit dari {@code id} (8 digit terakhir bila id cukup panjang, dipadding nol bila lebih pendek), atau angka acak 8 digit bila {@code id} {@code null}. */
	private static String formatKodeTransaksi(Long id) {
		if (id == null) {
			return String.format("%08d", Common.randLong());
		}
		String kode = String.valueOf(id);
		if (kode.length() >= 8) {
			return kode.substring(kode.length() - 8);
		}
		return String.format("%08d", id);
	}

	/** Mencetak struk PDF untuk pembayaran tunai/manual (tanpa payment gateway) dan mengirimkannya ke wali murid lewat email/WhatsApp; tidak melakukan apa pun bila {@code pembayaranSiswa} {@code null}. */
	@SuppressWarnings({ })
	public static void cetakStruk(PembayaranSiswa pembayaranSiswa) throws Exception {
		if (pembayaranSiswa == null) return; 

		Map parameters = new HashMap();
		parameters.put("id_pembayaran", pembayaranSiswa.getId());
		parameters.put("id_sekolah", pembayaranSiswa.getSekolah() != null ? pembayaranSiswa.getSekolah().getId() : -1L);
		parameters.put("id_bri", -1L);
		parameters.put("id_bni", -1L);
		parameters.put("id_bsi", -1L);
		parameters.put("id_va", -1L);

		parameters.put("kode_transaksi", formatKodeTransaksi(pembayaranSiswa.getId()));
		parameters.put("waktu_cetak", Common.dateFormat1.get()
				.format(pembayaranSiswa.getTanggal() != null ? pembayaranSiswa.getTanggal() : WaktuUtil.getDate()));

		if (pembayaranSiswa.getCalonSiswa() != null) {
			Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
		} else if (pembayaranSiswa.getSiswa() != null) {
			Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
		}

		dataPembayaran(pembayaranSiswa, null, null, null, null, parameters);

		try {
			Report.generatePDFReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			kirim(pembayaranSiswa, file);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:279");
		}
	}

	/** Mencetak struk PDF setoran deposit siswa ({@link DepositSiswa}, hanya bila nominal &gt; 0.1) dan mengirimkannya ke wali murid lewat email/WhatsApp. */
	@SuppressWarnings({ })
	public static void cetakDeposit(DepositSiswa depositSiswa) throws Exception {
		if (depositSiswa != null && depositSiswa.getNominal() != null && depositSiswa.getNominal() > 0.1) {
			Map parameters = new HashMap();
			parameters.put("id_deposit", depositSiswa.getId());
			parameters.put("id_sekolah", depositSiswa.getSekolah() != null ? depositSiswa.getSekolah().getId() : -1L);
			parameters.put("terbilang", IndonesianNumberToWords.convert(depositSiswa.getNominal().longValue()));

			try {
				Report.generatePDFReport(Report.PDF, parameters, "sekolah/deposit", WaktuUtil.getDate(), Common.locale);
				File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/deposit", WaktuUtil.getDate(), Common.locale);
				kirim(depositSiswa.getSiswa(), depositSiswa.getCalonSiswa(), null, "", file);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:296");
			}
		}
	}

	/** Mencetak struk PDF pembayaran via payment gateway BRI dan mengirimkannya ke wali murid; lihat pola umum {@code cetak<Bank>} pada javadoc kelas. */
	@SuppressWarnings({ })
	public static void cetakBri(PembayaranSiswa pembayaranSiswa, BriRequest briRequest) throws Exception {
		Map parameters = new HashMap();
		Long briId = (briRequest != null) ? briRequest.getId() : -1L;
		Long psId = (pembayaranSiswa != null) ? pembayaranSiswa.getId() : -1L;

		parameters.put("id_pembayaran", (pembayaranSiswa == null || pembayaranSiswa.getBriRequest() != null) ? -1L : psId);
		parameters.put("id_sekolah", pembayaranSiswa == null ? -1L : (pembayaranSiswa.getSekolah() != null ? pembayaranSiswa.getSekolah().getId() : -1L));
		parameters.put("id_bri", (pembayaranSiswa != null && pembayaranSiswa.getBriRequest() != null) ? pembayaranSiswa.getBriRequest().getId() : briId);

		if (briRequest != null && briRequest.getAmount() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(briRequest.getAmount().longValue()));
		} else if (pembayaranSiswa != null && pembayaranSiswa.getNominal() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(pembayaranSiswa.getNominal().longValue()));
		} else {
			parameters.put("terbilang", "");
		}

		Long refId = pembayaranSiswa != null ? pembayaranSiswa.getId() : (briRequest != null ? briRequest.getId() : null);
		parameters.put("kode_transaksi", formatKodeTransaksi(refId));

		Date tglCetak = WaktuUtil.getDate();
		if (pembayaranSiswa != null && pembayaranSiswa.getTanggal() != null) {
			tglCetak = pembayaranSiswa.getTanggal();
		} else if (briRequest != null && briRequest.getTanggal_dirubah() != null) {
			tglCetak = briRequest.getTanggal_dirubah();
		}
		parameters.put("waktu_cetak", Common.dateFormat1.get().format(tglCetak));

		if (pembayaranSiswa != null) {
			if (pembayaranSiswa.getCalonSiswa() != null) {
				Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
			} else if (pembayaranSiswa.getSiswa() != null) {
				Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
			}
			dataPembayaran(pembayaranSiswa, null, briRequest, null, null, parameters);
		}

		try {
			Report.generatePDFReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);

			if (briRequest != null) {
				kirim(briRequest.getSiswa(), briRequest.getCalonSiswa(), pembayaranSiswa, file);
			} else if (pembayaranSiswa != null) {
				kirim(pembayaranSiswa, file);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:349");
		}
	}

	/** Mencetak struk PDF pembayaran via payment gateway BNI dan mengirimkannya ke wali murid; lihat pola umum {@code cetak<Bank>} pada javadoc kelas. */
	@SuppressWarnings({ })
	public static void cetakBni(PembayaranSiswa pembayaranSiswa, BniRequest bniRequest) throws Exception {
		Map parameters = new HashMap();
		Long bniId = (bniRequest != null) ? bniRequest.getId() : -1L;
		Long psId = (pembayaranSiswa != null) ? pembayaranSiswa.getId() : -1L;

		parameters.put("id_pembayaran", (pembayaranSiswa == null || pembayaranSiswa.getBniRequest() != null) ? -1L : psId);
		parameters.put("id_sekolah", pembayaranSiswa == null ? -1L : (pembayaranSiswa.getSekolah() != null ? pembayaranSiswa.getSekolah().getId() : -1L));
		parameters.put("id_bni", (pembayaranSiswa != null && pembayaranSiswa.getBniRequest() != null) ? pembayaranSiswa.getBniRequest().getId() : bniId);

		if (bniRequest != null && bniRequest.getAmount() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(bniRequest.getAmount().longValue()));
		} else if (pembayaranSiswa != null && pembayaranSiswa.getNominal() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(pembayaranSiswa.getNominal().longValue()));
		} else {
			parameters.put("terbilang", "");
		}

		Long refId = pembayaranSiswa != null ? pembayaranSiswa.getId() : (bniRequest != null ? bniRequest.getId() : null);
		parameters.put("kode_transaksi", formatKodeTransaksi(refId));

		Date tglCetak = WaktuUtil.getDate();
		if (pembayaranSiswa != null && pembayaranSiswa.getTanggal() != null) {
			tglCetak = pembayaranSiswa.getTanggal();
		} else if (bniRequest != null && bniRequest.getTanggal_dirubah() != null) {
			tglCetak = bniRequest.getTanggal_dirubah();
		}
		parameters.put("waktu_cetak", Common.dateFormat1.get().format(tglCetak));

		if (pembayaranSiswa != null) {
			if (pembayaranSiswa.getCalonSiswa() != null) {
				Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
			} else if (pembayaranSiswa.getSiswa() != null) {
				Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
			}
			dataPembayaran(pembayaranSiswa, bniRequest, null, null, null, parameters);
		}

		try {
			Report.generatePDFReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);

			if (bniRequest != null) {
				kirim(bniRequest.getSiswa(), bniRequest.getCalonSiswa(), pembayaranSiswa, file);
			} else if (pembayaranSiswa != null) {
				kirim(pembayaranSiswa, file);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:401");
		}
	}

	/** Mengirim notifikasi pembayaran untuk {@code pembayaranSiswa} (siswa/calon siswa diambil darinya); tidak melakukan apa pun bila {@code pembayaranSiswa} {@code null}. */
	public static void kirim(PembayaranSiswa pembayaranSiswa, File file) throws Exception {
		if (pembayaranSiswa == null) return;
		kirim(pembayaranSiswa.getSiswa(), pembayaranSiswa.getCalonSiswa(), pembayaranSiswa, file);
	}

	/** Menyusun URL struk terenkripsi (bila {@code pembayaranSiswa} tersimpan) dan mendelegasikan ke {@link #kirim(Siswa, CalonSiswa, Double, String, File)} dengan nominal dari {@code pembayaranSiswa}. */
	public static void kirim(Siswa siswa, CalonSiswa calonSiswa, PembayaranSiswa pembayaranSiswa, File file)
			throws Exception {
		String url = "";
		if (pembayaranSiswa != null && pembayaranSiswa.getId() != null) {
			url = Common.getRequestHostWithProtocol() + "/Struk?id="
					+ URLEncoder.encode(("EE" + Common.desEncrypter.get().encrypt(pembayaranSiswa.getId() + "")), "UTF-8");
		}
		kirim(siswa, calonSiswa, pembayaranSiswa == null ? null : pembayaranSiswa.getNominal(), url, file);
	}

	/**
	 * Implementasi kanonik pengiriman notifikasi pembayaran: menyusun subjek+isi pesan (salam
	 * disesuaikan jam, nominal dan link struk disisipkan bila diberikan), mengirim email lewat
	 * {@link MailSender#sendMailLampiran} ke alamat email siswa/calon siswa (digabung bila
	 * keduanya valid), dan bila konfigurasi {@code aktifkan_kirim_notif_pembayaran_ke_wa} aktif,
	 * mengirim WhatsApp ke seluruh nomor telepon terkait siswa/calon siswa (dari
	 * {@code ambilTelp()}, melewati nomor placeholder semua-nol) lewat timer tertunda 2 detik agar
	 * tidak memblokir alur cetak.
	 *
	 * @param siswa      siswa penerima notifikasi, boleh {@code null} bila memakai {@code calonSiswa}
	 * @param calonSiswa calon siswa penerima notifikasi, boleh {@code null} bila memakai {@code siswa}
	 * @param nilai      nominal pembayaran untuk ditampilkan di pesan, boleh {@code null}
	 * @param url        link struk online, boleh {@code null}/kosong
	 * @param file       berkas PDF struk untuk dilampirkan
	 * @throws Exception diteruskan dari kegagalan pengiriman email
	 */
	public static void kirim(final Siswa siswa, final CalonSiswa calonSiswa, final Double nilai, final String url,
			final File file) throws Exception {

		JSONArray userIds = new JSONArray();
		if (siswa != null && siswa.getNomorIndukNasional() != null && !siswa.getNomorIndukNasional().isEmpty()) {
			userIds.put(siswa.getNomorIndukNasional());
		} else if (calonSiswa != null && calonSiswa.getNomorIndukNasional() != null) {
			userIds.put(calonSiswa.getNomorIndukNasional());
		}

		final String sekolah = siswa != null ? (siswa.getSekolah() != null ? siswa.getSekolah().getNama() : "")
				: (calonSiswa != null ? (calonSiswa.getSekolah() != null ? calonSiswa.getSekolah().getNama() : "") : "");
		final String nama = siswa != null ? siswa.getNama() : (calonSiswa != null ? calonSiswa.getNama() : "");

		int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		final String waktu = (jam >= 10 && jam < 15) ? "Siang"
				: (jam >= 15 && jam < 18) ? "Sore" : (jam >= 18 && jam <= 24) ? "Malam" : "Pagi";

		String subject = "Informasi Pembayaran Berhasil => Siswa: " + nama;

		String nominalStr = (nilai == null) ? "" : " senilai Rp " + Common.numberFormat.get().format(nilai) + " ";
		String linkStr = (url == null || url.trim().isEmpty()) ? "." : ", atau juga bisa buka di link " + url;

		// Optimasi: Build String Email
		StringBuilder bodyEmail = new StringBuilder();
		bodyEmail.append("Selamat ").append(waktu).append(",<br><br>Yth. Bapak/Ibu Wali Murid,<br><br><br>")
				 .append("Kami dari pihak sekolah <b>").append(sekolah).append("</b> menginformasikan bahwa pembayaran atas nama ")
				 .append(nama).append(nominalStr)
				 .append(" telah berhasil dilakukan. Silakan periksa catatan transaksi pada file terlampir atau pada aplikasi atau portal kami")
				 .append(linkStr).append("\r\n<br><br>\nTerima kasih atas partisipasi Bapak/Ibu.");

		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
		StringBuilder emailUser = new StringBuilder();

		if (siswa != null && siswa.getAlamatEmail() != null && Common.isValidEmailAddress(siswa.getAlamatEmail())) {
			emailUser.append(siswa.getAlamatEmail().trim());
		}
		if (calonSiswa != null && calonSiswa.getAlamatEmail() != null
				&& Common.isValidEmailAddress(calonSiswa.getAlamatEmail())) {
			if (emailUser.length() > 0)
				emailUser.append(",");
			emailUser.append(calonSiswa.getAlamatEmail().trim());
		}

		MailSender.sendMailLampiran(userIds, subject, bodyEmail.toString(), sender, emailUser.toString(),
				calonSiswa != null ? calonSiswa : siswa, null, false, file);

		if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pembayaran_ke_wa")) {

			// Optimasi Ekstrim: Bangun pesan WA SATU KALI di luar Timer Event. Menghemat CPU & Memori
			final String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
					"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n").getNilai();

			StringBuilder bodyWa = new StringBuilder();
			bodyWa.append("Selamat ").append(waktu).append(",\n\nYth. Bapak/Ibu Wali Murid,\n\n")
				  .append("Kami dari pihak sekolah *").append(sekolah).append("* menginformasikan bahwa pembayaran atas nama *")
				  .append(nama).append("*").append(nominalStr)
				  .append(" telah berhasil dilakukan. Silakan periksa catatan transaksi pada file terlampir atau pada aplikasi atau portal kami")
				  .append(linkStr).append("\r\n\n\n*Terima kasih atas partisipasi Bapak/Ibu.*");

			final String bodyWaFinal = bodyWa.toString();
			final String urlD = (file != null)
					? Common.getRequestHostWithProtocolSimple() + file.getAbsolutePath().split("webapps")[1]
					: url;
			final Sekolah sekolahObj = siswa != null ? siswa.getSekolah() : (calonSiswa != null ? calonSiswa.getSekolah() : null);
			final Set<String> forms = siswa != null ? siswa.ambilTelp() : (calonSiswa != null ? calonSiswa.ambilTelp() : new HashSet<String>());

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					for (String from : forms) {
						if (from != null && !from.trim().isEmpty() && !from.trim().equals("00000000000000000000")
								&& !from.trim().equals("000000000")) {
							Wa.kirimWaViaUltramsg(from, dawal + bodyWaFinal, "Bukti_Pembayaran.pdf", urlD,
									Wa.buatProfile(sekolahObj, PerguruanTinggiUtil.getPerguruanTinggi()));
						}
					}
				}
			}, "", false, 2000);
		}
	}

	/** Mencetak struk PDF pembayaran via Virtual Account bank dan mengirimkannya ke wali murid; parameter {@code cetak} memicu pembuatan salinan PDF tambahan (mis. untuk dicetak langsung di kasir). Lihat pola umum {@code cetak<Bank>} pada javadoc kelas. */
	@SuppressWarnings({ })
	public static void cetakVa(PembayaranSiswa pembayaranSiswa, VirtualAccountBank virtualAccountBank, boolean cetak)
			throws Exception {
		Map parameters = new HashMap();
		Long vaId = (virtualAccountBank != null) ? virtualAccountBank.getId() : -1L;
		Long psId = (pembayaranSiswa != null) ? pembayaranSiswa.getId() : -1L;

		parameters.put("id_pembayaran", (pembayaranSiswa == null || pembayaranSiswa.getBniRequest() != null
				|| pembayaranSiswa.getVirtualAccountBank() != null) ? -1L : psId);
		parameters.put("id_sekolah", pembayaranSiswa == null ? -1L : (pembayaranSiswa.getSekolah() != null ? pembayaranSiswa.getSekolah().getId() : -1L));
		parameters.put("id_va", (pembayaranSiswa != null && pembayaranSiswa.getVirtualAccountBank() != null)
						? pembayaranSiswa.getVirtualAccountBank().getId()
						: vaId);

		if (virtualAccountBank != null && virtualAccountBank.getTotal() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(virtualAccountBank.getTotal().longValue()));
		} else if (pembayaranSiswa != null && pembayaranSiswa.getNominal() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(pembayaranSiswa.getNominal().longValue()));
		} else {
			parameters.put("terbilang", "");
		}

		Long refId = pembayaranSiswa != null ? pembayaranSiswa.getId() : (virtualAccountBank != null ? virtualAccountBank.getId() : null);
		parameters.put("kode_transaksi", formatKodeTransaksi(refId));

		Date tglCetak = WaktuUtil.getDate();
		if (pembayaranSiswa != null && pembayaranSiswa.getTanggal() != null) {
			tglCetak = pembayaranSiswa.getTanggal();
		} else if (virtualAccountBank != null && virtualAccountBank.getTanggal_dirubah() != null) {
			tglCetak = virtualAccountBank.getTanggal_dirubah();
		}
		parameters.put("waktu_cetak", Common.dateFormat1.get().format(tglCetak));

		if (pembayaranSiswa != null) {
			if (pembayaranSiswa.getCalonSiswa() != null) {
				Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
			} else if (pembayaranSiswa.getSiswa() != null) {
				Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
			}
			dataPembayaran(pembayaranSiswa, null, null, null, virtualAccountBank, parameters);
		}

		try {
			File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			if (virtualAccountBank != null) {
				kirim(virtualAccountBank.getSiswa(), virtualAccountBank.getCalonSiswa(), pembayaranSiswa, file);
			} else if (pembayaranSiswa != null) {
				kirim(pembayaranSiswa, file);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:552");
		}

		if (cetak) {
			try {
				Report.generatePDFReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:559");
			}
		}
	}

	/** Mencetak struk PDF pembayaran via payment gateway BSI dan mengirimkannya ke wali murid; lihat pola umum {@code cetak<Bank>} pada javadoc kelas. */
	@SuppressWarnings({ })
	public static void cetakBsi(PembayaranSiswa pembayaranSiswa, BsiRequest bsiRequest) throws Exception {
		Map parameters = new HashMap();
		Long bsiId = (bsiRequest != null) ? bsiRequest.getId() : -1L;
		Long psId = (pembayaranSiswa != null) ? pembayaranSiswa.getId() : -1L;

		parameters.put("id_pembayaran", (pembayaranSiswa == null || pembayaranSiswa.getBsiRequest() != null) ? -1L : psId);
		parameters.put("id_bsi", (pembayaranSiswa != null && pembayaranSiswa.getBsiRequest() != null)
						? pembayaranSiswa.getBsiRequest().getId()
						: bsiId);
		parameters.put("id_sekolah", pembayaranSiswa == null ? -1L : (pembayaranSiswa.getSekolah() != null ? pembayaranSiswa.getSekolah().getId() : -1L));

		if (bsiRequest != null && bsiRequest.getAmount() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(bsiRequest.getAmount().longValue()));
		} else if (pembayaranSiswa != null && pembayaranSiswa.getNominal() != null) {
			parameters.put("terbilang", IndonesianNumberToWords.convert(pembayaranSiswa.getNominal().longValue()));
		} else {
			parameters.put("terbilang", "");
		}

		Long refId = pembayaranSiswa != null ? pembayaranSiswa.getId() : (bsiRequest != null ? bsiRequest.getId() : null);
		parameters.put("kode_transaksi", formatKodeTransaksi(refId));

		Date tglCetak = WaktuUtil.getDate();
		if (pembayaranSiswa != null && pembayaranSiswa.getTanggal() != null) {
			tglCetak = pembayaranSiswa.getTanggal();
		} else if (bsiRequest != null && bsiRequest.getTanggal_dirubah() != null) {
			tglCetak = bsiRequest.getTanggal_dirubah();
		}
		parameters.put("waktu_cetak", Common.dateFormat1.get().format(tglCetak));

		if (pembayaranSiswa != null) {
			if (pembayaranSiswa.getCalonSiswa() != null) {
				Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
			} else if (pembayaranSiswa.getSiswa() != null) {
				Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
			}
			dataPembayaran(pembayaranSiswa, null, null, bsiRequest, null, parameters);
		}

		try {
			Report.generatePDFReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);
			File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran", WaktuUtil.getDate(), Common.locale);

			if (bsiRequest != null) {
				kirim(bsiRequest.getSiswa(), bsiRequest.getCalonSiswa(), pembayaranSiswa, file);
			} else if (pembayaranSiswa != null) {
				kirim(pembayaranSiswa, file);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/PembayaranSiswaUtil.java:614");
		}
	}
}