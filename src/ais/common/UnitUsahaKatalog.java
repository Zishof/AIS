package ais.common;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;

/**
 * <h3>Katalog tunggal jenis Unit Usaha yang dapat dipilih per {@code Toko}.</h3>
 *
 * <p>Satu toko/pedagang boleh memiliki LEBIH DARI SATU unit usaha (mis. bengkel motor yang juga
 * menjual sparepart dan jasa cuci) -- pilihan disimpan sebagai JSON array kode pada kolom
 * {@code Toko.unitUsahaJson}. Katalog di-hardcode di kode (bukan tabel DB) dengan alasan yang
 * sama seperti {@link EbisnisMenuKatalog}: daftarnya berubah mengikuti rilis kode (generator
 * data contoh per unit usaha ikut rilis), bukan data yang diedit runtime; kunci tak dikenal di
 * JSON tersimpan cukup diabaikan sehingga tidak pernah memecahkan parse.</p>
 *
 * <p>Dipakai oleh: form Toko di ZK ({@code TokoAction}), JSP ({@code toko.jsp}), API klien POS
 * Desktop/Android ({@code TokoApiHelper}, aksi {@code unit_usaha_katalog}), dan generator data
 * contoh produk ({@code PosDemoProvisionHelper} -- katalog produk contoh per unit usaha).</p>
 */
public final class UnitUsahaKatalog {

	private UnitUsahaKatalog() {
	}

	/** Satu jenis unit usaha: kode stabil (disimpan di JSON) + label tampilan + grup UI. */
	public static final class Entri {
		public final String kode;
		public final String label;
		public final String grup;

		public Entri(String grup, String kode, String label) {
			this.grup = grup;
			this.kode = kode;
			this.label = label;
		}
	}

	public static final String GRUP_RETAIL = "Retail & Toko";
	public static final String GRUP_KULINER = "Kuliner & Minuman";
	public static final String GRUP_OTOMOTIF = "Otomotif";
	public static final String GRUP_JASA = "Jasa";
	public static final String GRUP_AGRI = "Agribisnis & Peternakan";
	public static final String GRUP_LAINNYA = "Lainnya";

