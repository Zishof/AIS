package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaItemBiaya;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;

public class TagihanUtilCalonSiswa {

	private static void pastikanNominalBulananSamaDenganNominalBiaya(Session session, Tagihan tagihan,
			NominalBiaya nominalBiaya) {
		if (session == null || tagihan == null || tagihan.getId() == null || nominalBiaya == null
				|| nominalBiaya.getPengaturanBiaya() == null
				|| nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah() == null
				|| !"Bulanan".equalsIgnoreCase(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode())) {
			return;
		}

		// Jangan sentuh tagihan yang sudah dibayar
		if (tagihan.getPembayaranSiswaDetail() != null) {
			return;
		}

		Double nominalAcuan = Boolean.TRUE.equals(nominalBiaya.getBukanTagihan()) ? 0.0 : nominalBiaya.getNominal();
		if (nominalAcuan == null) {
			nominalAcuan = 0.0;
		}
		// nominalBiaya (FK) tidak boleh diubah jika sudah punya referensi sendiri
		if (tagihan.getNominalBiaya() == null) {
			tagihan.setNominalBiaya(nominalBiaya);
		}
		tagihan.setBayarKe(1);
		tagihan.setNominal(nominalAcuan);
		Transaction tx = session.beginTransaction();
		Common.refreshUpdate(session, tagihan);
		tx.commit();
	}

