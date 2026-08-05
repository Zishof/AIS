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
import ais.database.model.CatatanMahasiswa;
import ais.database.model.KelompokParameterTambahanCatatanMahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

public class ParameterTambahanCatatanMahasiswaListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private CatatanMahasiswa catatanMahasiswa;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanCatatanMahasiswa> kelompokParameterTambahanCatatanMahasiswas;

	public ParameterTambahanCatatanMahasiswaListener(CatatanMahasiswa catatanMahasiswa,
			Set<KelompokParameterTambahanCatatanMahasiswa> kelompokParameterTambahanCatatanMahasiswas, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCatatanMahasiswas = kelompokParameterTambahanCatatanMahasiswas;
		this.rows = rows;
		this.catatanMahasiswa = catatanMahasiswa;
		this.lampiranLains = lampiranLains;
	}

	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa = (KelompokParameterTambahanCatatanMahasiswa) row
					.getAttribute("kelompokParameterTambahanCatatanMahasiswa");
			if (parameterTambahan != null && kelompokParameterTambahanCatatanMahasiswa != null) {
				String jenis = kelompokParameterTambahanCatatanMahasiswa.getId() + "->" + parameterTambahan.getId();

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

	public void onSave(CatatanMahasiswa catatanMahasiswa) {

		catatanMahasiswa.populateParameterTambahan(parameterRows);

	}

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
				catatanMahasiswa.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa : kelompokParameterTambahanCatatanMahasiswas) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCatatanMahasiswa.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanMahasiswa.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa",
											kelompokParameterTambahanCatatanMahasiswa))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanMahasiswa",
											"kelompokParameterTambahanCatatanMahasiswa")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCatatanMahasiswa.getId() + "->" + parameterTambahan.getId();

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setAttribute("kelompokParameterTambahanCatatanMahasiswa", kelompokParameterTambahanCatatanMahasiswa);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = catatanMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanCatatanMahasiswaListener.java:154");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							catatanMahasiswa.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
