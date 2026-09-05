package ais.action.master.helper;

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
import ais.database.model.Pengaduan;
import ais.database.model.KelompokParameterTambahanPengaduan;
import ais.database.model.ParameterTambahanPengaduan;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * {@link EventListener} yang membangun secara dinamis input-input "parameter tambahan" pada form
 * {@link Pengaduan}, dikelompokkan menurut {@link KelompokParameterTambahanPengaduan}. Struktur dan
 * perilakunya identik dengan {@link ParameterTambahanCatatanPegawaiListener} (lihat javadoc kelas
 * tersebut untuk detail alur), hanya berbeda entitas target: setiap kali dipanggil ({@link #onEvent}),
 * seluruh baris parameter lama dihapus dan dibangun ulang dari konfigurasi
 * {@link ParameterTambahanPengaduan} yang aktif per kelompok, dengan nilai tersimpan diurai dari
 * {@code pengaduan.getParameterTambahanInds()} (format baris {@code "kelompokId->parameterId<=>nilai<=>keterangan"}).
 *
 * <p>
 * {@link #validate()} harus dipanggil sebelum simpan formulir untuk memastikan parameter wajib dan
 * lampiran wajib sudah lengkap; {@link #onSave(Pengaduan)} menuliskan nilai-nilai terisi kembali ke
 * entitas {@link Pengaduan} yang diberikan.
 * </p>
 */
public class ParameterTambahanPengaduanListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private Pengaduan pengaduan;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanPengaduan> kelompokParameterTambahanPengaduans;

	/**
	 * @param pengaduan                              entitas pengaduan yang formulirnya dibangun
	 * @param kelompokParameterTambahanPengaduans    kelompok-kelompok parameter yang ditampilkan
	 * @param parameterRows                          list keluaran yang diisi baris-baris parameter yang dibangun
	 * @param lampiranLains                           peta lampiran yang sudah diunggah, dikunci per "kelompokId->parameterId"
	 * @param rows                                    container {@link Rows} tempat baris parameter ditambahkan
	 */
	public ParameterTambahanPengaduanListener(Pengaduan pengaduan,
			Set<KelompokParameterTambahanPengaduan> kelompokParameterTambahanPengaduans, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanPengaduans = kelompokParameterTambahanPengaduans;
		this.rows = rows;
		this.pengaduan = pengaduan;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter yang sedang dibangun: parameter wajib-diisi harus memiliki
	 * nilai, dan parameter yang mewajibkan lampiran harus sudah memiliki entri di {@code lampiranLains}.
	 * Menampilkan dialog peringatan berisi label parameter yang gagal validasi (validasi berhenti pada
	 * kegagalan pertama).
	 *
	 * @return {@code true} bila seluruh parameter valid; {@code false} bila ada yang gagal
	 * @throws Exception diteruskan dari kegagalan menampilkan dialog
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan = (KelompokParameterTambahanPengaduan) row
					.getAttribute("kelompokParameterTambahanPengaduan");
			if (parameterTambahan != null && kelompokParameterTambahanPengaduan != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(Pengaduan.class, pengaduan.getId(),
						kelompokParameterTambahanPengaduan.getId() + "->" + parameterTambahan.getId());

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

	/** Menuliskan nilai-nilai parameter tambahan yang sedang terisi (dari {@code parameterRows}) ke entitas {@code pengaduan} yang diberikan, siap disimpan. */
	public void onSave(Pengaduan pengaduan) {

		pengaduan.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan: menghapus baris lama, lalu untuk setiap
	 * kelompok mengambil parameter yang aktif dan dikonfigurasi untuk kelompok tersebut, mengurai
	 * nilai tersimpan dari {@code pengaduan.getParameterTambahanInds()}, dan membangun komponen
	 * inputnya lewat {@code ParameterTambahan.initComponent}. Baris header kelompok hanya ditampilkan
	 * bila ada parameter yang berhasil dibangun untuk kelompok tersebut.
	 *
	 * @param event event pemicu (tidak dipakai isinya)
	 * @throws Exception diteruskan dari kegagalan Hibernate atau pembangunan komponen
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
				pengaduan.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan : kelompokParameterTambahanPengaduans) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanPengaduan.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanPengaduan.class)
									.add(Restrictions.eq("kelompokParameterTambahanPengaduan",
											kelompokParameterTambahanPengaduan))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanPengaduan",
											"kelompokParameterTambahanPengaduan")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanPengaduan.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(Pengaduan.class, pengaduan.getId(),
						kelompokParameterTambahanPengaduan.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setValign("top");row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");row.setAttribute("kelompokParameterTambahanPengaduan", kelompokParameterTambahanPengaduan);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = pengaduan.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(LampiranLain.kunciNilaiParameterTambahan(jenis))) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPengaduanListener.java:153");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							pengaduan.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
