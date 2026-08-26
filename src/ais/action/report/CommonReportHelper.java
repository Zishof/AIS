package ais.action.report;
import ais.common.CommonPMB;
import ais.common.PesanFormalHelper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.KonfigurasiTampilanBiodataCalonMahasiswaAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiAsistenDosen;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiDosen;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiGuru;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapAbsensiPerSiswa;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.UtsDanUasCheckerHelper;
import ais.action.master.pmb.CariDataPesertaUjianAction;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.action.master.pmb.VerifikasiPMBHelper;
import ais.action.master.pmb.nim.NimGenerator;
import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.surat.SuratKeluarAction;
import ais.action.report.format1.akademik.LaporanBeasiswaMahasiswa;
import ais.action.report.format1.akademik.LaporanKHS;
import ais.action.report.format1.akademik.LaporanKartuMahasiswa;
import ais.action.report.format1.akademik.LaporanKegiatanKemahasiswaan;
import ais.action.report.format1.akademik.LaporanKegiatanKesiswaan;
import ais.action.report.format1.akademik.LaporanOrganisasiDosen;
import ais.action.report.format1.akademik.LaporanOrganisasiMahasiswa;
import ais.action.report.format1.akademik.LaporanPrestasiMahasiswa;
import ais.action.report.format1.sekolah.LaporanOrganisasiSiswa;
import ais.action.servlet.Wa;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.CommonPMB;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.common.ManajemenProperty;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CommonVO;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailKelasPertemuan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KelasPertemuan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaMatapelajaran;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PengecualianJadwalPenilaianDosen;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.RuangPMB;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Staff;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Statusabsensi;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.UjianPMB;
import ais.database.model.VOMahasiswa;
import ais.database.model.VOPembelajaran;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.UjianPSB;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.SuratKeluar;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class CommonReportHelper {

	private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("dd MMMMM yyyy", Common.locale);
		}
	};

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<Map> buatParameterBuktiDariRincianLayar(Kegiatan kegiatan,
			List<Map<String, Serializable>> rincianLayar) {
		List<Map> hasil = new ArrayList<Map>();
		if (kegiatan == null || rincianLayar == null) {
			return hasil;
		}
		for (Map<String, Serializable> sumber : selaraskanSnapshotDenganRingkasan(kegiatan, rincianLayar)) {
			if (sumber == null || !(sumber.get("biaya") instanceof Number)) {
				continue;
			}
			Double nilaiLayar = Double.valueOf(((Number) sumber.get("biaya")).doubleValue());
			Map map = new HashMap(sumber);
			// Snapshot sudah berisi tagihan neto yang sama dengan panel rincian. Template
			// menghitung tagihan - diskon = biaya, sehingga ketiganya dinormalisasi di sini
			// dan tidak lagi membaca item DetailKegiatan lama yang sudah tidak tampil.
			map.put("tagihan", nilaiLayar);
			map.put("diskon", Double.valueOf(0.0));
			map.put("biaya", nilaiLayar);
			map.put("semester", sumber.get("semester") == null ? kegiatan.getSemster() : sumber.get("semester"));
			map.put("tahun_ajaran", sumber.get("tahun_ajaran") == null
					? kegiatan.getTahunAkademik() : sumber.get("tahun_ajaran"));
			map.put("nama_kegiatan", sumber.get("nama_kegiatan") == null
					? kegiatan.getJenisKegiatan().getNamaKegiatan() : sumber.get("nama_kegiatan"));
			map.put("ref_number", kegiatan.getRefNumber());
			map.put("validator", kegiatan.getValidator());
			map.put("keterangan", kegiatan.getKeterangan());
			if (!map.containsKey("keterangan1")) map.put("keterangan1", "");
			if (!map.containsKey("uraian")) map.put("uraian", "");

			if (kegiatan.getMahasiswa() != null) {
				Mahasiswa mahasiswa = kegiatan.getMahasiswa();
				map.put("nim", mahasiswa.getNim());
				map.put("no_registrasi", mahasiswa.getNim());
				map.put("nama_mahasiswa", mahasiswa.getNama());
				map.put("nama_fakultas", mahasiswa.getJurusan() == null
						|| mahasiswa.getJurusan().getFakultas() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getNama());
				map.put("nama_jurusan", mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
				map.put("program", mahasiswa.getProgram());
			} else if (kegiatan.getCalonMahasiswa() != null) {
				BiodataCalonMahasiswa calon = kegiatan.getCalonMahasiswa();
				map.put("nim", calon.getNim());
				String nomorRegistrasi = calon.getNoRegistrasi() == null ? "" : calon.getNoRegistrasi();
				if (calon.getMahasiswa() != null && calon.getMahasiswa().getNim() != null) {
					nomorRegistrasi += "(" + calon.getMahasiswa().getNim() + ")";
				}
				map.put("no_registrasi", nomorRegistrasi);
				map.put("nama_mahasiswa", calon.getNama());
				Jurusan jurusan = calon.getProdiLulus() != null ? calon.getProdiLulus()
						: calon.getProdi1() != null ? calon.getProdi1() : calon.getProdi2();
				map.put("nama_fakultas", jurusan == null || jurusan.getFakultas() == null
						? "" : jurusan.getFakultas().getNama());
				map.put("nama_jurusan", jurusan == null ? "" : jurusan.getNama());
				map.put("program", calon.getProgram());
			}
			hasil.add(map);
		}
		return hasil;
	}

	/**
	 * Rekonsiliasi terakhir antara rincian item dan angka ringkasan kegiatan.
	 *
	 * <p>Ringkasan kegiatan memakai penghitung neto yang juga memperhitungkan diskon,
	 * DetailKegiatan terbaru, dan item bukan-tagihan. Sebaliknya, rincian historis dapat
	 * masih berisi nominal bruto. Selisih itu sebelumnya membuat layar terlihat lunas
	 * tetapi PDF kembali menampilkan sisa tagihan (contoh: Rp350.000). Agar audit tetap
	 * transparan, selisih tidak disembunyikan atau dipaksakan ke salah satu item; laporan
	 * menambah satu baris "Selisih Tagihan" sehingga total rincian sama persis
	 * dengan Tagihan dan Dibayar pada ringkasan.</p>
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<Map<String, Serializable>> selaraskanSnapshotDenganRingkasan(Kegiatan kegiatan,
			Collection rincianLayar) {
		List<Map<String, Serializable>> hasil = new ArrayList<Map<String, Serializable>>();
		double totalTagihanRincian = 0.0;
		double totalDibayarRincian = 0.0;
		java.util.LinkedHashSet<String> itemDenganSelisih = new java.util.LinkedHashSet<String>();

		if (rincianLayar != null) {
			for (Object baris : rincianLayar) {
				if (!(baris instanceof Map)) {
					continue;
				}
				Map<String, Serializable> salinan = new HashMap<String, Serializable>((Map) baris);
				hasil.add(salinan);
				if (salinan.get("biaya") instanceof Number) {
					totalTagihanRincian += ((Number) salinan.get("biaya")).doubleValue();
				}
				if (salinan.get("dibayar") instanceof Number) {
					totalDibayarRincian += ((Number) salinan.get("dibayar")).doubleValue();
				}
				if (salinan.get("sisa") instanceof Number
						&& Math.abs(((Number) salinan.get("sisa")).doubleValue()) > 0.1
						&& salinan.get("item_biaya") != null) {
					itemDenganSelisih.add(salinan.get("item_biaya").toString());
				}
			}
		}

		if (kegiatan == null) {
			return hasil;
		}

		Double tagihanRingkasan = ais.action.master.helper.KegiatanPersistenceHelper
				.hitungTagihanSegarKonsisten(kegiatan);
		if (tagihanRingkasan == null || tagihanRingkasan.doubleValue() <= 0.1) {
			tagihanRingkasan = kegiatan.hitungTagihan();
		}
		Double dibayarRingkasan = kegiatan.hitungDibayar();
		double selisihTagihan = (tagihanRingkasan == null ? 0.0 : tagihanRingkasan.doubleValue())
				- totalTagihanRincian;
		double selisihDibayar = (dibayarRingkasan == null ? 0.0 : dibayarRingkasan.doubleValue())
				- totalDibayarRincian;

		if (Math.abs(selisihTagihan) <= 0.1 && Math.abs(selisihDibayar) <= 0.1) {
			return hasil;
		}

		Map<String, Serializable> acuan = hasil.isEmpty() ? null : hasil.get(0);
		Map<String, Serializable> penyesuaian = new HashMap<String, Serializable>();
		penyesuaian.put("kode", "ADJ");
		StringBuilder namaSelisih = new StringBuilder("Selisih Tagihan");
		if (!itemDenganSelisih.isEmpty()) {
			namaSelisih.append(" (");
			int panjangMaksimal = 120;
			for (String namaItem : itemDenganSelisih) {
				String pemisah = namaSelisih.charAt(namaSelisih.length() - 1) == '(' ? "" : ", ";
				if (namaSelisih.length() + pemisah.length() + namaItem.length() + 1 > panjangMaksimal) {
					namaSelisih.append(pemisah).append("item lainnya");
					break;
				}
				namaSelisih.append(pemisah).append(namaItem);
			}
			namaSelisih.append(")");
		}
		penyesuaian.put("item_biaya", namaSelisih.toString());
		penyesuaian.put("biaya", Double.valueOf(selisihTagihan));
		penyesuaian.put("dibayar", Double.valueOf(selisihDibayar));
		penyesuaian.put("sisa", Double.valueOf(selisihTagihan - selisihDibayar));
		penyesuaian.put("semester", acuan != null && acuan.get("semester") != null
				? acuan.get("semester") : kegiatan.getSemster());
		penyesuaian.put("tahun_ajaran", acuan != null && acuan.get("tahun_ajaran") != null
				? acuan.get("tahun_ajaran") : kegiatan.getTahunAkademik());
		penyesuaian.put("nama_kegiatan", kegiatan.getJenisKegiatan() == null ? ""
				: kegiatan.getJenisKegiatan().getNamaKegiatan());
		penyesuaian.put("nama_kegiatan_semester",
				(kegiatan.getJenisKegiatan() == null ? "" : kegiatan.getJenisKegiatan().getNamaKegiatan())
						+ "-" + kegiatan.getSemster());
		hasil.add(penyesuaian);
		return hasil;
	}

	public static File resizeImage(File file) throws Exception {
		File fileKecil = new File(file.getParentFile().getAbsolutePath() + "/kecil_" + file.getName());
		if (!fileKecil.exists()) {

			BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(file);
			if (originalImage == null) {
				return file; // gambar tak valid / terlalu besar -> pakai berkas asli (cegah OOM)
			}
			int IMG_HEIGHT = 323;
			int IMG_WIDTH = (int) (originalImage.getWidth() * ((IMG_HEIGHT * 1.0) / (originalImage.getHeight() * 1.0)));
			int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
			BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
			Graphics2D g = resizedImage.createGraphics();
			g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
			g.dispose();

			ImageIO.write(resizedImage, "jpg", fileKecil);
		}
		return fileKecil;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File cetakBuktipembayaranMahasiswa(Kegiatan kegiatan, boolean kirim) {
		return cetakBuktipembayaranMahasiswa(kegiatan, kirim, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File cetakBuktipembayaranMahasiswa(Kegiatan kegiatan, boolean kirim,
			List<Map<String, Serializable>> rincianLayar) {
		try {
			// Samakan tagihan tersimpan dengan tampilan grid layar sebelum mencetak: item biaya
			// ber-rumus KRS (mis. UTS/UAS/SKS) yang nilainya tidak diinput manual dihitung ulang
			// mengikuti KRS terkini, lalu kolom (tagihan/amount/amount_terhutang) yang dibaca PDF
			// diperbarui. Hanya saat ada session web aktif (cetak interaktif), bukan kirim email
			// terjadwal, agar tidak ada session yang menggantung di thread non-web.
			if (rincianLayar == null && kegiatan != null
					&& kegiatan.getId() != null && kegiatan.getMahasiswa() != null
					&& Sessions.getCurrent() != null) {
				try {
					ais.action.master.helper.KegiatanPersistenceHelper.segarkanTagihanLive(kegiatan.getId());
				} catch (Exception eSegar) {
					Common.tampilErrorJikaAdmin(eSegar);
				}
			}
			String fileReport = "Bukti_Pembayaran_Mahasiswa_tagihan";
			if (Common.bolehKonfigurasi("bukti_pembayaran_berdasarkan_sejarah_pembayaran", Konfigurasi.TIDAK_AKTIF)) {
				fileReport = "Bukti_Pembayaran_Mahasiswa_Berdasar_sejarah";
			} else {
				List<CicilanPembayaran> cicilanPembayarans = kegiatan.ambilCicilan();
				if (cicilanPembayarans != null) {
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
							fileReport = "Bukti_Pembayaran_Mahasiswa";
							break;
						}
					}
				}
			}

			Map parameters = ais.common.HashMapGenerator.getRand();

			// ISOLASI SESSION: Hanya dibuka jika dibutuhkan dan DIJAMIN ditutup
			if (kegiatan.getValidator() != null && !kegiatan.getValidator().equals("-")) {
				Session session = null;
				try {
					session = ais.action.report.Report.openNativeSession();
					Pegawai petugas = (Pegawai) session.createCriteria(Pegawai.class)
							.add(Restrictions.ilike("nama", kegiatan.getValidator(), MatchMode.EXACT))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1).uniqueResult();

					if (petugas == null) {
						Long petugasId = (Long) session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.property("pegawai.id"))
								.add(Restrictions.isNotNull("pegawai"))
								.add(Restrictions.ilike("userNama", kegiatan.getValidator(), MatchMode.EXACT))
								.setMaxResults(1).uniqueResult();
						if (petugasId != null) {
							petugas = (Pegawai) session.get(Pegawai.class, petugasId);
						}
					}

					if (petugas != null) {
						LampiranLain lam = LampiranLain.ambil(petugas.getId(), LampiranLain.TTD_PEGAWAI);
						if (lam == null && petugas.getDosen() != null) {
							lam = LampiranLain.ambil(petugas.getDosen().getId(), LampiranLain.TTD_DOSEN);
						}

						if (lam != null && lam.getNama() != null) {
							String namaFile = lam.getNama().toLowerCase();
							if (namaFile.endsWith(".jpg") || namaFile.endsWith(".png") || namaFile.endsWith(".jpeg")
									|| namaFile.endsWith(".gif") || namaFile.endsWith(".tif")
									|| namaFile.endsWith(".bmp")) {
								try {
									parameters.put("ttd_petugas", lam.ambilFile().getAbsolutePath());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:312");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:318");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				} finally {
					ais.action.report.Report.closeNativeSession(session);
				}
			}

			try {
				Fakultas fakultas = kegiatan.getMahasiswa() != null ? kegiatan.getMahasiswa().getJurusan().getFakultas()
						: (kegiatan.getCalonMahasiswa().getProdiLulus() != null
								? kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas()
								: kegiatan.getCalonMahasiswa().getProdi1().getFakultas());
				LampiranLain kop = LampiranLain.ambil(false, fakultas.getId(), LampiranLain.KOP_FAKULTAS);
				if (kop != null) {
					File fileKop = kop.ambilFile();
					if (fileKop != null && fileKop.exists()) {
						parameters.put("KOP_FAKULTAS", fileKop.getAbsolutePath());
						parameters.put("KOP_FAKULTAS_" + fakultas.getId(), fileKop.getAbsolutePath());
						parameters.put("KOP_FAKULTAS_" + fakultas.getNama(), fileKop.getAbsolutePath());
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:338");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

			parameters.put("tanggal", kegiatan.getTanggal());
			parameters.put("kegiatan", kegiatan.getId());
			parameters.put("kasir", kegiatan.getValidator());

			String reportPath = Common.ambilREAL_PATH_REPORT();
			File myfilebarcode1 = new File(
					reportPath + "/crcode_" + URLEncoder.encode(kegiatan.getValidator(), "UTF-8") + ".png");

			if (!myfilebarcode1.exists()) {
				BarcodeCommon.generateCRCode(kegiatan.getValidator(), myfilebarcode1);
			}
			parameters.put("cr_code", myfilebarcode1.getAbsolutePath());
			parameters.put("qr_code", myfilebarcode1.getAbsolutePath());

			List<Map> maps = new ArrayList<Map>();
			List<Long> detailKegiatans = new ArrayList<Long>();
			boolean gunakanRincianLayar = "Bukti_Pembayaran_Mahasiswa_tagihan".equals(fileReport)
					&& rincianLayar != null;

			if (!gunakanRincianLayar)
				for (DetailKegiatan detailKegiatan : kegiatan.ambilDetailKegiatan(true)) {
				detailKegiatans.add(detailKegiatan.getId());

				Double jumlah = detailKegiatan.getBiaya();
				if (detailKegiatan.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					jumlah = -Math.abs(jumlah);
				}
				DetailBiaya detailBiaya = detailKegiatan.getDetailBiaya();

				if (kegiatan.getMahasiswa() != null
						&& detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)) {
					detailBiaya.updateKeterangan(kegiatan.getMahasiswa(), kegiatan.getSemster());
				}

				// Selaraskan dengan tampilan grid layar: item biaya ber-rumus KRS yang nilainya
				// tidak diinput manual (nilaiBisaDiubah=false), mis. UTS/UAS/SKS, dihitung ulang
				// mengikuti KRS terkini (bisa 0 bila tidak ada matakuliah terkait). Item flat,
				// tunggakan, parameter tambahan, dan item yang bisa diubah manual tidak diubah.
				if (kegiatan.getMahasiswa() != null && detailBiaya.getItemBiaya() != null
						&& !Boolean.TRUE.equals(detailBiaya.getItemBiaya().getNilaiBisaDiubah())
						&& !detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)
						&& !detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
						&& detailBiaya.getItemBiaya().getParameterTambahan() == null) {
					detailBiaya.updateKeterangan(kegiatan.getMahasiswa(), kegiatan.getSemster());
					Double live = Kegiatan.ambilJumlahTagihan((DetailKegiatan) null, kegiatan, detailBiaya, false);
					if (live != null) {
						jumlah = live;
					}
				}

				Double biaya = detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU)
						? detailBiaya.getTunggakanLalu()
						: jumlah;

				if (biaya.intValue() != 0) {
					Map map = new HashMap();
					map.put("kode", detailKegiatan.getItemBiaya().getKode());

					// OPTIMASI: Menggunakan StringBuilder untuk efisiensi RAM
					StringBuilder ibName = new StringBuilder(detailKegiatan.getItemBiaya().getNama());
					if (detailBiaya.getDetailSettingBiaya() != null
							&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
							&& detailBiaya.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1) {
						ibName.append(", ke-").append(detailBiaya.getBayarKe());
					}
					map.put("item_biaya", ibName.toString());
					map.put("tagihan", biaya);
					map.put("biaya", biaya - detailKegiatan.getDiskon());
					map.put("diskon", detailKegiatan.getDiskon());
					map.put("nim", kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim());
					map.put("nama_mahasiswa", kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama());
					map.put("nama_fakultas", kegiatan.getMahasiswa() == null ? ""
							: kegiatan.getMahasiswa().getJurusan().getFakultas().getNama());
					map.put("nama_jurusan",
							kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getJurusan().getNama());
					map.put("program", kegiatan.getMahasiswa() == null ? "" : kegiatan.getProgram());
					map.put("semester", kegiatan.getSemster());
					map.put("tahun_ajaran", kegiatan.getTahunAkademik());
					map.put("nama_kegiatan", kegiatan.getJenisKegiatan().getNamaKegiatan());
					map.put("ref_number", kegiatan.getRefNumber());
					map.put("validator", kegiatan.getValidator());

					if (kegiatan.getMahasiswa() != null && !detailKegiatan.getItemBiaya().getPenghitungan()
							.equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)) {
						map.put("keterangan1", detailKegiatan.getDetailBiaya().getKeterangan());
					} else {
						map.put("keterangan1", detailKegiatan.getKeterangan());
					}

					map.put("uraian", detailKegiatan.getUraian());
					map.put("keterangan", kegiatan.getKeterangan());
					maps.add(map);
				}
			}

			if (gunakanRincianLayar) {
				maps = buatParameterBuktiDariRincianLayar(kegiatan, rincianLayar);
			}

			if (detailKegiatans.isEmpty()) {
				detailKegiatans.add(-1L);
			}
			parameters.put("detailKegiatans", detailKegiatans.toArray());

			if (!Common.bolehKonfigurasi("bukti_pembayaran_berdasarkan_sejarah_pembayaran", Konfigurasi.TIDAK_AKTIF)) {
				parameters.put("maps", maps);
			}

			File file;
			if (Sessions.getCurrent() != null) {
				file = Report.generatePDFReport(Report.PDF, parameters, fileReport,
						ais.ui.util.WaktuUtil.getDate(), null, Common.locale, null);
			} else {
				file = Report.generateFileReportSimple(Report.PDF, parameters, fileReport);
			}

			if (kirim && file != null) {
				CommonEmail.infoBayar(kegiatan, file);
			}
			return file;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:456");
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	// KE-FIX (race condition nimkey — genNim dipicu ASYNC via AuditListener$8, bisa berjalan
	// nyaris bersamaan untuk beberapa calon mahasiswa): algoritma NimGenerator (mayoritas
	// implementasi) membangkitkan nomor urut via cari-nomor-terbesar-lalu+1 tanpa lock. Dua
	// eksekusi genNim yang overlap bisa menghasilkan nim/nimkey SAMA sebelum salah satunya
	// sempat commit, sehingga yang kedua gagal "duplicate key value violates unique constraint
	// mahasiswa_nimkey_key". Perbaikan: retry beberapa kali khusus untuk pelanggaran constraint
	// nimkey ini — regenerate ulang nim (query max+1 akan melihat baris pesaing yang sudah
	// commit di percobaan sebelumnya) lalu simpan ulang. Algoritma dasar pembangkitan NIM untuk
	// kasus normal (tanpa konflik) TIDAK diubah.
	private static final int MAX_RETRY_GEN_NIM = 5;

	private static boolean isNimkeyConstraintViolation(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String className = t.getClass() == null ? "" : t.getClass().getName();
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
			if ((className.indexOf("ConstraintViolationException") >= 0
					|| className.indexOf("SQLException") >= 0
					|| className.indexOf("GenericJDBCException") >= 0)
					&& (msg.indexOf("nimkey") >= 0
							|| msg.indexOf("mahasiswa_nimkey_key") >= 0)) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private static void genNim(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getCalonMahasiswa() == null) {
			return;
		}

		BiodataCalonMahasiswa calonMahasiswaAwal = kegiatan.getCalonMahasiswa();
		if (calonMahasiswaAwal.getMahasiswa() != null) {
			return;
		}

		boolean wajibBayar = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_melunasi_pembayaran_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);

		if (wajibBayar && kegiatan.getDibayar() <= 100000.0) {
			return;
		}

		Mahasiswa mahasiswa = null;
		BiodataCalonMahasiswa calonMahasiswa = calonMahasiswaAwal;
		Exception lastException = null;

		for (int attempt = 1; attempt <= MAX_RETRY_GEN_NIM; attempt++) {
			Session session = null;
			Transaction tx = null;
			mahasiswa = null;
			calonMahasiswa = calonMahasiswaAwal;
			lastException = null;

			try {
				System.out.println("=== genNim => ID Kegiatan: " + kegiatan.getId()
						+ (attempt > 1 ? (" (percobaan ke-" + attempt + ")") : ""));

				session = ais.action.report.Report.openNativeSession();
				tx = session.beginTransaction();

				if (calonMahasiswaAwal.getId() != null) {
					calonMahasiswa = (BiodataCalonMahasiswa) session.get(BiodataCalonMahasiswa.class,
							calonMahasiswaAwal.getId());
				}
				if (calonMahasiswa == null) {
					return;
				}
				if (calonMahasiswa.getMahasiswa() != null) {
					calonMahasiswaAwal.setMahasiswa(calonMahasiswa.getMahasiswa());
					return;
				}

				String nimGenClassName = Common
						.getKonfigurasi("class_untuk_generate_nim", "ais.action.master.pmb.nim.DefaultNimGenerator")
						.getNilai().trim();
				NimGenerator nimGenerator = (NimGenerator) Class.forName(nimGenClassName).newInstance();

				String nim = CommonPMB.ambilNimTersimpanDariRiwayatPmb(session, calonMahasiswa,
						calonMahasiswa.getMahasiswa());
				if (nim == null || nim.trim().isEmpty()) {
					nim = nimGenerator.generateNim(calonMahasiswa);
				}
				System.out.println("nim => " + nim);

				mahasiswa = CommonPMB.saveMahasiswa(session, calonMahasiswa, nim, false);

				if (tx != null && tx.isActive() && !tx.wasCommitted() && !tx.wasRolledBack()) {
					tx.commit();
				}
				tx = null;

				calonMahasiswa.setMahasiswa(mahasiswa);
				calonMahasiswaAwal.setMahasiswa(mahasiswa);
				if (kegiatan.getCalonMahasiswa() != null) {
					kegiatan.getCalonMahasiswa().setMahasiswa(mahasiswa);
				}
				// Berhasil — keluar dari loop retry.
				lastException = null;
				break;
			} catch (Exception e) {
				if (tx != null) {
					try {
						if (tx.isActive() && !tx.wasCommitted() && !tx.wasRolledBack()) {
							tx.rollback();
						}
					} catch (Exception ex) {
						System.err.println("Gagal rollback transaction genNim: " + ex.getMessage());
					}
				}
				lastException = e;
				if (isNimkeyConstraintViolation(e) && attempt < MAX_RETRY_GEN_NIM) {
					System.out.println("[genNim] Konflik nimkey (percobaan ke-" + attempt
							+ "), regenerate ulang NIM dan retry...");
					try {
						Thread.sleep(150L + (long) (Math.random() * 200L));
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
					// lanjut ke iterasi berikutnya (retry)
				} else {
					// Bukan konflik nimkey, atau sudah habis jatah retry — menyerah, log, jangan
					// ganggu thread audit latar (AuditListener$8) lainnya.
					Common.tampilErrorJikaAdmin(e);
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit genNim habis retry (kegiatan=" + kegiatan.getId() + ")");
					return;
				}
			} finally {
				ais.action.report.Report.closeNativeSession(session);
			}
		}

		if (lastException != null) {
			// Tidak seharusnya sampai sini (sudah ditangani di atas), tapi jaga-jaga agar tidak
			// melempar exception ke pemanggil (AuditListener$8) bila retry tetap habis.
			return;
		}

		try {
			if (mahasiswa != null && mahasiswa.getId() != null && calonMahasiswa != null
					&& calonMahasiswa.getId() != null) {
				CommonPMB.copyLampiran(calonMahasiswa, mahasiswa);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public static boolean checkGenNim(Kegiatan kegiatan) {
		return checkGenNim(kegiatan, true);
	}

	public static boolean checkGenNim(Kegiatan kegiatan, boolean debug) {
		if (debug)
			System.out.println("[DEBUG] === Memulai proses checkGenNim ===");

		// Proteksi (null-check) untuk mencegah NullPointerException
		if (kegiatan == null || kegiatan.getCalonMahasiswa() == null
				|| kegiatan.getCalonMahasiswa().getProdiLulus() == null) {
			if (debug)
				System.out.println(
						"[DEBUG] Validasi awal gagal: kegiatan, calon mahasiswa, atau prodi lulus bernilai null. Keluar dari method.");
			return false;
		}

		if (debug)
			System.out.println("[DEBUG] Data awal lolos validasi (Calon Mahasiswa ID: "
					+ kegiatan.getCalonMahasiswa().getId() + ").");

		Session session = null;
		Transaction tx = null;

		boolean wajibBayarPersen = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_membayar_persen_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);
		boolean wajibBayar = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_melunasi_pembayaran_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);
		boolean berhasil = false;
		try {
			if (debug)
				System.out.println("[DEBUG] Membuka session Hibernate lokal...");
			session = ais.action.report.Report.openNativeSession();

			// 1. Evaluasi apakah mahasiswa memenuhi syarat pembayaran (shouldGenNim)
			boolean shouldGenNim = false;

			if (wajibBayarPersen) {
				if (debug)
					System.out.println("[DEBUG] Cek konfigurasi: pembayaran daftar ulang berdasarkan PERSENTASE...");
				Double minPersen = 10.0;
				try {
					minPersen = Double.parseDouble(
							Common.getKonfigurasi("minimal_jumlah_persen_pembayaran_mahasiswa_otomatis_mendapatkan_nim",
									"0", kegiatan.getCalonMahasiswa().getProgram(),
									kegiatan.getCalonMahasiswa().getProdiLulus(),
									kegiatan.getCalonMahasiswa().getTahun().toString()).getNilai().trim());
				} catch (Exception e) {
					if (debug)
						System.out.println("[DEBUG] Gagal parsing minPersen, menggunakan default 10.0");
				}

				if (debug)
					System.out.println("[DEBUG] Persentase Lunas saat ini: " + kegiatan.getPersentaseLunas()
							+ " | Syarat Min Persen: " + minPersen);
				if (kegiatan.getPersentaseLunas() >= minPersen) {
					shouldGenNim = true;
					if (debug)
						System.out.println("[DEBUG] -> Syarat persentase terpenuhi. shouldGenNim = true");
				} else {
					if (debug)
						System.out.println("[DEBUG] -> Syarat persentase belum terpenuhi.");
				}
			}

			if (!shouldGenNim) {
				if (debug)
					System.out.println("[DEBUG] Cek konfigurasi: pelunasan berdasarkan KODE ITEM BIAYA / NOMINAL...");
				if (wajibBayar) {

					String kode = Common
							.getKonfigurasi("kode_item_biaya_untuk_pembayaran_mahasiswa_baru_otomatis_dapat_nim", "",
									kegiatan.getCalonMahasiswa().getProgram(),
									kegiatan.getCalonMahasiswa().getProdiLulus(),
									kegiatan.getCalonMahasiswa().getTahun().toString())
							.getNilai().trim();

					if (debug)
						System.out.println("[DEBUG] Kode item biaya ditemukan: '" + kode + "'");

					if (!kode.isEmpty()) {
						int countCicilan = ((Number) session.createCriteria(CicilanPembayaran.class)
								.createAlias("itemBiaya", "itemBiaya").add(Restrictions.eq("kegiatan.id", kegiatan.getId()))
								.add(Restrictions.ilike("itemBiaya.kode", kode, MatchMode.EXACT))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();

						if (debug)
							System.out.println("[DEBUG] Jumlah cicilan untuk kode tersebut: " + countCicilan);

						if (countCicilan > 0) {
							shouldGenNim = true;
							if (debug)
								System.out.println("[DEBUG] -> Syarat cicilan kode terpenuhi. shouldGenNim = true");
						}
					} else {
						Double minNominal = 10.0;
						try {
							minNominal = Double.parseDouble(Common
									.getKonfigurasi("minimal_jumlah_pembayaran_mahasiswa_otomatis_mendapatkan_nim", "0",
											kegiatan.getCalonMahasiswa().getProgram(),
											kegiatan.getCalonMahasiswa().getProdiLulus(),
											kegiatan.getCalonMahasiswa().getTahun().toString())
									.getNilai().trim());
						} catch (Exception e) {
							if (debug)
								System.out.println("[DEBUG] Gagal parsing minNominal, menggunakan default 10.0");
						}

						if (debug)
							System.out.println("[DEBUG] Nominal dibayar saat ini: " + kegiatan.getDibayar()
									+ " | Syarat Min Nominal: " + minNominal);
						if (kegiatan.getDibayar() >= minNominal) {
							shouldGenNim = true;
							if (debug)
								System.out.println("[DEBUG] -> Syarat nominal terpenuhi. shouldGenNim = true");
						} else {
							if (debug)
								System.out.println("[DEBUG] -> Syarat nominal belum terpenuhi.");
						}
					}
				}
			}

			// 2. Eksekusi aksi berdasarkan hasil evaluasi
			boolean hasMahasiswa = kegiatan.getCalonMahasiswa().getMahasiswa() != null;
			if (debug)
				System.out.println("[DEBUG] Hasil Evaluasi Akhir -> shouldGenNim: " + shouldGenNim + ", hasMahasiswa: "
						+ hasMahasiswa);

			if (shouldGenNim && !hasMahasiswa) {
				if (debug)
					System.out
							.println("[DEBUG] AKSI: Generate NIM (Syarat lunas terpenuhi & belum ada data mahasiswa)");
				genNim(kegiatan);
				if (debug)
					System.out.println("[DEBUG] Generate NIM selesai dipanggil.");

			} else if (!shouldGenNim && hasMahasiswa && wajibBayar) {
				if (debug)
					System.out.println(
							"[DEBUG] AKSI: Hapus Data Mahasiswa (Syarat lunas BELUM terpenuhi, tapi sudah ada data mahasiswa)");
				tx = session.beginTransaction();

				Long idMahasiswa = kegiatan.getCalonMahasiswa().getMahasiswa().getId();
				if (debug)
					System.out.println("[DEBUG] ID Mahasiswa yang akan dihapus: " + idMahasiswa);
				
				
				// 1. Hapus dulu di tabel online_users yang merujuk ke id tersebut
				//String deleteOnlineUsers = "DELETE FROM OnlineUsers ou WHERE ou.mahasiswa.id = :id";
				//session.createQuery(deleteOnlineUsers).setParameter("id", idMahasiswa).executeUpdate();

				String hql = "DELETE FROM Mahasiswa m WHERE m.id = :id AND NOT EXISTS "
						+ "(SELECT 1 FROM Detailperkuliahan dp WHERE dp.mahasiswa.id = m.id)";

				int deletedCount = session.createQuery(hql).setParameter("id", idMahasiswa).executeUpdate();

				berhasil = deletedCount > 0;

				if (debug)
					System.out.println("[DEBUG] Eksekusi delete selesai. Jumlah row terhapus: " + deletedCount);

				if (deletedCount > 0) {
					if (debug)
						System.out.println("[DEBUG] Berhasil dihapus dari database. Melepas relasi object di memori.");
					kegiatan.getCalonMahasiswa().setMahasiswa(null);
				} else {
					if (debug)
						System.out.println(
								"[DEBUG] Gagal menghapus mahasiswa. Kemungkinan id tidak ditemukan atau mahasiswa sudah terdaftar di DetailPerkuliahan.");
				}

				tx.commit();
				if (debug)
					System.out.println("[DEBUG] Transaksi commit sukses.");
			} else {
				if (debug)
					System.out.println(
							"[DEBUG] AKSI: Tidak ada tindakan yang dilakukan (Kondisi sudah sesuai / tidak memenuhi syarat untuk ubah state).");
			}

		} catch (Exception e) {
			if (debug) {
				System.out.println("[DEBUG] TERJADI EXCEPTION:");
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:729");
			}
			if (tx != null && tx.isActive()) {
				if (debug)
					System.out.println("[DEBUG] Melakukan rollback transaksi karena error.");
				tx.rollback();
			}
		} finally {
			if (debug)
				System.out.println("[DEBUG] Masuk ke blok finally, membersihkan session...");
			ais.action.report.Report.closeNativeSession(session);
			if (debug)
				System.out.println("[DEBUG] === Selesai proses checkGenNim ===");
		}
		if (debug)
			System.out.println("[DEBUG] === Selesai proses berhasil -> " + berhasil + " ===");
		return berhasil;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File cetakBuktipembayaranCalonMahasiswa(Kegiatan kegiatan, boolean kirim) {
		return cetakBuktipembayaranCalonMahasiswa(kegiatan, kirim, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File cetakBuktipembayaranCalonMahasiswa(Kegiatan kegiatan, boolean kirim,
			List<Map<String, Serializable>> rincianLayar) {
		Session sessionLocal = null;
		Transaction tx = null;
		File file = null;
		// KE-FIX (TransientObjectException / ConstraintViolationException FK pembayaran_daftar_ulang):
		// kegiatan bisa datang dari simpanPembayaranCalonMahasiswa() yang gagal tersimpan (id masih
		// null) -- melanjutkan ke bawah akan menulis FK yang menunjuk baris kegiatan yang belum/tidak
		// ada di DB. Hentikan di sini drpd membiarkan exception generik yang membingungkan.
		if (kegiatan == null || kegiatan.getId() == null) {
			ais.common.ErrorAuditUtil.record(
					new IllegalStateException("cetakBuktipembayaranCalonMahasiswa dipanggil dengan kegiatan null/belum tersimpan"),
					"auto-audit src/ais/action/report/CommonReportHelper.java:cetakBuktipembayaranCalonMahasiswa-guard");
			return null;
		}
		// FASE 1: UPDATE STATUS PEMBAYARAN DAN GENERATE NIM
		try {
			BiodataCalonMahasiswa bio = kegiatan.getCalonMahasiswa();
			JenisKegiatan jenisKegiatan = kegiatan.getJenisKegiatan();

			if (bio != null && jenisKegiatan != null) {
				sessionLocal = ais.action.report.Report.openNativeSession();
				tx = sessionLocal.beginTransaction();
				boolean isUpdate = false;

				if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
					bio.setPembayaranRegistrasi(kegiatan);
					sessionLocal.update(bio);
					isUpdate = true;
				} else if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
					bio.setPembayaranDaftarUlang(kegiatan);
					sessionLocal.update(bio);
					isUpdate = true;

					// Logika Otomatis NIM
					CommonReportHelper.checkGenNim(kegiatan);
				}

				if (isUpdate) {
					if (tx != null && tx.isActive()) {
						tx.commit();
					}
					tx = null;
				} else {
					tx.rollback();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:788");
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:792");
				}
			}
		} finally {
			if (sessionLocal != null && sessionLocal.isOpen()) {
				try {
					sessionLocal.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:799");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		// FASE 2: MEMBUAT PARAMETER CETAK PDF
		try {
			String fileReport = "Bukti_Pembayaran_Calon_Mahasiswa_tagihan";
			if (Common.bolehKonfigurasi("bukti_pembayaran_berdasarkan_sejarah_pembayaran", Konfigurasi.TIDAK_AKTIF)) {
				fileReport = "Bukti_Pembayaran_Calon_Mahasiswa_Berdasar_sejarah";
			} else {
				List<CicilanPembayaran> cicilanPembayarans = kegiatan.ambilCicilan();
				if (cicilanPembayarans != null) {
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
							fileReport = "Bukti_Pembayaran_Calon_Mahasiswa";
							break;
						}
					}
				}
			}

			Map parameters = ais.common.HashMapGenerator.getRand();

			if (kegiatan.getValidator() != null && !kegiatan.getValidator().equals("-")) {
				Session sessionPetugas = null;
				try {
					sessionPetugas = ais.action.report.Report.openNativeSession();
					Pegawai petugas = (Pegawai) sessionPetugas.createCriteria(Pegawai.class)
							.add(Restrictions.ilike("nama", kegiatan.getValidator(), MatchMode.EXACT))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1).uniqueResult();

					if (petugas == null) {
						Long petugasId = (Long) sessionPetugas.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.property("pegawai.id"))
								.add(Restrictions.isNotNull("pegawai"))
								.add(Restrictions.ilike("userNama", kegiatan.getValidator(), MatchMode.EXACT))
								.setMaxResults(1).uniqueResult();
						if (petugasId != null) {
							petugas = (Pegawai) sessionPetugas.get(Pegawai.class, petugasId);
						}
					}

					if (petugas != null) {
						LampiranLain lam = LampiranLain.ambil(petugas.getId(), LampiranLain.TTD_PEGAWAI);
						if (lam == null && petugas.getDosen() != null) {
							lam = LampiranLain.ambil(petugas.getDosen().getId(), LampiranLain.TTD_DOSEN);
						}

						if (lam != null && lam.getNama() != null) {
							String namaFile = lam.getNama().toLowerCase();
							if (namaFile.endsWith(".jpg") || namaFile.endsWith(".png") || namaFile.endsWith(".jpeg")
									|| namaFile.endsWith(".gif") || namaFile.endsWith(".bmp")) {
								try {
									parameters.put("ttd_petugas", lam.ambilFile().getAbsolutePath());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:856");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:862");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				} finally {
					ais.action.report.Report.closeNativeSession(sessionPetugas);
				}
			}

			try {
				Fakultas fakultas = kegiatan.getMahasiswa() != null ? kegiatan.getMahasiswa().getJurusan().getFakultas()
						: (kegiatan.getCalonMahasiswa().getProdiLulus() != null
								? kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas()
								: kegiatan.getCalonMahasiswa().getProdi1().getFakultas());
				LampiranLain kop = LampiranLain.ambil(false, fakultas.getId(), LampiranLain.KOP_FAKULTAS);
				if (kop != null) {
					File fileKop = kop.ambilFile();
					if (fileKop != null && fileKop.exists()) {
						parameters.put("KOP_FAKULTAS", fileKop.getAbsolutePath());
						parameters.put("KOP_FAKULTAS_" + fakultas.getId(), fileKop.getAbsolutePath());
						parameters.put("KOP_FAKULTAS_" + fakultas.getNama(), fileKop.getAbsolutePath());
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:883");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

			parameters.put("tanggal", kegiatan.getTanggal());
			parameters.put("kegiatan", kegiatan.getId());

			List<Map> maps = new ArrayList<Map>();
			List<Long> detailKegiatans = new ArrayList<Long>();
			boolean gunakanRincianLayar = "Bukti_Pembayaran_Calon_Mahasiswa_tagihan".equals(fileReport)
					&& rincianLayar != null;

			if (!gunakanRincianLayar)
				for (DetailKegiatan detailKegiatan : kegiatan.ambilDetailKegiatan(true)) {
				detailKegiatans.add(detailKegiatan.getId());
				Double jumlah = detailKegiatan.getBiaya();

				if (detailKegiatan.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					jumlah = -Math.abs(jumlah);
				}

				Double biaya = detailKegiatan.getDetailBiaya().getItemBiaya().getPenghitungan().equals(
						ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU) ? detailKegiatan.getDetailBiaya().getTunggakanLalu()
								: jumlah;

				if (biaya.intValue() != 0) {
					Map map = new HashMap();
					map.put("kode", detailKegiatan.getItemBiaya().getKode());

					StringBuilder ibName = new StringBuilder(detailKegiatan.getItemBiaya().getNama());
					if (detailKegiatan.getDetailBiaya().getDetailSettingBiaya() != null
							&& detailKegiatan.getDetailBiaya().getDetailSettingBiaya().getSettingBiaya() != null
							&& detailKegiatan.getDetailBiaya().getDetailSettingBiaya().getSettingBiaya()
									.getJumlahPembayaran() > 1) {
						ibName.append(", ke-").append(detailKegiatan.getDetailBiaya().getBayarKe());
					}

					map.put("item_biaya", ibName.toString());
					map.put("tagihan", biaya);
					map.put("biaya", biaya - detailKegiatan.getDiskon());
					map.put("diskon", detailKegiatan.getDiskon());
					map.put("nim", kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNim());

					String nr = kegiatan.getCalonMahasiswa() == null ? ""
							: kegiatan.getCalonMahasiswa().getNoRegistrasi();
					if (kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getMahasiswa() != null) {
						nr += "(" + kegiatan.getCalonMahasiswa().getMahasiswa().getNim() + ")";
					}
					map.put("no_registrasi", nr);

					map.put("nama_mahasiswa",
							kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama());

					Jurusan jurusan = kegiatan.getCalonMahasiswa() == null ? null
							: kegiatan.getCalonMahasiswa().getProdi1();
					if (kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
						jurusan = kegiatan.getCalonMahasiswa().getProdiLulus();
					}

					map.put("nama_fakultas", jurusan == null ? "" : jurusan.getFakultas().getNama());
					map.put("nama_jurusan", jurusan == null ? "" : jurusan.getNama());
					map.put("program",
							kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getProgram());
					map.put("semester", kegiatan.getSemster());
					map.put("tahun_ajaran", kegiatan.getTahunAkademik());
					map.put("nama_kegiatan", kegiatan.getJenisKegiatan().getNamaKegiatan());
					map.put("ref_number", kegiatan.getRefNumber());
					map.put("validator", kegiatan.getValidator());

					if (kegiatan.getMahasiswa() != null && !detailKegiatan.getItemBiaya().getPenghitungan()
							.equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)) {
						map.put("keterangan1", detailKegiatan.getDetailBiaya().getKeterangan());
					} else {
						map.put("keterangan1", detailKegiatan.getKeterangan());
					}
					map.put("uraian", detailKegiatan.getUraian());
					map.put("keterangan", kegiatan.getKeterangan());
					maps.add(map);
				}
			}

			if (gunakanRincianLayar) {
				maps = buatParameterBuktiDariRincianLayar(kegiatan, rincianLayar);
			}

			if (detailKegiatans.isEmpty()) {
				detailKegiatans.add(-1L);
			}
			parameters.put("detailKegiatans", detailKegiatans.toArray());

			if (!Common.bolehKonfigurasi("bukti_pembayaran_berdasarkan_sejarah_pembayaran", Konfigurasi.TIDAK_AKTIF)) {
				parameters.put("maps", maps);
			}

			if (Sessions.getCurrent() != null) {
				file = Report.generatePDFReport(Report.PDF, parameters, fileReport,
						ais.ui.util.WaktuUtil.getDate(), null, Common.locale, null);
			} else {
				file = Report.generateFileReportSimple(Report.PDF, parameters, fileReport);
			}

			if (kirim && file != null) {
				CommonEmail.infoBayar(kegiatan, file);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:980");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Common Report Helper", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakSuratKeteranganLulus(CalonSiswa calonSiswa) throws Exception {
		if (!calonSiswa.getTelahDiterima()) {
			MyMessageboxConfig.show("Mohon maaf, calon siswa ini belum dinyatakan diterima. Langkah yang dapat dilakukan: (1) Pastikan proses seleksi/penerimaan sudah diselesaikan oleh Admin Penerimaan; (2) Periksa status calon siswa di menu Penerimaan Siswa; (3) Ulangi proses cetak setelah status diperbarui. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

//		try {
//			CetakRegistrasiAction.singkronkanDenganPembayaran(calonMahasiswa.getId(), new Label(), 0, 1);
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:996");
//			e.printStackTrace();
//		}
		Konfigurasi konfigurasi = Common.getKonfigurasi("informasi_telah_lulus",
				"Silahkan melakukan daftar ulang dengan melakukan pembayaran di bank dengan menunjukkan nomor ujian Anda.");

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("biodata_id", calonSiswa.getId());
		parameters.put("info", konfigurasi.getNilai());

		Common.insertProperty(CalonSiswa.class, calonSiswa, parameters, "bio", 1);
		JadwalUjianPSB jadwalUjianPSB = (JadwalUjianPSB) HibernateUtil.currentSession()
				.createCriteria(JadwalUjianPSB.class)
				.add(Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.setMaxResults(1).uniqueResult();
		if (jadwalUjianPSB != null) {
			Common.insertProperty(JadwalUjianPSB.class, jadwalUjianPSB, parameters, "jadwal", 0);
		}

		UjianPSB ujianPSB = (UjianPSB) HibernateUtil.currentSession().createCriteria(UjianPSB.class)
				.add(Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.setMaxResults(1).uniqueResult();
		if (ujianPSB != null) {
			Common.insertProperty(UjianPSB.class, ujianPSB, parameters, "ujian", 0);
		}

		calonSiswa.putPhoto(parameters);
		GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
		LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
		if (kop != null) {
			try {
				parameters.put("kop_file", kop.ambilFile().getAbsolutePath());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:1028");
				// TODO: handle exception
			}
		}

		parameters.put("file_laporan", URLEncoder
				.encode(calonSiswa.getNoRegistrasi() + " " + calonSiswa.getNama() + " Keterangan_Lulus", "UTF-8"));
		if (calonSiswa.getBahasa() != null) {
			parameters.put("currentLang", calonSiswa.getBahasa());
		}
		Report.generatePDFReport(Report.PDF, parameters, "sekolah/Keterangan_Lulus", ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map genSklMap(BiodataCalonMahasiswa calonMahasiswa) {
		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("biodata_id", calonMahasiswa.getId());
		Konfigurasi konfigurasi = Common.getKonfigurasi("informasi_telah_lulus",
				"Silahkan melakukan daftar ulang dengan melakukan pembayaran di bank dengan menunjukkan nomor ujian Anda.");

		if (calonMahasiswa.getBahasa() != null) {
			parameters.put("currentLang", calonMahasiswa.getBahasa());
		}
		parameters.put("info", konfigurasi.getNilai());
		Session session = HibernateUtil.getSessionFactory().openSession();
		Map ujianData = new HashMap();
		UjianPMB ujianPMB;
		try {
			Long ujianPMBId = (Long) session.createCriteria(RuangPaketPMB.class)
					.createAlias("ruangPMB", "ruangPMB").createAlias("ruangPMB.ujianPMB", "ujianPMB")
					.setProjection(Projections.property("ujianPMB.id"))
					.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).addOrder(Order.asc("id"))
					.setMaxResults(1).uniqueResult();
			/*
			 * Projection entity relasi dapat menghasilkan proxy yang pemilik session-nya
			 * berbeda. Ambil id lalu muat ulang entity pada session lokal ini agar seluruh
			 * getter jadwal aman diakses sebelum session ditutup.
			 */
			ujianPMB = ujianPMBId == null ? null : (UjianPMB) session.get(UjianPMB.class, ujianPMBId);
			if (ujianPMB == null) {
				ujianPMB = (UjianPMB) session.createCriteria(UjianPMB.class)
						.add(Restrictions.eq("gelombangPendaftaran", calonMahasiswa.getGelombangPendaftaran()))
						.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
			}
			if (ujianPMB != null) {
				ujianData.put("tanggalujian1", ujianPMB.getTanggalUjian1());
				ujianData.put("tanggalujian2", ujianPMB.getTanggalUjian2());
				ujianData.put("tanggalujian3", ujianPMB.getTanggalUjian3());
				ujianData.put("tanggalujian4", ujianPMB.getTanggalUjian4());
				ujianData.put("tanggalujian5", ujianPMB.getTanggalUjian5());
				ujianData.put("tanggalujian6", ujianPMB.getTanggalUjian6());
				ujianData.put("tanggalujian7", ujianPMB.getTanggalUjian7());
				ujianData.put("tanggalujian8", ujianPMB.getTanggalUjian8());
				ujianData.put("tanggalujian9", ujianPMB.getTanggalUjian9());
				ujianData.put("tanggalujian10", ujianPMB.getTanggalUjian10());
				ujianData.put("info", ujianPMB.getKeterangan());
				ujianData.put("tampilkanjadwalujiandikartuujian", ujianPMB.getTampilkanJadwalUjianDiKartuUjian());
				ujianData.put("lokasi", ujianPMB.getLokasi());
			} else {
				ujianData.put("lokasi", "Belum ditentukan");
				ujianData.put("info", konfigurasi.getNilai());
				ujianData.put("tampilkanjadwalujiandikartuujian", false);
			}
		} catch (RuntimeException e) {
			// Koneksi/statement DB terputus di tengah proses cetak (mis. pool c3p0 mengembalikan
			// koneksi basi setelah proses ini berjalan lama) -> pesan jelas utk admin, bukan cuma
			// stack trace teknis PSQLException. Tidak menelan error: proses cetak memang tak bisa
			// dilanjutkan tanpa data ujian PMB yang valid.
			if (ais.common.Common.isTransientKoneksiError(e)) {
				ais.common.ErrorAuditUtil.record(e,
						"koneksi database terputus saat proses cetak Surat Keterangan Lulus (genSklMap) untuk biodata id="
								+ calonMahasiswa.getId());
				throw new RuntimeException(
						"Koneksi database terputus saat proses cetak Surat Keterangan Lulus. Silakan coba lagi beberapa saat.",
						e);
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.clear(); } catch (Exception e) { }
				try { session.disconnect(); } catch (Exception e) { }
				try { session.close(); } catch (Exception e) { }
			}
		}
		parameters.putAll(ujianData);

		calonMahasiswa.putPhoto(parameters);

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ArrayList detailBiayas = new ArrayList();
			PembayaranUtil.getInstance();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
					calonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, calonMahasiswa.getProdiLulus(), 1,
					true);
			detailBiayas.addAll(detailBiayas1);

			PembayaranUtil.getInstance();
			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, calonMahasiswa,
					ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1, detailBiayas, true, false);

			Collection biayaBulanan = null;
			if (countPengaturanBulanan > 0) {
				biayaBulanan = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(calonMahasiswa, session, 1,
						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, true, false);
			}
			Collection dataBTagihan = biayaBulanan != null ? biayaBulanan : detailBiayas;
			Double biaya = 0.0;
			List<Map> maps = new ArrayList<Map>();
			Kegiatan kegiatan = calonMahasiswa.getPembayaranDaftarUlang();

			DetailBiaya detailBiaya = null;
			String desc = "";
			Double jumlah = 0.0;
			Double jumlah_sebelum_diskon = 0.0;
			Double jumlah_diskon = 0.0;
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;

			parameters.put("infotambahan", CariDataPesertaUjianAction.genInfo(calonMahasiswa, dataBTagihan));

			for (Object o : dataBTagihan) {
				Map map = new HashMap();

				Double nilai_sebelum_diskon = 0.0;

				String namaTagihan = "";

				if (kegiatan != null && kegiatan.getJadwalPembayaran() != null) {
					map.put("jadwal_nama", kegiatan.getJadwalPembayaran().getNama());

					if (kegiatan.getJadwalPembayaran().getStartDate() != null) {
						parameters.put("jadwal_mulai.formated1",
								Common.dateFormat6.get().format(kegiatan.getJadwalPembayaran().getStartDate()));
						parameters.put("jadwal_mulai.formated2",
								Common.dateFormat2.get().format(kegiatan.getJadwalPembayaran().getStartDate()));
						parameters.put("jadwal_mulai.formated3",
								Common.dateFormat51.get().format(kegiatan.getJadwalPembayaran().getStartDate()));
						parameters.put("jadwal_mulai.formated4",
								Common.timeFormat.get().format(kegiatan.getJadwalPembayaran().getStartDate()));
						parameters.put("jadwal_mulai.formated5",
								Common.dateFormat1.get().format(kegiatan.getJadwalPembayaran().getStartDate()));
					}

					if (kegiatan.getJadwalPembayaran().getEndDate() != null) {
						parameters.put("jadwal_sampai.formated1",
								Common.dateFormat6.get().format(kegiatan.getJadwalPembayaran().getEndDate()));
						parameters.put("jadwal_sampai.formated2",
								Common.dateFormat2.get().format(kegiatan.getJadwalPembayaran().getEndDate()));
						parameters.put("jadwal_sampai.formated3",
								Common.dateFormat51.get().format(kegiatan.getJadwalPembayaran().getEndDate()));
						parameters.put("jadwal_sampai.formated4",
								Common.timeFormat.get().format(kegiatan.getJadwalPembayaran().getEndDate()));
						parameters.put("jadwal_sampai.formated5",
								Common.dateFormat1.get().format(kegiatan.getJadwalPembayaran().getEndDate()));
					}
				}

				if (o instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
					detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
					jumlah = pengaturanPembayaranBulanan.getNominal();

					nilai_sebelum_diskon = pengaturanPembayaranBulanan.getNominal();

					jumlah_sebelum_diskon += nilai_sebelum_diskon;

					desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
							: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan();

					namaTagihan = detailBiaya.getNama() + ((detailBiaya.getDetailSettingBiaya() != null
							&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
							&& detailBiaya.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1)
									? " ke-" + detailBiaya.getBayarKe()
									: "");

				} else if (o instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) o;

					nilai_sebelum_diskon = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru();

					jumlah_sebelum_diskon += nilai_sebelum_diskon;

					desc = detailBiaya.getKeterangan();

					jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);

					namaTagihan = detailBiaya.getNama() + ((detailBiaya.getDetailSettingBiaya() != null
							&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
							&& detailBiaya.getDetailSettingBiaya().getSettingBiaya().getJumlahPembayaran() > 1)
									? " ke-" + detailBiaya.getBayarKe()
									: "");

				}

				Collection<DetailKegiatan> detailKegiatans = pengaturanPembayaranBulanan == null || kegiatan == null
						|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(true);

				DetailKegiatan detailKegiatan = kegiatan == null || kegiatan.getId() == null ? null
						: (pengaturanPembayaranBulanan != null
								? kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatans)
								: kegiatan.ambilSatuDetailKegiatan(detailBiaya, true));

				if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya != null
						&& detailBiaya.getItemBiaya() != null && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()

						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null
						&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
						&& !kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.isEmpty()
						&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
								.contains(detailBiaya.getItemBiaya().getId())) {
					desc += ", " + kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getNama();
				} else {

					if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi() != null
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
							&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
							&& !kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.ambilItemBiayaIds().isEmpty()
							&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.ambilItemBiayaIds().contains(detailBiaya.getItemBiaya().getId())

							&&

							(

							kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() == null
									|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() != null
											&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterMulai() <= kegiatan.getSemster())

							)

							&&

							(

							kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() == null
									|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() != null
											&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterSampai() >= kegiatan.getSemster())

							)

					) {

						desc += ", "
								+ kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getNama();

					}

					else if (kegiatan != null && kegiatan.getMahasiswa() != null
							&& kegiatan.getMahasiswa().getJenisSeleksi() != null
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
							&& !(detailKegiatan != null && detailKegiatan.adaDiskon())
							&& !kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
									.isEmpty()
							&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
									.contains(detailBiaya.getItemBiaya().getId())

							&&

							(

							kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() == null
									|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() != null
											&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterMulai() <= kegiatan.getSemster())

							)

							&&

							(

							kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() == null
									|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() != null
											&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
													.getSemesterSampai() >= kegiatan.getSemster())

							)

					) {

						desc += ", " + kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getNama();

					}

					else {

						Kegiatan.hitungDiskon(detailKegiatan, kegiatan, detailBiaya, nilai_sebelum_diskon);
						if (detailKegiatan != null && detailKegiatan.adaDiskon()
								&& detailKegiatan.getDiskonMahasiswaData() != null
								&& detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa() != null) {
							desc += ", " + detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa().getNama();
						}
					}
				}
				Double diskon = detailKegiatan == null ? 0.0 : detailKegiatan.getDiskon();

				JenisDiskonMahasiswa jenisDiskonMahasiswa = detailKegiatan == null ? null
						: detailKegiatan.cariJenisDiskonMahasiswa();

				map.put("namaTagihan", namaTagihan);

				map.put("nilai_sebelum_diskon", nilai_sebelum_diskon);
				map.put("jumlah_sebelum_diskon", jumlah_sebelum_diskon);
				map.put("nilai_diskon", diskon);

				jumlah_diskon += diskon;

				map.put("nilai_diskon_berupa_persen",
						jenisDiskonMahasiswa == null ? false : jenisDiskonMahasiswa.getBerupaPersen());
				map.put("nilai_diskon_persen",
						jenisDiskonMahasiswa == null || !jenisDiskonMahasiswa.getBerupaPersen() ? 0
								: jenisDiskonMahasiswa.getDiskon());
				map.put("nilai_diskon_nama", jenisDiskonMahasiswa == null ? "" : jenisDiskonMahasiswa.getNama());
				map.put("nama_diskon", jenisDiskonMahasiswa == null ? "" : jenisDiskonMahasiswa.getNama());

				map.put("berupa_persen_diskon",
						jenisDiskonMahasiswa == null ? false : jenisDiskonMahasiswa.getBerupaPersen());
				map.put("angka_diskon", jenisDiskonMahasiswa == null || !jenisDiskonMahasiswa.getBerupaPersen() ? 0
						: jenisDiskonMahasiswa.getDiskon());

//				jumlah = jumlah - diskon;

				map.put("desc", desc);
				map.put("jumlah", jumlah);

				map.put("no_registrasi", calonMahasiswa.getNoRegistrasi());
				map.put("no_ujian", calonMahasiswa.getNoUjian());
				map.put("nama", calonMahasiswa.getNama());
				map.put("kartu", calonMahasiswa.getJenisKartuIdentitas() == null ? ""
						: calonMahasiswa.getJenisKartuIdentitas().getNama());
				map.put("no_identitas", calonMahasiswa.getNoIdentitas());
				map.put("jenis_kelamin", calonMahasiswa.getJenisKelamin());
				map.put("tempat_lahir", calonMahasiswa.getTempatLahir());
				{
					java.util.Date _tl = calonMahasiswa.getTanggalLahir();
					map.put("tanggal_lahir", _tl == null ? null : new java.sql.Date(_tl.getTime()));
				}
				map.put("alamat", calonMahasiswa.getAlamat());
				map.put("agama", calonMahasiswa.getAgama() == null ? "" : calonMahasiswa.getAgama().getNama());
				map.put("asal_sma", calonMahasiswa.getAsalSma());
				map.put("p1", calonMahasiswa.getProdi1() == null ? null : calonMahasiswa.getProdi1().getNama());
				map.put("p2", calonMahasiswa.getProdi2() == null ? null : calonMahasiswa.getProdi2().getNama());
				map.put("p3", calonMahasiswa.getProdi3() == null ? null : calonMahasiswa.getProdi3().getNama());
				map.put("p4", calonMahasiswa.getProdi4() == null ? null : calonMahasiswa.getProdi4().getNama());
				map.put("p5", calonMahasiswa.getProdi5() == null ? null : calonMahasiswa.getProdi5().getNama());
				map.put("lulus",
						calonMahasiswa.getProdiLulus() == null ? null : calonMahasiswa.getProdiLulus().getNama());
				map.put("lulusfakultas", calonMahasiswa.getProdiLulus() == null ? null
						: calonMahasiswa.getProdiLulus().getFakultas().getNama());

				map.put("tahunakademik", calonMahasiswa.getTahunAkademik());

				map.put("asal", calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma());
				map.put("jenjang", calonMahasiswa.getJenjang() == null ? "" : calonMahasiswa.getJenjang().getNama());
				map.put("gelombang", calonMahasiswa.getGelombangPendaftaran() == null ? ""
						: calonMahasiswa.getGelombangPendaftaran().getNama());

				map.putAll(ujianData);

				String prodi_pilihan = calonMahasiswa.getProdi1() == null ? "" : calonMahasiswa.getProdi1().getNama();
				if (calonMahasiswa.getProdi2() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi2().getNama();
				}
				if (calonMahasiswa.getProdi3() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi3().getNama();
				}
				if (calonMahasiswa.getProdi4() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi4().getNama();
				}
				if (calonMahasiswa.getProdi5() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi5().getNama();
				}

				map.put("prodi_pilihan", prodi_pilihan);

				biaya += jumlah;

				if (jumlah.intValue() != 0) {
					maps.add(map);
				}
			}

			if (maps.isEmpty()) {

				Collection<DetailKegiatan> detailKegiatans = pengaturanPembayaranBulanan == null || kegiatan == null
						|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(true);

				DetailKegiatan detailKegiatan = kegiatan == null || kegiatan.getId() == null ? null
						: (pengaturanPembayaranBulanan != null
								? kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan, detailKegiatans)
								: kegiatan.ambilSatuDetailKegiatan(detailBiaya, true));
				Kegiatan.hitungDiskon(detailKegiatan, kegiatan, detailBiaya, jumlah_sebelum_diskon);
				if (detailKegiatan != null && detailKegiatan.adaDiskon()
						&& detailKegiatan.getDiskonMahasiswaData() != null
						&& detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa() != null) {
					desc += ", " + detailKegiatan.getDiskonMahasiswaData().getJenisDiskonMahasiswa().getNama();
				}

				Double diskon = detailKegiatan == null ? 0.0 : detailKegiatan.getDiskon();
				JenisDiskonMahasiswa jenisDiskonMahasiswa = detailKegiatan == null ? null
						: detailKegiatan.cariJenisDiskonMahasiswa();

				Double nilai_sebelum_diskon = 0.0;

				Map map = new HashMap();
				map.put("desc", "Belum ada informasi pembayaran");
				map.put("jumlah", 0.0);
				map.put("no_registrasi", calonMahasiswa.getNoRegistrasi());
				map.put("no_ujian", calonMahasiswa.getNoUjian());
				map.put("nama", calonMahasiswa.getNama());
				map.put("kartu", calonMahasiswa.getJenisKartuIdentitas() == null ? ""
						: calonMahasiswa.getJenisKartuIdentitas().getNama());
				map.put("no_identitas", calonMahasiswa.getNoIdentitas());
				map.put("jenis_kelamin", calonMahasiswa.getJenisKelamin());
				map.put("tempat_lahir", calonMahasiswa.getTempatLahir());
				{
					java.util.Date _tl = calonMahasiswa.getTanggalLahir();
					map.put("tanggal_lahir", _tl == null ? null : new java.sql.Date(_tl.getTime()));
				}
				map.put("alamat", calonMahasiswa.getAlamat());
				map.put("agama", calonMahasiswa.getAgama() == null ? "" : calonMahasiswa.getAgama().getNama());
				map.put("asal_sma", calonMahasiswa.getAsalSma());
				map.put("p1", calonMahasiswa.getProdi1() == null ? null : calonMahasiswa.getProdi1().getNama());
				map.put("p2", calonMahasiswa.getProdi2() == null ? null : calonMahasiswa.getProdi2().getNama());
				map.put("p3", calonMahasiswa.getProdi3() == null ? null : calonMahasiswa.getProdi3().getNama());
				map.put("p4", calonMahasiswa.getProdi4() == null ? null : calonMahasiswa.getProdi4().getNama());
				map.put("p5", calonMahasiswa.getProdi5() == null ? null : calonMahasiswa.getProdi5().getNama());
				map.put("lulus",
						calonMahasiswa.getProdiLulus() == null ? null : calonMahasiswa.getProdiLulus().getNama());
				map.put("lulusfakultas", calonMahasiswa.getProdiLulus() == null ? null
						: calonMahasiswa.getProdiLulus().getFakultas().getNama());

				map.put("tahunakademik", calonMahasiswa.getTahunAkademik());

				map.put("jumlah_sebelum_diskon", jumlah_sebelum_diskon);
				map.put("nilai_diskon", diskon);
				map.put("nilai_sebelum_diskon", nilai_sebelum_diskon);
				map.put("nilai_diskon", diskon);

				map.put("nilai_diskon_berupa_persen",
						jenisDiskonMahasiswa == null ? false : jenisDiskonMahasiswa.getBerupaPersen());
				map.put("nilai_diskon_persen",
						jenisDiskonMahasiswa == null || !jenisDiskonMahasiswa.getBerupaPersen() ? 0
								: jenisDiskonMahasiswa.getDiskon());
				map.put("nilai_diskon_nama", jenisDiskonMahasiswa == null ? "" : jenisDiskonMahasiswa.getNama());
				map.put("nama_diskon", jenisDiskonMahasiswa == null ? "" : jenisDiskonMahasiswa.getNama());

				map.put("berupa_persen_diskon",
						jenisDiskonMahasiswa == null ? false : jenisDiskonMahasiswa.getBerupaPersen());
				map.put("angka_diskon", jenisDiskonMahasiswa == null || !jenisDiskonMahasiswa.getBerupaPersen() ? 0
						: jenisDiskonMahasiswa.getDiskon());

				map.put("asal", calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma());
				map.put("jenjang", calonMahasiswa.getJenjang() == null ? "" : calonMahasiswa.getJenjang().getNama());
				map.put("gelombang", calonMahasiswa.getGelombangPendaftaran() == null ? ""
						: calonMahasiswa.getGelombangPendaftaran().getNama());

				map.putAll(ujianData);

				String prodi_pilihan = calonMahasiswa.getProdi1() == null ? "" : calonMahasiswa.getProdi1().getNama();
				if (calonMahasiswa.getProdi2() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi2().getNama();
				}
				if (calonMahasiswa.getProdi3() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi3().getNama();
				}
				if (calonMahasiswa.getProdi4() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi4().getNama();
				}
				if (calonMahasiswa.getProdi5() != null) {
					prodi_pilihan += ", " + calonMahasiswa.getProdi5().getNama();
				}

				map.put("prodi_pilihan", prodi_pilihan);

				maps.add(map);
			}

			parameters.put("jumlah_diskon", jumlah_diskon);
			parameters.put("jumlah_sebelum_diskon", jumlah_sebelum_diskon);

			Double jumlah_setelah_diskon = jumlah_sebelum_diskon - jumlah_diskon;
			parameters.put("jumlah_setelah_diskon", jumlah_setelah_diskon);

			parameters.put("jumlah_setelah_diskon_format", Common.numberFormat.get().format(jumlah_setelah_diskon));
			parameters.put("jumlah_setelah_diskon_text",
					IndonesianNumberToWords.convert(jumlah_setelah_diskon.longValue()));

			parameters.put("maps", maps);
			parameters.put("biaya_masuk_kuliah", biaya);
			parameters.put("biaya_masuk_kuliah_format", Common.numberFormat.get().format(biaya));
			parameters.put("biaya_masuk_kuliah_text", IndonesianNumberToWords.convert(biaya.longValue()));

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:1546");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (session != null && session.isOpen()) {
				try { session.clear(); } catch (Exception e) { }
				try { session.disconnect(); } catch (Exception e) { }
				try { session.close(); } catch (Exception e) { }
			}
		}

		Common.insertProperty(BiodataCalonMahasiswa.class, calonMahasiswa, parameters, "bio", 2);

		if (calonMahasiswa.getTanggalDiterima() != null) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonMahasiswa.getTanggalDiterima());
			calendar.add(Calendar.DATE, 7); // 2 weeks
			ManajemenProperty.formatDate(parameters, "bio", "1.minggu.diterima", calendar.getTime());

			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonMahasiswa.getTanggalDiterima());
			calendar.add(Calendar.DATE, 14); // 2 weeks
			ManajemenProperty.formatDate(parameters, "bio", "2.minggu.diterima", calendar.getTime());

			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonMahasiswa.getTanggalDiterima());
			calendar.add(Calendar.DATE, 21); // 2 weeks
			ManajemenProperty.formatDate(parameters, "bio", "3.minggu.diterima", calendar.getTime());

			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonMahasiswa.getTanggalDiterima());
			calendar.add(Calendar.DATE, 28); // 2 weeks
			ManajemenProperty.formatDate(parameters, "bio", "4.minggu.diterima", calendar.getTime());
		}

		parameters.put("hp", calonMahasiswa.getHp() != null ? calonMahasiswa.getHp() : "");
		parameters.put("email", calonMahasiswa.getEmail() != null ? calonMahasiswa.getEmail() : "");

		return parameters;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void onCetakSuratKeteranganLulus(BiodataCalonMahasiswa calonMahasiswa, final boolean kirim)
			throws Exception {
		if (calonMahasiswa.getProdiLulus() == null) {
			MyMessageboxConfig.show("Mohon maaf, calon mahasiswa ini belum dinyatakan diterima. Langkah yang dapat dilakukan: (1) Pastikan proses seleksi/penerimaan sudah diselesaikan oleh Admin Penerimaan; (2) Periksa status calon mahasiswa di menu Penerimaan Mahasiswa; (3) Ulangi proses cetak setelah status diperbarui. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		try {
			CetakRegistrasiAction.singkronkanDenganPembayaran(calonMahasiswa.getId(), new Label(), 0, 1);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:1592");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		// FIX RuntimeException "Koneksi database terputus" (KE-10) menembus mentah ke user: genSklMap()
		// sengaja melempar RuntimeException saat koneksi DB terputus di tengah proses, tapi pemanggilan
		// ini sebelumnya TIDAK dibungkus try/catch sama sekali (beda dgn panggilan di atas & di bawahnya
		// yg sudah aman) -- exception-nya lolos sampai ke ZK tanpa pesan ramah. Tangkap & tampilkan
		// pesan yang jelas ke user, konsisten dgn pola penanganan error lain di method ini.
		Map parameters = null;
		try {
			parameters = genSklMap(calonMahasiswa);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:onCetakSuratKeteranganLulus-genSklMap");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat menyiapkan data cetak Surat Keterangan Lulus, kemungkinan disebabkan oleh gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Ulangi kembali proses cetak beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
			return;
		}
		try {
			parameters.put("file_laporan", URLEncoder.encode(
					calonMahasiswa.getNoRegistrasi() + " " + calonMahasiswa.getNama() + " Keterangan_Lulus", "UTF-8"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:1598");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		if (Common.bolehKonfigurasi("cetak_ktm_di_surat_keterangan_lulus") && calonMahasiswa.getMahasiswa() != null) {

			if (!kirim) {
				if (Common.bolehKonfigurasi("cetak_ktm_di_surat_keterangan_lulus_harus_mendapatkan_nim", Konfigurasi.TIDAK_AKTIF) && calonMahasiswa.getMahasiswa() == null) {
					MyMessageboxConfig.show("Mohon maaf, calon mahasiswa ini belum mendapatkan NIM. Langkah yang dapat dilakukan: (1) Pastikan proses pemberian NIM sudah dilakukan oleh Admin Akademik; (2) Periksa status mahasiswa di menu Data Mahasiswa; (3) Ulangi proses cetak setelah NIM diberikan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
			}

			File file = Report.generateDownloadReport(Report.PDF, parameters, "Keterangan_Lulus", null,
					ais.ui.util.WaktuUtil.getDate(), Common.locale, false);

			PDFMergerUtility ut = new PDFMergerUtility();
			ut.addSource(file);

			int masaKartuMahasiswa = LaporanKartuMahasiswa.ambilMasaBerlakuKartuMahasiswa();

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			try {
				calendar.setTime(calonMahasiswa.getTanggalDiterima());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:1628");
				// TODO: handle exception
			}
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + masaKartuMahasiswa);

			Date masa_berlaku_kartu = calendar.getTime();
			System.out.println("masa_berlaku_kartu => " + Common.dateFormat1.get().format(masa_berlaku_kartu));

			List list = new ArrayList();
			list.add(LaporanKartuMahasiswa.siapkanParemeter(calonMahasiswa.getMahasiswa()));

			Map parametersKartu = ais.common.HashMapGenerator.getRand();

			parametersKartu = LaporanKartuMahasiswa.siapkanParemeterGambar(parametersKartu, null);
			parametersKartu.put("tanggal_kartu", calonMahasiswa.getTanggalDiterima());
			parametersKartu.put("masa_berlaku_kartu", masa_berlaku_kartu);

			parametersKartu.put("belakang", true);
			parametersKartu.put("depan", true);
			parametersKartu.put("maps", list);

			File fileinfo = Report.generateDownloadReport(Report.PDF, parametersKartu, "format1/kartu_mahasiswa", null,
					ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
			ut.addSource(fileinfo);

			File filePdfBaru = new File(
					file.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
			ut.setDestinationStream(new FileOutputStream(filePdfBaru));
			ut.mergeDocuments();

			parameters.putAll(parametersKartu);
			Report.tampil(filePdfBaru, parameters);

			if (kirim) {
				CommonEmail.infoDaftarMahasiswaDinyatakanDIterima(calonMahasiswa, filePdfBaru);
			}
		} else {

			File file = Report.generateDownloadReport(Report.PDF, parameters, "Keterangan_Lulus", null,
					ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
			Report.tampil(file, parameters, "Keterangan_Lulus");
			if (kirim) {
				CommonEmail.infoDaftarMahasiswaDinyatakanDIterima(calonMahasiswa, file);
			}

		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void prosesSuratKeteranganHasilUjian(final BiodataCalonMahasiswa calonMahasiswa) throws Exception {

		final West west = new West();
		west.setWidth("300px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(
				calonMahasiswa.getTanggalSuratKelulusan() == null ? ais.ui.util.WaktuUtil.getDate()
						: calonMahasiswa.getTanggalSuratKelulusan()));
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		final MyTextbox nomor;
		row.appendChild(nomor = new MyTextbox(calonMahasiswa.getNomorSuratKelulusan()));
		nomor.setWidth("90%");

		final Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("biodata_id", calonMahasiswa.getId());

		calonMahasiswa.putPhoto(parameters);

		try {
			ArrayList detailBiayas = new ArrayList();
			PembayaranUtil.getInstance();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
					calonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, calonMahasiswa.getProdiLulus(), 1,
					false);
			detailBiayas.addAll(detailBiayas1);

			Session session = HibernateUtil.currentSession();
			PembayaranUtil.getInstance();
			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, calonMahasiswa,
					ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1, detailBiayas, false, false);

			Collection biayaBulanan = null;
			if (countPengaturanBulanan > 0) {
				biayaBulanan = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(calonMahasiswa, session, 1,
						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, false, false);
			}
			Collection dataBTagihan = biayaBulanan != null ? biayaBulanan : detailBiayas;
			Double biaya = 0.0;
			for (Object o : dataBTagihan) {
				Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(1, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
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
			parameters.put("biaya_masuk_kuliah", biaya);
			parameters.put("biaya_masuk_kuliah_format", Common.numberFormat.get().format(biaya));
			parameters.put("biaya_masuk_kuliah_text", IndonesianNumberToWords.convert(biaya.longValue()));
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:1758");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		Common.insertProperty(BiodataCalonMahasiswa.class, calonMahasiswa, parameters, "bio", 2);

		parameters.put("file_laporan",
				URLEncoder.encode(
						calonMahasiswa.getNoRegistrasi() + " " + calonMahasiswa.getNama() + " Keterangan_Hasil_Ujian",
						"UTF-8"));

		final EventListener eventListenerReport = Report.generatePDFReport("pdf", parameters, "Keterangan_Hasil_Ujian",
				ais.ui.util.WaktuUtil.getDate(), west);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				calonMahasiswa.setNomorSuratKelulusan(nomor.getValue());
				calonMahasiswa.setTanggalSuratKelulusan(tanggal.getValue());
				Common.refreshUpdate(calonMahasiswa);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						eventListenerReport.onEvent(new Event("", null, parameters));

					}
				});
			}
		};

		tanggal.addEventListener("onChange", eventListener);
		nomor.addEventListener("onChange", eventListener);

	}

	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	public static void prosesSuratTagihan(final BiodataCalonMahasiswa calonMahasiswa, final JenisKegiatan jenisKegiatan,
			final Kegiatan kegiatan, final Integer semester, final JadwalPembayaran jadwalPembayaran) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		final West west = new West();
		west.setWidth(
				tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null ? "300px" : "0px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		new MyColumnConfig().setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		final ParameterUmum parameterUmumTanggalJatuhTempo = Common.getParameterUmum(
				"tanggal jatuh tempo surat tagihan " + calonMahasiswa.getNoRegistrasi() + " "
						+ calonMahasiswa.getTahunAkademik() + " " + calonMahasiswa.getSemesterMulai(),
				Common.dateFormat1.get().format(calendar.getTime()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Jatuh Tempo"));
		final MyDatebox tanggalJatuhTempo;
		row.appendChild(tanggalJatuhTempo = new MyDatebox(
				Common.dateFormat1.get().parse(parameterUmumTanggalJatuhTempo.getNilai())));
		tanggalJatuhTempo.setReadonly(true);
		tanggalJatuhTempo.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumTanggalJatuhTempo.setNilai(Common.dateFormat1.get().format(tanggalJatuhTempo.getValue()));
				Common.refreshUpdate(parameterUmumTanggalJatuhTempo);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		final MyTextbox nomor;
		final ParameterUmum parameterUmum = Common.getParameterUmum("surat tagihan " + calonMahasiswa.getNoRegistrasi()
				+ " " + calonMahasiswa.getTahunAkademik() + " " + calonMahasiswa.getSemesterMulai(), "");
		row.appendChild(nomor = new MyTextbox(parameterUmum.getNilai()));
		nomor.setWidth("90%");
		nomor.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmum.setNilai(nomor.getValue());
				Common.refreshUpdate(parameterUmum);
			}
		});

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_cara_pembayaran_di_surat_tagihan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));

		SatuanKerja satuanKerja = Common.getSatuanKerja();
		final Combobox jenisPembayaran = new Combobox();
		row.appendChild(jenisPembayaran);

		Common.insertComboDanSemua(jenisPembayaran, new String[] { "nama", "bank" }, "akun", JenisPembayaran.class,
				"== Tidak menggunakan cara pembayaran ==",
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		final ParameterUmum parameterUmumRekeningPembayaran = Common
				.getParameterUmum("bank surat tagihan " + calonMahasiswa.getNoRegistrasi() + " "
						+ calonMahasiswa.getTahunAkademik() + " " + calonMahasiswa.getSemesterMulai(), "");

		// ISOLASI PENGAMBILAN DATA REKENING
		Session sessionLocal = null;
		JenisPembayaran selectedJenisPembayaran = null;
		try {
			sessionLocal = ais.action.report.Report.openNativeSession();
			selectedJenisPembayaran = (JenisPembayaran) sessionLocal.createCriteria(JenisPembayaran.class)
					.add(parameterUmumRekeningPembayaran.getNilai().isEmpty()
							|| !Common.isNumber(parameterUmumRekeningPembayaran.getNilai())
									? Restrictions.sqlRestriction("false")
									: Restrictions.idEq(Long.parseLong(parameterUmumRekeningPembayaran.getNilai())))
					.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:1899");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (sessionLocal != null && sessionLocal.isOpen()) {
				try {
					sessionLocal.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:1904");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		Common.selectComboItem(jenisPembayaran, selectedJenisPembayaran);
		jenisPembayaran.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) (jenisPembayaran.getSelectedItem() == null
						? null
						: jenisPembayaran.getSelectedItem().getValue());
				parameterUmumRekeningPembayaran
						.setNilai(pilihanJenisPembayaran == null ? "" : pilihanJenisPembayaran.getId().toString());
				Common.refreshUpdate(parameterUmumRekeningPembayaran);
			}
		});
		jenisPembayaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Denda"));
		final MyDoublebox denda;
		final ParameterUmum parameterUmumDenda = Common
				.getParameterUmum("denda surat tagihan " + calonMahasiswa.getNoRegistrasi() + " "
						+ calonMahasiswa.getTahunAkademik() + " " + calonMahasiswa.getSemesterMulai(), "0.0");
		row.appendChild(denda = new MyDoublebox(Double.parseDouble(parameterUmumDenda.getNilai())));
		denda.setWidth("90%");
		denda.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumDenda.setNilai((denda.getValue() == null ? 0.0 : denda.getValue()) + "");
				Common.refreshUpdate(parameterUmumDenda);
			}
		});

		final MyFormRow rowItemBiaya1 = new MyFormRow();
		rowItemBiaya1.setStyle("border:0px;background: transparent;");
		rowItemBiaya1.setParent(rows);
		rowItemBiaya1.appendChild(new ais.ui.util.MyLabelConfig("Pilih Tagihan"));
		rowItemBiaya1.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya1, "2");

		final MyFormRow rowItemBiaya2 = new MyFormRow();
		rowItemBiaya2.setStyle("border:0px;background: transparent;");
		rowItemBiaya2.setParent(rows);
		rowItemBiaya2.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya2, "2");

		final Vbox vboxItemBiaya = new Vbox();
		vboxItemBiaya.setParent(rowItemBiaya2);

		final Map parameters = ais.common.HashMapGenerator.getRandStringObject();
		Common.insertProperty(BiodataCalonMahasiswa.class, calonMahasiswa, parameters, "bio", 2);
		parameters.put("biodata_id", calonMahasiswa.getId());
		parameters.put("tanggal", tanggal.getValue());
		parameters.put("nomor", nomor.getValue());
		parameters.put("nama_ayah", calonMahasiswa.getNamaAyah());
		parameters.put("jenis_tagihan", jenisKegiatan.getNamaKegiatan());
		parameters.put("tahunakademik", calonMahasiswa.getTahunAkademik());
		parameters.put("dedaline", jadwalPembayaran.getEndDate());
		parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());
		parameters.put("semester", semester);
		parameters.put("tahun_akademik", calonMahasiswa.getTahunAkademik());

		parameters.put("file_laporan", URLEncoder
				.encode(calonMahasiswa.getNoRegistrasi() + " " + calonMahasiswa.getNama() + " Tagihan", "UTF-8"));

		if (jenisPembayaran.getSelectedItem() != null && jenisPembayaran.getSelectedItem().getValue() != null) {
			JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem().getValue();
			if (pilihanJenisPembayaran.getBank() != null) {
				parameters.put("rekening_pembayaran",
						" melalui Bank " + pilihanJenisPembayaran.getBank().getNama() + "</b>");
			} else {
				parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
			}
		}

		parameters.put("prosentaseDenda", denda.getValue());
		parameters.put("nama", calonMahasiswa.getNama());
		parameters.put("nim", calonMahasiswa.getMahasiswa() == null ? calonMahasiswa.getNoRegistrasi()
				: calonMahasiswa.getMahasiswa().getNim());

		if (calonMahasiswa.getProdiLulus() != null) {
			parameters.put("kaprodi", calonMahasiswa.getProdiLulus().getKaprodi() == null ? "(......................)"
					: calonMahasiswa.getProdiLulus().getKaprodi().getNama());
			parameters.put("jurusan", calonMahasiswa.getProdiLulus().getNama());
			parameters.put("fakultas", calonMahasiswa.getProdiLulus().getFakultas().getNama());
		} else if (calonMahasiswa.getProdi1() != null) {
			parameters.put("kaprodi", calonMahasiswa.getProdi1().getKaprodi() == null ? "(......................)"
					: calonMahasiswa.getProdi1().getKaprodi().getNama());
			parameters.put("jurusan", calonMahasiswa.getProdi1().getNama());
			parameters.put("fakultas", calonMahasiswa.getProdi1().getFakultas().getNama());
		}

		final List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		populateTagihanCalonMahasiswa(calonMahasiswa, jenisKegiatan, kegiatan, semester, maps);

		if (calonMahasiswa.getMahasiswa() != null) {
			populateTagihanMahasiswa(calonMahasiswa.getMahasiswa(), 1, maps);
		}

		Long jenis_tagihan_id = null;
		final List<MyCheckboxConfig> checkboxConfigs = new ArrayList<MyCheckboxConfig>();
		for (Map<String, Object> map : maps) {
			Long idTag = (Long) map.get("jenis_tagihan_id");
			if (jenis_tagihan_id == null || !jenis_tagihan_id.equals(idTag)) {
				new MyLabelBolder(map.get("jenis_tagihan").toString()).setParent(vboxItemBiaya);
				jenis_tagihan_id = idTag;
			}

			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) map
					.get("pengaturanPembayaranBulanan");
			DetailBiaya detailBiaya = (DetailBiaya) map.get("detailBiaya");
			Double nilai = (Double) map.get("nilai");

			MyCheckboxConfig checkBox = new MyCheckboxConfig(
					map.get("label").toString() + ", " + Common.numberFormat.get().format(nilai));
			checkBox.setChecked(nilai > 0.1);
			checkBox.setAttribute("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);
			checkBox.setAttribute("detailBiaya", detailBiaya);
			checkboxConfigs.add(checkBox);
			checkBox.setParent(vboxItemBiaya);
		}

		parameters.put("maps", maps);
		final EventListener eventListenerReport = Report.generatePDFReport("pdf", parameters, "Surat_Tagihan",
				ais.ui.util.WaktuUtil.getDate(), west);

		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameters.remove("maps");

				// OPTIMASI: Menggunakan StringBuilder untuk mengumpulkan tagihanData
				StringBuilder tagihanDataBldr = new StringBuilder();
				List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>();

				for (MyCheckboxConfig checkboxConfig : checkboxConfigs) {
					if (checkboxConfig.isChecked()) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulananAwal = (PengaturanPembayaranBulanan) checkboxConfig
								.getAttribute("pengaturanPembayaranBulanan");
						DetailBiaya detailBiayaAwal = (DetailBiaya) checkboxConfig.getAttribute("detailBiaya");

						for (Map<String, Object> map : maps) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) map
									.get("pengaturanPembayaranBulanan");
							DetailBiaya detailBiaya = (DetailBiaya) map.get("detailBiaya");

							if (pengaturanPembayaranBulananAwal != null && pengaturanPembayaranBulanan != null
									&& pengaturanPembayaranBulanan.getId()
											.equals(pengaturanPembayaranBulananAwal.getId())) {
								newMaps.add(map);
								try {
									Double nilai = (Double) map.get("nilai");
									String s = map.get("label").toString() + ", "
											+ Common.numberFormat.get().format(nilai);
									if (tagihanDataBldr.length() > 0)
										tagihanDataBldr.append("; ");
									tagihanDataBldr.append(s);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:2065");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								break;
							} else if (pengaturanPembayaranBulananAwal == null && detailBiayaAwal != null
									&& detailBiaya != null && detailBiaya.getId().equals(detailBiayaAwal.getId())) {
								newMaps.add(map);
								try {
									Double nilai = (Double) map.get("nilai");
									String s = map.get("label").toString() + ", "
											+ Common.numberFormat.get().format(nilai);
									if (tagihanDataBldr.length() > 0)
										tagihanDataBldr.append("; ");
									tagihanDataBldr.append(s);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:2078");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								break;
							}
						}
					}
				}
				parameters.put("tagihanData", tagihanDataBldr.toString());
				parameters.put("maps", newMaps);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						parameters.put("tanggal", tanggal.getValue());
						parameters.put("nomor", nomor.getValue());
						parameters.put("jenis_tagihan", jenisKegiatan.getNamaKegiatan().toUpperCase());
						parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());

						if (jenisPembayaran.getSelectedItem() != null
								&& jenisPembayaran.getSelectedItem().getValue() != null) {
							JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem()
									.getValue();
							if (pilihanJenisPembayaran.getBank() != null) {
								parameters.put("rekening_pembayaran",
										" melalui Bank " + pilihanJenisPembayaran.getBank().getNama() + "</b>");
							} else {
								parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
							}
						}
						parameters.put("prosentaseDenda", denda.getValue());
						eventListenerReport.onEvent(new Event("", null, parameters));
					}
				});
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		Button tampilkan;
		hbox.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan Tagihan", "/img/print.png"));
		tampilkan.addEventListener("onClick", eventListener);

		Button kirim;
		hbox.appendChild(kirim = new MyToolbarbuttonConfig("Kirim Tagihan", "/img/print.png"));
		kirim.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						String subject = "Tagihan untuk calon mahasiswa atas nama " + calonMahasiswa.getNama() + " ("
								+ calonMahasiswa.getNoRegistrasi() + ")";
						String tagihanData = parameters.get("tagihanData") + "";
						String body = "Terdapat tagihan untuk calon mahasiswa atas nama " + calonMahasiswa.getNama()
								+ " (" + calonMahasiswa.getNoRegistrasi()
								+ ") <br>Isi informasi tagihan adalah sbb:<br>" + tagihanData + ".<br><br>Terima Kasih";
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						JSONArray userIds = new JSONArray();
						userIds.put(calonMahasiswa.getNoRegistrasi());

						File file = new File(parameters.get("report_file") + "");
						MailSender.sendMailLampiranTagihan(userIds, subject, body, sender, calonMahasiswa.getEmail(),
								null, true, calonMahasiswa, file);
					}
				});
			}
		});

		Common.createDefaultTimer(eventListener);

		if (jadwalPembayaran != null && jadwalPembayaran.getEndDate() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			final MyCheckboxConfig menggunakanDeadline;
			row.appendChild(
					menggunakanDeadline = new MyCheckboxConfig("Jatuh tempo menggunakan masa akhir jadwal pembayaran"));

			if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
				menggunakanDeadline.setChecked(true);
				menggunakanDeadline.setDisabled(true);
				tanggalJatuhTempo.setValue(jadwalPembayaran.getEndDate());
				tanggalJatuhTempo.setDisabled(true);
			} else {
				menggunakanDeadline.setChecked(true);
				tanggalJatuhTempo.setValue(jadwalPembayaran.getEndDate());
			}

			menggunakanDeadline.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tanggalJatuhTempo.setDisabled(menggunakanDeadline.isChecked());
					if (menggunakanDeadline.isChecked()) {
						tanggalJatuhTempo.setValue(jadwalPembayaran.getEndDate());
						parameterUmumTanggalJatuhTempo
								.setNilai(Common.dateFormat1.get().format(tanggalJatuhTempo.getValue()));
						Common.refreshUpdate(parameterUmumTanggalJatuhTempo);
						eventListener.onEvent(arg0);
					}
				}
			});
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Double populateTagihanCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa,
			JenisKegiatan jenisKegiatan, Kegiatan kegiatan, Integer semester, List<Map<String, Object>> maps) {

		Double sisaTotal = 0.0;
		if (jenisKegiatan != null && calonMahasiswa != null) {
			Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
					: kegiatan.ambilDetailKegiatan(false);
			List<CicilanPembayaran> cicilanPembayarans = calonMahasiswa.ambilCicilan();

			HashMap<Long, ItemBiaya> itemBiayas = new HashMap<Long, ItemBiaya>();
			Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
			ArrayList detailBiayas = new ArrayList();

			if (prodiLulus == null || prodiLulus.getId() == null) {
				Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
						: calonMahasiswa.getProdi1();
				PembayaranUtil.getInstance();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
						.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, myjurusan1, semester, false);
				detailBiayas.addAll(detailBiayas1);
			} else {
				PembayaranUtil.getInstance();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
						.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, prodiLulus, semester, false);
				detailBiayas.addAll(detailBiayas1);
			}

			for (Object o : detailBiayas) {
				DetailBiaya detailBiaya = (DetailBiaya) o;
				if (detailBiaya != null && detailBiaya.getItemBiaya() != null) {
					itemBiayas.put(detailBiaya.getItemBiaya().getId(), detailBiaya.getItemBiaya());
				}
			}

			Collection biayaBulanan = detailBiayas;

			// MENGGUNAKAN SESSION TERPUSAT DENGAN PROTEKSI FINALLY
			Session sessionLocal = null;
			try {
				sessionLocal = ais.action.report.Report.openNativeSession();

				PembayaranUtil.getInstance();
				int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(sessionLocal, calonMahasiswa,
						jenisKegiatan, semester, detailBiayas, false, false);
				if (countPengaturanBulanan > 0) {
					biayaBulanan = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(calonMahasiswa,
							sessionLocal, semester, jenisKegiatan, detailBiayas, false, false);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:2237");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			} finally {
				ais.action.report.Report.closeNativeSession(sessionLocal);
			}

			for (Object arg1 : biayaBulanan) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				if (arg1 instanceof DetailBiaya) {
					DetailBiaya detailBiaya = (DetailBiaya) arg1;

					Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
					Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya, cicilanPembayarans);

					if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
							&& telahDibayar.intValue() > 0) {
						jumlah = telahDibayar;
					}

					Double sisa = jumlah - telahDibayar;
					sisaTotal += sisa;

					map.put("detailBiaya", detailBiaya);
					map.put("label", detailBiaya.getItemBiaya().getNama());
					map.put("nilai", sisa);
					map.put("telahDibayar", telahDibayar);
					map.put("tagihan", jumlah);
					map.put("jenis_tagihan", jenisKegiatan.getNamaKegiatan());
					map.put("jenis_tagihan_id", jenisKegiatan.getId());
					map.put("ta", kegiatan == null ? "" : kegiatan.getTahunAkademik());
					map.put("smt", kegiatan == null ? 0 : kegiatan.getSemster());

					if (jumlah != null && jumlah.intValue() != 0 && detailBiaya.getItemBiaya() != null
							&& detailBiaya.getItemBiaya().getDitampilkanDiSuratTagihan()) {
						maps.add(map);
					}

				} else if (arg1 instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) arg1;
					DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

					Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, null, semester,
							pengaturanPembayaranBulanan);
					Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
							cicilanPembayarans);

					if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
							&& telahDibayar.intValue() > 0) {
						jumlah = telahDibayar;
					}

					JadwalPembayaran jadwalPembayaran = kegiatan == null ? null : kegiatan.getJadwalPembayaran();
					JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
							&& jadwalPembayaran.getKhususUntukNim()
									.contains("," + calonMahasiswa.getNoRegistrasi() + ",") ? jadwalPembayaran : null;

					Double hasilDenda = pengaturanPembayaranBulanan.checkDenda(jumlah, ais.ui.util.WaktuUtil.getDate(),
							jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan());
					String desc = pengaturanPembayaranBulanan.getKeterangan();

					desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
							: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " "
							+ (hasilDenda.intValue() > jumlah.intValue() ? pengaturanPembayaranBulanan.getInfoDenda()
									: "");

					Double sisa = jumlah - telahDibayar;
					sisaTotal += sisa;

					map.put("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);
					map.put("detailBiaya", detailBiaya);
					map.put("label", desc);
					map.put("nilai", sisa);
					map.put("telahDibayar", telahDibayar);
					map.put("tagihan", jumlah);
					map.put("jenis_tagihan", jenisKegiatan.getNamaKegiatan());
					map.put("jenis_tagihan_id", jenisKegiatan.getId());
					map.put("ta", kegiatan == null ? "" : kegiatan.getTahunAkademik());
					map.put("smt", kegiatan == null ? 0 : kegiatan.getSemster());

					if (jumlah != null && jumlah.intValue() != 0 && detailBiaya.getItemBiaya() != null
							&& detailBiaya.getItemBiaya().getDitampilkanDiSuratTagihan()) {
						maps.add(map);
					}
				}
			}
		}
		return sisaTotal;
	}

	@SuppressWarnings("rawtypes")
	public static Double populateMapTagihanMahasiswa(Mahasiswa mahasiswa, Integer semester, Kegiatan kegiatan,
			JenisKegiatan jenisKegiatan, List<CicilanPembayaran> cicilanPembayarans, List<Map<String, Object>> maps) {
		Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
				: kegiatan.ambilDetailKegiatan(false);
		PembayaranUtil.getInstance();
		Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan,
				false);

		Double sisaTotal = 0.0;

		Session session = HibernateUtil.currentSession();
		PembayaranUtil.getInstance();
		int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan, semester,
				detailBiayas, false, false);

		Collection biayaBulanan = detailBiayas;
		if (countPengaturanBulanan > 0) {
			PembayaranUtil.getInstance();
			biayaBulanan = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan, "-1", true,
					true);
		}

		for (Object arg1 : biayaBulanan) {

			Map<String, Object> map = new java.util.HashMap<String, Object>();
			if (arg1 instanceof DetailBiaya) {
				DetailBiaya detailBiaya = (DetailBiaya) arg1;

				Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
				Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya, cicilanPembayarans);

				if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null && telahDibayar.intValue() > 0) {
					jumlah = telahDibayar;
				}

				Double sisa = jumlah - telahDibayar;
				sisaTotal += sisa;

				map.put("detailBiaya", detailBiaya);
				map.put("label", detailBiaya.getItemBiaya().getNama());

				map.put("nilai", sisa);
				map.put("telahDibayar", telahDibayar);
				map.put("tagihan", jumlah);

				map.put("jenis_tagihan", jenisKegiatan.getNamaKegiatan());
				map.put("jenis_tagihan_id", jenisKegiatan.getId());

				map.put("ta", kegiatan == null ? "" : kegiatan.getTahunAkademik());
				map.put("smt", kegiatan == null ? 0 : kegiatan.getSemster());

				if (jumlah != null && jumlah.intValue() != 0 && detailBiaya.getItemBiaya() != null
						&& detailBiaya.getItemBiaya().getDitampilkanDiSuratTagihan()) {
					maps.add(map);
				}
			} else if (arg1 instanceof PengaturanPembayaranBulanan) {
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) arg1;

				Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, semester,
						pengaturanPembayaranBulanan);
				Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
						cicilanPembayarans);

				if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null && telahDibayar.intValue() > 0) {
					jumlah = telahDibayar;
				}

				JadwalPembayaran jadwalPembayaran = kegiatan == null ? null : kegiatan.getJadwalPembayaran();
				JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
						&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
								? jadwalPembayaran
								: null;

				Double hasilDenda = pengaturanPembayaranBulanan.checkDenda(jumlah, ais.ui.util.WaktuUtil.getDate(), jdw,
						jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan());
				String desc = pengaturanPembayaranBulanan.getKeterangan();

				desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()) : desc)
						+ ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " "

						+ (hasilDenda.intValue() > jumlah.intValue() ? pengaturanPembayaranBulanan.getInfoDenda() : "");

				Double sisa = jumlah - telahDibayar;
				sisaTotal += sisa;

				map.put("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);
				map.put("detailBiaya", pengaturanPembayaranBulanan.getDetailBiaya());
				map.put("label", desc);
				map.put("nilai", sisa);
				map.put("telahDibayar", telahDibayar);
				map.put("tagihan", jumlah);
				map.put("jenis_tagihan", jenisKegiatan.getNamaKegiatan());
				map.put("jenis_tagihan_id", jenisKegiatan.getId());

				map.put("ta", kegiatan == null ? "" : kegiatan.getTahunAkademik());
				map.put("smt", kegiatan == null ? 0 : kegiatan.getSemster());

				if (jumlah != null && jumlah.intValue() != 0
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
						&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getDitampilkanDiSuratTagihan()) {
					maps.add(map);
				}
			}

		}

		return sisaTotal;
	}

	@SuppressWarnings({ "unchecked" })
	public static void populateTagihanMahasiswa(Mahasiswa mahasiswa, Integer semester, List<Map<String, Object>> maps) {

		List<CicilanPembayaran> cicilanPembayarans = mahasiswa.ambilCicilan();

		Session session = HibernateUtil.currentSession();
		List<JenisKegiatan> jenisKegiatans = session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.not(Restrictions.in("id",
						new Long[] { ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId(),
								ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() })))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		for (JenisKegiatan jenisKegiatan : jenisKegiatans) {
			Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);
			CommonReportHelper.populateMapTagihanMahasiswa(mahasiswa, semester, kegiatan, jenisKegiatan,
					cicilanPembayarans, maps);
		}
	}

	@SuppressWarnings({ "deprecation" })
	public static void prosesSuratTagihan(final Mahasiswa mahasiswa, final String tahunAkademik, final Integer semester,
			final JadwalPembayaran jadwalPembayaran) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		final West west = new West();
		west.setWidth(
				tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null ? "300px" : "0px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig col = new MyColumnConfig();
		col.setParent(columns);
		col.setWidth("35%");
		new MyColumnConfig().setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		final ParameterUmum parameterUmumTanggalJatuhTempo = Common.getParameterUmum(
				"tanggal jatuh tempo surat tagihan " + mahasiswa.getNim() + " " + tahunAkademik + " " + semester,
				Common.dateFormat1.get().format(calendar.getTime()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Jatuh Tempo"));
		final MyDatebox tanggalJatuhTempo;
		row.appendChild(tanggalJatuhTempo = new MyDatebox(
				Common.dateFormat1.get().parse(parameterUmumTanggalJatuhTempo.getNilai())));
		tanggalJatuhTempo.setReadonly(true);
		tanggalJatuhTempo.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumTanggalJatuhTempo.setNilai(Common.dateFormat1.get().format(tanggalJatuhTempo.getValue()));
				Common.refreshUpdate(parameterUmumTanggalJatuhTempo);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		final MyTextbox nomor;
		final ParameterUmum parameterUmum = Common
				.getParameterUmum("surat tagihan " + mahasiswa.getNim() + " " + tahunAkademik + " " + semester, "");
		row.appendChild(nomor = new MyTextbox(parameterUmum.getNilai()));
		nomor.setWidth("90%");
		nomor.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmum.setNilai(nomor.getValue());
				Common.refreshUpdate(parameterUmum);
			}
		});

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_cara_pembayaran_di_surat_tagihan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));
		final Combobox jenisPembayaran = new Combobox();
		row.appendChild(jenisPembayaran);

		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertComboDanSemua(jenisPembayaran, new String[] { "nama", "bank" }, "akun", JenisPembayaran.class,
				"== Tidak menggunakan cara pembayaran ==",
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		final ParameterUmum parameterUmumRekeningPembayaran = Common.getParameterUmum(
				"bank surat tagihan " + mahasiswa.getNim() + " " + tahunAkademik + " " + semester, "");

		// ISOLASI PENCARIAN JENIS PEMBAYARAN
		Session sessionLocal = null;
		JenisPembayaran selectedJenisPembayaran = null;
		try {
			sessionLocal = ais.action.report.Report.openNativeSession();
			selectedJenisPembayaran = (JenisPembayaran) sessionLocal.createCriteria(JenisPembayaran.class)
					.add(parameterUmumRekeningPembayaran.getNilai().isEmpty()
							|| !Common.isNumber(parameterUmumRekeningPembayaran.getNilai())
									? Restrictions.sqlRestriction("false")
									: Restrictions.idEq(Long.parseLong(parameterUmumRekeningPembayaran.getNilai())))
					.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:2554");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (sessionLocal != null && sessionLocal.isOpen()) {
				try {
					sessionLocal.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:2559");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		Common.selectComboItem(jenisPembayaran, selectedJenisPembayaran);
		jenisPembayaran.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) (jenisPembayaran.getSelectedItem() == null
						? null
						: jenisPembayaran.getSelectedItem().getValue());
				parameterUmumRekeningPembayaran
						.setNilai(pilihanJenisPembayaran == null ? "" : pilihanJenisPembayaran.getId().toString());
				Common.refreshUpdate(parameterUmumRekeningPembayaran);
			}
		});
		jenisPembayaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Denda"));
		final MyDoublebox denda;
		final ParameterUmum parameterUmumDenda = Common.getParameterUmum(
				"denda surat tagihan " + mahasiswa.getNim() + " " + tahunAkademik + " " + semester, "0.0");
		row.appendChild(denda = new MyDoublebox(Double.parseDouble(parameterUmumDenda.getNilai())));
		denda.setWidth("90%");
		denda.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumDenda.setNilai((denda.getValue() == null ? 0.0 : denda.getValue()) + "");
				Common.refreshUpdate(parameterUmumDenda);
			}
		});

		final MyFormRow rowItemBiaya1 = new MyFormRow();
		rowItemBiaya1.setStyle("border:0px;background: transparent;");
		rowItemBiaya1.setParent(rows);
		rowItemBiaya1.appendChild(new ais.ui.util.MyLabelConfig("Pilih Tagihan"));
		rowItemBiaya1.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya1, "2");

		final MyFormRow rowItemBiaya2 = new MyFormRow();
		rowItemBiaya2.setStyle("border:0px;background: transparent;");
		rowItemBiaya2.setParent(rows);
		rowItemBiaya2.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya2, "2");

		final Vbox vboxItemBiaya = new Vbox();
		vboxItemBiaya.setParent(rowItemBiaya2);

		final Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		parameters.put("biodata_id", mahasiswa.getId());
		parameters.put("tanggal", tanggal.getValue());
		parameters.put("nomor", nomor.getValue());
		parameters.put("tahunakademik", tahunAkademik);
		parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());

		if (jenisPembayaran.getSelectedItem() != null && jenisPembayaran.getSelectedItem().getValue() != null) {
			JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem().getValue();
			if (pilihanJenisPembayaran.getBank() != null) {
				parameters.put("rekening_pembayaran",
						" melalui Bank " + pilihanJenisPembayaran.getBank().getNama() + "</b>");
			} else {
				parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
			}
		}

		parameters.put("prosentaseDenda", denda.getValue());
		parameters.put("nama", mahasiswa.getNama());
		parameters.put("nim", mahasiswa.getNim());
		parameters.put("semester", semester);
		parameters.put("tahun_akademik", tahunAkademik);

		parameters.put("kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? "(......................)"
				: mahasiswa.getJurusan().getKaprodi().getNama());
		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());

		final List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		if (semester <= 1 || mahasiswa.getMerupakanPindahan()) {
			Session sessionPindah = null;
			try {
				sessionPindah = ais.action.report.Report.openNativeSession();
				BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) sessionPindah
						.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
				if (calonMahasiswa != null) {
					Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(semester,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					populateTagihanCalonMahasiswa(calonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU,
							kegiatan, semester, maps);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:2655");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			} finally {
				if (sessionPindah != null && sessionPindah.isOpen()) {
					try {
						sessionPindah.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:2660");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}
			}
		}

		populateTagihanMahasiswa(mahasiswa, semester, maps);

		Long jenis_tagihan_id = null;
		final List<MyCheckboxConfig> checkboxConfigs = new ArrayList<MyCheckboxConfig>();
		for (Map<String, Object> map : maps) {
			Long idTag = (Long) map.get("jenis_tagihan_id");
			if (jenis_tagihan_id == null || !jenis_tagihan_id.equals(idTag)) {
				new MyLabelBolder(map.get("jenis_tagihan").toString()).setParent(vboxItemBiaya);
				jenis_tagihan_id = idTag;
			}

			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) map
					.get("pengaturanPembayaranBulanan");
			DetailBiaya detailBiaya = (DetailBiaya) map.get("detailBiaya");
			Double nilai = (Double) map.get("nilai");

			MyCheckboxConfig checkBox = new MyCheckboxConfig(
					map.get("label").toString() + ", " + Common.numberFormat.get().format(nilai));
			checkBox.setChecked(nilai > 0.1);
			checkBox.setAttribute("pengaturanPembayaranBulanan", pengaturanPembayaranBulanan);
			checkBox.setAttribute("detailBiaya", detailBiaya);
			checkboxConfigs.add(checkBox);
			checkBox.setParent(vboxItemBiaya);
		}

		parameters.put("maps", maps);
		parameters.put("file_laporan",
				URLEncoder.encode(mahasiswa.getNim() + " " + mahasiswa.getNama() + " Tagihan", "UTF-8"));

		final EventListener eventListenerReport = Report.generatePDFReport("pdf", parameters, "Surat_Tagihan_Mahasiswa",
				ais.ui.util.WaktuUtil.getDate(), west);

		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameters.remove("maps");

				// OPTIMASI: Menggunakan StringBuilder untuk mengumpulkan tagihanData
				StringBuilder tagihanDataBldr = new StringBuilder();
				List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>();
				for (MyCheckboxConfig checkboxConfig : checkboxConfigs) {
					if (checkboxConfig.isChecked()) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulananAwal = (PengaturanPembayaranBulanan) checkboxConfig
								.getAttribute("pengaturanPembayaranBulanan");
						DetailBiaya detailBiayaAwal = (DetailBiaya) checkboxConfig.getAttribute("detailBiaya");

						for (Map<String, Object> map : maps) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) map
									.get("pengaturanPembayaranBulanan");
							DetailBiaya detailBiaya = (DetailBiaya) map.get("detailBiaya");

							if (pengaturanPembayaranBulananAwal != null && pengaturanPembayaranBulanan != null
									&& pengaturanPembayaranBulanan.getId()
											.equals(pengaturanPembayaranBulananAwal.getId())) {
								newMaps.add(map);
								try {
									Double nilai = (Double) map.get("nilai");
									String s = map.get("label").toString() + ", "
											+ Common.numberFormat.get().format(nilai);
									if (tagihanDataBldr.length() > 0)
										tagihanDataBldr.append("; ");
									tagihanDataBldr.append(s);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:2728");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								break;
							} else if (pengaturanPembayaranBulananAwal == null && detailBiayaAwal != null
									&& detailBiaya != null && detailBiaya.getId().equals(detailBiayaAwal.getId())) {
								newMaps.add(map);
								try {
									Double nilai = (Double) map.get("nilai");
									String s = map.get("label").toString() + ", "
											+ Common.numberFormat.get().format(nilai);
									if (tagihanDataBldr.length() > 0)
										tagihanDataBldr.append("; ");
									tagihanDataBldr.append(s);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:2741");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								break;
							}
						}
					}
				}
				parameters.put("tagihanData", tagihanDataBldr.toString());
				parameters.put("maps", newMaps);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						parameters.put("tanggal", tanggal.getValue());
						parameters.put("nomor", nomor.getValue());
						parameters.put("jenis_tagihan", "MAHASISWA");
						parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());
						if (jenisPembayaran.getSelectedItem() != null
								&& jenisPembayaran.getSelectedItem().getValue() != null) {
							JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem()
									.getValue();
							if (pilihanJenisPembayaran.getBank() != null) {
								parameters.put("rekening_pembayaran",
										" melalui Bank " + pilihanJenisPembayaran.getBank().getNama()
												+ " dengan nomor rekening <b>" + "</b>");
							} else {
								parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
							}
						}
						parameters.put("prosentaseDenda", denda.getValue());
						eventListenerReport.onEvent(new Event("", null, parameters));
					}
				});
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		Button tampilkan;
		hbox.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan Tagihan", "/img/print.png"));
		tampilkan.addEventListener("onClick", eventListener);

		Button kirim;
		hbox.appendChild(kirim = new MyToolbarbuttonConfig("Kirim Tagihan", "/img/print.png"));
		kirim.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						String subject = "Tagihan untuk mahasiswa atas nama " + mahasiswa.getNama() + " ("
								+ mahasiswa.getNim() + ")";
						String tagihanData = parameters.get("tagihanData") + "";
						String body = "Terdapat tagihan untuk mahasiswa atas nama " + mahasiswa.getNama() + " ("
								+ mahasiswa.getNim() + ") <br>Isi informasi tagihan adalah sbb:<br>" + tagihanData
								+ ".<br><br>Terima Kasih";
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

						JSONArray userIds = new JSONArray();
						userIds.put(mahasiswa.getNim());
						File file = new File(parameters.get("report_file") + "");

						MailSender.sendMailLampiranTagihan(userIds, subject, body, sender, mahasiswa.getEmail(), null,
								true, mahasiswa, file);
					}
				});
			}
		});

		Common.createDefaultTimer(eventListener);

		if (jadwalPembayaran != null && jadwalPembayaran.getEndDate() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			final MyCheckboxConfig menggunakanDeadline;
			row.appendChild(
					menggunakanDeadline = new MyCheckboxConfig("Jatuh tempo menggunakan masa akhir jadwal pembayaran"));

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				menggunakanDeadline.setChecked(true);
				menggunakanDeadline.setDisabled(true);
				tanggalJatuhTempo.setValue(jadwalPembayaran.getEndDate());
				tanggalJatuhTempo.setDisabled(true);
			} else {
				menggunakanDeadline.setChecked(true);
				tanggalJatuhTempo.setValue(jadwalPembayaran.getEndDate());
			}

			menggunakanDeadline.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tanggalJatuhTempo.setDisabled(menggunakanDeadline.isChecked());
					if (menggunakanDeadline.isChecked()) {
						tanggalJatuhTempo.setValue(jadwalPembayaran.getEndDate());
						parameterUmumTanggalJatuhTempo
								.setNilai(Common.dateFormat1.get().format(tanggalJatuhTempo.getValue()));
						Common.refreshUpdate(parameterUmumTanggalJatuhTempo);
						eventListener.onEvent(arg0);
					}
				}
			});
		}
	}

	public static File onCetakBiodataCalonMahasiswa(final BiodataCalonMahasiswa calonMahasiswa, boolean cetak)
			throws Exception {

		Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		parameters.put("biodata_id", calonMahasiswa.getId());

		calonMahasiswa.putPhoto(parameters);

		List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

		// =========================================================================
		// FASE 1: MEMASUKKAN DATA BIODATA (Lebih efisien memori, tanpa split <=>)
		// =========================================================================

		// Grup "" (Data Registrasi)
		String grupUtama = "";
		addData(maps, grupUtama, "Nomor Pendaftaran", calonMahasiswa.getNoRegistrasi());
		if (calonMahasiswa.getNoUjian() != null) {
			addData(maps, grupUtama, "Nomor Ujian", calonMahasiswa.getNoUjian());
		}
		addData(maps, grupUtama, "Tanggal Pendaftaran", calonMahasiswa.getTanggalPendaftaran() == null ? ""
				: Common.dateFormat6.get().format(calonMahasiswa.getTanggalPendaftaran()));
		addData(maps, grupUtama, "Tahun Akademik", calonMahasiswa.getTahunAkademik());
		addData(maps, grupUtama, "Gelombang Pendaftaran", calonMahasiswa.getGelombangPendaftaran() == null ? ""
				: calonMahasiswa.getGelombangPendaftaran().getNama());
		addData(maps, grupUtama, "Jenis Seleksi",
				(calonMahasiswa.getGelombangPendaftaran() == null
						|| calonMahasiswa.getGelombangPendaftaran().getJenisSeleksi() == null) ? ""
								: calonMahasiswa.getGelombangPendaftaran().getJenisSeleksi().getNama());

		// Grup "I. Data Calon Mahasiswa"
		String grup1 = "I. Data Calon Mahasiswa";
		addData(maps, grup1, "Nama Lengkap", calonMahasiswa.getNama());
		addData(maps, grup1, "Jenis Kartu Identitas", calonMahasiswa.getJenisKartuIdentitas() == null ? ""
				: calonMahasiswa.getJenisKartuIdentitas().getNama());
		addData(maps, grup1, "No Kartu Identitas", calonMahasiswa.getNoIdentitas());
		addData(maps, grup1, "Nomor Induk Siswa Nasional (NISN)", calonMahasiswa.getNisn());
		addData(maps, grup1, "Tempat Lahir", calonMahasiswa.getTempatLahir());
		addData(maps, grup1, "Tanggal Lahir", calonMahasiswa.getTanggalLahir() == null ? ""
				: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir()));
		addData(maps, grup1, "Email", calonMahasiswa.getEmail());
		addData(maps, grup1, "Jenis Kelamin", calonMahasiswa.getJenisKelamin());

		Integer statusNikah = calonMahasiswa.getStatusNikah();
		String strNikah = (statusNikah == null) ? ""
				: (statusNikah.equals(0) ? "Belum Nikah"
						: statusNikah.equals(1) ? "Nikah" : statusNikah.equals(2) ? "Janda" : "Duda");
		addData(maps, grup1, "Status Perkawinan", strNikah);
		addData(maps, grup1, "Agama", calonMahasiswa.getAgama() == null ? "" : calonMahasiswa.getAgama().getNama());
		addData(maps, grup1, "Kewarganegaraan", calonMahasiswa.getKewarganegaraan());
		addData(maps, grup1, "Asal Negara",
				calonMahasiswa.getAsalNegara() == null ? "" : calonMahasiswa.getAsalNegara().getNamaNegara());
		addData(maps, grup1, "Alamat Rumah", calonMahasiswa.getAlamat());
		addData(maps, grup1, "Dusun / Kampung", calonMahasiswa.getDusunCalon());
		addData(maps, grup1, "RT", calonMahasiswa.getRt());
		addData(maps, grup1, "RW", calonMahasiswa.getRw());
		addData(maps, grup1, "Kode Pos", calonMahasiswa.getKodePos());
		addData(maps, grup1, "Kelurahan / Desa", calonMahasiswa.getKelurahanCalon());
		addData(maps, grup1, "Kecamatan",
				calonMahasiswa.getKecamatanCalon() == null ? "" : calonMahasiswa.getKecamatanCalon().getNama());
		addData(maps, grup1, "Kota/Kabupaten",
				calonMahasiswa.getKotaCalon() == null ? "" : calonMahasiswa.getKotaCalon().getNama());
		addData(maps, grup1, "Propinsi",
				calonMahasiswa.getPropinsiCalon() == null ? "" : calonMahasiswa.getPropinsiCalon().getNama());
		addData(maps, grup1, "Telepon (atau HP) / No. WA ", calonMahasiswa.getTeleponRumah());

		// Grup "II. Data Pendidikan Asal"
		String grup2 = "II. Data Pendidikan Asal";
		addData(maps, grup2, "Jenis Pendidikan Sebelumnya",
				calonMahasiswa.getJenisSekolah() == null ? "" : calonMahasiswa.getJenisSekolah().getNama());
		String jurLain = (calonMahasiswa.getJurusanSekolahLain() == null
				|| calonMahasiswa.getJurusanSekolahLain().trim().isEmpty()) ? ""
						: " " + calonMahasiswa.getJurusanSekolahLain();
		addData(maps, grup2, "Nama jurusan pendidikan asal",
				(calonMahasiswa.getJurusanSekolah() == null ? "" : calonMahasiswa.getJurusanSekolah().getNama())
						+ jurLain);
		addData(maps, grup2, "Akreditasi Pendidikan Sebelumnya", calonMahasiswa.getAkreditasiSekolah());
		addData(maps, grup2, "Nama Pendidikan Sebelumnya", calonMahasiswa.getAsalSma());
		addData(maps, grup2, "Alamat Pendidikan Sebelumnya", calonMahasiswa.getAlamatAsalSma());
		addData(maps, grup2, "Kode Pos Pendidikan Sebelumnya", calonMahasiswa.getKodePosSekolah());
		addData(maps, grup2, "Kecamatan Pendidikan Sebelumnya",
				calonMahasiswa.getKecamatanSekolah() == null ? "" : calonMahasiswa.getKecamatanSekolah().getNama());
		addData(maps, grup2, "Kota/Kabupaten Pendidikan Sebelumnya",
				calonMahasiswa.getKotaSekolah() == null ? "" : calonMahasiswa.getKotaSekolah().getNama());
		addData(maps, grup2, "Propinsi Pendidikan Sebelumnya",
				calonMahasiswa.getPropinsiSekolah() == null ? "" : calonMahasiswa.getPropinsiSekolah().getNama());
		addData(maps, grup2, "Tahun Kelulusan", calonMahasiswa.getTahunKelulusan() + "");
		addData(maps, grup2, "Jurusan Pendidikan Sebelumnya",
				calonMahasiswa.getJurusanSekolah() == null ? "" : calonMahasiswa.getJurusanSekolah().getNama());

		// Grup "III. Data Orang Tua/Wali"
		String grup3 = "III. Data Orang Tua/Wali";
		addData(maps, grup3, "Nama Ayah", calonMahasiswa.getNamaAyah());
		addData(maps, grup3, "Nama Ibu", calonMahasiswa.getNamaIbu());
		addData(maps, grup3, "Nama Wali", calonMahasiswa.getNamaWali());
		addData(maps, grup3, "Alamat Orang Tua/Wali", calonMahasiswa.getAlamatOrtu());
		addData(maps, grup3, "RT Orang Tua/Wali", calonMahasiswa.getRtOrtu());
		addData(maps, grup3, "RW Orang Tua/Wali", calonMahasiswa.getRwOrtu());
		addData(maps, grup3, "Kode Pos Orang Tua/Wali", calonMahasiswa.getKodePosOrtu());
		addData(maps, grup3, "Kelurahan Orang Tua/Wali", calonMahasiswa.getKelurahanOrtu());
		addData(maps, grup3, "Kecamatan Orang Tua/Wali",
				calonMahasiswa.getKecamatanOrtu() == null ? "" : calonMahasiswa.getKecamatanOrtu().getNama());
		addData(maps, grup3, "Kabupaten/Kota Orang Tua/Wali",
				calonMahasiswa.getKotaOrtu() == null ? "" : calonMahasiswa.getKotaOrtu().getNama());
		addData(maps, grup3, "Propinsi Orang Tua/Wali",
				calonMahasiswa.getPropinsiOrtu() == null ? "" : calonMahasiswa.getPropinsiOrtu().getNama());
		addData(maps, grup3, "Telepon (atau HP) / No. WA Orang Tua/Wali", calonMahasiswa.getNoTelpOrtu());
		addData(maps, grup3, "Pendidikan Ayah",
				calonMahasiswa.getPendidikanOrtu() == null ? "" : calonMahasiswa.getPendidikanOrtu().getNama());
		addData(maps, grup3, "Pekerjaan Ayah",
				calonMahasiswa.getPekerjaanAyah() == null ? "" : calonMahasiswa.getPekerjaanAyah().getNama());
		addData(maps, grup3, "Pendapatan Ayah",
				calonMahasiswa.getPendapatanOrtu() == null ? ""
						: "Rp. " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtu().getMulaiDari())
								+ " - Rp. "
								+ Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtu().getSampai()));
		addData(maps, grup3, "Pendidikan Ibu",
				calonMahasiswa.getPendidikanOrtuIbu() == null ? "" : calonMahasiswa.getPendidikanOrtuIbu().getNama());
		addData(maps, grup3, "Pekerjaan Ibu",
				calonMahasiswa.getPekerjaanAyahIbu() == null ? "" : calonMahasiswa.getPekerjaanAyahIbu().getNama());
		addData(maps, grup3, "Pendapatan Ibu", calonMahasiswa.getPendapatanOrtuIbu() == null ? ""
				: "Rp. " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuIbu().getMulaiDari())
						+ " - Rp. "
						+ Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuIbu().getSampai()));
		addData(maps, grup3, "Pendidikan Wali",
				calonMahasiswa.getPendidikanOrtuWali() == null ? "" : calonMahasiswa.getPendidikanOrtuWali().getNama());
		addData(maps, grup3, "Pekerjaan Wali",
				calonMahasiswa.getPekerjaanAyahWali() == null ? "" : calonMahasiswa.getPekerjaanAyahWali().getNama());
		addData(maps, grup3, "Pendapatan Wali", calonMahasiswa.getPendapatanOrtuWali() == null ? ""
				: "Rp. " + Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuWali().getMulaiDari())
						+ " - Rp. "
						+ Common.numberFormat.get().format(calonMahasiswa.getPendapatanOrtuWali().getSampai()));

		// Grup "IV. Pilihan Paket"
		addData(maps, "IV. Pilihan Paket", "Pilihan Paket",
				calonMahasiswa.getPaket() == null ? "" : calonMahasiswa.getPaket().getNama());

		// Grup "V. Pilihan Program Studi (Prodi)"
		String grup5 = "V. Pilihan Program Studi (Prodi)";
		if (calonMahasiswa.getProdi1() != null)
			addData(maps, grup5, "Prodi I", calonMahasiswa.getProdi1().getNama());
		if (calonMahasiswa.getProdi2() != null)
			addData(maps, grup5, "Prodi II", calonMahasiswa.getProdi2().getNama());
		if (calonMahasiswa.getProdi3() != null)
			addData(maps, grup5, "Prodi III", calonMahasiswa.getProdi3().getNama());
		if (calonMahasiswa.getProdi4() != null)
			addData(maps, grup5, "Prodi IV", calonMahasiswa.getProdi4().getNama());
		if (calonMahasiswa.getProdi5() != null)
			addData(maps, grup5, "Prodi V", calonMahasiswa.getProdi5().getNama());
		if (calonMahasiswa.getProdiLulus() != null)
			addData(maps, grup5, "Prodi Lulus", calonMahasiswa.getProdiLulus().getNama());

		if (calonMahasiswa.getMahasiswa() == null) {
			addData(maps, grup5, "Program", calonMahasiswa.getProgram());
		} else {
			addData(maps, grup5, "Program",
					calonMahasiswa.getMahasiswa().getProgramBaru() == null ? calonMahasiswa.getMahasiswa().getProgram()
							: calonMahasiswa.getMahasiswa().getProgramBaru().getNamaBaru());
		}

		// Grup "VI. Pilihan Pindahan (Untuk Mahasiswa Pindahan)"
		String grup6 = "VI. Pilihan Pindahan (Untuk Mahasiswa Pindahan)";
		addData(maps, grup6, "Merupakan Pindahan",
				(calonMahasiswa.getMerupakanPindahan() != null && calonMahasiswa.getMerupakanPindahan()) ? "Ya"
						: "Tidak");
		if (calonMahasiswa.getMerupakanPindahan() != null && calonMahasiswa.getMerupakanPindahan()) {
			addData(maps, grup6, "Nama Kampus Sebelum Pindah", calonMahasiswa.getPindahanDariKampus());
			addData(maps, grup6, "Nama Program Studi Sebelum Pindah", calonMahasiswa.getPindahanDariProdi());
			addData(maps, grup6, "NIM/NPM Lama Sebelum Pindah", calonMahasiswa.getNimLamaSebelumPindah());
			addData(maps, grup6, "Pindah Dari Kampus Lama Di Semester",
					calonMahasiswa.getPindahDariKampusLamaDiSemester() + "");
			addData(maps, grup6, "Keterangan / Alasan Pindah", calonMahasiswa.getKeteranganPindah());
		}

		// Data Info Kampus menggunakan StringBuilder (Optimalisasi Memori)
		String grupInfo = "Mendapatkan informasi Pendaftaran Mahasiswa Baru";
		StringBuilder infoBuilder = new StringBuilder();
		if (calonMahasiswa.getInfoKampusDariMana() != null) {
			for (String s : calonMahasiswa.getInfoKampusDariMana().split(";")) {
				if (infoBuilder.length() > 0)
					infoBuilder.append(" dan ");
				infoBuilder.append(s);
			}
		}
		addData(maps, grupInfo, "Dapat info dari", infoBuilder.toString());
		addData(maps, grupInfo, "Nama teman/mahasiswa", calonMahasiswa.getNamaTemanInfoKampusDariMana());
		addData(maps, grupInfo, "Informasi dari", calonMahasiswa.getKeteranganInfoKampusDariMana());

		// Parameter Tambahan
		for (CommonVO commonVO : calonMahasiswa.ambilDataParameterTambahan()) {
			String lbl = commonVO.getName();
			String url = commonVO.getName2();
			String val = commonVO.getName1();
			try {
				if (val != null) {
					String[] d = StringUtils.split(val, ":");
					if (d.length > 1 && Common.isNumber(d[1].trim())) {
						val = d[0];
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3052");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

			if ((val != null && !val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null"))
					|| (url != null && !url.trim().isEmpty())) {
				String[] param = (lbl != null) ? lbl.split("->") : new String[] { "" };
				String groupParam = param[0];
				String labelParam = param.length > 1 ? param[1] : "";
				addDataUrl(maps, groupParam, labelParam, val, url);
			}
		}

		// =========================================================================
		// FASE 2: PENGAMBILAN DATA DATABASE TERPUSAT (Anti-Connection Leak)
		// =========================================================================
		Session sessionLocal = null;
		Transaction tx = null;

		try {
			sessionLocal = ais.action.report.Report.openNativeSession();
			tx = sessionLocal.beginTransaction();

			// A. Ambil Data Foto
			FotoBiodataCalonMahasiswa foto = (FotoBiodataCalonMahasiswa) sessionLocal
					.createCriteria(FotoBiodataCalonMahasiswa.class)
					.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult();

			if (foto != null) {
				if (foto.getLink() != null && foto.getLink().toLowerCase().contains("dropbox")) {
					addData(maps, "Lampiran-Lampiran", "Foto", foto.dropboxLinkRaw());
				} else if (foto.getGdrive() != null) {
					addData(maps, "Lampiran-Lampiran", "Foto", foto.exportGDriveUrl());
				} else if (foto.getFoto() != null) {
					String urlFoto = CommonMedia.getFile(foto.getId(), FotoBiodataCalonMahasiswa.class.getName());
					addData(maps, "Lampiran-Lampiran", "Foto", urlFoto);
				}
			}

			// B. Ambil Data Lampiran (menggunakan loop di dalam SATU session yang sama)
			String[] jenisLampirans = { LampiranLainBiodataCalonMahasiswa.IJAZAH,
					LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI, LampiranLainBiodataCalonMahasiswa.KTP,
					LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1, LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2,
					LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3, LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4,
					LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5,
					LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN,
					LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG };

			for (String jenis : jenisLampirans) {
				Long fileId = (Long) sessionLocal.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
						.setProjection(Projections.property("id")).add(Restrictions.eq("jenis", jenis))
						.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa.getId())).setMaxResults(1)
						.uniqueResult();

				if (fileId != null) {
					String fileUrl = CommonMedia.getFile(fileId, LampiranLainBiodataCalonMahasiswa.class.getName());
					addData(maps, "Lampiran-Lampiran", jenis, fileUrl);
				}
			}

			// C. Ambil Data Verifikasi Berkas
			if (calonMahasiswa.getGelombangPendaftaran() != null) {
				GelombangPendaftaran gel = (GelombangPendaftaran) sessionLocal.get(GelombangPendaftaran.class,
						calonMahasiswa.getGelombangPendaftaran().getId());
				if (gel != null) {
					Set<VerifikasiKelengkapanCalonMahasiswa> verifikasiTemp = gel
							.getVerifikasiKelengkapanCalonMahasiswas();
					JenisSeleksi jenisSeleksi = calonMahasiswa.getJenisSeleksi();
					if (jenisSeleksi != null) {
						jenisSeleksi = (JenisSeleksi) sessionLocal.get(JenisSeleksi.class, jenisSeleksi.getId());
						if (jenisSeleksi != null && !jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas().isEmpty()) {
							verifikasiTemp = jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas();
						}
					}

					List<VerifikasiKelengkapanCalonMahasiswa> verifikasiList = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
							verifikasiTemp);
					try {
						Collections.sort(verifikasiList);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3131");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					for (VerifikasiKelengkapanCalonMahasiswa v : verifikasiList) {
						if (v.getAktif()) {
							BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) sessionLocal
									.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
									.add(Restrictions.eq("verifikasiKelengkapanCalonMahasiswa", v))
									.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).setMaxResults(1)
									.uniqueResult();

							if (berkas == null) {
								berkas = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
								berkas.setBiodataCalonMahasiswa(calonMahasiswa);
								berkas.setVerifikasiKelengkapanCalonMahasiswa(v);
								sessionLocal.saveOrUpdate(berkas);
							}

							String keterangan = (berkas.getKeterangan() == null || berkas.getKeterangan().isEmpty())
									? ""
									: ", " + berkas.getKeterangan();
							String nilaiBerkas = (berkas.getVerified() ? "Telah sesuai" : "Belum Diverifikasi")
									+ keterangan;

							LampiranLain lampiranLain = LampiranLain.ambil(berkas.getId(),
									BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
							String lampUrl = null;
							if (lampiranLain != null) {
								lampUrl = lampiranLain.getGdrive() != null ? lampiranLain.forwardGDriveUrl()
										: CommonMedia.getFile(lampiranLain.getId(), LampiranLain.class.getName());
							}

							addDataUrl(maps, "Verifikasi Kelengkapan Berkas",
									berkas.getVerifikasiKelengkapanCalonMahasiswa().getNama(), nilaiBerkas, lampUrl);
						}
					}
				}
			}

			// D. Ambil Data Verifikasi Nilai Rapor
			if (calonMahasiswa.getPaket() != null) {
				@SuppressWarnings("unchecked")
				List<MatapelajaranSekolah> matapelajaranSekolahs = sessionLocal
						.createCriteria(PaketPunyaMatapelajaran.class)
						.setProjection(Projections.property("matapelajaranSekolah"))
						.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
						.add(Restrictions.eq("paket", calonMahasiswa.getPaket()))
						.add(Restrictions.eq("matapelajaranSekolah.aktif", true))
						.addOrder(Order.asc("matapelajaranSekolah.nama")).list();

				for (MatapelajaranSekolah mps : matapelajaranSekolahs) {
					BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran berkasNilai = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) sessionLocal
							.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.class)
							.add(Restrictions.eq("matapelajaranSekolah", mps))
							.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).setMaxResults(1)
							.uniqueResult();

					if (berkasNilai == null) {
						berkasNilai = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
						berkasNilai.setBiodataCalonMahasiswa(calonMahasiswa);
						berkasNilai.setMatapelajaranSekolah(mps);
						sessionLocal.saveOrUpdate(berkasNilai);
					}

					StringBuilder nilaiBuilder = new StringBuilder();
					if (calonMahasiswa.getPaket().getKelasVerifikasiRapor() != null) {
						for (String nilaikelas : calonMahasiswa.getPaket().getKelasVerifikasiRapor().split(";")) {
							if (!nilaikelas.trim().isEmpty() && berkasNilai.ambilNilai(nilaikelas.trim()) > 0.1
									&& berkasNilai.ambilVerifikasi(nilaikelas.trim())) {
								String[] ca = StringUtils.split(nilaikelas, ":");
								String kel = ca.length > 0 ? ca[0] : "";
								String sem = ca.length > 1 ? ca[1] : "";
								String s = "Kls:" + kel + (sem.isEmpty() ? "" : ", Smt:" + sem) + " = "
										+ Common.numberFormat.get().format(berkasNilai.ambilNilai(nilaikelas.trim()));
								if (nilaiBuilder.length() > 0)
									nilaiBuilder.append(", ");
								nilaiBuilder.append(s);
							}
						}
					}

					String ketNilai = berkasNilai.getKeterangan() == null ? "" : berkasNilai.getKeterangan();
					String nilaiAkhir = nilaiBuilder.toString();
					if (!ketNilai.isEmpty()) {
						nilaiAkhir += (nilaiAkhir.trim().isEmpty() ? "" : ", ") + ketNilai;
					}

					addData(maps, "Verifikasi Nilai Rapor", berkasNilai.getMatapelajaranSekolah().getNama(),
							nilaiAkhir);
				}
			}

			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3228");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:3231");
		} finally {
			if (sessionLocal != null && sessionLocal.isOpen()) {
				try {
					sessionLocal.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3236");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		// =========================================================================
		// FASE 3: PENYUSUNAN PARAMETER & CETAK PDF
		// =========================================================================
		for (Map<String, String> map : maps) {
			try {
				String label = map.get("label");
				String nilai = map.get("nilai");
				if (label != null) {
					parameters.put(label, nilai);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:3252");
			}
		}

		parameters.put("maps", maps);
		parameters.put("nama", calonMahasiswa.getNama());
		parameters.put("file_laporan",
				URLEncoder.encode(
						calonMahasiswa.getNoRegistrasi() + " " + calonMahasiswa.getNama() + " Biodata_Calon_Mahasiswa",
						"UTF-8"));

		if (cetak) {
			return Report.generatePDFReport(Report.PDF, parameters, "Biodata_Calon_Mahasiswa",
					ais.ui.util.WaktuUtil.getDate(), maps);
		} else {
			return Report.generateDownloadReport(Report.PDF, parameters, "Biodata_Calon_Mahasiswa", null,
					ais.ui.util.WaktuUtil.getDate(), Common.locale, false);
		}
	}

	// =========================================================================
	// METHOD PEMBANTU (HELPER) UNTUK EFISIENSI STRUKTUR DATA MAP
	// =========================================================================
	private static void addData(List<Map<String, String>> maps, String group, String label, String value) {
		if (value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("null")) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("grup", group);
			map.put("label", label);
			map.put("nilai", value);
			maps.add(map);
		}
	}

	private static void addDataUrl(List<Map<String, String>> maps, String group, String label, String value,
			String url) {
		if ((value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("null"))
				|| (url != null && !url.trim().isEmpty())) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("grup", group);
			map.put("label", label);
			map.put("nilai", value == null ? "" : value);
			if (url != null) {
				map.put("url", url);
			}
			maps.add(map);
		}
	}

	public static void onCetakSuratPendampingIjazah(Mahasiswa mahasiswa) throws Exception {

		Report.generatePDFReport(Report.PDF,
				LaporanPrestasiMahasiswa.generateParameter(mahasiswa, ais.ui.util.WaktuUtil.getDate()),
				"Prestasi_Mahasiswa", ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakAngkaKreditMahasiswa(Mahasiswa mahasiswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanKegiatanKemahasiswaan.generateParameter(mahasiswa, ais.ui.util.WaktuUtil.getDate()),
				"Angka_Kredit_Kegiatan_Mahasiswa", ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakRekapAngkaKreditMahasiswa(Mahasiswa mahasiswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanKegiatanKemahasiswaan.generateParameter(mahasiswa, ais.ui.util.WaktuUtil.getDate()),
				"Rekap_Angka_Kredit_Kegiatan_Mahasiswa", ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakAngkaKreditSiswa(Siswa siswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanKegiatanKesiswaan.generateParameter(siswa, ais.ui.util.WaktuUtil.getDate()),
				"Angka_Kredit_Kegiatan_Siswa", ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakRekapAngkaKreditSiswa(Siswa siswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanKegiatanKesiswaan.generateParameter(siswa, ais.ui.util.WaktuUtil.getDate()),
				"Rekap_Angka_Kredit_Kegiatan_Siswa", ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakOrganisasiMahasiswa(Mahasiswa mahasiswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanOrganisasiMahasiswa.generateParameter(mahasiswa, ais.ui.util.WaktuUtil.getDate()),
				"Organisasi_Mahasiswa", ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakOrganisasiSiswa(Siswa siswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanOrganisasiSiswa.generateParameter(siswa, ais.ui.util.WaktuUtil.getDate()), "Organisasi_Siswa",
				ais.ui.util.WaktuUtil.getDate());

	}

	public static void onCetakBeasiswaMahasiswa(Mahasiswa mahasiswa) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanBeasiswaMahasiswa.generateParameter(mahasiswa, ais.ui.util.WaktuUtil.getDate()),
				"Beasiswa_Mahasiswa", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static File onCetakPengecualianJadwalPenilaianDosen(
			PengecualianJadwalPenilaianDosen pengecualianJadwalPenilaianDosen) throws Exception {

		if (pengecualianJadwalPenilaianDosen.getDisetujuiOleh() == null) {
			MyMessageboxConfig.show("Mohon maaf, pengajuan ini belum disetujui. Langkah yang dapat dilakukan: (1) Hubungi atasan atau pejabat yang berwenang untuk memberikan persetujuan; (2) Periksa status pengajuan di menu Persetujuan; (3) Ulangi proses cetak setelah pengajuan disetujui. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		String hari = Common.dateFormatHari.get().format(pengecualianJadwalPenilaianDosen.getTanggalMulai());
		String bulan = Common.dateFormatBln.get().format(pengecualianJadwalPenilaianDosen.getTanggalMulai());
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(pengecualianJadwalPenilaianDosen.getTanggalMulai());
		int tanggal = calendar.get(Calendar.DATE);
		int tahun = calendar.get(Calendar.YEAR);
		String tanggalText = IndonesianNumberToWords.convert((long) tanggal);
		Dosen dosen = pengecualianJadwalPenilaianDosen.getDosen();

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		List<Map> maps = new ArrayList<Map>();

		Common.insertProperty(PengecualianJadwalPenilaianDosen.class, pengecualianJadwalPenilaianDosen, parameters, "",
				2);

		parameters.put("hari", hari);
		parameters.put("tanggal", tanggal);
		parameters.put("tanggalText", tanggalText);
		parameters.put("bulan", bulan);
		parameters.put("tahun", tahun);

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		List<Perkuliahan> perkuliahans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(criterion)
						.addOrder(Order.asc("id"))
						.add(Restrictions.eq("tahunAjaran", pengecualianJadwalPenilaianDosen.getTahunAkademik()))
						.add(Restrictions.eq("ganjilGenap", pengecualianJadwalPenilaianDosen.getJenisSemester())),
				Perkuliahan.class);

		for (Perkuliahan perkuliahan : perkuliahans) {
			Map map = new HashMap();
			Common.insertProperty(Perkuliahan.class, perkuliahan, map, "", 2);
			maps.add(map);
		}

		File file = Report.generatePDFReport(Report.PDF, parameters, "berita_acara_penilaian_dosen",
				ais.ui.util.WaktuUtil.getDate(), maps);
		return file;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File onCetakPenyediaAsset(final PenyediaAsset penyediaAsset, boolean download) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("biodata_id", penyediaAsset.getId());

		penyediaAsset.putPhoto(parameters);

		List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

		// =========================================================================
		// FASE 1: MEMASUKKAN DATA TANPA STRING SPLITTING (OPTIMASI MEMORI)
		// =========================================================================

		String grup1 = "Informasi Data Pokok Indetitas Perusahaan";
		addData(maps, grup1, "Kode Penyedia/Perusahaan", penyediaAsset.getKode(), "");
		addData(maps, grup1, "Nama Penyedia/Perusahaan", penyediaAsset.getNama(), "");
		addData(maps, grup1, "Kategori Penyedia/Perusahaan", penyediaAsset.getKategoriPenyediaAsset() == null ? ""
				: penyediaAsset.getKategoriPenyediaAsset().getNama(), "");
		addData(maps, grup1, "Jenis Penyedia/Perusahaan",
				penyediaAsset.getJenisPenyediaAsset() == null ? "" : penyediaAsset.getJenisPenyediaAsset().getNama(),
				"");
		addData(maps, grup1, "Status Penyedia/Perusahaan",
				penyediaAsset.getStatusPenyediaAsset() == null ? "" : penyediaAsset.getStatusPenyediaAsset().getNama(),
				"");
		addData(maps, grup1, "Alamat", penyediaAsset.getAlamat(), "");
		addData(maps, grup1, "Kode Pos", penyediaAsset.getKodePos(), "");
		addData(maps, grup1, "Telp.", penyediaAsset.getTelp(), "");
		addData(maps, grup1, "Fax.", penyediaAsset.getFax(), "");
		addData(maps, grup1, "Kecamatan",
				penyediaAsset.getKecamatan() == null ? "" : penyediaAsset.getKecamatan().getNama(), "");
		addData(maps, grup1, "Kota/Kabupaten", penyediaAsset.getKota() == null ? "" : penyediaAsset.getKota().getNama(),
				"");
		addData(maps, grup1, "Propinsi",
				penyediaAsset.getPropinsi() == null ? "" : penyediaAsset.getPropinsi().getNama(), "");
		addData(maps, grup1, "Longitude", penyediaAsset.getLongitude(), "");
		addData(maps, grup1, "Latitude", penyediaAsset.getLatitude(), "");
		addData(maps, grup1, "Kontak", penyediaAsset.getKontak(), "");
		addData(maps, grup1, "Email", penyediaAsset.getEmail(), "");
		addData(maps, grup1, "Keterangan", penyediaAsset.getKeterangan(), "");

		String grup2 = "Akta Pendirian Perusahaan";
		addData(maps, grup2, "No. Akta Pendirian", penyediaAsset.getNoAktaPendirian(), "");
		addData(maps, grup2, "Tanggal Akta Pendirian", penyediaAsset.getTanggalAktaPendirian() == null ? ""
				: Common.dateFormat2.get().format(penyediaAsset.getTanggalAktaPendirian()), "");
		addData(maps, grup2, "Nama Notaris", penyediaAsset.getNamaNotaris(), "");

		LampiranLain lampiranLain = LampiranLain.ambil(penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Akta_Pendirian");
		addData(maps, grup2, "Dokumen Akta Pendirian", lampiranLain == null ? "(blm upload)" : lampiranLain.getNama(),
				lampiranLain == null ? "" : lampiranLain.createLinkUri());

		addData(maps, grup2, "No. Pengesahan", penyediaAsset.getNoPengesahan(), "");
		addData(maps, grup2, "Tanggal Pengesahan", penyediaAsset.getTanggalPengesahan() == null ? ""
				: Common.dateFormat2.get().format(penyediaAsset.getTanggalPengesahan()), "");

		lampiranLain = LampiranLain.ambil(penyediaAsset.getId(), PenyediaAsset.class.getName() + "_Dokumen_Pengesahan");
		addData(maps, grup2, "Dokumen Pengesahan", lampiranLain == null ? "(blm upload)" : lampiranLain.getNama(),
				lampiranLain == null ? "" : lampiranLain.createLinkUri());

		String grup3 = "Akta Perubahan Terakhir Perusahaan";
		addData(maps, grup3, "No. Akta Pendirian", penyediaAsset.getNoAktaPendirianAkhir(), "");
		addData(maps, grup3, "Tanggal Akta Pendirian", penyediaAsset.getTanggalAktaPendirianAkhir() == null ? ""
				: Common.dateFormat2.get().format(penyediaAsset.getTanggalAktaPendirianAkhir()), "");
		addData(maps, grup3, "Nama Notaris", penyediaAsset.getNamaNotarisAkhir(), "");

		lampiranLain = LampiranLain.ambil(penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Akta_Perubahan_Akhir");
		addData(maps, grup3, "Dokumen Akta Pendirian", lampiranLain == null ? "(blm upload)" : lampiranLain.getNama(),
				lampiranLain == null ? "" : lampiranLain.createLinkUri());

		addData(maps, grup3, "No. Pengesahan", penyediaAsset.getNoPengesahanAkhir(), "");
		addData(maps, grup3, "Tanggal Pengesahan", penyediaAsset.getTanggalPengesahanAkhir() == null ? ""
				: Common.dateFormat2.get().format(penyediaAsset.getTanggalPengesahanAkhir()), "");

		lampiranLain = LampiranLain.ambil(penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Pengesahan_Akhir");
		addData(maps, grup3, "Dokumen Pengesahan", lampiranLain == null ? "(blm upload)" : lampiranLain.getNama(),
				lampiranLain == null ? "" : lampiranLain.createLinkUri());

		String grup4 = "NPWP";
		addData(maps, grup4, "NPWP", penyediaAsset.getNpwp(), "");
		lampiranLain = LampiranLain.ambil(penyediaAsset.getId(), PenyediaAsset.class.getName() + "_Dokumen_NPWP");
		addData(maps, grup4, "Dokumen NPWP", lampiranLain == null ? "(blm upload)" : lampiranLain.getNama(),
				lampiranLain == null ? "" : lampiranLain.createLinkUri());

		String grup5 = "Pakta Integritas";
		lampiranLain = LampiranLain.ambil(penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Pakta_Integritas");
		addData(maps, grup5, "Dokumen Pakta Integritas", lampiranLain == null ? "(blm upload)" : lampiranLain.getNama(),
				lampiranLain == null ? "" : lampiranLain.createLinkUri());

		// =========================================================================
		// FASE 2: MEMASUKKAN REKENING BANK
		// =========================================================================
		try {
			if (penyediaAsset.getBank() != null && !penyediaAsset.getBank().isEmpty()) {
				JSONArray array = new JSONArray(penyediaAsset.getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					String labelBank = jsonObject.isNull("bank") ? "(BANK)" : jsonObject.getString("bank");
					String nilaiRek = jsonObject.isNull("nomorRekening") ? "(No.Rek)"
							: jsonObject.getString("nomorRekening");

					// PERBAIKAN LOGIC: Sebelumnya variabel map ini tidak dimasukkan (add) ke maps
					addData(maps, "Rekening Bank", labelBank, nilaiRek, "");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3527");
			// Jika format JSON salah atau field bank kosong, lanjutkan saja
		}

		// =========================================================================
		// FASE 3: AMBIL DOKUMEN DARI DATABASE DENGAN SESSION TERPUSAT
		// =========================================================================
		Session sessionLocal = null;
		Transaction tx = null;

		try {
			sessionLocal = ais.action.report.Report.openNativeSession();
			tx = sessionLocal.beginTransaction();

			Map<Long, DokumenPenyediaAsset> mapa = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			if (mapa != null) {
				for (DokumenPenyediaAsset dokumenPenyediaAsset : mapa.values()) {
					dokumenPenyediaAssets.add(dokumenPenyediaAsset);
				}
				Collections.sort(dokumenPenyediaAssets);
			}

			for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

				PenyediaAssetPunyaDokumen temp = null;
				if (penyediaAsset != null && penyediaAsset.getId() != null) {
					temp = (PenyediaAssetPunyaDokumen) sessionLocal.createCriteria(PenyediaAssetPunyaDokumen.class)
							.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
							.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1).uniqueResult();
				}

				if (temp == null || temp.getId() == null) {
					temp = new PenyediaAssetPunyaDokumen();
					temp.setDokumenPenyediaAsset(dokumenPenyediaAsset);
					temp.setPenyediaAsset(penyediaAsset);
					// Save untuk dapatkan ID agar bisa ditarik LampiranLain-nya jika diperlukan
					sessionLocal.saveOrUpdate(temp);
				}

				PenyediaAssetPunyaDokumen penyediaAssetPunyaDokumen = temp;
				LampiranLain lampiranLainDokumen = LampiranLain.ambil(penyediaAssetPunyaDokumen.getId(),
						PenyediaAssetPunyaDokumen.class.getName());

				addData(maps, "Daftar Dokumen Persyaratan", dokumenPenyediaAsset.getNama(), temp.getStatus(),
						lampiranLainDokumen == null ? "" : lampiranLainDokumen.createLinkUri());
			}

			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3580");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:3583");
		} finally {
			if (sessionLocal != null && sessionLocal.isOpen()) {
				try {
					sessionLocal.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3588");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		// =========================================================================
		// FASE 4: SETUP PARAMETER CETAK (REPORT)
		// =========================================================================
		Common.insertProperty(PenyediaAsset.class, penyediaAsset, parameters, "data");

		System.out.println("maps Penyedia Asset => " + maps);

		for (Map<String, String> map : maps) {
			try {
				String label = map.get("label");
				String nilai = map.get("nilai");
				if (label != null && nilai != null) {
					parameters.put(label, nilai);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:3608");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}

		parameters.put("maps", maps);

		File file = download ? Report.generateFileReportWithProgress(Report.PDF, parameters, "Penyedia_Asset")
				: Report.generatePDFReport(Report.PDF, parameters, "Penyedia_Asset", ais.ui.util.WaktuUtil.getDate(),
						maps);

		return file;
	}

	// =========================================================================
	// METHOD PEMBANTU (HELPER) UNTUK EFISIENSI STRUKTUR DATA MAP
	// =========================================================================
	private static void addData(List<Map<String, String>> maps, String group, String label, String value, String url) {
		if (value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("null")) {
			Map<String, String> map = new java.util.HashMap<String, String>();
			map.put("grup", group);
			map.put("label", label);
			map.put("nilai", value);
			map.put("url", url == null ? "" : url);
			maps.add(map);
		}
	}

	public static File onCetakBiodataMahasiswa(BiodataMahasiswa biodataMahasiswa) throws Exception {

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		final Mahasiswa mahasiswa = biodataMahasiswa.getMahasiswa();

		parameters.put("biodata_id", biodataMahasiswa.getId());

		// Proteksi NullPointer untuk Fakultas & Jurusan
		if (mahasiswa != null && mahasiswa.getJurusan() != null) {
			parameters.put("jurusan_id", mahasiswa.getJurusan().getId());
			if (mahasiswa.getJurusan().getFakultas() != null) {
				parameters.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
			}
		}

		biodataMahasiswa.putPhoto(parameters);

		List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

		// =========================================================================
		// FASE 1: MEMASUKKAN DATA TANPA STRING SPLITTING (OPTIMASI MEMORI EKSTREM)
		// =========================================================================

		// Grup: Data Mahasiswa
		String grup1 = "Data Mahasiswa";
		addData(maps, grup1, "NIM", mahasiswa.getNim(), "");
		addData(maps, grup1, "Nama", mahasiswa.getNama(), "");
		addData(maps, grup1, "Tahun Angkatan",
				mahasiswa.getTahunangkatan() == null ? "" : mahasiswa.getTahunangkatan().toString(), "");
		addData(maps, grup1, "Tanggal Masuk",
				mahasiswa.getTanggalMasuk() == null ? "" : Common.dateFormat6.get().format(mahasiswa.getTanggalMasuk()),
				"");
		addData(maps, grup1, "Program",
				mahasiswa.getProgramBaru() == null ? mahasiswa.getProgram() : mahasiswa.getProgramBaru().getNamaBaru(),
				"");
		addData(maps, grup1, "Kewarganegaraan", mahasiswa.getWarganegara(), "");
		addData(maps, grup1, "Asal Negara", mahasiswa.getNegara() == null ? "" : mahasiswa.getNegara().getNamaNegara(),
				"");

		String namaFakultas = (mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null) ? ""
				: mahasiswa.getJurusan().getFakultas().getNama();
		addData(maps, grup1, "Fakultas", namaFakultas, "");
		addData(maps, grup1, Common.getBahasaConfig("Jurusan"),
				mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama(), "");

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
		addData(maps, grup1, "Dosen Pembimbing Akademik",
				(krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) ? krsMahasiswa.getDosenPa().getNama() : "",
				"");
		addData(maps, grup1, "Kelas", krsMahasiswa == null ? "" : krsMahasiswa.getKelas(), "");
		addData(maps, grup1, "Jenis Kelamin", mahasiswa.getKelamin(), "");
		addData(maps, grup1, "Tempat Lahir", mahasiswa.getTempatlahir(), "");
		addData(maps, grup1, "Tanggal Lahir",
				mahasiswa.getTanggallahir() == null ? "" : Common.dateFormat6.get().format(mahasiswa.getTanggallahir()),
				"");
		addData(maps, grup1, "Tinggi Badan", mahasiswa.getTinggi_badan() == null ? ""
				: Common.numberFormat.get().format(mahasiswa.getTinggi_badan()), "");
		addData(maps, grup1, "Berat Badan",
				mahasiswa.getBerat_badan() == null ? "" : Common.numberFormat.get().format(mahasiswa.getBerat_badan()),
				"");
		addData(maps, grup1, "Golongan darah", mahasiswa.getGolongan_darah(), "");
		addData(maps, grup1, "Agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama(), "");
		addData(maps, grup1, "Telp.", mahasiswa.getTelp(), "");
		addData(maps, grup1, "Email", mahasiswa.getEmail(), "");
		addData(maps, grup1, "Jenjang", mahasiswa.getJenjang() == null ? "" : mahasiswa.getJenjang().getNama(), "");

		String statusAktif = "";
		try {
			statusAktif = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa)
					.getStatusMahasiswa().getNama();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3705");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		addData(maps, grup1, "Status Mahasiswa", statusAktif, "");

		addData(maps, grup1, "Mulai belajar di semester",
				mahasiswa.getSemesterMulai() == null ? "" : mahasiswa.getSemesterMulai().toString(), "");
		addData(maps, grup1, "Status Awal Mahasiswa",
				mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama(), "");

		// Grup: Biodata Lengkap Mahasiswa
		String grup2 = "Biodata Lengkap Mahasiswa";
		addData(maps, grup2, "Asal pendidikan sebelumnya",
				biodataMahasiswa.getJenisSekolah() == null ? "" : biodataMahasiswa.getJenisSekolah().getNama(), "");
		addData(maps, grup2, "Asal SMA / Sederajat", biodataMahasiswa.getAsalSma(), "");
		addData(maps, grup2, "Nama di Ijazah SMA / Sederajat", biodataMahasiswa.getNamaUntukIjazah(), "");
		addData(maps, grup2, "No Ijazah SMA / Sederajat", biodataMahasiswa.getNoIjazah(), "");
		addData(maps, grup2, "Alamat Asal SMA / Sederajat", biodataMahasiswa.getAlamatAsalSma(), "");
		addData(maps, grup2, "Asal SMP / Sederajat", biodataMahasiswa.getAsalSmp(), "");
		addData(maps, grup2, "Alamat Asal SMP / Sederajat", biodataMahasiswa.getAlamatAsalSmp(), "");
		addData(maps, grup2, "Asal SD / Sederajat", biodataMahasiswa.getAsalSd(), "");
		addData(maps, grup2, "Alamat Asal SD / Sederajat", biodataMahasiswa.getAlamatAsalSd(), "");

		Boolean pernahPaud = biodataMahasiswa.getApakahPernahPaud();
		addData(maps, grup2, "Pernah PAUD (Pendidikan Anak Usia Dini)",
				(pernahPaud != null && pernahPaud) ? "Ya" : "Tidak", "");

		Boolean pernahTk = biodataMahasiswa.getApakahPernahTk();
		addData(maps, grup2, "Pernah TK (Taman kanak-Kanak)", (pernahTk != null && pernahTk) ? "Ya" : "Tidak", "");

		addData(maps, grup2, "Tempat Tinggal Saat Kuliah", biodataMahasiswa.getJenisTinggalMahasiswa() == null ? ""
				: biodataMahasiswa.getJenisTinggalMahasiswa().getNama(), "");
		addData(maps, grup2, "Ukuran Jaket", biodataMahasiswa.getUkuranJaket(), "");
		addData(maps, grup2, "HP", biodataMahasiswa.getHp(), "");
		addData(maps, grup2, "Operator Seluler",
				biodataMahasiswa.getOperatorSeluler() == null ? "" : biodataMahasiswa.getOperatorSeluler().getNama(),
				"");
		addData(maps, grup2, "Nomor Surat Ijin Mengemudi", biodataMahasiswa.getSuratIzinMengemudi(), "");
		addData(maps, grup2, "Transportasi Mahasiswa Saat Kuliah",
				biodataMahasiswa.getAlatTransportasiMahasiswa() == null ? ""
						: biodataMahasiswa.getAlatTransportasiMahasiswa().getNama(),
				"");
		addData(maps, grup2, "Kendaraan Kuliah", biodataMahasiswa.getKendaraanKuliah(), "");

		Integer pernPimpin = biodataMahasiswa.getPernahMemimpinOrganisasi();
		addData(maps, grup2, "Organisasi Intra Kampus yang diiskuti",
				(pernPimpin != null && pernPimpin.equals(0)) ? "Tidak" : "Ya", "");
		addData(maps, grup2, "Jabatan dalam organisasi mahasiswa", biodataMahasiswa.getNamaOrganisasi(), "");
		addData(maps, grup2, "Hobi", biodataMahasiswa.getHobi(), "");
		addData(maps, grup2, "Minat Seni", biodataMahasiswa.getMinatSeni(), "");
		addData(maps, grup2, "Kemampuan Bahasa 1", biodataMahasiswa.getKemampuanBahasa1(), "");
		addData(maps, grup2, "Kemampuan Bahasa 2", biodataMahasiswa.getKemampuanBahasa2(), "");
		addData(maps, grup2, "Kemampuan Bahasa 3", biodataMahasiswa.getKemampuanBahasa3(), "");

		Integer sttNikah = biodataMahasiswa.getStatusNikah();
		String strNikah = (sttNikah == null) ? ""
				: (sttNikah.equals(0) ? "Belum Nikah"
						: sttNikah.equals(1) ? "Nikah" : sttNikah.equals(2) ? "Janda" : "Duda");
		addData(maps, grup2, "Status Perkawinan", strNikah, "");
		addData(maps, grup2, "Agama", biodataMahasiswa.getAgama() == null ? "" : biodataMahasiswa.getAgama().getNama(),
				"");

		// Grup: Alamat Mahasiswa
		String grup3 = "Alamat Mahasiswa";
		addData(maps, grup3, "Nomor KTP tanpa tanda baca", biodataMahasiswa.getNoIdentitas(), "");
		addData(maps, grup3, "Alamat Rumah", biodataMahasiswa.getAlamat(), "");
		addData(maps, grup3, "Dusun / Kampung", biodataMahasiswa.getDusun(), "");
		addData(maps, grup3, "RT", biodataMahasiswa.getRt(), "");
		addData(maps, grup3, "RW", biodataMahasiswa.getRw(), "");
		addData(maps, grup3, "Kode Pos", biodataMahasiswa.getKodepos(), "");
		addData(maps, grup3, "Kelurahan / Desa", biodataMahasiswa.getKelurahan(), "");
		addData(maps, grup3, "Kecamatan",
				biodataMahasiswa.getKecamatan() == null ? "" : biodataMahasiswa.getKecamatan().getNama(), "");
		addData(maps, grup3, "Kota / Kabupaten",
				biodataMahasiswa.getKota() == null ? "" : biodataMahasiswa.getKota().getNama(), "");
		addData(maps, grup3, "Propinsi",
				biodataMahasiswa.getPropinsi() == null ? "" : biodataMahasiswa.getPropinsi().getNama(), "");
		addData(maps, grup3, "Telepon Rumah ", biodataMahasiswa.getTeleponRumah(), "");

		// Grup: Data Orang Tua Mahasiswa
		String grup4 = "Data Orang Tua Mahasiswa";
		addData(maps, grup4, "No. KK", biodataMahasiswa.getNoKK(), "");
		addData(maps, grup4, "Nama Ayah", biodataMahasiswa.getNamaAyah(), "");
		addData(maps, grup4, "Tanggal Lahir Ayah", biodataMahasiswa.getTanggalLahirAyah() == null ? ""
				: Common.dateFormat6.get().format(biodataMahasiswa.getTanggalLahirAyah()), "");
		addData(maps, grup4, "Jenis Pekerjaan Ayah", biodataMahasiswa.getJenisPekerjaanAyah() == null ? ""
				: biodataMahasiswa.getJenisPekerjaanAyah().getNama(), "");
		addData(maps, grup4, "Rata-rata penghasilan ayah", biodataMahasiswa.getJenisPenghasilanAyah() == null ? ""
				: biodataMahasiswa.getJenisPenghasilanAyah().getNama(), "");
		addData(maps, grup4, "Jenjang Pendidikan Ayah", biodataMahasiswa.getJenjangPendidikanAyah() == null ? ""
				: biodataMahasiswa.getJenjangPendidikanAyah().getNama(), "");
		addData(maps, grup4, "Nama Ibu", biodataMahasiswa.getNamaIbu(), "");
		addData(maps, grup4, "Tanggal Lahir Ibu", biodataMahasiswa.getTanggalLahirIbu() == null ? ""
				: Common.dateFormat6.get().format(biodataMahasiswa.getTanggalLahirIbu()), "");
		addData(maps, grup4, "Jenis Pekerjaan Ibu", biodataMahasiswa.getJenisPekerjaanIbu() == null ? ""
				: biodataMahasiswa.getJenisPekerjaanIbu().getNama(), "");
		addData(maps, grup4, "Rata-rata penghasilan ibu", biodataMahasiswa.getJenisPenghasilanIbu() == null ? ""
				: biodataMahasiswa.getJenisPenghasilanIbu().getNama(), "");
		addData(maps, grup4, "Jenjang Pendidikan Ibu", biodataMahasiswa.getJenjangPendidikanIbu() == null ? ""
				: biodataMahasiswa.getJenjangPendidikanIbu().getNama(), "");
		addData(maps, grup4, "Jumlah anggota keluarga",
				biodataMahasiswa.getBersaudara() == null ? "" : biodataMahasiswa.getBersaudara().toString(), "");

		// Grup: Data Orang Tua Wali
		String grup5 = "Data Orang Tua Wali";
		addData(maps, grup5, "Nama Wali", biodataMahasiswa.getNamaWali(), "");
		addData(maps, grup5, "Tanggal Lahir Wali", biodataMahasiswa.getTanggalLahirWali() == null ? ""
				: Common.dateFormat6.get().format(biodataMahasiswa.getTanggalLahirWali()), "");
		addData(maps, grup5, "Jenis Pekerjaan Wali", biodataMahasiswa.getJenisPekerjaanWali() == null ? ""
				: biodataMahasiswa.getJenisPekerjaanWali().getNama(), "");
		addData(maps, grup5, "Rata-rata penghasilan wali", biodataMahasiswa.getJenisPenghasilanWali() == null ? ""
				: biodataMahasiswa.getJenisPenghasilanWali().getNama(), "");
		addData(maps, grup5, "Jenjang Pendidikan Wali", biodataMahasiswa.getJenjangPendidikanWali() == null ? ""
				: biodataMahasiswa.getJenjangPendidikanWali().getNama(), "");

		// Parameter Tambahan & Alumni
		List<CommonVO> listParam = new ArrayList<CommonVO>();
		if (biodataMahasiswa.ambilDataParameterTambahan() != null)
			listParam.addAll(biodataMahasiswa.ambilDataParameterTambahan());
		if (biodataMahasiswa.ambilDataParameterTambahanAlumni() != null)
			listParam.addAll(biodataMahasiswa.ambilDataParameterTambahanAlumni());

		for (CommonVO commonVO : listParam) {
			String lbl = commonVO.getName();
			String url = commonVO.getName2();
			String val = commonVO.getName1();
			try {
				if (val != null) {
					String[] d = StringUtils.split(val, ":");
					if (d.length > 1 && Common.isNumber(d[1].trim())) {
						val = d[0];
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3837");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

			if ((val != null && !val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null"))
					|| (url != null && !url.trim().isEmpty())) {
				String[] param = (lbl != null) ? lbl.split("->") : new String[] { "" };
				String groupParam = param[0];
				String labelParam = param.length > 1 ? param[1] : "";
				addData(maps, groupParam, labelParam, val, url);
			}
		}

		// =========================================================================
		// FASE 2: PENGAMBILAN LAMPIRAN (Isolasi Database & Anti-Connection Leak)
		// =========================================================================
		Session streamingSession = null;
		Transaction tx = null;
		try {
			streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			tx = streamingSession.beginTransaction();

			// 1. Ambil Foto
			FotoMahasiswa foto = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();

			if (foto != null && foto.getFoto() != null) {
				String urlFoto = CommonMedia.getFile(foto.getId(), FotoMahasiswa.class.getName());
				addData(maps, "Lampiran-Lampiran", "Foto", urlFoto, "");
			}

			// 2. Ambil Seluruh Jenis Lampiran
			String[] arrJenis = { LampiranLainMahasiswa.IJAZAH, LampiranLainMahasiswa.TRANSKRIP_NILAI,
					LampiranLainMahasiswa.KTP, LampiranLainMahasiswa.AKTE,
					LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI, LampiranLainMahasiswa.NPWP,
					LampiranLainMahasiswa.KK, LampiranLainMahasiswa.KTP_AYAH, LampiranLainMahasiswa.KTP_IBU,
					LampiranLainMahasiswa.KTP_WALI, LampiranLainMahasiswa.LAMPIRAN_1, LampiranLainMahasiswa.LAMPIRAN_2,
					LampiranLainMahasiswa.LAMPIRAN_3, LampiranLainMahasiswa.LAMPIRAN_4,
					LampiranLainMahasiswa.LAMPIRAN_5 };

			for (String jenis : arrJenis) {
				Long fileId = (Long) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.property("id")).add(Restrictions.eq("jenis", jenis))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();

				if (fileId != null) {
					String urlFile = CommonMedia.getFile(fileId, LampiranLainMahasiswa.class.getName());
					addData(maps, "Lampiran-Lampiran", jenis, urlFile, "");
				}
			}

			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3892");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:3895");
		} finally {
			if (streamingSession != null && streamingSession.isOpen()) {
				try {
					streamingSession.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:3900");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		// =========================================================================
		// FASE 3: PENYUSUNAN PARAMETER & CETAK PDF
		// =========================================================================
		parameters.put("nama", mahasiswa.getNama());
		System.out.println("maps Biodata Mahasiswa => " + maps);

		for (Map<String, String> map : maps) {
			try {
				String label = map.get("label");
				String nilai = map.get("nilai");
				if (label != null && nilai != null) {
					parameters.put(label, nilai);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:3919");
				PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Common Report Helper", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}

		File file = Report.generatePDFReport(Report.PDF, parameters, "Biodata_Mahasiswa",
				ais.ui.util.WaktuUtil.getDate(), maps);
		return file;
	}

	@SuppressWarnings({})
	public static boolean onCetakKartuUjianPMB(Tbmuser tbmuser, BiodataCalonMahasiswa biodataCalonMahasiswa,
			String nomorUjian) throws Exception {
		return onCetakKartuUjianPMB(tbmuser, biodataCalonMahasiswa, nomorUjian, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean onCetakKartuUjianPMB(Tbmuser tbmuser, BiodataCalonMahasiswa biodataCalonMahasiswa,
			String nomorUjian, File bio) throws Exception {

		// Validasi dasar, cegah NPE jika parameter utamanya null
		if (biodataCalonMahasiswa == null) {
			return false;
		}

		Session sessionLocal = null;

		try {
			GelombangPendaftaran gelombang = biodataCalonMahasiswa.getGelombangPendaftaran();

			// 1. Validasi Verifikasi Dokumen
			if (gelombang != null && gelombang.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()) {
				if (!VerifikasiPMBHelper.checkVerifikasi(biodataCalonMahasiswa)) {
					return false;
				}
			}

			// 2. Validasi Kelengkapan Biodata
			List<String> daftarWajibDiisi = KonfigurasiTampilanBiodataCalonMahasiswaAction.dataYangWajibDiisi(tbmuser);
			if (daftarWajibDiisi != null) {
				for (String key : daftarWajibDiisi) {
					if (Common.checkIsNull(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, key)) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, biodata Anda harus dilengkapi terlebih dahulu. Data \"{V1}\" masih belum terisi dengan benar. Langkah yang dapat dilakukan: (1) lengkapi data tersebut pada formulir biodata; (2) pastikan seluruh isian telah sesuai; (3) simpan kembali biodata Anda.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, KonfigurasiTampilanBiodataCalonMahasiswaAction.keyDesc(key));
						return false;
					}
				}
			}

			// 3. Validasi Syarat Pembayaran Registrasi
			if (gelombang != null && gelombang.getHarusBayarSebelumBisaLogin()) {
				Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();

				if (kegiatan == null || kegiatan.getId() == null || !kegiatan.getLunas()) {
					String infoBelumBayar = Common.getKonfigurasi("infoBelumbayarSaatProsescalonMahasiswa",
							"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan proses pembayaran.")
							.getNilai();

					// Gunakan org.apache.commons.lang3.StringUtils.replace untuk efisiensi
					infoBelumBayar = org.apache.commons.lang3.StringUtils.replace(infoBelumBayar, "[noreg]",
							biodataCalonMahasiswa.getNoRegistrasi());

					MyMessageboxConfig.show(infoBelumBayar, "PERINGATAN", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}

			// 4. Generate atau Ambil Nomor Ujian
			String noUjian = (biodataCalonMahasiswa.getNoUjian() == null
					|| biodataCalonMahasiswa.getNoUjian().trim().isEmpty())
							? CommonPMB.generateNoUjian(tbmuser, biodataCalonMahasiswa)
							: biodataCalonMahasiswa.getNoUjian();

			if (noUjian == null || noUjian.trim().isEmpty()) {
				return false;
			}

			// =====================================================================
			// Buka Session Database HANYA DI SINI untuk menghemat Connection Pool
			// Karena tahap di atas tidak memerlukan kueri berat.
			// =====================================================================
			sessionLocal = ais.action.report.Report.openNativeSession();

			RuangPaketPMB ruangPaketPMB = (RuangPaketPMB) sessionLocal.createCriteria(RuangPaketPMB.class)
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)).setMaxResults(1)
					.uniqueResult();

			if (ruangPaketPMB == null) {
				// Sinkronkan noUjian di objek memori jika baru saja di-generate di langkah 4
				if ((biodataCalonMahasiswa.getNoUjian() == null
						|| biodataCalonMahasiswa.getNoUjian().trim().isEmpty())
						&& noUjian != null && !noUjian.trim().isEmpty()) {
					biodataCalonMahasiswa.setNoUjian(noUjian);
				}
				// Auto-assign ruang ujian — selalu dicoba saat cetak, tanpa tergantung konfigurasi
				ruangPaketPMB = CommonPMB.dapatkanRuangUjian(biodataCalonMahasiswa);
				if (ruangPaketPMB != null) {
					System.out.println("INFO PMB auto-assign ruang saat cetak: calon ID="
							+ biodataCalonMahasiswa.getId()
							+ " → ruangPaketPMB ID=" + ruangPaketPMB.getId());
				} else {
					// Tidak ada ruang yang dikonfigurasi admin → lanjutkan cetak tanpa blokir
					System.out.println("INFO PMB: tidak ada ruang ujian tersedia untuk calon ID="
							+ biodataCalonMahasiswa.getId()
							+ "; kartu dicetak tanpa info ruang.");
				}
			}

			// 5. Modifikasi state memori (sesuai original code)
			biodataCalonMahasiswa.put("1", "setCetakKartu");
			biodataCalonMahasiswa.setCetakKartu(1);

			// 6. Siapkan Parameter Laporan
			Map parameters = ais.common.HashMapGenerator.getRand();
			Common.insertProperty(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, parameters, "pmb");
			parameters.put("biodata_id", biodataCalonMahasiswa.getId());
			// Pastikan jika argumen nomorUjian null, digantikan dengan noUjian ter-generate
			parameters.put("nomorUjian", (nomorUjian != null && !nomorUjian.trim().isEmpty()) ? nomorUjian : noUjian);

			biodataCalonMahasiswa.putPhoto(parameters);

			// 7. Eksekusi Engine PDF Report
			Report.generatePDFReport(Report.PDF, parameters, "KartuUjianSpmbMandiri", ais.ui.util.WaktuUtil.getDate());

			File file = Report.generateDownloadReport(Report.PDF, parameters, "KartuUjianSpmbMandiri", null,
					ais.ui.util.WaktuUtil.getDate());

			// 8. Kirim Email Notifikasi
			if (bio != null) {
				CommonEmail.infoDaftarUjianMahasiswa(biodataCalonMahasiswa, new File[] { file, bio });
			} else {
				CommonEmail.infoDaftarUjianMahasiswa(biodataCalonMahasiswa, new File[] { file });
			}

			return true;

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/report/CommonReportHelper.java:4057");
			return false;
		} finally {
			// =====================================================================
			// TUTUP SESSION SECARA MUTLAK (Mencegah Database Server Hang/Macat)
			// =====================================================================
			if (sessionLocal != null && sessionLocal.isOpen()) {
				try {
					sessionLocal.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:4066");
					// Silent fail
				}
			}
		}
	}

	public static MyToolbarbuttonConfig cetakData(final Object obj, final String label, final String... columns) {

		HibernateUtil.currentSession().refresh(obj);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(label, "/img/print.png");

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				ais.action.report.helper.LoadingReportUtil.showBusy(label);

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_satu_data_"
								+ URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							ais.action.report.helper.LoadingReportUtil.showBusy(label);
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								ais.action.report.helper.LoadingReportUtil.clearBusy();
								ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
							} else if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								Common.clear(center);
								spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());
								spreadsheet.setMaxrows(columns.length + 1);
								spreadsheet.setMaxcolumns(2);

								South south = new South();
								south.setParent(borderlayout);

								Toolbar toolbar = new Toolbar();
								// toolbar.setHeight("25px");
								toolbar.setParent(south);
								MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
								cancel.setTooltiptext("Tutup");
								cancel.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										window.detach();
									}
								});
								cancel.setParent(toolbar);

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file),
													"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:4161");
											PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Common Report Helper", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
												new String[] {
													"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
													"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});

										}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								ais.action.report.helper.LoadingReportUtil.clearBusy();
								ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
							}

						} catch (Exception e) {
							ais.action.report.helper.LoadingReportUtil.clearBusy();
						}

					}
				});
				timer.start();

				try {

					ais.action.report.helper.LoadingReportUtil.showBusy(label);

					new Thread(new Runnable() {

						@SuppressWarnings("deprecation")
						@Override
						public void run() {

							try {

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("PRINT DATA");
								sheet.setDefaultColumnWidth(20);

								Class<? extends Object> clazz = obj.getClass();
								ClassMetadata metadata = HibernateUtil.getClassMetadata(clazz);

								try {
									label.setValue("Sedang memproses data " + obj.toString());

									for (int i = 0; i < columns.length; i++) {
										XSSFRow row = sheet.createRow(i);
										String content = "";
										String property = columns[i];
										try {
											content = (property.equals("") ? obj + ""
													: "" + metadata.getPropertyValue(obj, property, EntityMode.POJO));
										} catch (Exception e) {
											content = (property.equals("") ? obj + ""
													: "" + metadata.getIdentifier(obj, EntityMode.POJO));
										}

										row.createCell(0).setCellValue(property.toUpperCase());

										row.createCell(1).setCellValue(
												content == null || content.trim().equalsIgnoreCase("null") ? ""
														: content);
									}

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println("Your excel file has been generated! ");

								ais.action.report.helper.LoadingReportUtil.selesai(label);
							} catch (Exception e) {
								label.setValue("-");
							}

						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Common Report Helper", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		return toolbarbutton;
	}

	@SuppressWarnings({})
	public static MyWindow onCetakAbsensiPMBFoto() throws Exception {
		return onCetakAbsensiPMBFoto(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static MyWindow onCetakAbsensiPMBFoto(final RuangPMB ruang) throws Exception {

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		final Combobox pilihanProdi = new Combobox();
		pilihanProdi.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(" Pilihan 1");
		comboitem.setValue("1");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 2");
		comboitem.setValue("2");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 3");
		comboitem.setValue("3");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 4");
		comboitem.setValue("4");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 5");
		comboitem.setValue("5");
		pilihanProdi.appendChild(comboitem);

		pilihanProdi.setSelectedIndex(0);

		final Combobox pilihanJadwal = new Combobox();
		pilihanJadwal.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertComboDanSemua(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.pilihJurusan(jurusan, null);
		jurusan.setReadonly(true);

		final MyCheckboxConfig gabungSemua = new MyCheckboxConfig("Gabung Semua");
		gabungSemua.setChecked(ruang == null);
		gabungSemua.setVisible(ruang == null);

		final Combobox tahunAkademik = Common.generateTahunAjaran(null);
		final Combobox searchGelombang = new Combobox();
		searchGelombang.setReadonly(true);
		tahunAkademik.setReadonly(true);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);

		List<JadwalUjianPMB> jadwalUjianPMBs = HibernateUtil.currentSession().createCriteria(JadwalUjianPMB.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.createAlias("ujianPMB", "ujianPMB")
				.add(Restrictions.eq("ujianPMB.tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
				.add(ruang == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ujianPMB", ruang.getUjianPMB()))
				.add(ruang == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("paket", ruang.getPaket()), Restrictions.isNull("paket")))
				.addOrder(Order.asc("waktuMulai")).list();
		for (JadwalUjianPMB jadwalUjianPMB : jadwalUjianPMBs) {
			String waktu = Common.dateFormat51.get().format(jadwalUjianPMB.getWaktuMulai()) + " s.d "
					+ Common.timeFormat.get().format(jadwalUjianPMB.getWaktuSampai()) + " / " + jadwalUjianPMB.getNama()
					+ " / " + jadwalUjianPMB.getUjianPMB().getNama();
			comboitem = new MyComboitemConfig(waktu);
			comboitem.setValue(jadwalUjianPMB);
			pilihanJadwal.appendChild(comboitem);
		}

		if (!pilihanJadwal.getChildren().isEmpty()) {
			pilihanJadwal.setSelectedIndex(0);
		}

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		final String formatLaporan = "pdf";
		final String file = "AbsensiPMBPilihanProdi";
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("ujian", ruang == null || ruang.getUjianPMB() == null ? -1L : ruang.getUjianPMB().getId());
		parameters.put("ruang", ruang == null || ruang.getId() == null ? -1 : ruang.getId());
		parameters.put("tahunakademik", ruang == null ? "" : ruang.getTahunAkademik());
		parameters.put("gelombang_pendaftaran",
				ruang == null || ruang.getUjianPMB() == null || ruang.getUjianPMB().getGelombangPendaftaran() == null
						? ""
						: ruang.getUjianPMB().getGelombangPendaftaran().getNama());
		parameters.put("ket_ruang", ruang == null ? ""
				: ruang.getNama() + (ruang.getGedung() == null ? "" : " ( " + ruang.getGedung().getNama() + " )"));

		parameters.put("paket", ruang == null ? "" : ruang.getPaket().getNama());

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataAlbumPMBAdmin(ruang,
								"prodi" + pilihanProdi.getSelectedItem().getValue(),
								(JadwalUjianPMB) (pilihanJadwal.getSelectedItem() == null ? null
										: pilihanJadwal.getSelectedItem().getValue()),
								(String) tahunAkademik.getSelectedItem().getValue(),
								(GelombangPendaftaran) (searchGelombang.getSelectedItem() == null ? null
										: searchGelombang.getSelectedItem().getValue()),
								(Jurusan) (jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue()),
								gabungSemua.isChecked());
						File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file,
								ais.ui.util.WaktuUtil.getDate(), maps, bar, (Locale) arg0.getData());
						// Pratinjau HTML (mirip PDF) default + toggle ke PDF.
						Report.tampil(myfile, center);
					}
				});
			}
		};

		South south = new South();
		south.setParent(borderlayout);
		Toolbar toolbar;
		south.appendChild(toolbar = new Toolbar());

		toolbar.appendChild(pilihanProdi);
		pilihanProdi.addEventListener("onChange", eventListener);

		toolbar.appendChild(jurusan);
		jurusan.addEventListener("onChange", eventListener);

		toolbar.appendChild(tahunAkademik);

		final EventListener eventListenerTahunAkademik = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(pilihanJadwal);
				List<JadwalUjianPMB> jadwalUjianPMBs = HibernateUtil.currentSession()
						.createCriteria(JadwalUjianPMB.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.createAlias("ujianPMB", "ujianPMB")
						.add(Restrictions.eq("ujianPMB.tahunAkademik", tahunAkademik.getSelectedItem().getValue()))
						.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPMB.gelombangPendaftaran",
										searchGelombang.getSelectedItem().getValue()))
						.add(ruang == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPMB", ruang.getUjianPMB()))
						.add(ruang == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("paket", ruang.getPaket()),
										Restrictions.isNull("paket")))
						.addOrder(Order.asc("waktuMulai")).list();
				for (JadwalUjianPMB jadwalUjianPMB : jadwalUjianPMBs) {
					String waktu = Common.dateFormat51.get().format(jadwalUjianPMB.getWaktuMulai()) + " s.d "
							+ Common.timeFormat.get().format(jadwalUjianPMB.getWaktuSampai()) + " / "
							+ jadwalUjianPMB.getNama() + " / " + jadwalUjianPMB.getUjianPMB().getNama();
					MyComboitemConfig comboitem = new MyComboitemConfig(waktu);
					comboitem.setValue(jadwalUjianPMB);
					pilihanJadwal.appendChild(comboitem);
				}

				if (!pilihanJadwal.getChildren().isEmpty()) {
					pilihanJadwal.setSelectedIndex(0);
				} else {
					pilihanJadwal.setSelectedItem(null);
				}
				eventListener.onEvent(null);
			}
		};

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								tahunAkademik.getSelectedItem() == null
										|| tahunAkademik.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														tahunAkademik.getSelectedItem().getValue())));
				if (!searchGelombang.getChildren().isEmpty()) {
					searchGelombang.setSelectedIndex(0);
				}
				eventListenerTahunAkademik.onEvent(null);
			}
		});

		toolbar.appendChild(searchGelombang);

		toolbar.appendChild(pilihanJadwal);
		pilihanJadwal.addEventListener("onChange", eventListener);

		searchGelombang.addEventListener("onChange", eventListenerTahunAkademik);

		toolbar.appendChild(gabungSemua);
		gabungSemua.addEventListener("onClick", eventListener);

		MyButtonConfig toolbarbutton = new MyButtonConfig("XLS");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataAlbumPMBAdmin(ruang,
								"prodi" + pilihanProdi.getSelectedItem().getValue(),
								(JadwalUjianPMB) (pilihanJadwal.getSelectedItem() == null ? null
										: pilihanJadwal.getSelectedItem().getValue()),
								(String) tahunAkademik.getSelectedItem().getValue(),
								(GelombangPendaftaran) (searchGelombang.getSelectedItem() == null ? null
										: searchGelombang.getSelectedItem().getValue()),
								(Jurusan) (jurusan.getSelectedItem() == null
										|| jurusan.getSelectedItem().getValue() == null ? null
												: jurusan.getSelectedItem().getValue()),
								gabungSemua.isChecked());
						File myfile = Report.generateFileReportWithProgress(Report.XLS, parameters, file,
								ais.ui.util.WaktuUtil.getDate(), maps, bar, (Locale) arg0.getData());
						final AMedia amedia = new AMedia(file + ".xlsx", formatLaporan,
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
								new FileInputStream(myfile));
						Filedownload.save(amedia);
					}
				});

			}
		});

		Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue())));
		if (!searchGelombang.getChildren().isEmpty()) {
			searchGelombang.setSelectedIndex(0);
		}
		eventListenerTahunAkademik.onEvent(null);

		return window;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static MyWindow onCetakDataPMBFoto() throws Exception {

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		String[] col = new String[] { "jenis_seleksi-jenisSeleksi-Jenis Seleksi",
				"gelombang_pendaftaran-gelombangPendaftaran-Gelombang", "program-program-Program",
				"paket_registrasi_mahasiswa-paket-Paket", "jenjang-jenjang-Jenjang",
				"status_nikah-statusNikah-Status Nikah", "status_awal_mahasiswa-statusAwalMahasiswa-Kelompok",
				"jenis_sekolah_mahasiswa_baru-jenisSekolah-Jenis Pendidikan",
				"jurusan_sekolah_mahasiswa_baru-jurusanSekolah-Jurusan Pendidikan",
				"jenis_seleksi-jenisSeleksi-Jenis Seleksi", "nama_sekolah_asal-namaSekolahAsal-Nama Pendidikan",
				"tahun_kelulusan-tahunKelulusan-Tahun Lulus", "kewarganegaraan-kewarganegaraan-Warga Negara",
				"asal_negara-asalNegara-Negara", "propinsi_calon-propinsiCalon-Propinsi",
				"kota_calon-kotaCalon-Kota/Kabupaten", "kecamatan_calon_wilayah-kecamatanCalon-Kecamatan",
				"totalskor-totalSkor-Skor", "infokampusdarimana-infoKampusDariMana-Info", "agama-agama-Agama",
				"pekerjaan_orang_tua-pekerjaanAyah-Pekerjaan Ortu",
				"pendidikan_orang_tua-pendidikanOrtu-Pendidikan Ortu",
				"pendapatan_ortu-pendapatanOrtu-Pendapatan Ortu" };

		final Combobox pilihanBerdasar = new Combobox();
		pilihanBerdasar.setReadonly(true);
		for (String s : col) {
			String[] ss = s.split("-");
			MyComboitemConfig comboitem = new MyComboitemConfig(ss[2]);
			comboitem.setValue(ss[0] + "-" + ss[1]);
			pilihanBerdasar.appendChild(comboitem);
		}
		pilihanBerdasar.setSelectedIndex(0);

		final Combobox pilihanProdi = new Combobox();
		pilihanProdi.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(" Pilihan 1");
		comboitem.setValue("1");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 2");
		comboitem.setValue("2");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 3");
		comboitem.setValue("3");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 4");
		comboitem.setValue("4");
		pilihanProdi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(" Pilihan 5");
		comboitem.setValue("5");
		pilihanProdi.appendChild(comboitem);

		pilihanProdi.setSelectedIndex(0);

		final Combobox jurusan = new Combobox();
		Common.insertComboDanSemua(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.pilihJurusan(jurusan, null);
		jurusan.setReadonly(true);

		final Combobox tahunAkademik = Common.generateTahunAjaran(null);
		final Combobox searchGelombang = new Combobox();
		searchGelombang.setReadonly(true);
		tahunAkademik.setReadonly(true);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		final String formatLaporan = "pdf";
		final String file = "DataPMB";
		final Map parameters = ais.common.HashMapGenerator.getRand();

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (searchGelombang
								.getSelectedItem() == null ? null : searchGelombang.getSelectedItem().getValue());
						Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null
								|| jurusan.getSelectedItem().getValue() == null ? null
										: jurusan.getSelectedItem().getValue());
						String berdasar = pilihanBerdasar.getSelectedItem().getValue().toString().split("-")[1];

						parameters.put("tahunakademik", tahunAkademik.getSelectedItem().getValue());
						parameters.put("gelombang_pendaftaran",
								gelombangPendaftaran == null ? "" : gelombangPendaftaran.getNama());
						parameters.put("jurusan", jur == null ? "" : jur.getNama());

						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataPMB("prodi" + pilihanProdi.getSelectedItem().getValue(),

								(String) tahunAkademik.getSelectedItem().getValue(), gelombangPendaftaran, jur,
								berdasar);

						File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file,
								ais.ui.util.WaktuUtil.getDate(), maps, bar, (Locale) arg0.getData());
						// Pratinjau HTML (mirip PDF) default + toggle ke PDF.
						Report.tampil(myfile, center);
					}
				});
			}
		};

		North south = new North();
		south.setParent(borderlayout);
		Toolbar toolbar;
		south.appendChild(toolbar = new Toolbar());

		toolbar.appendChild(pilihanBerdasar);
		pilihanBerdasar.addEventListener("onChange", eventListener);

		toolbar.appendChild(pilihanProdi);
		pilihanProdi.addEventListener("onChange", eventListener);

		toolbar.appendChild(jurusan);
		jurusan.addEventListener("onChange", eventListener);

		toolbar.appendChild(tahunAkademik);

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								tahunAkademik.getSelectedItem() == null
										|| tahunAkademik.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														tahunAkademik.getSelectedItem().getValue())));
				Common.selectComboItem(searchGelombang, null);
				eventListener.onEvent(arg0);
			}
		});

		toolbar.appendChild(searchGelombang);
		searchGelombang.addEventListener("onChange", eventListener);

		MyButtonConfig toolbarbutton = new MyButtonConfig("Tampilkan");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", eventListener);

		toolbarbutton = new MyButtonConfig("Download XLS");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (searchGelombang
								.getSelectedItem() == null ? null : searchGelombang.getSelectedItem().getValue());
						Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null
								|| jurusan.getSelectedItem().getValue() == null ? null
										: jurusan.getSelectedItem().getValue());

						String berdasar = pilihanBerdasar.getSelectedItem().getValue().toString().split("-")[1];

						parameters.put("tahunakademik", tahunAkademik.getSelectedItem().getValue());
						parameters.put("gelombang_pendaftaran",
								gelombangPendaftaran == null ? "" : gelombangPendaftaran.getNama());
						parameters.put("jurusan", jur == null ? "" : jur.getNama());

						String bar = Common.getGeneratedBarCode();
						List<Map<String, Object>> maps = getDataPMB("prodi" + pilihanProdi.getSelectedItem().getValue(),

								(String) tahunAkademik.getSelectedItem().getValue(), gelombangPendaftaran, jur,
								berdasar);

						File myfile = Report.generateFileReportWithProgress(Report.XLS, parameters, file,
								ais.ui.util.WaktuUtil.getDate(), maps, bar, (Locale) arg0.getData());
						final AMedia amedia = new AMedia(file + ".xlsx", formatLaporan,
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
								new FileInputStream(myfile));
						Filedownload.save(amedia);
					}
				});

			}
		});

		Common.insertComboDanSemua(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue())));
		Common.selectComboItem(searchGelombang, null);

		return window;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getDataPMB(String pilihan, String tahunAkademik,
			GelombangPendaftaran gelombangPendaftaran, Jurusan jurusan, String berdasar) throws Exception {

		Session session = HibernateUtil.currentSession();
		List<BiodataCalonMahasiswa> listPendaftaranWisuda = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(gelombangPendaftaran == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran))

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", tahunAkademik))

				.add(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("" + pilihan, jurusan))
				.addOrder(Order.asc(berdasar)).addOrder(Order.asc("" + pilihan)).addOrder(Order.asc("noRegistrasi"))
				.list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<BiodataCalonMahasiswa> itr = listPendaftaranWisuda.iterator();

		try {
			ClassMetadata classMetadata = HibernateUtil.getClassMetadata(BiodataCalonMahasiswa.class);

			while (itr.hasNext()) {
				BiodataCalonMahasiswa biodataCalonMahasiswa = itr.next();
				Map<String, Object> map = new java.util.HashMap<String, Object>();

				Common.insertProperty(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, map, "biodataCalonMahasiswa");

				try {
					GeneralValueObject val = (GeneralValueObject) classMetadata.getPropertyValue(biodataCalonMahasiswa, berdasar, EntityMode.POJO);
					map.put("berdasar", val.getNama());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:4758");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}

				map.put("nama", biodataCalonMahasiswa.getNama() == null ? ""
						: biodataCalonMahasiswa.getNama().toUpperCase());
				map.put("no_reg", biodataCalonMahasiswa.getNoRegistrasi());
				map.put("no_ujian", biodataCalonMahasiswa.getNoUjian());
				map.put("paket",
						biodataCalonMahasiswa.getPaket() == null ? "" : biodataCalonMahasiswa.getPaket().getNama());

				map.put("pilihanke", pilihan.replaceAll("prodi", ""));

				if (pilihan.equalsIgnoreCase("prodi1")) {
					map.put("prodi", biodataCalonMahasiswa.getProdi1() == null ? ""
							: biodataCalonMahasiswa.getProdi1().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi2")) {
					map.put("prodi", biodataCalonMahasiswa.getProdi2() == null ? ""
							: biodataCalonMahasiswa.getProdi2().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi3")) {
					map.put("prodi", biodataCalonMahasiswa.getProdi3() == null ? ""
							: biodataCalonMahasiswa.getProdi3().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi4")) {
					map.put("prodi", biodataCalonMahasiswa.getProdi4() == null ? ""
							: biodataCalonMahasiswa.getProdi4().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi5")) {
					map.put("prodi", biodataCalonMahasiswa.getProdi5() == null ? ""
							: biodataCalonMahasiswa.getProdi5().getNama());
				}

				map.put("prodi1",
						biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getNama());

				map.put("prodi2",
						biodataCalonMahasiswa.getProdi2() == null ? "" : biodataCalonMahasiswa.getProdi2().getNama());

				map.put("prodi3",
						biodataCalonMahasiswa.getProdi3() == null ? "" : biodataCalonMahasiswa.getProdi3().getNama());

				map.put("prodi4",
						biodataCalonMahasiswa.getProdi4() == null ? "" : biodataCalonMahasiswa.getProdi4().getNama());

				map.put("prodi5",
						biodataCalonMahasiswa.getProdi5() == null ? "" : biodataCalonMahasiswa.getProdi5().getNama());

				String tempatLahir = biodataCalonMahasiswa.getTempatLahir() == null ? ""
						: biodataCalonMahasiswa.getTempatLahir().toUpperCase();
				String tanggalLahir = biodataCalonMahasiswa.getTanggalLahir() == null ? ""
						: Common.dateFormat2.get().format(biodataCalonMahasiswa.getTanggalLahir());
				map.put("ttl", tempatLahir + " / " + tanggalLahir);
				map.put("kelamin", biodataCalonMahasiswa.getJenisKelamin());
				String kota = biodataCalonMahasiswa.getKotaCalon() == null ? ""
						: biodataCalonMahasiswa.getKotaCalon().getNama();
				String propinsi = biodataCalonMahasiswa.getPropinsiCalon() == null ? ""
						: biodataCalonMahasiswa.getPropinsiCalon().getNama();
				String alamat = biodataCalonMahasiswa.getAlamat() == null ? ""
						: biodataCalonMahasiswa.getAlamat().toUpperCase();
				String kelurahanCalon = biodataCalonMahasiswa.getKelurahanCalon() == null ? ""
						: biodataCalonMahasiswa.getKelurahanCalon().toUpperCase();
				String kecamatanCalon = biodataCalonMahasiswa.getKecamatanCalon() == null ? ""
						: biodataCalonMahasiswa.getKecamatanCalon().getNama();
				map.put("alamat",
						alamat + ", " + kelurahanCalon + ", " + kecamatanCalon + ", " + kota.toUpperCase() + ", "
								+ propinsi.toUpperCase());

				map.put("tahunakademik", tahunAkademik);
				map.put("gelombang_pendaftaran", biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
						: biodataCalonMahasiswa.getGelombangPendaftaran().getNama());

				biodataCalonMahasiswa.putPhoto(map);

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		return maps;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getDataAlbumPMBAdmin(RuangPMB ruang, String pilihan,
			JadwalUjianPMB jadwalUjianPMB, String tahunAkademik, GelombangPendaftaran gelombangPendaftaran,
			Jurusan jurusan, Boolean gabungSemua) throws Exception {
		String waktu = jadwalUjianPMB == null ? ""
				: (Common.dateFormat51.get().format(jadwalUjianPMB.getWaktuMulai()) + " s.d "
						+ Common.timeFormat.get().format(jadwalUjianPMB.getWaktuSampai()));
		String tanggalUjian = jadwalUjianPMB == null ? ""
				: Common.dateFormat2.get().format(jadwalUjianPMB.getWaktuMulai());

		Session session = HibernateUtil.currentSession();
		List<RuangPaketPMB> listPendaftaranWisuda;
		if (ruang != null) {
			listPendaftaranWisuda = session.createCriteria(RuangPaketPMB.class)
					.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
					.add(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""))
					.add(Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
					.add(gelombangPendaftaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("biodataCalonMahasiswa.gelombangPendaftaran", gelombangPendaftaran))
					.add(jurusan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("biodataCalonMahasiswa." + pilihan, jurusan))
					.addOrder(Order.asc("biodataCalonMahasiswa." + pilihan))
					.addOrder(Order.asc("biodataCalonMahasiswa.noUjian")).add(Restrictions.eq("ruangPMB", ruang))
					.list();
		} else {
			listPendaftaranWisuda = session.createCriteria(RuangPaketPMB.class).createAlias("ruangPMB", "ruangPMB")
					.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
					.add(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""))
					.add(Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
					.add(gelombangPendaftaran == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("biodataCalonMahasiswa.gelombangPendaftaran", gelombangPendaftaran))
					.add(gabungSemua || jadwalUjianPMB == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("ruangPMB.ujianPMB", jadwalUjianPMB.getUjianPMB()))
					.add(jurusan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("biodataCalonMahasiswa." + pilihan, jurusan))
					.addOrder(Order.asc("biodataCalonMahasiswa." + pilihan))
					.addOrder(Order.asc("biodataCalonMahasiswa.noUjian")).list();

		}

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				RuangPaketPMB beanPendaftaranWisuda = (RuangPaketPMB) itr.next();
				// FIX NullPointerException: baris RuangPaketPMB bisa saja relasi biodataCalonMahasiswa-nya
				// sudah terhapus/null (mis. calon mahasiswa dibatalkan setelah ruang diisi), atau nama
				// belum diisi -- lewati baris tsb daripada melempar NPE yang menggagalkan seluruh render
				// album (dipanggil berulang oleh CommonTimerHelper via onEvent, bukan per-baris).
				if (beanPendaftaranWisuda.getBiodataCalonMahasiswa() == null) {
					continue;
				}
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("waktu", waktu);
				map.put("tanggalUjian", tanggalUjian);
				map.put("nama", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getNama() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getNama().toUpperCase());
				map.put("no_ujian", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getNoUjian());
				map.put("paket",
						beanPendaftaranWisuda.getRuangPMB() == null
								|| beanPendaftaranWisuda.getRuangPMB().getPaket() == null ? ""
										: beanPendaftaranWisuda.getRuangPMB().getPaket().getNama());
				if (gabungSemua) {
					map.put("jadwal_ujian_pmb", jadwalUjianPMB.getNama());
				} else {
					map.put("jadwal_ujian_pmb",
							(beanPendaftaranWisuda.getRuangPMB() == null
									|| beanPendaftaranWisuda.getRuangPMB().getUjianPMB() == null ? ""
											: beanPendaftaranWisuda.getRuangPMB().getUjianPMB().getNama())
									+ " / " + (jadwalUjianPMB == null ? "" : jadwalUjianPMB.getNama()));
				}
				map.put("pilihanke", pilihan.replaceAll("prodi", ""));
				map.put("ruang", beanPendaftaranWisuda.getRuangPMB() == null ? ""
						: beanPendaftaranWisuda.getRuangPMB().getNama());
				map.put("gedung",
						beanPendaftaranWisuda.getRuangPMB() == null
								|| beanPendaftaranWisuda.getRuangPMB().getGedung() == null ? ""
										: beanPendaftaranWisuda.getRuangPMB().getGedung().getNama());

				if (pilihan.equalsIgnoreCase("prodi1")) {
					map.put("prodi", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi1().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi2")) {
					map.put("prodi", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi2().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi3")) {
					map.put("prodi", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi3().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi4")) {
					map.put("prodi", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi4().getNama());
				} else if (pilihan.equalsIgnoreCase("prodi5")) {
					map.put("prodi", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi5().getNama());
				}

				map.put("prodi1", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi1() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi1().getNama());

				map.put("prodi2", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi2() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi2().getNama());

				map.put("prodi3", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi3() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi3().getNama());

				map.put("prodi4", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi4() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi4().getNama());

				map.put("prodi5", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi5() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getProdi5().getNama());

				map.put("ttl",
						beanPendaftaranWisuda.getBiodataCalonMahasiswa().getTempatLahir().toUpperCase() + " / "
								+ Common.dateFormat2.get()
										.format(beanPendaftaranWisuda.getBiodataCalonMahasiswa().getTanggalLahir()));
				map.put("kelamin", beanPendaftaranWisuda.getBiodataCalonMahasiswa().getJenisKelamin());
				String kota = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKotaCalon() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKotaCalon().getNama();
				String propinsi = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getPropinsiCalon() == null ? ""
						: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getPropinsiCalon().getNama();
					String alamat = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getAlamat() == null ? ""
							: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getAlamat();
					String kelurahan = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKelurahanCalon() == null ? ""
							: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKelurahanCalon();
					String kecamatan = beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKecamatanCalon() == null ? ""
							: beanPendaftaranWisuda.getBiodataCalonMahasiswa().getKecamatanCalon().getNama();
					map.put("alamat", alamat.toUpperCase() + ", " + kelurahan.toUpperCase()
							+ ", " + kecamatan + ", " + kota.toUpperCase() + ", " + propinsi.toUpperCase());

				map.put("tahunakademik", tahunAkademik);
				map.put("gelombang_pendaftaran",
						beanPendaftaranWisuda.getRuangPMB() == null
								|| beanPendaftaranWisuda.getRuangPMB().getUjianPMB() == null
								|| beanPendaftaranWisuda.getRuangPMB().getUjianPMB().getGelombangPendaftaran() == null
										? ""
										: beanPendaftaranWisuda.getRuangPMB().getUjianPMB().getGelombangPendaftaran()
												.getNama());

				beanPendaftaranWisuda.getBiodataCalonMahasiswa().putPhoto(map);

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		return maps;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakAbsensiPMB(RuangPMB ruangPMB) throws Exception {

		if (ruangPMB.getUjianPMB() == null) {
			return;
		}

		List<Map> maps = new ArrayList<Map>();
		List<String> names = new ArrayList<String>();
		List<String> fileNames = new ArrayList<String>();

		for (int i = 1; i <= ruangPMB.getUjianPMB().getJumlahHariUjian(); i++) {
			final Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("ruang", ruangPMB.getId());
			parameters.put("tanggalKe", i);
			maps.add(parameters);
			names.add("Hari ke-" + i);
			fileNames.add("AbsensiPMB_day1");
		}

		Report.generatePDFReport(Report.PDF, maps.toArray(new Map[] {}), fileNames.toArray(new String[] {}),
				names.toArray(new String[] {}), ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakVerifikasiPMB(RuangPMB ruangPMB) throws Exception {

		if (ruangPMB.getUjianPMB() == null) {
			return;
		}

		List<Map> maps = new ArrayList<Map>();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("ruang", ruangPMB.getId());
		maps.add(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "ValidasiPMB", ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({})
	public static void onLaporanAbsensi(VOPembelajaran voPembelajaran, Boolean tampiNilai) throws Exception {
		onLaporanAbsensi(voPembelajaran, tampiNilai, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map parameterKehadiran(final VOPembelajaran voPembelajaran, final Boolean tampiNilai,
			final Integer pilih) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();
		Dosen kaprodi = null;
		Dosen dekan = null;
		if (voPembelajaran instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
			kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
					: perkuliahan.getJurusan().getKaprodi();
			dekan = perkuliahan == null || perkuliahan.getJurusan() == null
					|| perkuliahan.getJurusan().getFakultas() == null ? null
							: perkuliahan.getJurusan().getFakultas().getDekan();

			parameters.put("fakultas_id",
					perkuliahan.getJurusan() == null ? -1L : perkuliahan.getJurusan().getFakultas().getId());
			parameters.put("jurusan_id", perkuliahan.getJurusan() == null ? -1L : perkuliahan.getJurusan().getId());

			Common.insertProperty(Fakultas.class, perkuliahan.getJurusan().getFakultas(), parameters, "fakultas", 1);

			if (perkuliahan != null) {
				if (perkuliahan.getJurusan() != null) {
					Common.insertProperty(Jurusan.class, perkuliahan.getJurusan(), parameters, "jur");
				}
				if (perkuliahan.getJurusan().getFakultas() != null) {
					Common.insertProperty(Fakultas.class, perkuliahan.getJurusan().getFakultas(), parameters, "fak");
				}
				if (perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					Common.insertProperty(PerguruanTinggi.class,
							perkuliahan.getJurusan().getFakultas().getPerguruanTinggi(), parameters, "pt");
				}
			}
		}

		parameters.put("perkuliahan", voPembelajaran.getId());
		parameters.put("tampil_nilai", tampiNilai ? "1" : "0");
		parameters.put("kaprodi", kaprodi == null ? "(                                          )" : kaprodi.getNama());
		parameters.put("nip", kaprodi == null ? "" : kaprodi.getCode());
		parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

		parameters.put("nama_kaprodi",
				kaprodi == null ? "(                                          )" : kaprodi.getNama());
		parameters.put("nip_kaprodi", kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());

		parameters.put("nidn_kaprodi", kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = null;
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:5060");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					parameters.put("ttd_kaprodi", ttd);
				}
			}
		}

		parameters.put("dekan", dekan == null ? "(                                          )" : dekan.getNama());

		parameters.put("nama_dekan", dekan == null ? "(                                          )" : dekan.getNama());
		parameters.put("nip_dekan", dekan == null || dekan.getCode() == null ? "" : dekan.getCode().trim());

		parameters.put("nidn_dekan", dekan == null || dekan.getNidn() == null ? "" : dekan.getNidn());

		if (dekan != null) {
			LampiranLain lam = LampiranLain.ambil(dekan.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					String ttd = null;
					try {
						ttd = lam.ambilFile().getAbsolutePath();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:5087");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					parameters.put("ttd_dekan", ttd);
				}
			}
		}

		List<Map<String, Serializable>> maps = generateParameterMapAbsensi(voPembelajaran);
		parameters.put("maps", maps);

		String ttd = null;
		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					ttd = lam.ambilFile().getAbsolutePath();

					parameters.put("ttd_kaprodi", ttd);
				}
			}
		}
		System.out.println("ttd_kaprodi => " + ttd);

		tambahStempelKaprodi(parameters, voPembelajaran);

		// Tambah data semua dosen ke top-level parameters agar semua tab JRXML bisa pakai
		if (voPembelajaran instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
			tambahTtdDosenPerkuliahan(parameters, perkuliahan);
		}

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void tambahTtdDosenPerkuliahan(Map parameters, Perkuliahan perkuliahan) {
		if (perkuliahan == null) {
			return;
		}
		Dosen[] dosens = new Dosen[] { perkuliahan.getDosen1(), perkuliahan.getDosen2(), perkuliahan.getDosen3(),
				perkuliahan.getDosen4(), perkuliahan.getDosen5(), perkuliahan.getDosen6(), perkuliahan.getDosen7(),
				perkuliahan.getDosen8(), perkuliahan.getDosen9(), perkuliahan.getDosen10() };
		for (int i = 0; i < dosens.length; i++) {
			tambahTtdDosen(parameters, dosens[i], i + 1);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void tambahTtdDosen(Map parameters, Dosen dosen, int index) {
		if (dosen == null) return;
		parameters.put("nama_dosen" + index, dosen.getNama() != null ? dosen.getNama() : "");
		parameters.put("nip_dosen" + index, dosen.getMycode() != null ? dosen.getMycode() : "");
		parameters.put("nip1_dosen" + index, dosen.getCode() != null ? dosen.getCode() : "");
		parameters.put("nidn_dosen" + index, dosen.getNidn() != null ? dosen.getNidn() : "");
		LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
		String namaFile = lam == null ? null : lam.getNama();
		if (namaFile == null) return;
		String lower = namaFile.toLowerCase();
		if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")
				|| lower.endsWith(".gif") || lower.endsWith(".tif") || lower.endsWith(".bmp")) {
			try {
				String path = lam.ambilFile().getAbsolutePath();
				parameters.put("ttd_dosen" + index, path);    // untuk Lanscape1, Lanscape, Portrait, Rinci
				parameters.put("ttd_dosen_" + index, path);   // untuk subreport Rekap Masuk
				parameters.put("ttd_dosen_id_" + dosen.getId(), path);
			} catch (Exception e) {
				e.printStackTrace();
				ais.common.ErrorAuditUtil.record(e, "tambahTtdDosen CommonReportHelper");
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void tambahStempelKaprodi(Map parameters, VOPembelajaran voPembelajaran) {
		if (!(voPembelajaran instanceof Perkuliahan)) {
			return;
		}
		Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
		Jurusan jurusan = perkuliahan.getJurusan();
		if (jurusan != null && jurusan.getId() != null) {
			tambahLampiranGambar(parameters, "stempel_jurusan", jurusan.getId(), LampiranLain.STEMPEL_JURUSAN);
			if (parameters.get("stempel_jurusan") != null) {
				parameters.put("cap_kaprodi", parameters.get("stempel_jurusan"));
			}
		}
		if (jurusan != null && jurusan.getFakultas() != null && jurusan.getFakultas().getPerguruanTinggi() != null
				&& jurusan.getFakultas().getPerguruanTinggi().getId() != null) {
			tambahLampiranGambar(parameters, "stempel_pt", jurusan.getFakultas().getPerguruanTinggi().getId(),
					LampiranLain.STEMPEL_PT);
			if (parameters.get("cap_kaprodi") == null && parameters.get("stempel_pt") != null) {
				parameters.put("cap_kaprodi", parameters.get("stempel_pt"));
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void tambahLampiranGambar(Map parameters, String key, Long ownerId, String jenis) {
		if (ownerId == null || jenis == null) {
			return;
		}
		LampiranLain lam = LampiranLain.ambil(ownerId, jenis);
		String namaFile = lam == null ? null : lam.getNama();
		if (!isFileGambar(namaFile)) {
			return;
		}
		try {
			File file = lam.ambilFile();
			if (file != null) {
				parameters.put(key, file.getAbsolutePath());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "tambahLampiranGambar CommonReportHelper " + key);
		}
	}

	private static boolean isFileGambar(String namaFile) {
		if (namaFile == null) {
			return false;
		}
		String lower = namaFile.toLowerCase();
		return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")
				|| lower.endsWith(".gif") || lower.endsWith(".tif") || lower.endsWith(".bmp");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanAbsensi(final VOPembelajaran voPembelajaran, final Boolean tampiNilai,
			final Integer pilih) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Map parameters = CommonReportHelper.parameterKehadiran(voPembelajaran, tampiNilai, pilih);

				if (voPembelajaran instanceof Perkuliahan) {
					Map parametersCover = ais.common.HashMapGenerator.getRand();
					parametersCover.put("perkuliahan", voPembelajaran == null || voPembelajaran.getId() == null ? -1 : voPembelajaran.getId());
					Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF,
							new Map[] { parameters, parameters, parameters, parametersCover, parameters },
							new String[] { "LaporanAbsensiLanscape", "LaporanAbsensiLanscape1", "LaporanAbsensi",
									"LaporanCoverAbsensi", "LaporanAbsensiLanscapeTotal" },
							new String[] { "Lanscape", "Lanscape Tgl", "Portrait", "Cover", "Rekap Masuk" },
							ais.ui.util.WaktuUtil.getDate());

					Tabpanels tabpanels = tabbox.getTabpanels();
					Tabs tabs = tabbox.getTabs();

					final Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
					final MyTabConfig tabRinci = new MyTabConfig("Rekap Rinci Peserta");
					tabRinci.setParent(tabs);

					final Tabpanel tabpanelRinci = new ais.ui.util.MyTabpanel();

					EventListener tabpanelRinciPesertaEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelRinci.getChildren().size() == 0) {
								Map parametersBaru = new HashMap(parameters);
								List<Map<String, Serializable>> maps = generateParameterMapAbsensiRinci(perkuliahan,
										true, false);
								parametersBaru.put("maps", maps);
								Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
										ais.ui.util.WaktuUtil.getDate(), Common.locale, null, tabpanelRinci);

							}
						}
					};

					tabpanelRinci.setParent(tabpanels);
					tabpanelRinci.setHeight("650px");
					tabRinci.addEventListener("onClick", tabpanelRinciPesertaEventListener);

					if (pilih != null && pilih.equals(6)) {
						tabpanelRinciPesertaEventListener.onEvent(null);
						tabRinci.setSelected(true);
					}

					final MyTabConfig tabRinciDosen = new MyTabConfig("Rekap Rinci Pengajar");
					tabRinciDosen.setParent(tabs);

					final Tabpanel tabpanelRinciDosen = new ais.ui.util.MyTabpanel();

					EventListener tabpanelRinciDosenEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelRinciDosen.getChildren().size() == 0) {
								Map parametersBaru = new HashMap(parameters);
								List<Map<String, Serializable>> maps = generateParameterMapAbsensiRinci(perkuliahan,
										false, true);
								parametersBaru.put("maps", maps);
								Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
										ais.ui.util.WaktuUtil.getDate(), Common.locale, null, tabpanelRinciDosen);

							}
						}
					};

					tabpanelRinciDosen.setParent(tabpanels);
					tabpanelRinciDosen.setHeight("650px");
					tabRinciDosen.addEventListener("onClick", tabpanelRinciDosenEventListener);

					if (pilih != null && pilih.equals(7)) {
						tabpanelRinciDosenEventListener.onEvent(null);
						tabRinciDosen.setSelected(true);
					}

					final MyTabConfig tabPenilaian = new MyTabConfig("Rekap Peserta");
					tabPenilaian.setParent(tabs);

					final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();

					EventListener tabpanelRekapMahasiswaEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelPenilaian.getChildren().size() == 0) {
								DashboardRekapAbsensiPerMahasiswa dashboardRekapAbsensiMahasiswa = new DashboardRekapAbsensiPerMahasiswa(
										perkuliahan);
								tabpanelPenilaian.appendChild(dashboardRekapAbsensiMahasiswa);
							}
						}
					};

					tabpanelPenilaian.setParent(tabpanels);
					tabpanelPenilaian.setHeight("650px");
					tabPenilaian.addEventListener("onClick", tabpanelRekapMahasiswaEventListener);

					if (pilih != null && pilih.equals(8)) {
						tabpanelRekapMahasiswaEventListener.onEvent(null);
						tabPenilaian.setSelected(true);
					}

					final MyTabConfig tabPenilaianDosen = new MyTabConfig("Rekap Pengajar");
					tabPenilaianDosen.setParent(tabs);

					final Tabpanel tabpanelPenilaianDosen = new ais.ui.util.MyTabpanel();

					EventListener tabpanelRekapDosenEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelPenilaianDosen.getChildren().size() == 0) {
								DashboardRekapAbsensiDosen dashboardRekapAbsensiDosen = new DashboardRekapAbsensiDosen(
										perkuliahan);
								tabpanelPenilaianDosen.appendChild(dashboardRekapAbsensiDosen);
							}
						}
					};

					tabpanelPenilaianDosen.setParent(tabpanels);
					tabpanelPenilaianDosen.setHeight("650px");
					tabPenilaianDosen.addEventListener("onClick", tabpanelRekapDosenEventListener);

					if (pilih != null && pilih.equals(9)) {
						tabpanelRekapDosenEventListener.onEvent(null);
						tabPenilaianDosen.setSelected(true);
					}

					final MyTabConfig tabPenilaianAsistenDosen = new MyTabConfig("Rekap Asisten Pengajar");
					tabPenilaianAsistenDosen.setParent(tabs);

					final Tabpanel tabpanelPenilaianAsistenDosen = new ais.ui.util.MyTabpanel();

					tabpanelPenilaianAsistenDosen.setParent(tabpanels);
					tabpanelPenilaianAsistenDosen.setHeight("650px");
					tabPenilaianAsistenDosen.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelPenilaianAsistenDosen.getChildren().size() == 0) {
								DashboardRekapAbsensiAsistenDosen dashboardRekapAbsensiDosen = new DashboardRekapAbsensiAsistenDosen(
										perkuliahan);
								tabpanelPenilaianAsistenDosen.appendChild(dashboardRekapAbsensiDosen);
							}
						}
					});
				} else {
					if (voPembelajaran instanceof KelompokKkn) {

						parameters.put("id_kkn", voPembelajaran.getId());

						Report.generatePDFReport(Report.PDF, parameters, "absensi_kelompok_kkn",
								ais.ui.util.WaktuUtil.getDate());
					} else if (voPembelajaran instanceof KelompokPkl) {

						parameters.put("id_pkl", voPembelajaran.getId());

						Report.generatePDFReport(Report.PDF, parameters, "absensi_kelompok_pkl",
								ais.ui.util.WaktuUtil.getDate());
					} else {
						parameters.put("id", voPembelajaran.getId());

						Report.generatePDFReport(Report.PDF, parameters, "absensi_lain",
								ais.ui.util.WaktuUtil.getDate());
					}
				}
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanAbsensi(final JadwalPelajaran jadwalPelajaran, final Boolean tampiNilai,
			final List<Long> statusPertemuans) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("jadwalPelajaran", jadwalPelajaran.getId());
				parameters.put("tampil_nilai", tampiNilai ? "1" : "0");

				parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

				parameters.put("nama_kaprodi",
						jadwalPelajaran.getSekolah() == null ? ""
								: jadwalPelajaran.getSekolah().getNamaKepalaSekolah() == null ? ""
										: jadwalPelajaran.getSekolah().getNamaKepalaSekolah());
				parameters.put("nip_kaprodi",
						jadwalPelajaran.getSekolah() == null ? ""
								: jadwalPelajaran.getSekolah().getNipKepalaSekolah() == null ? ""
										: jadwalPelajaran.getSekolah().getNipKepalaSekolah());

				List<Map<String, Serializable>> maps = generateParameterMapAbsensi(jadwalPelajaran);
				parameters.put("maps", maps);

				Map parametersCover = ais.common.HashMapGenerator.getRand();
				parametersCover.put("jadwalPelajaran", jadwalPelajaran == null || jadwalPelajaran.getId() == null ? -1 : jadwalPelajaran.getId());

				Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF,
						new Map[] { parameters, parameters, parameters, parametersCover, parameters },
						new String[] { "LaporanAbsensiLanscape", "LaporanAbsensiLanscape1", "LaporanAbsensi",
								"LaporanCoverAbsensi", "LaporanAbsensiLanscapeTotal" },
						new String[] { "Lanscape", "Lanscape Tgl", "Portrait", "Cover", "Rekap Masuk" },
						ais.ui.util.WaktuUtil.getDate());

				Tabpanels tabpanels = tabbox.getTabpanels();
				Tabs tabs = tabbox.getTabs();

				final MyTabConfig tabRinci = new MyTabConfig("Rekap Rinci Siswa");
				tabRinci.setParent(tabs);

				final Tabpanel tabpanelRinci = new ais.ui.util.MyTabpanel();

				tabpanelRinci.setParent(tabpanels);
				tabpanelRinci.setHeight("650px");
				tabRinci.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelRinci.getChildren().size() == 0) {
							Map parametersBaru = new HashMap(parameters);
							List<Map<String, Serializable>> maps = generateParameterMapAbsensiRinci(jadwalPelajaran,
									true, false, statusPertemuans);
							parametersBaru.put("maps", maps);
							Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
									ais.ui.util.WaktuUtil.getDate(), Common.locale, null, tabpanelRinci);

						}
					}
				});

				final MyTabConfig tabRinciGuru = new MyTabConfig("Rekap Rinci Guru");
				tabRinciGuru.setParent(tabs);

				final Tabpanel tabpanelRinciGuru = new ais.ui.util.MyTabpanel();

				tabpanelRinciGuru.setParent(tabpanels);
				tabpanelRinciGuru.setHeight("650px");
				tabRinciGuru.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelRinciGuru.getChildren().size() == 0) {
							Map parametersBaru = new HashMap(parameters);
							List<Map<String, Serializable>> maps = generateParameterMapAbsensiRinci(jadwalPelajaran,
									false, true, statusPertemuans);
							parametersBaru.put("maps", maps);
							Report.generatePDFReport("pdf", parametersBaru, "LaporanAbsensiRinci",
									ais.ui.util.WaktuUtil.getDate(), Common.locale, null, tabpanelRinciGuru);

						}
					}
				});

				final MyTabConfig tabPenilaian = new MyTabConfig("Rekap Siswa");
				tabPenilaian.setParent(tabs);

				final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();

				tabpanelPenilaian.setParent(tabpanels);
				tabpanelPenilaian.setHeight("650px");
				tabPenilaian.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelPenilaian.getChildren().size() == 0) {
							DashboardRekapAbsensiPerSiswa dashboardRekapAbsensiSiswa = new DashboardRekapAbsensiPerSiswa(
									jadwalPelajaran);
							tabpanelPenilaian.appendChild(dashboardRekapAbsensiSiswa);
						}
					}
				});

				final MyTabConfig tabPenilaianGuru = new MyTabConfig("Rekap Guru");
				tabPenilaianGuru.setParent(tabs);

				final Tabpanel tabpanelPenilaianGuru = new ais.ui.util.MyTabpanel();

				tabpanelPenilaianGuru.setParent(tabpanels);
				tabpanelPenilaianGuru.setHeight("650px");
				tabPenilaianGuru.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelPenilaianGuru.getChildren().size() == 0) {
							DashboardRekapAbsensiGuru dashboardRekapAbsensiGuru = new DashboardRekapAbsensiGuru(
									jadwalPelajaran);
							tabpanelPenilaianGuru.appendChild(dashboardRekapAbsensiGuru);
						}
					}
				});

			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanAbsensi(final KelasSiswa kelasSiswa, final Boolean tampiNilai) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("kelasSiswa", kelasSiswa.getId());

				System.out.println("parameters => " + parameters);

				System.out.println("parameters " + parameters);

				Map parametersCover = ais.common.HashMapGenerator.getRand();
				parametersCover.put("kelasSiswa", kelasSiswa == null || kelasSiswa.getId() == null ? -1 : kelasSiswa.getId());

				@SuppressWarnings("unused")
				Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF,
						new Map[] { parameters, parameters, parameters, parametersCover, parameters },
						new String[] { "LaporanAbsensiLanscape", "LaporanAbsensiLanscape1", "LaporanAbsensi",
								"LaporanCoverAbsensi", "LaporanAbsensiLanscapeTotal" },
						new String[] { "Lanscape", "Lanscape Tgl", "Portrait", "Cover", "Rekap Masuk" },
						ais.ui.util.WaktuUtil.getDate());

			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanTagihan(final Mahasiswa mhs, final TreeMap<String, Object[]> semua) throws Exception {

		// HAPUS Common.createDefaultTimer(...)
		// Jalankan secara langsung secara sinkron agar File Download tidak diblokir
		// ZK/Browser

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("nama_mahasiswa", mhs.getNama());
		parameters.put("no_registrasi", mhs.getNim());

		parameters.put("fakultas", mhs.getJurusan().getFakultas().getNama());
		parameters.put("jurusan", mhs.getJurusan().getNama());
		parameters.put("program", mhs.getProgram());
		parameters.put("tahunangkatan", mhs.getTahunangkatan());

		Common.insertProperty(Mahasiswa.class, mhs, parameters, "mhs");
		BiodataMahasiswa biodataMahasiswa = mhs.ambilBiodata();
		if (biodataMahasiswa != null) {
			Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, parameters, "bio");
		}
		BiodataCalonMahasiswa cln = mhs.getBiodataCalonMahasiswaData();
		if (cln != null) {
			Common.insertProperty(BiodataCalonMahasiswa.class, cln, parameters, "cln");
		}

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		for (String key : semua.keySet()) {
			Integer smt = Integer.parseInt(key.split("-")[1]);
			Object[] val = semua.get(key);
			Collection detailBiayas = (Collection) val[0];
			Kegiatan kegiatan = (Kegiatan) val[2];

			if (kegiatan != null && kegiatan.getAktif()) {
				if (tambahSnapshotTagihan(val, maps)) {
					continue;
				}

				Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
						: kegiatan.ambilDetailKegiatan(false);

				List<CicilanPembayaran> cicilanPembayarans = kegiatan == null || kegiatan.getId() == null
						? new ArrayList<CicilanPembayaran>()
						: kegiatan.ambilCicilan();

				for (Object o : detailBiayas) {

					DetailBiaya tempdetailBiaya = null;
					PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;
					if (o instanceof DetailBiaya) {
						tempdetailBiaya = (DetailBiaya) o;

					} else if (o instanceof PengaturanPembayaranBulanan) {
						temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						if (temppengaturanPembayaranBulanan != null) {
							tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
						}

					}

					DetailKegiatan tempdata = kegiatan == null ? null
							: temppengaturanPembayaranBulanan != null
									? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan, detailKegiatans)
									: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);

					DetailKegiatan detailKegiatan = tempdata;
					if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
						continue;
					}

					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

						Double jumlah = (mhs instanceof Mahasiswa)
								? Kegiatan.ambilJumlahTagihan(detailKegiatan,
										pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, (Mahasiswa) mhs, smt,
										pengaturanPembayaranBulanan)
								: Kegiatan.ambilJumlahTagihan(detailKegiatan,
										pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, smt,
										pengaturanPembayaranBulanan);

						Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
								cicilanPembayarans);

						if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
								&& telahDibayar.intValue() > 0) {
							jumlah = telahDibayar;
						}

						if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
								.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
							jumlah = -Math.abs(jumlah);
							telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
						}

						int tot = (int) (jumlah.intValue() + (telahDibayar == null ? 0.0 : telahDibayar.doubleValue()));
						if (tot == 0) {
							continue;
						}

						Double belumDibayar = jumlah - (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

						map.put("kode", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode());
						map.put("item_biaya", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
								+ ", Bulan " + pengaturanPembayaranBulanan.getNamaBulan());
						map.put("biaya", jumlah);
						map.put("dibayar", telahDibayar);
						map.put("sisa", belumDibayar);
						map.put("nama_kegiatan_semester",
								pengaturanPembayaranBulanan.getDetailBiaya().getJenisKegiatan().getNamaKegiatan() + "-"
										+ smt);
						map.put("semester", smt);

						if (mhs != null) {
							Mahasiswa mahasiswa = mhs;
							Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
							Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
							Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs, semesterMulai,
									mahasiswa.getSemesterMulai());

							String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
							map.put("tahun_ajaran", tahunAkademik);
						} else {
							map.put("tahun_ajaran", detailBiaya.getTahunAkademik());
						}

						map.put("nama_kegiatan",
								pengaturanPembayaranBulanan.getDetailBiaya().getJenisKegiatan().getNamaKegiatan());
					} else {
						DetailBiaya detailBiaya = (DetailBiaya) o;

						Double jumlah = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, false);

						Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya, cicilanPembayarans);

						if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
								&& telahDibayar.intValue() > 0) {
							jumlah = telahDibayar;
						}

						if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
							jumlah = -Math.abs(jumlah);
							telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
						}

						int tot = (int) (jumlah.intValue() + (telahDibayar == null ? 0.0 : telahDibayar.doubleValue()));
						if (tot == 0) {
							continue;
						}

						Double belumDibayar = jumlah - (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

						map.put("kode", detailBiaya.getItemBiaya().getKode());
						map.put("item_biaya",
								detailBiaya.getItemBiaya().getNama()
										+ (detailBiaya.getDetailSettingBiaya() != null
												&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
												&& detailBiaya.getDetailSettingBiaya().getSettingBiaya()
														.getJumlahPembayaran() > 1 ? ", ke-" + detailBiaya.getBayarKe()
																: ""));

						map.put("biaya", jumlah);
						map.put("dibayar", telahDibayar);
						map.put("sisa", belumDibayar);
						map.put("nama_kegiatan_semester", detailBiaya.getJenisKegiatan().getNamaKegiatan() + "-" + smt);
						map.put("semester", smt);
						if (mhs != null) {
							Mahasiswa mahasiswa = mhs;
							Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
							Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
							Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs, semesterMulai,
									mahasiswa.getSemesterMulai());

							String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
							map.put("tahun_ajaran", tahunAkademik);
						} else {
							map.put("tahun_ajaran", detailBiaya.getTahunAkademik());
						}
						map.put("nama_kegiatan", detailBiaya.getJenisKegiatan().getNamaKegiatan());
					}
					maps.add(map);
				}
			}
		}

		parameters.put("maps", maps);
		Report.generatePDFReport("pdf", parameters, "tagihan", ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean tambahSnapshotTagihan(Object[] val, List<Map<String, Serializable>> tujuan) {
		if (val == null || val.length < 4 || !(val[3] instanceof Collection)) {
			return false;
		}
		Kegiatan kegiatan = val[2] instanceof Kegiatan ? (Kegiatan) val[2] : null;
		tujuan.addAll(selaraskanSnapshotDenganRingkasan(kegiatan, (Collection) val[3]));
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanBebasTunggakan(final Mahasiswa mhs, final TreeMap<String, Object[]> semua)
			throws Exception {

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("nama_mahasiswa", mhs.getNama());
		parameters.put("no_registrasi", mhs.getNim());

		parameters.put("fakultas", mhs.getJurusan().getFakultas().getNama());
		parameters.put("jurusan", mhs.getJurusan().getNama());
		parameters.put("program", mhs.getProgram());
		parameters.put("tahunangkatan", mhs.getTahunangkatan());

		System.out.println("parameters " + parameters);
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		for (String key : semua.keySet()) {
			Integer smt = Integer.parseInt(key.split("-")[1]);
			Object[] val = semua.get(key);
			Collection detailBiayas = (Collection) val[0];
			Kegiatan kegiatan = (Kegiatan) val[2];
			if (kegiatan != null && kegiatan.getAktif()) {
				System.out.println("kegiatan -> " + kegiatan);
				System.out.println("detailBiayas -> " + detailBiayas);

//					if (kegiatan != null) {
				Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
						: kegiatan.ambilDetailKegiatan(false);

				List<CicilanPembayaran> cicilanPembayarans = kegiatan == null || kegiatan.getId() == null
						? new ArrayList<CicilanPembayaran>()
						: kegiatan.ambilCicilan();

				for (Object o : detailBiayas) {

					DetailBiaya tempdetailBiaya = null;
					PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;
					if (o instanceof DetailBiaya) {
						tempdetailBiaya = (DetailBiaya) o;

					} else if (o instanceof PengaturanPembayaranBulanan) {
						temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						if (temppengaturanPembayaranBulanan != null) {
							tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
						}

					}

					DetailKegiatan tempdata = kegiatan == null ? null
							: temppengaturanPembayaranBulanan != null
									? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan, detailKegiatans)
									: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);

					DetailKegiatan detailKegiatan = tempdata;
					if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
						continue;
					}

					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();

						Double jumlah = (mhs instanceof Mahasiswa)
								? Kegiatan.ambilJumlahTagihan(detailKegiatan,
										pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, (Mahasiswa) mhs, smt,
										pengaturanPembayaranBulanan)
								: Kegiatan.ambilJumlahTagihan(detailKegiatan,
										pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, smt,
										pengaturanPembayaranBulanan);

						Date tanggalBayar = WaktuUtil.getDate();
						for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
							try {
								if (cicilanPembayaran.getItemBiaya() != null
										&& cicilanPembayaran.getItemBiaya().getId().equals(
												pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
										&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
												.equals(cicilanPembayaran.getBayarKe())
										&& cicilanPembayaran.getKegiatan().getId().equals(kegiatan.getId())) {

									if (pengaturanPembayaranBulanan != null
											&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
										PengaturanPembayaranBulanan p = cicilanPembayaran
												.getPengaturanPembayaranBulanan();
										if (p.getDetailBiaya().getItemBiaya().getId().equals(
												pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
												&& p.getRealBulan()
														.equals(pengaturanPembayaranBulanan.getRealBulan())) {
											tanggalBayar = cicilanPembayaran.getTanggal();
										}
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:5737");
								// TODO: handle exception
							}
						}

						Double hasilDenda = detailKegiatan != null
								&& (detailKegiatan.getBatalkanDenda() || jumlah.intValue() == 0)
										? jumlah
										: detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom() ? jumlah
												: pengaturanPembayaranBulanan.checkDenda(jumlah, tanggalBayar,
														kegiatan.getJadwalPembayaran(), kegiatan.getJenisKegiatan());

						if (detailKegiatan != null && detailKegiatan.getMenggunakanDendaCustom()) {
							pengaturanPembayaranBulanan.setInfoDenda(" Penambahan denda senilai "
									+ Common.numberFormat.get().format(detailKegiatan.getDendaCustom()) + ".");
						}

						Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
								cicilanPembayarans);

						if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
								&& telahDibayar.intValue() > 0) {
							jumlah = telahDibayar;
						}

						if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
							jumlah = -Math.abs(jumlah);
							telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
						}

						jumlah = hasilDenda.intValue() > jumlah.intValue() ? hasilDenda : jumlah;

						int tot = (int) (jumlah.intValue() + (telahDibayar == null ? 0.0 : telahDibayar.doubleValue()));
						if (tot == 0) {
							continue;
						}

						Double belumDibayar = jumlah - (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

						map.put("kode", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode());
						map.put("item_biaya", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
								+ ", Bulan " + pengaturanPembayaranBulanan.getNamaBulan());
						map.put("biaya", jumlah);
						map.put("dibayar", telahDibayar);
						map.put("sisa", belumDibayar);
						map.put("nama_kegiatan_semester",
								pengaturanPembayaranBulanan.getDetailBiaya().getJenisKegiatan().getNamaKegiatan() + "-"
										+ smt);
						map.put("semester", smt);
						map.put("tahun_ajaran", pengaturanPembayaranBulanan.getDetailBiaya().getTahunAkademik());
						map.put("nama_kegiatan",
								pengaturanPembayaranBulanan.getDetailBiaya().getJenisKegiatan().getNamaKegiatan());

						if (belumDibayar > 0.1 && pengaturanPembayaranBulanan.getDetailBiaya().getJenisKegiatan()
								.getDigunakanSyaratCetakSuratBebasAktif()) {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, Bapak/Ibu masih memiliki tagihan {V1} untuk item biaya {V2} sebesar {V3} yang belum diselesaikan. Langkah yang dapat dilakukan: (1) lakukan pelunasan tagihan tersebut pada bagian keuangan; (2) simpan bukti pembayaran Anda dengan baik; (3) hubungi bagian keuangan apabila memerlukan informasi lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									detailBiaya.getJenisKegiatan().getNamaKegiatan(),
									detailBiaya.getItemBiaya().getNama(),
									Common.numberFormat.get().format(belumDibayar));
							return;
						}
					} else {
						DetailBiaya detailBiaya = (DetailBiaya) o;

						Double jumlah = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, false);

						Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya, cicilanPembayarans);

						if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
								&& telahDibayar.intValue() > 0) {
							jumlah = telahDibayar;
						}

						if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
							jumlah = -Math.abs(jumlah);
							telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
						}

						int tot = (int) (jumlah.intValue() + (telahDibayar == null ? 0.0 : telahDibayar.doubleValue()));
						if (tot == 0) {
							continue;
						}

						Double belumDibayar = jumlah - (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

						map.put("kode", detailBiaya.getItemBiaya().getKode());
						map.put("item_biaya",
								detailBiaya.getItemBiaya().getNama()
										+ (detailBiaya.getDetailSettingBiaya() != null
												&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
												&& detailBiaya.getDetailSettingBiaya().getSettingBiaya()
														.getJumlahPembayaran() > 1 ? ", ke-" + detailBiaya.getBayarKe()
																: ""));

						map.put("biaya", jumlah);
						map.put("dibayar", telahDibayar);
						map.put("sisa", belumDibayar);
						map.put("nama_kegiatan_semester", detailBiaya.getJenisKegiatan().getNamaKegiatan() + "-" + smt);
						map.put("semester", smt);
						map.put("tahun_ajaran", detailBiaya.getTahunAkademik());
						map.put("nama_kegiatan", detailBiaya.getJenisKegiatan().getNamaKegiatan());

						if (belumDibayar > 0.1
								&& detailBiaya.getJenisKegiatan().getDigunakanSyaratCetakSuratBebasAktif()) {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, Bapak/Ibu masih memiliki tagihan {V1} untuk item biaya {V2} sebesar {V3} yang belum diselesaikan. Langkah yang dapat dilakukan: (1) lakukan pelunasan tagihan tersebut pada bagian keuangan; (2) simpan bukti pembayaran Anda dengan baik; (3) hubungi bagian keuangan apabila memerlukan informasi lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									detailBiaya.getJenisKegiatan().getNamaKegiatan(),
									detailBiaya.getItemBiaya().getNama(),
									Common.numberFormat.get().format(belumDibayar));
							return;
						}
					}
					maps.add(map);
				}
			}
		}

		parameters.put("maps", maps);

		parameters.put("mahasiswa.nama", mhs.getNama());
		parameters.put("mahasiswa.nim", mhs.getNim());
		parameters.put("mahasiswa.jurusan.nama", mhs.getJurusan().getNama());
		parameters.put("semester", Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		parameters.put("tahun_akademik", Common.getCurrentTahunAkademik());
		parameters.put("tanggal", Common.dateFormat2.get().format(new Date()));
		parameters.put("bagian_keuangan", "Bagian Keuangan");

		Report.generatePDFReport("pdf", parameters, "surat_bebas_tuggakan", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanTagihan(final BiodataCalonMahasiswa mhs, final TreeMap<String, Object[]> semua)
			throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("nama_mahasiswa", mhs.getNama());
				parameters.put("no_registrasi", mhs.getNim());

				parameters.put("fakultas", mhs.getProdiLulus() != null ? mhs.getProdiLulus().getFakultas().getNama()
						: mhs.getProdi1() != null ? mhs.getProdi1().getFakultas().getNama() : "");
				parameters.put("jurusan", mhs.getProdiLulus() != null ? mhs.getProdiLulus().getNama()
						: mhs.getProdi1() != null ? mhs.getProdi1().getNama() : "");
				parameters.put("program", mhs.getProgram());
				parameters.put("tahunangkatan", mhs.getTahun());

				Common.insertProperty(BiodataCalonMahasiswa.class, mhs, parameters, "cln");

				Mahasiswa m = mhs.getMahasiswa();
				if (m != null) {
					BiodataMahasiswa biodataMahasiswa = m.ambilBiodata();
					if (biodataMahasiswa != null) {
						Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, parameters, "bio");
					}
					Common.insertProperty(Mahasiswa.class, m, parameters, "mhs");
				}

				System.out.println("parameters " + parameters);
				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

				for (String key : semua.keySet()) {
					Integer smt = Integer.parseInt(key.split("-")[1]);
					Object[] val = semua.get(key);
					Collection detailBiayas = (Collection) val[0];
					Kegiatan kegiatan = (Kegiatan) val[2];
					if (kegiatan != null && kegiatan.getAktif()) {
						Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
								: kegiatan.ambilDetailKegiatan(false);

						List<CicilanPembayaran> cicilanPembayarans = kegiatan == null || kegiatan.getId() == null
								? new ArrayList<CicilanPembayaran>()
								: kegiatan.ambilCicilan();

						for (Object o : detailBiayas) {

							DetailBiaya tempdetailBiaya = null;
							PengaturanPembayaranBulanan temppengaturanPembayaranBulanan = null;
							if (o instanceof DetailBiaya) {
								tempdetailBiaya = (DetailBiaya) o;

							} else if (o instanceof PengaturanPembayaranBulanan) {
								temppengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
								if (temppengaturanPembayaranBulanan != null) {
									tempdetailBiaya = temppengaturanPembayaranBulanan.getDetailBiaya();
								}

							}

							DetailKegiatan tempdata = kegiatan == null ? null
									: temppengaturanPembayaranBulanan != null
											? kegiatan.ambilSatuDetailKegiatan(temppengaturanPembayaranBulanan,
													detailKegiatans)
											: kegiatan.ambilSatuDetailKegiatan(tempdetailBiaya);

							DetailKegiatan detailKegiatan = tempdata;
							if (detailKegiatan != null && detailKegiatan.getBukanTagihan()) {
								continue;
							}

							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
							if (o instanceof PengaturanPembayaranBulanan) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;

								Double jumlah = Kegiatan.ambilJumlahTagihan(detailKegiatan,
										pengaturanPembayaranBulanan.getDetailBiaya(), kegiatan, smt,
										pengaturanPembayaranBulanan);

								Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan,
										pengaturanPembayaranBulanan, cicilanPembayarans);

								if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
										&& telahDibayar.intValue() > 0) {
									jumlah = telahDibayar;
								}

								if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getPenghitungan()
										.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									jumlah = -Math.abs(jumlah);
									telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
								}

								int tot = (int) (jumlah.intValue()
										+ (telahDibayar == null ? 0.0 : telahDibayar.doubleValue()));
								if (tot == 0) {
									continue;
								}

								Double belumDibayar = jumlah
										- (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

								map.put("kode", pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode());
								map.put("item_biaya",
										pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()
												+ ", Bulan " + pengaturanPembayaranBulanan.getNamaBulan());
								map.put("biaya", jumlah);
								map.put("dibayar", telahDibayar);
								map.put("sisa", belumDibayar);
								map.put("nama_kegiatan_semester", pengaturanPembayaranBulanan.getDetailBiaya()
										.getJenisKegiatan().getNamaKegiatan() + "-" + smt);
								map.put("semester", smt);

								if (m != null) {
									Mahasiswa mahasiswa = m;
									Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
									Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
									Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
											semesterMulai, mahasiswa.getSemesterMulai());

									String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
									map.put("tahun_ajaran", tahunAkademik);
								} else if (mhs != null) {
									Integer tahunAngkatanMhs = mhs.getTahun();
									Integer semesterMulai = mhs.getPindahDariKampusLamaDiSemester();
									Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
											semesterMulai, mhs.getSemesterMulai());

									String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
									map.put("tahun_ajaran", tahunAkademik);
								} else {
									map.put("tahun_ajaran",
											pengaturanPembayaranBulanan.getDetailBiaya().getTahunAkademik());
								}

								map.put("nama_kegiatan", pengaturanPembayaranBulanan.getDetailBiaya().getJenisKegiatan()
										.getNamaKegiatan());
							} else {
								DetailBiaya detailBiaya = (DetailBiaya) o;

								Double jumlah = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya,
										false);

								Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya,
										cicilanPembayarans);

								if (jumlah != null && jumlah.intValue() == 0 && telahDibayar != null
										&& telahDibayar.intValue() > 0) {
									jumlah = telahDibayar;
								}

								if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
									jumlah = -Math.abs(jumlah);
									telahDibayar = (telahDibayar == null ? 0.0 : -Math.abs(telahDibayar.doubleValue()));
								}

								int tot = (int) (jumlah.intValue()
										+ (telahDibayar == null ? 0.0 : telahDibayar.doubleValue()));
								if (tot == 0) {
									continue;
								}

								Double belumDibayar = jumlah
										- (telahDibayar == null ? 0.0 : telahDibayar.doubleValue());

								map.put("kode", detailBiaya.getItemBiaya().getKode());
								map.put("item_biaya", detailBiaya.getItemBiaya().getNama()
										+ (detailBiaya.getDetailSettingBiaya() != null
												&& detailBiaya.getDetailSettingBiaya().getSettingBiaya() != null
												&& detailBiaya.getDetailSettingBiaya().getSettingBiaya()
														.getJumlahPembayaran() > 1 ? ", ke-" + detailBiaya.getBayarKe()
																: ""));

								map.put("biaya", jumlah);
								map.put("dibayar", telahDibayar);
								map.put("sisa", belumDibayar);
								map.put("nama_kegiatan_semester",
										detailBiaya.getJenisKegiatan().getNamaKegiatan() + "-" + smt);
								map.put("semester", smt);
								if (m != null) {
									Mahasiswa mahasiswa = m;
									Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
									Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
									Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
											semesterMulai, mahasiswa.getSemesterMulai());

									String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
									map.put("tahun_ajaran", tahunAkademik);
								} else if (mhs != null) {
									Integer tahunAngkatanMhs = mhs.getTahun();
									Integer semesterMulai = mhs.getPindahDariKampusLamaDiSemester();
									Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
											semesterMulai, mhs.getSemesterMulai());

									String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
									map.put("tahun_ajaran", tahunAkademik);
								} else {
									map.put("tahun_ajaran", detailBiaya.getTahunAkademik());
								}
								map.put("nama_kegiatan", detailBiaya.getJenisKegiatan().getNamaKegiatan());
							}
							maps.add(map);
						}
					}
				}

				parameters.put("maps", maps);

				Report.generatePDFReport("pdf", parameters, "tagihan", ais.ui.util.WaktuUtil.getDate());

			}
		});
	}

	public static List<Map<String, Serializable>> generateParameterMapAbsensi(VOPembelajaran voPembelajaran)
			throws Exception {
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		List<Pertemuan> pertemuans = voPembelajaran.ambilPertemuanList();
		List<Dosen> dosens = voPembelajaran.populateDosenBuNama();

		if (voPembelajaran instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
			Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();

			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
						Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

						String linkFoto = CommonMedia
								.getUrlFotoPenggunaKecil(new Tbmuser(detailperkuliahan.getMahasiswa()));
						map.put("link_foto", linkFoto);

						BiodataMahasiswa biodataMahasiswa = detailperkuliahan.getMahasiswa().ambilBiodata();

						String hp = biodataMahasiswa.getHp();
						String telp = biodataMahasiswa.getTeleponRumah();

						String h = (hp == null || hp.toString().trim().equals("08100000000000000000")
								|| hp.toString().trim().equals("0000000000") ? "" : hp)
								+ (telp == null || telp.toString().trim().isEmpty()
										|| telp.toString().trim().equals("00000000000000000000")
										|| telp.toString().trim().equals("000000000")
												? ""
												: (hp == null || hp.toString().trim().isEmpty()
														|| hp.toString().trim().equals("08100000000000000000")
														|| hp.toString().trim().equals("0000000000") ? "" : " / ")
														+ telp);

						map.put("hp", h);

						map.put("nim", detailperkuliahan.getMahasiswa().getNim());
						map.put("nama", detailperkuliahan.getMahasiswa().getNama());
						// getRuang()/getJurusan() di sekitar sini sudah dijaga null, getMatakuliah()
						// belum -- perkuliahan tanpa matakuliah (mis. matakuliahnya sudah dihapus)
						// membuat SELURUH laporan gagal dicetak. Kini barisnya tetap tercetak
						// dengan kolom matakuliah kosong, sama seperti perlakuan ruang/jurusan.
						map.put("nama_matakuliah",
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());
						map.put("kode_matakuliah",
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());
						map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
						map.put("sks", perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getSks());
						map.put("jenis_semester",
								perkuliahan.getStatusSemesterPendek() != null
										&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
												? Perkuliahan.SP
												: perkuliahan.getGanjilGenap());

						map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
						// getJurusan() dijaga null, getFakultas() di belakangnya tidak: program studi
						// tanpa fakultas induk tetap melempar NPE dan menggagalkan laporan.
						map.put("fakultas", perkuliahan.getJurusan() == null
								|| perkuliahan.getJurusan().getFakultas() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getNama());
						map.put("kelas", perkuliahan.getKelas());
						map.put("nama_jurusan",
								perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
						map.put("semester", perkuliahan.getSemester());
						map.put("nama_kaprodi",
								perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getKaprodi() == null ? ""
												: perkuliahan.getJurusan().getKaprodi().getNama());
						map.put("nip_kaprodi",
								perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getKaprodi() == null ? ""
												: perkuliahan.getJurusan().getKaprodi().getCode());

						int index = 1;
						String dosensNama = "";
						for (Dosen dosen : dosens) {
							dosensNama += dosensNama.isEmpty() ? dosen.getNama() : " / " + dosen.getNama();
							map.put("nama_dosen" + index, dosen.getNama());
							map.put("nidn_dosen" + index, dosen.getNidn());
							map.put("nip_dosen" + index, dosen.getMycode());
							map.put("nip1_dosen" + index, dosen.getCode());

							LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
							String nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = null;
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:6175");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									map.put("ttd_dosen" + index, ttd);
									map.put("ttd_dosen_" + index, ttd);
									map.put("ttd_dosen_id_" + dosen.getId(), ttd);
								}
							}

							index++;
						}
						map.put("dosensNama", dosensNama);

						String ttd = null;
						if (perkuliahan.getDosen1() != null) {

							LampiranLain lam = LampiranLain.ambil(perkuliahan.getDosen1().getId(),
									LampiranLain.TTD_DOSEN);
							String nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:6200");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									map.put("ttd_dosen1", ttd);
									map.put("ttd_dosen_1", ttd);
									map.put("ttd_dosen_id_" + perkuliahan.getDosen1().getId(), ttd);
								}
							}

						}
//						System.out.println("ttd_dosen1 => " + ttd);

						map.put("nama_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
						map.put("nidn_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());
						map.put("nip_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getMycode());
						map.put("nip1_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getCode());

						if (perkuliahan.getDosen2() != null) {

							LampiranLain lam = LampiranLain.ambil(perkuliahan.getDosen2().getId(),
									LampiranLain.TTD_DOSEN);
							String nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:6233");
									}

									map.put("ttd_dosen2", ttd);
									map.put("ttd_dosen_2", ttd);
									map.put("ttd_dosen_id_" + perkuliahan.getDosen2().getId(), ttd);
								}
							}
						}
//						System.out.println("ttd_dosen2 => " + ttd);

						map.put("hari", perkuliahan.getHari());
						map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
								: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
						map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
								: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

						map.put("jenjang", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getJenjang().getNama());
						map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
						map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

						Map<Long, List<String>> dataAbsensi = new WeakHashMap<Long, List<String>>();
						int i = 1;
						int total = 0;
						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								String kode = pertemuan.retreiveAbsensiKode(detailperkuliahan.getMahasiswa().getId());
								int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
								total += nilai;
								map.put("p" + i, kode);
								map.put("n" + i, nilai);
								map.put("tgl" + i, pertemuan.getTanggal());
								map.put("status_pertemuan" + i, pertemuan.getStatusPertemuan() == null ? ""
										: pertemuan.getStatusPertemuan().getNama());
								i++;

								if (pertemuan.getPerkuliahan() != null) {
									if (!dataAbsensi.containsKey(pertemuan.getPerkuliahan().getId())) {
										dataAbsensi.put(pertemuan.getPerkuliahan().getId(), new ArrayList<String>());
										dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
									} else {
										dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
									}
								}
							}
						}

						Map<String, Integer> statuses = Perkuliahan.hitungStatus(
								dataAbsensi.get(detailperkuliahan.getPerkuliahan().getId()),
								detailperkuliahan.getMahasiswa().getId());

						int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
						int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
						int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;

						int semua = 0;
						semua += qtyAlpa;
						semua += qtySakit;
						semua += qtyIzin;
						int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

						map.put("qtyAlpa", qtyAlpa);
						map.put("qtySakit", qtySakit);
						map.put("qtyIzin", qtyIzin);
						map.put("qtyHadir", masuk);
						map.put("qtyTidakHadir", semua);

						map.put("total_n", total);

						maps.add(map);
					}
				}
			}

			int indexDosen = 1;
			for (Dosen dosen : dosens) {
				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

				String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(dosen));
				map.put("link_foto", linkFoto);

				map.put("nim", "Dosen " + (indexDosen == 1 ? "Utama" : "ke-" + indexDosen));
				map.put("nama", dosen.getNama());
				map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
				map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
				map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
				map.put("sks", perkuliahan.getMatakuliah().getSks());
				map.put("jenis_semester",
						perkuliahan.getStatusSemesterPendek() != null
								&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
										? Perkuliahan.SP
										: perkuliahan.getGanjilGenap());

				map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
				map.put("fakultas",
						perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getFakultas().getNama());
				map.put("kelas", perkuliahan.getKelas());
				map.put("nama_jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
				map.put("semester", perkuliahan.getSemester());
				map.put("nama_kaprodi",
						perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getKaprodi() == null ? ""
										: perkuliahan.getJurusan().getKaprodi().getNama());
				map.put("nip_kaprodi",
						perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getKaprodi() == null ? ""
										: perkuliahan.getJurusan().getKaprodi().getCode());
				map.put("nama_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
				map.put("nidn_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());
				map.put("nip_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getMycode());
				map.put("nip1_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());

				map.put("nama_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
				map.put("nidn_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());
				map.put("nip_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getMycode());
				map.put("nip1_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getCode());

				int index = 1;
				String dosensNama = "";
				for (Dosen dosen1 : dosens) {
					dosensNama += dosensNama.isEmpty() ? dosen1.getNama() : " / " + dosen1.getNama();
					map.put("nama_dosen" + index, dosen1.getNama());
					map.put("nidn_dosen" + index, dosen1.getNidn());
					map.put("nip_dosen" + index, dosen1.getMycode());
					map.put("nip1_dosen" + index, dosen1.getCode());
					index++;
				}
				map.put("dosensNama", dosensNama);

				map.put("hari", perkuliahan.getHari());
				map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
						: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
				map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
						: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

				map.put("jenjang",
						perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getJenjang().getNama());
				map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
				map.put("perguruan_tinggi",
						perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				Map<Long, List<String>> dataAbsensi = new WeakHashMap<Long, List<String>>();
				int i = 1;
				int total = 0;
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						String kode = pertemuan.retreiveAbsensiKode(dosen.getId());
						int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
						total += nilai;
						map.put("p" + i, kode);
						map.put("n" + i, nilai);
						map.put("tgl" + i, pertemuan.getTanggal());
						map.put("status_pertemuan" + i, pertemuan.getStatusPertemuan().getNama());
						i++;

						if (pertemuan.getPerkuliahan() != null) {
							if (!dataAbsensi.containsKey(pertemuan.getPerkuliahan().getId())) {
								dataAbsensi.put(pertemuan.getPerkuliahan().getId(), new ArrayList<String>());
								dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
							} else {
								dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
							}
						}
					}
				}
				map.put("total_n", total);

				Map<String, Integer> statuses = Perkuliahan.hitungStatus(dataAbsensi.get(perkuliahan.getId()),
						dosen.getId());

				int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
				int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
				int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;

				int semua = 0;
				semua += qtyAlpa;
				semua += qtySakit;
				semua += qtyIzin;
				int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

				map.put("qtyAlpa", qtyAlpa);
				map.put("qtySakit", qtySakit);
				map.put("qtyIzin", qtyIzin);
				map.put("qtyHadir", masuk);
				map.put("qtyTidakHadir", semua);

				maps.add(map);
				indexDosen++;
			}

			List<Mahasiswa> asistens = perkuliahan.ambilAsisten();
			for (Mahasiswa asisten : asistens) {
				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

				String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(asisten));
				map.put("link_foto", linkFoto);

				map.put("nim", "Asisten Dosen");
				map.put("nama", asisten.getNama());
				map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
				map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
				map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
				map.put("sks", perkuliahan.getMatakuliah().getSks());
				map.put("jenis_semester",
						perkuliahan.getStatusSemesterPendek() != null
								&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
										? Perkuliahan.SP
										: perkuliahan.getGanjilGenap());

				map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
				map.put("fakultas",
						perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getFakultas().getNama());
				map.put("kelas", perkuliahan.getKelas());
				map.put("nama_jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
				map.put("semester", perkuliahan.getSemester());
				map.put("nama_kaprodi",
						perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getKaprodi() == null ? ""
										: perkuliahan.getJurusan().getKaprodi().getNama());
				map.put("nip_kaprodi",
						perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getKaprodi() == null ? ""
										: perkuliahan.getJurusan().getKaprodi().getCode());

				map.put("nama_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
				map.put("nidn_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());
				map.put("nip_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getMycode());
				map.put("nip1_dosen1", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());

				map.put("nama_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
				map.put("nidn_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());
				map.put("nip_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getMycode());
				map.put("nip1_dosen2", perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getCode());

				int index = 1;
				String dosensNama = "";
				for (Dosen dosen : dosens) {
					if (dosen != null) {
						dosensNama += dosensNama.isEmpty() ? dosen.getNama() : " / " + dosen.getNama();
						map.put("nama_dosen" + index, dosen.getNama());
						map.put("nidn_dosen" + index, dosen.getNidn());
						map.put("nip_dosen" + index, dosen.getMycode());
						map.put("nip1_dosen" + index, dosen.getCode());
						index++;
					}
				}
				map.put("dosensNama", dosensNama);

				map.put("hari", perkuliahan.getHari());
				map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
						: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
				map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
						: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

				map.put("jenjang",
						perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getJenjang().getNama());
				map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
				map.put("perguruan_tinggi",
						perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				Map<Long, List<String>> dataAbsensi = new WeakHashMap<Long, List<String>>();
				int i = 1;
				int total = 0;
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						String kode = pertemuan.retreiveAbsensiKode(asisten.getId());
						int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
						total += nilai;
						map.put("p" + i, kode);
						map.put("n" + i, nilai);
						map.put("tgl" + i, pertemuan.getTanggal());
						map.put("status_pertemuan" + i, pertemuan.getStatusPertemuan().getNama());
						i++;

						if (pertemuan.getPerkuliahan() != null) {
							if (!dataAbsensi.containsKey(pertemuan.getPerkuliahan().getId())) {
								dataAbsensi.put(pertemuan.getPerkuliahan().getId(), new ArrayList<String>());
								dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
							} else {
								dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
							}
						}
					}
				}
				map.put("total_n", total);

				Map<String, Integer> statuses = Perkuliahan.hitungStatus(dataAbsensi.get(perkuliahan.getId()),
						asisten.getId());

				int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
				int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
				int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;

				int semua = 0;
				semua += qtyAlpa;
				semua += qtySakit;
				semua += qtyIzin;
				int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

				map.put("qtyAlpa", qtyAlpa);
				map.put("qtySakit", qtySakit);
				map.put("qtyIzin", qtyIzin);
				map.put("qtyHadir", masuk);
				map.put("qtyTidakHadir", semua);

				maps.add(map);
			}
		} else {
			boolean refresh = true;
			List<Long> mhss = voPembelajaran.ambilMahasiswaById(refresh);
			for (Long mhs : mhss) {
				Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), mhs);
				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
				String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(mahasiswa));
				map.put("link_foto", linkFoto);
				map.put("id", mahasiswa.getId());
				map.put("nim_mhs", mahasiswa.getNim());
				map.put("nama_mhs", mahasiswa.getNama());
				map.put("kelamin", mahasiswa.getKelamin());
				map.put("telp", mahasiswa.getTelp());
				map.put("email", mahasiswa.getEmail());
				map.put("jurusan", mahasiswa.getJurusan().getNama());
				map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("nama_mhs", mahasiswa.getNama());
				map.put("nama_mhs", mahasiswa.getNama());

				if (voPembelajaran instanceof KelompokKkn) {
					KelompokKkn kelompokKkn = (KelompokKkn) voPembelajaran;
					map.put("nama_beasiswa", kelompokKkn.getKkn().getNama());
					map.put("nama_kelompok", kelompokKkn.getNama_kelompok());
					map.put("alamat", kelompokKkn.getAlamat());
				} else if (voPembelajaran instanceof KelompokPkl) {
					KelompokPkl kelompokPkl = (KelompokPkl) voPembelajaran;
					map.put("nama_beasiswa", kelompokPkl.getPkl().getNama());
					map.put("nama_kelompok", kelompokPkl.getNama_kelompok());
					map.put("alamat", kelompokPkl.getAlamat());
				} else {
					map.put("nama_beasiswa", voPembelajaran.infoSimple());
				}

				int index = 1;
				String dosensNama = "";
				for (Dosen dosen : dosens) {
					if (dosen != null) {
						dosensNama += dosensNama.isEmpty() ? dosen.getNama() : " / " + dosen.getNama();
						map.put("nama_dosen" + index, dosen.getNama());
						map.put("nidn_dosen" + index, dosen.getNidn());
						map.put("nip_dosen" + index, dosen.getMycode());
						map.put("nip1_dosen" + index, dosen.getCode());
						index++;
					}
				}
				map.put("dosensNama", dosensNama);

				Map<Long, List<String>> dataAbsensi = new WeakHashMap<Long, List<String>>();
				int i = 1;
				int total = 0;
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						String kode = pertemuan.retreiveAbsensiKode(mhs);
						int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
						total += nilai;
						map.put("p" + i, kode);
						map.put("n" + i, nilai);
						map.put("tgl" + i, pertemuan.getTanggal());
						map.put("status_pertemuan" + i,
								pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());
						i++;

						if (pertemuan.getPerkuliahan() != null) {
							if (!dataAbsensi.containsKey(pertemuan.getPerkuliahan().getId())) {
								dataAbsensi.put(pertemuan.getPerkuliahan().getId(), new ArrayList<String>());
								dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
							} else {
								dataAbsensi.get(pertemuan.getPerkuliahan().getId()).add(pertemuan.getAbsensi());
							}
						}
					}
				}

				Map<String, Integer> statuses = Perkuliahan.hitungStatus(dataAbsensi.get(voPembelajaran.getId()), mhs);

				int qtyAlpa = statuses.containsKey("A") ? statuses.get("A") : 0;
				int qtySakit = statuses.containsKey("S") ? statuses.get("S") : 0;
				int qtyIzin = statuses.containsKey("I") ? statuses.get("I") : 0;

				int semua = 0;
				semua += qtyAlpa;
				semua += qtySakit;
				semua += qtyIzin;
				int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

				map.put("qtyAlpa", qtyAlpa);
				map.put("qtySakit", qtySakit);
				map.put("qtyIzin", qtyIzin);
				map.put("qtyHadir", masuk);
				map.put("qtyTidakHadir", semua);

				map.put("total_n", total);

				maps.add(map);
			}
		}
		return maps;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Serializable>> generateParameterMapAbsensi(JadwalPelajaran jadwalPelajaran)
			throws Exception {
		Session session = HibernateUtil.currentSession();
		List<KelasSiswaPunyaSiswa> detailjadwalPelajarans = session.createCriteria(KelasSiswaPunyaSiswa.class)
				.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas())).add(Restrictions.eq("true", true))
				.createAlias("siswa", "siswa").addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama"))
				.list();

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).addOrder(Order.asc("pertemuanKe")).list();

		for (KelasSiswaPunyaSiswa detailjadwalPelajaran : detailjadwalPelajarans) {
			Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

			String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(detailjadwalPelajaran.getSiswa()));
			map.put("link_foto", linkFoto);

			map.put("nim", detailjadwalPelajaran.getSiswa().getNim());
			map.put("nama", detailjadwalPelajaran.getSiswa().getNama());
			map.put("nama_matapelajaran", jadwalPelajaran.getMatapelajaran().getNama());
			map.put("kode_matapelajaran", jadwalPelajaran.getMatapelajaran().getKode());
			map.put("ruang", jadwalPelajaran.getRuang() == null ? "" : jadwalPelajaran.getRuang().getNama());
			map.put("jenis_semester",
					jadwalPelajaran.getSemester() % 2 == 0 ? JadwalPelajaran.GENAP : JadwalPelajaran.GANJIL);

			map.put("tahun_ajaran", jadwalPelajaran.getTahunAjaran());
			map.put("yayasan",
					jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getYayasan().getNama());
			map.put("kelas", jadwalPelajaran.getKelas());
			map.put("nama_sekolah", jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
			map.put("semester", jadwalPelajaran.getSemester());
			map.put("nama_kaprodi",
					jadwalPelajaran.getSekolah() == null ? ""
							: jadwalPelajaran.getSekolah().getNamaKepalaSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getNamaKepalaSekolah());
			map.put("nip_kaprodi",
					jadwalPelajaran.getSekolah() == null ? ""
							: jadwalPelajaran.getSekolah().getNipKepalaSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getNipKepalaSekolah());

			map.put("nama_guru1", jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNama());
			map.put("nidn_guru1", jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getKode());

			map.put("nama_guru2", jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNama());
			map.put("nidn_guru2", jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getKode());

			map.put("hari", jadwalPelajaran.getHari());
			map.put("jam", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
					: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));
			map.put("waktu", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
					: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));

			map.put("jenjang", jadwalPelajaran.getSekolah() == null ? ""
					: jadwalPelajaran.getSekolah().getJenisSekolah().getNama());
			map.put("sekolah", jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
			map.put("perguruan_tinggi",
					jadwalPelajaran.getSekolah() == null ? ""
							: jadwalPelajaran.getSekolah().getYayasan().getNama() == null ? ""
									: jadwalPelajaran.getSekolah().getYayasan().getNama());

			int i = 1;
			int total = 0;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					String kode = pertemuan.retreiveAbsensiKode(detailjadwalPelajaran.getSiswa().getId());
					int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
					total += nilai;
					map.put("p" + i, kode);
					map.put("n" + i, nilai);
					map.put("tgl" + i, pertemuan.getTanggal());
					map.put("status_pertemuan" + i, pertemuan.getStatusPertemuan().getNama());
					i++;
				}
			}

			map.put("total_n", total);

			maps.add(map);
		}

		List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
		int indexGuru = 1;
		for (Guru guru : gurus) {
			Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

			String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(guru));
			map.put("link_foto", linkFoto);

			map.put("nim", "Guru " + (indexGuru == 1 ? "Utama" : "ke-" + indexGuru));
			map.put("nama", guru.getNama());
			map.put("nama_matapelajaran", jadwalPelajaran.getMatapelajaran().getNama());
			map.put("kode_matapelajaran", jadwalPelajaran.getMatapelajaran().getKode());
			map.put("ruang", jadwalPelajaran.getRuang() == null ? "" : jadwalPelajaran.getRuang().getNama());

			map.put("jenis_semester",
					jadwalPelajaran.getSemester() % 2 == 0 ? JadwalPelajaran.GENAP : JadwalPelajaran.GANJIL);

			map.put("tahun_ajaran", jadwalPelajaran.getTahunAjaran());
			map.put("yayasan",
					jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getYayasan().getNama());
			map.put("kelas", jadwalPelajaran.getKelas());
			map.put("nama_sekolah", jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
			map.put("semester", jadwalPelajaran.getSemester());
			map.put("nama_kaprodi",
					jadwalPelajaran.getSekolah() == null ? ""
							: jadwalPelajaran.getSekolah().getNamaKepalaSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getNamaKepalaSekolah());
			map.put("nip_kaprodi",
					jadwalPelajaran.getSekolah() == null ? ""
							: jadwalPelajaran.getSekolah().getNipKepalaSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getNipKepalaSekolah());
			map.put("nama_guru1", jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNama());
			map.put("nidn_guru1", jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getKode());

			map.put("nama_guru2", jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNama());
			map.put("nidn_guru2", jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getKode());

			map.put("hari", jadwalPelajaran.getHari());
			map.put("jam", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
					: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));
			map.put("waktu", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
					: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));

			map.put("jenjang", jadwalPelajaran.getSekolah() == null ? ""
					: jadwalPelajaran.getSekolah().getJenisSekolah().getNama());
			map.put("sekolah", jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
			map.put("perguruan_tinggi",
					jadwalPelajaran.getSekolah() == null ? ""
							: jadwalPelajaran.getSekolah().getYayasan().getNama() == null ? ""
									: jadwalPelajaran.getSekolah().getYayasan().getNama());

			int i = 1;
			int total = 0;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					String kode = pertemuan.retreiveAbsensiKode(guru.getId());
					int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
					total += nilai;
					map.put("p" + i, kode);
					map.put("n" + i, nilai);
					map.put("tgl" + i, pertemuan.getTanggal());
					map.put("status_pertemuan" + i, pertemuan.getStatusPertemuan().getNama());
					i++;
				}
			}
			map.put("total_n", total);
			maps.add(map);
			indexGuru++;
		}

		return maps;
	}

	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinci(Perkuliahan perkuliahan, boolean mhs,
			boolean dsn) throws Exception {
		return generateParameterMapAbsensiRinci(perkuliahan, null, null, null, mhs, dsn);
	}

	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinci(VOPembelajaran voPembelajaran,
			Detailperkuliahan d, Pertemuan p, Dosen dos, boolean mhs, boolean dsn) throws Exception {

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		List<Pertemuan> pertemuans = p != null ? new ArrayList<Pertemuan>() : voPembelajaran.ambilPertemuanList();
		if (p != null) {
			pertemuans.add(p);
		}
		if (mhs) {
			if (voPembelajaran instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
				Collection<Long> detailperkuliahans = d != null ? new ArrayList<Long>()
						: perkuliahan.ambilDetailperkuliahan();

				if (d != null) {
					detailperkuliahans.add(d.getId());
				}

				for (Long detailperkuliahanid : detailperkuliahans) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {

						int i = 1;
						int total = 0;

						for (Pertemuan pertemuan : pertemuans) {
							if (pertemuan.getAktif()) {
								Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

								String linkFoto = CommonMedia
										.getUrlFotoPenggunaKecil(new Tbmuser(detailperkuliahan.getMahasiswa()));
								map.put("link_foto", linkFoto);

								map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
										: Common.dateFormat4.get().format(pertemuan.getTanggal()));
								map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
										: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
								map.put("waktu_rencana",
										perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
								map.put("waktu_realisasi", pertemuan
										.retreiveAbsensiMulai(detailperkuliahan.getMahasiswa().getId()) + " - "
										+ pertemuan.retreiveAbsensiSampai(detailperkuliahan.getMahasiswa().getId()));
								map.put("catatan",
										pertemuan.retreiveAbsensiKeterangan(detailperkuliahan.getMahasiswa().getId()));

								map.put("nim", detailperkuliahan.getMahasiswa().getNim());
								map.put("nama", detailperkuliahan.getMahasiswa().getNama());
								map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
								map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
								map.put("ruang",
										perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
								map.put("sks", perkuliahan.getMatakuliah().getSks());
								map.put("jenis_semester", perkuliahan.getGanjilGenap());

								map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
								map.put("fakultas", perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getNama());
								map.put("kelas", perkuliahan.getKelas());
								map.put("nama_jurusan",
										perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
								map.put("semester", perkuliahan.getSemester());
								map.put("nama_kaprodi",
										perkuliahan.getJurusan() == null ? ""
												: perkuliahan.getJurusan().getKaprodi() == null ? ""
														: perkuliahan.getJurusan().getKaprodi().getNama());
								map.put("nip_kaprodi",
										perkuliahan.getJurusan() == null ? ""
												: perkuliahan.getJurusan().getKaprodi() == null ? ""
														: perkuliahan.getJurusan().getKaprodi().getCode());

								map.put("nama_dosen1",
										perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
								map.put("nidn_dosen1",
										perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

								map.put("nama_dosen2",
										perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
								map.put("nidn_dosen2",
										perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

								map.put("hari", perkuliahan.getHari());
								map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
										: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
								map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
										: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

								map.put("jenjang", perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getJenjang().getNama());
								map.put("jurusan",
										perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
								map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
												: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi()
														.getNama());

								map.put("status_kehadiran",
										pertemuan.retreiveAbsensiNama(detailperkuliahan.getMahasiswa().getId()));

								String kode = pertemuan.retreiveAbsensiKode(detailperkuliahan.getMahasiswa().getId());
								int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
								total += nilai;
								map.put("p" + i, kode);
								map.put("n" + i, nilai);
								map.put("tgl" + i, pertemuan.getTanggal());
								map.put("status_pertemuan1",
										Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));

								map.put("total_n", total);

								maps.add(map);

								i++;
							}
						}
					}
				}
			}
		}

		if (dsn) {
			if (voPembelajaran instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
				List<Dosen> dosens = dos != null ? new ArrayList<Dosen>() : perkuliahan.populateDosenBuNama();
				if (dos != null) {
					dosens.add(dos);
				}
				int indexDosen = 1;
				for (Dosen dosen : dosens) {

					int i = 1;
					int total = 0;
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

							String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(dosen));
							map.put("link_foto", linkFoto);

							map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()));
							map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
							map.put("waktu_rencana",
									perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
							map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(dosen.getId()) + " - "
									+ pertemuan.retreiveAbsensiSampai(dosen.getId()));
							map.put("catatan", pertemuan.retreiveAbsensiKeterangan(dosen.getId()));

							map.put("nim", "Dosen " + (indexDosen == 1 ? "Utama" : "ke-" + indexDosen));
							map.put("nama", dosen.getNama());
							map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
							map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
							map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
							map.put("sks", perkuliahan.getMatakuliah().getSks());
							map.put("jenis_semester", perkuliahan.getGanjilGenap());

							map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
							map.put("fakultas", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getNama());
							map.put("kelas", perkuliahan.getKelas());
							map.put("nama_jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("semester", perkuliahan.getSemester());
							map.put("nama_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getNama());
							map.put("nip_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getCode());

							map.put("nama_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
							map.put("nidn_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

							map.put("nama_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
							map.put("nidn_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

							map.put("hari", perkuliahan.getHari());
							map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
							map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

							map.put("jenjang", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getJenjang().getNama());
							map.put("jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

							map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(dosen.getId()));

							String kode = pertemuan.retreiveAbsensiKode(dosen.getId());
							int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
							total += nilai;
							map.put("p" + i, kode);
							map.put("n" + i, nilai);
							map.put("tgl" + i, pertemuan.getTanggal());
							map.put("status_pertemuan1",
									Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));
							i++;

							map.put("total_n", total);
							maps.add(map);

						}
						indexDosen++;
					}
				}

				List<Mahasiswa> asistens = perkuliahan.ambilAsisten();
				for (Mahasiswa asisten : asistens) {

					int i = 1;
					int total = 0;
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

							String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(asisten));
							map.put("link_foto", linkFoto);

							map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()));
							map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
							map.put("waktu_rencana",
									perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
							map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(asisten.getId()) + " - "
									+ pertemuan.retreiveAbsensiSampai(asisten.getId()));
							map.put("catatan", pertemuan.retreiveAbsensiKeterangan(asisten.getId()));

							map.put("nim", "Asisten Dosen " + asisten.getNim());
							map.put("nama", asisten.getNama());
							map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
							map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
							map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
							map.put("sks", perkuliahan.getMatakuliah().getSks());
							map.put("jenis_semester", perkuliahan.getGanjilGenap());

							map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
							map.put("fakultas", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getNama());
							map.put("kelas", perkuliahan.getKelas());
							map.put("nama_jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("semester", perkuliahan.getSemester());
							map.put("nama_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getNama());
							map.put("nip_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getCode());

							map.put("nama_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
							map.put("nidn_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

							map.put("nama_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
							map.put("nidn_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

							map.put("hari", perkuliahan.getHari());
							map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
							map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

							map.put("jenjang", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getJenjang().getNama());
							map.put("jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

							map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(asisten.getId()));

							String kode = pertemuan.retreiveAbsensiKode(asisten.getId());
							int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
							total += nilai;
							map.put("p" + i, kode);
							map.put("n" + i, nilai);
							map.put("tgl" + i, pertemuan.getTanggal());
							map.put("status_pertemuan1",
									Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));
							i++;

							map.put("total_n", total);
							maps.add(map);
						}
					}
				}
			}
		}
		return maps;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinci(JadwalPelajaran jadwalPelajaran,
			boolean mhs, boolean dsn, List<Long> statusPertemuans) throws Exception {
		Session session = HibernateUtil.currentSession();

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).addOrder(Order.asc("pertemuanKe")).list();
		if (mhs) {
			List<KelasSiswaPunyaSiswa> detailjadwalPelajarans = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas())).add(Restrictions.eq("true", true))
					.createAlias("siswa", "siswa").addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama"))
					.list();

			for (KelasSiswaPunyaSiswa detailjadwalPelajaran : detailjadwalPelajarans) {

				int i = 1;
				int total = 0;

				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
								&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {

							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

							String linkFoto = CommonMedia
									.getUrlFotoPenggunaKecil(new Tbmuser(detailjadwalPelajaran.getSiswa()));
							map.put("link_foto", linkFoto);

							map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()));
							map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
							map.put("waktu_rencana",
									jadwalPelajaran.getWaktuMulai() + " - " + jadwalPelajaran.getWaktuSelesai());
							map.put("waktu_realisasi",
									pertemuan.retreiveAbsensiMulai(detailjadwalPelajaran.getSiswa().getId()) + " - "
											+ pertemuan
													.retreiveAbsensiSampai(detailjadwalPelajaran.getSiswa().getId()));
							map.put("catatan",
									pertemuan.retreiveAbsensiKeterangan(detailjadwalPelajaran.getSiswa().getId()));

							map.put("nim", detailjadwalPelajaran.getSiswa().getNim());
							map.put("nama", detailjadwalPelajaran.getSiswa().getNama());
							map.put("nama_matapelajaran", jadwalPelajaran.getMatapelajaran().getNama());
							map.put("kode_matapelajaran", jadwalPelajaran.getMatapelajaran().getKode());
							map.put("ruang",
									jadwalPelajaran.getRuang() == null ? "" : jadwalPelajaran.getRuang().getNama());

							map.put("jenis_semester", jadwalPelajaran.getSemester() % 2 == 0 ? JadwalPelajaran.GENAP
									: JadwalPelajaran.GANJIL);

							map.put("tahun_ajaran", jadwalPelajaran.getTahunAjaran());
							map.put("yayasan", jadwalPelajaran.getSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getYayasan().getNama());
							map.put("kelas", jadwalPelajaran.getKelas());
							map.put("nama_sekolah",
									jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
							map.put("semester", jadwalPelajaran.getSemester());
							map.put("nama_kaprodi",
									jadwalPelajaran.getSekolah() == null ? ""
											: jadwalPelajaran.getSekolah().getNamaKepalaSekolah() == null ? ""
													: jadwalPelajaran.getSekolah().getNamaKepalaSekolah());
							map.put("nip_kaprodi",
									jadwalPelajaran.getSekolah() == null ? ""
											: jadwalPelajaran.getSekolah().getNipKepalaSekolah() == null ? ""
													: jadwalPelajaran.getSekolah().getNipKepalaSekolah());

							map.put("nama_guru1",
									jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNama());
							map.put("nidn_guru1",
									jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getKode());

							map.put("nama_guru2",
									jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNama());
							map.put("nidn_guru2",
									jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getKode());

							map.put("hari", jadwalPelajaran.getHari());
							map.put("jam", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));
							map.put("waktu", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));

							map.put("jenjang", jadwalPelajaran.getSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getJenisSekolah().getNama());
							map.put("sekolah",
									jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
							map.put("perguruan_tinggi",
									jadwalPelajaran.getSekolah() == null ? ""
											: jadwalPelajaran.getSekolah().getYayasan().getNama() == null ? ""
													: jadwalPelajaran.getSekolah().getYayasan().getNama());

							map.put("status_kehadiran",
									pertemuan.retreiveAbsensiNama(detailjadwalPelajaran.getSiswa().getId()));

							String kode = pertemuan.retreiveAbsensiKode(detailjadwalPelajaran.getSiswa().getId());
							int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
							total += nilai;
							map.put("p" + i, kode);
							map.put("n" + i, nilai);
							map.put("tgl" + i, pertemuan.getTanggal());
							map.put("status_pertemuan1",
									Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));

							map.put("total_n", total);

							maps.add(map);

							i++;
						}
					}
				}

			}
		}

		if (dsn) {
			List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
			int indexGuru = 1;
			for (Guru guru : gurus) {

				int i = 1;
				int total = 0;
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
								&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {
							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

							String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(guru));
							map.put("link_foto", linkFoto);

							map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()));
							map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
							map.put("waktu_rencana",
									jadwalPelajaran.getWaktuMulai() + " - " + jadwalPelajaran.getWaktuSelesai());
							map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(guru.getId()) + " - "
									+ pertemuan.retreiveAbsensiSampai(guru.getId()));
							map.put("catatan", pertemuan.retreiveAbsensiKeterangan(guru.getId()));

							map.put("nim", "Guru " + (indexGuru == 1 ? "Utama" : "ke-" + indexGuru));
							map.put("nama", guru.getNama());
							map.put("nama_matapelajaran", jadwalPelajaran.getMatapelajaran().getNama());
							map.put("kode_matapelajaran", jadwalPelajaran.getMatapelajaran().getKode());
							map.put("ruang",
									jadwalPelajaran.getRuang() == null ? "" : jadwalPelajaran.getRuang().getNama());

							map.put("jenis_semester", jadwalPelajaran.getSemester() % 2 == 0 ? JadwalPelajaran.GENAP
									: JadwalPelajaran.GANJIL);

							map.put("tahun_ajaran", jadwalPelajaran.getTahunAjaran());
							map.put("yayasan", jadwalPelajaran.getSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getYayasan().getNama());
							map.put("kelas", jadwalPelajaran.getKelas());
							map.put("nama_sekolah",
									jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
							map.put("semester", jadwalPelajaran.getSemester());
							map.put("nama_kaprodi",
									jadwalPelajaran.getSekolah() == null ? ""
											: jadwalPelajaran.getSekolah().getNamaKepalaSekolah() == null ? ""
													: jadwalPelajaran.getSekolah().getNamaKepalaSekolah());
							map.put("nip_kaprodi",
									jadwalPelajaran.getSekolah() == null ? ""
											: jadwalPelajaran.getSekolah().getNipKepalaSekolah() == null ? ""
													: jadwalPelajaran.getSekolah().getNipKepalaSekolah());

							map.put("nama_guru1",
									jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNama());
							map.put("nidn_guru1",
									jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getKode());

							map.put("nama_guru2",
									jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getNama());
							map.put("nidn_guru2",
									jadwalPelajaran.getGuru2() == null ? "" : jadwalPelajaran.getGuru2().getKode());

							map.put("hari", jadwalPelajaran.getHari());
							map.put("jam", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));
							map.put("waktu", jadwalPelajaran.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (jadwalPelajaran.getWaktuMulai() + " s.d " + jadwalPelajaran.getWaktuSelesai()));

							map.put("jenjang", jadwalPelajaran.getSekolah() == null ? ""
									: jadwalPelajaran.getSekolah().getJenisSekolah().getNama());
							map.put("sekolah",
									jadwalPelajaran.getSekolah() == null ? "" : jadwalPelajaran.getSekolah().getNama());
							map.put("perguruan_tinggi",
									jadwalPelajaran.getSekolah() == null ? ""
											: jadwalPelajaran.getSekolah().getYayasan().getNama() == null ? ""
													: jadwalPelajaran.getSekolah().getYayasan().getNama());

							map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(guru.getId()));

							String kode = pertemuan.retreiveAbsensiKode(guru.getId());
							int nilai = kode != null && kode.equalsIgnoreCase("M") ? 1 : 0;
							total += nilai;
							map.put("p" + i, kode);
							map.put("n" + i, nilai);
							map.put("tgl" + i, pertemuan.getTanggal());
							map.put("status_pertemuan1",
									Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));
							i++;

							map.put("total_n", total);
							maps.add(map);
						}
					}
				}
				indexGuru++;
			}

		}
		return maps;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinciDosen(Fakultas fakultas,
			Jurusan jurusan, String kelas, MasaPerkuliahan masaPerkuliahan, String tahunAkademik, String jenisSemester,
			Integer semesterPendek, Integer ekstrakurikuler, Dosen dsn, String matakuliah, Matakuliah matkul,
			Date mulai, Date sampai, Boolean hanyaYgStatusMasuk, Boolean termasukTglRealisasi, Boolean tampilkanPermk,
			Integer tetap, List<Long> statusPertemuans) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		Criterion criterion = dsn == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("perkuliahan.dosen1", dsn),
						Restrictions.eq("perkuliahan.dosen2", dsn));

		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen3", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen4", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen5", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen6", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen7", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen8", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen9", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen10", dsn));

		if (dsn != null && dsn.getId() != null) {
			criterion = Restrictions.or(criterion, Restrictions.eq("dosenPengganti", dsn.getId()));
		}

		Criterion criterionTgl = Restrictions.sqlRestriction("true");

		// Penyaringan rentang tanggal. Secara DEFAULT berdasar tanggal TERJADWAL (kolom "tanggal")
		// sehingga tanggal yang tampil tidak keluar dari rentang yang dipilih.
		//
		// Bila opsi "Termasuk tanggal realisasi" DICENTANG (termasukTglRealisasi = true), pertemuan
		// JUGA diikutkan apabila tanggal REALISASI (kolom "tanggalRealisasi") berada dalam rentang.
		// Kolom tanggalRealisasi diisi saat dosen mengisi kehadiran dengan tanggal yang BERBEDA dari
		// jadwal (lihat AbsensiHelper.setTanggalRealisasi). Dengan meng-OR-kan kriteria realisasi,
		// pertemuan yang DIJADWALKAN di luar rentang tetapi DIREALISASI di dalam rentang — mis.
		// dijadwalkan bulan Mei namun baru terlaksana bulan Juli — tetap ikut terekap sesuai keluhan
		// pengguna. Pertemuan yang terlaksana tepat pada tanggal jadwalnya (kolom realisasi kosong)
		// tetap tertangkap oleh cabang tanggal TERJADWAL, sehingga tidak ada data yang hilang.
		if (mulai != null || sampai != null) {
			Criterion tglTerjadwal;
			if (mulai != null && sampai != null) {
				tglTerjadwal = Restrictions.between("tanggal", mulai, sampai);
			} else if (mulai != null) {
				tglTerjadwal = Restrictions.ge("tanggal", mulai);
			} else {
				tglTerjadwal = Restrictions.le("tanggal", sampai);
			}

			if (Boolean.TRUE.equals(termasukTglRealisasi)) {
				Criterion tglRealisasi;
				if (mulai != null && sampai != null) {
					tglRealisasi = Restrictions.between("tanggalRealisasi", mulai, sampai);
				} else if (mulai != null) {
					tglRealisasi = Restrictions.ge("tanggalRealisasi", mulai);
				} else {
					tglRealisasi = Restrictions.le("tanggalRealisasi", sampai);
				}
				criterionTgl = Restrictions.or(tglTerjadwal, tglRealisasi);
			} else {
				criterionTgl = tglTerjadwal;
			}
		}

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("tanggal")).add(criterionTgl)

				.addOrder(tampilkanPermk ? Order.asc("perkuliahan") : Order.asc("tanggal"))
				.addOrder(tampilkanPermk ? Order.asc("tanggal") : Order.asc("id"))

				.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")

				.add(Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))

				.add(dsn == null ? Restrictions.sqlRestriction("true") : criterion)

				.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.masaPerkuliahan", masaPerkuliahan))

				.add(kelas == null || kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("perkuliahan.kelas", kelas.trim(), MatchMode.EXACT))

				.add(jenisSemester == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								jenisSemester.equals(Perkuliahan.GANJIL) ? "semester%2=1" : "semester%2=0"))

				.add(semesterPendek == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
						: Restrictions.eq("perkuliahan.statusSemesterPendek", semesterPendek))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.jurusan", jurusan))
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jurusan.fakultas", fakultas));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		criteria.add(matkul != null ? Restrictions.eq("perkuliahan.matakuliah", matkul)
				: Restrictions.sqlRestriction("true"));

		List<Pertemuan> pertemuans = criteria.add(matakuliah.trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("matakuliah.kode", matakuliah.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("matakuliah.nama", matakuliah.trim(), MatchMode.ANYWHERE)))
				.list();

		System.out.println("pertemuans => " + pertemuans.size());

		Map<Dosen, List<Pertemuan>> dataPertemuans = new HashMap<Dosen, List<Pertemuan>>();
		Map<Dosen, List<Pertemuan>> dataPertemuansPengganti = new HashMap<Dosen, List<Pertemuan>>();

		for (Pertemuan pertemuan : pertemuans) {
			try {
				if (pertemuan.getAktif()) {
					if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
							&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {

						Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
						List<Dosen> dosens = perkuliahan.populateDosenBuNama();
						Dosen dosenPengganti = null;
						if (pertemuan.getDosenPengganti() != null) {
							dosenPengganti = (Dosen) ConstantValues.ambil(Dosen.class.getName(),
									pertemuan.getDosenPengganti());
							if (dosenPengganti != null) {
								dosens.add(dosenPengganti);
							}
						}

						for (Dosen dosen : dosens) {
							try {
								if (dsn != null && dosen != null && !dosen.getId().equals(dsn.getId())) {
									continue;
								}
								if (dataPertemuans.containsKey(dosen)) {
									dataPertemuans.get(dosen).add(pertemuan);
								} else {
									dataPertemuans.put(dosen, new ArrayList<Pertemuan>());
									dataPertemuans.get(dosen).add(pertemuan);
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:7478");
								// TODO: handle exception
							}
						}

						if (dosenPengganti != null) {
							try {

								if (dsn != null && dosenPengganti != null
										&& !dosenPengganti.getId().equals(dsn.getId())) {
									continue;
								}
								if (dataPertemuans.containsKey(dosenPengganti)) {
									dataPertemuansPengganti.get(dosenPengganti).add(pertemuan);
								} else {
									dataPertemuansPengganti.put(dosenPengganti, new ArrayList<Pertemuan>());
									dataPertemuansPengganti.get(dosenPengganti).add(pertemuan);
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:7496");
								// TODO: handle exception
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:7502");
				// TODO: handle exception
			}
		}

		for (Dosen dosen : dataPertemuans.keySet()) {

			if (tetap == null || dosen.getTetap().equals(tetap)) {

				for (Pertemuan pertemuan : dataPertemuans.get(dosen)) {
					if (pertemuan.getAktif()) {
						if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
								&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {

							if (hanyaYgStatusMasuk
									&& !pertemuan.retreiveAbsensiKode(dosen.getId()).equalsIgnoreCase("M")) {
								continue;
							}

							Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
							map.put("pertemuan", pertemuan);
							map.put("dosen_id", dosen.getId());
							map.put("jumlah_dosen", perkuliahan.getJumlahDosen());
							map.put("perkuliahan_id", perkuliahan.getId());
							if (pertemuan.getDosenPengganti() != null) {
								String nama = (String) session.createCriteria(Dosen.class)
										.add(Restrictions.idEq(pertemuan.getDosenPengganti()))
										.setProjection(Projections.property("nama")).uniqueResult();
								map.put("dosen_pengganti", nama);
							}
							map.put("perkuliahan", perkuliahan.getId());
							map.put("kode_perubahan", (tampilkanPermk ? perkuliahan.getId() : dosen.getId()) + "");

							String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(dosen));
							map.put("link_foto", linkFoto);

							map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()));
							map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
							map.put("waktu_rencana",
									perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
							map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(dosen.getId()) + " - "
									+ pertemuan.retreiveAbsensiSampai(dosen.getId()));
							map.put("catatan", pertemuan.retreiveAbsensiKeterangan(dosen.getId()));

							map.put("nim", dosen.getNidn());
							map.put("nama", dosen.getNama());
							map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
							map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
							map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
							map.put("sks",
									pertemuan.retreiveAbsensiKode(dosen.getId()).equalsIgnoreCase("M")
											? perkuliahan.getMatakuliah().getSks()
											: 0);

							map.put("sks_" + perkuliahan.getMatakuliah().getId(),
									pertemuan.retreiveAbsensiKode(dosen.getId()).equalsIgnoreCase("M")
											? perkuliahan.getMatakuliah().getSks()
											: 0);

							map.put("sks_mk", perkuliahan.getMatakuliah().getSks());

							map.put("jenis_semester",
									perkuliahan.getStatusSemesterPendek() != null
											&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
													? Perkuliahan.SP
													: perkuliahan.getGanjilGenap());

							map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
							map.put("fakultas", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getNama());
							map.put("kelas", perkuliahan.getKelas());
							map.put("nama_jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("semester", perkuliahan.getSemester());
							map.put("nama_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getNama());
							map.put("nip_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getCode());

							map.put("nama_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
							map.put("nidn_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

							map.put("nama_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
							map.put("nidn_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

							map.put("hari", perkuliahan.getHari());
							map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
							map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

							map.put("jenjang", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getJenjang().getNama());
							map.put("jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

							map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(dosen.getId()));

							map.put("status_pertemuan1",
									Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));

							maps.add(map);
						}
					}
				}
			}
		}

		for (Dosen dosen : dataPertemuansPengganti.keySet()) {

			if (tetap == null || dosen.getTetap().equals(tetap)) {

				for (Pertemuan pertemuan : dataPertemuansPengganti.get(dosen)) {
					if (pertemuan.getAktif()) {
						if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
								&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {

							if (hanyaYgStatusMasuk
									&& !pertemuan.retreiveAbsensiKode(dosen.getId()).equalsIgnoreCase("M")) {
								continue;
							}

							Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
							map.put("pertemuan", pertemuan);
							map.put("dosen_id", dosen.getId());
							map.put("jumlah_dosen", perkuliahan.getJumlahDosen());
							map.put("perkuliahan_id", perkuliahan.getId());
							if (pertemuan.getDosenPengganti() != null) {
								String nama = (String) session.createCriteria(Dosen.class)
										.add(Restrictions.idEq(pertemuan.getDosenPengganti()))
										.setProjection(Projections.property("nama")).uniqueResult();
								map.put("dosen_pengganti", nama);
							}
							map.put("perkuliahan", perkuliahan.getId());
							map.put("kode_perubahan", (tampilkanPermk ? perkuliahan.getId() : dosen.getId()) + "");

							String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(dosen));
							map.put("link_foto", linkFoto);

							map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal()));
							map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
							map.put("waktu_rencana",
									perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
							map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(dosen.getId()) + " - "
									+ pertemuan.retreiveAbsensiSampai(dosen.getId()));
							map.put("catatan", pertemuan.retreiveAbsensiKeterangan(dosen.getId()));

							map.put("nim", dosen.getNidn());
							map.put("nama", dosen.getNama());
							map.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
							map.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
							map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
							map.put("sks",
									pertemuan.retreiveAbsensiKode(dosen.getId()).equalsIgnoreCase("M")
											? perkuliahan.getMatakuliah().getSks()
											: 0);

							map.put("sks_" + perkuliahan.getMatakuliah().getId(),
									pertemuan.retreiveAbsensiKode(dosen.getId()).equalsIgnoreCase("M")
											? perkuliahan.getMatakuliah().getSks()
											: 0);

							map.put("sks_mk", perkuliahan.getMatakuliah().getSks());
							map.put("jenis_semester",
									perkuliahan.getStatusSemesterPendek() != null
											&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
													? Perkuliahan.SP
													: perkuliahan.getGanjilGenap());

							map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
							map.put("fakultas", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getNama());
							map.put("kelas", perkuliahan.getKelas());
							map.put("nama_jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("semester", perkuliahan.getSemester());
							map.put("nama_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getNama());
							map.put("nip_kaprodi",
									perkuliahan.getJurusan() == null ? ""
											: perkuliahan.getJurusan().getKaprodi() == null ? ""
													: perkuliahan.getJurusan().getKaprodi().getCode());

							map.put("nama_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
							map.put("nidn_dosen1",
									perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

							map.put("nama_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
							map.put("nidn_dosen2",
									perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

							map.put("hari", perkuliahan.getHari());
							map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
							map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
									: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

							map.put("jenjang", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getJenjang().getNama());
							map.put("jurusan",
									perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
							map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

							map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(dosen.getId()));

							map.put("status_pertemuan1",
									Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));

							maps.add(map);
						}
					}
				}
			}
		}

		return maps;
	}

	/**
	 * Versi SEKOLAH dari {@link #generateParameterMapAbsensiRinciDosen}: rekap rinci
	 * kehadiran MENGAJAR GURU berbasis {@code JadwalPelajaran} / {@code Pertemuan} /
	 * {@code Matapelajaran} / {@code KelasSiswa}. Sengaja memakai KUNCI PETA yang SAMA
	 * dengan versi dosen (nama, nama_matakuliah, kelas, fakultas, sks, dst.) supaya bisa
	 * langsung memakai ulang template Jasper {@code Kehadiran_Dosen} tanpa membuat
	 * template baru; pemetaan: guru&rarr;dosen, mata pelajaran&rarr;matakuliah,
	 * sekolah&rarr;fakultas, jam pelajaran&rarr;sks(1 per sesi masuk), yayasan&rarr;perguruan_tinggi.
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinciGuru(
			ais.database.model.sekolah.Yayasan yayasan, ais.database.model.sekolah.Sekolah sekolah, String kelas,
			String tahunAjaran, String jenisSemester, ais.database.model.sekolah.Guru guruFilter, String matapelajaran,
			Date mulai, Date sampai, Boolean hanyaYgStatusMasuk, Boolean termasukTglRealisasi,
			List<Long> statusPertemuans) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		Criterion criterionTgl = Restrictions.sqlRestriction("true");
		if (termasukTglRealisasi != null && termasukTglRealisasi.booleanValue()) {
			if (mulai != null && sampai != null) {
				criterionTgl = Restrictions.or(Restrictions.between("tanggalRealisasi", mulai, sampai),
						Restrictions.between("tanggal", mulai, sampai));
			} else if (mulai != null) {
				criterionTgl = Restrictions.or(Restrictions.ge("tanggalRealisasi", mulai),
						Restrictions.ge("tanggal", mulai));
			} else if (sampai != null) {
				criterionTgl = Restrictions.or(Restrictions.le("tanggalRealisasi", sampai),
						Restrictions.le("tanggal", sampai));
			}
		} else {
			if (mulai != null && sampai != null) {
				criterionTgl = Restrictions.between("tanggal", mulai, sampai);
			} else if (mulai != null) {
				criterionTgl = Restrictions.ge("tanggal", mulai);
			} else if (sampai != null) {
				criterionTgl = Restrictions.le("tanggal", sampai);
			}
		}

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("tanggal")).add(Restrictions.isNotNull("jadwalPelajaran")).add(criterionTgl)
				.addOrder(Order.asc("jadwalPelajaran")).addOrder(Order.asc("tanggal"))
				.createAlias("jadwalPelajaran", "jp")
				.createAlias("jp.matapelajaran", "mp", org.hibernate.Criteria.LEFT_JOIN)
				.createAlias("jp.kelas", "kl", org.hibernate.Criteria.LEFT_JOIN)
				.createAlias("jp.sekolah", "sk", org.hibernate.Criteria.LEFT_JOIN)
				.add(Restrictions.eq("jp.tahunAjaran", tahunAjaran));

		if (sekolah != null) {
			criteria.add(Restrictions.eq("jp.sekolah", sekolah));
		}
		if (guruFilter != null) {
			criteria.add(Restrictions.or(Restrictions.eq("jp.guru", guruFilter),
					Restrictions.or(Restrictions.eq("jp.guru2", guruFilter),
							Restrictions.or(Restrictions.eq("jp.guru3", guruFilter), Restrictions.or(
									Restrictions.eq("jp.guru4", guruFilter), Restrictions.eq("jp.guru5", guruFilter))))));
		}
		if (matapelajaran != null && !matapelajaran.trim().isEmpty()) {
			criteria.add(Restrictions.or(Restrictions.ilike("mp.nama", matapelajaran.trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("mp.kode", matapelajaran.trim(), MatchMode.ANYWHERE)));
		}
		if (kelas != null && !kelas.trim().isEmpty()) {
			criteria.add(Restrictions.ilike("kl.nama", kelas.trim(), MatchMode.ANYWHERE));
		}

		List<Pertemuan> pertemuans = criteria.list();

		// Kelompokkan per GURU (guru1..guru5 pada jadwal). Filter jenis semester dilakukan di
		// Java (lewat jadwal) agar tidak bergantung pada kolom semester di tabel pertemuan.
		Map<ais.database.model.sekolah.Guru, List<Pertemuan>> dataPertemuans = new java.util.HashMap<ais.database.model.sekolah.Guru, List<Pertemuan>>();
		for (Pertemuan pertemuan : pertemuans) {
			try {
				if (pertemuan.getAktif() == null || !pertemuan.getAktif().booleanValue()) {
					continue;
				}
				// Filter jenis pertemuan hanya berlaku bila daftar status diisi. Bila kosong/null
				// (tidak ada pilihan jenis), SEMUA jenis pertemuan diikutkan.
				if (statusPertemuans != null && !statusPertemuans.isEmpty()
						&& (pertemuan.getStatusPertemuan() == null || pertemuan.getStatusPertemuan().getId() == null
								|| !statusPertemuans.contains(pertemuan.getStatusPertemuan().getId()))) {
					continue;
				}
				ais.database.model.sekolah.JadwalPelajaran jp = pertemuan.getJadwalPelajaran();
				if (jp == null) {
					continue;
				}
				if (jenisSemester != null && jp.getSemester() != null) {
					boolean ganjil = (jp.getSemester().intValue() % 2) == 1;
					boolean mintaGanjil = jenisSemester.equals(ais.database.model.sekolah.JadwalPelajaran.GANJIL);
					if (ganjil != mintaGanjil) {
						continue;
					}
				}
				ais.database.model.sekolah.Guru[] kandidat = new ais.database.model.sekolah.Guru[] { jp.getGuru(),
						jp.getGuru2(), jp.getGuru3(), jp.getGuru4(), jp.getGuru5() };
				for (ais.database.model.sekolah.Guru g : kandidat) {
					if (g == null || g.getId() == null) {
						continue;
					}
					if (guruFilter != null && !g.getId().equals(guruFilter.getId())) {
						continue;
					}
					if (!dataPertemuans.containsKey(g)) {
						dataPertemuans.put(g, new ArrayList<Pertemuan>());
					}
					if (!dataPertemuans.get(g).contains(pertemuan)) {
						dataPertemuans.get(g).add(pertemuan);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:7856");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}

		for (ais.database.model.sekolah.Guru guru : dataPertemuans.keySet()) {
			for (Pertemuan pertemuan : dataPertemuans.get(guru)) {
				String kode = pertemuan.retreiveAbsensiKode(guru.getId());
				if (hanyaYgStatusMasuk != null && hanyaYgStatusMasuk.booleanValue()
						&& (kode == null || !kode.equalsIgnoreCase("M"))) {
					continue;
				}
				ais.database.model.sekolah.JadwalPelajaran jp = pertemuan.getJadwalPelajaran();
				ais.database.model.sekolah.Matapelajaran mp = jp.getMatapelajaran();
				ais.database.model.sekolah.KelasSiswa kl = jp.getKelas();
				ais.database.model.sekolah.Sekolah sk = jp.getSekolah();
				boolean masuk = kode != null && kode.equalsIgnoreCase("M");
				String wm = jp.getWaktuMulai() == null ? "" : jp.getWaktuMulai();
				String ws = jp.getWaktuSelesai() == null ? "" : jp.getWaktuSelesai();

				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
				map.put("pertemuan", pertemuan);
				map.put("dosen_id", guru.getId());
				map.put("jumlah_dosen", 1);
				map.put("perkuliahan_id", jp.getId());
				map.put("perkuliahan", jp.getId());
				map.put("kode_perubahan", jp.getId() + "");
				map.put("link_foto", "");
				map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
						: Common.dateFormat4.get().format(pertemuan.getTanggal()));
				map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
						: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
				map.put("waktu_rencana", wm + " - " + ws);
				map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(guru.getId()) + " - "
						+ pertemuan.retreiveAbsensiSampai(guru.getId()));
				map.put("catatan", pertemuan.retreiveAbsensiKeterangan(guru.getId()));
				map.put("nim", guru.getNip() == null ? "" : guru.getNip());
				map.put("nama", guru.getNama());
				map.put("nama_matakuliah", mp == null ? "" : mp.getNama());
				map.put("kode_matakuliah", mp == null ? "" : mp.getKode());
				map.put("ruang", jp.getRuang() == null ? "" : jp.getRuang().getNama());
				map.put("sks", masuk ? 1 : 0);
				map.put("sks_mk", 1);
				map.put("jenis_semester",
						jp.getSemester() != null && (jp.getSemester().intValue() % 2) == 1
								? ais.database.model.sekolah.JadwalPelajaran.GANJIL
								: ais.database.model.sekolah.JadwalPelajaran.GENAP);
				map.put("tahun_ajaran", jp.getTahunAjaran());
				map.put("fakultas", sk == null ? "" : sk.getNama());
				map.put("kelas", kl == null ? "" : kl.getNama());
				map.put("nama_jurusan", sk == null ? "" : sk.getNama());
				map.put("semester", jp.getSemester());
				map.put("nama_kaprodi", "");
				map.put("nip_kaprodi", "");
				map.put("nama_dosen1", guru.getNama());
				map.put("nidn_dosen1", guru.getNip() == null ? "" : guru.getNip());
				map.put("nama_dosen2", "");
				map.put("nidn_dosen2", "");
				map.put("hari", jp.getHari());
				map.put("jam", wm + " s.d " + ws);
				map.put("waktu", wm + " s.d " + ws);
				map.put("jenjang", "");
				map.put("jurusan", sk == null ? "" : sk.getNama());
				map.put("perguruan_tinggi", yayasan == null ? (sk == null ? "" : sk.getNama()) : yayasan.getNama());
				map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(guru.getId()));
				map.put("status_pertemuan1", Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));
				maps.add(map);
			}
		}
		return maps;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinciMahasiswa(Fakultas fakultas,
			Jurusan jurusan, String kelas, MasaPerkuliahan masaPerkuliahan, String tahunAkademik, String jenisSemester,
			Integer semesterPendek, Integer ekstrakurikuler, Dosen dsn, Mahasiswa mhs, String matakuliah,
			Matakuliah matkul, Date mulai, Date sampai, Boolean hanyaYgStatusMasuk, List<Long> statusPertemuans)
			throws Exception {
		Session session = HibernateUtil.currentSession();
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		Criterion criterion = dsn == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("perkuliahan.dosen1", dsn),
						Restrictions.eq("perkuliahan.dosen2", dsn));

		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen3", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen4", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen5", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen6", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen7", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen8", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen9", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen10", dsn));

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("tanggal"))

				.add(mulai != null ? Restrictions.ge("tanggal", mulai) : Restrictions.sqlRestriction("true"))

				.add(sampai != null ? Restrictions.le("tanggal", sampai) : Restrictions.sqlRestriction("true"))

				.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")

				.add(dsn == null ? Restrictions.sqlRestriction("true") : criterion)

				.add(Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))

				.add(mhs == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"perkuliahan in (select perkuliahan from detailperkuliahan where mahasiswa="
										+ mhs.getId() + " and persetujuan=1)"))

				.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.masaPerkuliahan", masaPerkuliahan))

				.add(kelas == null || kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("perkuliahan.kelas", kelas.trim(), MatchMode.EXACT))

				.add(jenisSemester == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								jenisSemester.equals(Perkuliahan.GANJIL) ? "semester%2=1" : "semester%2=0"))

				.add(semesterPendek == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
						: Restrictions.eq("perkuliahan.statusSemesterPendek", semesterPendek))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.jurusan", jurusan))
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jurusan.fakultas", fakultas));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		criteria.add(matkul != null ? Restrictions.eq("perkuliahan.matakuliah", matkul)
				: Restrictions.sqlRestriction("true"));

		List<Pertemuan> pertemuans = criteria.add(matakuliah.trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("matakuliah.kode", matakuliah.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("matakuliah.nama", matakuliah.trim(), MatchMode.ANYWHERE)))
				.list();

		System.out.println("pertemuans => " + pertemuans.size());

		Map<Mahasiswa, List<Pertemuan>> dataPertemuans = new HashMap<Mahasiswa, List<Pertemuan>>();

		for (Pertemuan pertemuan : pertemuans) {
			if (pertemuan.getAktif()) {
				if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
						&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {
					Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
					Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
								Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
								if (mhs != null && mahasiswa != null && !mahasiswa.getId().equals(mhs.getId())) {
									continue;
								}
								if (dataPertemuans.containsKey(mahasiswa)) {
									dataPertemuans.get(mahasiswa).add(pertemuan);
								} else {
									dataPertemuans.put(mahasiswa, new ArrayList<Pertemuan>());
									dataPertemuans.get(mahasiswa).add(pertemuan);
								}
							}
						}
					}
				}
			}
		}

		for (Mahasiswa mahasiswa : dataPertemuans.keySet()) {

			for (Pertemuan pertemuan : dataPertemuans.get(mahasiswa)) {
				if (pertemuan.getAktif()) {
					if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
							&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {
						if (hanyaYgStatusMasuk
								&& !pertemuan.retreiveAbsensiKode(mahasiswa.getId()).equalsIgnoreCase("M")) {
							continue;
						}

						Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

						Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

						if (pertemuan.getDosenPengganti() != null) {
							Dosen nama = (Dosen) ConstantValues.ambil(Dosen.class.getName(),
									pertemuan.getDosenPengganti());
							map.put("dosen_pengganti", nama == null ? "" : nama.getNama());
						} else if (perkuliahan.getDosen1() != null) {
							map.put("dosen_pengganti", perkuliahan.getDosen1().getNama());
						}
						map.put("jumlah_dosen", perkuliahan.getJumlahDosen());
						map.put("perkuliahan", perkuliahan.getId());
						map.put("kode_perubahan", mahasiswa.getId() + "");

						try {
							FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(),
									LampiranLain.TTD_MAHASISWA, LampiranLain.class);
							if (lampiranLain != null) {
								File file = lampiranLain.ambilFile();
								if (file != null && file.exists()) {
									map.put("ttd_mahasiswa", file.getAbsolutePath());
								}
								file = null;
							}
							lampiranLain = null;
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:8070");
//						e.printStackTrace();
						}

						String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(mahasiswa));
						map.put("link_foto", linkFoto);

						map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
								: Common.dateFormat4.get().format(pertemuan.getTanggal()));
						map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
								: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
						map.put("waktu_rencana", perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
						map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(mahasiswa.getId()) + " - "
								+ pertemuan.retreiveAbsensiSampai(mahasiswa.getId()));
						map.put("catatan", pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId()));

						map.put("nim", mahasiswa.getNim());
						map.put("nama", mahasiswa.getNama());
						// getRuang()/getJurusan() di sekitar sini sudah dijaga null, getMatakuliah()
						// belum -- perkuliahan tanpa matakuliah (mis. matakuliahnya sudah dihapus)
						// membuat SELURUH laporan gagal dicetak. Kini barisnya tetap tercetak
						// dengan kolom matakuliah kosong, sama seperti perlakuan ruang/jurusan.
						map.put("nama_matakuliah",
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());
						map.put("kode_matakuliah",
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());
						map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
						map.put("sks", perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getSks());
						map.put("jenis_semester",
								perkuliahan.getStatusSemesterPendek() != null
										&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
												? Perkuliahan.SP
												: perkuliahan.getGanjilGenap());

						map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
						// getJurusan() dijaga null, getFakultas() di belakangnya tidak: program studi
						// tanpa fakultas induk tetap melempar NPE dan menggagalkan laporan.
						map.put("fakultas", perkuliahan.getJurusan() == null
								|| perkuliahan.getJurusan().getFakultas() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getNama());
						map.put("kelas", perkuliahan.getKelas());
						map.put("nama_jurusan",
								perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
						map.put("semester", perkuliahan.getSemester());
						map.put("nama_kaprodi",
								perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getKaprodi() == null ? ""
												: perkuliahan.getJurusan().getKaprodi().getNama());
						map.put("nip_kaprodi",
								perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getKaprodi() == null ? ""
												: perkuliahan.getJurusan().getKaprodi().getCode());

						map.put("nama_dosen1",
								perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
						map.put("nidn_dosen1",
								perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

						map.put("nama_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
						map.put("nidn_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

						map.put("hari", perkuliahan.getHari());
						map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
								: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
						map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
								: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

						map.put("jenjang", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getJenjang().getNama());
						map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
						map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

						map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(mahasiswa.getId()));

						map.put("status_pertemuan1", Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));

						maps.add(map);
					}
				}
			}
		}

		return maps;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Serializable>> generateParameterMapAbsensiRinciAsisten(Fakultas fakultas,
			Jurusan jurusan, String kelas, MasaPerkuliahan masaPerkuliahan, String tahunAkademik, String jenisSemester,
			Integer semesterPendek, Integer ekstrakurikuler, Dosen dsn, String mhs, String matakuliah,
			Matakuliah matkul, Date mulai, Date sampai, Boolean hanyaYgStatusMasuk, List<Long> statusPertemuans)
			throws Exception {

		Session session = HibernateUtil.currentSession();
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		Criterion criterion = dsn == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("perkuliahan.dosen1", dsn),
						Restrictions.eq("perkuliahan.dosen2", dsn));

		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen3", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen4", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen5", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen6", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen7", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen8", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen9", dsn));
		criterion = Restrictions.or(criterion, Restrictions.eq("perkuliahan.dosen10", dsn));

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("tanggal"))

				.add(mulai != null ? Restrictions.ge("tanggal", mulai) : Restrictions.sqlRestriction("true"))

				.add(sampai != null ? Restrictions.le("tanggal", sampai) : Restrictions.sqlRestriction("true"))

				.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")

				.add(dsn == null ? Restrictions.sqlRestriction("true") : criterion)

				.add(Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))

				.add(mhs == null || mhs.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"perkuliahan in (select a.perkuliahan from mahasiswa_jadi_asisten a inner join mahasiswa b on (a.mahasiswa=b.id) where (b.nama ilike '%"
										+ mhs + "%' or b.nim ilike '%" + mhs + "%') and a.aktif )"))

				.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.masaPerkuliahan", masaPerkuliahan))

				.add(kelas == null || kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("perkuliahan.kelas", kelas.trim(), MatchMode.EXACT))

				.add(jenisSemester == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								jenisSemester.equals(Perkuliahan.GANJIL) ? "semester%2=1" : "semester%2=0"))

				.add(semesterPendek == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
						: Restrictions.eq("perkuliahan.statusSemesterPendek", semesterPendek))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("perkuliahan.jurusan", jurusan))
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jurusan.fakultas", fakultas));

		if (ekstrakurikuler != null && ekstrakurikuler.equals(Perkuliahan.EKSTRA)) {
			criteria.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(Restrictions.eq("matakuliah.extraKulikuler", true));
		} else {
			criteria.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(Restrictions.or(Restrictions.isNull("matakuliah.extraKulikuler"),
							Restrictions.eq("matakuliah.extraKulikuler", false)));
		}

		criteria.add(matkul != null ? Restrictions.eq("perkuliahan.matakuliah", matkul)
				: Restrictions.sqlRestriction("true"));

		List<Pertemuan> pertemuans = criteria.add(matakuliah.trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("matakuliah.kode", matakuliah.trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("matakuliah.nama", matakuliah.trim(), MatchMode.ANYWHERE)))
				.list();

		System.out.println("pertemuans => " + pertemuans.size());

		Map<Mahasiswa, List<Pertemuan>> dataPertemuans = new HashMap<Mahasiswa, List<Pertemuan>>();

		for (Pertemuan pertemuan : pertemuans) {
			if (pertemuan.getAktif()) {
				if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
						&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {
					Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
					List<Mahasiswa> mahasiswas = session.createCriteria(MahasiswaJadiAsisten.class)
							.createAlias("mahasiswa", "mahasiswa")
							.add(mhs == null || mhs.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("mahasiswa.nim", mhs.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("mahasiswa.nama", mhs.trim(), MatchMode.ANYWHERE)))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.property("mahasiswa"))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

					for (Mahasiswa mahasiswa : mahasiswas) {
						if (dataPertemuans.containsKey(mahasiswa)) {
							dataPertemuans.get(mahasiswa).add(pertemuan);
						} else {
							dataPertemuans.put(mahasiswa, new ArrayList<Pertemuan>());
							dataPertemuans.get(mahasiswa).add(pertemuan);
						}
					}
				}
			}
		}

		for (Mahasiswa mahasiswa : dataPertemuans.keySet()) {

			for (Pertemuan pertemuan : dataPertemuans.get(mahasiswa)) {
				if (pertemuan.getAktif()) {
					if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getId() != null
							&& statusPertemuans.contains(pertemuan.getStatusPertemuan().getId())) {
						if (hanyaYgStatusMasuk
								&& !pertemuan.retreiveAbsensiKode(mahasiswa.getId()).equalsIgnoreCase("M")) {
							continue;
						}

						Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

						Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

						if (pertemuan.getDosenPengganti() != null) {
							Dosen nama = (Dosen) ConstantValues.ambil(Dosen.class.getName(),
									pertemuan.getDosenPengganti());
							map.put("dosen_pengganti", nama == null ? "" : nama.getNama());
						} else if (perkuliahan.getDosen1() != null) {
							map.put("dosen_pengganti", perkuliahan.getDosen1().getNama());
						}
						map.put("jumlah_dosen", perkuliahan.getJumlahDosen());
						map.put("kode_perubahan", mahasiswa.getId() + "");

						String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(mahasiswa));
						map.put("link_foto", linkFoto);

						map.put("tanggal_rencana", pertemuan.getTanggal() == null ? ""
								: Common.dateFormat4.get().format(pertemuan.getTanggal()));
						map.put("tanggal_realisasi", pertemuan.getTanggalRealisasi() == null ? ""
								: Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi()));
						map.put("waktu_rencana", perkuliahan.getWaktuMulai() + " - " + perkuliahan.getWaktuSelesai());
						map.put("waktu_realisasi", pertemuan.retreiveAbsensiMulai(mahasiswa.getId()) + " - "
								+ pertemuan.retreiveAbsensiSampai(mahasiswa.getId()));
						map.put("catatan", pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId()));

						map.put("nim", mahasiswa.getNim());
						map.put("nama", mahasiswa.getNama());
						// getRuang()/getJurusan() di sekitar sini sudah dijaga null, getMatakuliah()
						// belum -- perkuliahan tanpa matakuliah (mis. matakuliahnya sudah dihapus)
						// membuat SELURUH laporan gagal dicetak. Kini barisnya tetap tercetak
						// dengan kolom matakuliah kosong, sama seperti perlakuan ruang/jurusan.
						map.put("nama_matakuliah",
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());
						map.put("kode_matakuliah",
								perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getKode());
						map.put("ruang", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
						map.put("sks", perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getSks());
						map.put("jenis_semester",
								perkuliahan.getStatusSemesterPendek() != null
										&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
												? Perkuliahan.SP
												: perkuliahan.getGanjilGenap());

						map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
						// getJurusan() dijaga null, getFakultas() di belakangnya tidak: program studi
						// tanpa fakultas induk tetap melempar NPE dan menggagalkan laporan.
						map.put("fakultas", perkuliahan.getJurusan() == null
								|| perkuliahan.getJurusan().getFakultas() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getNama());
						map.put("kelas", perkuliahan.getKelas());
						map.put("nama_jurusan",
								perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
						map.put("semester", perkuliahan.getSemester());
						map.put("nama_kaprodi",
								perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getKaprodi() == null ? ""
												: perkuliahan.getJurusan().getKaprodi().getNama());
						map.put("nip_kaprodi",
								perkuliahan.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getKaprodi() == null ? ""
												: perkuliahan.getJurusan().getKaprodi().getCode());

						map.put("nama_dosen1",
								perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
						map.put("nidn_dosen1",
								perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());

						map.put("nama_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama());
						map.put("nidn_dosen2",
								perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNidn());

						map.put("hari", perkuliahan.getHari());
						map.put("jam", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
								: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));
						map.put("waktu", perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() ? ""
								: (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai()));

						map.put("jenjang", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getJenjang().getNama());
						map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
						map.put("perguruan_tinggi", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
										: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama());

						map.put("status_kehadiran", pertemuan.retreiveAbsensiNama(mahasiswa.getId()));

						map.put("status_pertemuan1", Common.getBahasaConfig(pertemuan.getStatusPertemuan().getNama()));

						maps.add(map);
					}
				}
			}
		}

		return maps;
	}

	public static void cetakItemBiaya(Integer angkatan, String program, JenisKegiatan jenisKegiatan, Jenjang jenjang,
			Jurusan jurusan, JenisSeleksi jenisSeleksi, String wargaNegara, StatusMahasiswa statusMahasiswa,
			String semesterMulai, StatusAwalMahasiswa statusAwalMahasiswa, Paket paket,
			GelombangPendaftaran gelombangPendaftaran, String nilaiTambahan1, String nilaiTambahan2,
			String nilaiTambahan3) throws Exception {

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("nilaiTambahan1", nilaiTambahan1 == null ? "" : nilaiTambahan1);
		parameters.put("nilaiTambahan2", nilaiTambahan2 == null ? "" : nilaiTambahan2);
		parameters.put("nilaiTambahan3", nilaiTambahan3 == null ? "" : nilaiTambahan3);

		parameters.put("angkatan", angkatan);
		parameters.put("program", program);
		parameters.put("semester_mulai", semesterMulai);
		parameters.put("warganegara", wargaNegara);
		parameters.put("jenisSeleksi", jenisSeleksi == null || jenisSeleksi.getId() == null ? -1L : jenisSeleksi.getId());
		parameters.put("paket", paket == null || paket.getId() == null ? -1L : paket.getId());
		parameters.put("jenisKegiatan", jenisKegiatan == null || jenisKegiatan.getId() == null ? -1L : jenisKegiatan.getId());
		parameters.put("statusMahasiswa", statusMahasiswa == null || statusMahasiswa.getId() == null ? -1L : statusMahasiswa.getId());
		parameters.put("statusAwalMahasiswa", statusAwalMahasiswa == null || statusAwalMahasiswa.getId() == null ? -1L : statusAwalMahasiswa.getId());
		parameters.put("jenjang", jenjang == null || jenjang.getId() == null ? -1L : jenjang.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());

		parameters.put("gelombangPendaftaran", gelombangPendaftaran == null || gelombangPendaftaran.getId() == null ? -1L : gelombangPendaftaran.getId());

		parameters.put("nama_jenis_biaya", jenisKegiatan.getNamaKegiatan());
		parameters.put("nama_jurusan", jurusan == null ? "Semua" : jurusan.getNama());
		parameters.put("nama_jenjang", jenjang == null ? "Semua" : jenjang.getNama());
		parameters.put("nama_status_mahasiswa", statusMahasiswa.getNama());
		parameters.put("nama_status_awal_mhs", statusAwalMahasiswa.getNama());
		parameters.put("nama_jenis_seleksi", jenisSeleksi == null ? "" : jenisSeleksi.getNama());
		parameters.put("nama_paket", paket == null ? "" : paket.getNama());

		parameters.put("jenisKegiatanMax", jenisKegiatan == null ? -1 : jenisKegiatan.getMaxSmt());
		parameters.put("jenisKegiatanMin", jenisKegiatan == null ? -1 : jenisKegiatan.getMinSmt());

		System.out.println("paket = " + paket + ", parameters = " + parameters);

		Report.generatePDFReport(Report.PDF, parameters, "Detail_Biaya", ais.ui.util.WaktuUtil.getDate());
	}

	public static void cetakNilai(final Mahasiswa mahasiswa, final Integer semester, final Integer tahapan,
			final Integer semesterPendek, final boolean remedial, String tahunAjaran) throws Exception {

		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null) {
			List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(SyaratUjian.class).add(Restrictions.eq("nilai", true))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					SyaratUjian.class);

			System.out.println("syaratUjians => " + syaratUjians);

			for (SyaratUjian syaratUjian : syaratUjians) {
				SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Cetak KHS", warnings);
			}
		}
		if (!warnings.isEmpty()) {

			String w = "";
			for (String wa : warnings) {
				w += w.isEmpty() ? wa : "\n\n" + wa;
			}

			MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			boolean boleh_cetak_khs = Common.bolehKonfigurasi("mahasiswa_boleh_melihat_khs_sendiri");
			try {
				if (!boleh_cetak_khs) {

					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu Mahasiswa untuk sementara tidak diperkenankan mencetak Kartu Hasil Studi (KHS) secara mandiri. Langkah yang dapat dilakukan: (1) hubungi bagian administrasi akademik untuk memperoleh KHS Anda; (2) sampaikan Nomor Induk Mahasiswa kepada petugas; (3) hubungi admin apabila memerlukan informasi lebih lanjut.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

					return;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
			}
		}

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "prodi"))
				.setMaxResults(1).uniqueResult();

		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
				.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).uniqueResult();

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(),
				semesterPendek);

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRSBerikutNya = Common
				.getIpkUntukPengambilanKRS(mahasiswa, semester + 1, mahasiswa.getTahunangkatan(), fakultas, jurusan,
						mahasiswa.getProgram(), semesterPendek);

		final Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		try {
			FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA,
					LampiranLain.class);
			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				if (file != null && file.exists()) {
					parameters.put("ttd_mahasiswa", file.getAbsolutePath());
				}
				file = null;
			}
			lampiranLain = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:8472");
//			e.printStackTrace();
		}
		parameters.put("max_sks", pembatasanNilaiIPKUntukPengambilanKRS == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_next", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_berikut", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());
		parameters.put("remedial", remedial);
		parameters.put("semester", semester);
		parameters.put("semesterNext", semester);
		parameters.put("tahapan", tahapan);
		parameters.put("tahun_ajaran", tahunAjaran);
		parameters.put("pembantu_dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa.getId());
		// parameters.put("nip", staffDekan.getNip());
		parameters.put("tanggal", ais.ui.util.WaktuUtil.getDate());
		parameters.put("namamahasiswa", mahasiswa.getNama());
		parameters.put("namafakultas", mahasiswa.getJurusan().getFakultas().getNama());

		parameters.put("id_kaprodi",
				mahasiswa.getJurusan().getKaprodi() == null ? -1L : mahasiswa.getJurusan().getKaprodi().getId());
		parameters.put("id_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? -1L
				: mahasiswa.getJurusan().getFakultas().getDekan().getId());

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "2-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());

		parameters.put("semester_pendek", semesterPendek);

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, false);

		if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
			krsMahasiswa.getDosenPa().putPhoto(parameters);
		}
		parameters.put("nidndosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
		parameters.put("kodedosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());

		mahasiswa.putPhoto(parameters);
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));

		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();

		if (semester > 1) {
			Double iplast = Common.ipTerakhir(mahasiswa, semester);
			parameters.put("ip_sebelumnya", iplast);
		}
		parameters.put("ipk", ipkmhs);
		parameters.put("ips", ipmhs);
		parameters.put("sksk", sksmhs);
		parameters.put("sks", sksmhss);

		krsMahasiswa = semester <= 1 ? null
				: Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1,
						tahapan == null || tahapan.equals(0) ? null : tahapan - 1, semesterPendek);

		ipmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIps();
		ipkmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIpk();

		sksmhss = krsMahasiswa == null ? 0 : krsMahasiswa.getSksYangDiambil();
		sksmhs = krsMahasiswa == null ? 0 : krsMahasiswa.getSksk();

		parameters.put("ipk_1", ipkmhs);
		parameters.put("ips_1", ipmhs);
		parameters.put("sksk_1", sksmhs);
		parameters.put("sks_1", sksmhss);

		parameters.put("ip_kumulatif_1", ipkmhs);

		parameters.put("ip_semester_1", ipmhs);

		if (Common.bolehKonfigurasi("tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai", mahasiswa.hitungNilaiSampaiSemester(semester, null, null, true));
			if (semester > 1) {
				parameters.put("total_nilai_1", mahasiswa.hitungNilaiSampaiSemester(semester - 1, null, null, true));
			}
		}

		String khs = "Kartu_Hasil_Studi";
		if (Common.bolehKonfigurasi("khs_dipisahkan_tiap_jenjang", Konfigurasi.TIDAK_AKTIF)) {
			khs = "Kartu_Hasil_Studi_" + mahasiswa.getJenjang().getId();
			parameters.put("nama_laporan", khs);

		}

		if (Common.bolehKonfigurasi("tampilkan_total_mutu_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai_mutu", mahasiswa.hitungMutuSampaiSemester(semester, null, null, true));
			parameters.put("total_nilai_ip", mahasiswa.hitungNilaiIpSampaiSemester(semester, null, null, true));
		}

//		System.out.println("parameters => " + parameters);

		boolean konversiMasuk = Common.bolehKonfigurasi("masukkan_mk_konversi_di_khs", Konfigurasi.TIDAK_AKTIF);
		LaporanKHS.initParametermapKhs(parameters, semesterPendek, remedial, mahasiswa, semester, tahapan, krsMahasiswa,
				konversiMasuk, false);

		Report.generatePDFReport(Report.PDF, parameters, khs, ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({})
	public static Map<String, Object> generateParameterKrs(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean remedial, boolean uts, boolean uas) throws Exception {
		Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();

		try {
			FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA,
					LampiranLain.class);
			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				if (file != null && file.exists()) {
					parameters.put("ttd_mahasiswa", file.getAbsolutePath());
				}
				file = null;
			}
			lampiranLain = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:8617");
//			e.printStackTrace();
		}

		if (semester != null && semester > 1) {
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, tahapan, null);

			Double ipmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIps();
			Double ipkmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIpk();

			Integer sksmhss = krsMahasiswa == null ? 0 : krsMahasiswa.getSksYangDiambil();
			Integer sksmhs = krsMahasiswa == null ? 0 : krsMahasiswa.getSksk();

			parameters.put("ipk_sebelumnya", ipkmhs);
			parameters.put("ips_sebelumnya", ipmhs);
			parameters.put("sksk_sebelumnya", sksmhs);
			parameters.put("sks_sebelumnya", sksmhss);
		} else {
			parameters.put("ipk_sebelumnya", 0.0);
			parameters.put("ips_sebelumnya", 0.0);
			parameters.put("sksk_sebelumnya", 0);
			parameters.put("sks_sebelumnya", 0);
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, null);
		Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "");
		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs", 1, "mahasiswa");
		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) HibernateUtil.currentSession()
				.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).uniqueResult();
		mahasiswa.putPhoto(parameters);
		parameters.put("remedial", remedial);
		parameters.put("semester", semester);
		parameters.put("semesterNext", semester);
		parameters.put("tahapan", tahapan);

		parameters.put("mahasiswa", mahasiswa.getId());
		parameters.put("tanggal", dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Jurusan jurusan = mahasiswa.getJurusan();
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		parameters.put("semester_pendek", semesterPendek);
		parameters.put("namamahasiswa", mahasiswa.getNama());
		parameters.put("namafakultas", mahasiswa.getJurusan().getFakultas().getNama());
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? ""
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());

		if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
			krsMahasiswa.getDosenPa().putPhoto(parameters);
		}
		parameters.put("nipdosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
		parameters.put("kodedosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());
		mahasiswa.putPhoto(parameters);

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan().getFakultas(),
				mahasiswa.getJurusan(), mahasiswa.getProgram(), semesterPendek);
		Double minip = (double) (pembatasanNilaiIPKUntukPengambilanKRS == null
				? PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS()
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());
		parameters.put("maksimum_sks", minip);

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "1-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());

		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();

		if (semester > 1) {
			Double iplast = Common.ipTerakhir(mahasiswa, semester);
			parameters.put("ip_sebelumnya", iplast);
		}
		parameters.put("ip", ipkmhs);
		parameters.put("ipk", ipkmhs);
		parameters.put("ips", ipmhs);
		parameters.put("sksk", sksmhs);
		parameters.put("sks", sksmhss);

		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

		if (Common.bolehKonfigurasi("tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai", mahasiswa.hitungNilaiSampaiSemester(semester, null, null, true));
			if (semester > 1) {
				parameters.put("total_nilai_1", mahasiswa.hitungNilaiSampaiSemester(semester - 1, null, null, true));
			}
		}
		parameters.put("sks", sksmhss);

		if (Common.bolehKonfigurasi("saat_cetak_krs_tidak_tampil_export", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("tidak_tampil_pilihan_export", "true");
		}

		if (Common.bolehKonfigurasi("krs_ambil_dari_parameter")) {

			boolean konversiMasuk = Common.bolehKonfigurasi("masukkan_mk_konversi_di_krs", Konfigurasi.TIDAK_AKTIF);
			parameters.put("maps", CommonReportHelper.generateMap(mahasiswa, semester, tahapan, semesterPendek,
					remedial, krsMahasiswa, false, konversiMasuk, uts, uas, null));
		}

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Map> generateMap(Mahasiswa mahasiswa, Integer semester, Integer tahapan, Integer semesterPendek,
			boolean remedial, KrsMahasiswa krsMahasiswa, boolean infopertemuan, boolean termsukKonversi, boolean uts,
			boolean uas, Set<Long> longsHasilTidak) throws Exception {
		if (krsMahasiswa != null)
			krsMahasiswa.masukkanData("cetak_krs");
		List<Map> maps = new ArrayList<Map>();

		List<Long> detailperkuliahansDataSebelumnya = Common.getDetailperkuliahans(mahasiswa, semester - 1, tahapan,
				null, semesterPendek, remedial, false, false, false);

		boolean jikaTidakKetemuJadwalUjianmakaTidakTampil = Common.bolehKonfigurasi("jika_tidak_ketemu_jadwal_ujian_maka_tidak_tampil", Konfigurasi.TIDAK_AKTIF) && (uts || uas);

		boolean absensi_urut_berdasarkan_nim = Common.bolehKonfigurasi("absensi_urut_berdasarkan_nim");

		List<Object[]> selectedPertemuans = HibernateUtil.currentSession().createCriteria(DetailKelasPertemuan.class)
				.createAlias("kelasPertemuan", "kelasPertemuan")
				.setProjection(Projections.projectionList().add(Projections.property("kelasPertemuan.ruang.id"))
						.add(Projections.property("kelasPertemuan.pertemuan.id")))
				.add(Restrictions.isNotNull("kelasPertemuan.ruang"))
				.add(Restrictions.isNotNull("kelasPertemuan.pertemuan")).add(Restrictions.eq("mahasiswa", mahasiswa))
				.list();
		System.out
				.println("selectedPertemuans -> " + selectedPertemuans.size() + " longsHasilTidak " + longsHasilTidak);
		Map<Long, Long> ruanganPertemuans = new HashMap<Long, Long>();
		for (Object[] objects : selectedPertemuans) {
			try {
				ruanganPertemuans.put(((Number) objects[1]).longValue(), ((Number) objects[0]).longValue());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:8773");
			}
		}

		List<Long> detailperkuliahansData = Common.getDetailperkuliahans(mahasiswa, semester, tahapan, null,
				semesterPendek, remedial, false, false, false);
		for (Long detailperkuliahanid : detailperkuliahansData) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null
					&& (termsukKonversi || (!termsukKonversi && detailperkuliahan.getMatakuliahKonversi() == null))) {

				if (longsHasilTidak != null && longsHasilTidak.contains(detailperkuliahanid)) {
					continue;
				}

				Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();

				Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() != null
						? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();

				// KE-FIX (NullPointerException matakuliah.getTerdapatUas()/getTerdapatUts()):
				// detailperkuliahan yatim (tidak punya Perkuliahan ATAUPUN MatakuliahKonversi --
				// data tidak konsisten, mis. Perkuliahan induknya sudah dihapus) membuat matakuliah
				// di atas bernilai null; baris ini sebelumnya langsung memanggil method di atasnya
				// tanpa jaga-jaga. Lewati baris data yang tidak konsisten ini (sama seperti guard
				// longsHasilTidak di atas), bukan meng-crash seluruh proses cetak KRS mahasiswa.
				if (matakuliah == null) {
					continue;
				}
				if (uas && !matakuliah.getTerdapatUas()) {
					continue;
				}
				if (uts && !matakuliah.getTerdapatUts()) {
					continue;
				}

				// KE-FIX (NPE potensial): perkuliahan bisa null di sini (detailperkuliahan yatim yang
				// hanya punya matakuliahKonversi -- lihat catatan guard "matakuliah == null" di atas).
				// perkuliahan.ambilDetailperkuliahan(...) sebelumnya dipanggil tanpa jaga-jaga; noUrut
				// default 1 (sama seperti perlakuan "periode" null di bawah) bila perkuliahan null.
				Collection<Long> detailperkuliahans = perkuliahan == null ? new ArrayList<Long>()
						: perkuliahan.ambilDetailperkuliahan(null, null, "", !absensi_urut_berdasarkan_nim, false);
				int noUrut = 1;
				for (Long detailperkuliahanidAbsen : detailperkuliahans) {
					if (detailperkuliahanidAbsen.equals(detailperkuliahanid)) {
						break;
					}
					noUrut++;
				}

				Map map = new java.util.HashMap();
				mahasiswa.putPhoto(map);
				map.put("noUrut", noUrut);
				map.put("periode", perkuliahan == null ? "SP" : perkuliahan.getGanjilGenap());

				map.put("id_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? -1L
						: mahasiswa.getJurusan().getPegawai1().getId());

				map.put("jenis_pejabat_prodi_1", mahasiswa.getJurusan().getLabelPejabat1());
				map.put("nama_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? ""
						: mahasiswa.getJurusan().getPegawai1().getNama());
				map.put("nip_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? ""
						: mahasiswa.getJurusan().getPegawai1().getCode());

				map.put("id_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? -1L
						: mahasiswa.getJurusan().getPegawai2().getId());
				map.put("jenis_pejabat_prodi_2", mahasiswa.getJurusan().getLabelPejabat2());
				map.put("nama_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? ""
						: mahasiswa.getJurusan().getPegawai2().getNama());
				map.put("nip_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? ""
						: mahasiswa.getJurusan().getPegawai2().getCode());

				map.put("id_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? -1L
						: mahasiswa.getJurusan().getPegawai3().getId());
				map.put("jenis_pejabat_prodi_3", mahasiswa.getJurusan().getLabelPejabat3());
				map.put("nama_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? ""
						: mahasiswa.getJurusan().getPegawai3().getNama());
				map.put("nip_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? ""
						: mahasiswa.getJurusan().getPegawai3().getCode());

				map.put("id_pejabat_fakultas_1", mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
						: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());

				map.put("jenis_pejabat_fakultas_1", mahasiswa.getJurusan().getFakultas().getLabelPejabat1());
				map.put("nama_pejabat_fakultas_1", mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPegawai1().getNama());
				map.put("nip_pejabat_fakultas_1", mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPegawai1().getCode());

				map.put("id_pejabat_fakultas_2", mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
						: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());
				map.put("jenis_pejabat_fakultas_2", mahasiswa.getJurusan().getFakultas().getLabelPejabat2());
				map.put("nama_pejabat_fakultas_2", mahasiswa.getJurusan().getFakultas().getPegawai2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPegawai2().getNama());
				map.put("nip_pejabat_fakultas_2", mahasiswa.getJurusan().getFakultas().getPegawai2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPegawai2().getCode());

				map.put("id_pejabat_fakultas_3", mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
						: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());
				map.put("jenis_pejabat_fakultas_3", mahasiswa.getJurusan().getFakultas().getLabelPejabat3());
				map.put("nama_pejabat_fakultas_3", mahasiswa.getJurusan().getFakultas().getPegawai3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPegawai3().getNama());
				map.put("nip_pejabat_fakultas_3", mahasiswa.getJurusan().getFakultas().getPegawai3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPegawai3().getCode());

				map.put("program", detailperkuliahan.getPerkuliahan() == null ? ""
						: detailperkuliahan.getPerkuliahan().getProgram());
				map.put("persetujuan", detailperkuliahan.getPersetujuan());
				map.put("nama", mahasiswa.getNama());
				map.put("tahunangkatan", mahasiswa.getTahunangkatan());
				map.put("nim", mahasiswa.getNim());
				map.put("namamahasiswa", mahasiswa.getNama());

				map.put("kode_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? -1L
						: krsMahasiswa.getDosenPa().getMycode());
				map.put("id_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? -1L
						: krsMahasiswa.getDosenPa().getId());
				map.put("id_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? -1L
						: mahasiswa.getJurusan().getKaprodi().getId());
				map.put("id_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? -1L
						: mahasiswa.getJurusan().getFakultas().getDekan().getId());
				map.put("id_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? -1L
						: mahasiswa.getJurusan().getFakultas().getPudek1().getId());
				map.put("id_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? -1L
						: mahasiswa.getJurusan().getFakultas().getPudek2().getId());
				map.put("id_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? -1L
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getId());

				map.put("jurusan", mahasiswa.getJurusan().getNama());
				map.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
				map.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
				map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());
				map.put("semester", semester);

				map.put("semester_pk", perkuliahan == null ? null : perkuliahan.getSemester());
				map.put("tempatlahir", mahasiswa.getTempatlahir());
				map.put("tanggallahir", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat2.get().format(mahasiswa.getTanggallahir()));
				map.put("tanggallahir_1", mahasiswa.getTanggallahir() == null ? ""
						: Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));
				map.put("tahun_ajaran", detailperkuliahan.getTahunAkademik());
				map.put("kode_mata_kuliah", matakuliah.getKode());
				map.put("mata_kuliah", matakuliah.getNama());
				map.put("mata_kuliah_en", matakuliah.getNamaEn());

				map.put("sks", matakuliah.getSks());
				map.put("hari", perkuliahan == null ? "" : perkuliahan.getHari());
				map.put("waktu_mulai", perkuliahan == null ? "" : perkuliahan.getWaktuMulai());
				map.put("waktu_selesai", perkuliahan == null ? "" : perkuliahan.getWaktuSelesai());
				map.put("kelas", perkuliahan == null ? "" : perkuliahan.getKelas());
				map.put("ruang", perkuliahan == null || perkuliahan.getRuang() == null ? ""
						: perkuliahan.getRuang().getKodeRuangan() + " - " + perkuliahan.getRuang().getNama());
				map.put("ruangan", perkuliahan == null || perkuliahan.getRuang() == null ? ""
						: perkuliahan.getRuang().getKodeRuangan() + " - " + perkuliahan.getRuang().getNama());
				map.put("dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getNama());
				map.put("dosenpa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getNama());

				map.put("nip_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getCode());
				map.put("nidn_dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
						: krsMahasiswa.getDosenPa().getNidn());

				map.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNama());
				map.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getCode());
				map.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNidn());

				map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
				map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
				map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

				map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
				map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
				map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

				map.put("nama_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
				map.put("nip_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
				map.put("nidn_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

				List<Dosen> dosens = perkuliahan.populateDosenBuNama();
				int indexDosen = 1;
				for (Dosen d : dosens) {
					map.put("dosen_id_" + indexDosen, d.getId());
					map.put("dosen_nama_" + indexDosen, d.getNama());
					map.put("dosen_kode_" + indexDosen, d.getMycode());
					map.put("dosen_nidn_" + indexDosen, d.getNidn());
					map.put("dosen_nuptk_" + indexDosen, d.getNuptk());
					map.put("dosen_nip_" + indexDosen, d.getCode());
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(d));
					map.put("foto_dosen_" + indexDosen, url);
					indexDosen++;
				}

				map.put("dosen", perkuliahan == null ? "" : perkuliahan.ambilNamaDosens());
				map.put("merupakan_paralel", perkuliahan == null ? false : perkuliahan.getMerupakan_paralel());
				map.put("nama_perguruan_tinggi", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				map.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
				map.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
				map.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
				map.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

				map.put("perkuliahandimulai", perkuliahan == null ? null : perkuliahan.getPerkuliahanDimulai());
				map.put("perkuliahansampai", perkuliahan == null ? null : perkuliahan.getPerkuliahanSampai());

				map.put("catatan", krsMahasiswa == null ? "" : krsMahasiswa.getCatatan());
				boolean ada = false;
				if (uts) {
					List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();

					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							if (pertemuan.getStatusPertemuan() != null
									&& pertemuan.getStatusPertemuan().getNama() != null
									&& pertemuan.getTanggal() != null
									&& pertemuan.getStatusPertemuan().getNama().toLowerCase().contains("uts")) {
								map.put("tanggal", pertemuan.getTanggal());
								map.put("tanggal_ujian", pertemuan.getTanggal());
								map.put("waktu", pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai());
								map.put("catatan", pertemuan.getCatatan());
								map.put("ruangan", pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama());

								Long ruangId = ruanganPertemuans.get(pertemuan.getId());
								if (ruangId != null) {
									Ruang ruang = (Ruang) ConstantValues.ambil(Ruang.class.getName(), ruangId);
									if (ruang != null) {
										map.put("ruangan", ruang.getNama());
									}
								}
								ada = true;
							}
						}
					}
					pertemuans = null;

					if (!ada) {
						map.put("tanggal", null);
						map.put("waktu", perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai());
						map.put("catatan", "Jadwal UTS belum dibuat / tidak ada UTS");
						map.put("ruangan", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
					}
				}

				if (uas) {
					List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();

					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getAktif()) {
							if (pertemuan.getStatusPertemuan() != null
									&& pertemuan.getStatusPertemuan().getNama() != null
									&& pertemuan.getTanggal() != null
									&& pertemuan.getStatusPertemuan().getNama().toLowerCase().contains("uas")) {
								map.put("tanggal", pertemuan.getTanggal());
								map.put("tanggal_ujian", pertemuan.getTanggal());
								map.put("waktu", pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai());
								map.put("catatan", pertemuan.getCatatan());
								map.put("ruangan", pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama());

								Long ruangId = ruanganPertemuans.get(pertemuan.getId());
								if (ruangId != null) {
									Ruang ruang = (Ruang) ConstantValues.ambil(Ruang.class.getName(), ruangId);
									if (ruang != null) {
										map.put("ruangan", ruang.getNama());
									}
								}

								ada = true;
							}
						}
					}
					pertemuans = null;
					if (!ada) {
						map.put("tanggal", null);
						map.put("waktu", perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai());
						map.put("catatan", "Jadwal UAS belum dibuat / tidak ada UAS");
						map.put("ruangan", perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
					}
				}

				boolean ngulang = false;
				Integer ngulang_smt = 0;
				for (Long ngulangDid : detailperkuliahansDataSebelumnya) {
					Detailperkuliahan ngulangD = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, ngulangDid.toString());
					if (ngulangD != null) {
						Matakuliah matakuliah1 = ngulangD.getPerkuliahan() != null
								? ngulangD.getPerkuliahan().getMatakuliah()
								: ngulangD.getMatakuliahKonversi();
						if (matakuliah1 != null && matakuliah != null
								&& matakuliah1.getKode().equalsIgnoreCase(matakuliah.getKode())) {
							ngulang = true;
							ngulang_smt = ngulangD.getSemester();
						}
					}
				}
				map.put("ngulang", ngulang);
				map.put("ngulang_smt", ngulang_smt);

				map.put("catatan_khs", krsMahasiswa == null ? "" : krsMahasiswa.getCatatanKhs());
				map.put("keteranganjadwal", perkuliahan == null ? null : perkuliahan.getKeteranganJadwal());

				if (jikaTidakKetemuJadwalUjianmakaTidakTampil && !ada) {
					System.out.println("Tidak ada jadwal " + perkuliahan);
				} else {
					maps.add(map);
				}
			}
		}

		if (uts || uas) {
			Comparator<Map> mapComparator = new Comparator<Map>() {
				public int compare(Map m1, Map m2) {
					try {
						String tgl1 = Common.dateFormat8.get().format(m1.get("tanggal")) + "-" + m1.get("waktu");
						String tgl2 = Common.dateFormat8.get().format(m2.get("tanggal")) + "-" + m2.get("waktu");
						return tgl1.compareTo(tgl2);
					} catch (Exception e) {
						return 0;
					}
				}
			};

			Collections.sort(maps, mapComparator);
		}

		detailperkuliahansData = null;
		return maps;
	}

	public static void cetakKRS(final Mahasiswa mahasiswa, final Integer semester, final Integer tahapan,
			final Integer semesterPendek, final boolean remedial) throws Exception {

		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null) {
			List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(SyaratUjian.class).add(Restrictions.eq("krs", true))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					SyaratUjian.class);

			System.out.println("syaratUjians => " + syaratUjians);

			for (SyaratUjian syaratUjian : syaratUjians) {
				SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Cetak KRS", warnings);
			}
		}
		if (!warnings.isEmpty()) {

			String w = "";
			for (String wa : warnings) {
				w += w.isEmpty() ? wa : "\n\n" + wa;
			}

			MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (Common.bolehKonfigurasi("saat_cetak_krs_harus_telah_disetujui", Konfigurasi.TIDAK_AKTIF)) {

			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
			int jml = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)) {
						if ((remedial && detailperkuliahan.getPerkuliahan().getMerupakanRemedial())
								|| (!remedial && !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {

							if ((ConstantValues.aktifkanTahapan && tahapan != null && tahapan > 0)
									? detailperkuliahan.getTahap().equals(tahapan)
									: detailperkuliahan.getSemester().equals(semester)) {

								if ((semesterPendek == null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
										|| (semesterPendek != null
												&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
												&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
														.equals(semesterPendek))) {
									jml++;
								}

							}

						}
					}
				}
			}
			detailperkuliahans = null;

			if (jml > 0) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Bapak/Ibu belum dapat mencetak Kartu Rencana Studi (KRS) karena masih terdapat {V1} perkuliahan yang belum disetujui. Langkah yang dapat dilakukan: (1) hubungi Dosen Pembimbing Akademik untuk memperoleh persetujuan perkuliahan; (2) pastikan seluruh perkuliahan telah disetujui; (3) ulangi proses pencetakan KRS.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, jml);
				return;
			}
		}

		Map<String, Object> p1 = generateParameterKrs(mahasiswa, semester, tahapan, semesterPendek, remedial, false,
				false);
		Report.generatePDFReport(Report.PDF, new Map[] { p1, p1 }, new String[] { "Cetak_KRS_Mahasiswa" },
				new String[] { "KRS Mahasiswa" }, ais.ui.util.WaktuUtil.getDate());

	}

	public static void cetakUTS(final Mahasiswa mahasiswa, final Integer semester, final Integer tahapan,
			final String tahunAkademik, final Integer semesterPendek, final Boolean remedial, final Boolean tanya)
			throws Exception {

		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null) {
			List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(SyaratUjian.class).add(Restrictions.eq("uts", true))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					SyaratUjian.class);

			System.out.println("syaratUjians => " + syaratUjians);

			for (SyaratUjian syaratUjian : syaratUjians) {
				SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Cetak Kartu UTS",
						warnings);
			}
		}
		if (!warnings.isEmpty()) {
			String w = "";
			for (String wa : warnings) {
				w += w.isEmpty() ? wa : "\n\n" + wa;
			}
			if (tanya) {

				MyMessageboxConfig.showFormatCb(
						"{V1}\n\n\nApakah Bapak/Ibu yakin akan tetap melanjutkan proses pencetakan Kartu Ujian Tengah Semester (UTS) ini?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									lanjutCetakUTS(mahasiswa, semester, tahapan, tahunAkademik, semesterPendek,
											remedial, tanya);

								}

							}
						}, w);

			} else {
				MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			lanjutCetakUTS(mahasiswa, semester, tahapan, tahunAkademik, semesterPendek, remedial, tanya);
		}

	}

	public static void lanjutCetakUTS(final Mahasiswa mahasiswa, final Integer semester, final Integer tahapan,
			final String tahunAkademik, final Integer semesterPendek, final Boolean remedial, Boolean tanya)
			throws Exception {

		final Set<Long> longsHasilTidak = Common.checkStatusAbsensi(mahasiswa, semester, semesterPendek, "UTS");

		if (Common.bolehKonfigurasi("mahasiswa_harus_upload_foto_sebelum_ikut_ujian_uts", Konfigurasi.TIDAK_AKTIF)) {

			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			int fotoMahasiswa = ((Number) streamingSession.createCriteria(FotoMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			StreamingHibernateUtil.getInstance().closeSession();

			if (fotoMahasiswa == 0) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Mahasiswa dengan NIM {V1} belum dapat mencetak Kartu Ujian Tengah Semester (UTS) karena foto belum diunggah. Langkah yang dapat dilakukan: (1) unggah foto mahasiswa terlebih dahulu pada menu yang tersedia; (2) pastikan foto telah sesuai ketentuan; (3) ulangi proses pencetakan kartu ujian.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim());
				return;
			}
		}

		if (tanya) {

			if (!UtsDanUasCheckerHelper.checkItemBiayaPembayaranSebelumUTSSudahMemenuhi(mahasiswa, semester, tahapan,
					semesterPendek, new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								prosesCetakUTS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik,
										longsHasilTidak);

							}
						}
					})) {
				return;
			}

			if (UtsDanUasCheckerHelper.checkPembayaranSebelumUTSSudahMemenuhi(mahasiswa, semester, semesterPendek,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								prosesCetakUTS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik,
										longsHasilTidak);

							}
						}
					})) {
				prosesCetakUTS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik, longsHasilTidak);
			}
		} else {

			if (!UtsDanUasCheckerHelper.checkItemBiayaPembayaranSebelumUTSSudahMemenuhi(mahasiswa, semester, tahapan,
					semesterPendek, null)) {
				return;
			}

			if (!UtsDanUasCheckerHelper.checkPembayaranSebelumUTSSudahMemenuhi(mahasiswa, semester, semesterPendek,
					null)) {
				return;
			}

			prosesCetakUTS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik, longsHasilTidak);
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static void onLaporanAbsensi(final JadwalPelajaran jadwalPelajaran, final String ujian) throws Exception {
		Session session = HibernateUtil.currentSession();
		Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id")).createAlias("statusPertemuan", "statusPertemuan")
				.add(Restrictions.eq("statusPertemuan.nama", ujian))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).setMaxResults(1).uniqueResult();

		List<KelasPertemuan> kelasPertemuans = session.createCriteria(KelasPertemuan.class)
				.add(Restrictions.eq("pertemuan", pertemuan)).addOrder(Order.asc("nama")).list();
		if (kelasPertemuans.isEmpty()) {
			onLaporanAbsensi(jadwalPelajaran, ujian, null);
		} else {
			final MyWindow window = new MyWindow("Pilih Tahun Akademik dan Semester", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("300px");
			window.setWidth("600px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			center.setParent(borderlayout);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("20%");
			column.setParent(columns);
			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Kelas *"));
			final Combobox kelas;
			row.appendChild(kelas = new Combobox());
			kelas.setWidth("90%");
			kelas.setReadonly(true);
			Common.insertComboItems(kelas, "nama", kelasPertemuans);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak", "/img/save.gif");
			save.setTooltiptext("Cetak");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();

					if (kelas.getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, Kelas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Kelas dari daftar dropdown yang tersedia; (2) Pastikan data kelas sudah tersedia di sistem; (3) Ulangi proses cetak laporan absensi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					onLaporanAbsensi(jadwalPelajaran, ujian, (KelasPertemuan) kelas.getSelectedItem().getValue());

				}
			});
			save.setParent(toolbar);

			window.onModal();
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static void onLaporanAbsensi(final Perkuliahan perkuliahan, final String ujian) throws Exception {
		Pertemuan pertemuan = null;
		List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
		for (Pertemuan pertemuan2 : pertemuans) {
			if (pertemuan2 != null && pertemuan2.getStatusPertemuan() != null
					&& pertemuan2.getStatusPertemuan().getNama().toLowerCase().startsWith(ujian.toLowerCase())) {
				pertemuan = pertemuan2;
			}
		}
		pertemuans = null;

		System.out.println("pertemuan -> " + pertemuan);

		Session session = HibernateUtil.currentSession();
		List<KelasPertemuan> kelasPertemuans = session.createCriteria(KelasPertemuan.class)
				.add(Restrictions.eq("pertemuan", pertemuan)).addOrder(Order.asc("nama")).list();
		if (kelasPertemuans.isEmpty()) {
			onLaporanAbsensi(perkuliahan, ujian, null);
		} else {
			final MyWindow window = new MyWindow("Pilih Kelas", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("300px");
			window.setWidth("600px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			center.setParent(borderlayout);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("20%");
			column.setParent(columns);
			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Kelas *"));
			final Combobox kelas;
			row.appendChild(kelas = new Combobox());
			kelas.setWidth("90%");
			kelas.setReadonly(true);
			Common.insertComboItems(kelas, "nama", kelasPertemuans);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak", "/img/save.gif");
			save.setTooltiptext("Cetak");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();

					if (kelas.getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, Kelas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Kelas dari daftar dropdown yang tersedia; (2) Pastikan data kelas sudah tersedia di sistem; (3) Ulangi proses cetak laporan absensi. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					onLaporanAbsensi(perkuliahan, ujian, (KelasPertemuan) kelas.getSelectedItem().getValue());

				}
			});
			save.setParent(toolbar);

			window.onModal();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void mapLaporanBeritaAcara(Pertemuan pertemuan, KelasPertemuan kelasPertemuan,
			List<Map<String, Serializable>> maps, Map parameters, Label label) {
		if (pertemuan == null || maps == null || parameters == null) {
			return;
		}
		Perkuliahan p = pertemuan.getPerkuliahan();
		if (p == null) {
			return;
		}
		Perkuliahan perkuliahan = p.getPerkuliahan_paralel() == null ? p : p.getPerkuliahan_paralel();
		Session session = null;

		try {
			Jurusan jurusan = perkuliahan.getJurusan();
			Fakultas fakultas = jurusan == null ? null : jurusan.getFakultas();
			PerguruanTinggi perguruanTinggi = fakultas == null ? null : fakultas.getPerguruanTinggi();
			Matakuliah matakuliah = perkuliahan.getMatakuliah();

			if (perkuliahan != null) {
				Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");

				if (jurusan != null) {
					Common.insertProperty(Jurusan.class, jurusan, parameters, "jur");
				}
				if (fakultas != null) {
					Common.insertProperty(Fakultas.class, fakultas, parameters, "fak");
				}
				if (perguruanTinggi != null) {
					Common.insertProperty(PerguruanTinggi.class, perguruanTinggi, parameters, "pt");
				}

			}

			parameters.put("perkuliahan", perkuliahan.getId());
			parameters.put("kelas", kelasPertemuan != null
					? (perkuliahan.getSemester() + " "
							+ (kelasPertemuan.getNama() == null ? "" : kelasPertemuan.getNama()))
					: perkuliahan.getSemester() + " " + (perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()));

			parameters.put("program", perkuliahan.getProgram());
			parameters.put("jurusan", jurusan == null ? "" : jurusan.getNama());
			parameters.put("semester", perkuliahan.getSemester());
			parameters.put("sks", matakuliah == null ? 0 : matakuliah.getSks());

			parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
			parameters.put("tampil_nilai", 1);
			parameters.put("fakultas", fakultas == null ? "" : fakultas.getNama());
			parameters.put("jenis_semester", perkuliahan.getGanjilGenap() == null ? ""
					: perkuliahan.getGanjilGenap());
			parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
			parameters.put("kode_matakuliah", matakuliah == null ? "" : matakuliah.getKode());
			parameters.put("nama_matakuliah", matakuliah == null ? "" : matakuliah.getNama());

			List<Dosen> dataDosens = p.populateDosenBuNama();
			if (dataDosens == null) dataDosens = new ArrayList<Dosen>();
			if (dataDosens.size() > 1) {
				String dosenPengampu = "";
				for (Dosen dosen : dataDosens) {
					dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}
				parameters.put("dosen", dosenPengampu);
				dosenPengampu = "";
				for (Dosen dosen : dataDosens) {
					dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : "; " + dosen.getNama();
				}
				parameters.put("dosen_spl", dosenPengampu);
			} else {
				parameters.put("dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
				parameters.put("dosen_spl", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
			}

			parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
			parameters.put("jurusan", perkuliahan.getKurikulum() == null || jurusan == null ? "" : jurusan.getNama());

			if (perkuliahan.getKurikulum() != null && perkuliahan.getJurusan() != null
					&& perkuliahan.getJurusan().getGrupJurusan() != null
					&& perkuliahan.getJurusan().getGrupJurusan().getKajur() != null) {
				parameters.put("nama_kajur", perkuliahan.getJurusan() == null ? ""
						: perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
				parameters.put("nip_kajur", perkuliahan.getJurusan() == null ? ""
						: perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
			}

			session = ais.action.report.Report.openNativeSession();

			Pegawai petugas = kelasPertemuan != null
					? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas())
					: (Pegawai) (pertemuan == null || pertemuan.getPetugas() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));

			Pegawai petugas2 = kelasPertemuan != null
					? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas2())
					: (Pegawai) (pertemuan == null || pertemuan.getPetugas2() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));

			Pegawai petugas3 = kelasPertemuan != null
					? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas3())
					: (Pegawai) (pertemuan == null || pertemuan.getPetugas3() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));

			Pegawai petugas4 = kelasPertemuan != null
					? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas4())
					: (Pegawai) (pertemuan == null || pertemuan.getPetugas4() == null ? null
							: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

			Dosen pjawabDosen = kelasPertemuan != null
					? (Dosen) ConstantValues.ambil(Dosen.class.getName(), kelasPertemuan.getPjDosen())
					: (Dosen) (pertemuan == null || pertemuan.getPjDosen() == null ? null
							: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

			System.out.println("petugas => " + petugas);

			parameters.put("petugas", petugas == null ? "" : petugas.getNama());
			parameters.put("petugas_nip", petugas == null ? "" : petugas.getMycode());

			parameters.put("petugas2", petugas2 == null ? "" : petugas2.getNama());
			parameters.put("petugas_nip2", petugas2 == null ? "" : petugas2.getMycode());

			parameters.put("petugas3", petugas3 == null ? "" : petugas3.getNama());
			parameters.put("petugas_nip3", petugas3 == null ? "" : petugas3.getMycode());

			parameters.put("petugas4", petugas4 == null ? "" : petugas4.getNama());
			parameters.put("petugas_nip4", petugas4 == null ? "" : petugas4.getMycode());

			parameters.put("pjdosen", pjawabDosen == null ? "" : pjawabDosen.getNama());
			parameters.put("pjdosen_nip", pjawabDosen == null ? "" : pjawabDosen.getMycode());

			if (petugas != null) {
				LampiranLain lam = LampiranLain.ambil(petugas.getId(), LampiranLain.TTD_PEGAWAI);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9651");
						}

						parameters.put("ttd_petugas", ttd);
					}
				} else if (petugas.getDosen() != null) {
					lam = LampiranLain.ambil(petugas.getDosen().getId(), LampiranLain.TTD_DOSEN);
					nama = lam == null ? null : lam.getNama();

					if (nama != null) {
						if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
								|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
								|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
							String ttd = "";
							try {
								ttd = lam.ambilFile().getAbsolutePath();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9668");
							}

							parameters.put("ttd_petugas", ttd);
						}
					}
				}
			}
			if (petugas2 != null) {
				LampiranLain lam = LampiranLain.ambil(petugas2.getId(), LampiranLain.TTD_PEGAWAI);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9688");
						}

						parameters.put("ttd_petugas2", ttd);
					}
				} else if (petugas2.getDosen() != null) {
					lam = LampiranLain.ambil(petugas2.getDosen().getId(), LampiranLain.TTD_DOSEN);
					nama = lam == null ? null : lam.getNama();

					if (nama != null) {
						if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
								|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
								|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
							String ttd = "";
							try {
								ttd = lam.ambilFile().getAbsolutePath();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9705");
							}

							parameters.put("ttd_petugas2", ttd);
						}
					}
				}
			}
			if (petugas3 != null) {
				LampiranLain lam = LampiranLain.ambil(petugas3.getId(), LampiranLain.TTD_PEGAWAI);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9725");
						}

						parameters.put("ttd_petugas3", ttd);
					}
				} else if (petugas3.getDosen() != null) {
					lam = LampiranLain.ambil(petugas3.getDosen().getId(), LampiranLain.TTD_DOSEN);
					nama = lam == null ? null : lam.getNama();

					if (nama != null) {
						if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
								|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
								|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
							String ttd = "";
							try {
								ttd = lam.ambilFile().getAbsolutePath();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9742");
							}

							parameters.put("ttd_petugas3", ttd);
						}
					}
				}
			}
			if (petugas4 != null) {
				LampiranLain lam = LampiranLain.ambil(petugas4.getId(), LampiranLain.TTD_PEGAWAI);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9762");
						}

						parameters.put("ttd_petugas4", ttd);
					}
				} else if (petugas4.getDosen() != null) {
					lam = LampiranLain.ambil(petugas4.getDosen().getId(), LampiranLain.TTD_DOSEN);
					nama = lam == null ? null : lam.getNama();

					if (nama != null) {
						if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
								|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
								|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
							String ttd = "";
							try {
								ttd = lam.ambilFile().getAbsolutePath();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9779");
							}

							parameters.put("ttd_petugas4", ttd);
						}
					}
				}
			}
			if (pjawabDosen != null) {
				LampiranLain lam = LampiranLain.ambil(pjawabDosen.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9799");
						}
						parameters.put("ttd_pjdosen", ttd);
					}
				}
			}

			if (perkuliahan != null && perkuliahan.getJurusan() != null
					&& perkuliahan.getJurusan().getFakultas() != null
					&& perkuliahan.getJurusan().getFakultas().getDekan() != null) {

				parameters.put("nama_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getNama());
				parameters.put("kode_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getCode());
				parameters.put("mykode_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getMycode());
				parameters.put("nidn_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getNidn());

				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getDekan().getId(),
						LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9827");
						}
						parameters.put("ttd_dekan", ttd);
					}
				}
			}

			if (perkuliahan != null && perkuliahan.getJurusan() != null
					&& perkuliahan.getJurusan().getFakultas() != null
					&& perkuliahan.getJurusan().getFakultas().getPudek1() != null) {

				parameters.put("nama_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getNama());
				parameters.put("kode_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getCode());
				parameters.put("mykode_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getMycode());
				parameters.put("nidn_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getNidn());

				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getPudek1().getId(),
						LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9855");
						}
						parameters.put("ttd_pudek1", ttd);
					}
				}
			}

			if (perkuliahan != null && perkuliahan.getJurusan() != null
					&& perkuliahan.getJurusan().getFakultas() != null
					&& perkuliahan.getJurusan().getFakultas().getPudek2() != null) {

				parameters.put("nama_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getNama());
				parameters.put("kode_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getCode());
				parameters.put("mykode_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getMycode());
				parameters.put("nidn_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getNidn());

				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getPudek2().getId(),
						LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9883");
						}
						parameters.put("ttd_pudek2", ttd);
					}
				}
			}

			if (perkuliahan != null && perkuliahan.getJurusan() != null
					&& perkuliahan.getJurusan().getFakultas() != null
					&& perkuliahan.getJurusan().getFakultas().getPudek3() != null) {

				parameters.put("nama_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getNama());
				parameters.put("kode_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getCode());
				parameters.put("mykode_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getMycode());
				parameters.put("nidn_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getNidn());

				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getPudek3().getId(),
						LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9911");
						}
						parameters.put("ttd_pudek3", ttd);
					}
				}
			}

			if (perkuliahan != null && perkuliahan.getJurusan() != null
					&& perkuliahan.getJurusan().getKaprodi() != null) {

				parameters.put("nama_kaprodi", perkuliahan.getJurusan().getKaprodi().getNama());
				parameters.put("kode_kaprodi", perkuliahan.getJurusan().getKaprodi().getCode());
				parameters.put("mykode_kaprodi", perkuliahan.getJurusan().getKaprodi().getMycode());
				parameters.put("nidn_kaprodi", perkuliahan.getJurusan().getKaprodi().getNidn());

				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getKaprodi().getId(),
						LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = "";
						try {
							ttd = lam.ambilFile().getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:9938");
						}
						parameters.put("ttd_kaprodi", ttd);
					}
				}
			}

			parameters.put("pengawas_ujian",
					(petugas == null ? "" : petugas.getNama()) + " " + (petugas2 == null ? "" : petugas2.getNama())
							+ " " + (petugas3 == null ? "" : petugas3.getNama()));

			parameters.put("tanggal_ujian",
					kelasPertemuan != null && kelasPertemuan.getMulai() != null ? kelasPertemuan.getMulai()
							: pertemuan == null ? null : pertemuan.getTanggal());
			parameters.put("waktu", kelasPertemuan != null
					? (kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai()) + ""
							+ (kelasPertemuan.getMulai() == null ? ""
									: ", " + Common.dateFormat4.get().format(kelasPertemuan.getMulai()))

					: (pertemuan == null ? (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai())
							: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()) + ""
									+ (pertemuan.getTanggal() == null ? ""
											: ", " + Common.dateFormat4.get().format(pertemuan.getTanggal()))));

			parameters.put("waktu_aja",
					kelasPertemuan != null
							? (kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai())
							: pertemuan == null
									? (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai())
									: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()));

			parameters.put("tanggal_aja",
					kelasPertemuan != null
							? ((kelasPertemuan.getMulai() == null ? ""
									: Common.dateFormat4.get().format(kelasPertemuan.getMulai())))
							: pertemuan == null ? ""
									: (pertemuan.getTanggal() == null ? ""
											: Common.dateFormat4.get().format(pertemuan.getTanggal())));
			parameters.put("ruang",
					kelasPertemuan != null
							? (kelasPertemuan.getRuang() == null ? "" : kelasPertemuan.getRuang().getKodeRuangan())
							: pertemuan == null
									? (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan())
									: (pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getKodeRuangan()));

			Map<String, Integer> statuses = pertemuan == null ? null : pertemuan.hitungStatus();
			int teraftar = (kelasPertemuan != null
					? ((Number) session.createCriteria(DetailKelasPertemuan.class)
							.add(Restrictions.eq("kelasPertemuan", kelasPertemuan)).createCriteria("detailperkuliahan")
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI)).uniqueResult()).intValue()
					: perkuliahan.ambilDetailperkuliahanDisetujui().size());

			int masuk = pertemuan == null || statuses.get("M") == null ? 0 : statuses.get("M");
			int tidakmasuk = teraftar - masuk;
			parameters.put("Terdaftar", teraftar);
			parameters.put("Masuk", masuk);
			parameters.put("tidakmasuk", tidakmasuk);

			FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
					.add(Restrictions.eq("perkuliahan", perkuliahan))
					.add(Restrictions.eq("statusPertemuan", pertemuan.getStatusPertemuan())).setMaxResults(1)
					.uniqueResult();

			int rowIndex = 0;
			List<Long> detailperkuliahans = kelasPertemuan != null ? session.createCriteria(DetailKelasPertemuan.class)
					.add(Restrictions.eq("kelasPertemuan", kelasPertemuan))
					.setProjection(Projections.property("detailperkuliahan.id")).createCriteria("detailperkuliahan")
					.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
					.addOrder(Order.asc("mahasiswa.nim")).add(Restrictions.eq("perkuliahan", perkuliahan))
					.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
					.createCriteria("perkuliahan", Criteria.LEFT_JOIN)

					.add(Restrictions.eq("semester", perkuliahan.getSemester()))

					.list() : new ArrayList<Long>(perkuliahan.ambilDetailperkuliahanDisetujui());
			int jumlahHadir = 0;
			int jumlahIzin = 0;
			int jumlahSakit = 0;
			int jumlahAlpha = 0;

			int size = detailperkuliahans.size() + dataDosens.size();
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					rowIndex++;
					try {
						if (label != null)
							label.setValue("Sedang memproses data " + detailperkuliahan.getMahasiswa().toString()
									+ " untuk proses cetak berita acara ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

						Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
								Statusabsensi.class.getName(),
								pertemuan.retreiveAbsensiId(detailperkuliahan.getMahasiswa().getId()));

						if (statusabsensi != null) {
							jumlahHadir += statusabsensi.getKode().equalsIgnoreCase("M") ? 1 : 0;
							jumlahIzin += statusabsensi.getKode().equalsIgnoreCase("I") ? 1 : 0;
							jumlahSakit += statusabsensi.getKode().equalsIgnoreCase("S") ? 1 : 0;
							jumlahAlpha += statusabsensi.getKode().equalsIgnoreCase("A") ? 1 : 0;
						}
						String linkFoto = CommonMedia
								.getUrlFotoPenggunaKecil(new Tbmuser(detailperkuliahan.getMahasiswa()));
						map.put("jenis", "Mahasiswa");
						map.put("link_foto", linkFoto);
						map.put("kehadiran",
								statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama()));
						map.put("keterangan",
								pertemuan.retreiveAbsensiKeterangan(detailperkuliahan.getMahasiswa().getId()));
						map.put("nama_jurusan", detailperkuliahan.getMahasiswa().getJurusan().getNama());

						map.put("nim", detailperkuliahan.getMahasiswa().getNim());
						map.put("nama", detailperkuliahan.getMahasiswa().getNama().toUpperCase());
						map.put("kode_matakuliah", p.getMatakuliah().getKode());

						if (formatNilai != null) {
							Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
							map.put("nilai", (nilai));
						}
						map.put("nilai_total", detailperkuliahan.getTotalNilai());

						Jurusan jurusanLaporan = detailperkuliahan.getMahasiswa().getJurusan();
						Fakultas fakultasLaporan = jurusanLaporan == null ? null : jurusanLaporan.getFakultas();
						Dosen dekanLaporan = fakultasLaporan == null ? null : fakultasLaporan.getDekan();
						Dosen kaprodiLaporan = jurusanLaporan == null ? null : jurusanLaporan.getKaprodi();
						map.put("nip_kajur", dekanLaporan == null ? "" : dekanLaporan.getCode());
						map.put("nama_kajur", dekanLaporan == null ? "" : dekanLaporan.getNama());
						map.put("nip_kaprodi", kaprodiLaporan == null ? "" : kaprodiLaporan.getCode());
						map.put("nama_kaprodi", kaprodiLaporan == null ? "" : kaprodiLaporan.getNama());
						map.put("id_fakultas", fakultasLaporan == null ? -1L : fakultasLaporan.getId());

						try {
							Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
							Jurusan jurusanMahasiswa = mahasiswa == null ? null : mahasiswa.getJurusan();
							Fakultas fakultasMahasiswa = jurusanMahasiswa == null ? null
									: jurusanMahasiswa.getFakultas();
							Pegawai pejabatProdi1 = jurusanMahasiswa == null ? null : jurusanMahasiswa.getPegawai1();
							Pegawai pejabatProdi2 = jurusanMahasiswa == null ? null : jurusanMahasiswa.getPegawai2();
							Pegawai pejabatProdi3 = jurusanMahasiswa == null ? null : jurusanMahasiswa.getPegawai3();
							Pegawai pejabatFakultas1 = fakultasMahasiswa == null ? null
									: fakultasMahasiswa.getPegawai1();
							Pegawai pejabatFakultas2 = fakultasMahasiswa == null ? null
									: fakultasMahasiswa.getPegawai2();
							Pegawai pejabatFakultas3 = fakultasMahasiswa == null ? null
									: fakultasMahasiswa.getPegawai3();

							map.put("id_pejabat_prodi_1", pejabatProdi1 == null ? -1L : pejabatProdi1.getId());
							map.put("periode", perkuliahan == null ? "SP" : perkuliahan.getGanjilGenap());
							map.put("jenis_pejabat_prodi_1",
									jurusanMahasiswa == null ? "" : jurusanMahasiswa.getLabelPejabat1());
							map.put("nama_pejabat_prodi_1", pejabatProdi1 == null ? "" : pejabatProdi1.getNama());
							map.put("nip_pejabat_prodi_1", pejabatProdi1 == null ? "" : pejabatProdi1.getCode());

							map.put("id_pejabat_prodi_2", pejabatProdi2 == null ? -1L : pejabatProdi2.getId());
							map.put("jenis_pejabat_prodi_2",
									jurusanMahasiswa == null ? "" : jurusanMahasiswa.getLabelPejabat2());
							map.put("nama_pejabat_prodi_2", pejabatProdi2 == null ? "" : pejabatProdi2.getNama());
							map.put("nip_pejabat_prodi_2", pejabatProdi2 == null ? "" : pejabatProdi2.getCode());

							map.put("id_pejabat_prodi_3", pejabatProdi3 == null ? -1L : pejabatProdi3.getId());
							map.put("jenis_pejabat_prodi_3",
									jurusanMahasiswa == null ? "" : jurusanMahasiswa.getLabelPejabat3());
							map.put("nama_pejabat_prodi_3", pejabatProdi3 == null ? "" : pejabatProdi3.getNama());
							map.put("nip_pejabat_prodi_3", pejabatProdi3 == null ? "" : pejabatProdi3.getCode());

							map.put("id_pejabat_fakultas_1",
									pejabatFakultas1 == null ? -1L : pejabatFakultas1.getId());
							map.put("jenis_pejabat_fakultas_1",
									fakultasMahasiswa == null ? "" : fakultasMahasiswa.getLabelPejabat1());
							map.put("nama_pejabat_fakultas_1",
									pejabatFakultas1 == null ? "" : pejabatFakultas1.getNama());
							map.put("nip_pejabat_fakultas_1",
									pejabatFakultas1 == null ? "" : pejabatFakultas1.getCode());

							map.put("id_pejabat_fakultas_2",
									pejabatFakultas2 == null ? -1L : pejabatFakultas2.getId());
							map.put("jenis_pejabat_fakultas_2",
									fakultasMahasiswa == null ? "" : fakultasMahasiswa.getLabelPejabat2());
							map.put("nama_pejabat_fakultas_2",
									pejabatFakultas2 == null ? "" : pejabatFakultas2.getNama());
							map.put("nip_pejabat_fakultas_2",
									pejabatFakultas2 == null ? "" : pejabatFakultas2.getCode());

							map.put("id_pejabat_fakultas_3",
									pejabatFakultas3 == null ? -1L : pejabatFakultas3.getId());
							map.put("jenis_pejabat_fakultas_3",
									fakultasMahasiswa == null ? "" : fakultasMahasiswa.getLabelPejabat3());
							map.put("nama_pejabat_fakultas_3",
									pejabatFakultas3 == null ? "" : pejabatFakultas3.getNama());
							map.put("nip_pejabat_fakultas_3",
									pejabatFakultas3 == null ? "" : pejabatFakultas3.getCode());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10153");
						}

						maps.add(map);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10158");
					}
				}
			}

			int index = 1;
			for (Dosen dosen : dataDosens) {
				try {
					FileFotoLain lampiranLain = FileFotoLain.ambil(false, dosen.getId(), LampiranLain.TTD_DOSEN,
							LampiranLain.class);
					if (lampiranLain != null) {
						File file = lampiranLain.ambilFile();
						if (file != null && file.exists()) {
							parameters.put("ttd_dosen_" + index, file.getAbsolutePath());
						}
						file = null;
					}
					lampiranLain = null;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10174");
//						e.printStackTrace();
				}
				index++;
			}

			for (Dosen dosen : dataDosens) {
				rowIndex++;
				try {
					if (label != null)
						label.setValue(
								"Sedang memproses data " + dosen.toString() + " untuk proses cetak berita acara ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(dosen.getId()));

					if (statusabsensi != null) {
						jumlahHadir += statusabsensi.getKode().equalsIgnoreCase("M") ? 1 : 0;
						jumlahIzin += statusabsensi.getKode().equalsIgnoreCase("I") ? 1 : 0;
						jumlahSakit += statusabsensi.getKode().equalsIgnoreCase("S") ? 1 : 0;
						jumlahAlpha += statusabsensi.getKode().equalsIgnoreCase("A") ? 1 : 0;
					}
					String linkFoto = CommonMedia.getUrlFotoPenggunaKecil(new Tbmuser(dosen));
					map.put("jenis", "Dosen");
					map.put("link_foto", linkFoto);
					map.put("kehadiran", statusabsensi == null ? "-" : Common.getBahasaConfig(statusabsensi.getNama()));
					map.put("keterangan", pertemuan.retreiveAbsensiKeterangan(dosen.getId()));
					map.put("nama_jurusan", dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());

					map.put("nim", dosen.getNidn());
					map.put("nama", dosen.getNama().toUpperCase());
					map.put("kode_matakuliah", p.getMatakuliah().getKode());

					try {
						map.put("nip_kajur",
								dosen.getJurusan() == null || dosen.getJurusan().getFakultas() == null
										|| dosen.getJurusan().getFakultas().getDekan() == null ? ""
												: dosen.getJurusan().getFakultas().getDekan().getCode());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10213");
					}
					try {
						map.put("nama_kajur",
								dosen.getJurusan() == null || dosen.getJurusan().getFakultas() == null
										|| dosen.getJurusan().getFakultas().getDekan() == null ? ""
												: dosen.getJurusan().getFakultas().getDekan().getNama());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10218");
					}

					try {
						map.put("nip_kaprodi",
								dosen.getJurusan() == null ? "" : dosen.getJurusan().getKaprodi().getCode());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10224");
					}
					try {
						map.put("nama_kaprodi",
								dosen.getJurusan() == null ? "" : dosen.getJurusan().getKaprodi().getNama());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10229");
					}

					try {
						map.put("id_fakultas",
								dosen.getJurusan() == null || dosen.getJurusan().getFakultas() == null ? -1L : dosen.getJurusan().getFakultas().getId());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10235");
					}

					try {
						Dosen mahasiswa = dosen;
						if (mahasiswa.getJurusan() != null) {
						map.put("id_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? -1L
								: mahasiswa.getJurusan().getPegawai1().getId());
						map.put("periode", perkuliahan == null ? "SP" : perkuliahan.getGanjilGenap());
						map.put("jenis_pejabat_prodi_1", mahasiswa.getJurusan().getLabelPejabat1());
						map.put("nama_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? ""
								: mahasiswa.getJurusan().getPegawai1().getNama());
						map.put("nip_pejabat_prodi_1", mahasiswa.getJurusan().getPegawai1() == null ? ""
								: mahasiswa.getJurusan().getPegawai1().getCode());

						map.put("id_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? -1L
								: mahasiswa.getJurusan().getPegawai2().getId());
						map.put("jenis_pejabat_prodi_2", mahasiswa.getJurusan().getLabelPejabat2());
						map.put("nama_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? ""
								: mahasiswa.getJurusan().getPegawai2().getNama());
						map.put("nip_pejabat_prodi_2", mahasiswa.getJurusan().getPegawai2() == null ? ""
								: mahasiswa.getJurusan().getPegawai2().getCode());

						map.put("id_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? -1L
								: mahasiswa.getJurusan().getPegawai3().getId());
						map.put("jenis_pejabat_prodi_3", mahasiswa.getJurusan().getLabelPejabat3());
						map.put("nama_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? ""
								: mahasiswa.getJurusan().getPegawai3().getNama());
						map.put("nip_pejabat_prodi_3", mahasiswa.getJurusan().getPegawai3() == null ? ""
								: mahasiswa.getJurusan().getPegawai3().getCode());

						map.put("id_pejabat_fakultas_1",
								mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
										: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());

						map.put("jenis_pejabat_fakultas_1", mahasiswa.getJurusan().getFakultas().getLabelPejabat1());
						map.put("nama_pejabat_fakultas_1",
								mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? ""
										: mahasiswa.getJurusan().getFakultas().getPegawai1().getNama());
						map.put("nip_pejabat_fakultas_1",
								mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? ""
										: mahasiswa.getJurusan().getFakultas().getPegawai1().getCode());

						map.put("id_pejabat_fakultas_2",
								mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
										: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());
						map.put("jenis_pejabat_fakultas_2", mahasiswa.getJurusan().getFakultas().getLabelPejabat2());
						map.put("nama_pejabat_fakultas_2",
								mahasiswa.getJurusan().getFakultas().getPegawai2() == null ? ""
										: mahasiswa.getJurusan().getFakultas().getPegawai2().getNama());
						map.put("nip_pejabat_fakultas_2",
								mahasiswa.getJurusan().getFakultas().getPegawai2() == null ? ""
										: mahasiswa.getJurusan().getFakultas().getPegawai2().getCode());

						map.put("id_pejabat_fakultas_3",
								mahasiswa.getJurusan().getFakultas().getPegawai1() == null ? -1L
										: mahasiswa.getJurusan().getFakultas().getPegawai1().getId());
						map.put("jenis_pejabat_fakultas_3", mahasiswa.getJurusan().getFakultas().getLabelPejabat3());
						map.put("nama_pejabat_fakultas_3",
								mahasiswa.getJurusan().getFakultas().getPegawai3() == null ? ""
										: mahasiswa.getJurusan().getFakultas().getPegawai3().getNama());
						map.put("nip_pejabat_fakultas_3",
								mahasiswa.getJurusan().getFakultas().getPegawai3() == null ? ""
										: mahasiswa.getJurusan().getFakultas().getPegawai3().getCode());
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:10298");
					}

					maps.add(map);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10303");
				}
			}

			parameters.put("pertemuan_ke", pertemuan.getPertemuanKe());
			parameters.put("jumlahHadir", jumlahHadir);
			parameters.put("jumlahIzin", jumlahIzin);
			parameters.put("jumlahSakit", jumlahSakit);
			parameters.put("jumlahAlpha", jumlahAlpha);
			parameters.put("catatan_dosen", pertemuan.getCatatan());
			parameters.put("topik", pertemuan.getTopik());
			parameters.put("jenis_perkuliahan",
					pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());
			parameters.put("tanggal",
					Common.dateFormat2.get().format(pertemuan.getTanggalRealisasi() == null ? pertemuan.getTanggal()
							: pertemuan.getTanggalRealisasi()));
			parameters.put("hari",
					Common.dateFormatHari.get().format(pertemuan.getTanggalRealisasi() == null ? pertemuan.getTanggal()
							: pertemuan.getTanggalRealisasi()));

			String tahunAkademik = perkuliahan.getTahunAjaran();

			parameters.put("bar", "3-" + tahunAkademik + "-" + perkuliahan.getSemester() + "-" + perkuliahan.getId());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10329");
		} finally {
			ais.action.report.Report.closeNativeSession(session);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanBeritaAcara(final Pertemuan pertemuan, final KelasPertemuan kelasPertemuan)
			throws Exception {

		if (pertemuan == null) {
			return;
		}
		final Perkuliahan p = pertemuan.getPerkuliahan();
		if (p == null) {
			return;
		}
		final Perkuliahan perkuliahan = p.getPerkuliahan_paralel() == null ? p : p.getPerkuliahan_paralel();

		final List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		final Map parameters = ais.common.HashMapGenerator.getRand();

		final Label label = Common.displayLoadBar(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(),
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						parameters.put("maps", maps);

						Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF, new Map[] { parameters },
								new String[] { "BeritaAcaraPerkuliahan" },
								new String[] { "Berita Acara " + pertemuan.getTopik() },
								ais.ui.util.WaktuUtil.getDate());

						Tabpanels tabpanels = tabbox.getTabpanels();
						Tabs tabs = tabbox.getTabs();

						final MyTabConfig tabPenilaian = new MyTabConfig("Rekap Kehadiran");
						tabPenilaian.setParent(tabs);

						final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();

						tabpanelPenilaian.setParent(tabpanels);
						tabpanelPenilaian.setHeight("650px");
						tabPenilaian.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelPenilaian.getChildren().size() == 0) {
									DashboardRekapAbsensiPerMahasiswa dashboardRekapAbsensiMahasiswa = new DashboardRekapAbsensiPerMahasiswa(
											perkuliahan);
									tabpanelPenilaian.appendChild(dashboardRekapAbsensiMahasiswa);
								}
							}
						});

					}
				});

		new Thread(new Runnable() {

			@Override
			public void run() {
				CommonReportHelper.mapLaporanBeritaAcara(pertemuan, kelasPertemuan, maps, parameters, label);
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanAbsensi(final Perkuliahan p, final String ujian, final KelasPertemuan kelasPertemuan)
			throws Exception {

		final Perkuliahan perkuliahan = p.getPerkuliahan_paralel() == null ? p : p.getPerkuliahan_paralel();

		final Map<Long, List<Map<String, Serializable>>> maps = new HashMap<Long, List<Map<String, Serializable>>>();
		final Map parameters = ais.common.HashMapGenerator.getRand();

		final Label label = Common.displayLoadBar(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(),
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						parameters.put("maps", maps.get(perkuliahan.getId()));
						parameters.put("maps_tidak_hadir", maps.get(-perkuliahan.getId()));

						Map[] map;
						String[] laporans;
						String[] namaLaporans;

						map = new Map[] { parameters, parameters, parameters };
						laporans = new String[] { "LaporanAbsensi" + ujian, "CoverLaporanAbsensi" + ujian,
								"BeritaAcaraLaporanAbsensi" + ujian };
						namaLaporans = new String[] { "Daftar Hadir " + ujian, "Cover " + ujian,
								"Berita Acara " + ujian };

						Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF, map, laporans, namaLaporans,
								ais.ui.util.WaktuUtil.getDate());

						Tabpanels tabpanels = tabbox.getTabpanels();
						Tabs tabs = tabbox.getTabs();

						final MyTabConfig tabPenilaian = new MyTabConfig("Rekap Kehadiran");
						tabPenilaian.setParent(tabs);

						final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();

						tabpanelPenilaian.setParent(tabpanels);
						tabpanelPenilaian.setHeight("650px");
						tabPenilaian.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelPenilaian.getChildren().size() == 0) {
									DashboardRekapAbsensiPerMahasiswa dashboardRekapAbsensiMahasiswa = new DashboardRekapAbsensiPerMahasiswa(
											perkuliahan);
									tabpanelPenilaian.appendChild(dashboardRekapAbsensiMahasiswa);
								}
							}
						});

					}
				});

		new Thread(new Runnable() {

			@Override
			public void run() {

				List<Map<String, Serializable>> mapsList = new ArrayList<Map<String, Serializable>>();

				List<Map<String, Serializable>> mapsListTidakhadir = new ArrayList<Map<String, Serializable>>();

				try {

					if (perkuliahan != null) {
						Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");
					}

					parameters.put("perkuliahan", perkuliahan.getId());
					parameters
							.put("kelas",
									kelasPertemuan != null
											? (perkuliahan.getSemester() + " "
													+ (kelasPertemuan.getNama() == null ? ""
															: kelasPertemuan.getNama()))
											: perkuliahan.getSemester() + " "
													+ (perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()));

					parameters.put("program", perkuliahan.getProgram());
					parameters.put("jurusan",
							perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
					parameters.put("semester", perkuliahan.getSemester());
					parameters.put("sks", perkuliahan.getMatakuliah().getSks());

					parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
					parameters.put("tampil_nilai", 1);
					parameters.put("fakultas",
							perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getFakultas().getNama());
					parameters.put("jenis_semester",
							((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
					parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
					parameters.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
					parameters.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());

					List<Dosen> dataDosens = perkuliahan.populateDosenBuNama();
					if (dataDosens.size() > 1) {
						String dosenPengampu = "";
						for (Dosen dosen : dataDosens) {
							dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
						}
						parameters.put("dosen", dosenPengampu);
						dosenPengampu = "";
						for (Dosen dosen : dataDosens) {
							dosenPengampu += dosenPengampu.isEmpty() ? dosen.getNama() : "; " + dosen.getNama();
						}
						parameters.put("dosen_spl", dosenPengampu);
					} else {
						parameters.put("dosen",
								perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
						parameters.put("dosen_spl",
								perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
					}

					int index = 1;
					for (Dosen dosen : dataDosens) {
						parameters.put("dosen_id" + index, dosen.getId());
						try {
							FileFotoLain lampiranLain = FileFotoLain.ambil(false, dosen.getId(), LampiranLain.TTD_DOSEN,
									LampiranLain.class);
							if (lampiranLain != null) {
								File file = lampiranLain.ambilFile();
								if (file != null && file.exists()) {
									parameters.put("ttd_dosen_" + index, file.getAbsolutePath());
								}
								file = null;
								lampiranLain = null;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10527");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
						index++;
					}
					parameters.put("nidn_dosen",
							perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNidn());
					parameters.put("nip_dosen",
							perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
					parameters.put("jurusan",
							perkuliahan.getKurikulum() == null ? "" : perkuliahan.getJurusan().getNama());

					if (perkuliahan.getKurikulum() != null && perkuliahan.getJurusan() != null
							&& perkuliahan.getJurusan().getGrupJurusan() != null
							&& perkuliahan.getJurusan().getGrupJurusan().getKajur() != null) {
						parameters.put("nama_kajur", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getGrupJurusan().getKajur().getNama());
						parameters.put("nip_kajur", perkuliahan.getJurusan() == null ? ""
								: perkuliahan.getJurusan().getGrupJurusan().getKajur().getCode());
					}

					parameters.put("col1", ujian);
					parameters.put("ujian", ujian);

					Session session = ais.action.report.Report.openNativeSession();

					Pertemuan pertemuan = kelasPertemuan != null ? kelasPertemuan.getPertemuan() : null;

					Pegawai petugas = null;
					Pegawai petugas2 = null;
					Pegawai petugas3 = null;
					Pegawai petugas4 = null;

					Dosen pj = null;

					if (kelasPertemuan == null) {
						List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
						for (Pertemuan pertemuan2 : pertemuans) {
							if (pertemuan2 != null && pertemuan2.getStatusPertemuan() != null && pertemuan2
									.getStatusPertemuan().getNama().toLowerCase().startsWith(ujian.toLowerCase())) {
								pertemuan = pertemuan2;

								Pegawai p = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(),
										pertemuan.getPetugas());
								if (p != null) {
									petugas = p;
								}

								p = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2());
								if (p != null) {
									petugas2 = p;
								}

								p = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3());
								if (p != null) {
									petugas3 = p;
								}

								p = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4());
								if (p != null) {
									petugas4 = p;
								}

								Dosen pjawabDosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(),
										pertemuan2.getPjDosen());
								if (pjawabDosen != null) {
									pj = pjawabDosen;
								}
							}
						}
					} else {
						petugas = kelasPertemuan != null
								? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas())
								: (Pegawai) (pertemuan == null || pertemuan.getPetugas() == null ? null
										: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));

						petugas2 = kelasPertemuan != null
								? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas2())
								: (Pegawai) (pertemuan == null || pertemuan.getPetugas2() == null ? null
										: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));

						petugas3 = kelasPertemuan != null
								? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas3())
								: (Pegawai) (pertemuan == null || pertemuan.getPetugas3() == null ? null
										: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));

						petugas4 = kelasPertemuan != null
								? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas4())
								: (Pegawai) (pertemuan == null || pertemuan.getPetugas4() == null ? null
										: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

						pj = kelasPertemuan != null
								? (Dosen) ConstantValues.ambil(Dosen.class.getName(), kelasPertemuan.getPjDosen())
								: (Dosen) (pertemuan == null || pertemuan.getPjDosen() == null ? null
										: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));
					}

					if (petugas != null) {
						LampiranLain lam = LampiranLain.ambil(petugas.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10635");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas", ttd);
							}
						} else if (petugas.getDosen() != null) {
							lam = LampiranLain.ambil(petugas.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10652");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas", ttd);
								}
							}
						}
					}
					if (petugas2 != null) {
						LampiranLain lam = LampiranLain.ambil(petugas2.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10672");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas2", ttd);
							}
						} else if (petugas2.getDosen() != null) {
							lam = LampiranLain.ambil(petugas2.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10689");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas2", ttd);
								}
							}
						}
					}
					if (petugas3 != null) {
						LampiranLain lam = LampiranLain.ambil(petugas3.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10709");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas3", ttd);
							}
						} else if (petugas3.getDosen() != null) {
							lam = LampiranLain.ambil(petugas3.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10726");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas3", ttd);
								}
							}
						}
					}

					if (petugas4 != null) {
						LampiranLain lam = LampiranLain.ambil(petugas4.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10747");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas4", ttd);
							}
						} else if (petugas4.getDosen() != null) {
							lam = LampiranLain.ambil(petugas4.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10764");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas4", ttd);
								}
							}
						}
					}

					Dosen pjawabDosen = kelasPertemuan != null
							? (Dosen) ConstantValues.ambil(Dosen.class.getName(), kelasPertemuan.getPjDosen())
							: (Dosen) (pertemuan == null || pertemuan.getPjDosen() == null ? null
									: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

					parameters.put("pjdosen", pjawabDosen == null ? "" : pjawabDosen.getNama());
					parameters.put("pjdosen_nip", pjawabDosen == null ? "" : pjawabDosen.getMycode());

					if (pjawabDosen != null) {
						LampiranLain lam = LampiranLain.ambil(pjawabDosen.getId(), LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10793");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_pjdosen", ttd);
							}
						}
					}

					if (perkuliahan != null && perkuliahan.getJurusan() != null
							&& perkuliahan.getJurusan().getFakultas() != null
							&& perkuliahan.getJurusan().getFakultas().getDekan() != null) {

						parameters.put("nama_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getNama());
						parameters.put("kode_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getCode());
						parameters.put("mykode_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getMycode());
						parameters.put("nidn_dekan", perkuliahan.getJurusan().getFakultas().getDekan().getNidn());

						LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getDekan().getId(),
								LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10821");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_dekan", ttd);
							}
						}
					}

					if (perkuliahan != null && perkuliahan.getJurusan() != null
							&& perkuliahan.getJurusan().getFakultas() != null
							&& perkuliahan.getJurusan().getFakultas().getPudek1() != null) {

						parameters.put("nama_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getNama());
						parameters.put("kode_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getCode());
						parameters.put("mykode_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getMycode());
						parameters.put("nidn_pudek1", perkuliahan.getJurusan().getFakultas().getPudek1().getNidn());

						LampiranLain lam = LampiranLain.ambil(
								perkuliahan.getJurusan().getFakultas().getPudek1().getId(), LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10849");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_pudek1", ttd);
							}
						}
					}

					if (perkuliahan != null && perkuliahan.getJurusan() != null
							&& perkuliahan.getJurusan().getFakultas() != null
							&& perkuliahan.getJurusan().getFakultas().getPudek2() != null) {

						parameters.put("nama_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getNama());
						parameters.put("kode_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getCode());
						parameters.put("mykode_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getMycode());
						parameters.put("nidn_pudek2", perkuliahan.getJurusan().getFakultas().getPudek2().getNidn());

						LampiranLain lam = LampiranLain.ambil(
								perkuliahan.getJurusan().getFakultas().getPudek2().getId(), LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10877");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_pudek2", ttd);
							}
						}
					}

					if (perkuliahan != null && perkuliahan.getJurusan() != null
							&& perkuliahan.getJurusan().getFakultas() != null
							&& perkuliahan.getJurusan().getFakultas().getPudek3() != null) {

						parameters.put("nama_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getNama());
						parameters.put("kode_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getCode());
						parameters.put("mykode_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getMycode());
						parameters.put("nidn_pudek3", perkuliahan.getJurusan().getFakultas().getPudek3().getNidn());

						LampiranLain lam = LampiranLain.ambil(
								perkuliahan.getJurusan().getFakultas().getPudek3().getId(), LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10905");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_pudek3", ttd);
							}
						}
					}

					if (perkuliahan != null && perkuliahan.getJurusan() != null
							&& perkuliahan.getJurusan().getKaprodi() != null) {

						parameters.put("nama_kaprodi", perkuliahan.getJurusan().getKaprodi().getNama());
						parameters.put("kode_kaprodi", perkuliahan.getJurusan().getKaprodi().getCode());
						parameters.put("mykode_kaprodi", perkuliahan.getJurusan().getKaprodi().getMycode());
						parameters.put("nidn_kaprodi", perkuliahan.getJurusan().getKaprodi().getNidn());

						LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getKaprodi().getId(),
								LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:10932");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_kaprodi", ttd);
							}
						}
					}

					System.out.println("pertemuan => " + pertemuan);

					System.out.println("petugas => " + petugas + ", petugas2 " + petugas2 + ", petugas3 " + petugas3);

					parameters.put("petugas", petugas == null ? "" : petugas.getNama());
					parameters.put("petugas_nip", petugas == null ? "" : petugas.getMycode());

					parameters.put("petugas2", petugas2 == null ? "" : petugas2.getNama());
					parameters.put("petugas_nip2", petugas2 == null ? "" : petugas2.getMycode());

					parameters.put("petugas3", petugas3 == null ? "" : petugas3.getNama());
					parameters.put("petugas_nip3", petugas3 == null ? "" : petugas3.getMycode());

					parameters.put("petugas4", petugas4 == null ? "" : petugas4.getNama());
					parameters.put("petugas_nip4", petugas4 == null ? "" : petugas4.getMycode());

					parameters.put("pjdosen", pj == null ? "" : pj.getNama());
					parameters.put("pjdosen_nip", pj == null ? "" : pj.getMycode());

					parameters.put("pengawas_ujian",
							(petugas == null ? "" : petugas.getNama()) + " "
									+ (petugas2 == null ? "" : petugas2.getNama()) + " "
									+ (petugas3 == null ? "" : petugas3.getNama()));

					parameters.put("catatan", pertemuan == null ? null : pertemuan.getCatatan());

					parameters.put("tanggal_ujian",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null ? kelasPertemuan.getMulai()
									: pertemuan == null ? null : pertemuan.getTanggal());

					parameters.put("tanggal_ujian_format",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null
									? Common.dateFormat2.get().format(kelasPertemuan.getMulai())
									: pertemuan == null || pertemuan.getTanggal() == null ? null
											: Common.dateFormat2.get().format(pertemuan.getTanggal()));

					parameters.put("tanggal_ujian_tanggal",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null
									? Common.dateFormatTgl.get().format(kelasPertemuan.getMulai())
									: pertemuan == null || pertemuan.getTanggal() == null ? null
											: Common.dateFormatTgl.get().format(pertemuan.getTanggal()));

					parameters.put("tanggal_ujian_bulan",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null
									? Common.dateFormatBln.get().format(kelasPertemuan.getMulai())
									: pertemuan == null || pertemuan.getTanggal() == null ? null
											: Common.dateFormatBln.get().format(pertemuan.getTanggal()));

					parameters.put("tanggal_ujian_tahun",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null
									? Common.dateFormatThn.get().format(kelasPertemuan.getMulai())
									: pertemuan == null || pertemuan.getTanggal() == null ? null
											: Common.dateFormatThn.get().format(pertemuan.getTanggal()));

					parameters.put("tanggal_ujian_hari",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null
									? Common.dateFormatHari.get().format(kelasPertemuan.getMulai())
									: pertemuan == null || pertemuan.getTanggal() == null ? null
											: Common.dateFormatHari.get().format(pertemuan.getTanggal()));

					parameters.put("tanggal_lengkap",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null
									? Common.dateFormat6.get().format(kelasPertemuan.getMulai())
									: pertemuan == null || pertemuan.getTanggal() == null ? ""
											: Common.dateFormat6.get().format(pertemuan.getTanggal()));

					parameters.put("waktu", kelasPertemuan != null
							? (kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai()) + ""
									+ (kelasPertemuan.getMulai() == null ? ""
											: ", " + Common.dateFormat4.get().format(kelasPertemuan.getMulai()))

							: (pertemuan == null
									? (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai())
									: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()) + ""
											+ (pertemuan == null || pertemuan.getTanggal() == null ? ""
													: ", " + Common.dateFormat4.get().format(pertemuan.getTanggal()))));

					parameters.put("waktu_mulai", kelasPertemuan != null ? (kelasPertemuan.getWaktuMulai())
							: pertemuan == null ? (perkuliahan.getWaktuMulai()) : (pertemuan.getWaktuMulai()));

					parameters.put("waktu_selesai", kelasPertemuan != null ? (kelasPertemuan.getWaktuSelesai())
							: pertemuan == null ? (perkuliahan.getWaktuSelesai()) : (pertemuan.getWaktuSelesai()));

					parameters.put("waktu_aja",
							kelasPertemuan != null
									? (kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai())
									: pertemuan == null
											? (perkuliahan.getWaktuMulai() + " s.d " + perkuliahan.getWaktuSelesai())
											: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()));

					parameters.put("tanggal_aja",
							kelasPertemuan != null
									? ((kelasPertemuan.getMulai() == null ? ""
											: Common.dateFormat4.get().format(kelasPertemuan.getMulai())))
									: pertemuan == null ? ""
											: (pertemuan.getTanggal() == null ? ""
													: Common.dateFormat4.get().format(pertemuan.getTanggal())));
					parameters.put("ruang", kelasPertemuan != null
							? (kelasPertemuan.getRuang() == null ? "" : kelasPertemuan.getRuang().getKodeRuangan())
							: pertemuan == null
									? (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getKodeRuangan())
									: (pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getKodeRuangan()));

					parameters.put("nama_ruang",
							kelasPertemuan != null
									? (kelasPertemuan.getRuang() == null ? "" : kelasPertemuan.getRuang().getNama())
									: pertemuan == null
											? (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama())
											: (pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getNama()));

					Map<String, Integer> statuses = pertemuan == null ? null : pertemuan.hitungStatus();
					int teraftar = (kelasPertemuan != null ? ((Number) session
							.createCriteria(DetailKelasPertemuan.class)
							.add(Restrictions.eq("kelasPertemuan", kelasPertemuan)).createCriteria("detailperkuliahan")
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI)).uniqueResult()).intValue()
							: ((Number) session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("perkuliahan", perkuliahan))
									.setProjection(Projections.rowCount())
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI)).uniqueResult())
									.intValue());
					int masuk = pertemuan == null || statuses.get("M") == null ? 0 : statuses.get("M");
					int tidakmasuk = teraftar - masuk;
					parameters.put("Terdaftar", teraftar);
					parameters.put("Masuk", masuk);
					parameters.put("tidakmasuk", tidakmasuk);

					List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session);

					int rowIndex = 0;
					List<Long> detailperkuliahans = kelasPertemuan != null
							? session.createCriteria(DetailKelasPertemuan.class)
									.add(Restrictions.eq("kelasPertemuan", kelasPertemuan))
									.setProjection(Projections.property("detailperkuliahan.id"))
									.createCriteria("detailperkuliahan").add(Restrictions.isNull("ikutiPerkuliahan"))
									.createAlias("mahasiswa", "mahasiswa").addOrder(Order.asc("mahasiswa.nim"))
									.add(Restrictions.eq("perkuliahan", perkuliahan))
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.createCriteria("perkuliahan", Criteria.LEFT_JOIN)

									.add(Restrictions.eq("semester", perkuliahan.getSemester()))

									.list()
							: new ArrayList<Long>(perkuliahan.ambilDetailperkuliahan());

					List<String> statusPertemuan;
					statusPertemuan = new ArrayList<String>();

					if (pertemuan != null && pertemuan.getAbsensi() != null
							&& !pertemuan.getAbsensi().trim().isEmpty()) {
						statusPertemuan.add(pertemuan.getAbsensi());
					}

					int t_total_laki = 0;
					int m_total_laki = 0;
					int s_total_laki = 0;
					int i_total_laki = 0;
					int a_total_laki = 0;

					int t_total_perempuan = 0;
					int m_total_perempuan = 0;
					int s_total_perempuan = 0;
					int i_total_perempuan = 0;
					int a_total_perempuan = 0;

					boolean check_pembayaran_mahasiswa = Common.bolehKonfigurasi("saat_cetak_absensi_ujian_check_pembayaran_mahasiswa", Konfigurasi.TIDAK_AKTIF);

					List<SyaratUjian> syaratUjians = null;
					if (check_pembayaran_mahasiswa) {
						if (ujian.equalsIgnoreCase("uts")) {
							syaratUjians = ConstantValues.simpleList(
									HibernateUtil.currentSession().createCriteria(SyaratUjian.class)
											.add(Restrictions.eq("uts", true)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									SyaratUjian.class);
						} else if (ujian.equalsIgnoreCase("uas")) {
							syaratUjians = ConstantValues.simpleList(
									HibernateUtil.currentSession().createCriteria(SyaratUjian.class)
											.add(Restrictions.eq("uas", true)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									SyaratUjian.class);
						}
						System.out.println("syaratUjians => " + syaratUjians);
					}

					int jumlahHadir = 0;
					int jumlahIzin = 0;
					int jumlahSakit = 0;
					int jumlahAlpha = 0;

					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
								rowIndex++;
								try {
									label.setValue("Sedang memproses data "
											+ detailperkuliahan.getMahasiswa().toString()
											+ " untuk proses cetak absensi " + ujian + " (" + Common.numberFormat.get()
													.format(rowIndex * 100.0 / detailperkuliahans.size())
											+ " %)");

									KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(
											detailperkuliahan.getMahasiswa(), perkuliahan.getSemester(), null,
											perkuliahan.getStatusSemesterPendek());

									if (check_pembayaran_mahasiswa) {
										Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
										if (ujian.equalsIgnoreCase("uts")) {

											List<String> warnings = new ArrayList<String>();
											if (mahasiswa != null && syaratUjians != null) {

												for (SyaratUjian syaratUjian : syaratUjians) {
													SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null,
															mahasiswa, detailperkuliahan.getSemester(),
															"Cetak Kartu UTS", krsMahasiswa, warnings);
												}
											}
											if (!warnings.isEmpty()) {
												continue;
											}

										} else if (ujian.equalsIgnoreCase("uas")) {

											List<String> warnings = new ArrayList<String>();
											if (mahasiswa != null && syaratUjians != null) {

												for (SyaratUjian syaratUjian : syaratUjians) {
													SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null,
															mahasiswa, detailperkuliahan.getSemester(),
															"Cetak Kartu UAS", krsMahasiswa, warnings);
												}
											}
											if (!warnings.isEmpty()) {
												continue;
											}

										}
									}

									Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

									for (JenisKegiatan jenisKegiatan : ais.common.CommonHelperClass.jenisKegiatansUntukSyaratUjian) {
										Kegiatan kegiatan = detailperkuliahan.getMahasiswa()
												.ambilKegiatans(detailperkuliahan.getSemester(), jenisKegiatan);
										if (kegiatan != null) {
											map.put("persen_" + jenisKegiatan.getKode(), kegiatan.getPersentaseLunas());
											map.put("dibayar_" + jenisKegiatan.getKode(), kegiatan.getAmount());
											map.put("belum_dibayar_" + jenisKegiatan.getKode(),
													kegiatan.getAmountTerhutang());
											map.put("total_" + jenisKegiatan.getKode(),
													kegiatan.getAmount() + kegiatan.getAmountTerhutang());
										}
									}

									boolean baypass = Common.checkBaypassStatusPembayaranMahasiswa(
											krsMahasiswa.getSemester(), null, detailperkuliahan.getMahasiswa(),
											ais.common.CommonHelperClass.jenisKegiatansUntukSyaratUjian);
									map.put("baypass_pembayaran", baypass);
									if (ujian.equalsIgnoreCase("uts")) {
										map.put("nomor_ujian", krsMahasiswa.getNoUts());
									} else if (ujian.equalsIgnoreCase("uas")) {
										map.put("nomor_ujian", krsMahasiswa.getNoUas());
									}

									map.put("nim", detailperkuliahan.getMahasiswa().getNim() == null ? ""
											: detailperkuliahan.getMahasiswa().getNim());
									map.put("nama", detailperkuliahan.getMahasiswa().getNama() == null ? ""
											: detailperkuliahan.getMahasiswa().getNama().toUpperCase());
									map.put("kode_matakuliah", detailperkuliahan.getPerkuliahan() == null
											|| detailperkuliahan.getPerkuliahan().getMatakuliah() == null
											|| detailperkuliahan.getPerkuliahan().getMatakuliah().getKode() == null ? ""
													: detailperkuliahan.getPerkuliahan().getMatakuliah().getKode());

									for (FormatNilai formatNilai : formatNilais) {
										if (formatNilai == null) continue;
										Double nilai = detailperkuliahan.retreiveDetailNilai(formatNilai);
										String namaFormat = formatNilai.getNama() == null ? "" : formatNilai.getNama();
										if (namaFormat.toLowerCase().contains(ujian.toLowerCase())
												&& nilai != null && nilai.doubleValue() > 0.1) {
											map.put("nilai", nilai);
										}
										map.put("nilai_" + namaFormat, nilai == null ? Double.valueOf(0.0) : nilai);
									}
									map.put("nilai_total", detailperkuliahan.getTotalNilai());
									map.put("nilai_huruf", detailperkuliahan.getNilaiHuruf());

									Jurusan jurusanLaporan = detailperkuliahan.getMahasiswa().getJurusan();
									Fakultas fakultasLaporan = jurusanLaporan == null ? null : jurusanLaporan.getFakultas();
									Dosen dekanLaporan = fakultasLaporan == null ? null : fakultasLaporan.getDekan();
									Dosen kaprodiLaporan = jurusanLaporan == null ? null : jurusanLaporan.getKaprodi();
									map.put("nip_kajur", dekanLaporan == null || dekanLaporan.getCode() == null ? "" : dekanLaporan.getCode());
									map.put("nama_kajur", dekanLaporan == null || dekanLaporan.getNama() == null ? "" : dekanLaporan.getNama());
									map.put("nip_kaprodi", kaprodiLaporan == null || kaprodiLaporan.getCode() == null ? "" : kaprodiLaporan.getCode());
									map.put("nama_kaprodi", kaprodiLaporan == null || kaprodiLaporan.getNama() == null ? "" : kaprodiLaporan.getNama());
									map.put("id_fakultas", fakultasLaporan == null ? null : fakultasLaporan.getId());

									try {
										map.put("keterangan",
												pertemuan == null ? ""
														: pertemuan.retreiveAbsensiKeterangan(
																detailperkuliahan.getMahasiswa().getId()));
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:11256");
										// TODO: handle exception
									}

									Map<String, Integer> statusesq = Perkuliahan.hitungStatus(statusPertemuan,
											detailperkuliahan.getMahasiswa().getId());

									Statusabsensi statusabsensi = (Statusabsensi) (pertemuan == null ? null
											: ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan
													.retreiveAbsensiId(detailperkuliahan.getMahasiswa().getId())));

									if (statusabsensi != null) {
										jumlahHadir += statusabsensi.getKode().equalsIgnoreCase("M") ? 1 : 0;
										jumlahIzin += statusabsensi.getKode().equalsIgnoreCase("I") ? 1 : 0;
										jumlahSakit += statusabsensi.getKode().equalsIgnoreCase("S") ? 1 : 0;
										jumlahAlpha += statusabsensi.getKode().equalsIgnoreCase("A") ? 1 : 0;
									}

									int t = statusesq.get("T") == null ? 0 : statusesq.get("T");
									int m = statusesq.get("M") == null ? 0 : statusesq.get("M");
									int s = statusesq.get("S") == null ? 0 : statusesq.get("S");
									int i = statusesq.get("I") == null ? 0 : statusesq.get("I");
									int a = statusesq.get("A") == null ? 0 : statusesq.get("A");

									try {

										map.put("m", m);
										map.put("t", t);
										map.put("s", s);
										map.put("i", i);
										map.put("a", a);

										if (detailperkuliahan.getMahasiswa().getKelamin()
												.equalsIgnoreCase("Laki-laki")) {
											t_total_laki += t;
											m_total_laki += m;
											s_total_laki += s;
											i_total_laki += i;
											a_total_laki += a;
										} else {
											t_total_perempuan += t;
											m_total_perempuan += m;
											s_total_perempuan += s;
											i_total_perempuan += i;
											a_total_perempuan += a;
										}

									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:11303");
										// TODO: handle exception
									}

									mapsList.add(map);

									if (m == 0) {
										mapsListTidakhadir.add(map);
									}

								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11314");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
							}

							parameters.put("t_total_laki", t_total_laki);
							parameters.put("m_total_laki", m_total_laki);
							parameters.put("s_total_laki", s_total_laki);
							parameters.put("i_total_laki", i_total_laki);
							parameters.put("a_total_laki", a_total_laki);
							parameters.put("t_total_perempuan", t_total_perempuan);
							parameters.put("m_total_perempuan", m_total_perempuan);
							parameters.put("s_total_perempuan", s_total_perempuan);
							parameters.put("i_total_perempuan", i_total_perempuan);
							parameters.put("a_total_perempuan", a_total_perempuan);
						}
					}

					parameters.put("pertemuan_ke", pertemuan == null ? 0 : pertemuan.getPertemuanKe());
					parameters.put("jumlahHadir", jumlahHadir);
					parameters.put("jumlahIzin", jumlahIzin);
					parameters.put("jumlahSakit", jumlahSakit);
					parameters.put("jumlahAlpha", jumlahAlpha);
					parameters.put("catatan_dosen", pertemuan == null ? "" : pertemuan.getCatatan());
					parameters.put("topik", pertemuan == null ? "" : pertemuan.getTopik());
					parameters.put("jenis_perkuliahan", pertemuan == null ? ""
							: pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());
					parameters.put("tanggal",
							pertemuan == null ? ""
									: Common.dateFormat2.get()
											.format(pertemuan.getTanggalRealisasi() == null ? pertemuan.getTanggal()
													: pertemuan.getTanggalRealisasi()));
					parameters.put("hari",
							pertemuan == null ? ""
									: Common.dateFormatHari.get()
											.format(pertemuan.getTanggalRealisasi() == null ? pertemuan.getTanggal()
													: pertemuan.getTanggalRealisasi()));
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					ais.action.report.Report.closeCurrentSessionQuietly();

					String tahunAkademik = perkuliahan == null ? null : perkuliahan.getTahunAjaran();

					parameters.put("bar",
							"3-" + tahunAkademik + "-" + perkuliahan.getSemester() + "-" + perkuliahan.getId());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11362");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				maps.put(perkuliahan.getId(), mapsList);
				maps.put(-perkuliahan.getId(), mapsListTidakhadir);

				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanAbsensi(final JadwalPelajaran jadwalPelajaran, final String ujian,
			final KelasPertemuan kelasPertemuan) throws Exception {

		final List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		final Map parameters = ais.common.HashMapGenerator.getRand();

		final Label label = Common.displayLoadBar(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot(),
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						parameters.put("maps", maps);

						Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF,
								new Map[] { parameters, parameters, parameters },
								new String[] { "LaporanAbsensiUTS", "CoverLaporanAbsensiUTS",
										"BeritaAcaraLaporanAbsensiUTS" },
								new String[] { "Daftar Hadir " + ujian, "Cover " + ujian, "Berita Acara " + ujian },
								ais.ui.util.WaktuUtil.getDate());

						Tabpanels tabpanels = tabbox.getTabpanels();
						Tabs tabs = tabbox.getTabs();

						final MyTabConfig tabPenilaian = new MyTabConfig("Rekap Kehadiran");
						tabPenilaian.setParent(tabs);

						final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();

						tabpanelPenilaian.setParent(tabpanels);
						tabpanelPenilaian.setHeight("650px");
						tabPenilaian.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelPenilaian.getChildren().size() == 0) {
									DashboardRekapAbsensiPerSiswa dashboardRekapAbsensiSiswa = new DashboardRekapAbsensiPerSiswa(
											jadwalPelajaran);
									tabpanelPenilaian.appendChild(dashboardRekapAbsensiSiswa);
								}
							}
						});

					}
				});

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					if (jadwalPelajaran != null) {
						Common.insertProperty(JadwalPelajaran.class, jadwalPelajaran, parameters, "jadwalPelajaran");
					}

					parameters.put("jadwalPelajaran", jadwalPelajaran.getId());
					parameters
							.put("kelas",
									kelasPertemuan != null
											? (jadwalPelajaran.getSemester() + " "
													+ (kelasPertemuan.getNama() == null ? ""
															: kelasPertemuan.getNama()))
											: jadwalPelajaran.getSemester() + " "
													+ (jadwalPelajaran.getKelas() == null ? ""
															: jadwalPelajaran.getKelas()));

					parameters.put("sekolah", jadwalPelajaran.getSekolah().getNama());
					parameters.put("yayasan", jadwalPelajaran.getYayasan().getNama());
					parameters.put("semester", jadwalPelajaran.getSemester());

					parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
					parameters.put("tampil_nilai", 1);

					parameters.put("jenis_semester",
							((Integer) jadwalPelajaran.getSemester()) % 2 == 0 ? JadwalPelajaran.GENAP
									: JadwalPelajaran.GANJIL);
					parameters.put("tahun_ajaran", jadwalPelajaran.getTahunAjaran());
					parameters.put("kode_matapelajaran", jadwalPelajaran.getMatapelajaran().getKode());
					parameters.put("nama_matapelajaran", jadwalPelajaran.getMatapelajaran().getNama());

					List<Guru> dataGurus = jadwalPelajaran.populateGuruBuNama();
					if (dataGurus.size() > 1) {
						String guruPengampu = "";
						for (Guru guru : dataGurus) {
							guruPengampu += guruPengampu.isEmpty() ? guru.getNama() : ", " + guru.getNama();
						}
						parameters.put("guru", guruPengampu);
					} else {
						parameters.put("guru",
								jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNama());
					}

					parameters.put("nip_guru",
							jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNip());

					parameters.put("col1", ujian);
					parameters.put("ujian", ujian);

					Session session = ais.action.report.Report.openNativeSession();

					Pertemuan pertemuan = kelasPertemuan != null ? kelasPertemuan.getPertemuan()
							: (Pertemuan) session.createCriteria(Pertemuan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.addOrder(Order.desc("id")).createAlias("statusPertemuan", "statusPertemuan")
									.add(Restrictions.eq("statusPertemuan.nama", ujian))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).setMaxResults(1)
									.uniqueResult();

					Pegawai petugas = kelasPertemuan != null
							? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas())
							: (Pegawai) (pertemuan == null || pertemuan.getPetugas() == null ? null
									: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas()));

					Pegawai petugas2 = kelasPertemuan != null
							? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas2())
							: (Pegawai) (pertemuan == null || pertemuan.getPetugas2() == null ? null
									: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas2()));

					Pegawai petugas3 = kelasPertemuan != null
							? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas3())
							: (Pegawai) (pertemuan == null || pertemuan.getPetugas3() == null ? null
									: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas3()));

					Pegawai petugas4 = kelasPertemuan != null
							? (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), kelasPertemuan.getPetugas4())
							: (Pegawai) (pertemuan == null || pertemuan.getPetugas4() == null ? null
									: ConstantValues.ambil(Pegawai.class.getName(), pertemuan.getPetugas4()));

					Dosen pjawabDosen = kelasPertemuan != null
							? (Dosen) ConstantValues.ambil(Dosen.class.getName(), kelasPertemuan.getPjDosen())
							: (Dosen) (pertemuan == null || pertemuan.getPjDosen() == null ? null
									: ConstantValues.ambil(Dosen.class.getName(), pertemuan.getPjDosen()));

					if (petugas != null) {
						LampiranLain lam = LampiranLain.ambil(petugas.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11521");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas", ttd);
							}
						} else if (petugas.getDosen() != null) {
							lam = LampiranLain.ambil(petugas.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11538");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas", ttd);
								}
							}
						}
					}
					if (petugas2 != null) {
						LampiranLain lam = LampiranLain.ambil(petugas2.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11558");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas2", ttd);
							}
						} else if (petugas2.getDosen() != null) {
							lam = LampiranLain.ambil(petugas2.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11575");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas2", ttd);
								}
							}
						}
					}
					if (petugas3 != null) {
						LampiranLain lam = LampiranLain.ambil(petugas3.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11595");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas3", ttd);
							}
						} else if (petugas3.getDosen() != null) {
							lam = LampiranLain.ambil(petugas3.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11612");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas3", ttd);
								}
							}
						}
					}

					if (petugas4 != null) {
						LampiranLain lam = LampiranLain.ambil(petugas4.getId(), LampiranLain.TTD_PEGAWAI);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11633");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								parameters.put("ttd_petugas4", ttd);
							}
						} else if (petugas4.getDosen() != null) {
							lam = LampiranLain.ambil(petugas4.getDosen().getId(), LampiranLain.TTD_DOSEN);
							nama = lam == null ? null : lam.getNama();

							if (nama != null) {
								if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
										|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
										|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
									String ttd = "";
									try {
										ttd = lam.ambilFile().getAbsolutePath();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11650");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}

									parameters.put("ttd_petugas4", ttd);
								}
							}
						}
					}

					if (pjawabDosen != null) {
						LampiranLain lam = LampiranLain.ambil(pjawabDosen.getId(), LampiranLain.TTD_DOSEN);
						String nama = lam == null ? null : lam.getNama();

						if (nama != null) {
							if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
									|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
									|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
								String ttd = "";
								try {
									ttd = lam.ambilFile().getAbsolutePath();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11671");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
								parameters.put("ttd_pjdosen", ttd);
							}
						}
					}

					System.out.println("petugas => " + petugas);

					parameters.put("petugas", petugas == null ? "" : petugas.getNama());
					parameters.put("petugas_nip", petugas == null ? "" : petugas.getMycode());

					parameters.put("petugas2", petugas2 == null ? "" : petugas2.getNama());
					parameters.put("petugas_nip2", petugas2 == null ? "" : petugas2.getMycode());

					parameters.put("petugas3", petugas3 == null ? "" : petugas3.getNama());
					parameters.put("petugas_nip3", petugas3 == null ? "" : petugas3.getMycode());

					parameters.put("petugas3", petugas3 == null ? "" : petugas3.getNama());
					parameters.put("petugas_nip3", petugas3 == null ? "" : petugas3.getMycode());

					parameters.put("petugas4", petugas4 == null ? "" : petugas4.getNama());
					parameters.put("petugas_nip4", petugas4 == null ? "" : petugas4.getMycode());

					parameters.put("pjdosen", pjawabDosen == null ? "" : pjawabDosen.getNama());
					parameters.put("pjdosen_nip", pjawabDosen == null ? "" : pjawabDosen.getMycode());

					parameters.put("pengawas_ujian",
							(petugas == null ? "" : petugas.getNama()) + " "
									+ (petugas2 == null ? "" : petugas2.getNama()) + " "
									+ (petugas3 == null ? "" : petugas3.getNama()));

					parameters.put("tanggal_ujian",
							kelasPertemuan != null && kelasPertemuan.getMulai() != null ? kelasPertemuan.getMulai()
									: pertemuan == null ? null : pertemuan.getTanggal());
					parameters.put("waktu", kelasPertemuan != null
							? (kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai()) + ""
									+ (kelasPertemuan.getMulai() == null ? ""
											: ", " + Common.dateFormat4.get().format(kelasPertemuan.getMulai()))

							: (pertemuan == null
									? (jadwalPelajaran.getJamPelajaran().getMulaiS() + " s.d "
											+ jadwalPelajaran.getJamPelajaran().getSampaiS())
									: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()) + ""
											+ (pertemuan.getTanggal() == null ? ""
													: ", " + Common.dateFormat4.get().format(pertemuan.getTanggal()))));

					parameters.put("waktu_aja",
							kelasPertemuan != null
									? (kelasPertemuan.getWaktuMulai() + " s.d " + kelasPertemuan.getWaktuSelesai())
									: pertemuan == null
											? (jadwalPelajaran.getJamPelajaran().getMulaiS() + " s.d "
													+ jadwalPelajaran.getJamPelajaran().getSampaiS())
											: (pertemuan.getWaktuMulai() + " s.d " + pertemuan.getWaktuSelesai()));

					parameters.put("tanggal_aja",
							kelasPertemuan != null
									? ((kelasPertemuan.getMulai() == null ? ""
											: Common.dateFormat4.get().format(kelasPertemuan.getMulai())))
									: pertemuan == null ? ""
											: (pertemuan.getTanggal() == null ? ""
													: Common.dateFormat4.get().format(pertemuan.getTanggal())));
					parameters.put("ruang", kelasPertemuan != null
							? (kelasPertemuan.getRuang() == null ? "" : kelasPertemuan.getRuang().getKodeRuangan())
							: pertemuan == null
									? (jadwalPelajaran.getKelas().getRuang() == null ? ""
											: jadwalPelajaran.getKelas().getRuang().getKodeRuangan())
									: (pertemuan.getRuang() == null ? "" : pertemuan.getRuang().getKodeRuangan()));

					Map<String, Integer> statuses = pertemuan == null ? null : pertemuan.hitungStatus();
					int teraftar = (kelasPertemuan != null
							? ((Number) session.createCriteria(DetailKelasPertemuan.class)
									.add(Restrictions.eq("kelasPertemuan", kelasPertemuan))
									.createCriteria("kelasSiswaPunyaSiswa")
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.setProjection(Projections.rowCount())
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.uniqueResult()).intValue()
							: ((Number) session.createCriteria(KelasSiswaPunyaSiswa.class)
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.setProjection(Projections.rowCount())
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.uniqueResult()).intValue());
					int masuk = pertemuan == null || statuses.get("M") == null ? 0 : statuses.get("M");
					int tidakmasuk = teraftar - masuk;
					parameters.put("Terdaftar", teraftar);
					parameters.put("Masuk", masuk);
					parameters.put("tidakmasuk", tidakmasuk);

					int rowIndex = 0;
					List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = kelasPertemuan != null
							? session.createCriteria(DetailKelasPertemuan.class)
									.add(Restrictions.eq("kelasPertemuan", kelasPertemuan))
									.setProjection(Projections.property("kelasSiswaPunyaSiswa"))
									.createCriteria("kelasSiswaPunyaSiswa")
									.add(Restrictions.isNull("ikutiJadwalPelajaran")).createAlias("siswa", "siswa")
									.addOrder(Order.asc("siswa.nim"))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createCriteria("jadwalPelajaran", Criteria.LEFT_JOIN)

									.add(Restrictions.eq("semester", jadwalPelajaran.getSemester()))

									.list()
							: session.createCriteria(KelasSiswaPunyaSiswa.class)
									.add(Restrictions.isNull("ikutiJadwalPelajaran")).createAlias("siswa", "siswa")
									.addOrder(Order.asc("siswa.nim"))
									.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createCriteria("jadwalPelajaran", Criteria.LEFT_JOIN)

									.add(Restrictions.eq("semester", jadwalPelajaran.getSemester()))

									.list();

					for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
						rowIndex++;
						try {
							label.setValue("Sedang memproses data " + kelasSiswaPunyaSiswa.getSiswa().toString()
									+ " untuk proses cetak absensi " + ujian + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / kelasSiswaPunyaSiswas.size())
									+ " %)");

							Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();

							if (ujian.equalsIgnoreCase("uts")) {
								map.put("nomor_ujian", kelasSiswaPunyaSiswa.getNoUts());
							} else if (ujian.equalsIgnoreCase("uas")) {
								map.put("nomor_ujian", kelasSiswaPunyaSiswa.getNoUas());
							}

							map.put("nim", kelasSiswaPunyaSiswa.getSiswa().getNim());
							map.put("nama", kelasSiswaPunyaSiswa.getSiswa().getNama().toUpperCase());
							map.put("kode_matapelajaran", jadwalPelajaran.getMatapelajaran().getKode());

//							if (formatNilai != null) {
//								String nilai1 = kelasSiswaPunyaSiswa.retreiveDetailNilai(formatNilai,
//										jadwalPelajaran.getMatapelajaran(), jadwalPelajaran.getSemester());
//								map.put("nilai", (nilai1));
//							}

							maps.add(map);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:11813");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});

						}
					}

					parameters.put("pertemuan_ke", pertemuan.getPertemuanKe());
//					parameters.put("jumlahHadir", jumlahHadir);
//					parameters.put("jumlahIzin", jumlahIzin);
//					parameters.put("jumlahSakit", jumlahSakit);
//					parameters.put("jumlahAlpha", jumlahAlpha);
					parameters.put("catatan_dosen", pertemuan.getCatatan());
					parameters.put("topik", pertemuan.getTopik());
					parameters.put("jenis_perkuliahan",
							pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());
					parameters.put("tanggal",
							Common.dateFormat2.get()
									.format(pertemuan.getTanggalRealisasi() == null ? pertemuan.getTanggal()
											: pertemuan.getTanggalRealisasi()));
					parameters.put("hari",
							Common.dateFormatHari.get()
									.format(pertemuan.getTanggalRealisasi() == null ? pertemuan.getTanggal()
											: pertemuan.getTanggalRealisasi()));

					ais.action.report.Report.closeCurrentSessionQuietly();

					String tahunAkademik = jadwalPelajaran.getTahunAjaran();

					parameters.put("bar",
							"3-" + tahunAkademik + "-" + jadwalPelajaran.getSemester() + "-" + jadwalPelajaran.getId());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:11843");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}

	@SuppressWarnings("rawtypes")
	public static Map parameterCetakUTS(Mahasiswa mahasiswa, int semester, Integer tahapan, Integer semesterPendek,
			boolean hitungUang, Integer sp, boolean remedial, Date tanggal, Date tanggalDicetak,
			Set<Long> longsHasilTidak) throws Exception {
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek);

		Session session = ais.action.report.Report.openNativeSession();
		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
				.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).uniqueResult();
		ais.action.report.Report.closeNativeSession(session);
		// TODO: Cetak

		Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		try {
			FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA,
					LampiranLain.class);
			// Guard: mahasiswa belum pernah unggah tanda tangan -> ambil() bisa
			// mengembalikan null (belum ada lampiran), begitu juga ambilFile() bisa
			// null bila record ada tapi berkas fisiknya belum tersedia. Lewati saja
			// bagian ttd_mahasiswa jika salah satu null, jangan sampai NPE mentah.
			if (lampiranLain != null) {
				File file = lampiranLain.ambilFile();
				if (file != null && file.exists()) {
					parameters.put("ttd_mahasiswa", file.getAbsolutePath());
				}
				file = null;
			}
			lampiranLain = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:11874");
//			e.printStackTrace();
		}
		parameters.put("remedial", remedial);
		parameters.put("semester", semester);
		parameters.put("semesterNext", semester);
		parameters.put("tahapan", tahapan);
		parameters.put("mahasiswa", mahasiswa.getId());
		parameters.put("nim_mahasiswa", mahasiswa.getNim());
		parameters.put("semester_mahasiswa", semester);
		parameters.put("tahunAkademik_mahasiswa", krsMahasiswa.getTahunAkademik());
		parameters.put("tanggal", dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
		mahasiswa.putPhoto(parameters);
		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Jurusan jurusan = mahasiswa.getJurusan();
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("semester_pendek", krsMahasiswa.getSemesterPendek());
		parameters.put("namamahasiswa", mahasiswa.getNama());
		parameters.put("namafakultas", mahasiswa.getJurusan().getFakultas().getNama());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan().getFakultas(),
				mahasiswa.getJurusan(), mahasiswa.getProgram(), krsMahasiswa.getSemesterPendek());
		Double minip = pembatasanNilaiIPKUntukPengambilanKRS == null ? 0.0
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil();
		parameters.put("maksimum_sks", minip);
		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");
		parameters.put("nomor_ujian", krsMahasiswa.getNoUts());

		if (Common.bolehKonfigurasi("saat_cetak_kartu_uts_tidak_tampil_export", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("tidak_tampil_pilihan_export", "true");
		}

		if (krsMahasiswa != null)
			krsMahasiswa.masukkanData("cetak_kuts");

		parameters.put("maps", CommonReportHelper.generateMap(mahasiswa, semester, tahapan, semesterPendek, remedial,
				krsMahasiswa, true, false, true, false, longsHasilTidak));

		return parameters;
	}

	@SuppressWarnings("rawtypes")
	private static void prosesCetakUTS(Mahasiswa mahasiswa, Integer semester, Integer tahapan, Integer semesterPendek,
			Boolean remedial, String tahunAkademik, Set<Long> longsHasilTidak) throws Exception {

		West west = new West();
		west.setWidth("0px");

		boolean hitungUlang = false;
		Date tgl = new Date();
		Map parameters = CommonReportHelper.parameterCetakUTS(mahasiswa, semester, tahapan, semesterPendek, hitungUlang,
				semesterPendek, remedial, tgl, tgl, longsHasilTidak);

		Report.generatePDFReport("pdf", parameters, "Cetak_KUTS_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), west);

	}

	public static void cetakUAS(final Mahasiswa mahasiswa, final Integer semester, final Integer tahapan,
			final String tahunAkademik, final Integer semesterPendek, final Boolean remedial, final Boolean tanya)
			throws Exception {

		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null) {
			List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(SyaratUjian.class).add(Restrictions.eq("uas", true))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					SyaratUjian.class);

			System.out.println("syaratUjians => " + syaratUjians);

			for (SyaratUjian syaratUjian : syaratUjians) {
				SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Cetak Kartu UAS",
						warnings);
			}
		}
		if (!warnings.isEmpty()) {

			String w = "";
			for (String wa : warnings) {
				w += w.isEmpty() ? wa : "\n\n" + wa;
			}
			if (tanya) {

				MyMessageboxConfig.showFormatCb(
						"{V1}\n\n\nApakah Bapak/Ibu yakin akan tetap melanjutkan proses pencetakan Kartu Ujian Akhir Semester (UAS) ini?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									lanjutCetakUAS(mahasiswa, semester, tahapan, tahunAkademik, semesterPendek,
											remedial, tanya);

								}

							}
						}, w);

			} else {
				MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}

		} else {
			lanjutCetakUAS(mahasiswa, semester, tahapan, tahunAkademik, semesterPendek, remedial, tanya);
		}

	}

	public static void lanjutCetakUAS(final Mahasiswa mahasiswa, final Integer semester, final Integer tahapan,
			final String tahunAkademik, final Integer semesterPendek, final Boolean remedial, Boolean tanya)
			throws Exception {
		final Set<Long> longsHasilTidak = Common.checkStatusAbsensi(mahasiswa, semester, semesterPendek, "UAS");

		if (Common.bolehKonfigurasi("mahasiswa_harus_upload_foto_sebelum_ikut_ujian_uas", Konfigurasi.TIDAK_AKTIF)) {

			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			int fotoMahasiswa = ((Number) streamingSession.createCriteria(FotoMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			StreamingHibernateUtil.getInstance().closeSession();

			if (fotoMahasiswa == 0) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Mahasiswa dengan NIM {V1} belum dapat mencetak Kartu Ujian Akhir Semester (UAS) karena foto belum diunggah. Langkah yang dapat dilakukan: (1) unggah foto mahasiswa terlebih dahulu pada menu yang tersedia; (2) pastikan foto telah sesuai ketentuan; (3) ulangi proses pencetakan kartu ujian.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim());
				return;
			}
		}

		if (tanya) {

			if (!UtsDanUasCheckerHelper.checkItemBiayaPembayaranSebelumUASSudahMemenuhi(mahasiswa, semester, tahapan,
					semesterPendek, new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								prosesCetakUAS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik,
										longsHasilTidak);

							}
						}
					})) {
				return;
			}

			if (UtsDanUasCheckerHelper.checkPembayaranSebelumUASSudahMemenuhi(mahasiswa, semester, semesterPendek,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								prosesCetakUAS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik,
										longsHasilTidak);

							}
						}
					})) {
				prosesCetakUAS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik, longsHasilTidak);
			}
		} else {

			if (!UtsDanUasCheckerHelper.checkItemBiayaPembayaranSebelumUASSudahMemenuhi(mahasiswa, semester, tahapan,
					semesterPendek, null)) {
				return;
			}

			if (!UtsDanUasCheckerHelper.checkPembayaranSebelumUASSudahMemenuhi(mahasiswa, semester, semesterPendek,
					null)) {
				return;
			}
			prosesCetakUAS(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAkademik, longsHasilTidak);
		}
	}

	@SuppressWarnings("rawtypes")
		public static Map parameterCetakUAS(Mahasiswa mahasiswa, int semester, Integer tahapan, Integer semesterPendek,
				boolean hitungUang, boolean remedial, Date tanggal, Date tanggalDicetak, Set<Long> longsHasilTidak)
				throws Exception {
			if (mahasiswa == null) {
				throw new IllegalArgumentException("Mahasiswa belum dipilih untuk cetak UAS.");
			}
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, null);
			if (krsMahasiswa == null) {
				throw new IllegalArgumentException("KRS mahasiswa " + (mahasiswa.getNim() == null ? "" : mahasiswa.getNim())
						+ " semester " + semester + " belum ditemukan, sehingga kartu UAS belum bisa dicetak.");
			}

		// FIX NullPointerException (KE-4): openNativeSession() bisa saja mengembalikan null bila
		// koneksi native gagal dibuka (mis. gangguan sementara pool koneksi), dan mahasiswa.getJurusan()
		// bisa null (mahasiswa belum punya prodi). Sebelumnya kode langsung memakai keduanya tanpa
		// guard -> NPE mentah yg menggagalkan seluruh cetak UAS. jenjangProgramStudi tetap null bila
		// salah satu kondisi ini terjadi, sesuai perilaku semula (nilai ini hanya dipakai opsional di
		// bawah, bukan wajib).
		JenjangProgramStudi jenjangProgramStudi = null;
		Session session = ais.action.report.Report.openNativeSession();
		if (session != null) {
			try {
					if (mahasiswa.getJurusan() != null) {
						jenjangProgramStudi = (JenjangProgramStudi) session.createCriteria(JenjangProgramStudi.class)
								.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).setMaxResults(1).uniqueResult();
					}
			} finally {
				ais.action.report.Report.closeNativeSession(session);
			}
		}

		// TODO: Cetak
		Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		try {
			FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA,
					LampiranLain.class);
			File file = lampiranLain == null ? null : lampiranLain.ambilFile();
			if (file != null && file.exists()) {
				parameters.put("ttd_mahasiswa", file.getAbsolutePath());
			}
			file = null;
			lampiranLain = null;
		} catch (Exception e) {
//			e.printStackTrace();
		}
		parameters.put("remedial", remedial);
		parameters.put("semester", semester);
		parameters.put("semesterNext", semester);
		parameters.put("tahapan", tahapan);
		parameters.put("program", mahasiswa.getProgram());
		parameters.put("mahasiswa", mahasiswa.getId());
		parameters.put("nim_mahasiswa", mahasiswa.getNim());
		parameters.put("semester_mahasiswa", semester);
		parameters.put("tahunAkademik_mahasiswa", krsMahasiswa.getTahunAkademik());
		parameters.put("tanggal", dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
		mahasiswa.putPhoto(parameters);

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			// PERBAIKAN: mahasiswa.getJurusan() bisa null (mahasiswa belum punya prodi),
			// sama seperti kondisi yang sudah diguard di jenjangProgramStudi di atas --
			// cabang else ini sebelumnya langsung memanggil jurusan.getKaprodi() tanpa
			// cek null sehingga tetap bisa NPE walau fix KE-4 di atas sudah ada.
			Jurusan jurusan = mahasiswa.getJurusan();
			Dosen dosen = jurusan == null ? null : jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("semester_pendek", semesterPendek);
		parameters.put("namamahasiswa", mahasiswa.getNama());
		parameters.put("namafakultas", mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null
				? "" : mahasiswa.getJurusan().getFakultas().getNama());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan().getFakultas(),
				mahasiswa.getJurusan(), mahasiswa.getProgram(), semesterPendek);
		Double minip = pembatasanNilaiIPKUntukPengambilanKRS == null ? 0.0
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil();
		parameters.put("maksimum_sks", minip);

		parameters.put("nomor_ujian", krsMahasiswa.getNoUas());
		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

		if (Common.bolehKonfigurasi("saat_cetak_kartu_uas_tidak_tampil_export", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("tidak_tampil_pilihan_export", "true");
		}

		if (krsMahasiswa != null)
			krsMahasiswa.masukkanData("cetak_kuas");

		parameters.put("maps", CommonReportHelper.generateMap(mahasiswa, semester, tahapan, semesterPendek, remedial,
				krsMahasiswa, true, false, false, true, longsHasilTidak));
		return parameters;
	}

	@SuppressWarnings("rawtypes")
	private static void prosesCetakUAS(Mahasiswa mahasiswa, Integer semester, final Integer tahapan,
			Integer semesterPendek, Boolean remedial, String tahunAkademik, Set<Long> longsHasilTidak)
			throws Exception {

		Date tanggal = WaktuUtil.getDate();
		boolean hitungUang = false;
		Map parameters = CommonReportHelper.parameterCetakUAS(mahasiswa, semester, tahapan, semesterPendek, hitungUang,
				remedial, tanggal, tanggal, longsHasilTidak);

		West west = new West();
		west.setWidth("0px");

		Report.generatePDFReport("pdf", parameters, "Cetak_KUAS_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), west);

	}

	public static void prosesCetakKetAktif(Mahasiswa mahasiswa, String ta, String smt) throws Exception {
		Session session = HibernateUtil.currentSession();
		KlasifikasiSuratKeluar klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) ConstantValues.simpleObject(session
				.createCriteria(KlasifikasiSuratKeluar.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("aktifKuliah", true)).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).addOrder(Order.desc("id")), KlasifikasiSuratKeluar.class);

		if (klasifikasiSuratKeluar == null) {
			klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) ConstantValues
					.simpleObject(session.createCriteria(KlasifikasiSuratKeluar.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("aktifKuliah", true))
							.add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas())).setMaxResults(1)
							.addOrder(Order.desc("id")), KlasifikasiSuratKeluar.class);
		}

		if (klasifikasiSuratKeluar == null) {
			klasifikasiSuratKeluar = (KlasifikasiSuratKeluar) ConstantValues.simpleObject(
					session.createCriteria(KlasifikasiSuratKeluar.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("aktifKuliah", true)).setMaxResults(1).addOrder(Order.desc("id")),
					KlasifikasiSuratKeluar.class);
		}

		if (klasifikasiSuratKeluar == null) {
			MyMessageboxConfig.show("Surat keterangan aktif kulaih belum di sesuaikan. harap menghubungi admin.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		SuratKeluar suratKeluar = (SuratKeluar) session.createCriteria(SuratKeluar.class)
				.add(Restrictions.eq("tahunAkademik", ta)).add(Restrictions.eq("semester", smt))
				.add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)).addOrder(Order.desc("id"))
				.setMaxResults(1).uniqueResult();

		if (suratKeluar == null) {
			suratKeluar = new SuratKeluar();
		}
		suratKeluar.setMahasiswa(mahasiswa);
		suratKeluar.setKlasifikasiSuratKeluar(klasifikasiSuratKeluar);

		SuratKeluarAction.onAddExternal(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		}, suratKeluar, true, ta, smt);
	}

	public static void onCetakOrganisasiDosen(Dosen dosen) throws Exception {
		Report.generatePDFReport(Report.PDF,
				LaporanOrganisasiDosen.generateParameter(dosen, ais.ui.util.WaktuUtil.getDate()), "Organisasi_Dosen",
				ais.ui.util.WaktuUtil.getDate());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onLaporanAbsensi(final AsramaSiswa asramaSiswa, final Boolean tampiNilai) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("asramaSiswa", asramaSiswa.getId());

				Map parametersCover = ais.common.HashMapGenerator.getRand();
				parametersCover.put("asramaSiswa", asramaSiswa == null || asramaSiswa.getId() == null ? -1 : asramaSiswa.getId());

				@SuppressWarnings("unused")
				Tabbox tabbox = Report.generatePDFReportKembaliTab(Report.PDF,
						new Map[] { parameters, parameters, parameters, parametersCover, parameters },
						new String[] { "LaporanAbsensiLanscape", "LaporanAbsensiLanscape1", "LaporanAbsensi",
								"LaporanCoverAbsensi", "LaporanAbsensiLanscapeTotal" },
						new String[] { "Lanscape", "Lanscape Tgl", "Portrait", "Cover", "Rekap Masuk" },
						ais.ui.util.WaktuUtil.getDate());

			}
		});
	}

	@SuppressWarnings({ "deprecation" })
	public static void prosesSuratTagihan(final Siswa siswa, final Integer bulan, final Integer tahun)
			throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		final West west = new West();
		west.setWidth(tbmuser != null && tbmuser.getSiswa() == null ? "300px" : "0px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		final Combobox tahunAjaran = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAjaran);
		row.appendChild(tahunAjaran);
		tahunAjaran.setReadonly(true);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		final ParameterUmum parameterUmumTanggalJatuhTempo = Common.getParameterUmum(
				"tanggal jatuh tempo surat tagihan " + siswa.getNim(),
				Common.dateFormat1.get().format(calendar.getTime()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Jatuh Tempo"));
		final MyDatebox tanggalJatuhTempo;

		Date tglJatuhTempoVal = new Date();
		try {
			if (parameterUmumTanggalJatuhTempo.getNilai() != null
					&& !parameterUmumTanggalJatuhTempo.getNilai().trim().isEmpty()) {
				tglJatuhTempoVal = Common.dateFormat1.get().parse(parameterUmumTanggalJatuhTempo.getNilai());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:12311");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		row.appendChild(tanggalJatuhTempo = new MyDatebox(tglJatuhTempoVal));

		tanggalJatuhTempo.setReadonly(true);
		tanggalJatuhTempo.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumTanggalJatuhTempo.setNilai(Common.dateFormat1.get().format(tanggalJatuhTempo.getValue()));
				Common.refreshUpdate(parameterUmumTanggalJatuhTempo);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		final MyTextbox nomor;
		final ParameterUmum parameterUmum = Common.getParameterUmum("surat tagihan " + siswa.getNim(), "");
		row.appendChild(nomor = new MyTextbox(parameterUmum.getNilai() == null ? "" : parameterUmum.getNilai()));
		nomor.setWidth("90%");
		nomor.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmum.setNilai(nomor.getValue());
				Common.refreshUpdate(parameterUmum);
			}
		});

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_cara_pembayaran_di_surat_tagihan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));
		final Combobox jenisPembayaran;
		row.appendChild(jenisPembayaran = new Combobox());

		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertComboDanSemua(jenisPembayaran, new String[] { "nama", "bank" }, "akun", JenisPembayaran.class,
				"== Tidak menggunakan cara pembayaran ==",
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		final ParameterUmum parameterUmumRekeningPembayaran = Common
				.getParameterUmum("bank surat tagihan " + siswa.getNim(), "");

		Session session = null;
		JenisPembayaran selectedJenisPembayaran = null;
		try {
			session = ais.action.report.Report.openNativeSession();
			String nilaiRekening = parameterUmumRekeningPembayaran.getNilai();
			if (nilaiRekening != null && !nilaiRekening.trim().isEmpty() && Common.isNumber(nilaiRekening)) {
				selectedJenisPembayaran = (JenisPembayaran) session.createCriteria(JenisPembayaran.class)
						.add(Restrictions.idEq(Long.parseLong(nilaiRekening))).uniqueResult();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:12368");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:12373");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}

		Common.selectComboItem(jenisPembayaran, selectedJenisPembayaran);
		jenisPembayaran.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) (jenisPembayaran.getSelectedItem() == null
						? null
						: jenisPembayaran.getSelectedItem().getValue());
				parameterUmumRekeningPembayaran
						.setNilai(pilihanJenisPembayaran == null ? "" : pilihanJenisPembayaran.getId().toString());
				Common.refreshUpdate(parameterUmumRekeningPembayaran);
			}
		});
		jenisPembayaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Denda"));
		final MyDoublebox denda;
		final ParameterUmum parameterUmumDenda = Common.getParameterUmum("denda surat tagihan " + siswa.getNim(),
				"0.0");

		Double valDenda = 0.0;
		try {
			if (parameterUmumDenda.getNilai() != null && !parameterUmumDenda.getNilai().trim().isEmpty()) {
				valDenda = Double.parseDouble(parameterUmumDenda.getNilai());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/CommonReportHelper.java:12404");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		row.appendChild(denda = new MyDoublebox(valDenda));
		denda.setWidth("90%");
		denda.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumDenda.setNilai((denda.getValue() == null ? 0.0 : denda.getValue()) + "");
				Common.refreshUpdate(parameterUmumDenda);
			}
		});

		final Textbox catatan = new Textbox(parameterUmumDenda.getInfo1());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(catatan);
		catatan.setRows(3);
		catatan.setWidth("90%");
		catatan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumDenda.setInfo1(catatan.getValue());
				Common.refreshUpdate(parameterUmumDenda);
			}
		});

		final MyFormRow rowItemBiaya1 = new MyFormRow();
		rowItemBiaya1.setStyle("border:0px;background: transparent;");
		rowItemBiaya1.setParent(rows);
		rowItemBiaya1.appendChild(new ais.ui.util.MyLabelConfig("Pilih Tagihan"));
		rowItemBiaya1.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya1, "2");

		final MyFormRow rowItemBiaya2 = new MyFormRow();
		rowItemBiaya2.setStyle("border:0px;background: transparent;");
		rowItemBiaya2.setParent(rows);
		rowItemBiaya2.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya2, "2");

		final Vbox vboxItemBiaya = new Vbox();
		vboxItemBiaya.setParent(rowItemBiaya2);

		final Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		parameters.put("biodata_id", siswa.getId());
		parameters.put("tanggal", tanggal.getValue());
		parameters.put("nomor", nomor.getValue());
		parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());

		if (jenisPembayaran.getSelectedItem() != null && jenisPembayaran.getSelectedItem().getValue() != null) {
			JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem().getValue();
			if (pilihanJenisPembayaran.getBank() != null) {
				parameters.put("rekening_pembayaran",
						" melalui Bank " + pilihanJenisPembayaran.getBank().getNama() + "</b>");
			} else {
				parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
			}
		}

		parameters.put("catatan", catatan.getValue());
		parameters.put("prosentaseDenda", denda.getValue());
		parameters.put("nama", siswa.getNama());
		parameters.put("nim", siswa.getNim());
		parameters.put("tahun", tahun);
		parameters.put("bulan", bulan);

		// Proteksi NPE pada relasi Institusi
		String namaKepala = "(......................)";
		String namaSekolah = "";
		String namaYayasan = "";

		if (siswa.getSekolah() != null) {
			namaSekolah = siswa.getSekolah().getNama() == null ? "" : siswa.getSekolah().getNama();
			if (siswa.getSekolah().getNamaKepalaSekolah() != null
					&& !siswa.getSekolah().getNamaKepalaSekolah().trim().isEmpty()) {
				namaKepala = siswa.getSekolah().getNamaKepalaSekolah();
			}
			if (siswa.getSekolah().getYayasan() != null && siswa.getSekolah().getYayasan().getNama() != null) {
				namaYayasan = siswa.getSekolah().getYayasan().getNama();
			}
		}

		parameters.put("kepala_sekolah", namaKepala);
		parameters.put("kaprodi", namaKepala);
		parameters.put("sekolah", namaSekolah);
		parameters.put("yayasan", namaYayasan);
		parameters.put("fakultas", namaSekolah);
		parameters.put("jurusan", namaSekolah);

		final List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		populateTagihanSiswa(siswa, null, bulan, tahun, false,
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()),
				maps);

		Long jenis_tagihan_id = null;
		final List<MyCheckboxConfig> checkboxConfigs = new ArrayList<MyCheckboxConfig>();
		for (Map<String, Object> map : maps) {

			Long idTag = (Long) map.get("jenis_tagihan_id");
			if (jenis_tagihan_id == null || !jenis_tagihan_id.equals(idTag)) {
				new MyLabelBolder(map.get("jenis_tagihan").toString()).setParent(vboxItemBiaya);
				jenis_tagihan_id = idTag;
			}

			Tagihan tagihan = (Tagihan) map.get("tagihan");
			Double nilai = (Double) map.get("nilai");

			MyCheckboxConfig checkBox = new MyCheckboxConfig(
					map.get("label").toString() + ", " + Common.numberFormat.get().format(nilai));
			checkBox.setChecked(nilai > 0.1);
			checkBox.setAttribute("tagihan", tagihan);

			checkboxConfigs.add(checkBox);
			checkBox.setParent(vboxItemBiaya);
		}

		parameters.put("maps", maps);

		final EventListener eventListenerReport = Report.generatePDFReport("pdf", parameters, "Surat_Tagihan_Siswa",
				ais.ui.util.WaktuUtil.getDate(), west);

		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				parameters.remove("maps");

				StringBuilder sbTagihanData = new StringBuilder();
				List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>();

				for (MyCheckboxConfig checkboxConfig : checkboxConfigs) {
					if (checkboxConfig.isChecked()) {
						Tagihan tagihanAwal = (Tagihan) checkboxConfig.getAttribute("tagihan");

						for (Map<String, Object> map : maps) {
							Tagihan tagihan = (Tagihan) map.get("tagihan");
							if (tagihanAwal != null && tagihan != null && tagihan.getId().equals(tagihanAwal.getId())) {
								newMaps.add(map);

								try {
									Double nilai = (Double) map.get("nilai");
									String s = map.get("label").toString() + ", "
											+ Common.numberFormat.get().format(nilai);

									if (sbTagihanData.length() > 0)
										sbTagihanData.append("; ");
									sbTagihanData.append(s);

								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:12555");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								break;
							}
						}
					}
				}

				parameters.put("tagihanData", sbTagihanData.toString());
				parameters.put("maps", newMaps);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						parameters.put("tanggal", tanggal.getValue());
						parameters.put("nomor", nomor.getValue());
						parameters.put("jenis_tagihan", "SISWA");
						parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());

						if (jenisPembayaran.getSelectedItem() != null
								&& jenisPembayaran.getSelectedItem().getValue() != null) {
							JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem()
									.getValue();
							if (pilihanJenisPembayaran.getBank() != null) {
								parameters.put("rekening_pembayaran",
										" melalui Bank " + pilihanJenisPembayaran.getBank().getNama()
												+ " dengan nomor rekening <b>" + "</b>");
							} else {
								parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
							}
						}
						parameters.put("catatan", catatan.getValue());
						parameters.put("prosentaseDenda", denda.getValue());
						eventListenerReport.onEvent(new Event("", null, parameters));

					}
				});
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		Button tampilkan;
		hbox.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan Tagihan", "/img/print.png"));
		tampilkan.addEventListener("onClick", eventListener);

		tahunAjaran.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				maps.clear();
				populateTagihanSiswa(siswa, null, bulan, tahun, false,
						(String) (tahunAjaran.getSelectedItem() == null ? null
								: tahunAjaran.getSelectedItem().getValue()),
						maps);
				eventListener.onEvent(arg0);
			}
		});

		Button kirim;
		hbox.appendChild(kirim = new MyToolbarbuttonConfig("Kirim Tagihan ini ke Wali Murid", "/img/print.png"));
		kirim.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {

						String sekolah = siswa.getSekolah() != null ? siswa.getSekolah().getNama() : "";
						String nama = siswa.getNama() != null ? siswa.getNama() : "";

						String waktu = "";
						int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
						if (jam >= 10 && jam < 15) {
							waktu = "Siang";
						} else if (jam >= 15 && jam < 18) {
							waktu = "Sore";
						} else if (jam >= 18 && jam <= 24) {
							waktu = "Malam";
						} else {
							waktu = "Pagi";
						}

						String subject = "Tagihan siswa atas nama " + nama + " (" + siswa.getNomorInduk() + ")";

						String body = "Selamat " + waktu + ",<br><br>Yth. Bapak/Ibu Wali Murid dari <b>" + nama
								+ "</b>,<br>" + "Kami dari pihak sekolah *" + sekolah
								+ "* menyampaikan informasi mengenai tagihan yang perlu diselesaikan atas nama <b>"
								+ nama + "</b>"
								+ ". Rincian lengkap mengenai tagihan terlampir pada file terlampir. Kami harap Bapak/Ibu dapat melakukan pembayaran sesuai dengan batas waktu yang tertera pada file. Jika ada pertanyaan terkait tagihan atau pembayaran, silakan menghubungi kami pihak sekolah, kami siap membantu."
								+ "<br><br>" + "<b>Terima kasih atas perhatian dan kerja samanya.</b>";

						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						org.json.JSONArray userIds = new org.json.JSONArray();
						userIds.put(
								siswa.getNomorIndukNasional() == null || siswa.getNomorIndukNasional().trim().isEmpty()
										? siswa.getNomorInduk()
										: siswa.getNomorIndukNasional());

						File file = new File(parameters.get("report_file") + "");

						MailSender.sendMailLampiranTagihan(userIds, subject, body, sender, siswa.getAlamatEmail(), null,
								false, siswa, true, file);

						String url = Common.getRequestHostWithProtocolSimple()
								+ file.getAbsolutePath().split("webapps")[1];

						Set<String> forms = siswa.ambilTelp();
						if (forms != null) {
							for (String from : forms) {
								if (from != null && !from.trim().isEmpty()
										&& !(from.trim().equals("00000000000000000000")
												|| from.trim().equals("000000000"))) {

									body = "Selamat " + waktu + ",\n\nYth. Bapak/Ibu Wali Murid atas nama *" + nama
											+ "*,\n\n" + "Kami dari pihak sekolah *" + sekolah
											+ "* menyampaikan informasi mengenai tagihan yang perlu diselesaikan, rincian lengkap mengenai tagihan terlampir pada file terlampir. Kami harap Bapak/Ibu dapat melakukan pembayaran sesuai dengan batas waktu yang tertera pada file. Jika ada pertanyaan terkait tagihan atau pembayaran, silakan menghubungi kami pihak sekolah, kami siap membantu."
											+ "\n\n" + "*Terima kasih atas perhatian dan kerja samanya.*";

									String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
											"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
											.getNilai();
									Wa.kirimWaViaUltramsg(from, dawal + body, "Tagihan_Pembayaran.pdf", url,
											Wa.buatProfile(siswa.getSekolah(), null));
								}
							}
						}
					}
				});
			}
		});

		Common.createDefaultTimer(eventListener);
	}

	@SuppressWarnings({ "deprecation" })
	public static void prosesSuratTandaTerima(final Siswa siswa, final Integer bulan, final Integer tahun)
			throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		final West west = new West();
		west.setWidth(tbmuser != null && tbmuser.getSiswa() == null ? "300px" : "0px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		final Combobox tahunAjaran = new Combobox();
		Common.generateTahunAjaran(tahunAjaran);
		row.appendChild(tahunAjaran);
		tahunAjaran.setReadonly(true);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		final ParameterUmum parameterUmumTanggalJatuhTempo = Common.getParameterUmum(
				"tanggal jatuh tempo surat tagihan " + siswa.getNim(),
				Common.dateFormat1.get().format(calendar.getTime()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Jatuh Tempo"));
		final MyDatebox tanggalJatuhTempo;
		row.appendChild(tanggalJatuhTempo = new MyDatebox(
				Common.dateFormat1.get().parse(parameterUmumTanggalJatuhTempo.getNilai())));
		tanggalJatuhTempo.setReadonly(true);
		tanggalJatuhTempo.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumTanggalJatuhTempo.setNilai(Common.dateFormat1.get().format(tanggalJatuhTempo.getValue()));
				Common.refreshUpdate(parameterUmumTanggalJatuhTempo);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
		final MyTextbox nomor;
		final ParameterUmum parameterUmum = Common.getParameterUmum("surat tagihan " + siswa.getNim(), "");
		row.appendChild(nomor = new MyTextbox(parameterUmum.getNilai()));
		nomor.setWidth("90%");
		nomor.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmum.setNilai(nomor.getValue());
				Common.refreshUpdate(parameterUmum);
			}
		});

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_cara_pembayaran_di_surat_tagihan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));
		final Combobox jenisPembayaran;
		row.appendChild(jenisPembayaran = new Combobox());
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertComboDanSemua(jenisPembayaran, new String[] { "nama", "bank" }, "akun", JenisPembayaran.class,
				"== Tidak menggunakan cara pembayaran ==",
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		final ParameterUmum parameterUmumRekeningPembayaran = Common
				.getParameterUmum("bank surat tagihan " + siswa.getNim(), "");
		JenisPembayaran selectedJenisPembayaran = (JenisPembayaran) HibernateUtil.currentSession()
				.createCriteria(JenisPembayaran.class)
				.add(parameterUmumRekeningPembayaran.getNilai().isEmpty()
						|| !Common.isNumber(parameterUmumRekeningPembayaran.getNilai())
								? Restrictions.sqlRestriction("false")
								: Restrictions.idEq(Long.parseLong(parameterUmumRekeningPembayaran.getNilai())))
				.uniqueResult();
		Common.selectComboItem(jenisPembayaran, selectedJenisPembayaran);
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) (jenisPembayaran.getSelectedItem() == null
						? null
						: jenisPembayaran.getSelectedItem().getValue());
				parameterUmumRekeningPembayaran
						.setNilai(pilihanJenisPembayaran == null ? "" : pilihanJenisPembayaran.getId().toString());
				Common.refreshUpdate(parameterUmumRekeningPembayaran);
			}
		});
		jenisPembayaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Denda"));
		final MyDoublebox denda;
		final ParameterUmum parameterUmumDenda = Common.getParameterUmum("denda surat tagihan " + siswa.getNim(),
				"0.0");
		row.appendChild(denda = new MyDoublebox(Double.parseDouble(parameterUmumDenda.getNilai())));
		denda.setWidth("90%");
		denda.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumDenda.setNilai((denda.getValue() == null ? 0.0 : denda.getValue()) + "");
				Common.refreshUpdate(parameterUmumDenda);
			}
		});

		final Textbox catatan = new Textbox(parameterUmumDenda.getInfo1());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(catatan);
		catatan.setRows(3);
		catatan.setWidth("90%");
		catatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				parameterUmumDenda.setInfo1(catatan.getValue());
				Common.refreshUpdate(parameterUmumDenda);
			}
		});

		final MyFormRow rowItemBiaya1 = new MyFormRow();
		rowItemBiaya1.setStyle("border:0px;background: transparent;");
		rowItemBiaya1.setParent(rows);
		rowItemBiaya1.appendChild(new ais.ui.util.MyLabelConfig("Pilih Tagihan"));
		rowItemBiaya1.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya1, "2");

		final MyFormRow rowItemBiaya2 = new MyFormRow();
		rowItemBiaya2.setStyle("border:0px;background: transparent;");
		rowItemBiaya2.setParent(rows);
		rowItemBiaya2.setValign("top");
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya2, "2");

		final Vbox vboxItemBiaya = new Vbox();
		vboxItemBiaya.setParent(rowItemBiaya2);

		final Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		parameters.put("biodata_id", siswa.getId());
		parameters.put("tanggal", tanggal.getValue());
		parameters.put("nomor", nomor.getValue());
		parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());

		if (jenisPembayaran.getSelectedItem() != null && jenisPembayaran.getSelectedItem().getValue() != null) {
			JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem().getValue();
			if (pilihanJenisPembayaran.getBank() != null) {
				parameters.put("rekening_pembayaran",
						" melalui Bank " + pilihanJenisPembayaran.getBank().getNama() + "</b>");
			} else {
				parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
			}
		}

		parameters.put("catatan", catatan.getValue());
		parameters.put("prosentaseDenda", denda.getValue());
		parameters.put("nama", siswa.getNama());
		parameters.put("nim", siswa.getNim());
		parameters.put("tahun", tahun);
		parameters.put("bulan", bulan);

		parameters.put("kepala_sekolah",
				siswa.getSekolah().getNamaKepalaSekolah() == null
						|| siswa.getSekolah().getNamaKepalaSekolah().trim().isEmpty() ? "(......................)"
								: siswa.getSekolah().getNamaKepalaSekolah());
		parameters.put("kaprodi",
				siswa.getSekolah().getNamaKepalaSekolah() == null
						|| siswa.getSekolah().getNamaKepalaSekolah().trim().isEmpty() ? "(......................)"
								: siswa.getSekolah().getNamaKepalaSekolah());
		parameters.put("sekolah", siswa.getSekolah().getNama());
		parameters.put("yayasan", siswa.getSekolah().getYayasan().getNama());
		parameters.put("fakultas", siswa.getSekolah().getNama());
		parameters.put("jurusan", siswa.getSekolah().getNama());

		final List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		populateTagihanSiswa(siswa, null, bulan, tahun, true,
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()),
				maps);

		Long jenis_tagihan_id = null;
		final List<MyCheckboxConfig> checkboxConfigs = new ArrayList<MyCheckboxConfig>();
		for (Map<String, Object> map : maps) {

			Long idTag = (Long) map.get("jenis_tagihan_id");
			if (jenis_tagihan_id == null || !jenis_tagihan_id.equals(idTag)) {
				new MyLabelBolder(map.get("jenis_tagihan").toString()).setParent(vboxItemBiaya);
				jenis_tagihan_id = idTag;
			}

			Tagihan tagihan = (Tagihan) map.get("tagihan");

			Double nilai = (Double) map.get("nilai");

			MyCheckboxConfig checkBox = new MyCheckboxConfig(
					map.get("label").toString() + ", " + Common.numberFormat.get().format(nilai));
			checkBox.setChecked(nilai > 0.1);
			checkBox.setAttribute("tagihan", tagihan);

			checkboxConfigs.add(checkBox);
			checkBox.setParent(vboxItemBiaya);
		}

		parameters.put("maps", maps);

		final EventListener eventListenerReport = Report.generatePDFReport("pdf", parameters, "Tanda_Terima_Siswa",
				ais.ui.util.WaktuUtil.getDate(), west);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				parameters.remove("maps");

				String tagihanData = "";
				List<Map<String, Object>> newMaps = new ArrayList<Map<String, Object>>();
				for (MyCheckboxConfig checkboxConfig : checkboxConfigs) {
					if (checkboxConfig.isChecked()) {
						Tagihan tagihanAwal = (Tagihan) checkboxConfig.getAttribute("tagihan");

						for (Map<String, Object> map : maps) {
							Tagihan tagihan = (Tagihan) map.get("tagihan");
							if (tagihanAwal != null && tagihan != null && tagihan.getId().equals(tagihanAwal.getId())) {
								newMaps.add(map);

								try {
									Double nilai = (Double) map.get("nilai");

									String s = map.get("label").toString() + ", "
											+ Common.numberFormat.get().format(nilai);

									tagihanData += tagihanData.isEmpty() ? s : "; " + s;

								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:12961");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}

								break;
							}
						}
					}
				}
				parameters.put("tagihanData", tagihanData);
				parameters.put("maps", newMaps);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						parameters.put("tanggal", tanggal.getValue());
						parameters.put("nomor", nomor.getValue());
						parameters.put("jenis_tagihan", "SISWA");
						parameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());
						if (jenisPembayaran.getSelectedItem() != null
								&& jenisPembayaran.getSelectedItem().getValue() != null) {
							JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) jenisPembayaran.getSelectedItem()
									.getValue();
							if (pilihanJenisPembayaran.getBank() != null) {
								parameters.put("rekening_pembayaran",
										" melalui Bank " + pilihanJenisPembayaran.getBank().getNama()
												+ " dengan nomor rekening <b>" + "</b>");
							} else {
								parameters.put("rekening_pembayaran", " melalui  " + pilihanJenisPembayaran.getNama());
							}
						}
						parameters.put("catatan", catatan.getValue());
						parameters.put("prosentaseDenda", denda.getValue());
						eventListenerReport.onEvent(new Event("", null, parameters));

					}
				});
			}
		};
//		report_file
		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		Button tampilkan;
		hbox.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan Tagihan", "/img/print.png"));
		tampilkan.addEventListener("onClick", eventListener);

		tahunAjaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				maps.clear();
				populateTagihanSiswa(siswa, null, bulan, tahun, true,
						(String) (tahunAjaran.getSelectedItem() == null ? null
								: tahunAjaran.getSelectedItem().getValue()),
						maps);
				eventListener.onEvent(arg0);
			}
		});

		Button kirim;
		hbox.appendChild(kirim = new MyToolbarbuttonConfig("Kirim Tagihan", "/img/print.png"));
		kirim.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String subject = "Tagihan siswa atas nama " + siswa.getNama() + " (" + siswa.getNomorInduk()
								+ ")";

						String tagihanData = parameters.get("tagihanData") + "";

						String body = "Terdapat tagihan untuk siswa atas nama " + siswa.getNama() + " ("
								+ siswa.getNim() + ") " + "<br>Isi informasi tagihan adalah sbb:<br>" + tagihanData
								+ ".<br><br>Terima Kasih";
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						JSONArray userIds = new JSONArray();
						userIds.put(
								siswa.getNomorIndukNasional() == null || siswa.getNomorIndukNasional().trim().isEmpty()
										? siswa.getNomorInduk()
										: siswa.getNomorIndukNasional());

						File file = new File(parameters.get("report_file") + "");

						MailSender.sendMailLampiranTagihan(userIds, subject, body, sender, siswa.getAlamatEmail(), null,
								true, siswa, file);
					}
				});

			}
		});

		Common.createDefaultTimer(eventListener);

	}

	@SuppressWarnings({ "unchecked" })
	public static void populateTagihanSiswa(Siswa siswa, CalonSiswa calonSiswa, Integer bulan, Integer tahun,
			boolean tampilYgBelumDibayar, String tahunAjaran, List<Map<String, Object>> maps) {

		Session session = HibernateUtil.currentSession();

		if (tampilYgBelumDibayar) {
			List<PembayaranSiswa> pembayaranSiswas = session.createCriteria(PembayaranSiswa.class)
					.add(siswa == null || siswa.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("siswa", siswa))
					.add(calonSiswa == null || calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("calonSiswa", calonSiswa))
					.addOrder(Order.asc("tanggalBayar")).list();

			for (PembayaranSiswa pembayaranSiswa : pembayaranSiswas) {
				List<PembayaranSiswaDetail> pembayaranSiswaDetails = session.createCriteria(PembayaranSiswaDetail.class)
						.add(Restrictions.eq("pembayaranSiswa", pembayaranSiswa)).createAlias("tagihan", "tagihan")
						.createAlias("tagihan.pengaturanBiaya", "pengaturanBiaya")
						.createAlias("pengaturanBiaya.jenisBiayaSekolah", "jenisBiayaSekolah")
						.addOrder(Order.asc("jenisBiayaSekolah.nama")).addOrder(Order.asc("tagihan.tahunbulan")).list();

				for (PembayaranSiswaDetail pengaturanBiaya : pembayaranSiswaDetails) {
					try {
						Tagihan tagihan = pengaturanBiaya.getTagihan();
						Map<String, Object> map = new java.util.HashMap<String, Object>();

						Double telahDibayar = tagihan.getPembayaranSiswaDetail() == null ? 0.0
								: tagihan.getPembayaranSiswaDetail().getNominal();

						String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
								+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
										? " (ke " + tagihan.getBayarKe() + ")"
										: "");
						if (tagihan.getBulan() != null && tagihan.getBulan() > 0 && tagihan.getBulan() <= 12) {
							ket += ", Bulan " + Common.BULAN[tagihan.getBulan() - 1];
						}
						if (tagihan.getTahun() != null && tagihan.getTahun() > 1900) {
							ket += ", Tahun " + tagihan.getTahun();
						}
						Double denda = tagihan.getDenda();
						if (denda > 0.01) {
							ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
						}
						Date tglDeadline = tagihan.getTanggalDeadline();
						if (tglDeadline != null) {
							ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
						}

						ket += (tagihan.getDiskonSiswa() != null ? " - " + tagihan.getDiskonSiswa().getNama() : "");

						map.put("jenis", "Tagihan yang telah dibayar");
						map.put("tagihan", tagihan);
						map.put("label", ket);

						map.put("tahun_akademik", tagihan.getTahunAjaran());
						map.put("nilai", telahDibayar);
						map.put("telahDibayar", telahDibayar);
						map.put("jumlah", telahDibayar);

						map.put("jenis_tagihan", pengaturanBiaya.toString());
						map.put("jenis_tagihan_id", pengaturanBiaya.getId());

						map.put("ta", tagihan == null ? "" : tagihan.getTahunAjaran());
						map.put("smt",
								tagihan == null || tagihan.getTahunbulan() == null ? 0 : tagihan.getTahunbulan());

						maps.add(map);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:13133");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}
			}
		}

		List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(PengaturanBiaya
				.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), siswa, calonSiswa)

				.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
				.addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);

		for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
			JenisBiayaSekolah jenisBiaya = pengaturanBiaya.getJenisBiayaSekolah();
			if (pengaturanBiaya.getAktif() && ((siswa != null && !jenisBiaya.getGunakanCalonSiswa()
					&& DetailTagihanSiswaHelper.apakahAda(pengaturanBiaya, siswa))
					|| (calonSiswa != null && jenisBiaya.getGunakanCalonSiswa()
							&& DetailTagihanCalonSiswaHelper.apakahAda(pengaturanBiaya, calonSiswa)))) {

				List<Tagihan> tagihans = jenisBiaya.getGunakanCalonSiswa()
						? TagihanUtilCalonSiswa.getTagihan(pengaturanBiaya.getJenisBiayaSekolah(), pengaturanBiaya,
								calonSiswa, bulan, tahun, false)
						: TagihanUtil.getTagihan(pengaturanBiaya.getJenisBiayaSekolah(), pengaturanBiaya, siswa, bulan,
								tahun, false);

				if (!tagihans.isEmpty()) {
					boolean ada = false;
					for (Tagihan tagihan : tagihans) {
						if (tagihan.getPembayaranSiswaDetail() == null) {
							if (tagihan.getNominalBiaya().getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()
									|| tagihan.getNominal() > 0.1) {
								ada = true;
								break;
							}
						}
					}

					System.out.println("ada => " + ada);

					if (ada) {

						Double sisaTotal = 0.0;
						for (Tagihan tagihan : tagihans) {
							try {
								Map<String, Object> map = new java.util.HashMap<String, Object>();

								Double jumlah = tagihan.getNominal();
								Double telahDibayar = tagihan.getPembayaranSiswaDetail() == null ? 0.0
										: tagihan.getPembayaranSiswaDetail().getNominal();

								Double sisa = jumlah - telahDibayar;
								sisaTotal += sisa;

								String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
										+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
												? " (ke " + tagihan.getBayarKe() + ")"
												: "");
								if (tagihan.getBulan() != null && tagihan.getBulan() > 0 && tagihan.getBulan() <= 12) {
									ket += ", Bulan " + Common.BULAN[tagihan.getBulan() - 1];
								}
								if (tagihan.getTahun() != null && tagihan.getTahun() > 1900) {
									ket += ", Tahun " + tagihan.getTahun();
								}
								Double denda = tagihan.getDenda();
								if (denda > 0.01) {
									ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
								}
								Date tglDeadline = tagihan.getTanggalDeadline();
								if (tglDeadline != null) {
									ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
								}

								ket += (tagihan.getDiskonSiswa() != null ? " - " + tagihan.getDiskonSiswa().getNama()
										: "");

								map.put("jenis", "Tagihan yang belum dibayar");
								map.put("tagihan", tagihan);
								map.put("label", ket);

								map.put("tahun_akademik", tagihan.getTahunAjaran());
								map.put("nilai", sisa);
								map.put("telahDibayar", telahDibayar);
								map.put("jumlah", jumlah);

								map.put("jenis_tagihan", pengaturanBiaya.toString());
								map.put("jenis_tagihan_id", pengaturanBiaya.getId());

								map.put("ta", tagihan == null ? "" : tagihan.getTahunAjaran());
								map.put("smt", tagihan == null || tagihan.getTahunbulan() == null ? 0
										: tagihan.getTahunbulan());

								maps.add(map);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/CommonReportHelper.java:13226");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report Helper", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
							}
						}

					}
				}

			}
		}

	}

}
