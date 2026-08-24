package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.kursus.PesertaPunyaProdukKursus;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.WaktuUtil;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "virtual_account_bank")
public class VirtualAccountBank extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;
	private Boolean pakaiva;
	private String htmlTemporaryData;

	public static Set<Long> sukses = Collections.synchronizedSet(new HashSet<Long>());

	/**
	 * Guard umum supaya callback/payment VA yang sama tidak diproses ulang.
	 * Dicek dari dua sumber:
	 * 1) cache in-memory sukses, untuk callback berulang dalam JVM yang sama;
	 * 2) penanda permanen di database: kegiatan, pembayaran siswa, atau deposit/topup.
	 */
	public static boolean isSudahTerbayar(VirtualAccountBank virtualAccountBank) {
		if (virtualAccountBank == null) {
			return false;
		}

		Long id = virtualAccountBank.getId();
		if (id != null && sukses.contains(id)) {
			return true;
		}

		Double total = virtualAccountBank.getTotal();
		boolean punyaNilaiTagihan = total != null && total.doubleValue() > 0.1;
		if (!punyaNilaiTagihan) {
			return false;
		}

		return virtualAccountBank.getKegiatan() != null || virtualAccountBank.getPembayaran() != null
				|| virtualAccountBank.getDeposit() != null;
	}

	public static boolean isSudahTerbayarUntukPayment(VirtualAccountBank virtualAccountBank, boolean inquery,
			boolean reversal, boolean chek) {
		return !inquery && !reversal && !chek && isSudahTerbayar(virtualAccountBank);
	}

	private static void sinkronkanKegiatanSetelahBayar(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}
		try {
			KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static String ambilDefaultValidatorBank(String prefix) {
		String bank = prefix == null ? "" : prefix.trim().toLowerCase();
		if ("bsi".equals(bank)) {
			return Common.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai();
		}
		if ("bri".equals(bank)) {
			return Common.getKonfigurasi("default_validator_bri", "BRI").getNilai();
		}
		if ("bni".equals(bank)) {
			return Common.getKonfigurasi("default_validator_bni", "BNI").getNilai();
		}
		return prefix == null || prefix.trim().length() == 0 ? "BANK" : prefix.trim().toUpperCase();
	}

	public static void bayarTopup(VirtualAccountBank virtualAccountBankNtt, Session session, Date tanggal, String bank,
			boolean inquery, String notif) {
		if (!inquery && virtualAccountBankNtt.getTopup() != null && virtualAccountBankNtt.getTopup() > 0.1) {
			Transaction tx = null;
			try {
				Deposit deposit = (Deposit) (virtualAccountBankNtt.getDeposit() == null ? null
						: session.createCriteria(Deposit.class)
								.add(Restrictions.idEq(virtualAccountBankNtt.getDeposit())).uniqueResult());

				if (deposit == null || deposit.getId() == null) {
					deposit = new Deposit();
				}

				deposit.setMahasiswa(virtualAccountBankNtt.getMahasiswa());
				deposit.setAnggotaKoperasi(virtualAccountBankNtt.getAnggotaKoperasi());
				deposit.setSiswa(virtualAccountBankNtt.getSiswa());
				deposit.setCalonSiswa(virtualAccountBankNtt.getCalonSiswa());
				deposit.setBiodataCalonMahasiswa(virtualAccountBankNtt.getBiodataCalonMahasiswa());
				deposit.setNominal(virtualAccountBankNtt.getTopup());
				deposit.setWaktu(tanggal);

				tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, deposit);
				tx.commit();

				virtualAccountBankNtt.setWaktuBayar(tanggal);
				virtualAccountBankNtt.setNotif(notif);

				if (virtualAccountBankNtt.getVa() == null) {
					Va va = new Va();
					va.setKode(virtualAccountBankNtt.getKode());
					tx = session.beginTransaction();
					session.save(va);
					tx.commit();
					virtualAccountBankNtt.setVa(va);
				}

				virtualAccountBankNtt.setDeposit(deposit.getId());
				tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, virtualAccountBankNtt);
				tx.commit();

			} catch (Exception e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}


	private static void updateVirtualAccountSiswaMinimal(Session session, VirtualAccountBank va, Date tanggal,
			Long pembayaranId, String notif) throws Exception {
		if (session == null || va == null || va.getId() == null) {
			return;
		}
		FlushMode oldFlushMode = null;
		Transaction tx = null;
		try {
			oldFlushMode = session.getFlushMode();
			session.setFlushMode(FlushMode.MANUAL);
			tx = session.beginTransaction();
			Query query = session.createQuery("update VirtualAccountBank set waktuBayar = :waktuBayar, "
					+ "pembayaran = :pembayaran, notif = :notif, va = :va where id = :id");
			query.setParameter("waktuBayar", tanggal);
			query.setParameter("pembayaran", pembayaranId);
			query.setParameter("notif", notif);
			query.setParameter("va", va.getVa());
			query.setParameter("id", va.getId());
			query.executeUpdate();
			tx.commit();
			va.setWaktuBayar(tanggal);
			va.setPembayaran(pembayaranId);
			va.setNotif(notif);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:224");}
			throw e;
		} finally {
			try { if (oldFlushMode != null) session.setFlushMode(oldFlushMode); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:227");}
		}
	}

	public static Map<String, List<Tagihan>> bayarSiswa(VirtualAccountBank virtualAccountBankNtt, Session session,
			Date tanggal, String bank, boolean inquery, String notif, boolean cetak) {

		AkunPembayaranSiswa akunPembayaranSiswa = virtualAccountBankNtt.getAkunPembayaranSiswa();
		if (akunPembayaranSiswa == null) {
			Sekolah sekolah = virtualAccountBankNtt.getSiswa() == null
					? virtualAccountBankNtt.getCalonSiswa().getSekolah()
					: virtualAccountBankNtt.getSiswa().getSekolah();

			akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
					.createCriteria(AkunPembayaranSiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
					AkunPembayaranSiswa.class);
		}

		Map<String, List<Tagihan>> maps = new HashMap<String, List<Tagihan>>();
		if (virtualAccountBankNtt.getCicilan() != null && !virtualAccountBankNtt.getCicilan().isEmpty()) {
			for (String c : virtualAccountBankNtt.getCicilan().split(",")) {
				String[] parts = c.split("-");
				if (parts.length > 1) {
					Long idTagihan = Long.parseLong(parts[1]);
					Tagihan tagihan = (Tagihan) session.createCriteria(Tagihan.class)
							.add(Restrictions.idEq(idTagihan)).uniqueResult();
					if (tagihan != null && tagihan.getNominalBiaya() != null) {
						Long id = tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getId();
						String key = id + "_" + tagihan.getTahunbulan();
						if (!maps.containsKey(key)) {
							maps.put(key, new ArrayList<Tagihan>());
						}
						maps.get(key).add(tagihan);
					} else {
						System.out.println("Token cicilan VA dilewati karena tagihan sudah tidak tersedia: idTagihan="
								+ idTagihan + ", virtualAccountBankNtt id=" + virtualAccountBankNtt.getId()
								+ ", cicilan mentah='" + virtualAccountBankNtt.getCicilan() + "'");
					}
				}
			}
		}

		if (!inquery) {
			// Cek in-memory sukses set — tidak perlu query DB, paling cepat.
			if (virtualAccountBankNtt.getId() != null && sukses.contains(virtualAccountBankNtt.getId())) {
				System.out.println("Pembayaran VA " + virtualAccountBankNtt + " sudah pernah diproses (cache). Proses siswa dilewati.");
				return maps;
			}
			// Cek langsung ke PembayaranSiswa berdasarkan FK virtualAccountBank —
			// tidak andalkan kolom pembayaran di VAB yang bisa basi (mis. saat Cek Ulang
			// atau data tidak konsisten). Jika belum ada → lanjut insert.
			Double totalVaBayarSiswa = virtualAccountBankNtt.getTotal();
			if (totalVaBayarSiswa != null && totalVaBayarSiswa > 0.1 && virtualAccountBankNtt.getId() != null) {
				long countPembayaranSiswa = ((Number) session.createCriteria(PembayaranSiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("virtualAccountBank", virtualAccountBankNtt))
						.uniqueResult()).longValue();
				if (countPembayaranSiswa > 0) {
					System.out.println("Pembayaran VA " + virtualAccountBankNtt + " sudah ada di PembayaranSiswa. Proses siswa dilewati.");
					return maps;
				}
			}
			Transaction tx = null;
			try {
				if (maps != null && !maps.isEmpty()) {

					// Fallback: cek juga via id PembayaranSiswa yang tersimpan di kolom VAB
					if (virtualAccountBankNtt.getPembayaran() != null) {
						int count = ((Number) session.createCriteria(PembayaranSiswa.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.idEq(virtualAccountBankNtt.getPembayaran())).uniqueResult())
								.intValue();
						if (count > 0) {
							System.out
									.println("Pembayaran dengan va " + virtualAccountBankNtt.toString() + " sudah ada");
							return maps;
						}
					}

					Double tabungan = virtualAccountBankNtt.getTabungan();
					if (tabungan != null && tabungan > virtualAccountBankNtt.getTotal()) {
						tabungan = virtualAccountBankNtt.getTotal();
					}

					PembayaranSiswa pembayaranSiswa = new PembayaranSiswa();
					pembayaranSiswa.setSiswa(virtualAccountBankNtt.getSiswa());
					pembayaranSiswa.setCalonSiswa(virtualAccountBankNtt.getCalonSiswa());
					pembayaranSiswa.setTanggal(tanggal);
					pembayaranSiswa.setKeterangan(virtualAccountBankNtt.getKeterangan());
					pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
					pembayaranSiswa.setNominal(virtualAccountBankNtt.getTotal());
					pembayaranSiswa.setVirtualAccountBank(virtualAccountBankNtt);
					pembayaranSiswa.setDariTabungan(tabungan);
					pembayaranSiswa.setDariTabunganManual(tabungan);
					pembayaranSiswa.setTambahanDeposit(virtualAccountBankNtt.getTotal());
					pembayaranSiswa.setValidator(bank);

					tx = session.beginTransaction();
					if (pembayaranSiswa.getId() == null)
						session.save(pembayaranSiswa);
					else
						Common.refreshSaveOrUpdate(session, pembayaranSiswa);
					tx.commit();

					tx = session.beginTransaction();
					pembayaranSiswa.saveOrUpdateDeposit(session);
					tx.commit();

					virtualAccountBankNtt.setWaktuBayar(tanggal);
					virtualAccountBankNtt.setPembayaran(pembayaranSiswa.getId());
					virtualAccountBankNtt.setNotif(notif);

					if (virtualAccountBankNtt.getVa() == null) {
						Va va = new Va();
						va.setKode(virtualAccountBankNtt.getKode());
						tx = session.beginTransaction();
						session.save(va);
						tx.commit();
						virtualAccountBankNtt.setVa(va);
					}

					for (List<Tagihan> details : maps.values()) {
						for (Tagihan tagihan : details) {
							try {
								NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
								if (nominalBiaya != null) {
									Double nominal = tagihan.getNominal() - tagihan.getDiskon() + tagihan.getDenda();

									if (virtualAccountBankNtt.getCicilan() != null) {
										for (String s : virtualAccountBankNtt.getCicilan().split(",")) {
											String[] ss = s.split("-");
											if (ss.length > 2 && tagihan.getId().equals(Long.parseLong(ss[1].trim()))) {
												nominal = Double.parseDouble(ss[2].trim());
												break;
											}
										}
									}

									PembayaranSiswaDetail pembayaranSiswaDetail = new PembayaranSiswaDetail(tagihan);
									pembayaranSiswaDetail.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
									pembayaranSiswaDetail.setNominalBiaya(nominalBiaya);
									pembayaranSiswaDetail.setNominal(nominal);
									pembayaranSiswaDetail.setNominalManual(nominal);
									pembayaranSiswaDetail.setPembayaranSiswa(pembayaranSiswa);

									tx = session.beginTransaction();
									session.save(pembayaranSiswaDetail);
									tx.commit();

									session.refresh(tagihan);
									tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);

									tx = session.beginTransaction();
									session.update(tagihan);
									tx.commit();

									if (tagihan.getDiskonSiswa() != null
											&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
										DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
									}
								}
							} catch (Exception e) {
								if (tx != null && tx.isActive())
									tx.rollback();
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:388");
							}
						}
					}

					updateVirtualAccountSiswaMinimal(session, virtualAccountBankNtt, tanggal, pembayaranSiswa.getId(), notif);

					PembayaranSiswaUtil.cetakVa(pembayaranSiswa, virtualAccountBankNtt, cetak);
				}
				sukses.add(virtualAccountBankNtt.getId());

			} catch (Exception e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:402");
			}

			if (virtualAccountBankNtt != null) {
				if (virtualAccountBankNtt.getCalonSiswa() != null
						&& virtualAccountBankNtt.getCalonSiswa().getId() != null) {
					virtualAccountBankNtt.getCalonSiswa().populatePembayaran();
				}
				if (virtualAccountBankNtt.getSiswa() != null && virtualAccountBankNtt.getSiswa().getId() != null) {
					virtualAccountBankNtt.getSiswa().populatePembayaran();
				}
			}
		}
		return maps;
	}

	public static PembayaranSiswa bayarSiswaLangsung(List<Tagihan> tagihans, Session session, Date tanggal,
			Double total, Double tabungan, String bank, boolean cetak, Tbmuser tbmuser) {

		if (tagihans == null || tagihans.isEmpty())
			return null;
		if (tabungan != null && tabungan > total)
			tabungan = total;

		Tagihan tagihanD = tagihans.get(0);
		Sekolah sekolah = tagihanD.getSiswa() == null ? tagihanD.getCalonSiswa().getSekolah()
				: tagihanD.getSiswa().getSekolah();

		AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
				.createCriteria(AkunPembayaranSiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("dariTabungan", true)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
				AkunPembayaranSiswa.class);

		if (akunPembayaranSiswa == null) {
			akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
					.createCriteria(AkunPembayaranSiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
					AkunPembayaranSiswa.class);
		}

		PembayaranSiswa pembayaranSiswa = null;
		Transaction tx = null;
		try {
			pembayaranSiswa = new PembayaranSiswa();
			pembayaranSiswa.setSiswa(tagihanD.getSiswa());
			pembayaranSiswa.setCalonSiswa(tagihanD.getCalonSiswa());
			pembayaranSiswa.setTanggal(tanggal);
			pembayaranSiswa.setKeterangan(tagihanD.getInformasi());
			pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
			pembayaranSiswa.setNominal(total);
			pembayaranSiswa.setDariTabungan(tabungan);
			// bayar via deposit: tambahanDeposit = 0 agar tidak membuat record DepositSiswa baru
			pembayaranSiswa.setTambahanDeposit(0.0);
			pembayaranSiswa.setValidator(bank);
			pembayaranSiswa.setDariTabunganManual(tabungan);
			// Reload tbmuser dari session aktif (attached) agar FK valid saat session.save().
			// Jika user tidak ada di DB → null → kolom nullable → tidak ada FK violation.
			if (tbmuser != null && tbmuser.getUserId() != null) {
				Tbmuser attachedUser = (Tbmuser) session.get(Tbmuser.class, tbmuser.getUserId());
				pembayaranSiswa.setValidatorUser(attachedUser);
			}

			tx = session.beginTransaction();
			if (pembayaranSiswa.getId() == null)
				session.save(pembayaranSiswa);
			else
				Common.refreshSaveOrUpdate(session, pembayaranSiswa);
			tx.commit();

			tx = session.beginTransaction();
			pembayaranSiswa.saveOrUpdateDeposit(session);
			tx.commit();

			for (Tagihan tagihan : tagihans) {
				// Reload dalam session baru agar tidak ada masalah detached entity
				Tagihan tagihanPersisten = (Tagihan) session.get(Tagihan.class, tagihan.getId());
				if (tagihanPersisten == null) continue;
				NominalBiaya nominalBiaya = tagihanPersisten.getNominalBiaya();
				if (nominalBiaya != null) {
					Double nominal = tagihanPersisten.getNominal() - tagihanPersisten.getDiskon() + tagihanPersisten.getDenda();

					PembayaranSiswaDetail pembayaranSiswaDetail = new PembayaranSiswaDetail(tagihanPersisten);
					pembayaranSiswaDetail.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
					pembayaranSiswaDetail.setNominalBiaya(nominalBiaya);
					pembayaranSiswaDetail.setNominal(nominal);
					pembayaranSiswaDetail.setNominalManual(nominal);
					pembayaranSiswaDetail.setPembayaranSiswa(pembayaranSiswa);

					tx = session.beginTransaction();
					session.save(pembayaranSiswaDetail);
					tx.commit();

					tagihanPersisten.setPembayaranSiswaDetail(pembayaranSiswaDetail);

					tx = session.beginTransaction();
					session.update(tagihanPersisten);
					tx.commit();

					if (tagihanPersisten.getDiskonSiswa() != null && !tagihanPersisten.getDiskonSiswa().getMemotongTagihan()) {
						DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihanPersisten);
					}
				}
			}

			PembayaranSiswaUtil.cetakVa(pembayaranSiswa, null, cetak);

		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:513");
		}

		return pembayaranSiswa;
	}

	private static VirtualAccountBank buatAtauChekTagihan(Session sessionBaru, String kode, Double nilai, String tag,
			Tagihan... tagihan) {
		VirtualAccountBank virtualAccountBank = (VirtualAccountBank) sessionBaru
				.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("refTagihan", tag))
				.add(Restrictions.isNull("waktuBayar")).setMaxResults(1).uniqueResult();

		if (virtualAccountBank == null && tagihan != null && tagihan.length > 0) {
			Transaction tx = null;
			try {
				String code = kode + "-" + tag;
				Va va = new Va();
				va.setKode(code);
				tx = sessionBaru.beginTransaction();
				sessionBaru.save(va);
				tx.commit();

				virtualAccountBank = new VirtualAccountBank();
				virtualAccountBank.setCicilan(tag);
				virtualAccountBank.setVa(va);
				virtualAccountBank.setSiswa(tagihan[0].getSiswa());
				virtualAccountBank.setTahunAkademik(tagihan[0].getTahunAjaran());
				virtualAccountBank.setPt(tagihan[0].getSekolah().getPerguruanTinggi() == null ? null
						: tagihan[0].getSekolah().getPerguruanTinggi().getId());
				virtualAccountBank.setAmount(nilai);

				tx = sessionBaru.beginTransaction();
				sessionBaru.save(virtualAccountBank);
				tx.commit();
			} catch (Exception e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:550");
			}
		}
		return virtualAccountBank;
	}

	public static VirtualAccountBank ambilByNisAja(String kode, Double nilai) {
		VirtualAccountBank virtualAccountBank = null;
		Siswa siswa = ConstantValues.ambilByNis(kode);

		if (siswa != null) {
			Session sessionBaru = null;
			try {
				sessionBaru = HibernateUtil.openSession();

				// REFACTOR: Loop 1 to 4 to dynamically divide and search instead of
				// copy-pasting 4 blocks
				for (int i = 1; i <= 4; i++) {
					@SuppressWarnings("unchecked")
					List<Tagihan> tagihanList = sessionBaru.createCriteria(Tagihan.class)
							.add(Restrictions.eq("siswa", siswa))
							.add(Restrictions.or(
									Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
											Restrictions.sqlRestriction(
													"this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
									Restrictions.isNull("pembayaranSiswaDetail")))
							.add(Restrictions.eq("nominal", nilai / i)).addOrder(Order.asc("tahunbulan"))
							.setMaxResults(i).list();

					if (tagihanList != null && tagihanList.size() == i) {
						StringBuilder tagBuilder = new StringBuilder();
						for (int j = 0; j < tagihanList.size(); j++) {
							tagBuilder.append(tagihanList.get(j).getId());
							// Mempertahankan quirk delimiter asli (koma atau titik koma)
							if (j < tagihanList.size() - 1) {
								tagBuilder.append((i == 4 && j == 2) ? ";" : ",");
							}
						}

						Tagihan[] tagArray = tagihanList.toArray(new Tagihan[0]);
						virtualAccountBank = buatAtauChekTagihan(sessionBaru, kode, nilai, tagBuilder.toString(),
								tagArray);
						break; // Stop loop once match is found and created
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:596");
			} finally {
				if (sessionBaru != null) {
					KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
				}
			}
		}
		return virtualAccountBank;
	}

	private static void initialiseVirtualAccountForCaller(VirtualAccountBank va) {
		if (va == null) {
			return;
		}
		try {
			if (va.getBankHost() != null) {
				va.getBankHost().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:614");
		}
		try {
			if (va.getJenisKegiatan() != null) {
				va.getJenisKegiatan().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:620");
		}
		try {
			if (va.getMahasiswa() != null) {
				va.getMahasiswa().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:626");
		}
		try {
			if (va.getBiodataCalonMahasiswa() != null) {
				va.getBiodataCalonMahasiswa().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:632");
		}
		try {
			if (va.getJadwalPembayaran() != null) {
				va.getJadwalPembayaran().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:638");
		}
	}

	public static VirtualAccountBank ambilVa(String kode, BankHost bankHost) {
		return ambilVa(kode, null, bankHost, null);
	}

	public static VirtualAccountBank ambilVa(String kode, Double nominalP, BankHost bankHost) {
		return ambilVa(kode, nominalP, bankHost, null);
	}

	public static VirtualAccountBank ambilVa(String kode, Double nominalP, BankHost bankHost, Criterion crit) {
		Session sessionBaru = null;
		VirtualAccountBank virtualAccountBankNtt = null;
		Transaction tx = null;
		try {
			sessionBaru = HibernateUtil.openSession();
			virtualAccountBankNtt = (VirtualAccountBank) sessionBaru.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.or(Restrictions.isNull("bankHost"), Restrictions.eq("bankHost", bankHost)))
					.add(Restrictions.eq("kode", kode)).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

			if (virtualAccountBankNtt == null && crit != null) {
				virtualAccountBankNtt = (VirtualAccountBank) sessionBaru.createCriteria(VirtualAccountBank.class)
						.add(crit).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			}

			if (virtualAccountBankNtt != null) {
				initialiseVirtualAccountForCaller(virtualAccountBankNtt);
			}

			if (virtualAccountBankNtt != null
					&& (virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
					&& virtualAccountBankNtt.getWaktuBayar() == null) {

				Date tanggal = (Date) sessionBaru.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("refVa", virtualAccountBankNtt.getId()))
						.setProjection(Projections.property("tanggal")).setMaxResults(1).uniqueResult();
				virtualAccountBankNtt.setWaktuBayar(tanggal);

				if (bankHost != null && virtualAccountBankNtt.getBankHost() == null) {
					virtualAccountBankNtt.setBankHost(bankHost);
				}

				if (virtualAccountBankNtt.getVa() == null) {
					Va va = new Va();
					va.setKode(virtualAccountBankNtt.getKode());
					tx = sessionBaru.beginTransaction();
					sessionBaru.save(va);
					tx.commit();
					virtualAccountBankNtt.setVa(va);
				}

				tx = sessionBaru.beginTransaction();
				sessionBaru.update(virtualAccountBankNtt);
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:698");
		} finally {
			if (sessionBaru != null) {
				KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
			}
		}

		try {
			if (virtualAccountBankNtt == null && nominalP != null && nominalP > 0.1) {
				virtualAccountBankNtt = ambilByNisAja(kode, nominalP);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:710");
		}

		return virtualAccountBankNtt;
	}

	public static VirtualAccountBank ambilLink(String link, BankHost bankHost) {
		Session sessionBaru = null;
		VirtualAccountBank virtualAccountBankNtt = null;
		Transaction tx = null;
		try {
			sessionBaru = HibernateUtil.openSession();
			virtualAccountBankNtt = (VirtualAccountBank) sessionBaru.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.or(Restrictions.isNull("bankHost"), Restrictions.eq("bankHost", bankHost)))
					.add(Restrictions.ilike("link", link, MatchMode.END)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (virtualAccountBankNtt != null) {
				initialiseVirtualAccountForCaller(virtualAccountBankNtt);
			}

			if (virtualAccountBankNtt != null
					&& (virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
					&& virtualAccountBankNtt.getWaktuBayar() == null) {

				Date tanggal = (Date) sessionBaru.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("refVa", virtualAccountBankNtt.getId()))
						.setProjection(Projections.property("tanggal")).setMaxResults(1).uniqueResult();
				virtualAccountBankNtt.setWaktuBayar(tanggal);

				if (virtualAccountBankNtt.getVa() == null) {
					Va va = new Va();
					va.setKode(virtualAccountBankNtt.getKode());
					tx = sessionBaru.beginTransaction();
					sessionBaru.save(va);
					tx.commit();
					virtualAccountBankNtt.setVa(va);
				}

				tx = sessionBaru.beginTransaction();
				sessionBaru.update(virtualAccountBankNtt);
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:756");
		} finally {
			if (sessionBaru != null) {
				KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
			}
		}
		return virtualAccountBankNtt;
	}

	// Metode bayarVa terlalu besar, sudah saya kelompokkan Transaction
	// boundaries-nya
	@SuppressWarnings("unchecked")
	public static void bayarVa(final VirtualAccountBank virtualAccountBankNtt, Date tanggal, String data,
			Session session) {
		Transaction tx = null;
		try {
			// Cek in-memory sukses set (tidak perlu query DB, paling cepat).
			if (virtualAccountBankNtt.getId() != null && sukses.contains(virtualAccountBankNtt.getId())) {
				System.out.println("Pembayaran VA " + virtualAccountBankNtt + " sudah pernah diproses (cache). Proses mahasiswa dilewati.");
				return;
			}
			// Cek langsung ke CicilanPembayaran berdasarkan refVa = VAB.id —
			// tidak andalkan kolom kegiatan di VAB yang bisa basi (mis. saat Cek Ulang
			// atau data tidak konsisten). Jika belum ada → lanjut insert.
			// Kode di bawah sudah idempoten: Kegiatan→refreshSaveOrUpdate;
			// CicilanPembayaran→cek ref sebelum insert, update jika sudah ada.
			Double totalVaBayarVa = virtualAccountBankNtt.getTotal();
			boolean adaCicilan = virtualAccountBankNtt.getCicilan() != null
					&& !virtualAccountBankNtt.getCicilan().trim().isEmpty();
			if (totalVaBayarVa != null && totalVaBayarVa > 0.1
					&& virtualAccountBankNtt.getId() != null && adaCicilan) {
				// Bandingkan SUM(nilai) cicilan yang ada dengan total VA —
				// count saja tidak cukup karena mungkin hanya sebagian cicilan yang terhapus
				// (mis. dari 2 cicilan, 1 masih ada → count > 0 tapi data belum lengkap).
				Double sumCicilanVa = (Double) session.createCriteria(CicilanPembayaran.class)
						.setProjection(Projections.sum("nilai"))
						.add(Restrictions.eq("refVa", virtualAccountBankNtt.getId()))
						.uniqueResult();
				if (sumCicilanVa != null) {
					long sumBulat = Math.round(sumCicilanVa);
					long totalBulat = Math.round(totalVaBayarVa);
					if (sumBulat >= totalBulat) {
						System.out.println("Pembayaran VA " + virtualAccountBankNtt
								+ " sum CicilanPembayaran (" + sumBulat + ") >= total (" + totalBulat
								+ "). Proses mahasiswa dilewati.");
						return;
					}
					System.out.println("Pembayaran VA " + virtualAccountBankNtt
							+ " CicilanPembayaran belum lengkap (sum=" + sumBulat
							+ " < total=" + totalBulat + "). Lanjut insert cicilan yang kurang.");
				}
			}

			JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
			Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
			BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
			Integer semester = virtualAccountBankNtt.getSemester();
			String nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

			if (!virtualAccountBankNtt.getDetailbiaya().isEmpty() || !virtualAccountBankNtt.getCicilan().isEmpty()) {

				Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
						: session.createCriteria(Kegiatan.class)
								.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan())).uniqueResult());

				if (kegiatan == null || kegiatan.getId() == null) {
					kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).addOrder(Order.asc("id"))
							.add(biodataCalonMahasiswa != null
									? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
									: Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("jenisKegiatan", virtualAccountBankNtt.getJenisKegiatan()))
							.add(Restrictions.eq("semster", semester)).setMaxResults(1).uniqueResult();
				}

				if (kegiatan == null || kegiatan.getId() == null)
					kegiatan = new Kegiatan();

				kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
				kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
				kegiatan.setNama(nama);
				kegiatan.setAmount(virtualAccountBankNtt.getTotal());
				kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
				kegiatan.setMahasiswa(mahasiswa);
				kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
				kegiatan.setSemster(semester);
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setTanggal(tanggal);
				kegiatan.setValidated(1);
				kegiatan.setValidator(virtualAccountBankNtt.getBank());

				tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, kegiatan);
				tx.commit();

				List<Long> detailBiayasId = new ArrayList<Long>();
				for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
					try {
						detailBiayasId.add(Long.parseLong(id.trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:854");
					}
				}

				Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
						.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("id", detailBiayasId))
						.list();

				Double nilaiBiayaHarusDiBayars = 0.0;
				for (DetailBiaya detailBiaya : detailBiayas) {
					nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
				}

				if (virtualAccountBankNtt.getCicilan() != null && !virtualAccountBankNtt.getCicilan().isEmpty()) {
					for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {

						if (Common.isNumber(idPemBul) || idPemBul.startsWith("Bulanan-")) {
							Long idToSearch = Common.isNumber(idPemBul) ? Long.parseLong(idPemBul)
									: Long.parseLong(idPemBul.split("-")[1]);
							PengaturanPembayaranBulanan ppBln = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.idEq(idToSearch)).uniqueResult();

							if (ppBln != null) {
								String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
										+ virtualAccountBankNtt.getId();
								ItemBiaya itemBiaya = ppBln.getDetailBiaya().getItemBiaya();
								Double subtotal = 0.0;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[spl.length - 1]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:886");
								}

								if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
									subtotal = 0.0 - subtotal;

								CicilanPembayaran cp = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cp == null)
									cp = new CicilanPembayaran(ppBln.getDetailBiaya());

								cp.setRef(ref);
								cp.setValidator(virtualAccountBankNtt.getBank());
								cp.setKegiatan(kegiatan);
								cp.setItemBiaya(itemBiaya);
								cp.setPengaturanPembayaranBulanan(ppBln);
								cp.setRefVa(virtualAccountBankNtt.getId());
								cp.setNilai(subtotal);
								cp.setNilaiAsli(subtotal);
								cp.setTanggal(tanggal);
								cp.setJenisPembayaran(virtualAccountBankNtt.getBankHost() == null
										|| virtualAccountBankNtt.getBankHost().getJenisPembayaran() == null
												? ConstantValues.TUNAI
												: virtualAccountBankNtt.getBankHost().getJenisPembayaran());
								cp.setDenda(0.0);

								tx = session.beginTransaction();
								if (cp.getId() == null)
									session.save(cp);
								else
									Common.refreshUpdate(session, cp);
								tx.commit();
							}
						} else if (idPemBul.startsWith("Keranjang-")) {
							// Logic ini cukup panjang dan membuka session lokal, mari kita proteksi
							Session sessionLocalKeg = null;
							Transaction txLocal = null;
							try {
								Long idKeranjang = Long.parseLong(idPemBul.split("-")[1]);
								KegiatanTemporary kegTemp = (KegiatanTemporary) session
										.createCriteria(KegiatanTemporary.class).add(Restrictions.idEq(idKeranjang))
										.uniqueResult();

								if (kegTemp != null) {
									String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
											+ virtualAccountBankNtt.getId();
									sessionLocalKeg = HibernateUtil.openSession();
									kegTemp = (KegiatanTemporary) sessionLocalKeg.createCriteria(KegiatanTemporary.class)
											.add(Restrictions.idEq(idKeranjang)).uniqueResult();
									if (kegTemp == null) {
										continue;
									}

									mahasiswa = kegTemp.getMahasiswa();
									Integer smt = kegTemp.getSemster();
									jenisKegiatan = kegTemp.getJenisKegiatan();
									if (mahasiswa == null || smt == null || jenisKegiatan == null) {
										continue;
									}
									Kegiatan kegLocal = mahasiswa.ambilKegiatans(smt, jenisKegiatan);

									if (kegLocal == null || kegLocal.getId() == null)
										kegLocal = new Kegiatan();
									else
										kegLocal = (Kegiatan) sessionLocalKeg.createCriteria(Kegiatan.class)
												.add(Restrictions.idEq(kegLocal.getId())).uniqueResult();

									kegLocal.setJenisKegiatan(jenisKegiatan);
									kegLocal.setJadwalPembayaran(kegTemp.getJadwalPembayaran());
									kegLocal.setMahasiswa(mahasiswa);
									kegLocal.setSemster(smt);
									kegLocal.setStatusMahasiswa(kegTemp.getStatusMahasiswa());
									kegLocal.setTahunAkademik(kegTemp.getTahunAkademik());
									kegLocal.setTanggal(tanggal);
									kegLocal.setValidated(1);
									kegLocal.setValidator(virtualAccountBankNtt.getBank());
									kegLocal.setKeterangan(kegTemp.getKeterangan());

									txLocal = sessionLocalKeg.beginTransaction();
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegLocal);
									txLocal.commit();
									sinkronkanKegiatanSetelahBayar(kegLocal);

									List<CicilanPembayaran> cps = sessionLocalKeg
											.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.eq("kegiatanTemporary", kegTemp)).list();
									for (CicilanPembayaran cp : cps) {
										cp.setRef(ref);
										cp.setKegiatan(kegLocal);
										cp.setValidator(virtualAccountBankNtt.getBank());
										cp.setTanggal(tanggal);
										cp.setJenisPembayaran(virtualAccountBankNtt.getBankHost() == null
												|| virtualAccountBankNtt.getBankHost().getJenisPembayaran() == null
														? ConstantValues.TUNAI
														: virtualAccountBankNtt.getBankHost().getJenisPembayaran());

										txLocal = sessionLocalKeg.beginTransaction();
										Common.refreshSaveOrUpdate(sessionLocalKeg, cp);
										txLocal.commit();
									}

									if (kegTemp.getKegiatan() == null || kegTemp.getKegiatan().getId() == null
											|| !kegTemp.getKegiatan().getId().equals(kegLocal.getId())) {
										kegTemp.setKegiatan(kegLocal);
										txLocal = sessionLocalKeg.beginTransaction();
										Common.refreshSaveOrUpdate(sessionLocalKeg, kegTemp);
										txLocal.commit();
									}

									Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.isNotNull("itemBiaya"))
											.add(Restrictions.eq("kegiatan", kegLocal))
											.setProjection(Projections.sum("nilai")).uniqueResult();

									Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();
									Map<Long, DetailBiaya> map = new HashMap<Long, DetailBiaya>();
									Collection<DetailBiaya> myDBs = PembayaranUtilHelper
											.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, false);
									for (Object o : myDBs) {
										if (o instanceof DetailBiaya)
											map.put(((DetailBiaya) o).getId(), (DetailBiaya) o);
										else if (o instanceof PengaturanPembayaranBulanan)
											map.put(((PengaturanPembayaranBulanan) o).getDetailBiaya().getId(),
													((PengaturanPembayaranBulanan) o).getDetailBiaya());
									}

									Double nHarusDibayar = 0.0;
									for (DetailBiaya db : map.values())
										nHarusDibayar += Kegiatan.ambilJumlahTagihan(kegLocal, db);

									kegLocal.setAmountTerhutang(nHarusDibayar - amountTotal);
									kegLocal.setAmount(amountTotal);
									txLocal = sessionLocalKeg.beginTransaction();
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegLocal);
									txLocal.commit();
									sinkronkanKegiatanSetelahBayar(kegLocal);
								}
							} catch (Exception e) {
								if (txLocal != null && txLocal.isActive())
									txLocal.rollback();
								Common.tampilErrorJikaAdmin(e);
							} finally {
								if (sessionLocalKeg != null) {
									KegiatanPersistenceHelper.closeNativeSession(sessionLocalKeg);
								}
							}
						} else if (idPemBul.startsWith("Item-")) {
							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
									session.createCriteria(ItemBiaya.class)
											.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
									ItemBiaya.class);

							if (itemBiaya != null) {
								String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
										+ virtualAccountBankNtt.getId();
								Double subtotal = 0.0;
								Long detailBiayaId = null;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[2]);
									detailBiayaId = Long.parseLong(spl[4]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1049");
								}

								if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
									subtotal = 0.0 - subtotal;

								CicilanPembayaran cp = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cp == null)
									cp = new CicilanPembayaran(
											DetailBiaya.muatRefAman(session, detailBiayaId));
								cp.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
								cp.setRef(ref);
								cp.setValidator(virtualAccountBankNtt.getBank());
								cp.setKegiatan(kegiatan);
								cp.setItemBiaya(itemBiaya);
								cp.setPengaturanPembayaranBulanan(null);
								cp.setRefVa(virtualAccountBankNtt.getId());
								cp.setNilai(subtotal);
								cp.setNilaiAsli(subtotal);
								cp.setTanggal(tanggal);
								cp.setJenisPembayaran(virtualAccountBankNtt.getBankHost() == null
										|| virtualAccountBankNtt.getBankHost().getJenisPembayaran() == null
												? ConstantValues.TUNAI
												: virtualAccountBankNtt.getBankHost().getJenisPembayaran());
								cp.setDenda(0.0);

								tx = session.beginTransaction();
								if (cp.getId() == null)
									session.save(cp);
								else
									Common.refreshUpdate(session, cp);
								tx.commit();
							}
						}
					}
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				kegiatan.setDenda(d[1]);
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (d[0] - d[1]));
				kegiatan.setAmount(d[0] > 0.1 ? d[0] : virtualAccountBankNtt.getTotal());
				kegiatan.setValidator(virtualAccountBankNtt.getBank());

				tx = session.beginTransaction();
				Common.refreshUpdate(session, kegiatan);
					tx.commit();
					sinkronkanKegiatanSetelahBayar(kegiatan);

				VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
						virtualAccountBankNtt.getBank());
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1106");
		}
	}


	public static void updateVa(final VirtualAccountBank virtualAccountBankNtt, final Date waktuBayar,
			final Kegiatan kegiatan, String notif, String bank) {

		if (virtualAccountBankNtt == null) {
			return;
		}

		final Long idVa = virtualAccountBankNtt.getId();
		final Long idKegiatan = kegiatan == null ? null : kegiatan.getId();
		final String kodeVa = virtualAccountBankNtt.getKode();
		final String notifFinal = notif;
		final String bankFinal = bank;

		virtualAccountBankNtt.setKegiatan(idKegiatan);
		virtualAccountBankNtt.setWaktuBayar(waktuBayar);
		virtualAccountBankNtt.setNotif(notifFinal);
		if (bankFinal != null && bankFinal.trim().length() > 0) {
			virtualAccountBankNtt.setBank(bankFinal);
		}

		Session sessionBaru = null;
		Transaction tx = null;
		try {
			sessionBaru = HibernateUtil.openSession();
			VirtualAccountBank vaDb = idVa == null ? null
					: (VirtualAccountBank) sessionBaru.get(VirtualAccountBank.class, idVa);
			if (vaDb == null) {
				vaDb = virtualAccountBankNtt;
			}

			vaDb.setKegiatan(idKegiatan);
			vaDb.setWaktuBayar(waktuBayar);
			vaDb.setNotif(notifFinal);
			if (bankFinal != null && bankFinal.trim().length() > 0) {
				vaDb.setBank(bankFinal);
			}

			if (vaDb.getVa() == null) {
				Va va = new Va();
				va.setKode(kodeVa);
				tx = sessionBaru.beginTransaction();
				sessionBaru.save(va);
				tx.commit();
				tx = null;
				vaDb.setVa(va);
			}

			tx = sessionBaru.beginTransaction();
			Common.refreshSaveOrUpdate(sessionBaru, vaDb);
			tx.commit();
			tx = null;

			if (idVa != null) {
				if (idKegiatan != null || vaDb.getPembayaran() != null || vaDb.getDeposit() != null) {
					sukses.add(idVa);
				} else {
					sukses.remove(idVa);
				}
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1174");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
		}

		try {
			if (kegiatan != null && kegiatan.getId() != null) {
				try {
					Mahasiswa mhs = virtualAccountBankNtt.getMahasiswa();
					BiodataCalonMahasiswa bio = virtualAccountBankNtt.getBiodataCalonMahasiswa();
					if (mhs != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
					} else if (bio != null) {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				List<CicilanPembayaran> cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
				System.out.println("virtualAccountBank -> " + virtualAccountBankNtt + " kegiatan " + kegiatan
						+ " jumlah angsuran " + cicilanPembayarans.size());
				cicilanPembayarans.clear();
				cicilanPembayarans = null;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
			if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getMahasiswa() == null) {
				boolean wajibBayarPersen = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_membayar_persen_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);
				boolean wajibBayar = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_melunasi_pembayaran_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);

				if (wajibBayarPersen || wajibBayar) {
					JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
					if (jenisKegiatan != null && kegiatan != null && kegiatan.getId() != null
							&& kegiatan.getJenisKegiatan() != null
							&& kegiatan.getJenisKegiatan().getId().equals(jenisKegiatan.getId())) {
						CommonReportHelper.checkGenNim(kegiatan);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	public static void updateTotal(final VirtualAccountBank virtualAccountBankNtt, final Double total) {
		if (virtualAccountBankNtt.getTotal() != null && total != null
				&& total.intValue() == virtualAccountBankNtt.getTotal().intValue()) {
			System.out.println("virtualAccountBankNtt -> " + virtualAccountBankNtt + " total sudah sama");
			return;
		}

		try {
			Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
			BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
			String nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

			virtualAccountBankNtt.setNama(nama + "-" + Common.getGeneratedBarCode());
			virtualAccountBankNtt.setTotal(total);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1242");
		}

		final Long idV = virtualAccountBankNtt.getId();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1251");
				}

				Session sessionBaru = null;
				Transaction tx = null;
				try {
					sessionBaru = HibernateUtil.openSession();
					tx = sessionBaru.beginTransaction();
					sessionBaru.createSQLQuery("update virtual_account_bank set total = :total where id = :id")
								.setParameter("total", total)
								.setParameter("id", idV)
								.executeUpdate();
					tx.commit();
				} catch (Exception e) {
					if (tx != null && tx.isActive())
						tx.rollback();
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1267");
				} finally {
					if (sessionBaru != null) {
							KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
						}
				}
			}
		}).start();
	}

	public static String populateCicilan(Grid gridCicilan) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		StringBuilder cicilanBuilder = new StringBuilder();

		for (Row row : mycicilanrows) {
			try {
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
				if (jumlahCicilan.getValue() != null
						&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {
					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");

					if (cicilanPembayaran.getId() == null) {
						PengaturanPembayaranBulanan biaya = cicilanPembayaran.getPengaturanPembayaranBulanan();
						Double nilai = jumlahCicilan.getValue();

						if (biaya != null) {
							if (cicilanBuilder.length() > 0)
								cicilanBuilder.append(",");
							cicilanBuilder.append("Bulanan-").append(biaya.getId()).append("-").append(nilai);
						} else {
							Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
							DetailBiaya detailBiaya = (DetailBiaya) (myItemBiaya.getSelectedItem() == null ? null
									: myItemBiaya.getSelectedItem().getValue());
							ItemBiaya itemBiaya = (cicilanPembayaran.getItemBiaya() != null
									&& cicilanPembayaran.getItemBiaya().getId() != null)
											? cicilanPembayaran.getItemBiaya()
											: detailBiaya.getItemBiaya();
							detailBiaya = cicilanPembayaran.getDetailBiaya() != null
									? cicilanPembayaran.getDetailBiaya()
									: detailBiaya;

							if (cicilanBuilder.length() > 0)
								cicilanBuilder.append(",");
							cicilanBuilder.append("Item-").append(itemBiaya.getId()).append("-").append(nilai)
									.append("-").append(detailBiaya == null ? 1 : detailBiaya.getBayarKe()).append("-")
									.append(detailBiaya == null ? "null" : detailBiaya.getId());
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1317");
			}
		}
		return cicilanBuilder.toString();
	}

	// --- SISA GETTER SETTER SAMA PERSIS SEPERTI SEBELUMNYA ---
	// Demi menyingkat respon teks agar tidak terpotong namun tetap lengkap

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId != null && !olehId.trim().isEmpty())
			this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh != null && !oleh.trim().isEmpty())
			this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		try {
			jenisKegiatan = getJenisKegiatan();
			return id + "-" + kode + "-" + jenisKegiatan + getSiswa() + "-" + getMahasiswa() + " - " + getCalonSiswa()
					+ "-" + getDetailbiaya() + "-" + tahunAkademik + "-" + semester;
		} catch (Exception e) {
			return id + "-" + kode;
		}
	}

	private String kode;
	private String nama;
	private String bank;
	private String barcode;
	private String keterangan;
	private String request;
	private String response;
	private String notif;
	private String link;
	private String bulanan;
	private String cicilan;
	private String channel;
	private String detailbiaya;
	private JenisKegiatan jenisKegiatan;
	private Va va;
	private Siswa siswa;
	private CalonSiswa calonSiswa;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private JadwalPembayaran jadwalPembayaran;
	private Integer semester;
	private String tahunAkademik;
	private Date waktuBayar;
	private Double amount;
	private Double total;
	private Double biayaAdmin;
	private Long kegiatan;
	private Long deposit;
	private Long pembayaran;
	private Long pt;
	private Boolean otomatis;
	private Boolean kodeUnikLain;
	private Boolean terjadiKendala;
	private Date kadaluarsa;
	private Date kadaluarsaWaktu;
	private Date kadaluarsaBarcode;
	private BankHost bankHost;
	private String kelas;
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private AkunPembayaranSiswa akunPembayaranSiswa;
	private KanalPembayaran kanalPembayaran;
	private Double tabungan;
	private Double topup;
	private AnggotaKoperasi anggotaKoperasi;
	private CaraPembayaranKoperasi caraPembayaranKoperasi;

	private static String bersihkanTextDb(String value) {
		if (value == null) {
			return null;
		}
		return value.replace('\0', ' ').trim();
	}

	public VirtualAccountBank() {
	}

	public VirtualAccountBank(Long pt) {
		this.pt = pt;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "kode", updatable = false, nullable = false)
	public String getKode() {
		if (getOtomatis() && id != null) {
			try {
				String digitKetiga = "000000000000" + (id + 1);
				digitKetiga = digitKetiga.substring(digitKetiga.length() - 5);
				String kd = org.apache.commons.lang3.StringUtils.replace(
						org.apache.commons.lang3.StringUtils.replace(jenisKegiatan.getKode(), ".", ""), ",", "");
				if (kd.length() > 3)
					kd = kd.substring(kd.length() - 3);
				kode = kd + digitKetiga;
			} catch (Exception e) {
				String digitKetiga = "000000000000" + (id + 1);
				kode = digitKetiga.substring(digitKetiga.length() - 8);
			}
		}
		if (getRequest() != null && !getRequest().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				JSONObject jsonObject = new JSONObject(getRequest());
				if (!jsonObject.isNull("order_id"))
					kode = jsonObject.getString("order_id").trim();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1464");
			}
		} else {
			try {
				if (va != null && va.getKode() != null && !va.getKode().trim().isEmpty())
					kode = getVa().getKode();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1470");
			}
		}
		return kode;
	}

	public void setKode(String kode) {
		if (kode != null && !kode.trim().isEmpty() && (this.kode == null || this.kode.trim().isEmpty()))
			this.kode = kode;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getBank() != null && getBank().equalsIgnoreCase("flip") && getNotif() != null
				&& !getNotif().trim().isEmpty()) {
			try {
				nama = new JSONObject(getNotif()).get("id") + "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1487");
			}
		}
		return this.nama == null ? Common.getGeneratedBarCode() : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = bersihkanTextDb(nama);
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = bersihkanTextDb(keterangan);
	}

	public Double getTotal() {
		return total == null ? 0.0 : total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public String getBulanan() {
		return bulanan == null ? "" : bulanan.trim();
	}

	public void setBulanan(String bulanan) {
		this.bulanan = bulanan;
	}

	public String getDetailbiaya() {
		return detailbiaya == null ? "" : detailbiaya.trim();
	}

	public void setDetailbiaya(String detailbiaya) {
		this.detailbiaya = bersihkanTextDb(detailbiaya);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan = check(jenisKegiatan);
	}

	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa = check(mahasiswa);
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa = check(biodataCalonMahasiswa);
	}

	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		return jadwalPembayaran = check(jadwalPembayaran);
	}

	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	public Integer getSemester() {
		jenisKegiatan = getJenisKegiatan();
		if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			semester = 0;
		}
		return semester;
	}

	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			RencanaTahunAkademik s = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
					mahasiswa == null || mahasiswa.getJurusan() != null ? null : mahasiswa.getJurusan().getFakultas(),
					mahasiswa == null ? null : mahasiswa.getJurusan(),
					siswa != null ? siswa.getYayasan() : (calonSiswa != null ? calonSiswa.getYayasan() : null),
					siswa != null ? siswa.getSekolah() : (calonSiswa != null ? calonSiswa.getSekolah() : null),
					mahasiswa == null ? null : mahasiswa.getStatusAwalMahasiswa(),
					mahasiswa == null ? null : mahasiswa.getTahunangkatan(),
					mahasiswa == null ? null : mahasiswa.getProgram(),
					getWaktuBayar() == null ? getTanggal_dirubah() : getWaktuBayar(), null, null);
			if (s != null)
				tahunAkademik = s.getNama();
		}
		return tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	public String getBank() {
		if (getBankHost() != null && getBankHost().getNama() != null)
			bank = getBankHost().getNama();
		return bank;
	}

	public void setBank(String bank) {
		this.bank = bersihkanTextDb(bank);
	}

	public Long getKegiatan() {
		String k = retreive("k");
		String hapus = retreive("hapus");
		if (k != null && !k.isEmpty())
			kegiatan = Long.parseLong(k);
		if (hapus != null && hapus.equalsIgnoreCase("1"))
			kegiatan = null;
		return kegiatan;
	}

	public void setKegiatan(Long kegiatan) {
		if (kegiatan != null) {
			put(kegiatan.toString(), "k");
			put("0", "hapus");
		} else {
			put("1", "hapus");
			put(null, "k");
		}
		this.kegiatan = kegiatan;
	}

	public Boolean getKodeUnikLain() {
		return kodeUnikLain == null ? false : kodeUnikLain;
	}

	public void setKodeUnikLain(Boolean kodeUnikLain) {
		this.kodeUnikLain = kodeUnikLain;
	}

	@Column(columnDefinition = "text")
	public String getCicilan() {
		return cicilan == null ? "" : cicilan.trim();
	}

	public void setCicilan(String cicilan) {
		this.cicilan = bersihkanTextDb(cicilan);
	}

	@Column(name = "request", nullable = true, columnDefinition = "text")
	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = bersihkanTextDb(request);
	}

	@Column(name = "response", nullable = true, columnDefinition = "text")
	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = bersihkanTextDb(response);
	}

	public Boolean getOtomatis() {
		return otomatis == null ? false : otomatis;
	}

	public void setOtomatis(Boolean otomatis) {
		this.otomatis = otomatis;
	}

	public Boolean getTerjadiKendala() {
		return terjadiKendala == null ? false : terjadiKendala;
	}

	public void setTerjadiKendala(Boolean terjadiKendala) {
		this.terjadiKendala = terjadiKendala;
	}

	public int totalBiaya() {
		VirtualAccountBank virtualAccountBankNtt = this;
		return (virtualAccountBankNtt.getBiayaAdmin().intValue() + virtualAccountBankNtt.getTotal().intValue());
	}

	public Double getBiayaAdmin() {
		if (getNotif() != null && !getNotif().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				if (amount != null && amount > getTotal())
					biayaAdmin = amount - getTotal();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1697");
			}
		} else if (getBank() != null && getBank().equalsIgnoreCase("flip") && getNotif() != null
				&& !getNotif().trim().isEmpty()) {
			try {
				JSONObject notifJson = new JSONObject(getNotif());
				/* fee_amount tidak selalu dikirim Flip (mis. notifikasi lama/pending).
				 * Nilai ini opsional, jadi jangan memakai getter wajib yang melempar saat
				 * Hibernate memanggil property getter pada proses flush. */
				if (notifJson.has("fee_amount") && !notifJson.isNull("fee_amount")) {
					biayaAdmin = notifJson.optDouble("fee_amount", biayaAdmin == null ? 0.0 : biayaAdmin);
				}
			} catch (Exception e) {
				/* Notifikasi tidak valid tidak boleh menggagalkan flush entity. Pertahankan
				 * biaya admin yang sudah tersimpan (atau nol melalui return di bawah). */
				System.err.println("[VirtualAccountBank.getBiayaAdmin] notif Flip tidak dapat dibaca: "
						+ e.getMessage());
			}
		}
		return biayaAdmin == null ? 0.0 : biayaAdmin;
	}

	public void setBiayaAdmin(Double biayaAdmin) {
		this.biayaAdmin = biayaAdmin;
	}

	@Temporal(TemporalType.DATE)
	public Date getKadaluarsa() {
		jadwalPembayaran = getJadwalPembayaran();
		if (jadwalPembayaran != null && kadaluarsa == null && jadwalPembayaran.getEndDate() != null)
			kadaluarsa = jadwalPembayaran.getEndDate();
		return kadaluarsa;
	}

	public void setKadaluarsa(Date kadaluarsa) {
		if (kadaluarsaWaktu == null
				|| (kadaluarsa != null && !Common.timeFormat2.get().format(kadaluarsa).equals("00.00")))
			this.kadaluarsaWaktu = kadaluarsa;
		this.kadaluarsa = kadaluarsa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_host", nullable = true)
	public BankHost getBankHost() {
		return bankHost = check(bankHost);
	}

	public void setBankHost(BankHost bankHost) {
		this.bankHost = bankHost;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		return siswa = check(siswa);
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		return calonSiswa = check(calonSiswa);
	}

	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuBayar() {
		waktuBayar = getKegiatan() == null && getPembayaran() == null && getDeposit() == null ? null : waktuBayar;
		try {
			if (waktuBayar != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(waktuBayar);
				if (calendar.get(Calendar.YEAR) == 1970) {
					calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
					waktuBayar = calendar.getTime();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1770");
		}
		return waktuBayar;
	}

	public void setWaktuBayar(Date waktuBayar) {
		this.waktuBayar = waktuBayar;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = true)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		return pesertaPunyaProdukKursus;
	}

	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_pembayaran_siswa_id", nullable = true)
	public AkunPembayaranSiswa getAkunPembayaranSiswa() {
		akunPembayaranSiswa = check(akunPembayaranSiswa);
		if (akunPembayaranSiswa != null && (akunPembayaranSiswa.getDariTabungan() || akunPembayaranSiswa.getManual()))
			akunPembayaranSiswa = null;
		return akunPembayaranSiswa;
	}

	public void setAkunPembayaranSiswa(AkunPembayaranSiswa akunPembayaranSiswa) {
		this.akunPembayaranSiswa = akunPembayaranSiswa;
	}

	public Long getPembayaran() {
		return pembayaran;
	}

	public void setPembayaran(Long pembayaran) {
		this.pembayaran = pembayaran;
	}

	public Long getPt() {
		if (getSiswa() != null && getSiswa().getSekolah() != null
				&& getSiswa().getSekolah().getPerguruanTinggi() != null)
			pt = getSiswa().getSekolah().getPerguruanTinggi().getId();
		else if (getCalonSiswa() != null && getCalonSiswa().getSekolah() != null
				&& getCalonSiswa().getSekolah().getPerguruanTinggi() != null)
			pt = getCalonSiswa().getSekolah().getPerguruanTinggi().getId();
		else if (getMahasiswa() != null && getMahasiswa().getJurusan() != null
				&& getMahasiswa().getJurusan().getFakultas() != null
				&& getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null)
			pt = getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi().getId();
		else if (getBiodataCalonMahasiswa() != null && getBiodataCalonMahasiswa().getProdiLulus() != null
				&& getBiodataCalonMahasiswa().getProdiLulus().getFakultas() != null
				&& getBiodataCalonMahasiswa().getProdiLulus().getFakultas().getPerguruanTinggi() != null)
			pt = getBiodataCalonMahasiswa().getProdiLulus().getFakultas().getPerguruanTinggi().getId();
		else if (getBiodataCalonMahasiswa() != null && getBiodataCalonMahasiswa().getProdi1() != null
				&& getBiodataCalonMahasiswa().getProdi1().getFakultas() != null
				&& getBiodataCalonMahasiswa().getProdi1().getFakultas().getPerguruanTinggi() != null)
			pt = getBiodataCalonMahasiswa().getProdi1().getFakultas().getPerguruanTinggi().getId();
		return pt;
	}

	public void setPt(Long pt) {
		this.pt = pt;
	}

	@Column(columnDefinition = "text")
	public String getLink() {
		return link == null || link.isEmpty() ? "" : !link.startsWith("https") ? "https://" + link : link.trim();
	}

	public void setLink(String link) {
		this.link = bersihkanTextDb(link);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getKadaluarsaWaktu() {
		if (kadaluarsaWaktu == null)
			kadaluarsaWaktu = getKadaluarsa();
		return kadaluarsaWaktu;
	}

	public void setKadaluarsaWaktu(Date kadaluarsaWaktu) {
		this.kadaluarsaWaktu = kadaluarsaWaktu;
	}

	@Column(columnDefinition = "text")
	public String getNotif() {
		return notif;
	}

	public void setNotif(String notif) {
		this.notif = bersihkanTextDb(notif);
	}

	@Column(columnDefinition = "text")
	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getKadaluarsaBarcode() {
		return kadaluarsaBarcode;
	}

	public void setKadaluarsaBarcode(Date kadaluarsaBarcode) {
		this.kadaluarsaBarcode = kadaluarsaBarcode;
	}

	@Transient
	public String getHtmlTemporaryData() {
		return htmlTemporaryData;
	}

	public void setHtmlTemporaryData(String htmlTemporaryData) {
		this.htmlTemporaryData = htmlTemporaryData;
	}

	// Metode ini juga di-copy dari aslinya, karena sangat spesifik bisnis logic
	public static void populateCicilan(Session session, String cicilan, Kegiatan kegiatan, Long reqId,
			Date tanggalTransaksi, JenisPembayaran jenisPembayaran, String prefix) {
		Transaction tx = null;
		try {
			for (String idPemBul : StringUtils.split(cicilan, ",")) {
				if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
					PengaturanPembayaranBulanan ppBln = (PengaturanPembayaranBulanan) session
							.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();
					if (ppBln != null) {
						String ref = prefix + "-" + kegiatan.getId() + "-" + idPemBul + "-" + reqId;
						ItemBiaya itemBiaya = ppBln.getDetailBiaya().getItemBiaya();
						Double subtotal = 0.0;
						try {
							String[] spl = idPemBul.split("-");
							subtotal = Double.parseDouble(spl[spl.length - 1]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1910");
						}
						if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
							subtotal = 0.0 - subtotal;
						CicilanPembayaran cp = (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class)
								.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
						if (cp == null)
							cp = new CicilanPembayaran(ppBln.getDetailBiaya());
						cp.setRef(ref);
						cp.setValidator(ambilDefaultValidatorBank(prefix));
						cp.setKegiatan(kegiatan);
						cp.setItemBiaya(itemBiaya);
						cp.setPengaturanPembayaranBulanan(ppBln);
						cp.setRefVa(reqId);
						cp.setNilai(subtotal);
						cp.setNilaiAsli(cp.getNilai());
						cp.setTanggal(tanggalTransaksi == null ? WaktuUtil.getDate() : tanggalTransaksi);
						cp.setJenisPembayaran(jenisPembayaran);
						cp.setDenda(0.0);

						tx = session.beginTransaction();
						if (cp.getId() == null)
							session.save(cp);
						else
							Common.refreshUpdate(session, cp);
						tx.commit();
					}
				} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
					ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
							.simpleObject(
									session.createCriteria(ItemBiaya.class)
											.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
									ItemBiaya.class);
					if (itemBiaya != null) {
						String ref = prefix + "-" + kegiatan.getId() + "-" + idPemBul + "-" + reqId;
						Double subtotal = 0.0;
						Long detailBiayaId = null;
						try {
							String[] spl = idPemBul.split("-");
							subtotal = Double.parseDouble(spl[2]);
							detailBiayaId = Long.parseLong(spl[4]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1951");
						}
						if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
							subtotal = 0.0 - subtotal;
						CicilanPembayaran cp = (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class)
								.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
						if (cp == null)
							cp = new CicilanPembayaran(DetailBiaya.muatRefAman(session, detailBiayaId));
						cp.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
						cp.setRef(ref);
						cp.setValidator(ambilDefaultValidatorBank(prefix));
						cp.setKegiatan(kegiatan);
						cp.setItemBiaya(itemBiaya);
						cp.setPengaturanPembayaranBulanan(null);
						cp.setRefVa(reqId);
						cp.setNilai(subtotal);
						cp.setNilaiAsli(cp.getNilai());
						cp.setTanggal(tanggalTransaksi == null ? WaktuUtil.getDate() : tanggalTransaksi);
						cp.setJenisPembayaran(jenisPembayaran);
						cp.setDenda(0.0);

						tx = session.beginTransaction();
						if (cp.getId() == null)
							session.save(cp);
						else
							Common.refreshUpdate(session, cp);
						tx.commit();
					}
				}
			}
			sinkronkanKegiatanSetelahBayar(kegiatan);
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1985");
		}
	}

	public Double getAmount() {
		if (getNotif() != null && !getNotif().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				JSONObject jsonObject = new JSONObject(getNotif());
				JSONObject data = jsonObject.getJSONObject("data");
				if (data.getString("status").trim().equalsIgnoreCase("SUCCESS"))
					amount = data.getDouble("amount");
				else if (total != null && biayaAdmin != null)
					amount = total + total;
				else
					amount = 0.0;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2001");
			}
		} else if (getBank() != null && getBank().equalsIgnoreCase("flip") && getNotif() != null
				&& !getNotif().trim().isEmpty()) {
			try {
				amount = new JSONObject(getNotif()).getDouble("amount");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2007");
			}
		} else if (total != null && biayaAdmin != null)
			amount = biayaAdmin + total;
		return amount == null ? 0.0 : amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "va", nullable = true, unique = true)
	public Va getVa() {
		return va;
	}

	public void setVa(Va va) {
		this.va = va;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanal_pembayaran", nullable = true)
	public KanalPembayaran getKanalPembayaran() {
		if (getJenisKegiatan() != null && getJenisKegiatan().getKanalPembayaran() != null)
			kanalPembayaran = getJenisKegiatan().getKanalPembayaran();
		else
			kanalPembayaran = check(kanalPembayaran);
		return kanalPembayaran;
	}

	public void setKanalPembayaran(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi = check(anggotaKoperasi);
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		return caraPembayaranKoperasi = check(caraPembayaranKoperasi);
	}

	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	public Boolean getPakaiva() {
		return pakaiva == null ? true : pakaiva;
	}

	public void setPakaiva(Boolean pakaiva) {
		this.pakaiva = pakaiva;
	}

	public String getKelas() {
		try {
			if (getSiswa() != null) {
				KelasSiswa kelasSiswa = Siswa.ambilKelas(siswa, getTahunAkademik());
				kelas = kelasSiswa == null ? "-" : kelasSiswa.getNama();
			} else if (getMahasiswa() != null) {
				kelas = getMahasiswa().getKelas();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:2080");
		}
		return kelas;
	}

	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	public String getChannel() {
		if (getRequest() != null && !getRequest().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				JSONObject jsonObject = new JSONObject(getRequest());
				if (!jsonObject.isNull("channel"))
					channel = jsonObject.getJSONArray("channel").getString(0);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2096");
			}
		}
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Double getTabungan() {
		return tabungan == null ? 0.0 : tabungan;
	}

	public void setTabungan(Double tabungan) {
		this.tabungan = tabungan;
	}

	public Long getDeposit() {
		return deposit;
	}

	public void setDeposit(Long deposit) {
		this.deposit = deposit;
	}

	public Double getTopup() {
		return topup == null ? 0.0 : topup;
	}

	public void setTopup(Double topup) {
		this.topup = topup;
	}

	public static String curlSmartlinkGet(String linkPost, String username_va_e_smartlink,
			String password_va_e_smartlink) {
		String hasil = "";
		Process p = null;
		BufferedReader reader = null;
		BufferedReader errorReader = null;
		try {
			String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(username_va_e_smartlink,
					password_va_e_smartlink);
			if (Common.getKonfigurasi("curl_e_smartlink_via_server_lain", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF)) {
				String ipServerLain = Common.getKonfigurasi("curl_e_smartlink_via_IP", "38.47.178.46").getNilai()
						.trim();
				String portServerLain = Common.getKonfigurasi("curl_e_smartlink_via_PORT", "22031").getNilai().trim();
				String userServerLain = Common.getKonfigurasi("curl_e_smartlink_via_USER", "zishof").getNilai().trim();

				String remoteCurlCmd = "curl -s -k --location --request GET '" + linkPost + "' "
						+ "--header 'Content-Type: application/json' --header 'Authorization: Basic " + screet_key
						+ "'";
				String[] command = { "ssh", "-p", portServerLain, "-o", "StrictHostKeyChecking=no",
						userServerLain + "@" + ipServerLain, remoteCurlCmd };

				p = new ProcessBuilder(command).start();
			} else {
				String[] command = { "curl", "-s", "-k", "--location", "--request", "GET", linkPost, "--header",
						"Content-Type: application/json", "--header", "Authorization: Basic " + screet_key };
				p = new ProcessBuilder(command).start();
			}

			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				builder.append(line).append(System.getProperty("line.separator"));
			hasil = builder.toString();

			errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder errorBuilder = new StringBuilder();
			while ((line = errorReader.readLine()) != null)
				errorBuilder.append(line);

			if (errorBuilder.length() > 0 && hasil.trim().isEmpty()) {
				System.out.println("ERROR CURL: " + errorBuilder.toString());
				hasil = "{\"status\":\"error\",\"message\":\"Gagal koneksi CURL/SSH proxy\"}";
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:2176");
			hasil = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2182");
			}
			try {
				if (errorReader != null)
					errorReader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2187");
			}
			if (p != null)
				p.destroy();
		}
		return hasil;
	}

	public static String curlSmartlink(String strURL, String username_va_e_smartlink, String password_va_e_smartlink,
			JSONObject postData) {
		String hasil = "";
		Process p = null;
		BufferedReader reader = null;
		BufferedReader errorReader = null;
		try {
			String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(username_va_e_smartlink,
					password_va_e_smartlink);
			if (Common.getKonfigurasi("curl_e_smartlink_via_server_lain", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF)) {
				String ipServerLain = Common.getKonfigurasi("curl_e_smartlink_via_IP", "38.47.178.46").getNilai()
						.trim();
				String portServerLain = Common.getKonfigurasi("curl_e_smartlink_via_PORT", "22031").getNilai().trim();
				String userServerLain = Common.getKonfigurasi("curl_e_smartlink_via_USER", "zishof").getNilai().trim();

				String remoteCurlCmd = "curl -s -k -X POST '" + strURL + "' -H 'Content-Type: application/json' "
						+ "-H 'Accept: application/json' -H 'Authorization: Basic " + screet_key + "' --data-raw '"
						+ postData.toString() + "'";
				String[] command = { "ssh", "-p", portServerLain, "-o", "StrictHostKeyChecking=no",
						userServerLain + "@" + ipServerLain, remoteCurlCmd };
				p = new ProcessBuilder(command).start();
			} else {
				String[] command = { "curl", "-s", "-k", "-H", "Content-Type: application/json", "-H",
						"Accept: application/json", "-H", "Authorization: Basic " + screet_key, "-X", "POST", strURL,
						"--data-raw", postData.toString() };
				p = new ProcessBuilder(command).start();
			}

			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				builder.append(line).append(System.getProperty("line.separator"));
			hasil = builder.toString();

			errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder errorBuilder = new StringBuilder();
			while ((line = errorReader.readLine()) != null)
				errorBuilder.append(line);

			if (errorBuilder.length() > 0 && hasil.trim().isEmpty()) {
				System.out.println("ERROR CURL: " + errorBuilder.toString());
				hasil = "{\"status\":\"error\",\"message\":\"Gagal koneksi CURL/SSH proxy\"}";
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:2241");
			hasil = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2247");
			}
			try {
				if (errorReader != null)
					errorReader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2252");
			}
			if (p != null)
				p.destroy();
		}
		return hasil;
	}


	@Transient
	public boolean getTerbayarRingkas() {
		return getKegiatan() != null || getPembayaran() != null || getDeposit() != null || getWaktuBayar() != null;
	}

	@Transient
	public boolean getKadaluarsaRingkas() {
		Date kadaluarsa = getKadaluarsa();
		return kadaluarsa != null && kadaluarsa.before(WaktuUtil.getDate()) && !getTerbayarRingkas();
	}

	@Transient
	public String getStatusVirtualAccountRingkas() {
		if (getTerjadiKendala()) {
			return "Terjadi Kendala";
		}
		if (getTerbayarRingkas()) {
			return "Telah Bayar";
		}
		if (getKadaluarsaRingkas()) {
			return "Kadaluarsa";
		}
		return "Belum Bayar";
	}

	@Transient
	public String getNamaPemilikRingkas() {
		try {
			if (getMahasiswa() != null) {
				return (getMahasiswa().getNim() == null ? "" : getMahasiswa().getNim() + " - ")
						+ (getMahasiswa().getNama() == null ? "" : getMahasiswa().getNama());
			}
			if (getBiodataCalonMahasiswa() != null) {
				return (getBiodataCalonMahasiswa().getNoRegistrasi() == null ? ""
						: getBiodataCalonMahasiswa().getNoRegistrasi() + " - ")
						+ (getBiodataCalonMahasiswa().getNama() == null ? ""
								: getBiodataCalonMahasiswa().getNama());
			}
			if (getSiswa() != null) {
				return (getSiswa().getNomorInduk() == null ? "" : getSiswa().getNomorInduk() + " - ")
						+ (getSiswa().getNama() == null ? "" : getSiswa().getNama());
			}
			if (getCalonSiswa() != null) {
				return (getCalonSiswa().getNomorInduk() == null ? "" : getCalonSiswa().getNomorInduk() + " - ")
						+ (getCalonSiswa().getNama() == null ? "" : getCalonSiswa().getNama());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return getNama() == null ? "" : getNama();
	}

	@Transient
	public Double getTotalAman() {
		Double total = getTotal();
		return total == null ? 0.0 : total;
	}

}
