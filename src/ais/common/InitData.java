package ais.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ais.database.model.*;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.CaraPembayaranTransfer;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.Devisi;
import ais.database.model.akunting.GrupAkun;
import ais.database.model.akunting.JenisKasBesar;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.asset.CaraPengadaanAsset;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.JenisPekerjaanPenyedia;
import ais.database.model.asset.JenisPembayaranBarang;
import ais.database.model.asset.JenisPemesananPengadaanAsset;
import ais.database.model.asset.JenisPenerimaanBarang;
import ais.database.model.asset.JenisPengapusanBarang;
import ais.database.model.asset.JenisPenyediaAsset;
import ais.database.model.asset.KategoriAsset;
import ais.database.model.asset.KategoriPenyediaAsset;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.SatuanMasterAsset;
import ais.database.model.asset.StatusAsset;
import ais.database.model.asset.StatusPenyediaAsset;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.HukumanPegawai;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.JenisCutiDanIzin;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.employ.JenisKenaikanPangkat;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.MasaKerja;
import ais.database.model.employ.PelanggaranDanHukumanPegawai;
import ais.database.model.employ.PelanggaranPegawai;
import ais.database.model.employ.Pendidikan;
import ais.database.model.employ.Peraturan;
import ais.database.model.employ.SkorGolongan;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.employ.TipePegawai;
import ais.database.model.employ.Transport;
import ais.database.model.employ.UnitGolongan;
import ais.database.model.employ.UnitKerja;
import ais.database.model.epsbed.EpsbedJabatanAkademik;
import ais.database.model.epsbed.EpsbedStatusAktivitasDosen;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.KomponenPenilaianKkn;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.AturanDiskon;
import ais.database.model.koperasi.CalonAnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.JenisIdentitasAnggotaKoperasi;
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.KelompokParameterTambahanProdukKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.MejaKantin;
import ais.database.model.koperasi.ParameterTambahanProdukKoperasi;
import ais.database.model.koperasi.PengurusKoperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.SyaratProdukKoperasi;
import ais.database.model.koperasi.TipeAnggotaKoperasi;
import ais.database.model.koperasi.TipeProdukKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.database.model.kpi.FormatKpi;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.KategoriKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.MasaKpi;
import ais.database.model.kpi.NilaiDefaultKpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.kpi.PengajuanKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.kpi.SatuanKpi;
import ais.database.model.kpi.SkorKpi;
import ais.database.model.kursus.BeritaKursus;
import ais.database.model.kursus.KategoriProdukKursus;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.database.model.kursus.TingkatKelasProdukKursus;
import ais.database.model.library.Anggota;
import ais.database.model.library.DendaKeterlambatanItem;
import ais.database.model.library.Item;
import ais.database.model.library.JenisAnggota;
import ais.database.model.library.JenisIdentitasAnggota;
import ais.database.model.library.JenisItem;
import ais.database.model.library.KodeTransaksi;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeAnggota;
import ais.database.model.library.TipeItem;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KelompokParameterTambahanKegiatan;
import ais.database.model.lkp.SatuanKegiatanTugasJabatan;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.KategoriCpl;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfesiLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;
import ais.database.model.payroll.AdjusVariablePenggajian;
import ais.database.model.payroll.AsuransiPegawai;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.GajiTabahan;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.JatahCuti;
import ais.database.model.payroll.JenisGajiPegawai;
import ais.database.model.payroll.JenisPengajuanTransaksiPegawai;
import ais.database.model.payroll.JenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.database.model.payroll.KelompokItemGaji;
import ais.database.model.payroll.KelompokParameterTambahanGajiPegawai;
import ais.database.model.payroll.KelompokParameterTambahanPengajuanTransaksiPegawai;
import ais.database.model.payroll.KodeTunjangan;
import ais.database.model.payroll.LevelJabatan;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.payroll.LiburRutin;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranItemGajiPegawai;
import ais.database.model.payroll.PtkpPegawai;
import ais.database.model.payroll.RencanaGaji;
import ais.database.model.payroll.WaktuShift;
import ais.database.model.penelitiandanpengabdian.JenisPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.penelitiandanpengabdian.TahapanPenyusunanArtikel;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.KomponenPenilaianPkl;
import ais.database.model.rab.Indikator;
import ais.database.model.rab.JenisWorkspace;
import ais.database.model.rab.Kppn;
import ais.database.model.rab.MetodePengadaan;
import ais.database.model.rab.Mitra;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.Satuan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SatuanLokasi;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.UnitOrganisasi;
import ais.database.model.rab.Workspace;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.VerifikasiKelengkapanCalonPegawai;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.AlatTransportasiSiswa;
import ais.database.model.sekolah.AngketPenilaianGuru;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.DetailKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.FormatNis;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.GuruMengajar;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JabatanKegiatanKesiswaan;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPertemuanPSB;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.JenisAktiftasHarianDefault;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.JenisGuru;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisJadwalPelajaran;
import ais.database.model.sekolah.JenisMateriHarianDefault;
import ais.database.model.sekolah.JenisNilaiHuruf;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.JenisRaporSiswa;
import ais.database.model.sekolah.JenisSekolah;
import ais.database.model.sekolah.JenisTinggalSiswa;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KebutuhanKhususSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KelompokGelombang;
import ais.database.model.sekolah.KelompokJamPelajaran;
import ais.database.model.sekolah.KelompokKegiatanKesiswaan;
import ais.database.model.sekolah.KelompokMatapelajaran;
import ais.database.model.sekolah.KelompokParameterTambahanCalonSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.KelompokPendaftaranPsb;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.MasaJadwalPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PaketPsbPunyaGelombangPendaftaranPsb;
import ais.database.model.sekolah.PekerjaanOrtuSiswa;
import ais.database.model.sekolah.Pelanggaran;
import ais.database.model.sekolah.PelanggaranDanHukuman;
import ais.database.model.sekolah.PembinaSiswa;
import ais.database.model.sekolah.PendidikanOrangTuaSiswa;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.PenghasilanOrangTuaSiswa;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.PenugasanGuruMengajar;
import ais.database.model.sekolah.RuangPSB;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.SkalaKegiatanKesiswaan;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sekolah.StatusKeluarSiswa;
import ais.database.model.sekolah.SubMatapelajaran;
import ais.database.model.sekolah.UjianPSB;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sisdes.Penduduk;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.JenisSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.Sop;
import ais.database.model.sosial.Donatur;
import ais.database.model.sosial.GelombangDonatur;
import ais.database.model.sosial.KategoriProgramDonatur;
import ais.database.model.sosial.ProgramDonatur;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.HasilSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.KelompokNomorSurat;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;
import ais.database.model.surat.KlasifikasiSuratMasuk;
import ais.database.model.surat.LokerSurat;
import ais.database.model.surat.MasaBerlakuSurat;
import ais.database.model.surat.NomorSurat;
import ais.database.model.surat.SifatSurat;
import ais.database.model.surat.StatusDipertahankan;
import ais.database.model.surat.VariableSuratKeluar;
import ais.ui.util.WaktuUtil;

