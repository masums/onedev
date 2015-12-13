package com.pmease.commons.wicket.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;

import com.pmease.commons.wicket.component.floating.AlignPlacement;
import com.pmease.commons.wicket.component.floating.AlignTarget;
import com.pmease.commons.wicket.component.floating.ComponentTarget;
import com.pmease.commons.wicket.component.floating.RectTarget;
import com.pmease.commons.wicket.component.floating.FloatingPanel;

@SuppressWarnings("serial")
public abstract class DropdownLink extends AjaxLink<Void> {

	private final boolean alignTargetMouse;
	
	private final AlignPlacement placement;
	
	private FloatingPanel dropdown;

	public DropdownLink(String id) {
		this(id, AlignPlacement.bottom(0));
	}
	
	public DropdownLink(String id, AlignPlacement placement) {
		this(id, false, placement);
	}

	public DropdownLink(String id, boolean alignTargetMouse) {
		this(id, alignTargetMouse, AlignPlacement.bottom(0));
	}
	
	public DropdownLink(String id, boolean alignTargetMouse, AlignPlacement placement) {
		super(id);
		
		this.alignTargetMouse = alignTargetMouse;
		this.placement = placement;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(AttributeAppender.append("class", "dropdown-link"));
		
		setOutputMarkupId(true);
	}

	protected void onInitialize(FloatingPanel dropdown) {
		
	}
	
	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
		super.updateAjaxAttributes(attributes);

		String script = String.format(""
				+ "return {mouseX: $('#%s').data('mouseX'), mouseY: $('#%s').data('mouseY')};", 
				getMarkupId(), getMarkupId());
		attributes.getDynamicExtraParameters().add(script);
	}

	@Override
	public void onClick(AjaxRequestTarget target) {
		if (dropdown == null) { 
			AlignTarget alignFloatingWith;
			if (alignTargetMouse) {
				int mouseX = RequestCycle.get().getRequest().getRequestParameters()
						.getParameterValue("mouseX").toInt();
				int mouseY = RequestCycle.get().getRequest().getRequestParameters()
						.getParameterValue("mouseY").toInt();
				alignFloatingWith = RectTarget.ofMouse(mouseX, mouseY);
			} else { 
				alignFloatingWith =  new ComponentTarget(this);
			} 
			
			dropdown = new FloatingPanel(target, alignFloatingWith, placement) {
	
				@Override
				protected void onInitialize() {
					super.onInitialize();
					DropdownLink.this.onInitialize(this);
				}

				@Override
				protected Component newContent(String id) {
					return DropdownLink.this.newContent(id);
				}

				@Override
				protected void onClosed(AjaxRequestTarget target) {
					super.onClosed(target);

					String script = String.format("$('#%s').removeClass('dropdown-open');", 
							DropdownLink.this.getMarkupId(true));
					target.appendJavaScript(script);
					
					dropdown = null;
				}
	
			};
			String script = String.format(""
					+ "$('#%s').addClass('dropdown-open');"
					+ "$('#%s').data('trigger', $('#%s'));", 
					getMarkupId(), dropdown.getMarkupId(), getMarkupId());
			target.appendJavaScript(script);
		} else {
			dropdown.close(target);
		}
	}

	public void close(AjaxRequestTarget target) {
		if (dropdown != null)
			dropdown.close(target);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		String script = String.format(""
				+ "$('#%s').on('click', function(e){"
				+ "  $(this).data('mouseX', e.pageX).data('mouseY', e.pageY);"
				+ "});", getMarkupId());
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	protected abstract Component newContent(String id);
}