package ais.database.model.sister;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry pemetaan endpoint SISTER -> entitas terstruktur ecampus. Dipakai mesin sinkronisasi untuk
 * merutekan tiap endpoint ke tabel domainnya (bukan JSON generik). Dihasilkan otomatis dari api_spec.yaml.
 */
public final class SisterEntitasRegistry {
	/** Kelas utilitas statis (hanya berisi peta &amp; method statis) — konstruktor privat mencegah instansiasi. */
	private SisterEntitasRegistry() {}
	/**
	 * Peta endpoint SISTER (path dasar, tanpa query string) -&gt; kelas entitas terstruktur tujuan.
	 * Diisi sekali pada blok statis di bawah; dibaca lewat {@link #kelas(String)}, tidak pernah ditulis
	 * di luar kelas ini. Endpoint yang tidak terdaftar di sini (mis. {@code referensi/lembaga_sertifikasi},
	 * {@code sertifikasi_dosen}) ditangani lewat jalur kode khusus di {@link ais.common.DataSisterApi},
	 * bukan lewat registry ini.
	 */
	private static final Map<String, Class<?>> PETA = new HashMap<String, Class<?>>();
	/** Inisialisasi satu kali seluruh pemetaan endpoint -&gt; kelas entitas (referensi, data_pribadi, tridharma, BKD). */
	static {
		PETA.put("referensi/agama", RefAgamaSister.class);
		PETA.put("referensi/bidang_studi", RefBidangStudiSister.class);
		PETA.put("referensi/bidang_usaha", RefBidangUsahaSister.class);
		PETA.put("referensi/dudi", RefDudiSister.class);
		PETA.put("referensi/gelar_akademik", RefGelarAkademikSister.class);
		PETA.put("referensi/golongan_pangkat", RefGolonganPangkatSister.class);
		PETA.put("referensi/ikatan_kerja", RefIkatanKerjaSister.class);
		PETA.put("referensi/jabatan_fungsional", RefJabatanFungsionalSister.class);
		PETA.put("referensi/jabatan_negara", RefJabatanNegaraSister.class);
		PETA.put("referensi/jabatan_tugas_tambahan", RefJabatanTugasTambahanSister.class);
		PETA.put("referensi/jenis_bahan_ajar", RefJenisBahanAjarSister.class);
		PETA.put("referensi/jenis_beasiswa", RefJenisBeasiswaSister.class);
		PETA.put("referensi/jenis_diklat", RefJenisDiklatSister.class);
		PETA.put("referensi/jenis_dokumen", RefJenisDokumenSister.class);
		PETA.put("referensi/jenis_keluar", RefJenisKeluarSister.class);
		PETA.put("referensi/jenis_kepanitiaan", RefJenisKepanitiaanSister.class);
		PETA.put("referensi/jenis_kesejahteraan", RefJenisKesejahteraanSister.class);
		PETA.put("referensi/jenis_pekerjaan", RefJenisPekerjaanSister.class);
		PETA.put("referensi/jenis_penghargaan", RefJenisPenghargaanSister.class);
		PETA.put("referensi/jenis_publikasi", RefJenisPublikasiSister.class);
		PETA.put("referensi/jenis_tes", RefJenisTesSister.class);
		PETA.put("referensi/jenis_tunjangan", RefJenisTunjanganSister.class);
		PETA.put("referensi/jenjang_pendidikan", RefJenjangPendidikanSister.class);
		PETA.put("referensi/kategori_capaian_luaran", RefKategoriCapaianLuaranSister.class);
		PETA.put("referensi/kelompok_bidang", RefKelompokBidangSister.class);
		PETA.put("referensi/media_publikasi", RefMediaPublikasiSister.class);
		PETA.put("referensi/negara", RefNegaraSister.class);
		PETA.put("referensi/perguruan_tinggi", RefPerguruanTinggiSister.class);
		PETA.put("referensi/profil_pt", RefProfilPtSister.class);
		PETA.put("referensi/sdm", RefSdmSister.class);
		PETA.put("referensi/semester", RefSemesterSister.class);
		PETA.put("referensi/skim_kegiatan", RefSkimKegiatanSister.class);
		PETA.put("referensi/status_kepegawaian", RefStatusKepegawaianSister.class);
		PETA.put("referensi/sumber_gaji", RefSumberGajiSister.class);
		PETA.put("referensi/tingkat_penghargaan", RefTingkatPenghargaanSister.class);
		PETA.put("referensi/unit_kerja", RefUnitKerjaSister.class);
		PETA.put("referensi/wilayah", RefWilayahSister.class);
		PETA.put("referensi/mahasiswa_pddikti", RefMahasiswaPddiktiSister.class);
		PETA.put("data_pribadi/profil", DpProfilSister.class);
		PETA.put("data_pribadi/kependudukan", DpKependudukanSister.class);
		PETA.put("data_pribadi/keluarga", DpKeluargaSister.class);
		PETA.put("data_pribadi/alamat", DpAlamatSister.class);
		PETA.put("data_pribadi/kepegawaian", DpKepegawaianSister.class);
		PETA.put("data_pribadi/lain", DpLainSister.class);
		PETA.put("data_pribadi/bidang_ilmu", DpBidangIlmuSister.class);
		PETA.put("pengajaran", TridPengajaranSister.class);
		PETA.put("bimbingan_mahasiswa", TridBimbinganMahasiswaSister.class);
		PETA.put("pengujian_mahasiswa", TridPengujianMahasiswaSister.class);
		PETA.put("anggota_profesi", TridAnggotaProfesiSister.class);
		PETA.put("detasering", TridDetaseringSister.class);
		PETA.put("orasi_ilmiah", TridOrasiIlmiahSister.class);
		PETA.put("bahan_ajar", TridBahanAjarSister.class);
		PETA.put("tugas_tambahan", TridTugasTambahanSister.class);
		PETA.put("pembicara", TridPembicaraSister.class);
		PETA.put("jabatan_struktural", TridJabatanStrukturalSister.class);
		PETA.put("pengelola_jurnal", TridPengelolaJurnalSister.class);
		PETA.put("penghargaan", TridPenghargaanSister.class);
		PETA.put("visiting_scientist", TridVisitingScientistSister.class);
		PETA.put("pendidikan_formal", TridPendidikanFormalSister.class);
		PETA.put("diklat", TridDiklatSister.class);
		PETA.put("riwayat_pekerjaan", TridRiwayatPekerjaanSister.class);
		PETA.put("sertifikasi_profesi", TridSertifikasiProfesiSister.class);
		PETA.put("nilai_tes", TridNilaiTesSister.class);
		PETA.put("beasiswa", TridBeasiswaSister.class);
		PETA.put("kesejahteraan", TridKesejahteraanSister.class);
		PETA.put("tunjangan", TridTunjanganSister.class);
		PETA.put("dokumen", TridDokumenSister.class);
		PETA.put("kolaborator_eksternal", TridKolaboratorEksternalSister.class);
		PETA.put("inpassing", TridInpassingSister.class);
		PETA.put("jabatan_fungsional", TridJabatanFungsionalSister.class);
		PETA.put("kepangkatan", TridKepangkatanSister.class);
		PETA.put("penugasan", TridPenugasanSister.class);
		PETA.put("bimbing_dosen", TridBimbingDosenSister.class);
		PETA.put("penelitian", TridPenelitianSister.class);
		PETA.put("publikasi", TridPublikasiSister.class);
		PETA.put("pengabdian", TridPengabdianSister.class);
		PETA.put("penunjang_lain", TridPenunjangLainSister.class);
		PETA.put("kekayaan_intelektual", TridKekayaanIntelektualSister.class);
		PETA.put("bkd/laporan_akhir_bkd", BkdLaporanAkhirBkdSister.class);
		PETA.put("bkd/pendidikan", BkdPendidikanSister.class);
		PETA.put("bkd/ajar", BkdAjarSister.class);
		PETA.put("bkd/tunjang", BkdTunjangSister.class);
		PETA.put("bkd/pengmas", BkdPengmasSister.class);
		PETA.put("bkd/penelitian", BkdPenelitianSister.class);
	}

	/** Kelas entitas untuk endpoint (base, tanpa query), atau null bila tak terpetakan (fallback DataSister). */
	public static Class<?> kelas(String base) {
		return base == null ? null : PETA.get(base);
	}
}
