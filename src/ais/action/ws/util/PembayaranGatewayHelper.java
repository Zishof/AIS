package ais.action.ws.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.action.master.helper.PembayaranUtilHelper;
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
import ais.database.model.KegiatanTemporary;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;

/**
 * Helper BERSAMA pemrosesan rincian pembayaran Virtual Account untuk SEMUA servlet
 * payment gateway (BMS, BCA, Mandiri, BSI, Briva, Otto, OcbcNisp, Nagari, MncBank,
 * Maja, Jaring, Flip, Finpay, Esmartlink, Bjb, Bankaltimtara, PembayaranAction, Va, dll).
 *
 * Mengkonsolidasikan logika yang selama ini DI-COPY-PASTE di tiap servlet:
 *   - BLOK KERANJANG (idPemBul "Keranjang-") -> bentuk Kegiatan dari KegiatanTemporary,
 *     pindahkan CicilanPembayaran, hitung amount/terhutang/denda, updateVa.
 *   - BLOK CICILAN/ITEM ("Bulanan-", "Item-", numeric) + fallback parsing JSON request.
 * Semua konteks di-pass sebagai parameter (Session yang sama dengan penyimpanan) sehingga
 * tidak ada state tersembunyi. Memudahkan maintenance: perbaikan cukup di SATU tempat.
 */
public class PembayaranGatewayHelper {

	public static void prosesRincianVA(Session session, VirtualAccountBank virtualAccountBankNtt,
			boolean inquery, String bank, BankHost bankHost, Date tanggal, String data, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Integer semester, JenisKegiatan jenisKegiatan,
			JSONArray rincian) throws Exception {

		boolean keranjang = false;
		if (virtualAccountBankNtt.getCicilan() != null && !virtualAccountBankNtt.getCicilan().isEmpty()) {
			for (String idPemBul : org.apache.commons.lang.StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
				if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
					keranjang = true;
					break;
				}
			}
		}

