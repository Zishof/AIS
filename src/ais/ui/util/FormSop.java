package ais.ui.util;

import java.io.File;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.GeneralValueObject;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

public interface FormSop {
	
	
	
	
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception;

	public boolean onSave(Event event) throws Exception;

	public String istilah() throws Exception;

	public DataSop ambil() throws Exception;

	@SuppressWarnings("rawtypes")
	public Class ambilClass() throws Exception;

	public void setPersetujuan(boolean persetujuan);
	
	public File cetakData(GeneralValueObject generalValueObject) throws Exception;
}
