package ais.database.dao;

import org.hibernate.Session;

import ais.database.dao.akunting.AkunDao;
import ais.database.dao.akunting.AkunDaoImpl;
import ais.database.dao.akunting.AkunPajakDao;
import ais.database.dao.akunting.AkunPajakDaoImpl;
import ais.database.dao.akunting.DevisiDao;
import ais.database.dao.akunting.DevisiDaoImpl;
import ais.database.dao.akunting.GrupAkunDao;
import ais.database.dao.akunting.GrupAkunDaoImpl;
import ais.database.dao.akunting.GrupTransaksiDao;
import ais.database.dao.akunting.GrupTransaksiDaoImpl;
import ais.database.dao.akunting.JenisLaporanDao;
import ais.database.dao.akunting.JenisLaporanDaoImpl;
import ais.database.dao.akunting.JenisTransaksiDao;
import ais.database.dao.akunting.JenisTransaksiDaoImpl;
import ais.database.dao.akunting.KelompokLaporanDao;
import ais.database.dao.akunting.KelompokLaporanDaoImpl;
import ais.database.dao.akunting.KelompokLaporanPunyaAkunDao;
import ais.database.dao.akunting.KelompokLaporanPunyaAkunDaoImpl;
import ais.database.dao.akunting.MasterGrupLaporanDao;
import ais.database.dao.akunting.MasterGrupLaporanDaoImpl;
import ais.database.dao.akunting.MataUangDao;
import ais.database.dao.akunting.MataUangDaoImpl;
import ais.database.dao.akunting.TransaksiDao;
import ais.database.dao.akunting.TransaksiDaoImpl;
import ais.database.dao.asset.AssetDao;
import ais.database.dao.asset.AssetDaoImpl;
import ais.database.dao.asset.JenisAssetDao;
import ais.database.dao.asset.JenisAssetDaoImpl;
import ais.database.dao.asset.KelompokAssetDao;
import ais.database.dao.asset.KelompokAssetDaoImpl;
import ais.database.dao.asset.LokasiDao;
import ais.database.dao.asset.LokasiDaoImpl;
import ais.database.dao.asset.PemilikAssetDao;
import ais.database.dao.asset.PemilikAssetDaoImpl;
import ais.database.dao.asset.StatusAssetDao;
import ais.database.dao.asset.StatusAssetDaoImpl;
import ais.database.dao.beasiswa.BeasiswaPunyaPersyaratanDao;
import ais.database.dao.beasiswa.BeasiswaPunyaPersyaratanDaoImpl;
import ais.database.dao.beasiswa.MahasiswaBeasiswaPersyaratanDao;
import ais.database.dao.beasiswa.MahasiswaBeasiswaPersyaratanDaoImpl;
import ais.database.dao.beasiswa.MahasiswaDaftarBeasiswaDao;
import ais.database.dao.beasiswa.MahasiswaDaftarBeasiswaDaoImpl;
import ais.database.dao.beasiswa.PersyaratanBeasiswaDao;
import ais.database.dao.beasiswa.PersyaratanBeasiswaDaoImpl;
import ais.database.dao.employ.DaftarNilaiPelaksanaanPekerjaanDao;
import ais.database.dao.employ.DaftarNilaiPelaksanaanPekerjaanDaoImpl;
import ais.database.dao.employ.DiklatDao;
import ais.database.dao.employ.DiklatDaoImpl;
import ais.database.dao.employ.GajiPokokDao;
import ais.database.dao.employ.GajiPokokDaoImpl;
import ais.database.dao.employ.GolonganDao;
import ais.database.dao.employ.GolonganDaoImpl;
import ais.database.dao.employ.JabatanFungsionalDao;
import ais.database.dao.employ.JabatanFungsionalDaoImpl;
import ais.database.dao.employ.JabatanFungsionalTambahanDao;
import ais.database.dao.employ.JabatanFungsionalTambahanDaoImpl;
import ais.database.dao.employ.JabatanStrukturalDao;
import ais.database.dao.employ.JabatanStrukturalDaoImpl;
import ais.database.dao.employ.JenisDiklatDao;
import ais.database.dao.employ.JenisDiklatDaoImpl;
import ais.database.dao.employ.JenisJabatanDao;
import ais.database.dao.employ.JenisJabatanDaoImpl;
import ais.database.dao.employ.JenisKegiatanEmployDao;
import ais.database.dao.employ.JenisKegiatanEmployDaoImpl;
import ais.database.dao.employ.JenisPelatihanDao;
import ais.database.dao.employ.JenisPelatihanDaoImpl;
import ais.database.dao.employ.JenisPensiunDao;
import ais.database.dao.employ.JenisPensiunDaoImpl;
import ais.database.dao.employ.JenisPimpinanDao;
import ais.database.dao.employ.JenisPimpinanDaoImpl;
import ais.database.dao.employ.JenisTandaJasaDao;
import ais.database.dao.employ.JenisTandaJasaDaoImpl;
import ais.database.dao.employ.KeluargaPegawaiDao;
import ais.database.dao.employ.KeluargaPegawaiDaoImpl;
import ais.database.dao.employ.KenaikanGajiBerkalaDao;
import ais.database.dao.employ.KenaikanGajiBerkalaDaoImpl;
import ais.database.dao.employ.KenaikanPangkatDao;
import ais.database.dao.employ.KenaikanPangkatDaoImpl;
import ais.database.dao.employ.KonfigurasiSKDao;
import ais.database.dao.employ.KonfigurasiSKDaoImpl;
import ais.database.dao.employ.MutasiPindahDao;
import ais.database.dao.employ.MutasiPindahDaoImpl;
import ais.database.dao.employ.PendidikanDao;
import ais.database.dao.employ.PendidikanDaoImpl;
import ais.database.dao.employ.PensiunDao;
import ais.database.dao.employ.PensiunDaoImpl;
import ais.database.dao.employ.PeraturanDao;
import ais.database.dao.employ.PeraturanDaoImpl;
import ais.database.dao.employ.RiwayatPendidikanPegawaiDao;
import ais.database.dao.employ.RiwayatPendidikanPegawaiDaoImpl;
import ais.database.dao.employ.RiwayatStatusKepegawaianDao;
import ais.database.dao.employ.RiwayatStatusKepegawaianDaoImpl;
import ais.database.dao.employ.UnitKerjaDao;
import ais.database.dao.employ.UnitKerjaDaoImpl;
import ais.database.dao.kedokteran.JenisPertemuanDao;
import ais.database.dao.kedokteran.JenisPertemuanDaoImpl;
import ais.database.dao.kedokteran.PHDHasMahasiswaDao;
import ais.database.dao.kedokteran.PHDHasMahasiswaDaoImpl;
import ais.database.dao.kedokteran.PertemuanHasDosenDao;
import ais.database.dao.kedokteran.PertemuanHasDosenDaoImpl;
import ais.database.dao.library.*;
import ais.database.dao.rab.AcaraDao;
import ais.database.dao.rab.AcaraDaoImpl;
import ais.database.dao.rab.AcaraPunyaKendalaDao;
import ais.database.dao.rab.AcaraPunyaKendalaDaoImpl;
import ais.database.dao.rab.ChecklistLaporanDao;
import ais.database.dao.rab.ChecklistLaporanDaoImpl;
import ais.database.dao.rab.ChecklistLaporanDetailDao;
import ais.database.dao.rab.ChecklistLaporanDetailDaoImpl;
import ais.database.dao.rab.ChecklistLaporanDetailDefaultDao;
import ais.database.dao.rab.ChecklistLaporanDetailDefaultDaoImpl;
import ais.database.dao.rab.HasilSatuanDao;
import ais.database.dao.rab.HasilSatuanDaoImpl;
import ais.database.dao.rab.IndikatorDao;
import ais.database.dao.rab.IndikatorDaoImpl;
import ais.database.dao.rab.InformasiRabDao;
import ais.database.dao.rab.InformasiRabDaoImpl;
import ais.database.dao.rab.JenisInformasiRabDao;
import ais.database.dao.rab.JenisInformasiRabDaoImpl;
import ais.database.dao.rab.JenisParameterDao;
import ais.database.dao.rab.JenisParameterDaoImpl;
import ais.database.dao.rab.JenisTugasDao;
import ais.database.dao.rab.JenisTugasDaoImpl;
import ais.database.dao.rab.JenisWorkspaceDao;
import ais.database.dao.rab.JenisWorkspaceDaoImpl;
import ais.database.dao.rab.KegiatanSatkerDao;
import ais.database.dao.rab.KegiatanSatkerDaoImpl;
import ais.database.dao.rab.MetodePengadaanDao;
import ais.database.dao.rab.MetodePengadaanDaoImpl;
import ais.database.dao.rab.MitraDao;
import ais.database.dao.rab.MitraDaoImpl;
import ais.database.dao.rab.OutputKegiatanDao;
import ais.database.dao.rab.OutputKegiatanDaoImpl;
import ais.database.dao.rab.PejabatDao;
import ais.database.dao.rab.PejabatDaoImpl;
import ais.database.dao.rab.ProyekDao;
import ais.database.dao.rab.ProyekDaoImpl;
import ais.database.dao.rab.RencanaDanRealisasiOutputKegiatanDao;
import ais.database.dao.rab.RencanaDanRealisasiOutputKegiatanDaoImpl;
import ais.database.dao.rab.RenstraProgramDao;
import ais.database.dao.rab.RenstraProgramDaoImpl;
import ais.database.dao.rab.RenstraProgramPunyaIndikatorDao;
import ais.database.dao.rab.RenstraProgramPunyaIndikatorDaoImpl;
import ais.database.dao.rab.SasaranDao;
import ais.database.dao.rab.SasaranDaoImpl;
import ais.database.dao.rab.SatuanDao;
import ais.database.dao.rab.SatuanDaoImpl;
import ais.database.dao.rab.SatuanKerjaDao;
import ais.database.dao.rab.SatuanKerjaDaoImpl;
import ais.database.dao.rab.SumberDanaDao;
import ais.database.dao.rab.SumberDanaDaoImpl;
import ais.database.dao.rab.TorDao;
import ais.database.dao.rab.TorDaoImpl;
import ais.database.dao.rab.TugasDao;
import ais.database.dao.rab.TugasDaoImpl;
import ais.database.dao.rab.UnitOrganisasiDao;
import ais.database.dao.rab.UnitOrganisasiDaoImpl;
import ais.database.dao.rab.WorkspaceDao;
import ais.database.dao.rab.WorkspaceDaoImpl;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarDao;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarDaoImpl;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarStatusDao;
import ais.database.dao.surat.AlurPersetujuanSuratKeluarStatusDaoImpl;
import ais.database.dao.surat.AlurPersetujuanSuratMasukDao;
import ais.database.dao.surat.AlurPersetujuanSuratMasukDaoImpl;
import ais.database.dao.surat.AlurPersetujuanSuratMasukStatusDao;
import ais.database.dao.surat.AlurPersetujuanSuratMasukStatusDaoImpl;
import ais.database.dao.surat.KlasifikasiSuratKeluarDao;
import ais.database.dao.surat.KlasifikasiSuratKeluarDaoImpl;
import ais.database.dao.surat.KlasifikasiSuratKeluarUntukDao;
import ais.database.dao.surat.KlasifikasiSuratKeluarUntukDaoImpl;
import ais.database.dao.surat.KlasifikasiSuratMasukDao;
import ais.database.dao.surat.KlasifikasiSuratMasukDaoImpl;
import ais.database.dao.surat.KopSuratDao;
import ais.database.dao.surat.KopSuratDaoImpl;
import ais.database.dao.surat.LokerSuratDao;
import ais.database.dao.surat.LokerSuratDaoImpl;
import ais.database.dao.surat.OpsiSuratKeluarDao;
import ais.database.dao.surat.OpsiSuratKeluarDaoImpl;
import ais.database.dao.surat.OpsiSuratMasukDao;
import ais.database.dao.surat.OpsiSuratMasukDaoImpl;
import ais.database.dao.surat.PenyampaianSuratDao;
import ais.database.dao.surat.PenyampaianSuratDaoImpl;
import ais.database.dao.surat.SifatSuratDao;
import ais.database.dao.surat.SifatSuratDaoImpl;
import ais.database.dao.surat.SuratKeluarDao;
import ais.database.dao.surat.SuratKeluarDaoImpl;
import ais.database.dao.surat.SuratMasukDao;
import ais.database.dao.surat.SuratMasukDaoImpl;
import ais.database.hibernate.HibernateUtil;

