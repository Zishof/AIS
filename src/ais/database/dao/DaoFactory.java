package ais.database.dao;

import ais.database.dao.akunting.AkunDao;
import ais.database.dao.akunting.AkunPajakDao;
import ais.database.dao.akunting.DevisiDao;
import ais.database.dao.akunting.GrupAkunDao;
import ais.database.dao.akunting.GrupTransaksiDao;
import ais.database.dao.akunting.JenisLaporanDao;
import ais.database.dao.akunting.JenisTransaksiDao;
import ais.database.dao.akunting.KelompokLaporanDao;
import ais.database.dao.akunting.KelompokLaporanPunyaAkunDao;
import ais.database.dao.akunting.MasterGrupLaporanDao;
import ais.database.dao.akunting.MataUangDao;
import ais.database.dao.akunting.TransaksiDao;
import ais.database.dao.asset.AssetDao;
import ais.database.dao.asset.JenisAssetDao;
import ais.database.dao.asset.KelompokAssetDao;
import ais.database.dao.asset.LokasiDao;
import ais.database.dao.asset.PemilikAssetDao;
import ais.database.dao.asset.StatusAssetDao;
import ais.database.dao.beasiswa.BeasiswaPunyaPersyaratanDao;
import ais.database.dao.beasiswa.MahasiswaBeasiswaPersyaratanDao;
import ais.database.dao.beasiswa.MahasiswaDaftarBeasiswaDao;
import ais.database.dao.beasiswa.PersyaratanBeasiswaDao;
import ais.database.dao.employ.DaftarNilaiPelaksanaanPekerjaanDao;
import ais.database.dao.employ.DiklatDao;
import ais.database.dao.employ.GajiPokokDao;
import ais.database.dao.employ.GolonganDao;
import ais.database.dao.employ.JabatanFungsionalDao;
import ais.database.dao.employ.JabatanFungsionalTambahanDao;
import ais.database.dao.employ.JabatanStrukturalDao;
import ais.database.dao.employ.JenisDiklatDao;
import ais.database.dao.employ.JenisJabatanDao;
import ais.database.dao.employ.JenisKegiatanEmployDao;
import ais.database.dao.employ.JenisPelatihanDao;
import ais.database.dao.employ.JenisPensiunDao;
import ais.database.dao.employ.JenisPimpinanDao;
import ais.database.dao.employ.JenisTandaJasaDao;
import ais.database.dao.employ.KeluargaPegawaiDao;
import ais.database.dao.employ.KenaikanGajiBerkalaDao;
import ais.database.dao.employ.KenaikanPangkatDao;
import ais.database.dao.employ.KonfigurasiSKDao;
import ais.database.dao.employ.MutasiPindahDao;
import ais.database.dao.employ.PendidikanDao;
import ais.database.dao.employ.PensiunDao;
import ais.database.dao.employ.PeraturanDao;
import ais.database.dao.employ.RiwayatPendidikanPegawaiDao;
import ais.database.dao.employ.RiwayatStatusKepegawaianDao;
import ais.database.dao.employ.UnitKerjaDao;
import ais.database.dao.kedokteran.JenisPertemuanDao;
import ais.database.dao.kedokteran.PHDHasMahasiswaDao;
import ais.database.dao.kedokteran.PertemuanHasDosenDao;
import ais.database.dao.library.AnggotaDao;
import ais.database.dao.library.BatasWaktuPeminjamanItemDao;
import ais.database.dao.library.BiayaPendaftaranAnggotaDao;
import ais.database.dao.library.DataDdcItemDao;
import ais.database.dao.library.DataUdcItemDao;
import ais.database.dao.library.DdcItemDao;
import ais.database.dao.library.DendaKeterlambatanItemDao;
import ais.database.dao.library.DomainPenelitianDao;
import ais.database.dao.library.InformasiPerpustakaanDao;
import ais.database.dao.library.ItemDao;
import ais.database.dao.library.JenisAnggotaDao;
import ais.database.dao.library.JenisIdentitasAnggotaDao;
import ais.database.dao.library.JenisInformasiPerpustakaanDao;
import ais.database.dao.library.JenisItemDao;
import ais.database.dao.library.KategoriItemDao;
import ais.database.dao.library.KembaliPengadaanItemDao;
import ais.database.dao.library.KembaliPengadaanItemDetailDao;
import ais.database.dao.library.KoreksiItemDao;
import ais.database.dao.library.KoreksiItemDetailDao;
import ais.database.dao.library.LabelItemDao;
import ais.database.dao.library.PemesananPengadaanItemDao;
import ais.database.dao.library.PemesananPengadaanItemDetailDao;
import ais.database.dao.library.PeminjamanPengadaanItemDao;
import ais.database.dao.library.PeminjamanPengadaanItemDetailDao;
import ais.database.dao.library.PenerbitDao;
import ais.database.dao.library.PenerimaanPengadaanItemDao;
import ais.database.dao.library.PenerimaanPengadaanItemDetailDao;
import ais.database.dao.library.PengarangDao;
import ais.database.dao.library.PenyediaDao;
import ais.database.dao.library.PermintaanPengadaanItemDao;
import ais.database.dao.library.PermintaanPengadaanItemDetailDao;
import ais.database.dao.library.PerpustakaanDao;
import ais.database.dao.library.PesananAnggotaDao;
import ais.database.dao.library.PustakawanDao;
import ais.database.dao.library.RakDao;
import ais.database.dao.library.RakDetailDao;
import ais.database.dao.library.ReturPengadaanItemDao;
import ais.database.dao.library.ReturPengadaanItemDetailDao;
import ais.database.dao.library.SaldoAwalDao;
import ais.database.dao.library.SaldoAwalDetailDao;
import ais.database.dao.library.StatusItemDao;
import ais.database.dao.library.TerimaPengadaanItemDao;
import ais.database.dao.library.TerimaPengadaanItemDetailDao;
import ais.database.dao.library.TipeAnggotaDao;
import ais.database.dao.library.TipeItemDao;
import ais.database.dao.library.TopikItemDao;
import ais.database.dao.library.TransferPengadaanItemDao;
import ais.database.dao.library.TransferPengadaanItemDetailDao;
import ais.database.dao.library.UdcItemDao;
import ais.database.dao.library.VersiDdcItemDao;
import ais.database.dao.rab.AcaraDao;
import ais.database.dao.rab.AcaraPunyaKendalaDao;
import ais.database.dao.rab.ChecklistLaporanDao;
import ais.database.dao.rab.ChecklistLaporanDetailDao;
import ais.database.dao.rab.ChecklistLaporanDetailDefaultDao;
import ais.database.dao.rab.HasilSatuanDao;
import ais.database.dao.rab.IndikatorDao;
import ais.database.dao.rab.InformasiRabDao;
import ais.database.dao.rab.JenisInformasiRabDao;
import ais.database.dao.rab.JenisParameterDao;
import ais.database.dao.rab.JenisTugasDao;
import ais.database.dao.rab.JenisWorkspaceDao;
import ais.database.dao.rab.KegiatanSatkerDao;
import ais.database.dao.rab.MetodePengadaanDao;
import ais.database.dao.rab.MitraDao;
import ais.database.dao.rab.OutputKegiatanDao;
import ais.database.dao.rab.PejabatDao;
import ais.database.dao.rab.ProyekDao;
import ais.database.dao.rab.RencanaDanRealisasiOutputKegiatanDao;
import ais.database.dao.rab.RenstraProgramDao;
import ais.database.dao.rab.RenstraProgramPunyaIndikatorDao;
import ais.database.dao.rab.SasaranDao;
import ais.database.dao.rab.SatuanDao;
import ais.database.dao.rab.SatuanKerjaDao;
import ais.database.dao.rab.SumberDanaDao;
import ais.database.dao.rab.TorDao;
import ais.database.dao.rab.TugasDao;
import ais.database.dao.rab.UnitOrganisasiDao;
import ais.database.dao.rab.WorkspaceDao;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarDao;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarStatusDao;
import ais.database.dao.surat.AlurPersetujuanSuratMasukDao;
import ais.database.dao.surat.AlurPersetujuanSuratMasukStatusDao;
import ais.database.dao.surat.KlasifikasiSuratKeluarDao;
import ais.database.dao.surat.KlasifikasiSuratKeluarUntukDao;
import ais.database.dao.surat.KlasifikasiSuratMasukDao;
import ais.database.dao.surat.KopSuratDao;
import ais.database.dao.surat.LokerSuratDao;
import ais.database.dao.surat.OpsiSuratKeluarDao;
import ais.database.dao.surat.OpsiSuratMasukDao;
import ais.database.dao.surat.PenyampaianSuratDao;
import ais.database.dao.surat.SifatSuratDao;
import ais.database.dao.surat.SuratKeluarDao;
import ais.database.dao.surat.SuratMasukDao;

