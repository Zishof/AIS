package ais.action.servlet.api;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.JenisKegiatanPrasyaratAction;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BniCommon;
import ais.common.Common;
import ais.common.OnlineBmtUtil;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.VirtualAccountBank;
import ais.database.model.bni.BniRequest;
import ais.database.model.bni.BniRequestDetail;
import ais.database.model.bni.BniRequestDetailBiaya;
import ais.ui.util.MyDoublebox;
import ais.ui.util.WaktuUtil;

/**
 * Kumpulan handler API (dipanggil oleh dispatcher servlet API dengan konvensi
 * {@code (HttpServletRequest, JSONObject) -> JSONObject}, mengikuti pola yang sama seperti
 * {@link TabunganSiswa}) untuk fitur tagihan dan pembayaran mahasiswa via aplikasi mobile:
 * mengambil rincian tagihan per semester ({@link #tagihan}), mengambil riwayat kode Virtual
 * Account (VA) yang pernah dibuat ({@link #daftar_va}), dan membuat kode VA baru untuk membayar
 * satu atau lebih item tagihan terpilih ({@link #va}).
 *
 * <p>
 * Setiap handler memvalidasi token akses lewat {@code ApiUtil#currentUser} lebih dulu. Kode
 * status pada respons JSON mengikuti konvensi: {@code "00"} = berhasil, {@code "01"}/{@code
 * "02"}/{@code "03"} = kegagalan bisnis spesifik (token bukan mahasiswa, tagihan kosong, bank
 * tidak dikenal), {@code "90"} = galat internal, {@code "97"} = token tidak valid. Perhitungan
 * tagihan menghormati dua metode biaya: <i>insidentil</i> ({@link DetailBiaya} langsung) dan
 * <i>bulanan/cicilan</i> ({@link PengaturanPembayaranBulanan}, dengan kemungkinan denda
 * keterlambatan dihitung otomatis atau kustom per {@link DetailKegiatan}); nilai
 * {@code ItemBiaya.DIKALI_NILAI_MINUS} membalik tanda nominal menjadi kredit (mis. potongan/
 * beasiswa). Pembuatan VA di {@link #va} bercabang menurut bank tujuan: rute BNI memakai
 * {@link BniRequest}/{@link BniRequestDetail} lewat {@link BniCommon}, sedangkan bank lain
 * (Mandiri/Online/Smartlink/BCA/MNC/BSI/BTN/Finpay/Flip) memakai {@link VirtualAccountBank} lewat
 * jalur pembayaran umum.
 * </p>
 */
public class TagihanMahasiswa {

