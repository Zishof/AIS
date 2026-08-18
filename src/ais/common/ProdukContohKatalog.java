package ais.common;

import java.util.HashMap;
import java.util.Map;

/**
 * <h3>Katalog nama produk contoh per Unit Usaha ({@link UnitUsahaKatalog}).</h3>
 *
 * <p>Nama dibangun kombinatorik {@code dasar x variasi x satuan} + penghitung ekor
 * ("Paket 2", "Isi 3", ...) sehingga SATU unit usaha dapat menghasilkan sampai
 * 100.000 nama unik yang tetap tampak alami -- pola yang sama dengan katalog
 * minimarket lama di {@code PosDemoProvisionHelper.namaProdukMinimarket}, tanpa
 * label "uji"/"dummy" pada nama yang dilihat pengguna. Penghitung ekor TIDAK
 * dimodulo supaya nama selalu unik untuk nomor berapa pun.</p>
 *
 * <p>Rentang harga beli per unit usaha ikut di sini karena wajar berbeda jauh
 * (sembako ribuan rupiah vs hewan kurban jutaan); harga jual dihitung pemanggil
 * dengan markup deterministik. Kelompok jenis produk diturunkan dari GRUP katalog
 * unit usaha (retail/kuliner/otomotif/jasa/agri/lainnya).</p>
 */
public final class ProdukContohKatalog {

	private ProdukContohKatalog() {
	}

	/** Bahan pembangkit nama + rentang harga beli satu unit usaha. */
	public static final class Katalog {
		public final String[] dasar;
		public final String[] variasi;
		public final String[] satuan;
		/** Kata sambung penghitung ekor: "Isi" utk barang, "Paket" utk jasa/sewa. */
		public final String sufiks;
		public final double beliMin;
		public final double beliRentang;

		Katalog(String[] dasar, String[] variasi, String[] satuan,
				String sufiks, double beliMin, double beliRentang) {
			this.dasar = dasar;
			this.variasi = variasi;
			this.satuan = satuan;
			this.sufiks = sufiks;
			this.beliMin = beliMin;
			this.beliRentang = beliRentang;
		}
	}

	private static final Map<String, Katalog> PETA = new HashMap<String, Katalog>();

	private static final String[] DURASI_SEWA = {
			"6 Jam", "12 Jam", "24 Jam", "Harian", "Mingguan", "Bulanan" };
	private static final String[] BERAT_UMUM = {
			"250 g", "500 g", "1 kg", "2 kg", "5 kg", "10 kg" };
	private static final String[] PAKET_LAYANAN = {
			"Reguler", "Ekspres", "Premium", "Hemat", "Lengkap" };

	private static void daftar(String kode, String[] dasar, String[] variasi,
			String[] satuan, String sufiks, double beliMin, double beliRentang) {
		PETA.put(kode, new Katalog(dasar, variasi, satuan, sufiks, beliMin, beliRentang));
	}

