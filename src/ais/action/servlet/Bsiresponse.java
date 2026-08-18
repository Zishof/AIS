package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import com.bni.encrypt.BNIHash;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.LogHostToHost;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.bsi.BsiRequestDetail;
import ais.database.model.bsi.BsiResponse;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;

/**
 * Servlet implementation class CheckISBN
 */
public class Bsiresponse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Bsiresponse() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	private static boolean isRequestSudahDiproses(BsiRequest bsiRequest, Session session) {
		if (bsiRequest == null || session == null) {
			return false;
		}
		try {
			if (bsiRequest.getSiswa() != null || bsiRequest.getCalonSiswa() != null) {
				Number jumlahPembayaran = (Number) session.createCriteria(PembayaranSiswa.class)
						.add(Restrictions.eq("bsiRequest", bsiRequest))
						.setProjection(Projections.rowCount()).uniqueResult();
				if (jumlahPembayaran != null && jumlahPembayaran.intValue() > 0) {
					return true;
				}
			}

			Number jumlahLog = (Number) session.createCriteria(LogPembayaran.class)
					.add(Restrictions.eq("bsiRequest", bsiRequest))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (jumlahLog != null && jumlahLog.intValue() > 0) {
				return true;
			}

			Collection temporarys = bsiRequest.getKegiatanTemporarys();
			if (temporarys != null && !temporarys.isEmpty()) {
				boolean semuaSudahTerhubungKeKegiatan = true;
				for (Object object : temporarys) {
					KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) object;
					if (kegiatanTemporary == null || kegiatanTemporary.getKegiatan() == null
							|| kegiatanTemporary.getKegiatan().getId() == null) {
						semuaSudahTerhubungKeKegiatan = false;
						break;
					}
				}
				if (semuaSudahTerhubungKeKegiatan) {
					return true;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	public static Kegiatan createKegiatan(BsiRequest bsiRequest, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = bsiRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = bsiRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = bsiRequest.getBiodataCalonMahasiswa();

		Integer semester = bsiRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = bsiRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = bsiRequest.getJenisKegiatan();
		if (mhs != null) {
			kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
		} else if (bio != null) {
			kegiatan = bio.ambilKegiatans(semester, jenisKegiatan);
			if (kegiatan == null && semester <= 1) {
				kegiatan = bio.ambilKegiatans(jenisKegiatan);
			}
		}

		System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
				+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
				+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
				+ bsiRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ bsiRequest.getHapusCicilanSebelumnya());

		if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = new Kegiatan();
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setMahasiswa(mhs);
			kegiatan.setCalonMahasiswa(bio);
			kegiatan.setSemster(semester);
			if (mhs != null) {
				kegiatan.setStatusMahasiswa(Common
						.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster()).getStatusMahasiswa());
			} else if (bio != null) {
				kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			}
			kegiatan.setTahunAkademik(bsiRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(Common.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());
			kegiatan.setPengurangan(bsiRequest.getPengurangan());
			kegiatan.setKeterangan(bsiRequest.getKeterangan());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}

		return kegiatan;
	}

	@SuppressWarnings("unchecked")
	public static BsiRequest prosesResponse(BsiResponse bsiResponse, boolean hapusDulu) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		BsiRequest bsiRequest = (BsiRequest) session.createCriteria(BsiRequest.class)
				.add(Restrictions.eq("trxId", bsiResponse.getTrxId())).setMaxResults(1).uniqueResult();

