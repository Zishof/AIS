package ais.action.master.employ.util;

import java.util.TreeMap;

/**
 * Registri statis metadata field formulir biodata {@code Pegawai}: {@link #DATA} berisi seluruh
 * nama properti Hibernate yang tersedia untuk ditampilkan/dikonfigurasi pada form biodata pegawai
 * (mulai identitas dasar sampai atasan jabatan), {@link #DATA_DESC} berisi label tampilan
 * berbahasa Indonesia untuk tiap properti tersebut (indeks array bersesuaian satu-ke-satu dengan
 * {@link #DATA}), dan {@link #MAPPING_DATA} adalah peta gabungan {@code properti -> label} yang
 * dibangun otomatis di blok inisialisasi statis kelas (dipakai layar yang perlu menerjemahkan nama
 * field ke label tanpa menelusuri kedua array secara manual). {@link #DEFAULT_TIDAK_AKTIF} adalah
 * daftar nama field yang secara default disembunyikan/dinonaktifkan pada form biodata standar
 * (mis. field spesifik PNS seperti KARIS/ASKES/TASPEN/KARPEG dan pangkat/jabatan/koordinat).
 *
 * <p>
 * Kelas ini murni wadah konstanta (tidak ada instance state maupun method non-statis); konsumennya
 * mengiterasi {@link #DATA}/{@link #DATA_DESC} atau melihat {@link #MAPPING_DATA} untuk membangun
 * UI konfigurasi kolom formulir biodata pegawai secara dinamis.
 * </p>
 */
public class FormBiodataPegawaiUtil {

	/** Nama field yang secara default TIDAK aktif/ditampilkan pada form biodata pegawai standar. */
	public final static String[] DEFAULT_TIDAK_AKTIF = new String[] { "karis",

			"askes",

			"taspen",

			"karpeg", "pangkat", "jabatan", "lintang", "bujur", "spesialisasi1", "spesialisasi2", "spesialisasi3" };

	/** Seluruh nama properti Hibernate {@code Pegawai} yang tersedia untuk form biodata, sejajar indeks dengan {@link #DATA_DESC}. */
	public final static String[] DATA = new String[] { "id", "mycode", "code", "idfinger", "ktp", "nama",

			"statusPegawai",

			"statusKepegawaian",

			"sertifikasi", "pendidikan",

			"jenisTenagaKependidikan",

			"statusPerkawinan",

			"kelamin",

			"tempatlahir",

			"tanggallahir",

			"bahasa",

			"telp",

			"telpDarurat",

			"namaDarurat",

			"statusDarurat",

			"email",

			"golonganDarah",

			"nomorKartuKeluarga",

			"namaIbuKandung",

			"agama",

			"bank",

			"norek",

			"bank2",

			"norek2",

			"bank3",

			"norek3",

			"karis",

			"askes",

			"taspen",

			"karpeg",

			"npwp",

			"alamat",

			"alamatJalan",

			"alamatKelurahan",

			"alamatKecamatan",

			"alamatKabupaten",

			"alamatPropinsi",

			"lintang",

			"bujur",

			"keteranganBadanTinggi",

			"keteranganBadanBerat",

			"keteranganBadanRambut",

			"keteranganBadanBentukMuka",

			"keteranganBadanWarnaKulit",

			"keteranganBadanCiriKhas",

			"keteranganBadanCacat",

			"hobi",

			"keterangan",

			"spesialisasi1",

			"spesialisasi2",

			"spesialisasi3",

			"tipeMasaKerja",

			"tanggalMulaiPengalanKerja",

			"tanggalSampaiPengalanKerja",

			"tanggalmasukHonorer",

			"tanggalkeluarHonorer",

			"tanggalmasukSemiTetap",

			"tanggalkeluarSemiTetap",

			"tanggalmasuk", "tanggalkeluar",

			"asuransiPegawai1", "asuransiPegawai2", "asuransiPegawai3", "asuransiPegawai4",

			"nomorAsuransiPegawai1", "nomorAsuransiPegawai2", "nomorAsuransiPegawai3", "nomorAsuransiPegawai4",

			"tipePegawai", "unitKerja", "masaKerja", "ptkpPegawai", "pangkat", "jabatan", "satuanKerja",
			"jatahCutiTahunan", "usiaPensiun", "ikatanKerjaDosen", "tendikSekolah", "tendikJurusan", "tendikFakultas",
			"atasan", "atasanPendukung", "atasanPendukungCadangan", "atasanlangsung"

	};