public abstract class DaoFactory {

	private static DaoFactory instance;

	private static final String daoFactoryClassName = "ais.database.dao.HibernateDaoFactory";

	/**
	 * @return kembaliannya
	 */
	public static DaoFactory getInstance() {
		if (instance == null) {
			try {
				instance = (DaoFactory) Class.forName(daoFactoryClassName).newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate DAO factory class [" + daoFactoryClassName + "]", e);
			}
		}
		return instance;
	}

	/**
	 * @return kembaliannya
	 */
	public abstract TbmuserDao getTbmuserDao();

	public abstract DosenDao getDosenDao();

	public abstract PegawaiDao getPegawaiDao();

	public abstract MahasiswaDao getMahasiswaDao();

	public abstract MatakuliahDao getMatakuliahDao();

	public abstract ProgramDao getProgramDao();

	public abstract PerkuliahanDao getPerkuliahanDao();

	public abstract BeasiswaDao getBeasiswaDao();

	public abstract BeasiswaPunyaItemBiayaTambahanDao getBeasiswaPunyaItemBiayaTambahanDao();

	public abstract StatusKerjasamaMahasiswaDao getStatusKerjasamaMahasiswaDao();

	public abstract MahasiswaDapatBeasiswaDao getMahasiswaDapatBeasiswaDao();

