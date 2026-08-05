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
import ais.database.model.CatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
import ais.database.model.ParameterTambahanCatatanAdministrasi;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

public class ParameterTambahanCatatanAdministrasiListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private CatatanAdministrasi catatanAdministrasi;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanCatatanAdministrasi> kelompokParameterTambahanCatatanAdministrasis;

	public ParameterTambahanCatatanAdministrasiListener(CatatanAdministrasi catatanAdministrasi,
			Set<KelompokParameterTambahanCatatanAdministrasi> kelompokParameterTambahanCatatanAdministrasis, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanCatatanAdministrasis = kelompokParameterTambahanCatatanAdministrasis;
		this.rows = rows;
		this.catatanAdministrasi = catatanAdministrasi;
		this.lampiranLains = lampiranLains;
	}

	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanCatatanAdministrasi) row
					.getAttribute("kelompokParameterTambahanCatatanAdministrasi");
			if (parameterTambahan != null && kelompokParameterTambahanCatatanAdministrasi != null) {
				String jenis = kelompokParameterTambahanCatatanAdministrasi.getId() + "->" + parameterTambahan.getId();

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

	public void onSave(CatatanAdministrasi catatanAdministrasi) {

		catatanAdministrasi.populateParameterTambahan(parameterRows);

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
				catatanAdministrasi.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi : kelompokParameterTambahanCatatanAdministrasis) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanCatatanAdministrasi.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanAdministrasi.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi",
											kelompokParameterTambahanCatatanAdministrasi))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanAdministrasi",
											"kelompokParameterTambahanCatatanAdministrasi")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCatatanAdministrasi.getId() + "->" + parameterTambahan.getId();

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setAttribute("kelompokParameterTambahanCatatanAdministrasi", kelompokParameterTambahanCatatanAdministrasi);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = catatanAdministrasi.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanCatatanAdministrasiListener.java:154");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							catatanAdministrasi.getId(), val, ket, parameterTambahan, isi);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
