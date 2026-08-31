package ais.action.master.sirs.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.TarifKhususPunyaItem;
import ais.database.model.sirs.ItemMedis;

/**
 * Utilitas resolusi harga jual dan komponen biaya untuk item medis SIRS (rumah sakit), termasuk
 * dukungan tarif khusus (kontrak dokter/asuransi/komunitas/pasien tertentu, lihat
 * {@link CommonTarif#getTarif}). Bila belum ada {@link HargaJualItem} untuk kombinasi item/kelas
 * perawatan/tarif khusus yang diminta, method-method di sini akan membuatnya secara otomatis
 * (lazy-create) — untuk tarif khusus, harga awal disalin dari harga jual reguler item tersebut.
 */
public class CommonTarifItem {

	/** Seperti {@link #getHargaJualItem(ItemMedis, KelasPerawatan, TarifKhususPunyaItem)}, tanpa tarif khusus (harga jual reguler). */
	public static HargaJualItem getHargaJualItem(ItemMedis item, KelasPerawatan kelasPerawatan) {
		TarifKhususPunyaItem tarifKhususPunyaItem = null;
		return getHargaJualItem(item, kelasPerawatan, tarifKhususPunyaItem);
	}

	/** Menentukan {@link TarifKhususPunyaItem} yang berlaku (via {@link #getTarifKhususPunyaItem}) untuk kombinasi dokter/asuransi/komunitas/pasien, lalu mengambil {@link HargaJualItem} sesuai tarif tersebut. */
	public static HargaJualItem getHargaJualItem(ItemMedis item, KelasPerawatan kelasPerawatan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) {
		TarifKhususPunyaItem tarifKhususPunyaItem = getTarifKhususPunyaItem(item, dokter, asuransi, komunitas, pasien);
		return getHargaJualItem(item, kelasPerawatan, tarifKhususPunyaItem);
	}

	/** Seperti {@link #getHargaJualItem(ItemMedis, KelasPerawatan, TarifKhususPunyaItem)}, mengambil item dari {@code tarifKhususPunyaItem} itu sendiri. */
	public static HargaJualItem getHargaJualItem(TarifKhususPunyaItem tarifKhususPunyaItem,
			KelasPerawatan kelasPerawatan) {
		return getHargaJualItem(tarifKhususPunyaItem.getItem(), kelasPerawatan, tarifKhususPunyaItem);
	}

	/** Mencari {@link TarifKhususPunyaItem} yang berlaku untuk {@code item} pada kombinasi dokter/asuransi/komunitas/pasien tertentu, lewat resolusi tarif umum {@link CommonTarif#getTarif}. */
	public static TarifKhususPunyaItem getTarifKhususPunyaItem(ItemMedis item, Dokter dokter, Asuransi asuransi,
			Set<Komunitas> komunitas, Pasien pasien) {
		return (TarifKhususPunyaItem) CommonTarif.getTarif(TarifKhususPunyaItem.class, Restrictions.eq("item", item),
				dokter, asuransi, komunitas, pasien);
	}

