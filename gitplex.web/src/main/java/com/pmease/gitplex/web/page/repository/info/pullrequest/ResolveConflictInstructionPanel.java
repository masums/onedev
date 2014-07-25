package com.pmease.gitplex.web.page.repository.info.pullrequest;

import static com.pmease.gitplex.core.pullrequest.IntegrationStrategy.*;

import com.pmease.gitplex.core.GitPlex;

import org.apache.shiro.SecurityUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.permission.ObjectPermission;
import com.pmease.gitplex.core.pullrequest.IntegrationInfo;
import com.pmease.gitplex.core.pullrequest.IntegrationStrategy;

@SuppressWarnings("serial")
public class ResolveConflictInstructionPanel extends Panel {

	public ResolveConflictInstructionPanel(String id, IModel<PullRequest> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequest request = (PullRequest) getDefaultModelObject();
		Fragment fragment;
		Repository targetRepo = request.getTarget().getRepository();
		User user = GitPlex.getInstance(UserManager.class).getCurrent();
		IntegrationInfo integrationInfo = request.getIntegrationInfo();
		String requestHead = integrationInfo.getRequestHead();
		IntegrationStrategy strategy = integrationInfo.getIntegrationStrategy();
		boolean sameRepo = request.getTarget().getRepository().equals(request.getSource().getRepository());					
		if (strategy == REBASE_SOURCE) {
			fragment = new Fragment("content", "rebaseInSourceFrag", this);
			fragment.add(new Label("srcRepoName", request.getSource().getRepository()));
			fragment.add(new Label("srcBranchNameForCheckout", request.getSource().getName()));
			fragment.add(new Label("srcBranchNameForPush", request.getSource().getName()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("destBranchName", request.getTarget().getName()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("destRepoUrl", request.getTarget().getRepository().getUrl()));
			differentRepoContainer.add(new Label("destBranchName", request.getTarget().getName()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		} else if (strategy == REBASE_TARGET) {
			fragment = new Fragment("content", "rebaseInTargetFrag", this);
			fragment.add(new Label("destRepoName", request.getTarget().getRepository()));
			fragment.add(new Label("destBranchNameForCheckout", request.getTarget().getName()));
			fragment.add(new Label("destBranchNameForPush", request.getTarget().getName()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("srcBranchName", request.getSource().getName()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("srcRepoUrl", request.getSource().getRepository().getUrl()));
			differentRepoContainer.add(new Label("srcBranchName", request.getSource().getName()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		} else if (user != null 
						&& SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryWrite(targetRepo))
						&& targetRepo.getGateKeeper().checkCommit(user, request.getTarget(), requestHead).allowIntegration()) {
			fragment = new Fragment("content", "mergeInTargetFrag", this);
			fragment.add(new Label("destRepoName", request.getTarget().getRepository()));
			fragment.add(new Label("destBranchNameForCheckout", request.getTarget().getName()));
			fragment.add(new Label("destBranchNameForPush", request.getTarget().getName()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			sameRepoContainer.add(new Label("srcBranchName", request.getSource().getName()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			differentRepoContainer.add(new Label("srcRepoUrl", request.getSource().getRepository().getUrl()));
			differentRepoContainer.add(new Label("srcBranchName", request.getSource().getName()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		} else {
			fragment = new Fragment("content", "mergeInSourceFrag", this);
			fragment.add(new Label("srcRepoName", request.getSource().getRepository()));
			fragment.add(new Label("srcBranchNameForCheckout", request.getSource().getName()));
			fragment.add(new Label("srcBranchNameForPush", request.getSource().getName()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			sameRepoContainer.add(new Label("destBranchName", request.getTarget().getName()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			differentRepoContainer.add(new Label("destRepoUrl", request.getTarget().getRepository().getUrl()));
			differentRepoContainer.add(new Label("destBranchName", request.getTarget().getName()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		}
		
		add(fragment);
	}

}