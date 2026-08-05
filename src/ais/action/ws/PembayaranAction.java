/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.ws.logic.InqueryLogic;
import ais.action.ws.logic.PaymentLogic;
import ais.action.ws.logic.ReversalLogic;
import ais.action.ws.model.Response;
import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
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
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.WaktuUtil;

/**
 * 
 * @author fauzi
 */
public class PembayaranAction {

	public static ReversalLogic reversalLogic = new ReversalLogic();
	public static InqueryLogic inqueryLogic = new InqueryLogic();
	public static PaymentLogic paymentLogic = new PaymentLogic();

	/**
	 * Sample method
	 */
	public String hello(String name) {
		return "Hello " + name;
	}

	@SuppressWarnings("unchecked")
	public Response reversal(String nim, String nama, LogHostToHost logHostToHost, String nominalTagihan) {
		nim = nim == null ? "" : nim.trim();
		System.out.println(nama);
		BankHost bankHost = reversalLogic.pembayaranUtil.getBankHost();
		if (bankHost == null) {
			return CommonUtil.convertToResponse(reversalLogic.displayUtil.displayIpNotAllowed(logHostToHost, nim,
					bankHost, nama, ConstantUtil.REVERSAL));
		}

		if (Common.bolehKonfigurasi("bank_bisa_melakukan_reversal", Konfigurasi.TIDAK_AKTIF)) {

			if (Common.bolehKonfigurasi("pembayaran_via_bank_online_harus_via_va")
					&& (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF)
							|| Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF))) {
				Session session = HibernateUtil.openSession();
//				session.createSQLQuery("set statement_timeout to 5000;").executeUpdate();
				List<String[]> data = new ArrayList<String[]>();
				try {
					VirtualAccountBank virtualAccountBankNtt = VirtualAccountBank.ambilVa(nim, bankHost);

					JenisKegiatan jenisKegiatan = virtualAccountBankNtt == null ? null
							: virtualAccountBankNtt.getJenisKegiatan();
					Mahasiswa mahasiswa = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getMahasiswa();
					BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt == null ? null
							: virtualAccountBankNtt.getBiodataCalonMahasiswa();
					Integer semester = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getSemester();
					Double total = 0.0;

					String rincian = "";
					if (virtualAccountBankNtt != null && virtualAccountBankNtt.getCicilan() != null
							&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
						for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
							if (Common.isNumber(idPemBul)) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
										.createCriteria(PengaturanPembayaranBulanan.class)
										.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

								if (pengaturanPembayaranBulanan != null) {
									Double subtotal = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
											semester);
									total += subtotal;

									String s = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode()
											+ "\\"
											+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
											+ " " + pengaturanPembayaranBulanan.getNamaBulan() + "\\"
											+ subtotal.intValue();

									rincian += rincian.isEmpty() ? s : "|" + s;
								}
							} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
										.createCriteria(PengaturanPembayaranBulanan.class)
										.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();

								if (pengaturanPembayaranBulanan != null) {
									Double subtotal = 0.0;
									try {
										String[] spl = idPemBul.split("-");
										subtotal = Double.parseDouble(spl[spl.length - 1]);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:122");
									}

									if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
											.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
										subtotal = 0.0 - subtotal;
									}
									total += subtotal;

									String s = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode()
											+ "\\"
											+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
											+ " " + pengaturanPembayaranBulanan.getNamaBulan() + "\\"
											+ subtotal.intValue();

									rincian += rincian.isEmpty() ? s : "|" + s;
								}
							} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
								ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
										session.createCriteria(ItemBiaya.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
										ItemBiaya.class);

								if (itemBiaya != null) {

									Double subtotal = 0.0;
									try {
										String[] spl = idPemBul.split("-");
										subtotal = Double.parseDouble(spl[2]);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:151");
									}

									@SuppressWarnings("unused")
									Integer bayarke = 1;
									try {
										String[] spl = idPemBul.split("-");
										bayarke = Integer.parseInt(spl[3]);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:159");
									}

									if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
										subtotal = 0.0 - subtotal;
									}
									total += subtotal;

									String s = itemBiaya.getKode() + "\\" + itemBiaya.getNama() + "\\"
											+ subtotal.intValue();

									rincian += rincian.isEmpty() ? s : "|" + s;
								}
							}
						}
						VirtualAccountBank.updateTotal(virtualAccountBankNtt, total);
					}

					if (virtualAccountBankNtt != null && virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
						String s = "000\\Biaya Admin\\" + virtualAccountBankNtt.getBiayaAdmin().intValue();
						total += virtualAccountBankNtt.getBiayaAdmin();
						rincian += rincian.isEmpty() ? s : "|" + s;
					}

					if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
							&& (virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
							&& virtualAccountBankNtt.getJadwalPembayaran() != null) {

						Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
								: session.createCriteria(Kegiatan.class)
										.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan())).uniqueResult());

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
						kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
						kegiatan.setValidated(1);
						kegiatan.setValidator("Reversal Online");

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, kegiatan);
						session.getTransaction().commit();

						List<Long> detailBiayasId = new ArrayList<Long>();
						for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
							try {
								detailBiayasId.add(Long.parseLong(id.trim()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:230");

							}
						}

						Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
								.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("id", detailBiayasId))
								.list();

						Double nilaiBiayaHarusDiBayars = 0.0;
						for (DetailBiaya detailBiaya : detailBiayas) {
							Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

							nilaiBiayaHarusDiBayars += biaya;

						}

						session.getTransaction().begin();
						session.createSQLQuery(
								"delete from cicilan_pembayaran where ref_va = " + virtualAccountBankNtt.getId())
								.executeUpdate();
						session.getTransaction().commit();

						Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
						Double jumlah = d[0];
						Double denda = d[1];
						kegiatan.setDenda(denda.doubleValue());
						kegiatan.setAmountTerhutang(
								nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

						kegiatan.setAmount(
								jumlah.doubleValue() > 0.1 ? jumlah.doubleValue() : virtualAccountBankNtt.getTotal());
						kegiatan.setValidator("Reversal Online");

						virtualAccountBankNtt.setKegiatan(null);
						session.getTransaction().begin();
						Common.refreshUpdate(session, kegiatan);
						session.getTransaction().commit();

						VirtualAccountBank.updateVa(virtualAccountBankNtt, WaktuUtil.getDate(), kegiatan, nama,
								kegiatan.getValidator());

						data.add(new String[] { "response_code", ConstantUtil.SUCCESS });
						data.add(new String[] { "response_description", "Reversal Sukses Dilakukan" });
						data.add(new String[] { "nim", nim });
						data.add(new String[] { "kurs", "IDR" });
						data.add(new String[] { "nama",
								virtualAccountBankNtt.getMahasiswa() != null
										? virtualAccountBankNtt.getMahasiswa().getNama()
										: virtualAccountBankNtt.getBiodataCalonMahasiswa().getNama() });
						data.add(new String[] { "program",
								virtualAccountBankNtt.getMahasiswa() != null
										? virtualAccountBankNtt.getMahasiswa().getProgram()
										: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProgram() });
						data.add(new String[] { "fakultas", virtualAccountBankNtt.getMahasiswa() != null
								? virtualAccountBankNtt.getMahasiswa().getJurusan().getFakultas().getNama()
								: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus() != null
										? virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus().getFakultas()
												.getNama()
										: (virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1() == null ? ""
												: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1()
														.getFakultas().getNama()) });
						data.add(new String[] { "prodi", virtualAccountBankNtt.getMahasiswa() != null
								? virtualAccountBankNtt.getMahasiswa().getJurusan().getNama()
								: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus() != null
										? virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus().getNama()
										: (virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1() == null ? ""
												: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1()
														.getNama()) });
						data.add(new String[] { "angkatan",
								(virtualAccountBankNtt.getMahasiswa() != null
										? virtualAccountBankNtt.getMahasiswa().getTahunangkatan()
										: virtualAccountBankNtt.getBiodataCalonMahasiswa().getTahun()) + "" });
						data.add(new String[] { "semester",
								virtualAccountBankNtt.getSemester() % 2 == 1 ? Perkuliahan.GANJIL
										: Perkuliahan.GENAP });
						data.add(new String[] { "semester_ke", virtualAccountBankNtt.getSemester() + "" });
						data.add(new String[] { "tanggal_max",
								Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate()) });
						data.add(new String[] { "tanggal_min", Common.dateFormat2.get()
								.format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate()) });
						data.add(new String[] { "amount", "|" + rincian + "|" });
						data.add(new String[] { "total_amount", total.intValue() + "" });
						data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
						data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
						data.add(new String[] { "reference_number", nim });
						data.add(new String[] { "jumlah_yang_telah_dibayar", "0" });
					} else {
						data.add(new String[] { "response_code", ConstantUtil.NIM_NOT_FOUND });
						data.add(new String[] { "response_description",
								"Kode pembayaran tidak ditemukan atau tagihan telah dibayar" });
						data.add(new String[] { "nim", nim });
						data.add(new String[] { "kurs", "IDR" });
						data.add(new String[] { "nama",
								mahasiswa == null
										? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNama())
										: mahasiswa.getNama() });
						data.add(new String[] { "program",
								mahasiswa == null
										? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getProgram())
										: mahasiswa.getProgram() });
						data.add(new String[] { "fakultas",
								mahasiswa == null
										? (biodataCalonMahasiswa == null ? ""
												: biodataCalonMahasiswa.getProdiLulus() != null
														? biodataCalonMahasiswa.getProdiLulus().getFakultas().getNama()
														: (biodataCalonMahasiswa.getProdi1() != null
																? biodataCalonMahasiswa.getProdi1().getFakultas()
																		.getNama()
																: ""))
										: mahasiswa.getJurusan().getFakultas().getNama() });
						data.add(
								new String[] { "prodi",
										mahasiswa == null
												? (biodataCalonMahasiswa == null ? ""
														: biodataCalonMahasiswa.getProdiLulus() != null
																? biodataCalonMahasiswa.getProdiLulus().getNama()
																: (biodataCalonMahasiswa.getProdi1() != null
																		? biodataCalonMahasiswa.getProdi1().getNama()
																		: ""))
												: mahasiswa.getJurusan().getNama() });
						data.add(new String[] { "angkatan",
								mahasiswa == null
										? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getTahun() + "")
										: mahasiswa.getTahunangkatan() + "" });
						data.add(new String[] { "semester",
								virtualAccountBankNtt == null ? ""
										: virtualAccountBankNtt.getSemester() % 2 == 1 ? Perkuliahan.GANJIL
												: Perkuliahan.GENAP });
						data.add(new String[] { "semester_ke",
								virtualAccountBankNtt == null ? "" : virtualAccountBankNtt.getSemester() + "" });
						data.add(new String[] { "tanggal_max",
								virtualAccountBankNtt == null ? ""
										: Common.dateFormat2.get()
												.format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate()) });
						data.add(new String[] { "tanggal_min",
								virtualAccountBankNtt == null ? ""
										: Common.dateFormat2.get()
												.format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate()) });
						data.add(new String[] { "amount", "|" + rincian + "|" });
						data.add(new String[] { "total_amount", total.intValue() + "" });
						data.add(new String[] { "kode_status_pembayaran",
								virtualAccountBankNtt == null ? "" : jenisKegiatan.getKode() });
						data.add(new String[] { "keterangan_status_pembayaran",
								virtualAccountBankNtt == null ? "" : jenisKegiatan.getNamaKegiatan() });
						data.add(new String[] { "reference_number",
								virtualAccountBankNtt == null ? "" : virtualAccountBankNtt.getKode() });
						data.add(new String[] { "jumlah_yang_telah_dibayar", "0" });
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				try {
					logHostToHost.setBankHost(bankHost);
					logHostToHost.setIp(bankHost.getIp());
					logHostToHost.setNama(nama);
					logHostToHost.setNim(nim);
					logHostToHost.setKeterangan(CommonUtil.convertToString(data));
					CommonUtil.setRequestAndresponse(logHostToHost);
					logHostToHost.setTransactionType(ConstantUtil.REVERSAL);
					CommonUtil.setRequestAndresponse(logHostToHost);

					// JAMINAN: simpan via helper bersama (session terdedikasi + commit + retry, tak gagal).
					ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/PembayaranAction.java:397");
				}

				Common.closeNativeSessionQuietly(session);

				return CommonUtil.convertToResponse(data);
			} else {

				Mahasiswa mahasiswa = null;
				String kode = null;
				String bulan = null;

				try {
					Object[] objects = inqueryLogic.pembayaranUtil.ambilDataMahasiswa(nim);
					mahasiswa = (Mahasiswa) objects[0];
					kode = (String) objects[1];
					bulan = (String) objects[2];
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (mahasiswa == null) {
					// reversal untuk calon mahasiswa
					BiodataCalonMahasiswa biodataCalonMahasiswa = reversalLogic.pembayaranUtil
							.getCalonMahasiswaByNoUjian(nim);
					if (biodataCalonMahasiswa == null) {
						biodataCalonMahasiswa = reversalLogic.pembayaranUtil.getCalonMahasiswaByNoPendaftaran(nim);
						if (biodataCalonMahasiswa == null) {
							return CommonUtil.convertToResponse(reversalLogic.displayUtil
									.displayNimNotFound(logHostToHost, nim, bankHost, nama, ConstantUtil.REVERSAL));
						} else {

							nama = ("reversal calon mahasiswa dengan no pendaftaran = " + nim);
							return CommonUtil
									.convertToResponse(reversalLogic.reversalCalonMahasiswa(biodataCalonMahasiswa,
											bankHost, nama, logHostToHost, Double.parseDouble(nominalTagihan)));

						}
					} else {
						nama = ("reversal calon mahasiswa dengan no ujian = " + nim);
						return CommonUtil.convertToResponse(reversalLogic.reversalMahasiswaBaru(biodataCalonMahasiswa,
								bankHost, nama, logHostToHost, Double.parseDouble(nominalTagihan)));
					}
				} else {
					mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler" : mahasiswa.getProgram());
					return CommonUtil.convertToResponse(reversalLogic.reversalMahasiswaLama(mahasiswa, bankHost, nama,
							logHostToHost, Double.parseDouble(nominalTagihan), kode, bulan, nim));
				}
			}
		} else {
			return CommonUtil.convertToResponse(reversalLogic.displayUtil
					.displayReversalTidakDiaktifkan(logHostToHost, nim, bankHost, nama, ConstantUtil.REVERSAL)
					.toArray(new String[][] { null }));
		}
	}

	public Response inquery(String nim, String nama, LogHostToHost logHostToHost) {
		return inquery(nim, nama, logHostToHost, null);
	}

	public Response inquery(String nim, String nama, LogHostToHost logHostToHost, HttpServletRequest request) {
		nim = nim == null ? "" : nim.trim();
		System.out.println(nama);
		BankHost bankHost = inqueryLogic.pembayaranUtil.getBankHost(request);
		if (bankHost == null) {
			return CommonUtil.convertToResponse(inqueryLogic.displayUtil.displayIpNotAllowed(logHostToHost, nim,
					bankHost, nama, ConstantUtil.INQUERY));
		}

		if (Common.bolehKonfigurasi("pembayaran_via_bank_online_harus_via_va")
				&& (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF)
						|| Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF))) {

			Session session = HibernateUtil.openSession();
			List<String[]> data = new ArrayList<String[]>();
			try {
				VirtualAccountBank virtualAccountBankNtt = VirtualAccountBank.ambilVa(nim, bankHost);

				JenisKegiatan jenisKegiatan = virtualAccountBankNtt == null ? null
						: virtualAccountBankNtt.getJenisKegiatan();
				Mahasiswa mahasiswa = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getMahasiswa();
				BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt == null ? null
						: virtualAccountBankNtt.getBiodataCalonMahasiswa();
				Integer semester = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getSemester();
				Double total = 0.0;

				String rincian = "";
				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getCicilan() != null
						&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
					for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
						if (Common.isNumber(idPemBul)) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

							if (pengaturanPembayaranBulanan != null) {
								Double subtotal = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
										semester);
								
								
								
								total += subtotal;

								String s = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "\\"
										+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " "
										+ pengaturanPembayaranBulanan.getNamaBulan() + "\\" + subtotal.intValue();

								rincian += rincian.isEmpty() ? s : "|" + s;
							}
						} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();

							if (pengaturanPembayaranBulanan != null) {
								Double subtotal = 0.0;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[spl.length - 1]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:516");
								}

								if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
										.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									subtotal = 0.0 - subtotal;
								}
								total += subtotal;

								String s = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "\\"
										+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " "
										+ pengaturanPembayaranBulanan.getNamaBulan() + "\\" + subtotal.intValue();

								rincian += rincian.isEmpty() ? s : "|" + s;
							}
						} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
									session.createCriteria(ItemBiaya.class)
											.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
									ItemBiaya.class);

							if (itemBiaya != null) {

								Double subtotal = 0.0;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[2]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:543");
								}

								@SuppressWarnings("unused")
								Integer bayarke = 1;
								try {
									String[] spl = idPemBul.split("-");
									bayarke = Integer.parseInt(spl[3]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:551");
								}

								if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									subtotal = 0.0 - subtotal;
								}
								total += subtotal;

								String s = itemBiaya.getKode() + "\\" + itemBiaya.getNama() + "\\"
										+ subtotal.intValue();

								rincian += rincian.isEmpty() ? s : "|" + s;
							}
						}
					}

					VirtualAccountBank.updateTotal(virtualAccountBankNtt, total);
				}

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
					String s = "000\\Biaya Admin\\" + virtualAccountBankNtt.getBiayaAdmin().intValue();
					total += virtualAccountBankNtt.getBiayaAdmin();
					rincian += rincian.isEmpty() ? s : "|" + s;
				}

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
						&& virtualAccountBankNtt.getJadwalPembayaran() != null) {

					if (Common.dateFormat1.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate())
							.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))

							|| Common.dateFormat1.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate())
									.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))

							|| (virtualAccountBankNtt.getJadwalPembayaran().getEndDate()
									.after(ais.ui.util.WaktuUtil.getDate())
									&& virtualAccountBankNtt.getJadwalPembayaran().getStartDate()
											.before(ais.ui.util.WaktuUtil.getDate()))) {

						data.add(new String[] { "response_code", ConstantUtil.SUCCESS });
						data.add(new String[] { "response_description", "Inquiry Sukses Dilakukan" });
					} else {
						data.add(new String[] { "response_code", ConstantUtil.BILLS_HAS_EXPIRED });
						data.add(new String[] { "response_description",
								"Tidak ada jadwal pembayaran atau telah kadaluarsa" });
					}
					data.add(new String[] { "nim", nim });
					data.add(new String[] { "kurs", "IDR" });
					data.add(new String[] { "nama",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getNama()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getNama() });
					data.add(new String[] { "program",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getProgram()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProgram() });
					data.add(new String[] { "fakultas",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getJurusan().getFakultas().getNama()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus() != null
											? virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus()
													.getFakultas().getNama()
											: (virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1() == null ? ""
													: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1()
															.getFakultas().getNama()) });
					data.add(new String[] { "prodi",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getJurusan().getNama()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus() != null
											? virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus().getNama()
											: (virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1() == null ? ""
													: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1()
															.getNama()) });
					data.add(new String[] { "angkatan",
							(virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getTahunangkatan()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getTahun()) + "" });
					data.add(new String[] { "semester",
							virtualAccountBankNtt.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
					data.add(new String[] { "semester_ke", virtualAccountBankNtt.getSemester() + "" });
					data.add(new String[] { "tanggal_max",
							Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate()) });
					data.add(new String[] { "tanggal_min",
							Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate()) });
					data.add(new String[] { "amount", "|" + rincian + "|" });
					data.add(new String[] { "total_amount", total.intValue() + "" });
					data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
					data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
					data.add(new String[] { "reference_number", nim });
					data.add(new String[] { "jumlah_yang_telah_dibayar", "0" });
				} else {
					data.add(new String[] { "response_code", ConstantUtil.NIM_NOT_FOUND });
					data.add(new String[] { "response_description",
							"Kode pembayaran tidak ditemukan atau tagihan telah dibayar" });
					data.add(new String[] { "nim", nim });
					data.add(new String[] { "kurs", "IDR" });
					data.add(new String[] { "nama",
							mahasiswa == null ? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNama())
									: mahasiswa.getNama() });
					data.add(new String[] { "program",
							mahasiswa == null
									? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getProgram())
									: mahasiswa.getProgram() });
					data.add(
							new String[] { "fakultas",
									mahasiswa == null
											? (biodataCalonMahasiswa == null ? ""
													: biodataCalonMahasiswa.getProdiLulus() != null
															? biodataCalonMahasiswa.getProdiLulus().getFakultas()
																	.getNama()
															: (biodataCalonMahasiswa.getProdi1() != null
																	? biodataCalonMahasiswa.getProdi1().getFakultas()
																			.getNama()
																	: ""))
											: mahasiswa.getJurusan().getFakultas().getNama() });
					data.add(
							new String[] { "prodi",
									mahasiswa == null
											? (biodataCalonMahasiswa == null ? ""
													: biodataCalonMahasiswa.getProdiLulus() != null
															? biodataCalonMahasiswa.getProdiLulus().getNama()
															: (biodataCalonMahasiswa.getProdi1() != null
																	? biodataCalonMahasiswa.getProdi1().getNama()
																	: ""))
											: mahasiswa.getJurusan().getNama() });
					data.add(new String[] { "angkatan",
							mahasiswa == null
									? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getTahun() + "")
									: mahasiswa.getTahunangkatan() + "" });
					data.add(new String[] { "semester", virtualAccountBankNtt == null ? ""
							: virtualAccountBankNtt.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
					data.add(new String[] { "semester_ke",
							virtualAccountBankNtt == null ? "" : virtualAccountBankNtt.getSemester() + "" });
					data.add(new String[] { "tanggal_max", virtualAccountBankNtt == null ? ""
							: Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate()) });
					data.add(new String[] { "tanggal_min", virtualAccountBankNtt == null ? ""
							: Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate()) });
					data.add(new String[] { "amount", "|" + rincian + "|" });
					data.add(new String[] { "total_amount", total.intValue() + "" });
					data.add(new String[] { "kode_status_pembayaran",
							virtualAccountBankNtt == null ? "" : jenisKegiatan.getKode() });
					data.add(new String[] { "keterangan_status_pembayaran",
							virtualAccountBankNtt == null ? "" : jenisKegiatan.getNamaKegiatan() });
					data.add(new String[] { "reference_number",
							virtualAccountBankNtt == null ? "" : virtualAccountBankNtt.getKode() });
					data.add(new String[] { "jumlah_yang_telah_dibayar", "0" });
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				logHostToHost.setBankHost(bankHost);
				logHostToHost.setIp(bankHost.getIp());
				logHostToHost.setNama(nama);
				logHostToHost.setNim(nim);
				logHostToHost.setKeterangan(CommonUtil.convertToString(data));
				CommonUtil.setRequestAndresponse(logHostToHost);
				logHostToHost.setTransactionType(ConstantUtil.INQUERY);
				CommonUtil.setRequestAndresponse(logHostToHost);

				// JAMINAN: simpan via helper bersama (session terdedikasi + commit + retry, tak gagal).
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/PembayaranAction.java:715");
			}

			Common.closeNativeSessionQuietly(session);

			return CommonUtil.convertToResponse(data);
		} else {

			Response response = PembayaranAction.inquiryByNim(nim, bankHost, nama, logHostToHost);

			return response;
		}
	}

	public static Response inquiryByNim(String nim, BankHost bankHost, String nama, LogHostToHost logHostToHost) {
		Mahasiswa mahasiswa = null;
		String kode = null;
		String bulan = null;

		try {
			Object[] objects = inqueryLogic.pembayaranUtil.ambilDataMahasiswa(nim);
			mahasiswa = (Mahasiswa) objects[0];
			kode = (String) objects[1];
			bulan = (String) objects[2];
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		System.out.println("mahasiswa -> " + mahasiswa + " kode " + kode + " bulan " + bulan + " nim " + nim);

		if (mahasiswa == null) {
			// inquery untuk calon mahasiswa
			BiodataCalonMahasiswa biodataCalonMahasiswa = inqueryLogic.pembayaranUtil.getCalonMahasiswaByNoUjian(nim);

			System.out.println("by ujian biodataCalonMahasiswa -> " + biodataCalonMahasiswa);

			if (biodataCalonMahasiswa == null) {
				biodataCalonMahasiswa = inqueryLogic.pembayaranUtil.getCalonMahasiswaByNoPendaftaran(nim);

				System.out.println("by no reg biodataCalonMahasiswa -> " + biodataCalonMahasiswa);

				if (biodataCalonMahasiswa == null) {
					return CommonUtil.convertToResponse(inqueryLogic.displayUtil.displayNimNotFound(logHostToHost, nim,
							bankHost, nama, ConstantUtil.INQUERY));
				} else {
					nama = ("inquiry calon mahasiswa dengan no pendaftaran = " + nim);
					return CommonUtil.convertToResponse(
							inqueryLogic.inqueryCalonMahasiswa(biodataCalonMahasiswa, bankHost, nama, logHostToHost));

				}
			} else {
				nama = ("inquiry calon mahasiswa dengan no ujian = " + nim);
				return CommonUtil.convertToResponse(
						inqueryLogic.inqueryMahasiswaBaru(biodataCalonMahasiswa, bankHost, nama, logHostToHost));
			}
		} else {
			mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler" : mahasiswa.getProgram());
			return CommonUtil.convertToResponse(
					inqueryLogic.inqueryMahasiswaLama(mahasiswa, bankHost, nama, logHostToHost, kode, bulan));
		}
	}

	public Response pay(String nim, String reffNumber, String tanggalBayar, String jamBayar, String userID,
			String namaCabang, String nominalTagihan, String nama, LogHostToHost logHostToHost) {
		return pay(nim, reffNumber, tanggalBayar, jamBayar, userID, namaCabang, nominalTagihan, nama, logHostToHost,
				null);
	}

	@SuppressWarnings("unchecked")
	public Response pay(String nim, String reffNumber, String tanggalBayar, String jamBayar, String userID,
			String namaCabang, String nominalTagihan, String nama, LogHostToHost logHostToHost,
			HttpServletRequest request) {
		System.out.println(nama);
		nim = nim == null ? "" : nim.trim();
		BankHost bankHost = paymentLogic.pembayaranUtil.getBankHost(request);
		if (bankHost == null) {
			return CommonUtil.convertToResponse(
					paymentLogic.displayUtil.displayIpNotAllowed(logHostToHost, nim, bankHost, nama, ConstantUtil.PAY));
		}

		if (Common.bolehKonfigurasi("pembayaran_via_bank_online_harus_via_va")
				&& (Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF)
						|| Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF))) {

			boolean telahbayar = false;

			List<String[]> data = new ArrayList<String[]>();
			try {
				VirtualAccountBank virtualAccountBankNtt = VirtualAccountBank.ambilVa(nim, bankHost);

				Session session = HibernateUtil.currentNativeSession();
				JenisKegiatan jenisKegiatan = virtualAccountBankNtt == null ? null
						: virtualAccountBankNtt.getJenisKegiatan();
				Mahasiswa mahasiswa = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getMahasiswa();
				BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt == null ? null
						: virtualAccountBankNtt.getBiodataCalonMahasiswa();
				Integer semester = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getSemester();
				Double total = 0.0;

				String rincian = "";
				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getCicilan() != null
						&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
					for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
						if (Common.isNumber(idPemBul)) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

							if (pengaturanPembayaranBulanan != null) {
								Double subtotal = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa,
										semester);
								total += subtotal;

								String s = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "\\"
										+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " "
										+ pengaturanPembayaranBulanan.getNamaBulan() + "\\" + subtotal.intValue();

								rincian += rincian.isEmpty() ? s : "|" + s;
							}
						} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();

							if (pengaturanPembayaranBulanan != null) {
								Double subtotal = 0.0;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[spl.length - 1]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:844");
								}

								if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
										.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									subtotal = 0.0 - subtotal;
								}
								total += subtotal;

								String s = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "\\"
										+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + " "
										+ pengaturanPembayaranBulanan.getNamaBulan() + "\\" + subtotal.intValue();

								rincian += rincian.isEmpty() ? s : "|" + s;
							}
						} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
									session.createCriteria(ItemBiaya.class)
											.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
									ItemBiaya.class);

							if (itemBiaya != null) {

								Double subtotal = 0.0;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[2]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:871");
								}

								@SuppressWarnings("unused")
								Integer bayarke = 1;
								try {
									String[] spl = idPemBul.split("-");
									bayarke = Integer.parseInt(spl[3]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:879");
								}

								if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									subtotal = 0.0 - subtotal;
								}
								total += subtotal;

								String s = itemBiaya.getKode() + "\\" + itemBiaya.getNama() + "\\"
										+ subtotal.intValue();

								rincian += rincian.isEmpty() ? s : "|" + s;
							}
						}
					}
					VirtualAccountBank.updateTotal(virtualAccountBankNtt, total);
				}

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
					String s = "000\\Biaya Admin\\" + virtualAccountBankNtt.getBiayaAdmin().intValue();
					total += virtualAccountBankNtt.getBiayaAdmin();
					rincian += rincian.isEmpty() ? s : "|" + s;
				}

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
						&& virtualAccountBankNtt.getJadwalPembayaran() != null) {

					Double ds = Double.parseDouble(nominalTagihan);

					if (total.intValue() == ds.intValue()) {

						if (Common.dateFormat1.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate())
								.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))

								|| Common.dateFormat1.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate())
										.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))

								|| (virtualAccountBankNtt.getJadwalPembayaran().getEndDate()
										.after(ais.ui.util.WaktuUtil.getDate())
										&& virtualAccountBankNtt.getJadwalPembayaran().getStartDate()
												.before(ais.ui.util.WaktuUtil.getDate()))) {
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
							kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
							kegiatan.setValidated(1);
							kegiatan.setValidator(
									bankHost == null || bankHost.getNama() == null ? userID : bankHost.getNama());

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, kegiatan);
							session.getTransaction().commit();

							List<Long> detailBiayasId = new ArrayList<Long>();
							for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
								try {
									detailBiayasId.add(Long.parseLong(id.trim()));
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:965");

								}
							}

							Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
									.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", detailBiayasId))
									.list();

							Double nilaiBiayaHarusDiBayars = 0.0;
							for (DetailBiaya detailBiaya : detailBiayas) {
								Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

								nilaiBiayaHarusDiBayars += biaya;

							}

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

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														pengaturanPembayaranBulanan.getDetailBiaya());
												cicilanPembayaran.setRef(ref);
												cicilanPembayaran.setValidator(userID);
												cicilanPembayaran.setKegiatan(kegiatan);
												cicilanPembayaran.setItemBiaya(
														pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya());
												cicilanPembayaran
														.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
												cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
												cicilanPembayaran.setNilai(pengaturanPembayaranBulanan
														.ambilNominalModifikasi(mahasiswa, semester));
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												cicilanPembayaran.setTanggal(ais.ui.util.WaktuUtil.getDate());
												cicilanPembayaran.setJenisPembayaran(
														bankHost == null || bankHost.getJenisPembayaran() == null
																? ConstantValues.TUNAI
																: bankHost.getJenisPembayaran());
												cicilanPembayaran.setDenda(0.0);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

												session.getTransaction().begin();
												if (cicilanPembayaran.getId() == null)
													session.save(cicilanPembayaran);
												else
													Common.refreshUpdate(session, cicilanPembayaran);
												session.getTransaction().commit();
											} else {
												data.add(new String[] { "response_code", "03" });
												data.add(new String[] { "response_description", "Bill Already paid " });
												telahbayar = true;
											}

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

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														pengaturanPembayaranBulanan.getDetailBiaya());
												cicilanPembayaran.setRef(ref);
												cicilanPembayaran.setValidator(userID);
												cicilanPembayaran.setKegiatan(kegiatan);
												cicilanPembayaran.setItemBiaya(
														pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya());
												cicilanPembayaran
														.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
												cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

												Double subtotal = 0.0;
												try {
													String[] spl = idPemBul.split("-");
													subtotal = Double.parseDouble(spl[spl.length - 1]);
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:1064");
												}

												if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
														.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
													subtotal = 0.0 - subtotal;
												}

												cicilanPembayaran.setNilai(subtotal);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												cicilanPembayaran.setTanggal(ais.ui.util.WaktuUtil.getDate());
												cicilanPembayaran.setJenisPembayaran(
														bankHost == null || bankHost.getJenisPembayaran() == null
																? ConstantValues.TUNAI
																: bankHost.getJenisPembayaran());
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
											} else {
												data.add(new String[] { "response_code", "03" });
												data.add(new String[] { "response_description", "Bill Already paid " });
												telahbayar = true;
											}

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:1117");
											}

											Long detailBiayaId = null;
											try {
												String[] spl = idPemBul.split("-");
												detailBiayaId = Long.parseLong(spl[4]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/PembayaranAction.java:1124");
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														DetailBiaya.muatRefAman(session, detailBiayaId));
												cicilanPembayaran.setRef(ref);
												cicilanPembayaran.setValidator(userID);
												cicilanPembayaran.setKegiatan(kegiatan);
												cicilanPembayaran.setItemBiaya(itemBiaya);
												cicilanPembayaran.setPengaturanPembayaranBulanan(null);
												cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

												if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
													subtotal = 0.0 - subtotal;
												}

												cicilanPembayaran.setNilai(subtotal);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												cicilanPembayaran.setTanggal(ais.ui.util.WaktuUtil.getDate());
												cicilanPembayaran.setJenisPembayaran(
														bankHost == null || bankHost.getJenisPembayaran() == null
																? ConstantValues.TUNAI
																: bankHost.getJenisPembayaran());
												cicilanPembayaran.setDenda(0.0);
												cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
												session.getTransaction().begin();
												if (cicilanPembayaran.getId() == null)
													session.save(cicilanPembayaran);
												else
													Common.refreshUpdate(session, cicilanPembayaran);
												session.getTransaction().commit();
											} else {
												data.add(new String[] { "response_code", "03" });
												data.add(new String[] { "response_description", "Bill Already paid " });
												telahbayar = true;
											}

										}
									} else if (idPemBul.startsWith("Keranjang-")) {
										// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
										// menjadi Kegiatan+Cicilan nyata — pemroses terpusat yang sama dengan Esmartlink.
										ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
												virtualAccountBankNtt, false, userID, bankHost, ais.ui.util.WaktuUtil.getDate(),
												null, null);
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
							kegiatan.setValidator(userID);

							session.getTransaction().begin();
							Common.refreshUpdate(session, kegiatan);
							session.getTransaction().commit();
							VirtualAccountBank.updateVa(virtualAccountBankNtt, WaktuUtil.getDate(), kegiatan, nama,
									userID);
						}
					}

					if (Common.dateFormat1.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate())
							.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))

							|| Common.dateFormat1.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate())
									.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))

							|| virtualAccountBankNtt.getJadwalPembayaran().getEndDate()
									.after(ais.ui.util.WaktuUtil.getDate())
									&& virtualAccountBankNtt.getJadwalPembayaran().getStartDate()
											.before(ais.ui.util.WaktuUtil.getDate())) {
						data.add(
								new String[] { "response_code", total.intValue() == ds.intValue() ? ConstantUtil.SUCCESS
										: ConstantUtil.NOT_VALID_AMOUNT });
						data.add(new String[] { "response_description",
								total.intValue() == ds.intValue() ? "Pembayaran Sukses Dilakukan"
										: "Nominal pembayaran Tidak Sesuai" });
					} else {
						data.add(new String[] { "response_code", ConstantUtil.BILLS_HAS_EXPIRED });
						data.add(new String[] { "response_description",
								"Tidak ada jadwal pembayaran atau telah kadaluarsa" });
					}

					data.add(new String[] { "nim", nim });
					data.add(new String[] { "kurs", "IDR" });
					data.add(new String[] { "nama",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getNama()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getNama() });
					data.add(new String[] { "program",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getProgram()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProgram() });
					data.add(new String[] { "fakultas",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getJurusan().getFakultas().getNama()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus() != null
											? virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus()
													.getFakultas().getNama()
											: (virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1() == null ? ""
													: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1()
															.getFakultas().getNama()) });
					data.add(new String[] { "prodi",
							virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getJurusan().getNama()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus() != null
											? virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdiLulus().getNama()
											: (virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1() == null ? ""
													: virtualAccountBankNtt.getBiodataCalonMahasiswa().getProdi1()
															.getNama()) });
					data.add(new String[] { "angkatan",
							(virtualAccountBankNtt.getMahasiswa() != null
									? virtualAccountBankNtt.getMahasiswa().getTahunangkatan()
									: virtualAccountBankNtt.getBiodataCalonMahasiswa().getTahun()) + "" });
					data.add(new String[] { "semester",
							virtualAccountBankNtt.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
					data.add(new String[] { "semester_ke", virtualAccountBankNtt.getSemester() + "" });
					data.add(new String[] { "tanggal_max",
							Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate()) });
					data.add(new String[] { "tanggal_min",
							Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate()) });
					data.add(new String[] { "amount", "|" + rincian + "|" });
					data.add(new String[] { "total_amount", total.intValue() + "" });
					data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
					data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
					data.add(new String[] { "reference_number", nim });
					data.add(new String[] { "jumlah_yang_telah_dibayar", "0" });
				} else {
					data.add(new String[] { "response_code", ConstantUtil.NIM_NOT_FOUND });
					data.add(new String[] { "response_description",
							"Kode pembayaran tidak ditemukan atau tagihan telah dibayar" });
					data.add(new String[] { "nim", nim });
					data.add(new String[] { "kurs", "IDR" });
					data.add(new String[] { "nama",
							mahasiswa == null ? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNama())
									: mahasiswa.getNama() });
					data.add(new String[] { "program",
							mahasiswa == null
									? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getProgram())
									: mahasiswa.getProgram() });
					data.add(
							new String[] { "fakultas",
									mahasiswa == null
											? (biodataCalonMahasiswa == null ? ""
													: biodataCalonMahasiswa.getProdiLulus() != null
															? biodataCalonMahasiswa.getProdiLulus().getFakultas()
																	.getNama()
															: (biodataCalonMahasiswa.getProdi1() != null
																	? biodataCalonMahasiswa.getProdi1().getFakultas()
																			.getNama()
																	: ""))
											: mahasiswa.getJurusan().getFakultas().getNama() });
					data.add(
							new String[] { "prodi",
									mahasiswa == null
											? (biodataCalonMahasiswa == null ? ""
													: biodataCalonMahasiswa.getProdiLulus() != null
															? biodataCalonMahasiswa.getProdiLulus().getNama()
															: (biodataCalonMahasiswa.getProdi1() != null
																	? biodataCalonMahasiswa.getProdi1().getNama()
																	: ""))
											: mahasiswa.getJurusan().getNama() });
					data.add(new String[] { "angkatan",
							mahasiswa == null
									? (biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getTahun() + "")
									: mahasiswa.getTahunangkatan() + "" });
					data.add(new String[] { "semester", virtualAccountBankNtt == null ? ""
							: virtualAccountBankNtt.getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
					data.add(new String[] { "semester_ke",
							virtualAccountBankNtt == null ? "" : virtualAccountBankNtt.getSemester() + "" });
					data.add(new String[] { "tanggal_max", virtualAccountBankNtt == null ? ""
							: Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getEndDate()) });
					data.add(new String[] { "tanggal_min", virtualAccountBankNtt == null ? ""
							: Common.dateFormat2.get().format(virtualAccountBankNtt.getJadwalPembayaran().getStartDate()) });
					data.add(new String[] { "amount", "|" + rincian + "|" });
					data.add(new String[] { "total_amount", total.intValue() + "" });
					data.add(new String[] { "kode_status_pembayaran",
							virtualAccountBankNtt == null ? "" : jenisKegiatan.getKode() });
					data.add(new String[] { "keterangan_status_pembayaran",
							virtualAccountBankNtt == null ? "" : jenisKegiatan.getNamaKegiatan() });
					data.add(new String[] { "reference_number",
							virtualAccountBankNtt == null ? "" : virtualAccountBankNtt.getKode() });
					data.add(new String[] { "jumlah_yang_telah_dibayar", "0" });
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();

			Session session = HibernateUtil.currentNativeSession();
			try {
				logHostToHost.setBankHost(bankHost);
				logHostToHost.setIp(bankHost.getIp());
				logHostToHost.setNama(nama);
				logHostToHost.setNim(nim);
				logHostToHost.setKeterangan(CommonUtil.convertToString(data));
				CommonUtil.setRequestAndresponse(logHostToHost);
				logHostToHost.setTransactionType(ConstantUtil.PAY);
				CommonUtil.setRequestAndresponse(logHostToHost);

				// JAMINAN: simpan via helper bersama (session terdedikasi + commit + retry, tak gagal).
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/PembayaranAction.java:1337");
			}

			HibernateUtil.closeSession();

			if (telahbayar) {
				data.add(new String[] { "response_code", "03" });
				data.add(new String[] { "response_description", "Bill Already paid " });
			}

			return CommonUtil.convertToResponse(data);
		} else {

			Response response = PembayaranAction.payByNim(nim, bankHost, nama, logHostToHost, nominalTagihan);
			return response;
		}
	}

	public static Response payByNim(String nim, BankHost bankHost, String nama, LogHostToHost logHostToHost,
			String nominalTagihan) {
		Mahasiswa mahasiswa = null;
		String kode = null;
		String bulan = null;

		try {
			Object[] objects = inqueryLogic.pembayaranUtil.ambilDataMahasiswa(nim);
			mahasiswa = (Mahasiswa) objects[0];
			kode = (String) objects[1];
			bulan = (String) objects[2];
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (mahasiswa == null) {
			// payment untuk calon mahasiswa
			BiodataCalonMahasiswa biodataCalonMahasiswa = paymentLogic.pembayaranUtil.getCalonMahasiswaByNoUjian(nim);
			if (biodataCalonMahasiswa == null) {

				biodataCalonMahasiswa = paymentLogic.pembayaranUtil.getCalonMahasiswaByNoPendaftaran(nim);
				if (biodataCalonMahasiswa == null) {
					return CommonUtil.convertToResponse(paymentLogic.displayUtil.displayNimNotFound(logHostToHost, nim,
							bankHost, nama, ConstantUtil.PAY));
				} else {

					nama = ("pay calon mahasiswa dengan no pendaftaran = " + nim);
					return CommonUtil.convertToResponse(paymentLogic.pembayaranCalonMahasiswa(biodataCalonMahasiswa,
							bankHost, nama, Double.parseDouble(nominalTagihan), logHostToHost));

				}
			} else {
				nama = ("pay calon mahasiswa dengan no ujian = " + nim);
				return CommonUtil.convertToResponse(paymentLogic.pembayaranMahasiswaBaru(biodataCalonMahasiswa,
						bankHost, nama, Double.parseDouble(nominalTagihan), logHostToHost));
			}
		} else {
			mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler" : mahasiswa.getProgram());
			return CommonUtil.convertToResponse(paymentLogic.pembayaranMahasiswaLama(mahasiswa, bankHost, nama,
					Double.parseDouble(nominalTagihan), logHostToHost, kode, bulan, nim));
		}
	}
}
