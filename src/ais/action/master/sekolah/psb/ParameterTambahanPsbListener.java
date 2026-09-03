package ais.action.master.sekolah.psb;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelompokParameterTambahanCalonSiswa;
import ais.database.model.sekolah.ParameterTambahanGelombangPendaftaranPsb;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener form dinamis untuk parameter tambahan pada Penerimaan Siswa Baru (PSB/PPDB), khusus
 * satu gelombang pendaftaran ({@link GelombangPendaftaranPsb}). Mengikuti pola parameter tambahan
 * generik ({@link ParameterTambahan}), dengan tambahan aturan visibilitas berlapis: baris
 * parameter tersaring berdasarkan (1) gelombang pendaftaran yang berlaku (spesifik atau berlaku
 * untuk semua gelombang), dan (2) status login pengguna — parameter dapat dikonfigurasi tampil
 * hanya sebelum login (formulir publik) atau hanya setelah login, lewat flag
 * {@code tampilDiFromSebelumLogin}/{@code tampilDiFromSetelahLogin} pada relasi
 * {@link ParameterTambahanGelombangPendaftaranPsb}. Field {@code formPendaftaran} membedakan
 * konteks form pendaftaran awal vs form login calon siswa, dicek terhadap
 * {@code tampilFormTambahanSaatRegistrasi}/{@code tampilFormTambahanSaatLoginCalonMhs} pada
 * gelombang — dengan pengecualian permisif: saat pengguna belum login, salah satu dari kedua
 * centang tersebut sudah cukup untuk menampilkan form (perbaikan agar form pendaftaran publik
 * tidak kehilangan tampilan parameter tambahan hanya karena centang yang salah yang diperiksa).
 * Mendukung validasi wajib-isi/lampiran wajib lewat {@link #validate()} dan pemetaan nilai kembali
 * ke entitas lewat {@link #onSave(CalonSiswa)}.
 */
public class ParameterTambahanPsbListener implements EventListener {

	private List<Row> parameterRows;
	private Combobox gelombangPendaftaranPsb;
	private Rows rows;
	private CalonSiswa calonSiswa;
	private Map<String, LampiranLain> lampiranLains;
	private Boolean formPendaftaran = true;
	private GelombangPendaftaranPsb gel = null;

	/** Membuat listener dengan gelombang pendaftaran diambil dari dropdown {@code gelombangPendaftaranPsb} (dievaluasi ulang tiap {@link #onEvent}). */
	public ParameterTambahanPsbListener(CalonSiswa calonSiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Combobox gelombangPendaftaranPsb, Boolean formPendaftaran,
			Rows rows) {
		this.formPendaftaran = formPendaftaran;
		this.parameterRows = parameterRows;
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
		this.rows = rows;
		this.calonSiswa = calonSiswa;
		this.lampiranLains = lampiranLains;
	}

	/** Membuat listener dengan gelombang pendaftaran ({@code gel}) yang sudah diketahui secara eksplisit, tanpa bergantung pada dropdown. */
	public ParameterTambahanPsbListener(CalonSiswa calonSiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, GelombangPendaftaranPsb gel, Boolean formPendaftaran, Rows rows) {
		this.formPendaftaran = formPendaftaran;
		this.parameterRows = parameterRows;
		this.gel = gel;
		this.rows = rows;
		this.calonSiswa = calonSiswa;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang ditampilkan: parameter wajib diisi
	 * harus memiliki nilai, dan parameter yang mewajibkan lampiran harus sudah memiliki entri di
	 * {@code lampiranLains}. Menampilkan {@link MyMessageboxConfig} dan menghentikan pemeriksaan
	 * pada pelanggaran pertama.
	 *
	 * @return {@code true} bila seluruh parameter tambahan valid, {@code false} bila ada yang gagal
	 * @throws Exception diteruskan dari kegagalan membaca nilai baris
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa = (KelompokParameterTambahanCalonSiswa) row
					.getAttribute("kelompokParameterTambahanCalonSiswa");
			if (parameterTambahan != null && kelompokParameterTambahanCalonSiswa != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CalonSiswa.class, calonSiswa.getId(),
						kelompokParameterTambahanCalonSiswa.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				System.out.printf("validate " + val);
				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Pilihan \"" + parameterTambahan.getLabelInputan() + "\" harus dipilih",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran() && !lampiranLains.keySet().contains(jenis)) {
						MyMessageboxConfig.show(
								"Untuk pilihan \"" + parameterTambahan.getLabelInputan()
										+ "\", lampiran harus di-upload",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Menyalin nilai-nilai yang sedang diisikan pada {@code parameterRows} kembali ke entitas {@code calonSiswa}, dipanggil sebelum entitas disimpan. */
	@SuppressWarnings({})
	public void onSave(CalonSiswa calonSiswa) {

		calonSiswa.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan dinamis PSB sesuai gelombang pendaftaran
	 * dan status login pengguna saat ini (lihat javadoc kelas untuk aturan visibilitasnya). Bila
	 * gelombang tidak mengizinkan tampil pada konteks form saat ini, method berhenti tanpa
	 * menampilkan apa pun. Selain itu berperilaku sama seperti
	 * {@code ParameterTambahanCutiDanIzinListener#onEvent}: membangun baris judul kelompok dan
	 * baris input per parameter aktif, memuat nilai tersimpan dari
	 * {@code calonSiswa.getParameterTambahanInds()}, dan menempelkan tiap baris ke grid
	 * {@code rows}.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		boolean tampilkanLabelBesarPadaFormPMB = Common.bolehKonfigurasi("tampilkan_label_besar_pada_form_PMB");

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		GelombangPendaftaranPsb gel = this.gel != null ? this.gel
				: calonSiswa.getGelombangPendaftaranPsb() != null ? calonSiswa.getGelombangPendaftaranPsb()
						: (GelombangPendaftaranPsb) (gelombangPendaftaranPsb == null
								|| gelombangPendaftaranPsb.getSelectedItem() == null ? null
										: gelombangPendaftaranPsb.getSelectedItem().getValue());

		/*
		 * SARINGAN TAMPIL FORM TAMBAHAN.
		 *
		 * MASALAH SEBELUMNYA: formulir pendaftaran PUBLIK (/ppdb -> PSBAction ->
		 * CalonSiswaAction.onAddExternal) membuat listener ini dengan formPendaftaran=false,
		 * sehingga yang diperiksa HANYA "Tampil Form Tambahan Saat Login Calon Mhs".
		 * Centang "Tampil Form Tambahan Saat Registrasi" -- pilihan yang paling wajar dicentang
		 * admin untuk sebuah FORM PENDAFTARAN -- TIDAK PERNAH dibaca di jalur PSB, sehingga
		 * Form Tambahan (termasuk isian upload berkas) tidak pernah muncul walau sudah dicentang.
		 *
		 * PERBAIKAN: pada kondisi BELUM LOGIN (calon siswa/orang tua mengisi formulir publik),
		 * salah satu dari kedua centang sudah cukup untuk menampilkan. Sifatnya PERMISIF: tidak
		 * ada instalasi yang kehilangan tampilan yang selama ini sudah muncul, hanya menambah
		 * penghormatan pada centang "Saat Registrasi" yang selama ini diabaikan. Bila keduanya
		 * tidak dicentang, tetap disembunyikan seperti semula.
		 */
		Tbmuser penggunaSaatIni = Common.getCurrentUser();
		boolean belumLogin = penggunaSaatIni == null || penggunaSaatIni.getUserId() == null;
		if (gel != null) {
			boolean bolehTampil = belumLogin
					? (gel.getTampilFormTambahanSaatRegistrasi() || gel.getTampilFormTambahanSaatLoginCalonMhs())
					: (formPendaftaran ? gel.getTampilFormTambahanSaatRegistrasi()
							: gel.getTampilFormTambahanSaatLoginCalonMhs());
			if (!bolehTampil) {
				return;
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		boolean login = tbmuser != null && tbmuser.getUserId() != null;

		Session session = HibernateUtil.currentSession();
		List<KelompokParameterTambahanCalonSiswa> kelompokParameterTambahanCalonSiswas = session
				.createCriteria(ParameterTambahanGelombangPendaftaranPsb.class)

				.add(!login
						? Restrictions.or(Restrictions.isNull("tampilDiFromSebelumLogin"),
								Restrictions.eq("tampilDiFromSebelumLogin", true))
						: Restrictions.sqlRestriction("true"))
				.add(login
						? Restrictions.or(Restrictions.isNull("tampilDiFromSetelahLogin"),
								Restrictions.eq("tampilDiFromSetelahLogin", true))
						: Restrictions.sqlRestriction("true"))

				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanCalonSiswa", "kelompokParameterTambahanCalonSiswa")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanCalonSiswa.aktif", true))
				.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonSiswa"))
				.add(Restrictions.or(Restrictions.isNull("gelombangPendaftaranPsb"),
						Restrictions.eq("gelombangPendaftaranPsb", gel)))
				.list();
		Collections.sort(kelompokParameterTambahanCalonSiswas);

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				calonSiswa.populateParameterTambahan(parameterRows);
			}
		};

		for (KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa : kelompokParameterTambahanCalonSiswas) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setStyle("border:0px;background: transparent;");
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelBold(kelompokParameterTambahanCalonSiswa.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = session
					.createCriteria(ParameterTambahanGelombangPendaftaranPsb.class)

					.add(!login
							? Restrictions.or(Restrictions.isNull("tampilDiFromSebelumLogin"),
									Restrictions.eq("tampilDiFromSebelumLogin", true))
							: Restrictions.sqlRestriction("true"))
					.add(login
							? Restrictions.or(Restrictions.isNull("tampilDiFromSetelahLogin"),
									Restrictions.eq("tampilDiFromSetelahLogin", true))
							: Restrictions.sqlRestriction("true"))

					.add(Restrictions.eq("kelompokParameterTambahanCalonSiswa", kelompokParameterTambahanCalonSiswa))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanCalonSiswa", "kelompokParameterTambahanCalonSiswa")
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanCalonSiswa.aktif", true))
					.setProjection(Projections.groupProperty("parameterTambahan"))
					.add(Restrictions.or(Restrictions.isNull("gelombangPendaftaranPsb"),
							Restrictions.eq("gelombangPendaftaranPsb", gel)))
					.list();
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(tampilkanLabelBesarPadaFormPMB && !parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CalonSiswa.class, calonSiswa.getId(),
						kelompokParameterTambahanCalonSiswa.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();
					row.setValign("top");

					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");
					row.setAttribute("kelompokParameterTambahanCalonSiswa", kelompokParameterTambahanCalonSiswa);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = calonSiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/psb/ParameterTambahanPsbListener.java:222");

							}
						}
					}

					tampil |= ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							calonSiswa.getId(), val, ket, parameterTambahan, isi);
				}
			}
			rowParameterTambahan.setVisible(tampil);
		}
	}
}
