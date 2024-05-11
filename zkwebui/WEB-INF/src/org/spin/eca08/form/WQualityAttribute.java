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
 * Contributor(s): Carlos Parada www.erpya.com                    					 *
 *************************************************************************************/
package org.spin.eca08.form;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.NumberBox;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MProduct;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.spin.eca08.util.QualityAnalysisMaterialUtil;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.South;

/**
 * @author Carlos Parada, cparada@erpya.com
 */
public class WQualityAttribute extends QualityAttribute implements IFormController, EventListener {

	/**
	 * Constructor
	 */
	public WQualityAttribute() {
		
	}
	/** Window No		*/
	protected int 		p_WindowNo;
	protected CLogger log = CLogger.getCLogger(getClass());
	private Rows rows = null;
	private List<Object> m_editors = new ArrayList<>();

	/**	Custom Form			*/
	private CustomForm form = new CustomForm(){
		private static final long serialVersionUID = 1L;

		public void setProcessInfo(org.compiere.process.ProcessInfo pi) {
			setQualityAnalysisIdentifier(pi.getRecord_ID());
			
			log.fine("WQualityAttribute()");
			Optional<MProduct> maybeProduct = Optional.ofNullable(getProduct());
			maybeProduct.ifPresent(product -> setTitle(product.getName()));
			p_WindowNo = form.getWindowNo();

			try	{
				if (!dynInit())
					return;
			}
			catch(Exception e)
			{
				log.log(Level.SEVERE, "", e);
			}
		};
	};

	private ConfirmPanel 	confirmPanel = new ConfirmPanel(true);
	/**
	 * dynInit
	 * @return
	 * @throws Exception
	 */
	public boolean dynInit() throws Exception {
		log.config("dynInit()");
		
		System.setSecurityManager(null);
		form.setWidth("410px");
		form.setHeight("410px");
		
		Borderlayout layout = new Borderlayout();
		Center center = new Center();
		layout.appendChild(center);
		center.setFlex(true);
		center.setAutoscroll(true);
		center.setStyle("border: none");
		form.appendChild(layout);
		
		South south = new South();
		layout.appendChild(south);

		Grid grid = new Grid();
		grid.setWidth("400px");
		grid.setStyle("margin:0; padding:0;");
		grid.makeNoStrip();
		grid.setOddRowSclass("even");
		center.appendChild(grid);
        
		rows = new Rows();
		grid.appendChild(rows);
		
		//	ConfirmPanel
		confirmPanel.addActionListener(this);	
		
		south.appendChild(confirmPanel);
		
		zkInit();
		
		
		return true;
	}
	
	/**
	 * init ZK
	 * @throws Exception
	 */
	private void zkInit() throws Exception {
		log.info("zkInit()");

		if (!initAttributes()) {
			
		}
	}

