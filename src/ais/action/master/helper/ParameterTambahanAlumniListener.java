package ais.action.master.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.KelompokParameterTambahanAlumni;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAlumni;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

public class ParameterTambahanAlumniListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private BiodataMahasiswa biodataMahasiswa;
	private Map<String, LampiranLain> lampiranLains;
	private Boolean digunakanUntukPenggunaAlumni;

	public ParameterTambahanAlumniListener(BiodataMahasiswa biodataMahasiswa, List<Row> parameterRows,
			Map<String, LampiranLain> lampiranLains, Rows rows, Boolean digunakanUntukPenggunaAlumni) {
		this.digunakanUntukPenggunaAlumni = digunakanUntukPenggunaAlumni;
		this.parameterRows = parameterRows;
		this.rows = rows;
		this.biodataMahasiswa = biodataMahasiswa;
		this.lampiranLains = lampiranLains;
	}

	// OPTIMASI: Helper Method untuk Parse Parameter String ke Map 
	// agar tidak melakukan operasi split("\n") berulang kali dalam loop
	private static Map<String, String[]> parseParameterMap(String rawParam) {
		Map<String, String[]> mapParam = new HashMap<String, String[]>();
		if (rawParam != null && !rawParam.trim().isEmpty()) {
			String[] spl = rawParam.split("\n");
			for (String d : spl) {
				if (!d.trim().isEmpty()) {
					String[] value = d.split("<=>");
					if (value.length > 0) {
						mapParam.put(value[0].trim().toLowerCase(), value);
					}
				}
			}
		}
		return mapParam;
	}

	@SuppressWarnings("unchecked")
	public static boolean validate(BiodataMahasiswa biodataMahasiswa, EventListener eventListener,
			final Boolean tampilMessage, Boolean digunakanUntukPenggunaAlumni) throws Exception {

		if (biodataMahasiswa == null) return false;

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null : biodataMahasiswa.getMahasiswa().getTahunangkatan();
		
		Session session = null;
		List<ParameterTambahanAlumni> parameterTambahanAlumnis = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			parameterTambahanAlumnis = session.createCriteria(ParameterTambahanAlumni.class)
					.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
							gel == null ? Restrictions.sqlRestriction("false")
									: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
					.add(digunakanUntukPenggunaAlumni
							? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
							: Restrictions.or(
									Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
									Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true)).list();

		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:97");}
			}
		}

		if (parameterTambahanAlumnis == null || parameterTambahanAlumnis.isEmpty()) {
			return true;
		}

		// OPTIMASI: Parsing parameter tambahan 1x saja O(1) Lookup
		Map<String, String[]> mapValParam = parseParameterMap(biodataMahasiswa.getParameterTambahanIndsAlumni());

		for (ParameterTambahanAlumni parameterTambahanAlumni : parameterTambahanAlumnis) {
			ParameterTambahan parameterTambahan = parameterTambahanAlumni.getParameterTambahan();
			KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni = parameterTambahanAlumni.getKelompokParameterTambahanAlumni();

			if (parameterTambahan != null && kelompokParameterTambahanAlumni != null) {
				String jenis = kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId();
				String jenisKey = jenis.toLowerCase();

				String val = "";
				if (mapValParam.containsKey(jenisKey)) {
					String[] value = mapValParam.get(jenisKey);
					val = value.length > 1 ? value[1].trim() : "";
				}

				boolean isTipeInputanValid = parameterTambahanAlumni.getParameterTambahan() != null && !parameterTambahanAlumni.getParameterTambahan().getTipeDataInputan().equals(ParameterTambahan.TIDAK_ADA);
				boolean isEmptyValue = (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"));
				boolean wajib = parameterTambahanAlumni.getWajibDiisi() && isTipeInputanValid && isEmptyValue;

				if (wajib) {
					if (tampilMessage) {
						MyMessageboxConfig.show(
								"\"" + kelompokParameterTambahanAlumni.getNama()
										+ "\" harus Anda lengkapi !\n\nPilihan \"" + parameterTambahan.getLabelInputan()
										+ "\" harus dipilih",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, eventListener);
					} else {
						if (eventListener != null) {
							eventListener.onEvent(null);
						}
					}
					return false;
				}

				if (parameterTambahan.getLampiranWajibDiisi() && parameterTambahan.getHarusMenyertakanLampiran()) {
					LampiranLain lam = LampiranLain.ambil(biodataMahasiswa.getId(), jenis);
					if (lam == null) {
						if (tampilMessage) {
							MyMessageboxConfig.show(
									"\"" + kelompokParameterTambahanAlumni.getNama()
											+ "\" harus Anda lengkapi !\n\nUntuk pilihan \""
											+ parameterTambahan.getLabelInputan() + "\", lampiran harus diunggah",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									eventListener);
						} else {
							if (eventListener != null) {
								eventListener.onEvent(null);
							}
						}
						return false;
					}
				}
			}
		}
		return true;
	}

	public void onSave(BiodataMahasiswa biodataMahasiswa) {
		if (biodataMahasiswa != null && parameterRows != null) {
			biodataMahasiswa.populateParameterTambahanAlumni(parameterRows);
		}
	}

	public boolean check() {
		if (biodataMahasiswa == null) return false;

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null : biodataMahasiswa.getMahasiswa().getTahunangkatan();
		int count = 0;
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			count = ((Number) session.createCriteria(ParameterTambahanAlumni.class)
					.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
							gel == null ? Restrictions.sqlRestriction("false")
									: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
					.createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
					.add(digunakanUntukPenggunaAlumni
							? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
							: Restrictions.or(
									Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
									Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ParameterTambahanAlumniListener.java:194");
		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:197");}
			}
		}

		return count != 0;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		if (parameterRows != null) {
			for (Row row : parameterRows) {
				row.setVisible(false);
			}
			parameterRows.clear();
		}

		if (biodataMahasiswa == null) return;

		Integer gel = biodataMahasiswa.getMahasiswa() == null ? null : biodataMahasiswa.getMahasiswa().getTahunangkatan();
		
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			
			List<KelompokParameterTambahanAlumni> kelompokParameterTambahanAlumnis = ConstantValues.simpleList(
					session.createCriteria(ParameterTambahanAlumni.class)
							.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
									gel == null ? Restrictions.sqlRestriction("false")
											: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
							.createAlias("parameterTambahan", "parameterTambahan")
							.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
							.add(digunakanUntukPenggunaAlumni
									? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
									: Restrictions.or(
											Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
											Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
							.add(Restrictions.eq("parameterTambahan.aktif", true))
							.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true))
							.setProjection(Projections.groupProperty("kelompokParameterTambahanAlumni.id")),
					KelompokParameterTambahanAlumni.class, false);
					
			Collections.sort(kelompokParameterTambahanAlumnis);

			// OPTIMASI: Parsing data string ke dalam Map sekali saja sebelum looping bersarang
			Map<String, String[]> mapValParam = parseParameterMap(biodataMahasiswa.getParameterTambahanIndsAlumni());

			for (KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni : kelompokParameterTambahanAlumnis) {
				MyFormRow rowParameterTambahan = new MyFormRow();
				rowParameterTambahan.setStyle("border:0px;background: transparent;");
				rowParameterTambahan.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
				rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanAlumni.getNama() + ""));
				
				parameterRows.add(rowParameterTambahan);

				displayRinci(rowParameterTambahan, rows, session, kelompokParameterTambahanAlumni, gel, null, 0, mapValParam);
			}

		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:259");}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void displayRinci(Row rowParameterTambahan, Rows rowsUtama, Session session,
			KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni, Integer gel, ParameterTambahan parent,
			int indexParent, Map<String, String[]> mapValParam) {

		List<ParameterTambahanAlumni> parameterTambahanAlumnis = ConstantValues.simpleList(
				session.createCriteria(ParameterTambahanAlumni.class)
						.add(Restrictions.eq("kelompokParameterTambahanAlumni", kelompokParameterTambahanAlumni))
						.add(Restrictions.or(Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
								gel == null ? Restrictions.sqlRestriction("false")
										: Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE)))
						.createAlias("parameterTambahan", "parameterTambahan")
						.add(parent == null || parent.getId() == null ? Restrictions.isNull("parameterTambahan.parent") : Restrictions.eq("parameterTambahan.parent", parent))
						.createAlias("kelompokParameterTambahanAlumni", "kelompokParameterTambahanAlumni")
						.add(digunakanUntukPenggunaAlumni
								? Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", true)
								: Restrictions.or(
										Restrictions.eq("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni", false),
										Restrictions.isNull("kelompokParameterTambahanAlumni.digunakanUntukPenggunaAlumni")))
						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.add(Restrictions.eq("kelompokParameterTambahanAlumni.aktif", true)),
				ParameterTambahanAlumni.class);
				
		Collections.sort(parameterTambahanAlumnis);

		EventListener isi = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (biodataMahasiswa != null && parameterRows != null) {
					biodataMahasiswa.populateParameterTambahanAlumni(parameterRows);
				}
			}
		};

		boolean tampil = false;
		if (!parameterTambahanAlumnis.isEmpty()) {
			Map<Long, ParameterTambahan> mapdata = ConstantValues.ambilBerdasarClass(ParameterTambahan.class);
			
			for (ParameterTambahanAlumni parameterTambahanAlumni : parameterTambahanAlumnis) {
				ParameterTambahan parameterTambahan = parameterTambahanAlumni.getParameterTambahan();

				// SYARAT TAMPIL (conditional display): tampil HANYA bila jawaban parameter acuan == syaratNilai.
				// Dievaluasi dari jawaban tersimpan (mapValParam).
				final boolean lolosSyarat = lolosSyaratTampil(parameterTambahan, mapValParam);

				boolean adaChild = false;
				for (ParameterTambahan p : mapdata.values()) {
					if (p.getParent() != null && p.getParent().getId().equals(parameterTambahan.getId())) {
						adaChild = true;
						break;
					}
				}

				String jenis = kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId();
				String jenisKey = jenis.toLowerCase();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setAttribute("parameterTambahan", parameterTambahan);
				row.setAttribute("kelompokParameterTambahanAlumni", kelompokParameterTambahanAlumni);
				row.setParent(rowsUtama);
				
				row.appendChild(new Label(parameterTambahan.getLabelInputan() + (parameterTambahanAlumni.getWajibDiisi() ? " (*)" : " ")));
				
				if (parameterTambahan.getKeterangan() != null && !parameterTambahan.getKeterangan().trim().isEmpty()) {
					parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
				}

				// PENGAMBILAN NILAI MELALUI MAP (Sangat Cepat O(1))
				String val = "";
				String ket = "";
				if (mapValParam.containsKey(jenisKey)) {
					String[] value = mapValParam.get(jenisKey);
					val = value.length > 1 ? value[1].trim() : "";
					try {
						ket = value.length > 2 ? value[value.length - 1] : "";
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ParameterTambahanAlumniListener.java:340");}
				}

				boolean adaKomponen = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
						biodataMahasiswa.getId(), val, ket, parameterTambahan, isi);
				// SYARAT TAMPIL: sembunyikan baris parameter bila syarat tak terpenuhi.
				row.setVisible(lolosSyarat);
				tampil |= (adaKomponen && lolosSyarat);

				Rows childRows = rowsUtama;
				if (adaChild) {
					MyFormRow row1 = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(row1, "2");
					row1.setParent(rowsUtama);
					row1.setVisible(lolosSyarat); // ikut sembunyi bila induk parameter tak lolos syarat

					Grid grid = new Grid();
					grid.setSclass("dgrid fgrid");
					grid.setStyle("padding-left: 50px; background:transparent;");
					grid.setParent(row1);

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig();
					column.setParent(columns);

					childRows = new Rows();
					childRows.setParent(grid);

					displayRinci(rowParameterTambahan, childRows, session, kelompokParameterTambahanAlumni, gel,
							parameterTambahan, indexParent + 1, mapValParam);
				}
			}
			if (rowParameterTambahan != null) {
				rowParameterTambahan.setVisible(tampil);
			}
		}
	}

	/**
	 * Evaluasi SYARAT TAMPIL parameter: true bila TAK bersyarat, atau bila jawaban parameter acuan
	 * (syaratParameter) SAMA DENGAN syaratNilai. Nilai acuan dibaca dari peta jawaban tersimpan.
	 */
	private boolean lolosSyaratTampil(ParameterTambahan parameterTambahan, Map<String, String[]> mapValParam) {
		try {
			String json = parameterTambahan.getSyaratTampil();
			if (json == null || json.trim().isEmpty()) {
				return true; // tak bersyarat -> selalu tampil
			}
			org.json.JSONObject obj = new org.json.JSONObject(json.trim());
			org.json.JSONArray arr = obj.optJSONArray("syarat");
			if (arr == null || arr.length() == 0) {
				return true;
			}
			boolean or = "OR".equalsIgnoreCase(obj.optString("logika", "AND"));
			boolean hasilAnd = true;
			boolean hasilOr = false;
			int dievaluasi = 0;
			for (int i = 0; i < arr.length(); i++) {
				org.json.JSONObject c = arr.optJSONObject(i);
				if (c == null) {
					continue;
				}
				long pid = c.optLong("parameterId", 0L);
				if (pid == 0L) {
					continue;
				}
				String nilaiSyarat = c.optString("nilai", "");
				String nilaiAcuan = cariNilaiParam(mapValParam, Long.valueOf(pid));
				// Pencocokan label-aware (mis. "Bekerja" cocok dengan tersimpan "Bekerja:1") — konsisten dgn JSP.
				boolean cocok = ais.common.ParameterTambahanHtmlHelper.nilaiCocok(nilaiAcuan, nilaiSyarat);
				hasilAnd = hasilAnd && cocok;
				hasilOr = hasilOr || cocok;
				dievaluasi++;
			}
			if (dievaluasi == 0) {
				return true; // tak ada syarat valid -> tampil
			}
			return or ? hasilOr : hasilAnd;
		} catch (Exception e) {
			return true; // JSON invalid -> jangan sembunyikan (aman)
		}
	}

	/** Cari nilai jawaban parameter (berdasar id) dari peta jawaban (key: kelompokId->parameterId, lower-case). */
	private String cariNilaiParam(Map<String, String[]> mapValParam, Long parameterId) {
		if (mapValParam == null || parameterId == null) {
			return null;
		}
		String suffix = ("->" + parameterId).toLowerCase();
		for (Map.Entry<String, String[]> e : mapValParam.entrySet()) {
			if (e.getKey() != null && e.getKey().endsWith(suffix)) {
				String[] v = e.getValue();
				return (v != null && v.length > 1) ? v[1] : "";
			}
		}
		return null;
	}
}