public class InitData {

	public static volatile boolean init = false;
	private static volatile boolean running = false;
	private static volatile long lastStartedMillis = 0L;
	private static volatile long lastFinishedMillis = 0L;

	// Instance Executor Service
	public static ExecutorService executor;

	private static final Object LOCK_INIT = new Object();

	public static boolean isRunning() {
		return running;
	}

	public static boolean isInitialized() {
		return init;
	}

	public static long getLastStartedMillis() {
		return lastStartedMillis;
	}

	public static long getLastFinishedMillis() {
		return lastFinishedMillis;
	}

	public static void resetInitFlag() {
		synchronized (LOCK_INIT) {
			if (!running) {
				init = false;
				ConstantValues.udahSelesai = false;
				ConstantValues.selesaiInit = false;
			}
		}
	}

	public static void initData(boolean paksa) {
		synchronized (LOCK_INIT) {
			if (running) {
				return;
			}
			if (init && !paksa) {
				return;
			}

			running = true;
			lastStartedMillis = System.currentTimeMillis();
			init = true;
			ConstantValues.udahSelesai = false;
			boolean isSuccess = false;

			if (executor == null || executor.isShutdown() || executor.isTerminated()) {
				executor = Executors.newFixedThreadPool(5);
			}

			try {
				doInitData();
				isSuccess = true;
			} catch (Exception e) {
				System.err.println("Gagal memproses inisialisasi data: " + e.getMessage());
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/InitData.java:351");
				init = false;
			} finally {
				tutupExecutor();
				running = false;
				lastFinishedMillis = System.currentTimeMillis();

				if (isSuccess) {
					System.out.println("---------------------------------------------------");
					System.out.println("Semua data telah selesai diproses.");
					ConstantValues.udahSelesai = true;
					ConstantValues.selesaiInit = true;
				} else {
					ConstantValues.udahSelesai = false;
				}
			}
		}
	}

