package com.pmease.gitplex.web.page.repository.info.code.blob.renderer;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import com.pmease.gitplex.core.GitPlex;

import org.apache.commons.io.FilenameUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.tika.io.IOUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ContentDisposition;
import org.eclipse.jgit.lib.ObjectStream;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.gitplex.core.manager.RepositoryManager;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.permission.ObjectPermission;
import com.pmease.gitplex.web.exception.AccessDeniedException;
import com.pmease.gitplex.web.page.account.AccountPage;
import com.pmease.gitplex.web.page.repository.RepositoryPage;
import com.pmease.gitplex.web.service.FileBlob;
import com.pmease.gitplex.web.service.FileBlobService;
import com.pmease.gitplex.web.service.FileTypes;
import com.pmease.gitplex.web.util.UrlUtils;

public class RawBlobResource extends AbstractResource {
	private static final long serialVersionUID = 1L;

	public RawBlobResource() {
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		PageParameters params = attributes.getParameters();
		
		final String username = params.get(AccountPage.PARAM_USER).toString();
		final String repositoryName = params.get(RepositoryPage.PARAM_REPO).toString();
		final String revision = params.get("objectId").toString();
		
		List<String> paths = Lists.newArrayList();
		for (int i = 0; i < params.getIndexedCount(); i++) {
			paths.add(params.get(i).toString());
		}
		
		Preconditions.checkArgument(username != null);
		Preconditions.checkArgument(!Strings.isNullOrEmpty(repositoryName));
		Preconditions.checkArgument(!Strings.isNullOrEmpty(revision));
		Preconditions.checkArgument(!paths.isEmpty());
		
		Repository repository = GitPlex.getInstance(RepositoryManager.class).findBy(username, repositoryName);
		if (repository == null) {
			throw new EntityNotFoundException("Repository " + username + "/" + repositoryName + " doesn't exist");
		}
		
		if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryRead(repository))) {
			throw new AccessDeniedException("User " + SecurityUtils.getSubject() 
					+ " have no permission to access repository " 
					+ repository.getFullName());
		}
		
		ResourceResponse response = new ResourceResponse();
		
		if (response.dataNeedsToBeWritten(attributes)) {
			final String path = UrlUtils.concatSegments(paths);
			FileBlob blob = FileBlob.of(repository, revision, path);
			response.setContentLength(blob.getSize());
			String fileName = FilenameUtils.getName(blob.getFilePath());
			response.setFileName(fileName);
			response.setContentType(blob.getMediaType().toString());
			
			if (GitPlex.getInstance(FileTypes.class).isSafeInline(blob.getMediaType())) {
				response.setContentDisposition(ContentDisposition.INLINE);
			} else {
				response.setContentDisposition(ContentDisposition.ATTACHMENT);
			}
			
			final Long repositoryId = repository.getId();
			response.setWriteCallback(new WriteCallback() {
				@Override
				public void writeData(final Attributes attributes) {
					Repository repository = GitPlex.getInstance(Dao.class).load(Repository.class, repositoryId);
					ObjectStream os = GitPlex.getInstance(FileBlobService.class).openStream(repository, revision, path);
					try {
						ByteStreams.copy(os, attributes.getResponse().getOutputStream());
					} catch (IOException e) {
						throw Throwables.propagate(e);
					} finally {
						IOUtils.closeQuietly(os);
					}
				}
			});
		}
		
		return response;
	}

	
}