	/**
	 * Mengambil rincian tagihan mahasiswa (pemilik token) untuk satu semester ({@code smt}),
	 * dipecah per {@link JenisKegiatan} aktif (mis. SPP, her-registrasi) dan, di dalamnya, per
	 * item biaya insidentil atau per bulan cicilan. Untuk setiap jenis kegiatan, prasyaratnya
	 * diperiksa lebih dulu lewat {@code JenisKegiatanPrasyaratAction#checkSyarat}; kegiatan
	 * pendaftaran (calon mahasiswa/her-registrasi mahasiswa baru) memakai jalur perhitungan
	 * biaya berbeda ({@code PembayaranUtilHelper#getDetailBiayaCalonMahasiswa}) berdasarkan
	 * prodi lulus/pilihan calon mahasiswa, sementara mahasiswa aktif biasa memakai
	 * {@code PembayaranUtilHelper#getDetailBiayaMahasiswa}. Setiap baris tagihan menghitung
	 * jumlah tertagih, total sudah dibayar (dari {@link CicilanPembayaran} tersimpan), sisa
	 * belum dibayar, dan (untuk metode bulanan) denda keterlambatan berdasarkan tanggal
	 * pembayaran cicilan sebelumnya bila ada. Hanya baris dengan tagihan positif (&gt; 0.1) yang
	 * disertakan dalam hasil.
	 *
	 * @param req     request HTTP asli, sumber header autentikasi
	 * @param request payload JSON berisi {@code smt} (nomor semester) dan {@code refresh}
	 *                (opsional, paksa hitung ulang cache biaya bila {@code true})
	 * @return JSON berisi daftar baris tagihan, biaya administrasi, dan bank VA mobile yang
	 *         tersedia, atau JSON berisi {@code status} kegagalan/peringatan (mis. prasyarat
	 *         kegiatan belum terpenuhi) bila ada
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject tagihan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
//				Integer smt_mulai = Integer.parseInt(request.getString("smt_mulai"));
//				Integer smt_sampai = Integer.parseInt(request.getString("smt_sampai"));

				Integer smta = Integer.parseInt(request.getString("smt"));
//				Integer smt_sampai = Integer.parseInt(request.getString("smt_sampai"));
				Integer smt_mulai = smta;
				Integer smt_sampai = smta;
				Boolean refresh = request.isNull("refresh") ? false
						: Boolean.parseBoolean(request.getString("refresh"));
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				List<String> warnings = new ArrayList<String>();

				if (mahasiswa != null) {

					BiodataCalonMahasiswa calonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();

					Session session = HibernateUtil.openSession();
					try {
					List<JenisKegiatan> jenisKegiatans = ConstantValues.simpleList(
							session.createCriteria(JenisKegiatan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("namaKegiatan")),
							JenisKegiatan.class);

					System.out.println("jenisKegiatans => " + jenisKegiatans.size());
					JSONArray jsonArray = new JSONArray();
					for (JenisKegiatan jk : jenisKegiatans) {

						if (calonMahasiswa == null && ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
								&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jk.getId()))
								|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
										&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
												.equals(jk.getId())))) {
							continue;
						}

						Integer mulai = smt_mulai;
						Integer sampai = smt_sampai;
						if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
								&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jk.getId())

						) {
							mulai = 0;
							sampai = 0;
						}
						List<CicilanPembayaran> cicilanPembayarans;

						if (calonMahasiswa != null
								&& (jk.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())
										|| jk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))

						) {
							cicilanPembayarans = calonMahasiswa.ambilCicilan(jk, refresh);
						} else {
							cicilanPembayarans = mahasiswa.ambilCicilan(jk, refresh);
						}

						for (int smt = mulai; smt <= sampai; smt++) {
							Kegiatan kegiatan = mahasiswa.ambilKegiatans(smt, jk, refresh);

							boolean starat = JenisKegiatanPrasyaratAction.checkSyarat(mahasiswa, jk, smt,
									kegiatan == null ? null : kegiatan.getTahunAkademik(),
									(smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL), warnings);
							if (starat) {

								Collection<DetailKegiatan> detailKegiatans = kegiatan == null
										|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(refresh);

								Collection detailBiayas = new ArrayList();
								int countPengaturanBulanan = 0;
								if (calonMahasiswa != null
										&& jk.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {

									Jurusan prodiLulus = calonMahasiswa.getProdiLulus();

									if (prodiLulus == null || prodiLulus.getId() == null) {
										Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null
												? calonMahasiswa.getProdi2()
												: calonMahasiswa.getProdi1();
										java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
												.getDetailBiayaCalonMahasiswa(calonMahasiswa, jk, myjurusan1, smt,
														refresh);

										detailBiayas = detailBiayas1;
									} else {
										java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
												.getDetailBiayaCalonMahasiswa(calonMahasiswa, jk, prodiLulus, smt,
														refresh);
										detailBiayas = detailBiayas1;
									}
								} else if (calonMahasiswa != null
										&& jk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
									Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
									if (prodiLulus == null || prodiLulus.getId() == null) {
										Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null
												? calonMahasiswa.getProdi2()
												: calonMahasiswa.getProdi1();
										java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
												.getDetailBiayaCalonMahasiswa(calonMahasiswa, jk, myjurusan1, refresh);
										detailBiayas = detailBiayas1;
									} else {
										java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
												.getDetailBiayaCalonMahasiswa(calonMahasiswa, jk, prodiLulus, refresh);
										detailBiayas = detailBiayas1;
									}
									countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, calonMahasiswa,
											jk, smt, detailBiayas, refresh, false);

									if (countPengaturanBulanan > 0) {
										detailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
												calonMahasiswa, session, smt, jk, detailBiayas, refresh, false);
									}

								} else {
									countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jk,
											smt, detailBiayas, refresh, false);
									detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jk,
											refresh);

									if (countPengaturanBulanan > 0) {
										detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jk,
												countPengaturanBulanan > 0 ? "-1" : null, true, refresh);
									}
								}

								if (detailBiayas.isEmpty()) {
									continue;
								}

								List<Object[]> dataCicilan = new ArrayList<Object[]>();

								for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
									try {
										if (cicilanPembayaran.getKegiatan().getJenisKegiatan() != null) {
											dataCicilan.add(new Object[] { cicilanPembayaran.getId(),
													cicilanPembayaran.getItemBiaya().getId(),
													cicilanPembayaran.getPengaturanPembayaranBulanan() == null ? -1
															: cicilanPembayaran.getPengaturanPembayaranBulanan()
																	.getRealBulan(),
													cicilanPembayaran.getNilai(),
													cicilanPembayaran.getKegiatan().getId(),
													cicilanPembayaran.getKegiatan().getJenisKegiatan() });
										}
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:211");
										// TODO: handle exception
									}
								}
								Double totalTagihan = 0.0;
								Double totalDibayar = 0.0;
								Double totalBelumDibayar = 0.0;

								for (Object o : detailBiayas) {

									DetailBiaya tempdetailBiaya = null;
									PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;
									if (o instanceof DetailBiaya) {
										tempdetailBiaya = (DetailBiaya) o;

									} else if (o instanceof PengaturanPembayaranBulanan) {
										temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
										if (temppengaturanPembayaranBulanan != null) {
											tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
										}

									}

									DetailKegiatan tempdata = kegiatan == null ? null
											: temppengaturanPembayaranBulanan != null
													? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan,
															detailKegiatans)
													: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);

									DetailKegiatan detailKegiatan = tempdata;
									if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
										continue;
									}

									if (o instanceof PengaturanPembayaranBulanan) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;

										Double jumlah = Kegiatan.ambilJumlahTagihan(detailKegiatan,
												pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, mahasiswa, smt,
												pengaturanPembayaranBulanan);
										Date tanggalBayar = WaktuUtil.getDate();
										for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
											try {
												if (cicilanPembayaran.getItemBiaya() != null
														&& cicilanPembayaran.getItemBiaya().getId()
																.equals(pengaturanPembayaranBulanan.getDetailBiaya()
																		.getItemBiaya().getId())
														&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
																.equals(cicilanPembayaran.getBayarKe())
														&& kegiatan != null && cicilanPembayaran.getKegiatan().getId()
																.equals(kegiatan.getId())) {

													if (pengaturanPembayaranBulanan != null && cicilanPembayaran
															.getPengaturanPembayaranBulanan() != null) {
														PengaturanPembayaranBulanan p = cicilanPembayaran
																.getPengaturanPembayaranBulanan();
														if (p.getDetailBiaya().getItemBiaya().getId()
																.equals(pengaturanPembayaranBulanan.getDetailBiaya()
																		.getItemBiaya().getId())
																&& p.getRealBulan().equals(
																		pengaturanPembayaranBulanan.getRealBulan())) {
															tanggalBayar = cicilanPembayaran.getTanggal();
														}
													}
												}
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:276");
												// TODO: handle exception
											}
										}

										Double hasilDenda = detailKegiatan != null
												&& (detailKegiatan.getBatalkanDenda() || jumlah.intValue() == 0)
														? jumlah
														: detailKegiatan != null
																&& detailKegiatan.getMenggunakanDendaCustom() ? jumlah
																		: pengaturanPembayaranBulanan.checkDenda(jumlah,
																				tanggalBayar, null, jk);

										if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()) {
											pengaturanPembayaranBulanan.setInfoDenda(" Penambahan denda senilai "
													+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom())
													+ ".");
										}

										Double nilaiDenda = hasilDenda - jumlah;

										Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan,
												pengaturanPembayaranBulanan, cicilanPembayarans);

										jumlah = hasilDenda.intValue() > jumlah.intValue() ? hasilDenda : jumlah;

										if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
												.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
											jumlah = -Math.abs(jumlah);
											telahDibayar = (telahDibayar == null ? 0.0
													: -Math.abs(telahDibayar.doubleValue()));
										}

										totalTagihan += jumlah;

										totalDibayar += (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

										Double belumDibayar = jumlah
												- (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

										totalBelumDibayar += belumDibayar;

										if (jumlah > 0.1) {
											JSONObject js = new JSONObject();

											js.put("grup_kode", jk.getKode());
											js.put("grup_nama", jk.getNamaKegiatan());
											js.put("grup_ta", jk.getNamaKegiatan() + " semester "
													+ pengaturanPembayaranBulanan.getDetailBiaya().getSemester());
											js.put("nilaiDenda", nilaiDenda);
											js.put("metode", "Bulanan");
											js.put("bulan", pengaturanPembayaranBulanan.getRealBulan());
											js.put("bulan_tahun", pengaturanPembayaranBulanan.getRealBulanTahun());
											js.put("nama_bulan",
													pengaturanPembayaranBulanan.getNamaBulan()
															+ (pengaturanPembayaranBulanan.getDeadline() == null ? ""
																	: ", Deadline:" + Common.dateFormat1.get().format(
																			pengaturanPembayaranBulanan.getDeadline()))
															+ (nilaiDenda > 0.1
																	? ", " + pengaturanPembayaranBulanan.getInfoDenda()
																	: ""));
											js.put("id", pengaturanPembayaranBulanan.getId());
											js.put("kode", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getKode());
											js.put("nama",
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
															.getNama() + " bulan "
															+ pengaturanPembayaranBulanan.getNamaBulan());
											js.put("boleh_angsur", pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya().getMahasiswaBolehMencicilkan());

											js.put("nama_mahasiswa", mahasiswa.getNama());
											js.put("nama_jurusan", mahasiswa.getJurusan().getNama());
											js.put("telahDibayar", telahDibayar);
											js.put("belumDibayar", belumDibayar);
											js.put("tanggalDeadline",
													pengaturanPembayaranBulanan.getDeadline() == null ? ""
															: Common.dateFormat1.get()
																	.format(pengaturanPembayaranBulanan.getDeadline()));
											js.put("tagihan", jumlah);
											jsonArray.put(js);
										}
									} else {

										DetailBiaya detailBiaya = (DetailBiaya) o;

										Double jumlah = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan,
												detailBiaya, refresh);

										Double nilaiDenda =

												detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()
														? detailKegiatan.getDendaCustom()
														:

														0.0;

										jumlah += nilaiDenda;

										Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya,
												cicilanPembayarans);

										if (detailBiaya.getItemBiaya().getPenghitungan()
												.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
											jumlah = -Math.abs(jumlah);
											telahDibayar = (telahDibayar == null ? 0.0
													: -Math.abs(telahDibayar.doubleValue()));
										}

										totalTagihan += jumlah;

										totalDibayar += (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

										Double belumDibayar = jumlah
												- (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

										totalBelumDibayar += belumDibayar;
										if (jumlah > 0.1) {
											JSONObject js = new JSONObject();
											js.put("grup_kode", jk.getKode());
											js.put("grup_nama", jk.getNamaKegiatan());
											js.put("nilaiDenda", nilaiDenda);
											js.put("grup_ta",
													jk.getNamaKegiatan() + " semester " + detailBiaya.getSemester());
											js.put("metode", "Insidentil");
											js.put("bulan", 0);
											js.put("bulan_tahun", 0);
											js.put("nama_bulan",
													(detailKegiatan != null
															&& detailKegiatan.getMenggunakanDendaCustom()
																	? "Tambahan denda " + Common.numberFormat.get()
																			.format(detailKegiatan.getDendaCustom())
																	: ""));
											js.put("boleh_angsur",
													detailBiaya.getItemBiaya().getMahasiswaBolehMencicilkan());
											js.put("id", detailBiaya.getId());
											js.put("kode", detailBiaya.getItemBiaya().getKode());
											js.put("nama", detailBiaya.getItemBiaya().getNama());
											js.put("nama_mahasiswa", mahasiswa.getNama());
											js.put("nama_jurusan", mahasiswa.getJurusan().getNama());
											js.put("telahDibayar", telahDibayar);
											js.put("belumDibayar", belumDibayar);
											js.put("tanggalDeadline", "");
											js.put("tagihan", jumlah);
											jsonArray.put(js);
										}
									}
								}
							}
						}

					}

					if (warnings.isEmpty()) {

						double biayaAdministrasi = 0.0;
						try {
							biayaAdministrasi = Double
									.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:435");

						}

						jsonObject.put("biayaAdministrasi", biayaAdministrasi);

						jsonObject.put("data", jsonArray);
						jsonObject.put("status", "00");
						jsonObject.put("description", "Pengambilan data berhasil");
						Long ptId = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getId();
						String configuredBanks = Common.getKonfigurasi("bank_va_mobile_pt_" + ptId,
								"BNI,MANDIRI").getNilai();
						jsonObject.put("bank_va_mobile", OnlineBmtUtil.appendToConfiguredBanks(configuredBanks,
								OnlineBmtUtil.isPerguruanTinggiReady(ptId)));
					} else {

						String w = "";
						for (String s : warnings) {
							w += w.isEmpty() ? s : "\n" + s;
						}

						jsonObject.put("status", "01");
						jsonObject.put("description", w);
					}
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Pengambilan data gagal, token harus sebagai mahasiswa");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanMahasiswa.java:474");
			}
		}
		return jsonObject;
	}

	/** Mengurai {@code value} sebagai {@link Double}, mengembalikan {@code fallback} bila {@code value} kosong/{@code null}/tidak valid — menghindari try-catch kosong berulang di pemanggil. */
	private static Double parseDoubleSafe(String value, Double fallback) {
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}
		try {
			return Double.parseDouble(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	/**
	 * Mengambil hingga 50 kode Virtual Account (VA) terbaru yang pernah dibuat mahasiswa (pemilik
	 * token) untuk {@code bank} tertentu: rute BNI membaca dari {@link BniRequest} (status
	 * {@code "Payment Sukses"} dianggap lunas), rute bank lain (kosong/Mandiri/Online/Smartlink/
	 * BCA/MNC Bank/BSI/BTN) membaca dari {@link VirtualAccountBank} (dianggap lunas bila kolom
	 * {@code kegiatan} terisi).
	 *
	 * @param req     request HTTP asli, sumber header autentikasi
	 * @param request payload JSON berisi {@code bank} (nama bank atau kosong)
	 * @return JSON berisi daftar VA (kode, status lunas, total, tanggal kadaluarsa), atau JSON
	 *         berisi {@code status} galat bila token tidak valid
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject daftar_va(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				String bank = request.getString("bank");
				if (bank.isEmpty() || bank.equalsIgnoreCase("MANDIRI") || bank.equalsIgnoreCase("Online")
						|| bank.equalsIgnoreCase("Smartlink") || bank.equalsIgnoreCase("BCA")
						|| bank.equalsIgnoreCase("MNC Bank") || bank.equalsIgnoreCase("BSI")
						|| bank.equalsIgnoreCase("BTN") || bank.equalsIgnoreCase(OnlineBmtUtil.BANK_NAME)) {
					Session session = HibernateUtil.openSession();
					try {
					List<Object[]> objects = session.createCriteria(VirtualAccountBank.class)

							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("kegiatan")).add(Projections.property("total"))
									.add(Projections.property("kadaluarsa")))

							.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())).addOrder(Order.desc("id"))
							.setMaxResults(50).list();

					JSONArray jsonArray = new JSONArray();
					for (Object[] oo : objects) {

						JSONObject js = new JSONObject();
						js.put("va", oo[0]);
						js.put("lunas", oo[1] != null);
						js.put("total", oo[2]);
						js.put("kadaluarsa", oo[3] == null ? "" : Common.dateFormat3.get().format(oo[3]));

						jsonArray.put(js);
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else if (bank.isEmpty() || bank.equalsIgnoreCase("BNI")) {
					Session session = HibernateUtil.openSession();
					try {
					List<Object[]> objects = session.createCriteria(BniRequest.class)

							.setProjection(Projections.projectionList().add(Projections.property("va"))
									.add(Projections.property("status")).add(Projections.property("amount"))
									.add(Projections.property("billExpired")))

							.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())).addOrder(Order.desc("id"))
							.setMaxResults(50).list();

					JSONArray jsonArray = new JSONArray();
					for (Object[] oo : objects) {

						JSONObject js = new JSONObject();
						js.put("va", oo[0]);
						js.put("lunas", oo[1] != null && oo[1].equals("Payment Sukses"));
						js.put("total", oo[2]);
						js.put("kadaluarsa", oo[3] == null ? "" : Common.dateFormat3.get().format(oo[3]));

						jsonArray.put(js);
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");

					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanMahasiswa.java:577");
			}
		}
		return jsonObject;
	}

	/**
	 * Membuat kode Virtual Account (VA) baru untuk membayar satu atau lebih baris tagihan
	 * terpilih (identifikasi baris {@link PengaturanPembayaranBulanan} atau {@link DetailBiaya},
	 * tergantung {@code metode}) beserta nominal masing-masing, ditambah opsional nominal
	 * top-up tabungan. Bercabang menurut {@code bank} tujuan: bank Mandiri/Online/Smartlink/BCA/
	 * MNC Bank/BSI/BTN/Finpay/Flip membuat entri {@link VirtualAccountBank} lewat jalur
	 * pembayaran umum; bank BNI membuat {@link BniRequest} beserta rincian
	 * {@link BniRequestDetail}/{@link BniRequestDetailBiaya} lewat {@link BniCommon} (kode VA
	 * dapat pula dipetakan ke format VA bank lain via konfigurasi
	 * {@code prefix_kode_bank_lain_online} bila diisi).
	 *
	 * @param request             payload JSON berisi {@code tagihans} (peta id baris ke nominal),
	 *                            {@code bank}, {@code metode} ({@code "Bulanan"} atau lainnya),
	 *                            dan {@code tabungan} (opsional, nominal top-up)
	 * @param httpServletRequest request HTTP asli, sumber header autentikasi
	 * @return JSON berisi kode VA, rincian tagihan yang tercakup, biaya administrasi, dan
	 *         nominal top-up, atau JSON berisi {@code status} kegagalan (token bukan mahasiswa,
	 *         tagihan kosong, bank tidak dikenal, atau galat internal)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject va(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				JSONObject tagihansParam = new JSONObject(request.get("tagihans") + "");
				String bank = request.getString("bank");
				String metode = request.getString("metode");

				String tabunganStr = request.has("tabungan") && !request.isNull("tabungan")
						? request.getString("tabungan")
						: null;
				Double tabunganNumber = parseDoubleSafe(tabunganStr, 0.0);

				double nilaiYgAkanDibayar = 0.0;
				Map<Long, Double> ids = new HashMap<Long, Double>();
				Iterator<String> iterator = tagihansParam.keys();
				while (iterator.hasNext()) {
					String key = iterator.next();
					Double nominal = Double.parseDouble(tagihansParam.get(key).toString());
					nilaiYgAkanDibayar += nominal;

					ids.put(Long.parseLong(key), nominal);
				}

				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				if (mahasiswa != null) {

					BiodataCalonMahasiswa calonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();

					if (bank.isEmpty() || bank.equalsIgnoreCase("MANDIRI") || bank.equalsIgnoreCase("Online")
							|| bank.equalsIgnoreCase("Smartlink") || bank.equalsIgnoreCase("BSI")
							|| bank.equalsIgnoreCase("BCA") || bank.equalsIgnoreCase("MNC Bank")
							|| bank.equalsIgnoreCase("Finpay") || bank.equalsIgnoreCase("Flip")
							|| bank.equalsIgnoreCase("BTN") || bank.equalsIgnoreCase(OnlineBmtUtil.BANK_NAME)) {

					if (bank.equalsIgnoreCase(OnlineBmtUtil.BANK_NAME)
							&& !OnlineBmtUtil.isPerguruanTinggiReady(
									mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getId())) {
						jsonObject.put("status", "05");
						jsonObject.put("description", "Kanal Online BMT belum diaktifkan untuk perguruan tinggi ini");
						return jsonObject;
					}

						Session session = HibernateUtil.openSession();
						try {
						List<GeneralValueObject> tagihans = session
								.createCriteria(metode.equalsIgnoreCase("Bulanan") ? PengaturanPembayaranBulanan.class
										: DetailBiaya.class)
								.add(Restrictions.in("id", ids.keySet())).list();
						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);

//						String tahunAkademik = null;
						int i = 1;
						JenisKegiatan jenisKegiatan = null;
						Integer smt = 0;
						List detailBiayas = new ArrayList();
						Grid gridCicilan = new Grid();
						Rows rows = new Rows();
						rows.setParent(gridCicilan);

						Double biayaAdministrasi = 0.0;
						try {
							biayaAdministrasi = Double
									.parseDouble(Common.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:647");

						}

						if (bank.equalsIgnoreCase("Smartlink")) {
							try {
								biayaAdministrasi = Double.parseDouble(
										Common.getKonfigurasi("online_smartlink_biaya_administrasi", "0.0").getNilai());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:655");

							}
						}
						if (bank.equalsIgnoreCase(OnlineBmtUtil.BANK_NAME)) {
							biayaAdministrasi = parseDoubleSafe(
									Common.getKonfigurasi(Konfigurasi.ONLINE_BMT_BIAYA_ADMINISTRASI, "0.0").getNilai(), 0.0);
						}

						String ket = "";
						String pemb = "";
						String cicilan = "";
						Double total = 0.0;
						JSONArray items = new JSONArray();
						JadwalPembayaran jadwalPembayaran = null;

						if (biayaAdministrasi.intValue() > 0) {
							JSONObject jsonObjectitems = new JSONObject();
							jsonObjectitems.put("description", "Biaya Admin");
							jsonObjectitems.put("unitPrice", biayaAdministrasi.intValue());
							jsonObjectitems.put("qty", 1);
							jsonObjectitems.put("amount", biayaAdministrasi.intValue());
							items.put(jsonObjectitems);
						}

						String tahunAkademik = Common.getCurrentTahunAkademik();
						for (GeneralValueObject generalValueObject : tagihans) {
							Double nilai = ids.get(generalValueObject.getId());
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
							DetailBiaya detailBiaya = null;

							if (generalValueObject instanceof PengaturanPembayaranBulanan) {
								pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) generalValueObject;

								if (pengaturanPembayaranBulanan.getDetailBiaya().getTahunAkademik() != null
										&& !pengaturanPembayaranBulanan.getDetailBiaya().getTahunAkademik().trim()
												.isEmpty()) {
									tahunAkademik = pengaturanPembayaranBulanan.getDetailBiaya().getTahunAkademik();
								}

								detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
								smt = detailBiaya.getSemester();
								if (smt == null) {
									smt = mahasiswa.currentSemester();
								}
								if (detailBiaya != null) {
									detailBiayas.add(detailBiaya);
									jenisKegiatan = detailBiaya.getJenisKegiatan();
									Serializable[] serializables = PembayaranUtil.getInstance()
											.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(WaktuUtil.getDate(),
													jenisKegiatan, mahasiswa.getJenjang(), tahunAkademik,
													smt.intValue() % 2 != 0, mahasiswa.getJenisSeleksi(),
													mahasiswa.getProgram(), mahasiswa.getNim(), null);
									if (serializables[0] != null) {
										jadwalPembayaran = (JadwalPembayaran) serializables[0];
									}
								}

								Double hasilDenda = pengaturanPembayaranBulanan.checkDenda(nilai,
										ais.ui.util.WaktuUtil.getDate(), jadwalPembayaran, jenisKegiatan);

								cicilan += cicilan.isEmpty()
										? ("Bulanan-" + pengaturanPembayaranBulanan.getId().toString() + "-" + nilai)
										: "," + ("Bulanan-" + pengaturanPembayaranBulanan.getId().toString() + "-"
												+ nilai);

								ket += ket.isEmpty()
										? pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
										: "," + pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama();

								String desc = pengaturanPembayaranBulanan.getKeterangan();
								desc = (desc.isEmpty()
										? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
										: desc)
										+ ", Rp. " + Common.numberFormat.get().format(nilai)
										+ (hasilDenda.intValue() > nilai.intValue()
												? pengaturanPembayaranBulanan.getInfoDenda()
												: "");

								pemb += pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode().trim()
										+ "," + desc + ";";
								total += nilai;

								JSONObject jsonObjectitems = new JSONObject();
								jsonObjectitems.put("description", desc);
								jsonObjectitems.put("unitPrice", nilai.intValue());
								jsonObjectitems.put("qty", 1);
								jsonObjectitems.put("amount", nilai.intValue());
								items.put(jsonObjectitems);

							} else {
								detailBiaya = (DetailBiaya) generalValueObject;

								if (detailBiaya.getTahunAkademik() != null
										&& !detailBiaya.getTahunAkademik().trim().isEmpty()) {
									tahunAkademik = detailBiaya.getTahunAkademik();
								}
								smt = detailBiaya.getSemester();
								if (smt == null) {
									smt = mahasiswa.currentSemester();
								}
								ItemBiaya itemBiaya = detailBiaya.getItemBiaya();

								cicilan += cicilan.isEmpty()
										? ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-"
												+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId())
										: "," + ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-"
												+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId());

								String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);

								ket += ket.isEmpty() ? itemBiaya.getNama() : "," + itemBiaya.getNama();

								pemb += itemBiaya.getKode().trim() + "," + desc + ";";
								total += nilai;

								if (detailBiaya != null) {
									detailBiayas.add(detailBiaya);
									jenisKegiatan = detailBiaya.getJenisKegiatan();
									Serializable[] serializables = PembayaranUtil.getInstance()
											.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(WaktuUtil.getDate(),
													jenisKegiatan, mahasiswa.getJenjang(), tahunAkademik,
													smt.intValue() % 2 != 0, mahasiswa.getJenisSeleksi(),
													mahasiswa.getProgram(), mahasiswa.getNim(), null);

									if (serializables[0] != null) {
										jadwalPembayaran = (JadwalPembayaran) serializables[0];
									}
								}

								JSONObject jsonObjectitems = new JSONObject();
								jsonObjectitems.put("description", desc);
								jsonObjectitems.put("unitPrice", nilai.intValue());
								jsonObjectitems.put("qty", 1);
								jsonObjectitems.put("amount", nilai.intValue());
								items.put(jsonObjectitems);
							}

							if (detailBiaya != null) {
								tahunAkademik = detailBiaya.getTahunAkademik();
								jenisKegiatan = detailBiaya.getJenisKegiatan();
								ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
								Double nominal = ids.get(generalValueObject.getId());
								smt = detailBiaya.getSemester();
								if (smt == null) {
									smt = mahasiswa.currentSemester();
								}
								MyDoublebox jumlahCicilan = new MyDoublebox(nominal);

								Row row = new Row();
								row.setValign("top");
								rows.appendChild(row);

								CicilanPembayaran cicilanPembayaran = new CicilanPembayaran(detailBiaya);
								row.setValign("top");
								row.setAttribute("cicilanPembayaran", cicilanPembayaran);
								row.setValign("top");
								row.setAttribute("jumlahCicilan", jumlahCicilan);

								row.setValign("top");
								row.setAttribute("detailBiaya", detailBiaya);
								row.setValign("top");
								row.setAttribute("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);

								cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
								cicilanPembayaran.setItemBiaya(itemBiaya);
								cicilanPembayaran.setNilai(nominal);
								cicilanPembayaran.setKe(i);
								i++;

								if (detailBiaya.getTahunAkademik() != null
										&& !detailBiaya.getTahunAkademik().trim().isEmpty()) {
									tahunAkademik = detailBiaya.getTahunAkademik();
								}
							}
						}

						if (jadwalPembayaran == null) {
							jsonObject.put("status", "05");
							jsonObject.put("description", "Jadwal pembayaran tidak tersedia");
						} else {

							BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
									Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
							boolean finpay = bank.equalsIgnoreCase("Finpay");
							boolean otto = bank.equalsIgnoreCase("Otto");
							boolean flip = bank.equalsIgnoreCase("Flip");
							boolean smartlink = bank.equalsIgnoreCase("Smartlink");

							Map param = new HashMap();
							param.put("otto", otto);
							param.put("finpay", finpay);
							param.put("flip", flip);
							param.put("smartlink", smartlink);
							param.put(OnlineBmtUtil.PARAM_KEY, bank.equalsIgnoreCase(OnlineBmtUtil.BANK_NAME));
							if (smartlink) {
								param.put("smartlink_direct", smartlink);

								String link = Common.getRequestHostWithProtocol(httpServletRequest)
										+ "/common/smartlink/no_va2.zul?mahasiswa="
										+ (mahasiswa == null ? "-1" : mahasiswa.getId()) + "&calonMahasiswa"
										+ (calonMahasiswa == null ? "-1" : calonMahasiswa.getId()) + "&ket="
										+ URLEncoder.encode(ket, "UTF-8")

										+ "&items=" + URLEncoder.encode(items.toString(), "UTF-8")

										+ "&pemb=" + URLEncoder.encode(pemb, "UTF-8") + "&cicilan="
										+ URLEncoder.encode(cicilan, "UTF-8") + "&total=" + total + "&smt=" + smt

										+ "&bankHost=" + (bankHost == null ? "-1" : bankHost.getId())
										+ "&jadwalPembayaran="
										+ (jadwalPembayaran == null ? "-1" : jadwalPembayaran.getId());

								param.put("payment_url", link);
							}
							param.put("tahunAkademik", tahunAkademik);
							Double topup = null;
							VirtualAccountBank virtualAccountBank;
							boolean btnBelumAktif = false;
							if (bank.equalsIgnoreCase("BTN")) {
								// Pembayaran via Bank BTN: hormati konfigurasi aktifkan_pembayaran_via_bank_btn
								// (global + per-PT) sama seperti tombol di DaftarUlangMahasiswa*Action, dan pakai
								// downloader BTN agar nomor VA SESUAI (bukan downloader Online generik).
								ais.database.model.PerguruanTinggi ptBtn = ais.action.master.helper.util.PerguruanTinggiUtil
										.getPerguruanTinggi();
								boolean btnAktif = ais.database.model.Konfigurasi.AKTIF
										.equals(Common.getKonfigurasi("aktifkan_pembayaran_via_bank_btn",
												ais.database.model.Konfigurasi.TIDAK_AKTIF).getNilai())
										&& ais.database.model.Konfigurasi.AKTIF.equals(Common.getKonfigurasi(
												"aktifkan_pembayaran_via_bank_btn_pt_"
														+ (ptBtn == null ? "0" : ptBtn.getId()),
												ais.database.model.Konfigurasi.AKTIF).getNilai());
								if (btnAktif) {
									virtualAccountBank = ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn
											.downloadData(mahasiswa, smt, jadwalPembayaran, detailBiayas, gridCicilan);
								} else {
									virtualAccountBank = null;
									btnBelumAktif = true;
								}
							} else {
								virtualAccountBank = DownloadTagihanMahasiswaBankOnline.downloadData(mahasiswa, smt,
										jadwalPembayaran, detailBiayas, gridCicilan, param, biayaAdministrasi,
										tabunganNumber, topup, bankHost);
							}

							if (btnBelumAktif) {
								jsonObject.put("status", "05");
								jsonObject.put("description", "Pembayaran via Bank BTN belum diaktifkan");
							} else if (virtualAccountBank == null) {
								jsonObject.put("status", "05");
								jsonObject.put("description", "Pengambilan data gagal, nomor VA tidak bisa dibuat");
							} else {

								String va = virtualAccountBank.getKode();
								if (va == null || va.isEmpty()) {
									jsonObject.put("status", "05");
									jsonObject.put("description", "Pengambilan data gagal, nomor VA tidak bisa dibuat");
								} else {

									System.out.println("tagihans => " + tagihans.size());
									JSONArray jsonArray = new JSONArray();

									for (GeneralValueObject generalValueObject : tagihans) {

										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
										DetailBiaya detailBiaya = null;

										if (generalValueObject instanceof PengaturanPembayaranBulanan) {
											pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) generalValueObject;
											detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
										} else {
											detailBiaya = (DetailBiaya) generalValueObject;
										}
										jenisKegiatan = detailBiaya.getJenisKegiatan();
										Double nominal = ids.get(generalValueObject.getId());
										tahunAkademik = detailBiaya.getTahunAkademik();
										if (nominal > 0.1) {
											JSONObject js = new JSONObject();
											js.put("id", generalValueObject.getId());
											js.put("grup_kode", jenisKegiatan.getKode());
											js.put("grup_nama", jenisKegiatan.getNamaKegiatan());
											js.put("grup_ta", jenisKegiatan.getNamaKegiatan() + " semester " + smt);
											js.put("nama", detailBiaya.getItemBiaya().getNama()
													+ (pengaturanPembayaranBulanan == null ? 0
															: " bulan " + pengaturanPembayaranBulanan.getNamaBulan()));
											js.put("kode", detailBiaya.getItemBiaya().getKode());
											js.put("nominal", nominal);
											js.put("denda", 0.0);

											js.put("va", va);
											js.put("billExpired",
													virtualAccountBank == null ? ""
															: Common.dateFormat5.get()
																	.format(virtualAccountBank.getKadaluarsaWaktu()));

											js.put("bulan", pengaturanPembayaranBulanan == null ? 0
													: pengaturanPembayaranBulanan.getRealBulan());
											js.put("tahun", pengaturanPembayaranBulanan == null ? 0
													: pengaturanPembayaranBulanan.getRealBulanTahun());
											js.put("ke", 0);
											js.put("ta", tahunAkademik);

											if (virtualAccountBank.getLink() != null
													&& !virtualAccountBank.getLink().isEmpty()) {
												js.put("link", virtualAccountBank.getLink());
											}

											String kodebankLainOnline = Common
													.getKonfigurasi("prefix_kode_bank_lain_online", "").getNilai();
											if (!kodebankLainOnline.trim().isEmpty() && virtualAccountBank != null
													&& virtualAccountBank.getKanalPembayaran() != null
													&& virtualAccountBank.getKanalPembayaran().getBsiUsername() != null
													&& !virtualAccountBank.getKanalPembayaran().getBsiUsername()
															.isEmpty()) {
												va = (virtualAccountBank.getKanalPembayaran().getBsiUsername() + va);

												kodebankLainOnline = kodebankLainOnline + "" + va;
											} else if (!kodebankLainOnline.trim().isEmpty()) {
												kodebankLainOnline = kodebankLainOnline + "" + va;
											}
											if (!kodebankLainOnline.trim().isEmpty()) {
												js.put("va_bank_lain", kodebankLainOnline);
											}

											jsonArray.put(js);
										}
									}

									if (virtualAccountBank.getLink() != null
											&& !virtualAccountBank.getLink().isEmpty()) {
										jsonObject.put("link", virtualAccountBank.getLink());
									}

									jsonObject.put("va", va);
									jsonObject.put("biayaAdministrasi", biayaAdministrasi);

									jsonObject.put("data", jsonArray);
									jsonObject.put("status", "00");
									jsonObject.put("description", "Pengambilan data berhasil");
								}
							}
						}
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					}

					else if (bank.isEmpty() || bank.equalsIgnoreCase("BNI")) {

						if (!ids.isEmpty()) {

							Session sessionmy = HibernateUtil.openSession();
							try {
							List<GeneralValueObject> tagihans = sessionmy
									.createCriteria(
											metode.equalsIgnoreCase("Bulanan") ? PengaturanPembayaranBulanan.class
													: DetailBiaya.class)

									.add(Restrictions.in("id", ids.keySet())).list();

							String tahunAkademik = null;
							int i = 1;
							List<BniRequestDetail> bniRequestDetails = new ArrayList<BniRequestDetail>();
							List<BniRequestDetailBiaya> bniRequestDetailBiayas = new ArrayList<BniRequestDetailBiaya>();
							JenisKegiatan jenisKegiatan = null;
							Integer smt = 0;
							String cicilan = "";
							for (GeneralValueObject generalValueObject : tagihans) {
								BniRequestDetail bniRequestDetail = new BniRequestDetail();
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
								DetailBiaya detailBiaya = null;

								if (generalValueObject instanceof PengaturanPembayaranBulanan) {
									pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) generalValueObject;
									detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
								} else {
									detailBiaya = (DetailBiaya) generalValueObject;
								}
								tahunAkademik = detailBiaya.getTahunAkademik();
								jenisKegiatan = detailBiaya.getJenisKegiatan();
								ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
								Double nominal = ids.get(generalValueObject.getId());
								smt = detailBiaya.getSemester();
								bniRequestDetail.setIdCicilan(null);
								bniRequestDetail.setDetailBiaya(detailBiaya);
								bniRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
								bniRequestDetail.setItemBiaya(itemBiaya);
								bniRequestDetail.setKeterangan("Bayar via mobile");
								bniRequestDetail.setNilai(nominal);
								bniRequestDetail.setTanggal(WaktuUtil.getDate());
								bniRequestDetail.setKe(i);

								bniRequestDetail.setDenda(0.0);
								bniRequestDetail.setNilaiAsli(nominal);
								bniRequestDetails.add(bniRequestDetail);

								BniRequestDetailBiaya bniRequestDetailBiaya = new BniRequestDetailBiaya();
								bniRequestDetailBiaya.setDetailBiaya(detailBiaya);
								bniRequestDetailBiaya.setNilai(nominal);
								bniRequestDetailBiayas.add(bniRequestDetailBiaya);
								i++;

								if (pengaturanPembayaranBulanan != null) {
									cicilan += cicilan.isEmpty()
											? ("Bulanan-" + pengaturanPembayaranBulanan.getId().toString() + "-"
													+ nominal)
											: "," + ("Bulanan-" + pengaturanPembayaranBulanan.getId().toString() + "-"
													+ nominal);
								} else {
									cicilan += cicilan.isEmpty()
											? ("Item-" + itemBiaya.getId().toString() + "-" + nominal + "-"
													+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId())
											: "," + ("Item-" + itemBiaya.getId().toString() + "-" + nominal + "-"
													+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId());
								}
							}
							try { sessionmy.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:1066");}
							sessionmy.disconnect();
							sessionmy.close();

							Serializable[] serializables = PembayaranUtil.getInstance()
									.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(WaktuUtil.getDate(),
											jenisKegiatan, mahasiswa.getJenjang(), tahunAkademik,
											smt.intValue() % 2 != 0, mahasiswa.getJenisSeleksi(),
											mahasiswa.getProgram(), mahasiswa.getNim(), null);
							JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

							if (jadwalPembayaran == null) {
								jsonObject.put("status", "05");
								jsonObject.put("description", "Jadwal pembayaran tidak tersedia");
							} else {

								BniRequest bniRequest = BniCommon.onSaveBni(nilaiYgAkanDibayar, mahasiswa,
										calonMahasiswa, jenisKegiatan, jadwalPembayaran, smt, tahunAkademik,
										"Bayar via mobile", 0.0, nilaiYgAkanDibayar, bniRequestDetails,
										bniRequestDetailBiayas, false, null, cicilan);

								if (bniRequest == null) {
									jsonObject.put("status", "05");
									jsonObject.put("description", "Pengambilan data gagal, bniRequest null");
									return jsonObject;
								}
								String va = bniRequest.getVa();
								if (va == null || va.isEmpty()) {
									jsonObject.put("status", "05");
									jsonObject.put("description", "Pengambilan data gagal, nomor VA tidak bisa dibuat");
								} else {

									System.out.println("tagihans => " + tagihans.size());
									JSONArray jsonArray = new JSONArray();

									for (GeneralValueObject generalValueObject : tagihans) {

										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
										DetailBiaya detailBiaya = null;

										if (generalValueObject instanceof PengaturanPembayaranBulanan) {
											pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) generalValueObject;
											detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
										} else {
											detailBiaya = (DetailBiaya) generalValueObject;
										}
										jenisKegiatan = detailBiaya.getJenisKegiatan();
										Double nominal = ids.get(generalValueObject.getId());
										tahunAkademik = detailBiaya.getTahunAkademik();
										if (nominal > 0.1) {
											JSONObject js = new JSONObject();
											js.put("id", generalValueObject.getId());
											js.put("grup_kode", jenisKegiatan.getKode());
											js.put("grup_nama", jenisKegiatan.getNamaKegiatan());
											js.put("grup_ta", jenisKegiatan.getNamaKegiatan() + " semester " + smt);
											js.put("nama", detailBiaya.getItemBiaya().getNama()
													+ (pengaturanPembayaranBulanan == null ? 0
															: " bulan " + pengaturanPembayaranBulanan.getNamaBulan()));
											js.put("kode", detailBiaya.getItemBiaya().getKode());
											js.put("nominal", nominal);
											js.put("denda", 0.0);

											js.put("va", va);
											js.put("billExpired", bniRequest == null ? ""
													: Common.dateFormat5.get().format(bniRequest.getBillExpired()));

											js.put("bulan", pengaturanPembayaranBulanan == null ? 0
													: pengaturanPembayaranBulanan.getRealBulan());
											js.put("tahun", pengaturanPembayaranBulanan == null ? 0
													: pengaturanPembayaranBulanan.getRealBulanTahun());
											js.put("ke", 0);
											js.put("ta", tahunAkademik);

											String kodebankLainOnline = Common
													.getKonfigurasi("prefix_kode_bank_lain_online", "").getNilai();
											if (!kodebankLainOnline.trim().isEmpty()) {
												kodebankLainOnline = kodebankLainOnline + "" + va;
											}
											if (!kodebankLainOnline.trim().isEmpty()) {
												js.put("va_bank_lain", kodebankLainOnline);
											}

											jsonArray.put(js);
										}
									}

									double biayaAdministrasi = 0.0;
									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TagihanMahasiswa.java:1156");

									}

									JSONObject jsonObjectTabungan = new JSONObject();
									jsonObjectTabungan.put("nama", "Topup");
									jsonObjectTabungan.put("nilai", tabunganNumber);
									jsonArray.put(jsonObjectTabungan);
									jsonObject.put("topup", jsonObjectTabungan);
									jsonObject.put("va", va);
									jsonObject.put("biayaAdministrasi", biayaAdministrasi);

									jsonObject.put("data", jsonArray);
									jsonObject.put("status", "00");
									jsonObject.put("description", "Pengambilan data berhasil");
								}
							}
							} finally {
								HibernateUtil.closeSessionQuietly(sessionmy);
							}
						} else {
							jsonObject.put("status", "02");
							jsonObject.put("description", "Pengambilan data gagal, tagihan harus dimasukkan");
						}
					} else {
						jsonObject.put("status", "03");
						jsonObject.put("description", "Pengambilan data gagal, bank tidak ditemukan");
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Pengambilan data gagal, token harus sebagai mahasiswa");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TagihanMahasiswa.java:1195");
			}
		}
		return jsonObject;
	}
}