	/**
	 * Method untuk menangani penutupan Executor Service dengan aman Memisahkan
	 * logika agar catch-block tidak menumpuk di method utama
	 */
	private static void tutupExecutor() {
		if (executor != null && !executor.isTerminated()) {
			executor.shutdown();
			int awaitDetik = 600;
			try {
				awaitDetik = Integer.parseInt(
						Common.getKonfigurasi("await_init_executor_detik", "600").getNilai().trim());
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/InitData.java:381");
			}
			System.out.println("tutupExecutor: menunggu semua task init selesai (maks " + awaitDetik + " detik)...");
			try {
				if (!executor.awaitTermination(awaitDetik, TimeUnit.SECONDS)) {
					System.out.println("tutupExecutor: BATAS " + awaitDetik
							+ " detik tercapai — ada task init MENGGANTUNG; lanjut START aplikasi (shutdownNow). "
							+ "Data terkait akan dimuat on-demand.");
					executor.shutdownNow();
				} else {
					System.out.println("tutupExecutor: semua task init selesai.");
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt(); // Maintain interrupt status (Java 1.6 compatible)
			} catch (Exception e) {
				// Menangkap error lain jika ada, memastikan shutdown dipaksa
				executor.shutdownNow();
			}
		}
	}

	/**
	 * Helper method efisien untuk mengeksekusi inisialisasi secara dinamis
	 */
	private static void initClasses(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			InitDataHelper.initData(clazz);
		}
	}