	/** Label tampilan berbahasa Indonesia untuk tiap properti pada {@link #DATA}, sejajar indeks. */
	public final static String[] DATA_DESC = new String[] { "ID", "NIP", "NIP (PNS)", "Kode Fingerprint", "No. KTP",
			"Nama Lengkap",

			"Status Keaktifan Pegawai",

			"Status Kepegawaian",

			"Sertifikasi", "Pendidikan Terakhir",

			"Jenis Tenaga Pendidikan", "Status Perkawainan",

			"Jenis Kelamin",

			"Tempat Lahir",

			"Tanggal Lahir",

			"Bahasa",

			"Telpon",

			"Telpon Darurat",

			"Nama Pemilik Telpon Darurat",

			"Status Pemilik Telpon Darurat",

			"Email",

			"Golongan Darah",

			"Nomor Kartu Keluarga",

			"Nama Ibu Kandung",

			"Agama",

			"Bank Utama",

			"No. Rekening Utama",

			"Bank ke-2",

			"No. Rekening ke-2",

			"Bank ke-3",

			"No. Rekening ke-3",

			"KARIS",

			"ASKES",

			"TASPEN",

			"KARPEG",

			"NPWP",

			"Alamat",

			"Alamat/Jalan",

			"Kelurahan / Desa",

			"Kecamatan",

			"Kabupaten / Kota",

			"Propinsi",

			"Lintang",

			"Bujur",

			"Tinggi Badan (cm)",

			"Berat Badan (kg)",

			"Bentuk Rambut",

			"Bentuk muka",

			"Warna kulit",

			"Ciri-ciri khas",

			"Cacat tubuh",

			"Hobi",

			"Keterangan",

			"Spesialisasi I",

			"Spesialisasi II",

			"Spesialisasi III",

			"Tipe Masa Kerja Berlangsung",

			"Tanggal Terhitung Pengalaman Bekerja",

			"Tanggal Sampai Pengalaman Bekerja",

			"Tanggal Terhitung Pegawai Honorer",

			"Tanggal Sampai Pegawai Honorer",

			"Tanggal Terhitung Pegawai Semi Tetap",

			"Tanggal Sampai Pegawai Semi Tetap",

			"Tanggal Terhitung Pegawai Tetap",

			"Tanggal Sampai Pegawai Tetap",

			"Asuransi Pegawai I", "Asuransi Pegawai II", "Asuransi Pegawai III",

			"Asuransi Pegawai IV",

			"Nomor Asuransi Pegawai I", "Nomor Asuransi Pegawai II", "Nomor Asuransi Pegawai III",
			"Nomor Asuransi Pegawai IV",

			"Tipe Pegawai", "Unit Kerja", "Masa Kerja", "PTKP Pegawai", "Pangkat", "Deskripsi Jabatan", "Satuan Kerja",
			"Jumlah Cuti Tahunan", "Usia Pensiun", "Ikatan Kerja", "Tendik Sekolah", "Tendik Prodi", "Tendik Fakultas",
			"Atasan Jabatan Utama", "Atasan Jabatan Pendukung", "Atasan Jabatan Pendukung Cadangan", "Atasan Langsung"

	};

	/** Peta {@code nama properti -> label tampilan}, dibangun otomatis dari {@link #DATA}/{@link #DATA_DESC} di blok statis kelas ini. */
	public final static TreeMap<String, String> MAPPING_DATA = new TreeMap<String, String>();

	/** Mengisi {@link #MAPPING_DATA} dari pasangan {@link #DATA}/{@link #DATA_DESC}; ketidakcocokan indeks per elemen dicatat ke log tanpa menghentikan proses. */
	static {
		for (int i = 0; i < DATA.length; i++) {
			try {
				System.out.println(DATA[i] + "=" + DATA_DESC[i]);
				MAPPING_DATA.put(DATA[i], DATA_DESC[i]);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/employ/util/FormBiodataPegawaiUtil.java:267");
				System.out.println("error key " + DATA[i]);
			}
		}

		// System.out.println("MAPPING_DATA => " + MAPPING_DATA);
	}
}