		Date tanggalTransaksi = null;
		try {
			JSONObject jsonObject = new JSONObject(bsiResponse.getKeterangan());
			tanggalTransaksi = Common.databaseDateFormat1.get().parse(jsonObject.getString("datetime_payment"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:217");
			// TODO: handle exception
		}

		System.out.println("bsiResponse => " + bsiResponse + ", tanggalTransaksi => "
				+ Common.dateFormat3.get().format(tanggalTransaksi));

		if (bsiRequest != null && bsiResponse.getKodeStatus().toString().trim().equalsIgnoreCase("000")) {

			if (isRequestSudahDiproses(bsiRequest, session)) {
				System.out.println("Callback bsiRequest sudah pernah diproses, proses pembayaran dilewati: " + bsiRequest);
				return bsiRequest;
			}

			bsiRequest.setBsiResponse(bsiResponse);
			bsiRequest.setStatus("Payment Sukses");
			bsiRequest.setKodeStatus(bsiResponse.getKodeStatus());
			session.getTransaction().begin();
			session.update(bsiRequest);
			session.getTransaction().commit();

			String kodeAkun = Common.getKonfigurasi("kode_akun_bsi", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);

			if (!bsiRequest.getKegiatanTemporarys().isEmpty()) {
				Kegiatan kegiatan = null;
				for (KegiatanTemporary temporary : bsiRequest.getKegiatanTemporarys()) {
					Session sessionLocalKeg = null;
					try {
					sessionLocalKeg = HibernateUtil.getSessionFactory().openSession();
						KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) sessionLocalKeg
								.createCriteria(KegiatanTemporary.class).add(Restrictions.idEq(temporary.getId()))
								.uniqueResult();
						if (kegiatanTemporary != null) {

							Mahasiswa mahasiswa = kegiatanTemporary.getMahasiswa();
							Integer smt = kegiatanTemporary.getSemster();
							JenisKegiatan jenisKegiatan = kegiatanTemporary.getJenisKegiatan();

							kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan);
							if (kegiatan == null || kegiatan.getId() == null) {
								kegiatan = new Kegiatan();
							} else {
								kegiatan = (Kegiatan) sessionLocalKeg.createCriteria(Kegiatan.class)
										.add(Restrictions.idEq(kegiatan.getId())).uniqueResult();
							}

							kegiatan.setJenisKegiatan(jenisKegiatan);
							kegiatan.setJadwalPembayaran(kegiatanTemporary.getJadwalPembayaran());
							kegiatan.setMahasiswa(mahasiswa);
							kegiatan.setSemster(smt);
							kegiatan.setStatusMahasiswa(kegiatanTemporary.getStatusMahasiswa());
							kegiatan.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
							kegiatan.setTanggal(
									tanggalTransaksi == null ? kegiatanTemporary.getTanggal() : tanggalTransaksi);
							kegiatan.setValidated(1);
							kegiatan.setJenisKegiatan(jenisKegiatan);
							kegiatan.setValidator(Common
									.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());
							kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

							sessionLocalKeg.getTransaction().begin();
							Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
							sessionLocalKeg.getTransaction().commit();

							List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.eq("kegiatanTemporary.id", temporary.getId())).list();
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
								cicilanPembayaran.setKegiatan(kegiatan);
								cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
								cicilanPembayaran.setValidator(Common
										.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());
								cicilanPembayaran.setTanggal(
										tanggalTransaksi == null ? cicilanPembayaran.getTanggal() : tanggalTransaksi);
								sessionLocalKeg.getTransaction().begin();
								Common.refreshSaveOrUpdate(sessionLocalKeg, cicilanPembayaran);
								sessionLocalKeg.getTransaction().commit();
							}

							try {
								if (kegiatanTemporary.getKegiatan() == null || (kegiatanTemporary.getKegiatan() != null
										&& !kegiatanTemporary.getKegiatan().getId().equals(kegiatan.getId()))) {
									sessionLocalKeg.getTransaction().begin();
									kegiatanTemporary.setKegiatan(kegiatan);
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatanTemporary);
									sessionLocalKeg.getTransaction().commit();
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:306");
								// TODO: handle exception
							}

							Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.isNotNull("itemBiaya")).add(Restrictions.eq("kegiatan", kegiatan))
									.setProjection(Projections.sum("nilai")).uniqueResult();
							Double nilaiBiayaHarusDiBayars = 0.0;
							Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();

							try {

								Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
								Collection<DetailBiaya> mydetailBiayas = PembayaranUtilHelper
										.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, false);
								for (Object o : mydetailBiayas) {
									if (o instanceof DetailBiaya) {
										DetailBiaya detailBiaya = (DetailBiaya) o;
										map.put(detailBiaya.getId(), detailBiaya);
									} else if (o instanceof PengaturanPembayaranBulanan) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
										DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
										map.put(detailBiaya.getId(), detailBiaya);
									}
								}

								for (DetailBiaya detailBiaya : map.values()) {
									nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
								}

								kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - amountTotal);
								kegiatan.setAmount(amountTotal);
								sessionLocalKeg.getTransaction().begin();
								Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
								sessionLocalKeg.getTransaction().commit();
								KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (sessionLocalKeg != null) {
							try { sessionLocalKeg.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:351");}
							try { sessionLocalKeg.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:352");}
							try { sessionLocalKeg.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:353");}
						}
					}
				}

				if (bsiRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("bsiRequest", bsiRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("bsi_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:368");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setBsiRequest(bsiRequest);
					logPembayaran.setNominal(bsiRequest.getAmount());
					logPembayaran.setKeterangan(bsiRequest.getResponse());
					logPembayaran.setKegiatan(kegiatan);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

			} else {

				if (bsiRequest.getSiswa() != null || bsiRequest.getCalonSiswa() != null) {

					Sekolah sekolah = bsiRequest.getSiswa() != null ? bsiRequest.getSiswa().getSekolah()
							: bsiRequest.getCalonSiswa().getSekolah();
					String kode_akun_bsi = Common.getKonfigurasi("kode_akun_bsi", "").getNilai();
					AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) (kode_akun_bsi.isEmpty() ? null
							: ConstantValues.simpleObject(session.createCriteria(AkunPembayaranSiswa.class)
									.createAlias("akun", "akun").add(Restrictions.eq("sekolah", sekolah))
									.add(Restrictions.ilike("akun.kode", kode_akun_bsi, MatchMode.START))
									.addOrder(Order.desc("id")).setMaxResults(1), AkunPembayaranSiswa.class));
					if (akunPembayaranSiswa == null && !kode_akun_bsi.trim().isEmpty()) {
						Akun akun = (Akun) session.createCriteria(Akun.class)
								.add(Restrictions.eq("satuanKerja", sekolah.getSatuanKerja()))
								.add(Restrictions.ilike("kode", kode_akun_bsi, MatchMode.START)).setMaxResults(1)
								.uniqueResult();
						if (akun != null) {
							akunPembayaranSiswa = new AkunPembayaranSiswa();
							akunPembayaranSiswa.setAkun(akun);
							akunPembayaranSiswa.setNama("bayar via BNI");
							akunPembayaranSiswa.setSekolah(sekolah);
							session.getTransaction().begin();
							session.save(akunPembayaranSiswa);
							session.getTransaction().commit();
						}
					}

					session.createSQLQuery(
							"delete from sekolah.pembayaran_siswa where bsi_request_id = " + bsiRequest.getId())
							.executeUpdate();
					List<BsiRequestDetail> bsiRequestDetails = session.createCriteria(BsiRequestDetail.class)
							.add(Restrictions.eq("bsiRequest", bsiRequest)).list();
					Map<String, List<BsiRequestDetail>> maps = new HashMap<String, List<BsiRequestDetail>>();
					for (BsiRequestDetail bsiRequestDetail : bsiRequestDetails) {
						try {
							String key = bsiRequestDetail.getTagihan().getSiswa() != null
									? bsiRequestDetail.getTagihan().getSiswa().getId().toString()
									: bsiRequestDetail.getTagihan().getCalonSiswa().getId().toString();
							if (maps.containsKey(key)) {
								maps.get(key).add(bsiRequestDetail);
							} else {
								List<BsiRequestDetail> dataRequest = new ArrayList<BsiRequestDetail>();
								dataRequest.add(bsiRequestDetail);
								maps.put(key, dataRequest);
							}
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:427");
							// TODO: handle exception
						}
					}
					Double totalSemua = 0.0;

					for (List<BsiRequestDetail> details : maps.values()) {
						System.out.println("details -> " + details);
						BsiRequestDetail jenisBiayaSekolah = details.get(0);
						Double total = 0.0;
						for (BsiRequestDetail bsiRequestDetail : details) {
							total += (bsiRequestDetail.getTagihan().getNominal()
									+ bsiRequestDetail.getTagihan().getDenda());
						}
						totalSemua += total;

						PembayaranSiswa pembayaranSiswa = new PembayaranSiswa();
						pembayaranSiswa.setBulan(jenisBiayaSekolah.getTagihan().getBulan());
						pembayaranSiswa.setTahun(jenisBiayaSekolah.getTagihan().getTahun());
						pembayaranSiswa.setTahunDanBulan(jenisBiayaSekolah.getTagihan().getTahunbulan());
						pembayaranSiswa.setSiswa(bsiRequest.getSiswa());
						pembayaranSiswa.setCalonSiswa(bsiRequest.getCalonSiswa());
						pembayaranSiswa.setJenisBiayaSekolah(jenisBiayaSekolah.getTagihan().getNominalBiaya()
								.getPengaturanBiaya().getJenisBiayaSekolah());
						pembayaranSiswa.setTanggal(
								tanggalTransaksi == null ? ais.ui.util.WaktuUtil.getDate() : tanggalTransaksi);
						pembayaranSiswa.setKeterangan(bsiRequest.getKeterangan());
						pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
						pembayaranSiswa.setNominal(total);
						pembayaranSiswa.setBsiRequest(bsiRequest);
						// pembayaranSiswa.setTotalDeposit(total);
						pembayaranSiswa.setTambahanDeposit(total);
						pembayaranSiswa.setValidator(
								Common.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());

						session.getTransaction().begin();
						if (pembayaranSiswa.getId() == null) {
							session.save(pembayaranSiswa);
						} else {
							Common.refreshUpdate(session, pembayaranSiswa);
						}
						session.getTransaction().commit();

						session.getTransaction().begin();
						pembayaranSiswa.saveOrUpdateDeposit(session);
						session.getTransaction().commit();

						for (BsiRequestDetail bsiRequestDetail : details) {
							try {
								Tagihan tagihan = bsiRequestDetail.getTagihan();
								NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
								System.out.println("tagihan -> " + tagihan);
								if (nominalBiaya != null) {

									Double nominal = tagihan.getNominal();
									nominal = nominal - tagihan.getDiskon();
									nominal += tagihan.getDenda();

									PembayaranSiswaDetail pembayaranSiswaDetail = new PembayaranSiswaDetail(tagihan);
									pembayaranSiswaDetail.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
									pembayaranSiswaDetail.setNominalBiaya(nominalBiaya);
									pembayaranSiswaDetail.setNominal(nominal);
									pembayaranSiswaDetail.setNominalManual(nominal);
									pembayaranSiswaDetail.setPembayaranSiswa(pembayaranSiswa);
									session.getTransaction().begin();
									session.save(pembayaranSiswaDetail);
									session.getTransaction().commit();

									session.refresh(tagihan);
									tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
									session.getTransaction().begin();
									session.update(tagihan);
									session.getTransaction().commit();

									if (tagihan.getDiskonSiswa() != null
											&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
										DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
									}

								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bsiresponse.java:508");
							}
						}
					}

					try {

						Double deposit = bsiRequest.getAmount() - totalSemua;

						if (bsiRequest.getSiswa() != null) {

							DepositSiswa depositSiswa = (DepositSiswa) session.createCriteria(DepositSiswa.class)
									.add(Restrictions.eq("bsiRequest", bsiRequest)).setMaxResults(1).uniqueResult();
							if (depositSiswa == null) {
								depositSiswa = new DepositSiswa();
							}
							depositSiswa.setSiswa(bsiRequest.getSiswa());
							depositSiswa.setPembayaranSiswa(null);
							depositSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
							depositSiswa.setYayasan(depositSiswa.getYayasan());
							depositSiswa.setSekolah(sekolah);
							depositSiswa.setInquiryPembayaran("000000");
							depositSiswa.setNominal(deposit);
							depositSiswa.setTanggalBayar(
									tanggalTransaksi == null ? ais.ui.util.WaktuUtil.getDate() : tanggalTransaksi);
							depositSiswa.setWaktu(
									tanggalTransaksi == null ? ais.ui.util.WaktuUtil.getDate() : tanggalTransaksi);
							depositSiswa.setBsiRequest(bsiRequest);
							depositSiswa.setValidator(Common
									.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());
							depositSiswa.setKeterangan("Tabungan");

							session.getTransaction().begin();
							session.saveOrUpdate(depositSiswa);
							session.getTransaction().commit();

							PembayaranSiswaUtil.cetakDeposit(depositSiswa);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:546");
//						e.printStackTrace();
					}

					if (!maps.isEmpty()) {
						try {
							PembayaranSiswaUtil.cetakBsi(null, bsiRequest);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:553");
//							e.printStackTrace();
						}
					}
					
					if (bsiRequest != null && bsiRequest.getCalonSiswa() != null && bsiRequest.getCalonSiswa().getId() != null) {
						bsiRequest.getCalonSiswa().populatePembayaran();
					}
					if (bsiRequest != null && bsiRequest.getSiswa() != null && bsiRequest.getSiswa().getId() != null) {
						bsiRequest.getSiswa().populatePembayaran();
					}
				} else {

					Kegiatan kegiatan = Bsiresponse.createKegiatan(bsiRequest, session);

					if (hapusDulu) {
						session.createSQLQuery("delete from cicilan_pembayaran where ref_va = " + bsiRequest.getId()
								+ " and kegiatan=" + kegiatan.getId()).executeUpdate();
					}

					List<BsiRequestDetail> bsiRequestDetails = session.createCriteria(BsiRequestDetail.class)
							.add(Restrictions.isNull("idCicilan")).add(Restrictions.eq("bsiRequest", bsiRequest))
							.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();
					System.out.println("bsiRequestDetails==>" + bsiRequestDetails);
					if (!bsiRequestDetails.isEmpty()) {

						for (BsiRequestDetail bsiRequestDetail : bsiRequestDetails) {

							String ref = "bsiRequestDetail-" + bsiRequestDetail.getId();

							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
									.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
									.setMaxResults(1).uniqueResult();
							if (cicilanPembayaran == null) {
								cicilanPembayaran = new CicilanPembayaran(bsiRequestDetail.getDetailBiaya());
							}
							cicilanPembayaran.setTanggal(
									tanggalTransaksi == null ? bsiRequestDetail.getTanggal() : tanggalTransaksi);
							cicilanPembayaran.setRef(ref);
							cicilanPembayaran.setValidator(Common
									.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());
							cicilanPembayaran.setKe(bsiRequestDetail.getKe());
							cicilanPembayaran.setKegiatan(kegiatan);
							cicilanPembayaran.setItemBiaya(bsiRequestDetail.getItemBiaya());
							cicilanPembayaran.setRefVa(bsiRequest.getId());
							cicilanPembayaran
									.setPengaturanPembayaranBulanan(bsiRequestDetail.getPengaturanPembayaranBulanan());
							cicilanPembayaran.setNilai(bsiRequestDetail.getNilai());
							cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
							cicilanPembayaran.setDenda(bsiRequestDetail.getDenda());
							cicilanPembayaran.setNilaiAsli(bsiRequestDetail.getNilaiAsli());
							session.getTransaction().begin();
							if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
							session.getTransaction().commit();

							bsiRequestDetail.setCicilanref(cicilanPembayaran.getId());
							session.getTransaction().begin();
							session.update(bsiRequestDetail);
							session.getTransaction().commit();
						}

					} else if (bsiRequest.getCicilan() != null && !bsiRequest.getCicilan().isEmpty()) {
						VirtualAccountBank.populateCicilan(session, bsiRequest.getCicilan(), kegiatan,
								bsiRequest.getId(), tanggalTransaksi, jenisPembayaran, "bsi");
					}
					Double nilaiBiayaHarusDiBayars = bsiRequest.getNilaiBiayaHarusDiBayars();
					Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
					Double jumlah = d[0];
					Double denda = d[1];
					kegiatan.setDenda(denda.doubleValue());
					kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
					kegiatan.setAmount(jumlah.doubleValue());

					kegiatan.setValidator(
							Common.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, kegiatan);
					session.getTransaction().commit();

					if (bsiRequest.getAmount() > 0.1) {
						LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
								.add(Restrictions.eq("bsiRequest", bsiRequest)).setMaxResults(1).uniqueResult();
						if (logPembayaran == null) {
							logPembayaran = new LogPembayaran();
						}

						double biayaAdministrasi = 0.0;
						try {
							biayaAdministrasi = Double
									.parseDouble(Common.getKonfigurasi("bsi_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:644");

						}
						logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
						logPembayaran.setKegiatan(kegiatan);
						logPembayaran.setBsiRequest(bsiRequest);
						logPembayaran.setNominal(bsiRequest.getAmount());
						logPembayaran.setKeterangan(bsiResponse.getKeterangan());
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, logPembayaran);
						session.getTransaction().commit();
					}

					pembayaranUtil.updateTunggakan(kegiatan, session);
						KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);

					Mahasiswa mhs = bsiRequest.getMahasiswa();
					BiodataCalonMahasiswa bio = bsiRequest.getBiodataCalonMahasiswa();
					if (mhs != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
					} else if (bio != null) {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
					}
				}
			}
		}
		return bsiRequest;
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:673");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:674");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:675");}
			}
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), "BSI");
		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> BsiResponse data => " + data);
		System.out.println("==> BsiResponse querystring => " + querystring);

		String trx_id = null;
		BsiRequest bsiRequest = null;
		JSONObject bsi = null;
		// Jejak stack trace bila pemrosesan callback/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;
		try {

			JSONObject jsonObject = new JSONObject(data);
			String parsedData = jsonObject.getString("data");
			String merchant_id = jsonObject.getString("client_id");
			String Password = Common.getKonfigurasi("bsi_password", "685dedd9f045787873794ead6276f8bf").getNilai()
					.trim();

			{
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					Object[] bsiPasswords = (Object[]) session.createCriteria(Sekolah.class)
							.setProjection(Projections.projectionList().add(Projections.property("bsiPassword"))
									.add(Projections.property("bsiMerchantId")))
							.add(Restrictions.ilike("bsiMerchantId", merchant_id, MatchMode.ANYWHERE)).setMaxResults(1)
							.uniqueResult();

					if (bsiPasswords != null && bsiPasswords.length > 0) {
						String bsiPassword = bsiPasswords[0] + "";
						String bsiMerchantId = bsiPasswords[1] + "";
						System.out.println("chek bsiPassword " + bsiPassword + " bsiMerchantId " + bsiMerchantId);
						Integer tahun = 0;
						for (String p : StringUtils.split(bsiMerchantId, ";")) {
							try {
								String[] a = StringUtils.split(p, ":");
								if (a[1].equalsIgnoreCase(merchant_id)) {
									tahun = Integer.parseInt(a[0]);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bsiresponse.java:731");
							}
						}

						System.out.println("chek merchant_id " + merchant_id + " tahun " + tahun);

						if (!tahun.equals(0)) {

							if (StringUtils.contains(bsiPassword, ":")) {
								for (String s : StringUtils.split(bsiPassword, ";")) {
									Integer ta = Integer.parseInt(StringUtils.split(s, ":")[0]);
									String m = StringUtils.split(s, ":")[1];
									boolean ketemu = ta.equals(tahun);
									System.out.println("chek merchant_id " + merchant_id + " masuk " + tahun + " ta " + ta
											+ " m " + m + " ketemu " + ketemu);
									if (ketemu) {
										Password = m;
										break;
									}
								}
							}

						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bsiresponse.java:756");
				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:759");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:760");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:761");}
					}
				}
			}

			System.out.println("==> BsiResponse merchant_id " + merchant_id + ", Password " + Password + ", parsedData "
					+ parsedData);

			String decodeData = BNIHash.parseData(parsedData, merchant_id, Password);
			System.out.println("==> BsiResponse decoded => " + decodeData);
			bsi = new JSONObject(decodeData);

			trx_id = bsi.getString("trx_id");

			System.out.println("==> BsiResponse trx_id => " + trx_id + ", bsiResponse = " + bsi);
			if (trx_id != null && !trx_id.trim().isEmpty()) {

				String payment_amount = bsi.getString("payment_amount");
				String trx_amount = bsi.getString("trx_amount");

				System.out.println("payment_amount = " + payment_amount + ", trx_amount = " + trx_amount);

				String status = payment_amount.trim().equals(trx_amount.trim()) ? "000" : "001";

				Session session2 = null;
				BsiResponse bsiResponse = null;
				try {
					session2 = HibernateUtil.getSessionFactory().openSession();
					bsiResponse = (BsiResponse) session2.createCriteria(BsiResponse.class)
							.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).addOrder(Order.desc("id"))
							.uniqueResult();
					if (bsiResponse == null) {
						bsiResponse = new BsiResponse();
					}
					bsiResponse.setKeterangan(bsi.toString());
					bsiResponse.setNama(trx_id);
					bsiResponse.setStatus(BsiResponse.SEDANG_DIPROSES);
					bsiResponse.setMerchant(merchant_id);
					bsiResponse.setTrxId(trx_id);
					bsiResponse.setCallback(data);
					bsiResponse.setKodeStatus(status);
					session2.getTransaction().begin();
					session2.saveOrUpdate(bsiResponse);
					session2.getTransaction().commit();
				} finally {
					if (session2 != null) {
						try { session2.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:807");}
						try { session2.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:808");}
						try { session2.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bsiresponse.java:809");}
					}
				}
				if (bsiResponse != null) {
					bsiRequest = prosesResponse(bsiResponse, false);
				}
			}

		} catch (Exception e) {
			h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
			// (helper: session terdedikasi + commit + retry, tak pernah gagal).
			try {
				LogHostToHost logHostToHost = new LogHostToHost();
				logHostToHost.setKeterangan(data);
				try {
					if (request != null && request.getHeader("Cf-Connecting-Ip") != null) {
						logHostToHost.setIp(request.getHeader("Cf-Connecting-Ip"));
					} else if (request != null && request.getHeader("CF-Connecting-IP") != null) {
						logHostToHost.setIp(request.getHeader("CF-Connecting-IP"));
					} else if (request != null && request.getHeader("X-Forwarded-For") != null) {
						logHostToHost.setIp(request.getHeader("X-Forwarded-For"));
					} else if (request != null && request.getHeader("X-Real-IP") != null) {
						logHostToHost.setIp(request.getHeader("X-Real-IP"));
					} else {
						logHostToHost.setIp(
								request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : ""));
					}
				} catch (Exception e) {
					logHostToHost
							.setIp(request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : ""));
				}
				logHostToHost.setBankHost(bankHost);
				logHostToHost.setNim(bsiRequest == null ? ""
						: (bsiRequest.getMahasiswa() != null ? bsiRequest.getMahasiswa().getNim()
								: bsiRequest.getBiodataCalonMahasiswa() != null
										? bsiRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
										: ""));
				logHostToHost.setKode(trx_id);
				logHostToHost.setNama(bsiRequest == null ? ""
						: (bsiRequest.getMahasiswa() != null ? bsiRequest.getMahasiswa().getNama()
								: bsiRequest.getBiodataCalonMahasiswa() != null
										? bsiRequest.getBiodataCalonMahasiswa().getNama()
										: ""));
				logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
				logHostToHost
						.setResponseDescription(bsiRequest == null ? "Gagal" : "Sukses (" + bsiRequest.getResponse() + ")");
				logHostToHost.setNominal(
						bsiRequest == null ? 0.0 : (bsiRequest.getAmount() + bsiRequest.getBiayaAdministrasi()));
				logHostToHost.setItem(bsi == null ? "" : bsi.toString());
				logHostToHost.setStackTrace(h2hStackTrace);
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		String body = "{ \"status\" : \"000\" }";
		writer.write(body);

	}

	public static void main(String[] argv) {
		String parsedData = "TSRNTyEoSx5SFxhiDlJXVmRdR1YAX0xFU35nVk5fCnhLCFsLCmI7UhEjSiNTFhoXDhsETGRkWlEETllFUX5bTgsrOGsrYzlXPDxuag88WjlqJz4wDhsETVBlS1YAVkxFU35nVk5fCjskPR5GTh9GSCgfSyE8FxohHyYcHR8TEgR7SltLVwZbTkhhdxJXAFoKe1cMBycoSRw-IAwZHCETFh8qExRNPRgWHVAlIx0hRFAjTRdGUyhJSBEeO1t9X1dMWmNBSlxgW1ALCyEIFE4mIBkhRjsWPVx3FVt-BmNRB19-CCQJISIZHiInFhtJHxcZFlQhIRkhRk0fTA5CPmILEE5TBloRVF4JJhETGicoFhJHCxMIVw9mSFJVOFMMVCBNXyZMTDEkXB1iCBYJYlhUXWRSUkF4TEpVWAtiCyMTS1IhTyJGPms";
		String merchant_id = "9556";
		String Password = "5dc99d6f-5266-4981-a021-d9438ad4b7af";
		String decodeData = BNIHash.parseData(parsedData, merchant_id, Password);
		System.out.println("==> BsiResponse decoded => " + decodeData);
	}

}