	private static void doInitData() {

		WaktuUtil.reinit();

		// Kolom pada TABEL UTAMA diserahkan sepenuhnya ke Hibernate (hbm2ddl.auto=update).
		// Yang diselaraskan di sini HANYA tabel audit Envers (new_audit.*__audit), karena mode
		// update tidak selalu menambahkan kolom baru ke schema audit — lihat catatan di
		// hibernate.cfg.xml: tabel audit yang ketinggalan kolom membuat INSERT audit gagal
		// sehingga penyimpanan entity ber-@Audited ikut rollback (data tidak tersimpan).
		AturanCutiSchemaFix.initKolomAudit();
		KursusSchemaFix.initKolomBaru();
		SirsSchemaFix.initKolomBaru();
		DiskonMahasiswaSchemaFix.initKolomBaru();
		// FIX SQLGrammarException/GenericJDBCException "column ... jenis_seleksi_target does not
		// exist": kolom pada KelompokCalonMahasiswa yang belum ter-propagate ke semua instalasi.
		KelompokCalonMahasiswaSchemaFix.initKolomBaru();
		// Snapshot hasil akhir nilai harus tersedia sebelum migrasi/backfill nilai
		// dijalankan. Berlaku untuk tabel utama dan audit Envers.
		NilaiKunciSchemaFix.initKolomSnapshot();

		// Kolom teks yang diisi OTOMATIS oleh AuditTimestampInterceptor (oleh/olehid, berlaku utk
		// SEMUA entitas) bisa jauh melebihi varchar(255) default JPA -> INSERT gagal "value too
		// long for type character varying(255)" (pernah terjadi di virtual_account_bank, kini juga
		// di log_host_to_host). Cek+lebarkan ke text di sini SEKALI di awal startup, idempoten
		// (aman dijalankan berkali-kali; hanya alter bila kolom belum text).
		DatabaseTextColumnSchemaFix.initTextColumns();

		LiburNasional.reInitLiburNasional();

		/*
		 * initMaster wajib selesai terlebih dahulu sebelum init class lain berjalan.
		 *
		 * Penyebab: - initMaster mengisi banyak ConstantValues dari entity Hibernate. -
		 * Jika berjalan paralel dengan initData(Class), entity/collection yang sama
		 * bisa tersentuh dua session berbeda. - Pada Hibernate 3.6 hal ini dapat
		 * memicu: Illegal attempt to associate a collection with two open sessions.
		 */
		InitDataHelper.initMaster();

		executor.submit(new Runnable() {
			@Override
			public void run() {
				System.out.println("TASK-A MULAI: JenisTransaksi.reinitDefault + initJumlahTahapan");
				JenisTransaksi.reinitDefault();
				ConstantValues.initJumlahTahapan();
				System.out.println("TASK-A SELESAI");
			}
		});

		executor.submit(new Runnable() {
			@Override
			public void run() {
				System.out.println("TASK-B MULAI: initDataKegiatanKemahasiswan");
				InitDataHelper.initDataKegiatanKemahasiswan();
				System.out.println("TASK-B: initDataKegiatanKedosenan");
				InitDataHelper.initDataKegiatanKedosenan();
				System.out.println("TASK-B SELESAI");
			}
		});

		executor.submit(new Runnable() {
			@Override
			public void run() {
				System.out.println("TASK-C MULAI: initPembobotanNilai");
				ConstantValues.initPembobotanNilai();
				System.out.println("TASK-C SELESAI");
			}
		});

		// Eksekusi initClasses secara sequential (sesuai urutan aslinya agar menjaga
		// dependency)
		initClasses(Tbmuser.class, Dosen.class, Mahasiswa.class, Siswa.class, CalonSiswa.class, Guru.class,
				Tbmrole.class, BiodataMahasiswa.class, BiodataDosen.class, BiodataCalonMahasiswa.class,
				PendaftaranCutiMahasiswa.class, GolonganPns.class, KebutuhanKhususSiswa.class, PerguruanTinggi.class,
				PerguruanTinggiLain.class, Fakultas.class, Jenjang.class, JenjangProgramStudi.class, GrupJurusan.class,
				Jurusan.class, Kurikulum.class, Devisi.class, JenisTransaksi.class, Mitra.class, Satuan.class,
				SumberDana.class, JenisWorkspace.class, UnitOrganisasi.class, MetodePengadaan.class, JenisSekolah.class,
				KomponenPenilaianKkn.class, KomponenPenilaianPkl.class, OrganisasiDosen.class,
				JabatanOrganisasiDosen.class, JenisCapaianJurusan.class, JenisAktfitasMahasiswa.class,
				CustomerService.class, ProgramMahasiswa.class, TipePenelitianDanPengabdian.class,
				JenisPenelitianDanPengabdian.class, GrupAkun.class, MasterGrupLaporan.class, JenisLaporan.class,
				KalenderAkademik.class, KonfigurasiKalenderAkademik.class, Hukuman.class, Pelanggaran.class,
				PelanggaranDanHukuman.class, KlasifikasiSuratKeluarUntuk.class, HukumanPegawai.class,
				PelanggaranPegawai.class, PelanggaranDanHukumanPegawai.class, JenisBiayaSekolah.class,
				StatusAsset.class, KanalPembayaran.class, JenisKegiatanMahasiswa.class, StatusMatakuliah.class,
				TipeItem.class, Akreditasi.class, BlacklistIp.class, TingkatKelasProdukKursus.class,
				KategoriProdukKursus.class, Workspace.class, ParameterTambahan.class, ParameterTambahanMahasiswa.class,
				ParameterTambahanPengaduan.class, RolePrivilage.class, KelompokJenisSeleksi.class, Toko.class,
				JenisProduk.class, Produk.class, PembinaSiswa.class, JenisTabungan.class, PenugasanGuruMengajar.class,
				JenisTransaksiKoperasi.class, SubMatapelajaran.class, JenisJadwalPelajaran.class,
				KelompokJamPelajaran.class, MasterAsset.class, JenisAsset.class, KelompokAsset.class,
				SatuanMasterAsset.class, Anggota.class);

		initClasses(JenisPembayaranBarang.class, ProfesiLulusan.class, ProfilLulusan.class, ReferensiLulusan.class,
				KategoriCpl.class,
				CapaianLulusan.class, PembayaranItemGajiPegawai.class, CapaianPembelajaranLulusan.class,
				BahanKajian.class, JenisPengajuanPegawai.class, GrupParameterTambahan.class,
				KelompokParameterTambahanKegiatan.class, KelompokParameterTambahanCalonSiswa.class,
				KelompokParameterTambahanAlumni.class, KelompokParameterTambahanMahasiswa.class,
				KelompokParameterTambahanCalonMahasiswa.class, KelompokParameterTambahanPertemuan.class,
				ParameterTambahanAlumni.class, MahasiswaJadiAsisten.class, GrupChecklistPenilaianGuru.class,
				ChecklistPenilaianGuru.class, AngketPenilaianGuru.class, JenisPajakPpn.class);

		initClasses(JenisPekerjaanPenyedia.class, DiskonMahasiswa.class, JenisDiskonMahasiswa.class,
				JenisShiftPegawai.class, DetailJenisShiftPegawai.class, PaketPunyaGelombangPendaftaran.class,
				JenisRaporSiswa.class, JenisGuru.class, JenisPengaduan.class, DokumenAkreditasi.class,
				CalonAnggotaKoperasi.class, SatuanKerjaPegawai.class, AngketPenilaianUmum.class,
				AngketPenilaianDosen.class, GrupChecklistPenilaianDosen.class, ChecklistPenilaianDosen.class,
				GrupChecklistPenilaianUmum.class, JadwalChecklistPenilaianUmum.class, JurnalPenelitian.class,
				TahapanPenyusunanArtikel.class, ChecklistPenilaianUmum.class, JamPelajaran.class, Matapelajaran.class,
				KurikulumPunyaMatapelajaran.class, KurikulumSekolah.class, MasaJadwalPelajaran.class,
				JadwalPelajaran.class, KelompokParameterTambahanAlurSop.class, AktorSop.class,
				AkunPembayaranSiswa.class, LokerSurat.class, KelompokGelombang.class,
				KomponenPenilaianProposalSkripsi.class, Menu.class, KategoriAsset.class, JenisFormulirKegiatan.class,
				GrupFormulirKegiatan.class, Insentif.class, Makan.class, Transport.class, BlokirMahasiswa.class,
				KelompokKegiatanKemahasiswaan.class, DetailKelompokKegiatanKemahasiswaan.class,
				SkalaKegiatanKemahasiswaan.class, JabatanKegiatanKemahasiswaan.class, KelompokKegiatanKesiswaan.class,
				DetailKelompokKegiatanKesiswaan.class, SkalaKegiatanKesiswaan.class, JabatanKegiatanKesiswaan.class,
				CabangPrestasiMahasiswa.class, KategoriPrestasiMahasiswa.class, TargetKerjaPegawai.class,
				KelompokKegiatanKedosenan.class, DetailKelompokKegiatanKedosenan.class, JabatanKegiatanKedosenan.class,
				SkalaKegiatanKedosenan.class, KomponenProdukKursus.class, OperatorSeluler.class, Kelas.class,
				OrangTua.class, Penduduk.class, Closing.class, KelompokNomorSurat.class, NomorSurat.class,
				KategoriPengumuman.class, PengumumanPerkuliahan.class, AturanDiskon.class,
				JenisAktiftasHarianDefault.class, JenisMateriHarianDefault.class);

		initClasses(BeritaKursus.class, DetailGrupKategoriItemPenilaianSiswa.class,
				GrupKategoriItemPenilaianSiswa.class, Kppn.class, KelompokMahasiswa.class, SatuanLokasi.class,
				KelasSiswa.class, KelasSiswaPunyaSiswa.class, KelasLesSiswa.class, RuangPSB.class,
				KelasLesSiswaPunyaSiswa.class, JenisCutiDanIzin.class, AsramaSiswa.class, AsramaSiswaPunyaSiswa.class,
				JenisCatatanSiswa.class);

		initClasses(NomorSuratAlurKeuangan.class, JenisAnggotaKoperasi.class, JenisIdentitasAnggotaKoperasi.class,
				Koperasi.class, AnggotaKoperasi.class, TipeAnggotaKoperasi.class, PengurusKoperasi.class,
				TipeProdukKoperasi.class, ProdukKoperasi.class, MejaKantin.class, DynamicReport.class);

		initClasses(KelompokParameterTambahanProdukKoperasi.class, ParameterTambahanProdukKoperasi.class,
				KelasPmb.class, KomponenPenilaianProposalSkripsi.class, KomponenPenilaianSkripsi.class,
				SyaratProdukKoperasi.class, Keluarga.class, UjianPSB.class, Yayasan.class, Sekolah.class,
				SatuanKerja.class, Konsentrasi.class, Judisium.class, PekerjaanOrangTua.class, Pekerjaan.class,
				PendidikanOrangTua.class, Penghasilan.class, Pendidikan.class, JabatanStruktural.class, GajiPokok.class,
				Bank.class, Cabang.class, Departemen.class, LevelJabatan.class, FormatItemGaji.class,
				JenisTenagaKependidikan.class, StatusKerjasamaMahasiswa.class, JenisKartuIdentitasMahasiswaBaru.class,
				JurusanSekolahMahasiswaBaru.class, JenisJabatan.class, GajiTabahan.class, ItemGajiPegawai.class,
				ItemGaji.class, FormatKpi.class, JenisPajakBarang.class, KategoriPenghargaan.class, PengajuanKpi.class,
				AdjusVariablePenggajian.class, PenjurusanSekolah.class, KlasifikasiSuratKeluarParemeter.class,
				JenisKelompokStatusMahasiswa.class, KelompokStatusMahasiswa.class, CaraPembayaranKoperasi.class,
				GrupKuesionerUmum.class, KlasifikasiSuratKeluar.class, KlasifikasiSuratMasuk.class,
				AlurPersetujuanSuratKeluar.class, AlurPersetujuanSuratMasuk.class, Pejabat.class,
				PenjelasanBankSoal.class, CaraPengadaanAsset.class, JenisLayananKepadaMahasiswa.class,
				JenisShiftPunyaPegawai.class, KelompokItemGaji.class, Pegawai.class, SettingBiayaDetail.class,
				JenisNilaiHuruf.class, KelompokMatapelajaran.class, JenisPenilaian.class, JenisItemPenilaianSiswa.class,
				DetailJenisPenilaian.class, JenisItem.class, Item.class, BankHost.class, Konstanta.class,
				KategoriItemPenilaianSiswa.class, GelombangPendaftaranPegawai.class, PembombotanNilai.class,
				UjianPMB.class, RuangPMB.class, PesertaKursus.class, KategoriBankSoal.class, FormulirKegiatan.class,
				JatahCuti.class, JenisSop.class, Sop.class, AlurSop.class, DokumenAlurSop.class, DisposisiSop.class,
				DisposisiAlurSop.class, TransaksiKoperasi.class, TransaksiKoperasiDetail.class,
				JenisPenyediaAsset.class, Prefix.class, Wilayah.class, Propinsi.class, Kota.class,
				NamaSekolahAsal.class, PendapatanOrangTua.class, JenisSekolahMahasiswaBaru.class,
				PilihanPaketPerJurusanMhsBaru.class, JenisTinggalMahasiswa.class, AlatTransportasiMahasiswa.class,
				JenisTinggalSiswa.class, Statusabsensi.class, Ujian.class, PertemuanPunyaUjian.class,
				PengumumanAkademis.class, PaketPerkuliahan.class, AfiliasiCalonMahasiswa.class, Perkuliahan.class,
				JadwalUjianPMB.class, MahasiswaRequestTugasAkhir.class, Skripsi.class, Asrama.class, JenisAnggota.class,
				Perpustakaan.class, JenisIdentitasAnggota.class, TipeAnggota.class, TextBerjalan.class, Wisuda.class,
				Agama.class, LiburNasional.class, LiburRutin.class, JenisPengajuan.class, JenisNilaiHuruf.class,
				GelombangDonatur.class, KategoriProgramDonatur.class, ProgramDonatur.class, Kpi.class, ItemKpi.class,
				SatuanKpi.class, KategoriKpi.class, MasaKpi.class, InterviewCalonMahasiswa.class, SkorKpi.class,
				SkorGolongan.class, TipePegawai.class, TipeMasaKerja.class, MasaKerja.class, JenisGajiPegawai.class,
				DiskonSiswa.class, DiskonSiswaPunyaSiswa.class, KelompokMatakuliah.class, Matakuliah.class,
				MatakuliahPrasyarat.class, GrupPenilaian.class, PaketPsb.class,
				PaketPsbPunyaGelombangPendaftaranPsb.class, JenisTransaksiPegawai.class, JenisPembayaran.class,
				JenisKegiatan.class, JenisSeleksi.class, StatusKeluar.class, StatusMahasiswa.class,
				StatusAwalMahasiswa.class, JenisPembiayaanMahasiswa.class, SettingBiaya.class, DetailSettingBiaya.class,
				WaktuShift.class, SubGrupChecklistPenilaianUmum.class, UnitKerja.class, SumberGaji.class,
				StatusPegawai.class, StatusKewajibanBebanDosen.class, StatusKepegawaian.class, LembagaPengangkat.class,
				JenisPendidikDanTenagaKependidikan.class, IkatanKerjaDosen.class, ItemBiaya.class, Akun.class,
				VariableSuratKeluar.class, ItemBiayaSekolah.class, JenisEvaluasi.class, JamPerkuliahan.class,
				Pedagang.class, MasaPerkuliahan.class);

		initClasses(JenisPengapusanBarang.class, Donatur.class, JenisKerjasama.class, KodeTransaksi.class,
				TingkatKesulitanMatakuliah.class, Gedung.class, Ruang.class, Negara.class, UnitGolongan.class,
				ais.database.model.employ.Golongan.class, JabatanFungsionalDosen.class, Jabatan.class,
				JabatanFungsional.class, Peraturan.class, JenisKenaikanPangkat.class, PenilaianKpi.class,
				NilaiDefaultKpi.class, NilaiKpi.class, FormatKpiDetail.class, JenisUangMuka.class, JenisKasBesar.class,
				JenisKasKecil.class, KelompokPendaftaranPsb.class, Pendaftar.class, KenaikanPangkat.class,
				MatakuliahEkivalen.class, JadwalPembayaran.class, Paket.class, GelombangPendaftaran.class,
				GelombangPendaftaranPsb.class, MatapelajaranSekolah.class, PaketJurusanPmb.class,
				PaketPunyaProgram.class, TanyaJawab.class, JenisPengajuanTransaksiPegawai.class,
				KelompokParameterTambahanPengajuanTransaksiPegawai.class, StatusPertemuan.class,
				EpsbedJabatanAkademik.class, EpsbedStatusAktivitasDosen.class, UploadBiodataCalonMahasiswa.class,
				KelompokCalonMahasiswa.class, KelompokParameterTambahanCatatanSiswa.class,
				KelompokParameterTambahanCatatanAdministrasi.class, JenisCatatanAdministrasi.class,
				PembatasanNilaiIPKUntukPengambilanKRS.class, PtkpPegawai.class, AsuransiPegawai.class,
				DendaKeterlambatanItem.class, MasaBerlakuSurat.class, StatusDipertahankan.class, SifatSurat.class,
				Kkn.class, Pkl.class, KelompokKkn.class, KelompokPkl.class, PembombotanNilai.class,
				KurikulumPunyaMatakuliah.class, JadwalSidangTugasAkhir.class, FormatNilaiSkripsi.class,
				GelombangPendaftaranSidangTugasAkhir.class, FormatNilaiProposalSkripsi.class,
				TahapanAtauCapaianPembelajaran.class, JadwalSeminarTugasAkhir.class, Statusabsensi.class,
				KerjasamaAntarInstansi.class, StatusDomisiliSetelahLulus.class, StatusPekerjaanSetelahLulus.class,
				StatusSetelahLulus.class, JenisPenerimaBeasiswa.class, GuruMengajar.class,
				VerifikasiKelengkapanCalonMahasiswa.class, VerifikasiKelengkapanCalonSiswa.class,
				KomponenDataProdukKursus.class, ProdukKursus.class, SyaratUjian.class, Sertifikat.class,
				SatuanKegiatanTugasJabatan.class, KegiatanTugasJabatan.class, Indikator.class, StandarSPMI.class,
				ButirMutuSPMI.class, IndikatorSPMI.class, SkenarioSPMI.class, HasilSPMI.class, JenisSPMI.class,
				Beasiswa.class, BankSoal.class, BankSoalDetail.class, UjianPunyaSoal.class, JadwalPertemuanPSB.class,
				SubReport.class, PendukungReport.class, Lokasi.class, PemilikAsset.class, PenyediaAsset.class,
				KelompokStatusKeluarMahasiswa.class, KategoriPenyediaAsset.class, StatusPenyediaAsset.class,
				DokumenPenyediaAsset.class, VerifikasiKelengkapanCalonPegawai.class, CalonPegawai.class,
				PengaturanBiayaItemBiaya.class, PengaturanBiaya.class, KelompokParameterTambahanGajiPegawai.class,
				FormatNis.class);

		initClasses(JenisPenerimaanBarang.class, JenisPengeluaranMahasiswa.class, CaraPembayaranGaji.class,
				CaraPembayaranTransfer.class, JenisPemesananPengadaanAsset.class, RencanaGaji.class,
				PembayaranGaji.class, PendidikanOrangTuaSiswa.class, PenghasilanOrangTuaSiswa.class,
				AlatTransportasiSiswa.class, JenisTinggalSiswa.class, StatusAwalSiswa.class, StatusKeluarSiswa.class,
				PekerjaanOrtuSiswa.class, KodeTunjangan.class, NomorSuratAlurPengadaan.class,
				JenisKegiatanPrasyarat.class);

		// Template SOP AMI dibuat setelah master AktorSop/JenisSop/Sop/AlurSop selesai
		// dimuat. Helper idempoten: bila SOP sudah ada, tidak ada data yang diubah.
		AmiSopWorkflowTemplate.ensureCreated();

		// Eksekusi Logika Reload Defaults
		reloadDefaults();
	}