	/** Daftar lengkap, urut per grup. Menambah unit usaha baru = tambah baris di sini. */
	public static final List<Entri> DAFTAR = new ArrayList<Entri>();
	static {
		// -- Retail & Toko --
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_KELONTONG", "Toko Kelontong"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "MINIMARKET", "Minimarket"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "SUPERMARKET", "Supermarket"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "GROSIR", "Grosir / Distributor"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_SEMBAKO", "Toko Sembako"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_BANGUNAN", "Toko Bangunan & Material"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_ELEKTRONIK", "Toko Elektronik"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "KONTER_HP", "Konter HP, Pulsa & Aksesoris"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_PAKAIAN", "Toko Pakaian / Butik"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_SEPATU_TAS", "Toko Sepatu & Tas"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_MAINAN", "Toko Mainan"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_BUKU_ATK", "Toko Buku & ATK"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_KOSMETIK", "Toko Kosmetik & Perawatan"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "APOTEK", "Apotek / Toko Obat"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_PERTANIAN", "Toko Pertanian (Saprotan)"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_PAKAN_TERNAK", "Toko Pakan Ternak"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_EMAS", "Toko Emas & Perhiasan"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_BUNGA", "Toko Bunga (Florist)"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "PET_SHOP", "Pet Shop"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_IKAN_HIAS", "Toko Ikan Hias & Akuarium"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_BUAH_SAYUR", "Toko Buah & Sayur"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "TOKO_DAGING_FROZEN", "Toko Daging & Frozen Food"));
		DAFTAR.add(new Entri(GRUP_RETAIL, "BAKERY", "Toko Roti & Kue (Bakery)"));

		// -- Kuliner & Minuman --
		DAFTAR.add(new Entri(GRUP_KULINER, "WARUNG_MAKAN", "Warung / Rumah Makan"));
		DAFTAR.add(new Entri(GRUP_KULINER, "RESTORAN", "Restoran"));
		DAFTAR.add(new Entri(GRUP_KULINER, "KAFE", "Kafe / Kedai Kopi"));
		DAFTAR.add(new Entri(GRUP_KULINER, "ANGKRINGAN", "Angkringan / Kaki Lima"));
		DAFTAR.add(new Entri(GRUP_KULINER, "KATERING", "Katering"));
		DAFTAR.add(new Entri(GRUP_KULINER, "DEPOT_AIR", "Depot Air Minum Isi Ulang"));
		DAFTAR.add(new Entri(GRUP_KULINER, "KEDAI_MINUMAN", "Kedai Es & Minuman Kekinian"));

		// -- Otomotif --
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "BENGKEL_MOBIL", "Bengkel Mobil"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "BENGKEL_MOTOR", "Bengkel Motor"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "BENGKEL_SEPEDA", "Bengkel Sepeda"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "SPAREPART_MOBIL", "Toko Sparepart Mobil"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "SPAREPART_MOTOR", "Toko Sparepart Motor"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "CUCI_MOBIL", "Jasa Cuci Mobil"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "CUCI_MOTOR", "Jasa Cuci Motor"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "SEWA_MOBIL", "Sewa Mobil (Rental)"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "SEWA_MOTOR", "Sewa Motor"));
		DAFTAR.add(new Entri(GRUP_OTOMOTIF, "TAMBAL_BAN", "Tambal Ban & Toko Ban"));

		// -- Jasa --
		DAFTAR.add(new Entri(GRUP_JASA, "EKSPEDISI", "Ekspedisi / Kurir / Kargo"));
		DAFTAR.add(new Entri(GRUP_JASA, "LAUNDRY", "Laundry"));
		DAFTAR.add(new Entri(GRUP_JASA, "FOTOKOPI_PERCETAKAN", "Fotokopi & Percetakan"));
		DAFTAR.add(new Entri(GRUP_JASA, "STUDIO_FOTO", "Studio Foto"));
		DAFTAR.add(new Entri(GRUP_JASA, "SALON_BARBER", "Salon / Barbershop"));
		DAFTAR.add(new Entri(GRUP_JASA, "PENJAHIT_KONVEKSI", "Penjahit / Konveksi"));
		DAFTAR.add(new Entri(GRUP_JASA, "SERVIS_ELEKTRONIK", "Servis Elektronik"));
		DAFTAR.add(new Entri(GRUP_JASA, "SERVIS_HP_KOMPUTER", "Servis HP & Komputer"));
		DAFTAR.add(new Entri(GRUP_JASA, "SEWA_ALAT_PESTA", "Event Organizer & Sewa Alat Pesta"));
		DAFTAR.add(new Entri(GRUP_JASA, "SEWA_ALAT_BERAT", "Sewa Alat Berat"));
		DAFTAR.add(new Entri(GRUP_JASA, "TRAVEL_TIKET", "Travel & Tiket"));

		// -- Agribisnis & Peternakan --
		DAFTAR.add(new Entri(GRUP_AGRI, "RPA", "Jasa Pemotongan Ayam (RPA)"));
		DAFTAR.add(new Entri(GRUP_AGRI, "RPH", "Jasa Pemotongan Sapi/Kambing (RPH)"));
		DAFTAR.add(new Entri(GRUP_AGRI, "PETERNAKAN_UNGGAS", "Peternakan Unggas (Telur/DOC)"));
		DAFTAR.add(new Entri(GRUP_AGRI, "JUAL_HEWAN_TERNAK", "Penjualan Hewan Ternak"));
		DAFTAR.add(new Entri(GRUP_AGRI, "PENGGILINGAN_PADI", "Penggilingan Padi / Jasa Giling"));
		DAFTAR.add(new Entri(GRUP_AGRI, "PERIKANAN", "Perikanan / Tambak"));

		// -- Lainnya --
		DAFTAR.add(new Entri(GRUP_LAINNYA, "PENGINAPAN_KOS", "Penginapan / Kos"));
		DAFTAR.add(new Entri(GRUP_LAINNYA, "AGEN_GAS_GALON", "Agen Gas LPG & Galon"));
		DAFTAR.add(new Entri(GRUP_LAINNYA, "AGEN_PEMBAYARAN", "Agen Pembayaran (PPOB)"));
	}

	/** true bila {@code kode} dikenal katalog versi kode ini. */
	public static boolean dikenal(String kode) {
		if (kode == null) return false;
		for (Entri e : DAFTAR) {
			if (e.kode.equals(kode)) return true;
		}
		return false;
	}

	public static String labelDari(String kode) {
		for (Entri e : DAFTAR) {
			if (e.kode.equals(kode)) return e.label;
		}
		return kode;
	}

	/**
	 * Urai kolom {@code Toko.unitUsahaJson} menjadi himpunan kode VALID (kode tak dikenal
	 * dibuang diam-diam -- fail-safe thd data lama/masa depan). Null/kosong/rusak = himpunan
	 * kosong, artinya toko belum memilih unit usaha.
	 */
	public static Set<String> urai(String unitUsahaJson) {
		Set<String> hasil = new LinkedHashSet<String>();
		if (unitUsahaJson == null || unitUsahaJson.trim().isEmpty()) {
			return hasil;
		}
		try {
			JSONArray arr = new JSONArray(unitUsahaJson);
			for (int i = 0; i < arr.length(); i++) {
				String kode = arr.optString(i, "").trim();
				if (dikenal(kode)) {
					hasil.add(kode);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "UnitUsahaKatalog.urai: JSON rusak, dianggap kosong");
		}
		return hasil;
	}

	/** Serialisasi himpunan kode (hanya yang dikenal) ke JSON array utk disimpan di kolom. */
	public static String keJson(Set<String> kodeSet) {
		JSONArray arr = new JSONArray();
		if (kodeSet != null) {
			for (String kode : kodeSet) {
				if (dikenal(kode)) {
					arr.put(kode);
				}
			}
		}
		return arr.toString();
	}
}