	/**
	 * Mengambil {@link HargaJualItem} untuk kombinasi {@code item}/{@code kelasPerawatan}/
	 * {@code tarifKhususPunyaItem}. Bila {@code tarifKhususPunyaItem} diisi dan belum ada baris
	 * harga untuknya, satu baris baru dibuat dengan harga awal disalin dari harga jual reguler
	 * item tersebut pada kelas perawatan yang sama (atau 0 bila belum ada harga reguler). Bila
	 * {@code tarifKhususPunyaItem} {@code null} dan belum ada baris harga reguler, satu baris baru
	 * dibuat dengan harga awal 0. Baris baru langsung dipersist ke sesi Hibernate saat ini.
	 */
	public static HargaJualItem getHargaJualItem(ItemMedis item, KelasPerawatan kelasPerawatan,
			TarifKhususPunyaItem tarifKhususPunyaItem) {

		Session session = HibernateUtil.currentSession();

		HargaJualItem b;
		if (tarifKhususPunyaItem != null) {
			b = (HargaJualItem) session.createCriteria(HargaJualItem.class).add(Restrictions.isNull("item"))
					.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.eq("tarifKhususPunyaItem", tarifKhususPunyaItem)).setMaxResults(1).uniqueResult();
			if (b == null) {

				Number biaya = (Number) session.createCriteria(HargaJualItem.class)
						.setProjection(Projections.property("biaya")).add(Restrictions.eq("item", item))
						.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.add(Restrictions.isNull("tarifKhususPunyaItem")).setMaxResults(1).uniqueResult();

				b = new HargaJualItem();
				b.setHargaJual(biaya == null ? 0.0 : biaya.doubleValue());
				b.setTarifKhususPunyaItem(tarifKhususPunyaItem);
				b.setKelasPerawatan(kelasPerawatan);
				b.setItem(null);
				session.save(b);
			}
		} else {
			b = (HargaJualItem) session.createCriteria(HargaJualItem.class).add(Restrictions.eq("item", item))
					.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.isNull("tarifKhususPunyaItem")).setMaxResults(1).uniqueResult();
			if (b == null) {
				b = new HargaJualItem();
				b.setHargaJual(0.0);
				b.setTarifKhususPunyaItem(null);
				b.setKelasPerawatan(kelasPerawatan);
				b.setItem(item);
				session.save(b);
			}
		}

		System.out.println("hargaJualItem = " + b);

		return b;
	}

	/** Seperti {@link #getHargaJualItem(ItemMedis, KelasPerawatan, TarifKhususPunyaItem)}, memakai {@code session} yang diberikan pemanggil dan membungkus penyimpanan baris baru dalam transaksi eksplisit sendiri. */
	public static HargaJualItem getHargaJualItem(ItemMedis item, KelasPerawatan kelasPerawatan,
			TarifKhususPunyaItem tarifKhususPunyaItem, Session session) {
		HargaJualItem b;
		if (tarifKhususPunyaItem != null) {
			b = (HargaJualItem) session.createCriteria(HargaJualItem.class).add(Restrictions.isNull("item"))
					.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.eq("tarifKhususPunyaItem", tarifKhususPunyaItem)).setMaxResults(1).uniqueResult();
			if (b == null) {

				Number biaya = (Number) session.createCriteria(HargaJualItem.class)
						.setProjection(Projections.property("biaya")).add(Restrictions.eq("item", item))
						.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
						.add(Restrictions.isNull("tarifKhususPunyaItem")).setMaxResults(1).uniqueResult();

				b = new HargaJualItem();
				b.setHargaJual(biaya == null ? 0.0 : biaya.doubleValue());
				b.setTarifKhususPunyaItem(tarifKhususPunyaItem);
				b.setKelasPerawatan(kelasPerawatan);
				b.setItem(null);
				session.getTransaction().begin();
				session.save(b);
				session.getTransaction().commit();
			}
		} else {
			b = (HargaJualItem) session.createCriteria(HargaJualItem.class).add(Restrictions.eq("item", item))
					.add(Restrictions.eq("kelasPerawatan", kelasPerawatan))
					.add(Restrictions.isNull("tarifKhususPunyaItem")).setMaxResults(1).uniqueResult();
			if (b == null) {
				b = new HargaJualItem();
				b.setHargaJual(0.0);
				b.setTarifKhususPunyaItem(null);
				b.setKelasPerawatan(kelasPerawatan);
				b.setItem(item);
				session.getTransaction().begin();
				session.save(b);
				session.getTransaction().commit();
			}
		}
		return b;
	}

	/**
	 * Mengambil daftar {@link JenisBiaya} (bertipe {@link JenisBiaya#TIPE_ITEM}) yang benar-benar
	 * dipakai pada baris {@link Biaya} master (belum terpakai di transaksi layanan/detail
	 * transaksi apa pun) untuk kombinasi item/tarif khusus yang diberikan. Bila tidak ditemukan
	 * jenis biaya spesifik (item belum punya baris biaya sama sekali), method jatuh kembali ke
	 * seluruh jenis biaya default aktif bertipe item, terurut nama.
	 */
	@SuppressWarnings("unchecked")
	public static List<JenisBiaya> getJenisBiayas(ItemMedis item, TarifKhususPunyaItem tarifKhususPunyaItem) {
		Session session = HibernateUtil.currentSession();
		List<JenisBiaya> tempJenisBiayas = new ArrayList<JenisBiaya>();
		if (item != null && item.getId() != null) {
			tempJenisBiayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksiLayanan"))
					.add(Restrictions.isNull("detailTransaksi")).setProjection(Projections.groupProperty("jenisBiaya"))
					.createAlias("hargaJualItem", "hargaJualItem").createAlias("jenisBiaya", "jenisBiaya")
					.add(Restrictions.eq("jenisBiaya.aktif", true))
					.add(Restrictions.eq("jenisBiaya.tipe", JenisBiaya.TIPE_ITEM))

					.add(tarifKhususPunyaItem == null
							? Restrictions.and(Restrictions.eq("hargaJualItem.item", item),
									Restrictions.isNull("hargaJualItem.tarifKhususPunyaItem"))
							: Restrictions.and(
									Restrictions.eq("hargaJualItem.tarifKhususPunyaItem", tarifKhususPunyaItem),
									Restrictions.isNull("hargaJualItem.item")))

					.list();
		}

		if (tempJenisBiayas.isEmpty()) {
			tempJenisBiayas.addAll(session.createCriteria(JenisBiaya.class).add(Restrictions.eq("defaultAktif", true))
					.add(Restrictions.eq("tipe", JenisBiaya.TIPE_ITEM)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nama")).list());
		}

		return tempJenisBiayas;
	}

	/** Mengambil baris {@link Biaya} master (belum terpakai transaksi) untuk kombinasi item/tarif khusus dan satu {@code jenisBiaya} tertentu — dipakai setelah {@link #getJenisBiayas} untuk merinci komponen biaya per jenis. */
	@SuppressWarnings("unchecked")
	public static List<Biaya> getBiayaPerJenis(ItemMedis item, TarifKhususPunyaItem tarifKhususPunyaItem,
			JenisBiaya jenisBiaya) {
		Session session = HibernateUtil.currentSession();
		List<Biaya> biayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksiLayanan"))
				.add(Restrictions.isNull("detailTransaksi")).createAlias("hargaJualItem", "hargaJualItem")
				.add(Restrictions.eq("jenisBiaya", jenisBiaya))

				.add(tarifKhususPunyaItem == null
						? Restrictions.and(Restrictions.eq("hargaJualItem.item", item),
								Restrictions.isNull("hargaJualItem.tarifKhususPunyaItem"))
						: Restrictions.and(Restrictions.eq("hargaJualItem.tarifKhususPunyaItem", tarifKhususPunyaItem),
								Restrictions.isNull("hargaJualItem.item")))

				.list();

		return biayas;
	}

}