	@Override
	public void onEvent(Event e) throws Exception {
		
		log.info("actionPerformed(ActionEvent e) " + e);
		if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{	
			log.fine("Action Comand OK");
			saveSelection();
			dispose();
		}
		//  Cancel
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			log.fine("Action Comand CANCEL");
			dispose();
		}
	}

	public void dispose() {
		log.fine("Closed Window");
		form.dispose();
		
	}
	
	
	@Override
	public ADForm getForm() {
		return form;
	}

	@Override
	protected void addAttributeLine(MAttribute attribute, boolean isMandatory, boolean readOnly) {
		m_row++;
		Label label = new Label (attribute.getName());
		
			
		if (attribute.getDescription() != null)
			label.setTooltiptext(attribute.getDescription());
		
		Row row = rows.newRow();
		row.appendChild(label.rightAlign());
		//
		MAttributeInstance instance = getAttributeInstance(attribute.get_ID());
		if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
		{
			MAttributeValue[] values = attribute.getMAttributeValues();	//	optional = null
			Listbox editor = new Listbox();
			editor.setMold("select");
			for (MAttributeValue value : values) 
			{
				ListItem item = new ListItem(value != null ? value.getName() : "", value);
				editor.appendChild(item);
			}
			boolean found = false;
			if (instance != null)
			{
				for (int i = 0; i < values.length; i++)
				{
					if (values[i] != null && values[i].getM_AttributeValue_ID () == instance.getM_AttributeValue_ID ())
					{
						editor.setSelectedIndex (i);
						found = true;
						break;
					}
				}
				if (found)
					log.fine("Attribute=" + attribute.getName() + " #" + values.length + " - found: " + instance);
				else
					log.warning("Attribute=" + attribute.getName() + " #" + values.length + " - NOT found: " + instance);
			}	//	setComboBox
			else
				log.fine("Attribute=" + attribute.getName() + " #" + values.length + " no instance");
			row.appendChild(editor);
			editor.setEnabled(!readOnly);
			m_editors.add(editor);
		}
		else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
		{
			NumberBox editor = new NumberBox(false);
			if (instance != null)
				editor.setValue(instance.getValueNumber());
			else
				editor.setValue(Env.ZERO);
			row.appendChild(editor);
			editor.setEnabled(!readOnly);
			m_editors.add(editor);
		}
		else	//	Text Field
		{
			Textbox editor = new Textbox();
			if (instance != null)
				editor.setText(instance.getValue());
			row.appendChild(editor);
			editor.setEnabled(!readOnly);
			m_editors.add(editor);
		}
		
	}
	
	/**
	 *	Save Selection
	 *	@return void
	 */
	private void saveSelection() {
		if (isProcessed())
			return;
		log.fine ("Attributes=" + attributeUses.size());
		AtomicReference<String> mandatory = new AtomicReference<String>("");
		BinaryOperator<String> concat = (previous, next) -> previous.concat(next);
		AtomicInteger index = new AtomicInteger(0);
		attributeUses.forEach(attributeUse -> {
				MAttribute attribute = QualityAnalysisMaterialUtil.getAttribute(attributeUse.getM_Attribute_ID());
				if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType())) {
					Listbox editor = (Listbox)m_editors.get(index.get());
					MAttributeValue value = (MAttributeValue) editor.getSelectedItem().getValue();
					log.fine(attribute.getName() + "=" + value);
					if (attribute.isMandatory() && value == null)
						mandatory.accumulateAndGet(" - " + attribute.getName(), concat);
					//	Set value
					setAttributeInstanceValue(attribute.get_ID(), value);
				} else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType())) {
					NumberBox editor = (NumberBox)m_editors.get(index.get());
					Object valueObj = editor.getValue();
					BigDecimal value = Env.ZERO;
					if(valueObj instanceof Integer)
						value = new BigDecimal((Integer) valueObj);
					else
						value = (BigDecimal)valueObj;
					log.fine(attribute.getName() + "=" + value);
					if (attribute.isMandatory() && value == null)
						mandatory.accumulateAndGet(" - " + attribute.getName(), concat);
					if (value != null && value.scale() == 0)
						value = value.setScale(1, RoundingMode.HALF_UP);
					//	Set value
					setAttributeInstanceValue(attribute.get_ID(), Optional.ofNullable(value).orElse(Env.ZERO));
				} else {
					Textbox editor = (Textbox)m_editors.get(index.get());
					String value = editor.getText();
					log.fine(attribute.getName() + "=" + value);
					if (attribute.isMandatory() && (value == null || value.length() == 0))
						mandatory.accumulateAndGet(" - " + attribute.getName(), concat);
					//	Set value
					setAttributeInstanceValue(attribute.get_ID(), Optional.ofNullable(value).orElse(""));
				}
				
				index.getAndAdd(1);
		});
			
		//
		if (mandatory.get().length() > 0) {
			throw new AdempiereException("@FillMandatory@ " + mandatory);
		}
		
		//Update Attribute set instance Description
		updateDescription();
	}	//	saveSelection
	
}