	public static List<Tagihan> doGenerateTagihanBulanan(CalonSiswa calonSiswa, PengaturanBiaya pengaturanBiaya,
			Integer tahunSampai, Integer bulanSampai, boolean refresh) {
		List<Tagihan> tagihans = new ArrayList<Tagihan>();

		if (pengaturanBiaya != null && pengaturanBiaya.getTahunAngkatan() != 0 && calonSiswa.getTahunMasuk() != null
				&& !pengaturanBiaya.getTahunAngkatan().equals(calonSiswa.getTahunMasuk())) {
			return saring(tagihans);
		}

		int bulanTahunUtama = PembayaranSiswa.convert(tahunSampai, bulanSampai);
		Session session = null;
		// PERBAIKAN (session closed): currentNativeSession() thread-local, bukan call-scoped -- bila
		// pemanggil (mis. doSinkronkanTagihanCalonSiswa yang memegang batchSession di thread pool yang
		// sama) SUDAH membuka native session lebih dulu, panggilan di sini akan MEMINJAM (bukan membuka
		// baru) session ancestor tsb. Tutup paksa di finally seperti sebelumnya membuat ancestor melihat
		// "Session is closed!" saat lanjut memakainya (batchSession.update(...)). Hanya tutup bila
		// method INI yang benar-benar membuka baru.
		boolean sessionSudahTerbukaSebelumnya = HibernateUtil.isNativeSessionOpenForCurrentThread();
		try {
			session = HibernateUtil.currentNativeSession();
			List<PengaturanBiayaItemBiaya> daftarBiayas = ConstantValues
					.simpleList(
							session.createCriteria(PengaturanBiayaItemBiaya.class)
									.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
							PengaturanBiayaItemBiaya.class);

			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : daftarBiayas) {
				try {
					if (Tagihan.getBoleh(pengaturanBiaya, null, calonSiswa,
							pengaturanBiayaItemBiaya.getItemBiayaSekolah())) {

						Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
						cal.set(Calendar.DATE, 1);
						PembayaranSiswa pembayaranSiswaTerakhir = (PembayaranSiswa) session
								.createCriteria(PembayaranSiswa.class).add(Restrictions.isNotNull("bulan"))
								.add(Restrictions.isNotNull("tahun")).add(Restrictions.eq("calonSiswa", calonSiswa))
								.add(Restrictions.eq("jenisBiayaSekolah", pengaturanBiaya.getJenisBiayaSekolah()))
								.addOrder(Order.desc("tahunDanBulan")).setMaxResults(1).uniqueResult();

						if (pembayaranSiswaTerakhir != null && pembayaranSiswaTerakhir.getBulan() != null
								&& pembayaranSiswaTerakhir.getTahun() != null) {
							cal.set(Calendar.MONTH, pembayaranSiswaTerakhir.getBulan());
							cal.set(Calendar.YEAR, pembayaranSiswaTerakhir.getTahun());
						}

						Integer pembayaranTerakhir = 0;
						int iterasiBulan = 0; // Penjaga agar tidak infinite loop jika ada kesalahan convert

						while (bulanTahunUtama > pembayaranTerakhir && iterasiBulan < 120) {
							int tahunCurrent = cal.get(Calendar.YEAR);
							int bulanCurrent = cal.get(Calendar.MONTH);
							int bulanCurrentPlus = bulanCurrent + 1;
							pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);

							NominalBiaya nominalBiayaCalonSiswa = ambilNominalBiaya(pengaturanBiayaItemBiaya,
									calonSiswa, pembayaranTerakhir, session);
							if (nominalBiayaCalonSiswa == null || nominalBiayaCalonSiswa.getNominal() == null
									|| nominalBiayaCalonSiswa.getNominal() <= 0.1
									|| Boolean.TRUE.equals(nominalBiayaCalonSiswa.getBukanTagihan())) {
								cal.add(Calendar.MONTH, 1);
								iterasiBulan++;
								continue;
							}

							int batasBayar = nominalBiayaCalonSiswa.getDibayarSebayak() != null
									? nominalBiayaCalonSiswa.getDibayarSebayak()
									: 1;
							for (int bayarKe = 1; bayarKe <= batasBayar; bayarKe++) {
								Tagihan tagihan = Tagihan.ambilAtauBuat(session,
										nominalBiayaCalonSiswa.getItemBiayaSekolah(),
										nominalBiayaCalonSiswa.getPengaturanBiaya(), nominalBiayaCalonSiswa.getSiswa(),
										nominalBiayaCalonSiswa.getCalonSiswa(), bayarKe, nominalBiayaCalonSiswa,
										pembayaranTerakhir, refresh);

								if (tagihan != null && tagihan.getId() != null) {
									pastikanNominalBulananSamaDenganNominalBiaya(session, tagihan,
											nominalBiayaCalonSiswa);
									tagihans.add(tagihan);
								}
							}
							cal.add(Calendar.MONTH, 1);
							iterasiBulan++;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:154");
		} finally {
			if (!sessionSudahTerbukaSebelumnya) {
				closeSessionAndDisconnect(session);
			}
		}

		return saring(tagihans);
	}

	public static List<Tagihan> doGenerateTagihanTahunan(CalonSiswa calonSiswa, PengaturanBiaya pengaturanBiaya,
			Integer tahunSampai) {
		List<Tagihan> tagihans = new ArrayList<Tagihan>();

		if (pengaturanBiaya != null && pengaturanBiaya.getTahunAngkatan() != 0 && calonSiswa.getTahunMasuk() != null
				&& !pengaturanBiaya.getTahunAngkatan().equals(calonSiswa.getTahunMasuk())) {
			return saring(tagihans);
		}

		Session session = null;
		// PERBAIKAN (session closed): lihat catatan sama di doGenerateTagihanBulanan() -- jangan tutup
		// session ancestor yang dipinjam via currentNativeSession() dari thread yang sama.
		boolean sessionSudahTerbukaSebelumnya = HibernateUtil.isNativeSessionOpenForCurrentThread();
		try {
			session = HibernateUtil.currentNativeSession();
			List<PengaturanBiayaItemBiaya> daftarBiayas = ConstantValues
					.simpleList(
							session.createCriteria(PengaturanBiayaItemBiaya.class)
									.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
							PengaturanBiayaItemBiaya.class);
			List<Long> notPembayaran = new ArrayList<Long>();

			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : daftarBiayas) {
				try {
					NominalBiaya nominalBiayaCalonSiswa = ambilNominalBiaya(pengaturanBiayaItemBiaya, calonSiswa,
							session);

					if (Tagihan.getBoleh(pengaturanBiaya, null, calonSiswa,
							pengaturanBiayaItemBiaya.getItemBiayaSekolah()) && nominalBiayaCalonSiswa != null
							&& nominalBiayaCalonSiswa.getNominal() != null
							&& nominalBiayaCalonSiswa.getNominal() > 0.0) {

						int batasBayar = nominalBiayaCalonSiswa.getDibayarSebayak() != null
								? nominalBiayaCalonSiswa.getDibayarSebayak()
								: 1;

						for (int bayarKe = 1; bayarKe <= batasBayar; bayarKe++) {
							String kodeUnik = Tagihan.genCode(nominalBiayaCalonSiswa.getItemBiayaSekolah(),
									nominalBiayaCalonSiswa.getPengaturanBiaya(), tahunSampai,
									nominalBiayaCalonSiswa.getSiswa(), calonSiswa, bayarKe);

							Tagihan tagihan = (Tagihan) session.createCriteria(Tagihan.class)
									.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();

							if (tagihan == null) {
								try {
									Criteria critDetail = session.createCriteria(PembayaranSiswaDetail.class)
											.createAlias("tagihan", "tagihan")
											.add(Restrictions.eq("tagihan.bayarKe", bayarKe))
											.add(Restrictions.eq("nominalBiaya", nominalBiayaCalonSiswa));

									if (!notPembayaran.isEmpty()) {
										critDetail.add(Restrictions.not(Restrictions.in("id", notPembayaran)));
									}

									PembayaranSiswaDetail pembayaranSiswaDetail = (PembayaranSiswaDetail) critDetail
											.add(Restrictions.eq("itemBiayaSekolah",
													pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
											.createCriteria("pembayaranSiswa")
											.add(Restrictions.eq("calonSiswa", calonSiswa))
											.add(Restrictions.eq("jenisBiayaSekolah",
													pengaturanBiaya.getJenisBiayaSekolah()))
											.add(Restrictions.isNull("bulan"))
											.add(Restrictions.eq("tahun", tahunSampai)).setMaxResults(1)
											.addOrder(Order.desc("id")).uniqueResult();

									if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
										notPembayaran.add(pembayaranSiswaDetail.getId());
									}

									if (pembayaranSiswaDetail == null || pembayaranSiswaDetail.getTagihan() == null) {
										Double n = null;

										if (bayarKe > 1) {
											Number nm = (Number) session.createCriteria(Tagihan.class)
													.add(Restrictions.eq("nominalBiaya", nominalBiayaCalonSiswa))
													.add(Restrictions.lt("bayarKe", bayarKe))
													.setProjection(Projections.sum("nominal")).uniqueResult();

											if (nm != null && nominalBiayaCalonSiswa.getNominal() != null
													&& nm.doubleValue() < nominalBiayaCalonSiswa.getNominal()
															.doubleValue()) {
												n = nominalBiayaCalonSiswa.getNominal() - nm.doubleValue();
											}
										}

										tagihan = new Tagihan();
										tagihan.setNominalBiaya(nominalBiayaCalonSiswa);
										tagihan.setTahunbulan(tahunSampai);
										tagihan.setBulan(null);
										tagihan.setTahun(tahunSampai);
										tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
										tagihan.setCalonSiswa(calonSiswa);
										tagihan.setItemBiayaSekolah(pengaturanBiayaItemBiaya.getItemBiayaSekolah());
										tagihan.setBayarKe(bayarKe);
										tagihan.setNominal(n);

										Transaction tx = session.beginTransaction();
										session.save(tagihan);
										tx.commit();

										if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
											pembayaranSiswaDetail.setTagihan(tagihan);
											Transaction tx2 = session.beginTransaction();
											session.update(pembayaranSiswaDetail);
											tx2.commit();

											if (TagihanDiskonSiswaHelper.diskonTidakMemotongTagihan(tagihan)) {
												DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
											}
										}
										tagihans.add(tagihan);
									}
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							} else {
								tagihans.add(tagihan);
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:285");
		} finally {
			if (!sessionSudahTerbukaSebelumnya) {
				closeSessionAndDisconnect(session);
			}
		}

		return saring(tagihans);
	}

	public static List<Tagihan> doGenerateTagihanInsendentil(CalonSiswa calonSiswa, JenisBiayaSekolah jenisBiayaSekolah,
			boolean refresh) {

		List<Tagihan> tagihans = new ArrayList<Tagihan>();
		Session session = null;

		try {
			session = HibernateUtil.currentSession();
			Criteria crit = PengaturanBiaya
					.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), null, calonSiswa)
					.add(Restrictions.eq("jenisBiayaSekolah", jenisBiayaSekolah)).addOrder(Order.desc("id"));

			List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(crit, PengaturanBiaya.class);

			for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
				if (Tagihan.getBoleh(pengaturanBiaya, null, calonSiswa)) {
					List<Tagihan> subTagihans = doGenerateTagihanInsendentil(calonSiswa, pengaturanBiaya, refresh);
					tagihans.addAll(subTagihans);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:314");
		}
		// Session tidak ditutup secara eksplisit di sini karena currentSession()
		// dikelola Hibernate context
		return tagihans;
	}

	public static List<Tagihan> doGenerateTagihanInsendentil(CalonSiswa calonSiswa, PengaturanBiaya pengaturanBiaya,
			boolean refresh) {
		List<Tagihan> tagihans = new ArrayList<Tagihan>();

		if (pengaturanBiaya != null && pengaturanBiaya.getTahunAngkatan() != 0 && calonSiswa.getTahunMasuk() != null
				&& !pengaturanBiaya.getTahunAngkatan().equals(calonSiswa.getTahunMasuk())) {
			return saring(tagihans);
		}

		Session session = null;
		// PERBAIKAN (session closed): lihat catatan sama di doGenerateTagihanBulanan() -- jangan tutup
		// session ancestor yang dipinjam via currentNativeSession() dari thread yang sama.
		boolean sessionSudahTerbukaSebelumnya = HibernateUtil.isNativeSessionOpenForCurrentThread();
		try {
			session = HibernateUtil.currentNativeSession();
			List<PengaturanBiayaItemBiaya> daftarBiayas = ConstantValues
					.simpleList(
							session.createCriteria(PengaturanBiayaItemBiaya.class)
									.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
							PengaturanBiayaItemBiaya.class);

			for (PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya : daftarBiayas) {
				try {
					NominalBiaya nominalBiayaCalonSiswa = ambilNominalBiaya(pengaturanBiayaItemBiaya, calonSiswa,
							session);

					if (Tagihan.getBoleh(pengaturanBiaya, null, calonSiswa,
							pengaturanBiayaItemBiaya.getItemBiayaSekolah())
							&& nominalBiayaCalonSiswa != null
							&& ((nominalBiayaCalonSiswa.getNominal() != null && nominalBiayaCalonSiswa.getNominal() > 0)
									|| (nominalBiayaCalonSiswa.getItemBiayaSekolah() != null && nominalBiayaCalonSiswa
											.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()))) {

						Integer tahunbulan = nominalBiayaCalonSiswa.getTahunbulan() != null
								? nominalBiayaCalonSiswa.getTahunbulan()
								: PembayaranSiswa.convert(
										nominalBiayaCalonSiswa.getPengaturanBiaya().getJenisBiayaSekolah()
												.getUntukTahun(),
										nominalBiayaCalonSiswa.getPengaturanBiaya().getJenisBiayaSekolah()
												.getUntukBulan());

						int batasBayar = nominalBiayaCalonSiswa.getDibayarSebayak() != null
								? nominalBiayaCalonSiswa.getDibayarSebayak()
								: 1;

						for (int bayarKe = 1; bayarKe <= batasBayar; bayarKe++) {
							Tagihan tagihan = Tagihan.ambilAtauBuat(session,
									nominalBiayaCalonSiswa.getItemBiayaSekolah(),
									nominalBiayaCalonSiswa.getPengaturanBiaya(), nominalBiayaCalonSiswa.getSiswa(),
									nominalBiayaCalonSiswa.getCalonSiswa(), bayarKe, nominalBiayaCalonSiswa, tahunbulan,
									refresh);

							if (tagihan != null && tagihan.getId() != null) {
								tagihans.add(tagihan);
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:380");
		} finally {
			if (!sessionSudahTerbukaSebelumnya) {
				closeSessionAndDisconnect(session);
			}
		}

		return saring(tagihans);
	}

	public static void doSinkronkanTagihanCalonSiswa(PengaturanBiaya pengaturanBiaya, Label label, Textbox nama,
			boolean refresh) {
		label.setValue("Singkronisasi data tagihan");
		Session session = null;

		try {
			List<CalonSiswa> calonSiswaInstanceList = new ArrayList<CalonSiswa>();
			try {
				session = HibernateUtil.currentNativeSession();
				Criteria criteria = DetailTagihanCalonSiswaHelper.initCriteria(session, pengaturanBiaya, null, nama,
						null, false, true);
				calonSiswaInstanceList = ConstantValues.simpleList(criteria, CalonSiswa.class);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:401");
			} finally {
				closeSessionAndDisconnect(session);
			}

			int size = calonSiswaInstanceList.size();
			int indexKe = 0;

			// =========================================================================
			// MEMORY OPTIMIZATION: BATCH PROCESSING
			// Membuka session SATU KALI untuk memproses seluruh data di dalam loop,
			// kemudian memanggil clear() dan flush() secara berkala agar
			// memory tidak bocor / menumpuk (L1 Cache overflow).
			// =========================================================================
			Session batchSession = null;
			try {
				batchSession = HibernateUtil.currentNativeSession();

				String periode = pengaturanBiaya.getJenisBiayaSekolah() != null
						? pengaturanBiaya.getJenisBiayaSekolah().getPeriode()
						: "";

				if ("Bulanan".equals(periode)) {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 8);
					int tahunSampai = calendar.get(Calendar.YEAR);
					int bulanSampai = calendar.get(Calendar.MONTH);

					for (CalonSiswa calonSiswa : calonSiswaInstanceList) {
						label.setValue(
								"Memproses data tagihan " + calonSiswa.getNomorInduk() + " " + calonSiswa.getNama()
										+ " (" + Common.numberFormat.get().format((++indexKe) * 100.0 / size) + "%)");

						List<Tagihan> tags = doGenerateTagihanBulanan(calonSiswa, pengaturanBiaya, tahunSampai,
								bulanSampai, refresh);
						for (Tagihan tagihan : tags) {
							batchSession.update(tagihan);
						}

						// Bebaskan memory secara berkala setiap 20 siswa
						if (indexKe % 20 == 0) {
							batchSession.flush();
							batchSession.clear();
						}
					}
				} else if ("Tahunan".equals(periode)) {
					int tahunSampai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
					for (CalonSiswa calonSiswa : calonSiswaInstanceList) {
						label.setValue(
								"Memproses data tagihan " + calonSiswa.getNomorInduk() + " " + calonSiswa.getNama()
										+ " (" + Common.numberFormat.get().format((++indexKe) * 100.0 / size) + "%)");

						List<Tagihan> tags = doGenerateTagihanTahunan(calonSiswa, pengaturanBiaya, tahunSampai);
						for (Tagihan tagihan : tags) {
							batchSession.update(tagihan);
						}

						if (indexKe % 20 == 0) {
							batchSession.flush();
							batchSession.clear();
						}
					}
				} else {
					for (CalonSiswa calonSiswa : calonSiswaInstanceList) {
						label.setValue(
								"Memproses data tagihan " + calonSiswa.getNomorInduk() + " " + calonSiswa.getNama()
										+ " (" + Common.numberFormat.get().format((++indexKe) * 100.0 / size) + "%)");

						List<Tagihan> tags = doGenerateTagihanInsendentil(calonSiswa, pengaturanBiaya, refresh);
						for (Tagihan tagihan : tags) {
							batchSession.update(tagihan);
						}

						if (indexKe % 20 == 0) {
							batchSession.flush();
							batchSession.clear();
						}
					}
				}

				Transaction finalTx = batchSession.beginTransaction();
				batchSession.flush();
				finalTx.commit();

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:486");
			} finally {
				closeSessionAndDisconnect(batchSession);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:491");
		} finally {
			// fallback aman jika terjadi error sblm session pertama ditutup
			if (session != null && session.isOpen()) {
				closeSessionAndDisconnect(session);
			}
		}
		label.setValue("");
	}

	public static NominalBiaya ambilNominalBiaya(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya,
			CalonSiswa calonSiswa, Session session) {
		return ambilNominalBiaya(pengaturanBiayaItemBiaya, calonSiswa, null, session);
	}

	public static NominalBiaya ambilNominalBiaya(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya,
			CalonSiswa calonSiswa, Integer tahunbulan, Session session) {

		String kodeUnik = NominalBiaya.genCode(pengaturanBiayaItemBiaya.getItemBiayaSekolah(),
				pengaturanBiayaItemBiaya.getPengaturanBiaya(), null, calonSiswa, tahunbulan);

		NominalBiaya nominalBiayaCalonSiswa = (NominalBiaya) session.createCriteria(NominalBiaya.class)
				.add(Restrictions.eq("kodeUnik", kodeUnik)).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

		if (nominalBiayaCalonSiswa == null) {
			Criteria crit = session.createCriteria(NominalBiaya.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiayaItemBiaya.getPengaturanBiaya()))
					.add(Restrictions.eq("itemBiayaSekolah", pengaturanBiayaItemBiaya.getItemBiayaSekolah()));

			if (tahunbulan == null || tahunbulan < 2100) {
				crit.add(Restrictions.sqlRestriction("true"));
			} else {
				crit.add(Restrictions.eq("tahunbulan", tahunbulan));
			}

			nominalBiayaCalonSiswa = (NominalBiaya) crit.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1)
					.uniqueResult();
		}

		// Step 3 (last resort): cari NominalBiaya APAPUN untuk kombinasi
		// (pengaturanBiaya, itemBiayaSekolah, calonSiswa) tanpa filter tahunbulan/status.
		// Satu kombinasi PB+item+calonSiswa = satu NominalBiaya; reuse record lama
		// meskipun bukanTagihan=true, aktif=false, atau tahunbulan berbeda.
		if (nominalBiayaCalonSiswa == null) {
			nominalBiayaCalonSiswa = (NominalBiaya) session.createCriteria(NominalBiaya.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiayaItemBiaya.getPengaturanBiaya()))
					.add(Restrictions.eq("itemBiayaSekolah", pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
					.add(Restrictions.eq("calonSiswa", calonSiswa))
					.addOrder(Order.asc("id"))
					.setMaxResults(1).uniqueResult();
			if (nominalBiayaCalonSiswa != null) {
				System.out.println("[TagihanUtilCalonSiswa][ambilNominalBiaya] step3-reuse NominalBiaya id="
						+ nominalBiayaCalonSiswa.getId() + " bukanTagihan=" + nominalBiayaCalonSiswa.getBukanTagihan()
						+ " tahunbulan=" + nominalBiayaCalonSiswa.getTahunbulan()
						+ " (search-tahunbulan=" + tahunbulan + " kodeUnik=" + kodeUnik + ")");
			}
		}

		if (nominalBiayaCalonSiswa == null) {
			System.out.println("[TagihanUtilCalonSiswa][ambilNominalBiaya] creating NEW NominalBiaya"
					+ " kodeUnik=" + kodeUnik + " tahunbulan=" + tahunbulan
					+ " calonSiswa=" + (calonSiswa != null ? calonSiswa.getId() : null)
					+ " pb=" + (pengaturanBiayaItemBiaya.getPengaturanBiaya() != null
							? pengaturanBiayaItemBiaya.getPengaturanBiaya().getId() : null));
			nominalBiayaCalonSiswa = new NominalBiaya();
			nominalBiayaCalonSiswa.setTahunbulan(tahunbulan);
			nominalBiayaCalonSiswa.setNominal(pengaturanBiayaItemBiaya.getDefaultBiaya());
			nominalBiayaCalonSiswa.setItemBiayaSekolah(pengaturanBiayaItemBiaya.getItemBiayaSekolah());
			nominalBiayaCalonSiswa.setPengaturanBiaya(pengaturanBiayaItemBiaya.getPengaturanBiaya());
			nominalBiayaCalonSiswa.setCalonSiswa(calonSiswa);
			nominalBiayaCalonSiswa.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
			Transaction tx = session.beginTransaction();
			session.save(nominalBiayaCalonSiswa);
			tx.commit();
		}

		if (nominalBiayaCalonSiswa.getPengaturanBiayaItemBiaya() == null) {
			nominalBiayaCalonSiswa.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
			Transaction tx = session.beginTransaction();
			session.update(nominalBiayaCalonSiswa);
			tx.commit();
		}

		return ais.common.EntityIdentityMap.canonical(nominalBiayaCalonSiswa);
	}

	public static List<Tagihan> getTagihan(JenisBiayaSekolah jenisBiaya, PengaturanBiaya pengaturanData, CalonSiswa s,
			Integer bulan, Integer tahun, boolean refresh) {
		return getTagihan(jenisBiaya, pengaturanData, s, bulan, tahun, false, refresh);
	}

	public static List<Tagihan> getTagihan(JenisBiayaSekolah jenisBiaya, PengaturanBiaya pengaturanData, CalonSiswa s,
			Integer bulan, Integer tahun, Boolean semua, boolean refresh) {

		List<PengaturanBiaya> list = new ArrayList<PengaturanBiaya>();
		if (pengaturanData != null) {
			list.add(pengaturanData);
		}

		// Memanggil metode utama yang menggunakan parameter List
		return getTagihan(jenisBiaya, list, s, bulan, tahun, semua, refresh);
	}

	@SuppressWarnings("unchecked")
	public static List<Tagihan> getTagihan(JenisBiayaSekolah jenisBiaya, List<PengaturanBiaya> pengaturanDatas,
			CalonSiswa s, Integer bulan, Integer tahun, Boolean semua, boolean refresh) {

		// Validasi dasar
		if (jenisBiaya == null || s == null) {
			return new ArrayList<Tagihan>();
		}

		try {
			Integer tahunbulan = PembayaranSiswa.convert(tahun, bulan);
			Session session = null;
			List<Tagihan> tagihans = new ArrayList<Tagihan>();
			List<NominalBiaya> nominalsTagihan = new ArrayList<NominalBiaya>();

			try {
				session = HibernateUtil.openSession();
				Criteria criteria = session.createCriteria(Tagihan.class)
						.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
						.add(Restrictions.eq("itemBiayaSekolah.aktif", true))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("calonSiswa", s));

				// Jika daftar pengaturan biaya disediakan, gunakan operator IN
				if (pengaturanDatas != null && !pengaturanDatas.isEmpty()) {
					criteria.add(Restrictions.in("pengaturanBiaya", pengaturanDatas));
				} else {
					// Logika pencarian pengaturan biaya default jika parameter bernilai null/kosong
					criteria.createAlias("pengaturanBiaya", "pengaturanBiaya")
							.addOrder(Order.desc("pengaturanBiaya.id"))
							.add(s.getPenjurusanSekolah() == null
									? Restrictions.isNull("pengaturanBiaya.penjurusanSekolah")
									: Restrictions.or(Restrictions.isNull("pengaturanBiaya.penjurusanSekolah"),
											Restrictions.eq("pengaturanBiaya.penjurusanSekolah",
													s.getPenjurusanSekolah())))
							.add(Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiaya))
							.add(Restrictions.eq("pengaturanBiaya.sekolah", s.getSekolah()))
							.add(Restrictions.eq("pengaturanBiaya.statusAwalSiswa", s.getStatusAwalSiswa()));
				}

				criteria.add(Restrictions.or(Restrictions.gt("nominal", 0.1),
						Restrictions.eq("itemBiayaSekolah.nilaiBiayaBisaDiubahSaatPembayaran", true)))
						.addOrder(Order.asc("itemBiayaSekolah.nama")).addOrder(Order.asc("tahunbulan"))
						.addOrder(Order.asc("bayarKe"));

				if ("Bulanan".equals(jenisBiaya.getPeriode())) {
					criteria.add(Restrictions.le("tahunbulan", tahunbulan));
				} else if ("Tahunan".equals(jenisBiaya.getPeriode())) {
					criteria.add(Restrictions.le("tahun", tahun));
				}

				criteria.add(Restrictions.or(
						Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
								Restrictions.sqlRestriction("this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
						semua ? Restrictions.sqlRestriction("true") : Restrictions.isNull("pembayaranSiswaDetail")));

				tagihans = criteria.list();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:652");
			} finally {
				closeSessionAndDisconnect(session);
			}

			// Cek apakah ada nominal yang sudah terbentuk
			for (Tagihan tagihan : tagihans) {
				boolean bisaDiubah = tagihan.getNominalBiaya().getItemBiayaSekolah()
						.getNilaiBiayaBisaDiubahSaatPembayaran();
				boolean adaNominal = tagihan.getNominal() != null && tagihan.getNominal() > 0.1;

				if ("Bulanan".equals(jenisBiaya.getPeriode())) {
					if (tahunbulan.equals(tagihan.getTahunbulan()) && (bisaDiubah || adaNominal)) {
						nominalsTagihan.add(tagihan.getNominalBiaya());
					}
				} else if ("Tahunan".equals(jenisBiaya.getPeriode())) {
					if (tahun.equals(tagihan.getTahun()) && (bisaDiubah || adaNominal)) {
						nominalsTagihan.add(tagihan.getNominalBiaya());
					}
				} else {
					if (bisaDiubah || adaNominal) {
						nominalsTagihan.add(tagihan.getNominalBiaya());
					}
				}
			}

			// Jika tidak ditemukan tagihan, lakukan generate otomatis
			if (nominalsTagihan.isEmpty()) {
				Session innerSession = null;
				try {
					innerSession = HibernateUtil.openSession();

					List<PengaturanBiaya> listUntukGen = new ArrayList<PengaturanBiaya>();
					if (pengaturanDatas != null && !pengaturanDatas.isEmpty()) {
						listUntukGen.addAll(pengaturanDatas);
					} else {
						// Cari pengaturan biaya default
						PengaturanBiaya pbDefault = (PengaturanBiaya) innerSession.createCriteria(PengaturanBiaya.class)
								.add(s.getPenjurusanSekolah() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.isNull("penjurusanSekolah"),
												Restrictions.eq("penjurusanSekolah", s.getPenjurusanSekolah())))
								.add(Restrictions.or(Restrictions.isNull("statusAwalSiswa"),
										Restrictions.eq("statusAwalSiswa", s.getStatusAwalSiswa())))
								.add(Restrictions.eq("jenisBiayaSekolah", jenisBiaya))
								.add(Restrictions.eq("sekolah", s.getSekolah()))
								.add(Restrictions.or(Restrictions.eq("tahunAngkatan", 0),
										Restrictions.eq("tahunAngkatan", s.getTahunMasuk())))
								.setMaxResults(1).uniqueResult();
						if (pbDefault != null)
							listUntukGen.add(pbDefault);
					}

					for (PengaturanBiaya pb : listUntukGen) {
						if (Tagihan.getBoleh(pb, null, s)) {
							if ("Bulanan".equals(jenisBiaya.getPeriode())) {
								tagihans.addAll(doGenerateTagihanBulanan(s, pb, tahun, bulan, refresh));
							} else if ("Tahunan".equals(jenisBiaya.getPeriode())) {
								tagihans.addAll(doGenerateTagihanTahunan(s, pb, tahun));
							} else {
								tagihans.addAll(doGenerateTagihanInsendentil(s, pb, refresh));
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					closeSessionAndDisconnect(innerSession);
				}
			}

			boolean tagihanDIbuatOtomatisMenghitungSisa = Common.bolehKonfigurasi("tagihan_dibuat_otomatis_menghitung_sisa", Konfigurasi.TIDAK_AKTIF);

			if (tagihanDIbuatOtomatisMenghitungSisa && tagihans != null && !tagihans.isEmpty()) {
				// ===================================================================================
				// FITUR BARU: AUTO-BALANCING MENGGUNAKAN OOP (nb.ambilTagihans)
				// Hanya menambahkan Tagihan baru (Shortfall) ke dalam List eksisting.
				// ===================================================================================
				java.util.Set<Long> processedNbIds = new java.util.HashSet<Long>();
				java.util.Set<Long> existingTagihanIds = new java.util.HashSet<Long>();
				List<Tagihan> tagihanBaruDitambahkan = new ArrayList<Tagihan>();

				// 1. Kumpulkan semua ID Tagihan yang sudah eksisting di list awal
				for (Tagihan t : tagihans) {
					if (t != null && t.getId() != null) {
						existingTagihanIds.add(t.getId());
					}
				}

				// Gunakan perulangan index-based atau foreach yang aman
				for (int i = 0; i < tagihans.size(); i++) {
					Tagihan t = tagihans.get(i);
					if (t == null) continue;

					NominalBiaya nb = null;
					try {
						nb = t.getNominalBiaya();
					} catch (Exception e) {
						nb = null; // Failsafe untuk LazyInitialization / Session putus
					}

					if (nb != null && nb.getId() != null) {
						// Cegah eksekusi berulang untuk NominalBiaya yang sama
						if (!processedNbIds.contains(nb.getId())) {
							processedNbIds.add(nb.getId());

							// Panggil method ajaib OOP dari NominalBiaya
							List<Tagihan> tgs = nb.ambilTagihans();
							if (tgs != null && !tgs.isEmpty()) {
								// Filter: Hanya ambil Tagihan yang belum ada di list awal
								for (Tagihan tg : tgs) {
									if (tg != null) {
										// Cek apakah ini Tagihan baru (ID belum terdaftar, atau ID masih null karena baru dicreate)
										if (tg.getId() == null || !existingTagihanIds.contains(tg.getId())) {
											if (!tagihanBaruDitambahkan.contains(tg)) {
												tagihanBaruDitambahkan.add(tg);
												if (tg.getId() != null) {
													existingTagihanIds.add(tg.getId()); // Tandai agar tidak double
												}
											}
										}
									}
								}
							}
						}
					}
				}

				// 2. Jika terdapat penambahan Tagihan baru, masukkan ke list aslinya
				if (!tagihanBaruDitambahkan.isEmpty()) {
					tagihans.addAll(tagihanBaruDitambahkan);
				}
				// ===================================================================================
			}

			List<Tagihan> tagihanbaru = new ArrayList<Tagihan>();
			for (Tagihan tagihan : tagihans) {
				NominalBiaya nb = tagihan.getNominalBiaya();
				if (nb != null && tagihan.getBayarKe() <= nb.getDibayarSebayak()) {
					try {
						if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Bulanan")) {
							if (tagihan.getTahunbulan() == null) {
								continue;
							}
							Integer bMulai = nb.getPengaturanBiaya().getBulanMulai();
							Integer bSampai = nb.getPengaturanBiaya().getBulanSampai();

							if (bMulai != null && tagihan.getTahunbulan() < bMulai)
								continue;
							if (bSampai != null && tagihan.getTahunbulan() > bSampai)
								break;
						}
					}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:803");
						// TODO: handle exception
					}

					boolean bisaUbah = nb.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran();
					if (tagihan.getPembayaranSiswaDetail() == null) {
						if (bisaUbah || tagihan.getNominal() > 0.1) {
							tagihanbaru.add(tagihan);
						}
					} else {
						if (bisaUbah && tagihan.getNominal().intValue() == 0) {
							tagihanbaru.add(tagihan);
						}
					}
				}
			}

			// Sinkronisasi diskon tagihan dipindahkan ke helper reusable.
			// Helper ini memakai session terisolasi dan tidak memakai ConstantValues.simpleList(),
			// sehingga DiskonSiswa tidak menjadi proxy detach saat dibaca dari background thread.
			TagihanDiskonSiswaHelper.sinkronkanDiskon(tagihanbaru);

			return saring(tagihanbaru);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<Tagihan>();
		}
	}

	public static List<Tagihan> saring(List<Tagihan> tagihans) {
		return saring(tagihans, null);
	}

	public static List<Tagihan> saring(List<Tagihan> tagihans, Session sessiontemp) {
		List<Tagihan> tagihansHasil = new ArrayList<Tagihan>();
		java.util.Set<String> keys = new java.util.HashSet<String>();

		for (Tagihan tagihan : tagihans) {
			String key = tagihan.getKodeUnik();
			if (keys.contains(key)) {
				try {
					if (tagihan.getAktif() != null && tagihan.getAktif()) {
						if (sessiontemp == null) {
							Session session = null;
							try {
								session = HibernateUtil.openSession();
								session.refresh(tagihan);
								tagihan.setAktif(false);
								session.getTransaction().begin();
								Common.refreshUpdate(session, tagihan);
								session.getTransaction().commit();
							} catch (Exception e) {
								if (session != null && session.getTransaction() != null
										&& session.getTransaction().isActive()) {
									session.getTransaction().rollback();
								}
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:859");
							} finally {
								closeSessionAndDisconnect(session);
							}
						} else {
							sessiontemp.refresh(tagihan);
							tagihan.setAktif(false);
							sessiontemp.getTransaction().begin();
							Common.refreshUpdate(sessiontemp, tagihan);
							sessiontemp.getTransaction().commit();
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:872");
				}
			} else if (tagihan.getAktif() != null && tagihan.getAktif()) {
				tagihansHasil.add(tagihan);
				keys.add(key);
			}
		}
		tagihans.clear();
		return tagihansHasil;
	}

	public static void tampilkanKunci(Vbox vbox1, final Tagihan tagihan, final EventListener refrsh,
			final Tbmuser tbmuser, final MyDoublebox doublebox, final boolean byr) {
		// Overload kompatibilitas: default HORIZONTAL (Hbox) — perilaku lama, dipakai pemanggil lain.
		tampilkanKunci(vbox1, tagihan, refrsh, tbmuser, doublebox, byr, false);
	}

	/**
	 * Varian dengan opsi {@code vertikal}: bila {@code true}, tombol Kunci/Buka-Kunci/Bayar ditumpuk
	 * ke bawah (Vbox) — dipakai pada tampilan tagihan siswa yang dirancang vertikal agar sel tidak
	 * sempit. Bila {@code false}, tetap berdampingan (Hbox) seperti semula (kompatibel pemanggil lama).
	 */
	public static void tampilkanKunci(Vbox vbox1, final Tagihan tagihan, final EventListener refrsh,
			final Tbmuser tbmuser, final MyDoublebox doublebox, final boolean byr, final boolean vertikal) {
		if (tagihan != null && tagihan.getPengaturanBiaya() != null && tagihan.getPengaturanBiaya().getKunci() == null
				&& tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {

			final Toolbarbutton bukaKunciDetail = new ais.ui.util.MyToolbarbuttonConfig(
					tagihan.getKunci() == null ? "" : tagihan.getKunci().getUserNama(), "/img/svg/unlock.svg");
			final Toolbarbutton kunciDetail = new ais.ui.util.MyToolbarbuttonConfig(
					tagihan.getKunci() == null ? "Kunci" : tagihan.getKunci().getUserNama(), "/img/svg/lock.svg");
			final Toolbarbutton bayar = new ais.ui.util.MyToolbarbuttonConfig("Bayar", "/img/svg/money-bills.svg");

			org.zkoss.zul.Box vbox;
			if (vertikal) {
				vbox = new Vbox();
			} else {
				vbox = new Hbox();
			}
			vbox.setParent(vbox1);

			bukaKunciDetail.setParent(vbox);
			kunciDetail.setParent(vbox);

			if (byr) {
				bayar.setParent(vbox);
			}

			bukaKunciDetail.setStyle("font-size:7px;");
			kunciDetail.setStyle("font-size:7px;");
			bayar.setStyle("font-size:7px;");

			kunciDetail.setTooltiptext("Klik untuk meng-kunci tagihan ini");

			if (tagihan.getKunci() != null) {
				bukaKunciDetail.setTooltiptext(
						"Dikunci oleh " + tagihan.getKunci().getUserId() + ", klik untuk membuka kunci");
			} else {
				bukaKunciDetail.setTooltiptext("klik untuk membuka kunci");
			}

			kunciDetail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event event1) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mengunci tagihan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										Session session = null;
										org.hibernate.Transaction tx = null;

										try {
											// 1. Buka sesi baru yang aman dan independen
											session = HibernateUtil.getSessionFactory().openSession();

											// 2. Proteksi NullPointerException (Safety Check)
											if (tagihan != null && tagihan.getId() != null) {

												Tagihan tagg = (Tagihan) session.createCriteria(Tagihan.class)
														.add(Restrictions.idEq(tagihan.getId())).uniqueResult();

												if (tagg == null) {
													tagg = DetailTagihanSiswaHelper.butTagihanBaru(tagihan, session,
															tagihan.getBayarKe(), tagihan.getNominalBiaya(),
															tagihan.getBulan(), tagihan.getTahun(),
															new ArrayList<Long>(), tagihan.getPengaturanBiaya(), true);
												}

												if (tagg != null) {
													// 3. Eksekusi Mutasi Database dalam Blok Transaksi (Mencegah
													// Deadlock)
													tx = session.beginTransaction();

													Double nilaiBiaya = doublebox.getValue() != null
															? doublebox.getValue()
															: 0.0;
													tagg.setBiayaTemporary(nilaiBiaya);
													tagg.setKunci(tbmuser);

													Common.refreshUpdate(session, tagg);

													tx.commit();

													// 4. Update UI setelah transaksi dipastikan sukses
													if (refrsh != null) {
														refrsh.onEvent(event1);
													}
												}
											}
										} catch (Exception e) {
											// 5. Auto-rollback jika terjadi error di tengah proses mutasi
											if (tx != null && tx.isActive()) {
												try {
													tx.rollback();
												} catch (Exception exRollback) { ais.common.ErrorAuditUtil.record(exRollback, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:988");
												}
											}
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:991");
										} finally {
											// 6. Penutupan sesi super ketat untuk mencegah memory/connection leak
											if (session != null && session.isOpen()) {
												try {
													session.clear();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:997");
												}
												try {
													session.disconnect();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:1001");
												}
												try {
													session.close();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:1005");
												}
											}
										}
									}
								}
							});
				}
			});
			kunciDetail.setVisible(tagihan.getKunci() == null && !tagihan.ambilBukanTagihanData());
			kunciDetail.setOrient("vertical");

			bukaKunciDetail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event event1) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin membuka kunci tagihan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										tagihan.setKunci(null);
										Common.refreshUpdate(tagihan);
										refrsh.onEvent(event1);
									}
								}
							});
				}
			});

			bukaKunciDetail.setVisible(tagihan.getKunci() != null);
			if (tagihan.getKunci() != null) {
				bukaKunciDetail.setTooltiptext("Dikunci oleh " + tagihan.getKunci().getUserId());
			}

			bukaKunciDetail.setOrient("vertical");
			kunciDetail.setOrient("vertical");
			bayar.setOrient("vertical");

			bukaKunciDetail.setVisible(tagihan.getKunci() != null && !tagihan.ambilBukanTagihanData());
			bukaKunciDetail.setDisabled(tbmuser == null || tagihan.getKunci() == null
					|| !tagihan.getKunci().getUserId().equals(tbmuser.getUserId()));

			kunciDetail.setVisible(tagihan.getKunci() == null && !tagihan.ambilBukanTagihanData());
			if (Common.getApakahAdmin()) {
				kunciDetail.setDisabled(false);
			}

			bayar.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event event1) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin membayarkan tagihan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										if (tagihan.getSiswa() != null) {
											Common.displayWindow(
													"/pages/master/sekolah/pem_online.zul?lbl_siswa=true&tagihans="
															+ tagihan.getId() + "&siswa=" + tagihan.getSiswa().getId()
															+ "&pilihBulan=" + tagihan.getBulan() + "&pilihTahun="
															+ tagihan.getTahun(),
													true, "95%", Common.isMobile() ? "100%" : "90%", refrsh, "", false);
										} else if (tagihan.getCalonSiswa() != null) {
											Common.displayWindow(
													"/pages/master/sekolah/pem_online.zul?lbl_calon_siswa=true&tagihans="
															+ tagihan.getId() + "&calon_siswa="
															+ tagihan.getCalonSiswa().getId() + "&pilihBulan="
															+ tagihan.getBulan() + "&pilihTahun=" + tagihan.getTahun(),
													true, "95%", Common.isMobile() ? "100%" : "90%", refrsh, "", false);
										}
									}
								}
							});
				}
			});
		}
	}

	/**
	 * Utility method untuk menutup session agar tidak redundan dan memastikan
	 * null-check dan session tertutup rapat
	 */
	private static void closeSessionAndDisconnect(Session session) {
		if (session != null && session.isOpen()) {
			try {
				session.disconnect();
				session.close();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtilCalonSiswa.java:1095");
			}
		}
		HibernateUtil.closeSession();
	}
}
