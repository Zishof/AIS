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
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.bni.BniRequest;
import ais.database.model.bni.BniRequestDetail;
import ais.database.model.bni.BniResponse;
import ais.database.model.kursus.PesertaPunyaProdukKursus;
import ais.database.model.kursus.ProdukPeserta;
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
public class Bniresponse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Bniresponse() {
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


	@SuppressWarnings("rawtypes")
	private static boolean isRequestSudahDiproses(BniRequest bniRequest, Session session) {
		if (bniRequest == null || session == null) {
			return false;
		}
		try {
			// MAHASISWA via KegiatanTemporary (mis. Daftar Ulang): pembayaran dianggap "sudah diproses"
			// HANYA bila SETIAP kegiatanTemporary sudah terhubung ke Kegiatan DAN Kegiatan itu BENAR
			// memiliki CicilanPembayaran (pembayaran NYATA telah mendarat). Bila sekadar terhubung tapi
			// CicilanPembayaran KOSONG (akibat kegagalan PARSIAL pada percobaan sebelumnya — mis. session
			// closed saat migrasi cicilan), JANGAN dianggap selesai: kembalikan false agar "Cek Pembayaran"
			// memproses ULANG dan memindahkan cicilan dari kegiatanTemporary ke kegiatan.
			// CATATAN: pengecekan LogPembayaran SAJA TIDAK boleh mendahului ini — LogPembayaran bisa
			// tercatat walau migrasi cicilan gagal, sehingga dulu membuat re-check "berhasil" tetapi
			// pembayaran tidak pernah masuk ke CicilanPembayaran.
			Collection temporarys = bniRequest.getKegiatanTemporarys();
			if (temporarys != null && !temporarys.isEmpty()) {
				for (Object object : temporarys) {
					KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) object;
					if (kegiatanTemporary == null || kegiatanTemporary.getKegiatan() == null
							|| kegiatanTemporary.getKegiatan().getId() == null) {
						return false;
					}
					Number jumlahCicilan = (Number) session.createCriteria(CicilanPembayaran.class)
							.add(Restrictions.eq("kegiatan", kegiatanTemporary.getKegiatan()))
							.add(Restrictions.isNotNull("itemBiaya"))
							.setProjection(Projections.rowCount()).uniqueResult();
					if (jumlahCicilan == null || jumlahCicilan.intValue() == 0) {
						return false;
					}
				}
				return true;
			}

			// SISWA / CALON SISWA: bukti pembayaran nyata = PembayaranSiswa.
			if (bniRequest.getSiswa() != null || bniRequest.getCalonSiswa() != null) {
				Number jumlahPembayaran = (Number) session.createCriteria(PembayaranSiswa.class)
						.add(Restrictions.eq("bniRequest", bniRequest))
						.setProjection(Projections.rowCount()).uniqueResult();
				if (jumlahPembayaran != null && jumlahPembayaran.intValue() > 0) {
					return true;
				}
			}

			// Selain kasus mahasiswa-kegiatanTemporary di atas: LogPembayaran sebagai penanda sudah diproses.
			Number jumlahLog = (Number) session.createCriteria(LogPembayaran.class)
					.add(Restrictions.eq("bniRequest", bniRequest))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (jumlahLog != null && jumlahLog.intValue() > 0) {
				return true;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	public static Kegiatan createKegiatan(BniRequest bniRequest, Session session) {
		return createKegiatan(bniRequest, bniRequest.getJenisKegiatan(), session);
	}

	public static Kegiatan createKegiatan(BniRequest bniRequest, JenisKegiatan jenisKegiatanData, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = bniRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = bniRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = bniRequest.getBiodataCalonMahasiswa();

		Integer semester = bniRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = bniRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = jenisKegiatanData == null ? bniRequest.getJenisKegiatan() : jenisKegiatanData;
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
				+ bniRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ bniRequest.getHapusCicilanSebelumnya());

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
			kegiatan.setTahunAkademik(bniRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());
			kegiatan.setPengurangan(bniRequest.getPengurangan());
			kegiatan.setKeterangan(bniRequest.getKeterangan());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			session.save(kegiatan);
			session.getTransaction().commit();
		} else {
			// refresh hanya bila kegiatan masih terikat session ini; bila detached (karena
			// session lama tertutup) muat ulang by id agar dapat instance managed.
			try {
				if (session.contains(kegiatan)) {
					session.refresh(kegiatan);
				} else {
					Kegiatan reloaded = (Kegiatan) session.get(Kegiatan.class, kegiatan.getId());
					if (reloaded != null) {
						kegiatan = reloaded;
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
				try {
					Kegiatan reloaded = (Kegiatan) session.get(Kegiatan.class, kegiatan.getId());
					if (reloaded != null) {
						kegiatan = reloaded;
					}
				} catch (Exception ex) {
					ais.common.Common.tampilErrorJikaAdmin(ex);
				}
			}
		}

		return kegiatan;
	}

	@SuppressWarnings("unchecked")
	public static BniRequest prosesResponse(BniResponse bniResponse, boolean hapusDulu) {
		return prosesResponse(bniResponse, hapusDulu, false);
	}

	/**
	 * @param paksaProses bila TRUE, LEWATI guard {@link #isRequestSudahDiproses} (paksa proses ULANG).
	 *                    Dipakai saat admin menekan "Cek Pembayaran"/"Check Ulang Semua" agar pembayaran
	 *                    yang TIDAK SENGAJA TERHAPUS tetap bisa dipulihkan walau status terlihat "sudah
	 *                    diproses". Aman: proses idempoten — CicilanPembayaran dipindah/di-update &
	 *                    LogPembayaran di-reuse (query dulu), bukan dibuat ganda.
	 */
	public static BniRequest prosesResponse(BniResponse bniResponse, boolean hapusDulu, boolean paksaProses) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		BniRequest bniRequest = (BniRequest) session.createCriteria(BniRequest.class)
				.add(Restrictions.eq("trxId", bniResponse.getTrxId())).setMaxResults(1).uniqueResult();
		Kegiatan kegiatan = null;
		Date tanggalTransaksi = null;
		try {
			JSONObject jsonObject = new JSONObject(bniResponse.getKeterangan());
			tanggalTransaksi = Common.databaseDateFormat1.get().parse(jsonObject.getString("datetime_payment"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:271");
			// TODO: handle exception
		}

		System.out.println("bniResponse => " + bniResponse + ", tanggalTransaksi => "
				+ Common.dateFormat3.get().format(tanggalTransaksi));

		if (bniRequest != null && bniResponse.getKodeStatus().toString().trim().equalsIgnoreCase("000")) {

			if (!paksaProses && isRequestSudahDiproses(bniRequest, session)) {
				System.out.println("Callback bniRequest sudah pernah diproses, proses pembayaran dilewati: " + bniRequest);
				return bniRequest;
			}

			try {
				bniRequest.setBniResponse(bniResponse);
				bniRequest.setStatus("Payment Sukses");
				bniRequest.setKodeStatus(bniResponse.getKodeStatus());
				session.getTransaction().begin();
				Common.refreshUpdate(session, bniRequest);
				session.getTransaction().commit();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:292");
				// TODO: handle exception
			}

			String kodeAkun = Common.getKonfigurasi("kode_akun_bni", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);

			if (!bniRequest.getKegiatanTemporarys().isEmpty()) {
				for (KegiatanTemporary temporary : bniRequest.getKegiatanTemporarys()) {
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
							kegiatan.setValidator(Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());
							kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

							try {
								sessionLocalKeg.getTransaction().begin();
								Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
								sessionLocalKeg.getTransaction().commit();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:339");
								// TODO: handle exception
							}

							List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.eq("kegiatanTemporary", kegiatanTemporary)).list();
							for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
								try {
									cicilanPembayaran.setKegiatan(kegiatan);
									cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
									cicilanPembayaran.setValidator(
											Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());
									cicilanPembayaran
											.setTanggal(tanggalTransaksi == null ? cicilanPembayaran.getTanggal()
													: tanggalTransaksi);
									sessionLocalKeg.getTransaction().begin();
									Common.refreshSaveOrUpdate(sessionLocalKeg, cicilanPembayaran);
									sessionLocalKeg.getTransaction().commit();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:358");
									// TODO: handle exception
								}
							}

							try {
								if (kegiatanTemporary.getKegiatan() == null || (kegiatanTemporary.getKegiatan() != null
										&& !kegiatanTemporary.getKegiatan().getId().equals(kegiatan.getId()))) {
									sessionLocalKeg.getTransaction().begin();
									kegiatanTemporary.setKegiatan(kegiatan);
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatanTemporary);
									sessionLocalKeg.getTransaction().commit();
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:371");
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
									Double nilai = detailBiaya.hitungTotalKegiatan(kegiatan, sessionLocalKeg);
									nilaiBiayaHarusDiBayars += (nilai);
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
							try { sessionLocalKeg.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:416");}
							try { sessionLocalKeg.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:417");}
							try { sessionLocalKeg.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:418");}
						}
					}
				}

				if (bniRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("bniRequest", bniRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:433");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setBniRequest(bniRequest);
					logPembayaran.setNominal(bniRequest.getAmount());
					logPembayaran.setKeterangan(bniRequest.getResponse());
					logPembayaran.setKegiatan(kegiatan);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

			} else {

				if (bniRequest.getPesertaKursus() != null) {

					List<BniRequestDetail> bniRequestDetails = session.createCriteria(BniRequestDetail.class)
							.add(Restrictions.eq("bniRequest", bniRequest)).list();

					for (BniRequestDetail bniRequestDetail : bniRequestDetails) {
						PesertaPunyaProdukKursus pesertaPunyaProdukKursus = bniRequestDetail
								.getPesertaPunyaProdukKursus();
						if (pesertaPunyaProdukKursus != null) {
							session.refresh(pesertaPunyaProdukKursus);
							pesertaPunyaProdukKursus.setStatus(PesertaPunyaProdukKursus.TERBELI);
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, pesertaPunyaProdukKursus);
							session.getTransaction().commit();

							ProdukPeserta produkPeserta = (ProdukPeserta) session.createCriteria(ProdukPeserta.class)
									.add(Restrictions.eq("pesertaPunyaProdukKursus", pesertaPunyaProdukKursus))
									.setMaxResults(1).uniqueResult();
							if (produkPeserta == null) {
								produkPeserta = new ProdukPeserta();
								produkPeserta.setPesertaPunyaProdukKursus(pesertaPunyaProdukKursus);
								produkPeserta.setPesertaKursus(pesertaPunyaProdukKursus.getPesertaKursus());
								produkPeserta.setProdukKursus(pesertaPunyaProdukKursus.getProdukKursus());
								session.getTransaction().begin();
								session.save(produkPeserta);
								session.getTransaction().commit();
							}
						}
					}

				}

				else if (bniRequest.getSiswa() != null || bniRequest.getCalonSiswa() != null) {

					Sekolah sekolah = bniRequest.getSiswa() != null ? bniRequest.getSiswa().getSekolah()
							: bniRequest.getCalonSiswa().getSekolah();
					String kode_akun_bni = Common.getKonfigurasi("kode_akun_bni", "").getNilai();
					AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) (kode_akun_bni.isEmpty() ? null
							: ConstantValues.simpleObject(session.createCriteria(AkunPembayaranSiswa.class)
									.createAlias("akun", "akun").add(Restrictions.eq("sekolah", sekolah))
									.add(Restrictions.ilike("akun.kode", kode_akun_bni, MatchMode.START))
									.addOrder(Order.desc("id")).setMaxResults(1), AkunPembayaranSiswa.class));
					if (akunPembayaranSiswa == null && !kode_akun_bni.trim().isEmpty()) {
						Akun akun = (Akun) session.createCriteria(Akun.class)
								.add(Restrictions.eq("satuanKerja", sekolah.getSatuanKerja()))
								.add(Restrictions.ilike("kode", kode_akun_bni, MatchMode.START)).setMaxResults(1)
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

					if (akunPembayaranSiswa == null) {
						akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(
								session.createCriteria(AkunPembayaranSiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekolah))
										.setMaxResults(1),
								AkunPembayaranSiswa.class);
					}

					session.createSQLQuery(
							"delete from sekolah.pembayaran_siswa where bni_request_id = " + bniRequest.getId())
							.executeUpdate();
					List<BniRequestDetail> bniRequestDetails = session.createCriteria(BniRequestDetail.class)
							.add(Restrictions.eq("bniRequest", bniRequest)).list();
					Map<String, List<BniRequestDetail>> maps = new HashMap<String, List<BniRequestDetail>>();
					for (BniRequestDetail bniRequestDetail : bniRequestDetails) {
						try {
							String key = bniRequestDetail.getTagihan().getSiswa() != null
									? bniRequestDetail.getTagihan().getSiswa().getId().toString()
									: bniRequestDetail.getTagihan().getCalonSiswa().getId().toString();
							if (maps.containsKey(key)) {
								maps.get(key).add(bniRequestDetail);
							} else {
								List<BniRequestDetail> dataRequest = new ArrayList<BniRequestDetail>();
								dataRequest.add(bniRequestDetail);
								maps.put(key, dataRequest);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:534");
							// TODO: handle exception
						}
					}
					Double totalSemua = 0.0;
					PembayaranSiswa pembayaranSiswa = null;
					for (List<BniRequestDetail> details : maps.values()) {
						System.out.println("details -> " + details);
						BniRequestDetail jenisBiayaSekolah = details.get(0);
						Double total = 0.0;
						for (BniRequestDetail bniRequestDetail : details) {
							total += (bniRequestDetail.getTagihan().getNominal()
									+ bniRequestDetail.getTagihan().getDenda());
						}
						totalSemua += total;

						pembayaranSiswa = new PembayaranSiswa();
						pembayaranSiswa.setBulan(jenisBiayaSekolah.getTagihan().getBulan());
						pembayaranSiswa.setTahun(jenisBiayaSekolah.getTagihan().getTahun());
						pembayaranSiswa.setTahunDanBulan(jenisBiayaSekolah.getTagihan().getTahunbulan());
						pembayaranSiswa.setSiswa(bniRequest.getSiswa());
						pembayaranSiswa.setCalonSiswa(bniRequest.getCalonSiswa());
						pembayaranSiswa.setJenisBiayaSekolah(jenisBiayaSekolah.getTagihan().getNominalBiaya()
								.getPengaturanBiaya().getJenisBiayaSekolah());
						pembayaranSiswa.setTanggal(
								tanggalTransaksi == null ? ais.ui.util.WaktuUtil.getDate() : tanggalTransaksi);
						pembayaranSiswa.setKeterangan(bniRequest.getKeterangan());
						pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
						pembayaranSiswa.setNominal(total);
						pembayaranSiswa.setDariTabungan(bniRequest.getDeposit());
						pembayaranSiswa.setDariTabunganManual(bniRequest.getDeposit());
						pembayaranSiswa.setBniRequest(bniRequest);
						// pembayaranSiswa.setTotalDeposit(total);
						pembayaranSiswa.setTambahanDeposit(total);
						pembayaranSiswa.setValidator(Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());

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

						for (BniRequestDetail bniRequestDetail : details) {
							try {
								Tagihan tagihan = bniRequestDetail.getTagihan();
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
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bniresponse.java:616");
							}
						}
					}

					try {

						Double deposit = bniRequest.getAmount() - totalSemua;

						if (bniRequest.getSiswa() != null) {

							DepositSiswa depositSiswa = (DepositSiswa) session.createCriteria(DepositSiswa.class)
									.add(Restrictions.eq("bniRequest", bniRequest)).setMaxResults(1).uniqueResult();
							if (depositSiswa == null) {
								depositSiswa = new DepositSiswa();
							}
							depositSiswa.setSiswa(bniRequest.getSiswa());
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
							depositSiswa.setBniRequest(bniRequest);
							depositSiswa.setValidator(Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());
							depositSiswa.setKeterangan("Tabungan");

							session.getTransaction().begin();
							session.saveOrUpdate(depositSiswa);
							session.getTransaction().commit();

							PembayaranSiswaUtil.cetakDeposit(depositSiswa);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:653");
//						e.printStackTrace();
					}

					if (!maps.isEmpty()) {
						try {
							PembayaranSiswaUtil.cetakBni(pembayaranSiswa, bniRequest);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:660");
//							e.printStackTrace();
						}
					}

					if (bniRequest != null && bniRequest.getCalonSiswa() != null
							&& bniRequest.getCalonSiswa().getId() != null) {
						bniRequest.getCalonSiswa().populatePembayaran();
					}

					if (bniRequest != null && bniRequest.getSiswa() != null && bniRequest.getSiswa().getId() != null) {
						bniRequest.getSiswa().populatePembayaran();
					}

				} else {

					List<BniRequestDetail> bniRequestDetailsTemp = session.createCriteria(BniRequestDetail.class)
							.add(Restrictions.isNull("idCicilan")).add(Restrictions.eq("bniRequest", bniRequest))
							.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();

					Map<Long, List<BniRequestDetail>> maps = new HashMap<Long, List<BniRequestDetail>>();
					Map<Long, Kegiatan> mapKegiatan = new HashMap<Long, Kegiatan>();
					for (BniRequestDetail bniRequestDetail : bniRequestDetailsTemp) {
						if (bniRequestDetail.getDetailBiaya() != null
								&& bniRequestDetail.getDetailBiaya().getJenisKegiatan() != null) {
							Kegiatan kegiatanD = Bniresponse.createKegiatan(bniRequest,
									bniRequestDetail.getDetailBiaya().getJenisKegiatan(), session);
							List<BniRequestDetail> m = maps.get(kegiatanD.getId());
							if (m == null) {
								m = new ArrayList<BniRequestDetail>();
								maps.put(kegiatanD.getId(), m);
								mapKegiatan.put(kegiatanD.getId(), kegiatanD);
							}
							m.add(bniRequestDetail);
						}
					}

					System.out.println("maps -> " + maps + ", mapKegiatan -> " + mapKegiatan);

					class Proses {
						public void doProses(Kegiatan kegiatan, List<BniRequestDetail> bniRequestDetails,
								Session session, Date tanggalTransaksi, BniResponse bniResponse, BniRequest bniRequest,
								JenisPembayaran jenisPembayaran) {
							System.out.println("bniRequestDetails==>" + bniRequestDetails);
							if (!bniRequestDetails.isEmpty()) {

								for (BniRequestDetail bniRequestDetail : bniRequestDetails) {

									String ref = "bniRequestDetail-" + bniRequestDetail.getId();

									CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
											.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
											.setMaxResults(1).uniqueResult();
									if (cicilanPembayaran == null) {
										cicilanPembayaran = new CicilanPembayaran(bniRequestDetail.getDetailBiaya());
									}
									cicilanPembayaran
											.setTanggal(tanggalTransaksi == null ? bniRequestDetail.getTanggal()
													: tanggalTransaksi);
									cicilanPembayaran.setRef(ref);
									cicilanPembayaran.setValidator(
											Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());
									cicilanPembayaran.setKe(bniRequestDetail.getKe());
									cicilanPembayaran.setKegiatan(kegiatan);
									cicilanPembayaran.setRefVa(bniRequest.getId());
									cicilanPembayaran.setItemBiaya(bniRequestDetail.getItemBiaya());
									cicilanPembayaran.setPengaturanPembayaranBulanan(
											bniRequestDetail.getPengaturanPembayaranBulanan());
									cicilanPembayaran.setNilai(bniRequestDetail.getNilai());
									cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
									cicilanPembayaran.setDenda(bniRequestDetail.getDenda());
									cicilanPembayaran.setNilaiAsli(bniRequestDetail.getNilaiAsli());
									session.getTransaction().begin();
									if (cicilanPembayaran.getId() == null)
										session.save(cicilanPembayaran);
									else
										Common.refreshUpdate(session, cicilanPembayaran);
									session.getTransaction().commit();

									bniRequestDetail.setCicilanref(cicilanPembayaran.getId());
									session.getTransaction().begin();
									session.update(bniRequestDetail);
									session.getTransaction().commit();
								}

							} else if (bniRequest.getCicilan() != null && !bniRequest.getCicilan().isEmpty()) {
								VirtualAccountBank.populateCicilan(session, bniRequest.getCicilan(), kegiatan,
										bniRequest.getId(), tanggalTransaksi, jenisPembayaran, "bni");
							}

							Double nilaiBiayaHarusDiBayars = bniRequest.getNilaiBiayaHarusDiBayars();
							Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
							Double jumlah = d[0];
							Double denda = d[1];
							kegiatan.setDenda(denda.doubleValue());
							kegiatan.setAmountTerhutang(
									nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
							kegiatan.setAmount(jumlah.doubleValue());

							kegiatan.setValidator(Common.getKonfigurasi("default_validator_bni", "BNI").getNilai());

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, kegiatan);
							session.getTransaction().commit();

							if (bniRequest.getAmount() > 0.1) {
								LogPembayaran logPembayaran = (LogPembayaran) session
										.createCriteria(LogPembayaran.class)
										.add(Restrictions.eq("bniRequest", bniRequest)).setMaxResults(1).uniqueResult();
								if (logPembayaran == null) {
									logPembayaran = new LogPembayaran();
								}

								double biayaAdministrasi = 0.0;
								try {
									biayaAdministrasi = Double.parseDouble(
											Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:777");

								}
								logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
								logPembayaran.setKegiatan(kegiatan);
								logPembayaran.setBniRequest(bniRequest);
								logPembayaran.setNominal(bniRequest.getAmount());
								logPembayaran.setKeterangan(bniResponse.getKeterangan());
								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, logPembayaran);
								session.getTransaction().commit();
							}

							pembayaranUtil.updateTunggakan(kegiatan, session);
						KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
						}
					}

					Proses proses = new Proses();

					if (mapKegiatan.size() > 1) {

						for (Kegiatan kegiatanD : mapKegiatan.values()) {
							kegiatan = kegiatanD;
							if (hapusDulu) {
								session.createSQLQuery("delete from cicilan_pembayaran where ref_va = "
										+ bniRequest.getId() + " and kegiatan=" + kegiatanD.getId()).executeUpdate();
							}

							List<BniRequestDetail> bniRequestDetails = maps.get(kegiatanD.getId());
							proses.doProses(kegiatanD, bniRequestDetails, session, tanggalTransaksi, bniResponse,
									bniRequest, jenisPembayaran);
						}
					} else {
						kegiatan = Bniresponse.createKegiatan(bniRequest, session);
						proses.doProses(kegiatan, bniRequestDetailsTemp, session, tanggalTransaksi, bniResponse,
								bniRequest, jenisPembayaran);
					}

				}
			}
		}

		try {
			if (kegiatan != null && kegiatan.getId() != null) {
				Mahasiswa mhs = bniRequest.getMahasiswa();
				BiodataCalonMahasiswa bio = bniRequest.getBiodataCalonMahasiswa();
				if (mhs != null) {
					CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
				} else if (bio != null) {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bniresponse.java:831");
		}

		return bniRequest;
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:837");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:838");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:839");}
			}
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), "BNI");
		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> BniResponse data => " + data);
		System.out.println("==> BniResponse querystring => " + querystring);

		String trx_id = null;
		BniRequest bniRequest = null;
		JSONObject bni = null;
		// Jejak stack trace bila pemrosesan callback/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;

		/* Callback BNI kadang berisi body kosong / bukan JSON (health-check,
		 * scanner, atau error gateway). Jangan lempar ParseException ke log
		 * error; cukup tolak dengan respons singkat. */
		if (data == null || data.trim().length() == 0 || !data.trim().startsWith("{")) {
			try {
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write("{\"status\":\"999\",\"message\":\"payload bukan JSON\"}");
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:873");
			}
			return;
		}

		try {

			JSONObject jsonObject = new JSONObject(data);
			String parsedData = jsonObject.getString("data");
			String merchant_id = jsonObject.getString("client_id");
			String Password = Common.getKonfigurasi("bni_password", "685dedd9f045787873794ead6276f8bf").getNilai()
					.trim();

			{
				Session session = null;
				Sekolah sekolah = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					sekolah = (Sekolah) ConstantValues.simpleObject(session.createCriteria(Sekolah.class)
							.add(Restrictions.ilike("bniMerchantId", merchant_id, MatchMode.ANYWHERE)).setMaxResults(1),
							Sekolah.class);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bniresponse.java:895");
				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:898");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:899");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:900");}
					}
				}

				try {
					Password = sekolah != null && !sekolah.getBniPassword().isEmpty() ? sekolah.getBniPassword().trim()
							: Common.getKonfigurasi("bni_password", "685dedd9f045787873794ead6276f8bf").getNilai().trim();

					String bniPassword = sekolah != null && !sekolah.getBniPassword().isEmpty() ? sekolah.getBniPassword()
							: null;

					if (bniPassword != null && bniPassword.length() > 0) {

						String bniMerchantId = sekolah.getBniMerchantId();
						System.out.println("chek bniPassword " + bniPassword + " bniMerchantId " + bniMerchantId);
						Integer tahun = 0;
						for (String p : StringUtils.split(bniMerchantId, ";")) {
							try {
								String[] a = StringUtils.split(p, ":");
								// Entri bniMerchantId malformed (mis. tanpa ":") menghasilkan array
								// dengan panjang < 2; cek panjang dulu sebelum indexing agar tidak AIOOBE.
								if (a != null && a.length > 1 && a[1].equalsIgnoreCase(merchant_id)) {
									tahun = Integer.parseInt(a[0]);
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:922");
//							e.printStackTrace();
							}
						}

						System.out.println("chek merchant_id " + merchant_id + " tahun " + tahun);

						if (!tahun.equals(0)) {

							if (StringUtils.contains(bniPassword, ":")) {
								for (String s : StringUtils.split(bniPassword, ";")) {
									String[] sArr = StringUtils.split(s, ":");
									if (sArr == null || sArr.length < 2) {
										// entri bniPassword malformed (tanpa ":"); lewati agar tidak AIOOBE
										continue;
									}
									Integer ta = Integer.parseInt(sArr[0]);
									String m = sArr[1];
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

						System.out.println("chek merchant_id " + merchant_id + " tahun " + tahun);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bniresponse.java:950");
				}
			}

			System.out.println("==> BniResponse merchant_id " + merchant_id + ", Password " + Password + ", parsedData "
					+ parsedData);

			String decodeData = BNIHash.parseData(parsedData, merchant_id, Password);
			System.out.println("==> BniResponse decoded => " + decodeData);
			bni = new JSONObject(decodeData);

			trx_id = bni.getString("trx_id");

			System.out.println("==> BniResponse trx_id => " + trx_id + ", bniResponse = " + bni);
			if (trx_id != null && !trx_id.trim().isEmpty()) {

				String payment_amount = bni.getString("payment_amount");
				String trx_amount = bni.getString("trx_amount");

				System.out.println("payment_amount = " + payment_amount + ", trx_amount = " + trx_amount);

				String status = payment_amount.trim().equals(trx_amount.trim()) ? "000" : "001";

				Session session2 = null;
				BniResponse bniResponse = null;
				try {
					session2 = HibernateUtil.getSessionFactory().openSession();
					bniResponse = (BniResponse) session2.createCriteria(BniResponse.class)
							.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).addOrder(Order.desc("id"))
							.uniqueResult();
					if (bniResponse == null) {
						bniResponse = new BniResponse();
					}
					bniResponse.setKeterangan(bni.toString());
					bniResponse.setNama(trx_id);
					bniResponse.setStatus(BniResponse.SEDANG_DIPROSES);
					bniResponse.setMerchant(merchant_id);
					bniResponse.setTrxId(trx_id);
					bniResponse.setCallback(data);
					bniResponse.setKodeStatus(status);
					session2.getTransaction().begin();
					session2.saveOrUpdate(bniResponse);
					session2.getTransaction().commit();
				} finally {
					if (session2 != null) {
						try { session2.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:995");}
						try { session2.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:996");}
						try { session2.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bniresponse.java:997");}
					}
				}
				if (bniResponse != null) {
					bniRequest = prosesResponse(bniResponse, false);
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
				logHostToHost.setNim(bniRequest == null ? ""
						: (bniRequest.getMahasiswa() != null ? bniRequest.getMahasiswa().getNim()
								: bniRequest.getBiodataCalonMahasiswa() != null
										? bniRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
										: ""));
				logHostToHost.setKode(trx_id);
				logHostToHost.setNama(bniRequest == null ? ""
						: (bniRequest.getMahasiswa() != null ? bniRequest.getMahasiswa().getNama()
								: bniRequest.getBiodataCalonMahasiswa() != null
										? bniRequest.getBiodataCalonMahasiswa().getNama()
										: ""));
				logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
				logHostToHost
						.setResponseDescription(bniRequest == null ? "Gagal" : "Sukses (" + bniRequest.getResponse() + ")");
				logHostToHost.setNominal(
						bniRequest == null ? 0.0 : (bniRequest.getAmount() + bniRequest.getBiayaAdministrasi()));
				logHostToHost.setItem(bni == null ? "" : bni.toString());
				logHostToHost.setStackTrace(h2hStackTrace);
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
//		String body = "{ \"status\" : \"999\" }";
		String body = bniRequest == null ? "{ \"status\" : \"001\" }" : "{ \"status\" : \"000\" }";

		if (Common.bolehKonfigurasi("bni_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
//			Thread.sleep(3 * 1000);
			body = "{ \"status\" : \"001\" }";
		}

		System.out.println(body);
		writer.write(body);

	}

	public static void main(String[] argv) {
		String parsedData = "TSRNTyEoSx5SFxhiDlJXVmRdR1YAX0xFU35nVk5fCnhLCFsLCmI7UhEjSiNTFhoXDhsETGRkWlEETllFUX5bTgsrOGsrYzlXPDxuag88WjlqJz4wDhsETVBlS1YAVkxFU35nVk5fCjskPR5GTh9GSCgfSyE8FxohHyYcHR8TEgR7SltLVwZbTkhhdxJXAFoKe1cMBycoSRw-IAwZHCETFh8qExRNPRgWHVAlIx0hRFAjTRdGUyhJSBEeO1t9X1dMWmNBSlxgW1ALCyEIFE4mIBkhRjsWPVx3FVt-BmNRB19-CCQJISIZHiInFhtJHxcZFlQhIRkhRk0fTA5CPmILEE5TBloRVF4JJhETGicoFhJHCxMIVw9mSFJVOFMMVCBNXyZMTDEkXB1iCBYJYlhUXWRSUkF4TEpVWAtiCyMTS1IhTyJGPms";
		String merchant_id = "9556";
		String Password = "5dc99d6f-5266-4981-a021-d9438ad4b7af";
		String decodeData = BNIHash.parseData(parsedData, merchant_id, Password);
		System.out.println("==> BniResponse decoded => " + decodeData);
	}

}
