package ais.action.master.sekolah.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CatatanGuru;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanGuru;
import ais.database.model.sekolah.ParameterTambahanCatatanGuru;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener ZK yang membangun, memvalidasi, dan menyimpan "parameter tambahan" dinamis
 * ({@link ParameterTambahan}) pada form {@link CatatanGuru} (catatan guru terhadap siswa),
 * dikelompokkan per {@link KelompokParameterTambahanCatatanGuru} — padanan modul sekolah dari
 * {@code ParameterTambahanPengajuanTransaksiPegawaiListener} pada modul payroll, dengan struktur
 * dan pola kerja yang identik.
 *
 * <p>
 * Nilai tersimpan sebagai teks berformat baris {@code "<idKelompok>-><idParameter><=>nilai<=>keterangan"}
 * pada kolom {@code parameterTambahanInds} milik {@code catatanGuru}, di-parse ulang tiap kali form
 * dibangun untuk mengisi nilai awal komponen. {@link #onEvent} membangun ulang seluruh baris
 * parameter dari nol; {@link #validate()} memeriksa parameter wajib diisi dan lampiran wajib;
 * {@link #onSave} menuliskan nilai akhir kembali ke entitas lewat
 * {@code catatanGuru.populateParameterTambahan}.
 * </p>
 */
public class ParameterTambahanCatatanGuruListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private CatatanGuru catatanGuru;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanCatatanGuru> kelompokParameterTambahanCatatanGurus;

	/**
	 * Membuat listener terikat {@code catatanGuru} dan kelompok parameter yang relevan.
	 *
	 * @param catatanGuru                          entitas induk yang parameter tambahannya dikelola
	 * @param kelompokParameterTambahanCatatanGurus kelompok parameter yang ditampilkan
	 * @param parameterRows                        daftar baris komponen dinamis (diisi/dibersihkan oleh listener ini)
	 * @param lampiranLains                        peta lampiran per {@code jenis} parameter yang sudah diunggah
	 * @param rows                                 {@link Rows} host tempat baris ditempel
	 */
	public ParameterTambahanCatatanGuruListener(CatatanGuru catatanGuru,
			Set<KelompokParameterTambahanCatatanGuru> kelompokParameterTambahanCatatanGurus, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCatatanGurus = kelompokParameterTambahanCatatanGurus;
		this.rows = rows;
		this.catatanGuru = catatanGuru;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter yang sedang ditampilkan: menolak (menampilkan pesan dan
	 * mengembalikan {@code false}) bila ada parameter wajib diisi yang masih kosong, atau parameter
	 * yang mewajibkan lampiran tapi belum ada file di {@link #lampiranLains}.
	 *
	 * @return {@code true} bila seluruh baris valid atau tidak ada baris sama sekali
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanGuru = (KelompokParameterTambahanCatatanGuru) row
					.getAttribute("kelompokParameterTambahanCatatanGuru");
			if (parameterTambahan != null && kelompokParameterTambahanCatatanGuru != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanGuru.class, catatanGuru.getId(),
						kelompokParameterTambahanCatatanGuru.getId() + "->" + parameterTambahan.getId());

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

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

	/** Menuliskan nilai seluruh baris parameter dinamis saat ini ke {@code catatanGuru} lewat {@code populateParameterTambahan}. */
	public void onSave(CatatanGuru catatanGuru) {

		catatanGuru.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan dari nol: menyembunyikan/mengosongkan baris
	 * lama, lalu untuk tiap {@link KelompokParameterTambahanCatatanGuru} memuat
	 * {@link ParameterTambahan} aktif terkait (terurut), membuat baris judul kelompok dan satu baris
	 * komponen input per parameter (nilai awal di-parse dari {@code parameterTambahanInds}), lalu
	 * mendelegasikan pembuatan komponen ke {@link ParameterTambahan#initComponent}. Kelompok tanpa
	 * parameter yang benar-benar ditampilkan disembunyikan.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				catatanGuru.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanGuru : kelompokParameterTambahanCatatanGurus) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCatatanGuru.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanGuru.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanGuru",
											kelompokParameterTambahanCatatanGuru))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanGuru",
											"kelompokParameterTambahanCatatanGuru")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanGuru.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanGuru.class, catatanGuru.getId(),
						kelompokParameterTambahanCatatanGuru.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setAttribute("kelompokParameterTambahanCatatanGuru", kelompokParameterTambahanCatatanGuru);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = catatanGuru.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/ParameterTambahanCatatanGuruListener.java:154");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							catatanGuru.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