	public abstract MahasiswaDapatStatusKerjasamaMahasiswaDao getMahasiswaDapatStatusKerjasamaMahasiswaDao();

	public abstract KknDao getKknDao();

	public abstract MahasiswaDapatKknDao getMahasiswaDapatKknDao();

	public abstract PklDao getPklDao();

	public abstract MahasiswaDapatPklDao getMahasiswaDapatPklDao();

	public abstract DetailperkuliahanDao getDetailperkuliahanDao();

	public abstract FormatNilaiDao getFormatNilaiDao();

	public abstract NilaiHurufDao getNilaiHurufDao();

	public abstract PertemuanDao getPertemuanDao();

	public abstract MatakuliahPrasyaratDao getMatakuliahPrasyaratDao();

	public abstract MatakuliahEkivalenDao getMatakuliahEkivalenDao();

	public abstract FakultasDao getFakultasDao();

	public abstract JurusanDao getJurusanDao();

	public abstract SkripsiDao getSkripsiDao();

	public abstract RuangDao getRuangDao();

	public abstract KonsentrasiDao getKonsentrasiDao();

	public abstract GedungDao getGedungDao(); // add wildan

	public abstract KomentarDao getKomentarDao(); // add wildan

	public abstract BadanHukumDao getBadanHukumDao(); // add wildan

	public abstract JenjangProgramStudiDao getJenjangProgramStudiDao();

	public abstract JenjangDao getJenjangDao();

	public abstract JenisKegiatanDao getJenisKegiatanDao();

	public abstract KalenderAkademikDao getKalenderAkademikDao(); // add wildan

	public abstract MatakuliahBerbayarDao getMatakuliahBerbayarDao(); // add
																		// wildan

	public abstract PembobotanNilaiDao getPembobotanNilaiDao();

