/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 of the GNU General Public License as published          *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2013 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpconsultoresyasociados.com                      *
 *************************************************************************************/
package org.spin.app.form;


import java.util.List;
import java.util.logging.Level;

import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.window.FDialog;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
import org.compiere.util.ValueNamePair;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkex.zul.North;
import org.zkoss.zkex.zul.South;

/**
 * @author Yamel Senih
 *
 */
public class WGetWeight extends GetWeight implements IFormController, EventListener {
	
	/**
	 * *** Constructor de la Clase ***
	 * @author Yamel Senih 25/03/2013, 19:08:33
	 * @param gridTab
	 */
	public WGetWeight() {
		log.fine("WGetWeightUI()");
		setTitle(Msg.translate(Env.getCtx(), "GetWeightFromScale") + " .. ");
		
		p_WindowNo = form.getWindowNo();

		try	{
			loadWeightScale();
			if (!dynInit())
				return;
			setInitOK(true);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
			setInitOK(false);
		}
	}
	/** Window No		*/
	protected int 		p_WindowNo;
	protected CLogger log = CLogger.getCLogger(getClass());
	private String title;

	
	/**	Label Display				*/
	public Label 			lDisplay 	= new Label();
	/**	Display						*/
	public Textbox 			fDisplay 	= new Textbox();

	/**	Custom Form			*/
	private CustomForm form = new CustomForm(){
		public void setProcessInfo(org.compiere.process.ProcessInfo pi) {
			setFromPO(pi);
			
		};
		};

	/**	Weight Result				*/
	public String 		weight			= null;
	private boolean isClosed = false;
	
	//	
	private ConfirmPanel 	confirmPanel = new ConfirmPanel(true);
	private Borderlayout mainLayout = new Borderlayout();
	private Grid parameterLayout = GridFactory.newGridLayout();
	private Panel parameterPanel = new Panel();
	/**
	 *  Dynamic Init
	 *  @throws Exception if Lookups cannot be initialized
	 *  @return true if initialized
	 */
	@Override
	public boolean dynInit() throws Exception {
		log.config("dynInit()");

		System.setSecurityManager(null);
		zkInit();
		
		confirmPanel.addActionListener(this);

		form.setWidth("600px");
		form.setHeight("155px");
		form.setTitle(getTitle());
		form.setSizable(true);
		form.setBorder("normal");
		return true;
	}
	
	public void init(int windowNo, ProcessInfo processInfo) {
		setFromPO(processInfo);
		//	;
	}
	 
	/**
	 * Create UI Panel
	 * @author Raul Muñoz 16/01/2015, 12:11:08
	 * @throws Exception
	 * @return void
	 */
	private void zkInit() throws Exception {
		log.info("jbInit()");

		form.appendChild(mainLayout);
		mainLayout.setWidth("99%");
		mainLayout.setHeight("100%");
		mainLayout.setHeight("100%");
		mainLayout.setWidth("99%");

		parameterLayout.setWidth("100%");
		parameterPanel.appendChild(parameterLayout);
		North north = new North();
		north.setStyle("border: none");
		mainLayout.appendChild(north);
		north.appendChild(parameterPanel);
		
		Rows rows = null;
		Row row = null;
		
		parameterLayout.setWidth("600px");
		rows = parameterLayout.newRows();
		row = rows.newRow();
		
		//	
		lDisplay.setText(Msg.translate(Env.getCtx(), "Weight"));
		lDisplay.setStyle("font-size:42px;text-align: right;");
		fDisplay.setStyle("font-size:42px;text-align: right;");
		fDisplay.setWidth("400px");
		fDisplay.setHeight("80px");
		fDisplay.setText("- - - - - - - - - - -");
		fDisplay.setReadonly(true);
		//	
		
		row.appendChild(lDisplay);
		row.appendChild(fDisplay);
		fDisplay.addEventListener(Events.ON_CHANGING, this);
		loadButtons();
		//	Add Pane
		South south = new South();
		south.setStyle("border: none");
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);
		confirmPanel.addActionListener(this);
		//	
		weight = "";
	}

	/**
	 * Load Options to choice as buttons
	 * @author <a href="mailto:yamelsenih@gmail.com">Yamel Senih</a> 29/03/2013, 03:54:28
	 * @return void
	 * @throws Exception 
	 */
	private void loadButtons() throws Exception{
		log.info("loadButtons()");
		List<ValueNamePair> weightScaleList = getWeightScaleList();
		if(weightScaleList.size() == 0)
			throw new Exception(Msg.translate(Env.getCtx(), "@WeightScaleNotConfigForUser@"));
		//	
		weightScaleList.stream().forEach(weightScale -> {
			Button aa = new Button(weightScale.getName());
			aa.setLabel(weightScale.getName());
			aa.setName(weightScale.getName());
			aa.setId(weightScale.getID());
			confirmPanel.addButton(aa);
			aa.addEventListener(Events.ON_CLICK, this);
			aa.setName(weightScale.getID());
			aa.addActionListener(this);
			log.fine("WeightScale " + weightScale.getName());
		});
		
	}

	@Override
	public void onEvent(Event e) throws Exception {
		log.info("actionPerformed(ActionEvent e) " + e);
		if(e.getTarget().equals(fDisplay) && !isClosed) {
			Clients.evalJavaScript("$('#"+fDisplay.getId()+"').val('"+weight+"');");
			Events.echoEvent(Events.ON_CHANGING, fDisplay,weight);
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{	
			isClosed=true;
			stopService();
			log.fine("Action Comand OK");
			try
			{	
				Trx.run(new TrxRunnable()
				{
					public void run(String trxName)
					{
						log.fine("save(" + trxName + ")");
						processValue();
					}
				});
			}
			catch (Exception ex)
			{
				FDialog.error(p_WindowNo, form, "Error", ex.getLocalizedMessage());
			}
			finally {
				dispose();
			}
		}
		//  Cancel
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			log.fine("Action Comand CANCEL");
			stopService();
			dispose();
		}
		//	Serial Port Configuration
		else if(confirmPanel.getButton(e.getTarget().getId()) != null) {
			log.fine("Action Comand Any");
			setCurrentWeightScale(e.getTarget().getId());
			stopService();

			boolean ok = startService();
			if(!ok)
				FDialog.error(p_WindowNo, form, "Error", getMessage());
		}
	}

	public void dispose() {
		log.fine("Closed Window");
		mainLayout = null;
		parameterLayout = null;
		parameterPanel = null;
		isClosed = true;
		form.dispose();
		
	}
	
	
	@Override
	public ADForm getForm() {
		// TODO Auto-generated method stub
		return form;
	}

	public void setInitOK(boolean initOK) {
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public void refreshDisplay(String value) {
	    weight = value;
		Events.echoEvent(Events.ON_CHANGING, fDisplay,weight);
	}
	
}
