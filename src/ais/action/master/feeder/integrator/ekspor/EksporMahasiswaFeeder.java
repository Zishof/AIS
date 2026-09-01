package ais.action.master.feeder.integrator.ekspor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.common.newui.pekerjaan.PekerjaanRegistry;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.StatusMahasiswa;

/**
 * Penyusun berkas ekspor Mahasiswa untuk Feeder, tanpa ketergantungan pada ZK.
 *
 * <p><b>Mengapa dipisahkan.</b> Susunan kolom berkas ini ditentukan PDDIKTI:
 * empat puluh delapan kolom dengan urutan, kode, dan format yang harus persis.
 * Menulis ulang pemetaannya untuk jalur native berarti dua daftar kolom yang
 * harus dijaga tetap sama terhadap aturan pihak luar — dan yang menyimpan
 * akibat kesalahannya adalah lembaga, bukan aplikasi ini. Karena itu badan
 * ekspornya dipindahkan apa adanya ke sini, lalu dipakai bersama oleh panel ZK
 * dan oleh pekerjaan latar yang dapat ditanya kemajuannya.</p>
 *
 * <p>Yang berubah dari versi ZK hanyalah sumber nilainya: saringan datang dari
 * {@link SaringanFeeder} alih-alih widget, dan kemajuan dilaporkan lewat
 * {@link PekerjaanRegistry.Progres} alih-alih memperbarui label.</p>
 */
public final class EksporMahasiswaFeeder {

    private EksporMahasiswaFeeder() { }

