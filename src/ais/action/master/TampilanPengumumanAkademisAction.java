package ais.action.master;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.maintenance.PMBAction;
import ais.action.maintenance.PSBAction;
import ais.action.maintenance.ProfileAction;
import ais.action.master.asset.PenyediaAssetAction;
import ais.action.master.asset.TampilanPengumumanVendorAction;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.DiskusiPengumumanAkademisHelper;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.BiodataCalonMahasiswaAction;
import ais.action.master.pmb.CariDataPembayaranAction.ProsesUploadBuktiPembayaran;
import ais.action.master.pmb.TampilanPaymentGateway;
import ais.action.master.pmb.TampilanPengumumanPMBAction;
import ais.action.master.pmb.TampilanUjianCalonMahasiswa;
import ais.action.master.pmb.VerifikasiPMBHelper;
import ais.action.master.recruitment.CalonPegawaiAction;
import ais.action.master.recruitment.TampilanPengumumanKarirAction;
import ais.action.master.sekolah.CalonSiswaAction;
import ais.action.master.sekolah.SiswaAction;
import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.psb.CommonReportPsb;
import ais.action.master.sekolah.psb.TampilanUjianCalonSiswa;
import ais.action.master.sekolah.psb.VerifikasiPSBHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanKartuMahasiswa;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPMB;
import ais.common.CommonPSB;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Dosen;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.JadwalPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPengumuman;
import ais.database.model.Kegiatan;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPaket;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.RuangPaketPMB;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelompokGelombang;
import ais.database.model.sekolah.KelompokParameterTambahanCalonSiswa;
import ais.database.model.sekolah.ParameterTambahanGelombangPendaftaranPsb;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class TampilanPengumumanAkademisAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -2301873239699174688L;

	private Boolean readonly = false;
	private Borderlayout utama;

	private Center centerPengumuman;
	private Component menu;
	private Tabs tabspeng;
	private Tabpanels tabpanelspeng;
	private Textbox cari = new Textbox();
	private Rows rows;
	private Tbmuser tbmuser;

	private PerguruanTinggi selectedPerguruanTinggi;
	private East menuPintas;

	private Long idPengumuman = null;
	private boolean sederhana = false;

	private Html textBerjalan;
	private String menuBgColor;
	boolean sudahTampil = false;
	private PengumumanAkademis pengumumanAkademis = null;


	private static boolean isSessionUsable(Session session) {
		try {
			return session != null && session.isOpen();
		} catch (Exception e) {
			return false;
		}
	}

	private static String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	private static String htmlEscape(String value) {
		String text = safeText(value);
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static void appendPanelDescription(Groupbox groupbox, String description) {
		if (groupbox == null || description == null || description.trim().length() == 0) {
			return;
		}
		Html html = new Html("<div style=\"margin:8px 10px 10px 10px;padding:10px 12px;border-radius:12px;"
				+ "background:rgba(255,255,255,0.72);border:1px solid rgba(148,163,184,0.35);"
				+ "font-size:12px;line-height:1.55;color:#334155;box-shadow:0 6px 16px rgba(15,23,42,0.06);\">"
				+ htmlEscape(description) + "</div>");
		html.setParent(groupbox);
	}

	private static void applyModernGroupboxStyle(Groupbox groupbox) {
		if (groupbox == null) {
			return;
		}
		groupbox.setWidth("97%");
		groupbox.setStyle("border:1px solid rgba(148,163,184,0.45);padding:2px 3px 5px 0px;"
				+ "background-color:rgba(255,255,255,0.72);border-radius:14px;overflow:hidden;"
				+ "box-shadow:0 12px 28px rgba(15,23,42,0.10);max-width:100%;min-width:330px;");
	}

	/**
	 * Membangun satu baris informasi HTML (&lt;tr&gt;) untuk tabel profil calon siswa.
	 * Baris terdiri dari dua kolom: label (kiri, abu-abu) dan nilai (kanan, tebal).
	 * Tabel ini dirender langsung di dalam card profil sebagai HTML murni untuk
	 * kompatibilitas ZK 5.x yang tidak mendukung data-binding kompleks di komponen Html.
	 *
	 * <p>Cara pakai:
	 * <pre>
	 *   htmlBuilder.append(buildInfoRow("Nama", htmlEscape(calon.getNama())));
	 * </pre>
	 *
	 * @param label teks kolom kiri (label); boleh null (ditampilkan kosong)
	 * @param value teks/HTML kolom kanan (nilai); boleh null (ditampilkan kosong)
	 * @return string HTML &lt;tr&gt;...&lt;/tr&gt; siap digabung ke tabel
	 */
	private static String buildInfoRow(String label, String value) {
		return "<tr><td style='padding:4px 8px 4px 0;color:#64748b;white-space:nowrap;"
				+ "vertical-align:top;width:40%;font-size:12px;'>" + (label == null ? "" : label)
				+ "</td><td style='padding:4px 0 4px 2px;font-weight:500;vertical-align:top;"
				+ "font-size:12px;color:#1e293b;'>" + (value == null ? "" : value) + "</td></tr>";
	}

	private static double safeDouble(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private static boolean pembayaranDaftarUlangSudahLunas(Kegiatan kegiatanDaftarUlang,
			Double totalDaftarUlangSetting) {
		if (kegiatanDaftarUlang == null) {
			return totalDaftarUlangSetting != null && totalDaftarUlangSetting.doubleValue() < 0.01;
		}
		double dibayar = safeDouble(kegiatanDaftarUlang.getAmount());
		double persentase = safeDouble(kegiatanDaftarUlang.getPersentaseLunas());
		if (persentase >= 99.99) {
			return true;
		}
		if (totalDaftarUlangSetting != null) {
			return totalDaftarUlangSetting.doubleValue() < 0.01
					|| dibayar + 0.01 >= totalDaftarUlangSetting.doubleValue();
		}

		/*
		 * Pada beberapa data PMB, pembayaran daftar ulang sudah masuk dari host-to-host
		 * tetapi setting tagihan prodi tidak ditemukan. Jangan membuat step "Daftar
		 * Ulang" tetap abu-abu hanya karena setting tidak ada; pakai tagihan kegiatan
		 * yang tercatat sebagai fallback.
		 */
		double tagihanKegiatan = safeDouble(kegiatanDaftarUlang.getTagihan());
		return tagihanKegiatan > 0.01 && dibayar + 0.01 >= tagihanKegiatan;
	}

	private static double hitungBiayaRegistrasiCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (biodataCalonMahasiswa == null) {
			return 0.0;
		}
		double total = 0.0;
		try {
			JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
			Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
			if (jurusan == null || jurusan.getId() == null) {
				jurusan = biodataCalonMahasiswa.getProdi1() == null
						? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
			}
			java.util.Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper
					.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, jurusan, true);
			for (DetailBiaya detailBiaya : detailBiayas) {
				if (detailBiaya != null) {
					total += safeDouble(detailBiaya.getNilaiBiayaBaru() == null
							? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/TampilanPengumumanAkademisAction.java:hitungBiayaRegistrasiCalonMahasiswa");
		}
		return total;
	}

	private static Double hitungBiayaDaftarUlangCalonMahasiswa(BiodataCalonMahasiswa calon) {
		if (calon == null || calon.getProdiLulus() == null) {
			return null;
		}
		try {
			java.util.Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper
					.getDetailBiayaCalonMahasiswa(calon, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU,
							calon.getProdiLulus(), true);
			if (detailBiayas == null || detailBiayas.isEmpty()) {
				return null;
			}
			double total = 0.0;
			for (DetailBiaya detailBiaya : detailBiayas) {
				if (detailBiaya != null) {
					total += safeDouble(detailBiaya.getNilaiBiayaBaru() == null
							? detailBiaya.getNilaiBiaya() : detailBiaya.getNilaiBiayaBaru());
				}
			}
			return Double.valueOf(total);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"TampilanPengumumanAkademisAction.hitungBiayaDaftarUlangCalonMahasiswa");
			return null;
		}
	}

	/**
	 * Jadwal pembayaran adalah sumber kebenaran untuk membuka pembayaran daftar
	 * ulang. Batas daftar ulang pada gelombang hanya dipakai sebagai fallback untuk
	 * data lama yang belum memiliki relasi jadwal pembayaran.
	 */
	private static boolean pembayaranDaftarUlangMasihDibuka(BiodataCalonMahasiswa calon, Kegiatan kegiatan) {
		Date sekarang = WaktuUtil.getDate();
		JadwalPembayaran jadwalTertaut = kegiatan == null ? null : kegiatan.getJadwalPembayaran();

		if (jadwalTertaut != null) {
			boolean aktif = jadwalTertaut.getAktif();
			boolean sudahMulai = jadwalTertaut.getStartDate() == null
					|| !jadwalTertaut.getStartDate().after(sekarang);
			boolean belumBerakhir = jadwalTertaut.getEndDate() == null
					|| !jadwalTertaut.getEndDate().before(sekarang);
			if (aktif && sudahMulai && belumBerakhir) {
				return true;
			}
		}

		// Kegiatan lama dapat masih menunjuk jadwal yang sudah lewat. Cari jadwal aktif
		// terbaru agar jadwal baru yang dibuat petugas langsung berlaku di akun calon.
		try {
			JenisKegiatan jenis = kegiatan != null && kegiatan.getJenisKegiatan() != null
					? kegiatan.getJenisKegiatan()
					: ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
			java.io.Serializable[] hasil = CommonPMB.pembayaranUtil
					.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(sekarang, jenis,
							calon.getJenjang(), calon.getTahunAkademik(), Boolean.TRUE,
							calon.getJenisSeleksi(), calon.getProgram(), calon.getNoRegistrasi(),
							calon.getGelombangPendaftaran());
			if (hasil != null && hasil.length > 0 && hasil[0] instanceof JadwalPembayaran) {
				return true;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"TampilanPengumumanAkademisAction.pembayaranDaftarUlangMasihDibuka");
		}

		// Data lama tanpa relasi jadwal tetap mengikuti batas gelombang seperti semula.
		if (jadwalTertaut == null && calon.getGelombangPendaftaran() != null
				&& calon.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir() != null) {
			Date batasGelombang = calon.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir();
			return !batasGelombang.before(sekarang)
					|| Common.dateFormat1.get().format(batasGelombang)
							.equals(Common.dateFormat1.get().format(sekarang));
		}
		return jadwalTertaut == null;
	}

	/**
	 * Membangun widget progress tracker HTML/CSS modern yang menampilkan empat tahapan
	 * proses pendaftaran calon siswa: Daftar &rarr; Bayar &rarr; Verifikasi &rarr; Hasil.
	 *
	 * <p>Setiap tahap ditandai dengan lingkaran berwarna:
	 * <ul>
	 *   <li>Hijau (&check;) = selesai</li>
	 *   <li>Abu-abu (nomor) = belum selesai</li>
	 * </ul>
	 * Konektor antar tahap berwarna hijau bila kedua tahap sudah selesai, abu-abu bila
	 * salah satu belum.
	 *
	 * <p>Widget ini menggunakan <code>display:table</code> agar kompatibel dengan
	 * browser lama (IE9+, iOS Safari 6+) yang mungkin digunakan pada perangkat
	 * sekolah. Tidak bergantung pada library CSS eksternal.
	 *
	 * @param step1Daftar   true bila calon siswa sudah terdaftar (selalu true saat ini)
	 * @param step2Bayar    true bila ada pembayaran yang sudah dilakukan
	 * @param step3Verif    true bila berkas sudah terverifikasi oleh admin
	 * @param step4Hasil    true bila sudah ada hasil (diterima/ditolak/mundur)
	 * @return string HTML lengkap berisi widget progress tracker
	 */
	private static String buildProgressTrackerPsb(boolean step1Daftar, boolean step2Bayar,
			boolean step3Verif, boolean step4Hasil) {
		String[] labels = { "Daftar", "Bayar", "Verifikasi", "Hasil" };
		boolean[] done = { step1Daftar, step2Bayar, step3Verif, step4Hasil };
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:14px;padding-top:10px;border-top:1px solid #e2e8f0;'>")
		  .append("<div style='font-size:10px;font-weight:700;color:#94a3b8;letter-spacing:0.8px;")
		  .append("margin-bottom:8px;'>TAHAPAN PENDAFTARAN</div>")
		  .append("<div style='display:table;width:100%;table-layout:fixed;'><div style='display:table-row;'>");
		for (int i = 0; i < labels.length; i++) {
			String circleBg = done[i] ? "#22c55e" : "#e2e8f0";
			String circleClr = done[i] ? "white" : "#94a3b8";
			String textClr = done[i] ? "#15803d" : "#94a3b8";
			String icon = done[i] ? "&#10003;" : String.valueOf(i + 1);
			sb.append("<div style='display:table-cell;text-align:center;vertical-align:middle;padding:0 2px;'>")
			  .append("<div style='width:26px;height:26px;border-radius:50%;background:").append(circleBg)
			  .append(";color:").append(circleClr)
			  .append(";display:inline-block;line-height:26px;font-size:12px;font-weight:700;'>")
			  .append(icon).append("</div>")
			  .append("<div style='font-size:9.5px;color:").append(textClr)
			  .append(";margin-top:3px;font-weight:600;'>").append(labels[i]).append("</div></div>");
			if (i < labels.length - 1) {
				boolean connDone = done[i] && done[i + 1];
				sb.append("<div style='display:table-cell;vertical-align:middle;width:16px;'>")
				  .append("<div style='height:2px;background:").append(connDone ? "#22c55e" : "#e2e8f0")
				  .append(";'></div></div>");
			}
		}
		sb.append("</div></div></div>");
		return sb.toString();
	}

	/**
	 * Membuat HTML progress tracker 4 langkah untuk alur pendaftaran mahasiswa baru (PMB).
	 * Tahapan: Daftar &#8594; Bayar Registrasi &#8594; Seleksi &#8594; Daftar Ulang.
	 * Lingkaran hijau &#10003; jika langkah selesai, abu-abu jika belum.
	 *
	 * @param step1Daftar       true jika calon mahasiswa sudah mendaftar (selalu true saat login)
	 * @param step2BayarReg     true jika pembayaran registrasi sudah lunas 100%
	 * @param step3Seleksi      true jika sudah ada hasil seleksi (diterima/ditolak/mundur)
	 * @param step4DaftarUlang  true jika pembayaran daftar ulang sudah lunas 100%
	 * @return HTML string progress tracker siap pakai
	 */
	private static String buildProgressTrackerPmb(boolean step1Daftar, boolean step2BayarReg,
			boolean step3Seleksi, boolean step4DaftarUlang) {
		String[] labels = { "Daftar", "Bayar Reg.", "Seleksi", "Daftar Ulang" };
		boolean[] done = { step1Daftar, step2BayarReg, step3Seleksi, step4DaftarUlang };
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:14px;padding-top:10px;border-top:1px solid #e2e8f0;'>")
		  .append("<div style='font-size:10px;font-weight:700;color:#94a3b8;letter-spacing:0.8px;")
		  .append("margin-bottom:8px;'>TAHAPAN PENDAFTARAN</div>")
		  .append("<div style='display:table;width:100%;table-layout:fixed;'><div style='display:table-row;'>");
		for (int i = 0; i < labels.length; i++) {
			String circleBg = done[i] ? "#22c55e" : "#e2e8f0";
			String circleClr = done[i] ? "white" : "#94a3b8";
			String textClr = done[i] ? "#15803d" : "#94a3b8";
			String icon = done[i] ? "&#10003;" : String.valueOf(i + 1);
			sb.append("<div style='display:table-cell;text-align:center;vertical-align:middle;padding:0 2px;'")
			  .append("><div style='width:26px;height:26px;border-radius:50%;background:").append(circleBg)
			  .append(";color:").append(circleClr)
			  .append(";display:inline-block;line-height:26px;font-size:12px;font-weight:700;'")
			  .append(">").append(icon).append("</div>")
			  .append("<div style='font-size:9.5px;color:").append(textClr)
			  .append(";margin-top:3px;font-weight:600;'>").append(labels[i]).append("</div></div>");
			if (i < labels.length - 1) {
				boolean connDone = done[i] && done[i + 1];
				sb.append("<div style='display:table-cell;vertical-align:middle;width:16px;'>")
				  .append("<div style='height:2px;background:").append(connDone ? "#22c55e" : "#e2e8f0")
				  .append(";'></div></div>");
			}
		}
		sb.append("</div></div></div>");
		return sb.toString();
	}



	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		if (execution.getParameter("id_pengumuman") != null) {
			idPengumuman = Long.parseLong(execution.getParameter("id_pengumuman").trim());
		}
		if (execution.getParameter("sederhana") != null) {
			sederhana = true;
		}

		if (session.getAttribute("tabs") != null) {
			tabspeng = (Tabs) session.getAttribute("tabs");
		}
		if (session.getAttribute("tabpanels") != null) {
			tabpanelspeng = (Tabpanels) session.getAttribute("tabpanels");
		}

		Sessions.getCurrent().setAttribute("tabspeng", tabspeng);
		Sessions.getCurrent().setAttribute("tabpanelspeng", tabpanelspeng);

		tbmuser = sederhana ? Common.getCurrentUser() : Common.getCurrentFromSpringUser();
		if (tbmuser == null || tbmuser.getUserId() == null) {
			onSearchDefault(null);
			return;
		}
		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		menuBgColor = Common.getKonfigurasi("menu_bg_color", "#F5F5F5").getNilai();

		if (session != null) { session.setAttribute("usersTemp", tbmuser); }
		onSearchDefault(null);
		loadMenu();

		if (menuPintas != null && menuPintas.isVisible()) {
			menuPintas.setTitle("");
			ProfileAction.initProfile(tbmuser, menuPintas, pengumumanAkademis);
		}

		Common.initLaguage();

		if (textBerjalan != null) {
			String text = Common.tampilanTextBerjalan();
			if (!text.isEmpty()) {
				textBerjalan.setContent(text);
			} else {
				textBerjalan.getParent().setVisible(false);
				textBerjalan.setVisible(false);
			}
		}
	}

	private Grid loadMenu() {
		try {
			String desktopWidth = execution.getParameter("desktopWidth");

			if (sederhana || Common.isMobile() || (desktopWidth != null
					&& Integer.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE)) {
				menu = new North();
				((North) menu).setFlex(true);
				((North) menu).setHeight("100%");
			} else {
				menu = new West();
				((West) menu).setWidth("250px");
				((West) menu).setStyle("background:" + menuBgColor + " repeat-x 0 0;");
			}

			if (utama != null) {
				utama.appendChild(menu);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Row rowPencarian = Common.tampilanScroll1(menu);
		rowPencarian.setStyle("border:0px;background: " + menuBgColor + ";");

		Hbox hbox = new Hbox();
		hbox.setParent(rowPencarian);
		hbox.setWidth("100%");
		hbox.setPack("center");
		hbox.setAlign("center");

		MyLabelConfig c;
		hbox.appendChild(c = new MyLabelConfig("Cari:"));
		c.setStyle("font-size:11px;");

		cari = new Textbox();
		cari.setCols(10);
		hbox.appendChild(cari);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		hbox.appendChild(button);

		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		cari.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		MyFormRow rowDicari = new MyFormRow();
		rowDicari.setStyle("border:0px;background: " + menuBgColor + ";");
		rowDicari.setParent(rowPencarian.getParent());

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(rowDicari);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		rows = new Rows();
		rows.setParent(grid);
		loadData(cari.getValue());

		return rowPencarian.getGrid();
	}

	public static Grid createMenu() {
		TampilanPengumumanAkademisAction tampilanPengumumanAkademisAction = new TampilanPengumumanAkademisAction();
		tampilanPengumumanAkademisAction.sederhana = true;
		return tampilanPengumumanAkademisAction.loadMenu();
	}

	public void loadData(String keyword) {
		if ((Common.bolehKonfigurasi("aktifkan_menu_baru_untuk_pengguna", Konfigurasi.TIDAK_AKTIF) || Common.isAsliMobile()) && menu instanceof West) {

			menu.detach();

			if (pengumumanAkademis != null) {
				if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanAkademis.getKategoriPengumuman() != null
						&& pengumumanAkademis.getTampilkanPengumumanLain() && KategoriPengumuman.PENGUMUMAN_UTAMA
								.getId().equals(pengumumanAkademis.getKategoriPengumuman().getId())) {
					if (!Common.isMobile()) {
						if (menuPintas != null) {
							menuPintas.setVisible(false);
						}
						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setStyle("border:0px;background: " + menuBgColor + ";font-size: 10px;");
						row.setParent(rows);
						Sekolah sekolah = SekolahUtil.getSekolah();

						tampil(row, pengumumanAkademis, sekolah, tbmuser, selectedPerguruanTinggi, this, false, true,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
												.getAttribute("akademis");
										prosess(akademis.getId(), tabspeng, tabpanelspeng, sederhana, cari);
									}
								});

						row = new MyFormRow();
						row.setStyle("border:0px;background: " + menuBgColor + ";font-size: 10px;");
						row.setParent(rows);
						try {
							ProfileAction.initProfile(tbmuser, row, pengumumanAkademis);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		} else {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = initCriteriaStatic(true, tbmuser, selectedPerguruanTinggi, idPengumuman, session);

				TampilanPengumumanAkademisAction.loadData(rows, keyword, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] obj = (Object[]) arg0.getData();
						Long p = (Long) obj[0];
						Row row = (Row) obj[1];
						Clients.scrollIntoView(row);
						prosess(p, tabspeng, tabpanelspeng, sederhana, cari);
					}
				}, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) arg0.getData();
						prosess(pengumumanAkademis.getId(), tabspeng, tabpanelspeng, true, cari);
						MyMessageboxConfig.showFormat(
								"Kepada Bapak/Ibu, saat ini terdapat Polling / Jejak Pendapat dengan judul \"{V1}\" yang masih perlu Anda isi. Mohon berkenan melengkapi Polling / Jejak Pendapat berikut agar partisipasi dan masukan Anda dapat kami catat.",
								"Polling / Jejak Pendapat", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								pengumumanAkademis.getJudul());
					}
				}, criteria, tbmuser, menuPintas, Common.isMobile(),
						"border:0px;background: " + menuBgColor + ";font-size: 10px;");

			} finally {
				if (session != null) {
					try {
						if (session.isOpen())
							session.clear();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try {
						session.disconnect();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try {
						if (session.isOpen())
							session.close();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			}
		}
	}

	public static void loadData(Rows rows, String keyword, final EventListener eventListener,
			final EventListener pollingEventListener, Criteria criteria, Tbmuser tbmuser, East menuPintas,
			boolean mobile, String style) {

		String currentLang = null;
		try {
			currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}

		Common.clear(rows);
		try {
			if (keyword != null && !keyword.trim().isEmpty()) {
				criteria.add(Restrictions.ilike("judul", keyword.trim(), MatchMode.ANYWHERE));
			}

			List<PengumumanAkademis> pengumumanAkademises = ConstantValues.simpleList(
					criteria.setMaxResults(500).add(Restrictions.or(Restrictions.isNull("langsungMunculDiTab"),
							Restrictions.eq("langsungMunculDiTab", false))),
					PengumumanAkademis.class);

			if (pengumumanAkademises.size() > 0 && (keyword == null || keyword.trim().isEmpty())) {
				PengumumanAkademis pengumumanAkademis = pengumumanAkademises.get(0);
				if (!pengumumanAkademis.getDiperuntukkan().equals(PengumumanAkademis.UNTUK_CALON_MAHASISWA)
						&& !pengumumanAkademis.getDiperuntukkan().equals(PengumumanAkademis.UNTUK_CALON_SISWA)
						&& (KategoriPengumuman.PENGUMUMAN_UTAMA != null
								&& pengumumanAkademis.getKategoriPengumuman() != null
								&& pengumumanAkademis.getTampilkanPengumumanLain()
								&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
										.equals(pengumumanAkademis.getKategoriPengumuman().getId()))) {

					if (!mobile) {
						if (menuPintas != null) {
							menuPintas.setVisible(false);
						}
						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setStyle(style);
						row.setParent(rows);
						ProfileAction.initProfile(tbmuser, row, pengumumanAkademis);
					}
					return;
				}
			}

			KategoriPengumuman kategoriPengumuman = new KategoriPengumuman();
			kategoriPengumuman.setId(-1L);
			boolean sudahTampil = false;

			for (final PengumumanAkademis pengumumanAkademis : pengumumanAkademises) {
				if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanAkademis.getKategoriPengumuman() != null
						&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
								.equals(pengumumanAkademis.getKategoriPengumuman().getId())) {
					continue;
				}

				try {
					if (pengumumanAkademis.getIsiPolling() != null
							&& !pengumumanAkademis.getIsiPolling().trim().isEmpty()) {
						JSONArray isiPollings = new JSONArray(pengumumanAkademis.getIsiPolling());
						if (isiPollings.length() > 0 && tbmuser != null && tbmuser.getUserId() != null) {
							String jaw = pengumumanAkademis.getJawabanPolling() != null
									? pengumumanAkademis.getJawabanPolling()
									: "{}";
							JSONObject jawabanPolling = new JSONObject(jaw);
							boolean terjawab = !jawabanPolling.isNull(tbmuser.getUserId());
							if (!terjawab && !sudahTampil) {
								sudahTampil = true;
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										eventListener.onEvent(new Event("", null, pengumumanAkademis));
									}
								});
								break;
							}
						}
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}

			for (PengumumanAkademis pengumumanAkademis : pengumumanAkademises) {
				if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanAkademis.getKategoriPengumuman() != null
						&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
								.equals(pengumumanAkademis.getKategoriPengumuman().getId())) {
					continue;
				}

				final Long p = pengumumanAkademis.getId();
				try {
					KategoriPengumuman kategoriPengumumanTemporari = (KategoriPengumuman) pengumumanAkademis
							.getKategoriPengumuman();
					if (kategoriPengumumanTemporari != null && (kategoriPengumuman == null
							|| !kategoriPengumuman.getId().equals(kategoriPengumumanTemporari.getId()))) {
						kategoriPengumuman = kategoriPengumumanTemporari;
						String nmKategori = currentLang.equals(Tbmuser.ENGLISH) ? kategoriPengumuman.getNamaEn()
								: kategoriPengumuman.getNama();
						Group group = new ais.ui.util.MyGroupConfig(nmKategori);
						group.setParent(rows);

					} else if (kategoriPengumumanTemporari == null && kategoriPengumuman != null) {
						kategoriPengumuman = null;
						Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
						group.setParent(rows);
					}
				} catch (Exception e) {
					kategoriPengumuman = null;
					Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
					group.setParent(rows);
				}

				final MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setStyle(style);
				row.setParent(rows);

				String text = currentLang.equals(Tbmuser.ENGLISH) ? pengumumanAkademis.getJudulEn()
						: pengumumanAkademis.getJudul();
				if (text == null)
					text = "";
				text = text.length() > 255 ? text.substring(0, 254) + ".." : text;

				final Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig(text);
				toolbarbutton.setStyle("font-size: 11px;");
				row.appendChild(toolbarbutton);

				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(new Event("", null, new Object[] { p, row }));
					}
				});
			}
			pengumumanAkademises.clear();

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void prosess(Long pengumunan, Tabs tabspeng, Tabpanels tabpanelspeng, Component scrollto) {
		prosess(pengumunan, tabspeng, tabpanelspeng, false, scrollto);
	}

	public static void prosess(Long pengumunan, Tabs tabspeng, Tabpanels tabpanelspeng, boolean sederhana,
			Component scrollto) {
		prosess(pengumunan, tabspeng, tabpanelspeng, sederhana, false, scrollto, false);
	}

	@SuppressWarnings({ "unchecked" })
	public static void prosess(Long pengumunan, final Tabs tabspeng, final Tabpanels tabpanelspeng,
			final boolean sederhana, final boolean tampiltanpaWindow, final Component scrollto,
			final boolean tampilPengumumanLain) {

		final PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) ConstantValues
				.ambil(PengumumanAkademis.class.getName(), pengumunan);
		if (pengumumanAkademis != null) {
			List<Component> tabpanelsData = tabpanelspeng == null ? new ArrayList<Component>()
					: tabpanelspeng.getChildren();
			synchronized (tabpanelsData) {

				Window window = null;

				if (!tampiltanpaWindow) {
					String desktopWidth = ExecutionsCtrl.getCurrent().getParameter("desktopWidth");
					if (sederhana || Common.isMobile() || (desktopWidth != null && Integer
							.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE)) {
						window = new Window(pengumumanAkademis.getJudul(), "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("100%");
						window.setWidth("90%");
						Clients.scrollIntoView(window);
					}
				}

				if (window == null) {
					for (Component cc : tabpanelsData) {
						final Tabpanel myTabpanel = (Tabpanel) cc;
						if (myTabpanel.getAttribute("pengumumanAkademis") == null)
							continue;

						PengumumanAkademis myPengumumanAkademis = (PengumumanAkademis) myTabpanel
								.getAttribute("pengumumanAkademis");

						if (myTabpanel != null && myPengumumanAkademis.getId().toString()
								.equals(pengumumanAkademis.getId().toString())) {
							myTabpanel.getLinkedTab().setSelected(true);
							if (scrollto != null) {
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Clients.scrollIntoView(scrollto);
									}
								});
							}
							return;
						}
					}
				}

				final MyTabConfig tab = new MyTabConfig(pengumumanAkademis.getJudul());
				tab.setClosable(true);
				final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

				if (scrollto != null) {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Clients.scrollIntoView(scrollto);
						}
					});
				}

				if (window == null) {
					tab.setParent(tabspeng);
					tabpanel.setParent(tabpanelspeng);
				}

				tabpanel.setAttribute("pengumumanAkademis", pengumumanAkademis);
				tab.setSelected(true);

				Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
				subSubBorderlayout.setParent(window == null ? tabpanel : window);

				Center subcenter = new Center();
				subcenter.setParent(subSubBorderlayout);
				ais.ui.util.ZkCompat.setFlex(subcenter, true);
				subcenter.setBorder("none");

				Tbmuser tbmuser = Common.getCurrentUser();
				PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				Sekolah sekolah = SekolahUtil.getSekolah();

				tampil(subcenter, pengumumanAkademis, sekolah, tbmuser, selectedPerguruanTinggi, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						prosess(pengumumanAkademis.getId(), tabspeng, tabpanelspeng, sederhana, tampiltanpaWindow,
								scrollto, tampilPengumumanLain);
					}
				}, tampilPengumumanLain, false, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget().getAttribute("akademis");
						prosess(akademis.getId(), tabspeng, tabpanelspeng, sederhana, tampiltanpaWindow, scrollto,
								tampilPengumumanLain);
					}
				});

				if (window != null) {
					try {
						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(subSubBorderlayout);

						final Window myWindow = window;
						Toolbar toolbar1 = new Toolbar();
						toolbar1.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								myWindow.detach();
							}
						});
						cancel.setParent(toolbar1);
						window.onModal();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				} else {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Clients.scrollIntoView(tab);
						}
					});
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void sebagaiKelompok(KelompokGelombang kelompokGelombang) throws Exception {
		final MyWindow window = new MyWindow("Pilih Gelombang Pendaftaran", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("85%");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center a = new Center();
		a.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(a);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		try {
			Hbox hbox = PSBAction.headerBox();
			MyFormRow rowUtama1 = new MyFormRow();
			rowUtama1.setSclass("headerHbox");
			rowUtama1.appendChild(hbox);
			ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
			rowUtama1.setValign("top");
			rowUtama1.setParent(rows);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerimaan *"));
		row.appendChild(new Label(kelompokGelombang.getNama()));

		final Combobox gelombang = new Combobox();
		for (GelombangPendaftaranPsb gelombangPendaftaran : kelompokGelombang.gelombangPendaftaranPsbs) {
			Comboitem comboitem = new Comboitem(gelombangPendaftaran.getNama());
			comboitem.setValue(gelombangPendaftaran);
			gelombang.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem("== Pilih salah satu ==");
		comboitem.setValue(null);
		gelombang.appendChild(comboitem);
		gelombang.setSelectedItem(comboitem);
		gelombang.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang *"));
		row.appendChild(gelombang);
		gelombang.setWidth("95%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Pelajaran"));
		final Label ta;
		row.appendChild(ta = new Label());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi"));
		final Html smt;
		row.appendChild(smt = new Html());

		EventListener eventListenerGanti = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaranPsb gelombangPendaftaran = (GelombangPendaftaranPsb) (gelombang
						.getSelectedItem() == null ? null : gelombang.getSelectedItem().getValue());
				ta.setValue(gelombangPendaftaran == null ? "" : gelombangPendaftaran.getTahunAjaran());
				String info = gelombangPendaftaran == null ? "" : gelombangPendaftaran.getInformasi();

				if (gelombangPendaftaran != null) {
					LampiranLain lampiranLain = LampiranLain.ambil(gelombangPendaftaran.getId(), "INFO_PPDB");
					String linkInfo = null;
					if (lampiranLain != null && lampiranLain.getId() != null) {
						try {
							linkInfo = lampiranLain.createLinkUri();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}

					info = info.replaceAll("\n", "<br>");
					info += (linkInfo == null || linkInfo.trim().isEmpty() ? ""
							: (info.trim().isEmpty() ? "" : "<br><br>") + "<a href='#' onClick=\"popupCenter({url: '"
									+ linkInfo + "', title: 'PPDB Info', w: 1200, h: 600});\">"
									+ (gelombangPendaftaran.getKeterangan().isEmpty() ? "" : ", ")
									+ "informasi lebih lanjut klik ini..</a>");
				}
				smt.setContent(info);
			}
		};

		gelombang.addEventListener("onChange", eventListenerGanti);

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaranPsb myGelombangPendaftaranPsb = (GelombangPendaftaranPsb) (gelombang
						.getSelectedItem() == null ? null : gelombang.getSelectedItem().getValue());

				if (myGelombangPendaftaranPsb == null) {
					MyMessageboxConfig.show(
						"Mohon maaf, Bapak/Ibu belum memilih Gelombang pendaftaran. Gelombang wajib dipilih terlebih dahulu agar proses pendaftaran dapat dilanjutkan. Langkah yang dapat dilakukan: (1) klik pada kolom Gelombang; (2) pilih salah satu gelombang yang tersedia; (3) lanjutkan kembali proses pendaftaran.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				CalonSiswa calonSiswa = new CalonSiswa();
				calonSiswa.setGelombangPendaftaranPsb(myGelombangPendaftaranPsb);

				if (myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getHanyaUntukAnakPegawai()) {
					Tbmuser tbmuser = Common.getCurrentUser();
					if (tbmuser != null && tbmuser.getPegawai() != null) {
						calonSiswa.setOrangTuaPegawai(tbmuser.getPegawai());
					}
				}

				EventListener eventListenerPerubahan = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						CalonSiswa cs = (CalonSiswa) arg0.getData();
						Common.masukkanSession(CalonSiswa.class, cs);
					}
				};

				CalonSiswaAction.onAddExternal(null, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
					}
				}, eventListenerPerubahan, calonSiswa, null, null, null, myGelombangPendaftaranPsb);
				window.detach();
			}
		};

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Lanjut Daftar", "/img/save.gif");
		save.setTooltiptext("Lanjut Daftar");
		save.addEventListener("onClick", eventListener);
		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings("deprecation")
	public static void sebagaiKelompokPmb(KelompokGelombang kelompokGelombang) throws Exception {
		final MyWindow window = new MyWindow("Pilih Gelombang Pendaftaran", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("85%");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center a = new Center();
		a.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(a);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		try {
			Hbox hbox = PMBAction.headerBox(false);
			MyFormRow rowUtama1 = new MyFormRow();
			rowUtama1.setSclass("headerHbox");
			rowUtama1.appendChild(hbox);
			ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
			rowUtama1.setValign("top");
			rowUtama1.setParent(rows);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerimaan *"));
		row.appendChild(new Label(kelompokGelombang.getNama()));

		final Combobox gelombang = new Combobox();
		for (GelombangPendaftaran gelombangPendaftaran : kelompokGelombang.gelombangPendaftarans) {
			Comboitem comboitem = new Comboitem(gelombangPendaftaran.getNama());
			comboitem.setValue(gelombangPendaftaran);
			gelombang.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem("== Pilih salah satu ==");
		comboitem.setValue(null);
		gelombang.appendChild(comboitem);
		gelombang.setSelectedItem(comboitem);
		gelombang.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang *"));
		row.appendChild(gelombang);
		gelombang.setWidth("95%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		final Label ta;
		row.appendChild(ta = new Label());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		final Label smtD;
		row.appendChild(smtD = new Label());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi"));
		final Html smt;
		row.appendChild(smt = new Html());

		EventListener eventListenerGanti = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (gelombang.getSelectedItem() == null
						? null
						: gelombang.getSelectedItem().getValue());
				ta.setValue(gelombangPendaftaran == null ? "" : gelombangPendaftaran.getTahunAkademik());
				smtD.setValue(gelombangPendaftaran == null ? "" : gelombangPendaftaran.getJenisSemester());

				String info = gelombangPendaftaran == null ? "" : gelombangPendaftaran.getInfo();

				if (gelombangPendaftaran != null) {
					LampiranLain lampiranLain = LampiranLain.ambil(gelombangPendaftaran.getId(), "INFO_PMB");
					String linkInfo = null;
					if (lampiranLain != null && lampiranLain.getId() != null) {
						try {
							linkInfo = lampiranLain.createLinkUri();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					info = info.replaceAll("\n", "<br>");
					info += (linkInfo == null || linkInfo.trim().isEmpty() ? ""
							: (info.trim().isEmpty() ? "" : "<br><br>") + "<a href='#' onClick=\"popupCenter({url: '"
									+ linkInfo + "', title: 'PMB Info', w: 1200, h: 600});\">"
									+ (gelombangPendaftaran.getKeterangan().isEmpty() ? "" : ", ")
									+ "informasi lebih lanjut klik ini..</a>");
				}
				smt.setContent(info);
			}
		};

		gelombang.addEventListener("onChange", eventListenerGanti);

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaran myGelombangPendaftaran = (GelombangPendaftaran) (gelombang
						.getSelectedItem() == null ? null : gelombang.getSelectedItem().getValue());

				if (myGelombangPendaftaran == null) {
					MyMessageboxConfig.show(
						"Mohon maaf, Bapak/Ibu belum memilih Gelombang pendaftaran. Gelombang wajib dipilih terlebih dahulu agar proses pendaftaran dapat dilanjutkan. Langkah yang dapat dilakukan: (1) klik pada kolom Gelombang; (2) pilih salah satu gelombang yang tersedia; (3) lanjutkan kembali proses pendaftaran.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				ais.action.master.pmb.BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new ais.action.master.pmb.BiodataCalonMahasiswaAction(
						myGelombangPendaftaran, null, null, null, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
							}
						});

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(biodataCalonMahasiswaAction);
				biodataCalonMahasiswaAction.setWidth("900px");
				biodataCalonMahasiswaAction.setHeight("100%");
				biodataCalonMahasiswaAction.onModal();
				window.detach();
			}
		};

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Lanjut Daftar", "/img/save.gif");
		save.setTooltiptext("Lanjut Daftar");
		save.addEventListener("onClick", eventListener);
		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings("deprecation")
	public static void sebagaiALumni(final GelombangPendaftaran gelombangPendaftaran, final JenisSeleksi jenisSeleksi,
			final AfiliasiCalonMahasiswa afiliasiCalonMahasiswaData) throws Exception {

		final MyWindow window = new MyWindow("Pilih Alumni", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("350px");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center a = new Center();
		a.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(a);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		try {
			Hbox hbox = PMBAction.headerBox(false);
			MyFormRow rowUtama1 = new MyFormRow();
			rowUtama1.setSclass("headerHbox");
			rowUtama1.appendChild(hbox);
			ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
			rowUtama1.setValign("top");
			rowUtama1.setParent(rows);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang *"));
		row.appendChild(new Label(gelombangPendaftaran.getNama()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(new Label(gelombangPendaftaran.getTahunAkademik()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(new Label(gelombangPendaftaran.getJenisSemester()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Alumni *"));
		final AmbilDataMahasiswaBanbox alumni = new AmbilDataMahasiswaBanbox(false, true);
		row.appendChild(alumni);
		alumni.setWidth("90%");
		alumni.setReadonly(true);

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa mahasiswaAlumni = (Mahasiswa) alumni.getAttribute("mahasiswa");

				if (mahasiswaAlumni == null) {
					MyMessageboxConfig.show(
						"Mohon maaf, data alumni belum dipilih. Nama alumni wajib dipilih terlebih dahulu agar proses pendaftaran dapat dilanjutkan. Langkah yang dapat dilakukan: (1) klik pada kolom Alumni; (2) pilih nama alumni yang sesuai; (3) lanjutkan kembali proses pendaftaran.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					int sudahDaftar = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("mahasiswaAlumni", mahasiswaAlumni))
							.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					if (sudahDaftar == 0) {
						ais.action.master.pmb.BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new ais.action.master.pmb.BiodataCalonMahasiswaAction(
								gelombangPendaftaran, jenisSeleksi, afiliasiCalonMahasiswaData, mahasiswaAlumni,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
									}
								});

						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(biodataCalonMahasiswaAction);
						biodataCalonMahasiswaAction.setWidth("900px");
						biodataCalonMahasiswaAction.setHeight("100%");
						biodataCalonMahasiswaAction.onModal();
					} else {
						MyMessageboxConfig.show(
							"Mohon maaf, alumni tersebut sudah pernah terdaftar pada gelombang pendaftaran ini. Untuk melanjutkan pendaftaran, silakan masuk (login) menggunakan akun yang telah terdaftar. Langkah yang dapat dilakukan: (1) tutup jendela ini; (2) masuk menggunakan akun alumni yang bersangkutan; (3) lanjutkan proses pendaftaran dari akun tersebut.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					window.detach();
				} finally {
					if (session != null) {
						try {
							if (session.isOpen())
								session.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							session.disconnect();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							if (session.isOpen())
								session.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				}
			}
		};

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Lanjut", "/img/save.gif");
		save.setTooltiptext("Lanjut Tambah Data");
		save.addEventListener("onClick", eventListener);
		save.setParent(toolbar);

		window.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void tampilGelombang(final GelombangPendaftaran gelombangPendaftaranData,
			final PerguruanTinggi selectedPerguruanTinggi, String ta, GelombangPendaftaran gelombangPendaftaran,
			final JenisSeleksi jenisSeleksia, final AfiliasiCalonMahasiswa afiliasiCalonMahasiswaData,
			final Row component, final BiodataCalonMahasiswa biodataCalonMahasiswa, final MyToolbarbutton alur,
			final MyToolbarbutton informasiPembayaran, final MyToolbarbutton loginCalonMhs,
			final MyToolbarbutton informasiKelulusan, final MyToolbarbutton pembayaranViaPaymentGateway,
			final boolean mobile) {

		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();

			if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {

				try {
					String strHasilUjianMahasiswa = biodataCalonMahasiswa.retreive("hasilUjianMahasiswa");
					HasilUjianMahasiswa.tampilkanUjianKembali(strHasilUjianMahasiswa);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				if (Sessions.getCurrent(true).getAttribute("cetak") != null) {
					Sessions.getCurrent(true).removeAttribute("cetak");
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							BiodataCalonMahasiswaAction.onCetakKartu(biodataCalonMahasiswa, true);
						}
					});
				}

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setParent(component);
				grid.setOddRowSclass("non-odd");
				grid.setStyle("border:0px;background: transparent;");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setAlign("center");

				Rows rows = new Rows();
				rows.setParent(grid);
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				boolean tampilkanInformasiPembyaranDiPMB = Common.bolehKonfigurasi("tampilkan_informasi_pembyaran_di_pmb");
				boolean tampilkanInterviewDiPMB = Common.bolehKonfigurasi("tampilkan_interview_di_pmb");
				boolean tampilkanInformasiUjianDiPMB = Common.bolehKonfigurasi("tampilkan_informasi_ujian_di_pmb");
				boolean tampilkanInformasiKelulusan = Common.bolehKonfigurasi("tampilkan_informasi_kelulusan_di_pmb");
				boolean tampilkan_ujian_online_di_pmb = Common.bolehKonfigurasi("tampilkan_ujian_online_di_pmb");
				boolean tampilkanInformasiBuktiDiterima = Common.bolehKonfigurasi("tampilkan_informasi_bukti_diterima_di_pmb");

				GelombangPendaftaran myGelombangPendaftaran = biodataCalonMahasiswa.getGelombangPendaftaran();

				Groupbox groupboxStyled = new Groupbox();
				groupboxStyled.appendChild(new MyCaptionStyled("Profil Calon Mahasiswa"));
				groupboxStyled.setParent(row);
				applyModernGroupboxStyle(groupboxStyled);
				appendPanelDescription(groupboxStyled, "Ringkasan data pendaftaran, status pembayaran, ujian, dan kelulusan Anda tersedia di sini agar langkah berikutnya mudah diketahui.");

				if (myGelombangPendaftaran != null) {
					tampilkanInterviewDiPMB = myGelombangPendaftaran.getTerdapatInterview();
					tampilkan_ujian_online_di_pmb = myGelombangPendaftaran.getTerdapatUjianOnline();
					// GelombangPendaftaran bisa berupa proxy milik session LAIN (dimuat bersama
					// biodataCalonMahasiswa). session.refresh() atasnya melempar "illegally attempted to
					// associate a proxy with two open Sessions". Nilai yang dibutuhkan sudah dibaca di
					// atas, jadi refresh hanya dilakukan bila entity memang milik session ini; selain itu
					// dilewati dengan aman (dibungkus try/catch).
					try {
						if (session.contains(myGelombangPendaftaran)) {
							session.refresh(myGelombangPendaftaran);
						}
					} catch (Exception eRefreshGelombang) { ais.common.ErrorAuditUtil.record(eRefreshGelombang, "auto-audit(empty-catch) src/ais/action/master/TampilanPengumumanAkademisAction.java:1464");
						// proxy detached / milik session lain — lewati refresh, pakai nilai yang sudah ada.
					}

					MyFormRow row1 = new MyFormRow();
					row1.setParent(rows);

					Groupbox groupboxStyledLampiran = new Groupbox();
					groupboxStyledLampiran.appendChild(new MyCaptionStyled("Form Calon Mahasiswa"));
					groupboxStyledLampiran.setParent(row1);
					applyModernGroupboxStyle(groupboxStyledLampiran);
					appendPanelDescription(groupboxStyledLampiran, "Lengkapi jawaban tambahan yang diminta oleh panitia. Data ini membantu proses seleksi dan verifikasi berjalan lebih cepat.");

					Grid gridLampiran = new Grid();
					gridLampiran.setSclass("dgrid fgrid");
					gridLampiran.setParent(groupboxStyledLampiran);
					gridLampiran.setStyle("border:0px;background: transparent;");

					Columns columnsLampiran = new Columns();
					columnsLampiran.setParent(gridLampiran);

					MyColumnConfig columnLampiran = new MyColumnConfig();
					columnLampiran.setParent(columnsLampiran);
					columnLampiran.setAlign("right");

					columnLampiran = new MyColumnConfig();
					columnLampiran.setParent(columnsLampiran);
					columnLampiran.setAlign("left");

					Rows rowsLampiran = new Rows();
					rowsLampiran.setParent(gridLampiran);

					Tbmuser tbmuser = Common.getCurrentUser();
					List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas;

					if (myGelombangPendaftaran != null && myGelombangPendaftaran.getId() != null
							&& !myGelombangPendaftaran.getKelompokParameterTambahanCalonMahasiswas().isEmpty()) {
						// Mulai dari daftar spesifik Gelombang
						Set<Long> idsUdahAda = new HashSet<Long>();
						kelompokParameterTambahanCalonMahasiswas = new ArrayList<KelompokParameterTambahanCalonMahasiswa>();
						for (KelompokParameterTambahanCalonMahasiswa k : myGelombangPendaftaran.getKelompokParameterTambahanCalonMahasiswas()) {
							kelompokParameterTambahanCalonMahasiswas.add(k);
							if (k.getId() != null) idsUdahAda.add(k.getId());
						}
						// Tambah juga kelompok dari ParameterTambahanPaket yg tampilDiSemuaGelombang=true
						List<KelompokParameterTambahanCalonMahasiswa> tambahanSemua = session.createCriteria(ParameterTambahanPaket.class)
								.add(Restrictions.eq("tampilDiSemuaGelombang", true))
								.createAlias("parameterTambahan", "ptSemua")
								.createAlias("kelompokParameterTambahanCalonMahasiswa", "kpSemua")
								.add(Restrictions.eq("ptSemua.aktif", true))
								.add(Restrictions.eq("kpSemua.aktif", true))
								.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa"))
								.add(Restrictions.or(Restrictions.isNull("paket"),
										Restrictions.eq("paket", biodataCalonMahasiswa.getPaket())))
								.list();
						for (KelompokParameterTambahanCalonMahasiswa k : tambahanSemua) {
							if (k.getId() != null && !idsUdahAda.contains(k.getId())) {
								kelompokParameterTambahanCalonMahasiswas.add(k);
								idsUdahAda.add(k.getId());
							}
						}
					} else {
						kelompokParameterTambahanCalonMahasiswas = session.createCriteria(ParameterTambahanPaket.class)
								.add(Restrictions.or(Restrictions.eq("tampilDiSemuaGelombang", true),
										myGelombangPendaftaran == null ? Restrictions.sqlRestriction("false")
												: Restrictions.ilike("gelombangs",
														";" + myGelombangPendaftaran.getId() + ";",
														MatchMode.ANYWHERE)))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanCalonMahasiswa",
										"kelompokParameterTambahanCalonMahasiswa")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
								.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa"))
								.add(Restrictions.or(Restrictions.isNull("paket"),
										Restrictions.eq("paket", biodataCalonMahasiswa.getPaket())))
								.list();
					}

					Collections.sort(kelompokParameterTambahanCalonMahasiswas);
					boolean tampilSemua = false;

					for (KelompokParameterTambahanCalonMahasiswa k : kelompokParameterTambahanCalonMahasiswas) {
						if (k != null && (!k.getTampilDiFormPendaftaran() && tbmuser == null)) {
							continue;
						} else if (k != null && !k.getTampilDiFormSetelahLogin()) {
							continue;
						}

						MyFormRow rowParameterTambahan = new MyFormRow();
						rowParameterTambahan.setVisible(false);
						rowParameterTambahan.setParent(rowsLampiran);
						rowParameterTambahan.appendChild(new ais.ui.util.MyLabelStyled(k.getNama()));
						rowParameterTambahan.appendChild(new MyLabelStyled("Data"));

						List<ParameterTambahan> parameterTambahans = ConstantValues
								.simpleList(
										session.createCriteria(ParameterTambahanPaket.class)
												.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa", k))
												.add(Restrictions
														.or(Restrictions.eq("tampilDiSemuaGelombang", true),
																myGelombangPendaftaran == null
																		? Restrictions.sqlRestriction("false")
																		: Restrictions.ilike("gelombangs",
																				";" + myGelombangPendaftaran.getId()
																						+ ";",
																				MatchMode.ANYWHERE)))
												.createAlias("parameterTambahan", "parameterTambahan")
												.createAlias("kelompokParameterTambahanCalonMahasiswa",
														"kelompokParameterTambahanCalonMahasiswa")
												.add(Restrictions.eq("parameterTambahan.aktif", true))
												.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif",
														true))
												.setProjection(Projections.groupProperty("parameterTambahan.id"))
												.add(Restrictions.or(Restrictions.isNull("paket"),
														Restrictions.eq("paket", biodataCalonMahasiswa.getPaket()))),
										ParameterTambahan.class, false);

						Collections.sort(parameterTambahans);

						boolean tampil = false;
						rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());

						if (!parameterTambahans.isEmpty()) {
							for (ParameterTambahan parameterTambahan : parameterTambahans) {
								String jenis = k.getId() + "->" + parameterTambahan.getId();
								Row rowLampiran = new MyRowStyled();
								rowLampiran.setAttribute("parameterTambahan", parameterTambahan);
								rowLampiran.setAttribute("kelompokParameterTambahanCalonMahasiswa", k);
								rowLampiran.setParent(rowsLampiran);
								rowLampiran
										.appendChild(new ais.ui.util.MyLabelConfig(parameterTambahan.getLabelInputan()
												+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

								String val = "";
								String ket = "";
								String[] spl = biodataCalonMahasiswa.getParameterTambahanInds().split("\n");
								for (String d : spl) {
									String[] value = d.split("<=>");
									if (value[0].trim().equalsIgnoreCase(jenis)) {
										val = value.length > 1 ? value[1].trim() : "";
										try {
											ket = value.length > 0 ? value[value.length - 1] : "";
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								}
								tampil |= ParameterTambahan.initComponent(rowLampiran, rowsLampiran, jenis, null, null,
										biodataCalonMahasiswa.getId(), val, ket, parameterTambahan, null);
							}
						}

						rowParameterTambahan.setVisible(tampil);
						tampilSemua |= tampil;
					}
					row1.setVisible(tampilSemua);
				}

				MyFormRow rowBerkas = new MyFormRow();
				rowBerkas.setParent(rows);

				Groupbox groupboxStyledBerkas = new Groupbox();
				groupboxStyledBerkas.appendChild(new MyCaptionStyled("Berkas Calon Mahasiswa"));
				groupboxStyledBerkas.setParent(rowBerkas);
				applyModernGroupboxStyle(groupboxStyledBerkas);
				appendPanelDescription(groupboxStyledBerkas, "Unggah dan periksa dokumen persyaratan pendaftaran. Status berkas membantu Anda mengetahui dokumen yang sudah lengkap dan yang masih perlu diperbaiki.");

				Grid gridBerkas = new Grid();
				gridBerkas.setSclass("dgrid fgrid");
				gridBerkas.setParent(groupboxStyledBerkas);
				gridBerkas.setStyle("border:0px;background: transparent;");

				Columns columnsBerkas = new Columns();
				columnsBerkas.setParent(gridBerkas);

				MyColumnConfig columnBerkas = new MyColumnConfig();
				columnBerkas.setParent(columnsBerkas);
				columnBerkas.setAlign("center");

				Rows rowsBerkas = new Rows();
				rowsBerkas.setParent(gridBerkas);

				Row rowVerifikasi = new MyRowStyled();
				rowVerifikasi.setParent(rowsBerkas);

				Rows subRowsVerifikasi = new Rows();
				EventListener eventListenerBerkas = VerifikasiPMBHelper.tampilkanGrid(rowVerifikasi, subRowsVerifikasi,
						biodataCalonMahasiswa.getGelombangPendaftaran(),
						biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getId(), null, true);
				try {
					eventListenerBerkas.onEvent(null);
					rowBerkas.setVisible(rowVerifikasi.isVisible());
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				if (biodataCalonMahasiswa.getMahasiswa() != null) {
					MyFormRow rowDiterima = new MyFormRow();
					rowDiterima.setParent(rows);

					Groupbox groupboxDiterima = new Groupbox();
					groupboxDiterima.appendChild(
							new MyCaptionStyled("Selamat, Anda telah dinyatakan diterima sebagai mahasiswa"));
						appendPanelDescription(groupboxDiterima, "Informasi kelulusan dan langkah daftar ulang ditampilkan agar Anda dapat melanjutkan proses menjadi mahasiswa tanpa bingung.");
					groupboxDiterima.setParent(rowDiterima);
					applyModernGroupboxStyle(groupboxDiterima);

					Grid gridDiterima = new Grid();
					gridDiterima.setSclass("dgrid fgrid");
					gridDiterima.setParent(groupboxDiterima);
					gridDiterima.setStyle("border:0px;background: transparent;");

					Columns columnsDiterima = new Columns();
					columnsDiterima.setParent(gridDiterima);

					MyColumnConfig colDiterimaRight = new MyColumnConfig();
					colDiterimaRight.setParent(columnsDiterima);
					colDiterimaRight.setAlign("right");

					MyColumnConfig colDiterimaLeft = new MyColumnConfig();
					colDiterimaLeft.setParent(columnsDiterima);
					colDiterimaLeft.setAlign("left");

					Rows rowsDiterima = new Rows();
					rowsDiterima.setParent(gridDiterima);

					Row rowItem = new MyRowStyled();
					rowItem.setParent(rowsDiterima);
					rowItem.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
					rowItem.appendChild(new Label(biodataCalonMahasiswa.getMahasiswa().getNim()));

					rowItem = new MyRowStyled();
					rowItem.setParent(rowsDiterima);
					rowItem.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
					String namaJurusan = biodataCalonMahasiswa.getMahasiswa().getJurusan() != null
							? biodataCalonMahasiswa.getMahasiswa().getJurusan().getNama()
							: "-";
					rowItem.appendChild(new Label(namaJurusan));

					rowItem = new MyRowStyled();
					rowItem.setParent(rowsDiterima);
					rowItem.appendChild(new ais.ui.util.MyLabelConfig("Program"));
					rowItem.appendChild(new Label(biodataCalonMahasiswa.getMahasiswa().getProgram()));

					rowItem = new MyRowStyled();
					rowItem.setParent(rowsDiterima);
					rowItem.appendChild(new ais.ui.util.MyLabelConfig("Status"));
					String statusAwal = biodataCalonMahasiswa.getMahasiswa().getStatusAwalMahasiswa() != null
							? biodataCalonMahasiswa.getMahasiswa().getStatusAwalMahasiswa().getNama()
							: "-";
					rowItem.appendChild(new Label(statusAwal));

					rowItem = new MyRowStyled();
					rowItem.setParent(rowsDiterima);
					rowItem.appendChild(new ais.ui.util.MyLabelConfig("Cetak Bioadata"));
					MyToolbarbuttonConfig buttonCetakBio = new MyToolbarbuttonConfig("Biodata",
							"/img/online-icon_access.png");
					buttonCetakBio.setOrient("vertical");
					buttonCetakBio.setStyle("font-size:9px;");
					buttonCetakBio.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							BiodataMahasiswa biodataMahasiswa = biodataCalonMahasiswa.getMahasiswa().ambilBiodata();
							CommonReportHelper.onCetakBiodataMahasiswa(biodataMahasiswa);
						}
					});
					buttonCetakBio.setParent(rowItem);

					rowItem = new MyRowStyled();
					rowItem.setParent(rowsDiterima);
					rowItem.appendChild(new ais.ui.util.MyLabelConfig("Login Mahasiswa"));
					final A aLogin = new A("Tampilkan Link Login");
					aLogin.setHref("");
					rowItem.appendChild(aLogin);

					aLogin.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String code = biodataCalonMahasiswa.getMahasiswa().urlLogin();
							aLogin.setLabel(code);
							aLogin.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
									+ URLEncoder.encode(code, "UTF-8"));
						}
					});

					try {
						if (myGelombangPendaftaran != null
								&& myGelombangPendaftaran.getTampilkanQrCodeMahasiswaSetelahDapatNim()) {
							rowItem = new MyRowStyled();
							ais.ui.util.ZkCompat.setSpans(rowItem, "2");
							rowItem.setParent(rowsDiterima);
							MainHelper.onDapatkanKode(new Tbmuser(biodataCalonMahasiswa.getMahasiswa()), rowItem,
									false);
						}
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

				// Wadah tombol aksi — transparan, tanpa border, responsif
				Groupbox groupboxStyled1 = new Groupbox();
				groupboxStyled1.setParent(groupboxStyled);
				groupboxStyled1.setStyle("border:0;padding:0;background:transparent;max-width:100%;");

				String url = "";
				try {
					url = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa));
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				// Pra-komputasi data pembayaran agar bisa dipakai di HTML dan progress tracker
				Kegiatan kegiatan = null;
				Kegiatan kegiatanDaftarUlang = null;
				String teksBayarReg = "";
				String teksBayarReg2 = "";  // warna untuk HTML
				String teksBayarDaftarUlang = "";
				String teksBayarDU2 = "";   // warna untuk HTML
				Double totalDaftarUlangSetting = null;
				if (tampilkanInformasiPembyaranDiPMB) {
					kegiatan = biodataCalonMahasiswa.chekPembayaranRegistrasi();
					kegiatanDaftarUlang = biodataCalonMahasiswa.chekPembayaranDaftarUlang();
					totalDaftarUlangSetting = hitungBiayaDaftarUlangCalonMahasiswa(biodataCalonMahasiswa);

					double dibayarRegistrasi = kegiatan == null ? 0.0 : safeDouble(kegiatan.getAmount());
					double persentaseRegistrasi = kegiatan == null ? 0.0 : safeDouble(kegiatan.getPersentaseLunas());
					double totalRegistrasiKegiatan = kegiatan == null ? 0.0
							: safeDouble(kegiatan.getAmount()) + safeDouble(kegiatan.getAmountTerhutang());
					double totalRegistrasiSetting = hitungBiayaRegistrasiCalonMahasiswa(biodataCalonMahasiswa);
					double totalRegistrasi = totalRegistrasiKegiatan > 0.01
							? totalRegistrasiKegiatan
							: totalRegistrasiSetting;

					if (totalRegistrasi < 0.01) {
						teksBayarReg = Common.getBahasaConfig("Bebas") + " " + Common.getBahasaConfig("Pembayaran Reg.")
								+ " " + Common.getBahasaConfig("(Gratis)");
						teksBayarReg2 = "<span style='color:#16a34a;'>" + htmlEscape(teksBayarReg) + "</span>";
					} else if (dibayarRegistrasi < 0.01 && persentaseRegistrasi < 0.01) {
						teksBayarReg = "Belum Bayar " + Common.numberFormat.get().format(totalRegistrasi);
						teksBayarReg2 = "<span style='color:#dc2626;'>" + htmlEscape(teksBayarReg) + "</span>";
					} else if (persentaseRegistrasi >= 99.99) {
						teksBayarReg = "Lunas " + Common.numberFormat.get().format(totalRegistrasi);
						teksBayarReg2 = "<span style='color:#16a34a;'>&#10003; " + htmlEscape(teksBayarReg) + "</span>";
					} else {
						teksBayarReg = "Bayar " + Common.numberFormat.get().format(dibayarRegistrasi) + " dari tagihan "
								+ Common.numberFormat.get().format(totalRegistrasi)
								+ " atau " + Common.numberFormat.get().format(persentaseRegistrasi) + "%";
						teksBayarReg2 = "<span style='color:#d97706;'>" + htmlEscape(teksBayarReg) + "</span>";
					}

					if (biodataCalonMahasiswa.getProdiLulus() == null) {
						teksBayarDaftarUlang = "Menunggu hasil seleksi";
						teksBayarDU2 = "<span style='color:#92400e;'>" + htmlEscape(teksBayarDaftarUlang) + "</span>";
					} else if (totalDaftarUlangSetting == null) {
						double sudahDibayar = kegiatanDaftarUlang == null ? 0.0
								: safeDouble(kegiatanDaftarUlang.getAmount());
						if (pembayaranDaftarUlangSudahLunas(kegiatanDaftarUlang, totalDaftarUlangSetting)) {
							teksBayarDaftarUlang = "Lunas " + Common.numberFormat.get().format(sudahDibayar)
									+ " (setting tagihan prodi tidak ditemukan)";
							teksBayarDU2 = "<span style='color:#16a34a;'>&#10003; "
									+ htmlEscape(teksBayarDaftarUlang) + "</span>";
						} else {
							teksBayarDaftarUlang = sudahDibayar < 0.01 ? "Belum ada tagihan untuk prodi ini"
									: "Pembayaran tercatat " + Common.numberFormat.get().format(sudahDibayar)
											+ ", tetapi setting tagihan prodi tidak ditemukan";
							teksBayarDU2 = "<span style='color:#64748b;'>" + htmlEscape(teksBayarDaftarUlang) + "</span>";
						}
					} else if (totalDaftarUlangSetting.doubleValue() < 0.01) {
						teksBayarDaftarUlang = Common.getBahasaConfig("Bebas") + " "
								+ Common.getBahasaConfig("Pembayaran Daftar Ulang") + " "
								+ Common.getBahasaConfig("(Gratis)");
						teksBayarDU2 = "<span style='color:#16a34a;'>" + htmlEscape(teksBayarDaftarUlang) + "</span>";
					} else if (kegiatanDaftarUlang == null || safeDouble(kegiatanDaftarUlang.getAmount()) < 0.01) {
						teksBayarDaftarUlang = "Belum Bayar " + Common.numberFormat.get()
								.format(totalDaftarUlangSetting);
						teksBayarDU2 = "<span style='color:#dc2626;'>" + htmlEscape(teksBayarDaftarUlang) + "</span>";
					} else if (safeDouble(kegiatanDaftarUlang.getAmount()) + 0.01 >= totalDaftarUlangSetting.doubleValue()) {
						teksBayarDaftarUlang = "Lunas " + Common.numberFormat.get().format(totalDaftarUlangSetting);
						teksBayarDU2 = "<span style='color:#16a34a;'>&#10003; " + htmlEscape(teksBayarDaftarUlang) + "</span>";
					} else {
						teksBayarDaftarUlang = "Bayar " + Common.numberFormat.get().format(kegiatanDaftarUlang.getAmount())
								+ " dari tagihan "
								+ Common.numberFormat.get().format(totalDaftarUlangSetting)
								+ " atau " + Common.numberFormat.get().format(
										100.0 * safeDouble(kegiatanDaftarUlang.getAmount()) / totalDaftarUlangSetting.doubleValue())
								+ "%";
						teksBayarDU2 = "<span style='color:#d97706;'>" + htmlEscape(teksBayarDaftarUlang) + "</span>";
					}
				}

				// Badge status pendaftaran
				String statusBadgeTxt;
				String statusBadgeColor;
				String statusBadgeBg;
				if (biodataCalonMahasiswa.getMundur()) {
					statusBadgeTxt = "Mengundurkan Diri";
					statusBadgeColor = "#991b1b"; statusBadgeBg = "#fee2e2";
				} else if (biodataCalonMahasiswa.getDitolak()) {
					statusBadgeTxt = "Tidak Diterima";
					statusBadgeColor = "#7c3aed"; statusBadgeBg = "#ede9fe";
				} else if (biodataCalonMahasiswa.getProdiLulus() != null) {
					statusBadgeTxt = "&#10003; Diterima";
					statusBadgeColor = "#166534"; statusBadgeBg = "#dcfce7";
				} else {
					statusBadgeTxt = "Menunggu Seleksi";
					statusBadgeColor = "#92400e"; statusBadgeBg = "#fef3c7";
				}

				// Status teks + warna untuk baris info
				String diterima;
				String diterimaClr;
				if (biodataCalonMahasiswa.getMundur()) {
					diterima = "Mengundurkan diri"; diterimaClr = "#dc2626";
				} else if (biodataCalonMahasiswa.getDitolak()) {
					diterima = "Tidak diterima (ditolak)"; diterimaClr = "#7c3aed";
				} else if (biodataCalonMahasiswa.getProdiLulus() != null) {
					diterima = biodataCalonMahasiswa.getProdiLulus().getNama(); diterimaClr = "#16a34a";
				} else {
					diterima = "Belum dinyatakan lulus / diterima"; diterimaClr = "#92400e";
				}

				// Kumpulkan pilihan prodi
				StringBuilder prodiB = new StringBuilder();
				if (biodataCalonMahasiswa.getProdi1() != null)
					prodiB.append(htmlEscape(biodataCalonMahasiswa.getProdi1().getNama()));
				if (biodataCalonMahasiswa.getProdi2() != null)
					prodiB.append(prodiB.length() > 0 ? ", " : "").append(htmlEscape(biodataCalonMahasiswa.getProdi2().getNama()));
				if (biodataCalonMahasiswa.getProdi3() != null)
					prodiB.append(prodiB.length() > 0 ? ", " : "").append(htmlEscape(biodataCalonMahasiswa.getProdi3().getNama()));
				if (biodataCalonMahasiswa.getProdi4() != null)
					prodiB.append(prodiB.length() > 0 ? ", " : "").append(htmlEscape(biodataCalonMahasiswa.getProdi4().getNama()));
				if (biodataCalonMahasiswa.getProdi5() != null)
					prodiB.append(prodiB.length() > 0 ? ", " : "").append(htmlEscape(biodataCalonMahasiswa.getProdi5().getNama()));

				RuangPaketPMB ruangPaketPMB = (RuangPaketPMB) session.createCriteria(RuangPaketPMB.class)
						.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)).setMaxResults(1)
						.uniqueResult();

				StringBuilder htmlBuilder = new StringBuilder();

				// === HEADER KARTU: foto bulat 64px + nama + no.reg + badge status ===
				htmlBuilder
					.append("<div style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;padding:10px 12px 4px 12px;'>")
					.append("<div style='text-align:center;padding-bottom:14px;border-bottom:1px solid #e8ecf0;margin-bottom:10px;'>")
					.append("<div style='margin:0 auto 10px auto;width:192px;height:192px;border-radius:50%;")
					.append("overflow:hidden;border:4px solid #e2e8f0;box-shadow:0 4px 16px rgba(0,0,0,0.15);background:#f8fafc;'>")
					.append("<img src='")
					.append(htmlEscape(url))
					.append("' style='width:192px;height:192px;object-fit:cover;display:block;'/>")
					.append("</div>")
					.append("<div style='font-weight:700;font-size:15px;color:#1e293b;line-height:1.35;'>")
					.append(htmlEscape(safeText(biodataCalonMahasiswa.getNama()))).append("</div>")
					.append("<div style='font-size:12px;color:#64748b;margin-top:3px;'>No. Reg: ")
					.append(htmlEscape(safeText(biodataCalonMahasiswa.getNoRegistrasi()))).append("</div>")
					.append("<div style='margin-top:7px;'>")
					.append("<span style='display:inline-block;padding:3px 12px;border-radius:20px;")
					.append("font-size:11px;font-weight:600;background:").append(statusBadgeBg)
					.append(";color:").append(statusBadgeColor).append(";'>")
					.append(statusBadgeTxt).append("</span></div>")
					.append("</div>")
					// === TABEL INFO ===
					.append("<table style='width:100%;border-collapse:collapse;'>")
					.append(buildInfoRow(Common.getBahasaConfig("Gelombang"),
						htmlEscape(myGelombangPendaftaran == null ? "" : myGelombangPendaftaran.getNama())))
					.append(buildInfoRow(Common.getBahasaConfig("Jenis Seleksi"),
						htmlEscape(biodataCalonMahasiswa.getJenisSeleksi() == null ? ""
								: biodataCalonMahasiswa.getJenisSeleksi().getNama())))
					.append(buildInfoRow(Common.getBahasaConfig("Paket"),
						htmlEscape(biodataCalonMahasiswa.getPaket() == null ? "" : biodataCalonMahasiswa.getPaket().getNama())))
					.append(buildInfoRow(Common.getBahasaConfig("Periode"),
						htmlEscape(myGelombangPendaftaran == null ? ""
								: myGelombangPendaftaran.getTahunAkademik() + " / "
										+ myGelombangPendaftaran.getJenisSemester())))
					.append(buildInfoRow(Common.getBahasaConfig("No. Registrasi"),
						htmlEscape(safeText(biodataCalonMahasiswa.getNoRegistrasi()))))
					.append(buildInfoRow(Common.getBahasaConfig("No. Ujian"),
						htmlEscape(biodataCalonMahasiswa.getNoUjian() == null ? "-" : biodataCalonMahasiswa.getNoUjian())))
					.append(buildInfoRow(Common.getBahasaConfig("Nama"),
						"<strong>" + htmlEscape(safeText(biodataCalonMahasiswa.getNama())) + "</strong>"))
					.append(buildInfoRow(Common.getBahasaConfig("Tempat dan Tanggal Lahir"),
						htmlEscape(safeText(biodataCalonMahasiswa.getTempatLahir())) + ", "
						+ (biodataCalonMahasiswa.getTanggalLahir() == null ? ""
								: Common.dateFormat2.get().format(biodataCalonMahasiswa.getTanggalLahir()))))
					.append(buildInfoRow(Common.getBahasaConfig("Prodi Pilihan"), prodiB.toString()))
					.append(buildInfoRow(Common.getBahasaConfig("Prodi Diterima"),
						"<span style='color:" + diterimaClr + ";font-weight:600;'>" + htmlEscape(diterima) + "</span>"))
					.append(buildInfoRow(Common.getBahasaConfig("Program"),
						htmlEscape(safeText(biodataCalonMahasiswa.getProgram()))))
					.append(buildInfoRow(Common.getBahasaConfig("Ruang/Ujian (jika ada ujian)"),
						htmlEscape(ruangPaketPMB == null || ruangPaketPMB.getRuangPMB() == null ? ""
								: ruangPaketPMB.getRuangPMB().getNama())));

				if (tampilkanInformasiPembyaranDiPMB
						&& (myGelombangPendaftaran == null
								|| myGelombangPendaftaran
										.getTampilkanStatusPembayaranRegistrasiDiHalamanAwal())) {
					htmlBuilder.append(
							buildInfoRow(Common.getBahasaConfig("Pembayaran Reg."), teksBayarReg2));
				}
				if (tampilkanInformasiPembyaranDiPMB
						&& (myGelombangPendaftaran == null
								|| myGelombangPendaftaran
										.getTampilkanStatusPembayaranDaftarUlangDiHalamanAwal())) {
					htmlBuilder.append(
							buildInfoRow(Common.getBahasaConfig("Status/Daftar Ulang"), teksBayarDU2));
				}

				htmlBuilder.append("</table>");

				// === PROGRESS TRACKER HTML/CSS — 4 langkah pendaftaran mahasiswa ===
				boolean sudahBayarPrg = kegiatan != null && kegiatan.getPersentaseLunas() != null
						&& kegiatan.getPersentaseLunas() >= 100;
				boolean sudahSeleksiPrg = biodataCalonMahasiswa.getProdiLulus() != null
						|| biodataCalonMahasiswa.getDitolak() || biodataCalonMahasiswa.getMundur();
				boolean sudahDaftarUlangPrg = pembayaranDaftarUlangSudahLunas(kegiatanDaftarUlang,
						totalDaftarUlangSetting);
				htmlBuilder.append(buildProgressTrackerPmb(true, sudahBayarPrg, sudahSeleksiPrg, sudahDaftarUlangPrg));
				htmlBuilder.append("</div>"); // tutup div wrapper font-family
				groupboxStyled.appendChild(new Html(htmlBuilder.toString()));

				MyToolbarbutton logout = new MyToolbarbutton("fa-sign-out", "Logout");
				logout.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						PMBAction.setLogoutCalonMahasiswaPMB();
						PMBAction.redirectSetelahLogoutPMB();
					}
				});

				MyToolbarbutton ujian = new MyToolbarbutton("fa-pencil-square", "Ikut Ujian Sekarang");
				ujian.setVisible(tampilkan_ujian_online_di_pmb);
				ujian.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Session sessionAction = null;
								try {
									sessionAction = HibernateUtil.getSessionFactory().openSession();
									sessionAction.refresh(biodataCalonMahasiswa);

									boolean harusBayarSebelumLogin = biodataCalonMahasiswa.getGelombangPendaftaran()
											.getHarusBayarSebelumBisaLogin();
									if (harusBayarSebelumLogin) {
										if (biodataCalonMahasiswa.getPembayaranRegistrasi() == null
												|| biodataCalonMahasiswa.getPembayaranRegistrasi()
														.getPersentaseLunas() < 0.01) {
											MyMessageboxConfig.show(
										"Mohon maaf, Anda belum dapat mengikuti ujian karena pembayaran registrasi belum dilakukan. Pembayaran registrasi wajib diselesaikan terlebih dahulu sebelum mengikuti ujian. Langkah yang dapat dilakukan: (1) lakukan pembayaran registrasi sesuai ketentuan; (2) tunggu proses verifikasi pembayaran; (3) setelah pembayaran terverifikasi, silakan mengikuti ujian kembali.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}
									}

									if (biodataCalonMahasiswa.getGelombangPendaftaran()
											.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()
											&& !BiodataCalonMahasiswaAction.lengkap(biodataCalonMahasiswa)) {
										MyMessageboxConfig.showFormat(
									"Mohon maaf, Anda belum dapat melanjutkan proses ini karena data dan berkas Anda belum lengkap. Mohon lengkapi terlebih dahulu data diri Anda melalui menu \"{V1}\". Langkah yang dapat dilakukan: (1) buka menu \"{V1}\"; (2) lengkapi seluruh data dan berkas yang masih kosong; (3) simpan perubahan, kemudian ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, loginCalonMhs.getLabelC().getValue());
										return;
									}

									if (!VerifikasiPMBHelper.checkVerifikasiSebelumUjian(biodataCalonMahasiswa)) {
										return;
									}

									if (biodataCalonMahasiswa.getGelombangPendaftaran() != null
											&& biodataCalonMahasiswa.getGelombangPendaftaran()
													.getDokumenHarusDiverivikasiSebelumBisaIkutUjian()) {
										if (!VerifikasiPMBHelper.checkVerifikasi(biodataCalonMahasiswa)) {
											return;
										}
									}

									TampilanUjianCalonMahasiswa tampilanUjianCalonMahasiswa = new TampilanUjianCalonMahasiswa(
											true);
									tampilanUjianCalonMahasiswa.init(biodataCalonMahasiswa);
									tampilanUjianCalonMahasiswa
											.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
									tampilanUjianCalonMahasiswa.setHeight("100%");
									tampilanUjianCalonMahasiswa.setWidth("90%");
									tampilanUjianCalonMahasiswa.onModal();
								} finally {
									if (sessionAction != null && sessionAction.isOpen()) {
										try {
											sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								}
							}
						});
					}
				});

				MyToolbarbutton reg = new MyToolbarbutton("fa-print", "No.Reg");
				reg.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						BiodataCalonMahasiswaAction.onCetakKartu(biodataCalonMahasiswa, false);
					}
				});

				MyToolbarbutton bio = new MyToolbarbutton("fa-user-circle", "Biodata");
				bio.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);

							if (biodataCalonMahasiswa.getGelombangPendaftaran()
									.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()
									&& !BiodataCalonMahasiswaAction.lengkap(biodataCalonMahasiswa)) {
								MyMessageboxConfig.showFormat(
									"Mohon maaf, Anda belum dapat melanjutkan proses ini karena data dan berkas Anda belum lengkap. Mohon lengkapi terlebih dahulu data diri Anda melalui menu \"{V1}\". Langkah yang dapat dilakukan: (1) buka menu \"{V1}\"; (2) lengkapi seluruh data dan berkas yang masih kosong; (3) simpan perubahan, kemudian ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, loginCalonMhs.getLabelC().getValue());
								return;
							}
							CommonReportHelper.onCetakBiodataCalonMahasiswa(biodataCalonMahasiswa, true);
						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				MyToolbarbutton kartuUjian = new MyToolbarbutton("fa-pencil-square", "Kartu Ujian");
				kartuUjian.setVisible(tampilkanInformasiUjianDiPMB);
				kartuUjian.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);

							if (biodataCalonMahasiswa.getGelombangPendaftaran()
									.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()
									&& !BiodataCalonMahasiswaAction.lengkap(biodataCalonMahasiswa)) {
								MyMessageboxConfig.showFormat(
									"Mohon maaf, Anda belum dapat melanjutkan proses ini karena data dan berkas Anda belum lengkap. Mohon lengkapi terlebih dahulu data diri Anda melalui menu \"{V1}\". Langkah yang dapat dilakukan: (1) buka menu \"{V1}\"; (2) lengkapi seluruh data dan berkas yang masih kosong; (3) simpan perubahan, kemudian ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, loginCalonMhs.getLabelC().getValue());
								return;
							}
							CommonReportHelper.onCetakKartuUjianPMB(Common.getCurrentUser(), biodataCalonMahasiswa,
									biodataCalonMahasiswa.getNoUjian());
						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				MyToolbarbutton bayarRegistrasi = new MyToolbarbutton("fa-money", "Bayar Registrasi");
				bayarRegistrasi.setVisible(tampilkanInformasiPembyaranDiPMB && kegiatan != null
						&& kegiatan.getPersentaseLunas() < 99.0 && TampilanPaymentGateway.adaPaymentGatewayYangAktif());
				bayarRegistrasi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);
							TampilanPaymentGateway.tampilPembayaranRegistrasi(biodataCalonMahasiswa);
						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				MyToolbarbutton bayarDaftarUlang = new MyToolbarbutton("fa-money", "Bayar Daftar Ulang");
				bayarDaftarUlang.setVisible(tampilkanInformasiPembyaranDiPMB && kegiatanDaftarUlang != null
						&& kegiatanDaftarUlang.getPersentaseLunas() < 100.0);
				bayarDaftarUlang.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);
							TampilanPaymentGateway.tampilPembayaranDaftarUlang(biodataCalonMahasiswa);
						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				if (Common.bolehKonfigurasi("calon_mahasiswa_harus_lulus_sebelum_bayar_daftar_ulang")) {
					Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
					if (prodiLulus == null || prodiLulus.getId() == null) {
						bayarDaftarUlang.setVisible(false);
					}
				}
				if (bayarDaftarUlang.isVisible()
						&& !pembayaranDaftarUlangMasihDibuka(biodataCalonMahasiswa, kegiatanDaftarUlang)) {
					bayarDaftarUlang.setVisible(false);
				}

				MyToolbarbutton uploadRegistrasi = new MyToolbarbutton("fa-upload",
						"Upload Bukti Pembayaran");
				uploadRegistrasi.setVisible(tampilkanInformasiPembyaranDiPMB);
				uploadRegistrasi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);

							final MyWindow addWindow = new MyWindow("Upload Bukti Pembayaran", "none", true);
							addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							Borderlayout borderlayout = new Borderlayout();
							addWindow.appendChild(borderlayout);
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							MyGrid gridUpload = new MyGrid();
							gridUpload.setWidth("100%");
							gridUpload.setParent(center);
							gridUpload.setHeight("100%");

							Columns columnsUp = new Columns();
							columnsUp.setParent(gridUpload);
							new MyColumnConfig().setParent(columnsUp);
							new MyColumnConfig().setParent(columnsUp);

							Rows rowsUp = new Rows();
							rowsUp.setParent(gridUpload);

							MyFormRow rowUp = new MyFormRow();
							rowUp.setValign("top");
							rowUp.setParent(rowsUp);

							MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
									"Upload " + LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN
											+ Common.ukuranLabelFileUpload(),
									"/img/excel.png");
							upload.setUpload(Common.ukuranFileUpload());
							rowUp.appendChild(upload);

							upload.addEventListener("onUpload", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									UploadEvent uploadEvent = (UploadEvent) event;
									new ProsesUploadBuktiPembayaran(biodataCalonMahasiswa).upload(uploadEvent,
											LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN);
								}
							});

							MyToolbarbuttonConfig upload1 = new MyToolbarbuttonConfig(
									"Upload " + LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG
											+ Common.ukuranLabelFileUpload(),
									"/img/excel.png");
							rowUp.appendChild(upload1);

							upload1.setUpload(Common.ukuranFileUpload());
							upload1.addEventListener("onUpload", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									UploadEvent uploadEvent = (UploadEvent) event;
									new ProsesUploadBuktiPembayaran(biodataCalonMahasiswa).upload(uploadEvent,
											LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG);
								}
							});

							addWindow.setHeight("250px");
							addWindow.setWidth("600px");

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									addWindow.detach();
								}
							});
							cancel.setParent(toolbar);
							addWindow.onModal();

						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				MyToolbarbutton uploadInterview = new MyToolbarbutton("fa-handshake-o", "Interview");
				uploadInterview.setVisible(tampilkanInterviewDiPMB);
				uploadInterview.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						InterviewCalonMahasiswaAction.tampilkanInterview(biodataCalonMahasiswa);
					}
				});

				final Kegiatan fKegiatan = kegiatan;
				final Kegiatan fKegiatanDaftarUlang = kegiatanDaftarUlang;

				MyToolbarbutton cetakRegistrasi = new MyToolbarbutton("fa-print", "Cetak Bukti Pembayaran");
				cetakRegistrasi.setVisible(tampilkanInformasiPembyaranDiPMB);
				cetakRegistrasi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);

							final MyWindow addWindow = new MyWindow("Cetak Bukti Pembayaran", "none", true);
							addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							Borderlayout borderlayout = new Borderlayout();
							addWindow.appendChild(borderlayout);
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							MyGrid gridCtk = new MyGrid();
							gridCtk.setWidth("100%");
							gridCtk.setParent(center);
							gridCtk.setHeight("100%");

							Columns columnsCtk = new Columns();
							columnsCtk.setParent(gridCtk);
							new MyColumnConfig().setParent(columnsCtk);
							new MyColumnConfig().setParent(columnsCtk);

							Rows rowsCtk = new Rows();
							rowsCtk.setParent(gridCtk);

							MyFormRow rowCtk = new MyFormRow();
							rowCtk.setValign("top");
							rowCtk.setParent(rowsCtk);
							ais.ui.util.ZkCompat.setSpans(rowCtk, "2");
							CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(rowCtk);

							rowCtk = new MyFormRow();
							rowCtk.setParent(rowsCtk);
							rowCtk.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Registrasi")));
							new Label(biodataCalonMahasiswa.getNoRegistrasi()).setParent(rowCtk);

							rowCtk = new MyFormRow();
							rowCtk.setParent(rowsCtk);
							rowCtk.appendChild(new MyLabelConfig("No. Ujian"));
							new Label(biodataCalonMahasiswa.getNoUjian() == null ? ""
									: biodataCalonMahasiswa.getNoUjian()).setParent(rowCtk);

							rowCtk = new MyFormRow();
							rowCtk.setParent(rowsCtk);
							rowCtk.appendChild(new MyLabelConfig("Nama"));
							new Label(biodataCalonMahasiswa.getNama().toUpperCase()).setParent(rowCtk);

							rowCtk = new MyFormRow();
							rowCtk.setParent(rowsCtk);
							rowCtk.appendChild(new MyLabelConfig("Pembayaran Registrasi"));

							Kegiatan tempKegiatan = fKegiatan;
							if (tempKegiatan == null || tempKegiatan.getId() == null) {
								JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil
										.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
								tempKegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);

								if (tempKegiatan == null || tempKegiatan.getId() == null) {
									tempKegiatan = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
											biodataCalonMahasiswa, 0, biodataCalonMahasiswa.getTahunAkademik(), true,
											false, null, sessionAction);
								}
								if (tempKegiatan != null) {
									try {
										Transaction tx = sessionAction.beginTransaction();
										biodataCalonMahasiswa.setPembayaranRegistrasi(tempKegiatan);
										sessionAction.update(biodataCalonMahasiswa);
										tx.commit();
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
							}

							if (tempKegiatan != null
									&& ((int) (tempKegiatan.getAmount() + tempKegiatan.getAmountTerhutang())) == 0) {
								rowCtk.appendChild(new Label("Belum/tidak ada tagihan registrasi"));
							} else if (tempKegiatan == null || tempKegiatan.getPersentaseLunas() < 0.01) {
								rowCtk.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum melakukan pembayaran")));
							} else {
								final Kegiatan finalKeg = tempKegiatan;
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
										"Cetak Bukti Pembayaran Registrasi", "/img/svg/printer.svg");
								button.setTooltiptext("Cetak");
								button.setOrient("vertical");
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(finalKeg, false);
									}
								});
								button.setParent(rowCtk);
							}

							rowCtk = new MyFormRow();
							rowCtk.setParent(rowsCtk);
							rowCtk.appendChild(new MyLabelConfig("Pembayaran Daftar Ulang"));

							Kegiatan tempKegDaftarUlang = fKegiatanDaftarUlang;
							if (tempKegDaftarUlang == null) {
								JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil
										.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
								tempKegDaftarUlang = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);

								if (tempKegDaftarUlang == null || tempKegDaftarUlang.getId() == null) {
									tempKegDaftarUlang = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
											biodataCalonMahasiswa, 1, biodataCalonMahasiswa.getTahunAkademik(), true,
											false, null, sessionAction);
								}

								if (tempKegDaftarUlang != null) {
									try {
										Transaction tx = sessionAction.beginTransaction();
										biodataCalonMahasiswa.setPembayaranDaftarUlang(tempKegDaftarUlang);
										sessionAction.update(biodataCalonMahasiswa);
										tx.commit();
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
							}

							if (biodataCalonMahasiswa.getProdiLulus() == null) {
								rowCtk.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum dinyatakan diterima")));
							} else if (tempKegDaftarUlang != null && ((int) (tempKegDaftarUlang.getAmount()
									+ tempKegDaftarUlang.getAmountTerhutang())) == 0) {
								rowCtk.appendChild(new Label("Belum/tidak ada tagihan daftar ulang"));
							} else if (tempKegDaftarUlang == null || tempKegDaftarUlang.getPersentaseLunas() < 0.01) {
								String infoTghn = (tempKegDaftarUlang != null && (tempKegDaftarUlang.getAmount()
										+ tempKegDaftarUlang.getAmountTerhutang()) < 0.01)
												? "Tidak ada tagihan"
												: "Belum Bayar " + (tempKegDaftarUlang == null ? ""
														: Common.numberFormat.get().format(tempKegDaftarUlang.getAmount()
																+ tempKegDaftarUlang.getAmountTerhutang()));
								rowCtk.appendChild(new Label(infoTghn));
							} else {
								final Kegiatan finalKegDaftarUlang = tempKegDaftarUlang;
								Vbox vboxC = new Vbox();
								vboxC.setParent(rowCtk);
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
										"Cetak Bukti Pembayaran Daftar Ulang", "/img/svg/printer.svg");
								button.setTooltiptext("Cetak");
								button.setOrient("vertical");
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(finalKegDaftarUlang,
												false);
									}
								});
								button.setParent(vboxC);

								String infTxt = tempKegDaftarUlang.getPersentaseLunas().intValue() == 100
										? "Lunas " + Common.numberFormat.get().format(tempKegDaftarUlang.getAmount())
										: "Bayar " + Common.numberFormat.get().format(tempKegDaftarUlang.getAmount())
												+ " dari tagihan "
												+ Common.numberFormat.get().format(tempKegDaftarUlang.getAmount()
														+ tempKegDaftarUlang.getAmountTerhutang())
												+ " atau "
												+ Common.numberFormat.get().format(tempKegDaftarUlang.getPersentaseLunas())
												+ "%";
								vboxC.appendChild(new Label(infTxt));
							}

							addWindow.setHeight("450px");
							addWindow.setWidth("500px");

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);
							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									addWindow.detach();
								}
							});
							cancel.setParent(toolbar);
							addWindow.onModal();

						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				MyToolbarbutton ketDiterima = new MyToolbarbutton("fa-check-square-o", "Bukti Diterima");
				ketDiterima
						.setVisible(biodataCalonMahasiswa.getProdiLulus() != null && tampilkanInformasiBuktiDiterima);
				ketDiterima.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Session sessionAction = null;
						try {
							sessionAction = HibernateUtil.getSessionFactory().openSession();
							sessionAction.refresh(biodataCalonMahasiswa);

							if (biodataCalonMahasiswa.getGelombangPendaftaran()
									.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()
									&& !BiodataCalonMahasiswaAction.lengkap(biodataCalonMahasiswa)) {
								MyMessageboxConfig.showFormat(
									"Mohon maaf, Anda belum dapat melanjutkan proses ini karena data dan berkas Anda belum lengkap. Mohon lengkapi terlebih dahulu data diri Anda melalui menu \"{V1}\". Langkah yang dapat dilakukan: (1) buka menu \"{V1}\"; (2) lengkapi seluruh data dan berkas yang masih kosong; (3) simpan perubahan, kemudian ulangi kembali proses ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, loginCalonMhs.getLabelC().getValue());
								return;
							}
							CommonReportHelper.onCetakSuratKeteranganLulus(biodataCalonMahasiswa, false);
						} finally {
							if (sessionAction != null && sessionAction.isOpen()) {
								try {
									sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				});

				MyToolbarbutton cetakKtm = new MyToolbarbutton("fa-id-card", "Cetak E-KTM");
				cetakKtm.setVisible(biodataCalonMahasiswa.getMahasiswa() != null);
				cetakKtm.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						LaporanKartuMahasiswa kartuMahasiswa = new LaporanKartuMahasiswa(
								biodataCalonMahasiswa.getMahasiswa());
						kartuMahasiswa.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						kartuMahasiswa.setBorder(false);
						kartuMahasiswa.setBorder("none");
						kartuMahasiswa.setClosable(true);
						kartuMahasiswa.setTitle("Kartu Tanda Mahasiswa");
						kartuMahasiswa.setHeight("100%");
						kartuMahasiswa.setWidth("700px");
						kartuMahasiswa.onModal();
					}
				});

				List<MyToolbarbutton> awesomes = new ArrayList<MyToolbarbutton>();
				if (alur.isVisible())
					awesomes.add(alur);
				if (bayarRegistrasi.isVisible())
					awesomes.add(bayarRegistrasi);
				if (bayarDaftarUlang.isVisible())
					awesomes.add(bayarDaftarUlang);
				if (uploadRegistrasi.isVisible())
					awesomes.add(uploadRegistrasi);
				if (cetakRegistrasi.isVisible())
					awesomes.add(cetakRegistrasi);
				if (loginCalonMhs.isVisible())
					awesomes.add(loginCalonMhs);
				if (tampilkanInformasiKelulusan && informasiKelulusan.isVisible())
					awesomes.add(informasiKelulusan);
				if (ujian.isVisible())
					awesomes.add(ujian);
				if (uploadInterview.isVisible())
					awesomes.add(uploadInterview);
				if (reg.isVisible())
					awesomes.add(reg);
				if (bio.isVisible())
					awesomes.add(bio);
				if (kartuUjian.isVisible())
					awesomes.add(kartuUjian);
				if (ketDiterima.isVisible())
					awesomes.add(ketDiterima);
				if (cetakKtm.isVisible())
					awesomes.add(cetakKtm);
				if (logout.isVisible())
					awesomes.add(logout);

				if (mobile) {
					// Mobile: tombol vertikal bertumpuk
					Vbox vboxMobile = new Vbox();
					vboxMobile.setStyle("width:100%;padding:4px;");
					vboxMobile.setParent(groupboxStyled1);
					for (MyToolbarbutton awesome : awesomes) {
						new Space().setParent(vboxMobile);
						vboxMobile.appendChild(awesome);
					}
				} else {
					// Desktop: tombol rapat di tengah dengan flex wrap
					Div btnDiv = new Div();
					btnDiv.setStyle("display:flex;flex-wrap:wrap;justify-content:center;"
						+ "align-items:center;gap:8px 14px;padding:8px 12px;");
					btnDiv.setParent(groupboxStyled1);
					for (MyToolbarbutton awesome : awesomes) {
						btnDiv.appendChild(awesome);
					}
				}

				int lain = 0;
				component.setHeight(mobile ? "" + (1000 + lain) + "px" : "480px");

			} else {
				// =========================================================================
				// BAGIAN B: TAMPILAN DAFTAR GELOMBANG (BELUM LOGIN/PENGUNJUNG UMUM)
				// =========================================================================

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setOddRowSclass("non-odd");
				grid.setStyle("border:0px;background: transparent;");

				Rows rows = new Rows();
				rows.setParent(grid);

				int lain = 0;
				TreeSet<GelombangPendaftaran> gelombangPendaftarans = new TreeSet<GelombangPendaftaran>();
				List<KelompokGelombang> kelompokGelombangs = new ArrayList<KelompokGelombang>();
				Map<Long, JenisSeleksi> jenisSeleksisMap = new HashMap<Long, JenisSeleksi>();
				List<JenisSeleksi> jenisSeleksis;

				if (gelombangPendaftaranData == null || gelombangPendaftaranData.getId() == null) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					Box hboxCari = mobile ? new Vbox() : new Hbox();
					hboxCari.setWidth("95%");
					hboxCari.setPack("center");
					hboxCari.setAlign("center");
					hboxCari.setParent(row);

					final Combobox comboboxTa = new Combobox();

					if (Common.bolehKonfigurasi("tampilkan_pilihan_tahun_akademik_pmb")) {
						hboxCari.appendChild(new MyLabelConfig("Tahun Akademik"));
					}

					Map<Long, GeneralValueObject> gelombangsAktif = ConstantValues
							.ambilBerdasarClass(GelombangPendaftaran.class);
					TreeSet<String> tas = new TreeSet<String>();
					Date sekarang = WaktuUtil.getDate();
					List<GelombangPendaftaran> gelombangPendaftaransData = new ArrayList<GelombangPendaftaran>();

					for (Long gelId : gelombangsAktif.keySet()) {
						GelombangPendaftaran gelombangPendaftaran1 = (GelombangPendaftaran) ConstantValues
								.ambil(GelombangPendaftaran.class.getName(), gelId);
						if (gelombangPendaftaran1 != null && gelombangPendaftaran1.getBisaDipilihPendaftarOnline()
								&& gelombangPendaftaran1.getAktif()
								&& (gelombangPendaftaran1.getMulai().before(sekarang)
										|| Common.dateFormat8.get().format(gelombangPendaftaran1.getMulai())
												.equals(Common.dateFormat8.get().format(sekarang)))
								&& (gelombangPendaftaran1.getSampai().after(sekarang)
										|| Common.dateFormat8.get().format(gelombangPendaftaran1.getSampai())
												.equals(Common.dateFormat8.get().format(sekarang)))) {

							tas.add(gelombangPendaftaran1.getTahunAkademik());

							if (gelombangPendaftaran1.getKelompokGelombang() != null
									&& !gelombangPendaftaran1.getKelompokGelombang().gelombangPendaftarans
											.contains(gelombangPendaftaran1)) {
								gelombangPendaftaran1.getKelompokGelombang().gelombangPendaftarans
										.add(gelombangPendaftaran1);
							}

							if (gelombangPendaftaran1.getKelompokGelombang() != null) {
								if (!kelompokGelombangs.contains(gelombangPendaftaran1.getKelompokGelombang())) {
									kelompokGelombangs.add(gelombangPendaftaran1.getKelompokGelombang());
								}
							} else {
								gelombangPendaftarans.add(gelombangPendaftaran1);
								gelombangPendaftaransData.add(gelombangPendaftaran1);

								if (gelombangPendaftaran1.getJenisSeleksi() != null) {
									jenisSeleksisMap.put(gelombangPendaftaran1.getJenisSeleksi().getId(),
											gelombangPendaftaran1.getJenisSeleksi());
								}

								for (JenisSeleksi jenisSeleksi : gelombangPendaftaran1.ambilJenisSeleksi()) {
									jenisSeleksisMap.put(jenisSeleksi.getId(), jenisSeleksi);
								}
							}
						}
					}

					Collections.sort(gelombangPendaftaransData);
					jenisSeleksis = new ArrayList<JenisSeleksi>(jenisSeleksisMap.values());
					Collections.sort(kelompokGelombangs);
					Collections.sort(jenisSeleksis);

					jenisSeleksis.add(null);
					if (Common.bolehKonfigurasi("tampilkan_pilihan_tahun_akademik_pmb")) {
						for (String taa : tas) {
							Comboitem comboitem = new Comboitem(taa);
							comboitem.setValue(taa);
							comboboxTa.appendChild(comboitem);
						}

						if (Common.bolehKonfigurasi("tampilkan_pilihan_tahun_akademik_semua_pmb")) {
							Comboitem comboitem = new Comboitem("Semua");
							comboitem.setValue("-");
							comboboxTa.appendChild(comboitem);
							if (ta == null || ta.trim().equalsIgnoreCase("-")) {
								comboboxTa.setSelectedItem(comboitem);
							}
						}

						hboxCari.appendChild(comboboxTa);
						if (ta != null && !ta.trim().equalsIgnoreCase("-")) {
							Common.selectComboItem(true, comboboxTa, ta);
						}
						comboboxTa.setCols(6);
						comboboxTa.setReadonly(true);
					}

					gelombangPendaftaransData.add(null);
					final Combobox comboboxGelombang = new Combobox();

					if (Common.bolehKonfigurasi("tampilkan_pilihan_gelombang_pmb") && !gelombangPendaftarans.isEmpty()) {
						hboxCari.appendChild(new MyLabelConfig("Gelombang"));
						hboxCari.appendChild(comboboxGelombang);
						comboboxGelombang.setCols(8);
						Common.insertComboItems(comboboxGelombang, "nama", gelombangPendaftaransData);
						Common.selectComboItem(true, comboboxGelombang, gelombangPendaftaran);
						comboboxGelombang.setReadonly(true);
					}

					final Combobox comboboxJenisSeleksi = new Combobox();
					if (Common.bolehKonfigurasi("tampilkan_pilihan_jenis_seleksi_pmb") && !jenisSeleksisMap.isEmpty()) {
						hboxCari.appendChild(new MyLabelConfig("Seleksi"));
						hboxCari.appendChild(comboboxJenisSeleksi);
						comboboxJenisSeleksi.setCols(8);
						Common.insertComboItems(comboboxJenisSeleksi, "nama", jenisSeleksis);
						Common.selectComboItem(true, comboboxJenisSeleksi, jenisSeleksia);
						comboboxJenisSeleksi.setReadonly(true);
					}

					EventListener eventListener2 = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String t = (String) (comboboxTa.getSelectedItem() == null ? null
									: comboboxTa.getSelectedItem().getValue());
							GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) (comboboxGelombang
									.getSelectedItem() == null ? null : comboboxGelombang.getSelectedItem().getValue());
							JenisSeleksi comboboxJenisSeleksia = (JenisSeleksi) (comboboxJenisSeleksi
									.getSelectedItem() == null ? null
											: comboboxJenisSeleksi.getSelectedItem().getValue());

							Sessions.getCurrent(true).setAttribute("t", t);
							Sessions.getCurrent(true).setAttribute("gelombangPendaftaran", gelombangPendaftaran);
							Sessions.getCurrent(true).setAttribute("jenisSeleksia", comboboxJenisSeleksia);

							Executions.getCurrent().sendRedirect("");
						}
					};

					comboboxGelombang.addEventListener("onChange", eventListener2);
					comboboxTa.addEventListener("onChange", eventListener2);
					comboboxJenisSeleksi.addEventListener("onChange", eventListener2);

				} else {
					gelombangPendaftarans.add(gelombangPendaftaranData);

					if (gelombangPendaftaranData.getJenisSeleksi() != null) {
						jenisSeleksisMap.put(gelombangPendaftaranData.getJenisSeleksi().getId(),
								gelombangPendaftaranData.getJenisSeleksi());
					}
					for (JenisSeleksi jenisSeleksi : gelombangPendaftaranData.ambilJenisSeleksi()) {
						jenisSeleksisMap.put(jenisSeleksi.getId(), jenisSeleksi);
					}

					jenisSeleksis = new ArrayList<JenisSeleksi>(jenisSeleksisMap.values());
					Collections.sort(jenisSeleksis);
				}

				MyFormRow row = new MyFormRow();
				row.setParent(rows);

				Box hbox = mobile ? new Vbox() : new Hbox();
				hbox.setPack("center");
				hbox.setAlign("center");
				hbox.setParent(row);
				hbox.setWidth("100%");
				int jml = 0;

				grid.setParent(component);

				if (!kelompokGelombangs.isEmpty()) {
					for (final KelompokGelombang kelompokGelombang : kelompokGelombangs) {
						if (!mobile) {
							if (jml > 0 && jml % 3 == 0) {
								row = new MyFormRow();
								row.setParent(rows);
								hbox = new Hbox();
								hbox.setPack("center");
								hbox.setAlign("center");
								hbox.setWidth("100%");
								hbox.setParent(row);
							}
						}

						jml++;

						Date mulaiKelompok = null;
						Date sampaiKelompok = null;
						Collections.sort(kelompokGelombang.gelombangPendaftarans);

						for (GelombangPendaftaran gelombangPendaftaranKelompok : kelompokGelombang.gelombangPendaftarans) {
							if (gelombangPendaftaranKelompok.getMulai() != null && (mulaiKelompok == null
									|| mulaiKelompok.after(gelombangPendaftaranKelompok.getMulai()))) {
								mulaiKelompok = gelombangPendaftaranKelompok.getMulai();
							}
							if (gelombangPendaftaranKelompok.getSampai() != null && (sampaiKelompok == null
									|| sampaiKelompok.before(gelombangPendaftaranKelompok.getSampai()))) {
								sampaiKelompok = gelombangPendaftaranKelompok.getSampai();
							}
						}

						Groupbox groupboxStyled = new Groupbox();
						groupboxStyled.setStyle(
								"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");

						hbox.appendChild(groupboxStyled);
						groupboxStyled.appendChild(new MyCaptionStyled(kelompokGelombang.getNama()));

						LampiranLain lampiranLain = LampiranLain.ambil(kelompokGelombang.getId(), "INFO_KELOMPOK_PPDB");
						String linkInfo = null;
						if (lampiranLain != null && lampiranLain.getId() != null) {
							try {
								linkInfo = lampiranLain.createLinkUri();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}

						StringBuilder htmlKel = new StringBuilder();
						htmlKel.append(
								"<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
								.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Pendaftaran"))
								.append("</td><td>")
								.append(mulaiKelompok == null ? "" : Common.dateFormat1.get().format(mulaiKelompok))
								.append(" sd ")
								.append(sampaiKelompok == null ? "" : Common.dateFormat1.get().format(sampaiKelompok))
								.append("</td></tr>").append("<tr valign=\"top\"><td>")
								.append(Common.getBahasaConfig("Info")).append("</td><td>")
								.append(kelompokGelombang.getInfo())
								.append(linkInfo == null || linkInfo.trim().isEmpty() ? ""
										: "<a href='#' onClick=\"popupCenter({url: '" + linkInfo
												+ "', title: 'PPDB Info', w: 1200, h: 600});\">"
												+ (kelompokGelombang.getInfo().isEmpty() ? "" : ", ")
												+ "informasi lebih lanjut klik ini..</a>")
								.append("</td></tr>");

						if (!kelompokGelombang.getKeterangan().isEmpty()) {
							htmlKel.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Keterangan"))
									.append("</td><td>").append(kelompokGelombang.getKeterangan()).append("</td></tr>");
						}
						htmlKel.append("</table>");

						groupboxStyled.appendChild(new Html(htmlKel.toString()));

						Hbox hbox1 = new Hbox();
						groupboxStyled.appendChild(hbox1);

						MyToolbarbutton formulir = new MyToolbarbutton("fa-pencil-square-o",
								Common.getBahasaConfig("Daftar Sekarang"));
						formulir.getLabelC().setStyle("font-size:15px");
						hbox1.appendChild(formulir);
						formulir.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								sebagaiKelompokPmb(kelompokGelombang);
							}
						});
					}
				}

				if (!gelombangPendaftarans.isEmpty()) {
					for (final GelombangPendaftaran myGelombangPendaftaran : gelombangPendaftarans) {
						if (ta == null || ta.trim().isEmpty() || ta.trim().equalsIgnoreCase("-")
								|| (myGelombangPendaftaran.getTahunAkademik() != null && ta != null
										&& myGelombangPendaftaran.getTahunAkademik().equalsIgnoreCase(ta))) {

							if (myGelombangPendaftaran.getJenisSeleksiDipilihDiFormPendaftaran()) {
								if (!mobile) {
									if (jml > 0 && jml % 3 == 0) {
										row = new MyFormRow();
										row.setParent(rows);
										hbox = new Hbox();
										hbox.setPack("center");
										hbox.setAlign("center");
										hbox.setWidth("100%");
										hbox.setParent(row);
									}
								}
								jml++;

								LampiranLain lampiranLain = LampiranLain.ambil(myGelombangPendaftaran.getId(),
										"INFO_PMB");
								String linkInfo = null;
								if (lampiranLain != null && lampiranLain.getId() != null) {
									try {
										linkInfo = lampiranLain.createLinkUri();
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								}

								Groupbox groupboxStyled = new Groupbox();
								groupboxStyled.setStyle(
										"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 340px;");

								hbox.appendChild(groupboxStyled);
								groupboxStyled.appendChild(new MyCaptionStyled(myGelombangPendaftaran.getNama()));

								StringBuilder bGel = new StringBuilder();
								bGel.append(
										"<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">");

								if (myGelombangPendaftaran.getTampilkanMasaPendaftaranKeCalonMahasiswa()) {
									bGel.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Pendaftaran"))
											.append("</td><td>")
											.append(Common.dateFormat1.get().format(myGelombangPendaftaran.getMulai()))
											.append(" sd ")
											.append(Common.dateFormat1.get().format(myGelombangPendaftaran.getSampai()))
											.append("</td></tr>");
								}

								bGel.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Periode"))
										.append("</td><td>").append(myGelombangPendaftaran.getTahunAkademik())
										.append(" / ").append(myGelombangPendaftaran.getJenisSemester())
										.append("</td></tr>");

								if (!myGelombangPendaftaran.getInfo().isEmpty()) {
									bGel.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Informasi"))
											.append("</td><td>").append(myGelombangPendaftaran.getInfo())
											.append(linkInfo == null || linkInfo.trim().isEmpty() ? ""
													: "<a href='#' onClick=\"popupCenter({url: '" + linkInfo
															+ "', title: 'PPDB Info', w: 1200, h: 600});\">"
															+ (myGelombangPendaftaran.getInfo().isEmpty() ? "" : ", ")
															+ "informasi lebih lanjut klik ini..</a>")
											.append("</td></tr>");
								}
								bGel.append("</table>");
								groupboxStyled.appendChild(new Html(bGel.toString()));

								MyToolbarbutton formulir = new MyToolbarbutton("fa-pencil-square-o",
										Common.getBahasaConfig("Daftar Sekarang"));
								formulir.getLabelC().setStyle("font-size:15px");
								groupboxStyled.appendChild(formulir);
								formulir.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										if (myGelombangPendaftaran.getHarusSebagaiAlumni()) {
											sebagaiALumni(myGelombangPendaftaran, null, afiliasiCalonMahasiswaData);
										} else {
											ais.action.master.pmb.BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new ais.action.master.pmb.BiodataCalonMahasiswaAction(
													myGelombangPendaftaran, null, afiliasiCalonMahasiswaData, null,
													new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
														}
													});
											ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
													.appendChild(biodataCalonMahasiswaAction);
											biodataCalonMahasiswaAction.setWidth("900px");
											biodataCalonMahasiswaAction.setHeight("100%");
											biodataCalonMahasiswaAction.onModal();
										}
									}
								});

							} else {
								jenisSeleksis = new ArrayList<JenisSeleksi>();
								if (myGelombangPendaftaran != null && (gelombangPendaftaran == null
										|| (gelombangPendaftaran != null && gelombangPendaftaran.getId()
												.equals(myGelombangPendaftaran.getId())))) {
									if (jenisSeleksia == null || (myGelombangPendaftaran.getJenisSeleksi() != null
											&& jenisSeleksia != null && myGelombangPendaftaran.getJenisSeleksi().getId()
													.equals(jenisSeleksia.getId()))) {
										jenisSeleksis.add(myGelombangPendaftaran.getJenisSeleksi());
									}
									for (String kode : StringUtils.split(myGelombangPendaftaran.getJenisSeleksiLain(),
											",")) {
										if (!kode.trim().isEmpty()) {
											JenisSeleksi jenisSeleksi = (JenisSeleksi) ConstantValues.simpleObject(
													session.createCriteria(JenisSeleksi.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1),
													JenisSeleksi.class);
											if (jenisSeleksi != null) {
												if (jenisSeleksia == null || (jenisSeleksi != null
														&& jenisSeleksia != null
														&& jenisSeleksi.getId().equals(jenisSeleksia.getId()))) {
													jenisSeleksis.add(jenisSeleksi);
												}
											}
										}
									}
								}

								Collections.sort(jenisSeleksis);

								for (final JenisSeleksi jenisSeleksi : jenisSeleksis) {
									if (jenisSeleksi != null) {
										if (!mobile) {
											if (jml > 0 && jml % 3 == 0) {
												row = new MyFormRow();
												row.setParent(rows);
												hbox = new Hbox();
												hbox.setPack("center");
												hbox.setAlign("center");
												hbox.setWidth("100%");
												hbox.setParent(row);
											}
										}
										jml++;

										Groupbox groupboxStyled = new Groupbox();
										groupboxStyled.setStyle(
												"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");

										hbox.appendChild(groupboxStyled);
										groupboxStyled
												.appendChild(new MyCaptionStyled(myGelombangPendaftaran.getNama()));

										StringBuilder bJS = new StringBuilder();
										bJS.append(
												"<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
												.append("<tr valign=\"top\"><td>")
												.append(Common.getBahasaConfig("Pendaftaran")).append("</td><td>")
												.append(Common.dateFormat1.get().format(myGelombangPendaftaran.getMulai()))
												.append(" sd ")
												.append(Common.dateFormat1.get().format(myGelombangPendaftaran.getSampai()))
												.append("</td></tr>").append("<tr valign=\"top\"><td>")
												.append(Common.getBahasaConfig("Jenis Seleksi")).append("</td><td>")
												.append(jenisSeleksi == null ? "" : jenisSeleksi.getNama())
												.append("</td></tr>").append("<tr valign=\"top\"><td>")
												.append(Common.getBahasaConfig("Periode")).append("</td><td>")
												.append(myGelombangPendaftaran.getTahunAkademik()).append(" / ")
												.append(myGelombangPendaftaran.getJenisSemester())
												.append("</td></tr></table>");

										groupboxStyled.appendChild(new Html(bJS.toString()));

										MyToolbarbutton formulir = new MyToolbarbutton("fa-pencil-square-o",
												Common.getBahasaConfig("Daftar Sekarang"));
										formulir.getLabelC().setStyle("font-size:15px");
										groupboxStyled.appendChild(formulir);
										formulir.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												if (myGelombangPendaftaran.getHarusSebagaiAlumni()) {
													sebagaiALumni(myGelombangPendaftaran, jenisSeleksi,
															afiliasiCalonMahasiswaData);
												} else {
													ais.action.master.pmb.BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new ais.action.master.pmb.BiodataCalonMahasiswaAction(
															myGelombangPendaftaran, jenisSeleksi,
															afiliasiCalonMahasiswaData, null, new EventListener() {
																@Override
																public void onEvent(Event arg0) throws Exception {
																}
															});
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
															.appendChild(biodataCalonMahasiswaAction);
													biodataCalonMahasiswaAction.setWidth("900px");
													biodataCalonMahasiswaAction.setHeight("100%");
													biodataCalonMahasiswaAction.onModal();
												}
											}
										});
									}
								}
							}
						}
					}
				}

				component.setHeight(
						mobile ? (((jml + 1) * 160) + 160 + lain) + "px" : ((((jml / 3) + 1) * 170) + 50) + "px");

				if (!mobile) {
					if (jml > 0 && jml % 3 == 0) {
						row = new MyFormRow();
						row.setParent(rows);
						hbox = new Hbox();
						hbox.setPack("center");
						hbox.setAlign("center");
						hbox.setWidth("100%");
						hbox.setParent(row);
					}
				}
				jml++;

				if (Common.bolehKonfigurasi("tampilkan_login_pmb")) {
					Groupbox groupboxStyled = new Groupbox();
					groupboxStyled.setStyle(
							"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");

					String bodyLogin = Common.getKonfigurasi("header_login_pmb", "Login").getNilai();
					String bodyLoginTombol = Common.getKonfigurasi("tombol_login_pmb", "Login Sekarang").getNilai();

					hbox.appendChild(groupboxStyled);
					groupboxStyled.appendChild(new MyCaptionStyled(Common.getBahasaConfig(bodyLogin)));

					String bodyTxt = "<table style=\"width:100%;border:0px solid black;padding: 5px 20px 10px 5px;\">"
							+ "<tr valign=\"top\"><td>"
							+ Common.getBahasaConfig(
									"Jika Anda telah melakukan pendaftaran,<br>pilih login untuk melengkapi data,<br> informasi ujian dan pembayaran,<br>serta informasi kelulusan.")
							+ "</td></tr></table>";

					if (Common.currentLang().equals(Tbmuser.INDONESIA)) {
						bodyTxt = Common.getKonfigurasi("body_login_pmb", bodyTxt).getNilai();
					}
					groupboxStyled.appendChild(new Html(bodyTxt));

					MyToolbarbutton formulir = new MyToolbarbutton("fa-sign-in",
							Common.getBahasaConfig(bodyLoginTombol));
					formulir.getLabelC().setStyle("font-size:15px");
					groupboxStyled.appendChild(formulir);
					formulir.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.displayWindow("/pages/pmb/login_calon_mahasiswa.zul", true, "500px",
									Common.isMobile() ? "100%" : "850px", null, "", false);
						}
					});
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

	@SuppressWarnings({})
	public static Rows tampil(Component component, PengumumanAkademis pengumumanAkademis, Sekolah sekolah,
			Tbmuser tbmuser, PerguruanTinggi selectedPerguruanTinggi, final EventListener eventListener,
			boolean tampilPengumumanLain, boolean awal, EventListener eventListenerSub) {

		try {
			Grid grids = new Grid();
			grids.setMold("paging");
			grids.setParent(component);
			grids.setSclass("fgrid");
			grids.setWidth("100%");

			Columns columns = new Columns();
			columns.setParent(grids);
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grids);

			return tampil(rows, pengumumanAkademis, tbmuser, sekolah, selectedPerguruanTinggi, eventListener,
					tampilPengumumanLain, awal, eventListenerSub);
		} catch (Exception e) {
			return new Rows();
		}
	}

	public static Rows tampil(Rows rows, PengumumanAkademis pengumumanAkademis, Tbmuser tbmuser, Sekolah sekolah,
			PerguruanTinggi selectedPerguruanTinggi, EventListener eventListener, boolean tampilPengumumanLain,
			boolean awal, EventListener eventListenerSub) {

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		try {
			PengumumanAkademisAction.tampilPengumuman(rows, pengumumanAkademis, sekolah, tbmuser,
					selectedPerguruanTinggi, tampilPengumumanLain, awal, eventListenerSub);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		Vbox vbox3 = new Vbox();
		vbox3.setParent(row);
		DiskusiPengumumanAkademisHelper data = new DiskusiPengumumanAkademisHelper(vbox3, pengumumanAkademis);

		Common.createDefaultTimer(data);

		if (pengumumanAkademis.getAdaVideoConference() || pengumumanAkademis.getAdaVideoConferenceGoogleMeet()) {
			try {
				Common.createVideoConrefrence(pengumumanAkademis, vbox3, false, false, eventListener);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			Common.tampilOnline(pengumumanAkademis, vbox3);
		}

		row = new MyFormRow();
		row.setParent(rows);

		PengumumanAkademisAction.tampilkanPolling(pengumumanAkademis, row);

		return rows;
	}

	public static Criteria initCriteriaStatic(boolean order, Tbmuser tbmuser, PerguruanTinggi selectedPerguruanTinggi,
			Long idPengumuman, Session session) {

		if (!isSessionUsable(session)) {
			throw new IllegalStateException(
					"Session Hibernate untuk initCriteriaStatic sudah null atau tertutup. "
							+ "Buat session baru dengan openSession() sebelum memanggil method ini dan tutup di finally.");
		}

		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
		BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();

		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		Yayasan selectedYayasan = SekolahUtil.getYayasan();

		if (siswa != null) {
			selectedSekolah = siswa.getSekolah();
			selectedYayasan = siswa.getYayasan();
		}
		if (dosen != null && dosen.getId() != null) {
			selectedSekolah = null;
			selectedYayasan = null;
		}
		if (mahasiswa != null && mahasiswa.getId() != null) {
			selectedSekolah = null;
			selectedYayasan = null;
		}
		// SAMAKAN DENGAN ALGORITMA CODE LAMA (C:\opt\AIS - BARU): code lama TIDAK memaksa
		// selectedPerguruanTinggi = tbmuser.getPerguruanTinggi(). Override itu membuat filter
		// perguruanTinggi & fakultas.perguruanTinggi memakai PT milik user, yang bisa BERBEDA dari PT
		// konteks yang dikirim pemanggil (PerguruanTinggiUtil.getPerguruanTinggi bisa mengembalikan PT
		// domain/default). Akibatnya pengumuman ber-fakultas yang PT-nya tak sama dengan PT user TIDAK
		// muncul di papan Home (terlihat seperti pengumuman baru "hilang"). Kembali ke perilaku lama:
		// pakai selectedPerguruanTinggi apa adanya dari pemanggil. Penetralan sekolah/yayasan tetap
		// (khusus konteks PT), tapi TANPA menimpa selectedPerguruanTinggi.
		if (tbmuser != null && tbmuser.getPerguruanTinggi() != null) {
			selectedSekolah = null;
			selectedYayasan = null;
		}

		Criterion r = Restrictions.or(Restrictions.isNull("diperuntukkan"),
				Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_UMUM));

		if (mahasiswa != null) {
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(mahasiswa).getStatusMahasiswa();
			if (statusMahasiswa != null && ConstantValues.LULUS != null && statusMahasiswa.getId() != null
					&& ConstantValues.LULUS.getId() != null
					&& statusMahasiswa.getId().equals(ConstantValues.LULUS.getId())) {
				r = Restrictions.or(r, Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_ALUMNI));
			} else {
				r = Restrictions.or(r, Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_MAHASISWA));
			}

			Criterion tambahan = Restrictions.or(Restrictions.isNull("hanyaUntuk"),
					Restrictions.ilike("hanyaUntuk", "," + mahasiswa.getNim() + ",", MatchMode.ANYWHERE));
			tambahan = Restrictions.or(tambahan, Restrictions.eq("hanyaUntuk", ""));
			r = Restrictions.and(r, tambahan);

			tambahan = Restrictions.or(Restrictions.isNull("hanyaUntukAngkatan"), Restrictions
					.ilike("hanyaUntukAngkatan", "," + mahasiswa.getTahunangkatan() + ",", MatchMode.ANYWHERE));
			tambahan = Restrictions.or(tambahan, Restrictions.eq("hanyaUntukAngkatan", ""));
			tambahan = Restrictions.or(tambahan, Restrictions.isNull("hanyaUntukAngkatan"));

			tambahan = Restrictions.or(tambahan,
					Restrictions.ilike("hanyaUntukUsername", "," + mahasiswa.getNim() + ",", MatchMode.ANYWHERE));
			r = Restrictions.and(r, tambahan);
		} else if (biodataCalonMahasiswa != null) {
			r = Restrictions.or(r, Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_PESERTA));
			Criterion tambahan = Restrictions.or(Restrictions.isNull("hanyaUntuk"), Restrictions.ilike("hanyaUntuk",
					"," + biodataCalonMahasiswa.getNoRegistrasi() + ",", MatchMode.ANYWHERE));
			tambahan = Restrictions.or(tambahan, Restrictions.eq("hanyaUntuk", ""));
			r = Restrictions.and(r, tambahan);
		} else if (siswa != null) {
			Criterion kondisi1 = Restrictions.or(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_SISWA),
					Restrictions.ilike("hanyaUntukUsername", "," + tbmuser.getUserId() + ",", MatchMode.ANYWHERE));
			r = Restrictions.or(r, kondisi1);
		} else if (dosen != null) {
			Criterion kondisi1 = Restrictions.or(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_DOSEN),
					Restrictions.ilike("hanyaUntukUsername", "," + tbmuser.getUserId() + ",", MatchMode.ANYWHERE));
			r = Restrictions.or(r, kondisi1);
		} else if (guru != null) {
			Criterion kondisi1 = Restrictions.or(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_GURU),
					Restrictions.ilike("hanyaUntukUsername", "," + tbmuser.getUserId() + ",", MatchMode.ANYWHERE));
			r = Restrictions.or(r, kondisi1);
		} else if (tbmrole != null && tbmrole.getRoleId() != null) {
			/*
			 * Administrator melihat SEMUA pengumuman (termasuk yang diperuntukkan
			 * mahasiswa/dosen/siswa). Tanpa ini, pengumuman "Untuk Mahasiswa" yang
			 * tampil di master PengumumanAkademisAction tidak pernah muncul di
			 * papan pengumuman Home milik admin, sehingga terlihat seperti hilang.
			 * Bisa dimatikan via konfigurasi pengumuman_admin_tampil_semua.
			 */
			boolean adminLihatSemua = false;
			try {
				adminLihatSemua = Common.getApakahAdminLain(tbmuser)
						&& Common.bolehKonfigurasi("pengumuman_admin_tampil_semua");
			} catch (Exception e) {
				adminLihatSemua = false;
			}
			if (adminLihatSemua) {
				r = Restrictions.sqlRestriction("true");
			} else {
				r = Restrictions.or(r, Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_ADMIN));
				Criterion kondisi1 = Restrictions.ilike("hanyaUntuk", "," + tbmrole.getRoleId() + ",",
						MatchMode.ANYWHERE);
				Criterion kondisi2 = Restrictions.ilike("hanyaUntukUsername", "," + tbmuser.getUserId() + ",",
						MatchMode.ANYWHERE);
				Criterion kondisi3 = Restrictions.and(
						Restrictions.or(Restrictions.isNull("hanyaUntuk"), Restrictions.eq("hanyaUntuk", "")),
						Restrictions.or(Restrictions.isNull("hanyaUntukUsername"),
								Restrictions.eq("hanyaUntukUsername", "")));
				Criterion tambahan = Restrictions.or(kondisi1, kondisi2);
				r = Restrictions.and(r, Restrictions.or(tambahan, kondisi3));
			}
		}

		Criteria criteria = session.createCriteria(PengumumanAkademis.class)
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")))
				.add(idPengumuman != null ? Restrictions.idEq(idPengumuman) : Restrictions.sqlRestriction("true"))
				// Aturan user: pengumuman dgn yayasan/sekolah (dan fakultas/jurusan) NULL = UMUM → tampil
				// ke SEMUA. Dulu utk pengguna sekolah (non-PT, konfig off) klausa memaksa eq(yayasan)
				// TANPA mengizinkan NULL → pengumuman ber-yayasan NULL tersembunyi. Kini NULL SELALU
				// lolos: yayasan IS NULL OR yayasan = konteks.
				.add(selectedYayasan == null || selectedYayasan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("yayasan"), Restrictions.eq("yayasan", selectedYayasan)))
				.add(selectedSekolah == null || selectedSekolah.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", selectedSekolah)))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).add(r)
				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
								Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
						Restrictions.or(Restrictions.le("tanggal", ais.ui.util.WaktuUtil.getDate()),
								Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()))));

		if (!(siswa != null || guru != null)) {
			criteria.createAlias("fakultas", "fakultas", Criteria.LEFT_JOIN).add(guru != null || siswa != null
					|| selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("fakultas"),
									Restrictions.or(
											Restrictions.eq("fakultas.perguruanTinggi", selectedPerguruanTinggi),
											Restrictions.isNull("fakultas.perguruanTinggi"))));
		}

		if (mahasiswa != null) {
			criteria.add(
					Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", mahasiswa.getProgram())))
					.add(Restrictions.or(Restrictions.isNull("fakultas"),
							Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas())))
					.add(Restrictions.or(Restrictions.isNull("jurusan"),
							Restrictions.eq("jurusan", mahasiswa.getJurusan())));
		} else if (biodataCalonMahasiswa != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("program"),
					Restrictions.eq("program", biodataCalonMahasiswa.getProgram())))
					.add(biodataCalonMahasiswa.getProdiLulus() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("fakultas"),
									Restrictions.eq("fakultas", biodataCalonMahasiswa.getProdiLulus().getFakultas())))
					.add(biodataCalonMahasiswa.getProdiLulus() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("jurusan"),
									Restrictions.eq("jurusan", biodataCalonMahasiswa.getProdiLulus())));
		} else if (siswa != null) {
			criteria.add(
					Restrictions.or(Restrictions.isNull("yayasan"), Restrictions.eq("yayasan", siswa.getYayasan())))
					.add(Restrictions.or(Restrictions.isNull("sekolah"),
							Restrictions.eq("sekolah", siswa.getSekolah())));
		} else if (tbmuser != null) {
			// FIX (pengumuman "Untuk Umum" tak muncul di papan admin): pengumuman umum sering
			// menyimpan program = "" (STRING KOSONG), BUKAN NULL. Klausa lama hanya mengizinkan
			// isNull("program") -> pengumuman ber-program "" TERSARING KELUAR saat akun admin punya
			// program tertentu (mis. "SE Libur Nasional" Untuk Umum). Perlakukan program kosong SAMA
			// dengan NULL (berlaku untuk semua program) — sama seperti perbaikan filter tagihan UMUM.
			criteria.add(Restrictions.or(
					tbmuser.ambilProgram() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", "")),
					Restrictions.eq("program", tbmuser.ambilProgram() == null ? "" : tbmuser.ambilProgram().getNama())))
					.add(tbmuser.ambilFakultas() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("fakultas"),
									Restrictions.eq("fakultas", tbmuser.ambilFakultas())))
					.add(tbmuser.ambilJurusan() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("jurusan"),
									Restrictions.eq("jurusan", tbmuser.ambilJurusan())));
		}

		if (order) {
			criteria.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
					.addOrder(Order.asc("kategoriPengumuman.nomorUrut")).addOrder(Order.desc("tanggal"));
		}

		return criteria;
	}

	public void onSearchDefault(Event event) {
		pengumumanAkademis = null;
		if (!Common.isMobile() && !sederhana) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<PengumumanAkademis> listPengumumanAkademis = ConstantValues
						.simpleList(initCriteriaStatic(true, tbmuser, selectedPerguruanTinggi, idPengumuman, session)
								.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

				if (!listPengumumanAkademis.isEmpty()) {
					Sekolah sekolah = SekolahUtil.getSekolah();
					pengumumanAkademis = listPengumumanAkademis.get(0);
					tampil(centerPengumuman, pengumumanAkademis, sekolah, tbmuser, selectedPerguruanTinggi, this, false,
							true, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
											.getAttribute("akademis");
									prosess(akademis.getId(), tabspeng, tabpanelspeng, sederhana, cari);
								}
							});
				}
				if (pengumumanAkademis != null && pengumumanAkademis.getTampilkanProfile()) {
					menuPintas.setVisible(false);
				}

				List<PengumumanAkademis> listPengumumanAkademisLangsung = ConstantValues
						.simpleList(initCriteriaStatic(true, tbmuser, selectedPerguruanTinggi, idPengumuman, session)
								.add(Restrictions.eq("langsungMunculDiTab", true)), PengumumanAkademis.class);
				PengumumanAkademisAction.tampilPengumuanLangsungTampil(listPengumumanAkademisLangsung, tabspeng,
						tabpanelspeng);
			} finally {
				if (session != null) {
					try {
						if (session.isOpen())
							session.clear();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try {
						session.disconnect();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					try {
						if (session.isOpen())
							session.close();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			}
		}
	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public static void tampilGelombang(final PerguruanTinggi selectedPerguruanTinggi, final North component,
			final PenyediaAsset penyediaAsset, final boolean mobile) {

		if (penyediaAsset != null) {
			Grid grid = new Grid();
			grid.setSclass("dgrid fgrid");
			grid.setParent(component);
			grid.setOddRowSclass("non-odd");
			grid.setStyle("border:0px;background: transparent;");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setAlign("center");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Groupbox groupboxStyled = new Groupbox();
			groupboxStyled.appendChild(new MyCaptionStyled("Profil Perusahaan"));
			groupboxStyled.setParent(row);
			groupboxStyled.setWidth("97%");
			groupboxStyled.setStyle(
					"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;min-width: 330px;");

			String url = "";

			if (penyediaAsset.getId() != null) {
				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					LampiranLain lampiranLain = (LampiranLain) streamingSession.createCriteria(LampiranLain.class)
							.addOrder(Order.asc("id")).add(Restrictions.eq("ref", penyediaAsset.getId()))
							.setMaxResults(1).add(Restrictions.ilike("jenis", "Galery_PenyediaAsset_", MatchMode.START))
							.uniqueResult();

					url = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/TampilanPengumumanAkademisAction.java:3510");
				} finally {
					if (streamingSession != null) {
						try {
							if (streamingSession.isOpen())
								streamingSession.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							streamingSession.disconnect();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							if (streamingSession.isOpen())
								streamingSession.clear();
								streamingSession.disconnect();
								streamingSession.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				}
			}

			// OPTIMASI: Menggunakan StringBuilder
			StringBuilder htmlBuilder = new StringBuilder();
			htmlBuilder.append("<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
					.append("<tr valign=\"top\"><td valign=\"top\" ")
					.append(mobile ? "colspan=\"2\"" : "rowspan=\"15\"").append("><img style='height: ")
					.append(mobile ? "100px" : "140px")
					.append(";border: 0.5px solid #d9d9d9 !important; border-radius: 15px; aspect-ratio: 1; object-fit: cover; box-shadow: 2px 2px 4px grey;' src=\"")
					.append(url).append("\"/></td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("No. Registrasi")).append("</td><td>")
					.append(penyediaAsset.getKode()).append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Nama")).append("</td><td>").append(penyediaAsset.getNama())
					.append("</td></tr>");

			StringBuilder alamatBuilder = new StringBuilder(
					penyediaAsset.getAlamat() != null ? penyediaAsset.getAlamat() : "");
			if (penyediaAsset.getKecamatan() != null) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Kec.")
						.append(penyediaAsset.getKecamatan().getNama());
			}
			if (penyediaAsset.getKota() != null) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Kab/Kota.")
						.append(penyediaAsset.getKota().getNama());
			}
			if (penyediaAsset.getPropinsi() != null) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Prop.")
						.append(penyediaAsset.getPropinsi().getNama());
			}
			if (penyediaAsset.getKodePos() != null && !penyediaAsset.getKodePos().trim().isEmpty()) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Kode Pos ")
						.append(penyediaAsset.getKodePos());
			}

			htmlBuilder.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Alamat")).append("</td><td>")
					.append(alamatBuilder.toString()).append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Telp.")).append("</td><td>").append(penyediaAsset.getTelp())
					.append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Kontak Person")).append("</td><td>")
					.append(penyediaAsset.getKontak()).append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Email")).append("</td><td>").append(penyediaAsset.getEmail())
					.append("</td></tr>").append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Status"))
					.append("</td><td>")
					.append(penyediaAsset.getStatusPenyediaAsset() == null ? ""
							: penyediaAsset.getStatusPenyediaAsset().getNama())
					.append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Status Aktif")).append("</td><td>")
					.append(penyediaAsset.getAktif() != null && penyediaAsset.getAktif() ? "Ya" : "Tidak")
					.append("</td></tr>").append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Keterangan"))
					.append("</td><td>")
					.append(penyediaAsset.getKeterangan() != null ? penyediaAsset.getKeterangan() : "")
					.append("</td></tr>").append("</table>");

			groupboxStyled.appendChild(new Html(htmlBuilder.toString()));

			row = new MyFormRow();
			row.setParent(rows);

			Groupbox groupboxStyled1 = new Groupbox();
			groupboxStyled1.setParent(row);
			groupboxStyled1.setWidth("97%");
			groupboxStyled1.setStyle(
					"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;width: 320px;");

			MyToolbarbutton logout = new MyToolbarbutton("fa-sign-out", "Logout");
			logout.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.setLogoutPenyediaAsset();
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Executions.getCurrent().sendRedirect("");
						}
					});
				}
			});

			MyToolbarbutton bio = new MyToolbarbutton("fa-pencil-square-o", "Lengkapi Profil Perusahaan");
			bio.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PenyediaAssetAction.onAddExternal(null, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Executions.getCurrent().sendRedirect("");
						}
					}, penyediaAsset, null, null);
				}
			});

			MyToolbarbutton cetakbio = new MyToolbarbutton("fa-print", "Cetak Profil Perusahaan");
			cetakbio.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					File file = CommonReportHelper.onCetakPenyediaAsset(penyediaAsset, true);
					Filedownload.save(file, "application/pdf");
				}
			});

			if (mobile) {
				Vbox vbox = new Vbox();
				vbox.setParent(groupboxStyled1);
				vbox.appendChild(bio);
				vbox.appendChild(cetakbio);
				vbox.appendChild(logout);
			} else {
				Hbox hbox = new Hbox();
				hbox.setPack("center");
				hbox.setAlign("center");
				hbox.setParent(groupboxStyled1);
				hbox.appendChild(bio);
				hbox.appendChild(cetakbio);
				hbox.appendChild(logout);
			}

			int lain = 0;
			if (mobile) {
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					Sekolah selectedSekolah = SekolahUtil.getSekolah();
					Yayasan selectedYayasan = SekolahUtil.getYayasan();
					Criteria criteria = TampilanPengumumanVendorAction
							.initCriteriaStatic(true, selectedSekolah, selectedYayasan).setMaxResults(20);
					List<PengumumanAkademis> pengumumanAkademisLain = ConstantValues.simpleList(criteria,
							PengumumanAkademis.class);

					int size = pengumumanAkademisLain.size();
					if (size > 0) {
						lain = (size * 20) + 60;
					}
					PengumumanAkademisAction.tampilPengumumanLain(rows, null, pengumumanAkademisLain);
				} finally {
					if (session != null) {
						try {
							if (session.isOpen())
								session.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							session.disconnect();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							if (session.isOpen())
								session.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				}
			}

			component.setHeight(mobile ? "" + (700 + lain) + "px" : "300px");
		}
	}

	@SuppressWarnings("unchecked")
	public static void tampilGelombang(final PerguruanTinggi selectedPerguruanTinggi, final North component,
			final CalonPegawai calonPegawai, final boolean mobile) {

		if (calonPegawai != null) {
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(component);
			grid.setOddRowSclass("non-odd");
			grid.setStyle("border:0px;background: transparent;");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setAlign("center");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Groupbox groupboxStyled = new Groupbox();
			groupboxStyled.appendChild(new MyCaptionStyled("Profil Calon Pegawai"));
			groupboxStyled.setParent(row);
			groupboxStyled.setWidth("97%");
			groupboxStyled.setStyle(
					"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;min-width: 330px;");

			String url = "";

			if (calonPegawai.getId() != null) {
				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					LampiranLain lampiranLain = (LampiranLain) streamingSession.createCriteria(LampiranLain.class)
							.addOrder(Order.asc("id")).add(Restrictions.eq("ref", calonPegawai.getId()))
							.setMaxResults(1).add(Restrictions.ilike("jenis", "Galery_CalonPegawai_", MatchMode.START))
							.uniqueResult();

					url = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/TampilanPengumumanAkademisAction.java:3739");
				} finally {
					if (streamingSession != null) {
						try {
							if (streamingSession.isOpen())
								streamingSession.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							streamingSession.disconnect();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							if (streamingSession.isOpen())
								streamingSession.clear();
								streamingSession.disconnect();
								streamingSession.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				}
			}

			StringBuilder htmlBuilder = new StringBuilder();
			htmlBuilder.append("<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
					.append("<tr valign=\"top\"><td valign=\"top\" ")
					.append(mobile ? "colspan=\"2\"" : "rowspan=\"15\"").append("><img style='height: ")
					.append(mobile ? "100px" : "140px")
					.append(";border: 0.5px solid #d9d9d9 !important; border-radius: 15px; aspect-ratio: 1; object-fit: cover; box-shadow: 2px 2px 4px grey;' src=\"")
					.append(url).append("\"/></td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("No. Registrasi")).append("</td><td>")
					.append(calonPegawai.getKode() != null ? calonPegawai.getKode() : "").append("</td></tr>")
					.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Nama")).append("</td><td>")
					.append(calonPegawai.getNama() != null ? calonPegawai.getNama() : "").append("</td></tr>");

			StringBuilder alamatBuilder = new StringBuilder(
					calonPegawai.getAlamatPegawai() != null ? calonPegawai.getAlamatPegawai() : "");
			if (calonPegawai.getKecamatan() != null) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Kec.")
						.append(calonPegawai.getKecamatan().getNama());
			}
			if (calonPegawai.getKota() != null) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Kab/Kota.")
						.append(calonPegawai.getKota().getNama());
			}
			if (calonPegawai.getPropinsi() != null) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Prop.")
						.append(calonPegawai.getPropinsi().getNama());
			}
			if (calonPegawai.getKodePos() != null && !calonPegawai.getKodePos().trim().isEmpty()) {
				alamatBuilder.append(alamatBuilder.length() == 0 ? "" : ", ").append("Kode Pos ")
						.append(calonPegawai.getKodePos());
			}

			htmlBuilder.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Alamat")).append("</td><td>")
					.append(alamatBuilder.toString()).append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Telp.")).append("</td><td>")
					.append(calonPegawai.getTeleponPegawai() != null ? calonPegawai.getTeleponPegawai() : "")
					.append("</td></tr>").append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Email"))
					.append("</td><td>")
					.append(calonPegawai.getAlamatEmail() != null ? calonPegawai.getAlamatEmail() : "")
					.append("</td></tr>").append("<tr valign=\"top\"><td>")
					.append(Common.getBahasaConfig("Status Aktif")).append("</td><td>")
					.append(calonPegawai.getAktif() != null && calonPegawai.getAktif() ? "Ya" : "Tidak")
					.append("</td></tr>").append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Keterangan"))
					.append("</td><td>")
					.append(calonPegawai.getKeterangan() != null ? calonPegawai.getKeterangan() : "")
					.append("</td></tr>").append("</table>");

			groupboxStyled.appendChild(new Html(htmlBuilder.toString()));

			row = new MyFormRow();
			row.setParent(rows);

			Groupbox groupboxStyled1 = new Groupbox();
			groupboxStyled1.setParent(row);
			groupboxStyled1.setWidth("97%");
			groupboxStyled1.setStyle(
					"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;width: 320px;");

			MyToolbarbutton logout = new MyToolbarbutton("fa-sign-out", "Logout");
			logout.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.setLogoutCalonPegawai();
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Executions.getCurrent().sendRedirect("");
						}
					});
				}
			});

			MyToolbarbutton bio = new MyToolbarbutton("fa-pencil-square-o", "Lengkapi Profil Anda");
			bio.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CalonPegawaiAction.onAddExternal(null, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Executions.getCurrent().sendRedirect("");
						}
					}, calonPegawai, null, null);
				}
			});

			if (mobile) {
				Vbox vbox = new Vbox();
				vbox.setParent(groupboxStyled1);
				vbox.appendChild(bio);
				vbox.appendChild(logout);
			} else {
				Hbox hbox = new Hbox();
				hbox.setPack("center");
				hbox.setAlign("center");
				hbox.setParent(groupboxStyled1);
				hbox.appendChild(bio);
				hbox.appendChild(logout);
			}

			int lain = 0;
			if (mobile) {
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					Sekolah selectedSekolah = SekolahUtil.getSekolah();
					Yayasan selectedYayasan = SekolahUtil.getYayasan();
					Criteria criteria = TampilanPengumumanKarirAction
							.initCriteriaStatic(true, selectedSekolah, selectedYayasan).setMaxResults(20);
					List<PengumumanAkademis> pengumumanAkademisLain = ConstantValues.simpleList(criteria,
							PengumumanAkademis.class);

					int size = pengumumanAkademisLain.size();
					if (size > 0) {
						lain = (size * 20) + 60;
					}
					PengumumanAkademisAction.tampilPengumumanLain(rows, null, pengumumanAkademisLain);
				} finally {
					if (session != null) {
						try {
							if (session.isOpen())
								session.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							session.disconnect();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							if (session.isOpen())
								session.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				}
			}

			component.setHeight(mobile ? "" + (700 + lain) + "px" : "300px");

		} else {

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setOddRowSclass("non-odd");
			grid.setStyle("border:0px;background: transparent;");

			Rows rows = new Rows();
			rows.setParent(grid);

			int lain = 0;
			if (mobile) {
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					Criteria criteria = TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi)
							.setMaxResults(20);
					List<PengumumanAkademis> pengumumanAkademisLain = ConstantValues.simpleList(criteria,
							PengumumanAkademis.class);

					int size = pengumumanAkademisLain.size();
					if (size > 0) {
						lain = (size * 20) + 60;
					}
					PengumumanAkademisAction.tampilPengumumanLain(rows, null, pengumumanAkademisLain);
				} finally {
					if (session != null) {
						try {
							if (session.isOpen())
								session.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							session.disconnect();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						try {
							if (session.isOpen())
								session.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				}
			}

			Map<Long, GeneralValueObject> gelombangsAktif = ConstantValues
					.ambilBerdasarClass(GelombangPendaftaranPegawai.class);
			Date sekarang = WaktuUtil.getDate();
			List<GelombangPendaftaranPegawai> gelombangPendaftaranPegawais = new ArrayList<GelombangPendaftaranPegawai>();

			for (Long gelId : gelombangsAktif.keySet()) {
				GelombangPendaftaranPegawai gelombangPendaftaranPegawai1 = (GelombangPendaftaranPegawai) ConstantValues
						.ambil(GelombangPendaftaranPegawai.class.getName(), gelId);
				if (gelombangPendaftaranPegawai1 != null && gelombangPendaftaranPegawai1.getAktif()
						&& (gelombangPendaftaranPegawai1.getMulai().before(sekarang)
								|| Common.dateFormat8.get().format(gelombangPendaftaranPegawai1.getMulai())
										.equals(Common.dateFormat8.get().format(sekarang)))
						&& (gelombangPendaftaranPegawai1.getSampai().after(sekarang)
								|| Common.dateFormat8.get().format(gelombangPendaftaranPegawai1.getSampai())
										.equals(Common.dateFormat8.get().format(sekarang)))) {

					gelombangPendaftaranPegawais.add(gelombangPendaftaranPegawai1);
				}
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Box hbox = mobile ? new Vbox() : new Hbox();
			hbox.setPack("center");
			hbox.setAlign("center");
			hbox.setParent(row);
			hbox.setWidth("100%");
			int jml = 0;

			grid.setParent(component);

			if (!gelombangPendaftaranPegawais.isEmpty()) {
				for (final GelombangPendaftaranPegawai myGelombangPendaftaranPegawai : gelombangPendaftaranPegawais) {
					if (myGelombangPendaftaranPegawai.getAktif()) {
						if (!mobile) {
							if (jml > 0 && jml % 3 == 0) {
								row = new MyFormRow();
								row.setParent(rows);
								hbox = new Hbox();
								hbox.setPack("center");
								hbox.setAlign("center");
								hbox.setWidth("100%");
								hbox.setParent(row);
							}
						}

						jml++;

						Groupbox groupboxStyled = new Groupbox();
						groupboxStyled.setStyle(
								"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");

						hbox.appendChild(groupboxStyled);
						groupboxStyled.appendChild(new MyCaptionStyled(myGelombangPendaftaranPegawai.getNama()));

						StringBuilder htmlDaftar = new StringBuilder();
						htmlDaftar.append(
								"<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
								.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Pendaftaran"))
								.append("</td><td>")
								.append(Common.dateFormat1.get().format(myGelombangPendaftaranPegawai.getMulai()))
								.append(" sd ")
								.append(Common.dateFormat1.get().format(myGelombangPendaftaranPegawai.getSampai()))
								.append("</td></tr>").append("<tr valign=\"top\"><td>")
								.append(Common.getBahasaConfig("Informasi")).append("</td><td>")
								.append(myGelombangPendaftaranPegawai.getInformasi() != null
										? myGelombangPendaftaranPegawai.getInformasi()
										: "")
								.append("</td></tr>").append("</table>");

						groupboxStyled.appendChild(new Html(htmlDaftar.toString()));

						MyToolbarbutton formulir = new MyToolbarbutton("fa-pencil-square-o",
								Common.getBahasaConfig("Daftar Sekarang"));
						formulir.getLabelC().setStyle("font-size:15px");
						groupboxStyled.appendChild(formulir);
						formulir.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								CalonPegawai calonPegawai = new CalonPegawai();
								calonPegawai.setGelombangPendaftaranPegawai(myGelombangPendaftaranPegawai);

								CalonPegawaiAction.onAddExternalDaftar(null, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
									}
								}, calonPegawai, null, null);
							}
						});

						formulir = new MyToolbarbutton("fa-info-circle", Common.getBahasaConfig("Informasi"));
						formulir.getLabelC().setStyle("font-size:15px");
						groupboxStyled.appendChild(formulir);
						formulir.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								CalonPegawai calonPegawai = new CalonPegawai();
								calonPegawai.setGelombangPendaftaranPegawai(myGelombangPendaftaranPegawai);

								CalonPegawaiAction.onInfo(null, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
									}
								}, calonPegawai, null, null);
							}
						});
					}
				}
			}

			component.setHeight(
					mobile ? (((jml + 1) * 160) + 190 + lain) + "px" : ((((jml / 3) + 1) * 180) + 50) + "px");

			if (!mobile) {
				if (jml > 0 && jml % 3 == 0) {
					row = new MyFormRow();
					row.setParent(rows);
					hbox = new Hbox();
					hbox.setPack("center");
					hbox.setAlign("center");
					hbox.setWidth("100%");
					hbox.setParent(row);
				}
			}

			jml++;

			Groupbox groupboxStyled = new Groupbox();
			groupboxStyled.setStyle(
					"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");

			hbox.appendChild(groupboxStyled);
			groupboxStyled.appendChild(new MyCaptionStyled("Login"));

			String body = "<table style=\"width:100%;border:0px solid black;padding: 5px 20px 10px 5px;\">"
					+ "<tr valign=\"top\"><td>"
					+ Common.getBahasaConfig(
							"Jika Anda telah melakukan pendaftaran,<br>pilih login untuk melengkapi data,<br> informasi ujian dan pembayaran,<br>serta informasi kelulusan.")
					+ "</td></tr></table>";

			body = Common.getKonfigurasi("body_login_psb", body).getNilai();

			groupboxStyled.appendChild(new Html(body));

			MyToolbarbutton formulir = new MyToolbarbutton("fa-sign-in", Common.getBahasaConfig("Login Sekarang"));
			formulir.getLabelC().setStyle("font-size:15px");
			groupboxStyled.appendChild(formulir);
			formulir.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.displayWindow("/pages/psb/login_calon_siswa.zul", true, "500px",
							Common.isMobile() ? "100%" : "850px", null, "", false);
				}
			});
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void tampilGelombang(final PerguruanTinggi selectedPerguruanTinggi, String ta,
	        GelombangPendaftaranPsb gelombangPendaftaranPsb, final Row componentdata, final CalonSiswa calonSiswa,
	        final MyToolbarbutton alur, final MyToolbarbutton informasiPembayaran,
	        final MyToolbarbutton loginCalonMhs, final MyToolbarbutton informasiKelulusan,
	        final MyToolbarbutton pembayaranViaPaymentGateway, final MyToolbarbutton refresh,
	        final boolean mobile) {

	    refresh.setVisible(calonSiswa != null && calonSiswa.getId() != null);

	    Session session = null;

	    try {
	        session = HibernateUtil.getSessionFactory().openSession();

	        if (calonSiswa != null && calonSiswa.getId() != null) {

	            try {
	                String strHasilUjianMahasiswa = calonSiswa.retreive("hasilUjianMahasiswa");
	                HasilUjianMahasiswa.tampilkanUjianKembali(strHasilUjianMahasiswa);
	            } catch (Exception e) {
	                ais.common.Common.tampilErrorJikaAdmin(e);
	            }

	            if (Sessions.getCurrent(true).getAttribute("cetak") != null) {
	                Sessions.getCurrent(true).removeAttribute("cetak");
	                Common.createDefaultTimer(new EventListener() {
	                    @Override
	                    public void onEvent(Event arg0) throws Exception {
	                        CalonSiswaAction.onCetakKartu(calonSiswa, true);
	                        if (calonSiswa.getGelombangPendaftaranPsb() != null
	                                && calonSiswa.getGelombangPendaftaranPsb().getMunculkanTagihanSetelahDaftar()) {
	                            calonSiswa.munculkanFormPembayaran(new EventListener() {
	                                @Override
	                                public void onEvent(Event arg0) throws Exception {
	                                    Executions.getCurrent().sendRedirect("");
	                                }
	                            });
	                        }
	                    }
	                });
	            }

	            Grid grid = new Grid();
	            grid.setSclass("dgrid");
	            grid.setParent(componentdata);
	            grid.setOddRowSclass("non-odd");
	            grid.setStyle("border:0px;background: transparent;");

	            Columns columns = new Columns();
	            columns.setParent(grid);
	            MyColumnConfig column = new MyColumnConfig();
	            column.setParent(columns);
	            column.setAlign("center");

	            Rows rows = new Rows();
	            rows.setParent(grid);
	            MyFormRow row = new MyFormRow();
	            row.setValign("top");
	            row.setParent(rows);

	            final GelombangPendaftaranPsb myGelombangPendaftaranPsb = calonSiswa.getGelombangPendaftaranPsb();

	            if (myGelombangPendaftaranPsb != null) {

	                if (myGelombangPendaftaranPsb.getTampilFormTambahanDiHalamanUtama()) {
	                    MyFormRow row1 = new MyFormRow();
	                    row1.setParent(rows);

	                    Groupbox groupboxStyledLampiran = new Groupbox();
	                    groupboxStyledLampiran.appendChild(new MyCaptionStyled("Form Calon Siswa"));
	                    groupboxStyledLampiran.setParent(row1);
	                    groupboxStyledLampiran.setWidth("97%");
	                    groupboxStyledLampiran.setStyle(
	                            "border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;min-width: 330px;");

	                    Grid gridLampiran = new Grid();
	                    gridLampiran.setSclass("dgrid fgrid");
	                    gridLampiran.setParent(groupboxStyledLampiran);
	                    gridLampiran.setStyle("border:0px;background: transparent;");

	                    Columns columnsLampiran = new Columns();
	                    columnsLampiran.setParent(gridLampiran);

	                    MyColumnConfig columnLampiranRight = new MyColumnConfig();
	                    columnLampiranRight.setParent(columnsLampiran);
	                    columnLampiranRight.setAlign("right");

	                    MyColumnConfig columnLampiranLeft = new MyColumnConfig();
	                    columnLampiranLeft.setParent(columnsLampiran);
	                    columnLampiranLeft.setAlign("left");

	                    Rows rowsLampiran = new Rows();
	                    rowsLampiran.setParent(gridLampiran);

	                    List<KelompokParameterTambahanCalonSiswa> kelompokParameterTambahanCalonSiswas = session
	                            .createCriteria(ParameterTambahanGelombangPendaftaranPsb.class)
	                            .createAlias("parameterTambahan", "parameterTambahan")
	                            .createAlias("kelompokParameterTambahanCalonSiswa", "kelompokParameterTambahanCalonSiswa")
	                            .add(Restrictions.eq("parameterTambahan.aktif", true))
	                            .add(Restrictions.eq("kelompokParameterTambahanCalonSiswa.aktif", true))
	                            .setProjection(Projections.groupProperty("kelompokParameterTambahanCalonSiswa"))
	                            .add(Restrictions.or(Restrictions.isNull("gelombangPendaftaranPsb"),
	                                    Restrictions.eq("gelombangPendaftaranPsb", myGelombangPendaftaranPsb)))
	                            .list();

	                    Collections.sort(kelompokParameterTambahanCalonSiswas);
	                    boolean tampilSemua = false;

	                    // OPTIMASI: Caching data parameter menggunakan Map (O(1) Access) agar tidak terus di-split dalam perulangan bersarang
	                    Map<String, String[]> mapParamValues = new HashMap<String, String[]>();
	                    if (calonSiswa.getParameterTambahanInds() != null && !calonSiswa.getParameterTambahanInds().trim().isEmpty()) {
	                        String[] spl = calonSiswa.getParameterTambahanInds().split("\n");
	                        for (String d : spl) {
	                            String[] value = d.split("<=>");
	                            if (value.length > 0) {
	                                String keyJenis = value[0].trim();
	                                String vVal = value.length > 1 ? value[1].trim() : "";
	                                String vKet = "";
	                                try {
	                                    vKet = value.length > 2 ? value[value.length - 1] : "";
	                                } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	                                mapParamValues.put(keyJenis.toLowerCase(), new String[] { vVal, vKet });
	                            }
	                        }
	                    }

	                    for (KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa : kelompokParameterTambahanCalonSiswas) {
	                        MyFormRow rowParameterTambahan = new MyFormRow();
	                        rowParameterTambahan.setVisible(false);
	                        rowParameterTambahan.setParent(rowsLampiran);
	                        rowParameterTambahan.appendChild(new ais.ui.util.MyLabelStyled(kelompokParameterTambahanCalonSiswa.getNama()));
	                        rowParameterTambahan.appendChild(new MyLabelStyled("Data"));

	                        List<ParameterTambahan> parameterTambahans = session
	                                .createCriteria(ParameterTambahanGelombangPendaftaranPsb.class)
	                                .add(Restrictions.eq("kelompokParameterTambahanCalonSiswa", kelompokParameterTambahanCalonSiswa))
	                                .createAlias("parameterTambahan", "parameterTambahan")
	                                .createAlias("kelompokParameterTambahanCalonSiswa", "kelompokParameterTambahanCalonSiswa")
	                                .add(Restrictions.eq("parameterTambahan.aktif", true))
	                                .add(Restrictions.eq("kelompokParameterTambahanCalonSiswa.aktif", true))
	                                .setProjection(Projections.groupProperty("parameterTambahan"))
	                                .add(Restrictions.or(Restrictions.isNull("gelombangPendaftaranPsb"),
	                                        Restrictions.eq("gelombangPendaftaranPsb", myGelombangPendaftaranPsb)))
	                                .list();

	                        Collections.sort(parameterTambahans);

	                        boolean tampil = false;
	                        rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
	                        if (!parameterTambahans.isEmpty()) {
	                            for (ParameterTambahan parameterTambahan : parameterTambahans) {
	                                String jenis = kelompokParameterTambahanCalonSiswa.getId() + "->" + parameterTambahan.getId();
	                                Row rowLampiran = new MyRowStyled();
	                                rowLampiran.setAttribute("parameterTambahan", parameterTambahan);
	                                rowLampiran.setAttribute("kelompokParameterTambahanCalonSiswa", kelompokParameterTambahanCalonSiswa);
	                                rowLampiran.setParent(rowsLampiran);
	                                rowLampiran.appendChild(new ais.ui.util.MyLabelConfig(parameterTambahan.getLabelInputan()
	                                        + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

	                                String val = "";
	                                String ket = "";

	                                String[] paramData = mapParamValues.get(jenis.toLowerCase());
	                                if (paramData != null) {
	                                    val = paramData[0];
	                                    ket = paramData[1];
	                                }

	                                tampil |= ParameterTambahan.initComponent(rowLampiran, rowsLampiran, jenis, null,
	                                        null, calonSiswa.getId(), val, ket, parameterTambahan, null);
	                            }
	                        }
	                        rowParameterTambahan.setVisible(tampil);
	                        tampilSemua |= tampil;
	                    }
	                    row1.setVisible(tampilSemua);
	                }

	                if (myGelombangPendaftaranPsb.getTampilFormLampiranDiHalamanUtama()) {
	                    MyFormRow row1 = new MyFormRow();
	                    row1.setParent(rows);

	                    Groupbox groupboxStyledLampiran = new Groupbox();
	                    groupboxStyledLampiran.appendChild(new MyCaptionStyled("Berkas Calon Siswa"));
	                    groupboxStyledLampiran.setParent(row1);
	                    groupboxStyledLampiran.setWidth("97%");
	                    groupboxStyledLampiran.setStyle(
	                            "border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;min-width: 330px;");

	                    Grid gridLampiran = new Grid();
	                    gridLampiran.setSclass("dgrid fgrid");
	                    gridLampiran.setParent(groupboxStyledLampiran);
	                    gridLampiran.setStyle("border:0px;background: transparent;");

	                    Columns columnsLampiran = new Columns();
	                    columnsLampiran.setParent(gridLampiran);
	                    MyColumnConfig columnLampiranCenter = new MyColumnConfig();
	                    columnLampiranCenter.setParent(columnsLampiran);
	                    columnLampiranCenter.setAlign("center");

	                    Rows rowsLampiran = new Rows();
	                    rowsLampiran.setParent(gridLampiran);

	                    Row rowVerifikasi = new MyRowStyled();
	                    rowVerifikasi.setParent(rowsLampiran);

	                    Rows subRowsVerifikasiKelengkapanCalonSiswa = new Rows();
	                    EventListener eventListenerBerkas = VerifikasiPSBHelper.tampilkanGrid(rowVerifikasi,
	                            subRowsVerifikasiKelengkapanCalonSiswa, myGelombangPendaftaranPsb,
	                            calonSiswa == null ? null : calonSiswa.getId(), null, true);
	                    try {
	                        if (eventListenerBerkas != null) {
	                            eventListenerBerkas.onEvent(null);
	                        }
	                        row1.setVisible(rowVerifikasi.isVisible());
	                    } catch (Exception e) {
	                        ais.common.Common.tampilErrorJikaAdmin(e);
	                    }

	                    if (calonSiswa.getSiswa() != null) {
	                        row1 = new MyFormRow();
	                        row1.setParent(rows);

	                        Groupbox groupboxLulus = new Groupbox();
	                        groupboxLulus.appendChild(new MyCaptionStyled("Selamat, Anda telah dinyatakan diterima sebagai siswa"));
	                        groupboxLulus.setParent(row1);
	                        groupboxLulus.setWidth("97%");
	                        groupboxLulus.setStyle(
	                                "border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 100%;border-width: 1px;min-width: 330px;");

	                        Grid gridLulus = new Grid();
	                        gridLulus.setSclass("dgrid fgrid");
	                        gridLulus.setParent(groupboxLulus);
	                        gridLulus.setStyle("border:0px;background: transparent;");

	                        Columns columnsLulus = new Columns();
	                        columnsLulus.setParent(gridLulus);

	                        MyColumnConfig colRight = new MyColumnConfig();
	                        colRight.setParent(columnsLulus);
	                        colRight.setAlign("right");

	                        MyColumnConfig colLeft = new MyColumnConfig();
	                        colLeft.setParent(columnsLulus);
	                        colLeft.setAlign("left");

	                        Rows rowsLulus = new Rows();
	                        rowsLulus.setParent(gridLulus);

	                        Row rowDetail = new MyRowStyled();
	                        rowDetail.setParent(rowsLulus);
	                        rowDetail.appendChild(new ais.ui.util.MyLabelConfig("NIS"));
	                        rowDetail.appendChild(new Label(calonSiswa.getSiswa().getNomorInduk()));

	                        KelasSiswa kelasSiswa = calonSiswa.getSiswa().getKelas();
	                        String w = kelasSiswa == null ? "" : kelasSiswa.getNama();
	                        if (w != null && !w.isEmpty()) {
	                            Row rowKelas = new MyRowStyled();
	                            rowKelas.setParent(rowsLulus);
	                            rowKelas.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
	                            rowKelas.appendChild(new Label(w));
	                        }

	                        w = (kelasSiswa == null || kelasSiswa.getGuruPembina() == null) ? ""
	                                : kelasSiswa.getGuruPembina().getNama();
	                        if (w != null && !w.isEmpty()) {
	                            Row rowWali = new MyRowStyled();
	                            rowWali.setParent(rowsLulus);
	                            rowWali.appendChild(new ais.ui.util.MyLabelConfig("Wali Kelas"));
	                            rowWali.appendChild(new Label(w));
	                        }

	                        Row rowGanti = new MyRowStyled();
	                        rowGanti.setParent(rowsLulus);
	                        rowGanti.appendChild(new ais.ui.util.MyLabelConfig(""));
	                        MyToolbarbutton ganti = new MyToolbarbutton("fa-user-circle", "Ganti Profil Siswa");
	                        rowGanti.appendChild(ganti);

	                        ganti.addEventListener("onClick", new EventListener() {
	                            @Override
	                            public void onEvent(Event arg0) throws Exception {
	                                Session sessionAction = null;
	                                try {
	                                    sessionAction = HibernateUtil.getSessionFactory().openSession();
	                                    Siswa siswa = calonSiswa.getSiswa();
	                                    if (siswa != null) {
	                                        sessionAction.refresh(siswa);
	                                        SiswaAction.onAddExternal(null, new EventListener() {
	                                            @Override
	                                            public void onEvent(Event arg0) throws Exception {}
	                                        }, siswa);
	                                    }
	                                } catch (Exception e) {
	                                    ais.common.Common.tampilErrorJikaAdmin(e);
	                                } finally {
	                                    if (sessionAction != null) {
	                                        try { if (sessionAction.isOpen()) sessionAction.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	                                        try { sessionAction.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	                                        try { if (sessionAction.isOpen()) sessionAction.clear();
										sessionAction.disconnect();
										sessionAction.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	                                    }
	                                }
	                            }
	                        });

	                        Row rowLoginSiswa = new MyRowStyled();
	                        rowLoginSiswa.setParent(rowsLulus);
	                        rowLoginSiswa.appendChild(new ais.ui.util.MyLabelConfig("Login Siswa"));
	                        final A a = new A("Tampilkan Link Login");
	                        a.setHref("");
	                        rowLoginSiswa.appendChild(a);

	                        a.addEventListener("onClick", new EventListener() {
	                            @Override
	                            public void onEvent(Event arg0) throws Exception {
	                                Siswa siswa = calonSiswa.getSiswa();
	                                if (siswa != null) {
	                                    String code = siswa.urlLogin();
	                                    a.setLabel(code);
	                                    a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
	                                            + URLEncoder.encode(code, "UTF-8"));
	                                }
	                            }
	                        });

	                        try {
	                            if (myGelombangPendaftaranPsb != null
	                                    && myGelombangPendaftaranPsb.getTampilkanQrCodeMahasiswaSetelahDapatNim()) {
	                                Row rowQr = new MyRowStyled();
	                                ais.ui.util.ZkCompat.setSpans(rowQr, "2");
	                                rowQr.setParent(rowsLulus);
	                                MainHelper.onDapatkanKode(new Tbmuser(calonSiswa.getSiswa()), rowQr, false);
	                            }
	                        } catch (Exception e) {
	                            ais.common.Common.tampilErrorJikaAdmin(e);
	                        }
	                    }
	                }
	            }

	            // ================================================================
	            // PROFIL CALON SISWA — kartu modern HTML murni.
	            // Foto bulat kecil 64px, badge status, tabel info, progress tracker.
	            // ================================================================
	            Groupbox groupboxStyled = new Groupbox();
	            groupboxStyled.appendChild(new MyCaptionStyled("Profil Calon Siswa"));
	            groupboxStyled.setParent(row);
	            applyModernGroupboxStyle(groupboxStyled);
	            appendPanelDescription(groupboxStyled,
	                    "Data pendaftaran, status seleksi, dan informasi biaya kamu tampil di sini."
	                    + " Ikuti setiap tahapan agar proses pendaftaran selesai.");

	            // Wadah tombol aksi — transparan, tanpa border
	            Groupbox groupboxStyled1 = new Groupbox();
	            groupboxStyled1.setParent(groupboxStyled);
	            groupboxStyled1.setStyle("border:0;padding:0;background:transparent;max-width:100%;");

	            String url = "";
	            try {
	                url = CommonMedia.getUrlFotoPengguna(new Tbmuser(calonSiswa));
	            } catch (Exception e) {
	                ais.common.Common.tampilErrorJikaAdmin(e);
	            }

	            // Badge status — warna berbeda sesuai kondisi seleksi
	            String statusBadgeTxt;
	            String statusBadgeColor;
	            String statusBadgeBg;
	            if (calonSiswa.getMengundurkanDiri() != null && calonSiswa.getMengundurkanDiri()) {
	                statusBadgeTxt = "Mengundurkan Diri";
	                statusBadgeColor = "#991b1b"; statusBadgeBg = "#fee2e2";
	            } else if (calonSiswa.getDitolak() != null && calonSiswa.getDitolak()) {
	                statusBadgeTxt = "Tidak Diterima";
	                statusBadgeColor = "#7c3aed"; statusBadgeBg = "#ede9fe";
	            } else if (calonSiswa.getTelahDiterima() != null && calonSiswa.getTelahDiterima()) {
	                statusBadgeTxt = "&#10003; Telah Diterima";
	                statusBadgeColor = "#166534"; statusBadgeBg = "#dcfce7";
	            } else if (calonSiswa.getTerverifikasi() != null && calonSiswa.getTerverifikasi()) {
	                statusBadgeTxt = "&#10003; Terverifikasi";
	                statusBadgeColor = "#0369a1"; statusBadgeBg = "#e0f2fe";
	            } else {
	                statusBadgeTxt = "Menunggu Seleksi";
	                statusBadgeColor = "#92400e"; statusBadgeBg = "#fef3c7";
	            }

	            StringBuilder htmlBuilder = new StringBuilder();

	            // === HEADER KARTU: foto bulat 64px + nama + no.reg + badge status ===
	            htmlBuilder
	                .append("<div style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;padding:10px 12px 4px 12px;'>")
	                .append("<div style='text-align:center;padding-bottom:14px;border-bottom:1px solid #e8ecf0;margin-bottom:10px;'>")
	                .append("<div style='margin:0 auto 10px auto;width:192px;height:192px;border-radius:50%;")
	                .append("overflow:hidden;border:4px solid #e2e8f0;box-shadow:0 4px 16px rgba(0,0,0,0.15);background:#f8fafc;'>")
	                .append("<img src='")
	                .append(htmlEscape(url))
	                .append("' style='width:192px;height:192px;object-fit:cover;display:block;'/>")
	                .append("</div>")
	                .append("<div style='font-weight:700;font-size:15px;color:#1e293b;line-height:1.35;'>")
	                .append(htmlEscape(safeText(calonSiswa.getNama()))).append("</div>")
	                .append("<div style='font-size:12px;color:#64748b;margin-top:3px;'>No. Reg: ")
	                .append(htmlEscape(safeText(calonSiswa.getNoRegistrasi()))).append("</div>")
	                .append("<div style='margin-top:7px;'>")
	                .append("<span style='display:inline-block;padding:3px 12px;border-radius:20px;")
	                .append("font-size:11px;font-weight:600;background:").append(statusBadgeBg)
	                .append(";color:").append(statusBadgeColor).append(";'>")
	                .append(statusBadgeTxt).append("</span></div>")
	                .append("</div>")
	                // === TABEL INFO — 2 kolom: label kiri abu-abu, nilai kanan ===
	                .append("<table style='width:100%;border-collapse:collapse;'>")
	                .append(buildInfoRow(Common.getBahasaConfig("Gelombang"),
	                    htmlEscape(myGelombangPendaftaranPsb == null ? "" : myGelombangPendaftaranPsb.getNama())))
	                .append(buildInfoRow(Common.getBahasaConfig("Periode"),
	                    htmlEscape(myGelombangPendaftaranPsb == null ? "" : myGelombangPendaftaranPsb.getTahunAjaran())))
	                .append(buildInfoRow(Common.getBahasaConfig("Sekolah"),
	                    htmlEscape(calonSiswa.getSekolah() == null ? "" : calonSiswa.getSekolah().getNama())))
	                .append(buildInfoRow(Common.getBahasaConfig("Penjurusan"),
	                    htmlEscape(calonSiswa.getPenjurusanSekolah() == null ? "" : calonSiswa.getPenjurusanSekolah().getNama())))
	                .append(buildInfoRow(Common.getBahasaConfig("No. Registrasi"),
	                    htmlEscape(safeText(calonSiswa.getNoRegistrasi()))));

	            if (myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilUjian()) {
	                htmlBuilder.append(buildInfoRow(Common.getBahasaConfig("No. Ujian"),
	                    htmlEscape(calonSiswa.getNoUjian() == null ? "-" : calonSiswa.getNoUjian())));
	            }

	            htmlBuilder
	                .append(buildInfoRow(Common.getBahasaConfig("Nama"),
	                    "<strong>" + htmlEscape(safeText(calonSiswa.getNama())) + "</strong>"))
	                .append(buildInfoRow(Common.getBahasaConfig("Tempat dan Tanggal Lahir"),
	                    htmlEscape(safeText(calonSiswa.getTempatLahir())) + ", "
	                    + (calonSiswa.getTanggalLahir() == null ? ""
	                        : Common.dateFormat2.get().format(calonSiswa.getTanggalLahir()))))
	                .append(buildInfoRow(Common.getBahasaConfig("Nama Orang Tua"),
	                    htmlEscape(safeText(calonSiswa.getNamaAyah())) + " / "
	                    + htmlEscape(safeText(calonSiswa.getNamaIbu()))))
	                .append(buildInfoRow(Common.getBahasaConfig("Telp. Orang Tua"),
	                    htmlEscape(safeText(calonSiswa.getTeleponOrangTua()))));

	            if (calonSiswa.getJadwalPertemuanPSB() != null && calonSiswa.getJadwalPertemuanPSB().getAktif()) {
	                String jadwalStr = htmlEscape(calonSiswa.getJadwalPertemuanPSB().getNama())
	                        + ", Jadwal: "
	                        + Common.dateFormat51.get().format(calonSiswa.getJadwalPertemuanPSB().getWaktuMulai())
	                        + " s/d "
	                        + Common.dateFormat51.get().format(calonSiswa.getJadwalPertemuanPSB().getWaktuSampai());
	                htmlBuilder.append(buildInfoRow(
	                    Common.getBahasaConfig("Jadwal Pertemuan Siswa / Orang Tua"), jadwalStr));
	            }

	            // Status seleksi dengan warna berbeda tiap kondisi
	            String diterima;
	            String diterimaClr;
	            if (calonSiswa.getMengundurkanDiri() != null && calonSiswa.getMengundurkanDiri()) {
	                diterima = "Mengundurkan diri"; diterimaClr = "#dc2626";
	            } else if (calonSiswa.getDitolak() != null && calonSiswa.getDitolak()) {
	                diterima = "Tidak diterima (ditolak)"; diterimaClr = "#7c3aed";
	            } else if (calonSiswa.getTelahDiterima() != null && calonSiswa.getTelahDiterima()) {
	                diterima = "Telah diterima"; diterimaClr = "#16a34a";
	            } else if (calonSiswa.getTerverifikasi() != null && calonSiswa.getTerverifikasi()) {
	                diterima = "Telah diverifikasi"; diterimaClr = "#0369a1";
	            } else {
	                diterima = "Belum dinyatakan lulus / diterima"; diterimaClr = "#92400e";
	            }

	            htmlBuilder.append(buildInfoRow(Common.getBahasaConfig("Status Diterima"),
	                "<span style='color:" + diterimaClr + ";font-weight:600;'>" + htmlEscape(diterima) + "</span>"));

	            if (calonSiswa.getKeterangan() != null && !calonSiswa.getKeterangan().trim().isEmpty()) {
	                htmlBuilder.append(buildInfoRow(Common.getBahasaConfig("Keterangan Status"),
	                    htmlEscape(calonSiswa.getKeterangan())));
	            }

	            // === DATA PEMBAYARAN — kumpulkan lalu render info rows berwarna ===
	            Siswa siswa = calonSiswa.getSiswa();
	            List<Long> dibayars = new ArrayList<Long>();

	            Criteria pembayaransCriteria = session.createCriteria(PembayaranSiswaDetail.class)
	                    .add(Restrictions.isNotNull("tagihan")).createAlias("itemBiayaSekolah", "itemBiayaSekolah")
	                    .addOrder(Order.asc("itemBiayaSekolah.nama")).createAlias("pembayaranSiswa", "pembayaranSiswa");

	            if (siswa != null && calonSiswa != null) {
	                pembayaransCriteria.add(Restrictions.or(Restrictions.eq("pembayaranSiswa.siswa", siswa),
	                        Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa)));
	            } else if (siswa != null) {
	                pembayaransCriteria.add(Restrictions.eq("pembayaranSiswa.siswa", siswa));
	            } else if (calonSiswa != null) {
	                pembayaransCriteria.add(Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa));
	            } else {
	                pembayaransCriteria.add(Restrictions.sqlRestriction("false"));
	            }

	            List<PembayaranSiswaDetail> pembayarans = pembayaransCriteria.list();

	            Map<PengaturanBiaya, List<Tagihan>> mapsTag = new HashMap<PengaturanBiaya, List<Tagihan>>();
	            for (PembayaranSiswaDetail pembayaranSiswaDetail : pembayarans) {
	                Tagihan tagihan = pembayaranSiswaDetail.getTagihan();
	                if (tagihan != null && tagihan.getId() != null) {
	                    dibayars.add(tagihan.getId());
	                    List<Tagihan> tagihansD = mapsTag.get(tagihan.getPengaturanBiaya());
	                    if (tagihansD == null) {
	                        tagihansD = new ArrayList<Tagihan>();
	                        mapsTag.put(tagihan.getPengaturanBiaya(), tagihansD);
	                    }
	                    tagihansD.add(tagihan);
	                }
	            }

	            for (Map.Entry<PengaturanBiaya, List<Tagihan>> entry : mapsTag.entrySet()) {
	                PengaturanBiaya pengaturanBiaya = entry.getKey();
	                List<Tagihan> tagihans = entry.getValue();
	                StringBuilder biayaStr = new StringBuilder();
	                for (Tagihan tagihan : tagihans) {
	                    if (tagihan.getAktif() && !tagihan.ambilBukanTagihanData() && tagihan.getNominalBiaya() != null
	                            && !tagihan.getNominalBiaya().getBukanTagihan()) {
	                        String s = tagihan.getPembayaranSiswaDetail() != null
	                                ? "<span style='color:#16a34a;'>" + htmlEscape(tagihan.getItemBiayaSekolah().getNama())
	                                    + " &#10003; telah dibayar " + Common.numberFormat.get().format(tagihan.getNominal()) + "</span>"
	                                : "<span style='color:#dc2626;'>" + htmlEscape(tagihan.getItemBiayaSekolah().getNama())
	                                    + " belum dibayar " + Common.numberFormat.get().format(tagihan.getNominal()) + "</span>";
	                        if (biayaStr.length() > 0) biayaStr.append("<br>");
	                        biayaStr.append(s);
	                    }
	                }
	                if (biayaStr.length() > 0) {
	                    htmlBuilder.append(buildInfoRow(
	                        htmlEscape(pengaturanBiaya.getJenisBiayaSekolah().getNama()),
	                        biayaStr.toString()));
	                }
	            }

	            Integer bulan = (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);
	            Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);

	            List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(PengaturanBiaya
	                    .terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), siswa, calonSiswa)
	                    .addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
	                    .addOrder(Order.asc("jenisBiayaSekolah.nama")), PengaturanBiaya.class);

	            final List<Tagihan> belumDibayars = new ArrayList<Tagihan>();
	            for (PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
	                JenisBiayaSekolah jenisBiaya = pengaturanBiaya.getJenisBiayaSekolah();
	                if (pengaturanBiaya.getAktif() && jenisBiaya != null
	                        && ((siswa != null && !jenisBiaya.getGunakanCalonSiswa()
	                                && DetailTagihanSiswaHelper.apakahAda(pengaturanBiaya, siswa))
	                                || (calonSiswa != null && jenisBiaya.getGunakanCalonSiswa()
	                                        && DetailTagihanCalonSiswaHelper.apakahAda(pengaturanBiaya, calonSiswa)))) {

	                    List<Tagihan> tagihans = jenisBiaya.getGunakanCalonSiswa()
	                            ? TagihanUtilCalonSiswa.getTagihan(jenisBiaya, pengaturanBiaya, calonSiswa, bulan,
	                                    tahun, false)
	                            : TagihanUtil.getTagihan(jenisBiaya, pengaturanBiaya, siswa, bulan, tahun, false);

	                    StringBuilder biayaStr = new StringBuilder();
	                    if (tagihans != null) {
	                        for (Tagihan tagihan : tagihans) {
	                            // Cek tagihan yang belum dibayar dan valid
	                            if (!dibayars.contains(tagihan.getId()) && tagihan.getAktif()
	                                    && !tagihan.ambilBukanTagihanData() && tagihan.getNominalBiaya() != null
	                                    && !tagihan.getNominalBiaya().getBukanTagihan()) {
	                                String s = tagihan.getPembayaranSiswaDetail() != null
	                                        ? "<span style='color:#16a34a;'>" + htmlEscape(tagihan.getItemBiayaSekolah().getNama())
	                                            + " &#10003; telah dibayar " + Common.numberFormat.get().format(tagihan.getNominal()) + "</span>"
	                                        : "<span style='color:#dc2626;'>" + htmlEscape(tagihan.getItemBiayaSekolah().getNama())
	                                            + " belum dibayar " + Common.numberFormat.get().format(tagihan.getNominal()) + "</span>";
	                                if (tagihan.getPembayaranSiswaDetail() == null && tagihan.getNominal() > 0.1) {
	                                    belumDibayars.add(tagihan);
	                                }
	                                if (biayaStr.length() > 0) biayaStr.append("<br>");
	                                biayaStr.append(s);
	                            }
	                        }
	                    }
	                    if (biayaStr.length() > 0) {
	                        htmlBuilder.append(buildInfoRow(htmlEscape(jenisBiaya.getNama()), biayaStr.toString()));
	                    }
	                }
	            }

	            htmlBuilder.append("</table>");

	            // === PROGRESS TRACKER HTML/CSS — 4 langkah: Daftar-Bayar-Verifikasi-Hasil ===
	            boolean sudahBayarPrg = !mapsTag.isEmpty();
	            boolean sudahVerifPrg = calonSiswa.getTerverifikasi() != null && calonSiswa.getTerverifikasi();
	            boolean sudahHasilPrg = (calonSiswa.getTelahDiterima() != null && calonSiswa.getTelahDiterima())
	                    || (calonSiswa.getDitolak() != null && calonSiswa.getDitolak())
	                    || (calonSiswa.getMengundurkanDiri() != null && calonSiswa.getMengundurkanDiri());
	            htmlBuilder.append(buildProgressTrackerPsb(true, sudahBayarPrg, sudahVerifPrg, sudahHasilPrg));
	            htmlBuilder.append("</div>"); // tutup div wrapper font-family
	            groupboxStyled.appendChild(new Html(htmlBuilder.toString()));

	            MyToolbarbutton logout = new MyToolbarbutton("fa-sign-out", "Logout");
	            logout.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event arg0) throws Exception {
	                    Common.setLogoutCalonSiswa();
	                    Common.createDefaultTimer(new EventListener() {
	                        @Override
	                        public void onEvent(Event arg0) throws Exception {
	                            Executions.getCurrent().sendRedirect("");
	                        }
	                    });
	                }
	            });

	            MyToolbarbutton ujian = new MyToolbarbutton("fa-pencil-square", "Ikut Ujian Sekarang");
	            ujian.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event event) throws Exception {
	                    if (!GelombangPendaftaranPsb.chekSyaratBayar(calonSiswa)) return;
	                    if (!VerifikasiKelengkapanCalonSiswa.checkBerkasSebelumUjian(calonSiswa)) return;
	                    if (!VerifikasiKelengkapanCalonSiswa.checkBerkas(calonSiswa)) return;

	                    TampilanUjianCalonSiswa tampilanUjianCalonSiswa = new TampilanUjianCalonSiswa();
	                    tampilanUjianCalonSiswa.init(calonSiswa);
	                    tampilanUjianCalonSiswa.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
	                    tampilanUjianCalonSiswa.setHeight("100%");
	                    tampilanUjianCalonSiswa.setWidth("90%");
	                    tampilanUjianCalonSiswa.onModal();
	                }
	            });

	            MyToolbarbutton reg = new MyToolbarbutton("fa-print", "No.Reg");
	            reg.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event event) throws Exception {
	                    CalonSiswaAction.onCetakKartu(calonSiswa, false);
	                }
	            });

	            MyToolbarbutton bio = new MyToolbarbutton("fa-user-circle", "Biodata");
	            bio.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event event) throws Exception {
	                    if (myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getJenisBiayaSekolah() != null) {
	                        StringBuilder tagStr = new StringBuilder();
	                        for (Tagihan tagihan : belumDibayars) {
	                            if (tagihan.getAktif() && !tagihan.ambilBukanTagihanData()
	                                    && tagihan.getNominalBiaya() != null
	                                    && !tagihan.getNominalBiaya().getBukanTagihan()
	                                    && tagihan.getPembayaranSiswaDetail() == null) {
	                                if (tagStr.length() > 0)
	                                    tagStr.append("; ");
	                                tagStr.append(tagihan.getItemBiayaSekolah().getNama()).append(" ")
	                                        .append(Common.numberFormat.get().format(tagihan.getNominal()));
	                            }
	                        }
	                        if (tagStr.length() > 0) {
	                            MyMessageboxConfig.showFormat(
	                            "Mohon maaf, saat ini masih terdapat tagihan yang harus Bapak/Ibu selesaikan terlebih dahulu, dengan rincian sebagai berikut: {V1}. Langkah yang dapat dilakukan: (1) lakukan pelunasan atas seluruh tagihan tersebut; (2) tunggu proses verifikasi pembayaran; (3) setelah pembayaran terverifikasi, ulangi kembali proses pencetakan ini.",
	                                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, tagStr.toString());
	                            return;
	                        }
	                    }
	                    CommonReportPsb.onCetakCalonSiswa(calonSiswa);
	                }
	            });

	            MyToolbarbutton kartuUjian = new MyToolbarbutton("fa-pencil-square", "Kartu Ujian");
	            kartuUjian.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event event) throws Exception {
	                    if (calonSiswa.getNoUjian() != null && !calonSiswa.getNoUjian().trim().isEmpty()) {
	                        CommonReportPsb.onCetakKartuUjianPSB(calonSiswa, calonSiswa.getNoUjian());
	                        return;
	                    }
	                    String noUjianGenerated = CommonPSB.generateNoUjian(calonSiswa);
	                    if (noUjianGenerated != null && !noUjianGenerated.trim().isEmpty()) {
	                        CommonReportPsb.onCetakKartuUjianPSB(calonSiswa, noUjianGenerated);
	                    }
	                }
	            });

	            MyToolbarbutton ketDiterima = new MyToolbarbutton("fa-check-square-o", "Bukti Diterima");
	            ketDiterima.setVisible(calonSiswa.getTelahDiterima() != null && calonSiswa.getTelahDiterima());
	            ketDiterima.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event arg0) throws Exception {
	                    if (myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getJenisBiayaSekolah() != null) {
	                        StringBuilder tagStr = new StringBuilder();
	                        for (Tagihan tagihan : belumDibayars) {
	                            if (tagihan.getAktif() && !tagihan.ambilBukanTagihanData()
	                                    && tagihan.getNominalBiaya() != null
	                                    && !tagihan.getNominalBiaya().getBukanTagihan()
	                                    && tagihan.getPembayaranSiswaDetail() == null) {
	                                if (tagStr.length() > 0)
	                                    tagStr.append("; ");
	                                tagStr.append(tagihan.getItemBiayaSekolah().getNama()).append(" ")
	                                        .append(Common.numberFormat.get().format(tagihan.getNominal()));
	                            }
	                        }
	                        if (tagStr.length() > 0) {
	                            MyMessageboxConfig.showFormat(
	                            "Mohon maaf, saat ini masih terdapat tagihan yang harus Bapak/Ibu selesaikan terlebih dahulu, dengan rincian sebagai berikut: {V1}. Langkah yang dapat dilakukan: (1) lakukan pelunasan atas seluruh tagihan tersebut; (2) tunggu proses verifikasi pembayaran; (3) setelah pembayaran terverifikasi, ulangi kembali proses pencetakan ini.",
	                                    "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, tagStr.toString());
	                            return;
	                        }
	                    }
	                    CommonReportHelper.onCetakSuratKeteranganLulus(calonSiswa);
	                }
	            });

	            List<MyToolbarbutton> awesomes = new ArrayList<MyToolbarbutton>();
	            if (refresh.isVisible()) awesomes.add(refresh);
	            if (alur.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilAlur()) awesomes.add(alur);
	            if (pembayaranViaPaymentGateway.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilPembayaranViaPaymentGateway()) awesomes.add(pembayaranViaPaymentGateway);
	            if (loginCalonMhs.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilLengkapiBerkas()) awesomes.add(loginCalonMhs);
	            if (informasiKelulusan.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilInformasiKelulusan()) awesomes.add(informasiKelulusan);
	            if (ujian.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilUjian()) awesomes.add(ujian);
	            if (reg.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilCetakNoReg()) awesomes.add(reg);
	            if (bio.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilCetakBiodata()) awesomes.add(bio);
	            if (kartuUjian.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilCetakKartuUjian()) awesomes.add(kartuUjian);
	            if (ketDiterima.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilKeteranganDiterima()) awesomes.add(ketDiterima);
	            if (logout.isVisible() && myGelombangPendaftaranPsb != null && myGelombangPendaftaranPsb.getTampilLogout()) awesomes.add(logout);

	            if (mobile) {
	                // Mobile: tombol vertikal bertumpuk
	                Vbox vboxMobile = new Vbox();
	                vboxMobile.setStyle("width:100%;padding:4px;");
	                vboxMobile.setParent(groupboxStyled1);
	                for (MyToolbarbutton awesome : awesomes) {
	                    new Space().setParent(vboxMobile);
	                    vboxMobile.appendChild(awesome);
	                }
	            } else {
	                // Desktop: tombol rapat di tengah dengan flex wrap
	                Div btnDiv = new Div();
	                btnDiv.setStyle("display:flex;flex-wrap:wrap;justify-content:center;"
	                    + "align-items:center;gap:8px 14px;padding:8px 12px;");
	                btnDiv.setParent(groupboxStyled1);
	                for (MyToolbarbutton awesome : awesomes) {
	                    btnDiv.appendChild(awesome);
	                }
	            }

	            int lain = 0;
	            if (mobile) {
	                Criteria criteria = TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi).setMaxResults(20);
	                List<PengumumanAkademis> pengumumanAkademisLain = ConstantValues.simpleList(criteria, PengumumanAkademis.class);
	                int size = pengumumanAkademisLain.size();
	                if (size > 0) lain = (size * 20) + 60;
	                PengumumanAkademisAction.tampilPengumumanLain(rows, null, pengumumanAkademisLain);
	            }

	            componentdata.setHeight(mobile ? "" + (1000 + lain) + "px" : "490px");

	        } else {
	            Grid grid = new Grid();
	            grid.setSclass("dgrid");
	            grid.setOddRowSclass("non-odd");
	            grid.setStyle("border:0px;background: transparent;");

	            Rows rows = new Rows();
	            rows.setParent(grid);

	            int lain = 0;
	            if (mobile) {
	                Criteria criteria = TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi).setMaxResults(20);
	                List<PengumumanAkademis> pengumumanAkademisLain = ConstantValues.simpleList(criteria, PengumumanAkademis.class);
	                int size = pengumumanAkademisLain.size();
	                if (size > 0) lain = (size * 20) + 60;
	                PengumumanAkademisAction.tampilPengumumanLain(rows, null, pengumumanAkademisLain);
	            }

	            MyFormRow row = new MyFormRow();
	            row.setValign("top");
	            row.setParent(rows);

	            Box hboxCari = mobile ? new Vbox() : new Hbox();
	            hboxCari.setWidth("95%");
	            hboxCari.setPack("center");
	            hboxCari.setAlign("center");
	            hboxCari.setParent(row);
	            hboxCari.appendChild(new MyLabelConfig("Tahun Pelajaran"));

	            final Combobox comboboxTa = new Combobox();
	            Map<Long, GeneralValueObject> gelombangsAktif = ConstantValues.ambilBerdasarClass(GelombangPendaftaranPsb.class);
	            TreeSet<String> tas = new TreeSet<String>();
	            
	            Date sekarang = WaktuUtil.getDate();
	            // OPTIMASI: Format string di-cache di luar loop untuk mencegah pemborosan eksekusi Date formatter (GC pause issue)
	            String strSekarang = Common.dateFormat8.get().format(sekarang);
	            
	            List<GelombangPendaftaranPsb> gelombangPendaftaranPsbsData = new ArrayList<GelombangPendaftaranPsb>();
	            List<GelombangPendaftaranPsb> gelombangPendaftaranPsbs = new ArrayList<GelombangPendaftaranPsb>();
	            List<KelompokGelombang> kelompokGelombangs = new ArrayList<KelompokGelombang>();

	            Sekolah sekolah = SekolahUtil.getSekolah();
	            Yayasan yayasan = SekolahUtil.getYayasan();
	            Tbmuser tbmuser = Common.getCurrentUser();

	            if (gelombangsAktif != null) {
	                // OPTIMASI: Iterasi langsung dari values() untuk menghilangkan lookup (ConstantValues.ambil) O(N) ke database/cache
	                for (GeneralValueObject gvo : gelombangsAktif.values()) {
	                    GelombangPendaftaranPsb gelombangPendaftaranPsb1 = (GelombangPendaftaranPsb) gvo;
	                    
	                    if (gelombangPendaftaranPsb1 != null && gelombangPendaftaranPsb1.getAktif()
	                            && gelombangPendaftaranPsb1.getMulai() != null
	                            && gelombangPendaftaranPsb1.getSampai() != null) {
	                        
	                        String strMulai = Common.dateFormat8.get().format(gelombangPendaftaranPsb1.getMulai());
	                        String strSampai = Common.dateFormat8.get().format(gelombangPendaftaranPsb1.getSampai());

	                        if ((gelombangPendaftaranPsb1.getMulai().before(sekarang) || strMulai.equals(strSekarang))
	                                && (gelombangPendaftaranPsb1.getSampai().after(sekarang) || strSampai.equals(strSekarang))) {

	                            if (yayasan == null || yayasan.getId() == null || (gelombangPendaftaranPsb1.getYayasan() != null
	                                    && yayasan.getId().equals(gelombangPendaftaranPsb1.getYayasan().getId()))) {
	                                if (sekolah == null || sekolah.getId() == null
	                                        || (gelombangPendaftaranPsb1.getSekolah() != null
	                                                && sekolah.getId().equals(gelombangPendaftaranPsb1.getSekolah().getId()))) {
	                                    if (((tbmuser == null || tbmuser.getPegawai() == null)
	                                            && !gelombangPendaftaranPsb1.getHanyaUntukAnakPegawai())
	                                            || (tbmuser != null && tbmuser.getPegawai() != null
	                                                    && gelombangPendaftaranPsb1.getHanyaUntukAnakPegawai())) {

	                                        if (gelombangPendaftaranPsb1.getTahunAjaran() != null) {
	                                            tas.add(gelombangPendaftaranPsb1.getTahunAjaran());
	                                        }

	                                        KelompokGelombang kelGel = gelombangPendaftaranPsb1.getKelompokGelombang();
	                                        if (kelGel != null) {
	                                            if (kelGel.gelombangPendaftaranPsbs == null)
	                                                kelGel.gelombangPendaftaranPsbs = new ArrayList<GelombangPendaftaranPsb>();

	                                            if (!kelGel.gelombangPendaftaranPsbs.contains(gelombangPendaftaranPsb1)) {
	                                                kelGel.gelombangPendaftaranPsbs.add(gelombangPendaftaranPsb1);
	                                            }
	                                            if (!kelompokGelombangs.contains(kelGel)) {
	                                                kelompokGelombangs.add(kelGel);
	                                            }
	                                        } else {
	                                            gelombangPendaftaranPsbs.add(gelombangPendaftaranPsb1);
	                                            gelombangPendaftaranPsbsData.add(gelombangPendaftaranPsb1);
	                                        }
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }

	            Collections.sort(gelombangPendaftaranPsbsData);
	            Collections.sort(gelombangPendaftaranPsbs);
	            Collections.sort(kelompokGelombangs);

	            for (String taa : tas) {
	                Comboitem comboitem = new Comboitem(taa);
	                comboitem.setValue(taa);
	                comboboxTa.appendChild(comboitem);
	            }

	            Comboitem comboitemAll = new Comboitem("Semua");
	            comboitemAll.setValue(null);
	            comboboxTa.appendChild(comboitemAll);

	            hboxCari.appendChild(comboboxTa);
	            Common.selectComboItem(true, comboboxTa, ta);
	            comboboxTa.setCols(6);
	            comboboxTa.setReadonly(true);

	            if (!gelombangPendaftaranPsbs.isEmpty()) {
	                hboxCari.appendChild(new MyLabelConfig("Gelombang"));
	            }

	            gelombangPendaftaranPsbsData.add(null);
	            final Combobox comboboxGelombang = new Combobox();
	            comboboxGelombang.setVisible(!gelombangPendaftaranPsbs.isEmpty());
	            hboxCari.appendChild(comboboxGelombang);
	            comboboxGelombang.setCols(8);
	            Common.insertComboItems(comboboxGelombang, "nama", gelombangPendaftaranPsbsData);
	            Common.selectComboItem(true, comboboxGelombang, gelombangPendaftaranPsb);
	            comboboxGelombang.setReadonly(true);

	            EventListener eventListener2 = new EventListener() {
	                @Override
	                public void onEvent(Event arg0) throws Exception {
	                    String t = (String) (comboboxTa.getSelectedItem() == null ? null
	                            : comboboxTa.getSelectedItem().getValue());
	                    GelombangPendaftaranPsb g = (GelombangPendaftaranPsb) (comboboxGelombang
	                            .getSelectedItem() == null ? null : comboboxGelombang.getSelectedItem().getValue());
	                    Sessions.getCurrent(true).setAttribute("t", t);
	                    Sessions.getCurrent(true).setAttribute("gelombangPendaftaranPsb", g);
	                    Executions.getCurrent().sendRedirect("");
	                }
	            };
	            comboboxGelombang.addEventListener("onChange", eventListener2);
	            comboboxTa.addEventListener("onChange", eventListener2);

	            row = new MyFormRow();
	            row.setParent(rows);

	            Box hbox = mobile ? new Vbox() : new Hbox();
	            hbox.setPack("center");
	            hbox.setAlign("center");
	            hbox.setParent(row);
	            hbox.setWidth("100%");
	            int jml = 0;

	            grid.setParent(componentdata);

	            if (!kelompokGelombangs.isEmpty()) {
	                for (final KelompokGelombang kelompokGelombang : kelompokGelombangs) {
	                    if (!mobile) {
	                        if (jml > 0 && jml % 3 == 0) {
	                            row = new MyFormRow();
	                            row.setParent(rows);
	                            hbox = new Hbox();
	                            hbox.setPack("center");
	                            hbox.setAlign("center");
	                            hbox.setWidth("100%");
	                            hbox.setParent(row);
	                        }
	                    }
	                    jml++;

	                    Date mulaiKelompok = null;
	                    Date sampaiKelompok = null;

	                    if (kelompokGelombang.gelombangPendaftaranPsbs != null) {
	                        Collections.sort(kelompokGelombang.gelombangPendaftaranPsbs);
	                        for (GelombangPendaftaranPsb gPKelompok : kelompokGelombang.gelombangPendaftaranPsbs) {
	                            if (gPKelompok.getMulai() != null
	                                    && (mulaiKelompok == null || mulaiKelompok.after(gPKelompok.getMulai())))
	                                mulaiKelompok = gPKelompok.getMulai();
	                            if (gPKelompok.getSampai() != null
	                                    && (sampaiKelompok == null || sampaiKelompok.before(gPKelompok.getSampai())))
	                                sampaiKelompok = gPKelompok.getSampai();
	                        }
	                    }

	                    Groupbox groupboxKel = new Groupbox();
	                    groupboxKel.setStyle(
	                            "border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");
	                    hbox.appendChild(groupboxKel);
	                    groupboxKel.appendChild(new MyCaptionStyled(kelompokGelombang.getNama()));

	                    LampiranLain lampiranLain = LampiranLain.ambil(kelompokGelombang.getId(), "INFO_KELOMPOK_PPDB");
	                    String linkInfo = null;
	                    if (lampiranLain != null && lampiranLain.getId() != null) {
	                        try {
	                            linkInfo = lampiranLain.createLinkUri();
	                        } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	                    }

	                    String infoKel = kelompokGelombang.getInfo() == null ? "" : kelompokGelombang.getInfo();
	                    StringBuilder htmlKel = new StringBuilder();
	                    htmlKel.append("<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
	                            .append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Pendaftaran")).append("</td><td>")
	                            .append(mulaiKelompok == null ? "" : Common.dateFormat1.get().format(mulaiKelompok))
	                            .append(" sd ")
	                            .append(sampaiKelompok == null ? "" : Common.dateFormat1.get().format(sampaiKelompok))
	                            .append("</td></tr>").append("<tr valign=\"top\"><td>")
	                            .append(Common.getBahasaConfig("Info")).append("</td><td>").append(infoKel)
	                            .append(linkInfo == null || linkInfo.trim().isEmpty() ? ""
	                                    : "<a href='#' onClick=\"popupCenter({url: '" + linkInfo
	                                            + "', title: 'PPDB Info', w: 1200, h: 600});\">"
	                                            + (infoKel.isEmpty() ? "" : ", ")
	                                            + "informasi lebih lanjut klik ini..</a>")
	                            .append("</td></tr>");

	                    if (kelompokGelombang.getKeterangan() != null && !kelompokGelombang.getKeterangan().isEmpty()) {
	                        htmlKel.append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Keterangan"))
	                                .append("</td><td>").append(kelompokGelombang.getKeterangan()).append("</td></tr>");
	                    }
	                    htmlKel.append("</table>");
	                    groupboxKel.appendChild(new Html(htmlKel.toString()));

	                    Hbox hbox1 = new Hbox();
	                    groupboxKel.appendChild(hbox1);

	                    MyToolbarbutton formulirKel = new MyToolbarbutton("fa-pencil-square-o",
	                            Common.getBahasaConfig("Daftar Sekarang"));
	                    formulirKel.getLabelC().setStyle("font-size:15px");
	                    hbox1.appendChild(formulirKel);
	                    formulirKel.addEventListener("onClick", new EventListener() {
	                        @Override
	                        public void onEvent(Event arg0) throws Exception {
	                            sebagaiKelompok(kelompokGelombang);
	                        }
	                    });
	                }
	            }

	            if (!gelombangPendaftaranPsbs.isEmpty()) {
	                for (final GelombangPendaftaranPsb mGP : gelombangPendaftaranPsbs) {
	                    if (mGP.getKelompokGelombang() == null) {
	                        if ((gelombangPendaftaranPsb == null || (gelombangPendaftaranPsb != null
	                                && gelombangPendaftaranPsb.getId().equals(mGP.getId())))
	                                && (ta == null || ta.isEmpty()
	                                        || (ta != null && ta.equals(mGP.getTahunAjaran())))) {

	                            if (!mobile) {
	                                if (jml > 0 && jml % 3 == 0) {
	                                    row = new MyFormRow();
	                                    row.setParent(rows);
	                                    hbox = new Hbox();
	                                    hbox.setPack("center");
	                                    hbox.setAlign("center");
	                                    hbox.setWidth("100%");
	                                    hbox.setParent(row);
	                                }
	                            }
	                            jml++;

	                            Groupbox groupboxGel = new Groupbox();
	                            groupboxGel.setStyle(
	                                    "border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");
	                            hbox.appendChild(groupboxGel);
	                            groupboxGel.appendChild(new MyCaptionStyled(mGP.getNama()));

	                            LampiranLain lampiranLain = LampiranLain.ambil(mGP.getId(), "INFO_PPDB");
	                            String linkInfo = null;
	                            if (lampiranLain != null && lampiranLain.getId() != null) {
	                                try {
	                                    linkInfo = lampiranLain.createLinkUri();
	                                } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	                            }

	                            String ketGel = mGP.getKeterangan() == null ? "" : mGP.getKeterangan();
	                            StringBuilder htmlGel = new StringBuilder();
	                            htmlGel.append("<table style=\"width:100%;border:0px solid black;padding: 5px 20px 20px 5px;\">")
	                                    .append("<tr valign=\"top\"><td>").append(Common.getBahasaConfig("Pendaftaran")).append("</td><td>")
	                                    .append(mGP.getMulai() == null ? "" : Common.dateFormat1.get().format(mGP.getMulai()))
	                                    .append(" sd ")
	                                    .append(mGP.getSampai() == null ? "" : Common.dateFormat1.get().format(mGP.getSampai()))
	                                    .append("</td></tr>").append("<tr valign=\"top\"><td>")
	                                    .append(Common.getBahasaConfig("Info")).append("</td><td>").append(ketGel)
	                                    .append(linkInfo == null || linkInfo.trim().isEmpty() ? ""
	                                            : "<a href='#' onClick=\"popupCenter({url: '" + linkInfo
	                                                    + "', title: 'PPDB Info', w: 1200, h: 600});\">"
	                                                    + (ketGel.isEmpty() ? "" : ", ")
	                                                    + "informasi lebih lanjut klik ini..</a>")
	                                    .append("</td></tr>").append("<tr valign=\"top\"><td>")
	                                    .append(Common.getBahasaConfig("Periode")).append("</td><td>")
	                                    .append(mGP.getTahunAjaran()).append("</td></tr>").append("</table>");

	                            groupboxGel.appendChild(new Html(htmlGel.toString()));

	                            Hbox hbox1 = new Hbox();
	                            groupboxGel.appendChild(hbox1);

	                            MyToolbarbutton formulirGel = new MyToolbarbutton("fa-pencil-square-o", Common.getBahasaConfig("Daftar Sekarang"));
	                            formulirGel.getLabelC().setStyle("font-size:15px");
	                            hbox1.appendChild(formulirGel);
	                            formulirGel.addEventListener("onClick", new EventListener() {
	                                @Override
	                                public void onEvent(Event arg0) throws Exception {
	                                    CalonSiswa csNew = new CalonSiswa();
	                                    csNew.setGelombangPendaftaranPsb(mGP);

	                                    if (mGP != null && mGP.getHanyaUntukAnakPegawai()) {
	                                        Tbmuser tu = Common.getCurrentUser();
	                                        if (tu != null && tu.getPegawai() != null) {
	                                            csNew.setOrangTuaPegawai(tu.getPegawai());
	                                        }
	                                    }

	                                    EventListener evtPerubahan = new EventListener() {
	                                        @Override
	                                        public void onEvent(Event a1) throws Exception {
	                                            CalonSiswa cs2 = (CalonSiswa) a1.getData();
	                                            Common.masukkanSession(CalonSiswa.class, cs2);
	                                        }
	                                    };

	                                    CalonSiswaAction.onAddExternal(null, new EventListener() {
	                                        @Override
	                                        public void onEvent(Event a2) throws Exception {}
	                                    }, evtPerubahan, csNew, null, null, null, mGP);
	                                }
	                            });

	                            if (mGP.getSekolah() != null && mGP.getSekolah().getWa() != null && !mGP.getSekolah().getWa().trim().isEmpty()) {
	                                hbox1.appendChild(new Space());
	                                MyToolbarbutton whatsapp = new MyToolbarbutton("fa-whatsapp", Common.getBahasaConfig("Tanya"));
	                                whatsapp.getLabelC().setStyle("font-size:15px");
	                                hbox1.appendChild(whatsapp);
	                                whatsapp.addEventListener("onClick", new EventListener() {
	                                    @Override
	                                    public void onEvent(Event arg0) throws Exception {
	                                        String text = mGP.getSekolah().getJawabWhatsappPsb() == null ? "" : mGP.getSekolah().getJawabWhatsappPsb();
	                                        String hp = mGP.getSekolah().getWa();
	                                        if (hp != null && !hp.trim().isEmpty()
	                                                && !(hp.trim().equals("00000000000000000000") || hp.trim().equals("000000000"))) {
	                                            hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
	                                            hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
	                                            hp = !hp.startsWith("+") ? "+62" + hp : hp;
	                                        }
	                                        String linkWa = "https://api.whatsapp.com/send?phone=" + hp + "&text="
	                                                + URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8");
	                                        Executions.getCurrent().sendRedirect(linkWa, "_blank");
	                                    }
	                                });
	                            }
	                        }
	                    }
	                }
	            }

	            componentdata.setHeight(
	                    mobile ? (((jml + 1) * 160) + 190 + lain) + "px" : ((((jml / 3) + 1) * 180) + 50) + "px");

	            if (!mobile) {
	                if (jml > 0 && jml % 3 == 0) {
	                    row = new MyFormRow();
	                    row.setParent(rows);
	                    hbox = new Hbox();
	                    hbox.setPack("center");
	                    hbox.setAlign("center");
	                    hbox.setWidth("100%");
	                    hbox.setParent(row);
	                }
	            }
	            jml++;

	            Groupbox groupboxLogin = new Groupbox();
	            groupboxLogin.setStyle(
	                    "border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;width: 320px;");
	            hbox.appendChild(groupboxLogin);
	            groupboxLogin.appendChild(new MyCaptionStyled("Login"));

	            String bodyTxt = "<table style=\"width:100%;border:0px solid black;padding: 5px 20px 10px 5px;\">"
	                    + "<tr valign=\"top\"><td>"
	                    + Common.getBahasaConfig(
	                            "Jika Anda telah melakukan pendaftaran,<br>pilih login untuk melengkapi data,<br> informasi ujian dan pembayaran,<br>serta informasi kelulusan.")
	                    + "</td></tr></table>";

	            bodyTxt = Common.getKonfigurasi("body_login_psb", bodyTxt).getNilai();
	            groupboxLogin.appendChild(new Html(bodyTxt));

	            MyToolbarbutton btnLogin = new MyToolbarbutton("fa-sign-in", Common.getBahasaConfig("Login Sekarang"));
	            btnLogin.getLabelC().setStyle("font-size:15px");
	            groupboxLogin.appendChild(btnLogin);
	            btnLogin.addEventListener("onClick", new EventListener() {
	                @Override
	                public void onEvent(Event arg0) throws Exception {
	                    Common.displayWindow("/pages/psb/login_calon_siswa.zul", true, "500px",
								Common.isMobile() ? "100%" : "850px", null, "", false);
	                }
	            });
	        }
	    } catch (Exception e) {
	        ais.common.Common.tampilErrorJikaAdmin(e);
	    } finally {
	        // Blok finally dipastikan solid sesuai standar untuk men-destroy koneksi dan session
	        if (session != null) {
	            try { if (session.isOpen()) session.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	            try { session.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	            try { if (session.isOpen()) session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	        }
	    }
	}

}