	public abstract BankDao getBankDao();

	public abstract PerguruanTinggiDao getPerguruanTinggiDao();

	public abstract ItemBiayaDao getItemBiayaDao();

	public abstract JenisPembayaranDao getJenisPembayaranDao();

	public abstract DetailBiayaDao getDetailBiayaDao();

	// public abstract JenisBiayaDao getJenisBiayaDao(); // add wildan

	public abstract BiodataMahasiswaDao getBiodataMahasiswaDao(); // add wildan

	public abstract JenisSeleksiDao getJenisSeleksiDao();

	public abstract BiodataDosenDao getBiodataDosenDao(); // add wildan

	public abstract BiodataPegawaiDao getBiodataPegawaiDao();

	public abstract BiodataCalonMahasiswaDao getBiodataCalonMahasiswaDao(); // add

	// wildan

	public abstract RekeningDosenDao getRekeningDosenDao();

	public abstract PendaftaranWisudaDao getPendaftaranWisudaDao();

	public abstract NegaraDao getNegaraDao();

	public abstract WisudaDao getWisudaDao(); // add wildan

	public abstract PengumumanAkademisDao getPengumumanAkademisDao(); // add

	// wildan

	public abstract DiskusiPengumumanAkademisDao getDiskusiPengumumanAkademisDao(); // add

	// wildan

	public abstract LampiranPengumumanAkademisDao getLampiranPengumumanAkademisDao(); // add

	// wildan

	public abstract QuotaWisudaUntukFakultasDao getQuotaWisudaUntukFakultasDao();

	public abstract PropinsiDao getPropinsiDao(); // add wildan

	public abstract KotaDao getKotaDao(); // add wildan

	public abstract PaketRegistrasiMahasiswaDao getPaketRegistrasiMahasiswaDao(); // add

	// wildan

	public abstract PendaftaranSidangDao getPendaftaranSidangDao();

	public abstract AgamaDao getAgamaDao(); // add wildan

	public abstract PengajuanBeasiswaDao getPengajuanBeasiswaDao();

	public abstract UjianPMBDao getUjianPMBDao();

	public abstract KelasDao getKelasDao();

	public abstract KelompokMatakuliahDao getKelompokMatakuliahDao();

	public abstract StatusMatakuliahDao getStatusMatakuliahDao();

	public abstract BaypassPembayaranMahasiswaDao getBaypassPembayaranMahasiswaDao();

	public abstract PaketPerkuliahanDao getPaketPerkuliahanDao();

	public abstract TemplateQueryDao getTemplateQueryDao();

	public abstract MahasiswaRequestTugasAkhirMintaPembimbingDao getMahasiswaRequestTugasAkhirMintaPembimbingDao();

	public abstract MahasiswaRequestTugasAkhirDao getMahasiswaRequestTugasAkhirDao();

	public abstract JudisiumDao getJudisiumDao();

	public abstract StatusPegawaiDao getStatusPegawaiDao();

	public abstract InputPembayaranPunyaAkunDao getInputPembayaranPunyaAkunDao();

	public abstract MessageDao getMessageDao();

	public abstract PesanRuanganDao getPesanRuanganDao();

	public abstract KonfigurasiKalenderAkademikDao getKonfigurasiKalenderAkademikDao();

	public abstract TemplateSuratParameterDao getTemplateSuratParameterDao();

	public abstract FormatTemplateSuratDao getFormatTemplateSuratDao();

	public abstract TemplateSuratDao getTemplateSuratDao();

	public abstract JamPerkuliahanDao getJamPerkuliahanDao();

	public abstract KurikulumPunyaMatakuliahDetailDao getKurikulumPunyaMatakuliahDetailDao();

	public abstract TemplatePerkuliahanDetailDao getTemplatePerkuliahanDetailDao();

	public abstract TemplatePerkuliahanDao getTemplatePerkuliahanDao();

	public abstract NamaTugasKelompokPunyaMahasiswaDao getNamaTugasKelompokPunyaMahasiswaDao();