    /**
     * Susun berkas ekspor ke {@code tujuan}.
     *
     * @param tujuan  berkas .xlsx yang akan ditulis
     * @param s       saringan; boleh null, diperlakukan sebagai tanpa saringan
     * @param progres kanal pelaporan kemajuan
     * @return jumlah baris data yang ditulis
     */
    public static int tulis(File tujuan, SaringanFeeder s, PekerjaanRegistry.Progres progres) throws Exception {
        // Tipe saringan yang sama dipakai kedua puluh tiga panel unduh. Semula
        // kelas ini punya tipe sendiri; disatukan setelah panel lain menyusul,
        // supaya pemanggilnya tidak perlu tahu panel mana memakai tipe yang mana.
        if (s == null) s = new SaringanFeeder();
        s.rapikan();
        if (progres == null) {
            progres = new PekerjaanRegistry.Progres() {
                public void lapor(int persen, String pesan) { }
            };
        }
		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFSheet sheet = workbook.createSheet("Mahasiswa");
		sheet.setDefaultColumnWidth(18);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("NIM");
		rowhead.createCell(1).setCellValue("Nama");
		rowhead.createCell(2).setCellValue("Tempat Lahir");
		rowhead.createCell(3).setCellValue("Tanggal Lahir");
		rowhead.createCell(4).setCellValue("Jenis Kelamin");
		rowhead.createCell(5).setCellValue("NIK");
		rowhead.createCell(6).setCellValue("Agama");
		rowhead.createCell(7).setCellValue("NISN");
		rowhead.createCell(8).setCellValue("Jalur Pendaftaran");
		rowhead.createCell(9).setCellValue("NPWP");
		rowhead.createCell(10).setCellValue("Kewarganegaraan");
		rowhead.createCell(11).setCellValue("Jenis Pendaftaran");
		rowhead.createCell(12).setCellValue("Tgl Masuk Kuliah");
		rowhead.createCell(13).setCellValue("Mulai semester");
		rowhead.createCell(14).setCellValue("Jalan");
		rowhead.createCell(15).setCellValue("RT");
		rowhead.createCell(16).setCellValue("RW");
		rowhead.createCell(17).setCellValue("Nama Dusun");
		rowhead.createCell(18).setCellValue("Kelurahan");
		rowhead.createCell(19).setCellValue("Kecamatan");
		rowhead.createCell(20).setCellValue("Kode Pos");
		rowhead.createCell(21).setCellValue("Jenis Tinggal");
		rowhead.createCell(22).setCellValue("Alat Transportasi");
		rowhead.createCell(23).setCellValue("Telp Rumah");
		rowhead.createCell(24).setCellValue("No HP");
		rowhead.createCell(25).setCellValue("Email");
		rowhead.createCell(26).setCellValue("Terima KPS");
		rowhead.createCell(27).setCellValue("No KPS");
		rowhead.createCell(28).setCellValue("NIK Ayah");
		rowhead.createCell(29).setCellValue("Nama Ayah");
		rowhead.createCell(30).setCellValue("Tgl Lahir Ayah");
		rowhead.createCell(31).setCellValue("Pendidikan Ayah");
		rowhead.createCell(32).setCellValue("Pekerjaan Ayah");
		rowhead.createCell(33).setCellValue("Penghasilan Ayah");
		rowhead.createCell(34).setCellValue("NIK Ibu");
		rowhead.createCell(35).setCellValue("Nama Ibu");
		rowhead.createCell(36).setCellValue("Tanggal Lahir Ibu");
		rowhead.createCell(37).setCellValue("Pendidikan Ibu");
		rowhead.createCell(38).setCellValue("Pekerjaan Ibu");
		rowhead.createCell(39).setCellValue("Penghasilan Ibu");
		rowhead.createCell(40).setCellValue("Nama Wali");
		rowhead.createCell(41).setCellValue("Tanggal Lahir wali");
		rowhead.createCell(42).setCellValue("Pendidikan Wali");
		rowhead.createCell(43).setCellValue("Pekerjaan Wali");
		rowhead.createCell(44).setCellValue("Penghasilan Wali");
		rowhead.createCell(45).setCellValue("Kode Prodi");
		rowhead.createCell(46).setCellValue("Jenis Pembiayaan");
		rowhead.createCell(47).setCellValue("Biaya Masuk");

		Session session = HibernateUtil.currentNativeSession();

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (s.status != null) {
		String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
				+ s.status.getId() + " and tahunakademik = '"
				+ Common.getCurrentTahunAkademik() + "' and semester%2="
				+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
		System.out.println("sql=>" + sql);
		criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

			.add(criteriaStatus)

			.add(s.kelas != null && !s.kelas.trim().isEmpty()
					? Restrictions.ilike("kelas", s.kelas.trim(), MatchMode.EXACT)
					: Restrictions.sqlRestriction("true"))

			.add(Restrictions
					.and(s.nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nim", s.nim.trim(), MatchMode.ANYWHERE),

							s.nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.ilike("nama", s.nama.trim(),
											MatchMode.ANYWHERE)))

			.add(s.jurusan == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("jurusan", s.jurusan))

			.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

			.add(s.fakultas == null ? Restrictions.sqlRestriction("1=1")
					: CommonSearchFilterHelper.eqValue("jurusan.fakultas", s.fakultas, false))

			.add(s.program == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("program", s.program))

			.add(s.angkatan == null ? Restrictions.sqlRestriction("1=1")
					: Restrictions.eq("tahunangkatan", s.angkatan))

			.addOrder(Order.asc("nim")).list();

		int size = mahasiswas.size();

		int rowIndex = 1;
		for (Mahasiswa mahasiswa : mahasiswas) {

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		progres.lapor((int) (rowIndex * 100.0 / size),
			"Sedang memproses data " + mahasiswa.toString());

		XSSFRow row = sheet.createRow(rowIndex);

		row.createCell(0).setCellValue(mahasiswa.getNim());
		row.createCell(1).setCellValue(mahasiswa.getNama());
		row.createCell(2).setCellValue(mahasiswa.getTempatlahir());
		row.createCell(3).setCellValue(mahasiswa.getTanggallahir() == null ? ""
				: Common.databaseDateFormat.get().format(mahasiswa.getTanggallahir()));
		row.createCell(4).setCellValue(mahasiswa.getKelamin().equals("Laki-laki") ? "L" : "P");
		row.createCell(5).setCellValue(biodataMahasiswa.getNoIdentitas());
		row.createCell(6)
				.setCellValue(mahasiswa.getAgama() == null ? 0L : mahasiswa.getAgama().getFeeder());

		row.createCell(7).setCellValue(biodataMahasiswa.getNisn());

		row.createCell(8).setCellValue(
				mahasiswa.getJenisSeleksi() == null ? "" : mahasiswa.getJenisSeleksi().getKode());

		row.createCell(9).setCellValue(biodataMahasiswa.getNpwp());

		row.createCell(10)
				.setCellValue(mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getKode());

		row.createCell(11).setCellValue(mahasiswa.getStatusAwalMahasiswa().getFeeder());
		row.createCell(12).setCellValue(mahasiswa.getTanggalMasuk() == null ? ""
				: Common.databaseDateFormat.get().format(mahasiswa.getTanggalMasuk()));

		row.createCell(13).setCellValue(mahasiswa.getTahunangkatan()
				+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GENAP) ? "2" : "1"));

		row.createCell(14).setCellValue(biodataMahasiswa.getAlamat());
		row.createCell(15).setCellValue(biodataMahasiswa.getRt());
		row.createCell(16).setCellValue(biodataMahasiswa.getRw());
		row.createCell(17).setCellValue(biodataMahasiswa.getDusun());
		row.createCell(18).setCellValue(
				biodataMahasiswa.getKelurahan() == null || biodataMahasiswa.getKelurahan().trim().isEmpty()
						? "-" : biodataMahasiswa.getKelurahan());

		row.createCell(19).setCellValue(
				biodataMahasiswa.getKecamatan() == null ? "" : biodataMahasiswa.getKecamatan().getFeeder());
		row.createCell(20).setCellValue(biodataMahasiswa.getKodepos());
		row.createCell(21).setCellValue(biodataMahasiswa.getJenisTinggalMahasiswa() == null ? 1L
				: biodataMahasiswa.getJenisTinggalMahasiswa().getFeeder());
		row.createCell(22).setCellValue(biodataMahasiswa.getAlatTransportasiMahasiswa() == null ? 1L
				: biodataMahasiswa.getAlatTransportasiMahasiswa().getFeeder());

		row.createCell(23).setCellValue(biodataMahasiswa.getTeleponRumah());
		row.createCell(24).setCellValue(biodataMahasiswa.getHp());
		row.createCell(25).setCellValue(biodataMahasiswa.getEmail());
		row.createCell(26).setCellValue(0);
		row.createCell(27).setCellValue("");
		row.createCell(28).setCellValue(biodataMahasiswa.getNikAyah());

		row.createCell(29).setCellValue(biodataMahasiswa.getNamaAyah());
		row.createCell(30).setCellValue(biodataMahasiswa.getTanggalLahirAyah() == null ? ""
				: Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirAyah()));
		row.createCell(31)
				.setCellValue(biodataMahasiswa.getJenjangPendidikanAyah() == null
						|| biodataMahasiswa.getJenjangPendidikanAyah().getFeeder() == null ? ""
								: biodataMahasiswa.getJenjangPendidikanAyah().getFeeder().toString());
		row.createCell(32)
				.setCellValue(biodataMahasiswa.getJenisPekerjaanAyah() == null
						|| biodataMahasiswa.getJenisPekerjaanAyah().getFeeder() == null ? ""
								: biodataMahasiswa.getJenisPekerjaanAyah().getFeeder().toString());

