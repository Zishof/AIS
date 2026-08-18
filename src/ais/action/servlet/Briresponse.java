package ais.action.servlet;


import ais.action.master.helper.KegiatanPersistenceHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

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
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.bri.BriRequest;
import ais.database.model.bri.BriRequestDetail;
import ais.database.model.bri.BriResponse;
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
public class Briresponse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Briresponse() {
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


	private static boolean isRequestSudahDiproses(BriRequest briRequest, Session session) {
		if (briRequest == null || session == null) {
			return false;
		}
		try {
			if (briRequest.getSiswa() != null || briRequest.getCalonSiswa() != null) {
				Number jumlahPembayaran = (Number) session.createCriteria(PembayaranSiswa.class)
						.add(Restrictions.eq("briRequest", briRequest))
						.setProjection(Projections.rowCount()).uniqueResult();
				if (jumlahPembayaran != null && jumlahPembayaran.intValue() > 0) {
					return true;
				}
			}

			Number jumlahLog = (Number) session.createCriteria(LogPembayaran.class)
					.add(Restrictions.eq("briRequest", briRequest))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (jumlahLog != null && jumlahLog.intValue() > 0) {
				return true;
			}

			Collection temporarys = briRequest.getKegiatanTemporarys();
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

	public static Kegiatan createKegiatan(BriRequest briRequest, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = briRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = briRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = briRequest.getBiodataCalonMahasiswa();

		Integer semester = briRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = briRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = briRequest.getJenisKegiatan();
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
				+ briRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ briRequest.getHapusCicilanSebelumnya());

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
			kegiatan.setTahunAkademik(briRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator("BRI");
			kegiatan.setPengurangan(briRequest.getPengurangan());
			kegiatan.setKeterangan(briRequest.getKeterangan());
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
	public static BriRequest prosesResponse(BriResponse briResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		BriRequest briRequest = (BriRequest) session.createCriteria(BriRequest.class)
				.add(Restrictions.eq("trxId", briResponse.getTrxId())).setMaxResults(1).uniqueResult();

		if (briRequest != null && briResponse.getKodeStatus().toString().trim().equalsIgnoreCase("00")) {

			if (isRequestSudahDiproses(briRequest, session)) {
				System.out.println("Callback briRequest sudah pernah diproses, proses pembayaran dilewati: " + briRequest);
				return briRequest;
			}

			briRequest.setBriResponse(briResponse);
			briRequest.setStatus("Payment Sukses");
			briRequest.setKodeStatus(briResponse.getKodeStatus());
			session.getTransaction().begin();
			session.update(briRequest);
			session.getTransaction().commit();

			String kodeAkun = Common.getKonfigurasi("kode_akun_bri", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);

			if (!briRequest.getKegiatanTemporarys().isEmpty()) {
				Kegiatan kegiatan = null;
				for (KegiatanTemporary temporary : briRequest.getKegiatanTemporarys()) {
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
							}

							kegiatan.setJenisKegiatan(jenisKegiatan);
							kegiatan.setJadwalPembayaran(kegiatanTemporary.getJadwalPembayaran());
							kegiatan.setMahasiswa(mahasiswa);
							kegiatan.setSemster(smt);
							kegiatan.setStatusMahasiswa(kegiatanTemporary.getStatusMahasiswa());
							kegiatan.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
							kegiatan.setTanggal(kegiatanTemporary.getTanggal());
							kegiatan.setValidated(1);
							kegiatan.setJenisKegiatan(jenisKegiatan);
							kegiatan.setValidator("BRI");
							kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

							sessionLocalKeg.getTransaction().begin();
							Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
							sessionLocalKeg.getTransaction().commit();

							List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.eq("kegiatanTemporary.id", temporary.getId())).list();
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
								cicilanPembayaran.setValidator("BRI");
								cicilanPembayaran.setKegiatan(kegiatan);
								cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
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
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:282");
								// TODO: handle exception
							}

							Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.isNotNull("itemBiaya")).add(Restrictions.eq("kegiatan", kegiatan))
									.setProjection(Projections.sum("nilai")).uniqueResult();
							Double nilaiBiayaHarusDiBayars = 0.0;
							Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();

							try {

								Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
								Collection<DetailBiaya> mydetailBiayas = pembayaranUtil
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
							try { sessionLocalKeg.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:327");}
							try { sessionLocalKeg.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:328");}
							try { sessionLocalKeg.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:329");}
						}
					}
				}

				if (briRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("briRequest", briRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("bri_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:344");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setBriRequest(briRequest);
					logPembayaran.setNominal(briRequest.getAmount());
					logPembayaran.setKeterangan(briRequest.getResponse());
					logPembayaran.setKegiatan(kegiatan);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

			} else {

				if (briRequest.getSiswa() != null || briRequest.getCalonSiswa() != null) {

					Sekolah sekolah = briRequest.getSiswa() != null ? briRequest.getSiswa().getSekolah()
							: briRequest.getCalonSiswa().getSekolah();
					String kode_akun_bri = Common.getKonfigurasi("kode_akun_bri", "").getNilai();
					AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) (kode_akun_bri.isEmpty() ? null
							: ConstantValues
									.simpleObject(
											session.createCriteria(AkunPembayaranSiswa.class)
													.createAlias("akun", "akun")
													.add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1)
													.addOrder(Order.desc("id")).add(Restrictions.ilike("akun.kode",
															kode_akun_bri, MatchMode.START)),
											AkunPembayaranSiswa.class));
					if (akunPembayaranSiswa == null && !kode_akun_bri.trim().isEmpty()) {
						Akun akun = (Akun) session.createCriteria(Akun.class)
								.add(Restrictions.eq("satuanKerja", sekolah.getSatuanKerja()))
								.add(Restrictions.ilike("kode", kode_akun_bri, MatchMode.START)).setMaxResults(1)
								.uniqueResult();
						if (akun != null) {
							akunPembayaranSiswa = new AkunPembayaranSiswa();
							akunPembayaranSiswa.setAkun(akun);
							akunPembayaranSiswa.setNama("bayar via BRI");
							akunPembayaranSiswa.setSekolah(sekolah);
							session.getTransaction().begin();
							session.save(akunPembayaranSiswa);
							session.getTransaction().commit();
						}
					}

					session.createSQLQuery(
							"delete from sekolah.pembayaran_siswa where bri_request_id = " + briRequest.getId())
							.executeUpdate();
					List<BriRequestDetail> briRequestDetails = session.createCriteria(BriRequestDetail.class)
							.add(Restrictions.eq("briRequest", briRequest)).list();
					Map<String, List<BriRequestDetail>> maps = new HashMap<String, List<BriRequestDetail>>();
					for (BriRequestDetail briRequestDetail : briRequestDetails) {
						try {
							String key = briRequestDetail.getTagihan().getSiswa() != null
									? briRequestDetail.getTagihan().getSiswa().getId().toString()
									: briRequestDetail.getTagihan().getCalonSiswa().getId().toString();
							if (maps.containsKey(key)) {
								maps.get(key).add(briRequestDetail);
							} else {
								List<BriRequestDetail> dataRequest = new ArrayList<BriRequestDetail>();
								dataRequest.add(briRequestDetail);
								maps.put(key, dataRequest);
							}
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:407");
							// TODO: handle exception
						}
					}

					Double totalSemua = 0.0;
					for (List<BriRequestDetail> details : maps.values()) {

						BriRequestDetail jenisBiayaSekolah = details.get(0);

						Double total = 0.0;
						for (BriRequestDetail briRequestDetail : details) {
							total += (briRequestDetail.getTagihan().getNominal()
									+ briRequestDetail.getTagihan().getDenda());
						}
						totalSemua += total;

						PembayaranSiswa pembayaranSiswa = new PembayaranSiswa();
						pembayaranSiswa.setBulan(jenisBiayaSekolah.getTagihan().getBulan());
						pembayaranSiswa.setTahun(jenisBiayaSekolah.getTagihan().getTahun());
						pembayaranSiswa.setTahunDanBulan(jenisBiayaSekolah.getTagihan().getTahunbulan());
						pembayaranSiswa.setSiswa(briRequest.getSiswa());
						pembayaranSiswa.setCalonSiswa(briRequest.getCalonSiswa());
						pembayaranSiswa.setJenisBiayaSekolah(jenisBiayaSekolah.getTagihan().getNominalBiaya()
								.getPengaturanBiaya().getJenisBiayaSekolah());
						pembayaranSiswa.setTanggal(ais.ui.util.WaktuUtil.getDate());
						pembayaranSiswa.setKeterangan(briRequest.getKeterangan());
						pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
						pembayaranSiswa.setNominal(total);
						pembayaranSiswa.setBriRequest(briRequest);
						// pembayaranSiswa.setTotalDeposit(total);
						pembayaranSiswa.setTambahanDeposit(total);

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

						for (BriRequestDetail briRequestDetail : details) {
							Tagihan tagihan = briRequestDetail.getTagihan();
							NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
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

								tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
								session.getTransaction().begin();
								session.update(tagihan);
								session.getTransaction().commit();

								if (tagihan.getDiskonSiswa() != null
										&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
									DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
								}
							}
						}
					}
					try {
						PembayaranSiswaUtil.cetakBri(null, briRequest);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:485");
					}

					try {

						Double deposit = briRequest.getAmount() - totalSemua;

						if (briRequest.getSiswa() != null) {
							DepositSiswa depositSiswa = new DepositSiswa();
							depositSiswa.setSiswa(briRequest.getSiswa());
							depositSiswa.setPembayaranSiswa(null);

							depositSiswa.setYayasan(depositSiswa.getYayasan());
							depositSiswa.setSekolah(sekolah);
							depositSiswa.setInquiryPembayaran("000000");
							depositSiswa.setNominal(deposit);
							depositSiswa.setTanggalBayar(ais.ui.util.WaktuUtil.getDate());
							depositSiswa.setWaktu(ais.ui.util.WaktuUtil.getDate());
							depositSiswa.setKeterangan("Tabungan");
							session.getTransaction().begin();
							session.save(depositSiswa);
							session.getTransaction().commit();
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					
					if (briRequest != null && briRequest.getCalonSiswa() != null && briRequest.getCalonSiswa().getId() != null) {
						briRequest.getCalonSiswa().populatePembayaran();
					}
					if (briRequest != null && briRequest.getSiswa() != null && briRequest.getSiswa().getId() != null) {
						briRequest.getSiswa().populatePembayaran();
					}
					
				} else {

					Kegiatan kegiatan = Briresponse.createKegiatan(briRequest, session);
					Double nilaiBiayaHarusDiBayars = briRequest.getNilaiBiayaHarusDiBayars();

					List<BriRequestDetail> briRequestDetails = session.createCriteria(BriRequestDetail.class)
							.add(Restrictions.isNull("idCicilan")).add(Restrictions.eq("briRequest", briRequest))
							.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();
					System.out.println("briRequestDetails==>" + briRequestDetails);
					if (!briRequestDetails.isEmpty()) {

						for (BriRequestDetail briRequestDetail : briRequestDetails) {

							String ref = "briRequestDetail-" + briRequestDetail.getId();

							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
									.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
									.setMaxResults(1).uniqueResult();
							if (cicilanPembayaran == null) {
								cicilanPembayaran = new CicilanPembayaran(briRequestDetail.getDetailBiaya());

							}

							cicilanPembayaran.setRef(ref);
							cicilanPembayaran.setValidator("BRI");
							cicilanPembayaran.setKe(briRequestDetail.getKe());
							cicilanPembayaran.setKegiatan(kegiatan);
							cicilanPembayaran.setItemBiaya(briRequestDetail.getItemBiaya());
							cicilanPembayaran
									.setPengaturanPembayaranBulanan(briRequestDetail.getPengaturanPembayaranBulanan());
							cicilanPembayaran.setNilai(briRequestDetail.getNilai());
							cicilanPembayaran.setTanggal(briRequestDetail.getTanggal());
							cicilanPembayaran
									.setPengaturanPembayaranBulanan(briRequestDetail.getPengaturanPembayaranBulanan());
							cicilanPembayaran.setDenda(briRequestDetail.getDenda());
							cicilanPembayaran.setNilaiAsli(briRequestDetail.getNilaiAsli());
							session.getTransaction().begin();
							if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
							session.getTransaction().commit();

							briRequestDetail.setCicilanref(cicilanPembayaran.getId());
							session.getTransaction().begin();
							session.update(briRequestDetail);
							session.getTransaction().commit();
						}

						Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
						Double jumlah = d[0];
						Double denda = d[1];
						kegiatan.setDenda(denda.doubleValue());
						kegiatan.setAmountTerhutang(
								nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
						kegiatan.setAmount(jumlah.doubleValue());

						kegiatan.setValidator("BRI");

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, kegiatan);
						session.getTransaction().commit();
						KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);

					}

					if (briRequest.getAmount() > 0.1) {
						LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
								.add(Restrictions.eq("briRequest", briRequest)).setMaxResults(1).uniqueResult();
						if (logPembayaran == null) {
							logPembayaran = new LogPembayaran();
						}

						double biayaAdministrasi = 0.0;
						try {
							biayaAdministrasi = Double
									.parseDouble(Common.getKonfigurasi("bri_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:593");

						}
						logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
						logPembayaran.setKegiatan(kegiatan);
						logPembayaran.setBriRequest(briRequest);
						logPembayaran.setNominal(briRequest.getAmount());
						logPembayaran.setKeterangan(briResponse.getKeterangan());
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, logPembayaran);
						session.getTransaction().commit();
					}

					pembayaranUtil.updateTunggakan(kegiatan, session);
						KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);

					Mahasiswa mhs = briRequest.getMahasiswa();
					BiodataCalonMahasiswa bio = briRequest.getBiodataCalonMahasiswa();
					if (mhs != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
					} else if (bio != null) {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
					}
				}
			}
		}
		return briRequest;
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:622");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:623");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:624");}
			}
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), "BRI");
		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> BriResponse data => " + data);
		System.out.println("==> BriResponse querystring => " + querystring);

		String trx_id = null;
		BriRequest briRequest = null;
		JSONObject bri = null;
		// Jejak stack trace bila pemrosesan callback/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;
		try {

			String merchant_id = Common.getKonfigurasi("bri_merchant_id", "000").getNilai().trim();
			JSONObject jsonObject = new JSONObject(data);
			String parsedData = jsonObject.getString("data");

			bri = new JSONObject(parsedData);

			System.out.println("==> BriResponse decoded bri => " + bri);
			trx_id = bri.getString("trx_id");

			System.out.println("==> BriResponse trx_id => " + trx_id + ", briResponse = " + bri);
			if (trx_id != null && !trx_id.trim().isEmpty()) {

				String payment_amount = bri.getString("payment_amount");
				String trx_amount = bri.getString("trx_amount");

				System.out.println("payment_amount = " + payment_amount + ", trx_amount = " + trx_amount);

				String status = payment_amount.trim().equals(trx_amount.trim()) ? "000" : "001";

				BriResponse briResponse = null;
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					briResponse = (BriResponse) session.createCriteria(BriResponse.class)
							.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).addOrder(Order.desc("id"))
							.uniqueResult();
					if (briResponse == null) {
						briResponse = new BriResponse();
					}
					briResponse.setKeterangan(bri.toString());
					briResponse.setNama(trx_id);
					briResponse.setStatus(BriResponse.SEDANG_DIPROSES);
					briResponse.setMerchant(merchant_id);
					briResponse.setTrxId(trx_id);
					briResponse.setCallback(data);
					briResponse.setKodeStatus(status);
					session.getTransaction().begin();
					session.saveOrUpdate(briResponse);
					session.getTransaction().commit();
				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:693");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:694");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briresponse.java:695");}
					}
				}
				briRequest = prosesResponse(briResponse);
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
				logHostToHost.setNim(briRequest == null ? ""
						: (briRequest.getMahasiswa() != null ? briRequest.getMahasiswa().getNim()
								: briRequest.getBiodataCalonMahasiswa() != null
										? briRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
										: ""));
				logHostToHost.setKode(trx_id);
				logHostToHost.setNama(briRequest == null ? ""
						: (briRequest.getMahasiswa() != null ? briRequest.getMahasiswa().getNama()
								: briRequest.getBiodataCalonMahasiswa() != null
										? briRequest.getBiodataCalonMahasiswa().getNama()
										: ""));
				logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
				logHostToHost
						.setResponseDescription(briRequest == null ? "Gagal" : "Sukses (" + briRequest.getResponse() + ")");
				logHostToHost.setNominal(
						briRequest == null ? 0.0 : (briRequest.getAmount() + briRequest.getBiayaAdministrasi()));
				logHostToHost.setItem(bri == null ? "" : bri.toString());
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

}
