package ais.action.servlet.api;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikItemProfile;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.SatuanItem;

/**
 * <h3>Provisioning MINIMAL modul SIRS untuk UAT apotik (aksi {@code apotik_provision_demo}).</h3>
 *
 * <p>Latar: server eCampus akademik (mis. demo.ecampus.id) tidak mem-provisioning modul SIRS,
 * sehingga {@code sirs.kode_transaksi_medis} kosong ({@code ConstantValues.apotikJual == null}) dan
 * {@code sirs.item_medis} kosong -- seluruh transaksi apotik fail-closed dengan benar, tapi jalur-
 * positif (jual, tolak-kedaluwarsa, FEFO, narkotika) tak bisa diuji. Aksi ini menyiapkan data uji
 * MINIMAL supaya UAT bisa jalan.</p>
 *
 * <h3>Pengaman (agar TIDAK pernah jalan tak sengaja di server rumah sakit nyata)</h3>
 * <ul>
 *   <li>WAJIB admin sistem ({@code pedagang == null});</li>
 *   <li>WAJIB token konfirmasi eksplisit {@code konfirmasi == "SEED-DEMO-APOTIK"};</li>
 *   <li>Pembuatan data uji (item/resep) HANYA bila {@code sirs.item_medis} masih KOSONG --
 *       server ber-SIRS nyata (sudah ada item) TIDAK akan disentuh datanya.</li>
 * </ul>
 *
 * <p>Idempoten: kode transaksi &amp; master di-<i>ensure</i> by kode/nama; dipanggil dua kali tidak
 * menggandakan. Tanda ({@code jenis}) kode transaksi mengikuti semantik stok yang tidak ambigu
 * (jual/keluar = -1, beli/masuk = +1) -- rumus stok {@code SUM((qty+qty_bonus)*jenis)}.</p>
 */
public final class ApotikDemoProvisionHelper {