/**
 * Created by IntelliJ IDEA. User: Fauzi Date: Nov 20, 2008 Time: 12:43:24 PM To
 * change this template use File | Settings | File Templates.
 */
public class HibernateDaoFactory extends DaoFactory {

	/**
	 * @return kembaliannya
	 */
	public Session getCurrentSession() {
		return HibernateUtil.currentSession();
	}

	@Override
	public TbmuserDao getTbmuserDao() {
		return new TbmuserDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DosenDao getDosenDao() {
		return new DosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PegawaiDao getPegawaiDao() {
		return new PegawaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaDao getMahasiswaDao() {
		return new MahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MatakuliahDao getMatakuliahDao() {
		return new MatakuliahDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ProgramDao getProgramDao() {
		return new ProgramDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PerkuliahanDao getPerkuliahanDao() {
		return new PerkuliahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BeasiswaDao getBeasiswaDao() {
		return new BeasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BeasiswaPunyaItemBiayaTambahanDao getBeasiswaPunyaItemBiayaTambahanDao() {
		return new BeasiswaPunyaItemBiayaTambahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StatusKerjasamaMahasiswaDao getStatusKerjasamaMahasiswaDao() {
		return new StatusKerjasamaMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaDapatBeasiswaDao getMahasiswaDapatBeasiswaDao() {
		return new MahasiswaDapatBeasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaDapatStatusKerjasamaMahasiswaDao getMahasiswaDapatStatusKerjasamaMahasiswaDao() {
		return new MahasiswaDapatStatusKerjasamaMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KknDao getKknDao() {
		return new KknDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaDapatKknDao getMahasiswaDapatKknDao() {
		return new MahasiswaDapatKknDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PklDao getPklDao() {
		return new PklDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaDapatPklDao getMahasiswaDapatPklDao() {
		return new MahasiswaDapatPklDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DetailperkuliahanDao getDetailperkuliahanDao() {
		return new DetailperkuliahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public FormatNilaiDao getFormatNilaiDao() {
		return new FormatNilaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public NilaiHurufDao getNilaiHurufDao() {
		return new NilaiHurufDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PertemuanDao getPertemuanDao() {
		return new PertemuanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MatakuliahPrasyaratDao getMatakuliahPrasyaratDao() {
		return new MatakuliahPrasyaratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MatakuliahEkivalenDao getMatakuliahEkivalenDao() {
		return new MatakuliahEkivalenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public FakultasDao getFakultasDao() {
		return new FakultasDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JurusanDao getJurusanDao() {
		return new JurusanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SkripsiDao getSkripsiDao() {
		return new SkripsiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RuangDao getRuangDao() {
		return new RuangDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KonsentrasiDao getKonsentrasiDao() {
		return new KonsentrasiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GedungDao getGedungDao() {
		return new GedungDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KomentarDao getKomentarDao() {
		return new KomentarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenjangProgramStudiDao getJenjangProgramStudiDao() {
		// TODO Auto-generated method stub
		return new JenjangProgramStudiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenjangDao getJenjangDao() {
		// TODO Auto-generated method stub
		return new JenjangDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BadanHukumDao getBadanHukumDao() {
		return new BadanHukumDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisKegiatanDao getJenisKegiatanDao() {
		return new JenisKegiatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KalenderAkademikDao getKalenderAkademikDao() {
		return new KalenderAkademikDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MatakuliahBerbayarDao getMatakuliahBerbayarDao() {
		return new MatakuliahBerbayarDaoImpl().setSession(getCurrentSession());
	}

	public PembobotanNilaiDao getPembobotanNilaiDao() {
		return new PembobotanNilaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BankDao getBankDao() {

		return new BankDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PerguruanTinggiDao getPerguruanTinggiDao() {
		return new PerguruanTinggiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ItemBiayaDao getItemBiayaDao() {
		return new ItemBiayaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisPembayaranDao getJenisPembayaranDao() {
		return new JenisPembayaranDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DetailBiayaDao getDetailBiayaDao() {
		return new DetailBiayaDaoImpl().setSession(getCurrentSession());
	}

	// @Override
	//
	// public JenisBiayaDao getJenisBiayaDao() {
	// return new JenisBiayaDaoImpl().setSession(getCurrentSession());
	// }

	@Override
	public JenisSeleksiDao getJenisSeleksiDao() {
		return new JenisSeleksiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BiodataMahasiswaDao getBiodataMahasiswaDao() {
		return new BiodataMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BiodataDosenDao getBiodataDosenDao() {
		return new BiodataDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BiodataPegawaiDao getBiodataPegawaiDao() {
		return new BiodataPegawaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BiodataCalonMahasiswaDao getBiodataCalonMahasiswaDao() {
		return new BiodataCalonMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RekeningDosenDao getRekeningDosenDao() {

		return new RekeningDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PendaftaranWisudaDao getPendaftaranWisudaDao() {

		return new PendaftaranWisudaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public NegaraDao getNegaraDao() {
		return new NegaraDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public WisudaDao getWisudaDao() {
		return new WisudaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PengumumanAkademisDao getPengumumanAkademisDao() {
		return new PengumumanAkademisDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DiskusiPengumumanAkademisDao getDiskusiPengumumanAkademisDao() {
		return new DiskusiPengumumanAkademisDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public LampiranPengumumanAkademisDao getLampiranPengumumanAkademisDao() {
		return new LampiranPengumumanAkademisDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public QuotaWisudaUntukFakultasDao getQuotaWisudaUntukFakultasDao() {
		return new QuotaWisudaUntukFakultasDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PropinsiDao getPropinsiDao() {
		return new PropinsiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KotaDao getKotaDao() {
		return new KotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PaketRegistrasiMahasiswaDao getPaketRegistrasiMahasiswaDao() {
		return new PaketRegistrasiMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PendaftaranSidangDao getPendaftaranSidangDao() {

		return new PendaftaranSidangDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AgamaDao getAgamaDao() {
		return new AgamaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PengajuanBeasiswaDao getPengajuanBeasiswaDao() {
		return new PengajuanBeasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public UjianPMBDao getUjianPMBDao() {
		return new UjianPMBDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KelasDao getKelasDao() {
		return new KelasDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KelompokMatakuliahDao getKelompokMatakuliahDao() {
		return new KelompokMatakuliahDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StatusMatakuliahDao getStatusMatakuliahDao() {
		return new StatusMatakuliahDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BaypassPembayaranMahasiswaDao getBaypassPembayaranMahasiswaDao() {
		return new BaypassPembayaranMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PaketPerkuliahanDao getPaketPerkuliahanDao() {
		return new PaketPerkuliahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TemplateQueryDao getTemplateQueryDao() {
		return new TemplateQueryDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaRequestTugasAkhirMintaPembimbingDao getMahasiswaRequestTugasAkhirMintaPembimbingDao() {
		return new MahasiswaRequestTugasAkhirMintaPembimbingDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaRequestTugasAkhirDao getMahasiswaRequestTugasAkhirDao() {
		return new MahasiswaRequestTugasAkhirDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JudisiumDao getJudisiumDao() {
		return new JudisiumDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StatusPegawaiDao getStatusPegawaiDao() {
		return new StatusPegawaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public InputPembayaranPunyaAkunDao getInputPembayaranPunyaAkunDao() {
		return new InputPembayaranPunyaAkunDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MessageDao getMessageDao() {
		return new MessageDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PesanRuanganDao getPesanRuanganDao() {
		return new PesanRuanganDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KonfigurasiKalenderAkademikDao getKonfigurasiKalenderAkademikDao() {
		return new KonfigurasiKalenderAkademikDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TemplateSuratParameterDao getTemplateSuratParameterDao() {
		return new TemplateSuratParameterDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public FormatTemplateSuratDao getFormatTemplateSuratDao() {
		return new FormatTemplateSuratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TemplateSuratDao getTemplateSuratDao() {
		return new TemplateSuratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JamPerkuliahanDao getJamPerkuliahanDao() {
		return new JamPerkuliahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KurikulumPunyaMatakuliahDetailDao getKurikulumPunyaMatakuliahDetailDao() {
		return new KurikulumPunyaMatakuliahDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TemplatePerkuliahanDetailDao getTemplatePerkuliahanDetailDao() {
		return new TemplatePerkuliahanDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TemplatePerkuliahanDao getTemplatePerkuliahanDao() {
		return new TemplatePerkuliahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public NamaTugasKelompokPunyaMahasiswaDao getNamaTugasKelompokPunyaMahasiswaDao() {
		return new NamaTugasKelompokPunyaMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public NamaTugasKelompokDao getNamaTugasKelompokDao() {
		return new NamaTugasKelompokDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TugasKelompokDao getTugasKelompokDao() {
		return new TugasKelompokDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PertemuanPunyaUjianDao getPertemuanPunyaUjianDao() {
		return new PertemuanPunyaUjianDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PertemuanPunyaDiskusiDao getPertemuanPunyaDiskusiDao() {
		return new PertemuanPunyaDiskusiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MatakuliahPunyaBukuBahanAjarDao getMatakuliahPunyaBukuBahanAjarDao() {
		return new MatakuliahPunyaBukuBahanAjarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BukuBahanAjarDao getBukuBahanAjarDao() {
		return new BukuBahanAjarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public UjianDao getUjianDao() {
		return new UjianDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BankSoalDao getBankSoalDao() {
		return new BankSoalDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BankSoalDetailDao getBankSoalDetailDao() {
		return new BankSoalDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PendaftaranCutiMahasiswaDao getPendaftaranCutiMahasiswaDao() {
		return new PendaftaranCutiMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KonfigurasiDao getKonfigurasiDao() {
		return new KonfigurasiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DendaPembayaranDao getDendaPembayaranDao() {
		return new DendaPembayaranDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DendaPembayaranNominalDao getDendaPembayaranNominalDao() {
		return new DendaPembayaranNominalDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JadwalPembayaranDao getJadwalPembayaranDao() {
		return new JadwalPembayaranDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JabatanDao getJabatanDao() {
		return new JabatanDaoImpl().setSession(getCurrentSession());
	}

	public BankHostDao getBankHostDao() {
		return new BankHostDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TbmroleDao getTbmroleDao() {
		return new TbmroleDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisKartuIdentitasMahasiswaBaruDao getJenisKartuIdentitasMahasiswaBaruDao() {
		return new JenisKartuIdentitasMahasiswaBaruDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisSekolahMahasiswaBaruDao getJenisSekolahMahasiswaBaruDao() {
		return new JenisSekolahMahasiswaBaruDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JurusanSekolahMahasiswaBaruDao getJurusanSekolahMahasiswaBaruDao() {
		return new JurusanSekolahMahasiswaBaruDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PekerjaanOrangTuaDao getPekerjaanOrangTuaDao() {
		return new PekerjaanOrangTuaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PendidikanOrangTuaDao getPendidikanOrangTuaDao() {
		return new PendidikanOrangTuaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PendapatanOrangTuaDao getPendapatanOrangTuaDao() {
		return new PendapatanOrangTuaDaoImpl().setSession(getCurrentSession());
	}

	//
	// @Override
	// public DetailBiayaCalonMahasiswaDao getDetailBiayaCalonMahasiswaDao() {
	// return new DetailBiayaCalonMahasiswaDaoImpl()
	// .setSession(getCurrentSession());
	// }

	@Override
	public LogCsvFileUploadDao getLogCsvFileUploadDao() {
		return new LogCsvFileUploadDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StatusLulusCalonMahasiswaDao getStatusLulusCalonMahasiswaDao() {
		return new StatusLulusCalonMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KurikulumDao getKurikulumDao() {

		return new KurikulumDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AngkatanKurikulumDao getAngkatanKurikulumDao() {

		return new AngkatanKurikulumDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DetailKegiatanDao getDetailKegiatanDao() {

		return new DetailKegiatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisKegiatanDetailDao getJenisKegiatanDetailDao() {

		return new JenisKegiatanDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DetailJenisKegiatanDao getDetailJenisKegiatanDao() {
		// TODO Auto-generated method stub
		return new DetailJenisKegiatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KegiatanDao getKegiatanDao() {
		return new KegiatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KurikulumPunyaMatakuliahDao getKurikulumPunyaMatakuliahDao() {
		return new KurikulumPunyaMataKuliahDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GambarFakultasDao getGambarFakultasDao() {
		return new GambarFakultasDaoImpl().setSession(getCurrentSession());
	}

	public SettingBiayaDao getSettingBiayaDao() {
		// TODO Auto-generated method stub
		return new SettingBiayaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public FormatNilaiSkripsiDao getFormatNilaiSkripsiDao() {
		// TODO Auto-generated method stub
		return new FormatNilaiSkripsiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PembatasanNilaiIPKUntukPengambilanKRSDao getPembatasanNilaiIPKUntukPengambilanKRSDao() {
		// TODO Auto-generated method stub
		return new PembatasanNilaiIPKUntukPengambilanKRSDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DetailSettingBiayaDao getDetailSettingBiayaDao() {
		// TODO Auto-generated method stub
		return new DetailSettingBiayaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TimDosenDao gettTimDosenDao() {
		// TODO Auto-generated method stub
		return new TimDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KonversiDao getKonversiDao() {
		// TODO Auto-generated method stub
		return new KonversiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MatakuliahAwalKonversiDao getMatakuliahAwalKonversiDao() {
		// TODO Auto-generated method stub
		return new MatakuliahAwalKonversiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StaffDao getStaffDao() {
		// TODO Auto-generated method stub
		return new StaffDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PrefixDao getPrefixDao() {
		// TODO Auto-generated method stub
		return new PrefixDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TingkatKesulitanMatakuliahDao getTingkatKesulitanMatakuliahDao() {
		// TODO Auto-generated method stub
		return new TingkatKesulitanMatakuliahDaoImpl().setSession(getCurrentSession());

	}

	@Override
	public PaketDao getPaketDao() {
		// TODO Auto-generated method stub
		return new PaketDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PilihanPaketPerJurusanDao getPilihanPaketPerJurusanDao() {
		// TODO Auto-generated method stub
		return new PilihanPaketPerJurusanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PaketJurusanPmbDao getPaketJurusanPmbDao() {
		// TODO Auto-generated method stub
		return new PaketJurusanPmbDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public CekKesehatanDao getCekKesehatanDao() {
		// TODO Auto-generated method stub
		return new CekKesehatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RuangPMBDao getRuangPMBDao() {
		// TODO Auto-generated method stub
		return new RuangPMBDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RuangPaketPMBDao getRuangPaketPMBDao() {
		// TODO Auto-generated method stub
		return new RuangPaketPMBDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KapasitasMahasiswaBaruEpsbedDao getKapasitasMahasiswaBaruEpsbedDao() {
		// TODO Auto-generated method stub
		return new KapasitasMahasiswaBaruEpsbedDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public FasilitasAkademikJurusanDao getAkademikJurusanDao() {
		// TODO Auto-generated method stub
		return new FasilitasAkademikJurusanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PublikasiDosenDao getPublikasiDosenDao() {
		// TODO Auto-generated method stub
		return new PublikasiDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RiwayatPendidikanDosenDao getRiwayatPendidikanDosenDao() {
		// TODO Auto-generated method stub
		return new RiwayatPendidikanDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AkunDao getAkunDao() {
		// TODO Auto-generated method stub
		return new AkunDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisTransaksiDao getJenisTransaksiDao() {
		// TODO Auto-generated method stub
		return new JenisTransaksiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DevisiDao getDevisiDao() {
		// TODO Auto-generated method stub
		return new DevisiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GrupAkunDao getGrupAkunDao() {
		// TODO Auto-generated method stub
		return new GrupAkunDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GrupTransaksiDao getGrupTransaksiDao() {
		// TODO Auto-generated method stub
		return new GrupTransaksiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisLaporanDao getJenisLaporanDao() {
		// TODO Auto-generated method stub
		return new JenisLaporanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KelompokLaporanDao getKelompokLaporanDao() {
		// TODO Auto-generated method stub
		return new KelompokLaporanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KelompokLaporanPunyaAkunDao getKelompokLaporanPunyaAkunDao() {
		// TODO Auto-generated method stub
		return new KelompokLaporanPunyaAkunDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MasterGrupLaporanDao getMasterGrupLaporanDao() {
		// TODO Auto-generated method stub
		return new MasterGrupLaporanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MataUangDao getMataUangDao() {
		// TODO Auto-generated method stub
		return new MataUangDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AkunPajakDao getAkunPajakDao() {
		// TODO Auto-generated method stub
		return new AkunPajakDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TransaksiDao getTransaksiDao() {
		// TODO Auto-generated method stub
		return new TransaksiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public WorkspaceDao getWorkspaceDao() {
		// TODO Auto-generated method stub
		return new WorkspaceDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SatuanKerjaDao getSatuanKerjaDao() {
		// TODO Auto-generated method stub
		return new SatuanKerjaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ChecklistLaporanDao getChecklistLaporanDao() {
		// TODO Auto-generated method stub
		return new ChecklistLaporanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ChecklistLaporanDetailDao getChecklistLaporanDetailDao() {
		// TODO Auto-generated method stub
		return new ChecklistLaporanDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ChecklistLaporanDetailDefaultDao getChecklistLaporanDetailDefaultDao() {
		// TODO Auto-generated method stub
		return new ChecklistLaporanDetailDefaultDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SumberDanaDao getSumberDanaDao() {
		// TODO Auto-generated method stub
		return new SumberDanaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TugasDao getTugasDao() {
		// TODO Auto-generated method stub
		return new TugasDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ProyekDao getProyekDao() {
		// TODO Auto-generated method stub
		return new ProyekDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SatuanDao getSatuanDao() {
		// TODO Auto-generated method stub
		return new SatuanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public IndikatorDao getIndikatorDao() {
		// TODO Auto-generated method stub
		return new IndikatorDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TorDao getTorDao() {
		// TODO Auto-generated method stub
		return new TorDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RencanaDanRealisasiOutputKegiatanDao getRencanaDanRealisasiOutputKegiatanDao() {
		// TODO Auto-generated method stub
		return new RencanaDanRealisasiOutputKegiatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public OutputKegiatanDao getOutputKegiatanDao() {
		// TODO Auto-generated method stub
		return new OutputKegiatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SasaranDao getSasaranDao() {
		// TODO Auto-generated method stub
		return new SasaranDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisParameterDao getJenisParameterDao() {
		// TODO Auto-generated method stub
		return new JenisParameterDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisTugasDao getJenisTugasDao() {
		// TODO Auto-generated method stub
		return new JenisTugasDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public UnitOrganisasiDao getUnitOrganisasiDao() {
		// TODO Auto-generated method stub
		return new UnitOrganisasiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisWorkspaceDao getJenisWorkspaceDao() {
		// TODO Auto-generated method stub
		return new JenisWorkspaceDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public HasilSatuanDao getHasilSatuanDao() {
		// TODO Auto-generated method stub
		return new HasilSatuanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AcaraDao getAcaraDao() {
		// TODO Auto-generated method stub
		return new AcaraDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MetodePengadaanDao getMetodePengadaanDao() {
		// TODO Auto-generated method stub
		return new MetodePengadaanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KegiatanSatkerDao getKegiatanSatkerDao() {
		// TODO Auto-generated method stub
		return new KegiatanSatkerDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PejabatDao getPejabatDao() {
		// TODO Auto-generated method stub
		return new PejabatDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MitraDao getMitraDao() {
		// TODO Auto-generated method stub
		return new MitraDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AcaraPunyaKendalaDao getAcaraPunyaKendalaDao() {
		// TODO Auto-generated method stub
		return new AcaraPunyaKendalaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisItemDao getJenisItemDao() {
		return new JenisItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BatasWaktuPeminjamanItemDao getBatasWaktuPeminjamanItemDao() {
		return new BatasWaktuPeminjamanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DendaKeterlambatanItemDao getDendaKeterlambatanItemDao() {
		return new DendaKeterlambatanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BiayaPendaftaranAnggotaDao getBiayaPendaftaranAnggotaDao() {
		return new BiayaPendaftaranAnggotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TipeItemDao getTipeItemDao() {
		return new TipeItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KategoriItemDao getKategoriItemDao() {
		return new KategoriItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisPelatihanDao getJenisPelatihanDao() {
		return new JenisPelatihanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DdcItemDao getDdcItemDao() {
		return new DdcItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public UdcItemDao getUdcItemDao() {
		return new UdcItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisIdentitasAnggotaDao getJenisIdentitasAnggotaDao() {
		return new JenisIdentitasAnggotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ItemDao getItemDao() {
		return new ItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RakDao getRakDao() {
		return new RakDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisAnggotaDao getJenisAnggotaDao() {
		return new JenisAnggotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PesananAnggotaDao getPesananAnggotaDao() {
		return new PesananAnggotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisInformasiPerpustakaanDao getJenisInformasiPerpustakaanDao() {
		return new JenisInformasiPerpustakaanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisInformasiRabDao getJenisInformasiRabDao() {
		return new JenisInformasiRabDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TipeAnggotaDao getTipeAnggotaDao() {
		return new TipeAnggotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AnggotaDao getAnggotaDao() {
		return new AnggotaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public VersiDdcItemDao getVersiDdcItemDao() {
		return new VersiDdcItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DomainPenelitianDao getDomainPenelitianDao() {
		return new DomainPenelitianDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DataDdcItemDao getDataDdcItemDao() {
		return new DataDdcItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DataUdcItemDao getDataUdcItemDao() {
		return new DataUdcItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public InformasiPerpustakaanDao getInformasiPerpustakaanDao() {
		return new InformasiPerpustakaanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public InformasiRabDao getInformasiRabDao() {
		return new InformasiRabDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RenstraProgramDao getRenstraProgramDao() {
		return new RenstraProgramDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RenstraProgramPunyaIndikatorDao getRenstraProgramPunyaIndikatorDao() {
		return new RenstraProgramPunyaIndikatorDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PenyediaDao getPenyediaDao() {
		return new PenyediaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StatusItemDao getStatusItemDao() {
		return new StatusItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PengarangDao getPengarangDao() {
		return new PengarangDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PerpustakaanDao getPerpustakaanDao() {
		return new PerpustakaanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PustakawanDao getPustakawanDao() {
		return new PustakawanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SaldoAwalDao getSaldoAwalDao() {
		return new SaldoAwalDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RakDetailDao getRakDetailDao() {
		return new RakDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SaldoAwalDetailDao getSaldoAwalDetailDao() {
		return new SaldoAwalDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PermintaanPengadaanItemDao getPermintaanPengadaanItemDao() {
		return new PermintaanPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PermintaanPengadaanItemDetailDao getPermintaanPengadaanItemDetailDao() {
		return new PermintaanPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KoreksiItemDao getKoreksiItemDao() {
		return new KoreksiItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KoreksiItemDetailDao getKoreksiItemDetailDao() {
		return new KoreksiItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TransferPengadaanItemDao getTransferPengadaanItemDao() {
		return new TransferPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TransferPengadaanItemDetailDao getTransferPengadaanItemDetailDao() {
		return new TransferPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PeminjamanPengadaanItemDao getPeminjamanPengadaanItemDao() {
		return new PeminjamanPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PeminjamanPengadaanItemDetailDao getPeminjamanPengadaanItemDetailDao() {
		return new PeminjamanPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TerimaPengadaanItemDao getTerimaPengadaanItemDao() {
		return new TerimaPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TerimaPengadaanItemDetailDao getTerimaPengadaanItemDetailDao() {
		return new TerimaPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KembaliPengadaanItemDao getKembaliPengadaanItemDao() {
		return new KembaliPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KembaliPengadaanItemDetailDao getKembaliPengadaanItemDetailDao() {
		return new KembaliPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PemesananPengadaanItemDao getPemesananPengadaanItemDao() {
		return new PemesananPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PemesananPengadaanItemDetailDao getPemesananPengadaanItemDetailDao() {
		return new PemesananPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PenerimaanPengadaanItemDao getPenerimaanPengadaanItemDao() {
		return new PenerimaanPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PenerimaanPengadaanItemDetailDao getPenerimaanPengadaanItemDetailDao() {
		return new PenerimaanPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ReturPengadaanItemDao getReturPengadaanItemDao() {
		return new ReturPengadaanItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ReturPengadaanItemDetailDao getReturPengadaanItemDetailDao() {
		return new ReturPengadaanItemDetailDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public TopikItemDao getTopikItemDao() {
		return new TopikItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public LabelItemDao getLabelItemDao() {
		return new LabelItemDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PenerbitDao getPenerbitDao() {
		return new PenerbitDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public NilaiToeflToaflDao getNilaiToeflToaflDao() {
		// TODO Auto-generated method stub
		return new NilaiToeflToaflDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public LokasiDao getLokasiDao() {
		return new LokasiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisAssetDao getJenisAssetDao() {
		return new JenisAssetDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KelompokAssetDao getKelompokAssetDao() {
		return new KelompokAssetDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PemilikAssetDao getPemilikAssetDao() {
		return new PemilikAssetDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AssetDao getAssetDao() {
		return new AssetDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public StatusAssetDao getStatusAssetDao() {
		return new StatusAssetDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PendidikanDao getPendidikanDao() {
		return new PendidikanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DaftarNilaiPelaksanaanPekerjaanDao getDaftarNilaiPelaksanaanPekerjaanDao() {
		return new DaftarNilaiPelaksanaanPekerjaanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JabatanStrukturalDao getJabatanStrukturalDao() {
		return new JabatanStrukturalDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JabatanFungsionalDao getJabatanFungsionalDao() {
		return new JabatanFungsionalDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisJabatanDao getJenisJabatanDao() {
		return new JenisJabatanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisTandaJasaDao getJenisTandaJasaDao() {
		return new JenisTandaJasaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PeraturanDao getPeraturanDao() {
		return new PeraturanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GolonganDao getGolonganDao() {
		return new GolonganDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GajiPokokDao getGajiPokokDao() {
		return new GajiPokokDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisPimpinanDao getJenisPimpinanDao() {
		return new JenisPimpinanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public UnitKerjaDao getUnitKerjaDao() {
		return new UnitKerjaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public GrupChecklistPenilaianDosenDao getChecklistPenilaianDosenDao() {
		// TODO Auto-generated method stub
		return new GrupChecklistPenilaianDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ChecklistPenilaianDosenDao getCchecklistPenilaianDosenDao() {
		// TODO Auto-generated method stub
		return new ChecklistPenilaianDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RiwayatPendidikanPegawaiDao getRiwayatPendidikanPegawaiDao() {
		// TODO Auto-generated method stub
		return new RiwayatPendidikanPegawaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisDiklatDao getJenisDiklatDao() {
		// TODO Auto-generated method stub
		return new JenisDiklatDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public RiwayatStatusKepegawaianDao getRiwayatStatusKepegawaianDao() {
		// TODO Auto-generated method stub
		return new RiwayatStatusKepegawaianDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisKegiatanEmployDao getJenisKegiatanEmployDao() {
		return new JenisKegiatanEmployDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public DiklatDao getDiklatDao() {
		// TODO Auto-generated method stub
		return new DiklatDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisPensiunDao getJenisPensiunDao() {
		// TODO Auto-generated method stub
		return new JenisPensiunDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KenaikanPangkatDao getKenaikanPangkatDao() {
		// TODO Auto-generated method stub
		return new KenaikanPangkatDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KeluargaPegawaiDao getKeluargaPegawaiDao() {
		// TODO Auto-generated method stub
		return new KeluargaPegawaiDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PensiunDao getPensiunDao() {
		// TODO Auto-generated method stub
		return new PensiunDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KonfigurasiSKDao getKonfigurasiSKDao() {
		// TODO Auto-generated method stub
		return new KonfigurasiSKDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PersyaratanBeasiswaDao getPersyaratanBeasiswaDao() {
		// TODO Auto-generated method stub
		return new PersyaratanBeasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public BeasiswaPunyaPersyaratanDao getBeasiswaPunyaPersyaratanDao() {
		// TODO Auto-generated method stub
		return new BeasiswaPunyaPersyaratanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaDaftarBeasiswaDao getMahasiswaDaftarBeasiswaDao() {
		// TODO Auto-generated method stub
		return new MahasiswaDaftarBeasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MahasiswaBeasiswaPersyaratanDao getMahasiswaBeasiswaPersyaratanDao() {
		// TODO Auto-generated method stub
		return new MahasiswaBeasiswaPersyaratanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SuratMasukDao getSuratMasukDao() {
		return new SuratMasukDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SuratKeluarDao getSuratKeluarDao() {
		return new SuratKeluarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public SifatSuratDao getSifatSuratDao() {
		return new SifatSuratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KopSuratDao getKopSuratDao() {
		return new KopSuratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AlurPersetujuanSuratKeluarStatusDao getAlurPersetujuanSuratKeluarStatusDao() {
		return new AlurPersetujuanSuratKeluarStatusDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AlurPersetujuanSuratKeluarDao getAlurPersetujuanSuratKeluarDao() {
		return new AlurPersetujuanSuratKeluarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AlurPersetujuanSuratMasukStatusDao getAlurPersetujuanSuratMasukStatusDao() {
		return new AlurPersetujuanSuratMasukStatusDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public AlurPersetujuanSuratMasukDao getAlurPersetujuanSuratMasukDao() {
		return new AlurPersetujuanSuratMasukDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public LokerSuratDao getLokerSuratDao() {
		return new LokerSuratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PenyampaianSuratDao getPenyampaianSuratDao() {
		return new PenyampaianSuratDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public OpsiSuratMasukDao getOpsiSuratMasukDao() {
		return new OpsiSuratMasukDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public OpsiSuratKeluarDao getOpsiSuratKeluarDao() {
		return new OpsiSuratKeluarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KlasifikasiSuratKeluarDao getKlasifikasiSuratKeluarDao() {
		return new KlasifikasiSuratKeluarDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KlasifikasiSuratMasukDao getKlasifikasiSuratMasukDao() {
		return new KlasifikasiSuratMasukDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KlasifikasiSuratKeluarUntukDao getKlasifikasiSuratKeluarUntukDao() {
		return new KlasifikasiSuratKeluarUntukDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JenisPertemuanDao getJenisPertemuanDao() {
		// TODO Auto-generated method stub
		return new JenisPertemuanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ais.database.dao.kedokteran.PertemuanKedokteranDao getPertemuanKedokteranDao() {
		// TODO Auto-generated method stub
		return new ais.database.dao.kedokteran.PertemuanKedokteranDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PertemuanHasDosenDao getPertemuanHasDosenDao() {
		// TODO Auto-generated method stub
		return new PertemuanHasDosenDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public PHDHasMahasiswaDao getPhdHasMahasiswaDao() {
		// TODO Auto-generated method stub
		return new PHDHasMahasiswaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MetaReportDao getMetaReportDao() {
		// TODO Auto-generated method stub
		return new MetaReportDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public JabatanFungsionalTambahanDao getJabatanFungsionalTambahanDao() {
		// TODO Auto-generated method stub
		return new JabatanFungsionalTambahanDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public KenaikanGajiBerkalaDao getKenaikanGajiBerkalaDao() {
		// TODO Auto-generated method stub
		return new KenaikanGajiBerkalaDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public MutasiPindahDao getMutasiPindahDao() {
		// TODO Auto-generated method stub
		return new MutasiPindahDaoImpl().setSession(getCurrentSession());
	}

	@Override
	public ais.database.dao.employ.SatuanKerjaDao getSatuanKerjaEmployDao() {
		// TODO Auto-generated method stub
		return new ais.database.dao.employ.SatuanKerjaDaoImpl().setSession(getCurrentSession());
	}
}
