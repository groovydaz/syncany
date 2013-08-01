package org.syncany.tests.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.local.LocalConnection;

public class TestConfigUtil {
	public static Config createTestLocalConfig() throws Exception {
		return createTestLocalConfig("syncanyclient");
	}
	
	public static Config createTestLocalConfig(String machineName) throws Exception {
		return createTestLocalConfig(machineName, createTestLocalConnection());
	}
	
	public static Config createTestLocalConfig(String machineName, Connection connection) throws Exception {
		File tempClientDir = TestFileUtil.createTempDirectoryInSystemTemp("syncanyclient-"+machineName);
		File tempLocalDir = new File(tempClientDir+"/local");
		File tempAppDir = new File(tempClientDir+"/app");
		File tempAppCacheDir =new File(tempClientDir+"/cache");
		File tempAppDatabaseDir = new File(tempClientDir+"/db");
		
		tempLocalDir.mkdirs();
		tempAppDir.mkdirs();
		tempAppCacheDir.mkdirs();
		tempAppDatabaseDir.mkdirs();
		
		Config config = new Config("Password");
		config.setMachineName(machineName+Math.abs(new Random().nextInt()));
		config.setAppDir(tempAppDir);
		config.setAppCacheDir(tempAppCacheDir);
		config.setAppDatabaseDir(tempAppDatabaseDir);
		config.setLocalDir(tempLocalDir);			
		config.setConnection(connection);

		return config;		
	}
	
	public static Connection createTestLocalConnection() throws Exception {
		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp("syncanyrepo");

		Plugin plugin = Plugins.get("local");
		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());

		Connection conn = plugin.createConnection();
		conn.init(pluginSettings);
		
		return conn;
	}	

	public static void deleteTestLocalConfigAndData(Config config) {
		TestFileUtil.deleteDirectory(config.getLocalDir());
		TestFileUtil.deleteDirectory(config.getAppDir());
		TestFileUtil.deleteDirectory(config.getAppCacheDir());
		TestFileUtil.deleteDirectory(config.getAppDatabaseDir());
		
		deleteTestLocalConnection(config);
	}

	private static void deleteTestLocalConnection(Config config) {
		LocalConnection connection = (LocalConnection) config.getConnection();
		TestFileUtil.deleteDirectory(connection.getRepositoryPath());		
	}
}