		row.createCell(33)
				.setCellValue(biodataMahasiswa.getJenisPenghasilanAyah() == null
						|| biodataMahasiswa.getJenisPenghasilanAyah() == null ? ""
								: biodataMahasiswa.getJenisPenghasilanAyah().getFeeder().toString());

		row.createCell(34).setCellValue(biodataMahasiswa.getNikIbu());

		row.createCell(35).setCellValue(biodataMahasiswa.getNamaIbu());
		row.createCell(36).setCellValue(biodataMahasiswa.getTanggalLahirIbu() == null ? ""
				: Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirIbu()));
		row.createCell(37)
				.setCellValue(biodataMahasiswa.getJenjangPendidikanIbu() == null
						|| biodataMahasiswa.getJenjangPendidikanIbu().getFeeder() == null ? ""
								: biodataMahasiswa.getJenjangPendidikanIbu().getFeeder().toString());

		row.createCell(38)
				.setCellValue(biodataMahasiswa.getJenisPekerjaanIbu() == null
						|| biodataMahasiswa.getJenisPekerjaanIbu().getFeeder() == null ? ""
								: biodataMahasiswa.getJenisPekerjaanIbu().getFeeder().toString());
		row.createCell(39)
				.setCellValue(biodataMahasiswa.getJenisPenghasilanIbu() == null
						|| biodataMahasiswa.getJenisPenghasilanIbu().getFeeder() == null ? ""
								: biodataMahasiswa.getJenisPenghasilanIbu().getFeeder().toString());

		row.createCell(40).setCellValue(biodataMahasiswa.getNamaWali());
		row.createCell(41).setCellValue(biodataMahasiswa.getTanggalLahirWali() == null ? ""
				: Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirWali()));
		row.createCell(42)
				.setCellValue(biodataMahasiswa.getJenjangPendidikanWali() == null
						|| biodataMahasiswa.getJenjangPendidikanWali().getFeeder() == null ? ""
								: biodataMahasiswa.getJenjangPendidikanWali().getFeeder().toString());
		row.createCell(43)
				.setCellValue(biodataMahasiswa.getJenisPekerjaanWali() == null
						|| biodataMahasiswa.getJenisPekerjaanWali().getFeeder() == null ? ""
								: biodataMahasiswa.getJenisPekerjaanWali().getFeeder().toString());
		row.createCell(44)
				.setCellValue(biodataMahasiswa.getJenisPenghasilanWali() == null
						|| biodataMahasiswa.getJenisPenghasilanWali().getFeeder() == null ? ""
								: biodataMahasiswa.getJenisPenghasilanWali().getFeeder().toString());
		row.createCell(45).setCellValue(mahasiswa.getJurusan().getKodeEpsbed());

		row.createCell(46).setCellValue(mahasiswa.getJenisPembiayaanMahasiswa() == null ? 1L
				: mahasiswa.getJenisPembiayaanMahasiswa().getFeeder());

		BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
				session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1),
				BiodataCalonMahasiswa.class);
		Double biayamasuk = 0.0;

		if (biodataCalonMahasiswa != null) {
			try {
				@SuppressWarnings("rawtypes")
				Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(
						biodataCalonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU,
						mahasiswa.getJurusan(), false);

				int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session,
						biodataCalonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1,
						detailBiayas, false, false);

				if (countPengaturanBulanan > 0) {

					detailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
							biodataCalonMahasiswa, session, 1,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, false, false);

				}

				if (!detailBiayas.isEmpty()) {
					Kegiatan kegiatan = biodataCalonMahasiswa
							.ambilKegiatans(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					Double biaya = 0.0;
					for (Object o : detailBiayas) {
						if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							Double jumlah = pengaturanPembayaranBulanan.getNominal();
							biaya += jumlah;
						} else if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;

							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
							biaya += jumlah;
						}
					}
					biayamasuk = biaya;
				} else {
					biayamasuk = 0.0;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadMahasiswa.java:511");
				biayamasuk = 0.0;
			}
		}

		if (biayamasuk < 0.01) {
			try {
				int currentSemester = 1;
				@SuppressWarnings("rawtypes")
				Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
						currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

				int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
						ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, currentSemester, detailBiayas, false,
						false);

				if (countPengaturanBulanan > 0) {

					detailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa,
							currentSemester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA,
							countPengaturanBulanan > 0 ? "-1" : null, true, false);

				}

				if (!detailBiayas.isEmpty()) {
					Kegiatan kegiatan = mahasiswa.ambilKegiatans(currentSemester,
							ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
					Collection<DetailKegiatan> detailKegiatans = kegiatan == null
							|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(true);
					Double biaya = 0.0;
					for (Object o : detailBiayas) {
						if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans,
									mahasiswa, currentSemester, pengaturanPembayaranBulanan);
							biaya += jumlah;
						} else if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;

							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
							biaya += jumlah;
						}
					}
					biayamasuk = biaya;
				} else {
					biayamasuk = 0.0;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/integrator/helper/DownloadMahasiswa.java:559");
				biayamasuk = 0.0;
			}
		}

		row.createCell(47).setCellValue(biayamasuk);

		rowIndex++;

		}


        Common.setStyled(sheet);
        FileOutputStream keluaran = new FileOutputStream(tujuan);
        try {
            workbook.write(keluaran);
        } finally {
            keluaran.close();
        }
        int jumlah = rowIndex - 1;
        mahasiswas.clear();
        progres.lapor(100, "Selesai menyusun " + jumlah + " baris.");
        return jumlah;
    }
}