	/**
	 * Memisahkan kumpulan reload function agar body utama doInitData() bersih dan
	 * fokus.
	 */
	private static void reloadDefaults() {
		// 3. Masukkan tugas (Runnable) ke dalam executor
		InitData.executor.submit(new Runnable() {
			@Override
			public void run() {
				System.out.println("reloadDefaults: KebutuhanKhususSiswa ..."); KebutuhanKhususSiswa.reloadDefault();
				System.out.println("reloadDefaults: JenisPembayaranBarang ..."); JenisPembayaranBarang.reloadDefault();
				System.out.println("reloadDefaults: JenisPekerjaanPenyedia ..."); JenisPekerjaanPenyedia.reloadDefault();
				System.out.println("reloadDefaults: KategoriProdukKursus ..."); KategoriProdukKursus.reloadDefault();
				System.out.println("reloadDefaults: BeritaKursus ..."); BeritaKursus.reloadDefault();
				System.out.println("reloadDefaults: JenisPajakPpn ..."); JenisPajakPpn.reloadDefault();
				System.out.println("reloadDefaults: NomorSuratAlurKeuangan ..."); NomorSuratAlurKeuangan.reloadDefault();
				System.out.println("reloadDefaults: KelompokParameterTambahanProdukKoperasi ..."); KelompokParameterTambahanProdukKoperasi.checkCreateDefault();
				System.out.println("reloadDefaults: JenisPengapusanBarang ..."); JenisPengapusanBarang.reloadDefault();
				System.out.println("reloadDefaults: InitSirs.initData ..."); InitSirs.initData();
				System.out.println("reloadDefaults: JenisPenerimaanBarang ..."); JenisPenerimaanBarang.reloadDefault();
				System.out.println("reloadDefaults: JenisPengeluaranMahasiswa ..."); JenisPengeluaranMahasiswa.reloadDefault();
				System.out.println("reloadDefaults: CaraPembayaranGaji ..."); CaraPembayaranGaji.reloadDefault();
				System.out.println("reloadDefaults: CaraPembayaranTransfer ..."); CaraPembayaranTransfer.reloadDefault();
				System.out.println("reloadDefaults: JenisPembayaran ..."); JenisPembayaran.reloadDefault();
				System.out.println("reloadDefaults: JenisTabungan ..."); JenisTabungan.reloadDefault();
				System.out.println("reloadDefaults: JenisLaporan ..."); JenisLaporan.reloadDefault();
				System.out.println("reloadDefaults: MasterGrupLaporan ..."); MasterGrupLaporan.reloadDefault();
				System.out.println("reloadDefaults: KategoriPengumuman ..."); KategoriPengumuman.reloadDefault();
				System.out.println("reloadDefaults: JenisKasKecil ..."); JenisKasKecil.reloadDefault();
				System.out.println("reloadDefaults: JenisUangMuka ..."); JenisUangMuka.reloadDefault();
				System.out.println("reloadDefaults: JenisPenyediaAsset ..."); JenisPenyediaAsset.reloadDefault();
				System.out.println("reloadDefaults: KategoriPenyediaAsset ..."); KategoriPenyediaAsset.reloadDefault();
				System.out.println("reloadDefaults: StatusPenyediaAsset ..."); StatusPenyediaAsset.reloadDefault();
				System.out.println("reloadDefaults: DokumenPenyediaAsset ..."); DokumenPenyediaAsset.reloadDefault();
				System.out.println("reloadDefaults: VerifikasiKelengkapanCalonPegawai ..."); VerifikasiKelengkapanCalonPegawai.reloadDefault();
				System.out.println("reloadDefaults: ItemGaji.reloadKelompokItemGaji ..."); ItemGaji.reloadKelompokItemGaji();
				System.out.println("reloadDefaults: KodeTunjangan ..."); KodeTunjangan.reloadDefault();
				System.out.println("reloadDefaults: NomorSuratAlurPengadaan ..."); NomorSuratAlurPengadaan.reloadDefault();
				System.out.println("reloadDefaults: SELESAI");
			}
		});
	}
}
