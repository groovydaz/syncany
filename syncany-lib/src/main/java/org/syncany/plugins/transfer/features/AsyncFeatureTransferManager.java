/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.plugins.transfer.features;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageFileNotFoundException;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.RemoteFile;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class AsyncFeatureTransferManager implements FeatureTransferManager {
	private static final Logger logger = Logger.getLogger(AsyncFeatureTransferManager.class.getSimpleName());

	private final TransferManager underlyingTransferManager;
	private final Config config;
	private final Throttler throttler;

	public AsyncFeatureTransferManager(TransferManager originalTransferManager, TransferManager underlyingTransferManager, Config config, Async asyncAnnotation) {
		this.underlyingTransferManager = underlyingTransferManager;
		this.config = config;
		this.throttler = new Throttler(asyncAnnotation.maxRetries(), asyncAnnotation.maxWaitTime());
	}

	@Override
	public void connect() throws StorageException {
		underlyingTransferManager.connect();
	}

	@Override
	public void disconnect() throws StorageException {
		underlyingTransferManager.disconnect();
	}

	@Override
	public void init(final boolean createIfRequired) throws StorageException {
		underlyingTransferManager.init(createIfRequired);
	}

	@Override
	public void download(final RemoteFile remoteFile, final File localFile) throws StorageException {
		underlyingTransferManager.download(remoteFile, localFile);
	}

	@Override
	public void move(final RemoteFile sourceFile, final RemoteFile targetFile) throws StorageException {
		underlyingTransferManager.move(sourceFile, targetFile);
		waitForFile(targetFile);
	}

	@Override
	public void upload(final File localFile, final RemoteFile remoteFile) throws StorageException {
		underlyingTransferManager.upload(localFile, remoteFile);
		waitForFile(remoteFile);
	}

	@Override
	public boolean delete(final RemoteFile remoteFile) throws StorageException {
		return underlyingTransferManager.delete(remoteFile);
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(final Class<T> remoteFileClass) throws StorageException {
		return underlyingTransferManager.list(remoteFileClass);
	}

	@Override
	public String getRemoteFilePath(Class<? extends RemoteFile> remoteFileClass) {
		return underlyingTransferManager.getRemoteFilePath(remoteFileClass);
	}
	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		return underlyingTransferManager.test(testCreateTarget);
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return underlyingTransferManager.testTargetExists();
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return underlyingTransferManager.testTargetCanWrite();
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return underlyingTransferManager.testTargetCanCreate();
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return underlyingTransferManager.testRepoFileExists();
	}

	private void waitForFile(RemoteFile remoteFile) throws StorageException {
		while (true) {
			try {
				Path tempFilePath = Files.createTempFile("syncany-async-feature-tm", null);
				underlyingTransferManager.download(remoteFile, tempFilePath.toFile());

				logger.log(Level.FINER, "File found " + remoteFile);
				throttler.reset();
				Files.delete(tempFilePath);
			}
			catch (StorageFileNotFoundException e) {
				try {
					long waitForMs = throttler.next();
					logger.log(Level.FINER, "File not found on the remote side, perhaps its in transit, waiting " + waitForMs + "ms ...", e);
					Thread.sleep(waitForMs);
					continue;
				}
				catch (InterruptedException ie) {
					throw new StorageException("Unable to retry", ie);
				}
			}
			catch (IOException e) {
				throw new StorageException("Unable to retry", e);
			}

			break;
		}
	}

	private class Throttler {

		private final int maxRetries;
		private final int maxWait;
		private int currentIteration = 0;

		public Throttler(int maxRetries, int maxWait) {
			this.maxRetries = maxRetries;
			this.maxWait = maxWait * 1000; // maxWait in seconds
		}

		public long next() throws InterruptedException {
			long waitFor = (long) Math.pow(3, currentIteration++) * 100;

			if (waitFor > maxWait || currentIteration > maxRetries) {
				throw new InterruptedException("Unable to retry again, because ending criteria reached");
			}

			return waitFor;
		}

		public void reset() {
			currentIteration = 0;
		}

	}

}