	public abstract NamaTugasKelompokDao getNamaTugasKelompokDao();

	public abstract TugasKelompokDao getTugasKelompokDao();

	public abstract PertemuanPunyaUjianDao getPertemuanPunyaUjianDao();

	public abstract PertemuanPunyaDiskusiDao getPertemuanPunyaDiskusiDao();

	public abstract MatakuliahPunyaBukuBahanAjarDao getMatakuliahPunyaBukuBahanAjarDao();

	public abstract BukuBahanAjarDao getBukuBahanAjarDao();

	public abstract UjianDao getUjianDao();

	public abstract BankSoalDao getBankSoalDao();

	public abstract BankSoalDetailDao getBankSoalDetailDao();

	public abstract PendaftaranCutiMahasiswaDao getPendaftaranCutiMahasiswaDao(); // add

	// wildan

	public abstract KonfigurasiDao getKonfigurasiDao(); // add wildan

	public abstract DendaPembayaranDao getDendaPembayaranDao(); // add wildan

	public abstract DendaPembayaranNominalDao getDendaPembayaranNominalDao();

	// wildan

	public abstract JadwalPembayaranDao getJadwalPembayaranDao(); // add wildan

	public abstract JabatanDao getJabatanDao(); // add wildan

	public abstract BankHostDao getBankHostDao(); // add wildan

	public abstract TbmroleDao getTbmroleDao(); // add wildan

	public abstract JenisKartuIdentitasMahasiswaBaruDao getJenisKartuIdentitasMahasiswaBaruDao(); // add

	// wildan

	public abstract JenisSekolahMahasiswaBaruDao getJenisSekolahMahasiswaBaruDao(); // add

	// wildan

	public abstract JurusanSekolahMahasiswaBaruDao getJurusanSekolahMahasiswaBaruDao(); // add

	// wildan

	public abstract PekerjaanOrangTuaDao getPekerjaanOrangTuaDao(); // add

	// wildan

	public abstract PendidikanOrangTuaDao getPendidikanOrangTuaDao(); // add

	// wildan

	public abstract PendapatanOrangTuaDao getPendapatanOrangTuaDao(); // add

	// wildan

	// public abstract DetailBiayaCalonMahasiswaDao
	// getDetailBiayaCalonMahasiswaDao();

	public abstract LogCsvFileUploadDao getLogCsvFileUploadDao(); // add wildan

	public abstract StatusLulusCalonMahasiswaDao getStatusLulusCalonMahasiswaDao(); // add

	// wildan

	public abstract KurikulumDao getKurikulumDao();

	public abstract AngkatanKurikulumDao getAngkatanKurikulumDao();

	public abstract DetailKegiatanDao getDetailKegiatanDao(); // add wildan

	public abstract JenisKegiatanDetailDao getJenisKegiatanDetailDao(); // add

	// wildan

	public abstract DetailJenisKegiatanDao getDetailJenisKegiatanDao();

	public abstract KegiatanDao getKegiatanDao(); // add wildan

	public abstract KurikulumPunyaMatakuliahDao getKurikulumPunyaMatakuliahDao();

	public abstract GambarFakultasDao getGambarFakultasDao();

	public abstract SettingBiayaDao getSettingBiayaDao();

	public abstract FormatNilaiSkripsiDao getFormatNilaiSkripsiDao();

	public abstract PembatasanNilaiIPKUntukPengambilanKRSDao getPembatasanNilaiIPKUntukPengambilanKRSDao();

	public abstract DetailSettingBiayaDao getDetailSettingBiayaDao();

	public abstract TimDosenDao gettTimDosenDao();

	public abstract KonversiDao getKonversiDao(); // add wildan

	public abstract MatakuliahAwalKonversiDao getMatakuliahAwalKonversiDao(); // add

	// wildan

	public abstract StaffDao getStaffDao();

	public abstract PrefixDao getPrefixDao();

	public abstract TingkatKesulitanMatakuliahDao getTingkatKesulitanMatakuliahDao();

	public abstract PaketDao getPaketDao();

