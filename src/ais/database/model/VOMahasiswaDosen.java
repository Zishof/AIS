package ais.database.model;

import java.util.TreeMap;

import org.zkoss.zul.Label;

public interface VOMahasiswaDosen {
	public String ambilKode();

	public String getNama();

	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label);
}
