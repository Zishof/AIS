package ais.database.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataSemuaMahasiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kelas dasar legacy untuk entity yang mendukung parameter tambahan dinamis. Ejaan {@code Astract}
 * dipertahankan demi kompatibilitas; tipe ini menjadi satu sumber kontrak penyimpanan dan
 * pembacaan nilai tambahan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String TIDAK_ADA}, {@code String TEXT},
 * {@code String ANGKA}, {@code String TEXT_ANGKA}, {@code String TANGGAL}, {@code String TANGGAL_DAN_WAKTU},
 * {@code String WAKTU}, {@code String PILIHAN_YA_TIDAK}; inisialisasi/lifecycle ({@code initComponent()}, {@code
 * initComponent()}, {@code initComponent()}); pembacaan/pencarian ({@code tampil()}, {@code
 * ambilNilaiComponent()}, {@code ambilComponent()}, {@code ambilComponent()}, {@code ambilComponentCustom()},
 * {@code ambilVal()}); mutasi data ({@code parseTanggalAman()}); operasi domain lain ({@code nzp()}, {@code
 * rangkaiAlamatPenyedia()}, {@code rangkaiJenisPekerjaanPenyedia()}, {@code nilaiPenyediaUntukLabel()}, {@code
 * isiOtomatisParameterTerkaitPenyedia()}, {@code reevaluasiSkipLogic()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
public abstract class ParameterTambahanAstract extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String TIDAK_ADA = "Tidak ada data yang diinput";
	public static final String TEXT = "Berupa teks";
	public static final String ANGKA = "Berupa numerik / angka";
	public static final String TEXT_ANGKA = "Berupa teks / angka";
	public static final String TANGGAL = "Berupa tanggal";
	public static final String TANGGAL_DAN_WAKTU = "Berupa tanggal dan waktu";
	public static final String WAKTU = "Berupa waktu";
	public static final String PILIHAN_YA_TIDAK = "Berupa pilihan ya/tidak";
	public static final String PILIHAN_CUSTOM = "Berupa pilihan custom";
	public static final String PILIHAN_OBJECT = "Berupa pilihan data";
	public static final String PILIHAN_BANYAK = "Berupa banyak pilihan";
	public static final String PILIHAN_MATRIX = "Berupa pilihan matrix";
	public static final String PILIHAN_MATRIX_BANYAK_NILAI = "Berupa pilihan matrix banyak nilai";
	public static final String PILIHAN_MATRIX_BANYAK_COMBO = "Berupa pilihan matrix salah satu nilai";

	public static final String PILIHAN_MAHASISWA = "Berupa data mahasiswa";
	public static final String PILIHAN_SISWA = "Berupa data siswa";
	public static final String PILIHAN_DOSEN = "Berupa data dosen";
	public static final String PILIHAN_GURU = "Berupa data guru";
	public static final String PILIHAN_PEGAWAI = "Berupa data pegawai";
	public static final String PILIHAN_PENYEDIA = "Berupa data penyedia";
	public static final String PILIHAN_KELAS_SISWA = "Berupa data kelas siswa";

	public static final List<String> CUSTOM_PILIHAN = new ArrayList<String>();
	static {
		CUSTOM_PILIHAN.add(PILIHAN_MAHASISWA);
		CUSTOM_PILIHAN.add(PILIHAN_SISWA);
		CUSTOM_PILIHAN.add(PILIHAN_DOSEN);
		CUSTOM_PILIHAN.add(PILIHAN_GURU);
		CUSTOM_PILIHAN.add(PILIHAN_PEGAWAI);
		CUSTOM_PILIHAN.add(PILIHAN_PENYEDIA);
		CUSTOM_PILIHAN.add(PILIHAN_KELAS_SISWA);
	}

	public static void tampil(Vbox vbox2, ParameterTambahan parameterTambahan, final LampiranLain lampiranLain,
			String vall) {

		if (lampiranLain != null) {
			String u = lampiranLain.getUrl();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(vall.trim().isEmpty() ? u : vall,
					"/img/svg/download.svg");
			button.setTooltiptext("Download " + (vall.trim().isEmpty() ? u : vall));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (lampiranLain.getGdrive() != null && !lampiranLain.getGdrive().isEmpty()) {
						ExecutionsCtrl.getCurrent().sendRedirect(lampiranLain.downloadGDriveUrl(), "_blank");
					} else {
						Common.display(lampiranLain);
					}
				}
			});
			button.setParent(vbox2);
		} else {
			if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PENYEDIA)
					&& vall != null && vall.contains("->")) {
				String[] bagian = vall.split("->", 2);
				vbox2.appendChild(new MyLabelKecil(bagian.length > 1 ? bagian[1] : vall));
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
				try {
					Double nilai = vall.trim().isEmpty() ? 0.0 : Double.parseDouble(vall);
					vbox2.appendChild(new MyLabelKecil(Common.numberFormat.get().format(nilai)));
				} catch (Exception e) {
					vbox2.appendChild(new MyLabelKecil(vall));
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL)) {
				try {
					Date nilai = vall.trim().isEmpty() ? null : Common.dateFormat1.get().parse(vall);
					vbox2.appendChild(new MyLabelKecil(Common.dateFormat6.get().format(nilai)));
				} catch (Exception e) {
					vbox2.appendChild(new MyLabelKecil(vall));
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL_DAN_WAKTU)) {
				try {
					Date nilai = vall.trim().isEmpty() ? null : Common.dateFormat.get().parse(vall);
					vbox2.appendChild(new MyLabelKecil(Common.dateFormat61.get().format(nilai)));
				} catch (Exception e) {
					vbox2.appendChild(new MyLabelKecil(vall));
				}
			} else {
				vbox2.appendChild(new MyLabelKecil(vall));
			}
		}
	}

	public static boolean initComponent(Row row, Rows rows, String jenis, List<Row> parameterRows,
			final Map<String, LampiranLain> lampiranLains, Long ref, String val, String ket,
			ParameterTambahan parameterTambahan, EventListener eventListener) {
		return initComponent(row, rows, jenis, parameterRows, lampiranLains, ref, val, ket, parameterTambahan,
				eventListener, lampiranLains == null);
	}

	public static boolean initComponent(Row row, Rows rows, String jenis, List<Row> parameterRows,
			final Map<String, LampiranLain> lampiranLains, Long ref, String val, String ket,
			ParameterTambahan parameterTambahan, EventListener eventListener, boolean readonly) {
		String componenName = "component";
		return initComponent(row, rows, jenis, parameterRows, lampiranLains, ref, val, ket, parameterTambahan,
				eventListener, readonly, componenName);
	}

	/** null-safe trim untuk perakit teks penyedia. */
	private static String nzp(String s) {
		return s == null ? "" : s.trim();
	}

	/** Rangkai alamat lengkap penyedia (alamat, kec/kota/prop, kode pos, telp, fax, kontak, email). */
	private static String rangkaiAlamatPenyedia(ais.database.model.asset.PenyediaAsset p) {
		String alamat = nzp(p.getAlamat());
		if (p.getKecamatan() != null) {
			String c = "Kec." + p.getKecamatan().getNama();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (p.getKota() != null) {
			String c = "Kab/Kota." + p.getKota().getNama();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (p.getPropinsi() != null) {
			String c = "Prop." + p.getPropinsi().getNama();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getKodePos()).isEmpty()) {
			String c = "Kode Pos " + p.getKodePos();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getTelp()).isEmpty()) {
			String c = "Telp. " + p.getTelp();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getFax()).isEmpty()) {
			String c = "Fax. " + p.getFax();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getKontak()).isEmpty()) {
			String c = "Kontak. " + p.getKontak();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		if (!nzp(p.getEmail()).isEmpty()) {
			String c = "Email. " + p.getEmail();
			alamat += alamat.isEmpty() ? c : ", " + c;
		}
		return alamat;
	}

	/** Rangkai daftar jenis pekerjaan/barang-jasa penyedia (slot 1-5, dipisah koma). */
	private static String rangkaiJenisPekerjaanPenyedia(ais.database.model.asset.PenyediaAsset p) {
		String jenis = "";
		ais.database.model.asset.JenisPekerjaanPenyedia[] slots = new ais.database.model.asset.JenisPekerjaanPenyedia[] {
				p.getJenisPekerjaanPenyedia1(), p.getJenisPekerjaanPenyedia2(), p.getJenisPekerjaanPenyedia3(),
				p.getJenisPekerjaanPenyedia4(), p.getJenisPekerjaanPenyedia5() };
		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null) {
				jenis += jenis.isEmpty() ? slots[i].getNama() : ", " + slots[i].getNama();
			}
		}
		if (jenis.isEmpty() && p.getJenisPenyediaAsset() != null) {
			jenis = p.getJenisPenyediaAsset().getNama();
		}
		return jenis;
	}

	/** Tentukan nilai isian otomatis dari penyedia berdasarkan kata kunci pada label parameter. */
	private static String nilaiPenyediaUntukLabel(String labelLower, ais.database.model.asset.PenyediaAsset p) {
		if (labelLower.contains("alamat")) {
			return rangkaiAlamatPenyedia(p);
		}
		if (labelLower.contains("jenis") || labelLower.contains("barang") || labelLower.contains("jasa")
				|| labelLower.contains("pekerjaan")) {
			return rangkaiJenisPekerjaanPenyedia(p);
		}
		if (labelLower.contains("pic") || labelLower.contains("penanggung jawab")
				|| labelLower.contains("contact person") || labelLower.contains("narahubung")) {
			String pic = nzp(p.getKontak());
			return pic.isEmpty() ? nzp(p.getPemilik()) : pic;
		}
		if (labelLower.contains("email") || labelLower.contains("e-mail")) {
			return nzp(p.getEmail());
		}
		if (labelLower.contains("telp") || labelLower.contains("telepon") || labelLower.contains("hp")) {
			return nzp(p.getTelp());
		}
		if (labelLower.contains("fax")) {
			return nzp(p.getFax());
		}
		if (labelLower.contains("npwp")) {
			return nzp(p.getNpwp());
		}
		if (labelLower.contains("kontak")) {
			return nzp(p.getKontak());
		}
		return null; // label tidak dikenali -> jangan diisi
	}

	/**
	 * KORELASI ANTAR-PARAMETER: setelah pengguna memilih Penyedia (vendor) pada parameter bertipe
	 * {@link #PILIHAN_PENYEDIA}, isi otomatis parameter teks lain yang SE-KONTEKS. Konteks diambil dari
	 * label parameter vendor dengan membuang kata pengantar umum di depannya ("Nama Vendor I" →
	 * konteks "vendor i"); parameter lain dianggap se-konteks bila label-nya BERAKHIRAN konteks itu
	 * (endsWith, agar "Vendor I" tidak menular ke "Vendor II"). Nilai isian ditentukan dari kata kunci
	 * label (alamat / jenis barang-jasa / pic / telp / email / fax / npwp / kontak).
	 */
	public static void isiOtomatisParameterTerkaitPenyedia(ParameterTambahan ptVendor, Component komponenVendor,
			List<Row> parameterRows) {
		try {
			if (ptVendor == null || komponenVendor == null || parameterRows == null) {
				return;
			}
			Object data = komponenVendor.getAttribute("penyediaAsset");
			if (!(data instanceof ais.database.model.asset.PenyediaAsset)) {
				return;
			}
			ais.database.model.asset.PenyediaAsset penyedia = (ais.database.model.asset.PenyediaAsset) data;

			String konteks = nzp(ptVendor.getLabelInputan()).toLowerCase();
			String[] prefixBuang = new String[] { "nama", "pilih", "pilihan", "data" };
			boolean berubah = true;
			while (berubah) {
				berubah = false;
				for (int i = 0; i < prefixBuang.length; i++) {
					if (konteks.startsWith(prefixBuang[i] + " ")) {
						konteks = konteks.substring(prefixBuang[i].length() + 1).trim();
						berubah = true;
					}
				}
			}
			if (konteks.isEmpty()) {
				return;
			}

			for (Row r : parameterRows) {
				try {
					if (r == null) {
						continue;
					}
					Object ptO = r.getAttribute("parameterTambahan");
					Object cO = r.getAttribute("component");
					if (!(ptO instanceof ParameterTambahan) || !(cO instanceof Component) || cO == komponenVendor) {
						continue;
					}
					ParameterTambahan pt = (ParameterTambahan) ptO;
					String label = nzp(pt.getLabelInputan()).toLowerCase();
					if (label.isEmpty() || !label.endsWith(konteks)) {
						continue;
					}
					String nilai = nilaiPenyediaUntukLabel(label, penyedia);
					if (nilai == null) {
						continue;
					}
					Component c = (Component) cO;
					if (c instanceof org.zkoss.zul.impl.InputElement) {
						((org.zkoss.zul.impl.InputElement) c).setText(nilai);
						org.zkoss.zk.ui.event.Events.postEvent("onChange", c, null);
					}
				} catch (Throwable tRow) {
					ais.common.ErrorAuditUtil.record(tRow,
							"auto-audit korelasi-vendor per-baris ParameterTambahanAstract");
				}
			}
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "auto-audit isiOtomatisParameterTerkaitPenyedia");
		}
	}

	public static boolean initComponent(Row row, Rows rows, final String jenis, List<Row> parameterRows,
			final Map<String, LampiranLain> lampiranLains, Long ref, String val, String ket,
			ParameterTambahan parameterTambahan, EventListener eventListener, boolean readonly, String componenName) {

		if (parameterTambahan != null && (val == null || val.trim().isEmpty())) {
			val = parameterTambahan.getNilaiDefault();
		}

		Component component = ParameterTambahanAstract.ambilComponent(val, parameterTambahan, eventListener, readonly);

		boolean tampil = false;
		if (component != null) {
			row.appendChild(component);
			row.setValign("top");
			row.setAttribute(componenName, component);
			// SKIP-LOGIC (syaratTampil): daftarkan nilai komponen & pasang pemicu live (defensif, no-op bila gagal).
			try {
				row.setAttribute("stParamId", parameterTambahan.getId());
				row.setAttribute("stComponent", component);
				String stJsonSyarat = parameterTambahan.getSyaratTampil();
				if (stJsonSyarat != null && !stJsonSyarat.trim().isEmpty()) {
					row.setAttribute("stSyaratParam", parameterTambahan);
				}
				final java.util.List<Row> prSkip = parameterRows;
				if (prSkip != null) {
					EventListener reEvalSkip = new EventListener() {
						@Override
						public void onEvent(Event evSkip) throws Exception {
							reevaluasiSkipLogic(prSkip);
						}
					};
					component.addEventListener("onChange", reEvalSkip);
					component.addEventListener("onSelect", reEvalSkip);
				}
			} catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:211");
			}
			// KORELASI ANTAR-PARAMETER (vendor/penyedia): saat sebuah parameter bertipe
			// PILIHAN_PENYEDIA dipilih (mis. "Nama Vendor I"), parameter TEKS lain yang
			// se-konteks (label berakhiran sama, mis. "Alamat / Kontak Vendor I",
			// "Jenis Barang/Jasa Vendor I", "PIC Vendor I", "Telp Vendor I", "Email Vendor I")
			// otomatis terisi dari data PenyediaAsset terpilih.
			try {
				if (parameterTambahan != null && parameterRows != null && !readonly
						&& ParameterTambahanAstract.PILIHAN_PENYEDIA.equals(parameterTambahan.getTipeDataInputan())
						&& component instanceof ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox) {
					final ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox banboxPenyedia =
							(ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox) component;
					final ParameterTambahan ptVendor = parameterTambahan;
					final java.util.List<Row> prVendor = parameterRows;
					final EventListener listenerLama = banboxPenyedia.getEventListener();
					banboxPenyedia.setEventListener(new EventListener() {
						@Override
						public void onEvent(Event evVendor) throws Exception {
							isiOtomatisParameterTerkaitPenyedia(ptVendor, banboxPenyedia, prVendor);
							if (listenerLama != null) {
								listenerLama.onEvent(evVendor);
							}
						}
					});
				}
			} catch (Throwable tVendor) {
				ais.common.ErrorAuditUtil.record(tVendor, "auto-audit korelasi-vendor ParameterTambahanAstract.initComponent");
			}
			if (parameterTambahan.getTampilkanIsianKeterangan()) {
				MyFormRow rowKeterangan = new MyFormRow();
				if (parameterRows != null)
					parameterRows.add(rowKeterangan);
				rowKeterangan.setStyle("border:0px;background: transparent;");
				rowKeterangan.setParent(rows);
				try { rowKeterangan.setAttribute("stChildOf", parameterTambahan.getId()); } catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:219");}
				rowKeterangan.appendChild(new Label());
				Vbox vbox = new Vbox();
				vbox.setWidth("100%");
				rowKeterangan.appendChild(vbox);
				vbox.appendChild(new Label(parameterTambahan.getLabelInputanKeterangan()));

				if (lampiranLains == null) {
					vbox.appendChild(new Label(ket));
				} else {
					Textbox keterangan = new Textbox(ket);
					keterangan.setWidth("90%");
					keterangan.setRows(2);
					vbox.appendChild(keterangan);
					rowKeterangan.setAttribute("keterangan", keterangan);
					row.setValign("top");
					row.setAttribute("keterangan", keterangan);
				}
			}
			if (parameterTambahan.getHarusMenyertakanLampiran()) {
				MyFormRow rowUpload = new MyFormRow();
				if (parameterRows != null)
					parameterRows.add(rowUpload);
				rowUpload.setStyle("border:0px;background: transparent;");
				rowUpload.setParent(rows);
				try { rowUpload.setAttribute("stChildOf", parameterTambahan.getId()); } catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:244");}
				rowUpload.appendChild(new Label());

				Hbox hbox = new Hbox();
				hbox.setWidth("100%");
				hbox.setStyle("border:0px;background: transparent;");

				if (ref == null) {
					ref = Common.refSementara();
				}
				FileFotoLain fileFotoLain = LampiranLain.ambil(ref, jenis);
				if (lampiranLains == null && (fileFotoLain == null || fileFotoLain.getId() == null)) {
					hbox.appendChild(new MyLabelAgakKecilBoldMerah("Tidak/belum diupload"));
				} else {
					LampiranLain.createDownloadUploadFileLain(hbox, ref, jenis,
							parameterTambahan.getLabelInputan()
									+ (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
							false, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lainAlumni = (LampiranLain) arg0.getData();
									lampiranLains.put(jenis, lainAlumni);
								}
							}, lampiranLains, false, false, false, !readonly, null);
				}
				hbox.setParent(rowUpload);

				if (parameterTambahan.getHanyaTampilDiAdmin()) {
					boolean tidaktampil = !Common.getApakahAdmin(parameterTambahan.getKodeAdminYgBoleh());
					rowUpload.setVisible(!tidaktampil);
					tampil |= !tidaktampil;
				} else {
					tampil |= true;
				}
			}
		}

		if (parameterTambahan.getHanyaTampilDiAdmin()) {
			boolean tidaktampil = !Common.getApakahAdmin(parameterTambahan.getKodeAdminYgBoleh());
			row.setVisible(!tidaktampil);
			Common.freeze(row, tidaktampil);
			tampil |= !tidaktampil;
		} else {
			tampil |= true;
		}

		if (parameterRows != null)
			parameterRows.add(row);

		LampiranLain lampiranLain = LampiranLain.ambil(parameterTambahan.getId(),
				ParameterTambahanAstract.class.getName());
		if (lampiranLain != null) {
			MyFormRow rowLampiran = new MyFormRow();
			rowLampiran.setParent(rows);
			rowLampiran.appendChild(new Label());

			Vbox myvbox = new Vbox();
			myvbox.setParent(rowLampiran);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, parameterTambahan.getId(),
					ParameterTambahanAstract.class.getName(), "Lampiran", false, null, null, false, false, false,
					false);

			if (parameterTambahan.getHanyaTampilDiAdmin()) {
				boolean tidaktampil = !Common.getApakahAdmin(parameterTambahan.getKodeAdminYgBoleh());
				rowLampiran.setVisible(!tidaktampil);
				tampil |= !tidaktampil;
			} else {
				tampil |= true;
			}
		}

		try { if (parameterRows != null) reevaluasiSkipLogic(parameterRows); } catch (Throwable tSkip) { ais.common.ErrorAuditUtil.record(tSkip, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:318");}
		return tampil;
	}

	/**
	 * Membaca nilai KINI dari komponen input ZK (untuk evaluasi skip-logic secara live). Combobox dibaca dari
	 * value item terpilih (mis. "Bekerja:1" utk pilihan custom, "true"/"false" utk ya/tidak); komponen lain
	 * via getValue() refleksi. Aman: kembalikan "" bila gagal.
	 */
	public static String ambilNilaiComponent(Component c) {
		if (c == null) {
			return "";
		}
		try {
			if (c instanceof Combobox) {
				Combobox cb = (Combobox) c;
				Comboitem it = cb.getSelectedItem();
				if (it != null) {
					Object v = it.getValue();
					if (v != null) {
						return String.valueOf(v);
					}
					if (it.getLabel() != null) {
						return it.getLabel();
					}
				}
				return cb.getValue() == null ? "" : cb.getValue();
			}
			java.lang.reflect.Method m = c.getClass().getMethod("getValue");
			Object v = m.invoke(c);
			return v == null ? "" : String.valueOf(v);
		} catch (Throwable t) {
			return "";
		}
	}

	/**
	 * Evaluasi ulang SKIP-LOGIC (syaratTampil) untuk SELURUH baris parameter dalam satu form ZK. Dipanggil saat
	 * load (dari initComponent) dan setiap kali komponen acuan berubah (listener onChange/onSelect). Hanya baris
	 * yang PUNYA syaratTampil yang divisibilitasnya dikelola (baris lain tak disentuh, termasuk yang disembunyikan
	 * karena hanyaTampilDiAdmin). Baris turunan (keterangan/lampiran) ikut sembunyi bila induknya tersembunyi.
	 * Defensif total: bungkus try/catch agar tak pernah memutus render form.
	 */
	public static void reevaluasiSkipLogic(java.util.List<Row> parameterRows) {
		try {
			if (parameterRows == null || parameterRows.isEmpty()) {
				return;
			}
			java.util.Map<Long, String> peta = new java.util.HashMap<Long, String>();
			for (int i = 0; i < parameterRows.size(); i++) {
				Row r = parameterRows.get(i);
				if (r == null) {
					continue;
				}
				Object pid = r.getAttribute("stParamId");
				Object comp = r.getAttribute("stComponent");
				if (pid instanceof Long && comp instanceof Component) {
					peta.put((Long) pid, ambilNilaiComponent((Component) comp));
				}
			}
			java.util.Set<Long> hidden = new java.util.HashSet<Long>();
			for (int i = 0; i < parameterRows.size(); i++) {
				Row r = parameterRows.get(i);
				if (r == null) {
					continue;
				}
				Object p = r.getAttribute("stSyaratParam");
				if (p instanceof ParameterTambahan) {
					ParameterTambahan pt = (ParameterTambahan) p;
					boolean lolos = ais.common.ParameterTambahanHtmlHelper.lolosSyaratTampil(pt, peta);
					boolean adminHide = false;
					try {
						adminHide = pt.getHanyaTampilDiAdmin() && !Common.getApakahAdmin(pt.getKodeAdminYgBoleh());
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:391");
					}
					boolean visible = lolos && !adminHide;
					r.setVisible(visible);
					if (!visible && pt.getId() != null) {
						hidden.add(pt.getId());
					}
				}
			}
			for (int i = 0; i < parameterRows.size(); i++) {
				Row r = parameterRows.get(i);
				if (r == null) {
					continue;
				}
				Object childOf = r.getAttribute("stChildOf");
				if (childOf instanceof Long) {
					if (hidden.contains((Long) childOf)) {
						r.setVisible(false);
						r.setAttribute("stHidByLogic", Boolean.TRUE);
					} else if (Boolean.TRUE.equals(r.getAttribute("stHidByLogic"))) {
						r.setVisible(true);
						r.removeAttribute("stHidByLogic");
					}
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:416");
		}
	}

	public static Component ambilComponent(String val, ParameterTambahan parameterTambahan,
			EventListener eventListener) {
		return ambilComponent(val, parameterTambahan, eventListener, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Component ambilComponent(final String val, final ParameterTambahan parameterTambahan,
			final EventListener eventListener, boolean readonly) {
		Component component;

		if (parameterTambahan.getNilaiTidakBolehDiubah() || readonly) {
			if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)
			    || parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
				try {
					component = new Label(
							readonly && (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim()))
									? "{Tidak/belum diisi}"
									: Common.numberFormat.get().format(Double.parseDouble(val.trim())));
				} catch (Exception e) {
					component = new Label(readonly && (val == null || val.trim().isEmpty()) ? "{Tidak/belum diisi}"
							: val.equalsIgnoreCase("true") ? "Ya" : val.equalsIgnoreCase("false") ? "Tidak" : val);
				}
			} else {
				component = new Label(readonly && (val == null || val.trim().isEmpty()) ? "{Tidak/belum diisi}"
						: val.equalsIgnoreCase("true") ? "Ya" : val.equalsIgnoreCase("false") ? "Tidak" : val);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT)) {
			component = new Textbox(val);
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
			((Textbox) component).setRows(parameterTambahan.getJumlahBaris());
			((Textbox) component).setMaxlength(parameterTambahan.getJumlahText());

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL)) {
			Date nilai = null;
			try {
				nilai = val == null || val.trim().isEmpty() ? null : Common.dateFormat1.get().parse(val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:460"); }
			component = new MyDatebox(nilai);
			((MyDatebox) component).focus();
			((MyDatebox) component).setWidth("90%");
			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL_DAN_WAKTU)) {
			Date nilai = parseTanggalDanWaktu(val);
			component = new MyDatebox(nilai);
			((MyDatebox) component).setFormat(Common.dateFormat.get().toPattern());
			((MyDatebox) component).focus();
			((MyDatebox) component).setWidth("90%");
			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.WAKTU)) {
			Date nilai = null;
			try {
				nilai = val == null || val.trim().isEmpty() ? null : Common.timeFormat.get().parse(val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:485"); }
			component = new MyTimebox(nilai);
			((MyTimebox) component).setFormat(Common.timeFormat.get().toPattern());
			((MyTimebox) component).focus();
			((MyTimebox) component).setWidth("90%");
			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)) {
			Double nilai = null;
			try {
				// FIX NumberFormatException: nilaiStr bisa berisi string literal "null"
				// (bukan Java null) hasil serialisasi objek null di titik sebelumnya
				// (mis. String.valueOf(objekNull)). Anggap kosong, jangan parseDouble.
				nilai = (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) ? null
						: Double.parseDouble(val.trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:498"); }

			component = new MyDoublebox(nilai);
			((MyDoublebox) component).setWidth("90%");

			final Double nilailama = nilai;
			((MyDoublebox) component).addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Double valData = ((MyDoublebox) arg0.getTarget()).getValue();
					if (valData != null && parameterTambahan.getNilaiMin() > valData) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Nilai yang dimasukkan tidak boleh lebih kecil dari {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang dimasukkan; (2) masukkan nilai yang tidak kurang dari batas minimum; (3) simpan kembali data.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								Common.numberFormat.get().format(parameterTambahan.getNilaiMin()));
						((MyDoublebox) arg0.getTarget()).setValue(nilailama == null ? parameterTambahan.getNilaiMin() : nilailama);
					} else if (valData != null && parameterTambahan.getNilaiMax() < valData) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Nilai yang dimasukkan tidak boleh lebih besar dari {V1}. Langkah yang dapat dilakukan: (1) periksa kembali nilai yang dimasukkan; (2) masukkan nilai yang tidak melebihi batas maksimum; (3) simpan kembali data.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								Common.numberFormat.get().format(parameterTambahan.getNilaiMax()));
						((MyDoublebox) arg0.getTarget()).setValue(nilailama == null ? parameterTambahan.getNilaiMax() : nilailama);
					}
				}
			});

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
			component = new MyTextboxAngka(val);
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_YA_TIDAK)) {
			Boolean nilai = null;
			try {
				nilai = val == null || val.trim().isEmpty() ? null : Boolean.parseBoolean(val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:541"); }

			component = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig("Ya");
			comboitem.setValue(true);
			component.appendChild(comboitem);
			
			comboitem = new MyComboitemConfig("Tidak");
			comboitem.setValue(false);
			component.appendChild(comboitem);
			
			((Combobox) component).setReadonly(true);
			((Combobox) component).setWidth("90%");
			Common.selectComboItem(((Combobox) component), nilai);

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_CUSTOM)) {
			component = new Combobox();
			String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
			Arrays.sort(ss);
			for (String s : ss) {
				String[] ssss = StringUtils.split(s, ":");
				MyComboitemConfig comboitem = new MyComboitemConfig(ssss[0]);
				comboitem.setValue(s);
				component.appendChild(comboitem);
			}
			((Combobox) component).setReadonly(true);
			((Combobox) component).setWidth("90%");
			Common.selectComboItem(((Combobox) component), val);

			try {
				if (((Combobox) component).getSelectedItem() == null
						|| ((Combobox) component).getSelectedItem().getValue() == null) {
					String valBaru = val;
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						if (ssss.length > 1 && ssss[1].equalsIgnoreCase(val)) {
							valBaru = s;
						}
					}
					Common.selectComboItem(((Combobox) component), valBaru);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:586"); }

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_OBJECT)) {
			final Bandbox bandbox = new Bandbox();
			try {
				final Class clazz = Class.forName(parameterTambahan.getNilaiDataInputan());
				if (val != null && !val.trim().isEmpty()) {
					GeneralValueObject o = ConstantValues.ambil(clazz.getName(), Long.parseLong(val));
					if (o != null) {
						bandbox.setValue(o.getNama());
						bandbox.setAttribute("data", o);
					}
				}

				ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
				boolean adaNama = false;
				boolean adaKode = false;
				for (String p : classMetadata.getPropertyNames()) {
					if (p.equalsIgnoreCase("nama")) {
						adaNama = true;
					} else if (p.equalsIgnoreCase("kode")) {
						adaKode = true;
					}
				}
				final boolean dnama = adaNama;
				final boolean dkode = adaKode;

				Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
				bandpopup.setParent(bandbox);
				bandpopup.setWidth("400px");
				bandpopup.setHeight("400px");

				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setWidth("100%");
				radiogroup.setHeight("100%");
				radiogroup.setParent(bandpopup);

				Panel panel = new ais.ui.util.MyPanelConfig();
				panel.setParent(radiogroup);
				panel.setWidth("100%");
				panel.setHeight("100%");
				panel.setTitle(parameterTambahan.getLabelInputan());
				panel.setBorder("none");
				panel.setStyle("border:0px;");

				Panelchildren panelchildren = new Panelchildren();
				panelchildren.setParent(panel);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(panelchildren);

				North north = new North();
				north.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(north, true);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(north);

				final Textbox kode = new Textbox();
				final Textbox nama = new Textbox();
				final Rows rows = new Rows();

				EventListener eventListenerCari = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(rows);
						Session sessionLocal = null;
						
						try {
							// PERBAIKAN: Menggunakan session terisolasi & memastikan ditutup di finally
							sessionLocal = HibernateUtil.getSessionFactory().openSession();
							
							Criteria c = sessionLocal.createCriteria(clazz)
									.add(parameterTambahan.getKondisiDataInputan().isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.sqlRestriction(parameterTambahan.getKondisiDataInputan()))
									.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))
									.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
											: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
									.setMaxResults(1000);

							if (dnama && dkode) {
								c.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
							} else if (dkode) {
								c.addOrder(Order.asc("kode"));
							} else if (dnama) {
								c.addOrder(Order.asc("nama"));
							} else {
								c.addOrder(Order.asc("id"));
							}

							List<GeneralValueObject> generalValueObjects = ConstantValues.simpleList(c, clazz);

							for (final GeneralValueObject generalValueObject : generalValueObjects) {
								MyFormRow row = new MyFormRow();
								row.setValign("top");
								row.setParent(rows);

								String d = generalValueObject.toString();
								if (dnama) {
									d = generalValueObject.getNama();
								}

								Radio radio = new Radio(d);
								radio.setSelected(generalValueObject.getId().toString().equalsIgnoreCase(val));
								radio.setParent(row);
								radio.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										bandbox.setValue(generalValueObject.getNama());
										bandbox.setOpen(false);
										bandbox.setAttribute("data", generalValueObject);

										if (eventListener != null) {
											eventListener.onEvent(new Event("", bandbox, generalValueObject));
										}
									}
								});
							}
						} catch (Exception ex) {
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:711");
						} finally {
							if (sessionLocal != null && sessionLocal.isOpen()) {
								try { sessionLocal.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:714");}
							}
						}
					}
				};

				if (adaKode) {
					toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode")));
					toolbar.appendChild(kode);
					kode.setCols(4);
				}
				if (adaNama) {
					toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
					toolbar.appendChild(nama);
					nama.setCols(4);
				}

				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setMold("paging");
				grid.setPageSize(10);
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);

				org.zkoss.zul.Column column = new org.zkoss.zul.Column();
				column.setParent(columns);
				column.setLabel("Data " + clazz.getSimpleName());

				rows.setParent(grid);

				bandbox.addEventListener("onOpen", eventListenerCari);
				kode.addEventListener("onOK", eventListenerCari);
				nama.addEventListener("onOK", eventListenerCari);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
				button.addEventListener("onClick", eventListenerCari);
				button.setParent(toolbar);
				toolbar.appendChild(Common.createCleanButton(bandbox, new GetEventListener() {
					@Override
					public void setEventListener(EventListener eventListener) {}
					@Override
					public EventListener getEventListener() {
						bandbox.setAttribute("data", null);
						return eventListener;
					}
				}));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						bandbox.setOpen(false);
					}
				});
				cancel.setParent(toolbar);

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:784");
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX)) {
			final Bandbox bandbox = new Bandbox();
			bandbox.setValue(val);
			TreeSet<String> treeSet = new TreeSet<String>();
			String[] rowsData = StringUtils.split(parameterTambahan.getNilaiDataInputan(), "\n");
			Arrays.sort(rowsData);
			for (String rowData : rowsData) {
				String[] colAtauRow = rowData.split("->");
				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");
					String col = ssss[0].trim();
					treeSet.add(col);
				}
			}

			Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
			bandpopup.setParent(bandbox);
			bandpopup.setWidth("700px");
			bandpopup.setHeight("400px");

			Radiogroup radiogroup = new Radiogroup();
			radiogroup.setWidth("100%");
			radiogroup.setHeight("100%");
			radiogroup.setParent(bandpopup);

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(radiogroup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle(parameterTambahan.getLabelInputan());
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

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
					bandbox.setOpen(false);
				}
			});
			cancel.setParent(toolbar);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			org.zkoss.zul.Column column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Parameter");

			for (String c : treeSet) {
				column = new org.zkoss.zul.Column();
				column.setParent(columns);
				column.setLabel(c);
			}

			Rows rows = new Rows();
			rows.setParent(grid);

			for (String rowData : rowsData) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				String[] colAtauRow = rowData.split("->");
				String rData = colAtauRow[0];
				row.appendChild(new Label(rData));

				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);

				for (String c : treeSet) {
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						String col = ssss[0].trim();

						if (col.equalsIgnoreCase(c)) {
							final String nilai = ssss.length > 1 ? ssss[1] : s;
							Radio radio = new Radio(nilai);
							radio.setValue(nilai);
							radio.setParent(row);
							radio.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									bandbox.setValue(nilai);
									bandbox.setOpen(false);
									bandbox.setAttribute("nilai", nilai);

									if (eventListener != null) {
										eventListener.onEvent(new Event("", bandbox, nilai));
									}
								}
							});
						}
					}
				}
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_NILAI)) {
			final Bandbox bandbox = new Bandbox();
			JSONObject temporary;
			try {
				temporary = val == null || val.isEmpty() ? new JSONObject() : new JSONObject(val);
			} catch (Exception e) {
				temporary = new JSONObject();
			}
			final JSONObject jsonObject = temporary;

			bandbox.setValue(val);
			TreeSet<String> treeSet = new TreeSet<String>();
			String[] rowsData = StringUtils.split(parameterTambahan.getNilaiDataInputan(), "\n");
			Arrays.sort(rowsData);
			for (String rowData : rowsData) {
				String[] colAtauRow = rowData.split("->");
				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");
					String col = ssss[0].trim();
					treeSet.add(col);
				}
			}

			Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
			bandpopup.setParent(bandbox);
			bandpopup.setWidth("700px");
			bandpopup.setHeight("400px");

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(bandpopup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle(parameterTambahan.getLabelInputan());
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

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
					bandbox.setOpen(false);
				}
			});
			cancel.setParent(toolbar);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			org.zkoss.zul.Column column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Parameter");

			for (String c : treeSet) {
				column = new org.zkoss.zul.Column();
				column.setParent(columns);
				column.setLabel(c);
			}

			Rows rows = new Rows();
			rows.setParent(grid);

			for (String rowData : rowsData) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				String[] colAtauRow = rowData.split("->");
				final String rData = colAtauRow[0].trim();
				row.appendChild(new Label(rData));

				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);

				for (String c : treeSet) {
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						final String col = ssss[0].trim();

						if (col.equalsIgnoreCase(c)) {
							final String nilai = ssss.length > 1 ? ssss[1] : s;
							final Radio radio = new Radio(nilai);
							radio.setValue(nilai);
							radio.setParent(row);

							try {
								String key = rData.toLowerCase();
								JSONObject rowDataNilai = jsonObject.isNull(key) ? new JSONObject()
										: jsonObject.getJSONObject(key);
								String ni = rowDataNilai.isNull(col) ? "" : rowDataNilai.getString(col);
								radio.setChecked(nilai.equalsIgnoreCase(ni));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:1044"); }

							radio.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									String key = rData.toLowerCase();
									JSONObject rowData = jsonObject.isNull(key) ? new JSONObject()
											: jsonObject.getJSONObject(key);
									rowData.put(col, nilai);
									jsonObject.put(key, rowData);
									String nil = jsonObject.toString();
									bandbox.setValue(nil);
									bandbox.setAttribute("nilai", nil);

									if (eventListener != null) {
										eventListener.onEvent(new Event("", bandbox, nil));
									}
								}
							});
						}
					}
				}
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_COMBO)) {
			final Bandbox bandbox = new Bandbox();
			JSONObject temporary;
			try {
				temporary = val == null || val.isEmpty() ? new JSONObject() : new JSONObject(val);
			} catch (Exception e) {
				temporary = new JSONObject();
			}
			final JSONObject jsonObject = temporary;

			bandbox.setValue(val);
			TreeSet<String> treeSet = new TreeSet<String>();
			String[] rowsData = StringUtils.split(parameterTambahan.getNilaiDataInputan(), "\n");
			Arrays.sort(rowsData);
			for (String rowData : rowsData) {
				String[] colAtauRow = rowData.split("->");
				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");
					String col = ssss[0].trim();
					treeSet.add(col);
				}
			}

			Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
			bandpopup.setParent(bandbox);
			bandpopup.setWidth("500px");
			bandpopup.setHeight("300px");

			Panel panel = new ais.ui.util.MyPanelConfig();
			panel.setParent(bandpopup);
			panel.setWidth("100%");
			panel.setHeight("100%");
			panel.setTitle(parameterTambahan.getLabelInputan());
			panel.setBorder("none");
			panel.setStyle("border:0px;");

			Panelchildren panelchildren = new Panelchildren();
			panelchildren.setParent(panel);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panelchildren);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

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
					bandbox.setOpen(false);
				}
			});
			cancel.setParent(toolbar);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(center);

			Columns columns = new Columns();
			columns.setParent(grid);

			org.zkoss.zul.Column column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Parameter");

			column = new org.zkoss.zul.Column();
			column.setParent(columns);
			column.setLabel("Nilai");

			Rows rows = new Rows();
			rows.setParent(grid);

			for (String rowData : rowsData) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				String[] colAtauRow = rowData.split("->");
				final String rData = colAtauRow[0].trim();
				row.appendChild(new Label(rData));

				String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";
				String[] ss = StringUtils.split(cols, ";");
				Arrays.sort(ss);

				final Combobox comboboxNilai = new Combobox();
				comboboxNilai.setWidth("95%");
				row.appendChild(comboboxNilai);
				comboboxNilai.setReadonly(true);

				for (String c : treeSet) {
					for (String s : ss) {
						String[] ssss = StringUtils.split(s, ":");
						String col = ssss[0].trim();

						if (col.equalsIgnoreCase(c)) {
							String nilai = ssss.length > 1 ? ssss[1] : s;
							Comboitem radio = new Comboitem(nilai);
							radio.setValue(nilai);
							radio.setParent(comboboxNilai);
						}
					}
				}

				try {
					String key = rData.toLowerCase();
					String rowDataNilai = jsonObject.isNull(key) ? null : jsonObject.getString(key);
					Common.selectComboItem(comboboxNilai, rowDataNilai);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:1197"); }

				comboboxNilai.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						String key = rData.toLowerCase();
						jsonObject.put(key, comboboxNilai.getSelectedItem() == null ? ""
								: comboboxNilai.getSelectedItem().getValue());

						String nil = jsonObject.toString();
						bandbox.setValue(nil);
						bandbox.setAttribute("nilai", nil);

						if (eventListener != null) {
							eventListener.onEvent(new Event("", bandbox, nil));
						}
					}
				});
			}

			component = bandbox;
			((Bandbox) component).setReadonly(true);
			((Bandbox) component).setWidth("90%");

			if (eventListener != null) {
				component.addEventListener("onChange", eventListener);
			}

		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_BANYAK)) {
			component = new Vbox();
			String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
			Arrays.sort(ss);
			for (String s : ss) {
				MyCheckboxConfig comboitem = new MyCheckboxConfig(s);
				comboitem.setValue(s);
				component.appendChild(comboitem);

				if(val != null) {
					for (String g : val.split(";")) {
						if (g.trim().equalsIgnoreCase(s.trim())) {
							comboitem.setChecked(true);
						}
					}
				}

				if (eventListener != null) {
					comboitem.addEventListener("onClick", eventListener);
				}
			}

		} else if (ParameterTambahanAstract.CUSTOM_PILIHAN.contains(parameterTambahan.getTipeDataInputan())) {
			component = ParameterTambahanAstract.ambilComponentCustom(val, parameterTambahan, eventListener);
		} else {
			component = null;
		}
		
		return component;
	}

	public static Component ambilComponentCustom(String val, ParameterTambahan parameterTambahan,
			EventListener eventListener) {
		if (val != null && val.contains("->")) {
			val = val.split("->")[0];
		}
		
		Component component = new Label();
		if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MAHASISWA)) {
			component = new AmbilDataSemuaMahasiswaBanbox();
			Mahasiswa mahasiswa = (Mahasiswa) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Mahasiswa.class.getName(), Long.parseLong(val)));
			component.setAttribute("mahasiswa", mahasiswa);
			component.setAttribute("myValue", mahasiswa);
			((AmbilDataSemuaMahasiswaBanbox) component).setWidth("90%");
			((AmbilDataSemuaMahasiswaBanbox) component)
					.setValue(mahasiswa == null ? "" : mahasiswa.getNim() + "-" + mahasiswa.getNama());
			if (eventListener != null) {
				((AmbilDataSemuaMahasiswaBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_SISWA)) {
			component = new AmbilDataSiswaBanbox();
			Siswa siswa = (Siswa) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(val)));
			component.setAttribute("siswa", siswa);
			component.setAttribute("myValue", siswa);
			((AmbilDataSiswaBanbox) component)
					.setValue(siswa == null ? "" : siswa.getNomorIndukNasional() + "-" + siswa.getNama());
			((AmbilDataSiswaBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataSiswaBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_DOSEN)) {
			component = new AmbilDataDosenBanbox();
			Dosen dosen = (Dosen) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(val)));
			component.setAttribute("dosen", dosen);
			component.setAttribute("myValue", dosen);
			((AmbilDataDosenBanbox) component).setValue(dosen == null ? "" : dosen.getNama());
			((AmbilDataDosenBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataDosenBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_GURU)) {
			component = new AmbilDataGuruBanbox();
			Guru guru = (Guru) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Guru.class.getName(), Long.parseLong(val)));
			component.setAttribute("guru", guru);
			component.setAttribute("myValue", guru);
			((AmbilDataGuruBanbox) component).setValue(guru == null ? "" : guru.getNim() + "-" + guru.getNama());
			((AmbilDataGuruBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataGuruBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PEGAWAI)) {
			component = new AmbilDataPegawaiBanbox(false, true);
			Pegawai pegawai = (Pegawai) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(Pegawai.class.getName(), Long.parseLong(val)));
			component.setAttribute("pegawai", pegawai);
			component.setAttribute("myValue", pegawai);
			((AmbilDataPegawaiBanbox) component)
					.setValue(pegawai == null ? "" : pegawai.getNim() + "-" + pegawai.getNama());
			((AmbilDataPegawaiBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataPegawaiBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PENYEDIA)) {
			component = new AmbilDataPenyediaAssetBanbox();
			PenyediaAsset penyedia = (PenyediaAsset) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(PenyediaAsset.class.getName(), Long.parseLong(val)));
			component.setAttribute("penyediaAsset", penyedia);
			component.setAttribute("myValue", penyedia);
			((AmbilDataPenyediaAssetBanbox) component).setValue(penyedia == null ? "" : penyedia.getNama());
			((AmbilDataPenyediaAssetBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataPenyediaAssetBanbox) component).setEventListener(eventListener);
			}
		} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_KELAS_SISWA)) {
			component = new AmbilDataKelasSiswaBanbox();
			KelasSiswa kelasSiswa = (KelasSiswa) (val == null || val.isEmpty() || !Common.isNumber(val) ? null
					: ConstantValues.ambil(KelasSiswa.class.getName(), Long.parseLong(val)));
			component.setAttribute("kelasSiswa", kelasSiswa);
			component.setAttribute("myValue", kelasSiswa);
			((AmbilDataKelasSiswaBanbox) component)
					.setValue(kelasSiswa == null ? "" : kelasSiswa.getNim() + "-" + kelasSiswa.getNama());
			((AmbilDataKelasSiswaBanbox) component).setWidth("90%");
			if (eventListener != null) {
				((AmbilDataKelasSiswaBanbox) component).setEventListener(eventListener);
			}
		}

		return component;
	}

	public static String ambilVal(Row row, ParameterTambahan parameterTambahan) {
		String componentData = "component";
		return ambilVal(row, parameterTambahan, componentData);
	}

	public static String ambilVal(Row row, ParameterTambahan parameterTambahan, String componentData) {
		if (row == null) {
			return "";
		}
		Component component = (Component) row.getAttribute(componentData);
		if (component == null) {
			/*
			 * Cadangan bila atribut komponen tidak terpasang. Susunan baku satu baris
			 * parameter adalah [0] = Label judul, [1] = komponen masukan.
			 *
			 * TETAPI baris bisa sah-sah saja hanya berisi label: {@link #initComponent}
			 * MENDAFTARKAN baris ke parameterRows TANPA SYARAT (lihat parameterRows.add(row)
			 * di method itu), termasuk ketika rantai pembuatan komponen tidak menghasilkan
			 * apa pun -- mis. tipeDataInputan yang belum punya cabang penanganan, atau null.
			 *
			 * Dahulu indeks 1 diambil langsung dan IndexOutOfBoundsException-nya ditangkap
			 * sebagai KENDALI ALUR. Hasil akhirnya memang sama (nilai kosong), tetapi setiap
			 * penyimpanan membanjiri ErrorAudit dengan stack trace penuh -- satu per baris
			 * bermasalah, per simpan; terpantau di produksi lewat
			 * BiodataMahasiswa.populateParameterTambahanAlumni. Menangkap exception juga jauh
			 * lebih mahal daripada sekadar memeriksa ukuran daftar. Karena itu jumlah anak
			 * diperiksa lebih dulu, dan ketiadaan komponen diperlakukan sebagai keadaan
			 * WAJAR (nilai kosong), bukan sebagai error.
			 */
			java.util.List<?> anak = row.getChildren();
			if (anak == null || anak.size() < 2) {
				return "";
			}
			Object kandidat = anak.get(1);
			if (!(kandidat instanceof Component)) {
				return "";
			}
			component = (Component) kandidat;
		}
		return ambilValComponent(component, parameterTambahan);
	}

	/**
	 * Membaca teks MENTAH sebuah komponen masukan ZK TANPA memicu validasi.
	 *
	 * <p><b>Kenapa perlu.</b> {@code InputElement.getText()} bukan sekadar getter: ia
	 * MEM-VALIDASI ulang isi komponen dan melempar
	 * {@code WrongValueException: You must specify a number, rather than -.} bila pengguna
	 * mengetik teks yang belum berupa angka (mis. baru mengetik tanda minus "-"). Karena
	 * {@code getText()} itu justru dipakai pada jalur PEMULIHAN error, satu ketikan "-" membuat
	 * jalur pemulihan ikut gagal sehingga teks mentahnya hilang sama sekali dan proses simpan
	 * terganggu. {@code getRawText()} mengembalikan nilai apa adanya dari klien tanpa validasi,
	 * jadi dipakai lebih dulu; {@code getText()} hanya sebagai cadangan (dibungkus try-catch),
	 * dan bila semuanya gagal hasilnya null yang diartikan "tidak ada nilai".</p>
	 */
	private static String ambilTeksMentahAman(org.zkoss.zul.impl.InputElement inputElement) {
		if (inputElement == null) {
			return null;
		}
		try {
			String raw = inputElement.getRawText();
			if (raw != null) {
				return raw;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:ambilTeksMentahAman-getRawText");
		}
		try {
			Object rawValue = inputElement.getRawValue();
			if (rawValue != null) {
				return rawValue.toString();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:ambilTeksMentahAman-getRawValue");
		}
		try {
			return inputElement.getText();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAstract.java:ambilTeksMentahAman-getText");
		}
		return null;
	}

	private static Double ambilAngkaPertamaAman(String raw) {
		if (raw == null || raw.trim().length() == 0) {
			return null;
		}
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([+-]?[0-9]+([.,][0-9]+)?)").matcher(raw);
		if (!m.find()) {
			return null;
		}
		try {
			return Double.valueOf(Double.parseDouble(m.group(1).replace(",", ".")));
		} catch (Exception e) {
			return null;
		}
	}

	private static Date parseTanggalAman(String raw, java.text.DateFormat format) {
		if (raw == null || raw.trim().length() == 0 || format == null) {
			return null;
		}
		try {
			return format.parse(raw.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static Date parseTanggalDanWaktu(String raw) {
		if (raw == null || raw.trim().length() == 0) {
			return null;
		}
		Date hasil = parseTanggalAman(raw, Common.dateFormat.get());
		if (hasil != null) {
			return hasil;
		}
		String[] polaIso = new String[] { "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss" };
		for (int i = 0; i < polaIso.length; i++) {
			try {
				java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(polaIso[i]);
				format.setLenient(false);
				return format.parse(raw.trim());
			} catch (java.text.ParseException ignored) {
				// Coba pola berikutnya; input HTML datetime-local memang memakai ISO.
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String ambilValComponent(Component component, ParameterTambahan parameterTambahan) {
		String val = "";
		
		if (component == null) return val;

		try {
			if (parameterTambahan.getNilaiTidakBolehDiubah()) {
				if (component instanceof Label) {
					if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)
					    || parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
						try {
							val = Common.numberFormat.get().parse((((Label) component).getValue()).trim()).doubleValue() + "";
						} catch (Exception e) {
							val = (((Label) component).getValue()).trim();
						}
					} else {
						val = (((Label) component).getValue()).trim();
					}
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_OBJECT)) {
				GeneralValueObject generalValueObject = (GeneralValueObject) component.getAttribute("data");
				val = generalValueObject == null ? "-1" : generalValueObject.getId().toString();
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_NILAI)
					|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MATRIX_BANYAK_COMBO)) {
				if (component instanceof Textbox) {
					val = (((Textbox) component).getValue()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_CUSTOM)) {
				if (component instanceof Combobox) {
					val = (String) (((Combobox) component).getSelectedItem() == null ? ""
							: (((Combobox) component).getSelectedItem().getValue()));
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_BANYAK)) {
				// PERBAIKAN: Gunakan StringBuilder dibanding manipulasi string manual (+)
				StringBuilder sb = new StringBuilder();
				List<Component> components = component.getChildren();
				for (Component compo : components) {
					if (compo instanceof Checkbox) {
						Checkbox c = (Checkbox) compo;
						if (c.isChecked()) {
							if (sb.length() > 0) sb.append(";");
							sb.append(String.valueOf(c.getValue()));
						}
					}
				}
				val = sb.toString();
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL_DAN_WAKTU)) {
				if (component instanceof MyDatebox) {
					Date nilai;
					try {
						nilai = (((MyDatebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						String raw = ambilTeksMentahAman((MyDatebox) component);
						nilai = parseTanggalAman(raw, Common.dateFormat.get());
					}
					val = nilai == null ? "" : Common.dateFormat.get().format(nilai);
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.WAKTU)) {
				if (component instanceof MyTimebox) {
					Date nilai = (((MyTimebox) component).getValue());
					val = nilai == null ? "" : Common.timeFormat.get().format(nilai);
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TANGGAL)) {
				if (component instanceof MyDatebox) {
					Date nilai;
					try {
						nilai = (((MyDatebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						String raw = ambilTeksMentahAman((MyDatebox) component);
						nilai = parseTanggalAman(raw, Common.dateFormat1.get());
					}
					val = nilai == null ? "" : Common.dateFormat1.get().format(nilai);
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.ANGKA)) {
				if (component instanceof MyDoublebox) {
					try {
						val = (((MyDoublebox) component).getValue()) + "";
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						// FIX WrongValueException "You must specify a number, rather than ...":
						// Doublebox.getValue() melempar exception bila teks mentah tak bisa
						// di-parse sbg angka (mis. nilai lama tersimpan sbg string bebas "4
						// orang" sebelum field ini jadi Doublebox). JANGAN lempar ulang --
						// akan menggagalkan seluruh populateParameterTambahan utk field lain.
						// Fallback: ambil angka di depan teks mentah, bila tak ada anggap 0.
						// FIX WrongValueException "You must specify a number, rather than -.":
						// pembacaan teks mentah TIDAK BOLEH memakai getText() secara langsung,
						// karena getText() memvalidasi ulang isinya dan ikut melempar
						// WrongValueException (mis. pengguna baru mengetik tanda minus "-"),
						// sehingga jalur pemulihan ini justru ikut gagal dan proses simpan
						// parameter tambahan alumni batal. Pakai pembaca mentah yang aman
						// (getRawText lebih dulu) -- lihat ambilTeksMentahAman.
						String raw = ambilTeksMentahAman((MyDoublebox) component);
						Double fallback = ambilAngkaPertamaAman(raw);
						if (fallback == null) {
							fallback = ambilAngkaPertamaAman(wve.getMessage());
						}
						val = fallback == null ? "" : fallback + "";
					}
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.TEXT_ANGKA)) {
				if (component instanceof MyTextboxAngka) {
					val = (((MyTextboxAngka) component).getValue()) + "";
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_YA_TIDAK)) {
				if (component instanceof Combobox && ((Combobox) component).getSelectedItem() != null) {
					val = ((Boolean) ((Combobox) component).getSelectedItem().getValue()) + "";
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_MAHASISWA)) {
				if (component instanceof AmbilDataMahasiswaBanbox) {
					Mahasiswa mahasiswa = ((Mahasiswa) component.getAttribute("mahasiswa"));
					val = mahasiswa == null ? ""
							: mahasiswa.getId().toString() + "->" + (mahasiswa.getNim() + " " + mahasiswa.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_SISWA)) {
				if (component instanceof AmbilDataSiswaBanbox) {
					Siswa siswa = ((Siswa) component.getAttribute("siswa"));
					val = siswa == null ? ""
							: siswa.getId().toString() + "->" + siswa.getNomorIndukNasional() + " " + siswa.getNama();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_DOSEN)) {
				if (component instanceof AmbilDataDosenBanbox) {
					Dosen dosen = ((Dosen) component.getAttribute("dosen"));
					val = dosen == null ? ""
							: dosen.getId().toString() + "->" + (dosen.getNidn() + " " + dosen.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_GURU)) {
				if (component instanceof AmbilDataGuruBanbox) {
					Guru guru = ((Guru) component.getAttribute("guru"));
					val = guru == null ? ""
							: guru.getId().toString() + "->" + (guru.getKode() + " " + guru.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PEGAWAI)) {
				if (component instanceof AmbilDataPegawaiBanbox) {
					Pegawai pegawai = ((Pegawai) component.getAttribute("pegawai"));
					val = pegawai == null ? ""
							: pegawai.getId().toString() + "->" + (pegawai.getMycode() + " " + pegawai.getNama()).trim();
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_PENYEDIA)) {
				if (component instanceof AmbilDataPenyediaAssetBanbox) {
					PenyediaAsset penyedia = ((PenyediaAsset) component.getAttribute("penyediaAsset"));
					val = penyedia == null ? "" : penyedia.getId().toString() + "->"
							+ (penyedia.getNama() == null ? "" : penyedia.getNama().trim());
				}
			} else if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahanAstract.PILIHAN_KELAS_SISWA)) {
				if (component instanceof AmbilDataKelasSiswaBanbox) {
					KelasSiswa kelasSiswa = ((KelasSiswa) component.getAttribute("kelasSiswa"));
					val = kelasSiswa == null ? "" : kelasSiswa.getId().toString() + "->" + (kelasSiswa.getNama()).trim();
				}
			}
		} catch (org.zkoss.zk.ui.WrongValueException wve) {
			// Nilai parameter tambahan berasal dari isian dinamis. WrongValueException di sini
			// adalah input pengguna/data lama yang tidak sesuai tipe komponen, bukan error server.
			// Biarkan nilai kosong agar field lain tetap tersimpan dan log produksi tidak banjir.
			val = "";
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:1466");
		}

		try {
			if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
				if (component instanceof Textbox) {
					val = (((Textbox) component).getValue()).trim();
				} else if (component instanceof Combobox) {
					val = (((Combobox) component).getSelectedItem() == null ? ""
							: (((Combobox) component).getSelectedItem().getValue())).toString();
				} else if (component instanceof Datebox) {
					Date nilai;
					try {
						nilai = (((Datebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						nilai = parseTanggalAman(ambilTeksMentahAman((Datebox) component), Common.dateFormat1.get());
					}
					val = nilai == null ? "" : Common.dateFormat1.get().format(nilai);
				} else if (component instanceof Doublebox) {
					Double nilai;
					try {
						nilai = (((Doublebox) component).getValue());
					} catch (org.zkoss.zk.ui.WrongValueException wve) {
						nilai = ambilAngkaPertamaAman(ambilTeksMentahAman((Doublebox) component));
						if (nilai == null) {
							nilai = ambilAngkaPertamaAman(wve.getMessage());
						}
					}
					val = nilai == null ? "" : nilai + "";
				} else if (component instanceof Intbox) {
					val = (((Intbox) component).getValue()) + "";
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ParameterTambahanAstract.java:1486");
		}

		if (val != null) {
			val = org.apache.commons.lang3.StringUtils.replace(val, "\n", " ");
		} else {
			val = "";
		}

		return val;
	}
}