	public abstract PilihanPaketPerJurusanDao getPilihanPaketPerJurusanDao();

	public abstract PaketJurusanPmbDao getPaketJurusanPmbDao();

	public abstract CekKesehatanDao getCekKesehatanDao();

	public abstract RuangPMBDao getRuangPMBDao();

	public abstract RuangPaketPMBDao getRuangPaketPMBDao();

	public abstract KapasitasMahasiswaBaruEpsbedDao getKapasitasMahasiswaBaruEpsbedDao();

	public abstract FasilitasAkademikJurusanDao getAkademikJurusanDao();

	public abstract PublikasiDosenDao getPublikasiDosenDao();

	public abstract RiwayatPendidikanDosenDao getRiwayatPendidikanDosenDao();

	public abstract NilaiToeflToaflDao getNilaiToeflToaflDao();

	public abstract AkunDao getAkunDao();

	public abstract JenisTransaksiDao getJenisTransaksiDao();

	public abstract DevisiDao getDevisiDao();

	public abstract GrupAkunDao getGrupAkunDao();

	public abstract GrupTransaksiDao getGrupTransaksiDao();

	public abstract JenisLaporanDao getJenisLaporanDao();

	public abstract KelompokLaporanDao getKelompokLaporanDao();

	public abstract KelompokLaporanPunyaAkunDao getKelompokLaporanPunyaAkunDao();

	public abstract MasterGrupLaporanDao getMasterGrupLaporanDao();

	public abstract MataUangDao getMataUangDao();

	public abstract AkunPajakDao getAkunPajakDao();

	public abstract TransaksiDao getTransaksiDao();

	public abstract SatuanDao getSatuanDao();

	public abstract ChecklistLaporanDao getChecklistLaporanDao();

	public abstract ChecklistLaporanDetailDao getChecklistLaporanDetailDao();

	public abstract ChecklistLaporanDetailDefaultDao getChecklistLaporanDetailDefaultDao();

	public abstract IndikatorDao getIndikatorDao();

	public abstract TorDao getTorDao();

	public abstract RencanaDanRealisasiOutputKegiatanDao getRencanaDanRealisasiOutputKegiatanDao();

	public abstract OutputKegiatanDao getOutputKegiatanDao();

	public abstract SasaranDao getSasaranDao();

	public abstract JenisParameterDao getJenisParameterDao();

	public abstract JenisTugasDao getJenisTugasDao();

	public abstract UnitOrganisasiDao getUnitOrganisasiDao();

	public abstract JenisWorkspaceDao getJenisWorkspaceDao();

	public abstract HasilSatuanDao getHasilSatuanDao();

	public abstract AcaraDao getAcaraDao();

	public abstract MetodePengadaanDao getMetodePengadaanDao();

	public abstract KegiatanSatkerDao getKegiatanSatkerDao();

	public abstract PejabatDao getPejabatDao();

	public abstract MitraDao getMitraDao();

	public abstract AcaraPunyaKendalaDao getAcaraPunyaKendalaDao();

	public abstract WorkspaceDao getWorkspaceDao();

	public abstract SatuanKerjaDao getSatuanKerjaDao();

	public abstract SumberDanaDao getSumberDanaDao();

	public abstract ProyekDao getProyekDao();

	public abstract TugasDao getTugasDao();

	public abstract JenisItemDao getJenisItemDao();

	public abstract BatasWaktuPeminjamanItemDao getBatasWaktuPeminjamanItemDao();

	public abstract DendaKeterlambatanItemDao getDendaKeterlambatanItemDao();

	public abstract BiayaPendaftaranAnggotaDao getBiayaPendaftaranAnggotaDao();

	public abstract TipeItemDao getTipeItemDao();

	public abstract KategoriItemDao getKategoriItemDao();

	public abstract JenisPelatihanDao getJenisPelatihanDao();

	public abstract DdcItemDao getDdcItemDao();

	public abstract UdcItemDao getUdcItemDao();

	public abstract JenisIdentitasAnggotaDao getJenisIdentitasAnggotaDao();