		if (keranjang) {
			// ==========================================
			// BLOK 1: LOGIKA KERANJANG
			// ==========================================
			for (String idPemBul : org.apache.commons.lang.StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
				if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
					prosesSatuTokenKeranjang(session, idPemBul, virtualAccountBankNtt, inquery, bank, bankHost,
							tanggal, data, rincian);
				}
			}

		} else {
			// ==========================================
			// BLOK 2: BUKAN KERANJANG (CICILAN / JSON PARSING)
			// ==========================================

			// 1. Inisialisasi Kegiatan
			Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
					: session.createCriteria(Kegiatan.class).add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
							.uniqueResult());

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).addOrder(Order.asc("id"))
						.add(biodataCalonMahasiswa != null ? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
								: Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("jenisKegiatan", virtualAccountBankNtt.getJenisKegiatan()))
						.add(Restrictions.eq("semster", semester)).setMaxResults(1).uniqueResult();
			}

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = new Kegiatan();
			}

			kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
			kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
			kegiatan.setNama(mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama());
			kegiatan.setAmount(virtualAccountBankNtt.getTotal());
			kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
			kegiatan.setMahasiswa(mahasiswa);
			kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
			kegiatan.setSemster(semester);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setTanggal(tanggal);
			kegiatan.setValidated(1);
			kegiatan.setValidator(bank);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();

			// 2. Hitung Nilai Biaya Harus Dibayar
			List<Long> detailBiayasId = new ArrayList<Long>();
			for (String id : org.apache.commons.lang.StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
				try {
					detailBiayasId.add(Long.parseLong(id.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:124");
				}
			}

			Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
					.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("id", detailBiayasId))
					.list();

			Double nilaiBiayaHarusDiBayars = 0.0;
			for (DetailBiaya detailBiaya : detailBiayas) {
				nilaiBiayaHarusDiBayars += detailBiaya.hitungTotalKegiatan(kegiatan, session);
			}

			// 3. Proses Rincian dari Cicilan String ATAU Parse dari JSON Request (Jika
			// kosong)
			if (virtualAccountBankNtt.getCicilan() != null && !virtualAccountBankNtt.getCicilan().isEmpty()) {
				// -> A. PARSING DARI STRING CICILAN (LOGIKA LAMA)
				for (String idPemBul : org.apache.commons.lang.StringUtils.split(virtualAccountBankNtt.getCicilan(),
						",")) {

					if (Common.isNumber(idPemBul)) {
						PengaturanPembayaranBulanan pbb = (PengaturanPembayaranBulanan) session
								.createCriteria(PengaturanPembayaranBulanan.class)
								.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

						if (pbb != null) {
							String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
									+ virtualAccountBankNtt.getId();
							ItemBiaya itemBiaya = pbb.getDetailBiaya().getItemBiaya();

							Double subtotal = 0.0;
							try {
								String[] spl = idPemBul.split("-");
								subtotal = Double.parseDouble(spl[spl.length - 1]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:159");
							}

							if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								subtotal = 0.0 - subtotal;
							}

							if (!inquery) {
								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cicilanPembayaran == null)
									cicilanPembayaran = new CicilanPembayaran(pbb.getDetailBiaya());

								cicilanPembayaran.setRef(ref);
								cicilanPembayaran.setValidator(bank);
								cicilanPembayaran.setKegiatan(kegiatan);
								cicilanPembayaran.setItemBiaya(pbb.getDetailBiaya().getItemBiaya());
								cicilanPembayaran.setPengaturanPembayaranBulanan(pbb);
								cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
								cicilanPembayaran.setNilai(subtotal);
								cicilanPembayaran.setNilaiAsli(subtotal);
								cicilanPembayaran.setTanggal(tanggal);
								cicilanPembayaran.setJenisPembayaran(
										bankHost == null || bankHost.getJenisPembayaran() == null ? ConstantValues.TUNAI
												: bankHost.getJenisPembayaran());
								cicilanPembayaran.setDenda(0.0);

								session.getTransaction().begin();
								if (cicilanPembayaran.getId() == null)
									session.save(cicilanPembayaran);
								else
									Common.refreshUpdate(session, cicilanPembayaran);
								session.getTransaction().commit();

								// Update kolom "cicilans" di Kegiatan SAAT ITU JUGA, agar pembayaran
								// gateway (mis. Smartlink) langsung tampil tanpa perlu klik "Refresh".
								kegiatan.appendCicilan(cicilanPembayaran);
							}

							JSONObject jsonObjectRinci = new JSONObject();
							jsonObjectRinci.put("nama", pbb.getDetailBiaya().getItemBiaya().getNama());
							jsonObjectRinci.put("bulan", pbb.getNamaBulan());
							jsonObjectRinci.put("nominal", pbb.ambilNominalModifikasi(mahasiswa, semester));
							rincian.put(jsonObjectRinci);
						}

					} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
						PengaturanPembayaranBulanan pbb = (PengaturanPembayaranBulanan) session
								.createCriteria(PengaturanPembayaranBulanan.class)
								.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();

						if (pbb != null) {
							String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
									+ virtualAccountBankNtt.getId();
							ItemBiaya itemBiaya = pbb.getDetailBiaya().getItemBiaya();

							Double subtotal = 0.0;
							try {
								String[] spl = idPemBul.split("-");
								subtotal = Double.parseDouble(spl[spl.length - 1]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:221");
							}

							if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								subtotal = 0.0 - subtotal;
							}

							if (!inquery) {
								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cicilanPembayaran == null)
									cicilanPembayaran = new CicilanPembayaran(pbb.getDetailBiaya());

								cicilanPembayaran.setRef(ref);
								cicilanPembayaran.setValidator(bank);
								cicilanPembayaran.setKegiatan(kegiatan);
								cicilanPembayaran.setItemBiaya(itemBiaya);
								cicilanPembayaran.setPengaturanPembayaranBulanan(pbb);
								cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
								cicilanPembayaran.setNilai(subtotal);
								cicilanPembayaran.setNilaiAsli(subtotal);
								cicilanPembayaran.setTanggal(tanggal);
								cicilanPembayaran.setJenisPembayaran(
										bankHost == null || bankHost.getJenisPembayaran() == null ? ConstantValues.TUNAI
												: bankHost.getJenisPembayaran());
								cicilanPembayaran.setDenda(0.0);

								session.getTransaction().begin();
								if (cicilanPembayaran.getId() == null)
									session.save(cicilanPembayaran);
								else
									Common.refreshUpdate(session, cicilanPembayaran);
								session.getTransaction().commit();

								// Update kolom "cicilans" di Kegiatan SAAT ITU JUGA, agar pembayaran
								// gateway (mis. Smartlink) langsung tampil tanpa perlu klik "Refresh".
								kegiatan.appendCicilan(cicilanPembayaran);
							}

							JSONObject jsonObjectRinci = new JSONObject();
							jsonObjectRinci.put("nama", pbb.getDetailBiaya().getItemBiaya().getNama());
							jsonObjectRinci.put("bulan", pbb.getNamaBulan());
							jsonObjectRinci.put("nominal", subtotal);
							rincian.put(jsonObjectRinci);
						}

					} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
						ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
								.simpleObject(
										session.createCriteria(ItemBiaya.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
										ItemBiaya.class);

						if (itemBiaya != null) {
							String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
									+ virtualAccountBankNtt.getId();
							Double subtotal = 0.0;
							try {
								String[] spl = idPemBul.split("-");
								subtotal = Double.parseDouble(spl[2]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:283");
							}

							Long detailBiayaId = null;
							try {
								String[] spl = idPemBul.split("-");
								detailBiayaId = Long.parseLong(spl[4]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:290");
							}

							if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
								subtotal = 0.0 - subtotal;
							}

							if (!inquery) {
								CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cicilanPembayaran == null) {
									cicilanPembayaran = new CicilanPembayaran(
											DetailBiaya.muatRefAman(session, detailBiayaId));
								}
								cicilanPembayaran
										.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
								cicilanPembayaran.setRef(ref);
								cicilanPembayaran.setValidator(bank);
								cicilanPembayaran.setKegiatan(kegiatan);
								cicilanPembayaran.setItemBiaya(itemBiaya);
								cicilanPembayaran.setPengaturanPembayaranBulanan(null);
								cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
								cicilanPembayaran.setNilai(subtotal);
								cicilanPembayaran.setNilaiAsli(subtotal);
								cicilanPembayaran.setTanggal(tanggal);
								cicilanPembayaran.setJenisPembayaran(
										bankHost == null || bankHost.getJenisPembayaran() == null ? ConstantValues.TUNAI
												: bankHost.getJenisPembayaran());
								cicilanPembayaran.setDenda(0.0);

								session.getTransaction().begin();
								if (cicilanPembayaran.getId() == null)
									session.save(cicilanPembayaran);
								else
									Common.refreshUpdate(session, cicilanPembayaran);
								session.getTransaction().commit();

								// Update kolom "cicilans" di Kegiatan SAAT ITU JUGA, agar pembayaran
								// gateway (mis. Smartlink) langsung tampil tanpa perlu klik "Refresh".
								kegiatan.appendCicilan(cicilanPembayaran);
							}

							JSONObject jsonObjectRinci = new JSONObject();
							jsonObjectRinci.put("nama", itemBiaya.getNama());
							jsonObjectRinci.put("bulan", "");
							jsonObjectRinci.put("nominal", subtotal);
							rincian.put(jsonObjectRinci);
						}
					}
				}
			} else {
				// -> B. PARSING DARI FIELD REQUEST JSON (NEW LOGIC)
				String requestJsonStr = virtualAccountBankNtt.getRequest();
				if (requestJsonStr != null && !requestJsonStr.trim().isEmpty()) {
					try {
						JSONObject reqObj = new JSONObject(requestJsonStr);
						if (reqObj.has("item") && !reqObj.isNull("item")) {
							JSONArray itemsArray = reqObj.getJSONArray("item");

							for (int i = 0; i < itemsArray.length(); i++) {
								JSONObject itemObj = itemsArray.getJSONObject(i);
								String itemName = itemObj.getString("name");
								double subtotal = itemObj.getDouble("amount");

								// Mencari Item Biaya yang cocok berdasarkan nama
								ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
										.add(Restrictions.eq("aktif", true))
										.add(Restrictions.ilike("nama", itemName, MatchMode.EXACT)).setMaxResults(1)
										.uniqueResult();

								if (!inquery && itemBiaya != null) {
									String ref = "ntt-" + kegiatan.getId() + "-reqItem" + i + "-"
											+ virtualAccountBankNtt.getId();
									CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
											.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
											.setMaxResults(1).uniqueResult();

									if (cicilanPembayaran == null) {
										cicilanPembayaran = new CicilanPembayaran();
									}
									cicilanPembayaran.setRef(ref);
									cicilanPembayaran.setValidator(bank);
									cicilanPembayaran.setKegiatan(kegiatan);

									if (itemBiaya != null) {
										cicilanPembayaran.setItemBiaya(itemBiaya);
									}

									cicilanPembayaran.setPengaturanPembayaranBulanan(null);
									cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
									cicilanPembayaran.setNilai(subtotal);
									cicilanPembayaran.setNilaiAsli(subtotal);
									cicilanPembayaran.setTanggal(tanggal);
									cicilanPembayaran.setJenisPembayaran(
											bankHost == null || bankHost.getJenisPembayaran() == null
													? ConstantValues.TUNAI
													: bankHost.getJenisPembayaran());
									cicilanPembayaran.setDenda(0.0);

									session.getTransaction().begin();
									if (cicilanPembayaran.getId() == null)
										session.save(cicilanPembayaran);
									else
										Common.refreshUpdate(session, cicilanPembayaran);
									session.getTransaction().commit();

									// Update kolom "cicilans" di Kegiatan SAAT ITU JUGA, agar pembayaran
									// via gateway (mis. Smartlink) langsung tampil tanpa perlu klik
									// "Refresh" (yang melakukan hitung ulang). Hanya id cicilan yang baru
									// dibayar yang ditambahkan sebagai AKTIF; kegiatan disimpan di akhir
									// branch ini sehingga perubahan ter-persist.
									kegiatan.appendCicilan(cicilanPembayaran);
								}

								JSONObject jsonObjectRinci = new JSONObject();
								jsonObjectRinci.put("nama", itemName);
								jsonObjectRinci.put("bulan", "");
								jsonObjectRinci.put("nominal", subtotal);
								rincian.put(jsonObjectRinci);
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/ws/util/PembayaranGatewayHelper.java:414");
					}
				}
			}

			// 4. Update Kegiatan & Total
			if (!inquery) {
				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];

				kegiatan.setDenda(denda);
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah - denda));
				kegiatan.setAmount(jumlah > 0.1 ? jumlah : virtualAccountBankNtt.getTotal());
				kegiatan.setValidator(bank);

				session.getTransaction().begin();
				Common.refreshUpdate(session, kegiatan);
				session.getTransaction().commit();

				VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data, bank);
			}
		}
	}

	public static String ambilNamaItemCicilan(CicilanPembayaran cicilanPembayaran) {
		try {
			if (cicilanPembayaran.getDetailBiaya() != null
					&& cicilanPembayaran.getDetailBiaya().getItemBiaya() != null) {
				return cicilanPembayaran.getDetailBiaya().getItemBiaya().getNama();
			}
			if (cicilanPembayaran.getItemBiaya() != null) {
				return cicilanPembayaran.getItemBiaya().getNama();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return "Tagihan";
	}

	public static void rollbackSession(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:459");
		}
	}

	/**
	 * Apakah kolom {@code cicilan} sebuah VA mengandung token pembayaran Keranjang
	 * Belanja ({@code "Keranjang-<idKegiatanTemporary>-<nilai>"}). Dipakai servlet
	 * penerima pembayaran untuk memutuskan perlunya pemrosesan keranjang.
	 */
	public static boolean adaTokenKeranjang(VirtualAccountBank virtualAccountBank) {
		try {
			if (virtualAccountBank == null || virtualAccountBank.getCicilan() == null
					|| virtualAccountBank.getCicilan().isEmpty()) {
				return false;
			}
			for (String idPemBul : org.apache.commons.lang.StringUtils.split(virtualAccountBank.getCicilan(), ",")) {
				if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
					return true;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:479");
			/* toleran */
		}
		return false;
	}

	/**
	 * <h3>Pemroses SATU token pembayaran Keranjang Belanja — reusable untuk SEMUA servlet
	 * penerima pembayaran mahasiswa/calon mahasiswa.</h3>
	 *
	 * <p>
	 * Token berformat {@code "Keranjang-<idKegiatanTemporary>-<nilai>"} pada kolom
	 * {@code VirtualAccountBank.cicilan} (dibuat oleh
	 * {@code DownloadTagihanMahasiswaBankOnline.sendRequest} maupun wizard pembayaran
	 * multi-jenis). Untuk tiap token, method ini mengonversi draf {@link KegiatanTemporary}
	 * menjadi transaksi nyata — persis proses yang selama ini hanya hidup di jalur
	 * Esmartlink: cari/buat {@link Kegiatan} untuk (mahasiswa, jenisKegiatan, semester),
	 * salin atribut draf, pindahkan seluruh {@link CicilanPembayaran} anak draf ke Kegiatan
	 * nyata (ref unik + validator bank + jenis pembayaran dari {@link BankHost}), tautkan
	 * balik {@code kegiatanTemporary.setKegiatan(kegiatan)} sebagai penanda "sudah
	 * diproses", hitung ulang amount/amountTerhutang/denda, lalu {@code updateVa}.
	 * </p>
	 *
	 * <p>
	 * Aman dipanggil berulang (idempoten praktis): pemanggilan kedua atas token yang sama
	 * hanya menulis ulang nilai identik dan melewati penautan yang sudah terbentuk.
	 * Kegagalan satu token tidak melempar keluar — di-rollback + dilog agar token/proses
	 * lain tetap berjalan.
	 * </p>
	 *
	 * @param session  session Hibernate milik servlet pemanggil (transaksi dikelola method ini)
	 * @param idPemBul satu token {@code Keranjang-...} hasil split kolom cicilan
	 * @param inquery  true bila hanya cek tagihan (inquiry) — tidak ada perubahan data
	 * @param rincian  akumulator rincian item utk log/notifikasi; boleh null
	 * @return Kegiatan hasil konversi, atau null bila token tidak valid / inquiry / gagal
	 */
	@SuppressWarnings("unchecked")
	public static Kegiatan prosesSatuTokenKeranjang(Session session, String idPemBul,
			VirtualAccountBank virtualAccountBankNtt, boolean inquery, String bank, BankHost bankHost,
			Date tanggal, String data, JSONArray rincian) {

		Long idKeranjang = ambilIdKeranjang(idPemBul);
		if (idKeranjang == null) {
			return null;
		}
		KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) session.createCriteria(KegiatanTemporary.class)
				.add(Restrictions.idEq(idKeranjang)).uniqueResult();
		if (kegiatanTemporary == null) {
			return null;
		}

		Session sessionLocalKeg = session;
		try {
			if (inquery) {
				return null;
			}
			Mahasiswa mahasiswa = kegiatanTemporary.getMahasiswa();
			Integer smt = kegiatanTemporary.getSemster();
			JenisKegiatan jenisKegiatan = kegiatanTemporary.getJenisKegiatan();
			if (mahasiswa == null || smt == null || jenisKegiatan == null) {
				return null;
			}

			Kegiatan kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan);
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
			kegiatan.setTanggal(tanggal);
			kegiatan.setValidated(1);
			kegiatan.setValidator(bank);
			kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

			sessionLocalKeg.getTransaction().begin();
			Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
			sessionLocalKeg.getTransaction().commit();

			List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("kegiatanTemporary", kegiatanTemporary)).list();

			for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
				String ref = "ntt-keranjang-cicilanPembayaran-"
						+ (cicilanPembayaran.getId() == null ? "baru" : cicilanPembayaran.getId()) + "-" + idPemBul
						+ "-" + virtualAccountBankNtt.getId();

				cicilanPembayaran.setRef(ref);
				cicilanPembayaran.setKegiatan(kegiatan);
				cicilanPembayaran.setValidator(bank);
				cicilanPembayaran.setTanggal(tanggal);
				cicilanPembayaran.setJenisPembayaran(bankHost == null || bankHost.getJenisPembayaran() == null
						? ConstantValues.TUNAI
						: bankHost.getJenisPembayaran());

				sessionLocalKeg.getTransaction().begin();
				Common.refreshSaveOrUpdate(sessionLocalKeg, cicilanPembayaran);
				sessionLocalKeg.getTransaction().commit();

				// Pastikan cicilan keranjang ikut tercatat ke kolom "cicilans" Kegiatan
				// (auto saat bayar, tanpa perlu Refresh). Di-persist bersama kegiatan di bawah.
				kegiatan.appendCicilan(cicilanPembayaran);

				if (rincian != null) {
					JSONObject jsonObjectRinci = new JSONObject();
					jsonObjectRinci.put("nama", ambilNamaItemCicilan(cicilanPembayaran));
					jsonObjectRinci.put("nominal", cicilanPembayaran.getNilai());
					rincian.put(jsonObjectRinci);
				}
			}

			if (kegiatanTemporary.getKegiatan() == null || kegiatanTemporary.getKegiatan().getId() == null
					|| !kegiatanTemporary.getKegiatan().getId().equals(kegiatan.getId())) {
				sessionLocalKeg.getTransaction().begin();
				kegiatanTemporary.setKegiatan(kegiatan);
				Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatanTemporary);
				sessionLocalKeg.getTransaction().commit();
			}

			Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.isNotNull("itemBiaya")).add(Restrictions.eq("kegiatan", kegiatan))
					.setProjection(Projections.sum("nilai")).uniqueResult();

			Double nilaiBiayaHarusDiBayars = 0.0;
			Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();

			java.util.Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
			Collection<DetailBiaya> mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt,
					jenisKegiatan, false);
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

			Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(sessionLocalKeg, kegiatan);
			Double jumlahCicilan = d[0];
			Double denda = d[1];

			kegiatan.setDenda(denda);
			kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlahCicilan - denda));
			kegiatan.setAmount(jumlahCicilan > 0.1 ? jumlahCicilan : virtualAccountBankNtt.getTotal());
			kegiatan.setValidator(bank);

			sessionLocalKeg.getTransaction().begin();
			Common.refreshUpdate(sessionLocalKeg, kegiatan);
			sessionLocalKeg.getTransaction().commit();

			VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data, bank);
			return kegiatan;
		} catch (Exception e) {
			rollbackSession(sessionLocalKeg);
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	public static Long ambilIdKeranjang(String idPemBul) {
		if (idPemBul == null || !idPemBul.startsWith("Keranjang-")) {
			return null;
		}
		String[] parts = idPemBul.split("-");
		if (parts.length < 2) {
			return null;
		}
		try {
			return Long.valueOf(parts[1].trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	/**
	 * Simpan {@link LogHostToHost} dengan JAMINAN tinggi untuk SEMUA request bank.
	 *
	 * <p>Data log host-to-host adalah audit transaksi bank yang penting, jadi
	 * penyimpanannya TIDAK BOLEH bergantung pada keberhasilan transaksi bisnis
	 * sebelumnya maupun pada session caller (yang mungkin sudah rusak / ter-rollback).
	 * Karena itu method ini:</p>
	 * <ul>
	 * <li>memakai <b>session TERDEDIKASI</b> yang dibuka &amp; ditutup sendiri
	 * (independen dari session pemrosesan inquiry/pembayaran);</li>
	 * <li><b>commit sendiri</b> dan <b>mengulang sampai 3x</b> bila gagal;</li>
	 * <li><b>TIDAK PERNAH melempar exception</b> — kegagalan hanya dicatat ke
	 * konsol/log admin sehingga aman dipanggil dari blok {@code finally}.</li>
	 * </ul>
	 *
	 * @param log entitas yang akan disimpan (boleh sudah terisi sebagian saat error).
	 */
	public static void simpanLogHostToHost(LogHostToHost log) {
		if (log == null) {
			return;
		}
		final int maksPercobaan = 3;
		Throwable terakhir = null;
		for (int percobaan = 1; percobaan <= maksPercobaan; percobaan++) {
			Session session = null;
			try {
				// Selalu insert baru. Bila percobaan sebelumnya sudah men-set id (IDENTITY)
				// namun row gagal ter-commit, reset id agar insert berikutnya bersih.
				try {
					log.setId(null);
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:705");
				}
				session = HibernateUtil.openSession();
				session.beginTransaction();
				session.save(log);
				session.getTransaction().commit();
				return; // sukses tersimpan & ter-commit
			} catch (Throwable t) {
				terakhir = t;
				rollbackSession(session);
				System.err.println("[LogHostToHost] Gagal menyimpan log H2H (percobaan " + percobaan + "/"
						+ maksPercobaan + "): " + t);
				// Kegagalan transient (koneksi DB diputus admin/restart/failover, mis.
				// "terminating connection due to administrator command") TIDAK dieskalasi
				// ke audit per-percobaan agar log error tidak terbanjiri gangguan sesaat.
				// Beri jeda backoff agar koneksi pool tergantikan yang segar / DB selesai
				// restart sebelum percobaan berikutnya (sering sukses di percobaan ke-2/3).
				if (percobaan < maksPercobaan) {
					try {
						Thread.sleep(percobaan == 1 ? 600L : 1500L);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			} finally {
				tutupSessionAman(session);
			}
		}
		// Upaya terakhir: jejak isi log ke stderr agar tidak hilang total bila DB bermasalah.
		try {
			System.err.println("[LogHostToHost] GAGAL TOTAL menyimpan log H2H setelah " + maksPercobaan
					+ " percobaan. nim=" + log.getNim() + ", kode=" + log.getKode() + ", nominal=" + log.getNominal()
					+ ", response=" + log.getResponseDescription());
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:738");
		}
		// Eskalasi ke audit admin HANYA bila kegagalan BUKAN gangguan koneksi sesaat
		// (restart/failover/koneksi diputus admin/timeout/IO di luar kendali aplikasi →
		// cukup stderr). Kegagalan non-transient (mis. masalah data/constraint) tetap
		// dicatat agar tidak terlewat.
		try {
			if (terakhir != null && !kegagalanKoneksiTransient(terakhir)) {
				Common.tampilErrorJikaAdmin(terakhir instanceof Exception ? (Exception) terakhir
						: new Exception(terakhir));
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:749");
		}
	}

	/**
	 * Deteksi apakah {@link Throwable} berasal dari gangguan KONEKSI/ketersediaan DB
	 * yang bersifat SEMENTARA (restart, failover, koneksi diputus administrator,
	 * timeout, I/O), sehingga tidak perlu dieskalasi sebagai error aplikasi ke audit
	 * admin. Menelusuri seluruh rantai {@code cause} lalu memeriksa pesan dan SQLState
	 * (kelas 08xxx = connection exception; 57xxx = operator intervention, mis. 57P01
	 * "admin shutdown" / "terminating connection due to administrator command").
	 */
	private static boolean kegagalanKoneksiTransient(Throwable t) {
		Throwable c = t;
		int guard = 0;
		while (c != null && guard++ < 30) {
			String m = c.getMessage();
			if (m != null) {
				String s = m.toLowerCase();
				if (s.contains("terminating connection")
						|| s.contains("administrator command")
						|| s.contains("connection is closed")
						|| s.contains("connection has been closed")
						|| s.contains("connection reset")
						|| s.contains("connection refused")
						|| s.contains("connection attempt failed")
						|| s.contains("i/o error")
						|| s.contains("database system is")
						|| s.contains("broken pipe")
						|| s.contains("closed connection")
						|| s.contains("socket")) {
					return true;
				}
			}
			if (c instanceof java.sql.SQLException) {
				String st = ((java.sql.SQLException) c).getSQLState();
				if (st != null && (st.startsWith("08") || st.startsWith("57"))) {
					return true;
				}
			}
			c = c.getCause();
		}
		return false;
	}

	/**
	 * Ambil IP client dari request bank (urutan header proxy CF/X-Forwarded), dengan
	 * fallback ke remoteAddr lalu IP bankHost. Tidak pernah melempar exception.
	 */
	public static String ambilIpClient(HttpServletRequest request, BankHost bankHost) {
		try {
			if (request != null && request.getHeader("Cf-Connecting-Ip") != null) {
				return request.getHeader("Cf-Connecting-Ip");
			} else if (request != null && request.getHeader("CF-Connecting-IP") != null) {
				return request.getHeader("CF-Connecting-IP");
			} else if (request != null && request.getHeader("X-Forwarded-For") != null) {
				return request.getHeader("X-Forwarded-For");
			} else if (request != null && request.getHeader("X-Real-IP") != null) {
				return request.getHeader("X-Real-IP");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:809");
			// abaikan, pakai fallback
		}
		return request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : "");
	}

	/**
	 * Kumpulkan method HTTP + URI + SEMUA header request bank (nama & nilai, termasuk header
	 * berulang) jadi satu string siap-baca — supaya admin bisa memastikan PERSIS apa yang
	 * dikirim bank (Content-Type, User-Agent, header autentikasi/signature custom milik
	 * masing-masing bank, proxy chain X-Forwarded-For/CF-Connecting-IP, dst.) tanpa perlu
	 * membongkar log server mentah saat menganalisis error/sukses/pending. Tidak pernah
	 * melempar exception; mengembalikan string kosong bila request null/gagal dibaca.
	 *
	 * <p><b>Catatan keamanan:</b> sebagian bank mengirim token/signature di header — ini
	 * SAMA kelas sensitivitasnya dengan {@code request}/{@code response} body mentah yang
	 * SUDAH disimpan apa adanya di kolom lain pada entitas ini; tabel {@code log_host_to_host}
	 * memang tabel audit teknis internal (bukan tampilan pengguna akhir).</p>
	 */
	public static String ambilSemuaHeader(HttpServletRequest request) {
		if (request == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try {
			try {
				sb.append("Method: ").append(request.getMethod());
				sb.append("\nURI: ").append(request.getRequestURI());
				if (request.getQueryString() != null) {
					sb.append("?").append(request.getQueryString());
				}
				sb.append("\n");
			} catch (Exception eMeta) { ais.common.ErrorAuditUtil.record(eMeta, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:ambilSemuaHeaderMeta");
			}

			java.util.Enumeration<String> namaHeaders = request.getHeaderNames();
			while (namaHeaders != null && namaHeaders.hasMoreElements()) {
				String namaHeader = namaHeaders.nextElement();
				try {
					java.util.Enumeration<String> nilaiHeaders = request.getHeaders(namaHeader);
					StringBuilder nilaiGabungan = new StringBuilder();
					while (nilaiHeaders != null && nilaiHeaders.hasMoreElements()) {
						if (nilaiGabungan.length() > 0) {
							nilaiGabungan.append("; ");
						}
						nilaiGabungan.append(nilaiHeaders.nextElement());
					}
					sb.append(namaHeader).append(": ").append(nilaiGabungan).append("\n");
				} catch (Exception eSatu) { ais.common.ErrorAuditUtil.record(eSatu, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:ambilSemuaHeaderSatu");
				}
			}
		} catch (Exception e) {
			return sb.toString().trim();
		}
		return sb.toString().trim();
	}

	/**
	 * Catat satu {@link LogHostToHost} untuk request bank apa pun (pola seragam) lalu simpan
	 * dengan JAMINAN via {@link #simpanLogHostToHost(LogHostToHost)}. Cocok dipanggil dari blok
	 * {@code finally} tiap servlet bank dengan SATU baris. Tidak pernah melempar exception.
	 *
	 * <p>Kompatibilitas: overload lama tanpa {@code request}/{@code response}/
	 * {@code responseCode}/{@code transactionType} — delegasi ke overload lengkap dengan
	 * nilai null pada parameter tambahan tsb. Panggilan LAMA tetap otomatis mendapat
	 * "Saran Tindak Lanjut" (lihat overload lengkap) walau tidak diubah sama sekali.
	 *
	 * @param stackTrace jejak error saat inquiry/pembayaran (boleh null bila tidak ada error).
	 */
	public static void catatLogHostToHost(HttpServletRequest request, BankHost bankHost, String keterangan,
			String nim, String kode, String nama, String responseDescription, Double nominal, String item,
			String stackTrace) {
		catatLogHostToHost(request, bankHost, keterangan, nim, kode, nama, responseDescription, nominal, item,
				stackTrace, null, null, null, null);
	}

	/**
	 * Potong {@code nilai} secara aman ke {@code maxLength} karakter SEBELUM disimpan ke
	 * kolom DB (varchar terbatas) sehingga terhindar dari "value too long for type
	 * character varying(N)". Bila memang perlu dipotong, versi PENUH tetap dicatat ke
	 * stderr/log aplikasi lebih dulu -- supaya informasi debug (mis. header IP/proxy
	 * chain lengkap) tidak hilang total hanya karena kolom log_host_to_host membatasi
	 * panjangnya. Tidak pernah melempar exception.
	 *
	 * @param nilai      nilai mentah yang akan disimpan (boleh null).
	 * @param maxLength  panjang maksimum kolom tujuan.
	 * @param namaField  nama field/kolom (untuk keperluan jejak log saja).
	 */
	private static String potongAmanUntukKolomLog(String nilai, int maxLength, String namaField) {
		if (nilai == null || nilai.length() <= maxLength) {
			return nilai;
		}
		try {
			System.err.println("[LogHostToHost] Nilai field '" + namaField + "' dipotong dari " + nilai.length()
					+ " ke " + maxLength + " karakter sebelum disimpan ke DB (kolom varchar terbatas)."
					+ " Nilai PENUH (untuk debug): " + nilai);
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:potongAmanUntukKolomLog"); }
		return nilai.substring(0, maxLength);
	}

	/**
	 * Varian LENGKAP {@link #catatLogHostToHost(HttpServletRequest, BankHost, String, String,
	 * String, String, String, Double, String, String)} — menyertakan payload {@code request}/
	 * {@code response} MENTAH dan {@code responseCode}/{@code transactionType} ke kolom
	 * masing-masing (bukan cuma dikutip sepotong-sepotong di {@code keterangan}), plus
	 * MENYUSUN OTOMATIS "Saran Tindak Lanjut" (ditambahkan ke {@code responseDescription})
	 * berdasarkan sinyal yang tersedia — error (ada stackTrace), sukses nominal wajar,
	 * sukses TAPI nominal 0 (kasus umum: bank tak kirim kode jenis pembayaran → sistem
	 * cek kategori tagihan default, bukan tagihan aktual — lihat memory
	 * bankaltimtara-cekulang-tombol-hilang-bank-bms), pending, atau tak dapat disimpulkan.
	 * Tujuannya: admin yang membaca log ini LANGSUNG tahu apa yang terjadi dan apa
	 * langkah berikutnya, tanpa harus membongkar kode/log server.
	 *
	 * @param stackTrace      jejak error saat inquiry/pembayaran (boleh null bila tidak ada error).
	 * @param request         payload request MENTAH dari bank (JSON/body apa adanya, boleh null).
	 * @param response        payload response MENTAH yang dikirim balik ke bank (boleh null).
	 * @param responseCode    kode status transaksi (mis. "00"=sukses; konvensi tiap bank beda,
	 *                        boleh null bila tidak relevan/tidak tersedia).
	 * @param transactionType kode jenis transaksi H2H (inquiry/payment/reversal, dsb; boleh null).
	 */
	public static void catatLogHostToHost(HttpServletRequest request, BankHost bankHost, String keterangan,
			String nim, String kode, String nama, String responseDescription, Double nominal, String item,
			String stackTrace, String requestMentah, String responseMentah, String responseCode,
			Integer transactionType) {
		try {
			LogHostToHost log = new LogHostToHost();
			log.setKeterangan(keterangan);
			// "ip", "nim", "kode" TIDAK diberi anotasi @Column(length=...) pada entitas
			// LogHostToHost -> Hibernate memetakannya ke kolom varchar dengan panjang
			// Kolom "ip" kini bertipe text (lihat LogHostToHost.getIp() +
			// DatabaseTextColumnSchemaFix.initLogHostToHost()) -- tidak lagi wajib dipotong ke 255
			// demi muat di kolom, tapi tetap dibatasi ke angka besar sebagai jaga-jaga terhadap
			// header proxy-chain yang dimanipulasi/tak wajar panjangnya.
			log.setIp(potongAmanUntukKolomLog(ambilIpClient(request, bankHost), 4000, "ip"));
			log.setBankHost(bankHost);
			log.setNim(potongAmanUntukKolomLog(nim, 255, "nim"));
			log.setKode(potongAmanUntukKolomLog(kode, 255, "kode"));
			log.setNama(nama);
			log.setTanggal(ais.ui.util.WaktuUtil.getDate());
			log.setResponseDescription(saranTindakLanjut(responseDescription, nominal, stackTrace, responseCode));
			log.setNominal(nominal);
			log.setItem(item);
			log.setStackTrace(stackTrace);
			log.setRequest(requestMentah);
			log.setResponse(responseMentah);
			log.setResponseCode(responseCode);
			log.setTransactionType(transactionType);
			// SEMUA header HTTP (method+URI+query+header lengkap) dari request bank —
			// permintaan eksplisit: catat sedetail-detailnya agar admin bisa memastikan
			// PERSIS apa yang dikirim bank saat menganalisis error/sukses/pending.
			log.setInfo0("=== HTTP REQUEST (method/URI/header lengkap dari bank) ===\n"
					+ ambilSemuaHeader(request));
			simpanLogHostToHost(log);
		} catch (Throwable t) {
			try {
				Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:842");
			}
		}
	}

	/**
	 * Susun "Saran Tindak Lanjut" berdasarkan sinyal yang tersedia (bukan status eksplisit,
	 * karena tiap bank punya konvensi responseDescription/kode beda-beda) — ditambahkan
	 * di BELAKANG {@code responseDescription} asli (tidak menimpa/menghilangkan informasi
	 * asli dari bank), dipisah baris baru, sehingga admin membaca APA yang terjadi (dari
	 * bank) lalu APA yang perlu dilakukan (dari sini) dalam satu tempat.
	 */
	private static String saranTindakLanjut(String responseDescription, Double nominal, String stackTrace,
			String responseCode) {
		String d = responseDescription == null ? "" : responseDescription.toLowerCase();
		// "00" adalah konvensi UMUM banyak bank utk sukses, tapi TIDAK universal — dipakai
		// sbg sinyal TAMBAHAN saja, bukan satu-satunya penentu (deskripsi teks tetap primer).
		boolean kodeSukses = "00".equals(responseCode == null ? null : responseCode.trim());
		String saran;

		if (stackTrace != null && !stackTrace.trim().isEmpty()) {
			saran = "SARAN: Terjadi error saat memproses transaksi ini (lihat jejak error/Stack Trace). "
					+ "Langkah: (1) periksa Stack Trace utk penyebab teknis; (2) bila error koneksi/timeout ke "
					+ "gateway bank, gunakan tombol \"Cek Ulang\" di menu Virtual Account utk memverifikasi "
					+ "ulang langsung ke bank; (3) bila error data/logika (NPE, dsb), sampaikan ke tim developer "
					+ "beserta ID log ini.";
		} else if (nominal != null && nominal.intValue() == 0
				&& (kodeSukses || d.contains("sukses") || d.contains("success"))) {
			saran = "SARAN: Transaksi sukses SECARA TEKNIS namun nominal tercatat Rp0 — pola ini biasanya "
					+ "terjadi karena permintaan bank tidak menyertakan kode jenis pembayaran spesifik, sehingga "
					+ "sistem memeriksa kategori tagihan DEFAULT, bukan tagihan aktual mahasiswa. Langkah: "
					+ "(1) pastikan mahasiswa memang tidak salah dianggap tidak punya tagihan; (2) periksa "
					+ "parameter kode jenis pembayaran yang dikirim bank pada request ini; (3) gunakan tombol "
					+ "\"Cek Ulang\" di menu Virtual Account bila transaksi ini terkait VA terdaftar.";
		} else if (d.contains("pending") || d.contains("diproses") || d.contains("menunggu")
				|| d.contains("tunggu")) {
			saran = "SARAN: Status transaksi PENDING/menunggu. Langkah: (1) tunggu notifikasi callback "
					+ "berikutnya dari bank (biasanya beberapa menit); (2) bila setelah beberapa saat masih "
					+ "pending, gunakan tombol \"Cek Ulang\" di menu Virtual Account utk memeriksa status "
					+ "terbaru langsung ke gateway; (3) jangan proses ulang manual sebelum status final.";
		} else if (kodeSukses || d.contains("sukses") || d.contains("success")
				|| (nominal != null && nominal.intValue() > 0)) {
			saran = "SARAN: Transaksi tercatat berhasil. Bila mahasiswa melapor status di SIAKAD belum "
					+ "ter-update, gunakan tombol \"Cek Ulang\" di menu Virtual Account utk sinkronisasi ulang, "
					+ "dan pastikan CicilanPembayaran/DetailKegiatan terkait sudah terposting.";
		} else {
			saran = "SARAN: Status transaksi tidak dapat disimpulkan otomatis dari respons ini. Langkah: "
					+ "(1) periksa manual field Keterangan/Request/Response pada log ini; (2) gunakan tombol "
					+ "\"Cek Ulang\" di menu Virtual Account bila transaksi ini terkait VA terdaftar; (3) bila "
					+ "masih meragukan, hubungi Administrator sistem.";
		}

		return (responseDescription == null || responseDescription.trim().isEmpty() ? "" : responseDescription.trim() + "\n\n")
				+ saran;
	}

	/** Tutup session terdedikasi dengan aman (disconnect + close), menelan semua error. */
	private static void tutupSessionAman(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.disconnect();
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:856");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/ws/util/PembayaranGatewayHelper.java:862");
		}
	}

	/**
	 * Ubah {@link Throwable} menjadi String stack trace lengkap untuk disimpan ke kolom
	 * text {@code LogHostToHost.stackTrace}. Aman dipanggil dengan null (mengembalikan null).
	 */
	public static String ambilStackTrace(Throwable t) {
		if (t == null) {
			return null;
		}
		try {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			t.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		} catch (Throwable x) {
			return String.valueOf(t);
		}
	}
}
