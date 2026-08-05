package ais.action.master.helper;

import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.file.LampiranLain;

public interface JenisParsingReconsile {
	public void parsing(LampiranLain lampiranLain, JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost) throws Exception;
}