	private ApotikDemoProvisionHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/** Ensure satu KodeTransaksiMedis by kode; buat dgn nama+jenis bila belum ada. Set juga
	 *  ConstantValues LIVE supaya tidak perlu restart server. */
	private static KodeTransaksiMedis ensureKode(Session session, String kode, String nama, int jenis) {
		KodeTransaksiMedis k = (KodeTransaksiMedis) session.createCriteria(KodeTransaksiMedis.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		if (k == null) {
			k = new KodeTransaksiMedis();
			k.setKode(kode);
			k.setNama(nama);
			k.setJenis(Integer.valueOf(jenis));
			session.save(k);
		}
		return k;
	}

	public static void provisionDemo(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null || tbmuser.getPedagang() != null) {
			tolak(hasil, "Hanya admin sistem yang boleh menjalankan provisioning demo apotik.");
			return;
		}
		String konfirmasi = request == null ? "" : request.optString("konfirmasi", "").trim();
		if (!"SEED-DEMO-APOTIK".equals(konfirmasi)) {
			tolak(hasil, "Konfirmasi wajib: kirim {\"konfirmasi\":\"SEED-DEMO-APOTIK\"}. "
					+ "Aksi ini HANYA untuk server demo/UAT tanpa modul SIRS -- jangan dipakai di server rumah sakit nyata.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			JSONObject ringkas = new JSONObject();

			// 1) Kode transaksi + ConstantValues live (jenis: masuk +1 / keluar -1).
			KodeTransaksiMedis aj = ensureKode(session, "AJ", "Apotik Jual", -1);
			KodeTransaksiMedis bm = ensureKode(session, "BM", "Beli Masuk", 1);
			KodeTransaksiMedis adt = ensureKode(session, "ADT", "Adjustment Penambahan", 1);
			KodeTransaksiMedis adk = ensureKode(session, "ADK", "Adjustment Pengurangan", -1);
			KodeTransaksiMedis ar = ensureKode(session, "AR", "Apotik Retur", 1);
			KodeTransaksiMedis br = ensureKode(session, "BR", "Beli Retur", -1);
			ensureKode(session, "RAC", "Jasa Racik", 1);
			ConstantValues.apotikJual = aj;
			ConstantValues.beliMasuk = bm;
			ConstantValues.adjustmentPenambahan = adt;
			ConstantValues.adjustmentPengurangan = adk;
			ConstantValues.apotikRetur = ar;
			ConstantValues.beliRetur = br;
			ringkas.put("kodeTransaksi", "AJ/BM/ADT/ADK/AR/BR/RAC dipastikan + ConstantValues di-set live");

			// 2) Data uji item/resep -- HANYA bila item_medis masih kosong (server nyata dilewati).
			long jumlahItem = ((Number) session.createQuery("select count(i) from ItemMedis i")
					.uniqueResult()).longValue();
			if (jumlahItem > 0) {
				ringkas.put("dataUji", "DILEWATI -- sirs.item_medis sudah berisi " + jumlahItem
						+ " item (server ber-SIRS nyata, tidak disentuh)");
				tx.commit();
				hasil.put("status", "00");
				hasil.put("ringkasan", ringkas);
				return;
			}

			// Master pendukung (satuan + jenis item), idempoten by nama/kode.
			SatuanItem satuan = (SatuanItem) session.createCriteria(SatuanItem.class)
					.add(Restrictions.eq("nama", "Tablet").ignoreCase()).setMaxResults(1).uniqueResult();
			if (satuan == null) {
				satuan = new SatuanItem();
				satuan.setNama("Tablet");
				satuan.setNamaAwal("Tab");
				satuan.setJumlah(Integer.valueOf(1));
				session.save(satuan);
			}
			JenisItemMedis jenis = (JenisItemMedis) session.createCriteria(JenisItemMedis.class)
					.add(Restrictions.eq("kode", "OBT").ignoreCase()).setMaxResults(1).uniqueResult();
			if (jenis == null) {
				jenis = new JenisItemMedis();
				jenis.setKode("OBT");
				jenis.setNama("Obat");
				session.save(jenis);
			}

			// Dua ItemMedis uji: satu LASA (bebas), satu terkendali (narkotika).
			ItemMedis obatA = buatItem(session, "UJI-PCT", "Paracetamol 500mg (UJI)", satuan, jenis, 3000, 1500);
			ItemMedis obatB = buatItem(session, "UJI-CDN", "Codein 10mg (UJI, Narkotika)", satuan, jenis, 8000, 4000);

			// Profil apotik: A = LASA/bebas, B = narkotika (agar apotik_bayar wajib register).
			ensureProfil(session, obatA, ApotikItemProfile.GOLONGAN_BEBAS, true);
			ensureProfil(session, obatB, ApotikItemProfile.GOLONGAN_NARKOTIKA, false);

			// Satu resep uji berisi obat A (agar tebus-resep bisa diuji).
			Resep resep = (Resep) session.createCriteria(Resep.class)
					.add(Restrictions.eq("kode", "RSP-UJI-1")).setMaxResults(1).uniqueResult();
			if (resep == null) {
				resep = new Resep();
				resep.setKode("RSP-UJI-1");
				resep.setKeterangan("Resep uji UAT apotik");
				session.save(resep);
				ResepDetail rd = new ResepDetail();
				rd.setResep(resep);
				rd.setItem(obatA);
				rd.setJumlah(Double.valueOf(10));
				rd.setTanggal(new Date());
				session.save(rd);
			}

			tx.commit();
			JSONArray items = new JSONArray();
			items.put(new JSONObject().put("itemId", obatA.getId()).put("kode", "UJI-PCT")
					.put("golongan", "BEBAS").put("lasa", true));
			items.put(new JSONObject().put("itemId", obatB.getId()).put("kode", "UJI-CDN")
					.put("golongan", "NARKOTIKA"));
			ringkas.put("dataUji", "dibuat");
			ringkas.put("items", items);
			ringkas.put("resepId", resep.getId());
			ringkas.put("catatan", "Stok/batch belum ada -- gunakan apotik_terima_barang (ED lampau & depan) "
					+ "untuk membuat batch kedaluwarsa & valid, lalu uji apotik_bayar.");
			hasil.put("status", "00");
			hasil.put("ringkasan", ringkas);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static ItemMedis buatItem(Session session, String kode, String nama, SatuanItem satuan,
			JenisItemMedis jenis, double hargaJual, double hargaBeli) {
		ItemMedis it = (ItemMedis) session.createCriteria(ItemMedis.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		if (it == null) {
			it = new ItemMedis();
			it.setKode(kode);
			it.setNama(nama);
			it.setSatuanItem(satuan);
			it.setJenisItem(jenis);
			it.setDefaultHargaJual(Double.valueOf(hargaJual));
			it.setDefaultHargaBeli(Double.valueOf(hargaBeli));
			session.save(it);
		}
		return it;
	}

	private static void ensureProfil(Session session, ItemMedis item, String golongan, boolean lasa) {
		ApotikItemProfile p = (ApotikItemProfile) session.createCriteria(ApotikItemProfile.class)
				.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
		if (p == null) {
			p = new ApotikItemProfile();
			p.setItem(item);
		}
		p.setGolonganObat(golongan);
		p.setLasa(Boolean.valueOf(lasa));
		session.saveOrUpdate(p);
	}
}
