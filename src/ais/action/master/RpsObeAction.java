package ais.action.master;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AbsensiHelper;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.PenjadwalanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataDosenBanyak;
import ais.action.master.helper.generic.AmbilDataMatakuliahBanyak;
import ais.action.master.helper.obe.AmbilDataBahanKajianBanyak;
import ais.action.master.helper.obe.AmbilDataCapaianLulusanBanyak;
import ais.action.master.helper.obe.AmbilDataCapaianPembelajaranLulusanBanyak;
import ais.action.master.helper.obe.AmbilDataProfilLulusanBanyak;
import ais.action.master.helper.obe.AmbilDataReferensiLulusanBanyak;
import ais.action.master.obe.BahanKajianAction;
import ais.action.master.obe.CapaianLulusanAction;
import ais.action.master.obe.CapaianPembelajaranLulusanAction;
import ais.action.master.obe.ProfilLulusanAction;
import ais.action.master.obe.ReferensiLulusanAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.database.model.obe.ReferensiLulusan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupConfig;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakBesar;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelConfigAgakBesar;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RpsObeAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;

	private static final String CFG_HAK_AKSES_UBAH_RPS_OBE = "hak_akses_yang_boleh_mengubah_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_MK_OBE = "hak_akses_yang_boleh_mengubah_mk_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_OTORITAS_OBE = "hak_akses_yang_boleh_mengubah_otoritas_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_PL_OBE = "hak_akses_yang_boleh_mengubah_pl_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_CPL_OBE = "hak_akses_yang_boleh_mengubah_cpl_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_CPMK_OBE = "hak_akses_yang_boleh_mengubah_cpmk_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_SUB_CPMK_OBE = "hak_akses_yang_boleh_mengubah_sub_cpmk_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_SUB_CPMK_KORELASI_OBE = "hak_akses_yang_boleh_mengubah_sub_cpmk_korelasi_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_CPL_CPMK_OBE = "hak_akses_yang_boleh_mengubah_cpl_cpmk_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_DESKRIPSI_OBE = "hak_akses_yang_boleh_mengubah_deskripsi_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_RINCI_OBE = "hak_akses_yang_boleh_mengubah_rincian_rps_obe";
	private static final String CFG_HAK_AKSES_UBAH_CATATAN_OBE = "hak_akses_yang_Boleh_mengubah_catatan_obe";
	private static final String DEFAULT_HAK_AKSES_UBAH_OBE = "am,admfak,admprd,Akademik";
	private static final String DEFAULT_HAK_AKSES_UBAH_OBE_DOSEN = "am,admfak,admprd,Akademik,Dosen";
	/**
	 * Konfigurasi tampil/sembunyi tombol "Cetak PDF" pada RPS OBE.
	 * Default tidak tampil; aktifkan dengan mengisi nilai 1/ya/true/aktif pada
	 * konfigurasi {@value}. Cetak (HTML) selalu tersedia.
	 */
	private static final String CFG_TAMPILKAN_CETAK_PDF_RPS_OBE = "tampilkan_tombol_cetak_pdf_rps_obe";
	private boolean editBase = false;
	private boolean deleteBase = false;
	// Akses UPDATE/DELETE pada MENU OBE/CPMK milik pengguna (dihitung sekali di
	// initHakAkses). Dipakai agar pengguna yang DIBERI akses penuh ke menu OBE tetap
	// bisa mengubah RPS OBE meski membukanya dari konteks lain (mis. Agenda), karena
	// getCurrentMenu() di konteks itu bukan menu OBE.
	private boolean updateMenuObe = false;
	private boolean deleteMenuObe = false;

	// === KONSTANTA CSS MODERN UI ===
	private static final String BTN_PRIMARY = "font-size: 12px; font-weight: bold; color: #ffffff; background-color: #3b82f6; border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer; box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3); border: none; margin-right: 5px;";
	private static final String BTN_SUCCESS = "font-size: 12px; font-weight: bold; color: #ffffff; background-color: #10b981; border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer; box-shadow: 0 2px 4px rgba(16, 185, 129, 0.3); border: none; margin-right: 5px;";
	private static final String BTN_DANGER = "font-size: 12px; font-weight: bold; color: #ffffff; background-color: #ef4444; border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer; box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3); border: none; margin-right: 5px;";
	private static final String BTN_ICON = "background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 4px 8px; cursor: pointer; margin-right: 4px; transition: all 0.2s;";
	private static final String GRID_STYLE = "border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); background: #ffffff; margin-top: 5px; margin-bottom: 10px;";
	private static final String TITLE_STYLE = "font-size: 13px; font-weight: bold; color: #1e40af; margin-top: 15px; margin-bottom: 5px; display: block; border-bottom: 2px solid #bfdbfe; padding-bottom: 5px;";
	private static final String BADGE_STYLE = "color: #1e293b; font-size: 12px; font-weight: 600; background: #f1f5f9; padding: 4px 10px; border-radius: 6px; border: 1px solid #cbd5e1; display: inline-block;";

	private AmbilDataMatakuliahBanbox searchMk;
	private Combobox searchKurikulum;
	private MyLabelConfig labelKurikulum;
	/**
	 * Kartu "Pencarian &amp; Filter" (id <code>filterCard</code> di rps_obe.zul). Di-autowire
	 * oleh {@link org.zkoss.zk.ui.util.GenericAutowireComposer}. Disembunyikan otomatis lewat
	 * {@link #sembunyikanFilterCardBilaKosong()} ketika RPS dibuka untuk satu mata kuliah yang
	 * sudah pasti (field Matakuliah &amp; Kurikulum di-detach) sehingga tidak menyisakan kartu
	 * kosong. Bertipe {@link org.zkoss.zk.ui.Component} agar tahan bila sclass/tag div berubah.
	 */
	private org.zkoss.zk.ui.Component filterCard;
	private Rows rowsUtama;
	/** Tabbox RPS OBE (form dipecah per-tab agar tidak perlu scroll panjang). Dibangun ulang tiap onSearchDefault. */
	private org.zkoss.zul.Tabbox tabboxObe;

	/**
	 * Label tab yang diminta ditampilkan saat halaman dibuka sebagai popup (mis. dari layar Penilaian
	 * OBE ketika tombol "Format Nilai" diklik). Diisi di {@link #doAfterCompose(Component)} dari arg
	 * "tabAktif" (arg map {@code Executions.createComponents}). Dipakai SEKALI di onSearchDefault untuk
	 * memilih tab tsb (mis. "CPMK &amp; Sub-CPMK"), lalu dikosongkan agar refresh berikutnya tidak
	 * memaksa tab yang sama. null bila dibuka normal (bukan popup).
	 */
	private String tabAktifLabelDiminta = null;
	/** Grid asli (host rowsUtama dari zul) — disembunyikan, dipakai sebagai anchor penempatan tabbox. Ditangkap sekali. */
	private org.zkoss.zul.Grid gridAsliObe;
	/** Komponen induk tempat tabbox dipasang (kartu data zul). Ditangkap sekali. */
	private org.zkoss.zk.ui.Component anchorObe;

	// Field edit/delete berbasis role/privilege DIHAPUS. Izin ubah/hapus RPS OBE kini HANYA
	// ditentukan oleh KONFIGURASI (Common.getKonfigurasi via bolehMengubahFiturObe per-tab) +
	// tetap menghormati KUNCI kurikulum (terkunci). Cek privilege (checkPrevilages UPDATE/DELETE),
	// privilege menu OBE, dan gate dosen/guru pengampu SENGAJA diabaikan (sesuai permintaan).
	private boolean bolehUbahObe = false;

	private MyDatebox tanggalPenyusunan;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = null;
	private Tbmuser tbmuser;
	private Perkuliahan perkuliahan = null;

	private MyTextbox pengembangRps;
	private MyTextbox koordinator;
	private Matakuliah matakuliah;
	private MyTextbox deskripsiPembelajaran;
	private Row rowUtama;
	private Row rowUtamaKorelasi;
	private MyTextbox mitraPengembang;
	private JSONObject jsonArraykurikulumPunyaMatakuliah;
	private Row rowRinci;
	private MyCkEditor catatan;
	private MyTextbox cplBobotField;
	private MyTextbox pemetaanSoalUtsField;
	private MyTextbox pemetaanSoalUasField;
	private MyTextbox komponenPenilaianField;
	private MyTextbox teknikPerCpmkField;
	private MyTextbox rubrikPenilaianField;
	private MyTextbox analisisDosenField;
	private MyTextbox evaluasiAdminField;
	private MyDoublebox minimalKetercapaian;
	private MyCheckboxConfig tampilkanHanyaYgAktif;
	private MyCheckboxConfig nilaiMenggunakanCpmk;
	private Map<Integer, Pertemuan> pertemuansData = new HashMap<Integer, Pertemuan>();

	// HELPER MEMORY & CODE EFFICIENCY: Mencegah perulangan duplikat untuk parsing
	// ID
	private static Set<Long> parseIdsToSet(String commaSeparatedIds) {
		Set<Long> longs = new HashSet<Long>();
		if (commaSeparatedIds != null && !commaSeparatedIds.trim().isEmpty()) {
			String[] ids = commaSeparatedIds.split(",");
			for (int i = 0; i < ids.length; i++) {
				String d = ids[i].trim();
				// Format composite key "idPrimary|idSekunder" — ambil bagian sebelum '|' saja
				if (d.contains("|")) {
					d = d.substring(0, d.indexOf("|")).trim();
				}
				if (!d.isEmpty()) {
					try {
						longs.add(Long.valueOf(d));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:230");
						// format tidak dikenal, lewati
					}
				}
			}
		}
		return longs;
	}

	// HELPER: Menambahkan ID ke string berpemisah koma
	private static String appendIdToString(String currentStr, Long newId) {
		if (currentStr == null || currentStr.trim().isEmpty()) {
			return String.valueOf(newId);
		}
		return currentStr + "," + newId;
	}

	private static void closeSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			try {
				session.clear();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:254");
			}
			try {
				if (session.isConnected()) {
					session.disconnect();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:260");
			}
			try {
				if (session.isOpen()) {
					session.close();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:266");
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:268");
		}
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	/** Item daftar dengan tombol hapus selalu rata kanan (tanpa lampiran). */
	private static void buatRowItemHapus(Vbox parent, String label,
			boolean canDelete, EventListener hapusListener) {
		Hbox row = new Hbox();
		row.setWidth("100%");
		row.setAlign("center");
		row.setStyle("border-bottom:1px solid #f1f5f9; padding:4px 4px;");
		parent.appendChild(row);
		Label lbl = new Label(label);
		lbl.setHflex("1");
		lbl.setStyle("font-size:13px; color:#374151; word-break:break-word;");
		row.appendChild(lbl);
		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		btn.setStyle(BTN_ICON);
		btn.setTooltiptext(Common.getBahasaConfig("Hapus Data"));
		btn.setVisible(canDelete);
		btn.addEventListener("onClick", hapusListener);
		row.appendChild(btn);
	}

	/** Item daftar referensi: 2-baris card (nama+hapus | lampiran indented). */
	private static void buatRowRefHapus(Vbox parent, int nomor,
			ReferensiLulusan ref, boolean canDelete, EventListener hapusListener, boolean editMode) {
		Vbox card = new Vbox();
		card.setWidth("100%");
		card.setStyle("border-bottom:1px solid #e2e8f0; padding:6px 4px 8px 4px;");
		parent.appendChild(card);

		// Baris 1: nomor+nama (hflex=1) | tombol hapus rata kanan
		Hbox topRow = new Hbox();
		topRow.setWidth("100%");
		topRow.setAlign("center");
		card.appendChild(topRow);
		Label lbl = new Label(nomor + ". " + ref.getNama());
		lbl.setHflex("1");
		lbl.setStyle("font-size:13px; color:#374151; word-break:break-word; line-height:1.4;");
		topRow.appendChild(lbl);
		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		btn.setStyle(BTN_ICON);
		btn.setTooltiptext(Common.getBahasaConfig("Hapus Data"));
		btn.setVisible(canDelete);
		btn.addEventListener("onClick", hapusListener);
		topRow.appendChild(btn);

		// Baris 2: widget lampiran indented — visual terhubung ke nama di atas
		Hbox lampRow = new Hbox();
		lampRow.setStyle("padding-left:18px; padding-top:4px;");
		card.appendChild(lampRow);
		LampiranLain.createDownloadUploadFileLain(lampRow, ref.getId(),
				ReferensiLulusan.class.getName(), Common.getBahasaConfig("Lampiran"),
				false, null, null, false, false, false, editMode);
	}

	/**
	 * Bangun daftar referensi (Pustaka Utama + Pendukung) untuk RPS dari CSV id
	 * ReferensiLulusan yang diisi dosen (kpm.getPustaka / kpm.getPustakaPendukung).
	 * Sebelumnya "Daftar Referensi" hanya membaca field bebas 'catatan' sehingga
	 * kosong meski dosen sudah mengisi kartu referensi.
	 */
	private static String daftarReferensiHtml(KurikulumPunyaMatakuliah kpm) {
		if (kpm == null) return "";
		StringBuilder sb = new StringBuilder();
		int[] nomor = new int[] { 0 };
		appendReferensiDariCsv(sb, kpm.getPustaka(), "Pustaka Utama", nomor);
		appendReferensiDariCsv(sb, kpm.getPustakaPendukung(), "Pustaka Pendukung", nomor);
		return sb.toString();
	}

	private static void appendReferensiDariCsv(StringBuilder sb, String csv, String judul, int[] nomor) {
		if (csv == null || csv.trim().isEmpty()) return;
		StringBuilder isi = new StringBuilder();
		String[] ids = csv.split(",");
		for (int i = 0; i < ids.length; i++) {
			String d = ids[i].trim();
			if (d.isEmpty()) continue;
			try {
				Long id = Long.parseLong(d);
				ReferensiLulusan ref = (ReferensiLulusan) ConstantValues.ambil(
						ReferensiLulusan.class.getName(), id);
				if (ref == null) continue;
				String kodeRef = ref.getKode();                                           // "Kode Referensi"
				String namaRef = ref.getNama() != null ? ref.getNama().trim() : "";       // "Nama Referensi" = judul
				String ketRef  = ref.getKeterangan() != null ? ref.getKeterangan().trim() : "";
				if (kodeRef.isEmpty() && namaRef.isEmpty() && ketRef.isEmpty()) continue;
				nomor[0] = nomor[0] + 1;
				isi.append(nomor[0]).append(". ");
				// Show full citation: keterangan is the complete citation text
				String mainText = !ketRef.isEmpty() ? ketRef : namaRef;
				if (!mainText.isEmpty()) {
					isi.append(mainText);
				}
				if (!kodeRef.isEmpty()) {
					isi.append(" <span style='color:#555;font-size:9pt'>[").append(kodeRef).append("]</span>");
				}
				isi.append("<br>");
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:360");
			}
		}
		if (isi.length() > 0) {
			sb.append("<b>").append(judul).append("</b><br>").append(isi.toString());
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}
	private void initHakAkses() {
		boolean punyaHakUpdate = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		boolean punyaHakDelete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		boolean pengajarInternal = isPengajarInternal();
		boolean bolehUbahObeUmum = bolehMengubahFiturObe(CFG_HAK_AKSES_UBAH_RPS_OBE, true);

		// Hak umum menjadi dasar. Setiap bagian RPS OBE akan mengaktifkan ulang
		// edit/delete sesuai konfigurasi khusus method masing-masing sebelum dirender.
		// Hormati akses MENU OBE/CPMK (dihitung sekali). Bila pengguna diberi akses
		// UPDATE/DELETE penuh ke menu OBE oleh admin, ia boleh mengubah RPS OBE.
		updateMenuObe = punyaPrivilageMenuObe(CommonPrivilages.UPDATE);
		deleteMenuObe = punyaPrivilageMenuObe(CommonPrivilages.DELETE);
		editBase = updateMenuObe || (bolehUbahObeUmum && (punyaHakUpdate || pengajarInternal));
		deleteBase = deleteMenuObe || (bolehUbahObeUmum && (punyaHakDelete || pengajarInternal));
		// Izin dasar = KONFIGURASI umum saja (Common.getKonfigurasi CFG_HAK_AKSES_UBAH_RPS_OBE).
		// editBase/deleteBase (yang mengikutkan privilege/menu/pengampu) TIDAK dipakai lagi.
		bolehUbahObe = bolehUbahObeUmum;
	}

	private boolean isPengajarInternal() {
		try {
			return tbmuser != null && (tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isPesertaDidik() {
		try {
			return tbmuser == null || tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
					|| tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getCalonSiswa() != null;
		} catch (Exception e) {
			return true;
		}
	}

	private String getRoleIdUserAktif() {
		try {
			Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
			return hakAkses == null ? "" : safeTrim(hakAkses.getRoleId());
		} catch (Exception e) {
			return "";
		}
	}

	private boolean cocokRoleKonfigurasi(String daftarRole, String roleId) {
		if (daftarRole == null || daftarRole.trim().length() == 0) {
			return false;
		}
		for (String s : daftarRole.split(",")) {
			String r = safeTrim(s);
			if (r.length() == 0) {
				continue;
			}
			if ("*".equals(r) || "semua".equalsIgnoreCase(r) || r.equalsIgnoreCase(roleId)) {
				return true;
			}
		}
		return false;
	}

	private String getDefaultHakAksesFiturObe(String konfigurasiHakAkses) {
		if (CFG_HAK_AKSES_UBAH_DESKRIPSI_OBE.equals(konfigurasiHakAkses)
				|| CFG_HAK_AKSES_UBAH_RINCI_OBE.equals(konfigurasiHakAkses)
				|| CFG_HAK_AKSES_UBAH_CATATAN_OBE.equals(konfigurasiHakAkses)) {
			return DEFAULT_HAK_AKSES_UBAH_OBE_DOSEN;
		}
		return DEFAULT_HAK_AKSES_UBAH_OBE;
	}

	private boolean bolehMengubahFiturObe(String konfigurasiHakAkses, boolean gunakanDefaultUmum) {
		if (isPesertaDidik()) {
			return false;
		}
		String roleId = getRoleIdUserAktif();
		if (roleId.length() == 0 && !isPengajarInternal()) {
			return false;
		}

		String defaultRole = getDefaultHakAksesFiturObe(konfigurasiHakAkses);
		if (gunakanDefaultUmum && !CFG_HAK_AKSES_UBAH_RPS_OBE.equals(konfigurasiHakAkses)
				&& DEFAULT_HAK_AKSES_UBAH_OBE.equals(defaultRole)) {
			try {
				defaultRole = Common.getKonfigurasi(CFG_HAK_AKSES_UBAH_RPS_OBE, DEFAULT_HAK_AKSES_UBAH_OBE).getNilai();
			} catch (Exception e) {
				defaultRole = DEFAULT_HAK_AKSES_UBAH_OBE;
			}
		}

		String daftarRole = null;
		try {
			daftarRole = Common.getKonfigurasi(konfigurasiHakAkses, defaultRole).getNilai();
		} catch (Exception e) {
			daftarRole = defaultRole;
		}
		return cocokRoleKonfigurasi(daftarRole, roleId);
	}

	/**
	 * Apakah pengguna memiliki privilege {@code code} (UPDATE/DELETE) pada salah satu
	 * MENU bertema OBE/CPMK/Kurikulum OBE yang dapat ia akses. Ini menghormati akses
	 * menu yang diberikan administrator: bila admin memberi akses penuh (mis. ke menu
	 * "Capaian Pembelajaran Lulusan pada matakuliah (CPMK)"), pengguna tetap dapat
	 * mengubah RPS OBE meskipun {@code getCurrentMenu()} saat itu bukan menu OBE
	 * (mis. RPS OBE dibuka dari Agenda). Menu dimaterialkan dulu sebelum pengecekan
	 * privilege (yang membuka/menutup sesi tersendiri).
	 */
	private boolean punyaPrivilageMenuObe(Integer code) {
		try {
			Tbmuser u = tbmuser == null ? Common.getCurrentUser() : tbmuser;
			if (u == null || u.hakAkses() == null) {
				return false;
			}
			java.util.List<ais.database.model.Menu> menus = ais.database.hibernate.HibernateUtil.currentSession()
					.createCriteria(ais.database.model.Menu.class).list();
			for (ais.database.model.Menu m : menus) {
				if (m == null) {
					continue;
				}
				String nm = (m.getNama() == null ? "" : m.getNama()).toLowerCase();
				boolean menuObe = nm.indexOf("cpmk") >= 0 || nm.indexOf("capaian pembelajaran") >= 0
						|| nm.indexOf("kurikulum obe") >= 0 || nm.indexOf("rps obe") >= 0;
				if (menuObe && CommonPrivilages.checkPrevilages(m, code, u)) {
					return true;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:500");
		}
		return false;
	}

	private void gunakanHakAksesFiturObe(String konfigurasiHakAkses) {
		boolean terkunci = kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getDikunci() != null;
		boolean punyaHakUpdate = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		boolean punyaHakDelete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		boolean pengajarInternal = isPengajarInternal();
		boolean bolehFitur = !terkunci && bolehMengubahFiturObe(konfigurasiHakAkses, true);
		// Izin per-tab = KONFIGURASI saja (Common.getKonfigurasi via bolehMengubahFiturObe) + tetap
		// menghormati KUNCI. bolehFitur = !terkunci && bolehMengubahFiturObe(CFG, true).
		// Cek privilege (punyaHakUpdate/Delete), privilege menu OBE (updateMenuObe/deleteMenuObe),
		// dan gate dosen/guru pengampu (pengajarInternal) SENGAJA diabaikan.
		bolehUbahObe = bolehFitur;
	}

	private boolean bolehMengubahCatatanObe() {
		boolean terkunci = kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getDikunci() != null;
		// KONFIGURASI saja (Common.getKonfigurasi via bolehMengubahFiturObe) + hormati KUNCI.
		// Cek privilege (punyaHakUpdate) & gate dosen/guru pengampu (pengajarInternal) diabaikan.
		return !terkunci && bolehMengubahFiturObe(CFG_HAK_AKSES_UBAH_CATATAN_OBE, true);
	}

	/**
	 * Apakah tombol "Cetak PDF" boleh tampil. Default: tidak tampil (false).
	 * Aktif bila konfigurasi tampilkan_tombol_cetak_pdf_rps_obe bernilai
	 * 1 / true / ya / aktif. Cetak (HTML) tetap selalu tersedia.
	 */
	private boolean bolehTampilCetakPdf() {
		try {
			String v = Common.getKonfigurasi(CFG_TAMPILKAN_CETAK_PDF_RPS_OBE, "0").getNilai();
			if (v == null) {
				return false;
			}
			v = v.trim();
			return v.equals("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("ya")
					|| v.equalsIgnoreCase("aktif") || v.equalsIgnoreCase("yes");
		} catch (Exception e) {
			return false;
		}
	}



	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		// Tangkap permintaan tab awal (arg "tabAktif") SEKARANG selagi arg-stack createComponents masih
		// menunjuk ke arg popup — sebelum init* memanggil createComponents lain. Dipakai onSearchDefault.
		try {
			if (execution.getArg() != null && execution.getArg().get("tabAktif") != null) {
				tabAktifLabelDiminta = execution.getArg().get("tabAktif").toString();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:558");
		}

		// Sumber konteks: URL parameter (buka normal) ATAU arg map (dibuka sbg popup dari layar
		// Penilaian OBE via Executions.createComponents(zul, window, arg)). getArg dipakai bila
		// getParameter null agar popup Format Nilai OBE bisa mengoper id perkuliahan/mk/kur.
		String pPerkuliahan = execution.getParameter("perkuliahan");
		if (pPerkuliahan == null && execution.getArg() != null && execution.getArg().get("perkuliahan") != null) {
			pPerkuliahan = execution.getArg().get("perkuliahan").toString();
		}
		if (pPerkuliahan != null) {
			perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
					Long.valueOf(pPerkuliahan.trim()));
		}

		Matakuliah mk = null;
		String pMk = execution.getParameter("mk");
		if (pMk == null && execution.getArg() != null && execution.getArg().get("mk") != null) {
			pMk = execution.getArg().get("mk").toString();
		}
		if (pMk != null) {
			mk = (Matakuliah) ConstantValues.ambil(Matakuliah.class.getName(),
					Long.valueOf(pMk.trim()));
		}
		KurikulumPunyaMatakuliah kur = null;
		String pKur = execution.getParameter("kur");
		if (pKur == null && execution.getArg() != null && execution.getArg().get("kur") != null) {
			pKur = execution.getArg().get("kur").toString();
		}
		if (pKur != null) {
			kur = (KurikulumPunyaMatakuliah) ConstantValues.ambil(KurikulumPunyaMatakuliah.class.getName(),
					Long.valueOf(pKur.trim()));
		}

		initHakAkses();

		// Tombol "Cetak PDF" hanya tampil bila diaktifkan lewat konfigurasi
		// (default disembunyikan). Tombol "Cetak (HTML)" tetap selalu tersedia.
		try {
			org.zkoss.zk.ui.Component cetakBtn = comp.getFellowIfAny("cetak");
			if (cetakBtn instanceof org.zkoss.zul.Toolbarbutton) {
				((org.zkoss.zul.Toolbarbutton) cetakBtn).setVisible(bolehTampilCetakPdf());
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:601");
		}

		searchMk.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					Matakuliah mk = (Matakuliah) searchMk.getAttribute("matakuliah");
					List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
							.simpleList(
									session.createCriteria(KurikulumPunyaMatakuliah.class)
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.isNotNull("kurikulum"))
											.add(Restrictions.eq("matakuliah", mk))
											.createAlias("kurikulum", "kurikulum")
											.add(Restrictions.eq("kurikulum.obe", true)),
									KurikulumPunyaMatakuliah.class);

					searchKurikulum.setSelectedIndex(-1);
					searchKurikulum.setReadonly(true);
					Common.clear(searchKurikulum);

					if (kurikulumPunyaMatakuliahs.size() == 1) {
						Common.selectComboItem(true, searchKurikulum, kurikulumPunyaMatakuliahs.get(0));
						searchKurikulum.setDisabled(true);
						labelKurikulum.setVisible(false);
						searchKurikulum.setVisible(false);
						onSearchDefault(null);
					} else {
						searchKurikulum.setDisabled(false);
						labelKurikulum.setVisible(true);
						searchKurikulum.setVisible(true);
					}

					for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
						if (kurikulumPunyaMatakuliah.getKurikulum().apakahObe(
								perkuliahan == null ? null : perkuliahan.getTahunAjaran(),
								perkuliahan == null ? null : perkuliahan.getGanjilGenap())) {
							Comboitem comboitem = new Comboitem(kurikulumPunyaMatakuliah.getKurikulum().getNama() + ", "
									+ Common.getBahasaConfig("Semester") + " : "
									+ kurikulumPunyaMatakuliah.getSemester());
							comboitem.setValue(kurikulumPunyaMatakuliah);
							searchKurikulum.appendChild(comboitem);
						}
					}
				} finally {
					closeSessionQuietly(session);
				}
			}
		});

		if (mk != null) {
			searchMk.setAttribute("matakuliah", mk);
			searchMk.setValue(mk.getNama());
			searchMk.setDisabled(true);
		}

		if (kur != null) {
			Common.selectComboItem(true, searchKurikulum, kur);
			searchKurikulum.setDisabled(true);

			searchMk.setAttribute("matakuliah", kur.getMatakuliah());
			searchMk.setValue(kur.getMatakuliah().getNama());
			searchMk.setDisabled(true);

			// Kurikulum sudah PASTI (RPS dibuka untuk kurikulum tertentu). DETACH label & combo kurikulum
			// dari row — BUKAN sekadar setVisible(false) — agar TIDAK menyisakan combobox kosong yang
			// mengganggu (permintaan: jika disabled, jangan tetap di-appendChild ke row). Nilai kurikulum
			// tetap terbaca onSearchDefault via searchKurikulum.getSelectedItem() (selection bertahan
			// walau komponen sudah di-detach dari DOM). Pakai detach, sebab setValue(searchMk) di atas dapat
			// memicu eventListener yang me-setVisible(true) kembali sehingga combo kosong tetap muncul.
			if (labelKurikulum != null && labelKurikulum.getParent() != null) {
				labelKurikulum.detach();
			}
			if (searchKurikulum != null && searchKurikulum.getParent() != null) {
				searchKurikulum.detach();
			}

			onSearchDefault(null);
		}

		// Filter "Matakuliah *" pada RPS yang dibuka untuk MK tertentu sudah PASTI (di-disabled di
		// atas). Sesuai permintaan: bila disabled, JANGAN sisakan kotak di row — DETACH bandbox
		// Matakuliah beserta labelnya (bukan sekadar setVisible(false), karena setValue di atas dapat
		// memicu listener yang me-setVisible(true) lagi — sama alasannya dengan Kurikulum). Nilai &
		// atribut "matakuliah" tetap terbaca walau komponen sudah dilepas dari DOM.
		if (searchMk != null && searchMk.isDisabled()) {
			Component labelMatakuliah = searchMk.getPreviousSibling();
			if (labelMatakuliah != null && labelMatakuliah.getParent() != null) {
				labelMatakuliah.detach();
			}
			if (searchMk.getParent() != null) {
				searchMk.detach();
			}
		}

		// Bila tidak ada satu pun field filter yang masih tampil (RPS dibuka untuk satu mata
		// kuliah tertentu → Matakuliah & Kurikulum sudah di-detach), sembunyikan kartu filter
		// agar tidak menyisakan kotak "Pencarian & Filter" kosong. Tombol aksi (Refresh, Cetak,
		// Bantuan) TIDAK terpengaruh karena kini berada di header kartu data, bukan di kartu ini.
		sembunyikanFilterCardBilaKosong();
	}

	/**
	 * <h3>Menyembunyikan kartu "Pencarian &amp; Filter" ketika sudah tidak berisi field apa pun</h3>
	 *
	 * <p><b>Tujuan.</b> Halaman RPS OBE dapat dibuka dalam dua konteks berbeda. (1) Konteks
	 * umum: pengguna memilih sendiri Mata Kuliah lalu Kurikulum lewat kartu "Pencarian &amp;
	 * Filter" di bagian atas. (2) Konteks terarah: RPS dibuka langsung untuk satu mata kuliah /
	 * satu {@link ais.database.model.KurikulumPunyaMatakuliah} tertentu (mis. dari daftar mata
	 * kuliah, dari perkuliahan, atau dari Dasbor OBE). Pada konteks kedua, nilai Mata Kuliah dan
	 * Kurikulum sudah pasti sehingga {@link #doAfterCompose(Component)} melepaskan (detach)
	 * bandbox {@code searchMk}, combobox {@code searchKurikulum}, beserta label-nya dari DOM.
	 * Akibatnya kartu filter menjadi cangkang kosong — hanya menampilkan judul tanpa isi — yang
	 * mengganggu tampilan (persis keluhan pengguna: kotak "Pencarian &amp; Filter" kosong di
	 * bagian atas). Method ini menutup celah tersebut dengan menyembunyikan seluruh kartu filter
	 * begitu terdeteksi tidak lagi memuat field yang tampil.</p>
	 *
	 * <p><b>Cara kerja.</b> Sebuah field dianggap "masih tampil" apabila komponennya belum
	 * dilepas dari pohon komponen ({@code getParent() != null}) dan berstatus {@code isVisible()}.
	 * Method memeriksa dua field kunci: bandbox Mata Kuliah ({@code searchMk}) dan combobox
	 * Kurikulum ({@code searchKurikulum}). Jika salah satu masih tampil, kartu filter dibiarkan
	 * terlihat (konteks umum). Jika keduanya sudah tidak tampil, {@link #filterCard} disembunyikan
	 * dengan {@code setVisible(false)} — bukan di-detach — agar struktur DOM tetap utuh dan mudah
	 * ditampilkan kembali seandainya suatu saat diperlukan. Pemeriksaan dibungkus try/catch gaya
	 * Java 1.6 (satu blok {@code catch (Exception)}) sehingga kegagalan tak terduga (mis.
	 * komponen belum ter-autowire pada kondisi tertentu) tidak pernah menggagalkan proses
	 * {@code doAfterCompose} — kartu filter cukup dibiarkan pada kondisi default (tetap tampil),
	 * yang aman.</p>
	 *
	 * <p><b>Alasan desain (reuse &amp; pemeliharaan).</b> Logika dipusatkan dalam satu method
	 * kecil, bukan disebar sebagai potongan kondisi di berbagai cabang {@code doAfterCompose},
	 * sehingga: (a) aturan "sembunyikan bila kosong" cukup diubah di satu tempat bila kebijakan
	 * berubah; (b) dapat dipanggil ulang dari titik lain di masa depan (mis. bila field filter
	 * ditambah/dikurangi secara dinamis) tanpa menduplikasi kode; dan (c) mudah diuji secara
	 * terpisah. Method ini tidak menyentuh basis data sama sekali sehingga tidak ada
	 * {@link org.hibernate.Session} yang perlu dibuka maupun ditutup — konsisten dengan prinsip
	 * hanya membuka session bila benar-benar mengakses data, demi hemat memori dan koneksi.</p>
	 *
	 * <p><b>Efek terhadap fungsi lain.</b> Menyembunyikan kartu filter tidak memengaruhi
	 * pembacaan nilai kurikulum: {@link #onSearchDefault(Event)} membaca kurikulum dari
	 * {@code searchKurikulum.getSelectedItem()}, dan pemilihan tetap tersimpan pada komponen
	 * combobox walau komponennya sudah dilepas/ disembunyikan dari layar. Tombol aksi (Refresh,
	 * Cetak PDF, Cetak HTML, Bantuan) juga tidak terpengaruh karena sejak penataan ulang tata
	 * letak, tombol-tombol tersebut dipindah ke header kartu data (di atas deretan tab), bukan
	 * lagi berada di dalam kartu filter ini.</p>
	 */
	private void sembunyikanFilterCardBilaKosong() {
		if (filterCard == null) {
			return;
		}
		try {
			boolean adaFilterTampil = (searchMk != null && searchMk.getParent() != null && searchMk.isVisible())
					|| (searchKurikulum != null && searchKurikulum.getParent() != null && searchKurikulum.isVisible());
			filterCard.setVisible(adaFilterTampil);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:759");
			// Aman: bila deteksi gagal, biarkan kartu filter pada kondisi default (tetap tampil).
		}
	}

	public void tampilKunci(Component toolbar, final EventListener eventListener) {
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig(Common.getBahasaConfig("Buka"),
				"/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig(Common.getBahasaConfig("Kunci"),
				"/img/Lock-Lock-icon.png");

		bukaKunci.setStyle(BTN_SUCCESS);
		kunci.setStyle(BTN_DANGER);

		if (tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null && kurikulumPunyaMatakuliah != null) {

			toolbar.appendChild(bukaKunci);
			toolbar.appendChild(kunci);

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(Common.getBahasaConfig("Apakah Anda yakin ingin mengunci RPS ini?"),
							Common.getBahasaConfig("Konfirmasi"), MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										kurikulumPunyaMatakuliah.setDikunci(Common.getCurrentUser());
										Common.refreshUpdate(kurikulumPunyaMatakuliah);

										kunci.setVisible(kurikulumPunyaMatakuliah.getDikunci() == null);
										bukaKunci.setVisible(kurikulumPunyaMatakuliah.getDikunci() != null);
										if (kurikulumPunyaMatakuliah.getDikunci() != null) {
											bukaKunci.setLabel(Common.getBahasaConfig("Buka Kunci") + " ("
													+ kurikulumPunyaMatakuliah.getDikunci().getUserNama() + ")");
										}
										Common.createDefaultTimer(eventListener);
									}
								}
							});
				}
			});

			kunci.setVisible(kurikulumPunyaMatakuliah.getDikunci() == null);
			kunci.setDisabled(!Common.getApakahAdminBolehKunci());
			kunci.setParent(toolbar);

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(Common.getBahasaConfig("Apakah Anda yakin ingin membuka kunci RPS ini?"),
							Common.getBahasaConfig("Konfirmasi"), MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										kurikulumPunyaMatakuliah.setDikunci(null);
										Common.refreshUpdate(kurikulumPunyaMatakuliah);

										kunci.setVisible(kurikulumPunyaMatakuliah.getDikunci() == null);
										bukaKunci.setVisible(kurikulumPunyaMatakuliah.getDikunci() != null);
										Common.createDefaultTimer(eventListener);
									}
								}
							});
				}
			});

			bukaKunci.setVisible(kurikulumPunyaMatakuliah.getDikunci() != null);
			if (kurikulumPunyaMatakuliah.getDikunci() != null) {
				bukaKunci.setLabel(Common.getBahasaConfig("Buka Kunci") + " ("
						+ kurikulumPunyaMatakuliah.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((kurikulumPunyaMatakuliah.getDikunci() != null
					&& Common.getCurrentUser().getUserId() != null
					&& !kurikulumPunyaMatakuliah.getDikunci().getUserId().equals(Common.getCurrentUser().getUserId()))
					|| !Common.getApakahAdminBolehKunci());
			bukaKunci.setParent(toolbar);

			Konfigurasi kurikulumPunyaMatakuliahKunci = Common.getKonfigurasi("kunci_nilai_obe_untuk_admin",
					Konfigurasi.TIDAK_AKTIF);
			if (kurikulumPunyaMatakuliahKunci.getNilai().equals(Konfigurasi.AKTIF)) {
				if (Common.getCurrentUser().getRoot() != null && Common.getCurrentUser().getRoot()
						&& Common.getCurrentUser().hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)) {
					bukaKunci.setDisabled(false);
				}
			}
		}
	}

	private EventListener eventListener = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getMatakuliah() != null
					&& kurikulumPunyaMatakuliah.getKurikulum() != null) {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					session.beginTransaction();
						session.refresh(kurikulumPunyaMatakuliah);
						if (tanggalPenyusunan != null) {
							kurikulumPunyaMatakuliah.setTanggalPenyusunan(tanggalPenyusunan.getValue());
						}
						if (pengembangRps != null) {
							kurikulumPunyaMatakuliah.setPengembangRps(safeTrim(pengembangRps.getValue()));
						}
						if (koordinator != null) {
							kurikulumPunyaMatakuliah.setKoordinator(safeTrim(koordinator.getValue()));
						}
						if (deskripsiPembelajaran != null) {
							kurikulumPunyaMatakuliah.setDeskripsiPembelajaran(safeTrim(deskripsiPembelajaran.getValue()));
						}
						if (mitraPengembang != null) {
							kurikulumPunyaMatakuliah.setMitraPengembang(safeTrim(mitraPengembang.getValue()));
						}
						if (catatan != null) {
							kurikulumPunyaMatakuliah.setCatatan(catatan.getValue());
						}
						if (cplBobotField != null) {
							kurikulumPunyaMatakuliah.setCplBobot(safeTrim(cplBobotField.getValue()));
						}
						if (pemetaanSoalUtsField != null) {
							kurikulumPunyaMatakuliah.setPemetaanSoalUts(safeTrim(pemetaanSoalUtsField.getValue()));
						}
						if (pemetaanSoalUasField != null) {
							kurikulumPunyaMatakuliah.setPemetaanSoalUas(safeTrim(pemetaanSoalUasField.getValue()));
						}
						if (komponenPenilaianField != null) {
							kurikulumPunyaMatakuliah.setKomponenPenilaian(safeTrim(komponenPenilaianField.getValue()));
						}
						if (teknikPerCpmkField != null) {
							kurikulumPunyaMatakuliah.setTeknikPerCpmk(safeTrim(teknikPerCpmkField.getValue()));
						}
						if (rubrikPenilaianField != null) {
							kurikulumPunyaMatakuliah.setRubrikPenilaian(safeTrim(rubrikPenilaianField.getValue()));
						}
						if (minimalKetercapaian != null) {
							kurikulumPunyaMatakuliah.setMinimalKetercapaian(minimalKetercapaian.getValue());
						}
						if (nilaiMenggunakanCpmk != null) {
							kurikulumPunyaMatakuliah.setNilaiMenggunakanCpmk(nilaiMenggunakanCpmk.isChecked());
						}
						Common.refreshUpdate(session, kurikulumPunyaMatakuliah);
					session.getTransaction().commit();

					if (arg0 != null && nilaiMenggunakanCpmk != null && arg0.getTarget() == nilaiMenggunakanCpmk) {
						// Rebuild FormatNilai semua Perkuliahan yang memakai kurikulum ini
						// agar kolom penilaian (CPMK / Sub-CPMK) langsung berubah tanpa perlu
						// tekan tombol Format Nilai secara manual.
						Session sess2 = null;
						try {
							sess2 = HibernateUtil.openSession();
							sess2.beginTransaction();
							@SuppressWarnings("unchecked")
							java.util.List<ais.database.model.Perkuliahan> perkuliahanList = sess2
									.createCriteria(ais.database.model.Perkuliahan.class)
									.add(org.hibernate.criterion.Restrictions.eq(
											"kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah))
									.list();
							for (ais.database.model.Perkuliahan p : perkuliahanList) {
								p.belum("format_nilai_baru");
								ais.database.model.PembombotanNilai.setDefaultPembobotan(p, sess2, true);
							}
							sess2.getTransaction().commit();
						} catch (Exception eRebuild) {
							if (sess2 != null && sess2.getTransaction() != null) {
								try { sess2.getTransaction().rollback(); } catch (Exception er) { ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) RpsObeAction:rebuildFormatNilai:rollback"); }
							}
							ais.common.ErrorAuditUtil.record(eRebuild, "auto-audit RpsObeAction rebuildFormatNilai nilaiMenggunakanCpmk");
						} finally {
							if (sess2 != null && sess2.isOpen()) {
								try { sess2.close(); } catch (Exception er) { ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) RpsObeAction:rebuildFormatNilai:close"); }
							}
						}
						onSearchDefault(null);
					}
				} catch (Exception eSave) {
					if (session != null && session.getTransaction() != null) {
						try { session.getTransaction().rollback(); } catch (Exception er) { ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:eventListener"); }
					}
					ais.common.ErrorAuditUtil.record(eSave, "auto-audit RpsObeAction eventListener save");
				} finally {
					closeSessionQuietly(session);
				}
			}
		}
	};

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) throws Exception {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getId() != null) {
				session.refresh(kurikulumPunyaMatakuliah);
			}

			Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("id", kurikulumPunyaMatakuliah.getId());
			Common.insertProperty(KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah, parameters, "", 2);

			Set<Long> longs = parseIdsToSet(kurikulumPunyaMatakuliah.getMatakuliah().getCapaianLulusan());
			List<CapaianLulusan> capaianLulusans = ConstantValues
					.simpleList(
							session.createCriteria(CapaianLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianLulusan.class);

			List<Map> capaianLulusansD = new ArrayList<Map>();
			for (CapaianLulusan capaianLulusan : capaianLulusans) {
				Map map = new HashMap();
				Common.insertProperty(CapaianLulusan.class, capaianLulusan, map, "");
				capaianLulusansD.add(map);
				Common.insertProperty(CapaianLulusan.class, capaianLulusan, parameters,
						"capaianLulusan_" + capaianLulusan.getId());
			}
			parameters.put("capaianLulusans", capaianLulusansD);

			longs = parseIdsToSet(kurikulumPunyaMatakuliah.getMatakuliah().getCapaianPembelajaranLulusan());
			List<CapaianPembelajaranLulusan> capaianPembelajaranLulusans = ConstantValues
					.simpleList(
							session.createCriteria(CapaianPembelajaranLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianPembelajaranLulusan.class);

			List<Map> subCpmkD = new ArrayList<Map>();
			List<Map> capaianPembelajaranLulusansD = new ArrayList<Map>();
			for (CapaianPembelajaranLulusan capaianPembelajaranLulusan : capaianPembelajaranLulusans) {
				Map map = new HashMap();
				Common.insertProperty(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusan, map, "");
				capaianPembelajaranLulusansD.add(map);

				Common.insertProperty(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusan, parameters,
						"capaianPembelajaranLulusan_" + capaianPembelajaranLulusan.getId());

				JSONArray array = new JSONArray(capaianPembelajaranLulusan.getFormula());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					if (jsonObject.isNull("key"))
						continue;

					String kode = !jsonObject.isNull("kode") ? jsonObject.get("kode") + "" : "";
					String nama = !jsonObject.isNull("nama") ? jsonObject.get("nama") + "" : "";
					Double bobot = !jsonObject.isNull("bobot") ? Double.parseDouble(jsonObject.get("bobot") + "") : 0.0;

					Map map1 = new HashMap();
					Iterator<String> iterator = jsonObject.keys();
					while (iterator.hasNext()) {
						try {
							String keyData = iterator.next();
							map1.put(keyData, jsonObject.get(keyData));
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					map1.put("kode", kode);
					map1.put("nama", nama);
					map1.put("bobot", bobot);
					subCpmkD.add(map1);
				}
			}
			parameters.put("subCpmk", subCpmkD);
			parameters.put("capaianPembelajaranLulusans", capaianPembelajaranLulusansD);

			JSONObject jsonArraykurikulumPunyaMatakuliah;
			try {
				jsonArraykurikulumPunyaMatakuliah = new JSONObject(kurikulumPunyaMatakuliah.getRincian());
			} catch (Exception e) {
				jsonArraykurikulumPunyaMatakuliah = new JSONObject();
			}

			List<Map> rincianObe = new ArrayList<Map>();
			TreeMap<Integer, Map> maps = kurikulumPunyaMatakuliah.populateRinci(jsonArraykurikulumPunyaMatakuliah);
			for (Map map : maps.values()) {
				JSONObject jsonObject = (JSONObject) map.get("jsonObject");
				JSONObject subCpmk = (JSONObject) map.get("subCpmk");
				CapaianPembelajaranLulusan capaianPembelajaranLulusanData = (CapaianPembelajaranLulusan) map
						.get("capaianPembelajaranLulusanData");

				Map map1 = new HashMap();
				if (capaianPembelajaranLulusanData != null) {
					Common.insertProperty(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusanData, map1,
							"capaian");
					Common.insertProperty(CapaianPembelajaranLulusan.class, capaianPembelajaranLulusanData, parameters,
							"rincian_capaian_" + capaianPembelajaranLulusanData.getId());
				}

				Iterator<String> iterator = jsonObject.keys();
				while (iterator.hasNext()) {
					try {
						String keyData = iterator.next();
						map1.put(keyData, jsonObject.get(keyData));
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
				iterator = subCpmk.keys();
				while (iterator.hasNext()) {
					try {
						String keyData = iterator.next();
						map1.put(keyData, subCpmk.get(keyData));
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
				rincianObe.add(map1);
			}
			parameters.put("rincianObe", rincianObe);

			longs = parseIdsToSet(kurikulumPunyaMatakuliah.getMatakuliah().getBahanKajian());
			List<Map> bahanKajians = new ArrayList<Map>();
			for (Long idBahan : longs) {
				BahanKajian bahanKajian = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(), idBahan);
				if (bahanKajian != null) {
					Map map = new HashMap();
					Common.insertProperty(BahanKajian.class, bahanKajian, map, "");
					bahanKajians.add(map);
					Common.insertProperty(BahanKajian.class, bahanKajian, parameters,
							"bahanKajian_" + bahanKajian.getId());
				}
			}
			parameters.put("bahanKajians", bahanKajians);
			return parameters;
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings({})
	public static void cetak(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) throws Exception {
		Report.generatePDFReport(Report.PDF, parameter(kurikulumPunyaMatakuliah), "rps_obe",
				kurikulumPunyaMatakuliah.getTanggal_dirubah());
	}

	public void onCetak(Event event) throws Exception {
		if (kurikulumPunyaMatakuliah == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Data Matakuliah dan Kurikulum harus diisi"),
					Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		cetak(kurikulumPunyaMatakuliah);
	}

	public void onCetakHtml(Event event) throws Exception {
		if (kurikulumPunyaMatakuliah == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Data Matakuliah dan Kurikulum harus diisi"),
					Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		tampilkanCetakHtml(kurikulumPunyaMatakuliah, perkuliahan);
	}

	/**
	 * Buka jendela "Cetak RPS OBE (HTML)" untuk satu KurikulumPunyaMatakuliah
	 * berdasarkan id-nya. Dipakai ulang dari Dasbor OBE (ikon cetak per mata
	 * kuliah) sehingga format cetak HTML konsisten dengan tombol "Cetak (HTML)".
	 */
	public static void cetakHtmlByKpmId(Long kpmId) throws Exception {
		if (kpmId == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Data RPS OBE tidak ditemukan."),
					Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) ConstantValues.ambil(
				KurikulumPunyaMatakuliah.class.getName(), kpmId);
		if (kpm == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Data RPS OBE tidak ditemukan."),
					Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		tampilkanCetakHtml(kpm, null);
	}

	/**
	 * Inti cetak HTML: bangun halaman RPS OBE lalu buka di jendela baru.
	 * Dipakai bersama oleh tombol "Cetak (HTML)" dan ikon cetak di Dasbor OBE.
	 */
	private static void tampilkanCetakHtml(KurikulumPunyaMatakuliah kpm, Perkuliahan perkuliahan) throws Exception {
		try {
			String html = buildRpsHtmlPage(kpm, perkuliahan);
			String escaped = html.replace("\\", "\\\\").replace("`", "\\`");
			org.zkoss.zk.ui.util.Clients.evalJavaScript(
				"(function(){" +
				"var w=window.open('','_blank','width=1150,height=860,scrollbars=yes,resizable=yes');" +
				"if(!w){alert('Popup diblokir browser. Izinkan popup untuk situs ini lalu coba lagi.');return;}" +
				"w.document.open('text/html','replace');" +
				"w.document.write(`" + escaped + "`);" +
				"w.document.close();" +
				"})();"
			);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show("Gagal membuat preview: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
					Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String buildRpsHtmlPage(KurikulumPunyaMatakuliah kpm, Perkuliahan perkuliahan) throws Exception {
		Map p = parameter(kpm);

		String mkNama      = rpsHs(p, "matakuliah.nama");
		String mkKode      = rpsHs(p, "matakuliah.kode");
		Object sksRaw      = p.get("matakuliah.sks");
		int totalSks       = 0;
		if (sksRaw != null) { try { totalSks = Integer.parseInt(sksRaw.toString()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1139");} }
		// Gap #6: SKS T:/P: dari field sksDiskusi (teori) & sksPraktek
		Object sksDiskusiRaw = p.get("matakuliah.sksDiskusi");
		Object sksPraktekRaw = p.get("matakuliah.sksPraktek");
		int sksTeori   = 0;
		int sksPraktek = 0;
		if (sksDiskusiRaw != null) { try { sksTeori   = Integer.parseInt(sksDiskusiRaw.toString()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1145");} }
		if (sksPraktekRaw  != null) { try { sksPraktek = Integer.parseInt(sksPraktekRaw.toString());  } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1146");} }
		if (sksTeori == 0 && sksPraktek == 0) sksTeori = totalSks;
		String sksTpStr = ((sksTeori   > 0) ? "T:" + sksTeori   + " " : "")
		                + ((sksPraktek > 0) ? "P:" + sksPraktek          : "");
		sksTpStr = sksTpStr.trim();
		Object semRaw      = p.get("semester");
		String semester    = semRaw != null ? semRaw.toString() : "";
		String tglSusun    = rpsHs(p, "tanggalPenyusunan.formated5");
		String koordinator = rpsHs(p, "koordinator");
		String pengembang  = rpsHs(p, "pengembangRps");
		String mitra       = rpsHs(p, "mitraPengembang");
		String prodi       = rpsHs(p, "kurikulum.jurusan.nama");
		String jenjang     = rpsHs(p, "kurikulum.jurusan.jenjang.nama");
		String fakultas    = rpsHs(p, "kurikulum.jurusan.fakultas.nama");
		String kaprodi     = rpsHs(p, "kurikulum.jurusan.kaprodi.nama");
		String deskripsi   = rpsHs(p, "deskripsiPembelajaran");
		String catatan     = rpsHs(p, "catatan");
		// Gap #1: CPL bobot per MK
		String cplBobot    = rpsHs(p, "cplBobot");
		// Gap #2: pemetaan soal
		String pemetaanSoalUts = rpsHs(p, "pemetaanSoalUts");
		String pemetaanSoalUas = rpsHs(p, "pemetaanSoalUas");
		// Gap #3: komponen penilaian
		String komponenPenilaian = rpsHs(p, "komponenPenilaian");
		String teknikPerCpmk = rpsHs(p, "teknikPerCpmk");
		String rubrikPenilaian = rpsHs(p, "rubrikPenilaian");
		String institusi   = "";
		try { institusi = Common.getKonfigurasi("label_universitas", "").getNilai(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1172");}
		if (institusi == null || institusi.trim().isEmpty()) institusi = prodi;

		List<Map> cplList     = (List<Map>) p.get("capaianLulusans");
		List<Map> cpmkList    = (List<Map>) p.get("capaianPembelajaranLulusans");
		List<Map> subCpmkList = (List<Map>) p.get("subCpmk");
		List<Map> rincianList = (List<Map>) p.get("rincianObe");
		List<Map> bkList      = (List<Map>) p.get("bahanKajians");
		if (cplList     == null) cplList     = new ArrayList<Map>();
		if (cpmkList    == null) cpmkList    = new ArrayList<Map>();
		if (subCpmkList == null) subCpmkList = new ArrayList<Map>();
		if (rincianList == null) rincianList = new ArrayList<Map>();
		if (bkList      == null) bkList      = new ArrayList<Map>();

		String namaDosen = "", tahunAjaran = "", ganjilGenap = "";
		if (perkuliahan != null) {
			/* Perkuliahan tidak punya properti "dosen"; pengampu utama = dosen1. */
			if (perkuliahan.getDosen1() != null) namaDosen = safeTrim(perkuliahan.getDosen1().getNama());
			tahunAjaran = safeTrim(perkuliahan.getTahunAjaran());
			ganjilGenap = safeTrim(perkuliahan.getGanjilGenap());
		}
		String berlakuSejak = !tahunAjaran.isEmpty()
				? tahunAjaran + (!ganjilGenap.isEmpty() ? " – " + ganjilGenap : "")
				: tglSusun;

		StringBuilder sb = new StringBuilder(65536);
		sb.append("<!DOCTYPE html><html lang='id'><head><meta charset='UTF-8'>");
		sb.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");
		sb.append("<title>RPS OBE - ").append(rpsHe(mkNama)).append("</title>");
		sb.append("<style>").append(rpsHtmlCss()).append("</style></head><body>");

		sb.append("<div class='no-print toolbar'>");
		sb.append("<div class='toolbar-left'><span class='toolbar-brand'>&#128220; RPS OBE</span>");
		sb.append("<span class='toolbar-sub'>").append(rpsHe(mkKode));
		if (mkKode.length() > 0 && mkNama.length() > 0) sb.append(" &middot; ");
		sb.append(rpsHe(mkNama)).append("</span></div>");
		sb.append("<div class='toolbar-right'>");
		sb.append("<button onclick='window.print()' class='btn btn-cetak'>&#128438; Cetak / Simpan PDF</button>");
		sb.append("<button onclick='window.close()' class='btn btn-tutup'>&#10005; Tutup</button>");
		sb.append("</div></div>");
		sb.append("<div class='toolbar-hint no-print'>Tampilan ini otomatis menyesuaikan layar HP maupun komputer. Untuk hasil cetak terbaik, pilih orientasi <b>Landscape</b> dan kertas <b>A4</b>.</div>");
		sb.append("<div class='sheet'>");

		// ===== HAL 1: IDENTITAS + OTORISASI =====
		sb.append(rpsSectionIntro("Rencana Pembelajaran Semester (RPS) OBE",
			"Dokumen ringkas berisi identitas mata kuliah, dosen pengampu, target kemampuan (capaian), materi tiap minggu, dan cara penilaian selama satu semester."));
		sb.append(rpsPageHeader(institusi, fakultas, mkKode, berlakuSejak));
		sb.append("<table class='t-main'>");
		sb.append("<tr><td colspan='5' class='td-title'>RENCANA PEMBELAJARAN SEMESTER (RPS)</td></tr>");
		if (!fakultas.isEmpty()) sb.append("<tr><td colspan='5' class='td-subtitle'>").append(rpsHe(fakultas)).append("</td></tr>");
		if (!prodi.isEmpty())    sb.append("<tr><td colspan='5' class='td-subtitle'>PROGRAM STUDI ").append(jenjang.isEmpty() ? "" : rpsHe(jenjang) + " ").append(rpsHe(prodi)).append("</td></tr>");
		if (!tahunAjaran.isEmpty()) sb.append("<tr><td colspan='5' class='td-subtitle'>TAHUN AKADEMIK ").append(rpsHe(tahunAjaran)).append(!ganjilGenap.isEmpty() ? " &ndash; " + rpsHe(ganjilGenap) : "").append("</td></tr>");
		sb.append("</table>");

		sb.append("<table class='t-main'><tr class='tr-head'>");
		sb.append("<th>Nama Mata Kuliah</th><th>Kode Mata Kuliah</th><th>Bobot (sks)</th><th>TK/Semester</th><th>Tanggal Penyusunan</th></tr><tr>");
		sb.append("<td>").append(rpsHe(mkNama)).append("</td>");
		sb.append("<td class='tc'>").append(rpsHe(mkKode)).append("</td>");
		// Gap #6: tampilkan T:/P: di bawah total SKS
		sb.append("<td class='tc'>").append(totalSks).append(" SKS");
		if (!sksTpStr.isEmpty()) sb.append("<br><small style='color:#555'>(").append(sksTpStr).append(")</small>");
		sb.append("</td>");
		sb.append("<td class='tc'>").append(rpsHe(semester)).append("</td>");
		sb.append("<td class='tc'>").append(rpsHe(tglSusun)).append("</td>");
		sb.append("</tr>");
		// Gap #1: tampilkan baris CPL bobot jika ada
		if (!cplBobot.isEmpty()) {
			sb.append("<tr><td class='td-sl' colspan='2'>CPL yang Dibebankan (dengan Bobot)</td><td colspan='3'>");
			String[] cplParts = cplBobot.split(",");
			for (String cp : cplParts) {
				cp = cp.trim();
				if (cp.contains(":")) {
					String[] kv = cp.split(":", 2);
					sb.append("<span class='cpl-badge'>").append(rpsHe(kv[0].trim()))
					  .append(" <b>").append(rpsHe(kv[1].trim())).append("%</b></span> ");
				} else if (!cp.isEmpty()) {
					sb.append("<span class='cpl-badge'>").append(rpsHe(cp)).append("</span> ");
				}
			}
			sb.append("</td></tr>");
		}
		sb.append("</table>");

		sb.append("<table class='t-main'><tr><td colspan='3' class='td-otorisasi'>Otorisasi</td></tr>");
		sb.append("<tr class='tr-head'><th>Koordinator Mata Kuliah</th><th>Dosen</th><th>Ka Prodi</th></tr>");
		sb.append("<tr style='height:90px'>");
		sb.append("<td class='tc vbot'>").append(rpsHe(koordinator)).append("</td>");
		sb.append("<td class='tc vbot'>").append(rpsHe(namaDosen.isEmpty() ? pengembang : namaDosen)).append("</td>");
		sb.append("<td class='tc vbot'>").append(rpsHe(kaprodi)).append("</td></tr></table>");

		if (!pengembang.isEmpty() || !mitra.isEmpty()) {
			sb.append("<table class='t-main'><tr class='tr-head'><th>Pengembang RPS</th><th>Koordinator RMK</th><th colspan='2'>Mitra Pengembang</th></tr>");
			sb.append("<tr><td>").append(rpsHe(pengembang)).append("</td><td>").append(rpsHe(koordinator)).append("</td><td colspan='2'>").append(rpsHe(mitra)).append("</td></tr></table>");
		}

		// ===== HAL 2: CAPAIAN PEMBELAJARAN =====
		sb.append("<div class='page-break'></div>");
		sb.append(rpsPageHeader(institusi, fakultas, mkKode, berlakuSejak));
		sb.append(rpsSectionIntro("Capaian Pembelajaran",
			"Daftar kemampuan yang harus dikuasai mahasiswa: dari capaian lulusan (CPL), bahan kajian (BK), capaian mata kuliah (CPMK), sampai rincian tiap tahap (Sub-CPMK)."));

		int cpRows = 4 + cplList.size() + bkList.size() + cpmkList.size() + subCpmkList.size();
		if (bkList.isEmpty()) cpRows++;
		sb.append("<table class='t-main'><tr><td class='td-sl' rowspan='").append(cpRows).append("'>Capaian<br>Pembelajaran<br>(CP)</td>");
		sb.append("<td colspan='2' class='td-gh'>CPL-PRODI (Capaian Pembelajaran Lulusan Program Studi) yang Dibebankan Pada Mata Kuliah</td></tr>");
		for (Map cpl : cplList) {
			sb.append("<tr><td class='td-kd'>").append(rpsHe(rpsHs(cpl, "kode"))).append("</td><td>").append(rpsHe(rpsHs(cpl, "nama"))).append("</td></tr>");
		}
		sb.append("<tr><td colspan='2' class='td-gh'>BK (Bahan Kajian)</td></tr>");
		if (bkList.isEmpty()) {
			sb.append("<tr><td colspan='2'>&nbsp;</td></tr>");
		} else {
			for (Map bk : bkList) {
				sb.append("<tr><td class='td-kd'>").append(rpsHe(rpsHs(bk, "kode"))).append("</td><td>").append(rpsHe(rpsHs(bk, "nama"))).append("</td></tr>");
			}
		}
		sb.append("<tr><td colspan='2' class='td-gh'>CPMK (Capaian Pembelajaran Mata Kuliah)</td></tr>");
		for (Map cpmk : cpmkList) {
			sb.append("<tr><td class='td-kd'>").append(rpsHe(rpsHs(cpmk, "kode"))).append("</td><td>").append(rpsHe(rpsHs(cpmk, "nama"))).append("</td></tr>");
		}
		sb.append("<tr><td colspan='2' class='td-gh'>SUB CPMK : Kemampuan akhir tiap tahapan belajar MK (CPMK)</td></tr>");
		for (Map sub : subCpmkList) {
			String cpmkRef = rpsHs(sub, "capaian.kode");
			sb.append("<tr><td class='td-kd'>").append(rpsHe(normalizeSubCpmkKode(rpsHs(sub, "kode")))).append("</td><td>").append(rpsHe(rpsHs(sub, "nama")));
			if (!cpmkRef.isEmpty()) sb.append(" <span class='cpmk-ref'>(").append(rpsHe(cpmkRef)).append(")</span>");
			sb.append("</td></tr>");
		}
		sb.append("</table>");

		// Korelasi CPL terhadap Sub-CPMK
		if (!subCpmkList.isEmpty() && !cpmkList.isEmpty()) {
			sb.append("<table class='t-main'><tr><td colspan='").append(cpmkList.size() + 2).append("' class='td-gh'>Korelasi CPL terhadap Sub-CPMK <small style='font-weight:normal'>(isi: bobot kontribusi Sub-CPMK terhadap setiap CPMK, dalam %)</small></td></tr>");
			sb.append("<tr class='tr-head'><th>Sub-CPMK</th>");
			for (Map cpmk : cpmkList) sb.append("<th>").append(rpsHe(rpsHs(cpmk, "kode"))).append(" (%)</th>");
			sb.append("<th>Bobot (%)</th></tr>");
			double grandTotal = 0;
			for (Map sub : subCpmkList) {
				sb.append("<tr><td class='td-kd'>").append(rpsHe(normalizeSubCpmkKode(rpsHs(sub, "kode")))).append("</td>");
				double rowBobot = 0;
				for (int idx = 1; idx <= cpmkList.size(); idx++) {
					Object bv = sub.get("bobot_index_" + idx);
					double bd = 0;
					if (bv != null) { try { bd = Double.parseDouble(bv.toString()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1312");} }
					rowBobot += bd;
					sb.append("<td class='tc'>").append(bd > 0 ? rpsHd(bd) : "&mdash;").append("</td>");
				}
				Object bMain = sub.get("bobot");
				if (bMain != null) { try { rowBobot = Double.parseDouble(bMain.toString()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1317");} }
				grandTotal += rowBobot;
				sb.append("<td class='tc'>").append(rowBobot > 0 ? rpsHd(rowBobot) : "").append("</td></tr>");
			}
			sb.append("<tr class='tr-total'><td><b>Total</b></td>");
			for (int i = 0; i < cpmkList.size(); i++) sb.append("<td></td>");
			sb.append("<td class='tc'><b>").append(rpsHd(grandTotal)).append("</b></td></tr></table>");
		}

		// Gap #3: Tabel Komponen Penilaian
		if (!komponenPenilaian.isEmpty()) {
			String[] kompItems = komponenPenilaian.split(",");
			sb.append("<table class='t-main t-komponen'>");
			sb.append("<tr><td colspan='").append(kompItems.length + 1).append("' class='td-gh'>Komponen Penilaian</td></tr>");
			sb.append("<tr class='tr-head'><th>Komponen</th>");
			double totalPersen = 0;
			String[] kompHeaders = new String[kompItems.length];
			double[] kompValues  = new double[kompItems.length];
			for (int ki = 0; ki < kompItems.length; ki++) {
				String kp = kompItems[ki].trim();
				if (kp.contains(":")) {
					String[] kv = kp.split(":", 2);
					kompHeaders[ki] = kv[0].trim();
					try { kompValues[ki] = Double.parseDouble(kv[1].trim()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:1340");}
				} else {
					kompHeaders[ki] = kp;
				}
				sb.append("<th>").append(rpsHe(kompHeaders[ki])).append("</th>");
				totalPersen += kompValues[ki];
			}
			sb.append("</tr>");
			sb.append("<tr><td class='td-kd'>Bobot (%)</td>");
			for (int ki = 0; ki < kompItems.length; ki++) {
				sb.append("<td>").append(kompValues[ki] > 0 ? rpsHd(kompValues[ki]) : "").append("</td>");
			}
			sb.append("</tr>");
			sb.append("<tr class='tr-total'><td><b>Total</b></td>");
			for (int ki = 0; ki < kompItems.length; ki++) sb.append("<td></td>");
			sb.append("<td colspan='0'></td></tr>");
			// Validasi total = 100
			if (Math.abs(totalPersen - 100) > 0.01) {
				sb.append("<tr><td colspan='").append(kompItems.length + 1)
				  .append("' style='color:#dc3545;font-size:9pt;padding:3px'>&#9888; Total bobot penilaian = ")
				  .append(rpsHd(totalPersen)).append("% (seharusnya 100%)</td></tr>");
			}
			sb.append("</table>");
		}

		// Matrik Teknik Penilaian per CPMK
		if (!teknikPerCpmk.isEmpty()) {
			// Collect all unique techniques (ordered) from komponenPenilaian then from teknikPerCpmk values
			java.util.List<String> allTeknik = new java.util.ArrayList<String>();
			if (!komponenPenilaian.isEmpty()) {
				for (String kp : komponenPenilaian.split(",")) {
					if (kp.contains(":")) { String tn = kp.split(":", 2)[0].trim(); if (!allTeknik.contains(tn)) allTeknik.add(tn); }
				}
			}
			java.util.Map<String, java.util.List<String>> cpmkToTeknik = new java.util.LinkedHashMap<String, java.util.List<String>>();
			for (String line : teknikPerCpmk.split("\n")) {
				String l = line.trim();
				if (l.isEmpty() || !l.contains(":")) continue;
				String[] kv = l.split(":", 2);
				String cpmkKode = kv[0].trim();
				java.util.List<String> ts = new java.util.ArrayList<String>();
				for (String t : kv[1].split(",")) { String tn = t.trim(); if (!tn.isEmpty()) { ts.add(tn); if (!allTeknik.contains(tn)) allTeknik.add(tn); } }
				cpmkToTeknik.put(cpmkKode, ts);
			}
			if (!cpmkToTeknik.isEmpty() && !allTeknik.isEmpty()) {
				sb.append("<table class='t-main t-teknik'>");
				sb.append("<tr><td colspan='").append(allTeknik.size() + 1).append("' class='td-gh'>Matrik Teknik Penilaian per CPMK</td></tr>");
				sb.append("<tr class='tr-head'><th style='width:15%'>CPMK</th>");
				for (String tn : allTeknik) sb.append("<th>").append(rpsHe(tn)).append("</th>");
				sb.append("</tr>");
				for (java.util.Map.Entry<String, java.util.List<String>> e : cpmkToTeknik.entrySet()) {
					sb.append("<tr><td class='td-kd'>").append(rpsHe(e.getKey())).append("</td>");
					for (String tn : allTeknik) {
						boolean check = e.getValue().contains(tn);
						sb.append("<td class='tc'>").append(check ? "&#10003;" : "").append("</td>");
					}
					sb.append("</tr>");
				}
				sb.append("</table>");
			}
		}

		// Rubrik Penilaian (mis. rubrik presentasi/laporan) — opsional, dari field teks bebas.
		// Format: baris berawalan '#' = judul sub-rubrik; baris lain = Aspek|Bobot%|Skor4|Skor3|Skor2|Skor1.
		if (!rubrikPenilaian.isEmpty()) {
			sb.append("<table class='t-main t-rubrik'>");
			sb.append("<tr><td colspan='6' class='td-gh'>Rubrik Penilaian</td></tr>");
			boolean rubrikHeaderSudah = false;
			for (String rawLine : rubrikPenilaian.split("\n")) {
				String line = rawLine.trim();
				if (line.isEmpty()) continue;
				if (line.startsWith("#")) {
					sb.append("<tr><td colspan='6' class='td-gh' style='text-align:left'>")
					  .append(rpsHe(line.substring(1).trim())).append("</td></tr>");
					rubrikHeaderSudah = false;
					continue;
				}
				if (!rubrikHeaderSudah) {
					sb.append("<tr class='tr-head'>")
					  .append("<th style='width:22%'>Aspek Penilaian</th>")
					  .append("<th style='width:8%'>Bobot (%)</th>")
					  .append("<th>Skor 4 (Sangat Baik)</th>")
					  .append("<th>Skor 3 (Baik)</th>")
					  .append("<th>Skor 2 (Cukup)</th>")
					  .append("<th>Skor 1 (Kurang)</th></tr>");
					rubrikHeaderSudah = true;
				}
				String[] c = line.split("\\|");
				sb.append("<tr>");
				for (int i = 0; i < 6; i++) {
					String val = i < c.length ? c[i].trim() : "";
					sb.append("<td").append(i == 1 ? " class='tc'" : "").append(">")
					  .append(rpsHe(val)).append("</td>");
				}
				sb.append("</tr>");
			}
			sb.append("</table>");
		}

		// Gap #2: Pemetaan Soal UTS & UAS
		if (!pemetaanSoalUts.isEmpty() || !pemetaanSoalUas.isEmpty()) {
			sb.append("<table class='t-main'><tr>");
			if (!pemetaanSoalUts.isEmpty()) {
				sb.append("<td style='vertical-align:top;width:50%'>");
				sb.append("<table class='t-main t-soal' style='margin:0'>");
				sb.append("<tr><td colspan='2' class='td-gh'>Pemetaan Soal UTS</td></tr>");
				sb.append("<tr class='tr-head'><th>Sub CPMK</th><th>Nomor Soal</th></tr>");
				for (String line : pemetaanSoalUts.split("\n")) {
					line = line.trim();
					if (line.isEmpty()) continue;
					if (line.contains("|")) {
						String[] parts = line.split("\\|", 2);
						sb.append("<tr><td>").append(rpsHe(normalizeSubCpmkKode(parts[0].trim())))
						  .append("</td><td>").append(rpsHe(parts[1].trim())).append("</td></tr>");
					} else {
						sb.append("<tr><td colspan='2'>").append(rpsHe(line)).append("</td></tr>");
					}
				}
				sb.append("</table></td>");
			}
			if (!pemetaanSoalUas.isEmpty()) {
				sb.append("<td style='vertical-align:top'>");
				sb.append("<table class='t-main t-soal' style='margin:0'>");
				sb.append("<tr><td colspan='2' class='td-gh'>Pemetaan Soal UAS</td></tr>");
				sb.append("<tr class='tr-head'><th>Sub CPMK</th><th>Nomor Soal</th></tr>");
				for (String line : pemetaanSoalUas.split("\n")) {
					line = line.trim();
					if (line.isEmpty()) continue;
					if (line.contains("|")) {
						String[] parts = line.split("\\|", 2);
						sb.append("<tr><td>").append(rpsHe(normalizeSubCpmkKode(parts[0].trim())))
						  .append("</td><td>").append(rpsHe(parts[1].trim())).append("</td></tr>");
					} else {
						sb.append("<tr><td colspan='2'>").append(rpsHe(line)).append("</td></tr>");
					}
				}
				sb.append("</table></td>");
			}
			sb.append("</tr></table>");
		}

		// ===== HAL 3: DESKRIPSI + REFERENSI + MEDIA =====
		sb.append("<div class='page-break'></div>");
		sb.append(rpsPageHeader(institusi, fakultas, mkKode, berlakuSejak));
		sb.append(rpsSectionIntro("Deskripsi, Materi, dan Referensi Mata Kuliah",
			"Gambaran isi mata kuliah, materi yang dipelajari, sumber bacaan, media, dosen pengampu, dan beban belajar mahasiswa."));
		sb.append("<table class='t-main'>");
		sb.append("<tr><td class='td-sl'>Diskripsi Singkat MK</td><td>").append(rpsHe(deskripsi)).append("</td></tr>");
		sb.append("<tr><td class='td-sl'>Materi Pembelajaran</td><td>");
		if (!bkList.isEmpty()) {
			sb.append("<ol style='margin:0;padding-left:18px'>");
			for (Map bk : bkList) sb.append("<li>").append(rpsHe(rpsHs(bk, "nama"))).append("</li>");
			sb.append("</ol>");
		} else {
			sb.append("&nbsp;");
		}
		sb.append("</td></tr>");
		sb.append("<tr><td class='td-sl'>Daftar Referensi<br>(Jurnal, buku/e-book, 10 th)</td><td>");
		String referensiHtml = daftarReferensiHtml(kpm);
		if (!referensiHtml.isEmpty()) {
			sb.append(referensiHtml);
			if (!catatan.isEmpty()) sb.append(catatan);
		} else if (!catatan.isEmpty()) {
			sb.append(catatan);
		} else {
			sb.append("&nbsp;");
		}
		sb.append("</td></tr>");
		// Media pembelajaran: default standar yang dapat disesuaikan institusi.
		sb.append("<tr><td class='td-sl'>Media Pembelajaran</td><td>");
		sb.append("<b>Perangkat lunak:</b> e-Campus, aplikasi presentasi, video pembelajaran.<br>");
		sb.append("<b>Perangkat keras:</b> LCD/proyektor, laptop, papan tulis.</td></tr>");
		String dosenAmpuStr = namaDosen.isEmpty() ? pengembang : namaDosen;
		sb.append("<tr><td class='td-sl'>Nama Dosen Pengampu</td><td>").append(rpsHe(dosenAmpuStr)).append("</td></tr>");
		sb.append("<tr><td class='td-sl'>Matakuliah Prasyarat (Jika ada)</td><td>&nbsp;</td></tr>");
		sb.append("<tr><td class='td-sl'>Rincian beban Studi</td><td><b>").append(totalSks).append(" SKS</b><br>");
		sb.append("&bull; Tatap muka: ").append(totalSks).append(" SKS &times; 50 mnt &times; 14 mggu = ").append(totalSks * 50 * 14).append(" mnt = ").append(totalSks * 50).append(" mnt/mggu<br>");
		sb.append("&bull; Penugasan terstruktur: ").append(totalSks).append(" SKS &times; 60 mnt &times; 14 mggu = ").append(totalSks * 60 * 14).append(" mnt = ").append(totalSks * 60).append(" mnt/mggu<br>");
		sb.append("&bull; Belajar mandiri: ").append(totalSks).append(" SKS &times; 60 mnt &times; 14 mggu = ").append(totalSks * 60 * 14).append(" mnt = ").append(totalSks * 60).append(" mnt/mggu");
		sb.append("</td></tr>");
		// Metode Pembelajaran: dikumpulkan dari rincian mingguan (tanpa duplikat).
		java.util.LinkedHashSet<String> metodeSet = new java.util.LinkedHashSet<String>();
		for (Map r : rincianList) {
			String lu = rpsHs(r, "pembelajaranLuring");
			String da = rpsHs(r, "pembelajaranDaring");
			if (lu.length() > 0) metodeSet.add(lu);
			if (da.length() > 0) metodeSet.add(da);
		}
		sb.append("<tr><td class='td-sl'>Metode Pembelajaran</td><td>");
		if (!metodeSet.isEmpty()) {
			boolean firstM = true;
			for (String m : metodeSet) { if (!firstM) sb.append(", "); sb.append(rpsHe(m)); firstM = false; }
		} else {
			sb.append("&nbsp;");
		}
		sb.append("</td></tr>");
		// Metode Evaluasi: tabel Blueprint Asesmen (nama komponen + bobot).
		sb.append("<tr><td class='td-sl'>Metode Evaluasi<br><small>(Blueprint Asesmen)</small></td><td>");
		if (!komponenPenilaian.isEmpty()) {
			String[] kompItemsEval = komponenPenilaian.split(",");
			sb.append("<table style='width:auto;border-collapse:collapse;font-size:9pt'>");
			sb.append("<tr style='background:#dbe4f0'><th style='border:1px solid #000;padding:2px 8px'>Komponen Penilaian</th><th style='border:1px solid #000;padding:2px 8px'>Bobot (%)</th></tr>");
			double totalEval = 0;
			for (String kp : kompItemsEval) {
				kp = kp.trim();
				String nm = kp.contains(":") ? kp.split(":", 2)[0].trim() : kp;
				double bv = 0;
				if (kp.contains(":")) { try { bv = Double.parseDouble(kp.split(":", 2)[1].trim()); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:MetodeEvaluasiBobot");} }
				if (nm.isEmpty()) continue;
				totalEval += bv;
				sb.append("<tr><td style='border:1px solid #000;padding:2px 8px'>").append(rpsHe(nm)).append("</td>");
				sb.append("<td style='border:1px solid #000;padding:2px 8px;text-align:center'>").append(bv > 0 ? rpsHd(bv) : "&mdash;").append("</td></tr>");
			}
			sb.append("<tr style='background:#f1f5f9'><td style='border:1px solid #000;padding:2px 8px'><b>Total</b></td>");
			sb.append("<td style='border:1px solid #000;padding:2px 8px;text-align:center'><b>").append(rpsHd(totalEval)).append("</b></td></tr></table>");
		} else {
			sb.append("&nbsp;");
		}
		sb.append("</td></tr>");
		sb.append("</table>");

		// ===== HAL 4+: RINCIAN MINGGUAN =====
		if (!rincianList.isEmpty()) {
			sb.append("<div class='page-break'></div>");
			sb.append(rpsPageHeader(institusi, fakultas, mkKode, berlakuSejak));
			sb.append(rpsSectionIntro("Rencana Pembelajaran Mingguan",
				"Rincian tiap minggu: kemampuan yang ditargetkan, materi, cara belajar, pengalaman belajar, serta cara dan bobot penilaiannya."));
			sb.append("<div class='tbl-scroll'>");
			sb.append("<table class='t-rincian'><thead><tr class='tr-head'>");
			sb.append("<th rowspan='2' style='width:4%'>Mg<br>Ke-</th>");
			sb.append("<th rowspan='2' style='width:13%'>Sub-CPMK<br>(Kemampuan akhir yang direncanakan)</th>");
			sb.append("<th rowspan='2' style='width:12%;background:#fff3cd'>Materi<br>Pembelajaran</th>");
			sb.append("<th colspan='2' style='width:18%'>Bentuk Pembelajaran, Metode Pembelajaran,<br>Penugasan Mahasiswa [Estimasi Waktu]</th>");
			sb.append("<th rowspan='2' style='width:13%'>Pengalaman Belajar Mahasiswa</th>");
			sb.append("<th colspan='3'>Penilaian</th></tr>");
			sb.append("<tr class='tr-head'><th>Pembelajaran<br>Luring</th><th>Pembelajaran<br>Daring</th>");
			sb.append("<th>Kriteria &amp; Bentuk</th><th>Indikator</th><th style='width:5%'>Bobot<br>(%)</th></tr>");
			sb.append("</thead><tbody>");
			for (Map r : rincianList) {
				Object mgKe  = r.get("mulaiMingguKe");
				Object mgSmp = r.get("sampaiMingguKe");
				String mgStr = mgKe != null ? mgKe.toString() : "";
				if (mgSmp != null && !mgSmp.toString().isEmpty() && !"null".equals(mgSmp.toString())) mgStr += "-" + mgSmp;
				String subDes  = rpsHs(r, "sub_cpmk_des");
				String luring  = rpsHs(r, "pembelajaranLuring");
				String daring  = rpsHs(r, "pembelajaranDaring");
				String teknik  = rpsHs(r, "teknikDanKriteria");
				String indik   = rpsHs(r, "indikator");
				Object bkRaw   = r.get("bahanKajians");
				String bkStr   = bkJsonToNama(bkRaw);
				Object bobRaw  = r.get("bobot");
				String bobStr  = bobRaw != null ? bobRaw.toString() : "";
				sb.append("<tr>");
				sb.append("<td class='tc'>").append(rpsHe(mgStr)).append("</td>");
				sb.append("<td>").append(rpsHe(subDes)).append("</td>");
				sb.append("<td>").append(bkStr).append("</td>");
				sb.append("<td>").append(rpsHe(luring)).append("</td>");
				sb.append("<td>").append(rpsHe(daring)).append("</td>");
				sb.append("<td>").append(rpsHe(rpsHs(r, "pengalamanBelajar"))).append("</td>");
				sb.append("<td>").append(rpsHe(teknik)).append("</td>");
				sb.append("<td>").append(rpsHe(indik)).append("</td>");
				sb.append("<td class='tc'>").append(rpsHe(bobStr)).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</tbody></table>");
			sb.append("</div>");
		}

		// ===== PETA KETERCAPAIAN PEMBELAJARAN (alur HTML/CSS, tanpa JFreeChart) =====
		if (!rincianList.isEmpty()) {
			sb.append("<div class='page-break'></div>");
			sb.append(rpsPageHeader(institusi, fakultas, mkKode, berlakuSejak));
			sb.append(rpsSectionIntro("Peta Ketercapaian Pembelajaran",
				"Alur tahapan belajar dari minggu awal, melewati UTS dan UAS, hingga mencapai capaian lulusan."));
			sb.append(rpsFlowPeta(rincianList, cplList));
		}

		// ===== MATRIKS CAPAIAN PEMBELAJARAN vs BENTUK ASESMEN =====
		if (!rincianList.isEmpty()) {
			sb.append(rpsSectionIntro("Matriks Capaian Pembelajaran dan Asesmen",
				"Menghubungkan tiap Sub-CPMK dengan metode pembelajaran, bentuk penilaian, dan bobot nilainya dalam satu tabel ringkas."));
			sb.append(rpsAsesmenMatrix(rincianList));
		}

		// ===== CATATAN / PENJELASAN ISTILAH OBE =====
		sb.append("<div class='page-break'></div>");
		sb.append(rpsPageHeader(institusi, fakultas, mkKode, berlakuSejak));
		sb.append(rpsSectionIntro("Catatan & Penjelasan Istilah",
			"Penjelasan singkat istilah OBE agar dokumen ini mudah dipahami siapa pun, termasuk yang baru mengenal kurikulum OBE."));
		sb.append(rpsCatatan());

		sb.append("</div>"); // .sheet
		sb.append("</body></html>");
		return sb.toString();
	}

	/**
	 * Judul + kalimat penjelasan singkat sebuah bagian, dengan bahasa yang mudah
	 * dipahami end-user awam. Dipakai ulang di seluruh halaman cetak RPS OBE.
	 */
	private static String rpsSectionIntro(String judul, String penjelasan) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='sec-intro'>");
		sb.append("<div class='sec-judul'>").append(rpsHe(judul)).append("</div>");
		if (penjelasan != null && penjelasan.trim().length() > 0) {
			sb.append("<div class='sec-desc'>").append(rpsHe(penjelasan)).append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Pemenggalan teks agar kartu ringkas tetap rapi. */
	private static String rpsTrunc(String s, int max) {
		if (s == null) return "";
		String t = s.trim();
		if (t.length() <= max) return t;
		return t.substring(0, max).trim() + "…";
	}

	/**
	 * Peta Ketercapaian Pembelajaran: alur tahapan Sub-CPMK dari minggu awal,
	 * melewati UTS dan UAS, sampai menuju Capaian Pembelajaran Lulusan (CPL).
	 * Murni HTML/CSS (tanpa JFreeChart), responsif untuk HP maupun komputer.
	 */
	private static String rpsFlowPeta(java.util.List<Map> rincianList, java.util.List<Map> cplList) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='peta-wrap'>");
		// Tujuan akhir (CPL) sebagai puncak.
		sb.append("<div class='peta-goal'>");
		sb.append("<span class='peta-goal-ic'>&#127919;</span>");
		sb.append("<span class='peta-goal-txt'>Tujuan akhir: Capaian Pembelajaran Lulusan (CPL)");
		if (cplList != null && !cplList.isEmpty()) {
			sb.append(" &mdash; ");
			boolean first = true;
			for (Map cpl : cplList) {
				if (!first) sb.append(", ");
				sb.append(rpsHe(rpsHs(cpl, "kode")));
				first = false;
			}
		}
		sb.append("</span></div>");

		sb.append("<div class='peta-track'>");
		boolean utsDitandai = false;
		boolean adaPascaUts = false;
		for (Map r : rincianList) {
			int mgMulai = rpsAsInt(r.get("mulaiMingguKe"), 0);
			int mgSampai = rpsAsInt(r.get("sampaiMingguKe"), mgMulai);
			// Sisipkan penanda UTS saat tahapan mulai melewati pekan ke-8.
			if (!utsDitandai && mgMulai >= 9) {
				sb.append(rpsPetaMilestone("uts", "UTS", "Ujian Tengah Semester", ""));
				utsDitandai = true;
			}
			if (mgMulai >= 9) adaPascaUts = true;
			String mg = mgMulai > 0 ? ("Mg " + mgMulai + (mgSampai > mgMulai ? "–" + mgSampai : "")) : "Mg -";
			String judul = rpsHs(r, "kode");
			if (judul.length() == 0) judul = "Sub-CPMK";
			else judul = normalizeSubCpmkKode(judul);
			String desc = rpsTrunc(rpsHs(r, "sub_cpmk_des"), 110);
			Object bobRaw = r.get("bobot");
			String bobot = bobRaw != null && bobRaw.toString().trim().length() > 0 ? (bobRaw.toString().trim() + "%") : "";
			sb.append(rpsPetaMilestone("step", mg, judul, desc + (bobot.length() > 0 ? " (" + bobot + ")" : "")));
		}
		// UAS di ujung bila ada tahapan setelah UTS.
		if (adaPascaUts) {
			sb.append(rpsPetaMilestone("uas", "UAS", "Ujian Akhir Semester", ""));
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private static String rpsPetaMilestone(String tipe, String badge, String judul, String desc) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='peta-step peta-").append(tipe).append("'>");
		sb.append("<div class='peta-badge'>").append(rpsHe(badge)).append("</div>");
		sb.append("<div class='peta-body'>");
		sb.append("<div class='peta-judul'>").append(rpsHe(judul)).append("</div>");
		if (desc != null && desc.trim().length() > 0) {
			sb.append("<div class='peta-keterangan'>").append(rpsHe(desc)).append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	/**
	 * Matriks antara Capaian Pembelajaran dan bentuk Asesmen: menghubungkan
	 * tiap Sub-CPMK dengan CPMK, metode pembelajaran, bentuk penilaian, dan
	 * bobotnya. Dibangun dari data rincian mingguan yang sudah diisi.
	 */
	private static String rpsAsesmenMatrix(java.util.List<Map> rincianList) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='tbl-scroll'><table class='t-main t-matriks'><thead><tr class='tr-head'>");
		sb.append("<th style='width:4%'>No</th><th style='width:10%'>CPMK</th><th style='width:13%'>Sub-CPMK</th>");
		sb.append("<th>Metode Pembelajaran</th><th>Bentuk Penilaian</th><th style='width:8%'>Bobot (%)</th></tr></thead><tbody>");
		int no = 0;
		double total = 0;
		for (Map r : rincianList) {
			no++;
			String cpmk = rpsHs(r, "capaian.kode");
			String sub = normalizeSubCpmkKode(rpsHs(r, "kode"));
			String metode = rpsHs(r, "pembelajaranLuring");
			String daring = rpsHs(r, "pembelajaranDaring");
			if (daring.length() > 0) metode = metode.length() > 0 ? metode + "; " + daring : daring;
			String bentuk = rpsHs(r, "teknikDanKriteria");
			Object bobRaw = r.get("bobot");
			double bob = rpsAsDouble(bobRaw, 0);
			total += bob;
			sb.append("<tr>");
			sb.append("<td class='tc'>").append(no).append("</td>");
			sb.append("<td class='tc'>").append(rpsHe(cpmk)).append("</td>");
			sb.append("<td>").append(rpsHe(sub)).append("</td>");
			sb.append("<td>").append(rpsHe(metode)).append("</td>");
			sb.append("<td>").append(rpsHe(bentuk)).append("</td>");
			sb.append("<td class='tc'>").append(bob > 0 ? rpsHd(bob) : "").append("</td>");
			sb.append("</tr>");
		}
		sb.append("<tr class='tr-total'><td colspan='5' class='tc'><b>Total Bobot</b></td><td class='tc'><b>")
		  .append(rpsHd(total)).append("</b></td></tr>");
		sb.append("</tbody></table></div>");
		return sb.toString();
	}

	/** Catatan baku istilah OBE (CPL, CPMK, Sub-CPMK, penilaian, dll). */
	private static String rpsCatatan() {
		String[] notes = new String[] {
			"<b>CPL (Capaian Pembelajaran Lulusan)</b> adalah kemampuan yang wajib dimiliki setiap lulusan program studi, mencakup sikap, pengetahuan, dan keterampilan.",
			"<b>CPL yang dibebankan pada mata kuliah</b> adalah sebagian CPL prodi yang dititipkan untuk dibentuk lewat mata kuliah ini.",
			"<b>CPMK (Capaian Pembelajaran Mata Kuliah)</b> adalah penjabaran CPL yang lebih spesifik dan khusus untuk mata kuliah ini.",
			"<b>Sub-CPMK</b> adalah kemampuan akhir yang terukur pada tiap tahap belajar, diturunkan dari CPMK.",
			"<b>Penilaian Formatif</b> memantau perkembangan belajar dan memberi umpan balik agar mahasiswa mencapai target.",
			"<b>Penilaian Sumatif</b> menilai hasil belajar sebagai dasar kelulusan mata kuliah (ujian, tugas, proyek, dsb.).",
			"<b>Indikator</b> adalah pernyataan terukur tentang kemampuan/kinerja hasil belajar mahasiswa beserta buktinya.",
			"<b>Kriteria</b> adalah tolok ukur ketercapaian pembelajaran agar penilaian konsisten dan tidak bias.",
			"<b>Bentuk pembelajaran</b>: kuliah, responsi/tutorial, seminar, praktikum, praktik lapangan, dan bentuk lain yang relevan.",
			"<b>Metode pembelajaran</b>: diskusi kelompok, studi kasus, pembelajaran kooperatif/kolaboratif, berbasis proyek/masalah, dll.",
			"<b>Materi pembelajaran</b> adalah uraian bahan kajian yang disajikan dalam pokok dan sub-pokok bahasan.",
			"<b>Bobot penilaian</b> adalah persentase nilai tiap Sub-CPMK sesuai tingkat kesulitannya; totalnya 100%.",
			"<b>Singkatan waktu</b>: TM = Tatap Muka, BT = Belajar Terstruktur, BM = Belajar Mandiri."
		};
		StringBuilder sb = new StringBuilder();
		sb.append("<ol class='catatan-list'>");
		for (int i = 0; i < notes.length; i++) {
			sb.append("<li>").append(notes[i]).append("</li>");
		}
		sb.append("</ol>");
		return sb.toString();
	}

	private static int rpsAsInt(Object o, int def) {
		if (o == null) return def;
		try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return def; }
	}

	private static double rpsAsDouble(Object o, double def) {
		if (o == null) return def;
		try { return Double.parseDouble(o.toString().trim().replace("%", "").replace(",", ".")); }
		catch (Exception e) { return def; }
	}

	private static String rpsPageHeader(String institusi, String subTitle) {
		return rpsPageHeader(institusi, subTitle, "", "");
	}

	private static String rpsPageHeader(String institusi, String subTitle, String mkKode, String berlakuSejak) {
		StringBuilder sb = new StringBuilder();
		sb.append("<table class='t-header'><tr>");
		sb.append("<td class='td-logo' rowspan='3'></td>");
		sb.append("<td class='bold'>").append(rpsHe(institusi)).append("</td>");
		sb.append("<td class='td-kl'>Kode</td><td class='td-ks'>:</td><td>").append(rpsHe(mkKode)).append("</td></tr>");
		sb.append("<tr><td class='bold tc'>").append(rpsHe(subTitle)).append("</td>");
		sb.append("<td class='td-kl'>Berlaku sejak</td><td>:</td><td>").append(rpsHe(berlakuSejak)).append("</td></tr>");
		sb.append("<tr><td class='bold tc'>RENCANA PEMBELAJARAN SEMESTER (RPS)</td>");
		sb.append("<td class='td-kl'>Halaman</td><td>:</td><td></td></tr></table>");
		return sb.toString();
	}

	private static String rpsHtmlCss() {
		return
			"*{box-sizing:border-box}" +
			":root{--ink:#0f172a;--muted:#64748b;--line:#cbd5e1;--brand:#1e40af;--brand2:#3b82f6;--bg:#eef2f7;--goal:#7c3aed;--uts:#d97706;--uas:#dc2626}" +
			"html,body{margin:0;padding:0}" +
			"body{background:var(--bg);color:var(--ink);font-family:'Segoe UI',Arial,Helvetica,sans-serif;font-size:13px;line-height:1.45}" +
			/* ---- toolbar layar (tidak ikut tercetak) ---- */
			".toolbar{position:sticky;top:0;z-index:50;display:flex;align-items:center;justify-content:space-between;gap:12px;background:linear-gradient(135deg,#1e3a8a,#2563eb);color:#fff;padding:10px 18px;box-shadow:0 2px 10px rgba(2,6,23,.25);flex-wrap:wrap}" +
			".toolbar-left{display:flex;flex-direction:column;min-width:0}" +
			".toolbar-brand{font-weight:800;font-size:14px;letter-spacing:.3px}" +
			".toolbar-sub{font-size:12px;opacity:.9;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:60vw}" +
			".toolbar-right{display:flex;gap:8px;flex-wrap:wrap}" +
			".btn{border:none;border-radius:8px;padding:9px 16px;font-size:13px;font-weight:700;cursor:pointer;font-family:inherit}" +
			".btn-cetak{background:#f59e0b;color:#1f2937}.btn-cetak:hover{background:#fbbf24}" +
			".btn-tutup{background:rgba(255,255,255,.15);color:#fff;border:1px solid rgba(255,255,255,.4)}.btn-tutup:hover{background:rgba(255,255,255,.28)}" +
			".toolbar-hint{background:#fef9c3;border-bottom:1px solid #fde68a;color:#713f12;font-size:12px;padding:8px 18px}" +
			/* ---- lembar dokumen ---- */
			".sheet{max-width:1180px;margin:18px auto;background:#fff;border-radius:10px;box-shadow:0 10px 30px rgba(2,6,23,.12);padding:26px 30px}" +
			/* ---- tabel formal (Times, mengikuti format RPS resmi) ---- */
			".t-main,.t-rincian,.t-header,.t-matriks{font-family:'Times New Roman',Times,serif;color:#000}" +
			"table{width:100%;border-collapse:collapse;margin-bottom:10px}" +
			"td,th{border:1px solid #000;padding:4px 6px;vertical-align:top}" +
			".t-header{margin-bottom:6px}" +
			".td-logo{width:75px;text-align:center;font-size:8pt;color:#aaa}" +
			".td-kl{width:90px}.td-ks{width:12px}" +
			".td-title{text-align:center;font-weight:bold;font-size:13pt;padding:8px;background:#dbeafe}" +
			".td-subtitle{text-align:center;font-weight:bold;font-size:11pt;padding:4px}" +
			".td-otorisasi{text-align:center;font-weight:bold;background:#93c5fd;padding:5px}" +
			".td-sl{font-weight:bold;width:22%;background:#f1f5f9}" +
			".td-gh{font-weight:bold;background:#e2e8f0}" +
			".td-kd{width:15%;font-weight:bold}" +
			".tr-head th{background:#dbe4f0;text-align:center;font-size:10pt}" +
			".tr-total td{background:#f1f5f9}" +
			".tc{text-align:center}.vbot{vertical-align:bottom;padding-bottom:6px}.bold{font-weight:bold}" +
			".cpmk-ref{color:#555;font-size:9pt}" +
			".section-title{font-weight:bold;font-size:11pt;margin:6px 0 4px 0}" +
			".t-rincian{font-size:9pt}.t-rincian td,.t-rincian th{padding:3px 4px}" +
			".t-matriks{font-size:9.5pt}.t-matriks td,.t-matriks th{padding:4px 6px}" +
			".cpl-badge{display:inline-block;background:#dbeafe;border:1px solid #93c5fd;border-radius:4px;padding:2px 8px;margin:2px;font-size:10pt}" +
			".t-soal td:first-child{font-weight:bold;width:30%;background:#f8fafc}" +
			".t-komponen th{background:#dbe4f0;text-align:center;font-size:9pt}" +
			".t-komponen td{text-align:center;font-size:9pt}.t-komponen td:first-child{text-align:left;font-weight:bold}" +
			".t-teknik th{background:#d1fae5;text-align:center;font-size:9pt}" +
			".t-teknik td{text-align:center;font-size:9pt;color:#166534}.t-teknik td:first-child{text-align:left;font-weight:bold;color:#000}" +
			".warn-badge{display:inline-block;background:#fef3c7;border:1px solid #f59e0b;border-radius:3px;padding:1px 6px;font-size:8pt;color:#92400e;margin-left:4px}" +
			/* ---- intro tiap bagian (bahasa sederhana) ---- */
			".sec-intro{font-family:'Segoe UI',Arial,sans-serif;margin:18px 0 8px;padding:10px 14px;background:linear-gradient(135deg,#eff6ff,#f8fafc);border-left:5px solid var(--brand2);border-radius:8px}" +
			".sec-judul{font-weight:800;font-size:14px;color:var(--brand)}" +
			".sec-desc{font-size:12px;color:var(--muted);margin-top:2px}" +
			/* ---- pembungkus tabel agar bisa digeser di HP ---- */
			".tbl-scroll{width:100%;overflow-x:auto;-webkit-overflow-scrolling:touch}" +
			/* ---- peta ketercapaian (alur HTML/CSS) ---- */
			".peta-wrap{font-family:'Segoe UI',Arial,sans-serif;margin:8px 0 14px}" +
			".peta-goal{display:flex;align-items:center;gap:10px;background:linear-gradient(135deg,#7c3aed,#a855f7);color:#fff;border-radius:10px;padding:12px 16px;font-weight:700;box-shadow:0 4px 12px rgba(124,58,237,.3)}" +
			".peta-goal-ic{font-size:20px}" +
			".peta-track{display:flex;flex-wrap:wrap;gap:10px;margin-top:12px}" +
			".peta-step{flex:1 1 220px;min-width:200px;border:1px solid var(--line);border-radius:10px;padding:10px 12px;background:#fff;box-shadow:0 2px 6px rgba(2,6,23,.06)}" +
			".peta-badge{display:inline-block;font-weight:800;font-size:12px;color:#fff;background:var(--brand2);border-radius:999px;padding:2px 10px;margin-bottom:6px}" +
			".peta-judul{font-weight:700;font-size:13px;color:var(--ink)}" +
			".peta-keterangan{font-size:11.5px;color:var(--muted);margin-top:3px}" +
			".peta-uts{border-color:#fcd34d;background:#fffbeb}.peta-uts .peta-badge{background:var(--uts)}" +
			".peta-uas{border-color:#fecaca;background:#fef2f2}.peta-uas .peta-badge{background:var(--uas)}" +
			/* ---- catatan ---- */
			".catatan-list{font-family:'Segoe UI',Arial,sans-serif;font-size:12.5px;color:var(--ink);margin:6px 0;padding-left:20px}" +
			".catatan-list li{margin-bottom:5px}" +
			".page-break{height:0;margin:0;padding:0}" +
			/* ---- cetak ---- */
			"@media print{" +
			"body{background:#fff;font-size:10pt}" +
			".no-print{display:none!important}" +
			".sheet{max-width:none;margin:0;border-radius:0;box-shadow:none;padding:0}" +
			".sec-intro,.peta-goal,.peta-badge,.peta-uts,.peta-uas,.td-title,.td-subtitle,.td-otorisasi,.tr-head th{-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
			".tbl-scroll{overflow:visible}" +
			".sec-intro{break-inside:avoid}.peta-step{break-inside:avoid}" +
			".page-break{page-break-before:always}" +
			"table{page-break-inside:auto}tr{page-break-inside:avoid}thead{display:table-header-group}" +
			"@page{margin:1.2cm;size:A4 landscape}" +
			"}" +
			/* ---- mobile ---- */
			"@media screen and (max-width:768px){" +
			".sheet{margin:10px;padding:14px}" +
			".toolbar-sub{max-width:48vw}" +
			".peta-step{flex:1 1 100%}" +
			"}";
	}

	private static String rpsHs(Map m, String key) {
		if (m == null) return "";
		Object v = m.get(key);
		return v == null ? "" : v.toString().trim();
	}

	private static String rpsHe(String s) {
		if (s == null || s.isEmpty()) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\n", "<br>");
	}

	private static String rpsHd(double d) {
		if (d == (long) d) return String.valueOf((long) d);
		return String.format("%.1f", d);
	}

	/** Ubah JSONObject bahanKajians {"id":{"id":..,"nama":..},...} menjadi HTML list nama, sudah di-escape. */
	@SuppressWarnings("unchecked")
	private static String bkJsonToNama(Object raw) {
		if (raw == null) return "";
		if (!(raw instanceof JSONObject)) return rpsHe(raw.toString());
		JSONObject jo = (JSONObject) raw;
		StringBuilder out = new StringBuilder();
		java.util.Iterator<String> keys = jo.keys();
		while (keys.hasNext()) {
			String k = keys.next();
			JSONObject item = jo.optJSONObject(k);
			if (item != null) {
				String nama = item.optString("nama", "").trim();
				if (!nama.isEmpty()) {
					if (out.length() > 0) out.append("<br>");
					out.append(rpsHe(nama));
				}
			}
		}
		return out.toString();
	}

	// Gap #4: normalisasi kode Sub-CPMK agar konsisten ("SubCPMK 1.1" → "Sub CPMK 1.1")
	private static String normalizeSubCpmkKode(String kode) {
		if (kode == null) return "";
		String s = kode.trim();
		// Tangani variasi: "SubCPMK", "sub cpmk", "SUBCPMK", "Sub-CPMK", "SubCpmk"
		s = s.replaceAll("(?i)sub[\\s-_]*cpmk", "Sub CPMK");
		return s;
	}

	private void initMk() throws Exception {
		// rowsUtama di-autowire dari <rows id="rowsUtama"> pada rps_obe.zul. Bila null
		// (mis. zul/komponen belum ter-compose pada deploy yang belum sinkron), jangan
		// NPE — lewati pembangunan detail Mata Kuliah agar halaman tetap tampil.
		if (rowsUtama == null || kurikulumPunyaMatakuliah == null
				|| kurikulumPunyaMatakuliah.getMatakuliah() == null
				|| kurikulumPunyaMatakuliah.getKurikulum() == null) {
			return;
		}
		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Mata Kuliah"));
		rowsUtama.appendChild(group);

		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Kode"),
				new Label(kurikulumPunyaMatakuliah.getMatakuliah().getKode()));
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Nama"), RevisiHelper.createNewRevisi(Matakuliah.class,
				kurikulumPunyaMatakuliah.getMatakuliah(), kurikulumPunyaMatakuliah.getMatakuliah().getNama()));
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Rumpun MK"),
				new Label(kurikulumPunyaMatakuliah.getMatakuliah().getKelompokMatakuliah() == null ? ""
						: kurikulumPunyaMatakuliah.getMatakuliah().getKelompokMatakuliah().getNama()));
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("SKS Mata Kuliah"),
				new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getMatakuliah().getSks())));

		if (kurikulumPunyaMatakuliah.getMatakuliah().getSksDiskusi() > 0)
			createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("SKS Tatap Muka"),
					new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getMatakuliah().getSksDiskusi())));
		if (kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktek() > 0)
			createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("SKS Praktikum"),
					new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktek())));
		if (kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktekLapangan() > 0)
			createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("SKS Praktikum Lapangan"), new Label(
					Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktekLapangan())));
		if (kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktekLapangan() > 0)
			createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("SKS Simulasi"),
					new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getMatakuliah().getSksSimulasi())));

		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Kurikulum"),
				RevisiHelper.createNewRevisi(KurikulumPunyaMatakuliah.class, kurikulumPunyaMatakuliah,
						kurikulumPunyaMatakuliah.getKurikulum().getNama()));
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Program Studi"),
				new Label(kurikulumPunyaMatakuliah.getKurikulum().getJurusan() == null ? ""
						: kurikulumPunyaMatakuliah.getKurikulum().getJurusan().getNama()));
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Semester"),
				new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getSemester())));

		tanggalPenyusunan = new MyDatebox(kurikulumPunyaMatakuliah.getTanggalPenyusunan());
		tanggalPenyusunan.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Tanggal Penyusunan"),
				bolehUbahObe ? tanggalPenyusunan
						: new Label(kurikulumPunyaMatakuliah.getTanggalPenyusunan() == null ? ""
								: Common.dateFormat6.get().format(kurikulumPunyaMatakuliah.getTanggalPenyusunan())));

		boolean bolehLihat = tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getGuru() == null;

		if (bolehLihat) {
			MyFormRow row = new MyFormRow();
			row.setValign("middle");
			row.setStyle("border-bottom: 1px dashed #e2e8f0; padding: 10px 0; background: transparent;");
			row.setParent(rowsUtama);
			MyLabelConfigAgakBesar lbl = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Kunci RPS Ini"));
			lbl.setStyle("font-weight: 600; color: #475569; font-size: 12px;");
			row.appendChild(lbl);
			Hbox hbox = new Hbox();
			row.appendChild(hbox);
			tampilKunci(hbox, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);
				}
			});

			if (perkuliahan != null && kurikulumPunyaMatakuliah.getDikunci() == null) {
				row = new MyFormRow();
				row.setValign("middle");
				row.setStyle("border-bottom: 1px dashed #e2e8f0; padding: 10px 0; background: transparent;");
				row.setParent(rowsUtama);
				lbl = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Salin Data dari RPS Lain"));
				lbl.setStyle("font-weight: 600; color: #475569; font-size: 12px;");
				row.appendChild(lbl);
				hbox = new Hbox();
				row.appendChild(hbox);
				PenjadwalanHelper.tampilTombolAmbil(hbox, perkuliahan, null, null, null, null, null, null,
						new DataLoader() {
							@Override
							public void loadData(Object value) {
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										perkuliahan.belum();
										onSearchDefault(arg0);
									}
								});
							}
						});
			}
		}

		minimalKetercapaian = new MyDoublebox(kurikulumPunyaMatakuliah.getMinimalKetercapaian());
		minimalKetercapaian.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Nilai Minimal Ketercapaian"),
				bolehUbahObe ? minimalKetercapaian
						: new Label(Common.numberFormat.get().format(kurikulumPunyaMatakuliah.getMinimalKetercapaian())));

		nilaiMenggunakanCpmk = new MyCheckboxConfig(Common.getBahasaConfig("Ya"));
		nilaiMenggunakanCpmk.setChecked(kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk());
		nilaiMenggunakanCpmk.addEventListener("onClick", eventListener);
		createRowLabelAndValue(rowsUtama,
				Common.getBahasaConfig("Bobot Penilaian Menggunakan CPMK (Tidak ada Sub-CPMK)"),
				bolehUbahObe ? nilaiMenggunakanCpmk
						: new Label(kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? Common.getBahasaConfig("Ya")
								: Common.getBahasaConfig("Tidak")));
	}

	// HELPER UI: Meringkas pembuatan row dengan Facelift
	private void createRowLabelAndValue(Rows parent, String labelText, Component valueComponent) {
		MyFormRow row = new MyFormRow();
		row.setValign("middle");
		row.setStyle("border-bottom: 1px dashed #e2e8f0; padding: 10px 0; background: transparent;");
		row.setParent(parent);
		MyLabelConfigAgakBesar lbl = new MyLabelConfigAgakBesar(labelText);
		lbl.setStyle("font-weight: 600; color: #475569; font-size: 12px;");
		row.appendChild(lbl);
		
		if (valueComponent instanceof Label) {
			((Label) valueComponent).setStyle(BADGE_STYLE);
		}
		row.appendChild(valueComponent);
	}

	private void initOtoritas() {
		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Otoritas"));
		rowsUtama.appendChild(group);

		pengembangRps = new MyTextbox(kurikulumPunyaMatakuliah.getPengembangRps());
		pengembangRps.setRows(2);
		pengembangRps.setWidth("95%");
		pengembangRps.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Pengembang RPS"),
				bolehUbahObe ? pengembangRps : new Label(kurikulumPunyaMatakuliah.getPengembangRps()));

		koordinator = new MyTextbox(kurikulumPunyaMatakuliah.getKoordinator());
		koordinator.setRows(2);
		koordinator.setWidth("95%");
		koordinator.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Koordinator"),
				bolehUbahObe ? koordinator : new Label(kurikulumPunyaMatakuliah.getKoordinator()));

		createRowLabelAndValue(rowsUtama, Common.getBahasaConfig("Ketua Program Studi"),
				new Label(kurikulumPunyaMatakuliah.getKurikulum().getJurusan() == null
						|| kurikulumPunyaMatakuliah.getKurikulum().getJurusan().getKaprodi() == null ? ""
								: kurikulumPunyaMatakuliah.getKurikulum().getJurusan().getKaprodi().getNama()));
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void initPl() throws Exception {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Group group = new Group(Common.getBahasaConfig("Profil Lulusan") + " - "
					+ kurikulumPunyaMatakuliah.getKurikulum().getJurusan().getNama());
			rowsUtama.appendChild(group);

			final Set<Long> longs = parseIdsToSet(matakuliah.getCapaianLulusan());
			List<CapaianLulusan> capaianLulusans = ConstantValues
					.simpleList(
							session.createCriteria(CapaianLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianLulusan.class);

			Set<Long> longsProfile = parseIdsToSet(matakuliah.getProfilLulusan());
			List<ProfilLulusan> profilLulusans = ConstantValues.simpleList(
					session.createCriteria(ProfilLulusan.class)
							.add(longsProfile.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", longsProfile))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					ProfilLulusan.class);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			MyLabelConfigAgakBesar lblTitle = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Tambah Profil Lulusan"));
			lblTitle.setStyle(TITLE_STYLE);
			row.appendChild(lblTitle);

			Hbox hbox = new Hbox();
			hbox.setVisible(bolehUbahObe && tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null);
			hbox.setParent(row);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Ambil Profil Lulusan yang Tersedia"), "/img/svg/search.svg");
			button.setStyle(BTN_PRIMARY);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session ses = null;
					try {
						ses = HibernateUtil.openSession();
						Set<Long> longs = parseIdsToSet(matakuliah.getProfilLulusan());
						List<ProfilLulusan> profilLulusans = ConstantValues.simpleList(
								ses.createCriteria(ProfilLulusan.class)
										.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("id", longs))
										.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions
												.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								ProfilLulusan.class);

						AmbilDataProfilLulusanBanyak ambilDataProfilLulusanBanyak = new AmbilDataProfilLulusanBanyak(
								profilLulusans, matakuliah.getJurusan());
						ambilDataProfilLulusanBanyak
								.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						ambilDataProfilLulusanBanyak.setHeight("95%");
						ambilDataProfilLulusanBanyak.setWidth("700px");

						ambilDataProfilLulusanBanyak.setEventListener(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								List<ProfilLulusan> profilLulusanes = (List<ProfilLulusan>) arg0.getData();
								for (ProfilLulusan profilLulusan : profilLulusanes) {
									matakuliah.setProfilLulusan(
											appendIdToString(matakuliah.getProfilLulusan(), profilLulusan.getId()));
								}
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						});
						ambilDataProfilLulusanBanyak.onModal();
					} finally {
						if (ses != null) {
							ses.clear();
							ses.disconnect();
							ses.close();
						}
					}
				}
			});

			button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah Profil Lulusan Baru"),
					"/img/svg/addthis.svg");
			button.setStyle(BTN_SUCCESS);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ProfilLulusan profilLulusan = new ProfilLulusan();
					profilLulusan.setJurusan(perkuliahan == null ? null : perkuliahan.getJurusan());
					ProfilLulusanAction.onAddExternal(arg0, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							ProfilLulusan profilLulusan = (ProfilLulusan) arg0.getData();
							if (profilLulusan != null) {
								matakuliah.setProfilLulusan(
										appendIdToString(matakuliah.getProfilLulusan(), profilLulusan.getId()));
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						}
					}, profilLulusan);
				}
			});

			MyToolbarbuttonConfig btnAiPl = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Generate PL berdasarkan AI"), "/img/svg/sparkles.svg");
			btnAiPl.setStyle("font-size: 12px; font-weight: bold; color: #ffffff; background-color: #7c3aed;"
					+ " border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer;"
					+ " box-shadow: 0 2px 4px rgba(124,58,237,0.3); border: none; margin-right: 5px;");
			btnAiPl.setParent(hbox);
			btnAiPl.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilGenerateAiPl();
				}
			});

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(row);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig colKode = new MyColumnConfig(Common.getBahasaConfig("Kode"));
			colKode.setWidth("80px");
			colKode.setParent(columns);

			MyColumnConfig colProfil = new MyColumnConfig(Common.getBahasaConfig("Profil"));
			colProfil.setWidth("300px");
			colProfil.setParent(columns);

			for (CapaianLulusan capaianLulusan : capaianLulusans) {
				MyColumnConfig colCpl = new MyColumnConfig(capaianLulusan.getKode());
				colCpl.setWidth("40px");
				colCpl.setTooltiptext(capaianLulusan.getKode() + " " + capaianLulusan.getNama());
				colCpl.setParent(columns);
			}

			MyColumnConfig colEmpty1 = new MyColumnConfig();
			colEmpty1.setParent(columns);

			MyColumnConfig colEmpty2 = new MyColumnConfig("");
			colEmpty2.setWidth("80px");
			colEmpty2.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);
			reloadPl(rows, capaianLulusans, profilLulusans);
		} finally {
			closeSessionQuietly(session);
		}
	}

	// ============================================================
	// AI Generate PL - tampilGenerateAiPl, panggilAi, aiKonfigNilai
	// ============================================================

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void tampilGenerateAiPl() throws Exception {
		// Gather matakuliah data in ZK event thread before spawning background thread
		final String namaMk = matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final String deskripMk = matakuliah.getKeterangan() != null ? matakuliah.getKeterangan() : "";
		final String namaJurusan;
		final List<ProfilLulusan> semuaPlProdi;

		Session sesScan = null;
		try {
			sesScan = HibernateUtil.openSession();
			namaJurusan = (matakuliah.getJurusan() != null && matakuliah.getJurusan().getNama() != null)
					? matakuliah.getJurusan().getNama() : "";
			Long jId = (matakuliah.getJurusan() != null) ? matakuliah.getJurusan().getId() : null;
			if (jId != null) {
				semuaPlProdi = ConstantValues.simpleList(
						sesScan.createCriteria(ProfilLulusan.class)
								.add(Restrictions.eq("jurusan.id", jId))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
						ProfilLulusan.class);
			} else {
				semuaPlProdi = new ArrayList<ProfilLulusan>();
			}
		} finally {
			closeSessionQuietly(sesScan);
		}

		// Build prompt
		StringBuilder psb = new StringBuilder();
		psb.append("Nama Matakuliah: ").append(namaMk).append("\n");
		psb.append("Kode: ").append(kodeMk).append("\n");
		if (!deskripMk.isEmpty()) {
			psb.append("Deskripsi: ").append(deskripMk).append("\n");
		}
		psb.append("Program Studi: ").append(namaJurusan).append("\n");
		psb.append("\nDaftar Profil Lulusan yang tersedia di program studi ini:\n");
		if (semuaPlProdi.isEmpty()) {
			psb.append("(Belum ada Profil Lulusan terdaftar)\n");
		} else {
			for (ProfilLulusan pl : semuaPlProdi) {
				psb.append(pl.getKode()).append(" - ").append(pl.getNama()).append("\n");
			}
		}
		psb.append("\nTolong analisis: Profil Lulusan mana yang paling cocok untuk matakuliah ini?\n");
		psb.append("Juga usulkan Profil Lulusan BARU jika ada yang relevan tapi belum ada di daftar.\n\n");
		psb.append("Format jawaban WAJIB (jangan tambah teks lain di luar format ini):\n");
		psb.append("COCOK: [kode1, kode2, ...]\n");
		psb.append("ALASAN_COCOK: [alasan singkat mengapa profil-profil tersebut cocok untuk matakuliah ini]\n");
		psb.append("USUL_BARU:\n");
		psb.append("- [KODE_BARU]: [deskripsi singkat profil lulusan baru yang disarankan]\n");
		psb.append("(tulis TIDAK ADA jika tidak ada usulan baru)\n");
		final String promptAi = psb.toString();

		// Show loading modal window
		final MyWindow loadingWin = new MyWindow(
				Common.getBahasaConfig("Generate PL berdasarkan AI"), "none", false);
		loadingWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		loadingWin.setWidth("560px");
		loadingWin.setClosable(false);

		Vbox loadingVbox = new Vbox();
		loadingVbox.setAlign("center");
		loadingVbox.setHflex("1");
		loadingVbox.setStyle("padding:20px;text-align:center;");
		loadingVbox.setParent(loadingWin);

		Label loadingLbl = new Label(
				Common.getBahasaConfig("AI sedang menyusun jawaban..."));
		loadingLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
		loadingLbl.setParent(loadingVbox);

		Label loadingLbl2 = new Label(
				Common.getBahasaConfig("Teks di bawah muncul langsung saat AI mengetik."));
		loadingLbl2.setStyle("font-size:11px;color:#64748b;margin-top:6px;");
		loadingLbl2.setParent(loadingVbox);

		// Area STREAMING: teks yang sedang diketik AI (diperbarui oleh pollingTimer).
		final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
		streamBox.setMultiline(true);
		streamBox.setReadonly(true);
		streamBox.setRows(9);
		streamBox.setHflex("1");
		streamBox.setStyle("width:100%;margin-top:12px;font-family:monospace;font-size:11px;"
				+ "color:#334155;background:#f8fafc;");
		streamBox.setParent(loadingVbox);

		loadingWin.onModal();

		// Shared result array: null=not done, ""=error/empty, else=response text
		final String[] aiResult = new String[]{ null };
		final String[] aiErrorMsg = new String[]{ "" };
		final StringBuffer aiStream = new StringBuffer();

		// Background thread for AI HTTP call (STREAMING → token disalurkan ke aiStream)
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String r = panggilAi(promptAi, aiStream);
					aiResult[0] = (r == null) ? "" : r;
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "RpsObeAction.tampilGenerateAiPl.thread");
					aiResult[0] = "";
					aiErrorMsg[0] = (e.getMessage() != null) ? e.getMessage() : "Unknown error";
				}
			}
		}).start();

		// ZK timer to poll the result every 1.5 seconds and update UI
		final org.zkoss.zul.Timer pollingTimer = new org.zkoss.zul.Timer(1500);
		pollingTimer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		pollingTimer.setRepeats(true);
		pollingTimer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event evtTimer) throws Exception {
				// Perbarui area streaming tiap tik: tampilkan teks yang sudah diketik AI sejauh ini.
				try {
					String cur = aiStream.toString();
					if (cur.length() > 0 && !cur.equals(streamBox.getValue())) {
						streamBox.setValue(cur);
						// Auto-scroll ke bawah agar token terbaru terlihat.
						org.zkoss.zk.ui.util.Clients.scrollIntoView(streamBox);
					}
				} catch (Exception igStream) {
				}
				if (aiResult[0] == null) {
					return; // still running
				}
				// Done — stop timer and close loading window
				pollingTimer.stop();
				pollingTimer.detach();
				loadingWin.detach();

				final String resp = aiResult[0];
				if (resp.isEmpty()) {
					MyMessageboxConfig.show(
							Common.getBahasaConfig(
									"AI tidak dapat diakses saat ini. Periksa konfigurasi AI_PROVIDER_AKTIF dan API key.")
									+ (aiErrorMsg[0].isEmpty() ? "" : "\n" + aiErrorMsg[0]),
							Common.getBahasaConfig("Informasi"),
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				// ---- Parse AI response ----
				final List<String> cocokKodes = new ArrayList<String>();
				final String[] alasanHolder = new String[]{ "" };
				final List<String[]> usulBaru = new ArrayList<String[]>(); // [kode, nama]

				String[] lines = resp.split("\\n");
				boolean inUsulBaru = false;
				for (String l : lines) {
					String t = l.trim();
					String tu = t.toUpperCase();
					if (tu.startsWith("COCOK:")) {
						String val = t.substring("COCOK:".length()).trim().replaceAll("[\\[\\]]", "");
						for (String part : val.split("[,;]")) {
							String kd = part.trim();
							if (!kd.isEmpty()) {
								cocokKodes.add(kd.toUpperCase());
							}
						}
						inUsulBaru = false;
					} else if (tu.startsWith("ALASAN_COCOK:")) {
						alasanHolder[0] = t.substring("ALASAN_COCOK:".length()).trim()
								.replaceAll("[\\[\\]]", "").trim();
						inUsulBaru = false;
					} else if (tu.startsWith("USUL_BARU:")) {
						inUsulBaru = true;
					} else if (inUsulBaru && t.startsWith("-")) {
						String item = t.substring(1).trim();
						int ci = item.indexOf(":");
						if (ci > 0) {
							String ubKode = item.substring(0, ci).trim().replaceAll("[\\[\\]]", "");
							String ubNama = item.substring(ci + 1).trim().replaceAll("[\\[\\]]", "");
							if (!ubKode.isEmpty() && !ubNama.isEmpty()
									&& !ubKode.equalsIgnoreCase("TIDAK ADA")) {
								usulBaru.add(new String[]{ ubKode, ubNama });
							}
						}
					}
				}

				// Match cocok codes against the loaded PL list
				final List<ProfilLulusan> direkomendasikan = new ArrayList<ProfilLulusan>();
				for (ProfilLulusan pl : semuaPlProdi) {
					for (String kd : cocokKodes) {
						if (kd.equalsIgnoreCase(pl.getKode() != null ? pl.getKode().trim() : "")) {
							direkomendasikan.add(pl);
							break;
						}
					}
				}

				// ---- Build result popup ----
				final MyWindow resultWin = new MyWindow(
						Common.getBahasaConfig("Rekomendasi AI - Profil Lulusan"), "none", true);
				resultWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				resultWin.setWidth("620px");
				resultWin.setHeight("80%");

				org.zkoss.zk.ui.Component scrollHost = Common.tampilanScroll(resultWin);
				Vbox mainVbox = new Vbox();
				mainVbox.setStyle("padding:16px;width:100%;box-sizing:border-box;");
				mainVbox.setWidth("100%");
				mainVbox.setParent(scrollHost);

				// Alasan block
				if (!alasanHolder[0].isEmpty()) {
					Label lblAlasan = new Label(
							Common.getBahasaConfig("Alasan AI") + ": " + alasanHolder[0]);
					lblAlasan.setStyle(
							"font-size:12px;color:#475569;background:#f1f5f9;padding:8px 12px;"
							+ "border-radius:6px;display:block;margin-bottom:10px;");
					lblAlasan.setMultiline(true);
					mainVbox.appendChild(lblAlasan);
				}

				// Section: Recommended PLs
				Label lblCocok = new Label(
						Common.getBahasaConfig("Profil Lulusan yang Direkomendasikan"));
				lblCocok.setStyle(TITLE_STYLE);
				mainVbox.appendChild(lblCocok);

				final List<Checkbox> cbCocok = new ArrayList<Checkbox>();
				if (direkomendasikan.isEmpty()) {
					Label lblNone = new Label("("
							+ Common.getBahasaConfig(
									"Tidak ada PL yang sesuai ditemukan atau semua sudah ditambahkan")
							+ ")");
					lblNone.setStyle("font-size:12px;color:#94a3b8;");
					mainVbox.appendChild(lblNone);
				} else {
					for (ProfilLulusan pl : direkomendasikan) {
						Hbox plRow = new Hbox();
						plRow.setStyle("margin-bottom:6px;");
						mainVbox.appendChild(plRow);
						Checkbox cb = new Checkbox();
						cb.setChecked(true);
						cb.setLabel(pl.getKode() + " - " + pl.getNama());
						cb.setValue(String.valueOf(pl.getId()));
						cb.setParent(plRow);
						cbCocok.add(cb);
					}
				}

				// Section: Suggested new PLs
				final List<Checkbox> cbUsulBaru = new ArrayList<Checkbox>();
				if (!usulBaru.isEmpty()) {
					Label lblUsul = new Label(
							Common.getBahasaConfig("Usulan Profil Lulusan Baru"));
					lblUsul.setStyle(TITLE_STYLE);
					mainVbox.appendChild(lblUsul);

					Label lblUsulInfo = new Label(Common.getBahasaConfig(
							"Centang untuk membuat PL baru di database dan menambahkannya ke matakuliah ini."));
					lblUsulInfo.setStyle("font-size:11px;color:#64748b;margin-bottom:6px;");
					mainVbox.appendChild(lblUsulInfo);

					for (String[] ub : usulBaru) {
						Hbox ubRow = new Hbox();
						ubRow.setStyle("margin-bottom:6px;");
						mainVbox.appendChild(ubRow);
						Checkbox cb = new Checkbox();
						cb.setChecked(false);
						cb.setLabel("[" + ub[0] + "] " + ub[1]);
						cb.setValue(ub[0]);
						cb.setParent(ubRow);
						cbUsulBaru.add(cb);
					}
				}

				// Buttons
				Hbox btnBox = new Hbox();
				btnBox.setStyle("margin-top:16px;");
				mainVbox.appendChild(btnBox);

				final List<String[]> usulBaruFinal = usulBaru;

				MyToolbarbuttonConfig btnTerapkan = new MyToolbarbuttonConfig(
						Common.getBahasaConfig("Terapkan"), "/img/svg/check2.svg");
				btnTerapkan.setStyle(BTN_SUCCESS);
				btnTerapkan.setParent(btnBox);
				btnTerapkan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event evtApply) throws Exception {
						// Add recommended existing PLs
						for (Checkbox cb : cbCocok) {
							if (cb.isChecked() && cb.getValue() != null
									&& !((String) cb.getValue()).trim().isEmpty()) {
								try {
									Long plId = Long.parseLong(((String) cb.getValue()).trim());
									matakuliah.setProfilLulusan(
											appendIdToString(matakuliah.getProfilLulusan(), plId));
								} catch (NumberFormatException nfe) { /* skip invalid */ }
							}
						}
						// Save new PLs to DB and add to matakuliah
						if (!cbUsulBaru.isEmpty()) {
							Session saveS = null;
							try {
								saveS = HibernateUtil.openSession();
								saveS.beginTransaction();
								for (int idx = 0; idx < cbUsulBaru.size(); idx++) {
									Checkbox cb = cbUsulBaru.get(idx);
									if (cb.isChecked() && idx < usulBaruFinal.size()) {
										String[] ub = usulBaruFinal.get(idx);
										ProfilLulusan newPl = new ProfilLulusan();
										newPl.setKode(ub[0]);
										newPl.setNama(ub[1]);
										newPl.setJurusan(matakuliah.getJurusan());
										saveS.save(newPl);
										saveS.flush();
										if (newPl.getId() != null) {
											matakuliah.setProfilLulusan(
													appendIdToString(matakuliah.getProfilLulusan(),
															newPl.getId()));
										}
									}
								}
								saveS.getTransaction().commit();
							} catch (Exception eSave) {
								if (saveS != null && saveS.getTransaction() != null) {
									try { saveS.getTransaction().rollback(); } catch (Exception er) { ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java"); }
								}
								ais.common.ErrorAuditUtil.record(eSave, "RpsObeAction.terapkanAiPl");
							} finally {
								closeSessionQuietly(saveS);
							}
						}
						Common.refreshUpdate(matakuliah);
						resultWin.detach();
						onSearchDefault(null);
					}
				});

				MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig(
						Common.getBahasaConfig("Batal"), "/img/svg/close-circle-line.svg");
				btnBatal.setStyle(BTN_DANGER);
				btnBatal.setParent(btnBox);
				btnBatal.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event evtBatal) throws Exception {
						resultWin.detach();
					}
				});

				resultWin.onModal();
			}
		});
		pollingTimer.start();
	}

	/** Callback saat streaming AI selesai (dipanggil di thread event ZK). */
	private interface HasilAiListener {
		void selesai(String hasil) throws Exception;
	}

	/**
	 * Menjalankan panggilan AI STREAMING dengan popup progres (teks muncul saat AI mengetik), lalu
	 * memanggil {@code cb.selesai(hasil)} di thread event ZK. Dipakai bersama Generate CPL/CPMK/Deskripsi.
	 */
	private static String potongKonteks(String s, int maks) {
		if (s == null) {
			return "";
		}
		String t = s.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
		return t.length() > maks ? t.substring(0, maks) + "…" : t;
	}

	private void tambahKonteksJika(StringBuilder c, String label, String val) {
		String v = potongKonteks(val, 500);
		if (v.length() > 0) {
			c.append(label).append(": ").append(v).append("\n");
		}
	}

	/** Daftar "kode: nama" dari CSV id untuk ProfilLulusan/CapaianLulusan/CapaianPembelajaranLulusan. */
	private String daftarKodeNama(String csv, Class clazz) {
		StringBuilder sb = new StringBuilder();
		if (csv == null) {
			return "";
		}
		for (String s : csv.split(",")) {
			s = s.trim();
			if (s.length() == 0) {
				continue;
			}
			try {
				Object o = ConstantValues.ambil(clazz.getName(), Long.parseLong(s));
				if (o == null) {
					continue;
				}
				String kode = "";
				String nama = "";
				if (o instanceof ProfilLulusan) {
					kode = safeTrim(((ProfilLulusan) o).getKode());
					nama = safeTrim(((ProfilLulusan) o).getNama());
				} else if (o instanceof CapaianLulusan) {
					kode = safeTrim(((CapaianLulusan) o).getKode());
					nama = safeTrim(((CapaianLulusan) o).getNama());
				} else if (o instanceof CapaianPembelajaranLulusan) {
					CapaianPembelajaranLulusan c = (CapaianPembelajaranLulusan) o;
					kode = safeTrim(c.getKode());
					nama = safeTrim(c.getNama());
					sb.append("  - ").append(kode).append(kode.length() > 0 ? ": " : "").append(nama).append("\n");
					try {
						org.json.JSONArray fa = new org.json.JSONArray(c.getFormula());
						for (int i = 0; i < fa.length(); i++) {
							JSONObject d = fa.getJSONObject(i);
							if (d.isNull("key")) {
								continue;
							}
							String sk = d.isNull("kode") ? "" : d.get("kode") + "";
							String sn = d.isNull("nama") ? "" : d.get("nama") + "";
							sb.append("      · Sub-CPMK ").append(sk).append(" ").append(sn).append("\n");
						}
					} catch (Exception e) {
					}
					continue;
				}
				sb.append("  - ").append(kode).append(kode.length() > 0 ? ": " : "").append(nama).append("\n");
			} catch (Exception e) {
			}
		}
		return sb.toString();
	}

	/**
	 * Rangkum SEMUA data RPS/OBE yang sudah tersimpan menjadi blok teks konteks, agar setiap Generate AI
	 * mengambil keputusan lebih tepat (nama/SKS/rumpun MK, prodi, deskripsi, PL/CPL/CPMK/Sub-CPMK, bahan
	 * kajian, pustaka, rincian tiap pertemuan, catatan &amp; data OBE, dan CQI).
	 */
	private String bangunKonteksObe() {
		StringBuilder c = new StringBuilder();
		try {
			c.append("=== KONTEKS RPS/OBE YANG SUDAH TERSIMPAN (pakai untuk keputusan yang tepat, jangan diulang di jawaban) ===\n");
			if (matakuliah != null) {
				c.append("Matakuliah: ").append(safeTrim(matakuliah.getNama()));
				if (matakuliah.getKode() != null) {
					c.append(" (").append(matakuliah.getKode()).append(")");
				}
				c.append("\n");
				try {
					if (matakuliah.getKelompokMatakuliah() != null
							&& matakuliah.getKelompokMatakuliah().getNama() != null) {
						c.append("Rumpun/Kelompok MK: ").append(matakuliah.getKelompokMatakuliah().getNama())
								.append("\n");
					}
				} catch (Exception e) {
				}
				try {
					c.append("SKS: ").append(matakuliah.getSks()).append("\n");
				} catch (Exception e) {
				}
				try {
					if (matakuliah.getJurusan() != null) {
						c.append("Program Studi: ").append(safeTrim(matakuliah.getJurusan().getNama())).append("\n");
						if (matakuliah.getJurusan().getKaprodi() != null
								&& matakuliah.getJurusan().getKaprodi().getNama() != null) {
							c.append("Ketua Program Studi: ").append(matakuliah.getJurusan().getKaprodi().getNama())
									.append("\n");
						}
					}
				} catch (Exception e) {
				}
				tambahKonteksJika(c, "Default Deskripsi Pembelajaran", matakuliah.getDeskripsiPembelajaran());
				tambahKonteksJika(c, "Default Capaian/Kompetensi", matakuliah.getCapaianPembelajaranProdi());
			}
			if (kurikulumPunyaMatakuliah != null) {
				try {
					if (kurikulumPunyaMatakuliah.getMinimalKetercapaian() != null) {
						c.append("Nilai Minimal Ketercapaian: ")
								.append(kurikulumPunyaMatakuliah.getMinimalKetercapaian()).append("\n");
					}
				} catch (Exception e) {
				}
				c.append("Penilaian menggunakan CPMK (tanpa Sub-CPMK): ")
						.append(Boolean.TRUE.equals(kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) ? "Ya" : "Tidak")
						.append("\n");
				tambahKonteksJika(c, "Pengembang RPS", kurikulumPunyaMatakuliah.getPengembangRps());
				tambahKonteksJika(c, "Koordinator", kurikulumPunyaMatakuliah.getKoordinator());
				tambahKonteksJika(c, "Mitra Pengembang", kurikulumPunyaMatakuliah.getMitraPengembang());
			}
			if (matakuliah != null) {
				String pl = daftarKodeNama(matakuliah.getProfilLulusan(), ProfilLulusan.class);
				if (pl.length() > 0) {
					c.append("Profil Lulusan (PL):\n").append(pl);
				}
				String cpl = daftarKodeNama(matakuliah.getCapaianLulusan(), CapaianLulusan.class);
				if (cpl.length() > 0) {
					c.append("Capaian Lulusan (CPL):\n").append(cpl);
				}
				String cpmk = daftarKodeNama(matakuliah.getCapaianPembelajaranLulusan(),
						CapaianPembelajaranLulusan.class);
				if (cpmk.length() > 0) {
					c.append("CPMK (+ Sub-CPMK):\n").append(cpmk);
				}
			}
			if (kurikulumPunyaMatakuliah != null) {
				tambahKonteksJika(c, "Deskripsi Singkat MK", kurikulumPunyaMatakuliah.getDeskripsiPembelajaran());
			}
			if (matakuliah != null) {
				java.util.List<String[]> bk = ambilBahanKajianList(matakuliah.getBahanKajian());
				if (!bk.isEmpty()) {
					c.append("Bahan Kajian: ");
					for (String[] x : bk) {
						c.append(x[1]).append("; ");
					}
					c.append("\n");
				}
			}
			if (kurikulumPunyaMatakuliah != null) {
				java.util.List<String[]> pu = ambilReferensiList(kurikulumPunyaMatakuliah.getPustaka());
				if (!pu.isEmpty()) {
					c.append("Pustaka Utama: ");
					for (String[] x : pu) {
						c.append(x[1]).append("; ");
					}
					c.append("\n");
				}
				java.util.List<String[]> pp = ambilReferensiList(kurikulumPunyaMatakuliah.getPustakaPendukung());
				if (!pp.isEmpty()) {
					c.append("Pustaka Pendukung: ");
					for (String[] x : pp) {
						c.append(x[1]).append("; ");
					}
					c.append("\n");
				}
				// Rincian ringkas
				try {
					java.util.TreeMap maps = kurikulumPunyaMatakuliah
							.populateRinci(new JSONObject(kurikulumPunyaMatakuliah.getRincian()));
					if (!maps.isEmpty()) {
						c.append("Rincian/Agenda tersimpan:\n");
						for (Object mo : maps.values()) {
							Object jo = ((java.util.Map) mo).get("jsonObject");
							if (jo instanceof JSONObject) {
								JSONObject r = (JSONObject) jo;
								c.append("  - Minggu ").append(r.optInt("mulaiMingguKe", 0)).append("-")
										.append(r.optInt("sampaiMingguKe", 0)).append(": ")
										.append(potongKonteks(r.optString("sub_cpmk_des", ""), 60)).append(" | ")
										.append(potongKonteks(r.optString("indikator", ""), 80)).append("\n");
							}
						}
					}
				} catch (Exception e) {
				}
				tambahKonteksJika(c, "Catatan", kurikulumPunyaMatakuliah.getCatatan());
				tambahKonteksJika(c, "Bobot CPL per MK", kurikulumPunyaMatakuliah.getCplBobot());
				tambahKonteksJika(c, "Komponen Penilaian (%)", kurikulumPunyaMatakuliah.getKomponenPenilaian());
				tambahKonteksJika(c, "Teknik Penilaian per CPMK", kurikulumPunyaMatakuliah.getTeknikPerCpmk());
				tambahKonteksJika(c, "Rubrik Penilaian", kurikulumPunyaMatakuliah.getRubrikPenilaian());
				tambahKonteksJika(c, "Pemetaan Soal UTS", kurikulumPunyaMatakuliah.getPemetaanSoalUts());
				tambahKonteksJika(c, "Pemetaan Soal UAS", kurikulumPunyaMatakuliah.getPemetaanSoalUas());
			}
			// CQI ringkas
			try {
				if (perkuliahan != null && perkuliahan.getCqiData() != null
						&& perkuliahan.getCqiData().trim().length() > 0) {
					org.json.JSONArray arr = new org.json.JSONArray(perkuliahan.getCqiData());
					if (arr.length() > 0) {
						c.append("Analisis Dosen per CPMK (CQI) tersimpan:\n");
						for (int i = 0; i < arr.length(); i++) {
							JSONObject e = arr.optJSONObject(i);
							if (e == null) {
								continue;
							}
							String ms = potongKonteks(e.optString("masalah", ""), 80);
							if (ms.length() == 0) {
								continue;
							}
							c.append("  - ").append(e.optString("cpmk", "")).append(": masalah=").append(ms)
									.append("; rencana=").append(potongKonteks(e.optString("rencana", ""), 80))
									.append("\n");
						}
					}
				}
			} catch (Exception e) {
			}
			c.append("=== AKHIR KONTEKS ===\n\n");
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "RpsObeAction.bangunKonteksObe");
		}
		return c.toString();
	}

	private void jalankanAiStreaming(final String judul, final String prompt, final HasilAiListener cb)
			throws Exception {
		final MyWindow loadingWin = new MyWindow(judul, "none", false);
		loadingWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		loadingWin.setWidth("560px");
		loadingWin.setClosable(false);

		Vbox loadingVbox = new Vbox();
		loadingVbox.setAlign("center");
		loadingVbox.setHflex("1");
		loadingVbox.setStyle("padding:20px;text-align:center;");
		loadingVbox.setParent(loadingWin);

		Label loadingLbl = new Label(Common.getBahasaConfig("AI sedang menyusun jawaban..."));
		loadingLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
		loadingLbl.setParent(loadingVbox);

		Label loadingLbl2 = new Label(Common.getBahasaConfig("Teks di bawah muncul langsung saat AI mengetik."));
		loadingLbl2.setStyle("font-size:11px;color:#64748b;margin-top:6px;");
		loadingLbl2.setParent(loadingVbox);

		final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
		streamBox.setMultiline(true);
		streamBox.setReadonly(true);
		streamBox.setRows(9);
		streamBox.setHflex("1");
		streamBox.setStyle("width:100%;margin-top:12px;font-family:monospace;font-size:11px;"
				+ "color:#334155;background:#f8fafc;");
		streamBox.setParent(loadingVbox);

		loadingWin.onModal();

		final String[] aiResult = new String[]{ null };
		final String[] aiErrorMsg = new String[]{ "" };
		final StringBuffer aiStream = new StringBuffer();

		// Prepend KONTEKS RPS/OBE tersimpan yang komprehensif agar keputusan AI lebih tepat.
		final String promptFull = bangunKonteksObe() + prompt;

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String r = panggilAi(promptFull, aiStream);
					aiResult[0] = (r == null) ? "" : r;
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "RpsObeAction.jalankanAiStreaming.thread");
					aiResult[0] = "";
					aiErrorMsg[0] = (e.getMessage() != null) ? e.getMessage() : "Unknown error";
				}
			}
		}).start();

		final org.zkoss.zul.Timer pollingTimer = new org.zkoss.zul.Timer(1200);
		pollingTimer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		pollingTimer.setRepeats(true);
		pollingTimer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event evtTimer) throws Exception {
				try {
					String cur = aiStream.toString();
					if (cur.length() > 0 && !cur.equals(streamBox.getValue())) {
						streamBox.setValue(cur);
						org.zkoss.zk.ui.util.Clients.scrollIntoView(streamBox);
					}
				} catch (Exception ig) {
				}
				if (aiResult[0] == null) {
					return;
				}
				pollingTimer.stop();
				pollingTimer.detach();
				loadingWin.detach();
				final String resp = aiResult[0];
				if (resp == null || resp.trim().length() == 0) {
					MyMessageboxConfig.show(
							Common.getBahasaConfig("AI tidak dapat diakses saat ini. Coba lagi beberapa saat.")
									+ (aiErrorMsg[0].isEmpty() ? "" : "\n" + aiErrorMsg[0]),
							Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				try {
					cb.selesai(resp);
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "RpsObeAction.jalankanAiStreaming.cb");
				}
			}
		});
		pollingTimer.start();
	}

	private String csvSeleksiMk(String jenis) {
		if ("CPL".equals(jenis)) {
			return matakuliah.getCapaianLulusan();
		}
		return matakuliah.getCapaianPembelajaranLulusan();
	}

	private void setCsvSeleksiMk(String jenis, String csv) {
		if ("CPL".equals(jenis)) {
			matakuliah.setCapaianLulusan(csv);
		} else {
			matakuliah.setCapaianPembelajaranLulusan(csv);
		}
	}

	/** Ambil pool prodi sebagai String[]{id, kode, nama}. */
	private List<String[]> ambilPoolSeleksi(String jenis, Session ses, Long jId) {
		List<String[]> hasil = new ArrayList<String[]>();
		if (jId == null) {
			return hasil;
		}
		if ("CPL".equals(jenis)) {
			List<CapaianLulusan> ls = ConstantValues.simpleList(
					ses.createCriteria(CapaianLulusan.class).add(Restrictions.eq("jurusan.id", jId))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
					CapaianLulusan.class);
			for (CapaianLulusan c : ls) {
				hasil.add(new String[]{ String.valueOf(c.getId()), c.getKode() != null ? c.getKode() : "",
						c.getNama() != null ? c.getNama() : "" });
			}
		} else {
			List<CapaianPembelajaranLulusan> ls = ConstantValues.simpleList(
					ses.createCriteria(CapaianPembelajaranLulusan.class).add(Restrictions.eq("jurusan.id", jId))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
					CapaianPembelajaranLulusan.class);
			for (CapaianPembelajaranLulusan c : ls) {
				hasil.add(new String[]{ String.valueOf(c.getId()), c.getKode() != null ? c.getKode() : "",
						c.getNama() != null ? c.getNama() : "" });
			}
		}
		return hasil;
	}

	/** Buat entitas CPL/CPMK baru, simpan, kembalikan id. */
	private Long buatEntitasSeleksi(String jenis, Session saveS, String kode, String nama) {
		if ("CPL".equals(jenis)) {
			CapaianLulusan e = new CapaianLulusan();
			e.setKode(kode);
			e.setNama(nama);
			e.setJurusan(matakuliah.getJurusan());
			e.setKhususBuatMk(matakuliah);
			e.setAktif(Boolean.TRUE);
			saveS.save(e);
			saveS.flush();
			return e.getId();
		}
		CapaianPembelajaranLulusan e = new CapaianPembelajaranLulusan();
		e.setKode(kode);
		e.setNama(nama);
		e.setJurusan(matakuliah.getJurusan());
		e.setKhususBuatMk(matakuliah);
		e.setAktif(Boolean.TRUE);
		saveS.save(e);
		saveS.flush();
		return e.getId();
	}

	/** Generate AI untuk seleksi CPL/CPMK (pola sama dengan Profil Lulusan). jenis = "CPL" | "CPMK". */
	private void tampilGenerateAiSeleksi(final String jenis) throws Exception {
		final boolean isCpl = "CPL".equals(jenis);
		final String namaItem = isCpl ? "Capaian Pembelajaran Lulusan (CPL)"
				: "Capaian Pembelajaran Matakuliah (CPMK)";
		final String judul = Common
				.getBahasaConfig(isCpl ? "Generate CPL berdasarkan AI" : "Generate CPMK berdasarkan AI");

		final String namaMk = matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final String deskripMk = matakuliah.getKeterangan() != null ? matakuliah.getKeterangan() : "";
		final String namaJurusan = (matakuliah.getJurusan() != null && matakuliah.getJurusan().getNama() != null)
				? matakuliah.getJurusan().getNama() : "";
		final Long jId = (matakuliah.getJurusan() != null) ? matakuliah.getJurusan().getId() : null;

		final List<String[]> pool;
		Session sesScan = null;
		try {
			sesScan = HibernateUtil.openSession();
			pool = ambilPoolSeleksi(jenis, sesScan, jId);
		} finally {
			closeSessionQuietly(sesScan);
		}

		StringBuilder psb = new StringBuilder();
		psb.append("Nama Matakuliah: ").append(namaMk).append("\n");
		psb.append("Kode: ").append(kodeMk).append("\n");
		if (!deskripMk.isEmpty()) {
			psb.append("Deskripsi: ").append(deskripMk).append("\n");
		}
		psb.append("Program Studi: ").append(namaJurusan).append("\n");
		psb.append("\nDaftar ").append(namaItem).append(" yang tersedia di program studi ini:\n");
		if (pool.isEmpty()) {
			psb.append("(Belum ada ").append(namaItem).append(" terdaftar)\n");
		} else {
			for (String[] p : pool) {
				psb.append(p[1]).append(" - ").append(p[2]).append("\n");
			}
		}
		psb.append("\nTolong analisis: ").append(namaItem)
				.append(" mana yang paling cocok untuk matakuliah ini?\n");
		psb.append("Juga usulkan ").append(namaItem).append(" BARU jika relevan tapi belum ada di daftar.\n\n");
		psb.append("Format jawaban WAJIB (jangan tambah teks lain di luar format ini):\n");
		psb.append("COCOK: [kode1, kode2, ...]\n");
		psb.append("ALASAN_COCOK: [alasan singkat mengapa cocok untuk matakuliah ini]\n");
		psb.append("USUL_BARU:\n");
		psb.append("- [KODE_BARU]: [deskripsi singkat item baru yang disarankan]\n");
		psb.append("(tulis TIDAK ADA jika tidak ada usulan baru)\n");
		final String promptAi = psb.toString();
		final List<String[]> poolFinal = pool;

		jalankanAiStreaming(judul, promptAi, new HasilAiListener() {
			@Override
			public void selesai(String resp) throws Exception {
				final List<String> cocokKodes = new ArrayList<String>();
				final String[] alasanHolder = new String[]{ "" };
				final List<String[]> usulBaru = new ArrayList<String[]>();
				String[] lines = resp.split("\\n");
				boolean inUsulBaru = false;
				for (String l : lines) {
					String t = l.trim();
					String tu = t.toUpperCase();
					if (tu.startsWith("COCOK:")) {
						String val = t.substring("COCOK:".length()).trim().replaceAll("[\\[\\]]", "");
						for (String part : val.split("[,;]")) {
							String kd = part.trim();
							if (!kd.isEmpty()) {
								cocokKodes.add(kd.toUpperCase());
							}
						}
						inUsulBaru = false;
					} else if (tu.startsWith("ALASAN_COCOK:")) {
						alasanHolder[0] = t.substring("ALASAN_COCOK:".length()).trim().replaceAll("[\\[\\]]", "")
								.trim();
						inUsulBaru = false;
					} else if (tu.startsWith("USUL_BARU:")) {
						inUsulBaru = true;
					} else if (inUsulBaru && t.startsWith("-")) {
						String item = t.substring(1).trim();
						int ci = item.indexOf(":");
						if (ci > 0) {
							String ubKode = item.substring(0, ci).trim().replaceAll("[\\[\\]]", "");
							String ubNama = item.substring(ci + 1).trim().replaceAll("[\\[\\]]", "");
							if (!ubKode.isEmpty() && !ubNama.isEmpty() && !ubKode.equalsIgnoreCase("TIDAK ADA")) {
								usulBaru.add(new String[]{ ubKode, ubNama });
							}
						}
					}
				}

				final List<String[]> direkomendasikan = new ArrayList<String[]>();
				for (String[] p : poolFinal) {
					String kode = p[1] != null ? p[1].trim().toUpperCase() : "";
					for (String kd : cocokKodes) {
						if (kd.equalsIgnoreCase(kode)) {
							direkomendasikan.add(p);
							break;
						}
					}
				}

				final MyWindow resultWin = new MyWindow(
						Common.getBahasaConfig("Rekomendasi AI") + " - " + namaItem, "none", true);
				resultWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				resultWin.setWidth("620px");
				resultWin.setHeight("80%");
				org.zkoss.zk.ui.Component scrollHost = Common.tampilanScroll(resultWin);
				Vbox mainVbox = new Vbox();
				mainVbox.setStyle("padding:16px;width:100%;box-sizing:border-box;");
				mainVbox.setWidth("100%");
				mainVbox.setParent(scrollHost);

				if (!alasanHolder[0].isEmpty()) {
					Label lblAlasan = new Label(Common.getBahasaConfig("Alasan AI") + ": " + alasanHolder[0]);
					lblAlasan.setStyle("font-size:12px;color:#475569;background:#f1f5f9;padding:8px 12px;"
							+ "border-radius:6px;display:block;margin-bottom:10px;");
					lblAlasan.setMultiline(true);
					mainVbox.appendChild(lblAlasan);
				}

				Label lblCocok = new Label(Common.getBahasaConfig("Yang Direkomendasikan") + " (" + namaItem + ")");
				lblCocok.setStyle(TITLE_STYLE);
				mainVbox.appendChild(lblCocok);

				final List<Checkbox> cbCocok = new ArrayList<Checkbox>();
				if (direkomendasikan.isEmpty()) {
					Label lblNone = new Label("(" + Common
							.getBahasaConfig("Tidak ada yang sesuai ditemukan atau semua sudah ditambahkan") + ")");
					lblNone.setStyle("font-size:12px;color:#94a3b8;");
					mainVbox.appendChild(lblNone);
				} else {
					for (String[] p : direkomendasikan) {
						Hbox r = new Hbox();
						r.setStyle("margin-bottom:6px;");
						mainVbox.appendChild(r);
						Checkbox cb = new Checkbox();
						cb.setChecked(true);
						cb.setLabel(p[1] + " - " + p[2]);
						cb.setValue(p[0]);
						cb.setParent(r);
						cbCocok.add(cb);
					}
				}

				final List<Checkbox> cbUsulBaru = new ArrayList<Checkbox>();
				final List<String[]> usulBaruFinal = usulBaru;
				if (!usulBaru.isEmpty()) {
					Label lblUsul = new Label(Common.getBahasaConfig("Usulan Baru") + " (" + namaItem + ")");
					lblUsul.setStyle(TITLE_STYLE);
					mainVbox.appendChild(lblUsul);
					Label lblUsulInfo = new Label(Common.getBahasaConfig(
							"Centang untuk membuat item baru di database dan menambahkannya ke matakuliah ini."));
					lblUsulInfo.setStyle("font-size:11px;color:#64748b;margin-bottom:6px;");
					mainVbox.appendChild(lblUsulInfo);
					for (String[] ub : usulBaru) {
						Hbox r = new Hbox();
						r.setStyle("margin-bottom:6px;");
						mainVbox.appendChild(r);
						Checkbox cb = new Checkbox();
						cb.setChecked(false);
						cb.setLabel("[" + ub[0] + "] " + ub[1]);
						cb.setValue(ub[0]);
						cb.setParent(r);
						cbUsulBaru.add(cb);
					}
				}

				Hbox btnBox = new Hbox();
				btnBox.setStyle("margin-top:16px;");
				mainVbox.appendChild(btnBox);

				MyToolbarbuttonConfig btnTerapkan = new MyToolbarbuttonConfig(
						Common.getBahasaConfig("Terapkan"), "/img/svg/check2.svg");
				btnTerapkan.setStyle(BTN_SUCCESS);
				btnTerapkan.setParent(btnBox);
				btnTerapkan.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event evtApply) throws Exception {
						for (Checkbox cb : cbCocok) {
							if (cb.isChecked() && cb.getValue() != null
									&& !((String) cb.getValue()).trim().isEmpty()) {
								try {
									Long id = Long.parseLong(((String) cb.getValue()).trim());
									setCsvSeleksiMk(jenis, appendIdToString(csvSeleksiMk(jenis), id));
								} catch (NumberFormatException nfe) {
								}
							}
						}
						if (!cbUsulBaru.isEmpty()) {
							Session saveS = null;
							try {
								saveS = HibernateUtil.openSession();
								saveS.beginTransaction();
								for (int idx = 0; idx < cbUsulBaru.size(); idx++) {
									Checkbox cb = cbUsulBaru.get(idx);
									if (cb.isChecked() && idx < usulBaruFinal.size()) {
										String[] ub = usulBaruFinal.get(idx);
										Long nid = buatEntitasSeleksi(jenis, saveS, ub[0], ub[1]);
										if (nid != null) {
											setCsvSeleksiMk(jenis, appendIdToString(csvSeleksiMk(jenis), nid));
										}
									}
								}
								saveS.getTransaction().commit();
							} catch (Exception eSave) {
								if (saveS != null && saveS.getTransaction() != null) {
									try {
										saveS.getTransaction().rollback();
									} catch (Exception er) {
									}
								}
								ais.common.ErrorAuditUtil.record(eSave, "RpsObeAction.terapkanAiSeleksi");
							} finally {
								closeSessionQuietly(saveS);
							}
						}
						Common.refreshUpdate(matakuliah);
						resultWin.detach();
						onSearchDefault(null);
					}
				});

				MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig(
						Common.getBahasaConfig("Batal"), "/img/svg/close-circle-line.svg");
				btnBatal.setStyle(BTN_DANGER);
				btnBatal.setParent(btnBox);
				btnBatal.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event evtBatal) throws Exception {
						resultWin.detach();
					}
				});

				resultWin.onModal();
			}
		});
	}

	/** Generate AI untuk Deskripsi Mata Kuliah (teks tunggal → editable → disimpan ke KurikulumPunyaMatakuliah). */
	private void tampilGenerateAiDeskripsi() throws Exception {
		final String namaMk = matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final String namaJurusan = (matakuliah.getJurusan() != null && matakuliah.getJurusan().getNama() != null)
				? matakuliah.getJurusan().getNama() : "";
		final String deskripLama = (kurikulumPunyaMatakuliah != null
				&& kurikulumPunyaMatakuliah.getDeskripsiPembelajaran() != null)
						? kurikulumPunyaMatakuliah.getDeskripsiPembelajaran() : "";

		StringBuilder psb = new StringBuilder();
		psb.append("Buatkan DESKRIPSI SINGKAT mata kuliah (1-2 paragraf, Bahasa Indonesia formal-akademis). ");
		psb.append("Jawab HANYA isi deskripsinya, tanpa judul, tanpa penanda format, tanpa poin.\n\n");
		psb.append("Nama Matakuliah: ").append(namaMk).append("\n");
		psb.append("Kode: ").append(kodeMk).append("\n");
		psb.append("Program Studi: ").append(namaJurusan).append("\n");
		if (!deskripLama.isEmpty()) {
			psb.append("Deskripsi saat ini (boleh diperbaiki/dilengkapi): ").append(deskripLama).append("\n");
		}
		final String promptAi = psb.toString();

		jalankanAiStreaming(Common.getBahasaConfig("Generate Deskripsi berdasarkan AI"), promptAi,
				new HasilAiListener() {
					@Override
					public void selesai(String resp) throws Exception {
						final String hasil = resp.trim();
						final MyWindow resultWin = new MyWindow(
								Common.getBahasaConfig("Rekomendasi AI - Deskripsi Mata Kuliah"), "none", true);
						resultWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						resultWin.setWidth("620px");
						org.zkoss.zk.ui.Component scrollHost = Common.tampilanScroll(resultWin);
						Vbox mainVbox = new Vbox();
						mainVbox.setStyle("padding:16px;width:100%;box-sizing:border-box;");
						mainVbox.setWidth("100%");
						mainVbox.setParent(scrollHost);

						Label info = new Label(Common
								.getBahasaConfig("Tinjau/edit deskripsi dari AI lalu Terapkan untuk menyimpannya."));
						info.setStyle("font-size:11px;color:#64748b;margin-bottom:8px;display:block;");
						mainVbox.appendChild(info);

						final org.zkoss.zul.Textbox boxEdit = new org.zkoss.zul.Textbox();
						boxEdit.setMultiline(true);
						boxEdit.setRows(10);
						boxEdit.setWidth("100%");
						boxEdit.setValue(hasil);
						boxEdit.setParent(mainVbox);

						Hbox btnBox = new Hbox();
						btnBox.setStyle("margin-top:16px;");
						mainVbox.appendChild(btnBox);

						MyToolbarbuttonConfig btnTerapkan = new MyToolbarbuttonConfig(
								Common.getBahasaConfig("Terapkan"), "/img/svg/check2.svg");
						btnTerapkan.setStyle(BTN_SUCCESS);
						btnTerapkan.setParent(btnBox);
						btnTerapkan.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event evtApply) throws Exception {
								String txt = boxEdit.getValue() != null ? boxEdit.getValue().trim() : "";
								if (kurikulumPunyaMatakuliah != null) {
									kurikulumPunyaMatakuliah.setDeskripsiPembelajaran(txt);
									Common.refreshUpdate(kurikulumPunyaMatakuliah);
								}
								if (deskripsiPembelajaran != null) {
									deskripsiPembelajaran.setValue(txt);
								}
								resultWin.detach();
								onSearchDefault(null);
							}
						});

						MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig(
								Common.getBahasaConfig("Batal"), "/img/svg/close-circle-line.svg");
						btnBatal.setStyle(BTN_DANGER);
						btnBatal.setParent(btnBox);
						btnBatal.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event evtBatal) throws Exception {
								resultWin.detach();
							}
						});

						resultWin.onModal();
					}
				});
	}

	/**
	 * Calls the configured AI provider (Gemini/Groq/OpenAI/DeepSeek/Ollama)
	 * using OpenAI-compatible chat completions. Reads all settings from Konfigurasi.
	 * Returns the assistant text content, or "" on any error.
	 */
	private static String panggilAi(String prompt) {
		return panggilAi(prompt, 1, null);
	}

	/** Varian STREAMING: token AI disalurkan ke {@code sink} saat tiba (untuk progres langsung di UI). */
	private static String panggilAi(String prompt, StringBuffer sink) {
		return panggilAi(prompt, 1, sink);
	}

	private static String panggilAi(String prompt, int attempt, StringBuffer sink) {
		java.net.HttpURLConnection conn = null;
		try {
			// Determine provider
			String provider = aiKonfigNilai("AI_PROVIDER_AKTIF", "GEMINI").toUpperCase();

			// Build credentials and base URL based on provider
			String apiKey;
			String baseUrl;
			String model;
			if ("GEMINI".equals(provider)) {
				apiKey   = aiKonfigNilai("AI_GEMINI_KEY",
						"AIzaSyAFSzVMA8o9DWZpHCsTQT8Mf4M5SN77e2E");
				baseUrl  = aiKonfigNilai("AI_GEMINI_BASE_URL",
						"https://generativelanguage.googleapis.com/v1beta/openai");
				model    = aiKonfigNilai("AI_GEMINI_MODEL", "gemini-1.5-flash");
			} else if ("GROQ".equals(provider)) {
				apiKey   = aiKonfigNilai("AI_GROQ_KEY", "");
				baseUrl  = aiKonfigNilai("AI_GROQ_BASE_URL", "https://api.groq.com/openai/v1");
				model    = aiKonfigNilai("AI_GROQ_MODEL", "llama-3.1-8b-instant");
			} else if ("OPENAI".equals(provider)) {
				apiKey   = aiKonfigNilai("AI_OPENAI_CLOUD_KEY", "");
				baseUrl  = aiKonfigNilai("AI_OPENAI_CLOUD_BASE_URL", "https://api.openai.com/v1");
				model    = aiKonfigNilai("AI_OPENAI_CLOUD_MODEL", "gpt-4o-mini");
			} else if ("DEEPSEEK".equals(provider)) {
				apiKey   = aiKonfigNilai("AI_DEEPSEEK_KEY", "");
				baseUrl  = aiKonfigNilai("AI_DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1");
				model    = aiKonfigNilai("AI_DEEPSEEK_MODEL", "deepseek-chat");
			} else {
				// OLLAMA_LOCAL, OLLAMA_PROXY or any other
				apiKey   = aiKonfigNilai("AI_OPENAI_KEY", "");
				baseUrl  = aiKonfigNilai("AI_OPENAI_BASE_URL",
						"http://localhost:11434/v1");
				model    = aiKonfigNilai("AI_OPENAI_MODEL", "qwen2.5:1.5b-instruct-q4_K_M");
			}

			// Override with explicit full URL if set
			String fullUrl = aiKonfigNilai("AI_OPENAI_URL", "");
			if (!fullUrl.isEmpty()) {
				baseUrl = fullUrl;
			}

			// PAKSA ke server AI & model RINGAN yang ditentukan. Kunci config BARU (ai_rpsobe_*) sengaja
			// dipakai agar tak terikat baris DB lama; ini MENANG atas pilihan provider/URL di atas.
			// Ollama tak butuh API key → kosongkan (tak akan kirim header Authorization).
			baseUrl = aiKonfigNilai("ai_rpsobe_base_url", "http://38.47.182.162:11434/v1");
			model = aiKonfigNilai("ai_rpsobe_model", "qwen2.5:1.5b-instruct-q4_K_M");
			apiKey = aiKonfigNilai("ai_rpsobe_api_key", "");

			int timeoutMs = 60000;
			try {
				timeoutMs = (int) Double.parseDouble(
						aiKonfigNilai("AI_TIMEOUT_MS", "60000"));
			} catch (NumberFormatException nfe) { /* use default */ }

			// Build endpoint URL from base URL
			String endpoint = baseUrl.trim();
			while (endpoint.endsWith("/")) {
				endpoint = endpoint.substring(0, endpoint.length() - 1);
			}
			if (!endpoint.endsWith("/chat/completions")
					&& !endpoint.endsWith("/v1/chat/completions")) {
				if (endpoint.endsWith("/v1") || endpoint.endsWith("/openai")
						|| endpoint.endsWith("/ai/v1")) {
					endpoint = endpoint + "/chat/completions";
				} else {
					endpoint = endpoint + "/v1/chat/completions";
				}
			}

			// Build OpenAI-compatible JSON payload
			String systemPrompt = "Anda adalah asisten akademik berbasis OBE (Outcome-Based Education) "
					+ "untuk perguruan tinggi di Indonesia. "
					+ "Gunakan Bahasa Indonesia yang formal dan akademis. "
					+ "Ikuti format jawaban yang diminta dengan tepat.";

			JSONObject payload = new JSONObject();
			payload.put("model", model);
			payload.put("stream", sink != null);
			payload.put("temperature", 0.4);
			payload.put("max_tokens", 1500);
			JSONArray messages = new JSONArray();
			JSONObject sysMsg = new JSONObject();
			sysMsg.put("role", "system");
			sysMsg.put("content", systemPrompt);
			messages.put(sysMsg);
			JSONObject userMsg = new JSONObject();
			userMsg.put("role", "user");
			userMsg.put("content", prompt);
			messages.put(userMsg);
			payload.put("messages", messages);

			// HTTP POST
			java.net.URL url = new java.net.URL(endpoint);
			conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(timeoutMs);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");
			if (!apiKey.isEmpty()) {
				conn.setRequestProperty("Authorization", "Bearer " + apiKey);
			}

			byte[] bodyBytes = payload.toString().getBytes("UTF-8");
			java.io.OutputStream os = conn.getOutputStream();
			try {
				os.write(bodyBytes);
				os.flush();
			} finally {
				try { os.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java"); }
			}

			int status = conn.getResponseCode();
			java.io.InputStream is = (status >= 200 && status < 300)
					? conn.getInputStream() : conn.getErrorStream();

			java.io.BufferedReader br = new java.io.BufferedReader(
					new java.io.InputStreamReader(is, "UTF-8"));

			// ---- MODE STREAMING (SSE OpenAI-compatible) ----
			// Baris berbentuk: data: {"choices":[{"delta":{"content":"..."}}]}  ... diakhiri  data: [DONE].
			// Setiap potongan konten disalurkan ke 'sink' agar UI bisa menampilkannya langsung.
			if (sink != null) {
				StringBuilder full = new StringBuilder();
				String errText = null;
				String sline;
				while ((sline = br.readLine()) != null) {
					String t = sline.trim();
					if (t.length() == 0) {
						continue;
					}
					if (t.startsWith("data:")) {
						t = t.substring(5).trim();
					}
					if ("[DONE]".equals(t)) {
						break;
					}
					if (!t.startsWith("{")) {
						errText = t; // baris bukan JSON (mis. teks error "server busy")
						continue;
					}
					try {
						JSONObject o = new JSONObject(t);
						if (o.has("error") && !o.isNull("error")) {
							errText = String.valueOf(o.opt("error"));
							continue;
						}
						JSONArray ch = o.optJSONArray("choices");
						if (ch != null && ch.length() > 0) {
							JSONObject c0 = ch.getJSONObject(0);
							String piece = "";
							JSONObject delta = c0.optJSONObject("delta");
							if (delta != null) {
								piece = delta.optString("content", "");
							}
							if (piece.length() == 0 && c0.has("message")) {
								piece = c0.getJSONObject("message").optString("content", "");
							}
							if (piece.length() > 0) {
								full.append(piece);
								sink.append(piece);
							}
						}
					} catch (Exception exLine) {
						// baris tak ter-parse → lewati
					}
				}
				try { br.close(); } catch (Exception ig) { }
				String hasilStream = full.toString();
				if (hasilStream.trim().length() == 0 && errText != null) {
					ais.common.ErrorAuditUtil.record(new Exception("Ollama stream error: " + errText),
							"RpsObeAction.panggilAi.stream");
					String low = errText.toLowerCase();
					boolean sementara = low.contains("busy") || low.contains("pending")
							|| low.contains("try again") || low.contains("loading")
							|| low.contains("timeout") || low.contains("unavailable");
					if (sementara && attempt < 3) {
						try { Thread.sleep(1500L * attempt); } catch (Exception ie) { }
						return panggilAi(prompt, attempt + 1, sink);
					}
				}
				return hasilStream;
			}

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			try { br.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java"); }

			String rawResp = sb.toString();
			if (rawResp == null || rawResp.trim().isEmpty()) {
				return "";
			}

			// Guard: respons yang BUKAN JSON (mis. teks error Ollama "server busy, maximum pending
			// requests exceeded", halaman HTML 404/502) → JANGAN lempar JSONException. Catat ringkas;
			// bila errornya SEMENTARA (sibuk/antre/muat), tunggu lalu coba lagi (maks 3x).
			String respTrim = rawResp.trim();
			if (!respTrim.startsWith("{") && !respTrim.startsWith("[")) {
				String ringkas = respTrim.length() > 200 ? respTrim.substring(0, 200) : respTrim;
				ais.common.ErrorAuditUtil.record(new Exception("Respons AI bukan JSON: " + ringkas),
						"RpsObeAction.panggilAi.nonjson");
				String low = respTrim.toLowerCase();
				boolean sementara = low.contains("busy") || low.contains("pending")
						|| low.contains("try again") || low.contains("loading")
						|| low.contains("timeout") || low.contains("unavailable");
				if (sementara && attempt < 3) {
					try {
						Thread.sleep(1500L * attempt);
					} catch (Exception ie) {
					}
					return panggilAi(prompt, attempt + 1, sink);
				}
				return "";
			}

			// Parse OpenAI-compatible response
			try {
				JSONObject respJson = new JSONObject(respTrim);
				if (respJson.has("choices")) {
					JSONArray choices = respJson.getJSONArray("choices");
					if (choices.length() > 0) {
						JSONObject choice = choices.getJSONObject(0);
						if (choice.has("message")) {
							JSONObject msg = choice.getJSONObject("message");
							if (msg.has("content")) {
								return msg.getString("content");
							}
						}
						if (choice.has("content")) {
							return choice.getString("content");
						}
					}
				}
				// Ollama fallback
				if (respJson.has("response")) {
					return respJson.getString("response");
				}
				if (respJson.has("text")) {
					return respJson.getString("text");
				}
			} catch (Exception parseEx) {
				ais.common.ErrorAuditUtil.record(parseEx, "RpsObeAction.panggilAi.parse");
			}
			return "";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "RpsObeAction.panggilAi");
			return "";
		} finally {
			if (conn != null) {
				try { conn.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java"); }
			}
		}
	}

	/**
	 * Reads a Konfigurasi value by key; returns {@code defaultValue} if not found.
	 */
	private static String aiKonfigNilai(String key, String defaultValue) {
		try {
			Konfigurasi k = Common.getKonfigurasi(key, defaultValue);
			if (k != null && k.getNilai() != null && !k.getNilai().trim().isEmpty()) {
				return k.getNilai().trim();
			}
		} catch (Exception e) {
			// return default silently
		}
		return defaultValue;
	}

	/**
	 * Menghapus satu token dari daftar CSV secara aman per-token (cocok penuh
	 * antar koma). Menggantikan StringUtils.replace berbasis substring lama yang
	 * dapat menggabungkan token bertetangga sehingga memproduksi token komposit
	 * korup pada kolom capaian_lulusan.profil (mis. "3_2547_2638"). Token kosong
	 * ikut dibersihkan. Gaya Java 1.6/1.7: tanpa lambda/Stream.
	 */
	private static String hapusTokenCsvProfil(String csv, String token) {
		if (csv == null || csv.length() == 0) {
			return "";
		}
		if (token == null || token.trim().length() == 0) {
			return csv;
		}
		String target = token.trim();
		StringBuilder sb = new StringBuilder();
		String[] parts = csv.split(",");
		for (int i = 0; i < parts.length; i++) {
			String t = parts[i] == null ? "" : parts[i].trim();
			if (t.length() == 0 || t.equals(target)) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(t);
		}
		return sb.toString();
	}

	private void reloadPl(final Rows rows, final List<CapaianLulusan> capaianLulusans,
			final List<ProfilLulusan> profilLulusans) throws Exception {
		// Hak edit sudah ditentukan penuh oleh variabel "edit" (lihat initHakAkses /
		// gunakanHakAksesFiturObe) yang SUDAH mengikutkan dosen/guru pengampu
		// (pengajarInternal) sesuai konfigurasi role tiap bagian RPS OBE. Guard
		// lokal ini hanya menahan peserta didik (mahasiswa/calon/siswa) sebagai
		// pengaman tambahan; dosen & guru TIDAK lagi diblokir di sini.
		boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;
		Common.clear(rows);
		for (final ProfilLulusan profilLulusan : profilLulusans) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(RevisiHelper.createNewRevisi(ProfilLulusan.class, profilLulusan, profilLulusan.getKode()));
			row.appendChild(new Label(profilLulusan.getNama()));

			for (final CapaianLulusan capaianLulusan : capaianLulusans) {
				final String idBaru = profilLulusan.getId() + "_" + kurikulumPunyaMatakuliah.getId();
				if (bolehEdit && bolehUbahObe) {
					final Checkbox checkbox = new Checkbox();
					checkbox.setTooltiptext(profilLulusan.getKode() + " " + profilLulusan.getNama());
					checkbox.setChecked(capaianLulusan.getProfil().contains("," + profilLulusan.getId() + ",")
							|| capaianLulusan.getProfil().contains("," + idBaru + ","));
					row.appendChild(checkbox);
					checkbox.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String p = capaianLulusan.getProfil();
							if (checkbox.isChecked()) {
								p += p.isEmpty() ? idBaru + "" : "," + idBaru;
							} else {
								p = hapusTokenCsvProfil(p, profilLulusan.getId() + "");
								p = hapusTokenCsvProfil(p, idBaru);
							}
							capaianLulusan.setProfil(p);
							Common.refreshUpdate(capaianLulusan);
						}
					});
				} else {
					row.appendChild(new Label(capaianLulusan.getProfil().contains("," + profilLulusan.getId() + ",")
							|| capaianLulusan.getProfil().contains("," + idBaru + ",") ? Common.getBahasaConfig("Ya")
									: Common.getBahasaConfig("Tidak")));
				}
			}

			row.appendChild(new Label());
			Hbox hboxdata = new Hbox();
			hboxdata.setVisible(bolehUbahObe && bolehEdit);
			row.appendChild(hboxdata);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setStyle(BTN_ICON);
			button.setParent(hboxdata);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ProfilLulusanAction.onAddExternal(arg0, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							ProfilLulusan profilLulusanBaru = (ProfilLulusan) arg0.getData();
							if (profilLulusanBaru != null) {
								matakuliah.setProfilLulusan(
										appendIdToString(matakuliah.getProfilLulusan(), profilLulusanBaru.getId()));
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						}
					}, profilLulusan);
				}
			});

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setStyle(BTN_ICON);
			button.setTooltiptext(Common.getBahasaConfig("Hapus Data"));
			button.setVisible(bolehUbahObe && bolehEdit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
							Common.getBahasaConfig("Konfirmasi"), MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											matakuliah.setProfilLulusan(org.apache.commons.lang3.StringUtils.replace(
													matakuliah.getProfilLulusan(), "," + profilLulusan.getId(), ""));
											Common.refreshUpdate(matakuliah);
											onSearchDefault(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.getBahasaConfig(
													"Data ini tidak dapat dihapus karena berelasi dengan data lainnya. Error:")
													+ " " + e.getMessage());
										}
									}
								}
							});
				}
			});
			button.setParent(hboxdata);
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void initCpl() throws Exception {
		MyGroupConfig group = new MyGroupConfig(
				Common.getBahasaConfig("Capaian Lulusan - Program Studi yang Dibebankan pada MK"));
		rowsUtama.appendChild(group);

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			final Set<Long> longs = parseIdsToSet(matakuliah.getCapaianLulusan());
			List<CapaianLulusan> capaianLulusans = ConstantValues
					.simpleList(
							session.createCriteria(CapaianLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianLulusan.class);

			// Lihat catatan di guard bolehEdit lain: dosen/guru tidak diblokir di
			// sini; otorisasi sesungguhnya ada pada variabel "edit".
			boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			MyLabelConfigAgakBesar lblTitle = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Tambah Capaian Lulusan"));
			lblTitle.setStyle(TITLE_STYLE);
			row.appendChild(lblTitle);

			Hbox hbox = new Hbox();
			hbox.setVisible(bolehUbahObe && bolehEdit);
			hbox.setParent(row);

			MyToolbarbuttonConfig btnAiCpl = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Generate CPL berdasarkan AI"), "/img/svg/sparkles.svg");
			btnAiCpl.setStyle("font-size: 12px; font-weight: bold; color: #ffffff; background-color: #7c3aed;"
					+ " border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer;"
					+ " box-shadow: 0 2px 4px rgba(124,58,237,0.3); border: none; margin-right: 5px;");
			btnAiCpl.setParent(hbox);
			btnAiCpl.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilGenerateAiSeleksi("CPL");
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Ambil CPL yang Sudah Ada"),
					"/img/svg/search.svg");
			button.setStyle(BTN_PRIMARY);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session ses = null;
					try {
						ses = HibernateUtil.openSession();
						List<CapaianLulusan> cplList = ConstantValues.simpleList(
								ses.createCriteria(CapaianLulusan.class)
										.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("id", longs))
										.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions
												.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								CapaianLulusan.class);

						AmbilDataCapaianLulusanBanyak ambilData = new AmbilDataCapaianLulusanBanyak(cplList,
								matakuliah.getJurusan(), matakuliah);
						ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						ambilData.setHeight("95%");
						ambilData.setWidth("700px");

						ambilData.setEventListener(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								List<CapaianLulusan> dataList = (List<CapaianLulusan>) arg0.getData();
								for (CapaianLulusan item : dataList) {
									matakuliah.setCapaianLulusan(
											appendIdToString(matakuliah.getCapaianLulusan(), item.getId()));
								}
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						});
						ambilData.onModal();
					} finally {
						if (ses != null) {
							ses.clear();
							ses.disconnect();
							ses.close();
						}
					}
				}
			});

			button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah CPL Baru"), "/img/svg/addthis.svg");
			button.setStyle(BTN_SUCCESS);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					CapaianLulusan cplBaru = new CapaianLulusan();
					cplBaru.setJurusan(perkuliahan == null ? null : perkuliahan.getJurusan());
					cplBaru.setKhususBuatMk(matakuliah);
					CapaianLulusanAction.onAddExternal(arg0, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							CapaianLulusan cpl = (CapaianLulusan) arg0.getData();
							if (cpl != null) {
								matakuliah.setCapaianLulusan(
										appendIdToString(matakuliah.getCapaianLulusan(), cpl.getId()));
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						}
					}, cplBaru, matakuliah);
				}
			});

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(row);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig colKode = new MyColumnConfig(Common.getBahasaConfig("Kode"));
			colKode.setWidth("10%");
			colKode.setParent(columns);

			MyColumnConfig colIsi = new MyColumnConfig(Common.getBahasaConfig("Isi"));
			colIsi.setParent(columns);

			MyColumnConfig colHapus = new MyColumnConfig(Common.getBahasaConfig("Hapus"));
			colHapus.setWidth("10%");
			colHapus.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);
			for (final CapaianLulusan cpl : capaianLulusans) {
				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(RevisiHelper.createNewRevisi(CapaianLulusan.class, cpl, cpl.getKode()));
				row.appendChild(new Label(cpl.getNama()));

				Hbox hboxdata = new Hbox();
				hboxdata.setVisible(bolehUbahObe && bolehEdit);
				row.appendChild(hboxdata);

				button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setStyle(BTN_ICON);
				button.setParent(hboxdata);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						CapaianLulusanAction.onAddExternal(arg0, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								CapaianLulusan dataCpl = (CapaianLulusan) arg0.getData();
								if (dataCpl != null) {
									matakuliah.setCapaianLulusan(
											appendIdToString(matakuliah.getCapaianLulusan(), dataCpl.getId()));
									Common.refreshUpdate(matakuliah);
									onSearchDefault(null);
								}
							}
						}, cpl, matakuliah);
					}
				});

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setStyle(BTN_ICON);
				button.setTooltiptext(Common.getBahasaConfig("Hapus Data"));
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
								Common.getBahasaConfig("Konfirmasi"), MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												matakuliah
														.setCapaianLulusan(org.apache.commons.lang3.StringUtils.replace(
																matakuliah.getCapaianLulusan(), "," + cpl.getId(), ""));
												Common.refreshUpdate(matakuliah);
												onSearchDefault(null);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.getBahasaConfig(
														"Data ini tidak dapat dihapus karena berelasi dengan data lainnya. Error:")
														+ " " + e.getMessage());
											}
										}
									}
								});
					}
				});
				button.setParent(hboxdata);
			}
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void initCpmk() throws Exception {
		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Capaian Pembelajaran Mata Kuliah (CPMK)"));
		rowsUtama.appendChild(group);

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			final Set<Long> longs = parseIdsToSet(matakuliah.getCapaianPembelajaranLulusan());

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			MyLabelConfigAgakBesar lblTitle = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Tambah CPMK"));
			lblTitle.setStyle(TITLE_STYLE);
			row.appendChild(lblTitle);

			// Lihat catatan di guard bolehEdit lain: dosen/guru tidak diblokir di
			// sini; otorisasi sesungguhnya ada pada variabel "edit".
			boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;

			Hbox hbox = new Hbox();
			hbox.setVisible(bolehUbahObe && bolehEdit);
			hbox.setParent(row);

			MyToolbarbuttonConfig btnAiCpmk = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Generate CPMK berdasarkan AI"), "/img/svg/sparkles.svg");
			btnAiCpmk.setStyle("font-size: 12px; font-weight: bold; color: #ffffff; background-color: #7c3aed;"
					+ " border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer;"
					+ " box-shadow: 0 2px 4px rgba(124,58,237,0.3); border: none; margin-right: 5px;");
			btnAiCpmk.setParent(hbox);
			btnAiCpmk.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilGenerateAiSeleksi("CPMK");
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Ambil CPMK yang Sudah Ada"), "/img/svg/search.svg");
			button.setStyle(BTN_PRIMARY);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session ses = null;
					try {
						ses = HibernateUtil.openSession();
						List<CapaianPembelajaranLulusan> cplList = ConstantValues
								.simpleList(
										ses.createCriteria(CapaianPembelajaranLulusan.class)
												.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
														: Restrictions.in("id", longs))
												.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true))),
										CapaianPembelajaranLulusan.class);

						AmbilDataCapaianPembelajaranLulusanBanyak ambilData = new AmbilDataCapaianPembelajaranLulusanBanyak(
								cplList, matakuliah.getJurusan(), matakuliah);
						ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						ambilData.setHeight("95%");
						ambilData.setWidth("700px");

						ambilData.setEventListener(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								List<CapaianPembelajaranLulusan> dataList = (List<CapaianPembelajaranLulusan>) arg0
										.getData();
								for (CapaianPembelajaranLulusan item : dataList) {
									matakuliah.setCapaianPembelajaranLulusan(
											appendIdToString(matakuliah.getCapaianPembelajaranLulusan(), item.getId()));
								}
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						});
						ambilData.onModal();
					} finally {
						if (ses != null) {
							ses.clear();
							ses.disconnect();
							ses.close();
						}
					}
				}
			});

			button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah CPMK Baru"), "/img/svg/addthis.svg");
			button.setStyle(BTN_SUCCESS);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					CapaianPembelajaranLulusan cpmkBaru = new CapaianPembelajaranLulusan();
					cpmkBaru.setJurusan(perkuliahan == null ? null : perkuliahan.getJurusan());
					cpmkBaru.setKhususBuatMk(matakuliah);
					CapaianPembelajaranLulusanAction.onAddExternal(arg0, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							CapaianPembelajaranLulusan cpmk = (CapaianPembelajaranLulusan) arg0.getData();
							if (cpmk != null) {
								matakuliah.setCapaianPembelajaranLulusan(
										appendIdToString(matakuliah.getCapaianPembelajaranLulusan(), cpmk.getId()));
								Common.refreshUpdate(matakuliah);
								onSearchDefault(null);
							}
						}
					}, cpmkBaru, matakuliah);
				}
			});

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			List<CapaianPembelajaranLulusan> cplList = ConstantValues
					.simpleList(
							session.createCriteria(CapaianPembelajaranLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianPembelajaranLulusan.class);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(row);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig colKode = new MyColumnConfig(Common.getBahasaConfig("Kode"));
			colKode.setWidth("10%");
			colKode.setParent(columns);

			MyColumnConfig colIsi = new MyColumnConfig(Common.getBahasaConfig("Isi"));
			colIsi.setParent(columns);

			if (kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
				MyColumnConfig colBobot = new MyColumnConfig(Common.getBahasaConfig("Bobot (%)"));
				colBobot.setWidth("10%");
				colBobot.setParent(columns);

				MyColumnConfig colMinimal = new MyColumnConfig(Common.getBahasaConfig("Minimal"));
				colMinimal.setWidth("10%");
				colMinimal.setParent(columns);
			}

			MyColumnConfig colHapus = new MyColumnConfig(Common.getBahasaConfig("Hapus"));
			colHapus.setWidth("10%");
			colHapus.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);
			for (final CapaianPembelajaranLulusan cpl : cplList) {
				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(RevisiHelper.createNewRevisi(CapaianPembelajaranLulusan.class, cpl, cpl.getKode()));
				row.appendChild(new Label(cpl.getNama()));

				if (kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
					if (bolehUbahObe && bolehEdit) {
						final MyDoublebox doubleboxBobot = new MyDoublebox(cpl.getBobot());
						row.appendChild(doubleboxBobot);
						doubleboxBobot.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								cpl.setBobot(doubleboxBobot.getValue() == null ? 0.0 : doubleboxBobot.getValue());
								simpanCplLangsung(cpl);
								initSubCpmkKorelasiReload();
							}
						});
						final MyDoublebox doubleboxMinimal = new MyDoublebox(cpl.getMinimal());
						row.appendChild(doubleboxMinimal);
						doubleboxMinimal.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								cpl.setMinimal(doubleboxMinimal.getValue() == null ? 0.0 : doubleboxMinimal.getValue());
								simpanCplLangsung(cpl);
							}
						});
					} else {
						row.appendChild(new Label(Common.numberFormat.get().format(cpl.getBobot()) + " %"));
						row.appendChild(new Label(
								cpl.getMinimal() == null ? "" : Common.numberFormat.get().format(cpl.getMinimal())));
					}
				}

				Hbox hboxdata = new Hbox();
				hboxdata.setVisible(bolehUbahObe && bolehEdit);
				row.appendChild(hboxdata);

				button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setStyle(BTN_ICON);
				button.setParent(hboxdata);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						CapaianPembelajaranLulusanAction.onAddExternal(arg0, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								CapaianPembelajaranLulusan dataCpl = (CapaianPembelajaranLulusan) arg0.getData();
								if (dataCpl != null) {
									matakuliah.setCapaianPembelajaranLulusan(appendIdToString(
											matakuliah.getCapaianPembelajaranLulusan(), dataCpl.getId()));
									Common.refreshUpdate(matakuliah);
									onSearchDefault(null);
								}
							}
						}, cpl, matakuliah);
					}
				});

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setStyle(BTN_ICON);
				button.setTooltiptext(Common.getBahasaConfig("Hapus Data"));
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
								Common.getBahasaConfig("Konfirmasi"), MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												matakuliah.setCapaianPembelajaranLulusan(
														org.apache.commons.lang3.StringUtils.replace(
																matakuliah.getCapaianPembelajaranLulusan(),
																"," + cpl.getId(), ""));
												Common.refreshUpdate(matakuliah);
												onSearchDefault(null);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.getBahasaConfig(
														"Data ini tidak dapat dihapus karena berelasi dengan data lainnya. Error:")
														+ " " + e.getMessage());
											}
										}
									}
								});
					}
				});
				button.setParent(hboxdata);
			}
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings({ "deprecation" })
	private void initSubCpmk() throws Exception {
		MyGroupConfig group = new MyGroupConfig(
				Common.getBahasaConfig("Kemampuan Akhir Tiap Tahapan Belajar (Sub-CPMK)"));
		rowsUtama.appendChild(group);

		rowUtama = new MyFormRow();
		rowUtama.setValign("top");
		rowUtama.setParent(rowsUtama);
		ais.ui.util.ZkCompat.setSpans(rowUtama, "2");
		initSubCpmkReload();
	}

	@SuppressWarnings({ })
	private void initSubCpmkReload() throws Exception {
		Common.clear(rowUtama);
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> longs = parseIdsToSet(matakuliah.getCapaianPembelajaranLulusan());

			// Lihat catatan di guard bolehEdit lain: dosen/guru tidak diblokir di
			// sini; otorisasi sesungguhnya ada pada variabel "edit".
			boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;

			List<CapaianPembelajaranLulusan> cplList = ConstantValues
					.simpleList(
							session.createCriteria(CapaianPembelajaranLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianPembelajaranLulusan.class);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(rowUtama);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig colKodeCpmk = new MyColumnConfig(Common.getBahasaConfig("Kode CPMK"));
			colKodeCpmk.setWidth("15%");
			colKodeCpmk.setParent(columns);

			MyColumnConfig colKodeSubCpmk = new MyColumnConfig(Common.getBahasaConfig("Kode Sub-CPMK"));
			colKodeSubCpmk.setWidth("15%");
			colKodeSubCpmk.setParent(columns);

			MyColumnConfig colSubCpmk = new MyColumnConfig(Common.getBahasaConfig("Sub-CPMK"));
			colSubCpmk.setParent(columns);

			if (!kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
				MyColumnConfig colBobot = new MyColumnConfig(Common.getBahasaConfig("Bobot (%)"));
				colBobot.setWidth("8%");
				colBobot.setParent(columns);

				MyColumnConfig colMinimal = new MyColumnConfig(Common.getBahasaConfig("Minimal"));
				colMinimal.setWidth("8%");
				colMinimal.setParent(columns);
			}

			Rows rows = new Rows();
			rows.setParent(grid);
			for (final CapaianPembelajaranLulusan cpl : cplList) {
				final JSONArray array = new JSONArray(cpl.getFormula());
				for (int i = 0; i < array.length(); i++) {
					final JSONObject jsonObject = array.getJSONObject(i);
					if (jsonObject.isNull("key"))
						continue;

					String nama = !jsonObject.isNull("nama") ? jsonObject.get("nama") + "" : "";
					String kode = !jsonObject.isNull("kode") ? jsonObject.get("kode") + "" : "";
					Double bobot = !jsonObject.isNull("bobot") ? Double.parseDouble(jsonObject.get("bobot") + "") : 0.0;

					Double minimal = null;
					if (!jsonObject.isNull("minimal")) {
						try {
							minimal = Double.parseDouble(jsonObject.get("minimal") + "");
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					if (minimal == null || minimal.intValue() == 0)
						minimal = null;

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(cpl.getKode()));
					row.appendChild(new Label(kode));
					row.appendChild(new Label(nama));

					if (!kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
						if (bolehUbahObe && bolehEdit) {
							row.appendChild(new Label(Common.numberFormat.get().format(bobot) + " %"));

							final MyDoublebox doubleboxMinimal = new MyDoublebox(minimal);
							row.appendChild(doubleboxMinimal);
							doubleboxMinimal.setCols(2);
							doubleboxMinimal.addEventListener("onChange", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									jsonObject.put("minimal",
											doubleboxMinimal.getValue() == null ? 0.0 : doubleboxMinimal.getValue());
									cpl.setFormula(array.toString());
									simpanCplLangsung(cpl);
								}
							});
						} else {
							row.appendChild(new Label(Common.numberFormat.get().format(bobot) + " %"));
							row.appendChild(
									new Label(minimal == null ? "" : Common.numberFormat.get().format(minimal) + " %"));
						}
					}
				}
			}
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings("deprecation")
	private void initSubCpmkKorelasi() throws Exception {
		if (!kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
			MyGroupConfig group = new MyGroupConfig(
					Common.getBahasaConfig("Korelasi Antara CPL/CPMK Terhadap Sub-CPMK"));
			rowsUtama.appendChild(group);

			rowUtamaKorelasi = new MyFormRow();
			rowUtamaKorelasi.setValign("top");
			rowUtamaKorelasi.setParent(rowsUtama);
			ais.ui.util.ZkCompat.setSpans(rowUtamaKorelasi, "2");
			initSubCpmkKorelasiReload();
		}
	}

	@SuppressWarnings({ })
	/**
	 * Simpan perubahan CPL (bobot/minimal/formula korelasi CPMK↔Sub-CPMK) LANGSUNG ke
	 * database dalam transaksi terdedikasi yang di-COMMIT, lalu tutup session di finally.
	 *
	 * Sebelumnya dipakai Common.refreshUpdate(cpl) (memakai currentSession yang baru
	 * di-commit di akhir request). Karena reload matriks segera membuka session BARU
	 * (HibernateUtil.openSession) dan membacanya, perubahan yang belum ter-commit TIDAK
	 * terlihat → nilai tampak kembali ke lama, seolah "harus input 2x/berkali-kali baru
	 * tersimpan". Dengan commit eksplisit di sini, reload berikutnya langsung membaca
	 * nilai terbaru (flush instan ke DB).
	 */
	private void simpanCplLangsung(CapaianPembelajaranLulusan cpl) {
		if (cpl == null) {
			return;
		}
		Session s = null;
		org.hibernate.Transaction tx = null;
		try {
			s = HibernateUtil.openSession();
			tx = s.beginTransaction();
			s.update(cpl);
			tx.commit();
			tx = null;
		} catch (Exception e) {
			if (tx != null) {
				try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:2967");}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (s != null) {
				try { s.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:2972");}
				try { s.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:2973");}
			}
		}
	}

	private void initSubCpmkKorelasiReload() throws Exception {
		Common.clear(rowUtamaKorelasi);
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> longs = parseIdsToSet(matakuliah.getCapaianPembelajaranLulusan());

			// Pengecekan hak akses edit secara umum
			// Lihat catatan di guard bolehEdit lain: dosen/guru tidak diblokir di
			// sini; otorisasi sesungguhnya ada pada variabel "edit".
			boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;

			List<CapaianPembelajaranLulusan> cplList = ConstantValues
					.simpleList(
							session.createCriteria(CapaianPembelajaranLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianPembelajaranLulusan.class);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(rowUtamaKorelasi);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig colKodeSubCpmk = new MyColumnConfig(Common.getBahasaConfig("Kode Sub-CPMK"));
			colKodeSubCpmk.setWidth("10%");
			colKodeSubCpmk.setParent(columns);

			Map<Integer, Long> mapsData = new HashMap<Integer, Long>();
			int ix = 1;
			for (CapaianPembelajaranLulusan cpl : cplList) {
				Column column1 = new Column(cpl.getKode() + " (%)");
				column1.setParent(columns);
				column1.setWidth("5%");
				mapsData.put(ix, cpl.getId());
				ix++;
			}

			MyColumnConfig colEmpty = new MyColumnConfig();
			colEmpty.setParent(columns);

			MyColumnConfig colBobot = new MyColumnConfig(Common.getBahasaConfig("Bobot (%)"));
			colBobot.setWidth("5%");
			colBobot.setParent(columns);

			Double totalBobot = 0.0;
			Rows rows = new Rows();
			rows.setParent(grid);

			final int finalIx = ix;

			for (final CapaianPembelajaranLulusan cpl : cplList) {
				final JSONArray array = new JSONArray(cpl.getFormula());
				for (int i = 0; i < array.length(); i++) {
					final JSONObject jsonObject = array.getJSONObject(i);
					if (jsonObject.isNull("key"))
						continue;

					String kode = !jsonObject.isNull("kode") ? jsonObject.get("kode") + "" : "";
					Double bobot = !jsonObject.isNull("bobot") ? Double.parseDouble(jsonObject.get("bobot") + "") : 0.0;

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(kode));

					for (int index = 1; index < finalIx; index++) {
						final int currentIndex = index;
						Long idC = mapsData.get(index);
						Double bobotMapingCPMKDanSUbCPMK = !jsonObject.isNull("bobot_index_" + currentIndex)
								? Double.parseDouble(jsonObject.get("bobot_index_" + currentIndex) + "")
								: ((idC != null && idC.equals(cpl.getId())) ? bobot : 0.0);

						jsonObject.put("bobot_index_" + currentIndex, bobotMapingCPMKDanSUbCPMK);

						if (bolehUbahObe && bolehEdit) {
							final MyDoublebox myDoublebox = new MyDoublebox(bobotMapingCPMKDanSUbCPMK);
							row.appendChild(myDoublebox);
							myDoublebox.setCols(2);
							myDoublebox.addEventListener("onChange", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									Double newValue = myDoublebox.getValue() == null ? 0.0 : myDoublebox.getValue();
									jsonObject.put("bobot_index_" + currentIndex, newValue);

									Double newRowTotal = 0.0;
									for (int k = 1; k < finalIx; k++) {
										Double b = !jsonObject.isNull("bobot_index_" + k)
												? Double.parseDouble(jsonObject.get("bobot_index_" + k) + "")
												: 0.0;
										newRowTotal += b;
									}

									jsonObject.put("bobot", newRowTotal);
									cpl.setFormula(array.toString());
									simpanCplLangsung(cpl);

									initSubCpmkKorelasiReload();
									initSubCpmkReload();
								}
							});
						} else {
							row.appendChild(new Label(Common.numberFormat.get().format(bobotMapingCPMKDanSUbCPMK) + " %"));
						}
					}

					row.appendChild(new Label());
					if (bolehUbahObe && bolehEdit) {
						// Cari kolom CPMK pemilik Sub-CPMK ini (bobot_index untuk CPMK ini)
						int ownerIdx = -1;
						for (int k = 1; k < finalIx; k++) {
							if (cpl.getId().equals(mapsData.get(k))) { ownerIdx = k; break; }
						}
						final int finalOwnerIdx = ownerIdx;
						final MyDoublebox bobotTotalBox = new MyDoublebox(bobot > 0 ? bobot : null);
						bobotTotalBox.setCols(3);
						bobotTotalBox.setTooltiptext(
							"Ketik bobot Sub-CPMK ini → otomatis mengisi kolom CPMK \"" + cpl.getKode() + "\" sesuai.");
						row.appendChild(bobotTotalBox);
						bobotTotalBox.addEventListener("onChange", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								double newBobot = bobotTotalBox.getValue() == null ? 0.0 : bobotTotalBox.getValue();
								jsonObject.put("bobot", newBobot);
								if (finalOwnerIdx > 0) {
									jsonObject.put("bobot_index_" + finalOwnerIdx, newBobot);
								}
								cpl.setFormula(array.toString());
								simpanCplLangsung(cpl);
								initSubCpmkKorelasiReload();
								initSubCpmkReload();
							}
						});
					} else {
						row.appendChild(new Label(Common.numberFormat.get().format(bobot) + " %"));
					}
					totalBobot += bobot;
				}
			}

			Foot foot = new Foot();
			foot.setParent(grid);
			Footer footer = new Footer(Common.getBahasaConfig("Total"));
			footer.setParent(foot);
			for (int index = 1; index < ix; index++) {
				new Footer().setParent(foot);
			}
			new Footer().setParent(foot);
			foot.appendChild(new Footer(Common.numberFormat.get().format(totalBobot) + " %"));
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings({ "deprecation" })
	private void initCplCpmk() throws Exception {
		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Korelasi Antara CPL dengan CPMK"));
		rowsUtama.appendChild(group);

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> longsCpmk = parseIdsToSet(matakuliah.getCapaianPembelajaranLulusan());

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsUtama);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			List<CapaianPembelajaranLulusan> cplList = ConstantValues.simpleList(
					session.createCriteria(CapaianPembelajaranLulusan.class)
							.add(longsCpmk.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", longsCpmk))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					CapaianPembelajaranLulusan.class);

			Set<Long> longsCpl = parseIdsToSet(matakuliah.getCapaianLulusan());
			List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(
					session.createCriteria(CapaianLulusan.class)
							.add(longsCpl.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", longsCpl))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					CapaianLulusan.class);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(row);
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig colCpl = new MyColumnConfig(Common.getBahasaConfig("CPL"));
			colCpl.setWidth("10%");
			colCpl.setParent(columns);

			for (final CapaianPembelajaranLulusan cpl : cplList) {
				Column column1 = new Column(cpl.getKode());
				column1.setParent(columns);
				column1.setWidth("5%");
				column1.setTooltiptext(cpl.getNama());
			}

			MyColumnConfig colEmpty = new MyColumnConfig();
			colEmpty.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);
			reloadCplCpmk(rows, cplList, capaianLulusans);
		} finally {
			closeSessionQuietly(session);
		}
	}

	private void reloadCplCpmk(final Rows rows, final List<CapaianPembelajaranLulusan> cplList,
			final List<CapaianLulusan> capaianLulusans) throws Exception {
		Common.clear(rows);
		// Hak edit sudah ditentukan penuh oleh variabel "edit" (lihat initHakAkses /
		// gunakanHakAksesFiturObe) yang SUDAH mengikutkan dosen/guru pengampu
		// (pengajarInternal) sesuai konfigurasi role tiap bagian RPS OBE. Guard
		// lokal ini hanya menahan peserta didik (mahasiswa/calon/siswa) sebagai
		// pengaman tambahan; dosen & guru TIDAK lagi diblokir di sini.
		boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;

		for (final CapaianLulusan cplItem : capaianLulusans) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(cplItem.getKode()));

			for (CapaianPembelajaranLulusan cpmkItem : cplList) {
				final String key = cpmkItem.getId() + "";

				if (bolehUbahObe && bolehEdit) {
					final Checkbox checkbox = new Checkbox();
					checkbox.setTooltiptext(cpmkItem.getKode() + " " + cpmkItem.getNama());
					checkbox.setChecked(cplItem.getCapaianPembelajaranLulusan().contains("," + key + ","));
					row.appendChild(checkbox);
					checkbox.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							String p = cplItem.getCapaianPembelajaranLulusan();
							if (checkbox.isChecked()) {
								p += p.isEmpty() ? key + "" : "," + key;
							} else {
								p = org.apache.commons.lang3.StringUtils.replace(p, "," + key, "");
							}
							cplItem.setCapaianPembelajaranLulusan(p);
							Common.refreshUpdate(cplItem);
							reloadCplCpmk(rows, cplList, capaianLulusans);
						}
					});
				} else {
					row.appendChild(new Label(cplItem.getCapaianPembelajaranLulusan().contains("," + key + ",")
							? Common.getBahasaConfig("Ya")
							: ""));
				}
			}
			row.appendChild(new Label());
		}
	}

	/** Generate GABUNGAN via AI untuk tab Deskripsi &amp; Pustaka: Deskripsi, Mitra Pengembang (teks),
	 *  Bahan Kajian, Pustaka Utama &amp; Pendukung (buat entri baru + tautkan). Streaming + refresh. */
	private void generateDeskripsiPustakaAi() throws Exception {
		final String namaMk = matakuliah != null && matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah != null && matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final String namaJur = (matakuliah != null && matakuliah.getJurusan() != null
				&& matakuliah.getJurusan().getNama() != null) ? matakuliah.getJurusan().getNama() : "";

		StringBuilder p = new StringBuilder();
		p.append("Buatkan konten RPS untuk mata kuliah \"").append(namaMk).append("\" (kode ").append(kodeMk)
				.append(", program studi ").append(namaJur).append("), Bahasa Indonesia formal-akademis.\n");
		p.append("Keluarkan HANYA JSON object valid (tanpa teks/markdown lain) dengan kunci PERSIS:\n");
		p.append("{\n");
		p.append("\"deskripsi\":\"deskripsi singkat mata kuliah 1-2 paragraf\",\n");
		p.append("\"mitraPengembang\":\"pihak/mitra pengembang RPS (institusi/asosiasi relevan)\",\n");
		p.append("\"bahanKajian\":[\"materi/bahan kajian 1\",\"materi 2\",\"materi 3\"],\n");
		p.append("\"pustakaUtama\":[\"sitasi utama format Penulis. (Tahun). Judul. Penerbit.\"],\n");
		p.append("\"pustakaPendukung\":[\"sitasi pendukung 1\",\"sitasi 2\"]\n");
		p.append("}\n");
		final String prompt = p.toString();

		jalankanAiStreaming(Common.getBahasaConfig("Generate AI (Deskripsi, Bahan Kajian, Pustaka & Mitra)"), prompt,
				new HasilAiListener() {
					@Override
					public void selesai(String resp) throws Exception {
						String js = potongJsonObj(resp);
						if (js == null) {
							return;
						}
						org.json.JSONObject o = new org.json.JSONObject(js);
						if (o.has("deskripsi")) {
							String v = o.optString("deskripsi", "");
							if (deskripsiPembelajaran != null) {
								deskripsiPembelajaran.setValue(v);
							}
							if (kurikulumPunyaMatakuliah != null) {
								kurikulumPunyaMatakuliah.setDeskripsiPembelajaran(v);
							}
						}
						if (o.has("mitraPengembang")) {
							String v = o.optString("mitraPengembang", "");
							if (mitraPengembang != null) {
								mitraPengembang.setValue(v);
							}
							if (kurikulumPunyaMatakuliah != null) {
								kurikulumPunyaMatakuliah.setMitraPengembang(v);
							}
						}

						Session s = null;
						try {
							s = HibernateUtil.openSession();
							s.beginTransaction();
							ais.database.model.PerguruanTinggi pt = ais.action.master.helper.util.PerguruanTinggiUtil
									.getPerguruanTinggi();
							ais.database.model.Jurusan jur = matakuliah != null ? matakuliah.getJurusan() : null;

							org.json.JSONArray bk = o.optJSONArray("bahanKajian");
							if (bk != null) {
								for (int i = 0; i < bk.length(); i++) {
									String nama = bk.optString(i, "").trim();
									if (nama.length() == 0) {
										continue;
									}
									BahanKajian e = new BahanKajian();
									e.setNama(nama);
									e.setPerguruanTinggi(pt);
									e.setJurusan(jur);
									e.setAktif(Boolean.TRUE);
									e.setKhususBuatMk(matakuliah);
									s.save(e);
									s.flush();
									if (e.getId() != null) {
										matakuliah.setBahanKajian(
												appendIdToString(matakuliah.getBahanKajian(), e.getId()));
									}
								}
							}
							org.json.JSONArray pu = o.optJSONArray("pustakaUtama");
							if (pu != null) {
								for (int i = 0; i < pu.length(); i++) {
									String judul = pu.optString(i, "").trim();
									if (judul.length() == 0) {
										continue;
									}
									ReferensiLulusan r = new ReferensiLulusan();
									r.setNama(judul);
									r.setPerguruanTinggi(pt);
									r.setAktif(Boolean.TRUE);
									s.save(r);
									s.flush();
									if (r.getId() != null) {
										kurikulumPunyaMatakuliah.setPustaka(
												appendIdToString(kurikulumPunyaMatakuliah.getPustaka(), r.getId()));
									}
								}
							}
							org.json.JSONArray pp = o.optJSONArray("pustakaPendukung");
							if (pp != null) {
								for (int i = 0; i < pp.length(); i++) {
									String judul = pp.optString(i, "").trim();
									if (judul.length() == 0) {
										continue;
									}
									ReferensiLulusan r = new ReferensiLulusan();
									r.setNama(judul);
									r.setPerguruanTinggi(pt);
									r.setAktif(Boolean.TRUE);
									s.save(r);
									s.flush();
									if (r.getId() != null) {
										kurikulumPunyaMatakuliah.setPustakaPendukung(appendIdToString(
												kurikulumPunyaMatakuliah.getPustakaPendukung(), r.getId()));
									}
								}
							}
							s.getTransaction().commit();
						} catch (Exception ex) {
							if (s != null && s.getTransaction() != null) {
								try {
									s.getTransaction().rollback();
								} catch (Exception er) {
								}
							}
							ais.common.ErrorAuditUtil.record(ex, "RpsObeAction.generateDeskripsiPustakaAi");
						} finally {
							closeSessionQuietly(s);
						}

						Common.refreshUpdate(matakuliah);
						Common.refreshUpdate(kurikulumPunyaMatakuliah);
						onSearchDefault(null);
						MyMessageboxConfig.show(
								Common.getBahasaConfig(
										"Deskripsi, Bahan Kajian, Pustaka & Mitra terisi via AI. Tinjau lalu simpan."),
								Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
					}
				});
	}

	private void initDeskripsi() {
		// Hak edit sudah ditentukan penuh oleh variabel "edit" (lihat initHakAkses /
		// gunakanHakAksesFiturObe) yang SUDAH mengikutkan dosen/guru pengampu
		// (pengajarInternal) sesuai konfigurasi role tiap bagian RPS OBE. Guard
		// lokal ini hanya menahan peserta didik (mahasiswa/calon/siswa) sebagai
		// pengaman tambahan; dosen & guru TIDAK lagi diblokir di sini.
		boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null;

		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Deskripsi, Bahan Kajian, dan Pustaka"));
		rowsUtama.appendChild(group);

		if (bolehUbahObe && bolehEdit) {
			MyFormRow rowAiDp = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowAiDp, "2");
			rowAiDp.setParent(rowsUtama);
			MyToolbarbuttonConfig btnAiDp = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Generate AI (Deskripsi, Bahan Kajian, Pustaka & Mitra)"),
					"/img/svg/sparkles.svg");
			btnAiDp.setStyle("font-size:12px;font-weight:bold;color:#ffffff;background-color:#7c3aed;"
					+ "border-radius:6px;padding:6px 15px;border:none;cursor:pointer;margin:4px 0;");
			btnAiDp.setParent(rowAiDp);
			btnAiDp.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					generateDeskripsiPustakaAi();
				}
			});
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblDesc = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Deskripsi Singkat Mata Kuliah"));
		lblDesc.setStyle(TITLE_STYLE);
		row.appendChild(lblDesc);
		
		deskripsiPembelajaran = new MyTextbox(kurikulumPunyaMatakuliah.getDeskripsiPembelajaran());
		deskripsiPembelajaran.setRows(5);
		deskripsiPembelajaran.setWidth("95%");
		if (bolehUbahObe && bolehEdit) {
			row.appendChild(deskripsiPembelajaran);
		} else {
			row.appendChild(new Label(kurikulumPunyaMatakuliah.getDeskripsiPembelajaran()));
		}
		deskripsiPembelajaran.addEventListener("onChange", eventListener);

		if (bolehUbahObe && bolehEdit) {
			Hbox hboxAiDesc = new Hbox();
			hboxAiDesc.setStyle("margin-top:6px;");
			hboxAiDesc.setParent(row);
			MyToolbarbuttonConfig btnAiDesc = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Generate Deskripsi berdasarkan AI"), "/img/svg/sparkles.svg");
			btnAiDesc.setStyle("font-size: 12px; font-weight: bold; color: #ffffff; background-color: #7c3aed;"
					+ " border-radius: 6px; padding: 6px 15px; text-decoration: none; cursor: pointer;"
					+ " box-shadow: 0 2px 4px rgba(124,58,237,0.3); border: none; margin-right: 5px;");
			btnAiDesc.setParent(hboxAiDesc);
			btnAiDesc.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilGenerateAiDeskripsi();
				}
			});
		}

		// ================= BAHAN KAJIAN =================
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblBahan = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Bahan Kajian: Materi Pembelajaran"));
		lblBahan.setStyle(TITLE_STYLE);
		row.appendChild(lblBahan);

		final Set<Long> longsBahanKajian = parseIdsToSet(matakuliah.getBahanKajian());

		Vbox vbox = new Vbox();
		row.appendChild(vbox);
		Hbox hbox = new Hbox();
		vbox.appendChild(hbox);
		hbox.setVisible(bolehUbahObe && bolehEdit);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				Common.getBahasaConfig("Ambil Bahan Kajian yang Sudah Ada"), "/img/svg/search.svg");
		button.setStyle(BTN_PRIMARY);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					List<BahanKajian> bahanKajians = ConstantValues.simpleList(
							session.createCriteria(BahanKajian.class)
									.add(longsBahanKajian.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longsBahanKajian))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							BahanKajian.class);

					AmbilDataBahanKajianBanyak ambilData = new AmbilDataBahanKajianBanyak(bahanKajians,
							matakuliah.getJurusan(), matakuliah);
					ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilData.setHeight("95%");
					ambilData.setWidth("700px");

					ambilData.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<BahanKajian> dataList = (List<BahanKajian>) arg0.getData();
							for (BahanKajian item : dataList) {
								matakuliah.setBahanKajian(appendIdToString(matakuliah.getBahanKajian(), item.getId()));
							}
							Common.refreshUpdate(matakuliah);
							onSearchDefault(null);
						}
					});
					ambilData.onModal();
				} finally {
					closeSessionQuietly(session);
				}
			}
		});

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah Bahan Kajian Baru"), "/img/svg/addthis.svg");
		button.setStyle(BTN_SUCCESS);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				BahanKajian bahanKajianBaru = new BahanKajian();
				bahanKajianBaru.setJurusan(perkuliahan == null ? null : perkuliahan.getJurusan());
				bahanKajianBaru.setKhususBuatMk(matakuliah);
				BahanKajianAction.onAddExternal(arg0, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						BahanKajian bk = (BahanKajian) arg0.getData();
						if (bk != null) {
							matakuliah.setBahanKajian(appendIdToString(matakuliah.getBahanKajian(), bk.getId()));
							Common.refreshUpdate(matakuliah);
							onSearchDefault(null);
						}
					}
				}, bahanKajianBaru, matakuliah);
			}
		});

		int oi = 1;
		for (final Long idBahan : longsBahanKajian) {
			BahanKajian bahanKajian = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(), idBahan);
			if (bahanKajian != null) {
				buatRowItemHapus(vbox, oi + ". " + bahanKajian.getNama(), bolehUbahObe && bolehEdit, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
								Common.getBahasaConfig("Konfirmasi"), MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												matakuliah.setBahanKajian(org.apache.commons.lang3.StringUtils
														.replace(matakuliah.getBahanKajian(), "," + idBahan, ""));
												Common.refreshUpdate(matakuliah);
												onSearchDefault(null);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.getBahasaConfig(
														"Data ini tidak dapat dihapus karena berelasi. Error:") + " "
														+ e.getMessage());
											}
										}
									}
								});
					}
				});
				oi++;
			}
		}

		// ================= PUSTAKA UTAMA =================
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblPustaka = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Pustaka Utama"));
		lblPustaka.setStyle(TITLE_STYLE);
		row.appendChild(lblPustaka);

		vbox = new Vbox();
		row.appendChild(vbox);
		hbox = new Hbox();
		vbox.appendChild(hbox);
		hbox.setVisible(bolehUbahObe && bolehEdit);

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Ambil Referensi yang Sudah Ada"),
				"/img/svg/search.svg");
		button.setStyle(BTN_PRIMARY);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					Set<Long> longsRef = parseIdsToSet(kurikulumPunyaMatakuliah.getPustaka());
					List<ReferensiLulusan> refList = ConstantValues.simpleList(
							session.createCriteria(ReferensiLulusan.class)
									.add(longsRef.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longsRef))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							ReferensiLulusan.class);

					AmbilDataReferensiLulusanBanyak ambilData = new AmbilDataReferensiLulusanBanyak(refList);
					ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilData.setHeight("95%");
					ambilData.setWidth("700px");

					ambilData.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<ReferensiLulusan> dataList = (List<ReferensiLulusan>) arg0.getData();
							for (ReferensiLulusan item : dataList) {
								kurikulumPunyaMatakuliah.setPustaka(
										appendIdToString(kurikulumPunyaMatakuliah.getPustaka(), item.getId()));
							}
							Common.refreshUpdate(kurikulumPunyaMatakuliah);
							onSearchDefault(null);
						}
					});
					ambilData.onModal();
				} finally {
					closeSessionQuietly(session);
				}
			}
		});

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah Referensi Baru"), "/img/svg/addthis.svg");
		button.setStyle(BTN_SUCCESS);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ReferensiLulusanAction.onAddExternal(arg0, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						ReferensiLulusan ref = (ReferensiLulusan) arg0.getData();
						if (ref != null) {
							kurikulumPunyaMatakuliah
									.setPustaka(appendIdToString(kurikulumPunyaMatakuliah.getPustaka(), ref.getId()));
							Common.refreshUpdate(kurikulumPunyaMatakuliah);
							onSearchDefault(null);
						}
					}
				}, new ReferensiLulusan());
			}
		});

		oi = 1;
		for (final String idBahan : kurikulumPunyaMatakuliah.getPustaka().split(",")) {
			if (!idBahan.trim().isEmpty()) {
				try {
					final ReferensiLulusan refLulusan = (ReferensiLulusan) ConstantValues
							.ambil(ReferensiLulusan.class.getName(), Long.parseLong(idBahan.trim()));
					if (refLulusan != null) {
						buatRowRefHapus(vbox, oi, refLulusan, bolehUbahObe && hbox.isVisible(), new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								MyMessageboxConfig.show(
										Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
										Common.getBahasaConfig("Konfirmasi"),
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {
													try {
														kurikulumPunyaMatakuliah
																.setPustaka(org.apache.commons.lang3.StringUtils
																		.replace(kurikulumPunyaMatakuliah.getPustaka(),
																				"," + idBahan, ""));
														Common.refreshUpdate(kurikulumPunyaMatakuliah);
														onSearchDefault(null);
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														MyMessageboxConfig.show(Common.getBahasaConfig(
																"Data ini tidak dapat dihapus karena berelasi. Error:")
																+ " " + e.getMessage());
													}
												}
											}
										});
							}
						}, bolehUbahObe);
						oi++;
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		// ================= PUSTAKA PENDUKUNG =================
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblPendukung = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Pustaka Pendukung"));
		lblPendukung.setStyle(TITLE_STYLE);
		row.appendChild(lblPendukung);

		vbox = new Vbox();
		row.appendChild(vbox);
		hbox = new Hbox();
		vbox.appendChild(hbox);
		hbox.setVisible(bolehUbahObe && bolehEdit);

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Ambil Referensi yang Sudah Ada"),
				"/img/svg/search.svg");
		button.setStyle(BTN_PRIMARY);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					Set<Long> longsRefPend = parseIdsToSet(kurikulumPunyaMatakuliah.getPustakaPendukung());
					List<ReferensiLulusan> refList = ConstantValues.simpleList(
							session.createCriteria(ReferensiLulusan.class)
									.add(longsRefPend.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longsRefPend))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							ReferensiLulusan.class);

					AmbilDataReferensiLulusanBanyak ambilData = new AmbilDataReferensiLulusanBanyak(refList);
					ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilData.setHeight("95%");
					ambilData.setWidth("700px");

					ambilData.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<ReferensiLulusan> dataList = (List<ReferensiLulusan>) arg0.getData();
							for (ReferensiLulusan item : dataList) {
								kurikulumPunyaMatakuliah.setPustakaPendukung(
										appendIdToString(kurikulumPunyaMatakuliah.getPustakaPendukung(), item.getId()));
							}
							Common.refreshUpdate(matakuliah);
							onSearchDefault(null);
						}
					});
					ambilData.onModal();
				} finally {
					closeSessionQuietly(session);
				}
			}
		});

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah Referensi Baru"), "/img/svg/addthis.svg");
		button.setStyle(BTN_SUCCESS);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ReferensiLulusanAction.onAddExternal(arg0, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						ReferensiLulusan ref = (ReferensiLulusan) arg0.getData();
						if (ref != null) {
							kurikulumPunyaMatakuliah.setPustakaPendukung(
									appendIdToString(kurikulumPunyaMatakuliah.getPustakaPendukung(), ref.getId()));
							Common.refreshUpdate(kurikulumPunyaMatakuliah);
							onSearchDefault(null);
						}
					}
				}, new ReferensiLulusan());
			}
		});

		oi = 1;
		for (final String idBahan : kurikulumPunyaMatakuliah.getPustakaPendukung().split(",")) {
			if (!idBahan.trim().isEmpty()) {
				try {
					final ReferensiLulusan refLulusan = (ReferensiLulusan) ConstantValues
							.ambil(ReferensiLulusan.class.getName(), Long.parseLong(idBahan.trim()));
					if (refLulusan != null) {
						buatRowRefHapus(vbox, oi, refLulusan, bolehUbahObe && hbox.isVisible(), new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								MyMessageboxConfig.show(
										Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
										Common.getBahasaConfig("Konfirmasi"),
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {
													try {
														kurikulumPunyaMatakuliah.setPustakaPendukung(
																org.apache.commons.lang3.StringUtils.replace(
																		kurikulumPunyaMatakuliah.getPustakaPendukung(),
																		"," + idBahan, ""));
														Common.refreshUpdate(kurikulumPunyaMatakuliah);
														onSearchDefault(null);
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														MyMessageboxConfig.show(Common.getBahasaConfig(
																"Data ini tidak dapat dihapus karena berelasi. Error:")
																+ " " + e.getMessage());
													}
												}
											}
										});
							}
						}, bolehUbahObe);
						oi++;
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		// ================= DOSEN PENGAMPU =================
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblDsn = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Dosen Pengampu"));
		lblDsn.setStyle(TITLE_STYLE);
		row.appendChild(lblDsn);

		vbox = new Vbox();
		row.appendChild(vbox);
		hbox = new Hbox();
		vbox.appendChild(hbox);
		hbox.setVisible(bolehUbahObe && bolehEdit);

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Masukkan Data Dosen"), "/img/svg/addthis.svg");
		button.setStyle(BTN_SUCCESS);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					Set<Long> longsDosen = parseIdsToSet(kurikulumPunyaMatakuliah.getDosen());
					List<Dosen> dosens = ConstantValues.simpleList(
							session.createCriteria(Dosen.class)
									.add(longsDosen.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longsDosen))
									.addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Dosen.class);

					AmbilDataDosenBanyak ambilData = new AmbilDataDosenBanyak(dosens);
					ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilData.setHeight("95%");
					ambilData.setWidth("700px");

					ambilData.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Dosen> dataList = (List<Dosen>) arg0.getData();
							for (Dosen dsn : dataList) {
								if (!kurikulumPunyaMatakuliah.getDosen().contains("," + dsn.getId() + ",")) {
									kurikulumPunyaMatakuliah.setDosen(
											appendIdToString(kurikulumPunyaMatakuliah.getDosen(), dsn.getId()));
								}
							}
							Common.refreshUpdate(kurikulumPunyaMatakuliah);
							onSearchDefault(null);
						}
					});
					ambilData.onModal();
				} finally {
					closeSessionQuietly(session);
				}
			}
		});

		oi = 1;
		for (final String d : kurikulumPunyaMatakuliah.getDosen().split(",")) {
			if (!d.trim().isEmpty()) {
				try {
					final Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(d.trim()));
					if (dosen != null) {
						buatRowItemHapus(vbox, oi + ". " + dosen.getNama(), bolehUbahObe && hbox.isVisible(), new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								MyMessageboxConfig.show(
										Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
										Common.getBahasaConfig("Konfirmasi"),
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {
													try {
														kurikulumPunyaMatakuliah
																.setDosen(org.apache.commons.lang3.StringUtils.replace(
																		kurikulumPunyaMatakuliah.getDosen(), "," + d,
																		""));
														Common.refreshUpdate(kurikulumPunyaMatakuliah);
														onSearchDefault(null);
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														MyMessageboxConfig.show(Common.getBahasaConfig(
																"Data ini tidak dapat dihapus karena berelasi. Error:")
																+ " " + e.getMessage());
													}
												}
											}
										});
							}
						});
						oi++;
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		// ================= MITRA & PRASYARAT =================
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblMitra = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Mitra Pengembang"));
		lblMitra.setStyle(TITLE_STYLE);
		row.appendChild(lblMitra);
		
		mitraPengembang = new MyTextbox(kurikulumPunyaMatakuliah.getMitraPengembang());
		mitraPengembang.setRows(5);
		mitraPengembang.setWidth("95%");
		if (bolehUbahObe) {
			row.appendChild(mitraPengembang);
		} else {
			row.appendChild(new Label(kurikulumPunyaMatakuliah.getMitraPengembang()));
		}
		mitraPengembang.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		MyLabelConfigAgakBesar lblPrasyarat = new MyLabelConfigAgakBesar(Common.getBahasaConfig("Mata Kuliah Prasyarat"));
		lblPrasyarat.setStyle(TITLE_STYLE);
		row.appendChild(lblPrasyarat);

		vbox = new Vbox();
		row.appendChild(vbox);
		hbox = new Hbox();
		vbox.appendChild(hbox);
		hbox.setVisible(bolehUbahObe && bolehEdit);

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Masukkan Data Mata Kuliah Prasyarat"),
				"/img/svg/search.svg");
		button.setStyle(BTN_PRIMARY);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					Set<Long> longsMk = parseIdsToSet(kurikulumPunyaMatakuliah.getMkPrasyarat());
					List<Matakuliah> mkList = ConstantValues.simpleList(
							session.createCriteria(Matakuliah.class)
									.add(longsMk.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longsMk))
									.addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Matakuliah.class);

					AmbilDataMatakuliahBanyak ambilData = new AmbilDataMatakuliahBanyak(mkList);
					ambilData.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilData.setHeight("95%");
					ambilData.setWidth("700px");

					ambilData.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Matakuliah> dataList = (List<Matakuliah>) arg0.getData();
							for (Matakuliah mkItem : dataList) {
								if (!kurikulumPunyaMatakuliah.getMkPrasyarat().contains("," + mkItem.getId() + ",")) {
									kurikulumPunyaMatakuliah.setMkPrasyarat(appendIdToString(
											kurikulumPunyaMatakuliah.getMkPrasyarat(), mkItem.getId()));
								}
							}
							Common.refreshUpdate(kurikulumPunyaMatakuliah);
							onSearchDefault(null);
						}
					});
					ambilData.onModal();
				} finally {
					closeSessionQuietly(session);
				}
			}
		});

		oi = 1;
		for (final String d : kurikulumPunyaMatakuliah.getMkPrasyarat().split(",")) {
			if (!d.trim().isEmpty()) {
				try {
					final Matakuliah mk = (Matakuliah) ConstantValues.ambil(Matakuliah.class.getName(),
							Long.parseLong(d.trim()));
					if (mk != null) {
						buatRowItemHapus(vbox, oi + ". " + mk.getKode() + " " + mk.getNama(), bolehUbahObe && hbox.isVisible(), new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								MyMessageboxConfig.show(
										Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
										Common.getBahasaConfig("Konfirmasi"),
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												int i = Integer.parseInt(event.getData().toString());
												if (i == MyMessageboxConfig.OK) {
													try {
														kurikulumPunyaMatakuliah.setMkPrasyarat(
																org.apache.commons.lang3.StringUtils.replace(
																		kurikulumPunyaMatakuliah.getMkPrasyarat(),
																		"," + d, ""));
														Common.refreshUpdate(kurikulumPunyaMatakuliah);
														onSearchDefault(null);
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														MyMessageboxConfig.show(Common.getBahasaConfig(
																"Data ini tidak dapat dihapus karena berelasi. Error:")
																+ " " + e.getMessage());
													}
												}
											}
										});
							}
						});
						oi++;
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

	public static void editRinci(final JSONObject jsonObject, final String keyData,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, final JSONObject jsonArraykurikulumPunyaMatakuliah,
			final EventListener reloadRincian) throws Exception {

		final MyWindow window = new MyWindow(Common.getBahasaConfig("Rincian RPS OBE"), "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("95%");
		window.setWidth("600px");
		window.setStyle("background-color: #f8fafc; border-radius: 12px;");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setStyle("background: transparent; border: none;");
		borderlayout.setParent(window);
		Center center = new Center();
		center.setStyle("background: transparent; border: none; padding: 15px;");
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setStyle(GRID_STYLE);
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig col1 = new MyColumnConfig();
		col1.setWidth("40%");
		col1.setParent(columns);

		MyColumnConfig col2 = new MyColumnConfig();
		col2.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Mulai Minggu Ke *")));
		final Intbox mulaiMingguKe = new Intbox(
				jsonObject.isNull("mulaiMingguKe") ? 1 : jsonObject.getInt("mulaiMingguKe"));
		row.appendChild(mulaiMingguKe);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Sampai Minggu Ke *")));
		final Intbox sampaiMingguKe = new Intbox(
				jsonObject.isNull("sampaiMingguKe") ? 1 : jsonObject.getInt("sampaiMingguKe"));
		row.appendChild(sampaiMingguKe);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		final Combobox jumlahCpmb = new Combobox();
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? Common.getBahasaConfig("Jumlah CPMK *")
						: Common.getBahasaConfig("Jumlah Sub-CPMK *")));
		for (int i = 1; i <= 5; i++) {
			Comboitem comboitem = new Comboitem(
					kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? i + " CPMK" : i + " Sub-CPMK");
			comboitem.setValue(i);
			jumlahCpmb.appendChild(comboitem);
		}
		row.appendChild(jumlahCpmb);
		jumlahCpmb.setWidth("95%");
		jumlahCpmb.setReadonly(true);
		Common.selectComboItem(jumlahCpmb, jsonObject.isNull("jumlahCpmk") ? 1 : jsonObject.getInt("jumlahCpmk"));

		final Combobox subCpmb = new Combobox(), subCpmb2 = new Combobox(), subCpmb3 = new Combobox(),
				subCpmb4 = new Combobox(), subCpmb5 = new Combobox();

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> longs = parseIdsToSet(kurikulumPunyaMatakuliah.getMatakuliah().getCapaianPembelajaranLulusan());
			List<CapaianPembelajaranLulusan> cplList = ConstantValues
					.simpleList(
							session.createCriteria(CapaianPembelajaranLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianPembelajaranLulusan.class);

			for (CapaianPembelajaranLulusan cpl : cplList) {
				if (kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
					String itemLabel = cpl.getKode() + " " + cpl.getNama();

					Comboitem item1 = new Comboitem(itemLabel);
					item1.setValue(cpl.getId());
					subCpmb.appendChild(item1);

					Comboitem item2 = new Comboitem(itemLabel);
					item2.setValue(cpl.getId());
					subCpmb2.appendChild(item2);

					Comboitem item3 = new Comboitem(itemLabel);
					item3.setValue(cpl.getId());
					subCpmb3.appendChild(item3);

					Comboitem item4 = new Comboitem(itemLabel);
					item4.setValue(cpl.getId());
					subCpmb4.appendChild(item4);

					Comboitem item5 = new Comboitem(itemLabel);
					item5.setValue(cpl.getId());
					subCpmb5.appendChild(item5);
				} else {
					JSONArray array = new JSONArray(cpl.getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObjectD = array.getJSONObject(i);
						if (jsonObjectD.isNull("key"))
							continue;

						String kode = !jsonObjectD.isNull("kode") ? jsonObjectD.get("kode") + "" : "";
						String nama = !jsonObjectD.isNull("nama") ? jsonObjectD.get("nama") + "" : "";
						String val = jsonObjectD.get("key").toString() + "_" + cpl.getId();
						String desc = cpl.getKode() + " " + cpl.getNama();

						Comboitem ci1 = new Comboitem(kode + " " + nama);
						ci1.setDescription(desc);
						ci1.setValue(val);
						subCpmb.appendChild(ci1);
						Comboitem ci2 = new Comboitem(kode + " " + nama);
						ci2.setDescription(desc);
						ci2.setValue(val);
						subCpmb2.appendChild(ci2);
						Comboitem ci3 = new Comboitem(kode + " " + nama);
						ci3.setDescription(desc);
						ci3.setValue(val);
						subCpmb3.appendChild(ci3);
						Comboitem ci4 = new Comboitem(kode + " " + nama);
						ci4.setDescription(desc);
						ci4.setValue(val);
						subCpmb4.appendChild(ci4);
						Comboitem ci5 = new Comboitem(kode + " " + nama);
						ci5.setDescription(desc);
						ci5.setValue(val);
						subCpmb5.appendChild(ci5);
					}
				}
			}
		} finally {
			closeSessionQuietly(session);
		}

		// Helper untuk mempermudah render Combobox Sub CPMK
		renderSubCpmkCombo(row, rows, subCpmb, kurikulumPunyaMatakuliah, jsonObject, "sub_cpmk",
				Common.getBahasaConfig(kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? "CPMK *" : "Sub-CPMK *"));
		renderSubCpmkCombo(row, rows, subCpmb2, kurikulumPunyaMatakuliah, jsonObject, "sub_cpmk2",
				Common.getBahasaConfig(
						kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? "CPMK Ke-2 *" : "Sub-CPMK Ke-2 *"));
		renderSubCpmkCombo(row, rows, subCpmb3, kurikulumPunyaMatakuliah, jsonObject, "sub_cpmk3",
				Common.getBahasaConfig(
						kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? "CPMK Ke-3 *" : "Sub-CPMK Ke-3 *"));
		renderSubCpmkCombo(row, rows, subCpmb4, kurikulumPunyaMatakuliah, jsonObject, "sub_cpmk4",
				Common.getBahasaConfig(
						kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? "CPMK Ke-4 *" : "Sub-CPMK Ke-4 *"));
		renderSubCpmkCombo(row, rows, subCpmb5, kurikulumPunyaMatakuliah, jsonObject, "sub_cpmk5",
				Common.getBahasaConfig(
						kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? "CPMK Ke-5 *" : "Sub-CPMK Ke-5 *"));

		EventListener eventListenerJml = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer jumlahCpmdata = (Integer) (jumlahCpmb.getSelectedItem() == null ? 1
						: jumlahCpmb.getSelectedItem().getValue());
				subCpmb.getParent().setVisible(true);
				subCpmb2.getParent().setVisible(jumlahCpmdata > 1);
				subCpmb3.getParent().setVisible(jumlahCpmdata > 2);
				subCpmb4.getParent().setVisible(jumlahCpmdata > 3);
				subCpmb5.getParent().setVisible(jumlahCpmdata > 4);
			}
		};

		jumlahCpmb.addEventListener("onChange", eventListenerJml);
		eventListenerJml.onEvent(null);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Indikator *")));
		final MyTextbox indikator = new MyTextbox(
				jsonObject.isNull("indikator") ? Common.getBahasaConfig("Ketepatan ..")
						: jsonObject.getString("indikator"));
		row.appendChild(indikator);
		indikator.setWidth("95%");
		indikator.setRows(5);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Teknik & Kriteria *")));
		final MyTextbox teknikDanKriteria = new MyTextbox(
				jsonObject.isNull("teknikDanKriteria") ? "- Kriteria: ...\n\n- Bentuk: ..."
						: jsonObject.getString("teknikDanKriteria"));
		row.appendChild(teknikDanKriteria);
		teknikDanKriteria.setWidth("95%");
		teknikDanKriteria.setRows(5);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Metode Pembelajaran *")));
		final MyTextbox metodePembelajaran = new MyTextbox(jsonObject.isNull("metodePembelajaran")
				? Common.getBahasaConfig("Project Based Learning atau lainnya ..")
				: jsonObject.getString("metodePembelajaran"));
		row.appendChild(metodePembelajaran);
		metodePembelajaran.setWidth("95%");
		metodePembelajaran.setRows(5);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Pembelajaran Luring *")));
		final MyTextbox pembelajaranLuring = new MyTextbox(jsonObject.isNull("pembelajaranLuring")
				? "- Kuliah: ...\n\n- Diskusi: ...\n\n- Tugas Individu: ...\n\n- Membentuk kelompok: ..."
				: jsonObject.getString("pembelajaranLuring"));
		row.appendChild(pembelajaranLuring);
		pembelajaranLuring.setWidth("95%");
		pembelajaranLuring.setRows(5);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Pembelajaran Daring *")));
		final MyTextbox pembelajaranDaring = new MyTextbox(jsonObject.isNull("pembelajaranDaring")
				? Common.getBahasaConfig("e-Learning: dilakukan dengan memanfaatkan aplikasi e-campus")
				: jsonObject.getString("pembelajaranDaring"));
		row.appendChild(pembelajaranDaring);
		pembelajaranDaring.setWidth("95%");
		pembelajaranDaring.setRows(5);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Pengalaman Belajar Mahasiswa")));
		final MyTextbox pengalamanBelajar = new MyTextbox(jsonObject.isNull("pengalamanBelajar")
				? "" : jsonObject.getString("pengalamanBelajar"));
		row.appendChild(pengalamanBelajar);
		pengalamanBelajar.setWidth("95%");
		pengalamanBelajar.setRows(5);

		// -- Setup Checkboxes for Bahan Kajian, Pustaka Utama, Pustaka Pendukung --
		final JSONObject bahanKajians = jsonObject.isNull("bahanKajians") ? new JSONObject()
				: jsonObject.getJSONObject("bahanKajians");
		setupCheckboxesRinci(rows, Common.getBahasaConfig("Bahan Kajian"),
				kurikulumPunyaMatakuliah.getMatakuliah().getBahanKajian(), bahanKajians, BahanKajian.class, jsonObject,
				"bahanKajians");

		final JSONObject pustakaUtamas = jsonObject.isNull("pustakaUtamas") ? new JSONObject()
				: jsonObject.getJSONObject("pustakaUtamas");
		setupCheckboxesRinci(rows, Common.getBahasaConfig("Pustaka Utama"), kurikulumPunyaMatakuliah.getPustaka(),
				pustakaUtamas, ReferensiLulusan.class, jsonObject, "pustakaUtamas");

		final JSONObject pustakaPendukungs = jsonObject.isNull("pustakaPendukungs") ? new JSONObject()
				: jsonObject.getJSONObject("pustakaPendukungs");
		setupCheckboxesRinci(rows, Common.getBahasaConfig("Pustaka Pendukung"),
				kurikulumPunyaMatakuliah.getPustakaPendukung(), pustakaPendukungs, ReferensiLulusan.class, jsonObject,
				"pustakaPendukungs");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setStyle("background: #ffffff; border-top: 1px solid #e2e8f0; padding: 12px;");
		south.setParent(borderlayout);
		
		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("float: right; background: transparent; border: none; padding-right: 15px;");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"), "/img/cancel.gif");
		cancel.setStyle("font-size: 13px; font-weight: bold; color: #475569; background-color: #f1f5f9; border-radius: 6px; padding: 6px 15px; border: 1px solid #cbd5e1; cursor: pointer; margin-right: 8px; text-decoration: none;");
		cancel.setTooltiptext(Common.getBahasaConfig("Tutup"));
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(Common.getBahasaConfig("Simpan"), "/img/save.gif");
		save.setStyle(BTN_SUCCESS);
		save.setTooltiptext(Common.getBahasaConfig("Proses Penyimpanan"));
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event event) throws Exception {
				if (subCpmb.getSelectedItem() == null || subCpmb.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show(
							kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()
									? Common.getBahasaConfig("CPMK harus diisi")
									: Common.getBahasaConfig(
											"Sub-CPMK (sebagai kemampuan akhir yang diharapkan) harus diisi"),
							Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (indikator.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show(Common.getBahasaConfig("Indikator harus diisi"),
							Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (teknikDanKriteria.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show(Common.getBahasaConfig("Teknik dan kriteria harus diisi"),
							Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (metodePembelajaran.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show(Common.getBahasaConfig("Metode Pembelajaran harus diisi"),
							Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (pembelajaranLuring.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show(Common.getBahasaConfig("Pembelajaran luring harus diisi"),
							Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (pembelajaranDaring.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show(Common.getBahasaConfig("Pembelajaran daring harus diisi"),
							Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				int mulai = mulaiMingguKe.getValue() == null ? 0 : mulaiMingguKe.getValue();
				int sampai = sampaiMingguKe.getValue() == null ? 0 : sampaiMingguKe.getValue();

				if (keyData == null) {
					TreeMap<Integer, Map> maps = kurikulumPunyaMatakuliah
							.populateRinci(jsonArraykurikulumPunyaMatakuliah);
					for (Map map : maps.values()) {
						JSONObject jsonObjMap = (JSONObject) map.get("jsonObject");
						JSONObject subCpmkMap = (JSONObject) map.get("subCpmk");
						CapaianPembelajaranLulusan cplData = (CapaianPembelajaranLulusan) map
								.get("capaianPembelajaranLulusanData");

						if (subCpmkMap != null && cplData != null) {
							int mggMulai = jsonObjMap.getInt("mulaiMingguKe");
							int mggSampai = jsonObjMap.getInt("sampaiMingguKe");

							if (mggMulai <= mulai && mggSampai >= mulai) {
								MyMessageboxConfig.show(
										Common.getBahasaConfig("Minggu mulai untuk pertemuan ke-") + mulai + " "
												+ Common.getBahasaConfig("sudah ada dalam rincian OBE"),
										Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
										MyMessageboxConfig.INFORMATION);
								return;
							}
							if (mggMulai <= sampai && mggSampai >= sampai) {
								MyMessageboxConfig.show(
										Common.getBahasaConfig("Minggu sampai untuk pertemuan ke-") + sampai + " "
												+ Common.getBahasaConfig("sudah ada dalam rincian OBE"),
										Common.getBahasaConfig("Peringatan"), MyMessageboxConfig.OK,
										MyMessageboxConfig.INFORMATION);
								return;
							}
						}
					}
				}

				if (sampai < mulai)
					sampai = mulai;

				Integer jumlahCpmdata = (Integer) (jumlahCpmb.getSelectedItem() == null ? 1
						: jumlahCpmb.getSelectedItem().getValue());

				jsonObject.put("metodePembelajaran", metodePembelajaran.getValue().trim());
				jsonObject.put("pembelajaranDaring", pembelajaranDaring.getValue().trim());
				jsonObject.put("pembelajaranLuring", pembelajaranLuring.getValue().trim());
				jsonObject.put("pengalamanBelajar", pengalamanBelajar.getValue().trim());
				jsonObject.put("teknikDanKriteria", teknikDanKriteria.getValue().trim());
				jsonObject.put("indikator", indikator.getValue().trim());

				// Set value dari Helper Combobox
				extractComboValues(jsonObject, subCpmb, "cpmk_des", "sub_cpmk_des", "sub_cpmk",
						kurikulumPunyaMatakuliah);
				extractComboValues(jsonObject, subCpmb2, "cpmk_des2", "sub_cpmk_des2", "sub_cpmk2",
						kurikulumPunyaMatakuliah);
				extractComboValues(jsonObject, subCpmb3, "cpmk_des3", "sub_cpmk_des3", "sub_cpmk3",
						kurikulumPunyaMatakuliah);
				extractComboValues(jsonObject, subCpmb4, "cpmk_des4", "sub_cpmk_des4", "sub_cpmk4",
						kurikulumPunyaMatakuliah);
				extractComboValues(jsonObject, subCpmb5, "cpmk_des5", "sub_cpmk_des5", "sub_cpmk5",
						kurikulumPunyaMatakuliah);

				jsonObject.put("mulaiMingguKe", mulai);
				jsonObject.put("sampaiMingguKe", sampai);
				jsonObject.put("pustakaUtamas", pustakaUtamas);
				jsonObject.put("pustakaPendukungs", pustakaPendukungs);
				jsonObject.put("bahanKajians", bahanKajians);
				jsonObject.put("jumlahCpmk", jumlahCpmdata == null ? 1 : jumlahCpmdata);

				if (keyData == null) {
					jsonArraykurikulumPunyaMatakuliah.put(Common.getGeneratedBarCode(15), jsonObject);
				} else {
					jsonArraykurikulumPunyaMatakuliah.put(keyData, jsonObject);
				}
				kurikulumPunyaMatakuliah.setRincian(jsonArraykurikulumPunyaMatakuliah.toString());
				Common.refreshUpdate(kurikulumPunyaMatakuliah);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						window.detach();
						reloadRincian.onEvent(arg0);
					}
				});
			}
		});
		save.setParent(toolbar);
		window.onModal();
	}

	// HELPER UI: Meringkas pembuatan Combobox Sub CPMK
	private static void renderSubCpmkCombo(Row row, Rows rows, Combobox combo,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, JSONObject jsonObject, String jsonKey, String labelName)
			throws Exception {
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(labelName));
		row.appendChild(combo);
		combo.setWidth("95%");
		combo.setReadonly(true);

		Comboitem ciDef = new Comboitem(Common.getBahasaConfig("== Belum Ditentukan =="));
		ciDef.setDescription(
				Common.getBahasaConfig(kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? "CPMK belum ditentukan"
						: "Sub-CPMK belum ditentukan"));
		ciDef.setValue("-1");
		combo.appendChild(ciDef);

		if (kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()) {
			try {
				Common.selectComboItem(combo,
						jsonObject.isNull(jsonKey) ? null : Long.valueOf(jsonObject.getString(jsonKey).trim()));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		} else {
			Common.selectComboItem(combo, jsonObject.isNull(jsonKey) ? null : jsonObject.getString(jsonKey));
		}
	}

	// HELPER: Ekstraksi Data dari Combobox ke JSON
	private static void extractComboValues(JSONObject jsonObject, Combobox combo, String keyCpmkDes,
			String keySubCpmkDes, String keySubCpmk, KurikulumPunyaMatakuliah kur) {
		try {
			if (combo.getSelectedItem() != null && combo.getSelectedItem().getValue() != null) {
				jsonObject.put(keyCpmkDes, kur.getNilaiMenggunakanCpmk() ? combo.getSelectedItem().getValue()
						: combo.getSelectedItem().getDescription());
				jsonObject.put(keySubCpmkDes, combo.getSelectedItem().getLabel());
				jsonObject.put(keySubCpmk, combo.getSelectedItem().getValue().toString());
			} else {
				jsonObject.put(keySubCpmk, "");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	// HELPER UI: Render Checkboxes List untuk Edit Rincian
	@SuppressWarnings("rawtypes")
	private static void setupCheckboxesRinci(Rows rows, String label, String idsFromMk, final JSONObject jsonObjectData,
			final Class clazz, final JSONObject mainJsonObject, final String mainJsonKey) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfigAgakBesar(label));
		Vbox vbox = new Vbox();
		row.appendChild(vbox);

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			for (final String idStr : idsFromMk.split(",")) {
				try {
					if (!idStr.trim().isEmpty()) {
						final Object entity = ConstantValues.ambil(clazz.getName(), Long.parseLong(idStr.trim()));
						if (entity != null) {
							final String namaValue = clazz == BahanKajian.class ? ((BahanKajian) entity).getNama()
									: ((ReferensiLulusan) entity).getNama();
							final Long idValue = clazz == BahanKajian.class ? ((BahanKajian) entity).getId()
									: ((ReferensiLulusan) entity).getId();

							Hbox hboxD = new Hbox();
							vbox.appendChild(hboxD);
							final Checkbox pilih = new Checkbox(namaValue);
							hboxD.appendChild(pilih);
							pilih.setChecked(!jsonObjectData.isNull(idStr));

							pilih.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									if (pilih.isChecked()) {
										JSONObject itemObj = jsonObjectData.isNull(idStr) ? new JSONObject()
												: jsonObjectData.getJSONObject(idStr);
										itemObj.put("id", idValue);
										itemObj.put("nama", namaValue);
										jsonObjectData.put(idStr, itemObj);
									} else {
										jsonObjectData.remove(idStr);
									}
									mainJsonObject.put(mainJsonKey, jsonObjectData);
								}
							});

							// Jika Referensi Lulusan, sediakan fitur download file lampiran
							if (clazz == ReferensiLulusan.class) {
								Hbox hboxF = new Hbox();
								hboxF.setParent(hboxD);
								LampiranLain.createDownloadUploadFileLain(hboxF, idValue,
										ReferensiLulusan.class.getName(), Common.getBahasaConfig("Lampiran"), false,
										null, null, false, false, false, false);
							}
						}
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings("deprecation")
	private java.util.List<String[]> ambilBahanKajianList(String csv) {
		java.util.List<String[]> l = new java.util.ArrayList<String[]>();
		if (csv == null) {
			return l;
		}
		for (String s : csv.split(",")) {
			s = s.trim();
			if (s.length() == 0) {
				continue;
			}
			try {
				BahanKajian b = (BahanKajian) ConstantValues.ambil(BahanKajian.class.getName(), Long.parseLong(s));
				if (b != null) {
					l.add(new String[]{ String.valueOf(b.getId()), b.getNama() != null ? b.getNama() : "" });
				}
			} catch (Exception e) {
			}
		}
		return l;
	}

	private java.util.List<String[]> ambilReferensiList(String csv) {
		java.util.List<String[]> l = new java.util.ArrayList<String[]>();
		if (csv == null) {
			return l;
		}
		for (String s : csv.split(",")) {
			s = s.trim();
			if (s.length() == 0) {
				continue;
			}
			try {
				ReferensiLulusan r = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(),
						Long.parseLong(s));
				if (r != null) {
					l.add(new String[]{ String.valueOf(r.getId()), r.getNama() != null ? r.getNama() : "" });
				}
			} catch (Exception e) {
			}
		}
		return l;
	}

	/** Bangun map JSON {id:{id,nama}} dari daftar NOMOR (1-based) yang dipilih AI ke daftar referensi. */
	private JSONObject bangunMapRef(org.json.JSONArray nos, java.util.List<String[]> list) throws Exception {
		JSONObject m = new JSONObject();
		if (nos == null) {
			return m;
		}
		for (int i = 0; i < nos.length(); i++) {
			int no = nos.optInt(i, 0);
			if (no >= 1 && no <= list.size()) {
				String[] it = list.get(no - 1);
				JSONObject o = new JSONObject();
				o.put("id", it[0]);
				o.put("nama", it[1]);
				m.put(it[0], o);
			}
		}
		return m;
	}

	/**
	 * Popup Generate AI untuk Rincian/Agenda: tanya jumlah pertemuan (default 16 − minggu terisi) + catatan
	 * opsional; AI memetakan Sub-CPMK &amp; mengisi tiap pertemuan (indikator, teknik, metode, luring, daring,
	 * bahan kajian, pustaka), lalu disimpan ke kurikulumPunyaMatakuliah.getRincian().
	 */
	private void bukaGenerateRinciAi() throws Exception {
		// Hitung minggu yang sudah tertutup.
		int banyakMinggu = 0;
		try {
			java.util.TreeMap maps = kurikulumPunyaMatakuliah.populateRinci(jsonArraykurikulumPunyaMatakuliah);
			for (Object mo : maps.values()) {
				Object jo = ((java.util.Map) mo).get("jsonObject");
				if (jo instanceof JSONObject) {
					int smp = ((JSONObject) jo).optInt("sampaiMingguKe", 0);
					if (smp > banyakMinggu) {
						banyakMinggu = smp;
					}
				}
			}
		} catch (Exception e) {
		}
		int dtTmp = 16 - banyakMinggu;
		if (dtTmp < 1) {
			dtTmp = 1;
		}
		final int defTambah = dtTmp;
		final int startWeek = banyakMinggu + 1;

		// Kumpulkan Sub-CPMK.
		final boolean modeCpmk = Boolean.TRUE.equals(kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk());
		final java.util.List<String[]> subList = new java.util.ArrayList<String[]>();
		Session sc = null;
		try {
			sc = HibernateUtil.openSession();
			java.util.Set<Long> ids = parseIdsToSet(matakuliah.getCapaianPembelajaranLulusan());
			if (!ids.isEmpty()) {
				List<CapaianPembelajaranLulusan> cplList = ConstantValues.simpleList(
						sc.createCriteria(CapaianPembelajaranLulusan.class).add(Restrictions.in("id", ids))
								.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
						CapaianPembelajaranLulusan.class);
				for (CapaianPembelajaranLulusan cpl : cplList) {
					if (modeCpmk) {
						subList.add(new String[]{ String.valueOf(cpl.getId()),
								((cpl.getKode() != null ? cpl.getKode() : "") + " "
										+ (cpl.getNama() != null ? cpl.getNama() : "")).trim() });
					} else {
						try {
							org.json.JSONArray fa = new org.json.JSONArray(cpl.getFormula());
							for (int i = 0; i < fa.length(); i++) {
								JSONObject d = fa.getJSONObject(i);
								if (d.isNull("key")) {
									continue;
								}
								String kode = d.isNull("kode") ? "" : d.get("kode") + "";
								String nama = d.isNull("nama") ? "" : d.get("nama") + "";
								subList.add(new String[]{ d.get("key").toString() + "_" + cpl.getId(),
										(kode + " " + nama).trim() });
							}
						} catch (Exception e) {
						}
					}
				}
			}
		} finally {
			closeSessionQuietly(sc);
		}

		final java.util.List<String[]> bkList = ambilBahanKajianList(matakuliah.getBahanKajian());
		final java.util.List<String[]> puList = ambilReferensiList(kurikulumPunyaMatakuliah.getPustaka());
		final java.util.List<String[]> ppList = ambilReferensiList(kurikulumPunyaMatakuliah.getPustakaPendukung());
		final String namaMk = matakuliah != null && matakuliah.getNama() != null ? matakuliah.getNama() : "";

		// Popup parameter.
		final MyWindow win = new MyWindow(Common.getBahasaConfig("Generate Rincian/Agenda via AI"), "none", true);
		win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		win.setWidth("520px");
		Vbox vb = new Vbox();
		vb.setStyle("padding:16px;width:100%;box-sizing:border-box;");
		vb.setWidth("100%");
		vb.setParent(win);

		Label l1 = new Label(Common.getBahasaConfig("Jumlah pertemuan yang ingin ditambahkan") + ":");
		l1.setStyle("font-weight:bold;font-size:11px;");
		vb.appendChild(l1);
		final org.zkoss.zul.Intbox tJml = new org.zkoss.zul.Intbox(defTambah);
		tJml.setWidth("90px");
		vb.appendChild(tJml);
		Label l1b = new Label(
				"(" + Common.getBahasaConfig("Sudah terisi sampai minggu ke") + " " + banyakMinggu + "; "
						+ Common.getBahasaConfig("target 16 pertemuan") + ")");
		l1b.setStyle("font-size:10px;color:#94a3b8;display:block;margin-bottom:8px;");
		vb.appendChild(l1b);

		Label l2 = new Label(Common.getBahasaConfig("Catatan tambahan untuk AI (opsional)") + ":");
		l2.setStyle("font-weight:bold;font-size:11px;");
		vb.appendChild(l2);
		final org.zkoss.zul.Textbox tCatatan = new org.zkoss.zul.Textbox();
		tCatatan.setMultiline(true);
		tCatatan.setRows(3);
		tCatatan.setWidth("100%");
		vb.appendChild(tCatatan);

		Hbox bb = new Hbox();
		bb.setStyle("margin-top:14px;");
		vb.appendChild(bb);
		MyToolbarbuttonConfig btnGo = new MyToolbarbuttonConfig(Common.getBahasaConfig("Generate"),
				"/img/svg/sparkles.svg");
		btnGo.setStyle("color:#fff;background-color:#16a34a;border-radius:6px;padding:6px 14px;border:none;"
				+ "cursor:pointer;margin-right:6px;");
		btnGo.setParent(bb);
		btnGo.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				int jml = tJml.getValue() == null ? defTambah : tJml.getValue().intValue();
				if (jml < 1) {
					jml = 1;
				}
				if (jml > 24) {
					jml = 24;
				}
				final int jmlF = jml;
				final String catatan = tCatatan.getValue() == null ? "" : tCatatan.getValue().trim();
				win.detach();

				StringBuilder p = new StringBuilder();
				p.append("Buat rencana ").append(jmlF).append(" pertemuan RPS untuk mata kuliah \"").append(namaMk)
						.append("\", dimulai dari minggu ke-").append(startWeek).append(".\n\n");
				p.append("Sub-CPMK tersedia (rujuk dengan NOMOR):\n");
				if (subList.isEmpty()) {
					p.append("(belum ada Sub-CPMK)\n");
				} else {
					for (int i = 0; i < subList.size(); i++) {
						p.append(i + 1).append(". ").append(subList.get(i)[1]).append("\n");
					}
				}
				if (!bkList.isEmpty()) {
					p.append("\nBahan Kajian tersedia (NOMOR):\n");
					for (int i = 0; i < bkList.size(); i++) {
						p.append(i + 1).append(". ").append(bkList.get(i)[1]).append("\n");
					}
				}
				if (!puList.isEmpty()) {
					p.append("\nPustaka Utama tersedia (NOMOR):\n");
					for (int i = 0; i < puList.size(); i++) {
						p.append(i + 1).append(". ").append(puList.get(i)[1]).append("\n");
					}
				}
				if (!ppList.isEmpty()) {
					p.append("\nPustaka Pendukung tersedia (NOMOR):\n");
					for (int i = 0; i < ppList.size(); i++) {
						p.append(i + 1).append(". ").append(ppList.get(i)[1]).append("\n");
					}
				}
				if (catatan.length() > 0) {
					p.append("\nCatatan tambahan dari dosen: ").append(catatan).append("\n");
				}
				p.append("\nUntuk TIAP pertemuan: pilih Sub-CPMK yang paling cocok (NOMOR), isi indikator, ");
				p.append("teknik & kriteria, metode pembelajaran, pembelajaran luring, pembelajaran daring, dan ");
				p.append("pilih bahan kajian/pustaka relevan (NOMOR, boleh kosong []). Bahasa Indonesia akademis.\n");
				p.append("Keluarkan HANYA JSON array valid berisi ").append(jmlF)
						.append(" objek (tanpa teks lain), format persis:\n");
				p.append("[{\"minggu\":").append(startWeek)
						.append(",\"subCpmkNo\":1,\"indikator\":\"...\",\"teknikDanKriteria\":\"- Kriteria: ...\\n"
								+ "- Bentuk: ...\",\"metodePembelajaran\":\"...\",\"pembelajaranLuring\":\"- Kuliah: ...\\n"
								+ "- Diskusi: ...\",\"pembelajaranDaring\":\"e-Learning: ...\",\"pengalamanBelajar\":\"...\","
								+ "\"bahanKajianNo\":[1],\"pustakaUtamaNo\":[1],\"pustakaPendukungNo\":[]}]\n");
				final String prompt = p.toString();

				jalankanAiStreaming(Common.getBahasaConfig("Generate Rincian/Agenda via AI"), prompt,
						new HasilAiListener() {
							@Override
							public void selesai(String resp) throws Exception {
								int a = resp.indexOf('[');
								int b = resp.lastIndexOf(']');
								if (a < 0 || b <= a) {
									return;
								}
								org.json.JSONArray arr = new org.json.JSONArray(resp.substring(a, b + 1));
								int dibuat = 0;
								for (int i = 0; i < arr.length(); i++) {
									JSONObject o = arr.optJSONObject(i);
									if (o == null) {
										continue;
									}
									int minggu = o.optInt("minggu", startWeek + i);
									JSONObject r = new JSONObject();
									r.put("mulaiMingguKe", minggu);
									r.put("sampaiMingguKe", minggu);
									r.put("jumlahCpmk", 1);
									int subNo = o.optInt("subCpmkNo", 0);
									String subVal = (subNo >= 1 && subNo <= subList.size()) ? subList.get(subNo - 1)[0]
											: "-1";
									String subLabel = (subNo >= 1 && subNo <= subList.size())
											? subList.get(subNo - 1)[1] : "";
									r.put("sub_cpmk", subVal);
									r.put("sub_cpmk_des", subLabel);
									r.put("cpmk_des", subLabel);
									r.put("indikator", o.optString("indikator", ""));
									r.put("teknikDanKriteria", o.optString("teknikDanKriteria", ""));
									r.put("metodePembelajaran", o.optString("metodePembelajaran", ""));
									r.put("pembelajaranLuring", o.optString("pembelajaranLuring", ""));
									r.put("pembelajaranDaring", o.optString("pembelajaranDaring", ""));
									r.put("pengalamanBelajar", o.optString("pengalamanBelajar", ""));
									r.put("bahanKajians", bangunMapRef(o.optJSONArray("bahanKajianNo"), bkList));
									r.put("pustakaUtamas", bangunMapRef(o.optJSONArray("pustakaUtamaNo"), puList));
									r.put("pustakaPendukungs", bangunMapRef(o.optJSONArray("pustakaPendukungNo"), ppList));
									jsonArraykurikulumPunyaMatakuliah.put(Common.getGeneratedBarCode(15), r);
									dibuat++;
								}
								kurikulumPunyaMatakuliah.setRincian(jsonArraykurikulumPunyaMatakuliah.toString());
								Common.refreshUpdate(kurikulumPunyaMatakuliah);
								reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah,
										perkuliahan, rowRinci, bolehUbahObe, bolehUbahObe, pertemuansData,
										!tampilkanHanyaYgAktif.isChecked());
								MyMessageboxConfig.show(
										dibuat + " " + Common.getBahasaConfig("pertemuan berhasil dibuat via AI."),
										Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
										MyMessageboxConfig.INFORMATION);
							}
						});
			}
		});
		MyToolbarbuttonConfig btnCancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Batal"),
				"/img/svg/close-circle-line.svg");
		btnCancel.setStyle("color:#fff;background-color:#dc2626;border-radius:6px;padding:6px 14px;border:none;"
				+ "cursor:pointer;");
		btnCancel.setParent(bb);
		btnCancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				win.detach();
			}
		});
		win.onModal();
	}

	private void initRinci(boolean refresh) throws Exception {
		tampilkanHanyaYgAktif = new MyCheckboxConfig(Common.getBahasaConfig("Hanya yang Aktif"));
		tampilkanHanyaYgAktif.setChecked(true);

		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Rincian Kurikulum"));
		rowsUtama.appendChild(group);

		MyFormRow row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setValign("top");
		row.setParent(rowsUtama);

		Vbox vbox = new Vbox();
		vbox.setParent(row);
		Hbox hbox = new Hbox();
		vbox.appendChild(hbox);
		hbox.setVisible(
				bolehUbahObe && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getGuru() == null);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tambah Rincian Baru"),
				"/img/svg/addthis.svg");
		button.setStyle(BTN_SUCCESS);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				RpsObeAction.editRinci(new JSONObject(), null, kurikulumPunyaMatakuliah,
						jsonArraykurikulumPunyaMatakuliah, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah,
										perkuliahan, rowRinci, bolehUbahObe, bolehUbahObe, pertemuansData,
										!tampilkanHanyaYgAktif.isChecked());
							}
						});
			}
		});

		if (perkuliahan != null) {
			PenjadwalanHelper.tampilTombolAturUlangWaktu(hbox, perkuliahan, null, null, null, null, null, null, null,
					new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							perkuliahan.belum();
							onSearchDefault(arg0);
						}
					}).setStyle(BTN_PRIMARY);

			button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Urutkan Otomatis Berdasarkan Tanggal"),
					"/img/svg/refresh-cw.svg");
			button.setStyle(BTN_PRIMARY);
			button.setParent(hbox);
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@SuppressWarnings("rawtypes")
						@Override
						public void onEvent(Event arg0) throws Exception {
							int banyak = 0;
							TreeMap<Integer, Map> maps = kurikulumPunyaMatakuliah
									.populateRinci(jsonArraykurikulumPunyaMatakuliah);
							for (Map map : maps.values()) {
								JSONObject jsonObject = (JSONObject) map.get("jsonObject");
								JSONObject subCpmk = (JSONObject) map.get("subCpmk");
								CapaianPembelajaranLulusan cplData = (CapaianPembelajaranLulusan) map
										.get("capaianPembelajaranLulusanData");
								if (subCpmk != null && cplData != null) {
									int sampaiMingguKe = jsonObject.getInt("sampaiMingguKe");
									if (banyak < sampaiMingguKe)
										banyak = sampaiMingguKe;
								}
							}

							refreshPertemuan(banyak, pertemuansData, perkuliahan);

							Session session = null;
							try {
								session = HibernateUtil.openSession();
								List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.addOrder(Order.asc("tanggal")).add(Restrictions.isNotNull("tanggal"))
										.addOrder(Order.asc("id")).add(Restrictions.eq("perkuliahan", perkuliahan))
										.list();

								int pertemuanKe = 1;
								for (Pertemuan pertemuan : pertemuansTemp) {
									pertemuan.setPertemuanManual(pertemuanKe);
									pertemuan.setPertemuanKe(pertemuanKe);
									Common.refreshUpdate(session, pertemuan);
									pertemuanKe++;
								}
								perkuliahan.reInitPertemuan(pertemuansTemp, session);
							} finally {
								closeSessionQuietly(session);
							}

							reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah, perkuliahan,
									rowRinci, bolehUbahObe, bolehUbahObe, pertemuansData, !tampilkanHanyaYgAktif.isChecked());
						}
					});
				}
			});

			if (Common.getApakahAdmin(Tbmrole.AKADEMIK + "," + Tbmrole.ADMINISTRATOR)) {
				hbox.appendChild(tampilkanHanyaYgAktif);
				tampilkanHanyaYgAktif.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah,
										perkuliahan, rowRinci, bolehUbahObe, bolehUbahObe, pertemuansData,
										!tampilkanHanyaYgAktif.isChecked());
							}
						});
					}
				});
			}
		}

		if (bolehUbahObe) {
			MyToolbarbuttonConfig btnGenRinci = new MyToolbarbuttonConfig(Common.getBahasaConfig("Generate AI"),
					"/img/svg/sparkles.svg");
			btnGenRinci.setStyle("font-size:12px;font-weight:bold;color:#ffffff;background-color:#7c3aed;"
					+ "border-radius:6px;padding:6px 15px;border:none;cursor:pointer;");
			btnGenRinci.setParent(hbox);
			btnGenRinci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					bukaGenerateRinciAi();
				}
			});
		}

		button = new MyToolbarbuttonConfig(Common.getBahasaConfig("Segarkan (Refresh)"), "/img/svg/refresh-cw.svg");
		button.setStyle(BTN_PRIMARY);
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah, perkuliahan,
								rowRinci, bolehUbahObe, bolehUbahObe, pertemuansData, !tampilkanHanyaYgAktif.isChecked());
					}
				});
			}
		});

		try {
			jsonArraykurikulumPunyaMatakuliah = new JSONObject(kurikulumPunyaMatakuliah.getRincian());
		} catch (Exception e) {
			jsonArraykurikulumPunyaMatakuliah = new JSONObject();
		}

		rowRinci = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowRinci, "2");
		rowRinci.setValign("top");
		rowRinci.setParent(rowsUtama);
		reloadRinci(refresh, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah, perkuliahan, rowRinci, bolehUbahObe,
				bolehUbahObe, pertemuansData, !tampilkanHanyaYgAktif.isChecked());
	}

	public static void displayRinci(Component parent, int mulaiMingguKe, int sampaiMingguKe, boolean refresh,
			JSONObject jsonObject, JSONObject subCpmk, CapaianPembelajaranLulusan capaianPembelajaranLulusanData,
			Map<Integer, Pertemuan> pertemuansData, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			Perkuliahan perkuliahan, EventListener refreshEvent, boolean edit) {

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(parent);
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig().setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int minggu = mulaiMingguKe; minggu <= sampaiMingguKe; minggu++) {
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			try {
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				hbox.setAlign("center");
				hbox.setPack("center");
				hbox.setWidth("100%");
				displayMinggu(hbox, minggu, refresh, jsonObject, subCpmk, capaianPembelajaranLulusanData,
						pertemuansData, kurikulumPunyaMatakuliah, perkuliahan, edit, refreshEvent);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void updatePertemuan(Pertemuan pertemuan, int minggu, JSONObject jsonObject, String tpkData)
			throws Exception {
		String indikator = jsonObject.isNull("indikator") ? "" : jsonObject.getString("indikator");
		String teknikDanKriteria = jsonObject.isNull("teknikDanKriteria") ? ""
				: jsonObject.getString("teknikDanKriteria");
		String metodePembelajaran = jsonObject.isNull("metodePembelajaran")
				? Common.getBahasaConfig("Project Based Learning atau lainnya ..")
				: jsonObject.getString("metodePembelajaran");
		String pembelajaranLuring = jsonObject.isNull("pembelajaranLuring") ? ""
				: jsonObject.getString("pembelajaranLuring");
		String pembelajaranDaring = jsonObject.isNull("pembelajaranDaring") ? ""
				: jsonObject.getString("pembelajaranDaring");

		StringBuilder kajianBuilder = new StringBuilder();
		JSONObject bahanKajians = jsonObject.isNull("bahanKajians") ? new JSONObject()
				: jsonObject.getJSONObject("bahanKajians");
		Iterator<String> pus = bahanKajians.keys();
		while (pus.hasNext()) {
			try {
				String idBahan = pus.next();
				JSONObject p = bahanKajians.isNull(idBahan) ? new JSONObject() : bahanKajians.getJSONObject(idBahan);
				if (kajianBuilder.length() > 0)
					kajianBuilder.append(", ");
				kajianBuilder.append(p.getString("nama"));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		StringBuilder tpkBuilder = new StringBuilder();
		JSONObject pustakaUtamas = jsonObject.isNull("pustakaUtamas") ? new JSONObject()
				: jsonObject.getJSONObject("pustakaUtamas");
		pus = pustakaUtamas.keys();
		while (pus.hasNext()) {
			try {
				String idBahan = pus.next();
				JSONObject p = pustakaUtamas.isNull(idBahan) ? new JSONObject() : pustakaUtamas.getJSONObject(idBahan);
				ReferensiLulusan ref = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(),
						Long.parseLong(p.get("id") + ""));
				if (ref != null) {
					if (tpkBuilder.length() > 0)
						tpkBuilder.append(", ");
					tpkBuilder.append(ref.getNama());
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		JSONObject pustakaPendukungs = jsonObject.isNull("pustakaPendukungs") ? new JSONObject()
				: jsonObject.getJSONObject("pustakaPendukungs");
		pus = pustakaPendukungs.keys();
		while (pus.hasNext()) {
			try {
				String idBahan = pus.next();
				JSONObject p = pustakaPendukungs.isNull(idBahan) ? new JSONObject()
						: pustakaPendukungs.getJSONObject(idBahan);
				ReferensiLulusan ref = (ReferensiLulusan) ConstantValues.ambil(ReferensiLulusan.class.getName(),
						Long.parseLong(p.get("id") + ""));
				if (ref != null) {
					if (tpkBuilder.length() > 0)
						tpkBuilder.append(", ");
					tpkBuilder.append(ref.getNama());
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		pertemuan.setPertemuanManual(minggu);
		pertemuan.setPertemuanKe(minggu);
		pertemuan.setTopik(tpkData);
		pertemuan.setBukuRujukan1(kajianBuilder.toString());
		pertemuan.setBukuRujukan2(tpkBuilder.toString());
		pertemuan.setIndikator(indikator);
		pertemuan.setTugasDanPenilaian(teknikDanKriteria);
		pertemuan.setMetodePembelajaran(metodePembelajaran);
		pertemuan.setPengalamanBelajar(Common.getBahasaConfig("Pembelajaran Luring: ") + pembelajaranLuring + "\n\n"
				+ Common.getBahasaConfig("Pembelajaran Daring: ") + pembelajaranDaring);

		// Simpan pertemuan lewat SESSION KHUSUS bertransaksi sendiri, lalu commit & tutup di finally.
		// updatePertemuan dipanggil dari event TIMER (CommonTimerHelper). Pada konteks itu
		// currentSession bisa TANPA transaksi aktif atau sudah tertutup, sehingga pola lama
		// (Common.refreshSaveOrUpdate(currentSession) + commit + beginTransaction pada currentSession)
		// memunculkan "createCriteria is not valid without active transaction" dan, setelah commit
		// menutup session, "Session is closed!". Session terdedikasi: persist selalu commit permanen
		// (tombol Segarkan yang membaca via openSession lain tetap melihat data) dan aman dari state
		// session timer.
		//
		// AKAR "org.hibernate.AssertionFailure: entity was not detached" (dominan di log ECAMPUS):
		// merge(pertemuan) men-cascade merge ke asosiasi to-one milik pertemuan. Saat baris pertemuan
		// dimuat ulang oleh merge, asosiasi tersebut ikut termuat menjadi instance TERKELOLA di sesi
		// yang sama; ketika cascade mencoba me-merge asosiasi itu Hibernate menemukan target == entity
		// (bukan detached) lalu melempar AssertionFailure. Efeknya: perubahan topik/indikator TIDAK
		// tersimpan (ditelan catch) sehingga setiap reload memicu updatePertemuan lagi (error berulang
		// + beban tulis DB). Karena updatePertemuan HANYA mengubah kolom SKALAR (topik, indikator,
		// bukuRujukan, dst.), untuk baris yang SUDAH ADA cukup muat instance terkelola lalu salin
		// kolom skalar dan andalkan dirty-checking — TANPA merge/cascade. Baris baru (id null) atau
		// yang sudah terhapus di DB tetap memakai merge (perilaku lama dipertahankan).
		Session sesiSimpan = null;
		org.hibernate.Transaction txSimpan = null;
		try {
			sesiSimpan = HibernateUtil.openSession();
			txSimpan = sesiSimpan.beginTransaction();
			Pertemuan terkelola = pertemuan.getId() == null ? null
					: (Pertemuan) sesiSimpan.get(Pertemuan.class, pertemuan.getId());
			if (terkelola != null) {
				terkelola.setPertemuanManual(pertemuan.getPertemuanManual());
				terkelola.setPertemuanKe(pertemuan.getPertemuanKe());
				terkelola.setTopik(pertemuan.getTopik());
				terkelola.setBukuRujukan1(pertemuan.getBukuRujukan1());
				terkelola.setBukuRujukan2(pertemuan.getBukuRujukan2());
				terkelola.setIndikator(pertemuan.getIndikator());
				terkelola.setTugasDanPenilaian(pertemuan.getTugasDanPenilaian());
				terkelola.setMetodePembelajaran(pertemuan.getMetodePembelajaran());
				terkelola.setPengalamanBelajar(pertemuan.getPengalamanBelajar());
				txSimpan.commit(); // dirty-checking meng-UPDATE hanya kolom skalar yang berubah
			} else if (pertemuan.getId() == null) {
				// Pertemuan benar-benar BARU (belum pernah tersimpan). Insert via merge
				// (perilaku lama). Aman dari AssertionFailure karena sesi masih kosong:
				// asosiasi to-one dimuat segar saat cascade -> bukan instance yang sudah terkelola.
				Object hasilMerge = sesiSimpan.merge(pertemuan);
				txSimpan.commit();
				if (hasilMerge instanceof Pertemuan) {
					pertemuan.setId(((Pertemuan) hasilMerge).getId());
				}
			} else {
				// id != null TAPI baris tidak ada di DB (mis. sudah dihapus, atau penyimpanan
				// awal di refreshPertemuan gagal sehingga id sempat terpasang tapi row tak commit).
				// JANGAN merge: merge atas entitas ber-id yang barisnya hilang akan masuk jalur
				// transient-insert lalu men-cascade merge ke asosiasi to-one -> memicu
				// org.hibernate.AssertionFailure "entity was not detached" (persis error log ECAMPUS,
				// termasuk varian entityIsTransient -> cascadeBeforeSave). Tidak ada baris untuk
				// di-UPDATE, jadi lewati dengan aman tanpa mengganggu render.
				try { txSimpan.rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:4683");}
				txSimpan = null;
			}
		} catch (Exception e) {
			if (txSimpan != null) {
				try { txSimpan.rollback(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:4688");}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (sesiSimpan != null) {
				try { sesiSimpan.clear(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:4693");}
				try { sesiSimpan.disconnect(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:4694");}
				try { sesiSimpan.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:4695");}
			}
		}
	}

	public static void displayMinggu(final Component pertemuanBox, final int minggu, final boolean refresh,
			final JSONObject jsonObject, final JSONObject subCpmk,
			final CapaianPembelajaranLulusan capaianPembelajaranLulusanData,
			final Map<Integer, Pertemuan> pertemuansData, final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final Perkuliahan perkuliahan, final boolean edit, final EventListener refreshEvent) throws Exception {

		Common.clear(pertemuanBox);
		final Pertemuan pertemuan = pertemuansData == null ? null : pertemuansData.get(minggu);

		if (pertemuan != null) {
			String kodeSub = subCpmk.isNull("kode") ? "" : subCpmk.getString("kode");
			String namaSub = subCpmk.isNull("nama") ? Common.getBahasaConfig("Belum Ditentukan")
					: subCpmk.getString("nama");
			String tpkData = kodeSub + " " + namaSub;

			if (refresh || !pertemuan.getTopik().equalsIgnoreCase(tpkData.trim())) {
				updatePertemuan(pertemuan, minggu, jsonObject, tpkData);
			}

			if (pertemuan.getId() != null) {
				if (edit && (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getDikunci() == null)) {
					Hbox hboxdata = new Hbox();
					hboxdata.setParent(pertemuanBox);
					RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan, Common.getBahasaConfig("Minggu Ke-"))
							.setParent(hboxdata);

					final MyIntbox mingguKe = new MyIntbox(minggu);
					mingguKe.setCols(1);
					mingguKe.setParent(hboxdata);
					mingguKe.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Integer mgg = mingguKe.getValue();
							if (mgg != null && !mgg.equals(minggu)) {
								Session session = null;
								try {
									session = HibernateUtil.openSession();
									Pertemuan pertemuanLama = pertemuansData == null ? null : pertemuansData.get(mgg);
									if (pertemuanLama != null) {
										pertemuanLama.setPertemuanManual(pertemuan.getPertemuanManual());
										pertemuanLama.setPertemuanKe(pertemuan.getPertemuanKe());
										Common.refreshUpdate(session, pertemuanLama);
									}
									pertemuan.setPertemuanManual(mgg);
									pertemuan.setPertemuanKe(mgg);
									Common.refreshUpdate(session, pertemuan);
								} finally {
									closeSessionQuietly(session);
								}
								Common.createDefaultTimer(refreshEvent);
							}
						}
					});
				} else {
					RevisiHelper
							.createNewRevisi(Pertemuan.class, pertemuan,
									Common.getBahasaConfig("Minggu Ke-") + Common.numberFormat.get().format(minggu))
							.setParent(pertemuanBox);
				}
			} else {
				new MyLabelAgakBesar(Common.getBahasaConfig("Minggu Ke-") + Common.numberFormat.get().format(minggu))
						.setParent(pertemuanBox);
			}
			new Space().setParent(pertemuanBox);

			Hbox hboxUtama = new Hbox();
			hboxUtama.setAlign("center");
			hboxUtama.setPack("center");
			hboxUtama.setWidth("100%");
			hboxUtama.setParent(pertemuanBox);

			final Combobox combobox = new Combobox();
			Common.insertCombo(combobox, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(combobox, pertemuan.getStatusPertemuan());
			combobox.setCols(5);
			combobox.setParent(hboxUtama);
			combobox.setReadonly(true);
			combobox.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setStatusPertemuan((StatusPertemuan) combobox.getSelectedItem().getValue());
					Session session = null;
					try {
						session = HibernateUtil.openSession();
						Common.refreshUpdate(session, pertemuan);
					} finally {
						closeSessionQuietly(session);
					}
				}
			});

			Tbmuser currentUser = Common.getCurrentUser();
			if (currentUser.getMahasiswa() == null && (currentUser.ambilDosen() == null
					|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {
				Hbox vbox = new Hbox();
				vbox.setParent(hboxUtama);
				final MyDatebox mulai = new MyDatebox();
				mulai.setValue(pertemuan.getTanggal());
				mulai.setCols(5);
				mulai.setParent(vbox);
				mulai.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setTanggal(mulai.getValue());
						pertemuan.setTanggalEdit(mulai.getValue());
						Session session = null;
						try {
							session = HibernateUtil.openSession();
							Common.refreshUpdate(session, pertemuan);
						} finally {
							closeSessionQuietly(session);
						}
					}
				});

				final Timebox waktuMulai = new ais.ui.util.MyTimebox();
				final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
				waktuMulai.setFormat(Common.timeFormat.get().toPattern());
				waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

				try {
					waktuMulai.setValue(
							pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				try {
					waktuSelesai.setValue(
							pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				EventListener updateLocal = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuMulai.getValue()));
						pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuSelesai.getValue()));
						Session session = null;
						try {
							session = HibernateUtil.openSession();
							Common.refreshUpdate(session, pertemuan);
						} finally {
							closeSessionQuietly(session);
						}
					}
				};

				waktuMulai.setCols(1);
				waktuSelesai.setCols(1);
				waktuMulai.addEventListener("onChange", updateLocal);
				waktuSelesai.addEventListener("onChange", updateLocal);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				waktuMulai.setParent(hbox);
				waktuSelesai.setParent(hbox);
			} else {
				Vbox vbox = new Vbox();
				vbox.setParent(hboxUtama);
				new Label(pertemuan.getTanggal() == null ? "" : Common.dateFormat1.get().format(pertemuan.getTanggal()))
						.setParent(vbox);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				new MyLabelKecil(pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai()).setParent(hbox);
				new MyLabelKecil(pertemuan.getWaktuSelesai() == null ? "" : " s.d " + pertemuan.getWaktuSelesai())
						.setParent(hbox);
			}

			Component videoConferenceBtn = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false,
					new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							displayMinggu(pertemuanBox, minggu, false, jsonObject, subCpmk,
									capaianPembelajaranLulusanData, pertemuansData, kurikulumPunyaMatakuliah,
									perkuliahan, edit, refreshEvent);
						}
					});

			Component absenBtn = AbsensiHelper.createTombolAbsen(pertemuan, true, new DataLoader() {
				@Override
				public void loadData(Object value) {
					try {
						displayMinggu(pertemuanBox, minggu, false, jsonObject, subCpmk, capaianPembelajaranLulusanData,
								pertemuansData, kurikulumPunyaMatakuliah, perkuliahan, edit, refreshEvent);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			});

			AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {
				@Override
				public void loadData(Object value) {
					try {
						displayMinggu(pertemuanBox, minggu, false, jsonObject, subCpmk, capaianPembelajaranLulusanData,
								pertemuansData, kurikulumPunyaMatakuliah, perkuliahan, edit, refreshEvent);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}, videoConferenceBtn, absenBtn, DashboardTimelinePertemuan.createScanFoto(currentUser, pertemuan),
					hboxUtama).setParent(pertemuanBox);

			DashboardTimelinePertemuan.tampilOnline(pertemuan, pertemuanBox, currentUser, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						displayMinggu(pertemuanBox, minggu, false, jsonObject, subCpmk, capaianPembelajaranLulusanData,
								pertemuansData, kurikulumPunyaMatakuliah, perkuliahan, edit, refreshEvent);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static void refreshPertemuan(int banyak, Map<Integer, Pertemuan> pertemuansData, Perkuliahan perkuliahan) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
			perkuliahan.reInitPertemuan(pertemuansTemp, session);

			Date curr = perkuliahan.getTanggalMulaiPerkuliahan();
			Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
			if (curr != null)
				myCalendar.setTime(curr);

			if (perkuliahan.getJumlahMaksimalPertemuan() != null && banyak > perkuliahan.getJumlahMaksimalPertemuan()) {
				banyak = perkuliahan.getJumlahMaksimalPertemuan();
			}
			StringBuilder sqlIdsBuilder = new StringBuilder();
			for (int minggu = 1; minggu <= banyak; minggu++) {
				Pertemuan pert = null;
				for (Pertemuan p : pertemuansTemp) {
					if (p != null && p.getPertemuanKe().equals(minggu)) {
						pert = p;
						break;
					}
				}

				if (perkuliahan.getLewatiTanggalMerahNasional()) {
					myCalendar = Common.tanggalMerahAja(perkuliahan.getJenis(), myCalendar);
				}
				Date currDate = myCalendar.getTime();

				if (pert == null) {
					pert = new Pertemuan();
					pert.setPerkuliahan(perkuliahan);
					pert.setPertemuanManual(minggu);
					pert.setPertemuanKe(minggu);
					if (minggu == perkuliahan.getJumlahMaksimalPertemuan()) {
						pert.setStatusPertemuan(ConstantValues.UAS);
						pert.setTopik(Common.getBahasaConfig("Pertemuan Ke-") + minggu + " : UAS");
						pert.setMetodePembelajaran(Common.getBahasaConfig("Mengerjakan soal UAS"));
					} else if (minggu == (perkuliahan.getJumlahMaksimalPertemuan() / 2)) {
						pert.setStatusPertemuan(ConstantValues.UTS);
						pert.setTopik(Common.getBahasaConfig("Pertemuan Ke-") + minggu + " : UTS");
						pert.setMetodePembelajaran(Common.getBahasaConfig("Mengerjakan soal UTS"));
					} else {
						pert.setStatusPertemuan(ConstantValues.TATAP_MUKA);
					}
					pert.setWaktuMulai(perkuliahan.getWaktuMulai());
					pert.setWaktuSelesai(perkuliahan.getWaktuSelesai());
					pert.setTanggal(currDate);
					pert.setMulai(currDate);
					pert.setSelesai(null);
					pert.setRuang(perkuliahan.getRuang());
					// Simpan tiap pertemuan BARU dalam TRANSAKSI-nya SENDIRI. Sebelumnya save()+flush()
					// dijalankan TANPA transaksi pada session hasil openSession(); di PostgreSQL/Hibernate
					// operasi tulis tanpa transaksi bisa GAGAL, dan sekali satu gagal transaksi implisit
					// menjadi "aborted" sehingga SEMUA pertemuan berikutnya (mis. 9..16) ikut gagal. Dengan
					// transaksi per-pertemuan, satu kegagalan tidak meracuni pertemuan lain (commit/rollback
					// terisolasi), dan pertemuan yang berhasil benar-benar tersimpan.
					org.hibernate.Transaction txPert = null;
					try {
						txPert = session.beginTransaction();
						session.save(pert);
						txPert.commit();
						txPert = null;
					} catch (Exception e) {
						if (txPert != null) {
							try {
								txPert.rollback();
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:4994");
							}
						}
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					perkuliahan.populatePertemuan(pert);
				}

				// GUARD: bila penyimpanan pertemuan BARU gagal (mis. kendala DB pada pertemuan 9..16
				// yang belum ada), pert.getId() bisa NULL. Sebelumnya baris di bawah langsung memanggil
				// pert.getId().toString() → NPE yang MENGGAGALKAN SELURUH proses "membuat agenda" (loop
				// berhenti, pertemuan berikutnya tak ikut dibuat). Kini pertemuan tanpa id dilewati
				// dengan aman (error penyimpanannya sudah ditampilkan di atas), agar pertemuan lain tetap
				// diproses dan tanggal tetap maju.
				if (pert.getId() != null) {
					pertemuansData.put(pert.getPertemuanKe(), pert);

					if (sqlIdsBuilder.length() > 0)
						sqlIdsBuilder.append(",");
					sqlIdsBuilder.append(pert.getId().toString());
				}

				if (curr != null) {
					myCalendar = Common.curreDate(perkuliahan.getJenis(), myCalendar);
				}
			}

			if (sqlIdsBuilder.length() > 0) {
				String sqlUpdate = "update pertemuan set aktif=false where id not in (" + sqlIdsBuilder.toString()
						+ ") and perkuliahan=" + perkuliahan.getId();
				// DML native juga WAJIB di dalam transaksi (tanpa transaksi bisa gagal/tidak commit).
				org.hibernate.Transaction txUpd = null;
				try {
					txUpd = session.beginTransaction();
					session.createSQLQuery(sqlUpdate).executeUpdate();
					txUpd.commit();
					txUpd = null;
				} catch (Exception e) {
					if (txUpd != null) {
						try {
							txUpd.rollback();
						} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5035");
						}
					}
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
			pertemuansTemp.clear();
		} finally {
			closeSessionQuietly(session);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Component labelRingkas(final String teks) {
		final String isi = teks == null ? "" : teks.trim();
		if (isi.length() <= 25) {
			Label label = new Label(isi);
			label.setTooltiptext(isi);
			return label;
		}
		Hbox box = new Hbox();
		box.setStyle("align-items:flex-start;gap:4px;");
		Label ringkas = new Label(isi.substring(0, 25) + "...");
		ringkas.setTooltiptext(isi);
		ringkas.setParent(box);
		Label baca = new Label("baca");
		baca.setStyle("color:#2563eb;cursor:pointer;font-size:11px;font-weight:bold;text-decoration:underline;");
		baca.setParent(box);
		baca.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(isi, Common.getBahasaConfig("Teks Lengkap"), MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		return box;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void reloadRinci(boolean refresh, final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final JSONObject jsonArraykurikulumPunyaMatakuliah, final Perkuliahan perkuliahan, final Component rowRinci,
			final boolean edit, final boolean delete, final Map<Integer, Pertemuan> pertemuansData,
			final boolean tampilnaNonAktif) throws Exception {

		Common.clear(rowRinci);

		if (tampilnaNonAktif) {
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(rowRinci);
			grid.setHeight("100%");
			grid.setStyle(GRID_STYLE);
			
			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig col1 = new MyColumnConfig();
			col1.setWidth("0px");
			col1.setParent(columns);
			MyColumnConfig col2 = new MyColumnConfig("No.");
			col2.setWidth("30px");
			col2.setParent(columns);
			// Lebar kolom diskala agar TOTAL <= 100% (sebelumnya 20+20+8+20+18+10+10+10+10+5+18+5+3
			// = 157% → grid MELUBER horizontal sehingga tampilan "Hanya yang Aktif" OFF berantakan).
			MyColumnConfig col3 = new MyColumnConfig(Common.getBahasaConfig("Kemampuan Akhir Pembelajaran"));
			col3.setWidth("13%");
			col3.setParent(columns);
			MyColumnConfig col4 = new MyColumnConfig(Common.getBahasaConfig("Kriteria, Indikator & Bobot Penilaian"));
			col4.setWidth("13%");
			col4.setParent(columns);
			MyColumnConfig col5 = new MyColumnConfig(Common.getBahasaConfig("Waktu"));
			col5.setWidth("5%");
			col5.setParent(columns);
			MyColumnConfig col6 = new MyColumnConfig(Common.getBahasaConfig("Pengalaman Belajar"));
			col6.setWidth("13%");
			col6.setParent(columns);
			MyColumnConfig col7 = new MyColumnConfig(Common.getBahasaConfig("Tugas Dan Penilaian"));
			col7.setWidth("11%");
			col7.setParent(columns);
			MyColumnConfig col8 = new MyColumnConfig(Common.getBahasaConfig("Bahan Kajian"));
			col8.setWidth("6%");
			col8.setParent(columns);
			MyColumnConfig col9 = new MyColumnConfig(Common.getBahasaConfig("Referensi"));
			col9.setWidth("6%");
			col9.setParent(columns);
			MyColumnConfig col10 = new MyColumnConfig(Common.getBahasaConfig("Metode Pembelajaran"));
			col10.setWidth("6%");
			col10.setParent(columns);
			MyColumnConfig col11 = new MyColumnConfig(Common.getBahasaConfig("Jenis Pertemuan"));
			col11.setWidth("6%");
			col11.setParent(columns);
			MyColumnConfig col12 = new MyColumnConfig(Common.getBahasaConfig("Aktif"));
			col12.setWidth("3%");
			col12.setParent(columns);
			MyColumnConfig col13 = new MyColumnConfig(Common.getBahasaConfig("Tanggal/Waktu"));
			col13.setWidth("11%");
			col13.setParent(columns);
			MyColumnConfig col14 = new MyColumnConfig("");
			col14.setWidth("3%");
			col14.setParent(columns);

			MyColumnConfig colCheck = new MyColumnConfig();
			colCheck.setImage("/img/svg/check2-circle.svg");
			colCheck.setHoverImage("/img/svg/check-circled-outline.svg");
			colCheck.setWidth("2%");
			colCheck.setParent(columns);

			Session session = null;
			try {
				session = HibernateUtil.openSession();
				List<Long> pertemuanss = session.createCriteria(Pertemuan.class)
						.setProjection(Projections.property("id"))
						.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
						.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
						.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

				ListModel strset = new SimpleListModel(pertemuanss);
				grid.setRowRenderer(new PenjadwalanHelper.PertemuanRenderer(perkuliahan, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah, perkuliahan,
								rowRinci, edit, delete, pertemuansData, tampilnaNonAktif);
					}
				}));
				grid.setModelCheckMobile(strset);
			} finally {
				closeSessionQuietly(session);
			}

		} else {
			final EventListener refreshEvent = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					reloadRinci(true, kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah, perkuliahan,
							rowRinci, edit, delete, pertemuansData, tampilnaNonAktif);
				}
			};

			int banyak = 0;
			TreeMap<Integer, Map> maps = kurikulumPunyaMatakuliah.populateRinci(jsonArraykurikulumPunyaMatakuliah);
			for (Map map : maps.values()) {
				JSONObject jsonObject = (JSONObject) map.get("jsonObject");
				JSONObject subCpmk = (JSONObject) map.get("subCpmk");
				CapaianPembelajaranLulusan cplData = (CapaianPembelajaranLulusan) map
						.get("capaianPembelajaranLulusanData");
				if (subCpmk != null && cplData != null) {
					int sampaiMingguKe = jsonObject.getInt("sampaiMingguKe");
					if (banyak < sampaiMingguKe)
						banyak = sampaiMingguKe;
				}
			}

			if (perkuliahan != null && perkuliahan.getId() != null) {
				pertemuansData.clear();
				if (!refresh) {
					Object[] a = perkuliahan.ambilPertemuan(0, banyak, false);
					List<Long> pertemuans = (List<Long>) a[0];
					for (Long pertemuanid : pertemuans) {
						Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
						if (p != null)
							pertemuansData.put(p.getPertemuanKe(), p);
					}
				} else {
					refreshPertemuan(banyak, pertemuansData, perkuliahan);
				}
			}

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setStyle(GRID_STYLE);
			grid.setParent(rowRinci);
			grid.setHeight("100%");
			Auxhead auxhead = new Auxhead();
			auxhead.setParent(grid);

			Auxheader auxheader = new Auxheader();
			MyLabelConfig label = new MyLabelConfig(Common.getBahasaConfig("Minggu Ke"));
			label.setMultiline(true);
			auxheader.appendChild(label);
			auxheader.setParent(auxhead);

			auxheader = new Auxheader();
			label = new MyLabelConfig(
					kurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk() ? Common.getBahasaConfig("CPMK")
							: Common.getBahasaConfig("Sub-CPMK"));
			label.setMultiline(true);
			auxheader.appendChild(label);
			auxheader.setParent(auxhead);

			auxheader = new Auxheader();
			label = new MyLabelConfig(Common.getBahasaConfig("Penilaian"));
			label.setMultiline(true);
			auxheader.appendChild(label);
			auxheader.setColspan(2);
			auxheader.setParent(auxhead);

			auxheader = new Auxheader();
			auxheader.setColspan(4);
			label = new MyLabelConfig(
					Common.getBahasaConfig("Bentuk Pembelajaran, Metode Pembelajaran, Penugasan Mahasiswa"));
			label.setMultiline(true);
			auxheader.appendChild(label);
			auxheader.setParent(auxhead);

			auxheader = new Auxheader();
			label = new MyLabelConfig(Common.getBahasaConfig("Materi Pembelajaran"));
			label.setMultiline(true);
			auxheader.appendChild(label);
			auxheader.setParent(auxhead);
			new Auxheader().setParent(auxhead);

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig col1 = new MyColumnConfig("");
			col1.setWidth("0px");
			col1.setParent(columns);
			MyColumnConfig col2 = new MyColumnConfig("");
			col2.setWidth("70px");
			col2.setParent(columns);
			MyColumnConfig col3 = new MyColumnConfig("");
			col3.setWidth("30%");
			col3.setParent(columns);
			MyColumnConfig col4 = new MyColumnConfig(Common.getBahasaConfig("Indikator"));
			col4.setParent(columns);
			MyColumnConfig col5 = new MyColumnConfig(Common.getBahasaConfig("Teknik & Kriteria"));
			col5.setParent(columns);
			MyColumnConfig col6 = new MyColumnConfig(Common.getBahasaConfig("Metode Pembelajaran"));
			col6.setParent(columns);
			MyColumnConfig col7 = new MyColumnConfig(Common.getBahasaConfig("Pembelajaran Luring"));
			col7.setParent(columns);
			MyColumnConfig col8 = new MyColumnConfig(Common.getBahasaConfig("Pembelajaran Daring"));
			col8.setParent(columns);
			MyColumnConfig col9 = new MyColumnConfig("");
			col9.setParent(columns);
			MyColumnConfig col10 = new MyColumnConfig("");
			col10.setWidth("5%");
			col10.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);
			int max = 0;

			for (Map map : maps.values()) {
				final JSONObject jsonObject = (JSONObject) map.get("jsonObject");
				JSONObject subCpmk = (JSONObject) map.get("subCpmk");
				CapaianPembelajaranLulusan cplData = (CapaianPembelajaranLulusan) map
						.get("capaianPembelajaranLulusanData");

				if (subCpmk != null && cplData != null) {
					int jumlahCpmk = jsonObject.isNull("jumlahCpmk") ? 1 : jsonObject.getInt("jumlahCpmk");
					MyFormRow row = new MyFormRow();
					MyDetail detail = new MyDetail();
					detail.setParent(row);
					detail.setOpen(true);

					if (perkuliahan != null && perkuliahan.getId() != null) {
						MyGroupboxStyled tools = new MyGroupboxStyled();
						tools.setWidth("100%");
						tools.setParent(detail);
						tools.setStyleLangsung("text-align:center; border: 1px solid #e2e8f0; padding: 10px; background-color: #f8fafc; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); max-width: 98%; margin: 10px auto;");
						displayRinci(tools, jsonObject.getInt("mulaiMingguKe"), jsonObject.getInt("sampaiMingguKe"),
								refresh, jsonObject, subCpmk, cplData, pertemuansData, kurikulumPunyaMatakuliah,
								perkuliahan, refreshEvent, edit);
					}

					row.setValign("top");
					row.appendChild(new Label(
							jsonObject.getInt("mulaiMingguKe") + " s.d " + jsonObject.getInt("sampaiMingguKe")));

					Vbox idata = new Vbox();
					for (int i = 1; i <= jumlahCpmk; i++) {
						String de = jsonObject.isNull("sub_cpmk_des" + (i == 1 ? "" : i)) ? ""
								: jsonObject.get("sub_cpmk_des" + (i == 1 ? "" : i)) + "";
						if (!de.isEmpty())
							idata.appendChild(labelRingkas(de));
					}
					row.appendChild(idata);

					row.appendChild(labelRingkas(jsonObject.isNull("indikator") ? "" : jsonObject.getString("indikator")));
					row.appendChild(labelRingkas(
							jsonObject.isNull("teknikDanKriteria") ? "" : jsonObject.getString("teknikDanKriteria")));
					row.appendChild(labelRingkas(jsonObject.isNull("metodePembelajaran")
							? Common.getBahasaConfig("Project Based Learning atau lainnya ..")
							: jsonObject.getString("metodePembelajaran")));
					row.appendChild(labelRingkas(
							jsonObject.isNull("pembelajaranLuring") ? "" : jsonObject.getString("pembelajaranLuring")));
					row.appendChild(labelRingkas(
							jsonObject.isNull("pembelajaranDaring") ? "" : jsonObject.getString("pembelajaranDaring")));

					if (max < jsonObject.getInt("sampaiMingguKe"))
						max = jsonObject.getInt("sampaiMingguKe");

					StringBuilder pustakaBuilder = new StringBuilder();
					JSONObject bahanKajians = jsonObject.isNull("bahanKajians") ? new JSONObject()
							: jsonObject.getJSONObject("bahanKajians");
					Iterator<String> pus = bahanKajians.keys();
					while (pus.hasNext()) {
						try {
							String idBahan = pus.next();
							JSONObject p = bahanKajians.isNull(idBahan) ? new JSONObject()
									: bahanKajians.getJSONObject(idBahan);
							if (pustakaBuilder.length() > 0)
								pustakaBuilder.append(", ");
							pustakaBuilder.append(p.getString("nama"));
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					row.appendChild(labelRingkas(pustakaBuilder.toString()));

					Hbox hbox = new Hbox();
					row.appendChild(hbox);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setStyle(BTN_ICON);
					button.setTooltiptext(Common.getBahasaConfig("Ubah Data"));
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							RpsObeAction.editRinci(jsonObject, jsonObject.getString("keyData"),
									kurikulumPunyaMatakuliah, jsonArraykurikulumPunyaMatakuliah, refreshEvent);
						}
					});
					button.setParent(hbox);

					button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setStyle(BTN_ICON);
					button.setTooltiptext(Common.getBahasaConfig("Hapus Data"));
					button.setVisible(delete);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show(
									Common.getBahasaConfig("Apakah Anda yakin ingin menghapus data ini?"),
									Common.getBahasaConfig("Konfirmasi"),
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {
													jsonObject.remove("mulaiMingguKe");
													jsonObject.remove("sampaiMingguKe");
													kurikulumPunyaMatakuliah
															.setRincian(jsonArraykurikulumPunyaMatakuliah.toString());
													Common.refreshUpdate(kurikulumPunyaMatakuliah);
													refreshEvent.onEvent(event);
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(Common.getBahasaConfig(
															"Data ini tidak dapat dihapus karena berelasi. Error:")
															+ " " + e.getMessage());
												}
											}
										}
									});
						}
					});
					button.setParent(hbox);
					row.setParent(rows);
				}
			}

			if (perkuliahan != null && !perkuliahan.getJumlahMaksimalPertemuan().equals(max)) {
				perkuliahan.setJumlahMaksimalPertemuan(max);
				Common.refreshUpdate(perkuliahan);
			}
		}
	}

	@SuppressWarnings("deprecation")
	/** Generate Catatan (CkEditor) + seluruh Data Tambahan OBE via AI (streaming) lalu isi semua field. */
	private void generateCatatanObeAi() throws Exception {
		final String namaMk = matakuliah != null && matakuliah.getNama() != null ? matakuliah.getNama() : "";
		final String kodeMk = matakuliah != null && matakuliah.getKode() != null ? matakuliah.getKode() : "";
		final String deskr = (kurikulumPunyaMatakuliah != null
				&& kurikulumPunyaMatakuliah.getDeskripsiPembelajaran() != null)
						? kurikulumPunyaMatakuliah.getDeskripsiPembelajaran() : "";

		StringBuilder p = new StringBuilder();
		p.append("Buatkan DATA OBE lengkap untuk mata kuliah \"").append(namaMk).append("\" (kode ").append(kodeMk)
				.append("), tingkat perguruan tinggi, Bahasa Indonesia formal-akademis.\n");
		if (deskr != null && deskr.trim().length() > 0) {
			p.append("Deskripsi: ").append(deskr.trim()).append("\n");
		}
		p.append("\nKeluarkan HANYA JSON OBJECT valid (tanpa teks/markdown lain) dengan kunci PERSIS berikut, ");
		p.append("dan IKUTI format contoh tiap nilai:\n");
		p.append("{\n");
		p.append("\"catatan\":\"<p>Catatan/keterangan tambahan RPS &amp; daftar referensi (boleh HTML sederhana)</p>\",\n");
		p.append("\"cplBobot\":\"CPL-2:15,CPL-4:45,CPL-9:40\",\n");
		p.append("\"komponenPenilaian\":\"Kuis:10,Tugas:10,Keaktifan:10,UTS:30,UAS:40\",\n");
		p.append("\"teknikPerCpmk\":\"CPMK-1:Kuis,UTS\\nCPMK-2:Tugas,Unjuk Kerja,UAS\\nCPMK-3:Partisipasi,UTS,UAS\",\n");
		p.append("\"rubrikPenilaian\":\"#Rubrik Presentasi (40%)\\nKelengkapan Materi|15|Lengkap & mendalam|Lengkap kurang dalam|Kurang lengkap|Tidak lengkap\",\n");
		p.append("\"pemetaanSoalUts\":\"Sub CPMK 1.1|PG 1\\nSub CPMK 2.1|PG 8,12,17; Esai 4,5\",\n");
		p.append("\"pemetaanSoalUas\":\"Sub CPMK 2.4|PG 11,12,13\\nSub CPMK 4.1|PG 14-17; Esai 4,5\"\n");
		p.append("}\n");
		p.append("Total persen komponenPenilaian = 100; total cplBobot = 100. Gunakan \\n untuk baris baru dalam string.\n");
		final String prompt = p.toString();

		jalankanAiStreaming(Common.getBahasaConfig("Generate Catatan & Data OBE berdasarkan AI"), prompt,
				new HasilAiListener() {
					@Override
					public void selesai(String resp) throws Exception {
						String js = potongJsonObj(resp);
						if (js == null) {
							return;
						}
						org.json.JSONObject o = new org.json.JSONObject(js);
						if (catatan != null && o.has("catatan")) {
							String v = o.optString("catatan", "");
							catatan.setValue(v);
							kurikulumPunyaMatakuliah.setCatatan(v);
						}
						if (cplBobotField != null && o.has("cplBobot")) {
							String v = o.optString("cplBobot", "");
							cplBobotField.setValue(v);
							kurikulumPunyaMatakuliah.setCplBobot(v);
						}
						if (komponenPenilaianField != null && o.has("komponenPenilaian")) {
							String v = o.optString("komponenPenilaian", "");
							komponenPenilaianField.setValue(v);
							kurikulumPunyaMatakuliah.setKomponenPenilaian(v);
						}
						if (teknikPerCpmkField != null && o.has("teknikPerCpmk")) {
							String v = o.optString("teknikPerCpmk", "");
							teknikPerCpmkField.setValue(v);
							kurikulumPunyaMatakuliah.setTeknikPerCpmk(v);
						}
						if (rubrikPenilaianField != null && o.has("rubrikPenilaian")) {
							String v = o.optString("rubrikPenilaian", "");
							rubrikPenilaianField.setValue(v);
							kurikulumPunyaMatakuliah.setRubrikPenilaian(v);
						}
						if (pemetaanSoalUtsField != null && o.has("pemetaanSoalUts")) {
							String v = o.optString("pemetaanSoalUts", "");
							pemetaanSoalUtsField.setValue(v);
							kurikulumPunyaMatakuliah.setPemetaanSoalUts(v);
						}
						if (pemetaanSoalUasField != null && o.has("pemetaanSoalUas")) {
							String v = o.optString("pemetaanSoalUas", "");
							pemetaanSoalUasField.setValue(v);
							kurikulumPunyaMatakuliah.setPemetaanSoalUas(v);
						}
						Common.refreshUpdate(kurikulumPunyaMatakuliah);
						MyMessageboxConfig.show(
								Common.getBahasaConfig("Catatan & Data OBE berhasil diisi via AI. Silakan tinjau."),
								Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
					}
				});
	}

	private static String potongJsonObj(String s) {
		if (s == null) {
			return null;
		}
		int a = s.indexOf('{');
		int b = s.lastIndexOf('}');
		if (a >= 0 && b > a) {
			return s.substring(a, b + 1);
		}
		return null;
	}

	private void initCatatan() {
		boolean boleh = bolehMengubahCatatanObe();
		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Catatan"));
		rowsUtama.appendChild(group);

		if (boleh) {
			MyFormRow rowAi = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowAi, "2");
			rowAi.setParent(rowsUtama);
			MyToolbarbuttonConfig btnAiCatatan = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Generate Catatan & Data OBE berdasarkan AI"), "/img/svg/sparkles.svg");
			btnAiCatatan.setStyle("font-size:12px;font-weight:bold;color:#ffffff;background-color:#7c3aed;"
					+ "border-radius:6px;padding:6px 15px;border:none;cursor:pointer;margin:4px 0;");
			btnAiCatatan.setParent(rowAi);
			btnAiCatatan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					generateCatatanObeAi();
				}
			});
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rowsUtama);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		catatan = new MyCkEditor();
		catatan.setValue(kurikulumPunyaMatakuliah.getCatatan());
		catatan.setHeight("400px");
		catatan.setWidth("95%");
		if (boleh) {
			row.appendChild(catatan);
		} else {
			row.appendChild(new MyHtml(kurikulumPunyaMatakuliah.getCatatan()));
		}
		catatan.addEventListener("onChange", eventListener);
	}

	@SuppressWarnings("deprecation")
	private void initObeExtra() {
		boolean boleh = bolehUbahObe && bolehMengubahCatatanObe();
		MyGroupConfig group = new MyGroupConfig(Common.getBahasaConfig("Data Tambahan OBE (Pemetaan CPL & Soal)"));
		rowsUtama.appendChild(group);

		// ── Bobot CPL per MK ────────────────────────────────────────────────────
		cplBobotField = new MyTextbox(kurikulumPunyaMatakuliah.getCplBobot());
		cplBobotField.setRows(2);
		cplBobotField.setWidth("95%");
		cplBobotField.setTooltiptext("Contoh: CPL-2:15,CPL-4:45,CPL-9:40  (kode CPL:persen, pisah koma)");
		cplBobotField.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, "Bobot CPL per MK",
				boleh ? cplBobotField : new Label(kurikulumPunyaMatakuliah.getCplBobot()));

		// ── Komponen Penilaian ───────────────────────────────────────────────────
		komponenPenilaianField = new MyTextbox(kurikulumPunyaMatakuliah.getKomponenPenilaian());
		komponenPenilaianField.setRows(2);
		komponenPenilaianField.setWidth("95%");
		komponenPenilaianField.setTooltiptext("Contoh: Kuis:10,Tugas:10,Keaktifan:10,UTS:30,UAS:40");
		komponenPenilaianField.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, "Komponen Penilaian (%)",
				boleh ? komponenPenilaianField : new Label(kurikulumPunyaMatakuliah.getKomponenPenilaian()));

		// ── Teknik Penilaian per CPMK ────────────────────────────────────────────
		teknikPerCpmkField = new MyTextbox(kurikulumPunyaMatakuliah.getTeknikPerCpmk());
		teknikPerCpmkField.setRows(5);
		teknikPerCpmkField.setWidth("95%");
		teknikPerCpmkField.setTooltiptext(
				"Format: satu baris per CPMK — Kode CPMK:Teknik1,Teknik2\n" +
				"Contoh:\nCPMK-1:Kuis,UTS\nCPMK-2:Tugas,Unjuk Kerja,UAS\nCPMK-3:Partisipasi,UTS,UAS\n" +
				"Teknik umum: Kuis, Tugas, Partisipasi, Observasi, Unjuk Kerja, MBKM, UTS, UAS, Tes Lisan");
		teknikPerCpmkField.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, "Teknik Penilaian per CPMK",
				boleh ? teknikPerCpmkField : new Label(kurikulumPunyaMatakuliah.getTeknikPerCpmk()));

		// ── Rubrik Penilaian ─────────────────────────────────────────────────────
		rubrikPenilaianField = new MyTextbox(kurikulumPunyaMatakuliah.getRubrikPenilaian());
		rubrikPenilaianField.setRows(6);
		rubrikPenilaianField.setWidth("95%");
		rubrikPenilaianField.setTooltiptext(
				"Rubrik penilaian (mis. presentasi/laporan). Format tiap baris:\n" +
				"#Judul Rubrik (bobot%)   -> baris berawalan '#' = judul sub-rubrik\n" +
				"Aspek|Bobot%|Skor 4|Skor 3|Skor 2|Skor 1\n" +
				"Contoh:\n#Rubrik Materi Presentasi (40%)\n" +
				"Kelengkapan Materi|15|Lengkap & mendalam|Lengkap kurang dalam|Kurang lengkap|Tidak lengkap");
		rubrikPenilaianField.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, "Rubrik Penilaian",
				boleh ? rubrikPenilaianField : new Label(kurikulumPunyaMatakuliah.getRubrikPenilaian()));

		// ── Pemetaan Soal UTS ────────────────────────────────────────────────────
		pemetaanSoalUtsField = new MyTextbox(kurikulumPunyaMatakuliah.getPemetaanSoalUts());
		pemetaanSoalUtsField.setRows(6);
		pemetaanSoalUtsField.setWidth("95%");
		pemetaanSoalUtsField.setTooltiptext("Format tiap baris: Sub CPMK 1.1|PG 1  /  Sub CPMK 2.1|PG 8,12,17; Esai 4,5");
		pemetaanSoalUtsField.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, "Pemetaan Soal UTS",
				boleh ? pemetaanSoalUtsField : new Label(kurikulumPunyaMatakuliah.getPemetaanSoalUts()));

		// ── Pemetaan Soal UAS ────────────────────────────────────────────────────
		pemetaanSoalUasField = new MyTextbox(kurikulumPunyaMatakuliah.getPemetaanSoalUas());
		pemetaanSoalUasField.setRows(6);
		pemetaanSoalUasField.setWidth("95%");
		pemetaanSoalUasField.setTooltiptext("Format tiap baris: Sub CPMK 2.4|PG 11,12,13  /  Sub CPMK 4.1|PG 14-17; Esai 4,5");
		pemetaanSoalUasField.addEventListener("onChange", eventListener);
		createRowLabelAndValue(rowsUtama, "Pemetaan Soal UAS",
				boleh ? pemetaanSoalUasField : new Label(kurikulumPunyaMatakuliah.getPemetaanSoalUas()));
	}

	/**
	 * Analisis Dosen per CPMK dan Evaluasi Admin — bagian dari tab "Catatan &amp; OBE".
	 * Dosen mengisi analisis ketercapaian CPMK per semester; Admin dapat menambahkan
	 * evaluasi dan catatan tindak lanjut.
	 *
	 * Data disimpan di perkuliahan.cqiData (JSON) menggunakan field:
	 *   masalah, analisis, rencana, pj, targetWaktu, status (isian dosen),
	 *   adminKomentar (isian admin/Kajur).
	 */
	@SuppressWarnings({"unchecked", "deprecation"})
	private void initAnalisisdanEvaluasi() {
		if (perkuliahan == null) return;
		// Tentukan hak akses
		String roleId = (tbmuser == null || tbmuser.hakAkses() == null)
				? "" : safeTrim(tbmuser.hakAkses().getRoleId());
		final boolean isAdmin = "am".equals(roleId) || "adAkdmk".equals(roleId)
				|| "Akademik".equals(roleId) || "admfak".equals(roleId)
				|| "admprd".equals(roleId) || "Kajur".equals(roleId);
		final boolean isDosen = tbmuser != null && tbmuser.ambilDosen() != null;
		final boolean bolehEdit = isDosen || isAdmin;
		final boolean bolehEditAdmin = isAdmin;

		// === Bagian A: Analisis Dosen per CPMK ============================
		MyGroupConfig groupDosen = new MyGroupConfig("Analisis Dosen per CPMK (CQI)");
		rowsUtama.appendChild(groupDosen);

		MyFormRow infoRow = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(infoRow, "2");
		infoRow.setParent(rowsUtama);
		infoRow.appendChild(new org.zkoss.zul.Html(
			"<div style='font-size:12px;padding:10px 14px;background:#fff7ed;border-radius:10px;"
			+ "border:1px solid #fed7aa;color:#78350f;margin-bottom:6px;'>"
			+ "<b>CQI (Continuous Quality Improvement)</b> — Loop 1 per Semester.<br>"
			+ "Dosen mengisi identifikasi masalah ketercapaian CPMK, analisis penyebab, dan rencana tindak lanjut "
			+ "untuk dijadikan dasar perbaikan semester berikutnya. "
			+ (isAdmin ? "<br><span style='color:#1e40af;font-weight:700;'>&#128272; Anda login sebagai Admin — "
				+ "kolom Evaluasi Admin dapat diedit.</span>" : "")
			+ "</div>"));

		// Load CPMK list dari perkuliahan ini
		final List<CapaianPembelajaranLulusan> cpmkList =
				new ArrayList<CapaianPembelajaranLulusan>();
		if (perkuliahan.getMatakuliah() != null) {
			Session cpmkSess = null;
			try {
				cpmkSess = HibernateUtil.openSession();
				String cpmkCsv = perkuliahan.getMatakuliah().getCapaianPembelajaranLulusan();
				if (cpmkCsv != null && !cpmkCsv.trim().isEmpty()) {
					Set<Long> ids = new HashSet<Long>();
					for (String s : cpmkCsv.split(",")) {
						if (s == null || s.trim().length() == 0) {
							continue;
						}
						try {
							ids.add(Long.parseLong(s.trim()));
						} catch (Exception ex) {
							ais.common.ErrorAuditUtil.record(ex,
									"auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5522");
						}
					}
					if (!ids.isEmpty()) {
						cpmkList.addAll((List<CapaianPembelajaranLulusan>)
							cpmkSess.createCriteria(CapaianPembelajaranLulusan.class)
								.add(Restrictions.in("id", ids))
								.addOrder(Order.asc("kode")).list());
					}
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5531");
				/* ignore */
			} finally {
				if (cpmkSess != null) try { cpmkSess.close(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5534");}
			}
		}

		// Parse existing CQI JSON
		final Map<String, JSONObject> cqiMap =
				new java.util.LinkedHashMap<String, JSONObject>();
		String cqiRaw = perkuliahan.getCqiData();
		if (cqiRaw != null && !cqiRaw.trim().isEmpty()) {
			try {
				JSONArray arr = new JSONArray(cqiRaw);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject e = arr.optJSONObject(i);
					if (e != null && !e.optString("cpmk","").isEmpty())
						cqiMap.put(e.optString("cpmk",""), e);
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5550");}
		}

		if (cpmkList.isEmpty()) {
			MyFormRow emptyRow = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(emptyRow, "2");
			emptyRow.setParent(rowsUtama);
			emptyRow.appendChild(new org.zkoss.zul.Html(
				"<div style='color:#94a3b8;padding:10px 14px;font-size:12px;'>"
				+ "&#9432; CPMK belum didefinisikan untuk mata kuliah ini. "
				+ "Silakan isi CPMK di tab <b>CPMK &amp; Sub-CPMK</b> terlebih dahulu.</div>"));
		} else {
			// Grid per CPMK dengan field: masalah, analisis, rencana, pj, targetWaktu, status, adminKomentar
			MyFormRow cqiRow = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(cqiRow, "2");
			cqiRow.setValign("top");
			cqiRow.setParent(rowsUtama);

			MyGrid cqiGrid = new MyGrid();
			cqiGrid.setWidth("100%");
			cqiGrid.setStyle("border:1px solid #e5e7eb;border-radius:12px;font-size:11px;");
			cqiGrid.setParent(cqiRow);

			Columns cqiCols = new Columns();
			cqiCols.setParent(cqiGrid);
			String[][] colDefs = {
				{"CPMK","10%"}, {"Masalah / Gap Ketercapaian",null}, {"Analisis Penyebab",null},
				{"Rencana Tindak Lanjut",null}, {"PJ","9%"}, {"Target Waktu","10%"},
				{"Status","8%"}, {"Evaluasi Admin","13%"}
			};
			for (String[] cd : colDefs) {
				MyColumnConfig cqiCol = new MyColumnConfig(cd[0]);
				if (cd[1] != null) cqiCol.setWidth(cd[1]);
				cqiCol.setParent(cqiCols);
			}

			final Rows cqiRows = new Rows();
			cqiRows.setParent(cqiGrid);

			final List<String> cpmkKodes = new ArrayList<String>();
			final List<MyTextbox> masalahList  = new ArrayList<MyTextbox>();
			final List<MyTextbox> analisisList = new ArrayList<MyTextbox>();
			final List<MyTextbox> rencanaList  = new ArrayList<MyTextbox>();
			final List<MyTextbox> pjList       = new ArrayList<MyTextbox>();
			final List<MyTextbox> targetList   = new ArrayList<MyTextbox>();
			final List<MyTextbox> statusList   = new ArrayList<MyTextbox>();
			final List<MyTextbox> adminKomList = new ArrayList<MyTextbox>();

			for (CapaianPembelajaranLulusan cpmk : cpmkList) {
				String kode = safeTrim(cpmk.getKode());
				JSONObject entry = cqiMap.containsKey(kode)
						? cqiMap.get(kode) : new JSONObject();
				cpmkKodes.add(kode);

				Row cqiCellRow = new Row();
				cqiCellRow.setValign("top");
				cqiCellRow.setParent(cqiRows);

				Vbox vb = new Vbox();
				Label lKode = new Label(kode);
				lKode.setStyle("font-weight:bold;color:#7c3aed;");
				lKode.setParent(vb);
				Label lNama = new Label(safeTrim(cpmk.getNama()));
				lNama.setStyle("font-size:10px;color:#64748b;");
				lNama.setParent(vb);
				vb.setParent(cqiCellRow);

				MyTextbox tbMasalah = new MyTextbox(entry.optString("masalah",""));
				tbMasalah.setRows(2); tbMasalah.setWidth("95%"); tbMasalah.setDisabled(!bolehEdit);
				tbMasalah.setTooltiptext("Deskripsikan masalah/gap ketercapaian CPMK ini");
				masalahList.add(tbMasalah); tbMasalah.setParent(cqiCellRow);

				MyTextbox tbAnalisis = new MyTextbox(entry.optString("analisis",""));
				tbAnalisis.setRows(2); tbAnalisis.setWidth("95%"); tbAnalisis.setDisabled(!bolehEdit);
				tbAnalisis.setTooltiptext("Analisis akar penyebab masalah ketercapaian");
				analisisList.add(tbAnalisis); tbAnalisis.setParent(cqiCellRow);

				MyTextbox tbRencana = new MyTextbox(entry.optString("rencana",""));
				tbRencana.setRows(2); tbRencana.setWidth("95%"); tbRencana.setDisabled(!bolehEdit);
				tbRencana.setTooltiptext("Rencana tindak lanjut perbaikan semester berikutnya");
				rencanaList.add(tbRencana); tbRencana.setParent(cqiCellRow);

				MyTextbox tbPj = new MyTextbox(entry.optString("pj",""));
				tbPj.setWidth("95%"); tbPj.setDisabled(!bolehEdit);
				tbPj.setTooltiptext("Penanggung jawab tindak lanjut");
				pjList.add(tbPj); tbPj.setParent(cqiCellRow);

				MyTextbox tbTarget = new MyTextbox(entry.optString("targetWaktu",""));
				tbTarget.setWidth("95%"); tbTarget.setDisabled(!bolehEdit);
				tbTarget.setTooltiptext("Target waktu — mis. Semester Genap 2025/2026");
				targetList.add(tbTarget); tbTarget.setParent(cqiCellRow);

				MyTextbox tbStatus = new MyTextbox(entry.optString("status","Planned"));
				tbStatus.setWidth("95%"); tbStatus.setDisabled(!bolehEdit);
				tbStatus.setTooltiptext("Status: Planned / In Progress / Completed / Ditunda");
				statusList.add(tbStatus); tbStatus.setParent(cqiCellRow);

				// Admin evaluation column
				String adminKomVal = entry.optString("adminKomentar","");
				if (bolehEditAdmin) {
					MyTextbox tbAdminKom = new MyTextbox(adminKomVal);
					tbAdminKom.setRows(2); tbAdminKom.setWidth("95%");
					tbAdminKom.setTooltiptext("Evaluasi/rekomendasi admin atas analisis dosen untuk CPMK ini");
					adminKomList.add(tbAdminKom); tbAdminKom.setParent(cqiCellRow);
				} else {
					adminKomList.add(null);
					Label lblAdmin = new Label(adminKomVal.isEmpty() ? "-" : adminKomVal);
					lblAdmin.setStyle("font-size:10px;color:#1e40af;font-style:italic;padding:4px;");
					cqiCellRow.appendChild(lblAdmin);
				}
			}

			// Tombol simpan CQI
			MyFormRow btnRow = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(btnRow, "2");
			btnRow.setParent(rowsUtama);
			Toolbar cqiToolbar = new Toolbar();
			cqiToolbar.setStyle("background:transparent;border:none;");
			cqiToolbar.setParent(btnRow);

			if (bolehEdit) {
				final Map<String, JSONObject> cqiMapFinal = cqiMap;

				// Generate CQI via AI: isi Masalah/Gap, Analisis Penyebab, Rencana Tindak Lanjut tiap CPMK.
				MyToolbarbuttonConfig btnGenCqi = new MyToolbarbuttonConfig(
						Common.getBahasaConfig("Generate CQI via AI"), "/img/svg/sparkles.svg");
				btnGenCqi.setStyle("font-weight:bold;background:#7c3aed;color:#fff;border-radius:6px;"
						+ "padding:5px 14px;border:0;margin-right:6px;");
				btnGenCqi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						String namaMk = "";
						try {
							if (perkuliahan != null && perkuliahan.getMatakuliah() != null
									&& perkuliahan.getMatakuliah().getNama() != null) {
								namaMk = perkuliahan.getMatakuliah().getNama();
							}
						} catch (Exception e) {
						}
						StringBuilder p = new StringBuilder();
						p.append("Untuk mata kuliah \"").append(namaMk)
								.append("\", buat analisis CQI (Continuous Quality Improvement) per CPMK. ");
						p.append("Untuk TIAP CPMK isi: masalah/gap ketercapaian yang umum terjadi, analisis ");
						p.append("penyebab (akar masalah), dan rencana tindak lanjut perbaikan. ");
						p.append("Bahasa Indonesia akademis, ringkas.\n\nDaftar CPMK:\n");
						for (int i = 0; i < cpmkKodes.size(); i++) {
							String nm = i < cpmkList.size() ? safeTrim(cpmkList.get(i).getNama()) : "";
							p.append(cpmkKodes.get(i)).append(": ").append(nm).append("\n");
						}
						p.append("\nKeluarkan HANYA JSON array valid (satu objek per CPMK sesuai kode), format:\n");
						p.append("[{\"cpmk\":\"<kode>\",\"masalah\":\"...\",\"analisis\":\"...\",\"rencana\":\"...\"}]\n");
						final String prompt = p.toString();

						jalankanAiStreaming(Common.getBahasaConfig("Generate CQI via AI"), prompt,
								new HasilAiListener() {
									@Override
									public void selesai(String resp) throws Exception {
										int a = resp.indexOf('[');
										int b = resp.lastIndexOf(']');
										if (a < 0 || b <= a) {
											return;
										}
										JSONArray arr = new JSONArray(resp.substring(a, b + 1));
										for (int i = 0; i < arr.length(); i++) {
											JSONObject o = arr.optJSONObject(i);
											if (o == null) {
												continue;
											}
											String kode = o.optString("cpmk", "").trim();
											int idx = cpmkKodes.indexOf(kode);
											if (idx < 0 && i < cpmkKodes.size()) {
												idx = i;
											}
											if (idx < 0 || idx >= cpmkKodes.size()) {
												continue;
											}
											if (masalahList.get(idx) != null) {
												masalahList.get(idx).setValue(o.optString("masalah", ""));
											}
											if (analisisList.get(idx) != null) {
												analisisList.get(idx).setValue(o.optString("analisis", ""));
											}
											if (rencanaList.get(idx) != null) {
												rencanaList.get(idx).setValue(o.optString("rencana", ""));
											}
										}
										MyMessageboxConfig.show(
												Common.getBahasaConfig(
														"Analisis CQI terisi via AI. Tinjau lalu klik Simpan CQI."),
												Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
												MyMessageboxConfig.INFORMATION);
									}
								});
					}
				});
				btnGenCqi.setParent(cqiToolbar);

				MyToolbarbuttonConfig btnSimpanCqi = new MyToolbarbuttonConfig(
						"Simpan CQI", "/img/save.gif");
				btnSimpanCqi.setStyle("font-weight:bold;background:#c2410c;color:#fff;"
						+ "border-radius:6px;padding:5px 14px;border:0;");
				btnSimpanCqi.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						JSONArray result = new JSONArray();
						for (int i = 0; i < cpmkKodes.size(); i++) {
							JSONObject en = new JSONObject();
							en.put("cpmk",       cpmkKodes.get(i));
							en.put("masalah",    masalahList.get(i).getValue().trim());
							en.put("analisis",   analisisList.get(i).getValue().trim());
							en.put("rencana",    rencanaList.get(i).getValue().trim());
							en.put("pj",         pjList.get(i).getValue().trim());
							en.put("targetWaktu",targetList.get(i).getValue().trim());
							String statusVal = statusList.get(i).getValue().trim();
							en.put("status", statusVal.isEmpty() ? "Planned" : statusVal);
							// Preserve or update adminKomentar
							String prevAdmin = cqiMapFinal.containsKey(cpmkKodes.get(i))
									? cqiMapFinal.get(cpmkKodes.get(i)).optString("adminKomentar","")
									: "";
							en.put("adminKomentar",
									bolehEditAdmin && adminKomList.get(i) != null
									? adminKomList.get(i).getValue().trim()
									: prevAdmin);
							result.put(en);
						}
						Session saveSess = null;
						try {
							saveSess = HibernateUtil.openSession();
							Perkuliahan perkToSave = (Perkuliahan) saveSess.get(
									Perkuliahan.class, perkuliahan.getId());
							if (perkToSave != null) {
								perkToSave.setCqiData(result.toString());
								saveSess.update(perkToSave);
								saveSess.flush();
								perkuliahan.setCqiData(result.toString());
							}
						} finally {
							if (saveSess != null) {
								try { saveSess.flush(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5713");}
								try { saveSess.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5714");}
							}
						}
						MyMessageboxConfig.show("CQI berhasil disimpan.",
								"Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				});
				btnSimpanCqi.setParent(cqiToolbar);
			}
		}
	}

	/**
	 * Membuat satu Tab + Tabpanel berisi grid 2-kolom (label/value) di dalam {@link #tabboxObe},
	 * lalu mengembalikan {@link Rows}-nya. Pemanggil men-set {@code this.rowsUtama = hasil} SEBELUM
	 * memanggil method init section terkait, sehingga seluruh section RPS OBE — yang sebelumnya
	 * menumpuk pada satu halaman panjang — kini dibangun ke dalam tab tersendiri TANPA mengubah
	 * method init mana pun (init tetap menulis ke {@code this.rowsUtama}).
	 */
	private Rows buatTabRowsObe(org.zkoss.zul.Tabs tabs, org.zkoss.zul.Tabpanels tabpanels, String judul) {
		ais.ui.util.MyTabConfig tab = new ais.ui.util.MyTabConfig(judul);
		tab.setParent(tabs);
		org.zkoss.zul.Tabpanel panel = new org.zkoss.zul.Tabpanel();
		panel.setParent(tabpanels);
		MyGrid g = new MyGrid();
		g.setSclass("fgrid");
		g.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(g);
		MyColumnConfig c1 = new MyColumnConfig();
		c1.setWidth("20%");
		c1.setParent(columns);
		MyColumnConfig c2 = new MyColumnConfig();
		c2.setParent(columns);
		Rows r = new Rows();
		r.setParent(g);
		g.setParent(panel);
		return r;
	}

	public void onSearchDefault(Event event) throws Exception {
		// Tangkap anchor asli (grid + kartu data dari zul) SEKALI, sebelum rowsUtama dialihkan ke tab.
		if (gridAsliObe == null && rowsUtama != null) {
			org.zkoss.zk.ui.Component induk = rowsUtama.getParent();
			if (induk instanceof org.zkoss.zul.Grid) {
				gridAsliObe = (org.zkoss.zul.Grid) induk;
				anchorObe = gridAsliObe.getParent();
			}
		}

		// Bersihkan tabbox lama + INGAT tab aktif agar setelah simpan/refresh user tetap di tab yang sama.
		int tabAktif = -1;
		if (tabboxObe != null) {
			try { tabAktif = tabboxObe.getSelectedIndex(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5767");}
			try { tabboxObe.detach(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5768");}
			tabboxObe = null;
		}
		if (gridAsliObe != null) {
			try { Common.clear(gridAsliObe.getRows()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5772");}
			gridAsliObe.setVisible(false);
		} else {
			Common.clear(rowsUtama);
		}

		matakuliah = null;
		kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) (searchKurikulum.getSelectedItem() == null ? null
				: searchKurikulum.getSelectedItem().getValue());

		if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getMatakuliah() != null
				&& kurikulumPunyaMatakuliah.getKurikulum() != null) {
			initHakAkses();
			matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

			// === Bangun TABBOX: form RPS OBE dipecah PER-TAB supaya tidak perlu scroll panjang. ===
			// Tiap tab punya grid 2-kolom sendiri; this.rowsUtama diarahkan ke tab terkait sebelum
			// memanggil init section. Saat onSearchDefault dipanggil ulang (mis. setelah simpan/refresh),
			// tabbox dibangun ulang lalu TAB AKTIF dipulihkan -> efektif user tetap berada di tab itu.
			org.zkoss.zk.ui.Component induk = (anchorObe != null) ? anchorObe
					: (gridAsliObe != null ? gridAsliObe.getParent() : rowsUtama.getParent());
			tabboxObe = new org.zkoss.zul.Tabbox();
			tabboxObe.setWidth("100%");
			tabboxObe.setParent(induk);
			org.zkoss.zul.Tabs tabs = new org.zkoss.zul.Tabs();
			tabs.setParent(tabboxObe);
			org.zkoss.zul.Tabpanels tabpanels = new org.zkoss.zul.Tabpanels();
			tabpanels.setParent(tabboxObe);

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("Mata Kuliah"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_MK_OBE);
			initMk();

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("Otoritas"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_OTORITAS_OBE);
			initOtoritas();

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("Profil Lulusan"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_PL_OBE);
			initPl();

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("CPL"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_CPL_OBE);
			initCpl();

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("CPMK & Sub-CPMK"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_CPMK_OBE);
			initCpmk();
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_SUB_CPMK_OBE);
			initSubCpmk();
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_SUB_CPMK_KORELASI_OBE);
			initSubCpmkKorelasi();
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_CPL_CPMK_OBE);
			initCplCpmk();

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("Deskripsi & Pustaka"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_DESKRIPSI_OBE);
			initDeskripsi();

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("Rincian / Agenda"));
			gunakanHakAksesFiturObe(CFG_HAK_AKSES_UBAH_RINCI_OBE);
			initRinci(event != null);

			rowsUtama = buatTabRowsObe(tabs, tabpanels, Common.getBahasaConfig("Catatan & OBE"));
			bolehUbahObe = bolehMengubahCatatanObe();
			initCatatan();
			initObeExtra();
			initAnalisisdanEvaluasi();

			// Bila dibuka sbg popup dari Penilaian OBE dgn permintaan tab tertentu (field diisi di
			// doAfterCompose dari arg "tabAktif", mis. "CPMK & Sub-CPMK"), pilih tab itu SEKALI agar
			// langsung fokus ke halaman diminta. Pakai FIELD (bukan getArg di sini) karena init* di atas
			// bisa memanggil createComponents lain yg menggeser arg-stack. Dikosongkan setelah dipakai
			// agar refresh/aksi berikutnya tetap mempertahankan tab yg sedang dibuka pengguna.
			if (tabAktifLabelDiminta != null) {
				try {
					String labelCari = Common.getBahasaConfig(tabAktifLabelDiminta);
					for (int it = 0; it < tabs.getChildren().size(); it++) {
						org.zkoss.zul.Tab tt = (org.zkoss.zul.Tab) tabs.getChildren().get(it);
						if (tt != null && tt.getLabel() != null && tt.getLabel().equalsIgnoreCase(labelCari)) {
							tabAktif = it;
							break;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5856");}
				tabAktifLabelDiminta = null;
			}

			// Pulihkan tab aktif (jika sebelumnya ada) agar refresh tidak melompat ke tab pertama.
			if (tabAktif >= 0 && tabAktif < tabs.getChildren().size()) {
				try { tabboxObe.setSelectedIndex(tabAktif); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/RpsObeAction.java:5862");}
			}
		} else if (gridAsliObe != null) {
			// Tidak ada MK terpilih -> tampakkan kembali grid asli (kosong) agar layout tidak rusak.
			gridAsliObe.setVisible(true);
		}
	}

}