	static {
		// ------------------------------ Retail & Toko ------------------------------
		daftar("TOKO_KELONTONG", new String[] {
				"Beras", "Gula Pasir", "Minyak Goreng", "Telur Ayam", "Tepung Terigu",
				"Kopi Bubuk", "Teh Celup", "Susu Kental Manis", "Mie Instan", "Kerupuk",
				"Sabun Mandi", "Deterjen", "Sampo Sachet", "Kecap Manis", "Saus Sambal",
				"Garam Dapur", "Air Mineral", "Gula Merah", "Santan Instan", "Obat Nyamuk Bakar" },
			new String[] { "Cap Dua Tani", "Cap Ikan Mas", "Cap Jempol", "Merek Lokal",
				"Premium", "Ekonomis", "Curah", "Kemasan Pabrik" },
			new String[] { "250 g", "500 g", "1 kg", "2 kg", "5 kg", "1 liter", "Sachet", "Renceng" },
			"Isi", 500.0, 150000.0);
		daftar("MINIMARKET", new String[] {
				"Aqua Air Mineral", "Indomie Mi Goreng", "Ultra Milk Susu UHT", "Teh Botol Sosro",
				"Kapal Api Kopi Bubuk", "Roma Biskuit Kelapa", "SilverQueen Cokelat", "Chitato Keripik",
				"Sari Roti Tawar", "ABC Kecap Manis", "Bimoli Minyak Goreng", "Sunlight Sabun Cuci",
				"Rinso Deterjen", "Lifebuoy Sabun Mandi", "Pepsodent Pasta Gigi", "Pantene Sampo",
				"Paseo Tisu", "MamyPoko Popok Bayi", "Panadol Tablet", "Pocari Sweat" },
			new String[] { "Sachet", "Pouch", "Botol", "Kaleng", "Kotak", "Cup", "Refill", "Family Pack" },
			new String[] { "50 g", "100 g", "200 g", "250 ml", "330 ml", "500 ml", "600 ml", "1 liter" },
			"Isi", 500.0, 250000.0);
		daftar("SUPERMARKET", new String[] {
				"Daging Sapi Segar", "Ayam Broiler", "Ikan Kembung", "Udang Segar", "Brokoli",
				"Wortel", "Apel Fuji", "Jeruk Mandarin", "Anggur Merah", "Keju Cheddar",
				"Yogurt", "Sosis Sapi", "Nugget Ayam", "Roti Gandum", "Beras Organik",
				"Pasta", "Saus Pasta", "Salad Sayur", "Susu Segar", "Air Mineral" },
			new String[] { "Segar", "Organik", "Import", "Lokal", "Premium", "Beku", "Kemasan" },
			BERAT_UMUM, "Isi", 2000.0, 300000.0);
		daftar("GROSIR", new String[] {
				"Beras", "Gula Pasir", "Minyak Goreng", "Tepung Terigu", "Mie Instan",
				"Air Mineral", "Kopi Sachet", "Deterjen", "Sabun Batang", "Snack Kiloan",
				"Kecap", "Saus Sambal", "Tisu", "Popok Bayi", "Minuman Ringan", "Biskuit Kaleng" },
			new String[] { "Karton", "Bal", "Dus", "Karung", "Pack Grosir", "Krat" },
			new String[] { "Isi 12", "Isi 24", "Isi 40", "Isi 48", "25 kg", "50 kg" },
			"Lot", 20000.0, 800000.0);
		daftar("TOKO_SEMBAKO", new String[] {
				"Beras", "Gula Pasir", "Minyak Goreng", "Tepung Terigu", "Telur Ayam",
				"Bawang Merah", "Bawang Putih", "Cabai Merah", "Kacang Tanah", "Kacang Hijau",
				"Ikan Asin", "Teri Medan", "Kemiri", "Ketumbar", "Merica Butir", "Gula Merah" },
			new String[] { "Super", "Medium", "Premium", "Curah", "Kemasan", "Lokal" },
			BERAT_UMUM, "Isi", 1000.0, 200000.0);
		daftar("TOKO_BANGUNAN", new String[] {
				"Semen", "Pasir", "Batu Split", "Bata Merah", "Batako", "Besi Beton",
				"Triplek", "Cat Tembok", "Cat Kayu", "Paku", "Pipa PVC", "Keran Air",
				"Keramik Lantai", "Genteng", "Kawat Bendrat", "Kayu Kaso", "Papan Cor", "Lem Kayu" },
			new String[] { "Standar", "SNI", "Premium", "Ekonomis", "Tahan Air", "Anti Karat" },
			new String[] { "Per Sak", "Per m3", "Per Batang", "Per Lembar", "Per Kg", "Per Dus", "Per m2" },
			"Isi", 5000.0, 900000.0);
		daftar("TOKO_ELEKTRONIK", new String[] {
				"Televisi LED", "Kulkas", "Mesin Cuci", "Kipas Angin", "Rice Cooker",
				"Blender", "Setrika", "Dispenser", "Speaker Aktif", "AC Split",
				"Microwave", "Kompor Listrik", "Lampu LED", "Stop Kontak", "Kabel Rol", "Antena Digital" },
			new String[] { "32 Inch", "43 Inch", "1 Pintu", "2 Pintu", "Low Watt", "Standar", "Digital" },
			new String[] { "Unit", "Set", "Paket", "Garansi 1 Tahun", "Garansi 2 Tahun", "Promo" },
			"Paket", 50000.0, 6000000.0);
		daftar("KONTER_HP", new String[] {
				"Pulsa Telkomsel", "Pulsa Indosat", "Pulsa XL", "Paket Data Telkomsel",
				"Paket Data Indosat", "Paket Data XL", "Voucher Game", "Kartu Perdana",
				"Casing HP", "Tempered Glass", "Charger", "Kabel Data", "Earphone",
				"Headset Bluetooth", "Power Bank", "Memory Card", "Baterai HP", "Holder HP" },
			new String[] { "Original", "OEM", "Premium", "Universal", "Fast Charging", "Anti Gores" },
			new String[] { "5.000", "10.000", "25.000", "50.000", "100.000", "Pcs" },
			"Paket", 3000.0, 300000.0);
		daftar("TOKO_PAKAIAN", new String[] {
				"Kaos Polos", "Kemeja", "Celana Jeans", "Celana Chino", "Jaket Hoodie",
				"Gamis", "Hijab Segi Empat", "Batik Pria", "Dress Wanita", "Rok",
				"Kaos Anak", "Piyama", "Daster", "Celana Pendek", "Sweater", "Kaos Kaki" },
			new String[] { "Katun", "Denim", "Polyester", "Rayon", "Motif", "Polos", "Sablon", "Bordir" },
			new String[] { "Ukuran S", "Ukuran M", "Ukuran L", "Ukuran XL", "Ukuran XXL", "All Size" },
			"Isi", 10000.0, 400000.0);
		daftar("TOKO_SEPATU_TAS", new String[] {
				"Sepatu Sneakers", "Sepatu Formal", "Sepatu Sekolah", "Sandal Jepit",
				"Sandal Gunung", "Sepatu Olahraga", "Sepatu Boots", "Flat Shoes",
				"Tas Ransel", "Tas Selempang", "Tas Tote", "Dompet Pria", "Dompet Wanita",
				"Koper", "Tas Laptop", "Tas Sekolah" },
			new String[] { "Kulit", "Kanvas", "Sintetis", "Suede", "Anti Air", "Casual", "Formal" },
			new String[] { "Ukuran 36", "Ukuran 38", "Ukuran 40", "Ukuran 42", "Ukuran 44", "All Size" },
			"Isi", 15000.0, 800000.0);
		daftar("TOKO_MAINAN", new String[] {
				"Mobil Remote Control", "Boneka Beruang", "Blok Susun", "Puzzle Kayu",
				"Mainan Masak-Masakan", "Robot Transformasi", "Pistol Air", "Layang-Layang",
				"Bola Plastik", "Sepeda Mini", "Skuter Anak", "Mainan Edukasi",
				"Drone Mini", "Kartu Permainan", "Slime Kit", "Action Figure" },
			new String[] { "Baterai", "Pull Back", "Kayu", "Plastik ABS", "Edukatif", "Karakter" },
			new String[] { "Kecil", "Sedang", "Besar", "Set", "Paket Hemat", "Edisi Baru" },
			"Seri", 5000.0, 500000.0);
		daftar("TOKO_BUKU_ATK", new String[] {
				"Buku Tulis", "Pulpen", "Pensil", "Penghapus", "Penggaris", "Spidol",
				"Kertas HVS", "Amplop", "Map Plastik", "Stapler", "Buku Gambar", "Krayon",
				"Tinta Printer", "Lem Kertas", "Gunting", "Cutter", "Kalkulator", "Buku Agenda" },
			new String[] { "38 Lembar", "58 Lembar", "A4", "F4", "Hitam", "Biru", "Warna", "Standar" },
			new String[] { "Pcs", "Lusin", "Pack", "Rim", "Box", "Set" },
			"Isi", 1000.0, 150000.0);
		daftar("TOKO_KOSMETIK", new String[] {
				"Lipstik", "Bedak Tabur", "Bedak Padat", "Foundation", "Maskara",
				"Eyeliner", "Pelembab Wajah", "Serum Wajah", "Sunscreen", "Toner",
				"Micellar Water", "Masker Wajah", "Parfum", "Body Lotion", "Lip Cream",
				"Pensil Alis", "BB Cream", "Cushion" },
			new String[] { "Matte", "Glossy", "Natural", "Waterproof", "SPF 30", "SPF 50", "Brightening" },
			new String[] { "15 ml", "30 ml", "50 ml", "100 ml", "Mini", "Full Size" },
			"Isi", 5000.0, 300000.0);
		daftar("APOTEK", new String[] {
				"Paracetamol", "Amoxicillin", "Vitamin C", "Vitamin D3", "Obat Batuk Sirup",
				"Obat Maag", "Oralit", "Minyak Kayu Putih", "Balsem", "Plester Luka",
				"Kasa Steril", "Alkohol 70 Persen", "Povidone Iodine", "Masker Medis",
				"Hand Sanitizer", "Termometer", "Obat Flu", "Obat Alergi", "Multivitamin Anak", "Suplemen Zat Besi" },
			new String[] { "Tablet", "Kapsul", "Sirup", "Strip", "Botol", "Tube", "Generik", "Paten" },
			new String[] { "250 mg", "500 mg", "60 ml", "100 ml", "10 Tablet", "20 Kapsul" },
			"Box", 1000.0, 200000.0);
		daftar("TOKO_PERTANIAN", new String[] {
				"Pupuk NPK", "Pupuk Urea", "Pupuk Kompos", "Pupuk KCL", "Benih Padi",
				"Benih Jagung", "Benih Cabai", "Benih Tomat", "Insektisida", "Fungisida",
				"Herbisida", "Zat Pengatur Tumbuh", "Sprayer", "Cangkul", "Sabit",
				"Mulsa Plastik", "Polybag", "Sekam Bakar" },
			new String[] { "Granul", "Cair", "Bubuk", "Organik", "Hibrida", "Unggul" },
			new String[] { "250 g", "500 g", "1 kg", "5 kg", "25 kg", "50 kg", "1 liter" },
			"Isi", 3000.0, 500000.0);
		daftar("TOKO_PAKAN_TERNAK", new String[] {
				"Pakan Ayam Broiler", "Pakan Ayam Petelur", "Pakan Lele", "Pakan Nila",
				"Pakan Sapi", "Pakan Kambing", "Pakan Burung", "Pakan Kelinci",
				"Konsentrat", "Dedak Padi", "Jagung Giling", "Bungkil Kedelai",
				"Mineral Ternak", "Vitamin Ternak", "Grit Kerang", "Pakan Kucing", "Pakan Anjing" },
			new String[] { "Starter", "Grower", "Finisher", "Premium", "Ekonomis", "Protein Tinggi" },
			new String[] { "1 kg", "5 kg", "10 kg", "25 kg", "50 kg", "Karung" },
			"Isi", 5000.0, 600000.0);
		daftar("TOKO_EMAS", new String[] {
				"Cincin Emas", "Kalung Emas", "Gelang Emas", "Anting Emas", "Liontin",
				"Cincin Kawin", "Gelang Bayi", "Kalung Anak", "Emas Batangan",
				"Cincin Perak", "Kalung Perak", "Gelang Perak" },
			new String[] { "24 Karat", "22 Karat", "18 Karat", "17 Karat", "Putih", "Kuning", "Rose Gold" },
			new String[] { "1 gram", "2 gram", "3 gram", "5 gram", "10 gram", "25 gram" },
			"Seri", 400000.0, 15000000.0);
		daftar("TOKO_BUNGA", new String[] {
				"Buket Mawar", "Buket Lily", "Buket Bunga Matahari", "Buket Tulip",
				"Rangkaian Anggrek", "Bunga Papan Duka", "Bunga Papan Wedding",
				"Bunga Papan Grand Opening", "Bunga Meja", "Standing Flower",
				"Tanaman Hias Monstera", "Tanaman Hias Sansevieria", "Pot Keramik",
				"Vas Bunga", "Bunga Artificial" },
			new String[] { "Merah", "Putih", "Pink", "Kuning", "Mix Warna", "Premium", "Mini" },
			new String[] { "5 Tangkai", "10 Tangkai", "20 Tangkai", "Kecil", "Sedang", "Besar" },
			"Seri", 15000.0, 900000.0);
		daftar("PET_SHOP", new String[] {
				"Makanan Kucing", "Makanan Anjing", "Pasir Kucing", "Vitamin Kucing",
				"Shampoo Hewan", "Kandang Kucing", "Kandang Anjing", "Tali Tuntun",
				"Kalung Hewan", "Mainan Kucing", "Tempat Makan Hewan", "Obat Kutu",
				"Snack Hewan", "Sisir Bulu", "Pet Cargo", "Baju Kucing" },
			new String[] { "Kitten", "Adult", "Puppy", "Premium", "Grain Free", "Wangi Lavender" },
			new String[] { "500 g", "1 kg", "2.5 kg", "5 kg", "Pcs", "Set" },
			"Isi", 5000.0, 400000.0);
		daftar("TOKO_IKAN_HIAS", new String[] {
				"Ikan Cupang", "Ikan Koi", "Ikan Mas Koki", "Ikan Guppy", "Ikan Molly",
				"Ikan Neon Tetra", "Ikan Manfish", "Akuarium", "Filter Akuarium",
				"Aerator", "Lampu Akuarium", "Pakan Ikan Hias", "Tanaman Air",
				"Batu Hias", "Pasir Malang", "Obat Ikan" },
			new String[] { "Halfmoon", "Plakat", "Slayer", "Import", "Lokal", "Grade A", "Grade B" },
			new String[] { "Per Ekor", "Per Pasang", "Kecil", "Sedang", "Besar", "Per Set" },
			"Seri", 3000.0, 500000.0);
		daftar("TOKO_BUAH_SAYUR", new String[] {
				"Apel Malang", "Jeruk Pontianak", "Mangga Harum Manis", "Pisang Cavendish",
				"Semangka", "Melon", "Pepaya California", "Alpukat Mentega", "Anggur Hijau",
				"Salak Pondoh", "Bayam", "Kangkung", "Sawi Hijau", "Tomat", "Cabai Merah",
				"Cabai Rawit", "Bawang Merah", "Bawang Putih", "Kentang", "Wortel" },
			new String[] { "Segar", "Organik", "Super", "Grade A", "Lokal", "Import" },
			new String[] { "250 g", "500 g", "1 kg", "2 kg", "Per Ikat", "Per Buah" },
			"Isi", 1000.0, 100000.0);
		daftar("TOKO_DAGING_FROZEN", new String[] {
				"Daging Sapi Slice", "Daging Rendang", "Ayam Karkas", "Ayam Fillet",
				"Ikan Dori Fillet", "Udang Kupas", "Cumi Ring", "Bakso Sapi",
				"Sosis Ayam", "Nugget Ayam", "Kentang Goreng Beku", "Dimsum",
				"Siomay", "Kebab Frozen", "Daging Burger", "Otak-Otak" },
			new String[] { "Premium", "Reguler", "Jumbo", "Mini", "Original", "Pedas" },
			new String[] { "250 g", "500 g", "1 kg", "Per Pack", "Pack Besar", "Pack Kecil" },
			"Isi", 5000.0, 300000.0);
		daftar("BAKERY", new String[] {
				"Roti Tawar", "Roti Sobek", "Roti Cokelat", "Roti Keju", "Donat",
				"Croissant", "Bolu Pandan", "Brownies", "Kue Lapis", "Cheese Cake",
				"Black Forest", "Tart Buah", "Cupcake", "Pastry Apel", "Roti Gandum",
				"Bagelen", "Kue Sus", "Pia" },
			new String[] { "Cokelat", "Keju", "Vanila", "Pandan", "Original", "Spesial", "Premium" },
			new String[] { "Slice", "Utuh", "Box Kecil", "Box Sedang", "Box Besar", "Mini" },
			"Isi", 2000.0, 300000.0);

		// ---------------------------- Kuliner & Minuman ----------------------------
		daftar("WARUNG_MAKAN", new String[] {
				"Nasi Rames", "Nasi Ayam Goreng", "Nasi Ayam Bakar", "Nasi Rendang",
				"Nasi Telur Dadar", "Nasi Lele Goreng", "Soto Ayam", "Rawon",
				"Pecel Sayur", "Gado-Gado", "Sayur Asem", "Sayur Lodeh", "Ikan Bakar",
				"Tempe Goreng", "Tahu Goreng", "Sambal Terasi", "Es Teh", "Es Jeruk" },
			new String[] { "Porsi Biasa", "Porsi Jumbo", "Pedas", "Tidak Pedas", "Komplit", "Tanpa Nasi" },
			new String[] { "Per Porsi", "Per Paket", "Bungkus", "Makan di Tempat" },
			"Paket", 3000.0, 60000.0);
		daftar("RESTORAN", new String[] {
				"Sup Iga", "Steak Sirloin", "Steak Tenderloin", "Ayam Panggang",
				"Gurame Asam Manis", "Udang Saus Padang", "Cah Kangkung", "Fuyunghai",
				"Nasi Goreng Spesial", "Mie Goreng Seafood", "Salad Buah", "Sup Krim Jagung",
				"Spaghetti Bolognese", "Chicken Cordon Bleu", "Es Krim", "Puding Cokelat",
				"Jus Alpukat", "Mocktail Tropis" },
			new String[] { "Porsi Single", "Porsi Double", "Porsi Keluarga", "Pedas", "Original", "Spesial" },
			new String[] { "Per Porsi", "Per Paket", "Set Menu", "Ala Carte" },
			"Paket", 10000.0, 250000.0);
		daftar("KAFE", new String[] {
				"Kopi Americano", "Kopi Latte", "Cappuccino", "Espresso",
				"Kopi Susu Gula Aren", "Cold Brew", "Matcha Latte", "Cokelat Panas",
				"Teh Tarik", "Lemon Tea", "Croffle", "Waffle", "Kentang Goreng",
				"Sandwich", "Pasta Aglio Olio", "Roti Bakar", "Cheesecake Slice", "Butter Cookies" },
			new String[] { "Hot", "Iced", "Less Sugar", "Normal Sugar", "Extra Shot", "Large", "Regular" },
			new String[] { "Cup Kecil", "Cup Sedang", "Cup Besar", "Per Porsi" },
			"Paket", 5000.0, 80000.0);
		daftar("ANGKRINGAN", new String[] {
				"Nasi Kucing", "Sate Usus", "Sate Telur Puyuh", "Sate Kulit",
				"Tempe Bacem", "Tahu Bacem", "Mendoan", "Ceker Bakar",
				"Kepala Ayam Bakar", "Wedang Jahe", "Es Teh", "Kopi Joss",
				"Susu Jahe", "Roti Bakar", "Pisang Bakar", "Kerupuk" },
			new String[] { "Original", "Pedas", "Manis", "Bakar", "Goreng", "Spesial" },
			new String[] { "Per Porsi", "Per Tusuk", "Per Bungkus", "Per Gelas" },
			"Paket", 1000.0, 25000.0);
		daftar("KATERING", new String[] {
				"Nasi Box Ayam", "Nasi Box Rendang", "Nasi Box Ikan", "Prasmanan Standar",
				"Prasmanan Premium", "Paket Aqiqah", "Paket Hajatan", "Paket Rapat Kantor",
				"Snack Box", "Tumpeng Mini", "Tumpeng Besar", "Paket Coffee Break",
				"Paket Pernikahan", "Paket Arisan", "Paket Buka Puasa", "Nasi Kotak Sekolah" },
			new String[] { "25 Pax", "50 Pax", "100 Pax", "200 Pax", "Ekonomis", "Deluxe" },
			new String[] { "Per Pax", "Per Paket", "Per Event" },
			"Paket", 10000.0, 2000000.0);
		daftar("DEPOT_AIR", new String[] {
				"Isi Ulang Galon", "Galon Baru Plus Isi", "Air RO", "Air Mineral",
				"Air Hexagonal", "Tutup Galon", "Tisu Galon", "Pompa Galon Manual",
				"Pompa Galon Elektrik", "Antar Galon", "Cuci Galon", "Sikat Galon" },
			new String[] { "19 Liter", "15 Liter", "Galon Kecil", "Reguler", "Premium" },
			new String[] { "Per Galon", "Per Pcs", "Per Layanan" },
			"Paket", 2000.0, 60000.0);
		daftar("KEDAI_MINUMAN", new String[] {
				"Es Teh Manis", "Thai Tea", "Green Tea", "Boba Milk Tea",
				"Brown Sugar Boba", "Es Kopi Susu", "Taro Milk", "Red Velvet",
				"Es Buah", "Es Campur", "Es Doger", "Jus Mangga", "Jus Alpukat",
				"Jus Jambu", "Smoothies Strawberry", "Es Cincau", "Es Kelapa Muda", "Milkshake Cokelat" },
			new String[] { "Regular", "Large", "Less Ice", "Extra Boba", "Less Sugar", "Normal" },
			new String[] { "Cup 14 oz", "Cup 16 oz", "Cup 22 oz", "Botol 250 ml", "Botol 1 Liter" },
			"Paket", 2000.0, 30000.0);

		// -------------------------------- Otomotif ---------------------------------
		daftar("BENGKEL_MOBIL", new String[] {
				"Servis Berkala", "Tune Up Mesin", "Ganti Oli Mesin", "Ganti Oli Transmisi",
				"Spooring", "Balancing", "Ganti Kampas Rem", "Ganti Aki", "Servis AC Mobil",
				"Ganti Timing Belt", "Servis Rem", "Ganti Busi", "Kuras Radiator",
				"Ganti Filter Udara", "Ganti Shockbreaker", "Scan Mesin", "Perbaikan Kaki-Kaki", "Ganti V-Belt" },
			new String[] { "Mobil Kecil", "Sedan", "MPV", "SUV", "Double Cabin", "Semua Tipe" },
			new String[] { "Paket Standar", "Paket Lengkap", "Jasa Saja", "Termasuk Sparepart" },
			"Paket", 25000.0, 2500000.0);
		daftar("BENGKEL_MOTOR", new String[] {
				"Servis Rutin", "Ganti Oli Mesin", "Ganti Oli Gardan", "Servis CVT",
				"Servis Karburator", "Servis Injeksi", "Ganti Kampas Rem", "Ganti Ban Luar",
				"Ganti Ban Dalam", "Ganti Aki", "Ganti Busi", "Ganti Rantai Set",
				"Ganti Roller", "Ganti Vanbelt", "Press Ban", "Servis Besar Turun Mesin" },
			new String[] { "Motor Matic", "Motor Bebek", "Motor Sport", "Motor Trail", "Semua Tipe" },
			new String[] { "Paket Standar", "Paket Lengkap", "Jasa Saja", "Termasuk Sparepart" },
			"Paket", 10000.0, 900000.0);
		daftar("BENGKEL_SEPEDA", new String[] {
				"Servis Ringan", "Servis Total", "Setel Rem", "Setel Gigi",
				"Ganti Ban Luar", "Ganti Ban Dalam", "Ganti Rantai", "Ganti Gear Set",
				"Ganti Kampas Rem", "Pasang Aksesoris", "Ganti Jari-Jari", "Setel Pelek",
				"Ganti Pedal", "Ganti Sadel" },
			new String[] { "Sepeda Gunung", "Sepeda Lipat", "Sepeda BMX", "Road Bike", "Sepeda Anak" },
			new String[] { "Paket Standar", "Paket Lengkap", "Jasa Saja", "Termasuk Sparepart" },
			"Paket", 5000.0, 300000.0);
		daftar("SPAREPART_MOBIL", new String[] {
				"Filter Oli", "Filter Udara", "Filter AC", "Kampas Rem Depan",
				"Kampas Rem Belakang", "Busi", "Aki 45 Ah", "Aki 65 Ah", "Shockbreaker",
				"Bearing Roda", "Tie Rod", "Ball Joint", "Timing Belt", "V-Belt",
				"Radiator", "Water Pump", "Wiper", "Lampu Depan", "Oli Mesin 10W-40", "Oli Transmisi" },
			new String[] { "Original", "OEM", "Aftermarket", "Import", "Lokal", "Racing" },
			new String[] { "Pcs", "Set", "Pasang", "Per Liter" },
			"Isi", 15000.0, 2000000.0);
		daftar("SPAREPART_MOTOR", new String[] {
				"Kampas Rem", "Busi", "Oli Mesin", "Oli Gardan", "Roller CVT",
				"Vanbelt", "Rantai Set", "Gear Set", "Aki Kering", "Bohlam Depan",
				"Sein Set", "Spion", "Filter Udara", "Kabel Gas", "Kabel Rem",
				"Handle Rem", "Ban Luar", "Ban Dalam", "Karburator", "Injektor" },
			new String[] { "Original", "OEM", "Aftermarket", "Racing", "Standar" },
			new String[] { "Pcs", "Set", "Pasang", "Per Botol" },
			"Isi", 5000.0, 700000.0);
		daftar("CUCI_MOBIL", new String[] {
				"Cuci Eksterior", "Cuci Interior", "Cuci Komplit", "Cuci Mesin",
				"Doorsmeer", "Wax Body", "Poles Body", "Poles Kaca", "Semir Ban",
				"Vakum Interior", "Fogging Interior", "Cuci Kolong", "Detailing Ringan", "Coating Nano" },
			new String[] { "Mobil Kecil", "Sedan", "MPV", "SUV", "Big SUV" },
			new String[] { "Sekali Cuci", "Paket 5 Kali", "Paket 10 Kali", "Member Bulanan" },
			"Paket", 10000.0, 500000.0);
		daftar("CUCI_MOTOR", new String[] {
				"Cuci Reguler", "Cuci Premium", "Cuci Detail", "Semir Body",
				"Semir Ban", "Poles Body", "Cuci Mesin", "Cuci Helm",
				"Doorsmeer Motor", "Wax Motor" },
			new String[] { "Motor Kecil", "Motor Sedang", "Motor Besar", "Moge" },
			new String[] { "Sekali Cuci", "Paket 5 Kali", "Paket 10 Kali", "Member Bulanan" },
			"Paket", 5000.0, 100000.0);
		daftar("SEWA_MOBIL", new String[] {
				"Sewa Toyota Avanza", "Sewa Toyota Innova", "Sewa Toyota Hiace",
				"Sewa Daihatsu Xenia", "Sewa Honda Brio", "Sewa Mitsubishi Xpander",
				"Sewa Suzuki Ertiga", "Sewa Toyota Alphard", "Sewa Isuzu Elf",
				"Sewa Bus Medium", "Sewa Pick Up", "Sewa Truk Engkel" },
			new String[] { "Lepas Kunci", "Dengan Sopir", "Sopir Plus BBM", "Dalam Kota", "Luar Kota" },
			DURASI_SEWA, "Paket", 100000.0, 1500000.0);
		daftar("SEWA_MOTOR", new String[] {
				"Sewa Honda Beat", "Sewa Honda Vario", "Sewa Honda PCX", "Sewa Yamaha NMAX",
				"Sewa Yamaha Mio", "Sewa Honda Scoopy", "Sewa Yamaha XMAX", "Sewa Vespa Matic",
				"Sewa Honda CRF", "Sewa Yamaha Aerox" },
			new String[] { "Termasuk Helm", "2 Helm", "Termasuk Jas Hujan", "Antar Jemput" },
			DURASI_SEWA, "Paket", 25000.0, 400000.0);
		daftar("TAMBAL_BAN", new String[] {
				"Tambal Ban Tubeless", "Tambal Ban Biasa", "Ganti Ban Luar", "Ganti Ban Dalam",
				"Isi Angin Nitrogen", "Tambal Cacing", "Balancing Ban", "Rotasi Ban",
				"Ban Baru Ring 14", "Ban Baru Ring 17", "Ban Mobil Ring 15", "Ban Mobil Ring 16" },
			new String[] { "Motor", "Mobil", "Truk", "Sepeda" },
			new String[] { "Per Titik", "Per Ban", "Per Set", "Paket" },
			"Paket", 5000.0, 800000.0);

		// ---------------------------------- Jasa -----------------------------------
		daftar("EKSPEDISI", new String[] {
				"Kirim Paket Reguler", "Kirim Paket Ekspres", "Kirim Paket Same Day",
				"Kirim Kargo Darat", "Kirim Kargo Laut", "Kirim Kargo Udara",
				"Kirim Dokumen", "Kirim Motor", "Kirim Elektronik", "Kirim Makanan Beku",
				"Packing Kayu", "Packing Bubble Wrap", "Asuransi Pengiriman",
				"Jemput Paket", "Sewa Kontainer", "Kirim Sepeda" },
			new String[] { "Dalam Kota", "Antar Kota", "Antar Provinsi", "Jawa-Bali", "Luar Pulau" },
			new String[] { "Per Kg", "Per 5 Kg", "Per 10 Kg", "Per Koli", "Per Kubikasi" },
			"Paket", 5000.0, 900000.0);
		daftar("LAUNDRY", new String[] {
				"Cuci Kering Setrika", "Cuci Kering Lipat", "Setrika Saja",
				"Cuci Sepatu", "Cuci Tas", "Cuci Boneka", "Cuci Karpet", "Cuci Gorden",
				"Cuci Bed Cover", "Cuci Selimut", "Cuci Jas", "Cuci Gaun", "Dry Cleaning", "Cuci Helm" },
			new String[] { "Reguler 3 Hari", "Ekspres 1 Hari", "Kilat 6 Jam", "Paket Member" },
			new String[] { "Per Kg", "Per Pcs", "Per Meter", "Per Pasang" },
			"Paket", 3000.0, 100000.0);
		daftar("FOTOKOPI_PERCETAKAN", new String[] {
				"Fotokopi Hitam Putih", "Fotokopi Warna", "Print Dokumen", "Print Foto",
				"Cetak Banner", "Cetak Spanduk", "Cetak Stiker", "Cetak Kartu Nama",
				"Cetak Undangan", "Cetak Brosur", "Jilid Spiral", "Jilid Hard Cover",
				"Laminating", "Scan Dokumen", "Cetak Nota", "Cetak Stempel", "Cetak Kalender" },
			new String[] { "A4", "F4", "A3", "A5", "Glossy", "Doff", "Bolak-Balik" },
			new String[] { "Per Lembar", "Per Box", "Per Rim", "Per Meter", "Per Pcs" },
			"Paket", 200.0, 300000.0);
		daftar("STUDIO_FOTO", new String[] {
				"Pas Foto", "Foto Keluarga", "Foto Wisuda", "Foto Produk",
				"Foto Prewedding", "Foto Maternity", "Foto Bayi", "Video Wedding",
				"Video Company Profile", "Cetak Foto 4R", "Cetak Foto 10R",
				"Bingkai Foto", "Sewa Studio", "Edit Foto", "Restorasi Foto Lama" },
			new String[] { "Indoor", "Outdoor", "Background Polos", "Konsep Custom", "Paket Hemat" },
			new String[] { "Per Sesi", "Per Jam", "Per Paket", "Per Lembar" },
			"Paket", 5000.0, 2000000.0);
		daftar("SALON_BARBER", new String[] {
				"Potong Rambut Pria", "Potong Rambut Wanita", "Potong Rambut Anak",
				"Cukur Jenggot", "Creambath", "Hair Spa", "Smoothing", "Rebonding",
				"Cat Rambut", "Highlight", "Keriting", "Facial Wajah", "Masker Wajah",
				"Pijat Kepala", "Cuci Blow", "Styling" },
			new String[] { "Junior Stylist", "Senior Stylist", "Kapster Pria", "Kapster Wanita" },
			new String[] { "Per Layanan", "Paket 3 Kali", "Paket 5 Kali", "Member" },
			"Paket", 10000.0, 600000.0);
		daftar("PENJAHIT_KONVEKSI", new String[] {
				"Jahit Kemeja", "Jahit Celana", "Jahit Gamis", "Jahit Kebaya",
				"Jahit Jas", "Jahit Seragam Sekolah", "Jahit Seragam Kantor",
				"Permak Celana", "Ganti Resleting", "Vermak Ukuran", "Bordir Logo",
				"Sablon Kaos", "Jahit Gorden", "Jahit Mukena", "Obras", "Wolsum" },
			new String[] { "Bahan Sendiri", "Termasuk Bahan", "Ekspres", "Reguler" },
			new String[] { "Per Pcs", "Per Lusin", "Per Kodi", "Per Meter" },
			"Paket", 5000.0, 800000.0);
		daftar("SERVIS_ELEKTRONIK", new String[] {
				"Servis TV", "Servis Kulkas", "Servis Mesin Cuci", "Servis AC",
				"Servis Kipas Angin", "Servis Rice Cooker", "Servis Blender",
				"Servis Microwave", "Servis Dispenser", "Servis Setrika",
				"Isi Freon AC", "Cuci AC", "Bongkar Pasang AC", "Ganti Kompresor" },
			new String[] { "Panggilan", "Di Tempat", "Garansi 30 Hari", "Garansi 90 Hari" },
			new String[] { "Per Unit", "Per Layanan", "Jasa Saja", "Termasuk Sparepart" },
			"Paket", 25000.0, 1000000.0);
		daftar("SERVIS_HP_KOMPUTER", new String[] {
				"Ganti LCD HP", "Ganti Baterai HP", "Ganti Konektor Cas", "Flash HP",
				"Servis Mati Total", "Ganti Kamera", "Install Ulang Windows",
				"Install Software", "Upgrade RAM", "Upgrade SSD", "Servis Laptop Overheat",
				"Ganti Keyboard Laptop", "Ganti Engsel Laptop", "Recovery Data", "Servis Printer" },
			new String[] { "HP Android", "iPhone", "Laptop", "PC", "Tablet" },
			new String[] { "Per Unit", "Per Layanan", "Jasa Saja", "Termasuk Sparepart" },
			"Paket", 25000.0, 1500000.0);
		daftar("SEWA_ALAT_PESTA", new String[] {
				"Sewa Tenda", "Sewa Kursi Futura", "Sewa Meja Bundar", "Sewa Sound System",
				"Sewa Panggung", "Sewa Genset", "Sewa AC Standing", "Sewa Kipas Misting",
				"Sewa Alat Prasmanan", "Sewa Dekorasi Pelaminan", "Sewa Karpet",
				"Sewa Lighting", "Sewa Proyektor", "Sewa Photo Booth", "Paket Pernikahan", "Paket Ulang Tahun" },
			new String[] { "Standar", "VIP", "Custom", "Termasuk Kru" },
			new String[] { "Per Hari", "Per Event", "Per Set", "Per Unit" },
			"Paket", 25000.0, 5000000.0);
		daftar("SEWA_ALAT_BERAT", new String[] {
				"Sewa Excavator", "Sewa Bulldozer", "Sewa Wheel Loader", "Sewa Vibro Roller",
				"Sewa Crane", "Sewa Forklift", "Sewa Dump Truck", "Sewa Concrete Mixer",
				"Sewa Genset Besar", "Sewa Scaffolding", "Sewa Jack Hammer", "Sewa Stamper" },
			new String[] { "Mini", "Standar", "Besar", "Termasuk Operator", "Lepas Kunci" },
			new String[] { "Per Jam", "Per Hari", "Per Minggu", "Per Bulan" },
			"Paket", 150000.0, 20000000.0);
		daftar("TRAVEL_TIKET", new String[] {
				"Tiket Pesawat Domestik", "Tiket Kereta", "Tiket Bus", "Tiket Kapal",
				"Travel Antar Kota", "Paket Wisata Bali", "Paket Wisata Jogja",
				"Paket Wisata Bromo", "Paket Umroh", "Voucher Hotel", "Sewa Elf Wisata",
				"Paket Study Tour", "Antar Jemput Bandara", "City Tour" },
			new String[] { "Ekonomi", "Bisnis", "Eksekutif", "Promo", "Reguler" },
			new String[] { "Per Orang", "Per Pax", "Per Grup", "Per Trip" },
			"Paket", 25000.0, 5000000.0);

		// ------------------------- Agribisnis & Peternakan -------------------------
		daftar("RPA", new String[] {
				"Jasa Potong Ayam", "Jasa Potong Plus Bersih", "Jasa Cabut Bulu",
				"Ayam Karkas Utuh", "Ayam Potong 8", "Ayam Potong 12", "Fillet Dada",
				"Fillet Paha", "Sayap Ayam", "Ceker Ayam", "Kepala Ayam",
				"Ati Ampela", "Kulit Ayam", "Tulang Ayam" },
			new String[] { "Ayam Broiler", "Ayam Kampung", "Ayam Pejantan", "Ayam Petelur Afkir" },
			new String[] { "Per Ekor", "Per Kg", "Per 10 Ekor", "Per 100 Ekor" },
			"Paket", 2000.0, 200000.0);
		daftar("RPH", new String[] {
				"Jasa Potong Sapi", "Jasa Potong Kambing", "Jasa Potong Domba",
				"Jasa Potong Kerbau", "Daging Sapi Segar", "Daging Kambing Segar",
				"Iga Sapi", "Tetelan", "Jeroan Sapi", "Kikil", "Tulang Sup",
				"Kepala Kambing", "Kaki Sapi", "Jasa Cacah Daging", "Jasa Kemas Kurban" },
			new String[] { "Sapi Lokal", "Sapi Limousin", "Kambing Etawa", "Kambing Jawa", "Domba" },
			new String[] { "Per Ekor", "Per Kg", "Per Paket Kurban" },
			"Paket", 10000.0, 3000000.0);
		daftar("PETERNAKAN_UNGGAS", new String[] {
				"Telur Ayam Ras", "Telur Ayam Kampung", "Telur Bebek", "Telur Puyuh",
				"Telur Asin", "DOC Broiler", "DOC Layer", "DOD Bebek",
				"Ayam Kampung Hidup", "Bebek Hidup", "Puyuh Hidup", "Pupuk Kandang" },
			new String[] { "Grade A", "Grade B", "Super", "Fertil", "Konsumsi" },
			new String[] { "Per Butir", "Per Tray", "Per Kg", "Per Ekor", "Per Box 100" },
			"Isi", 500.0, 150000.0);
		daftar("JUAL_HEWAN_TERNAK", new String[] {
				"Sapi Limousin", "Sapi Simental", "Sapi Bali", "Sapi Madura",
				"Kambing Etawa", "Kambing Kacang", "Kambing Boer", "Domba Garut",
				"Domba Ekor Gemuk", "Kerbau", "Ayam Kampung", "Bebek Peking",
				"Entok", "Kelinci Pedaging" },
			new String[] { "Bakalan", "Siap Potong", "Indukan", "Anakan", "Kurban Grade A", "Kurban Grade B" },
			new String[] { "Per Ekor", "Per Pasang", "Bobot 100-200 kg", "Bobot 200-400 kg", "Bobot 400 kg Lebih" },
			"Paket", 50000.0, 20000000.0);
		daftar("PENGGILINGAN_PADI", new String[] {
				"Jasa Giling Gabah", "Jasa Selep Beras", "Jasa Poles Beras",
				"Beras Medium", "Beras Premium", "Beras Pecah Kulit", "Menir",
				"Bekatul", "Dedak Halus", "Dedak Kasar", "Sekam", "Sekam Bakar",
				"Jasa Jemur Gabah", "Jasa Angkut Gabah" },
			new String[] { "IR64", "Ciherang", "Pandan Wangi", "Rojolele", "Mentik" },
			new String[] { "Per Kg", "Per 25 kg", "Per 50 kg", "Per Kuintal", "Per Ton" },
			"Paket", 500.0, 900000.0);
		daftar("PERIKANAN", new String[] {
				"Ikan Lele Konsumsi", "Ikan Nila", "Ikan Gurame", "Ikan Patin",
				"Ikan Mas", "Ikan Bawal", "Udang Vaname", "Bibit Lele",
				"Bibit Nila", "Bibit Gurame", "Benur Udang", "Pakan Apung",
				"Jasa Panen", "Jasa Sortir Ikan" },
			new String[] { "Ukuran 5-7 cm", "Ukuran 7-9 cm", "Ukuran Konsumsi", "Indukan", "Super" },
			new String[] { "Per Kg", "Per Ekor", "Per 1000 Ekor", "Per Kolam" },
			"Paket", 1000.0, 500000.0);

		// --------------------------------- Lainnya ---------------------------------
		daftar("PENGINAPAN_KOS", new String[] {
				"Kamar Standar", "Kamar Deluxe", "Kamar Family", "Kamar AC",
				"Kamar Non-AC", "Kos Putra", "Kos Putri", "Kos Pasutri",
				"Extra Bed", "Laundry Kamar", "Parkir Mobil", "Sewa Aula",
				"Paket Bulanan Kos", "Deposit Kunci" },
			new String[] { "Kamar Mandi Dalam", "Kamar Mandi Luar", "Termasuk Sarapan",
				"Termasuk Listrik", "WiFi" },
			new String[] { "Per Malam", "Per Minggu", "Per Bulan", "Per Tahun" },
			"Paket", 25000.0, 3000000.0);
		daftar("AGEN_GAS_GALON", new String[] {
				"Gas LPG 3 kg", "Gas LPG 5.5 kg", "Gas LPG 12 kg", "Tabung 3 kg Baru",
				"Tabung 12 kg Baru", "Galon Isi Ulang", "Galon Baru Plus Isi",
				"Air Mineral Dus", "Regulator Gas", "Selang Gas", "Antar Gas", "Antar Galon" },
			new String[] { "Isi Ulang", "Tukar Tabung", "Tabung Plus Isi", "Antar Gratis" },
			new String[] { "Per Tabung", "Per Galon", "Per Dus", "Per Paket" },
			"Paket", 5000.0, 400000.0);
		daftar("AGEN_PEMBAYARAN", new String[] {
				"Token Listrik", "Bayar Tagihan Listrik", "Bayar PDAM", "Bayar BPJS",
				"Bayar Internet", "Top Up e-Wallet", "Top Up DANA", "Top Up OVO",
				"Top Up GoPay", "Transfer Bank", "Tarik Tunai", "Bayar Cicilan",
				"Bayar TV Kabel", "Isi Saldo Game", "Bayar Pajak Kendaraan", "Bayar Telepon" },
			new String[] { "20.000", "50.000", "100.000", "200.000", "500.000", "Nominal Bebas" },
			new String[] { "Per Transaksi", "Per Bulan", "Per Voucher" },
			"Paket", 1000.0, 500000.0);
	}