	public abstract ItemDao getItemDao();

	public abstract RakDao getRakDao();

	public abstract JenisAnggotaDao getJenisAnggotaDao();

	public abstract PesananAnggotaDao getPesananAnggotaDao();

	public abstract JenisInformasiPerpustakaanDao getJenisInformasiPerpustakaanDao();

	public abstract JenisInformasiRabDao getJenisInformasiRabDao();

	public abstract TipeAnggotaDao getTipeAnggotaDao();

	public abstract AnggotaDao getAnggotaDao();

	public abstract VersiDdcItemDao getVersiDdcItemDao();

	public abstract DomainPenelitianDao getDomainPenelitianDao();

	public abstract DataDdcItemDao getDataDdcItemDao();

	public abstract DataUdcItemDao getDataUdcItemDao();

	public abstract InformasiPerpustakaanDao getInformasiPerpustakaanDao();

	public abstract InformasiRabDao getInformasiRabDao();

	public abstract RenstraProgramPunyaIndikatorDao getRenstraProgramPunyaIndikatorDao();

	public abstract RenstraProgramDao getRenstraProgramDao();

	public abstract PenyediaDao getPenyediaDao();

	public abstract StatusItemDao getStatusItemDao();

	public abstract PengarangDao getPengarangDao();

	public abstract TopikItemDao getTopikItemDao();

	public abstract LabelItemDao getLabelItemDao();

	public abstract PenerbitDao getPenerbitDao();

	public abstract SaldoAwalDetailDao getSaldoAwalDetailDao();

	public abstract RakDetailDao getRakDetailDao();

	public abstract SaldoAwalDao getSaldoAwalDao();

	public abstract KoreksiItemDetailDao getKoreksiItemDetailDao();

	public abstract KoreksiItemDao getKoreksiItemDao();

	public abstract PermintaanPengadaanItemDetailDao getPermintaanPengadaanItemDetailDao();

	public abstract PermintaanPengadaanItemDao getPermintaanPengadaanItemDao();

	public abstract TransferPengadaanItemDetailDao getTransferPengadaanItemDetailDao();

	public abstract TransferPengadaanItemDao getTransferPengadaanItemDao();

	public abstract PeminjamanPengadaanItemDetailDao getPeminjamanPengadaanItemDetailDao();

	public abstract PeminjamanPengadaanItemDao getPeminjamanPengadaanItemDao();

	public abstract TerimaPengadaanItemDetailDao getTerimaPengadaanItemDetailDao();

	public abstract TerimaPengadaanItemDao getTerimaPengadaanItemDao();

	public abstract KembaliPengadaanItemDetailDao getKembaliPengadaanItemDetailDao();

	public abstract KembaliPengadaanItemDao getKembaliPengadaanItemDao();

	public abstract PemesananPengadaanItemDetailDao getPemesananPengadaanItemDetailDao();

	public abstract PemesananPengadaanItemDao getPemesananPengadaanItemDao();

	public abstract PenerimaanPengadaanItemDetailDao getPenerimaanPengadaanItemDetailDao();

	public abstract PenerimaanPengadaanItemDao getPenerimaanPengadaanItemDao();

	public abstract ReturPengadaanItemDetailDao getReturPengadaanItemDetailDao();

	public abstract ReturPengadaanItemDao getReturPengadaanItemDao();

	public abstract PerpustakaanDao getPerpustakaanDao();

	public abstract PustakawanDao getPustakawanDao();

	public abstract LokasiDao getLokasiDao();

	public abstract JenisAssetDao getJenisAssetDao();

	public abstract KelompokAssetDao getKelompokAssetDao();

	public abstract PemilikAssetDao getPemilikAssetDao();

	public abstract AssetDao getAssetDao();

	public abstract StatusAssetDao getStatusAssetDao();

	public abstract DaftarNilaiPelaksanaanPekerjaanDao getDaftarNilaiPelaksanaanPekerjaanDao();

	public abstract PendidikanDao getPendidikanDao();

	public abstract JabatanStrukturalDao getJabatanStrukturalDao();

