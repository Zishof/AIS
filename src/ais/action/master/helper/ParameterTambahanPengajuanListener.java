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
import ais.database.model.sekolah.PengajuanSiswa;
import ais.database.model.PengajuanMahasiswa;
import ais.database.model.KelompokParameterTambahanPengajuan;
import ais.database.model.ParameterTambahanPengajuan;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

/**
 * Pengelola baris-baris parameter tambahan dinamis pada form pengajuan pendaftaran, mendukung
 * dua konteks entitas yang saling eksklusif: {@link PengajuanMahasiswa} (pengajuan di jenjang
 * perguruan tinggi) dan {@link PengajuanSiswa} (pengajuan di jenjang sekolah). Perilakunya
 * sepenuhnya paralel dengan {@link ParameterTambahanPerbaikanAssetListener}, hanya beda entitas
 * target: parameter dikelompokkan lewat {@link KelompokParameterTambahanPengajuan}, tiap
 * kelompok memiliki beberapa {@link ParameterTambahan} yang dikonfigurasi dari master data, dan
 * kelas ini men-generate baris form ({@link Row}) secara runtime, memvalidasi isian wajib/
 * lampiran wajib, serta menuliskan hasil isian kembali ke entitas pengajuan yang bersangkutan.
 *
 * <p>
 * Hanya satu dari dua konstruktor yang dipakai per instance — instance yang dibuat dengan
 * konstruktor {@link PengajuanMahasiswa} memiliki {@code pengajuanSiswa == null} (dan
 * sebaliknya), dan {@link #onEvent(Event)} memilih cabang logika berdasarkan field mana yang
 * tidak null.
 * </p>
 */
public class ParameterTambahanPengajuanListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private PengajuanMahasiswa pengajuanMahasiswa = null;
	private PengajuanSiswa pengajuanSiswa = null;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanPengajuan> kelompokParameterTambahanPengajuans;

	/** Konstruktor untuk konteks pengajuan mahasiswa (perguruan tinggi); lihat parameter pada konstruktor {@link PengajuanSiswa} untuk penjelasan lengkap tiap argumen. */
	public ParameterTambahanPengajuanListener(PengajuanMahasiswa pengajuan,
			Set<KelompokParameterTambahanPengajuan> kelompokParameterTambahanPengajuans, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanPengajuans = kelompokParameterTambahanPengajuans;
		this.rows = rows;
		this.pengajuanMahasiswa = pengajuan;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Konstruktor untuk konteks pengajuan siswa (sekolah).
	 *
	 * @param pengajuan                            entitas pengajuan siswa yang sedang diedit
	 * @param kelompokParameterTambahanPengajuans  kelompok parameter tambahan yang berlaku
	 * @param parameterRows                        daftar baris form yang dikelola bersama
	 * @param lampiranLains                         lampiran yang sudah diunggah, dikunci per
	 *                                              {@code "kelompokId->parameterId"}
	 * @param rows                                  kontainer ZK tempat baris parameter ditambahkan
	 */
	public ParameterTambahanPengajuanListener(PengajuanSiswa pengajuan,
			Set<KelompokParameterTambahanPengajuan> kelompokParameterTambahanPengajuans, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanPengajuans = kelompokParameterTambahanPengajuans;
		this.rows = rows;
		this.pengajuanSiswa = pengajuan;
		this.lampiranLains = lampiranLains;
	}

	/**
	 * Memvalidasi seluruh baris parameter tambahan yang sedang ditampilkan: parameter wajib
	 * ({@link ParameterTambahan#getWajibDiisi()}) harus terisi, dan parameter yang mensyaratkan
	 * lampiran harus punya entri di {@code lampiranLains}. Berhenti dan menampilkan
	 * {@link MyMessageboxConfig} begitu menemukan pelanggaran pertama.
	 *
	 * @return {@code true} bila semua baris valid; {@code false} begitu ditemukan pelanggaran
	 */
	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan = (KelompokParameterTambahanPengajuan) row
					.getAttribute("kelompokParameterTambahanPengajuan");
			if (parameterTambahan != null && kelompokParameterTambahanPengajuan != null) {
				String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();

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

	/** Menulis nilai isian dari {@code parameterRows} saat ini ke entitas {@code pengajuan} mahasiswa yang diberikan. */
	public void onSave(PengajuanMahasiswa pengajuan) {

		pengajuan.populateParameterTambahan(parameterRows);

	}


	/** Menulis nilai isian dari {@code parameterRows} saat ini ke entitas {@code pengajuan} siswa yang diberikan. */
	public void onSave(PengajuanSiswa pengajuan) {

		pengajuan.populateParameterTambahan(parameterRows);

	}

	/**
	 * Membangun ulang seluruh baris parameter tambahan pada form: mengosongkan baris lama,
	 * lalu memilih konteks ({@code pengajuanSiswa} atau {@code pengajuanMahasiswa}, mana yang
	 * tidak null) dan untuk tiap kelompok parameter yang berlaku pada konteks tersebut,
	 * mengambil {@link ParameterTambahan} aktif terkait, membuat baris judul kelompok + baris
	 * per parameter (lewat {@link ParameterTambahan#initComponent}), memulihkan nilai
	 * tersimpan sebelumnya dari {@code getParameterTambahanInds()} entitas pengajuan
	 * bersangkutan, dan menyembunyikan baris judul kelompok bila tidak ada parameter yang
	 * tampil. Kedua cabang (siswa/mahasiswa) menjalankan logika yang identik, hanya berbeda
	 * entitas dan kelas sumber datanya.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		if (pengajuanSiswa != null) {

			EventListener isi = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pengajuanSiswa.populateParameterTambahan(parameterRows);
				}
			};

			Session session = HibernateUtil.currentSession();

			for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : kelompokParameterTambahanPengajuans) {

				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setVisible(false);
				rowParameterTambahan.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanPengajuan.getNama() + ""));
				parameterRows.add(rowParameterTambahan);

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan",
										kelompokParameterTambahanPengajuan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				boolean tampil = false;
				rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
				if (!parameterTambahans.isEmpty()) {

					for (ParameterTambahan parameterTambahan : parameterTambahans) {
						String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setValign("top");
						row.setAttribute("parameterTambahan", parameterTambahan);
						row.setValign("top");
						row.setAttribute("kelompokParameterTambahanPengajuan", kelompokParameterTambahanPengajuan);
						row.setParent(rows);
						row.appendChild(new Label(parameterTambahan.getLabelInputan()
								+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
						if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
							parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
						}
						String val = "";
						String ket = "";
						String[] spl = pengajuanSiswa.getParameterTambahanInds().split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							if (value[0].trim().equalsIgnoreCase(jenis)) {
								val = value.length > 1 ? value[1].trim() : "";
								try {
									ket = value.length > 0 ? value[value.length - 1] : "";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPengajuanListener.java:175");

								}
							}
						}

						boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
								pengajuanSiswa.getId(), val, ket, parameterTambahan, isi);

						// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

						tampil |= t;

					}
				}

				rowParameterTambahan.setVisible(tampil);
			}
		}
		
		
		else if (pengajuanMahasiswa != null) {

			EventListener isi = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pengajuanMahasiswa.populateParameterTambahan(parameterRows);
				}
			};

			Session session = HibernateUtil.currentSession();

			for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : kelompokParameterTambahanPengajuans) {

				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setVisible(false);
				rowParameterTambahan.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanPengajuan.getNama() + ""));
				parameterRows.add(rowParameterTambahan);

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan",
										kelompokParameterTambahanPengajuan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				boolean tampil = false;
				rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
				if (!parameterTambahans.isEmpty()) {

					for (ParameterTambahan parameterTambahan : parameterTambahans) {
						String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setValign("top");
						row.setAttribute("parameterTambahan", parameterTambahan);
						row.setValign("top");
						row.setAttribute("kelompokParameterTambahanPengajuan", kelompokParameterTambahanPengajuan);
						row.setParent(rows);
						row.appendChild(new Label(parameterTambahan.getLabelInputan()
								+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
						if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
							parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
						}
						String val = "";
						String ket = "";
						String[] spl = pengajuanMahasiswa.getParameterTambahanInds().split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							if (value[0].trim().equalsIgnoreCase(jenis)) {
								val = value.length > 1 ? value[1].trim() : "";
								try {
									ket = value.length > 0 ? value[value.length - 1] : "";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanPengajuanListener.java:257");

								}
							}
						}

						boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
								pengajuanMahasiswa.getId(), val, ket, parameterTambahan, isi);

						// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

						tampil |= t;

					}
				}

				rowParameterTambahan.setVisible(tampil);
			}
		}
	}
}
