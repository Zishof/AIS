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
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.ParameterTambahanCatatanSiswa;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Listener ZK yang membangun secara dinamis form "parameter tambahan" (field kustom yang
 * dikonfigurasi admin, dikelompokkan lewat {@link KelompokParameterTambahanCatatanSiswa}) pada
 * layar Catatan Siswa, mengikuti pola yang sama seperti
 * {@link ais.action.master.sop.helper.ParameterTambahanDisposisiAlurSopListener} untuk modul SOP.
 *
 * <p>
 * Dipasang sebagai listener suatu event (mis. saat kelompok/jenis catatan berubah): {@link
 * #onEvent(Event)} membersihkan baris parameter yang sudah ada, lalu untuk tiap kelompok
 * parameter aktif membangun ulang baris-baris input ({@link MyFormRow}) berdasarkan definisi
 * {@link ParameterTambahanCatatanSiswa} yang aktif, mengisi nilai tersimpan (bila ada) dari
 * kolom {@code parameterTambahanInds} berformat baris {@code "kelompokId->parameterId<=>nilai<=>keterangan"}.
 * {@link #validate()} memeriksa field wajib isi dan lampiran wajib sebelum data disimpan;
 * {@link #onSave(CatatanSiswa)} menuliskan kembali nilai form ke entitas {@link CatatanSiswa}.
 * </p>
 */
public class ParameterTambahanCatatanSiswaListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private CatatanSiswa catatanSiswa;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanCatatanSiswa> kelompokParameterTambahanCatatanSiswas;

	/**
	 * Membangun listener untuk satu instance form catatan siswa.
	 *
	 * @param catatanSiswa                              entitas catatan siswa yang sedang diisi/diedit
	 * @param kelompokParameterTambahanCatatanSiswas     kelompok parameter tambahan yang relevan untuk konteks form ini
	 * @param parameterRows                              daftar baris ZK yang dikelola listener (diisi/dibersihkan di tempat)
	 * @param lampiranLains                              peta lampiran yang sudah diunggah, berkunci {@code "kelompokId->parameterId"}
	 * @param rows                                       kontainer ZK {@link Rows} tempat baris parameter disisipkan
	 */
	public ParameterTambahanCatatanSiswaListener(CatatanSiswa catatanSiswa,
			Set<KelompokParameterTambahanCatatanSiswa> kelompokParameterTambahanCatatanSiswas, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCatatanSiswas = kelompokParameterTambahanCatatanSiswas;
		this.rows = rows;
		this.catatanSiswa = catatanSiswa;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang ditampilkan: parameter wajib isi
	 * harus memiliki nilai, dan parameter yang mewajibkan lampiran harus sudah memiliki lampiran
	 * terunggah. Menampilkan pesan informasi dan menghentikan pemeriksaan pada pelanggaran
	 * pertama yang ditemukan.
	 *
	 * @return {@code true} bila seluruh parameter valid (atau tidak ada parameter ditampilkan), {@code false} bila ada pelanggaran
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa = (KelompokParameterTambahanCatatanSiswa) row
					.getAttribute("kelompokParameterTambahanCatatanSiswa");
			if (parameterTambahan != null && kelompokParameterTambahanCatatanSiswa != null) {
				String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanSiswa.class, catatanSiswa.getId(),
						kelompokParameterTambahanCatatanSiswa.getId() + "->" + parameterTambahan.getId());

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

	/** Menuliskan kembali nilai-nilai yang sudah diisi pada {@link #parameterRows} ke entitas {@code catatanSiswa} sebelum disimpan. */
	public void onSave(CatatanSiswa catatanSiswa) {

		catatanSiswa.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris input parameter tambahan: menyembunyikan/membersihkan baris
	 * lama, lalu untuk setiap kelompok parameter aktif menambahkan judul kelompok dan baris
	 * input per parameter aktif (diurutkan), mengisi nilai tersimpan dari
	 * {@code catatanSiswa.getParameterTambahanInds()} bila ada. Baris judul kelompok hanya
	 * ditampilkan bila kelompok tersebut memiliki minimal satu parameter yang benar-benar
	 * dirender ({@link ParameterTambahan#initComponent}).
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
				catatanSiswa.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa : kelompokParameterTambahanCatatanSiswas) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCatatanSiswa.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanSiswa.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa",
											kelompokParameterTambahanCatatanSiswa))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanSiswa",
											"kelompokParameterTambahanCatatanSiswa")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanSiswa.class, catatanSiswa.getId(),
						kelompokParameterTambahanCatatanSiswa.getId() + "->" + parameterTambahan.getId());

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setAttribute("kelompokParameterTambahanCatatanSiswa", kelompokParameterTambahanCatatanSiswa);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = catatanSiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/ParameterTambahanCatatanSiswaListener.java:154");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							catatanSiswa.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