	public abstract JabatanFungsionalDao getJabatanFungsionalDao();

	public abstract JenisJabatanDao getJenisJabatanDao();

	public abstract JenisTandaJasaDao getJenisTandaJasaDao();

	public abstract PeraturanDao getPeraturanDao();

	public abstract GolonganDao getGolonganDao();

	public abstract GajiPokokDao getGajiPokokDao();

	public abstract JenisPimpinanDao getJenisPimpinanDao();

	public abstract UnitKerjaDao getUnitKerjaDao();

	public abstract GrupChecklistPenilaianDosenDao getChecklistPenilaianDosenDao();

	public abstract ChecklistPenilaianDosenDao getCchecklistPenilaianDosenDao();

	public abstract RiwayatPendidikanPegawaiDao getRiwayatPendidikanPegawaiDao();

	public abstract JenisDiklatDao getJenisDiklatDao();

	public abstract RiwayatStatusKepegawaianDao getRiwayatStatusKepegawaianDao();

	public abstract JenisKegiatanEmployDao getJenisKegiatanEmployDao();

	public abstract DiklatDao getDiklatDao();

	public abstract JenisPensiunDao getJenisPensiunDao();

	public abstract KenaikanPangkatDao getKenaikanPangkatDao();

	public abstract KeluargaPegawaiDao getKeluargaPegawaiDao();

	public abstract PensiunDao getPensiunDao();

	public abstract KonfigurasiSKDao getKonfigurasiSKDao();

	public abstract PersyaratanBeasiswaDao getPersyaratanBeasiswaDao();

	public abstract BeasiswaPunyaPersyaratanDao getBeasiswaPunyaPersyaratanDao();

	public abstract MahasiswaDaftarBeasiswaDao getMahasiswaDaftarBeasiswaDao();

	public abstract MahasiswaBeasiswaPersyaratanDao getMahasiswaBeasiswaPersyaratanDao();

	public abstract SuratMasukDao getSuratMasukDao();

	public abstract SuratKeluarDao getSuratKeluarDao();

	public abstract SifatSuratDao getSifatSuratDao();

	public abstract KopSuratDao getKopSuratDao();

	public abstract AlurPersetujuanSuratKeluarStatusDao getAlurPersetujuanSuratKeluarStatusDao();

	public abstract AlurPersetujuanSuratKeluarDao getAlurPersetujuanSuratKeluarDao();

	public abstract AlurPersetujuanSuratMasukStatusDao getAlurPersetujuanSuratMasukStatusDao();

	public abstract AlurPersetujuanSuratMasukDao getAlurPersetujuanSuratMasukDao();

	public abstract LokerSuratDao getLokerSuratDao();

	public abstract PenyampaianSuratDao getPenyampaianSuratDao();

	public abstract OpsiSuratMasukDao getOpsiSuratMasukDao();

	public abstract OpsiSuratKeluarDao getOpsiSuratKeluarDao();

	public abstract KlasifikasiSuratKeluarDao getKlasifikasiSuratKeluarDao();

	public abstract KlasifikasiSuratMasukDao getKlasifikasiSuratMasukDao();

	public abstract KlasifikasiSuratKeluarUntukDao getKlasifikasiSuratKeluarUntukDao();

	public abstract JenisPertemuanDao getJenisPertemuanDao();

	public abstract ais.database.dao.kedokteran.PertemuanKedokteranDao getPertemuanKedokteranDao();

	public abstract PertemuanHasDosenDao getPertemuanHasDosenDao();

	public abstract PHDHasMahasiswaDao getPhdHasMahasiswaDao();

	public abstract MetaReportDao getMetaReportDao();

	public abstract JabatanFungsionalTambahanDao getJabatanFungsionalTambahanDao();

	public abstract KenaikanGajiBerkalaDao getKenaikanGajiBerkalaDao();

	public abstract MutasiPindahDao getMutasiPindahDao();

	public abstract ais.database.dao.employ.SatuanKerjaDao getSatuanKerjaEmployDao();
}
