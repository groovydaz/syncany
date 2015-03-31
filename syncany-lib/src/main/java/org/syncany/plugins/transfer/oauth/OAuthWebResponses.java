package org.syncany.plugins.transfer.oauth;

import org.apache.commons.io.IOUtils;
import io.undertow.util.StatusCodes;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public abstract class OAuthWebResponses {

	public static OAuthWebResponse createValidResponse() {
		return new OAuthWebResponse(StatusCodes.OK, loadHtml("ValidWebResponse.html", "Token successfully extracted."));
	}

	public static OAuthWebResponse createBadResponse() {
		return new OAuthWebResponse(StatusCodes.BAD_REQUEST, loadHtml("BadRequestWebResponse.html", "Error while acquiring token."));
	}

	private static String loadHtml(String fileName, String fallbackString) {
		String html = fallbackString;
		try {
			html = IOUtils.toString(OAuthWebResponses.class.getResourceAsStream("/org/syncany/plugins/oauth/" + fileName));
		}
		catch (Exception e) {
			// use fallback plain string
		}

		return html;
	}

}