	/** Kelompok jenis produk per GRUP unit usaha -- nama JenisProduk = "label unit - kelompok". */
	public static String[] kelompokJenis(String grup) {
		if (UnitUsahaKatalog.GRUP_KULINER.equals(grup)) {
			return new String[] { "Makanan", "Minuman", "Paket", "Menu Tambahan" };
		}
		if (UnitUsahaKatalog.GRUP_OTOMOTIF.equals(grup)) {
			return new String[] { "Jasa", "Suku Cadang", "Oli dan Cairan", "Paket Servis" };
		}
		if (UnitUsahaKatalog.GRUP_JASA.equals(grup)) {
			return new String[] { "Layanan Utama", "Layanan Ekspres", "Paket Langganan", "Tambahan" };
		}
		if (UnitUsahaKatalog.GRUP_AGRI.equals(grup)) {
			return new String[] { "Hasil Utama", "Hasil Sampingan", "Jasa", "Bibit dan Indukan" };
		}
		if (UnitUsahaKatalog.GRUP_LAINNYA.equals(grup)) {
			return new String[] { "Layanan Utama", "Produk", "Paket", "Tambahan" };
		}
		return new String[] { "Barang Utama", "Barang Pelengkap", "Paket Hemat", "Promo" };
	}

	/** Katalog cadangan bila kode belum punya entri (fail-safe utk kode baru). */
	private static Katalog generik(String kodeUnit) {
		String label = UnitUsahaKatalog.labelDari(kodeUnit);
		return new Katalog(
				new String[] { "Produk " + label, "Layanan " + label, "Paket " + label },
				PAKET_LAYANAN,
				new String[] { "Kecil", "Sedang", "Besar", "Per Paket" },
				"Seri", 1000.0, 500000.0);
	}

	public static Katalog untuk(String kodeUnit) {
		Katalog k = PETA.get(kodeUnit);
		return k == null ? generik(kodeUnit) : k;
	}

	/**
	 * Nama produk ke-{@code nomor} (1-based) utk unit usaha {@code kodeUnit}. Deterministik
	 * dan unik utk nomor berapa pun: penghitung ekor tidak dimodulo.
	 */
	public static String namaProduk(String kodeUnit, int nomor) {
		Katalog k = untuk(kodeUnit);
		int dasar = Math.max(0, nomor - 1);
		int nd = k.dasar.length, nv = k.variasi.length, ns = k.satuan.length;
		String nama = k.dasar[dasar % nd] + " "
				+ k.variasi[(dasar / nd) % nv] + " "
				+ k.satuan[(dasar / (nd * nv)) % ns];
		int ekor = 1 + (dasar / (nd * nv * ns));
		return ekor == 1 ? nama : nama + " " + k.sufiks + " " + ekor;
	}

	/** Harga beli (HPP) deterministik dalam rentang wajar unit usaha ybs. */
	public static double hargaBeli(String kodeUnit, int nomor) {
		Katalog k = untuk(kodeUnit);
		return k.beliMin + ((nomor * 137) % (long) k.beliRentang);
	}
}
